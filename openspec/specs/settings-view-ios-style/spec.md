# settings-view-ios-style Specification

## Purpose

`settings-view-ios-style` は、`KsSettingsViewUI`（iOS）の **スタイル・レイアウト層**（`UICollectionViewCompositionalLayout` / `UICollectionLayoutListConfiguration` の構成・`KsSettingsViewStyle` クラシック/モダン切替・Section H/F と Root H/F の描画・Section Header/Footer の sticky 抑止と垂直配置・罫線インセット規則・headerHeight と空 supplementary の非生成・余白最小化・`KsListCellBase` を含む Cell View 共通基底・`Cell.cellHeight` の UI 反映）を担う capability である。`settings-view-ios-host` が提供する `UICollectionViewDiffableDataSource` 基盤と Cell ViewHolder/Renderer 抽象の上で、最終的な見た目を組み立てる責務を持つ。`Theme` / `CellStyle` 値をプラットフォーム値に変換する責務は `settings-view-ios-theme-bridge` に分離されており、本 capability はその変換結果を消費する立場である。

## Requirements
### Requirement: UICollectionView のレイアウト

UI は `UICollectionLayoutListConfiguration`（iOS 14+）を `UICollectionViewCompositionalLayout` の `.list` で構成しなければならない (SHALL)。Cell の高さは `estimatedItemSize = .automatic` で Auto Layout により決定されなければならない (MUST)。

ただし `Theme.hasUnevenRows == false` のとき、各 Cell は **固定高さ** で描画されなければならない (MUST)。固定高さ値は `max(Theme.rowHeight, MinRowHeight)` で算出する（`Theme.rowHeight == -1` のときは `MinRowHeight` を採用）。`MinRowHeight` は `48` ポイントとする (MUST)。固定高さは Cell View の `contentView` に対し `heightAnchor.constraint(equalToConstant: effectiveHeight)` で適用する。

`Theme.hasUnevenRows == true` のとき、各 Cell は Auto Layout による可変高さでありつつ、`contentView.heightAnchor.constraint(greaterThanOrEqualToConstant: effectiveMinHeight)` で **最低高さ保証** を付与しなければならない (MUST)。`effectiveMinHeight` は同様に `max(Theme.rowHeight, MinRowHeight)` とする。さらに個別 Cell の `CellStyle.cellHeight` が指定されている場合は、その値を優先（固定高さ・最低高さの双方で個別値を採用）しなければならない (MUST)。

`Theme.hasUnevenRows` のデフォルト値は **`true`**（Auto 高さ + 下限保証）とする (MUST)。これにより、`Theme()` を引数なしで構築した既定状態では各 Cell が内容に応じて自然に伸縮する。「全 Cell を一律固定高さで揃えたい」用途では、利用者が `Theme(hasUnevenRows: false)` を明示指定することで従来の固定高さモードを選べる。デフォルト `true` 化はオリジナル `AiForms.Maui.SettingsView` の `AiTableView`（`RowHeight = UITableView.AutomaticDimension` + `MinRowHeight = 48`）の挙動踏襲である。

#### Scenario: List 設定の使用

- **GIVEN** `KsSettingsViewController` が初期化済み
- **WHEN** `view.subviews` に含まれる `UICollectionView` のレイアウトを取得する
- **THEN** 取得したレイアウトは `UICollectionViewCompositionalLayout` であり、内部設定は List ベースである

#### Scenario: 区切り線とヘッダ・フッタ

- **GIVEN** `Section` に `header` が `SectionAccessory.text("一般")` で指定されている
- **WHEN** Cell が描画される
- **THEN** `UICollectionLayoutListConfiguration.headerMode = .supplementary` 等を用いてヘッダ領域に "一般" が表示され、`Theme.separatorColor` で区切り線色が設定される

#### Scenario: 固定高さ（HasUnevenRows = false）

- **GIVEN** `Theme(rowHeight: 60, hasUnevenRows: false)` で初期化された `KsSettingsView`、複数 Cell が並ぶ
- **WHEN** 表示される
- **THEN** すべての Cell の高さが `60pt`（`MinRowHeight = 48pt` より大きいので 60 を採用）に固定される。長文 Description が含まれる場合は省略される

#### Scenario: 可変高さ（HasUnevenRows = true、新デフォルト）

