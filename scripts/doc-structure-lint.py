#!/usr/bin/env python3
"""育つ文書 (concepts / roadmaps) の構造検査 (Kasane 標準 lint)。

追記を重ねるうちに本文が一枚の箇条書きへ潰れていくのを検出する。規約は ksn-core references/doc-structure.md。

使い方:
  python3 scripts/doc-structure-lint.py                # 検査範囲 (config lint.doc-structure.scope) を検査
  python3 scripts/doc-structure-lint.py --paths a b    # 指定パスだけ検査 (書き込み経路のスキルはこちらを使う。ディレクトリは .md へ展開、不在パスは exit 2)
  python3 scripts/doc-structure-lint.py --verbose      # 違反を省略せず全件表示する
  python3 scripts/doc-structure-lint.py --stats        # 違反の有無によらず実測値を一覧する (棚卸し・閾値調整用)
  python3 scripts/doc-structure-lint.py --selftest     # 判定・全体実行の読み飛ばし・--paths の疎通確認

全体実行 (--paths なし) は、もう育たない roadmap を読み飛ばす — archive 済みのロードマップ (kasane/roadmaps/archive/)
と、roadmap.md のフェーズ一覧で状態が completed / dropped / promoted のフェーズ配下。書き直すと当時の記録の改変に
なり是正できないため (kasane/changes/archive/ がもともと scope に無いのと揃える)。--paths で明示したときは検査する。

判定 (既定値。config.yaml の lint.doc-structure で上書きできる):
  item-chars     箇条書き 1 項目が 200 字を超える           → 違反 (段落・表の行・小節のいずれかへ移す)
  section-items  1 見出し配下のトップレベル項目が 10 を超える → 違反 (小節へ割る)
  nest-depth     箇条書きのネストが 3 段以上                 → 違反 (小節か表へ移す)
  heading-chars  1 見出しあたりの散文字数が 1200 字を超える   → 警告 (節を割らずに育てた長文)
  file-chars     1 ファイルの散文が 10000 字を超える          → 警告 (分割の検討。判断は ksn-core references/concepts.md)
  adr-title-chars     ADR のタイトルが 100 字を超える             → 違反 (1 文に縮め、細部は出典へ)
  adr-decision-chars  ADR の Decision 節の散文が 1500 字を超える  → 違反 (設計に当たる記述を concepts / 出典へ移す)
  adr-alternatives    ADR の却下案が 10 件を超える                → 違反 (同じ方向の中の案は出典に残す)

adr-* の 3 検査は kasane/decisions/ 配下で status が proposed (または未記載) のファイルにだけ掛かる — ADR が詳細設計書に
なるのを昇格前に止めるための検査で、accepted 以後は本文が不変で是正できない。規約は ksn-core references/decisions.md
「決定の粒度」。

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
# 全体実行で読み飛ばす凍結済みの roadmap の置き場と、凍結済みとみなすフェーズ状態
# (ksn-roadmap が completed / dropped / promoted のフェーズの改訂を禁じている)
ROADMAPS = "kasane/roadmaps"
FROZEN_PHASE_STATES = {"completed", "dropped", "promoted"}

DEFAULTS = {
    "item-chars": 200,
    "section-items": 10,
    "nest-depth": 3,
    "heading-chars": 1200,
    "file-chars": 10000,
    "adr-title-chars": 100,
    "adr-decision-chars": 1500,
    "adr-alternatives": 10,
}

BULLET = re.compile(r"^(\s*)(?:[-*+]|\d+[.)])\s+(.*)$")
HEADING = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE = re.compile(r"^\s*(?:```|~~~)")
TABLE = re.compile(r"^\s*\|")
TABLE_SEP = re.compile(r"^\s*\|\s*:?-")
FM_KEY = re.compile(r"^([A-Za-z][A-Za-z-]*):\s*(.*)$")
# ADR の決定の粒度の検査を掛ける status (accepted 以後は本文が不変)
ADR_CHECK_STATES = {"", "proposed"}


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


def frontmatter(src: str) -> tuple[dict, int]:
    """先頭の frontmatter を (キー → 値, 本文の開始行) で返す。無ければ ({}, 0)。"""
    lines = src.split("\n")
    if not lines or lines[0].strip() != "---":
        return {}, 0
    fm: dict[str, str] = {}
    for j in range(1, len(lines)):
        if lines[j].strip() == "---":
            return fm, j + 1
        m = FM_KEY.match(lines[j])
        if m:
            fm[m.group(1)] = m.group(2).strip().strip("\"'")
    return {}, 0


def is_adr(rel: str) -> bool:
    return "decisions" in rel.split("/")


def adr_metrics(src: str) -> dict:
    """ADR の決定の粒度のメトリクス: タイトル字数・Decision 節の散文字数・却下案の件数。

    節は h2 で切る (Decision 節の h3 小節は Decision に含める)。散文は parse() と同じく表・コードブロックを
    数えない。却下案は Alternatives Considered 節のトップレベル項目と、表の見出し行・区切り行を除いた行を数える。
    """
    fm, start = frontmatter(src)
    section = None
    in_fence = False
    decision = 0
    alts = 0
    seen_sep = False
    for raw in src.split("\n")[start:]:
        if FENCE.match(raw):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        m = HEADING.match(raw)
        if m:
            if len(m.group(1)) <= 2:
                section = m.group(2).strip()
                seen_sep = False
            continue
        if TABLE.match(raw):
            if section == "Alternatives Considered":
                if TABLE_SEP.match(raw):
                    seen_sep = True
                elif seen_sep:
                    alts += 1
            continue
        b = BULLET.match(raw)
        if section == "Decision":
            decision += len((b.group(2) if b else raw).strip())
        elif section == "Alternatives Considered" and b and len(b.group(1).expandtabs(4)) == 0:
            alts += 1
    return {
        "title": fm.get("title", ""),
        "status": fm.get("status", ""),
        "decision_chars": decision,
        "alternatives": alts,
    }


def check(src: str, th: dict, adr: bool = False) -> tuple[list[tuple[int, str]], list[str]]:
    """1 ファイルの (違反, 警告) を返す。違反は (行番号, 本文) の組。adr なら決定の粒度の 3 検査も掛ける。"""
    m = parse(src)
    errors: list[tuple[int, str]] = []
    warns: list[str] = []

    if adr:
        a = adr_metrics(src)
        if len(a["title"]) > th["adr-title-chars"]:
            errors.append((0, f"ADR のタイトルが {len(a['title'])} 字 — 「何を〜する」の 1 文に縮め、"
                              f"細部は Decision 節にも書かず出典に残す (ksn-core references/decisions.md「決定の粒度」)"))
        if a["decision_chars"] > th["adr-decision-chars"]:
            errors.append((0, f"ADR の Decision 節の散文が {a['decision_chars']} 字 — 書き換えてもタイトルが変わらない"
                              f"記述 (名前の一覧・既定値・場合分け・読み口) は設計。concepts か出典へ移す"))
        if a["alternatives"] > th["adr-alternatives"]:
            errors.append((0, f"ADR の却下案が {a['alternatives']} 件 — 採ってもタイトルが変わらない案は設計の却下案。"
                              f"出典の design.md に残し ADR から外す"))

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


def phase_states(src: str) -> list[tuple[str, str]]:
    """roadmap.md のフェーズ一覧から (フェーズ ID, 状態) を返す。

    列は見出し行の「ID」「状態」で引く (Repo 列の有無で位置が変わるため)。表が見つからない・崩れているときは
    空を返す — 読み飛ばしが起きないだけで、検査の素通りにはならない。
    """
    found: list[tuple[str, str]] = []
    cols: tuple[int, int] | None = None
    for line in src.split("\n"):
        if not TABLE.match(line):
            cols = None
            continue
        cells = [c.strip().strip("`*") for c in line.strip().strip("|").split("|")]
        if cols is None:
            if "ID" in cells and "状態" in cells:
                cols = (cells.index("ID"), cells.index("状態"))
            continue
        if len(cells) > max(cols) and cells[cols[0]].startswith("phase-"):
            found.append((cells[cols[0]], (cells[cols[1]].split() or [""])[0]))
    return found


def frozen_dirs(root: str) -> set[str]:
    """全体実行で読み飛ばすディレクトリ (リポジトリ相対): archive 済みのロードマップと、凍結済みのフェーズ。"""
    dirs = {f"{ROADMAPS}/archive"}
    base = os.path.join(root, ROADMAPS)
    if not os.path.isdir(base):
        return dirs
    for rid in sorted(os.listdir(base)):
        path = os.path.join(base, rid, "roadmap.md")
        if rid == "archive" or not os.path.isfile(path):
            continue
        with open(path, encoding="utf-8") as f:
            src = f.read()
        dirs.update(f"{ROADMAPS}/{rid}/phases/{pid}" for pid, state in phase_states(src)
                    if state in FROZEN_PHASE_STATES)
    return dirs


def is_target_name(fn: str) -> bool:
    return fn.endswith(".md") and fn not in SKIP_NAMES


def targets(root: str, paths: list[str] | None, scope: list[str],
            excludes: list[str]) -> tuple[list[str], int]:
    """検査対象 (リポジトリ相対) と、全体実行で読み飛ばした凍結済み文書の件数を返す。"""
    if paths:
        # ディレクトリは走査して展開し、不在パスは例外 (main で exit 2) — 無言の「違反なし」を出さない。
        # 明示指定なので凍結済みの roadmap も読み飛ばさない
        return [rel for rel in LP.expand_paths(root, paths, (".md",), SKIP_NAMES)
                if not LP.is_excluded(rel, excludes)], 0
    frozen = frozen_dirs(root)
    found: list[str] = []
    skipped = 0
    for s in scope:
        base = os.path.join(root, s)
        if not os.path.isdir(base):
            continue
        for dirpath, dirnames, filenames in os.walk(base):
            # 凍結済みの子ディレクトリを刈る。scope 自体が凍結済みディレクトリなら刈らずに検査する (明示の指定として扱う)
            kept = []
            for d in sorted(dirnames):
                if LP.normalize_rel(os.path.join(dirpath, d), root) in frozen:
                    skipped += sum(1 for _dp, _dn, fns in os.walk(os.path.join(dirpath, d))
                                   for fn in fns if is_target_name(fn))
                else:
                    kept.append(d)
            dirnames[:] = kept
            for fn in sorted(filenames):
                if not is_target_name(fn):
                    continue
                rel = LP.normalize_rel(os.path.join(dirpath, fn), root)
                if not LP.is_excluded(rel, excludes):
                    found.append(rel)
    return sorted(found), skipped


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
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
    try:
        target_list, skipped = targets(root, paths, scope, excludes)
    except LP.PathNotFound as e:
        sys.stderr.write(f"--paths に存在しないパスがあります: {e} "
                         f"(複数パスは引数を分けて渡す。zsh の未クォート変数展開は単語分割されない)\n")
        return 2
    adr_checked = False
    for rel in target_list:
        full = os.path.join(root, rel)
        with open(full, encoding="utf-8") as f:
            src = f.read()
        adr = is_adr(rel) and adr_metrics(src)["status"] in ADR_CHECK_STATES
        adr_checked = adr_checked or adr
        e, w = check(src, th, adr)
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
        if adr_checked:
            print(f"  proposed の ADR の上限: タイトル {th['adr-title-chars']} 字 / Decision 節の散文 "
                  f"{th['adr-decision-chars']} 字 / 却下案 {th['adr-alternatives']} 件")
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
    if skipped:
        print(("\n" if by_file else "") +
              f"対象外: もう育たない roadmap の {skipped} ファイル (archive 済み・completed / dropped / promoted の"
              f"フェーズ。検査するなら --paths で指定する)")
    return 1 if total_e else 0


# ---------- 自己テスト ----------

def selftest() -> int:
    import subprocess
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    # 1 節に 11 項目 (section-items の既定 10 を超える) — 検査されれば必ず違反になる本文
    bad = "# t\n\n## 節\n" + "".join(f"- 項目{i}\n" for i in range(11))
    roadmap = "\n".join([
        "# live", "", "## フェーズ一覧",
        "| ID | 状態 | 種別 | Repo | フェーズ詳細 | Change |",
        "|---|---|---|---|---|---|",
        "| phase-1-done | completed | change | — | [agenda](phases/phase-1-done/agenda.md) | — |",
        "| phase-2-drop | dropped | research | — | [agenda](phases/phase-2-drop/agenda.md) | — |",
        "| phase-3-wip | in-progress | change | — | [agenda](phases/phase-3-wip/agenda.md) | — |",
        "",
    ])
    files = {
        "kasane/concepts/bad.md": bad,
        "kasane/roadmaps/archive/2026-01-01-old/phases/phase-1-a/agenda.md": bad,
        "kasane/roadmaps/live/roadmap.md": roadmap,
        "kasane/roadmaps/live/phases/phase-1-done/agenda.md": bad,
        "kasane/roadmaps/live/phases/phase-2-drop/history.md": bad,
        "kasane/roadmaps/live/phases/phase-3-wip/agenda.md": bad,
    }
    archived = "kasane/roadmaps/archive/2026-01-01-old/phases/phase-1-a/agenda.md"

    # ADR の決定の粒度: タイトル 150 字・Decision 節 2500 字・却下案 12 件 (表) を持つ本文。
    # 箇条書きを使わないため、一般の構造検査には掛からない (掛かるのは adr-* の 3 検査だけ)
    def adr(status: str, bloated: bool) -> str:
        title = "あ" * (150 if bloated else 40)
        decision = ("い" * 100 + "\n\n") * (25 if bloated else 5)
        if bloated:
            alts = "| 案 | 却下理由 |\n|---|---|\n" + "".join(f"| 案{i} | 理由 |\n" for i in range(12))
        else:
            alts = "".join(f"- 案{i}: 理由\n" for i in range(3))
        return (f"---\nid: 0001\ntitle: {title}\nstatus: {status}\ndate: 2026-01-01\n---\n\n"
                f"## Context\n\n背景\n\n## Decision\n\n{decision}"
                f"## Alternatives Considered\n\n{alts}\n## Consequences\n\n- 正: a\n- 負: b\n\n"
                f"## Revisit When\n\n- 前提が崩れたとき\n")

    files["kasane/decisions/core/0001-bloated-proposed.md"] = adr("proposed", True)
    files["kasane/decisions/core/0002-bloated-accepted.md"] = adr("accepted", True)
    files["kasane/decisions/core/0003-lean-proposed.md"] = adr("proposed", False)

    print("[フェーズ一覧の読み取り]")
    states = dict(phase_states(roadmap))
    check(states == {"phase-1-done": "completed", "phase-2-drop": "dropped", "phase-3-wip": "in-progress"},
          "Repo 列ありの表から ID と状態を引く", f"実際 {states}")

    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["git", "init", "-q"], cwd=tmp, check=True, capture_output=True)
        for rel, text in files.items():
            os.makedirs(os.path.dirname(os.path.join(tmp, rel)), exist_ok=True)
            with open(os.path.join(tmp, rel), "w", encoding="utf-8") as f:
                f.write(text)

        def run(*args: str) -> tuple[int, str]:
            proc = subprocess.run([sys.executable, os.path.abspath(__file__), *args],
                                  cwd=tmp, capture_output=True, text=True)
            return proc.returncode, proc.stdout + proc.stderr

        print("[全体実行]")
        code, out = run()
        check(code == 1, "違反ありで exit 1", f"実際 {code}")
        check("kasane/concepts/bad.md" in out, "scope 内の違反を検出する")
        check("phase-3-wip/agenda.md" in out, "進行中のフェーズは検出する")
        check("roadmaps/archive/" not in out, "archive 済みの roadmap は検出しない")
        check("phase-1-done/" not in out, "completed のフェーズは検出しない")
        check("phase-2-drop/" not in out, "dropped のフェーズは検出しない")
        check("3 ファイル" in out, "読み飛ばした件数を表示する")

        print("[--paths の明示指定]")
        code, out = run("--paths", "kasane/roadmaps/archive")
        check(code == 1 and archived in out, "archive を指定すれば検出する", f"exit {code}")
        code, out = run("--paths", "kasane/roadmaps/live/phases/phase-1-done/agenda.md")
        check(code == 1 and "phase-1-done/agenda.md" in out, "completed のフェーズを指定すれば検出する",
              f"exit {code}")

        print("[ADR の決定の粒度]")
        code, out = run("--paths", "kasane/decisions")
        check(code == 1 and "0001-bloated-proposed.md" in out, "proposed で粒度を超えた ADR を検出する", f"exit {code}")
        check("タイトルが 150 字" in out, "タイトルの字数を検出する")
        check("Decision 節の散文が 2500 字" in out, "Decision 節の散文字数を検出する")
        check("却下案が 12 件" in out, "表の却下案の件数を検出する (見出し行・区切り行は数えない)")
        check("0002-bloated-accepted.md" not in out, "accepted の ADR には掛けない")
        check("0003-lean-proposed.md" not in out, "粒度に収まる proposed の ADR は検出しない")

        print("[scope の明示指定]")
        with open(os.path.join(tmp, "kasane", "config.yaml"), "w", encoding="utf-8") as f:
            f.write("lint:\n  doc-structure:\n    scope: [kasane/roadmaps/archive]\n")
        code, out = run()
        check(code == 1 and archived in out, "scope 自体が archive なら検査する", f"exit {code}")

    print(f"\n自己テスト: {'全件 OK' if not failures else f'{failures} 件 NG'}")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
