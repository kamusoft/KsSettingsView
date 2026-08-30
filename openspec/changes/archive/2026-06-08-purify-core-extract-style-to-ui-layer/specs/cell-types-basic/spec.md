## MODIFIED Requirements

### Requirement: KsImage 値型

`KsImage` は Cell のアイコン表現を運ぶ sealed 型でなければならない (SHALL)。**`KsImage` は `KsSettingsViewUI`（iOS）および `ks-settingsview-ui`（Android）に所属する (MUST)。`KsSettingsViewCore` / `ks-settingsview-core` には所属してはならない (MUST NOT)**。プラットフォーム UI 型（`UIImage`、`Drawable`）を派生の中に保持する（プラットフォーム固有派生として隔離される）。`Hashable` / `equals` 契約を満たさなければならない (MUST)。

iOS 側（`KsSettingsViewUI`）は次の派生を持たなければならない (MUST)：

- `systemName(String)`: SF Symbols 名を保持する派生
- `uiImage(UIImage)`: 任意の `UIImage` を保持する派生

Android 側（`ks-settingsview-ui`）は次の派生を持たなければならない (MUST)：

- `Resource(@DrawableRes resId: Int)`: Android リソース ID を保持する派生（主軸）
- `Drawable(android.graphics.drawable.Drawable)`: 任意の `Drawable` を保持する派生
- `SystemName(String)`: iOS との API 対称性のための派生（Android では解決不可、UI 層は無視する）

#### Scenario: iOS の派生定義

- **GIVEN** Swift `KsSettingsViewUI` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `public enum KsImage: Hashable` であり、`case systemName(String)` と `case uiImage(UIImage)` の 2 ケースを持つ。`Hashable` 実装は、`systemName` ケースは内部 String の hash、`uiImage` ケースは `ObjectIdentifier(uiImage)` 相当の参照同一性で hash する

#### Scenario: Android の派生定義

- **GIVEN** Kotlin `ks-settingsview-ui` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `sealed interface KsImage` であり、サブタイプとして `data class Resource(@DrawableRes val resId: Int) : KsImage`、`class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage`、`data class SystemName(val name: String) : KsImage` の 3 派生を持つ。`Drawable` は参照同一性で `equals` / `hashCode` を持ち、`Resource` / `SystemName` は値同一性で `equals` / `hashCode` を持つ

#### Scenario: iOS の構築

- **GIVEN** Swift コード（`import KsSettingsViewUI` 済み）
- **WHEN** `KsImage.systemName("bell")` または `KsImage.uiImage(UIImage(systemName: "bell")!)` を構築する
- **THEN** 該当ケースのイミュータブル値として生成され、パターンマッチ可能となる

#### Scenario: Android の構築

- **GIVEN** Kotlin コード（`import jp.kamusoft.kssettingsview.ui.KsImage` 済み）
- **WHEN** `KsImage.Resource(R.drawable.ic_settings)` または `KsImage.Drawable(ContextCompat.getDrawable(context, R.drawable.ic_settings)!!)` または `KsImage.SystemName("bell")` を構築する
- **THEN** 該当派生のイミュータブル値として生成され、`when` でパターンマッチ可能となる

#### Scenario: Core モジュールには所属しない

- **GIVEN** iOS の `import KsSettingsViewCore`、Android の `import jp.kamusoft.kssettingsview.core.*`
- **WHEN** `KsImage` を参照する
- **THEN** 解決できずビルドエラーとなる。`KsImage` は UI 層モジュールへのインポートが必須

### Requirement: ButtonCell

`ButtonCell` はボタン用途のセルでなければならない (SHALL)。`title` をボタンスタイルで表示しなければならない (MUST)。タップで `onTap` を発火しなければならない (MUST)。**`titleColor` の型は Native 型 (`UIColor?` / Compose `Color?`) でなければならない (MUST)**。ボタンテキストの色は次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別、Optional、Native 型）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor`（Native 型）が指定されていればそれを採用
3. それ以外で `Theme.titleColor`（Native 型）が指定されていればそれを採用
4. それ以外はプラットフォーム標準のボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）

タイトルの水平方向の揃え位置は `titleAlignment: CellTitleAlignment`（既定 `.center`）で指定できなければならない (MUST)。

#### Scenario: Theme.titleColor が ButtonCell に効く

- **GIVEN** iOS の `Theme(titleColor: UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0))`、`ButtonCell(title: "登録", titleColor: nil)`、当該 Cell の `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** ボタンテキストの色は `Theme.titleColor` 由来の橙系色になる（プラットフォーム標準ボタン色ではない）

