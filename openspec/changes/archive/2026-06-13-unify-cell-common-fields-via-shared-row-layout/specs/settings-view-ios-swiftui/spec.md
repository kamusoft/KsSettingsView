## ADDED Requirements

### Requirement: 共通行レイアウト関数 applyCellBaseLayout

`KsSettingsViewUI`（iOS）は、全 Cell View が共通して使う **行レイアウト関数 `applyCellBaseLayout(...)`** を `internal` 可視性で提供しなければならない (SHALL)。この関数は `cell-types-basic` の「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement で規定された 2 系統のレイアウト規約（本体行 `[icon][title / description][valueText (title 行の右寄せ)][accessory (右側中央)]` + `hintText` の右上 float 配置）を `UICollectionViewListCell` 上に組み立てる責務を持つ。

iOS 実装は `UIListContentConfiguration` ベースを **維持** しなければならない (MUST)。本体行（icon / title / description / valueText / accessory）は `UIListContentConfiguration` + `UICellAccessory` で構成し、`hintText` のみ `UICollectionViewListCell` の `contentView` の **外側**（`UICollectionViewListCell` 直下、すなわち `cell.addSubview(hintLabel)`）に専用の `UILabel` を配置することで、オリジナル `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` の `HintLabel`（`UITableViewCell` 直下に AddSubview、`TopAnchor=2`, `RightAnchor=-10`）相当の右上 float 配置を再現する。

関数のシグネチャは次の形でなければならない (MUST)：

```swift
@MainActor
internal func applyCellBaseLayout(
    _ listCell: UICollectionViewListCell,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Bool,
    accessories: [UICellAccessory] = []
)
```

実装上の振る舞いは以下を満たさなければならない (MUST)：

- `title` / `description` / `valueText` / `icon` を `UIListContentConfiguration` に反映する。`description` と `valueText` の組み合わせに応じて `cell()` / `subtitleCell()` / `valueCell()` のいずれかを選択する（既存 `applyLabelCellContents` の分岐を踏襲）。
- `icon` の `KsImage` 派生（`systemName` / `uiImage`）を網羅して `content.image` に設定する。`icon == nil` のときは `content.image = nil` を明示する。
- 「`description` と `valueText` 両方ありかつ subtitle 構成のとき」の `valueText` は `UICellAccessory.customView(placement: .trailing())` として組み立て、`listCell.accessories` の **先頭側**（最も content 寄り）に置く。これにより本体行の title 行右寄せに valueText が表示される（既存挙動を踏襲）。
- `hintText` は **`UICellAccessory` には含めない** (MUST NOT)。代わりに、`UICollectionViewListCell` 直下に専用の `UILabel`（以下「hintLabel」と呼ぶ）を `cell.addSubview(hintLabel)` で追加し、以下の AutoLayout 制約で右上 float 配置する：
  - `hintLabel.topAnchor.constraint(equalTo: cell.topAnchor, constant: 2)`
  - `hintLabel.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor, constant: -10)`
  - `hintLabel.bottomAnchor.constraint(lessThanOrEqualTo: cell.bottomAnchor, constant: -12)`（hintLabel が縦方向にはみ出さないようにする）
