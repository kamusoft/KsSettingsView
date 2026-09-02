#!/usr/bin/env python3
"""ソースコメント規約の検査 (Kasane 標準 lint / hook。opt-in)。

ソースコードのコメントに、アーカイブされる作業文書への参照 (kasane/ 配下のパス・
レビュー通番・タスク通番等) やデルタスペック構文キーワードが混入するのを防ぐ。
規約の全文は kasane/handbook/**/comment-policy.md (配布元は ksn-init templates/)。

使い方:
  python3 scripts/comment-policy-lint.py            # lint: 追跡 + 未追跡の対象ソースを検査 (違反があれば exit 1)
  python3 scripts/comment-policy-lint.py --hook     # hook: PreToolUse の stdin JSON を検査し、新規混入があれば deny を返す
  python3 scripts/comment-policy-lint.py --paths a b  # 指定パスだけ lint
  python3 scripts/comment-policy-lint.py --summary    # ファイル単位の件数だけ表示
  python3 scripts/comment-policy-lint.py --advisory   # 履歴記述などの要確認 (advisory) 類型も表示
  python3 scripts/comment-policy-lint.py --selftest   # 検出ロジック・列挙・hook の疎通確認

検出は 2 段階:
  BLOCKING  機械的に一意判定できる禁止参照。hook はこれの新規混入を拒否する
  ADVISORY  履歴記述など文脈依存で誤検知しうる類型。報告のみで止めない

hook は**ラチェット方式**: その書き込みで新しく増えるコメント行だけを検査する。
書き込み前から在った違反は既存債務として見逃すため、違反を抱えたファイルの編集は
巻き添えでは止まらない。誤検知の行には ``comment-policy:allow`` を書き添えると除外できる。

kasane/config.yaml の `lint.comment-policy:` で調整する (節の定義が opt-in の宣言):
  comment-policy:
    exclude: []   # 追加の対象外 (先頭セグメント or glob)。kasane/ .claude/ build/ .git/ は常に対象外
    ext: []       # 検査対象に追加する拡張子

共通ヘルパ (repo_root / normalize_rel / load_config / config_get / is_excluded) は
local-path-lint.py から importlib で読み込む (同じディレクトリに置く)。
"""

from __future__ import annotations

import importlib.util
import json
import os
import re
import subprocess
import sys


