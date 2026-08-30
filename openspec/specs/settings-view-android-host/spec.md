# settings-view-android-host Specification

## Purpose

`settings-view-android-host` は、`ks-settingsview-ui`（Android）の **ホスト層**（`KsSettingsView` 本体・`RecyclerView` ベースのリスト基盤・Cell レジストリと ViewHolder 抽象・`SettingsRootStore`・ライフサイクル管理）を担う capability である。`KsSettingsViewCore` のドメインモデル (`SettingsRoot` / `Section` / `Cell`) を入力として、`RecyclerView` + `ConcatAdapter`（`headerAdapter` + `mainListAdapter` + `footerAdapter`）で平坦化されたリストとして描画するための土台を定義する。`DiffUtil` による差分検出、`ComposeView` 用 ViewHolder の `setViewCompositionStrategy`、`SettingsRootStore` の `StateFlow` 駆動も本 capability に含まれる。スタイル切替（クラシック/モダン）や Section H/F の描画詳細は `settings-view-android-style`、Theme/CellStyle 変換は `settings-view-android-theme-bridge`、Compose ラッパ・DSL は `settings-view-android-compose` に分離されている。本 UI 層は Android View（XML レイアウト）、Jetpack Compose、および MAUI バインディングから利用されることを前提とする。

## Requirements
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

### Requirement: RecyclerView と Adapter 構成

UI は `RecyclerView` + `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` で構築されなければならない (SHALL)。`mainListAdapter` は `ListAdapter<CellListItem, RecyclerView.ViewHolder>` であり、Section H/F + Cell を単一リストで平坦化して扱わなければならない (MUST)。`headerAdapter` / `footerAdapter` は `RootHeaderFooterAdapter`（後述）として実装されなければならない (MUST)。`CellListItem` は `sealed interface` であり、Section ヘッダ・Cell 行・Section フッタの 3 サブタイプ（`SectionHeader` / `CellRow` / `SectionFooter`）を持たなければならない (MUST)。サブタイプ名 `CellRow` は、Core 側の `Cell`（インターフェース）型との衝突を避けるため意図的に区別する。

#### Scenario: CellListItem の sealed 階層

- **GIVEN** `ks-settingsview-ui` モジュール
- **WHEN** `CellListItem` を参照する
- **THEN** `sealed interface` であり、`SectionHeader(sectionId, accessory: SectionAccessory)`、`CellRow(sectionId, cell: Cell)`、`SectionFooter(sectionId, accessory: SectionAccessory)` の 3 サブタイプを持つ

#### Scenario: 平坦化されたリスト（mainListAdapter）

- **GIVEN** Section 数 2、各 Section に 3 Cell ずつ、各 Section に header（`Text` 形式）・footer（`Text` 形式）あり
- **WHEN** `mainListAdapter` に渡されるリストを観察する
- **THEN** リスト長は 2 \* (1 SectionHeader + 3 Cells + 1 SectionFooter) = 10 である

#### Scenario: ConcatAdapter の構成

- **GIVEN** Root H/F あり、Section 1 つ、Cell 2 つ
- **WHEN** `RecyclerView.adapter` を参照する
- **THEN** `ConcatAdapter` であり、`adapters` プロパティは `[headerAdapter, mainListAdapter, footerAdapter]` の順で 3 つを保持する

### Requirement: DiffUtil 差分検出

`DiffUtil.ItemCallback<CellListItem>` を実装し、`areItemsTheSame` は ID 比較でなければならない (MUST)。`areContentsTheSame` は **同一 id（`areItemsTheSame` が true）であれば常に `true` を返さなければならない** (MUST)。すなわち `areContentsTheSame` は Cell の内容（`data class equals` の全フィールド比較）を判定に用いてはならない (MUST NOT)（「表示状態同期の三層分離」: 構造同期は id 同一性のみ）。`SectionAccessory.View(KsAnyView)` の扱い（差分検出非参加）は従来どおりとする。

`getItemId` は Cell / Section の **id に基づく安定 ID** を返さなければならない (MUST)。内容依存の `hashCode`（Cell の全フィールドから算出される値）を `getItemId` に用いてはならない (MUST NOT)。`RootHeaderFooterAdapter` の予約値（`1L` / `2L`）と衝突しない値域を維持する。

