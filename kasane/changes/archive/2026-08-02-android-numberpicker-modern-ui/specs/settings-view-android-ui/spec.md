# Delta: settings-view-android-ui (android-numberpicker-modern-ui)

## ADDED Requirements

### Requirement: NumberPickerCell の unit と表示値の生成

Android の `NumberPickerCell` は単位文字列 `unit: String` (既定 `""`) を持つ SHALL。フォーマット規則は iOS と同一とする: `unit` が空なら値の数値文字列、非空なら `"<値> <unit>"` (半角スペース区切り)。Cell 行の valueText 表示は `valueText` の明示指定があればそれを優先し、`null` のときは `value` へフォーマット規則を適用して自動生成する。選択面の候補表示は `valueText` の有無にかかわらず、常に各候補値へフォーマット規則を個別に適用して生成する (`valueText` の優先は Cell 行のみ)。Compose DSL の TwoWay overload (`DSLSectionScope.NumberPickerCell`) にも `unit` 引数 (既定 `""`) を追加する。

#### Scenario: unit 指定時の自動表示
- **GIVEN** `value = 15`、`unit = "px"`、`valueText = null` の NumberPickerCell
- **WHEN** Cell 行を表示する
- **THEN** valueText 表示は「15 px」となる

#### Scenario: unit 未指定時の自動表示
- **GIVEN** `value = 30`、`unit` 未指定 (既定 `""`)、`valueText = null` の NumberPickerCell
- **WHEN** Cell 行を表示する
- **THEN** valueText 表示は「30」となる (suffix なし)

#### Scenario: valueText 明示指定は unit より優先される
- **GIVEN** `value = 15`、`unit = "px"`、`valueText = "十五ピクセル"` の NumberPickerCell
- **WHEN** Cell 行を表示する
- **THEN** valueText 表示は「十五ピクセル」となる

#### Scenario: 選択面の候補表示にも同じフォーマットを適用する
- **GIVEN** `min = 10`、`max = 20`、`step = 5`、`unit = "px"` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補の表示は「10 px」「15 px」「20 px」となる

#### Scenario: valueText 明示指定は候補表示に影響しない
- **GIVEN** `min = 10`、`max = 20`、`step = 5`、`unit = "px"`、`valueText = "十五ピクセル"` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補の表示は「10 px」「15 px」「20 px」となる (valueText は候補表示に適用されない)

#### Scenario: Compose DSL overload から unit を指定できる
- **GIVEN** `Section { NumberPickerCell(title = "サイズ", value = state, unit = "px") }` の DSL 定義
- **WHEN** Cell model が生成される
- **THEN** 生成された NumberPickerCell の `unit` は「px」である

### Requirement: NumberPickerCell 選択面の提示

Android host は、`isEnabled` な NumberPickerCell の行タップで数値選択面をモーダル提示する SHALL。選択面のタイトルには `pickerTitle ?: title` を表示する。候補は `min` から `max` まで `step` 刻みで昇順に列挙する (`step <= 0` は 1 へ fallback)。`min > max` の場合は選択面を提示せず、警告ログを残す (現行挙動の維持)。候補件数は 64bit 整数で算出し、`Int` の表現上限 (2^31 − 1) を超える場合も選択面を提示せず警告ログを残す。候補の列挙は `max` 付近の step 加算で数値がオーバーフローしても終端し、全ての `min <= max` な指定で有限件に確定する。`isEnabled = false` の行タップでは選択面を提示しない。確定・キャンセルの操作ラベルは OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) から解決する。

#### Scenario: タイトルの解決
- **GIVEN** `pickerTitle = "サイズを選択"` かつ `title = "サイズ"` の NumberPickerCell
- **WHEN** 行をタップして選択面を開く
- **THEN** 選択面のタイトルに「サイズを選択」が表示される (pickerTitle が null なら「サイズ」)

#### Scenario: step 刻みの候補列挙
- **GIVEN** `min = 0`、`max = 100`、`step = 25` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補は 0, 25, 50, 75, 100 の5件が昇順に列挙される

#### Scenario: step が 0 以下なら 1 へ fallback する
- **GIVEN** `min = 1`、`max = 3`、`step = 0` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補は 1, 2, 3 の3件となる

#### Scenario: min > max では選択面を提示しない
- **GIVEN** `min = 10`、`max = 5` の NumberPickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されず、警告ログが記録される

#### Scenario: 候補件数が Int 上限を超える指定では提示しない
- **GIVEN** `min = Int.MIN_VALUE`、`max = Int.MAX_VALUE`、`step = 1` の NumberPickerCell (候補件数 2^32 件)
- **WHEN** 行をタップする
- **THEN** 選択面は提示されず、警告ログが記録される

#### Scenario: max 付近の step 加算でも列挙が終端する
- **GIVEN** `min = Int.MAX_VALUE - 1`、`max = Int.MAX_VALUE`、`step = 5` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補は `Int.MAX_VALUE - 1` の1件のみで、列挙は終端する

