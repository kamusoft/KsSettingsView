# Delta: samples-android (add-entrycell-placeholder-color)

## ADDED Requirements

### Requirement: placeholder 色のデモ行

Android サンプルの Entry デモに、placeholder 色を指定した text 空の `EntryCell` を1行追加する (SHALL — 文言・構成は 3 platform でパリティを保つ)。

#### Scenario: デモ行で指定色の placeholder が確認できる
- **GIVEN** サンプルアプリの Entry デモ画面
- **WHEN** placeholder 色指定のデモ行を表示する
- **THEN** その行の placeholder (hint) が既定色の行と異なる指定色で表示される
