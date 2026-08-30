## ADDED Requirements

### Requirement: Section Header / Footer の sticky 抑止

`settings-view-ios-ui` は `UICollectionViewCompositionalLayout` の `boundarySupplementaryItems` に対して、Section Header および Section Footer の両方とも `pinToVisibleBounds = false` を強制しなければならない (MUST)。Sticky な追従挙動は採用してはならない (MUST NOT)。

#### Scenario: Section Header の sticky 抑止

- **GIVEN** 複数 Section を持つ SettingsView をスクロールする
- **WHEN** ある Section の Header が画面上端を超えてスクロールする
- **THEN** Header は上端に固定されず、通常スクロールに従って画面外に出る

#### Scenario: Section Footer の sticky 抑止

- **GIVEN** 複数 Section を持つ SettingsView をスクロールする
- **WHEN** ある Section の Footer が画面下端を下回ろうとする
- **THEN** Footer は画面下端に固定されず、通常スクロールに従って画面外に出る

#### Scenario: 既存 Header sticky 抑止の維持

- **GIVEN** 前 change `refine-basic-cells-style` で実装された Header の `pinToVisibleBounds = false`
- **WHEN** 本 change で boundarySupplementaryItems のループを変更する
- **THEN** Header の sticky 抑止は維持されたまま、Footer 側にも同じ抑止が追加される

### Requirement: viewBackgroundColor のセクション間反映

`settings-view-ios-ui` は `Theme.viewBackgroundColor` を `UICollectionView` の背景色として設定する際、`UICollectionLayoutListConfiguration.backgroundColor` を `.clear` に設定しなければならない (MUST)。これにより `UICollectionView.backgroundColor` がセクション間の隙間（supplementary 領域・section inset）にも透過して反映され、`Theme.viewBackgroundColor` が見える状態になる。

#### Scenario: viewBackgroundColor がセクション間にも反映される

- **GIVEN** `Theme(viewBackgroundColor: KsColor(0.95, 0.93, 0.90, 1.0), cellBackgroundColor: KsColor.white)` を適用した SettingsView
- **WHEN** iOS で描画される
- **THEN** 各 Cell の背景は `cellBackgroundColor` の白、Section 間（Header / Footer 領域および Section inset）の背景は `viewBackgroundColor` の薄ベージュ色が反映される

#### Scenario: cellBackgroundColor の維持

- **GIVEN** 上記と同じ Theme
- **WHEN** Cell が描画される
- **THEN** Cell 自身の背景描画は `UIListContentConfiguration.backgroundConfiguration` 経由で `cellBackgroundColor` が維持され、`backgroundColor = .clear` の変更によって Cell の背景が消えてはならない

### Requirement: Section.headerHeight の UI 反映

`settings-view-ios-ui` は `Section.headerHeight: Double` の値を `UICollectionViewCompositionalLayout` のセクション Header 高さに反映しなければならない (MUST)。優先順位は以下：

1. `section.headerHeight > 0` → `.absolute(section.headerHeight)` を Header の `layoutSize.heightDimension` に適用する
2. `section.headerHeight == -1` かつ `section.header` が非空（テキストまたは View） → `.estimated(自然な値)` を適用し UIKit に高さ自動算出させる
3. `section.headerHeight == -1` かつ `section.header == nil`（テキスト空または未指定） → Header の supplementary 自体を生成してはならない (MUST NOT)

#### Scenario: headerHeight 正値による固定高さ

- **GIVEN** `Section(header: SectionAccessory.text("一般"), headerHeight: 40, ...)`
- **WHEN** iOS で描画される
- **THEN** その Section の Header の表示高さが 40pt 固定で描画され、テキストの自然な高さよりも 40pt が優先される

#### Scenario: headerHeight 正値が AutoLayout 下端揃えと両立する

- **GIVEN** `Section(header: SectionAccessory.text("CommandCell"), headerHeight: 60, ...)`
- **WHEN** iOS で描画される
- **THEN** boundary supplementary item の `layoutSize.heightDimension` は `.absolute(60)` に設定され、Phase 15.1 で導入された UILabel + AutoLayout 制約（`bottomAnchor == contentView.bottomAnchor`, priority 999）と両立してテキストが 60pt 領域の下端に張り付く形で描画される。`.estimated(...)` への置き換えで指定値が打ち消されてはならない (MUST NOT)

