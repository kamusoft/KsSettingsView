package jp.kamusoft.kssettingsview.core

/**
 * [SettingsRootDiff.UpdateAccessory] で Accessory の更新対象（Root H/F / Section H/F）を
 * 表現する sum type。
 *
 * - [RootHeader] / [RootFooter]: Root レベル H/F
 * - [SectionHeader] / [SectionFooter]: 指定 Section の H/F（[sectionId] で対象を特定）
 *
 * Root と Section で H/F の責務が分かれるため、位置を型で表現して
 * [SettingsRootDiff.UpdateAccessory] の適用先を一意に決める。
 */
sealed interface AccessoryTarget {

    /** Root レベルのヘッダ */
    data object RootHeader : AccessoryTarget

    /** Root レベルのフッタ */
    data object RootFooter : AccessoryTarget

    /**
     * 指定 Section のヘッダ
     * @property sectionId 対象 Section の ID
     */
    data class SectionHeader(val sectionId: String) : AccessoryTarget

    /**
     * 指定 Section のフッタ
     * @property sectionId 対象 Section の ID
     */
    data class SectionFooter(val sectionId: String) : AccessoryTarget
}
