# Delta: settings-view-android-ui (android-datepicker-spinner-wheel)

## ADDED Requirements

### Requirement: DatePickerCell (Spinner) 選択面の提示

Android host は、`uiStyle = DatePickerUIStyle.Spinner` かつ `isEnabled` な DatePickerCell の行タップで、年・月・日の3系列からなる日付選択面をモーダル提示する SHALL。選択面のタイトルには `pickerTitle ?: title` を表示する。確定・キャンセルの操作ラベルは OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) から解決する。`isEnabled = false` の行タップでは選択面を提示しない。

#### Scenario: タイトルの解決
- **GIVEN** `pickerTitle = "日付を選択"` かつ `title = "誕生日"` の Spinner モード DatePickerCell
- **WHEN** 行をタップして選択面を開く
- **THEN** 選択面のタイトルに「日付を選択」が表示される (pickerTitle が null なら「誕生日」)

#### Scenario: 年・月・日の3系列が提示される
- **GIVEN** `date = 2026-08-02` の Spinner モード DatePickerCell
- **WHEN** 行をタップして選択面を開く
- **THEN** 年・月・日の3系列が提示され、それぞれ独立に候補を選択できる

#### Scenario: 無効 Cell は選択面を提示しない
- **GIVEN** `isEnabled = false` の Spinner モード DatePickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されない

### Requirement: 日付候補の範囲と初期選択

選択面の候補は `minDate` / `maxDate` を尊重する SHALL。年候補は `minDate` の年 (未指定なら 1900 年) から `maxDate` の年 (未指定なら 2100 年) までを列挙する。選択中の年・月に対する月候補・日候補は、組み立てた日付が `minDate`..`maxDate` の範囲に収まる選択肢だけに制限される。選択面は、開いた時点で `date` を選択中として提示する。`date` が `minDate`..`maxDate` の範囲外の場合は、最も近い範囲端 (minDate または maxDate) を選択中として提示する。

年候補の件数は 64bit 整数で算出し、提示上限 1,000,000 件を超える指定では選択面を提示せず警告ログを残す (ホイールのスクロール範囲が破綻する水準より1桁以上低い防御上限)。`minDate > maxDate` の場合、および未指定側への既定 (1900 / 2100) 適用後に年候補の下限が上限を超える構成の場合も、選択面を提示せず警告ログを残す。

#### Scenario: 未指定時の年候補の既定範囲
- **GIVEN** `minDate = null`、`maxDate = null` の Spinner モード DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 年候補は 1900 年から 2100 年までが列挙される

#### Scenario: minDate / maxDate による年候補の制限
- **GIVEN** `minDate = 2020-04-01`、`maxDate = 2030-09-30` の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 年候補は 2020 年から 2030 年までに制限される

#### Scenario: 境界年では月候補も制限される
- **GIVEN** `minDate = 2020-04-01` の DatePickerCell で、選択面の年系列を 2020 年にする
- **WHEN** 月系列の候補を確認する
- **THEN** 月候補は 4 月から 12 月までに制限される

#### Scenario: 初期選択は date
- **GIVEN** `date = 2026-08-02` (範囲内) の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 年 2026・月 8・日 2 がそれぞれ選択中として提示される

#### Scenario: 範囲外の date は最も近い範囲端へ丸めて提示する
- **GIVEN** `date = 1970-01-01`、`minDate = 2020-04-01` の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 2020-04-01 (minDate) が選択中として提示される

#### Scenario: minDate > maxDate では選択面を提示しない
- **GIVEN** `minDate = 2030-01-01`、`maxDate = 2020-12-31` の DatePickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されず、警告ログが記録される

#### Scenario: 既定適用後に範囲が空になる構成では提示しない
- **GIVEN** `minDate = 2200-01-01`、`maxDate = null` (既定上限 2100) の DatePickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されず、警告ログが記録される

#### Scenario: 年候補件数が提示上限を超える指定では提示しない
- **GIVEN** `minDate = LocalDate.MIN`、`maxDate = LocalDate.MAX` の DatePickerCell (年候補件数 1,999,999,999 件が提示上限 1,000,000 件を超える)
- **WHEN** 行をタップする
- **THEN** 選択面は提示されず、警告ログが記録される

### Requirement: 年・月の変更への日候補の追随

日候補の件数は、選択中の年・月の実日数に追随する SHALL (閏年を含む)。年または月の変更によって選択中の日が新しい年・月の末日を超える場合は、末日へ丸める。さらに、年または月の変更後に組み立てた日付が `minDate`..`maxDate` の範囲外になる場合は、範囲内の最も近い日付へ丸めて3系列の選択中へ反映する。

