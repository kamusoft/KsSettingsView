## ADDED Requirements

### Requirement: cell-types-basic 共通規約への opt-in 準拠

本 capability で追加される入力系 Cell（`EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の **5 種**）は、`cell-types-basic` capability で確立された **基本 Cell 7 種の共通規約** に opt-in 準拠しなければならない (SHALL)。具体的には以下を満たさなければならない (MUST)：

- **共通 Optional フィールド**: 入力系 Cell は `description: String?`（既定 `nil`）/ `icon: KsImage?`（既定 `nil`）/ `hintText: String?`（既定 `nil`）/ `isEnabled: Bool`（既定 `true`）/ `isVisible: Bool`（既定 `true`）を持たなければならない (MUST)。`valueText: String?` については `EntryCell` を除く 4 種（`PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`）が持たなければならない (MUST)。`EntryCell` は右側 accessory が `UITextField` / `EditText` であり `valueText` の表示位置と物理的に競合するため `valueText` を持たない (MUST NOT)。
- **isEnabled の効果**: `isEnabled = false` のとき、入力コントロール（`UITextField` / `EditText` / chevron 領域のタップ可能性）はユーザー操作に応答してはならず (MUST NOT)、タイトル / 説明文 / 値テキスト / ヒントテキストの色は `Theme.disabledTextColor` に置換しなければならない (MUST)。`alpha` による半透明化は行ってはならない (MUST NOT)。
- **isVisible の効果と VisibilityAware opt-in**: 入力系 Cell 5 種はすべて `VisibilityAware` プロトコル（iOS）/ interface（Android）に opt-in 準拠しなければならない (MUST)。`isVisible = false` のとき UI 層 visible projection から除外され、構造同期上の削除として検出される。`Hashable` / `equals` / `withDSLID` / `withStyle` / `data class copy()` 経路で `isVisible` を保持しなければならない (MUST)。
- **共通行レイアウト関数経由での描画**: 入力系 Cell の View / ViewHolder は、`cell-types-basic` で確立された共通行レイアウト関数 `applyCellBaseLayout(...)`（iOS）/ `applyCellBaseLayout(views, ...)`（Android）を経由してレイアウトを構成しなければならない (MUST)。各 Cell は accessory slot に Cell 固有のコントロール（`EntryCell` は `UITextField` / `EditText`、ピッカー系 4 種（PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）は chevron）を組み込むだけで、`title` / `description` / `valueText` / `icon` / `hintText` のレイアウトロジックを重複実装してはならない (MUST NOT)。
- **Theme.cellTitleColor / cellTitleFont の 3 段階解決**: 入力系 Cell 5 種は、タイトルの色 / フォントを `CellStyle.titleColor / titleFont → Theme.cellTitleColor / cellTitleFont → プラットフォーム既定`（iOS: `UIColor.label` / `UIFont.preferredFont(forTextStyle: .body)`、Android: `TextView` 既定）の 3 段階優先順位で解決しなければならない (MUST)。
- **accentColor の 4 段階解決**: 入力系 Cell 5 種すべてに `accentColor` プロパティ（任意、Native 型 `UIColor?` / `Color?`、既定 `nil`）を持たせなければならない (MUST)。意味は Cell ごとに異なる（`EntryCell` は caret 色、ピッカー系は選択強調色）。解決順序は `Cell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → プラットフォーム既定` の 4 段階としなければならない (MUST)。
- **style プロパティ（任意保持）**: `cell-types-basic` の既存 Cell（`LabelCell` 等）と同様、入力系 Cell 5 種は `style: CellStyle?`（UI 層所属、`UIColor?` / `UIFont?` / Compose `Color?` / `TextStyle?` を直接保持、既定 `nil`）を **任意プロパティ** として持たなければならない (MUST)。`Cell` / `KsCell` 抽象は `style` プロパティを要求しないため、本プロパティは各入力 Cell が個別に保持する形となる（`DSLStyleModifiable` / `DSLStyleModifiableCell` 準拠手段として）。後述の各 Cell 個別 Requirement の「フィールド一覧」では本プロパティを再掲しないが、入力系 5 種すべてが本規約に従って `style` を保持しなければならない (MUST)。

#### Scenario: 共通 Optional フィールドの存在（EntryCell 以外）

- **GIVEN** `PickerCell(title: "テーマ", description: "外観の切替", valueText: nil, icon: KsImage.systemName("paintpalette"), hintText: "新着", items: ["ライト", "ダーク"], selectedIndex: .constant(0))`
- **WHEN** Cell が表示される
- **THEN** 左端にアイコン、その右にタイトル「テーマ」と説明「外観の切替」が縦並びで配置され、本体行の右側中央に chevron、セル右上に float で「新着」が表示される。`valueText` は `nil` のため Decision 7 に従い「ライト」（現在の選択値の文字列化）が自動表示される

#### Scenario: EntryCell は valueText を持たない

- **GIVEN** `EntryCell` のコンストラクタおよび DSL 拡張関数のシグネチャ
- **WHEN** `EntryCell(title: "ニックネーム", valueText: "X", text: $userName)` のように `valueText` 引数を渡そうとコンパイルする
- **THEN** **コンパイルエラー** になる（`EntryCell` には `valueText` パラメータが定義されていない）。これは旧 `AiForms.Maui.SettingsView/SettingsView/Cells/EntryCell.cs` が `ValueText` を入力値そのものとして再利用する設計の踏襲である

#### Scenario: 共通行レイアウト関数経由での描画

- **GIVEN** 任意の入力系 Cell（例: `TimePickerCell(title: "アラーム", description: "毎朝", icon: KsImage.systemName("alarm"), time: $morningAlarm)`）
- **WHEN** UI 層が当該 Cell を描画する
- **THEN** UI 層は共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(views, ...)`）を経由して `title` / `description` / `valueText`（現在時刻の "HH:mm" 表現）/ `icon` / `hintText` を配置し、accessory slot に chevron を組む。View / ViewHolder 側で title / description / icon / hintText のレイアウトロジックを重複実装しない

#### Scenario: isEnabled = false での色置換

- **GIVEN** `NumberPickerCell(title: "音量", value: $volume, isEnabled: false)`、`Theme(disabledTextColor: UIColor.lightGray)`
- **WHEN** SettingsView に表示してユーザーが Cell をタップしようとする
- **THEN** タップは無効化されモーダルは開かず、タイトル「音量」と valueText（現在の音量値の文字列化）の色は `UIColor.lightGray` に置換される。`alpha` による半透明化は発生しない

#### Scenario: isVisible = false で描画除外

