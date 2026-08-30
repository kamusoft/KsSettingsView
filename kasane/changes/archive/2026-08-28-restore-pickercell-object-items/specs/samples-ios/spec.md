# samples-ios — デルタスペック (restore-pickercell-object-items)

## ADDED Requirements

### Requirement: PickerCell の object 候補デモ (iOS)

iOS sample は、object 候補 (主表示 + 副表示の射影) と選択結果の object 受け取りを示す PickerCell デモを含む SHALL。

#### Scenario: object 候補の選択
- **GIVEN** sample の PickerCell object デモ
- **WHEN** 行をタップして副表示付き候補を選択する
- **THEN** 選択面に主表示と副表示が表示され、確定すると行の値表示が選択項目に更新される
