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
public sealed interface AccessoryTarget {

    /** Root レベルのヘッダ */
    public data object RootHeader : AccessoryTarget

    /** Root レベルのフッタ */
    public data object RootFooter : AccessoryTarget

    /**
     * 指定 Section のヘッダ
     * @property sectionId 対象 Section の ID
     */
    public data class SectionHeader(public val sectionId: String) : AccessoryTarget

    /**
     * 指定 Section のフッタ
     * @property sectionId 対象 Section の ID
     */
    public data class SectionFooter(public val sectionId: String) : AccessoryTarget
}
