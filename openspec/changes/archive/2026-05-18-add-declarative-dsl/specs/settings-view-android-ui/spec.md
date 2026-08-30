## MODIFIED Requirements

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
   - `add-partial-update-native` で導入された経路を維持する
   - パワーユーザー向け（大量データ・無限スクロール・命令型操作が必要なケース）
   - `add-partial-update-native` で導入された `headerView` / `footerView` パラメータは本提案で **削除** し (MUST NOT)、`rootHeader` / `rootFooter` パラメータに改名・一本化する。本ライブラリは運用前のため互換維持は不要

<!-- 注: 本 BREAKING 変更は `add-partial-update-native` の archive 完了後、本提案の archive で連続的に適用される。
     archive 順序は proposal.md の依存関係セクション参照。 -->

2. **DSL 方式**:
   ```kotlin
   @Composable
   fun KsSettingsView(
       modifier: Modifier = Modifier,
       style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
       rootHeader: (@Composable () -> Unit)? = null,
       rootFooter: (@Composable () -> Unit)? = null,
       content: SettingsRootScope.() -> Unit,
   )
   ```
   - 宣言的に Cell ツリーを記述する Compose 流儀の経路
   - 内部で `remember { SettingsRootStore(...) }` を保持し、Recomposition のたびに新旧の宣言ツリーを比較して `SettingsRootDiff` 列を算出、内部 Store の Diff 経路に流す
   - 一般用途（静的・数十〜数百セルの典型的な設定画面）向け

両方の関数で `AndroidView` を内部で利用し、`factory` で `KsSettingsView`（FrameLayout）を作成し `view.bind(store)` を呼ぶ。`update` で `style` / `rootHeader` / `rootFooter` を反映する。

旧 `root: SettingsRoot` / `onChange: (SettingsRoot) -> Unit` 引数は廃止された状態のままとする (MUST NOT 復活)。

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
- **`DSLReidentifiableCell` / `DSLStyleModifiableCell` interface の配置モジュール**：
  - これらの interface は `ks-settingsview-core` モジュール（パッケージ `jp.kamusoft.kssettingsview.core`）に定義しなければならない (MUST)
  - 後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `ks-settingsview-ui` モジュールに配置されるため、`ks-settingsview-ui` の Cell が `DSLReidentifiableCell` を implement できるよう、最下層 Core モジュールに置く（`ks-settingsview-ui → ks-settingsview-compose` の循環依存回避）
  - `ks-settingsview-compose` モジュール内の DSL ロジック（`DSLNodes.kt` 等）は Core に置かれた interface を import して利用する
- **Section の ID 自動採番**：
  - 既存の `section(id: String, ...)` は明示 ID 指定が必須のままだが、新規 `Section(...)` 関数は ID 省略時に自動採番（同一性判定戦略に従う）
- **Cell の Modifier 風 API**：以下の **2 系統**を併存させる
  - **`CellHandle` 経由 chain**（DSL 内推奨）: `Section("...") { LabelCell(title = "...").cellHeight(80.dp).font(...) }`
    - `CellHandle.font(font: KsFont): CellHandle`
    - `CellHandle.icon(icon: KsIcon): CellHandle`
    - `CellHandle.cellHeight(height: Dp): CellHandle`
    - `CellHandle.titleColor(color: KsColor): CellHandle`
    - `CellHandle.backgroundColor(color: KsColor): CellHandle`
    - `CellHandle.disabled(flag: Boolean): CellHandle`
    - `CellHandle.cellID(id: Any): CellHandle`：明示 Cell ID
    - すべて `@SettingsRootDsl` 付き拡張関数として実装し、内部の `DSLSectionScope` 経由で対応する `DSLCellNode` を更新する
  - **`Cell` 値型 modifier**（既存、外部 Cell 値や Store 方式での利用用に維持）:
    - `Cell.font(font: KsFont): Cell` / `Cell.cellHeight(height: Dp): Cell` / `Cell.cellID(id: Any): Cell` 等
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

DSL は内部 `SettingsRootStore` の初期化に使われると同時に、Recomposition のたびに新ツリーを構築して旧ツリーとの Diff を算出する責務を持つ (MUST)。

