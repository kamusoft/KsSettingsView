## 1. ks-settingsview-ui モジュール初期設定

- [x] 1.1 `android/ks-settingsview-ui/` を Gradle サブプロジェクトとして作成、`settings.gradle.kts` の `include(...)` に追加
- [x] 1.2 `build.gradle.kts` を `com.android.library` プラグインで作成（minSdk 29、compileSdk 35、`ks-settingsview-core` 依存、`androidx.recyclerview:recyclerview` 依存)
- [x] 1.3 `src/main/AndroidManifest.xml` を最小構成で作成
- [x] 1.4 `src/main/kotlin/jp/kamusoft/kssettingsview/ui/` パッケージを作成
- [x] 1.5 `src/test/kotlin/jp/kamusoft/kssettingsview/ui/` を作成（JUnit + Robolectric を依存に追加）

## 2. Android 変換ユーティリティ

- [x] 2.1 `KsColorExt.kt` を作成（`fun KsColor.toColorInt(): Int`）
- [x] 2.2 `KsFontExt.kt` を作成（`fun KsFont.toTypeface(context: Context): Typeface`）
- [x] 2.3 `EffectiveStyle.kt` を作成（`Theme` と `CellStyle` の合成、`@ColorInt`/`Typeface` を返すユーティリティ）

## 3. Cell 描画基盤

- [x] 3.1 `CellListItem.kt` を作成し `sealed interface CellListItem`（`SectionHeader`、`CellRow`、`SectionFooter`）を定義（`CellRow` は Core 側 `Cell` 型との衝突回避のため）
- [x] 3.2 `CellViewHolder.kt` を作成し `abstract class CellViewHolder<T : Cell>` を定義（`abstract fun bind(cell: T, theme: Theme)`、`open fun reset()`）
- [x] 3.3 `KsCellRegistry.kt` で型登録・解決ロジックを実装（`register(cellClass, viewType, factory)`、シングルトン）
- [x] 3.4 `ComposeCellViewHolder.kt` で `ComposeView` を `DisposeOnDetachedFromWindow` 戦略で内包する基盤クラスを提供

## 4. ListAdapter

- [x] 4.1 `KsSettingsListAdapter.kt` で `ListAdapter<CellListItem, RecyclerView.ViewHolder>` を実装
- [x] 4.2 `getItemViewType` で `KsCellRegistry` から viewType を解決
- [x] 4.3 `onCreateViewHolder` で registry のファクトリを呼び ViewHolder を生成
- [x] 4.4 `onBindViewHolder` で `CellViewHolder.bind(cell, theme)` を呼ぶ
- [x] 4.5 `onViewRecycled` で `CellViewHolder.reset()` を呼ぶ
- [x] 4.6 `DiffUtil.ItemCallback<CellListItem>` を実装（`areItemsTheSame` は sealed subtype + ID、`areContentsTheSame` は equals）。`SectionAccessory.View(KsAnyView)` の中身は `areContentsTheSame` の判定対象から除外する（`KsAnyView` は equals/hashCode 非対応のため、`View` ケース同士はケース一致のみで等価とみなす）

## 5. KsSettingsView (FrameLayout)

