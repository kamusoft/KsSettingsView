package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.SettingsRoot

/**
 * 設定ツリーを組み立てて [KsSettingsBridge.setRoot] へ渡す Builder。
 *
 * Section / Cell の ID は Bridge が採番し、追加 API の戻り値として呼び出し側へ返す
 * （maui/ADR-0005）。呼び出し側は返された ID だけを更新 API に渡す。
 */
class KsBridgeRootBuilder {

    private val mutableSections: MutableList<KsBridgeSection> = mutableListOf()

    /** 追加順の Section 群（スナップショット）。 */
    val sections: List<KsBridgeSection>
        get() = mutableSections.toList()

    /**
     * header / footer テキストを持つ Section を生成して末尾に追加する。
     *
     * @param headerText ヘッダテキスト（`null` でヘッダなし）
     * @param footerText フッタテキスト（`null` でフッタなし）
     * @return 追加した Section DTO（`sectionID` は Bridge 採番済み）
     */
    fun addSection(headerText: String?, footerText: String?): KsBridgeSection {
        val section = KsBridgeSection(headerText = headerText, footerText = footerText)
        mutableSections.add(section)
        return section
    }

    /**
     * 生成済みの Section DTO を末尾に追加する。
     *
     * @param section 追加する Section DTO
     * @return 追加した Section の sectionID
     */
    fun addSection(section: KsBridgeSection): String {
        mutableSections.add(section)
        return section.sectionID
    }

    /**
     * 指定 Section の末尾に Cell を追加する。
     *
     * @param cell 追加する Cell DTO（Cell 種を問わない）
     * @param sectionID 追加先 Section の sectionID
     * @return 追加した Cell の cellID。[sectionID] が Builder 内に存在しない場合は `null`（no-op）
     */
    fun addCell(cell: KsBridgeCell, sectionID: String): String? {
        val section = mutableSections.firstOrNull { it.sectionID == sectionID } ?: return null
        return section.addCell(cell)
    }

    /**
     * 指定 Section の末尾に LabelCell を追加する。
     *
     * Cell 種を問わない [addCell] と同じ動作で、LabelCell に限った書き味を残す。
     *
     * @param cell 追加する Cell DTO
     * @param sectionID 追加先 Section の sectionID
     * @return 追加した Cell の cellID。[sectionID] が Builder 内に存在しない場合は `null`（no-op）
     */
    fun addLabelCell(cell: KsBridgeLabelCell, sectionID: String): String? =
        addCell(cell, sectionID)

    /**
     * Builder の現在の内容から Native の `SettingsRoot` を組み立てる。
     *
     * @param relay 配下 Cell のユーザー操作を転送する中継
     */
    @JvmSynthetic
    internal fun makeRoot(relay: KsBridgeInteractionRelay): SettingsRoot = SettingsRoot(
        sections = mutableSections.map { it.makeSection(relay) },
    )
}
