---
type: concept
title: TimePickerCell の選択面
description: TimePickerCell の行タップで開く時刻選択 UI の契約 (Android のボトムシート + 時・分ホイール・12/24h の決定・確定と破棄) と iOS との対比
tags: [cells, time-picker, selection-surface, styling]
timestamp: 2026-08-28
---

# TimePickerCell の選択面

この文書は、`TimePickerCell` の行タップで開く時刻選択 UI (以下「選択面」) の挙動契約を説明する。想定読者はライブラリの実装者と、挙動契約を知りたい利用者の両方である。読むと、Android の選択面の器 (ボトムシート + 時・分ホイール)、12 時間制 / 24 時間制の決定方法、確定と破棄の意味論、iOS との違いが分かる。`TimePickerCell` のモデル — `time` / `format` / `is24Hour`、選択面のタイトル `pickerTitle`、確定通知の `onValueChanged` — は [入力 Cell](input-cells.md)、器・ヘッダー・スナップ静止・アクセシビリティの共通契約は [NumberPickerCell の選択面](number-picker-selection-surface.md) を先に読むと分かりやすい。

## 目的

選択面は「時刻を選び、確定して初めて反映される」体験を提供する境界である。Android はホスト Activity の型と XML テーマに関わらず同一の選択面を提示し (Fragment 機構を持たない `ComponentActivity` でも動作する — [android/ADR-0018](../../../decisions/android/0018-timepickercell-bottom-sheet-wheel-unification.md))、iOS と同じ「ホイールで選ぶ」操作感に揃える。

## 提示の器

| プラットフォーム | 器 | 実装の入口 |
|---|---|---|
| iOS | 埋め込み `UIDatePicker` (時刻モード) を `inputView` 経由でキーボード位置にスライドアップ表示。ツールバー (Cancel / タイトル / Done) 付き | `TimePickerCellView` |
| Android | ボトムシート + 時・分ホイール。器の意匠 (ドラッグハンドル + キャンセル/タイトル/確定ヘッダー + ホイール行) は `NumberSelectionSheet` / `DateSelectionSheet` と同系 | `TimePickerCellViewHolder` + `TimeSelectionSheet` + `KsWheelView` |

Android の旧 UI (`MaterialTimePicker` の時計ダイヤルダイアログ) は廃止されており、キーボードによる時刻入力モードは提供しない (android/ADR-0018 の負の帰結として合意済み)。`TimeSelectionSheet` と `KsWheelView` は internal の内部部品であり、公開契約は Cell model と選択面の挙動だけである。

## 共通の挙動契約

- 提示: `isEnabled` な TimePickerCell の行タップで開く。`isEnabled = false` はタップ無効
- タイトル: `pickerTitle` があればそれ、なければ `title` で解決する
- 初期選択: 開いた時点の `cell.time`
- 確定のみ反映: 確定操作で、その時点の選択 (時・分・(12 時間制では) 午前/午後) から作った値を引数に `onValueChanged` を1回発火して閉じる。非確定の閉じ方 (キャンセル・外側タップ・Back・下スワイプ等、器が提供するすべての経路) では発火せず、変更は破棄される。Android の確定値は `LocalTime`、iOS は元の `cell.time` の日付成分を保持した `Date`

## 時制の決定と候補系列

選択面の時制 (12/24 時間制) は、両 platform とも **`is24Hour` (Bool・既定 `true` = 24時間制) が唯一の決定源**である ([core/ADR-0028](../../../decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md))。`format` は行の valueText の文字列化 (表示責務) にのみ効き、時制判定には一切関与しない。端末の 24 時間設定にも依存しない — 同じ Cell 構成なら、どの端末でも同じ時制の選択面が提示される。`format` と `is24Hour` の食い違い (例: `format = "h:mm a"` かつ `is24Hour = true`) は検証・フォールバックせず利用者責任である。

Android の候補系列:

