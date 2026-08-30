# Candidate: docs core / cells / styling 残差

## 対象と抽出順

対象は `docs/core-model.md`、`docs/cells.md`、`docs/styling-and-theming.md`。先に現行の core-model / cells / styling / architecture concepts と、iOS・Android の Core、具象 Cell、`Theme`、`CellStyle`、`EffectiveStyle`、対応テストを確認し、その後に docs を照合した。

docs の章立てや全フィールドを転記せず、公開 API と利用例、複数箇所へ分散した解決規則、外観上の横断契約のうち既存 concepts に未回収のものだけを候補にした。

## 概念候補

### Theme / CellStyle の公開 API（提案カテゴリ: styling/、既存 `style-resolution.md` の公開 API 補強または別 reference）

`Theme` は画面全体の既定値、`CellStyle` は単一 Cell の上書きを表す UI 層の公開値型である。既存 [スタイルの所有と実効値解決](../../../concepts/styling/style-resolution.md) は所有境界と大枠の優先順位を説明しているが、利用者がどの入口へ何を指定できるかと、単純な「CellStyle → Theme → default」だけでは表せない特殊解決をまだ案内していない。

#### 公開 API の要点

| 型 | 公開する値のまとまり |
|---|---|
| `Theme` | canvas / Cell 背景、separator、選択・accent・disabled 色、scroll indicator、行高さ、Section Header / Footer、Cell の title / valueText / description / hintText / icon の画面既定 |
| `CellStyle` | title / valueText / description / hintText の色と font、icon size / radius、単一行の height / background / accent |
| `EffectiveStyle` | `Theme` と `CellStyle` を Native 描画で未解決値が残らない形へ合成する実装アンカー。iOS は公開型、Android は module-internal |

色・font・寸法は iOS が `UIColor` / `UIFont` / `CGFloat`、Android が Compose `Color` / `TextStyle` / `Dp` を直接受ける。`CellStyle` の全フィールドが未指定なら `Theme` を継承する。

通常の優先順位に加えて、次の特殊解決は公開 API 利用時の再導出コストが高い。

- `Theme.cellTitleFontSize > 0` は、`CellStyle.titleFont` が選ばれた場合も含め、最終 title font の size を上書きする。
- valueText の色と font は `CellStyle` → Theme の valueText 既定 → Theme の title 既定 → platform default の順で解決する。
- hintText の色は `CellStyle.hintTextColor` → `Theme.cellHintTextColor` → `Theme.cellAccentColor`、icon は未指定時に 24pt / 24dp・radius 0 へ解決する。
- Header / Footer font は `Theme.headerFont` / `footerFont` を基礎にし、対応する正の `headerFontSize` / `footerFontSize` が最終 size を上書きする。

#### 利用例

```swift
let theme = Theme(
    backgroundColor: .systemGroupedBackground,
    cellAccentColor: .systemOrange,
    cellTitleFontSize: 17
)

let cell = LabelCell(
    title: "重要",
    style: CellStyle(titleColor: .systemOrange, cellHeight: 64)
)
```

```kotlin
val theme = Theme(
    backgroundColor = Color(0xFFF2F2F7),
    cellAccentColor = Color(0xFFFF9500),
    cellTitleFontSize = 17.0,
)

val cell = LabelCell(
    title = "重要",
    style = CellStyle(titleColor = Color(0xFFFF9500), cellHeight = 64.dp),
)
```

#### 保証すること

- 未指定値を継承の意思として扱い、描画時には Native 値へ解決する。
- platform ごとの Native 型を利用者が直接指定できる。
- size 専用フィールドと button title の特殊優先順位を通常規則から区別する。

#### してはいけないこと

- `CellStyle()` の未指定フィールドを透明色や0寸法として扱わない。
- Android の内部 `EffectiveStyle` を利用者向け公開入口として案内しない。
- iOS / Android の platform default の生値が常に同一だと仮定しない。

出典: `ios/Sources/KsSettingsViewUI/Theme.swift` / `CellStyle.swift` / `EffectiveStyle.swift`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` / `CellStyle.kt` / `EffectiveStyle.kt`、両 platform の `EffectiveStyleResolutionTests` / `EffectiveStyleResolutionTest` と `EffectiveStyleTests` / `EffectiveStyleTest`、`docs/styling-and-theming.md` §2–4。

### Accessory へ任意 View を渡す公開 API（提案カテゴリ: core-model/、既存 `settings-tree.md` への追記）

既存 [設定ツリー](../../../concepts/core-model/settings-tree.md) は `KsAnyView` の責務と等価性境界を説明しているが、公開 factory / case と最小利用例は未回収である。Accessory の任意 View は Core が platform 型を完全排除していないことを利用者が実際に扱う入口なので、公開 API 例として残す価値がある。

#### 公開 API

- iOS: `KsAnyView.swiftUI { ... }` / `KsAnyView.uiKit { ... }`
- Android: `KsAnyView.Compose { ... }` / `KsAnyView.AndroidView { context -> ... }`
- 生成値を `SectionAccessory.view` / `.View` または `RootAccessory.view` / `.View` へ渡す。

```swift
let section = Section(
    header: .view(.swiftUI { Text("詳細設定") }),
    cells: []
)
```

```kotlin
val section = Section(
    id = "details",
    header = SectionAccessory.View(
        KsAnyView.Compose { Text("詳細設定") },
    ),
)
```

#### 保証すること

- SwiftUI / UIKit と Compose / Android View の任意 View を Accessory の同じ model 境界へ渡せる。
- `KsAnyView` の内容を `SettingsRoot` / `Section` の値等価へ参加させない。

#### してはいけないこと

- 任意 View の内容変更を model の値比較だけで検出できると仮定しない。
- `KsAnyView` を「platform 非依存の Core 値」と説明しない。

出典: `ios/Sources/KsSettingsViewCore/KsAnyView.swift` / `SectionAccessory.swift` / `RootAccessory.swift`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt` / `SectionAccessory.kt` / `RootAccessory.kt`、両 platform の `KsAnyViewTests` と Accessory tests、`docs/core-model.md` §3–5。

