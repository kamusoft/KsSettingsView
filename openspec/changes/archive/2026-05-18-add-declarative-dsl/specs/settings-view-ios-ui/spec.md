## MODIFIED Requirements

### Requirement: SwiftUI ラッパ KsSettingsView

`KsSettingsView` は `UIViewControllerRepresentable` に準拠し、SwiftUI から `KsSettingsViewController` を直接利用できなければならない (SHALL)。

公開イニシャライザとして以下の **2 種類** を提供しなければならない (MUST)：

1. **Store 方式 init**: `init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic)`
   - `add-partial-update-native` で導入された経路を維持する
   - パワーユーザー向け（大量データ・無限スクロール・命令型操作が必要なケース）
2. **DSL 方式 init**: `init(style: KsSettingsViewStyle = .classic, @SettingsRootBuilder _ sections: () -> [KsSettingsViewCore.Section])`
   - 宣言的に Cell ツリーを記述する SwiftUI 流儀の経路
   - 内部で `@StateObject private var internalStore: SettingsRootStore` を保持し、`body` 再評価のたびに新旧の宣言ツリーを比較して `SettingsRootDiff` 列を算出、内部 Store の `applyDiff(_:)` に流す
   - 一般用途（静的・数十〜数百セルの典型的な設定画面）向け

両方の init で生成された `KsSettingsView` は、以下の View modifier に対応しなければならない (MUST)：

- `.rootHeader(_ text: String)` / `.rootHeader<V: View>(@ViewBuilder content: () -> V)`：Root Header を文字列または任意 View で指定
- `.rootFooter(_ text: String)` / `.rootFooter<V: View>(@ViewBuilder content: () -> V)`：Root Footer 同上
- `.style(_ style: KsSettingsViewStyle)`：スタイル切替（init 引数と同等）
- `.theme(_ theme: Theme)`：Theme 切替

