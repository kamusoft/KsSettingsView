# Delta: settings-view-android-ui (fix-picker-dialog-recreation)

## ADDED Requirements

### Requirement: 再生成後のピッカーダイアログの完全復元

Android host は、TimePickerCell または DatePickerCell (`uiStyle = DatePickerUIStyle.Material`) のピッカーダイアログ表示中に Activity が再生成された場合、KsSettingsView が Window に attach され root が反映された時点で、現 root に**対応する適格な Cell** が存在するなら、復元されたダイアログを表示中のまま次の状態を回復する SHALL。

適格条件: 同一 `id` かつ同型 (TimePicker ダイアログには TimePickerCell、DatePicker ダイアログには `uiStyle = DatePickerUIStyle.Material` の DatePickerCell) の Cell が現 root に**ちょうど1つ**存在すること。`isEnabled` の値・`onValueChanged` が null であること・`minDate` / `maxDate` 等の構成値の変化は適格性に影響しない。復元されたダイアログは表示時の構成と選択状態 (Material の saved state) を維持する — Cell の構成値が再生成をまたいで変わっていても、表示中のダイアログには反映しない (通常表示中に Cell 内容が更新された場合と同じ扱い)。

回復する状態:

1. 確定操作を行うと、その Cell の `onValueChanged` が非 null ならダイアログ上の選択値で1回発火する (null なら何も起きない。キャンセル・dismiss では発火しない)
2. ダイアログ配色は、通常表示時と同じ配色解決規則を、再生成後の root / Theme に基づいて適用する
3. DatePickerCell で `todayText` が非 null かつ非空文字なら、「今日へ移動する操作」を再提示する。再提示された操作は通常表示時と同じ観察可能な挙動を満たす: 実行すると表示月と選択中の日付が今日になり、操作自体は `onValueChanged` を発火せず、今日が `minDate`..`maxDate` の範囲外 (日単位比較) なら何も変更せず、操作のラベルはアクセシビリティサービスにも同じ文字列で公開される

対応付けは Cell の `id` で行い、`id` に使用される文字列の内容 (ドット等の区切り文字を含む場合も) によらず成立する SHALL。DatePickerCell の「今日へ移動する操作」による作り直し後のダイアログ (世代を経たダイアログ) も同様に復元対象となる。

#### Scenario: TimePicker の値確定が回復する
- **GIVEN** 安定 id を持つ TimePickerCell のダイアログを表示中に Activity を再生成し、同一 id の Cell を含む root が反映された
- **WHEN** ダイアログの確定操作を行う
- **THEN** その Cell の `onValueChanged` がダイアログ上の選択時刻で1回発火する

#### Scenario: DatePicker の値確定が回復する
- **GIVEN** 安定 id を持つ Material モード DatePickerCell のダイアログを表示中に Activity を再生成し、同一 id の Cell を含む root が反映された
- **WHEN** ダイアログの確定操作を行う
- **THEN** その Cell の `onValueChanged` がダイアログ上の選択日付で1回発火する

#### Scenario: キャンセルでは発火しない
- **GIVEN** 再生成後に復元されたピッカーダイアログ
- **WHEN** キャンセル操作または dismiss を行う
- **THEN** `onValueChanged` は発火しない

#### Scenario: 配色が再適用される
- **GIVEN** テーマ色 (backgroundColor / accentColor / titleColor) を指定した KsSettingsView 上のピッカーダイアログを表示中に Activity を再生成した
- **WHEN** 復元が完了する
- **THEN** ダイアログの配色は通常表示時と同じ解決規則で適用された状態になる (Material 既定配色のままにならない)

#### Scenario: 「今日」操作が再提示される
- **GIVEN** `todayText = "今日"` の Material モード DatePickerCell のダイアログを表示中に Activity を再生成した
- **WHEN** 復元が完了する
- **THEN** 「今日」をラベルとする操作が再提示され、実行すると表示月と選択中の日付が今日になる

#### Scenario: 作り直し世代のダイアログも復元される
- **GIVEN** 「今日へ移動する操作」による作り直しを経たダイアログ (世代付き) を表示中に Activity を再生成した
- **WHEN** 同一 id の Cell を含む root が反映される
- **THEN** そのダイアログも完全復元の対象となり、確定操作で `onValueChanged` が発火する

