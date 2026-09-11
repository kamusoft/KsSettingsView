#!/bin/bash
# 公開レジストリへの反映待ち。
#
# 使い方:
#   scripts/release/wait-for-registries.sh <version>
#   scripts/release/wait-for-registries.sh --selftest
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
# 照会の結果は対象ごとに 3 つへ分類する。判定不能はさらに失敗の種別まで残す。
#
#   未照会      まだ 1 度も照会していない (上限に達して巡回を打ち切ったときに残りうる)
#   反映済み    当該 version が配信元から取得できる。以後その対象は再照会しない
#   未反映      照会できたうえで、当該 version がまだ無い
#   判定不能    照会そのものが行えなかった。種別は次の 3 つ
#                 通信そのものの失敗       要求が相手に届かない・応答が返らない
#                 応答が成功を示さない     2xx でも 404 でもない応答 (5xx など)
#                 応答を解釈できない       応答の形が想定と違い、有無を読み取れない
#
# 判定不能でも待機は続ける。レジストリ側の一時的な不調で待ちを打ち切ると、公開そのものは
# 済んでいるのに後段が走らない。区別を残すのは、上限まで待って失敗したときに「遅い」のか
# 「壊れている」のかを出力から読み分けられるようにするため。
#
# 待機の間隔と上限は環境変数で上書きできる (テストと運用の調整用):
#   KSR_POLL_INTERVAL_SECONDS  ポーリング間隔 (既定 30)
#   KSR_POLL_TIMEOUT_SECONDS   上限 (既定 2700 = 45 分)
#
# 上限は巡回の切れ目だけでなく照会 1 件ごとにも効く。期限を過ぎた照会は送らず、その対象は
# 保持している分類のまま残す。したがって待機が上限を越えて居座るのは、最後に始まった照会
# 1 件ぶん (応答上限) までで、呼び出し側 (job) の実行時間上限より先にこちらが失敗する。
#
# ネットワークへ出るのは実行本番だけで、--selftest は HTTP 送信関数をモックへ差し替えて
# URL の組み立て・応答の分類・巡回の進み方だけを検査する。

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

# 対象の分類。
readonly STATE_REFLECTED="reflected"
readonly STATE_PENDING="pending"
# まだ 1 度も照会していない対象の初期値。未反映 (照会できたうえで当該 version が無い) とは
# 別の状態で、上限に達して巡回を打ち切ったときに「照会が行われなかった対象」を示す。
readonly STATE_UNPROBED="unprobed"
readonly STATE_UNKNOWN_TRANSPORT="unknown-transport"
readonly STATE_UNKNOWN_STATUS="unknown-status"
readonly STATE_UNKNOWN_PARSE="unknown-parse"

# 詳細を持たない分類で置くプレースホルダ。分類と詳細を 1 行で受け渡すため、空にはしない。
readonly NO_DETAIL="-"

# curl が応答を得られなかったときに置くステータスコード。実在する HTTP の値と衝突しない。
readonly TRANSPORT_FAILURE_STATUS="000"

usage() {
    cat >&2 <<EOF
使い方: $(basename "${BASH_SOURCE[0]}") <version>
        $(basename "${BASH_SOURCE[0]}") --selftest
EOF
}

fail() {
    echo "::error::$1" >&2
    exit 1
}

# --- URL の組み立て ----------------------------------------------------------------

maven_pom_url() {
    local version="$1"
    echo "${MAVEN_CENTRAL_BASE_URL}/${MAVEN_GROUP_PATH}/${MAVEN_ARTIFACT_ID}/${version}/${MAVEN_ARTIFACT_ID}-${version}.pom"
}

nuget_index_url() {
    echo "${NUGET_FLAT_CONTAINER_URL}/$1/index.json"
}

# --- HTTP ---------------------------------------------------------------------------

# 照会 1 回あたりの応答の上限 (秒)。
readonly HTTP_MAX_TIME_SECONDS=120

# 照会 1 回に許す秒数。どの照会にも同じ上限を与える。
#
# 期限までの残り時間で切り詰めたくなるが、それは採らない。残りが短いときに切り詰めると、
# 正常に応答している相手を自分の都合で打ち切ることになり、ただ未反映なだけの対象が
# 「通信そのものの失敗」へ化ける。分類がいちばん必要な瞬間 (上限まで待って失敗する瞬間)
# に嘘をつくことになるため、期限の歯止めは「期限を過ぎた照会を送らない」側で掛ける。
request_max_time() {
    printf '%s' "${HTTP_MAX_TIME_SECONDS}"
}

