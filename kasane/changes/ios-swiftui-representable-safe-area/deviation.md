# Deviation: ios-swiftui-representable-safe-area

- tasks 2.1 (iOS Sample の回避策除去): tasks では 7 画面 → 実際は 5 画面 (BasicCells / InputCells / CustomCell / DSLDemo / StoreDemo)。理由: MinimalDiffableDemoView と SectionDecorationSpikeView は `KsSettingsView` を通さない技術検証画面 (自前の Representable を直接ホスト) で、specs/samples-ios の Requirement は「`KsSettingsView` を置く画面」に対象を絞っている。2 画面から除去するとラッパの既定が効かず帯が出て Scenario「取り除く前と同じ見た目」に反するため、利用側の `.ignoresSafeArea(.container, edges: .bottom)` を残す (2026-09-19、orchestrator 裁定)
