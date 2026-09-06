#!/usr/bin/env python3
"""インストール例に書かれた version の置換と検査。

貼ってそのまま使える依存宣言は、ルート README 2 枚 (英語 / 日本語) と利用者向け Skill
4 本 x 2 言語 (`skills/{en,ja}/kssettingsview-*/SKILL.md`) の導入節にある。リリースの
たびにこの 14 行を同じ version へ揃えるのがこのスクリプトの仕事で、release workflow の
validate は検査モードを呼ぶ。

対象は次の 3 種で、いずれもコードブロックの中にあるものだけを見る。同じ値が散文の説明にも
現れるが (prerelease の書き方の案内)、そちらは version を持たないプレースホルダなので
対象にしない。

  SwiftPM  .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "X.Y.Z")
  Maven    implementation("jp.kamusoft:kssettingsview:X.Y.Z")
  NuGet    <PackageReference Include="KsSettingsView.Maui" Version="X.Y.Z" />

どの種をどのファイルに期待するかは TARGET_FILES で持つ。README は 3 種すべてを 1 行ずつ
持ち、Skill は自 platform の 1 種だけを持つ (iOS: SwiftPM / Android: Maven /
MAUI・AiForms 移行: NuGet)。期待していない種はそのファイルでは探さない。

SwiftPM の宣言は `exact:` で書く。`from:` は prerelease の tag を解決しないため、
prerelease を配る間も貼ってそのまま動く形にならない。現状が `from:` でも `exact:` でも
置換後は `exact:` に揃え、検査モードも `exact:` を要求する。

各ファイルで期待する各種がちょうど 1 行見つかることを前提とする。0 行または 2 行以上なら、
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

# 対象の種別名。エラー出力でどの行が見つからなかったかを示す文字列でもある。
SWIFTPM = "SwiftPM の依存宣言"
MAVEN = "Maven 座標"
NUGET = "NuGet の PackageReference"

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


def find_targets(
    lines: list[str], names: tuple[str, ...]
) -> dict[str, list[tuple[int, re.Match[str]]]]:
    """コードブロック内の対象行を種別ごとに集める。

    探すのは names に挙げた種別だけ。戻り値の各要素は (0 始まりの行番号, マッチ結果)。
    ブロック外の行は見ないので、散文中に同じ字面が現れても拾わない。
    """
    found: dict[str, list[tuple[int, re.Match[str]]]] = {name: [] for name in names}
    in_code_block = False

    for index, line in enumerate(lines):
        if FENCE_RE.match(line):
            in_code_block = not in_code_block
            continue
        if not in_code_block:
            continue
        for name in names:
            match = PATTERNS[name].match(line.rstrip("\n"))
            if match is not None:
                found[name].append((index, match))

    return found


def rewrite(name: str, match: re.Match[str], version: str) -> str:
    """対象行を新しい version の行 (改行なし) へ書き換える。"""
    if name == SWIFTPM:
        # prerelease を解決させるため、`from:` で書かれていても `exact:` へ揃える。
        return f'{match.group("prefix")}exact{match.group("mid")}{version}{match.group("suffix")}'
    return f'{match.group("prefix")}{version}{match.group("suffix")}'


Parsed = dict[str, tuple[list[str], dict[str, list[tuple[int, re.Match[str]]]], tuple[str, ...]]]


def collect(root: str) -> tuple[Parsed, list[str]]:
    """全対象ファイルを読み、対象行を集める。1 行に確定できないものは problems に積む。"""
    parsed: Parsed = {}
    problems: list[str] = []

    for relative, names in TARGET_FILES:
        path = local_path(root, relative)
        if not os.path.isfile(path):
            problems.append(f"{relative}: ファイルが無い")
            continue
        with open(path, encoding="utf-8") as f:
            lines = f.read().splitlines(keepends=True)
        found = find_targets(lines, names)
        for name in names:
            count = len(found[name])
            if count != 1:
                problems.append(
                    f"{relative}: {name} がコードブロック内に {count} 行ある (1 行であるべき)"
                )
        parsed[relative] = (lines, found, names)

    return parsed, problems


def replace(root: str, version: str) -> int:
    parsed, problems = collect(root)
    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print("対象行を確定できないため置換しない", file=sys.stderr)
        return 1

    changed = 0
    for relative, (lines, found, names) in parsed.items():
        for name in names:
            index, match = found[name][0]
            ending = "\n" if lines[index].endswith("\n") else ""
            lines[index] = rewrite(name, match, version) + ending
            changed += 1
        with open(local_path(root, relative), "w", encoding="utf-8") as f:
            f.write("".join(lines))

    print(f"{len(TARGET_FILES)} ファイルのインストール例 {changed} 行を {version} にした")
    return 0


def check(root: str, version: str) -> int:
    parsed, problems = collect(root)

    for relative, (_lines, found, names) in parsed.items():
        for name in names:
            if len(found[name]) != 1:
                continue
            index, match = found[name][0]
            actual = match.group("version")
            if actual != version:
                problems.append(
                    f"{relative}:{index + 1}: {name} の version が {version} でない (実際は {actual})"
                )
            if name == SWIFTPM and match.group("keyword") != "exact":
                problems.append(
                    f"{relative}:{index + 1}: {name} は exact: で書く "
                    f"(実際は {match.group('keyword')}: — prerelease が解決されない)"
                )

    if problems:
        for problem in problems:
            print(f"::error::{problem}", file=sys.stderr)
        print(f"インストール例が {version} と一致しない ({len(problems)} 件)", file=sys.stderr)
        return 1

    print(f"インストール例 {TOTAL_TARGET_LINES} 行が {version} と一致する")
    return 0


# --- 自己テスト ------------------------------------------------------------------------
#
# 一時ディレクトリに置いたコピーに対してだけ実行し、リポジトリの README と Skill は
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

SELFTEST_IOS_SKILL = """# iOS Skill

