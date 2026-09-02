#!/usr/bin/env python3
"""整合性チェックの検査対象ファイル一覧を生成する。

目的:
  manifest の targets (言語抜き Skill 相対パス) を en/ja に展開したものと
  readmes を、1 行 1 パスで吐く。旧名残 grep・内部リンク解決・lint・
  配信識別子 grep はこの一覧を読んで対象を決める。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  環境変数 DOCS_REFRESH_README_ONLY — "1" のとき targets 由来を除き
    readmes だけを吐く。
  カレントディレクトリはリポジトリルート。

出力:
  標準出力にリポジトリ相対パスの一覧。呼び出し側がリダイレクトで
  一時ファイルへ保存する。
"""

import json, os
M = json.load(open(os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")))
readme_only = os.environ.get("DOCS_REFRESH_README_ONLY") == "1"
if not readme_only:
    for p in M["targets"]:
        print(f"skills/en/{p}")
        print(f"skills/ja/{p}")
for p in M["readmes"]:
    print(p)
