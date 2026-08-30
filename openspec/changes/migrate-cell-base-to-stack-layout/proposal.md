# Cell Base レイアウトを独自 UIStackView 構造へ移行

## Why

iOS の共通行レイアウト関数 `applyCellBaseLayout(...)` は現在 Apple 標準の `UIListContentConfiguration`（固定フィールド `text` / `secondaryText` / `image` + `UICellAccessory`）に依存している。Apple 推奨の構造ではあるが、以下の本質的な制約がある：

1. **EntryCell の `UITextField` が title 残り領域全幅を取れない**: `UICellAccessory.customView(placement: .trailing())` の枠サイズが intrinsicContentSize または固定 frame に縛られ、現行実装は `frame.width = 180pt` 固定。長文入力時に右端で見切れる。AiForms.Maui.SettingsView オリジナル iOS 実装は title の右に `UIStackView` で `UITextField` を配置し、Hugging / CCR 優先度で「Title はコンテンツ幅で固定、TextField は残り領域全幅」を実現していた。
2. **派生 Cell が `contentView` 内に subview を自由配置できない**: 標準構成（icon + text/secondaryText + accessory）以外のレイアウトを表現する手段がなく、今後カスタム Cell や複雑な入力 Cell を追加する際の拡張性が低い。

AiForms オリジナル `CellBaseView.cs` L656-758 は UITableViewCell を継承しつつ、独自 `UIStackView` 階層（horizontal stackH + vertical stackV + horizontal contentStack + descriptionLabel）を `ContentView` 直下に構築している。派生クラスは `ContentStack.AddArrangedSubview(...)` で title の右に好きな subview を横並びで追加でき、EntryCell は `UITextField` を、ピッカー系 4 種は valueLabel + chevron を、Switch / Checkbox / Radio 系は対応 control を、それぞれ追加するだけで成立する。

本変更はこの AiForms オリジナル構造を iOS 側 KsSettingsView に取り込み、`applyCellBaseLayout` を Apple 標準依存から自前 UIStackView 構造に置き換える。

## What Changes

- **BREAKING**: `applyCellBaseLayout(...)` の API を `UICellAccessory` ベースから素の `UIView` ベース（`trailingViews: [UIView]`）に置き換える。`UIListContentConfiguration` は内部で使わない
- **BREAKING**: `KsListCellBase` が `iconImageView` / `titleLabel` / `descriptionLabel` / `contentStack` / `stackV` / `stackH` の 6 個の subview を `contentView` 直下に install する。`init(frame:)` で 1 度だけ subview 階層と AutoLayout 制約を構築する
- 旧パラメータ `valueText: String?` は `valueLabelText: String?` に名称変更（実体は内部で `UILabel` を生成して `trailingViews` 末尾に詰む既存挙動を維持）
- 旧パラメータ `accessories: [UICellAccessory]` を廃止し、`trailingViews: [UIView]` に置換。Cell renderer 側は素の `UIView`（UISwitch / UIImageView / KsCheckmarkView / UITextField 等）を渡す
- 12 Cell renderer（CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / LabelCell / EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）すべてが新 API に追従
- chevron 生成の共通ヘルパ `makeChevronView()` を導入（`UIImage(systemName: "chevron.right")` を nest した `UIImageView`）
- `hintLabel`（cell 直下右上 float 配置）は既存挙動を維持（変更なし）
- 既存テスト（`BasicCellsTests.swift` / `UnifyCellCommonFieldsTests.swift` / `InputCellsTests.swift` 等）の `contentConfiguration` / `UICellAccessory` ベース assert を新構造（`titleLabel.text` / `contentStack.arrangedSubviews` ベース）に置換

見た目（spacing / margin / 配置）は AiForms オリジナルおよび現行 KsSettingsView と等価になるよう調整する。**外部利用者向け API（`KsCellRegistry` / DSL / Theme / CellStyle）には影響なし**。

## Capabilities

### New Capabilities

（なし）

### Modified Capabilities

- `cell-types-basic`: 共通行レイアウト関数 `applyCellBaseLayout` の Requirement を MODIFIED。旧 `UIListContentConfiguration` 経路の規定を撤回し、新たに「自前 `UIStackView` 構造 + `trailingViews: [UIView]` 経路で `title` / `description` / `icon` / `hintText` / 派生 subview を構成」する MUST 規定を導入する
- `settings-view-ios-host`: `KsListCellBase` が `contentView` 直下に独自 `UIStackView` 階層（stackH / stackV / contentStack）を install する MUST 規定を ADDED。subview 構造とその priority（Hugging / CCR）を spec として明文化する

## Impact

- **iOS のみ**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`（全面書き換え）、`ios/Sources/KsSettingsViewUI/KsListCellBase.swift`（subview 6 個と install ロジック追加）、12 Cell View ファイル（render 内の新 API 追従）
- **Android は無影響**: `applyCellBaseLayout` は iOS のキャプチャ。Android `applyCellBaseLayout(views, ...)` は別経路で実装されておりそのまま
- **テスト**: `BasicCellsTests.swift` / `UnifyCellCommonFieldsTests.swift` / `InputCellsTests.swift` 等で `contentConfiguration` / `UICellAccessory` ベース assert を新構造ベースに置換。構造リグレッション防止テストを 1 件追加（`KsListCellBase` の subview hierarchy 検証）
- **依存追加なし**: 既存 UIKit のみで完結
- **外部 API 影響なし**: `KsCellRegistry` 公開 API / SwiftUI DSL / Cell 型定義 / Theme / CellStyle は変わらない
- **後続 change への寄与**: `add-cell-types-input` の EntryCell 補足タスク（タップフォーカス・Done ツールバー・スクロール時 keyboardDismiss）は本 change 完了後に着手することで、「UITextField 全幅」要件が自動達成される

## Risks

- **リスク A — 12 Cell の見た目デグレ**: 自前 Stack 構造への移行で iconSize / spacing / margin が AiForms オリジナル準拠になり、KsSettingsView 現状の見た目と微妙にズレる可能性。**対策**: design.md で「Theme.cellIconSize / 行 margin」等の解決経路を明文化し、現状挙動を維持する Test を残す。Simulator 上で全 Cell の見た目をスクリーンショット比較する
- **リスク B — 行高さ計算**: 既存 `KsCellViewSupport.applyEffectiveHeight` と新 stack 構造の minHeight 制約が衝突しないか。**対策**: AiForms オリジナル準拠で `stackH.heightAnchor.constraint(greaterThanOrEqualTo: minHeight, priority: 999)` を install
- **リスク C — テスト改修工数**: `contentConfiguration` / `UICellAccessory` 経路で assert している既存テストが多数。**対策**: 同等の意味を持つ新構造 assert に機械的に置換し、テスト網羅性を維持
- **リスク D — Section H/F の supplementary view との不整合**: `KsSettingsViewController.makeAccessoryListCell` は section header/footer を `UIListContentConfiguration` で描画している。これは Cell renderer とは独立だが、整合性のため将来同じ自前 Stack 構造に揃えるかは別 change で議論（本 change のスコープ外）
