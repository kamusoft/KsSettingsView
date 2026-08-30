## 参考実装

本変更提案の実装に着手する**前に必ず**以下を確認すること。入力系 Cell は旧版 AiForms の細かなオプション（`PageTitle` / `AccentColor` / `Format` / `SelectionMode` 等）が多く、漏らすと利用者の移行で躓く。

- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**:
    - §2 `CellBase の共通 BindableProperty 一覧`（22 プロパティ。本提案の各入力 Cell も共通フィールドはここから引き継ぐ）
    - §3 `各 Cell 固有の BindableProperty`（`EntryCell` / `PickerCell` / `TextPickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の項目）
    - §10 `Sample コードの場所`（XAML での旧版使用例）
- [`docs/cells.md`](../../../docs/cells.md) — `cell-types-basic` で確立された **基本 Cell 7 種の共通規約** の解説（共通行レイアウト・accessory 配置・`hintText` の右上 float・`isEnabled` / `isVisible` の意味）。入力系 Cell も全く同じ規約に乗せる
- [`openspec/specs/cell-types-basic/spec.md`](../../specs/cell-types-basic/spec.md) — 「全 Cell 共通の description / valueText / icon / hintText フィールド」「全 Cell 共通の isEnabled」「全 Cell 共通の isVisible」「全 Cell 共通の Theme.titleColor / Theme.titleFont 反映」「共通行レイアウト関数経由での描画」Requirement
- 原典コード：
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/EntryCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/EntryCell.cs) — `ValueText` は **TwoWay**、`Keyboard` 種別、`IsPassword`、`Placeholder`、`AccentColor`（caret 色）、`TextAlignment`、`MaxLength`（旧版互換）
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs) — 14 個のプロパティ、単一/複数選択、`SelectedItem(s)` **TwoWay**、`PageTitle`、`UseAutoValueText`
  - `TextPickerCell.cs`：本提案では **移植対象外**（使用頻度が低く `PickerCell(selectionMode: .single, ...)` で代替可能）
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/NumberPickerCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/NumberPickerCell.cs) — `Number` **TwoWay**、`Min = 0` / `Max = 100`
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/TimePickerCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/TimePickerCell.cs) — `Time` **TwoWay**、`Format = "t"`
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/DatePickerCell.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/DatePickerCell.cs) — `Date` **TwoWay**、`Format = "d"`、`MinimumDate` / `MaximumDate`、`IsAndroidSpinnerStyle` の Android 固有挙動
  - [`../AiForms.Maui.SettingsView/SettingsView/Pages/`](file://../AiForms.Maui.SettingsView/SettingsView/Pages/) — `PickerCell` モーダル Page 実装

**重要**: 旧版は `TimeSpan` / `DateTime` / `Keyboard` を直接 `BindableProperty` に出していた。KsSettingsView Native でも **同じ方針** を採る — 日時型は iOS `Foundation.Date` / Android `java.time.LocalTime` / `java.time.LocalDate`、キーボード種別は iOS `UIKit.UIKeyboardType` / Android `android.text.InputType`（`Int`）を **UI 層 API でそのまま直接公開する**。`KsColor` / `KsFont` 独自値型の前例（Native 型を独自値型でラップした結果、利用者の使い勝手が著しく悪化し最終的に `purify-core-extract-style-to-ui-layer` で撤回された）と同じ轍を踏まないため、`KsKeyboardType` / `KsTime` / `KsDate` の独自値型・独自列挙型はすべて **廃案** とする。`add-maui-cells` での Bridge 境界では、C# `Keyboard` / `TimeSpan` / `DateTime` から各 Native 型への変換を実施する。

## Context

入力系 Cell（Entry / Picker / NumberPicker / TimePicker / DatePicker の **5 種**。旧 AiForms には TextPickerCell も存在したが本提案では移植対象外）はユーザー入力を伴うため、ピッカーモーダル UX、キーボード制御、範囲バリデーションなど基本 Cell より複雑な状態を扱う。旧 `AiForms.Maui.SettingsView/SettingsView/Cells/PickerCell.cs` などの仕様を参考にしつつ、Native プラットフォームに適合した UX に再設計する。

本提案は当初 2026-06-07 に作成されたが、その後に多数の依存 change が archive されて規約が大幅に整理された。特に以下の確立済み仕様に乗せる必要がある：

- `cell-types-basic`：共通 Optional 6 フィールド（`description` / `valueText` / `icon` / `hintText` / `isEnabled` / `isVisible`）、`VisibilityAware` opt-in、共通行レイアウト関数 `applyCellBaseLayout(...)` 経由、`Theme.cellTitleColor` / `Theme.cellTitleFont` の 3 段階解決（`CellStyle → Theme → 既定`）
- `settings-view-core`：`Cell` / `KsCell` 抽象は `style` プロパティを要求しない、`SettingsRoot` は `Theme` を保持しない、`SettingsRootDiff.replaceCell` は内容更新（reconfigure）経路、`Section.isVisible` / `Cell.isVisible` は構造同期上の追加・削除アニメーション（reconfigure 経路ではない）
- `settings-view-ios-style` / `settings-view-android-style`：`Theme` / `CellStyle` は **UI 層所属** で Native 型（`UIColor?` / `UIFont?` / Compose `Color?` / `TextStyle?`）を直接保持
- `settings-view-ios-swiftui` / `settings-view-android-compose`：DSL 拡張関数規約・`@Binding<T>` / `MutableState<T>` 駆動の Binding セル規約
- `purify-core-extract-style-to-ui-layer` の教訓：Native 値型を独自値型でラップする設計（旧 `KsColor` / `KsFont`）は利用者の使い勝手と相互運用性を損ねるため、本提案でも **日時型は独自定義せず Native 型を直接 UI 層 API に公開する**

## Goals / Non-Goals

**Goals:**
- **5 種** の入力系 Cell を iOS / Android で実装：Entry / Picker / NumberPicker / TimePicker / DatePicker
- 各 Cell は `cell-types-basic` の共通 Optional 6 フィールドを持ち、`VisibilityAware` に opt-in 準拠し、共通行レイアウト関数経由でレイアウト構成する
- TwoWay binding を iOS `@Binding<T>` / Android `MutableState<T>` で全 5 Cell に提供（callback API も併設）
- ピッカーモーダル UX をプラットフォーム慣習に合わせる（iOS は `UIPickerView` / `UITableViewController` / `UIDatePicker` モーダル、Android は `AlertDialog` / Material Picker / `android.widget.DatePicker` Spinner）
- `PickerCell` で **単一選択 / 複数選択 両モードをサポート**（旧 AiForms 互換、`SelectionMode = Single / Multiple` 相当）。`maxSelectedNumber` で複数選択上限も指定可能
- `DatePickerCell` で **Android 側の Material / Spinner UI 切替をサポート**（旧 AiForms の `IsAndroidSpinnerStyle` 相当）
- `EntryCell` に **`maxLength: Int?`** を持たせて旧 AiForms `MaxLength` 互換とする
- **Native 型を UI 層 API でそのまま直接公開する**：
  - 日時：iOS `Foundation.Date` / Android `java.time.LocalTime` / `java.time.LocalDate`
  - キーボード種別：iOS `UIKit.UIKeyboardType` / Android `android.text.InputType`（`Int`）
  - 独自値型・独自列挙型（`KsKeyboardType` / `KsTime` / `KsDate`）は導入しない。Core 層への追加もない

**Non-Goals:**
- 旧 `TextPickerCell` は **移植対象外**（使用頻度が低く、API が `PickerCell(selectionMode: .single, ...)` でほぼ代替可能。`SelectedItem: String` ベースの API を必要とする利用者は `selectedIndex` から `items[selectedIndex]` を引いて使う）
- カスタムピッカーレイアウト（複数 wheel ピッカーなど）はサポートしない（旧 AiForms 互換のシンプル形式のみ）
- `PickerCell` の `UseNaturalSort`（自然順ソート）と `SelectedItemsOrderKey`（ソートキー）は本提案では実装しない（利用者は `items: [String]` を事前にソートしてから渡す方針）
- 多言語ロケール固有の日付フォーマットの自動切替は `format` 文字列指定で代替（locale 別自動判定は Phase 6）
- MAUI Handler および `samples/maui/` への 5 種入力 Cell ページ追加は `add-maui-cells` で対応
- 旧 AiForms の `PageTitle` / `PickerTitle` プロパティを「モーダル画面のナビゲーションバータイトル」として保持する用途で `pageTitle: String?` / `pickerTitle: String?` を持たせるが、Android `AlertDialog` の `setTitle` も同じ値を流用するなど、厳密な「画面（Page）」概念は再現しない

## Decisions

### Decision 1: 日時型・キーボード種別ともに Native 型を UI 層 API でそのまま直接公開（独自値型・独自列挙型は導入しない）

**選択**: 入力系 Cell のフィールド型はすべて **Native 型を UI 層 API のシグネチャに直接出す**。独自値型・独自列挙型でラップしない。Core モジュール（`settings-view-core` capability）への追加もない。

| 用途 | iOS（`KsSettingsViewUI`） | Android（`ks-settingsview-ui`） |
|---|---|---|
| 時刻（`TimePickerCell.time`） | `Foundation.Date`（`Binding<Date>`） | `java.time.LocalTime`（`MutableState<LocalTime>`） |
| 日付（`DatePickerCell.date` / `minDate` / `maxDate`） | `Foundation.Date`（`Binding<Date>` / `Date?`） | `java.time.LocalDate`（`MutableState<LocalDate>` / `LocalDate?`） |
| キーボード種別（`EntryCell.keyboardType`） | `UIKit.UIKeyboardType`（既定 `.default`） | `android.text.InputType` の `Int` 定数（既定 `InputType.TYPE_CLASS_TEXT`） |

**理由**:
- `KsColor` / `KsFont` 独自値型の前例（`add-monorepo-foundation` 〜 `add-settings-view-core` で導入、`purify-core-extract-style-to-ui-layer` で UI 層に移して Native 型直接保持に切替えるまで利用者の使い勝手と相互運用性が著しく損なわれた）と **同じ轍を踏まない**
- 利用者が `Date` / `LocalDate` / `UIKeyboardType` / `InputType.TYPE_*` を既存の他ライブラリ（Realm / Room / Codable JSON、`UITextField` 直設定 / `EditText.inputType` 直設定など）から取得・流用した値をそのまま渡せる
- iOS では `Foundation.Date` が日時表現の de facto standard、`UIKeyboardType` がキーボード種別の de facto standard で、各エコシステム（`Calendar` / `DateFormatter` / `UIDatePicker` / `UITextField`）と完全に整合する
- Android では `java.time.LocalTime` / `LocalDate`（API 26+）と `android.text.InputType` の `Int` 定数が標準で、`MaterialTimePicker` / `MaterialDatePicker` / `EditText.inputType` API と直接相互運用可能
- `add-maui-cells` の Bridge 境界では、C# `TimeSpan` / `DateTime` / `Keyboard` から各 Native 型への変換を素直に実装できる
- 独自抽象を挟まないことで Core 層がシンプルに保たれ、本提案で `settings-view-core` capability への変更が一切不要となる（delta spec も不要）

**代替案**:
- 独自値型 `KsTime` / `KsDate` / 独自列挙型 `KsKeyboardType`：旧 proposal 案。`KsColor` の前例から学ぶ通り、利用者の利便性を損ねるため却下
- `java.util.Date`（Android）：API レガシーで mutable。`java.time.*` が使える環境では非推奨
- iOS の `DateComponents`：時刻 / 日付の分離表現として候補だが、`UIDatePicker` との相互運用は `Foundation.Date` がより直接的

**Android `minSdk` 制約**: `java.time.*` は API 26+。`ks-settingsview-ui` の現在の `minSdk` を確認し、26 未満を要する場合は `desugar_jdk_libs`（`coreLibraryDesugaringEnabled = true`）で対応する。

**例外（UI 層独自列挙型）**: `PickerSelectionMode` / `DatePickerUIStyle` は Cell の動作モードを表す **UI 層独自の論理スイッチ** であり、対応する Native 型が存在しない（UIKit / Android にも `single / multiple` や `wheels / calendar` を 1 つの列挙で表す型はない）ため独自列挙型を UI 層に置く。これは「Native 型をラップする」ではなく「UI 層の論理スイッチを定義する」用途のため Decision 1 の方針と矛盾しない。なお `DatePickerUIStyle` は iOS / Android で同じ型名・同じプロパティ名 (`uiStyle`) を採用するが、enum のケースはプラットフォーム固有 UI を反映する（iOS: `.wheels` / `.calendar`、Android: `.Material` / `.Spinner`）。詳細は Decision 12 参照。

### Decision 2: PickerCell の単一/複数選択両対応、TextPickerCell は移植対象外

**選択**:

- `PickerCell` は **単一選択 / 複数選択 両モード** をサポートする。`selectionMode: PickerSelectionMode`（`single` / `multiple`、既定 `single`）で切替える
  - `selectionMode = .single` の TwoWay binding は `selectedIndex: Binding<Int?>` / `MutableState<Int?>`、callback は `onSelectionChanged: ((Int) -> Void/Unit)?`
  - `selectionMode = .multiple` の TwoWay binding は `selectedIndices: Binding<Set<Int>>` / `MutableState<Set<Int>>`、callback は `onMultiSelectionChanged: ((Set<Int>) -> Void/Unit)?`
  - `maxSelectedNumber: Int = 0`（`0` は無制限、`.multiple` モードでのみ有効）
- `TextPickerCell` は **本提案では移植しない**（旧 AiForms にあるが使用頻度が低く、`PickerCell(selectionMode: .single, ...)` でほぼ代替可能なため）

iOS モーダルでは `.single` のとき `UITableViewCell.accessoryType = .checkmark`、`.multiple` のとき複数行に `.checkmark` をつけて「完了」ボタンで dismiss。Android では `.single` のとき `AlertDialog.Builder.setSingleChoiceItems`、`.multiple` のとき `setMultiChoiceItems` を使用する。

**理由**:
- 旧 AiForms `PickerCell` には `SelectionMode = Single / Multiple` / `SelectedItems: IList` / `MaxSelectedNumber` があり、利用者の移行で必要
- 複数選択の結果は `Set<Int>`（順序非依存、重複なし）で公開し、利用者が `items[i]` を引いて使う想定
- 旧 `TextPickerCell` は `SelectedItem: String` ベースの薄い差分しかなく、`PickerCell(selectionMode: .single)` + `items[selectedIndex]` で代替できるため、API 表面積を増やす価値が低いと判断

**代替案**:
- `TextPickerCell` も実装し `selectedItem: Binding<String?>` を公開：旧 AiForms 互換性は上がるが、ほぼ同等の Cell を 2 種維持することになり保守コストが見合わない。利用者には移行ドキュメントで `PickerCell` への置き換え方を案内する
- `PickerCell` を単一選択のみにし、複数選択は別 Cell `MultiPickerCell` として追加：旧 AiForms の `PickerCell.SelectionMode` 設計と異なり、移行容易性が低い

**旧 TextPickerCell ユーザーの移行ガイド**: `TextPickerCell(items: ["A", "B"], selectedItem: $value)` → `PickerCell(items: ["A", "B"], selectedIndex: $index)` に書き換え、`$value` を必要とするコードは `items[index]` で置換する。`docs/cell-types-input.md` にも明記する。

### Decision 3: Picker UI の実装方式

**選択**:
- iOS：
  - `PickerCell`（`.single`）→ `UITableViewController` を `UINavigationController` でモーダル表示（`UITableViewCell.accessoryType = .checkmark` で選択表示）
  - `PickerCell`（`.multiple`）→ `UITableViewController`（`allowsMultipleSelection = true`）+ navigation bar の「完了」ボタンで dismiss
  - `NumberPickerCell` → **AiForms 互換の埋め込み方式**：透明 no-caret `UITextField` (`EmbeddedPickerHostField`) を `ContentView` の subview に貼り、その `inputView` に `UIPickerView`、`inputAccessoryView` に `[Cancel] [Title] [Done]` の `UIToolbar` をセットして、Cell タップで `becomeFirstResponder()` を呼び iOS にキーボード位置へスライドアップ表示させる
  - `TimePickerCell` → **AiForms 互換の埋め込み方式**：同じく `EmbeddedPickerHostField.inputView = UIDatePicker(.time)`
  - `DatePickerCell` → `uiStyle: DatePickerUIStyle` で UI を切替
    - `DatePickerUIStyle.wheels`（既定）→ **AiForms 互換の埋め込み方式**：`EmbeddedPickerHostField.inputView = UIDatePicker(.date) + .wheels`。Toolbar に `todayText` が non-nil なら `[Cancel] [Title] [Today] [Done]` 構成、nil なら `[Cancel] [Title] [Done]` 構成
    - `DatePickerUIStyle.calendar` → **`.pageSheet` + `.custom` detent (≒480pt) のシート**で `UIDatePicker(.date) + .inline` カレンダー grid を表示（iOS カレンダーアプリ風）。下部に `[Cancel] [todayText?] [Done]` の `UIButton` バー。シート提示 VC は `DatePickerCalendarSheetController` として実装
- Android：
  - `PickerCell`（`.single`）→ `AlertDialog.Builder().setSingleChoiceItems`
  - `PickerCell`（`.multiple`）→ `AlertDialog.Builder().setMultiChoiceItems` + 「完了」ボタン
  - `NumberPickerCell` → `android.widget.NumberPicker` を内包する `AlertDialog`
  - `TimePickerCell` → `com.google.android.material.timepicker.MaterialTimePicker`
  - `DatePickerCell` → `uiStyle: DatePickerUIStyle` で UI を切替（iOS と同じプロパティ名を採用、Decision 12 参照）
    - `DatePickerUIStyle.Material`（既定）→ `com.google.android.material.datepicker.MaterialDatePicker`
    - `DatePickerUIStyle.Spinner` → `android.widget.DatePicker`（`android:datePickerMode="spinner"`）を内包する `AlertDialog`

**理由**:
- iOS の埋め込み方式（`UITextField.inputView` 経由）は AiForms オリジナル (`NumberPickerCellView.cs` / `TimePickerCellView.cs` / `DatePickerCellView.cs` の `DummyField` パターン) と同等の UX を再現する。フルスクリーンモーダルだとユーザーの注意を Settings 画面から完全に奪うが、埋め込み方式なら Cell 行が見えたままで操作できるため Settings の文脈に留まる
- iOS の `DatePickerCell` には埋め込みウィール（`.wheels`）に加えて `.calendar` モード（iOS カレンダーアプリ風の `.inline` カレンダー grid）を提供し、利用者が UI 選択肢を持てるようにする。Android の `Material` / `Spinner` 切替と並列のコンセプト
- 各プラットフォームで最も慣習的な UX
- 標準 API のため追加依存ライブラリは不要（`MaterialTimePicker` / `MaterialDatePicker` の `androidx.fragment` 依存だけは新規追加する）
- 旧 AiForms `DatePickerCell` の `IsAndroidSpinnerStyle = true` で Spinner UI を選んでいたユーザーが Material 既定で UX が変わってしまうのを避けるため、明示的に切替可能とする

**代替案（iOS Picker UI）**:
- フルスクリーンモーダル（`UINavigationController` + present）：旧版 (`add-cell-types-input` 初期実装) で採用していた方式。実装は単純だが、画面遷移感が強すぎて Settings 画面の文脈から離れる。Image #2 のような UX となり AiForms 互換性も失われるため不採用
- インライン展開（Cell 自体が拡張する）：旧 AiForms にあったが、画面が狭くなり Section 内 Cell 順序が乱れる。Phase 6 で再検討。
- iPad の `popoverPresentationStyle`：iPhone では fall back されフルスクリーンになるため不採用

**代替案（DatePickerCell の calendar mode の表示形式）**:
- `UIAlertController` に `UIDatePicker.inline` を subview として突っ込む：幅が ~270pt と狭く `.inline` カレンダーが押しつぶされる。Apple 公式サポート外の使い方で将来壊れるリスクあり
- 透明オーバーレイ ViewController で自前モーダル：ダークモード / accessibility / ジェスチャを全部自前実装する必要があり実装重い
- `.formSheet`：iPhone でも横幅を取りすぎる
- **採用案: `.pageSheet` + `.custom` detent (480pt) + `prefersGrabberVisible`**：iOS 16+ の Apple 公式 API。iOS カレンダーアプリ「新規イベント」画面と同じ手法。`.custom` detent で「画面下半分くらいに収まる小さめシート」を実現できる。iOS 15 環境では `.medium()` detent に fall back

**代替案（DatePickerCell の Android UI 切替プロパティ）**:
- `DatePickerUIStyle` を Cell プロパティではなく Theme 全体設定とする：DatePickerCell 単位で切替えたい用途（例：誕生日入力は Material、予約日入力は Spinner）に対応しづらいため Cell 単位とする。

### Decision 4: モーダル / シート / 埋め込み Picker の表示起点

**選択**: Cell 自身に `onTap` 相当のクロージャを持たせるのではなく、Cell View / ViewHolder 内部で表示制御を行う。利用者は TwoWay binding と `onValueChanged` callback のみ受け取れば良い：

- iOS 埋め込み方式（`NumberPickerCell` / `TimePickerCell` / `DatePickerCell(.wheels)`）：Cell タップ → `embeddedField.becomeFirstResponder()` で iOS が `inputView` を自動的にスライドアップ表示する。`KeyWindowResolver` 経由のモーダル提示は不要
- iOS シート方式（`DatePickerCell(.calendar)`）：Cell タップ → `KeyWindowResolver.topPresentedViewController().present(DatePickerCalendarSheetController, animated: true)` で `.pageSheet` + `.custom` detent シートを提示
- iOS モーダル方式（`PickerCell`）：Cell タップ → `KeyWindowResolver.topPresentedViewController().present(...)` で `UINavigationController` ベースのモーダルを提示
- Android：`View.context as Activity` または `FragmentActivity` から `AlertDialog` / `MaterialDatePicker` / `MaterialTimePicker` を起動する

**理由**:
- API がシンプル（利用者は提示管理を意識しない）
- 旧 AiForms と同じ UX
- 埋め込み方式は iOS の `responder chain` に乗るため、別途モーダル提示パスを通す必要がなく実装が単純

**代替案**:
- 利用者にモーダル提示責任を持たせる：API 拡張性は高いが、シンプルなケースが煩雑になる。Phase 6 で `KsCellInteractionContext` のような抽象を導入検討。

### Decision 5: Entry のフォーカス管理

**選択**: 利用者は `EntryCell.text` を `@Binding<String>` / `MutableState<String>` で受け取る。フォーカス遷移時・入力確定時の通知は内部で `text` Binding を逐次更新する（旧 AiForms の `ValueText: TwoWay` と同じ振る舞い）。`isFocused` プロパティは Phase 6 で検討。

**理由**:
- 利用者がリアルタイムバリデーションを書きやすい
- 性能的にも EntryCell の bind は通常 1 セルのみ frequent change なので問題なし
- TwoWay binding 規約とも整合する

### Decision 6: TwoWay binding の DSL 規約（本提案で新規規定）

**選択**: 入力系 Cell 5 種すべての DSL 拡張関数 / コンストラクタは、状態フィールドを TwoWay binding 引数で受け取る。

| Cell | iOS DSL 引数 | Android DSL 引数 |
|---|---|---|
| `EntryCell` | `text: Binding<String>` | `text: MutableState<String>` |
| `PickerCell`（`.single`） | `selectedIndex: Binding<Int?>` | `selectedIndex: MutableState<Int?>` |
| `PickerCell`（`.multiple`） | `selectedIndices: Binding<Set<Int>>` | `selectedIndices: MutableState<Set<Int>>` |
| `NumberPickerCell` | `value: Binding<Int>` | `value: MutableState<Int>` |
| `TimePickerCell` | `time: Binding<Date>` | `time: MutableState<LocalTime>` |
| `DatePickerCell` | `date: Binding<Date>` | `date: MutableState<LocalDate>` |

`Cell` 値型を直接構築する経路（Store 方式・外部から `Cell` 値を渡す場合）では、TwoWay binding ではなく **値 + `onValueChanged: ((T) -> Void)?` callback** を併設する。これにより以下の 2 系統が両立する：

- DSL 経路：`EntryCell(title: "ニックネーム", text: $userName)` のように直接 binding を渡す
- Store 経路：`EntryCell(title: "ニックネーム", text: currentValue, onTextChanged: { newText in store.replaceCell(...) })`

**理由**:
- `cell-types-basic` の `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は既に `isOn: $foo` / `isOn = state` の TwoWay binding 規約で先行確立されている。入力 Cell も同じ書き味に揃えることで一貫性を保つ
- 利用者にとって SwiftUI / Compose らしい宣言的記法になる
- DSL 経路と Store 経路の両方をサポートでき、power user の選択肢を温存