#### Scenario: id に区切り文字を含む Cell でも対応付けが成立する
- **GIVEN** `id = "section.1.time"` のようにドットを含む id の TimePickerCell のダイアログを表示中に Activity を再生成した
- **WHEN** 同一 id の Cell を含む root が反映される
- **THEN** 対応付けが成立し、完全復元される

### Requirement: 対応付け不能時の dismiss フォールバック

Activity 再生成後、復元されたピッカーダイアログに対応する適格な Cell (「再生成後のピッカーダイアログの完全復元」に定める適格条件を満たす Cell) が現 root に存在しない場合、Android host はそのダイアログを閉じる SHALL。同一 id の Cell が存在しても、同型でない場合 (DatePickerCell の `uiStyle` が Spinner に変更されている等) や、同一 id の候補が現 root に複数ある場合も「対応する Cell が存在しない」として扱い、ダイアログを閉じる SHALL。このとき、いかなる Cell の `onValueChanged` も発火してはならない (MUST NOT)。別の Cell への値の書き込みは、対応付けの成否によらず発生しない SHALL。

複数インスタンス時の規則: 復元走査の時点で同一 FragmentManager 上に複数の KsSettingsView が attach されている場合、所有者を一意に決められないため完全復元は行わず、復元されたピッカーダイアログを閉じる SHALL (同一 id の適格な Cell が複数インスタンスの root に存在しても、いずれの Cell の `onValueChanged` にも登録しない)。また、1つの復元 Fragment に対する処理 (復元または dismiss) は、複数のインスタンスが走査しても一度だけ行われる SHALL。

#### Scenario: 該当 id が現 root にないダイアログは閉じられる
- **GIVEN** ピッカーダイアログを表示中に Activity を再生成し、当該 id の Cell を含まない root が反映された
- **WHEN** 復元走査が実行される
- **THEN** ダイアログは閉じられ、いかなる Cell の `onValueChanged` も発火しない

#### Scenario: 既定のランダム id では dismiss になる
- **GIVEN** id を明示せず構築した (既定のランダム id の) TimePickerCell のダイアログを表示中に Activity を再生成し、アプリが Cell を再構築した root を反映した
- **WHEN** 復元走査が実行される
- **THEN** id が一致しないためダイアログは閉じられ、誤発火はない

#### Scenario: uiStyle が変更されていたら閉じられる
- **GIVEN** Material モードで表示したダイアログの再生成後、同一 id の DatePickerCell が `uiStyle = Spinner` に変更された root が反映された
- **WHEN** 復元走査が実行される
- **THEN** ダイアログは閉じられ、`onValueChanged` は発火しない

#### Scenario: 同一 id の候補が複数なら閉じられる
- **GIVEN** 再生成後の root に同一 id の同種 Cell が2つ存在する
- **WHEN** 復元走査が実行される
- **THEN** ダイアログは閉じられ、いかなる Cell の `onValueChanged` も発火しない

#### Scenario: 複数の KsSettingsView が存在すると閉じられ、二重発火しない
- **GIVEN** 同一 Activity に2つの KsSettingsView が attach され、双方の root に同一 id の同種 Cell が存在する状態でピッカーダイアログ表示中に Activity を再生成した
- **WHEN** 各インスタンスの復元走査が実行される
- **THEN** ダイアログは一度だけ閉じられ、いずれの Cell の `onValueChanged` も発火しない

### Requirement: 復元走査の駆動条件

復元走査は、KsSettingsView が「Window に attach 済み」かつ「root 反映済み」の両条件を最初に満たした時点で実行される SHALL。root の反映と attach のどちらが先に起きても復元は成立する。復元走査が繰り返されても、リスナー登録・配色適用・「今日」操作の提示が重複しない SHALL (確定操作での `onValueChanged` 発火は常に1回)。

#### Scenario: attach 前に root が反映される経路でも復元される
- **GIVEN** 再生成後、KsSettingsView が Window に attach される前に root が反映された (Compose の factory 内 bind 相当)
- **WHEN** その後 View が Window に attach される
- **THEN** 復元走査が実行され、完全復元が成立する

#### Scenario: attach 後に root が反映される経路でも復元される
- **GIVEN** 再生成後、KsSettingsView が Window に attach された後に root が反映された
- **WHEN** root の反映が完了する
- **THEN** 復元走査が実行され、完全復元が成立する

#### Scenario: 確定操作の発火は1回に保たれる
- **GIVEN** 復元走査を経たピッカーダイアログ
- **WHEN** 確定操作を1回行う
- **THEN** `onValueChanged` はちょうど1回発火する
