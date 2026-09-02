#!/usr/bin/env python3
"""SKILL.md の frontmatter 検査。

目的:
  targets が指す各 Skill の SKILL.md (en/ja) の frontmatter が Agent Skills
  標準フィールド (name / description / license / metadata、metadata は
  language / source) の範囲内に収まり、name が en/ja で同一、
  metadata.language がパスの言語と一致することを確かめる。

入力:
  環境変数 DOCS_REFRESH_MANIFEST — 検査対象の manifest パス
    (既定: ディスクの skills/.manifest.json)
  カレントディレクトリはリポジトリルート。

出力:
  標準出力に違反の行。無ければ "frontmatter OK"。
"""

import json, os, re
ALLOWED_TOP = {"name", "description", "license", "metadata"}
ALLOWED_META = {"language", "source"}
M = json.load(open(os.environ.get("DOCS_REFRESH_MANIFEST", "skills/.manifest.json")))
skills = sorted({rel.split("/")[0] for rel in M["targets"]})
def front(path):
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    m = re.match(r'---\n(.*?)\n---\n', text, re.S)
    if not m:
        return None
    top, meta, in_meta = {}, {}, False
    for line in m.group(1).splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if re.match(r'^\S', line):
            in_meta = False
            key, _, val = line.partition(":")
            top[key.strip()] = val.strip().strip('"')
            in_meta = key.strip() == "metadata"
        elif in_meta:
            key, _, val = line.strip().partition(":")
            meta[key.strip()] = val.strip().strip('"')
    return top, meta
issues = []
for skill in skills:
    pair = {}
    for lang in ("en", "ja"):
        path = f"skills/{lang}/{skill}/SKILL.md"
        if not os.path.exists(path):
            issues.append(f"  {path}: MISSING")
            continue
        parsed = front(path)
        if parsed is None:
            issues.append(f"  {path}: frontmatter not found")
            continue
        top, meta = parsed
        pair[lang] = top
        extra = set(top) - ALLOWED_TOP
        if extra:
            issues.append(f"  {path}: non-standard field(s) {sorted(extra)}")
        for req in ("name", "description"):
            if req not in top:
                issues.append(f"  {path}: required field '{req}' missing")
        extra_meta = set(meta) - ALLOWED_META
        if extra_meta:
            issues.append(f"  {path}: non-standard metadata field(s) {sorted(extra_meta)}")
        if meta.get("language") != lang:
            issues.append(f"  {path}: metadata.language={meta.get('language')!r} != {lang!r}")
    if "en" in pair and "ja" in pair and pair["en"].get("name") != pair["ja"].get("name"):
        issues.append(f"  {skill}: name differs en={pair['en'].get('name')!r} ja={pair['ja'].get('name')!r}")
print("\n".join(issues) if issues else "frontmatter OK")
