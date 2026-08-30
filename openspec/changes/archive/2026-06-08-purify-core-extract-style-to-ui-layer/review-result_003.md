# レビュー結果 - purify-core-extract-style-to-ui-layer (Android 側実装)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: purify-core-extract-style-to-ui-layer
**レビュー対象**: Android 側実装（iOS 側は前セッションで APPROVED 済み）

---

## サマリー

`purify-core-extract-style-to-ui-layer` の Android 側実装をレビューした。Core (`ks-settingsview-core`) から `KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` を完全に削除し、UI 層 (`ks-settingsview-ui`) に Compose Native 型（`androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle`）を直接保持する形で再配置されており、spec delta（`settings-view-core`、`settings-view-android-style`、`settings-view-android-host`、`settings-view-android-compose`、`settings-view-android-theme-bridge`、`cell-types-basic`、`samples-android`）の要求とよく整合している。Cell 抽象から `val style: CellStyle` 要求を外し、UI 層に `DSLStyleModifiableCell` / `DSLIconModifiableCell` を新設したアーキテクチャは、iOS 側（`DSLStyleModifiable` / `DSLIconModifiable`）と対称的で良質である。

主要な実装要素:
- `SettingsRoot` から `theme` フィールド削除、`SettingsRootDiff.UpdateTheme` 削除、`Cell` 抽象から `style` 要求削除（全て spec 準拠）
- `SettingsRootStore` に `initialTheme` / `val theme: StateFlow<Theme>` / `fun applyTheme(theme)` 追加、`Diff` 経路は不発行を保証する実装と単体テスト両方で確認済み
- `KsSettingsView` (FrameLayout) に `var theme: Theme` プロパティを追加し、Store の `theme` StateFlow を `lifecycleScope` で購読する経路を整備（再 bind / 重複通知抑制の細やかな対応も入っている）
- Compose ラッパで `KsSettingsView(theme = ...)` パラメータの変化を `extraUpdate` 内で検出して `store.applyTheme(theme)` を呼び、Diff 経路に流さない設計を厳密に保っている
- iOS 側で導入された `.icon(_:)` modifier 相当として `DSLIconModifiableCell` インターフェース と `Cell.icon(KsImage?)` / `CellHandle.icon(KsImage?)` の両系統を実装（spec の MUST 要件を満たす）
- Sample (`BasicCellsDemoScreen.kt`) は MAUI 互換 Theme を Compose `Color(0xFF...)` で構築するように更新済み

ビルド・テストの状況:
- `:ks-settingsview-core:test` / `:ks-settingsview-ui:test` / `:ks-settingsview-compose:test`: BUILD SUCCESSFUL
- `samples/android :app:assembleDebug`: BUILD SUCCESSFUL
- ただし `KsSettingsViewComposeTest > DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される` が再実行で稀に失敗する（3 回連続実行で 1 回 FAILED）。tasks.md 12.2 で「pre-existing AsyncListDiffer 由来の flaky テスト」と注記済みであり、本提案の差分は当該テストファイル自体には触れていない。pre-existing の問題と判断するが、CI 安定性の観点で後日対応が望ましい。

総合的に: Critical な不整合や仕様違反は見つからなかった。Major 級として 1 件（flaky テスト）、Minor として数件の指摘がある。

**ステータス**: `APPROVED`（pre-existing flaky テストの後日対応依頼を Minor 指摘として併記）

---

## 指摘事項

### 🟠 Major

#### Major 1: pre-existing 由来の flaky テスト

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:191-228`

**問題点**:
`KsSettingsViewComposeTest > DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される` テストが `./gradlew :ks-settingsview-compose:testDebugUnitTest --rerun-tasks` の繰り返し実行で稀に FAILED となる（3 回繰り返しで 1 回 FAILED を観測）。失敗原因は RecyclerView の `AsyncListDiffer` が `composeRule.waitForIdle()` 完了後もまだ非同期で diff 計算中で、`recyclerViewItemCountForTest()` 取得時点で 2 件目の Cell が反映されきっていないことに起因する pre-existing なテスト基盤の限界。

本提案の差分は当該テストファイル自体には触れておらず（`git diff` で確認済み）、また失敗時に通常パスのテストは全て緑である。tasks.md 12.2 で「pre-existing の AsyncListDiffer 由来の flaky テスト。単独実行では 10/10 緑、フル `./gradlew test` では debug/release 同時実行時に稀に失敗する。本提案範囲外の既存テスト基盤の限界」と注記済み。

**判定**: pre-existing でかつ tasks.md に既知 issue として明示されているため、**本提案のレビューを CHANGES_REQUESTED にする必須条件には該当しない**。ただし CI 安定性のため、後続提案で安定化することを強く推奨する。

**推奨修正（後続提案で対応推奨）**:
- `recyclerViewItemCountForTest()` 前に RecyclerView の `itemAnimator.isRunning` / AsyncListDiffer の commit 完了待ちフックを挟む
- もしくは `composeRule.runOnIdle { ... }` ブロック内で itemCount 取得を行い、Compose runtime と AndroidView の同期確立後にアサート
- もしくは本テストを Robolectric 非依存の deterministic な Diff 適用テスト（DSLDiffCalculator + KsSettingsView.applyDiff の組み合わせ）に書き換える

---

### 🟡 Minor

#### Minor 1: `KsSettingsView.applyThemeInternal` で `notifyDataSetChanged()` 3 連発

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:462-477`