**代替案**:
- callback のみ：iOS / Android の現代的 UI フレームワーク慣習から外れる
- TwoWay のみ：Store 経路の利用者が冗長に書く必要が出る

### Decision 7: 共通フィールドの適用範囲（valueText の例外）

**選択**: 入力系 Cell 5 種は基本的に `cell-types-basic` の共通 Optional 6 フィールドをすべて持つが、`EntryCell` のみ **`valueText` を持たない**（右側 accessory が `UITextField` であり、`valueText` の表示位置と物理的に競合するため）。それ以外のピッカー系 4 種（PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）は `valueText` を持ち、利用者が明示指定しない場合は **現在の選択値を文字列化したもの** が `valueText` に自動表示される（旧 AiForms の `UseAutoValueText = true` 既定挙動の踏襲）。利用者が `valueText` を明示指定した場合はその値を優先する。

**理由**:
- `EntryCell` は入力中の値が `UITextField` / `EditText` 自身に表示されるため `valueText` を別途出すと冗長
- ピッカー系 4 種（PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）は「現在の選択値を行内に表示しつつ、タップでモーダル」が旧 AiForms と同じ UX
- 利用者が表示文言をカスタムしたいケース（例：通貨表記）にも `valueText` 明示指定で対応可能

### Decision 8: 各 Cell 固有プロパティの既定値

