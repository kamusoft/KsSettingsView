# レビュー結果 - purify-core-extract-style-to-ui-layer (#004)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: purify-core-extract-style-to-ui-layer
**レビュー対象**: Round 3（review-result_003.md）の残課題 2 件への対応
- Major 1: flaky テスト `DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される` の決定的同期修正
- Minor 1: `applyThemeInternal` での `notifyDataSetChanged()` 3 連発の最適化（payload 付き `notifyItemRangeChanged` 化）

---

## サマリー

review-result_003.md で指摘した 2 件の残課題に対する追加実装をレビューした。いずれも丁寧に対応されており、spec 違反・リグレッションリスクは認められない。

### 主な確認事項

#### A. flaky テスト修正
- `KsSettingsViewComposeTest.kt` に `flushIdle()` / `waitForAdapterItemCount(layout, expected, maxIterations)` の 2 ヘルパを追加し、`composeRule.waitForIdle()` で拾いきれない Robolectric main looper 上の AsyncListDiffer commit メッセージを `shadowOf(Looper.getMainLooper()).idle()` で空になるまで進める設計。
- 既存の `waitForIdle()` 呼び出しを `flushIdle()` に置換、最終 assert 直前で `waitForAdapterItemCount(layout, expected = 3)` を呼んで決定的に同期。
- 単独実行・フル `--rerun-tasks` 含めて 3 回連続で BUILD SUCCESSFUL を確認。
- 加えて Round 3 Minor 2（Compose 層 Theme パラメータ経路の単体テスト不在）も同時に対応されており、`DSL 方式で theme パラメータを変更すると layout の theme に反映される` / `DSL 方式で同値の theme は recomposition 後も layout に再設定されない` の 2 件が追加されている。

#### B. notifyDataSetChanged 3 連発の最適化
- `KsSettingsView.kt` の `applyThemeInternal` で、3 つの子 Adapter (`mainListAdapter` / `headerAdapter` / `footerAdapter`) に対する `notifyDataSetChanged()` を payload 付き `notifyItemRangeChanged(0, itemCount, PAYLOAD_THEME)` に置換。
- 各 Adapter ごとに `itemCount > 0` ガードを入れ、空 Adapter には不要な通知を発行しない。
- `@SuppressLint("NotifyDataSetChanged")` を削除し、`companion object` に `const val PAYLOAD_THEME: String = "ks-theme"` を導入。
- 副次的に `KsSettingsView.theme` setter には同値スキップガード（`if (themeBacking == value) return`）も追加され、Suggestion 1 にも対応されている。

### 仕様準拠の確認

`settings-view-android-host/spec.md` の **「Theme プロパティ更新で表示が再評価」Scenario** は
> **THEN** `RecyclerView.backgroundColor` が新 Theme の `viewBackgroundColor` に更新され、表示中の各 ViewHolder が新 Theme で再 bind される。`mainListAdapter.submitList(...)` は呼ばれない（構造差分ではないため）

を要求する。本実装は:
1. `recyclerView.setBackgroundColor(theme.viewBackgroundColor.toArgb())` で背景色更新
2. `notifyItemRangeChanged(0, itemCount, PAYLOAD_THEME)` で全表示中 ViewHolder に payload 付き通知
3. `submitList` は呼ばない
4. `applyDecoration(style)` で ItemDecoration（separator 色等）を再構築

を行っており、spec 要件を完全に満たしている。

### テスト実行結果

```
./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test --rerun-tasks
→ BUILD SUCCESSFUL in 14s（166 actionable tasks: 166 executed）

./gradlew :ks-settingsview-compose:test --rerun-tasks  # 3 回連続実行
→ 3/3 BUILD SUCCESSFUL（flaky 再発なし）
```

**ステータス**: `APPROVED`

---

## 指摘事項

### Critical: なし

### Major: なし

### 🟡 Minor

