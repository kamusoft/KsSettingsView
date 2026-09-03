#!/usr/bin/env python3
"""README のインストール例に書かれた version の置換と検査。

ルート README 2 枚 (英語 / 日本語) のインストール例には、貼ってそのまま使える依存宣言が
platform ごとに 1 行ずつある。リリースのたびにこの 6 行 (2 枚 x 3 行) を同じ version へ
揃えるのがこのスクリプトの仕事で、release workflow の validate は検査モードを呼ぶ。

対象は次の 3 行で、いずれもコードブロックの中にあるものだけを見る。同じ値が散文の説明にも
現れるが (prerelease の書き方の案内)、そちらは version を持たないプレースホルダなので
対象にしない。

  SwiftPM  .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "X.Y.Z")
  Maven    implementation("jp.kamusoft:kssettingsview:X.Y.Z")
  NuGet    <PackageReference Include="KsSettingsView.Maui" Version="X.Y.Z" />

SwiftPM の宣言は `exact:` で書く。`from:` は prerelease の tag を解決しないため、
prerelease を配る間も貼ってそのまま動く形にならない。現状が `from:` でも `exact:` でも
置換後は `exact:` に揃え、検査モードも `exact:` を要求する。

各ファイルで各対象がちょうど 1 行見つかることを前提とする。0 行または 2 行以上なら、
どの行を書き換えるべきか決まらないので、何も書き換えずに失敗する。

使い方:
  python3 scripts/release/set-readme-version.py <version>           # 置換
  python3 scripts/release/set-readme-version.py --check <version>   # 検査 (不一致で exit 1)
  python3 scripts/release/set-readme-version.py --selftest          # 自己テスト
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile

# 対象の README。どちらも同じ 3 行を持つ。
READMES = ["README.md", "README_ja.md"]

# コードブロックの境界。開始と終了の両方がこの形。
FENCE_RE = re.compile(r"^\s*```")

# 対象行の形。version 部分を group("version") に、SwiftPM は解決方法の語を
# group("keyword") に取る。前後は原文のまま残すので、インデントや行末の差異は保たれる。
SWIFTPM_RE = re.compile(
    r'^(?P<prefix>\s*\.package\(url:\s*'
    r'"https://github\.com/kamusoft/KsSettingsView-SPM(?:\.git)?",\s*)'
    r'(?P<keyword>from|exact)(?P<mid>:\s*")(?P<version>[^"]*)(?P<suffix>".*)$'
)
MAVEN_RE = re.compile(
    r'^(?P<prefix>\s*implementation\("jp\.kamusoft:kssettingsview:)'
    r'(?P<version>[^"]*)(?P<suffix>"\).*)$'
)
NUGET_RE = re.compile(
    r'^(?P<prefix>\s*<PackageReference Include="KsSettingsView\.Maui" Version=")'
    r'(?P<version>[^"]*)(?P<suffix>".*)$'
)

# (対象名, 正規表現)。対象名はエラー出力でどの行が見つからなかったかを示す。
TARGETS = [
    ("SwiftPM の依存宣言", SWIFTPM_RE),
    ("Maven 座標", MAVEN_RE),
    ("NuGet の PackageReference", NUGET_RE),
]


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def find_targets(lines: list[str]) -> dict[str, list[tuple[int, re.Match[str]]]]:
    """コードブロック内の対象行を対象名ごとに集める。

    戻り値の各要素は (0 始まりの行番号, マッチ結果)。ブロック外の行は見ないので、
    散文中に同じ字面が現れても拾わない。
    """
    found: dict[str, list[tuple[int, re.Match[str]]]] = {name: [] for name, _ in TARGETS}
    in_code_block = False

    for index, line in enumerate(lines):
        if FENCE_RE.match(line):
            in_code_block = not in_code_block
            continue
        if not in_code_block:
            continue
        for name, pattern in TARGETS:
            match = pattern.match(line.rstrip("\n"))
            if match is not None:
                found[name].append((index, match))

    return found


def rewrite(name: str, match: re.Match[str], version: str) -> str:
    """対象行を新しい version の行 (改行なし) へ書き換える。"""
    if name == "SwiftPM の依存宣言":
        # prerelease を解決させるため、`from:` で書かれていても `exact:` へ揃える。
        return f'{match.group("prefix")}exact{match.group("mid")}{version}{match.group("suffix")}'
    return f'{match.group("prefix")}{version}{match.group("suffix")}'


def collect(root: str) -> tuple[dict[str, tuple[list[str], dict[str, list[tuple[int, re.Match[str]]]]]], list[str]]:
    """全 README を読み、対象行を集める。1 行に確定できないものは problems に積む。"""
    parsed: dict[str, tuple[list[str], dict[str, list[tuple[int, re.Match[str]]]]]] = {}
    problems: list[str] = []

    for readme in READMES:
        path = os.path.join(root, readme)
        if not os.path.isfile(path):
            problems.append(f"{readme}: ファイルが無い")
            continue
        with open(path, encoding="utf-8") as f:
            lines = f.read().splitlines(keepends=True)
        found = find_targets(lines)
        for name, _ in TARGETS:
            count = len(found[name])
            if count != 1:
                problems.append(f"{readme}: {name} がコードブロック内に {count} 行ある (1 行であるべき)")
        parsed[readme] = (lines, found)

    return parsed, problems


def replace(root: str, version: str) -> int:
    parsed, problems = collect(root)
    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print("対象行を確定できないため置換しない", file=sys.stderr)
        return 1

    changed = 0
    for readme, (lines, found) in parsed.items():
        for name, _ in TARGETS:
            index, match = found[name][0]
            ending = "\n" if lines[index].endswith("\n") else ""
            lines[index] = rewrite(name, match, version) + ending
            changed += 1
        with open(os.path.join(root, readme), "w", encoding="utf-8") as f:
            f.write("".join(lines))

    print(f"{len(READMES)} 枚の README の {changed} 行を {version} にした")
    return 0


def check(root: str, version: str) -> int:
    parsed, problems = collect(root)

    for readme, (_lines, found) in parsed.items():
        for name, _ in TARGETS:
            if len(found[name]) != 1:
                continue
            index, match = found[name][0]
            actual = match.group("version")
            if actual != version:
                problems.append(
                    f"{readme}:{index + 1}: {name} の version が {version} でない (実際は {actual})"
                )
            if name == "SwiftPM の依存宣言" and match.group("keyword") != "exact":
                problems.append(
                    f"{readme}:{index + 1}: {name} は exact: で書く "
                    f"(実際は {match.group('keyword')}: — prerelease が解決されない)"
                )

    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print(f"README のインストール例が {version} と一致しない ({len(problems)} 件)", file=sys.stderr)
        return 1

    print(f"README のインストール例 {len(READMES) * len(TARGETS)} 行が {version} と一致する")
    return 0


# --- 自己テスト ------------------------------------------------------------------------
#
# 一時ディレクトリに置いた README のコピーに対してだけ実行し、リポジトリの README は
# 読むだけで書き換えない。

SELFTEST_README = """# Title

