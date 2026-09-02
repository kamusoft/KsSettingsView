#!/usr/bin/env python3
"""API 名の網羅検査 (concepts → skills の内容突き合わせ)。

目的:
  manifest の targets を Skill 単位に集約し、その Skill の源泉 concepts に
  バッククォート括りで登場する API トークンが、その Skill のどのファイル
  (ja 版で代表 — コード・API 名は en/ja で一致するため片側で足りる) にも
  登場しないものを報告する。ハッシュ差分では検出できない翻訳時の取りこぼし用。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に "  <skill> <- <concept>: <トークン列>" の行。無ければ
  "API-name coverage OK"。ヒューリスティックのため誤検出はあり得るので、
  結果は報告のみに使う (要追従リストへ自動昇格させない)。
"""

import json, os, re
from collections import defaultdict
M = json.load(open(os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")))
TOKEN = re.compile(r'`([A-Za-z_][A-Za-z0-9_.]*(?:\(\))?)`')
STOP = {"true", "false", "null", "nil", "None", "self", "this", "var", "val", "let",
        "public", "internal", "private", "open", "static", "enum", "class", "struct",
        "interface", "protocol", "data", "case", "import", "async", "await"}
def looks_api(t):
    core = t.rstrip("()")
    if core in STOP or len(core) < 3:
        return False
    return "." in core or core[0].isupper() or any(c.isupper() for c in core[1:])
def body(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()
skill_files = defaultdict(list)   # skill 名 -> 言語抜き相対パス群
skill_srcs = defaultdict(set)     # skill 名 -> 源泉 concepts 群
for rel, srcs in M["targets"].items():
    skill = rel.split("/")[0]
    skill_files[skill].append(rel)
    skill_srcs[skill].update(srcs)
issues = []
for skill in sorted(skill_files):
    hay = "".join(body(f"skills/ja/{rel}") for rel in skill_files[skill]
                  if os.path.exists(f"skills/ja/{rel}"))
    missing = defaultdict(set)
    for src in sorted(skill_srcs[skill]):
        cpath = os.path.join("kasane/concepts", src)
        if not os.path.exists(cpath):
            continue
        for t in TOKEN.findall(body(cpath)):
            if not looks_api(t):
                continue
            core = t.rstrip("()")
            # ドット付き完全形は、末尾セグメント (メンバー名) が載っていれば掲載済み扱い
            if core in hay or ("." in core and core.split(".")[-1] in hay):
                continue
            missing[src].add(t)
    for src, toks in sorted(missing.items()):
        issues.append(f"  {skill} <- {src}: {', '.join(sorted(toks))}")
print("\n".join(issues) if issues else "API-name coverage OK")
