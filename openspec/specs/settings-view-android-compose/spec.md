# settings-view-android-compose Specification

## Purpose

`settings-view-android-compose` は、`ks-settingsview-compose`（Android）の **宣言的 UI ラッパ層** を担う capability である。Jetpack Compose から `settings-view-android-host` の `KsSettingsView`（FrameLayout 派生 View）を `AndroidView` 経由で薄くラップする `@Composable fun KsSettingsView(...)` と、宣言的に Cell ツリーを記述する `settingsRoot { ... }` / `KsSettingsView { ... }` DSL を定義する。Section / Cell の同一性判定戦略（明示 ID → forEach key → 構造位置 fallback の優先順位）、DSL ツリーから `SettingsRootDiff` を算出するロジック、`MutableState` 駆動の Binding セル規約も本 capability に含まれる。Android の RecyclerView / Adapter 基盤・スタイル切替・Theme 変換などは下位の 3 spec（host / style / theme-bridge）に分離されており、本 capability はそれらの上に「Compose 流の書き味」を提供する立場である。

## Requirements
### Requirement: Compose ラッパ KsSettingsView

`@Composable fun KsSettingsView` は Compose から `KsSettingsView`（FrameLayout 派生 View）を利用できる薄いラッパとして提供しなければならない (SHALL)。

公開 Composable 関数として以下の **2 種類** を提供しなければならない (MUST)：

1. **Store 方式**:
   ```kotlin
   @Composable
   fun KsSettingsView(
       store: SettingsRootStore,
       modifier: Modifier = Modifier,
       style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
       rootHeader: (@Composable () -> Unit)? = null,
       rootFooter: (@Composable () -> Unit)? = null,
   )
   ```
   - Store ベースの経路。Theme は `store.theme` の StateFlow を購読
   - パワーユーザー向け

2. **DSL 方式**:
   ```kotlin
   @Composable
   fun KsSettingsView(
       modifier: Modifier = Modifier,
       style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
       theme: Theme = Theme(),
       rootHeader: (@Composable () -> Unit)? = null,
       rootFooter: (@Composable () -> Unit)? = null,
       content: SettingsRootScope.() -> Unit,
   )
   ```
   - 宣言的に Cell ツリーを記述する Compose 流儀の経路
   - **`theme` パラメータは UI 層 `Theme` 型（フィールドは Compose `Color` / `TextStyle` 直接保持）**
   - 内部で `remember { SettingsRootStore(initialRoot = ..., initialTheme = theme) }` を保持し、Recomposition のたびに新旧の宣言ツリーを比較して `SettingsRootDiff` 列を算出、内部 Store の Diff 経路に流す
   - `theme` パラメータが変化したら `store.applyTheme(newTheme)` を呼ぶ
   - 一般用途向け

両方の関数で `AndroidView` を内部で利用し、`factory` で `KsSettingsView`（FrameLayout）を作成し `view.bind(store)` を呼ぶ。`update` で `style` / `rootHeader` / `rootFooter` を反映する。Theme は Store 経由で View に伝搬する。

旧 `root: SettingsRoot` / `onChange: (SettingsRoot) -> Unit` 引数は廃止された状態のままとする (MUST NOT 復活)。

#### Scenario: DSL 方式での Compose 利用（Theme 付き）

- **GIVEN** Compose 関数内で
  ```kotlin
  KsSettingsView(theme = Theme(separatorColor = Color(0xFFE6DAB9))) {
      Section("ユーザー") {
          LabelCell(title = "Hello")
      }
  }
  ```
- **WHEN** Compose が初回 Composition する
- **THEN** 内部 `SettingsRootStore` が DSL の root と引数の theme で初期化され、`KsSettingsView`（FrameLayout）に bind されて初期描画される

#### Scenario: theme パラメータの変化

- **GIVEN** 上記 DSL で `theme` が State により切替わる
- **WHEN** Recomposition が起こる
- **THEN** 内部 Store の `applyTheme(newTheme)` が呼ばれ、`KsSettingsView.theme` プロパティが更新される。`SettingsRootDiff` は発行されない

#### Scenario: DSL 方式での Compose 利用

- **GIVEN** Compose 関数内で
  ```kotlin
  KsSettingsView {
      Section(header = "ユーザー") {
          LabelCell("名前")
      }
  }
  ```
  と記述
- **WHEN** 初回 Composition される
- **THEN** 内部で `remember { SettingsRootStore(initialRoot = DSLの評価結果) }` が生成され、`AndroidView.factory` で `KsSettingsView` が作られ、`view.bind(internalStore)` が呼ばれる

#### Scenario: DSL 方式での @State 変更による再描画

- **GIVEN** `val userName = remember { mutableStateOf("Taro") }` と
  ```kotlin
  KsSettingsView {
      Section { LabelCell(userName.value) }
  }
  ```
  が画面表示中
- **WHEN** `userName.value = "Hanako"` を代入
- **THEN** Recomposition で新ツリーが構築され、Cell ID 同一・内容違いと判定されて `.replaceCell` Diff が内部 Store 経由で View に流れ、該当 Cell のみが再描画される

#### Scenario: Store 方式での Compose 利用

- **GIVEN** Compose 関数内で `val store = remember { SettingsRootStore(initialRoot = ...) }` 宣言
- **WHEN** `KsSettingsView(store = store)` を Composition する
- **THEN** `AndroidView.factory` で `KsSettingsView` が作られ、`view.bind(store)` が呼ばれて Store の Diff Flow が購読される（既存挙動を維持）

#### Scenario: Store 方式でのメソッド呼び出しによる再描画

- **GIVEN** `KsSettingsView(store = store)` が描画中
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** View 側で `applyDiff` が呼ばれて新規 Cell 行が挿入アニメーションで追加される（既存挙動を維持）

#### Scenario: rootHeader 引数の任意 Composable 指定

- **GIVEN** Compose 関数内で
  ```kotlin
  KsSettingsView(rootHeader = { Text("プロフィール") }) { ... }
  ```
  と記述
- **WHEN** 初回 Composition される
- **THEN** `view.rootHeader = RootAccessory.View(KsAnyView.Compose { Text("プロフィール") })` 相当に内部変換され、Compose で描画される

#### Scenario: rootHeader を null にする

- **GIVEN** `KsSettingsView(rootHeader = { ... }) { ... }` で描画中
- **WHEN** Recomposition で `rootHeader = null` に変化
- **THEN** `view.rootHeader = null` が反映され、RecyclerView 先頭の Header が削除される

#### Scenario: rootFooter 引数の任意 Composable 指定

- **GIVEN** Compose 関数内で
  ```kotlin
  KsSettingsView(rootFooter = { Text("© 2026") }) { ... }
  ```
  と記述
- **WHEN** 初回 Composition される
- **THEN** `view.rootFooter = RootAccessory.View(KsAnyView.Compose { Text("© 2026") })` 相当に内部変換され、Compose で描画される

#### Scenario: rootFooter を null にする

- **GIVEN** `KsSettingsView(rootFooter = { ... }) { ... }` で描画中
- **WHEN** Recomposition で `rootFooter = null` に変化
- **THEN** `view.rootFooter = null` が反映され、RecyclerView 末尾の Footer が削除される

#### Scenario: 旧 headerView パラメータはコンパイルエラー

- **GIVEN** Compose 関数内で `KsSettingsView(store = store, headerView = { Text("旧") })` と記述
- **WHEN** ビルドする
- **THEN** `headerView` / `footerView` パラメータは本提案で削除されているため、Kotlin コンパイラが `unresolved reference: headerView` エラーを報告する。利用者は `rootHeader = { ... }` への書き換えが必要

### Requirement: Compose DSL

宣言的 DSL を提供し、Compose 内で Cell ツリーを構築できなければならない (SHALL)。DSL は以下の要素を含む完全な宣言的記法を実現しなければならない (MUST)：

