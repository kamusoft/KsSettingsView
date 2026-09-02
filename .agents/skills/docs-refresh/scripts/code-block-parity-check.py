#!/usr/bin/env python3
"""en/ja のコードブロック byte 一致検査。

目的:
  言語ペア (targets の Skill ファイルペアと、readmes の <stem>.md ↔
  <stem>_ja.md) について、コードブロックの数・順序・言語指定・中身が
  完全に一致することを確かめる。コード例は言語に依らず同一とする原則の担保。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  環境変数 DOCS_REFRESH_README_ONLY — "1" のとき targets の Skill ペアを
    対象から外し、readmes の言語ペアだけを検査する。
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に不一致の行。無ければ "code blocks byte-identical"。
"""

import json, os, re
M = json.load(open(os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")))
README_ONLY = os.environ.get("DOCS_REFRESH_README_ONLY") == "1"

def language_pairs(M):
    """検査対象の (en 側, ja 側, 表示名) を列挙する"""
    out = [] if README_ONLY else [(f"skills/en/{rel}", f"skills/ja/{rel}", rel) for rel in M["targets"]]
    readmes = set(M["readmes"])
    for p in M["readmes"]:
        if p.endswith("_ja.md"):
            continue
        ja = p[:-len(".md")] + "_ja.md"
        if ja in readmes:
            out.append((p, ja, p))
    return out

FENCE = re.compile(r'^(```+)([^\n]*)\n(.*?)^\1\s*$', re.M | re.S)
def blocks(path):
    with open(path, encoding="utf-8") as fh:
        return [(m.group(2).strip(), m.group(3)) for m in FENCE.finditer(fh.read())]
issues = []
for en, ja, label in language_pairs(M):
    if not (os.path.exists(en) and os.path.exists(ja)):
        continue
    be, bj = blocks(en), blocks(ja)
    if len(be) != len(bj):
        issues.append(f"  {label}: code block count differs en={len(be)} ja={len(bj)}")
        continue
    for i, (x, y) in enumerate(zip(be, bj), 1):
        if x != y:
            issues.append(f"  {label}: code block #{i} differs (lang or body)")
print("\n".join(issues) if issues else "code blocks byte-identical")
