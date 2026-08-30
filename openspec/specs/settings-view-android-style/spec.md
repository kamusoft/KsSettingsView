# settings-view-android-style Specification

## Purpose

`settings-view-android-style` は、`ks-settingsview-ui`（Android）の **スタイル・レイアウト層**（`KsSettingsViewStyle` クラシック/モダン切替・`ItemDecoration` 群・Section H/F と Root H/F の描画詳細・セクション罫線・行高さと垂直パディング・SwitchCell / CheckboxCell の見た目調整）を担う capability である。`settings-view-android-host` が提供する `RecyclerView` 基盤と Cell ViewHolder 抽象の上で、最終的な見た目（フラットな旧 AiForms 互換とモダンな角丸グルーピング）を組み立てる責務を持つ。`Theme` / `CellStyle` 値をプラットフォーム値に変換する責務は `settings-view-android-theme-bridge` に分離されており、本 capability はその変換結果を消費する立場である。

## Requirements
### Requirement: スタイル切替（クラシック/モダン）

`KsSettingsView` は `var style: KsSettingsViewStyle` プロパティを持たなければならない (SHALL)。`KsSettingsViewStyle` は `Classic`（旧 AiForms 互換のフラットな見た目）と `Modern`（最新 OS 設定画面風の角丸グルーピング）の 2 ケースを持つ enum でなければならない (MUST)。`style` の変更時は内部 `RecyclerView` の `ItemDecoration` を入れ替えなければならない (MUST)。

#### Scenario: Classic スタイルの ItemDecoration

- **GIVEN** `KsSettingsView` を `style = Classic` で初期化
- **WHEN** 内部 `RecyclerView.itemDecorationCount` および各 ItemDecoration のクラスを取得する
- **THEN** `ClassicSectionDecoration`（フラットな区切り線のみ）が登録されており、`ModernSectionDecoration` は登録されていない

#### Scenario: Modern スタイルの ItemDecoration

- **GIVEN** `KsSettingsView` を `style = Modern` で初期化
- **WHEN** 内部 `RecyclerView` の ItemDecoration を取得する
- **THEN** `ModernSectionDecoration`（Section 単位の角丸背景・外側マージン描画）が登録されている

#### Scenario: 動的なスタイル切替

- **GIVEN** `KsSettingsView` が `style = Classic` で表示中
- **WHEN** `view.style = KsSettingsViewStyle.Modern` を代入する
- **THEN** `RecyclerView` の既存 ItemDecoration が removeItemDecoration で取り除かれ、`ModernSectionDecoration` が addItemDecoration で追加される。`invalidateItemDecorations` が呼ばれて再描画される

#### Scenario: Compose ラッパでのスタイル指定

- **GIVEN** Compose で `KsSettingsView(root = state, style = KsSettingsViewStyle.Modern)` を記述
- **WHEN** 初回 Composition される
- **THEN** `AndroidView.factory` で生成された `KsSettingsView` の `style` が `Modern` で初期化される

### Requirement: Section H/F（SectionAccessory）の描画

`KsSettingsView` は `SectionAccessory.Text(String)` 形式のヘッダ／フッタを TextView ベースの ViewHolder で描画しなければならない (SHALL)。`SectionAccessory.View(KsAnyView)` 形式は、`KsAnyView.Compose` backing の場合は `ComposeView` を内包する ViewHolder で `setContent { compose() }` により描画し、`KsAnyView.AndroidView` backing の場合は `factory(context)` で生成した View を `addView` する ViewHolder で描画しなければならない (MUST)。`ComposeView` を含む ViewHolder は `setViewCompositionStrategy(DisposeOnDetachedFromWindow)` を強制した基盤クラスを使わなければならない (MUST)。

#### Scenario: Text 形式ヘッダの描画

