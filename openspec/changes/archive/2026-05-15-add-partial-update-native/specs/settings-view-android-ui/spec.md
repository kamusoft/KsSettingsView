## MODIFIED Requirements

### Requirement: KsSettingsView の公開 API

`KsSettingsView` は `FrameLayout` を継承し、`SettingsRootStore` のバインドおよび `applyDiff(_:)` メソッドにより内部 `RecyclerView` の `ConcatAdapter`（`headerAdapter`、`mainListAdapter`、`footerAdapter` の 3 段構成）を更新しなければならない (SHALL)。`mainListAdapter` には Section H/F + Cell の平坦リストを `submitList` で渡し、`headerAdapter` / `footerAdapter` には `rootHeader` / `rootFooter` プロパティを渡す (MUST)。

`var root: SettingsRoot` の公開 setter は廃止し (MUST NOT)、内部状態は `SettingsRootStore` または `applyDiff(_:)` 経由でのみ更新可能としなければならない (MUST)。Test / Preview 用に `internal fun setRootDirect(root: SettingsRoot)` を提供しなければならない (MUST)。

`KsSettingsView` は Root H/F 用プロパティ `var rootHeader: RootAccessory?` および `var rootFooter: RootAccessory?` を持たなければならない (MUST)。setter で `RootHeaderFooterAdapter` の表示状態を更新する。

本 View は通常の Android XML レイアウトおよび Compose `AndroidView` 経由、MAUI バインディングから利用される (MUST)。

#### Scenario: Store バインドでの初期化

- **GIVEN** `val store = SettingsRootStore(initialRoot = ...)` で生成した Store
- **WHEN** `view.bind(store)` を呼ぶ
- **THEN** View は Store の `state.value` で初期 root を反映し、`store.diffs` の `Flow` を `lifecycleScope` で `collect` 購読する

#### Scenario: Store メソッド呼び出しで表示が更新

- **GIVEN** `view.bind(store)` 済み
- **WHEN** `store.insertCell(newCell, sectionId = sid, at = 0)` を呼ぶ
- **THEN** View は Diff を購読経路で受け取り、`applyDiff(_:)` を介して `mainListAdapter` の内部 `List<CellListItem>` を 1 件だけ更新し `submitList` する

#### Scenario: applyDiff の直接呼び出し

- **GIVEN** `KsSettingsView` インスタンス
- **WHEN** `view.applyDiff(SettingsRootDiff.RemoveCell(cellId = someId))` を呼ぶ
- **THEN** 該当 Cell が `mainListAdapter` の内部リストから削除され、`submitList` で `DiffUtil` により削除アニメーションが反映される

#### Scenario: setRootDirect の利用（Test/Preview）