def _load_sibling():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-path-lint.py")
    spec = importlib.util.spec_from_file_location("local_path_lint", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load_sibling()

# 検査対象 = コメント構文を持つソースファイル (config `lint.comment-policy.ext` で追加できる)
DEFAULT_TARGET_EXT = {
    ".swift", ".kt", ".kts", ".cs", ".java",
    ".xml", ".xaml", ".axml", ".gradle", ".pro",
}

# 常に対象外の先頭セグメント (kasane/ は独立した規約体系、残りは生成物・メタデータ)
BUILTIN_EXCLUDE = ["kasane", ".claude", "build", ".git"]

# 誤検知を個別に無効化する行内マーカー
ALLOW_MARKER = "comment-policy:allow"

# 書き込みを拒否する禁止参照。(正規表現, 説明) の組
BLOCKING_PATTERNS = [
    (r"(?:^|[^\w/])kasane/", "作業文書 (kasane/ 配下) のパス参照"),
    (r"(?:^|[^\w/])openspec/", "凍結済み OpenSpec 文書のパス参照"),
    (r"(?:Phase|Round|Decision)\s*[0-9]", "議論プロセスのローカル通番"),
    (r"論点\s*[0-9]", "議論プロセスのローカル通番"),
    (r"(?:Critical|Major|Minor)\s*-\s*[0-9]", "レビュー指摘の通番"),
    (r"review-(?:result|[0-9])", "レビュー文書への参照"),
    (r"(?:proposal|design|tasks|brief|spec)\.md", "変更アーティファクト文書への参照"),
    (r"\bspec\s+L[0-9]", "アーカイブ文書の行番号参照"),
    (r"delta\s+spec|デルタスペック", "デルタスペックへの裸参照"),
    (r"\b(?:Requirement|Scenario)\b", "デルタスペックの構造名の裸参照"),
    (r"承認モック|approved\.png", "承認モックへの参照"),
    (r"\b(?:MUST|SHALL|SHOULD|MAY)(?:\s+NOT)?\b", "デルタスペック構文キーワード"),
    (r"案\s*[αβγδ]", "議論プロセスの仮称"),
    # tasks.md のタスク通番。単層 (`タスク 5`) と階層 (`タスク 2.4`) の双方を見る。
    # 直後に数字を要求するので一般語用法 (「タスクを 3 回実行する」) は当たらない
    (r"(?:タスク|[Tt]ask|TASK)\s*[0-9]+(?:\.[0-9]+)*", "変更アーティファクト内のタスク通番"),
]

# 報告のみ行う類型。文脈次第で正当な記述もありうるため書き込みは止めない
ADVISORY_PATTERNS = [
    (r"旧(?:実装|方式|仕様|API|設計|バージョン)", "履歴記述 (現在形の仕様説明に書き換える)"),
    (r"全面刷新|から移植|へ移行|を撤去|撤去した|廃止された|だった", "履歴記述 (現在形の仕様説明に書き換える)"),
]

_BLOCKING = [(re.compile(p), label) for p, label in BLOCKING_PATTERNS]
_ADVISORY = [(re.compile(p), label) for p, label in ADVISORY_PATTERNS]

# 行内の文字列リテラル (コメント判定を誤らせるため先に潰す)
_STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"' r"|'(?:\\.|[^'\\])*'")

# 規約が許容する URL。パターン照合の前に取り除く
_URL_RE = re.compile(r"\bhttps?://\S+")

_XML_EXT = {".xml", ".xaml", ".axml"}
_HASH_EXT = {".pro"}


# ---------- 設定 ----------

def load_policy(root: str) -> tuple[set[str], list[str]]:
    """(対象拡張子, 対象外パターン) を config から解決する。"""
    cfg = LP.load_config(root)
    ext = {str(e).lower() for e in LP.config_get(cfg, "lint.comment-policy.ext", []) or []}
    ext = DEFAULT_TARGET_EXT | {e if e.startswith(".") else "." + e for e in ext}
    excludes = BUILTIN_EXCLUDE + [str(x) for x in LP.config_get(cfg, "lint.comment-policy.exclude", []) or []]
    excludes += LP.load_excludes(root)
    return ext, excludes


# ---------- コメント抽出 ----------

def _strip_strings(line: str) -> str:
    """文字列リテラルの中身を空白で潰す。`"http://..."` を行コメントと誤認しないため。

    後段でコメント開始位置を元の行に対する添字として使うので、長さは保存する。
    """
    return _STRING_RE.sub(lambda m: m.group(0)[0] + " " * (len(m.group(0)) - 2) + m.group(0)[-1], line)


def extract_comments(text: str, ext: str):
    """(行番号, コメント本文) の列を返す。行番号は 1 始まり。"""
    if ext in _XML_EXT:
        yield from _extract_xml(text)
    elif ext in _HASH_EXT:
        for i, line in enumerate(text.splitlines(), 1):
            if line.lstrip().startswith("#"):
                yield i, line.strip()
    else:
        yield from _extract_c_like(text)


def _extract_xml(text: str):
    in_comment = False
    for i, line in enumerate(text.splitlines(), 1):
        rest = line
        buf = []
        while rest:
            if in_comment:
                end = rest.find("-->")
                if end < 0:
                    buf.append(rest)
                    rest = ""
                else:
                    buf.append(rest[:end])
                    rest = rest[end + 3 :]
                    in_comment = False
            else:
                start = rest.find("<!--")
                if start < 0:
                    break
                rest = rest[start + 4 :]
                in_comment = True
        if buf:
            yield i, " ".join(s.strip() for s in buf).strip()


