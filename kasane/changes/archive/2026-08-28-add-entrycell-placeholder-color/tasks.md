# Tasks: add-entrycell-placeholder-color

## 1. iOS core

- [x] 1.1 `EntryCell.placeholderColor` (UIColor?) を追加 — init 2種 / `==` / `hash` / `withDSLID` / `withStyle` / `withIcon` の全列挙箇所へ反映 (→ Requirement: EntryCell の placeholder 文字色指定)
- [x] 1.2 `CellStyle.placeholderColor` / `Theme.cellPlaceholderColor` を追加し、`EffectiveStyle` に解決を実装。手動等価性の列挙 (`Theme.==` / `CellStyle.==` / `hashCellStyle`) へ新フィールドを追加 (→ Requirement: iOS の placeholder 色描画)
- [x] 1.3 `EntryCellView` の render で色指定時は attributed 表示 (実効 valueText font 適用)、未指定時はプレーン表示。placeholder nil は表示なし・空文字列は安全に描画。`prepareForReuse` でリセット (→ Scenario: 指定色で placeholder が表示される / font 指定と色指定が共存する / 再利用行に色が残らない / placeholder 文字列 nil + 色指定でも安全に描画される)
- [x] 1.4 Theme 再適用経路で placeholder 色が追従することを確認・必要なら実装 (→ Scenario: Theme 変更が表示中の placeholder に追従する)
- [x] 1.5 iOS テスト追加: 解決順 (Cell → CellStyle → Theme → 既定)・attributed/プレーン切替・nil/空文字列・reuse リセット・placeholder 色だけ変えた `EntryCell` / `CellStyle` / `Theme` が非同値になること (→ Requirement: EntryCell の placeholder 文字色指定 / iOS の placeholder 色描画)

## 2. Android core

- [x] 2.1 `EntryCell.placeholderColor` (Color?) を追加 — 手動 override の `equals` / `hashCode` へも列挙追加 (→ Requirement: EntryCell の placeholder 文字色指定)
- [x] 2.2 `CellStyle.placeholderColor` / `Theme.cellPlaceholderColor` を追加し、`EffectiveStyle` に解決を実装 (→ Requirement: Android の placeholder (hint) 色描画)
- [x] 2.3 `EntryCellViewHolder` の bind で hint 色を差分判定付きで適用。生成時のホスト既定 `ColorStateList` を捕捉し、未指定 (再利用・明示→未指定の遷移含む) では既定 `ColorStateList` を復元 (→ Scenario: 指定色で placeholder が表示される / 再利用行に色が残らない / 明示色から未指定へ戻すとホスト既定の状態別色へ復帰する / 明示色は無効状態でも変わらない)
- [x] 2.4 Theme 再適用経路で placeholder 色が追従することを確認・必要なら実装 (→ Scenario: Theme 変更が表示中の placeholder に追従する)
- [x] 2.5 入力文字色を valueText の解決順へ是正 (`titleColor` 直参照を置換、disabled 優先は維持) (→ Requirement: 入力文字色の valueText 解決 (規約乖離の是正))
- [x] 2.6 Compose DSL (`InputCellDsl.kt` の `EntryCell` 2 overload) に `placeholderColor` 引数を追加し、`InputCellDslTest` を更新 (→ Requirement: EntryCell の placeholder 文字色指定)
- [x] 2.7 Android テスト追加 (`InputCellsTest` 等): 解決順・hint 色適用と `ColorStateList` 復元・無効状態の明示色維持・placeholder 色だけ変えた `EntryCell` の非同値・valueText 是正 (明示指定時 / 未指定 fallback / disabled) (→ 上記全 Requirement)

## 3. Bridge

- [x] 3.1 iOS `KsBridgeEntryCell` に `placeholderColor` (NSNumber?) を追加し `makeCell` で変換。`KsBridgeTheme` に `cellPlaceholderColor` を追加 (→ Requirement: placeholder 色の輸送)
- [x] 3.2 Android `KsBridgeEntryCell` に `placeholderColor` (Int?) を追加し変換。Theme DTO に `cellPlaceholderColor` を追加 (→ Requirement: placeholder 色の輸送)
- [x] 3.3 Bridge テスト追加 (iOS Bridge Tests / Android bridge tests): Entry DTO → `makeCell` の変換・Theme resolve・null の未指定写し (→ Scenario: per-cell 値が native cell へ写る / Theme 値が native Theme へ写る / null は未指定として写る)

## 4. MAUI facade

- [x] 4.1 `EntryCell.PlaceholderColor` (BindableProperty) を追加 — CLR プロパティ / `CreateSnapshot` / `AffectsSnapshot` (→ Requirement: EntryCell.PlaceholderColor facade)
- [x] 4.2 `KsEntryCellSnapshot` に `int? PlaceholderColor` を追加 (→ Requirement: placeholder 色の輸送)
- [x] 4.3 `SettingsView.CellPlaceholderColor` (BindableProperty) を追加 — `KsThemeSnapshot` への写しと Theme 更新配信 (→ Requirement: SettingsView.CellPlaceholderColor (Theme 段))
- [x] 4.4 `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の `KsBridgeEntryCell` / `KsBridgeTheme` に対応プロパティを追加 (Swift 側追加だけでは C# Gateway から参照できない) (→ Requirement: placeholder 色の輸送)
- [x] 4.5 両 OS の `KsBridgeGateway` で Entry / Theme の変換に placeholder 色を追加 (→ Requirement: placeholder 色の輸送)
- [x] 4.6 MAUI テスト追加 (`ConversionPathTests` 等): Entry / Theme の変換パス・null 未指定の写し・`AffectsSnapshot` (PlaceholderColor 変更が内容更新として配信)・表示中の `PlaceholderColor` / `CellPlaceholderColor` 変更の配信 (→ Requirement: placeholder 色の輸送 / EntryCell.PlaceholderColor facade / SettingsView.CellPlaceholderColor (Theme 段)、Scenario: 表示中の変更が反映される / 表示中の Theme 変更が追従する)

## 5. サンプルと視覚照合

- [x] 5.1 iOS / Android / MAUI サンプルの Entry デモに placeholder 色指定の行を1行追加 (3 platform で文言パリティ。Theme 段の一括指定はサンプルに含めず自動テストで検証) (→ Requirement: placeholder 色のデモ行 ×3)
- [x] 5.2 mock (ui/mock/placeholder-color.html) との視覚照合 — 未指定行が OS 既定のまま・Cell 個別指定行が指定色・入力済みテキストに色が乗らないことをスクリーンショットで確認 (→ Requirement: EntryCell の placeholder 文字色指定)