#### Scenario: 31 日から日数の少ない月への変更は末日へ丸める
- **GIVEN** 選択面で 2026 年 1 月 31 日を選択中
- **WHEN** 月系列を 2 月に変更する
- **THEN** 日候補は 1〜28 になり、選択中の日は 28 日へ丸められる

#### Scenario: 閏年の 2 月は 29 日まで列挙される
- **GIVEN** 選択面で 2028 年 (閏年) を選択中
- **WHEN** 月系列を 2 月に変更する
- **THEN** 日候補は 1〜29 が列挙される

#### Scenario: 年の変更でも日が追随する
- **GIVEN** 選択面で 2028 年 2 月 29 日 (閏年) を選択中
- **WHEN** 年系列を 2027 年 (平年) に変更する
- **THEN** 日候補は 1〜28 になり、選択中の日は 28 日へ丸められる

#### Scenario: 年・月の変更で範囲外になった日付は範囲内の最近傍へ丸める
- **GIVEN** `minDate = 2020-04-15` の選択面で 2021 年 1 月 31 日を選択中
- **WHEN** 年系列を 2020 年に変更する
- **THEN** 選択中は 2020-04-15 (範囲内の最も近い日付) になる

### Requirement: 今日へのジャンプ (todayText)

Android の `DatePickerCell` は `todayText: String?` (既定 `null`) を持つ SHALL。非 null かつ非空文字のとき、Spinner モードの選択面に `todayText` をラベルとする「今日へジャンプする操作」を提示し、実行すると3系列の選択中がデバイスの現在日付 (端末の既定タイムゾーンにおける今日) になる。この操作自体は `onValueChanged` を発火しない (値の確定は確定操作のみ)。今日が `minDate`..`maxDate` の範囲外の場合、操作を実行しても選択状態を変更しない。`todayText` が `null` または空文字のときはこの操作を提示しない (iOS と同じ非表示条件)。`uiStyle = Material` の選択 UI では `todayText` は影響しない (無視される)。Compose DSL の DatePickerCell overload にも `todayText` 引数 (既定 `null`) を追加する。

#### Scenario: 今日へジャンプする
- **GIVEN** `todayText = "今日"` の選択面で 2020 年 1 月 1 日を選択中 (今日は範囲内)
- **WHEN** 今日へジャンプする操作を実行する
- **THEN** 3系列の選択中がデバイスの現在日付になり、`onValueChanged` は発火しない

#### Scenario: todayText が null または空文字なら操作を提示しない
- **GIVEN** `todayText = null` (既定) または `todayText = ""` の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 今日へジャンプする操作は提示されない

#### Scenario: Material モードでは todayText は影響しない
- **GIVEN** `todayText = "今日"`、`uiStyle = DatePickerUIStyle.Material` の DatePickerCell
- **WHEN** 行をタップして選択 UI を開く
- **THEN** 今日へジャンプする操作は提示されず、選択 UI の挙動は todayText 未指定時と変わらない

#### Scenario: 今日が範囲外なら何も変更しない
- **GIVEN** `todayText = "今日"`、`maxDate = 2020-12-31` (今日より過去) の選択面で 2020 年 6 月 1 日を選択中
- **WHEN** 今日へジャンプする操作を実行する
- **THEN** 選択中は 2020 年 6 月 1 日のまま変化しない

#### Scenario: Compose DSL overload から todayText を指定できる
- **GIVEN** `Section { DatePickerCell(title = "誕生日", date = state, todayText = "今日") }` の DSL 定義
- **WHEN** Cell model が生成される
- **THEN** 生成された DatePickerCell の `todayText` は「今日」である

### Requirement: 確定と非確定 dismiss

選択面の確定操作は、その時点で選択中の年・月・日から組み立てた LocalDate を引数に `onValueChanged` を1回発火して選択面を閉じる SHALL。確定に至らない閉じ方 — キャンセルボタン・選択面の外側タップ・Back 操作・下方向スワイプによる dismiss — では、いずれの経路でも `onValueChanged` を発火しない。

#### Scenario: 確定で選択日付を1回通知する
- **GIVEN** `date = 2026-08-02` の選択面で 2026 年 9 月 15 日を選択中にした
- **WHEN** 確定操作を行う
- **THEN** `onValueChanged(2026-09-15)` が1回発火し、選択面が閉じる

#### Scenario: 非確定 dismiss は経路によらず callback を発火しない
- **GIVEN** 選択面で選択中の日付を変更した
- **WHEN** キャンセルボタン / 選択面の外側タップ / Back 操作 / 下方向スワイプのいずれかで選択面が閉じる
- **THEN** `onValueChanged` は発火せず、変更は破棄される

