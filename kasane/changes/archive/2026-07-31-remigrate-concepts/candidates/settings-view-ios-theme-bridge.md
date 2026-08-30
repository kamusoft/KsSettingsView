# Candidate: settings-view-ios-theme-bridge

## 概念候補

### iOS SwiftUI のスタイル modifier 境界 (提案カテゴリ: platforms/)

`KsSettingsViewSwiftUI` は UIKit 型を受け取る値型 modifier を提供し、SwiftUI DSL の記述を `KsSettingsViewUI` の `CellStyle`、`KsImage`、`Theme` へ接続する。modifier は元の Cell / Section を変更せず、対応する公開プロトコルを実装した型だけを copy して返す。

#### 公開 API

Cell の `font(_:)`、`descriptionFont(_:)`、`iconSize(_:)`、`cellHeight(_:)`、`titleColor(_:)`、`backgroundColor(_:)` は、対象が `DSLStyleModifiable` に準拠するときだけ現在の `CellStyle` の該当フィールドを差し替える。他のフィールドは保持され、元の Cell は変化しない。

`icon(_:)` は対象が `DSLIconModifiable` に準拠するときだけ `withIcon(_:)` で copy を返す。現行の組み込み Cell 12 種は `DSLStyleModifiable` と `DSLIconModifiable` の両方に準拠する。利用者定義 Cell で同じ modifier を有効にするには、対応プロトコルと copy API を実装する必要がある。

`sectionHeader(_:)` / `sectionFooter(_:)` は文字列または SwiftUI `View` を受け取り、`SectionAccessory.text` / `.view(KsAnyView.swiftUI)` を持つ Section copy を返す。

#### 保証すること

- style modifier の連鎖は、それまでに設定した `CellStyle` の他フィールドを失わない。
- modifier が作る copy は Cell の `id` と DSL identity hint を維持し、見た目の変更を構造上の別 Cell にしない。
- UIKit の色・font・寸法を `UIColor` / `UIFont` / `CGFloat` のまま受け取り、Core に中間スタイル型を要求しない。
- 対応プロトコルに準拠しない利用者定義 Cell へ style / icon modifier を呼んだ場合は、型を壊さず元の Cell を返す。
- `disabled(_:)` は現行では常に no-op であり、Cell の `isEnabled` を書き換えない。無効 Cell は各 Cell initializer の `isEnabled` で構築する。

#### してはいけないこと

- `disabled(_:)` を機能する無効化 API として利用例に載せてはならない。
- modifier から Cell の具象型を失う existential を返してはならない。利用者が同じ型のまま連鎖できる `Self` を返す。
- style modifier のたびに新しい `CellStyle` を作る際、対象外フィールドを既定値へ戻してはならない。

#### 利用例

```swift
let cell = LabelCell(title: "名前")
    .titleColor(.systemOrange)
    .backgroundColor(.secondarySystemGroupedBackground)
    .font(.preferredFont(forTextStyle: .headline))
    .icon(.systemName("person"))
    .cellHeight(60)

KsSettingsView {
    Section { cell }
        .sectionHeader("プロフィール")
        .sectionFooter { Text("公開範囲を確認してください") }
}
```

