# Candidate: settings-view-android-host

## 概念候補

### Android Native Host (提案カテゴリ: platforms/)

#### 目的

`KsSettingsView` は Core の `SettingsRoot` を Android View のリストへ接続する公開 Host である。XML / `findViewById` から直接利用できるほか、Compose の `AndroidView` と外部バインディングが再利用する描画基盤になる。Core モデルの定義、宣言ツリー同士の比較、`Theme` / `CellStyle` の実効値解決は Host の責務ではない。

#### 責務境界

- `SettingsRootStore` の初期 `state` / `theme` を取り込み、構造変更、内容更新バッチ、Theme 変更を View のライフサイクルに結び付けて購読する。
- hidden を含む完全な `SettingsRoot` を保持し、`flatten` で visible projection を `CellListItem` の平坦リストへ変換する。
- Root H/F は `rootHeader` / `rootFooter`、Section H/F は `Section.header` / `footer` として描画する。Root H/F は `SettingsRoot` に含めない。
- Cell 固有の ViewHolder 生成は `KsCellRegistry` へ委譲し、Host 本体は具象 Cell 型を知らない。
- `style` と Section H/F の寸法・罫線・行レイアウトは `settings-view-android-style`、Compose DSL と宣言ツリーの Diff 算出は `settings-view-android-compose`、Theme 値の解決は `settings-view-android-theme-bridge` の候補へ合流させる。

#### 保証すること

- 公開入口は `KsSettingsView(context, attrs)`、`bind(store)`、`applyDiff(diff)`、`theme`、`style`、`rootHeader`、`rootFooter` である。`SettingsRoot` の公開 setter は持たない。
- `bind(store)` は `state.value` と `theme.value` を即時反映する。attach 前で `ViewTreeLifecycleOwner` が得られなくても Store を保持し、`onAttachedToWindow` で購読開始を再試行する。同じ Store の再 bind では有効な購読を作り直さず、別 Store への bind では古い購読を cancel する。
- 空の `SettingsRoot` は有効な入力であり、空の `RecyclerView` として描画できる。
- `RecyclerView` は Root Header、Section H/F + Cell、Root Footer の順序を維持する。Root H/F の `null` / 非 `null` 遷移は 0 / 1 行の追加・削除として反映される。
- `RootAccessory.Text` / `.View` と `SectionAccessory.Text` / `.View` を扱う。任意 View は `KsAnyView.Compose` または `KsAnyView.AndroidView` を表示できる。
- `theme` 更新は構造リストを差し替えず、背景、表示中の行、Root / Section H/F、`ItemDecoration` を新 Theme で再評価する。
- detach 時は Store 購読を cancel し、内部 `RecyclerView.adapter` を `null` にして RecyclerView から Adapter 参照を切る。

#### してはいけないこと

- 利用者コードから内部 `SettingsRoot` や `setRootDirect` を直接操作しない。`setRootDirect` は module-internal の Test / Preview 用である。
- Theme 更新を `SettingsRootDiff` に混ぜない。
- Cell 具象型ごとの分岐を `KsSettingsView` へ追加しない。
- hidden 要素を完全 model から削除して可視性を表現しない。
- `SettingsRootDiff` の index を visible projection 上の位置として渡さない。

#### 公開 API

- `KsSettingsView(context, attrs)` / `bind(store)` / `applyDiff(diff)`。
- `theme` / `style` / `rootHeader` / `rootFooter`。
- `SettingsRootStore(initialRoot, initialTheme)` / `SettingsRootStore.preview(root, theme)`。

#### 利用例

