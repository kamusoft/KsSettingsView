package jp.kamusoft.kssettingsview.core

/**
 * テスト専用ダミー Cell（モジュール内テストから利用）。
 *
 * `Cell` は通常の `interface` のため、技術的には外部モジュールからも実装可能だが、
 * 本ダミーはモジュール内テストから利用する想定で `internal` 可視性に抑え、
 * production API として漏れないようにしている。Kotlin の `internal` は
 * Gradle compilation スコープのため、本モジュールの test compilation と
 * main compilation 双方から参照できる。
 *
 * `Cell` は `val style: CellStyle` 抽象を要求しない（core/ADR-0009）ため、
 * ダミー Cell も `style` フィールドを持たない。
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
