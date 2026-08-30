package jp.kamusoft.kssettingsview.core

/**
 * Section のヘッダ／フッタ位置に配置可能な内容を表す sum type。
 *
 * - [Text] は文字列ヘッダ／フッタの簡潔表現。
 * - [View] は任意 View ヘッダ／フッタ（[KsAnyView] ラップ）。
 *
 * ヘッダ／フッタは表示専用の装飾領域であり、Cell（タップ・選択・編集する行）の概念は
 * 持ち込まない。任意の見た目が要る場合は [Text] ではなく [View] に [KsAnyView] を包んで渡す。
 */
sealed interface SectionAccessory {

    /**
     * 文字列ヘッダ／フッタ。
     * Android 側では `RecyclerView` のヘッダ／フッタ ViewHolder に直接表示できる最短経路。
     *
     * `data class` の自動 `equals` / `hashCode` により、文字列内容の等価性で判定される。
     */
    data class Text(val value: String) : SectionAccessory

    /**
     * 任意 View ヘッダ／フッタ（[KsAnyView] ラップ）。
     *
     * [view] の中身（`KsAnyView`）は等価性判定対象から除外する。
     * `equals` / `hashCode` は手動実装で「クラス一致のみで等価」とする。
     *
     * @property view 描画する任意 View（`KsAnyView`）
     */
    class View(val view: KsAnyView) : SectionAccessory {

        /** クラス一致のみで等価判定（[view] の中身は無視）。 */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is View
        }

        /** クラス識別子のみを hash に混ぜる（[view] の中身は hash 計算対象外）。 */
        override fun hashCode(): Int {
            return View::class.hashCode()
        }

        override fun toString(): String = "SectionAccessory.View(view=$view)"
    }
}
