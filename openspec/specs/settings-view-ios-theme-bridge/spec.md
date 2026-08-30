# settings-view-ios-theme-bridge Specification

## Purpose

`settings-view-ios-theme-bridge` は、`KsSettingsViewCore` の `Theme` / `CellStyle` / `KsColor` / `KsImage` などの論理スタイル値を iOS / UIKit プラットフォーム値（`UIColor` / `UIFont` / `UIImage` 等）に変換する **テーマ変換ブリッジ層** を担う capability である。各 Cell View が描画時に参照する実効スタイルの合成（CellStyle → Theme → プラットフォーム fallback の3段階）、タッチフィードバック（`selectedColor` のセル背景反映）、`isEnabled = false` 時の描画変換、ButtonCell の `baseColor` 解決順序、`KsImage.uiImage` 派生（`systemName` / 任意 `UIImage`）の解決を定義する。`settings-view-ios-host`（ホスト層）と `settings-view-ios-style`（スタイル・レイアウト層）はいずれも本 capability の変換結果を消費する立場であり、Theme 値の変換ロジック自体を含まない。

## Requirements
### Requirement: Theme / CellStyle の UIKit 変換

`Theme` および `CellStyle` の各フィールドは `UIColor` / `UIFont` を直接保持するため、**`KsColor` / `KsFont` からの変換ユーティリティは存在しない (MUST NOT 存在)**。本 Requirement の責務は「**実効スタイル合成**」のみとする。

実効スタイル合成では、`CellStyle` の各フィールド（`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `iconSize` / `iconRadius` / `cellHeight` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor`）が `nil` のとき、対応する `Theme` フィールドで補完しなければならない (MUST)。`CellStyle.backgroundColor` 未指定時は `Theme.cellBackgroundColor`、`CellStyle.accentColor` 未指定時は `Theme.cellAccentColor` を採用する。`UICollectionView` 自体の背景色は `Theme.viewBackgroundColor` を採用しなければならない (MUST)。

タイトル色／フォントの合成は次の 3 段階優先順位でなければならない (MUST)：

1. `CellStyle.titleColor` が `nil` でなければそれを採用（`UIColor` をそのまま使う）
2. それ以外で `Theme.titleColor` が `nil` でなければそれを採用
3. それ以外は `UIColor.label`（システム既定）にフォールバック

`titleFont` も同様に `CellStyle.titleFont` → `Theme.titleFont` → `UIFont.preferredFont(forTextStyle: .body)` の順序で解決する。

EffectiveStyle は「タイトル色が明示由来か（CellStyle または Theme のいずれかから指定されたか）プラットフォーム fallback 由来か」を判定できる Bool フラグ（例: `titleColorIsExplicit`）を提供しなければならない (MUST)。このフラグは ButtonCell の `baseColor` 解決で使用される。

#### Scenario: 実効スタイルの合成（Theme.titleColor 採用）

- **GIVEN** Cell の `CellStyle.titleColor = nil`、`Theme.titleColor = UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)`
- **WHEN** 描画用に「実効スタイル」を計算する
- **THEN** `effective.titleColor` は `UIColor(red: 0.2, green: 0.4, blue: 0.6, alpha: 1.0)` と等価、`effective.titleColorIsExplicit == true` となる

#### Scenario: 実効スタイルの合成（プラットフォーム fallback）

- **GIVEN** Cell の `CellStyle.titleColor = nil`、`Theme.titleColor = nil`
- **WHEN** 描画用に「実効スタイル」を計算する
- **THEN** `effective.titleColor == UIColor.label`、`effective.titleColorIsExplicit == false` となる

#### Scenario: 実効スタイルの合成（CellStyle 優先）

- **GIVEN** `CellStyle.titleColor = UIColor.red`、`Theme.titleColor = UIColor.blue`
- **WHEN** 実効スタイルを計算する
- **THEN** `effective.titleColor == UIColor.red`、`effective.titleColorIsExplicit == true` となる

