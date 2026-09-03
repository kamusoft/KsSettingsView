#!/bin/bash
# .NET MAUI 消費者のビルド。
#
# net10.0-android と net10.0-ios を Release でビルドする。iOS は Simulator RID を明示して
# 署名情報を要求しない形にする。restore は実行ごとに空のパッケージ展開先を使い、
# 解決版と取得元を check-dependencies.py で検査する。
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <フォルダフィード>] [--work <dir>]
#
# --reference を与えると、そのフォルダフィードをそのまま使いフィード準備を行わない。
# 与えない dry-run では prepare-feed.sh を呼んで pack する。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="maui"
# 本体の開発用既定値と同じ値を持つ (宣言元から自動追随はしない)。
KSV_DEFAULT_VERSION="0.0.0-dev"
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

readonly PROJECT="VerificationApp.csproj"
readonly NUGET_ORG_SOURCE="https://api.nuget.org/v3/index.json"

ksv_parse_args "$@"

work="${KSV_WORK:-${TMPDIR:-/tmp}/kssettingsview-verification/maui}"
mkdir -p "${work}"
work="$(cd "${work}" && pwd -P)"

reference="${KSV_REFERENCE}"
if [ "${KSV_MODE}" = "dry-run" ] && [ -z "${reference}" ]; then
    reference="$("${SCRIPT_DIR}/prepare-feed.sh" \
        --mode "${KSV_MODE}" --version "${KSV_VERSION}" --work "${work}" | tail -n 1)"
fi

if [ "${KSV_MODE}" = "dry-run" ]; then
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    reference="$(cd "${reference}" && pwd -P)"
    config="${SCRIPT_DIR}/nuget.dry-run.config"
    expected_source="${reference}"
    export KSSV_LOCAL_FEED="${reference}"
else
    config="${SCRIPT_DIR}/nuget.smoke.config"
    expected_source="${NUGET_ORG_SOURCE}"
fi

# packageSourceMapping は global packages folder に展開済みのパッケージには働かない。
# 実行ごとに空の展開先を使い、ユーザー環境や CI のキャッシュを参照しない。
packages="${work}/packages"
rm -rf "${packages}"
mkdir -p "${packages}"

# 手元の作業ツリーを汚さないよう、中間出力も作業ディレクトリへ逃がす。
obj="${work}/obj/"
bin="${work}/bin/"

# iOS Simulator の RID はホストのアーキテクチャに合わせる。実機向けの RID を選ぶと
# 署名情報を要求され、検証範囲の外に出る。
case "$(uname -m)" in
    arm64|aarch64) ios_rid="iossimulator-arm64" ;;
    *) ios_rid="iossimulator-x64" ;;
esac

common_args=(
    # 構成は restore と build で同じ形の指定にする。dotnet restore は -c を受け付けない。
    "-p:Configuration=Release"
    "-p:KsSettingsViewVersion=${KSV_VERSION}"
    "-p:RestoreConfigFile=${config}"
    "-p:RestorePackagesPath=${packages}"
    "-p:BaseIntermediateOutputPath=${obj}"
    "-p:BaseOutputPath=${bin}"
)

# Android のビルドが使う JVM を明示できるようにする (CI の JDK 選択に追随する)。
if [ -n "${JAVA_HOME:-}" ]; then
    common_args+=("-p:JavaSdkDirectory=${JAVA_HOME}")
fi

echo "==== restore ===="
(cd "${SCRIPT_DIR}" && dotnet restore "${PROJECT}" "${common_args[@]}")

python3 "${SCRIPT_DIR}/check-dependencies.py" \
    --assets "${obj}project.assets.json" \
    --packages "${packages}" \
    --expected-version "${KSV_VERSION}" \
    --expected-source "${expected_source}" \
    | ksv_evidence "解決版と取得元"

echo "==== Release ビルド (net10.0-android) ===="
# native ライブラリの重複 (XA4301) は利用者のアプリで実害が出るため失敗にする。
# 警告全般をエラーに昇格させると検証範囲の外の指摘まで巻き込むので、
# ビルド出力からこの 1 件だけを拾って判定し、重複したライブラリのパスを残す。
android_log="${work}/build-android.log"
(cd "${SCRIPT_DIR}" && dotnet build "${PROJECT}" -f net10.0-android --no-restore "${common_args[@]}") \
    2>&1 | tee "${android_log}"

android_duplicates="${work}/xa4301.txt"
grep -F "XA4301" "${android_log}" | sort -u > "${android_duplicates}" || true

if [ -s "${android_duplicates}" ]; then
    ksv_evidence "XA4301 (native ライブラリの重複)" < "${android_duplicates}"
    ksv_fail "Android の Release ビルドで native ライブラリの重複 (XA4301) が出ました"
fi

echo "検出なし" | ksv_evidence "XA4301 (native ライブラリの重複)"

echo "==== Release ビルド (net10.0-ios, ${ios_rid}) ===="
(cd "${SCRIPT_DIR}" && dotnet build "${PROJECT}" -f net10.0-ios --no-restore \
    "-p:RuntimeIdentifier=${ios_rid}" "${common_args[@]}")
