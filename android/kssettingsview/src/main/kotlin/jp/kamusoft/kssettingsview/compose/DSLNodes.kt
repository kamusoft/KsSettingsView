package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DSLIconModifiableCell
import jp.kamusoft.kssettingsview.ui.DSLStyleModifiableCell
import jp.kamusoft.kssettingsview.ui.KsImage

/**
 * DSL（`KsSettingsView { Section { Cell... } }`）の中間ノード型。
 *
 * 目的:
 *   - DSL ビルド中に Section / Cell の **ID 採番ヒント** を保持する。
 *   - 評価結果として `SettingsRoot` を取り出せるよう、`resolve(...)` で
 *     `Section.id` / `Cell.id` を確定する。
 *
 * `purify-core-extract-style-to-ui-layer` で `SettingsRoot` から `theme` フィールドが
 * 削除されたため、`DSLRootTree` も `theme` を保持しない（Theme は呼び出し側で別管理）。
 */
internal data class DSLCellNode(
    val cell: Cell,
    val identityHint: DSLIdentityHint? = null,
) {
    /**
     * セクション位置・Cell 型から導出した安定 ID を返す。
     */
    fun resolvedId(sectionId: String, indexInSection: Int): String {
        identityHint?.let { return DSLIdentityId.id(from = it) }
        val cellTypeName = cell::class.qualifiedName ?: "Cell"
        return DSLIdentityId.id(
            from = DSLIdentityHint.Positional(
                sectionId = sectionId,
                indexInSection = indexInSection,
                cellType = cellTypeName,
            ),
        )
    }
}

internal data class DSLSectionNode(
    val header: SectionAccessory? = null,
    val footer: SectionAccessory? = null,
    val identityHint: DSLIdentityHint? = null,
    val cellNodes: List<DSLCellNode> = emptyList(),
    /**
     * Section ヘッダ高さ（既定 `-1.0` = 自動）。`Section.headerHeight` に転写される。
     */
    val headerHeight: Double = -1.0,
    /**
     * Section 可視性（既定 `true`）。`Section.isVisible` に転写される。
     */
    val isVisible: Boolean = true,
    /**
     * Section Header の表示トグル（既定 `true`）。`Section.isHeaderVisible` に転写される。
     */
    val isHeaderVisible: Boolean = true,
    /**
     * Section Footer の表示トグル（既定 `true`）。`Section.isFooterVisible` に転写される。
     */
    val isFooterVisible: Boolean = true,
) {
    /**
     * ルート位置から導出した安定 ID を返す。
     */
    fun resolvedId(rootIdx: Int): String {
        identityHint?.let { return DSLIdentityId.id(from = it) }
        if (header is SectionAccessory.Text) {
            return DSLIdentityId.id(
                from = DSLIdentityHint.HeaderText(rootIdx = rootIdx, text = header.value),
            )
        }
        return DSLIdentityId.id(from = DSLIdentityHint.RootPosition(rootIdx = rootIdx))
    }
}

/**
 * DSL 評価結果のルート。Section ノード列 + Root H/F（パラメータ由来）を保持する。
 *
 * Theme は本ツリーに含まれない（`KsSettingsView(theme = ...)` 引数で別経路として扱う）。
 */
internal data class DSLRootTree(
    val sectionNodes: List<DSLSectionNode>,
    val rootHeader: RootAccessory? = null,
    val rootFooter: RootAccessory? = null,
) {
    /** Section / Cell の resolved ID を反映した `List<Section>` を返す。 */
    fun resolvedSections(): List<Section> {
        return sectionNodes.mapIndexed { idx, sectionNode ->
            val sectionId = sectionNode.resolvedId(idx)
            val resolvedCells = sectionNode.cellNodes.mapIndexed { cellIdx, cellNode ->
                val resolvedCellId = cellNode.resolvedId(sectionId, cellIdx)
                rebindCellId(cellNode.cell, resolvedCellId)
            }
            Section(
                id = sectionId,
                header = sectionNode.header,
                footer = sectionNode.footer,
                cells = resolvedCells,
                headerHeight = sectionNode.headerHeight,
                isVisible = sectionNode.isVisible,
                isHeaderVisible = sectionNode.isHeaderVisible,
                isFooterVisible = sectionNode.isFooterVisible,
            )
        }
    }

    /** DSL ツリーから `SettingsRoot` を構築する。 */
    fun toSettingsRoot(): SettingsRoot {
        return SettingsRoot(sections = resolvedSections())
    }
}

/**
 * Cell の `id` を新しい値に差し替えた copy を返すヘルパ。
 *
 * `Cell` インターフェースは `val id: String` のみを要求しており、書き換え API を持たない。
 * 具象 `data class` であれば Kotlin の reflection `copy()` を呼べるが、外部実装の Cell には
 * 対応できない。本提案では「DSL で利用される具象 Cell は `DSLReidentifiableCell` を実装する」
 * 規約とし、未実装な場合は元 Cell をそのまま返す（利用者責任で id 安定性を確保）。
 */
internal fun rebindCellId(cell: Cell, newId: String): Cell {
    return if (cell is DSLReidentifiableCell) {
        if (cell.id == newId) cell else cell.withDSLId(newId)
    } else {
        cell
    }
}

/**
 * `Cell.cellID(id: Any)` で **明示 ID** を付与された Cell の sentinel ラッパ。
 *
 * - DSL 評価中に `cell(LabelCell(...).cellID("x"))` の形で出現する。
 * - [DSLSectionScope.cell] で unwrap され、内部 [wrapped] が [DSLCellNode.cell] に格納される。
 * - [explicitId] は [DSLCellNode.identityHint] に [DSLIdentityHint.Explicit] として転写される。
 *
 * 利用者からは本クラスを直接生成・参照することはなく、`Cell.cellID(id)` 拡張関数経由でのみ
 * 構築される。`DSLReidentifiableCell` / [DSLStyleModifiableCell] / [DSLIconModifiableCell] 各規約を
 * transparent に委譲することで、`.cellID(...).font(...).icon(...)` のような **modifier 連結後** でも
 * 明示 ID ヒントが失われない。
 *
 * 仕様: `settings-view-android-ui` "Cell ID 判定の優先順位 2"（`.cellID(...)` 明示指定の採用）。
 */
internal data class DSLExplicitIdCell(
    val wrapped: Cell,
    val explicitId: Any,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell {
    override val id: String get() = wrapped.id

    override val style: CellStyle
        get() = (wrapped as? DSLStyleModifiableCell)?.style ?: CellStyle()

    override fun withDSLId(newId: String): Cell {
        return if (wrapped is DSLReidentifiableCell) {
            DSLExplicitIdCell(wrapped = wrapped.withDSLId(newId), explicitId = explicitId)
        } else {
            // 元 Cell が DSLReidentifiableCell でなければ id 書き換え不可。元のまま維持。
            this
        }
    }

    override fun withDSLStyle(newStyle: CellStyle): Cell {
        return if (wrapped is DSLStyleModifiableCell) {
            DSLExplicitIdCell(wrapped = wrapped.withDSLStyle(newStyle), explicitId = explicitId)
        } else {
            this
        }
    }

    override fun withDSLIcon(newIcon: KsImage?): Cell {
        return if (wrapped is DSLIconModifiableCell) {
            DSLExplicitIdCell(wrapped = wrapped.withDSLIcon(newIcon), explicitId = explicitId)
        } else {
            this
        }
    }
}