**選択**:

| Cell | プロパティ | 既定値 | 旧 AiForms との対応 |
|---|---|---|---|
| `EntryCell` | `keyboardType` | iOS `.default`（`UIKeyboardType`）/ Android `InputType.TYPE_CLASS_TEXT`（`Int`） | `Keyboard.Default` |
| `EntryCell` | `isPassword` | `false` | `IsPassword = false` |
| `EntryCell` | `textAlignment` | `.end` | `TextAlignment.End`（旧 AiForms `EntryCell.TextAlignmentProperty` の defaultValue） |
| `EntryCell` | `accentColor` | `nil` | `AccentColor = Default` |
| `EntryCell` | `maxLength` | `nil`（無制限） | `MaxLength = int.MaxValue` |
| `PickerCell` | `selectionMode` | `.single` | `SelectionMode = Single` |
| `PickerCell` | `maxSelectedNumber` | `0`（無制限、`.multiple` のみ有効） | `MaxSelectedNumber = 0` |
| `PickerCell` | `displayFormatter` | `nil`（そのまま表示） | `UseAutoValueText = true` |
| `PickerCell` | `accentColor` | `nil` | `AccentColor = Default` |
| `NumberPickerCell` | `min` / `max` / `step` | `0` / `100` / `1` | `Min = 0` / `Max = 100` |
| `NumberPickerCell` | `unit` | `""`（空文字 = suffix なし） | `Unit = ""`（旧 AiForms `NumberPickerCell.Unit`） |
| `TimePickerCell` | `time` 型 | iOS `Date` / Android `LocalTime` | `Time: TimeSpan = TimeSpan.Zero` |
| `TimePickerCell` | `format` | `"HH:mm"` | `Format = "t"`（24h 表示に近い解釈） |
| `DatePickerCell` | `date` 型 | iOS `Date` / Android `LocalDate` | `Date: DateTime = DateTime.Today` |
| `DatePickerCell` | `format` | `"yyyy/MM/dd"` | `Format = "d"`（日本式の short date） |
| `DatePickerCell` | `minDate` / `maxDate` | `nil`（無制限） | `MinimumDate = DateTime.MinValue` |
| `DatePickerCell` | `uiStyle` | iOS `DatePickerUIStyle.wheels` / Android `DatePickerUIStyle.Material` | iOS: 新規（旧版にはない）／ Android: `IsAndroidSpinnerStyle = false` 相当 |
| `DatePickerCell` | `todayText`（iOS のみ） | `nil`（非表示） | `TodayText = null`（旧 AiForms `DatePickerCell.TodayText`） |
| `DatePickerCell` | `androidButtonColor`（Android のみ） | `null` | `AndroidButtonColor = Default` |

