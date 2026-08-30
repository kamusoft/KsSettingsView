# Exploration: datepickercell-today-shortcut

datepickercell-color-adjust の探索から切り出した機能追加 (2026-08-02)。2026-08-03 に議論を再開し、駆動経路のスパイク調査 (ksn-dual-research) を完了。

## 課題 / 動機

iOS の DatePickerCell には「今日」ショートカットボタンがある (`todayText: String?`、AiForms `TodayText` 互換)。Android のカレンダーモード (MaterialDatePicker) には相当機能がなく、platform parity が欠けている。

## iOS 側の確定仕様 (ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift で確認)

- `todayText` が `nil` / 空なら非表示。指定時はカレンダーモードの下部ボタンバー (キャンセル/完了の並び) に表示
- タップで**表示月・選択日とも today へ移動** (「選択状態にかかわらず今日のページに移動すべき」がオーナー確認済み仕様)。確定は「完了」ボタンの責務のまま
- min/max の範囲チェックは**日単位**で比較し、today が範囲外なら何もしない (時刻成分まで比較すると `maximumDate: Date()` 指定で弾かれる罠への対処)
- 注: 同ファイルの冒頭コメント (「選択は変えない」) は陳腐化しており実装と矛盾 (2026-08-03 発見)。**修正は本 change のタスクに含める** (2026-08-03 ユーザー確定。コメントのみの修正でコード変更なし)

## スコープの現状 (2026-08-03 再確認 — android-datepicker-spinner-wheel が一部を先行実現)

- `DatePickerCell.todayText` 公開 API: **追加済み** (DatePickerCell.kt:42)。本変更での公開 API 追加はもう無い
- Spinner モード: **実装済み** (DateSelectionSheet の今日 chip。ジャンプのみ・範囲外無反応・日単位比較で iOS 仕様と一致)
- `todayProvider` (テスト注入点): DatePickerCellViewHolder に既存
- **残スコープはカレンダーモード (MaterialDatePicker) のみ**: ボタン注入 + 今日ジャンプ駆動

## 並走調査結果 (2026-08-03 ksn-dual-research: codex × ksn-researcher、双方一致 🟢)

- material 1.12.0 に表示後の選択日・表示月を変える公開 API は無い (`setSelection` / `setOpenAt` は Builder 時のみ、`MaterialCalendar.setCurrentMonth` は package-private)
- **本命 (経路 A)**: `mtrl_calendar_months` の RecyclerView を今日の月へスクロール → `month_grid` (GridView) で今日のセルを `performItemClick`。ユーザー実タップと同一の正規経路 (MonthsPagerAdapter:124 → MaterialCalendar.onDayClick:212 → MaterialDatePicker.onSelectionChanged:513) が走り、選択・ヘッダ・OK ボタン・選択マルが全部ライブラリ自身の通知チェーンで追随。positive callback は発火しない。リフレクション不要
- リフレクション経路の決定的却下理由: 本ライブラリは consumer-rules.pro を持たず、**消費側アプリの R8 が material の private メンバをリネームするため release ビルドのみで静かに壊れる**
- フォールバック (経路 D): dismiss → `setSelection(today)` + `setOpenAt(today)` で再 build (100% 公開 API、ちらつきあり)
- 唯一の実測ポイント: `scrollToPosition` 後に目的月 ViewHolder が bind されるタイミング。`post` + 回数上限リトライで吸収し、提案フェーズの Robolectric スパイクで確認
- 実装メモ: 月位置は adapter stable ID (月初 UTC ms) 照合 + attach 済み子走査の併用が堅牢 / `notifyDataSetChanged` が走るため pre-draw フック内から呼ばない / ボタン設置は `date_picker_actions` の index 0 (フルスクリーンモードには無いので `confirm_button` の親へフォールバック) / `mtrl_calendar_months` `date_picker_actions` を MaterialIds 契約テストへ追加 (`month_grid` は登録済み)

## 決定事項

- datepickercell-color-adjust には含めず別変更とする (2026-08-02 ユーザー確定)。実装順序は color-adjust が先 → 完了済み。走査フック (DatePickerColorizer、android/ADR-0008) は利用可能
- **駆動経路は経路 A (View 階層駆動 + performItemClick) を採用、経路 D (再 build) をフォールバックとする** (2026-08-03 ユーザー確定 → android/ADR-0010 として起票)
- min/max 範囲外時はボタン側の事前 `LocalDate` 日単位比較で早期 return (無反応・減光なし。iOS / Spinner 実装と同型)
- ボタンの見た目の先例: Spinner の今日 chip (accent 色 outline)。カレンダー側の配置・配色はモックで確定

## ADR 候補

- **起票済み: android/ADR-0010** (今日ジャンプは正規クリック経路への View 階層駆動、リフレクション不採用) — status: accepted (2026-08-03 ユーザー承認)

## 未決の論点 (提案フェーズで確定)

- テキスト入力モード (MaterialTextInputPicker) の扱い: no-op かボタン非表示か
- タイムゾーンの採択の明文化: material の「今日」枠線は UTC 基準、`todayProvider` は端末 TZ。Spinner と揃えて端末 TZ 継続が自然だが意識的に記録する
- ボタンの配色ロール・配置の最終形 (モックで確定)
- 年選択グリッド表示中の駆動 (`month_navigation_fragment_toggle` を performClick して日モードへ戻す — 公開 API のみで可)

## UI 素材

- なし (現時点で貼付画像なし。モックは ksn-propose で作成)

## 変更級: M (2026-08-03 ユーザー確定)

公開 API 追加は解消済みだが、material 内部契約への依存追加 + 契約テスト + Robolectric スパイク + UI (ボタン注入とモック照合) を含むため。