#### Scenario: CellStyle.backgroundColor の合成

- **GIVEN** `Theme(cellBackgroundColor: UIColor.white)` と `CellStyle(backgroundColor: UIColor.yellow)` の Cell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の実効背景色は黄（`CellStyle.backgroundColor` 優先）になる

#### Scenario: CellStyle.accentColor の合成

- **GIVEN** `Theme(cellAccentColor: UIColor.blue)` と `CellStyle(accentColor: UIColor.green)` の SwitchCell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の SwitchCell の ON 時の色は緑（`CellStyle.accentColor` 優先）になる

#### Scenario: viewBackgroundColor の反映

- **GIVEN** `Theme(viewBackgroundColor: UIColor(red: 0.95, green: 0.93, blue: 0.90, alpha: 1.0))` で初期化された `KsSettingsViewController`
- **WHEN** 表示される
- **THEN** `UICollectionView` の `backgroundColor` が当該色に設定される（個別 Cell の背景色とは独立に反映される）

#### Scenario: valueTextColor / valueTextFont の合成

- **GIVEN** `Theme(descriptionColor: UIColor.gray)` と `CellStyle(valueTextColor: UIColor.darkGray)` の LabelCell（`valueText = "オン"`）
- **WHEN** 実効スタイルを計算して LabelCell が描画される
- **THEN** `valueText` の文字色は `darkGray`（`CellStyle.valueTextColor` 優先）で表示される

#### Scenario: KsColor 変換ユーティリティの不在

- **GIVEN** `KsSettingsViewUI` モジュール
- **WHEN** `UIColor.init(ksColor:)` を探す
- **THEN** 当該 init は存在しない。`KsColor` 自体が存在しないため変換不要

### Requirement: タッチフィードバック（selectedColor の反映）

各 Cell View（`UICollectionViewListCell` サブクラス）は、`configurationUpdateHandler` を設定し、`UICellConfigurationState.isHighlighted == true` または `isSelected == true` のとき、`backgroundConfiguration.backgroundColor` を `Theme.selectedColor` で塗り替えなければならない (MUST)。非ハイライト・非選択状態に戻ったとき、`backgroundConfiguration.backgroundColor` は実効 `cellBackgroundColor`（`CellStyle.backgroundColor ?? Theme.cellBackgroundColor`）に戻らなければならない (MUST)。

この挙動は LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell のすべての Cell View で一貫して提供されなければならない (MUST)。

#### Scenario: タップ中の背景色変化

- **GIVEN** `Theme(cellBackgroundColor: .white, selectedColor: UIColor(red: 1.0, green: 0.75, blue: 0.0, alpha: 0.3))` の LabelCell が表示されている
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
- タイトル／説明文／値テキスト／ヒントテキストの色を `Theme.disabledTextColor`（**型は `UIColor`**）に置換。
- Cell 全体への `alpha` 適用や半透明化は行わない (MUST NOT)。

`cell.isEnabled == true`（既定）のときは、通常の bind ロジックを適用する。

#### Scenario: SwitchCell isEnabled = false の描画

- **GIVEN** `Theme(disabledTextColor: UIColor.lightGray)` と `SwitchCell(title: "通知", isOn: true, isEnabled: false)`
- **WHEN** iOS で描画される
- **THEN** `UISwitch.isEnabled = false`（標準の disabled 表示）になり、タイトル文字色は `UIColor.lightGray` に置換される。Cell コンテナはタップを受け付けない

#### Scenario: LabelCell isEnabled = false の描画

- **GIVEN** `LabelCell(title: "通知", description: "詳細", valueText: "オン", isEnabled: false)` と `Theme.disabledTextColor = UIColor.lightGray`
- **WHEN** iOS で描画される
- **THEN** タイトル・説明・値テキストすべての色が `UIColor.lightGray`（`Theme.disabledTextColor`）に置換される

#### Scenario: isEnabled = false 時にもタッチフィードバックは発生しない

