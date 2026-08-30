## ADDED Requirements

### Requirement: Theme 型 (UI 層)

`KsSettingsViewUI` モジュールは、SettingsView 全体に適用される論理スタイルを保持する値型 `Theme` を提供しなければならない (SHALL)。最低限、`separatorColor`、`cellBackgroundColor`、`viewBackgroundColor`、`selectedColor`、`cellAccentColor`、`titleColor`、`titleFont`、`headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`disabledTextColor`、`rowHeight`、`hasUnevenRows`、`scrollIndicatorVisible` を含まなければならない (MUST)。

**フィールド型は UIKit Native 型 `UIColor` / `UIFont` を直接保持しなければならない (MUST)。** `KsColor` / `KsFont` などの中間論理表現を経由してはならない (MUST NOT)。

`titleColor` は Cell タイトルの既定色を表す Optional `UIColor?` でなければならない (MUST)。未指定（`nil`）のとき UI 層は `UIColor.label` にフォールバックする。

`titleFont` は Cell タイトルの既定フォントを表す Optional `UIFont?` でなければならない (MUST)。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .body)` にフォールバックする。

`viewBackgroundColor` は SettingsView（`UICollectionView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した色でなければならない (MUST)。

`rowHeight: Int` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（iOS 48pt）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える Bool でなければならない (SHALL)。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `UIColor` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、Double）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Swift `struct` として `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` および `UIFont` は Swift の `Equatable` に準拠していないためコンパイラによる自動合成は不可能 (MUST NOT)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)`、`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` を用いた手動 `==` / `!=` 実装が必須 (MUST)。`Hashable` 準拠は必須としない。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（パラメータなし init）
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`viewBackgroundColor` は `UIColor.systemBackground` 系または `cellBackgroundColor` と同等の白系、`rowHeight = -1`、`hasUnevenRows = false`、`headerFontSize = -1`、`footerFontSize = -1`、`disabledTextColor` はやや薄い灰色（例: `UIColor(white: 0.6, alpha: 1)` 相当）、`titleColor = nil`、`titleFont = nil` を取る

#### Scenario: titleColor / titleFont の Optional 性

- **GIVEN** `Theme()` の `titleColor` / `titleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも Optional（`UIColor?` / `UIFont?`）であり、既定値は `nil` である

#### Scenario: Native 型を直接保持

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `KsColor` や独自論理色型を経由せず、`UIColor` / `UIFont` を直接保持する

#### Scenario: viewBackgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(viewBackgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0), cellBackgroundColor: .white)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色（セル間／セクション間に見える背景）は `viewBackgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

#### Scenario: rowHeight / hasUnevenRows の既定組み合わせ

- **GIVEN** `Theme()`（未指定）
- **WHEN** `theme.rowHeight` と `theme.hasUnevenRows` を参照する
- **THEN** それぞれ `-1` と `false` が返る（UI 層はこの組み合わせを「固定高さ・最低高さ MinRowHeight」と解釈する）

#### Scenario: 利用者が UIColor をそのまま渡せる

- **GIVEN** 利用者コード `Theme(separatorColor: UIColor.systemGray3, cellBackgroundColor: .white)`
- **WHEN** コンパイル・実行する
- **THEN** ビルドエラーなく構築でき、`KsColor` などの中間型を書く必要がない

### Requirement: CellStyle 型 (UI 層)

`KsSettingsViewUI` モジュールは、単一 Cell に適用されるスタイルを表す値型 `CellStyle` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- `titleColor: UIColor?`
- `titleFont: UIFont?`
- `descriptionColor: UIColor?`
- `descriptionFont: UIFont?`
- `valueTextColor: UIColor?`
- `valueTextFont: UIFont?`
- `iconSize: CGFloat?`
- `iconRadius: CGFloat?`
- `cellHeight: CGFloat?`
- `hintTextColor: UIColor?`
- `hintTextFont: UIFont?`
- `backgroundColor: UIColor?`
- `accentColor: UIColor?`

**色・フォント系フィールドは `UIColor?` / `UIFont?` を直接保持しなければならない (MUST)。`iconSize` / `iconRadius` / `cellHeight` は `CGFloat?` でなければならない (MUST)。**

`backgroundColor` は Cell 個別の背景色を表し、未指定（`nil`）のとき `Theme.cellBackgroundColor` で補完されなければならない (MUST)。

`accentColor` は Cell 個別の accent 色を表し、未指定のとき `Theme.cellAccentColor` で補完されなければならない (MUST)。

`valueTextColor` / `valueTextFont` は LabelCell / CommandCell の `valueText` に適用する色／フォントを表し、未指定のとき `Theme.descriptionColor` / `Theme.descriptionFont` または UI 層既定で補完されなければならない (MUST)。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** デフォルトコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`nil`）」となり、`Theme` から継承される

#### Scenario: Native 型を直接保持

- **GIVEN** `CellStyle` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `UIColor?` / `UIFont?` を直接保持する

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor: UIColor.red)` を持つ Cell と `Theme(cellBackgroundColor: UIColor.white)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor: UIColor.green)` を持つ Cell と `Theme(cellAccentColor: UIColor.blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

### Requirement: KsImage 型 (UI 層)

`KsSettingsViewUI` モジュールは、Cell のアイコン表現に用いる sealed 値型 `KsImage` を提供しなければならない (SHALL)。`KsImage` は Swift `enum` として定義され、以下のケースを持たなければならない (MUST)：

- `systemName(String)`: SF Symbols 名（例: `"bell"`、`"externaldrive"`）
- `uiImage(UIImage)`: 任意の `UIImage`（カスタムアセット等）

UI 層は派生に応じて以下を行わなければならない (MUST)：

- `.systemName(name)` → `UIImage(systemName: name)` で解決し、取得失敗時はアイコン非表示にフォールバック
- `.uiImage(image)` → `image` をそのまま設定

`KsImage` は `Hashable` プロトコルに準拠しなければならない (MUST)。実装は以下：

- `.systemName(s)`: 内部 String の hash 値で同定
- `.uiImage(img)`: 参照同一性（`ObjectIdentifier(img)`）で同定

#### Scenario: 型の所属

- **GIVEN** `KsImage` の所属モジュール
- **WHEN** import 文を書く
- **THEN** `import KsSettingsViewUI` で `KsImage` を解決できる。`import KsSettingsViewCore` のみでは解決できない

#### Scenario: systemName 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に `UIImage(systemName: "bell")` が描画される

#### Scenario: uiImage 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.uiImage(UIImage(named: "custom_icon")!))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に渡された UIImage がそのまま描画される

#### Scenario: Hashable 契約

- **GIVEN** 同一 `systemName` の 2 つの `KsImage.systemName` インスタンス
- **WHEN** 等価性とハッシュ値を比較する
- **THEN** 等価判定が真、ハッシュ値が一致する

### Requirement: Theme / CellStyle の Hashable / Equatable 契約

`KsSettingsViewUI` 層の `Theme` および `CellStyle` は Swift `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)` ベースで判定する。`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` ベースで判定する。

`Hashable` 準拠は必須としない。SettingsView の構造同期は Core の SettingsRoot id 同一性ベースで行われるため、Theme / CellStyle の Hashable は不要。テストや内部比較で必要な場合のみ手動実装する。

#### Scenario: Theme の Equatable

- **GIVEN** 同一フィールド値を持つ 2 つの `Theme` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価と判定される

#### Scenario: CellStyle の Equatable

- **GIVEN** 同一フィールド値を持つ 2 つの `CellStyle` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価と判定される
