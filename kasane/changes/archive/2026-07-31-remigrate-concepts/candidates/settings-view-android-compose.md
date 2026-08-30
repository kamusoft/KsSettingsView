# Candidate: settings-view-android-compose

## 概念候補

### Android Compose Bridge と宣言 DSL (提案カテゴリ: platforms/)

`ks-settingsview-compose` は、Jetpack Compose の状態を Android Native Host の `jp.kamusoft.kssettingsview.ui.KsSettingsView` へ接続する公開ラッパ層である。Compose 専用のリスト描画基盤は持たず、`AndroidView` に FrameLayout 派生 Host を埋め込み、`SettingsRootStore` と Host の既存更新経路を再利用する。

#### 目的と責務境界

- 一般的な設定画面には宣言 DSL 方式、大量データや命令型の部分操作には Store 方式を公開する。
- Compose 側は宣言ツリーの構築、Recomposition をまたぐ内部 Store の保持、値と callback の橋渡しを担う。
- `RecyclerView`、visible projection、Cell の描画、Theme / `CellStyle` の実効値解決は `ks-settingsview-ui` が担う。Compose ラッパで再実装しない。
- `Theme` は `SettingsRoot` に含めず、`SettingsRootStore.applyTheme` の独立経路へ渡す。

#### 公開 API

入口は同名の `@Composable fun KsSettingsView(...)` 2 overload である。

| 方式 | Store の所有者 | 主な入口 | 向く用途 |
|---|---|---|---|
| DSL | ラッパ内部。Composition の identity が続く間 `remember` で保持 | `KsSettingsView(theme = ...) { Section { ... } }` | 一般的な静的・中規模画面 |
| Store | 利用者 | `KsSettingsView(store = store, ...)` | 大量データ、高頻度更新、命令型操作 |

両方式とも `modifier`、`style`、任意 Composable の `rootHeader` / `rootFooter` を受ける。Store 方式の Theme は `store.theme`、DSL 方式の Theme は `theme: Theme = Theme()` が入口である。`style` の既定値は `KsSettingsViewStyle.Classic`。

`settingsRoot { section(id = ...) { cell(...) } }` は Store 初期値用の純粋な `SettingsRoot` builder として別に残る。これは Recomposition 対応の `DSLSettingsRootScope` ではなく、明示 ID を受ける `SettingsRootScope` を使う。

#### 利用例

```kotlin
@Composable
fun SettingsScreen() {
    val enabled = remember { mutableStateOf(true) }

    KsSettingsView(
        style = KsSettingsViewStyle.Classic,
        theme = Theme(),
        rootHeader = { Text("プロフィール") },
    ) {
        Section(header = "通知") {
            SwitchCell(title = "プッシュ通知", isOn = enabled)
            LabelCell(title = "バージョン", valueText = "1.0.0")
                .cellID("app-version")
        }.sectionFooter("端末の通知設定も確認してください")
    }
}
```

Store 方式では Store 自体を `remember` し、公開操作を呼ぶ。

```kotlin
val store = remember {
    SettingsRootStore(
        initialRoot = settingsRoot {
            section(id = "general", header = "一般") {
                cell(LabelCell(id = "version", title = "バージョン"))
            }
        },
        initialTheme = Theme(),
    )
}

KsSettingsView(store = store)
store.insertCell(newCell, sectionId = "general", at = 1)
```

#### DSL の公開契約

