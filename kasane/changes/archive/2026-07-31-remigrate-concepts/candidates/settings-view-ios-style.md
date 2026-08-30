# Candidate: settings-view-ios-style

## 概念候補

### スタイルの所有と実効値解決 (提案カテゴリ: styling/)

iOS の見た目に関する公開値は `KsSettingsViewUI` が所有する。画面全体の既定値は `Theme`、単一 Cell の上書きは `CellStyle`、描画へ渡す解決済み値は `EffectiveStyle` が担い、Core の設定ツリーへ `UIColor`、`UIFont`、`CGFloat` などの UIKit 型を持ち込まない。

#### 公開 API

- `Theme` は SettingsView 全体の canvas、Cell、選択・無効状態、行高さ、Section Header / Footer、Cell 共通表示の既定値を保持する immutable な値型である。`UIColor` / `UIFont` / `CGFloat` を直接受け取り、`Equatable` は `isEqual(_:)` によって値比較する。
- `CellStyle` は単一 Cell の title、description、valueText、hintText、icon、背景、accent、行高さを部分上書きする immutable な値型である。全フィールドの `nil` は「値なし」ではなく `Theme` から継承する指定を表す。
- `EffectiveStyle(theme:cellStyle:)` は描画前に Optional を解決し、各 Cell View が同じ規則を使える非 Optional の値へまとめる。通常の優先順位は `CellStyle` → `Theme` → platform default である。
- `SettingsRootStore.applyTheme(_:)` と `KsSettingsViewController.applyTheme(_:)` は Theme を構造変更から独立して更新する。SwiftUI では `KsSettingsView.theme(_:)` が入口になる。

Theme と Cell の背景は別の表示領域である。`Theme.backgroundColor` は `UICollectionView` と Section 間の canvas、`Theme.cellBackgroundColor` は Cell の既定背景、`CellStyle.backgroundColor` は個別 Cell の背景を表し、一方から他方を推論しない。

#### 保証すること

- title、description、valueText、hintText、icon、背景、accent は `CellStyle` の明示値を `Theme` より優先する。Theme 側も未指定なら UIKit の意味色・text style などへ解決し、描画側へ未解決値を渡さない。
- `Theme.cellTitleFontSize > 0` は、`CellStyle.titleFont` または `Theme.cellTitleFont` から選んだ font の family / weight を保ったまま pointSize だけを上書きする。
- valueText は `CellStyle.valueText*` → `Theme.cellValueText*` → `Theme.cellTitle*` → body / label、hintText 色は `CellStyle.hintTextColor` → `Theme.cellHintTextColor` → `Theme.cellAccentColor` の順で解決する。
- `ButtonCell.titleColor` は例外として `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → `UIColor.systemBlue` の 4 段で解決する。無効時にはこの結果より `Theme.disabledTextColor` を優先する。
- Cell 種別が `accentColor` を直接持つ場合は、その値を `CellStyle.accentColor` と `Theme.cellAccentColor` より先に採用する。
- 同値の `Theme` は `SettingsRootStore.applyTheme(_:)` で通知を抑制し、Theme 更新は `SettingsRootDiff` を発行しない。

#### してはいけないこと

- `Theme` / `CellStyle` の公開境界へ `KsColor` / `KsFont` のような中間論理型を再導入してはならない。
- `nil` の `CellStyle` フィールドを透明色やゼロ値として描画してはならない。
- Theme 更新を Section / Cell の挿入・削除・移動を表す構造 Diff として扱ってはならない。

#### 利用例

```swift
let theme = Theme(
    backgroundColor: .systemGroupedBackground,
    cellBackgroundColor: .secondarySystemGroupedBackground,
    cellAccentColor: .systemOrange,
    hasUnevenRows: true,
    cellTitleColor: .label
)

let emphasized = LabelCell(
    style: CellStyle(titleColor: .systemOrange, cellHeight: 80),
    title: "強調表示"
)

