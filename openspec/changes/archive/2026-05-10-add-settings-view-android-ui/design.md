## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**: §7（Android Native 実装の特徴）、§9（NativeCollectionView から引き継ぐパターン）、§8（メモリリーク対策）
- 原典コード：
  - [`../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.Android.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.Android.cs) — RecyclerView ベースの旧実装（手動 Adapter / NotifyDataSetChanged 系）
  - [`../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs) — **本家 SettingsView の平坦リスト実装。`List<RowInfo>` で Section/Cell/Header/Footer を一列に並べ、各 RowInfo に Section 参照を持たせる方式**（Decision 1 の根拠）
  - [`../AiForms.Maui.SettingsView/SettingsView/Native/Android/SVItemdecoration.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/SVItemdecoration.cs) — **平坦リスト + ViewHolder の Section 参照で区切り線を描画する実例**（モダン UI の角丸グルーピング描画でも同じ前後参照パターンが流用可能）
  - [`../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/`](file://../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/) — `ListAdapter + DiffUtil + ConcatAdapter` のパターン実装例（用途が異なるため本変更提案では ConcatAdapter は採用しない）
  - [`../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/ContentsAdapter.cs`](file://../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/ContentsAdapter.cs) — ListAdapter + DiffUtil の具体実装（OnBindViewHolder で ResetCell→BindCell の明示ライフサイクル管理あり）
  - [`../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/AiRecyclerView.cs`](file://../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/AiRecyclerView.cs) — `NestedScrollingEnabled = false` 等のパフォーマンス工夫

**重要**: NativeCollectionView は ConcatAdapter を採用しているが、本変更提案は **Section H/F + Cell の平坦リスト部分**には単一 ListAdapter + 平坦リスト方式を選択している（Decision 1 参照）。一方、**リスト全体の Root H/F（`SettingsRoot.header` / `footer`）** には ConcatAdapter で外側にラップする方式を採用する（Decision 5d 参照、`AiForms.Maui.NativeCollectionView` の HeaderAdapter / FooterAdapter パターンを踏襲）。両者は別軸の判断であり矛盾しない。

## Context

Android の設定画面 UI を `RecyclerView + ListAdapter + DiffUtil` で構築する。`AiForms.Maui.NativeCollectionView/Platforms/Android/` の知見（ListAdapter + DiffUtil、ConcatAdapter、`NestedScrollingEnabled = false` などのパフォーマンス工夫）を活かす。Compose 利用者にも自然に届けるため、`AndroidView` ラッパと DSL を Phase 1 で同梱する。本変更提案は Cell 1 種（PoC）が表示できるところまでを範囲とし、各種具象 Cell の追加は別変更提案で対応する。

## Goals / Non-Goals

**Goals:**
- `RecyclerView + ListAdapter + DiffUtil` の統合された UI 基盤
- `KsCellRegistry` による Cell 型の動的登録
- `CellViewHolder<T>` 型安全な ViewHolder 抽象
- Compose ラッパ + DSL（`settingsRoot { section { cell() } }`）
- ComposeView ライフサイクル管理基盤（`DisposeOnDetachedFromWindow`）
- PoC `PocLabelCell` での動作確認

**Non-Goals:**
- 具象 Cell（LabelCell、SwitchCell ...）の追加は本変更提案では行わない
- ItemTouchHelper によるドラッグ＆ドロップは本変更提案では扱わない（Phase 6 で再検討）
- カスタムセル（任意 Composable 埋め込み）は `add-cell-types-custom` で扱う
- MAUI バインディング層は `add-maui-bindings` で扱う

## Decisions

### Decision 1: Section H/F + Cell の平坦リスト部分は単一 ListAdapter

**選択**: Section ヘッダ／Cell 行／Section フッタを `sealed interface CellListItem`（サブタイプ: `SectionHeader` / `CellRow` / `SectionFooter`）で表現し、単一 `ListAdapter<CellListItem, RecyclerView.ViewHolder>` で全項目を扱う。**Section 単位の `ConcatAdapter` 分割は使わない**。各 `CellListItem` は所属 Section の ID（または Section 参照）を保持し、ItemDecoration から境界判定に使う。サブタイプ名 `CellRow` は Core 側の `Cell`（`sealed interface`）型との衝突を避けるための命名である。

> 注: 本決定は「Section H/F + Cell の平坦リスト部分」に関するものである。リスト全体の Root H/F（`SettingsRoot.header` / `footer`）は別軸の判断として ConcatAdapter で外側にラップする（Decision 5d 参照）。Root H/F の ConcatAdapter ラップは Section 単位の adapter 分割ではなく、平坦リスト adapter を中央に挟む形のため、本 Decision の「Section 単位 ConcatAdapter 不採用」とは矛盾しない。

**理由**:
- ヘッダ／フッタの差分も DiffUtil が一貫して扱える
- ConcatAdapter は内部で sub adapter ごとの状態管理が必要で、Cell の上下移動（後続 Phase）や Section 全体の追加/削除で複雑化する
- 旧 `AiForms.Maui.NativeCollectionView` は ConcatAdapter を採用しているが、用途（任意ヘッダ View 挿入）が異なる
- **本家 `AiForms.Maui.SettingsView`（旧版 SettingsView）は `ModelProxy : List<RowInfo>` による平坦リスト方式で実装されており、本決定は実績ベース**（参考: [`../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs)）。各 `RowInfo` は `Section` 参照と `ViewType`（TextHeader/CustomHeader/TextFooter/CustomFooter/Cell 種別）を保持する。`SVItemdecoration.cs` も同 Section 参照経由で境界判定し、区切り線描画を実現している。
- モダン UI の角丸グルーピング描画（Section 単位の外側マージン・上下端の角丸）は、`ItemDecoration.getItemOffsets` / `onDraw` 内で `bindingAdapterPosition` から前後の `CellListItem` を `O(1)` で参照し、「同 sectionId の Cell が直前/直後にあるか」を比較するだけで判定できる。Section 数 5〜10、Cell 総数 100 程度の設定画面規模では、平坦リスト全体に対する DiffUtil 計算コストも実用上問題ない。

