---
id: 0010
title: DatePickerCell カレンダーの今日ジャンプは正規クリック経路への View 階層駆動で行い、リフレクションは採らない
status: superseded
date: 2026-08-03
---

## Context

DatePickerCell のカレンダーモード (`MaterialDatePicker` / material 1.12.0) に iOS パリティの「今日」ショートカットを追加する (`todayText`)。要件は「タップで表示月・選択日とも today へ移動 (確定は既存 OK ボタンの責務のまま)」「min/max は日単位比較で、範囲外なら何もしない」。

material 1.12.0 には表示後に選択日・表示月を変更する公開 API が無い (2026-08-03 並走調査で確認): `setSelection` / `CalendarConstraints.Builder.setOpenAt` は Builder 構築時のみ、`MaterialCalendar.setCurrentMonth(Month)` は package-private、`MaterialDatePicker.calendar` / `dateSelector` は private。一方、日付セルの正規クリック経路 (MonthsPagerAdapter:124 → MaterialCalendar.onDayClick:212 → MaterialDatePicker.onSelectionChanged:513) は、validator 判定・選択変更・ヘッダ/OK ボタン更新・選択マル再描画を一括で駆動する。

前提となる注入土台: 表示後の内部 View 走査フック (DatePickerColorizer、ADR-0008) が既にあり、内部 R.id への依存は MaterialIds + 契約テストで管理する取り決めがある。

## Decision

1. **今日ジャンプは View 階層駆動で正規クリック経路に流す (経路 A)**: `mtrl_calendar_months` の RecyclerView を今日の月へスクロールし、目的月の `month_grid` (GridView) で今日のセルを `performItemClick` する。ライブラリ自身の通知チェーンに選択・ヘッダ・OK ボタンの更新を任せ、positive callback は発火させない (確定は OK ボタンの責務のまま)
2. **リフレクションは採らない**
3. **経路 A が駆動できない場合のフォールバックは picker の再 build (経路 D)**: dismiss して `setSelection(today)` + `setOpenAt(today)` で作り直す (100% 公開 API)
4. 範囲チェックはボタン押下時に `LocalDate` の日単位比較で行い、範囲外は早期 return (無反応。iOS / Spinner 実装と同型)。validator ゲート任せにしない — 先に月スクロールすると表示だけ動いてしまうため
5. 依存する内部 R.id (`mtrl_calendar_months`、`date_picker_actions`) は MaterialIds に集約し契約テストで検証する (ADR-0008 の取り決めに従う)

## Alternatives Considered

- **リフレクションで内部 API を叩く (`MaterialDatePicker.calendar` → `setCurrentMonth`、`dateSelector.select`)** — 却下。必要な要素が全て private / package-private であることに加え、本ライブラリは consumer-rules.pro を持たないため、**消費側アプリの R8 が material の内部メンバをリネームし、release ビルドのみで静かに壊れる** (デバッグでは再現しない)。さらに `dateSelector.select()` 直叩きでは `OnSelectionChangedListener` が発火せず、ヘッダ/OK ボタン更新を自前再現する羽目になり機能的にも劣る。View 走査 (R8 の影響を受けない) という ADR-0008 の方針にも逆行する
- **同一パッケージ shim (`com.google.android.material.datepicker` に自前クラスを置く)** — 却下。package-private をコンパイル時リンケージで解決でき R8 にも耐えるが、`MaterialCalendar` は `@RestrictTo(LIBRARY_GROUP)` で lint 対象となり、material 更新時の保守コストも高い。経路 A で同じ結果が出せる以上、採る理由がない
- **常に dismiss → 再 build (経路 D を本命にする)** — 却下 (フォールバックとしてのみ採用)。公開 API のみで最も安全だが、ダイアログの dismiss/show アニメーションでちらつき、Fragment tag と Colorizer の再 attach のやり直しも発生する
- **DayViewDecorator (1.7+ の公開拡張点)** — 却下。背景・文字色・drawable の装飾しかできず、選択も表示月も駆動できない

## Consequences

- 正: リフレクション不使用のため消費側 R8 の影響を受けず、ADR-0008 の「内部 View 走査 + 契約テスト」の既存方針・既存フック (DatePickerColorizer の FragmentLifecycleCallbacks) にそのまま相乗りできる
- 正: 正規クリック経路を通すため、ヘッダ・OK ボタン・選択マルの状態追随をライブラリに任せられ、自前再現コードを持たない
- 負: material 内部契約への依存が増える (`mtrl_calendar_months` の RecyclerView 構造、`month_grid` の adapter が UTC epoch ms を item に持つこと、`date_picker_actions` のボタン行)。MaterialIds 契約テストへの追加が義務
- 負: `scrollToPosition` 後に目的月の ViewHolder が bind されるタイミングは机上で確定できない (post + 回数上限リトライで吸収する前提)。この待ち合わせの成立は実装前の Robolectric スパイクで実測して確定する
- 注: フルスクリーンモードには `date_picker_actions` が存在しないため、ボタン設置は「無ければ `confirm_button` の親」フォールバックを持つ。テキスト入力モードの扱い (no-op かボタン非表示か) は提案フェーズで確定する

出典: kasane/changes/datepickercell-today-shortcut/exploration.md (2026-08-03 の ksn-dual-research 並走調査: codex × ksn-researcher 双方一致、material-1.12.0 ソース JAR / AAR res の静的読解) / android/ADR-0008 (View 走査 + 契約テスト方針の原決定)
