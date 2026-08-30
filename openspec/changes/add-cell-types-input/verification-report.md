# Verification Report: add-cell-types-input (iOS 側 限定検証)

**検証日時**: 2026-06-14
**検証対象**: iOS (KsSettingsViewUI Swift) 実装のみ
**対象外**: Android / Compose DSL / Sample / docs（意図的未着手）

---

## Summary

| Dimension    | Status                                                   |
|--------------|----------------------------------------------------------|
| Completeness | iOS タスク 25/25 完了。Android/Compose/Sample/docs は対象外 |
| Correctness  | iOS MUST 要件すべて充足。テスト 39 件実装                |
| Coherence    | design.md の全 Decision に適合                           |

---

## 検証スコープ

以下の iOS タスクが完了マーク済みであることを確認した（tasks.md の `[x]`）：

| タスク ID | 内容 |
|---|---|
| 1.1 | `PickerSelectionMode.swift` 追加（`public enum PickerSelectionMode: Hashable` 2 ケース） |
| 1.4 | `PickerSelectionMode` ユニットテスト |
| 2.1 | `EntryCell.swift` 実装 |
| 2.2 | `EntryCellView.swift` 実装 |
| 2.3 | `maxLength` 制限（`UITextField.delegate` 経由） |
| 2.4 | DSL 経路 `Binding<String>` init |
| 2.5 | EntryCell テスト群 |
| 3.1 | `PickerCell.swift` 実装（単一/複数両対応） |
| 3.2 | `PickerCellView.swift` 実装 |
| 3.3 | `PickerListViewController.swift` 実装 |
| 3.4 | PickerCell テスト群 |
| 3.5 | TextPickerCell は実装しない（対象外として確認済み） |
| 4.1 | `NumberPickerCell.swift` 実装 |
| 4.2 | `NumberPickerCellView.swift` 実装 |
| 4.3 | NumberPickerCell テスト群 |
| 5.1 | `TimePickerCell.swift` 実装 |
| 5.2 | `TimePickerCellView.swift` 実装 |
| 5.3 | TimePickerCell テスト群 |
| 6.1 | `DatePickerCell.swift` 実装 |
| 6.2 | `DatePickerCellView.swift` 実装 |
| 6.3 | DatePickerCell テスト群 |
| 6.4 | iOS に `androidUiStyle`/`androidButtonColor` 引数なし |
| 7.1 | `KsCellRegistry+InputCells.swift` 実装 |
| 7.2 | `KsSettingsViewController.init` での auto-register（オプトアウト対応） |
| 16.1 | xcodebuild test 313 件全成功 |

---

## Completeness（完全性）

### iOS タスク完了状況

iOS スコープのタスク（セクション 1.1/1.4/2/3/4/5/6/7/16.1）はすべて `[x]` 完了。

### 対象外タスク（意図的未着手、INVALID の理由にしない）

- タスク 0.4、1.2、1.3、8.x、9.x、10.x、11.x、12.x、13.x、14.x、15.x、16.2

### Spec Coverage（iOS MUST 要件）

spec.md から iOS に関係する MUST 要件を抽出し実装と突き合わせた結果：

