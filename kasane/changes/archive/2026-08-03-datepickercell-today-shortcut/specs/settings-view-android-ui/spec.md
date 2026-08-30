# Delta: settings-view-android-ui (datepickercell-today-shortcut)

## ADDED Requirements

### Requirement: カレンダーモードの今日ショートカットの提示

Android host は、`uiStyle = DatePickerUIStyle.Material` かつ `todayText` が非 null かつ非空文字の DatePickerCell のカレンダー選択 UI に、`todayText` をラベルとする「今日へ移動する操作」を提示する SHALL。`todayText` が null または空文字のときはこの操作を提示しない (現行どおりの構成)。操作のラベルはアクセシビリティサービスにも同じ文字列で公開される。

#### Scenario: todayText 指定時に操作が提示される
- **GIVEN** `todayText = "今日"`、`uiStyle = DatePickerUIStyle.Material` の DatePickerCell
- **WHEN** 行をタップしてカレンダー選択 UI を開く
- **THEN** 「今日」をラベルとする操作が提示される

#### Scenario: todayText が null または空文字なら提示しない
- **GIVEN** `todayText = null` (既定) または `todayText = ""` の Material モード DatePickerCell
- **WHEN** カレンダー選択 UI を開く
- **THEN** 今日へ移動する操作は提示されず、選択 UI の構成は現行と変わらない

#### Scenario: 操作のラベルがアクセシビリティに公開される
- **GIVEN** `todayText = "今日"` の Material モードのカレンダー選択 UI
- **WHEN** 今日へ移動する操作のアクセシビリティ情報を取得する
- **THEN** 操作は「今日」を名前とし、クリック可能な操作としてアクセシビリティサービスへ公開される

### Requirement: カレンダーの今日への移動

今日へ移動する操作を実行すると、カレンダーの表示月とその選択中の日付が、いずれもデバイスの現在日付 (端末の既定タイムゾーンにおける今日) になる SHALL。選択日の表示・確定操作の状態も選択変更に追随する。この操作自体は `onValueChanged` を発火しない (値の確定は既存の確定操作のみが行う)。今日が `minDate`..`maxDate` の範囲外の場合 (比較は日単位)、操作を実行しても表示月・選択状態のいずれも変更しない。操作を連続して実行した結果は1回の実行と同じである SHALL。移動の完了前に選択 UI が閉じられた場合、選択 UI を再提示せず、`onValueChanged` も発火しない SHALL。

#### Scenario: 表示月と選択日が今日になる
- **GIVEN** `todayText = "今日"`、`date = 2020-01-01` の Material モード DatePickerCell のカレンダー選択 UI (2020年1月を表示中、今日は範囲内)
- **WHEN** 今日へ移動する操作を実行する
- **THEN** 表示月が今日の属する月になり、選択中の日付が今日になり、`onValueChanged` は発火しない

#### Scenario: 値の確定は確定操作の責務のまま
- **GIVEN** 今日へ移動する操作を実行済みのカレンダー選択 UI
- **WHEN** 確定操作を行う
- **THEN** `onValueChanged(今日)` が1回発火する (確定せずキャンセルまたは dismiss した場合は発火しない)

#### Scenario: 今日が範囲外なら何も変更しない
- **GIVEN** `todayText = "今日"`、`maxDate = 2020-12-31` (今日より過去) のカレンダー選択 UI (2020年6月を表示中)
- **WHEN** 今日へ移動する操作を実行する
- **THEN** 表示月・選択中の日付とも変化しない

#### Scenario: min/max 当日は有効 (日単位比較)
- **GIVEN** `todayText = "今日"`、`minDate = 今日`、`maxDate = 今日` のカレンダー選択 UI
- **WHEN** 今日へ移動する操作を実行する
- **THEN** 選択中の日付が今日になる (min 側・max 側とも時刻成分の比較で弾かれない)

#### Scenario: 移動の完了前に閉じられたら再提示しない
- **GIVEN** 今日へ移動する操作を実行した直後のカレンダー選択 UI
- **WHEN** 移動の完了前に選択 UI を dismiss する
- **THEN** 選択 UI は再提示されず、`onValueChanged` も発火しない

