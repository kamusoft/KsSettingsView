package jp.kamusoft.kssettingsview.bridge

import android.view.View
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.CustomCell

/**
 * 任意の View を行の内容として表示する Cell（`CustomCell`）を輸送する DTO。
 *
 * 内容は [view]（platform View の実体）と [contentToken]（その実体の世代）の対で運ぶ。Native の
 * content には token だけを格納するため、Native から見た内容の等価性は token の値等価で決まる
 * （maui/ADR-0020）。token が同じ間は、他のプロパティ変更で再バインドが起きても埋め込まれた View は
 * 同一インスタンスのまま維持される。
 *
 * [view] が `null` の DTO は内容なしの行として描画される。
 *
 * 共通行レイアウトのスロット（タイトル・説明文・値・ヒント・アイコン）を持たない Cell のため、
 * 基底の `title` / `descriptionText` / `valueText` / `hintText` / `icon` は Native へ写されない。
 *
 * タップは [hasTapHandler] が `true` のときだけ [KsBridgeInteractionListener.customCellTapped] で
 * 通知される。`false` の行はタップ動作を持たず、内容の中の操作を妨げない。
 */
class KsBridgeCustomCell @JvmOverloads constructor(
    title: String,
    descriptionText: String? = null,
    valueText: String? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
) : KsBridgeCell(
    title = title,
    descriptionText = descriptionText,
    valueText = valueText,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
) {

    /** 行の内容として表示する View（未指定は `null` で内容なし） */
    var view: View? = null

    /** 内容として埋め込む View の世代。実体が入れ替わるたびに変わる値を上位層が振る。 */
    var contentToken: String = ""

    /** Disclosure Indicator を表示するフラグ */
    var showArrowIndicator: Boolean = false

    /** 行タップを通知するフラグ */
    var hasTapHandler: Boolean = false

    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell {
        val content = view
        // 購読なしの行は onTap を持たせない（内容の中の操作を妨げないため）。
        val onTap: (() -> Unit)? = if (hasTapHandler) {
            { relay.customCellTapped(id) }
        } else {
            null
        }

        return CustomCell(
            id = id,
            style = resolvedStyle,
            content = contentToken,
            showArrow = showArrowIndicator,
            onTap = onTap,
            isEnabled = isEnabled,
            isVisible = isVisible,
            builder = { token ->
                // 行タップは内容の上でも発火させる（内容の中の操作に消費されたときは発火しない）。
                KsBridgeCellContentView.Content(view = content, token = token, onRowTap = onTap)
            },
        )
    }
}