- **既存の `settingsRoot { ... }` ビルダ関数を維持**：`SettingsRoot` 値型を直接構築する純粋関数として、Store 方式利用者の初期 root 構築用に提供を継続
- **`@DslMarker SettingsRootDsl` を継続使用**：`DSLSettingsRootScope` / `DSLSectionScope` の入れ子誤用を防ぐ
- **`DSLSettingsRootScope` の拡張**：
  - 既存の `section(id:, header:, footer:, block:)` API を維持
  - 新規 `Section(header:, footer:, headerContent:, footerContent:, block:): SectionHandle` 関数を提供（id 省略可、ID 自動採番、戻り値は `SectionHandle` で modifier chain 可能）
    - `header: String?` および `headerContent: (@Composable () -> Unit)?` を排他的に受け入れ（両方指定は禁止、ランタイム検証）
    - `footer: String?` および `footerContent: (@Composable () -> Unit)?` も同様
  - 独自 `forEach<T>(items: List<T>, key: (T) -> Any, content: DSLSettingsRootScope.(T) -> Unit)` を追加（ルート用：Section 群を展開、Compose 公式の `key` lambda 流儀）
  - 独自 `forEach<T : KsIdentifiable>(items: List<T>, content: DSLSettingsRootScope.(T) -> Unit)` も追加（`KsIdentifiable` marker 経由で `key` 省略可、SwiftUI Identifiable 版と並列、`inline reified` で実装）
- **`DSLSectionScope` の拡張**：
  - 既存の `cell(cell: Cell): CellHandle` API を維持（戻り値を `Unit` から `CellHandle` に変更し modifier chain 可能化）
  - **Cell 直置き用 DSL 拡張関数**: 具象 Cell 型ごとに `DSLSectionScope` の拡張関数を提供する規約とする
    - 例: `fun DSLSectionScope.LabelCell(title: String, ...): CellHandle = cell(LabelCell(...))`（後続 `add-cell-types-*` で具象実装、本提案では規約のみ）
    - 利用者は `Section("...") { LabelCell(title = "...") }` のように iOS と完全に同じ書き味で Cell を直置きできる
  - `operator fun Cell.unaryPlus(): CellHandle = cell(this)` を `DSLSectionScope` に定義：外部から渡された `Cell` 値を `+cell` で DSL に流す逃げ道として提供
  - 独自 `forEach<T>(items: List<T>, key: (T) -> Any, content: DSLSectionScope.(T) -> Unit)` を追加（セクション内用：Cell 群を展開、`key` lambda 版）
  - 独自 `forEach<T : KsIdentifiable>(items: List<T>, content: DSLSectionScope.(T) -> Unit)` も追加（`KsIdentifiable` marker 経由で `key` 省略可）
- **`KsIdentifiable` marker interface**：
  - `ks-settingsview-compose` モジュール内に `interface KsIdentifiable { val id: Any }` を定義
  - 具象 data class が `KsIdentifiable` を実装することで、`forEach(items) { ... }` の `key` 省略版オーバーロードが利用可能となる
  - Compose 公式の `LazyColumn.items(key = { ... })` と作法を揃える `key` lambda 版と併存させ、利用者の好み・用途に応じて選択可能とする
- **DSL Cell 識別性 interface**: `DSLReidentifiableCell` / `DSLStyleModifiableCell`
  - これらの interface は `ks-settingsview-core` モジュール（パッケージ `jp.kamusoft.kssettingsview.core`）に定義しなければならない (MUST)
  - 後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `ks-settingsview-ui` モジュールに配置されるため、Core モジュールに置くことで依存方向を保つ
- **Section の ID 自動採番**：
  - 既存の `section(id: String, ...)` は明示 ID 指定が必須のままだが、新規 `Section(...)` 関数は ID 省略時に自動採番（同一性判定戦略に従う）
- **Cell の Modifier 風 API**：以下の **2 系統**を併存させる
  - **`CellHandle` 経由 chain**（DSL 内推奨）: `Section("...") { LabelCell(title = "...").cellHeight(80.dp).titleColor(Color.Red) }`
    - `CellHandle.font(font: TextStyle): CellHandle`（**型は `TextStyle`**、`KsFont` ではない）
    - `CellHandle.icon(icon: KsImage): CellHandle`（**`KsImage` は `ks-settingsview-ui` 所属**）
    - `CellHandle.cellHeight(height: Dp): CellHandle`
    - `CellHandle.titleColor(color: Color): CellHandle`（**型は Compose `androidx.compose.ui.graphics.Color`**）
    - `CellHandle.backgroundColor(color: Color): CellHandle`（**同上**）
    - `CellHandle.disabled(flag: Boolean): CellHandle`
    - `CellHandle.cellID(id: Any): CellHandle`：明示 Cell ID
    - すべて `@SettingsRootDsl` 付き拡張関数として実装し、内部の `DSLSectionScope` 経由で対応する `DSLCellNode` を更新する
  - **`Cell` 値型 modifier**（既存、外部 Cell 値や Store 方式での利用用に維持）:
    - `Cell.font(font: TextStyle): Cell` / `Cell.cellHeight(height: Dp): Cell` / `Cell.cellID(id: Any): Cell` 等
    - data class copy で新インスタンスを返す（イミュータブル）
- **Section の Modifier 風 API**: `SectionHandle` 経由 chain
  - `SectionHandle.sectionHeader(text: String): SectionHandle`
  - `SectionHandle.sectionHeader(content: @Composable () -> Unit): SectionHandle`
  - `SectionHandle.sectionFooter(text: String): SectionHandle`
  - `SectionHandle.sectionFooter(content: @Composable () -> Unit): SectionHandle`
  - `SectionHandle.sectionID(id: Any): SectionHandle`：明示 Section ID
  - すべて `@SettingsRootDsl` 付き拡張関数として実装し、内部の `DSLSettingsRootScope` 経由で対応する `DSLSectionNode` を更新する
  - `SectionHandle` / `CellHandle` は `internal constructor` + `@SettingsRootDsl` で外部生成不可とし、scope 越境を防ぐ
- **具象 Cell コンストラクタの `id` デフォルト値規約**:
  - 具象 Cell 実装（`LabelCell` 等、後続 `add-cell-types-*` で実装）は `id: String` パラメータに **UUID ベースのデフォルト値**（例: `"label-${java.util.UUID.randomUUID()}"`）を持たせなければならない (SHALL)
  - DSL 経路では `DSLReidentifiableCell.withDSLId(...)` で本仕様の優先順位に従う ID に rebind されるため、デフォルト UUID 値が最終 Cell ID として表面化することはない
  - 利用者は DSL 内で `LabelCell(title = "...")` のように `id` 引数省略で記述できる

DSL は内部 `SettingsRootStore` の初期化に使われると同時に、Recomposition のたびに新ツリーを構築して旧ツリーとの Diff を算出する責務を持つ (MUST)。**`settingsRoot` 関数の `theme` 引数は削除する (MUST NOT)**：Theme は `KsSettingsView(theme = ...)` 引数で受ける経路に一本化される。

#### Scenario: DSL から SettingsRoot 構築（Theme なし）

- **GIVEN** Kotlin コード内で
  ```kotlin
  val root: SettingsRoot = settingsRoot {
      section(id = "user", header = "一般") { /* cell(LabelCell(...)) */ }
  }
  ```
  と記述
- **WHEN** `root` を評価する
- **THEN** `SettingsRoot.sections` に 1 つの `Section` が含まれる。`root.theme` プロパティは存在しない（SettingsRoot から削除済み）

#### Scenario: CellHandle の型

- **GIVEN** DSL 内のコード `LabelCell(title = "X").titleColor(Color.Red).backgroundColor(Color.Yellow).font(TextStyle.Default)`
- **WHEN** コンパイルする
- **THEN** `.titleColor(...)` は Compose `Color` を受け、`.backgroundColor(...)` は Compose `Color` を受け、`.font(...)` は `TextStyle` を受ける。`KsColor` / `KsFont` を渡そうとするとビルドエラーになる

#### Scenario: CellHandle.icon の型

- **GIVEN** DSL 内のコード `LabelCell(title = "X").icon(KsImage.Resource(R.drawable.ic_x))`
- **WHEN** コンパイルする
- **THEN** `.icon(...)` は `KsImage`（`jp.kamusoft.kssettingsview.ui` 所属）を受ける。`jp.kamusoft.kssettingsview.core.KsImage` は存在しないため、`import jp.kamusoft.kssettingsview.ui.KsImage` が必要

