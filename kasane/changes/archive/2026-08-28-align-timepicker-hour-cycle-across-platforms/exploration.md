# Exploration: align-timepicker-hour-cycle-across-platforms

起票日: 2026-08-27 / 起票元: relax-android-host-prerequisites の実装中 (12時間制デモの platform parity 確認で発覚、オーナー判断で起票) / 探索: 2026-08-28

## 課題 / 動機

TimePickerCell の選択面の時制 (12/24時間制) の決定源が platform 間で非対称になっている。

- **Android**: `format` 文字列に AM/PM パターン文字 `a` を含むかで決まる (relax-android-host-prerequisites で明文化。旧 MaterialTimePicker 実装の踏襲)
- **iOS**: `UIDatePicker(.time)` が locale 非上書きのため**端末の設定** (地域と24時間表示スイッチ) で決まる。`format` は行の valueText の文字列化にしか効かない (ios/Sources/KsSettingsViewUI/TimePickerCellView.swift の datePicker 初期化部)

このため iOS では「行の表示は 2:30 PM なのにホイールは 0–23」という組み合わせが起き得る。また 12時間制デモセル (`format = "h:mm a"`) は samples/android にのみ常設されており (視覚検証の都合)、iOS / MAUI サンプルへの追随は端末設定次第でデモとして成立しないため保留した (sample-parity の「追跡付き片側先行」として relax-android-host-prerequisites の deviation.md に記録済み)。

オーナーの整理 (2026-08-28): 根本の問題は表示責務の `format` と UI 機能 (時制) の混在。format はあくまで「どう表示するか」であり、12時間制の機能を制御するものではない。

## 検討した選択肢 (却下案と理由を含む)

採用案と却下案の全文は core/ADR-0028 に記録。要点:

- **採用: TimePickerCell に `is24Hour: Bool` (既定 true=24h) を新設し、時制の唯一の決定源とする**。format は表示専用へ。
- 却下: enum (`HourCycle`) — 二値で完結し「端末に従う」第3値は廃止する思想のため拡張に備える意味が薄い
- 却下: フラグ未指定時の format 推定フォールバック (nullable) — 責務混在が半分残り、nullable 化で契約が濁る
- 却下: iOS を format 駆動へ統一 — 責務混在の固定化
- 却下: Android を端末設定駆動へ統一 — UI 機能が外部状態依存になり開発者が制御できない
- 却下: 意図的な platform 差として明文化 — 仕様統一の製品目的に反する

## 決定事項

- 時制フラグは **Bool `is24Hour`、既定 `true` (24時間制)**。3面 (MAUI facade / Android / iOS) + bridge DTO / snapshot に追加
- Android の `timeFormatUsesAmPm` による時制判定は**完全撤去** (決定源は `is24Hour` のみ)
- iOS の端末設定依存を廃止 (実装手段は locale 上書き等、提案フェーズで確定)
- `format` と `is24Hour` の食い違いは検証・フォールバックせず利用者責任
- 破壊的変更として受容: 既存の `format = "h:mm a"` 利用箇所は `is24Hour = false` の明示で移行 (サンプル・ドキュメント更新)
- 12時間制デモセルを全サンプル (android / ios / maui) に常設し、sample-parity を解消する (本 change に同梱)

## ADR 候補 (作成済み: core/ADR-0028 / 未起票: なし)

- core/ADR-0028 (status: accepted) — 2026-08-28 オーナー承認済み

## 未決の論点

- iOS の 12/24 強制の実装手段 (UIDatePicker.locale 上書きの具体 locale 選定・AM/PM 表示 locale との兼ね合い) — 提案/実装フェーズで確定
- concepts/core/cells/time-picker-selection-surface.md の時制契約の書き換え内容 (蒸留時)

## UI 素材 (ui/references/ の一覧と注釈)

なし (既存ホイール UI の系列切替のみで、新規の見た目は増えない想定)

## 変更級の推奨: M (理由)

- 3面 + bridge + snapshot の公開 API 追加 (facade 契約の変更)
- Android の時制判定ロジック撤去 + テスト改修、iOS の picker 挙動変更 + テスト新設という複数能力にまたがる
- 破壊的変更を含み、concepts / サンプル3面の追随が必要
- 見た目の新規要素はなくモック不要 → L には届かない