**MAUI 版での「Section 構造再現の苦労」と KsSettingsView の差**:

| MAUI 版で大変だった点 | 原因 | KsSettingsView での扱い |
|---|---|---|
| Section 追加/削除時の index 計算（`RowIndexFromParentCollection` 等） | `ObservableCollection.NotifyCollectionChanged` を手動で `NotifyItemRangeInserted/Removed` に翻訳していたため | `ListAdapter.submitList(...)` 1 発で DiffUtil が自動計算するため**構造的に発生しない** |
| ItemDecoration での区切り線描画の境界判定 | `ViewHolder.RowInfo.Section` 参照を毎ループで取得・前 Holder と比較する手書きロジック | `sealed interface CellListItem` で型安全に分岐でき、`bindingAdapterPosition` 経由で前後アイテムを参照する**同等以上にシンプルな実装**が可能 |

**代替案**:
- ConcatAdapter で Section 単位に sub adapter を分割：(1) Section 全体の追加/削除で sub-adapter 群を入れ替える必要があり内部状態管理が複雑、(2) ViewType 名前空間管理が `KsCellRegistry` の設計と二重化する、(3) ItemDecoration の境界判定が `ConcatAdapter.adapters[]` を辿る形で逆に複雑化する、(4) Cell の Section 間移動（将来要件の可能性）の差分アニメーションが Section 単位に分断される — 等の理由で本ライブラリでは採用しない。**Root H/F のための ConcatAdapter ラップ（Decision 5d）はこれと別軸で、Section 単位の分割には踏み込まない。**
- 完全 Compose / LazyColumn ベース：MAUI バインディング可能性を損なうため不採用（後続 `add-cell-types-custom` での ComposeView 内包は OK だが、外枠は Native View を維持）。

### Decision 2: ListAdapter で AsyncListDiffer を内蔵

**選択**: `ListAdapter` を採用（内部で `AsyncListDiffer` を使用）。差分計算はバックグラウンドスレッドで実行される。

**理由**:
- 数百〜数千 Cell でも UI スレッドをブロックしない
- `submitList` API がイディオマティック

**代替案**:
- 直接 `RecyclerView.Adapter + DiffUtil.calculateDiff`：手動で差分計算する必要があり煩雑。

### Decision 3: KsCellRegistry の中央集権化

**選択**: `KsCellRegistry` シングルトン（または KsSettingsView ローカル）に Cell 型と viewType・ViewHolder ファクトリのペアを登録。

**理由**:
- 後続 `add-cell-types-*` 各変更提案が独立して新 Cell を追加可能
- MAUI バインディング層も Bridge 経由で同じ registry を呼べる
- iOS 版 `KsCellRegistry` と対称

**代替案**:
- ListAdapter 内部で `when (cell)` パターンマッチ：Cell 種類が増えるたび編集が必要、独立変更提案で扱いづらい。

### Decision 4: Compose ラッパは AndroidView 経由