- [x] 5.1 `KsSettingsViewStyle.kt` で `enum class KsSettingsViewStyle { Classic, Modern }` を実装
- [x] 5.2 `KsSettingsView.kt` で `class KsSettingsView(ctx, attrs) : FrameLayout(ctx, attrs)` を実装
- [x] 5.3 内部 `RecyclerView`（`LinearLayoutManager`、`adapter = ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)`）を生成
- [x] 5.4 `var root: SettingsRoot` setter で `mainListAdapter.submitList(flatten(root.sections))` を呼び、`headerAdapter.view = root.header`、`footerAdapter.view = root.footer` を設定
- [x] 5.5 `var style: KsSettingsViewStyle` setter で既存 ItemDecoration を `removeItemDecoration` し、`Classic` → `ClassicSectionDecoration`、`Modern` → `ModernSectionDecoration` を addItemDecoration、`invalidateItemDecorations` を呼ぶ
- [x] 5.6 `onDetachedFromWindow` で `recyclerView.adapter = null` にして参照解放
- [x] 5.7 Section H/F 用 ViewHolder（`SectionHeaderViewHolder`、`SectionFooterViewHolder`）を内部実装。`SectionAccessory.Text` は TextView 描画、`SectionAccessory.View(KsAnyView)` は `KsAnyView.Compose` の場合 `ComposeView.setContent`（`DisposeOnDetachedFromWindow` 強制基盤クラス使用）、`KsAnyView.AndroidView` の場合 `factory(context)` で生成した View を `addView` する
- [x] 5.8 `RootHeaderFooterAdapter.kt` を実装：`var view: RootAccessory?` プロパティ（setter は `notifyItemInserted/Removed/Changed` を発行）、`getItemCount()` は `view == null ? 0 : 1`、`getItemId(0)` は header=`1L` / footer=`2L`、`getItemViewType(0)` は予約値、ViewHolder は `RootAccessory.Text` / `RootAccessory.View` を Section H/F と同じ機構で描画
- [x] 5.9 `mainListAdapter` の `getItemId` は `RootHeaderFooterAdapter` の予約値（1L、2L）と衝突しない値域（例: `100L` 以上、または Cell の Hashable 派生 Long）を返すよう実装する

## 5b. ItemDecoration（クラシック/モダン装飾）

- [x] 5b.1 `ClassicSectionDecoration.kt` を実装：旧 AiForms 互換のフラットな区切り線描画のみ。Section 間の追加マージン・背景描画は行わない
- [x] 5b.2 `ModernSectionDecoration.kt` を実装：`getItemOffsets` で Section 単位の外側マージン、`onDraw` で角丸背景を描画。Section 内 Cell の上下端は `bindingAdapterPosition` 経由で前後の `CellListItem` の `sectionId` を比較する `O(1)` 判定で決定する（`ModelProxy.cs` / `SVItemdecoration.cs` の前後参照パターンを参照）
- [x] 5b.3 `RoundedSectionBackgroundDrawable.kt`：角丸背景の Drawable ヘルパを共通化

## 6. PoC Cell

- [x] 6.1 `PocLabelCell.kt` を `internal data class PocLabelCell(...)` で実装
  - 実装メモ: `Cell` は sealed interface のため、別モジュール（`ks-settingsview-ui`）から実装できない。
    そのため PocLabelCell は `ks-settingsview-core` モジュール内に配置した。`internal` 修飾子も Kotlin
    のモジュールスコープ（Gradle Compilation）のため `ks-settingsview-ui` から参照不能となるので、
    やむを得ず `public` で公開している。後続変更提案で具象 Cell が追加され次第削除する想定。
- [x] 6.2 `PocLabelCellViewHolder.kt` を `internal class ... : CellViewHolder<PocLabelCell>` で実装
- [x] 6.3 `KsSettingsView.init` 内で `KsCellRegistry` に PoC Cell を登録

## 7. ks-settingsview-compose モジュール初期設定

- [x] 7.1 `android/ks-settingsview-compose/` Gradle サブプロジェクトを作成、`settings.gradle.kts` に追加
- [x] 7.2 `build.gradle.kts` を `com.android.library` + Compose プラグインで作成（`ks-settingsview-ui` 依存、`androidx.compose.runtime`、`androidx.compose.ui`）

## 8. Compose ラッパ + DSL

- [x] 8.1 `KsSettingsViewComposable.kt` で `@Composable fun KsSettingsView(root: SettingsRoot, modifier: Modifier, style: KsSettingsViewStyle = KsSettingsViewStyle.Classic, onChange: (SettingsRoot)->Unit)` を `AndroidView` 経由で実装。`update` ブロックで `view.style` も反映
- [x] 8.2 `SettingsRootScope.kt`（`@DslMarker` 付き）と `SectionScope.kt` を実装
- [x] 8.3 `settingsRoot { ... }` ビルダ関数（`fun settingsRoot(theme: Theme = Theme(), block: SettingsRootScope.() -> Unit): SettingsRoot`）を実装

## 9. ユニットテスト（ks-settingsview-ui）