KsSettingsView {
    Section { emphasized }
}
.theme(theme)
```

出典: `ios/Sources/KsSettingsViewUI/Theme.swift` / `CellStyle.swift` / `EffectiveStyle.swift` / `SettingsRootStore.swift`、`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `CellModifiers.swift`、`ios/Tests/KsSettingsViewUITests/EffectiveStyleTests.swift` / `EffectiveStyleResolutionTests.swift` / `SettingsRootStoreTests.swift`、`openspec/specs/settings-view-ios-style/spec.md` Purpose・Theme / CellStyle / EffectiveStyle Requirements、`docs/styling-and-theming.md` §§1–4, 11–12。

### iOS の Cell 共通行レイアウト (提案カテゴリ: styling/)

`KsListCellBase` と `applyCellBaseLayout(...)` は Cell 種別をまたぐ行の視覚文法を一か所に集約する。任意の icon、title、任意の description、valueText または trailing control、任意の hintText を組み、各 Cell View は trailing control の内容と操作だけを追加する。

#### 責務境界

- `KsListCellBase` は共通 subview、再利用時のリセット、選択フィードバック、高さ補正を所有する。
- `applyCellBaseLayout(...)` は `EffectiveStyle` を title / description / valueText / hintText / icon / 背景へ反映し、Cell 種別固有の `UIView` を `trailingViews` として受け取る。
- `LabelCellView`、基本 Cell View、入力 Cell View はいずれも同じ共通行を使う。入力、選択、navigation など trailing control の挙動は各 renderer の責務である。

#### 保証すること

- title と valueText / trailing control は同じ段、description はその下に表示する。hintText は accessory の有無に左右されず Cell 右上に表示する。
- description、icon、hintText が未指定または空なら該当 view を隠し、空の占有領域を残さない。
- valueText と trailing control は併存でき、追加順序は title → valueText → trailing control となる。
- `CellStyle.cellHeight` は `Theme.rowHeight` より優先する。最終高さは iOS の最低行高 48pt を下回らない。
- `Theme.hasUnevenRows == true`（既定）では解決済み高さを下限として内容の自然高まで伸び、`false` では解決済み高さへ固定する。`preferredLayoutAttributesFitting(_:)` でも同じ規則を維持する。

#### してはいけないこと

- Cell 種別ごとに title / description / icon / hintText の別レイアウトを複製してはならない。
- `contentConfiguration` / `UICellAccessory` と自前の共通行を同時に使い、二重の title・accessory を生成してはならない。
- 行高さを platform 間で同じ数値へ機械的に揃えてはならない。iOS の最低行高は 48pt であり、他 platform の token とは独立する。

