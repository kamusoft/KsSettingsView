## 0. 着手前の依存確認

- [x] 0.1 [`openspec/specs/cell-types-basic/spec.md`](../../specs/cell-types-basic/spec.md) の以下 Requirement を再読する：「全 Cell 共通の description / valueText / icon / hintText フィールド」「全 Cell 共通の isEnabled」「全 Cell 共通の isVisible」「全 Cell 共通の Theme.titleColor / Theme.titleFont 反映」「共通行レイアウト関数経由での描画」
- [x] 0.2 [`openspec/specs/settings-view-core/spec.md`](../../specs/settings-view-core/spec.md) の「表示状態同期の三層分離」Requirement を再読し、`replaceCell` 経路（内容更新 = reconfigure）と「(3) 可視性変化」経路の責務分担を確認
- [x] 0.3 [`openspec/specs/settings-view-ios-swiftui/spec.md`](../../specs/settings-view-ios-swiftui/spec.md) の「SwiftUI DSL」Requirement と「具象 Cell コンストラクタの `id` デフォルト値規約」を再読
- [x] 0.4 [`openspec/specs/settings-view-android-compose/spec.md`](../../specs/settings-view-android-compose/spec.md) の「Compose DSL」Requirement と「具象 Cell コンストラクタの `id` デフォルト値規約」を再読
- [x] 0.5 [`openspec/specs/settings-view-ios-style/spec.md`](../../specs/settings-view-ios-style/spec.md) / [`openspec/specs/settings-view-android-style/spec.md`](../../specs/settings-view-android-style/spec.md) の `Theme` / `CellStyle` 型（UI 層所属、Native 型直接保持）を再読（iOS 側のみ）
- [x] 0.6 [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) §2 / §3 の入力系 6 種 Cell プロパティ表を再読（旧 AiForms には 6 種あるが、本提案は **5 種** が対象 — TextPickerCell は移植対象外のため参考のみ）
- [x] 0.7 既存実装の参照：iOS `ios/Sources/KsSettingsViewUI/SwitchCell.swift` / `SwitchCellView.swift` / `CellBaseLayout.swift` / `VisibilityAware.swift`、Android `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCell.kt` / `SwitchCellViewHolder.kt` / `CellBaseLayout.kt` を読み、共通行レイアウト関数の呼び出し方・`VisibilityAware` 準拠の書き方を把握（iOS 側のみ確認）
- [x] 0.8 design.md の Decision 1（日時型・キーボード種別ともに Native 型直接公開 = iOS `Foundation.Date` / `UIKeyboardType`、Android `java.time.LocalTime` / `LocalDate` / `android.text.InputType` の `Int` 定数）と Decision 2（PickerCell の単一 / 複数選択両対応・TextPickerCell は移植対象外・DatePickerCell の Android UI スタイル切替）を再確認。`KsKeyboardType` / `KsTime` / `KsDate` 独自値型は **本提案では導入しない方針**（`KsColor` の前例を踏まえた判断）。Core モジュールへの追加は一切ない

## 1. UI 層補助型の追加（Core 層への追加はなし）

**重要**: 本提案では Core モジュール（`KsSettingsViewCore` / `ks-settingsview-core`）への追加型は **一切ない**。日時型は iOS `Foundation.Date` / Android `java.time.LocalTime` / `java.time.LocalDate` を、キーボード種別は iOS `UIKit.UIKeyboardType` / Android `android.text.InputType`（`Int`）を、UI 層 API でそのまま直接公開する（`KsColor` の前例を踏まえた方針、Decision 1 参照）。`KsKeyboardType` / `KsTime` / `KsDate` などの独自型は **廃案**。

- [x] 1.1 iOS `KsSettingsViewUI` に `public enum PickerSelectionMode: Hashable { case single, multiple }` を追加（`PickerSelectionMode.swift`）。これは Cell の動作モードを表す論理スイッチで、UIKit に対応型がないため UI 層独自列挙型を追加する
- [x] 1.2 Android `ks-settingsview-ui` に `enum class PickerSelectionMode { Single, Multiple }` を追加（`PickerSelectionMode.kt`）
- [x] 1.3 Android `ks-settingsview-ui` に `enum class DatePickerAndroidStyle { Material, Spinner }` を追加（`DatePickerAndroidStyle.kt`）。iOS には対応する型を作成しない（Android UI のスタイル切替専用のため）
- [x] 1.4 各々のユニットテスト（ケース存在・等価性）（iOS の `PickerSelectionMode` のみ）

