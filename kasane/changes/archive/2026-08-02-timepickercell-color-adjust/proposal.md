# Proposal: timepickercell-color-adjust

## Why

Android 版 TimePickerCell の時刻選択ダイアログ (MaterialTimePicker) は、キーボード入力 UI / 時計文字盤 UI とも Material 規定の配色のままで、KsSettingsView のテーマ色が反映されない。サンプルアプリでは OK/キャンセルだけホスト Activity テーマの色を拾い、文字盤・針・選択枠は既定の紫のままという混色状態になっており、テーマ機能を使う利用アプリで統一感を損なう。

## What Changes

- TimePickerCell の時刻選択ダイアログに、表示時点の実効テーマ色を動的に適用する (android/ADR-0006 の View 走査方式)
- 色マッピング:
  - ダイアログ背景 ← `Theme.backgroundColor`
  - アクセント部位 (時/分の選択枠・選択塗り・キーボード入力欄の枠とキャレット・OK/キャンセル・時計の針とノブ・AM/PM 選択状態) ← `TimePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の解決値 (styling/style-resolution.md の4段解決に従い、Cell 固有値を先頭に置く)
  - 通常文字 (ヘッダタイトル・文字盤の数字・チップ内数字・「時間」「分」ラベル・入力文字) ← 実効タイトル文字色 (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定)
  - アクセント上に載る文字 (ノブ上の数字等) ← 黒と白のうちアクセント色とのコントラスト比が高い方を自動選択
- 未接続だった `TimePickerCell.accentColor` プロパティをこのダイアログ配色に接続する
- 走査・再適用ロジックは内部ヘルパー (TimePickerColorizer 相当) 1 クラスに閉じる。公開 API の追加はなし

影響する能力: cell-types-input (TimePickerCell / Android)

## Non-Goals

- DatePickerCell (MaterialDatePicker / カレンダー形式) への横展開 — 同種の問題を持つが別変更とする
- iOS 側の TimePickerCell — 対象外
- PickerCell / NumberPickerCell 等、AlertDialog 系ダイアログの配色 — 対象外
- material-components のバージョン変更 — 1.12.0 のまま

## Impact

- 公開 API の破壊的変更なし (既存プロパティの接続のみ。挙動としては「無視されていた accentColor が効くようになる」)
- material-components 内部実装への依存が新たに発生 (ADR-0006 で合意済み。1.12.0 固定・ヘルパー隔離・アップグレード時の追随確認が条件)
- 内部 R.id 参照による lint `PrivateResource` の抑制が必要
- 机上確定のみで実機未検証の 3 点 (針の ColorFilter 描画 / pre-draw 再適用のちらつき / 入力欄枠の駆動 state) を実装タスク冒頭の検証に含める

## 級: M

範囲は TimePickerCell 1 系統 + ヘルパー 1 クラスと狭いが、外部ライブラリ内部依存の質的リスクと実機検証項目を明文化する必要があり、UI 変更としてモック承認ゲートを通すため。

domain: android
