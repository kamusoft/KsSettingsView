## RENAMED Requirements

- FROM: `### Requirement: viewBackgroundColor のセクション間反映`
- TO: `### Requirement: backgroundColor のセクション間反映`

## MODIFIED Requirements

### Requirement: backgroundColor のセクション間反映

`settings-view-ios-ui` は `Theme.backgroundColor` を `UICollectionView` の背景色として設定する際、`UICollectionLayoutListConfiguration.backgroundColor` を `.clear` に設定しなければならない (MUST)。これにより `UICollectionView.backgroundColor` がセクション間の隙間（supplementary 領域・section inset）にも透過して反映され、`Theme.backgroundColor` が見える状態になる。

本 Requirement で参照する `Theme.backgroundColor` は、本 change 内で **`Theme.viewBackgroundColor` からリネーム** されたフィールドである（旧名 `viewBackgroundColor` は互換シムなしで削除されており、本 Requirement の本文・Scenario のいずれも旧名を参照してはならない (MUST NOT)）。

#### Scenario: backgroundColor がセクション間にも反映される

- **GIVEN** `Theme(backgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0), cellBackgroundColor: .white)` を適用した SettingsView
- **WHEN** iOS で描画される
- **THEN** 各 Cell の背景は `cellBackgroundColor` の白、Section 間（Header / Footer 領域および Section inset）の背景は `backgroundColor` の薄ベージュ色が反映される

#### Scenario: cellBackgroundColor の維持

- **GIVEN** 上記と同じ Theme
- **WHEN** Cell が描画される
- **THEN** Cell 自身の背景描画は `UIListContentConfiguration.backgroundConfiguration` 経由で `cellBackgroundColor` が維持され、`backgroundColor = .clear` の変更によって Cell の背景が消えてはならない

### Requirement: Theme 型 (UI 層)

`KsSettingsViewUI` モジュールは、SettingsView 全体に適用される論理スタイルを保持する値型 `Theme` を提供しなければならない (SHALL)。最低限、以下のフィールドを含まなければならない (MUST)：

- 全体背景・装飾: `separatorColor`、`backgroundColor`、`cellBackgroundColor`、`selectedColor`、`cellAccentColor`、`disabledTextColor`、`scrollIndicatorVisible`
- 行高さ: `rowHeight`、`hasUnevenRows`
- Header: `headerTextColor`、`headerBackgroundColor`、`headerFontSize`、`headerFont`、`headerHeight`
- Footer: `footerTextColor`、`footerBackgroundColor`、`footerFontSize`、`footerFont`
- Cell 全体既定: `cellTitleColor`、`cellTitleFont`、`cellTitleFontSize`、`cellValueTextColor`、`cellValueTextFont`、`cellDescriptionColor`、`cellDescriptionFont`、`cellHintTextColor`、`cellHintFont`、`cellIconSize`、`cellIconRadius`

**フィールド型は UIKit Native 型 `UIColor` / `UIFont` / `CGFloat` を直接保持しなければならない (MUST)。** `KsColor` / `KsFont` などの中間論理表現を経由してはならない (MUST NOT)。

#### リネーム

**従前の `viewBackgroundColor` は `backgroundColor` にリネームされる (MUST)**。互換シム（旧名 deprecated 残し）を提供してはならない (MUST NOT)。同様に、**従前の `titleColor` は `cellTitleColor` にリネームされる (MUST)** ことで、オリジナル `AiForms.Maui.SettingsView.SettingsView.CellTitleColor` と命名整合する。`titleFont` も同じ整合のため `cellTitleFont` にリネームする (MUST)。

#### Cell タイトル系

`cellTitleColor` は Cell タイトルの既定色を表す Optional `UIColor?` でなければならない (MUST)。未指定（`nil`）のとき UI 層は `UIColor.label` にフォールバックする。

`cellTitleFont` は Cell タイトルの既定フォントを表す Optional `UIFont?` でなければならない (MUST)。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .body)` にフォールバックする。

`cellTitleFontSize: Double` は Cell タイトルの **既定フォントサイズ単独** を表す独立フィールドで、既定値は `-1.0`（未指定）でなければならない (MUST)。`cellTitleFont` と `cellTitleFontSize` が両方非 nil / `-1.0` 以外のとき、**`cellTitleFontSize` を size として優先**しなければならない (MUST)。すなわち最終的に描画される size は `cellTitleFontSize > 0 ? CGFloat(cellTitleFontSize) : cellTitleFont.pointSize` となる。

#### Cell 説明・値・ヒント・アイコン系（新規追加）

`cellValueTextColor: UIColor?` は LabelCell / CommandCell の valueText（および description / valueText を持つ後続 Cell）の **全体既定色** を表す。未指定（`nil`）のとき UI 層は `Theme.cellTitleColor` または `UIColor.label` にフォールバックする。

`cellValueTextFont: UIFont?` は valueText の **全体既定フォント** を表す。未指定のとき UI 層は `Theme.cellTitleFont` または `UIFont.preferredFont(forTextStyle: .body)` にフォールバックする。

`cellDescriptionColor: UIColor?` は description の **全体既定色** を表す。未指定のとき UI 層は `UIColor.secondaryLabel` 相当（`UIColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)` ライトモード固定色）にフォールバックする。

`cellDescriptionFont: UIFont?` は description の **全体既定フォント** を表す。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .footnote)` にフォールバックする。

