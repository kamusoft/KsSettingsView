#!/bin/bash
# Android 消費者検証のフィード準備。
#
# dry-run の参照先となる mavenLocal へ、本体ライブラリを指定 version で発行する。
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>] [--work <dir>]
#
# 標準出力の最終行に、準備した参照先 (ローカル Maven リポジトリ) の絶対パスを出す。
# smoke では Maven Central を参照するため何も準備せず、参照先も出力しない。
#
# --work は受け付けるが Android では効かない (発行先は mavenLocal、ビルド出力は Gradle の
# build ディレクトリで、いずれも作業ディレクトリの指定を持たない)。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="android"
# 本体の開発用既定値。バージョンカタログの kssettingsview キーが単一の宣言元。
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

if [ -z "${KSV_DEFAULT_VERSION}" ]; then
    ksv_fail "android/gradle/libs.versions.toml から kssettingsview の version を読めない"
fi

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (Maven Central) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

(cd "${REPO_ROOT}/android" && ./gradlew --console=plain \
    -Pversion="${KSV_VERSION}" \
    :kssettingsview:publishToMavenLocal)

# mavenLocal の位置。~/.m2/settings.xml に localRepository の指定があればそれに従い、
# 無ければ Maven の既定 ~/.m2/repository を使う。
maven_local="$(python3 -c '
import os
import xml.etree.ElementTree as ET

settings = os.path.expanduser("~/.m2/settings.xml")
location = ""
if os.path.isfile(settings):
    try:
        for element in ET.parse(settings).getroot().iter():
            if element.tag.rsplit("}", 1)[-1] == "localRepository" and element.text:
                # Maven の補間 ${user.home} は環境変数ではないので先に置き換えてから展開する
                location = os.path.expandvars(element.text.strip().replace("${user.home}", os.path.expanduser("~")))
                break
    except ET.ParseError:
        pass
print(location or os.path.expanduser("~/.m2/repository"))
')"

# 要求した version がそのまま発行されたことを確かめる。座標がずれたまま消費者ビルドへ
# 進むと、解決できた事実が「要求した版を解決できた」証拠にならなくなる。
published="${maven_local}/jp/kamusoft/kssettingsview/${KSV_VERSION}"
if [ ! -d "${published}" ]; then
    ksv_fail "要求した version が発行されていません: jp.kamusoft:kssettingsview:${KSV_VERSION} (${published} が無い)"
fi

echo "${maven_local}"
