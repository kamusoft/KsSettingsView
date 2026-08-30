# レビュー結果 - add-settings-view-android-ui

**レビュー日時**: 2026年05月10日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-android-ui

## サマリー

`add-settings-view-android-ui` は OpenSpec で定義された Android UI 基盤（`ks-settingsview-ui` / `ks-settingsview-compose`）を実装した変更提案である。`RecyclerView + ListAdapter + ConcatAdapter(headerAdapter, mainListAdapter, footerAdapter)` という Decision 1 / Decision 5d の構造、`SectionAccessory` / `RootAccessory` の Text / View（Compose / AndroidView）両ケース対応、`Classic` / `Modern` の `ItemDecoration` 切替、`KsCellRegistry` 中央レジストリ、`ComposeCellViewHolder`（`DisposeOnDetachedFromWindow` 強制）、`settingsRoot { ... }` DSL など、要件はおおむね正確に実装されている。

ビルドおよびテストは全て成功（core 47 + ui 35 + compose 7 = 89 件、failures=0、errors=0）。tasks.md の全 52 タスクが実装ファイルの存在と挙動レベルで確認できる。

ただし、spec.md の Requirement「Cell レジストリ」配下の Scenario「未登録 Cell の扱い」に関し、**リリースビルドで空のプレースホルダ ViewHolder を返す**フォールバック挙動が実装されておらず、`KsCellRegistry.viewTypeOf` / `createViewHolder` 共に常に `IllegalStateException` をスローする実装になっている点が仕様と乖離している。Major 指摘として扱う。

その他、`CellListItem` が spec で「`sealed class`」と表現されているのに対し実装は `sealed interface` である（Kotlin 上はほぼ等価だが、用語の整合性）こと、`KsFont` 変換が `weight` を 4 値→2 値に縮退しているが Android 28+ なら `Typeface.create(Typeface, weight, italic)` でより忠実な表現が可能であること、などの Minor / Suggestion 指摘がある。

**判定**: `CHANGES_REQUESTED`

## 指摘事項

### 🟠 Major: 未登録 Cell のリリースビルド向けフォールバックが未実装（仕様乖離）

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt:121,132`

**問題点**:
spec.md「Cell レジストリ」Requirement の Scenario「未登録 Cell の扱い」は次のように規定している。

> - **GIVEN** `KsCellRegistry` に未登録の Cell が submit される
> - **WHEN** ListAdapter が描画を試みる
> - **THEN** **デバッグビルドでは `IllegalStateException` をスロー、リリースビルドでは空のプレースホルダ ViewHolder を返してアプリクラッシュを防ぐ**

しかし実装は `viewTypeOf` / `createViewHolder` 共に無条件で `throw IllegalStateException(...)` するのみで、リリースビルド時のプレースホルダ ViewHolder 経路が存在しない。本変更提案は MAUI バインディング層（`add-maui-bindings`）からも利用される計画があるため、リリースビルドで未登録 Cell が混入した際のクラッシュ耐性は本仕様で意図的に要求されている安全網と読める。

`KsCellRegistryTest.kt` 側も `assertThrows(IllegalStateException::class.java)` 一辺倒で、リリース時フォールバックの挙動を検証していない。

**推奨修正**:
1. `BuildConfig.DEBUG`（`buildFeatures.buildConfig = false` を有効に戻すか、本目的のために `applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE` を読む）または明示的な `KsCellRegistry.strictMode: Boolean` フラグを導入。
2. リリース／非 strict 時は空 `View(parent.context)` を `RecyclerView.ViewHolder` でラップした `EmptyPlaceholderViewHolder` を返す。
3. 仕様 Scenario を裏付ける「リリースビルド相当（strict=false）でプレースホルダが返る」テストを `KsCellRegistryTest` に追加。

```kotlin
// KsCellRegistry.kt（推奨形のスケッチ）
@Volatile
var strictMode: Boolean = true // デバッグビルドでは true、リリースで false に設定する想定

fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val entry = entriesByViewType[viewType]
    if (entry != null) return entry.factory(parent)
    if (strictMode) {
        throw IllegalStateException("viewType $viewType is not registered in KsCellRegistry")
    }
    return EmptyPlaceholderViewHolder.create(parent)
}

