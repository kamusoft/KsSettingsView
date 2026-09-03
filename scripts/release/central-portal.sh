#!/bin/bash
# Sonatype Central Portal Publisher API の操作。deployment の状態照会・release・待機・drop と、
# 座標 + version が Maven Central へ公開済みかの判定を行う。
#
# 使い方:
#   scripts/release/central-portal.sh status <deployment-id>
#   scripts/release/central-portal.sh release <deployment-id>
#   scripts/release/central-portal.sh wait-published <deployment-id>
#   scripts/release/central-portal.sh drop <deployment-id>
#   scripts/release/central-portal.sh published <version>
#   scripts/release/central-portal.sh --selftest
#
# サブコマンドの契約:
#
#   status          deployment の状態 (PENDING / VALIDATING / VALIDATED / PUBLISHING /
#                   PUBLISHED / FAILED) を標準出力へ 1 行で出す。その ID の deployment が
#                   存在しない (HTTP 404) 場合は、失敗ではなく `NOT_FOUND` を出す。
#                   削除済みの ID を持ったまま再実行しても、呼び出し側が「引き継ぐ
#                   deployment は無い」と判断して再 upload へ進めるようにするため。
#   release         VALIDATED であることを再確認してから release する。VALIDATED 以外なら
#                   何も送らずに失敗する。
#   wait-published  PUBLISHED になるまで待つ。FAILED / NOT_FOUND になったら失敗する。
#   drop            VALIDATED / FAILED のときだけ削除する。PUBLISHING / PUBLISHED は
#                   API 上削除できないため、理由を出して正常終了する (失敗経路の後始末から
#                   呼ばれるので、削除できない状態を失敗にしない)。NOT_FOUND は既に
#                   存在しないので、同じく何も送らず正常終了する。
#   published       公開済みなら exit 0、未公開なら exit 1、判定できなければ exit 2。
#                   Publisher API は「座標 + version が公開済みか」を返すエンドポイントを
#                   公開していない (https://central.sonatype.org/publish/publish-portal-api/
#                   が定義するのは upload / status / deployment の release と削除 /
#                   deployment 内ファイルの download のみ) ため、Maven Central の
#                   配信元である repo1.maven.org の pom への HEAD で判定する。
#
# 認証: Central Portal の User Token を環境変数 MAVEN_CENTRAL_USERNAME /
# MAVEN_CENTRAL_PASSWORD で受け取り、`user:token` を base64 した値を Bearer に載せる。
# published サブコマンドは公開リポジトリを見るだけなので認証を要求しない。
#
# 待機の間隔と上限は環境変数で上書きできる (テストと運用の調整用):
#   KSR_POLL_INTERVAL_SECONDS  wait-published のポーリング間隔 (既定 30)
#   KSR_POLL_TIMEOUT_SECONDS   wait-published の上限 (既定 1800)
#
# ネットワークへ出るのは実行本番だけで、--selftest は HTTP 送信関数をモックへ差し替えて
# URL の組み立て・応答の解釈・状態分岐だけを検査する。

set -euo pipefail

# Publisher API の基点と、Maven Central の配信元。
readonly PORTAL_BASE_URL="https://central.sonatype.com/api/v1/publisher"
readonly MAVEN_CENTRAL_BASE_URL="https://repo1.maven.org/maven2"

# 公開座標。artifactId とディレクトリの対応は groupId のドットをスラッシュに開いたもの。
readonly MAVEN_GROUP_PATH="jp/kamusoft"
readonly MAVEN_ARTIFACT_ID="kssettingsview"

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <サブコマンド> <引数>

  status <deployment-id>          deployment の状態を出力する
  release <deployment-id>         VALIDATED を再確認してから release する
  wait-published <deployment-id>  PUBLISHED になるまで待つ
  drop <deployment-id>            VALIDATED / FAILED のときだけ削除する
  published <version>             公開済みなら 0、未公開なら 1 で終了する
  --selftest                      ネットワークに出ずに自己テストを実行する
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# --- URL の組み立て ----------------------------------------------------------------

