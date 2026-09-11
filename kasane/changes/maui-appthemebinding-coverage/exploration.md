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
- **(2026-09-11 追記、`../ColorAnalyzer` の AiForms 移行 `migrate-settingsview-to-kssettingsview` より)** `AppThemeBinding` だけでなく **`DynamicResource` でも同じ**。Cell に直接書いた `AccentColor="{DynamicResource AccentColor}"` は、**初期解決は届くが外観切替では再評価されない**。独立した調査で上記と同じ見立て (Cell が element ツリーに繋がれていない) に到達しており、裏付けとして記録する
  - 実測 (iOS シミュレータ、画素値サンプリング): ライト起動時 `#3367CC` (`LightTheme` の `AccentColor` と一致)、ダーク起動時 `#6C91D9` (`DarkTheme` と一致) → **初期解決は外観に応じて正しく行われる**。一方ライト表示中にダークへ切り替えると `#3367CC` のまま (期待 `#6C91D9`) → **切替追従しない**
  - 判定を濁らせないため `SettingsView.CellAccentColor="Red"` を当てた状態で検証した。赤にならず `AccentColor` の値が出たことが、Cell 直書きの解決が効いている証拠
  - コード上の裏付け: `maui/` 全体で `AddLogicalChild` を呼ぶのは accessory の View のみ (`Internals/KsAccessoryViewOwnership.cs:54` ← `Internals/KsSettingsController.cs:499` / `:821` / `:1002`)。**Section・Cell 自体を論理子に追加する箇所がない**。一方 `SwitchCell.cs:64-66` の `AffectsSnapshot` は `AccentColor` を含み、変更さえ起きれば native まで届く配線は存在する (`Internals/KsSettingsController.cs:2010` / `:2054` / `:463-465` / `:1519-1529`)
  - **利用者向け文書への影響**: `skills/{en,ja}/kssettingsview-maui/references/cells.md:3` と `skills/{en,ja}/kssettingsview-aiforms-migration/references/api-mapping.md:45` は「Section と Cell は tree に載らないため `{Binding}` は効くが `x:Reference` と `{DynamicResource}` は解決されない」と書いているが、**初期解決は届くので「解決されない」は不正確**。正しくは「一度きりの解決は届くが、リソース辞書の差し替え通知は Cell まで伝播しない」。この記述は AiForms からの移行者が Cell の色指定をどう書くかを左右するため、実挙動に合わせて直す必要がある (詳細は `settingsview-migration-defects`)
- 固定の置き場: MAUI Sample の外観切替デモ (add-sample-dark-mode-toggle で追加済みの 3 面) に `AppThemeBinding` 利用の Cell / Section を常設するか、MAUI 層の自動テスト (facade の色プロパティ変更 → bridge DTO → Theme 再適用) で固定するか、両方か
- `cross/conventions/sample-parity.md` (Sample 同等性の規約) との整合 — iOS / Android の Sample に対応物 (dynamic `UIColor` / `isSystemInDarkTheme()` 分岐) を置くべきか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
