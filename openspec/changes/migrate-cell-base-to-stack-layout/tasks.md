# 実装タスク（migrate-cell-base-to-stack-layout）

## 0. 着手前の依存確認

- [ ] 0.1 [`design.md`](./design.md) の Decision 1〜6 を再読し、subview 階層 / 新 API シグネチャ / chevron ヘルパ / テスト方針を把握する
- [ ] 0.2 [`specs/cell-types-basic/spec.md`](./specs/cell-types-basic/spec.md) の MODIFIED Requirement「全 Cell 共通の description / valueText / icon / hintText フィールド」と追加された 2 件の Scenario（自前 UIStackView 階層が使われる / trailing controls 中心の表現）を再読する
- [ ] 0.3 [`specs/settings-view-ios-host/spec.md`](./specs/settings-view-ios-host/spec.md) の ADDED Requirement「KsListCellBase の自前 UIStackView 階層」を再読し、`stackH` / `stackV` / `contentStack` の構造と制約を把握する
- [ ] 0.4 AiForms オリジナル `../AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` L656-758 を参照し、`SetUpContentView()` の挙動を踏襲することを確認する
- [ ] 0.5 現状の `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` を読み、`hintLabel` lazy 生成 / `KsCellViewSupport` 連携を把握する
- [ ] 0.6 現状の `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift` を読み、置き換え対象となる旧 API（`accessories: [UICellAccessory]` / `valueText: String?`）の呼び出し箇所 12 件を grep で確認する

## 1. KsListCellBase の自前 UIStackView 階層 install

- [ ] 1.1 `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` に subview 6 個（`iconImageView` / `titleLabel` / `descriptionLabel` / `contentStack` / `stackV` / `stackH`）を internal stored property として追加
- [ ] 1.2 `init(frame:)` で 1 度だけ `installBaseLayout()` を呼び、subview 階層と AutoLayout 制約を構築する。AiForms オリジナル `CellBaseView.cs` L656-758 を踏襲する：`stackH.layoutMargins = (6, 16, 6, 16)`、`isLayoutMarginsRelativeArrangement = true`、`stackH.spacing = 16`、`stackV.spacing = 4`、`contentStack.spacing = 6`、Hugging / CCR 優先度設定
- [ ] 1.3 `stackH` の `topAnchor` / `leadingAnchor` / `trailingAnchor` / `bottomAnchor` を `contentView` のそれにイコール、`heightAnchor` に minHeight 制約（priority 999）を install
- [ ] 1.4 `prepareForReuse()` を override し、`titleLabel.text = nil` / `descriptionLabel.text = nil; isHidden = true` / `iconImageView.image = nil; isHidden = true` / `contentStack` から trailingViews（titleLabel 以外）を `removeArrangedSubview + removeFromSuperview` で除去
- [ ] 1.5 既存 `hintLabel` lazy 生成・右上 float 配置の挙動は変更しない（既存挙動維持）
- [ ] 1.6 `KsListCellBase` の単体ビルドが通ることを確認

## 2. CellBaseLayout.swift の新 API 実装

- [ ] 2.1 `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift` の旧 `applyCellBaseLayout(_:title:description:valueText:icon:hintText:effective:isEnabled:accessories:titleColorOverride:)` および `theme` 経由 overload を削除
- [ ] 2.2 新 API `applyCellBaseLayout(_ listCell: KsListCellBase, title:, description:, icon:, hintText:, effective:, theme:, isEnabled:, trailingViews: [UIView] = [], valueLabelText: String? = nil, titleColorOverride: UIColor? = nil)` を実装：
  - `listCell.contentConfiguration = nil` / `listCell.accessories = []` を明示
  - `titleLabel.text/font/textColor` を更新（`isEnabled == false` のとき `effective.disabledTextColor`、否のとき `titleColorOverride ?? effective.titleColor`）
  - `descriptionLabel.text/font/textColor/isHidden` を更新（`description == nil || description.isEmpty` のとき `isHidden = true`）
  - `iconImageView.image/isHidden` を `KsImage` 解決経由で更新（`nil` 時 `isHidden = true`）
  - `valueLabelText` が non-nil なら内部で `UILabel` を生成して `trailingViews.last` の前に詰む（旧 valueText の代替）
  - `contentStack` に `trailingViews` を順次 `addArrangedSubview` する
  - `backgroundConfiguration` で `effective.cellBackgroundColor` を反映
  - `KsCellViewSupport.state(listCell).theme = theme` → `setRenderState(...)` → `applyEffectiveHeight(...)` を呼び共通状態を反映
  - `ensureHintLabel()` で hintLabel を取得し font/textColor/text/isHidden を反映、末尾で `bringSubviewToFront(hintLabel)`
