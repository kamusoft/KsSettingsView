#!/bin/bash
# 2 つの Maven 発行物ツリーの同一性の検査。
#
# 使い方:
#   scripts/release/compare-maven-artifacts.sh <発行物 A の jp/> <発行物 B の jp/>
#
# 同じ commit・同じ JDK・同じランナー OS で 2 回発行した結果が、署名の有無を除いて同じ内容に
# なることを確かめる。Android の配布物は消費者検証に渡した未署名の発行物と、公開時に署名鍵つきで
# 作り直した発行物が別のファイルになるため、公開前にこの検査で「検証したものと出すものが同じ」
# ことを担保する。
#
# 比較の規則:
#
#   pom / Gradle module metadata   byte 単位で比較する
#   アーカイブ (aar / jar)          エントリ名の一覧と、各エントリの内容を比較する
#                                  (zip に記録される更新時刻・圧縮方法の差は無視する)
#   署名 (`.asc`) と checksum       比較しない (署名の有無がそもそもの差なので)
#   maven-metadata*.xml            比較しない
#
# 比較の対象は成果物 (pom / Gradle module metadata / aar / sources jar / javadoc jar) に限る。
# `maven-metadata-local.xml` はローカルリポジトリへの発行の副産物であり、発行時刻
# (`<lastUpdated>`) とそのマシンで発行済みの version 一覧を持つ。成果物の内容とは無関係に
# 実行ごとに変わるため、比較に含めると必ず差異になる。
#
# 片方にしか無いファイルも差異として扱う。差異が 1 件でもあれば、該当するファイルを列挙して
# 失敗する。
#
# 依存するのは bash / find / sort / cmp / unzip / シェルに付属するハッシュコマンドだけで、
# ネットワークへは出ない。

set -euo pipefail

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <発行物 A の jp/> <発行物 B の jp/>" >&2
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

if [ $# -ne 2 ]; then
    usage
    exit 2
fi

readonly LEFT_ROOT="$1"
readonly RIGHT_ROOT="$2"

[ -d "${LEFT_ROOT}" ]  || fail "発行物のディレクトリがありません: ${LEFT_ROOT}"
[ -d "${RIGHT_ROOT}" ] || fail "発行物のディレクトリがありません: ${RIGHT_ROOT}"
command -v unzip > /dev/null 2>&1 || fail "unzip が必要です (アーカイブの内容比較に使います)"

# 内容比較に使うハッシュコマンド。macOS は shasum、Linux は sha256sum を持つ。
if command -v shasum > /dev/null 2>&1; then
    readonly HASH_TOOL="shasum"
elif command -v sha256sum > /dev/null 2>&1; then
    readonly HASH_TOOL="sha256sum"
else
    fail "shasum / sha256sum のいずれも見つかりません (内容比較に必要)"
fi

# 比較対象のファイルを、ルートからの相対パスとして改行区切りで並べる。
# 署名・checksum・リポジトリのメタデータは比較対象から外す。
list_files() {
    local root="$1"
    ( cd "${root}" && find . -type f \
        ! -name '*.asc' ! -name '*.md5' ! -name '*.sha1' \
        ! -name '*.sha256' ! -name '*.sha512' \
        ! -name 'maven-metadata*.xml' \
        | sed 's|^\./||' | LC_ALL=C sort )
}

# アーカイブの内容を「<エントリ名> <内容のハッシュ>」の行として並べる。
# ディレクトリエントリ (名前が `/` で終わる) は内容を持たないので名前だけを出す。
archive_contents() {
    local archive="$1"
    local entry hash
    while IFS= read -r entry; do
        [ -n "${entry}" ] || continue
        case "${entry}" in
            */)
                echo "${entry} -"
                ;;
            *)
                hash="$(unzip -p "${archive}" "${entry}" | "${HASH_TOOL}" | awk '{print $1}')"
                echo "${entry} ${hash}"
                ;;
        esac
    done < <(unzip -Z1 "${archive}" | LC_ALL=C sort)
}

differences=0

report_difference() {
    echo "::error::$1" >&2
    differences=$((differences + 1))
}

work="$(mktemp -d)"
trap 'rm -rf "${work}"' EXIT

list_files "${LEFT_ROOT}"  > "${work}/left-files"
list_files "${RIGHT_ROOT}" > "${work}/right-files"

if [ ! -s "${work}/left-files" ]; then
    fail "比較する成果物が 1 件も見つかりません: ${LEFT_ROOT}"
fi

while IFS= read -r relative; do
    [ -n "${relative}" ] || continue
    report_difference "片方にしかありません (A のみ): ${relative}"
done < <(LC_ALL=C comm -23 "${work}/left-files" "${work}/right-files")

while IFS= read -r relative; do
    [ -n "${relative}" ] || continue
    report_difference "片方にしかありません (B のみ): ${relative}"
done < <(LC_ALL=C comm -13 "${work}/left-files" "${work}/right-files")

compared=0
while IFS= read -r relative; do
    [ -n "${relative}" ] || continue
    left="${LEFT_ROOT}/${relative}"
    right="${RIGHT_ROOT}/${relative}"
    compared=$((compared + 1))

    case "${relative}" in
        *.aar|*.jar|*.zip)
            archive_contents "${left}"  > "${work}/left-entries"
            archive_contents "${right}" > "${work}/right-entries"
            if ! cmp -s "${work}/left-entries" "${work}/right-entries"; then
                report_difference "アーカイブの内容が異なります: ${relative}"
                # どのエントリが違うのかまで出す (エントリ名だけの差も内容の差もこの diff に出る)。
                diff "${work}/left-entries" "${work}/right-entries" >&2 || true
            fi
            ;;
        *)
            if ! cmp -s "${left}" "${right}"; then
                report_difference "内容が異なります: ${relative}"
            fi
            ;;
    esac
done < <(LC_ALL=C comm -12 "${work}/left-files" "${work}/right-files")

if [ "${differences}" -gt 0 ]; then
    fail "発行物に ${differences} 件の差異があります (署名を除く比較)"
fi

echo "発行物 ${compared} 件が一致します (署名を除く比較): ${LEFT_ROOT} / ${RIGHT_ROOT}"