| 要件 | 実装ファイル | 充足状況 |
|---|---|---|
| 5 種すべて `VisibilityAware` 準拠 | 各 Cell struct | OK（全 5 Cell `VisibilityAware` 準拠を型として確認） |
| `EntryCell` は `valueText` を持たない（MUST NOT） | `EntryCell.swift` | OK（フィールド未定義） |
| 4 種は `valueText` を持つ | `PickerCell/NumberPickerCell/TimePickerCell/DatePickerCell` | OK |
| `keyboardType: UIKeyboardType` を `UITextField.keyboardType` に直接代入 | `EntryCellView.swift:62` | OK |
| `isPassword` で `isSecureTextEntry` 切替 | `EntryCellView.swift:63` | OK |
| `accentColor` を `tintColor` に反映 | `EntryCellView.swift:71-75` | OK |
| `maxLength` 非 nil 時 `shouldChangeCharactersIn` で拒否 | `EntryCellView.swift:136-147` | OK |
| `maxLength = nil` で無制限 | `EntryCellView.swift:141` | OK（`guard let maxLength` 前提） |
| `id: UUID = UUID()` デフォルト値（全 5 Cell） | 各 Cell struct init | OK |
| `withDSLID` / `withStyle` / `withIcon` で `isVisible` 保持 | 各 Cell struct | OK |
| 共通行レイアウト関数 `applyCellBaseLayout(...)` 経由 | 各 CellView | OK（全 5 Cell View で確認） |
| `EntryCell.isEnabled = false` で `UITextField.isEnabled = false` | `EntryCellView.swift:65` | OK |
| isEnabled 色置換（disabledTextColor） | `EntryCellView.swift:67` | OK |
| `alpha` 半透明化は行わない | 各 CellView 実装 | OK（実装上 alpha 操作なし） |
| `PickerSelectionMode: Hashable` | `PickerSelectionMode.swift:18` | OK |
| `.single` は選択即時 dismiss | `PickerListViewController.swift:128` | OK |
| `.multiple` は「完了」ボタン（`barButtonSystemItem: .done`）で dismiss | `PickerListViewController.swift:93-97` | OK |
| `.multiple` で上限到達時は新規チェック無視 + 触覚フィードバック | `PickerListViewController.swift:141-146` | OK |
| `valueText = nil` のとき選択値を自動表示 | `PickerCell.effectiveValueText()` | OK |
| `.single` 自動 valueText: `items[selectedIndex]` を `displayFormatter` 経由 | `PickerCell.swift:287-290` | OK |
| `.multiple` 自動 valueText: `, ` 連結 | `PickerCell.swift:291-296` | OK |
| `NumberPickerCell`: `min=0/max=100/step=1` 既定値 | `NumberPickerCell.swift:45-47` | OK |
| `NumberPickerCell` タップで `UIPickerView` 内蔵モーダル | `NumberPickerCellView.swift:66-78` | OK（`NumberPickerModalController` UIPickerView ベース） |
| `TimePickerCell.time: Binding<Date>` Native `Foundation.Date` 直接 | `TimePickerCell.swift:81` | OK |
| `TimePickerCell.format` 既定値 `"HH:mm"` | `TimePickerCell.swift:48` | OK |
| `TimePickerCell`: hour/minute 成分のみ参照 | `TimePickerCellView.swift:73-82` | OK |
| `TimePickerCell` タップで `UIDatePicker(.time)` モーダル | `TimePickerCellView.swift:63` | OK（`DatePickerModalController(mode: .time)`） |
| `DatePickerCell.date: Binding<Date>` Native `Foundation.Date` 直接 | `DatePickerCell.swift:83` | OK |
| `DatePickerCell.format` 既定値 `"yyyy/MM/dd"` | `DatePickerCell.swift:47` | OK |
| `DatePickerCell.minDate/maxDate` → `UIDatePicker.minimumDate/maximumDate` | `DatePickerCellView.swift:63-70`, `DatePickerModalController.swift:151-152` | OK |
| iOS に `androidUiStyle`/`androidButtonColor` 引数なし（MUST NOT） | `DatePickerCell.swift` | OK（フィールド未定義） |
| `registerInputCells()` extension で 5 種を登録 | `KsCellRegistry+InputCells.swift` | OK |
| `KsSettingsViewController.init` で auto-register（オプトアウト可） | `KsSettingsViewController.swift:140/168` | OK（`autoRegisterInputCells: Bool = true` 引数） |
| DSL 経路 `Binding<String/Int/Int?/Set<Int>/Date>` init（全 5 Cell） | 各 Cell DSL init | OK |
| Store 経路 callback（`onTextChanged`/`onSelectionChanged`/`onMultiSelectionChanged`/`onValueChanged`） | 各 Cell Store init | OK |
| `DateFormatter` キャッシュ（`CachedDateFormatter`）| `TimePickerCell.swift:104`、`DatePickerCell.swift:107` | OK（`CachedDateFormatter.swift` 確認済み） |
| `CellStyle` は `style: CellStyle = CellStyle()` で全 Cell が保持 | 各 Cell struct | OK |

---

## Correctness（正確性）

### Scenario カバレッジ（iOS スコープのみ）

