## MODIFIED Requirements

### Requirement: SwiftUI ラッパ KsSettingsView

`KsSettingsView` は `UIViewControllerRepresentable` に準拠し、SwiftUI から `KsSettingsViewController` を直接利用できなければならない (SHALL)。

公開イニシャライザとして以下の **2 種類** を提供しなければならない (MUST)：

1. **Store 方式 init**: `init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic)`
   - Store ベースの経路を維持する
   - パワーユーザー向け（大量データ・無限スクロール・命令型操作が必要なケース）
2. **DSL 方式 init**: `init(style: KsSettingsViewStyle = .classic, @SettingsRootBuilder _ sections: () -> [KsSettingsViewCore.Section])`
   - 宣言的に Cell ツリーを記述する SwiftUI 流儀の経路
   - 内部で `@StateObject private var internalStore: SettingsRootStore` を保持し、`body` 再評価のたびに新旧の宣言ツリーを比較して `SettingsRootDiff` 列を算出、内部 Store の `applyDiff(_:)` に流す
   - 一般用途（静的・数十〜数百セルの典型的な設定画面）向け

両方の init で生成された `KsSettingsView` は、以下の View modifier に対応しなければならない (MUST)：

- `.rootHeader(_ text: String)` / `.rootHeader<V: View>(@ViewBuilder content: () -> V)`：Root Header を文字列または任意 View で指定
- `.rootFooter(_ text: String)` / `.rootFooter<V: View>(@ViewBuilder content: () -> V)`：Root Footer 同上
- `.style(_ style: KsSettingsViewStyle)`：スタイル切替（init 引数と同等）
- `.theme(_ theme: Theme)`：Theme 切替（`Theme` は `KsSettingsViewUI` 所属、フィールドは `UIColor` / `UIFont` 直接保持）

`.theme(_ theme: Theme)` modifier は受け取った Theme を内部 Store の `applyTheme(_:)` に流すか、DSL 方式の場合は内部 `SettingsRootStore` の初期 Theme として保持する (MUST)。**Theme は `SettingsRoot` には含まれないため (MUST NOT)、DSL の `SettingsRootBuilder` も Theme 引数を取らない (MUST NOT)**。

`@Binding<SettingsRoot>` を受け取る旧 init は廃止された状態のままとする (MUST NOT 復活)。

#### Scenario: DSL 方式での初回作成

- **GIVEN** SwiftUI View 内で
  ```swift
  KsSettingsView {
      Section { LabelCell("Hello") }
  }
  .theme(Theme(separatorColor: .systemGray3))
  ```
- **WHEN** SwiftUI が `makeUIViewController(context:)` を呼ぶ
- **THEN** 内部 `SettingsRootStore` が DSL から構築した root と `.theme(_:)` modifier で指定された Theme で初期化され、`KsSettingsViewController` がそれを反映する

#### Scenario: Store 方式での初回作成

- **GIVEN** `let store = SettingsRootStore(initialRoot: ..., initialTheme: ...)` を保持し、`KsSettingsView(store: store)` を View に配置
- **WHEN** SwiftUI が `makeUIViewController(context:)` を呼ぶ
- **THEN** Controller が Store の root / theme を購読して初期描画する

#### Scenario: .theme modifier で Theme 切替

- **GIVEN** `KsSettingsView { ... }.theme(theme1)` が表示中
- **WHEN** State 変化により `.theme(theme2)` に切り替わる
- **THEN** Controller の `applyTheme(theme2)` 経路で View が再評価され、`SettingsRootDiff` は発行されない

#### Scenario: style modifier

- **GIVEN** SwiftUI View 内で `KsSettingsView { ... }.style(.modern)` を記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `style` プロパティが `.modern` に設定され、内部レイアウトが再構築される

### Requirement: SwiftUI DSL

宣言的 DSL（`@resultBuilder` を用いた `SettingsRootBuilder`、`SectionBuilder`）を提供し、SwiftUI 内で Cell ツリーを構築できなければならない (SHALL)。DSL は以下の要素を含む完全な宣言的記法を実現しなければならない (MUST)：

- **`@resultBuilder SettingsRootBuilder`**：`KsSettingsView { ... }` のルートスコープを構成。`KsSettingsViewCore.Section` 値および独自 `ForEach` 関数の戻り値（`[KsSettingsViewCore.Section]`）を受け入れる
- **`@resultBuilder SectionBuilder`**：`Section { ... }` の内部スコープを構成。`any KsCell` 準拠の Cell 値および独自 `ForEach` 関数の戻り値（`[any KsCell]`）を受け入れる
- **独自 `ForEach` 関数（4 オーバーロード）**：
  - ルート用 × `Identifiable` 版：`func ForEach<Data, Element>(_ data: Data, content: (Element) -> [KsSettingsViewCore.Section]) -> [KsSettingsViewCore.Section]`
  - ルート用 × `id:` KeyPath 版：`func ForEach<Data, Element, ID>(_ data: Data, id: KeyPath<Element, ID>, content: (Element) -> [KsSettingsViewCore.Section]) -> [KsSettingsViewCore.Section]`
  - セクション内用 × `Identifiable` 版：`func ForEach<Data, Element>(_ data: Data, content: (Element) -> [any KsCell]) -> [any KsCell]`
  - セクション内用 × `id:` KeyPath 版：`func ForEach<Data, Element, ID>(_ data: Data, id: KeyPath<Element, ID>, content: (Element) -> [any KsCell]) -> [any KsCell]`
