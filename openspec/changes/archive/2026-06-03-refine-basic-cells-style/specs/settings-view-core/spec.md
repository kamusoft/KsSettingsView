## MODIFIED Requirements

### Requirement: Theme 型

`Theme` は SettingsView 全体に適用される論理スタイルを保持する値型でなければならない (SHALL)。最低限、`separatorColor`、`cellBackgroundColor`、`viewBackgroundColor`、`selectedColor`、`cellAccentColor`、`titleColor`、`titleFont`、`headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`disabledTextColor`、`rowHeight`、`hasUnevenRows`、`scrollIndicatorVisible` を含まなければならない (MUST)。

`titleColor` は Cell タイトルの既定色を表す Optional（`KsColor?` / `nullable`）でなければならない (MUST)。未指定（`nil` / `null`）のとき UI 層はプラットフォーム既定（iOS: `UIColor.label`、Android: `TextView` の既定色）にフォールバックする。原典 AiForms.Maui.SettingsView の `Theme.CellTitleColor` 相当の責務を持つ。

`titleFont` は Cell タイトルの既定フォントを表す Optional（`KsFont?` / `nullable`）でなければならない (MUST)。未指定のとき UI 層はプラットフォーム既定フォント（iOS: `UIFont.preferredFont(forTextStyle: .body)`、Android: `TextView` の既定フォント）にフォールバックする。原典 AiForms.Maui.SettingsView の `Theme.CellTitleFont` 相当の責務を持つ。

`viewBackgroundColor` は SettingsView（`UICollectionView` / `RecyclerView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した色でなければならない (MUST)。原典 AiForms.Maui.SettingsView の `SettingsView.BackgroundColor` 相当の責務を持つ。

`rowHeight` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（iOS 48pt / Android 44dp）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える Bool でなければならない (SHALL)。原典 `SettingsView.HasUnevenRows` 相当。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための色でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、Double）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（Swift: パラメータなし init、Kotlin: 引数なし）
- **WHEN** `Theme()` を構築する
- **THEN** 中立的なクロスプラットフォーム既定値を持つ。具体的には `viewBackgroundColor` は `cellBackgroundColor` と同等の白系、`rowHeight = -1`、`hasUnevenRows = false`、`headerFontSize = -1`、`footerFontSize = -1`、`disabledTextColor` はやや薄い灰色（例: `KsColor(0.6, 0.6, 0.6, 1.0)` 相当）、`titleColor = nil` / `null`、`titleFont = nil` / `null` を取る

#### Scenario: titleColor / titleFont の Optional 性

- **GIVEN** `Theme()` の `titleColor` / `titleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも Optional（Swift: `KsColor?` / `KsFont?`、Kotlin: nullable）であり、既定値は `nil` / `null` である

#### Scenario: プラットフォーム型を持たない

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `UIColor` や `android.graphics.Color` を直接持たず、論理表現（例: `KsColor` の独自 RGBA 値型、フォントファミリ名と weight を表す `KsFont`）のみを保持する

#### Scenario: viewBackgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(viewBackgroundColor: KsColor(0.95, 0.93, 0.90, 1.0), cellBackgroundColor: .white)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色（セル間／セクション間に見える背景）は `viewBackgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

#### Scenario: rowHeight / hasUnevenRows の既定組み合わせ

- **GIVEN** `Theme()`（未指定）
- **WHEN** `theme.rowHeight` と `theme.hasUnevenRows` を参照する
- **THEN** それぞれ `-1` と `false` が返る（UI 層はこの組み合わせを「固定高さ・最低高さ MinRowHeight」と解釈する）

#### Scenario: API 互換性（既存呼び出し）

- **GIVEN** 既存のコード `Theme(separatorColor: ..., cellBackgroundColor: ..., selectedColor: ...)`（新フィールドを指定しない呼び出し）
- **WHEN** コンパイル・実行する
- **THEN** 新フィールドはすべてデフォルト値が適用され、ビルドエラーや実行時エラーは発生しない

### Requirement: CellStyle 型

`CellStyle` は単一 Cell に適用されるスタイルを表す値型でなければならない (SHALL)。最低限、`titleColor`、`titleFont`、`descriptionColor`、`descriptionFont`、`valueTextColor`、`valueTextFont`、`iconSize`、`iconRadius`、`cellHeight`、`hintTextColor`、`hintTextFont`、`backgroundColor`、`accentColor` を含まなければならない (MUST)。

`backgroundColor` は Cell 個別の背景色を表し、未指定（`nil` / `null`）のとき `Theme.cellBackgroundColor` で補完されなければならない (MUST)。

`accentColor` は Cell 個別の accent 色（CheckboxCell の塗り、RadioCell / SimpleCheckCell のチェックマーク、SwitchCell のスイッチ ON 色など）を表し、未指定のとき `Theme.cellAccentColor` で補完されなければならない (MUST)。

`valueTextColor` / `valueTextFont` は LabelCell / CommandCell の `valueText`（右寄せ表示の値）に適用する色／フォントを表し、未指定のとき `Theme.descriptionColor` / `Theme.descriptionFont` または UI 層既定で補完されなければならない (MUST)。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** デフォルトコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`nil` / `null`）」となり、`Theme` から継承される

#### Scenario: Theme との継承関係

- **GIVEN** `CellStyle` のあるフィールドが「未指定」、対応する `Theme` のフィールドに値あり
- **WHEN** UI 層が描画用に「実効スタイル」を計算する
- **THEN** `CellStyle` 未指定フィールドは `Theme` の値で補完される（実効スタイル合成は UI 層の責務であるため、Core ではフィールドが nullable / Optional で定義されていることのみが Core 仕様の対象）

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor: KsColor.red)` を持つ Cell と `Theme(cellBackgroundColor: KsColor.white)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor: KsColor.green)` を持つ Cell と `Theme(cellAccentColor: KsColor.blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: API 互換性（既存呼び出し）

- **GIVEN** 既存のコード `CellStyle(titleColor: ..., titleFont: ...)`（新フィールドを指定しない呼び出し）
- **WHEN** コンパイル・実行する
- **THEN** 新フィールドはすべてデフォルト値（`nil` / `null`）が適用され、ビルドエラーや実行時エラーは発生しない

## ADDED Requirements

### Requirement: CellTitleAlignment 列挙型

`KsSettingsViewCore` は `CellTitleAlignment` という公開列挙型を定義しなければならない (SHALL)。`start`、`center`、`end` の 3 ケースを持たなければならない (MUST)。これは `ButtonCell.titleAlignment` などのフィールドで使用される。

#### Scenario: 3 ケースの定義

- **GIVEN** `KsSettingsViewCore` モジュールをインポート
- **WHEN** `CellTitleAlignment` を参照する
- **THEN** `CellTitleAlignment.start` / `.center` / `.end` の 3 ケースがすべて参照可能で、Swift `enum` または Kotlin `enum class` として宣言されている

#### Scenario: Hashable / equals 契約

- **GIVEN** `CellTitleAlignment.center` の 2 つの参照
- **WHEN** 等価性を比較する
- **THEN** Swift `==` および Kotlin `equals()` で `true` を返す
