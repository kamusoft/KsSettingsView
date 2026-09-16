#!/usr/bin/env python3
"""内部リンク解決の検査。

目的:
  検査対象ファイルに書かれた markdown リンクのうち、外部 URL 以外の
  相対リンクがリポジトリ内の実在ファイルへ解決することを確かめる。

入力:
  環境変数 DOCS_REFRESH_TARGETS — 1 行 1 パスの検査対象一覧
    (targets-list.py の出力)。既定は /tmp/docs-refresh-targets.txt。
    呼び出し側が一覧を別の場所へ書き出す場合は必ずこの環境変数で渡す
    (既定のパスは他プロジェクトの過去実行が残した一覧と衝突しうるため)。
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に未解決リンク・欠落ファイルの行。無ければ
  "All internal links resolve"。対象一覧が空のときは検査せずその旨を出す
  (0 件を適合と誤読しないため)。一覧そのものが読めないときは
  標準エラーへ理由を出して exit 1 (検査したつもりで素通りさせないため)。
"""

import os, re, sys

targets_path = os.environ.get("DOCS_REFRESH_TARGETS", "/tmp/docs-refresh-targets.txt")
try:
    with open(targets_path, encoding="utf-8") as fh:
        targets = [l.strip() for l in fh if l.strip()]
except OSError as e:
    print(
        f"検査対象一覧を読めませんでした: {targets_path} ({e.strerror})\n"
        "targets-list.py の出力先と DOCS_REFRESH_TARGETS を一致させてから再実行してください。",
        file=sys.stderr,
    )
    raise SystemExit(1)
if not targets:
    print("検査対象が空です (manifest の targets / readmes を確認してから再実行)")
    raise SystemExit(0)
issues = []
for path in targets:
    if not os.path.exists(path):
        issues.append(f"  {path}: MISSING")
        continue
    with open(path, encoding="utf-8") as fh:
        content = fh.read()
    for m in re.finditer(r'\[([^\]]+)\]\(([^)#\s]+)(?:#[^)]*)?\)', content):
        target = m.group(2)
        if target.startswith(("http://", "https://", "mailto:", "file://")):
            continue
        base = os.path.dirname(path) or "."
        resolved = os.path.normpath(os.path.join(base, target))
        if not os.path.exists(resolved):
            issues.append(f"  {path}: [{m.group(1)}]({target}) -> {resolved} MISSING")
print("\n".join(issues) if issues else "All internal links resolve")