### ButtonCell の補助フィールドと titleAlignment（提案カテゴリ: cells/、既存 `basic-cells.md` への追記）

全7種が `valueText`・`icon`・`hintText` を持ち、`ButtonCell` だけが `description` を持たないという現行 concept の訂正済み契約を前提に、`ButtonCell` の補助フィールド有無で `titleAlignment` の適用範囲が変わる点を補う。

- `valueText` / `icon` / `hintText` がすべて未指定なら title が唯一の行内容となり、`titleAlignment` は利用可能な title 領域全体へ反映される。Android は専用 constraint で title を行全体へ広げ、iOS は共通行 layout のまま残り領域を title 列が占める。
- いずれかを指定すると共通行の他要素へ領域を渡し、`titleAlignment` は残った title 列内へ反映される。
- 既定は center。Swift は `.start` / `.center` / `.end`、Kotlin は `START` / `CENTER` / `END` を使う。

#### 保証すること

- 補助フィールドを追加しても ButtonCell は Disclosure Indicator を表示しない。
- 補助フィールドの有無にかかわらず `titleAlignment` を反映する。

#### してはいけないこと

- `ButtonCell` へ `description` を追加しない。
- Kotlin の enum case を Swift と同じ小文字表記で案内しない。

出典: `ios/Sources/KsSettingsViewUI/ButtonCell.swift` / `ButtonCellView.swift`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ButtonCell.kt` / `ButtonCellViewHolder.kt`、iOS `UnifyCellCommonFieldsTests.swift` と両 platform `BasicCellsTests` / `BasicCellsTest`、`docs/cells.md` §ButtonCell。

### Section Header / Footer と Classic separator の外観契約（提案カテゴリ: styling/、既存 `list-appearance.md` への追記）

既存 [設定 list の外観と補助領域](../../../concepts/styling/list-appearance.md) に、両 platform の実装とテストで固定され、docs に残っている次の外観契約を補う。

#### Header / Footer

- text の Section Header は領域の下側、Section Footer は上側へ揃える。
- `Section.headerHeight > 0` を最優先し、`Section.headerHeight == -1` かつ `Theme.headerHeight > 0` なら Theme の固定高さを使う。両方未指定なら内容に応じた自動高さを使う。

#### Classic separator

- Section 最初の Cell の上端と最後の Cell の下端は全幅、Section 内の中間 separator は左から 16pt / 16dp inset する。icon の有無で inset を変えない。
- Android は Cell 背景の後に 1物理 pixel の hairline を描き、Root Header / Footer と Section Accessory 行を separator 対象に含めない。
- Modern の Section 背景・角丸と Classic separator の規則を混在させない。

#### 保証すること

- Section 個別の明示高さを Theme の既定より優先する。
- Classic の Section 境界と中間行を separator geometry で区別する。

#### してはいけないこと

- `Section.headerHeight = 0` や `-1` 未満に意味を追加しない。
- Android の1物理 pixelを1dpへ換算しない。
- icon の有無で中間 separator の開始位置を変えない。

出典: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` / `ClassicSectionDecoration.kt`、iOS `SectionAccessoryRenderingTests.swift`、Android `SectionAccessoryRenderingTest.kt` / `ClassicSectionDecorationTest.kt`、`docs/styling-and-theming.md` §7–8。

## ADR 候補

なし。

今回回収した責務境界は ADR-0005（Root / Section Accessory）、ADR-0009（UI 層 Native styling）、ADR-0011（共通 Cell 行）に既に含まれる。Header / Footer の垂直配置、height fallback、separator geometry、ButtonCell の layout 分岐は、覆すコスト・境界横断・将来制約の3基準に照らして L2 concept の具体契約に留めるのが妥当である。

## drift 所見

