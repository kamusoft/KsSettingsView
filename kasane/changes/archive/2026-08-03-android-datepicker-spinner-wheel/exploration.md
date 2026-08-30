# Exploration: android-datepicker-spinner-wheel

## 課題 / 動機

Android 版 DatePickerCell の `DatePickerUIStyle.Spinner` は不具合により意図した UI として起動しない。実体は `AlertDialog` + `android.widget.DatePicker` + `calendarViewShown = false` の弱い実装で、Material テーマ環境では spinner 表示に切り替わらずカレンダーが表示されてしまう (土台ウィジェットの限界、android/ADR-0007 Context で既知)。

archive/android-numberpicker-modern-ui (ADR-0007) で確立した「ボトムシート + 自作ホイール `KsWheelView`」方式で置き換え、モダンホイールが起動するようにする。同 proposal の Non-Goals で予告されていた続編 change にあたる。

## 調査結果 (2026-08-02、ksn-scout 委譲 + 直接確認)

- 「起動しない」の正体はクラッシュではなく表示不発: `DatePickerCellViewHolder.kt:110-138` の `showSpinnerDatePicker` はダイアログを表示するが、カレンダー実装が出てしまう
- `KsWheelView` (KsWheelView.kt:84、internal) は「候補件数 + index→表示文字列関数 + initialIndex + スタイル」だけを受け取る汎用設計。コメントで「将来 DatePicker ホイール版へ展開する前提」と明記。ただし**単一列専用**で `itemCount` は不変
- 器の `NumberSelectionSheet` はホイール1個専用。3連には DatePicker 専用シートの新設が必要
- 確定契約: `NumberSelectionSheet` は確定でのみ `onConfirmed` を1回発火。取消・外側タップ・Back・下スワイプでは発火しない
- `DatePickerCell.kt:39-40` に `minDate` / `maxDate` が定義済み
- iOS 側: `.wheels` は inline 埋め込み型 (`UIDatePicker`)。`DatePickerCell.todayText: String?` (DatePickerCell.swift:44) のオプトインで「今日」ボタンが出る。今日が min/max 範囲外なら何もしない安全弁あり (DatePickerCalendarSheetController.swift:190)。Android にはこのプロパティが無い (パリティ欠落)
- `KsWheelView` に外部ジャンプ用 API は無いが、アクセシビリティ操作用の `scrollToPositionWithOffset` + 選択更新の内部機構あり (KsWheelView.kt:331-343) — 「今日」ジャンプの追加コストは小さい

## 検討した選択肢 (却下案と理由を含む)

- **採用: ボトムシート + 3連 `KsWheelView` (年/月/日)** — ADR-0005/0007 の既定路線に乗り、器・スタイル解決 (`PickerSheetStyle` / `KsWheelStyle`)・確定契約をそのまま流用できる
- **却下: `android.widget.DatePicker` の spinner 表示を成立させる修正** — 土台ウィジェットの限界 + Holo 見た目はオーナーの刷新方針に反する
- **却下: iOS `.wheels` と同じ inline 埋め込み型** — Android はボトムシート路線の一貫性を優先 (ユーザー確認 2026-08-02)

## 決定事項

- 置き換え先はボトムシート + 3連ホイール — ユーザー確定 (2026-08-02)
- 範囲外日は月末日に丸める (1/31 → 2月 → 2/28、iOS `UIDatePicker` 標準挙動と揃える)
- `minDate` / `maxDate` をホイール候補の範囲制限として尊重する
- `todayText: String? = null` を iOS と同名・同意味論で Android `DatePickerCell` に公開プロパティ追加し、本変更のスコープに含める — ユーザー確定 (2026-08-02)。互換性契約は numberpicker の `unit` と同じソース互換 (ABI 非保証)
- 「今日」タップはホイール位置を動かすだけで、`onValueChanged` は確定時のみ発火

## ADR 候補

- 作成済み: android/ADR-0009 (accepted、2026-08-02 ユーザー承認) — DatePickerCell (Spinner) の選択 UI はボトムシート + 3連自作ホイールで実装する

## 未決の論点

- 日ホイールの候補数追随の方式 (ホイール再生成 or `itemCount` 可変化改修) → 実装フェーズで決定
- 3連ホイールの列順・ラベル表記 (「2026年 / 8月 / 2日」等) と「今日」ボタンの配置 → ksn-propose のモックで比較・確定
- Compose DSL (InputCellDsl.kt 相当) への `todayText` 追加要否 → propose 時に確認

## UI 素材 (ui/references/)

- なし (現時点で貼付画像なし。参考: archive/android-numberpicker-modern-ui/ui/ のモック・検証スクショが同系の器の見た目の先例)

## 変更級の推奨: M (理由)

公開 API 追加 (`todayText`) + 選択 UI の刷新 + UI 変更としてモック承認ゲートを通すため。触る範囲は DatePickerCell 系 + シート新設 + `KsWheelView` の内部 API 追加に閉じ、先行 android-numberpicker-modern-ui (M 級) と同規模・同構図。
