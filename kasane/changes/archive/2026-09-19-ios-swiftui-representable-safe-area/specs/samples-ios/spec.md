## ADDED Requirements

### Requirement: iOS Sample のデモ画面はラッパの既定配置で bar の後ろまで回り込む
`samples/ios` のデモ画面のうち `KsSettingsView` (SwiftUI ラッパ) を置く画面は、利用側でセーフエリアを無視する修飾 (`.ignoresSafeArea` 系) を書かず、ラッパの既定配置で一覧が画面下端まで描かれる SHALL。画面の文言・Section 構成・Cell 構成は変えない SHALL (handbook `cross/sample-parity.md`)。

#### Scenario: 回避策を取り除いても見た目が変わらない
- **GIVEN** 利用側の `.ignoresSafeArea(.container, edges: .bottom)` を取り除いたデモ画面
- **WHEN** 画面を表示して末尾までスクロールする
- **THEN** 一覧の背景が画面下端まで描かれ、末尾 Cell がホームインジケータ領域に隠れずに見える (取り除く前と同じ見た目)

#### Scenario: Sample の文言と構成は不変
- **GIVEN** 回避策を取り除いたデモ画面
- **WHEN** 画面タイトル・Section・Cell の文言と並びを Android / MAUI Sample と比較する
- **THEN** 本変更前と同じく一致している