- `is24Hour = true` (既定): 24 時間制 — 時 0–23 / 分 0–59 の2系列 (系列 = 選択面を構成する各ホイール。[DatePickerCell の選択面](date-picker-selection-surface.md) と共通の語)。系列順は 時・分 固定
- `is24Hour = false`: 12 時間制 — 時 1–12 / 分 0–59 / 午前・午後 の3系列。**系列の順序は端末 Locale の 12 時間表記パターンに従う** (ja 等の前置き locale では 午前/午後・時・分、en 等では 時・分・AM/PM) — iOS の埋め込み picker が locale で列順を組み替えるのに合わせた提示揃え。深夜 0 時台は「12 + 午前」、正午台は「12 + 午後」として提示・往復する
- 午前/午後のラベルは端末 Locale の表記から導出する (自前の翻訳文字列は同梱しない)

iOS の埋め込み picker も時制は `is24Hour` で決まる。強制は hour cycle のみに作用させ、AM/PM 等の**表記の言語・地域は端末 Locale 由来を保つ** (`HourCycleLocale` が基準 Locale の hourCycle だけを差し替える)。

スナップ静止 (ホイールが候補位置で止まって初めてその候補が選択中になる) の意味論・候補領域の下スワイプが dismiss にならないこと・アクセシビリティ (系列ごとの選択中公開と前後候補アクション)・操作ラベル (OS の公開文字列リソース) は [NumberPickerCell の選択面](number-picker-selection-surface.md) と同じ契約である。配色も既存シート系と同じ styling 解決に従う (ヘッダー操作・選択帯の強調 = accent の3段解決 `TimePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`、シートの面 = `Theme.backgroundColor`)。

## 構成変更・ホスト破棄の挙動 (Android)

- 選択面は構成変更 (回転等) による Activity 再生成後に再提示されない (シート系選択面の共通契約。このとき `onValueChanged` は発火しない)。旧 Material ダイアログは Fragment 機構で復元されていたため、これは android/ADR-0018 に記録された利用者可視の挙動変更である
- ホスト (Activity) の破棄には追随して閉じる — 回転や finish で window leak を起こさない

## 保証すること

- 確定 callback は確定操作の1回だけ発火し、非確定 dismiss はどの経路でも発火しない — これが崩れると、利用者アプリの状態が「開いて閉じただけ」で書き換わる
- 12 時間制の境界 (深夜 0 時台 / 正午台) の提示と確定値の往復は無損失である — `time = 00:30` を開いてそのまま確定すれば `LocalTime.of(0, 30)` が届く
- 時制は端末設定にも `format` にも依存せず `is24Hour` だけで決まる — 同じ Cell 構成なら、どの端末でも同じ時制の選択面が提示される (core/ADR-0028)

## してはいけないこと

- `TimeSelectionSheet` / `KsWheelView` を公開 API として利用者に案内しない — internal の内部部品である
- 時制を `format` 文字列や端末の 24 時間設定 (`DateFormat.is24HourFormat`) から決めると仮定しない — 決定源は `is24Hour` だけである (core/ADR-0028)
- 選択面がキーボード入力モードを持つと想定しない — 旧 Material ダイアログの入力モードは器の置換で廃止済みである

## 用語

- **選択面** / **器** / **系列** / **スナップ静止**: [DatePickerCell の選択面](date-picker-selection-surface.md)・[NumberPickerCell の選択面](number-picker-selection-surface.md) と共通の語

## 関連

- [入力 Cell](input-cells.md) — `TimePickerCell` のモデルとプラットフォーム差の一覧
- [NumberPickerCell の選択面](number-picker-selection-surface.md) — 器・ヘッダー・スナップ静止・アクセシビリティ契約の共有元
- [DatePickerCell の選択面](date-picker-selection-surface.md) — 同系の日付選択シートとカレンダーダイアログ
- [スタイルの所有と実効値解決](../styling/style-resolution.md) — 実効値解決の一般規則
- [android/ADR-0018](../../../decisions/android/0018-timepickercell-bottom-sheet-wheel-unification.md) — 全ホストをボトムシート + 時分ホイールに統一した決定
- [core/ADR-0028](../../../decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md) — 時制の決定源を is24Hour に一本化した決定
