## MODIFIED Requirements

### Requirement: UICollectionView のレイアウト

UI は `UICollectionLayoutListConfiguration`（iOS 14+）を `UICollectionViewCompositionalLayout` の `.list` で構成しなければならない (SHALL)。Cell の高さは `estimatedItemSize = .automatic` で Auto Layout により決定されなければならない (MUST)。

ただし `Theme.hasUnevenRows == false` のとき、各 Cell は **固定高さ** で描画されなければならない (MUST)。固定高さ値は `max(Theme.rowHeight, MinRowHeight)` で算出する（`Theme.rowHeight == -1` のときは `MinRowHeight` を採用）。`MinRowHeight` は `48` ポイントとする (MUST)。固定高さは Cell View の `contentView` に対し `heightAnchor.constraint(equalToConstant: effectiveHeight)` で適用する。

`Theme.hasUnevenRows == true` のとき、各 Cell は Auto Layout による可変高さでありつつ、`contentView.heightAnchor.constraint(greaterThanOrEqualToConstant: effectiveMinHeight)` で **最低高さ保証** を付与しなければならない (MUST)。`effectiveMinHeight` は同様に `max(Theme.rowHeight, MinRowHeight)` とする。さらに個別 Cell の `CellStyle.cellHeight` が指定されている場合は、その値を優先（固定高さ・最低高さの双方で個別値を採用）しなければならない (MUST)。

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

#### Scenario: 可変高さ（HasUnevenRows = true）

- **GIVEN** `Theme(rowHeight: -1, hasUnevenRows: true)` で初期化された `KsSettingsView`、長文 Description を持つ Cell と単行 Cell が混在
- **WHEN** 表示される
- **THEN** 各 Cell は最低高さ `48pt` を保証しつつ、内容に応じて伸縮する（長文 Cell は 48pt より高くなり、単行 Cell は 48pt 固定）

#### Scenario: CellStyle.cellHeight の優先

- **GIVEN** `Theme(rowHeight: 44, hasUnevenRows: true)` と `CellStyle(cellHeight: 80)` を持つ特定 Cell
- **WHEN** 表示される
- **THEN** 当該 Cell の最低高さは `80pt`（`CellStyle.cellHeight` 優先）となる。他 Cell は `44pt`（または `MinRowHeight = 48pt` のうち大きい方）保証

### Requirement: Theme / CellStyle の UIKit 変換

`Theme` および `CellStyle` の論理スタイルを `UIColor` および `UIFont` に変換するユーティリティが提供されなければならない (SHALL)。変換は `KsSettingsViewUI` モジュールの内部または公開ユーティリティで行わなければならない (MUST)。

実効スタイル合成では、`CellStyle` の各フィールド（`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `iconSize` / `iconRadius` / `cellHeight` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor`）が `nil` のとき、対応する `Theme` フィールドで補完しなければならない (MUST)。`CellStyle.backgroundColor` 未指定時は `Theme.cellBackgroundColor`、`CellStyle.accentColor` 未指定時は `Theme.cellAccentColor` を採用する。`UICollectionView` 自体の背景色は `Theme.viewBackgroundColor` を採用しなければならない (MUST)。

タイトル色／フォントの合成は次の 3 段階優先順位でなければならない (MUST)：

1. `CellStyle.titleColor` が `nil` でなければそれを `UIColor` に変換して採用
2. それ以外で `Theme.titleColor` が `nil` でなければそれを `UIColor` に変換して採用
3. それ以外は `UIColor.label`（システム既定）にフォールバック

`titleFont` も同様に `CellStyle.titleFont` → `Theme.titleFont` → `UIFont.preferredFont(forTextStyle: .body)` の順序で解決する。

EffectiveStyle は「タイトル色が明示由来か（CellStyle または Theme のいずれかから指定されたか）プラットフォーム fallback 由来か」を判定できる Bool フラグ（例: `titleColorIsExplicit`）を提供しなければならない (MUST)。このフラグは ButtonCell の `baseColor` 解決で使用される。

#### Scenario: KsColor から UIColor

- **GIVEN** `KsColor(red: 1.0, green: 0.5, blue: 0.0, alpha: 1.0)`
- **WHEN** `UIColor(ksColor:)` イニシャライザを呼ぶ
- **THEN** `UIColor` の RGBA が `(1.0, 0.5, 0.0, 1.0)` と一致する