出典: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` / `SectionModifiers.swift`、`ios/Sources/KsSettingsViewUI/DSLStyleModifiable.swift` / `DSLIconModifiable.swift` と組み込み Cell 12 種の `withStyle(_:)` / `withIcon(_:)`、`ios/Tests/KsSettingsViewSwiftUITests/CellModifiersTests.swift` / `SectionModifiersTests.swift`、`samples/ios/KsSettingsViewSample/DSLDemoView.swift`。

### iOS SwiftUI の Theme 伝播 (提案カテゴリ: platforms/)

`KsSettingsView.theme(_:)` は `Theme` を SwiftUI wrapper から `SettingsRootStore` へ渡し、Store の Theme publisher を通じて `KsSettingsViewController` へ反映する。Theme は設定ツリーの構造ではないため、Store 方式と DSL 方式のどちらでも `SettingsRootDiff` から分離した専用経路を使う。

#### Store 方式

`KsSettingsView(store:style:)` は外部 `SettingsRootStore` をそのまま使用する。`.theme(theme)` がなければ Store の現在 Theme を維持し、指定があれば controller の生成・更新時に同値判定後 `store.applyTheme(theme)` を呼ぶ。この modifier は wrapper 内だけの装飾ではなく、渡された外部 Store の Theme を更新する。

#### DSL 方式

`KsSettingsView { ... }` は内部 `SettingsRootStore` を View identity の間保持する。初回 Theme は `.theme(theme)` の指定値、未指定なら `Theme()` である。再評価では構造 Diff を先に Store へ流し、前回と異なる Theme だけを `store.applyTheme(_:)` へ渡す。

DSL 再評価時に Theme 指定が `nil` なら、直前の Theme を維持する。modifier を外したことを「`Theme()` へ戻す」という命令としては扱わない。

#### 保証すること

- `SettingsRootStore.applyTheme(_:)` は永続 Theme 状態を更新するが、`SettingsRootDiff` を発行しない。同値 Theme の通知は抑制する。
- Theme 更新で Section / Cell の ID と構造を変えず、controller は同じ item を再構成して見た目を更新する。
- Store / DSL の両経路で、SwiftUI の `body` getter 自体に Store 更新の副作用を置かず、Representable の make / update 経路で Theme を反映する。
- `.style(_:)`、`.rootHeader(_:)`、`.rootFooter(_:)` は Theme と別の公開状態として controller へ渡す。

#### してはいけないこと

- Theme を `SettingsRoot` に埋め戻したり、insert / remove / replace と同じ構造 Diff に変換してはならない。
- `.theme` が未指定の Store 方式で、外部 Store の Theme を `Theme()` に上書きしてはならない。
- DSL 方式で View 再評価のたびに新しい Store を作り、Theme の現在値を失ってはならない。

#### 利用例

```swift
let theme = Theme(
    backgroundColor: .systemGroupedBackground,
    cellAccentColor: .systemOrange,
    hasUnevenRows: true
)