- **Section の DSL 専用 init**：
  - `Section("ヘッダ文字列") { /* cells */ }`：文字列ヘッダ
  - `Section(header: SectionAccessory?, footer: SectionAccessory?) { /* cells */ }`：明示 Accessory
  - `Section { /* cells */ }`：ヘッダ・フッタなし
- **Section の View modifier**：
  - `.sectionHeader(_ text: String) -> KsSettingsViewCore.Section`
  - `.sectionHeader<V: View>(@ViewBuilder content: () -> V) -> KsSettingsViewCore.Section`
  - `.sectionFooter(_ text: String) -> KsSettingsViewCore.Section`
  - `.sectionFooter<V: View>(@ViewBuilder content: () -> V) -> KsSettingsViewCore.Section`
  - `.sectionID(_ id: AnyHashable) -> KsSettingsViewCore.Section`
- **Cell の View modifier**（拡張メソッドチェーン、各 modifier は Self を返す）：
  - `.titleColor(_ color: UIColor)`：タイトル色（**型は `UIColor`**、`KsColor` ではない）
  - `.font(_ font: UIFont)`：フォント（**型は `UIFont`**、`KsFont` ではない）
  - `.icon(_ icon: KsImage)`：アイコン（**`KsImage` は `KsSettingsViewUI` 所属**）
  - `.cellHeight(_ height: CGFloat)`
  - `.backgroundColor(_ color: UIColor)`：背景色（**型は `UIColor`**）
  - `.disabled(_ flag: Bool)`
  - `.cellID(_ id: AnyHashable)`：明示 Cell ID
  - すべて自身を copy して新値を返す（イミュータブル、SwiftUI 流儀）
- **`DSLReidentifiable` / `DSLStyleModifiable` protocol の配置モジュール**：
  - `DSLReidentifiable` は `KsSettingsViewCore` モジュールに定義しなければならない (MUST)（`CellStyle` を参照しないため、最下層 Core に置くことで `KsSettingsViewUI` の具象 Cell が準拠できる）
  - `DSLStyleModifiable` は `KsSettingsViewUI` モジュールに定義しなければならない (MUST)（本提案により `CellStyle` が `KsSettingsViewCore` から `KsSettingsViewUI` 所属へ移動したため、`DSLStyleModifiable.withStyle(_ style: CellStyle) -> Self` の宣言が `CellStyle` を参照する以上 Core には置けない）
  - `KsSettingsViewSwiftUI` モジュール内の DSL ロジック（`DSLNodes.swift` / `CellModifiers.swift` 等）は `KsSettingsViewCore`（`DSLReidentifiable`）と `KsSettingsViewUI`（`DSLStyleModifiable`）の両方を import して利用する
- **具象 Cell コンストラクタの `id` デフォルト値規約**：
  - 具象 Cell 実装は `id: UUID` パラメータに **`UUID()` のデフォルト値** を持たせなければならない (SHALL)
  - DSL 経路では `DSLReidentifiable.withDSLID(_:)` で本仕様の優先順位に従う ID に rebind される

DSL は内部 `SettingsRootStore` の初期化に使われると同時に、`body` 再評価のたびに新ツリーを構築して旧ツリーとの Diff を算出する責務を持つ (MUST)。`SettingsRoot` は Root H/F と Theme を保持しないため、DSL も `header` / `footer` / `theme` 引数を取らない (MUST NOT)。Root H/F は `KsSettingsView` の `.rootHeader(...)` / `.rootFooter(...)` modifier、Theme は `.theme(_:)` modifier 経由で指定する。

#### Scenario: Cell modifier の型

- **GIVEN** DSL 内のコード `LabelCell(title: "X").titleColor(.red).backgroundColor(.yellow).font(.preferredFont(forTextStyle: .body))`
- **WHEN** コンパイルする
- **THEN** `.titleColor(_:)` は `UIColor` を受け、`.backgroundColor(_:)` は `UIColor` を受け、`.font(_:)` は `UIFont` を受ける。`KsColor` / `KsFont` を渡そうとするとビルドエラーになる

#### Scenario: .icon modifier の型

- **GIVEN** DSL 内のコード `LabelCell(title: "X").icon(.systemName("bell"))`
- **WHEN** コンパイルする
- **THEN** `.icon(_:)` は `KsImage`（`KsSettingsViewUI` 所属）を受ける。`KsSettingsViewCore` には `KsImage` が存在しないため、`import KsSettingsViewUI` が必要