```xml
<jp.kamusoft.kssettingsview.ui.KsSettingsView
    android:id="@+id/settings_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
val section = Section(
    id = "general",
    header = SectionAccessory.Text("一般"),
    cells = listOf(LabelCell(title = "バージョン", valueText = "1.0.0")),
)
val store = SettingsRootStore(
    initialRoot = SettingsRoot(sections = listOf(section)),
    initialTheme = Theme(),
)

findViewById<KsSettingsView>(R.id.settings_view).apply {
    rootHeader = RootAccessory.Text("プロフィール")
    bind(store)
}

store.insertCell(
    cell = LabelCell(title = "ライセンス"),
    sectionId = section.id,
    at = section.cells.size,
)
```

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ApplyDiffTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootAccessoryRenderingTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapterTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/MemoryLeakTest.kt` / `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt` / `openspec/specs/settings-view-android-host/spec.md` (Purpose) / `docs/platform-guide-android.md`

### Store の状態と更新通知 (提案カテゴリ: architecture/)

#### 目的

`SettingsRootStore` は、購読開始時点でも取得できる現在状態と、その後の変更意図を伝える一過性通知を分離する。Android では `StateFlow<SettingsRoot>` / `StateFlow<Theme>` と module-internal の `SharedFlow<SettingsRootDiff>` / 内容更新バッチを組み合わせる。

#### 責務境界

- hidden 要素を含む完全な `SettingsRoot` と現在の `Theme` を保持する。
- Section / Cell 操作では保持状態を先に更新し、同じ操作を表す構造 Diff を発行する。
- 連動する複数 Cell の内容更新は `replaceCells` で一つの更新バッチとして発行する。
- Theme は構造ではないため、`applyTheme` では `theme` のみ更新し、構造 Diff を発行しない。
- 平坦リスト、ViewHolder、アニメーション、model から visible projection への変換は Host が担う。

#### 保証すること

- `state` と `theme` は読み取り専用 `StateFlow` として公開され、利用者は Store の公開操作を通して更新する。
- insert / move の位置は hidden を含む model 配列上の位置であり、挿入先は有効範囲へ clamp される。
- 対象が存在しない Section / Cell の remove / move / replace と、存在しない Section への Cell insert は、状態を変えず構造 Diff も発行しない。
- `replaceCells` は存在する Cell だけを一つの状態 commit にまとめ、適用した Cell ID 群を一つの内容更新バッチとして流す。適用対象が 0 件なら状態も通知も変えない。
- `applyTheme` は `SettingsRootDiff` を発行しない。現在 Theme は `theme.value` から復元できる。
- 構造通知は replay されないため、購読開始時の復元は `state.value`、Theme の復元は `theme.value` を使う。

#### してはいけないこと

- `state` / `theme` の内部 `MutableStateFlow` を利用者側から直接更新しない。
- Theme 更新を構造 Diff として発行しない。
- Store 内で RecyclerView、Adapter、ViewHolder を操作しない。
- `replaceCell` / `replaceCells` で Cell ID を変更しない。同一 ID の内容更新に使い、ID 変更は構造操作で表す。

#### 公開 API

- 初期化: `SettingsRootStore(initialRoot, initialTheme)`、`preview(root, theme)`。
- Root / Section: `replaceAll`、`insertSection`、`removeSection`、`moveSection`、`replaceSection`。
- Cell: `insertCell`、`removeCell`、`replaceCell`、`replaceCells`、`moveCell`。
- Accessory / Theme: `updateAccessory`、`applyTheme`。
- 現在値: `state`、`theme`。

#### 利用例

```kotlin
val store = SettingsRootStore(initialRoot = root, initialTheme = Theme())
val view = KsSettingsView(context).apply { bind(store) }

