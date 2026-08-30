#!/usr/bin/env python3
"""生ログの無害化 (Kasane 標準)。

端末の生ログ・ビルドログ・クラッシュログを証跡 (evidence/ や verify / review の本文) に貼る前に通す。
local-path-lint.py (ローカル絶対パス) と identity-lint.py (個体・個人・秘密を特定する値) の判定を使い、
検出した値を規約のプレースホルダに置き換えて出力する。規約は ksn-core references/evidence.md。

使い方:
  python3 scripts/log-sanitize.py <file>...              # 置換結果を stdout に出力 (複数ファイルは連結)
  python3 scripts/log-sanitize.py --in-place <file>...   # ファイルを置換結果で上書き
  <command> | python3 scripts/log-sanitize.py            # stdin → stdout
  python3 scripts/log-sanitize.py --summary <file>       # 置換件数 (種類別) だけを表示

置換の対応 (種類 → プレースホルダ) は identity-lint.py の PLACEHOLDERS と、パスは
`/Users/<USER>/` `/Volumes/<VOLUME>/` `C:\\Users\\<USER>\\`。lint と同じ判定なので、
sanitize を通した本文は hook / lint を通る。`lint.identity.allow` の値と config の disable 群は置換しない。
"""

from __future__ import annotations

import importlib.util
import os
import re
import sys


def _load(name: str):
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), name)
    spec = importlib.util.spec_from_file_location(name.replace("-", "_").replace(".py", ""), path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load("local-path-lint.py")
ID = _load("identity-lint.py")

PATH_RULES = [
    (re.compile(r"/Users/" + LP.NAME + r"/"), "/Users/<USER>/"),
    (re.compile(r"/Volumes/" + LP.NAME), "/Volumes/<VOLUME>"),
    (re.compile(r"([A-Za-z]:\\\\?)Users(\\\\?)" + LP.NAME), r"\1Users\2<USER>"),
]


def sanitize_line(line: str, settings, counts: dict[str, int]) -> str:
    for pat, rep in PATH_RULES:
        line, n = pat.subn(rep, line)
        if n:
            counts["local-path"] = counts.get("local-path", 0) + n
    found = ID.find_identities(line, settings, changes_scope=True)
    # 後ろから置換して位置を壊さない
    for kind, value, start, end in sorted(found, key=lambda f: f[2], reverse=True):
        placeholder = ID.PLACEHOLDERS[kind][1]
        line = line[:start] + placeholder + line[end:]
        counts[kind] = counts.get(kind, 0) + 1
    return line


def sanitize_text(text: str, settings, counts: dict[str, int]) -> str:
    return "\n".join(sanitize_line(l, settings, counts) for l in text.split("\n"))


def main(argv: list[str]) -> int:
    in_place = "--in-place" in argv
    summary_only = "--summary" in argv
    files = [a for a in argv if not a.startswith("--")]
    root = LP.repo_root()
    settings = ID.Settings(root)
    counts: dict[str, int] = {}

    if not files:
        out = sanitize_text(sys.stdin.read(), settings, counts)
        if not summary_only:
            sys.stdout.write(out)
    else:
        for f in files:
            with open(f, encoding="utf-8", errors="replace") as fh:
                text = fh.read()
            out = sanitize_text(text, settings, counts)
            if in_place:
                with open(f, "w", encoding="utf-8") as fh:
                    fh.write(out)
            elif not summary_only:
                sys.stdout.write(out)

    total = sum(counts.values())
    if total:
        detail = ", ".join(f"{k} {v}" for k, v in sorted(counts.items()))
        sys.stderr.write(f"sanitize: {total} 件を置換 ({detail})\n")
    else:
        sys.stderr.write("sanitize: 置換対象なし\n")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
