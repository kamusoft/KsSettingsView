#!/bin/bash
# .NET MAUI 消費者検証のフィード準備。
#
# facade と binding 2 件を指定 version で pack し、ローカルフォルダフィードへ置く。
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>] [--work <dir>]
#
# 標準出力の最終行に、準備したフォルダフィードの絶対パスを出す。
# smoke では nuget.org を参照するため何も準備せず、参照先も出力しない。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="maui"
# 本体の開発用既定値と同じ値を持つ (宣言元から自動追随はしない)。
KSV_DEFAULT_VERSION="0.0.0-dev"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

ksv_parse_args "$@"

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (nuget.org) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

work="${KSV_WORK:-${TMPDIR:-/tmp}/kssettingsview-verification/maui}"
mkdir -p "${work}"
work="$(cd "${work}" && pwd -P)"

feed="${work}/feed"
# 前回の pack 成果が残っていると、フィードに無いはずの version が解決できてしまう。
rm -rf "${feed}"
mkdir -p "${feed}"

# Android binding の pack は aar を作るために本体の Gradle ビルドを呼ぶ。その実行 JVM を
# 明示できるよう、JAVA_HOME があれば .NET Android SDK へ渡す (CI の JDK 選択に追随する)。
pack_args=()
if [ -n "${JAVA_HOME:-}" ]; then
    pack_args+=("-p:JavaSdkDirectory=${JAVA_HOME}")
fi

projects=(
    "maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj"
    "maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj"
    "maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj"
)
for project in "${projects[@]}"; do
    dotnet pack "${REPO_ROOT}/${project}" \
        -c Release \
        -p:Version="${KSV_VERSION}" \
        -o "${feed}" \
        ${pack_args[@]+"${pack_args[@]}"}
done

for package in KsSettingsView.Maui KsSettingsView.Binding.iOS KsSettingsView.Binding.Android; do
    [ -f "${feed}/${package}.${KSV_VERSION}.nupkg" ] \
        || ksv_fail "pack されていません: ${package}.${KSV_VERSION}.nupkg"
done

echo "${feed}"
