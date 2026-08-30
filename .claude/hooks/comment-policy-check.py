#!/usr/bin/env python3
"""ソースコメント規約の禁止参照を、書き込み前に検査してブロックする PreToolUse hook。

規約は kasane/concepts/cross/conventions/comment-policy.md。
検出ロジックは scripts/comment_policy_rules.py にあり、リポジトリ全体 lint
(scripts/comment-policy-lint.py) と共有する。

**ラチェット方式**: 検査するのは「その書き込みで新しく増えるコメント行」だけ。
Edit なら old_string に、Write なら書き込み前のファイルに既に存在していたコメントは
見逃す。既存の違反を抱えたファイルの編集が巻き添えでブロックされるのを避けつつ、
新規混入だけを確実に止める。

禁止参照を新しく増やす書き込みは exit 2 で拒否する。誤検知の行には
``comment-policy:allow`` を書き添えれば個別に除外できる。
"""

from __future__ import annotations

import json
import os
import sys

_REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(_REPO_ROOT, "scripts"))

try:
    from comment_policy_rules import (
        ALLOW_MARKER,
        TARGET_EXT,
        extract_comments,
        is_excluded,
        scan_text,
        to_repo_relative,
    )
except Exception as exc:  # 検出ロジックを読めない = 検査が無音で死ぬ状態なので必ず知らせる
    print(f"[comment-policy] 検出ロジックを読み込めません: {exc}", file=sys.stderr)
    print(f"[comment-policy] 期待した場所: {os.path.join(_REPO_ROOT, 'scripts')}", file=sys.stderr)
    sys.exit(0)


def _baseline_comments(tool_name: str, tool_input: dict, path: str, ext: str) -> set[str]:
    """書き込み前から存在していたコメント行の集合を返す。"""
    if tool_name == "Edit":
        base = tool_input.get("old_string") or ""
    elif tool_name == "MultiEdit":
        base = "\n".join(e.get("old_string") or "" for e in tool_input.get("edits") or [])
    else:  # Write = ファイル全置換なので、ディスク上の現在の内容が基準
        try:
            with open(path, encoding="utf-8") as f:
                base = f.read()
        except (OSError, UnicodeDecodeError):
            base = ""
    return {c for _, c in extract_comments(base, ext)}


def _written_text(tool_name: str, tool_input: dict) -> str:
    if tool_name == "MultiEdit":
        return "\n".join(e.get("new_string") or "" for e in tool_input.get("edits") or [])
    return tool_input.get("new_string") or tool_input.get("content") or ""


def main() -> int:
    try:
        data = json.load(sys.stdin)
    except Exception as exc:
        print(f"[comment-policy] 入力を解釈できません: {exc}", file=sys.stderr)
        return 0

    tool_name = data.get("tool_name", "")
    tool_input = data.get("tool_input") or {}
    path = tool_input.get("file_path") or ""
    ext = os.path.splitext(path)[1].lower()
    if ext not in TARGET_EXT:
        return 0

    project_dir = data.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR")
    rel = to_repo_relative(path, project_dir)
    if is_excluded(rel):
        return 0

    text = _written_text(tool_name, tool_input)
    if not text:
        return 0

    findings = scan_text(text, ext)
    if not findings:
        return 0

    # ラチェット: 書き込み前から在った行は既存債務として見逃す
    baseline = _baseline_comments(tool_name, tool_input, path, ext)
    new_findings = [f for f in findings if f[2] not in baseline]
    if not new_findings:
        return 0

    print(f"[comment-policy] {rel}: 禁止された参照をコメントに新規追加しています。", file=sys.stderr)
    for lineno, reason, line in new_findings[:10]:
        print(f"  - {reason}: {line}", file=sys.stderr)
    if len(new_findings) > 10:
        print(f"  - (他 {len(new_findings) - 10} 件)", file=sys.stderr)
    print(
        "\nコメントは、そのファイルだけを読む人に意味が通る形で書いてください。\n"
        "  - 参照が装飾なら削除する (例: 「// 仕様: <アーカイブ文書のパス>」の行ごと消す)\n"
        "  - 設計理由と一体なら、対応する ADR があれば `<domain>/ADR-NNNN` へ置換する\n"
        "  - 対応 ADR が無ければ、コメント内で自己完結する説明に書き直す (新規 ADR は起票しない)\n"
        "  - MUST / SHOULD 等は自然な日本語 (「〜する」「〜してはいけない」) に直す\n"
        "規約全文: kasane/concepts/cross/conventions/comment-policy.md\n"
        f"誤検知の場合のみ、その行に {ALLOW_MARKER} を書き添えれば除外されます。",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
