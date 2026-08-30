#!/bin/bash
# KsSettingsViewBridge の xcframework を生成する。
#
# デバイス (arm64) とシミュレータ (arm64 / x86_64) の両スライスを archive してから結合する。
# 生成物は build/KsSettingsViewBridge.xcframework。中間生成物 (DerivedData・xcarchive) も
# すべて build/ 配下へ閉じ込めるため、作り直したいときは build/ を消せばよい。
#
# 前提:
#   - Xcode のバージョンは .NET for iOS workload が要求するものに合わせる (DEVELOPER_DIR で指定)。
#   - Xcode project は KsSettingsViewBridge / KsSettingsViewUI / KsSettingsViewCore を
#     ひとつの静的ライブラリへまとめる (MACH_O_TYPE = staticlib)。
#   - 静的ライブラリなので署名は不要。archive では署名関連の設定を無効化して、
#     署名環境の有無に結果が左右されないようにしている。

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="${PROJECT_DIR}/KsSettingsViewBridge.xcodeproj"
SCHEME="KsSettingsViewBridge"
CONFIGURATION="${CONFIGURATION:-Release}"
BUILD_DIR="${PROJECT_DIR}/build"
DERIVED_DATA_DIR="${BUILD_DIR}/DerivedData"
FRAMEWORK_PATH="Products/Library/Frameworks/${SCHEME}.framework"

archive_slice() {
    local destination="$1"
    local archive_path="$2"
    xcodebuild archive \
        -project "${PROJECT}" \
        -scheme "${SCHEME}" \
        -configuration "${CONFIGURATION}" \
        -destination "${destination}" \
        -archivePath "${archive_path}" \
        -derivedDataPath "${DERIVED_DATA_DIR}" \
        SKIP_INSTALL=NO \
        BUILD_LIBRARY_FOR_DISTRIBUTION=YES \
        CODE_SIGNING_ALLOWED=NO \
        CODE_SIGNING_REQUIRED=NO \
        CODE_SIGN_IDENTITY=""

    # archive 自体は成功しても framework が install されないことがある (scheme が
    # project の target ではなく同名の SwiftPM product へ解決された場合など)。
    # 後段の -create-xcframework が読みにくいエラーで落ちる前にここで検出する。
    if [ ! -d "${archive_path}.xcarchive/${FRAMEWORK_PATH}" ]; then
        echo "エラー: ${archive_path}.xcarchive に ${SCHEME}.framework が install されていません。" >&2
        echo "  scheme '${SCHEME}' が Xcode project の target を指しているか確認してください" >&2
        echo "  (共有 scheme: ${PROJECT}/xcshareddata/xcschemes/${SCHEME}.xcscheme)。" >&2
        exit 1
    fi
}

mkdir -p "${BUILD_DIR}"
archive_slice "generic/platform=iOS" "${BUILD_DIR}/ios"
archive_slice "generic/platform=iOS Simulator" "${BUILD_DIR}/iossimulator"

XCFRAMEWORK="${BUILD_DIR}/${SCHEME}.xcframework"
if [ -d "${XCFRAMEWORK}" ]; then
    rm -rf "${XCFRAMEWORK}"
fi

xcodebuild -create-xcframework \
    -framework "${BUILD_DIR}/ios.xcarchive/${FRAMEWORK_PATH}" \
    -framework "${BUILD_DIR}/iossimulator.xcarchive/${FRAMEWORK_PATH}" \
    -output "${XCFRAMEWORK}"

echo "生成しました: ${XCFRAMEWORK}"
