# Exploration: maui-cell-appthemebinding-logical-child

## 課題 / 動機

MAUI で `Section` / `Cell` の色プロパティに書いた `AppThemeBinding` は、現行契約では外観変更で再評価されない (core/ADR-0031 Decision 2 の MAUI 節: `Section` / `Cell` は `SettingsView` の element ツリーに繋がない。利用者の手段は外観変更の購読 + 再代入)。maui-appearance-change-tracking の spike ②-b (tasks 5.1、2026-09-15) で、`Section` / `Cell` を論理子に繋ぐと `AppThemeBinding` が再評価されて native の行の描画まで届くことが iOS / Android の両方で**成立**した。採用するなら core/ADR-0031 Decision 2 の MAUI 節と Revisit When の改訂を伴う設計変更になるため、本 change として起票する (オーナー裁定 2026-09-15、maui-appearance-change-tracking の proposal / tasks 5.1 の取り決めどおり同 change には同梱しない)。

### spike の調査結果 (同じ調査を繰り返さないための記録。全文は `../maui-appearance-change-tracking/exploration.md` の「spike ②-b の結果 (2026-09-15)」)

- **原因の同定**: ツリー外の `Element` では `AppThemeBinding` が外観変更を受け取らない、という見立ては実測で裏付けられ、`Parent` を入れるだけで解消した
- **試した最小改変 (一時、戻し済み)**: facade の BindingContext 配布処理 (`maui/KsSettingsView.Maui/Internals/KsBindingContextBinder.cs` の `Distribute`) で `SetInheritedBindingContext` に続けて `owner.AddLogicalChild(child)` を 1 行 (`child.Parent` が null のときだけ)。この処理は SettingsView → Section と Section → Cell の両方で使われるため 1 箇所で両段が論理子になる。`Section` / `Cell` / `SettingsView` 自体は未改変
- **観察 (iOS Simulator iOS 26.0 / Android Emulator API 35)**: MAUI Sample「基本 Cell 7 種デモ」の ButtonCell `TitleColor` に `{AppThemeBinding Light=#FF008000, Dark=#FFFF00FF}` を付け (code-behind の再代入を外す)、表示したまま外観を切り替えると同じ画面・同じ行のまま緑 → マゼンタへ描き直された。Android は `MainActivity` の `Recreate()` を外し同一 Activity のまま確認。証跡は `../maui-appearance-change-tracking/evidence/maui-spike-logical-child-{ios,android}-{light,dark}.png`
- **既存テストへの影響**: 改変込みで MAUI facade テスト 528 件 / 失敗 0 (改変なしと同じ)。`LeakTests` / `BindingContextTests` / 多重配置の検査は落ちなかったが、これは安全の根拠にならない (下記)
- **構造上残る問題 (本 change の設計論点)**: (1) 最小改変は `AddLogicalChild` が片道で、Section を Root から外す・Cell を Section から外す・ItemsSource の再生成・Root 差し替えの 4 経路で `RemoveLogicalChild` が対にならず `Parent` が古い所有者を指し続ける。実装では外す側 (`KsItemsSourceBinder` / controller の対応表と同じ 4 経路) を全部対にする必要がある。(2) `child.Parent is null` のときだけ繋いだため、既に `Parent` を持つ Cell は黙って繋がれない。多重配置の判定が controller の対応表と `Parent` の二系統になる論点は未解消。(3) BindingContext の二重伝播・継承プロパティの伝播変化は改変込みテストが緑だった範囲では観測されなかったが、積極的には検査していない。(4) `AppThemeBinding` の再評価は MAUI 本体 (`Application.RequestedTheme`) が担うため、ライブラリ側に新しい購読は不要に見える (未検証)
- **前史**: cell-style-dark-appearance-parity (archive 2026-09-06) の 0.1 で「Cell の `AppThemeBinding` は再評価されない」を実測し、手段を購読 + 再代入に改めた経緯。利用者向け Skill の `DynamicResource` 記述の訂正は settingsview-migration-defects が扱う

関連: core/ADR-0031 (Decision 2 の MAUI 節・Revisit When)、`kasane/concepts/maui/api/maui-styling.md`、`maui/api/maui-facade.md`。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: core/ADR-0031 Decision 2 の MAUI 節の改訂 (amends または supersede))

## 未決の論点

- 未探索 (簡易起票)
- 外す側 4 経路の `RemoveLogicalChild` の設計と、`Parent` を持つ Cell の多重配置判定を controller の対応表と `Parent` のどちらに一本化するか
- leak (`Parent` が古い所有者を指す) と継承プロパティ伝播 (BindingContext・`FlowDirection` 等) の副作用検査の範囲
- 採用時の利用者向け契約: Cell の色プロパティに `AppThemeBinding` / `DynamicResource` を書けるようになる (購読 + 再代入は不要になる) — Skill / concepts / Sample (3 面一致の規約との整合) の追随範囲

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (ADR-0031 の改訂を伴うため M 以上の見込み)