internal class EmptyPlaceholderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    companion object {
        fun create(parent: ViewGroup): EmptyPlaceholderViewHolder {
            val v = View(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0,
                )
            }
            return EmptyPlaceholderViewHolder(v)
        }
    }
}
```

なお、本指摘は openspec/changes/add-settings-view-android-ui/specs/settings-view-android-ui/spec.md に明記された Scenario への適合を要求するものであり、禁止事項（仕様書き換え指示）には該当しない。

---

### 🟡 Minor: `CellListItem` が spec の「`sealed class`」表現と異なり `sealed interface` で実装されている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellListItem.kt:18`

**問題点**:
- proposal.md と spec.md の Scenario「CellListItem の sealed 階層」 / Requirement「RecyclerView と Adapter 構成」では一貫して **`sealed class`** と表現されている。
- tasks.md 3.1 も「`sealed class CellListItem`（`SectionHeader`、`Cell`、`SectionFooter`）」と明示。
- 実装は `internal sealed interface CellListItem`（`SectionHeader` / `CellRow` / `SectionFooter`）であり、subtype 名も spec 上の "Cell" → 実装 "CellRow" にリネームされている。

Kotlin の `sealed interface` と `sealed class` はこのドメインでの差は実質ないが、用語と subtype 名の不一致は仕様 ↔ 実装トレースを取りにくくする。subtype 名の `Cell` → `CellRow` は、ドメインモデル `Cell` 型との衝突回避という妥当な意図に見えるため、**spec 側の subtype 名を `CellRow` に揃えるのか、実装を `Cell` に戻すのか**を確定したい。

**推奨修正**:
A. 実装を spec に合わせる：`sealed class CellListItem` に変更し、subtype を `Cell` にリネーム。`Cell` 型との衝突は完全修飾名（`jp.kamusoft.kssettingsview.core.Cell`）で解決。
B. spec を実装に合わせる：spec / proposal / tasks の表現を `sealed interface` / `CellRow` に揃える（**禁止事項に抵触するため、orchestrator 経由で別 OpenSpec change を立てるのが筋**）。

最低でも、本変更提案 archive 前に「実装と spec の乖離を残置するなら理由を design.md または review-result に明記」する形で記録すべき。

---

### 🟡 Minor: `KsFont` の `weight` 4 値が 2 値（NORMAL / BOLD）に縮退している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsFontExt.kt:35-40`

**問題点**:
`KsFontWeight` は `REGULAR / MEDIUM / SEMIBOLD / BOLD` の 4 値で定義されているが、変換は次のように 2 値（NORMAL / BOLD）に丸めている：

```kotlin
val style = when (weight) {
    KsFontWeight.REGULAR -> Typeface.NORMAL
    KsFontWeight.MEDIUM -> Typeface.NORMAL   // ← 縮退
    KsFontWeight.SEMIBOLD -> Typeface.BOLD   // ← 縮退
    KsFontWeight.BOLD -> Typeface.BOLD
}
```

Android Pie (API 28) 以降では `Typeface.create(Typeface family, int weight, boolean italic)` が利用可能で、本プロジェクトは `minSdk = 29` のため、最初から weight 値（400 / 500 / 600 / 700）を直接指定すれば論理ウェイトと描画結果が一致する。`MEDIUM` を `NORMAL` に丸めるのは品質上もったいない。

**推奨修正**:

```kotlin
internal fun KsFont.toTypeface(context: Context): Typeface {
    val baseTypeface = if (family != null) {
        Typeface.create(family, Typeface.NORMAL)
    } else {
        Typeface.DEFAULT
    }
    val numericWeight = when (weight) {
        KsFontWeight.REGULAR -> 400
        KsFontWeight.MEDIUM -> 500
        KsFontWeight.SEMIBOLD -> 600
        KsFontWeight.BOLD -> 700
    }
    return Typeface.create(baseTypeface, numericWeight, /* italic = */ false)
}
```

`KsFontExtTest`（現状未作成）または `EffectiveStyleTest` に「MEDIUM / SEMIBOLD が NORMAL / BOLD と異なる Typeface を生成する」検証を追加する。

