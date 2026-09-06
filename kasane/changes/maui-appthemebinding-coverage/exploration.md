# Exploration: maui-appthemebinding-coverage

## 課題 / 動機

docs-refresh (2026-09-06) で、MAUI 利用者がライト / ダーク両外観の色を自分で決める手段として `AppThemeBinding` を facade (`SettingsView`) の色プロパティに使うレシピを、`skills/{en,ja}/kssettingsview-maui/references/styling.md` と `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md` の 2 Skill に掲載した。

一方、この経路が Theme の再適用まで届くという根拠は concepts (`maui/api/maui-styling.md`「未設定の色と外観 (ライト / ダーク)」、`core/styling/style-resolution.md` の MAUI 行) の「Simulator / Emulator で確認済み」という記述のみで、`maui/` 配下のコード・Sample・テストには `AppThemeBinding` が 1 箇所も現れない (fix-default-colors-dark-appearance の tasks 0.1 spike で確認された経路だが、常設の固定がない)。利用者向け文書が推奨する経路として、Sample かテストで回帰を固定しておく価値がある。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- **(2026-09-06 追記、cell-style-dark-appearance-parity の tasks 0.1 spike より)** Cell 単位の色プロパティ (`ButtonCell.TitleColor`) に付けた `AppThemeBinding` は、iOS / Android とも外観切替で再評価されず native まで届かない。同じ画面の素の `Label` の `AppThemeBinding` は再評価され、同じ Cell プロパティへの直接代入は同一 View のまま行に届く。見立て (実測から。MAUI 本体の binding 実装は未読): `Section` / `CellBase` は `Element` だが MAUI の element ツリーに繋がれておらず (`Parent` が null)、ツリー外の `Element` では `AppThemeBinding` が外観変更を受け取らない (ツリーに載せない素の `Label` でも同様に再評価されないことを対照実験で確認)。`SettingsView` 単位の Theme プロパティで効くのは `SettingsView` が `View` としてツリー上にあるため。証跡: `kasane/changes/cell-style-dark-appearance-parity/evidence/maui-*-basic-cells-buttoncell-*` (archive 後は `kasane/changes/archive/*-cell-style-dark-appearance-parity/evidence/`)。本 change の論点として「Section / Cell を element ツリーに繋ぐ (`AddLogicalChild` 相当) 改修の可否と副作用 (BindingContext 継承等)」を加える。オーナー裁定 (2026-09-06): cell-style-dark-appearance-parity 側では MAUI の手段を「外観変更を購読して Cell プロパティへ再代入」に改めて続行し、`AppThemeBinding` を Cell で効かせる改修は本スタブで別途探索する
- 固定の置き場: MAUI Sample の外観切替デモ (add-sample-dark-mode-toggle で追加済みの 3 面) に `AppThemeBinding` 利用の Cell / Section を常設するか、MAUI 層の自動テスト (facade の色プロパティ変更 → bridge DTO → Theme 再適用) で固定するか、両方か
- `cross/conventions/sample-parity.md` (Sample 同等性の規約) との整合 — iOS / Android の Sample に対応物 (dynamic `UIColor` / `isSystemInDarkTheme()` 分岐) を置くべきか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
