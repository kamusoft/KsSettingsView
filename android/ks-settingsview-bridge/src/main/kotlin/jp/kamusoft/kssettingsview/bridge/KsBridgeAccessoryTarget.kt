package jp.kamusoft.kssettingsview.bridge

/**
 * Accessory（Root / Section の header・footer）の更新対象。
 *
 * [SectionHeader] / [SectionFooter] を指定するときは、あわせて対象 Section の sectionID を渡す。
 * [RootHeader] / [RootFooter] では sectionID は参照されない。
 */
enum class KsBridgeAccessoryTarget {
    /** Root レベルのヘッダ */
    RootHeader,

    /** Root レベルのフッタ */
    RootFooter,

    /** 指定 Section のヘッダ */
    SectionHeader,

    /** 指定 Section のフッタ */
    SectionFooter,
}
