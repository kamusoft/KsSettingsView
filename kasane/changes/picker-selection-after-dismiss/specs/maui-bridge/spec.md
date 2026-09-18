## ADDED Requirements

### Requirement: PickerCell の閉じ切り通知を interaction 経路で中継する
Bridge は PickerCell の閉じ切り通知を interaction delegate / listener の専用メソッド (`pickerCellSelectionCompleted(cellID, index)` / `pickerCellMultiSelectionCompleted(cellID, indices)`) で通知する SHALL。通知は既存の確定通知 (`pickerCellSelectionChanged` / `pickerCellMultiSelectionChanged`) の後に届き、同じ cellID と同じ選択を運ぶ SHALL。複数選択の indices は既存の確定通知と同じ正規化 (昇順・重複なし) で渡す SHALL。両 OS の Bridge で同じ意味・同じ名前である SHALL。

#### Scenario: 単一選択の閉じ切りが cellID と index 付きで届く
- **GIVEN** Bridge 経由で構築した単一選択の PickerCell に interaction delegate / listener が設定されている
- **WHEN** Native の選択面で候補を確定し、選択面が閉じ切る
- **THEN** `pickerCellSelectionChanged(cellID, index)` の後に `pickerCellSelectionCompleted(cellID, index)` が同じ cellID と index で 1 回届く

#### Scenario: 複数選択の閉じ切りが正規化された indices 付きで届く
- **GIVEN** Bridge 経由で構築した複数選択の PickerCell に interaction delegate / listener が設定されている
- **WHEN** Native の選択面で確定操作を行い、選択面が閉じ切る
- **THEN** `pickerCellMultiSelectionChanged(cellID, indices)` の後に `pickerCellMultiSelectionCompleted(cellID, indices)` が同じ cellID と昇順・重複なしの indices で 1 回届く

#### Scenario: 破棄後は閉じ切り通知も届かない
- **GIVEN** Bridge を破棄した (interaction delegate / listener が解除されている)
- **WHEN** 破棄前に開いていた選択面が確定操作で閉じ切る
- **THEN** `pickerCellSelectionCompleted` / `pickerCellMultiSelectionCompleted` は届かない

### Requirement: DatePickerCell の閉じ切り通知は interaction 経路に含めない
Bridge は DatePickerCell の閉じ切り通知 (Native の `onValueCompleted`) を interaction delegate / listener へ中継しない SHALL。DatePickerCell の interaction 通知は従来の `datePickerCellChanged` のみである SHALL。

#### Scenario: DatePicker を確定して閉じても追加の通知は届かない
- **GIVEN** Bridge 経由で構築した DatePickerCell に interaction delegate / listener が設定されている
- **WHEN** Native の選択面で日付を確定し、選択面が閉じ切る
- **THEN** 届く通知は `datePickerCellChanged(cellID, date)` の 1 回だけである
