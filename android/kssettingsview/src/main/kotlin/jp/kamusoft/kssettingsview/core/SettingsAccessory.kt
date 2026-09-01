package jp.kamusoft.kssettingsview.core

/**
 * [SettingsRootDiff.UpdateAccessory] で [RootAccessory] / [SectionAccessory] を
 * 統一的に扱うための sum type。
 *
 * - [Root]: Root レベル H/F に使用
 * - [Section]: Section レベル H/F に使用
 *
 * # 等価性契約
 *
 * `data class` の自動 `equals` / `hashCode` を採用する。内部 [RootAccessory.View] /
 * [SectionAccessory.View] ケースの `KsAnyView` は等価性判定対象外であり、これは内部の
 * `equals` / `hashCode` 手動実装（クラス一致のみで等価）により自動的に継承される。
 *
 * 本型は [RootAccessory] / [SectionAccessory] を置き換えない。Store API や
 * 利用者コードでは個別型を使い、Diff DTO 内部での統一表現専用とする。
 */
public sealed interface SettingsAccessory {

    /** Root レベル H/F に使用 */
    public data class Root(public val accessory: RootAccessory) : SettingsAccessory

    /** Section レベル H/F に使用 */
    public data class Section(public val accessory: SectionAccessory) : SettingsAccessory
}