| Scenario | テストメソッド | 充足状況 |
|---|---|---|
| id 引数省略で UUID 自動採番 | `test_入力系Cell5種_idデフォルトUUID自動採番` | OK |
| EntryCell は valueText を持たない（コンパイルエラー） | 型定義で保証（実行時テスト不要） | OK |
| TwoWay 入力反映（callback 経由） | `test_EntryCellView_TwoWay入力でcallbackが呼ばれる` | OK |
| keyboardType Native 型反映 | `test_EntryCellView_keyboardTypeがNative型で直接反映される` | OK |
| isPassword マスク | `test_EntryCellView_isPasswordでisSecureTextEntryが切り替わる` | OK |
| accentColor tintColor 反映 | `test_EntryCellView_accentColorがtintColorに反映される` | OK |
| maxLength 制限（境界値） | `test_EntryCellView_maxLength超過時はshouldChangeで拒否` | OK |
| maxLength = nil 無制限 | `test_EntryCellView_maxLengthがnilのときは無制限` | OK |
| isEnabled = false で disabled | `test_EntryCellView_isEnabledFalseでUITextFieldがdisabled` | OK |
| prepareForReuse でリセット | `test_EntryCellView_prepareForReuseで状態クリア` | OK |
| VisibilityAware 準拠（5 種一括） | `test_入力系Cell5種_VisibilityAware準拠` | OK |
| 単一選択 effectiveValueText | `test_PickerCell_effectiveValueText_単一_displayFormatterなし` | OK |
| 複数選択 effectiveValueText カンマ連結 | `test_PickerCell_effectiveValueText_複数_カンマ連結` | OK |
| displayFormatter 加工 | `test_PickerCell_effectiveValueText_単一_displayFormatter` | OK |
| valueText 明示指定が優先 | `test_PickerCell_effectiveValueText_明示指定が優先` | OK |
| 単一選択モーダルで選択確定 | `test_PickerListViewController_単一選択` | OK |
| 複数選択モーダルで「完了」押下確定 | `test_PickerListViewController_複数選択_完了で確定` | OK |
| maxSelectedNumber 上限超過無視 | `test_PickerListViewController_複数選択_maxSelectedNumber上限超過は無視` | OK |
| NumberPickerCell 既定値 | `test_NumberPickerCell_既定値` | OK |
| NumberPickerCell 値変更通知 | `test_NumberPickerModalController_doneで選択値が通知される` | OK |
| TimePickerCell 既定 format | `test_TimePickerCell_既定format` | OK |
| TimePickerCell effectiveValueText format 反映 | `test_TimePickerCell_effectiveValueText_format反映` | OK |
| DatePickerCell 既定 format・範囲制限デフォルト nil | `test_DatePickerCell_既定format` | OK |
| DatePickerCell effectiveValueText | `test_DatePickerCell_effectiveValueText_format反映` | OK |
| DatePickerModalController 日付変更通知 | `test_DatePickerModalController_doneで日付通知` | OK |
| registerInputCells 5 種登録 | `test_registerInputCells_5種が登録される` | OK |

### 未充足のシナリオ（iOS スコープ内）

以下の Scenario は spec に明示されているが、`InputCellsTests.swift` の 39 テストには専用のテストメソッドが存在しない。ただし、実装上は対応するロジックが存在するため動作的には充足している。

- **「共通行レイアウト関数経由の描画」のシナリオ**: 各 CellView で `applyCellBaseLayout(...)` を直接呼んでいることはコード上明白だが、テストで `applyCellBaseLayout` の呼び出し自体を assert するケースはない（`render` 後の内容確認で間接的に検証）。
- **「Theme.cellTitleColor の 3 段階解決」のシナリオ**: `EffectiveStyle` が theme フォールバックを担う実装であり、基本 Cell 系テストで既に検証済みの仕組みを踏襲。入力 Cell 固有のテストは存在しない。
- **「isEnabled = false での色置換（alpha なし）」のシナリオ**: `textColor = disabledTextColor` で色置換していることはコード上確認済み。alpha 操作がないことの explicit なテストはない。

これらは実装コードで充足されており、テストが不在でも CRITICAL 相当の問題はない。