`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、追加・削除・移動・差し替え（id 変化）の構造操作で内部 `List<CellListItem>` を変更し `mainListAdapter.submitList(newList)` を呼ぶ (MUST)。一方、`replaceCell`（同一 id の内容更新）は、セルの再生成を伴わない **ViewHolder の部分更新**（`notifyItemChanged(position)` 相当、または該当 ViewHolder への直接反映）で処理しなければならず (MUST)、`submitList` による行差し替え（フルリバインド）を引き起こしてはならない (MUST NOT)。`DiffUtil` のバックグラウンド差分計算は追加・削除・移動のアニメーションにのみ用いる。

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.InsertCell(sectionId = sid, index = 0, cell = newCell))`
- **WHEN** `submitList` 後の DiffUtil 差分計算を観察する
- **THEN** 内部リストに 1 件だけ `CellListItem.CellRow` が挿入され、対応する Cell 行のみ挿入アニメーションが発生する

#### Scenario: Cell 削除時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.RemoveCell(cellId = cid))`
- **WHEN** `submitList` 後の差分計算を観察する
- **THEN** 該当 `CellListItem.CellRow` が削除され、その行のみ削除アニメーションが発生する

#### Scenario: 内容変化は areContentsTheSame で再描画されない

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）だけが異なる新旧 `CellListItem.CellRow`
- **WHEN** `DiffUtil` が `areItemsTheSame`（true）と `areContentsTheSame` を評価する
- **THEN** `areContentsTheSame` は（同一 id のため）`true` を返し、当該行のフルリバインド（`onBindViewHolder` による行全体の再生成・再 bind）は発生しない

#### Scenario: getItemId は内容に依存しない

- **GIVEN** 同一 id だが内容プロパティが異なる 2 つの Cell（順に submit される）
- **WHEN** `getItemId(position)` を評価する
- **THEN** 同一 id の Cell に対しては内容が変化しても同一の itemId を返す（内容依存の hashCode を用いない）。`RootHeaderFooterAdapter` の予約値 `1L` / `2L` とは衝突しない

#### Scenario: replaceCell は ViewHolder の部分更新で反映

- **GIVEN** `view.applyDiff(SettingsRootDiff.ReplaceCell(cellId = cid, newCell = updated))`（同一 id の内容更新）
- **WHEN** 適用後の描画を観察する
- **THEN** 該当 position の同一 ViewHolder が部分更新（再生成を伴わない bind 反映）され、内容が更新される。行の差し替えアニメーションやちらつきは発生しない

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

### Requirement: Cell レジストリ

`KsCellRegistry` は `Cell` 型から `ViewHolder` ファクトリと `viewType` Int への解決を担う中央レジストリでなければならない (SHALL)。`KsCellRegistry.register(...)` で具象 Cell 型を登録できなければならない (MUST)。`KsCellRegistry` は外部モジュール（Sample アプリや利用側アプリ）から参照可能な可視性（Kotlin の `public`）を持たなければならない (MUST)。`register` / `viewTypeOf` / `isRegistered` / `strictMode` および `CELL_VIEW_TYPE_MIN` 等の利用側に必要な API も `public` でなければならない (MUST)。

#### Scenario: Cell 型の登録と解決

- **GIVEN** `KsCellRegistry` が初期化済み
- **WHEN** `registry.register(MyCell::class, viewType = 1) { parent -> MyCellViewHolder(...) }` を呼ぶ
- **THEN** ListAdapter の `getItemViewType` で `MyCell` インスタンスは 1 を返し、`onCreateViewHolder` で `MyCellViewHolder` が生成される

#### Scenario: 未登録 Cell の扱い

- **GIVEN** `KsCellRegistry` に未登録の Cell が submit される
- **WHEN** ListAdapter が描画を試みる
- **THEN** デバッグビルドでは `IllegalStateException` をスロー、リリースビルドでは空のプレースホルダ ViewHolder を返してアプリクラッシュを防ぐ

#### Scenario: 外部モジュールからの利用

- **GIVEN** Sample アプリ（別 Gradle モジュール）が `ks-settingsview-ui` を依存する
- **WHEN** Sample アプリの `Application#onCreate` から `KsCellRegistry.register(SampleLabelCell::class, viewType = 100) { parent -> SampleLabelCellViewHolder(parent) }` を呼ぶ
- **THEN** コンパイルが通り、Sample アプリ側で独自定義した `Cell` 実装を登録できる

### Requirement: CellViewHolder 抽象

`CellViewHolder<T : Cell>` は `RecyclerView.ViewHolder` を継承する抽象クラスでなければならない (SHALL)。`abstract fun bind(cell: T, theme: Theme)` および `open fun reset()` を持たなければならない (MUST)。`CellViewHolder` は外部モジュール（Sample アプリや利用側アプリ）から派生可能な可視性（Kotlin の `public`）を持たなければならない (MUST)。

