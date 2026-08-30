## MODIFIED Requirements

### Requirement: UICollectionView のレイアウト

UI は `UICollectionLayoutListConfiguration`（iOS 14+）を `UICollectionViewCompositionalLayout` の `.list` で構成しなければならない (SHALL)。Cell の高さは `estimatedItemSize = .automatic` で Auto Layout により決定されなければならない (MUST)。

ただし `Theme.hasUnevenRows == false` のとき、各 Cell は **固定高さ** で描画されなければならない (MUST)。固定高さ値は `max(Theme.rowHeight, MinRowHeight)` で算出する（`Theme.rowHeight == -1` のときは `MinRowHeight` を採用）。`MinRowHeight` は `48` ポイントとする (MUST)。固定高さは Cell View の `contentView` に対し `heightAnchor.constraint(equalToConstant: effectiveHeight)` で適用する。

`Theme.hasUnevenRows == true` のとき、各 Cell は Auto Layout による可変高さでありつつ、`contentView.heightAnchor.constraint(greaterThanOrEqualToConstant: effectiveMinHeight)` で **最低高さ保証** を付与しなければならない (MUST)。`effectiveMinHeight` は同様に `max(Theme.rowHeight, MinRowHeight)` とする。さらに個別 Cell の `CellStyle.cellHeight` が指定されている場合は、その値を優先（固定高さ・最低高さの双方で個別値を採用）しなければならない (MUST)。

`Theme.hasUnevenRows` のデフォルト値は **`true`**（Auto 高さ + 下限保証）とする (MUST)。これにより、`Theme()` を引数なしで構築した既定状態では各 Cell が内容に応じて自然に伸縮する。「全 Cell を一律固定高さで揃えたい」用途では、利用者が `Theme(hasUnevenRows: false)` を明示指定することで従来の固定高さモードを選べる。デフォルト `true` 化はオリジナル `AiForms.Maui.SettingsView` の `AiTableView`（`RowHeight = UITableView.AutomaticDimension` + `MinRowHeight = 48`）の挙動踏襲である。

#### Scenario: List 設定の使用

- **GIVEN** `KsSettingsViewController` が初期化済み
- **WHEN** `view.subviews` に含まれる `UICollectionView` のレイアウトを取得する
- **THEN** 取得したレイアウトは `UICollectionViewCompositionalLayout` であり、内部設定は List ベースである

#### Scenario: 区切り線とヘッダ・フッタ

- **GIVEN** `Section` に `header` が `SectionAccessory.text("一般")` で指定されている
- **WHEN** Cell が描画される
- **THEN** `UICollectionLayoutListConfiguration.headerMode = .supplementary` 等を用いてヘッダ領域に "一般" が表示され、`Theme.separatorColor` で区切り線色が設定される

#### Scenario: 固定高さ（HasUnevenRows = false）

- **GIVEN** `Theme(rowHeight: 60, hasUnevenRows: false)` で初期化された `KsSettingsView`、複数 Cell が並ぶ
- **WHEN** 表示される
- **THEN** すべての Cell の高さが `60pt`（`MinRowHeight = 48pt` より大きいので 60 を採用）に固定される。長文 Description が含まれる場合は省略される

#### Scenario: 可変高さ（HasUnevenRows = true、新デフォルト）

- **GIVEN** `Theme(rowHeight: -1)` で初期化された `KsSettingsView`（`hasUnevenRows` は新デフォルト `true` が適用される）、長文 Description を持つ Cell と単行 Cell が混在
- **WHEN** 表示される
- **THEN** 各 Cell は最低高さ `48pt`（`MinRowHeight`）を保証しつつ、内容に応じて伸縮する（長文 Cell は 48pt より高くなり、単行 Cell は 48pt 固定）。`Theme()` を引数なしで構築しても同じ振る舞いとなる

#### Scenario: CellStyle.cellHeight の優先

- **GIVEN** `Theme(rowHeight: 44, hasUnevenRows: true)` と `CellStyle(cellHeight: 80)` を持つ特定 Cell
- **WHEN** 表示される
- **THEN** 当該 Cell の最低高さは `80pt`（`CellStyle.cellHeight` 優先）となる。他 Cell は `44pt`（または `MinRowHeight = 48pt` のうち大きい方）保証

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

`cellTitleFontSize: Double` は Cell タイトルの **既定フォントサイズ単独** を表す独立フィールドで、既定値は `-1.0`(未指定)でなければならない (MUST)。`cellTitleFont` と `cellTitleFontSize` が両方非 nil / `-1.0` 以外のとき、**`cellTitleFontSize` を size として優先**しなければならない (MUST)。すなわち最終的に描画される size は `cellTitleFontSize > 0 ? CGFloat(cellTitleFontSize) : cellTitleFont.pointSize` となる。

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

`hasUnevenRows` は行高さを「個別 Cell ごとに可変」（`true`）にするか「全 Cell 一律固定」（`false`）にするかを切り替える Bool でなければならない (SHALL)。**デフォルト値は `true`** とする (MUST)。これによりオリジナル `AiForms.Maui.SettingsView` の `AiTableView`（`RowHeight = UITableView.AutomaticDimension` + `MinRowHeight = 48`）の「Auto 高さ + 下限保証」既定挙動と整合する。「全 Cell を一律固定高さで揃えたい」用途では利用者が `Theme(hasUnevenRows: false)` を明示指定する。

`disabledTextColor` は Cell の `isEnabled = false` のとき、タイトル／説明文／値テキスト／ヒントテキストの色を置換するための `UIColor` でなければならない (SHALL)。

`headerFontSize` / `footerFontSize` は Section ヘッダ／フッタの既定フォントサイズ（論理単位、Double）でなければならない (SHALL)。`-1` は「未指定（プラットフォーム既定値）」を意味する。

`Theme` は Swift `struct` として `Equatable` プロトコルに準拠しなければならない (MUST)。`UIColor` および `UIFont` は Swift の `Equatable` に準拠していないためコンパイラによる自動合成は不可能 (MUST NOT)。`UIColor` フィールドの等価性は `UIColor.isEqual(_:)`、`UIFont` フィールドの等価性は `UIFont.isEqual(_:)` を用いた手動 `==` / `!=` 実装が必須 (MUST)。`Hashable` 準拠は必須としない。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（パラメータなし init）
- **WHEN** `Theme()` を構築する
- **THEN** 中立的な既定値を持つ。`backgroundColor` は `UIColor.systemBackground` 系または白系、`rowHeight = -1`、`hasUnevenRows = true`、`headerFontSize = -1`、`footerFontSize = -1`、`headerHeight = -1.0`、`disabledTextColor` はやや薄い灰色、`cellTitleColor = nil`、`cellTitleFont = nil`、`cellTitleFontSize = -1.0`、新規フィールド（`cellValueTextColor` / `cellValueTextFont` / `cellDescriptionColor` / `cellDescriptionFont` / `cellHintTextColor` / `cellHintFont` / `cellIconSize` / `cellIconRadius` / `headerFont` / `footerFont`）はすべて `nil`

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
- **THEN** それぞれ `-1` と `true` が返る（UI 層はこの組み合わせを「Auto 高さ + 最低高さ MinRowHeight」と解釈する）

#### Scenario: 利用者が UIColor をそのまま渡せる

- **GIVEN** 利用者コード `Theme(separatorColor: UIColor.systemGray3, cellBackgroundColor: .white)`
- **WHEN** コンパイル・実行する
- **THEN** ビルドエラーなく構築でき、`KsColor` などの中間型を書く必要がない