`cellHintTextColor: UIColor?` は hintText の **全体既定色** を表す。未指定のとき UI 層は accent 色相当（`Theme.cellAccentColor`）にフォールバックする。

`cellHintFont: UIFont?` は hintText の **全体既定フォント** を表す。未指定のとき UI 層は `UIFont.preferredFont(forTextStyle: .footnote)` にフォールバックする。

`cellIconSize: CGFloat?` は icon の **全体既定サイズ**（正方形の一辺 pt）を表す。未指定のとき UI 層は既定（24pt）にフォールバックする。`CellStyle.iconSize: CGFloat?` と型を一致させ、`EffectiveStyle.effectiveIconSize` の結果も `CGFloat`（一辺）を返すことで「icon は正方形」というアイコン表現の前提を spec レベルで揃える。オリジナル `AiForms.Maui.SettingsView.SettingsView.CellIconSize`（`Size` 型）に対しては、本実装では「`Width` と `Height` のうち大きい方を使うか、Width のみを採用する」とは限定せず、`CellStyle.iconSize` 設計に従って **一辺スカラー** に簡素化する。

`cellIconRadius: CGFloat?` は icon の **全体既定角丸半径** を表す。未指定のとき UI 層は既定（0pt = 角丸なし）にフォールバックする。

#### Header / Footer Font 系（新規追加）

`headerFont: UIFont?` は Section Header の **全体既定フォント**（family / weight / 装飾を含む）を表す Optional フィールドでなければならない (MUST)。未指定のとき UI 層は既存 `headerFontSize` のみで描画する。`headerFontSize > 0` かつ `headerFont != nil` のとき、**`headerFontSize` を size として優先**する (MUST)。

`footerFont: UIFont?` は Section Footer の **全体既定フォント** を表す。挙動は `headerFont` と同じく `footerFontSize` 優先である (MUST)。

`headerHeight: Double` は SettingsView 全体に適用される Section Header の **既定高さ**（論理単位 = pt）を表し、既定値は `-1.0`（未指定 = 自動）でなければならない (MUST)。Section ごとの `Section.headerHeight` が `-1.0` のときは本値を採用する。

#### 既存維持

`backgroundColor` は SettingsView（`UICollectionView`）自身の背景色を表し、`cellBackgroundColor`（個々の Cell の背景色）とは独立した色でなければならない (MUST)。

`rowHeight: Int` は SettingsView 全体に適用される行高さの基準値（論理単位、整数）でなければならない (SHALL)。`-1` は「未指定」を意味し、UI 層は `MinRowHeight`（iOS 48pt）を下限として用いる。

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える Bool でなければならない (SHALL)。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `UIColor` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、Double）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Swift `struct` として `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` および `UIFont` は Swift の `Equatable` に準拠していないためコンパイラによる自動合成は不可能 (MUST NOT)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)`、`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` を用いた手動 `==` / `!=` 実装が必須 (MUST)。`Hashable` 準拠は必須としない。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（パラメータなし init）
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`backgroundColor` は `UIColor.systemBackground` 系または白系、`rowHeight = -1`、`hasUnevenRows = false`、`headerFontSize = -1`、`footerFontSize = -1`、`headerHeight = -1.0`、`disabledTextColor` はやや薄い灰色、`cellTitleColor = nil`、`cellTitleFont = nil`、`cellTitleFontSize = -1.0`、新規フィールド（`cellValueTextColor` / `cellValueTextFont` / `cellDescriptionColor` / `cellDescriptionFont` / `cellHintTextColor` / `cellHintFont` / `cellIconSize` / `cellIconRadius` / `headerFont` / `footerFont`）はすべて `nil`

#### Scenario: viewBackgroundColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().viewBackgroundColor` を参照する Swift コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（旧名は完全に削除され、互換シムも提供されない）

#### Scenario: titleColor は存在しない

- **GIVEN** `Theme` の型定義
- **WHEN** `Theme().titleColor` を参照する Swift コードを書いてコンパイルする
- **THEN** **コンパイルエラー** になる（`cellTitleColor` への書き換えが必須）

#### Scenario: cellTitleColor / cellTitleFont の Optional 性