- **GIVEN** `Section(header = SectionAccessory.Text("一般"), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の TextView に "一般" が描画される

#### Scenario: View 形式ヘッダ（Compose backing）の描画

- **GIVEN** `Section(header = SectionAccessory.View(KsAnyView.Compose { ProfileCard() }), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の `ComposeView` に対し `setContent { ProfileCard() }` が呼ばれ、Compose 描画が行われる

#### Scenario: View 形式ヘッダ（Android View backing）の描画

- **GIVEN** `Section(header = SectionAccessory.View(KsAnyView.AndroidView { ctx -> MyCustomView(ctx) }), ...)` を含む `SettingsRoot`
- **WHEN** `view.root` に代入する
- **THEN** ヘッダ ViewHolder の container に対し `factory(context)` で生成された `MyCustomView` インスタンスが `addView` され、可視描画される

#### Scenario: View 形式ヘッダの中身更新（差分検出非対応）

- **GIVEN** `view.root` に `SectionAccessory.View(KsAnyView.Compose { Counter(value = 1) })` を含む root を代入
- **WHEN** `SectionAccessory.View(KsAnyView.Compose { Counter(value = 2) })` を含む root に置き換える
- **THEN** `KsAnyView` は差分検出に参加しないため ViewHolder の生成・破棄は走らないが、`ComposeView.setContent` の再呼び出しによって `Counter(value = 2)` の中身が再描画される

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsView` は `rootHeader: RootAccessory?` / `rootFooter: RootAccessory?` を UI 層プロパティとして持ち、`ConcatAdapter` の先頭 / 末尾に位置する `RootHeaderFooterAdapter` で描画しなければならない (SHALL)。`RootHeaderFooterAdapter` は `view: RootAccessory?` プロパティを持ち、`null` のとき `getItemCount()` は 0、非 `null` のとき 1 を返さなければならない (MUST)。`view` プロパティの setter は変化前後の `null` / 非 `null` 状態に応じて `notifyItemInserted(0)` / `notifyItemRemoved(0)` / `notifyItemChanged(0)` を発行しなければならない (MUST)。`headerAdapter.getItemId(0)` は `1L`、`footerAdapter.getItemId(0)` は `2L` を予約値として返し、`mainListAdapter` 側の `getItemId` はこれと衝突しない値域を使わなければならない (MUST)。

`RootHeaderFooterAdapter` の ViewHolder は `RootAccessory.Text(String)` を TextView 描画、`RootAccessory.View(KsAnyView)` を `ComposeView.setContent`（Compose backing）または `addView`（Android View backing）で描画しなければならない (MUST)。`SettingsRoot` 値型自体には `header` / `footer` を含まないため (MUST NOT)、本 Requirement の入力源は UI 層プロパティ（`view.rootHeader` 代入、Compose ラッパの `headerView` 引数、または `SettingsRootStore.updateAccessory(target: AccessoryTarget.RootHeader, accessory:)` Diff 経由）のみとする。

<!-- 注: `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、Root H/F の入力源を UI 層プロパティ（`KsSettingsView.rootHeader` / `rootFooter`、Compose ラッパの `headerView` / `footerView` 引数、`SettingsRootStore.updateAccessory(target: AccessoryTarget.RootHeader/RootFooter, accessory:)` Diff 経由）に変更している。`RootHeaderFooterAdapter` の描画ロジック自体は維持される。Requirement 名は archive 済 spec との連続性を保つため変更しないが、説明文と Scenario は新 API に合わせて書き直している。 -->

#### Scenario: Root Header（Text）の描画

- **GIVEN** `view.rootHeader = RootAccessory.Text("プロフィール")` を代入
- **WHEN** RecyclerView を描画する
- **THEN** RecyclerView 先頭に "プロフィール" を表示する 1 つの ViewHolder が描画される（`headerAdapter.itemCount = 1`）

#### Scenario: Root Footer（View、Compose backing）の描画

- **GIVEN** `view.rootFooter = RootAccessory.View(KsAnyView.Compose { Text("v1.0.0") })` を代入
- **WHEN** RecyclerView を描画する
- **THEN** RecyclerView 末尾に Compose で `Text("v1.0.0")` が描画される ViewHolder が表示される（`footerAdapter.itemCount = 1`）

#### Scenario: Root H/F が null の場合

- **GIVEN** `view.rootHeader = null` および `view.rootFooter = null`
- **WHEN** RecyclerView を描画する
- **THEN** `headerAdapter.itemCount = 0` および `footerAdapter.itemCount = 0` となり、RecyclerView は `mainListAdapter` の Section H/F + Cell のみを描画する

#### Scenario: Root Header の追加・削除通知

- **GIVEN** `view.rootHeader = null` の状態
- **WHEN** `view.rootHeader = RootAccessory.Text("新規")` に変更する
- **THEN** `headerAdapter.notifyItemInserted(0)` が発行され、Header 1 行が挿入アニメーションで追加される

#### Scenario: Store 経由の Accessory 更新

- **GIVEN** Store が初期化済み、View が Store にバインド済み
- **WHEN** `store.updateAccessory(target = AccessoryTarget.RootHeader, accessory = SettingsAccessory.Root(RootAccessory.Text("X")))` を呼ぶ
- **THEN** Store が `UpdateAccessory(...)` Diff を発行し、View の `applyDiff` が `rootHeader` を `RootAccessory.Text("X")` に更新する

#### Scenario: ID 衝突回避

- **GIVEN** `headerAdapter` / `footerAdapter` / `mainListAdapter` がすべて `setHasStableIds(true)` の場合
- **WHEN** ConcatAdapter 内の各 adapter の `getItemId` を確認する
- **THEN** `headerAdapter.getItemId(0) = 1L`、`footerAdapter.getItemId(0) = 2L`、`mainListAdapter` の各 ID は 1L / 2L と衝突しない値域（例: `100L` 以上、または Cell の Hashable から派生する Long）を返す

### Requirement: 行高さ（RowHeight / HasUnevenRows）の適用

各 `CellViewHolder` は bind 時に、`Theme.rowHeight` / `Theme.hasUnevenRows` / `CellStyle.cellHeight` を合成した実効高さを Cell コンテナに適用しなければならない (MUST)。

実効高さ算出は以下の通り：

- `effectiveBase = CellStyle.cellHeight ?? Theme.rowHeight` （正の値が指定されていれば採用）
- 上記いずれも未指定（`null` または `-1`／非正の値）のときは `MIN_ROW_HEIGHT_DP = 60dp` を base に採用する (MUST)
- `effectiveHeightDp = max(effectiveBase, MIN_ROW_HEIGHT_DP)`（`MIN_ROW_HEIGHT_DP = 60dp`、最終下限）
- `effectiveHeightPx = (effectiveHeightDp * Resources.displayMetrics.density).toInt()`

`MIN_ROW_HEIGHT_DP = 60dp` の根拠は、オリジナル `AiForms.Maui.SettingsView` の
`AiRecyclerView.UpdateRowHeight()`（`Native/Android/AiRecyclerView.cs:228-235`）が
`RowHeight == -1` のとき自動的に `60` をセットし、続く `SettingsViewRecyclerAdapter.cs:483` で
`max(rowHeight=60, MinRowHeight=44) = 60` を最終高さに採用する挙動の踏襲である。
旧設計の `MinRowHeight = 44dp` は実質デッドコード（最終高さは常に 60dp 以上）であったため、
`refine-cell-layout-after-unify-review` Phase 11 のオーナー判断で Android の最終下限を
60dp に統一する（44dp 廃止）。iOS 側（`minRowHeight = 48`、オリジナル `AiTableView.cs:19`
踏襲）はプラットフォーム慣習として据え置く。

`Theme()` を引数なしで構築した場合、未指定時の base が直接 44dp になると SwitchCell の
hintText が switch に重なる等、視覚的に詰まりすぎる事象が発生するためこの 60dp 既定を
SoT 化する。

適用方法：

- `Theme.hasUnevenRows == false` のとき: `container.layoutParams.height = effectiveHeightPx` で **固定高さ** を適用しなければならない (MUST)。すべての Cell が同じ高さに揃う（個別 `CellStyle.cellHeight` が指定された Cell はその Cell 単位で固定高さが上書きされる）。
- `Theme.hasUnevenRows == true` のとき: `container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT` かつ `container.minimumHeight = effectiveHeightPx` で **最低高さ保証付きの可変高さ** を適用しなければならない (MUST)。長文 Description などで自然に伸縮する。

bind 時の高さ更新は前回値と異なる場合のみ `requestLayout()` を呼んで再レイアウトをトリガーしなければならない (MUST)。

`Theme.hasUnevenRows` のデフォルト値は **`true`**（最低高さ保証付きの可変高さ）とする (MUST)。これによりオリジナル `AiForms.Maui.SettingsView` の `AiRecyclerView.UpdateRowHeight()`（`RowHeight = -1` のとき自動で `60` をセットしつつ MinHeight 扱い）と整合した「Auto 高さ + 下限保証」既定挙動が得られる。「全 Cell を一律固定高さで揃えたい」用途では利用者が `Theme(hasUnevenRows = false)` を明示指定することで従来の固定高さモードを選べる。

#### Scenario: 固定高さ（HasUnevenRows = false）

- **GIVEN** `Theme(rowHeight = 60, hasUnevenRows = false)` で初期化された `KsSettingsView`、画面密度 2.0、複数 Cell が並ぶ
- **WHEN** Android で表示される
- **THEN** すべての Cell コンテナの `layoutParams.height` が `120 px`（60 dp × 2.0）に設定される

#### Scenario: 可変高さ（HasUnevenRows = true、新デフォルト）

- **GIVEN** `Theme()` を引数なしで構築した `KsSettingsView`（`rowHeight = -1` / `hasUnevenRows = true` 新デフォルト）、画面密度 2.0、長文 Description を持つ Cell と単行 Cell が混在
- **WHEN** Android で表示される
- **THEN** 各 Cell コンテナの `layoutParams.height` が `WRAP_CONTENT`、`minimumHeight` が `120 px`（`MIN_ROW_HEIGHT_DP = 60dp` × 2.0）に設定される。長文 Cell は 120 px より高くなり、単行 Cell は 120 px 相当に保たれる。これによりオリジナル `AiForms.Maui.SettingsView` の `AiRecyclerView`（`RowHeight = -1` のとき自動で `60` をセット）と整合する

#### Scenario: CellStyle.cellHeight の優先

- **GIVEN** `Theme(rowHeight = 80, hasUnevenRows = false)` と `CellStyle(cellHeight = 100)` を持つ特定 CommandCell、画面密度 2.0
- **WHEN** Android で表示される
- **THEN** 当該 Cell の `layoutParams.height` は `200 px`（100 dp × 2.0、`CellStyle.cellHeight` 優先）。他 Cell は `160 px`（80 dp × 2.0、`Theme.rowHeight` 採用）。なお `Theme.rowHeight` が `MIN_ROW_HEIGHT_DP = 60dp` 未満（例: 30dp）の場合は最終下限 60dp までガードされて `120 px` になる

### Requirement: SwitchCell の Thumb / Track 色分離

`settings-view-android-ui` は `SwitchCellViewHolder` で `MaterialSwitch` の `thumbTintList` と `trackTintList` を独立に設定しなければならない (MUST)。Track 側も状態別 `ColorStateList` で設定しなければならない (MUST)。Material 3 標準の MaterialSwitch オフ時挙動に揃え、**オフ時の Track と Thumb には異なる Material トークンを使い、両者が視覚的に分離して見える色にしなければならない (MUST)**。

- `trackTintList`: 状態別 `ColorStateList`
  - `android.R.attr.state_checked = true` → 実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）
  - `android.R.attr.state_checked = false` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerHighest, Color.LTGRAY)` 相当（薄いグレー）。古い Material ライブラリで `colorSurfaceContainerHighest` が未解決の場合は `Color.LTGRAY` にフォールバックする。
- `thumbTintList`: 状態別 `ColorStateList`
  - `android.R.attr.state_checked = true` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE)` 相当（白系）
  - `android.R.attr.state_checked = false` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline, Color.GRAY)` 相当(中間〜濃いグレー）

**オフ時の Track（`colorSurfaceContainerHighest` ≒ 薄いグレー）と Thumb（`colorOutline` ≒ 中間グレー）は明確に異なる色でなければならない (MUST)**。両方を `colorOutline` にしてはならない (MUST NOT)。

**Rationale**: 前回実装では Track / Thumb のオフ時を両方とも `colorOutline` にしていたため、Image #6 のスクリーンショットで両者が同色化し「Switch がのっぺりした 1 枚のグレー帯」に見える状態だった。Material 3 標準（Image #7 の Google Play 通知設定など）に揃え、オフ時 Track を `colorSurfaceContainerHighest`（薄いグレー）、オフ時 Thumb を `colorOutline`（中間グレー）にすることで、Thumb / Track の輪郭が明確に分離して見える。

#### Scenario: checked = true 時の Thumb 白系・Track accent

- **GIVEN** `SwitchCell(title: "通知", isOn: true)` で `Theme(cellAccentColor: KsColor.orange)` を適用
- **WHEN** Android で描画される
- **THEN** Track はオレンジ色（実効 accent）で、Thumb は白系（`colorOnPrimary` 相当）で描画され、Thumb と Track が視覚的に分離して見える

#### Scenario: checked = false 時の Thumb と Track が分離した色になる

- **GIVEN** `SwitchCell(title: "通知", isOn: false)`
- **WHEN** Android で描画される
- **THEN** Track は薄いグレー（`colorSurfaceContainerHighest` 相当）、Thumb は中間グレー（`colorOutline` 相当）で描画される。Track と Thumb の色は明確に異なり、Switch の輪郭が視覚的に判別できる。Track は accent 色（オレンジ）にはならない。

#### Scenario: オフ時 Track と Thumb の色は等しくない

- **GIVEN** `SwitchCell(title: "通知", isOn: false)` を任意の Theme で描画
- **WHEN** `trackTintList.getColorForState(unchecked, ...)` と `thumbTintList.getColorForState(unchecked, ...)` を取得する
- **THEN** 2 つの色は等しくない（`!=`）。

#### Scenario: CellStyle.accentColor の優先

- **GIVEN** `SwitchCell(title: "通知", isOn: true, style: CellStyle(accentColor: KsColor.green))` を `Theme(cellAccentColor: KsColor.orange)` 下で表示
- **WHEN** Android で描画される
- **THEN** Track は緑（`CellStyle.accentColor` 優先）、Thumb は白系で描画される

### Requirement: セクション罫線の描画位置と太さ

`settings-view-android-ui` の `ClassicSectionDecoration`（`onDrawOver` で描画）は、AiForms.Maui.SettingsView オリジナル `Platforms/Droid/SVItemdecoration.cs` および `drawable/divider.xml` (`<size android:height="1px"/>`) の挙動に揃え、次の規則で罫線を描画しなければならない (MUST)。

- **線の太さ**: 1 ピクセル固定（`density` による dp 換算は行わない）。`Paint.strokeWidth` または描画矩形の高さで 1px を保証する。
- **描画位置**: 各 Section の上端と下端、および Section 内の Cell 間。
  - セクション最初の Cell の上端 → 罫線を描画する (MUST)
  - セクション最後の Cell の下端 → 罫線を描画する (MUST)
  - セクション内 Cell 間 → 罫線を描画する (MUST)
- **線の色**: `Theme.separatorColor`。
- **左インセット規則**（iOS と視覚的に揃える。AiForms オリジナル準拠）:
  - セクション最初 Cell の **上端罫線** → 左インセット 0（`paddingLeft` から `width - paddingRight` まで、端から端で描画）
  - セクション最後 Cell の **下端罫線** → 左インセット 0（端から端で描画）
  - セクション内中間 Cell の **下端罫線** → 左インセット 16dp 相当（`paddingLeft + 16 * displayMetrics.density` から `width - paddingRight` まで描画）
  - アイコン有無に関わらず固定（iOS と同様、AiForms オリジナル準拠の挙動）

**Rationale**: iOS 側の「罫線インセット規則」Requirement（セクション境界 = 0、セクション内中間 = 16pt）とクロスプラットフォームで視覚的に揃えるため、Android 側も同じ規則を採用する。AiForms.Maui.SettingsView オリジナル Android のスクリーンショットでも、セクション内 Cell 間罫線に約 16dp の左インセットが見えており、本実装はこれに準拠する。

#### Scenario: セクション上端の罫線

- **GIVEN** 2 つ以上のセクションを持つ SettingsView の 2 番目のセクション
- **WHEN** Android で描画される
- **THEN** 2 番目のセクションの最初の Cell の **上端**にも `Theme.separatorColor` で 1px の罫線が描画される（iOS と同じ「セクション境界では端から端」の見た目）

#### Scenario: 罫線の太さが 1px

- **GIVEN** 任意の Cell 行
- **WHEN** Android で描画される
- **THEN** Cell 下端の罫線は **1 ピクセル**（density による dp 換算なし）で描画され、端末密度に関わらず細い hairline として見える

#### Scenario: セクション境界の罫線は端から端で描画される

- **GIVEN** 3 つの Cell を持つセクションがあり、その前後に別の Section が存在する
- **WHEN** Android で描画される
- **THEN** セクション最初 Cell の上端罫線とセクション最後 Cell の下端罫線は、左インセット 0（`paddingLeft` から `width - paddingRight` まで）で描画される

#### Scenario: セクション内中間 Cell の下端罫線は 16dp インセットで描画される

- **GIVEN** 3 つの Cell を持つセクション
- **WHEN** Android で描画される
- **THEN** セクション内中間 Cell（最初でも最後でもない Cell）の下端罫線は、左に 16dp 相当のインセット（`paddingLeft + 16 * displayMetrics.density` から `width - paddingRight` まで）で描画され、iOS の `bottomSeparatorInsets.leading = 16pt` と視覚的に揃う

#### Scenario: アイコン有り Cell の中間下端罫線も同じ 16dp インセット

- **GIVEN** アイコン有り Cell とアイコン無し Cell が混在する 1 つのセクション
- **WHEN** Android で描画される
- **THEN** セクション内中間 Cell の下端罫線はアイコンの有無に関わらず一律 16dp 相当のインセットで描画され、Cell ごとにインセット幅が変動してはならない（iOS の固定 16pt と同じ方針）

### Requirement: CheckboxCell の右端整列強化

`settings-view-android-ui` は `CheckboxCellViewHolder` で `MaterialCheckBox` に明示サイズ（24dp × 24dp）の `LayoutParams` を設定しなければならない (MUST)。`setPadding(0, 0, 0, 0)` および `minimumWidth = 0` / `minimumHeight = 0` の設定は維持する。さらに必要に応じて `marginEnd` の微調整を行い、`SwitchCell` / `RadioCell` / `SimpleCheckCell` の各右端 X 座標と ±1px 以内に一致させなければならない (MUST)。

#### Scenario: 24dp 明示サイズの適用

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)`
- **WHEN** Android で描画される
- **THEN** `MaterialCheckBox` の `layoutParams.width == (24 * density).toInt()` かつ `height == (24 * density).toInt()` となる

