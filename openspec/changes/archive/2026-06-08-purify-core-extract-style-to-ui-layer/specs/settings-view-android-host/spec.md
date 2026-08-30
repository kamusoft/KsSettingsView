## MODIFIED Requirements

### Requirement: KsSettingsView の公開 API

`KsSettingsView` は `FrameLayout` を継承し、`SettingsRootStore` のバインドおよび `applyDiff(_:)` メソッドにより内部 `RecyclerView` の `ConcatAdapter`（`headerAdapter`、`mainListAdapter`、`footerAdapter` の 3 段構成）を更新しなければならない (SHALL)。`mainListAdapter` には Section H/F + Cell の平坦リストを `submitList` で渡し、`headerAdapter` / `footerAdapter` には `rootHeader` / `rootFooter` プロパティを渡す (MUST)。

`var root: SettingsRoot` の公開 setter は廃止し (MUST NOT)、内部状態は `SettingsRootStore` または `applyDiff(_:)` 経由でのみ更新可能としなければならない (MUST)。Test / Preview 用に `internal fun setRootDirect(root: SettingsRoot, theme: Theme = Theme())` を提供しなければならない (MUST)。

`KsSettingsView` は Root H/F 用プロパティ `var rootHeader: RootAccessory?` および `var rootFooter: RootAccessory?` を持たなければならない (MUST)。setter で `RootHeaderFooterAdapter` の表示状態を更新する。

**Theme 経路**: `KsSettingsView` は `var theme: Theme` プロパティを公開し、setter で `RecyclerView.backgroundColor` および各表示中 ViewHolder の実効スタイルを再評価しなければならない (MUST)。Store バインド時は `store.theme` の `StateFlow` を `lifecycleScope` で `collect` し、変更を View に反映する。**Theme 更新は `applyDiff` 経路を通らない (MUST NOT)**。`SettingsRoot(sections = ...)` には `theme` 引数は存在しない (MUST NOT)。

本 View は通常の Android XML レイアウトおよび Compose `AndroidView` 経由、MAUI バインディングから利用される (MUST)。

#### Scenario: Store バインドでの初期化

- **GIVEN** `val store = SettingsRootStore(initialRoot = ..., initialTheme = someTheme)` で生成した Store
- **WHEN** `view.bind(store)` を呼ぶ
- **THEN** View は Store の `state.value` で初期 root を反映し、`store.diffs` の `Flow` と `store.theme` の `StateFlow` を `lifecycleScope` で `collect` 購読する

#### Scenario: Store メソッド呼び出しで表示が更新

- **GIVEN** `view.bind(store)` 済み
- **WHEN** `store.insertCell(newCell, sectionId = sid, at = 0)` を呼ぶ
- **THEN** View は Diff を購読経路で受け取り、`applyDiff(_:)` を介して `mainListAdapter` の内部 `List<CellListItem>` を 1 件だけ更新し `submitList` する

#### Scenario: applyDiff の直接呼び出し

- **GIVEN** `KsSettingsView` インスタンス
- **WHEN** `view.applyDiff(SettingsRootDiff.RemoveCell(cellId = someId))` を呼ぶ
- **THEN** 該当 Cell が `mainListAdapter` の内部リストから削除され、`submitList` で `DiffUtil` により削除アニメーションが反映される

#### Scenario: Theme プロパティ更新で表示が再評価

- **GIVEN** `KsSettingsView` インスタンスが画面表示中
- **WHEN** `view.theme = Theme(separatorColor = Color(0xFFE6DAB9))` を代入する
- **THEN** `RecyclerView.backgroundColor` が新 Theme の `viewBackgroundColor` に更新され、表示中の各 ViewHolder が新 Theme で再 bind される。`mainListAdapter.submitList(...)` は呼ばれない（構造差分ではないため）

#### Scenario: setRootDirect の利用（Test/Preview）