`accentColor` の解決順序は基本 Cell と同じ `Cell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → プラットフォーム既定` の 4 段階。`titleColor` / `titleFont` は `cell-types-basic` で確立された 3 段階解決（`CellStyle → Theme → 既定`）に従う。

**理由**:
- 旧 AiForms 利用者にとって既定値が変わらないことが移行容易性を最優先する
- `format` 文字列は C# の `ToString(format)` と Swift の `DateFormatter.dateFormat` で互換性のある記法（`HH` / `mm` / `yyyy` / `MM` / `dd`）を採用し、Android `SimpleDateFormat` でもそのまま使えるようにする

### Decision 9: 共通行レイアウト関数の入力 Cell 対応

**選択**: 入力系 Cell も `cell-types-basic` で確立された共通行レイアウト関数 `applyCellBaseLayout(...)`（iOS）/ `applyCellBaseLayout(views, ...)`（Android）を経由してレイアウトを構成しなければならない。各 Cell View / ViewHolder は accessory slot に Cell 固有のコントロールを組み込むだけで、`title` / `description` / `valueText` / `icon` / `hintText` のレイアウトロジックを重複実装してはならない。

各 Cell の accessory slot 内容：

| Cell | accessory slot 内容 |
|---|---|
| `EntryCell` | `UITextField` / `EditText`（右側に入力欄を配置） |
| `PickerCell` | chevron（Disclosure Indicator） |
| `NumberPickerCell` | chevron |
| `TimePickerCell` | chevron |
| `DatePickerCell` | chevron |

