## Why

設定画面で頻繁に登場する入力系 Cell（テキスト入力・候補選択・数値選択・時刻選択・日付選択）を、`cell-types-basic` で確立された **基本 Cell 7 種の共通規約**（`description` / `valueText` / `icon` / `hintText` / `isEnabled` / `isVisible` 共通フィールド・`VisibilityAware` opt-in・共通行レイアウト関数・`titleColor` / `titleFont` の 3 段階解決（`CellStyle → Theme → 既定`）・`accentColor` の 4 段階解決（`Cell → CellStyle → Theme → プラットフォーム既定`）・TwoWay binding の DSL 拡張規約）にきっちり乗せた形で実装する。本変更提案完了で旧 `AiForms.Maui.SettingsView` の主要 Cell 機能は `CustomCell` を除いて Native iOS / Android 上で揃う（旧 `TextPickerCell` のみ、使用頻度が低く `PickerCell` で代替可能なため移植対象外とする）。

本提案は当初 2026-06-07 に作成されたが、その後 `add-declarative-dsl` / `purify-core-extract-style-to-ui-layer` / `add-cell-types-basic` / `add-visibility-flags-section-and-cell` などが順次 archive され、依存先の capability 名・規約が大きく更新されたため、最新 spec 群（`cell-types-basic` / `settings-view-core` / `settings-view-{ios,android}-{host,style,swiftui,compose,theme-bridge}`）に整合する形で再編する。同時に「`KsColor` の前例（独自値型でラップして使い勝手を損ね、最終的に Native 型直接保持に切替えた）と同じ轍を踏まない」方針を徹底し、日時型・キーボード種別ともに **Native 型を UI 層 API で直接公開する**（独自値型・独自列挙型は導入しない）。

## What Changes

- iOS `KsSettingsViewUI` に以下の入力系 Cell（Swift `struct`、`KsCell` 準拠）と対応する `UICollectionViewCell` サブクラスを追加：
  - `EntryCell`：右側 accessory に `UITextField` を内蔵。`text` を `Binding<String>` で受ける TwoWay。`placeholder: String?`、`keyboardType: UIKeyboardType`（**Native 型を直接公開**、既定 `.default`）、`isPassword: Bool`、`textAlignment: CellTitleAlignment`、`accentColor: UIColor?`（caret 色）、`maxLength: Int?`（既定 `nil` = 無制限、旧 AiForms `MaxLength` 互換）
  - `PickerCell`：タップでモーダル `UITableViewController` を開き、文字列リストから選ぶ。`items: [String]`、`selectionMode: PickerSelectionMode`（`.single` / `.multiple`、既定 `.single`）、`.single` の TwoWay は `selectedIndex: Binding<Int?>`、`.multiple` の TwoWay は `selectedIndices: Binding<Set<Int>>`、`maxSelectedNumber: Int = 0`（`0` は無制限、`.multiple` のみ有効）、`displayFormatter: ((String) -> String)?`、`accentColor: UIColor?`、`pageTitle: String?`
  - `NumberPickerCell`：タップで **AiForms 互換の埋め込み `UIPickerView`** を `inputView` 経由でキーボード位置にスライドアップ表示（モーダルではない）。`min: Int = 0`、`max: Int = 100`、`step: Int = 1`、`value: Binding<Int>`、`unit: String = ""`（任意の単位文字列、空文字以外なら valueText / Picker 各行に "<value> <unit>" 形式の suffix が付く。旧 AiForms `NumberPickerCell.Unit` 互換）、`pickerTitle: String?`、`accentColor: UIColor?`
  - `TimePickerCell`：タップで **AiForms 互換の埋め込み `UIDatePicker(.time)`** を `inputView` 経由でキーボード位置にスライドアップ表示（モーダルではない）。`time: Binding<Date>`（**`Foundation.Date` を直接公開**、`Calendar.current` ベースの hour/minute 解釈は UI 層内部で行う）、`format: String = "HH:mm"`、`pickerTitle: String?`、`accentColor: UIColor?`
  - `DatePickerCell`：`uiStyle: DatePickerUIStyle`（`.wheels` / `.calendar`、既定 `.wheels`）で UI を切替。`.wheels` のときタップで **AiForms 互換の埋め込み `UIDatePicker(.date) + .wheels`** を `inputView` 経由でキーボード位置にスライドアップ表示。`.calendar` のときタップで **`.pageSheet` + `.custom` detent (≒480pt) の小さなシート**に `UIDatePicker(.date) + .inline` カレンダー grid を表示（iOS カレンダーアプリ風）。`date: Binding<Date>`（**`Foundation.Date` を直接公開**）、`format: String = "yyyy/MM/dd"`、`minDate: Date?`、`maxDate: Date?`、`pickerTitle: String?`、`accentColor: UIColor?`、`todayText: String?`（既定 `nil`。non-nil/非空のとき Wheels Toolbar / Calendar ボタンバーに「Today」相当ボタンが表示され、タップで Picker の表示日を today に動かす。旧 AiForms `DatePickerCell.TodayText` 互換）
