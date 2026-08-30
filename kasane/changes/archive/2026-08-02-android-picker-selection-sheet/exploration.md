# Exploration: android-picker-selection-sheet

## 課題 / 動機

Android の PickerCell の選択 UI が `AlertDialog` (Radio/Checkbox 描画) で古臭い。オリジナル AiForms はページ遷移形式、KsSettingsView iOS はページシート。iOS に近い「下から出る選択面」の体験に揃えたい。

## 検討した選択肢 (却下案と理由を含む)

| 案 | 評価 |
|---|---|
| ダイアログ現状維持 | 却下: 古臭く、求めていた UX ではない |
| ページ形式 (AiForms 同型) | 却下: AiForms は MAUI のナビゲーション機構前提。ライブラリがページ遷移機構を抱えることになる |
| **ボトムシート (Material BottomSheetDialog)** | **採用**: 追加依存なし (Theme.Material3 必須プロジェクト)、標準挙動が無料、中身のレイアウトは自由 |
| 自前オーバーレイ実装 | 却下: 保守コストに見合う利点なし |

高さの挙動:

| 案 | 評価 |
|---|---|
| **A: コンテンツ高 + 画面約半分上限 + 内部スクロール + ドラッグ全展開** | **採用**: Material 標準挙動。項目数によらず自然 |
| B: 常にほぼ全画面 (iOS 踏襲) | 却下: 項目が少ないと空白が不自然 |
| C: 固定比率 | 却下: 同上 |

## 決定事項

- 器: `BottomSheetDialog`。ヘッダーは「キャンセル / タイトル / (複数選択時のみ) 完了」で iOS の nav bar 構成と同型
- 行: RecyclerView で「タイトル左 + チェック右」。チェックは Checkbox/Radio でなく独自のチェックマーク drawable (accentColor tint)。iOS の `.checkmark` accessory と同じ見え方
- 単一選択: タップ即確定 dismiss / 複数選択: 完了で確定
- 挙動仕様 (callback タイミング・`maxSelectedNumber`・haptic) は変更しない — iOS/Android で既に揃っており、変わるのは器と行の見た目だけ
- スコープ: **PickerCell のみ**。NumberPickerCell / TimePickerCell / DatePickerCell (ホイール) のシート化は将来の別変更候補

## ADR 候補 (作成済み: android/ADR-0005 / 未起票: なし)

- [android/ADR-0005](../../decisions/android/0005-pickercell-selection-ui-bottom-sheet.md) — PickerCell の選択 UI はボトムシートで表示する (proposed)

## 未決の論点

- シート内の細部デザイン (余白・文字サイズ・ドラッグハンドルの有無) — ksn-propose の mock で確定させる

## UI 素材 (ui/references/ の一覧と注釈)

- `android-current-dialog.jpg` — 現実装の AlertDialog (置き換え対象。この見た目にしない)
- `aiforms-page.jpg` — オリジナル AiForms のページ形式 (行レイアウト「タイトル左 + チェック右」の参考。ページ遷移自体は不採用)
- `ios-page-sheet.png` — KsSettingsView iOS のページシート (ヘッダー構成・行レイアウト・チェック描画の正。高さの挙動のみ Android 慣習に変える)

## 変更級の推奨: M (理由)

- 触る能力は 1 つ (PickerCell の Android 選択 UI)。公開 API 変更なし (`selectionMode` 等のモデルは不変)
- ただし UI を伴う変更のため、デザインブリーフ + mock 承認ゲート (ksn-propose) が必要
- 実装は器の差し替え + シート内 UI 新設 + テストで複数ファイルに及ぶ