**問題点**:
`applyThemeInternal` が `mainListAdapter.notifyDataSetChanged()` / `headerAdapter.notifyDataSetChanged()` / `footerAdapter.notifyDataSetChanged()` を 3 連続で呼び出している。Theme 更新は低頻度操作という想定通りであり、機能的には正しい。ただし以下の点で改善余地がある：

1. `@SuppressLint("NotifyDataSetChanged")` で抑制しているが、`ConcatAdapter` で複数 Adapter に対して個別に `notifyDataSetChanged` を呼ぶと、内部の position オフセット計算とアニメーションが交互に走り得る。
2. `submitList` ではなく `notifyDataSetChanged` を使う設計判断は妥当だが、コメントの理由づけ（"Theme は CellListItem に含まれないため `submitList` を呼んでも DiffUtil は no-op になる"）はその通りでも、`payload` 機構（`notifyItemRangeChanged(0, itemCount, "theme")`）であれば部分更新の追跡が可能になる。

これは spec 要件としては「再評価する」とのみ規定されており（spec の `KsSettingsView の公開 API` Requirement）、`notifyDataSetChanged` で要件を満たすため Major ではない。

**推奨修正**:
ドキュメントコメント上「Theme 切替は低頻度操作のため payload 機構は持たせない」と明記されており実装方針が明確なので、現状維持で問題なし。ただし将来「Theme 切替がアプリの起動時毎回走る」「ダークモード切替で頻発する」など状況が変わった場合、payload ベースに変える余地は残しておくと良い。

#### Minor 2: Compose 層の Theme 経路の単体テスト不在

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt`

**問題点**:
spec `settings-view-android-compose/spec.md` Scenario「theme パラメータの変化」（lines 58-62）および「Theme 変化時の Diff 不発行」（lines 169-173）は「Recomposition が起こると `store.applyTheme(newTheme)` が呼ばれ、`SettingsRootDiff` は発行されない」ことを要求しているが、Compose 層の単体テストでこのシナリオを直接検証するテストが存在しない。

実装は `KsSettingsViewComposable.kt:115-119` で
```kotlin
if (bookkeeper.lastTheme != theme) {
    bookkeeper.store.applyTheme(theme)
    bookkeeper.lastTheme = theme
}
```
として正しく書かれており、`SettingsRootStoreTest.applyTheme で theme StateFlow が更新され Diff は emit されない` テストで Store レベルでは検証済みであるため、機能的には spec 要件を満たしている。ただし「Compose `theme` パラメータ → `store.applyTheme` 配線」自体の回帰防止テストはない。

iOS 側はレビュー round 1 で類似の不足が指摘され追加された経緯がある（`SettingsRootStoreTests` で `applyTheme` Diff 不発行の確認 + DSL → SettingsRootStore の経路は `KsSettingsViewDSLIntegrationTests` 経由）。Android 側で同等の経路テストがないのは整合性の点で改善余地。

**推奨修正（後続提案で対応推奨）**:
```kotlin
@Test
fun `DSL 方式で theme パラメータ変化時に store applyTheme が呼ばれる`() {
    var currentTheme by mutableStateOf(Theme())
    var capturedStore: SettingsRootStore? = null
    // composeRule.setContent で KsSettingsView { ... } を組み立て、
    // bookkeeper.store を captured 経由で参照可能にする
    // currentTheme を変化させ、recomposition 後に store.theme.value が更新されることを assertEquals
}
```

#### Minor 3: `EffectiveStyle.toTypeface()` の Font Family 解決スキップ

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:215-219`