#### Scenario: headerHeight = -1 + header テキスト有りの自動高さ

- **GIVEN** `Section(header: SectionAccessory.text("一般"), headerHeight: -1, ...)`（既定）
- **WHEN** iOS で描画される
- **THEN** Header はテキスト寸法に応じた自動高さで描画され、不要な上下余白を持たない

#### Scenario: headerHeight = -1 + header 空時の supplementary 非生成

- **GIVEN** `Section(header: nil, headerHeight: -1, ...)`（既定、ヘッダ未設定）
- **WHEN** iOS で描画される
- **THEN** その Section の Header 領域は supplementary として生成されず、前 Section との間には Section inset のみが見える状態となる

### Requirement: Section Footer 空時の supplementary 非生成

`settings-view-ios-ui` は Section の `footer` が `nil` または `SectionAccessory.text("")`（空文字列）の場合、Section Footer の supplementary 領域を生成してはならない (MUST NOT)。Footer 領域が見えないことで、Section 末尾には次 Section の Section inset のみが残る状態となる。

#### Scenario: footer 未設定時の余白除去

- **GIVEN** `Section(footer: nil, ...)`
- **WHEN** iOS で描画される
- **THEN** Section 末尾には Footer の supplementary 領域が生成されず、不要な余白（前 change までは数十 pt の灰色帯として現れていた）が消える

#### Scenario: footer 設定時の通常描画

- **GIVEN** `Section(footer: SectionAccessory.text("注意: 削除は取り消せません"), ...)`
- **WHEN** iOS で描画される
- **THEN** Footer の supplementary 領域が通常通り生成され、テキスト寸法に応じた自然な高さで描画される

### Requirement: Header / Footer 周辺の不要余白の最小化

`settings-view-ios-ui` の Header / Footer 描画は、Android 版と同等の密度になるよう、不要な上下インセットを残してはならない (MUST NOT)。具体的には：

- `UICollectionLayoutListConfiguration.headerTopPadding` を `0` に設定しなければならない (MUST)。既定値（iOS 15+ で ~18pt）のままにしてはならない (MUST NOT)。
- Section / Root の `NSCollectionLayoutBoundarySupplementaryItem` の `heightDimension` は `.estimated(20)` 以下（テキスト 1 行 + 上下マージン 4pt 程度）に設定しなければならない (MUST)。`.list(using:)` 既定の `.estimated(44)` 相当の大きな値をそのまま使ってはならない (MUST NOT)。
- 同 `NSCollectionLayoutBoundarySupplementaryItem.contentInsets` は `.zero` に明示的に設定しなければならない (MUST)。
- Header テキスト下と直下 Cell 上端の間に、`UIListContentConfiguration` の既定上下パディング以上の余白を発生させない (MUST NOT)。Header / Footer 用 supplementary に適用する `UIListContentConfiguration.directionalLayoutMargins` は上下 2pt 以下に詰めなければならない (MUST)。
- Footer テキスト上と直前 Cell 下端の間に、`UIListContentConfiguration` の既定上下パディング以上の余白を発生させない (MUST NOT)。
- 空テキストや `nil` の Header / Footer は supplementary 自体を生成しない（既存「Section Footer 空時の supplementary 非生成」要件のとおり）。

これらすべてを満たすことで、Header / Footer 周辺の上下余白合計は **8pt 以下**（おおむね Header テキスト直上の 2pt + 直下の 2pt + Cell 側の罫線インセット程度）に収まり、Android スクリーンショットと同等の密度となる。

**Rationale**: 前回実装で `UIListContentConfiguration.directionalLayoutMargins` だけを 2pt に縮めても、`UICollectionLayoutListConfiguration.headerTopPadding`（既定 ~18pt）と supplementary item の `.estimated(44)` 高さが残っていたため、Header / Footer の上下に 30〜40pt 規模の余白が発生していた。実機目視確認で Android と比較してスカスカに見える状態だったため、これらの値を併せて 0 化・縮小する必要がある。

#### Scenario: Header 下の余白が詰まる

- **GIVEN** "CommandCell" ヘッダの直下に CommandCell 行がある
- **WHEN** iOS で描画される
- **THEN** "CommandCell" ヘッダのベースライン下と最初の Cell の上罫線の間に、`UIListContentConfiguration` の既定値（8〜10pt 程度）相当の余白が残らず、Android 側と同等の密度（合計 8pt 以下）に詰まる