#### Scenario: 実効スタイルの合成（Theme.titleColor 採用）

- **GIVEN** Cell の `CellStyle.titleColor = nil`、`Theme.titleColor = KsColor(0.2, 0.4, 0.6, 1.0)`
- **WHEN** 描画用に「実効スタイル」を計算する
- **THEN** `effective.titleColor` は `UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)` 相当となり、`effective.titleColorIsExplicit == true` となる

#### Scenario: 実効スタイルの合成（プラットフォーム fallback）

- **GIVEN** Cell の `CellStyle.titleColor = nil`、`Theme.titleColor = nil`
- **WHEN** 描画用に「実効スタイル」を計算する
- **THEN** `effective.titleColor == UIColor.label`、`effective.titleColorIsExplicit == false` となる

#### Scenario: 実効スタイルの合成（CellStyle 優先）

- **GIVEN** `CellStyle.titleColor = KsColor.red`、`Theme.titleColor = KsColor.blue`
- **WHEN** 実効スタイルを計算する
- **THEN** `effective.titleColor == UIColor.red`、`effective.titleColorIsExplicit == true` となる

#### Scenario: CellStyle.backgroundColor の合成

- **GIVEN** `Theme(cellBackgroundColor: KsColor.white)` と `CellStyle(backgroundColor: KsColor.yellow)` の Cell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の実効背景色は黄（`CellStyle.backgroundColor` 優先）になる

#### Scenario: CellStyle.accentColor の合成

- **GIVEN** `Theme(cellAccentColor: KsColor.blue)` と `CellStyle(accentColor: KsColor.green)` の SwitchCell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の SwitchCell の ON 時の色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: viewBackgroundColor の反映

- **GIVEN** `Theme(viewBackgroundColor: KsColor(0.95, 0.93, 0.90, 1.0))` で初期化された `KsSettingsViewController`
- **WHEN** 表示される
- **THEN** `UICollectionView` の `backgroundColor` が当該色に設定される（個別 Cell の背景色とは独立に反映される）

#### Scenario: valueTextColor / valueTextFont の合成

- **GIVEN** `Theme(descriptionColor: KsColor.gray)` と `CellStyle(valueTextColor: KsColor.darkGray)` の LabelCell（`valueText = "オン"`）
- **WHEN** 実効スタイルを計算して LabelCell が描画される
- **THEN** `valueText` の文字色は `darkGray`（`CellStyle.valueTextColor` 優先）で表示される

## ADDED Requirements

### Requirement: タッチフィードバック（selectedColor の反映）

各 Cell View（`UICollectionViewListCell` サブクラス）は、`configurationUpdateHandler` を設定し、`UICellConfigurationState.isHighlighted == true` または `isSelected == true` のとき、`backgroundConfiguration.backgroundColor` を `Theme.selectedColor` で塗り替えなければならない (MUST)。非ハイライト・非選択状態に戻ったとき、`backgroundConfiguration.backgroundColor` は実効 `cellBackgroundColor`（`CellStyle.backgroundColor ?? Theme.cellBackgroundColor`）に戻らなければならない (MUST)。

この挙動は LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell のすべての Cell View で一貫して提供されなければならない (MUST)。

#### Scenario: タップ中の背景色変化

- **GIVEN** `Theme(cellBackgroundColor: .white, selectedColor: KsColor(1.0, 0.75, 0.0, 0.3))` の LabelCell が表示されている
- **WHEN** ユーザーが Cell をタップして指を離さない（押下中）
- **THEN** Cell の背景色が橙の半透明色 `selectedColor` に変化する

#### Scenario: タップ解除時の復帰

- **GIVEN** タップ中で `selectedColor` が反映されている Cell
- **WHEN** ユーザーが指を離す
- **THEN** Cell の背景色が実効 `cellBackgroundColor` に戻る

#### Scenario: CommandCell の選択フィードバック

- **GIVEN** `Theme.selectedColor` が設定された `CommandCell`（`onTap` を持つ）
- **WHEN** ユーザーがタップする
- **THEN** タップ中に背景が `selectedColor` で塗られ、指を離した瞬間に `onTap` が発火し背景は元に戻る（タップフィードバックがユーザーに伝わる）