# 待機の期限 (SECONDS 基準) と、期限を設けているかどうか。待機の外から 1 回だけ照会する
# 使い方をしても、期限未設定を「残り時間なし」と読み違えて照会を止めないようにする。
POLL_DEADLINE=0
POLL_DEADLINE_SET=0

# 期限までの残り秒数。期限を設けていなければ、常に照会を許す値を返す。
remaining_seconds() {
    if [ "${POLL_DEADLINE_SET}" -eq 0 ]; then
        printf '1'
        return 0
    fi
    printf '%s' $(( POLL_DEADLINE - SECONDS ))
}

# 残り秒数が、新しい照会を始めてよい量かどうか。残り時間の計算とは分けて、固定入力で
# 検査できる形にしている。
can_start_probe() {
    [ "$1" -gt 0 ]
}

# HTTP 要求を 1 件送り、応答を「1 行目 = ステータスコード、2 行目以降 = 本文」の形で
# 標準出力へ出す。引数: <メソッド (HEAD|GET)> <URL>
#
# 通信そのものが失敗した場合はステータスコードに 000 を置く。curl の終了コードを捨てて
# 「未反映」に畳み込むと、レジストリが壊れていても上限まで静かに待つことになる。
#
# 自己テストはこの関数だけをモックへ差し替える。URL の組み立てと応答の分類は差し替えの
# 外にあるので、モックでも本番と同じ経路が走る。
http_get() {
    local method="$1" url="$2"
    local -a curl_args=(
        --silent --show-error --location
        --connect-timeout 30 --max-time "$(request_max_time)"
        --write-out $'\n%{http_code}'
    )
    if [ "${method}" = "HEAD" ]; then
        # HEAD は本文を持たない。--head を付けないと curl が本文を待ってしまう。
        curl_args+=(--head --output /dev/null)
    fi

    # 通信が失敗したときの curl の説明は標準エラーへ流れたままにする (--show-error)。
    # 判定不能として記録した後で原因を追えるようにするため。
    local response
    if ! response="$(curl "${curl_args[@]}" "${url}")"; then
        printf '%s\n' "${TRANSPORT_FAILURE_STATUS}"
        return 0
    fi
    # --write-out で末尾に付けたステータスコードを先頭へ移す。
    printf '%s\n%s' "${response##*$'\n'}" "${response%$'\n'*}"
}

response_status() {
    printf '%s' "${1%%$'\n'*}"
}

response_body() {
    local response="$1"
    local body="${response#*$'\n'}"
    if [ "${body}" = "${response}" ]; then
        return 0
    fi
    printf '%s' "${body}"
}

# 3 桁の数字なら 0。応答の先頭行がステータスコードとして読めるかの判定。
is_http_status() {
    case "$1" in
        [0-9][0-9][0-9]) return 0 ;;
        *) return 1 ;;
    esac
}

# --- 分類 ------------------------------------------------------------------------

# 分類と詳細を 1 行で返す ("<分類> <詳細>")。詳細を持たない分類は NO_DETAIL を置く。
classified() {
    printf '%s %s\n' "$1" "${2:-${NO_DETAIL}}"
}

# Maven Central の当該 version の pom を照会して分類する。
#
# URL に version が入るので、応答が 200 であることがそのまま「当該 version が反映済み」を
# 意味する。本文は読まないため、「照会は成功したが version が無い」は 404 として現れる。
maven_probe() {
    local version="$1"
    local response status
    response="$(http_get HEAD "$(maven_pom_url "${version}")")"
    status="$(response_status "${response}")"

    if ! is_http_status "${status}"; then
        classified "${STATE_UNKNOWN_PARSE}" "ステータスコードを読み取れない"
        return 0
    fi
    case "${status}" in
        "${TRANSPORT_FAILURE_STATUS}") classified "${STATE_UNKNOWN_TRANSPORT}" ;;
        200) classified "${STATE_REFLECTED}" ;;
        404) classified "${STATE_PENDING}" ;;
        *)   classified "${STATE_UNKNOWN_STATUS}" "HTTP ${status}" ;;
    esac
}

# flat container の index から当該 version の有無を判定する。
# exit 0 = 含む / 1 = 含まない / 2 = 応答を解釈できない。
nuget_index_contains_version() {
    KSR_WANTED_VERSION="$1" python3 -c '
import json
import os
import sys

wanted = os.environ["KSR_WANTED_VERSION"].lower()
try:
    payload = json.load(sys.stdin)
except ValueError:
    sys.exit(2)
if not isinstance(payload, dict):
    sys.exit(2)
versions = payload.get("versions")
if not isinstance(versions, list):
    sys.exit(2)
# 要素を文字列へ変換してから比較すると、null や数値を含む解釈不能な応答が
# 「当該 version を含まない」= 未反映 に化ける。型が想定と違う時点で判定不能にする。
if not all(isinstance(v, str) for v in versions):
    sys.exit(2)
sys.exit(0 if wanted in [v.lower() for v in versions] else 1)
'
}