KsSettingsView {
    Section("一般") {
        LabelCell(title: "バージョン", valueText: "1.0.0")
    }
}
.theme(theme)
```

Store を共有する場合は、modifier の代わりに所有者が直接 `store.applyTheme(newTheme)` を呼んでも同じ publisher 経路で controller に伝播する。

出典: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `DSLDiffCalculator.swift`、`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` / `KsSettingsViewController.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift` / `KsSettingsViewRepresentableTests.swift` / `KsSettingsViewMakeUIViewControllerTests.swift`、`ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift` / `ApplyDiffTests.swift`、`kasane/changes/remigrate-concepts/reference/old-concepts/architecture/store-and-update-streams.md` / `display-state-synchronization.md`、`docs/styling-and-theming.md` §12。

## ADR 候補

なし。Theme / CellStyle / KsImage を UI 層へ置き、Native 型を直接公開し、Theme 更新を構造 Diff から分離する判断は既存 `kasane/decisions/0009-ui-layer-native-styling.md` に包含される。modifier の protocol-based copy はその決定を SwiftUI DSL へ接続する局所的な API 契約であり、新しい横断判断ではない。

## drift 所見

- spec の Purpose は Core が `Theme` / `CellStyle` / `KsColor` / `KsImage` を所有し UIKit 値へ変換する bridge を説明するが、現行ではこれらは `KsSettingsViewUI` 所属で、`UIColor` / `UIFont` / `KsImage.uiImage` を直接保持する。変換層は存在せず、実体は `EffectiveStyle` による実効値解決である。(`openspec/specs/settings-view-ios-theme-bridge/spec.md` Purpose / `ios/Sources/KsSettingsViewUI/Theme.swift` / `CellStyle.swift` / `KsImage.swift` / `EffectiveStyle.swift`、`kasane/decisions/0009-ui-layer-native-styling.md`)
- spec の Requirement / Scenario は削除済みの `Theme.viewBackgroundColor` / `Theme.titleColor` / `Theme.titleFont` / `Theme.descriptionColor` を参照する。現行名は `backgroundColor` / `cellTitleColor` / `cellTitleFont` / `cellDescriptionColor` である。(`openspec/specs/settings-view-ios-theme-bridge/spec.md` 「Theme / CellStyle の UIKit 変換」・ButtonCell Requirements / `ios/Sources/KsSettingsViewUI/Theme.swift`)
- `docs/styling-and-theming.md` と `docs/platform-guide-ios.md` の CellStyle 例は `LabelCell(title: ..., style: ...)` の順で引数を渡すが、現行 initializer は `LabelCell(style:title:...)` の順を要求するため、そのままではコンパイルできない。(`docs/styling-and-theming.md` §3 / `docs/platform-guide-ios.md` §9 / `ios/Sources/KsSettingsViewUI/LabelCell.swift`)
- `docs/platform-guide-ios.md` は modifier chain に `.disabled(true)` を含めるが、現行 `KsCell.disabled(_:)` は常に元の Cell を返す no-op であり、`isEnabled` を変更しない。(`docs/platform-guide-ios.md` §9 / `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift`)
- `DSLIconModifiable.swift` と `CellModifiers.swift` の説明は icon 対応を `LabelCell` / `CommandCell` に限定し、`SwitchCell` 等では no-op とするが、現行の基本 Cell 7 種と入力 Cell 5 種はすべて `icon` / `withIcon(_:)` を持ち `DSLIconModifiable` に準拠する。実装と同じリポジトリ内の API 説明が乖離している。(`ios/Sources/KsSettingsViewUI/DSLIconModifiable.swift` / `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` / 組み込み Cell 12 種)
- 旧 concept は Theme 変更時に「現在の表示を再評価する」とするが、`KsSettingsViewController.applyTheme(_:)` は Cell item と canvas 背景だけを更新し、表示済み Section / Root supplementary の再構成や layout 再構築を行わない。このため header / footer の色・font・`Theme.headerHeight` は即時反映されない可能性がある。(`kasane/changes/remigrate-concepts/reference/old-concepts/styling/style-resolution.md` 「更新境界」 / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` `applyTheme(_:)`)

## 用語

- DSLStyleModifiable: CellStyle を読み取り、`withStyle(_:)` で同じ具象型の copy を返せる Cell の公開プロトコル。
- DSLIconModifiable: `withIcon(_:)` で同じ具象型の copy を返せる Cell の公開プロトコル。
- Store 方式: 利用者が所有する `SettingsRootStore` を `KsSettingsView(store:style:)` に渡す経路。
- DSL 方式: `KsSettingsView { ... }` が内部 Store と前回ツリーを保持し、再評価結果を差分反映する経路。
- Theme 伝播: SwiftUI wrapper の Theme 指定を Store の永続状態へ反映し、publisher 経由で controller に届ける更新経路。

## 抽出メモ

実効スタイルの優先順位、Cell の視覚状態、共通行レイアウト、リスト外観は `settings-view-ios-style` candidate と重複するため、本 candidate では SwiftUI bridge 固有の modifier 適用条件と Theme 伝播を独立材料として抽出した。Batch B 統合では前者を `styling/style-resolution.md` へ、後者を `architecture/store-and-update-streams.md` または platform 固有 API 節へ合流するのが自然である。

SwiftUI `.theme(_:)` の Store / DSL make・update 経路を直接検証するテストは見当たらず、現在の保証はコード読解と Store 単体テストに基づく。統合時にはテスト不足を drift ではなく検証鮮度の所見として扱う。
