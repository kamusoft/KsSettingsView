# 証跡: Pixel 6a 実機の dumpsys gfxinfo 計測

計測日: 2026-08-28 / 端末: Pixel 6a 実機 (Android 16) / 対象画面: MAUI サンプル「CustomCell デモ」(および比較用の Native Android サンプル同画面)

## 計測手順

各構成とも同一手順:

1. アプリを force-stop → 起動 → メニューから CustomCell デモへ遷移
2. `adb shell dumpsys gfxinfo <pkg> reset` で統計をリセット
3. 高速フリング往復: `adb shell input swipe 540 1900 540 500 80` を 8 回 (上方向)、逆方向を 8 回 (下方向)。各フリング間 0.7 秒待機
4. `adb shell dumpsys gfxinfo <pkg>` の集計 (Total frames 〜 percentiles) を採取

使用スクリプト (measure_scroll.sh):

```sh
adb -s "$SERIAL" shell dumpsys gfxinfo "$PKG" reset > /dev/null
sleep 1
for i in $(seq 1 8); do adb -s "$SERIAL" shell input swipe 540 1900 540 500 80; sleep 0.7; done
for i in $(seq 1 8); do adb -s "$SERIAL" shell input swipe 540 500 540 1900 80; sleep 0.7; done
sleep 1
adb -s "$SERIAL" shell dumpsys gfxinfo "$PKG" | sed -n '/Total frames rendered/,/HISTOGRAM/p'
```

計測回数: Debug (既定) / Release / native 基準は各 1 回。Debug + `UseInterpreter=false` のみ 2 回 (1 回目の直後にアプリ再起動なしで連続計測。2 回目の悪化には連続計測による端末発熱の影響が混ざっている可能性がある)。

ビルド: いずれも worktree HEAD (0a5a1f3) 時点のソース。Debug/Release とも csproj は無設定 (`UseInterpreter=false` の回のみ csproj に Debug 限定で一時追加、後に取り消し済み)。native サンプルは端末に既存インストールのビルド。

## 1. Native Android サンプル (基準) — jp.kamusoft.kssettingsview.samples.android

```
Total frames rendered: 361
Janky frames: 22 (6.09%)
Janky frames (legacy): 49 (13.57%)
50th percentile: 6ms
90th percentile: 28ms
95th percentile: 44ms
99th percentile: 81ms
Number Missed Vsync: 5
Number Slow UI thread: 22
Number Frame deadline missed: 22
```

## 2. MAUI Debug 既定 (インタープリタ有効) — jp.kamusoft.kssettingsview.samples.maui

```
Total frames rendered: 142
Janky frames: 45 (31.69%)
Janky frames (legacy): 68 (47.89%)
50th percentile: 15ms
90th percentile: 121ms
95th percentile: 150ms
99th percentile: 150ms
Number Missed Vsync: 27
Number Slow UI thread: 45
Number Frame deadline missed: 45
```

## 3. MAUI Release (csproj 無設定) — jp.kamusoft.kssettingsview.samples.maui

```
Total frames rendered: 458
Janky frames: 21 (4.59%)
Janky frames (legacy): 39 (8.52%)
50th percentile: 5ms
90th percentile: 12ms
95th percentile: 34ms
99th percentile: 85ms
Number Missed Vsync: 13
Number Slow UI thread: 21
Number Frame deadline missed: 21
```

## 4. MAUI Debug + UseInterpreter=false (1 回目)

```
Total frames rendered: 272
Janky frames: 24 (8.82%)
Janky frames (legacy): 35 (12.87%)
50th percentile: 6ms
90th percentile: 53ms
95th percentile: 85ms
99th percentile: 200ms
Number Missed Vsync: 14
Number Slow UI thread: 24
```

## 5. MAUI Debug + UseInterpreter=false (2 回目・連続計測)

```
Total frames rendered: 216
Janky frames: 42 (19.44%)
Janky frames (legacy): 64 (29.63%)
50th percentile: 12ms
90th percentile: 65ms
95th percentile: 85ms
99th percentile: 117ms
Number Missed Vsync: 22
Number Slow UI thread: 42
```

## 体感確認

- Release: ユーザーが実機で操作し「全然なめらか」と解消を確認
- Debug + UseInterpreter=false: ユーザーが実機で操作し「カクカクはする。若干マシな気がする」