`add-partial-update-native` で導入された `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier は本提案で **削除** する (MUST NOT)。本ライブラリは運用前のため互換維持は不要。利用者は `.rootHeader(...)` / `.rootFooter(...)` のみを使用する。

<!-- 注: 本 BREAKING 変更は `add-partial-update-native` の archive 完了後、本提案の archive で連続的に適用される。
     archive 順序は proposal.md の依存関係セクション参照。 -->

<!-- 注: 本 MODIFIED Requirement の変更後全文は、archive 済 Source of Truth + `add-partial-update-native` delta + 本提案の変更を
     すべて統合した最終形を記述している。`add-partial-update-native` の先行 archive を前提とする。 -->


`@Binding<SettingsRoot>` を受け取る旧 init は廃止された状態のままとする (MUST NOT 復活)。

#### Scenario: DSL 方式での初回作成

- **GIVEN** SwiftUI View 内で
  ```swift
  KsSettingsView {
      Section { LabelCell("Hello") }
  }
  ```
  と記述
- **WHEN** SwiftUI が `makeUIViewController(context:)` を呼ぶ
- **THEN** 内部 `@StateObject` の `SettingsRootStore` が初期 root（DSL の評価結果）で構築され、`KsSettingsViewController(store: internalStore, style: .classic)` が生成される

#### Scenario: DSL 方式での @State 変更による再描画

- **GIVEN** `@State var userName = "Taro"` と
  ```swift
  KsSettingsView {
      Section { LabelCell(userName) }
  }
  ```
  が画面表示中
- **WHEN** `userName = "Hanako"` を代入
- **THEN** body 再評価で新ツリーが構築され、Cell ID 同一・内容違いと判定されて `.replaceCell` Diff が内部 Store 経由で Controller に流れ、該当 Cell のみが再描画される（周辺 Cell は無傷）

#### Scenario: Store 方式での初回作成

- **GIVEN** SwiftUI View 内で `@StateObject var store = SettingsRootStore(initialRoot: ...)` 宣言
- **WHEN** `KsSettingsView(store: store)` を body から返す
- **THEN** SwiftUI は `makeUIViewController(context:)` を呼び、`KsSettingsViewController(store: store, style: .classic)` が生成される（既存挙動を維持）

#### Scenario: Store 方式でのメソッド呼び出しによる再描画

- **GIVEN** `KsSettingsView(store: store)` が画面表示中
- **WHEN** ボタン押下などで `store.insertCell(newCell, in: sectionID, at: 0)` を呼ぶ
- **THEN** Controller が Store の Diff Publisher を購読しており、`applyDiff(.insertCell(...))` が呼ばれて新しい Cell 行が挿入アニメーションで追加される（既存挙動を維持）

#### Scenario: rootHeader modifier の文字列指定

- **GIVEN** SwiftUI View 内で
  ```swift
  KsSettingsView { ... }
      .rootHeader("プロフィール")
  ```
  と記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `rootHeader` プロパティが `RootAccessory.text("プロフィール")` に設定される

#### Scenario: rootHeader modifier の任意 View 指定

- **GIVEN** SwiftUI View 内で
  ```swift
  KsSettingsView { ... }
      .rootHeader { ProfileCard() }
  ```
  と記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `rootHeader` プロパティが `RootAccessory.view(KsAnyView.swiftUI { ProfileCard() })` に設定される

#### Scenario: rootFooter modifier の任意 View 指定

- **GIVEN** SwiftUI View 内で
  ```swift
  KsSettingsView { ... }
      .rootFooter { Text("© 2026") }
  ```
  と記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `rootFooter` プロパティが任意 View ベースの `RootAccessory.view(...)` に設定される

#### Scenario: 旧 .header(...) modifier はコンパイルエラー

- **GIVEN** SwiftUI View 内で `KsSettingsView(store: store).header(.text("プロフィール"))` を記述
- **WHEN** ビルドする
- **THEN** `.header(_ accessory: RootAccessory?)` 拡張メソッドは本提案で削除されているため、コンパイルエラーとなる。利用者は `.rootHeader("プロフィール")` への書き換えが必要

#### Scenario: style modifier

- **GIVEN** SwiftUI View 内で `KsSettingsView { ... }.style(.modern)` を記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `style` プロパティが `.modern` に設定され、内部レイアウトが再構築される

### Requirement: SwiftUI DSL

宣言的 DSL（`@resultBuilder` を用いた `SettingsRootBuilder`、`SectionBuilder`）を提供し、SwiftUI 内で Cell ツリーを構築できなければならない (SHALL)。DSL は以下の要素を含む完全な宣言的記法を実現しなければならない (MUST)：

- **`@resultBuilder SettingsRootBuilder`**：`KsSettingsView { ... }` のルートスコープを構成。`KsSettingsViewCore.Section` 値および独自 `ForEach` 関数の戻り値（`[KsSettingsViewCore.Section]`）を受け入れる
- **`@resultBuilder SectionBuilder`**：`Section { ... }` の内部スコープを構成。`any KsCell` 準拠の Cell 値および独自 `ForEach` 関数の戻り値（`[any KsCell]`）を受け入れる
- **独自 `ForEach` 関数（4 オーバーロード）**：
  - ルート用 × `Identifiable` 版：`func ForEach<Data, Element>(_ data: Data, content: (Element) -> [KsSettingsViewCore.Section]) -> [KsSettingsViewCore.Section]`（`Data: RandomAccessCollection, Element == Data.Element, Element: Identifiable`）
  - ルート用 × `id:` KeyPath 版：`func ForEach<Data, Element, ID>(_ data: Data, id: KeyPath<Element, ID>, content: (Element) -> [KsSettingsViewCore.Section]) -> [KsSettingsViewCore.Section]`（`Data: RandomAccessCollection, Element == Data.Element, ID: Hashable`）
  - セクション内用 × `Identifiable` 版：`func ForEach<Data, Element>(_ data: Data, content: (Element) -> [any KsCell]) -> [any KsCell]`（`Data: RandomAccessCollection, Element == Data.Element, Element: Identifiable`）
  - セクション内用 × `id:` KeyPath 版：`func ForEach<Data, Element, ID>(_ data: Data, id: KeyPath<Element, ID>, content: (Element) -> [any KsCell]) -> [any KsCell]`（`Data: RandomAccessCollection, Element == Data.Element, ID: Hashable`）
  - 注: content クロージャ自体は通常の関数クロージャとして受け取り、`@resultBuilder` 属性は付けない（content 内で `SettingsRootBuilder` / `SectionBuilder` を直接ネストする記述は別途 `Section { ... }` イニシャライザや `SettingsRoot { ... }` 等で実現される）
- **Section の DSL 専用 init**：
  - `Section("ヘッダ文字列") { /* cells */ }`：文字列ヘッダ
  - `Section(header: SectionAccessory?, footer: SectionAccessory?) { /* cells */ }`：明示 Accessory
  - `Section { /* cells */ }`：ヘッダ・フッタなし
- **Section の View modifier**：
  - `.sectionHeader(_ text: String) -> KsSettingsViewCore.Section`
  - `.sectionHeader<V: View>(@ViewBuilder content: () -> V) -> KsSettingsViewCore.Section`：任意 View ヘッダ
  - `.sectionFooter(_ text: String) -> KsSettingsViewCore.Section`
  - `.sectionFooter<V: View>(@ViewBuilder content: () -> V) -> KsSettingsViewCore.Section`
  - `.sectionID(_ id: AnyHashable) -> KsSettingsViewCore.Section`：明示 Section ID
- **Cell の View modifier**（拡張メソッドチェーン、各 modifier は Self を返す）：
  - `.font(_ font: KsFont)` / `.icon(_ icon: KsIcon)` / `.cellHeight(_ height: CGFloat)` / `.backgroundColor(_ color: KsColor)` / `.disabled(_ flag: Bool)`
  - `.cellID(_ id: AnyHashable)`：明示 Cell ID
  - すべて自身を copy して新値を返す（イミュータブル、SwiftUI 流儀）
- **`DSLReidentifiable` / `DSLStyleModifiable` protocol の配置モジュール**：
  - これらの protocol は `KsSettingsViewCore` モジュールに定義しなければならない (MUST)
  - 後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `KsSettingsViewUI` モジュールに配置されるため、`KsSettingsViewUI` の Cell が `DSLReidentifiable` を準拠できるよう、最下層 Core モジュールに置く（`KsSettingsViewUI → KsSettingsViewSwiftUI` の循環依存回避）
  - `KsSettingsViewSwiftUI` モジュール内の DSL ロジック（`DSLNodes.swift` 等）は Core に置かれた protocol を import して利用する
  - 当初 `KsSettingsViewSwiftUI` モジュールに定義されていたが本提案で Core に移動する
- **具象 Cell コンストラクタの `id` デフォルト値規約**：
  - 具象 Cell 実装（`LabelCell` 等、後続 `add-cell-types-*` で実装）は `id: UUID` パラメータに **`UUID()` のデフォルト値** を持たせなければならない (SHALL)
  - DSL 経路では `DSLReidentifiable.withDSLID(_:)` で本仕様の優先順位に従う ID に rebind されるため、デフォルト UUID 値が最終 Cell ID として表面化することはない
  - 利用者は DSL 内で `LabelCell(title: "...")` のように `id` 引数省略で記述できる

DSL は内部 `SettingsRootStore` の初期化に使われると同時に、`body` 再評価のたびに新ツリーを構築して旧ツリーとの Diff を算出する責務を持つ (MUST)。`SettingsRoot` は Root H/F を保持しないため、DSL も `header` / `footer` 引数を取らない (MUST NOT)。Root H/F は `KsSettingsView` の `.rootHeader(...)` / `.rootFooter(...)` modifier 経由で指定する。

#### Scenario: 基本的な DSL 記述

- **GIVEN** SwiftUI コード内で
  ```swift
  let view = KsSettingsView {
      Section("ユーザー") {
          LabelCell("名前")
          CommandCell("ログアウト") { /* action */ }
      }
      Section { LabelCell("一行Section") }
  }
  ```
- **WHEN** view を評価する
- **THEN** 2 つの Section、合計 3 つの Cell が宣言ツリーとして構築され、内部 Store の初期 root に反映される

#### Scenario: ForEach（Identifiable 版・セクション内）

- **GIVEN** `Identifiable` 準拠の `items: [Todo]` と
  ```swift
  KsSettingsView {
      Section("Todo") {
          ForEach(items) { item in
              LabelCell(item.name)
          }
      }
  }
  ```
- **WHEN** 評価する
- **THEN** items.count 個の Cell が Section 内に展開され、各 Cell の Cell ID は `item.id` から導出される

#### Scenario: ForEach（id: KeyPath 版・セクション内）

- **GIVEN** `Identifiable` でない `items: [LegacyModel]` と `id: \.myKey` を渡す
  ```swift
  Section {
      ForEach(items, id: \.myKey) { item in
          LabelCell(item.name)
      }
  }
  ```
- **WHEN** 評価する
- **THEN** items.count 個の Cell が展開され、各 Cell の Cell ID は `item.myKey` から導出される

#### Scenario: ForEach（ルート版・Section 群を展開）

- **GIVEN** `Identifiable` 準拠の `groups: [Group]` と
  ```swift
  KsSettingsView {
      ForEach(groups) { group in
          Section(group.name) {
              ForEach(group.items) { item in
                  LabelCell(item.name)
              }
          }
      }
  }
  ```
- **WHEN** 評価する
- **THEN** groups.count 個の Section が展開され、Section ID は `group.id` から、Cell ID は `item.id` から導出される

#### Scenario: Section H/F modifier の文字列指定

- **GIVEN**
  ```swift
  Section {
      LabelCell("一行目")
  }
  .sectionHeader("見出し")
  .sectionFooter("注釈")
  ```
- **WHEN** 評価する
- **THEN** Section の `header` が `SectionAccessory.text("見出し")`、`footer` が `SectionAccessory.text("注釈")` となる

#### Scenario: Section H/F modifier の任意 View 指定

- **GIVEN**
  ```swift
  Section { LabelCell("一行目") }
      .sectionHeader { HStack { Image(systemName: "bell"); Text("通知").bold() } }
  ```
- **WHEN** 評価する
- **THEN** Section の `header` が `SectionAccessory.view(KsAnyView.swiftUI { ... })` となり、UI 層が `UIHostingConfiguration` で任意 View を描画する

#### Scenario: Cell modifier の連鎖適用

- **GIVEN**
  ```swift
  LabelCell("名前", value: "Taro")
      .font(.headline)
      .icon(.system("person"))
      .cellHeight(60)
  ```
- **WHEN** 評価する
- **THEN** 元 Cell の値を copy した新 Cell が返され、`style.font` / `style.icon` / `style.cellHeight` が指定値に上書きされる（元 Cell は不変）

#### Scenario: 明示 .cellID(_:) による Cell 同一性指定

- **GIVEN**
  ```swift
  Section {
      LabelCell("動的Cell").cellID("dynamic-cell-1")
  }
  ```
- **WHEN** body 再評価をまたいで評価する
- **THEN** Cell ID が `"dynamic-cell-1"` の `AnyHashable` 値として固定され、Section 内位置や Cell 型に依存しない安定 ID となる

#### Scenario: 明示 .sectionID(_:) による Section 同一性指定

- **GIVEN**
  ```swift
  KsSettingsView {
      Section { LabelCell("動的Section") }.sectionID("dynamic-section-1")
  }
  ```
- **WHEN** body 再評価をまたいで評価する
- **THEN** Section ID が `"dynamic-section-1"` の `AnyHashable` 値として固定される

### Requirement: メモリリーク防止

`KsSettingsViewController` および `KsSettingsView` は `deinit` 時に内部 `UICollectionView` の DataSource、Delegate、registered Cell の参照、および Store の Diff Publisher 購読をすべて解放しなければならない (MUST)。SwiftUI ラッパの Coordinator が Store を強参照する場合、Controller が破棄された時点で購読が解除されること。

**DSL 方式での `@StateObject` 内部 Store のライフサイクル**: `KsSettingsView` の View identity が維持される間は内部 Store も保持され、View が破棄されると Store も解放されなければならない (MUST)。`@StateObject` の標準的なライフサイクル（SwiftUI Runtime 管理）に従う。

<!-- 注: 本 MODIFIED Requirement の変更後全文は、`add-partial-update-native` で追加された
     「Store 購読の解除」段落と Scenario を継承し、本提案の DSL 方式向け `@StateObject` ライフサイクル段落と Scenario を追加した最終形を記述している。 -->

#### Scenario: ViewController が deinit される

- **GIVEN** `KsSettingsViewController` を `present` したのち `dismiss` する
- **WHEN** 親 ViewController から開放後 1 ランループ以上経過する
- **THEN** `KsSettingsViewController` インスタンスは deinit され、`weak var` で保持していた参照が `nil` になる

#### Scenario: Store 購読の解除

- **GIVEN** `KsSettingsViewController(store: store)` が deinit される
- **WHEN** Controller の deinit を観察する
- **THEN** Store の Diff Publisher 購読は解除され、Controller への参照が残らない（Store が長命であっても Controller がリークしない）

#### Scenario: DSL 方式の内部 Store が View 破棄時に解放される

- **GIVEN** `KsSettingsView { Section { ... } }` を含む View が画面に表示中
- **WHEN** 親 View 階層から `KsSettingsView` が外れ、View identity が失われる
- **THEN** 内部 `@StateObject` の Store も解放される（`@StateObject` の標準ライフサイクル）

## ADDED Requirements

### Requirement: DSL → SettingsRootDiff 算出ロジック

`KsSettingsViewSwiftUI` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsViewController.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは以下の手順に従わなければならない (MUST)：

1. **Section レベルの突合**：
   - 旧ツリーと新ツリーの Section ID 集合を比較
   - 新ツリーにあって旧ツリーにない Section ID → `.insertSection(at:, section:)` Diff を発行
   - 旧ツリーにあって新ツリーにない Section ID → `.removeSection(sectionID:)` Diff を発行
   - 両ツリーに存在し位置が異なる Section ID → `.moveSection(from:, to:)` Diff を発行
   - 両ツリーに存在し H/F（`SectionAccessory`）が異なる Section → `.updateAccessory(target: .sectionHeader/.sectionFooter, accessory:)` Diff を発行
2. **各 Section 内の Cell レベルの突合**：
   - 新セクションにあって旧セクションにない Cell ID → `.insertCell(sectionID:, at:, cell:)` Diff を発行
   - 旧セクションにあって新セクションにない Cell ID → `.removeCell(cellID:)` Diff を発行
   - 両セクションに存在し位置が異なる Cell ID → `.moveCell(cellID:, to:)` Diff を発行
   - 両セクションに存在し Cell 値の `Equatable` 比較で差がある Cell ID → `.replaceCell(cellID:, new:)` Diff を発行
3. **Root H/F の突合**：
   - `.rootHeader(...)` / `.rootFooter(...)` modifier の値が変化した場合 → `.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff を発行