- **GIVEN** `Theme(rowHeight: -1)` で初期化された `KsSettingsView`（`hasUnevenRows` は新デフォルト `true` が適用される）、長文 Description を持つ Cell と単行 Cell が混在
- **WHEN** 表示される
- **THEN** 各 Cell は最低高さ `48pt`（`MinRowHeight`）を保証しつつ、内容に応じて伸縮する（長文 Cell は 48pt より高くなり、単行 Cell は 48pt 固定）。`Theme()` を引数なしで構築しても同じ振る舞いとなる

#### Scenario: CellStyle.cellHeight の優先

- **GIVEN** `Theme(rowHeight: 44, hasUnevenRows: true)` と `CellStyle(cellHeight: 80)` を持つ特定 Cell
- **WHEN** 表示される
- **THEN** 当該 Cell の最低高さは `80pt`（`CellStyle.cellHeight` 優先）となる。他 Cell は `44pt`（または `MinRowHeight = 48pt` のうち大きい方）保証

### Requirement: スタイル切替（クラシック/モダン）

`KsSettingsViewController` は `public var style: KsSettingsViewStyle` プロパティを持たなければならない (SHALL)。`KsSettingsViewStyle` は `.classic`（旧 AiForms 互換のフラットな見た目）と `.modern`（最新 OS 設定画面風の角丸グルーピング）の 2 ケースを持つ enum でなければならない (MUST)。`style` の変更時は内部 `UICollectionView` のレイアウトを再構築しなければならない (MUST)。

classic スタイル（`.plain` Appearance）では、オリジナル `AiForms.Maui.SettingsView` 互換の挙動として、セクションヘッダーをスクロール上端に固定してはならない (MUST NOT)。すなわち header supplementary の `pinToVisibleBounds` は `false` 相当でなければならず、ヘッダーはコンテンツと共にスクロールアウトしなければならない (MUST)。

また、footer（`Section.footer`）を持たないセクションに対して、空のフッター supplementary 領域（グレーの帯）を表示してはならない (MUST NOT)。`UICollectionLayoutListConfiguration.footerMode` は「root 内に footer を持つセクションが 1 つでも存在するか」で決定し、いずれのセクションも footer を持たない場合は `.none` としてフッター領域を生成してはならない (MUST NOT)。

#### Scenario: classic スタイルの Appearance

- **GIVEN** `KsSettingsViewController(style: .classic)` を初期化
- **WHEN** 内部 `UICollectionView` のレイアウト設定を取得する
- **THEN** `UICollectionLayoutListConfiguration.appearance` が `.plain` に設定されている

#### Scenario: classic スタイルでヘッダーが固定されない

- **GIVEN** `KsSettingsViewController(style: .classic)` で複数セクション（各セクションにヘッダーあり）を表示している
- **WHEN** コンテンツを下方向にスクロールする
- **THEN** セクションヘッダーは画面上端に固定されず、コンテンツと共にスクロールアウトする（`pinToVisibleBounds = false` 相当）

#### Scenario: footer を持たないセクションに空フッター帯が出ない

- **GIVEN** `KsSettingsViewController(style: .classic)` で、いずれのセクションも `footer` を持たない `root` を表示している
- **WHEN** レイアウトを構築する
- **THEN** `footerMode` は `.none` となり、各セクション下部に空のフッター supplementary 領域（グレーの帯）が表示されない

#### Scenario: footer を持つセクションがある場合は footer を描画する

- **GIVEN** `KsSettingsViewController(style: .classic)` で、一部のセクションが `footer` を持つ `root` を表示している
- **WHEN** レイアウトを構築する
- **THEN** `footerMode` は `.supplementary` となり、`footer` を持つセクションにのみ意味のあるフッターが描画される（`footer` を持たないセクションには空の帯が現れない）

#### Scenario: modern スタイルの Appearance

- **GIVEN** `KsSettingsViewController(style: .modern)` を初期化
- **WHEN** 内部 `UICollectionView` のレイアウト設定を取得する
- **THEN** `UICollectionLayoutListConfiguration.appearance` が `.insetGrouped` に設定されている

#### Scenario: 動的なスタイル切替

- **GIVEN** `KsSettingsViewController(style: .classic)` が画面表示中
- **WHEN** `controller.style = .modern` を代入する
- **THEN** 内部レイアウトが `.insetGrouped` ベースで再構築され、既存の `root` スナップショットがそのまま再描画される（差分アニメーションは発生しない）

#### Scenario: SwiftUI ラッパでのスタイル指定

