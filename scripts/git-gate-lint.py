#!/usr/bin/env python3
"""commit / push 時点の防波堤 (Kasane 標準 lint / hook)。

書き込み時点の PreToolUse hook (identity-lint / local-path-lint) は Write / Edit / apply_patch しか通らず、
Bash のヒアドキュメントや cp で持ち込んだファイルは素通りする。ここは git の履歴に入る直前で
同じ判定を掛け直し、個体・個人・秘密を特定する値とローカル絶対パスが commit / push されるのを止める。
判定そのものは持たず、同じディレクトリの identity-lint.py と local-path-lint.py を読み込んで使う
(識別子の規約は ksn-core references/evidence.md、パスは references/paths.md)。

使い方:
  python3 scripts/git-gate-lint.py --staged          # pre-commit: ステージされた blob を検査 (違反があれば exit 1)
  python3 scripts/git-gate-lint.py --push            # pre-push: stdin の ref 一覧から push 対象コミットの blob を検査
  python3 scripts/git-gate-lint.py --range A..B      # 指定範囲のコミットが触ったファイルを検査 (手動点検用)
  python3 scripts/git-gate-lint.py --bash-hook       # PreToolUse (matcher: Bash) の stdin JSON を読み、git commit / push なら同じ検査をして deny を返す
  python3 scripts/git-gate-lint.py --selftest        # 一時リポジトリで各モードの疎通確認

検査するのは blob (git が実際に記録する内容)。worktree の内容ではないので、直したのに add し忘れたケースも正しく止まる。
push 検査は範囲内の各コミット時点の blob を見る (途中のコミットで入れて後で消した値も履歴には残るため)。

--bash-hook の判定:
  - コマンドに `git commit` を含む → ステージ blob を検査。同じコマンドに `git add` も含むなら、
    add で入りうる worktree の変更ファイル (未追跡・未ステージ) も検査する (hook は add の実行前に走るため)
  - コマンドに `git push` を含む → まだどのリモートにも無いコミット (`HEAD --not --remotes`) を検査
  - `--no-verify` / commit の `-n` / `core.hooksPath` の上書きを含む → 検査の迂回として deny
    (代わりの動き: 違反行を log-sanitize.py かプレースホルダで直してから commit / push する)

git hook としての登録 (ksn-init が配る .githooks/ を使う):
  git config core.hooksPath .githooks
"""

from __future__ import annotations

import importlib.util
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SELF_BASENAME = os.path.basename(os.path.abspath(__file__))
ZERO_SHA = re.compile(r"^0+$")

# コマンド判定 (--bash-hook)。`git -C dir commit` `git -c k=v push` のようにオプションを挟んだ形も拾う
GIT_CMD = r"\bgit\b(?:\s+-[-\w]+(?:[= ]\S+)?)*\s+"
RE_COMMIT = re.compile(GIT_CMD + r"commit\b")
RE_PUSH = re.compile(GIT_CMD + r"push\b")
RE_ADD = re.compile(GIT_CMD + r"add\b")
RE_NO_VERIFY = re.compile(r"--no-verify\b")
RE_COMMIT_N = re.compile(GIT_CMD + r"commit\b[^|;&\n]*\s-[a-zA-Z]*n[a-zA-Z]*\b")  # commit -n / -an は --no-verify
RE_HOOKSPATH = re.compile(r"core\.hooksPath", re.IGNORECASE)