4. **Theme の突合**：
   - 旧 Theme と新 Theme が異なる場合 → `.updateTheme(newTheme)` Diff を発行
5. **Cell 値の比較対象**：
   - `KsAnyView` を含むフィールドは比較対象から除外（既存仕様、`Hashable` 非準拠）
   - その他のフィールドは `KsCell` の `Hashable`（`Equatable`）契約で自動比較
6. **任意 View 形式（`.view(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.view` ケース同士・`RootAccessory.view` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `updateAccessory` Diff は **発行しない**
   - 異なるケース（`.text` → `.view` または `.view` → `.text`、`nil` → `.view` 等）の場合のみ `updateAccessory` Diff を発行

#### Scenario: Cell 内容変更時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`（Section ID・Cell ID は同じ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.replaceCell(cellID: <same>, new: LabelCell("Hanako"))` のみが発行される（周辺の何も変わらない要素は Diff にならない）

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`（A の Cell ID は同じ、B は新規）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.insertCell(sectionID: <same>, at: 1, cell: LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.removeCell(cellID: <B のID>)` のみが発行される

#### Scenario: Section 追加時の Diff 発行

- **GIVEN** 旧ツリーが Section 1 つのみ、新ツリーが Section 2 つ（既存 + 末尾追加）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.insertSection(at: 1, section: <newSection>)` のみが発行される

#### Scenario: Section 削除時の Diff 発行

- **GIVEN** 旧ツリーが Section 2 つ（Section A + Section B、各々 Section ID は安定）、新ツリーが Section 1 つ（Section A のみ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.removeSection(sectionID: <B のID>)` のみが発行される（Section A 内の Cell は完全保持）