### Requirement: 代替表示状態からの今日への移動

カレンダー選択 UI が日グリッド以外を表示している状態でも、今日へ移動する操作は成立する SHALL。年選択の表示中に実行した場合は、日グリッドの表示へ戻したうえで表示月・選択中の日付を今日にする。テキスト入力の表示中に実行した場合は、選択 UI がカレンダー (日グリッド) の表示に切り替わり、表示月・選択中の日付が今日になる。いずれの場合も `onValueChanged` は発火せず、今日が範囲外のときの扱いは「カレンダーの今日への移動」と同じ (何も変更しない)。

#### Scenario: 年選択の表示中から今日へ移動する
- **GIVEN** カレンダー選択 UI で年選択の表示に切り替えた状態 (今日は範囲内)
- **WHEN** 今日へ移動する操作を実行する
- **THEN** 日グリッドの表示に戻り、表示月・選択中の日付が今日になる

#### Scenario: テキスト入力の表示中から今日へ移動する
- **GIVEN** カレンダー選択 UI でテキスト入力の表示に切り替えた状態 (今日は範囲内)
- **WHEN** 今日へ移動する操作を実行する
- **THEN** カレンダー (日グリッド) の表示に切り替わり、表示月・選択中の日付が今日になり、`onValueChanged` は発火しない

#### Scenario: 不完全なテキスト入力で無効化された確定操作が今日への移動で有効に戻る
- **GIVEN** テキスト入力の表示で不完全な入力により確定操作が無効な状態 (今日は範囲内)
- **WHEN** 今日へ移動する操作を実行する
- **THEN** カレンダー表示に切り替わって選択中が今日になり、選択日の表示と確定操作の有効状態が選択に追随する

## MODIFIED Requirements

### Requirement: 今日へのジャンプ (todayText)

Android の `DatePickerCell` は `todayText: String?` (既定 `null`) を持つ SHALL。非 null かつ非空文字のとき、Spinner モードの選択面に `todayText` をラベルとする「今日へジャンプする操作」を提示し、実行すると3系列の選択中がデバイスの現在日付 (端末の既定タイムゾーンにおける今日) になる。この操作自体は `onValueChanged` を発火しない (値の確定は確定操作のみ)。今日が `minDate`..`maxDate` の範囲外の場合、操作を実行しても選択状態を変更しない。`todayText` が `null` または空文字のときはこの操作を提示しない (iOS と同じ非表示条件)。`uiStyle = Material` の選択 UI での提示と挙動は、本変更の ADDED Requirements (カレンダーモードの今日ショートカットの提示 / カレンダーの今日への移動 / 代替表示状態からの今日への移動) に定める。Compose DSL の DatePickerCell overload も `todayText` 引数 (既定 `null`) を持つ。

#### Scenario: 今日へジャンプする
- **GIVEN** `todayText = "今日"` の選択面で 2020 年 1 月 1 日を選択中 (今日は範囲内)
- **WHEN** 今日へジャンプする操作を実行する
- **THEN** 3系列の選択中がデバイスの現在日付になり、`onValueChanged` は発火しない

#### Scenario: todayText が null または空文字なら操作を提示しない
- **GIVEN** `todayText = null` (既定) または `todayText = ""` の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 今日へジャンプする操作は提示されない

#### Scenario: 今日が範囲外なら何も変更しない
- **GIVEN** `todayText = "今日"`、`maxDate = 2020-12-31` (今日より過去) の選択面で 2020 年 6 月 1 日を選択中
- **WHEN** 今日へジャンプする操作を実行する
- **THEN** 選択中は 2020 年 6 月 1 日のまま変化しない

#### Scenario: Compose DSL overload から todayText を指定できる
- **GIVEN** `Section { DatePickerCell(title = "誕生日", date = state, todayText = "今日") }` の DSL 定義
- **WHEN** Cell model が生成される
- **THEN** 生成された DatePickerCell の `todayText` は「今日」である
