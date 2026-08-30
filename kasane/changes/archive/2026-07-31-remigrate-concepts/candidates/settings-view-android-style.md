# Candidate: settings-view-android-style

## 概念候補

### Android の Theme・CellStyle・EffectiveStyle (提案カテゴリ: styling/)

Android の見た目は UI 層の公開値型 `Theme` と `CellStyle` で指定し、内部型 `EffectiveStyle` が Android View の描画値へ解決する。Core はスタイル値を所有しない。公開型は中間表現を置かず、Compose Native 型の `Color`、`TextStyle`、`Dp` を直接受け取る。

#### 責務境界

- `Theme` は SettingsView 全体、Section Header / Footer、Cell 全体既定を保持する。`backgroundColor` は `RecyclerView` 自身、`cellBackgroundColor` は各 Cell の背景であり、互いに推論しない。
- `CellStyle` は単一 Cell の上書きを保持する。全フィールドの既定値 `null` は「未指定」ではなく Theme から継承する意思を表す。
- `EffectiveStyle` は論理値を `Color` / `TextStyle` / `Dp` のまま解決するアクセサと、Android View 用の ARGB `Int` / `Typeface` / sp `Float` への変換を担う。Cell の意味や構造差分は扱わない。
- Theme の変更は `SettingsRootDiff` に含めない。`SettingsRootStore.applyTheme` または `KsSettingsView.theme` の独立経路で現在の行を再 bind し、同値 Theme は再適用しない。

#### 保証すること

- 通常属性は `CellStyle.X` → `Theme.cellX` → platform default の順で解決する。valueText は Theme の valueText 指定がなければ title 指定へ、hintText は Theme の hint 指定がなければ accent へフォールバックする。
- `Theme.cellTitleFontSize > 0` は、解決した `titleFont` の `fontSize` だけを上書きする。Header / Footer でも `headerFontSize` / `footerFontSize` が対応する `TextStyle` の size より優先する。
- `ButtonCell` のタイトル色は `ButtonCell.titleColor` → `CellStyle.titleColor` → `Theme.cellTitleColor` → Material `colorPrimary` の4段で解決し、Material 属性を解決できなければ system blue 相当へフォールバックする。
- `Theme.backgroundColor` の変更は `RecyclerView` 背景へ反映し、Theme を使う main / root header / root footer の各 Adapter を再 bind する。装飾も新しい Theme で再構築する。
- 旧名 `viewBackgroundColor` / `titleColor` / `titleFont` の互換シムは持たず、公開名は `backgroundColor` / `cellTitleColor` / `cellTitleFont` とする。

#### してはいけないこと

- `KsColor` / `KsFont` のような共通論理型を Theme と CellStyle の間に挟まない。
- Theme の更新を構造変更として `SettingsRootDiff` へ混ぜない。
- SettingsView の canvas 背景と Cell 背景を同じ値として扱わない。

#### 公開 API

- `Theme`: 全体背景と装飾、行高さ、Section H/F、Cell 全体既定をまとめる `data class`。
- `CellStyle`: title / description / valueText / hintText、icon、行高、背景、accent の Cell 個別上書きをまとめる `data class`。
- `KsSettingsView.theme`: View 直接利用時の Theme 入口。
- Compose DSL の `KsSettingsView(theme = ...)`: 宣言的利用時の Theme 入口。

#### 利用例

