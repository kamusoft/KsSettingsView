#!/bin/zsh
# タップを挟まないバースト入力の再現・検証ループ。
#
# 前提: 対象 EditText がフォーカス済み・キャレット末尾・qwerty English。
# 対象 EditText に maxLength を設定しないこと — 本スクリプトは試行間で値を初期化せず
# 5 文字ずつ伸ばし続けるため、上限があると数試行で飽和し、以降の全試行が「値が伸びない」
# = FAIL として計上される (レース由来の破損と区別できない)。before/after が同値のまま
# 変化しない FAIL が続く場合はこの飽和を疑う。
# 各イテレーション: `input text abcde` のみ (タップ・キーイベントなし)。
# 判定: 実行後の値 == 実行前の値 + "abcde"、かつ EditText の bounds が不変。
#
# bounds が動いた試行と uiautomator dump に失敗した試行は SKIP とし、有効試行に数えない。
# FAIL が 1 件でもあるか、有効試行 (PASS + FAIL) が MIN_VALID に満たなければ非ゼロで終了する
# (確率的なレースのため、少数試行や全 SKIP で「合格」と誤判定させない)。
#
# 環境変数:
#   SERIAL     adb のデバイスシリアル
#   PREFIX     対象 EditText の値の先頭に置いた固定文字列 (dump からの行特定に使う)
#   MIN_VALID  合格に必要な有効試行数 (既定 15)
#   LOG        実行結果ログの保存先パス (未指定なら保存しない)
#   LABEL      証跡に残す対象の名前 (例: maui-sample / native-sample)
#
# 使い方: zsh repro-burst-loop.sh [試行回数]
SERIAL=${SERIAL:-<android-device-serial>}
PREFIX=${PREFIX:-Tanaka}
MIN_VALID=${MIN_VALID:-15}
LABEL=${LABEL:-unspecified}
LOG=${LOG:-}
N=${1:-20}

if [[ -n "$LOG" ]]; then
  mkdir -p "$(dirname "$LOG")" || exit 2
  # 以降の標準出力・標準エラーをログへも書き出す。
  exec > >(tee -a "$LOG") 2>&1
fi

fail_run() { echo "$1"; echo "result=ERROR"; exit 2 }

command -v adb >/dev/null 2>&1 || fail_run "adb が見つからない"
adb -s $SERIAL get-state >/dev/null 2>&1 || fail_run "デバイス $SERIAL に接続できない"

MODEL=$(adb -s $SERIAL shell getprop ro.product.model 2>/dev/null | tr -d '\r')
RELEASE=$(adb -s $SERIAL shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
echo "date=$(date '+%Y-%m-%d %H:%M:%S') label=$LABEL serial=$SERIAL model=$MODEL android=$RELEASE"
echo "prefix=$PREFIX iterations=$N min_valid=$MIN_VALID"

read_state() {
  adb -s $SERIAL exec-out uiautomator dump /dev/tty 2>/dev/null | python3 -c "
import sys, re
xml = sys.stdin.read()
m = re.search(r'<node[^>]*text=\"(${PREFIX}[^\"]*)\"[^>]*bounds=\"(\[[^\"]+?\]\[[^\"]+?\])\"', xml)
print((m.group(1) + '|' + m.group(2)) if m else 'DUMP_FAILED|?')"
}

PASS=0; FAIL=0; SKIP=0
for i in $(seq 1 $N); do
  B=$(read_state); BEFORE=${B%%|*}; BB=${B##*|}
  if [[ "$BEFORE" == "DUMP_FAILED" ]]; then echo "iter $i: SKIP (dump failed)"; SKIP=$((SKIP+1)); continue; fi
  adb -s $SERIAL shell input text abcde
  sleep 2
  A=$(read_state); AFTER=${A%%|*}; AB=${A##*|}
  if [[ "$AFTER" == "DUMP_FAILED" ]]; then echo "iter $i: SKIP (dump failed after input)"; SKIP=$((SKIP+1)); continue; fi
  if [[ "$BB" != "$AB" ]]; then
    echo "iter $i: SKIP (bounds moved $BB -> $AB, after=...${AFTER: -16})"; SKIP=$((SKIP+1)); continue
  fi
  if [[ "$AFTER" == "${BEFORE}abcde" ]]; then
    PASS=$((PASS+1)); echo "iter $i: OK   (...${AFTER: -16})"
  else
    FAIL=$((FAIL+1)); echo "iter $i: FAIL (before=...${BEFORE: -16} after=...${AFTER: -20})"
  fi
done

VALID=$((PASS+FAIL))
echo "---"
echo "pass=$PASS fail=$FAIL skip=$SKIP valid=$VALID / $N (min_valid=$MIN_VALID)"

STATUS=0
if (( FAIL > 0 )); then
  echo "判定: NG (破損 $FAIL 件)"
  STATUS=1
elif (( VALID < MIN_VALID )); then
  echo "判定: 不成立 (有効試行 $VALID < $MIN_VALID — SKIP が多すぎて検証になっていない)"
  STATUS=1
else
  echo "判定: OK (有効試行 $VALID・破損 0)"
fi
echo "result=$STATUS"
exit $STATUS
