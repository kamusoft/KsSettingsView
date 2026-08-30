# Tasks: android-picker-selection-sheet

## 1. 選択シート UI の新設

- [x] 1.1 ボトムシート選択面コンポーネントを新設する (ヘッダー + 候補リスト、承認 mock 準拠) (→ Requirement: PickerCell 選択面の提示)
- [x] 1.2 独自チェックマーク drawable の描画と強調色の3段解決 (`accentColor` → `style.accentColor` → `Theme.cellAccentColor`) (→ Requirement: 選択印の強調色)
- [x] 1.3 高さ挙動: コンテンツ高 + 画面約半分上限 + 内部スクロール + ドラッグ全展開 (→ mock と brief の検証条件が正、ADR-0005)
- [x] 1.4 候補行のアクセシビリティ状態の公開 (表示名・選択状態・トグル後更新) (→ Requirement: 候補行のアクセシビリティ状態)

## 2. PickerCellViewHolder の差し替え

- [x] 2.1 タップ時の `AlertDialog` 提示をボトムシート提示へ差し替える (→ Requirement: PickerCell 選択面の提示)
- [x] 2.2 単一選択: タップ即確定・dismiss の配線 (→ Requirement: 単一選択の即時確定)
- [x] 2.3 複数選択: 作業状態・完了確定・キャンセル破棄・上限 + haptic の移植 (→ Requirement: 複数選択の確定・破棄と上限)

## 3. テスト

- [x] 3.1 タイトル解決・候補列挙と formatter・OS ラベル解決・キャンセルおよび非確定 dismiss 全経路 (外側タップ / Back / 下スワイプ) の callback 不発火のテスト (→ Requirement: PickerCell 選択面の提示 の全 Scenario)
- [x] 3.2 単一選択の即時確定テスト (→ Scenario: 項目タップで即確定して閉じる)
- [x] 3.3 複数選択の確定・破棄・上限・解除のテスト (→ Scenario: 確定操作で確定する / キャンセルで作業状態を破棄する / 上限到達時は新規チェックを無視して触覚フィードバック / 上限到達時もチェック解除は可能)
- [x] 3.4 強調色の3段解決のテスト (→ Requirement: 選択印の強調色 の全 Scenario)
- [x] 3.5 アクセシビリティ状態のテスト (→ Requirement: 候補行のアクセシビリティ状態 の全 Scenario)
- [x] 3.6 範囲外 index 保持・初期上限超過のテスト (→ Requirement: モデル値の許容と非正規化 の全 Scenario)
- [x] 3.7 Robolectric でのシート表示検証可否を確認し、不可の部分は実機証跡タスクへ回す

## 4. 検証

- [x] 4.1 mock (approved.png) との視覚照合 — 単一/複数/上限/項目多数の各状態のスクリーンショットを ui/verification/ に保存
- [x] 4.2 実機での動作確認 (シート表示・haptic・ドラッグ挙動)
