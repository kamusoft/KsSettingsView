## ADDED Requirements

### Requirement: Theme 型 (UI 層)

`ks-settingsview-ui` モジュールは、SettingsView 全体に適用される論理スタイルを保持する値型 `Theme` を提供しなければならない (SHALL)。最低限、`separatorColor`、`cellBackgroundColor`、`viewBackgroundColor`、`selectedColor`、`cellAccentColor`、`titleColor`、`titleFont`、`headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`disabledTextColor`、`rowHeight`、`hasUnevenRows`、`scrollIndicatorVisible` を含まなければならない (MUST)。

**フィールド型は Compose Native 型 `androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` を直接保持しなければならない (MUST)。** `KsColor` / `KsFont` などの中間論理表現を経由してはならない (MUST NOT)。

`titleColor` は Cell タイトルの既定色を表す nullable `Color?` でなければならない (MUST)。未指定（`null`）のとき UI 層は `TextView` 既定色（`android.R.attr.textColorPrimary` 相当）にフォールバックする。

`titleFont` は Cell タイトルの既定フォントを表す nullable `TextStyle?` でなければならない (MUST)。未指定のとき UI 層は `TextView` 既定フォントにフォールバックする。

`viewBackgroundColor` は SettingsView（`RecyclerView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した `Color` でなければならない (MUST)。

`rowHeight` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（Android 44dp）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える `Boolean` でなければならない (SHALL)。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `Color` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、`Double`）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Kotlin `data class` として `equals` / `hashCode` を自動取得する (MUST)。Compose `Color` は `@JvmInline value class Color(val value: ULong)` のため `data class` のフィールドとして自然に `equals` / `hashCode` に参加する。`TextStyle` は通常のクラスだが `equals` を実装しているため `data class` のフィールドとして利用可能。

#### Scenario: Theme のデフォルト値

- **GIVEN** 引数なしコンストラクタ
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`viewBackgroundColor` は白系、`rowHeight = -1`、`hasUnevenRows = false`、`headerFontSize = -1`、`footerFontSize = -1`、`disabledTextColor` はやや薄い灰色（例: `Color(0xFF999999)` 相当）、`titleColor = null`、`titleFont = null` を取る

#### Scenario: titleColor / titleFont の nullable 性

- **GIVEN** `Theme()` の `titleColor` / `titleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも nullable（`Color?` / `TextStyle?`）であり、既定値は `null` である

#### Scenario: Native 型を直接保持

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `KsColor` や独自論理色型を経由せず、`androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` を直接保持する

#### Scenario: viewBackgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(viewBackgroundColor = Color(0xFFF2EFE6), cellBackgroundColor = Color.White)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色は `viewBackgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

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

`backgroundColor` は Cell 個別の背景色を表し、未指定（`null`）のとき `Theme.cellBackgroundColor` で補完されなければならない (MUST)。

`accentColor` は Cell 個別の accent 色を表し、未指定のとき `Theme.cellAccentColor` で補完されなければならない (MUST)。

`valueTextColor` / `valueTextFont` は LabelCell / CommandCell の `valueText` に適用する色／フォントを表し、未指定のとき `Theme.descriptionColor` / `Theme.descriptionFont` または UI 層既定で補完されなければならない (MUST)。

`CellStyle` は Kotlin `data class` として定義され、`equals` / `hashCode` を自動取得する。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** 引数なしコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`null`）」となり、`Theme` から継承される

#### Scenario: Native 型を直接保持

- **GIVEN** `CellStyle` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `Color?` / `TextStyle?` を直接保持する

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor = Color.Red)` を持つ Cell と `Theme(cellBackgroundColor = Color.White)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor = Color.Green)` を持つ Cell と `Theme(cellAccentColor = Color.Blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

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
