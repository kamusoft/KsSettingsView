#!/usr/bin/env python3
"""インストール例の契約の検査。

貼ってそのまま使える依存宣言は、ルート README 2 枚 (英語 / 日本語) と利用者向け Skill
4 本 x 2 言語 (`skills/{en,ja}/kssettingsview-*/SKILL.md`) の導入節にある。これらの例は
具体的な version を持たず、version の位置にはプレースホルダ `{version}` を置く。埋め
忘れは依存解決の失敗として利用者に必ず露見し、リリースのたびに書き換える必要もない。

検査する契約は次の 4 つ。

  1. 対象の各行がプレースホルダを持ち、具体的な version を持たない
  2. SwiftPM の依存宣言が `exact:` で書かれている
  3. 各ファイルに最新リリースへの案内がある
  4. 英語版と日本語版が同じ Skill の集合を持ち、対応する Skill が同じ種類・同じ本数の
     インストール宣言を持つ

対象は次の 3 種で、いずれもコードブロックの中にあるものだけを見る。同じ字面が散文の
説明にも現れる (プレースホルダの読み方や prerelease の書き方の案内) が、そちらは対象に
しない。

  SwiftPM  .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "{version}")
  Maven    implementation("jp.kamusoft:kssettingsview:{version}")
  NuGet    <PackageReference Include="KsSettingsView.Maui" Version="{version}" />

どの種をどのファイルに期待するかは TARGET_FILES で持つ。README は 3 種すべてを 1 行ずつ
持ち、Skill は自 platform の 1 種だけを持つ (iOS: SwiftPM / Android: Maven /
MAUI・AiForms 移行: NuGet)。期待していない種がそのファイルに現れた場合も違反とする。

対象表だけでは、新しい Skill が表に登録されないまま増えたときにその存在を検出できない。
このため `skills/{en,ja}/*/SKILL.md` の実構成も列挙し、表に無い Skill・表にあるのに実在
しない Skill・英日で食い違う Skill をいずれも失敗とする。

使い方:
  python3 scripts/install-example-lint.py            # 検査 (違反があれば exit 1)
  python3 scripts/install-example-lint.py --selftest # 各検査項目の検出力の確認
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

# version の位置に置くプレースホルダ。3 経路すべてで構文としては合法だが、
# どこでも version として解決できない。
PLACEHOLDER = "{version}"

# 最新リリースへの案内先。この URL は常にその時点の最新リリースへ解決される。
LATEST_RELEASE_URL = "https://github.com/kamusoft/KsSettingsView/releases/latest"

# 対象の種別名。エラー出力でどの行が問題かを示す文字列でもある。
SWIFTPM = "SwiftPM の依存宣言"
MAVEN = "Maven 座標"
NUGET = "NuGet の PackageReference"
KIND_NAMES = (SWIFTPM, MAVEN, NUGET)

# コードブロックの境界。開始と終了の両方がこの形。
FENCE_RE = re.compile(r"^\s*```")

# 対象行の形。version 部分を group("version") に、SwiftPM は解決方法の語を
# group("keyword") に取る。
SWIFTPM_RE = re.compile(
    r'^\s*\.package\(url:\s*'
    r'"https://github\.com/kamusoft/KsSettingsView-SPM(?:\.git)?",\s*'
    r'(?P<keyword>from|exact):\s*"(?P<version>[^"]*)".*$'
)
MAVEN_RE = re.compile(
    r'^\s*implementation\("jp\.kamusoft:kssettingsview:(?P<version>[^"]*)"\).*$'
)
NUGET_RE = re.compile(
    r'^\s*<PackageReference Include="KsSettingsView\.Maui" Version="(?P<version>[^"]*)".*$'
)

PATTERNS = {SWIFTPM: SWIFTPM_RE, MAVEN: MAVEN_RE, NUGET: NUGET_RE}

# 対象ファイルと、そのファイルで期待する種別。パスは "/" 区切りのリポジトリ相対。
TARGET_FILES: list[tuple[str, tuple[str, ...]]] = [
    ("README.md", (SWIFTPM, MAVEN, NUGET)),
    ("README_ja.md", (SWIFTPM, MAVEN, NUGET)),
    ("skills/en/kssettingsview-ios/SKILL.md", (SWIFTPM,)),
    ("skills/ja/kssettingsview-ios/SKILL.md", (SWIFTPM,)),
    ("skills/en/kssettingsview-android/SKILL.md", (MAVEN,)),
    ("skills/ja/kssettingsview-android/SKILL.md", (MAVEN,)),
    ("skills/en/kssettingsview-maui/SKILL.md", (NUGET,)),
    ("skills/ja/kssettingsview-maui/SKILL.md", (NUGET,)),
    ("skills/en/kssettingsview-aiforms-migration/SKILL.md", (NUGET,)),
    ("skills/ja/kssettingsview-aiforms-migration/SKILL.md", (NUGET,)),
]

# 全ファイルの期待行数の合計。検査の成功メッセージで使う。
TOTAL_TARGET_LINES = sum(len(names) for _, names in TARGET_FILES)

# 利用者向け Skill の言語ディレクトリ。
LANGUAGES = ("en", "ja")

# 対象表に載る Skill のパスの形。group(1) が言語、group(2) が Skill 名。
SKILL_PATH_RE = re.compile(r"^skills/(en|ja)/([^/]+)/SKILL\.md$")


def repo_root() -> str:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        return out or os.getcwd()
    except Exception:
        return os.getcwd()


def local_path(root: str, relative: str) -> str:
    """"/" 区切りのリポジトリ相対パスを、この OS のパスへ組み立てる。"""
    return os.path.join(root, *relative.split("/"))


def registered_skills() -> dict[str, set[str]]:
    """対象表が登録している Skill 名を言語ごとに集める。"""
    result: dict[str, set[str]] = {language: set() for language in LANGUAGES}
    for relative, _names in TARGET_FILES:
        match = SKILL_PATH_RE.match(relative)
        if match is not None:
            result[match.group(1)].add(match.group(2))
    return result


def actual_skills(root: str) -> dict[str, set[str]]:
    """`skills/<言語>/` に実在する Skill (SKILL.md を持つディレクトリ) を集める。"""
    result: dict[str, set[str]] = {language: set() for language in LANGUAGES}
    for language in LANGUAGES:
        base = local_path(root, f"skills/{language}")
        if not os.path.isdir(base):
            continue
        for name in sorted(os.listdir(base)):
            if os.path.isfile(os.path.join(base, name, "SKILL.md")):
                result[language].add(name)
    return result


def scan(lines: list[str]) -> dict[str, list[tuple[int, re.Match[str]]]]:
    """コードブロック内のインストール宣言を種別ごとに集める。

    期待していない種別も含めて 3 種すべてを探す。ブロック外の行は見ないので、
    散文中に同じ字面が現れても拾わない。戻り値の各要素は (0 始まりの行番号, マッチ結果)。
    """
    found: dict[str, list[tuple[int, re.Match[str]]]] = {name: [] for name in KIND_NAMES}
    in_code_block = False

    for index, line in enumerate(lines):
        if FENCE_RE.match(line):
            in_code_block = not in_code_block
            continue
        if not in_code_block:
            continue
        for name in KIND_NAMES:
            match = PATTERNS[name].match(line.rstrip("\n"))
            if match is not None:
                found[name].append((index, match))

    return found


def check_structure(root: str, problems: list[str]) -> None:
    """利用者向け Skill の実構成と対象表を突き合わせる。"""
    registered = registered_skills()
    actual = actual_skills(root)

    for language in LANGUAGES:
        for name in sorted(actual[language] - registered[language]):
            problems.append(
                f"skills/{language}/{name}/SKILL.md: 実在するが検査の対象表に無い"
            )
        for name in sorted(registered[language] - actual[language]):
            problems.append(
                f"skills/{language}/{name}/SKILL.md: 対象表にあるが実在しない"
            )

    only_en = sorted(actual["en"] - actual["ja"])
    only_ja = sorted(actual["ja"] - actual["en"])
    for name in only_en:
        problems.append(f"{name}: 英語版だけに存在する (日本語版が無い)")
    for name in only_ja:
        problems.append(f"{name}: 日本語版だけに存在する (英語版が無い)")


def check_files(
    root: str, problems: list[str]
) -> dict[str, dict[str, int]]:
    """各対象ファイルの宣言と案内を検査し、種別ごとの実測本数を返す。"""
    counts: dict[str, dict[str, int]] = {}

    for relative, expected in TARGET_FILES:
        path = local_path(root, relative)
        if not os.path.isfile(path):
            problems.append(f"{relative}: ファイルが無い")
            continue
        with open(path, encoding="utf-8") as f:
            text = f.read()
        lines = text.splitlines(keepends=True)
        found = scan(lines)
        counts[relative] = {name: len(found[name]) for name in KIND_NAMES}

        for name in KIND_NAMES:
            occurrences = found[name]
            if name in expected and len(occurrences) != 1:
                problems.append(
                    f"{relative}: {name} がコードブロック内に {len(occurrences)} 行ある "
                    f"(1 行であるべき)"
                )
            if name not in expected and occurrences:
                problems.append(
                    f"{relative}: このファイルが持たないはずの {name} が "
                    f"{len(occurrences)} 行ある"
                )
            for index, match in occurrences:
                actual_version = match.group("version")
                if actual_version != PLACEHOLDER:
                    problems.append(
                        f"{relative}:{index + 1}: {name} の version が "
                        f"プレースホルダ {PLACEHOLDER} でない (実際は {actual_version})"
                    )
                if name == SWIFTPM and match.group("keyword") != "exact":
                    problems.append(
                        f"{relative}:{index + 1}: {name} は exact: で書く "
                        f"(実際は {match.group('keyword')}: — 上限が次のメジャーまで開き固定にならない)"
                    )

        if LATEST_RELEASE_URL not in text:
            problems.append(
                f"{relative}: 最新リリースへの案内 ({LATEST_RELEASE_URL}) が無い"
            )

    return counts


def check_language_parity(
    counts: dict[str, dict[str, int]], problems: list[str]
) -> None:
    """英語版と日本語版の Skill が同じ種類・同じ本数の宣言を持つことを検査する。"""
    by_skill: dict[str, dict[str, str]] = {}
    for relative, _expected in TARGET_FILES:
        match = SKILL_PATH_RE.match(relative)
        if match is None:
            continue
        by_skill.setdefault(match.group(2), {})[match.group(1)] = relative

    for skill in sorted(by_skill):
        paths = by_skill[skill]
        if set(paths) != set(LANGUAGES):
            continue
        en, ja = paths["en"], paths["ja"]
        if en not in counts or ja not in counts:
            continue
        for name in KIND_NAMES:
            if counts[en][name] != counts[ja][name]:
                problems.append(
                    f"{skill}: {name} の本数が英日で違う "
                    f"(en {counts[en][name]} 行 / ja {counts[ja][name]} 行)"
                )


def lint(root: str) -> int:
    problems: list[str] = []
    check_structure(root, problems)
    counts = check_files(root, problems)
    check_language_parity(counts, problems)

    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print(f"インストール例が契約を満たしていない ({len(problems)} 件)", file=sys.stderr)
        return 1

    print(
        f"{len(TARGET_FILES)} ファイルのインストール例 {TOTAL_TARGET_LINES} 行が契約を満たす"
    )
    return 0


# --- 自己テスト ------------------------------------------------------------------------
#
# 一時ディレクトリに組んだ木に対してだけ実行し、リポジトリの README と Skill は
# 読まない。各検査項目について、違反を入れた木で exit 1 になることを確かめる。

SELFTEST_README = """# Title