ピッカー系 4 種（PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）の `valueText`（現在の選択値の文字列化）は共通行レイアウト関数の `valueText` slot に渡す（title 行の右寄せ位置に表示）。

**理由**:
- 基本 Cell との視覚的整合性（accessory 右端 X 座標の揃え、`hintText` の右上 float、`description` の縦並びレイアウト）
- レイアウトロジック重複の排除（保守性）
- `cell-types-basic` spec の「後続変更提案で追加される新規 Cell 種別も同じ共通行レイアウト関数を経由しなければならない (MUST)」要求を満たす

### Decision 10: 入力 Cell の VisibilityAware opt-in

**選択**: 入力系 Cell 5 種は `VisibilityAware` プロトコル / interface に opt-in 準拠する（基本 Cell 7 種と並列）。`isVisible: Bool`（既定 `true`）プロパティを保持し、`Hashable` / `equals` / `withDSLID` / `withStyle` / `data class copy()` 経路で `isVisible` を保持する。

**理由**:
- 基本 Cell との一貫性
- `Section.isVisible` / `Cell.isVisible` ベースの「(3) 可視性変化」経路（`settings-view-core` の表示状態同期三層分離 Requirement）が入力 Cell でも自然に動くようにする

### Decision 11: iOS DatePickerCell の 2 モード（Wheels / Calendar）