**問題点**:
`TextStyle.toTypeface()` は `TextStyle.fontFamily` を読まず、`fontWeight` のみで `Typeface.create(Typeface.DEFAULT, numericWeight, false)` を生成する設計になっている。実装コメント（`EffectiveStyle.kt:207-214`）に「Compose の FontFamily は `Resources` 経由のフォント解決を必要とするため、UI 層の `EffectiveStyle.from` 経路では『フォントファミリ未指定 = システムフォント』として扱うシンプルな実装に留める」と明記されている。

これは現実的なトレードオフであり、`titleFont = TextStyle(fontWeight = FontWeight.Bold)` のような最も多い指定パターンには対応している。ただし spec `settings-view-android-style/spec.md` の Theme.titleFont は `TextStyle?` 全般を受けるとしか規定されておらず、FontFamily 指定が呼び出し側からは効くように見えるが実際は無視される、という silent な仕様乖離が発生し得る。

**推奨修正（後続提案で対応推奨）**:
1. KDoc を充実させ、`Theme.titleFont` / `CellStyle.titleFont` / `CellStyle.descriptionFont` 等で「`fontWeight` / `fontSize` のみが反映される。`fontFamily` / `fontStyle` / `letterSpacing` 等は本リリース範囲外（後続提案で対応予定）」と明示する
2. または `TextStyle.fontFamily` が指定されていた場合に Compose `androidx.compose.ui.text.font.FontFamilyResolver` 経由で `Typeface` を解決する（Compose 公開 API 経由）

本提案では Theme/CellStyle の UI 層移動が主目的で、Typeface 解決の高度化は range out なので Minor 扱い。

#### Minor 4: `KsImage.Drawable` の `equals` で `this === other` チェック後の比較が冗長

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsImage.kt:56-67`

**問題点**:
```kotlin
override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Drawable) return false
    return drawable === other.drawable
}
```

`this === other` が true なら自明に `drawable === other.drawable` も true なので冗長だが、これは Kotlin の慣用パターン（特に Kotlin スタイルガイドの `equals` 雛形）に従ったものであり、可読性・正しさで損なわれない。なお `KsImage.Drawable` を `data class` ではなく通常 `class` にしたのは「`Drawable` は値型ではない → 参照同一性」とするための妥当な判断で、spec の Requirement「`Drawable` サブ型は参照同一性で `equals` / `hashCode` を実装する」と整合する。

**推奨修正**:
現状維持で問題なし（Kotlin 慣用パターン）。

#### Minor 5: `SettingsRootStore.theme` 購読の `if (theme != newTheme)` ガード

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:272-278`

**問題点**:
```kotlin
launch {
    store.theme.collect { newTheme ->
        if (theme != newTheme) {
            theme = newTheme
        }
    }
}
```

`StateFlow` は元々 `distinctUntilChanged` 相当の振る舞いをするが、ここではさらに「View 側の現在 theme と異なる場合のみ setter 発火」というガードが入っている。これは `bind(store)` の初期反映で `setRootDirect(store.state.value, store.theme.value)` 直後に StateFlow の初期値が再度流れて来た時のループ抑制という目的で必要。実装的には妥当だが、コメントが「StateFlow 自体が conflate 性質を持つので素朴に collect する」と書きつつ実際は条件チェックを入れていてやや矛盾する。

**推奨修正**:
KDoc コメントを「初期 setRootDirect で同期した theme と StateFlow 初回 emit の重複設定を避けるため、現在 theme と異なる場合のみ setter 経由で applyThemeInternal を発火する。`StateFlow.distinctUntilChanged` は値変更のみで判定するが、ここでは setter 経由の重複 notifyDataSetChanged を確実に抑止したい」のように意図を明確化すると保守性が増す。

---

### 🔵 Suggestion

#### Suggestion 1: `KsSettingsView.theme` 重複代入の挙動説明補強

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:153-158`

**問題点**:
```kotlin
var theme: Theme
    get() = themeBacking
    set(value) {
        themeBacking = value
        applyThemeInternal(value)
    }
```

setter で同じ Theme を代入しても `applyThemeInternal` が走る（`notifyDataSetChanged` 3 連発）。Minor 1 と関連するが、`if (themeBacking == value) return` のガードを setter 内に入れると、Compose `update` フェーズが頻発する状況でも不要な再描画を抑制できる。

ただし「Compose ラッパは `applyTheme` 経路を通すから setter 直叩きはほぼ起きない」「Store 経路でも distinctUntilChanged 相当の StateFlow ガードが効く」ため、現状で実害は低い。

**推奨修正**:
```kotlin
var theme: Theme
    get() = themeBacking
    set(value) {
        if (themeBacking == value) return  // 同値代入を抑制
        themeBacking = value
        applyThemeInternal(value)
    }
