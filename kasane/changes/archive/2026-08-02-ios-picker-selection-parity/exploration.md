# Exploration: ios-picker-selection-parity

## 課題 / 動機

iOS の PickerCell 選択面 (`PickerListViewController`、ページシート) が2点で AiForms 原典・Android 実装から遅れている:

1. **スタイル継承の欠落**: accent 色 (チェックマーク tint) 以外はすべて UIKit 既定のまま。行の文字色・フォント・シート/行の背景・separator・タップハイライト・ナビバー (Cancel/完了/タイトル) が Theme / EffectiveStyle を継承していない。呼び出し側 `PickerCellView.presentPickerModal` は `lastTheme` を保持しているのに accentColor しか渡していない (配線欠落。ksn-scout 調査 2026-08-02、根拠: PickerListViewController.swift:69-119 / PickerCellView.swift:69-91)
2. **初期スクロールなし**: 選択中の項目まで自動スクロールせず常に先頭表示。AiForms 原典は iOS も `PickerTableViewController.InitializeScroll()` で `ScrollToRow(..., .Middle)` を呼ぶ (ksn-scout 調査 2026-08-02)。Android は change: android-picker-selection-sheet で対応済み (deviation.md 記録)

両方とも同一ファイル (`PickerListViewController.swift`) を触るため、1つの change に統合する (オーナー決定 2026-08-02。当初 別セッションのタスクとして起票した初期スクロール単独対応はキャンセル)。

## 検討した選択肢 (却下案と理由を含む)

- **初期スクロールとスタイル継承を別 change に分ける** — 却下: 同一ファイルの変更で競合するだけ。統合で1回のレビュー/検証にまとめる (オーナー決定)
- 継承の基準は **AiForms 原典 PickerPage の継承リスト + Android 版 `PickerSheetStyle` のマッピング** (既に確立済み) を踏襲する

## 決定事項

- スタイル継承のマッピング (Android 版と対になる契約):
  - 行タイトルの文字色/フォント = `EffectiveStyle.titleColor` / `titleFont`
  - シート面・行背景 = `EffectiveStyle.cellBackgroundColor` / separator = `Theme.separatorColor` / タップハイライト = `Theme.selectedColor` (`selectedBackgroundColor`)
  - チェック accent = 現行の3段解決を維持
  - ナビバー (Cancel / 完了 / タイトル) の色・サイズの扱いは propose で詰める (EffectiveStyle に直接対応プロパティなし。Android は 確定文字色 = Theme.backgroundColor / ヘッダーサイズ = 実効タイトル ±1sp を契約化済み — iOS はページシート + ナビバーの慣習との整合を考慮する)
- 初期スクロール: 選択中の項目 (複数選択は選択中の最小 index) が見える状態で開く。AiForms 原典は `.Middle`。位置の詳細 (middle か top か) は propose で確定
- iOS 側の `EffectiveStyle` に必要な解決値はすべて存在する (新規トークン追加は不要見込み)

## ADR 候補 (作成済み: なし / 未起票: なし)

- 新規 ADR は不要見込み — [android/ADR-0005](../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md) と Android 変更で確立した「選択面はセルスタイルを継承する」方針の iOS 適用。クロスプラットフォーム共通契約としての concepts 化は ksn-distill の申し送り事項

## 未決の論点

- ナビバーの Cancel / 完了 / タイトルへの色・フォント適用の範囲 (iOS のシステム慣習 (青 tint) を上書きするか、accent 継承にするか)
- 初期スクロール位置 (.middle / .top — AiForms は middle、Android 実装は先頭寄せ)

## UI 素材 (ui/references/ の一覧と注釈)

- 新規画像なし。参照は [android-picker-selection-sheet/ui/](../android-picker-selection-sheet/ui/) の mock / verification (対になる Android 実装の最終状態) と、iOS 現状の `ios-page-sheet.png` (同 change の references)

## 変更級の推奨: M (理由)

- 触る能力は 1 つ (iOS PickerCell 選択面)。公開 API 変更なし・可逆
- UI を伴う変更 (配色・スクロール挙動) のため propose で mock 要否を判断 (既存ページのトークン適用なので軽量 mock または現状スクショ注釈で足りる可能性)
- Android 版デルタスペックの契約がほぼ流用可能で、実装は配線中心
