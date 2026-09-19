# Deviation: ios-cell-highlight-delayed-on-touchdown

- 合意済みスコープ外: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift` の CommandCell「プロフィール」に push 遷移を一時的に足して遷移中の押下色を実機確認し、確認後に撤去した (Sample のプラットフォーム間一致の対象となるため恒久化しない)
- 合意済みスコープ外 (exploration.md の決定事項は controller の設定 1 箇所 + collection view サブクラスのみ): ライブ調整の指示により `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` (押下色の遅延・フェード・復帰の制御)、`KsSettingsViewController.swift` の didSelect / viewWillAppear / viewWillDisappear (解除タイミング)、`KsListCellBase.swift` / `CustomCellView.swift` の prepareForReuse (押下色状態のリセット)、`CellBaseLayout.swift` / `CustomCellView.swift` の render (押下色表示中は平常色で塗り替えない) にも手を入れた。理由: 実機評価で「遷移後に色が変わる」体感の主因が解除タイミングにあり、再構成で押下色が消える Cell 種があると分かったため (2026-09-18。経緯は session.md)