- **GIVEN** `val view = KsSettingsView(context)`、テストコード
- **WHEN** `view.setRootDirect(SettingsRoot(sections = listOf(...), theme = Theme()))` を呼ぶ
- **THEN** Store を介さず直接 root が反映され、`mainListAdapter` に平坦化リストが submit される

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsView(context)` を初期化した直後
- **WHEN** Activity に attach される
- **THEN** 内部 `RecyclerView` および `ListAdapter` が準備済み、空 `SettingsRoot(sections = emptyList(), theme = Theme())` 相当のリストが提示されエラーなく描画される

#### Scenario: rootHeader の設定

- **GIVEN** `KsSettingsView` インスタンス
- **WHEN** `view.rootHeader = RootAccessory.Text("プロフィール")` を代入する
- **THEN** `headerAdapter.itemCount = 1` となり、RecyclerView 先頭に "プロフィール" のヘッダ ViewHolder が描画される

#### Scenario: rootHeader を null にすると削除

- **GIVEN** `view.rootHeader = RootAccessory.Text("X")` で描画中
- **WHEN** `view.rootHeader = null` を代入する
- **THEN** `headerAdapter.notifyItemRemoved(0)` 等が発行され、`headerAdapter.itemCount = 0` となる

### Requirement: Compose ラッパ KsSettingsView

`@Composable fun KsSettingsView(store: SettingsRootStore, modifier: Modifier = Modifier, style: KsSettingsViewStyle = KsSettingsViewStyle.Classic, headerView: (@Composable () -> Unit)? = null, footerView: (@Composable () -> Unit)? = null)` を提供し、Compose から利用できなければならない (SHALL)。内部は `AndroidView` で `KsSettingsView`（FrameLayout）を埋め込み、`factory` で `view.bind(store)` を呼び、`update` で `style` / `headerView` / `footerView` を反映しなければならない (MUST)。

旧 `root: SettingsRoot` / `onChange: (SettingsRoot) -> Unit` 引数は廃止する (MUST NOT)。

#### Scenario: Compose からの利用

- **GIVEN** Compose 関数内で `val store = remember { SettingsRootStore(initialRoot = ...) }` 宣言
- **WHEN** `KsSettingsView(store = store)` を Composition する
- **THEN** `AndroidView.factory` で `KsSettingsView` が作られ、`view.bind(store)` が呼ばれて Store の Diff Flow が購読される

#### Scenario: Store メソッド呼び出しで再描画

- **GIVEN** `KsSettingsView(store = store)` が描画中
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** View 側で `applyDiff` が呼ばれて新規 Cell 行が挿入アニメーションで追加される

#### Scenario: headerView 引数の指定

- **GIVEN** Compose 関数内で `KsSettingsView(store = store, headerView = { Text("プロフィール") })` と記述
- **WHEN** 初回 Composition される
- **THEN** `view.rootHeader = RootAccessory.View(KsAnyView.Compose { Text("プロフィール") })` 相当に内部変換され、Compose で描画される

#### Scenario: headerView を null にする

- **GIVEN** `KsSettingsView(store = store, headerView = { ... })` で描画中
- **WHEN** Recomposition で `headerView = null` に変化
- **THEN** `view.rootHeader = null` が反映され、RecyclerView 先頭の Header が削除される

### Requirement: DiffUtil 差分検出

`DiffUtil.ItemCallback<CellListItem>` を実装し、`areItemsTheSame` は ID 比較、`areContentsTheSame` は data class equals でなければならない (MUST)。`SectionAccessory.View(KsAnyView)` の `KsAnyView` 中身は `areContentsTheSame` の判定対象から除外されなければならない (MUST)（`KsAnyView` は差分検出に参加せず、`View` ケース同士はケース一致のみで等価とみなす）。

`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、内部 `List<CellListItem>` を変更し、`mainListAdapter.submitList(newList)` を呼ばなければならない (MUST)。`DiffUtil` のバックグラウンド差分計算により、追加・削除・移動・置換のアニメーションが自動的に適用される。

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.InsertCell(sectionId = sid, index = 0, cell = newCell))`
- **WHEN** `submitList` 後の DiffUtil 差分計算を観察する
- **THEN** 内部リストに 1 件だけ `CellListItem.CellRow` が挿入され、対応する Cell 行のみ挿入アニメーションが発生する

#### Scenario: Cell 削除時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.RemoveCell(cellId = cid))`
- **WHEN** `submitList` 後の差分計算を観察する
- **THEN** 該当 `CellListItem.CellRow` が削除され、その行のみ削除アニメーションが発生する

#### Scenario: Theme 更新

- **GIVEN** `view.applyDiff(SettingsRootDiff.UpdateTheme(newTheme))`
- **WHEN** 適用後の描画を観察する
- **THEN** すべての可視 Cell の bind が新 Theme で再呼び出しされる

#### Scenario: 存在しない cellId への操作（DEBUG）

- **GIVEN** 内部リストに存在しない `cellId` を持つ `RemoveCell` Diff
- **WHEN** DEBUG ビルドで `view.applyDiff(RemoveCell(cellId = notExistId))` を呼ぶ
- **THEN** `error(...)` などで即座にクラッシュする

