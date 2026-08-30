# maui-bridge — デルタスペック (restore-pickercell-object-items)

## MODIFIED Requirements

### Requirement: PickerCell の輸送

snapshot / bridge DTO は PickerCell の候補を、候補1件 = 主表示 + 任意の副表示を持つ1要素として運ぶ SHALL (表示射影は facade が適用済みで、native 側は射影を再解決しない)。副表示の有無は輸送で保存される SHALL (空文字列の副表示は facade 側で「なし」へ正規化済み)。選択状態は index (`SelectedIndex` / `SelectedIndices`) で運び、ユーザー操作の選択通知は従来どおり index で facade へ戻る SHALL。

#### Scenario: 副表示の有無が往復で保存される
- **GIVEN** 副表示の有る候補と無い候補が混在する PickerCell
- **WHEN** facade から native へ輸送して選択面を開く
- **THEN** 副表示の有る候補だけが native 側でも副表示を持つ

#### Scenario: 副表示付き候補の輸送
- **GIVEN** 副表示を持つ候補列を輸送した PickerCell
- **WHEN** native の選択面を開く
- **THEN** 候補行に主表示と副表示が表示される

#### Scenario: 選択通知は index で戻る
- **GIVEN** 表示中の PickerCell
- **WHEN** 選択面で候補を確定する
- **THEN** facade には選択 index (単一) / index 集合 (複数) が通知される
