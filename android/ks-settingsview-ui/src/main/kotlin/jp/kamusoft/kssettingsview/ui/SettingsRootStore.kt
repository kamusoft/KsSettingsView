package jp.kamusoft.kssettingsview.ui

import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * `SettingsRoot` の状態管理と部分更新 Diff 発行を担う Store。
 *
 * Compose / UI 層から `state.value` で現在の `SettingsRoot` を、`theme.value` で現在の
 * `Theme` を取得でき、`diffs` Flow を `collect` することで Store のメソッド呼び出しに対応する
 * `SettingsRootDiff` を受け取れる。
 *
 * # Theme の扱い
 *
 * `Theme` は UI 層に属する（core/ADR-0009）ため Core の `SettingsRoot` は保持せず、本 Store が
 * `initialTheme` から `val theme: StateFlow<Theme>` として独立に配信する。差し替えは
 * [applyTheme] で行い、構造 Diff（`SettingsRootDiff`）の経路には載せない。
 *
 * 内部 [MutableSharedFlow] は `replay = 0`、`extraBufferCapacity = 64` で構成する。
 */
class SettingsRootStore(
    initialRoot: SettingsRoot,
    initialTheme: Theme = Theme(),
) {

    private val _state: MutableStateFlow<SettingsRoot> = MutableStateFlow(initialRoot)

    /** 現在の `SettingsRoot` 状態。Compose `collectAsState` 等で監視可能。 */
    val state: StateFlow<SettingsRoot> = _state.asStateFlow()

    private val _theme: MutableStateFlow<Theme> = MutableStateFlow(initialTheme)

    /**
     * 現在の `Theme` 状態。`KsSettingsView.bind(store)` が購読し、変更を View に反映する。
     *
     * `applyTheme(_)` で更新される。`SettingsRootDiff` 経路では配信しない（独立 StateFlow）。
     */
    val theme: StateFlow<Theme> = _theme.asStateFlow()

    // 内部 Diff Flow。
    private val _diffs: MutableSharedFlow<SettingsRootDiff> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /**
     * Diff Flow。`internal` 公開とし、同一モジュール内 UI 層
     * （[KsSettingsView] など）が `collect` して `applyDiff(...)` を呼ぶ統合経路を確立する。
     */
    internal val diffs: SharedFlow<SettingsRootDiff> = _diffs.asSharedFlow()

    private val _contentUpdateBatches: MutableSharedFlow<List<String>> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /**
     * 内容更新バッチ Flow。UI 層が collect し、対象 cellId 群を 1 回の部分更新で反映する。
     */
    internal val contentUpdateBatches: SharedFlow<List<String>> = _contentUpdateBatches.asSharedFlow()

    private val _accessoryMeasureInvalidations: MutableSharedFlow<AccessoryTarget> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /**
     * accessory の再計測要求 Flow。UI 層が collect し、対象領域だけを測り直す。
     */
    internal val accessoryMeasureInvalidations: SharedFlow<AccessoryTarget> =
        _accessoryMeasureInvalidations.asSharedFlow()

    // MARK: - Root 全体操作

    /** `SettingsRoot` 全体を差し替える。 */
    fun replaceAll(root: SettingsRoot) {
        _state.value = root
        emitDiff(SettingsRootDiff.Full(root))
    }

    // MARK: - Section 操作

    /** Section を追加する。 */
    fun insertSection(section: Section, at: Int) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val clamped = at.coerceIn(0, sections.size)
        sections.add(clamped, section)
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.InsertSection(index = clamped, section = section))
    }

    /** 指定 ID の Section を削除する。存在しない ID は no-op。 */
    fun removeSection(sectionId: String) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val index = sections.indexOfFirst { it.id == sectionId }
        if (index < 0) return
        sections.removeAt(index)
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.RemoveSection(sectionId = sectionId))
    }

    /** Section の順序を変更する。範囲外 from は no-op。 */
    fun moveSection(from: Int, to: Int) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        if (from !in sections.indices) return
        val moved = sections.removeAt(from)
        val clamped = to.coerceIn(0, sections.size)
        sections.add(clamped, moved)
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.MoveSection(from = from, to = to))
    }

    /** 指定 ID の Section を新しい Section で置換する。存在しない ID は no-op。 */
    fun replaceSection(sectionId: String, new: Section) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val index = sections.indexOfFirst { it.id == sectionId }
        if (index < 0) return
        sections[index] = new
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.ReplaceSection(sectionId = sectionId, newSection = new))
    }

    // MARK: - Cell 操作

    /** Cell を指定 Section に追加する。Section が存在しない場合は no-op。 */
    fun insertCell(cell: Cell, sectionId: String, at: Int) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val sectionIndex = sections.indexOfFirst { it.id == sectionId }
        if (sectionIndex < 0) return
        val target = sections[sectionIndex]
        val cells = target.cells.toMutableList()
        val clampedAt = at.coerceIn(0, cells.size)
        cells.add(clampedAt, cell)
        sections[sectionIndex] = target.copy(cells = cells.toList())
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.InsertCell(sectionId = sectionId, index = clampedAt, cell = cell))
    }

    /** 指定 ID の Cell を削除する。存在しない ID は no-op。 */
    fun removeCell(cellId: String) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        var found = false
        for (i in sections.indices) {
            val target = sections[i]
            val cellIndex = target.cells.indexOfFirst { it.id == cellId }
            if (cellIndex >= 0) {
                val cells = target.cells.toMutableList()
                cells.removeAt(cellIndex)
                sections[i] = target.copy(cells = cells.toList())
                _state.value = current.copy(sections = sections.toList())
                found = true
                break
            }
        }
        if (!found) return
        emitDiff(SettingsRootDiff.RemoveCell(cellId = cellId))
    }

    /** 指定 ID の Cell を新しい Cell で置換する。存在しない ID は no-op。 */
    fun replaceCell(cellId: String, new: Cell) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        var found = false
        for (i in sections.indices) {
            val target = sections[i]
            val cellIndex = target.cells.indexOfFirst { it.id == cellId }
            if (cellIndex >= 0) {
                val cells = target.cells.toMutableList()
                cells[cellIndex] = new
                sections[i] = target.copy(cells = cells.toList())
                _state.value = current.copy(sections = sections.toList())
                found = true
                break
            }
        }
        if (!found) return
        emitDiff(SettingsRootDiff.ReplaceCell(cellId = cellId, newCell = new))
    }

    /**
     * 複数 Cell の内容を一括置換し、**1 回のバッチ内容更新**として配信する。
     */
    fun replaceCells(updates: List<Pair<String, Cell>>) {
        if (updates.isEmpty()) return
        val current = _state.value
        val sections = current.sections.toMutableList()
        val appliedIds = mutableListOf<String>()
        for ((cellId, new) in updates) {
            for (i in sections.indices) {
                val target = sections[i]
                val cellIndex = target.cells.indexOfFirst { it.id == cellId }
                if (cellIndex >= 0) {
                    val cells = target.cells.toMutableList()
                    cells[cellIndex] = new
                    sections[i] = target.copy(cells = cells.toList())
                    appliedIds.add(cellId)
                    break
                }
            }
        }
        if (appliedIds.isEmpty()) return
        _state.value = current.copy(sections = sections.toList())
        _contentUpdateBatches.tryEmit(appliedIds.toList())
    }

    /** 指定 ID の Cell を同一 Section 内で移動する。存在しない ID は no-op。 */
    fun moveCell(cellId: String, to: Int) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        var found = false
        for (i in sections.indices) {
            val target = sections[i]
            val cellIndex = target.cells.indexOfFirst { it.id == cellId }
            if (cellIndex >= 0) {
                val cells = target.cells.toMutableList()
                val moved = cells.removeAt(cellIndex)
                val clamped = to.coerceIn(0, cells.size)
                cells.add(clamped, moved)
                sections[i] = target.copy(cells = cells.toList())
                _state.value = current.copy(sections = sections.toList())
                found = true
                break
            }
        }
        if (!found) return
        emitDiff(SettingsRootDiff.MoveCell(cellId = cellId, toIndex = to))
    }

    // MARK: - Accessory 操作

    /**
     * Accessory（Root H/F / Section H/F）を更新する。
     *
     * Section H/F の `sectionId` が現在状態に存在しない場合は、state 更新も Diff emit も行わない
     * no-op とする（core/ADR-0020）。Root H/F は `SettingsRoot` 値型に state を持たないため
     * sectionId 検証の対象外であり、常に Diff を emit する。
     */
    fun updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?) {
        when (target) {
            AccessoryTarget.RootHeader, AccessoryTarget.RootFooter -> {
                // state 変更不要（UI 層プロパティへの反映は applyDiff 側で行う）
            }
            is AccessoryTarget.SectionHeader -> {
                val updated = updateSectionAccessory(
                    sectionId = target.sectionId,
                    accessory = accessory,
                    isHeader = true,
                )
                if (!updated) return
            }
            is AccessoryTarget.SectionFooter -> {
                val updated = updateSectionAccessory(
                    sectionId = target.sectionId,
                    accessory = accessory,
                    isHeader = false,
                )
                if (!updated) return
            }
        }
        emitDiff(SettingsRootDiff.UpdateAccessory(target = target, accessory = accessory))
    }

    /**
     * 表示中の accessory 領域の高さを測り直すよう Host へ要求する。
     *
     * view accessory の中身が自分の計測結果を変えたときに呼ぶ。要求は一過性の通知であり、
     * Store の復元可能な現在状態は変化しない — 購読者がいない間に呼んだ要求は誰にも届かない
     * まま捨てられる。固定高さの領域では再計測しても高さが変わらず、実質的に何も起きない。
     *
     * @param target 再計測する accessory
     */
    fun invalidateAccessoryMeasurement(target: AccessoryTarget) {
        _accessoryMeasureInvalidations.tryEmit(target)
    }

    // MARK: - Theme 操作

    /**
     * `Theme` 全体を更新する。`SettingsRootDiff` は emit しない（独立 [theme] StateFlow で配信）。
     *
     * @param theme 新しい [Theme]
     */
    fun applyTheme(theme: Theme) {
        _theme.value = theme
    }

    // MARK: - 内部ヘルパ

    private fun emitDiff(diff: SettingsRootDiff) {
        _diffs.tryEmit(diff)
    }

    /**
     * Section H/F を更新する。
     *
     * @return state を更新した場合 `true`、`sectionId` が現在状態に存在せず何もしなかった場合 `false`
     */
    private fun updateSectionAccessory(
        sectionId: String,
        accessory: SettingsAccessory?,
        isHeader: Boolean,
    ): Boolean {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val index = sections.indexOfFirst { it.id == sectionId }
        if (index < 0) return false

        val newAccessory: SectionAccessory? = when (accessory) {
            is SettingsAccessory.Section -> accessory.accessory
            is SettingsAccessory.Root, null -> null
        }

        val target = sections[index]
        sections[index] = target.copy(
            header = if (isHeader) newAccessory else target.header,
            footer = if (isHeader) target.footer else newAccessory,
        )
        _state.value = current.copy(sections = sections.toList())
        return true
    }

    companion object {
        /**
         * Preview / Test 用ファクトリ。
         *
         * @param root 初期 `SettingsRoot`
         * @param theme 初期 `Theme`（既定 `Theme()`）
         * @return 指定 root / theme を初期値とする Store
         */
        fun preview(root: SettingsRoot, theme: Theme = Theme()): SettingsRootStore {
            return SettingsRootStore(initialRoot = root, initialTheme = theme)
        }
    }
}