- Android `ks-settingsview-ui` に対応する Kotlin `data class`（`Cell` 実装）と `CellViewHolder` 実装を追加：
  - `EntryCell`：右側 accessory に `EditText`。`text: MutableState<String>` で TwoWay。`placeholder: String?`、`keyboardType: Int`（**Native `android.text.InputType` の Int 定数を直接公開**、既定 `InputType.TYPE_CLASS_TEXT`）、`isPassword: Boolean`、`textAlignment: CellTitleAlignment`、`accentColor: Color?`、`maxLength: Int?`（既定 `null` = 無制限）
  - `PickerCell`：タップで `AlertDialog`。`selectionMode = PickerSelectionMode.Single` のとき `selectedIndex: MutableState<Int?>` + `AlertDialog.setSingleChoiceItems`、`selectionMode = PickerSelectionMode.Multiple` のとき `selectedIndices: MutableState<Set<Int>>` + `AlertDialog.setMultiChoiceItems`。`maxSelectedNumber: Int = 0`
  - `NumberPickerCell`：タップで `android.widget.NumberPicker` 内蔵 `AlertDialog`。`value: MutableState<Int>`、`unit: String = ""`（任意の単位文字列、iOS と同じく "<value> <unit>" 形式の suffix）
  - `TimePickerCell`：タップで `MaterialTimePicker`。`time: MutableState<LocalTime>`（**`java.time.LocalTime` を直接公開**）
  - `DatePickerCell`：`uiStyle: DatePickerUIStyle`（`Material` / `Spinner`、既定 `Material`）で UI を切替。`Material` のときタップで `MaterialDatePicker`、`Spinner` のときタップで `android.widget.DatePicker` の Spinner モード（`android:datePickerMode="spinner"`）を内包する `AlertDialog`。`date: MutableState<LocalDate>`（**`java.time.LocalDate` を直接公開**）、`minDate: LocalDate?`、`maxDate: LocalDate?`、`androidButtonColor: Color?`（旧 AiForms 互換）
  - **クロスプラットフォーム命名規約**：`DatePickerCell.uiStyle: DatePickerUIStyle` は iOS / Android 両方で同じプロパティ名・同じ型名を採用するが、enum のケースはプラットフォーム固有 UI を反映する（iOS: `.wheels` / `.calendar`、Android: `.Material` / `.Spinner`）。これにより利用者は両プラットフォームで対称な API として扱える
- すべての入力系 Cell（5 種）に以下の **`cell-types-basic` 共通規約** を適用：
  - 共通 Optional フィールド `description: String?` / `valueText: String?` / `icon: KsImage?` / `hintText: String?` を持つ（ただし `EntryCell` は右側 accessory が `UITextField` のため `valueText` は **持たない**。ピッカー系 4 種は `valueText` を「現在の選択値を文字列化したもの」として既定表示する）
  - `isEnabled: Bool`（既定 `true`）：`false` でテキスト色を `Theme.disabledTextColor` に置換し、コントロール要素を disabled 化
  - `isVisible: Bool`（既定 `true`）：`VisibilityAware` プロトコル / interface に opt-in 準拠
  - 共通行レイアウト関数 `applyCellBaseLayout(...)`（iOS）/ `applyCellBaseLayout(views, ...)`（Android）経由でレイアウト構成（accessory slot に各 Cell 固有のコントロールを組み込む）
  - Theme.cellTitleColor / Theme.cellTitleFont の 3 段階解決（`CellStyle → Theme → 既定`）に従う
- 入力系 Cell の **TwoWay binding 規約**（本提案で新規規定）：
  - iOS DSL：`@Binding<T>` を引数に取る（例: `EntryCell(title: "...", text: $userName)`）
  - Android Compose DSL：`MutableState<T>` を引数に取る（例: `EntryCell(title = "...", text = userNameState)`）
  - 利用者が外部 callback パターンを必要とする場合は、`Cell` 値型を直接構築する経路で各 Cell 固有名の callback（`onTextChanged` / `onSelectionChanged` / `onMultiSelectionChanged` / `onValueChanged`）も併設する
- **Native 型直接公開の方針**（`KsColor` / `KsFont` の前例と同じ轍を踏まない）：
  - 日時：iOS は `Foundation.Date`、Android は `java.time.LocalTime` / `java.time.LocalDate` を UI 層 API でそのまま公開（独自値型 `KsTime` / `KsDate` は導入しない）
  - キーボード種別：iOS は `UIKit.UIKeyboardType`、Android は `android.text.InputType`（`Int` 定数）を UI 層 API でそのまま公開（独自列挙型 `KsKeyboardType` は導入しない）
  - Core 層には本提案で追加する型はない