- **GIVEN** `CommandCell(title: "管理者のみ", isEnabled: false, onTap: {...})`
- **WHEN** ユーザーがタップする
- **THEN** `selectedColor` は反映されず、`onTap` も呼ばれない

### Requirement: ButtonCell の baseColor 解決順序

`ButtonCellView` はボタンテキストの基準色（disabled 適用前の色）を次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別、型: `UIColor?`）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor` が指定されていれば `effective.titleColor` を採用
3. それ以外で `Theme.titleColor` が指定されていれば `effective.titleColor` を採用
4. それ以外は `UIColor.systemBlue`（標準ボタン色）

「2 または 3 のいずれか」の判定は EffectiveStyle の `titleColorIsExplicit`（または同等のフラグ）で行ってよい。`cell.isEnabled == false` のときは、上記で解決した基準色ではなく `effective.disabledTextColor` を用いてテキストを描画しなければならない (MUST)。

#### Scenario: ButtonCell.titleColor 指定時

- **GIVEN** `ButtonCell(title: "削除", titleColor: UIColor.red)`、`CellStyle.titleColor = nil`、`Theme.titleColor = UIColor.green`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.red`（Cell 個別 `titleColor` 優先）になる

#### Scenario: CellStyle.titleColor 指定時

- **GIVEN** `ButtonCell(title: "次へ", titleColor: nil)`、`CellStyle.titleColor = UIColor.purple`、`Theme.titleColor = UIColor.green`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.purple`（CellStyle 経由）になる

#### Scenario: Theme.titleColor 指定時

- **GIVEN** `ButtonCell(title: "登録", titleColor: nil)`、`CellStyle.titleColor = nil`、`Theme.titleColor = UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0)`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `Theme.titleColor` の UIColor 値になる（4 段階目の `.systemBlue` ではなく、Theme 既定が反映される）

#### Scenario: 全段階未指定時のシステム既定

- **GIVEN** `ButtonCell(title: "OK", titleColor: nil)`、`CellStyle.titleColor = nil`、`Theme.titleColor = nil`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.systemBlue`（4 段階目のシステム既定）になる

#### Scenario: isEnabled = false 時の disabledTextColor 適用

- **GIVEN** `ButtonCell(title: "削除", titleColor: UIColor.red, isEnabled: false)`、`Theme.disabledTextColor = UIColor.lightGray`
- **WHEN** ButtonCellView が描画される
- **THEN** ボタンテキストの色は `UIColor.lightGray`（disabledTextColor）に置換される（baseColor の `.red` は使われない）

### Requirement: KsImage.uiImage 派生の解決

`KsSettingsViewUI` は `KsImage` の `systemName(String)` 派生と `uiImage(UIImage)` 派生の両方を解決して `UIImageView.image` に設定しなければならない (MUST)。具体的には：

- `KsImage.systemName(name)` → `UIImage(systemName: name)` を取得し設定する。取得失敗時はアイコン非表示（`isHidden = true`）にフォールバック
- `KsImage.uiImage(image)` → `image` をそのまま設定する

`KsImage` 型は `KsSettingsViewUI` モジュールに所属する。`KsSettingsViewCore` には存在しない (MUST NOT 存在)。

#### Scenario: systemName 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に `UIImage(systemName: "bell")` が描画される

#### Scenario: uiImage 派生の解決

- **GIVEN** `LabelCell(icon: KsImage.uiImage(UIImage(named: "custom_icon")!))`
- **WHEN** iOS で描画される
- **THEN** Cell のアイコン領域に渡された UIImage がそのまま描画される

#### Scenario: systemName 不正名のフォールバック

- **GIVEN** `LabelCell(icon: KsImage.systemName("non_existent_symbol_xyz"))`
- **WHEN** iOS で描画される
- **THEN** `UIImage(systemName:)` が `nil` を返すため、Cell のアイコン領域は非表示にフォールバックし、Title が左寄せに配置される
