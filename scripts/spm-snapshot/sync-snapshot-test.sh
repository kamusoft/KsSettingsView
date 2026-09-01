#!/bin/bash
# sync-snapshot.sh のテスト。
#
# 使い方: scripts/spm-snapshot/sync-snapshot-test.sh
#
# 一時ディレクトリに配信リポジトリを模した git リポジトリを作って同期先とし、配置内容・冪等性・
# 誤指定の拒否・コピー元不足時の無変更・git 非操作を検証する。monorepo 側は読むだけで変更しない。

set -uo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
readonly SYNC_SCRIPT="${SCRIPT_DIR}/sync-snapshot.sh"
readonly MONOREPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd -P)"
readonly DISTRIBUTION_ORIGIN="https://github.com/kamusoft/KsSettingsView-SPM.git"

readonly WORK_ROOT="$(mktemp -d)"
trap 'rm -rf "${WORK_ROOT}"' EXIT

# 内容比較に使うハッシュコマンド。macOS は shasum、Linux は sha256sum を持つ。
if command -v shasum > /dev/null 2>&1; then
    HASH_TOOL="shasum"
elif command -v sha256sum > /dev/null 2>&1; then
    HASH_TOOL="sha256sum"
else
    echo "エラー: shasum / sha256sum のいずれも見つかりません (内容比較に必要)" >&2
    exit 1
fi
readonly HASH_TOOL

failures=0
current_case=""

begin_case() {
    current_case="$1"
}

pass() {
    echo "  ok   : ${current_case} — $1"
}

fail() {
    echo "  FAIL : ${current_case} — $1" >&2
    failures=$((failures + 1))
}

assert_equals() {
    local expected="$1" actual="$2" message="$3"
    if [ "${expected}" = "${actual}" ]; then
        pass "${message}"
    else
        fail "${message} (期待: '${expected}' / 実際: '${actual}')"
    fi
}

# 配信リポジトリを模した作業コピーを作る。第 2 引数を渡すとその URL を origin にする。
make_destination() {
    local name="$1"
    local origin="${2:-${DISTRIBUTION_ORIGIN}}"
    local path="${WORK_ROOT}/${name}"
    mkdir -p "${path}"
    git -C "${path}" init --quiet --initial-branch=main
    git -C "${path}" remote add origin "${origin}"
    echo "${path}"
}

# monorepo を模した最小のコピー元を作り、その中に置いた同期スクリプトのパスを返す。
# スクリプトは自身の位置から 2 階層上を monorepo ルートとみなすので、この配置がそのまま
# 「どのディレクトリを monorepo と見るか」を決める。
make_fake_monorepo() {
    local root="$1"
    local script_dir="${root}/scripts/spm-snapshot"
    mkdir -p "${script_dir}" "${root}/ios/Sources" "${root}/ios/Tests"
    cp "${SYNC_SCRIPT}" "${script_dir}/sync-snapshot.sh"
    cp "${SCRIPT_DIR}/README.template.md" "${script_dir}/README.template.md"
    touch "${root}/ios/Package.swift" "${root}/LICENSE"
    echo "${script_dir}/sync-snapshot.sh"
}

# `.git` を除いた同期先の内容を、パスとファイル種別の一覧として安定した順序で書き出す。
snapshot_tree() {
    local path="$1"
    ( cd "${path}" && find . -mindepth 1 -path ./.git -prune -o -print | sort )
}

# ディレクトリ配下の相対パス一覧と全ファイルの内容を、単一のハッシュにまとめる。
# コピー元と同期先を突き合わせて、木の形と中身の両方が一致することを確かめるのに使う。
tree_fingerprint() {
    local path="$1"
    (
        cd "${path}" || exit 1
        find . -mindepth 1 | sort
        find . -type f -print0 | sort -z | xargs -0 "${HASH_TOOL}"
    ) | "${HASH_TOOL}"
}

run_sync() {
    "${SYNC_SCRIPT}" "$@" > /dev/null 2>&1
}

# 祖先判定の内部モードを呼び、期待どおりに判定されることを確かめる。
assert_ancestor() {
    local candidate="$1" inner="$2" expected="$3" message="$4"
    local actual status=0
    "${SYNC_SCRIPT}" --self-or-ancestor-check "${candidate}" "${inner}" > /dev/null 2>&1 || status=$?
    case "${status}" in
        0) actual="祖先" ;;
        1) actual="非祖先" ;;
        *) actual="判定エラー (exit ${status})" ;;
    esac
    assert_equals "${expected}" "${actual}" "${message}"
}

echo "sync-snapshot.sh のテスト"

# --- ホワイトリスト 5 点の配置 ---------------------------------------------------

begin_case "ホワイトリスト 5 点の配置"
destination="$(make_destination "whitelist")"
run_sync "${destination}"
assert_equals "0" "$?" "同期が成功する"
assert_equals "LICENSE Package.swift README.md Sources Tests" \
    "$(cd "${destination}" && ls -A | grep -v '^\.git$' | sort | tr '\n' ' ' | sed 's/ $//')" \
    "直下に 5 点だけが存在する"