- [ ] 2.3 chevron 共通ヘルパ `makeChevronView() -> UIImageView` を internal helper として実装。SF Symbol `chevron.right` / `tintColor = .tertiaryLabel` / `preferredSymbolConfiguration` 設定 / `contentMode = .center`
- [ ] 2.4 `CellBaseLayout.swift` の単体ビルドが通ることを確認

## 3. 12 Cell renderer を新 API に追従

- [ ] 3.1 `LabelCellView.swift`: `applyCellBaseLayout(..., trailingViews: [])` または `valueLabelText: cell.valueText, trailingViews: []` に変更
- [ ] 3.2 `CommandCellView.swift`: `trailingViews: [makeChevronView()]`（または cell.hideArrow に応じて条件分岐）
- [ ] 3.3 `ButtonCellView.swift`: `titleColorOverride: resolvedTitleColor, trailingViews: cell.valueText != nil ? [makeValueLabel(...)] : []`
- [ ] 3.4 `SwitchCellView.swift`: `trailingViews: [uiSwitch]`
- [ ] 3.5 `CheckboxCellView.swift`: `trailingViews: [ksCheckmarkView]`
- [ ] 3.6 `RadioCellView.swift`: `trailingViews: [ksCheckmarkView]`
- [ ] 3.7 `SimpleCheckCellView.swift`: `trailingViews: [ksCheckmarkView]`
- [ ] 3.8 `EntryCellView.swift`: `trailingViews: [textField]`、textField の `setContentHuggingPriority(.defaultLow, for: .horizontal)` / `setContentCompressionResistancePriority(.defaultLow, for: .horizontal)` を init で設定。`textField.frame = CGRect(0, 0, 180, 32)` 固定指定を削除
- [ ] 3.9 `PickerCellView.swift`: `valueLabelText: resolvedValueText, trailingViews: [makeChevronView()]`
- [ ] 3.10 `NumberPickerCellView.swift`: `valueLabelText: String(cell.value), trailingViews: [makeChevronView()]`
- [ ] 3.11 `TimePickerCellView.swift`: `valueLabelText: formattedTime, trailingViews: [makeChevronView()]`
- [ ] 3.12 `DatePickerCellView.swift`: `valueLabelText: formattedDate, trailingViews: [makeChevronView()]`
- [ ] 3.13 各 Cell renderer の `prepareForReuse` を見直し、Cell 専有の subview（UISwitch / UITextField 等）は base に依存せず render で毎回再追加されることを確認。base 側 `prepareForReuse` で `contentStack` の trailingViews が除去されるため、render で再度 `addArrangedSubview` される設計に統一

## 4. テストの追従

- [ ] 4.1 `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` の `cell.contentConfiguration.text` / `.secondaryText` / `.image` の assert を `base.titleLabel.text` / `base.descriptionLabel.text` / `base.iconImageView.image` に機械的に置換
- [ ] 4.2 `BasicCellsTests.swift` の `cell.accessories.count` / `accessories.contains` 系 assert を `base.contentStack.arrangedSubviews.count` / 特定型存在チェックに置換
- [ ] 4.3 `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift` の同様の assert を置換
- [ ] 4.4 `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` の同様の assert を置換（EntryCell の `_textField` 取得経路は維持）
- [ ] 4.5 `BasicCellsTests.swift` に新規テスト `test_KsListCellBase_subviewHierarchy_AiForms準拠()` を追加し、subview 階層が想定どおり組まれていることを assert（design.md Decision 5 のコード片を参照）
- [ ] 4.6 `swift test --package-path ios` で全件 pass を確認

## 5. サンプルアプリでの見た目確認

- [ ] 5.1 `samples/ios/KsSettingsViewSample` を Simulator で起動し、すべての Cell ページを開いて見た目をスクリーンショット比較（icon あり / なし、description あり / なし、hintText あり / なし、disabled 状態）
- [ ] 5.2 LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell の 12 種すべてで現状から見た目崩れがないことを目視確認
- [ ] 5.3 EntryCell で長文を入力し、UITextField が title 残り領域全幅で表示され文字が見切れないことを確認（後続 change `add-cell-types-input` の補足タスクで完成する前段検証）
- [ ] 5.4 iOS 16 / 17 / 18 の Simulator それぞれで動作確認

## 6. 完了条件

- [ ] 6.1 `swift test` 全件 pass
- [ ] 6.2 サンプルアプリでデグレなし（目視 + スクリーンショット比較）
- [ ] 6.3 `openspec validate migrate-cell-base-to-stack-layout` で問題なし
- [ ] 6.4 `opsx:verify migrate-cell-base-to-stack-layout` でレビュー通過
- [ ] 6.5 PR 作成 → レビュー通過 → マージ → `opsx:archive migrate-cell-base-to-stack-layout`