**選択**: iOS `DatePickerCell` に `uiStyle: DatePickerUIStyle`（`.wheels` / `.calendar`、既定 `.wheels`）を持たせ、利用者が UI 形式を選べるようにする：

- `DatePickerUIStyle.wheels`（既定）：AiForms 互換の埋め込みホイール。`UIDatePicker(.date) + .wheels` を `EmbeddedPickerHostField.inputView` 経由でキーボード位置にスライドアップ表示。Toolbar に `[Cancel] [pickerTitle?] [todayText?] [Done]`
- `DatePickerUIStyle.calendar`：iOS カレンダーアプリ風。`UIDatePicker(.date) + .inline` のカレンダー grid を `.pageSheet` + `.custom` detent (480pt) のシートで表示。シート下部に `[Cancel] [todayText?] [Done]` ボタンバー。`todayText` のタップは表示月と選択日をともに today にジャンプさせる（選択状態にかかわらず today のページへ）

**理由**:
- AiForms オリジナルからの移行ユーザーには `.wheels` を既定値で提供することで UX 互換性を保つ
- 一方、iOS 14+ で導入された `.inline` カレンダースタイル（iOS カレンダーアプリで使われる手法）は最近の iOS UX 慣習に近く、利用者が望むケースに応じて選べた方がよい
- `.calendar` モードを `.pageSheet` + `.custom` detent に乗せるのは Apple 公式 API のみで実現でき、ダークモード / accessibility / ジェスチャに自動対応する
- Android の `Material` / `Spinner` 切替と対称的なコンセプトとなり、利用者にとって学習コストが低い（Decision 12 参照）

