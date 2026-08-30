# セカンドオピニオン: android-picker-selection-sheet (code-review / 対応ホスト側レビュー: review-003)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 修正サイクル3 + 項目 D 反映後の実装 diff
---
# 最終レビュー結果

**判定: APPROVED**

Critical 0 / Major 0 / Minor 0 / Suggestion 0。新規指摘はありません。

## 前回指摘の確認

| 前回指摘 | 状態 | 確認結果 |
|---|---|---|
| Major: 対称スロットが狭幅で画面を超過する | **解消** | [PickerSelectionSheet.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:398) で、測定可能幅に応じて対称幅と固有幅を切り替えています。`HeaderLayout.onMeasure` 内で毎回再計算されるため、再測定にも追従します。 |
| Major: 横方向の有効タップ領域がスロット全域になっていない | **解消** | [PickerSelectionSheet.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:348) で両スロットを48dp以上にし、[同ファイル](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:449) の `TouchDelegate` が横・縦ともスロット全域を委譲しています。取消側にも適用済みで、実 `MotionEvent` を送るテストも追加されています。 |
| Minor: ヘッダー総高がmockより膨らむ | **解消** | 子Viewの最小高さ指定が除かれ、ヘッダーの上下paddingもなく、48dpスロット高がそのままヘッダー高になります。[PickerSelectionSheetTest.kt](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:329) でもmock相当範囲を実測しています。 |

## 新規変更の確認

初期表示では自然高を測定後、折り目からヘッダー等を差し引いた高さへリストを制約し、その後に `scrollToPositionWithOffset` を設定しています。したがって、初期選択行を可視化するための実レイアウト条件は成立しています。

上方向への展開では、`BottomSheetBehavior` の状態・slide callbackから制約を一度だけ解除し、リストを `WRAP_CONTENT` に戻しています。初期スクロール位置の維持、展開後のヘッダー表示、内部スクロール、確定・取消経路にも静的な破綻は見当たりません。

初期スクロールの乖離と `KsSimpleCheckView` 再利用は、提示されたオーナー裁定およびADR追補に従い、問題として扱っていません。テストは再実行せず、報告された全1176件成功を採用しました。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

- 相方: APPROVED (前回指摘 Major 2 / Minor 1 すべて解消、新規指摘なし)
- ホスト側 review-003.md: CHANGES_REQUESTED (Major 1 / Minor 1 / Suggestion 2)
- 前回指摘の解消状況は両者一致 (確定4件すべて解消)

| # | 指摘 | 出典 | 採否 |
|---|---|---|---|
| 1 | リストの上方向スクロール1回で制約解除が発火しシートが全画面へ瞬間移動 (Major) | ホストのみ | **採用** — nested scroll 経路の実測付き (dy=30px → STATE_EXPANDED / top=0)。相方の静的レビューでは nested scroll のシミュレーションが範囲外で見逃し。解除方式自体はオーナー裁定済みのため、指摘は解除トリガーの粒度に限定 → トリガー仕様はオーナー判断へ |
| 2 | 候補行の文字サイズが EffectiveStyle を素通りして固定 16f (Minor) | ホストのみ | **採用** — 色・Typeface は解決済みで文字サイズだけ半端という根拠が具体的 |
| 3 | 文字サイズ3箇所が mock から +1sp (Suggestion) | ホストのみ | **採用 (視覚照合の確認対象)** |
| 4 | ADR-0005 本文と追補の文言矛盾 (Suggestion) | ホストのみ (継続) | **蒸留申し送り** (前回から継続) |

判定が割れた点 (相方 APPROVED vs ホスト Major) は根拠の強さで裁定 — 相方への再提示は行わない (実測 vs 静的確認で情報量に差があり、矛盾ではなく検出力の差)。