[ -f "${destination}/Package.swift" ] && [ -d "${destination}/Sources" ] && [ -d "${destination}/Tests" ] \
    && pass "Package.swift はファイル、Sources / Tests はディレクトリとして配置される" \
    || fail "5 点の種別が期待と異なる"
assert_equals "$(cat "${MONOREPO_ROOT}/ios/Package.swift")" "$(cat "${destination}/Package.swift")" \
    "Package.swift の内容がコピー元と一致する"
assert_equals "$(cat "${SCRIPT_DIR}/README.template.md")" "$(cat "${destination}/README.md")" \
    "README.md はテンプレートの内容になる"
assert_equals "$(cat "${MONOREPO_ROOT}/LICENSE")" "$(cat "${destination}/LICENSE")" \
    "LICENSE の内容がコピー元と一致する"
assert_equals "$(tree_fingerprint "${MONOREPO_ROOT}/ios/Sources")" "$(tree_fingerprint "${destination}/Sources")" \
    "Sources/ の相対パス一覧とファイル内容がコピー元と一致する"
assert_equals "$(tree_fingerprint "${MONOREPO_ROOT}/ios/Tests")" "$(tree_fingerprint "${destination}/Tests")" \
    "Tests/ の相対パス一覧とファイル内容がコピー元と一致する"

# --- 列挙外ファイルの混入防止 ----------------------------------------------------

begin_case "列挙外ファイルの混入防止"
destination="$(make_destination "leftover")"
mkdir -p "${destination}/Sources/Stale" "${destination}/docs"
touch "${destination}/Sources/Stale/Old.swift" "${destination}/docs/note.md" \
    "${destination}/CHANGELOG.md" "${destination}/.hidden"
run_sync "${destination}"
assert_equals "0" "$?" "同期が成功する"
assert_equals "LICENSE Package.swift README.md Sources Tests" \
    "$(cd "${destination}" && ls -A | grep -v '^\.git$' | sort | tr '\n' ' ' | sed 's/ $//')" \
    "列挙外の残骸 (隠しファイル含む) がすべて除去される"
[ -e "${destination}/Sources/Stale" ] \
    && fail "前回スナップショットの残骸がディレクトリ内に残っている" \
    || pass "ディレクトリ内部の残骸も残らない"

# --- 冪等性 -----------------------------------------------------------------------

begin_case "冪等性"
destination="$(make_destination "idempotent")"
run_sync "${destination}"
first="$(snapshot_tree "${destination}")"
first_hash="$(cd "${destination}" && find . -path ./.git -prune -o -type f -print0 | sort -z | xargs -0 "${HASH_TOOL}" | "${HASH_TOOL}")"
run_sync "${destination}"
assert_equals "0" "$?" "2 回目の同期が成功する"
assert_equals "${first}" "$(snapshot_tree "${destination}")" "2 回目の実行後もファイル一覧が同一である"
assert_equals "${first_hash}" \
    "$(cd "${destination}" && find . -path ./.git -prune -o -type f -print0 | sort -z | xargs -0 "${HASH_TOOL}" | "${HASH_TOOL}")" \
    "2 回目の実行後も全ファイルの内容が同一である"

# --- 同期先の誤指定の拒否 ----------------------------------------------------------

begin_case "同期先の誤指定の拒否 (git top-level でない)"
plain="${WORK_ROOT}/not-a-repo"
mkdir -p "${plain}"
touch "${plain}/keep.txt"
run_sync "${plain}"
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
assert_equals "./keep.txt" "$(snapshot_tree "${plain}")" "同期先の内容が変更されない"

begin_case "同期先の誤指定の拒否 (git リポジトリのサブディレクトリ)"
destination="$(make_destination "subdir-repo")"
mkdir -p "${destination}/nested"
touch "${destination}/nested/keep.txt"
run_sync "${destination}/nested"
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
assert_equals "./keep.txt" "$(snapshot_tree "${destination}/nested")" "同期先の内容が変更されない"

begin_case "同期先の誤指定の拒否 (origin が配信リポジトリでない)"
destination="$(make_destination "wrong-origin" "https://github.com/kamusoft/KsSettingsView.git")"
touch "${destination}/keep.txt"
run_sync "${destination}"
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
assert_equals "./keep.txt" "$(snapshot_tree "${destination}")" "同期先の内容が変更されない"

begin_case "同期先の誤指定の拒否 (origin が配信リポジトリ名を含む別 URL)"
destination="$(make_destination "lookalike-origin" "https://github.com/evil/kamusoft/KsSettingsView-SPM.git")"
touch "${destination}/keep.txt"
run_sync "${destination}"
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
assert_equals "./keep.txt" "$(snapshot_tree "${destination}")" "同期先の内容が変更されない"

