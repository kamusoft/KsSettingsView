# UI Brief: add-entrycell-placeholder-color

## 画面と状態

対象は `EntryCell` の入力欄の placeholder 表示のみ。行レイアウト・寸法・font は変更しない。

- **未入力 (placeholder 表示)**: 解決済み placeholder 色で表示。未指定なら OS 既定 (iOS システム placeholder 色 / Android `textColorHint` — 状態別表現を含めそのまま)
- **入力済み**: placeholder は非表示。入力テキストは valueText 解決色 (placeholder 色は関与しない)
- **無効状態**: 入力テキストのみ disabled 文字色。明示指定の placeholder 色は有効・無効で変化しない
- **ライト / ダーク**: 未指定時のみ OS 既定としてモードに自動追従。指定色は指定値のまま表示
- mock は Theme 段未指定の構成 (サンプルの Entry デモと同一)。Theme 一括指定の経路は画面内の全 `EntryCell` に掛かるため mock では表さず、自動テストで検証する

## リファレンス注釈

references/ なし (既存 Cell への色プロパティ追加のため、参照デザイン素材は不要)。

## デザイントークン参照

色はすべて解決値を使い、生値を持ち込まない:

- placeholder ← `EntryCell.placeholderColor` → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → platform default
- 入力テキスト ← valueText の解決値 (kasane/concepts/core/styling/style-resolution.md)

mock 内の青・橙は**例示値** (利用者が指定した色のイメージ) であり、ライブラリの既定色ではない。

## 承認モック

mock/placeholder-color.html を採用 (approved.png、初回承認 2026-08-27 → セカンドオピニオン指摘反映 (Theme 一括指定行の削除) 後に同日再承認)。1案のみ提示 — 色は利用者指定値をそのまま適用するだけでデザイン分岐が無いため (ユーザー合意済み)。

## 照合結果

verification/ の各画像と approved.png を照合した (2026-08-27、ユーザーの最終承認済み 2026-08-27)。対応は以下:

- `ios-entry-placeholder-light.png` / `android-entry-placeholder-light.png` / `maui-android-entry-placeholder.png` / `maui-ios-entry-placeholder.png` — mock ライト面の 3 行 (未指定 = OS 既定 / Cell 個別指定 = 指定色 / 入力済み = valueText 色) に対応
- `ios-entry-placeholder-entered.png` / `android-entry-placeholder-entered.png` — placeholder 色指定行に入力した状態。入力テキストが valueText 色で描画され placeholder 色が乗らないこと
- `ios-entry-placeholder-dark.png` — mock ダーク面に対応。未指定行だけが OS 既定としてダークへ追従し、指定色は不変
- `android-entry-valuetext-default.png` / `android-entry-valuetext-theme.png` — Android 入力文字色の valueText 是正 (利用者可視の挙動変更) の証跡。前者は既定構成 (サンプルは valueText 色を指定しない) で入力済みテキストが従来どおり title 色の解決値のまま描画されること、後者は `Theme.cellValueTextColor` を明示した構成で入力済みテキストだけが指定色になり、行タイトル・placeholder 色・未入力行の placeholder は変わらないことを示す。後者は撮影用にサンプル Theme へ一時的に `cellValueTextColor` を足して撮り、撮影後に元へ戻している (サンプルの成果物に変更は残していない)

撮影はすべて Simulator / Emulator + デモデータ (架空の氏名・メール・電話番号) で行い、保存画像に個人要素が写っていないことを確認済み。

証跡範囲: 全段未指定時に入力文字色がホストテーマの既定文字色 (`android:textColorPrimary`) へ落ちる経路は、サンプルが `Theme.cellTitleColor` を明示するため画面証跡の対象外。担保は `InputCellsTest` の「EntryCell の入力文字色は全段未指定ならホストテーマの文字色になる」(ダークテーマ上で実測) を含む 4 テスト。

合意済み妥協: 0 件。

## トークン候補

サンプルが placeholder 色デモ行に渡す `#D6885A` は mock の例示値をそのまま採った。ライブラリのトークンではなくサンプル共通定義 (`SampleTheme.demoPlaceholderOrange` / `DemoPlaceholderOrange`) に置いてある。
