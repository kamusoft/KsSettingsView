#!/bin/bash
# 保留中の Maven deployment を次の attempt へ引き継ぐための判定。
#
# 使い方:
#   scripts/release/deployment-handover.sh inspect <ディレクトリ>
#   scripts/release/deployment-handover.sh writeback <deployment-id> <deployment の状態>
#   scripts/release/deployment-handover.sh --selftest
#
# 同じ version の再実行は、前の attempt が Central Portal に残した保留中の deployment を
# 引き継いで決着から続ける。引き継ぎは artifact に置いた 1 ファイル (deployment-id.txt) で、
# 内容が空白だけなら「引き継ぐ ID は無い」を意味する。
#
#   inspect     引き継ぎファイルを読み、次の 3 つを区別して出力する。出力は
#               "<キー>=<値>" の行で、workflow はそのまま step の output に写す。
#
#                 引き継ぎあり  loaded=true  present=true   deployment-id=<ID>
#                 引き継ぎなし  loaded=true  present=false  deployment-id=
#                 読み込み失敗  出力を出さずに失敗する (loaded は true にならない)
#
#               「引き継ぎが無い初回」と「読み込みそのものの失敗」を同じ扱いにすると、
#               引き継ぐべき deployment があるのに upload をやり直し、保留中の
#               deployment を二重に作ってしまう。
#
#   writeback   後始末を終えた deployment の引き継ぎをどう扱うかを判定し、次の 1 語を出す。
#
#                 clear  空の内容で上書きしてよい (deployment が消えたことを確認済み)
#                 store  手元の ID をそのまま引き継ぐ (deployment はまだ残っている)
#
#               引数の契約: deployment-id は**必ず非空で渡す**。手元の ID が何かを
#               知らないまま引き継ぎファイルを書き換えてよいかは決められないため、空で
#               呼ばれたら判定を返さずに失敗する。呼び出し側は「ID を持っているとき
#               だけ後始末に入る」形で守ること。deployment の状態は照会できた値をその
#               まま渡す (空や未知の語は「まだ残っている」側に倒れて store になる)。
#
#               空で上書きしてよいのは、自分が持っている ID の deployment が消えたと
#               確認できた場合だけ。それ以外で空にすると、まだ生きている引き継ぎを消し、
#               保留中の deployment が迷子になる。
#
# ネットワークにも Central Portal にも触らない。状態の照会は呼び出し側が行い、その結果を
# writeback の引数として渡す。

set -euo pipefail

# 引き継ぎを置くファイル名。workflow が artifact として上げ下げする単位でもある。
readonly HANDOVER_FILE_NAME="deployment-id.txt"

# deployment が存在しないことを表す状態。central-portal.sh が status で出す語彙に合わせる。
readonly DEPLOYMENT_NOT_FOUND="NOT_FOUND"

readonly DECISION_STORE="store"
readonly DECISION_CLEAR="clear"

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <サブコマンド> <引数>

  inspect <ディレクトリ>              引き継ぎを読み、有無を key=value で出力する
  writeback <deployment-id> <状態>    後始末の後の引き継ぎの扱いを判定する (ID は非空)
  --selftest                          自己テストを実行する
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

handover_path() {
    printf '%s/%s' "${1%/}" "${HANDOVER_FILE_NAME}"
}

# 引き継ぎファイルを読み、有無を key=value の行で出す。読めなければ失敗する。
cmd_inspect() {
    local directory="$1"
    local file
    file="$(handover_path "${directory}")"

    if [ ! -e "${file}" ] && [ ! -L "${file}" ]; then
        # 初回の attempt には引き継ぎ artifact が無い。ファイルが作られないのが正常。
        printf 'loaded=true\npresent=false\ndeployment-id=\n'
        return 0
    fi
    if [ ! -f "${file}" ]; then
        fail "引き継ぎが通常のファイルではない: ${file}"
    fi
    if [ ! -r "${file}" ]; then
        fail "引き継ぎを読み取れない: ${file}"
    fi

    local content
    if ! content="$(tr -d '[:space:]' < "${file}")"; then
        fail "引き継ぎの読み取りに失敗した: ${file}"
    fi
    if [ -z "${content}" ]; then
        # 後始末が deployment を消したときは、空白だけの内容で保存し直される。
        printf 'loaded=true\npresent=false\ndeployment-id=\n'
        return 0
    fi
    printf 'loaded=true\npresent=true\ndeployment-id=%s\n' "${content}"
}