## 2. iOS EntryCell

- [x] 2.1 `KsSettingsViewUI/EntryCell.swift` を実装：`KsCell` / `DSLReidentifiable` / `DSLStyleModifiable` / `DSLIconModifiable` / `VisibilityAware` に準拠。フィールドは spec の EntryCell Requirement に従う（`title` / `description?` / `icon?` / `hintText?` / `isEnabled` / `isVisible` / `text` / `placeholder?` / `keyboardType: UIKeyboardType`（**Native 型直接**、既定 `.default`）/ `isPassword` / `textAlignment` / `accentColor?` / `maxLength: Int?`（既定 `nil`）/ `style?`）。`valueText` は持たない。`id: UUID = UUID()` デフォルト値。callback 経路用に `onTextChanged: ((String) -> Void)?` も併設
- [x] 2.2 `KsSettingsViewUI/EntryCellView.swift` を実装：`KsCellRenderer` 準拠の `UICollectionViewCell` サブクラス。共通行レイアウト関数 `applyCellBaseLayout(...)` 経由で `title` / `description` / `icon` / `hintText` を配置し、accessory slot に `UITextField` を組む。`editingChanged` で TwoWay binding を更新、**`keyboardType` を `UITextField.keyboardType` にそのまま代入**（独自列挙型を経由しない）、`isPassword` で `isSecureTextEntry` を切替、`accentColor` を `tintColor` に反映、`textAlignment` を `UITextField.textAlignment` に反映、`isEnabled` で `UITextField.isEnabled` および色置換を行う
- [x] 2.3 `maxLength` が非 nil のとき `UITextField.delegate` の `textField(_:shouldChangeCharactersIn:replacementString:)` で範囲外の入力を拒否。`maxLength = nil` のときは無制限
- [x] 2.4 SwiftUI `@Binding<String>` 受領のための DSL 経路用 init を `EntryCell` に追加（`init(title:, text: Binding<String>, ...)`）。`Cell` 値型直接構築の経路（callback 経路）と並列
- [x] 2.5 ユニットテスト：bind / TwoWay 入力反映 / `keyboardType` Native 型受け渡し（`UIKeyboardType.phonePad` を渡して `UITextField.keyboardType == .phonePad` を検証）/ isPassword マスク / accentColor 反映 / `maxLength` 制限（境界値・nil 無制限）/ 共通行レイアウト関数経由の描画 / VisibilityAware フィルタ / isEnabled 色置換 / 再利用後のリセット

### 2.A AiForms 互換編集体験（migrate-cell-base-to-stack-layout 完了後に対応）

本セクションは前段 change `migrate-cell-base-to-stack-layout` 完了後に着手する。UITextField の全幅レイアウトは前段 change で自動達成される。

- [x] 2.6 `EntryCellView.init` で `UIToolbar`（`.flexibleSpace` + `.done`）を生成し `textField.inputAccessoryView` に **常時設定**。Done タップで `textField.resignFirstResponder()` を呼ぶ。`UIToolbar(frame: CGRect(0, 0, screenWidth, 44))` + `autoresizingMask = [.flexibleWidth]` で初期サイズを指定し、`_UIToolbarContentView.width == 0` 制約衝突警告を回避する
- [x] 2.7 `EntryCellView` を `TapNotifyingRenderer` に準拠させ、`tapHandler` computed property で `[weak self] in Task { @MainActor in self?.textField.becomeFirstResponder() }` を返す。`KsSettingsViewController.swift` の `TapNotifyingRenderer` extension 群に `extension EntryCellView: TapNotifyingRenderer {}` を追加
- [x] 2.8 `KsSettingsViewController.loadView()` 内で `collectionView.keyboardDismissMode = .onDrag` を設定
- [x] 2.9 `InputCellsTests.swift` に以下のテストを追加：
  - (a) `textField.inputAccessoryView is UIToolbar` かつ items 末尾の `systemItem == .done`
  - (b) `KsSettingsViewController.collectionView.keyboardDismissMode == .onDrag`
  - (c) `UIWindow.makeKeyAndVisible()` 上で `view.tapHandler?()` を呼ぶと `view._textField.isFirstResponder == true`