#### Scenario: Footer 上の余白が詰まる

- **GIVEN** "RadioCell" セクションの最後の Cell 直下に Footer テキストがある
- **WHEN** iOS で描画される
- **THEN** 最後の Cell の下罫線と Footer テキストの上端の間に、`UIListContentConfiguration` 既定値相当の余白が残らず、Android 側と同等の密度（合計 8pt 以下）に詰まる

#### Scenario: headerTopPadding が 0 に設定される

- **GIVEN** `UICollectionLayoutListConfiguration` が `makeLayout(for:)` で生成される
- **WHEN** 設定値を検証する
- **THEN** `headerTopPadding == 0` である（iOS 15+ 既定の ~18pt ではない）

### Requirement: 罫線インセット規則

`settings-view-ios-ui` は Cell の罫線（separator）を `UIListSeparatorConfiguration` でカスタマイズし、Cell の位置に応じてインセット幅を切り替えなければならない (MUST)。

- **セクション最初の Cell の top separator** → 可視（`.visible`）、`topSeparatorInsets.leading = 0`、`topSeparatorInsets.trailing = 0`（端から端へ）
- **セクション最後の Cell の bottom separator** → 可視（`.visible`）、`bottomSeparatorInsets.leading = 0`、`bottomSeparatorInsets.trailing = 0`（端から端へ）
- **セクション内 Cell 間の bottom separator** → 可視（`.visible`）、`bottomSeparatorInsets.leading = 16pt`（固定）、`bottomSeparatorInsets.trailing = 0`

セクション内中間 Cell の bottom separator のインセット幅は、**アイコンの有無に関わらず固定 16pt（標準左マージン）** とする (MUST)。

**Rationale**: AiForms.Maui.SettingsView オリジナル（参照: `AiForms.Maui.SettingsView/Platforms/iOS/`）のスクリーンショットで、アイコン有り Cell（例: Storage）が並ぶセクションと、アイコン無し Cell（例: Favorites / Switch / Checkbox）が並ぶセクションのいずれも、Cell 間の罫線が同じ 16pt 程度のインセットで揃っていることが確認されている。これに合わせ、本実装でも「動的 titleLeadingPosition（アイコン有り → 52pt）」のロジックは採用しない。

#### Scenario: セクション境界の罫線（端から端）

- **GIVEN** 1 つのセクションが 3 つの Cell を持ち、その前後に別 Section が存在する
- **WHEN** iOS で描画される
- **THEN** セクション最初の Cell の上罫線とセクション最後の Cell の下罫線は、画面の端から端まで描画される（leading inset = 0）

#### Scenario: セクション内 Cell 間の罫線（固定 16pt）

- **GIVEN** アイコン無しの 3 つの Cell が並ぶセクション
- **WHEN** iOS で描画される
- **THEN** Cell 間の下罫線は左 16pt のインセットを持って描画される

#### Scenario: アイコン有り Cell の罫線（固定 16pt）

- **GIVEN** アイコン有り（24×24pt）の Cell が並ぶセクション
- **WHEN** iOS で描画される
- **THEN** Cell 間の下罫線は左 16pt のインセットを持って描画される（AiForms オリジナルと同じくアイコンの有無に関わらず同位置）

#### Scenario: アイコン有り / 無し混在セクションの罫線（全て 16pt）

- **GIVEN** 1 つのセクションにアイコン有り Cell とアイコン無し Cell が混在して並ぶ
- **WHEN** iOS で描画される
- **THEN** セクション内中間 Cell の bottom separator はすべて `leading = 16pt` で揃って描画され、Cell ごとにインセット幅が変動してはならない

#### Scenario: 単一 Cell のセクション

- **GIVEN** 1 つの Cell のみを持つセクション
- **WHEN** iOS で描画される
- **THEN** その Cell は「セクション最初かつ最後」となり、上下両方の罫線が端から端まで描画される

#### Scenario: アイコンあり Cell のセクション境界での罫線（端から端）

