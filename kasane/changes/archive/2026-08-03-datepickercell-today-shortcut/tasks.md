# Tasks: datepickercell-today-shortcut

## 1. スパイク (机上で確定できない前提の実測 — 最初に行う)

- [x] 1.1 Robolectric スパイク: `mtrl_calendar_months` の `scrollToPosition` 後に目的月の `month_grid` が bind されるまでの待ち合わせ (`post` + 回数上限リトライ) が成立するかを実測する (→ ADR-0010 Consequences の残リスク)。不成立ならフォールバック D の適用範囲拡大を実装前にユーザーへ報告して合意を取る

## 2. 今日ボタンの注入 (UI)

- [x] 2.1 `todayText` 非 null 非空のとき、DatePickerColorizer の既存フック (FragmentLifecycleCallbacks) で `date_picker_actions` に「今日」ボタンを注入する。同コンテナは右寄せのため左端配置は伸縮スペーサ等で実現し、検証は実座標で行う (brief.md 配置の実装注意)。ボタン行が無い構成 (フルスクリーン) は `confirm_button` の親へフォールバック (→ Requirement: カレンダーモードの今日ショートカットの提示)
- [x] 2.2 `MaterialIds` へ `mtrl_calendar_months` / `date_picker_actions` を追加し、`DatePickerMaterialContractTest` に存在・型の検証を足す
- [x] 2.3 承認 mock との視覚照合 (ksn-ui。brief.md の検証条件を含む)

## 3. 今日ジャンプ駆動

- [x] 3.1 ボタン押下時の範囲チェック: `todayProvider` の値を `minDate`/`maxDate` と `LocalDate` 日単位で比較し、範囲外は早期 return (→ Requirement: カレンダーの今日への移動 / Scenario: 今日が範囲外なら何も変更しない・min/max 当日は有効)
- [x] 3.2 経路 A: 今日の月の position 特定 (adapter stable ID 照合 + attach 済み子走査の併用) → `scrollToPosition` → 今日セルの `performItemClick` (→ Requirement: カレンダーの今日への移動)
- [x] 3.3 年選択グリッド表示中はモードトグルの `performClick` で日グリッドへ戻してから経路 A を駆動 (→ Requirement: 代替表示状態からの今日への移動 / Scenario: 年選択)
- [x] 3.4 フォールバック D: テキスト入力モード表示中・経路 A 駆動不能時は dismiss して `setSelection(today)` + `setOpenAt(today)` で再 build (Colorizer/今日ボタンの再 attach 込み。表示中かつ今日が範囲内での no-op は許容しない) (→ Requirement: 代替表示状態からの今日への移動 / Scenario: テキスト入力)
- [x] 3.5 ジャンプ処理の single-flight 化とキャンセル: dismiss・Fragment view 破棄で posted リトライを無効化し (世代番号 or キャンセルトークン)、連打は1回の実行と同じ結果にする。再 build は旧 Fragment の破棄後に行い tag を世代分離する (→ Requirement: カレンダーの今日への移動 / Scenario: 移動の完了前に閉じられたら再提示しない)

## 4. テスト (Scenario 対応)

- [x] 4.1 提示条件: todayText 指定で提示 / null・空文字で非提示 (→ Requirement: カレンダーモードの今日ショートカットの提示 の全 Scenario)
- [x] 4.2 今日への移動: 表示月・選択日の移動 / callback 不発火 / OK 確定で1回発火 / 範囲外無反応 / 境界日有効 (→ Requirement: カレンダーの今日への移動 の全 Scenario)
- [x] 4.3 代替表示状態: 年選択中 / テキスト入力中 (→ Requirement: 代替表示状態からの今日への移動 の全 Scenario)
- [x] 4.4 既存の Spinner 系 Scenario テストが回帰していないことの確認 (→ MODIFIED Requirement: 今日へのジャンプ (todayText))
- [x] 4.5 ViewHolder 公開経路の統合テスト: 固定 `todayProvider` を注入した ViewHolder を bind → 行タップ → 今日操作 → OK 確定までを、通常経路 (経路 A) と再 build 経路 (フォールバック D) の双方で実施し callback 回数を検証する (既存 DatePickerDialogIntegrationTest の picker 直接構築ではカバーされない `todayText`/`todayProvider` の結線を通す)

## 5. iOS コメント修正

- [x] 5.1 `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift` の冒頭コメント (「選択は変えない」) を実装 (表示月・選択日とも today へ移動、確定は完了ボタンの責務) に合わせて修正する。コメントのみでコード変更なし
