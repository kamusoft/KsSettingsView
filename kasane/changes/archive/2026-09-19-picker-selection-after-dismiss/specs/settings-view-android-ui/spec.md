## ADDED Requirements

### Requirement: PickerCell 単一選択の閉じ切り通知 (Android)
`PickerCell` は単一選択の閉じ切り callback (`onSelectionCompleted`) を持てる SHALL。候補タップで確定したとき、選択面が閉じ切った後 (プラットフォームが選択面の dismiss 完了として報告する時点) に確定した index を引数として 1 回だけ発火する SHALL。発火は値の callback (`onSelectionChanged`) の後である SHALL。非確定 dismiss (キャンセル・外側タップ・Back・ハンドル / ヘッダー起点の下スワイプ) ではどの callback も発火しない SHALL。callback は省略可能で、既存の呼び出しは変更なしに動く SHALL。

#### Scenario: 候補タップで値 callback の後に閉じ切り callback が届く
- **GIVEN** `onSelectionChanged` と `onSelectionCompleted` を持つ単一選択の PickerCell の選択面が開いている
- **WHEN** 候補をタップし、選択面が閉じ切る
- **THEN** `onSelectionChanged(index)` が先に、`onSelectionCompleted(index)` が閉じ切り後に、同じ index で 1 回ずつ発火する

#### Scenario: 非確定 dismiss のどの経路でも閉じ切り callback が発火しない
- **GIVEN** `onSelectionCompleted` を持つ単一選択の PickerCell の選択面が開いている
- **WHEN** キャンセル・外側タップ・Back・下スワイプのいずれかで閉じる
- **THEN** `onSelectionChanged` も `onSelectionCompleted` も発火しない

### Requirement: PickerCell 複数選択の閉じ切り通知 (Android)
`PickerCell` は複数選択の閉じ切り callback (`onMultiSelectionCompleted`) を持てる SHALL。確定操作 (OK) で確定したとき、選択面が閉じ切った後に確定した index 集合を引数として 1 回だけ発火する SHALL。発火は値の callback (`onMultiSelectionChanged`) の後で、同じ集合を運ぶ SHALL。候補のトグルでは発火しない SHALL。非確定 dismiss ではどの callback も発火しない SHALL。

#### Scenario: OK で値 callback の後に閉じ切り callback が届く
- **GIVEN** 複数選択の PickerCell の選択面で候補をいくつかトグルしている
- **WHEN** OK を押し、選択面が閉じ切る
- **THEN** `onMultiSelectionChanged(集合)` が先に、`onMultiSelectionCompleted(集合)` が閉じ切り後に、同じ集合で 1 回ずつ発火する

#### Scenario: 候補のトグルだけでは閉じ切り callback が発火しない
- **GIVEN** 複数選択の PickerCell の選択面が開いている
- **WHEN** 候補をトグルする
- **THEN** `onMultiSelectionCompleted` は発火しない

#### Scenario: キャンセルでは閉じ切り callback が発火しない
- **GIVEN** 複数選択の PickerCell の選択面で候補をトグルしている
- **WHEN** キャンセルで閉じる
- **THEN** `onMultiSelectionChanged` も `onMultiSelectionCompleted` も発火しない

### Requirement: DatePickerCell の閉じ切り通知 (Android)
`DatePickerCell` は閉じ切り callback (`onValueCompleted`) を持てる SHALL。確定操作 (OK) で確定したとき、選択面が閉じ切った後に確定した日付を引数として 1 回だけ発火する SHALL。閉じ切りとは、プラットフォームが選択面の dismiss 完了として報告する時点を指す SHALL。Spinner のボトムシートは hide アニメーション完了後、カレンダーダイアログは window のフェード完了前に報告される (意図的なプラットフォーム差。退場アニメーションの完了までは待たない)。発火は値の callback (`onValueChanged`) の後で、同じ日付を運ぶ SHALL。uiStyle に関係なく同じ契約である SHALL。非確定 dismiss ではどの callback も発火しない SHALL。既存の呼び出しは変更なしに動く SHALL。

#### Scenario: Spinner の OK で値 callback の後に閉じ切り callback が届く
- **GIVEN** uiStyle が Spinner の DatePickerCell の選択面が開いている
- **WHEN** 日付を選んで OK を押し、シートが閉じ切る
- **THEN** `onValueChanged(日付)` が先に、`onValueCompleted(日付)` が閉じ切り後に、同じ日付で 1 回ずつ発火する

#### Scenario: カレンダーの OK で値 callback の後に閉じ切り callback が届く
- **GIVEN** uiStyle がカレンダーの DatePickerCell の選択面が開いている
- **WHEN** 日付を選んで OK を押し、ダイアログが閉じる
- **THEN** `onValueChanged(日付)` が先に、`onValueCompleted(日付)` が閉じた後に、同じ日付で 1 回ずつ発火する

#### Scenario: 非確定 dismiss では発火しない
- **GIVEN** DatePickerCell の選択面が開いている (uiStyle は任意)
- **WHEN** キャンセル・外側タップ・Back のいずれかで閉じる
- **THEN** `onValueChanged` も `onValueCompleted` も発火しない

### Requirement: 閉じ切り callback はすべての構築経路で指定できる (Android)
PickerCell の閉じ切り callback は、data class の直接構築、object 候補を射影する factory、Compose DSL の overload のすべてで指定できる SHALL。DatePickerCell の閉じ切り callback は data class の直接構築と Compose DSL の overload で指定できる SHALL。`copy` による派生を経ても保持される SHALL。

#### Scenario: 射影 factory で構築した PickerCell に閉じ切り callback を渡せる
- **GIVEN** object の候補列と射影を渡す factory で `onSelectionCompleted` を指定して構築した PickerCell
- **WHEN** 候補をタップして選択面が閉じ切る
- **THEN** 射影された値の書き戻しの後に `onSelectionCompleted(index)` が発火する

#### Scenario: Compose DSL で構築した PickerCell に閉じ切り callback を渡せる
- **GIVEN** Compose DSL の picker で `onSelectionCompleted` を指定している
- **WHEN** 候補をタップして選択面が閉じ切る
- **THEN** 状態の更新の後に `onSelectionCompleted(index)` が発火する

### Requirement: 閉じ切り通知を足しても選択面の再提示と参照解放は従来どおり働く (Android)
閉じ切り callback の配線は、選択面の dismiss 後に行われている既存の後始末 (参照解放と再提示の可否) を損なわない SHALL。

#### Scenario: 確定して閉じた後に同じ Cell の選択面を再び開ける
- **GIVEN** `onSelectionCompleted` を持つ PickerCell で候補をタップして選択面が閉じ切った
- **WHEN** 同じ Cell の行を再びタップする
- **THEN** 選択面が再び開き、再度の確定で `onSelectionCompleted` がもう 1 回発火する
