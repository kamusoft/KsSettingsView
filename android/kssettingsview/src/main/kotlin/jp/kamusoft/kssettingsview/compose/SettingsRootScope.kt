package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot

/**
 * `settingsRoot { ... }` DSL のスコープを区別するための `@DslMarker`。
 *
 * 入れ子の `section { }` 内から `settingsRoot` のメソッドを直接呼び出すコンパイル時誤用を防止する。
 *
 * スコープ制御は receiver の**型**に付与された marker だけに由来するため、付与先は型宣言・型使用・
 * 型エイリアスに限定する。関数へ付けても効果が無いため、[Target] でコンパイルエラーにする。
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS)
public annotation class SettingsRootDsl

/**
 * `settingsRoot { ... }` DSL の receiver スコープ。
 *
 * 内部に Section リストを蓄え、最終的に [build] で `SettingsRoot` を組み立てる。
 *
 * Theme は Core の `SettingsRoot` ではなく UI 層が持つため（core/ADR-0009）、本 DSL は
 * `theme` 引数を受け取らない。Theme は `KsSettingsView(theme = ...)` 経路で渡す。
 */
@SettingsRootDsl
public class SettingsRootScope internal constructor() {

    private val sections: MutableList<Section> = mutableListOf()

    /**
     * Section を 1 つ追加する。
     *
     * 各引数の並びと既定値は [Section] に揃えてあり、指定した値はそのまま [Section] へ転写される。
     * 既定値の意味は [Section] の KDoc を参照する。
     *
     * @param id Section の一意 ID
     * @param header Section ヘッダ。文字列ヘッダは `header = SectionAccessory.Text("一般")` のように
     *   包むか、文字列ヘッダを取る `section` オーバーロードを使う
     * @param footer Section フッタ
     * @param headerHeight Section ヘッダの高さ
     * @param isVisible Section の可視性
     * @param isHeaderVisible Section ヘッダの表示トグル
     * @param isFooterVisible Section フッタの表示トグル
     * @param block [SectionScope] のレシーバラムダ。`cell(...)` で Cell を追加する
     */
    public fun section(
        id: String,
        header: SectionAccessory? = null,
        footer: SectionAccessory? = null,
        headerHeight: Double = -1.0,
        isVisible: Boolean = true,
        isHeaderVisible: Boolean = true,
        isFooterVisible: Boolean = true,
        block: SectionScope.() -> Unit = {},
    ) {
        val sectionScope = SectionScope().apply(block)
        sections.add(
            Section(
                id = id,
                header = header,
                footer = footer,
                cells = sectionScope.build(),
                headerHeight = headerHeight,
                isVisible = isVisible,
                isHeaderVisible = isHeaderVisible,
                isFooterVisible = isFooterVisible,
            ),
        )
    }

    /**
     * 文字列ヘッダ付きで Section を追加する糖衣関数。
     *
     * 引数の並びと既定値は `SectionAccessory` を取る `section` オーバーロードと同じで、
     * [header] / [footer] だけが文字列になる。
     * 既定値の意味は [Section] の KDoc を参照する。
     *
     * @param id Section の一意 ID
     * @param header 文字列ヘッダ。内部的に `SectionAccessory.Text(header)` にラップされる
     * @param footer 文字列フッタ。`null` を渡すとフッタなしになる。非 `null` なら
     *   `SectionAccessory.Text(footer)` にラップされる
     * @param headerHeight Section ヘッダの高さ
     * @param isVisible Section の可視性
     * @param isHeaderVisible Section ヘッダの表示トグル
     * @param isFooterVisible Section フッタの表示トグル
     * @param block [SectionScope] のレシーバラムダ。`cell(...)` で Cell を追加する
     */
    public fun section(
        id: String,
        header: String,
        footer: String? = null,
        headerHeight: Double = -1.0,
        isVisible: Boolean = true,
        isHeaderVisible: Boolean = true,
        isFooterVisible: Boolean = true,
        block: SectionScope.() -> Unit = {},
    ) {
        section(
            id = id,
            header = SectionAccessory.Text(header),
            footer = footer?.let { SectionAccessory.Text(it) },
            headerHeight = headerHeight,
            isVisible = isVisible,
            isHeaderVisible = isHeaderVisible,
            isFooterVisible = isFooterVisible,
            block = block,
        )
    }

    /**
     * 蓄積された Section から [SettingsRoot] を構築する。
     */
    internal fun build(): SettingsRoot {
        return SettingsRoot(sections = sections.toList())
    }
}

/**
 * `section { ... }` DSL の receiver スコープ。
 *
 * 内部に Cell リストを蓄え、最後に親 [SettingsRootScope] へ [Section] を渡す。
 */
@SettingsRootDsl
public class SectionScope internal constructor() {

    private val cells: MutableList<Cell> = mutableListOf()

    /**
     * 任意 Cell を 1 つ追加する。
     *
     * @param cell 追加する Cell
     */
    public fun cell(cell: Cell) {
        cells.add(cell)
    }

    /**
     * 蓄積された Cell リストを返す。
     */
    internal fun build(): List<Cell> = cells.toList()
}

/**
 * `settingsRoot { ... }` DSL のエントリポイント。
 *
 * 構築するのは Core の [SettingsRoot] のみで、Theme は受け取らない（core/ADR-0009）。Theme は
 * `KsSettingsView(theme = ...)` または `SettingsRootStore(initialTheme = ...)` 経路で渡す。
 *
 * @param block [SettingsRootScope] のレシーバラムダ
 * @return 構築された [SettingsRoot]
 */
public fun settingsRoot(
    block: SettingsRootScope.() -> Unit,
): SettingsRoot {
    return SettingsRootScope().apply(block).build()
}
