# Deviation: ios-appearance-test-observe-rendered-values

- exploration.md「探索で分かった事実」の「製品は行背景を外観変化で再適用していない」: 実装時の実測で誤りと判明。外観切替で UIKit が行の構成更新 (`configurationUpdateHandler`) を呼び、押下色の後始末 (`KsCellViewSupport.restoreNormalColor`) が保存済みの平常色 (dynamic な `UIColor`) を背景設定へ入れ直している。明示的な trait 購読は無いが、背景の再設定経路はある。今の観測で足りるという結論と決定事項は変わらない (入れ直すのは同じ dynamic 色で、解決は UIKit に任せる)。(2026-09-26)
- 決定事項 2 の検出力確認の一時改変: 「CustomCell の行背景を固定の light 値にする」を描画時の背景設定だけに入れると、上の再設定経路で dynamic 色へ戻されて新テストは緑のままだった。改変先を、保存する平常色そのもの (`CustomCellView.render` の `setRenderState` に渡す `effectiveBackgroundColor`) を描画時点の trait で固定値へ解決する形に変え、新テストが「切替後の CustomCell の行背景: 実測 (1.0, 1.0, 1.0)」で赤になることを確認した。改変は戻し `git diff -- ios/Sources` が空であることを確認済み。(2026-09-26)
