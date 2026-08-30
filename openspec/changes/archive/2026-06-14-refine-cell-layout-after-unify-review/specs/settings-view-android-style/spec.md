## MODIFIED Requirements

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
