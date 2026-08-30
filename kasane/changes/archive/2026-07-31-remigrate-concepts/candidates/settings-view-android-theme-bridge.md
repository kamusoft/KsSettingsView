# Candidate: settings-view-android-theme-bridge

## 概念候補

### Android Compose の Theme bridge (提案カテゴリ: platforms/)

Android Compose 入口は UI 層の Native `Theme` を `SettingsRootStore` の独立した Theme 状態へ渡し、同じ Store が保持する `SettingsRoot` と組み合わせて Native `KsSettingsView` を描画する。Theme は構造差分ではなく、現在の行・装飾を再 bind する描画入力である。

#### 責務境界

- Store 方式の `KsSettingsView(store = ...)` は `store.theme` を購読し、呼び出し側が `SettingsRootStore(initialTheme = ...)` または `store.applyTheme(...)` で Theme を所有する。
- DSL 方式の `KsSettingsView(theme = ...) { ... }` は内部 `SettingsRootStore` を `remember` し、最初の Theme を `initialTheme` として渡す。再 composition で Theme が変わった場合だけ `applyTheme` を呼ぶ。
- Theme は `SettingsRootDiff` に含めず、`SettingsRootStore.theme: StateFlow<Theme>` で構造状態と独立して伝播する。`applyTheme` は Diff を emit しない。
- Compose wrapper は `AndroidView` 内に Native `KsSettingsView` を生成する。Compose は状態伝播とライフサイクルを担い、Cell の最終描画は共通の Native renderer が担う。
- Theme / CellStyle / EffectiveStyle の値型と解決規則は「Android の Theme・CellStyle・EffectiveStyle」候補へ統合し、この概念では Compose と Store の伝播境界だけを扱う。

#### 保証すること

- DSL 方式で `theme` が前回値と等しい再 composition は `applyTheme` を呼ばず、不要な Native View 更新を起こさない。
- Theme の変更は既存の Store と SettingsRoot を維持したまま Native `KsSettingsView.theme` へ到達し、main / root header / root footer の再 bind と Decoration の再構築を引き起こす。
- DSL 方式と Store 方式は同じ `SettingsRootStore` と Native `KsSettingsView` へ収束し、最終描画経路を二重化しない。
- `MaterialSwitch` などが参照する属性は `AndroidView` の Context Theme から解決される。このためホストの XML Theme は `Theme.Material3.*` 派生でなければならず、Compose の `MaterialTheme` だけでは代替できない。

#### してはいけないこと

- Theme 変更を Section / Cell の挿入・削除・更新として `SettingsRootDiff` に混ぜない。
- Compose 専用 Cell renderer を作って Native renderer と描画規則を分岐させない。
- `AndroidView` の Context が Material3 Theme であるというホスト前提を、Compose `MaterialTheme` の存在から推論しない。

#### 公開 API

- `KsSettingsView(store: SettingsRootStore, ...)`: Store を呼び出し側が所有する Compose 入口。
- `KsSettingsView(theme: Theme = Theme(), content: SettingsScope.() -> Unit, ...)`: DSL と Theme を同時に受け取る Compose 入口。
- `SettingsRootStore(initialTheme: Theme = Theme())`: Store 生成時の Theme 入口。
- `SettingsRootStore.applyTheme(theme: Theme)`: 構造 Diff を伴わない Theme 更新入口。

#### 利用例