## Installation

### iOS

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
]
```

To pin a prerelease, use `exact: "X.Y.Z-beta.N"`.
The Maven coordinates are `implementation("jp.kamusoft:kssettingsview:0.0.0")` in prose.

### Android

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```

### .NET MAUI

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```
"""


def selftest() -> int:
    import contextlib
    import io

    failures = 0

    def report(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    def run(func, *args) -> tuple[int, str]:
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = func(*args)
        return code, buf.getvalue()

    def stage(root: str, contents: dict[str, str]) -> None:
        for name, text in contents.items():
            with open(os.path.join(root, name), "w", encoding="utf-8") as f:
                f.write(text)

    print("[置換]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, {name: SELFTEST_README for name in READMES})
        code, out = run(replace, tmp, "1.2.3-beta.4")
        report(code == 0, "置換が成功する", out.strip())

        after = {}
        for name in READMES:
            with open(os.path.join(tmp, name), encoding="utf-8") as f:
                after[name] = f.read()

        report(
            all(
                '.package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "1.2.3-beta.4")'
                in text for text in after.values()
            ),
            "SwiftPM の from: が exact: の新 version になる",
        )
        report(
            all('implementation("jp.kamusoft:kssettingsview:1.2.3-beta.4")' in text
                for text in after.values()),
            "Maven 座標が新 version になる",
        )
        report(
            all('<PackageReference Include="KsSettingsView.Maui" Version="1.2.3-beta.4" />' in text
                for text in after.values()),
            "NuGet の Version が新 version になる",
        )
        report(
            all('use `exact: "X.Y.Z-beta.N"`' in text for text in after.values()),
            "散文のプレースホルダは書き換えない",
        )
        report(
            all('`implementation("jp.kamusoft:kssettingsview:0.0.0")` in prose' in text
                for text in after.values()),
            "コードブロック外の同じ字面は書き換えない",
        )
        report(
            all(text.count("1.2.3-beta.4") == 3 for text in after.values()),
            "書き換わるのは 1 枚あたり 3 行だけ",
            "; ".join(f"{n}={t.count('1.2.3-beta.4')}" for n, t in after.items()),
        )

        code, out = run(check, tmp, "1.2.3-beta.4")
        report(code == 0, "置換後の検査が通る", out.strip())

        code, out = run(check, tmp, "9.9.9")
        report(code == 1, "別の version の検査は失敗する")
        report("README.md:9" in out, "不一致の行番号が出力される", out.strip())

    print("[該当行が確定できない場合]")
    with tempfile.TemporaryDirectory() as tmp:
        broken = SELFTEST_README.replace(
            '    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")\n',
            "    // 形の変わった宣言\n",
        )
        stage(tmp, {"README.md": broken, "README_ja.md": SELFTEST_README})
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が無ければ置換しない")
        report("README.md" in out and "SwiftPM" in out, "見つからなかったファイルと対象が出力される", out.strip())
        with open(os.path.join(tmp, "README_ja.md"), encoding="utf-8") as f:
            report('kssettingsview:0.1.0' in f.read(), "1 枚でも確定できなければ他方も書き換えない")

    with tempfile.TemporaryDirectory() as tmp:
        duplicated = SELFTEST_README.replace(
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n',
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n'
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n',
        )
        stage(tmp, {"README.md": duplicated, "README_ja.md": SELFTEST_README})
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が 2 行あれば置換しない")
        report("2 行ある" in out, "見つかった行数が出力される", out.strip())

    print("[実物の README に対する疎通]")
    root = repo_root()
    if not all(os.path.isfile(os.path.join(root, name)) for name in READMES):
        report(False, "リポジトリの README を読める", root)
    else:
        with tempfile.TemporaryDirectory() as tmp:
            for name in READMES:
                shutil.copyfile(os.path.join(root, name), os.path.join(tmp, name))
            code, out = run(replace, tmp, "9.9.9-rc.7")
            report(code == 0, "実物の README で 6 行を確定できる", out.strip())
            code, out = run(check, tmp, "9.9.9-rc.7")
            report(code == 0, "置換したコピーの検査が通る", out.strip())

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def usage() -> None:
    print(
        "使い方: set-readme-version.py <version> | --check <version> | --selftest",
        file=sys.stderr,
    )


def main(argv: list[str]) -> int:
    if len(argv) == 1 and argv[0] == "--selftest":
        return selftest()
    if len(argv) == 2 and argv[0] == "--check":
        version = argv[1]
    elif len(argv) == 1 and not argv[0].startswith("-"):
        version = argv[0]
    else:
        usage()
        return 2

    if not version or '"' in version:
        print(f'::error::version として使えない値: {version}', file=sys.stderr)
        return 2

    root = repo_root()
    if argv[0] == "--check":
        return check(root, version)
    return replace(root, version)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