#### Minor 1: `waitForAdapterItemCount` が失敗時に sleep / 早期失敗しない

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:373-385`（追加された `waitForAdapterItemCount`）

**問題点**:
```kotlin
private fun waitForAdapterItemCount(
    layout: KsSettingsViewLayout,
    expected: Int,
    maxIterations: Int = 50,
) {
    for (i in 0 until maxIterations) {
        if (layout.recyclerViewItemCountForTest() == expected) return
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }
    // ここで return しなくても assertEquals 側で詳細メッセージ付きで失敗するため、即時失敗はしない。
}
```

実装方針として「最大 50 回ループしてからは呼び出し元の `assertEquals` に失敗を委ねる」設計だが、ヘルパ宣言の KDoc には「1 回 1ms 待機」とあるのに実際は `Thread.sleep` を持たず、`shadowOf(Looper.getMainLooper()).idle()` と `composeRule.waitForIdle()` の繰り返しで消費する設計。実装と KDoc コメントの "1 回 1ms 待機" の表現がズレている（実時間 sleep はない）。

実害は無い（むしろ Robolectric は擬似的な時間軸で動くため `Thread.sleep` 不要）が、コメント上の誤解を解消するために KDoc を「`shadowOf(Looper.getMainLooper()).idle()` と `composeRule.waitForIdle()` を交互に最大 [maxIterations] 回呼び、各反復で Robolectric main looper のメッセージキューが完全に消費されるまで進める。実時間 sleep は持たない」のように整理すると正確になる。

**推奨修正**:
```kotlin
/**
 * RecyclerView Adapter の itemCount が期待値に達するまで、main looper の idle と
 * `composeRule.waitForIdle()` を交互に進めるヘルパ。
 *
 * `AsyncListDiffer` の commit コールバックは main looper にバウンスされるが、その時点で
 * `KsSettingsView` 内の `submitContentUpdate` が次の `notifyItemChanged` を発行することもあり、
 * 1 度の `flushIdle` では収束しないケースがある。本ヘルパは最大 [maxIterations] 回（既定 50 回）
 * まで `shadowOf(Looper.getMainLooper()).idle()` + `composeRule.waitForIdle()` を交互に呼び、
 * 期待状態に到達するまで決定的に同期する（実時間 sleep は持たない）。
 *
 * @throws AssertionError 期待値に到達しなかった場合は本ヘルパは何もせず return し、呼び出し元の
 *   assertEquals 側で詳細メッセージ付きの失敗が発生する。
 */
```

#### Minor 2: `flushIdle()` の 2 段階 `waitForIdle()` の意図がやや不明

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:355-360`

**問題点**:
```kotlin
private fun flushIdle() {
    composeRule.waitForIdle()
    shadowOf(Looper.getMainLooper()).idle()
    composeRule.waitForIdle()
}
```

`composeRule.waitForIdle()` を 2 回呼んでいるが、KDoc コメントには 1 回目と 2 回目の役割の差が明示されていない。実際は:
- 1 回目: Compose runtime のリコンポーズが完了するのを待つ
- `shadowOf(Looper.getMainLooper()).idle()`: AsyncListDiffer の main looper post を消費
- 2 回目: AsyncListDiffer の commit に伴って Compose 側で起きうる二次的なリコンポーズ（例: `AndroidView.update` 内 `extraUpdate` が次の Diff を発行する経路）を再度待つ

という構造のはず。コメントを補強しておくと将来の保守時に役立つ。

**推奨修正**:
```kotlin
/**
 * Compose runtime と Robolectric の main looper の両方を idle まで進めるヘルパ。
 *
 * 1 回目の `composeRule.waitForIdle()` で Compose のリコンポーズを完了させ、`AndroidView.update` を
 * 確実に走らせる。その後 `shadowOf(Looper.getMainLooper()).idle()` で AsyncListDiffer の main looper
 * post を消化。最後にもう 1 度 `composeRule.waitForIdle()` を呼んで、AsyncListDiffer 完了後に
 * 起きうる二次的なリコンポーズ（次世代 Diff 発行など）も拾う。
 */
```

#### Minor 3: `applyThemeInternal` 内 `applyDecoration(style)` のコメント追加余地

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:498-499`

**問題点**:
```kotlin
// ItemDecoration を新 Theme で新規構築（separator 色等の反映）
applyDecoration(style)
```

`applyDecoration` は内部で `recyclerView.removeItemDecoration(currentDecoration)` → `recyclerView.addItemDecoration(new)` を行う設計だが、Theme 切替で ItemDecoration が「再構築（重新建立）される」点がコード上やや暗黙的。`ItemDecoration` は `internalTheme` を constructor 引数として受けるため、Theme 反映には再構築が必須であることを 1 行コメントで明示してあるとレビュー追跡しやすい。

**推奨修正**:
```kotlin
// ItemDecoration は constructor で theme をキャプチャするため、Theme 反映には再構築が必要。
// applyDecoration() 内部で remove → add の入れ替えを行う。
applyDecoration(style)
```

これは現状コメントでも意図が読めるので Minor の中でも軽い指摘。

---

### 🔵 Suggestion

#### Suggestion 1: payload を活かす将来拡張余地のコメント追加

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:613-621`

