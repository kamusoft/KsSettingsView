#!/bin/zsh
# iOS Simulator 版: タップを挟まないバースト入力の再現・検証ループ。
#
# Android 版 repro-burst-loop.sh と同じ構造・同じ判定で、注入を mobilecli `io text`
# (フォールバックで WebDriverAgent の `/wda/keys` frequency 指定)、読み戻しを
# mobilecli `dump ui` の JSON (`value` / `rect`) に置き換えたもの。
#
# 前提: 対象 EntryCell の入力欄がフォーカス済み・キャレット末尾・English キーボード。
# 対象 EntryCell に maxLength を設定しないこと — 本スクリプトは試行間で値を初期化せず
# 5 文字ずつ伸ばし続けるため、上限があると数試行で飽和し、以降の全試行が「値が伸びない」
# = FAIL として計上される (レース由来の破損と区別できない)。before/after が同値のまま
# 変化しない FAIL が続く場合はこの飽和を疑う。
# 各イテレーション: 注入コマンドのみ (タップ・キーイベントなし)。
# 判定: 実行後の値 == 実行前の値 + INJECT、かつ入力欄の rect が不変。
#
# rect が動いた試行と dump ui に失敗した試行は SKIP とし、有効試行に数えない。
# PREFIX に一致する入力欄が 1 件でないとき (0 件・複数件) は SKIP ではなく前提失敗として
# 非ゼロで終了する (対象を取り違えたまま「合格」と誤判定させない)。
# FAIL が 1 件でもあるか、有効試行 (PASS + FAIL) が MIN_VALID に満たなければ非ゼロで終了する
# (確率的なレースのため、少数試行や全 SKIP で「合格」と誤判定させない)。
#
# 環境変数:
#   UDID        Simulator の UDID
#   BUNDLE_ID   対象アプリの bundle identifier
#   SCREEN      対象画面: email (入力 Cell 5 種デモのメール欄 / TwoWay 経路)
#                        store (Store 方式デモの EntryCell / Store 直接経路)
#   MOBILECLI   mobilecli の実行パス
#   INJECT      1 試行あたりの注入文字列 (既定 abcde)
#   INJECT_MODE mobilecli (既定) / wda
#   WDA_URL     INJECT_MODE=wda のときの WebDriverAgent ベース URL (既定 http://127.0.0.1:8100)
#   WDA_FREQ    INJECT_MODE=wda のときの typing frequency (既定 60)
#   WDA_SESSION INJECT_MODE=wda のときの session id (未指定なら起動時に新規作成する)
#   SETTLE      注入後に書き戻しの往復を待つ秒数 (既定 2)
#   MIN_VALID   合格に必要な有効試行数 (既定 15)
#   LOG         実行結果ログの保存先パス (未指定なら保存しない)
#   LABEL       証跡に残す対象の名前 (例: before-email / after-store)
#
# 使い方: zsh repro-burst-loop-ios.sh [試行回数]
UDID=${UDID:-<ios-simulator-udid>}
BUNDLE_ID=${BUNDLE_ID:-jp.kamusoft.kssettingsview.samples.ios}
SCREEN=${SCREEN:-email}
MOBILECLI=${MOBILECLI:-mobilecli}
INJECT=${INJECT:-abcde}
INJECT_MODE=${INJECT_MODE:-mobilecli}
WDA_URL=${WDA_URL:-http://127.0.0.1:8100}
WDA_FREQ=${WDA_FREQ:-60}
WDA_SESSION=${WDA_SESSION:-}
SETTLE=${SETTLE:-2}
MIN_VALID=${MIN_VALID:-15}
LABEL=${LABEL:-unspecified}
LOG=${LOG:-}
N=${1:-20}

case "$SCREEN" in
  email) MENU_LABEL="入力 Cell 5 種デモ"; PREFIX="tanaka.taro" ;;
  store) MENU_LABEL="Store 方式デモ";     PREFIX="store.entry" ;;
  *)     echo "SCREEN は email / store のいずれか (指定値: $SCREEN)"; echo "result=ERROR"; exit 2 ;;
esac

