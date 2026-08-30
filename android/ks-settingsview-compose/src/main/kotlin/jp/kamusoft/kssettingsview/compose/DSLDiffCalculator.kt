package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.VisibilityAware

/**
 * DSL の旧宣言ツリーと新宣言ツリーから `SettingsRootDiff` 列を算出する。
 *
 * # 表示状態同期の三層分離（core/ADR-0010）
 *
 * [compute] は **構造同期の差分のみ**（Insert/Remove/Move/Section H/F/Root H/F/Theme）を算出する。
 * **内容変化（同一 id で内容プロパティのみが異なる Cell）では `ReplaceCell` を発行しない**
 * （構造同期は id 同一性のみ）。内容更新は [contentUpdates] で別途列挙し、Compose ラッパが
 * ViewHolder の部分更新経路（`store.replaceCell` → `applyDiff(ReplaceCell)` →
 * `notifyItemChanged`）へ流すことで、セルを再生成せず反映する。
 *
 * iOS 側 (`DSLDiffCalculator.swift`) は内容変化で `replaceCell` を発行し `reconfigureItems` 経路に
 * 載せる（プラットフォーム間で経路は異なるが、上位原則「構造同期は id 同一性のみ・内容更新は
 * セルを再生成しない」は共通。経路差は実装都合）。
 */
internal object DSLDiffCalculator {

    /**
     * Diff 算出に渡す resolved 済み DSL ツリー。
     *
     * Theme は構造モデルから分離され UI 層の独立経路で扱うため（core/ADR-0009）、
     * `SettingsRootDiff` 経由の構造同期の対象外であり、本ツリーには含まれない
     * （独立 API `store.applyTheme(_)` で扱う）。
     */
    data class ResolvedTree(
        val sections: List<Section>,
        val rootHeader: RootAccessory?,
        val rootFooter: RootAccessory?,
    )

    /**
     * 旧/新の DSL ルートツリー（resolved 済み）を比較し、Diff 列を返す。
     */
    fun compute(from: ResolvedTree, to: ResolvedTree): List<SettingsRootDiff> {
        val diffs = mutableListOf<SettingsRootDiff>()

        // 0. Full 更新を要する変化の preflight 検出（[requiresFullRefresh]）。
        //    検出時は `SettingsRootDiff.Full(newRoot)` のみを発行して終了する。
        if (requiresFullRefresh(from = from, to = to)) {
            return listOf(SettingsRootDiff.Full(SettingsRoot(sections = to.sections)))
        }

        // 構造同期は id 同一性のみで行う（core/ADR-0010）。
        // 早期 return 判定も「構造（Section/Cell の id 集合・順序）と H/F が同一か」で行う。
        // 内容プロパティのみが変化したケース（構造同一）では構造 Diff は空であり、内容更新は
        // contentUpdates() 経由で別途反映されるため、ここで早期 return してよい。
        // Theme は構造同期の対象外（`store.applyTheme(_)` 経由で別途反映される）。
        if (sameStructure(from, to)
            && from.rootHeader == to.rootHeader
            && from.rootFooter == to.rootFooter
        ) {
            return emptyList()
        }

        // 1. Section レベル
        diffs.addAll(sectionLevelDiffs(old = from.sections, new = to.sections))

        // 2. 各 Section 内 Cell レベル
        val oldByID = from.sections.associateBy { it.id }
        for (newSection in to.sections) {
            val oldSection = oldByID[newSection.id] ?: continue
            diffs.addAll(
                cellLevelDiffs(
                    sectionId = newSection.id,
                    old = oldSection.cells,
                    new = newSection.cells,
                ),
            )
        }

        // 1.5 Section H/F 変化（同 SectionID）
        for (newSection in to.sections) {
            val oldSection = oldByID[newSection.id] ?: continue
            if (oldSection.header != newSection.header) {
                diffs.add(
                    SettingsRootDiff.UpdateAccessory(
                        target = AccessoryTarget.SectionHeader(sectionId = newSection.id),
                        accessory = newSection.header?.let { SettingsAccessory.Section(it) },
                    ),
                )
            }
            if (oldSection.footer != newSection.footer) {
                diffs.add(
                    SettingsRootDiff.UpdateAccessory(
                        target = AccessoryTarget.SectionFooter(sectionId = newSection.id),
                        accessory = newSection.footer?.let { SettingsAccessory.Section(it) },
                    ),
                )
            }
        }

        // 3. Root H/F
        if (from.rootHeader != to.rootHeader) {
            diffs.add(
                SettingsRootDiff.UpdateAccessory(
                    target = AccessoryTarget.RootHeader,
                    accessory = to.rootHeader?.let { SettingsAccessory.Root(it) },
                ),
            )
        }
        if (from.rootFooter != to.rootFooter) {
            diffs.add(
                SettingsRootDiff.UpdateAccessory(
                    target = AccessoryTarget.RootFooter,
                    accessory = to.rootFooter?.let { SettingsAccessory.Root(it) },
                ),
            )
        }

        // 4. Theme は SettingsRootDiff に含めない（独立 API `store.applyTheme(_)` 経由で扱う）。
        //    本 calculator は Theme の異同を検知せず、呼び出し側（KsSettingsViewComposable）が
        //    `theme` パラメータの変化を検出して直接 `store.applyTheme(newTheme)` を呼ぶ責務を担う。

        return diffs
    }