#### Scenario: KsSettingsView DSL receiver での Section 記述

- **GIVEN** Compose 関数内で
  ```kotlin
  KsSettingsView {
      Section("ユーザー") {
          LabelCell(title = "名前")
          CommandCell(title = "ログアウト") { /* action */ }
      }
  }
  ```
- **WHEN** Composition する
- **THEN** Section が ID 自動採番で構築され、内部 Store の初期 root に反映される。Cell は具象 Cell 型ごとの DSL 拡張関数 `LabelCell(title = "名前")` で直置きでき、`cell(...)` ラップは不要

#### Scenario: forEach（セクション内 Cell 用、key lambda 版）

- **GIVEN** `val items: List<Todo>` と
  ```kotlin
  KsSettingsView {
      Section("Todo") {
          forEach(items, key = { it.id }) { todo ->
              LabelCell(title = todo.name)
          }
      }
  }
  ```
- **WHEN** Composition する
- **THEN** items.size 個の Cell が Section 内に展開され、各 Cell の Cell ID は `key` lambda が返す `it.id` から導出される

#### Scenario: forEach（セクション内 Cell 用、KsIdentifiable 版で key 省略）

- **GIVEN** `data class Todo(override val id: Int, val name: String) : KsIdentifiable` と
  ```kotlin
  KsSettingsView {
      Section("Todo") {
          forEach(items) { todo ->            // key 省略
              LabelCell(title = todo.name)
          }
      }
  }
  ```
- **WHEN** Composition する
- **THEN** `Todo` が `KsIdentifiable` を実装しているため `forEach` の `key` 省略版オーバーロードが解決され、各 Cell の Cell ID は `todo.id` から導出される。`key = { it.id }` 明示版と完全に等価な振る舞いとなる

#### Scenario: forEach（ルート用 Section 群を展開）

- **GIVEN** `val groups: List<Group>` と
  ```kotlin
  KsSettingsView {
      forEach(groups, key = { it.id }) { group ->
          Section(group.name) {
              forEach(group.items, key = { it.id }) { item ->
                  LabelCell(title = item.name)
              }
          }
      }
  }
  ```
- **WHEN** Composition する
- **THEN** groups.size 個の Section が展開され、Section ID は `group.id` から、Cell ID は `item.id` から導出される

#### Scenario: Section の文字列ヘッダ指定（引数版）

- **GIVEN**
  ```kotlin
  Section(header = "見出し", footer = "注釈") {
      LabelCell(title = "一行目")
  }
  ```
- **WHEN** Composition する
- **THEN** Section の `header` が `SectionAccessory.Text("見出し")`、`footer` が `SectionAccessory.Text("注釈")` となる

#### Scenario: Section の sectionHeader / sectionFooter modifier chain

- **GIVEN**
  ```kotlin
  Section("見出し") {
      LabelCell(title = "一行目")
  }.sectionFooter("注釈")
  ```
- **WHEN** Composition する
- **THEN** `Section(...)` の戻り値 `SectionHandle` に対し `.sectionFooter("注釈")` が適用され、Section の `header = SectionAccessory.Text("見出し")`、`footer = SectionAccessory.Text("注釈")` となる。iOS 側の `Section("見出し") { ... }.sectionFooter("注釈")` と完全並列な書き味

#### Scenario: SectionHandle.sectionHeader による任意 Composable 上書き

- **GIVEN**
  ```kotlin
  Section("既存") { LabelCell(title = "...") }
      .sectionHeader { Row { Icon(...); Text("通知設定", fontWeight = FontWeight.Bold) } }
  ```
- **WHEN** Composition する
- **THEN** `Section("既存")` で一旦設定された `SectionAccessory.Text("既存")` ヘッダが `.sectionHeader { ... }` で上書きされ、`SectionAccessory.View(KsAnyView.Compose { ... })` となり UI 層が `ComposeView` で任意 Composable を描画する

#### Scenario: Section の任意 Composable ヘッダ指定（引数版）

- **GIVEN**
  ```kotlin
  Section(headerContent = {
      Row { Icon(...); Text("通知設定", fontWeight = FontWeight.Bold) }
  }) {
      LabelCell(title = "一行目")
  }
  ```
- **WHEN** Composition する
- **THEN** Section の `header` が `SectionAccessory.View(KsAnyView.Compose { ... })` となり、UI 層が `ComposeView` で任意 Composable を描画する

#### Scenario: Section の header と headerContent 両方指定（エラー）

- **GIVEN**
  ```kotlin
  Section(header = "文字列", headerContent = { Text("View") }) { ... }
  ```
- **WHEN** Composition する
- **THEN** ランタイム検証エラー（DEBUG ビルドで `IllegalArgumentException` 等）、両方の同時指定は禁止

#### Scenario: Cell modifier の連鎖適用（CellHandle 経由）

- **GIVEN**
  ```kotlin
  Section("Cell Modifier") {
      LabelCell(title = "名前")
          .font(KsFont.headline)
          .icon(KsIcon.system("person"))
          .cellHeight(60.dp)
  }
  ```
- **WHEN** 評価する
- **THEN** `LabelCell(title = "名前")` の戻り値 `CellHandle` に対し各 modifier が順に適用され、内部の `DSLCellNode` の Cell 値が data class copy で新インスタンスに更新される。`style.font` / `style.icon` / `style.cellHeight` が指定値に上書きされ、元 Cell の data class インスタンスは不変

#### Scenario: Cell 直置き（cell ラップ省略、具象 Cell 型 DSL 拡張関数）

- **GIVEN**
  ```kotlin
  Section("静的") {
      LabelCell(title = "固定 A")
      LabelCell(title = "固定 B")
  }
  ```
- **WHEN** Composition する
- **THEN** `DSLSectionScope` の拡張関数 `LabelCell(title:)` が解決され、内部で `cell(LabelCell(...))` が呼ばれて Cell が DSL ツリーに追加される。利用者は `cell(LabelCell(...))` のラップを書かずに済み、iOS の `LabelCell(title: "固定 A")` 直置きと完全並列な書き味となる

#### Scenario: 外部 Cell 値を unaryPlus で DSL に流す

- **GIVEN**
  ```kotlin
  val externalCell: Cell = someCellProvider.create()
  Section("動的") {
      +externalCell
  }
  ```
- **WHEN** Composition する
- **THEN** `DSLSectionScope.operator fun Cell.unaryPlus()` が呼ばれ、`externalCell` が `cell(externalCell)` 同等で DSL ツリーに追加される。戻り値 `CellHandle` に対しさらに `.cellHeight(...)` 等 modifier を chain できる

#### Scenario: 明示 cellID による Cell 同一性指定（CellHandle 経由）

- **GIVEN**
  ```kotlin
  Section("動的") {
      LabelCell(title = "動的Cell").cellID("dynamic-cell-1")
  }
  ```
- **WHEN** Recomposition をまたいで評価する
- **THEN** Cell ID が `"dynamic-cell-1"` として固定され、Section 内位置や Cell 型に依存しない安定 ID となる。`CellHandle.cellID(...)` 経由で `DSLCellNode.identityHint` に `Explicit(id)` が記録される

#### Scenario: 明示 sectionID による Section 同一性指定（SectionHandle 経由）

- **GIVEN**
  ```kotlin
  KsSettingsView {
      Section { LabelCell(title = "動的Section") }.sectionID("dynamic-section-1")
  }
  ```
- **WHEN** Recomposition をまたいで評価する
- **THEN** Section ID が `"dynamic-section-1"` として固定される。`SectionHandle.sectionID(...)` 経由で `DSLSectionNode.identityHint` に `Explicit(id)` が記録される

#### Scenario: 具象 Cell の id デフォルト値による省略可

- **GIVEN** `data class LabelCell(override val id: String = "label-${UUID.randomUUID()}", override val style: CellStyle = CellStyle(), val title: String) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell` という具象 Cell（後続 `add-cell-types-*` 提案で実装される規約）と、
  ```kotlin
  Section("静的") {
      LabelCell(title = "固定")     // id 引数省略
  }
  ```
