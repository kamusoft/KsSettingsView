# Cell Base レイアウトの自前 UIStackView 構造化 — 技術設計

## Context

KsSettingsView iOS は現在、共通行レイアウト関数 `applyCellBaseLayout(...)`（`ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`）で `UIListContentConfiguration` を組み立て、`UICellAccessory` を `accessories` 配列に詰めて Cell の見た目を構成している。これは Apple 標準の「list cell の正規パターン」を踏襲した実装である。

しかし、AiForms.Maui.SettingsView オリジナル iOS 実装（`SettingsView/Native/iOS/Cells/CellBaseView.cs` L656-758）は UITableViewCell を継承しつつ自前 `UIStackView` 階層（`stackH` + `stackV` + `contentStack` + `descriptionLabel`）を `ContentView` 直下に install する設計で、派生 Cell は `ContentStack.AddArrangedSubview(...)` で title の右に任意の subview を横並び追加できる。Hugging / CCR の優先度設定により「Title はコンテンツ幅で固定、追加 subview は残り領域全幅」が成立する。

KsSettingsView の `applyCellBaseLayout` 経由では：
- EntryCell の `UITextField` を `UICellAccessory.customView(placement: .trailing())` に押し込むと、`UICellAccessory` の幅は customView の intrinsic size か固定 frame に縛られる。現実装は `frame.width = 180pt` 固定で、長文入力時に見切れる
- 派生 Cell が「icon + title/secondaryText + accessory」以外の構造を表現できない

実機検証で EntryCell の見切れが発覚し、根本対策として AiForms オリジナル構造への移行を採る。本 change は iOS 側 Cell Base レイアウト関数の全面書き換えを行う独立 change として位置付け、後続の `add-cell-types-input` の EntryCell 補足タスクが本 change 上に成立する。

現状の関連ファイル：
- `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift` 200 行 — `UIListContentConfiguration` ベース
- `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` — `UICollectionViewListCell` 派生、`hintLabel` lazy 生成 + `KsCellViewSupport` の薄いラッパ
- 12 Cell renderer（CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / LabelCell / EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）— すべて `applyCellBaseLayout` を呼ぶ
- `BasicCellsTests.swift` / `UnifyCellCommonFieldsTests.swift` / `InputCellsTests.swift` — `contentConfiguration` / `accessories` ベース assert 多数

## Goals / Non-Goals

**Goals:**

- `applyCellBaseLayout` を AiForms オリジナル準拠の自前 `UIStackView` 構造に置き換える（`UIListContentConfiguration` / `UICellAccessory` 経路を廃止）
- 12 Cell renderer すべてが新 API で動作する状態を達成
- 見た目（spacing / margin / 配置）は AiForms オリジナルおよび現状 KsSettingsView と等価
- `hintLabel`（右上 float）の挙動を維持
- EntryCell の `UITextField` が title 残り領域全幅で配置できる前提を整える（本 change の直接ゴールではなく、後続 change `add-cell-types-input` の EntryCell 補足タスクで完成）
- 構造リグレッション防止テストを 1 件追加

**Non-Goals:**

- Android 実装の変更（`applyCellBaseLayout(views, ...)` は別経路で独立、本 change スコープ外）
- Section header / footer の supplementary view の自前 UIStackView 化（`KsSettingsViewController.makeAccessoryListCell` は引き続き `UICollectionViewListCell + UIListContentConfiguration` を使う、本 change スコープ外）
- 外部公開 API（`KsCellRegistry` / DSL / Theme / CellStyle）の破壊的変更
- EntryCell の AiForms 互換 4 点（Done ツールバー / タップフォーカス / スクロール時 dismiss）の実装（後続 change `add-cell-types-input` で対応）
- 新しい Cell 型の追加

## Decisions

### Decision 1: `KsListCellBase` が自前 `UIStackView` 階層を install する

**選択**: `KsListCellBase.init(frame:)` で 1 度だけ AiForms オリジナル準拠の subview 階層と AutoLayout 制約を構築する。

```
contentView
  └─ stackH (horizontal, alignment=.center, spacing=16, layoutMargins=(6,16,6,16),
            isLayoutMarginsRelativeArrangement=true)
       ├─ iconImageView (UIImageView, Hugging=999/CCR=999 → 固定サイズ、Image nil 時 isHidden=true)
       └─ stackV (vertical, spacing=4, Hugging=1/CCR=999)
            ├─ contentStack (horizontal, spacing=6, Hugging=1/CCR=999)
            │    └─ titleLabel (UILabel, Hugging=1/CCR=999)
            │    [render 時に派生 subview が trailingViews として追加される]
            └─ descriptionLabel (UILabel, numberOfLines=0, Hugging=1/CCR=999, 空時 isHidden=true)
```

`stackH` の `topAnchor` / `leadingAnchor` / `trailingAnchor` / `bottomAnchor` は `contentView` のそれにイコール。`heightAnchor` には `greaterThanOrEqualTo: minHeight, priority: 999` の制約を install して minHeight を確保。

