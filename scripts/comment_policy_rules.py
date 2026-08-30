"""ソースコメント規約 (kasane/concepts/cross/conventions/comment-policy.md) の検出ロジック。

リポジトリ全体スキャン (scripts/comment-policy-lint.py) と
書き込み前の PreToolUse hook (.claude/hooks/comment-policy-check.py) の両方から使う。
検出するのは機械的に一意に判定できる禁止参照だけで、hook はこれを検出すると書き込みを拒否する。
履歴記述のように文脈依存で誤検知しうる類型は検出対象にしない (レビューで見る)。

誤検知に当たった行には行内に ``comment-policy:allow`` を書くと除外できる。
"""

from __future__ import annotations

import os
import re

# 検査対象 = コメント構文を持つソースファイル (規約の適用範囲に準拠)
TARGET_EXT = {".swift", ".kt", ".kts", ".cs", ".java", ".xml", ".gradle", ".pro"}

# 規約の対象外ディレクトリ (リポジトリルートからの第 1 セグメントで判定する)
EXCLUDE_TOP_DIRS = {"openspec", "kasane", "docs", ".claude", "build", ".git"}

# 誤検知を個別に無効化する行内マーカー
ALLOW_MARKER = "comment-policy:allow"

# 書き込みを拒否する禁止参照。(正規表現, 説明) の組
BLOCKING_PATTERNS = [
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
    # 「タスク」単体は Gradle のタスクやバックグラウンド処理の意味でも使うが、
    # 直後に数字を要求する時点で一般語用法 (「タスクを 3 回実行する」) とは分離できる。
    (r"(?:タスク|[Tt]ask|TASK)\s*[0-9]+(?:\.[0-9]+)*", "変更アーティファクト内のタスク通番"),
]

_BLOCKING = [(re.compile(p), label) for p, label in BLOCKING_PATTERNS]

# 行内の文字列リテラル (コメント判定を誤らせるため先に潰す)
_STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"' r"|'(?:\\.|[^'\\])*'")

# 規約が許容する URL。パターン照合の前に取り除く
_URL_RE = re.compile(r"\bhttps?://\S+")

_XML_EXT = {".xml"}
_HASH_EXT = {".pro"}


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

    ブロックコメント (/* */) と Kotlin / Swift の複数行文字列 (\"\"\") の
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


def is_excluded(rel_path: str) -> bool:
    """リポジトリルートからの相対パスが規約の対象外かを返す。"""
    head = rel_path.replace(os.sep, "/").lstrip("/").split("/", 1)[0]
    return head in EXCLUDE_TOP_DIRS


def to_repo_relative(path: str, project_dir: str | None = None) -> str:
    """絶対パスをリポジトリルートからの相対パスへ正規化する。

    git worktree (`.claude/worktrees/<name>/`) 配下はワークツリールート基準に直す。
    ここを取り違えると worktree 内の編集が丸ごと対象外になる。
    """
    p = path.replace(os.sep, "/")
    # マーカー本体は先読みに置く。消費すると区切りの "/" が食われ、
    # `.claude/worktrees/a/.claude/worktrees/b/` のような連続出現の 2 個目を見落とす
    last_end = None
    for m in re.finditer(r"(?:^|/)(?=(\.claude/worktrees/[^/]+/))", p):
        last_end = m.end(1)
    if last_end is not None:
        return p[last_end:]
    root = (project_dir or os.environ.get("CLAUDE_PROJECT_DIR") or "").replace(os.sep, "/").rstrip("/")
    if root and p.startswith(root + "/"):
        return p[len(root) + 1 :]
    return p.lstrip("/")


def scan_text(text: str, ext: str):
    """テキストを走査して (行番号, 説明, 該当行) の列を返す。"""
    findings = []
    for lineno, comment in extract_comments(text, ext):
        if not comment or ALLOW_MARKER in comment:
            continue
        # URL は規約が許容する参照なので照合対象から外す
        target = _URL_RE.sub(" ", comment)
        for regex, label in _BLOCKING:
            if regex.search(target):
                findings.append((lineno, label, comment[:160]))
                break
    return findings
