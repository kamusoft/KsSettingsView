#!/usr/bin/env python3
"""concepts 網羅検査。

目的:
  kasane/concepts/ に実在する概念ファイルのうち、manifest の targets の値にも
  excluded のキーにも現れないもの (配置判断が必要) と、manifest に載っているが
  実在しないもの (targets / excluded の整理が必要) を洗い出す。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に UNCOVERED / DELETED の一覧。どちらも無ければ "concepts coverage OK"。
"""

import json, os
M = json.load(open(os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")))
referenced = {c for srcs in M["targets"].values() for c in srcs}
excluded = set(M["excluded"])
actual = set()
for root, _, files in os.walk("kasane/concepts"):
    for f in files:
        if f.endswith(".md") and f not in ("index.md", "log.md", "rules.md"):
            actual.add(os.path.relpath(os.path.join(root, f), "kasane/concepts"))
uncovered = sorted(actual - referenced - excluded)
missing = sorted((referenced | excluded) - actual)
if uncovered:
    print("UNCOVERED (未参照かつ未除外 — 配置判断が必要):")
    print("\n".join("  " + p for p in uncovered))
if missing:
    print("DELETED (manifest にあるが実在しない — targets/excluded の整理が必要):")
    print("\n".join("  " + p for p in missing))
if not uncovered and not missing:
    print("concepts coverage OK")
