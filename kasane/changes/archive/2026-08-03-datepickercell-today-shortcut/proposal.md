# Proposal: datepickercell-today-shortcut

## Why

iOS の DatePickerCell カレンダーモードには「今日」ショートカット (`todayText`) があるが、Android のカレンダーモード (MaterialDatePicker) には無く、platform parity が欠けている。`todayText` 公開 API と Spinner モードの今日ジャンプは archive/android-datepicker-spinner-wheel が先行実現済みで、同 proposal が「Material への todayText 展開は別 change」と予告していた続編にあたる。

## What Changes

- **今日ボタンの注入**: `todayText` 指定時のみ、MaterialDatePicker のボタン行 (`date_picker_actions`) の先頭に「今日」ボタンを追加する。注入は DatePickerColorizer の既存フック (FragmentLifecycleCallbacks、android/ADR-0008) に相乗りする。フルスクリーンモードでボタン行が無い場合は `confirm_button` の親へフォールバック
- **今日ジャンプ**: タップで表示月・選択日とも today へ移動する (android/ADR-0010 の経路 A: 月 RecyclerView をスクロールし今日セルを `performItemClick` で正規クリック経路に流す)。値の確定は既存 OK ボタンの責務のまま。今日が min/max 範囲外なら日単位の事前比較で無反応
- **フォールバック**: 経路 A が駆動できない場合 (テキスト入力モード表示中・グリッド未発見) は picker を dismiss して `setSelection(today)` + `setOpenAt(today)` で再 build する (ADR-0010 の経路 D)。年選択グリッド表示中はモードトグルの `performClick` で日表示へ戻してから経路 A を駆動
- **契約テスト**: 依存する内部 R.id (`mtrl_calendar_months` / `date_picker_actions`) を `MaterialIds` に追加し `DatePickerMaterialContractTest` で検証
- **スパイク先行**: `scrollToPosition` 後の月グリッド bind 待ち合わせ (机上で確定できない唯一の論点) を Robolectric スパイクとしてタスク先頭に置く
- **iOS コメント修正**: DatePickerCalendarSheetController.swift の陳腐化した冒頭コメント (「選択は変えない」) を実装 (表示月・選択日とも移動) に合わせて修正する。コメントのみでコード変更なし

影響する能力: `settings-view-android-ui` (DatePickerCell Material モード)

## Non-Goals

- 公開 API の変更 (`todayText` / `todayProvider` は既存のまま消費するだけ)
- Spinner モード (DateSelectionSheet) の挙動変更
- iOS / MAUI / Compose DSL の実装変更 (iOS はコメント修正のみ)
- material-components のバージョン変更 (1.12.0 固定は ADR-0006/0008 の前提)

## Impact

- **公開 API**: 変更なし。`todayText` 指定済みの利用者はカレンダーモードでもボタンが出るようになる (オプトイン挙動の充足であり破壊的変更ではない)
- **視覚変更**: `todayText` 指定時のみカレンダーダイアログにボタンが1つ増える。未指定 (既定) は現状維持
- **内部依存**: material 内部契約への依存が増える (`mtrl_calendar_months` の構造・`month_grid` adapter の UTC ms 契約・`date_picker_actions`)。MaterialIds + 契約テストで管理 (ADR-0008 の取り決め)
- **タイムゾーン**: 「今日」は端末 TZ (`todayProvider` = `LocalDate.now()`) で判定する。Spinner モードと同一の採択。material が今日の枠線を描く基準 (UTC) とは日付境界付近でズレ得ることを意識的に許容する
- **リスク**: スクロール後 bind の待ち合わせタイミング。スパイクで確定し、表示中かつ今日が範囲内で経路 A が駆動できない場合は必ずフォールバック D で成立させる (仕様上の無反応は「今日が範囲外」と「移動完了前に選択 UI が閉じられた」場合のみ。クラッシュさせない)

## 級: M

公開 API 追加なしだが、material 内部契約への依存追加 + 契約テスト + スパイク + UI (ボタン注入とモック照合) を含む機能追加のため。

domain: android
(iOS はコメントのみの修正で挙動変更も蒸留対象の知識も生まないため、cross ではなく android とする — 2026-08-03 オーナー確定)
