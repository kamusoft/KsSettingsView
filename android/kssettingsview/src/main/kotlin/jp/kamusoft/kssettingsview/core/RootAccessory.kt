package jp.kamusoft.kssettingsview.core

/**
 * `SettingsRoot` のヘッダ／フッタ位置に配置可能な内容を表す sum type。
 *
 * - [Text]: 文字列ヘッダ／フッタ（簡潔表現）
 * - [View]: 任意 View ヘッダ／フッタ（[KsAnyView] ラップ）
 *
 * Root と Section で装飾の責務が異なるため（core/ADR-0005）、[SectionAccessory] とは別型とし、
 * Root 固有の挙動分岐（ピン留め・テーマ継承ルール等）を独立に持てるようにする。
 */
public sealed interface RootAccessory {

    /**
     * 文字列ヘッダ／フッタ。
     *
     * `data class` の自動 `equals` / `hashCode` により、文字列内容の等価性で判定される。
     */
    public data class Text(public val value: String) : RootAccessory

    /**
     * 任意 View ヘッダ／フッタ（[KsAnyView] ラップ）。
     *
     * [view] の中身（`KsAnyView`）は等価性判定対象から除外する。
     * `equals` / `hashCode` は手動実装で「クラス一致のみで等価」とする。
     * これは差分検出（`SettingsRoot` の `equals`）が `KsAnyView` の中身に依存しないようにするため。
     *
     * @property view 描画する任意 View（`KsAnyView`）
     */
    public class View(public val view: KsAnyView) : RootAccessory {

        /** クラス一致のみで等価判定（[view] の中身は無視）。 */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            // クラス一致のみで等価とみなす（KsAnyView の中身は判定対象外）
            return other is View
        }

        /** クラス識別子のみを hash に混ぜる（[view] の中身は hash 計算対象外）。 */
        override fun hashCode(): Int {
            return View::class.hashCode()
        }

        override fun toString(): String = "RootAccessory.View(view=$view)"
    }
}
