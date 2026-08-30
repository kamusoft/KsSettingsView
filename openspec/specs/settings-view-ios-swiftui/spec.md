# settings-view-ios-swiftui Specification

## Purpose

`settings-view-ios-swiftui` は、`KsSettingsViewUI`（iOS）の **宣言的 UI ラッパ層** を担う capability である。SwiftUI から `settings-view-ios-host` の `KsSettingsViewController` を `UIViewControllerRepresentable` 経由で薄くラップする `KsSettingsView` View と、宣言的に Cell ツリーを記述する `KsSettingsView { ... }` DSL を定義する。Section / Cell の同一性判定戦略（明示 ID → `ForEach` Identifiable/KeyPath → 構造位置 fallback の優先順位）、DSL ツリーから `SettingsRootDiff` を算出するロジック、`@State` / `@Binding` 駆動の Binding セル規約も本 capability に含まれる。iOS の UICollectionView / DiffableDataSource 基盤・スタイル切替・Theme 変換などは下位の 3 spec（host / style / theme-bridge）に分離されており、本 capability はそれらの上に「SwiftUI 流の書き味」を提供する立場である。

## Requirements
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
  - 具象 Cell 実装（`LabelCell` 等、後続 `add-cell-types-*` で実装）は `id: UUID` パラメータに **`UUID()` のデフォルト値** を持たせなければならない (SHALL)
  - DSL 経路では `DSLReidentifiable.withDSLID(_:)` で本仕様の優先順位に従う ID に rebind されるため、デフォルト UUID 値が最終 Cell ID として表面化することはない
  - 利用者は DSL 内で `LabelCell(title: "...")` のように `id` 引数省略で記述できる

DSL は内部 `SettingsRootStore` の初期化に使われると同時に、`body` 再評価のたびに新ツリーを構築して旧ツリーとの Diff を算出する責務を持つ (MUST)。`SettingsRoot` は Root H/F と Theme を保持しないため、DSL も `header` / `footer` / `theme` 引数を取らない (MUST NOT)。Root H/F は `KsSettingsView` の `.rootHeader(...)` / `.rootFooter(...)` modifier、Theme は `.theme(_:)` modifier 経由で指定する。

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
      .font(.preferredFont(forTextStyle: .headline))
      .icon(.systemName("person"))
      .cellHeight(60)
  ```
- **WHEN** 評価する
- **THEN** 元 Cell の値を copy した新 Cell が返され、`style.titleFont` / `icon` / `style.cellHeight` が指定値に上書きされる（元 Cell は不変）

#### Scenario: Cell modifier の型

- **GIVEN** DSL 内のコード `LabelCell(title: "X").titleColor(.red).backgroundColor(.yellow).font(.preferredFont(forTextStyle: .body))`
- **WHEN** コンパイルする
- **THEN** `.titleColor(_:)` は `UIColor` を受け、`.backgroundColor(_:)` は `UIColor` を受け、`.font(_:)` は `UIFont` を受ける。`KsColor` / `KsFont` を渡そうとするとビルドエラーになる

#### Scenario: .icon modifier の型

- **GIVEN** DSL 内のコード `LabelCell(title: "X").icon(.systemName("bell"))`
- **WHEN** コンパイルする
- **THEN** `.icon(_:)` は `KsImage`（`KsSettingsViewUI` 所属）を受ける。`KsSettingsViewCore` には `KsImage` が存在しないため、`import KsSettingsViewUI` が必要

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

### Requirement: DSL → SettingsRootDiff 算出ロジック

`KsSettingsViewSwiftUI` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsViewController.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは以下の手順に従わなければならない (MUST)：

1. **可視性変化の preflight 検出**：
   - 旧ツリーと新ツリーの間で、同一 ID の Section について `isVisible` の値が変化している、または同一 Cell ID について `(cell as? VisibilityAware)?.isVisible ?? true` の値が変化していることを検出した場合、通常の section / cell 差分算出には進まず、`.full(newRoot)` Diff のみを発行して終了しなければならない (MUST)。
   - 可視性差分は通常の `.replaceCell`（reconfigure 経路）に乗せてはならない (MUST NOT)。可視性変化は構造同期上の追加・削除として表現される必要があり、reconfigure 経路では正しく扱えないため。
2. **Section レベルの突合**（可視性差分が無い場合に実施）：
   - 旧ツリーと新ツリーの Section ID 集合を比較
   - 新ツリーにあって旧ツリーにない Section ID → `.insertSection(at:, section:)` Diff を発行
   - 旧ツリーにあって新ツリーにない Section ID → `.removeSection(sectionID:)` Diff を発行
   - 両ツリーに存在し位置が異なる Section ID → `.moveSection(from:, to:)` Diff を発行
   - 両ツリーに存在し H/F（`SectionAccessory`）が異なる Section → `.updateAccessory(target: .sectionHeader/.sectionFooter, accessory:)` Diff を発行
3. **各 Section 内の Cell レベルの突合**：
   - 新セクションにあって旧セクションにない Cell ID → `.insertCell(sectionID:, at:, cell:)` Diff を発行
   - 旧セクションにあって新セクションにない Cell ID → `.removeCell(cellID:)` Diff を発行
   - 両セクションに存在し位置が異なる Cell ID → `.moveCell(cellID:, to:)` Diff を発行
   - 両セクションに存在し Cell 値が異なる Cell ID → `.replaceCell(cellID:, new:)` Diff を発行（**`replaceCell` は同一 id の内容更新を表し、`reconfigureItems` 経路で反映される。セルの破棄・再生成を意味しない**）
4. **Root H/F の突合**：
   - `.rootHeader(...)` / `.rootFooter(...)` modifier の値が変化した場合 → `.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff を発行
