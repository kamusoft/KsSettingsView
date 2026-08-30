"""pixie4 (実機 / iOS 16.6.1) 向けバースト入力の再現ループ。

Simulator 版 repro-burst-loop-ios.sh と同じ判定 (AFTER == BEFORE + INJECT かつ rect 不変 /
PASS・FAIL・SKIP / MIN_VALID) を、WebDriverAgent の REST だけで実行する使い捨てドライバ。
"""
import json
import os
import sys
import time
import urllib.request
import urllib.error

BASE = os.environ.get("WDA_URL", "http://127.0.0.1:8101")
BUNDLE = "jp.kamusoft.kssettingsview.samples.ios"
SCREEN = os.environ.get("SCREEN", "email")
MENU, PREFIX = {
    "email": ("入力 Cell 5 種デモ", "tanaka.taro"),
    "store": ("Store 方式デモ", "store.entry"),
}[SCREEN]
INJECT = os.environ.get("INJECT", "abcde")
FREQ = int(os.environ.get("WDA_FREQ", "1000"))
SETTLE = float(os.environ.get("SETTLE", "2"))
MIN_VALID = int(os.environ.get("MIN_VALID", "15"))
N = int(sys.argv[1]) if len(sys.argv) > 1 else 20


def call(method, path, data=None, timeout=60):
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(BASE + path, data=body, method=method,
                                 headers={"Content-Type": "application/json"})
    try:
        raw = urllib.request.urlopen(req, timeout=timeout).read().decode()
    except urllib.error.HTTPError as err:
        raw = err.read().decode()
    return json.loads(raw)


session = call("POST", "/session", {"capabilities": {"alwaysMatch": {
    "bundleId": BUNDLE, "shouldWaitForQuiescence": False, "shouldTerminateApp": True}}})
sid = session.get("sessionId") or (session.get("value") or {}).get("sessionId")
if not sid:
    print("session を作成できない:", json.dumps(session)[:400])
    raise SystemExit(2)
print(f"date={time.strftime('%Y-%m-%d %H:%M:%S')} device=pixie4 screen={SCREEN} "
      f"prefix={PREFIX} inject={INJECT} freq={FREQ} settle={SETTLE}")
print(f"iterations={N} min_valid={MIN_VALID}")
time.sleep(3)


def find(using, value):
    res = call("POST", f"/session/{sid}/elements", {"using": using, "value": value})
    return [e["ELEMENT"] for e in (res.get("value") or []) if isinstance(e, dict) and "ELEMENT" in e]


def rect(uid):
    return (call("GET", f"/session/{sid}/element/{uid}/rect").get("value") or {})


def value_of(uid):
    return call("GET", f"/session/{sid}/element/{uid}/attribute/value").get("value")


menu = find("predicate string", f"label == '{MENU}'")
if not menu:
    print(f"ルートメニューに「{MENU}」が見つからない")
    raise SystemExit(2)
call("POST", f"/session/{sid}/element/{menu[0]}/click")
time.sleep(2)


def target():
    """PREFIX で始まる value を持つ入力欄を 1 件だけ返す。"""
    hits = find("predicate string",
                f"type == 'XCUIElementTypeTextField' AND value BEGINSWITH '{PREFIX}'")
    return hits


hits = target()
if len(hits) != 1:
    print(f"PREFIX '{PREFIX}' に一致する入力欄が {len(hits)} 件 (1 件でないため対象を特定できない)")
    raise SystemExit(2)
field = hits[0]

# 入力欄の右端をタップしてフォーカスを確立する (キャレットを末尾へ置くため)。
r = rect(field)
call("POST", f"/session/{sid}/actions", {"actions": [{
    "type": "pointer", "id": "finger1", "parameters": {"pointerType": "touch"},
    "actions": [
        {"type": "pointerMove", "duration": 0,
         "x": int(r["x"] + r["width"] - 6), "y": int(r["y"] + r["height"] / 2)},
        {"type": "pointerDown", "button": 0},
        {"type": "pause", "duration": 60},
        {"type": "pointerUp", "button": 0},
    ]}]})
time.sleep(2)

# フォーカスが取れていないと注入が空振りし、値が伸びないだけの偽 FAIL になる。
# キーボードの出現をフォーカス確立の証拠として確認する。
if not find("predicate string", "type == 'XCUIElementTypeKeyboard'"):
    print("入力欄のフォーカス確立に失敗した (キーボードが出ていない)")
    raise SystemExit(2)

PASS = FAIL = SKIP = 0
for i in range(1, N + 1):
    before, before_rect = value_of(field), rect(field)
    if not isinstance(before, str):
        print(f"iter {i}: SKIP (value 取得に失敗)")
        SKIP += 1
        continue
    call("POST", f"/session/{sid}/wda/keys", {"value": list(INJECT), "frequency": FREQ})
    time.sleep(SETTLE)
    after, after_rect = value_of(field), rect(field)
    if not isinstance(after, str):
        print(f"iter {i}: SKIP (注入後の value 取得に失敗)")
        SKIP += 1
        continue
    if before_rect != after_rect:
        print(f"iter {i}: SKIP (rect moved {before_rect} -> {after_rect}, after=...{after[-16:]})")
        SKIP += 1
        continue
    if after == before + INJECT:
        PASS += 1
        print(f"iter {i}: OK   (...{after[-16:]})")
    else:
        FAIL += 1
        print(f"iter {i}: FAIL (before=...{before[-16:]} after=...{after[-20:]})")

VALID = PASS + FAIL
print("---")
print(f"pass={PASS} fail={FAIL} skip={SKIP} valid={VALID} / {N} (min_valid={MIN_VALID})")
if FAIL > 0:
    print(f"判定: NG (破損 {FAIL} 件)")
    status = 1
elif VALID < MIN_VALID:
    print(f"判定: 不成立 (有効試行 {VALID} < {MIN_VALID} — SKIP が多すぎて検証になっていない)")
    status = 1
else:
    print(f"判定: OK (有効試行 {VALID}・破損 0)")
    status = 0
print(f"result={status}")
call("DELETE", f"/session/{sid}")
raise SystemExit(status)