def _extract_c_like(text: str):
    """Swift / Kotlin / Java / C# / Gradle 系のコメントを取り出す。

    ブロックコメント (/* */) と Kotlin / Swift / C# の複数行文字列 (\"\"\") の
    継続状態を持ち越して判定する。
    """
    in_block = False
    in_raw_string = False
    for i, line in enumerate(text.splitlines(), 1):
        if in_raw_string:
            if '"""' in line:
                in_raw_string = False
            continue
        if in_block:
            end = line.find("*/")
            if end < 0:
                yield i, line.strip()
                continue
            yield i, line[:end].strip()
            line = line[end + 2 :]
            in_block = False

        scan = _strip_strings(line)
        # 複数行文字列の開始 (対応する終了が同一行にない場合)
        triple = scan.count('"""')
        if triple % 2 == 1:
            in_raw_string = True

        block_start = scan.find("/*")
        line_start = scan.find("//")
        if block_start >= 0 and (line_start < 0 or block_start < line_start):
            end = scan.find("*/", block_start + 2)
            if end < 0:
                in_block = True
                yield i, line[block_start:].strip()
            else:
                yield i, line[block_start : end + 2].strip()
        elif line_start >= 0:
            yield i, line[line_start:].strip()


# ---------- 判定 ----------

def scan_text(text: str, ext: str):
    """テキストを走査して (種別, 行番号, 説明, 該当行) の列を返す。

    種別は "blocking" / "advisory"。
    """
    findings = []
    for lineno, comment in extract_comments(text, ext):
        if not comment or ALLOW_MARKER in comment:
            continue
        # URL は規約が許容する参照なので照合対象から外す
        target = _URL_RE.sub(" ", comment)
        for regex, label in _BLOCKING:
            if regex.search(target):
                findings.append(("blocking", lineno, label, comment[:160]))
                break
        else:
            for regex, label in _ADVISORY:
                if regex.search(target):
                    findings.append(("advisory", lineno, label, comment[:160]))
                    break
    return findings


# ---------- lint モード ----------

def target_files(root: str, paths: list[str], ext: set[str], excludes: list[str]) -> list[str]:
    # 追跡中に加えて未追跡 (ignore 済みは除く) も列挙し、新規ファイルの素通りを防ぐ
    files = []
    for extra in ([], ["--others", "--exclude-standard"]):
        cmd = ["git", "-C", root, "ls-files", "-z", *extra] + paths
        out = subprocess.run(cmd, capture_output=True, text=True, check=True)
        for rel in out.stdout.split("\0"):
            if not rel:
                continue
            if os.path.splitext(rel)[1].lower() not in ext:
                continue
            if LP.is_excluded(LP.normalize_rel(rel, root), excludes):
                continue
            files.append(rel)
    # unmerged path はステージごとに重複して返るため排除し、順序も安定させる
    return sorted(dict.fromkeys(files))


def lint(root: str, paths: list[str], summary: bool = False, advisory: bool = False) -> int:
    ext_set, excludes = load_policy(root)
    files = target_files(root, paths, ext_set, excludes)

    total_blocking = 0
    total_advisory = 0
    dirty_files = 0

    for rel in files:
        try:
            with open(os.path.join(root, rel), encoding="utf-8") as f:
                text = f.read()
        except (OSError, UnicodeDecodeError):
            continue
        findings = scan_text(text, os.path.splitext(rel)[1].lower())
        if not advisory:
            findings = [f for f in findings if f[0] == "blocking"]
        if not findings:
            continue
        dirty_files += 1
        blocking = sum(1 for f in findings if f[0] == "blocking")
        total_blocking += blocking
        total_advisory += len(findings) - blocking
        if summary:
            label = f"{blocking} 件"
            if len(findings) - blocking:
                label += f" (+ 要確認 {len(findings) - blocking} 件)"
            print(f"{rel}: {label}")
            continue
        print(f"\n{rel}")
        for kind, lineno, reason, line in findings:
            mark = "禁止" if kind == "blocking" else "要確認"
            print(f"  {rel}:{lineno}: [{mark}] {reason}")
            print(f"      {line}")

    print(
        f"\n合計: {dirty_files} ファイル / 禁止 {total_blocking} 件"
        + (f" / 要確認 {total_advisory} 件" if advisory else "")
        + f" (検査対象 {len(files)} ファイル)"
    )
    return 1 if total_blocking else 0


