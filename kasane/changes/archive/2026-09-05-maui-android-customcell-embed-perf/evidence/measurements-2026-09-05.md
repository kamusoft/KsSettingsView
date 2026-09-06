# 証跡: MAUI Android CustomCell の行リサイクル経路の実測 (2026-09-05)

計測日: 2026-09-05 / 端末: Pixel 4a 実機 (Android 13, 60Hz) と Pixel 6a 実機 (Android 16, 60Hz) / 対象画面: MAUI サンプル「CustomCell デモ」/ ビルド: いずれも Release (`dotnet build samples/maui/KsSettingsView.Sample.Maui -f net10.0-android -c Release`)、探索ブランチ HEAD (61c829f) のソースに一時プローブを加えたもの (計測後に全て取り消し済み)

## 一時プローブ (取り消し済み)

- `KsAccessoryHostView` (maui/KsSettingsView.Maui/Platforms/Android/): コンストラクタ (materialize)・`OnMeasure` (spec と `Stopwatch` 所要 µs)・`OnLayout` を `Log.Debug("KsPerfProbe", ...)` で記録
- `KsBridgeCellContentView.Content` (android/kssettingsview-bridge/): `AndroidView` の factory 実行を記録
- `CustomCellViewHolder` (android/kssettingsview/): `bind` / `reset` を記録
- 「重い content」構成: CustomCellDemoPage.xaml のダミー行 DataTemplate の trailing を `VerticalStackLayout` (SampleTagLabel + Label × 8) に差し替え (行あたり View 約 7 個 → 約 15 個)

## 計測手順

元 change (archive/2026-08-28-customcell-android-maui-perf/evidence/gfxinfo-pixel6a.md) と同じ。`scripts/measure_scroll.sh <serial> <pkg> <label>` — gfxinfo reset → 高速フリング 8 往復 (上 8 回 + 下 8 回、各 0.7 秒待機) → gfxinfo 採取 + logcat の KsPerfProbe を保存。atrace は `atrace --async_start -b 65536 -a <pkg> view gfx input` を張ってフリング 6 往復、`--async_stop -z` で採取し `scripts/atrace_frames.py <trace> <pid>` でメインスレッドの区間を集計。

## 1. gfxinfo (Release)

| 構成 | 端末 | Total | Janky | p50 | p90 | p95 | p99 |
|---|---|---|---|---|---|---|---|
| 現行 content (View 7 個) + プローブ | Pixel 4a | 406 | 18 (4.4%) | 9ms | 25ms | 36ms | 250ms |
| 現行 content (View 7 個) + プローブ | Pixel 6a | 456 | 19 (4.2%) | 5ms | 12ms | 29ms | 93ms |
| 重い content (View 15 個) + プローブ | Pixel 4a | 406 | 59 (14.5%) | 10ms | 73ms | 97ms | 150ms |
| 重い content (View 15 個) + プローブ | Pixel 6a | 528 | 5 (1.0%) | 6ms | 19ms | 22ms | 38ms |
| 参考: native Android サンプル同画面 (端末の既存ビルド、構成不明) | Pixel 4a | 580 | 23 (4.0%) | 18ms | 21ms | 23ms | 200ms |

Pixel 6a の元 change 実測 (2026-08-28、プローブなし Release): Janky 4.6% / p90 12ms — 今回のプローブ付きと同水準で、プローブの記録コストは結果を歪めていない。

## 2. プローブ集計 (1 セッション = フリング 8 往復)

| 構成 | 端末 | bind | factory | materialize | measure 回数 | measure/bind | measure µs: 中央値 / p90 / 最大 | measure 合計 |
|---|---|---|---|---|---|---|---|---|
| 現行 content | Pixel 4a | 77 | 76 | 0 | 118 | 1.5 | 206 / 651 / 21,580 | 56ms |
| 現行 content | Pixel 6a | 75 | 76 | 0 | 142 | 1.9 | 95 / 353 / 900 | 21ms |
| 重い content | Pixel 4a | 83 | 86 | 0 | 198 | 2.4 | 206 / 971 / 49,917 | 114ms |
| 重い content | Pixel 6a | 83 | 88 | 0 | 265 | 3.2 | 99 / 599 / 951 | 46ms |

- measure の spec は全件 `w=Exactly / h=Unspecified` (行幅固定・高さは内容任せ)
- materialize 0 = wrapper の再実体化は起きていない (世代トークンが安定しているため)。factory ≒ bind = 行のリサイクル 1 回につき輸送 View の再親付け 1 回
- measure の合計は最悪でも 406 フレームに対し 114ms (1 フレーム平均 0.3ms、予算 16.7ms の 2%)。最大値の外れ値 (21ms / 50ms) は各 1 件で、初回表示の行に出る

## 3. atrace のフレーム内訳 (重い content、Pixel 4a、フリング 6 往復、メインスレッド)

区間の合計 (上位):

| 区間 | 回数 | 合計 | 最大 |
|---|---|---|---|
| traversal | 328 | 2704ms | 84ms |
| draw → Record View#draw() | 328 | 1486ms | 72ms |
| AndroidOwner:measureAndLayout | 488 | 1149ms | 61ms |
| Compose:onPositionedCallbacks | 1023 | 997ms | 59ms |
| dispatchApplyInsets | 58 | 639ms | 15ms |
| Recomposer:recompose | 70 | 340ms | 12ms |
| RV Scroll | 124 | 335ms | 14ms |
| layout (RV OnLayout) | 77 | 328ms | 10ms |

フレームを「bind が起きたか」で分けた比較 (frame / draw 記録 / inset 再配布 / positioned callbacks の中央値、ms):

| フレーム群 | n | frame | draw | insets | positioned | recompose |
|---|---|---|---|---|---|---|
| bind あり | 40 | 36.5 | 15.4 | 8.8 | 10.3 | 3.8 |
| bind なし・直前フレームに bind あり | 21 | 33.1 | 14.2 | 9.0 | 9.7 | 5.2 |
| bind なし・inset 再配布あり | 30 | 41.1 | 17.1 | 11.8 | 12.3 | 4.3 |
| bind なし・inset 再配布なし | 275 | 0.9 | 0.3 | 0 | 0 | 0 |

- 遅いフレーム (>16.7ms) 83 本のうち 66 本が bind または inset 再配布のあるフレーム
- bind 53 回に対し inset 再配布のあるフレーム 58 本: 再親付け (`addView`) に伴う `requestApplyInsets` がウィンドウ全体への inset 再配布を毎回起こしている
- `Compose:onPositionedCallbacks` は `AndroidViewHolder` が `onGloballyPositioned` で埋め込み View の Android 側 `layout()` を呼ぶ区間で、wrapper の `OnLayout` → MAUI の Arrange がここに含まれる
- ViewRootImpl の `measure` 区間は毎フレーム 0.3ms 前後 (RecyclerView の measure キャッシュにより実質空)。MAUI 側の計測は `RV Scroll` 内の `AndroidOwner:onMeasure` (最大 1.5ms) に現れる