- **GIVEN** アイコン有り（24×24pt）の Cell がセクション最初に配置されている
- **WHEN** iOS で描画される
- **THEN** その Cell の **上罫線** は `topSeparatorInsets.leading = 0` で描画され、アイコン下の領域も含めて画面の端から端まで罫線が途切れずに描画される

#### Scenario: アイコンあり Cell のセクション末尾での罫線（端から端）

- **GIVEN** アイコン有り Cell がセクション末尾に配置されている
- **WHEN** iOS で描画される
- **THEN** その Cell の **下罫線** は `bottomSeparatorInsets.leading = 0` で描画され、アイコン下の領域も含めて端から端まで描画される

### Requirement: Section Footer の文字色フォールバック

`settings-view-ios-ui` の Section Footer 描画（supplementary 経路）は、`Theme.footerTextColor` の値をそのまま `UIColor` に変換して使用しなければならない (MUST)。Footer 文字色はあくまで `Theme.footerTextColor` の責務であり、UI 層側で追加の dynamic color フォールバック（`UIColor.secondaryLabel` 等）への分岐は行わない (MUST NOT)。

`Theme.footerTextColor` の既定値は `Theme.defaultFooterTextColor = KsColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)`（およそ `#6D6D72`、`UIColor.secondaryLabel` のライトモード値に近い固定グレー）であり、明示指定がない場合はこの既定値（固定 RGB のグレー）が Footer のテキスト色として使用される。`Theme.footerTextColor` が明示指定されている場合はその色を優先する。

**Rationale**: AiForms.Maui.SettingsView オリジナル `Platforms/iOS/SettingsTableSource.cs` の Footer 描画ロジックは `_settingsView.FooterTextColor.IsDefault() ? UIColor.Gray : ...` で、こちらも dynamic color ではなく `UIColor.Gray` 相当の固定 RGB のグレーを採用している。本実装はこれに合わせて `Theme.defaultFooterTextColor` の固定 RGB をそのまま使用する方針とする。ダークモード時の dynamic color（`UIColor.secondaryLabel`）への自動切り替え対応は、本変更提案のスコープ外とする（将来的な拡張余地として、必要になった時点で `Theme.footerTextColor` の型を Optional 化する等の別 change で扱う）。

#### Scenario: footerTextColor 未指定時の defaultFooterTextColor 適用

- **GIVEN** `Theme(footerTextColor: 未指定 = Theme.defaultFooterTextColor, ...)` を適用した SettingsView
- **WHEN** Section Footer がテキストとして描画される
- **THEN** Footer ラベルの `textColor` は `KsColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)` 相当の固定グレー（`#6D6D72` 相当、`UIColor.secondaryLabel` のライトモード色に近い）で描画される。accent 色や `headerTextColor` 由来の色にはならない

#### Scenario: footerTextColor 明示指定時の優先

- **GIVEN** `Theme(footerTextColor: KsColor(red: 0.6, green: 0.6, blue: 0.6, alpha: 1.0), ...)`
- **WHEN** Section Footer がテキストとして描画される
- **THEN** Footer ラベルの `textColor` は指定された KsColor で描画され、既定値の `defaultFooterTextColor` は適用されない

### Requirement: LabelCell の description と valueText の並列描画

`settings-view-ios-ui` の `LabelCellView`（`applyLabelCellContents` 経由で `CommandCellView` も含む）は、`description` と `valueText` が **同時に指定されている場合に両方を表示**しなければならない (MUST)。Android 側の `[icon][title / description][valueText (右寄せ)]` レイアウトと視覚的に同等になるよう、`description` は title 下に配置し、`valueText` は trailing 側にラベル accessory として配置する。

- `description` のみ → `subtitleCell()` で title 下に description を表示する。
- `valueText` のみ → `valueCell()` で title 右側に valueText を表示する。
- `description` と `valueText` の両方 → `subtitleCell()` で title 下に description を表示し、加えて `UICellAccessory.label`（または `customView` 経由の右寄せラベル）で valueText を trailing に配置する。

#### Scenario: description のみ

- **GIVEN** `LabelCell(title: "Storage", description: "本体ストレージ")`
- **WHEN** iOS で描画される
- **THEN** title 下に description が 1 行で表示される

#### Scenario: valueText のみ

- **GIVEN** `LabelCell(title: "バージョン", valueText: "1.0.0")`
- **WHEN** iOS で描画される
- **THEN** title の右側に valueText "1.0.0" が表示される

