# 実行時確認 (Simulator)

実装後のビルドで iOS Sample (`samples/ios/KsSettingsViewSample.xcodeproj`、ローカルパッケージ `ios/` を参照) を
Simulator (iPhone 17 / iOS 26.5) で起動し、本 change が書き換えた 2 経路 — 行タップ通知とタッチフィードバック — を
実操作で確認した。

ユニットテストが観測するのは内部値であり、利用者が見るのは画面であるため (kasane/lessons/process.md L-003)。

## 手順と結果

| # | 操作 | 通る経路 | 結果 | 証跡 |
|---|---|---|---|---|
| 1 | root 画面の CommandCell 行「入力 Cell 5 種デモ」をタップ | `TapNotifyingRenderer` 経由の行タップ通知 (CommandCellView) | 画面遷移が発生し入力 Cell 一覧が表示された | [01-input-cells-list.png](01-input-cells-list.png) |
| 2 | PickerCell 行「テーマ / ライト」をタップ | `TapNotifyingRenderer` 経由の行タップ通知 (PickerCellView) | 選択シート「テーマを選択」が開いた | [02-picker-sheet.png](02-picker-sheet.png) |
| 3 | シートで「ダーク」を選択 | 選択結果のコールバックと Cell 内容更新 | 画面上部が「最後のイベント: テーマ → ダーク」に変わり、行の値表示が「ダーク」に更新された | [03-picker-selected.png](03-picker-selected.png) |
| 4 | 戻るボタンで root へ pop | Controller を含む画面の pop・破棄経路 | クラッシュせず root 画面へ戻り、一覧の描画も保たれている | [04-root-after-pop.png](04-root-after-pop.png) |
| 5 | 基本 Cell デモの CommandCell 行「プロフィール」を押下したまま保持し、保持中にキャプチャ | タッチフィードバック (`MainActor.assumeIsolated` を通す押下ハイライト) | 押下行だけが Theme の押下色 (AccentColor #FFBF00 の半透明) で塗られ、上下の行は塗られない | [05-press-highlight.png](05-press-highlight.png) |
| 6 | 指を離した後にキャプチャ | 同上 (解除側) | 押下行が平常時の Cell 背景へ戻った | [06-press-released.png](06-press-released.png) |

手順 5 / 6 の塗りは画素で確認した (押下行は行幅にわたり一様に `RGB(246, 224, 158)`、上下の行と解除後は `RGB(255, 255, 255)`)。
スクリーンショットは Simulator の色プロファイル変換を経るため、実際の色値の一致検証はユニットテスト側が担う。

手順 5 の撮影方法: 押下を保持する touch path (同一座標で 1500ms × 2 点) を送りながら、別プロセスから
`xcrun simctl io <device> screenshot` を時間差 (0.8 / 1.5 / 2.2 秒) で撮り、保持中のフレームを採った。

## この確認の範囲外

- `KsSettingsViewController` インスタンスが実際に解放されたことの証明: 手順 4 が示すのは pop・破棄経路が
  例外なく通ることまでである。解放そのものは `MemoryLeakTests` の weak 参照が nil になるアサーションが担保する
- 修正前ビルドとの A/B: 本 change は挙動不変が契約のリファクタで、再現すべき症状が存在しないため撮っていない
  (Swift 6 ビルドの warning については修正前コードとの比較を [swift6-build.txt](swift6-build.txt) に記録した)