`hintLabel` は AiForms オリジナル同様 `contentView` ではなく Cell 自身（`self`）に直接 addSubview し、右上 float 配置（既存挙動維持）。

**理由**:
- AiForms オリジナルの構造をそのまま踏襲することで、見た目・余白・Hugging/CCR 設定の整合性が取れる
- 派生 Cell が `base.contentStack.addArrangedSubview(subview)` で title の右に任意 subview を追加でき、EntryCell の `UITextField` 全幅、Picker 系の valueLabel + chevron、Switch/Checkbox の各 control が同じパターンで成立する
- `init` で 1 度だけ subview 構築するため、render が高速（subview の text/font/isHidden を更新するだけ）

**代替案**:
- (a) `UIListContentConfiguration` を活かしつつ EntryCell だけ例外的に独自レイアウト → 一貫性が崩れ、テスト方針も Cell ごとに分かれる。spec 規定も「例外規定」を明文化する必要がある
- (b) `contentView` 内に subview を都度生成 → render のたびに UIStackView を install する分パフォーマンスが落ちる、再利用時の view ヒエラルキー破壊リスク
- (c) Apple の `UIListContentView` のカスタム subclass → `UIListContentView` の内部実装は private で、追加 subview の正しい配置位置を制御できない

### Decision 2: `applyCellBaseLayout` の新 API — `trailingViews: [UIView]` + `valueLabelText: String?`

**選択**:

```swift
@MainActor
internal func applyCellBaseLayout(
    _ listCell: KsListCellBase,
    title: String,
    description: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    theme: Theme,
    isEnabled: Bool,
    trailingViews: [UIView] = [],
    valueLabelText: String? = nil,
    titleColorOverride: UIColor? = nil
)
```

`trailingViews` は title の右に並べる任意 UIView 配列。例：
- `EntryCellView` → `[textField]`
- `CommandCellView` → `[makeChevronView()]`
- `SwitchCellView` → `[uiSwitch]`
- `PickerCellView` → `[makeChevronView()]`（または `valueLabelText` ショートカット + `[makeChevronView()]`）

`valueLabelText` は便利ショートカット：non-nil のとき内部で `UILabel` を生成して `trailingViews` の末尾に詰む。旧 `valueText` パラメータの代替で、`LabelCell` / `PickerCell` 等が value 表示で使う。

**理由**:
- AiForms オリジナル `ContentStack.AddArrangedSubview(...)` の感覚で「title の右に何を並べるか」を素直に表現できる
- `UICellAccessory` の中間型を挟まず、直接 `UIView` を渡せるため customView の幅問題が原理的に発生しない
- `valueLabelText` ショートカットで「Picker 系の `[valueLabel, chevron]` を 1 行で渡せる」設計が両立

**代替案**:
- 旧 `accessories: [UICellAccessory]` を維持して中身を customView ベースに統一 → `UICellAccessory` 経由の幅制約問題が残る
- `leadingViews` / `trailingViews` を両方サポート → leading 側に追加するユースケースが無く、過剰設計

### Decision 3: `contentConfiguration` / `UICellAccessory` の使用を完全に廃止

**選択**: 新 `applyCellBaseLayout` 内で `listCell.contentConfiguration = nil` / `listCell.accessories = []` を明示的にセットし、Apple 標準経路を完全に無効化する。

**理由**:
- 自前 stack 構造と `UIListContentConfiguration` を共存させると、title の描画位置が 2 重定義され、レイアウトが不安定になる
- システム自動 margin（左 16pt / 右 16pt）に依存しなくなるため、`stackH.layoutMargins = (6, 16, 6, 16)` で自前管理する
- `UICellAccessory` 由来の chevron や customView は使わず、すべて `trailingViews` 経由で添える

**代替案**: `contentConfiguration` を残して title を Apple 標準で、accessory を自前で → title 位置が両方の系統で計算され、AutoLayout 警告と微妙な位置ズレが発生

### Decision 4: chevron 共通ヘルパ `makeChevronView()` を導入