## Setup

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
]
```

To pin a prerelease, use `exact: "X.Y.Z-beta.N"`.
"""

SELFTEST_ANDROID_SKILL = """# Android Skill

## Setup

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```
"""

SELFTEST_NUGET_SKILL = """# MAUI Skill

## Setup

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```
"""

# 種別ごとの自己テスト用テキスト。README 以外は自 platform の 1 行だけを持つ。
SELFTEST_BY_TARGETS: dict[tuple[str, ...], str] = {
    (SWIFTPM, MAVEN, NUGET): SELFTEST_README,
    (SWIFTPM,): SELFTEST_IOS_SKILL,
    (MAVEN,): SELFTEST_ANDROID_SKILL,
    (NUGET,): SELFTEST_NUGET_SKILL,
}


def selftest_tree() -> dict[str, str]:
    """全対象ファイルぶんの自己テスト用テキストを、リポジトリ相対パスで返す。"""
    return {relative: SELFTEST_BY_TARGETS[names] for relative, names in TARGET_FILES}


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
        for relative, text in contents.items():
            path = local_path(root, relative)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)

    def read_all(root: str) -> dict[str, str]:
        result = {}
        for relative, _names in TARGET_FILES:
            with open(local_path(root, relative), encoding="utf-8") as f:
                result[relative] = f.read()
        return result

    def files_with(target: str) -> list[str]:
        return [relative for relative, names in TARGET_FILES if target in names]

    print("[置換]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, selftest_tree())
        code, out = run(replace, tmp, "1.2.3-beta.4")
        report(code == 0, "置換が成功する", out.strip())

        after = read_all(tmp)

        report(
            all(
                '.package(url: "https://github.com/kamusoft/KsSettingsView-SPM", exact: "1.2.3-beta.4")'
                in after[relative] for relative in files_with(SWIFTPM)
            ),
            "SwiftPM の from: が exact: の新 version になる (README 2 枚 + iOS Skill 2 枚)",
        )
        report(
            all('implementation("jp.kamusoft:kssettingsview:1.2.3-beta.4")' in after[relative]
                for relative in files_with(MAVEN)),
            "Maven 座標が新 version になる (README 2 枚 + Android Skill 2 枚)",
        )
        report(
            all('<PackageReference Include="KsSettingsView.Maui" Version="1.2.3-beta.4" />'
                in after[relative] for relative in files_with(NUGET)),
            "NuGet の Version が新 version になる (README 2 枚 + MAUI / AiForms Skill 4 枚)",
        )
        report(
            all('use `exact: "X.Y.Z-beta.N"`' in after[relative]
                for relative in ["README.md", "skills/en/kssettingsview-ios/SKILL.md"]),
            "散文のプレースホルダは書き換えない",
        )
        report(
            '`implementation("jp.kamusoft:kssettingsview:0.0.0")` in prose' in after["README.md"],
            "コードブロック外の同じ字面は書き換えない",
        )
        report(
            all(after[relative].count("1.2.3-beta.4") == len(names)
                for relative, names in TARGET_FILES),
            "書き換わるのはファイルごとに期待した本数だけ",
            "; ".join(f"{r}={after[r].count('1.2.3-beta.4')}" for r, _ in TARGET_FILES),
        )

        code, out = run(check, tmp, "1.2.3-beta.4")
        report(code == 0, "置換後の検査が通る", out.strip())

        code, out = run(check, tmp, "9.9.9")
        report(code == 1, "別の version の検査は失敗する")
        report("README.md:9" in out, "README の不一致の行番号が出力される", out.strip())
        report(
            "skills/en/kssettingsview-ios/SKILL.md:7" in out,
            "Skill の不一致の行番号が出力される",
            out.strip(),
        )

    print("[Skill ファイルは自分の種別だけを見る]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        # iOS Skill に Android 向けの行が混ざっていても、期待する種別ではないので触らない。
        tree["skills/en/kssettingsview-ios/SKILL.md"] = SELFTEST_IOS_SKILL + (
            "\n```kotlin\n"
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n'
            "```\n"
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 0, "期待外の種別が混ざっていても置換は成功する", out.strip())
        with open(local_path(tmp, "skills/en/kssettingsview-ios/SKILL.md"), encoding="utf-8") as f:
            ios_text = f.read()
        report(
            'implementation("jp.kamusoft:kssettingsview:0.1.0")' in ios_text,
            "期待していない種別の行は書き換えない",
        )
        report(ios_text.count("1.2.3") == 1, "書き換わるのは期待した 1 行だけ")

    print("[該当行が確定できない場合]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["README.md"] = SELFTEST_README.replace(
            '    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")\n',
            "    // 形の変わった宣言\n",
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が無ければ置換しない")
        report("README.md" in out and "SwiftPM" in out, "見つからなかったファイルと種別が出力される", out.strip())
        with open(local_path(tmp, "README_ja.md"), encoding="utf-8") as f:
            report('kssettingsview:0.1.0' in f.read(), "1 枚でも確定できなければ他方も書き換えない")

    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["skills/ja/kssettingsview-ios/SKILL.md"] = SELFTEST_IOS_SKILL.replace(
            '    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")\n',
            "    // 形の変わった宣言\n",
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "Skill の対象行が無ければ置換しない")
        report(
            "skills/ja/kssettingsview-ios/SKILL.md" in out and "SwiftPM" in out,
            "見つからなかった Skill と種別が出力される",
            out.strip(),
        )
        with open(local_path(tmp, "README.md"), encoding="utf-8") as f:
            report('kssettingsview:0.1.0' in f.read(), "Skill 1 枚の不備で README も書き換えない")

    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["README.md"] = SELFTEST_README.replace(
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n',
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n'
            '    implementation("jp.kamusoft:kssettingsview:0.1.0")\n',
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象行が 2 行あれば置換しない")
        report("2 行ある" in out, "見つかった行数が出力される", out.strip())

    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        tree["skills/en/kssettingsview-maui/SKILL.md"] = SELFTEST_NUGET_SKILL.replace(
            '  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />\n',
            '  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />\n'
            '  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />\n',
        )
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "Skill の対象行が 2 行あれば置換しない")
        report(
            "skills/en/kssettingsview-maui/SKILL.md" in out and "2 行ある" in out,
            "重複した Skill と行数が出力される",
            out.strip(),
        )

    print("[SwiftPM が from: のままの場合]")
    with tempfile.TemporaryDirectory() as tmp:
        stage(tmp, selftest_tree())
        run(replace, tmp, "1.2.3-beta.4")
        # 置換直後は exact: に揃っているので、iOS Skill 1 枚だけを from: へ戻す。
        # version は一致したままなので、検査が落ちる理由は解決方法の語だけになる。
        ios_skill = "skills/en/kssettingsview-ios/SKILL.md"
        ios_path = local_path(tmp, ios_skill)
        with open(ios_path, encoding="utf-8") as f:
            reverted = f.read().replace('", exact: "', '", from: "')
        with open(ios_path, "w", encoding="utf-8") as f:
            f.write(reverted)

        code, out = run(check, tmp, "1.2.3-beta.4")
        report(code == 1, "version が一致していても from: なら検査が失敗する", out.strip())
        report(
            f"{ios_skill}:7" in out and "exact: で書く" in out,
            "exact: を要求する指摘にファイルと行番号が出力される",
            out.strip(),
        )
        report(
            "version が 1.2.3-beta.4 でない" not in out,
            "version の不一致としては報告しない",
            out.strip(),
        )

    print("[対象ファイルが無い場合]")
    with tempfile.TemporaryDirectory() as tmp:
        tree = selftest_tree()
        absent = "skills/ja/kssettingsview-android/SKILL.md"
        del tree[absent]
        stage(tmp, tree)
        code, out = run(replace, tmp, "1.2.3")
        report(code == 1, "対象ファイルが無ければ置換しない")
        report(
            absent in out and "ファイルが無い" in out,
            "無いファイルの名前が出力される",
            out.strip(),
        )
        with open(local_path(tmp, "README.md"), encoding="utf-8") as f:
            report('kssettingsview:0.1.0' in f.read(), "1 枚が無ければ残りも書き換えない")
        code, out = run(check, tmp, "0.1.0")
        report(code == 1, "対象ファイルが無ければ検査も失敗する", out.strip())

    print("[実物のファイルに対する疎通]")
    root = repo_root()
    missing = [
        relative for relative, _names in TARGET_FILES
        if not os.path.isfile(local_path(root, relative))
    ]
    if missing:
        report(False, "リポジトリの対象ファイルを読める", ", ".join(missing))
    else:
        with tempfile.TemporaryDirectory() as tmp:
            for relative, _names in TARGET_FILES:
                destination = local_path(tmp, relative)
                os.makedirs(os.path.dirname(destination), exist_ok=True)
                shutil.copyfile(local_path(root, relative), destination)
            code, out = run(replace, tmp, "9.9.9-rc.7")
            report(
                code == 0,
                f"実物のファイルで {TOTAL_TARGET_LINES} 行を確定できる",
                out.strip(),
            )
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
