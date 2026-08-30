# Candidate: settings-view-ios-swiftui

## 概念候補

### SwiftUI と Native Host の Bridge (提案カテゴリ: architecture/)

`KsSettingsViewSwiftUI` は、SwiftUI の宣言状態または利用者所有の `SettingsRootStore` を、UIKit の `KsSettingsViewController` へ接続する Bridge である。公開入口の `KsSettingsView` 自体は `SwiftUI.View` であり、内部の `StoreBackedRepresentable` / `DSLBackedRepresentable` が `UIViewControllerRepresentable` 境界を担う。

#### 責務境界

- Store 方式の `KsSettingsView(store:style:)` は、利用者が所有する `SettingsRootStore` をそのまま `KsSettingsViewController` へ渡す。大量データ、高頻度更新、命令型の `insertCell` / `removeCell` 等を利用者側で制御したい場合の入口である。
- DSL 方式の `KsSettingsView(style:) { ... }` は、`@KsSettingsViewBuilder` で `DSLSectionNode` 列を組み立て、SwiftUI の View identity が続く間、`DSLBookkeeper` が内部 Store と前回の resolved tree を保持する。一般的な設定画面の宣言的入口である。
- DSL の評価と Store への差分適用は SwiftUI `body` の getter では行わず、内部 `DSLBackedRepresentable.updateUIViewController` の更新境界で行う。`body` は Representable の構築だけを担う。
- `rootHeader` / `rootFooter` / `style` は `KsSettingsViewController` の画面プロパティへ反映する。`theme` は `SettingsRootDiff` へ混ぜず、`SettingsRootStore.applyTheme(_:)` へ流す。
- Native list、Cell renderer、visible projection、Theme / `CellStyle` の解決は `KsSettingsViewUI` が担う。`KsSettingsViewSwiftUI` はそれらを再実装しない。

#### 保証すること

- Store 方式と DSL 方式は別々の描画基盤を持たず、どちらも `SettingsRootStore` と `KsSettingsViewController` の更新経路へ収束する。
- Store 方式では、外部 Store の初期 root / theme と後続の変更通知を Controller が購読する。
- DSL 方式では、初回評価から安定 ID 解決済みの `SettingsRoot` と初期 Theme を持つ内部 Store を作り、後続評価では前回ツリーとの差だけを同じ Store へ適用する。
- `.rootHeader(...)` / `.rootFooter(...)` は文字列と任意 SwiftUI `View` の両方を受ける。`.style(_:)` と `.theme(_:)` は Store 方式・DSL 方式の両方で利用できる。
- `Section` の DSL は文字列または `SectionAccessory` の Header / Footer、`headerHeight`、`isVisible` と Cell 列を受ける。`ksSection(...)` と `KsSection` は、`SwiftUI.Section` との名前衝突を避ける公開入口である。
- `KsSettingsViewBuilder` / `KsSectionBuilder` / `SectionBuilder` / `SettingsRootBuilder` は、単一要素、配列、`for`、`if`、`if/else` を平坦な Section / Cell 列へ展開する。
- Cell と Section の modifier は値型 copy を返す。`font` / `descriptionFont` / `iconSize` / `cellHeight` / `titleColor` / `backgroundColor` は、対応 protocol に opt-in した Cell の `CellStyle` または `KsImage` を更新する。

#### してはいけないこと

- Root Header / Footer や Theme を `SettingsRoot` のフィールドとして扱ってはならない。
- DSL 評価中の SwiftUI `body` から Native View や Store を直接変更してはならない。
- DSL 方式の `KsSettingsView` に対して、Store 方式専用の `makeController()` / `applyUpdate(to:coordinator:)` を一般の公開更新経路として使ってはならない。`makeController()` は DSL backing では `fatalError` になる。
- `KsSettingsViewSwiftUI` が Cell の描画、registry、visible projection、style 解決まで担うと解釈してはならない。
- `.disabled(_:)` で Cell が無効化されると仮定してはならない。現行実装は引数を捨てて元の Cell を返す no-op である。

#### 公開 API

主要な入口は `KsSettingsView` の Store 方式 init と DSL 方式 init、`KsSettingsViewBuilder`、`SectionBuilder` / `KsSectionBuilder`、`ksSection(...)`、独自 `ForEach`、Root / Section / Cell modifier である。Root modifier は `rootHeader` / `rootFooter` / `style` / `theme`、Section modifier は `sectionHeader` / `sectionFooter` / `sectionID`、Cell modifier は style / icon 系と `cellID` を提供する。

#### 利用例

一般的な設定画面では DSL 方式を使う。

