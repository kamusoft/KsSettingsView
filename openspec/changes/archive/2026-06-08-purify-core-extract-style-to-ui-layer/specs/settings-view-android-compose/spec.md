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

### Requirement: Compose DSL

宣言的 DSL（`@SettingsRootDsl` を用いた `SettingsRootScope` 等）を提供し、Compose 内で Cell ツリーを構築できなければならない (SHALL)。DSL は以下の要素を含む完全な宣言的記法を実現しなければならない (MUST)：

- **`@SettingsRootDsl` annotation 付き Scope クラス**: `SettingsRootScope`、`SectionScope`、`SettingsRootScopeAtRoot` 等
- **`forEach` 関数** (Compose 用、Kotlin 標準 `forEach` と衝突しない命名):
  - ルート用: `SettingsRootScope.forEach<T>(items: List<T>, content: SettingsRootScope.(T) -> Unit)`
  - セクション内用: `SectionScope.forEach<T>(items: List<T>, content: SectionScope.(T) -> Unit)`
- **Section の DSL 専用関数**:
  - `Section(header: String? = null, footer: String? = null, content: SectionScope.() -> Unit)`
  - `Section(header: SectionAccessory? = null, footer: SectionAccessory? = null, content: SectionScope.() -> Unit)`
  - 後方互換のため、既存の `section(id = ..., header = ..., footer = ..., content = ...)` (id 必須) も維持
- **DSL Cell 識別性 interface**: `DSLReidentifiableCell` / `DSLStyleModifiableCell`
  - これらの interface は `ks-settingsview-core` モジュール（パッケージ `jp.kamusoft.kssettingsview.core`）に定義しなければならない (MUST)
  - 後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `ks-settingsview-ui` モジュールに配置されるため、Core モジュールに置くことで依存方向を保つ
- **Section の ID 自動採番**:
  - `Section(...)` 関数は ID 省略時に自動採番（同一性判定戦略に従う）
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
- **具象 Cell コンストラクタの `id` デフォルト値規約**:
  - 具象 Cell 実装（`LabelCell` 等）は `id: String` パラメータに **UUID ベースのデフォルト値** を持たせなければならない (SHALL)

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

### Requirement: DSL → SettingsRootDiff 算出ロジック（Compose）

`ks-settingsview-compose` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsView.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは iOS 側と完全に同等でなければならない (MUST)：

1. **Section レベルの突合**：Section ID 集合の比較で `InsertSection` / `RemoveSection` / `MoveSection` / `UpdateAccessory`（Section H/F 用）を発行
2. **各 Section 内の Cell レベルの突合**：Cell ID 集合の比較で `InsertCell` / `RemoveCell` / `MoveCell` を発行する（構造変化＝追加・削除・移動・id 変化のみ）。**両セクションに同一 Cell ID が存在し内容（プロパティ）だけが異なる場合、`ReplaceCell` を構造同期の差分として発行してはならない** (MUST NOT)。同一 id の内容更新は ViewHolder の部分更新経路で反映する
3. **Root H/F の突合**：`rootHeader` / `rootFooter` パラメータの値が変化した場合 `UpdateAccessory`（Root H/F 用）を発行
4. **Theme の突合**：Theme は `SettingsRootDiff` には含まれない (MUST NOT)。Theme の変化は `KsSettingsView(theme = ...)` パラメータの再評価で `store.applyTheme(newTheme)` を呼ぶ経路で反映される（独立 API）
5. **構造同期の同一性判定対象**：Section / Cell の **id 同一性のみ** で追加・削除・移動を判定する。Cell の内容プロパティを構造同期の判定に用いてはならない (MUST NOT)
6. **任意 View 形式（`SectionAccessory.View(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.View` ケース同士・`RootAccessory.View` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `UpdateAccessory` Diff は **発行しない**
   - 異なるケース（`Text` → `View` または `View` → `Text`、`null` → `View` 等）の場合のみ `UpdateAccessory` Diff を発行

#### Scenario: Cell 内容変更時は ReplaceCell を発行しない

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`（Section ID・Cell ID は同じ、内容のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff（`InsertCell` / `RemoveCell` / `MoveCell` / `ReplaceCell`）は発行されない

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertCell(sectionId = <same>, index = 1, cell = LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveCell(cellId = <B の ID>)` のみが発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveCell(cellId = <B の ID>, toIndex = 2)` または `SettingsRootDiff.MoveCell(cellId = <C の ID>, toIndex = 1)` のいずれか（実装定義）が発行される

#### Scenario: Theme 変化時の Diff 不発行

- **GIVEN** 旧 `KsSettingsView(theme = themeA) { ... }` と新 `KsSettingsView(theme = themeB) { ... }`（root 内容は不変）
- **WHEN** Recomposition が起こる
- **THEN** `SettingsRootDiff` は何も発行されない。代わりに `store.applyTheme(themeB)` が呼ばれて `KsSettingsView.theme` プロパティが更新される