- **GIVEN** SwiftUI で `KsSettingsView(root: $root, style: .modern)` を記述
- **WHEN** `makeUIViewController(context:)` が呼ばれる
- **THEN** 生成された `KsSettingsViewController` の `style` が `.modern` で初期化される

### Requirement: Section H/F（SectionAccessory）の描画

`KsSettingsViewController` は `SectionAccessory.text(String)` 形式のヘッダ／フッタを `UICollectionLayoutListConfiguration` の supplementary header / footer として文字列で描画しなければならない (SHALL)。`SectionAccessory.view(KsAnyView)` 形式は `UICollectionViewListCell.contentConfiguration` を `UIHostingConfiguration { ... }`（SwiftUI backing）または `addSubview`（UIView backing）で構成して描画しなければならない (MUST)。

#### Scenario: text 形式ヘッダの描画

- **GIVEN** `Section(header: .text("一般"), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view にテキスト "一般" が描画される

#### Scenario: view 形式ヘッダ（SwiftUI backing）の描画

- **GIVEN** `Section(header: .view(KsAnyView.swiftUI { ProfileCardView() }), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view の `contentConfiguration` が `UIHostingConfiguration` で構成され、`ProfileCardView` の中身が描画される

#### Scenario: view 形式ヘッダ（UIView backing）の描画

- **GIVEN** `Section(header: .view(KsAnyView.uiKit { MyCustomUIView() }), ...)` を含む `SettingsRoot`
- **WHEN** `controller.root` に代入する
- **THEN** ヘッダ supplementary view に `MyCustomUIView` インスタンスが addSubview され、可視描画される

#### Scenario: view 形式ヘッダの中身更新（差分検出非対応）

- **GIVEN** `controller.root` に `.view(KsAnyView.swiftUI { Counter(value: 1) })` を含む root を代入
- **WHEN** `.view(KsAnyView.swiftUI { Counter(value: 2) })` を含む root に置き換える
- **THEN** `KsAnyView` は差分検出に参加しないため supplementary view 自体の生成・破棄は走らないが、`contentConfiguration` の再構成によって `Counter(value: 2)` の中身が再描画される

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsViewController` は `rootHeader: RootAccessory?` および `rootFooter: RootAccessory?` を UI 層プロパティとして持ち、`UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header"` / `"ks-root-footer"`、`alignment: .top` / `.bottom` で配置しなければならない (SHALL)。デフォルトで `pinToVisibleBounds = false`（スクロール追従）でなければならない (MUST)。`RootAccessory.text` ケースは `UIListContentConfiguration` ベースの文字列描画、`RootAccessory.view(KsAnyView)` ケースは `UIHostingConfiguration` または `addSubview` で描画しなければならない (MUST)。`rootHeader` / `rootFooter` が `nil` の場合は対応する supplementary item を boundary から省略しなければならない (MUST)。

`SettingsRoot` 値型自体には `header` / `footer` を含まないため (MUST NOT)、本 Requirement の入力は UI 層プロパティ（`controller.rootHeader` の代入、SwiftUI ラッパの `.header(...)` modifier、または `SettingsRootStore.updateAccessory(target: .rootHeader, accessory:)` Diff 経由）のみとする。

<!-- 注: `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、Root H/F の入力源を UI 層プロパティ（`KsSettingsViewController.rootHeader` / `rootFooter`、SwiftUI ラッパの `.header(...)` / `.footer(...)` modifier、`SettingsRootStore.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff 経由）に変更している。boundary supplementary item 配置・描画ロジック自体は維持される。Requirement 名は archive 済 spec との連続性を保つため変更しないが、説明文と Scenario は新 API に合わせて書き直している。 -->

#### Scenario: Root Header（text）の描画

- **GIVEN** `controller.rootHeader = .text("プロフィール")` を代入
- **WHEN** Controller が描画される
- **THEN** UICollectionView 上端に "プロフィール" の boundary supplementary view が表示される

#### Scenario: Root Footer（view、SwiftUI backing）の描画

- **GIVEN** `controller.rootFooter = .view(KsAnyView.swiftUI { Text("v1.0.0") })` を代入
- **WHEN** Controller が描画される
- **THEN** UICollectionView 下端に Text("v1.0.0") の boundary supplementary view が描画される

#### Scenario: Root H/F のスクロール追従

- **GIVEN** Root Header を持つ Controller が描画中
- **WHEN** UICollectionView を下方向にスクロールする
- **THEN** Root Header は画面上端に固定されず、コンテンツと共にスクロールアウトする（`pinToVisibleBounds = false` のデフォルト挙動）

#### Scenario: Root H/F が nil の場合

- **GIVEN** `controller.rootHeader = nil` および `controller.rootFooter = nil`
- **WHEN** Controller が描画される
- **THEN** boundary supplementary items は配置されず、既存の sections のみが描画される

#### Scenario: Store 経由の Accessory 更新

- **GIVEN** Store が初期化済み、Controller が `store` を購読中
- **WHEN** `store.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` を呼ぶ
- **THEN** Store が `.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` Diff を発行し、Controller の `applyDiff` が `rootHeader` を `.text("X")` に更新する

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

### Requirement: backgroundColor のセクション間反映

`settings-view-ios-ui` は `Theme.backgroundColor` を `UICollectionView` の背景色として設定する際、`UICollectionLayoutListConfiguration.backgroundColor` を `.clear` に設定しなければならない (MUST)。これにより `UICollectionView.backgroundColor` がセクション間の隙間（supplementary 領域・section inset）にも透過して反映され、`Theme.backgroundColor` が見える状態になる。

本 Requirement で参照する `Theme.backgroundColor` は、`port-theme-and-cellstyle-missing-fields` change で **`Theme.viewBackgroundColor` からリネーム** されたフィールドである（旧名 `viewBackgroundColor` は互換シムなしで削除されており、本 Requirement の本文・Scenario のいずれも旧名を参照してはならない (MUST NOT)）。

#### Scenario: backgroundColor がセクション間にも反映される

- **GIVEN** `Theme(backgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0), cellBackgroundColor: .white)` を適用した SettingsView
- **WHEN** iOS で描画される
- **THEN** 各 Cell の背景は `cellBackgroundColor` の白、Section 間（Header / Footer 領域および Section inset）の背景は `backgroundColor` の薄ベージュ色が反映される

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



### Requirement: Theme 型 (UI 層)

`KsSettingsViewUI` モジュールは、SettingsView 全体に適用される論理スタイルを保持する値型 `Theme` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- 全体背景・装飾: `separatorColor`、`backgroundColor`、`cellBackgroundColor`、`selectedColor`、`cellAccentColor`、`disabledTextColor`、`scrollIndicatorVisible`
- 行高さ: `rowHeight`、`hasUnevenRows`
- Header: `headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`headerFont`、`headerHeight`
- Footer: `footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`footerFont`
- Cell 全体既定: `cellTitleColor`、`cellTitleFont`、`cellTitleFontSize`、`cellValueTextColor`、`cellValueTextFont`、`cellDescriptionColor`、`cellDescriptionFont`、`cellHintTextColor`、`cellHintFont`、`cellIconSize`、`cellIconRadius`

**フィールド型は UIKit Native 型 `UIColor` / `UIFont` / `CGFloat` を直接保持しなければならない (MUST)。** `KsColor` / `KsFont` などの中間論理表現を経由してはならない (MUST NOT)。

#### リネーム

**従前の `viewBackgroundColor` は `backgroundColor` にリネームされる (MUST)**。互換シム（旧名 deprecated 残し）を提供してはならない (MUST NOT)。同様に、**従前の `titleColor` は `cellTitleColor` にリネームされる (MUST)** ことで、オリジナル `AiForms.Maui.SettingsView.SettingsView.CellTitleColor` と命名整合する。`titleFont` も同じ整合のため `cellTitleFont` にリネームする (MUST)。

#### Cell タイトル系

`cellTitleColor` は Cell タイトルの既定色を表す Optional `UIColor?` でなければならない (MUST)。未指定（`nil`）のとき UI 層は `UIColor.label` にフォールバックする。

`cellTitleFont` は Cell タイトルの既定フォントを表す Optional `UIFont?` でなければならない (MUST)。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .body)` にフォールバックする。

`cellTitleFontSize: Double` は Cell タイトルの **既定フォントサイズ単独** を表す独立フィールドで、既定値は `-1.0`（未指定）でなければならない (MUST)。`cellTitleFont` と `cellTitleFontSize` が両方非 nil / `-1.0` 以外のとき、**`cellTitleFontSize` を size として優先**しなければならない (MUST)。すなわち最終的に描画される size は `cellTitleFontSize > 0 ? CGFloat(cellTitleFontSize) : cellTitleFont.pointSize` となる。

#### Cell 説明・値・ヒント・アイコン系（新規追加）

`cellValueTextColor: UIColor?` は LabelCell / CommandCell の valueText（および description / valueText を持つ後続 Cell）の **全体既定色** を表す。未指定（`nil`）のとき UI 層は `Theme.cellTitleColor` または `UIColor.label` にフォールバックする。

`cellValueTextFont: UIFont?` は valueText の **全体既定フォント** を表す。未指定のとき UI 層は `Theme.cellTitleFont` または `UIFont.preferredFont(forTextStyle: .body)` にフォールバックする。

`cellDescriptionColor: UIColor?` は description の **全体既定色** を表す。未指定のとき UI 層は `UIColor.secondaryLabel` 相当（`UIColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)` ライトモード固定色）にフォールバックする。

`cellDescriptionFont: UIFont?` は description の **全体既定フォント** を表す。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .footnote)` にフォールバックする。