`CellViewHolder` は同一 id の Cell の内容更新を、セルを再生成せずに反映できなければならない (MUST)。具体的には、内容変化時に同一 ViewHolder に対して最新 Cell を反映する**部分更新**が可能であること（`notifyItemChanged` による再 bind、または ViewHolder が保持する View への直接反映）。チェック系 Cell（Switch / Checkbox / Radio / SimpleCheck）の ViewHolder は、ユーザー操作時に **View 自身を直接トグル（TwoWay）** し `onValueChanged` / `onSelected` でモデルへ書き戻さなければならない (MUST)。この TwoWay の内容更新は `submitList` / `DiffUtil` の再構築を経由してはならない (MUST NOT)。RadioCell のグループ連動（同一 `groupId` の他セルの選択解除）は、該当セル（旧選択・新選択）の部分更新で反映しなければならず (MUST)、グループ全体の再生成（`ReplaceCell`）を用いてはならない (MUST NOT)。

#### Scenario: bind の呼び出し

- **GIVEN** `CellViewHolder` 派生クラス
- **WHEN** ListAdapter が `onBindViewHolder` する
- **THEN** `bind(cell, theme)` が呼ばれ、Cell の内容と Theme が View に反映される

#### Scenario: reset によるクリア

- **GIVEN** `CellViewHolder` が一度 bind された後 RecyclerView から detach
- **WHEN** `onViewRecycled` が呼ばれる
- **THEN** ViewHolder 内の画像・テキスト参照、保持していた `Job`/`Disposable` がクリアされる

#### Scenario: チェック系の TwoWay トグル

- **GIVEN** Checkbox / Switch / SimpleCheck の ViewHolder が表示されている
- **WHEN** ユーザーがセルをタップする
- **THEN** ViewHolder が自身の View（CheckBox / Switch / KsSimpleCheckView）を直接トグルし、`onValueChanged(newValue)` を発火する。`submitList` / `DiffUtil` の再構築を経由せず、行全体の再描画（ちらつき）は発生しない

#### Scenario: RadioCell のグループ連動

- **GIVEN** 同一 `groupId` の RadioCell 群が表示され、ある値が選択されている
- **WHEN** ユーザーが別の RadioCell をタップする
- **THEN** タップされたセルが選択状態（チェック表示）になり `onSelected(value)` を発火し、旧選択セルの選択解除は該当セルの部分更新で反映される。グループ全体の再生成は発生しない

### Requirement: ComposeView ライフサイクル管理

ViewHolder 内で `ComposeView` を利用する場合、`setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)` を強制する基盤クラスを提供しなければならない (MUST)。

#### Scenario: ComposeView 用 ViewHolder

- **GIVEN** `ComposeCellViewHolder` 抽象クラス
- **WHEN** インスタンス化時に内部 `ComposeView` を構築する
- **THEN** `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` が自動で適用される

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

### Requirement: visible projection の flatten 規約

`KsSettingsView` の `flatten` 経路（`SettingsRoot.sections` を `CellListItem` 平坦リストへ展開する処理）は、`Section.isVisible = false` の Section、および `VisibilityAware.isVisible = false` の Cell を平坦リストから除外しなければならない (MUST)。

具体的には：

- `Section.isVisible = false` の Section は、header / footer / 全 cells を `CellListItem` 平坦リストから除外しなければならない (MUST)。
- visible な Section 内の Cell について、`(cell as? VisibilityAware)?.isVisible == false` の Cell を `CellListItem.CellRow` から除外しなければならない (MUST)。
- `VisibilityAware` プロトコルに準拠していない Cell は、フィルタの判定で常に visible として扱わなければならない (MUST)。

一方、`internalRoot`（model）は hidden 含むフル状態として保持しなければならない (MUST)。`flatten` の結果（visible projection）と `internalRoot`（model）は明確に役割を分離して管理しなければならない (MUST)。

#### Scenario: hidden Section は flatten 結果から除外される

- **GIVEN** `internalRoot.sections` に `Section(id: "s1", isVisible: false, ...)` を含む
- **WHEN** `flatten(internalRoot.sections)` を呼ぶ
- **THEN** 結果の `List<CellListItem>` には s1 由来の `SectionHeader` / `CellRow` / `SectionFooter` がいずれも含まれない

#### Scenario: hidden Cell は flatten 結果から除外される