```kotlin
val settingsTheme = Theme(
    backgroundColor = Color(0xFFF2EFE6),
    cellBackgroundColor = Color.White,
    cellAccentColor = Color(0xFFFFBF00),
    hasUnevenRows = true,
)

KsSettingsView(theme = settingsTheme) {
    Section(header = "一般") {
        LabelCell(
            title = "強調表示",
            style = CellStyle(
                titleColor = Color.Red,
                cellHeight = 80.dp,
            ),
        )
    }
}
```

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt` / `CellStyle.kt` / `EffectiveStyle.kt` / `KsSettingsView.kt`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`、`ThemeTest.kt` / `ThemeRenameTest.kt` / `CellStyleTest.kt` / `EffectiveStyleTest.kt` / `EffectiveStyleResolutionTest.kt`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt`、spec Purpose および Theme / CellStyle / EffectiveStyle Requirements。

### Android Cell 共通行レイアウトと視覚状態 (提案カテゴリ: styling/)

全 Cell ViewHolder は `CellBaseViews` と `applyCellBaseLayout` を共有し、任意要素の配置、実効スタイル、行高、通常・押下・無効状態を同じ視覚文法で描画する。Cell 種別固有の trailing control と操作は各 ViewHolder が担う。

#### 保証すること

- 行は左から icon、中央の title / description、右側の valueText / trailing control を持ち、hintText は右上前面に置く。任意値が `null` なら対応 View を `GONE` にして空領域を残さない。
- title と description は中央寄せの packed vertical chain、valueText は title の baseline、trailing control は行の右端中央へ配置する。hintText は trailing control より後の Z 順に置く。
- Android の実効行高は正の `CellStyle.cellHeight` → 正の `Theme.rowHeight` → 60dp の順で選び、最終値を必ず60dp以上にする。
- `hasUnevenRows = true` を既定とし、`WRAP_CONTENT` と minimum height により内容に応じて伸長させる。`false` のときだけ解決済み高さへ固定する。
- Cell コンテナの左右 padding は16dp、上下 padding は4dpとする。icon の後ろには16dpの間隔を置く。
- enabled 時の背景は実効 Cell 背景、押下時は `Theme.selectedColor` の `RippleDrawable` を使う。
- disabled 時は title / description / valueText / hintText を `Theme.disabledTextColor` に置換し、内部 Native control の `isEnabled` に disabled 表現を委譲する。行全体の alpha は下げず、操作と Ripple の発火を抑止する。
- Switch の ON track は実効 accent、ON thumb は Material `colorOnPrimary`、OFF track は `colorSurfaceContainerHighest`、OFF thumb は `colorOutline` とし、OFF の track と thumb を同色にしない。

#### してはいけないこと

- trailing control の生成や操作を `applyCellBaseLayout` に持ち込まない。
- disabled 表現として Cell 全体の alpha を一律に下げない。
- 可変高さモードで内容を60dpに切り詰めない。

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` / `MinHeightConstraintLayout.kt` / 各 `*CellViewHolder.kt` / `KsSimpleCheckView.kt`、`EffectiveStyleTest.kt` / `MinHeightConstraintLayoutTest.kt` / `BasicCellsTest.kt` / `UnifyCellCommonFieldsTest.kt`、spec の行高さ、SwitchCell、基本 Cell 垂直 padding Requirements。

### Android の Classic / Modern リスト外観 (提案カテゴリ: styling/)

`KsSettingsViewStyle` は同じ `RecyclerView`、Adapter、Cell Renderer を保ったまま、Section の装飾だけを `Classic` と `Modern` で切り替える公開 API である。既定は `Classic`。

#### 保証すること

- `KsSettingsView.style` の変更時は既存の `ItemDecoration` を除去して新しいものを1つだけ登録し、`invalidateItemDecorations()` で再描画する。Compose wrapper でも `style` の変化を同じ View property へ反映する。
- `ClassicSectionDecoration` は `Theme.separatorColor` の1物理 pixel hairline を Cell の上へ `onDrawOver` で描く。Section 境界は端から端、Section 内の中間罫線は左16dp inset とする。
- `ModernSectionDecoration` は Section に上下12dp、左右16dpの外側領域を取り、Section 単位で `Theme.cellBackgroundColor` の角丸背景を描く。
- Root Header / Footer は main list の装飾対象に含めない。Classic の罫線は Cell 行だけを対象とし、Modern の Section 背景は main list 内の Section H/F と Cell をまとめる。
- Theme の更新時は現在の Style に対応する Decoration を新 Theme で再構築し、separator / Section 背景の色を更新する。