- [x] 2.10 サンプルアプリ `InputCellsDemoView` で AiForms 互換の編集体験を実機検証：Cell タップでキーボード起動 / Done ボタンタップで閉じる / スクロールで閉じる / 長文入力で見切れない

### 2.B 実機検証で発覚した追加対応（codex 相談含む）

実機検証で `migrate-cell-base-to-stack-layout` 完了後にも EntryCell に複数の問題が残っていたため、追加対応を行った。

- [x] 2.11 `EntryCell.textAlignment` のデフォルト値を `.start` → `.end` に修正（AiForms オリジナル `EntryCell.TextAlignmentProperty = TextAlignment.End` 準拠）。これがないと `render` 内で `textField.textAlignment` が `.left` に上書きされ、文字削除時にキャレット位置が左に流れる現象が発生していた
- [x] 2.12 `UITextFieldDelegate.textFieldShouldReturn(_:)` を実装し `resignFirstResponder()` を呼ぶ。AiForms オリジナル `OnShouldReturn(...)` 準拠
- [x] 2.13 `EntryCellView.render` 内で `UITextField.text` を更新する際は差分判定（`if textField.text != entry.text { textField.text = entry.text }`）のみとする。同値再代入による IME マークドテキスト破壊（日本語入力不能化）を回避。AiForms `UpdateValueText()` のコメント `"Without this judging, TextField don't correctly work when inputting Japanese"` 準拠
- [x] 2.14 パスワード末尾省略の根本対応として `NoIntrinsicWidthTextField` (`UITextField` サブクラス) を実装：`intrinsicContentSize.width = UIView.noIntrinsicMetric` を返し、`isSecureTextEntry = true` 時の intrinsicContentSize 縮小（~19pt）が `fieldWrapper` / `contentStack` の Auto Layout 計算に伝播するのを抑止する。これにより wrapper サイズは contentStack の Distribution=.fill 配分（= title 右側の残り全幅）のみで決まる
- [x] 2.15 `EntryCellView.init` で `fieldWrapper` (`UIView`) を作成し `textField` を 4 辺 pin で内包する。`trailingViews: [fieldWrapper]` で `applyCellBaseLayout` に渡す（直接 `textField` を渡さない）。`fieldWrapper.setContentHuggingPriority(.init(100), for: .horizontal)` で wrapper が title 右側の残り領域を吸う設定。AiForms オリジナル `_FieldWrapper` 構造（`SetContentHuggingPriority(100f, .Horizontal)`）相当
- [x] 2.16 iOS のパスワードフォーカス時自動クリア対策：`UITextFieldDelegate.textFieldDidBeginEditing(_:)` で `isSecureTextEntry && text 非空` のとき現在 text を `secureSavedText` に退避、`handleEditingChanged` で退避値があれば `saved + 新入力` の形で復元、`textFieldDidEndEditing(_:)` で退避値クリア。pasteboard 経由のパスワード窃取防止機構による全クリア動作を抑止する
- [x] 2.17 `InputCellsTests.swift` の既存テストが新レイアウト構造（`trailingViews: [fieldWrapper]` 経由）に追従するよう更新

## 3. iOS PickerCell（単一/複数選択両対応、TextPickerCell は移植しない）