### Requirement: isEnabled 描画の反映

各 Cell View は `cell.isEnabled == false` のとき、以下を適用しなければならない (MUST)：

- Cell View の `isUserInteractionEnabled = false`（タップを通さない）。
- 内部コントロール要素（SwitchCell の `UISwitch`、CheckboxCell のチェックボックス View、RadioCell / SimpleCheckCell のチェック表示 View、ButtonCell / CommandCell の Label / tap area）の `isEnabled = false`（コントロール自体の disabled 表示）。
- タイトル／説明文／値テキスト／ヒントテキストの色を `Theme.disabledTextColor` に置換。
- Cell 全体への `alpha` 適用や半透明化は行わない (MUST NOT)。

`cell.isEnabled == true`（既定）のときは、通常の bind ロジックを適用する。

#### Scenario: SwitchCell isEnabled = false の描画

- **GIVEN** `Theme(disabledTextColor: KsColor.lightGray)` と `SwitchCell(title: "通知", isOn: true, isEnabled: false)`
- **WHEN** iOS で描画される
- **THEN** `UISwitch.isEnabled = false`（標準の disabled 表示）になり、タイトル文字色は lightGray に置換される。Cell コンテナはタップを受け付けない

#### Scenario: LabelCell isEnabled = false の描画

- **GIVEN** `LabelCell(title: "通知", description: "詳細", valueText: "オン", isEnabled: false)` と `Theme.disabledTextColor`
- **WHEN** iOS で描画される
- **THEN** タイトル・説明・値テキストすべての色が `disabledTextColor` に置換される

#### Scenario: isEnabled = false 時にもタッチフィードバックは発生しない

- **GIVEN** `CommandCell(title: "管理者のみ", isEnabled: false, onTap: {...})`
- **WHEN** ユーザーがタップする
- **THEN** `selectedColor` は反映されず、`onTap` も呼ばれない

### Requirement: ButtonCell の baseColor 解決順序

`ButtonCellView` はボタンテキストの基準色（disabled 適用前の色）を次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor` が指定されていれば `effective.titleColor` を採用
3. それ以外で `Theme.titleColor` が指定されていれば `effective.titleColor` を採用
4. それ以外は `UIColor.systemBlue`（標準ボタン色）

「2 または 3 のいずれか」の判定は EffectiveStyle の `titleColorIsExplicit`（または同等のフラグ）で行ってよい。`cell.isEnabled == false` のときは、上記で解決した基準色ではなく `effective.disabledTextColor` を用いてテキストを描画しなければならない (MUST)。

#### Scenario: ButtonCell.titleColor 指定時

- **GIVEN** `ButtonCell(title: "削除", titleColor: KsColor.red)`、`CellStyle.titleColor = nil`、`Theme.titleColor = KsColor.green`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.red`（Cell 個別 `titleColor` 優先）になる

#### Scenario: CellStyle.titleColor 指定時

- **GIVEN** `ButtonCell(title: "次へ", titleColor: nil)`、`CellStyle.titleColor = KsColor.purple`、`Theme.titleColor = KsColor.green`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.purple`（CellStyle 経由）になる

#### Scenario: Theme.titleColor 指定時

- **GIVEN** `ButtonCell(title: "登録", titleColor: nil)`、`CellStyle.titleColor = nil`、`Theme.titleColor = KsColor(0.8, 0.6, 0.0, 1.0)`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `Theme.titleColor` に解決された UIColor になる（4 段階目の `.systemBlue` ではなく、Theme 既定が反映される）

#### Scenario: 全段階未指定時のシステム既定

- **GIVEN** `ButtonCell(title: "OK", titleColor: nil)`、`CellStyle.titleColor = nil`、`Theme.titleColor = nil`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.systemBlue`（4 段階目のシステム既定）になる

#### Scenario: isEnabled = false 時の disabledTextColor 適用

- **GIVEN** `ButtonCell(title: "削除", titleColor: KsColor.red, isEnabled: false)`、`Theme.disabledTextColor = KsColor.lightGray`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.lightGray`（disabledTextColor）に置換される（baseColor の `.red` は使われない）
