package jp.kamusoft.kssettingsview.core

/**
 * テスト専用ダミー Cell（モジュール内テストから利用）。
 *
 * `Cell` は通常の `interface` のため、技術的には外部モジュールからも実装可能だが、
 * 本ダミーは Core / UI モジュールの内部テストから利用する想定で `internal` 可視性に
 * 抑え、production API として漏れないようにしている。Kotlin の `internal` は
 * Gradle compilation スコープのため、`ks-settingsview-core` の test compilation と
 * main compilation 双方からは参照できる。後続変更提案で具象 Cell が追加されれば
 * 本ファイルは削除される。
 *
 * `purify-core-extract-style-to-ui-layer` で `Cell` から `val style: CellStyle` 抽象が
 * 削除されたため、ダミー Cell も `style` フィールドを持たない。
 */
internal data class DummyLabelCell(
    override val id: String,
    val title: String,
) : Cell

/**
 * テスト専用ダミー Cell（モジュール内テストから利用）。
 *
 * 詳細は [DummyLabelCell] のドキュメントを参照。
 */
internal data class DummySwitchCell(
    override val id: String,
    val isOn: Boolean,
) : Cell
