#!/bin/bash
# 公開レジストリへの反映待ち。
#
# 使い方:
#   scripts/release/wait-for-registries.sh <version>
#
# publish 直後は、Maven Central も nuget.org も配信元へ同期されるまで数分から数十分かかる。
# 反映前に消費者検証 (smoke) を走らせると、利用者から解決できないのか単に間に合っていない
# だけなのかが区別できないため、次の 4 つすべてが取得可能になるまで待ってから先へ進める。
#
#   Maven Central  jp.kamusoft:kssettingsview の pom (repo1.maven.org)
#   nuget.org      KsSettingsView.Maui / KsSettingsView.Binding.iOS /
#                  KsSettingsView.Binding.Android の 3 Package ID
#
# nuget.org は flat container の index.json に含まれる version の一覧で判定する。index の
# version は小文字へ正規化されるため、比較も小文字で行う。
#
# 待機の間隔と上限は環境変数で上書きできる (テストと運用の調整用):
#   KSR_POLL_INTERVAL_SECONDS  ポーリング間隔 (既定 30)
#   KSR_POLL_TIMEOUT_SECONDS   上限 (既定 2700 = 45 分)

set -euo pipefail

readonly MAVEN_CENTRAL_BASE_URL="https://repo1.maven.org/maven2"
readonly MAVEN_GROUP_PATH="jp/kamusoft"
readonly MAVEN_ARTIFACT_ID="kssettingsview"

readonly NUGET_FLAT_CONTAINER_URL="https://api.nuget.org/v3-flatcontainer"
# flat container の URL は Package ID を小文字にしたものを使う。
readonly NUGET_PACKAGE_IDS=(
    "kssettingsview.maui"
    "kssettingsview.binding.ios"
    "kssettingsview.binding.android"
)

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <version>" >&2
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# Maven Central に当該 version の pom があれば 0。
maven_central_ready() {
    local version="$1"
    local url="${MAVEN_CENTRAL_BASE_URL}/${MAVEN_GROUP_PATH}/${MAVEN_ARTIFACT_ID}/${version}/${MAVEN_ARTIFACT_ID}-${version}.pom"
    local status
    status="$(curl --silent --show-error --location --head --output /dev/null \
        --connect-timeout 30 --max-time 120 --write-out '%{http_code}' "${url}" || echo "000")"
    [ "${status}" = "200" ]
}

# nuget.org の当該 Package ID の index に当該 version が含まれれば 0。
nuget_ready() {
    local package_id="$1" version="$2"
    local url="${NUGET_FLAT_CONTAINER_URL}/${package_id}/index.json"
    local body
    body="$(curl --silent --show-error --location \
        --connect-timeout 30 --max-time 120 "${url}" || echo "")"
    [ -n "${body}" ] || return 1
    printf '%s' "${body}" | KSR_WANTED_VERSION="${version}" python3 -c '
import json
import os
import sys

wanted = os.environ["KSR_WANTED_VERSION"].lower()
try:
    payload = json.load(sys.stdin)
except ValueError:
    sys.exit(1)
versions = payload.get("versions")
if not isinstance(versions, list):
    sys.exit(1)
sys.exit(0 if wanted in [str(v).lower() for v in versions] else 1)
'
}

if [ $# -ne 1 ]; then
    usage
    exit 2
fi

readonly VERSION="$1"
readonly INTERVAL="${KSR_POLL_INTERVAL_SECONDS:-30}"
readonly TIMEOUT="${KSR_POLL_TIMEOUT_SECONDS:-2700}"
readonly DEADLINE=$(( SECONDS + TIMEOUT ))

echo "反映を待ちます (version ${VERSION}、${INTERVAL} 秒間隔、上限 ${TIMEOUT} 秒)"

maven_done=0
declare -a nuget_done
for _ in "${NUGET_PACKAGE_IDS[@]}"; do
    nuget_done+=(0)
done

while :; do
    pending=()

    if [ "${maven_done}" -eq 0 ]; then
        if maven_central_ready "${VERSION}"; then
            maven_done=1
            echo "Maven Central に反映されました: ${MAVEN_ARTIFACT_ID}:${VERSION}"
        else
            pending+=("Maven Central")
        fi
    fi

    for index in "${!NUGET_PACKAGE_IDS[@]}"; do
        [ "${nuget_done[${index}]}" -eq 0 ] || continue
        package_id="${NUGET_PACKAGE_IDS[${index}]}"
        if nuget_ready "${package_id}" "${VERSION}"; then
            nuget_done[${index}]=1
            echo "nuget.org に反映されました: ${package_id} ${VERSION}"
        else
            pending+=("nuget.org/${package_id}")
        fi
    done

    if [ "${#pending[@]}" -eq 0 ]; then
        echo "4 件すべてが取得可能になりました: ${VERSION}"
        exit 0
    fi

    if [ "${SECONDS}" -ge "${DEADLINE}" ]; then
        fail "反映を待ちきれませんでした (上限 ${TIMEOUT} 秒、未反映: ${pending[*]})"
    fi

    echo "待機中 (未反映: ${pending[*]})"
    sleep "${INTERVAL}"
done