`cellHintTextColor: UIColor?` は hintText の **全体既定色** を表す。未指定のとき UI 層は accent 色相当（`Theme.cellAccentColor`）にフォールバックする。

`cellHintFont: UIFont?` は hintText の **全体既定フォント** を表す。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .footnote)` にフォールバックする。

`cellIconSize: CGFloat?` は icon の **全体既定サイズ**（正方形の一辺 pt）を表す。未指定のとき UI 層は既定（24pt）にフォールバックする。`CellStyle.iconSize: CGFloat?` と型を一致させ、`EffectiveStyle.effectiveIconSize` の結果も `CGFloat`（一辺）を返すことで「icon は正方形」というアイコン表現の前提を spec レベルで揃える。オリジナル `AiForms.Maui.SettingsView.SettingsView.CellIconSize`（`Size` 型）に対しては、本実装では「`Width` と `Height` のうち大きい方を使うか、Width のみを採用する」とは限定せず、`CellStyle.iconSize` 設計に従って **一辺スカラー** に簡素化する。

`cellIconRadius: CGFloat?` は icon の **全体既定角丸半径** を表す。未指定のとき UI 層は既定（0pt = 角丸なし）にフォールバックする。

#### Header / Footer Font 系（新規追加）

`headerFont: UIFont?` は Section Header の **全体既定フォント**（family / weight / 装飾を含む）を表す Optional フィールドでなければならない (MUST)。未指定のとき UI 層は既存 `headerFontSize` のみで描画する。`headerFontSize > 0` かつ `headerFont != nil` のとき、**`headerFontSize` を size として優先**する (MUST)。

`footerFont: UIFont?` は Section Footer の **全体既定フォント** を表す。挙動は `headerFont` と同じく `footerFontSize` 優先である (MUST)。

`headerHeight: Double` は SettingsView 全体に適用される Section Header の **既定高さ**（論理単位 = pt）を表し、既定値は `-1.0`（未指定 = 自動）でなければならない (MUST)。Section ごとの `Section.headerHeight` が `-1.0` のときは本値を採用する。

#### 既存維持

`backgroundColor` は SettingsView（`UICollectionView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した色でなければならない (MUST)。

