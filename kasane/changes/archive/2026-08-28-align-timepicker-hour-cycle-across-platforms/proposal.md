# Proposal: align-timepicker-hour-cycle-across-platforms

## Why

TimePickerCell の選択面の時制 (12/24時間制) の決定源が platform 間で非対称だった — Android は `format` 文字列の `a` の有無、iOS は端末の設定 (地域・24時間表示スイッチ)。表示責務である `format` と選択 UI の機能 (時制) が混在し、iOS では「行は 2:30 PM・ホイールは 0–23」という不整合も起き得た。12時間制デモも端末設定に依存しない Android にしか常設できず、sample-parity が破れていた。

core/ADR-0028 で「時制は `is24Hour` (Bool・既定 true=24時間制) を唯一の決定源とし、`format` は表示専用へ戻す」と決定済み。本変更はその実装である。

## What Changes

- `is24Hour: Bool` (既定 `true` = 24時間制) を TimePickerCell に追加: Android native / iOS native / Compose・SwiftUI bridge / MAUI facade (snapshot・gateway・iOS binding assembly 含む)。Compose の TwoWay DSL 拡張関数にも引数として追加
- `is24Hour` を各層の更新検知 (Android/iOS の equality、iOS の DSL 再構築 helper、MAUI の snapshot 差分判定) に参加させ、表示済み Cell の変更が次回提示に反映されることを保証
- Android: `format` の `a` 有無による時制判定を選択面の決定源から撤去 (`is24Hour` に置換)。系列構成・AM/PM ラベルの Locale 解決・12h 境界の丸めは現行契約を維持
- iOS: 埋め込み picker の時制を `is24Hour` で決定し、端末設定依存を廃止
- サンプル3面に 12時間制デモセルを常設 (Android は既存デモを `is24Hour = false` 明示へ移行、iOS / MAUI は新設) — sample-parity の解消
- 影響能力: cell-types-input / android-timepicker / ios-timepicker / maui-cells / maui-bridge / samples-android / samples-ios / samples-maui

## Non-Goals

- DatePickerCell など他の日時系 Cell への時制フラグ展開 — 選択面に時制の概念を持つのは TimePickerCell のみ (別能力)
- `format` と `is24Hour` の整合検証・警告 — ADR-0028 で「食い違いは利用者責任」と決定済み
- AM/PM 表示文言の解決方式の変更 — 端末 Locale 由来の現行契約を維持 (時制の決定と表記の解決は別問題)
- concepts / skills / README の追従書き換え — 蒸留 (ksn-distill) と docs-refresh の責務

## Impact

- **破壊的変更あり**: Android で `format = "h:mm a"` により 12時間ホイールを得ていた利用は `is24Hour = false` の明示が必要になる。iOS は端末が 12時間表示設定でも既定は 24時間ホイールになる (端末設定への追従をやめる)
- iOS の時制強制は hour cycle のみに作用させ、AM/PM 等の表記の言語・地域は端末 Locale 由来を保つ (契約として ios-timepicker スペックに明記。表記言語まで変える実装は不合格)
- ui/ は作らない: 選択面は既存ホイール UI の系列切替のみで、新規の視覚要素・レイアウト判断がないため

## 級: M

3面 + bridge の公開 API 追加と破壊的変更を伴うが、単一 Cell の単一機能でモック不要。複数 capability 横断だが同形の前例 (add-entrycell-placeholder-color) の M 運用に準拠 — spec-review の指摘を受けオーナー裁定で M 維持を確定 (second-opinion-spec-001.md)。

domain: cross
