# Delta Spec: android-datepicker (カレンダー選択面の Compose 統一)

対象能力: android-datepicker — DatePickerCell の選択面 (uiStyle `Material` のカレンダー型)。本デルタは `MaterialDatePicker` (DialogFragment) を廃止し、Compose Material3 DatePicker のダイアログ表示に統一した後の契約を定義する (android/ADR-0019)。ホイール型 (uiStyle `Spinner`) の契約 (kasane/concepts/core/cells/date-picker-selection-surface.md) は変更しない。共通の挙動契約 (確定のみ反映・タイトル解決・範囲制限・todayText の意味論) は同概念文書のプラットフォーム共通契約を維持する。

## ADDED Requirements

### Requirement: ホスト前提に依存しないカレンダー選択面の提示

`isEnabled` な uiStyle `Material` の DatePickerCell の行タップで、ホスト Activity の型 (`ComponentActivity` を含む) と XML テーマに関わらずカレンダー選択面 (ダイアログ) が提示される SHALL。選択面はカレンダー表示とテキスト入力の2モードを持ち、相互に切り替えられる SHALL。タイトルは `pickerTitle` があればそれ、なければ `title` で解決する SHALL。

#### Scenario: ComponentActivity ホストでの提示

- **GIVEN** `ComponentActivity` (FragmentActivity でない) のホストに配置した有効な DatePickerCell (uiStyle `Material`)
- **WHEN** 行をタップする
- **THEN** カレンダー選択面が提示される (何も起きない・例外、のいずれも発生しない)

#### Scenario: 入力モードの切替

- **GIVEN** カレンダー表示中の選択面
- **WHEN** テキスト入力モードへ切り替える
- **THEN** 日付をテキストで入力でき、カレンダー表示へ戻せる

### Requirement: 確定のみ反映 (共通契約の維持)

確定操作で、その時点の選択日から作った `LocalDate` を引数に `onValueChanged` を1回発火して閉じる SHALL。非確定の閉じ方 (キャンセル・外側タップ・Back 等、器が提供するすべての経路) では発火せず、変更は破棄される SHALL。

#### Scenario: 確定で1回発火

- **GIVEN** 選択面で日付を選択した状態
- **WHEN** 確定操作を行う
- **THEN** 選択日の `LocalDate` で `onValueChanged` が1回だけ発火し、選択面が閉じる

#### Scenario: 非確定 dismiss は無発火

- **GIVEN** 選択面で選択を変更した状態
- **WHEN** キャンセル (または外側タップ・Back) で閉じる
- **THEN** `onValueChanged` は発火しない

### Requirement: 範囲制限 (共通契約の維持)

`minDate` / `maxDate` の範囲外の日付は選択できない SHALL (カレンダー表示・テキスト入力の両モード)。選択面が提示・確定する日付は常に範囲内である SHALL。開いた時点の `cell.date` が範囲外の場合は最も近い範囲端へ丸めて提示する SHALL (ホイール型選択面の既存契約と同一)。年の候補範囲は `minDate` / `maxDate` の未指定側をそれぞれ 1900 / 2100 とする SHALL (同上)。

#### Scenario: 範囲外日付の選択不可

- **GIVEN** `minDate` / `maxDate` を指定した DatePickerCell の選択面
- **WHEN** 範囲外の日付を選択しようとする (カレンダーのタップまたはテキスト入力)
- **THEN** 選択は成立せず、確定できるのは範囲内の日付だけである

#### Scenario: 範囲外の初期値は範囲端へ丸めて提示

- **GIVEN** `date` が `minDate` より前の DatePickerCell
- **WHEN** 選択面を開く
- **THEN** `minDate` が選択中として提示される (確定するまで `cell.date` は変わらない)

### Requirement: タイムゾーンに依存しない日付の往復

選択面の内部表現と `LocalDate` の変換は日単位で安定であり、端末のタイムゾーンによって確定日が選択したカレンダー日付から前後にずれない SHALL。端末タイムゾーンを参照するのは「今日」の算出だけである SHALL。

#### Scenario: 非 UTC タイムゾーンでの日付一致

- **GIVEN** 端末タイムゾーンが UTC から大きく離れた設定 (東西いずれか)
- **WHEN** カレンダーで特定の日付を選択して確定する
- **THEN** `onValueChanged` に渡る `LocalDate` は選択したカレンダー日付と一致する