def _load(name: str):
    path = os.path.join(HERE, name)
    spec = importlib.util.spec_from_file_location(name.replace("-", "_").replace(".py", ""), path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load("local-path-lint.py")
ID = _load("identity-lint.py")


# ---------- git ----------

def _git(root: str, *args: str, stdin: str | None = None) -> str:
    proc = subprocess.run(["git", *args], cwd=root, capture_output=True, text=True, input=stdin)
    if proc.returncode != 0:
        return ""
    return proc.stdout


def _git_bytes(root: str, *args: str) -> bytes | None:
    proc = subprocess.run(["git", *args], cwd=root, capture_output=True)
    return proc.stdout if proc.returncode == 0 else None


def _zsplit(out: str) -> list[str]:
    return [p for p in out.split("\0") if p]


def _decode(blob: bytes | None) -> str | None:
    """blob を本文にする。バイナリ (NUL を含む) は検査しない。"""
    if blob is None or b"\0" in blob[:8000]:
        return None
    return blob.decode("utf-8", errors="replace")


def staged_items(root: str) -> list[tuple[str, str]]:
    """(リポジトリ相対パス, ステージされた本文)。"""
    items = []
    for rel in _zsplit(_git(root, "diff", "--cached", "--name-only", "--diff-filter=ACMR", "-z")):
        text = _decode(_git_bytes(root, "show", f":{rel}"))
        if text is not None:
            items.append((rel, text))
    return items


def worktree_items(root: str) -> list[tuple[str, str]]:
    """未ステージの変更・未追跡ファイル (git add で入りうるもの) の worktree 本文。"""
    rels = set(_zsplit(_git(root, "diff", "--name-only", "--diff-filter=ACMR", "-z")))
    rels |= set(_zsplit(_git(root, "ls-files", "--others", "--exclude-standard", "-z")))
    items = []
    for rel in sorted(rels):
        try:
            with open(os.path.join(root, rel), "rb") as f:
                text = _decode(f.read())
        except OSError:
            continue
        if text is not None:
            items.append((rel, text))
    return items


def commit_items(root: str, commits: list[str]) -> list[tuple[str, str]]:
    """各コミットが触ったファイルの、そのコミット時点の本文。表示名は `<short sha>:<path>`。"""
    items = []
    for sha in commits:
        files = _zsplit(_git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", "--root",
                             "--diff-filter=ACMR", "-z", sha))
        for rel in files:
            text = _decode(_git_bytes(root, "show", f"{sha}:{rel}"))
            if text is not None:
                items.append((f"{sha[:7]}:{rel}", text))
    return items


def range_commits(root: str, spec: str) -> list[str]:
    return _git(root, "rev-list", "--no-merges", spec).split()


def unpushed_commits(root: str) -> list[str]:
    return _git(root, "rev-list", "--no-merges", "HEAD", "--not", "--remotes").split()


def push_commits_from_stdin(root: str, lines: list[str]) -> list[str]:
    """pre-push の stdin (`<local_ref> <local_sha> <remote_ref> <remote_sha>`) から検査対象コミットを集める。"""
    commits: list[str] = []
    for line in lines:
        parts = line.split()
        if len(parts) != 4:
            continue
        _local_ref, local_sha, _remote_ref, remote_sha = parts
        if ZERO_SHA.match(local_sha):
            continue  # ref の削除
        if ZERO_SHA.match(remote_sha):
            spec = [local_sha, "--not", "--remotes"]  # 新規ブランチ: どのリモートにも無いコミット
        else:
            spec = [f"{remote_sha}..{local_sha}"]
        commits += _git(root, "rev-list", "--no-merges", *spec).split()
    seen: set[str] = set()
    return [c for c in commits if not (c in seen or seen.add(c))]


# ---------- 判定 (identity-lint / local-path-lint と同じ) ----------

class Checker:
    def __init__(self, root: str):
        self.root = root
        self.settings = ID.Settings(root)
        self.excludes = LP.load_excludes(root)

    def check(self, label: str, text: str) -> list[str]:
        rel = label.split(":", 1)[1] if re.match(r"^[0-9a-f]{7}:", label) else label
        rel = LP.normalize_rel(rel, self.root)
        base = os.path.basename(rel)
        if base in (SELF_BASENAME, ID.SELF_BASENAME, LP.SELF_BASENAME):
            return []  # lint スクリプト自身には自己テストの違反例が載る
        hits: list[str] = []
        path_ok = not LP.is_excluded(rel, self.excludes)
        id_ok = self.settings.in_scope(rel)
        if not (path_ok or id_ok):
            return []
        changes_scope = ID.is_changes_scope(rel)
        for i, line in enumerate(text.splitlines(), 1):
            if path_ok and LP.is_violation(line):
                hits.append(f"{label}:{i}: ローカル絶対パス: {line.strip()[:120]}")
            if id_ok:
                found = ID.find_identities(line, self.settings, changes_scope)
                if found:
                    hits.append(f"{label}:{i}: {ID.describe(found)}")
        return hits


def check_items(root: str, items: list[tuple[str, str]]) -> list[str]:
    checker = Checker(root)
    hits: list[str] = []
    for label, text in items:
        hits += checker.check(label, text)
    return hits


GUIDE = ("直し方: 生ログは `python3 scripts/log-sanitize.py <file>` でプレースホルダに置換し、本文の実値は "
         "`<uuid>` `<email>` `/Users/<USER>/` 等に書き換える (ksn-core references/evidence.md, references/paths.md)。")


def report(hits: list[str], what: str) -> int:
    if not hits:
        return 0
    print(f"{what}に個体・個人・秘密を特定する値、またはローカル絶対パスが含まれています:")
    for h in hits[:30]:
        print("  " + h)
    if len(hits) > 30:
        print(f"  (他 {len(hits) - 30} 件)")
    print(GUIDE)
    return 1