- `Section(...)` は文字列または任意 Composable の Header / Footer、`headerHeight`、`isVisible` と Cell 列を受け、`SectionHandle` を返す。文字列と Composable の同じ位置への同時指定は `IllegalArgumentException` になる。
- `DSLSectionScope` には基本 Cell 7 種と入力 Cell 5 種を直置きする拡張関数があり、いずれも `CellHandle` を返す。外部から受け取った `Cell` は `cell(cell)` または `+cell` で流せる。
- `SectionHandle` は `sectionHeader` / `sectionFooter` / `sectionID`、`CellHandle` は `font(TextStyle)` / `cellHeight(Dp)` / `titleColor(Color)` / `backgroundColor(Color)` / `icon(KsImage?)` / `cellID` を chain できる。同じ modifier は `Cell` 値型にも copy を返す拡張として存在する。
- 組み込み Cell 12 種は `DSLStyleModifiableCell` と `DSLIconModifiableCell` に準拠する。利用者定義 Cell で style、icon、DSL ID の再束縛を有効にするには、それぞれ対応 interface と copy API を実装する。
- `disabled(Boolean)` は現行では `CellHandle` 版・`Cell` 版とも no-op である。無効化には各 Cell initializer の `isEnabled` を使う。
- `forEach(items, key = ...)` は Root と Section の両 scope にあり、`KsIdentifiable` 実装型では key lambda を省略できる。
- TwoWay helper は Compose の `MutableState` 自体を Cell に保持せず、評価時点の `state.value` を Cell 値へ写し、Cell callback から `state.value` へ書き戻す。`SwitchCell` の `MutableState<Boolean>` overload、入力 Cell 5 種の `MutableState` overload が代表例である。基本 Cell の `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は値と callback を受ける。

#### 保証すること

- `AndroidView.factory` で Native Host を生成して `bind(store)` し、`AndroidView.update` で `style`、Root H/F と DSL の再評価結果を反映する。
- DSL 方式の内部 Store と前回 resolved tree は、Composition の identity が続く間維持する。外部 state の連続変更でも `AndroidView.update` が再評価され、2 回目以降を取りこぼさない。
- 初回 Theme は内部 Store の `initialTheme` に渡し、以後は前回と異なる Theme だけを `applyTheme` する。同値 Theme の再適用で不要な更新を生じさせない。
- 同一 ID の内容変更では構造 Diff を発行せず、`contentUpdates` から `store.replaceCells` の ViewHolder 部分更新経路へ渡す。
- `isVisible` が変化した場合は他の構造・内容差分を混ぜず `SettingsRootDiff.Full(newRoot)` 一件へ切り替え、`contentUpdates` を空にする。

#### してはいけないこと

- Store 方式と DSL 方式に別の Native 描画基盤を持たせない。
- Root H/F、Theme、`CellStyle` を Core の `SettingsRoot` に戻さない。
- `.disabled(true)` を機能する公開無効化 API として案内しない。
- Compose state object を Cell の永続状態として保持させない。Cell には評価時点の値と書き戻し callback を渡す。
- `settingsRoot` の純粋 builder と Recomposition 用 `KsSettingsView { ... }` DSL を同じ scope として説明しない。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` / `SettingsRootScope.kt` / `DSLScope.kt` / `DSLHandles.kt` / `BasicCellDsl.kt` / `InputCellDsl.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt` / `BasicCellDslTest.kt` / `InputCellDslTest.kt` / `DSLHandleTest.kt`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt` / `InputCellsDemoScreen.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose。

### 宣言ツリーの identity (提案カテゴリ: architecture/、Batch D で iOS 候補と統合)

Compose は Recomposition のたびに Section / Cell の値を再生成する。`DSLRootTree` は一時インスタンスの既定 UUID ではなく `DSLIdentityHint` から決定的な String ID を生成し、前回と同じ意味の要素を追跡する。

#### identity hint と現行優先順位

現行 Android 実装の実効優先順位は次の通りである。

1. `.sectionID(...)` / `.cellID(...)` の `Explicit`
2. `forEach` の key または `KsIdentifiable.id`
3. 静的 Section: `(root position, header text)`、Header がなければ root position
4. 静的 Cell: `(resolved Section ID, position in Section, Cell type)`

`forEach` は content が付けた既存 hint を上書きしないため、明示 ID と key を併用した場合は明示 ID が勝つ。ただし ADR-0008、旧 spec、docs は key を第一優先としており現行コードと一致しない。利用者向け契約を確定するまでは、一つの要素で key と明示 ID を併用しない。

`sectionID("x")` / `cellID("x")` の引数は最終 `Section.id` / `Cell.id` そのものではなく、32 桁の決定的 hash を導く hint である。同じ型と値の hint は同じ ID へ解決されるが、入力文字列と最終 ID の文字列一致に依存してはいけない。

一つの `forEach` item から同じ階層へ複数 Section / Cell を追加すると、すべてに同じ key hint が付き ID が衝突する。一 item は一 Section または一 Cell に対応させる。

利用者定義 Cell の最終 ID を DSL で再束縛するには `DSLReidentifiableCell` が必要である。非準拠 Cell は元の `Cell.id` を維持するため、利用者が安定性を保証する。

#### 保証すること

- 同じ hint から Recomposition をまたいで同じ ID を生成する。
- ID の入力型も区別し、`123: Int` と `"123": String` を同じ hint とみなさない。
- title、選択値、`CellStyle` などの内容変更で identity を変えない。
- `forEach` の安定 key を用いた追加・並べ替えで既存要素の ID を維持する。

#### してはいけないこと

- 動的な挿入、削除、並べ替えを位置 fallback だけで追跡しない。
- 一つの `forEach` item から同階層へ複数要素を返さない。
- key と明示 ID の併用時の優先順位へ依存しない。
- 明示 ID の入力値と resolved ID の文字列が同じだと仮定しない。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DeclarativeDSLIdentity.kt` / `DSLNodes.kt` / `DSLScope.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DeclarativeDSLIdentityTest.kt` / `DSLIntegrationTest.kt` / `DSLHandleTest.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose、`kasane/decisions/0008-stable-declarative-tree-identity.md`。