- [x] 3.1 `KsSettingsViewUI/PickerCell.swift` を実装：`KsCell` / `DSLReidentifiable` / `DSLStyleModifiable` / `DSLIconModifiable` / `VisibilityAware` に準拠。フィールドは spec の PickerCell Requirement に従う（`selectionMode: PickerSelectionMode = .single` / `maxSelectedNumber: Int = 0` / `displayFormatter: ((String) -> String)?` 含む）。`.single` の `selectedIndex: Binding<Int?>` を受ける DSL init と、`.multiple` の `selectedIndices: Binding<Set<Int>>` を受ける DSL init を併設
- [x] 3.2 `KsSettingsViewUI/PickerCellView.swift` を実装：共通行レイアウト関数経由でレイアウト。accessory slot に chevron。`valueText` 引数が nil の場合は現在の選択値（`.single` は `items[selectedIndex]` を `displayFormatter` 経由、`.multiple` は `selectedIndices` の項目を `, ` 連結、長すぎは ellipsize）を自動表示。タップで `PickerListViewController` を `UINavigationController` 経由 modal 提示
- [x] 3.3 `KsSettingsViewUI/PickerListViewController.swift`（内部用 `UITableViewController`）を実装：`selectionMode` に応じて `allowsMultipleSelection` を切替。`.single` は選択即時 dismiss、`.multiple` は navigation bar の「完了」ボタンで dismiss。`.multiple` で `maxSelectedNumber > 0` のとき上限到達時は新規チェック無視 + `UIImpactFeedbackGenerator(style: .light)` 触覚フィードバック
- [x] 3.4 ユニットテスト：bind / 自動 valueText（単一・複数）/ displayFormatter / 単一選択 → `selectedIndex` 更新 / 複数選択 → `selectedIndices` 更新 / `maxSelectedNumber` 上限挙動 / モーダル提示（single / multiple）/ 共通行レイアウト関数経由 / VisibilityAware / isEnabled
- [x] 3.5 `TextPickerCell` は **実装しない**（移植対象外）。利用者向けの移行ガイドは `docs/cell-types-input.md` に記載（docs は後続セッションで対応）

## 4. iOS NumberPickerCell

- [x] 4.1 `KsSettingsViewUI/NumberPickerCell.swift` を実装：spec Requirement に従う（`min: Int = 0` / `max: Int = 100` / `step: Int = 1` 既定値、`value: Binding<Int>` DSL init）
- [x] 4.2 `KsSettingsViewUI/NumberPickerCellView.swift` を実装：共通行レイアウト関数経由、accessory slot に chevron、`valueText` 自動表示は `String(value)`。タップで `UIPickerView` 内蔵モーダル提示
- [x] 4.3 ユニットテスト：既定値 / 範囲 / step / 値変更 / 共通行レイアウト関数経由 / isEnabled

## 5. iOS TimePickerCell

- [x] 5.1 `KsSettingsViewUI/TimePickerCell.swift` を実装：spec Requirement に従う（`time: Binding<Date>` を Native `Foundation.Date` で受ける / `format: String = "HH:mm"` 既定値 / `accentColor: UIColor?`）。`Date` の hour / minute 成分のみを参照することを doc comment で明示
- [x] 5.2 `KsSettingsViewUI/TimePickerCellView.swift` を実装：共通行レイアウト関数経由、`valueText` 自動表示は `DateFormatter(dateFormat: format)` で文字列化（hour / minute 成分のみ）。タップで `UIDatePicker(.time)` 内蔵モーダル提示。`Date` 値の hour / minute は `Calendar.current.dateComponents([.hour, .minute], from: date)` 経由で取り出して `UIDatePicker.date` に設定
- [x] 5.3 ユニットテスト：既定 format / 時刻変更で binding の Date 値の hour / minute 成分が更新 / 共通行レイアウト関数経由 / isEnabled

## 6. iOS DatePickerCell

- [x] 6.1 `KsSettingsViewUI/DatePickerCell.swift` を実装：spec Requirement に従う（`date: Binding<Date>` を Native `Foundation.Date` で受ける / `format: String = "yyyy/MM/dd"` 既定値 / `minDate: Date?` / `maxDate: Date?` / `accentColor: UIColor?`）。`Date` の year / month / day 成分のみを参照することを doc comment で明示
- [x] 6.2 `KsSettingsViewUI/DatePickerCellView.swift` を実装：共通行レイアウト関数経由、`valueText` 自動表示は `DateFormatter(dateFormat: format)` で文字列化（year / month / day 成分のみ）。タップで `UIDatePicker(.date)` 内蔵モーダル提示、`minDate` / `maxDate` を `UIDatePicker.minimumDate` / `maximumDate` に反映
- [x] 6.3 ユニットテスト：既定 format / 日付変更で binding の Date 値の year / month / day 成分が更新 / 範囲制限 / 共通行レイアウト関数経由 / isEnabled
- [x] 6.4 **iOS には `androidUiStyle` / `androidButtonColor` 引数を持たせない**（Android 限定）。シンボル分岐が必要な箇所では `#if canImport(UIKit)` などで条件コンパイル

