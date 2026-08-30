# Tasks: fix-dsl-header-height-diff

## 1. Android (Compose DSL)

- [x] 1.1 `DSLDiffCalculator.kt` に headerHeight の preflight 検出を追加する — `compute` は検出時 `Full(newRoot)` のみを返し、`contentUpdates` も同条件で空リストを返す (→ Requirement: Compose DSL の headerHeight 変更の表示反映)
- [x] 1.2 `DSLDiffCalculatorTest.kt` に対称テストを追加: headerHeight のみ変更 → `Full` のみ + `contentUpdates` 空。遷移 3 種 (正値間 / `-1.0 → 正値` / `正値 → -1.0`) をカバーする (→ Scenario: headerHeight のみの変更が表示へ反映される)
- [x] 1.3 テスト: headerHeight + Cell 内容の同時変更 → `Full` のみ + `contentUpdates` 空 (→ Scenario: headerHeight と Cell 内容の同時変更で両方が反映される)
- [x] 1.4 テスト: headerHeight 不変 + 内容のみ変更 → `Full` 非発行・`contentUpdates` に列挙 (既存経路の退行防止) (→ Scenario: headerHeight が不変なら preflight は発火しない)

## 2. iOS (SwiftUI DSL)

- [x] 2.1 `DSLDiffCalculator.swift` に headerHeight の preflight 検出を追加する — 検出時 `.full(newRoot)` を発行し、同一再評価内で内容が変わった同一 ID の Cell があれば `.full` に続けて `.replaceCell` を発行する (→ Requirement: SwiftUI DSL の headerHeight 変更の表示反映)
- [x] 2.2 `DSLDiffCalculatorTests.swift` に対称テストを追加: headerHeight のみ変更 → `.full` 発行。遷移 3 種 (正値間 / `-1 → 正値` / `正値 → -1`) をカバーする (→ Scenario: headerHeight のみの変更が表示へ反映される)
- [x] 2.3 テスト: headerHeight + Cell 内容の同時変更 → `.full` に続く当該 Cell の `.replaceCell` の発行を検証する (→ Scenario: headerHeight と Cell 内容の同時変更で両方が反映される)
- [x] 2.4 テスト: headerHeight 不変 + 内容のみ変更 → `.full` 非発行・`.replaceCell` 発行 (既存経路の退行防止) (→ Scenario: headerHeight が不変なら preflight は発火しない)

## 3. iOS Store 経由の対称確認 (ADR-0018 の4象限を閉じる)

- [x] 3.1 Store `replaceSection` および `.full` で headerHeight のみ変更 → 表示中 header の実高さの反映を観測するシミュレータ XCTest を追加する。目視 A/B (4.2) は補助証跡とし、テストの代替にしない (→ Requirement: Store 経由の headerHeight 変更の表示反映 / Scenario: replaceSection・.full による headerHeight 変更が表示へ反映される)

## 4. 実機/シミュレータ検証 (視覚系修正の A/B 実測)

- [x] 4.1 Android エミュレータ/実機: Compose DSL で headerHeight を動的に変える A/B (修正前=不変・修正後=反映) を実測し、証跡を verification/ へ保存する
- [x] 4.2 iOS シミュレータ: SwiftUI DSL で同様の A/B を実測し、あわせて Store `replaceSection` 経由の反映も目視する。証跡を verification/ へ保存する