store.insertCell(newCell, sectionId = "general", at = 0)
store.replaceCell(cellId = newCell.id, new = updatedCell)
store.applyTheme(darkTheme) // SettingsRootDiff は発行しない
```

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt` / `openspec/specs/settings-view-android-host/spec.md` (Purpose, SettingsRootStore) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/store-and-update-streams.md` / `docs/architecture.md`

### 表示状態同期 (提案カテゴリ: architecture/)

#### 目的

設定ツリーの変化は、表示構造、同一 ID の内容、可視性、Theme で必要な反映経路が異なる。Android Host はこれらを混同せず、行のちらつき、hidden 要素の喪失、複数 Cell の内容更新漏れを防ぐ。

#### 責務境界

| 対象 | Android Host の反映経路 |
|---|---|
| Section / Cell の追加・削除・移動 | `flatten` 後のリストを `ListAdapter.submitList` へ渡す |
| 同一 ID の Cell 内容更新 | リスト参照の commit 後、対象位置へ `notifyItemChanged` |
| 連動する複数 Cell の内容更新 | 単一 `submitList` の commit 後、対象位置群へ `notifyItemChanged` |
| `isVisible` の変更 | hidden を含む model から visible projection を再構築 |
| Theme | `theme` の専用経路で表示中の範囲を再 bind |

#### 保証すること

- `CellListItemDiffCallback.areItemsTheSame` は subtype と ID だけを比較し、`areContentsTheSame` は同一 item なら内容にかかわらず `true` を返す。
- stable item ID は Cell 内容に依存しない。Cell 行は `cell.id`、Section H/F は `sectionId` と役割から得た安定キーを使い、Root H/F の予約 ID と分離する。
- `ReplaceCell` は同一 ID の内容更新として同じ ViewHolder を再 bind する。RadioCell のように連動する複数 Cell は一つの commit 後にまとめて再 bind する。
- `internalRoot` は hidden Section / Cell を保持する。visible projection は hidden Section の H/F と全 Cell、`VisibilityAware.isVisible == false` の Cell を除外する。
- `VisibilityAware` に準拠しない利用者定義 Cell は safe-by-default で visible として扱う。
- hidden 対象への構造・Accessory 更新は model に反映し、projection に変化がなければ表示上の no-op になる。
- `ReplaceCell` で旧 Cell と新 Cell の可視性が異なる場合、および `ReplaceSection` の場合は、部分内容更新ではなく visible projection の全再構築へ fallback する。

#### してはいけないこと

- Cell の内容を構造上の同一性や stable item ID に含めない。
- 可視性変更を通常の内容更新だけで処理しない。
- hidden 要素を model から除去して可視性を実現しない。
- 連動する複数 Cell に対して独立した `submitList` を連続発行しない。
- Theme 更新を構造、内容、可視性のいずれかへ擬装しない。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` (`applyDiff`, `flatten`) / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/VisibilityAware.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ListAdapterDiffTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/FlattenVisibilityTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/VisibilityApplyDiffTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt` (`replaceCells`) / `openspec/specs/settings-view-android-host/spec.md` (DiffUtil, visible projection) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/display-state-synchronization.md` / `docs/architecture.md`

### Cell Renderer Registry (提案カテゴリ: architecture/)

#### 目的

`KsCellRegistry` と `CellViewHolder<T>` は Cell モデル型と Android 描画型の対応を Host の型分岐から分離する。標準 Cell と利用者定義 Cell を同じ解決経路へ載せ、Cell 追加時に `KsSettingsView` を変更しないための拡張境界である。

#### 責務境界

- `KsCellRegistry` は具象 `Cell` 型から `viewType` と `CellViewHolder` factory への対応を登録・解決する。
- `CellViewHolder.bind(cell, theme)` は最新 Cell と Theme を View へ反映し、`reset()` は再利用時に listener、Job、画像、埋め込み View などを解放する。
- Host は viewType 解決、ViewHolder 生成、bind / reset 呼び出しを担うが、Cell 固有の描画内容を知らない。

#### 保証すること

- `KsCellRegistry.register`、`viewTypeOf`、`isRegistered`、`strictMode`、`CELL_VIEW_TYPE_MIN` と `CellViewHolder<T>` は外部モジュールから利用できる。
- 同じ Cell 型の再登録は後勝ちで factory を置き換える。別の Cell 型へ同じ viewType を割り当てると `IllegalArgumentException` になる。同じ Cell 型の viewType を変更した場合は古い逆引きを残さない。
- 標準の基本 Cell 7 種と入力 Cell 5 種は `KsSettingsView` の構築時に自動登録される。利用者定義 Cell は表示前に明示登録する。
- 未登録 Cell / viewType は `strictMode == true` で `IllegalStateException` として早期検出し、`false` では高さ 0 の placeholder へ退避して画面全体のクラッシュを避ける。
- `KsAnyView.Compose` を表示する Accessory は `DisposeOnDetachedFromWindow` を使い、ViewHolder の再 bind では `ComposeView` を再利用して content state だけを更新する。recycle 時は埋め込み View とキャッシュを解放する。