**選択**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift` 内に internal helper として `makeChevronView() -> UIImageView` を実装。SF Symbol `chevron.right` を `UIImage(systemName:)` で生成し、`tintColor` は `UIColor.tertiaryLabel`、`preferredSymbolConfiguration` は `.init(font: .preferredFont(forTextStyle: .body), scale: .small)`、`contentMode = .center`、固定サイズ制約は付けず intrinsicContentSize に従う。

**理由**:
- CommandCell / Picker 系 4 種で共通利用するため一元化
- AiForms オリジナルでは `Disclosure Indicator` を UITableViewCell の `AccessoryType` で出していたが、自前 stack に乗せるため自前生成する

**代替案**: 各 Cell renderer で個別に chevron 生成 → コード重複、tintColor / size の整合が崩れる

### Decision 5: テスト構造リグレッション防止テストの 1 件追加

**選択**: `BasicCellsTests.swift` に以下を追加：

```swift
func test_KsListCellBase_subviewHierarchy_AiForms準拠() {
    let base = KsListCellBase(frame: CGRect(x: 0, y: 0, width: 320, height: 44))
    // contentView 直下に stackH が存在
    XCTAssertTrue(base.stackH.isDescendant(of: base.contentView))
    // stackH の arrangedSubviews が [iconImageView, stackV]
    XCTAssertEqual(base.stackH.arrangedSubviews, [base.iconImageView, base.stackV])
    // stackV の arrangedSubviews が [contentStack, descriptionLabel]
    XCTAssertEqual(base.stackV.arrangedSubviews, [base.contentStack, base.descriptionLabel])
    // contentStack の最初の arrangedSubview が titleLabel
    XCTAssertEqual(base.contentStack.arrangedSubviews.first, base.titleLabel)
}
```

これにより subview 階層が将来誤って改変されても CI で検出できる。

**理由**: 自前 UIStackView 構造は spec で MUST 化するため、構造のリグレッションを早期検出する必要がある

**代替案**: snapshot テスト → セットアップ重く CI コストが上がる。ヒエラルキー単体の検証で十分

### Decision 6: 既存テストの `contentConfiguration` / `accessories` assert は新構造ベースに機械的に置換

**選択**: 既存テストで以下のような assert を置換：

| 旧 | 新 |
|---|---|
| `XCTAssertEqual(cell.contentConfiguration.text, "Title")` | `XCTAssertEqual(base.titleLabel.text, "Title")` |
| `XCTAssertEqual(cell.contentConfiguration.secondaryText, "Desc")` | `XCTAssertEqual(base.descriptionLabel.text, "Desc")` |
| `XCTAssertEqual(cell.accessories.count, 1)` | `XCTAssertEqual(base.contentStack.arrangedSubviews.count, 2)` (title + 1 trailing) |
| `XCTAssertTrue(cell.accessories.contains(where: { $0.accessoryType == .disclosureIndicator }))` | `XCTAssertTrue(base.contentStack.arrangedSubviews.last is UIImageView)` |

**理由**: 検証する「Cell が title/description/accessory を持つこと」の意味は同じで、検証対象を新構造に向けるだけ。テスト網羅性は維持される

## Risks / Trade-offs

- **見た目デグレ**: 自前 Stack 構造への移行で iconSize / spacing / margin / 行高さが現状と微妙にズレる可能性 → **対策**: Simulator 上で全 Cell の見た目を実機 / 旧版とスクリーンショット比較。`stackH.layoutMargins = (6, 16, 6, 16)` + `iconImageView` サイズ制約は AiForms オリジナル準拠の値を `EffectiveStyle.iconSize` 経由で適用
- **行高さ計算の衝突**: `KsCellViewSupport.applyEffectiveHeight` が install する高さ制約と、`stackH` の `minHeight` 制約が両方 active になると AutoLayout 競合 → **対策**: 既存 `applyEffectiveHeight` が `cell.contentView.heightAnchor` に制約を貼っているなら維持しつつ、`stackH.heightAnchor.constraint(greaterThanOrEqualTo: minHeight, priority: 999)` で stackH 側にも minHeight を持たせる。priority 999 で衝突時もシステム警告を抑制
- **テスト改修工数**: `contentConfiguration` / `UICellAccessory` 経路で assert している既存テストが多数（`BasicCellsTests.swift` 753 行 / `UnifyCellCommonFieldsTests.swift` 548 行を中心に 60〜80 行程度の改修見込み）→ **対策**: 機械的置換で対応、置換後も意味が同等
- **EffectiveStyle の API 拡張**: 既存 `effective.cellBackgroundColor` / `titleFont` / `titleColor` / `descriptionFont` / `descriptionColor` / `valueTextFont` / `valueTextColor` / `hintTextFont` / `hintTextColor` / `disabledTextColor` / `accentColor` などは現状の API で足りる想定 → **対策**: 実装時に不足が判明したら本 change スコープ内で `EffectiveStyle` に getter を追加。仕様変更を伴わない範囲なら独立 change 不要
- **後方互換性のロスト**: 外部から `UICellAccessory` を渡している利用者がいないか → **対策**: 公開 API は `KsCellRegistry` 経由のみで `UICellAccessory` を露出していない。`KsCellRegistry+InputCells.swift` 等を grep して安全確認

## Migration Plan

1. proposal.md / design.md / specs / tasks.md を作成・レビュー
2. `KsListCellBase` に subview 6 個を install → 単体ビルド成功
3. `CellBaseLayout.swift` の新 API 実装 → 単体ビルド成功
4. 12 Cell renderer を新 API に追従（1 ファイルずつ）→ 各 `swift test --filter <CellName>Tests` で個別検証
5. テスト改修（`BasicCellsTests` / `UnifyCellCommonFieldsTests` / `InputCellsTests` を機械的に置換）
6. 全件 `swift test` pass
7. Simulator で `KsSettingsViewSample` 起動、各 Cell の見た目を旧版とスクリーンショット比較
8. `opsx:verify` → `opsx:archive`

ロールバック: 本 change は 1 PR にまとめ、cherry-pick / revert がしやすい状態を保つ。問題が出たら PR ごと revert で旧状態に戻る。

## Open Questions

- なし（実装時に判断する細部は実装者に委ねる）