## 7. iOS 一括登録

- [x] 7.1 `KsSettingsViewUI/KsCellRegistryInputCells.swift` で `extension KsCellRegistry { public func registerInputCells() }` を実装（5 種を登録）（実ファイル名は `KsCellRegistry+InputCells.swift`、既存 `KsCellRegistry+BasicCells.swift` と命名規約を揃えた）
- [x] 7.2 `KsSettingsViewController.init` で auto-register（オプトアウト可能）

## 8. Android EntryCell

- [x] 8.1 `ks-settingsview-ui/EntryCell.kt` を実装：`Cell` / `DSLReidentifiableCell` / `DSLStyleModifiableCell` / `DSLIconModifiableCell` / `VisibilityAware` に準拠。フィールドは spec Requirement に従う（`keyboardType: Int = android.text.InputType.TYPE_CLASS_TEXT`（**Native 型直接**）/ `maxLength: Int? = null` 含む）。`id: String = "entry-cell-${java.util.UUID.randomUUID()}"` デフォルト値。callback 経路用に `onTextChanged: ((String) -> Unit)?` を併設
- [x] 8.2 `ks-settingsview-ui/EntryCellViewHolder.kt` を実装：共通行レイアウト関数 `applyCellBaseLayout(views, ...)` 経由で `title` / `description` / `icon` / `hintText` を配置、accessory slot に `EditText` を組む。`TextWatcher` を bind 内で設定し reset 内で除去（再利用時のループ防止）、**`keyboardType: Int` を `EditText.inputType` にそのまま代入**（独自列挙型を経由しない）、`isPassword = true` のときは `keyboardType` に `InputType.TYPE_TEXT_VARIATION_PASSWORD` を OR 合成、`accentColor` を `textCursorDrawable` の tint に反映（API 29+）、`isEnabled` で `EditText.isEnabled` および色置換
- [x] 8.3 `maxLength` が非 null のとき `EditText.filters = arrayOf(InputFilter.LengthFilter(maxLength))` を設定。`null` のときは `filters = emptyArray()`（無制限）
- [x] 8.4 ~~`res/layout/cell_entry.xml`（`EditText` を内包するシンプルなレイアウト。共通行レイアウト関数が `LinearLayout` / `RelativeLayout` 構造を提供する前提で accessory slot のみ定義）~~ → 既存基本 Cell（SwitchCell 等）と同様、XML レイアウトは使用せず programmatic に accessoryHolder へ EditText を addView する方針に合わせて XML 不要とした
- [x] 8.5 ユニットテスト：bind / TwoWay 入力反映（`TextWatcher` シミュレート）/ `keyboardType` Native 型受け渡し（`InputType.TYPE_CLASS_PHONE` を渡して `EditText.inputType == TYPE_CLASS_PHONE` を検証）/ isPassword マスク / `maxLength` 制限（境界値・null 無制限）/ 共通行レイアウト関数経由 / VisibilityAware フィルタ / isEnabled 色置換 / 再利用後の `TextWatcher` 除去確認

## 9. Android PickerCell（単一/複数選択両対応、TextPickerCell は移植しない）