### 宣言ツリーの構造・内容・可視性同期 (提案カテゴリ: architecture/、Batch D で iOS 候補と統合)

`DSLDiffCalculator` は再評価した resolved tree を、構造、内容、可視性、Theme の異なる経路へ分ける。これは Native Host の描画詳細ではなく、宣言ツリーを Store 更新へ変換する境界である。

| 変化 | 判定 | Android Compose での出力 |
|---|---|---|
| Section / Cell の追加・削除・移動 | ID 集合と順序 | `Insert` / `Remove` / `Move` の構造 Diff |
| Section / Root H/F のケースまたは Text 変更 | Accessory の等価性 | `UpdateAccessory` |
| 同一 ID Cell の内容変更 | Cell の値等価 | `contentUpdates` → `store.replaceCells` |
| Section / Cell の `isVisible` 変更 | 同一 ID 間の可視性比較 | `Full(newRoot)` 一件、内容更新なし |
| Theme 変更 | 前回 Theme との比較 | `store.applyTheme`。構造 Diff なし |

`SectionAccessory.View` / `RootAccessory.View` は中身の `KsAnyView` を等価性へ含めない。同じ View ケース同士では `UpdateAccessory` を発行せず、Composable 内容の更新は描画側へ委ねる。

#### 保証すること

- 構造同期は ID だけで判定し、内容等価を追加・削除・移動の判定へ使わない。
- 同一 ID の内容変更は Cell を別要素として扱わず、部分更新へ流す。
- 可視性変化を内容更新へ流さず、hidden を含む model から visible projection を再構築できる Full 経路へ切り替える。
- Theme を `SettingsRootDiff` に含めない。

#### してはいけないこと

