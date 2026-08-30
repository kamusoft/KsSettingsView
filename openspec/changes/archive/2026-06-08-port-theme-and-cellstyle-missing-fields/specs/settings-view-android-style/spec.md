## MODIFIED Requirements

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

`rowHeight` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（Android 44dp）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える `Boolean` でなければならない (SHALL)。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `Color` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、`Double`）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Kotlin `data class` として `equals` / `hashCode` を自動取得する (MUST)。Compose `Color` は `@JvmInline value class Color(val value: ULong)` のため `data class` のフィールドとして自然に `equals` / `hashCode` に参加する。`TextStyle` は通常のクラスだが `equals` を実装しているため `data class` のフィールドとして利用可能。

#### Scenario: Theme のデフォルト値

- **GIVEN** 引数なしコンストラクタ
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`backgroundColor` は白系、`rowHeight = -1`、`hasUnevenRows = false`、`headerFontSize = -1`、`footerFontSize = -1`、`headerHeight = -1.0`、`disabledTextColor` はやや薄い灰色、`cellTitleColor = null`、`cellTitleFont = null`、`cellTitleFontSize = -1.0`、新規フィールド（`cellValueTextColor` / `cellValueTextFont` / `cellDescriptionColor` / `cellDescriptionFont` / `cellHintTextColor` / `cellHintFont` / `cellIconSize` / `cellIconRadius` / `headerFont` / `footerFont`）はすべて `null`

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
- **THEN** それぞれ `-1` と `false` が返る（UI 層はこの組み合わせを「固定高さ・最低高さ MinRowHeight」と解釈する）

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

## ADDED Requirements

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