```swift
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

struct SettingsScreen: View {
    @State private var enabled = true

    var body: some View {
        KsSettingsView {
            Section("通知") {
                SwitchCell(
                    title: "プッシュ通知",
                    isOn: enabled,
                    onValueChanged: { enabled = $0 }
                )
            }
            .sectionFooter("端末の通知設定も確認してください")
        }
        .rootHeader("プロフィール")
        .style(.modern)
    }
}
```

命令型の構造更新を直接制御する場合は Store 方式を使う。

```swift
let store = SettingsRootStore(initialRoot: root, initialTheme: theme)
let view = KsSettingsView(store: store, style: .classic)

store.insertCell(newCell, in: sectionID, at: 0)
```

出典: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `KsSettingsViewBuilder.swift` / `SectionBuilder.swift` / `SettingsRootBuilder.swift` / `CellModifiers.swift` / `SectionModifiers.swift`、`ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewMakeUIViewControllerTests.swift` / `KsSettingsViewRepresentableTests.swift` / `SettingsRootBuilderTests.swift` / `CellModifiersTests.swift` / `SectionModifiersTests.swift`、`samples/ios/KsSettingsViewSample/ContentView.swift` / `DSLDemoView.swift` / `BasicCellsDemoView.swift` / `InputCellsDemoView.swift`、`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose、`docs/architecture.md` §4、`docs/platform-guide-ios.md` §1〜§3 / §11

### 宣言ツリーの identity と差分更新 (提案カテゴリ: architecture/)

SwiftUI は `body` 評価ごとに Section と Cell の値を作り直すため、一時インスタンスの `UUID()` をそのまま構造 identity に使うと、同じ行を継続追跡できない。`DSLSectionNode` / `DSLCellNode` は意味のあるヒントから決定的な UUID を解決し、`DSLDiffCalculator` はその ID を構造変更、内容更新、可視性変更の判定へ使う。

#### 責務境界

- Section の実効 identity 優先順位は、明示 `.sectionID(_:)`、独自 `ForEach` の item ID、`(rootIdx, header text)`、`rootIdx` fallback の順である。コード上は `DSLHintRegistry` が `.explicit` を `.forEach` より優先する。
- Cell の実効 identity 優先順位は、明示 `.cellID(_:)`、独自 `ForEach` の item ID、`(Section ID, Section 内 index, Cell 型)` fallback の順である。
- `DSLIdentityUUID.uuid(from:)` は同じ `DSLIdentityHint` を同じ UUID へ決定的に変換する。Swift 標準 `Hasher` は process ごとに seed が変わるため使わず、`StableHasher` が hint の種別と値を固定 namespace 内で変換する。
- `DSLRootTree.resolvedSections()` は Node の identity を Core の `Section.id` / `KsCell.id` へ反映する。Cell 側の ID 再束縛には `DSLReidentifiable.withDSLID(_:)` への opt-in が必要である。
- `DSLDiffCalculator` は resolved tree 同士を比較し、Section / Cell の追加、削除、移動、Accessory 更新、同一 ID の Cell 内容更新を `SettingsRootDiff` に変換する。
- Theme の更新は差分列に含めず `applyTheme` へ分離する。可視性変更は通常の内容更新へ押し込まず、preflight で `.full(newRoot)` だけを返して visible projection の再構築へ渡す。

#### 保証すること

- 同じ明示 ID、同じ `ForEach` item ID、または同じ静的 fallback 条件からは、SwiftUI の再評価をまたいで同じ Section / Cell UUID が得られる。
- 静的 DSL の内容が同じなら、二回評価しても Section / Cell ID は一致し、`DSLDiffCalculator.compute` は空列を返す。
- 同じ Cell ID のまま title、state、`CellStyle` 等が変わった場合は `.replaceCell` を発行し、構造 identity は変えない。連続する内容更新でも `KsCellID` は同じ UUID を保つ。
- 追加、削除、移動は ID 集合と順序から構造 Diff を発行する。独自 `ForEach` へ item を追加した場合、既存 item の ID を保ち、新規 item だけを insert として扱う。
- 同一 ID の `Section.isVisible` または `VisibilityAware.isVisible` が変わった場合、他の差分を混ぜず `.full(newRoot)` だけを発行する。
- Section / Root の文字列 Accessory の変更は `.updateAccessory` を発行する。`.view(KsAnyView)` 同士は中身を比較しないため、同じ case 間では Accessory 差分を発行しない。
- Cell modifier による値の再構築は ID を維持する。内容が変われば、同じ ID に対する `.replaceCell` として扱う。

#### してはいけないこと

- 動的な挿入・削除・並べ替えがある構造で、位置 fallback に意味的な identity を期待してはならない。`ForEach` または明示 `sectionID` / `cellID` を使う。
- Cell の title、checked 状態、style 等を構造 identity に含めてはならない。
- 同一 ID の内容変更を remove + insert や別 identity として扱ってはならない。
- 可視性変更を `.replaceCell` だけで処理してはならない。
- `DSLReidentifiable` に準拠しない独自 Cellで、`.cellID(_:)` や positional hint が実際の `KsCell.id` を書き換えると仮定してはならない。非準拠 Cell は元の ID のまま返される。
- 一つの `ForEach` item から複数の Section または Cell を返してよいと無条件に仮定してはならない。現行実装は同じ item ID hint を全結果へ付けるため、同一階層で ID が衝突する。

#### 公開 API と利用例

動的コレクションは `Identifiable` または `id:` KeyPath を独自 `ForEach` へ渡す。個々の要素を別の意味で継続追跡したい場合は明示 ID を指定する。

```swift
struct Item: Identifiable {
    let id: UUID
    let title: String
}