# ---------- hook モード ----------

def _hook_items(data: dict, root: str) -> list[tuple[str, str, str]]:
    """PreToolUse 入力から (対象パス, 書き込み前の基準本文, 書き込む本文) の組を取り出す。

    Claude Code (Write / Edit / MultiEdit) と codex (apply_patch) の両対応。
    基準本文は、Edit 系では old_string、全置換 (Write) と apply_patch では
    ディスク上の現在の内容 — そこに既にあるコメントはラチェットで見逃す。
    """
    tool = data.get("tool_name", "")
    ti = data.get("tool_input") or {}
    path = ti.get("file_path") or ""

    def disk(p: str) -> str:
        full = p if os.path.isabs(p) else os.path.join(root, p)
        try:
            with open(full, encoding="utf-8") as f:
                return f.read()
        except (OSError, UnicodeDecodeError):
            return ""

    if tool == "Edit":
        return [(path, ti.get("old_string") or "", ti.get("new_string") or "")]
    if tool == "MultiEdit":
        edits = ti.get("edits") or []
        base = "\n".join(e.get("old_string") or "" for e in edits)
        new = "\n".join(e.get("new_string") or "" for e in edits)
        return [(path, base, new)]
    if tool == "Write":
        return [(path, disk(path), ti.get("content") or "")]
    command = str(ti.get("command", ""))
    if tool == "apply_patch" or "*** Begin Patch" in command:
        items = []
        current = ""
        added: dict[str, list[str]] = {}
        for line in command.splitlines():
            m = re.match(r"^\*\*\* (?:Add|Update) File: (.+)$", line)
            if m:
                current = m.group(1).strip()
                added.setdefault(current, [])
                continue
            if current and line.startswith("+") and not line.startswith("+++"):
                added[current].append(line[1:])
        for p, lines in added.items():
            items.append((p, disk(p), "\n".join(lines)))
        return items
    return []


def hook() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception:
        return 0
    cwd = data.get("cwd") or os.getcwd()
    root = LP.repo_root(cwd)
    ext_set, excludes = load_policy(root)

    hits: list[str] = []
    for path, base, new in _hook_items(data, root):
        ext = os.path.splitext(path)[1].lower()
        if not new or ext not in ext_set:
            continue
        rel = LP.normalize_rel(path, root)
        if LP.is_excluded(rel, excludes):
            continue
        findings = [f for f in scan_text(new, ext) if f[0] == "blocking"]
        if not findings:
            continue
        # ラチェット: 書き込み前から在ったコメント行は既存債務として見逃す
        baseline = {c for _, c in extract_comments(base, ext)}
        for _, _lineno, reason, line in findings:
            if line not in baseline:
                hits.append(f"{rel}: {reason}: {line[:120]}")
    if not hits:
        return 0
    reason = (
        "コメントへの禁止参照の新規追加はブロックします。コメントは、そのファイルだけを読む人に意味が通る形で書いてください。\n"
        + "\n".join("  - " + h for h in hits[:10])
        + (f"\n  - (他 {len(hits) - 10} 件)" if len(hits) > 10 else "")
        + "\n"
        "  - 参照が装飾なら行ごと削除 / 設計理由と一体なら `<domain>/ADR-NNNN` へ置換、対応 ADR が無ければ自己完結する説明に書き直す\n"
        "  - MUST / SHOULD 等は自然な日本語 (「〜する」「〜してはいけない」) に直す\n"
        "規約全文: kasane/handbook/ の comment-policy.md。"
        f"誤検知の場合のみ、その行に {ALLOW_MARKER} を書き添えれば除外されます。"
    )
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": "deny",
            "permissionDecisionReason": reason,
        }
    }, ensure_ascii=False))
    return 0


