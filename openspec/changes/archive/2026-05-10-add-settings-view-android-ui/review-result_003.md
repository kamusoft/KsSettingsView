# レビュー結果 - add-settings-view-android-ui (3 回目: Suggestion 追加対応のレビュー)

**レビュー日時**: 2026年05月10日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-android-ui
**前回レビュー**: review-result_002.md (`APPROVED`、Suggestion 3 件残置)

## サマリー

review-result_002.md にて `APPROVED` 判定の上で残置されていた Suggestion 3 件への追加対応をレビューした。各 Suggestion の対応状況は以下の通り。

| # | Suggestion | 対応状況 |
|---|------------|----------|
| 1 | `CellListItem.kt:10` KDoc / `design.md:166` の `sealed class` → `sealed interface` 表記訂正 | **完全に解消** |
| 2 | `SectionAccessoryViewHolders.bindKsAnyView` を `MutableState` + `container.tag` キャッシュ方式に最適化（再 bind 時の `setContent` 再呼び出しを回避） | **設計・実装ともに妥当に解消**（テストでも動作担保） |
| 3 | `KsSettingsViewComposeTest` を `createComposeRule()` ベースの Compose UI Test に書き換え | **意図通りに解消**（限界は tasks.md 10.1 に明記） |

ビルド・テストはすべて成功。`./gradlew :ks-settingsview-ui:test :ks-settingsview-compose:test --rerun-tasks` を実行し、`testDebugUnitTest` 配下 19 テストクラスが `failures=0 / errors=0 / skipped=0` で完走することを確認した（implementer 報告の core 47 + ui 43 + compose 8 = 98 件と整合）。

特に Suggestion 2 の `bindKsAnyView` 改修は次の 4 点を満たす良設計：

1. **ComposeView 単一インスタンス再利用**: `container.tag` に `ComposeAccessoryHolder(composeView, contentState)` をキャッシュ。再 bind 時は `state.value = anyView.content` の代入のみで Compose の差分更新が走る。
2. **Compose↔AndroidView 切替の安全性**: `KsAnyView.AndroidView` 分岐で `container.tag = null` 設定後に `removeAllViews()` を呼ぶ順序になっており、ComposeView が detach されることで `DisposeOnDetachedFromWindow` 戦略により Composition が確実に破棄される。
3. **ViewHolder 再利用時の状態リセット**: `reset()` で `removeAllViews` + `tag = null` を行い、次 bind で新規 ComposeView を生成する経路を保証（テスト「reset で ComposeView 再利用キャッシュが解放される」で検証済み）。
4. **Section / Root 双方に同等の改修**: `SectionAnyViewAccessoryViewHolder` と `RootAnyViewAccessoryViewHolder` の両方で `bindKsAnyView` を共有しており重複なし。

仕様（spec / proposal / design / tasks）と実装の整合性は完全に保たれている。

**判定**: `APPROVED`

---

## 各 Suggestion の対応確認詳細

### Suggestion 1: `sealed class` → `sealed interface` の用語訂正（解消確認）

**確認内容**:
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellListItem.kt:10`
  - 修正前: `「sealed class」で 3 つのサブタイプに分岐させる…`
  - 修正後: `「sealed interface」で 3 つのサブタイプに分岐させる…`（実装行 18 の `internal sealed interface CellListItem` と一致）
- `openspec/changes/add-settings-view-android-ui/design.md:166`
  - 修正前: `… sealed class subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。`
  - 修正後: `… sealed interface subtype をまず比較してから ID 比較する実装で、誤検出を防ぐ。`

**評価**: 仕様・実装間で `sealed interface` / subtype 名 `CellRow` の表記が完全に一貫した。Source of Truth spec への sync 段階でも齟齬なし。**解消**。

### Suggestion 2: `ComposeView` 再利用最適化（解消確認）

**確認内容**:

`SectionAccessoryViewHolders.kt` の `bindKsAnyView`（lines 213–255）が以下の構造に書き換えられている。

```kotlin
internal fun bindKsAnyView(container: FrameLayout, anyView: KsAnyView) {
    when (anyView) {
        is KsAnyView.Compose -> {
            val cached = container.tag as? ComposeAccessoryHolder
            if (cached == null) {
                container.removeAllViews()
                val state = mutableStateOf<@Composable () -> Unit>(anyView.content)
                val composeView = ComposeView(container.context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent { state.value.invoke() }
                }
                container.addView(composeView, ...)
                container.tag = ComposeAccessoryHolder(composeView, state)
            } else {
                cached.contentState.value = anyView.content
            }
        }
        is KsAnyView.AndroidView -> {
            container.tag = null
            container.removeAllViews()
            val nativeView: View = anyView.factory(container.context)
            container.addView(nativeView, ...)
        }
    }
}

