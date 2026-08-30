# Proposal: android-numberpicker-modern-ui

## Why

Android 版 NumberPickerCell の選択 UI は `AlertDialog` + `android.widget.NumberPicker` で、Holo 時代の古い見た目のまま。オーナーは刷新を希望している。また Android 版には `unit` プロパティ自体が存在せず、iOS (AiForms 互換の `unit` + フォーマッタ保有) に対してプロパティパリティが欠落しており、「15 px」のような単位付き表示ができない。

## What Changes

- **unit パリティ**: `NumberPickerCell` (ui モジュール) と Compose DSL の TwoWay overload (`InputCellDsl.kt`) に `unit: String = ""` を追加。valueText 自動表示とピッカー候補表示の両方に iOS 同等のフォーマット (`"<value> <unit>"`、unit 空なら数値のみ) を適用する
- **選択 UI 刷新**: `AlertDialog` + `widget.NumberPicker` を、ボトムシート + 自作ホイール (RecyclerView + LinearSnapHelper) に置き換える (android/ADR-0007)。器は ADR-0005 の `PickerSelectionSheet` と同系の構成 (ドラッグハンドル + ヘッダー + コンテンツ)
- **ホイール部品の新設**: スナップ式ホイールを再利用可能な内部部品として作る (将来の DatePicker ホイール版展開の土台)

影響する能力: `settings-view-android-ui` (NumberPickerCell の表示と選択 UI)

## Non-Goals

- DatePickerCell / TimePickerCell の選択 UI 変更 (DatePicker ホイール版への展開は続編 change)
- iOS / MAUI 側の変更 (iOS は既に unit を持つ)
- PickerCell のボトムシート (`PickerSelectionSheet`) の挙動変更
- ホイール部品の公開 API 化 (内部部品に留める)

## Impact

- **公開 API**: `NumberPickerCell` と Compose DSL overload に `unit` 引数を追加 (挿入位置は iOS と同順の `value` 直後)。互換性の契約は**ソース互換** (named 引数・既定値前提) — data class の constructor / `copy` のシグネチャが変わるため **ABI 互換は保証対象外** (利用アプリは同一バージョンでの再ビルドを前提とする)。`value` 以降を位置引数で渡している呼び出しはソース修正が必要になり得る
- **視覚変更**: 選択 UI がダイアログからボトムシートに変わる (挙動契約 — 確定/キャンセルの意味論 — は維持)
- **concepts への影響**: `core/cells/input-cells.md` の「`NumberPickerCell.unit` は iOS 固有」というプラットフォーム差の記載が解消される (蒸留時に追随)
- **リスク**: 自作ホイールのスナップ・慣性の作り込み品質。mock 承認と実機視覚照合で担保する

## 級: M

公開 API 追加 + UI 刷新だが、触る範囲は NumberPickerCell 系に閉じる (先行 android-picker-selection-sheet と同規模)。

domain: android