`rowHeight: Int` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（iOS 48pt）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える Bool でなければならない (SHALL)。**デフォルト値は `true`** とする (MUST)。これによりオリジナル `AiForms.Maui.SettingsView` の `AiTableView`（`RowHeight = UITableView.AutomaticDimension` + `MinRowHeight = 48`）の「Auto 高さ + 下限保証」既定挙動と整合する。「全 Cell を一律固定高さで揃えたい」用途では利用者が `Theme(hasUnevenRows: false)` を明示指定する。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `UIColor` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、Double）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Swift `struct` として `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` および `UIFont` は Swift の `Equatable` に準拠していないためコンパイラによる自動合成は不可能 (MUST NOT)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)`、`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` を用いた手動 `==` / `!=` 実装が必須 (MUST)。`Hashable` 準拠は必須としない。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（パラメータなし init）
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`backgroundColor` は `UIColor.systemBackground` 系または白系、`rowHeight = -1`、`hasUnevenRows = true`、`headerFontSize = -1`、`footerFontSize = -1`、`headerHeight = -1.0`、`disabledTextColor` はやや薄い灰色、`cellTitleColor = nil`、`cellTitleFont = nil`、`cellTitleFontSize = -1.0`、新規フィールド（`cellValueTextColor` / `cellValueTextFont` / `cellDescriptionColor` / `cellDescriptionFont` / `cellHintTextColor` / `cellHintFont` / `cellIconSize` / `cellIconRadius` / `headerFont` / `footerFont`）はすべて `nil`

#### Scenario: viewBackgroundColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().viewBackgroundColor` を参照する Swift コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（旧名は完全に削除され、互換シムも提供されない）