- **GIVEN** `val view = KsSettingsView(context)`、テストコード
- **WHEN** `view.setRootDirect(SettingsRoot(sections = listOf(...)), theme = Theme())` を呼ぶ
- **THEN** Store を介さず直接 root と theme が反映され、`mainListAdapter` に平坦化リストが submit される

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsView(context)` を初期化した直後
- **WHEN** Activity に attach される
- **THEN** 内部 `RecyclerView` および `ListAdapter` が準備済み、空 `SettingsRoot(sections = emptyList())` 相当のリストが提示されエラーなく描画される

#### Scenario: rootHeader の設定

- **GIVEN** `KsSettingsView` インスタンス
- **WHEN** `view.rootHeader = RootAccessory.Text("プロフィール")` を代入する
- **THEN** `headerAdapter.itemCount = 1` となり、RecyclerView 先頭に "プロフィール" のヘッダ ViewHolder が描画される

#### Scenario: rootHeader を null にすると削除

- **GIVEN** `view.rootHeader = RootAccessory.Text("X")` で描画中
- **WHEN** `view.rootHeader = null` を代入する
- **THEN** `headerAdapter.notifyItemRemoved(0)` 等が発行され、`headerAdapter.itemCount = 0` となる

### Requirement: SettingsRootStore（Android）

`ks-settingsview-ui` モジュールは、`SettingsRoot` の状態管理と部分更新 Diff 発行を担う `SettingsRootStore` クラスを提供しなければならない (SHALL)。`SettingsRootStore` は `class SettingsRootStore(initialRoot: SettingsRoot, initialTheme: Theme = Theme())` であり、`val state: StateFlow<SettingsRoot>` プロパティで現在の root を、**`val theme: StateFlow<Theme>` プロパティで現在の Theme を公開しなければならない (MUST)**。内部に `MutableSharedFlow<SettingsRootDiff>`（replay=0）で Diff を発行し、UI 層 View がこれを購読することで `applyDiff(_:)` を呼ぶ統合経路を確立しなければならない (MUST)。

`SettingsRootStore` は以下のメソッドを公開しなければならない (MUST)：

- `fun replaceAll(root: SettingsRoot)`
- `fun insertSection(section: Section, at: Int)`
- `fun removeSection(sectionId: String)`
- `fun moveSection(from: Int, to: Int)`
- `fun replaceSection(sectionId: String, new: Section)`
- `fun insertCell(cell: Cell, sectionId: String, at: Int)`
- `fun removeCell(cellId: String)`
- `fun replaceCell(cellId: String, new: Cell)`
- `fun moveCell(cellId: String, to: Int)`
- `fun updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)`
- `fun applyTheme(theme: Theme)`

**`applyTheme(_:)` は Diff SharedFlow 経路を通らない (MUST NOT)**。代わりに内部の `MutableStateFlow<Theme>` を更新し、購読者（KsSettingsView）が Theme 変更通知を受けて View に反映する。

Preview / Test 用ファクトリとして `companion object { fun preview(root: SettingsRoot, theme: Theme = Theme()): SettingsRootStore }` を提供しなければならない (MUST)。

#### Scenario: Store の初期化と root / theme 取得

- **GIVEN** `val initial = SettingsRoot(sections = listOf(...))` と `val theme = Theme(separatorColor = Color(0xFFE6DAB9))`
- **WHEN** `val store = SettingsRootStore(initialRoot = initial, initialTheme = theme)` を構築する
- **THEN** `store.state.value` は `initial` と等価、`store.theme.value` は `theme` と等価になる

#### Scenario: Store の Theme 省略時の初期化

- **GIVEN** `val initial = SettingsRoot(sections = listOf(...))`
- **WHEN** `val store = SettingsRootStore(initialRoot = initial)` を構築する
- **THEN** `store.theme.value` は `Theme()` の既定値になる

#### Scenario: insertCell メソッド呼び出し

- **GIVEN** Store が初期化済み、Section が 1 つ存在
- **WHEN** `store.insertCell(newCell, sectionId = sid, at = 0)` を呼ぶ
- **THEN** `store.state.value` の該当 Section の `cells[0]` が `newCell` になり、内部 SharedFlow が `InsertCell(sectionId = sid, index = 0, cell = newCell)` を emit する

#### Scenario: removeCell メソッド呼び出し

- **GIVEN** Store が初期化済み、Section に Cell が複数存在
- **WHEN** `store.removeCell(cellId = someId)` を呼ぶ
- **THEN** `store.state.value` の該当 Section から `cellId` を持つ Cell が除去され、SharedFlow が `RemoveCell(cellId = someId)` を emit する

#### Scenario: updateAccessory メソッド呼び出し

- **GIVEN** Store が初期化済み
- **WHEN** `store.updateAccessory(target = AccessoryTarget.RootHeader, accessory = SettingsAccessory.Root(RootAccessory.Text("X")))` を呼ぶ
- **THEN** SharedFlow が `UpdateAccessory(target = AccessoryTarget.RootHeader, accessory = ...)` を emit する

#### Scenario: applyTheme メソッド呼び出し

- **GIVEN** Store が初期化済み、現在 Theme は既定値
- **WHEN** `store.applyTheme(Theme(separatorColor = Color(0xFFE6DAB9)))` を呼ぶ
- **THEN** `store.theme.value` が新 Theme に更新され、`theme` StateFlow が通知を emit する。`SettingsRootDiff` SharedFlow は何も emit しない

#### Scenario: preview ファクトリの利用

- **GIVEN** Compose Preview コードで `val store = remember { SettingsRootStore.preview(root = ...) }`
- **WHEN** `KsSettingsView(store = store)` を Preview に表示する
- **THEN** 通常の Store と同じ動作で Preview が表示される

#### Scenario: StateFlow の通知

- **GIVEN** Store と Compose 関数（`store.state.collectAsState()` で監視）
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** `state` の Flow に新しい root が emit され、Composable が再 Composition される（必要に応じて）
