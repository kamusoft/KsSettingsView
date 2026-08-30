## Why

`add-settings-view-core` で確立した Core モデルを描画する Android の UI 基盤を構築する必要がある。`RecyclerView` + `ListAdapter` + `DiffUtil` を採用することで、`AiForms.Maui.NativeCollectionView` の知見を踏襲しつつ高パフォーマンスかつ滑らかな差分アニメーションを実現する。本変更提案では UI 基盤（`KsSettingsView`（FrameLayout）、`ListAdapter`、`ConcatAdapter`、`CellViewHolder` 抽象、Compose ラッパ + DSL）を整備し、PoC として最小 1 種の動作確認用 Cell を表示できるところまで持っていく。

## What Changes

- 新モジュール `ks-settingsview-ui`（Android Kotlin）を追加：
  - `class KsSettingsView(ctx, attrs) : FrameLayout`：`var root: SettingsRoot` setter で内部 ListAdapter に submitList する。`var style: KsSettingsViewStyle` setter で ItemDecoration を入れ替える
  - `enum class KsSettingsViewStyle { Classic, Modern }`：`Classic`（旧 AiForms 互換のフラットな見た目）/ `Modern`（最新 OS 設定画面風の角丸グルーピング）
  - 内部 `RecyclerView` + `ListAdapter<CellListItem, RecyclerView.ViewHolder>`（Section ヘッダ・Cell 行・フッタを `sealed interface CellListItem` で平坦化。サブタイプ名は `SectionHeader` / `CellRow` / `SectionFooter`。`CellRow` は Core 側の `Cell` 型との衝突を避けるための名称）
  - `DiffUtil.ItemCallback<CellListItem>` を採用、`areItemsTheSame` は ID 比較、`areContentsTheSame` は data class equals。`SectionAccessory.View(KsAnyView)` の中身（`KsAnyView`）は差分検出に参加しない
  - 内部 Section H/F + Cell の平坦リストには「単一 ListAdapter で平坦リスト」方式を採用（Section 単位を `ConcatAdapter` で分割する案は不採用、Decision 1 参照）
  - **Root H/F のみ** `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` で外側にラップする（Decision 1 の「Section 単位の ConcatAdapter 分割」とは別軸の判断、Decision 5d 参照）。`headerAdapter` / `footerAdapter` は `ItemCount = 0/1` 切り替え式の独立 Adapter
  - Section H/F の `SectionAccessory.View(KsAnyView)` ケースは平坦リストの `CellListItem` から `ComposeView`（`@Composable` backing）または Android View（factory backing）を内包する ViewHolder で描画する
  - `abstract class CellViewHolder<T : Cell>(view: View)`：Cell ごとの描画契約。`bind(cell: T, theme: Theme)`、`reset()` を持つ
  - `KsCellRegistry`：`Cell` 型から `ViewHolder` ファクトリ・`viewType` Int への解決を行う中央レジストリ
  - `Theme` および `CellStyle` を Android `Color`（`@ColorInt`）/ `Typeface` に変換するユーティリティ
  - `ClassicSectionDecoration` / `ModernSectionDecoration`：`RecyclerView.ItemDecoration` 派生。Classic は装飾なし（区切り線のみ）、Modern は Section 単位に角丸背景・外側マージンを描画
- 新モジュール `ks-settingsview-compose`（Android Kotlin）を追加：
  - `@Composable fun KsSettingsView(root: SettingsRoot, modifier: Modifier = Modifier, style: KsSettingsViewStyle = KsSettingsViewStyle.Classic, onChange: (SettingsRoot)->Unit = {})`：内部で `AndroidView` ラッパ
  - DSL：`fun settingsRoot(theme: Theme = Theme(), block: SettingsRootScope.() -> Unit): SettingsRoot` ビルダ関数
- PoC として `PocLabelCell`（id・title のみ表示）を `ks-settingsview-ui` 内部に置き、テストとサンプル動作確認に使用する。本 Cell は本格 Cell 追加段階で削除する
- `ComposeView` を ViewHolder で利用する場合は `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を強制する基盤クラスを提供
- 単体テスト（JUnit + Robolectric）を整備：snapshot 適用後の RecyclerView itemCount、Theme 適用、メモリリーク検証、`style` 切替（`Classic` ↔ `Modern`）で ItemDecoration が入れ替わること、`SectionAccessory.Text` ヘッダ描画、`SectionAccessory.View` ヘッダの `KsAnyView` 描画（Compose / Android View backing）、`SettingsRoot.header` / `footer`（Root H/F）の Text / View 両ケース描画と `null` 時の `ItemCount=0` 動作

## Capabilities

### New Capabilities
- `settings-view-android-ui`: Android UI 基盤（KsSettingsView FrameLayout、RecyclerView、ListAdapter、Cell レジストリ、Compose ラッパ、DSL）の振る舞いを規定する

### Modified Capabilities
（なし）

## Impact

- 影響範囲：Android Native の UI 層
- 依存：`add-monorepo-foundation`、`add-settings-view-core`
- 後続変更が依存：`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`（Android バインディング部分）
- リスク：中。`ComposeView` ライフサイクル、ViewHolder 再利用時の状態リセットに注意
