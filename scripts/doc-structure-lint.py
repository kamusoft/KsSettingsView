#!/usr/bin/env python3
"""育つ文書 (concepts / roadmaps) の構造検査 (Kasane 標準 lint)。

追記を重ねるうちに本文が一枚の箇条書きへ潰れていくのを検出する。規約は ksn-core references/doc-structure.md。

使い方:
  python3 scripts/doc-structure-lint.py                # 検査範囲 (config lint.doc-structure.scope) を検査
  python3 scripts/doc-structure-lint.py --paths a b    # 指定ファイルだけ検査 (書き込み経路のスキルはこちらを使う)
  python3 scripts/doc-structure-lint.py --verbose      # 違反を省略せず全件表示する
  python3 scripts/doc-structure-lint.py --stats        # 違反の有無によらず実測値を一覧する (棚卸し・閾値調整用)

判定 (既定値。config.yaml の lint.doc-structure で上書きできる):
  item-chars     箇条書き 1 項目が 200 字を超える           → 違反 (段落・表の行・小節のいずれかへ移す)
  section-items  1 見出し配下のトップレベル項目が 10 を超える → 違反 (小節へ割る)
  nest-depth     箇条書きのネストが 3 段以上                 → 違反 (小節か表へ移す)
  heading-chars  1 見出しあたりの散文字数が 1200 字を超える   → 警告 (節を割らずに育てた長文)
  file-chars     1 ファイルの散文が 10000 字を超える          → 警告 (分割の検討。判断は ksn-core references/concepts.md)

heading-chars の分子は**表・コードブロック・図・frontmatter を除いた字数**。表や mermaid で書くほど数値が下がる
(推奨表現を使うことが数値の改善に一致する)。

違反があれば exit 1、警告だけなら exit 0。**hook には登録しない** — 書き込みのたびに止めると追記が進まないため、
書き込み経路のスキル (ksn-distill / ksn-concept / ksn-migrate / ksn-drift) が確定前に明示的に実行する。
"""

from __future__ import annotations

import importlib.util
import os
import re
import sys


