# Tasks: fix-cell-accessory-vertical-fill

## 1. KsListCellBase — accessoryHolder の追加

- [x] 1.1 `KsListCellBase` に `accessoryHolder` を追加し、stackH の 3 番目の arrangedSubview として install する。空のとき `isHidden = true` (→ Requirement: KsListCellBase の自前 UIStackView 階層 / Scenario: 初期化直後の subview hierarchy)
- [x] 1.2 `prepareForReuse()` で `accessoryHolder` の内容を除去する (first responder 保護の既存挙動は維持) (→ Scenario: prepareForReuse で行内 trailing と accessory が除去される)

## 2. applyCellBaseLayout — accessoryView 系統の追加

- [x] 2.1 `applyCellBaseLayout` に `accessoryView: UIView?` パラメータを追加し、non-nil で `accessoryHolder` へ配置・nil で空 + `isHidden = true` にする (→ Scenario: accessoryView が accessoryHolder に配置される / accessoryView が nil なら accessoryHolder は空で隠れる)
- [x] 2.2 `contentConfiguration = nil` / `accessories = []` の既存 MUST を維持したまま、行内 trailing (`trailingViews` / `valueLabelText`) の経路は変更しない (→ Scenario: render 後の行内 trailing 配置)

## 3. Cell renderer 9 種の振り分け

- [x] 3.1 SwitchCellView: toggle を `trailingViews` から `accessoryView` へ (→ Requirement: Cell 級アクセサリと行内 trailing の 2 系統配置 / Scenario: SwitchCell の description がアクセサリの下に回り込まない)
- [x] 3.2 CheckboxCellView / RadioCellView / SimpleCheckCellView: checkbox / checkmark を `accessoryView` へ
- [x] 3.3 CommandCellView: chevron を `accessoryView` へ (`hideArrow == true` のとき nil)
- [x] 3.4 PickerCellView / NumberPickerCellView / TimePickerCellView / DatePickerCellView: chevron を `accessoryView` へ、valueText は `valueLabelText` (行内) のまま。`EmbeddedPickerHostField` の contentView 背面配置と first responder 挙動は変更しない (→ Scenario: Picker 系は valueText が行内・chevron が Cell 級)
- [x] 3.5 ButtonCellView / LabelCellView / EntryCellView は変更なしであることを確認 (Entry は `trailingViews: [fieldWrapper]` のまま) (→ Scenario: EntryCell の入力フィールドは行内のまま)

## 4. テスト

- [x] 4.1 subview hierarchy テストを新構造 (`[iconImageView, stackV, accessoryHolder]`) に追随させる (→ Scenario: 初期化直後の subview hierarchy)
- [x] 4.2 `accessoryView` 配置・nil 時の非表示・`prepareForReuse` クリアのテストを追加 (→ Scenario: accessoryView が accessoryHolder に配置される / accessoryView が nil なら accessoryHolder は空で隠れる / prepareForReuse で行内 trailing と accessory が除去される)
- [x] 4.3 再 render の置換規則テスト (non-nil A → non-nil B → nil、hideArrow トグル) を追加 (→ Scenario: 再 render でアクセサリが蓄積しない)
- [x] 4.4 固定幅レイアウトでの幾何テスト (description maxX <= accessoryHolder minX、アクセサリ中心 Y ≈ contentView 中心 Y、nil 時の stackV 回復) を追加 (→ Scenario: レイアウト後の幾何関係)
- [x] 4.5 既存の `contentStack.arrangedSubviews` ベース assert (BasicCellsTests / UnifyCellCommonFieldsTests / InputCellsTests 等) を 2 系統振り分け後の構造へ追随させる
- [x] 4.6 全件 `swift test` pass

## 5. 視覚照合

- [x] 5.1 Sample アプリを Simulator で起動し、承認済み mock (`ui/mock/approved.png`) およびオリジナル参照 (`ui/references/original-settingsview-maui.png`) とスクリーンショット比較 — SwitchCell の description 折り返し幅とアクセサリ垂直センターを確認
- [x] 5.2 description なしセル・icon ありセル・EntryCell の見た目が現状から劣化していないことをスクリーンショットで確認
