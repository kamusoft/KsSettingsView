# UI Brief: fix-picker-dialog-recreation

## 画面と状態

新規画面・新規 UI 要素なし。対象は既存のピッカーダイアログ (MaterialTimePicker / MaterialDatePicker) の Activity 再生成後の表示状態のみ:

- 復元成功: 通常表示時と同一の見た目 (配色・「今日」ボタンの有無) を回復した状態
- 復元不能 (dismiss フォールバック): ダイアログが表示されない状態

## リファレンス注釈

なし (デザイン素材の持ち込みなし)。

## モック免除の記録

mock は作成しない。**視覚の正は「同じ Cell / Theme で通常表示したダイアログ」**であり、受け入れ基準は「再生成後の表示が通常表示と一致すること」。新規のデザイン判断が存在しないため、mock 承認ゲートは適用しない (proposal.md「UI アーティファクト」参照)。

## デザイントークン参照

なし (配色は既存の解決規則 — Theme / CellStyle / Cell の段階解決 — をそのまま使用)。

## 検証証跡

実行時挙動の検証規約 (concepts/cross/conventions/runtime-behavior-verification.md) に基づく修正前の再現・修正後の解消のスクリーンショットは `ui/verification/` に保存する (tasks.md グループ5)。