## Installation

Replace the placeholder `{version}` with the version you want; see
https://github.com/kamusoft/KsSettingsView/releases/latest for the current one.

### iOS

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "{version}")
]
```

To pin a prerelease, use `exact: "X.Y.Z-beta.N"`.
The Maven coordinates are `implementation("jp.kamusoft:kssettingsview:0.0.0")` in prose.

### Android

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:{version}")
}
```

### .NET MAUI

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="{version}" />
</ItemGroup>
```
"""

SELFTEST_IOS_SKILL = """# iOS Skill

## Setup

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "{version}")
]
```

See https://github.com/kamusoft/KsSettingsView/releases/latest for the current version.
"""

SELFTEST_ANDROID_SKILL = """# Android Skill

## Setup

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:{version}")
}
```

See https://github.com/kamusoft/KsSettingsView/releases/latest for the current version.
"""

SELFTEST_NUGET_SKILL = """# MAUI Skill

## Setup

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="{version}" />
</ItemGroup>
```

See https://github.com/kamusoft/KsSettingsView/releases/latest for the current version.
"""

# 種別ごとの自己テスト用テキスト。README 以外は自 platform の 1 行だけを持つ。
SELFTEST_BY_TARGETS: dict[tuple[str, ...], str] = {
    (SWIFTPM, MAVEN, NUGET): SELFTEST_README,
    (SWIFTPM,): SELFTEST_IOS_SKILL,
    (MAVEN,): SELFTEST_ANDROID_SKILL,
    (NUGET,): SELFTEST_NUGET_SKILL,
}


def selftest() -> int:
    import contextlib
    import io
    import shutil
    import tempfile

    failures = 0

    def check(ok: bool, name: str, detail: str = "") -> None:
        nonlocal failures
        failures += 0 if ok else 1
        print(f"  {'OK  ' if ok else 'NG  '} {name}{f' ({detail})' if detail else ''}")

    def write(root: str, relative: str, text: str) -> None:
        path = local_path(root, relative)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)

    def build(root: str) -> None:
        """契約を満たす木を組む。"""
        for relative, names in TARGET_FILES:
            write(root, relative, SELFTEST_BY_TARGETS[names])

    def run(root: str) -> tuple[int, str]:
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
            code = lint(root)
        return code, buf.getvalue()

    with tempfile.TemporaryDirectory() as base:
        print("[契約を満たす木]")
        root = os.path.join(base, "ok")
        build(root)
        code, out = run(root)
        check(code == 0, "違反が無ければ exit 0", out.strip())

        print("[プレースホルダ]")
        root = os.path.join(base, "concrete")
        build(root)
        write(root, "README.md", SELFTEST_README.replace(
            'exact: "{version}"', 'exact: "1.2.3"'
        ))
        code, out = run(root)
        check(code == 1, "具体 version の書き戻しで exit 1")
        check("README.md:12" in out, "ファイルと行が出力される", out.strip())
        check("1.2.3" in out, "実際の値が出力される")

        print("[SwiftPM の解決方法]")
        root = os.path.join(base, "from")
        build(root)
        write(root, "skills/en/kssettingsview-ios/SKILL.md", SELFTEST_IOS_SKILL.replace(
            'exact: "{version}"', 'from: "{version}"'
        ))
        code, out = run(root)
        check(code == 1, "from: への書き換えで exit 1")
        check("exact: で書く" in out, "exact: を求める理由が出力される", out.strip())

        print("[最新リリースへの案内]")
        root = os.path.join(base, "no-link")
        build(root)
        write(root, "skills/ja/kssettingsview-maui/SKILL.md",
              SELFTEST_NUGET_SKILL.replace(LATEST_RELEASE_URL, "https://example.invalid/"))
        code, out = run(root)
        check(code == 1, "案内が無ければ exit 1")
        check("最新リリースへの案内" in out, "不足の理由が出力される", out.strip())

        print("[対象表と実構成の突合]")
        root = os.path.join(base, "unregistered")
        build(root)
        for language in LANGUAGES:
            write(root, f"skills/{language}/kssettingsview-tvos/SKILL.md",
                  SELFTEST_NUGET_SKILL)
        code, out = run(root)
        check(code == 1, "対象表に無い Skill で exit 1")
        check("kssettingsview-tvos" in out, "未登録の Skill 名が出力される", out.strip())

        root = os.path.join(base, "missing")
        build(root)
        shutil.rmtree(local_path(root, "skills/ja/kssettingsview-maui"))
        code, out = run(root)
        check(code == 1, "対象表にあるのに実在しない Skill で exit 1")
        check("対象表にあるが実在しない" in out, "不在の理由が出力される", out.strip())

        print("[英日の構成]")
        root = os.path.join(base, "lang-only")
        build(root)
        write(root, "skills/en/kssettingsview-tvos/SKILL.md", SELFTEST_NUGET_SKILL)
        code, out = run(root)
        check(code == 1, "片言語だけの Skill 追加で exit 1")
        check("英語版だけに存在する" in out, "言語差の理由が出力される", out.strip())

        root = os.path.join(base, "lang-count")
        build(root)
        write(root, "skills/en/kssettingsview-android/SKILL.md",
              SELFTEST_ANDROID_SKILL + """
```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:{version}")
}
```
""")
        code, out = run(root)
        check(code == 1, "片言語だけの宣言追加で exit 1")
        check("本数が英日で違う" in out, "英日の本数差が出力される", out.strip())

        print("[期待していない種別]")
        root = os.path.join(base, "unexpected")
        build(root)
        write(root, "skills/en/kssettingsview-ios/SKILL.md", SELFTEST_IOS_SKILL + """
```xml
<PackageReference Include="KsSettingsView.Maui" Version="{version}" />
```
""")
        code, out = run(root)
        check(code == 1, "そのファイルが持たない種別の混入で exit 1")
        check("持たないはずの" in out, "混入した種別が出力される", out.strip())

        print("[散文は見ない]")
        root = os.path.join(base, "prose")
        build(root)
        write(root, "README.md", SELFTEST_README + """
The released version looks like `implementation("jp.kamusoft:kssettingsview:1.2.3")`.
""")
        code, out = run(root)
        check(code == 0, "コードブロック外の具体 version は拾わない", out.strip())

    print("失敗なし" if failures == 0 else f"失敗 {failures} 件")
    return 0 if failures == 0 else 1


def main(argv: list[str]) -> int:
    if "--selftest" in argv:
        return selftest()
    return lint(repo_root())


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