- **WHEN** DSL を評価する
- **THEN** コンストラクタが `id = "label-${ランダムUUID}"` のデフォルト値で `LabelCell` を生成し、DSL 経路で `DSLReidentifiableCell.withDSLId(...)` により本仕様の優先順位に従う ID（この場合は `(SectionID, indexInSection, CellType)` ハッシュ）に rebind される。利用者が `id` を意識せずに済み、iOS Sample の `LabelCell(title: "固定")` と完全並列な書き味となる

### Requirement: DSL → SettingsRootDiff 算出ロジック（Compose）

`ks-settingsview-compose` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsView.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは iOS 側（`settings-view-ios-ui` capability の同名 Requirement）と完全に同等でなければならない (MUST)：

1. **可視性変化の preflight 検出**：
   - 旧ツリーと新ツリーの間で、同一 ID の Section について `isVisible` の値が変化している、または同一 Cell ID について `(cell as? VisibilityAware)?.isVisible ?: true` の値が変化していることを検出した場合、通常の section / cell 差分算出には進まず、`SettingsRootDiff.Full(newRoot)` のみを発行して終了しなければならない (MUST)。
   - 可視性差分を内容更新経路（ViewHolder の部分更新経路 / `contentUpdates`）に流してはならない (MUST NOT)。同条件下では `contentUpdates` は空リストを返さなければならない (MUST)。可視性変化は構造同期上の追加・削除として表現される必要があり、内容更新経路では正しく扱えないため。
2. **Section レベルの突合**（可視性差分が無い場合に実施）：Section ID 集合の比較で `InsertSection` / `RemoveSection` / `MoveSection` / `UpdateAccessory`（Section H/F 用）を発行
3. **各 Section 内の Cell レベルの突合**：Cell ID 集合の比較で `InsertCell` / `RemoveCell` / `MoveCell` を発行する（構造変化＝追加・削除・移動・id 変化のみ）。**両セクションに同一 Cell ID が存在し内容（プロパティ）だけが異なる場合、`ReplaceCell` を構造同期の差分として発行してはならない** (MUST NOT)（「表示状態同期の三層分離」: 構造同期は id 同一性のみ）。同一 id の内容更新は ViewHolder の部分更新経路（`DiffUtil 差分検出` Requirement / `CellViewHolder 抽象` Requirement 参照）で反映する
4. **Root H/F の突合**：`rootHeader` / `rootFooter` パラメータの値が変化した場合 `UpdateAccessory`（Root H/F 用）を発行
5. **Theme の突合**：Theme は `SettingsRootDiff` には含まれない (MUST NOT)。Theme の変化は `KsSettingsView(theme = ...)` パラメータの再評価で `store.applyTheme(newTheme)` を呼ぶ経路で反映される（独立 API）
6. **構造同期の同一性判定対象**：Section / Cell の **id 同一性のみ** で追加・削除・移動を判定する。Cell の内容プロパティを構造同期の判定に用いてはならない (MUST NOT)
7. **任意 View 形式（`SectionAccessory.View(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.View` ケース同士・`RootAccessory.View` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `UpdateAccessory` Diff は **発行しない**
   - 異なるケース（`Text` → `View` または `View` → `Text`、`null` → `View` 等）の場合のみ `UpdateAccessory` Diff を発行

#### Scenario: Cell 内容変更時は ReplaceCell を発行しない

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`(Section ID・Cell ID は同じ、内容のみ変化)
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff（`InsertCell` / `RemoveCell` / `MoveCell` / `ReplaceCell`）は発行されない。内容更新は ViewHolder の部分更新経路で反映される

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertCell(sectionId = <same>, index = 1, cell = LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveCell(cellId = <B の ID>)` のみが発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`(同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveCell(cellId = <B の ID>, toIndex = 2)` または `SettingsRootDiff.MoveCell(cellId = <C の ID>, toIndex = 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない

#### Scenario: Section 追加時の Diff 発行

- **GIVEN** 旧ツリーが Section 1 つのみ、新ツリーが Section 2 つ（既存 + 末尾追加）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertSection(index = 1, section = <newSection>)` のみが発行される

#### Scenario: Section 削除時の Diff 発行

- **GIVEN** 旧ツリーが Section 2 つ（Section A + Section B、各々 Section ID は安定）、新ツリーが Section 1 つ（Section A のみ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveSection(sectionId = <B の ID>)` のみが発行される（Section A 内の Cell は完全保持）

#### Scenario: Section 移動時の Diff 発行

- **GIVEN** 旧ツリーで Section 3 つが並んでいる状態と、新ツリーで Section の順序が変わった状態（各 Section ID は不変）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveSection(fromIndex = <旧位置>, toIndex = <新位置>)` Diff が発行され、Section 内の Cell は再構築されずに移動アニメーションが走る

#### Scenario: 任意 Composable 形式の Section H/F が変化しても UpdateAccessory 非発行

- **GIVEN** 旧ツリー `Section(headerContent = { CardA() }) { ... }` と新ツリー `Section(headerContent = { CardB() }) { ... }`(同 Section ID、Header が両方 `View` ケース)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`View` ケース同士は等価とみなされ `UpdateAccessory` Diff は発行されない。任意 Composable の中身更新は既存仕様通り `ComposeView.setContent` の再実行に委ねられる

#### Scenario: 任意 Composable 形式の Root H/F が変化しても UpdateAccessory 非発行

- **GIVEN** 旧 `KsSettingsView(rootHeader = { HeaderA() }) { ... }` と新 `KsSettingsView(rootHeader = { HeaderB() }) { ... }`(両方とも任意 Composable 指定)
- **WHEN** Recomposition で Diff 算出ロジックを実行
- **THEN** 同じ `View` ケース同士は等価とみなされ、`UpdateAccessory(target = RootHeader, ...)` Diff は発行されない

#### Scenario: Section H/F のケース変化（Text → View）で UpdateAccessory 発行

- **GIVEN** 旧ツリー `Section(header = "文字列") { ... }` と新ツリー `Section(headerContent = { CustomHeader() }) { ... }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `Text` ケースから `View` ケースへの遷移は検出可能なため `SettingsRootDiff.UpdateAccessory(target = SectionHeader(sectionId), accessory = SettingsAccessory.Section(SectionAccessory.View(...)))` が発行される

#### Scenario: Section H/F 変更時の Diff 発行

- **GIVEN** 旧ツリー `Section(header = "旧") { ... }` と新ツリー `Section(header = "新") { ... }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.UpdateAccessory(target = SectionHeader(sectionId), accessory = SettingsAccessory.Section(SectionAccessory.Text("新")))` が発行される

#### Scenario: Root H/F 変更時の Diff 発行（null → 任意 Composable のケース変化）

- **GIVEN** 旧 `KsSettingsView(rootHeader = null) { ... }` と新 `KsSettingsView(rootHeader = { Text("新") }) { ... }`(`null` から `View` ケースへの遷移)
- **WHEN** Recomposition で Diff 算出ロジックを実行
- **THEN** ケース変化（`null` → `View`）が検出され、`SettingsRootDiff.UpdateAccessory(target = RootHeader, accessory = SettingsAccessory.Root(RootAccessory.View(...)))` が発行される

注: 同じ `View` ケース同士の Composable 中身変化は `UpdateAccessory` 非発行となる（直前の Scenario 参照）。Compose の `rootHeader` は `(@Composable () -> Unit)?` 型のため、ケース変化として観測可能なのは `null` ↔ 非 `null` の遷移のみとなる。

#### Scenario: 同一 id の構造で構造 Diff 空

- **GIVEN** 旧ツリーと新ツリーで Section / Cell の id 集合・順序が完全に同一（内容プロパティの異同は問わない）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff 列（Insert/Remove/Move/Replace 系）は空となる。内容プロパティが変化していても構造同期は発火せず、内容更新は ViewHolder の部分更新経路で反映される

#### Scenario: Theme 変化時の Diff 不発行

- **GIVEN** 旧 `KsSettingsView(theme = themeA) { ... }` と新 `KsSettingsView(theme = themeB) { ... }`（root 内容は不変）
- **WHEN** Recomposition が起こる
- **THEN** `SettingsRootDiff` は何も発行されない。代わりに `store.applyTheme(themeB)` が呼ばれて `KsSettingsView.theme` プロパティが更新される

#### Scenario: 可視性変化のみで `Full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A", isVisible = true) }` と新ツリー `Section { LabelCell("A", isVisible = false) }`（同 Section ID、同 Cell ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される。`contentUpdates` は空リストを返す

