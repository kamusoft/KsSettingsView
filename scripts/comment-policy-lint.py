#!/usr/bin/env python3
"""ソースコメント規約の違反をリポジトリ全体から検出する lint。

使い方:

    python3 scripts/comment-policy-lint.py            # 追跡中の全ソースを検査
    python3 scripts/comment-policy-lint.py --summary  # ファイル単位の件数だけ表示
    python3 scripts/comment-policy-lint.py ios/Sources  # パスを絞って検査
    python3 scripts/comment-policy-lint.py --selftest  # 検出ロジックと hook の疎通確認

規約は kasane/handbook/cross/comment-policy.md。
検出ロジックは scripts/comment_policy_rules.py に同居し、書き込み前の hook と共有する。
違反が 1 件でもあれば終了コード 1 を返す。
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from comment_policy_rules import TARGET_EXT, is_excluded, scan_text  # noqa: E402


def repo_root() -> str:
    out = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True,
        text=True,
        check=True,
    )
    return out.stdout.strip()


def tracked_files(root: str, paths: list[str]) -> list[str]:
    cmd = ["git", "-C", root, "ls-files", "-z"] + paths
    out = subprocess.run(cmd, capture_output=True, text=True, check=True)
    files = []
    for rel in out.stdout.split("\0"):
        if not rel:
            continue
        if os.path.splitext(rel)[1].lower() not in TARGET_EXT:
            continue
        if is_excluded(rel):
            continue
        files.append(rel)
    return files


# 検出ロジックと hook の疎通を確認するケース。(説明, ソース, 期待する禁止件数)
SELFTEST_CASES = [
    ("許容: ADR 参照", "// 判断の根拠は cross/ADR-0017 に従う\n", 0),
    ("許容: URL", "// 詳細は https://example.com/changes/design.md を参照\n", 0),
    ("許容: 恒常規格", "// RFC 6749 のトークン形式に合わせる\n", 0),
    ("許容: 文字列リテラル内の禁止語", 'let s = "openspec/changes/foo/spec.md"\n', 0),
    ("許容: allow マーカー", "// kasane/changes/foo/spec.md comment-policy:allow\n", 0),
    ("禁止: アーカイブパス", "// 仕様: kasane/changes/foo/specs/spec.md\n", 1),
    ("禁止: 凍結資料パス", "// 仕様: openspec/changes/foo/design.md\n", 1),
    ("禁止: 議論通番", "// 設計判断は Decision 5 に従う\n", 1),
    ("禁止: 構文キーワード", "// この関数は MUST NOT be called twice\n", 1),
    ("禁止: 構造名の裸参照", '/* "セル型の追加" Requirement を満たす */\n', 1),
    ("禁止: ブロックコメント継続行", "/**\n * 仕様: kasane/changes/foo/spec.md\n */\n", 1),
    ("禁止: タスク通番 (階層)", "// MARK: - ButtonCell の切替（タスク 2.4 / 3.3）\n", 1),
    ("禁止: タスク通番 (単層)", "// 検証はタスク 5 で行う\n", 1),
    ("禁止: タスク通番 (英語・単層)", "// covered by task 3\n", 1),
    ("許容: 通番を伴わないタスク語", "// assembleDebug タスクを 3 回実行して平均を取る\n", 0),
    ("許容: 公開恒久文書の裸ホストパス", "// 仕様は developer.android.com/reference/android/view/View に従う\n", 0),
    ("許容: リポジトリ名を含む公開 URL", "// 実装は github.com/kamusoft/kasane/blob/main/README.md を参照\n", 0),
    ("許容: Apple 公式文書の裸ホストパス", "// developer.apple.com/documentation/uikit/uiview を参照\n", 0),
]

# hook のラチェット動作 (既存行は見逃し、新規混入は止める) を確認するケース
HOOK_CASES = [
    ("新規混入は拒否", "", "// 仕様: kasane/changes/foo/spec.md\n", 2),
    ("既存行の持ち越しは許可", "// 仕様: kasane/changes/foo/spec.md\n", "// 仕様: kasane/changes/foo/spec.md\nlet x = 1\n", 0),
    ("規約対象外パスは検査しない", "", "// 仕様: kasane/changes/foo/spec.md\n", 0),
]


def run_selftest(root: str) -> int:
    failures = 0
    print("[検出ロジック]")
    for name, src, expected in SELFTEST_CASES:
        actual = len(scan_text(src, ".swift"))
        ok = actual == expected
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name} (期待 {expected} / 実際 {actual})")

    print("[hook 疎通]")
    hook = os.path.join(root, ".claude", "hooks", "comment-policy-check.py")
    if not os.path.exists(hook):
        print(f"  NG   hook が見つかりません: {hook}")
        return failures + 1
    # worktree 配下のパスでも検査されること (規約対象外の除外に巻き込まれないこと) を含めて確認する。
    # 経路名はターゲットごとに明示して持つ。パス文字列から判定すると、worktree 内で
    # 自己テストを走らせたときにリポジトリルート側のパスにも `/worktrees/` が含まれ、
    # 「通常経路でも発火する」ことの確認が消えてしまう。
    targets = [
        ("通常", os.path.join(root, "ios/Sources/KsSettingsViewCore/SelfTest.swift")),
        ("worktree", os.path.join(root, ".claude/worktrees/wt-selftest/ios/Sources/KsSettingsViewCore/SelfTest.swift")),
    ]
    for label, old, new, expected in HOOK_CASES:
        paths = targets if "対象外" not in label else [("対象外", os.path.join(root, "kasane/SelfTest.swift"))]
        for where, target in paths:
            payload = {
                "tool_name": "Edit",
                "cwd": root,
                "tool_input": {"file_path": target, "old_string": old, "new_string": new},
            }
            proc = subprocess.run(
                [sys.executable, hook], input=json.dumps(payload), capture_output=True, text=True
            )
            ok = proc.returncode == expected
            failures += 0 if ok else 1
            print(f"  {'OK  ' if ok else 'NG  '} {label} ({where}: 期待 exit {expected} / 実際 {proc.returncode})")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(description="ソースコメント規約の違反を検出する")
    parser.add_argument("--selftest", action="store_true", help="検出ロジックと hook の疎通を確認する")
    parser.add_argument("paths", nargs="*", help="検査対象パス (既定はリポジトリ全体)")
    parser.add_argument("--summary", action="store_true", help="ファイル単位の件数だけ表示する")
    args = parser.parse_args()

    root = repo_root()
    if args.selftest:
        return 1 if run_selftest(root) else 0

    files = tracked_files(root, args.paths)

    total_blocking = 0
    dirty_files = 0

    for rel in files:
        full = os.path.join(root, rel)
        try:
            with open(full, encoding="utf-8") as f:
                text = f.read()
        except (OSError, UnicodeDecodeError):
            continue
        ext = os.path.splitext(rel)[1].lower()
        findings = scan_text(text, ext)
        if not findings:
            continue
        dirty_files += 1
        total_blocking += len(findings)
        if args.summary:
            print(f"{rel}: {len(findings)} 件")
            continue
        print(f"\n{rel}")
        for lineno, reason, line in findings:
            print(f"  {rel}:{lineno}: [禁止] {reason}")
            print(f"      {line}")

    print(
        f"\n合計: {dirty_files} ファイル / 禁止 {total_blocking} 件"
        f" (検査対象 {len(files)} ファイル)"
    )
    return 1 if total_blocking else 0


if __name__ == "__main__":
    sys.exit(main())
