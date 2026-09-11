#!/usr/bin/env python3
"""Release ノートの組み立て。

GitHub Release の本文を、前回のリリース以降に `main` へ入った pull request の本文から作る
(cross/ADR-0030)。載せる内容は人が pull request 本文の `## Changes` セクションに明示的に
書いたものだけで、ラベル等からの推測は使わない。

対象の決め方:

  1. 起点の tag を選ぶ。次をすべて満たす Release のうち、`main` の first-parent 上で
     対象 commit からもっとも近いものの tag とする。
       - 今回の version ではない (Release 作成だけが失敗した再実行で起点がずれない)
       - draft ではなく公開済みである
       - その tag が `main` の first-parent 上で対象 commit の祖先である
     満たす Release が 1 つも無ければ (初回のリリース) 履歴の最初から今回の commit までを
     範囲とする。
  2. 起点と対象 commit の間に入った commit に紐づく pull request を引き、base が `main` の
     ものだけを残して pull request 単位で重複を除く。

記載の文法は閉じた形にし、認識できない入力は黙って無視せず失敗させる:

  - `## Changes` の見出しはちょうど 1 つ。見出しの直後から次の見出しまたは本文末までが範囲
  - 範囲の空行でない行は、すべて `- <種別>: <説明>` または `- none`
  - 種別は breaking / feature / fix / docs の 4 つ
  - 説明は前後の空白を除いて非空
  - `- none` は単独でのみ許し、項目が 0 個の空セクションは失敗

使い方:
  python3 scripts/release/build-release-notes.py collect \\
      --repo OWNER/NAME --version X.Y.Z --sha COMMIT --out DIR
  python3 scripts/release/build-release-notes.py render \\
      --pulls pulls.json --repo OWNER/NAME --version X.Y.Z [--base-tag TAG]
  python3 scripts/release/build-release-notes.py --selftest

`collect` は GitHub API を呼ぶ。token は環境変数 GH_TOKEN (無ければ GITHUB_TOKEN) から読む。
`render` は API も git も使わない純粋な組み立てで、pull request 本文の集合を模した JSON
(`[{"number": 42, "body": "..."}]`) を入力に取る。
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request

# 種別と、ノートに出す見出し。並び順もこの定義の順。
# 見出しは Release ページに出る公開物なので英語で書く (利用者の言語圏を仮定しない)。
KINDS: list[tuple[str, str]] = [
    ("breaking", "Breaking Changes"),
    ("feature", "Features"),
    ("fix", "Bug Fixes"),
    ("docs", "Documentation"),
]

# 利用者向けの変更を書くセクションの見出しと、変更が無いことを示す単独の記載。
SECTION_HEADING_PATTERN = re.compile(r"^##[ \t]+Changes[ \t]*$")
NONE_MARKER = "- none"

# 対象とする pull request の base ブランチ。
TARGET_BASE_BRANCH = "main"

# 1 ページあたりの取得件数の上限 (GitHub API の上限値)。
PER_PAGE = 100

NOTES_FILENAME = "notes.md"
META_FILENAME = "meta.json"


class NotesError(Exception):
    """入力の記載が文法に合わない、または対象を決められないときに投げる。"""


# ---------------------------------------------------------------- 解析と整形


def split_lines(text: str) -> list[str]:
    """改行コードの違いを吸収して行に分ける。"""
    return text.replace("\r\n", "\n").replace("\r", "\n").split("\n")


def extract_section(body: str) -> list[str]:
    """pull request 本文から `## Changes` の範囲を取り出す。"""
    lines = split_lines(body or "")
    heads = [i for i, line in enumerate(lines) if SECTION_HEADING_PATTERN.match(line)]
    if not heads:
        raise NotesError("`## Changes` セクションが無い")
    if len(heads) > 1:
        raise NotesError(f"`## Changes` の見出しが {len(heads)} 個ある (1 つだけ書く)")
    start = heads[0] + 1
    end = len(lines)
    for index in range(start, len(lines)):
        if re.match(r"^#{1,6}[ \t]", lines[index]):
            end = index
            break
    return lines[start:end]


def parse_items(body: str) -> list[tuple[str, str]]:
    """pull request 本文から (種別, 説明) の並びを返す。変更なしの記載では空を返す。"""
    kinds = {key for key, _ in KINDS}
    items: list[tuple[str, str]] = []
    none_written = False
    for raw in extract_section(body):
        line = raw.rstrip()
        if not line.strip():
            continue
        if line == NONE_MARKER:
            none_written = True
            continue
        match = re.match(r"^-[ ]+([A-Za-z]+):(.*)$", line)
        if not match:
            raise NotesError(
                f"認識できない行: 「{line}」"
                " (`- <種別>: <説明>` または `- none` の形で書く)"
            )
        kind = match.group(1)
        description = match.group(2).strip()
        if kind not in kinds:
            raise NotesError(
                f"種別が不正: 「{line}」"
                " (使えるのは breaking / feature / fix / docs)"
            )
        if not description:
            raise NotesError(f"説明が空: 「{line}」")
        items.append((kind, description))
    if none_written and items:
        raise NotesError("`- none` は単独でのみ書ける (他の項目と併記されている)")
    if not none_written and not items:
        raise NotesError(
            "`## Changes` に項目が 1 つも無い"
            " (利用者向けの変更が無ければ `- none` と書く)"
        )
    return items


def parse_pulls(pulls: list[dict]) -> list[tuple[int, list[tuple[str, str]]]]:
    """pull request の並びを解析する。失敗したときはどの pull request かを添えて投げる。"""
    parsed: list[tuple[int, list[tuple[str, str]]]] = []
    for pull in pulls:
        number = pull["number"]
        try:
            parsed.append((number, parse_items(pull.get("body") or "")))
        except NotesError as error:
            raise NotesError(f"#{number}: {error}") from error
    return parsed


def render_notes(pulls: list[dict], repo: str, version: str, base_tag: str | None) -> str:
    """解析した pull request の並びから Release 本文を作る。"""
    parsed = parse_pulls(pulls)
    grouped: dict[str, list[str]] = {key: [] for key, _ in KINDS}
    for number, items in parsed:
        for kind, description in items:
            grouped[kind].append(f"- {description} (#{number})")

    blocks: list[str] = ["## What's Changed"]
    if any(grouped[key] for key, _ in KINDS):
        for key, title in KINDS:
            if not grouped[key]:
                continue
            blocks.append(f"### {title}\n" + "\n".join(grouped[key]))
    else:
        blocks.append("No user-facing changes in this release.")

    if base_tag:
        blocks.append(
            f"**Full Changelog**: https://github.com/{repo}/compare/{base_tag}...{version}"
        )
    else:
        blocks.append("This is the first release.")
    return "\n\n".join(blocks) + "\n"


# ---------------------------------------------------------------- git の照会


def git(repo_dir: str, *args: str) -> str:
    """git を実行して標準出力を返す。"""
    result = subprocess.run(
        ["git", "-C", repo_dir, *args],
        capture_output=True, text=True, check=True,
    )
    return result.stdout.strip()


def resolve_commit(repo_dir: str, rev: str) -> str | None:
    """rev が指す commit を返す。解決できなければ None。"""
    try:
        return git(repo_dir, "rev-list", "-n", "1", rev)
    except subprocess.CalledProcessError:
        return None


def first_parent_chain(repo_dir: str, sha: str) -> list[str]:
    """対象 commit から辿れる first-parent の並びを返す (先頭が対象 commit)。"""
    out = git(repo_dir, "rev-list", "--first-parent", sha)
    return [line for line in out.splitlines() if line]


def commits_in_range(repo_dir: str, base_sha: str | None, sha: str) -> list[str]:
    """起点から対象 commit までの間に入った commit を返す。起点が無ければ履歴の最初から。"""
    spec = f"{base_sha}..{sha}" if base_sha else sha
    out = git(repo_dir, "rev-list", spec)
    return [line for line in out.splitlines() if line]


def select_base(
    releases: list[dict], version: str, repo_dir: str, sha: str
) -> tuple[str, str] | None:
    """起点となる Release の (tag, commit) を返す。該当が無ければ None。"""
    chain = first_parent_chain(repo_dir, sha)
    distance = {commit: index for index, commit in enumerate(chain)}
    best: tuple[int, str, str] | None = None
    for release in releases:
        tag = release.get("tag_name") or ""
        if not tag or tag == version or release.get("draft"):
            continue
        commit = resolve_commit(repo_dir, f"refs/tags/{tag}")
        if commit is None or commit not in distance:
            continue
        candidate = (distance[commit], tag, commit)
        if best is None or candidate[0] < best[0]:
            best = candidate
    if best is None:
        return None
    return best[1], best[2]


# ---------------------------------------------------------------- GitHub API


class GitHubApi:
    """GitHub API の読み取り。ページ送りを辿って全件を返す。"""

    def __init__(self, token: str, base_url: str = "https://api.github.com") -> None:
        self._token = token
        self._base_url = base_url.rstrip("/")

    def _get(self, path: str, page: int) -> list[dict]:
        url = f"{self._base_url}{path}?per_page={PER_PAGE}&page={page}"
        request = urllib.request.Request(url)
        request.add_header("Accept", "application/vnd.github+json")
        request.add_header("X-GitHub-Api-Version", "2022-11-28")
        request.add_header("Authorization", f"Bearer {self._token}")
        with urllib.request.urlopen(request, timeout=60) as response:
            return json.loads(response.read().decode("utf-8"))

    def paged(self, path: str) -> list[dict]:
        """1 ページに収まらない件数でも取りこぼさずに全件を返す。

        取りこぼしは失敗にならず「項目の足りないノート」として静かに成功するため、
        最後のページ (取得件数が上限未満) に到達するまで辿る。
        """
        items: list[dict] = []
        page = 1
        while True:
            chunk = self._get(path, page)
            items.extend(chunk)
            if len(chunk) < PER_PAGE:
                return items
            page += 1


def collect_pulls(api, repo: str, commits: list[str]) -> list[dict]:
    """commit に紐づく pull request のうち base が `main` のものを、マージ順で返す。"""
    found: dict[int, dict] = {}
    for commit in commits:
        for pull in api.paged(f"/repos/{repo}/commits/{commit}/pulls"):
            if (pull.get("base") or {}).get("ref") != TARGET_BASE_BRANCH:
                continue
            number = pull["number"]
            # 同じ pull request が複数の commit に紐づいても 1 度だけ数える。
            if number in found:
                continue
            found[number] = {
                "number": number,
                "body": pull.get("body") or "",
                "merged_at": pull.get("merged_at") or "",
            }
    return sorted(found.values(), key=lambda pull: (pull["merged_at"], pull["number"]))


# ---------------------------------------------------------------- 収集の実行


def collect(
    api, repo: str, version: str, sha: str, repo_dir: str, out_dir: str
) -> tuple[str, dict]:
    """対象を集めて検査し、確定したノート本文と対象の内訳を返す。"""
    releases = api.paged(f"/repos/{repo}/releases")
    base = select_base(releases, version, repo_dir, sha)
    base_tag, base_sha = base if base else (None, None)
    commits = commits_in_range(repo_dir, base_sha, sha)
    pulls = collect_pulls(api, repo, commits)
    notes = render_notes(pulls, repo, version, base_tag)
    meta = {
        "version": version,
        "base-tag": base_tag,
        "commit": sha,
        "pulls": [pull["number"] for pull in pulls],
    }
    if out_dir:
        os.makedirs(out_dir, exist_ok=True)
        with open(os.path.join(out_dir, NOTES_FILENAME), "w", encoding="utf-8") as handle:
            handle.write(notes)
        with open(os.path.join(out_dir, META_FILENAME), "w", encoding="utf-8") as handle:
            json.dump(meta, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
    return notes, meta


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


# ---------------------------------------------------------------- 自己テスト


class FakeApi:
    """自己テスト用の API。path ごとに返す並びを持ち、ページ送りを模す。"""

    def __init__(self, responses: dict[str, list[dict]]) -> None:
        self._responses = responses
        self.requested: list[str] = []

    def paged(self, path: str) -> list[dict]:
        self.requested.append(path)
        return list(self._responses.get(path, []))


class PagedGitHubApi(GitHubApi):
    """ページ送りの検査用。`GitHubApi` の 1 ページ取得だけを差し替え、辿る処理は本物を通す。

    ページ送りを自己テスト側へ書き写すと、実装が 1 ページで打ち切る形に退行しても
    検査は緑のままになる。ここで差し替えるのは HTTP 要求だけに留める。
    """

    def __init__(self, pages: list[list[dict]]) -> None:
        super().__init__("")
        self._pages = pages
        self.requested_pages: list[int] = []

    def _get(self, path: str, page: int) -> list[dict]:
        self.requested_pages.append(page)
        if 1 <= page <= len(self._pages):
            return list(self._pages[page - 1])
        # 範囲を越えた要求には空を返す (GitHub API の最終ページ以降と同じ振る舞い)。
        return []


def changes_body(*lines: str) -> str:
    """`## Changes` を持つ pull request 本文を組み立てる (自己テストの入力)。"""
    return "## Summary\n\n本文\n\n## Changes\n\n" + "\n".join(lines) + "\n"


def run_git(repo_dir: str, *args: str) -> None:
    subprocess.run(["git", "-C", repo_dir, *args], check=True, capture_output=True)


def build_history(repo_dir: str) -> dict[str, str]:
    """自己テスト用の git 履歴を作り、名前から commit への対応を返す。

    first-parent の本線に 3 つの commit を置き、本線から外れた枝にも tag を打つ。
    """
    run_git(repo_dir, "init", "--initial-branch=main")
    run_git(repo_dir, "config", "user.email", "release@example.invalid")
    run_git(repo_dir, "config", "user.name", "release")
    commits: dict[str, str] = {}
    for name in ("first", "second", "third"):
        with open(os.path.join(repo_dir, f"{name}.txt"), "w", encoding="utf-8") as handle:
            handle.write(name)
        run_git(repo_dir, "add", "-A")
        run_git(repo_dir, "commit", "-m", name)
        commits[name] = git(repo_dir, "rev-parse", "HEAD")
    run_git(repo_dir, "tag", "0.1.0-beta.1", commits["first"])
    run_git(repo_dir, "tag", "0.1.0-beta.2", commits["second"])
    # リリースに対応しない、作業中に作られた tag。直前の Release の tag より後に置く。
    run_git(repo_dir, "tag", "9.9.9", commits["third"])
    # 今回の version の tag (再実行で既に存在する状態)。
    run_git(repo_dir, "tag", "0.1.0-beta.3", commits["third"])
    # 本線から外れた枝の commit に打った tag。
    run_git(repo_dir, "checkout", "--quiet", "-b", "side", commits["first"])
    with open(os.path.join(repo_dir, "side.txt"), "w", encoding="utf-8") as handle:
        handle.write("side")
    run_git(repo_dir, "add", "-A")
    run_git(repo_dir, "commit", "-m", "side")
    commits["side"] = git(repo_dir, "rev-parse", "HEAD")
    run_git(repo_dir, "tag", "0.9.9", commits["side"])
    run_git(repo_dir, "checkout", "--quiet", "main")
    return commits


def selftest_parsing() -> list[tuple[str, bool]]:
    """記載の解析と整形を検査する。結果は (名前, 成否) の並びで返す。"""
    repo = "kamusoft/KsSettingsView"
    results: list[tuple[str, bool]] = []

    def check(name: str, passed: bool) -> None:
        results.append((name, passed))

    def fails(pulls: list[dict], fragment: str) -> bool:
        try:
            render_notes(pulls, repo, "1.0.0", "0.9.0")
        except NotesError as error:
            return fragment in str(error)
        return False

    notes = render_notes(
        [
            {"number": 42, "body": changes_body(
                "- fix: 塗り残しを直した",
                "- breaking: 色指定が非 null になった",
            )},
            {"number": 45, "body": changes_body("- feature: 行の高さを指定できる")},
        ],
        repo, "1.0.0", "0.9.0",
    )
    check("種別ごとにまとまり、定義順に並ぶ",
          notes.index("Breaking Changes") < notes.index("Features")
          < notes.index("Bug Fixes"))
    check("項目の無い種別の見出しは出さない", "Documentation" not in notes)
    check("複数 pull request 分が連結される",
          "- 行の高さを指定できる (#45)" in notes and "- 塗り残しを直した (#42)" in notes)
    check("出所の pull request 番号が付く", "(#42)" in notes and "(#45)" in notes)
    check("比較の位置が末尾に入る",
          notes.rstrip().endswith(f"https://github.com/{repo}/compare/0.9.0...1.0.0"))

    same = render_notes(
        [
            {"number": 1, "body": changes_body("- fix: 同じ文面")},
            {"number": 2, "body": changes_body("- fix: 同じ文面")},
        ],
        repo, "1.0.0", "0.9.0",
    )
    check("異なる pull request の同じ文面は両方残る",
          "- 同じ文面 (#1)" in same and "- 同じ文面 (#2)" in same)

    none_only = render_notes([{"number": 7, "body": changes_body("- none")}], repo, "1.0.0", "0.9.0")
    check("`- none` だけの pull request からは項目が載らない",
          "No user-facing changes in this release." in none_only and "###" not in none_only)

    empty = render_notes([], repo, "1.0.0", None)
    check("対象が 0 件でも本文が決まる",
          "No user-facing changes in this release." in empty)
    check("初回のリリースでは比較の位置を含めない",
          "compare" not in empty and "This is the first release." in empty)

    check("セクションが無いと失敗する",
          fails([{"number": 3, "body": "## Summary\n\n本文\n"}], "セクションが無い"))
    check("見出しが 2 つあると失敗する",
          fails([{"number": 3, "body": changes_body("- fix: x") + "\n## Changes\n\n- fix: y\n"}],
                "見出しが 2 個ある"))
    check("空のセクションは失敗する",
          fails([{"number": 3, "body": "## Changes\n\n## 次\n"}], "項目が 1 つも無い"))
    check("不正な種別は失敗する",
          fails([{"number": 3, "body": changes_body("- chore: 片付け")}], "種別が不正"))
    check("認識できない非空行は失敗する",
          fails([{"number": 3, "body": changes_body("* feature: 別記法")}], "認識できない行"))
    check("地の文は失敗する",
          fails([{"number": 3, "body": changes_body("No user-facing changes in this release.")}],
                "認識できない行"))
    check("空の説明は失敗する",
          fails([{"number": 3, "body": changes_body("- fix:")}], "説明が空"))
    check("`- none` と項目の共存は失敗する",
          fails([{"number": 3, "body": changes_body("- none", "- fix: 直した")}],
                "単独でのみ書ける"))
    check("失敗の原因が pull request 番号で分かる",
          fails([{"number": 91, "body": "## Summary\n"}], "#91: "))

    template = os.path.join(repo_root(), ".github", "pull_request_template.md")
    if os.path.exists(template):
        with open(template, encoding="utf-8") as handle:
            body = handle.read()
        try:
            check("pull request テンプレートの未編集状態を受理する", parse_items(body) == [])
        except NotesError:
            check("pull request テンプレートの未編集状態を受理する", False)
    else:
        check("pull request テンプレートが存在する", False)
    return results


def selftest_selection() -> list[tuple[str, bool]]:
    """起点と対象の決め方を、模した Release 一覧と一時的な git 履歴で検査する。"""
    import tempfile

    results: list[tuple[str, bool]] = []
    with tempfile.TemporaryDirectory() as work:
        commits = build_history(work)
        head = commits["third"]
        published = [
            {"tag_name": "0.1.0-beta.1", "draft": False},
            {"tag_name": "0.1.0-beta.2", "draft": False},
        ]

        base = select_base(published, "0.1.0-beta.3", work, head)
        results.append(("直前の公開済み Release が起点になる", base == ("0.1.0-beta.2", commits["second"])))

        with_untagged_release = published + [{"tag_name": "0.1.0-beta.3", "draft": False}]
        results.append((
            "今回の version の tag は起点にならない",
            select_base(with_untagged_release, "0.1.0-beta.3", work, head)
            == ("0.1.0-beta.2", commits["second"]),
        ))
        results.append((
            "Release を持たない tag は起点にならない",
            select_base(published, "0.1.0-beta.3", work, head)[0] == "0.1.0-beta.2",
        ))
        with_draft = published + [{"tag_name": "0.1.0-beta.3", "draft": True}]
        results.append((
            "draft の Release は起点にならない",
            select_base(with_draft, "0.1.0-beta.4", work, head) == ("0.1.0-beta.2", commits["second"]),
        ))
        with_side = published + [{"tag_name": "0.9.9", "draft": False}]
        results.append((
            "対象 commit の祖先でない Release は起点にならない",
            select_base(with_side, "0.1.0-beta.3", work, head) == ("0.1.0-beta.2", commits["second"]),
        ))
        results.append((
            "公開済み Release が無ければ起点も無い",
            select_base([], "0.1.0-beta.3", work, head) is None,
        ))
        results.append((
            "起点が無ければ履歴の最初から集める",
            len(commits_in_range(work, None, head)) == 3,
        ))
        results.append((
            "起点があればその後の commit だけを集める",
            commits_in_range(work, commits["second"], head) == [commits["third"]],
        ))

        # 同じ pull request が 2 つの commit に紐づき、base が main でないものが混ざる状態。
        main_pull = {"number": 42, "base": {"ref": "main"}, "merged_at": "2026-09-01T00:00:00Z",
                     "body": changes_body("- fix: 直した")}
        develop_pull = {"number": 41, "base": {"ref": "develop"}, "merged_at": "2026-08-31T00:00:00Z",
                        "body": changes_body("- fix: 直した")}
        api = FakeApi({
            f"/repos/x/y/commits/{commits['second']}/pulls": [main_pull, develop_pull],
            f"/repos/x/y/commits/{commits['third']}/pulls": [main_pull],
        })
        pulls = collect_pulls(api, "x/y", [commits["second"], commits["third"]])
        results.append(("同じ pull request は 1 度だけ数える", [p["number"] for p in pulls] == [42]))
        results.append(("開発ブランチ宛ての pull request は対象にならない",
                        all(p["number"] != 41 for p in pulls)))

        ordered = collect_pulls(
            FakeApi({
                f"/repos/x/y/commits/{commits['third']}/pulls": [
                    {"number": 50, "base": {"ref": "main"}, "merged_at": "2026-09-05T00:00:00Z",
                     "body": changes_body("- fix: 後")},
                    {"number": 49, "base": {"ref": "main"}, "merged_at": "2026-09-02T00:00:00Z",
                     "body": changes_body("- fix: 先")},
                ],
            }),
            "x/y", [commits["third"]],
        )
        results.append(("対象はマージ順に並ぶ", [p["number"] for p in ordered] == [49, 50]))

        # 収集から整形までを通しで実行する (Release の取得も模した API から返す)。
        collect_api = FakeApi({
            "/repos/x/y/releases": published,
            f"/repos/x/y/commits/{commits['third']}/pulls": [main_pull],
        })
        notes, meta = collect(collect_api, "x/y", "0.1.0-beta.3", head, work, "")
        results.append(("収集から整形までが通る", "- 直した (#42)" in notes))
        results.append(("対象の内訳が残る",
                        meta["base-tag"] == "0.1.0-beta.2" and meta["pulls"] == [42]
                        and meta["commit"] == head))
    return results


def selftest_paging() -> list[tuple[str, bool]]:
    """1 ページに収まらない件数でも取りこぼさないことを検査する。"""
    full_page = [{"number": i, "base": {"ref": "main"},
                  "merged_at": f"2026-09-01T00:00:{i:02d}Z",
                  "body": changes_body(f"- fix: 修正 {i}")} for i in range(PER_PAGE)]
    last_page = [{"number": PER_PAGE, "base": {"ref": "main"},
                  "merged_at": "2026-09-02T00:00:00Z",
                  "body": changes_body("- fix: 最後")}]

    api = PagedGitHubApi([full_page, last_page])
    pulls = collect_pulls(api, "x/y", ["sha"])

    # 最後のページがちょうど上限件数で、次のページが空で返る場合。件数だけでは
    # 終わりを判定できないため、空ページまで辿れることを別に確かめる。
    boundary_api = PagedGitHubApi([full_page])
    boundary_pulls = collect_pulls(boundary_api, "x/y", ["sha"])

    return [
        ("ページ送りで全件を取る", len(pulls) == PER_PAGE + 1),
        ("最後のページまで辿る", api.requested_pages == [1, 2]),
        ("2 ページ目の項目がノートに載る",
         "- 最後 (#100)" in render_notes(pulls, "x/y", "1.0.0", "0.9.0")),
        ("上限ちょうどのページの次を空で受けて終わる", len(boundary_pulls) == PER_PAGE),
        ("上限ちょうどのページの後も次を要求する", boundary_api.requested_pages == [1, 2]),
    ]


def selftest() -> int:
    results: list[tuple[str, bool]] = []
    results += selftest_parsing()
    results += selftest_selection()
    results += selftest_paging()

    failures = 0
    for name, passed in results:
        if passed:
            print(f"  OK   {name}")
        else:
            print(f"  NG   {name}")
            failures += 1
    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


# ---------------------------------------------------------------- 入口


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument("--selftest", action="store_true")
    subparsers = parser.add_subparsers(dest="command")

    collect_parser = subparsers.add_parser("collect")
    collect_parser.add_argument("--repo", required=True)
    collect_parser.add_argument("--version", required=True)
    collect_parser.add_argument("--sha", required=True)
    collect_parser.add_argument("--out", required=True)
    collect_parser.add_argument("--repo-dir", default=repo_root())

    render_parser = subparsers.add_parser("render")
    render_parser.add_argument("--pulls", required=True)
    render_parser.add_argument("--repo", required=True)
    render_parser.add_argument("--version", required=True)
    render_parser.add_argument("--base-tag", default=None)

    args = parser.parse_args(argv)
    if args.selftest:
        return selftest()

    if args.command == "render":
        with open(args.pulls, encoding="utf-8") as handle:
            pulls = json.load(handle)
        try:
            sys.stdout.write(render_notes(pulls, args.repo, args.version, args.base_tag))
        except NotesError as error:
            print(f"::error::Release ノートの記載が不正: {error}", file=sys.stderr)
            return 1
        return 0

    if args.command == "collect":
        token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN") or ""
        if not token:
            print("::error::GitHub API の token が渡っていない (GH_TOKEN)", file=sys.stderr)
            return 1
        api = GitHubApi(token, os.environ.get("GITHUB_API_URL", "https://api.github.com"))
        try:
            _, meta = collect(api, args.repo, args.version, args.sha, args.repo_dir, args.out)
        except NotesError as error:
            print(f"::error::Release ノートの記載が不正: {error}", file=sys.stderr)
            return 1
        except urllib.error.URLError as error:
            print(f"::error::GitHub API の照会に失敗した: {error}", file=sys.stderr)
            return 1
        print(
            f"起点: {meta['base-tag'] or '(初回)'} / 対象 commit: {meta['commit']}"
            f" / 対象 pull request: {meta['pulls'] or '(なし)'}"
        )
        return 0

    parser.print_help()
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