#### Scenario: 可視性変化 + 内容変化で `Full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("旧", isVisible = true) }` と新ツリー `Section { LabelCell("新", isVisible = false) }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される。`contentUpdates` は空リストを返し、内容変化は `Full` に内包される

#### Scenario: Section.isVisible 変化で `Full` 発行

- **GIVEN** 旧ツリー `Section("一般", isVisible = true) { ... }` と新ツリー `Section("一般", isVisible = false) { ... }`（同 Section ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される

### Requirement: Compose DSL における isVisible 引数

`ks-settingsview-compose` の Compose DSL は、Section ヘルパおよび本変更提案で扱う 7 種の Cell ヘルパに `isVisible: Boolean = true` 引数を提供しなければならない (SHALL)。

- Section ヘルパ：`Section(header: String?, ..., isVisible: Boolean = true) { ... }` の形で `isVisible` 引数を受け取り、生成される `Section` ドメインモデルの `isVisible` フィールドに反映する。
- 各 Cell ヘルパ：`LabelCell(..., isVisible: Boolean = true)` の形で `isVisible` 引数を受け取り、生成される Cell モデルの `isVisible` フィールドに反映する。

既定値は `true` で、既存呼び出しは引数省略で互換維持される。

#### Scenario: Section に isVisible を指定できる

- **GIVEN** Compose DSL で `Section(header = "一般", isVisible = condition) { LabelCell(title = "通知") }` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `Section` ドメインモデルの `isVisible` が `condition` の値を反映する

#### Scenario: Cell に isVisible を指定できる

- **GIVEN** Compose DSL で `LabelCell(title = "通知", isVisible = showAdvanced)` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `LabelCell` モデルの `isVisible` が `showAdvanced` の値を反映する

#### Scenario: isVisible 未指定でも既存コードがビルドできる

- **GIVEN** 既存コード `LabelCell(title = "通知")`（`isVisible` 引数を指定しない）
- **WHEN** コンパイル・実行する
- **THEN** 既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

### Requirement: Section / Cell の同一性判定戦略（Compose）

`ks-settingsview-compose` の DSL → Diff 算出ロジックは、iOS 側（`settings-view-ios-ui` capability の同名 Requirement）と完全に同等の優先順位で Section / Cell の ID を採番しなければならない (SHALL)。

**Section ID 判定の優先順位**：

1. `forEach` 配下：`key` lambda の戻り値を採用
2. `Section.sectionID(id: Any)` 拡張関数で明示指定されている場合：その値を採用
3. ヘッダが `SectionAccessory.Text(String)` の場合：`(ルート位置, ヘッダ文字列)` のハッシュを採用
4. フォールバック：ルート位置（`rootIdx`）ベースのハッシュを採用

**Cell ID 判定の優先順位**：

1. `forEach` 配下：`key` lambda の戻り値を採用
2. `Cell.cellID(id: Any)` 拡張関数で明示指定されている場合：その値を採用
3. デフォルト：`(SectionID, Section 内位置, Cell 型)` のハッシュを採用

判定された ID は `String` 型（既存 `Section.id` / `Cell.id` の型を維持）に変換される。同じ DSL 記述に対して Recomposition をまたいでも安定した ID を返さなければならない (MUST)。

ヘッダなし複数 Section が動的に追加・削除される構造は **位置ベースのフォールバック** に依存するため、Section 追加・削除で全 ID がずれるリスクがあることを明記する。利用者には `forEach` または `Section.sectionID(...)` の明示指定を推奨ドキュメント指針として案内する。

#### Scenario: 完全静的構造での Recomposition 耐性

- **GIVEN**
  ```kotlin
  KsSettingsView {
      Section(header = "ユーザー") { LabelCell("A"); SwitchCell("B") }
      Section(header = "詳細") { CommandCell("C") }
  }
  ```
- **WHEN** Recomposition を 2 回行う
- **THEN** 1 回目と 2 回目で各 Section ID・Cell ID が完全に一致する

#### Scenario: forEach 配下の Cell ID 引き継ぎ

- **GIVEN** `val items: List<Todo>` と `forEach(items, key = { it.id }) { LabelCell(it.name) }`
- **WHEN** items に新規 Todo を追加（既存 item の id は変わらない）
- **THEN** 既存 Cell の Cell ID は不変、新規 Todo の id から導出された Cell ID のみが新規追加され、`InsertCell` Diff が発行される

#### Scenario: forEach 配下の Section ID 引き継ぎ

- **GIVEN** `val groups: List<Group>` と `forEach(groups, key = { it.id }) { group -> Section { ... } }`
- **WHEN** groups の先頭に新規 Group を insert
- **THEN** 既存 Section の Section ID は不変、新規 Group の id から導出された Section ID のみが先頭に追加され、`InsertSection(index = 0, ...)` Diff が発行される

#### Scenario: ヘッダなし複数 Section の動的追加（フォールバック挙動）

- **GIVEN** 旧ツリー
  ```kotlin
  Section { LabelCell("A") }
  Section { LabelCell("B") }
  ```
  と新ツリー（先頭に新 Section 追加）
  ```kotlin
  Section { LabelCell("X") }  // 新規
  Section { LabelCell("A") }
  Section { LabelCell("B") }
  ```
- **WHEN** Diff 算出ロジックを実行
- **THEN** 位置ベースのフォールバックにより、すべての Section の ID がずれて検出される（実装上の制約）。利用者は `Section.sectionID(...)` 明示または `forEach` の利用を推奨される

#### Scenario: 明示 sectionID による動的追加の安定化

- **GIVEN** 旧ツリー
  ```kotlin
  Section { LabelCell("A") }.sectionID("a")
  Section { LabelCell("B") }.sectionID("b")
  ```
  と新ツリー
  ```kotlin
  Section { LabelCell("X") }.sectionID("x")  // 新規
  Section { LabelCell("A") }.sectionID("a")
  Section { LabelCell("B") }.sectionID("b")
  ```
- **WHEN** Diff 算出ロジックを実行
- **THEN** `InsertSection(index = 0, section = <x>)` のみが発行され、既存 Section は完全保持される

### Requirement: DSL での Bindingセル規約（Compose）

`ks-settingsview-compose` の DSL は、双方向バインド対応 Cell（後続 `add-cell-types-*` 提案で追加される `SwitchCell` / `EntryCell` / `PickerCell` 等）が `MutableState<T>` 引数を受け取れる規約を支援しなければならない (SHALL)。

- 各双方向バインド Cell は Compose 流儀で `SwitchCell(title: String, isOn: MutableState<Boolean>)` などのコンストラクタ / Composable 関数を公開する
- `MutableState` は Cell data class の内部に保持され、`state.value` の比較で Diff 算出時の値判定に使用される
- ユーザー操作で Cell の値が変わった場合（例：Switch の Toggle）、View 側のイベントが `state.value = newValue` の代入で書き戻され、Compose の `MutableState` 更新で Recomposition がトリガーされる
- 高頻度更新パス（EntryCell の連続入力等）は View 側で 200ms debounce 後に `updateCellValue(cellId, value)` を呼び、Diff 経路を通らない直行ルートで反映される
- 本提案では具象 Cell 型の追加は行わない（`add-cell-types-*` で実装）が、DSL がこの Binding 規約をサポートする責務を持つ
- **Binding セルの Cell ID 採番**: Binding セルの内部 `id` フィールドを `UUID.randomUUID()` 等で自動採番してはならない (MUST NOT)。Cell ID は本 capability の `Section / Cell の同一性判定戦略（Compose）` Requirement に従い、`forEach` 配下なら `key` lambda の戻り値、`Cell.cellID(...)` 拡張関数があればその値、デフォルトは `(SectionID, Section 内位置, Cell 型)` のハッシュを採用する。Recomposition のたびに新規 `UUID` を生成する実装は Diff 同一性判定を破壊するため禁止する