status_url() {
    echo "${PORTAL_BASE_URL}/status?id=$1"
}

deployment_url() {
    echo "${PORTAL_BASE_URL}/deployment/$1"
}

# Maven Central 上の pom の URL。ここに pom があることが「その version が公開済み」を意味する。
maven_pom_url() {
    local version="$1"
    echo "${MAVEN_CENTRAL_BASE_URL}/${MAVEN_GROUP_PATH}/${MAVEN_ARTIFACT_ID}/${version}/${MAVEN_ARTIFACT_ID}-${version}.pom"
}

# --- HTTP ---------------------------------------------------------------------------

# 認証情報が揃っていなければ、要求を送る前に止める。
#
# この検査を portal_authorization の中に置くと、呼び出しがコマンド置換 (サブシェル) に
# なるため exit がサブシェルしか終わらせず、壊れたヘッダのまま要求が出てしまう。
# 検査は呼び出し側の shell で行う。
require_portal_credentials() {
    if [ -z "${MAVEN_CENTRAL_USERNAME:-}" ] || [ -z "${MAVEN_CENTRAL_PASSWORD:-}" ]; then
        fail "Central Portal の認証情報が無い (MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD)"
    fi
}

# Portal API の Authorization ヘッダの値。
portal_authorization() {
    # base64 の改行はヘッダを壊すので落とす (Linux の base64 は既定で 76 桁で折り返す)。
    printf 'Bearer %s' \
        "$(printf '%s:%s' "${MAVEN_CENTRAL_USERNAME}" "${MAVEN_CENTRAL_PASSWORD}" | base64 | tr -d '\n')"
}

# HTTP 要求を 1 件送り、応答を「1 行目 = ステータスコード、2 行目以降 = 本文」の形で
# 標準出力へ出す。引数: <メソッド> <URL> <認証 (auth|noauth)>
#
# 呼び出し側は必ずコマンド置換で受けるため、ステータスコードを変数へ書き出す形にはできない
# (置換はサブシェルで走り、変数が呼び出し側へ戻らない)。応答の先頭行に載せて返す。
# 取り出しは response_status / response_body が行う。
#
# 自己テストはこの関数だけをモックへ差し替える。URL の組み立てと応答の解釈は差し替えの外に
# あるので、モックでも本番と同じ経路が走る。
http_request() {
    local method="$1" url="$2" auth="$3"
    local -a curl_args=(
        --silent --show-error --location
        --connect-timeout 30 --max-time 300
        --request "${method}"
        --write-out $'\n%{http_code}'
    )
    if [ "${method}" = "HEAD" ]; then
        # HEAD は本文を持たない。--head を付けないと curl が本文を待ってしまう。
        curl_args+=(--head --output /dev/null)
    fi
    if [ "${auth}" = "auth" ]; then
        require_portal_credentials
        curl_args+=(--header "Authorization: $(portal_authorization)")
    fi

    local response
    if ! response="$(curl "${curl_args[@]}" "${url}")"; then
        fail "HTTP 要求に失敗した: ${method} ${url}"
    fi
    # --write-out で末尾に付けたステータスコードを先頭へ移す。
    printf '%s\n%s' "${response##*$'\n'}" "${response%$'\n'*}"
}

# http_request の応答からステータスコードを取り出す。
response_status() {
    printf '%s' "${1%%$'\n'*}"
}

# http_request の応答から本文を取り出す (本文が無ければ空文字)。
response_body() {
    local response="$1"
    local body="${response#*$'\n'}"
    if [ "${body}" = "${response}" ]; then
        return 0
    fi
    printf '%s' "${body}"
}

# --- 応答の解釈 ----------------------------------------------------------------------