# ---------- モード ----------

def mode_staged(root: str) -> int:
    return report(check_items(root, staged_items(root)), "ステージされた内容")


def mode_push(root: str, stdin_lines: list[str]) -> int:
    commits = push_commits_from_stdin(root, stdin_lines)
    return report(check_items(root, commit_items(root, commits)), "push 対象のコミット")


def mode_range(root: str, spec: str) -> int:
    return report(check_items(root, commit_items(root, range_commits(root, spec))), f"範囲 {spec} のコミット")


def bash_hook_decision(root: str, command: str) -> str | None:
    """Bash コマンドを見て、deny する理由を返す (問題なければ None)。"""
    is_commit = bool(RE_COMMIT.search(command))
    is_push = bool(RE_PUSH.search(command))
    if not (is_commit or is_push):
        return None
    if RE_NO_VERIFY.search(command) or RE_HOOKSPATH.search(command) or (is_commit and RE_COMMIT_N.search(command)):
        return ("git hook の迂回 (`--no-verify` / `-n` / core.hooksPath の上書き) はブロックします。"
                "pre-commit / pre-push の検査で止まった行を直してから、迂回オプション無しで実行してください。\n" + GUIDE)
    hits: list[str] = []
    if is_commit:
        items = staged_items(root)
        if RE_ADD.search(command):
            items += worktree_items(root)
        hits += check_items(root, items)
    if is_push:
        hits += check_items(root, commit_items(root, unpushed_commits(root)))
    if not hits:
        return None
    what = "commit" if is_commit and not is_push else ("push" if is_push and not is_commit else "commit / push")
    return (f"{what} しようとしている内容に個体・個人・秘密を特定する値、またはローカル絶対パスが含まれています。"
            "履歴に入る前に直してください。\n" + "\n".join(hits[:10])
            + (f"\n(他 {len(hits) - 10} 件)" if len(hits) > 10 else "") + "\n" + GUIDE)


def mode_bash_hook() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    command = str((data.get("tool_input") or {}).get("command") or "")
    if not command:
        return 0
    cwd = data.get("cwd") or os.getcwd()
    root = LP.repo_root(cwd)
    reason = bash_hook_decision(root, command)
    if reason is None:
        return 0
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))
    return 0


# ---------- 自己テスト ----------