KsSettingsView {
    Section("項目") {
        ForEach(items) { item in
            LabelCell(title: item.title)
        }
    }

    Section {
        LabelCell(title: "固定項目").cellID("fixed-item")
    }
    .sectionID("fixed-section")
}
```

出典: `ios/Sources/KsSettingsViewSwiftUI/DeclarativeDSLIdentity.swift` / `DSLNodes.swift` / `DSLHintRegistry.swift` / `ForEachDSL.swift` / `DSLDiffCalculator.swift` / `KsSettingsView.swift`、`ios/Sources/KsSettingsViewCore/Section.swift`、`ios/Tests/KsSettingsViewSwiftUITests/DeclarativeDSLIdentityTests.swift` / `ForEachDSLTests.swift` / `KsSettingsViewDSLIntegrationTests.swift` / `DSLDiffCalculatorTests.swift` / `DSLVisibilityPreflightTests.swift`、`samples/ios/KsSettingsViewSample/DSLDemoView.swift` / `VisibilityDemoView.swift`、`openspec/specs/settings-view-ios-swiftui/spec.md` Purpose /「Section / Cell の同一性判定戦略」/「DSL → SettingsRootDiff 算出ロジック」Requirements、`kasane/changes/remigrate-concepts/reference/old-concepts/architecture/declarative-tree-identity.md` / `declarative-ui-bridge.md` / `display-state-synchronization.md`、`docs/architecture.md` §2 / §5、`docs/platform-guide-ios.md` §5〜§8

## ADR 候補

- SwiftUI の一般用途には内部 Store を持つ DSL 方式、命令型・高頻度用途には利用者所有 Store 方式を提供し、両方を同じ `SettingsRootStore → KsSettingsViewController` 経路へ収束させる — 出典: `openspec/specs/settings-view-ios-swiftui/spec.md` Purpose /「SwiftUI ラッパ KsSettingsView」Requirement、`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`、選別基準: 能力・コンポーネント境界を越える、将来の決定を制約する
- 宣言ツリーの identity を値内容から分離し、決定的 hint から Section / Cell UUID を再束縛したうえで、構造変更・同一 ID の内容 reconfigure・可視性変更・Theme 更新を別経路へ分ける — 出典: `openspec/specs/settings-view-ios-swiftui/spec.md`「Section / Cell の同一性判定戦略」/「DSL → SettingsRootDiff 算出ロジック」Requirements、`ios/Sources/KsSettingsViewSwiftUI/DeclarativeDSLIdentity.swift` / `DSLNodes.swift` / `DSLDiffCalculator.swift`、選別基準: 覆すコストが高い、能力・コンポーネント境界を越える、将来の決定を制約する

## drift 所見

- 旧 spec と docs は公開 `KsSettingsView` 自体を `UIViewControllerRepresentable` 準拠と説明するが、現行の公開型は `SwiftUI.View` に準拠し、内部 `StoreBackedRepresentable` / `DSLBackedRepresentable` が `UIViewControllerRepresentable` を担う (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI ラッパ KsSettingsView」Requirement / `docs/architecture.md` §6 ↔ `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`)。
- 旧 spec は DSL init を `@SettingsRootBuilder (...) -> [Section]` とし、独自 `ForEach` の content closure に result builder を付けないとしているが、現行コードは `@KsSettingsViewBuilder (...) -> [DSLSectionNode]` を公開し、`ForEach` の content に `@SettingsRootBuilder` / `@KsSectionBuilder` を付けている (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI ラッパ KsSettingsView」/「SwiftUI DSL」Requirements ↔ `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` / `ForEachDSL.swift`)。
- 旧 spec と docs の ID 優先順位は `ForEach item ID > 明示 sectionID/cellID` だが、現行 `DSLHintRegistry.shouldOverride` は既存 `.explicit` を後から付く `.forEach` で上書きしないため、実効優先順位は明示 ID > ForEach item ID である。両者を組み合わせた優先順位テストはない (`openspec/specs/settings-view-ios-swiftui/spec.md`「Section / Cell の同一性判定戦略」Requirement / `docs/architecture.md` §6 / `docs/platform-guide-ios.md` §6 ↔ `ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift` / `ForEachDSL.swift`)。
- 旧 spec と `docs/platform-guide-ios.md` は `.disabled(_:)` を Cell の無効化 modifier として案内するが、現行実装は引数を捨てて元の Cell を返す no-op であり、無効状態を変更しない (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL」Requirement / `docs/platform-guide-ios.md` §9 ↔ `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift`)。
- `sectionHeader` / `sectionFooter` は元 Section の値型 copy を返す契約だが、`copyWith` が `isVisible` を新 Section へ渡さないため、`isVisible: false` の Section にこれらを適用すると既定値 `true` へ戻る。旧 spec の isVisible 保持と modifier copy の説明、および `SectionModifiersTests` の「元値は不変」という限定テストではこの組み合わせを検出していない (`openspec/specs/settings-view-ios-swiftui/spec.md`「SwiftUI DSL における isVisible 引数」Requirement ↔ `ios/Sources/KsSettingsViewSwiftUI/SectionModifiers.swift` / `ios/Sources/KsSettingsViewCore/Section.swift` / `ios/Tests/KsSettingsViewSwiftUITests/SectionModifiersTests.swift`)。
- 独自 `ForEach` の content は builder により一 item から複数 Section / Cell を返せるが、`attachForEachHint` は同じ item ID を各結果へ付けるため、複数結果は同一階層で同じ resolved UUID になる。旧 concepts の「同じ設定ツリー内で ID は一意」という説明、公開 builder 形状と一致しない。テストは一 item 一要素だけを検証している (`kasane/changes/remigrate-concepts/reference/old-concepts/architecture/declarative-tree-identity.md` ↔ `ios/Sources/KsSettingsViewSwiftUI/ForEachDSL.swift` / `DSLNodes.swift` / `ios/Tests/KsSettingsViewSwiftUITests/ForEachDSLTests.swift`)。
- `docs/platform-guide-ios.md` の利用者定義 Cell 手順は `KsCell` 準拠と renderer 登録だけを案内するが、DSL 再評価で fallback ID または `.cellID(_:)` を実際の `KsCell.id` へ反映するには `DSLReidentifiable` への opt-in も必要である。非準拠 Cell は `DSLCellNode.resolvedCell(withID:)` で元の Cell のまま返る (`docs/platform-guide-ios.md` §10 ↔ `ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift` / `ios/Sources/KsSettingsViewCore/DSLCellIdentity.swift`)。

## 用語

- KsSettingsView: Store 方式と DSL 方式を選べる公開 SwiftUI `View`。UIKit Bridge は内部 Representable が担う。
- Store 方式: 利用者所有の `SettingsRootStore` を `KsSettingsViewController` へ接続する利用形態。
- DSL 方式: `KsSettingsViewBuilder` で宣言ツリーを評価し、内部 Store と前回ツリーから差分を適用する利用形態。
- DSLSectionNode / DSLCellNode: Section / Cell 値と identity hint を、resolved UUID の確定まで保持する中間ノード。
- DSLIdentityHint: 明示 ID、ForEach item ID、header / position fallback を区別する identity の入力。
- resolved tree: identity hint を Core の `Section.id` / `KsCell.id` へ反映済みの宣言ツリー。
- reconfigure: 同一 Cell ID を保ったまま表示内容だけを更新すること。iOS DSL では `.replaceCell` がこの意図を表す。
- preflight: 通常 Diff の算出前に可視性変化を検査し、必要なら `.full` だけへ切り替える判定。

## 抽出メモ

- 「SwiftUI と Native Host の Bridge」は旧 `architecture/declarative-ui-bridge.md` の iOS 材料である。Android Compose candidate と統合し、プラットフォーム固有の公開 API 例を併記するのがよい。
- 「宣言ツリーの identity と差分更新」は旧 `architecture/declarative-tree-identity.md` の後継材料であり、Batch A から後送された「表示状態同期」と重なる。identity の意味論は独立概念に残し、`.replaceCell` / visibility / Theme の経路分離は Android Compose / UI host の材料と合わせて「表示状態同期」へ分割する余地がある。
- `DSLDiffCalculator` の各 diff 発行順や `StableHasher` の内部演算は再導出容易な実装詳細なので、概念候補には列挙していない。
- modifier の全 signature 一覧は価値 lint を通らないため省略し、入口となる modifier 群、値型 copy、対応 protocol への opt-in だけを残した。
- ADR 候補は既存 backfill ADR との包含関係を指揮側で確認する。抽出ワーカーとして新 ADR の起票・統合判断は行わない。
