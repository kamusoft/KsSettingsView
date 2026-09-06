# 基準画像 (before) — Android Native Sample

本 change の実装着手前 (Android 本体・bridge のコードを一切変えていない状態) に撮った描画の記録。tasks 8.2 の照合で `after-*.png` と突き合わせる正になる。

- 撮影日: 2026-09-06
- 実行面: Android Native Sample (`samples/android/`) を Emulator (API 35 / Android 15、1080x2340 / density 440) で実行
- 外観の切替: Sample ルートメニューの「外観」項目群 (ライト / ダーク)。Sample は改変していない
- 撮影位置: 各画面を開いた直後の初期スクロール位置。`-scrolled-` が付くものは同じ画面を下方向へ 2 回スワイプした位置

| ファイル | 画面 | 外観 | 写っている主な対象 |
|---|---|---|---|
| before-basic-cells-light.png | 基本 Cell 7 種デモ (上部) | ライト | CommandCell / LabelCell / SwitchCell accent / CheckboxCell accent |
| before-basic-cells-dark.png | 基本 Cell 7 種デモ (上部) | ダーク | 同上 |
| before-basic-cells-scrolled-light.png | 基本 Cell 7 種デモ (下部) | ライト | RadioCell accent / SimpleCheckCell accent / ButtonCell の title 色 |
| before-basic-cells-scrolled-dark.png | 基本 Cell 7 種デモ (下部) | ダーク | 同上 |
| before-input-cells-light.png | 入力 Cell 5 種デモ (上部) | ライト | EntryCell 群、Cell 個別指定の placeholder 色 (「表示名」の行) |
| before-input-cells-dark.png | 入力 Cell 5 種デモ (上部) | ダーク | 同上 |
| before-input-cells-scrolled-light.png | 入力 Cell 5 種デモ (下部) | ライト | TimePickerCell / DatePickerCell (ホイール・カレンダー) / 下部配置 EntryCell |
| before-input-cells-scrolled-dark.png | 入力 Cell 5 種デモ (下部) | ダーク | 同上 |
| before-date-picker-dialog-light.png | 入力 Cell 5 種デモ の「予約日」から開くカレンダーダイアログ | ライト | 選択日の accent、「今日」/ Cancel / OK のボタン色 (androidButtonColor) |
| before-date-picker-dialog-dark.png | 同上 | ダーク | 同上 |
| before-unify-common-fields-light.png | 共通フィールド統合デモ | ライト | Theme を渡さない画面の既定色、Cell 個別の accent (Switch / Checkbox / Radio / SimpleCheck) と hintText |
| before-unify-common-fields-dark.png | 共通フィールド統合デモ | ダーク | 同上 |

## 補足

- tasks 8.2 は「各画面ライト / ダーク各 1 枚」と書いているが、基本 Cell デモと入力 Cell デモは 1 画面に収まらない。本 change が型を変える Cell 固有色 (ButtonCell の title、RadioCell / SimpleCheckCell の accent、DatePickerCell 系) が初期位置では画面外にあるため、同じ画面の下部を写した `-scrolled-` を各外観 1 枚ずつ足してある。`after-*.png` も同じ 12 枚の組で撮ると差分照合ができる
- 「日付選択デモ」は Android Native Sample の画面一覧 (`SampleScreen.kt`) に独立した項目として存在しない。DatePickerCell (カレンダー) の選択ダイアログを指すものとして撮ってある
- 画像はすべて Emulator + Sample 同梱のデモデータ (架空の氏名・`example.com` のメール・`090-0000-0000`) で、端末名・アカウント・位置情報は写っていない
