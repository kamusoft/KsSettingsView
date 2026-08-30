---
id: 0028
title: TimePickerCell の時制は is24Hour を唯一の決定源とし format は表示専用とする
status: accepted
date: 2026-08-28
---

## Context

TimePickerCell の選択面 (ホイール) の時制 (12/24時間制) の決定源が platform 間で非対称だった:

- **Android**: `format` 文字列の引用符外の小文字 `a` の有無で判定 (`TimeSelectionSheet.kt` の `timeFormatUsesAmPm`)。旧 MaterialTimePicker 実装の踏襲で、relax-android-host-prerequisites で契約として明文化されていた。
- **iOS**: `UIDatePicker(.time)` に locale を設定しておらず、端末の設定 (地域と24時間表示スイッチ) で決まる。`format` は行の valueText の文字列化にしか効かない。

このため iOS では「行の表示は 2:30 PM なのにホイールは 0–23」という組み合わせが起き得た。また 12時間制デモセルは端末設定に依存せず成立する Android サンプルにのみ常設でき、iOS / MAUI への追随は保留されていた (sample-parity の追跡付き片側先行)。

根本の問題は、**表示責務である `format` と、選択 UI の機能 (時制) という別の責務が混ざっていた**こと。format はあくまで「どう表示するか」であり、12時間制という UI 機能を制御するものではない。

## Decision

TimePickerCell 自体に時制フラグ **`is24Hour: Bool` (既定 `true` = 24時間制)** を持たせ、これを選択面の時制の**唯一の決定源**とする。3面 (MAUI facade / Android / iOS) 共通:

- `format` は行の valueText の文字列化 (表示責務) にのみ使う。時制判定には一切関与しない。
- Android の「`a` の有無で時制判定」ロジックは決定源としては完全撤去する。
- iOS の端末設定依存を廃止する。ホイールの時制は端末の地域・24時間表示設定に依らず `is24Hour` で決まる。
- `format` と `is24Hour` が食い違う指定 (例: `format = "h:mm a"` かつ `is24Hour = true`) は検証・フォールバックせず、利用者の責任とする。
- 既定 `true` は既定 `format = "HH:mm"` と噛み合い、フラグ未指定の既存利用の見た目を変えない。

## Alternatives Considered

- **enum (`HourCycle.h12 / .h24`) で表す**: 却下。時制は「12 か 24 か」の二値で完結し、「端末に従う」という第3の値は本決定で廃止する思想のため、enum で拡張に備える意味が薄い。3面 + bridge に enum の輸送定義が増えるコストも Bool に劣る。
- **フラグ未指定時のみ format から推定するフォールバック (nullable フラグ)**: 却下。「未指定なら format が機能を制御する」という廃止したい責務混在が半分残る。「未指定」を表すための nullable 化で 3面 + bridge の契約も濁る。既存の `format = "h:mm a"` 利用箇所は `is24Hour = false` の明示で移行する (破壊的変更として受容)。
- **iOS を format 駆動に揃える (Android 方式へ統一)**: 却下。platform 間の対称は得られるが、表示責務と UI 機能の混在が両 OS で固定化される。
- **Android を端末設定駆動に寄せる (iOS 方式へ統一)**: 却下。セルの UI 機能が端末設定という外部状態に依存し、開発者が時制を決定できない。デモ・テストも端末状態依存になる。
- **「意図的な platform 差」として concepts に明文化して維持**: 却下。同じ指定で選択面の時制が platform により違う状態は、プラットフォーム間で仕様・動作を統一する製品目的に反する。

## Consequences

- 正: 3面で同じ指定が同じ選択面になり、MAUI facade から見た契約が対称になる。
- 正: `format` が純粋な表示責務に戻り、責務分離が明確になる。
- 正: 時制が端末状態に依存しなくなり、12時間制デモセルを全サンプルに常設できる (sample-parity の解消)。
- 負: 破壊的変更 — Android で `format = "h:mm a"` により 12時間ホイールを得ていた既存利用者は `is24Hour = false` の明示が必要になる。
- 負: iOS では端末が 12時間表示設定でも既定は 24時間ホイールになる (端末設定への追従をやめる)。
- 負: concepts (time-picker-selection-surface.md) の時制契約の書き換え、Android の時制判定テストの改修、bridge DTO / snapshot の拡張が必要。

出典: kasane/changes/align-timepicker-hour-cycle-across-platforms/exploration.md / 探索の会話中の議論 (2026-08-28)