- `hintLabel` の `font` は `effective.hintTextFont`、`textColor` は `effective.hintTextColor`（`isEnabled == false` のときは `effective.disabledTextColor`）、`textAlignment = .right`、`numberOfLines = 1`、`lineBreakMode = .byTruncatingTail`（オリジナル挙動の「小さな・右寄せ・1 行・末尾省略」を踏襲）。
- `hintText == nil` または空文字のときは `hintLabel.isHidden = true`、`hintText != nil` のときは `hintLabel.text = hintText` を反映して `isHidden = false`。
- 最終的に `listCell.accessories` には **本体行の trailing accessories として** 次の順番で配置する（インデックスが小さいほど content 寄り、インデックスが大きいほど画面右端寄り）: `[valueText accessory (subtitle 構成時のみ), 呼び出し側 accessories...]`。**`hintText` は含めない**。すなわち、呼び出し側 `accessories` 引数（Cell 種別固有の trailing コントロール: `UISwitch` / `MaterialCheckBox` 相当の customView / `chevron` 等）が最も画面右端寄り、subtitle 構成の `valueText` がその左に並ぶ。
- `isEnabled == false` のときは、各テキスト色（`title` / `description` / `valueText` / `hintLabel`）を `effective.disabledTextColor` で上書きする。
- `effective: EffectiveStyle` を受け取り、`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `hintTextColor` / `hintTextFont` / `cellBackgroundColor` をそこから解決する（Change 1 で確立された `CellStyle → Theme → 既定` の解決順序に委譲する）。
- `KsCellViewSupport.setRenderState(listCell, theme:, isEnabled:, effectiveBackgroundColor:)` と `KsCellViewSupport.applyEffectiveHeight(listCell, effective:)` を内部で呼ぶ。これにより `KsListCellBase.preferredLayoutAttributesFitting` で `CellStyle.cellHeight` が反映される経路が維持される。

#### hintLabel の所有とリサイクル管理

`hintLabel` は `UICollectionViewListCell` の所有とし、Cell 再利用時のリサイクル管理は以下のいずれかの方式で実装しなければならない (MUST)：

- 方式 A: `KsListCellBase` 派生に `hintLabel: UILabel?` プロパティを宣言し、`applyCellBaseLayout` の初回呼び出しで lazy に生成・`addSubview` し、参照を保持する。`prepareForReuse()` では `hintLabel.text = nil` / `isHidden = true` をリセットする（subview としては保持し続け、生成コストを削減する）。
- 方式 B: Associated Object（`objc_setAssociatedObject`）で `hintLabel` への参照を `UICollectionViewListCell` に紐づけ、`applyCellBaseLayout` 内で取得・遅延生成する。

いずれの方式でも、複数回 `applyCellBaseLayout` を呼んでも `hintLabel` が重複 `addSubview` されてはならない (MUST NOT)。テストでは `hintLabel` の subview 数が常に 1 個（または `isHidden = true` の 1 個）であることを検証する。

#### 各 Cell View からの利用

各 Cell View（`LabelCellView` / `CommandCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView` / `ButtonCellView`）は、`render(cell:theme:)` 内で `applyCellBaseLayout(...)` を呼び出して描画しなければならない (MUST)。`title` / `description` / `valueText` / `icon` / `hintText` のレイアウト組み立てロジックを各 Cell View 内に重複して実装してはならない (MUST NOT)。

各 Cell View は、自身固有の trailing コントロール（例: `SwitchCellView` の `UISwitch`、`CheckboxCellView` の角丸チェックボックス View、`RadioCellView` の checkmark customView、`CommandCellView` の chevron）を `UICellAccessory` として組み立て、`applyCellBaseLayout(...)` の `accessories` 引数に渡さなければならない (MUST)。`LabelCellView` および `ButtonCellView` は常に `accessories: []` を渡す（trailing コントロールを持たない）。`ButtonCellView` における `icon` / `valueText` / `hintText` の有無は **ボタンスタイルレイアウト（中央寄せ等）と通常レイアウトの切り替え判定にのみ**用いられ、`accessories` 引数の中身には影響しない（`ButtonCell` は `cell-types-basic` の MUST NOT 制約により `description` フィールドを持たないため、判定対象には `description` は含まれない）。

#### 旧 ksCellRow 関数からのリネーム

本 Requirement における関数名 `applyCellBaseLayout` は、本 change の旧版で採用されていた関数名 `ksCellRow` から **リネーム** されたものである。`ksCellRow` 関数は本 Requirement 適用時に **削除** しなければならない (MUST)。両者の I/F は等価であり、シグネチャの第 1 引数は引き続き `UICollectionViewListCell` を受け取る UIKit Builder 関数である。

#### Scenario: applyCellBaseLayout が共通フィールドを反映し hintText を右上 float 配置する

- **GIVEN** `let cell = SwitchCell(title: "通知", description: "プッシュ通知", valueText: "オン", icon: KsImage.systemName("bell"), hintText: "推奨", isOn: true)`、SwitchCellView の `render` 内で `applyCellBaseLayout(self, title: cell.title, description: cell.description, valueText: cell.valueText, icon: cell.icon, hintText: cell.hintText, effective: effective, isEnabled: cell.isEnabled, accessories: [switchAccessory])` を呼ぶ
- **WHEN** Cell が描画される
- **THEN** `listCell.contentConfiguration` の `text` が "通知"、`secondaryText` が "プッシュ通知"、`image` が `UIImage(systemName: "bell")` で組まれる。`listCell.accessories` は `[valueText label "オン" customView, UISwitch の customView]` の順（インデックス 0 が最も content 寄り、最後の要素が最も画面右端寄り）で並び、`hintText` は accessories に含まれない。`hintText` 「推奨」は `cell` 直下の `hintLabel`（`topAnchor = 2`, `trailingAnchor = -10`）にテキスト反映され、右上 float 表示される

#### Scenario: hintText と accessory が物理的に重ならない

- **GIVEN** `SwitchCell(title: "通知", hintText: "推奨", isOn: true)` を描画した状態
- **WHEN** Cell の subview / accessory の frame を取得する
- **THEN** `hintLabel.frame.maxY` は `cell.contentView.center.y` よりも上にあり（通常 hint 1 行分の高さ + マージン以内）、UISwitch を含む accessory の frame は `cell.contentView.center.y` 近辺にある。両者は右端 X が揃っているが、縦方向に位置が分離しているため重ならない

#### Scenario: 各 Cell View が共通レイアウト関数を経由する

- **GIVEN** `KsSettingsViewUI` ソース内の `LabelCellView.swift` / `CommandCellView.swift` / `SwitchCellView.swift` / `CheckboxCellView.swift` / `RadioCellView.swift` / `SimpleCheckCellView.swift` / `ButtonCellView.swift`
- **WHEN** これらのファイルから `render(cell:theme:)` の本体を grep する
- **THEN** 各 Cell View は `applyCellBaseLayout(...)` を呼び出しており、`UIListContentConfiguration.cell()` / `subtitleCell()` / `valueCell()` の生成や `content.text = ...` / `content.image = ...` を直接書いている箇所はない（旧 `applyLabelCellContents` / `ksCellRow` 等のヘルパは `applyCellBaseLayout` への置き換え後に削除される）

#### Scenario: hintLabel が prepareForReuse で適切にリサイクルされる

- **GIVEN** `UICollectionViewListCell` を再利用するため `cellForRowAt` で前回 `hintText = "推奨"` が反映されていた cell に対し、次は `hintText = nil` の Cell モデルを bind する
- **WHEN** `applyCellBaseLayout(..., hintText: nil, ...)` が呼ばれる
- **THEN** `hintLabel.text = nil` または `hintLabel.isHidden = true` がリセットされ、前回の「推奨」テキストは表示されない。`hintLabel` の subview 数は 1 個のまま（`addSubview` の二重呼び出しは発生しない）

#### Scenario: cellHeight 反映が維持される

- **GIVEN** `LabelCell(title: "X", style: CellStyle(cellHeight: 80.0))`、`Theme.hasUnevenRows = false`
- **WHEN** `applyCellBaseLayout(...)` 経由で描画される
- **THEN** 内部で `KsCellViewSupport.applyEffectiveHeight(listCell, effective:)` が呼ばれ、`KsListCellBase.preferredLayoutAttributesFitting` 経路で実視覚的セル高さが 80pt（誤差±数pt、既存 Phase 17 テストの許容範囲）に固定される

#### Scenario: applyCellBaseLayout が internal 可視性

- **GIVEN** `KsSettingsViewUI` の外部モジュール（例: `KsSettingsViewCore` / サンプルアプリ / 後続 change で追加される未来の Cell）
- **WHEN** `import KsSettingsViewUI` 後に `applyCellBaseLayout(...)` を直接呼び出そうとする
- **THEN** `internal` 可視性のためコンパイルエラーになる。外部から共通行レイアウトを再利用する必要が生じた場合は、本 Requirement とは別の change で `public` 化を検討する

#### Scenario: 旧 ksCellRow 関数が削除されている

- **GIVEN** 本 change 適用後の `ios/Sources/KsSettingsViewUI/` ディレクトリ
- **WHEN** `ksCellRow` を grep する
- **THEN** 関数定義が存在せず、呼び出し箇所もすべて `applyCellBaseLayout` にリネームされている