#### Scenario: 4 種アクセサリの右端整列

- **GIVEN** 同じ画面に `SwitchCell`、`CheckboxCell`、`RadioCell`、`SimpleCheckCell` を順に並べた状態
- **WHEN** Android で描画される
- **THEN** 各 Cell の右端アクセサリ（Switch / CheckBox / チェックマーク / SimpleCheck）の右端 X 座標は ±1px 以内で一致する

### Requirement: Section Header / Footer の垂直配置

`settings-view-android-ui` の Section Header / Footer 描画（`SectionTextAccessoryViewHolder` 経路）は、AiForms.Maui.SettingsView オリジナル iOS 側 `TextHeaderView.cs` の挙動（既定 = `LayoutAlignment.End`）と視覚的に揃え、以下の垂直配置を実装しなければならない (MUST)。

- **Section Header** の TextView は `Gravity.BOTTOM or Gravity.START` で **下端揃え**にする (MUST)。
- **Section Footer** の TextView は `Gravity.TOP or Gravity.START` で **上端揃え**にする (MUST)。

これにより、Header テキストは直下の Cell とぴったり接する位置に表示され、Footer テキストは直上の Cell とぴったり接する位置に表示される。iOS との視覚的な整合性も担保される。

#### Scenario: Section Header の下揃え