#### Scenario: description と valueText の両方

- **GIVEN** `LabelCell(title: "Storage", description: "This is description. ...", valueText: "256 GB", icon: KsImage.systemName("externaldrive"))`
- **WHEN** iOS で描画される
- **THEN** 左にアイコン、中央上段に title「Storage」、中央下段に description「This is description. ...」、右端に valueText「256 GB」が同時表示され、Android 側のレイアウトと視覚的に一致する

### Requirement: Section Header / Footer の垂直配置

`settings-view-ios-ui` の Section Header / Footer 描画（supplementary 経路）は、AiForms.Maui.SettingsView オリジナル `Platforms/iOS/TextHeaderView.cs` の挙動（`HeaderTextVerticalAlign` 既定 = `LayoutAlignment.End`）に揃え、以下の垂直配置を実装しなければならない (MUST)。

- **Section Header** のテキストは boundary supplementary item の **下端揃え（bottom alignment）** で描画する (MUST)。テキストが中央や上端に配置されてはならない (MUST NOT)。
- **Section Footer** のテキストは boundary supplementary item の **上端揃え（top alignment）** で描画する (MUST)。テキストが中央や下端に配置されてはならない (MUST NOT)。

実装手段は問わないが、`UIListContentConfiguration.cell()` の既定挙動（中央揃え）では bottom / top の配置を取れないため、**UIView + UILabel + AutoLayout 制約** で Header / Footer 用 supplementary view を構築するか、同等の手段を採る。

**Rationale**: AiForms.Maui.SettingsView オリジナルでは `TextHeaderView` が `SetVerticalAlignment(LayoutAlignment.End)` で Label を ContentView の bottom anchor に揃え、`TextFooterView` は top anchor に揃える設計になっている。これは「Header テキストは次の Cell の直上にぴったり接する」「Footer テキストは前の Cell の直下にぴったり接する」という視覚効果を出すための意図的な配置であり、本実装もこれに合わせる。

#### Scenario: Section Header の下揃え

- **GIVEN** `Section(header: SectionAccessory.text("CommandCell"), headerHeight: 60, ...)`
- **WHEN** iOS で描画される
- **THEN** Header の "CommandCell" テキストは固定 60pt の supplementary 領域の **下端**に張り付くように配置され、その下に続く最初の Cell との余白が最小化される

#### Scenario: Section Footer の上揃え

- **GIVEN** `Section(footer: SectionAccessory.text("You can select either TypeA or TypeB."), ...)`
- **WHEN** iOS で描画される
- **THEN** Footer の説明テキストは supplementary 領域の **上端**に張り付くように配置され、その上の最後の Cell との余白が最小化される

#### Scenario: headerHeight = -1 自動高さでも下揃え

- **GIVEN** `Section(header: SectionAccessory.text("LabelCell"), headerHeight: -1, ...)`（自動高さ）
- **WHEN** iOS で描画される
- **THEN** Header の "LabelCell" テキストは（テキスト自然高さに応じた supplementary の中で）下端揃えで描画され、中央揃え・上端揃えにはならない

### Requirement: KsImage.uiImage 派生の解決

`settings-view-ios-ui` は `KsImage` の `systemName(String)` 派生と `uiImage(UIImage)` 派生の両方を解決して `UIImageView.image` に設定しなければならない (MUST)。具体的には：

- `KsImage.systemName(name)` → `UIImage(systemName: name)` を取得し設定する。取得失敗時はアイコン非表示（`isHidden = true`）にフォールバック
- `KsImage.uiImage(image)` → `image` をそのまま設定する

#### Scenario: systemName 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に `UIImage(systemName: "bell")` が描画される

#### Scenario: uiImage 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.uiImage(UIImage(named: "custom_icon")!))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に渡された UIImage がそのまま描画される

#### Scenario: systemName 不正名のフォールバック

- **GIVEN** `LabelCell(icon: KsImage.systemName("non_existent_symbol_xyz"))`
- **WHEN** iOS で描画される
- **THEN** `UIImage(systemName:)` が `nil` を返すため、Cell のアイコン領域は非表示にフォールバックし、Title が左寄せに配置される

### Requirement: Cell.cellHeight の UI 反映（Phase 17 追加対応）