#### してはいけないこと

- Style 切替で SettingsRoot、安定 ID、Adapter 構造、Cell Registry を変更しない。
- Classic の hairline を dp 換算して密度ごとに太くしない。
- Classic の中間罫線 inset を icon の有無で変えない。

#### 公開 API と利用例

```kotlin
KsSettingsView(
    style = KsSettingsViewStyle.Modern,
) {
    Section(header = "一般") {
        LabelCell(title = "バージョン", valueText = "1.0.0")
    }
}
```

出典: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewStyle.kt` / `ClassicSectionDecoration.kt` / `ModernSectionDecoration.kt` / `KsSettingsView.kt`、`KsSettingsViewStyleTest.kt` / `ClassicSectionDecorationTest.kt`、spec Purpose とスタイル切替・罫線 Requirements、`samples/android/` の各 `KsSettingsView(style = ...)`。

### Android ホストの Material3 Theme 前提 (提案カテゴリ: platforms/)

`ks-settingsview-ui` は `MaterialSwitch`、`MaterialCheckBox`、Material color attributes を使うため、ホストアプリの Android Theme を描画入力として消費する。Compose の `MaterialTheme` だけではなく、`AndroidView` が受け取る Context の XML Theme が Material3 派生であることが前提となる。

#### 保証すること

- Material color attribute の解決に失敗した一部の色は、白・灰・system blue などの明示 fallback を使う。
- Sample は `Theme.Material3.DayNight.NoActionBar` を application theme とし、利用者が必要な前提を実行可能な形で示す。

#### してはいけないこと

- framework 標準 `Theme.Material.*`、`Theme.AppCompat.*`、旧 `Theme.MaterialComponents.*` だけで `MaterialSwitch` を構築できると想定しない。`?attr/materialSwitchStyle` を解決できる `Theme.Material3.*` 派生 Theme をホスト側で指定する。

出典: `android/ks-settingsview-ui/build.gradle.kts`、`SwitchCellViewHolder.kt`、`ButtonCellViewHolder.kt`、`samples/android/app/src/main/AndroidManifest.xml`、`BasicCellsTest.kt`、`docs/platform-guide-android.md`。

## ADR 候補

- Android の `Theme` / `CellStyle` は Core の共通論理型を介さず Compose Native 型を UI 層で直接公開する — 出典: spec「Theme 型 (UI 層)」「CellStyle 型 (UI 層)」および `Theme.kt` / `CellStyle.kt`、選別基準: 能力・モジュール境界を越え、将来の Core API と両 platform の対称性を制約する。
- Android View ホストに `Theme.Material3.*` 派生 Theme を要求する — 出典: `android/ks-settingsview-ui/build.gradle.kts` の Material Components 依存コメント、Sample `AndroidManifest.xml`、docs のテーマ要件、選別基準: ホストアプリ境界を越え、利用可能な Android Theme と Native control 選択を将来にわたり制約する。
- Android の既定を「可変高さ + 最低60dp」とし、iOS の48ptへ機械的に揃えない — 出典: spec「行高さ」Requirement、`EffectiveStyle.MIN_ROW_HEIGHT_DP` と関連テスト、選別基準: AiForms 互換性に基づく platform 差であり、全 Cell のレイアウトと将来の視覚調整を制約する。

## drift 所見

- spec の Compose スタイル指定 Scenario は `KsSettingsView(root = state, style = ...)` を例示するが、現行の公開 overload は Store 方式 `KsSettingsView(store = ...)` と DSL 方式 `KsSettingsView(theme = ...) { ... }` であり `root` 引数はない（`openspec/specs/settings-view-android-style/spec.md` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`）。
- spec は `Theme(cellTitleFont = TextStyle(fontFamily = myFamily))` が同じ `FontFamily` を描画へ流す e2e を要求するが、`TextStyle.toTypeface()` は `fontFamily` を参照せず `Typeface.DEFAULT` と numeric weight だけを使う。italic やその他の font 属性も保持しない（同 spec「fontFamily 反映の e2e」/ `EffectiveStyle.kt`）。
- `Theme.cellIconSize` / `cellIconRadius` と `CellStyle.iconSize` / `iconRadius` は `EffectiveStyle` で解決・テストされる一方、`applyCellBaseLayout` は `iconView` を構築時の24dp固定サイズから変更せず、radius も描画へ適用しない。公開フィールドの値が Android View の最終表示へ接続されていない（同 spec「Theme 型」「EffectiveStyle」/ `Theme.kt` / `CellStyle.kt` / `EffectiveStyle.kt` / `CellBaseLayout.kt`）。
- `Theme.scrollIndicatorVisible` は公開値として保持・テストされるが、`KsSettingsView` の `RecyclerView` scrollbar へ適用するコードが見当たらない（同 spec「Theme 型」/ `Theme.kt` / `KsSettingsView.kt`）。
- spec「基本 Cell 共通の垂直パディング」はコンテナを `LinearLayout` と記すが、現行は `MinHeightConstraintLayout` を root とする `ConstraintLayout` 構造である。また同 Requirement 本文は minimum height の既定を44dpと記すが、同 spec の行高さ Requirement、コード、テストはいずれも60dpを正とする（同 spec / `CellBaseLayout.kt` / `EffectiveStyle.kt`）。
- 旧 concept「Cellの視覚状態」は「操作可能なCellだけが押下フィードバックを示す」とするが、現行テストは handler を持たない `LabelCell` や `CheckboxCell` も enabled なら Ripple 用に clickable とする契約を固定している（`kasane/changes/remigrate-concepts/reference/old-concepts/styling/cell-visual-states.md` / `BasicCellsTest.kt` / `applyCellBackground`）。
- `ClassicSectionDecoration` の冒頭コメントは「1dp相当」と記すが、実装、詳細コメント、spec、テストの現実は density 換算しない1物理 pixel固定である（`ClassicSectionDecoration.kt` / spec「セクション罫線」/ `ClassicSectionDecorationTest.kt`）。