- **GIVEN** `Section(header = "CommandCell", headerHeight = 60.0, ...)` を持つセクション
- **WHEN** Android で描画される
- **THEN** Header の "CommandCell" テキストは固定 60dp の TextView 領域の **下端**に張り付くように配置される

#### Scenario: Section Footer の上揃え

- **GIVEN** `Section(footer = "You can select either TypeA or TypeB.", ...)`
- **WHEN** Android で描画される
- **THEN** Footer テキストは TextView 領域の **上端**に張り付くように配置される

### Requirement: Section.headerHeight の UI 反映

`settings-view-android-ui` は `Section.headerHeight: Double` の値を Header の描画高さに反映しなければならない (MUST)。

- `headerHeight > 0` → `SectionTextAccessoryViewHolder` の `itemView.layoutParams.height = (headerHeight * displayMetrics.density).toInt()` を明示設定して **固定高さ**で描画する (MUST)。
- `headerHeight == -1`（既定）→ `itemView.layoutParams.height = WRAP_CONTENT` のまま、テキスト寸法に応じた自動高さで描画する。

実装の最小要件として、`CellListItem.SectionHeader` データクラスに `headerHeight: Double` フィールドを追加し、`KsSettingsView.flatten()` から `section.headerHeight` を伝搬する。`SectionTextAccessoryViewHolder.bind()` で受け取った値を `layoutParams.height` に反映する。