#### Scenario: Section H/F 変更時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader("旧")` と新ツリー `Section { LabelCell("A") }.sectionHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .sectionHeader(sectionID), accessory: .section(.text("新")))` が発行される

#### Scenario: Root H/F 変更時の Diff 発行

- **GIVEN** 旧 modifier `.rootHeader("旧")` と新 modifier `.rootHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .rootHeader, accessory: .root(.text("新")))` が発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`（同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveCell(cellID: <B のID>, to: 2)` または `.moveCell(cellID: <C のID>, to: 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない（Cell 値は等価のため `replaceCell` は発行されない）

#### Scenario: Section 移動時の Diff 発行

- **GIVEN** 旧ツリーで Section 3 つが並んでいる状態と、新ツリーで Section の順序が変わった状態（各 Section ID は不変）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveSection(from: <旧位置>, to: <新位置>)` Diff が発行され、Section 内の Cell は再構築されずに移動アニメーションが走る

#### Scenario: 任意 View 形式の Section H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardA() }` と新ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardB() }`（同 Section ID、Header が両方 `.view` ケース）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`.view` ケース同士は等価とみなされ `updateAccessory` Diff は発行されない。任意 View の中身更新は既存仕様通り `UIHostingConfiguration` の再構成に委ねられる

#### Scenario: 任意 View 形式の Root H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧 modifier `.rootHeader { HeaderA() }` と新 modifier `.rootHeader { HeaderB() }`（両方とも任意 View 指定）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 同じ `.view` ケース同士は等価とみなされ、`updateAccessory(target: .rootHeader, ...)` Diff は発行されない

