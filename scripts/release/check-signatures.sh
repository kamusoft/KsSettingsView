#!/bin/bash
# Maven 発行物に GPG 署名 (`.asc`) が揃っているかの検査。
#
# 使い方:
#   scripts/release/check-signatures.sh <発行物の jp/ ディレクトリ>
#
# 引数には mavenLocal へ発行した結果の `jp/` (groupId の先頭セグメント) を渡す。配下を再帰的に
# 走査し、成果物 (aar / pom / jar / Gradle module metadata) の 1 件ごとに `<成果物>.asc` が
# 対で存在することを確かめる。1 件でも欠けていれば、欠けているファイルを列挙して失敗する。
#
# Maven Central への upload は署名の無い成果物を受け付けない。署名鍵の secret が空でも
# Gradle の発行タスク自体は成功してしまうため、upload の前にこの検査を挟んで、鍵が届いて
# いないことを不可逆操作の前に露見させる。
#
# checksum ファイル (`.md5` / `.sha1` / `.sha256` / `.sha512`) と `maven-metadata*.xml` は
# 成果物ではないので対象にしない。成果物が 1 件も見つからない場合は、走査先の指定を誤った
# ものとして失敗する (空ディレクトリを緑で通さない)。

set -euo pipefail

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <発行物の jp/ ディレクトリ>" >&2
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

if [ $# -ne 1 ]; then
    usage
    exit 2
fi

readonly ARTIFACT_ROOT="$1"

[ -d "${ARTIFACT_ROOT}" ] || fail "発行物のディレクトリがありません: ${ARTIFACT_ROOT}"

artifacts=()
while IFS= read -r -d '' file; do
    artifacts+=("${file}")
done < <(
    find "${ARTIFACT_ROOT}" -type f \
        \( -name '*.aar' -o -name '*.pom' -o -name '*.jar' -o -name '*.module' \) \
        -print0 | sort -z
)

if [ "${#artifacts[@]}" -eq 0 ]; then
    fail "署名を検査する成果物が 1 件も見つかりません: ${ARTIFACT_ROOT}"
fi

missing=()
for artifact in "${artifacts[@]}"; do
    if [ ! -f "${artifact}.asc" ]; then
        missing+=("${artifact#"${ARTIFACT_ROOT}"/}")
    fi
done

if [ "${#missing[@]}" -gt 0 ]; then
    for name in "${missing[@]}"; do
        echo "::error::署名 (.asc) がありません: ${name}" >&2
    done
    fail "成果物 ${#artifacts[@]} 件のうち ${#missing[@]} 件に署名がありません"
fi

echo "成果物 ${#artifacts[@]} 件すべてに署名 (.asc) があります: ${ARTIFACT_ROOT}"