#### Scenario: titleColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().titleColor` を参照する Swift コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（`cellTitleColor` への書き換えが必須）

#### Scenario: cellTitleColor / cellTitleFont の Optional 性

- **GIVEN** `Theme()` の `cellTitleColor` / `cellTitleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも Optional（`UIColor?` / `UIFont?`）であり、既定値は `nil` である

#### Scenario: cellTitleFontSize 既定値

- **GIVEN** `Theme()` の `cellTitleFontSize` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定」を表す

#### Scenario: cellTitleFontSize と cellTitleFont 併設時の size 優先

- **GIVEN** `Theme(cellTitleFont: UIFont.systemFont(ofSize: 14), cellTitleFontSize: 20.0)`
- **WHEN** UI 層が Cell タイトルを描画する
- **THEN** 最終的な pointSize は **20.0pt**（`cellTitleFontSize` 優先）で描画され、`cellTitleFont` の pointSize 14 は無視される。family / weight など `cellTitleFont` の他属性は維持される

#### Scenario: 新規 Cell 全体既定フィールドの保持

- **GIVEN** `Theme(cellHintTextColor: UIColor.red, cellIconSize: 32.0)`
- **WHEN** 値を参照する
- **THEN** `cellHintTextColor` は `UIColor.red`、`cellIconSize` は `32.0`（一辺 pt）を返す

#### Scenario: headerHeight 既定値

- **GIVEN** `Theme()` の `headerHeight` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定 = 自動」を表す

#### Scenario: backgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(backgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0), cellBackgroundColor: .white)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色（セル間／セクション間に見える背景）は `backgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

#### Scenario: Native 型を直接保持

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `KsColor` や独自論理色型を経由せず、`UIColor` / `UIFont` / `CGFloat` を直接保持する

#### Scenario: rowHeight / hasUnevenRows の既定組み合わせ

- **GIVEN** `Theme()`（未指定）
- **WHEN** `theme.rowHeight` と `theme.hasUnevenRows` を参照する
- **THEN** それぞれ `-1` と `true` が返る（UI 層はこの組み合わせを「Auto 高さ + 最低高さ MinRowHeight」と解釈する）

#### Scenario: 利用者が UIColor をそのまま渡せる

- **GIVEN** 利用者コード `Theme(separatorColor: UIColor.systemGray3, cellBackgroundColor: .white)`
- **WHEN** コンパイル・実行する
- **THEN** ビルドエラーなく構築でき、`KsColor` などの中間型を書く必要がない

### Requirement: CellStyle 型 (UI 層)

`KsSettingsViewUI` モジュールは、単一 Cell に適用されるスタイルを表す値型 `CellStyle` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- `titleColor: UIColor?`
- `titleFont: UIFont?`
- `descriptionColor: UIColor?`
- `descriptionFont: UIFont?`
- `valueTextColor: UIColor?`
- `valueTextFont: UIFont?`
- `iconSize: CGFloat?`
- `iconRadius: CGFloat?`
- `cellHeight: CGFloat?`
- `hintTextColor: UIColor?`
- `hintTextFont: UIFont?`
- `backgroundColor: UIColor?`
- `accentColor: UIColor?`

**色・フォント系フィールドは `UIColor?` / `UIFont?` を直接保持しなければならない (MUST)。`iconSize` / `iconRadius` / `cellHeight` は `CGFloat?` でなければならない (MUST)。**

CellStyle のフィールドはいずれも `nil` を取りうる Optional であり、`nil` のとき UI 層は **`Theme` の対応する全体既定フィールド**（解決順序: `Theme.cellTitleColor` / `Theme.cellTitleFont` / `Theme.cellTitleFontSize` / `Theme.cellValueTextColor` / `Theme.cellValueTextFont` / `Theme.cellDescriptionColor` / `Theme.cellDescriptionFont` / `Theme.cellHintTextColor` / `Theme.cellHintFont` / `Theme.cellIconSize` / `Theme.cellIconRadius` / `Theme.cellBackgroundColor` / `Theme.cellAccentColor`）にフォールバックしなければならない (MUST)。`Theme` 側も未指定の場合は UI 層既定値（UIKit プラットフォーム既定または本 spec の他 Requirement で定義された値）を用いる。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** デフォルトコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`nil`）」となり、`Theme` から継承される

