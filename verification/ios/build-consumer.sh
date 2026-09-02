#!/bin/bash
# iOS 消費者のビルド。
#
# Package.swift.template から mode に応じた Package.swift を作業ディレクトリに生成し、
# 利用者向けドキュメントの iOS 最小例を含む Sources/ を並べて、iOS Simulator 向けの
# Release ビルドを行う。署名情報は要求しない。
#
# 使い方:
#   build-consumer.sh [--mode <dry-run|smoke>] [--version <version>]
#                     [--reference <snapshot dir>] [--work <dir>]
#
# --reference を与えると、その参照先をそのまま使いフィード準備を行わない。
# 与えない dry-run では prepare-feed.sh を呼んで参照先を用意する。
#
# 解決結果の証跡として、生成した Package.swift と依存グラフを標準出力に出す。
# path 参照には version が無いため dry-run では Package.resolved が生成されない。
# smoke では解決後の Package.resolved を出す。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="ios"
KSV_DEFAULT_VERSION=""
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

readonly DISTRIBUTION_URL="https://github.com/kamusoft/KsSettingsView-SPM"

ksv_parse_args "$@"

work="${KSV_WORK:-${TMPDIR:-/tmp}/kssettingsview-verification/ios}"
mkdir -p "${work}"
work="$(cd "${work}" && pwd -P)"

reference="${KSV_REFERENCE}"
if [ "${KSV_MODE}" = "dry-run" ] && [ -z "${reference}" ]; then
    reference="$("${SCRIPT_DIR}/prepare-feed.sh" --mode "${KSV_MODE}" --work "${work}" | tail -n 1)"
fi

if [ "${KSV_MODE}" = "dry-run" ]; then
    [ -d "${reference}" ] || ksv_fail "参照先がディレクトリではありません: ${reference}"
    reference="$(cd "${reference}" && pwd -P)"
    [ "$(basename "${reference}")" = "KsSettingsView-SPM" ] \
        || ksv_fail "参照先のディレクトリ名が KsSettingsView-SPM ではありません (package identity が変わります): ${reference}"
    dependency=".package(path: \"${reference}\")"
else
    dependency=".package(url: \"${DISTRIBUTION_URL}\", exact: \"${KSV_VERSION}\")"
fi

consumer="${work}/consumer"
mkdir -p "${consumer}/Sources"
# ソースは毎回コピーし直す。作業ディレクトリを再利用しても前回の内容が残らないようにする。
find "${consumer}/Sources" -mindepth 1 -maxdepth 1 -exec rm -rf {} +
cp -R "${SCRIPT_DIR}/Sources/VerificationApp" "${consumer}/Sources/VerificationApp"

python3 - "${SCRIPT_DIR}/Package.swift.template" "${consumer}/Package.swift" "${dependency}" <<'PY'
import sys

template_path, output_path, dependency = sys.argv[1], sys.argv[2], sys.argv[3]
with open(template_path, encoding="utf-8") as f:
    template = f.read()
# 差し込み口はちょうど 1 か所。コメント等に増えると意図しない位置まで置換される。
placeholder = "@KSSV_" + "DEPENDENCY@"
occurrences = template.count(placeholder)
if occurrences != 1:
    raise SystemExit(f"テンプレートの差し込み口が 1 か所ではない: {occurrences} か所")
with open(output_path, "w", encoding="utf-8") as f:
    f.write(template.replace(placeholder, dependency))
PY

ksv_evidence "生成した Package.swift" < "${consumer}/Package.swift"

(cd "${consumer}" && swift package show-dependencies) | ksv_evidence "依存グラフ"

echo "==== Release ビルド (iOS Simulator 向け) ===="
(cd "${consumer}" && xcodebuild \
    -scheme VerificationApp \
    -destination 'generic/platform=iOS Simulator' \
    -configuration Release \
    -derivedDataPath "${work}/DerivedData" \
    build)

if [ -f "${consumer}/Package.resolved" ]; then
    ksv_evidence "Package.resolved" < "${consumer}/Package.resolved"
elif [ "${KSV_MODE}" = "smoke" ]; then
    ksv_fail "smoke なのに Package.resolved が生成されていない: ${consumer}"
else
    echo "dry-run の依存は path 参照だけで version constraint を持たないため、Package.resolved は生成されない" \
        | ksv_evidence "Package.resolved"
fi
