## MODIFIED Requirements

### Requirement: ButtonCell

`ButtonCell` はボタン用途のセルでなければならない (SHALL)。`title` をボタンスタイルで表示しなければならない (MUST)。タップで `onTap` を発火しなければならない (MUST)。ボタンテキストの色は次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別、Optional）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor` が指定されていればそれを採用
3. それ以外で `Theme.titleColor` が指定されていればそれを採用
4. それ以外はプラットフォーム標準のボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）

タイトルの水平方向の揃え位置は `titleAlignment: CellTitleAlignment`（既定 `.center`）で指定できなければならない (MUST)。

#### Scenario: Theme.titleColor が ButtonCell に効く

- **GIVEN** `Theme(titleColor: KsColor(0.8, 0.6, 0.0, 1.0))`、`ButtonCell(title: "登録", titleColor: nil)`、当該 Cell の `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** ボタンテキストの色は `Theme.titleColor` 由来の橙系色になる（プラットフォーム標準ボタン色ではない）

#### Scenario: Cell 個別 titleColor が Theme より優先

- **GIVEN** `Theme(titleColor: KsColor.green)`、`ButtonCell(title: "削除", titleColor: KsColor.red)`
- **WHEN** Cell が描画される
- **THEN** ボタンテキストの色は赤（Cell 個別 `titleColor` 優先、Theme よりも上位）

#### Scenario: 既定の中央寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`（`titleAlignment` 省略）
- **WHEN** SettingsView に表示される
- **THEN** Cell 中央にタイトルが表示され、Disclosure Indicator は表示されない

#### Scenario: titleAlignment = .start での左寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", titleAlignment: .start, onTap: {...})`
- **WHEN** SettingsView に表示される
- **THEN** Cell の左端（リーディング側）寄りにタイトルが表示される

#### Scenario: titleAlignment = .end での右寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", titleAlignment: .end, onTap: {...})`
- **WHEN** SettingsView に表示される
- **THEN** Cell の右端（トレーリング側）寄りにタイトルが表示される

#### Scenario: titleAlignment 省略時の既定値と API 互換性

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`（`titleAlignment` を指定しない既存呼び出し）
- **WHEN** コンパイル・実行してインスタンスを参照する
- **THEN** `buttonCell.titleAlignment == .center` で、ビルドエラーや実行時エラーは発生しない

### Requirement: CheckboxCell

`CheckboxCell` は ON/OFF をチェックマークで表すセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。チェック時のアイコン（accent 表示）は `CellStyle.accentColor` または `Theme.cellAccentColor` で着色されなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `CheckBox`（`UIButton` + `Draw`）相当の **角丸の四角いチェックボックス UI** でなければならない (MUST)。すなわち、角丸（CornerRadius 相当）の四角枠（BorderWidth 相当）を持ち、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークを重ね、非チェック時は枠のみを表示する。このチェックボックスは右端に `UICellAccessory.customView`（`placement: .trailing`）として常設し、チェック状態の切り替えは accessory の追加・削除ではなくカスタム View 内部の再描画で行わなければならない (MUST)（追加・削除に伴うスライドアニメーションを避けるため）。

Android では、チェック表現は `com.google.android.material.checkbox.MaterialCheckBox` を用いた角丸の四角いチェックボックスでなければならない (MUST)。`MaterialCheckBox` 自体の内側 padding（タッチ域確保のための既定 padding）は `setPadding(0, 0, 0, 0)` および `minimumWidth = 0` / `minimumHeight = 0` で無効化し、accessoryHolder 右端と CheckboxCell のチェックボックス右端が SwitchCell / RadioCell / SimpleCheckCell と同一 X 座標に揃わなければならない (MUST)。`buttonTintList` は実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）で着色されなければならない (MUST)。

#### Scenario: チェック状態の表示（iOS）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)` を iOS で表示
- **WHEN** 表示される
- **THEN** 右端に角丸の四角いチェックボックス（accent カラーで塗りつぶし＋白いチェックマーク）が表示される

#### Scenario: 非チェック状態の表示（iOS）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: false)` を iOS で表示
- **WHEN** 表示される
- **THEN** 右端に角丸の四角い枠のみ（塗りつぶし・チェックマークなし）が表示され、accessory の位置はチェック時と同一である

#### Scenario: チェック状態の表示（Android）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)` を Android で表示し、`Theme.cellAccentColor = KsColor.yellow`
- **WHEN** 表示される
- **THEN** 右端に `MaterialCheckBox` が `buttonTintList = ColorStateList.valueOf(yellow)` で着色されチェック状態で表示され、チェックボックスの右端は同じセル内の他アクセサリと同一 X 座標に揃う

#### Scenario: 右端アクセサリ位置の整列（Android）

- **GIVEN** 同じ画面に SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell を順に並べた状態
- **WHEN** Android で表示する
- **THEN** 各 Cell の右端アクセサリ（Switch / CheckBox / チェックマーク / SimpleCheck）の右端 X 座標がすべて一致する（ピクセル単位の差は ±1 px 以内）

#### Scenario: タップで toggle