- 同一 ID の内容変更に構造上の `ReplaceCell` を生成しない。
- 可視性変化と `contentUpdates` を同時に流さない。
- `KsAnyView` の lambda / factory 内容を値比較しようとしない。

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt` / `KsSettingsViewComposable.kt`、`android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculatorTest.kt` / `DSLIntegrationTest.kt` / `DSLVisibilityPreflightTest.kt` / `KsSettingsViewComposeTest.kt`、`openspec/specs/settings-view-android-compose/spec.md` Purpose、`kasane/decisions/0010-three-way-display-state-synchronization.md`。

## ADR 候補

- なし。Store / DSL の併存と収束は ADR-0007、stable identity は ADR-0008、構造・内容・可視性分離は ADR-0010 が既に accepted である。下記 drift は新 ADR を追加せず、既存決定と現行実装の照合対象とする。

## drift 所見

1. 旧 spec の DSL 方式シグネチャは `content: SettingsRootScope.() -> Unit` とするが、実装は `DSLSettingsRootScope.() -> Unit`。純粋 builder の `SettingsRootScope` とは別型である。(`openspec/specs/settings-view-android-compose/spec.md` / `KsSettingsViewComposable.kt` / `SettingsRootScope.kt`)
2. 旧 spec は `DSLSettingsRootScope` が既存 `section(id:, ...)` を維持するとするが、実装で明示 ID の小文字 `section` を持つのは純粋 builder の `SettingsRootScope` だけ。Recomposition DSL は大文字 `Section(...)` を使う。(`openspec/specs/settings-view-android-compose/spec.md` / `DSLScope.kt` / `SettingsRootScope.kt`)
3. ADR-0008・旧 spec・`docs/platform-guide-android.md` は `forEach key` を明示 ID より優先するとするが、実装は content が設定した `Explicit` hint を `forEach` が上書きしないため明示 ID が勝つ。(`kasane/decisions/0008-stable-declarative-tree-identity.md` / `openspec/specs/settings-view-android-compose/spec.md` / `docs/platform-guide-android.md` / `DSLScope.kt`)
4. 旧 spec は `.cellID("dynamic-cell-1")` / `.sectionID("dynamic-section-1")` により最終 ID がその文字列として固定されるとするが、実装は explicit 値を namespaced hash に変換して 32 桁 hex を最終 ID にする。テストも文字列一致ではなく再評価間の安定性だけを検証する。(`openspec/specs/settings-view-android-compose/spec.md` / `DeclarativeDSLIdentity.kt` / `DSLHandleTest.kt`)
5. 旧 spec 前半の state 変更 Scenario と Binding Scenario は Cell 内容変更で `ReplaceCell Diff` を発行するとする一方、現行 `DSLDiffCalculator.compute` は構造 Diff を空にし、`contentUpdates` を `store.replaceCells` へ渡す。旧 spec 後半には現行挙動と一致する記述もあり、spec 内部でも矛盾している。(`openspec/specs/settings-view-android-compose/spec.md` / `DSLDiffCalculator.kt` / `KsSettingsViewComposable.kt` / `DSLDiffCalculatorTest.kt`)
6. 旧 spec は `MutableState` を Cell data class 内に保持するとするが、実装は評価時点の値だけを Cell に格納し callback から state へ書き戻す。旧 `declarative-ui-bridge.md` の記述は現行コードと一致する。(`openspec/specs/settings-view-android-compose/spec.md` / `BasicCellDsl.kt` / `InputCellDsl.kt` / `reference/old-concepts/architecture/declarative-ui-bridge.md`)
7. 旧 spec は `DSLReidentifiableCell` と `DSLStyleModifiableCell` の両方を Core 所属とするが、実装は前者が core、後者と `DSLIconModifiableCell` が UI 所属である。(`openspec/specs/settings-view-android-compose/spec.md` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/DSLCellIdentity.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DSLStyleModifiable.kt` / `DSLIconModifiable.kt`)
8. 旧 spec の modifier Scenario は `KsFont` / `KsIcon` と `style.font` / `style.icon` を使うが、現行 API は Compose `TextStyle`、UI 層 `KsImage`、`CellStyle.titleFont`、Cell 本体の `icon` である。同じ spec の別節は現行型を記述しており内部矛盾になっている。(`openspec/specs/settings-view-android-compose/spec.md` / `DSLHandles.kt` / `CellModifiers.kt`)
9. `docs/platform-guide-android.md` は `.disabled(true)` を有効な modifier chain として案内するが、`CellHandle.disabled` と `Cell.disabled` は常に no-op。(`docs/platform-guide-android.md` / `DSLHandles.kt` / `CellModifiers.kt`)
10. `DSLScope.kt` と `CellModifiers.kt` の KDoc は `SwitchCell` / `CheckboxCell` 等が icon を持たない例を挙げるが、現行の基本7種・入力5種はすべて `DSLIconModifiableCell` に準拠し `withDSLIcon` を実装する。(`DSLScope.kt` / `CellModifiers.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/*Cell.kt`)
11. `docs/platform-guide-android.md` の Store 利用例は `insertCell(..., index = 0)` 等の引数名を案内するが、現行 Store API と Sample は `at` を使う。(`docs/platform-guide-android.md` / `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt` / `SettingsRootStore`)
12. 旧 spec 後半の `applyCellBaseLayout`、`MinHeightConstraintLayout`、各 ViewHolder の制約は `ks-settingsview-ui` の Host / Style 責務であり、Compose capability の実装領域には存在しない。capability 境界が混在しているため、本候補には取り込まず Android Host / Style 候補へ委ねる。(`openspec/specs/settings-view-android-compose/spec.md` / `android/ks-settingsview-ui/src/main/...`)

## 用語

- Store 方式: 利用者所有の `SettingsRootStore` を `KsSettingsView(store = ...)` へ渡す方式。
- DSL 方式: `KsSettingsView { ... }` が内部 Store と前回 resolved tree を `remember` する方式。
- `settingsRoot` builder: Store 初期値の `SettingsRoot` を明示 ID 付きで構築する純粋関数。Recomposition DSL とは別物。
- identity hint: explicit ID、`forEach` key、Header / 位置など、最終 String ID を決定する入力。
- resolved tree: identity hint を `Section.id` / `Cell.id` へ反映済みの宣言ツリー。
- `contentUpdates`: 同一 ID で値が変わった Cell の列。構造 Diff とは別に部分更新へ渡す。
- preflight: 通常差分の前に可視性変化を検出し、Full 更新へ切り替える判定。

## 抽出メモ

- `Android Compose Bridge と宣言 DSL` は Batch C の platform 固有 reference として独立価値がある。Batch B の `platforms/ios-swiftui.md` と同じ粒度で配置できる。
- identity と構造・内容・可視性同期は iOS / Android 共通原則であり、Android 固有文書へ重複させるより Batch D で architecture 概念へ統合するのが適切。ただし explicit と `forEach` の現行優先順位 drift は解消せず明示する必要がある。
- Cell 12 種の全パラメータ列挙は `cells/` の確定概念とコードから再導出しやすいため省いた。Compose 固有の state bridge と modifier の差だけを残した。
- `applyCellBaseLayout` 以下は Android Compose から見た下位 Host / Style の内部実装であり、この capability の独立概念に含めない。
