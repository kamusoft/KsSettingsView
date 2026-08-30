# Delta: maui-bridge (add-accessory-visibility-toggle)

## ADDED Requirements

### Requirement: KsBridgeSection の可視トグル輸送

`KsBridgeSection` (iOS / Android) は `isHeaderVisible` / `isFooterVisible` (既定 `true`) を保持し、core `Section` への変換時にそのまま写す (SHALL)。専用の bridge 操作は追加せず、トグルの変更は既存の `replaceSection` 経路で輸送する (SHALL)。

#### Scenario: DTO のトグルが core Section へ伝搬する
- **GIVEN** `isHeaderVisible = false` を設定した `KsBridgeSection`
- **WHEN** core `Section` へ変換する
- **THEN** 生成された Section の `isHeaderVisible` は `false` である (`isFooterVisible` も同様)

#### Scenario: 既定値は true
- **GIVEN** トグルを設定しない `KsBridgeSection`
- **WHEN** core `Section` へ変換する
- **THEN** 生成された Section のトグルは両方 `true` である