iOS の全 Cell View は、`CellStyle.cellHeight`（または `Theme.rowHeight`）が指定された場合、その値を実際の描画 frame.height に反映 SHALL する。
`UICollectionViewListCell` の self-sizing（`UIListContentConfiguration` の intrinsic 高さ）だけでは
指定値が反映されないケースがあるため、各 Cell View は共通基底クラス `KsListCellBase` を継承し、
`preferredLayoutAttributesFitting(_:)` を override して proposed attributes の `size.height` を補正 MUST する。
補正は以下の規則に従う：

- `Theme.hasUnevenRows == false`（固定高さモード）→ `proposed.size.height` を無視して **厳密に** `effectiveCellHeight` に揃える。
- `Theme.hasUnevenRows == true`（可変高さモード）→ `effectiveCellHeight` を **下限** として `max(proposed.size.height, effectiveCellHeight)` を採用する。

`KsCellViewSupport.applyEffectiveHeight(_:effective:)` で記録された
`lastHeight` / `lastIsFixedHeight` を `KsCellViewSupport.adjustedLayoutAttributes(_:proposed:)` 経由で参照する。

参照: AiForms.Maui.SettingsView オリジナル `Native/iOS/SettingsTableSource.cs` lines 113-135
（`GetHeightForRow` が `cell.Height` の `NFloat` を直接返し、`UITableView` の rect 計算に反映される設計）。
`UICollectionView` 系では `preferredLayoutAttributesFitting(_:)` がそれに相当する経路となる。

#### Scenario: cellHeight 80 指定時にセルの描画高さが 80pt 以上になる

- **GIVEN** `Theme(hasUnevenRows: true)` の SettingsRoot に `CommandCell(style: CellStyle(cellHeight: 80), title: "Tanaka Taro", description: "tanaka.taro@example.com")` を含むセクションが定義されている
- **WHEN** `UICollectionView.layoutIfNeeded()` 後に当該セルの `cellForItem(at:)` を取得する
- **THEN** 取得したセルの `frame.height` は 80pt 以上（許容誤差 ±0.5pt）でなければならない

#### Scenario: cellHeight 120 指定時の任意指定値反映

- **GIVEN** `Theme(hasUnevenRows: true)` の SettingsRoot に `CommandCell(style: CellStyle(cellHeight: 120), title: "Tall Row")` を含むセクションが定義されている
- **WHEN** `UICollectionView.layoutIfNeeded()` 後に当該セルの `cellForItem(at:)` を取得する
- **THEN** 取得したセルの `frame.height` は 120pt 以上（許容誤差 ±0.5pt）でなければならない（80pt 偶然マッチではなく、任意の指定値が反映される回帰保証）

#### Scenario: cellHeight 未指定時の標準動作

- **GIVEN** `CommandCell(title: "プロフィール")`（`style.cellHeight == nil`、`Theme.rowHeight == -1`）が定義されている
- **WHEN** iOS で描画される
- **THEN** `effectiveCellHeight` は `EffectiveStyle.minRowHeight`（48pt）に下限ガードされ、`UIListContentConfiguration` の intrinsic 高さが 48pt 以上であればその intrinsic 値が採用される（可変高さモード）

### Requirement: Cell View 共通基底クラスの導入（Phase 17 追加対応）

iOS の全 Cell View（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）は、共通基底クラス `KsListCellBase: UICollectionViewListCell` を継承 SHALL する。
共通基底クラスは以下を担う：

1. `init(frame:)` で `KsCellViewSupport.installSelectedColorHandler(self)` を呼び出してタッチフィードバックを登録する。
2. `preferredLayoutAttributesFitting(_:)` を override し、`KsCellViewSupport.adjustedLayoutAttributes(self, proposed:)` 経由で `cellHeight` 反映を行う。

これにより、各具象 Cell View は `cellHeight` 反映ロジックを個別に実装する必要がなく、保守性が確保される。

#### Scenario: 全 Cell View が KsListCellBase を継承する

- **GIVEN** iOS の全 Cell View（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）が定義されている
- **WHEN** 各 Cell View のクラス階層を確認する
- **THEN** すべて `KsListCellBase: UICollectionViewListCell` を継承し、独自の `init(frame:)` 内で `installSelectedColorHandler` を再度呼ぶことはしない（基底クラスが既に呼ぶため）