    // MARK: - Section レベル突合

    private fun sectionLevelDiffs(
        old: List<Section>,
        new: List<Section>,
    ): List<SettingsRootDiff> {
        val diffs = mutableListOf<SettingsRootDiff>()
        val oldIds = old.map { it.id }.toSet()
        val newIds = new.map { it.id }.toSet()

        // 削除
        for (section in old) {
            if (section.id !in newIds) {
                diffs.add(SettingsRootDiff.RemoveSection(sectionId = section.id))
            }
        }

        // 追加
        for ((idx, section) in new.withIndex()) {
            if (section.id !in oldIds) {
                diffs.add(
                    SettingsRootDiff.InsertSection(index = idx, section = section),
                )
            }
        }

        // 移動
        for ((newIdx, section) in new.withIndex()) {
            if (section.id in oldIds) {
                val oldIdx = old.indexOfFirst { it.id == section.id }
                if (oldIdx != newIdx) {
                    diffs.add(SettingsRootDiff.MoveSection(from = oldIdx, to = newIdx))
                }
            }
        }

        return diffs
    }

    // MARK: - Cell レベル突合（構造同期のみ）
    //
    // 「表示状態同期の三層分離」（core/ADR-0010）に従い、本算出ロジックは
    // **id 同一性のみ** で Insert/Remove/Move を判定する。同一 id で内容プロパティだけが異なる
    // Cell に対して `ReplaceCell` を **発行しない**（構造同期は内容等価性を用いない）。内容更新は
    // [contentUpdates] が別途列挙し、Compose ラッパが ViewHolder の部分更新経路へ流す。

    private fun cellLevelDiffs(
        sectionId: String,
        old: List<Cell>,
        new: List<Cell>,
    ): List<SettingsRootDiff> {
        val diffs = mutableListOf<SettingsRootDiff>()
        val oldIds = old.map { it.id }.toSet()
        val newIds = new.map { it.id }.toSet()

        // 削除
        for (cell in old) {
            if (cell.id !in newIds) {
                diffs.add(SettingsRootDiff.RemoveCell(cellId = cell.id))
            }
        }

        // 追加
        for ((idx, cell) in new.withIndex()) {
            if (cell.id !in oldIds) {
                diffs.add(
                    SettingsRootDiff.InsertCell(sectionId = sectionId, index = idx, cell = cell),
                )
            }
        }

        // 移動（id 同一性のみ。内容変化での ReplaceCell は発行しない）
        for ((newIdx, cell) in new.withIndex()) {
            if (cell.id !in oldIds) continue
            val oldIdx = old.indexOfFirst { it.id == cell.id }
            if (oldIdx < 0) continue
            if (oldIdx != newIdx) {
                diffs.add(SettingsRootDiff.MoveCell(cellId = cell.id, toIndex = newIdx))
            }
        }

        return diffs
    }