5. **Theme の突合**：
   - Theme は `SettingsRootDiff` には含まれない (MUST NOT)。Theme の変化は `.theme(_:)` modifier の再評価で `store.applyTheme(newTheme)` を呼ぶ経路で反映される（独立 API）
6. **Cell 値の比較対象**：
   - `KsAnyView` を含むフィールドは比較対象から除外（既存仕様、`Hashable` 非準拠）
   - その他のフィールドは `KsCell` の `Hashable`（`Equatable`）契約で比較し、**差があれば内容更新として `.replaceCell`（reconfigure 経路）を発行する**。`.replaceCell` は構造同期（snapshot の item 集合・順序）を変更せず、同一 id のセル内容の reconfigure として扱われる
   - 注: プラットフォーム間で内容更新の経路が異なる。iOS は DSL から `.replaceCell` を発行し `applyDiff` が `reconfigureItems` で反映する。Android（`settings-view-android-ui` の DSL → SettingsRootDiff 算出ロジック（Compose））は内容変化で `ReplaceCell` を発行せず、アダプタが ViewHolder を直接部分更新する。いずれも上位原則「構造同期は id 同一性のみ・内容更新はセルを再生成しない」に従う（経路の差は実装都合であり原則は共通）
7. **任意 View 形式（`.view(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.view` ケース同士・`RootAccessory.view` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `updateAccessory` Diff は **発行しない**
   - 異なるケース（`.text` → `.view` または `.view` → `.text`、`nil` → `.view` 等）の場合のみ `updateAccessory` Diff を発行

#### Scenario: Cell 内容変更時の Diff 発行（reconfigure 経路）

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`(Section ID・Cell ID は同じ)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.replaceCell(cellID: <same>, new: LabelCell("Hanako"))` のみが発行される。この Diff は構造同期（item 集合・順序）を変えず、`reconfigureItems` で同一セルの内容のみ更新される（セル破棄・再生成は伴わない）

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`(A の Cell ID は同じ、B は新規)
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

#### Scenario: チェック系の内容変化はセルを再生成しない

- **GIVEN** 旧ツリー `Section { CheckboxCell("規約", isChecked: false) }` と新ツリー `Section { CheckboxCell("規約", isChecked: true) }`(同 Section ID・Cell ID)
- **WHEN** Diff 算出 → applyDiff を実行
- **THEN** `.replaceCell` が発行され `reconfigureItems` で同一セルの内容のみ更新される。セルの破棄・再生成（reload）や行全体のちらつきは発生しない

#### Scenario: Section H/F 変更時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader("旧")` と新ツリー `Section { LabelCell("A") }.sectionHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .sectionHeader(sectionID), accessory: .section(.text("新")))` が発行される

#### Scenario: Root H/F 変更時の Diff 発行

- **GIVEN** 旧 modifier `.rootHeader("旧")` と新 modifier `.rootHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .rootHeader, accessory: .root(.text("新")))` が発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`(同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveCell(cellID: <B のID>, to: 2)` または `.moveCell(cellID: <C のID>, to: 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない（Cell 値は等価のため `replaceCell` は発行されない）

#### Scenario: Section 移動時の Diff 発行

- **GIVEN** 旧ツリーで Section 3 つが並んでいる状態と、新ツリーで Section の順序が変わった状態（各 Section ID は不変）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveSection(from: <旧位置>, to: <新位置>)` Diff が発行され、Section 内の Cell は再構築されずに移動アニメーションが走る

#### Scenario: 任意 View 形式の Section H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardA() }` と新ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardB() }`(同 Section ID、Header が両方 `.view` ケース)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`.view` ケース同士は等価とみなされ `updateAccessory` Diff は発行されない。任意 View の中身更新は既存仕様通り `UIHostingConfiguration` の再構成に委ねられる

#### Scenario: 任意 View 形式の Root H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧 modifier `.rootHeader { HeaderA() }` と新 modifier `.rootHeader { HeaderB() }`(両方とも任意 View 指定)
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