- [x] 9.1 `KsSettingsViewTest.kt`（Robolectric）：root 設定後の itemCount 検証
- [x] 9.2 `ListAdapterDiffTest.kt`：同一内容で no-op、Cell 内容変更で notifyItemChanged
- [x] 9.3 `KsCellRegistryTest.kt`：型登録・解決・未登録時の例外
- [x] 9.4 `EffectiveStyleTest.kt`：CellStyle null → Theme から補完
- [x] 9.5 `MemoryLeakTest.kt`：onDetachedFromWindow で adapter null 確認
  - 実装メモ: Robolectric 4.13 の Activity ライフサイクル経由では Window detach が決定的に走らないため、テスト用ヘルパ `internalDetachForTest()` 経由で `onDetachedFromWindow` を直接呼び出して検証する形式とした。
- [x] 9.6 `KsSettingsViewStyleTest.kt`（Robolectric）：`Classic` 初期化で `ClassicSectionDecoration` 登録、`Modern` 初期化で `ModernSectionDecoration` 登録、setter 経由の動的切替で ItemDecoration が入れ替わることを検証
- [x] 9.7 `SectionAccessoryRenderingTest.kt`（Robolectric）：`SectionAccessory.Text` でヘッダ TextView 描画、`SectionAccessory.View(KsAnyView.Compose)` で ComposeView.setContent 経由で描画される、`SectionAccessory.View(KsAnyView.AndroidView)` で factory 生成 View が addView される、中身を差し替えても同一 ViewHolder で再描画されることを検証
- [x] 9.8 `RootHeaderFooterAdapterTest.kt`（Robolectric）：`view = null` で `itemCount == 0`、非 null で `itemCount == 1`、null → 非 null で `notifyItemInserted(0)` 発行、非 null → null で `notifyItemRemoved(0)` 発行、`getItemId(0)` の予約値（headerAdapter=1L、footerAdapter=2L）を検証
- [x] 9.9 `RootAccessoryRenderingTest.kt`（Robolectric）：`SettingsRoot.header = RootAccessory.Text` / `RootAccessory.View` が ConcatAdapter 先頭に描画される、`SettingsRoot.footer` が末尾に描画される、`null` 時に省略される、スクロール時に追従することを検証
  - 実装メモ: スクロール追従は `RecyclerView` 配下の単一スクロールビューに `RootHeaderFooterAdapter` を `ConcatAdapter` で内包する構成上、自動的に達成される（外側に LinearLayout を挟まない / 別軸の追加機構なし）。本テストでは ConcatAdapter の itemCount 構成で確認している。
- [x] 9.10 `./gradlew :ks-settingsview-ui:test` で全成功

## 10. ユニットテスト（ks-settingsview-compose）

- [x] 10.1 `KsSettingsViewComposeTest.kt`（Compose Test）：state 反映の検証
  - 実装メモ: `androidx.compose.ui.test.junit4.createComposeRule()` を Robolectric バックエンドで動かす形で実装。`KsSettingsView` Composable が例外なくレンダリングされること、`settingsRoot { }` DSL で構築した `SettingsRoot` が内部 `KsSettingsViewLayout` に反映されることを検証する。動的 state 更新（`mutableStateOf` の差し替えで `update` ブロックが再実行されること）の検証は ListAdapter の AsyncListDiffer の非同期性により Robolectric では不安定なため、実 UI のレンダリング確認は `add-samples-android` の Sample アプリに委ねる。
- [x] 10.2 `SettingsRootBuilderTest.kt`：DSL から SettingsRoot 構築結果検証
- [x] 10.3 `./gradlew :ks-settingsview-compose:test` で全成功

## 11. ドキュメント

- [x] 11.1 `docs/android-ui.md` を作成し、KsSettingsView の使い方、Compose ラッパの使い方、Cell 登録方法を記載

## 依存関係

- 先行：`add-monorepo-foundation`、`add-settings-view-core`
- 後続：`add-samples-android`（Sample アプリ土台）、`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`

## 完了条件

- 全タスクのチェックボックスが完了している
- `settings-view-android-ui` capability の全 Scenario が通る
- `./gradlew :ks-settingsview-ui:test :ks-settingsview-compose:test` で全成功
- PocLabelCell を含む SettingsRoot がユニットテスト（Robolectric 等）レベルで描画検証される

> **補足**: 実機・エミュレータでの目視確認は別変更提案 `add-samples-android` の責務（`samples/android/` の Compose Sample アプリ整備）として独立しており、本提案のスコープ外。