- **GIVEN** `DatePickerCell(title: "誕生日", date: $birthday, isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されず、構造同期上は削除として検出される。`SettingsRoot.sections[*].cells` 上ではデータとして保持される

#### Scenario: VisibilityAware への opt-in

- **GIVEN** iOS の各入力系 Cell struct（`EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`）、および Android の各 data class
- **WHEN** プロトコル / interface 一覧を確認する
- **THEN** iOS では `VisibilityAware` プロトコルに準拠し `var isVisible: Bool { get }` を提供、Android では `VisibilityAware` interface を実装し `val isVisible: Boolean` を提供する。`(cell as? VisibilityAware)?.isVisible` で取得できる

#### Scenario: Theme.cellTitleColor の 3 段階解決

- **GIVEN** `Theme(cellTitleColor: UIColor.purple)` を適用した SettingsView に `EntryCell` / `PickerCell` / `NumberPickerCell` などが並ぶ。各 Cell の `CellStyle.titleColor = nil`
- **WHEN** SettingsView が描画される
- **THEN** すべての入力系 Cell のタイトル文字色が紫（`Theme.cellTitleColor`）に統一される

#### Scenario: accentColor の 4 段階解決（EntryCell）

- **GIVEN** `EntryCell(title: "メモ", text: $memo, accentColor: UIColor.systemPink)`、`Theme(cellAccentColor: UIColor.systemBlue)`
- **WHEN** iOS で表示される
- **THEN** `UITextField.tintColor` は `systemPink`（`EntryCell.accentColor` 最優先）になり、caret / 選択ハイライトに反映される

#### Scenario: 任意プロパティ style: CellStyle? の保持

- **GIVEN** iOS の `EntryCell(title: "メモ", text: $memo, style: CellStyle(titleColor: UIColor.orange))` および Android の `EntryCell(title = "メモ", text = state, style = CellStyle(titleColor = Color(0xFFFF8800)))`
- **WHEN** インスタンスを生成して `style` プロパティを参照する
- **THEN** UI 層所属の `CellStyle`（`UIColor?` / `UIFont?` / Compose `Color?` / `TextStyle?` を直接保持）が代入されており、`DSLStyleModifiable` / `DSLStyleModifiableCell` 経由の `withStyle(...)` / `withDSLStyle(...)` で別 `CellStyle` を持つ新インスタンスを生成できる。`style` 引数を省略した場合は既定 `nil` が適用される

### Requirement: 入力系 Cell の id デフォルト値規約

本 capability で追加されるすべての入力系 Cell は、UI 層（`KsSettingsViewUI` / `ks-settingsview-ui`）に配置され、`settings-view-ios-swiftui` / `settings-view-android-compose` で確定した「具象 Cell コンストラクタの `id` デフォルト値規約」に従わなければならない (SHALL)。

- iOS: 各 Cell struct は `id: UUID = UUID()` のデフォルト値を持たなければならない (MUST)
- Android: 各 Cell data class は `id: String = "<className>-${java.util.UUID.randomUUID()}"` のデフォルト値を持たなければならない (MUST)（例: `EntryCell` は `"entry-cell-${...}"`）
- DSL 経路では `DSLReidentifiable.withDSLID(_:)`（iOS）/ `DSLReidentifiableCell.withDSLId(...)`（Android）により、`(SectionID, indexInSection, CellType)` ハッシュベースの安定 ID に rebind されなければならない (MUST)
- 利用者は DSL 内で `EntryCell(title: "ニックネーム", text: $userName)` のように `id` 引数を省略して記述できなければならない (MUST)

#### Scenario: id 引数省略で生成

- **GIVEN** iOS `EntryCell(title: "ニックネーム", text: .constant(""))`、Android `EntryCell(title = "ニックネーム", text = remember { mutableStateOf("") })`（`id` 引数省略）
- **WHEN** Cell インスタンスを生成する
- **THEN** iOS では `id` が `UUID()` で自動採番された値、Android では `"entry-cell-${ランダム UUID}"` 形式の文字列が `id` に格納される。コンパイル・実行ともにエラーは出ない

#### Scenario: DSL 経路での id rebind

- **GIVEN** DSL 経路で `Section("入力") { EntryCell(title: "...", text: $text) }`
- **WHEN** DSL → Diff 算出ロジックが評価される
- **THEN** コンストラクタデフォルト値の `id` は本仕様の優先順位に従う安定 ID に rebind され、Recomposition / body 再評価をまたいで同じ ID を保持する

### Requirement: TwoWay binding の DSL 規約

本 capability で追加される入力系 Cell 5 種は、状態フィールドを TwoWay binding 引数で受け取らなければならない (SHALL)。

- iOS DSL：`@Binding<T>` を引数に取らなければならない (MUST)
- Android Compose DSL：`androidx.compose.runtime.MutableState<T>` を引数に取らなければならない (MUST)

各 Cell の TwoWay binding 引数：

| Cell | iOS | Android |
|---|---|---|
| `EntryCell` | `text: Binding<String>` | `text: MutableState<String>` |
| `PickerCell`（`selectionMode = .single`） | `selectedIndex: Binding<Int?>` | `selectedIndex: MutableState<Int?>` |
| `PickerCell`（`selectionMode = .multiple`） | `selectedIndices: Binding<Set<Int>>` | `selectedIndices: MutableState<Set<Int>>` |
| `NumberPickerCell` | `value: Binding<Int>` | `value: MutableState<Int>` |
| `TimePickerCell` | `time: Binding<Date>`（`Foundation.Date`） | `time: MutableState<LocalTime>`（`java.time.LocalTime`） |
| `DatePickerCell` | `date: Binding<Date>`（`Foundation.Date`） | `date: MutableState<LocalDate>`（`java.time.LocalDate`） |

`TimePickerCell` / `DatePickerCell` の日時型は **Native 型を直接公開する** (MUST)。独自値型（`KsTime` / `KsDate`）は導入しない (MUST NOT)。`TimePickerCell` は `Date` / `LocalTime` の **時刻成分（hour / minute）のみ** を使用する。`DatePickerCell` は `Date` / `LocalDate` の **日付成分（year / month / day）のみ** を使用する。

`Cell` 値型を直接構築する経路（Store 方式・外部から `Cell` 値を渡す場合）では、TwoWay binding ではなく **値 + Cell 固有名の callback**（iOS は `((T) -> Void)?`、Android は `((T) -> Unit)?` の Optional 関数型）を併設しなければならない (MUST)。callback の具体名は各 Cell Requirement で個別に定義する（一覧）：

| Cell | callback 名 |
|---|---|
| `EntryCell` | `onTextChanged` |
| `PickerCell`（`.single`） | `onSelectionChanged`（`(Int) -> Void/Unit`） |
| `PickerCell`（`.multiple`） | `onMultiSelectionChanged`（`(Set<Int>) -> Void/Unit`） |
| `NumberPickerCell` | `onValueChanged` |
| `TimePickerCell` | `onValueChanged`（`(Date) -> Void` / `(LocalTime) -> Unit`） |
| `DatePickerCell` | `onValueChanged`（`(Date) -> Void` / `(LocalDate) -> Unit`） |

例: `EntryCell(title: "メモ", text: "現在値", onTextChanged: { newText in ... })`、`PickerCell(title: "テーマ", items: themes, selectedIndex: 0, onSelectionChanged: { newIndex in ... })`。

ユーザー操作によりコントロールの値が変わったとき、UI 層は TwoWay binding の setter（または対応する Cell 固有名 callback）を呼んで通知しなければならない (MUST)。逆に外部から binding 元の値が変化したときは、Recomposition / body 再評価経由で同一 id の Cell に対する `replaceCell` Diff が発行され、内容更新（reconfigure）として反映されなければならない (MUST)（破棄・再生成は MUST NOT）。

#### Scenario: EntryCell の TwoWay 入力反映（iOS）

- **GIVEN** SwiftUI で `@State var userName = ""` と `EntryCell(title: "名前", text: $userName)` を DSL 経路で配置
- **WHEN** ユーザーが `UITextField` に "Taro" と入力する
- **THEN** 1 文字入力ごとに `userName` State が更新され、SwiftUI body が再評価される。同一 id の Cell に対する `replaceCell` Diff が発行され、Cell View は同一セルの reconfigure で表示更新される（破棄・再生成は発生しない）

#### Scenario: EntryCell の TwoWay 入力反映（Android）

- **GIVEN** Compose で `val userName = remember { mutableStateOf("") }` と `EntryCell(title = "名前", text = userName)` を DSL 経路で配置
- **WHEN** ユーザーが `EditText` に "Hanako" と入力する
- **THEN** 1 文字入力ごとに `userName.value` が更新され、Compose Recomposition が走る。同一 id の Cell に対する `replaceCell` Diff が発行され、ViewHolder の reconfigure で内容更新される（ViewHolder の破棄・再生成は発生しない）

#### Scenario: Store 経路での callback 利用

- **GIVEN** `let cell = EntryCell(title: "メモ", text: store.currentMemo, onTextChanged: { newText in store.replaceMemoCell(newText: newText) })` を Store 経由で配置
- **WHEN** ユーザーが入力する
- **THEN** `onTextChanged(newText)` callback が逐次呼ばれ、`store.replaceCell(...)` 経由で Diff が発行される

#### Scenario: 外部からの binding 元変化が Cell に反映

- **GIVEN** `@State var alarmTime: Date = Calendar.current.date(bySettingHour: 7, minute: 0, second: 0, of: Date())!` と `TimePickerCell(title: "アラーム", time: $alarmTime)` が表示中
- **WHEN** 外部処理で `alarmTime = Calendar.current.date(bySettingHour: 8, minute: 30, second: 0, of: Date())!` を代入
- **THEN** SwiftUI body 再評価で同一 id の `TimePickerCell` に対する `replaceCell` Diff が発行され、Cell View の valueText が "08:30" に reconfigure 更新される

### Requirement: Compose DSL 拡張関数による入力 Cell 直置き

本 capability で追加される Compose 側の各入力系 Cell は、`settings-view-android-compose` で確定した「具象 Cell 型ごとの DSL 拡張関数」規約に従い、`DSLSectionScope` の拡張関数として直置き API を提供しなければならない (SHALL)。

例のシグネチャ（Decision 8 の既定値に従う）：

```kotlin
fun DSLSectionScope.EntryCell(
    title: String,
    text: MutableState<String>,
    description: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    placeholder: String? = null,
    keyboardType: Int = android.text.InputType.TYPE_CLASS_TEXT,
    isPassword: Boolean = false,
    textAlignment: CellTitleAlignment = CellTitleAlignment.end,
    accentColor: Color? = null,
    maxLength: Int? = null,
    style: CellStyle = CellStyle(),
): CellHandle