# ---------- 自己テスト ----------

# (説明, ソース, 期待する禁止件数)
SELFTEST_CASES = [
    ("許容: ADR 参照", "// 判断の根拠は cross/ADR-0007 に従う\n", 0),
    ("許容: URL", "// 詳細は https://example.com/changes/design.md を参照\n", 0),
    ("許容: 恒常規格", "// RFC 6749 のトークン形式に合わせる\n", 0),
    ("許容: 文字列リテラル内の禁止語", 'val s = "kasane/changes/foo/spec.md"\n', 0),
    ("許容: 一般語のタスク", "// バックグラウンドでタスクを実行する\n", 0),
    ("許容: allow マーカー", "// kasane/changes/foo/spec.md comment-policy:allow\n", 0),
    ("禁止: 作業文書パス", "// 仕様: kasane/changes/foo/specs/spec.md\n", 1),
    ("禁止: ロードマップ文書パス", "// 経緯: kasane/roadmaps/foo/phases/p1/history.md\n", 1),
    ("禁止: openspec パス", "// 旧仕様: openspec/specs/cell-types/spec.md\n", 1),
    ("禁止: 議論通番", "// 設計判断は Decision 5 に従う\n", 1),
    ("禁止: タスク通番", "// タスク 2.4 で導入した検査\n", 1),
    ("禁止: 構文キーワード", "// この関数は MUST NOT be called twice\n", 1),
    ("禁止: 構造名の裸参照", '/* "テーブル初期化" Requirement を満たす */\n', 1),
    ("禁止: ブロックコメント継続行", "/**\n * 仕様: kasane/changes/foo/spec.md\n */\n", 1),
]

# hook のラチェット動作を確認するケース: (説明, 基準 old_string, 書き込む new_string, deny を期待するか)
HOOK_CASES = [
    ("新規混入は拒否", "", "// 仕様: kasane/changes/foo/spec.md\n", True),
    ("既存行の持ち越しは許可", "// 仕様: kasane/changes/foo/spec.md\n",
     "// 仕様: kasane/changes/foo/spec.md\nval x = 1\n", False),
]


