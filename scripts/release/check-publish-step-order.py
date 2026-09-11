#!/usr/bin/env python3
"""publish job の step 順序の検査。

保留中の deployment の引き継ぎは、publish が必要とする他の成果物の取得より前に読む。
後ろにあると、成果物の取得に失敗した attempt が引き継ぎを読まないまま終わり、前の attempt が
残した保留中の deployment がどの attempt からも見えなくなる。順序は step を並べ替えるだけで
崩れ、崩れても平時の実行は緑のままなので、機械的に検査する。

検査する内容:

  - 引き継ぎを取ってくる step (deployment の artifact を download する step) が 1 件ある
  - 引き継ぎを読む step (deployment-handover.sh inspect を呼ぶ step) が 1 件ある
  - 他の成果物を download する step が 1 件以上ある
  - 引き継ぎの download → 引き継ぎの読み込み → 他の成果物の download の順に並んでいる

読み込みが download より前にあっても平時は緑のまま進む (引き継ぎが無い初回として扱われ、
保留中の deployment を二重に作る) ので、この 2 つの前後関係も検査に含める。

使い方:
  python3 scripts/release/check-publish-step-order.py
  python3 scripts/release/check-publish-step-order.py --release-yml PATH
  python3 scripts/release/check-publish-step-order.py --selftest
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys

# 引き継ぎの置き場。download する step をこの path で見分ける。
HANDOVER_PATH_MARKER = "/deployment"

# 引き継ぎを読む step を見分ける呼び出し。
HANDOVER_INSPECT_MARKER = "deployment-handover.sh inspect"

# 成果物を download する step を見分ける action。
DOWNLOAD_ACTION_MARKER = "actions/download-artifact@"


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def publish_steps(text: str) -> list[tuple[str, str]]:
    """publish job の step を (名前, 本文) の並びで返す。

    job は 2 桁字下げのキー、step は `      - name:` で始まる。PyYAML に頼らないのは、
    この検査自体を CI の素の python3 で回すため。
    """
    steps: list[tuple[str, list[str]]] = []
    inside_job = False
    for line in text.splitlines():
        if re.match(r"^  [A-Za-z][\w-]*:\s*$", line):
            inside_job = line.strip() == "publish:"
            continue
        if not inside_job:
            continue
        match = re.match(r"^      - name:\s*(.+?)\s*$", line)
        if match:
            steps.append((match.group(1), []))
            continue
        if steps:
            steps[-1][1].append(line)
    return [(name, "\n".join(body)) for name, body in steps]


def check(release_yml: str) -> int:
    with open(release_yml, encoding="utf-8") as handle:
        text = handle.read()

    steps = publish_steps(text)
    if not steps:
        print("::error::publish job の step を読み取れない", file=sys.stderr)
        return 1

    handover_download: list[int] = []
    handover_inspect: list[int] = []
    other_downloads: list[int] = []
    for index, (_, body) in enumerate(steps):
        if HANDOVER_INSPECT_MARKER in body:
            handover_inspect.append(index)
        if DOWNLOAD_ACTION_MARKER not in body:
            continue
        if re.search(r"^\s+path:.*" + re.escape(HANDOVER_PATH_MARKER) + r"\s*$", body, re.MULTILINE):
            handover_download.append(index)
        else:
            other_downloads.append(index)

    errors: list[str] = []
    if len(handover_download) != 1:
        errors.append(f"引き継ぎを download する step が 1 件でない ({len(handover_download)} 件)")
    if len(handover_inspect) != 1:
        errors.append(f"引き継ぎを読む step が 1 件でない ({len(handover_inspect)} 件)")
    if not other_downloads:
        errors.append("他の成果物を download する step が無い")

    if not errors:
        first_other = min(other_downloads)
        first_other_name = steps[first_other][0]
        download_index = handover_download[0]
        inspect_index = handover_inspect[0]

        if inspect_index < download_index:
            errors.append(
                f"引き継ぎの読み込み ({steps[inspect_index][0]}) が"
                f" 引き継ぎの download ({steps[download_index][0]}) より前にある"
            )
        for index, what in ((download_index, "引き継ぎの download"),
                            (inspect_index, "引き継ぎの読み込み")):
            if index > first_other:
                errors.append(
                    f"{what} ({steps[index][0]}) が"
                    f" 他の成果物の取得 ({first_other_name}) より後ろにある"
                )

    if errors:
        for message in errors:
            print(f"::error::{message}", file=sys.stderr)
        return 1

    print(
        "publish の step 順序は正しい: "
        f"{steps[handover_download[0]][0]} → {steps[handover_inspect[0]][0]}"
        f" → {steps[min(other_downloads)][0]}"
    )
    return 0


def step_bounds(lines: list[str], name: str) -> tuple[int, int]:
    """step 1 件が占める行の範囲を返す。

    step の本文は名前の行より深く字下げされる。字下げが 6 未満の行 (次の job など) か、
    次の step の名前の行に当たったところで終わりとする。
    """
    start = lines.index(f"      - name: {name}")
    end = start + 1
    while end < len(lines):
        line = lines[end]
        if line.startswith("      - name: "):
            break
        if line.strip() and len(line) - len(line.lstrip(" ")) < 6:
            break
        end += 1
    return start, end


def move_step(text: str, name: str, before: str) -> str:
    """step を別の step の直前へ動かした workflow を返す (自己テストの入力を作る)。"""
    lines = text.splitlines()
    start, end = step_bounds(lines, name)
    block = lines[start:end]
    del lines[start:end]
    target, _ = step_bounds(lines, before)
    return "\n".join(lines[:target] + block + lines[target:]) + "\n"


def delete_step(text: str, name: str) -> str:
    """step を消した workflow を返す (自己テストの入力を作る)。"""
    lines = text.splitlines()
    start, end = step_bounds(lines, name)
    del lines[start:end]
    return "\n".join(lines) + "\n"


def selftest() -> int:
    """実物の workflow を土台に、順序を崩した入力で検査が落ちることを確かめる。

    この検査は「崩れていないこと」しか出力しないため、崩した入力を通してしまう形に
    退化しても平時は緑のままになる。検出力そのものをここで確かめる。
    """
    import tempfile

    release_yml = os.path.join(repo_root(), ".github", "workflows", "release.yml")
    with open(release_yml, encoding="utf-8") as handle:
        original = handle.read()

    handover_download = "Download previous deployment id"
    handover_inspect = "Inspect previous deployment handover"
    other_download = "Download Android artifacts"

    cases: list[tuple[str, str, int]] = [
        ("実物の workflow は検査を通る", original, 0),
        (
            "引き継ぎの読み込みを download より前へ動かすと落ちる",
            move_step(original, handover_inspect, handover_download),
            1,
        ),
        (
            "引き継ぎの読み込みを他の成果物の取得より後ろへ動かすと落ちる",
            move_step(original, handover_inspect, "Configure distribution repository access"),
            1,
        ),
        (
            "引き継ぎの download を他の成果物の取得より後ろへ動かすと落ちる",
            move_step(original, handover_download, "Configure distribution repository access"),
            1,
        ),
        ("引き継ぎの download を消すと落ちる", delete_step(original, handover_download), 1),
        (
            "引き継ぎの読み込みを消すと落ちる",
            original.replace(HANDOVER_INSPECT_MARKER, "handover-removed.sh read"),
            1,
        ),
        (
            "他の成果物の取得が無くなると落ちる",
            delete_step(
                delete_step(
                    delete_step(original, other_download),
                    "Download MAUI packages",
                ),
                "Download release notes",
            ),
            1,
        ),
    ]

    failures = 0
    with tempfile.TemporaryDirectory() as work:
        for index, (name, content, expected) in enumerate(cases):
            path = os.path.join(work, f"release-{index}.yml")
            with open(path, "w", encoding="utf-8") as handle:
                handle.write(content)
            # 検査の出力そのものは検証対象ではないので、終了コードだけを見る。
            stdout, stderr = sys.stdout, sys.stderr
            with open(os.devnull, "w", encoding="utf-8") as quiet:
                sys.stdout = sys.stderr = quiet
                try:
                    actual = check(path)
                finally:
                    sys.stdout, sys.stderr = stdout, stderr
            if actual == expected:
                print(f"  OK   {name}")
            else:
                print(f"  NG   {name} (exit {actual} / 期待 {expected})")
                failures += 1

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(add_help=True)
    parser.add_argument(
        "--release-yml",
        default=os.path.join(repo_root(), ".github", "workflows", "release.yml"),
    )
    parser.add_argument("--selftest", action="store_true")
    args = parser.parse_args(argv)
    if args.selftest:
        return selftest()
    return check(args.release_yml)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