**Rationale**: 従来は `Section.headerHeight` が Core 側に存在するだけで UI 層に伝搬しておらず、Sample で `headerHeight = 40.0` / `60.0` を指定しても他セクションと同じ自動高さで描画されていた（実機画像 Image #10 で確認）。本要件で UI 層への伝搬経路と高さ適用を明示する。

#### Scenario: headerHeight 正値による固定高さ

- **GIVEN** `Section(header = "CommandCell", headerHeight = 60.0, ...)`
- **WHEN** Android で描画される
- **THEN** Header の表示高さが 60dp に density を掛けた px 値で固定され、他セクション（既定 `-1`）と明らかに異なる高さで描画される

#### Scenario: headerHeight = -1 既定値の自動高さ

- **GIVEN** `Section(header = "LabelCell", ...)`（既定 `headerHeight = -1.0`）
- **WHEN** Android で描画される
- **THEN** Header の `itemView.layoutParams.height` は `WRAP_CONTENT` のままで、テキスト寸法に応じた自動高さで描画される

### Requirement: 基本 Cell 共通の垂直パディング

`settings-view-android-ui` の基本 Cell 7 種（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）のコンテナ View（`LinearLayout`）は、AiForms.Maui.SettingsView オリジナル `Platforms/Android/Resources/layout/cellbaseview.axml` の `paddingTop="4dp"` / `paddingBottom="4dp"` に揃え、**上下パディングを 4dp**（density を掛けた px 値）に設定しなければならない (MUST)。

- 横方向のパディングは標準左マージン 16dp を維持する。
- 各 Cell の最低高さ（`minimumHeight` 経由）は `EffectiveStyle.effectiveHeightDp`（既定 44dp = `MinRowHeight`）で保証されるため、視覚的な行高さは大きく変わらず、AiForms オリジナルの密度に揃う。

これにより iOS スクリーンショット（Image #8）および AiForms.Maui.SettingsView オリジナル（Image #11）と Android（Image #10）の上下密度差が解消される。

#### Scenario: 基本 Cell の垂直パディングが 4dp

- **GIVEN** 任意の基本 Cell（例: `LabelCell`）
- **WHEN** Android で描画される
- **THEN** `container.paddingTop == (4 * density).toInt()` かつ `container.paddingBottom == (4 * density).toInt()` であり、`paddingLeft` / `paddingRight` は引き続き `(16 * density).toInt()`


### Requirement: Theme 型 (UI 層)

`ks-settingsview-ui` モジュールは、SettingsView 全体に適用される論理スタイルを保持する値型 `Theme` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- 全体背景・装飾: `separatorColor`、`backgroundColor`、`cellBackgroundColor`、`selectedColor`、`cellAccentColor`、`disabledTextColor`、`scrollIndicatorVisible`
- 行高さ: `rowHeight`、`hasUnevenRows`
- Header: `headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`headerFont`、`headerHeight`
- Footer: `footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`footerFont`
- Cell 全体既定: `cellTitleColor`、`cellTitleFont`、`cellTitleFontSize`、`cellValueTextColor`、`cellValueTextFont`、`cellDescriptionColor`、`cellDescriptionFont`、`cellHintTextColor`、`cellHintFont`、`cellIconSize`、`cellIconRadius`

**フィールド型は Compose Native 型 `androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` / `androidx.compose.ui.unit.Dp` を直接保持しなければならない (MUST)。** `KsColor` / `KsFont` などの中間論理表現を経由してはならない (MUST NOT)。

#### リネーム

**従前の `viewBackgroundColor` は `backgroundColor` にリネームされる (MUST)**。互換シム（旧名 deprecated 残し）を提供してはならない (MUST NOT)。同様に、**従前の `titleColor` は `cellTitleColor` にリネームされる (MUST)** ことで、オリジナル `AiForms.Maui.SettingsView.SettingsView.CellTitleColor` と命名整合する。`titleFont` も同じ整合のため `cellTitleFont` にリネームする (MUST)。

#### Cell タイトル系

`cellTitleColor` は Cell タイトルの既定色を表す nullable `Color?` でなければならない (MUST)。未指定（`null`）のとき UI 層は `TextView` 既定色（`android.R.attr.textColorPrimary` 相当）にフォールバックする。

`cellTitleFont` は Cell タイトルの既定フォントを表す nullable `TextStyle?` でなければならない (MUST)。未指定のとき UI 層は `TextView` 既定フォントにフォールバックする。