    // MARK: - 構造同一性判定（id 同一性のみ）

    /**
     * 旧/新ツリーの **構造**（Section / Cell の id 集合・順序）が完全に同一かを判定する。
     *
     * 内容プロパティ（`title` / `isOn` 等）の異同は判定に含めない（構造同期は id 同一性のみ）。
     * H/F・Theme の異同は呼び出し側で別途比較する。
     */
    private fun sameStructure(from: ResolvedTree, to: ResolvedTree): Boolean {
        if (from.sections.size != to.sections.size) return false
        for (i in from.sections.indices) {
            val a = from.sections[i]
            val b = to.sections[i]
            if (a.id != b.id) return false
            // Section H/F は構造ではなく accessory 比較（呼び出し側が個別判定）だが、
            // 早期 return を安全に行うため、ここでも H/F の異同を構造同一性に含めて検出する。
            if (a.header != b.header) return false
            if (a.footer != b.footer) return false
            if (a.cells.size != b.cells.size) return false
            for (j in a.cells.indices) {
                if (a.cells[j].id != b.cells[j].id) return false
            }
        }
        return true
    }

    // MARK: - 内容更新の列挙（reconfigure 相当）

    /**
     * 同一 id を持つ Cell のうち、**内容（プロパティ）が変化した** Cell を列挙する。
     *
     * 「表示状態同期の三層分離」に従い、内容更新は構造 Diff（[compute]）には含めず、本メソッドが
     * 別途列挙する。Compose ラッパはこの結果を `store.replaceCell`（→ `applyDiff(ReplaceCell)`
     * → `notifyItemChanged` の部分更新）へ流し、セルを再生成せず内容のみ反映する。
     *
     * RadioCell のグループ連動（同一 `groupId` の旧選択セルで `selectedValue` が変化する）も、
     * 該当セルの内容変化として本メソッドで検出され、部分更新で ✓ 表示が更新される。
     *
     * 内容の比較は値型の `equals`（`onValueChanged` 等のクロージャは Cell 側 equals で除外済み）に
     * 委ねる。両ツリーに同一 id が存在する Cell のみを対象とし、追加・削除・移動は対象外
     * （それらは [compute] の構造 Diff が担う）。
     */
    fun contentUpdates(from: ResolvedTree, to: ResolvedTree): List<Cell> {
        // preflight が発火する場合、内容更新は `Full(newRoot)` の適用に内包されるため、
        // 本メソッドは **空リスト** を返す（[requiresFullRefresh]）。
        if (requiresFullRefresh(from = from, to = to)) {
            return emptyList()
        }
        val updates = mutableListOf<Cell>()
        val oldCellsById = HashMap<String, Cell>()
        for (section in from.sections) {
            for (cell in section.cells) {
                oldCellsById[cell.id] = cell
            }
        }
        for (section in to.sections) {
            for (cell in section.cells) {
                val oldCell = oldCellsById[cell.id] ?: continue
                // 同一 id で内容が異なる → 内容更新対象
                if (oldCell != cell) {
                    updates.add(cell)
                }
            }
        }
        return updates
    }

    // MARK: - Full 更新を要する変化の preflight 検出

    /**
     * 旧/新ツリーの差分を通常の構造同期 / 内容更新経路では表示へ届けられず、`Full(newRoot)` の
     * 発行が必要かを判定する。
     *
     * [compute] と [contentUpdates] の双方がこの単一の判定を用いる。両者の分岐条件が食い違うと
     * `Full` と内容更新の二重反映、または更新の取りこぼしが無音で起きるため、preflight の対象を
     * 増やすときも判定は本メソッドに集約する。
     */
    private fun requiresFullRefresh(from: ResolvedTree, to: ResolvedTree): Boolean =
        containsVisibilityChange(from = from, to = to) ||
            containsHeaderHeightChange(from = from, to = to) ||
            containsAccessoryVisibilityChange(from = from, to = to)