- **GIVEN** `Theme()` の `cellTitleColor` / `cellTitleFont` フィールド
- **WHEN** 型を確認する
- **THEN** どちらも Optional（`UIColor?` / `UIFont?`）であり、既定値は `nil` である

#### Scenario: cellTitleFontSize 既定値

- **GIVEN** `Theme()` の `cellTitleFontSize` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定」を表す

#### Scenario: cellTitleFontSize と cellTitleFont 併設時の size 優先

- **GIVEN** `Theme(cellTitleFont: UIFont.systemFont(ofSize: 14), cellTitleFontSize: 20.0)`
- **WHEN** UI 層が Cell タイトルを描画する
- **THEN** 最終的な pointSize は **20.0pt**（`cellTitleFontSize` 優先）で描画され、`cellTitleFont` の pointSize 14 は無視される。family / weight など `cellTitleFont` の他属性は維持される

#### Scenario: 新規 Cell 全体既定フィールドの保持

- **GIVEN** `Theme(cellHintTextColor: UIColor.red, cellIconSize: 32.0)`
- **WHEN** 値を参照する
- **THEN** `cellHintTextColor` は `UIColor.red`、`cellIconSize` は `32.0`（一辺 pt）を返す

#### Scenario: headerHeight 既定値

- **GIVEN** `Theme()` の `headerHeight` フィールド
- **WHEN** 値を参照する
- **THEN** `-1.0` が返り「未指定 = 自動」を表す

#### Scenario: backgroundColor と cellBackgroundColor が独立

- **GIVEN** `Theme(backgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0), cellBackgroundColor: .white)`
- **WHEN** 値を参照する
- **THEN** SettingsView 全体の背景色（セル間／セクション間に見える背景）は `backgroundColor`、個別 Cell の背景色は `cellBackgroundColor` として別の色を保持できる

#### Scenario: Native 型を直接保持

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `KsColor` や独自論理色型を経由せず、`UIColor` / `UIFont` / `CGFloat` を直接保持する

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

CellStyle のフィールドはいずれも `nil` を取りうる Optional であり、`nil` のとき UI 層は **`Theme` の対応する全体既定フィールド**（解決順序: `Theme.cellTitleColor` / `Theme.cellTitleFont` / `Theme.cellTitleFontSize` / `Theme.cellValueTextColor` / `Theme.cellValueTextFont` / `Theme.cellDescriptionColor` / `Theme.cellDescriptionFont` / `Theme.cellHintTextColor` / `Theme.cellHintFont` / `Theme.cellIconSize` / `Theme.cellIconRadius` / `Theme.cellBackgroundColor` / `Theme.cellAccentColor`）にフォールバックしなければならない (MUST)。`Theme` 側も未指定の場合は UI 層既定値（UIKit プラットフォーム既定または本 spec の他 Requirement で定義された値）を用いる。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** デフォルトコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** すべてのフィールドが「未指定（`nil`）」となり、`Theme` から継承される

#### Scenario: Native 型を直接保持

- **GIVEN** `CellStyle` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `UIColor?` / `UIFont?` / `CGFloat?` を直接保持する

#### Scenario: backgroundColor の独立性

- **GIVEN** `CellStyle(backgroundColor: UIColor.red)` を持つ Cell と `Theme(cellBackgroundColor: UIColor.white)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効背景色は赤（`CellStyle.backgroundColor` 優先）になる

#### Scenario: accentColor の独立性

- **GIVEN** `CellStyle(accentColor: UIColor.green)` を持つ Cell と `Theme(cellAccentColor: UIColor.blue)`
- **WHEN** UI 層が実効スタイルを計算する
- **THEN** 実効 accent 色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: hintTextColor の Theme フォールバック

- **GIVEN** `CellStyle(hintTextColor: nil)` を持つ Cell と `Theme(cellHintTextColor: UIColor.red)`
- **WHEN** UI 層が実効 hintText 色を計算する
- **THEN** 実効 hintText 色は **赤**（`Theme.cellHintTextColor` から落ちてくる）になる

#### Scenario: iconSize の Theme フォールバック

- **GIVEN** `CellStyle(iconSize: nil)` を持つ Cell と `Theme(cellIconSize: 32.0)`
- **WHEN** UI 層が実効 iconSize を計算する
- **THEN** 実効 iconSize は **`32.0`（一辺 pt）**（`Theme.cellIconSize` から落ちてくる）になる

## ADDED Requirements

### Requirement: EffectiveStyle の解決順序

`KsSettingsViewUI` モジュールは、Cell 描画時の最終スタイル値を「CellStyle → Theme → 既定」の 3 段で解決するユーティリティ `EffectiveStyle` を提供しなければならない (SHALL)。`EffectiveStyle` は各 Cell プロパティに対応する **アクセサ関数** を提供し、各 Cell View の描画処理から呼び出されなければならない (MUST)。

解決順序：

```
最終値 = cellStyle.X            if X != nil
       else theme.cellX         if cellX != nil
       else プラットフォーム既定（本 spec の他 Requirement または UI 層内の既定値）