`cellTitleFontSize: Double` は Cell タイトルの **既定フォントサイズ単独** を表す独立フィールドで、既定値は `-1.0`（未指定）でなければならない (MUST)。`cellTitleFont` と `cellTitleFontSize` が両方非 nil / `-1.0` 以外のとき、**`cellTitleFontSize` を size として優先**しなければならない (MUST)。すなわち最終的に描画される size は `cellTitleFontSize > 0 ? cellTitleFontSize.sp : cellTitleFont.fontSize` となる。

#### Cell 説明・値・ヒント・アイコン系（新規追加）

`cellValueTextColor: Color?` は LabelCell / CommandCell の valueText（および description / valueText を持つ後続 Cell）の **全体既定色** を表す。未指定（`null`）のとき UI 層は `Theme.cellTitleColor` または `TextView` 既定にフォールバックする。

`cellValueTextFont: TextStyle?` は valueText の **全体既定フォント** を表す。未指定のとき UI 層は `Theme.cellTitleFont` または既定にフォールバックする。

`cellDescriptionColor: Color?` は description の **全体既定色** を表す。未指定のとき UI 層は既定（やや薄いグレー、`Color(0xFF6D6D72)` 相当）にフォールバックする。

`cellDescriptionFont: TextStyle?` は description の **全体既定フォント** を表す。未指定のとき UI 層は既定（caption 系）にフォールバックする。

`cellHintTextColor: Color?` は hintText の **全体既定色** を表す。未指定のとき UI 層は既定（accent 色相当）にフォールバックする。

`cellHintFont: TextStyle?` は hintText の **全体既定フォント** を表す。未指定のとき UI 層は既定にフォールバックする。

`cellIconSize: Dp?` は icon の **全体既定サイズ**（正方形の一辺 dp）を表す。未指定のとき UI 層は既定（24dp）にフォールバックする。`CellStyle.iconSize: Dp?` と型を一致させ、`EffectiveStyle.effectiveIconSize` の結果も `Dp`（一辺）を返すことで「icon は正方形」というアイコン表現の前提を spec レベルで揃える。オリジナル `AiForms.Maui.SettingsView.SettingsView.CellIconSize`（`Size` 型）に対しては、本実装では「`Width` と `Height` のうち大きい方を使うか、Width のみを採用する」とは限定せず、`CellStyle.iconSize` 設計に従って **一辺スカラー** に簡素化する。

`cellIconRadius: Dp?` は icon の **全体既定角丸半径** を表す。未指定のとき UI 層は既定（0dp = 角丸なし）にフォールバックする。

#### Header / Footer Font 系（新規追加）

`headerFont: TextStyle?` は Section Header の **全体既定フォント**（family / weight / 装飾を含む）を表す nullable フィールドでなければならない (MUST)。未指定のとき UI 層は既存 `headerFontSize` のみで描画する。`headerFontSize > 0` かつ `headerFont != null` のとき、**`headerFontSize` を size として優先**する (MUST)。

`footerFont: TextStyle?` は Section Footer の **全体既定フォント** を表す。挙動は `headerFont` と同じく `footerFontSize` 優先である (MUST)。

`headerHeight: Double` は SettingsView 全体に適用される Section Header の **既定高さ**（論理単位）を表し、既定値は `-1.0`（未指定 = 自動）でなければならない (MUST)。Section ごとの `Section.headerHeight` が `-1.0` のときは本値を採用する。

#### 既存維持

`backgroundColor` は SettingsView（`RecyclerView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した `Color` でなければならない (MUST)。

`rowHeight` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MIN_ROW_HEIGHT_DP = 60dp` を未指定時の base として採用する（オリジナル `AiForms.Maui.SettingsView.AiRecyclerView` 踏襲）。最終下限も同じ `MIN_ROW_HEIGHT_DP = 60dp`（44dp は廃止、Phase 11 にて 60dp 一本に統一）。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える `Boolean` でなければならない (SHALL)。**デフォルト値は `true`** とする (MUST)。これによりオリジナル `AiForms.Maui.SettingsView` の `AiRecyclerView`（`RowHeight = -1` のとき自動で `60` をセットしつつ MinHeight 扱い）の挙動と整合した「Auto 高さ + 下限保証」既定挙動が得られる。「全 Cell を一律固定高さで揃えたい」用途では利用者が `Theme(hasUnevenRows = false)` を明示指定する。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `Color` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、`Double`）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Kotlin `data class` として `equals` / `hashCode` を自動取得する (MUST)。Compose `Color` は `@JvmInline value class Color(val value: ULong)` のため `data class` のフィールドとして自然に `equals` / `hashCode` に参加する。`TextStyle` は通常のクラスだが `equals` を実装しているため `data class` のフィールドとして利用可能。

#### Scenario: Theme のデフォルト値

- **GIVEN** 引数なしコンストラクタ
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`backgroundColor` は白系、`rowHeight = -1`、`hasUnevenRows = true`、`headerFontSize = -1`、`footerFontSize = -1`、`headerHeight = -1.0`、`disabledTextColor` はやや薄い灰色、`cellTitleColor = null`、`cellTitleFont = null`、`cellTitleFontSize = -1.0`、新規フィールド（`cellValueTextColor` / `cellValueTextFont` / `cellDescriptionColor` / `cellDescriptionFont` / `cellHintTextColor` / `cellHintFont` / `cellIconSize` / `cellIconRadius` / `headerFont` / `footerFont`）はすべて `null`

#### Scenario: viewBackgroundColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().viewBackgroundColor` を参照する Kotlin コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（旧名は完全に削除され、互換シムも提供されない）

#### Scenario: titleColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().titleColor` を参照する Kotlin コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（`cellTitleColor` への書き換えが必須）

#### Scenario: cellTitleColor / cellTitleFont の nullable 性

