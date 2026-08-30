# samples-maui デルタスペック

`samples/maui` の画面集合の拡張。sample-parity 規約 (concepts/cross/conventions/sample-parity.md) が上位規約。

## ADDED Requirements

### Requirement: デモページ4画面の追加

MAUI サンプルは「基本 Cell」「入力 Cell」「Cell 共通フィールド」「isVisible」の4デモページを持ち、各ページは iOS / Android サンプルの対応ページと画面タイトル・メニュー文言・Section / Cell の構成・表示文言・デモデータ (初期値・選択肢・色指定を含む) が一致しなければならない (SHALL)。色は platform 固有の semantic color を使わず、iOS / Android サンプルの共通色定義と同一の値を参照しなければならない (SHALL)。

Store / DSL 方式デモは MAUI の一致対象外とする — デモ対象の公開 API (Store 直接操作・宣言 DSL) が MAUI に存在しないため (phase-4 agenda 論点8決定。sample-parity 規約への例外条項「デモ対象の公開 API が存在する platform に限る」の追加は本 change の蒸留時に concepts へ反映する)。CustomCell デモは phase-5 で追随する一時的な片側先行として扱う。

#### Scenario: 画面集合の一致

- **GIVEN** iOS / Android / MAUI の3サンプルアプリ
- **WHEN** ルートメニューと4デモページを見比べる
- **THEN** メニュー項目・画面タイトル・Section / Cell 構成・文言・デモデータが一致している (Store / DSL デモは上記の例外により、CustomCell デモは phase-5 追随までの片側先行として、MAUI に存在しなくてよい)

#### Scenario: デモ操作の動作

- **GIVEN** MAUI サンプルの入力 Cell デモページ
- **WHEN** 各入力 Cell を操作して値を変更する
- **THEN** iOS / Android サンプルの同ページと同じ挙動 (値の反映・表示更新) を示す

## REMOVED Requirements

### Requirement: LabelCell 検証ページ

**Reason**: phase-3 の sample-parity 検証枠として置いた暫定画面。基本 Cell デモページの追加により役目を終えるため、同一 change 内で削除する (残すと「画面の集合は全 platform で同一」への余剰画面になる)。
