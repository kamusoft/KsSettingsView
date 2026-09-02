#!/bin/bash
# SwiftPM 配信リポジトリ (KsSettingsView-SPM) の作業コピーへ、iOS package のスナップショットを同期する。
#
# 使い方:
#   scripts/spm-snapshot/sync-snapshot.sh <配信リポジトリのチェックアウト済み作業コピー>
#
# 配置するのは次の 5 点だけで、それ以外の既存内容 (`.git` を除く) は除去する。
#
#   ios/Package.swift          -> <同期先>/Package.swift
#   ios/Sources/               -> <同期先>/Sources/
#   ios/Tests/                 -> <同期先>/Tests/
#   LICENSE                    -> <同期先>/LICENSE
#   README.template.md         -> <同期先>/README.md
#
# 責務の境界:
#   - git 操作 (commit / tag / push / remote 設定の変更) は一切行わない。結果は未コミットの
#     working tree の変更として残るので、確認したうえで呼び出し側が commit する。
#   - ネットワークにアクセスしない。
#   - 何度実行しても同じ結果になる (冪等)。
#
# 安全のため、除去を含む破壊的操作の前に次を全件検証する。1 つでも満たさない場合は同期先を
# 一切変更せずに異常終了する。引数の誤指定で無関係な作業ツリーを消さないための歯止めである。
#
#   1. コピー元 5 点がすべて存在する
#   2. 同期先が (シンボリックリンクを解決した上で) git top-level ディレクトリそのものである
#   3. 同期先の origin remote URL が配信リポジトリを指す
#   4. 同期先が monorepo 自身、または monorepo を含む祖先ディレクトリでない
#
# 検証 4 の判定だけを単体で確かめたい場合のために、同期を行わない内部モードを持つ:
#
#   sync-snapshot.sh --self-or-ancestor-check <候補パス> <内側のパス>
#
# 候補パスが内側のパス自身またはその祖先なら 0、そうでなければ 1 で終了する。実在しない
# パスでも判定できるので、filesystem root のような実際には渡せない値も検査できる。

set -euo pipefail

# 配信リポジトリの識別子。リポジトリを rename した場合はこの定数を追随させる。
readonly DISTRIBUTION_REPO="kamusoft/KsSettingsView-SPM"

usage() {
    echo "使い方: $(basename "${BASH_SOURCE[0]}") <配信リポジトリの作業コピー>" >&2
}

fail() {
    echo "エラー: $1" >&2
    exit 1
}

# 第 1 引数が第 2 引数自身、または第 2 引数を含む祖先ディレクトリなら真を返す。
# 双方の末尾を区切り文字で揃えてから前方一致を見るため、`/foo` と `/foobar` を取り違えず、
# filesystem root (`/`) も「あらゆるパスの祖先」として正しく判定できる。
is_self_or_ancestor_of() {
    local candidate="${1%/}/"
    local inner="${2%/}/"
    [ "${inner#"${candidate}"}" != "${inner}" ]
}

if [ "${1:-}" = "--self-or-ancestor-check" ]; then
    if [ $# -ne 3 ]; then
        echo "使い方: $(basename "${BASH_SOURCE[0]}") --self-or-ancestor-check <候補パス> <内側のパス>" >&2
        exit 2
    fi
    if is_self_or_ancestor_of "$2" "$3"; then
        exit 0
    fi
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)" \
    || fail "スクリプト自身の位置を解決できません"
readonly SCRIPT_DIR
MONOREPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)" \
    || fail "monorepo のルートを解決できません: ${SCRIPT_DIR}/../.."
readonly MONOREPO_ROOT
readonly README_TEMPLATE="${SCRIPT_DIR}/README.template.md"