# nuget.org の当該 Package ID の index を照会して分類する。
nuget_probe() {
    local package_id="$1" version="$2"
    local response status body contains=0
    response="$(http_get GET "$(nuget_index_url "${package_id}")")"
    status="$(response_status "${response}")"

    if ! is_http_status "${status}"; then
        classified "${STATE_UNKNOWN_PARSE}" "ステータスコードを読み取れない"
        return 0
    fi
    case "${status}" in
        "${TRANSPORT_FAILURE_STATUS}")
            classified "${STATE_UNKNOWN_TRANSPORT}"
            return 0
            ;;
        200)
            ;;
        404)
            # Package ID がまだ 1 version も出ていない間は index ごと 404 になる。
            classified "${STATE_PENDING}"
            return 0
            ;;
        *)
            classified "${STATE_UNKNOWN_STATUS}" "HTTP ${status}"
            return 0
            ;;
    esac

    body="$(response_body "${response}")"
    printf '%s' "${body}" | nuget_index_contains_version "${version}" || contains=$?
    case "${contains}" in
        0) classified "${STATE_REFLECTED}" ;;
        1) classified "${STATE_PENDING}" ;;
        *) classified "${STATE_UNKNOWN_PARSE}" "version の一覧を読み取れない" ;;
    esac
}

# 分類を人が読む文字列にする。引数: <分類> <詳細>
state_label() {
    local state="$1" detail="${2:-${NO_DETAIL}}"
    local suffix=""
    if [ "${detail}" != "${NO_DETAIL}" ]; then
        suffix=": ${detail}"
    fi
    case "${state}" in
        "${STATE_UNPROBED}")          printf '未照会' ;;
        "${STATE_REFLECTED}")         printf '反映済み' ;;
        "${STATE_PENDING}")           printf '未反映' ;;
        "${STATE_UNKNOWN_TRANSPORT}") printf '判定不能 (通信そのものの失敗%s)' "${suffix}" ;;
        "${STATE_UNKNOWN_STATUS}")    printf '判定不能 (応答が成功を示さない%s)' "${suffix}" ;;
        "${STATE_UNKNOWN_PARSE}")     printf '判定不能 (応答を解釈できない%s)' "${suffix}" ;;
        *)                            printf '不明な分類 (%s)' "${state}" ;;
    esac
}

# --- 待機の対象 --------------------------------------------------------------------
#
# 4 対象 (Maven 座標 1 件と nuget.org の Package ID 3 件) を、並びの同じ 4 本の配列で
# 持つ。分類は対象ごとに独立して保持し、いったん反映済みになった対象は再照会しない。

declare -a TARGET_KIND=()
declare -a TARGET_KEY=()
declare -a TARGET_LABEL=()
declare -a TARGET_STATE=()
declare -a TARGET_DETAIL=()

reset_targets() {
    TARGET_KIND=("maven")
    TARGET_KEY=("${MAVEN_ARTIFACT_ID}")
    TARGET_LABEL=("Maven Central ${MAVEN_GROUP_PATH//\//.}:${MAVEN_ARTIFACT_ID}")
    local package_id
    for package_id in "${NUGET_PACKAGE_IDS[@]}"; do
        TARGET_KIND+=("nuget")
        TARGET_KEY+=("${package_id}")
        TARGET_LABEL+=("nuget.org/${package_id}")
    done

    TARGET_STATE=()
    TARGET_DETAIL=()
    local index
    for index in "${!TARGET_KIND[@]}"; do
        TARGET_STATE+=("${STATE_UNPROBED}")
        TARGET_DETAIL+=("${NO_DETAIL}")
    done
}

# 対象 1 件を照会して分類を返す。引数: <添字> <version>
probe_target() {
    local index="$1" version="$2"
    if [ "${TARGET_KIND[${index}]}" = "maven" ]; then
        maven_probe "${version}"
    else
        nuget_probe "${TARGET_KEY[${index}]}" "${version}"
    fi
}