**選択**: `@Composable fun KsSettingsView(root: SettingsRoot, ...)` の内部は `AndroidView(factory = { KsSettingsView(it) }, update = { it.root = root })`。

**理由**:
- 既存 `KsSettingsView` (FrameLayout) を変更せず再利用できる
- MAUI バインディングも同じ FrameLayout を参照するため、Compose 利用と MAUI 利用で実装重複が起きない

**代替案**:
- 純粋 Compose 実装（`LazyColumn` ベース）：パフォーマンスは魅力だが MAUI 利用者と共通化できず保守コストが倍。Phase 6 のモダン UI で再検討。

### Decision 5: ComposeView 基盤クラス

**選択**: `abstract class ComposeCellViewHolder : CellViewHolder<...>` を提供し、内部 `ComposeView` 構築時に `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を強制する。

**理由**:
- カスタムセル（`add-cell-types-custom` で追加）が ComposeView を使う際、ライフサイクル設定漏れを防止
- 計画書でリスクとして特定済み

**代替案**:
- 各 ViewHolder で個別設定：実装漏れリスクが高い。

### Decision 5b: クラシック/モダンのスタイル切替を Phase 1 で同梱

**選択**: `KsSettingsView`（FrameLayout）に `var style: KsSettingsViewStyle` を持たせ、setter で内部 `RecyclerView` の `ItemDecoration` を `ClassicSectionDecoration` / `ModernSectionDecoration` に入れ替える。Compose ラッパもイニシャライザ引数で同 enum を受け取る。

**理由**:
- 「クラシック」と「モダン」の違いは描画基盤（RecyclerView）ではなく**見た目（Section の装飾）**であることが探索で確定した（MAUI バインディング可能性のため Native View ベースは双方共通）
- Android では iOS のような `Appearance` 標準 enum がないため、Section ごとの「角丸背景・外側マージン」を `ItemDecoration.onDraw` / `getItemOffsets` で描画する独自実装が必要
- 旧計画では「Phase 6 でモダン UI として再検討」だったが、Phase 1 から両方をサンプルで検証可能にすることで、後続 Cell 拡張（`add-cell-types-*`）の見た目検証にも両 style を活用できる
- ItemDecoration 入れ替え方式のため、Adapter / ViewHolder / Cell 実装には影響しない（責務分離）

**代替案**:
- マテリアル MDC の `MaterialCardView` を Section ごとに wrap：Cell 単位の RecyclerView 構造と相性が悪く、ヘッダ／フッタとの位置揃えが煩雑になる。
- Theme の一部に統合：見た目スタイル（角丸装飾の有無）は色・フォントとは直交する概念のため、別プロパティの方が利用者にとって直感的。
- 動的切替不可（init 引数のみ）：`removeItemDecoration` / `addItemDecoration` で切替可能なため、setter で柔軟に変更できる方が DI / テストで便利。

### Decision 5c: Section H/F の `SectionAccessory.View(KsAnyView)` ケースを本実装する

**選択**: `Section.header` / `Section.footer` の型は Core 側で `SectionAccessory?`（`Text(String)` / `View(KsAnyView)`）として再定義済み（`refactor-accessory-and-root-hf` で確定）。UI 層では `Text` を `HeaderViewHolder` / `FooterViewHolder`（テキスト表示専用）で描画し、`View(KsAnyView)` を `ComposeView` 内包の ViewHolder（`KsAnyView.Compose` backing）または Android View を `addView` する ViewHolder（`KsAnyView.AndroidView` backing）で描画する。本変更提案で両ケースとも完成形を提供する。

**理由**:
- `KsAnyView` は装飾領域専用の型消去ラッパであり、Cell 概念と独立しているため、`add-cell-types-custom` の `CustomCell` 実装と分離して扱える（Cell 概念排除の方針 → `refactor-accessory-and-root-hf` Decision 1）
- `KsAnyView.Compose` の場合、`ComposeView.setContent { compose() }` で中身を毎 bind 時に再構成できる。`KsAnyView` は差分検出に参加しないため、bind の度に setContent が呼ばれることで中身更新が吸収される
- `ComposeView` の lifecycle は `Decision 5`（`DisposeOnDetachedFromWindow`）と一貫した方針で扱う

**代替案**:
- Phase 1 では `View` を最小高さプレースホルダでフォールバックし `add-cell-types-custom` で本実装：Core 型の受け入れ口だけを残す中途半端な状態となり、ユーザ視点で機能不全。Cell 概念排除（`KsAnyView` の独立化）により本実装の責務切り出しが可能になったため不採用。
- `View(KsAnyView)` を `CustomCell` の ViewHolder 機構に流用：Cell 概念混入を残すため不採用。

### Decision 5d: Root H/F は ConcatAdapter で外側ラップする

**選択**: `SettingsRoot.header` / `footer` の Root ヘッダ／フッタは、平坦リスト ListAdapter の外側に `ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` の構成でラップして配置する。`headerAdapter` / `footerAdapter` は専用の `RootHeaderFooterAdapter`（`ItemCount = 0/1` 切り替え式、`getItemViewType` / `getItemId` は固定値）として実装する。`headerAdapter` の `getItemId` は `1L`、`footerAdapter` は `2L` を予約し、`mainListAdapter` 側はこれと衝突しない大きな値域の ID を使う。

**理由**:
- `AiForms.Maui.NativeCollectionView` の `HeaderAdapter` / `FooterAdapter` 実装と同じパターンで実績がある（`Platforms/Android/HeaderFooterAdapter.cs`）
- 既存の平坦リスト `mainListAdapter`（Section H/F + Cell）に手を入れず、責務分離が明確
- Decision 1 の「Section 単位の ConcatAdapter 分割を使わない」とは別軸の判断（こちらはリスト全体の前後に固定 1 要素を挟む単純な構造）
- 中身の更新は `notifyItemChanged(0)` による再 bind で吸収する（`KsAnyView` の差分判定なし）
- `headerAdapter.view = null` で `ItemCount = 0` に切り替わり、Root Header 自体を非表示にできる

**代替案**:
- `mainListAdapter` の `CellListItem` に `RootHeader` / `RootFooter` ケースを追加: DiffUtil 一元化されるが、Root と Section の概念が混ざる。`KsAnyView` の equals 不在により実質的な差分検出は効かないため、ConcatAdapter 化のメリット（責務分離、AiForms 互換）の方が大きい。不採用。
- 外側に `LinearLayout` で Header/Footer を挟む：RecyclerView のスクロール領域の外に出てしまい、`AiForms.Maui.NativeCollectionView` の HeaderView と挙動が異なる（一緒にスクロールしない）。不採用。

### Decision 6: DSL のスコープ型

**選択**: `SettingsRootScope` と `SectionScope` の Kotlin 標準的な receiver スコープ型を提供。`@DslMarker` で誤った入れ子を防止。

**理由**:
- Compose ライブラリの慣習に沿う
- `@DslMarker` でスコープ越えのメソッド呼び出しをコンパイル時拒否

## Risks / Trade-offs

- **リスク**: ViewHolder 再利用時に古い状態（Switch の listener、AsyncTask など）が残ると不正動作
  - **緩和策**: `CellViewHolder.reset()` を `RecyclerView.Adapter.onViewRecycled` で必ず呼ぶ。各 ViewHolder の `reset()` 実装を要件化。
- **リスク**: ComposeView の lifecycleOwner が RecyclerView と一致しないと再 Composition が止まらない
  - **緩和策**: `DisposeOnDetachedFromWindow` を強制する基盤クラスを提供（仕様化）。
- **リスク**: 平坦リスト方式だと、Section ヘッダ／フッタの DiffUtil 計算で Cell との型差別がノイズになる
  - **緩和策**: `areItemsTheSame` で sealed interface subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。
- **トレードオフ**: 旧 AiForms にあった「ドラッグ＆ドロップ並べ替え」は本変更提案では実装しない。Phase 6 で再検討。

## Open Questions

（解消済み）
- ~~ヘッダ／フッタに任意 Composable を渡せる API は Phase 1 で提供するか？~~ → **Decision 5c（更新版）で解消**。Core 側の `SectionAccessory` を `Text(String)` / `View(KsAnyView)` に再定義（`refactor-accessory-and-root-hf` で確定）し、UI 層は本変更提案で `Text` / `View` の両ケースを本実装する。`KsAnyView` は Cell 概念と独立した装飾領域専用ラッパであるため、`add-cell-types-custom` の CustomCell 実装と分離して扱える。
- ~~モダン UI は Phase 6 で再検討~~ → **Decision 5b で解消**。クラシック/モダンは描画基盤（RecyclerView）の差ではなく `ItemDecoration` の差であることが確定したため、Phase 1 から `style` プロパティで両対応する。

（残課題）
- Root H/F のスクロール固定（`StickyHeaderItemDecoration` 等の追加導入）は本変更提案では扱わず、後続改善で検討する。デフォルトはスクロール追従（`AiForms.Maui.NativeCollectionView` の `HeaderView` と同じ挙動）。
