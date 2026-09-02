#!/bin/bash
# Android 消費者のビルド。
#
# 消費者アプリの release variant をビルドし、解決結果の証跡として依存ツリーの
# `jp.kamusoft` 行を出す。
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <local maven repository>] [--work <dir>]
#
# --reference を与えると、その参照先をそのまま使いフィード準備を行わない。
# 与えない dry-run では prepare-feed.sh を呼んで mavenLocal へ発行する。
#
# --work は受け付けるが Android では効かない (ビルド出力は Gradle の build ディレクトリで、
# 作業ディレクトリの指定を持たない)。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="android"
KSV_DEFAULT_VERSION="$(
    sed -n 's/^kssettingsview[[:space:]]*=[[:space:]]*"\(.*\)"$/\1/p' \
        "${REPO_ROOT}/android/gradle/libs.versions.toml"
)"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"
# shellcheck source=./android-sdk.sh
. "${SCRIPT_DIR}/android-sdk.sh"

ksv_parse_args "$@"
ksv_ensure_android_home

reference="${KSV_REFERENCE}"
if [ "${KSV_MODE}" = "dry-run" ] && [ -z "${reference}" ]; then
    reference="$("${SCRIPT_DIR}/prepare-feed.sh" \
        --mode "${KSV_MODE}" --version "${KSV_VERSION}" | tail -n 1)"
fi

gradle_args=(
    "--console=plain"
    "-PksSettingsViewMode=${KSV_MODE}"
    "-PksSettingsViewVersion=${KSV_VERSION}"
)
if [ "${KSV_MODE}" = "dry-run" ] && [ -n "${reference}" ]; then
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    gradle_args+=("-PksSettingsViewReference=$(cd "${reference}" && pwd -P)")
fi

echo "==== Release ビルド ===="
(cd "${SCRIPT_DIR}" && ./gradlew "${gradle_args[@]}" :app:assembleRelease)

# Gradle の失敗と「行が見つからない」を区別する。パイプで grep に繋ぐと Gradle が落ちた
# ときも空文字になり、原因と食い違うメッセージで終わる。
tree="$(mktemp)"
trap 'rm -f "${tree}"' EXIT
if ! (cd "${SCRIPT_DIR}" && ./gradlew "${gradle_args[@]}" -q \
    :app:dependencies --configuration releaseRuntimeClasspath) > "${tree}"; then
    cat "${tree}" >&2
    ksv_fail "依存ツリーの取得に失敗しました (:app:dependencies)"
fi

resolved="$(grep "jp.kamusoft" "${tree}" || true)"
if [ -z "${resolved}" ]; then
    ksv_fail "依存ツリーに jp.kamusoft の行がありません"
fi
printf '%s\n' "${resolved}" | ksv_evidence "releaseRuntimeClasspath の jp.kamusoft 行"