def _load_sibling():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-path-lint.py")
    spec = importlib.util.spec_from_file_location("local_path_lint", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


LP = _load_sibling()

# 既定の検査範囲。規約既定は ksn-core references/config.md の lint.doc-structure.scope と揃える
DEFAULT_SCOPE = ["kasane/concepts", "kasane/handbook", "kasane/roadmaps"]
# index.md / log.md / rules.md は目次・履歴・定義表であり、項目の列挙が正しい形なので対象外
SKIP_NAMES = {"index.md", "log.md", "rules.md"}
# 1 ファイルあたりに表示する違反の件数 (--verbose で解除)
SHOW_PER_FILE = 3

DEFAULTS = {
    "item-chars": 200,
    "section-items": 10,
    "nest-depth": 3,
    "heading-chars": 1200,
    "file-chars": 10000,
}

BULLET = re.compile(r"^(\s*)(?:[-*+]|\d+[.)])\s+(.*)$")
HEADING = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE = re.compile(r"^\s*(?:```|~~~)")
TABLE = re.compile(r"^\s*\|")


class Item:
    """箇条書きの 1 項目 (継続行を含み、ネストした子項目は含まない)。"""

    def __init__(self, lineno: int, depth: int, heading: str):
        self.lineno = lineno
        self.depth = depth
        self.heading = heading
        self.parts: list[str] = []

    @property
    def chars(self) -> int:
        return sum(len(p.strip()) for p in self.parts)

    @property
    def head(self) -> str:
        return self.parts[0].strip()[:36] if self.parts else ""


def parse(src: str) -> dict:
    """Markdown を解析して構造メトリクスを返す。"""
    lines = src.split("\n")
    start = 0
    if lines and lines[0].strip() == "---":
        for j in range(1, len(lines)):
            if lines[j].strip() == "---":
                start = j + 1
                break

    items: list[Item] = []
    headings: list[str] = []
    section_counts: dict[str, int] = {}
    prose = 0
    cur: Item | None = None
    heading = "(冒頭)"
    indents: list[int] = []
    in_fence = False

    for n in range(start, len(lines)):
        raw = lines[n]

        if FENCE.match(raw):
            in_fence = not in_fence
            cur, indents = None, []
            continue
        if in_fence:
            continue  # コードブロック・mermaid は推奨表現なので散文字数に数えない
        if TABLE.match(raw):
            cur, indents = None, []
            continue  # 表も推奨表現

        m = HEADING.match(raw)
        if m:
            heading = m.group(2).strip()
            headings.append(heading)
            section_counts.setdefault(heading, 0)
            cur, indents = None, []
            continue

        m = BULLET.match(raw)
        if m:
            indent = len(m.group(1).expandtabs(4))
            while indents and indents[-1] >= indent:
                indents.pop()
            indents.append(indent)
            cur = Item(n + 1, len(indents), heading)
            cur.parts.append(m.group(2))
            items.append(cur)
            if len(indents) == 1:
                section_counts[heading] = section_counts.get(heading, 0) + 1
            prose += len(m.group(2).strip())
            continue

        if not raw.strip():
            cur, indents = None, []
            continue

        if cur is not None:
            cur.parts.append(raw)
        prose += len(raw.strip())

    return {
        "items": items,
        "headings": headings,
        "section_counts": section_counts,
        "prose": prose,
        "heading_chars": prose // max(1, len(headings)),
    }


def check(src: str, th: dict) -> tuple[list[tuple[int, str]], list[str]]:
    """1 ファイルの (違反, 警告) を返す。違反は (行番号, 本文) の組。"""
    m = parse(src)
    errors: list[tuple[int, str]] = []
    warns: list[str] = []

    for it in m["items"]:
        if it.chars > th["item-chars"]:
            errors.append((it.lineno, f"箇条書き 1 項目が {it.chars} 字 — "
                                      f"段落・表の行・小節のいずれかへ移す 「{it.head}…」"))
        if it.depth >= th["nest-depth"]:
            errors.append((it.lineno, f"箇条書きのネストが {it.depth} 段 — "
                                      f"小節か表へ移す 「{it.head}…」"))

    for heading, count in m["section_counts"].items():
        if count > th["section-items"]:
            errors.append((0, f"見出し「{heading}」配下のトップレベル項目が {count} 件 — 小節へ割る"))

    if m["prose"] > th["file-chars"]:
        warns.append(f"散文 {m['prose']} 字 — 複数の概念を抱えていないか確認する "
                     f"(節を整えてもなお大きいなら分割を検討: ksn-core references/concepts.md)")

    if m["heading_chars"] > th["heading-chars"]:
        warns.append(f"見出しあたり {m['heading_chars']} 字 "
                     f"(見出し {len(m['headings'])} 個で散文 {m['prose']} 字) — "
                     f"節を割るか、対の並びを表へ移す")

    errors.sort(key=lambda e: e[0])
    return errors, warns


def targets(root: str, paths: list[str] | None, scope: list[str], excludes: list[str]) -> list[str]:
    if paths:
        return [LP.normalize_rel(p, root) for p in paths]
    found: list[str] = []
    for s in scope:
        base = os.path.join(root, s)
        if not os.path.isdir(base):
            continue
        for dirpath, _dirnames, filenames in os.walk(base):
            for fn in sorted(filenames):
                if not fn.endswith(".md") or fn in SKIP_NAMES:
                    continue
                rel = LP.normalize_rel(os.path.join(dirpath, fn), root)
                if not LP.is_excluded(rel, excludes):
                    found.append(rel)
    return sorted(found)


def main(argv: list[str]) -> int:
    stats = "--stats" in argv
    verbose = "--verbose" in argv
    paths = None
    if "--paths" in argv:
        paths = [a for a in argv[argv.index("--paths") + 1:] if not a.startswith("--")]

    root = LP.repo_root()
    cfg = LP.load_config(root)
    th = dict(DEFAULTS)
    for key in DEFAULTS:
        v = LP.config_get(cfg, f"lint.doc-structure.{key}")
        # 整数で来る想定だが、引用符付き ("200") で書かれても受ける (不正値は既定値のまま)
        if v is not None and not isinstance(v, (list, dict, bool)):
            try:
                th[key] = int(str(v).strip())
            except ValueError:
                pass
    scope = LP.config_get(cfg, "lint.doc-structure.scope") or DEFAULT_SCOPE
    excludes = LP.load_excludes(root)

    by_file: list[tuple[str, list[tuple[int, str]], list[str]]] = []
    rows: list[tuple[str, dict]] = []
    for rel in targets(root, paths, scope, excludes):
        full = os.path.join(root, rel)
        if not os.path.isfile(full):
            continue
        with open(full, encoding="utf-8") as f:
            src = f.read()
        e, w = check(src, th)
        if e or w:
            by_file.append((rel, e, w))
        if stats:
            rows.append((rel, parse(src)))

    if stats:
        print(f"{'file':64} {'見出し':>4} {'散文':>6} {'/見出し':>7} {'項目':>4} {'最長':>5}")
        for rel, m in sorted(rows, key=lambda r: -r[1]["heading_chars"]):
            longest = max((i.chars for i in m["items"]), default=0)
            print(f"{rel:64} {len(m['headings']):4} {m['prose']:6} {m['heading_chars']:7} "
                  f"{len(m['items']):4} {longest:5}")
        print()

    total_e = sum(len(e) for _, e, _ in by_file)
    if total_e:
        print(f"育つ文書の構造規約に反しています (ksn-core references/doc-structure.md) — "
              f"{total_e} 件 / {sum(1 for _, e, _ in by_file if e)} ファイル:")
        print(f"  上限: 項目 {th['item-chars']} 字 / 節あたり {th['section-items']} 項目 / "
              f"ネスト {th['nest-depth'] - 1} 段")
        for rel, errs, _ in by_file:
            if not errs:
                continue
            print(f"\n  {rel} — {len(errs)} 件")
            shown = errs if verbose else errs[:SHOW_PER_FILE]
            for lineno, msg in shown:
                loc = f":{lineno}" if lineno else ""
                print(f"    {loc} {msg}")
            if len(errs) > len(shown):
                print(f"    ほか {len(errs) - len(shown)} 件 (--verbose で全件)")

    warned = [(rel, w) for rel, _, w in by_file if w]
    if warned:
        print("\n構造の注意 (違反ではないが、節を割るか表へ移すのが望ましい):")
        for rel, ws in warned:
            for w in ws:
                print(f"  {rel}: {w}")

    if not by_file and not stats:
        print("構造 lint: 違反なし")
    return 1 if total_e else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
