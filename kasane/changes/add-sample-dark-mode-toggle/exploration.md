# Exploration: add-sample-dark-mode-toggle (簡易起票スタブ)

起票日: 2026-08-27 / 起票元: relax-android-host-prerequisites の視覚検証 (material3 1.4.0 再照合) で、dark テーマと範囲つきカレンダーの検証にサンプルの一時改変が必要だったことから。オーナー判断で起票

## 課題 / 動機

サンプルアプリにダークモードの切り替え手段がなく、ライブラリのダーク配色 (Theme の dark 値・端末夜間モードでの M3 由来色) を目視確認するには、検証のたびにサンプルへ一時パラメータ (dark Theme の色値等) を投入して撮影後に戻す手当てが必要。Compose 版の更新ごとに視覚再照合が要る運用 (binding csproj の版整合規律) では、この手当てが毎回発生する。

サンプルにダークモード切り替え (ライブラリ Theme の light/dark 切替、または端末夜間モード連動のデモ) を常設すると、検証コストが下がり利用者向けのデモ価値もある。

関連して、範囲つき (min/max 指定) の Material カレンダーのデモも現状サンプルに無く、範囲外 disabled の視覚確認も同じ一時投入で行っている。

## 検討した選択肢 (却下案と理由を含む)

(未検討)

## 決定事項

(なし)

## ADR 候補

(なし)

## 未決の論点

- **未探索 (簡易起票)**
- 切り替えの形: ライブラリ Theme (色値) の light/dark プリセット切替か、端末夜間モードへの追随デモか、その両方か
- **platform parity**: サンプル間 (iOS / Android / MAUI) の構成一致規約 (kasane/handbook/cross/sample-parity.md) に従い、片側先行にしない (relax-android-host-prerequisites での教訓 — lessons/inbox/single-platform-exposure-added-without-sibling-parity-check.md)
- 範囲つきカレンダーデモ (min/max) の常設を同梱するか
- 12時間制 TimePicker デモの parity 追随 (align-timepicker-hour-cycle-across-platforms の決着に依存) との関係整理

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (S〜M 見込み、parity 対応の範囲次第)