begin_case "同期先の誤指定の拒否 (monorepo 自身)"
# 配信リポジトリの origin を持つ git top-level 自身を monorepo のルートに仕立てる。こうすると
# 検証 1〜3 をすべて通過するため、自己指定を止めているのが検証 4 であることを確かめられる。
self_root="${WORK_ROOT}/self-monorepo"
mkdir -p "${self_root}"
git -C "${self_root}" init --quiet --initial-branch=main
git -C "${self_root}" remote add origin "${DISTRIBUTION_ORIGIN}"
self_sync="$(make_fake_monorepo "${self_root}")"
touch "${self_root}/keep.txt"
"${self_sync}" "${self_root}" > /dev/null 2>&1
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
[ -f "${self_root}/keep.txt" ] && [ -f "${self_root}/ios/Package.swift" ] && [ -f "${self_root}/LICENSE" ] \
    && pass "monorepo 自身の内容が変更されない" \
    || fail "monorepo 自身の内容が除去された"

begin_case "同期先の誤指定の拒否 (monorepo の祖先ディレクトリ)"
# monorepo の親を、配信リポジトリの origin を持つ git top-level に見せかけても拒否されること。
ancestor="${WORK_ROOT}/ancestor"
mkdir -p "${ancestor}"
git -C "${ancestor}" init --quiet --initial-branch=main
git -C "${ancestor}" remote add origin "${DISTRIBUTION_ORIGIN}"
touch "${ancestor}/keep.txt"
ancestor_sync="$(make_fake_monorepo "${ancestor}/repo")"
"${ancestor_sync}" "${ancestor}" > /dev/null 2>&1
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
[ -f "${ancestor}/keep.txt" ] && [ -f "${ancestor}/repo/ios/Package.swift" ] \
    && pass "祖先ディレクトリの内容が変更されない" \
    || fail "祖先ディレクトリの内容が除去された"

begin_case "祖先判定 (実ディレクトリを操作しない判定ロジック単体)"
# filesystem root は同期先として実際には渡せないので、判定モードだけを直接呼んで検査する。
assert_ancestor "/" "/some/where/monorepo" "祖先" "filesystem root はあらゆるパスの祖先と判定される"
assert_ancestor "/a/b" "/a/b" "祖先" "同一パスは自身として拒否対象になる"
assert_ancestor "/a/b/" "/a/b" "祖先" "末尾スラッシュの有無で判定が変わらない"
assert_ancestor "/a/b" "/a/bcd/e" "非祖先" "名前の前方一致だけでは祖先と判定しない"
assert_ancestor "/a/b/c" "/a/b" "非祖先" "内側のパスの子孫は祖先と判定しない"

# --- コピー元不足時の無変更 --------------------------------------------------------

begin_case "コピー元不足時の無変更"
# monorepo を模した最小のコピー元を作り、そこから 1 点だけ欠落させた状態で実行する。
fake_root="${WORK_ROOT}/fake-monorepo"
fake_sync="$(make_fake_monorepo "${fake_root}")"
destination="$(make_destination "missing-source")"
touch "${destination}/keep.txt"
rm -rf "${fake_root}/ios/Tests"
"${fake_sync}" "${destination}" > /dev/null 2>&1
assert_equals "1" "$([ $? -ne 0 ] && echo 1 || echo 0)" "非ゼロ終了する"
assert_equals "./keep.txt" "$(snapshot_tree "${destination}")" "同期先の内容が変更されない"

# --- git 非操作 --------------------------------------------------------------------

begin_case "git 非操作"
destination="$(make_destination "git-untouched")"
touch "${destination}/seed.txt"
git -C "${destination}" add seed.txt > /dev/null 2>&1
git -C "${destination}" \
    -c user.name=tester -c user.email=tester@example.com \
    commit --quiet -m "seed" > /dev/null 2>&1
head_before="$(git -C "${destination}" rev-parse HEAD)"
commits_before="$(git -C "${destination}" rev-list --count HEAD)"
tags_before="$(git -C "${destination}" tag | sort)"
index_before="$(git -C "${destination}" ls-files --stage | "${HASH_TOOL}")"
origin_before="$(git -C "${destination}" remote get-url origin)"
run_sync "${destination}"
assert_equals "0" "$?" "同期が成功する"
[ -d "${destination}/.git" ] && pass ".git が保持される" || fail ".git が失われた"
assert_equals "${head_before}" "$(git -C "${destination}" rev-parse HEAD)" "HEAD が変わらない"
assert_equals "${commits_before}" "$(git -C "${destination}" rev-list --count HEAD)" "commit が増えない"
assert_equals "${tags_before}" "$(git -C "${destination}" tag | sort)" "tag が増えない"
assert_equals "${index_before}" "$(git -C "${destination}" ls-files --stage | "${HASH_TOOL}")" "index が変わらない"
assert_equals "${origin_before}" "$(git -C "${destination}" remote get-url origin)" "remote 設定が変わらない"
[ -n "$(git -C "${destination}" status --porcelain)" ] \
    && pass "同期結果は未コミットの working tree の変更として残る" \
    || fail "working tree に変更が現れていない"

# --- 結果 --------------------------------------------------------------------------

echo
if [ "${failures}" -eq 0 ]; then
    echo "すべて成功しました。"
    exit 0
fi
echo "失敗: ${failures} 件" >&2
exit 1