#### Scenario: 存在しない cellId への操作（Release）

- **GIVEN** 内部リストに存在しない `cellId` を持つ `RemoveCell` Diff
- **WHEN** Release ビルドで `view.applyDiff(RemoveCell(cellId = notExistId))` を呼ぶ
- **THEN** クラッシュせず、`Log.w` でログ出力されるのみで内部リストは変更されない

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsView` は `rootHeader: RootAccessory?` / `rootFooter: RootAccessory?` を UI 層プロパティとして持ち、`ConcatAdapter` の先頭 / 末尾に位置する `RootHeaderFooterAdapter` で描画しなければならない (SHALL)。`RootHeaderFooterAdapter` は `view: RootAccessory?` プロパティを持ち、`null` のとき `getItemCount()` は 0、非 `null` のとき 1 を返さなければならない (MUST)。`view` プロパティの setter は変化前後の `null` / 非 `null` 状態に応じて `notifyItemInserted(0)` / `notifyItemRemoved(0)` / `notifyItemChanged(0)` を発行しなければならない (MUST)。`headerAdapter.getItemId(0)` は `1L`、`footerAdapter.getItemId(0)` は `2L` を予約値として返し、`mainListAdapter` 側の `getItemId` はこれと衝突しない値域を使わなければならない (MUST)。

`RootHeaderFooterAdapter` の ViewHolder は `RootAccessory.Text(String)` を TextView 描画、`RootAccessory.View(KsAnyView)` を `ComposeView.setContent`（Compose backing）または `addView`（Android View backing）で描画しなければならない (MUST)。`SettingsRoot` 値型自体には `header` / `footer` を含まないため (MUST NOT)、本 Requirement の入力源は UI 層プロパティ（`view.rootHeader` 代入、Compose ラッパの `headerView` 引数、または `SettingsRootStore.updateAccessory(target: AccessoryTarget.RootHeader, accessory:)` Diff 経由）のみとする。

<!-- 注: `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、Root H/F の入力源を UI 層プロパティ（`KsSettingsView.rootHeader` / `rootFooter`、Compose ラッパの `headerView` / `footerView` 引数、`SettingsRootStore.updateAccessory(target: AccessoryTarget.RootHeader/RootFooter, accessory:)` Diff 経由）に変更している。`RootHeaderFooterAdapter` の描画ロジック自体は維持される。Requirement 名は archive 済 spec との連続性を保つため変更しないが、説明文と Scenario は新 API に合わせて書き直している。 -->

#### Scenario: Root Header（Text）の描画

- **GIVEN** `view.rootHeader = RootAccessory.Text("プロフィール")` を代入
- **WHEN** RecyclerView を描画する
- **THEN** RecyclerView 先頭に "プロフィール" を表示する 1 つの ViewHolder が描画される（`headerAdapter.itemCount = 1`）

#### Scenario: Root Footer（View、Compose backing）の描画

- **GIVEN** `view.rootFooter = RootAccessory.View(KsAnyView.Compose { Text("v1.0.0") })` を代入
- **WHEN** RecyclerView を描画する
- **THEN** RecyclerView 末尾に Compose で `Text("v1.0.0")` が描画される ViewHolder が表示される（`footerAdapter.itemCount = 1`）

#### Scenario: Root H/F が null の場合

- **GIVEN** `view.rootHeader = null` および `view.rootFooter = null`
- **WHEN** RecyclerView を描画する
- **THEN** `headerAdapter.itemCount = 0` および `footerAdapter.itemCount = 0` となり、RecyclerView は `mainListAdapter` の Section H/F + Cell のみを描画する

#### Scenario: Root Header の追加・削除通知