#### Scenario: DSL から SettingsRoot 構築（既存 settingsRoot 関数）

- **GIVEN** Kotlin コード内で
  ```kotlin
  val root = settingsRoot(theme = Theme()) {
      section(id = "user", header = "一般") { /* cell(LabelCell(...)) */ }
  }
  ```
  と記述
- **WHEN** `root` を評価する
- **THEN** `SettingsRoot.sections` に 1 つの `Section` が含まれ、その `cells` に DSL で記述された Cell が並ぶ（既存挙動を維持）

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

## ADDED Requirements

### Requirement: DSL → SettingsRootDiff 算出ロジック（Compose）

`ks-settingsview-compose` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsView.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは iOS 側（`settings-view-ios-ui` capability の同名 Requirement）と完全に同等でなければならない (MUST)：

1. **Section レベルの突合**：Section ID 集合の比較で `InsertSection` / `RemoveSection` / `MoveSection` / `UpdateAccessory`（Section H/F 用）を発行
2. **各 Section 内の Cell レベルの突合**：Cell ID 集合の比較で `InsertCell` / `RemoveCell` / `MoveCell` / `ReplaceCell` を発行
3. **Root H/F の突合**：`rootHeader` / `rootFooter` パラメータの値が変化した場合 `UpdateAccessory`（Root H/F 用）を発行
4. **Theme の突合**：Theme が変化した場合 `UpdateTheme` を発行
5. **Cell 値の比較対象**：`KsAnyView` を含むフィールドは比較対象から除外、その他は data class equals で自動比較
6. **任意 View 形式（`SectionAccessory.View(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.View` ケース同士・`RootAccessory.View` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `UpdateAccessory` Diff は **発行しない**
   - 異なるケース（`Text` → `View` または `View` → `Text`、`null` → `View` 等）の場合のみ `UpdateAccessory` Diff を発行

#### Scenario: Cell 内容変更時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`（Section ID・Cell ID は同じ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.ReplaceCell(cellId = <same>, new = LabelCell("Hanako"))` のみが発行される

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertCell(sectionId = <same>, index = 1, cell = LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveCell(cellId = <B の ID>)` のみが発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`（同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveCell(cellId = <B の ID>, toIndex = 2)` または `SettingsRootDiff.MoveCell(cellId = <C の ID>, toIndex = 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない（Cell 値は等価のため `ReplaceCell` は発行されない）

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

- **GIVEN** 旧ツリー `Section(headerContent = { CardA() }) { ... }` と新ツリー `Section(headerContent = { CardB() }) { ... }`（同 Section ID、Header が両方 `View` ケース）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`View` ケース同士は等価とみなされ `UpdateAccessory` Diff は発行されない。任意 Composable の中身更新は既存仕様通り `ComposeView.setContent` の再実行に委ねられる

#### Scenario: 任意 Composable 形式の Root H/F が変化しても UpdateAccessory 非発行

- **GIVEN** 旧 `KsSettingsView(rootHeader = { HeaderA() }) { ... }` と新 `KsSettingsView(rootHeader = { HeaderB() }) { ... }`（両方とも任意 Composable 指定）
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

- **GIVEN** 旧 `KsSettingsView(rootHeader = null) { ... }` と新 `KsSettingsView(rootHeader = { Text("新") }) { ... }`（`null` から `View` ケースへの遷移）
- **WHEN** Recomposition で Diff 算出ロジックを実行
- **THEN** ケース変化（`null` → `View`）が検出され、`SettingsRootDiff.UpdateAccessory(target = RootHeader, accessory = SettingsAccessory.Root(RootAccessory.View(...)))` が発行される

注: 同じ `View` ケース同士の Composable 中身変化は `UpdateAccessory` 非発行となる（直前の Scenario 参照）。Compose の `rootHeader` は `(@Composable () -> Unit)?` 型のため、ケース変化として観測可能なのは `null` ↔ 非 `null` の遷移のみとなる。

#### Scenario: 同一ツリーで Diff 空

- **GIVEN** 旧ツリーと新ツリーが完全に同一（Cell の equals 比較で全一致）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 発行される Diff 列は空となり、`applyDiff` は呼ばれない

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
