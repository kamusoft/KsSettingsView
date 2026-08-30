# Tasks: ios-picker-selection-parity

## 1. スタイル継承の配線

- [x] 1.1 `PickerCellView.presentPickerModal` で Theme / CellStyle からスタイル解決値一式を組み立てて `PickerListViewController` へ渡す (→ Requirement: PickerCell 選択面のスタイル継承)
- [x] 1.2 候補行への適用: タイトル文字色/フォント (cellTitleFontSize 上書き規則含む)・行背景・区切り線・タップハイライト (→ Requirement: PickerCell 選択面のスタイル継承)
- [x] 1.3 ナビゲーションバーへの適用: Cancel/確定 = 選択印と同一の解決済み強調色、タイトル文字色 = 実効タイトル色 (→ Requirement: ナビゲーションバーへのスタイル適用)
- [x] 1.4 タイトル解決を `pageTitle ?: title` へ変更 (→ Requirement: 選択面のタイトル解決)
- [x] 1.5 候補行のアクセシビリティ状態の公開 (表示名・選択状態・トグル後更新) (→ Requirement: 候補行のアクセシビリティ状態)

## 2. 初期スクロール

- [x] 2.1 表示時に選択中の項目 (単一: selectedIndex / 複数: 選択中の最小 index。有効 index の抽出はスクロール先計算のみで選択集合は正規化しない) へ中央寄せでスクロール。端部クランプ許容・空 items ではスクロールしない (→ Requirement: 選択中の項目への初期スクロール)

## 3. テスト

- [x] 3.1 スタイル継承のテスト: Theme 由来 / CellStyle 優先 / accent 3段全段 (→ Requirement: PickerCell 選択面のスタイル継承 の全 Scenario)
- [x] 3.2 ナビバーのテスト: 単一 (Cancel のみ・解決済み accent) / 複数 (Cancel + 確定・タイトル色) (→ Requirement: ナビゲーションバーへのスタイル適用 の全 Scenario)
- [x] 3.3 タイトル解決のテスト (pageTitle あり / nil フォールバック) (→ Requirement: 選択面のタイトル解決)
- [x] 3.4 アクセシビリティ状態のテスト (公開・トグル後更新) (→ Requirement: 候補行のアクセシビリティ状態 の全 Scenario)
- [x] 3.5 初期スクロールのテスト: 中央寄せ判定 (単一/複数)・範囲外混在で callback 保持・未選択/範囲外のみ・空 items (→ Requirement: 選択中の項目への初期スクロール の全 Scenario)
- [x] 3.6 配線の検証 seam: `presentPickerModal` 経由で構築された VC のスタイル引数 (CellStyle / Theme / Cell 固有 accent) を観測できるテストを追加する (VC 直接生成では配線漏れを検出できないため)
- [x] 3.7 キャンセル経路のテスト: 単一・複数のキャンセルで callback が発火しないことを明示的に検証 (既存テストに無い経路)
- [x] 3.8 既存の選択挙動テスト (即確定・完了確定) の退行確認

## 4. 検証

- [x] 4.1 mock (approved.png) との視覚照合 — 単一/複数/項目多数の各状態のスクリーンショットを ui/verification/ に保存 (シミュレータ可)
- [x] 4.2 実機またはシミュレータでの動作確認 (スクロール位置・配色のテーマ追従・Android 最終版との見え方比較)
