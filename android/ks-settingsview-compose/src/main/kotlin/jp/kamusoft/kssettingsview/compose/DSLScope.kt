package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DSLIconModifiableCell
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import jp.kamusoft.kssettingsview.ui.KsImage

/**
 * `KsSettingsView { ... }` DSL Composable のレシーバスコープ。
 *
 * 内部に [DSLSectionNode] を蓄え、最終的に [build] で [DSLRootTree] を組み立てる。
 */
@SettingsRootDsl
class DSLSettingsRootScope internal constructor() {

    private val sectionNodes: MutableList<DSLSectionNode> = mutableListOf()

    /**
     * Section を 1 つ追加する。
     *
     * @param header 文字列ヘッダ（省略可、`headerContent` と排他指定）
     * @param footer 文字列フッタ（省略可、`footerContent` と排他指定）
     * @param headerContent 任意 Composable ヘッダ（省略可、`header` と排他指定）
     * @param footerContent 任意 Composable フッタ（省略可、`footer` と排他指定）
     * @param isHeaderVisible Header 表示トグル（`false` で内容があっても Header を隠す）
     * @param isFooterVisible Footer 表示トグル（`false` で内容があっても Footer を隠す）
     * @param block [DSLSectionScope] のレシーバラムダ
     */
    fun Section(
        header: String? = null,
        footer: String? = null,
        headerContent: (@Composable () -> Unit)? = null,
        footerContent: (@Composable () -> Unit)? = null,
        headerHeight: Double = -1.0,
        isVisible: Boolean = true,
        isHeaderVisible: Boolean = true,
        isFooterVisible: Boolean = true,
        block: DSLSectionScope.() -> Unit = {},
    ): SectionHandle {
        require(!(header != null && headerContent != null)) {
            "Section: header と headerContent は同時指定できません"
        }
        require(!(footer != null && footerContent != null)) {
            "Section: footer と footerContent は同時指定できません"
        }
        val headerAccessory: SectionAccessory? = when {
            header != null -> SectionAccessory.Text(header)
            headerContent != null -> SectionAccessory.View(KsAnyView.Compose { headerContent() })
            else -> null
        }
        val footerAccessory: SectionAccessory? = when {
            footer != null -> SectionAccessory.Text(footer)
            footerContent != null -> SectionAccessory.View(KsAnyView.Compose { footerContent() })
            else -> null
        }
        val sectionScope = DSLSectionScope().apply(block)
        val sectionNode = DSLSectionNode(
            header = headerAccessory,
            footer = footerAccessory,
            cellNodes = sectionScope.build(),
            headerHeight = headerHeight,
            isVisible = isVisible,
            isHeaderVisible = isHeaderVisible,
            isFooterVisible = isFooterVisible,
        )
        sectionNodes.add(sectionNode)
        return SectionHandle(scope = this, index = sectionNodes.size - 1)
    }

    /**
     * 動的コレクションを Section 群として展開する。
     *
     * @param items 展開対象のコレクション
     * @param key 各要素の identity を返す lambda（ID 自動採番に使用）
     * @param content 各要素から Section を構築するレシーバラムダ
     */
    fun <T> forEach(
        items: List<T>,
        key: (T) -> Any,
        content: DSLSettingsRootScope.(T) -> Unit,
    ) {
        val before = sectionNodes.size
        for (item in items) {
            content(item)
            // content 内で追加された新規 Section に key 由来のヒントを付与する。
            val keyValue = key(item)
            for (i in before until sectionNodes.size) {
                val existing = sectionNodes[i]
                if (existing.identityHint == null) {
                    sectionNodes[i] = existing.copy(
                        identityHint = DSLIdentityHint.ForEach(keyValue),
                    )
                }
            }
        }
    }

    /**
     * `Section { ... }.sectionID(id)` のための中置記法的拡張は外部関数（中置）で提供する。
     * ここではレジストリ的にヒントを記録するためのメソッドを公開する。
     */
    internal fun overrideLastSectionId(hint: DSLIdentityHint) {
        if (sectionNodes.isEmpty()) return
        val last = sectionNodes.last()
        sectionNodes[sectionNodes.size - 1] = last.copy(identityHint = hint)
    }

    /**
     * [SectionHandle] から指定された位置 [index] の Section に Header accessory を上書きする。
     */
    internal fun updateSectionHeader(index: Int, accessory: SectionAccessory?) {
        if (index !in sectionNodes.indices) return
        val current = sectionNodes[index]
        sectionNodes[index] = current.copy(header = accessory)
    }

    /**
     * [SectionHandle] から指定された位置 [index] の Section に Footer accessory を上書きする。
     */
    internal fun updateSectionFooter(index: Int, accessory: SectionAccessory?) {
        if (index !in sectionNodes.indices) return
        val current = sectionNodes[index]
        sectionNodes[index] = current.copy(footer = accessory)
    }

    /**
     * [SectionHandle] から指定された位置 [index] の Section に明示 ID ヒントを上書きする。
     */
    internal fun overrideSectionIdAt(index: Int, hint: DSLIdentityHint) {
        if (index !in sectionNodes.indices) return
        val current = sectionNodes[index]
        sectionNodes[index] = current.copy(identityHint = hint)
    }

    /**
     * 蓄積された Section ノード列を返す。
     */
    internal fun build(): List<DSLSectionNode> = sectionNodes.toList()
}

/**
 * `Section { ... }` の内部スコープ。
 */
@SettingsRootDsl
class DSLSectionScope internal constructor() {

    private val cellNodes: MutableList<DSLCellNode> = mutableListOf()

