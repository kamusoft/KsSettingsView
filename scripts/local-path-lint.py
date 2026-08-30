#!/usr/bin/env python3
"""ローカル絶対パスの検査 (Kasane 標準 lint / hook)。

追跡ファイルに `/Users/<名前>/` `/Volumes/<名前>` `C:\\Users\\<名前>\\` のような
ローカル環境の絶対パスが書き込まれるのを防ぐ。規約は ksn-core references/paths.md。

使い方:
  python3 scripts/local-path-lint.py            # lint: 追跡ファイル全体を検査 (CI 用。違反があれば exit 1)
  python3 scripts/local-path-lint.py --hook     # hook: PreToolUse の stdin JSON を検査し、違反があれば deny を返す
  python3 scripts/local-path-lint.py --paths a b  # 指定ファイルだけ lint

判定ロジック (is_violation) は両モードで共通。lint の走査は `git grep` で候補行を絞ってから判定する。

除外 (違反にしない):
  - プレースホルダ: `/Users/<USER>/` `/Volumes/<name>` `${HOME}` `$USER` 等、名前部分が実名でないもの
  - 省略例示: `/Users/.../` `/Volumes/...`
  - kasane/config.yaml の `lint.exclude` に列挙したリポジトリ相対パス (先頭セグメント一致、または fnmatch glob)

兄弟スクリプト (identity-lint.py / log-sanitize.py) はこのファイルの共通ヘルパ (repo_root / normalize_rel /
load_config / texts_from_hook_input) を importlib で読み込んで使う。

検査対象パスはリポジトリ相対に正規化し、worktree プレフィックス (`.claude/worktrees/<name>/`) を剥がしてから
除外判定する (`.claude/` を丸ごと除外すると worktree 配下が無検査になるため)。
"""

from __future__ import annotations

import fnmatch
import json
import os
import re
import subprocess
import sys

# 名前部分: 実名として扱う文字集合。`<` `$` `{` `.` で始まるものはプレースホルダ・例示として除外
NAME = r"[A-Za-z0-9][A-Za-z0-9._-]*"
PATTERNS = [
    re.compile(r"/Users/" + NAME + r"/"),
    re.compile(r"/Volumes/" + NAME),
    re.compile(r"[A-Za-z]:\\\\?Users\\\\?" + NAME),
]
# git grep 用の粗い候補パターン (判定は PATTERNS で行う)
GREP_PATTERN = r"/Users/|/Volumes/|[A-Za-z]:\\\\?Users"
WORKTREE_PREFIX = re.compile(r"^\.claude/worktrees/[^/]+/")


def is_violation(line: str) -> bool:
    """1 行がローカル絶対パスを含むか (除外規則適用後)。"""
    return any(p.search(line) for p in PATTERNS)


def repo_root(cwd: str | None = None) -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            cwd=cwd, capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or (cwd or os.getcwd())
    except Exception:
        return cwd or os.getcwd()


def normalize_rel(path: str, root: str) -> str:
    """リポジトリ相対パスに正規化し、worktree プレフィックスを剥がす。"""
    if os.path.isabs(path):
        try:
            rel = os.path.relpath(path, root)
        except ValueError:
            rel = path
    else:
        rel = path
    rel = rel.replace(os.sep, "/")
    return WORKTREE_PREFIX.sub("", rel)


def _strip_comment(line: str) -> str:
    """行末コメントを落とす (引用符内の # は保持)。"""
    out = []
    quote = None
    for i, ch in enumerate(line):
        if quote:
            out.append(ch)
            if ch == quote:
                quote = None
            continue
        if ch in ("'", '"'):
            quote = ch
            out.append(ch)
            continue
        if ch == "#" and (i == 0 or line[i - 1] in " \t"):
            break
        out.append(ch)
    return "".join(out).rstrip()


def _scalar(v: str):
    v = v.strip()
    if v.startswith("[") and v.endswith("]"):
        inner = v[1:-1].strip()
        return [_scalar(x) for x in inner.split(",")] if inner else []
    if len(v) >= 2 and v[0] == v[-1] and v[0] in "'\"":
        return v[1:-1]
    if v.lower() in ("true", "false"):
        return v.lower() == "true"
    return v


def load_config(root: str) -> dict:
    """kasane/config.yaml を最小サブセットで読む (PyYAML 非依存)。

    対応: インデントによるネスト、`key: value`、`key: [a, b]`、`key:` + `- item` のブロックリスト、
    引用符付きスカラー、コメント。アンカー・複数行スカラー等は非対応 (config.yaml では使わない)。
    """
    cfg = os.path.join(root, "kasane", "config.yaml")
    if not os.path.isfile(cfg):
        return {}
    rootd: dict = {}
    stack: list[tuple[int, object]] = [(-1, rootd)]  # (indent, container)
    pending: tuple[dict, str, int] | None = None     # 値未確定のキー (次行でリスト/辞書が決まる): (親, キー, キー行のインデント)
    with open(cfg, encoding="utf-8") as f:
        for raw in f:
            line = _strip_comment(raw.rstrip("\n"))
            if not line.strip():
                continue
            indent = len(line) - len(line.lstrip(" "))
            body = line.strip()
            while stack and stack[-1][0] >= indent and len(stack) > 1:
                stack.pop()
            parent = stack[-1][1]
            if body.startswith("- "):
                item = body[2:]
                if pending is not None and pending[0] is parent and indent > pending[2]:
                    lst: list = []
                    pending[0][pending[1]] = lst
                    stack.append((pending[2], lst))
                    parent = lst
                    pending = None
                if isinstance(parent, list):
                    parent.append(_scalar(item))
                continue
            if ":" not in body:
                continue
            key, _, val = body.partition(":")
            key = key.strip()
            if pending is not None and pending[0] is parent and indent > pending[2]:
                d: dict = {}
                pending[0][pending[1]] = d
                stack.append((pending[2], d))
                parent = d
                pending = None
            if not isinstance(parent, dict):
                continue
            if val.strip() == "":
                parent[key] = None
                pending = (parent, key, indent)
            else:
                parent[key] = _scalar(val)
    return rootd