#### Scenario: 可視性変化のみで `.full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A", isVisible: true) }` と新ツリー `Section { LabelCell("A", isVisible: false) }`（同 Section ID、同 Cell ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される。`.replaceCell` は発行されない

#### Scenario: 可視性変化 + 内容変化で `.full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("旧", isVisible: true) }` と新ツリー `Section { LabelCell("新", isVisible: false) }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される。内容変化は `.full` に内包される（`.replaceCell` は発行されない）

#### Scenario: Section.isVisible 変化で `.full` 発行

- **GIVEN** 旧ツリー `Section("一般", isVisible: true) { ... }` と新ツリー `Section("一般", isVisible: false) { ... }`（同 Section ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される

### Requirement: SwiftUI DSL における isVisible 引数

`KsSettingsViewSwiftUI` の SwiftUI DSL は、Section ヘルパおよび本変更提案で扱う 7 種の Cell ヘルパに `isVisible: Bool = true` 引数を提供しなければならない (SHALL)。

- Section ヘルパ：`Section(_ header: String?, ..., isVisible: Bool = true) { ... }` の形で `isVisible` 引数を受け取り、生成される `Section` ドメインモデルの `isVisible` フィールドに反映する。
- 各 Cell ヘルパ：`LabelCell(..., isVisible: Bool = true)` の形で `isVisible` 引数を受け取り、生成される Cell モデルの `isVisible` フィールドに反映する。

既定値は `true` で、既存呼び出しは引数省略で互換維持される。

#### Scenario: Section に isVisible を指定できる

- **GIVEN** SwiftUI DSL で `Section("一般", isVisible: condition) { LabelCell(title: "通知") }` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `Section` ドメインモデルの `isVisible` が `condition` の値を反映する

#### Scenario: Cell に isVisible を指定できる

- **GIVEN** SwiftUI DSL で `LabelCell(title: "通知", isVisible: showAdvanced)` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `LabelCell` モデルの `isVisible` が `showAdvanced` の値を反映する

#### Scenario: isVisible 未指定でも既存コードがビルドできる

- **GIVEN** 既存コード `LabelCell(title: "通知")`（`isVisible` 引数を指定しない）
- **WHEN** コンパイル・実行する
- **THEN** 既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

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

- **GIVEN** 旧ツリー `LabelCell("名前")` と新ツリー `LabelCell("名前").font(.headline)`(同位置・同型・同 title)
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
  - `hintLabel.trailingAnchor.constraint(equalTo: cell.trailingAnchor, constant: -10)`
  - `hintLabel.bottomAnchor.constraint(lessThanOrEqualTo: cell.bottomAnchor, constant: -12)`（hintLabel が縦方向にはみ出さないようにする）

  ここで `cell.trailingAnchor` は **`UICollectionViewListCell` 自身の trailingAnchor**（cell の右端）であり、`cell.contentView.trailingAnchor` ではない。`contentView.trailingAnchor` は accessory がある cell では accessory 領域の左端で終わるため、`hintLabel` が accessory の左にずれて見える問題が発生する。オリジナル `AiForms.Maui.SettingsView/SettingsView/Native/iOS/Cells/CellBaseView.cs` の `_HintLabel.RightAnchor.ConstraintEqualTo(this.RightAnchor, -10)`（`this` = `UITableViewCell`）と一致させるため、`cell.trailingAnchor` 基準を採用する。
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
- **THEN** `listCell.contentConfiguration` の `text` が "通知"、`secondaryText` が "プッシュ通知"、`image` が `UIImage(systemName: "bell")` で組まれる。`listCell.accessories` は `[valueText label "オン" customView, UISwitch の customView]` の順（インデックス 0 が最も content 寄り、最後の要素が最も画面右端寄り）で並び、`hintText` は accessories に含まれない。`hintText` 「推奨」は `cell` 直下の `hintLabel`（`topAnchor = cell.topAnchor + 2`, `trailingAnchor = cell.trailingAnchor - 10`）にテキスト反映され、右上 float 表示される

#### Scenario: hintLabel が accessory のある cell でも cell 右端基準で配置される

- **GIVEN** `SwitchCell(title: "通知", hintText: "推奨", isOn: true)`（accessory に UISwitch を持つ）と `ButtonCell(title: "登録", valueText: "送信", hintText: "推奨")`（accessory なし）の両方を画面に描画した状態
- **WHEN** 両 cell の `hintLabel.frame.maxX` を取得する
- **THEN** 両 cell の `hintLabel.frame.maxX` は cell.bounds の右端から 10pt 内側（`cell.bounds.maxX - 10`）と一致する。accessory の有無に関わらず hintLabel は同じ右端基準位置に float 配置され、`SwitchCell` の hintLabel が `UISwitch` の左にずれることはない

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