- **GIVEN** `Theme()` の `cellTitleColor` / `cellTitleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも nullable（`Color?` / `TextStyle?`）であり、既定値は `null` である

#### Scenario: cellTitleFontSize 既定値

- **GIVEN** `Theme()` の `cellTitleFontSize` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定」を表す

#### Scenario: cellTitleFontSize と cellTitleFont 併設時の size 優先

- **GIVEN** `Theme(cellTitleFont = TextStyle(fontSize = 14.sp), cellTitleFontSize = 20.0)`
- **WHEN** UI 層が Cell タイトルを描画する
- **THEN** 最終的な size は **20.0sp 相当**（`cellTitleFontSize` 優先）で描画され、`cellTitleFont` の `fontSize = 14.sp` は無視される。family / weight など `cellTitleFont` の他属性は維持される

#### Scenario: 新規 Cell 全体既定フィールドの保持

- **GIVEN** `Theme(cellHintTextColor = Color.Red, cellIconSize = 32.dp)`
- **WHEN** 値を参照する
- **THEN** `cellHintTextColor` は `Color.Red`、`cellIconSize` は `32.dp`（一辺）を返す

#### Scenario: headerHeight 既定値

- **GIVEN** `Theme()` の `headerHeight` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定 = 自動」を表す

#### Scenario: backgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(backgroundColor = Color(0xFFF2EFE6), cellBackgroundColor = Color.White)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色は `backgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

#### Scenario: Native 型を直接保持

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `KsColor` や独自論理色型を経由せず、`Color` / `TextStyle` / `Dp` を直接保持する

#### Scenario: rowHeight / hasUnevenRows の既定組み合わせ

- **GIVEN** `Theme()`（未指定）
- **WHEN** `theme.rowHeight` と `theme.hasUnevenRows` を参照する
- **THEN** それぞれ `-1` と `true` が返る（UI 層はこの組み合わせを「Auto 高さ + 最低高さ 60dp」と解釈する）

#### Scenario: 利用者が Compose Color をそのまま渡せる

- **GIVEN** 利用者コード `Theme(separatorColor = Color(0xFFE6DAB9), cellBackgroundColor = Color.White)`
- **WHEN** コンパイル・実行する
- **THEN** ビルドエラーなく構築でき、`KsColor` などの中間型を書く必要がない

### Requirement: CellStyle 型 (UI 層)

`ks-settingsview-ui` モジュールは、単一 Cell に適用されるスタイルを表す値型 `CellStyle` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- `titleColor: Color?`
- `titleFont: TextStyle?`
- `descriptionColor: Color?`
- `descriptionFont: TextStyle?`
- `valueTextColor: Color?`
- `valueTextFont: TextStyle?`
- `iconSize: Dp?`
- `iconRadius: Dp?`
- `cellHeight: Dp?`
- `hintTextColor: Color?`
- `hintTextFont: TextStyle?`
- `backgroundColor: Color?`
- `accentColor: Color?`

**色・フォント系フィールドは `Color?` / `TextStyle?` を直接保持しなければならない (MUST)。`iconSize` / `iconRadius` / `cellHeight` は `Dp?` でなければならない (MUST)。**

CellStyle のフィールドはいずれも `null` を取りうる Optional であり、`null` のとき UI 層は **`Theme` の対応する全体既定フィールド**（解決順序: `Theme.cellTitleColor` / `Theme.cellTitleFont` / `Theme.cellTitleFontSize` / `Theme.cellValueTextColor` / `Theme.cellValueTextFont` / `Theme.cellDescriptionColor` / `Theme.cellDescriptionFont` / `Theme.cellHintTextColor` / `Theme.cellHintFont` / `Theme.cellIconSize` / `Theme.cellIconRadius` / `Theme.cellBackgroundColor` / `Theme.cellAccentColor`）にフォールバックしなければならない (MUST)。`Theme` 側も未指定の場合は UI 層既定値（Native プラットフォーム既定または本 spec の他 Requirement で定義された値）を用いる。

`CellStyle` は Kotlin `data class` として定義され、`equals` / `hashCode` を自動取得する。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** 引数なしコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`null`）」となり、`Theme` から継承される

#### Scenario: Native 型を直接保持