if [ $# -ne 1 ]; then
    usage
    exit 1
fi

readonly DESTINATION_ARG="$1"

# --- 検証 1: コピー元 5 点の存在 -------------------------------------------------

[ -f "${MONOREPO_ROOT}/ios/Package.swift" ] || fail "コピー元が見つかりません: ios/Package.swift"
[ -d "${MONOREPO_ROOT}/ios/Sources" ]      || fail "コピー元が見つかりません: ios/Sources/"
[ -d "${MONOREPO_ROOT}/ios/Tests" ]        || fail "コピー元が見つかりません: ios/Tests/"
[ -f "${MONOREPO_ROOT}/LICENSE" ]          || fail "コピー元が見つかりません: LICENSE"
[ -f "${README_TEMPLATE}" ]                || fail "コピー元が見つかりません: ${README_TEMPLATE}"

# --- 検証 2: 同期先が git top-level ディレクトリそのものであること -----------------

[ -d "${DESTINATION_ARG}" ] || fail "同期先がディレクトリではありません: ${DESTINATION_ARG}"

# シンボリックリンク・相対表記を解決した canonical path で比較する。
DESTINATION="$(cd "${DESTINATION_ARG}" && pwd -P)"
readonly DESTINATION

destination_toplevel="$(git -C "${DESTINATION}" rev-parse --show-toplevel 2>/dev/null || true)"
[ -n "${destination_toplevel}" ] || fail "同期先が git リポジトリではありません: ${DESTINATION}"

destination_toplevel="$(cd "${destination_toplevel}" && pwd -P)"
if [ "${destination_toplevel}" != "${DESTINATION}" ]; then
    fail "同期先が git top-level ディレクトリではありません (top-level: ${destination_toplevel}): ${DESTINATION}"
fi

# --- 検証 3: origin remote URL が配信リポジトリを指すこと --------------------------

origin_url="$(git -C "${DESTINATION}" remote get-url origin 2>/dev/null || true)"
[ -n "${origin_url}" ] || fail "同期先に origin remote がありません: ${DESTINATION}"

# 末尾の `.git` とスラッシュの表記ゆれだけを吸収し、残りは受理する URL 形式を列挙して照合する
# (部分一致で判定すると `.../evil/kamusoft/KsSettingsView-SPM` のような URL も通ってしまう)。
normalized_origin="${origin_url%/}"
normalized_origin="${normalized_origin%.git}"
case "${normalized_origin}" in
    "https://github.com/${DISTRIBUTION_REPO}") ;;
    "ssh://git@github.com/${DISTRIBUTION_REPO}") ;;
    "git@github.com:${DISTRIBUTION_REPO}") ;;
    *)
        fail "同期先の origin が配信リポジトリ (${DISTRIBUTION_REPO}) を指していません: ${origin_url}"
        ;;
esac

# --- 検証 4: 同期先が monorepo 自身・その祖先でないこと ----------------------------

if is_self_or_ancestor_of "${DESTINATION}" "${MONOREPO_ROOT}"; then
    fail "同期先が monorepo 自身、または monorepo を含む祖先ディレクトリです: ${DESTINATION}"
fi

# --- ここから破壊的操作 ------------------------------------------------------------

# 未コミットの内容もこの後の除去で消えるため、失う前に知らせる (中断はしない — CI から
# 呼ぶ経路を止めないため)。
if [ -n "$(git -C "${DESTINATION}" status --porcelain)" ]; then
    echo "警告: 同期先に未コミットの変更があります。この同期で失われます: ${DESTINATION}" >&2
fi

# `.git` (worktree ではファイルになる) だけを残して既存内容を除去する。
while IFS= read -r -d '' entry; do
    if [ "$(basename "${entry}")" = ".git" ]; then
        continue
    fi
    rm -rf "${entry}"
done < <(find "${DESTINATION}" -mindepth 1 -maxdepth 1 -print0)

cp "${MONOREPO_ROOT}/ios/Package.swift" "${DESTINATION}/Package.swift"
cp -R "${MONOREPO_ROOT}/ios/Sources" "${DESTINATION}/Sources"
cp -R "${MONOREPO_ROOT}/ios/Tests" "${DESTINATION}/Tests"
cp "${MONOREPO_ROOT}/LICENSE" "${DESTINATION}/LICENSE"
cp "${README_TEMPLATE}" "${DESTINATION}/README.md"

echo "同期しました: ${DESTINATION}"