private data class ComposeAccessoryHolder(
    val composeView: ComposeView,
    val contentState: MutableState<@Composable () -> Unit>,
)
```

設計上のチェックポイント評価：

| 観点 | 評価 |
|------|------|
| 再 bind 時の `setContent` 重複呼び出し回避 | `setContent` は ViewHolder 単位で 1 度だけ。state 値差し替えのみで recomposition が発火する Compose 慣用パターンであり、JetPack 公式の `AndroidView` `update` ブロックや `LazyColumn` の item recomposition と同型 |
| `DisposeOnDetachedFromWindow` 維持 | ComposeView 生成時に同戦略を強制設定しており、ViewHolder detach 時に Composition が確実に破棄される（既存仕様 Decision 5 を遵守） |
| Compose↔AndroidView 切替時のメモリリーク防止 | `container.tag = null` を先に実行 → `removeAllViews()` で ComposeView が detach → `DisposeOnDetachedFromWindow` により Composition 破棄。順序が安全 |
| ViewHolder 再利用 (`onViewRecycled` → `reset()`) | `reset()` で `removeAllViews` + `tag = null`。次 bind で新規 ComposeView を生成（テストで参照差確認済み） |
| Section / Root 共通化 | `SectionAnyViewAccessoryViewHolder` / `RootAnyViewAccessoryViewHolder` の両方が同一 `bindKsAnyView` を呼ぶため、変更が一元化 |

懸念点と評価：

- **`container.tag` の利用**: `FrameLayout.tag` は public API（任意 Object 格納用）で外部から書き換え可能だが、`SectionAnyViewAccessoryViewHolder` / `RootAnyViewAccessoryViewHolder` の itemView は ViewHolder 内部で完結しており、外部から tag を触る経路がない。安全。
- **Compose→Compose 切替で `KsAnyView.Compose.content` ラムダが capture する別 state（外部 `mutableStateOf` 等）への反応性**: `state.value.invoke()` は state.value（コンテンツラムダ）が変わる度に呼び直される。コンテンツラムダ内部で `remember` / `LaunchedEffect` を使う場合のキー再評価は通常の Compose 規則に従う。ラムダインスタンスが同一でも内部 state 参照は recomposition で再評価されるため、KsAnyView 利用者の期待を裏切らない。
- **`setContent` の Compose 設定面の差**: 本実装では再 setContent を呼ばないため `Composition` が継続。ライフサイクル単位で 1 つの Composition が走る、JetPack 推奨形に近い。

テストでの動作担保：

- `SectionAccessoryRenderingTest`（7 件）に新規 2 ケース追加：
  - `SectionAccessory View Compose から AndroidView に切り替えると ComposeView は破棄される`（Compose↔AndroidView 切替のリグレッション検証）
  - `SectionAnyViewAccessoryViewHolder の reset で ComposeView 再利用キャッシュが解放される`（reset 経路の検証）
- 既存ケース `SectionAccessory View の中身を差し替えても同一 ViewHolder で再描画される` は実装の意図通りに「ComposeView 自体は再利用される」（`assertEquals(firstChild, container.getChildAt(0))`）形に書き換わっており、再利用設計の期待値が明示された。

**評価**: 設計・実装・テストともに妥当。`setContent` 再呼び出し回避による Composition の継続性・GC 圧低減・`DisposeOnDetachedFromWindow` との一貫性が達成され、Suggestion を完全に解消している。**解消**。

### Suggestion 3: `KsSettingsViewComposeTest` の Compose UI Test 化（解消確認）

**確認内容**:
- `androidx.compose.ui:ui-test-junit4` を `testImplementation`、`androidx.compose.ui:ui-test-manifest` を `debugImplementation` / `releaseImplementation` 双方に追加（Robolectric の release variant でも ComponentActivity が解決できるよう、両 variant に manifest を載せる対応）。
- `androidx.recyclerview:recyclerview:1.3.2` を `testImplementation` に追加（テストヘルパで `RecyclerView` を直接参照するため）。
- `KsSettingsViewComposeTest` を `@get:Rule val composeRule = createComposeRule()` ベースに書き換え。3 ケース：
  1. **「KsSettingsView Composable は SettingsRoot を反映してレンダリングされる」** — `composeRule.setContent { KsSettingsView(...) }` で例外なくレンダリング、`composeRule.waitForIdle()` で待機。
  2. **「settingsRoot DSL で構築した SettingsRoot が KsSettingsViewLayout に反映される」** — テストで直接 `AndroidView` を起動し、`factory` 内で内部 layout 参照を `capturedLayout` に保持。`KsSettingsViewLayout.root` が DSL 結果に一致し、内部 RecyclerView の `itemCount == 3`（SectionHeader + CellRow×2）を assert。
  3. **「style 引数の既定値は Classic」** — `style` 省略時の Composable 起動が成功することを確認。

**動的 state 更新テスト**は tasks.md 10.1 の実装メモで「ListAdapter の AsyncListDiffer の非同期性により Robolectric では不安定なため、実 UI のレンダリング確認は `add-samples-android` の Sample アプリに委ねる」と明記。これは `AsyncListDiffer` がバックグラウンド Executor で差分計算→メインスレッドへ post する設計上、Robolectric の `Looper` 進行制御だけでは決定論的なテストが書けないことが理由として妥当（`InstantTaskExecutorRule` 相当の AsyncListDiffer 用テストツールは AndroidX 公式に存在しない）。Sample アプリ（`add-samples-android`）への委譲方針は、本変更提案 tasks.md 10.1 / 完了条件の補足とも整合。

**ビルド・テスト確認**: `:ks-settingsview-compose:testDebugUnitTest` が成功し、`KsSettingsViewComposeTest` の 3 件が `failures=0`。`createComposeRule()` で起動した Composition は Robolectric 上で安定。

**評価**: Compose UI Test の依存追加（`ui-test-junit4` / `ui-test-manifest` を debug/release 両 variant）は適切。Robolectric バックエンドでの起動も問題なく動作している。動的 state 更新の Sample アプリ委譲も理由とともに tasks.md に明記されており、変更提案の責務分離（`add-samples-android` 別変更提案）と整合する。**解消**。

---

## リグレッションの有無

`bindKsAnyView` の改修と `KsSettingsViewComposeTest` の刷新が既存挙動に影響していないかを以下の観点で確認した。

- **`DisposeOnDetachedFromWindow` 強制基盤クラス（`ComposeCellViewHolder`）の維持**: 本対応では Section H/F 用の `bindKsAnyView` が ViewHolder 内で `ComposeView` を直接生成する経路だが、`setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を生成時に必ず設定しており、Decision 5 の方針と一致。`ComposeCellViewHolder` 基盤クラス自体は Cell の `add-cell-types-custom` 用途で残置されており、本改修は影響しない。
- **Section / Root H/F の差し替え挙動**: 既存の Compose↔Compose、Compose↔AndroidView、AndroidView↔Compose、AndroidView↔AndroidView の 4 経路すべてが container の中身を期待通りに置き換える（テスト「中身を差し替えても同一 ViewHolder で再描画される」「Compose から AndroidView に切り替え」「reset で再利用キャッシュ解放」で検証済み）。
- **stable IDs / `RootHeaderFooterAdapter`**: 本対応で touch していない。
- **`onViewRecycled` 経路**: `reset()` 内で `removeAllViews` + `tag = null` を実行する設計で、ViewHolder 再利用時の状態残骸を防止。Suggestion 2 の修正後も `KsSettingsListAdapter.onViewRecycled` の reset 呼び出し契約は維持されている。
- **テスト副作用**: `KsCellRegistry` シングルトンへの登録は既存テストと変わらず、`@After tearDown` で `clear()` + `strictMode` リセットの契約も保たれている。Compose UI Test 追加でも他テストへの副作用なし。

