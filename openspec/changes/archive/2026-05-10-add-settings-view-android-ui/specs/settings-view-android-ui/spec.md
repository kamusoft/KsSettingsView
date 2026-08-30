## ADDED Requirements

### Requirement: KsSettingsView の公開 API

`KsSettingsView` は `FrameLayout` を継承し、`var root: SettingsRoot` プロパティの設定で内部 `RecyclerView` の `ConcatAdapter`（`headerAdapter`、`mainListAdapter`、`footerAdapter` の 3 段構成）を更新しなければならない (SHALL)。`mainListAdapter` には Section H/F + Cell の平坦リストを `submitList` で渡し、`headerAdapter` / `footerAdapter` には `SettingsRoot.header` / `footer` を渡す (MUST)。本 View は通常の Android XML レイアウトおよび Compose `AndroidView` 経由、MAUI バインディングから利用される (MUST)。

#### Scenario: ルートの設定で表示が更新

- **GIVEN** 既存の `KsSettingsView` が画面表示されている
- **WHEN** `view.root = newRoot` を代入する
- **THEN** `mainListAdapter.submitList(...)` が呼ばれて Section H/F + Cell の平坦リストが反映され、`headerAdapter` / `footerAdapter` の `view` プロパティが `newRoot.header` / `footer` で更新される。`RecyclerView.adapter.itemCount` は `mainListAdapter` の itemCount + `headerAdapter` の 0/1 + `footerAdapter` の 0/1 の合計と一致する

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsView(context)` を初期化した直後
- **WHEN** Activity に attach される
- **THEN** 内部 `RecyclerView` および `ListAdapter` が準備済み、空 `SettingsRoot()` 相当のリストが提示されエラーなく描画される

### Requirement: RecyclerView と Adapter 構成

UI は `RecyclerView` + `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` で構築されなければならない (SHALL)。`mainListAdapter` は `ListAdapter<CellListItem, RecyclerView.ViewHolder>` であり、Section H/F + Cell を単一リストで平坦化して扱わなければならない (MUST)。`headerAdapter` / `footerAdapter` は `RootHeaderFooterAdapter`（後述）として実装されなければならない (MUST)。`CellListItem` は `sealed interface` であり、Section ヘッダ・Cell 行・Section フッタの 3 サブタイプ（`SectionHeader` / `CellRow` / `SectionFooter`）を持たなければならない (MUST)。サブタイプ名 `CellRow` は、Core 側の `Cell`（`sealed interface`）型との衝突を避けるため意図的に区別する。

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

`DiffUtil.ItemCallback<CellListItem>` を実装し、`areItemsTheSame` は ID 比較、`areContentsTheSame` は data class equals でなければならない (MUST)。`SectionAccessory.View(KsAnyView)` の `KsAnyView` 中身は `areContentsTheSame` の判定対象から除外されなければならない (MUST)（`KsAnyView` は差分検出に参加せず、`View` ケース同士はケース一致のみで等価とみなす）。

#### Scenario: 同一内容の submit は差分なし

- **GIVEN** 同一フィールドの SettingsRoot を 2 回連続で代入
- **WHEN** ListAdapter の差分計算を観察する
- **THEN** `notifyItemChanged` 等の通知が発生しない（DiffUtil が等価判定）

#### Scenario: Cell 内容変更時の partial bind

- **GIVEN** Cell の 1 フィールドのみ変更した SettingsRoot
- **WHEN** submit する
- **THEN** その Cell 行に対し `notifyItemChanged(position)` のみ発生、他の Cell は再 bind されない

### Requirement: スタイル切替（クラシック/モダン）

`KsSettingsView` は `var style: KsSettingsViewStyle` プロパティを持たなければならない (SHALL)。`KsSettingsViewStyle` は `Classic`（旧 AiForms 互換のフラットな見た目）と `Modern`（最新 OS 設定画面風の角丸グルーピング）の 2 ケースを持つ enum でなければならない (MUST)。`style` の変更時は内部 `RecyclerView` の `ItemDecoration` を入れ替えなければならない (MUST)。

#### Scenario: Classic スタイルの ItemDecoration

- **GIVEN** `KsSettingsView` を `style = Classic` で初期化
- **WHEN** 内部 `RecyclerView.itemDecorationCount` および各 ItemDecoration のクラスを取得する
- **THEN** `ClassicSectionDecoration`（フラットな区切り線のみ）が登録されており、`ModernSectionDecoration` は登録されていない

#### Scenario: Modern スタイルの ItemDecoration

- **GIVEN** `KsSettingsView` を `style = Modern` で初期化
- **WHEN** 内部 `RecyclerView` の ItemDecoration を取得する
- **THEN** `ModernSectionDecoration`（Section 単位の角丸背景・外側マージン描画）が登録されている

#### Scenario: 動的なスタイル切替

- **GIVEN** `KsSettingsView` が `style = Classic` で表示中
- **WHEN** `view.style = KsSettingsViewStyle.Modern` を代入する
- **THEN** `RecyclerView` の既存 ItemDecoration が removeItemDecoration で取り除かれ、`ModernSectionDecoration` が addItemDecoration で追加される。`invalidateItemDecorations` が呼ばれて再描画される

#### Scenario: Compose ラッパでのスタイル指定

- **GIVEN** Compose で `KsSettingsView(root = state, style = KsSettingsViewStyle.Modern)` を記述
- **WHEN** 初回 Composition される
- **THEN** `AndroidView.factory` で生成された `KsSettingsView` の `style` が `Modern` で初期化される

### Requirement: Section H/F（SectionAccessory）の描画

`KsSettingsView` は `SectionAccessory.Text(String)` 形式のヘッダ／フッタを TextView ベースの ViewHolder で描画しなければならない (SHALL)。`SectionAccessory.View(KsAnyView)` 形式は、`KsAnyView.Compose` backing の場合は `ComposeView` を内包する ViewHolder で `setContent { compose() }` により描画し、`KsAnyView.AndroidView` backing の場合は `factory(context)` で生成した View を `addView` する ViewHolder で描画しなければならない (MUST)。`ComposeView` を含む ViewHolder は `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を強制した基盤クラスを使わなければならない (MUST)。

