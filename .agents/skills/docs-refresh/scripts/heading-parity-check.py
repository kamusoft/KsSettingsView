#!/usr/bin/env python3
"""en/ja の節構成一致検査。

目的:
  言語ペア (targets の Skill ファイルペアと、readmes の <stem>.md ↔
  <stem>_ja.md) について、frontmatter とコードブロックを除いた本文の
  見出し階層の並びが一致することを確かめる。見出しの文言は言語が違うため
  比較しない。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  環境変数 DOCS_REFRESH_README_ONLY — "1" のとき targets の Skill ペアを
    対象から外し、readmes の言語ペアだけを検査する。
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に不一致・欠落の行。無ければ "en/ja heading structure OK"。
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

H = re.compile(r'^(#{1,6})\s', re.M)
def levels(path):
    with open(path, encoding="utf-8") as fh:
        body = re.sub(r'^---\n.*?\n---\n', '', fh.read(), count=1, flags=re.S)
    body = re.sub(r'^```.*?^```', '', body, flags=re.M | re.S)
    return [len(m.group(1)) for m in H.finditer(body)]
issues = []
for en, ja, label in language_pairs(M):
    for p in (en, ja):
        if not os.path.exists(p):
            issues.append(f"  {p}: MISSING")
    if not (os.path.exists(en) and os.path.exists(ja)):
        continue
    le, lj = levels(en), levels(ja)
    if le != lj:
        issues.append(f"  {label}: heading levels differ en={le} ja={lj}")
print("\n".join(issues) if issues else "en/ja heading structure OK")
