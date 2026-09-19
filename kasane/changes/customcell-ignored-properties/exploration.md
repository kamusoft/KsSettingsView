# Exploration: customcell-ignored-properties

## 課題 / 動機

- MAUI の `CustomCell` は `CellBase` の直接派生で、共通行レイアウトのスロットを持たない。`CellBase` から継承する次のプロパティは設定しても例外・警告なしに**黙って無視される** (`maui/KsSettingsView.Maui/CustomCell.cs` の class remarks、`CreateSnapshot` / `AffectsSnapshot`):
  - `Title` / `Description` / `HintText`
  - `IconSource` / `IconSize` / `IconRadius`
  - テキスト系スタイル: `TitleColor` / `TitleFont*` / `DescriptionColor` / `DescriptionFont*` / `ValueTextColor` / `ValueTextFont*` / `HintTextColor` / `HintFont*`
  - 効くのは `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` と固有プロパティ (`Content` / `Command` / `CommandParameter` / `ShowArrowIndicator`) だけ
- この無視は意図した設計で、`kasane/decisions/maui/0021-customcell-cellbase-inheritance-silent-noop-surface.md` が決めている (同一スタイル指定を CustomCell を含む複数 Cell へまとめて当てられるようにするため)。本課題はその決定の再検討を含む
- AiForms.Maui.SettingsView の旧 `CustomCell` は `CommandCell` 派生で、これらが描画されていた。AiForms から移行する利用者は、ビルドも表示も通るまま表示だけが消える退行を踏む
- 実例 (発見の文脈: 利用側 ColorAnalyzer の移行作業中の探索): ColorAnalyzer のフィードバック画面は、複数行入力の独自 Cell (`CustomCell` 派生の `EditorCell`) の `HintText` に入力エラー文言をバインドして表示している。そのまま移すとエラー文言が無言で消える
- 出典: `../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/exploration.md`

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- 未探索 (簡易起票)
- これらのプロパティを `CustomCell` で有効化するか、受け付け自体をやめて (または警告等で) 利用者が気づけるようにするか。`HintText` だけでなく無視されるプロパティ全体を対象に検討する
- maui/ADR-0021 の前提要確認 (改訂候補): AiForms 移行時の無言の退行という観点が当時の検討に含まれていたか
- 利用側への影響: ColorAnalyzer は change `migrate-remaining-pages-to-kssettingsview` で、`EditorCell` 内に自前のエラー表示ラベルを持ち、基底の `HintText` をそこへ流す暫定対応で進める。**将来 `HintText` を有効化すると、この利用側で二重表示になる**ため、有効化する場合は利用側への周知が要る
- 移行ガイド (`kssettingsview-aiforms-migration`) での扱い (記述の有無は未確認)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