#### Scenario: Text 形式ヘッダの描画

- **GIVEN** `Section(header = SectionAccessory.Text("一般"), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の TextView に "一般" が描画される

#### Scenario: View 形式ヘッダ（Compose backing）の描画

- **GIVEN** `Section(header = SectionAccessory.View(KsAnyView.Compose { ProfileCard() }), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の `ComposeView` に対し `setContent { ProfileCard() }` が呼ばれ、Compose 描画が行われる

#### Scenario: View 形式ヘッダ（Android View backing）の描画

- **GIVEN** `Section(header = SectionAccessory.View(KsAnyView.AndroidView { ctx -> MyCustomView(ctx) }), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の container に対し `factory(context)` で生成された `MyCustomView` インスタンスが `addView` され、可視描画される

#### Scenario: View 形式ヘッダの中身更新（差分検出非対応）

- **GIVEN** `view.root` に `SectionAccessory.View(KsAnyView.Compose { Counter(value = 1) })` を含む root を代入
- **WHEN** `SectionAccessory.View(KsAnyView.Compose { Counter(value = 2) })` を含む root に置き換える
- **THEN** `KsAnyView` は差分検出に参加しないため ViewHolder の生成・破棄は走らないが、`ComposeView.setContent` の再呼び出しによって `Counter(value = 2)` の中身が再描画される

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsView` は `SettingsRoot.header` / `footer`（`RootAccessory?`）を `ConcatAdapter` の先頭 / 末尾に位置する `RootHeaderFooterAdapter` で描画しなければならない (SHALL)。`RootHeaderFooterAdapter` は `view: RootAccessory?` プロパティを持ち、`null` のとき `getItemCount()` は 0、非 `null` のとき 1 を返さなければならない (MUST)。`view` プロパティの setter は変化前後の `null` / 非 `null` 状態に応じて `notifyItemInserted(0)` / `notifyItemRemoved(0)` / `notifyItemChanged(0)` を発行しなければならない (MUST)。`headerAdapter.getItemId(0)` は `1L`、`footerAdapter.getItemId(0)` は `2L` を予約値として返し、`mainListAdapter` 側の `getItemId` はこれと衝突しない値域を使わなければならない (MUST)。

`RootHeaderFooterAdapter` の ViewHolder は `RootAccessory.Text(String)` を TextView 描画、`RootAccessory.View(KsAnyView)` を `ComposeView.setContent`（Compose backing）または `addView`（Android View backing）で描画しなければならない (MUST)。

#### Scenario: Root Header（Text）の描画

- **GIVEN** `SettingsRoot(header = RootAccessory.Text("プロフィール"), ...)`
- **WHEN** `view.root` に代入する
- **THEN** RecyclerView 先頭に "プロフィール" を表示する 1 つの ViewHolder が描画される（`headerAdapter.itemCount = 1`）

#### Scenario: Root Footer（View、Compose backing）の描画

- **GIVEN** `SettingsRoot(footer = RootAccessory.View(KsAnyView.Compose { Text("v1.0.0") }), ...)`
- **WHEN** `view.root` に代入する
- **THEN** RecyclerView 末尾に Compose で `Text("v1.0.0")` が描画される ViewHolder が表示される（`footerAdapter.itemCount = 1`）

#### Scenario: Root H/F が null の場合

- **GIVEN** `SettingsRoot(header = null, footer = null, ...)`
- **WHEN** `view.root` に代入する
- **THEN** `headerAdapter.itemCount = 0` および `footerAdapter.itemCount = 0` となり、RecyclerView は `mainListAdapter` の Section H/F + Cell のみを描画する

#### Scenario: Root Header の追加・削除通知

- **GIVEN** `view.root.header = null` の状態
- **WHEN** `view.root.header = RootAccessory.Text("新規")` に変更する
- **THEN** `headerAdapter.notifyItemInserted(0)` が発行され、Header 1 行が挿入アニメーションで追加される

#### Scenario: Root Header の中身更新（差分検出非対応）

- **GIVEN** `view.root.header = RootAccessory.View(KsAnyView.Compose { ... })` で描画中
- **WHEN** 同じスロットに別の `KsAnyView` を持つ root を代入する
- **THEN** ViewHolder の生成・破棄は走らないが、`notifyItemChanged(0)` 経由で `ComposeView.setContent` が再呼び出され、新しい中身が描画される

#### Scenario: ID 衝突回避

- **GIVEN** `headerAdapter` / `footerAdapter` / `mainListAdapter` がすべて `setHasStableIds(true)` の場合
- **WHEN** ConcatAdapter 内の各 adapter の `getItemId` を確認する
- **THEN** `headerAdapter.getItemId(0) = 1L`、`footerAdapter.getItemId(0) = 2L`、`mainListAdapter` の各 ID は 1L / 2L と衝突しない値域（例: `100L` 以上、または Cell の Hashable から派生する Long）を返す

### Requirement: Cell レジストリ

`KsCellRegistry` は `Cell` 型から `ViewHolder` ファクトリと `viewType` Int への解決を担う中央レジストリでなければならない (SHALL)。`KsCellRegistry.register(...)` で具象 Cell 型を登録できなければならない (MUST)。

#### Scenario: Cell 型の登録と解決

- **GIVEN** `KsCellRegistry` が初期化済み
- **WHEN** `registry.register(MyCell::class, viewType = 1) { parent -> MyCellViewHolder(...) }` を呼ぶ
- **THEN** ListAdapter の `getItemViewType` で `MyCell` インスタンスは 1 を返し、`onCreateViewHolder` で `MyCellViewHolder` が生成される

#### Scenario: 未登録 Cell の扱い

- **GIVEN** `KsCellRegistry` に未登録の Cell が submit される
- **WHEN** ListAdapter が描画を試みる
- **THEN** デバッグビルドでは `IllegalStateException` をスロー、リリースビルドでは空のプレースホルダ ViewHolder を返してアプリクラッシュを防ぐ

### Requirement: CellViewHolder 抽象

`CellViewHolder<T : Cell>` は `RecyclerView.ViewHolder` を継承する抽象クラスでなければならない (SHALL)。`abstract fun bind(cell: T, theme: Theme)` および `open fun reset()` を持たなければならない (MUST)。

#### Scenario: bind の呼び出し

- **GIVEN** `CellViewHolder` 派生クラス
- **WHEN** ListAdapter が `onBindViewHolder` する
- **THEN** `bind(cell, theme)` が型安全に呼ばれる

#### Scenario: reset での参照解放

- **GIVEN** `CellViewHolder` が一度 bind された後 RecyclerView から detach
- **WHEN** `onViewRecycled` で `reset()` が呼ばれる
- **THEN** ViewHolder 内の画像・テキスト参照、保持していた `Job`/`Disposable` がクリアされる

### Requirement: Theme / CellStyle の Android 変換

`Theme` および `CellStyle` の論理スタイルを `@ColorInt`（Android `Color` Int 表現）および `Typeface` に変換するユーティリティが提供されなければならない (SHALL)。

#### Scenario: KsColor から ColorInt

- **GIVEN** `KsColor(red = 1.0, green = 0.5, blue = 0.0, alpha = 1.0)`
- **WHEN** `KsColor.toColorInt()` 拡張関数を呼ぶ
- **THEN** ARGB Int として `0xFFFF8000` が返る

#### Scenario: 実効スタイルの合成

- **GIVEN** Cell の `CellStyle.titleColor = null`、`Theme` のデフォルト titleColor が指定されている
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `Theme` の titleColor が使われる

### Requirement: Compose ラッパ KsSettingsView

`@Composable fun KsSettingsView(root: SettingsRoot, modifier: Modifier, style: KsSettingsViewStyle = KsSettingsViewStyle.Classic, onChange: (SettingsRoot) -> Unit)` を提供し、Compose から利用できなければならない (SHALL)。内部は `AndroidView` で `KsSettingsView` (FrameLayout) を埋め込み、`style` 引数を内部 View へ反映しなければならない (MUST)。

#### Scenario: Compose からの利用

- **GIVEN** Compose 関数内で `KsSettingsView(root = state, onChange = { state = it })` と記述
- **WHEN** 初回 Composition される
- **THEN** `AndroidView.factory` で `KsSettingsView` が作られ、`update` ブロックで `view.root = state` が反映される

#### Scenario: 状態変更時の再 Composition

- **GIVEN** Compose 関数で `state` を変更
- **WHEN** Recomposition が発生
- **THEN** `AndroidView.update` ブロックが再実行され、`view.root = newState` が反映される

### Requirement: Compose DSL

`settingsRoot { ... }` ビルダ関数で `SettingsRoot` を宣言的に構築できる DSL を提供しなければならない (SHALL)。

#### Scenario: DSL から SettingsRoot 構築

- **GIVEN** Kotlin コード内で
  ```kotlin
  val root = settingsRoot(theme = Theme()) {
      section(header = "一般") { /* pocLabelCell(...) */ }
  }
  ```
  と記述
- **WHEN** `root` を評価する
- **THEN** `SettingsRoot.sections` に 1 つの `Section` が含まれ、その `cells` に DSL で記述された Cell が並ぶ

### Requirement: ComposeView ライフサイクル管理

ViewHolder 内で `ComposeView` を利用する場合、`setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)` を強制する基盤クラスを提供しなければならない (MUST)。

#### Scenario: ComposeView 用 ViewHolder

- **GIVEN** `ComposeCellViewHolder` 抽象クラス
- **WHEN** インスタンス化時に内部 `ComposeView` を構築する
- **THEN** `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` が自動で適用される

### Requirement: メモリリーク防止

`KsSettingsView`（FrameLayout）は `onDetachedFromWindow` で内部 RecyclerView の adapter を `null` にし、ListAdapter の参照を解放しなければならない (MUST)。

#### Scenario: View が detach される

- **GIVEN** `KsSettingsView` が Activity に attach されたのち remove される
- **WHEN** `onDetachedFromWindow` が呼ばれる
- **THEN** `recyclerView.adapter == null` となり、ListAdapter への参照が解放される

### Requirement: PoC Cell の存在

`ks-settingsview-ui` モジュールは PoC 用の最小 Cell（`PocLabelCell`：id・title のみ）を `internal` として持ち、ユニットテストおよびサンプル動作確認で使用しなければならない (SHALL)。具象 Cell が追加された段階で削除されなければならない (MUST)。

#### Scenario: PocLabelCell の表示

- **GIVEN** `SettingsRoot` 内に `PocLabelCell(title = "Hello")` を含む `Section` が 1 つ
- **WHEN** `KsSettingsView.root` に代入する
- **THEN** RecyclerView に少なくとも 1 つの行が描画され、ViewHolder の TextView が "Hello" を表示する