#### してはいけないこと

- Cell 固有の描画分岐を `KsSettingsView` / `KsSettingsListAdapter` に追加しない。
- 利用者定義 Cell の viewType に Root / Section Accessory の予約領域を使わない。`KsCellRegistry.CELL_VIEW_TYPE_MIN` 以上を使う。
- ViewHolder の再利用時に前の listener、Job、画像、埋め込み View を残さない。
- `strictMode` の初期値をビルド種別の自動判定だと仮定しない。Android Library 自身はアプリ側の `BuildConfig.DEBUG` を参照できないため、利用アプリが起動時に明示設定する。

#### 公開 API と利用例

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG

KsCellRegistry.register(
    cellClass = MyCell::class,
    viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN + 50,
) { parent ->
    MyCellViewHolder(parent)
}

class MyCellViewHolder(parent: ViewGroup) :
    CellViewHolder<MyCell>(createMyCellView(parent)) {
    override fun bind(cell: MyCell, theme: Theme) {
        // 最新 Cell と Theme を反映する。
    }

    override fun reset() {
        // listener や非同期処理を解放する。
    }
}
```

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryBasicCells.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryInputCells.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellViewHolder.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryRenderingTest.kt` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt` / `openspec/specs/settings-view-android-host/spec.md` (Cell レジストリ, CellViewHolder, ComposeView ライフサイクル) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/cell-renderer-registry.md` / `docs/platform-guide-android.md`

## ADR 候補

