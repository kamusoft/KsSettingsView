# Proposal: ios-picker-selection-parity

## Why

iOS の PickerCell 選択面 (`PickerListViewController`、ページシート) は accent 色 (チェックマーク tint) しか Cell/Theme スタイルを継承しておらず、行の文字色・フォント・背景・separator・タップハイライト・ナビバーがシステム既定のまま。また選択中の項目への初期スクロールもない。AiForms 原典 (PickerPage のスタイル継承 / PickerTableViewController の初期スクロール) と、Android 実装 (change: android-picker-selection-sheet で確立したセルスタイル継承 + 初期スクロール) の双方から遅れている。両対応は同一ファイルの変更のため1 change に統合する。

## What Changes

- `PickerCellView.presentPickerModal` が Theme / CellStyle 由来の解決値一式を `PickerListViewController` へ渡すようにする (現状は accentColor のみ)
- 選択面のスタイル継承: 行タイトルの文字色/フォント = 実効タイトル値、行と面の背景 = 実効セル背景色、separator = `Theme.separatorColor`、タップハイライト = `Theme.selectedColor`。チェック accent は現行3段解決を維持
- ナビバー: Cancel / 完了 のボタン色に accent を適用し、タイトル文字色は実効タイトル色を適用する (フォントサイズはシステム既定を維持 — iOS のナビバー慣習を優先し、Android のヘッダーサイズ ±1sp 契約は持ち込まない)
- 初期スクロール: 選択中の項目 (複数選択は選択中の最小 index) が可視領域の中央付近に来た状態で開く (AiForms 原典どおり)。有効 index の抽出はスクロール先計算のみで、選択集合は正規化しない (範囲外 index 保持の現行挙動を維持)
- 選択面のタイトルを `pageTitle ?: title` へ変更する (現行は pageTitle のみで nil ならタイトルなし。Android と揃える — オーナー決定 2026-08-02)
- 候補行のアクセシビリティ状態 (表示名・選択状態・トグル後更新) を公開する (Android の契約と parity — オーナー決定 2026-08-02)
- 影響する能力: `settings-view-ios-host` (PickerCell の iOS 選択面の提示挙動)

先例の区別: **契約の先例**は Android デルタスペックの accent 3段解決・アクセシビリティ状態と deviation の初期スクロール。**配色の視覚的先例**は Android の承認 mock / verification 最終版 (行文字・背景・separator 等の完全継承は Android では実装で確立し、契約としては本 change のデルタスペックが初出)。

## Non-Goals

- 選択面の構造変更 (ページシート + ナビバー + UITableView の形は不変。ボトムシート化はしない)
- description / SubDisplay 相当の行2行目表示 (AiForms との設計差異は別イシュー)
- PickerCell モデル・公開 API の変更 / Android 側の変更
- NumberPicker / TimePicker / DatePicker 系の選択 UI

## Impact

- 破壊的変更なし (公開 API 不変)。利用者アプリでは選択面の配色がテーマ追従になる視覚的変更
- 利用者に見える挙動変更: pageTitle 未指定の選択面に Cell の title がタイトル表示されるようになる (現行はタイトルなし)
- 影響範囲は `PickerListViewController` と `PickerCellView` の提示部のみ。iOS の `EffectiveStyle` に必要な resolver は全て既存で、新規トークン追加なし
- リスク: ナビバーへの色適用はシステム既定 (青 tint) からの変更で、利用者の見慣れた標準感が薄れる可能性 (テーマが accent 未指定なら既定 accent が適用される)

## 級: M

UI を伴う変更 (mock 承認ゲート必要)、公開 API 変更なし・単一能力・配線中心のため。

domain: ios