# status 応答の JSON から deploymentState を取り出す。
parse_deployment_state() {
    python3 -c '
import json
import sys

try:
    payload = json.load(sys.stdin)
except ValueError:
    sys.exit(1)
state = payload.get("deploymentState")
if not isinstance(state, str) or not state:
    sys.exit(1)
print(state)
'
}

# deployment が存在しないときに状態の代わりに返す値。API の状態名と衝突しない名前にする。
readonly DEPLOYMENT_NOT_FOUND="NOT_FOUND"

# deployment の状態を照会して 1 行で返す。
deployment_state() {
    local id="$1"
    local response status state
    response="$(http_request POST "$(status_url "${id}")" auth)"
    status="$(response_status "${response}")"
    # 404 は「その ID の deployment はもう無い」であって照会の失敗ではない。drop 済みの
    # ID を引き継いだ再実行がここで止まらないよう、状態として区別できる形で返す。
    if [ "${status}" = "404" ]; then
        printf '%s\n' "${DEPLOYMENT_NOT_FOUND}"
        return 0
    fi
    if [ "${status}" != "200" ]; then
        fail "deployment の状態を照会できない (HTTP ${status}): ${id}"
    fi
    if ! state="$(response_body "${response}" | parse_deployment_state)"; then
        fail "状態照会の応答から deploymentState を取り出せない: ${id}"
    fi
    printf '%s\n' "${state}"
}

# --- サブコマンド --------------------------------------------------------------------

cmd_status() {
    deployment_state "$1"
}

cmd_release() {
    local id="$1"
    local state
    state="$(deployment_state "${id}")"
    if [ "${state}" != "VALIDATED" ]; then
        fail "release できる状態ではない (VALIDATED を期待、実際は ${state}): ${id}"
    fi

    local status
    status="$(response_status "$(http_request POST "$(deployment_url "${id}")" auth)")"
    # release は本文を返さない。204 以外は受理されていない。
    if [ "${status}" != "204" ]; then
        fail "release に失敗した (HTTP ${status}): ${id}"
    fi
    echo "release を要求した: ${id}"
}

cmd_wait_published() {
    local id="$1"
    local interval="${KSR_POLL_INTERVAL_SECONDS:-30}"
    local timeout="${KSR_POLL_TIMEOUT_SECONDS:-1800}"
    local deadline=$(( SECONDS + timeout ))
    local state

    while :; do
        state="$(deployment_state "${id}")"
        case "${state}" in
            PUBLISHED)
                echo "公開された: ${id}"
                return 0
                ;;
            FAILED)
                fail "deployment が FAILED になった: ${id}"
                ;;
            "${DEPLOYMENT_NOT_FOUND}")
                fail "公開を待っている deployment が存在しない: ${id}"
                ;;
        esac
        if [ "${SECONDS}" -ge "${deadline}" ]; then
            fail "公開を待ちきれなかった (上限 ${timeout} 秒、最後の状態 ${state}): ${id}"
        fi
        echo "待機中 (${state}): ${id}"
        sleep "${interval}"
    done
}

cmd_drop() {
    local id="$1"
    local state
    state="$(deployment_state "${id}")"
    case "${state}" in
        VALIDATED|FAILED)
            ;;
        "${DEPLOYMENT_NOT_FOUND}")
            echo "削除しない (deployment が存在しない): ${id}"
            return 0
            ;;
        *)
            # PUBLISHING / PUBLISHED は API 上削除できない。PENDING / VALIDATING は
            # 検証中で削除を受け付けないので、いずれも何もせず理由だけ残す。
            echo "削除しない (${state} は削除できる状態ではない): ${id}"
            return 0
            ;;
    esac

    local status
    status="$(response_status "$(http_request DELETE "$(deployment_url "${id}")" auth)")"
    if [ "${status}" != "204" ]; then
        fail "drop に失敗した (HTTP ${status}): ${id}"
    fi
    echo "drop した (${state}): ${id}"
}