- **GIVEN** visible な Section の `cells` に `VisibilityAware.isVisible = false` の Cell を含む
- **WHEN** `flatten` を呼ぶ
- **THEN** 結果の `List<CellListItem>` から当該 Cell の `CellListItem.CellRow` が除外される

#### Scenario: VisibilityAware 非準拠 Cell は常に flatten 結果に含まれる

- **GIVEN** `VisibilityAware` に準拠しない外部 Cell が `cells` に含まれる
- **WHEN** `flatten` を呼ぶ
- **THEN** 当該 Cell の `CellListItem.CellRow` は除外されず、常に flatten 結果に含まれる

### Requirement: 部分 Diff の index 規約と hidden 対象の no-op（Android）

`KsSettingsView.applyDiff(_:)` は、`SettingsRootDiff` の部分 Diff ケース（`InsertSection` / `RemoveSection` / `MoveSection` / `ReplaceSection` / `InsertCell` / `RemoveCell` / `ReplaceCell` / `MoveCell` / `UpdateAccessory`）について、以下の規約に従わなければならない (MUST)。

**index 引数の解釈:**

部分 Diff の `index` / `at` / `to` 引数は、すべて **model 配列基準（hidden 含む）** で解釈しなければならない (MUST)。

**hidden 対象の挙動:**

- 対象 Section / Cell が hidden の場合、`internalRoot` の更新は実行しなければならない (MUST)。
- `flatten` 再計算経路によって visible projection は自動的に更新されるため、hidden 対象の Diff は `flatten` 結果に変化を生じない（自然な no-op となる）。
- `notifyItemChanged` 系の部分更新を hidden 対象に対して呼び出した場合でも、対応する ViewHolder が存在しないため自然に no-op となる。これは正常な動作として扱わなければならない (MUST)。

#### Scenario: hidden Cell への RemoveCell は flatten 結果に影響しない

- **GIVEN** `Section` に hidden Cell を含み、その Cell に対する `RemoveCell(cellId)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** `internalRoot` から当該 Cell が削除される一方、`flatten` 結果には元から含まれていなかったため `submitList` の前後で visible projection は変化しない

#### Scenario: hidden Section への UpdateAccessory は model のみ更新

- **GIVEN** hidden Section に対する `UpdateAccessory` Diff が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** `internalRoot` 上の当該 Section の `header` / `footer` は更新される。`flatten` 結果には元から含まれていないため visible projection は変化しない。後で当該 Section が `isVisible = true` に切り替わると、更新済みの accessory が描画される

### Requirement: ReplaceCell / ReplaceSection の可視性切替防御（Android）

DSL / アプリ層は、`SettingsRootDiff.ReplaceCell` / `SettingsRootDiff.ReplaceSection` で可視性（`isVisible`）だけを変える操作を行ってはならない (MUST NOT)。可視性変更は `SettingsRootDiff.Full(newRoot)` 経由で発行されなければならない (MUST)。

UI 層 (`KsSettingsView`) は、受け取った `ReplaceCell` で旧 Cell と新 Cell の `isVisible` が異なることを **`internalRoot` から取得した旧値で** 検出した場合、Full 経路（`setRootDirect(internalRoot, internalTheme)` 相当）にフォールバックしなければならない (MUST)。検出は visible projection 上の存在チェックよりも先に行わなければならず (MUST)、旧 Cell が hidden であっても model 上から取得した旧値で判定できなければならない (MUST)。

UI 層は、受け取った `ReplaceSection` を常に Full 経路（`setRootDirect(internalRoot, internalTheme)` 相当）で処理しなければならない (MUST)。`ReplaceSection` は型上 Section 全体置換であり、内部の任意の変化を内包し得るため、内部 cell の細粒度差分抽出を試みてはならない (MUST NOT)。

#### Scenario: ReplaceCell で visibility 切替が検出される（Android）

- **GIVEN** `internalRoot` 上の Cell `X` が `isVisible = true` で、新 Cell `X'`（同一 id、`isVisible = false`）を伴う `ReplaceCell(cellId: X, newCell: X')` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は可視性切替を検出し、Full 経路にフォールバックする。`internalRoot` 更新後に `setRootDirect` 相当が呼ばれ、visible projection が再構築される

#### Scenario: ReplaceSection は常に Full 経路で処理される（Android）

- **GIVEN** `ReplaceSection(sectionId:, newSection:)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は内部の cells / accessory / visibility の細粒度差分を抽出せず、Full 経路にフォールバックする
