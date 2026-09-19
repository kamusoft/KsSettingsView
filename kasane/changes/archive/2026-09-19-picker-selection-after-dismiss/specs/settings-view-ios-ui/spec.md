## ADDED Requirements

### Requirement: PickerCell 単一選択の閉じ切り通知 (iOS)
`PickerCell` は単一選択の閉じ切り callback (`onSelectionCompleted`) を持てる SHALL。候補タップで確定したとき、選択面が閉じ切った後に確定した index を引数として 1 回だけ発火する SHALL。発火は値の callback (`onSelectionChanged`) の後である SHALL。非確定 dismiss (Cancel・ページシート標準の dismiss 操作) ではどの callback も発火しない SHALL。callback は省略可能で、既存の呼び出しは変更なしに動く SHALL。

#### Scenario: 候補タップで値 callback の後に閉じ切り callback が届く
- **GIVEN** `onSelectionChanged` と `onSelectionCompleted` を持つ単一選択の PickerCell の選択面が開いている
- **WHEN** 候補をタップし、選択面が閉じ切る
- **THEN** `onSelectionChanged(index)` が先に、`onSelectionCompleted(index)` が選択面の閉じ切り後に、それぞれ 1 回ずつ同じ index で発火する

#### Scenario: Cancel では閉じ切り callback が発火しない
- **GIVEN** `onSelectionCompleted` を持つ単一選択の PickerCell の選択面が開いている
- **WHEN** Cancel で閉じる
- **THEN** `onSelectionChanged` も `onSelectionCompleted` も発火しない

#### Scenario: 対話的 dismiss では閉じ切り callback が発火しない
- **GIVEN** `onSelectionCompleted` を持つ単一選択の PickerCell の選択面が開いている
- **WHEN** ページシート標準の dismiss 操作で閉じる
- **THEN** `onSelectionCompleted` は発火しない

#### Scenario: callback を渡さない既存の呼び出しはそのまま動く
- **GIVEN** `onSelectionCompleted` を指定せずに構築した単一選択の PickerCell
- **WHEN** 候補をタップして確定する
- **THEN** `onSelectionChanged` は従来どおり発火し、エラーや追加の副作用は無い

### Requirement: PickerCell 複数選択の閉じ切り通知 (iOS)
`PickerCell` は複数選択の閉じ切り callback (`onMultiSelectionCompleted`) を持てる SHALL。確定操作で確定したとき、選択面が閉じ切った後に確定した index 集合を引数として 1 回だけ発火する SHALL。発火は値の callback (`onMultiSelectionChanged`) の後で、同じ集合を運ぶ SHALL。候補のトグルでは発火しない SHALL。非確定 dismiss ではどの callback も発火しない SHALL。

#### Scenario: 確定操作で値 callback の後に閉じ切り callback が届く
- **GIVEN** 複数選択の PickerCell の選択面で候補をいくつかトグルしている
- **WHEN** 確定操作を行い、選択面が閉じ切る
- **THEN** `onMultiSelectionChanged(集合)` が先に、`onMultiSelectionCompleted(集合)` が閉じ切り後に、同じ集合で 1 回ずつ発火する

#### Scenario: 候補のトグルだけでは閉じ切り callback が発火しない
- **GIVEN** 複数選択の PickerCell の選択面が開いている
- **WHEN** 候補をトグルする (確定操作はしない)
- **THEN** `onMultiSelectionCompleted` は発火しない

#### Scenario: Cancel では閉じ切り callback が発火しない
- **GIVEN** 複数選択の PickerCell の選択面で候補をトグルしている
- **WHEN** Cancel で閉じる
- **THEN** `onMultiSelectionChanged` も `onMultiSelectionCompleted` も発火しない

### Requirement: PickerCell の閉じ切り callback はすべての構築経路と再構築で保持される (iOS)
単一 / 複数のそれぞれについて、値を直接渡す構築経路、`Binding` を渡す構築経路、object 候補を射影する構築経路 (ジェネリック init と String 特殊化) のすべてで閉じ切り callback を指定できる SHALL。Cell の派生 (`with*` 系の再構築) を経ても閉じ切り callback は保持される SHALL。

#### Scenario: Binding 経路で閉じ切り callback を指定できる
- **GIVEN** `selectedIndex: Binding<Int?>` と `onSelectionCompleted` を渡して構築した PickerCell
- **WHEN** 候補をタップして選択面が閉じ切る
- **THEN** Binding の値が更新され、その後に `onSelectionCompleted(index)` が発火する

#### Scenario: object 射影経路で閉じ切り callback を指定できる
- **GIVEN** object の候補列と射影 closure、`onSelectionCompleted` を渡して構築した PickerCell
- **WHEN** 候補をタップして選択面が閉じ切る
- **THEN** 射影された値の書き戻しの後に `onSelectionCompleted(index)` が発火する

#### Scenario: 再構築後も閉じ切り callback が残る
- **GIVEN** `onSelectionCompleted` を持つ PickerCell
- **WHEN** `with*` 系の再構築で派生した Cell を使って選択面を開き、候補をタップして閉じ切る
- **THEN** 元の `onSelectionCompleted` が発火する

### Requirement: DatePickerCell の閉じ切り通知 (iOS)
`DatePickerCell` は閉じ切り callback (`onValueCompleted`) を持てる SHALL。確定操作 (Done) で確定したとき、選択面 (カレンダー / ホイールいずれの形式でも) が閉じ切った後に確定した日付を引数として 1 回だけ発火する SHALL。発火は値の callback (`onValueChanged`) の後で、同じ日付を運ぶ SHALL。uiStyle (カレンダー / ホイール) に関係なく同じ契約である SHALL。非確定 dismiss (Cancel・シート標準の dismiss 操作) ではどの callback も発火しない SHALL。値を渡す構築経路と `Binding` を渡す構築経路の両方で指定でき、派生 (`withDSLID` / `withStyle` / `withIcon`) を経ても保持される SHALL。既存の呼び出しは変更なしに動く SHALL。

#### Scenario: カレンダーの Done で値 callback の後に閉じ切り callback が届く
- **GIVEN** uiStyle がカレンダーの DatePickerCell の選択面が開いている
- **WHEN** 日付を選んで Done を押し、シートが閉じ切る
- **THEN** `onValueChanged(日付)` が先に、`onValueCompleted(日付)` が閉じ切り後に、同じ日付で 1 回ずつ発火する

#### Scenario: カレンダーの Cancel と対話的 dismiss では発火しない
- **GIVEN** uiStyle がカレンダーの DatePickerCell の選択面が開いている
- **WHEN** Cancel またはシート標準の dismiss 操作で閉じる
- **THEN** `onValueChanged` も `onValueCompleted` も発火しない

#### Scenario: ホイールの Done で入力面が閉じ切った後に閉じ切り callback が届く
- **GIVEN** uiStyle がホイールの DatePickerCell の入力面が表示されている
- **WHEN** Done を押し、入力面が閉じ切る
- **THEN** `onValueChanged(日付)` が先に、`onValueCompleted(日付)` が入力面の閉じ切り後に発火する

#### Scenario: 派生後も DatePickerCell の閉じ切り callback が残る
- **GIVEN** `onValueCompleted` を持つ DatePickerCell
- **WHEN** `withStyle` で派生した Cell を使って Done で確定し閉じ切る
- **THEN** 元の `onValueCompleted` が発火する

#### Scenario: ホイールの Cancel では発火しない
- **GIVEN** uiStyle がホイールの DatePickerCell の入力面が表示されている
- **WHEN** Cancel を押す
- **THEN** `onValueChanged` も `onValueCompleted` も発火しない