#### Scenario: Section H/F のケース変化（text → view）で updateAccessory 発行

- **GIVEN** 旧ツリー `Section { ... }.sectionHeader("文字列")` と新ツリー `Section { ... }.sectionHeader { CustomHeader() }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.text` ケースから `.view` ケースへの遷移は検出可能なため `.updateAccessory(target: .sectionHeader(...), accessory: .section(.view(...)))` が発行される

#### Scenario: 同一ツリーで Diff 空

- **GIVEN** 旧ツリーと新ツリーが完全に同一（Cell の Equatable 比較で全一致）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 発行される Diff 列は空となり、`applyDiff` は呼ばれない（無駄な再描画を防止）

### Requirement: Section / Cell の同一性判定戦略

`KsSettingsViewSwiftUI` の DSL → Diff 算出ロジックは、以下の優先順位で Section / Cell の ID を採番しなければならない (SHALL)。

**Section ID 判定の優先順位**：

1. ForEach 配下：`item.id`（`Identifiable.id` または `id:` KeyPath の値）を `AnyHashable` でラップして採用
2. `.sectionID(_ id: AnyHashable)` modifier が明示指定されている場合：その値を採用
3. ヘッダが `SectionAccessory.text(String)` の場合：`(ルート位置, ヘッダ文字列)` のハッシュを採用
4. フォールバック：ルート位置（`rootIdx`）ベースのハッシュを採用