def selftest() -> int:
    import contextlib
    import io
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    print("[検出ロジック]")
    for name, src, expected in SELFTEST_CASES:
        actual = len([f for f in scan_text(src, ".kt") if f[0] == "blocking"])
        check(actual == expected, name, f"期待 {expected} / 実際 {actual}")

    with tempfile.TemporaryDirectory() as tmp:
        tmp = os.path.realpath(tmp)
        subprocess.run(["git", "init", "-q"], cwd=tmp, check=True, capture_output=True)
        os.makedirs(os.path.join(tmp, "src"))
        with open(os.path.join(tmp, "src", "A.kt"), "w", encoding="utf-8") as f:
            f.write("// 仕様: kasane/changes/foo/spec.md\nval a = 1\n")
        os.makedirs(os.path.join(tmp, "vendor"))
        with open(os.path.join(tmp, "vendor", "B.kt"), "w", encoding="utf-8") as f:
            f.write("// 仕様: kasane/changes/foo/spec.md\n")
        os.makedirs(os.path.join(tmp, "kasane"))
        with open(os.path.join(tmp, "kasane", "config.yaml"), "w", encoding="utf-8") as f:
            f.write("lint:\n  comment-policy:\n    exclude:\n      - vendor\n")

        print("[lint 疎通 (未追跡ファイルの列挙・config 除外)]")
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = lint(tmp, [])
        out = buf.getvalue()
        check("src/A.kt" in out, "未追跡の新規ファイルが走査対象に入る")
        check(code == 1, "違反ありで exit 1")
        check("vendor/B.kt" not in out, "comment-policy.exclude のパスは検査しない")

        print("[hook 疎通]")
        # worktree 配下のパスでも検査されること (対象外の除外に巻き込まれないこと) を含めて確認する
        variants = [
            ("通常", os.path.join(tmp, "src", "New.kt")),
            ("worktree", os.path.join(tmp, ".claude", "worktrees", "wt", "src", "New.kt")),
        ]
        for label, old, new, expect_deny in HOOK_CASES:
            for where, target in variants:
                payload = {"tool_name": "Edit", "cwd": tmp,
                           "tool_input": {"file_path": target, "old_string": old, "new_string": new}}
                proc = subprocess.run([sys.executable, os.path.abspath(__file__), "--hook"],
                                      input=json.dumps(payload), capture_output=True, text=True)
                denied = '"deny"' in proc.stdout
                check(proc.returncode == 0 and denied == expect_deny, f"{label} ({where})",
                      f"期待 deny={expect_deny} / 実際 deny={denied}")
        for label, target, expect_deny in [
            ("規約対象外パスは検査しない", os.path.join(tmp, "kasane", "SelfTest.kt"), False),
            ("config 除外パスは検査しない", os.path.join(tmp, "vendor", "New.kt"), False),
        ]:
            payload = {"tool_name": "Edit", "cwd": tmp,
                       "tool_input": {"file_path": target, "old_string": "",
                                      "new_string": "// 仕様: kasane/changes/foo/spec.md\n"}}
            proc = subprocess.run([sys.executable, os.path.abspath(__file__), "--hook"],
                                  input=json.dumps(payload), capture_output=True, text=True)
            denied = '"deny"' in proc.stdout
            check(proc.returncode == 0 and denied == expect_deny, label,
                  f"期待 deny={expect_deny} / 実際 deny={denied}")
        # codex apply_patch: 既存ファイル (src/A.kt) への追記。既存違反は見逃し、新規追加行だけ止める
        patch = ("*** Begin Patch\n*** Update File: src/A.kt\n"
                 "+// レビュー指摘 Major-1 の対応\n*** End Patch")
        payload = {"tool_name": "apply_patch", "cwd": tmp, "tool_input": {"command": patch}}
        proc = subprocess.run([sys.executable, os.path.abspath(__file__), "--hook"],
                              input=json.dumps(payload), capture_output=True, text=True)
        check(proc.returncode == 0 and '"deny"' in proc.stdout, "apply_patch の新規混入は拒否")
        patch_ok = ("*** Begin Patch\n*** Update File: src/A.kt\n"
                    "+val b = 2\n*** End Patch")
        payload = {"tool_name": "apply_patch", "cwd": tmp, "tool_input": {"command": patch_ok}}
        proc = subprocess.run([sys.executable, os.path.abspath(__file__), "--hook"],
                              input=json.dumps(payload), capture_output=True, text=True)
        check(proc.returncode == 0 and '"deny"' not in proc.stdout, "apply_patch の無害な追記は許可")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return 1 if failures else 0


# ---------- エントリポイント ----------

def main(argv: list[str]) -> int:
    if "--hook" in argv:
        return hook()
    if "--selftest" in argv:
        return selftest()
    summary = "--summary" in argv
    advisory = "--advisory" in argv
    paths: list[str] = []
    if "--paths" in argv:
        paths = [a for a in argv[argv.index("--paths") + 1:] if not a.startswith("--")]
    else:
        paths = [a for a in argv if not a.startswith("--")]
    return lint(LP.repo_root(), paths, summary=summary, advisory=advisory)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
