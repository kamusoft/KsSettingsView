# Delta: settings-view-android-ui (add-maui-modern-style)

## ADDED Requirements

### Requirement: Section 装飾値の非有限数正規化

Section 装飾4属性の描画時正規化は、非有限 (NaN・±∞) の `sectionMargin` 成分・`sectionCornerRadius`・`sectionBorderWidth` を 0 として扱う (SHALL)。現状は NaN が `max(0,·)` を素通りして `roundToInt()` で例外に到達し得るため、正規化の先頭で非有限を 0 へ落とす。Theme 構築時には拒否しない既存契約は維持する (正規化は描画時のみ)。

#### Scenario: 非有限の装飾値でも例外なく描画される
- **GIVEN** NaN を含む `sectionMargin` 成分と非有限の `sectionCornerRadius` を持つ Theme
- **WHEN** Modern style で描画する
- **THEN** 例外は発生せず、非有限の成分は 0 として扱われる