**代替案**: Decision 3 の「代替案（DatePickerCell の calendar mode の表示形式）」セクション参照。

**Today ボタンの挙動詳細（Wheels モード）**:
- `todayText: String?` が非 nil かつ非空のとき Toolbar に「今日」相当のボタンを追加
- タップで `wheelsPicker.setDate(today, animated: true)`。**ただし** `wheelsPicker.date` が既に today と同日（年月日一致）の場合、UIKit が「変化なし」と判定して wheel が再描画されないため、`Calendar.date(byAdding: .second, value: 1, to: todayStart)` で 1 秒ずらしたダミー値を `animated: false` で先に setDate して強制差分を作る
- `minDate` / `maxDate` 範囲外の場合は何もしない。範囲チェックは **日単位** (`Calendar.startOfDay(for:)` 同士の比較) で行う：時刻成分まで含めた `Date` 同士の `<` / `>` 比較を行うと、`maxDate: Date()` 指定時に「ボタン押下時の `Date()`」が「render() で固定した maxDate」より僅かに後の時刻になり毎回 abort される罠を踏むため
- Done で確定 → year/month/day のみ取り出して元 `cell.date` の hour/minute/second と合体して `onValueChanged` に渡す
- Cancel で `preSelectedDate` に戻して resignFirstResponder

**Today ボタンの挙動詳細（Calendar モード）**:
- 仕様（ユーザー確認済）: 「選択状態にかかわらず今日のページに移動すべき」。表示月と選択日をともに today にジャンプさせる
- `.inline` カレンダーで `datePicker.setDate(today, animated: true)` を呼ぶが、既に同日選択中の場合は Wheels モードと同じく `Calendar.date(byAdding: .second, value: 1, to: todayStart)` のダミー値で強制差分を作ったうえで today に setDate する（UIKit の「変化なし」最適化を回避）
- 範囲チェックも Wheels モードと同様に **日単位** (`Calendar.startOfDay(for:)` 同士の比較) で行う

**Calendar モードのシート再提示**（Done / Cancel / スワイプダウンで閉じた後にもう一度 Cell をタップしたときの挙動）:
- `currentCalendarController` は `weak` 参照ではなく `strong` 参照で保持し、シート dismiss 完了 callback (`dismiss(animated:completion:)` の completion、および `UIAdaptivePresentationControllerDelegate.presentationControllerDidDismiss`) で明示的に `nil` 化する
- `weak` 参照だと UIKit の保持タイミング次第で deinit が遅延し、`if currentCalendarController != nil { return }` の多重提示ガードに弾かれて再タップが効かない不具合があった（初版実装はこの問題を抱えていた）

### Decision 12: クロスプラットフォーム DatePickerUIStyle 命名規約

**選択**: `DatePickerCell.uiStyle: DatePickerUIStyle` は iOS / Android 両方で **同じプロパティ名 (`uiStyle`) · 同じ型名 (`DatePickerUIStyle`)** を採用するが、enum のケースはプラットフォーム固有 UI を反映する：

| プラットフォーム | enum ケース | 既定値 |
|---|---|---|
| iOS | `DatePickerUIStyle.wheels` / `.calendar` | `.wheels` |
| Android | `DatePickerUIStyle.Material` / `.Spinner` | `.Material` |

**理由**:
- 利用者が両プラットフォームで対称な API として扱える（プロパティ名・型名の対称性）
- 各プラットフォームの UI 慣習に合致したケース名を選べる（iOS の "wheels" / "calendar" と Android の "Material" / "Spinner" は本質的に別の選択肢のため、無理に統一すると不自然になる）
- 既定値は両方とも「旧 AiForms 互換 UI」を選択（iOS は `.wheels` = ホイール埋め込み、Android は `.Material` = `MaterialDatePicker`）。`IsAndroidSpinnerStyle = false` を既定としていた AiForms 流に追従
- `DatePickerAndroidStyle`（古い名前、Android 限定であることをプレフィックスで強調）から `DatePickerUIStyle` への rename は本提案で初めて実施。クロスプラットフォーム API の対称性のためにあえて型名衝突を許容する（同じプロジェクト内でも iOS コードと Android コードは異なるモジュール・異なるソースセットに配置されるため衝突は起きない）

**代替案**:
- プロパティ名と型名を完全にプラットフォーム別にする（iOS `iosUiStyle: DatePickerIOSStyle`、Android `androidUiStyle: DatePickerAndroidStyle`）：型名から「Android 限定」「iOS 限定」が一目で分かるが、両プラットフォームを跨いだ API 学習コストが上がり、クロスプラットフォーム MAUI バインディングを書くときも煩雑になる
- 4 ケースをひとつの enum にまとめる（`.wheels / .calendar / .material / .spinner`、各プラットフォームで非対応値は fallback）：プラットフォーム間違いで指定したケースが silent fallback されると debug 困難。型システムで防げないリスクがあり却下
- **採用案**: 型名・プロパティ名は対称、enum ケースはプラットフォーム固有。利用者は iOS では `.wheels` / `.calendar`、Android では `.Material` / `.Spinner` を書くだけで、両プラットフォームの UI 選択を統一的に意識できる

