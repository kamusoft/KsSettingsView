package jp.kamusoft.kssettingsview.ui

import android.util.Log
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
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.cancellation.CancellationException

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
public class SettingsRootStore(
    initialRoot: SettingsRoot,
    initialTheme: Theme = Theme(),
) {

    private val _state: MutableStateFlow<SettingsRoot> = MutableStateFlow(initialRoot)

    /** 現在の `SettingsRoot` 状態。Compose `collectAsState` 等で監視可能。 */
    public val state: StateFlow<SettingsRoot> = _state.asStateFlow()

    private val _theme: MutableStateFlow<Theme> = MutableStateFlow(initialTheme)

    /**
     * 現在の `Theme` 状態。`KsSettingsView.bind(store)` が購読し、変更を View に反映する。
     *
     * `applyTheme(_)` で更新される。`SettingsRootDiff` 経路では配信しない（独立 StateFlow）。
     */
    public val theme: StateFlow<Theme> = _theme.asStateFlow()

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

    /**
     * Root H/F の更新を知らせる相手（bind 中の [RootAccessoryReceiver]）の登録。
     *
     * Root H/F は本 Store の現在状態に含まれない（core/ADR-0005）ため、通知を取りこぼすと復元できない。
     * [diffs] は replay を持たず容量も有限で、購読の開始が bind から戻る時点に間に合う保証も無いので、
     * Root 対象だけは登録先へ同期に直接知らせる（core/ADR-0033）。
     *
     * 保持するのは登録だけで値は持たない。相手は弱参照で覚え、参照が切れた登録は次に知らせる時点で
     * 取り除く。どのスレッドからの更新でも壊れないよう、走査と追加・削除は複製で進む実装を使う。
     */
    private val rootAccessoryReceivers: CopyOnWriteArrayList<WeakReference<RootAccessoryReceiver>> =
        CopyOnWriteArrayList()

    // MARK: - Root 全体操作

    /** `SettingsRoot` 全体を差し替える。 */
    public fun replaceAll(root: SettingsRoot) {
        _state.value = root
        emitDiff(SettingsRootDiff.Full(root))
    }

    // MARK: - Section 操作

    /** Section を追加する。 */
    public fun insertSection(section: Section, at: Int) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val clamped = at.coerceIn(0, sections.size)
        sections.add(clamped, section)
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.InsertSection(index = clamped, section = section))
    }

    /** 指定 ID の Section を削除する。存在しない ID は no-op。 */
    public fun removeSection(sectionId: String) {
        val current = _state.value
        val sections = current.sections.toMutableList()
        val index = sections.indexOfFirst { it.id == sectionId }
        if (index < 0) return
        sections.removeAt(index)
        _state.value = current.copy(sections = sections.toList())
        emitDiff(SettingsRootDiff.RemoveSection(sectionId = sectionId))
    }

    /** Section の順序を変更する。範囲外 from は no-op。 */
    public fun moveSection(from: Int, to: Int) {
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
    public fun replaceSection(sectionId: String, new: Section) {
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
    public fun insertCell(cell: Cell, sectionId: String, at: Int) {
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
    public fun removeCell(cellId: String) {
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
    public fun replaceCell(cellId: String, new: Cell) {
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
    public fun replaceCells(updates: List<Pair<String, Cell>>) {
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
    public fun moveCell(cellId: String, to: Int) {
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
     *
     * どのスレッドから呼んでもよい。Root H/F の値は bind 中の Host へその場で知らせ、表示への反映は
     * Host がメインスレッドで行う。ある Host での反映が失敗しても呼び出し元へは伝わらず、他の Host
     * への知らせと Diff の emit は続く。
     */
    public fun updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?) {
        when (target) {
            AccessoryTarget.RootHeader, AccessoryTarget.RootFooter -> {
                // state は変えず、bind 中の Host へ直接知らせる（表示への反映は Host が行う）。
                deliverRootAccessory(
                    SettingsRootDiff.UpdateAccessory(target = target, accessory = accessory),
                )
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
    public fun invalidateAccessoryMeasurement(target: AccessoryTarget) {
        _accessoryMeasureInvalidations.tryEmit(target)
    }

    // MARK: - Theme 操作

    /**
     * `Theme` 全体を更新する。`SettingsRootDiff` は emit しない（独立 [theme] StateFlow で配信）。
     *
     * @param theme 新しい [Theme]
     */
    public fun applyTheme(theme: Theme) {
        _theme.value = theme
    }

    // MARK: - Root accessory の受け口

    /**
     * Root H/F の更新を知らせる相手として [receiver] を登録する。
     *
     * 登録は相手ごとに 1 件で、同じ相手を登録し直しても増えない。登録は [receiver] を弱参照で覚える
     * ため、Store が生きていても相手の寿命は延びない。
     */
    internal fun registerRootAccessoryReceiver(receiver: RootAccessoryReceiver) {
        pruneRootAccessoryReceivers()
        if (rootAccessoryReceivers.any { it.get() === receiver }) return
        rootAccessoryReceivers.add(WeakReference(receiver))
    }

    /** [receiver] への知らせを止める。登録が無ければ何もしない。 */
    internal fun unregisterRootAccessoryReceiver(receiver: RootAccessoryReceiver) {
        rootAccessoryReceivers.removeIf { reference ->
            val registered = reference.get()
            registered == null || registered === receiver
        }
    }

    /**
     * Root 対象の更新を、登録されている全ての相手へ知らせる。
     *
     * 自分を配送元として渡すことで、受け取り側は現在結び付いている Store からの更新だけを受理できる。
     * ある相手での失敗は記録に残すだけで、他の相手への知らせと Diff の emit は続ける。ただしコルーチンの
     * キャンセルの合図は握り潰さずそのまま投げ直す — 更新の呼び出し元が中断されたことを消してしまうため。
     */
    private fun deliverRootAccessory(update: SettingsRootDiff.UpdateAccessory) {
        pruneRootAccessoryReceivers()
        for (reference in rootAccessoryReceivers) {
            val receiver = reference.get() ?: continue
            try {
                receiver.receiveRootAccessoryUpdate(source = this, update = update)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: RuntimeException) {
                Log.e(LOG_TAG, "updateAccessory: root accessory delivery failed for a receiver", error)
            }
        }
    }

    /** 参照が切れた登録を取り除く。 */
    private fun pruneRootAccessoryReceivers() {
        rootAccessoryReceivers.removeIf { it.get() == null }
    }

    /** Test / 診断用に、現在生きている登録の件数を返す。 */
    internal fun internalRootAccessoryReceiverCount(): Int {
        pruneRootAccessoryReceivers()
        return rootAccessoryReceivers.size
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

    public companion object {

        /** 診断ログのタグ。 */
        private const val LOG_TAG = "SettingsRootStore"

        /**
         * Preview / Test 用ファクトリ。
         *
         * @param root 初期 `SettingsRoot`
         * @param theme 初期 `Theme`（既定 `Theme()`）
         * @return 指定 root / theme を初期値とする Store
         */
        internal fun preview(root: SettingsRoot, theme: Theme = Theme()): SettingsRootStore {
            return SettingsRootStore(initialRoot = root, initialTheme = theme)
        }
    }
}