```

`titleFontSize` のみ特殊で、`theme.cellTitleFontSize` が `> 0` の場合は `cellTitleFont.pointSize` を **上書き** する。

EffectiveStyle は以下のアクセサを最低限提供しなければならない (MUST)：

- `effectiveTitleColor(cellStyle, theme) -> UIColor`
- `effectiveTitleFont(cellStyle, theme) -> UIFont`（pointSize は `cellTitleFontSize` で上書きされた最終値）
- `effectiveDescriptionColor(cellStyle, theme) -> UIColor`
- `effectiveDescriptionFont(cellStyle, theme) -> UIFont`
- `effectiveValueTextColor(cellStyle, theme) -> UIColor`
- `effectiveValueTextFont(cellStyle, theme) -> UIFont`
- `effectiveHintTextColor(cellStyle, theme) -> UIColor`
- `effectiveHintFont(cellStyle, theme) -> UIFont`
- `effectiveIconSize(cellStyle, theme) -> CGFloat`（icon は正方形、一辺 pt を返す）
- `effectiveIconRadius(cellStyle, theme) -> CGFloat`
- `effectiveBackgroundColor(cellStyle, theme) -> UIColor`
- `effectiveAccentColor(cellStyle, theme) -> UIColor`
- `effectiveCellHeight(cellStyle, theme) -> CGFloat`（既存）

`ButtonCell.titleColor` のみ特殊で、Cell 個別の `titleColor` フィールドを **最優先** とする 4 段解決を維持する（既存 cell-types-basic spec 規約を尊重）：

```
ButtonCell.titleColor → CellStyle.titleColor → Theme.cellTitleColor → プラットフォーム既定
```

#### Scenario: 通常 Cell の解決順序（CellStyle 優先）

- **GIVEN** `CellStyle(titleColor: UIColor.red)` を持つ LabelCell と `Theme(cellTitleColor: UIColor.blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.red`**（CellStyle が優先される）

#### Scenario: 通常 Cell の解決順序（Theme フォールバック）

- **GIVEN** `CellStyle(titleColor: nil)` を持つ LabelCell と `Theme(cellTitleColor: UIColor.blue)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.blue`**（Theme から落ちてくる）

#### Scenario: 通常 Cell の解決順序（既定フォールバック）

- **GIVEN** `CellStyle(titleColor: nil)` を持つ LabelCell と `Theme(cellTitleColor: nil)`
- **WHEN** `EffectiveStyle.effectiveTitleColor(cellStyle, theme)` を呼ぶ
- **THEN** 戻り値は **`UIColor.label`**（プラットフォーム既定）

#### Scenario: cellTitleFontSize 優先

- **GIVEN** `CellStyle(titleFont: nil)` を持つ Cell と `Theme(cellTitleFont: UIFont.systemFont(ofSize: 14), cellTitleFontSize: 20.0)`
- **WHEN** `EffectiveStyle.effectiveTitleFont(cellStyle, theme).pointSize` を取得する
- **THEN** `20.0`（`cellTitleFontSize` で pointSize が上書きされる）

#### Scenario: ButtonCell.titleColor の 4 段解決（Cell 個別最優先）

- **GIVEN** `ButtonCell(titleColor: UIColor.red)`、`CellStyle(titleColor: UIColor.green)`、`Theme(cellTitleColor: UIColor.blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`UIColor.red`**（ButtonCell.titleColor が最優先）

#### Scenario: ButtonCell.titleColor が nil の場合は CellStyle 経由

- **GIVEN** `ButtonCell(titleColor: nil)`、`CellStyle(titleColor: UIColor.green)`、`Theme(cellTitleColor: UIColor.blue)`
- **WHEN** ButtonCell の最終 title 色を解決する
- **THEN** **`UIColor.green`**（CellStyle.titleColor から落ちる）

#### Scenario: UIFont equals の安定性

- **GIVEN** 同一の `UIFont.systemFont(ofSize: 16, weight: .regular)` を渡して構築した 2 つの `Theme` インスタンス
- **WHEN** `==` で比較する
- **THEN** 等価判定が真になる（`UIFont.isEqual(_:)` ベースで同じフォントとして扱われる）

#### Scenario: fontFamily 反映の e2e

- **GIVEN** カスタム `UIFont(name: "Avenir-Heavy", size: 18)` を `Theme(cellTitleFont: customFont)` に設定した KsSettingsViewController
- **WHEN** LabelCell を描画する
- **THEN** Cell 内の title `UILabel.font` は `customFont` と `UIFont.isEqual(_:)` で等価で、`fontName` が `"Avenir-Heavy"` を含む（既定 `.SFUI-...` 系にフォールバックしない）
