---
id: 0009
title: DatePickerCell (Spinner) の選択 UI はボトムシート + 3連自作ホイールで実装する
status: accepted
date: 2026-08-02
---

## Context

Android 版 DatePickerCell の `DatePickerUIStyle.Spinner` は、`AlertDialog` + `android.widget.DatePicker` に `calendarViewShown = false` を設定して「スピナーに近い見た目」を狙う弱い実装だった (DatePickerCellViewHolder.kt の `showSpinnerDatePicker`)。しかし API 21+ の `DatePicker` は XML の `android:datePickerMode="spinner"` を明示しない限り Material テーマ環境ではカレンダー実装が使われ、プログラム生成では spinner 表示に切り替わらない — 土台ウィジェット自体の限界であり、Spinner 指定でもカレンダーが表示されてしまう不具合として顕在化していた (android/ADR-0007 の Context でも既知事実として記録済み)。

一方、NumberPickerCell の刷新 (archive/android-numberpicker-modern-ui、android/ADR-0007) で「ボトムシート + 自作ホイール (`KsWheelView`: RecyclerView + LinearSnapHelper)」の方式が確立し、`KsWheelView` は「候補件数 + index→表示文字列関数」だけを受け取る汎用設計で「将来 DatePicker ホイール版へ展開する前提」と明記されていた。同 proposal の Non-Goals でも「DatePicker ホイール版への展開は続編 change」と予告されており、本決定はその続編にあたる。

iOS 側の対応物は `.wheels` (キーボード位置に埋め込む inline 型の `UIDatePicker`) で、`todayText: String?` 指定時のみ「今日」ボタンが出るオプトイン仕様を持つ (DatePickerCell.swift)。Android の `DatePickerCell` には `todayText` が存在せず、プロパティパリティが欠落している。

## Decision

`DatePickerUIStyle.Spinner` の選択 UI を、`AlertDialog` + `android.widget.DatePicker` からボトムシート + 自作ホイールに置き換える:

1. **器**: DatePicker 専用シート (DateSelectionSheet 仮) を新設する。構成は `NumberSelectionSheet` と同系 (ドラッグハンドル + `SheetHeaderView` [取消/タイトル/確定] + コンテンツ) で、コンテンツに `KsWheelView` を年/月/日の3連で横に並べる
2. **日数変動**: 年/月ホイールの変更に応じて日ホイールの候補数を動的に追随させる (2月・30日月)。範囲外になった選択日は月末日に丸める (例: 1/31 → 2月 → 2/28。iOS `UIDatePicker` の標準挙動と揃える)
3. **日付範囲**: `DatePickerCell.minDate` / `maxDate` をホイール候補の範囲制限として尊重する (iOS はネイティブが尊重するためパリティ上必須)
4. **todayText パリティ**: `DatePickerCell` に `todayText: String? = null` を iOS と同名・同意味論 (null なら非表示のオプトイン) で追加する。タップで3連ホイールを今日の位置へスクロールする。今日が min/max 範囲外なら何もしない (iOS と同じ安全弁)
5. **挙動契約**: 確定操作でのみ `onValueChanged` を1回発火し、取消・外側タップ・Back・下スワイプでは発火しない (`NumberSelectionSheet` と同一契約)。「今日」タップはホイール位置を動かすだけで発火しない

スタイル解決は既存の `PickerSheetStyle` / `KsWheelStyle.from(sheetStyle)` の規則をそのまま踏襲する。

## Alternatives Considered

- **`android.widget.DatePicker` の spinner 表示を成立させる方向での修正** — 却下。XML の `datePickerMode="spinner"` 指定にはレイアウトリソース/テーマ設定の追加が必要な上、Material テーマ環境での表示切り替えは土台ウィジェットの限界に阻まれる (ADR-0007 Context で既知)。Holo 時代の見た目に戻すこと自体もオーナーの刷新方針に反する
- **iOS `.wheels` と同じ inline 埋め込み型 (キーボード位置に出す)** — 却下。iOS とは形が厳密には違うが、Android 側は ADR-0005 (ボトムシート) / ADR-0007 (自作ホイール) でボトムシート路線が確立しており、器・ヘッダー・スタイル解決・確定契約をそのまま流用できる一貫性を優先する (2026-08-02 ユーザー確認)

## Consequences

- 正: Spinner モードが「カレンダーが出てしまう」不具合状態から脱し、意図したホイール UI として機能する
- 正: 器・スタイル解決・確定コールバック契約が NumberPickerCell と対称になり、学習コスト・保守コストが下がる
- 正: `todayText` のプロパティパリティ欠落が解消される
- 負: 公開 API 追加 (`todayText`) により、data class の constructor / `copy` シグネチャが変わる。互換性の契約はソース互換 (named 引数・既定値前提) で、ABI 互換は保証対象外 (numberpicker 変更の `unit` と同じ契約)
- 負: `KsWheelView` に内部 API の追加が必要 (指定 index へのスクロール、日ホイールの候補数追随)。候補数追随の方式 (ホイール再生成 or itemCount 可変化) は実装フェーズで決める
- 注: 3連ホイールの列順・ラベル表記・「今日」ボタンの配置は見た目の領分であり、ksn-propose のモック承認で確定する

出典: kasane/changes/android-datepicker-spinner-wheel/exploration.md (2026-08-02 の探索議論・ksn-scout 調査) / android/ADR-0005・ADR-0007 (器とホイールの原決定) / archive/android-numberpicker-modern-ui/proposal.md (続編予告)