# 後始末を終えた deployment の引き継ぎの扱いを判定する。
cmd_writeback() {
    local deployment_id="$1" state="$2"
    deployment_id="$(printf '%s' "${deployment_id}" | tr -d '[:space:]')"

    if [ -z "${deployment_id}" ]; then
        # 手元の ID を知らないまま引き継ぎファイルに触ってよいかは決められない。黙って
        # 現状維持にすると、呼び出し側が ID を取り違えている事実も一緒に隠れる。
        fail "後始末の判定には deployment ID が要る (空で呼ばれた)"
    fi
    if [ "${state}" = "${DEPLOYMENT_NOT_FOUND}" ]; then
        # 後始末で実際に消えた deployment の ID を次の attempt へ渡すと、存在しない ID の
        # 照会から始まることになる。消えたことを確認できたここだけが、空で上書きしてよい。
        printf '%s\n' "${DECISION_CLEAR}"
        return 0
    fi
    # deployment はまだ残っている。次の attempt が続きを行えるよう ID を引き継ぐ。
    printf '%s\n' "${DECISION_STORE}"
}

# --- 自己テスト ----------------------------------------------------------------------

selftest() {
    SELFTEST_WORK="$(mktemp -d)"
    trap 'chmod -R u+rwx "${SELFTEST_WORK}" 2>/dev/null || true; rm -rf "${SELFTEST_WORK}"' EXIT
    local work="${SELFTEST_WORK}"

    local failures=0

    check() {
        local ok="$1" name="$2" detail="${3:-}"
        if [ "${ok}" = "0" ]; then
            echo "  OK   ${name}"
        else
            echo "  NG   ${name}${detail:+ (${detail})}"
            failures=$((failures + 1))
        fi
    }

    contains() {
        if [ "${1#*"$2"}" != "$1" ]; then
            echo 0
        else
            echo 1
        fi
    }

    # 引き継ぎディレクトリを作り直す補助。
    fresh_dir() {
        local directory="${work}/$1"
        chmod -R u+rwx "${directory}" 2>/dev/null || true
        rm -rf "${directory}"
        mkdir -p "${directory}"
        printf '%s' "${directory}"
    }

    echo "[引き継ぎあり]"
    local directory output code
    directory="$(fresh_dir with-id)"
    printf '8f0c5a12-0000-4000-8000-000000000001\n' > "${directory}/${HANDOVER_FILE_NAME}"
    output="$(cmd_inspect "${directory}")"
    check "$(contains "${output}" "present=true")" "引き継ぎがあれば present=true" "${output}"
    check "$(contains "${output}" "deployment-id=8f0c5a12-0000-4000-8000-000000000001")" \
        "引き継いだ ID を出力する" "${output}"
    check "$(contains "${output}" "loaded=true")" "読み込み済みであることを出力する" "${output}"

    echo "[初回で引き継ぎなし]"
    directory="$(fresh_dir first-attempt)"
    output="$(cmd_inspect "${directory}")"
    check "$(contains "${output}" "present=false")" "ファイルが無ければ present=false" "${output}"
    check "$(contains "${output}" "loaded=true")" "ファイルが無くても読み込みは成立する" "${output}"
    check "$(contains "${output}" "deployment-id=")" "ID は空で出力する" "${output}"

    # 後始末が空の内容で保存し直した引き継ぎも、引き継ぐものが無い扱いになる。
    directory="$(fresh_dir cleared)"
    printf '\n' > "${directory}/${HANDOVER_FILE_NAME}"
    output="$(cmd_inspect "${directory}")"
    check "$(contains "${output}" "present=false")" "空白だけの内容は引き継ぎなし" "${output}"

    echo "[引き継ぎの読み込み失敗]"
    directory="$(fresh_dir unreadable-kind)"
    mkdir -p "${directory}/${HANDOVER_FILE_NAME}"
    code=0
    output="$(cmd_inspect "${directory}" 2>&1)" || code=$?
    check "$([ "${code}" != "0" ] && echo 0 || echo 1)" "通常のファイルでなければ失敗する" "exit ${code}"
    check "$([ "$(contains "${output}" "loaded=true")" = "1" ] && echo 0 || echo 1)" \
        "読み込みに失敗したら loaded=true を出さない" "${output}"

    directory="$(fresh_dir unreadable-mode)"
    printf 'abc\n' > "${directory}/${HANDOVER_FILE_NAME}"
    chmod 000 "${directory}/${HANDOVER_FILE_NAME}"
    if [ -r "${directory}/${HANDOVER_FILE_NAME}" ]; then
        # 権限を無視できる実行者 (root) では、この経路を再現できないので飛ばす。
        echo "  --   権限で読めないファイルの検査は飛ばす (実行者が読めてしまう)"
    else
        code=0
        output="$(cmd_inspect "${directory}" 2>&1)" || code=$?
        check "$([ "${code}" != "0" ] && echo 0 || echo 1)" "権限で読めなければ失敗する" "exit ${code}"
        check "$([ "$(contains "${output}" "present=false")" = "1" ] && echo 0 || echo 1)" \
            "読めないファイルを引き継ぎなしに畳み込まない" "${output}"
    fi

    echo "[読み込み後に別の成果物の取得が失敗]"
    # 引き継ぎは読めているが、この attempt は deployment を作らないまま失敗する。
    # 手元に ID が無いので後始末の判定にも入れない。呼び出し側のゲートが外れてここへ
    # 来たら、黙って現状維持にせず契約違反として落とす。
    local code=0
    ( cmd_writeback "" "" > /dev/null 2>&1 ) || code=$?
    check "$([ "${code}" != "0" ] && echo 0 || echo 1)" \
        "ID を持たない呼び出しは判定を返さず失敗する" "exit ${code}"
    code=0
    ( cmd_writeback "   " "${DEPLOYMENT_NOT_FOUND}" > /dev/null 2>&1 ) || code=$?
    check "$([ "${code}" != "0" ] && echo 0 || echo 1)" \
        "空白だけの ID も失敗にする (消えた確認があっても空で上書きしない)" "exit ${code}"

    echo "[後始末の後の引き継ぎの扱い]"
    check "$([ "$(cmd_writeback "8f0c5a12" "${DEPLOYMENT_NOT_FOUND}")" = "${DECISION_CLEAR}" ] && echo 0 || echo 1)" \
        "消えた deployment の ID は引き継がない (空で上書きする)" "$(cmd_writeback "8f0c5a12" "${DEPLOYMENT_NOT_FOUND}")"
    local state
    for state in PUBLISHING PUBLISHED VALIDATED FAILED PENDING VALIDATING ""; do
        check "$([ "$(cmd_writeback "8f0c5a12" "${state}")" = "${DECISION_STORE}" ] && echo 0 || echo 1)" \
            "${state:-状態不明} の deployment は ID を引き継ぐ (空で上書きしない)" "$(cmd_writeback "8f0c5a12" "${state}")"
    done

    if [ "${failures}" -eq 0 ]; then
        echo "失敗なし"
        return 0
    fi
    echo "失敗 ${failures} 件" >&2
    return 1
}

# --- 入口 ----------------------------------------------------------------------------

main() {
    if [ $# -eq 0 ]; then
        usage
        exit 2
    fi

    local subcommand="$1"
    shift

    case "${subcommand}" in
        --selftest)
            selftest
            ;;
        inspect)
            if [ $# -ne 1 ]; then
                usage
                exit 2
            fi
            cmd_inspect "$1"
            ;;
        writeback)
            if [ $# -ne 2 ]; then
                usage
                exit 2
            fi
            cmd_writeback "$1" "$2"
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "::error::不明なサブコマンド: ${subcommand}" >&2
            usage
            exit 2
            ;;
    esac
}

main "$@"
