# Tasks: android-datepicker-spinner-wheel

## 1. KsWheelView の内部拡張

- [x] 1.1 指定 index へのプログラム的スクロール (選択更新込み) の internal API を追加 (→ Requirement: 今日へのジャンプ (todayText))
- [x] 1.2 候補数の動的追随の方式を決定し実装 (ホイール再生成 or itemCount 可変化 — 実装冒頭で決定し deviation ではなく設計メモとして tasks に追記) (→ Requirement: 年・月の変更への日候補の追随)
- [x] 1.3 スナップ確定時のみ発火する選択変更通知の internal callback を追加 — 通常スクロール / アクセシビリティ操作 / プログラム的スクロールの全経路で発火し、候補更新による丸めで再入しないこと (→ Requirement: 年・月の変更への日候補の追随 / 選択操作の意味論)

## 2. DatePicker 専用シートの新設

- [x] 2.1 DateSelectionSheet (仮) を NumberSelectionSheet と同系の器で新設し、KsWheelView を年/月/日の3連で配置 (→ Requirement: DatePickerCell (Spinner) 選択面の提示)
- [x] 2.2 年候補範囲 (minDate/maxDate、既定 1900〜2100)・境界年/月の候補制限・初期選択 (date、範囲外は範囲端へ丸め) を実装。不正構成 (minDate > maxDate / 既定適用後の空範囲 / 年候補件数の提示上限超過) は提示せず警告ログ (→ Requirement: 日付候補の範囲と初期選択)
- [x] 2.3 年/月変更時の日候補追随・末日丸め・minDate..maxDate 範囲への最近傍丸めを実装 (→ Requirement: 年・月の変更への日候補の追随)
- [x] 2.4 確定で LocalDate を組み立てて onConfirmed を1回発火、非確定 dismiss (キャンセル/外側タップ/Back/下スワイプ) では発火しない契約を実装 (→ Requirement: 確定と非確定 dismiss)
- [x] 2.5 スナップ静止時のみ選択更新・候補領域の下方向操作で dismiss しない挙動を3連構成で担保 (→ Requirement: 選択操作の意味論)
- [x] 2.6 強調色の段階解決 (accentColor → style.accentColor → Theme.cellAccentColor) と、確定/キャンセル操作色への androidButtonColor 引き継ぎ (指定時最優先) を接続 (→ Requirement: 選択面の強調色)
- [x] 2.7 「今日」ボタン (todayText 非 null・非空文字時のみ表示、タップで3列を今日へ、範囲外なら no-op、callback 非発火) を承認モックの配置で実装。「今日」の取得はテストから注入可能にする (Clock / today provider) (→ Requirement: 今日へのジャンプ (todayText))
- [x] 2.8 各系列のアクセシビリティ状態公開 (系列名 + 選択値) とアクション対応 (→ Requirement: 候補のアクセシビリティ状態)
- [x] 2.9 候補表示文字列の端末 Locale 導出 (ICU パターン導出・自前文字列なし・列順は年→月→日固定) (→ Requirement: 候補表示の Locale 追随)

## 3. 公開 API とホスト接続

- [x] 3.1 DatePickerCell に todayText: String? = null を追加 (挿入位置は uiStyle 直後、equals/hashCode/copy 含む) (→ Requirement: 今日へのジャンプ (todayText))
- [x] 3.2 Compose DSL の DatePickerCell overload に todayText 引数を追加 (挿入位置は uiStyle 直後) (→ Requirement: 今日へのジャンプ (todayText) — DSL Scenario)
- [x] 3.3 DatePickerCellViewHolder の Spinner 分岐 (showSpinnerDatePicker) を新シートの提示に差し替え、旧 AlertDialog + widget.DatePicker 実装を削除 (→ Requirement: DatePickerCell (Spinner) 選択面の提示)

## 4. テスト