## Risks / Trade-offs

- **リスク**: 各プラットフォームのモーダル提示が `keyWindow` / `Activity` に依存し、組み込み環境（マルチウィンドウ、Compose Navigation のネスト）で問題が起きる
  - **緩和策**: `KsCellInteractionContext` のような抽象（present / dismiss を行うインターフェース）を Phase 6 で導入検討。本変更提案では「最も一般的な単一 Activity / 単一 Window 構成」を前提とする。
- **リスク**: `MaterialDatePicker` / `MaterialTimePicker` は `androidx.fragment` 依存
  - **緩和策**: `ks-settingsview-ui` は AppCompat / Material 依存をすでに持つため許容。`build.gradle.kts` に `androidx.fragment:fragment-ktx` を追加する。
- **リスク**: Android `java.time.LocalTime` / `LocalDate` は API 26+ のため、`minSdk` がそれ未満の場合は `desugar_jdk_libs` 設定が必要
  - **緩和策**: `ks-settingsview-ui/build.gradle.kts` で `compileOptions { coreLibraryDesugaringEnabled = true }` および `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:...")` を設定。tasks.md にも記載する。
- **リスク**: iOS `Foundation.Date` は時刻（hour:minute）と日付（year/month/day）の両方を `Date` 1 つで持つため、`TimePickerCell.time` と `DatePickerCell.date` で「時刻成分」「日付成分」のどちらだけが意味を持つかが API シグネチャから読み取りづらい
  - **緩和策**: 各 Cell の Requirement と `docs/cell-types-input.md` で「`TimePickerCell` は `Date` の `hour` / `minute` のみを参照する」「`DatePickerCell` は `Date` の `year` / `month` / `day` のみを参照する」を明文化。内部実装は `Calendar.current.dateComponents([.hour, .minute], from: date)` などで成分を取り出す
- **リスク**: `PickerCell` の `.multiple` モードで `maxSelectedNumber` 制限を超えてユーザーがチェックを増やしたときの UX が不明瞭
  - **緩和策**: 上限到達時は新規チェックを無視し（既選択は維持）、軽い触覚フィードバック（iOS は `UIImpactFeedbackGenerator(style: .light)`、Android は `view.performHapticFeedback(...)`）で気づきを与える。spec.md の Scenario に明記する。
- **リスク**: `EntryCell` の `TextWatcher`（Android）/ `editingChanged` イベント（iOS）が `MutableState` / `Binding` 経由で TwoWay 反映されると、Recomposition / body 再評価がループする可能性
  - **緩和策**: ViewHolder / Cell View 内で「自身が送出した変更による再描画」をガードする（古い値と新しい値を比較し、同値なら更新スキップ）。`bind` 時に `TextWatcher` を一旦解除してから `EditText.setText(...)` し、再度追加する（既存 `SwitchCell` 等で確立されたパターン）。
- **リスク**: `valueText` 自動表示（Decision 7）と利用者明示指定の優先順位が分かりにくい
  - **緩和策**: spec.md の Scenario で「`valueText` が `nil` のときは選択値文字列化を表示、明示指定時はその値を表示」を明文化する。`docs/cell-types-input.md` でも例示する。

## Open Questions

- ピッカー系 4 種で「選択値の文字列化」を行う際、`displayFormatter: ((T) -> String)?` を全 Cell に持たせるか、`PickerCell` のみに持たせるか
  - 現提案では `PickerCell` のみに `displayFormatter: ((String) -> String)?` を持たせ、`NumberPickerCell` / `TimePickerCell` / `DatePickerCell` は `format: String?`（DateFormatter / DateTimeFormatter 形式文字列）で代替する方針とする
  - `NumberPickerCell` は単純な `Int → String` 変換のみのため `displayFormatter` を持たない（必要なら利用者が `valueText` 明示指定で対応）
- `PickerCell(.multiple)` の `valueText` 自動表示はどうする？
  - 現提案では「選択された項目を `, ` で連結した文字列」を自動表示する方針（例: `selectedIndices = {0, 2}` / `items = ["A", "B", "C"]` → `"A, C"`）。長すぎる場合は ellipsize end。利用者が `valueText` 明示指定すれば優先される。spec の Scenario で明示する。
- `EntryCell` の `maxLength`（旧 AiForms の `MaxLength: int`）について
  - 本提案で **含める**（`maxLength: Int?`、既定 `nil` = 無制限）。iOS は `UITextField.delegate` の `shouldChangeCharactersIn` で範囲チェック、Android は `EditText.filters = arrayOf(InputFilter.LengthFilter(maxLength))` で実装する。利用者が `text` Binding の onChange でも追加バリデーションを書ける（補完関係）
- iOS の `UITextField` キャレット色（`tintColor`）と Android `EditText` キャレット色（`textCursorDrawable`）の制御を `accentColor` で統一できるか
  - iOS は `tintColor` で簡単。Android は `textCursorDrawable` を runtime に色変えするには API 29+ が必要。最低 API ライン（`ks-settingsview-ui` の `minSdk`）を確認した上で実装時に決定。
- `DatePickerCell(uiStyle: .Spinner)`（Android）でも `minDate` / `maxDate` を反映できるか
  - `android.widget.DatePicker` には `setMinDate(Long)` / `setMaxDate(Long)`（epoch milliseconds）があるため反映可能。実装時に `LocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()` で変換する。