# 反映済みでない対象だけを 1 巡照会し、保持している分類を更新する。
poll_once() {
    local version="$1"
    local index result
    for index in "${!TARGET_KIND[@]}"; do
        if [ "${TARGET_STATE[${index}]}" = "${STATE_REFLECTED}" ]; then
            continue
        fi
        if ! can_start_probe "$(remaining_seconds)"; then
            # 期限を過ぎたら、残りの対象は照会せず保持している分類のまま残す。送っても
            # 打ち切ることになり、応答している相手を「通信そのものの失敗」にしてしまう。
            break
        fi
        result="$(probe_target "${index}" "${version}")"
        TARGET_STATE[${index}]="${result%% *}"
        TARGET_DETAIL[${index}]="${result#* }"
    done
}

# 対象ごとに保持している分類を 1 行ずつ出す。引数: <出力先 (stdout|stderr)>
print_states() {
    local stream="$1"
    local index line
    for index in "${!TARGET_KIND[@]}"; do
        line="  ${TARGET_LABEL[${index}]}: $(state_label "${TARGET_STATE[${index}]}" "${TARGET_DETAIL[${index}]}")"
        if [ "${stream}" = "stderr" ]; then
            printf '%s\n' "${line}" >&2
        else
            printf '%s\n' "${line}"
        fi
    done
}

# 反映済みでない対象 (未照会・未反映・判定不能のいずれか) が 1 件でもあれば 0 を返す。
has_unreflected() {
    local index
    for index in "${!TARGET_KIND[@]}"; do
        if [ "${TARGET_STATE[${index}]}" != "${STATE_REFLECTED}" ]; then
            return 0
        fi
    done
    return 1
}

# 4 対象すべてが反映済みになるまで待つ。
wait_for_registries() {
    local version="$1"
    local interval="${KSR_POLL_INTERVAL_SECONDS:-30}"
    local timeout="${KSR_POLL_TIMEOUT_SECONDS:-2700}"
    POLL_DEADLINE=$(( SECONDS + timeout ))
    POLL_DEADLINE_SET=1
    local deadline="${POLL_DEADLINE}"

    reset_targets
    echo "反映を待ちます (version ${version}、${interval} 秒間隔、上限 ${timeout} 秒)"

    local round=0
    while :; do
        round=$((round + 1))
        poll_once "${version}"

        echo "巡回 ${round} の分類:"
        print_states stdout

        if ! has_unreflected; then
            echo "${#TARGET_KIND[@]} 件すべてが取得可能になりました: ${version}"
            return 0
        fi

        # 次の巡回は sleep の後に始まる。そこが期限を過ぎるなら巡回に入らず、ここで
        # 打ち切る。期限後に始めた巡回は 1 件も照会できず、同じ分類をもう一度出すだけ。
        if [ $(( SECONDS + interval )) -ge "${deadline}" ]; then
            echo "対象ごとの最後の分類:" >&2
            print_states stderr
            fail "反映を待ちきれませんでした (上限 ${timeout} 秒、巡回 ${round} 回)"
        fi

        sleep "${interval}"
    done
}

