## ADDED Requirements

### Requirement: Theme を渡さない画面のライブラリ既定色の外観追随 (Android)

Sample の Theme を渡さない画面 (Store 方式 / DSL 方式 / 共通フィールド統合 / isVisible と、Section 装飾デモのうちライブラリ既定の下地を使う部分) は、ライブラリ既定色のまま外観 (ライト / ダーク) に追随する (SHALL)。Sample はこれらの画面にダーク用の色を補わない (SHALL NOT) — ライブラリ既定色のダーク描画を目視確認する装置として保つ。3 面の文言・画面構成は変えない (SHALL)。

#### Scenario: Theme を渡さない画面をダークで開く
- **GIVEN** ルートメニューの外観で「ダーク」を選んだ Sample
- **WHEN** Theme を渡さない画面を開く
- **THEN** list 下地・Cell 背景・separator・Header / Footer の文字がライブラリの dark 既定で描画され、Cell title と組み合わせて判読できる

### Requirement: Sample Theme の未指定色は Unspecified で表す

Android Sample の `SampleTheme` は、Theme / CellStyle の色を未指定にする箇所で `null` ではなく `Color.Unspecified` を渡す (SHALL)。Sample の light / dark プリセットの値は本 change 前と同じである (SHALL)。

#### Scenario: Section 装飾デモの既定の枠線色
- **GIVEN** 枠線色を指定しない Section 装飾デモのプリセット
- **WHEN** Theme を構築する
- **THEN** `sectionBorderColor` は `Color.Unspecified` であり、Modern の箱にボーダーは描かれない