cmd_published() {
    local version="$1"
    local url status
    url="$(maven_pom_url "${version}")"
    status="$(response_status "$(http_request HEAD "${url}" noauth)")"
    case "${status}" in
        200)
            echo "公開済み: ${MAVEN_ARTIFACT_ID}:${version}"
            return 0
            ;;
        404)
            echo "未公開: ${MAVEN_ARTIFACT_ID}:${version}"
            return 1
            ;;
        *)
            echo "::error::公開の有無を判定できない (HTTP ${status}): ${url}" >&2
            return 2
            ;;
    esac
}

# --- 自己テスト ----------------------------------------------------------------------
#
# http_request をモックへ差し替え、URL の組み立て・応答の解釈・状態分岐を検査する。
# モックは呼び出しを記録ファイルへ追記し、応答を台本ファイルから 1 件ずつ取り出す。
# 記録とカーソルをファイルに置くのは、失敗を捕まえるためにサブシェルで実行する検査でも
# 呼び出し記録を親から読めるようにするため。

selftest() {
    # trap はスクリプト終了時に走るため、作業ディレクトリの変数は関数ローカルにしない。
    SELFTEST_WORK="$(mktemp -d)"
    trap 'rm -rf "${SELFTEST_WORK}"' EXIT
    local work="${SELFTEST_WORK}"

    MOCK_CALLS="${work}/calls"
    MOCK_SCRIPT="${work}/script"
    MOCK_CURSOR="${work}/cursor"

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

    # 台本を仕込み直す。各行は "<HTTP ステータス> <本文>" で、呼び出し順に消費される。
    arrange() {
        : > "${MOCK_CALLS}"
        printf '%s\n' "$@" > "${MOCK_SCRIPT}"
        echo 0 > "${MOCK_CURSOR}"
    }

    # 記録された呼び出しを "<メソッド> <URL> <認証>" の行として返す。
    calls() {
        cat "${MOCK_CALLS}"
    }

    # 第 1 引数が第 2 引数を部分文字列として含むなら 0、含まなければ 1 を出力する。
    # (case 文はコマンド置換の中に書けない — パターン末尾の `)` が置換を閉じてしまう)
    contains() {
        if [ "${1#*"$2"}" != "$1" ]; then
            echo 0
        else
            echo 1
        fi
    }

    # http_request のモック。ここから先は本物の curl を呼ばない。
    # 本物と同じく「1 行目 = ステータスコード、2 行目以降 = 本文」の形で返す。
    http_request() {
        local method="$1" url="$2" auth="$3"
        echo "${method} ${url} ${auth}" >> "${MOCK_CALLS}"

        local index
        index="$(cat "${MOCK_CURSOR}")"
        index=$((index + 1))
        echo "${index}" > "${MOCK_CURSOR}"

        local line
        line="$(sed -n "${index}p" "${MOCK_SCRIPT}")"
        if [ -z "${line}" ]; then
            echo "モックの台本が尽きた (${index} 件目): ${method} ${url}" >&2
            return 1
        fi
        printf '%s\n%s' "${line%% *}" "${line#* }"
    }

    echo "[URL の組み立て]"
    check "$([ "$(status_url abc)" = "https://central.sonatype.com/api/v1/publisher/status?id=abc" ] && echo 0 || echo 1)" \
        "status の URL" "$(status_url abc)"
    check "$([ "$(deployment_url abc)" = "https://central.sonatype.com/api/v1/publisher/deployment/abc" ] && echo 0 || echo 1)" \
        "deployment の URL" "$(deployment_url abc)"
    check "$([ "$(maven_pom_url 1.2.3-beta.4)" = "https://repo1.maven.org/maven2/jp/kamusoft/kssettingsview/1.2.3-beta.4/kssettingsview-1.2.3-beta.4.pom" ] && echo 0 || echo 1)" \
        "Maven Central の pom の URL" "$(maven_pom_url 1.2.3-beta.4)"

    echo "[状態の解釈]"
    arrange '200 {"deploymentId":"abc","deploymentState":"VALIDATED"}'
    check "$([ "$(cmd_status abc)" = "VALIDATED" ] && echo 0 || echo 1)" "status が deploymentState を返す"
    check "$([ "$(calls)" = "POST https://central.sonatype.com/api/v1/publisher/status?id=abc auth" ] && echo 0 || echo 1)" \
        "status は認証つきの POST 1 件" "$(calls)"

    arrange '200 {"deploymentId":"abc"}'
    check "$(if ( cmd_status abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "deploymentState が無い応答で失敗する"

    arrange '401 {"error":"unauthorized"}'
    check "$(if ( cmd_status abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "200 以外の状態照会で失敗する"

    arrange '404 {"error":"not found"}'
    check "$([ "$(cmd_status abc)" = "NOT_FOUND" ] && echo 0 || echo 1)" \
        "404 は NOT_FOUND を返す (失敗にしない)"

    echo "[release]"
    arrange '200 {"deploymentState":"VALIDATED"}' '204 '
    check "$(if ( cmd_release abc > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" "VALIDATED なら release する"
    check "$([ "$(calls | sed -n 2p)" = "POST https://central.sonatype.com/api/v1/publisher/deployment/abc auth" ] && echo 0 || echo 1)" \
        "release は deployment へ POST する" "$(calls | sed -n 2p)"

    arrange '200 {"deploymentState":"PENDING"}'
    check "$(if ( cmd_release abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" "VALIDATED 以外は release しない"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" \
        "release しないときは状態照会だけで止まる" "$(calls)"

    arrange '200 {"deploymentState":"VALIDATED"}' '500 '
    check "$(if ( cmd_release abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" "release が 204 以外なら失敗する"

    echo "[drop の状態分岐]"
    local state
    for state in VALIDATED FAILED; do
        arrange "200 {\"deploymentState\":\"${state}\"}" '204 '
        check "$(if ( cmd_drop abc > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" "${state} は drop する"
        check "$([ "$(calls | sed -n 2p)" = "DELETE https://central.sonatype.com/api/v1/publisher/deployment/abc auth" ] && echo 0 || echo 1)" \
            "${state} の drop は DELETE を送る" "$(calls | sed -n 2p)"
    done
    for state in PUBLISHING PUBLISHED PENDING VALIDATING; do
        arrange "200 {\"deploymentState\":\"${state}\"}"
        local output
        output="$(cmd_drop abc 2>&1)"
        check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" "${state} は DELETE を送らない" "$(calls)"
        check "$(contains "${output}" "${state}")" "${state} は理由を出力する" "${output}"
    done

    # drop の後は同じ ID の照会が 404 になる。後始末の直後にもう一度 drop を呼んでも
    # 失敗せず、DELETE も送らないことを確かめる (失敗経路が二重に走っても止まらない)。
    arrange '404 {"error":"not found"}'
    local dropped_output
    dropped_output="$(cmd_drop abc 2>&1)"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" \
        "drop 済みの ID には DELETE を送らない" "$(calls)"
    check "$(contains "${dropped_output}" "存在しない")" \
        "drop 済みの ID は存在しない旨を出力する" "${dropped_output}"

    arrange '404 {"error":"not found"}'
    check "$(if ( KSR_POLL_INTERVAL_SECONDS=0 cmd_wait_published abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "存在しない deployment の公開待ちは失敗する"

    arrange '404 {"error":"not found"}'
    check "$(if ( cmd_release abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "存在しない deployment は release しない"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" \
        "release しないときは状態照会だけで止まる (NOT_FOUND)" "$(calls)"

    echo "[wait-published]"
    arrange '200 {"deploymentState":"PUBLISHING"}' '200 {"deploymentState":"PUBLISHING"}' '200 {"deploymentState":"PUBLISHED"}'
    check "$(if ( KSR_POLL_INTERVAL_SECONDS=0 cmd_wait_published abc > /dev/null 2>&1 ); then echo 0; else echo 1; fi)" \
        "PUBLISHING を経て PUBLISHED になれば成功する"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "3" ] && echo 0 || echo 1)" "PUBLISHED まで照会を繰り返す" "$(calls)"

    arrange '200 {"deploymentState":"PUBLISHING"}' '200 {"deploymentState":"FAILED"}'
    check "$(if ( KSR_POLL_INTERVAL_SECONDS=0 cmd_wait_published abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "FAILED になれば失敗する"

    arrange '200 {"deploymentState":"PUBLISHING"}' '200 {"deploymentState":"PUBLISHING"}'
    check "$(if ( KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=0 cmd_wait_published abc > /dev/null 2>&1 ); then echo 1; else echo 0; fi)" \
        "上限を過ぎれば失敗する"

    echo "[published]"
    local published_code
    arrange '200 '
    published_code=0
    ( cmd_published 1.2.3 > /dev/null 2>&1 ) || published_code=$?
    check "$([ "${published_code}" = "0" ] && echo 0 || echo 1)" "200 なら公開済み (exit 0)" "exit ${published_code}"
    check "$([ "$(calls)" = "HEAD https://repo1.maven.org/maven2/jp/kamusoft/kssettingsview/1.2.3/kssettingsview-1.2.3.pom noauth" ] && echo 0 || echo 1)" \
        "published は認証なしの HEAD 1 件" "$(calls)"

    arrange '404 '
    published_code=0
    ( cmd_published 1.2.3 > /dev/null 2>&1 ) || published_code=$?
    check "$([ "${published_code}" = "1" ] && echo 0 || echo 1)" "404 なら未公開 (exit 1)" "exit ${published_code}"

    arrange '503 '
    published_code=0
    ( cmd_published 1.2.3 > /dev/null 2>&1 ) || published_code=$?
    check "$([ "${published_code}" = "2" ] && echo 0 || echo 1)" "判定できない応答は exit 2" "exit ${published_code}"

    echo "[認証情報]"
    check "$(if ( MAVEN_CENTRAL_USERNAME="" MAVEN_CENTRAL_PASSWORD="" require_portal_credentials 2>/dev/null ); then echo 1; else echo 0; fi)" \
        "認証情報が空なら止まる"
    check "$(if ( MAVEN_CENTRAL_USERNAME="user" MAVEN_CENTRAL_PASSWORD="" require_portal_credentials 2>/dev/null ); then echo 1; else echo 0; fi)" \
        "片方だけでも止まる"
    check "$(if ( MAVEN_CENTRAL_USERNAME="user" MAVEN_CENTRAL_PASSWORD="token" require_portal_credentials 2>/dev/null ); then echo 0; else echo 1; fi)" \
        "両方あれば通る"
    local authorization
    authorization="$(MAVEN_CENTRAL_USERNAME="user" MAVEN_CENTRAL_PASSWORD="token" portal_authorization)"
    check "$([ "${authorization}" = "Bearer dXNlcjp0b2tlbg==" ] && echo 0 || echo 1)" \
        "Bearer は user:token の base64" "${authorization}"

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
        status|release|wait-published|drop|published)
            if [ $# -ne 1 ]; then
                usage
                exit 2
            fi
            # Portal API を叩くサブコマンドは、要求を組み立てる前に認証情報を確かめる
            # (published は公開リポジトリを見るだけなので認証を要求しない)。
            if [ "${subcommand}" != "published" ]; then
                require_portal_credentials
            fi
            case "${subcommand}" in
                status)         cmd_status "$1" ;;
                release)        cmd_release "$1" ;;
                wait-published) cmd_wait_published "$1" ;;
                drop)           cmd_drop "$1" ;;
                published)      cmd_published "$1" ;;
            esac
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