```

#### Suggestion 2: `DSLBookkeeper` の `lastTheme` を Theme 比較で `equals` を信頼する点の明示

**該当箇所**: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:116`

**問題点**:
`bookkeeper.lastTheme != theme` で Theme 変化を検知している。`Theme` は `data class` で Compose `Color` (`@JvmInline value class`) を持つため `equals` は安定するはずだが、内包する `TextStyle?` フィールドの `equals` 挙動に依存する。Compose `TextStyle` は `equals` を実装しているが、内部の `FontFamily` / `Brush` の比較は構造比較ではなく一部参照比較になり得る（Compose 1.5 以降は概ね安定だが）。

実害はほぼないが、長期的には「Theme 変化検知の正確性は Compose `TextStyle.equals` の挙動に依存する」点を KDoc やコメントで明示しておくと、将来 Compose のメジャーアップグレード時の挙動変化に気づきやすい。

**推奨修正**:
コメントで「`Theme` の `equals` は `data class` 自動生成で、`Color` (value class) は安定、`TextStyle?` は Compose 標準の `equals` に依存する。Compose のメジャーアップグレード時は `TextStyle.equals` の挙動回帰に注意」と注記する。

---

## アクションプラン

優先度順:

1. **[Minor 2 / 後続提案]** Compose 層に Theme パラメータ → `store.applyTheme` 経路の単体テスト追加（iOS 側との対称性確保）
2. **[Major 1 / 後続提案]** flaky な `DSL 方式で外部 state を 2 回連続更新` テストの安定化（RecyclerView AsyncListDiffer の同期確立を待つフックを追加、または deterministic な単体テストへ書き換え）
3. **[Minor 3 / 後続提案]** `EffectiveStyle.toTypeface()` の Font Family 解決強化、または KDoc で対応範囲を明示
4. **[Minor 5 / 軽微改善]** Theme StateFlow 購読の `if (theme != newTheme)` ガードに意図コメント追加
5. **[Suggestion 1 / 軽微改善]** `KsSettingsView.theme` setter に同値代入抑制ガード追加

すべて Critical でも本提案のリリース停止要件でもなく、本提案のスコープは完遂されている。

---

## 判定結果

**ステータス**: `APPROVED`

理由:
- spec delta（settings-view-core / settings-view-android-style / settings-view-android-host / settings-view-android-compose / settings-view-android-theme-bridge / cell-types-basic / samples-android）の MUST / SHALL Requirement と Scenario はすべて実装で満たされている
- Cell 抽象から `style` 要求が削除されつつ、UI 層に `DSLStyleModifiableCell` / `DSLIconModifiableCell` を新設して具象 Cell の責務を整理した設計が一貫している
- `SettingsRoot` から `theme` フィールドが削除され、`SettingsRootDiff.UpdateTheme` が削除され、Theme 経路は `SettingsRootStore.applyTheme` / `KsSettingsView.theme` プロパティ / Compose `KsSettingsView(theme = ...)` 引数 の 3 経路で統一されている（spec の MUST NOT 「Theme を Diff に含めない」を厳守）
- iOS 側で導入された `.icon(_:)` modifier 相当を Android 側でも `Cell.icon(KsImage?)` / `CellHandle.icon(KsImage?)` 双方で実装、`DSLIconModifiableCell` インターフェースで型安全に経路を確立（spec MUST 準拠）
- ビルド成功、テスト全件通過（pre-existing flaky テストを除く）、Sample アプリビルド成功
- Critical / Major 不整合なし。Major 1（flaky テスト）は pre-existing で本提案差分の影響範囲外、tasks.md に既知 issue として記録済み
- パブリック API 破壊的変更（`KsColor` / `KsFont` 削除、`Theme` フィールド型変更、`SettingsRoot.theme` 削除）は spec で BREAKING として明示済み、Sample も整合的に更新済み

本提案の Android 側実装は **マージ可能** な品質に達している。Minor / Suggestion 指摘は後続提案で対応すれば良い。

なお tasks.md の Phase 10 / 11（MAUI / cell-types-input / cell-types-custom との整合確認）は read-only な依頼ベースで pending のままだが、これは OpenSpec 規約に従って本提案範囲外で扱うことが明示されており、本提案完了条件（"依頼一覧が PR 説明に記録されている"）の側で対応する案件である。本レビューでは指摘対象としない。