def selftest() -> int:
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    def run(root: str, *args: str, stdin: str | None = None) -> subprocess.CompletedProcess:
        return subprocess.run([sys.executable, os.path.abspath(__file__), *args],
                              cwd=root, capture_output=True, text=True, input=stdin)

    def hook(root: str, command: str) -> str | None:
        payload = json.dumps({"tool_name": "Bash", "cwd": root, "tool_input": {"command": command}})
        out = run(root, "--bash-hook", stdin=payload).stdout.strip()
        if not out:
            return None
        return json.loads(out)["hookSpecificOutput"]["permissionDecisionReason"]

    print("[コマンド判定]")
    check(bool(RE_COMMIT.search('git commit -m "x"')), "git commit")
    check(bool(RE_COMMIT.search("git -C sub commit -am x")), "git -C dir commit")
    check(bool(RE_PUSH.search("git push -u origin main")), "git push")
    check(not RE_COMMIT.search("git log --oneline | grep commit"), "log の出力に commit が混じる行は対象外")
    check(bool(RE_COMMIT_N.search("git commit -n -m x")), "commit -n は迂回")
    check(bool(RE_COMMIT_N.search("git commit -anm x")), "commit -anm は迂回")
    check(not RE_COMMIT_N.search("git commit -m x && git push -n"), "push -n (dry-run) は迂回ではない")

    print("[一時リポジトリでの疎通]")
    with tempfile.TemporaryDirectory() as tmp:
        root = os.path.realpath(tmp)
        git = lambda *a, **k: subprocess.run(["git", *a], cwd=root, capture_output=True, text=True, **k)  # noqa: E731
        git("init", "-q", "-b", "main")
        git("config", "user.name", "t")
        git("config", "user.email", "t@example.com")
        os.makedirs(os.path.join(root, "scripts"))
        for f in (SELF_BASENAME, ID.SELF_BASENAME, LP.SELF_BASENAME):
            with open(os.path.join(HERE, f), "rb") as src, open(os.path.join(root, "scripts", f), "wb") as dst:
                dst.write(src.read())
        docs = os.path.join(root, "kasane", "changes", "x")
        os.makedirs(docs)
        with open(os.path.join(docs, "clean.md"), "w", encoding="utf-8") as f:
            f.write("問題のない本文\n")
        git("add", ".")
        git("commit", "-qm", "init")

        leak = os.path.join(docs, "log.md")
        with open(leak, "w", encoding="utf-8") as f:
            # 違反例。ローカルパスはリポジトリ全体の local-path-lint に拾われないよう分割して書く
            f.write("session 12345678-1234-1234-1234-1234567890AB を取得\ncd " + "/Users/" + "taro/proj\n")
        r = run(root, "--staged")
        check(r.returncode == 0, "未ステージの違反は --staged で止めない")
        reason = hook(root, 'git add -A && git commit -m "x"')
        check(reason is not None and "uuid" in reason and "ローカル絶対パス" in reason,
              "--bash-hook: add を含む commit は worktree の違反で deny")
        check(hook(root, 'git commit -m "x"') is None, "--bash-hook: add を含まない commit は未ステージの違反で止めない")

        git("add", "kasane/changes/x/log.md")
        r = run(root, "--staged")
        check(r.returncode == 1 and "uuid" in r.stdout and "ローカル絶対パス" in r.stdout, "--staged: ステージ違反で exit 1")
        with open(leak, "w", encoding="utf-8") as f:
            f.write("clean\n")
        r = run(root, "--staged")
        check(r.returncode == 1, "--staged: worktree を直しても add していなければ止める")
        check(hook(root, 'git commit -m "x"') is not None, "--bash-hook: ステージ違反の commit を deny")
        check(hook(root, 'git commit --no-verify -m "x"') is not None and "迂回" in hook(root, 'git commit --no-verify -m "x"'),
              "--bash-hook: --no-verify を deny")
        check(hook(root, "git -c core.hooksPath=/dev/null commit -m x") is not None, "--bash-hook: core.hooksPath 上書きを deny")
        check(hook(root, "git status") is None, "--bash-hook: commit / push 以外は素通し")

        hooks_dir = os.path.join(root, ".githooks")
        os.makedirs(hooks_dir)
        with open(os.path.join(hooks_dir, "pre-commit"), "w") as f:
            f.write(f'#!/bin/sh\nexec "{sys.executable}" scripts/{SELF_BASENAME} --staged\n')
        os.chmod(os.path.join(hooks_dir, "pre-commit"), 0o755)
        git("config", "core.hooksPath", ".githooks")
        r = git("commit", "-qm", "leak")
        check(r.returncode != 0, "pre-commit hook として登録した状態で commit が止まる")
        git("commit", "-qm", "leak", "--no-verify")
        head = git("rev-parse", "HEAD").stdout.strip()
        check(bool(head), "--no-verify で違反コミットを作成 (push 検査の前提)")

        r = run(root, "--range", "HEAD~1..HEAD")
        check(r.returncode == 1 and "uuid" in r.stdout, "--range: 範囲内コミットの blob 違反で exit 1")
        base = git("rev-parse", "HEAD~1").stdout.strip()
        r = run(root, "--push", stdin=f"refs/heads/main {head} refs/heads/main {base}\n")
        check(r.returncode == 1, "--push: 既存ブランチへの push 範囲で exit 1")
        r = run(root, "--push", stdin=f"refs/heads/main {head} refs/heads/main {'0' * 40}\n")
        check(r.returncode == 1, "--push: 新規ブランチ (remote sha ゼロ) でも検査する")
        r = run(root, "--push", stdin=f"refs/heads/main {'0' * 40} refs/heads/main {head}\n")
        check(r.returncode == 0, "--push: ref 削除は検査しない")
        check(hook(root, "git push origin main") is not None, "--bash-hook: 未 push の違反コミットを push で deny")

        git("reset", "-q", "--hard", "HEAD~1")
        check(hook(root, "git push origin main") is None, "--bash-hook: 違反コミットを取り除けば push は通る")
        r = run(root, "--staged")
        check(r.returncode == 0 and r.stdout == "", "--staged: ステージが空なら無音で exit 0")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    if "--bash-hook" in argv:
        return mode_bash_hook()
    if "--selftest" in argv:
        return selftest()
    root = LP.repo_root()
    if "--staged" in argv:
        return mode_staged(root)
    if "--push" in argv:
        return mode_push(root, sys.stdin.read().splitlines())
    if "--range" in argv:
        i = argv.index("--range")
        if i + 1 >= len(argv):
            print("--range には A..B の形で範囲を指定してください", file=sys.stderr)
            return 2
        return mode_range(root, argv[i + 1])
    print(__doc__.split("使い方:")[1].split("\n\n")[0].strip(), file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