#### Scenario: 無効 Cell は選択面を提示しない
- **GIVEN** `isEnabled = false` の NumberPickerCell
- **WHEN** 行をタップする
- **THEN** 選択面は提示されない

### Requirement: 選択候補の初期状態と選択操作

選択面は、開いた時点で `value` に一致する候補を選択中として提示する SHALL (`value` が候補に含まれない場合は先頭候補)。選択操作中は常にいずれか1つの候補だけが選択中となり、選択中の候補は他の候補と判別できる形で提示される。選択中候補の更新は、候補の並びが候補位置に静止 (スナップ) した時点でのみ行い、ドラッグ・慣性移動の途中では直前の選択中候補を維持する。候補領域での下方向のスクロール操作は候補の遷移であり、選択面の dismiss を引き起こさない (下方向スワイプによる dismiss は候補領域の外 — ドラッグハンドル・ヘッダー — を起点とする操作に限る)。

#### Scenario: 初期選択は現在値
- **GIVEN** `min = 0`、`max = 100`、`step = 25`、`value = 50` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補 50 が選択中として提示される

#### Scenario: 現在値が候補に含まれない場合は先頭候補
- **GIVEN** `min = 0`、`max = 100`、`step = 25`、`value = 30` の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 候補 0 (先頭) が選択中として提示される

#### Scenario: 移動中の確定は直前にスナップ静止した候補を採用する
- **GIVEN** `value = 50` で開いた選択面 (選択中 50) で、候補の並びが慣性移動中
- **WHEN** 静止する前に確定操作を行う
- **THEN** `onValueChanged(50)` が発火する (静止していない位置の候補は採用されない)

#### Scenario: 候補領域の下方向操作はシートを閉じない
- **GIVEN** 選択面が表示されている
- **WHEN** 候補領域を下方向にスワイプする
- **THEN** 選択面は閉じず、候補が遷移する

### Requirement: 確定と非確定 dismiss

選択面の確定操作は、その時点で選択中の候補値を引数に `onValueChanged` を1回発火して選択面を閉じる SHALL。確定に至らない閉じ方 — キャンセルボタン・選択面の外側タップ・Back 操作・下方向スワイプによる dismiss — では、いずれの経路でも `onValueChanged` を発火しない。

#### Scenario: 確定で選択値を1回通知する
- **GIVEN** `value = 50` の選択面で候補 75 を選択中にした
- **WHEN** 確定操作を行う
- **THEN** `onValueChanged(75)` が1回発火し、選択面が閉じる

#### Scenario: 非確定 dismiss は経路によらず callback を発火しない
- **GIVEN** 選択面で選択中の候補を変更した
- **WHEN** キャンセルボタン / 選択面の外側タップ / Back 操作 / 下方向スワイプのいずれかで選択面が閉じる
- **THEN** `onValueChanged` は発火せず、変更は破棄される

### Requirement: 選択面の強調色

選択面の選択中候補の強調色は、既存のスタイル解決契約「Cell 固有値 → CellStyle → Theme」に従い、`NumberPickerCell.accentColor` → `NumberPickerCell.style.accentColor` → `Theme.cellAccentColor` の順で解決する SHALL。

#### Scenario: Cell 固有値が最優先される
- **GIVEN** `accentColor` を明示指定した NumberPickerCell (`style.accentColor` も指定あり)
- **WHEN** 選択面を開く
- **THEN** 選択中候補の強調は `accentColor` の指定色で表示される

#### Scenario: Theme の既定色へフォールバックする
- **GIVEN** `accentColor` と `style.accentColor` がいずれも null の NumberPickerCell
- **WHEN** 選択面を開く
- **THEN** 選択中候補の強調は `Theme.cellAccentColor` の色で表示される

### Requirement: 候補のアクセシビリティ状態

選択面は、選択中の候補の表示文字列 (unit 適用後) をアクセシビリティサービスへ公開する SHALL。選択中の候補が変わったときは、公開される状態も更新される。また、アクセシビリティ操作 (前候補・次候補へのアクセシビリティアクション) で選択中候補を変更できる SHALL。操作による変更は選択中候補の提示と公開状態の両方へ反映され、先頭・末尾の候補ではその方向への変更は行われない。

#### Scenario: 選択中候補が公開される
- **GIVEN** `unit = "px"` の選択面で候補 15 が選択中
- **WHEN** 選択領域のアクセシビリティ情報を取得する
- **THEN** 「15 px」が選択中の値として公開される

#### Scenario: アクセシビリティ操作で候補を変更できる
- **GIVEN** `min = 5`、`max = 25`、`step = 5`、`unit = "px"` の選択面で候補 15 が選択中
- **WHEN** 次候補へのアクセシビリティアクションを実行する
- **THEN** 選択中は候補 20 になり、「20 px」が公開される

#### Scenario: 端の候補ではその方向へ変更されない
- **GIVEN** 末尾候補が選択中の選択面
- **WHEN** 次候補へのアクセシビリティアクションを実行する
- **THEN** 選択中の候補は変わらない