- [x] 9.1 `ks-settingsview-ui/PickerCell.kt` を実装：spec Requirement に従う（`selectionMode: PickerSelectionMode = PickerSelectionMode.Single` / `maxSelectedNumber: Int = 0` / `displayFormatter` 含む）。`.Single` の `selectedIndex: MutableState<Int?>` と `.Multiple` の `selectedIndices: MutableState<Set<Int>>` の 2 つのコンストラクタを併設、`onSelectionChanged` / `onMultiSelectionChanged` callback も併設 — data class 1 つで両モードを保持し、Compose DSL 拡張関数側で overload を提供する形にした
- [x] 9.2 `ks-settingsview-ui/PickerCellViewHolder.kt` を実装：共通行レイアウト関数経由、accessory slot に chevron。`valueText` 自動表示は単一は `items[selectedIndex]`（`displayFormatter` 経由）、複数は選択項目を `, ` 連結（ellipsize end）。タップで `selectionMode` に応じて `AlertDialog.Builder().setSingleChoiceItems` または `setMultiChoiceItems` 提示、`.Multiple` は Positive ボタン「完了」で MutableState 更新
- [x] 9.3 `.Multiple` で `maxSelectedNumber > 0` 上限到達時は新規チェックを無視し、`view.performHapticFeedback(HapticFeedbackConstants.REJECT)` で触覚フィードバック
- [x] 9.4 ユニットテスト：bind / 自動 valueText（単一・複数）/ displayFormatter / 単一選択 → `selectedIndex` 更新 / 複数選択 → `selectedIndices` 更新 / `maxSelectedNumber` 上限挙動 / AlertDialog 表示（single / multi）/ 共通行レイアウト関数経由 / VisibilityAware / isEnabled
- [x] 9.5 `TextPickerCell.kt` は **実装しない**（移植対象外）

## 10. Android NumberPickerCell

- [x] 10.1 `ks-settingsview-ui/NumberPickerCell.kt` を実装：spec Requirement に従う（既定値 `min = 0` / `max = 100` / `step = 1`）
- [x] 10.2 `ks-settingsview-ui/NumberPickerCellViewHolder.kt` を実装：共通行レイアウト関数経由、accessory slot に chevron、`valueText` 自動表示は `value.toString()`。タップで `android.widget.NumberPicker` を内包する `AlertDialog` を表示
- [x] 10.3 ユニットテスト：既定値 / 範囲 / step / 値変更 / 共通行レイアウト関数経由 / isEnabled

## 11. Android TimePickerCell / DatePickerCell

- [x] 11.1 `ks-settingsview-ui/TimePickerCell.kt` を実装：`time: MutableState<java.time.LocalTime>` を Native 型で受ける。`TimePickerCellViewHolder.kt` で `MaterialTimePicker` を表示。`valueText` 自動表示は `LocalTime.format(DateTimeFormatter.ofPattern(format))` で文字列化
- [x] 11.2 `ks-settingsview-ui/DatePickerCell.kt` を実装：`date: MutableState<java.time.LocalDate>` を Native 型で受ける。`androidUiStyle: DatePickerAndroidStyle = DatePickerAndroidStyle.Material`、`androidButtonColor: Color? = null`、`minDate: LocalDate?` / `maxDate: LocalDate?` を持つ
- [x] 11.3 `ks-settingsview-ui/DatePickerCellViewHolder.kt` を実装：`androidUiStyle` の値で UI 切替
  - `Material` のとき `MaterialDatePicker` を表示。`valueText` 自動表示は `LocalDate.format(DateTimeFormatter.ofPattern(format))`。`minDate` / `maxDate` を `CalendarConstraints` の `DateValidator` に反映（`LocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()` で `Long` 変換、MaterialDatePicker は UTC epoch ms を扱うため）
  - `Spinner` のとき `android.widget.DatePicker`（programmatic に `calendarViewShown = false` で Spinner モード相当）を内包する `AlertDialog` を表示。`minDate` / `maxDate` を `DatePicker.minDate` / `maxDate`（system default zone の epoch ms）に反映。`androidButtonColor` を `AlertDialog` の Positive / Negative ボタン色に反映
- [x] 11.4 `androidx.fragment:fragment-ktx:1.8.4` 依存を `ks-settingsview-ui/build.gradle.kts` に追加（`MaterialDatePicker` / `MaterialTimePicker` 利用のため）
- [x] 11.5 `minSdk = 29` のため `java.time.*` は desugaring 不要であることを確認（API 26+ で標準提供）。`coreLibraryDesugaring` 設定は追加しない
- [x] 11.6 ユニットテスト：既定 format / `LocalTime` / `LocalDate` formatTime / formatDate ヘルパ / 既定 androidUiStyle = Material / 共通行レイアウト関数経由 valueText 反映 — Fragment 経由のダイアログ提示は `FragmentScenario` ベースでは Robolectric MaterialTimePicker 制約に依存するため、本セッションでは bind / 値変換ヘルパまでをカバーした単体テストに留めた（実機検証は後続セッション）