**現状コメント**:
```kotlin
/**
 * Theme 更新時の `notifyItemRangeChanged` payload キー。
 *
 * `applyThemeInternal` から発行される部分更新通知に付与する。各 ViewHolder の
 * `onBindViewHolder(holder, position)`（payload なし版）が呼ばれて再 bind されるため
 * 視覚的同等性は保たれる。payload 自体は将来「Theme 差分のみを反映する高速 bind」を
 * 各 ViewHolder で実装したくなったときの識別子として残している。
 */
const val PAYLOAD_THEME: String = "ks-theme"
```

意図は明確で良いが、現時点で「payload あり経路の bind オーバーロードを実装したくなった場合は `onBindViewHolder(holder, position, payloads)` を override し、`PAYLOAD_THEME` を含むなら Theme 差分のみを反映する fastBind を行う」のような実装ガイドを書いておくと、後続改修時に方針がブレない。

**推奨修正（任意）**:
```kotlin
/**
 * Theme 更新時の `notifyItemRangeChanged` payload キー。
 *
 * 現状: `onBindViewHolder(holder, position, payloads)` をオーバーライドしている Adapter / ViewHolder は
 * なく、payloads 版のデフォルト実装が `onBindViewHolder(holder, position)`（payload なし版）に委譲する
 * ため、結果として全 ViewHolder で通常の `bind(cell, theme)` が走り Theme 反映は確実に行われる。
 * このため payload キー自体は現状で動作上は無視される識別子。
 *
 * 将来「Theme 差分のみを反映する高速 bind」を実装する場合は、各 Adapter で
 * `onBindViewHolder(holder, position, payloads)` を override し、payloads が `PAYLOAD_THEME` を
 * 含む場合のみ Theme 関連プロパティの差分更新（背景色 / 文字色のみ）を行う fastBind 経路に
 * 分岐させると良い。
 */
const val PAYLOAD_THEME: String = "ks-theme"
```

