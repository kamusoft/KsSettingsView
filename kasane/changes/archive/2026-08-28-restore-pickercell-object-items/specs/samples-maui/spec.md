# samples-maui — デルタスペック (restore-pickercell-object-items)

## ADDED Requirements

### Requirement: PickerCell の object 候補デモ (MAUI)

MAUI sample は、object の `ItemsSource` に `DisplayMember` / `SubDisplayMember` を指定し、`SelectedItem` / `SelectedItems` の TwoWay バインドを示す PickerCell デモを含む SHALL。

#### Scenario: object 候補の選択
- **GIVEN** sample の PickerCell object デモ
- **WHEN** 行をタップして副表示付き候補を選択する
- **THEN** 選択面に主表示と副表示が表示され、確定すると行の値表示と ViewModel 側の選択項目が更新される