```kotlin
val settingsTheme = Theme(
    backgroundColor = Color(0xFFF5F5F5),
    cellTitleColor = Color(0xFF202020),
)

KsSettingsView(theme = settingsTheme) {
    Section(header = "一般") {
        LabelCell(title = "バージョン", valueText = "1.0.0")
    }
}
```

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` / `KsSettingsView.kt`、`KsSettingsViewComposeTest.kt` / `SettingsRootStoreTest.kt`、ADR-0004 / ADR-0006 / ADR-0007 / ADR-0009。

### Android Compose Cell modifier の契約 (提案カテゴリ: platforms/)

Android Compose DSL は、構築済み `Cell` に適用する extension modifier と、DSL に追加済みの Cell を指す `CellHandle` modifier の2面を提供する。どちらも Cell を不変値としてコピーし、対応 capability protocol を実装しない独自 Cell には暗黙の no-op となる。

#### 責務境界

- `Cell` extension は `DSLStyleModifiableCell` / `DSLIconModifiableCell` を検査し、対応する場合だけ `withDSLStyle` / `withDSLIcon` で新しい Cell を返す。
- `CellHandle` は DSL 内の Cell index を指し、同じ capability protocol を通じてその位置の Cell を置換する。無効な index は例外にせず no-op とする。
- `DSLExplicitIdCell` は明示 ID を保持したまま、style / icon の変更を内包 Cell へ委譲する。`cellID` を重ねた場合は最後の ID が有効になる。
- Core の `Cell` は open な最小抽象を維持し、Compose modifier 対応を全独自 Cell に強制しない。独自 Cell が modifier を受けたい場合は UI 層の capability protocol を明示実装する。

#### modifier 対応表

| modifier | 変更対象 | 非対応時・注意点 |
|---|---|---|
| `font(TextStyle)` | `CellStyle.titleFont` | hintText の font は変更しない。`DSLStyleModifiableCell` でなければ no-op |
| `cellHeight(Dp)` | `CellStyle.cellHeight` | `DSLStyleModifiableCell` でなければ no-op |
| `titleColor(Color)` | `CellStyle.titleColor` | `DSLStyleModifiableCell` でなければ no-op |
| `backgroundColor(Color)` | `CellStyle.backgroundColor` | `DSLStyleModifiableCell` でなければ no-op |
| `icon(KsImage?)` | Cell の `icon` | `DSLIconModifiableCell` でなければ no-op。`null` は icon を除去 |
| `cellID(Any)` | DSL の明示安定 ID | 既存の明示 ID wrapper があれば最後の値へ置換 |
| `disabled(Boolean)` | なし | 引数にかかわらず現行実装は常に no-op |

#### 保証すること

- 現行の組み込み Cell 12種はすべて `DSLStyleModifiableCell` と `DSLIconModifiableCell` を実装するため、style 4種と `icon` modifier を適用できる。
- modifier は元の Cell を破壊せず `copy` した値へ変更を反映する。連鎖時は直前までの変更と明示 ID を保持する。
- `disabled(true)` は Cell の `isEnabled` を変えない。無効 Cell を構築する場合は各 Cell の `isEnabled = false` を使う。
- `font` は title のみを変更する。hintText の font を同時変更するという契約は持たない。

#### してはいけないこと

- no-op の `disabled` を利用例や概念文書で有効な modifier として案内しない。
- capability protocol 未実装の独自 Cell に style / icon modifier が適用されると仮定しない。
- `font` から `CellStyle.hintTextFont` も変更されると推論しない。
- 現在の組み込み Cell の icon 対応を `LabelCell` / `CommandCell` だけに限定して記述しない。

#### 利用例

```kotlin
KsSettingsView(theme = Theme()) {
    Section(header = "アカウント") {
        LabelCell(
            title = "通知",
            isEnabled = false,
        )
            .titleColor(Color.Gray)
            .backgroundColor(Color.White)
            .cellHeight(64.dp)
            .icon(notificationIcon)
            .cellID("notification")
    }
}
```

出典: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CellModifiers.kt` / `DSLHandles.kt` / `DSLNodes.kt` / `DSLScope.kt`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DSLStyleModifiable.kt` / `DSLIconModifiable.kt`、12種の組み込み Cell model、`DSLHandleTest.kt` / `BasicCellDslTest.kt` / `InputCellDslTest.kt`、ADR-0009 / ADR-0013。

## ADR 候補

- Android ホストの XML Theme に `Theme.Material3.*` 派生を要求し、Compose `MaterialTheme` だけでは代替できないとする — 出典: `ks-settingsview-ui/build.gradle.kts`、`MaterialSwitch` を生成する ViewHolder、Sample `AndroidManifest.xml`、`BasicCellsTest.kt`。選別基準: ライブラリ利用者の application Theme と Native control 選択を横断して制約する。`settings-view-android-style` の同一候補と統合する。
- Theme / CellStyle を UI 層の Native 型として所有する判断は ADR-0009、Native renderer を Compose でも共有する判断は ADR-0004、DSL と Store の収束は ADR-0007、Theme を構造 Diff から分離する判断は ADR-0006 と ADR-0009、独自 Cell の modifier 対応を任意 capability とする境界は ADR-0013 で既に記録済みであり、新規 ADR を重ねない。

## drift 所見

- spec の Purpose は `KsSettingsViewCore` が `Theme` / `CellStyle` / `KsColor` / `KsImage` の論理値を所有すると記すが、現行実装では Theme と CellStyle は UI 層が Compose Native 型で所有し、`KsColor` は存在しない。同じ spec 後半の「UI 層」「Native 型」Requirement とも自己矛盾する（`openspec/specs/settings-view-android-theme-bridge/spec.md` / `Theme.kt` / `CellStyle.kt` / ADR-0009）。
- spec は旧公開名 `Theme.viewBackgroundColor` / `Theme.titleColor` / `Theme.titleFont` と `Theme(descriptionColor = ...)` を繰り返し使用する。現行名は `backgroundColor` / `cellTitleColor` / `cellTitleFont` / `cellDescriptionColor` で、旧名の互換 shim はない（同 spec / `Theme.kt` / `ThemeRenameTest.kt`）。
- spec の Switch accent Scenario は checked thumb と track の双方を accent green にすると記すが、現行描画は checked track に accent、checked thumb に Material `colorOnPrimary` を使う（同 spec / `SwitchCellViewHolder.kt` / `BasicCellsTest.kt`）。
- `docs/platform-guide-android.md` は `CellHandle` の連鎖例で `.disabled(true)` を案内するが、`CellHandle.disabled` と `Cell.disabled` は引数にかかわらず同じ値を返すだけで、`isEnabled` を変更しない（同 docs / `DSLHandles.kt` / `CellModifiers.kt`）。
- `CellModifiers.kt` の `font` コメントは「タイトル / ヒントテキスト」と記すが、実装は `CellStyle.titleFont` だけを更新し `hintTextFont` は変更しない。`CellHandle.font` も同じ挙動である。
- `DSLIconModifiable.kt`、`CellModifiers.kt`、`DSLScope.kt` のコメントは icon modifier 対応を `LabelCell` / `CommandCell` に限定するが、現行の組み込み Cell 12種はすべて `DSLIconModifiableCell` を実装し、`icon` を保持する。
- theme bridge spec は Native `MaterialSwitch` がホスト Context の `?attr/materialSwitchStyle` を要求することと、XML Theme を `Theme.Material3.*` 派生にする前提を記述していない。Compose `MaterialTheme` だけの利用者には実行時クラッシュ条件が見えない（同 spec / `ks-settingsview-ui/build.gradle.kts` / `docs/platform-guide-android.md` / Sample `AndroidManifest.xml`）。
- modifier のテストは `cellHeight` / `font` / `titleColor` / `cellID` を主に固定しており、`backgroundColor` / `icon` / 常時 no-op の `disabled` を公開 API 単位で直接検証するテストが見当たらない（`DSLHandleTest.kt` / `BasicCellDslTest.kt` / `InputCellDslTest.kt`）。

## 用語

- `Theme bridge`: Compose の `Theme` 入力を Store の独立 Theme 状態と Native `KsSettingsView` へ伝える経路。
- `CellHandle`: DSL へ追加済みの Cell の位置を指し、その Cell を不変更新するためのハンドル。
- `DSLStyleModifiableCell`: Compose style modifier を受けるために UI 層が定義する任意 capability protocol。
- `DSLIconModifiableCell`: Compose icon modifier を受けるために UI 層が定義する任意 capability protocol。
- `DSLExplicitIdCell`: 明示安定 ID を付与しつつ、内包 Cell の style / icon capability を透過する wrapper。
- `no-op`: 対象値を変更せず同じ Cell または CellHandle を返す挙動。非対応 capability、無効 index、現行の `disabled` で発生する。

## 抽出メモ

- Theme の値と EffectiveStyle の解決順は `settings-view-android-style.md` の「Android の Theme・CellStyle・EffectiveStyle」に統合し、本候補から二重に概念化しない。
- Theme bridge は ADR-0004 / 0006 / 0007 / 0009 の組み合わせで設計理由を追跡できる。新規判断として残るのは Material3 ホスト Theme 前提であり、これは sibling candidate と単一 ADR 候補に統合する。
- `disabled` は互換予約 API なのか未実装なのかコードだけでは判断できない。現実は no-op と記録し、実装するか API / docs を除去するかはオーナー判断に委ねる。
- `font` の契約と icon capability の対象 Cell はコードとコメントが食い違うため、concept では実装を正として記述した。
- 抽出順は Compose modifier / Store / Cell model / tests を先に読み、その後に OpenSpec、旧 concept、docs、既存 ADR を照合した。