- UI 層配置の補助型として `PickerSelectionMode`（`single` / `multiple`、iOS は Swift `enum`、Android は Kotlin `enum class`、Cell の動作モードを表す論理列挙）と `DatePickerUIStyle`（クロスプラットフォーム命名規約：iOS は `wheels` / `calendar`、Android は `Material` / `Spinner`、各プラットフォーム UI 層にそれぞれ別 enum として実装）を追加。これらは UI 層の論理スイッチを表す独自列挙型のためプラットフォーム固有型では代替できない
- `KsCellRegistry.registerInputCells()`（iOS）/ `KsCellRegistry.registerInputCells(context)`（Android）を提供
- 各入力系 Cell に対応する `DSLSectionScope` 拡張関数を `ks-settingsview-compose` モジュールに追加（iOS は `@resultBuilder SectionBuilder` で Cell 値を直置きできるため別途規約不要）
- 各 Cell のユニットテスト：bind、共通フィールドの表示・隠蔽、TwoWay 入力イベント通知、Theme 適用、共通行レイアウト関数経由の描画、`VisibilityAware` 経由の visibility フィルタ、`isEnabled` 適用、再利用後のリセット、DSL 拡張関数経由での Cell 配置検証、`maxLength` 上限テスト
- iOS / Android Sample に 5 種の入力系 Cell を表示するページを追加（新 DSL 形式・TwoWay binding 形式で記述）

## Capabilities

### New Capabilities
- `cell-types-input`: 入力系 Cell 群（Entry / Picker / NumberPicker / TimePicker / DatePicker の 5 種）の振る舞い・共通フィールド適用・TwoWay binding 規約・登録 API・UI 層補助列挙型（`PickerSelectionMode` / `DatePickerUIStyle`）を規定する

### Modified Capabilities
（なし。Core / 他 capability への変更はない）

> 注: 本提案の入力系 Cell 5 種の DSL 拡張関数（`fun DSLSectionScope.EntryCell(...)` 等）は `cell-types-input` capability の Requirement「Compose DSL 拡張関数による入力 Cell 直置き」として規定する。`settings-view-android-compose` capability の Compose DSL Requirement で既に「具象 Cell 型ごとに `DSLSectionScope` の拡張関数として直置き API を提供する規約」が確定済みのため、本提案はその規約に従って実装するのみで、`settings-view-android-compose` 自体の Requirement を Modify する必要はない。
>
> 同様に「共通 Optional 6 フィールド / `isEnabled` / `isVisible` / 共通行レイアウト関数経由 / `VisibilityAware` opt-in / Theme 3 段階解決 / `accentColor` 4 段階解決」は `cell-types-basic` capability の Requirement に「後続変更提案で追加される新規 Cell 種別も同じ規約を満たす」と既に書かれているため、入力系 Cell も同規約に従って実装すれば足り、`cell-types-basic` 自体の Requirement を Modify する必要はない。本提案の `cell-types-input` spec では「同規約に opt-in する旨」を Requirement として宣言する形で受ける。
>
> Core 層 (`settings-view-core`) への追加もない（旧版で検討していた `KsKeyboardType` / `KsTime` / `KsDate` はすべて廃案、Native 型直接公開で代替）。

## Impact

- 影響範囲：iOS UI モジュール（`KsSettingsViewUI`）、Android UI モジュール（`ks-settingsview-ui`）、Android Compose モジュール（`ks-settingsview-compose` への DSL 拡張関数追加）、両 Sample。Core モジュールへの変更はなし
- 依存：
  - 先行 archive 済み：`add-monorepo-foundation` / `add-settings-view-core` / `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-declarative-dsl` / `add-partial-update-core` / `add-partial-update-native` / `add-cell-types-basic` / `port-theme-and-cellstyle-missing-fields` / `purify-core-extract-style-to-ui-layer` / `unify-cell-common-fields-via-shared-row-layout` / `refine-cell-layout-after-unify-review` / `add-visibility-flags-section-and-cell` / `add-samples-ios` / `add-samples-android`
  - 参照する最新 capability：`settings-view-core` / `cell-types-basic` / `settings-view-ios-host` / `settings-view-ios-style` / `settings-view-ios-swiftui` / `settings-view-ios-theme-bridge` / `settings-view-android-host` / `settings-view-android-style` / `settings-view-android-compose` / `settings-view-android-theme-bridge` / `samples-ios` / `samples-android`
- 並列性：archive 済みの依存先がすべて完了しているため、本提案は単独で進行可能
- 後続変更が依存：`add-cell-types-custom`（`CustomCell` 追加。並列可）、`add-maui-cells`（MAUI 側 5 種入力 Cell の `BindableObject` + `Handler` 実装、および `samples/maui/` への 5 種入力 Cell ページ追加。本提案完了後に着手。MAUI 側は C# `Keyboard` / `TimeSpan` / `DateTime` から `UIKeyboardType` / `Int` (InputType) / `Foundation.Date` / `java.time.LocalTime` / `LocalDate` への変換を Bridge 境界で実施する）
- 旧 AiForms 移行ユーザーへの注意：旧 `TextPickerCell` は本提案で **移植対象外**。利用者は `PickerCell(selectionMode: .single, ...)` で代替する（`selectedItem: String?` ベースの API ではなく `selectedIndex: Int?` ベースになる点が差分）
- リスク：中。各 Cell の入力 UX を旧 AiForms と互換に保つには Sample での目視検証が必要。TwoWay binding が iOS と Android で文法的に並列な書き味になっていることも Sample 段階で確認する。
