# Exploration: fix-ios-replace-cell-type-change

## 課題 / 動機

iOS `KsSettingsViewController` の部分更新経路が Cell の具象型変化を検出せず無条件に
`reconfigureItems` を使うため、`replaceCell` / `replaceCells` で具象型が変わる差し替え
(例: `LabelCell → SwitchCell`) を行うと UIKit が
「Attempted to dequeue a cell for a different registration or reuse identifier」で例外を投げてクラッシュする。

- 再現確認済み: `LabelCell → SwitchCell` の差し替え (add-maui-custom-cell 実装中にオーナーが発見)
- MAUI facade からは `section.Cells[0] = new CustomCell()` のような別種置き換えで
  `KsSettingsController.ReplaceCells` → `gateway.ReplaceCell` 経由でこの経路に乗る

## 現状 (コードで確認)

- full snapshot 経路: `FullSnapshotContentTargets.compute` (ios/Sources/KsSettingsViewUI/FullSnapshotContentTargets.swift:66)
  が `type(of: oldCell) != type(of: cell)` で `reload` に振り分け済み — 型変化対応の正
- 部分更新経路は **2本** あり、どちらも未対応:
  1. `applyReplaceCell` (KsSettingsViewController.swift:1616) — `.replaceCell` diff 単発経路。
     visibility 切替検出 (旧 Cell を cellIndex から取得) はあるが型変化検出が無く、
     1676–1681 行で無条件 `reconfigureItems`
  2. `applyContentUpdateBatch` (KsSettingsViewController.swift:1156) — `replaceCells` バッチ経路。
     `self.root = store.root` → `rebuildModelIndexes()` で旧 Cell が消えるため、
     旧型の退避は **rebuild 前** に行う必要がある

## 検討した選択肢 (却下案と理由を含む)

- 採用: 部分更新経路 2 本に `FullSnapshotContentTargets` と同じ具象型比較を入れ、
  型変化した Cell だけ `reloadItems` へ振り分ける (full 経路の既存設計に整合)
- 却下: 型変化時に full snapshot へフォールバック — visibility 切替の既存フォールバックと同型だが、
  型変化は構造 (item 集合・順序) を変えないため full 再適用は過剰。対象 Cell の reload で十分

## 決定事項

- 修正対象は `applyReplaceCell` と `applyContentUpdateBatch` の両方 (報告経路 = MAUI ReplaceCells はバッチ側)
- 判定は `type(of: old) != type(of: new)` (FullSnapshotContentTargets と同一基準)
- 回帰テストは修正前の実装で fail (クラッシュ再現) することを確認してから修正を入れる
  (runtime-behavior-verification.md: テストで症状を再現できる場合はテストが再現手順を兼ねる)

## ADR 候補

なし (full 経路の既存設計 = 型変化は reload、に部分更新経路を揃えるだけで新しい設計判断はない)

## 未決の論点

なし

## UI 素材

なし (外観変更なし・クラッシュ修正)

## 変更級の推奨: S (確定)

公開 API 変更なし / 単一ドメイン (ios) / 可逆 / UI モック不要 / デルタスペック不要