# --- 自己テスト ----------------------------------------------------------------------
#
# http_get をモックへ差し替え、URL の組み立て・応答の分類・巡回の進み方を検査する。
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
    # 台本が尽きたことを記録する印。モックの return は AND-OR 配下のコマンド置換では
    # 呼び出し側へ伝わらないため、失敗を印として残し、検査側が明示的に見る。
    MOCK_EXHAUSTED="${work}/exhausted"
    # 応答に時間がかかる相手を模す秒数。ファイルがあればその秒数だけ待ってから応答する。
    MOCK_DELAY="${work}/delay"

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

    # 台本を仕込み直す。各行は "<ステータスコード> <本文>" で、呼び出し順に消費される。
    arrange() {
        : > "${MOCK_CALLS}"
        printf '%s\n' "$@" > "${MOCK_SCRIPT}"
        echo 0 > "${MOCK_CURSOR}"
        rm -f "${MOCK_EXHAUSTED}"
        rm -f "${MOCK_DELAY}"
    }

    # 台本を使い切っていなければ 0。巡回を回す検査はこれを見て、想定より多く照会した
    # 回帰を「NG」として出す (見ないと空応答が判定不能へ畳み込まれ、上限まで回り続ける)。
    not_exhausted() {
        if [ -e "${MOCK_EXHAUSTED}" ]; then
            echo 1
        else
            echo 0
        fi
    }

    calls() {
        cat "${MOCK_CALLS}"
    }

    # 記録から「<メソッド> <URL>」だけを取り出す (応答上限の欄を落とす)。
    call_targets() {
        sed 's/ max-time=[0-9]*$//' "${MOCK_CALLS}"
    }

    # 記録された照会がすべて応答上限いっぱいを使っていれば 0。
    all_calls_use_full_max_time() {
        if [ "$(grep -c "max-time=${HTTP_MAX_TIME_SECONDS}$" "${MOCK_CALLS}")" \
            = "$(wc -l < "${MOCK_CALLS}" | tr -d ' ')" ]; then
            echo 0
        else
            echo 1
        fi
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

    # http_get のモック。ここから先は本物の curl を呼ばない。
    http_get() {
        local method="$1" url="$2"
        # 本物と同じ関数から応答上限を取り、記録に残す。照会 1 件ごとの上限が
        # 残り時間で切り詰められる形へ戻れば、巡回の検査がそれを見て落ちる。
        echo "${method} ${url} max-time=$(request_max_time)" >> "${MOCK_CALLS}"
        if [ -f "${MOCK_DELAY}" ]; then
            sleep "$(cat "${MOCK_DELAY}")"
        fi

        local index
        index="$(cat "${MOCK_CURSOR}")"
        index=$((index + 1))
        echo "${index}" > "${MOCK_CURSOR}"

        local line
        line="$(sed -n "${index}p" "${MOCK_SCRIPT}")"
        if [ -z "${line}" ]; then
            echo "モックの台本が尽きた (${index} 件目): ${method} ${url}" >&2
            printf '%s\n' "${index} 件目: ${method} ${url}" >> "${MOCK_EXHAUSTED}"
            return 1
        fi
        printf '%s\n%s' "${line%% *}" "${line#* }"
    }

    # 分類を 1 件だけ試す補助。引数: <台本の行> <照会 (maven|nuget)>
    probe_once() {
        arrange "$1"
        if [ "$2" = "maven" ]; then
            maven_probe 1.2.3
        else
            nuget_probe kssettingsview.maui 1.2.3
        fi
    }

    echo "[URL の組み立て]"
    check "$([ "$(maven_pom_url 1.2.3-beta.4)" = "https://repo1.maven.org/maven2/jp/kamusoft/kssettingsview/1.2.3-beta.4/kssettingsview-1.2.3-beta.4.pom" ] && echo 0 || echo 1)" \
        "Maven Central の pom の URL" "$(maven_pom_url 1.2.3-beta.4)"
    check "$([ "$(nuget_index_url kssettingsview.maui)" = "https://api.nuget.org/v3-flatcontainer/kssettingsview.maui/index.json" ] && echo 0 || echo 1)" \
        "nuget.org の index の URL" "$(nuget_index_url kssettingsview.maui)"

    # Maven は URL に version が入るので、応答の本文を読まない。「照会できたが当該
    # version が無い」は 404 として現れ、本文由来の解釈失敗も起きない。応答を解釈
    # できないのは、ステータスコードそのものが読めない場合になる。
    # 期限の歯止め。残り時間の計算とは分けてあるので、固定入力で決定的に検査できる。
    echo "[期限の判定]"
    local remaining
    for remaining in 1 7 120 3600; do
        check "$(if can_start_probe "${remaining}"; then echo 0; else echo 1; fi)" \
            "残り ${remaining} 秒なら照会を始める"
    done
    for remaining in 0 -1 -600; do
        check "$(if can_start_probe "${remaining}"; then echo 1; else echo 0; fi)" \
            "残り ${remaining} 秒なら照会を始めない"
    done
    POLL_DEADLINE_SET=0
    check "$(if can_start_probe "$(remaining_seconds)"; then echo 0; else echo 1; fi)" \
        "期限を設けていなければ常に照会を始める"

    # 応答上限は期限までの残りに左右されない。残り時間で切り詰めると、正常に応答して
    # いる相手を自分で打ち切り、未反映の対象が通信の失敗に化ける。
    echo "[照会 1 回の応答上限]"
    POLL_DEADLINE_SET=1
    POLL_DEADLINE=$(( SECONDS - 600 ))
    check "$([ "$(request_max_time)" = "${HTTP_MAX_TIME_SECONDS}" ] && echo 0 || echo 1)" \
        "期限を過ぎていても応答上限は変わらない" "$(request_max_time)"
    POLL_DEADLINE=$(( SECONDS + 1 ))
    check "$([ "$(request_max_time)" = "${HTTP_MAX_TIME_SECONDS}" ] && echo 0 || echo 1)" \
        "期限が目前でも応答上限は変わらない" "$(request_max_time)"
    POLL_DEADLINE_SET=0

    echo "[Maven の分類]"
    local outcome
    outcome="$(probe_once '200 ' maven)"
    check "$([ "${outcome}" = "reflected -" ] && echo 0 || echo 1)" "200 は反映済み" "${outcome}"
    check "$([ "$(call_targets)" = "HEAD https://repo1.maven.org/maven2/jp/kamusoft/kssettingsview/1.2.3/kssettingsview-1.2.3.pom" ] && echo 0 || echo 1)" \
        "Maven の照会は HEAD 1 件" "$(calls)"
    outcome="$(probe_once '404 ' maven)"
    check "$([ "${outcome}" = "pending -" ] && echo 0 || echo 1)" "404 は未反映" "${outcome}"
    outcome="$(probe_once '503 ' maven)"
    check "$([ "${outcome}" = "unknown-status HTTP 503" ] && echo 0 || echo 1)" \
        "5xx は判定不能 (応答が成功を示さない)" "${outcome}"
    outcome="$(probe_once '000 ' maven)"
    check "$([ "${outcome}" = "unknown-transport -" ] && echo 0 || echo 1)" \
        "通信の失敗は判定不能 (通信そのものの失敗)" "${outcome}"
    outcome="$(probe_once 'curl: (6) could not resolve host' maven)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "ステータスコードが読めない応答は判定不能 (応答を解釈できない)" "${outcome}"

    echo "[nuget.org の分類]"
    outcome="$(probe_once '200 {"versions":["1.2.2","1.2.3"]}' nuget)"
    check "$([ "${outcome}" = "reflected -" ] && echo 0 || echo 1)" "200 かつ当該 version ありは反映済み" "${outcome}"
    check "$([ "$(call_targets)" = "GET https://api.nuget.org/v3-flatcontainer/kssettingsview.maui/index.json" ] && echo 0 || echo 1)" \
        "nuget.org の照会は GET 1 件" "$(calls)"
    outcome="$(probe_once '200 {"versions":["1.2.2"]}' nuget)"
    check "$([ "${outcome}" = "pending -" ] && echo 0 || echo 1)" "200 かつ当該 version なしは未反映" "${outcome}"
    arrange '200 {"versions":["1.2.3-BETA.1"]}'
    outcome="$(nuget_probe kssettingsview.maui 1.2.3-beta.1)"
    check "$([ "${outcome}" = "reflected -" ] && echo 0 || echo 1)" \
        "version の比較は大文字小文字を無視する" "${outcome}"
    outcome="$(probe_once '404 ' nuget)"
    check "$([ "${outcome}" = "pending -" ] && echo 0 || echo 1)" "404 は未反映" "${outcome}"
    outcome="$(probe_once '500 ' nuget)"
    check "$([ "${outcome}" = "unknown-status HTTP 500" ] && echo 0 || echo 1)" \
        "5xx は判定不能 (応答が成功を示さない)" "${outcome}"
    outcome="$(probe_once '000 ' nuget)"
    check "$([ "${outcome}" = "unknown-transport -" ] && echo 0 || echo 1)" \
        "通信の失敗は判定不能 (通信そのものの失敗)" "${outcome}"
    outcome="$(probe_once '200 <html>not json</html>' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "JSON でない本文は判定不能 (応答を解釈できない)" "${outcome}"
    outcome="$(probe_once '200 {"versions":"1.2.3"}' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "version の一覧が配列でない応答は判定不能 (応答を解釈できない)" "${outcome}"
    # 要素を文字列へ変換してから比較すると、null や数値が「当該 version ではない」=
    # 未反映 に化ける。型が想定と違う応答は未反映へ畳み込まない。
    outcome="$(probe_once '200 {"versions":[null]}' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "version の一覧に null を含む応答は判定不能 (応答を解釈できない)" "${outcome}"
    outcome="$(probe_once '200 {"versions":["1.2.2",3]}' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "文字列と数値が混在する一覧は判定不能 (応答を解釈できない)" "${outcome}"
    outcome="$(probe_once '200 {"versions":[{"version":"1.2.3"}]}' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "version の一覧の要素が object の応答は判定不能 (応答を解釈できない)" "${outcome}"
    outcome="$(probe_once 'curl: (28) timed out' nuget)"
    check "$([ "${outcome%% *}" = "unknown-parse" ] && echo 0 || echo 1)" \
        "ステータスコードが読めない応答は判定不能 (応答を解釈できない)" "${outcome}"

    # 巡回の検査。台本は Maven → nuget 3 件の順に消費される。巡回を回す検査には必ず
    # 短い上限を与える — 既定 (45 分) のままだと、照会回数が増える回帰が「数秒で NG」
    # ではなく「呼び出し側のタイムアウト」として出る。台本切れの印もあわせて見る。
    echo "[巡回]"
    local output=""
    local code=0

    # 1 巡して上限に達する実行。間隔を上限と同じにすると、1 巡目の直後に「次の巡回は
    # 期限後になる」が必ず成立するので、時計の刻みに左右されずに失敗経路へ入る。
    arrange \
        '200 ' \
        '200 {"versions":["1.2.3"]}' \
        '200 {"versions":["1.2.2"]}' \
        '503 '
    output="$(KSR_POLL_INTERVAL_SECONDS=5 KSR_POLL_TIMEOUT_SECONDS=5 wait_for_registries 1.2.3 2>&1)" || code=$?
    check "$([ "${code}" != "0" ] && echo 0 || echo 1)" "上限に達すれば失敗する" "exit ${code}"
    check "$(not_exhausted)" "上限超過の検査で台本を使い切らない" "$(cat "${MOCK_EXHAUSTED}" 2>/dev/null)"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "4" ] && echo 0 || echo 1)" \
        "上限に達する実行でも 4 対象を 1 巡だけ照会する" "$(calls)"
    # 失敗の直前の巡回が、残り時間を理由に照会を切り詰めていないこと。切り詰めると
    # 未反映の対象が「通信そのものの失敗」に化け、失敗出力がそのまま嘘になる。
    check "$(all_calls_use_full_max_time)" \
        "上限に達する実行でも照会は応答上限いっぱいを使う" "$(calls)"
    check "$(contains "${output}" "Maven Central jp.kamusoft:kssettingsview: 反映済み")" \
        "混在する巡回で Maven の分類が残る" "${output}"
    check "$(contains "${output}" "nuget.org/kssettingsview.maui: 反映済み")" \
        "混在する巡回で反映済みの Package ID が残る" "${output}"
    check "$(contains "${output}" "nuget.org/kssettingsview.binding.ios: 未反映")" \
        "混在する巡回で未反映の Package ID が残る" "${output}"
    check "$(contains "${output}" "nuget.org/kssettingsview.binding.android: 判定不能 (応答が成功を示さない: HTTP 503)")" \
        "混在する巡回で判定不能が種別つきで残る" "${output}"
    check "$(contains "${output}" "対象ごとの最後の分類")" \
        "上限超過の出力に対象ごとの最後の分類が付く" "${output}"
    # 期限後に始まる巡回は 1 件も照会できず、同じ分類をもう一度出すだけになる。
    check "$([ "$(contains "${output}" "巡回 2 の分類")" = "1" ] && echo 0 || echo 1)" \
        "期限後に巡回を始めない (空回りの巡回を出さない)" "${output}"

    # 1 巡目で反映済みになった対象は 2 巡目で照会しない。台本は 2 巡目に Maven の
    # 応答を置いていないので、再照会すれば nuget 用の応答を食い違って消費する。
    arrange \
        '200 ' \
        '200 {"versions":["1.2.2"]}' \
        '200 {"versions":["1.2.2"]}' \
        '200 {"versions":["1.2.2"]}' \
        '200 {"versions":["1.2.3"]}' \
        '200 {"versions":["1.2.3"]}' \
        '200 {"versions":["1.2.3"]}'
    code=0
    output="$(KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=5 wait_for_registries 1.2.3 2>&1)" || code=$?
    check "$([ "${code}" = "0" ] && echo 0 || echo 1)" "全件が反映済みになれば成功する" "exit ${code}"
    check "$(not_exhausted)" "想定より多く照会していない" "$(cat "${MOCK_EXHAUSTED}" 2>/dev/null)"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "7" ] && echo 0 || echo 1)" \
        "反映済みの対象は再照会しない" "$(calls)"

    # 判定不能からの回復。1 巡目で通信に失敗した対象が、2 巡目の照会で反映済みになる。
    arrange \
        '000 ' \
        '200 {"versions":["1.2.3"]}' \
        '200 {"versions":["1.2.3"]}' \
        '200 {"versions":["1.2.3"]}' \
        '200 '
    code=0
    output="$(KSR_POLL_INTERVAL_SECONDS=0 KSR_POLL_TIMEOUT_SECONDS=5 wait_for_registries 1.2.3 2>&1)" || code=$?
    check "$([ "${code}" = "0" ] && echo 0 || echo 1)" "判定不能から回復すれば成功する" "exit ${code}"
    check "$(not_exhausted)" "回復の検査で台本を使い切らない" "$(cat "${MOCK_EXHAUSTED}" 2>/dev/null)"
    check "$(contains "${output}" "判定不能 (通信そのものの失敗)")" \
        "回復する前の巡回では判定不能として出る" "${output}"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "5" ] && echo 0 || echo 1)" \
        "回復後は再照会しない" "$(calls)"

    # 期限を過ぎた巡回は 1 件も照会せず、保持している分類をそのまま残す。ここが崩れると、
    # 上限直後の巡回が正常な相手を打ち切って「通信そのものの失敗」に塗り替える。
    echo "[期限を過ぎた後の巡回]"
    arrange '200 ' '200 {"versions":["1.2.3"]}' '200 {"versions":["1.2.3"]}' '200 {"versions":["1.2.3"]}'
    reset_targets
    TARGET_STATE[1]="${STATE_UNKNOWN_STATUS}"
    TARGET_DETAIL[1]="HTTP 503"
    POLL_DEADLINE_SET=1
    POLL_DEADLINE=$(( SECONDS - 1 ))
    poll_once 1.2.3
    check "$([ "$(calls | wc -l | tr -d ' ')" = "0" ] && echo 0 || echo 1)" \
        "期限を過ぎていれば 1 件も照会しない" "$(calls)"
    check "$([ "${TARGET_STATE[0]}" = "${STATE_UNPROBED}" ] && echo 0 || echo 1)" \
        "1 度も照会していない対象は未照会のまま残る" "${TARGET_STATE[0]}"
    check "$([ "${TARGET_STATE[1]}" = "${STATE_UNKNOWN_STATUS}" ] && [ "${TARGET_DETAIL[1]}" = "HTTP 503" ] && echo 0 || echo 1)" \
        "判定不能の対象は種別ごと残る" "${TARGET_STATE[1]} ${TARGET_DETAIL[1]}"

    # 期限までまだ余裕があれば、同じ入口から 4 対象すべてを照会する。
    POLL_DEADLINE=$(( SECONDS + 3600 ))
    poll_once 1.2.3
    check "$([ "$(calls | wc -l | tr -d ' ')" = "4" ] && echo 0 || echo 1)" \
        "期限内なら 4 対象すべてを照会する" "$(calls)"
    check "$(not_exhausted)" "期限の検査で台本を使い切らない" "$(cat "${MOCK_EXHAUSTED}" 2>/dev/null)"

    # 1 巡目の途中で期限に達する場合。照会が済んだ対象はその結果を、まだ照会していない
    # 対象は未照会を保つ。未照会を未反映の初期値で埋めると、照会していない対象まで
    # 「照会できたが当該 version が無い」と読める出力になる。
    echo "[巡回の途中で期限に達する]"
    arrange '200 ' '200 {"versions":["1.2.3"]}' '200 {"versions":["1.2.3"]}' '200 {"versions":["1.2.3"]}'
    echo 1 > "${MOCK_DELAY}"
    reset_targets
    POLL_DEADLINE=$(( SECONDS + 1 ))
    poll_once 1.2.3
    rm -f "${MOCK_DELAY}"
    check "$([ "$(calls | wc -l | tr -d ' ')" = "1" ] && echo 0 || echo 1)" \
        "期限に達した時点で残りの対象を照会しない" "$(calls)"
    check "$([ "${TARGET_STATE[0]}" = "${STATE_REFLECTED}" ] && echo 0 || echo 1)" \
        "照会が済んだ対象はその結果の分類になる" "${TARGET_STATE[0]}"
    local unprobed_index
    for unprobed_index in 1 2 3; do
        check "$([ "${TARGET_STATE[${unprobed_index}]}" = "${STATE_UNPROBED}" ] && echo 0 || echo 1)" \
            "照会していない対象 ${unprobed_index} は未照会のまま" "${TARGET_STATE[${unprobed_index}]}"
    done
    output="$(print_states stdout)"
    check "$(contains "${output}" "nuget.org/kssettingsview.maui: 未照会")" \
        "未照会は出力でも未反映と区別できる" "${output}"
    check "$([ "$(contains "${output}" "未反映")" = "1" ] && echo 0 || echo 1)" \
        "照会していない対象を未反映として出さない" "${output}"
    check "$(if has_unreflected; then echo 0; else echo 1; fi)" \
        "未照会が残っていれば待機は続く"

    POLL_DEADLINE_SET=0

    if [ "${failures}" -eq 0 ]; then
        echo "失敗なし"
        return 0
    fi
    echo "失敗 ${failures} 件" >&2
    return 1
}

# --- 入口 ----------------------------------------------------------------------------

main() {
    if [ $# -ne 1 ]; then
        usage
        exit 2
    fi
    case "$1" in
        --selftest)
            selftest
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        -*)
            echo "::error::不明な引数: $1" >&2
            usage
            exit 2
            ;;
        *)
            wait_for_registries "$1"
            ;;
    esac
}

main "$@"
