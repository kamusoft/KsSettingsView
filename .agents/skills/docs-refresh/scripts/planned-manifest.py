#!/usr/bin/env python3
"""予定 manifest の生成。

目的:
  ディスクの skills/.manifest.json に、承認済みの配置判断 (targets 追記) ・
  除外判断 (excluded 追記) ・削除済み concept の整理を重ねた「この実行で
  書き出す予定の manifest」を標準出力へ吐く。整合性チェックはディスクの
  manifest ではなくこの出力を入力にする。

入力:
  ディスクの skills/.manifest.json (カレントディレクトリはリポジトリルート)。
  環境変数 DOCS_REFRESH_DECISIONS — 承認済み判断を書いた JSON ファイルのパス
    (省略時は判断なし = 入力 manifest をそのまま整形して出力する)。
    JSON の形:
      {
        "addTargets":   {"<言語抜き Skill 相対パス>": ["<新規 concept パス>", ...]},
        "addExcluded":  {"<concept パス>": "<除外理由>"},
        "dropConcepts": ["<削除済み concept パス>", ...]
      }
    いずれのキーも省略可。

出力:
  標準出力に整形済み JSON。呼び出し側がリダイレクトで一時ファイルへ保存する。
"""

import json, os
M = json.load(open("skills/.manifest.json"))

decisions_path = os.environ.get("DOCS_REFRESH_DECISIONS")
D = json.load(open(decisions_path, encoding="utf-8")) if decisions_path else {}
ADD_TARGETS = D.get("addTargets", {})
ADD_EXCLUDED = D.get("addExcluded", {})
DROP_CONCEPTS = D.get("dropConcepts", [])

for rel, srcs in ADD_TARGETS.items():
    M["targets"][rel] = sorted(set(M["targets"].get(rel, [])) | set(srcs))
M["excluded"].update(ADD_EXCLUDED)
for c in DROP_CONCEPTS:
    for rel in M["targets"]:
        M["targets"][rel] = [x for x in M["targets"][rel] if x != c]
    M["excluded"].pop(c, None)
print(json.dumps(M, ensure_ascii=False, indent=2))