出典: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` / `CellBaseLayout.swift` / `KsCellViewSupport.swift`、`ios/Sources/KsSettingsViewUI/*CellView.swift`、`ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift` / `KsSettingsViewControllerTests.swift`、`openspec/specs/settings-view-ios-style/spec.md` Purpose・UICollectionView レイアウト・Cell.cellHeight・Cell View 共通基底 Requirements、`docs/styling-and-theming.md` §§6, 14。

### Cell の視覚状態 (提案カテゴリ: styling/)

通常、押下・選択、無効という状態は、`EffectiveStyle` で解決した静的な色・font・背景の上に重ねる。共通の状態遷移は `KsCellViewSupport` と `applyCellBaseLayout(...)` が担い、Native control 固有の disabled 表現は各 Cell View が担う。

#### 保証すること

- 操作可能な Cell が highlighted または selected の間だけ `Theme.selectedColor` を背景に使い、状態解除時は `CellStyle.backgroundColor ?? Theme.cellBackgroundColor` へ戻す。
- `isEnabled == false` では Cell 自身の user interaction を止め、title、description、valueText、hintText の色を `Theme.disabledTextColor` へ置換し、選択背景を表示しない。
- `UISwitch`、checkbox、checkmark、picker、text field などは各 Native control の `isEnabled` / disabled 表現も併用する。
- 行全体の alpha は一律に下げない。背景・icon・レイアウトを不必要に薄めず、テキストと Native control で操作不能を示す。

#### してはいけないこと

- 無効 Cell に selectedColor を適用してはならない。
- Cell 全体の opacity 変更だけで disabled 表現を済ませてはならない。
- Cell 固有色・CellStyle・Theme の解決順序を renderer ごとに独自実装してはならない。例外的な固有色も `EffectiveStyle` の共通規則へ接続する。

出典: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` / `CellBaseLayout.swift` / `EffectiveStyle.swift`、`ios/Sources/KsSettingsViewUI/ButtonCellView.swift` / `SwitchCellView.swift` / `CheckboxCellView.swift` / `RadioCellView.swift` / `SimpleCheckCellView.swift`、`ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift` / `UnifyCellCommonFieldsTests.swift`、`docs/styling-and-theming.md` §§9–11。

### iOS 設定リストの外観と補助領域 (提案カテゴリ: styling/)

`KsSettingsViewStyle` は設定内容を変えずにリスト外観を切り替える公開 enum である。`.classic` は `UICollectionLayoutListConfiguration.Appearance.plain`、`.modern` は `.insetGrouped` に対応し、`KsSettingsViewController.style` または SwiftUI の `KsSettingsView.style(_:)` から指定する。

#### 公開 API

```swift
KsSettingsView {
    Section("一般") {
        LabelCell(title: "バージョン", valueText: "1.0.0")
    }
}
.style(.modern)
.rootHeader("プロフィール")
```

UIKit では `KsSettingsViewController(store:style:)` で初期値を渡し、表示中に `controller.style` を変更できる。同じ値の再代入はレイアウトを作り直さず、値が変わった場合だけ既存 root を保ったまま layout を再構築する。

#### 保証すること

- Classic / Modern の切替は Section の装飾と grouping を変えるが、設定ツリー、Cell ID、Cell renderer 登録、現在の表示内容を変えない。
- Section と Root の Header / Footer は content と共にスクロールし、画面端へ sticky に固定しない。
- 空の Section Header / Footer と未指定の Root Header / Footer は supplementary 領域を生成しない。
- Section Header は後続 Cell 側へ下揃え、Section Footer は先行 Cell 側へ上揃えにする。`Section.headerHeight > 0` は `Theme.headerHeight` より優先し、どちらも未指定なら内容から自動算出する。
- `Theme.backgroundColor` を collection view の canvas に使い、list configuration は透明にして Section 間にも canvas を見せる。Cell 自身の背景は実効 `cellBackgroundColor` を保つ。
- 中間 Cell の separator は icon の有無にかかわらず同じ leading inset、Section の最初と最後の境界線は端から端という視覚規則を共有する。
- Section Header / Footer の font は `Theme.headerFont` / `footerFont` を基にし、正の `headerFontSize` / `footerFontSize` があれば size を上書きする。Footer text color は `Theme.footerTextColor` をそのまま使う。

#### してはいけないこと

- 外観切替を設定ツリーの構造変更として扱ってはならない。
- Header / Footer が空のとき、意味のないグレー帯や固定余白を残してはならない。
- iOS Footer の既定文字色を無条件に dynamic `secondaryLabel` へ置き換えてはならない。AiForms 互換の固定グレーは title / description の意味色とは別契約である。
- separator inset を icon の有無で変えてはならない。

出典: `ios/Sources/KsSettingsViewUI/KsSettingsViewStyle.swift` / `KsSettingsViewController.swift` / `EffectiveStyle.swift`、`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、`ios/Tests/KsSettingsViewUITests/KsSettingsViewStyleTests.swift` / `KsSettingsViewControllerTests.swift` / `EffectiveStyleResolutionTests.swift`、`openspec/specs/settings-view-ios-style/spec.md` Purpose・スタイル切替・Header / Footer・背景・罫線 Requirements、`docs/styling-and-theming.md` §§5, 7–8。

## ADR 候補

なし。Native 型を UI 層が直接所有し Core から分離する判断は既存 `kasane/decisions/0009-ui-layer-native-styling.md` に包含される。Classic / Modern の UIKit Appearance 対応や共通行の局所的実装は、独立した新 ADR にするより concepts の公開契約・視覚契約として残す方が適切である。

## drift 所見

- spec は `Theme.separatorColor` で separator 色を設定するとするが、現行の `separatorConfiguration(for:base:)` は visibility と inset だけを変更し、`Theme.separatorColor` / `EffectiveStyle.separatorColor` を描画へ渡していない。`Theme.separatorColor` は保持・比較・Store 通知の材料に留まる。(`openspec/specs/settings-view-ios-style/spec.md` 「区切り線とヘッダ・フッタ」Scenario / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` / `EffectiveStyle.swift`)
- `Theme.scrollIndicatorVisible` は公開値として保持・比較されるが、`UICollectionView.showsVerticalScrollIndicator` / `showsHorizontalScrollIndicator` へ適用するコードがない。Theme 更新時にも表示状態は変わらない。(`ios/Sources/KsSettingsViewUI/Theme.swift` / `KsSettingsViewController.swift`、`docs/styling-and-theming.md` §2)
- `Theme.headerBackgroundColor` / `footerBackgroundColor` は公開値として保持・比較されるが、Section / Root supplementary の `backgroundConfiguration` または背景 view へ適用されていない。(`ios/Sources/KsSettingsViewUI/Theme.swift` / `KsSettingsViewController.swift` `makeAccessoryListCell`・`applyAccessoryToListCell`、`docs/styling-and-theming.md` §2)
- `KsSettingsViewController.applyTheme(_:)` は canvas 背景と Cell item を再評価する一方、表示済み Section / Root supplementary の再構成や layout 再構築を行わない。このため Theme 更新後の header / footer 色・font と `Theme.headerHeight` は、その場では更新されない可能性がある。旧 concept の「現在の表示を再評価する」という主張より現行コードの更新範囲が狭い。(`kasane/changes/remigrate-concepts/reference/old-concepts/styling/style-resolution.md` 「更新境界」 / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` `applyTheme(_:)`)
- spec の description 既定色には「`UIColor.secondaryLabel` 相当のライトモード固定色」という記述があるが、現行 `Theme.defaultCellDescriptionColor` は dynamic `UIColor.secondaryLabel` そのものである。旧 concept は description を system appearance 追従色として Footer の固定グレーと区別しており、コードと一致する。(`openspec/specs/settings-view-ios-style/spec.md` Theme「Cell 説明・値・ヒント・アイコン系」 / `ios/Sources/KsSettingsViewUI/Theme.swift` / `kasane/changes/remigrate-concepts/reference/old-concepts/styling/list-appearance.md`)
- spec の SwiftUI style Scenario は廃止済みの `KsSettingsView(root: $root, style:)` を、Root H/F の注記は廃止済みの `.header(...)` / `.footer(...)` を参照する。現行入口は Store または DSL initializer と `.rootHeader(...)` / `.rootFooter(...)` である。(`openspec/specs/settings-view-ios-style/spec.md` 「SwiftUI ラッパでのスタイル指定」Scenario・Root H/F 注記 / `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `docs/platform-guide-ios.md` §2)
- iOS platform guide は `.disabled(true)` を Cell modifier による上書き例として示すが、現行 `KsCell.disabled(_:)` は明示的な no-op である。利用例としては操作不能状態を作れない。(`docs/platform-guide-ios.md` §9 / `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift`)

## 用語

- Theme: SettingsView 全体のスタイル既定値を保持する `KsSettingsViewUI.Theme`。
- CellStyle: 単一 Cell の部分上書きを保持する `KsSettingsViewUI.CellStyle`。Optional の `nil` は Theme 継承を表す。
- EffectiveStyle: Theme と CellStyle を描画値へ解決する `KsSettingsViewUI.EffectiveStyle`。
- canvas: `Theme.backgroundColor` が塗る `UICollectionView` と Section 間の背景領域。Cell 背景とは独立する。
- trailing control: title と同じ段の右側に置く `UISwitch`、checkmark、text field、chevron などの Cell 種別固有 view。
- supplementary: Section / Root Header / Footer を Cell 本体とは別に描画する UICollectionView の補助領域。

## 抽出メモ

4 概念候補はいずれも Android の `settings-view-android-style` と同じ抽象を共有するため、Batch B 統合では旧 `styling/style-resolution.md`、`cell-row-layout.md`、`cell-visual-states.md`、`list-appearance.md` の粒度を土台に platform 共通契約へまとめ、iOS 固有アンカーと利用例を併記するのが自然である。

本 candidate では公開 API と複数ファイルに分散する解決・視覚契約を残し、stack spacing、label margin、estimated height などの生値と UIKit 制約の実装手順は価値 lint により除外した。separator inset と iOS 最低行高は、利用者が見た目と高さを予測する長命な視覚契約なので例外的に残した。
