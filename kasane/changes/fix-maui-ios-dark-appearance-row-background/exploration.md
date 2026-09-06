# Exploration: fix-maui-ios-dark-appearance-row-background

## 課題 / 動機

cell-style-dark-appearance-parity の tasks 8.4 (MauiHost の外観追随確認、2026-09-06) で発見。MAUI iOS で SettingsView を表示したまま OS の外観をダークへ切り替えると、行の背景がライトのまま (白) 残る一方、未指定のテキスト色だけが dark 既定 (白系) へ解決され、Cell の文字が読めなくなる。本 change (cell-style-dark-appearance-parity) の変更を外した HEAD のビルドでも同じ手順で再現するため、本 change 起因ではない既存不具合。MAUI Android では起きない (背景・テキストとも dark で描かれる)。facade は Theme を空で送っており、iOS host 側かハンドラ側かの切り分けは未実施。証跡: `kasane/changes/cell-style-dark-appearance-parity/evidence/maui-host-buttoncell-retheme-ios-dark.png` (archive 後は `kasane/changes/archive/*-cell-style-dark-appearance-parity/evidence/`) にこの状態が写っている。

関連: [core/ADR-0030](../../decisions/core/0030-theme-dark-appearance-library-owned-light-dark-defaults.md) (未指定色の外観既定への解決)、fix-default-colors-dark-appearance (archive 2026-09-06) の iOS 側の実装。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- iOS Native Host 単体 (MAUI を介さない) でも表示中の切替で行背景が残るか (fix-default-colors-dark-appearance の iOS テストは trait 変更時の再解決を固定しているはずで、MAUI 経路固有かの切り分けが先)
- MAUI iOS の handler が外観変更時に native の `applyTheme` / 再描画を呼んでいるか

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定