**結論**: リグレッションなし。

---

## 仕様・タスク・実装メモの整合性

- `proposal.md` / `design.md` / `tasks.md` / `spec.md`：Suggestion 1 の用語訂正で `sealed interface` 表記が完全一致。Suggestion 2 の改修は実装詳細であり仕様書への記載対象ではない（`KsAnyView` の差分検出方針・`setContent` 制約は spec に存在しない）。Suggestion 3 の Compose UI Test 化は tasks.md 10.1 の実装メモが更新済み。
- `tasks.md` 10.1 実装メモ：`createComposeRule()` を Robolectric バックエンドで動かす旨が明示され、動的 state 更新の Sample 委譲理由も妥当に記載。**禁止事項（仕様書き換え・proposal/design に反する指摘）には一切該当しない**。
- 完了条件：tasks.md の全タスクが `[x]` で完了状態。`./gradlew :ks-settingsview-ui:test :ks-settingsview-compose:test` も全成功。

---

## 指摘事項

なし（残課題ゼロ）。

## アクションプラン

なし。本変更提案は archive に進んで問題ない。

## 判定結果

**ステータス**: `APPROVED`

**理由**:
- review-result_002.md で残置されていた Suggestion 3 件すべてが、設計・実装・テスト・ドキュメントの観点で適切に解消されている。
- ビルド・テストはすべて成功（19 テストクラス、98 件、failures=0 / errors=0 / skipped=0）。
- リグレッションは見当たらず、`DisposeOnDetachedFromWindow` 強制 / Section/Root H/F の差し替え挙動 / ViewHolder 再利用契約のいずれも維持されている。
- 仕様（spec / proposal / design / tasks）と実装の整合性は完全に保たれており、Source of Truth spec への sync 段階としても十分な品質。

archive 段階に進めて問題ないと判断する。