### Requirement: 今日ジャンプ (共通契約の維持)

`todayText` が非 null かつ非空文字のときだけ「今日」操作を提示する SHALL。実行すると選択日と表示月がデバイスの現在日付 (端末タイムゾーンの今日) へ移動し、この操作自体は `onValueChanged` を発火しない SHALL。今日が `minDate`..`maxDate` の範囲外なら選択状態を変更しない SHALL (日単位比較・境界日は有効)。連続実行は冪等 SHALL。テキスト入力モードの表示中に実行するとカレンダー表示に戻って成立する SHALL。

#### Scenario: ジャンプは発火しない

- **GIVEN** `todayText` を指定した選択面で過去の日付を選択中
- **WHEN** 「今日」操作を実行する
- **THEN** 選択日と表示月が今日になり、`onValueChanged` は発火しない

#### Scenario: 範囲外セーフガード

- **GIVEN** 今日が `maxDate` より後の DatePickerCell の選択面
- **WHEN** 「今日」操作を実行する
- **THEN** 選択状態は変化しない

### Requirement: 構成変更をまたぐ表示継続

構成変更 (回転等) をまたぐカレンダー選択面の挙動は、ホスト形態ごとに次を保証する SHALL:

1. **構成変更を in-place で処理するホスト** (Activity 再生成が起きない構成。MAUI テンプレート既定を含む): 選択面は開いたまま生存し、選択状態を維持する SHALL
2. **Activity 再生成が起きるホストのうち、View 階層のインスタンス状態保存に参加できる構成** (安定した View id の付与等の成立条件と各ホスト形態の成立性は実装時のスパイクで確定する): 選択状態 (選択日・表示月・表示モード) を維持して再提示される SHALL。再提示は、保存時と同一 `cell.id` の DatePickerCell (uiStyle `Material`) が復元後の root に存在する場合に限る SHALL。不成立の場合は再提示せず、いかなる Cell へも値を書き込まない SHALL NOT
3. **状態保存に参加できないと確定したホスト形態**: 選択面は閉じ、`onValueChanged` は発火しない SHALL (シート系選択面と同じ縮退。該当形態はスパイク結果を受けて本 Requirement の注記として明文化する)

復元後の選択面でも配色・今日ジャンプ・確定/破棄の契約は全て有効である SHALL (従来の「復元後は配色対象外」の既知問題を残さない)。

#### Scenario: Activity 再生成をまたぐ状態維持

- **GIVEN** 状態保存が成立するホストで、選択日を変更し表示月を移動したカレンダー選択面
- **WHEN** Activity が再生成される (回転または `Activity.recreate()` 相当)
- **THEN** 選択面が同じ選択日・表示月・表示モードで再提示され、確定すればその選択日で発火する

#### Scenario: in-place 構成変更では開いたまま生存

- **GIVEN** 構成変更を in-place で処理するホスト (MAUI テンプレート既定等) で表示中のカレンダー選択面
- **WHEN** 画面を回転する (Activity は再生成されない)
- **THEN** 選択面は閉じずに選択状態を維持する

#### Scenario: 対応 Cell 不在時は復元しない

- **GIVEN** カレンダー選択面の表示中に保存された状態
- **WHEN** 再生成後の root に同一 `cell.id` の DatePickerCell (uiStyle `Material`) が存在しない
- **THEN** 選択面は再提示されず、`onValueChanged` も発火しない

## REMOVED Requirements

### Requirement: Material ダイアログの View 走査配色とヘッダ補正

**Reason**: 対象 UI (`MaterialDatePicker`) の廃止に伴い、表示後の内部 View 走査による配色・ヘッダ重なり補正の契約 (android/ADR-0008) は対象を失う。新選択面の配色は4色ロール (背景・強調・通常文字・アクセント上文字) を正面の配色 API で指定する形で維持する。「Material Components のカレンダースタイル属性カスタム時の誤分類」という既知の限界も走査の廃止と共に消滅する。

### Requirement: 今日ジャンプの View 階層駆動

**Reason**: 対象実装 (正規クリック経路への View 階層駆動、android/ADR-0010) の廃止。今日ジャンプの挙動契約自体は本デルタの「今日ジャンプ (共通契約の維持)」が引き継ぎ、実現方式が状態操作に変わる。
