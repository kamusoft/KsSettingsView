# Proposal: android-picker-selection-sheet

## Why

Android の PickerCell の選択 UI は `AlertDialog` (Radio/Checkbox 描画) で、オーナー評価は「古臭くて求めていたものではない」。iOS はページシート + 独自チェックマーク描画であり、プラットフォーム間で選択体験の質に差がある。「下から出る選択面」という体験を iOS と揃えつつ、Android の慣習に沿った形で刷新する ([android/ADR-0005](../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md))。

## What Changes

- PickerCell タップ時の選択 UI を `AlertDialog` からボトムシート (Material `BottomSheetDialog`) に変更する
- シートの構成: ヘッダー (キャンセル / タイトル / 複数選択時のみ OK) + 選択リスト。行は「タイトル左 + チェックマーク右」で、チェックは Checkbox/Radio ではなく独自 drawable (accentColor tint)
- 単一選択はタップ即確定で閉じる。複数選択は「OK」で確定。操作ラベルは OS 公開リソース (`android.R.string.ok` / `cancel`) を使い自前文字列を同梱しない (ADR-0005 追補)。確定 callback のタイミング・`maxSelectedNumber` 上限・上限到達時の haptic は現挙動を維持する
- 影響する能力: `settings-view-android-ui` (PickerCell の Android 選択面の提示挙動)

## Non-Goals

- NumberPickerCell / TimePickerCell / DatePickerCell の選択 UI 変更 (将来の別変更候補)
- PickerCell のモデル・公開 API の変更 (`selectionMode` / binding / callback は不変)
- iOS 側の変更
- Cell 行そのもの (タイトル + valueText + chevron) の見た目変更

## Impact

- 破壊的変更なし (公開 API 不変)。ただし利用者アプリの選択 UI の見た目が変わる (視覚的変更)
- 影響範囲は `PickerCellViewHolder` のタップ時提示ロジックと、新設するシート UI コンポーネント
- リスク: BottomSheetDialog はテーマ依存 (Theme.Material3 必須は既存要件のため追加負担なし)。Robolectric でのシート表示検証可否は実装時に確認し、不足分は実機証跡で担保する

## 級: M

UI を伴う変更 (mock 承認ゲート必要)、公開 API 変更なし・単一能力のため。

domain: android