1. 3文書が `openspec/specs/...` を「正本」と記す ↔ この repository では `openspec/` は凍結済みで、現仕様の SSoT は code と test、長命な入口は Kasane concepts である（`docs/core-model.md` / `docs/cells.md` / `docs/styling-and-theming.md`、`AGENTS.md`、`ksn-core`）。
2. Core を「UI 非依存」「UIKit / Compose / Material に一切依存しない」と記す ↔ `KsAnyView` が iOS で SwiftUI / UIKit、Android で Compose / Android View を直接参照する。現行 `settings-tree.md` の「Core は platform 型を完全には排除していない」が正しい（`docs/core-model.md` 冒頭 / `ios/Sources/KsSettingsViewCore/KsAnyView.swift` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt`）。
3. iOS の `Section.cells` を `[AnyCell]` とし、`AnyCell` を提供すると記す ↔ 現行は `[any KsCell]` であり `AnyCell` は存在しない（`docs/core-model.md` §2, §6 / `ios/Sources/KsSettingsViewCore/Section.swift:36`）。
4. Swift の `Hashable` を自動実装と記す ↔ `Section`、Accessory、具象 Cell は existential、任意 View、callback、Native style 値を扱うため手動で等価性と hash を実装している。保証すべきなのは「値等価を持つ」ことで、自動合成は現行契約ではない（`docs/core-model.md` 設計方針 / `Section.swift` / 各 Cell model）。
5. Kotlin の `CellTitleAlignment` case を `start / center / end` と記す ↔ 現行 Kotlin enum は `START / CENTER / END`。`docs/cells.md` の `CellTitleAlignment.center` 例もコンパイルしない（`docs/core-model.md` §7 / `docs/cells.md` ButtonCell 例 / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/CellTitleAlignment.kt`）。
6. 「ButtonCell を除く6種」が `description`、`valueText`、`icon`、`hintText` を持つという表現 ↔ 全7種が `valueText`・`icon`・`hintText` を持ち、`ButtonCell` だけが `description` を持たない。現行 `basic-cells.md` は訂正済み（`docs/cells.md` 共通 Optional フィールド / 両 platform `ButtonCell`）。
7. iOS 例で基本 `SwitchCell` / `CheckboxCell` / `SimpleCheckCell` に `$state` を渡す ↔ iOS の基本7種は `Binding` initializer を持たず、値 + callback を使う。`Binding` initializer があるのは入力 Cell（`docs/cells.md` 各例 / iOS の基本 Cell model / `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`）。
8. `hasUnevenRows = false` を「全 Cell を rowHeight で一律固定」と記す ↔ 実装は先に `CellStyle.cellHeight`、次に `Theme.rowHeight`、最後に platform minimum を解決し、その Cell ごとの解決済み高さを固定する（`docs/styling-and-theming.md` §6 / 両 platform `EffectiveStyle` と対応 tests / 現行 `cell-row-layout.md`）。

## 既知の Batch A 相対リンク修正候補

`kasane/concepts/core-model/` と `kasane/concepts/cells/` の確定済み5文書に、旧「concepts root 基準」で書かれたリンクが24件ある。実配置基準では次のように直す。

| 配置元 | 誤った形 | 正しい形 |
|---|---|---|
| `core-model/settings-tree.md` | `core-model/structural-changes.md` | `structural-changes.md` |
| `core-model/settings-tree.md` | `cells/basic-cells.md` / `cells/input-cells.md` / `cells/ks-image.md` | `../cells/basic-cells.md` / `../cells/input-cells.md` / `../cells/ks-image.md` |
| `core-model/structural-changes.md` | `core-model/settings-tree.md` | `settings-tree.md` |
| `cells/*.md` | `cells/basic-cells.md` / `cells/input-cells.md` / `cells/ks-image.md` | `basic-cells.md` / `input-cells.md` / `ks-image.md` |
| `cells/basic-cells.md`, `cells/input-cells.md` | `core-model/settings-tree.md` | `../core-model/settings-tree.md` |

対象件数は `settings-tree.md` 12件、`structural-changes.md` 1件、`basic-cells.md` 7件、`input-cells.md` 2件、`ks-image.md` 2件。本文の意味は変えず、リンク target だけを修正する。

出典: `ksn-core` の「概念間リンクは実配置基準」、`rg` による上記5文書の Markdown link 全件照合。

## 用語

- Native type: iOS の `UIColor` / `UIFont` / `CGFloat`、Android の Compose `Color` / `TextStyle` / `Dp` など、各 platform が提供する型。
- platform default: `CellStyle` と `Theme` のどちらにも値がないとき、Native UI が最終値として使う既定。
- height fallback: `Section.headerHeight`、`Theme.headerHeight`、自動高さの順に採用する Header 高さ解決。
- auxiliary field: `ButtonCell` の `valueText` / `icon` / `hintText`。いずれかの有無で title の layout 範囲が変わる。

## 抽出メモ

- 概念候補は4件。うち3件は既存 concept への追記候補、`Theme / CellStyle` は公開 API の探索入口として別 reference 化と既存 `style-resolution.md` への統合のどちらも可能。統合判断は指揮側に委ねる。
- ADR 候補は0件、drift は8件。
- docs の px / pt / dp や View hierarchy の逐語的説明は、上記の cross-platform 契約を除いて高腐食・低再導出コストとして回収しなかった。
- `docs/cells.md` の Sample 表示文字列や navigation、`docs/styling-and-theming.md` の内部 class 継承説明は製品契約として回収しなかった。