if [[ -n "$LOG" ]]; then
  mkdir -p "$(dirname "$LOG")" || exit 2
  # 以降の標準出力・標準エラーをログへも書き出す。
  exec > >(tee -a "$LOG") 2>&1
fi

fail_run() { echo "$1"; echo "result=ERROR"; exit 2 }

command -v "$MOBILECLI" >/dev/null 2>&1 || [[ -x "$MOBILECLI" ]] || fail_run "mobilecli が見つからない: $MOBILECLI"
command -v xcrun >/dev/null 2>&1 || fail_run "xcrun が見つからない"
xcrun simctl list devices booted 2>/dev/null | grep -q "$UDID" || fail_run "Simulator $UDID が booted でない"

dump_ui() { "$MOBILECLI" dump ui --device "$UDID" 2>/dev/null }

# PREFIX で始まる value を持つ入力欄を探し、「一致件数|値|rect」を返す。
read_state() {
  dump_ui | PREFIX="$PREFIX" python3 -c "
import json, os, sys
prefix = os.environ['PREFIX']
try:
    doc = json.load(sys.stdin)
except Exception:
    print('DUMP_FAILED|?|?'); sys.exit(0)
hits = []
def walk(node):
    value = node.get('value')
    if isinstance(value, str) and value.startswith(prefix):
        r = node.get('rect') or {}
        hits.append((value, '%s,%s,%s,%s' % (r.get('x'), r.get('y'), r.get('width'), r.get('height'))))
    for child in node.get('children') or []:
        walk(child)
for element in (doc.get('data') or {}).get('elements') or []:
    walk(element)
if len(hits) == 1:
    print('1|%s|%s' % hits[0])
else:
    print('%d|?|?' % len(hits))
"
}