def config_get(cfg: dict, dotted: str, default=None):
    cur: object = cfg
    for part in dotted.split("."):
        if not isinstance(cur, dict) or part not in cur:
            return default
        cur = cur[part]
    return default if cur is None else cur


def load_excludes(root: str) -> list[str]:
    """lint.exclude (旧 paths.lint-exclude も読む)。"""
    cfg = load_config(root)
    ex = config_get(cfg, "lint.exclude")
    if ex is None:
        ex = config_get(cfg, "paths.lint-exclude", [])
    return [str(x) for x in (ex if isinstance(ex, list) else [ex])]


def is_excluded(rel: str, excludes: list[str]) -> bool:
    first = rel.split("/", 1)[0]
    for pat in excludes:
        pat = pat.rstrip("/")
        if rel == pat or first == pat or rel.startswith(pat + "/") or fnmatch.fnmatch(rel, pat):
            return True
    return False


# ---------- lint モード ----------

def lint(root: str, paths: list[str] | None) -> int:
    excludes = load_excludes(root)
    cmd = ["git", "grep", "--untracked", "-nI", "-E", GREP_PATTERN, "--"]
    cmd += paths if paths else ["."]
    proc = subprocess.run(cmd, cwd=root, capture_output=True, text=True)
    if proc.returncode not in (0, 1):
        sys.stderr.write(proc.stderr)
        return 2
    violations = []
    for entry in proc.stdout.splitlines():
        try:
            path, lineno, text = entry.split(":", 2)
        except ValueError:
            continue
        rel = normalize_rel(path, root)
        if is_excluded(rel, excludes):
            continue
        if is_violation(text):
            violations.append(f"{rel}:{lineno}: {text.strip()}")
    if violations:
        print("ローカル絶対パスが含まれています (ksn-core references/paths.md):")
        for v in violations:
            print("  " + v)
        return 1
    return 0


# ---------- hook モード ----------

PATCH_FILE_RE = re.compile(r"^\*\*\* (?:Add|Update|Delete) File: (.+)$")


def texts_from_hook_input(data: dict) -> list[tuple[str, str]]:
    """PreToolUse 入力から (対象パス, 検査する本文) の組を取り出す。Claude Code / codex 両対応。"""
    ti = data.get("tool_input") or {}
    tool = data.get("tool_name", "")
    out: list[tuple[str, str]] = []
    fp = ti.get("file_path") or ti.get("notebook_path") or ""
    if "content" in ti:
        out.append((fp, str(ti["content"])))
    if "new_string" in ti:
        out.append((fp, str(ti["new_string"])))
    if "new_source" in ti:
        out.append((fp, str(ti["new_source"])))
    for e in ti.get("edits") or []:
        out.append((fp, str(e.get("new_string", ""))))
    if tool == "apply_patch" or (not out and "command" in ti and "*** Begin Patch" in str(ti.get("command", ""))):
        current = ""
        added: dict[str, list[str]] = {}
        for line in str(ti.get("command", "")).splitlines():
            m = PATCH_FILE_RE.match(line)
            if m:
                current = m.group(1).strip()
                added.setdefault(current, [])
                continue
            if line.startswith("+") and not line.startswith("+++"):
                added.setdefault(current, []).append(line[1:])
        for path, lines in added.items():
            out.append((path, "\n".join(lines)))
    return out


def hook() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    cwd = data.get("cwd") or os.getcwd()
    root = repo_root(cwd)
    excludes = load_excludes(root)
    hits: list[str] = []
    for path, text in texts_from_hook_input(data):
        rel = normalize_rel(path, root) if path else ""
        if rel and is_excluded(rel, excludes):
            continue
        for i, line in enumerate(text.splitlines(), 1):
            if is_violation(line):
                hits.append(f"{rel or '(本文)'}:{i}: {line.strip()[:120]}")
    if not hits:
        return 0
    reason = (
        "ローカル絶対パス (/Users/<名前>/ 等) の書き込みはブロックします。"
        "リポジトリ相対パス・`../<リポジトリ名>/`・`~/` で書き直してください (ksn-core references/paths.md)。\n"
        + "\n".join(hits[:10])
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))
    return 0


def main(argv: list[str]) -> int:
    if "--hook" in argv:
        return hook()
    paths: list[str] | None = None
    if "--paths" in argv:
        paths = argv[argv.index("--paths") + 1:]
    return lint(repo_root(), paths)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