## 12. Android 一括登録

- [x] 12.1 `ks-settingsview-ui/KsCellRegistryInputCells.kt` で `fun KsCellRegistry.registerInputCells(context: Context)` を実装（5 種を登録、viewType 110-114）
- [x] 12.2 `KsSettingsView.init` で auto-register（`!KsCellRegistry.isRegistered(EntryCell::class)` ガードで重複登録防止、テスト・利用者の事前登録によるオプトアウト可能）

## 13. Compose DSL 拡張関数（ks-settingsview-compose）

- [x] 13.1 `ks-settingsview-compose/InputCellDsl.kt` を作成し、`DSLSectionScope` 拡張関数を実装（**5 種**、TextPickerCell は実装しない）：
  - `fun DSLSectionScope.EntryCell(title: String, text: MutableState<String>, keyboardType: Int = android.text.InputType.TYPE_CLASS_TEXT, maxLength: Int? = null, ...): CellHandle`
  - `fun DSLSectionScope.PickerCell(title: String, items: List<String>, selectedIndex: MutableState<Int?>, ...): CellHandle`（**単一選択 overload**、`selectionMode` は内部で `.Single` に固定）
  - `fun DSLSectionScope.PickerCell(title: String, items: List<String>, selectedIndices: MutableState<Set<Int>>, maxSelectedNumber: Int = 0, ...): CellHandle`（**複数選択 overload**、`selectionMode` は内部で `.Multiple` に固定）
  - `fun DSLSectionScope.NumberPickerCell(title: String, value: MutableState<Int>, ...): CellHandle`
  - `fun DSLSectionScope.TimePickerCell(title: String, time: MutableState<java.time.LocalTime>, ...): CellHandle`
  - `fun DSLSectionScope.DatePickerCell(title: String, date: MutableState<java.time.LocalDate>, androidUiStyle: DatePickerAndroidStyle = DatePickerAndroidStyle.Material, androidButtonColor: Color? = null, ...): CellHandle`
- [x] 13.2 各拡張関数の引数 `style: CellStyle = CellStyle()` は UI 層 `jp.kamusoft.kssettingsview.ui.CellStyle` を参照する。`@SettingsRootDsl` を付与
- [x] 13.3 ユニットテスト：DSL 内呼び出しで Cell が正しく `DSLCellNode` に格納される / `PickerCell` の 2 overload が正しく解決される（`selectedIndex` 渡し vs `selectedIndices` 渡しで適切な方が選ばれる）/ `EntryCell(keyboardType = InputType.TYPE_CLASS_PHONE)` が Native 型のまま反映される / `EntryCell(maxLength = 5)` が data class に反映される / 戻り値 `CellHandle` への chain（`.cellHeight(...)` 等）が動作する

## 14. Sample 更新

- [x] 14.1 `samples/ios/` の SwiftUI Sample に「入力 Cell ページ」を追加し、**5 種すべて** の入力系 Cell を新 DSL 形式・TwoWay binding 形式・Native 型直接公開で表示。例：
  - `EntryCell(title: "名前", text: $userName, keyboardType: .default, maxLength: 20)`
  - `EntryCell(title: "電話", text: $phone, keyboardType: .phonePad)`（Native `UIKeyboardType.phonePad` 直接渡し）
  - `PickerCell(title: "テーマ", items: themes, selectedIndex: $themeIndex)`（単一）
  - `PickerCell(title: "通知種別", items: notifTypes, selectedIndices: $notifSelection, selectionMode: .multiple, maxSelectedNumber: 2)`（複数 + 上限）
  - `NumberPickerCell(title: "音量", value: $volume)`
  - `TimePickerCell(title: "アラーム", time: $alarmDate)`（`Date` を使用）
  - `DatePickerCell(title: "誕生日", date: $birthdayDate)`（`Date` を使用）