### Requirement: 選択操作の意味論

各系列は常にいずれか1つの候補だけが選択中となり、選択中候補の更新は候補の並びが候補位置に静止 (スナップ) した時点でのみ行う SHALL。移動中に確定した場合は、その系列は直前に静止した候補を採用する。候補領域での下方向のスクロール操作は候補の遷移であり、選択面の dismiss を引き起こさない。

#### Scenario: 移動中の確定は直前にスナップ静止した候補を採用する
- **GIVEN** 2026 年 8 月 2 日を選択中の選択面で、日系列が慣性移動中
- **WHEN** 静止する前に確定操作を行う
- **THEN** `onValueChanged(2026-08-02)` が発火する (静止していない位置の候補は採用されない)

#### Scenario: 候補領域の下方向操作はシートを閉じない
- **GIVEN** 選択面が表示されている
- **WHEN** 候補領域を下方向にスワイプする
- **THEN** 選択面は閉じず、候補が遷移する

### Requirement: 選択面の強調色

選択面の選択中候補の強調色は、既存のスタイル解決契約「Cell 固有値 → CellStyle → Theme」に従い、`DatePickerCell.accentColor` → `DatePickerCell.style.accentColor` → `Theme.cellAccentColor` の順で解決する SHALL。選択面の確定・キャンセル操作の色は、`androidButtonColor` が指定されていればそれを最優先し、未指定なら上記の段階解決に従う SHALL (現行 Spinner ダイアログのボタン色契約の引き継ぎ)。

#### Scenario: Cell 固有値が最優先される
- **GIVEN** `accentColor` を明示指定した DatePickerCell (`style.accentColor` も指定あり)
- **WHEN** 選択面を開く
- **THEN** 選択中候補の強調は `accentColor` の指定色で表示される

#### Scenario: Theme の既定色へフォールバックする
- **GIVEN** `accentColor` と `style.accentColor` がいずれも null の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** 選択中候補の強調は `Theme.cellAccentColor` の色で表示される

#### Scenario: androidButtonColor は確定・キャンセル操作に引き継がれる
- **GIVEN** `androidButtonColor` を明示指定した DatePickerCell (`accentColor` も指定あり)
- **WHEN** 選択面を開く
- **THEN** 確定・キャンセル操作は `androidButtonColor` の指定色で表示される (選択中候補の強調は accentColor のまま)

### Requirement: 候補表示の Locale 追随

各系列の候補表示文字列は、端末 Locale の日付表記慣行から導出する SHALL (自前の翻訳文字列は同梱しない)。系列の並び順は Locale によらず年→月→日で固定する。アクセシビリティへ公開する表示文字列にも同じ表記を用いる。

#### Scenario: 日本語 Locale の表示
- **GIVEN** 端末 Locale が日本語で、2026-08-02 を選択中の選択面
- **WHEN** 各系列の表示を確認する
- **THEN** 年系列は「2026年」、月系列は「8月」、日系列は「2日」と表示される

#### Scenario: 英語 Locale の表示
- **GIVEN** 端末 Locale が英語で、2026-08-02 を選択中の選択面
- **WHEN** 各系列の表示を確認する
- **THEN** 年系列は「2026」(接尾辞なし)、月系列は英語の月表記 (例: Aug)、日系列は「2」(接尾辞なし) と表示される

### Requirement: 候補のアクセシビリティ状態

選択面の各系列は、系列の意味 (年・月・日のいずれか) を識別できる名前と併せて、選択中の候補の表示文字列を個別にアクセシビリティサービスへ公開する SHALL。選択中の候補が変わったときは、公開される状態も更新される。また、アクセシビリティ操作 (前候補・次候補へのアクセシビリティアクション) で系列ごとに選択中候補を変更できる SHALL。先頭・末尾の候補ではその方向への変更は行われない。年・月をアクセシビリティ操作で変更した場合も、日候補の追随と末日への丸めは同様に適用される。

#### Scenario: 各系列の選択中候補が公開される
- **GIVEN** 2026 年 8 月 2 日を選択中の選択面
- **WHEN** 各系列のアクセシビリティ情報を取得する
- **THEN** 年・月・日の各系列から、それぞれの選択中の表示文字列が公開される

#### Scenario: アクセシビリティ操作で系列ごとに候補を変更できる
- **GIVEN** 2026 年 8 月 2 日を選択中の選択面
- **WHEN** 月系列の次候補へのアクセシビリティアクションを実行する
- **THEN** 月系列の選択中は 9 月になり、公開される状態も更新される