**Cell ID 判定の優先順位**：

1. ForEach 配下：`item.id` を `AnyHashable` でラップして採用
2. `.cellID(_ id: AnyHashable)` modifier が明示指定されている場合：その値を採用
3. デフォルト：`(SectionID, Section 内位置, Cell 型)` のハッシュを採用

判定された ID は `KsCellID` 型または Section 識別子（`UUID` 相当）に変換され、`SettingsRootDiff` 経路で Native UI 層に渡される。同じ DSL 記述に対して body 再評価をまたいでも安定した ID を返さなければならない (MUST)。

ヘッダなし複数 Section が動的に追加・削除される構造は **位置ベースのフォールバック** に依存するため、Section 追加・削除で全 ID がずれるリスクがあることを明記する。利用者には `ForEach` または `.sectionID(_:)` の明示指定を推奨ドキュメント指針として案内する。

#### Scenario: 完全静的構造での body 再評価耐性

- **GIVEN**
  ```swift
  KsSettingsView {
      Section { LabelCell("A"); SwitchCell("B") }.sectionHeader("ユーザー")
      Section { CommandCell("C") }.sectionHeader("詳細")
  }
  ```
- **WHEN** body を 2 回評価する
- **THEN** 1 回目と 2 回目で各 Section ID・Cell ID が完全に一致する（位置 + ヘッダ文字列のハッシュベース）