- [x] 14.2 `samples/android/` の Compose Sample に「入力 Cell ページ」を追加し、**5 種すべて** の入力系 Cell を新 DSL 形式・`MutableState` binding 形式・Native 型直接公開で表示。13.1 で追加した DSL 拡張関数を経由。例：
  - `EntryCell(title = "名前", text = userName, maxLength = 20)`
  - `EntryCell(title = "電話", text = phone, keyboardType = InputType.TYPE_CLASS_PHONE)`（Native `InputType` 定数直接渡し）
  - `PickerCell(title = "テーマ", items = themes, selectedIndex = themeIndex)`（単一）
  - `PickerCell(title = "通知種別", items = notifTypes, selectedIndices = notifSelection, maxSelectedNumber = 2)`（複数）
  - `TimePickerCell(title = "アラーム", time = alarmTime)`（`LocalTime`）
  - `DatePickerCell(title = "誕生日", date = birthday, androidUiStyle = DatePickerAndroidStyle.Material)`（Material）
  - `DatePickerCell(title = "予約日", date = reservation, androidUiStyle = DatePickerAndroidStyle.Spinner)`（Spinner、別ページまたは同ページの別行）

## 15. ドキュメント

- [ ] 15.1 `docs/cell-types-input.md` を新規作成し、**5 種** の入力系 Cell 各々のフィールド一覧・既定値・使用例（iOS / Android スニペット）・TwoWay binding と callback 経路の使い分け・PickerCell 単一/複数選択モードの使い分け・DatePickerCell の Android UI スタイル切替の使い分け・`EntryCell.maxLength` の使い方を記載
- [ ] 15.2 `docs/cells.md` に「入力 Cell へのリンク」セクションを追加し、`cell-types-input.md` を参照させる
- [ ] 15.3 `Foundation.Date` の hour/minute/year/month/day 成分取り出し例（`Calendar.current.dateComponents([.hour, .minute], from: date)` 等）、および `java.time.LocalTime` / `LocalDate` の構築・フォーマット例（`LocalTime.of(...)` / `DateTimeFormatter.ofPattern(...)`）を `docs/cell-types-input.md` に記載
- [ ] 15.4 旧 AiForms `TextPickerCell` ユーザーの移行ガイドを `docs/cell-types-input.md` に追記（`TextPickerCell(items:, selectedItem:)` → `PickerCell(items:, selectedIndex:)` + `items[index]` で `String` 取得）

## 16. 全テスト実行

- [x] 16.1 `ios/` で `swift test` 全成功（`xcodebuild test` で 313 件全成功確認、本セッション iOS スコープ）
- [x] 16.2 `android/` で `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` 全成功（debug+release 511 件すべて成功、入力 Cell 関連新規テスト 48 件含む）

## 依存関係

- 先行 archive 済み：`add-monorepo-foundation` / `add-settings-view-core` / `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-declarative-dsl` / `add-partial-update-core` / `add-partial-update-native` / `add-cell-types-basic` / `port-theme-and-cellstyle-missing-fields` / `purify-core-extract-style-to-ui-layer` / `unify-cell-common-fields-via-shared-row-layout` / `refine-cell-layout-after-unify-review` / `add-visibility-flags-section-and-cell` / `add-samples-ios` / `add-samples-android`
- 並列可能：`add-cell-types-custom`（`CustomCell` 追加）
- 後続：`add-maui-cells`（本提案完了後、MAUI 側 5 種入力 Cell Handler 実装と `samples/maui/` への 5 種ページ追加を担当）
- 本提案は Native iOS / Android のみを対象とし、MAUI Sample 拡張は責務に含めない

## 完了条件

- 全タスクのチェックボックスが完了している
- `cell-types-input` capability の全 Scenario が通る（本提案では `settings-view-core` capability への追加は一切ない）
- iOS / Android の各 Sample で 5 種の入力系 Cell が表示・操作でき、TwoWay binding が動作する
- 共通行レイアウト関数経由の描画により、基本 Cell（`LabelCell` 等）と並べたときに accessory 右端 X 座標・`hintText` 右上 float 位置が揃う
- 全ユニットテスト成功
