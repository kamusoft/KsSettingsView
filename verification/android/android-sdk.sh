# Android 消費者検証が使う Android SDK ロケーションの解決。
#
# verification/android/ は本体 (android/) と Sample (samples/android/) に続く 3 つめの
# Gradle build root で、Android Gradle Plugin は build root ごとに local.properties を
# 独立して解決する。開発環境の手順は SDK の指定手段として ANDROID_HOME の export と
# 各 build root の local.properties への sdk.dir 記載の 2 経路を対等に案内しているため、
# 後者だけを設定している環境でも消費者検証が動くようにする。
#
# 未追跡ファイルを増やさないため verification/android/local.properties は生成せず、
# 本体 build root の local.properties から読んだ値を ANDROID_HOME として export する。
#
# source する側は REPO_ROOT と verification-args.sh (ksv_fail) を先に用意しておく。

ksv_ensure_android_home() {
    if [ -n "${ANDROID_HOME:-}" ]; then
        return 0
    fi

    if [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        export ANDROID_HOME="${ANDROID_SDK_ROOT}"
        return 0
    fi

    local properties="${REPO_ROOT}/android/local.properties"
    local sdk_dir=""
    if [ -f "${properties}" ]; then
        # 同じキーが複数行あれば後勝ち (Java の properties と同じ扱い)。
        sdk_dir="$(
            sed -n 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*\(.*\)$/\1/p' "${properties}" \
                | tail -n 1
        )"
    fi

    if [ -z "${sdk_dir}" ]; then
        ksv_fail "Android SDK の場所が分かりません。ANDROID_HOME か ANDROID_SDK_ROOT を設定するか、${properties} に sdk.dir を書いてください"
    fi

    if [ ! -d "${sdk_dir}" ]; then
        ksv_fail "${properties} の sdk.dir が指すディレクトリがありません: ${sdk_dir}"
    fi

    export ANDROID_HOME="${sdk_dir}"
    echo "ANDROID_HOME を ${properties} の sdk.dir から解決しました"
}