---

### 🟡 Minor: `ClassicSectionDecoration` の区切り線色 alpha チャンネル丸め誤差

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsColorExt.kt:18-23`

**問題点**:
`KsColor.toColorInt` は `(value * 255.0).toInt()` で丸めているため、`0.5 → 127`（切り捨て）になる。`EffectiveStyleTest` でも `assertEquals(127, Color.green(argb))` という形で実装方針を許容しているが、spec.md の Scenario「KsColor から ColorInt」は

> `KsColor(red = 1.0, green = 0.5, blue = 0.0, alpha = 1.0)` → `0xFFFF8000`

を要求している。`0x80 = 128` であり、**実装の切り捨て丸めだと 0x7F=127 が返る** ため、spec 例示値「`0xFFFF8000`」とは厳密には不一致になる。テスト側はこれを 127 で許容しており、結果的に「テストは通るが spec 例示と齟齬」という状態。

`Math.round(value * 255.0).toInt()` または `((value * 255.0) + 0.5).toInt()`（ただし負値考慮不要なので前者で十分）に変更すれば、`0.5 → 128` で spec 例示と一致する。

**推奨修正**:

```kotlin
@ColorInt
internal fun KsColor.toColorInt(): Int {
    fun comp(v: Double): Int = ((v.coerceIn(0.0, 1.0) * 255.0) + 0.5).toInt()
    return Color.argb(comp(alpha), comp(red), comp(green), comp(blue))
}
```

`EffectiveStyleTest` の該当 assertion を `assertEquals(0x80, Color.green(argb))` に更新する。

---

### 🟡 Minor: `SectionAnyViewAccessoryViewHolder.bind` で毎回 `ComposeView` を新規生成しており再利用効率が悪い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:86,198-225`

**問題点**:
`bindKsAnyView` は `container.removeAllViews()` してから新しい `ComposeView` を作って addView する実装になっている。これは spec の Scenario「View 形式ヘッダの中身更新（差分検出非対応）」を機能的に満たすが、

1. 既存 ComposeView を残して `setContent` だけ更新できれば `Composition` の cancel / re-create コストが無駄に発生しない（ただし `KsAnyView.Compose.content` は別ラムダなので `setContent` で十分捌ける）
2. ViewHolder reuse 時に新しい ComposeView を毎回 attach することで、`DisposeOnDetachedFromWindow` 戦略が「detach されていない既存 ComposeView の再 setContent」より重い経路になる
3. AndroidView ケースでも factory が毎 bind で呼ばれて新規 View インスタンスを生成するため、毎フレームの GC 圧が増える

**推奨修正**（任意）：
- 直前の `KsAnyView` 種別が同じ（Compose↔Compose / AndroidView↔AndroidView）なら、既存 child を再利用して `setContent` または factory 結果の差分のみ反映する分岐を追加。
- 異なる種別への切替時のみ removeAllViews + addView。

ただし Phase 1 ではパフォーマンスより実装簡素化を優先しているため Suggestion 寄り。本指摘は性能観点で将来の改善候補として記録する程度で良い。

---

### 🔵 Suggestion: `KsCellRegistry` のテスト clear が他テストに副作用を残す可能性

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryTest.kt:31-34`

**問題点**:
`KsCellRegistry` は `object`（プロセス全体で 1 個のシングルトン）であり、`KsCellRegistryTest` は `@After tearDown` で `clear()` しているが、他のテストクラス（`KsSettingsViewTest` 等）は `KsSettingsView.init { ... }` 経由でレジストリに `PocLabelCell` を登録する。テスト実行順序によっては `KsCellRegistryTest` 実行後に `PocLabelCell` 登録が消えた状態で次のテストが走るが、`KsSettingsView` を再 new すれば init で再登録されるためたまたま回復している。

**推奨修正**（任意）：
- `KsSettingsView.init` の登録処理を冪等にしている（`isRegistered` チェック）こと自体は良い。明示的な「テスト隔離契約」を `KsCellRegistry` クラスドキュメントに追記する、または `KsCellRegistry` を DI 可能にして KsSettingsView コンストラクタで受け取る形にすると、テスト容易性 / DI 観点で更に堅牢になる。
- ただし、Decision 3（中央レジストリ + シングルトン）の設計判断と直交するため、現状維持でも問題なし。

---

### 🔵 Suggestion: `RootHeaderFooterAdapter` の getItemViewType の `null` フォールバック

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/RootHeaderFooterAdapter.kt:73-79`