#### Scenario: MutableState 付き Cell の DSL 記述（規約検証）

- **GIVEN** `val isOn = remember { mutableStateOf(true) }` と
  ```kotlin
  Section {
      SwitchCell("通知", isOn = isOn)
  }
  ```
  の DSL 記述（`SwitchCell` は後続提案で実装）
- **WHEN** DSL → Diff 算出ロジックがこの Cell を扱う
- **THEN** Cell 値の比較では `isOn.value` を参照し、`MutableState` の値が変わった場合のみ `ReplaceCell` Diff（または `updateCellValue` 直行パス）が発行される

#### Scenario: ユーザー操作による MutableState への書き戻し（規約検証）

- **GIVEN** `val isOn = remember { mutableStateOf(false) }` で `SwitchCell("通知", isOn = isOn)` が画面表示中
- **WHEN** ユーザーが Switch を ON にする
- **THEN** View 側のイベントが `isOn.value = true` を実行し、Compose の Recomposition で body が再評価されるが、Diff 算出ロジックで値が一致するため `ReplaceCell` Diff は発行されない（無限ループ防止）


### Requirement: 共通行レイアウト関数 applyCellBaseLayout（View ベース）

`ks-settingsview-ui`（Android）は、全 CellViewHolder が共通で保持する **View ベースの行レイアウト関数 `applyCellBaseLayout(views, ...)`** を `internal` 可視性で提供しなければならない (SHALL)。この関数は `cell-types-basic` の「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement で規定された 2 系統のレイアウト規約（本体行 `[icon][title / description][valueText (title 行の右寄せ)][accessory (右側中央)]` + `hintText` の右上 float 配置）を `ConstraintLayout` ベースの `CellBaseViews` 構造体に対して反映する責務を持つ。

本 Requirement では、**Compose（`androidx.compose.runtime`）を用いない** ことを明確に定める (MUST NOT)。すなわち、ViewHolder の `bind(...)` 内で `ComposeView.setContent { ... }` を呼び出して `KsCellRow` Composable を組む実装方式は採用してはならない (MUST NOT)。理由はオリジナル `AiForms.Maui.SettingsView` の Android 実装が `RelativeLayout` ベースの View ヒエラルキーで構成されており、本 change の目的は「共通フィールドの単一化」であって UI 実装の Compose 化ではないこと、また MAUI 移植も視野に入れたパフォーマンス・互換性の観点で View ベースが優位なことによる（unify change の `design.md` Decision 11 参照）。

過渡的に存在した Compose 版 `KsCellRow.kt`（`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt` 等）は、本 Requirement 適用時に **完全に削除** しなければならない (MUST)。

#### CellBaseViews 構造体

`CellBaseViews` は、全 CellViewHolder が共通で保持する View 参照群を束ねた構造体（`internal class` または `data class`）でなければならない (MUST)。少なくとも以下のフィールドを持つ：

- `root: ConstraintLayout` — セル全体のルート ViewGroup
- `iconView: ImageView` — アイコン表示
- `titleView: TextView` — タイトル表示
- `descriptionView: TextView` — 説明文表示
- `valueTextView: TextView` — 値テキスト表示（title 行の右寄せ）
- `accessoryHolder: FrameLayout` — Cell 種別固有の trailing コントロールを差し込むためのコンテナ
- `hintTextView: TextView` — ヒントテキスト表示（右上 float）

`CellBaseViews` の構築は programmatic（Kotlin コードによる動的構築）で行わなければならない (MUST)。XML レイアウトファイル（`layout/*.xml`）への切り出しは行わない。

#### ConstraintLayout root の minimumHeight 下限保証

`buildCellBaseViews` の `root` View は、標準 `androidx.constraintlayout.widget.ConstraintLayout` ではなく **`MinHeightConstraintLayout`**（`ConstraintLayout` を継承し `onMeasure` 後に `measuredHeight` を `minimumHeight` で下限ガードする internal サブクラス）を使用しなければならない (MUST)。

`MinHeightConstraintLayout` は `onMeasure(widthMeasureSpec, heightMeasureSpec)` 内で、まず `super.onMeasure(widthMeasureSpec, heightMeasureSpec)` を呼んで標準の制約解決を行い、その結果 `measuredHeight < minimumHeight` の場合は `heightMeasureSpec` を `MeasureSpec.EXACTLY(minimumHeight)` に差し替えて **再度** `super.onMeasure(...)` を呼ぶことで、内部の制約解決を `minimumHeight` 高さで再実行しなければならない (MUST)。これにより `measuredHeight` だけでなく **子 View の縦位置**（chain bias / `TOP=parent.TOP` + `BOTTOM=parent.BOTTOM` の CenterVertical 等）も新しい高さに合わせて再配置される。`setMeasuredDimension(measuredWidth, minimumHeight)` で `measuredHeight` 値のみを上書きする方式は、`accessoryHolder` の CenterVertical 等が元の小さい高さに対して計算されたまま残るため採用してはならない (MUST NOT)。

`measuredHeight >= minimumHeight` のとき（内容が下限を超える Cell）は intrinsic な測定結果をそのまま維持し、可変高さの上方向伸縮を阻害してはならない (MUST NOT)。`minimumHeight <= 0` のときも再 measure を行ってはならない (MUST NOT)。