#### Scenario: Cell 個別 titleColor が Theme より優先

- **GIVEN** iOS の `Theme(titleColor: UIColor.green)`、`ButtonCell(title: "削除", titleColor: UIColor.red)`
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
- **THEN** Cell の右端(トレーリング側)寄りにタイトルが表示される

#### Scenario: titleAlignment 省略時の既定値と API 互換性

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`（`titleAlignment` を指定しない既存呼び出し）
- **WHEN** コンパイル・実行してインスタンスを参照する
- **THEN** `buttonCell.titleAlignment == .center` で、ビルドエラーや実行時エラーは発生しない

### Requirement: SwitchCell

`SwitchCell` は ON/OFF を切り替えるトグルスイッチを持つセルでなければならない (SHALL)。`title`、`description`（任意）、`isOn: Bool` を持ち、ユーザーがスイッチを操作したときに `onValueChanged` 通知を発火しなければならない (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**）でスイッチ ON 時の色を指定できなければならない (MUST)。

#### Scenario: 初期状態の表示

- **GIVEN** `SwitchCell(title: "通知", isOn: true)`
- **WHEN** SettingsView に表示される
- **THEN** 右側に UISwitch（または SwitchCompat）が ON 状態で表示される

#### Scenario: ユーザー操作で値が変わる

- **GIVEN** `SwitchCell(title: "通知", isOn: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーがスイッチをタップして ON にする
- **THEN** `onValueChanged(true)` が呼ばれる

#### Scenario: accentColor の型

- **GIVEN** iOS で `SwitchCell(title: "通知", isOn: true, accentColor: UIColor.green)` または Android で `SwitchCell(title = "通知", isOn = true, accentColor = Color.Green)`
- **WHEN** コンパイルする
- **THEN** ビルドエラーなく構築できる。`KsColor` を渡そうとするとビルドエラーとなる

### Requirement: CheckboxCell

`CheckboxCell` は ON/OFF をチェックマークで表すセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**）でチェック時の塗り色を指定できなければならない (MUST)。チェック時のアイコン（accent 表示）は `CellStyle.accentColor` または `Theme.cellAccentColor` で着色されなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `CheckBox`（`UIButton` + `Draw`）相当の **角丸の四角いチェックボックス UI** でなければならない (MUST)。すなわち、角丸（CornerRadius 相当）の四角枠（BorderWidth 相当）を持ち、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークを重ね、非チェック時は枠のみを表示する。このチェックボックスは右端に `UICellAccessory.customView`（`placement: .trailing`）として常設し、チェック状態の切り替えは accessory の追加・削除ではなくカスタム View 内部の再描画で行わなければならない (MUST)（追加・削除に伴うスライドアニメーションを避けるため）。

Android では、チェック表現は `com.google.android.material.checkbox.MaterialCheckBox` を用いた角丸の四角いチェックボックスでなければならない (MUST)。`MaterialCheckBox` 自体の内側 padding（タッチ域確保のための既定 padding）は `setPadding(0, 0, 0, 0)` および `minimumWidth = 0` / `minimumHeight = 0` で無効化し、accessoryHolder 右端と CheckboxCell のチェックボックス右端が SwitchCell / RadioCell / SimpleCheckCell と同一 X 座標に揃わなければならない (MUST)。`buttonTintList` は実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）で着色されなければならない (MUST)。

#### Scenario: チェック状態の表示（iOS）

- **GIVEN** `CheckboxCell(title: "規約に同意", isChecked: true)`
- **WHEN** iOS で表示される
- **THEN** 右端に角丸の四角いチェックボックス UI が常設され、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークが重ねて表示される

#### Scenario: チェック状態の表示（Android、accent 色適用）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)` を Android で表示し、`Theme.cellAccentColor = Color.Yellow`
- **WHEN** ViewHolder が bind する
- **THEN** `MaterialCheckBox.buttonTintList` が黄色（`Theme.cellAccentColor` 由来）で着色される

### Requirement: 全 Cell 共通の Theme.titleColor / Theme.titleFont 反映

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）はすべて、タイトルの色／フォントを次の 3 段階優先順位で解決しなければならない (MUST)：

1. 当該 Cell の `CellStyle.titleColor` / `CellStyle.titleFont` が指定されていればそれを採用（**型は Native 型**）
2. それ以外で `Theme.titleColor` / `Theme.titleFont` が指定されていればそれを採用（**型は Native 型**）
3. それ以外はプラットフォーム既定（iOS: `UIColor.label` / `UIFont.preferredFont(forTextStyle: .body)`、Android: `TextView` 既定色・既定フォント）

`ButtonCell` に限り、第 4 段階としてプラットフォーム標準ボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）が追加され、4 段階目に位置する（Requirement: ButtonCell を参照）。

#### Scenario: Theme.titleColor が全 Cell タイトル色に反映される

- **GIVEN** iOS の `Theme(titleColor: UIColor.purple)` で初期化された SettingsView に `LabelCell` / `SwitchCell` / `CheckboxCell` などが並ぶ。各 Cell の `CellStyle.titleColor = nil`
- **WHEN** SettingsView が描画される
- **THEN** すべての Cell のタイトル文字色が紫（`Theme.titleColor`）に統一される

#### Scenario: CellStyle.titleColor が Theme.titleColor より優先

- **GIVEN** iOS の `Theme(titleColor: UIColor.purple)`、`LabelCell(title: "強調", style: CellStyle(titleColor: UIColor.orange))`
- **WHEN** Cell が描画される
- **THEN** 当該 Cell のタイトル色は橙（`CellStyle.titleColor` 優先）、他 Cell は紫（Theme 由来）

#### Scenario: Theme.titleColor が nil の場合のフォールバック

- **GIVEN** `Theme()`（`titleColor = nil`）、`LabelCell(title: "標準")` で `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** タイトル色はプラットフォーム既定（iOS: `UIColor.label`、Android: `TextView` 既定色）になる

### Requirement: Compose DSL 拡張関数による Cell 直置き

Compose DSL（`ks-settingsview-compose`）は、各具象 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）について **`DSLSectionScope` の拡張関数** を提供し、`cell(...)` ヘルパを介さず Cell を直接 DSL に並べられるようにしなければならない (SHALL)。各拡張関数の引数として `id: String = "<cell-prefix>-${UUID.randomUUID()}"` を提供し (MUST)、`KsCell` 引数構造を CellHandle として返さなければならない (MUST)。

例: `fun DSLSectionScope.LabelCell(title: String, description: String? = null, valueText: String? = null, icon: KsImage? = null, hintText: String? = null, style: CellStyle = CellStyle()): CellHandle = cell(LabelCell(title = title, description = description, valueText = valueText, icon = icon, hintText = hintText, style = style))`

**`icon` パラメータの型は `KsImage?`（`jp.kamusoft.kssettingsview.ui.KsImage`）でなければならない (MUST)**。`style: CellStyle` の `CellStyle` も UI 層所属。

#### Scenario: LabelCell の DSL 直置き

- **GIVEN** Compose DSL 内で `Section("...") { LabelCell(title = "ストレージ", icon = KsImage.Resource(R.drawable.ic_storage)) }`
- **WHEN** ツリーをビルドする
- **THEN** ビルドエラーなく `CellHandle` が返され、Section の Cell リストに `LabelCell` が並ぶ

#### Scenario: 関連型のインポート

- **GIVEN** 上記コード
- **WHEN** import 文を書く
- **THEN** `import jp.kamusoft.kssettingsview.ui.KsImage` および `import jp.kamusoft.kssettingsview.ui.CellStyle` が必要（Core 側 import では解決できない）