#### Scenario: Native 型を直接保持

- **GIVEN** `CellStyle` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `UIColor?` / `UIFont?` / `CGFloat?` を直接保持する

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor: UIColor.red)` を持つ Cell と `Theme(cellBackgroundColor: UIColor.white)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor: UIColor.green)` を持つ Cell と `Theme(cellAccentColor: UIColor.blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: hintTextColor の Theme フォールバック

- **GIVEN** `CellStyle(hintTextColor: nil)` を持つ Cell と `Theme(cellHintTextColor: UIColor.red)`
- **WHEN** UI 層が実効 hintText 色を計算する
- **THEN** 実効 hintText 色は **赤**（`Theme.cellHintTextColor` から落ちてくる）になる

#### Scenario: iconSize の Theme フォールバック

- **GIVEN** `CellStyle(iconSize: nil)` を持つ Cell と `Theme(cellIconSize: 32.0)`
- **WHEN** UI 層が実効 iconSize を計算する
- **THEN** 実効 iconSize は **`32.0`（一辺 pt）**（`Theme.cellIconSize` から落ちてくる）になる

### Requirement: EffectiveStyle の解決順序

`KsSettingsViewUI` モジュールは、Cell 描画時の最終スタイル値を「CellStyle → Theme → 既定」の 3 段で解決するユーティリティ `EffectiveStyle` を提供しなければならない (SHALL)。`EffectiveStyle` は各 Cell プロパティに対応する **アクセサ関数** を提供し、各 Cell View の描画処理から呼び出されなければならない (MUST)。

解決順序：

```
最終値 = cellStyle.X            if X != nil
       else theme.cellX         if cellX != nil
       else プラットフォーム既定（本 spec の他 Requirement または UI 層内の既定値）
```

`titleFontSize` のみ特殊で、`theme.cellTitleFontSize` が `> 0` の場合は `cellTitleFont.pointSize` を **上書き** する。

EffectiveStyle は以下のアクセサを最低限提供しなければならない (MUST)：

- `effectiveTitleColor(cellStyle, theme) -> UIColor`
- `effectiveTitleFont(cellStyle, theme) -> UIFont`（pointSize は `cellTitleFontSize` で上書きされた最終値）
- `effectiveDescriptionColor(cellStyle, theme) -> UIColor`
- `effectiveDescriptionFont(cellStyle, theme) -> UIFont`
- `effectiveValueTextColor(cellStyle, theme) -> UIColor`
- `effectiveValueTextFont(cellStyle, theme) -> UIFont`
- `effectiveHintTextColor(cellStyle, theme) -> UIColor`
- `effectiveHintFont(cellStyle, theme) -> UIFont`
- `effectiveIconSize(cellStyle, theme) -> CGFloat`（icon は正方形、一辺 pt を返す）
- `effectiveIconRadius(cellStyle, theme) -> CGFloat`
- `effectiveBackgroundColor(cellStyle, theme) -> UIColor`
- `effectiveAccentColor(cellStyle, theme) -> UIColor`
- `effectiveCellHeight(cellStyle, theme) -> CGFloat`（既存）

`ButtonCell.titleColor` のみ特殊で、Cell 個別の `titleColor` フィールドを **最優先** とする 4 段解決を維持する（既存 cell-types-basic spec 規約を尊重）：

```
ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → プラットフォーム既定
```

#### Scenario: 通常 Cell の解決順序（CellStyle 優先）

- **GIVEN** `CellStyle(titleColor: UIColor.red)` を持つ LabelCell と `Theme(cellTitleColor: UIColor.blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.red`**（CellStyle が優先される）

#### Scenario: 通常 Cell の解決順序（Theme フォールバック）

- **GIVEN** `CellStyle(titleColor: nil)` を持つ LabelCell と `Theme(cellTitleColor: UIColor.blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.blue`**（Theme から落ちてくる）

#### Scenario: 通常 Cell の解決順序（既定フォールバック）

- **GIVEN** `CellStyle(titleColor: nil)` を持つ LabelCell と `Theme(cellTitleColor: nil)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.label`**（プラットフォーム既定）

#### Scenario: cellTitleFontSize 優先

- **GIVEN** `CellStyle(titleFont: nil)` を持つ Cell と `Theme(cellTitleFont: UIFont.systemFont(ofSize: 14), cellTitleFontSize: 20.0)`
- **WHEN** `EffectiveStyle.effectiveTitleFont(cellStyle, theme).pointSize` を取得する
- **THEN** `20.0`（`cellTitleFontSize` で pointSize が上書きされる）

#### Scenario: ButtonCell.titleColor の 4 段解決（Cell 個別最優先）

- **GIVEN** `ButtonCell(titleColor: UIColor.red)`、`CellStyle(titleColor: UIColor.green)`、`Theme(cellTitleColor: UIColor.blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`UIColor.red`**（ButtonCell.titleColor が最優先）

#### Scenario: ButtonCell.titleColor が nil の場合は CellStyle 経由

- **GIVEN** `ButtonCell(titleColor: nil)`、`CellStyle(titleColor: UIColor.green)`、`Theme(cellTitleColor: UIColor.blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`UIColor.green`**（CellStyle.titleColor から落ちる）

#### Scenario: UIFont equals の安定性

- **GIVEN** 同一の `UIFont.systemFont(ofSize: 16, weight: .regular)` を渡して構築した 2 つの `Theme` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価判定が真になる（`UIFont.isEqual(_:)` ベースで同じフォントとして扱われる）

#### Scenario: fontFamily 反映の e2e

- **GIVEN** カスタム `UIFont(name: "Avenir-Heavy", size: 18)` を `Theme(cellTitleFont: customFont)` に設定した KsSettingsViewController
- **WHEN** LabelCell を描画する
- **THEN** Cell 内の title `UILabel.font` は `customFont` と `UIFont.isEqual(_:)` で等価で、`fontName` が `"Avenir-Heavy"` を含む（既定 `.SFUI-...` 系にフォールバックしない）

### Requirement: KsImage 型 (UI 層)

`KsSettingsViewUI` モジュールは、Cell のアイコン表現に用いる sealed 値型 `KsImage` を提供しなければならない (SHALL)。`KsImage` は Swift `enum` として定義され、以下のケースを持たなければならない (MUST)：

- `systemName(String)`: SF Symbols 名（例: `"bell"`、`"externaldrive"`）
- `uiImage(UIImage)`: 任意の `UIImage`（カスタムアセット等）

UI 層は派生に応じて以下を行わなければならない (MUST)：

- `.systemName(name)` → `UIImage(systemName: name)` で解決し、取得失敗時はアイコン非表示にフォールバック
- `.uiImage(image)` → `image` をそのまま設定

`KsImage` は `Hashable` プロトコルに準拠しなければならない (MUST)。実装は以下：

- `.systemName(s)`: 内部 String の hash 値で同定
- `.uiImage(img)`: 参照同一性（`ObjectIdentifier(img)`）で同定

#### Scenario: 型の所属

- **GIVEN** `KsImage` の所属モジュール
- **WHEN** import 文を書く
- **THEN** `import KsSettingsViewUI` で `KsImage` を解決できる。`import KsSettingsViewCore` のみでは解決できない

#### Scenario: systemName 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に `UIImage(systemName: "bell")` が描画される

#### Scenario: uiImage 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.uiImage(UIImage(named: "custom_icon")!))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に渡された UIImage がそのまま描画される

#### Scenario: Hashable 契約

- **GIVEN** 同一 `systemName` の 2 つの `KsImage.systemName` インスタンス
- **WHEN** 等価性とハッシュ値を比較する
- **THEN** 等価判定が真、ハッシュ値が一致する

### Requirement: Theme / CellStyle の Hashable / Equatable 契約

`KsSettingsViewUI` 層の `Theme` および `CellStyle` は Swift `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)` ベースで判定する。`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` ベースで判定する。

`Hashable` 準拠は必須としない。SettingsView の構造同期は Core の SettingsRoot id 同一性ベースで行われるため、Theme / CellStyle の Hashable は不要。テストや内部比較で必要な場合のみ手動実装する。

#### Scenario: Theme の Equatable

- **GIVEN** 同一フィールド値を持つ 2 つの `Theme` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価と判定される

#### Scenario: CellStyle の Equatable

- **GIVEN** 同一フィールド値を持つ 2 つの `CellStyle` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価と判定される
