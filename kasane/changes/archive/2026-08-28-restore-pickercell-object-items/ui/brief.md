# UI Brief: restore-pickercell-object-items

## 画面と状態

対象は PickerCell の選択面 (Android: ボトムシート / iOS: ページシート) の候補行のみ。器・ヘッダー・確定/破棄の挙動は変更しない。

- 候補行の構造: 主表示 (+ 副表示 subText、任意) + 選択印 (accent 色チェックマーク)
- 状態:
  - 副表示なし (全行) — 現行と同一の1行構成
  - 副表示あり — 主表示 + 副表示の2行構成
  - 混在 — 副表示の有る行だけ2行
  - 副表示が長い — 扱いは mock 2案の選定論点 (A: 1行で末尾省略 / B: 最大2行で折り返し)

## リファレンス注釈

references/ なし (移植元 AiForms の副表示は MAUI ページの `SimpleCheckCell.Description` であり、視覚の参照元にはしない — 見た目の正は本 change の mock)。

## デザイントークン参照

- 副表示の文字: description 系統の実効値 ([スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md) の解決順で `CellStyle.descriptionColor / descriptionFont` → `Theme.cellDescriptionColor / cellDescriptionFont`)
- 主表示・背景・区切り線・ハイライト・選択印: 現行の選択面スタイル継承表 ([PickerCell の選択面](../../../concepts/core/cells/picker-selection-surface.md)) のまま変更しない

## 承認モック

- **mock/plan-a-subtext-single-line.html を採用 (approved.png)** — 2026-08-28 オーナー承認。副表示は1行で末尾省略 (副表示あり行の行高が長さに依存しない。副表示なし行は従来の1行構成)。不採用: plan-b-subtext-two-line-wrap.html (2行折り返し・行高可変)
- approved.png は架空のデモデータのみで個人情報の写り込みなしを確認済み (2026-08-28)
- 承認後修正: 説明キャプションの「全行の行高が一定」を「副表示あり行の行高が一定」へ訂正し approved.png を再撮影 (second-opinion-spec-001 m3。候補行の視覚実体は不変) (2026-08-28)

## 照合結果

### 選択面単体 (先行グループ)

- `verification/ios-picker-selection-subtext.png` / `ios-picker-selection-no-subtext.png`、`verification/android-picker-selection-subtext.png` / `android-picker-selection-no-subtext.png` を approved.png と照合。乖離なし (2026-08-28)

### sample 実アプリ (ナビゲーションバー / ヘッダー込み)

- `verification/ios-sample-picker-object-selection.png` (iPhone 17 シミュレータ / iOS 26.0)、`verification/android-sample-picker-object-selection.png` (Android エミュレータ) を approved.png と照合 (2026-08-28)。行の構造 (主表示 + 副表示の2行 / 副表示なし行は1行)、長い副表示の1行末尾省略、選択印の accent 色、副表示の description 系統色のいずれも一致。乖離なし
- `verification/ios-sample-picker-object-rows.png` / `android-sample-picker-object-rows.png` は呼び出し元の Cell 行 (object 候補デモ) の記録。mock の対象外だが、確定後の値表示が選択項目に更新されることの証跡
- `verification/maui-sample-picker-object-selection.png` (MAUI sample を iPhone 17 シミュレータで実行 / iOS 26.0) を approved.png と照合 (2026-08-28)。行の構造・長い副表示の1行末尾省略・選択印の accent 色は一致。乖離なし。撮影時に単一選択 (`SelectedItem`) と複数選択 (`SelectedItems`) の両方で候補を確定し、ViewModel 側が選択要素そのものを受け取る (画面上部の「最後のイベント」行が確定した要素の主表示で更新される) ことも目視確認した
- 器・ヘッダーの見た目 (iOS のアイコンボタン / Android の「キャンセル・OK」、accent の黄) は sample が使う共有 Theme (`SampleTheme.maui`) 由来であり、mock が描いた既定色の器とは色が異なる。候補行の構造・トークン・意図は一致しており、本 change の対象外の差分

## トークン候補

- Android の主表示⇔副表示間の余白 2dp (`ROW_SUBTEXT_TOP_MARGIN_DP`) は Theme トークンに対応する値を持たず、選択面のファイル内定数として保持している (先行グループからの申し送り)