    /**
     * Cell を 1 つ追加する。
     *
     * `cell` が [DSLExplicitIdCell]（`Cell.cellID(...)` で付与された sentinel ラッパ）の場合、
     * 内部 Cell を取り出して [DSLCellNode.cell] に格納し、明示 ID を
     * [DSLCellNode.identityHint] = [DSLIdentityHint.Explicit] として転写する。
     * これにより `.cellID(...)` 明示指定が Cell ID 採番ヒントとして正しく機能する
     * （`DSLRootTree.resolvedSections()` で Positional フォールバックより優先される）。
     *
     * @param cell 追加する Cell（`Cell.cellID(...)` 適用後の [DSLExplicitIdCell] も受理）
     */
    fun cell(cell: Cell): CellHandle {
        val node = when (cell) {
            is DSLExplicitIdCell -> DSLCellNode(
                cell = cell.wrapped,
                identityHint = DSLIdentityHint.Explicit(cell.explicitId),
            )
            else -> DSLCellNode(cell = cell)
        }
        cellNodes.add(node)
        return CellHandle(sectionScope = this, index = cellNodes.size - 1)
    }

    /**
     * `+Cell` 記法で Cell を追加する糖衣構文。
     *
     * `Section { +LabelCell(title = "...") }` のように `cell(...)` を省略して
     * 直接 Cell を流せる。
     */
    operator fun Cell.unaryPlus(): CellHandle = cell(this)

    /**
     * 動的コレクションを Cell 群として展開する。
     */
    fun <T> forEach(
        items: List<T>,
        key: (T) -> Any,
        content: DSLSectionScope.(T) -> Unit,
    ) {
        val before = cellNodes.size
        for (item in items) {
            content(item)
            val keyValue = key(item)
            for (i in before until cellNodes.size) {
                val existing = cellNodes[i]
                if (existing.identityHint == null) {
                    cellNodes[i] = existing.copy(
                        identityHint = DSLIdentityHint.ForEach(keyValue),
                    )
                }
            }
        }
    }

    /**
     * 直前に追加された Cell に明示 ID ヒントを付与する。
     * `.cellID(...)` 拡張関数から呼ばれる。
     */
    internal fun overrideLastCellId(hint: DSLIdentityHint) {
        if (cellNodes.isEmpty()) return
        val last = cellNodes.last()
        cellNodes[cellNodes.size - 1] = last.copy(identityHint = hint)
    }

    /**
     * [CellHandle] から指定された位置 [index] の Cell に明示 ID ヒントを上書きする。
     */
    internal fun overrideCellIdAt(index: Int, hint: DSLIdentityHint) {
        if (index !in cellNodes.indices) return
        val current = cellNodes[index]
        cellNodes[index] = current.copy(identityHint = hint)
    }

    /**
     * [CellHandle] から指定された位置 [index] の Cell の `style` を変換関数で書き換える。
     *
     * Cell が [DSLStyleModifiableCell] を実装している場合のみ反映され、未実装な Cell に
     * 対しては no-op になる。
     */
    internal fun mutateCellStyleAt(index: Int, transform: (CellStyle) -> CellStyle) {
        if (index !in cellNodes.indices) return
        val current = cellNodes[index]
        val baseCell = current.cell
        val newCell: Cell = when (baseCell) {
            is DSLStyleModifiableCell -> baseCell.withDSLStyle(transform(baseCell.style))
            else -> baseCell
        }
        cellNodes[index] = current.copy(cell = newCell)
    }

    /**
     * [CellHandle] から指定された位置 [index] の Cell の `icon` を新値で上書きする。
     *
     * Cell が [DSLIconModifiableCell] を実装している場合のみ反映され、未実装な Cell に
     * 対しては no-op になる（アイコン領域を持たない `CustomCell`）。
     */
    internal fun mutateCellIconAt(index: Int, icon: KsImage?) {
        if (index !in cellNodes.indices) return
        val current = cellNodes[index]
        val baseCell = current.cell
        val newCell: Cell = when (baseCell) {
            is DSLIconModifiableCell -> baseCell.withDSLIcon(icon)
            else -> baseCell
        }
        cellNodes[index] = current.copy(cell = newCell)
    }

    /**
     * 蓄積された Cell ノード列を返す。
     */
    internal fun build(): List<DSLCellNode> = cellNodes.toList()
}

// =============================================================================
// KsIdentifiable 版 forEach（key 省略形）
// =============================================================================

/**
 * [KsIdentifiable] を実装したコレクションを Section 群として展開する。
 *
 * `key` lambda を省略でき、内部で `it.id` を `key` として使用する
 * （`forEach(items, key = { it.id }, content = content)` に委譲）。
 *
 * 利用例:
 * ```kotlin
 * data class DemoItem(override val id: Int, val name: String) : KsIdentifiable
 *
 * KsSettingsView {
 *     forEach(items) { item ->
 *         Section(header = item.name) { ... }
 *     }
 * }
 * ```
 */
inline fun <reified T : KsIdentifiable> DSLSettingsRootScope.forEach(
    items: List<T>,
    noinline content: DSLSettingsRootScope.(T) -> Unit,
) {
    forEach(items, key = { it.id }, content = content)
}

/**
 * [KsIdentifiable] を実装したコレクションを Cell 群として展開する（セクション内 forEach の key 省略版）。
 */
inline fun <reified T : KsIdentifiable> DSLSectionScope.forEach(
    items: List<T>,
    noinline content: DSLSectionScope.(T) -> Unit,
) {
    forEach(items, key = { it.id }, content = content)
}
