# Tasks: fix-ios-full-content-refresh

## 1. 実装

- [x] 1.1 reconfigure 対象選定を**純粋 helper** として分離する: 旧・新 visible projection を入力に「双方に存在し値 (`==`) が変化し、具象型が同一で、supplementary reload 対象 Section に属さない Cell の ID 集合」と「具象型が変わった同一 ID Cell の ID 集合」を返す (→ Requirement: full 更新における同一 ID Cell の内容反映)
- [x] 1.2 `applyFullSnapshot` で helper の結果を適用する: 内容差分 Cell は `reconfigureItems`、具象型変更 Cell は `reloadItems` (cell 交換許容) で snapshot 適用時に反映する。対象が空でも構造反映 (`dataSource.apply`) は必ず実行する (→ Requirement: full 更新における同一 ID Cell の内容反映)
- [x] 1.3 `DSLDiffCalculator` の headerHeight preflight から `contentUpdateDiffs` の続発を除去し、`.full(newRoot)` のみの発行にする (可視性 preflight と同形に統一。分岐の統合は実装裁量) (→ Requirement: SwiftUI DSL の headerHeight 変更の表示反映)

## 2. テスト

- [x] 2.1 対象選定 helper の単体テストを追加する: 返却 ID 集合を直接検証する。境界ケース — 初回適用 (旧が空)・完全同値・新規挿入・削除・Cell / Section の可視性切替・移動 + 内容変更の複合・header 変更 Section (reload 対象) の除外・具象型変更の分離 (→ Requirement の対象選定契約。全件 reconfigure する誤実装で落ちることを確認する)
- [x] 2.2 repro テスト `_ReproFullContentRefreshTests.swift` を正式テストに改名・整理する (探索用の注記を除去し、Scenario「full 更新で表示中セルの内容変化が反映される」に対応させる)
- [x] 2.3 Scenario「内容変化した表示中セルの行 identity が維持される」のテストを追加する (適用前後で `cellForItem` のインスタンス同一性を検証)
- [x] 2.4 Scenario「構造変更と内容変更が混在する full 更新」のテストを追加する
- [x] 2.5 Scenario「可視性と内容の同時変更で内容が取りこぼされない」のテストを追加する
- [x] 2.6 Scenario「replaceSection で同一 ID Cell の内容変化が反映される」のテストを追加する
- [x] 2.7 Scenario「header と Cell 内容の同時変更で両方が反映される」のテストを追加する
- [x] 2.8 Scenario「同一 ID で具象型が変わる Cell の差し替え」のテストを追加する
- [x] 2.9 `DSLDiffCalculatorTests` の `test_headerHeightとCell内容の同時変更でfullに続けてreplaceCellが発行される` を Scenario「headerHeight と Cell 内容の同時変更で両方が反映され内容再適用は一度だけ」に合わせて `.full` 1件のみ期待へ更新する。表示レベルの両方反映 (高さ + 内容) は UI 層テストで検証する

## 3. 検証

- [x] 3.1 iOS 全テストスイートを実行し回帰がないことを確認する。実行契約: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=<利用可能な機種名>'`。終了コードだけでなく末尾の `Executed N tests, with M failures` を記録し、実行件数が既存全件 + 追加分であることを確認する (`applyFullSnapshot` は `.full` / `replaceAll` / `replaceSection` / DSL preflight の合流点のため全体回帰が必須)
