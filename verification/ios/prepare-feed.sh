#!/bin/bash
# iOS 消費者検証のフィード準備。
#
# dry-run の参照先となるスナップショットを作る。配信リポジトリと同じファイル配置を持ち、
# ディレクトリ名が `KsSettingsView-SPM` になるディレクトリを用意し、
# scripts/spm-snapshot/sync-snapshot.sh でモノレポの iOS package を同期する。
# package identity はこのディレクトリ名から決まる。
#
# 使い方:
#   prepare-feed.sh [--mode <dry-run|smoke>] [--version <version>] [--work <dir>]
#
# 標準出力の最終行に、準備した参照先の絶対パスを出す (build-consumer.sh の --reference に渡せる)。
# smoke では公開レジストリを参照するため何も準備せず、参照先も出力しない。
#
# 同期スクリプトは、同期先が origin remote に配信リポジトリを持つ git top-level である
# ことを要求する。commit も push もしないので、ここでは空の作業コピーを初期化して
# origin だけを設定する。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"

KSV_PLATFORM="ios"
# iOS はスナップショット参照のため version を持たない。
KSV_DEFAULT_VERSION=""
# shellcheck source=../lib/verification-args.sh
. "${REPO_ROOT}/verification/lib/verification-args.sh"

readonly DISTRIBUTION_REMOTE="https://github.com/kamusoft/KsSettingsView-SPM.git"

ksv_parse_args "$@"

if [ "${KSV_MODE}" = "smoke" ]; then
    echo "smoke では公開レジストリ (配信リポジトリの tag) を参照するため、ローカル参照先は準備しない"
    exit 0
fi

work="${KSV_WORK:-${TMPDIR:-/tmp}/kssettingsview-verification/ios}"
mkdir -p "${work}"
work="$(cd "${work}" && pwd -P)"

snapshot="${work}/KsSettingsView-SPM"
mkdir -p "${snapshot}"

if [ ! -d "${snapshot}/.git" ]; then
    git -C "${snapshot}" init --quiet
fi

if git -C "${snapshot}" remote get-url origin >/dev/null 2>&1; then
    git -C "${snapshot}" remote set-url origin "${DISTRIBUTION_REMOTE}"
else
    git -C "${snapshot}" remote add origin "${DISTRIBUTION_REMOTE}"
fi

"${REPO_ROOT}/scripts/spm-snapshot/sync-snapshot.sh" "${snapshot}"

echo "${snapshot}"
