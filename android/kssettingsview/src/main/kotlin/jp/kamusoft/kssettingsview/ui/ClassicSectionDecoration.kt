package jp.kamusoft.kssettingsview.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

/**
 * `AiForms.Maui.SettingsView` と同じフラットな区切り線描画。
 *
 * 各 Cell 行の下端に Theme.separatorColor で 1dp 相当の区切り線を引き、
 * `Theme.sectionMargin` の上下成分を Section 単位の外側余白として確保する。
 * 角丸背景・ボーダーの描画は行わない（それらは [ModernSectionDecoration] の担当）。
 *
 * @param theme 線色・Section 余白を取得するための Theme（実行中に変更される可能性があるため `var`）
 */
internal class ClassicSectionDecoration(
    var theme: Theme,
) : RecyclerView.ItemDecoration() {

    private val paint: Paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    /**
     * Cell の上下に区切り線を描画する。
     *
     * `onDraw` ではなく `onDrawOver` を使う理由:
     * - `LabelCellViewHolder` 等の Cell ViewHolder は `container.setBackgroundColor(white)` で
     *   Cell 全面を不透明に塗る。`onDraw` は children 描画より先に呼ばれるため、その後に塗られる
     *   Cell 背景に区切り線が上書きされて見えなくなる。
     * - `onDrawOver` は children 描画後に呼ばれるため、Cell 背景の上に区切り線が確実に重なる。
     *
     * 線の太さ: AiForms.Maui.SettingsView の `Platforms/Android/Resources/drawable/divider.xml`
     * (`<size android:height="1px" />`) に揃え、**1 ピクセル固定 hairline** とする
     * （dp 換算は行わない）。
     *
     * 描画位置: 各 Cell の下端 1px。さらに **セクション最初の Cell** に対しては上端 1px にも
     * 罫線を描画し、AiForms オリジナル `SVItemdecoration.cs` の
     * `ShowSectionTopBottomBorder = true` 相当の見た目（セクション境界の上下に罫線）を実現する。
     * セクション最初か否かは、直前 CellRow の `sectionId` と比較して判定する。
     *
     * 罫線の左インセット（iOS と揃える）:
     * - **セクション境界**（セクション最初 Cell の上端 / セクション最後 Cell の下端）→ インセット 0
     *   （`paddingLeft` から `width - paddingRight` まで、端から端で描画）
     * - **セクション内中間 Cell の下端** → 左インセット 16dp 相当
     *   （`paddingLeft + 16 * density` から `width - paddingRight` まで描画）
     *
     * これにより iOS の `bottomSeparatorInsets.leading = 16pt`（中間 Cell）/ 0pt（境界 Cell）
     * と視覚的に揃う。AiForms オリジナル Android 側もセクション内 Cell 間は左に約 16dp の
     * インセットを持つスクリーンショットが確認されており、本実装はこれに準拠する。
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        // AiForms オリジナル divider.xml の `<size android:height="1px" />` 相当。
        // dp 換算は行わず、1 物理ピクセル固定（hairline）。
        val separatorThicknessPx = 1.0f
        val separatorColor = theme.separatorColor.toArgb()

        val concatAdapter = parent.adapter as? ConcatAdapter ?: return

        // 16dp 相当の px 値（iOS の 16pt インセットと整合させる）。
        val density = parent.resources.displayMetrics.density
        val midSeparatorInsetPx = 16f * density

        for (i in 0 until parent.childCount) {
            val child: View = parent.getChildAt(i)
            val holder = parent.getChildViewHolder(child) ?: continue

            // mainListAdapter（KsSettingsListAdapter）配下の項目に限定して区切り線を描く。
            // Root H/F（RootHeaderFooterAdapter）は対象外。
            val mainAdapterIndex = findMainAdapterIndex(concatAdapter)
            if (mainAdapterIndex < 0) continue

            // bindingAdapterPosition は ConcatAdapter ではなく内部 sub-adapter の位置を返す。
            // 内部 adapter が KsSettingsListAdapter かどうかを判定するために、Holder の bindingAdapter で確認する。
            val bindingAdapter = holder.bindingAdapter
            if (bindingAdapter !is KsSettingsListAdapter) continue

            val pos = holder.bindingAdapterPosition
            if (pos < 0) continue
            val item = bindingAdapter.currentList.getOrNull(pos) ?: continue

            // CellListItem.CellRow 直下のみ罫線を描画する（Header / Footer 行は対象外）。
            if (item !is CellListItem.CellRow) continue

            // ItemAnimator のアニメーション中、child は translationY / alpha で動的に補間される。
            // child.bottom は静的なレイアウト座標を返すため、アニメ中の見た目に追従させるには
            // translationY を足す必要がある（これをしないと「実在するが透明な Cell の下端」に
            // 罫線が描かれ、本来の見た目の位置とずれて視覚的なゴーストが出る）。
            // alpha も同様に Cell 本体に合わせ、アニメ中は線も透ける。
            val translationY = child.translationY
            val edgeLeft = parent.paddingLeft.toFloat()
            val right = (parent.width - parent.paddingRight).toFloat()
            val bottom = child.bottom.toFloat() + translationY
            val top = child.top.toFloat() + translationY

            paint.color = separatorColor
            val alphaFloat = (child.alpha * 255f).coerceIn(0f, 255f)
            paint.alpha = alphaFloat.toInt()

            // 直前 / 直後 item を見てセクション境界かを判定する。
            // 直前 item が同一 sectionId の CellRow でなければ「セクション最初」とみなす
            // （Header 行・別 Section の Cell・先頭 = pos == 0 のいずれも該当）。
            // 直後 item が同一 sectionId の CellRow でなければ「セクション最後」とみなす。
            val prevItem = bindingAdapter.currentList.getOrNull(pos - 1)
            val nextItem = bindingAdapter.currentList.getOrNull(pos + 1)
            val isSectionTop = prevItem !is CellListItem.CellRow ||
                prevItem.sectionId != item.sectionId
            val isSectionBottom = nextItem !is CellListItem.CellRow ||
                nextItem.sectionId != item.sectionId

            // 下端罫線:
            // - セクション最後 Cell → インセット 0（端から端）で描画
            // - セクション内中間 Cell → 左インセット 16dp で描画（iOS の 16pt と揃える）
            val bottomLeft = bottomSeparatorLeftFor(
                isSectionBottom = isSectionBottom,
                edgeLeft = edgeLeft,
                midSeparatorInsetPx = midSeparatorInsetPx,
            )
            c.drawRect(bottomLeft, bottom - separatorThicknessPx, right, bottom, paint)

            // 上端罫線: セクション最初の Cell の上端にも 1px hairline。
            // インセット 0（端から端）で描画（iOS の topSeparatorInsets.leading = 0 と一致）。
            if (isSectionTop) {
                c.drawRect(edgeLeft, top, right, top + separatorThicknessPx, paint)
            }
        }
    }

    /**
     * Section 単位の上下外側マージンだけをオフセットとして確保する。
     *
     * 区切り線の分のオフセットは入れない（既存 Cell の下端に重ねる方式）。
     * `Theme.sectionMargin` の水平成分は Classic では無視する（Section 境界を全幅に保つため）。
     * 未指定時の実効値は上下 0 で、従来の Classic の表示と一致する。
     */
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val metrics = SectionBoxMetrics.resolve(
            theme = theme,
            style = KsSettingsViewStyle.Classic,
            density = parent.resources.displayMetrics.density,
            layoutDirection = if (parent.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            },
        )
        val (top, bottom) = SectionUnitMargin.verticalOffsets(
            view = view,
            parent = parent,
            marginTopPx = metrics.marginTopPx.roundToInt(),
            marginBottomPx = metrics.marginBottomPx.roundToInt(),
        )
        outRect.set(0, top, 0, bottom)
    }

    /**
     * `ConcatAdapter` 内で `KsSettingsListAdapter` の sub-adapter index を探す。
     *
     * 現状は単に存在判定のために使うので、index を返さなくてもよいが、将来用途を考慮して残す。
     */
    private fun findMainAdapterIndex(concat: ConcatAdapter): Int {
        return concat.adapters.indexOfFirst { it is KsSettingsListAdapter }
    }

    companion object {
        /**
         * 下端罫線の左座標を、当該 Cell がセクション末尾か中間かによって切り替える純粋関数。
         *
         * 罫線インセット規則の中核ロジック。`onDrawOver` から呼び出されるとともに、
         * ユニットテストで規則を直接検証するためにも参照される。
         *
         * - セクション最後 Cell → `edgeLeft`（端から端、インセット 0）
         * - セクション内中間 Cell → `edgeLeft + midSeparatorInsetPx`（左インセット 16dp 相当）
         *
         * @param isSectionBottom 当該 Cell がセクション末尾の場合 true
         * @param edgeLeft `parent.paddingLeft.toFloat()` 相当の Cell 領域左端
         * @param midSeparatorInsetPx 中間 Cell に適用する左インセット（通常 `16f * density`）
         */
        internal fun bottomSeparatorLeftFor(
            isSectionBottom: Boolean,
            edgeLeft: Float,
            midSeparatorInsetPx: Float,
        ): Float = if (isSectionBottom) edgeLeft else edgeLeft + midSeparatorInsetPx
    }
}
