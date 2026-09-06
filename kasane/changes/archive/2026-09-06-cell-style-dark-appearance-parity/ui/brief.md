# UI Brief: cell-style-dark-appearance-parity

## 画面と状態

新規デザインなし。本 change は Android の Cell 固有色の型変更と 3 platform の契約・テストの追加で、描画結果 (ライト / ダークとも) は本 change 前と同じであることが正。対象画面は Android Native Sample の基本 Cell デモ・共通フィールド統合デモ・入力 Cell デモ・日付選択デモ (Cell 固有色を明示している画面と、`Unspecified` の解決経路が通る画面)。

## リファレンス注釈

なし (references/ は持たない)。

## デザイントークン参照

色ロールと既定値は [スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md) の「既定色と外観の追随」。本 change で値は変えない。

## 承認モック

mock は作らない (新規デザインなし)。見た目の正は**実装着手前に同じ画面・同じ外観で撮った基準画像** (`verification/` に `before-<画面>-<light|dark>.png` として置く)。

## 照合記録

- 照合日: 2026-09-06 (Android 分。Android 本体 / bridge の実装後、iOS / MAUI 分は別途)
- 実行面: Android Native Sample を Emulator (API 35 / Android 15、1080x2340 / density 440) で実行。基準画像の撮影環境と同一。Sample の改変なし
- 手順: ルートメニューの「外観」でライト / ダークを選び、基準画像と同じ 6 画面 × 2 外観を、同じ初期スクロール位置 (`-scrolled-` は同じ最下端位置) で撮影して `verification/after-*.png` に保存した
- 結果: **12 枚すべてが基準画像と一致**。判定はステータスバー帯 (上端 100px、時計だけが変わる) を除いた全画素の比較で、差分の bounding box が 12 枚とも空 (差分画素 0)。Cell 固有色の型変更による透明黒の混入はない
- 目視確認: 明示色 (SwitchCell / RadioCell / SimpleCheckCell の accent、ButtonCell の title、EntryCell の placeholder) はライト / ダークとも指定値のまま描かれ、未指定色は外観の既定で描かれている。余白・整列・重なり・入力欄の placeholder・システムバー境界にも異常なし
- 合意済み妥協: なし

| 最終画像 | 対応する基準画像 | 判定 |
|---|---|---|
| after-basic-cells-light.png | before-basic-cells-light.png | 一致 |
| after-basic-cells-dark.png | before-basic-cells-dark.png | 一致 |
| after-basic-cells-scrolled-light.png | before-basic-cells-scrolled-light.png | 一致 |
| after-basic-cells-scrolled-dark.png | before-basic-cells-scrolled-dark.png | 一致 |
| after-input-cells-light.png | before-input-cells-light.png | 一致 |
| after-input-cells-dark.png | before-input-cells-dark.png | 一致 |
| after-input-cells-scrolled-light.png | before-input-cells-scrolled-light.png | 一致 |
| after-input-cells-scrolled-dark.png | before-input-cells-scrolled-dark.png | 一致 |
| after-date-picker-dialog-light.png | before-date-picker-dialog-light.png | 一致 |
| after-date-picker-dialog-dark.png | before-date-picker-dialog-dark.png | 一致 |
| after-unify-common-fields-light.png | before-unify-common-fields-light.png | 一致 |
| after-unify-common-fields-dark.png | before-unify-common-fields-dark.png | 一致 |