- **GIVEN** `CheckboxCell(isChecked: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `onValueChanged(true)` が呼ばれ、内部状態が更新されると次回レンダリング時にチェックマークが表示される

## ADDED Requirements

### Requirement: 全 Cell 共通の isEnabled

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて `isEnabled: Bool`（既定 `true`）フィールドを持たなければならない (SHALL)。

`isEnabled = false` のとき：

- 当該 Cell のコントロール要素（`SwitchCell` のスイッチ、`CheckboxCell` のチェックボックス、`RadioCell` / `SimpleCheckCell` のチェック表示要素、`CommandCell` / `ButtonCell` のタップ可能領域）はユーザー操作に応答してはならない (MUST NOT)。具体的には、UI コントロールの `isEnabled` を `false` にし、Cell コンテナのタップハンドラを無効化する。
- 当該 Cell のタイトル／説明文／値テキスト／ヒントテキストの色は **`Theme.disabledTextColor`** に置換されなければならない (MUST)。Cell 全体への `alpha` 適用や半透明化は行ってはならない (MUST NOT)。
- `LabelCell` は元来コントロール要素を持たないが、`isEnabled = false` の場合もテキスト色置換規則は同様に適用しなければならない (MUST)。

`isEnabled = true`（既定値）のときは、本変更提案の他の Requirement に従う通常の描画・操作を行う。

#### Scenario: SwitchCell の isEnabled = false

- **GIVEN** `SwitchCell(title: "通知", isOn: true, isEnabled: false)`
- **WHEN** SettingsView に表示してユーザーがスイッチをタップしようとする
- **THEN** スイッチ UI は disabled 表示となりタップに反応せず、タイトル色は `Theme.disabledTextColor` に置換される。`onValueChanged` は発火しない

#### Scenario: CommandCell の isEnabled = false

- **GIVEN** `CommandCell(title: "ライセンス", isEnabled: false, onTap: {...})`
- **WHEN** SettingsView に表示してユーザーが Cell をタップする
- **THEN** タップは無効化されており `onTap` は呼ばれない。タイトル・説明文の色は `Theme.disabledTextColor` に置換される

#### Scenario: LabelCell の isEnabled = false

- **GIVEN** `LabelCell(title: "通知", description: "プッシュ通知設定", valueText: "オン", isEnabled: false)`
- **WHEN** SettingsView に表示される
- **THEN** タイトル・説明文・値テキストすべての色が `Theme.disabledTextColor` に置換される。コントロール要素はないため操作面での変化はない

#### Scenario: API 互換性（既存呼び出し）

- **GIVEN** 既存のコード `SwitchCell(title: "通知", isOn: true)`（`isEnabled` を指定しない呼び出し）
- **WHEN** コンパイル・実行する
- **THEN** `isEnabled` は既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: isEnabled 変更時の差分検出

- **GIVEN** 同一 id の Cell について `isEnabled = true → false` に変更
- **WHEN** Diff 検出（DSL 経路または `SettingsRootDiff.replaceCell`）が走る
- **THEN** Section 構造の追加・削除ではなく `replaceCell` 経路で同一 ViewHolder に対する内容更新（reconfigureItems / notifyItemChanged）として反映される

### Requirement: 全 Cell 共通の Theme.titleColor / Theme.titleFont 反映

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）はすべて、タイトルの色／フォントを次の 3 段階優先順位で解決しなければならない (MUST)：

1. 当該 Cell の `CellStyle.titleColor` / `CellStyle.titleFont` が指定されていればそれを採用
2. それ以外で `Theme.titleColor` / `Theme.titleFont` が指定されていればそれを採用
3. それ以外はプラットフォーム既定（iOS: `UIColor.label` / `UIFont.preferredFont(forTextStyle: .body)`、Android: `TextView` 既定色・既定フォント）

`ButtonCell` に限り、第 4 段階としてプラットフォーム標準ボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）が追加され、4 段階目に位置する（Requirement: ButtonCell を参照）。

#### Scenario: Theme.titleColor が全 Cell タイトル色に反映される

- **GIVEN** `Theme(titleColor: KsColor.purple)` で初期化された SettingsView に `LabelCell` / `SwitchCell` / `CheckboxCell` などが並ぶ。各 Cell の `CellStyle.titleColor = nil`
- **WHEN** SettingsView が描画される
- **THEN** すべての Cell のタイトル文字色が紫（`Theme.titleColor`）に統一される

#### Scenario: CellStyle.titleColor が Theme.titleColor より優先

- **GIVEN** `Theme(titleColor: KsColor.purple)`、`LabelCell(title: "強調", style: CellStyle(titleColor: KsColor.orange))`
- **WHEN** Cell が描画される
- **THEN** 当該 Cell のタイトル色は橙（`CellStyle.titleColor` 優先）、他 Cell は紫（Theme 由来）

#### Scenario: Theme.titleColor が nil の場合のフォールバック

- **GIVEN** `Theme()`（`titleColor = nil`）、`LabelCell(title: "標準")` で `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** タイトル色はプラットフォーム既定（iOS: `UIColor.label`、Android: `TextView` 既定色）になる

