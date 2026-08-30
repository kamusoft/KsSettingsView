# Proposal: datepickercell-color-adjust

## Why

Android 版 DatePickerCell (カレンダーモード) の日付選択ダイアログ (MaterialDatePicker) に 2 つの問題がある。(1) ヘッダのタイトルと選択日テキストが上下に重なり、タイトルがほぼ読めない (固定 dp ベースライン設計と日本語大フォントの衝突が最有力仮説)。(2) 配色が Material 規定のままで、OK/キャンセルだけホスト Activity テーマの色を拾う混色状態になっており、KsSettingsView のテーマ色が反映されない。後者は timepickercell-color-adjust の Non-Goals で「別変更」と予告されていた横展開にあたる。

## What Changes

- DatePickerCell (カレンダーモード) のダイアログに、表示時点の実効テーマ色を動的に適用する (android/ADR-0008 = ADR-0006 の View 走査方式の横展開)
- 色マッピングは TimePickerCell と同じ考え方:
  - ダイアログ背景 ← `Theme.backgroundColor`
  - アクセント部位 (選択日の塗り・今日の枠・OK/キャンセル・テキスト入力モードの入力欄枠とキャレット等) ← `DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の解決値
  - 通常文字 (ヘッダ・曜日・日付数字・年月ラベル等) ← 実効タイトル文字色 (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定)
  - アクセント上に載る文字 (選択日の数字等) ← 黒と白のうち、アクセント色を背景へ合成した実効面とのコントラスト比が高い方を自動選択
  - 色ロールの導出規則 (`TimePickerColors` / `ColorRoles`) を再利用する
- 未接続だった `DatePickerCell.accentColor` プロパティをこのダイアログ配色に接続する
- ヘッダのタイトル/選択日の重なりを同じ走査フックで補正する (重なり原因の実機確定と補正パラメータの決定は実装タスク冒頭のスパイクで行う)
- カレンダー UI に加え、鉛筆アイコンで切り替わるテキスト入力モードも配色対象に含める
- 走査・補正ロジックは内部ヘルパ (DatePickerColorizer 相当) 1 クラスに閉じる。公開 API の追加はなし

影響する能力: cell-types-input (DatePickerCell / Android)

## Non-Goals

- 「今日」ショートカットボタン (iOS の `todayText` 相当) の Android 対応 — 別変更 (datepickercell-today-shortcut) とする。公開 API 追加を伴う機能追加であり、本変更の走査フックを注入土台として使う
- `DatePickerUIStyle.Spinner` (AlertDialog 系) の配色 — 対象外 (TimePicker 変更と同じ境界)
- ダイアログ表示中の Activity/構成再生成後の復元 (着色・確定リスナーとも失われる) — 既知の構造問題として fix-picker-dialog-recreation (起票済み) の領分。本変更の Requirement は表示セッション内に限定する
- iOS 側の DatePickerCell — 対象外
- TimePickerCell 側の変更 — 対象外 (色ロール導出の共通化リファクタは行うが挙動は変えない)
- material-components のバージョン変更 — 1.12.0 のまま

## Impact

- 公開 API の破壊的変更なし (既存プロパティの接続のみ。「無視されていた accentColor が効くようになる」)
- material-components 内部実装への依存が MaterialDatePicker 分増える (ADR-0008 で合意済み。1.12.0 固定・ヘルパ隔離・アップグレード時の追随確認が条件)
- 内部 R.id 参照による lint `PrivateResource` の抑制が必要
- 机上確定のみの仮説 (ヘッダ重なりの CJK 大フォント原因) の実機検証を実装タスク冒頭に含める

## 級: M

範囲は DatePickerCell 1 系統 + ヘルパ 1 クラスと狭いが、外部ライブラリ内部依存の質的リスクと実機検証項目があり、UI 変更としてモック承認ゲートを通すため (timepickercell-color-adjust と対称)。

domain: android
