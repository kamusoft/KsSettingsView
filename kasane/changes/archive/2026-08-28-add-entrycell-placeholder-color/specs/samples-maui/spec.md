# Delta: samples-maui (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: placeholder 色のデモ行

MAUI サンプルの Entry デモに、`PlaceholderColor` を指定した text 空の `EntryCell` を1行追加する (SHALL — 文言・構成は 3 platform でパリティを保つ)。

#### Scenario: デモ行で指定色の placeholder が確認できる
- **GIVEN** サンプルアプリの Entry デモ画面
- **WHEN** placeholder 色指定のデモ行を表示する
- **THEN** その行の placeholder が既定色の行と異なる指定色で表示される (iOS / Android 両ターゲット)