# ラベル一致の要素をタップする (画面遷移用)。
tap_label() {
  local target="$1"
  local point
  point=$(dump_ui | TARGET="$target" python3 -c "
import json, os, sys
target = os.environ['TARGET']
try:
    doc = json.load(sys.stdin)
except Exception:
    sys.exit(1)
def walk(node):
    if node.get('label') == target or node.get('name') == target:
        r = node.get('rect') or {}
        if r:
            return (r['x'] + r['width'] / 2, r['y'] + r['height'] / 2)
    for child in node.get('children') or []:
        found = walk(child)
        if found:
            return found
    return None
for element in (doc.get('data') or {}).get('elements') or []:
    found = walk(element)
    if found:
        print('%d,%d' % (int(found[0]), int(found[1])))
        sys.exit(0)
sys.exit(1)
")
  [[ -n "$point" ]] || return 1
  "$MOBILECLI" io tap "$point" --device "$UDID" >/dev/null 2>&1
}

# 入力欄の右端をタップしてフォーカスを確立する (キャレットを末尾へ置くため)。
focus_field() {
  local state x y w h point
  state=$(read_state)
  [[ "${state%%|*}" == "1" ]] || return 1
  local rect=${state##*|}
  x=${rect%%,*}; rect=${rect#*,}
  y=${rect%%,*}; rect=${rect#*,}
  w=${rect%%,*}; rect=${rect#*,}
  h=$rect
  point="$(( x + w - 6 )),$(( y + h / 2 ))"
  "$MOBILECLI" io tap "$point" --device "$UDID" >/dev/null 2>&1
}

inject() {
  case "$INJECT_MODE" in
    mobilecli)
      "$MOBILECLI" io text "$INJECT" --device "$UDID" >/dev/null 2>&1
      ;;
    wda)
      WDA_URL="$WDA_URL" WDA_FREQ="$WDA_FREQ" WDA_SESSION="$WDA_SESSION" INJECT="$INJECT" python3 -c "
import json, os, urllib.request
base = os.environ['WDA_URL']
session = os.environ['WDA_SESSION']
payload = json.dumps({
    'value': list(os.environ['INJECT']),
    'frequency': int(os.environ['WDA_FREQ']),
}).encode()
request = urllib.request.Request(base + '/session/' + session + '/wda/keys', data=payload,
                                 headers={'Content-Type': 'application/json'})
urllib.request.urlopen(request, timeout=60).read()
" >/dev/null 2>&1
      ;;
    *)
      fail_run "INJECT_MODE は mobilecli / wda のいずれか (指定値: $INJECT_MODE)"
      ;;
  esac
}

# WDA 注入は session 付きエンドポイントのみを受け付けるため、未指定なら新規に取得する。
if [[ "$INJECT_MODE" == "wda" && -z "$WDA_SESSION" ]]; then
  WDA_SESSION=$(WDA_URL="$WDA_URL" python3 -c "
import json, os, sys, urllib.request
base = os.environ['WDA_URL']
payload = json.dumps({'capabilities': {'alwaysMatch': {'shouldWaitForQuiescence': False}}}).encode()
request = urllib.request.Request(base + '/session', data=payload,
                                 headers={'Content-Type': 'application/json'})
try:
    body = json.loads(urllib.request.urlopen(request, timeout=60).read().decode())
except Exception as error:
    sys.exit(1)
print(body.get('sessionId') or (body.get('value') or {}).get('sessionId') or '')
")
  [[ -n "$WDA_SESSION" ]] || fail_run "WebDriverAgent ($WDA_URL) の session を作成できない"
fi

RUNTIME=$(xcrun simctl list devices booted 2>/dev/null | grep "$UDID" | sed 's/ *(.*//;s/^ *//')
echo "date=$(date '+%Y-%m-%d %H:%M:%S') label=$LABEL udid=$UDID device=$RUNTIME"
echo "screen=$SCREEN prefix=$PREFIX inject=$INJECT mode=$INJECT_MODE freq=$WDA_FREQ settle=$SETTLE"
echo "iterations=$N min_valid=$MIN_VALID"

# 画面遷移: アプリを起動し直してルートメニューから対象画面へ入り、入力欄をフォーカスする。
xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1
sleep 1
xcrun simctl launch "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || fail_run "アプリを起動できない ($BUNDLE_ID)"
sleep 3
tap_label "$MENU_LABEL" || fail_run "ルートメニューに「$MENU_LABEL」が見つからない"
sleep 2

INIT=$(read_state)
INIT_COUNT=${INIT%%|*}
[[ "$INIT_COUNT" == "1" ]] || fail_run "PREFIX '$PREFIX' に一致する入力欄が $INIT_COUNT 件 (1 件でないため対象を特定できない)"

focus_field || fail_run "入力欄のフォーカス確立に失敗した"
sleep 2

PASS=0; FAIL=0; SKIP=0
for i in $(seq 1 $N); do
  B=$(read_state); BC=${B%%|*}; BREST=${B#*|}; BEFORE=${BREST%%|*}; BB=${BREST##*|}
  if [[ "$BC" != "1" ]]; then
    if [[ "$BEFORE" == "DUMP_FAILED" || "$BC" == "DUMP_FAILED" ]]; then
      echo "iter $i: SKIP (dump failed)"; SKIP=$((SKIP+1)); continue
    fi
    echo "iter $i: 前提失敗 (PREFIX 一致 $BC 件)"; echo "result=ERROR"; exit 2
  fi
  inject
  sleep "$SETTLE"
  A=$(read_state); AC=${A%%|*}; AREST=${A#*|}; AFTER=${AREST%%|*}; AB=${AREST##*|}
  if [[ "$AC" != "1" ]]; then
    if [[ "$AFTER" == "DUMP_FAILED" || "$AC" == "DUMP_FAILED" ]]; then
      echo "iter $i: SKIP (dump failed after input)"; SKIP=$((SKIP+1)); continue
    fi
    echo "iter $i: 前提失敗 (注入後の PREFIX 一致 $AC 件)"; echo "result=ERROR"; exit 2
  fi
  if [[ "$BB" != "$AB" ]]; then
    echo "iter $i: SKIP (rect moved $BB -> $AB, after=...${AFTER: -16})"; SKIP=$((SKIP+1)); continue
  fi
  if [[ "$AFTER" == "${BEFORE}${INJECT}" ]]; then
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
