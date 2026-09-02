# 消費者検証スクリプトが共有する引数解釈。
#
# 各 platform の prepare-feed.sh / build-consumer.sh から source して使う。
# source する側は、あらかじめ次の 2 つを定義しておく。
#
#   KSV_PLATFORM        platform 名 (ios / android / maui)。使い方の表示に使う
#   KSV_DEFAULT_VERSION dry-run で version 指定が無いときの既定値。version を持たない
#                       platform (iOS) は空文字にする
#
# ksv_parse_args "$@" を呼ぶと、次の変数が設定される。
#
#   KSV_MODE       dry-run | smoke
#   KSV_VERSION    解決後の version (iOS の dry-run では空)
#   KSV_REFERENCE  --reference で与えられた準備済み参照先 (未指定なら空。dry-run 専用)
#   KSV_WORK       作業ディレクトリ (未指定なら platform ごとの既定)
#
# 許可値以外の mode、smoke で version が無い場合、smoke に --reference を与えた場合は、
# フィード準備や依存解決へ進む前にこの関数が異常終了する。

ksv_fail() {
    echo "エラー: $1" >&2
    exit 1
}

# 解決結果の証跡を標準出力へ出し、CI では job summary にも残す。
# 本文は標準入力から受け取る。
#
#   printf '%s\n' "$resolved" | ksv_evidence "解決した依存"
ksv_evidence() {
    local title="$1"
    local body
    body="$(cat)"

    echo "==== ${title} ===="
    printf '%s\n' "${body}"

    if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
        {
            echo "### ${KSV_PLATFORM}: ${title}"
            echo
            echo '```'
            printf '%s\n' "${body}"
            echo '```'
            echo
        } >> "${GITHUB_STEP_SUMMARY}"
    fi
}

ksv_usage() {
    cat >&2 <<EOF
使い方: $(basename "$0") [オプション]

  --mode <dry-run|smoke>  参照先の選択 (既定: dry-run)
  --version <version>     解決する version (smoke では必須)
  --reference <dir>       準備済みの参照先。与えるとフィード準備を行わない (dry-run 専用)
  --work <dir>            作業ディレクトリ (Android では効かない: 作業場所は Gradle の build と mavenLocal)
EOF
}

ksv_parse_args() {
    KSV_MODE="dry-run"
    KSV_VERSION=""
    KSV_REFERENCE=""
    KSV_WORK=""

    while [ $# -gt 0 ]; do
        case "$1" in
            --mode)
                [ $# -ge 2 ] || ksv_fail "--mode に値がありません"
                KSV_MODE="$2"
                shift 2
                ;;
            --version)
                [ $# -ge 2 ] || ksv_fail "--version に値がありません"
                KSV_VERSION="$2"
                shift 2
                ;;
            --reference)
                [ $# -ge 2 ] || ksv_fail "--reference に値がありません"
                KSV_REFERENCE="$2"
                shift 2
                ;;
            --work)
                [ $# -ge 2 ] || ksv_fail "--work に値がありません"
                KSV_WORK="$2"
                shift 2
                ;;
            -h|--help)
                ksv_usage
                exit 0
                ;;
            *)
                ksv_usage
                ksv_fail "不明な引数: $1"
                ;;
        esac
    done

    case "${KSV_MODE}" in
        dry-run|smoke) ;;
        *)
            ksv_fail "mode は dry-run か smoke のいずれかです: ${KSV_MODE}"
            ;;
    esac

    if [ "${KSV_MODE}" = "smoke" ] && [ -z "${KSV_VERSION}" ]; then
        ksv_fail "smoke では --version が必須です"
    fi

    # smoke の参照先は公開レジストリで、準備済みの参照先を差し込む余地がない。
    # 黙って無視すると「渡した配布物を検証した」と誤解されるため、組み合わせ自体を拒む。
    if [ "${KSV_MODE}" = "smoke" ] && [ -n "${KSV_REFERENCE}" ]; then
        ksv_fail "smoke では --reference を指定できません (参照先は公開レジストリで、準備済みの配布物は使いません)"
    fi

    if [ -z "${KSV_VERSION}" ]; then
        KSV_VERSION="${KSV_DEFAULT_VERSION}"
    fi

    echo "platform=${KSV_PLATFORM} mode=${KSV_MODE} version=${KSV_VERSION:-(なし)}"
}