---

## Coherence（設計整合性）

| design.md Decision | 実装の対応 | 充足状況 |
|---|---|---|
| Decision 1: Native 型直接公開 | `UIKeyboardType`/`Foundation.Date` を直接フィールドに。独自型なし | OK |
| Decision 1 例外: `PickerSelectionMode` UI 層独自型 | `PickerSelectionMode.swift` で UI 層に配置 | OK |
| Decision 2: TextPickerCell は移植対象外 | 実装ファイルなし、タスク 3.5 確認 | OK |
| Decision 3: モーダル方式（UITableViewController + UINavigationController） | `PickerListViewController` + `UINavigationController` でモーダル | OK |
| Decision 4: `keyWindow.rootViewController` からモーダル起動 | `keyWindowRootViewController()` helper 経由 | OK |
| Decision 5: TwoWay binding は `onTextChanged` で逐次更新 | `editingChanged` target で毎入力 callback | OK |
| Decision 6: 全 5 Cell に `Binding<T>` DSL init + callback Store init | 各 Cell で確認 | OK |
| Decision 7: EntryCell は `valueText` なし、ピッカー 4 種は自動表示 | コード確認済み | OK |
| Decision 8: 既定値（`keyboardType: .default`/`format: "HH:mm"`/`min=0/max=100/step=1`/`format: "yyyy/MM/dd"`） | 各 Cell init 確認 | OK |
| Decision 9: `applyCellBaseLayout(...)` 経由 | 全 5 CellView で確認 | OK |
| Decision 10: 全 5 Cell `VisibilityAware` opt-in | 全 5 Cell struct で `VisibilityAware` 準拠 | OK |

### Minor: `DatePickerCell` の year/month/day のみ参照について

spec では「`Date` の year/month/day 成分のみを参照（MUST）」とあるが、`DatePickerCellView` のコールバック実装（:75-85）は year/month/day を取り出して元の hour/minute/second と合成した Date を返している。これは spec の意図（DatePickerCell は日付成分のみを扱う = 時刻成分は変えない）に正確に従った実装であり、問題はない。

---

## Issues 一覧

### CRITICAL（なし）

iOS スコープ内で CRITICAL 相当の問題は発見されなかった。

### WARNING（なし）

### SUGGESTION

1. **共通行レイアウト関数呼び出しの明示的テストがない**
   - 対象: `InputCellsTests.swift`
   - 詳細: `applyCellBaseLayout` の呼び出し自体を assert するテストが存在しない。`render` 後の cell title/description 表示確認で間接的には担保されている。
   - 推奨: `TimePickerCellView` や `DatePickerCellView` の render テストで `contentConfiguration` の title/description を検査するケースを追加することで直接担保できる。

2. **isEnabled = false 時の「色置換のみ・alpha 操作なし」の explicit テスト不在**
   - 対象: `InputCellsTests.swift`
   - 詳細: `isEnabled = false` で `UITextField.isEnabled == false` のテストはあるが、`textColor == disabledTextColor` および `alpha` が 1.0 のままであることの assert がない。
   - 推奨: `test_EntryCellView_isEnabledFalseでdisabledTextColorに色置換()` テストを追加。

---

## Final Assessment

**iOS スコープの CRITICAL 問題: なし。WARNING 問題: なし。**

iOS 側の spec MUST 要件はすべて充足されており、39 件のテストが tasks.md に記録されたユニットテスト要件を網羅している。2 件の SUGGESTION（共通行レイアウト関数の明示的テスト、色置換の explicit テスト）は任意改善項目であり、アーカイブを妨げない。

**判定: VALID**

---

## 対象外範囲の明示

本検証で意図的に除外したスコープ：

| 範囲 | 理由 |
|---|---|
| Android (`ks-settingsview-ui` Kotlin) | 段階的実装方式により未着手（tasks.md で `[ ]` のまま） |
| Compose DSL (`ks-settingsview-compose`) | 同上 |
| Sample アプリ (`samples/ios/`, `samples/android/`) | 同上 |
| docs (`docs/cell-types-input.md` 等) | 同上 |

これらの未実装は本検証の INVALID 判定要因にしない。