**問題点**:
`view == null` の時に `KsCellRegistry.VIEW_TYPE_ROOT_TEXT` を返す防御コードがあるが、`view == null` のときは `getItemCount() == 0` なので RecyclerView が `getItemViewType` を呼ぶ経路は存在しない（`require(position == 0)` の前に `getItemCount` での弾きが入る）。`null` 時のフォールバック値を返すより `error("...")` で早期検知した方が、契約違反時のデバッグが楽。

**推奨修正**（任意）：
```kotlin
override fun getItemViewType(position: Int): Int {
    require(position == 0) { "RootHeaderFooterAdapter only supports position 0" }
    return when (val v = view) {
        is RootAccessory.Text -> KsCellRegistry.VIEW_TYPE_ROOT_TEXT
        is RootAccessory.View -> KsCellRegistry.VIEW_TYPE_ROOT_VIEW
        null -> error("getItemViewType called while view == null (itemCount should be 0)")
    }
}
```

---

### 🔵 Suggestion: `KsSettingsViewComposeTest` がスモークテストに留まり Compose 経路の bind 結果を検証していない

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:43-67`

**問題点**:
spec Scenario「Compose からの利用」「状態変更時の再 Composition」を仕様として持つが、テストは「Composable がコンパイルし、引数を受け取れる」スモークまで。tasks.md 10.1 の実装メモにも記載があるとおり、`androidx.compose.ui.test:ui-test-junit4` + Robolectric バックエンドが導入できれば `composeTestRule.setContent { KsSettingsView(...) }` で実 bind 結果の検証が可能。

`add-samples-android` で目視確認する方針自体は受け入れ可能だが、Phase 1 で「state 反映」の自動化された回帰テストを残せると堅牢。

**推奨修正**（任意）：
- `androidx.compose.ui.test.junit4:1.7.5` + `compose.ui:ui-test-manifest` を `testImplementation` に追加。
- `createComposeRule()` を使い、`onChange` callback を仕込んで再 Composition で `view.root` が更新されることを検証する。

ただし本指摘は実装メモで明示的に「サンプル側で確認する」と記載済みのため、Suggestion 留め。

## アクションプラン

優先度順：

1. **【必須・Major】未登録 Cell のリリースビルド向けプレースホルダ ViewHolder フォールバックを実装**し、対応するテストケースを追加する（spec.md「未登録 Cell の扱い」Scenario）。
2. **【推奨・Minor】`CellListItem` の `sealed class` / `sealed interface` および subtype 名（`Cell` / `CellRow`）の整合**を取る。実装に合わせて spec を更新する場合は、本変更提案 archive 前に別 OpenSpec change で同期。
3. **【推奨・Minor】`KsColor.toColorInt` の丸めを切り捨て→四捨五入に修正**し、spec.md 例示値（`0.5 → 0x80`）と整合させる。
4. **【推奨・Minor】`KsFont.toTypeface` の weight 縮退を解消**し、minSdk 29 の前提で `Typeface.create(base, numericWeight, false)` を使う。
5. **【任意・Suggestion】`SectionAnyViewAccessoryViewHolder` の ComposeView/factory 再利用パスを検討**（パフォーマンス改善）。
6. **【任意・Suggestion】`KsSettingsViewComposeTest` を Compose UI Test 化**し、state 反映を自動検証する。

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

**理由**:
- spec.md「未登録 Cell の扱い」Scenario のリリースビルド向けフォールバックが未実装であり、これは Major 級の仕様乖離である。
- Minor 指摘（`sealed class` / 丸め誤差 / weight 縮退）も spec 例示・記述との整合性に関わるため、archive 前に解消することを推奨する。

ビルド・テストは全て成功（89 件、failures=0、errors=0）しているため、上記対応のみ完了すれば再レビュー → APPROVED 化が可能と判断する。