- **GIVEN** `view.rootHeader = null` の状態
- **WHEN** `view.rootHeader = RootAccessory.Text("新規")` に変更する
- **THEN** `headerAdapter.notifyItemInserted(0)` が発行され、Header 1 行が挿入アニメーションで追加される

#### Scenario: Store 経由の Accessory 更新

- **GIVEN** Store が初期化済み、View が Store にバインド済み
- **WHEN** `store.updateAccessory(target = AccessoryTarget.RootHeader, accessory = SettingsAccessory.Root(RootAccessory.Text("X")))` を呼ぶ
- **THEN** Store が `UpdateAccessory(...)` Diff を発行し、View の `applyDiff` が `rootHeader` を `RootAccessory.Text("X")` に更新する

#### Scenario: ID 衝突回避

- **GIVEN** `headerAdapter` / `footerAdapter` / `mainListAdapter` がすべて `setHasStableIds(true)` の場合
- **WHEN** ConcatAdapter 内の各 adapter の `getItemId` を確認する
- **THEN** `headerAdapter.getItemId(0) = 1L`、`footerAdapter.getItemId(0) = 2L`、`mainListAdapter` の各 ID は 1L / 2L と衝突しない値域（例: `100L` 以上、または Cell の Hashable から派生する Long）を返す

### Requirement: メモリリーク防止

`KsSettingsView`（FrameLayout）は `onDetachedFromWindow` で内部 RecyclerView の adapter を `null` にし、ListAdapter の参照を解放しなければならない (MUST)。さらに `SettingsRootStore` の Diff `Flow` を購読している場合、`lifecycleScope` または `viewTreeLifecycleOwner` 経由で確実に cancel されなければならない (MUST)。

#### Scenario: View が detach される

- **GIVEN** `KsSettingsView` が Activity に attach されたのち remove される
- **WHEN** `onDetachedFromWindow` が呼ばれる
- **THEN** `recyclerView.adapter == null` となり、ListAdapter への参照が解放される

#### Scenario: Store 購読の解除

- **GIVEN** `view.bind(store)` 済みの View が `onDetachedFromWindow` される
- **WHEN** detach 後の状態を観察する
- **THEN** Store の Diff Flow 購読 Job が cancel され、View への強参照が残らない

## ADDED Requirements

### Requirement: SettingsRootStore（Android）

`ks-settingsview-ui` モジュールは、`SettingsRoot` の状態管理と部分更新 Diff 発行を担う `SettingsRootStore` クラスを提供しなければならない (SHALL)。`SettingsRootStore` は `class SettingsRootStore(initialRoot: SettingsRoot)` であり、`val state: StateFlow<SettingsRoot>` プロパティで現在の root を公開しなければならない (MUST)。内部に `MutableSharedFlow<SettingsRootDiff>`（replay=0）で Diff を発行し、UI 層 View がこれを購読することで `applyDiff(_:)` を呼ぶ統合経路を確立しなければならない (MUST)。

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
- `fun updateTheme(theme: Theme)`

Preview / Test 用ファクトリとして `companion object { fun preview(root: SettingsRoot): SettingsRootStore }` を提供しなければならない (MUST)。

#### Scenario: Store の初期化と root 取得

- **GIVEN** `val initial = SettingsRoot(sections = listOf(...), theme = Theme())`
- **WHEN** `val store = SettingsRootStore(initialRoot = initial)` を構築する
- **THEN** `store.state.value` は `initial` と等価になる

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

#### Scenario: preview ファクトリの利用

- **GIVEN** Compose Preview コードで `val store = remember { SettingsRootStore.preview(root = ...) }`
- **WHEN** `KsSettingsView(store = store)` を Preview に表示する
- **THEN** 通常の Store と同じ動作で Preview が表示される

#### Scenario: StateFlow の通知

- **GIVEN** Store と Compose 関数（`store.state.collectAsState()` で監視）
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** `state` の Flow に新しい root が emit され、Composable が再 Composition される（必要に応じて）