- 構造同期は ID の同一性だけで判定し、同一 ID の内容更新と可視性変更を別経路へ分離する — 出典: `openspec/specs/settings-view-android-host/spec.md`「DiffUtil 差分検出」「visible projection の flatten 規約」「ReplaceCell / ReplaceSection の可視性切替防御」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt` / `ListAdapterDiffTest.kt`、選別基準: Host・Store・Compose DSL の境界を越え、将来の Diff 実装を制約する。
- Theme を `SettingsRoot` / `SettingsRootDiff` から分離し、`SettingsRootStore.theme` の専用 `StateFlow` で Host へ反映する — 出典: `openspec/specs/settings-view-android-host/spec.md`「KsSettingsView の公開 API」「SettingsRootStore（Android）」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`、選別基準: Core・Store・Host・Compose の境界を越え、将来の公開 API を制約する。
- Cell 可視性を Core の `Cell` へ要求せず、UI 層の `VisibilityAware` へ opt-in した型だけをフィルタし、非準拠型は visible と扱う — 出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/VisibilityAware.kt` / `KsSettingsView.flatten` / `FlattenVisibilityTest.kt` / `openspec/specs/settings-view-android-host/spec.md`「visible projection の flatten 規約」、選別基準: Core と UI の境界を越え、利用者定義 Cell の将来互換性を制約する。

## drift 所見

- Theme 更新シナリオは新 Theme の `viewBackgroundColor` が背景へ反映されると記すが、現行公開名は `Theme.backgroundColor` で、旧名は削除済みである (`openspec/specs/settings-view-android-host/spec.md`「Theme プロパティ更新で表示が再評価」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` / `ThemeRenameTest.kt`)。
- 同じ spec 内で Theme は `SettingsRootDiff` を通らないと定める一方、「Theme 更新」シナリオだけは削除済みの `SettingsRootDiff.UpdateTheme` を `applyDiff` へ渡している。現行コードでは `SettingsRootDiff` に Theme case がなく、`SettingsRootStore.applyTheme` → `theme: StateFlow<Theme>` の専用経路を使う (`openspec/specs/settings-view-android-host/spec.md`「DiffUtil 差分検出」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` / `KsSettingsView.kt`)。
- Cell 登録シナリオは利用者定義 Cell に `viewType = 1` を例示するが、現行 Host は 1〜4 を Section H/F 用に予約し、Cell の推奨開始値を `CELL_VIEW_TYPE_MIN = 100` としている。`KsSettingsListAdapter.onCreateViewHolder` は 1〜4 を Registry より先に Accessory ViewHolder へ分岐するため、この例は安全に動作しない (`openspec/specs/settings-view-android-host/spec.md`「Cell 型の登録と解決」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt` / `KsSettingsListAdapter.kt`)。
- `SettingsRootStore.replaceCells` は RadioCell グループ連動を一つの更新バッチとして扱う現行 public API だが、旧 spec の公開メソッド一覧と `docs/platform-guide-android.md` §11 の主要メソッド一覧にない (`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` / `SettingsRootStoreTest.kt` / `openspec/specs/settings-view-android-host/spec.md`「SettingsRootStore（Android）」 / `docs/platform-guide-android.md`)。
- `docs/platform-guide-android.md` §11 の Store 利用例は `store.insertCell(..., index = 0)` と記すが、現行 Kotlin API の引数名は `at` であり、このコードはコンパイルできない (`docs/platform-guide-android.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`)。
- 旧 concept は適用できない操作では「状態も通知も変えない」と一般化しているが、`SettingsRootStore.updateAccessory` は存在しない Section ID でも `updateSectionAccessory` の no-op 後に `UpdateAccessory` を無条件発行する (`kasane/changes/remigrate-concepts/reference/old-concepts/architecture/store-and-update-streams.md` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`)。
- `onDetachedFromWindow` は `recyclerView.adapter = null` にする一方、`onAttachedToWindow` は Store 購読の再開だけを行い、`concatAdapter` を再設定しない。同じ `KsSettingsView` インスタンスを detach 後に再 attach すると購読は再試行されても表示 Adapter は復元されない。spec は detach 時の解放と attach 時の利用可能性をそれぞれ要求しており、再 attach の契約が実装・テストとも未確立である (`openspec/specs/settings-view-android-host/spec.md`「初期化直後の状態」「メモリリーク防止」 / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` / `MemoryLeakTest.kt`)。

## 用語

- Host: Core の `SettingsRoot` / `SettingsRootDiff` を Android の `RecyclerView` へ接続する `KsSettingsView`。
- model: hidden Section / Cell を含む `SettingsRoot` の完全な状態。
- visible projection: `Section.isVisible` と `VisibilityAware.isVisible` により model から表示対象だけを取り出した派生状態。
- content update batch: 同じ状態 commit に属する複数 Cell ID を、一回の `submitList` 完了後にまとめて再 bind する更新単位。
- Registry: Cell 型から `viewType` と `CellViewHolder` factory を解決する `KsCellRegistry`。
- Root H/F: 画面全体の Header / Footer。`SettingsRoot` には含めず、`KsSettingsView.rootHeader` / `rootFooter` が保持する。

## 抽出メモ

- 4 概念候補を抽出した。`Android Native Host` は `platforms/` の Android 固有公開 API と利用例として独立させ、`Store の状態と更新通知`、`表示状態同期`、`Cell Renderer Registry` は iOS Host 候補と照合して `architecture/` へ統合するのが妥当である。ここでは統合判断を行っていない。
- Root / Section Accessory の型定義と `SettingsRootDiff` case 自体は Batch A の `settings-tree` / `structural-changes` と重なるため再定義せず、Android Host がどう表示・更新するかに限定した。
- RecyclerView の 3 Adapter、`CellListItem` の内部 sealed 階層、stable ID のハッシュ関数などは実装詳細であり、公開契約と表示状態同期の理解に必要な範囲だけ残した。