// PickerCell（単一選択）
fun DSLSectionScope.PickerCell(
    title: String,
    items: List<String>,
    selectedIndex: MutableState<Int?>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    pageTitle: String? = null,
    displayFormatter: ((String) -> String)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle

// PickerCell（複数選択）
fun DSLSectionScope.PickerCell(
    title: String,
    items: List<String>,
    selectedIndices: MutableState<Set<Int>>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    maxSelectedNumber: Int = 0,
    pageTitle: String? = null,
    displayFormatter: ((String) -> String)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle
```

残り 3 種の DSL 拡張関数シグネチャ：

```kotlin
fun DSLSectionScope.NumberPickerCell(
    title: String,
    value: MutableState<Int>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
    unit: String = "",
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle

fun DSLSectionScope.TimePickerCell(
    title: String,
    time: MutableState<java.time.LocalTime>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    format: String = "HH:mm",
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle

fun DSLSectionScope.DatePickerCell(
    title: String,
    date: MutableState<java.time.LocalDate>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    format: String = "yyyy/MM/dd",
    minDate: java.time.LocalDate? = null,
    maxDate: java.time.LocalDate? = null,
    uiStyle: DatePickerUIStyle = DatePickerUIStyle.Material,
    androidButtonColor: Color? = null,
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle
```

`DatePickerUIStyle` は両プラットフォーム所属の列挙型で、Android では `Material`（既定、`MaterialDatePicker` を使用）/ `Spinner`（`android.widget.DatePicker` の `datePickerMode="spinner"` を使用）の 2 ケースを持つ。iOS には同名の `DatePickerUIStyle` が独立に存在し `.wheels` / `.calendar` ケースを持つ（design.md Decision 12 のクロスプラットフォーム命名規約に従う）。

- 各 DSL 拡張関数の引数 `style: CellStyle` の型 `CellStyle` は **UI 層所属**（`jp.kamusoft.kssettingsview.ui.CellStyle`、Compose `Color?` / `TextStyle?` を直接保持する型）でなければならない (MUST)
- `style: CellStyle = CellStyle()` のデフォルト初期化は、`settings-view-android-style` で UI 層に配置された `CellStyle` 型が **パラメータなしコンストラクタ**（全フィールドにデフォルト値 `null` を持つ）を提供することに依存する (SHALL)
- 各 DSL 拡張関数の戻り値は `CellHandle` でなければならない (MUST)（`.cellHeight(...)` 等の handle 経由 modifier chain を可能にするため）
- 配置先は `ks-settingsview-compose` モジュール、`InputCellDsl.kt`（集約）または `<CellName>Dsl.kt`（個別）

iOS 側では Swift `@resultBuilder SectionBuilder` の機構により Cell 値を直置きできるため、別途 DSL 拡張関数の規約は不要。iOS 各 Cell の `style: CellStyle` プロパティは UI 層所属の `KsSettingsViewUI.CellStyle`（`UIColor?` / `UIFont?` を直接保持）を参照する。

#### Scenario: Compose DSL 内での EntryCell 直置き

- **GIVEN**
  ```kotlin
  val userName = remember { mutableStateOf("") }
  KsSettingsView {
      Section("プロフィール") {
          EntryCell(title = "ニックネーム", text = userName)
      }
  }
  ```
- **WHEN** Composition する
- **THEN** `DSLSectionScope` の拡張関数 `EntryCell(title:, text:, ...)` が解決され、内部で `cell(EntryCell(...))` が呼ばれて Cell が DSL ツリーに追加される。`MutableState<String>` 経由でテキスト変更が双方向バインドされる

#### Scenario: 関連型のインポート

- **GIVEN** 上記コード
- **WHEN** import 文を書く
- **THEN** `import jp.kamusoft.kssettingsview.ui.EntryCell`（DSL 拡張関数）/ `import jp.kamusoft.kssettingsview.ui.KsImage` / `import jp.kamusoft.kssettingsview.ui.CellStyle` が必要。`keyboardType` 用には `android.text.InputType` を import する（Native 型直接使用）

### Requirement: EntryCell

`EntryCell` はテキスト入力用セルでなければならない (SHALL)。以下のフィールドを持たなければならない (MUST)：

- `title: String`
- `description: String?`（既定 `nil`）
- `icon: KsImage?`（既定 `nil`）
- `hintText: String?`（既定 `nil`）
- `isEnabled: Bool`（既定 `true`）
- `isVisible: Bool`（既定 `true`）
- `text: Binding<String>` / `MutableState<String>`（TwoWay）または `text: String` + `onTextChanged: ((String) -> Void)?`（Store 経路）
- `placeholder: String?`（既定 `nil`）
- iOS: `keyboardType: UIKeyboardType`（**`UIKit.UIKeyboardType` を直接公開**、既定 `.default`）
- Android: `keyboardType: Int`（**`android.text.InputType` の `Int` 定数を直接公開**、既定 `InputType.TYPE_CLASS_TEXT`）
- `isPassword: Bool / Boolean`（既定 `false`）
- `textAlignment: CellTitleAlignment`（既定 `.end`、AiForms オリジナル `EntryCell.TextAlignmentProperty` の既定 `TextAlignment.End` 準拠）
- `accentColor: UIColor? / Color?`（既定 `nil`、caret 色および選択ハイライト色）
- `maxLength: Int?`（既定 `nil` = 無制限、旧 AiForms `MaxLength: int` 互換）

右側に `UITextField`（iOS）/ `EditText`（Android）を配置しなければならない (MUST)。iOS の `UITextField` は title ラベルの右に配置され、title の右側に残る領域全幅を占有しなければならない (MUST)。

iOS のレイアウト構造は AiForms.Maui.SettingsView オリジナル `EntryCellView.cs` 準拠の `_FieldWrapper` 方式に従わなければならない (MUST)：

- `KsListCellBase` の自前 `UIStackView` 階層（`contentStack`）に `trailingViews: [fieldWrapper]` として **`UIView` ラッパ**（`fieldWrapper`）を渡す。`UITextField` を直接 `contentStack` に入れてはならない (MUST NOT)。
- `UITextField` は `fieldWrapper` の subview として **Auto Layout で 4 辺 pin**（`leadingAnchor` / `trailingAnchor` / `topAnchor` / `bottomAnchor` を wrapper のそれぞれに `constraint(equalTo:)`）しなければならない (MUST)。
- `UITextField` サブクラスは `intrinsicContentSize.width` を `UIView.noIntrinsicMetric` に override しなければならない (MUST)。これにより `isSecureTextEntry = true` のとき UIKit が intrinsicContentSize を ~19pt に縮めても wrapper サイズには影響しない。
- `fieldWrapper.setContentHuggingPriority(.init(100), for: .horizontal)` で title 右側の残り領域を吸う設定としなければならない (MUST)。
- 固定幅枠（旧 `UICellAccessory.customView` ベースの 180pt 固定 frame 等）で見切れる実装、または `UITextField` の `intrinsicContentSize` をそのまま使ってレイアウト計算する実装を採用してはならない (MUST NOT)。

`keyboardType` は **Native 型を直接 `UITextField.keyboardType` / `EditText.inputType` に代入** しなければならない (MUST)。独自列挙型（`KsKeyboardType` 等）を挟んではならない (MUST NOT)。`isPassword = true` のとき入力文字をマスクしなければならない (MUST)。フォーカス状態に応じてキーボードを表示し、入力変更で TwoWay binding を更新しなければならない (MUST)。

`maxLength` が `nil` 以外の値で指定されたとき、UI 層は入力された文字数が `maxLength` を超えないよう制限しなければならない (MUST)：

- iOS：`UITextField.delegate` の `textField(_:shouldChangeCharactersIn:replacementString:)` で範囲外の入力を拒否
- Android：`EditText.filters` に `android.text.InputFilter.LengthFilter(maxLength)` を設定

`EntryCell` は `valueText` フィールドを持ってはならない (MUST NOT)（共通規約 Requirement の例外規定に従う）。

iOS の `EntryCell` は AiForms.Maui.SettingsView オリジナル `EntryCellView.cs` 準拠の編集体験を備えなければならない (MUST)：

- **Cell タップ → UITextField フォーカス**: Cell の任意位置をタップしたとき、`UITextField` が `becomeFirstResponder()` でフォーカスを取得しなければならない (MUST)。`UICollectionViewDelegate.collectionView(_:didSelectItemAt:)` 経路で `TapNotifyingRenderer.tapHandler` を介してディスパッチする。
- **Done ツールバー常時表示**: `UITextField.inputAccessoryView` に `UIToolbar`（右端に `UIBarButtonItem(barButtonSystemItem: .done)`、左に `.flexibleSpace`）を **常時設定** しなければならない (MUST)。Done タップで `UITextField.resignFirstResponder()` を呼んでキーボードを閉じる。本ツールバーはオプトイン／オプトアウトの cell プロパティを持たず、すべての EntryCell に常時表示される。生成時 `UIToolbar(frame: CGRect(0, 0, screenWidth, 44))` のように初期 frame を指定し、`autoresizingMask = [.flexibleWidth]` を設定する (MUST)。これは `_UIToolbarContentView.width == 0` と内部 `TB_Trailing_Trailing` 制約の衝突警告を防ぐためで、AiForms オリジナル `new UIToolbar(new CGRect(0, 0, 50, 44))` と同等の対策である。
- **完了キーで閉じる**: `UITextFieldDelegate.textFieldShouldReturn(_:)` を実装し、`UITextField.resignFirstResponder()` を呼んで `true` を返さなければならない (MUST)。AiForms オリジナル `EntryCellView.cs` の `OnShouldReturn(...)` 準拠。
- **日本語 IME 対応 (差分判定)**: `render` 内で `UITextField.text` を更新する際は、**現在値と新値が異なる場合のみ代入** しなければならない (MUST)。同値の再代入は IME のマークドテキスト（変換途中の下線付きテキスト）を破壊し、日本語等 2 バイト言語の入力を不能にするため、`if textField.text != entry.text { textField.text = entry.text }` の形式で差分判定を行う。AiForms オリジナル `EntryCellView.cs` `UpdateValueText()` のコメント「`Without this judging, TextField don't correctly work when inputting Japanese (maybe other 2byte languages either).`」準拠。
- **secureTextEntry の自動クリア対策**: `isPassword = true` の `UITextField` は iOS の仕様により、フォーカス取得後の初回入力時に既存テキストが自動的に全クリアされる（pasteboard 経由のパスワード窃取防止機構）。これを抑止しなければならない (MUST)。`UITextFieldDelegate.textFieldDidBeginEditing(_:)` で `isSecureTextEntry && text 非空` のとき現在の text を退避し、`editingChanged` の初回で退避値を prepend する形で復元する。`textFieldDidEndEditing(_:)` で退避値をクリアする。

#### Scenario: テキスト入力で binding が更新される

- **GIVEN** `EntryCell(title: "ニックネーム", text: $userName)` が表示されている
- **WHEN** ユーザーが UITextField / EditText に "k", "ka", "kam" と入力する
- **THEN** 1 文字入力ごとに `userName` Binding / `userName.value` が "k" → "ka" → "kam" と更新される

#### Scenario: KeyboardType の反映（Native 型直接公開）

- **GIVEN** iOS で `EntryCell(title: "電話", text: $phone, keyboardType: .phonePad)`（`UIKeyboardType` を直接渡す）、または Android で `EntryCell(title = "電話", text = phone, keyboardType = InputType.TYPE_CLASS_PHONE)`（`Int` を直接渡す）
- **WHEN** UITextField / EditText を取得する
- **THEN** iOS では `UITextField.keyboardType == UIKeyboardType.phonePad`、Android では `EditText.inputType == InputType.TYPE_CLASS_PHONE` が設定されている。独自列挙型を経由した変換ロジックは存在しない

#### Scenario: KeyboardType 引数の型は Native 型

- **GIVEN** iOS / Android の `EntryCell` のコンストラクタおよび DSL 拡張関数のシグネチャ
- **WHEN** 型一覧を確認する
- **THEN** `keyboardType` 引数の型は iOS が `UIKit.UIKeyboardType`、Android が `Int`（`android.text.InputType` の定数を渡すことを想定）であり、独自列挙型 `KsKeyboardType` のような名前の型は **存在しない**

#### Scenario: isPassword の反映

- **GIVEN** `EntryCell(title: "パスワード", text: $password, isPassword: true)`
- **WHEN** ユーザーが入力する
- **THEN** iOS では `UITextField.isSecureTextEntry = true` で表示文字が "●" にマスクされ、Android では `EditText.inputType` に `InputType.TYPE_TEXT_VARIATION_PASSWORD` が併用されて同様にマスクされる

#### Scenario: iOS パスワードのフォーカス時自動クリア抑止

- **GIVEN** iOS の `EntryCell(title: "パスワード", text: $password, isPassword: true)` で `password = "secret123"` が既に入力されている状態
- **WHEN** ユーザーが `UITextField` にフォーカスし、続けて 1 文字 "x" を入力する
- **THEN** `UITextField.text == "secret123x"` となり、`$password == "secret123x"` となる。iOS 仕様で発生するはずの「初回入力時に既存テキストが全クリアされる」現象は抑止される

#### Scenario: iOS 日本語 IME 入力対応

- **GIVEN** iOS の `EntryCell(title: "名前", text: $name)` が描画されている
- **WHEN** ユーザーが日本語 IME で「あいう」と入力する（変換中の状態を含む）
- **THEN** 変換途中で `render` が再呼び出しされても IME のマークドテキスト（下線付き表示）は破壊されず、確定までスムーズに入力できる。これは `render` 内で `if textField.text != entry.text { textField.text = entry.text }` の差分判定で同値再代入を回避することにより成立する

#### Scenario: accentColor の caret 反映

- **GIVEN** `EntryCell(title: "メモ", text: $memo, accentColor: UIColor.systemPink)`
- **WHEN** iOS で `UITextField` がフォーカスを得る
- **THEN** caret（カーソル）と選択ハイライトが pink で描画される（`tintColor = systemPink`）

#### Scenario: maxLength の制限

- **GIVEN** `EntryCell(title: "ニックネーム", text: $userName, maxLength: 5)`（`userName` 初期値 `""`）
- **WHEN** ユーザーが UITextField / EditText に "abcdefg" と入力しようとする
- **THEN** 入力は 5 文字（"abcde"）でブロックされ、6 文字目以降の入力は無視される。`userName` Binding / `userName.value` も最大 `"abcde"` までしか更新されない

#### Scenario: maxLength = nil で無制限

- **GIVEN** `EntryCell(title: "メモ", text: $memo)`（`maxLength` 省略）
- **WHEN** 任意の長さの文字列を入力する
- **THEN** 入力に長さ制限は適用されない。`memo` Binding / `memo.value` は入力された文字列をそのまま保持する

#### Scenario: iOS UITextField は title 残り領域全幅を占有する

- **GIVEN** iOS の `EntryCell(title: "メール", text: $email)` が描画されている
- **WHEN** Cell 内の subview を観察する
- **THEN** `KsListCellBase.contentStack.arrangedSubviews` の末尾に `fieldWrapper`（`UIView`）が追加されており、`fieldWrapper.subviews` の中に `UITextField` が存在する。`UITextField` は wrapper に Auto Layout 4 辺 pin（`leadingAnchor` / `trailingAnchor` / `topAnchor` / `bottomAnchor`）で配置されている。`UITextField` サブクラスの `intrinsicContentSize.width == UIView.noIntrinsicMetric` であり、`fieldWrapper.contentHuggingPriority(for: .horizontal) == 100` である。長文を入力しても title 列の右側に残る領域全幅で表示され、180pt 固定幅で見切れることはない。`isPassword = true` で `●` 文字列が長くなっても末尾 / 左端の見切れは発生しない

#### Scenario: iOS Cell タップでフォーカスが当たる

- **GIVEN** iOS の `EntryCell(title: "名前", text: $name)` が描画されており、`UITextField` は first responder ではない状態
- **WHEN** Cell の任意位置（title ラベル付近含む）をタップする
- **THEN** `UICollectionViewDelegate.collectionView(_:didSelectItemAt:)` 経路で `tapHandler` が呼ばれ、`UITextField.becomeFirstResponder()` が実行され、キーボードが表示される

#### Scenario: iOS Done ツールバーが常時表示される

- **GIVEN** iOS の任意の `EntryCell` のレンダリング結果
- **WHEN** `UITextField.inputAccessoryView` を観察する
- **THEN** `UIToolbar` が設定されており、`items` の末尾が `UIBarButtonItem(barButtonSystemItem: .done)` である。Done タップで `UITextField.resignFirstResponder()` が呼ばれ、キーボードが閉じる

### Requirement: EntryCell 編集中のキーボード自動非表示

iOS の `KsSettingsViewController` は、`UICollectionView.keyboardDismissMode` を `.onDrag` に設定しなければならない (MUST)。ユーザーが list を drag scroll したとき、編集中の `UITextField` の first responder が解除されキーボードが閉じる。AiForms.Maui.SettingsView オリジナル `AiTableView` の `KeyboardDismissMode = .OnDrag` 挙動と等価。

#### Scenario: スクロールでキーボードが閉じる

- **GIVEN** iOS の `EntryCell` を編集中（`UITextField` が first responder）
- **WHEN** ユーザーが `UICollectionView` を drag scroll する
- **THEN** `UITextField` が resign first responder し、キーボードが閉じる

#### Scenario: keyboardDismissMode の設定

- **GIVEN** iOS の `KsSettingsViewController` の `loadView()` 実行直後
- **WHEN** `collectionView.keyboardDismissMode` を観察する
- **THEN** `.onDrag` に設定されている

### Requirement: PickerCell

`PickerCell` は候補リストから **単一または複数** の項目を選択するセルでなければならない (SHALL)。以下のフィールドを持つ (MUST)：

- `title: String`
- `description: String?` / `valueText: String?` / `icon: KsImage?` / `hintText: String?` / `isEnabled: Bool` / `isVisible: Bool`（共通規約）
- `items: [String]`
- `pageTitle: String?`（既定 `nil`、モーダル画面のナビゲーションバータイトル）
- `displayFormatter: ((String) -> String)?`（既定 `nil`、表示用フォーマッタ）
- `accentColor: UIColor? / Color?`（既定 `nil`、選択中項目のチェック色）
- `selectionMode: PickerSelectionMode`（`.single` / `.multiple`、既定 `.single`、UI 層所属の列挙型）
- `selectionMode = .single` の TwoWay binding：`selectedIndex: Binding<Int?> / MutableState<Int?>` または `selectedIndex: Int?` + `onSelectionChanged: ((Int) -> Void/Unit)?`
- `selectionMode = .multiple` の TwoWay binding：`selectedIndices: Binding<Set<Int>> / MutableState<Set<Int>>` または `selectedIndices: Set<Int>` + `onMultiSelectionChanged: ((Set<Int>) -> Void/Unit)?`
- `maxSelectedNumber: Int`（既定 `0`、`0` は無制限。`.multiple` モードでのみ意味を持つ）

旧 `AiForms.Maui.SettingsView` の `TextPickerCell`（`SelectedItem: String` ベース）は本 capability では **移植対象外** とする (MUST NOT 提供)。利用者は `PickerCell(selectionMode: .single, ..., selectedIndex: $index)` で代替し、`items[index]` で `String` 値を取得することとする（移行ガイドは `docs/cell-types-input.md` 参照）。

Cell 表示時、`valueText` 引数が `nil` の場合は **現在の選択値を文字列化したもの** を valueText slot に自動表示しなければならない (MUST)：

- `PickerCell(.single)`: `selectedIndex` が `n` のとき `items[n]` を表示（`displayFormatter` 指定時は `displayFormatter(items[n])`）
- `PickerCell(.multiple)`: 選択された項目を `, ` で連結した文字列を表示（例: `selectedIndices = {0, 2}` / `items = ["A", "B", "C"]` → `"A, C"`）。`displayFormatter` 指定時は各項目に適用してから連結。横幅超過時は ellipsize end

タップで選択モーダルを開かなければならない (MUST)。

- `PickerCell(.single)`：iOS は `UITableViewController` を `UINavigationController` でモーダル提示、Android は `AlertDialog.Builder().setSingleChoiceItems`
- `PickerCell(.multiple)`：iOS は `UITableViewController`（`allowsMultipleSelection = true`）+ navigation bar の「完了」ボタンで dismiss、Android は `AlertDialog.Builder().setMultiChoiceItems` + 「完了」ボタン

`PickerCell(.multiple)` で `maxSelectedNumber > 0` の場合、選択数が上限に達した状態で未選択の項目をユーザーがチェックしようとしても、UI 層は新規チェックを **無視** しなければならない (MUST)（既選択は維持）。同時に軽い触覚フィードバック（iOS は `UIImpactFeedbackGenerator(style: .light).impactOccurred()`、Android は `view.performHapticFeedback(HapticFeedbackConstants.REJECT)`）でユーザーに上限到達を通知すべきである (SHOULD)。

#### Scenario: 現在の選択値の自動 valueText 表示（PickerCell 単一）

- **GIVEN** `PickerCell(title: "テーマ", items: ["ライト", "ダーク", "自動"], selectedIndex: .constant(1))`（`valueText` 省略、`selectionMode` 省略 = `.single`）
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot に "ダーク" と自動表示される

#### Scenario: 現在の選択値の自動 valueText 表示（PickerCell 複数）

- **GIVEN** `PickerCell(title: "通知種別", items: ["メール", "プッシュ", "SMS"], selectedIndices: .constant(Set([0, 2])), selectionMode: .multiple)`（`valueText` 省略）
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot に "メール, SMS" と自動表示される（選択された項目を `, ` で連結）。横幅超過時は ellipsize end で省略表示

#### Scenario: displayFormatter による表示加工

- **GIVEN** `PickerCell(title: "通貨", items: ["JPY", "USD"], selectedIndex: .constant(0), displayFormatter: { "[$0]" })`
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot に "[JPY]" と表示される

#### Scenario: valueText 明示指定が優先

- **GIVEN** `PickerCell(title: "テーマ", valueText: "カスタム", items: ["ライト", "ダーク"], selectedIndex: .constant(0))`
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot は "カスタム"（明示指定）が表示される（自動表示 "ライト" は採用されない）

#### Scenario: タップでモーダル表示（単一選択）

- **GIVEN** `PickerCell(selectionMode: .single, ...)` が表示されている
- **WHEN** Cell をタップする
- **THEN** iOS は `UITableViewController` 形式のモーダルが `UINavigationController` 経由で提示され、Android は `AlertDialog.Builder().setSingleChoiceItems` で単一選択リストが表示される。`pageTitle` が指定されていれば iOS のナビゲーションバーおよび Android の `AlertDialog.setTitle` に反映される

#### Scenario: タップでモーダル表示（複数選択）

- **GIVEN** `PickerCell(selectionMode: .multiple, ...)` が表示されている
- **WHEN** Cell をタップする
- **THEN** iOS は `UITableViewController`（`allowsMultipleSelection = true`、各行 `.checkmark` accessory、navigation bar に「完了」ボタン）が提示され、Android は `AlertDialog.Builder().setMultiChoiceItems` + Positive ボタン「完了」で複数選択 UI が表示される

#### Scenario: 選択変更で binding が更新（PickerCell 単一）

- **GIVEN** モーダルが開いている `PickerCell(items: ["ライト", "ダーク", "自動"], selectedIndex: $themeIndex)`
- **WHEN** ユーザーが "自動" を選択する
- **THEN** モーダルが閉じ、`themeIndex` Binding が `2` に更新される。Cell の valueText 自動表示も "自動" に更新される

#### Scenario: 選択変更で binding が更新（PickerCell 複数）

- **GIVEN** モーダルが開いている `PickerCell(items: ["A", "B", "C"], selectedIndices: $selected, selectionMode: .multiple)`（初期 `selected = {0}`）
- **WHEN** ユーザーが "B" と "C" にチェックを追加して「完了」を押す
- **THEN** モーダルが閉じ、`selected` Binding が `{0, 1, 2}` に更新される。Cell の valueText 自動表示が "A, B, C" に更新される

#### Scenario: maxSelectedNumber の上限到達

- **GIVEN** `PickerCell(items: ["A", "B", "C", "D"], selectedIndices: $selected, selectionMode: .multiple, maxSelectedNumber: 2)`（初期 `selected = {0, 1}`、モーダル展開中）
- **WHEN** ユーザーが未選択の "C" をチェックしようとする
- **THEN** "C" のチェックは入らず（`{0, 1}` のまま維持）、軽い触覚フィードバックが発生する。既選択 "A" / "B" のチェックを外す操作は通常通り可能

#### Scenario: TextPickerCell は本 capability で提供されない

- **GIVEN** 本 capability の公開 API 一覧
- **WHEN** `TextPickerCell` を参照しようとする
- **THEN** 型解決できずコンパイルエラーとなる。利用者は `PickerCell(items: [...], selectedIndex: ...)` を使い、`String` 値が必要な場合は `items[selectedIndex]` で取得する

### Requirement: PickerSelectionMode 列挙型

UI 層（`KsSettingsViewUI` / `ks-settingsview-ui`）は、`PickerCell` の選択モードを表す `PickerSelectionMode` 列挙型を提供しなければならない (SHALL)。

- iOS：`public enum PickerSelectionMode { case single, multiple }`（`Hashable` / `Equatable` 自動準拠）
- Android：`enum class PickerSelectionMode { Single, Multiple }`

#### Scenario: iOS 定義

- **GIVEN** Swift `KsSettingsViewUI` モジュール
- **WHEN** `PickerSelectionMode` を参照する
- **THEN** `public enum PickerSelectionMode: Hashable { case single, case multiple }` として定義されており、`PickerSelectionMode.single` / `.multiple` でケース参照できる

#### Scenario: Android 定義

- **GIVEN** Kotlin `ks-settingsview-ui` モジュール
- **WHEN** `PickerSelectionMode` を参照する
- **THEN** `enum class PickerSelectionMode { Single, Multiple }` として定義されており、`PickerSelectionMode.Single` / `.Multiple` でケース参照できる

### Requirement: NumberPickerCell

`NumberPickerCell` は範囲指定の数値選択セルでなければならない (SHALL)。以下のフィールドを持たなければならない (MUST)：

- `title: String`
- `description: String?` / `valueText: String?` / `icon: KsImage?` / `hintText: String?` / `isEnabled: Bool` / `isVisible: Bool`（共通規約）
- `min: Int`（既定 `0`）
- `max: Int`（既定 `100`）
- `step: Int`（既定 `1`）
- `value: Binding<Int> / MutableState<Int>` または `value: Int` + `onValueChanged: ((Int) -> Void)?`
- `unit: String`（既定 `""`、任意の単位文字列。空文字列以外のとき valueText 自動表示と Picker UI の各候補表示に `"<value> <unit>"` 形式の suffix が付与される。旧 AiForms `NumberPickerCell.Unit` 互換）
- `pickerTitle: String?`（既定 `nil`）
- `accentColor: UIColor? / Color?`（既定 `nil`）

`valueText` 引数が `nil` の場合、現在の `value` と `unit` から以下の規則で文字列化して valueText slot に自動表示しなければならない (MUST)：

- `unit` が空文字列 (`""`) のとき: `String(value)` のみ（例: `value = 50` → `"50"`）
- `unit` が非空のとき: `"\(value) \(unit)"` 形式（例: `value = 15` / `unit = "px"` → `"15 px"`）

iOS の `NumberPickerCell` は **AiForms 互換の埋め込み Picker 方式** で実装しなければならない (MUST)：

- 透明な no-caret な `UITextField` サブクラス (`EmbeddedPickerHostField`) を Cell の `ContentView` の subview に貼り、frame を `ContentView.bounds` に追従させる
- `embeddedField.inputView = UIPickerView()` をセット、`embeddedField.inputAccessoryView` に `[Cancel] [pickerTitle?] [Done]` の `UIToolbar` をセットする
- Cell タップ → `KsSettingsViewController.collectionView(_:didSelectItemAt:)` 経路で `tapHandler` 経由で `embeddedField.becomeFirstResponder()` が呼ばれ、iOS が `UIPickerView` をキーボード位置にスライドアップ表示する
- Done タップ → 選択中の値を `onValueChanged` callback に渡し、`embeddedField.resignFirstResponder()` で picker を閉じる
- Cancel タップ → 選択を提示開始時点 (`preSelectedIndex`) に戻して `resignFirstResponder()`。`onValueChanged` は呼ばない (MUST NOT)
- フルスクリーンモーダル (`UINavigationController` ベースの present) で表示してはならない (MUST NOT)

Android の `NumberPickerCell` は `android.widget.NumberPicker` を内包する `AlertDialog` を表示しなければならない (MUST)。

両プラットフォームとも `min`〜`max` の範囲を `step` 刻みで選択可能でなければならない (MUST)。`step <= 0` のときは 1 にフォールバックしなければならない (MUST)。`min > max` の場合は警告ログを残し、候補値リストを単要素 `[min]` で構築するか、ピッカー表示を抑止する (MAY)。

iOS の `UIPickerView` の各候補行の表示文字列も `unit` を反映しなければならない (MUST)。`pickerView(_:titleForRow:forComponent:)` は `effectiveValueText()` と同じフォーマット規則で `"<values[row]> <unit>"` を返す。

#### Scenario: 数値ピッカー表示と既定値（iOS 埋め込み方式）

- **GIVEN** iOS で `NumberPickerCell(title: "音量", min: 0, max: 100, step: 5, value: $volume)`（`volume = 50`）
- **WHEN** Cell をタップする
- **THEN** `EmbeddedPickerHostField` が `becomeFirstResponder()` を呼び、iOS が `UIPickerView` をキーボード位置にスライドアップ表示する。Toolbar に `[Cancel] [Done]` が並ぶ。初期選択行が 50、step 5 ごとに 0〜100 が選択可能となる。フルスクリーンモーダルは提示されない

#### Scenario: 数値ピッカー表示と既定値（Android）

- **GIVEN** Android で `NumberPickerCell(title = "音量", min = 0, max = 100, step = 5, value = volume)`（`volume.value = 50`）
- **WHEN** Cell をタップする
- **THEN** `android.widget.NumberPicker` を含む `AlertDialog` が表示され、初期値が 50、step 5 ごとに 0〜100 が選択可能となる

#### Scenario: 値変更で binding が更新（iOS）

- **GIVEN** iOS で `NumberPickerCell(value: $volume)`（初期 50）の埋め込み Picker が表示されている
- **WHEN** ユーザーが 75 を選択して Done をタップする
- **THEN** Picker が閉じ、`volume` Binding が `75` に更新される。Cell の valueText 自動表示も "75" に更新される

#### Scenario: Cancel で元値に戻る（iOS 埋め込み方式）

- **GIVEN** iOS で `NumberPickerCell(value: $volume)`（初期 50）の埋め込み Picker が表示され、ユーザーが Picker で 75 までスクロールしている
- **WHEN** Cancel をタップする
- **THEN** Picker が閉じ、`volume` Binding は **50 のまま変化しない**（onValueChanged は呼ばれない）。次回 Picker を開くと初期選択行が 50 に戻っている

#### Scenario: 既定値（min = 0, max = 100, step = 1, unit = ""）

- **GIVEN** `NumberPickerCell(title: "明るさ", value: $brightness)`（範囲・step・unit 省略）
- **WHEN** インスタンスを構築する
- **THEN** `min == 0` / `max == 100` / `step == 1` / `unit == ""` が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: unit 既定値は空文字列

- **GIVEN** `NumberPickerCell(title: "個数", value: 3)`（unit 引数省略）
- **WHEN** `effectiveValueText()` を取得する
- **THEN** `"3"` が返る（suffix なし）。Picker UI の各候補行も `"0"` `"1"` `"2"` ... と数字のみ表示される

#### Scenario: unit による valueText 自動 suffix（iOS / Android 両方）

- **GIVEN** `NumberPickerCell(title: "サイズ", min: 10, max: 20, step: 1, value: 15, unit: "px")`（valueText 省略）
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot に `"15 px"` と表示される

#### Scenario: unit が Picker UI の各候補行にも反映される（iOS）

- **GIVEN** iOS で `NumberPickerCell(title: "サイズ", min: 10, max: 20, step: 1, value: 15, unit: "px")` の埋め込み Picker を開く
- **WHEN** `UIPickerView` の各行の表示を観察する
- **THEN** 各行は `"10 px"` / `"11 px"` / `"12 px"` ... `"20 px"` と表示される（数字のみではない）

#### Scenario: valueText 明示指定が unit suffix より優先

- **GIVEN** `NumberPickerCell(title: "サイズ", valueText: "デフォルト", value: 15, unit: "px")`
- **WHEN** Cell が表示される
- **THEN** Cell 右側の valueText slot は `"デフォルト"`（valueText 明示指定）が表示される（`"15 px"` 自動生成は採用されない）

### Requirement: TimePickerCell

`TimePickerCell` は時刻選択用セルでなければならない (SHALL)。以下のフィールドを持たなければならない (MUST)：

- `title: String`
- `description: String?` / `valueText: String?` / `icon: KsImage?` / `hintText: String?` / `isEnabled: Bool` / `isVisible: Bool`（共通規約）
- iOS: `time: Binding<Date>`（`Foundation.Date`、時刻成分 hour / minute のみ参照）または `time: Date` + `onValueChanged: ((Date) -> Void)?`
- Android: `time: MutableState<LocalTime>`（`java.time.LocalTime`）または `time: LocalTime` + `onValueChanged: ((LocalTime) -> Unit)?`
- `format: String`（既定 `"HH:mm"`、iOS は `DateFormatter.dateFormat` / Android は `DateTimeFormatter.ofPattern(format)` 互換の format 文字列）
- `pickerTitle: String?`（既定 `nil`）
- `accentColor: UIColor? / Color?`（既定 `nil`）

iOS では `Date` の `hour` / `minute` 成分のみを参照し、`year` / `month` / `day` 成分は無視しなければならない (MUST)。内部実装は `Calendar.current.dateComponents([.hour, .minute], from: date)` で成分を取り出す。

`valueText` 引数が `nil` の場合、現在の `time` を `format` に従って文字列化して valueText slot に自動表示しなければならない (MUST)。

iOS の `TimePickerCell` は **AiForms 互換の埋め込み Picker 方式** で実装しなければならない (MUST)：

- 透明な no-caret な `UITextField` サブクラス (`EmbeddedPickerHostField`) を Cell の `ContentView` の subview に貼り、frame を `ContentView.bounds` に追従させる
- `embeddedField.inputView = UIDatePicker(.time, preferredDatePickerStyle: .wheels)` をセット、`embeddedField.inputAccessoryView` に `[Cancel] [pickerTitle?] [Done]` の `UIToolbar` をセットする
- Cell タップ → `tapHandler` 経由で `embeddedField.becomeFirstResponder()` が呼ばれ、iOS が `UIDatePicker` をキーボード位置にスライドアップ表示する
- Done タップ → hour / minute のみ取り出して `onValueChanged` callback に渡し、`embeddedField.resignFirstResponder()` で picker を閉じる
- Cancel タップ → `datePicker.date = preSelectedDate` で提示開始時点の Date に戻して `resignFirstResponder()`。`onValueChanged` は呼ばない (MUST NOT)
- フルスクリーンモーダル (`UINavigationController` ベースの present) で表示してはならない (MUST NOT)

Android の `TimePickerCell` はタップで `MaterialTimePicker` を表示しなければならない (MUST)。

#### Scenario: 時刻表示と変更（iOS 埋め込み方式）

- **GIVEN** iOS で `@State var alarm = Calendar.current.date(bySettingHour: 7, minute: 30, second: 0, of: Date())!` と `TimePickerCell(title: "アラーム", time: $alarm)`
- **WHEN** Cell をタップして埋め込み `UIDatePicker(.time)` を開き 8:00 に変更し Done を押す
- **THEN** Cell の valueText 表示が "08:00" に更新され、`alarm` Binding の `Date` 値の `hour` / `minute` 成分が `8` / `0` に更新される。フルスクリーンモーダルは提示されない

#### Scenario: 時刻表示と変更（Android）

- **GIVEN** Android で `val alarm = remember { mutableStateOf(LocalTime.of(7, 30)) }` と `TimePickerCell(title = "アラーム", time = alarm)`
- **WHEN** タップで `MaterialTimePicker` を開き 8:00 に変更し OK を押す
- **THEN** Cell の valueText 表示が "08:00" に更新され、`alarm.value` が `LocalTime.of(8, 0)` に更新される

#### Scenario: format の反映

- **GIVEN** iOS で `TimePickerCell(title: "予定", time: .constant(/* 14:05 */), format: "h:mm a")`、または Android で `TimePickerCell(title = "予定", time = remember { mutableStateOf(LocalTime.of(14, 5)) }, format = "h:mm a")`
- **WHEN** Cell が表示される
- **THEN** valueText slot に "2:05 PM" 相当（ロケール依存）が表示される

### Requirement: DatePickerCell

`DatePickerCell` は日付選択用セルでなければならない (SHALL)。以下のフィールドを持たなければならない (MUST)：

- `title: String`
- `description: String?` / `valueText: String?` / `icon: KsImage?` / `hintText: String?` / `isEnabled: Bool` / `isVisible: Bool`（共通規約）
- iOS: `date: Binding<Date>`（`Foundation.Date`、日付成分 year / month / day のみ参照）または `date: Date` + `onValueChanged: ((Date) -> Void)?`
- Android: `date: MutableState<LocalDate>`（`java.time.LocalDate`）または `date: LocalDate` + `onValueChanged: ((LocalDate) -> Unit)?`
- `format: String`（既定 `"yyyy/MM/dd"`、iOS は `DateFormatter.dateFormat` / Android は `DateTimeFormatter.ofPattern(format)` 互換の format 文字列）
- iOS: `minDate: Date?` / `maxDate: Date?`（既定 `nil`）
- Android: `minDate: LocalDate?` / `maxDate: LocalDate?`（既定 `nil`）
- `pickerTitle: String?`（既定 `nil`）
- `uiStyle: DatePickerUIStyle`（既定値はプラットフォーム固有。iOS: `.wheels`、Android: `.Material`。詳細は "DatePickerUIStyle 列挙型" Requirement 参照）
- iOS: `todayText: String?`（既定 `nil`。非 nil / 非空のとき Picker UI に「Today」相当ボタンが追加され、タップで Picker の日付を today に動かす。旧 AiForms `DatePickerCell.TodayText` 互換）
- iOS: `accentColor: UIColor?`（既定 `nil`）
- Android: `androidButtonColor: Color?`（既定 `null`、旧 AiForms `AndroidButtonColor` 相当、OK / CANCEL ボタンの色）
- Android: `accentColor: Color?`（既定 `null`、選択日付の強調色）

iOS では `Date` の `year` / `month` / `day` 成分のみを参照し、`hour` / `minute` / `second` 成分は無視しなければならない (MUST)。内部実装は `Calendar.current.dateComponents([.year, .month, .day], from: date)` で成分を取り出す。

`valueText` 引数が `nil` の場合、現在の `date` を `format` に従って文字列化して valueText slot に自動表示しなければならない (MUST)。

iOS の `DatePickerCell` は `uiStyle` の値に従って UI を切替えなければならない (MUST)：

- `uiStyle = DatePickerUIStyle.wheels` のとき **AiForms 互換の埋め込み Picker 方式**：
  - 透明な no-caret な `UITextField` サブクラス (`EmbeddedPickerHostField`) を Cell の `ContentView` の subview に貼り、frame を `ContentView.bounds` に追従させる
  - `embeddedField.inputView = UIDatePicker(.date, preferredDatePickerStyle: .wheels)` をセット、`embeddedField.inputAccessoryView` に Toolbar をセットする
  - `todayText` が nil / 空のとき Toolbar は `[Cancel] [pickerTitle?] [Done]` 構成、非 nil かつ非空のとき `[Cancel] [pickerTitle?] [todayText] [Done]` 構成（`todayText` ボタンと `[Done]` の間に 20pt の固定スペースを置く、AiForms オリジナル準拠）
  - Cell タップ → `tapHandler` 経由で `embeddedField.becomeFirstResponder()` が呼ばれ、iOS が `UIDatePicker` をキーボード位置にスライドアップ表示する
  - Done タップ → year / month / day のみ取り出して `onValueChanged` callback に渡し、`embeddedField.resignFirstResponder()` で picker を閉じる
  - Cancel タップ → `datePicker.date = preSelectedDate` で提示開始時点の Date に戻して `resignFirstResponder()`。`onValueChanged` は呼ばない (MUST NOT)
  - `todayText` ボタンタップ → 範囲チェックを通れば `wheelsPicker.setDate(today, animated: true)` で wheel を今日に動かす。`wheelsPicker.date` が既に同日（年月日一致）の場合は UIKit が「変化なし」と判定して再描画されないため、`Calendar.date(byAdding: .second, value: 1, to: todayStart)` で 1 秒だけずらしたダミー値を `animated: false` で挟んだ後 today に setDate する（強制再描画）。範囲チェックは **日単位** で行う：`Calendar.startOfDay(for: today) < Calendar.startOfDay(for: minDate)` または `Calendar.startOfDay(for: today) > Calendar.startOfDay(for: maxDate)` のときのみ no-op とする (MUST)。時刻成分まで含めた `Date` 同士の `<` / `>` 比較を行ってはならない (MUST NOT)。これは `maxDate: Date()` 指定時に「ボタン押下時の Date()」が「render() で固定した maxDate」より僅かに後の時刻になり、毎回 abort される罠を避けるため。範囲外の場合は何もしない（AiForms オリジナル `DatePickerCellView.cs.SetToday()` 準拠）
  - フルスクリーンモーダル (`UINavigationController` ベースの present) で表示してはならない (MUST NOT)
- `uiStyle = DatePickerUIStyle.calendar` のとき **`.pageSheet` + `.custom` detent シート方式**：
  - `DatePickerCalendarSheetController`（`UIViewController` 派生）を `KeyWindowResolver.topPresentedViewController()` 経由で `present(..., animated: true)` する
  - sheet 設定：`modalPresentationStyle = .pageSheet`、`sheetPresentationController.detents = [.custom { _ in 480 }]`（iOS 16+。iOS 15 では `.medium()` に fall back）、`prefersGrabberVisible = true`、`preferredCornerRadius = 16`
  - sheet 内：`UIDatePicker(.date, preferredDatePickerStyle: .inline)`（カレンダー grid）と下部ボタンバー `[Cancel] [todayText?] [Done]` を縦スタックで配置
  - 多重提示防止：既に sheet 提示中の場合は新たな sheet を提示しない (MUST NOT)
  - Done タップ → year / month / day のみ取り出して `onValueChanged` callback に渡し、sheet を dismiss
  - Cancel タップ → sheet を dismiss、`onValueChanged` は呼ばない (MUST NOT)
  - `todayText` ボタンタップ → 範囲チェックを通れば `.inline` カレンダーの表示月および選択日を today にジャンプさせる（**選択状態にかかわらず today へ移動**）。`datePicker.date` が既に同日の場合は Wheels モードと同じく 1 秒ずらしたダミー値で強制再描画する。範囲チェックも Wheels モードと同様に **日単位** (`Calendar.startOfDay(for:)` 同士の比較) で行わなければならない (MUST)

Android の `DatePickerCell` は `uiStyle` の値に従って UI を切替えなければならない (MUST)：

- `uiStyle = DatePickerUIStyle.Material` のとき `com.google.android.material.datepicker.MaterialDatePicker`
- `uiStyle = DatePickerUIStyle.Spinner` のとき `android.widget.DatePicker`（`android:datePickerMode="spinner"`）を内包する `AlertDialog`

`minDate` / `maxDate` が指定されている場合は当該範囲外の日付を選択不可としなければならない (MUST)。

- iOS（両モード共通）：`UIDatePicker.minimumDate` / `maximumDate` に `Date?` を直接代入
- Android Material：`MaterialDatePicker.Builder.setCalendarConstraints(...)` の `DateValidator` で範囲制限
- Android Spinner：`android.widget.DatePicker.minDate` / `maxDate`（`Long` epoch milliseconds）に `LocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()` 変換値を設定

#### Scenario: 日付表示と変更（iOS Wheels モード）

- **GIVEN** iOS で `@State var birthday: Date = Calendar.current.date(from: DateComponents(year: 2000, month: 1, day: 1))!` と `DatePickerCell(title: "誕生日", date: $birthday)`（`uiStyle` 省略 = `.wheels`）
- **WHEN** Cell をタップして埋め込み `UIDatePicker(.date) + .wheels` を開き 2000/12/31 に変更し Done を押す
- **THEN** Picker が閉じ、Cell の valueText 表示が "2000/12/31" に更新され、`birthday` Binding の `Date` 値の `year` / `month` / `day` 成分が `2000` / `12` / `31` に更新される。フルスクリーンモーダルは提示されない

#### Scenario: 日付表示と変更（iOS Calendar モード）

- **GIVEN** iOS で `@State var reservation: Date = ...` と `DatePickerCell(title: "予約日", date: $reservation, uiStyle: .calendar)`
- **WHEN** Cell をタップする
- **THEN** `DatePickerCalendarSheetController` が `.pageSheet` + `.custom` detent (~480pt) で提示され、`UIDatePicker(.date) + .inline` のカレンダー grid と下部ボタンバー `[Cancel] [Done]` が表示される。シート上部に grabber が表示される

#### Scenario: 日付表示と変更（Android）

- **GIVEN** Android で `val birthday = remember { mutableStateOf(LocalDate.of(2000, 1, 1)) }` と `DatePickerCell(title = "誕生日", date = birthday)`（`uiStyle` 省略 = `Material`）
- **WHEN** タップで `MaterialDatePicker` を開き 2000/12/31 に変更し OK を押す
- **THEN** Cell の valueText 表示が "2000/12/31" に更新され、`birthday.value` が `LocalDate.of(2000, 12, 31)` に更新される

#### Scenario: 範囲制限

- **GIVEN** iOS で `DatePickerCell(title: "予約日", date: $reservation, minDate: 2026/6/1 相当 Date, maxDate: 2026/12/31 相当 Date)`、または Android で `DatePickerCell(title = "予約日", date = reservation, minDate = LocalDate.of(2026, 6, 1), maxDate = LocalDate.of(2026, 12, 31))`
- **WHEN** ピッカーを開く
- **THEN** 2026/6/1 より前および 2026/12/31 より後の日付は選択不可となる（iOS は `UIDatePicker.minimumDate` / `maximumDate`、Android Material は `CalendarConstraints`、Android Spinner は `DatePicker.minDate` / `maxDate` で実現）

#### Scenario: iOS Wheels モード Today ボタンで本日にセット

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday, todayText: "今日")`（`birthday = 1990/01/01`、`uiStyle` 省略 = `.wheels`）
- **WHEN** Cell をタップして埋め込み Picker を開き、Toolbar の「今日」ボタンを押す
- **THEN** `wheelsPicker.date` の wheel UI 表示が今日の年月日に動く（`birthday` Binding は Done を押すまで変化しない）

#### Scenario: iOS Wheels モード Today ボタンは既に同日でも再描画される

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday, todayText: "今日")` の Picker を開いた状態で、`wheelsPicker.date` が既に today と同日（年月日一致）になっている
- **WHEN** 「今日」ボタンを押す
- **THEN** 同日 setDate は `Calendar.date(byAdding: .second, value: 1, to: todayStart)` のダミー値を `animated: false` で挟む実装により、wheel が確実に再描画される（UIKit の「変化なし」最適化で何も起こらない、という挙動になってはならない）

#### Scenario: iOS Wheels モード Today ボタンは todayText nil なら非表示

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday)`（`todayText` 省略）
- **WHEN** Cell をタップして埋め込み Picker を開き、Toolbar を観察する
- **THEN** Toolbar の items は `[Cancel] [pickerTitle?] [Done]` 構成で、「今日」相当のボタンは存在しない

#### Scenario: iOS Wheels モード Cancel で元値に戻る

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday)`（`birthday = 1990/01/01`）の埋め込み Picker が表示されており、ユーザーが Picker を 2026/06/14 までスクロール
- **WHEN** Cancel をタップする
- **THEN** Picker が閉じ、`birthday` Binding は **1990/01/01 のまま変化しない**（onValueChanged は呼ばれない）。次回 Picker を開くと Picker の date が 1990/01/01 に戻っている

#### Scenario: iOS Calendar モード Today ボタンで本日へジャンプ

- **GIVEN** iOS で `DatePickerCell(title: "予約日", date: $reservation, uiStyle: .calendar, todayText: "今日")`（`reservation = 1990/01/01`）の sheet が表示されている
- **WHEN** sheet 下部の「今日」ボタンを押す
- **THEN** `.inline` カレンダーの表示月および選択日が today にジャンプする（**選択状態にかかわらず today のページへ移動**）。`reservation` Binding は Done を押すまで変化しない

#### Scenario: iOS Calendar モード Today ボタンは既に today を選択中でも再描画される

- **GIVEN** iOS で `DatePickerCell(title: "予約日", date: $reservation, uiStyle: .calendar, todayText: "今日")` の sheet が表示されており、ユーザーが既に today を選択している状態（または別月を表示中だが選択日は today のまま）
- **WHEN** 「今日」ボタンを押す
- **THEN** 同日 setDate は `Calendar.date(byAdding: .second, value: 1, to: todayStart)` のダミー値を挟む実装により、表示月が today の月に確実に再描画される（UIKit の「変化なし」最適化で何も起こらない、という挙動になってはならない）

#### Scenario: iOS DatePicker Today ボタンの範囲チェックは日単位で行う

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday, maxDate: Date(), todayText: "今日")`（`maxDate` を Cell 構築時の `Date()` で渡す）
- **WHEN** ユーザーが Cell をタップし「今日」ボタンを押す
- **THEN** ボタン押下時の `Date()` が render() で渡された `maxDate` より僅かに後の時刻であっても、`Calendar.startOfDay(for: today) <= Calendar.startOfDay(for: maxDate)` のため範囲内と判定され、wheel または `.inline` カレンダーが today へ正しくジャンプする（時刻成分まで含めた `Date` 同士の `>` 比較で abort されてはならない）

#### Scenario: iOS Calendar モード Done で閉じた後も再タップで開ける

- **GIVEN** iOS で `DatePickerCell(title: "予約日", date: $reservation, uiStyle: .calendar)` の sheet を開き、Done もしくは Cancel で閉じた直後
- **WHEN** 同じ Cell を再度タップする
- **THEN** sheet が再表示される（前回提示した `DatePickerCalendarSheetController` の参照が weak で残り続けて多重提示ガードに弾かれる、という挙動になってはならない。実装は strong 参照 + dismiss completion / `UIAdaptivePresentationControllerDelegate.presentationControllerDidDismiss` で明示 nil 化することで保証する）

#### Scenario: Android Spinner スタイル

- **GIVEN** Android で `DatePickerCell(title = "誕生日", date = birthday, uiStyle = DatePickerUIStyle.Spinner)`
- **WHEN** タップで日付ピッカーを開く
- **THEN** `android.widget.DatePicker`（`android:datePickerMode="spinner"`）を内包する `AlertDialog` が表示され、年・月・日の wheel スピナーで日付を選択できる（`MaterialDatePicker` は使用されない）

#### Scenario: uiStyle 省略時のプラットフォーム別既定値

- **GIVEN** iOS で `DatePickerCell(title: "誕生日", date: $birthday)`、Android で `DatePickerCell(title = "誕生日", date = birthday)`（共に `uiStyle` 省略）
- **WHEN** インスタンスを構築する
- **THEN** iOS では `uiStyle == DatePickerUIStyle.wheels`、Android では `uiStyle == DatePickerUIStyle.Material` が適用される。ビルドエラーや実行時エラーは発生しない

### Requirement: DatePickerUIStyle 列挙型

iOS UI 層（`KsSettingsViewUI`）と Android UI 層（`ks-settingsview-ui`）は、それぞれ `DatePickerCell` の UI スタイルを表す `DatePickerUIStyle` 列挙型を提供しなければならない (SHALL)。本列挙型はクロスプラットフォーム命名規約として **同じ型名・同じプロパティ名 (`uiStyle`)** を採用するが、enum のケースは各プラットフォーム固有 UI を反映する：

- iOS：`public enum DatePickerUIStyle: Hashable, Sendable { case wheels, calendar }`
  - `.wheels`（既定）：AiForms 互換の埋め込みホイール (`UIDatePicker(.date) + .wheels`)。`EmbeddedPickerHostField.inputView` 経由でキーボード位置にスライドアップ表示
  - `.calendar`：iOS カレンダーアプリ風 (`UIDatePicker(.date) + .inline`)。`.pageSheet` + `.custom` detent (480pt) シートで表示
- Android：`enum class DatePickerUIStyle { Material, Spinner }`
  - `Material`（既定）：`com.google.android.material.datepicker.MaterialDatePicker`
  - `Spinner`：`android.widget.DatePicker` の Spinner モード（旧 AiForms `IsAndroidSpinnerStyle = true` 相当）

iOS と Android で型名・プロパティ名は対称だが、enum ケースの集合は重ならない (MUST NOT) — iOS には `Material` / `Spinner` ケースは存在せず、Android には `wheels` / `calendar` ケースは存在しない。これは各プラットフォームの UI 慣習に合致したケース名を選ぶ設計判断であり、design.md Decision 12 で記述されている。

#### Scenario: iOS 定義

- **GIVEN** Swift `KsSettingsViewUI` モジュール
- **WHEN** `DatePickerUIStyle` を参照する
- **THEN** `public enum DatePickerUIStyle: Hashable, Sendable { case wheels, case calendar }` として定義されており、`DatePickerUIStyle.wheels` / `.calendar` でケース参照できる

#### Scenario: Android 定義

- **GIVEN** Kotlin `ks-settingsview-ui` モジュール
- **WHEN** `DatePickerUIStyle` を参照する
- **THEN** `enum class DatePickerUIStyle { Material, Spinner }` として定義されており、`DatePickerUIStyle.Material` / `.Spinner` でケース参照できる

#### Scenario: iOS には Material / Spinner ケースが存在しない

- **GIVEN** Swift `KsSettingsViewUI` モジュール
- **WHEN** `DatePickerUIStyle.material` または `.spinner` を参照しようとする
- **THEN** ケース解決できずコンパイルエラーとなる（iOS の `DatePickerUIStyle` は `.wheels` / `.calendar` のみ）

#### Scenario: Android には wheels / calendar ケースが存在しない

- **GIVEN** Kotlin `ks-settingsview-ui` モジュール
- **WHEN** `DatePickerUIStyle.Wheels` または `.Calendar` を参照しようとする
- **THEN** ケース解決できずコンパイルエラーとなる（Android の `DatePickerUIStyle` は `Material` / `Spinner` のみ）

### Requirement: 入力 Cell の登録 API

各プラットフォームは、入力系 Cell 群を `KsCellRegistry` にまとめて登録する `registerInputCells()`（iOS）/ `registerInputCells(context)`（Android）を提供しなければならない (SHALL)。

- iOS: `extension KsCellRegistry { public func registerInputCells() }` を `KsCellRegistryInputCells.swift` に実装する
- Android: `fun KsCellRegistry.registerInputCells(context: Context)` を `KsCellRegistryInputCells.kt` に実装する

`KsSettingsViewController.init`（iOS）/ `KsSettingsView.init`（Android）でオプトアウト可能な auto-register として呼び出してもよい（実装詳細）。

#### Scenario: iOS 一括登録

- **GIVEN** `KsCellRegistry.shared`
- **WHEN** `KsCellRegistry.shared.registerInputCells()` を呼ぶ
- **THEN** EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell の 5 種が registry に登録される

#### Scenario: Android 一括登録

- **GIVEN** `KsCellRegistry`
- **WHEN** `KsCellRegistry.registerInputCells(context)` を呼ぶ
- **THEN** 5 種すべての ViewHolder ファクトリと viewType が登録される

### Requirement: ユニットテスト

各入力系 Cell に対して、bind / 共通フィールド表示 / TwoWay 入力通知 / Theme 適用 / 共通行レイアウト関数経由の描画 / VisibilityAware 経由フィルタ / isEnabled 適用 / 再利用後のリセットを検証するユニットテストが存在しなければならない (SHALL)。

#### Scenario: EntryCell の入力通知テスト

- **GIVEN** `EntryCell(title: "メモ", text: $text)` を bind した ViewHolder（`text` の初期値 `""`）
- **WHEN** UITextField / EditText に "abc" を入力するイベントをシミュレート
- **THEN** `text` Binding / `text.value` が最終的に `"abc"` に更新されることをテストアサーションで確認する

#### Scenario: 共通フィールド表示テスト

- **GIVEN** iOS で `TimePickerCell(title: "アラーム", description: "毎朝", icon: KsImage.systemName("alarm"), hintText: "新規", time: .constant(Calendar.current.date(bySettingHour: 7, minute: 0, second: 0, of: Date())!))`、または Android で `TimePickerCell(title = "アラーム", description = "毎朝", icon = KsImage.SystemName("alarm"), hintText = "新規", time = remember { mutableStateOf(LocalTime.of(7, 0)) })` を bind した ViewHolder
- **WHEN** ViewHolder の View 階層を検査する
- **THEN** `title` slot に "アラーム"、`description` slot に "毎朝"、`icon` slot に bell アイコン、`hintText` slot に "新規"、`valueText` slot に "07:00" が表示される

#### Scenario: isVisible filter テスト

- **GIVEN** 同一 Section に `EntryCell(isVisible: true)` と `EntryCell(isVisible: false)` を 1 つずつ含む構成
- **WHEN** UI 層が visible projection を算出する
- **THEN** `VisibilityAware` 経由のフィルタにより、`isVisible: false` の Cell が projection から除外される

#### Scenario: DSL 経由 Cell 配置テスト（Android）

- **GIVEN** Compose の DSL `Section("入力") { EntryCell(title = "名前", text = state) }` を評価
- **WHEN** 内部 DSL ツリー（`DSLCellNode` 配列）を検査する
- **THEN** ツリーには `EntryCell` インスタンス 1 件が格納され、戻り値 `CellHandle` への `.cellHeight(80.dp)` chain が動作する