#### Scenario: ForEach 配下の Cell ID 引き継ぎ

- **GIVEN** `Identifiable` の `items: [Todo]` と `ForEach(items) { item in LabelCell(item.name) }`
- **WHEN** items に新規 Todo を append（既存 item の id は変わらない）
- **THEN** 既存 Cell の Cell ID は不変、新規 Todo の id から導出された Cell ID のみが新規追加され、`.insertCell` Diff が発行される

#### Scenario: ForEach 配下の Section ID 引き継ぎ

- **GIVEN** `Identifiable` の `groups: [Group]` と `ForEach(groups) { group in Section { ... } }`
- **WHEN** groups の先頭に新規 Group を insert
- **THEN** 既存 Section の Section ID は不変、新規 Group の id から導出された Section ID のみが先頭に追加され、`.insertSection(at: 0, ...)` Diff が発行される

#### Scenario: ヘッダ文字列ベースの Section ID 安定性

- **GIVEN**
  ```swift
  Section { LabelCell("A") }.sectionHeader("固定見出し")
  ```
  と同じ記述を別 body 評価で再構築
- **WHEN** Section ID を比較
- **THEN** `(rootIdx, "固定見出し")` のハッシュで一致する

#### Scenario: ヘッダなし複数 Section の動的追加（フォールバック挙動）

- **GIVEN** 旧ツリー
  ```swift
  Section { LabelCell("A") }
  Section { LabelCell("B") }
  ```
  と新ツリー（先頭に新 Section 追加）
  ```swift
  Section { LabelCell("X") }  // 新規
  Section { LabelCell("A") }
  Section { LabelCell("B") }
  ```
- **WHEN** Diff 算出ロジックを実行
- **THEN** 位置ベースのフォールバックにより、すべての Section の ID がずれて検出される（実装上の制約）。利用者は `.sectionID(_:)` 明示または `ForEach` の利用を推奨される

