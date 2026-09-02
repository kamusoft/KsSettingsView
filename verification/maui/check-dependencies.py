#!/usr/bin/env python3
"""MAUI 消費者が解決した KsSettingsView パッケージの版と取得元を検査する。

restore の結果から次の 2 点を確かめる。

1. facade (KsSettingsView.Maui) と binding 2 件 (KsSettingsView.Binding.iOS /
   KsSettingsView.Binding.Android) の解決版が、要求した version と一致すること
2. 3 件の取得元が、指定した参照先であること

版は `project.assets.json` の targets から読む。取得元は展開先の
`<id>/<version>/.nupkg.metadata` の source から読む。`project.assets.json` の
restore.sources は「構成したソースの一覧」であって実際の取得元ではないため使わない。
packageSourceMapping は展開済みのパッケージには働かないので、取得元の確認は
パッケージ単位のこの記録でしか行えない。

使い方:
    check-dependencies.py --assets <project.assets.json>
                          --packages <展開先ディレクトリ>
                          --expected-version <version>
                          --expected-source <フォルダフィードのパス、または URL>
"""

import argparse
import json
import os
import sys

# facade と、platform TFM の依存として推移的に入る binding 2 件。
FACADE = "KsSettingsView.Maui"
BINDINGS = ("KsSettingsView.Binding.iOS", "KsSettingsView.Binding.Android")
REQUIRED = (FACADE,) + BINDINGS


def read_resolved_versions(assets_path):
    """assets から KsSettingsView 系パッケージの解決版を集める。

    戻り値は {パッケージ ID: {version, ...}}。同じ ID が複数の TFM に現れるため、
    版が TFM ごとに割れていないことも呼び出し側で判定できるよう集合で返す。
    """
    with open(assets_path, encoding="utf-8") as f:
        assets = json.load(f)

    resolved = {}
    for libraries in assets.get("targets", {}).values():
        for key in libraries:
            package_id, _, version = key.partition("/")
            if package_id in REQUIRED:
                resolved.setdefault(package_id, set()).add(version)
    return resolved


def read_source(packages_path, package_id, version):
    """展開先の .nupkg.metadata から取得元を読む。無ければ None を返す。"""
    metadata_path = os.path.join(
        packages_path, package_id.lower(), version.lower(), ".nupkg.metadata"
    )
    if not os.path.isfile(metadata_path):
        return None
    with open(metadata_path, encoding="utf-8") as f:
        return json.load(f).get("source")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", required=True)
    parser.add_argument("--packages", required=True)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--expected-source", required=True)
    args = parser.parse_args()

    resolved = read_resolved_versions(args.assets)

    failures = []
    rows = []
    for package_id in REQUIRED:
        versions = sorted(resolved.get(package_id, ()))
        if not versions:
            failures.append(f"{package_id} が解決されていない")
            rows.append((package_id, "(未解決)", "(なし)"))
            continue
        if len(versions) > 1:
            failures.append(
                f"{package_id} の解決版が TFM ごとに割れている: {', '.join(versions)}"
            )
        for version in versions:
            source = read_source(args.packages, package_id, version)
            rows.append((package_id, version, source or "(取得元の記録なし)"))
            if version != args.expected_version:
                failures.append(
                    f"{package_id} の解決版が要求と異なる: "
                    f"要求 {args.expected_version} / 解決 {version}"
                )
            if source is None:
                failures.append(f"{package_id} {version} の取得元の記録がない")
            elif source != args.expected_source:
                failures.append(
                    f"{package_id} {version} の取得元が参照先と異なる: "
                    f"期待 {args.expected_source} / 実際 {source}"
                )

    width = max(len(row[0]) for row in rows)
    lines = [f"{package_id.ljust(width)}  {version}  <- {source}" for package_id, version, source in rows]
    print("\n".join(lines))

    if failures:
        print("", file=sys.stderr)
        for failure in failures:
            print(f"エラー: {failure}", file=sys.stderr)
        return 1

    print(f"facade と binding 2 件が {args.expected_version} で一致し、取得元も参照先と一致する")
    return 0


if __name__ == "__main__":
    sys.exit(main())