- **GIVEN** `CellStyle` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `Color?` / `TextStyle?` / `Dp?` を直接保持する

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor = Color.Red)` を持つ Cell と `Theme(cellBackgroundColor = Color.White)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor = Color.Green)` を持つ Cell と `Theme(cellAccentColor = Color.Blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: hintTextColor の Theme フォールバック

- **GIVEN** `CellStyle(hintTextColor = null)` を持つ Cell と `Theme(cellHintTextColor = Color.Red)`
- **WHEN** UI 層が実効 hintText 色を計算する
- **THEN** 実効 hintText 色は **赤**（`Theme.cellHintTextColor` から落ちてくる）になる

#### Scenario: iconSize の Theme フォールバック

- **GIVEN** `CellStyle(iconSize = null)` を持つ Cell と `Theme(cellIconSize = 32.dp)`
- **WHEN** UI 層が実効 iconSize を計算する
- **THEN** 実効 iconSize は **`32.dp`（一辺）**（`Theme.cellIconSize` から落ちてくる）になる

### Requirement: EffectiveStyle の解決順序

`ks-settingsview-ui` モジュールは、Cell 描画時の最終スタイル値を「CellStyle → Theme → 既定」の 3 段で解決するユーティリティ `EffectiveStyle` を提供しなければならない (SHALL)。`EffectiveStyle` は各 Cell プロパティに対応する **アクセサ関数** を提供し、各 Cell ViewHolder の bind 処理から呼び出されなければならない (MUST)。

解決順序：

```
最終値 = cellStyle.X            if X != null
       else theme.cellX         if cellX != null
       else プラットフォーム既定（本 spec の他 Requirement または UI 層内の既定値）
```

`titleFontSize` のみ特殊で、`theme.cellTitleFontSize` が `> 0` の場合は `cellTitleFont.fontSize` を **上書き** する。

EffectiveStyle は以下のアクセサを最低限提供しなければならない (MUST)：

- `effectiveTitleColor(cellStyle, theme): Color`
- `effectiveTitleFont(cellStyle, theme): TextStyle`（fontSize は `cellTitleFontSize` で上書きされた最終値）
- `effectiveDescriptionColor(cellStyle, theme): Color`
- `effectiveDescriptionFont(cellStyle, theme): TextStyle`
- `effectiveValueTextColor(cellStyle, theme): Color`
- `effectiveValueTextFont(cellStyle, theme): TextStyle`
- `effectiveHintTextColor(cellStyle, theme): Color`
- `effectiveHintFont(cellStyle, theme): TextStyle`
- `effectiveIconSize(cellStyle, theme): Dp`（icon は正方形、一辺 dp を返す）
- `effectiveIconRadius(cellStyle, theme): Dp`
- `effectiveBackgroundColor(cellStyle, theme): Color`
- `effectiveAccentColor(cellStyle, theme): Color`
- `effectiveCellHeightDp(cellStyle, theme): Int`（既存）

`ButtonCell.titleColor` のみ特殊で、Cell 個別の `titleColor` フィールドを **最優先** とする 4 段解決を維持する（既存 cell-types-basic spec 規約を尊重）：

```
ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → プラットフォーム既定
```

#### Scenario: 通常 Cell の解決順序（CellStyle 優先）

- **GIVEN** `CellStyle(titleColor = Color.Red)` を持つ LabelCell と `Theme(cellTitleColor = Color.Blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`Color.Red`**（CellStyle が優先される）

#### Scenario: 通常 Cell の解決順序（Theme フォールバック）

- **GIVEN** `CellStyle(titleColor = null)` を持つ LabelCell と `Theme(cellTitleColor = Color.Blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`Color.Blue`**（Theme から落ちてくる）

#### Scenario: 通常 Cell の解決順序（既定フォールバック）

- **GIVEN** `CellStyle(titleColor = null)` を持つ LabelCell と `Theme(cellTitleColor = null)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **プラットフォーム既定の TextView title 色相当**（例: `Color(0xFF1C1B1F)` 相当の Material on-surface 色）

#### Scenario: cellTitleFontSize 優先

- **GIVEN** `CellStyle(titleFont = null)` を持つ Cell と `Theme(cellTitleFont = TextStyle(fontSize = 14.sp), cellTitleFontSize = 20.0)`
- **WHEN** `EffectiveStyle.effectiveTitleFont(cellStyle, theme).fontSize` を取得する
- **THEN** `20.sp` 相当（`cellTitleFontSize` で size が上書きされる）

#### Scenario: ButtonCell.titleColor の 4 段解決（Cell 個別最優先）

- **GIVEN** `ButtonCell(titleColor = Color.Red)`、`CellStyle(titleColor = Color.Green)`、`Theme(cellTitleColor = Color.Blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`Color.Red`**（ButtonCell.titleColor が最優先）

#### Scenario: ButtonCell.titleColor が null の場合は CellStyle 経由

- **GIVEN** `ButtonCell(titleColor = null)`、`CellStyle(titleColor = Color.Green)`、`Theme(cellTitleColor = Color.Blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`Color.Green`**（CellStyle.titleColor から落ちる）

#### Scenario: fontFamily 反映の e2e

- **GIVEN** カスタム `FontFamily` インスタンス `myFamily` を使う `Theme(cellTitleFont = TextStyle(fontFamily = myFamily))` を設定した KsSettingsView
- **WHEN** LabelCell を描画する
- **THEN** Cell 内の title テキストの `fontFamily` 状態には **同一の `myFamily` インスタンス**が流れており、`==` で等価判定が成立する。フォントが既定 (sans-serif) にフォールバックしない

#### Scenario: fontSize 反映の e2e

- **GIVEN** `Theme(cellTitleFont = TextStyle(fontSize = 24.sp))` を設定した KsSettingsView
- **WHEN** LabelCell を描画し、レイアウト後の title TextView の measured height を取得する
- **THEN** measured height は 24sp に density を掛けた値に近い（`12.sp * density` 比で明確に大きい）。`cellTitleFontSize` 未指定下で、`fontSize` がレイアウトに反映されている

### Requirement: KsImage 型 (UI 層)

`ks-settingsview-ui` モジュールは、Cell のアイコン表現に用いる sealed 型 `KsImage` を提供しなければならない (SHALL)。`KsImage` は Kotlin `sealed interface` として定義され、以下のサブ型を持たなければならない (MUST)：

- `data class Resource(@DrawableRes val resId: Int) : KsImage`: Android リソース ID
- `class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage`: 任意の Drawable
- `data class SystemName(val name: String) : KsImage`: iOS SF Symbols 名（Android では解決不可、フォールバック対象）

UI 層は派生に応じて以下を行わなければならない (MUST)：

1. `null` → アイコン領域を `View.GONE` で非表示
2. `KsImage.Drawable` → `setImageDrawable(it.drawable)`
3. `KsImage.Resource` → `ContextCompat.getDrawable(context, it.resId)` で解決して `setImageDrawable`
4. `KsImage.SystemName` → 解決不可。`View.GONE` でフォールバック

`Drawable` サブ型は参照同一性で `equals` / `hashCode` を実装する。`Resource` / `SystemName` は `data class` の自動 `equals` / `hashCode` を用いる。

#### Scenario: 型の所属

- **GIVEN** `KsImage` の所属モジュール
- **WHEN** import 文を書く
- **THEN** `import jp.kamusoft.kssettingsview.ui.KsImage` で解決できる。`import jp.kamusoft.kssettingsview.core.KsImage` は存在しない

#### Scenario: KsImage.Resource の解決

- **GIVEN** `LabelCell(icon = KsImage.Resource(R.drawable.ic_storage))`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域に `ContextCompat.getDrawable(context, R.drawable.ic_storage)` が `setImageDrawable` で設定される

#### Scenario: KsImage.Drawable の解決

- **GIVEN** `LabelCell(icon = KsImage.Drawable(customDrawable))`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域に渡された `customDrawable` がそのまま `setImageDrawable` で設定される

#### Scenario: KsImage.SystemName のフォールバック

- **GIVEN** `LabelCell(icon = KsImage.SystemName("bell"))`
- **WHEN** Android で描画される
- **THEN** UI 層は解決できず、アイコン領域は `View.GONE` でフォールバックする。エラーログや throw は発生しない

#### Scenario: icon = null

- **GIVEN** `LabelCell(icon = null)`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域は `View.GONE` で非表示となる