#### Scenario: 明示 .sectionID(_:) による動的追加の安定化

- **GIVEN** 旧ツリー
  ```swift
  Section { LabelCell("A") }.sectionID("a")
  Section { LabelCell("B") }.sectionID("b")
  ```
  と新ツリー
  ```swift
  Section { LabelCell("X") }.sectionID("x")  // 新規
  Section { LabelCell("A") }.sectionID("a")
  Section { LabelCell("B") }.sectionID("b")
  ```
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.insertSection(at: 0, section: <x>)` のみが発行され、既存 Section は完全保持される

#### Scenario: Cell modifier 適用でも Cell ID が維持される

- **GIVEN** 旧ツリー `LabelCell("名前")` と新ツリー `LabelCell("名前").font(.headline)`（同位置・同型・同 title）
- **WHEN** Diff 算出ロジックを実行
- **THEN** Cell ID は同じ `(SectionID, 0, LabelCell)` のハッシュで一致するが、Cell 値（`style.font`）が違うため `.replaceCell` Diff が発行される

### Requirement: DSL での Bindingセル規約

`KsSettingsViewSwiftUI` の DSL は、双方向バインド対応 Cell（後続 `add-cell-types-*` 提案で追加される `SwitchCell` / `EntryCell` / `PickerCell` 等）が `@Binding<T>` 引数を受け取れる規約を支援しなければならない (SHALL)。

- 各双方向バインド Cell は SwiftUI 流儀で `init(_ title: String, isOn: Binding<Bool>)` などのイニシャライザを公開する
- Binding は Cell 値型の内部に保持され、`wrappedValue` の比較で Diff 算出時の値判定に使用される
- ユーザー操作で Cell の値が変わった場合（例：Switch の Toggle）、Native 層から SwiftUI の `@Binding` 経由で元の `@State` に書き戻される
- 高頻度更新パス（EntryCell の連続入力等）は Native 側で 200ms debounce 後に `updateCellValue(cellId:value:)` を呼び、Diff 経路を通らない直行ルートで反映される
- 本提案では具象 Cell 型の追加は行わない（`add-cell-types-*` で実装）が、DSL がこの Binding 規約をサポートする責務を持つ
- **Binding セルの Cell ID 採番**: Binding セルの内部 `id` フィールドを `UUID()` 等で自動採番してはならない (MUST NOT)。Cell ID は本 capability の `Section / Cell の同一性判定戦略` Requirement に従い、`ForEach` 配下なら `item.id`、`.cellID(_:)` modifier があればその値、デフォルトは `(SectionID, Section 内位置, Cell 型)` のハッシュを採用する。body 再評価のたびに新規 `UUID` を生成する実装は Diff 同一性判定を破壊するため禁止する

#### Scenario: Binding付き Cell の DSL 記述（規約検証）

- **GIVEN** `@State var isOn = true` と
  ```swift
  Section {
      SwitchCell("通知", isOn: $isOn)
  }
  ```
  の DSL 記述（`SwitchCell` は後続提案で実装）
- **WHEN** DSL → Diff 算出ロジックがこの Cell を扱う
- **THEN** Cell 値の比較では `isOn.wrappedValue` を参照し、`@State` の値が変わった場合のみ `.replaceCell` Diff（または `updateCellValue` 直行パス）が発行される

#### Scenario: ユーザー操作による @State への書き戻し（規約検証）

- **GIVEN** `@State var isOn = false` の状態で `SwitchCell("通知", isOn: $isOn)` が画面表示中
- **WHEN** ユーザーが Switch を ON にする
- **THEN** Native 層のイベントが SwiftUI 経由で `isOn` を `true` に書き戻し、`@State` の更新で body 再評価が走るが、Diff 算出ロジックで値が一致するため `.replaceCell` Diff は発行されない（無限ループ防止）