## 用語

- `Theme`: SettingsView 全体の Android UI スタイルを保持する公開 `data class`。
- `CellStyle`: 単一 Cell の Theme 上書きを保持する公開 `data class`。`null` は継承を意味する。
- `EffectiveStyle`: Theme と CellStyle を描画可能な確定値へ解決する内部型とアクセサ群。
- `Classic`: 1px hairline を中心にしたフラットな Section 外観。
- `Modern`: margin と角丸背景で Section をグルーピングする外観。
- `CellBaseViews`: Android Cell の共通 View 構造。`MinHeightConstraintLayout`、共通テキスト、icon、accessory holder を束ねる。
- `platform default`: Context の Android / Material Theme 属性、または属性解決不能時に UI 層が採用する固定 fallback。

## 抽出メモ

- `Theme` / `CellStyle` / `EffectiveStyle` は `settings-view-android-theme-bridge` と材料が重なる。独立文書を二重化せず、同 capability の候補と統合して style resolution の単一概念にするのが自然。
- Cell 共通行・視覚状態・Classic/Modern は iOS と責務が対称である。Android 固有値を残しつつ、Batch D で `settings-view-ios-style` と統合すれば `styling/cell-row-layout.md`、`styling/cell-visual-states.md`、`styling/list-appearance.md` の3概念へ整理できる。
- Material3 Theme 前提は Android 固有のホスト契約なので `platforms/` が中心。横断的な style resolution から参照する形がよい。
- drift の解消方向は判断していない。特に `fontFamily` と icon size/radius は公開 API が存在するため、文書側を弱めるか実装を接続するかをオーナー判断に委ねる。