    /**
     * 旧/新ツリーの間で、同一 ID の Section について `isVisible` が変化、または同一 Cell ID で
     * `(cell as? VisibilityAware)?.isVisible ?: true` が変化しているかを判定する。
     *
     * 可視性差分は ID 集合を扱う構造同期にも、同一 Cell を再構成する内容更新にも収まらないため、
     * 検出した場合は `Full(newRoot)` のみを発行し、`contentUpdates` は空リストを返す
     * （core/ADR-0010）。
     */
    internal fun containsVisibilityChange(from: ResolvedTree, to: ResolvedTree): Boolean {
        val oldSectionVisible = HashMap<String, Boolean>()
        val oldCellVisible = HashMap<String, Boolean>()
        for (section in from.sections) {
            oldSectionVisible[section.id] = section.isVisible
            for (cell in section.cells) {
                oldCellVisible[cell.id] = (cell as? VisibilityAware)?.isVisible ?: true
            }
        }
        for (section in to.sections) {
            val oldVis = oldSectionVisible[section.id]
            if (oldVis != null && oldVis != section.isVisible) {
                return true
            }
            for (cell in section.cells) {
                val newVis = (cell as? VisibilityAware)?.isVisible ?: true
                val ov = oldCellVisible[cell.id]
                if (ov != null && ov != newVis) {
                    return true
                }
            }
        }
        return false
    }

    // MARK: - Header / Footer 表示トグル変化の preflight 検出

    /**
     * 旧/新ツリーの間で、同一 ID の Section の `isHeaderVisible` / `isFooterVisible` が
     * 変化しているかを判定する。
     *
     * トグルは accessory の値そのものを変えないため Section H/F の accessory 比較には現れず、
     * 構造同期（id 同一性）にも現れない。検出した場合は `SettingsRootDiff.Full(newRoot)` のみを
     * 発行し、`contentUpdates` は空リストを返す。これにより DSL 経由のトグル変更は Store 経由と
     * 同じ表示結果へ到達する（core/ADR-0023、core/ADR-0018）。
     */
    internal fun containsAccessoryVisibilityChange(from: ResolvedTree, to: ResolvedTree): Boolean {
        val oldToggles = HashMap<String, Pair<Boolean, Boolean>>()
        for (section in from.sections) {
            oldToggles[section.id] = section.isHeaderVisible to section.isFooterVisible
        }
        for (section in to.sections) {
            val old = oldToggles[section.id] ?: continue
            if (old.first != section.isHeaderVisible || old.second != section.isFooterVisible) {
                return true
            }
        }
        return false
    }

    // MARK: - headerHeight 変化の preflight 検出

    /**
     * 旧/新ツリーの間で、同一 ID の Section の `headerHeight` が変化しているかを判定する。
     *
     * 検出対象は固定高さ間の変更（正値 → 別の正値）、自動から固定（`-1.0` → 正値）、
     * 固定から自動（正値 → `-1.0`）のいずれも含む。
     *
     * headerHeight は Section H/F の accessory 比較にも構造同期（id 同一性）にも現れないため、
     * 通常の Diff 経路では表示へ届かない。可視性変化と同じく検出した場合は
     * `SettingsRootDiff.Full(newRoot)` のみを発行し、`contentUpdates` は空リストを返す。
     * これにより DSL 経由の headerHeight 変更は Store 経由と同じ表示結果へ到達する
     * （core/ADR-0018）。
     */
    internal fun containsHeaderHeightChange(from: ResolvedTree, to: ResolvedTree): Boolean {
        val oldHeaderHeights = HashMap<String, Double>()
        for (section in from.sections) {
            oldHeaderHeights[section.id] = section.headerHeight
        }
        for (section in to.sections) {
            val oldHeight = oldHeaderHeights[section.id] ?: continue
            if (oldHeight != section.headerHeight) {
                return true
            }
        }
        return false
    }
}