- [x] 4.1 候補範囲・初期選択・丸めの単体テスト (既定年範囲 / min-max 制限 / 境界月制限 / 初期=date / 範囲外丸め / minDate > maxDate・空範囲・件数上限の非提示) (→ Requirement: 日付候補の範囲と初期選択)
- [x] 4.2 日候補追随の単体テスト (31日→2月末日丸め / 閏年29日 / 年変更の追随 / 範囲外への遷移の最近傍丸め) (→ Requirement: 年・月の変更への日候補の追随)
- [x] 4.3 todayText の単体テスト (ジャンプ / null・空文字の非提示 / 範囲外 no-op / Material モードで無視 / DSL 生成 — today 注入で日付を固定して決定的に検証) (→ Requirement: 今日へのジャンプ (todayText))
- [x] 4.4 確定/非確定 dismiss の callback 契約テスト (移動中確定の静止値採用を含む) (→ Requirement: 確定と非確定 dismiss / 選択操作の意味論)
- [x] 4.5 タイトル解決・無効 Cell・強調色解決 (androidButtonColor 引き継ぎ含む) のテスト (→ Requirement: DatePickerCell (Spinner) 選択面の提示 / 選択面の強調色)
- [x] 4.6 アクセシビリティ状態 (系列名 + 選択値)・アクションのテスト (→ Requirement: 候補のアクセシビリティ状態)
- [x] 4.7 日本語・英語 Locale の表示文字列テスト (→ Requirement: 候補表示の Locale 追随)

## 5. サンプルと視覚照合

- [x] 5.1 samples/android の Spinner モード Cell (誕生日) に todayText を指定してデモ導線を更新 (→ Requirement: 今日へのジャンプ (todayText))
- [x] 5.2 実機/エミュレータで mock (approved.png) との視覚照合 — brief.md の検証条件 (スナップ静止 / 初期=date / 日追随 / 今日ジャンプ / 確定契約) を判定し照合結果を brief に記録 (→ 全 Requirement)

## 設計メモ (実装フェーズで決定)

### 1.2 候補数の動的追随は「itemCount 可変化」を採用

ホイールの再生成ではなく、`KsWheelView` に候補の差し替え API
(`setCandidates(itemCount, displayTextAt, selectedIndex)`) を足す方式を採った。

- ホイールの View identity が保たれるため、アクセシビリティノード・フォーカス・スクロール状態が
  差し替えのたびに作り直されない (再生成方式では TalkBack のフォーカスが飛ぶ)
- 3連のうち月・日は年/月の選択に応じて何度も候補が変わるため、差し替えの頻度が高い経路になる
- 候補は「件数 + index→表示文字列」の遅延解決のままなので、差し替えは2つのフィールドの入れ替えと
  `notifyDataSetChanged` + 選択位置へのスクロールで済む

あわせて 1.3 の通知契約を「スナップ静止・アクセシビリティ操作・プログラム的スクロールでは発火し、
`setCandidates` による差し替えと丸めでは発火しない」と定めた。差し替えを指示した側 (シート) は
新しい選択を既に知っているため、通知すると同期処理へ再入するだけになる。シート側にも同期中フラグを
置いて二重に再入を防いでいる。

### 4.1 の保留は解消済み (提示上限 1,000,000 件への引き直し)

当初、「年候補件数が Int 上限を超える指定では提示しない」Scenario は GIVEN の
`minDate = LocalDate.MIN` / `maxDate = LocalDate.MAX` でも年候補件数が 1,999,999,999 件にとどまり
`Int.MAX_VALUE` (2,147,483,647) を超えないため、規則どおりに実装すると選択面が「提示される」ことになり、
Scenario に対応するテストが書けず保留としていた。

オーナー決定により上限を「`Int` の表現上限」から**提示上限 1,000,000 件**へ引き直し (2026-08-02)。
`LocalDate` の値域では到達不能だった旧上限に代えて、ホイールのスクロール範囲が破綻する水準
(1万件規模) に対して2桁の余裕を持つ防御上限を置く。これにより Scenario の GIVEN が到達可能になり、
`DateSelectionSheetTest`「年候補件数が提示上限を超える指定では選択面を提示せず警告ログを残す」で
検証している。件数の算出は桁溢れを避けるため 64bit のまま維持している。