#### Suggestion 2: `notifyItemRangeChanged` 連発時の `DefaultItemAnimator` 挙動の明示

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:483-497`

**補足説明**:
`DefaultItemAnimator.canReuseUpdatedViewHolder(viewHolder, payloads)` は payloads が空でない場合のみ `true` を返し、change animation を抑制して同一 ViewHolder の更新として扱う。本実装では `PAYLOAD_THEME` を必ず渡すため、Theme 更新時に change animation のちらつきは発生せず、`notifyDataSetChanged`（全 ViewHolder 再生成）よりも視覚的に滑らかな更新が期待できる。

**推奨修正（任意）**:
コードコメントで「`DefaultItemAnimator` は payload あり通知に対して change animation をスキップし同一 ViewHolder への更新として扱う。これにより `notifyDataSetChanged` よりも滑らかな更新になる」と明記すると、payload 設計の理由がより伝わりやすい。

---

## アクションプラン

優先度順:

1. **[Minor 1, Minor 2 / 軽微改善]** `waitForAdapterItemCount` と `flushIdle` の KDoc 補強（"1 ms 待機" の誤解を解消、2 段階 `waitForIdle()` の意図を明示）。
2. **[Minor 3 / 軽微改善]** `applyDecoration(style)` 呼び出し直前のコメントで「ItemDecoration は constructor で theme をキャプチャするため再構築が必須」を明示。
3. **[Suggestion 1, 2 / 任意]** payload キーの将来拡張余地と `DefaultItemAnimator` 挙動の説明コメントを補強。

いずれも Critical / Major ではなく、本 Round の修正のリリース停止要件ではない。後続提案または同 PR 内のドキュメント補強で対応可能。

---

## 検証した観点と結果

| 観点 | 結果 |
| --- | --- |
| openspec の Spec / Scenario 違反 | なし。`settings-view-android-host/spec.md` の Theme プロパティ更新 Scenario の MUST 条件をすべて満たす |
| テスト同期の妥当性（`shadowOf(Looper.getMainLooper()).idle()`） | 正しい。Robolectric の main looper を idle まで進める標準パターン |
| payload 付き `notifyItemRangeChanged` の使い方 | 正しい。`itemCount > 0` ガード入り、payloads 版 `onBindViewHolder` をオーバーライドしていないため payload なし版の `bind(cell, theme)` 経路で再 bind が確実に走る |
| ConcatAdapter 配下での挙動 | 正しい。各子 Adapter で個別に `notifyItemRangeChanged` を発行する設計は ConcatAdapter で必須（ConcatAdapter 自体に notify メソッドはない） |
| 視覚的同等性（Theme 全 Cell 再 bind） | 担保されている。全表示中 ViewHolder で `bind(cell, theme)` または `bind(accessory, theme, ...)` が再実行され、Theme 由来の色・フォントが反映される。`DefaultItemAnimator.canReuseUpdatedViewHolder` の挙動で change animation のちらつきもなし |
| ItemDecoration 再構築経路 | 担保されている。`applyDecoration(style)` で `currentDecoration` を `removeItemDecoration` → 新 Theme で `addItemDecoration` する経路は変更なし |
| 既存テスト（Theme 変更系・ApplyDiff 系）への影響 | リグレッションなし。`SettingsRootStoreTest.applyTheme` / `KsSettingsViewComposeTest` の Theme 経路 2 件 / その他 ApplyDiff 系テストすべて緑 |
| パフォーマンス改善方向 | 妥当。`notifyDataSetChanged` の全 ViewHolder 再生成・onCreate コストを避け、`notifyItemRangeChanged` で同一 ViewHolder への再 bind に留めるのは正方向の最適化 |
| iOS / 他 active 提案への影響 | なし。Android 側の `KsSettingsView` 内部実装に閉じた変更で iOS 側は影響を受けない。他 active 提案（add-maui-bridge / add-cell-types-input 等）も影響なし |
| flaky テスト再発確認 | `./gradlew :ks-settingsview-compose:test --rerun-tasks` を 3 回連続実行で全て BUILD SUCCESSFUL。フル `:ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test --rerun-tasks` も BUILD SUCCESSFUL |
| 同値 Theme 代入時のループ防止 | 正しく実装されている。`MutableStateFlow.value` setter の equals 判定 + `KsSettingsView.theme` setter の `themeBacking == value` ガード + Compose 側 `bookkeeper.lastTheme != theme` ガードの 3 段防御 |

---

## 判定結果

**ステータス**: `APPROVED`

### APPROVED の根拠

1. Round 3 で残課題として明示した Major 1（flaky テスト）と Minor 1（notifyDataSetChanged 3 連発）の両方が、適切な設計・実装で解決されている。
2. spec `settings-view-android-host/spec.md` の Theme プロパティ更新に関する MUST / SHALL Requirement と Scenario をすべて満たしており、規約違反はない。
3. 全テスト（166 actionable tasks）が BUILD SUCCESSFUL、flaky テストも 3 回連続 rerun で再発しない。
4. Round 3 Minor 2（Compose 層 Theme 経路テスト不在）にも対応されており、`DSL 方式で theme パラメータを変更すると layout の theme に反映される` / `DSL 方式で同値の theme は recomposition 後も layout に再設定されない` の 2 件で経路の回帰防止テストが追加されている。
5. Round 3 Suggestion 1（`KsSettingsView.theme` setter の同値スキップガード）にも対応されており、`if (themeBacking == value) return` が setter 内に追加されている。
6. payload 付き `notifyItemRangeChanged` の設計選択は適切で、`DefaultItemAnimator.canReuseUpdatedViewHolder` の挙動により `notifyDataSetChanged` よりも滑らかな視覚更新が得られる。
7. 残る指摘事項は全て Minor / Suggestion で、コメント補強レベルの軽微な改善のみ。リリース停止要件はない。

Round 3 で APPROVED 判定済みの本提案について、残課題対応の Round 4 も問題なく完遂されている。本提案は **アーカイブ可能** な品質に達している。