この MUST を導入する根拠：
- 標準 `ConstraintLayout` は、`layoutParams.height = WRAP_CONTENT` かつ親（`RecyclerView` + `LinearLayoutManager`）から `heightSpec = UNSPECIFIED` で measure される実機シナリオで、`setMinimumHeight()` を尊重しない測定結果を返すケースが知られている（[Common ConstraintLayout Pitfalls](https://blog.ostebaronen.dk/2018/12/common-constraintlayout-mistakes.html) / [androidx/constraintlayout#855](https://github.com/androidx/constraintlayout/issues/855) / [b/136492486](https://issuetracker.google.com/issues/136492486)）。
- オリジナル `AiForms.Maui.SettingsView` の `SettingsViewRecyclerAdapter.cs:483-487` も `holder.Body` と `nativeCell` の両方に `SetMinimumHeight` を呼ぶ回避策を取っている（コメント `// it is neccesary to set both`）。
- Robolectric テストでは `root.minimumHeight == 60dp 相当 px` が観測できる一方、実機では `Theme()` 既定の `applyEffectiveHeight(isFixedHeight = false)` で設定した `minimumHeight = 60dp` が measure に反映されず Cell が詰まる事象が `refine-cell-layout-after-unify-review` のオーナー実機確認で確認されている。

`applyEffectiveHeight(view, effective)` の `isFixedHeight = false` 経路（`Theme.hasUnevenRows == true` 既定経路）では、`layoutParams.height = WRAP_CONTENT` のまま `view.minimumHeight = effectiveHeightPx` を設定すれば、`MinHeightConstraintLayout.onMeasure` の下限ガードによって実機 measure 結果も `effectiveHeightPx` 以上に保証される。

#### ConstraintLayout 配置規約

`CellBaseViews` のルートは `ConstraintLayout` でなければならず、内部 View の制約は以下の構造を満たさなければならない (MUST)：

- **iconView**: 左端中央 — `Start=parent.Start`, `Top=parent.Top`, `Bottom=parent.Bottom`（CenterVertical）。**`iconView` には `End` 制約を持たせず、右側余白は後段 `titleView` / `descriptionView` の `Start=iconView.End` 接続に margin を渡して与えなければならない (MUST)**。すなわち `iconView.layoutParams.marginEnd` を設定しても ConstraintLayout は対応 anchor が無いと無視するため、その方法は採用してはならない (MUST NOT)。
- **titleView と descriptionView は本体行の縦中央寄せ vertical chain を構成しなければならない (MUST)**:
  - **titleView**: icon の右、accessoryHolder の左、本体行 vertical chain の **head** — `Start=iconView.End` に margin `iconMarginEnd`（16dp 相当 px）を渡し、`Top=parent.Top`, `End=accessoryHolder.Start`, `Bottom=descriptionView.Top`。`iconView` が `GONE` のときに余白を潰すため `setGoneMargin(titleView.id, ConstraintSet.START, 0)` を明示的に設定しなければならない (MUST)。
  - **descriptionView**: icon の右、accessoryHolder の左、本体行 vertical chain の **tail** — `Start=iconView.End` に margin `iconMarginEnd`（16dp 相当 px）を渡し、`Top=titleView.Bottom`, `End=accessoryHolder.Start`, `Bottom=parent.Bottom`。同じく `setGoneMargin(descriptionView.id, ConstraintSet.START, 0)` を設定しなければならない (MUST)。
  - 両者の vertical chain は `ConstraintSet.CHAIN_PACKED` で結ばれ、`verticalBias = 0.5f` により本体行が cell 縦中央に packed 配置されなければならない (MUST)。
  - `description == null` で `descriptionView.visibility = GONE` のとき、ConstraintLayout は GONE chain member をスペース 0 として扱うため、`titleView` 単独でも縦中央寄せ配置が維持される。
- **valueTextView**: title 行の右寄せ — `End=accessoryHolder.Start`, `Baseline=titleView.Baseline`（title と同じ行で右端に配置）。titleView が vertical chain により cell 縦中央付近に配置されるため、valueTextView もその縦中央付近に位置する。
- **accessoryHolder**: 右端中央 — `End=parent.End`, `Top=parent.Top`, `Bottom=parent.Bottom`（CenterVertical）
- **hintTextView**: セル右上に float 配置 — `End=parent.End`, `Top=parent.Top` (セル上端から数 dp のマージン)、Z 順は accessoryHolder より後ろに `addView` することで前面（最前面）に置く

`hintTextView` と `accessoryHolder` は両者とも右端揃いとなるため物理的に重なり得るが、`hintTextView` がセル上端基準・`accessoryHolder` がセル縦中央基準で配置されるため通常は干渉しない。`hintTextView` は `accessoryHolder` より後に `addView` することで Z 順の前面に置かれ、万一の干渉時にも `hintText` が前面に見える状態を保証しなければならない (MUST)。

#### applyCellBaseLayout 関数

`applyCellBaseLayout` 関数のシグネチャは次の形でなければならない (MUST)：

```kotlin
internal fun applyCellBaseLayout(
    views: CellBaseViews,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Boolean,
)
```

実装上の振る舞いは以下を満たさなければならない (MUST)：

- `title` は `views.titleView.text` に反映し、フォントを `effective.titleFont` で、文字色を `effective.titleColor`（`isEnabled == false` のときは `effective.disabledTextColor`）で設定する。
- `description` が `null` のときは `views.descriptionView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.descriptionFont` / `effective.descriptionColor`（disabled 時は `disabledTextColor`）。
- `valueText` が `null` のときは `views.valueTextView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.valueTextFont` / `effective.valueTextColor`（disabled 時は `disabledTextColor`）。
- `icon` の `KsImage` 派生（`Resource` / `Drawable` / `SystemName`）を網羅して `views.iconView` に反映する。`Resource` は `setImageResource(resId)`、`Drawable` は `setImageDrawable(drawable)`、`SystemName` 派生は Android では解決不可のため `iconView.visibility = GONE`。`icon == null` のときも `iconView.visibility = GONE`。
- `hintText` が `null` のときは `views.hintTextView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.hintTextFont` / `effective.hintTextColor`（disabled 時は `disabledTextColor`）。`hintTextView` の `singleLine = true` / `ellipsize = END` / `gravity = END` を設定する（小さなテキスト・右寄せ・1 行・末尾省略のオリジナル挙動を踏襲）。
- `views.root` の背景色を `effective.cellBackgroundColor` で適用する。
- `isEnabled` を `views.root.isEnabled` に反映し、サブ View にも適切に伝播する。

#### 各 CellViewHolder からの利用

各 CellViewHolder（`LabelCellViewHolder` / `CommandCellViewHolder` / `SwitchCellViewHolder` / `CheckboxCellViewHolder` / `RadioCellViewHolder` / `SimpleCheckCellViewHolder` / `ButtonCellViewHolder`）は、内部で `CellBaseViews` を 1 個保持し、`bind(cell, theme)` 内で `applyCellBaseLayout(views, ...)` を呼び出して共通フィールドを描画しなければならない (MUST)。`title` / `description` / `valueText` / `icon` / `hintText` のレイアウト構築コード（テキスト反映・色反映・フォント反映・visibility 制御）を各 ViewHolder 内に重複実装してはならない (MUST NOT)。

各 CellViewHolder は、自身固有の trailing コントロール（例: `SwitchCellViewHolder` の `com.google.android.material.materialswitch.MaterialSwitch`、`CheckboxCellViewHolder` の `com.google.android.material.checkbox.MaterialCheckBox`、`RadioCellViewHolder` の `KsCheckmarkAccessoryView` 相当、`SimpleCheckCellViewHolder` の checkmark View、`CommandCellViewHolder` の chevron `ImageView`）を `views.accessoryHolder` に `addView` して配置しなければならない (MUST)。`LabelCellViewHolder` および `ButtonCellViewHolder` は `accessoryHolder` を空のまま使用する（addView しない）。

`MaterialCheckBox` の右端整列規約（`cell-types-basic` の「右端アクセサリ位置の整列（Android）」Scenario）は、`CheckboxCellViewHolder` が `MaterialCheckBox` を `accessoryHolder` に追加する際に `setPadding(0, 0, 0, 0)` / `minimumWidth = 0` / `minimumHeight = 0` を設定することで満たされなければならない (MUST)。

#### ButtonCellViewHolder の aux 切替

`ButtonCellViewHolder` は、`cell.valueText` / `cell.icon` / `cell.hintText` のいずれかが指定されている場合は **通常レイアウト**（上記 `applyCellBaseLayout` を経由したレイアウト）で描画し、`titleAlignment` は title 列の中での揃え位置（`titleView.gravity`）のみに反映しなければならない (MUST)。すべて `null` の場合は **ボタンスタイル**（`titleAlignment` を Cell 全体の中央寄せ／左寄せ／右寄せに反映）で描画しなければならない (MUST)。ボタンスタイル時にも `CellBaseViews` を使うが、`iconView` / `descriptionView` / `valueTextView` / `accessoryHolder` / `hintTextView` は全て `GONE` とし、`titleView` のみを Cell 全体に広げて配置する。

#### Scenario: CellBaseViews 経由で SwitchCell が描画される

- **GIVEN** `SwitchCellViewHolder` の bind 内で `applyCellBaseLayout(views, title = "通知", description = "プッシュ通知", valueText = "オン", icon = KsImage.Resource(R.drawable.ic_notifications), hintText = "推奨", effective = effective, isEnabled = true)` を呼び、その後 `views.accessoryHolder.addView(materialSwitch)` を呼ぶ
- **WHEN** Cell が表示される
- **THEN** 本体行は左端にベルアイコン（CenterVertical）、その右に「通知」「プッシュ通知」が縦中央寄せの vertical chain で配置され（cell 縦中央付近に packed）、title 行右端寄せに「オン」、右端中央に `MaterialSwitch`（ON 状態）が配置される。`hintText` 「推奨」はセル右上に float 表示され、`MaterialSwitch` とは上端 vs 縦中央で位置が分かれるため通常は重ならない

#### Scenario: 本体行 vertical chain で title / description / valueText が cell 縦中央付近に配置される

- **GIVEN** `Theme(hasUnevenRows = true)` の `KsSettingsView` に、十分な cell 高さ（例: 80dp）を持つ `SwitchCell(title = "通知", description = "プッシュ通知", valueText = "オン")` を含むセクションが描画されている
- **WHEN** レイアウト後の `titleView` / `descriptionView` / `valueTextView` / `accessoryHolder` の Y 座標を測定する
- **THEN** `titleView` と `descriptionView` は vertical chain で packed 配置され、両者を結合した縦中心が `root.height / 2` 付近にある。`valueTextView` は `titleView.BASELINE` に紐付くため `titleView` の縦中心 Y と同じ位置にある。`accessoryHolder` の縦中心 Y も `root.height / 2` 付近にあり、本体行 / accessory が cell 縦中央付近で揃って配置される

#### Scenario: description が GONE のときも titleView が縦中央寄せ

- **GIVEN** `ButtonCell(title = "ログアウト")`（description / valueText / icon / hintText いずれも null）を描画する。`descriptionView.visibility = GONE`
- **WHEN** レイアウト後の `titleView.top` / `titleView.bottom` を測定する
- **THEN** `titleView` の縦中心 Y が `root.height / 2` 付近にある（GONE の `descriptionView` は chain member としてスペース 0 で扱われるため、titleView 単独でも packed bias 0.5 が機能し、縦中央寄せが維持される）

#### Scenario: 各 ViewHolder が applyCellBaseLayout を経由する

- **GIVEN** `ks-settingsview-ui` ソース内の `LabelCellViewHolder.kt` / `CommandCellViewHolder.kt` / `SwitchCellViewHolder.kt` / `CheckboxCellViewHolder.kt` / `RadioCellViewHolder.kt` / `SimpleCheckCellViewHolder.kt` / `ButtonCellViewHolder.kt`
- **WHEN** これらのファイルから `bind(...)` の本体を grep する
- **THEN** 各 ViewHolder は `applyCellBaseLayout(views, ...)` を呼び出しており、テキスト反映（`textView.text = ...`）・色反映・フォント反映・visibility 制御を各 ViewHolder 内で個別に書いている箇所はない。各 ViewHolder 内に残るのは「accessoryHolder への trailing コントロールの addView」と Cell 種別固有のイベントハンドラ（`OnCheckedChangeListener` 等）のみである

#### Scenario: applyCellBaseLayout が internal 可視性

- **GIVEN** `ks-settingsview-ui` の外部モジュール（例: `ks-settingsview-core` / サンプルアプリ / 後続 change で追加される未来の Cell）
- **WHEN** `import jp.kamusoft.kssettingsview.ui.applyCellBaseLayout` 後に直接呼び出そうとする
- **THEN** `internal` 可視性のためコンパイルエラーになる（同モジュール内からは呼べる）

#### Scenario: iconView と titleView の右側余白は ConstraintSet.connect の margin で与える

- **GIVEN** アイコン付きの `SwitchCell(title = "通知", icon = KsImage.Resource(...), isOn = true)` を `CellBaseViews` で描画する
- **WHEN** Robolectric / 実機で `iconView.right` と `titleView.left` を測定する
- **THEN** `titleView.left - iconView.right` は `iconMarginEnd = 16dp 相当の px` に一致する。これは `iconView.layoutParams.marginEnd` ではなく、`set.connect(titleView.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)` で margin を渡すことで成立する

#### Scenario: アイコン無しのとき titleView の左端余白は潰される

- **GIVEN** アイコン無しの `SwitchCell(title = "通知", isOn = true)` を `CellBaseViews` で描画する（`iconView.visibility = GONE`）
- **WHEN** Robolectric / 実機で `titleView.left` を測定する
- **THEN** `titleView.left` は `root.paddingLeft` 付近に張り付く。これは `set.setGoneMargin(titleView.id, ConstraintSet.START, 0)` により GONE 時に `iconMarginEnd` 余白が消失するためである

#### Scenario: hintTextView は右上 float 配置で accessoryHolder と重ならない

- **GIVEN** `CellBaseViews` を `ConstraintLayout` で構築し、`SwitchCellViewHolder` で `applyCellBaseLayout(views, title = "通知", hintText = "推奨", ...)` を呼び `accessoryHolder` に `MaterialSwitch` を追加して描画した状態
- **WHEN** 実機・エミュレータでセルをレイアウトして座標を取得する
- **THEN** `hintTextView` の `top` は `root.top` から数 dp（マージン分）のオフセットで配置され、`hintTextView` の `right` は `root.right` から数 dp のオフセットで配置される。`accessoryHolder` の縦中央 Y 座標は `root` の縦中央付近で、`hintTextView` の bottom よりも下にある（通常 hint テキスト 1 行分の高さ程度のクリアランスが空く）。両者は物理的に重ならない

#### Scenario: MinHeightConstraintLayout が minimumHeight を measure に反映する

- **GIVEN** `MinHeightConstraintLayout` を `minimumHeight = 60dp 相当 px` で構築し、子要素として高さ 10dp 相当の TextView を 1 個だけ持たせる
- **WHEN** 親から `widthSpec = EXACTLY 400dp 相当 px` / `heightSpec = UNSPECIFIED` で `measure(...)` を呼ぶ
- **THEN** `measuredHeight == 60dp 相当の px` になる（標準 `ConstraintLayout` だと子要素の合計高さ 10dp 程度を返す場合があるが、`MinHeightConstraintLayout` は `onMeasure` 後に下限ガードする）

#### Scenario: MinHeightConstraintLayout は intrinsic 値を阻害しない

- **GIVEN** `MinHeightConstraintLayout` を `minimumHeight = 60dp 相当 px` で構築し、子要素として高さ 120dp 相当の TextView を `TOP=parent.TOP` で配置する
- **WHEN** 親から `heightSpec = UNSPECIFIED` で `measure(...)` を呼ぶ
- **THEN** `measuredHeight >= 120dp 相当 px` になる（intrinsic な測定結果が `minimumHeight` を超えるときは super 由来の値を維持し、上方向の伸縮を阻害しない）

#### Scenario: buildCellBaseViews の root は MinHeightConstraintLayout 実装である

- **GIVEN** `buildCellBaseViews(ctx)` を呼ぶ
- **WHEN** 返り値の `views.root` の Kotlin クラスを確認する
- **THEN** `views.root is MinHeightConstraintLayout == true`（標準 `ConstraintLayout` のままではない）

#### Scenario: Theme 未指定時に Cell の measuredHeight が 60dp 相当 px 以上になる

- **GIVEN** `SwitchCellViewHolder` を `Theme()`（デフォルト、`hasUnevenRows = true`）で bind し、root を `LinearLayoutManager` 相当の親から `heightSpec = UNSPECIFIED` で `measure(...)` する
- **WHEN** `views.root.measuredHeight` を確認する
- **THEN** `views.root.measuredHeight >= 60dp 相当の px`（`MinHeightConstraintLayout.onMeasure` の下限ガードが効いており、実機でも `Theme()` 既定の最低高さ保証 60dp が成立する）

#### Scenario: Compose 版 KsCellRow.kt が削除されている

- **GIVEN** 本 change 適用後の `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/` ディレクトリ
- **WHEN** `KsCellRow` / `KsCellRowLayout.kt` を grep / find する
- **THEN** ファイル自体が存在せず、`@Composable fun KsCellRow(...)` の定義も削除されている。プロダクションコード内に `import androidx.compose.runtime.Composable` を通じた共通行レイアウト Composable は存在しない

#### Scenario: ButtonCellViewHolder の aux 切替

- **GIVEN-1** `ButtonCell(title: "ログアウト", titleAlignment: .center, onTap: {...})`（`icon` / `valueText` / `hintText` すべて `null`）
- **WHEN-1** `ButtonCellViewHolder.bind(...)` が描画する
- **THEN-1** ボタンスタイルが選択され、`iconView` / `descriptionView` / `valueTextView` / `accessoryHolder` / `hintTextView` は全て `GONE`、`titleView` のみが Cell 全体に広がり、`titleAlignment = .center` により中央寄せで「ログアウト」が表示される
- **GIVEN-2** `ButtonCell(title: "登録", valueText: "送信", icon: KsImage.Resource(R.drawable.ic_send), titleAlignment: .start, onTap: {...})`
- **WHEN-2** `ButtonCellViewHolder.bind(...)` が描画する
- **THEN-2** 通常レイアウトが選択され、`applyCellBaseLayout` 経由で左端アイコン、`titleView`（左寄せ／`titleAlignment = .start` を title 列内 gravity に反映）、title 行右寄せに valueText「送信」が配置される
