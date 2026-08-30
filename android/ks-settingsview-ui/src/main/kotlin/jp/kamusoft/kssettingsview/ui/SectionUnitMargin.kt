package jp.kamusoft.kssettingsview.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Section 単位（Section Header・Cell の箱・Section Footer を一体とした表示単位）の
 * 上下外側余白を、`ItemDecoration.getItemOffsets` の offset として求めるヘルパ。
 *
 * `ClassicSectionDecoration` と `ModernSectionDecoration` が同じ余白規則を共有する。
 */
internal object SectionUnitMargin {

    /**
     * 対象 View に与える上下 offset を求める。
     *
     * 余白の置き方は次のとおり。
     *
     * - 隣接 Section の間隔は「前 Section の下余白 + 次 Section の上余白」の加算になる
     *   （Section の先頭行に上余白、末尾行に下余白を入れるため）。
     * - 先頭 Section の上・末尾 Section の下にも同じ余白を取る。Root Header / Footer がある場合、
     *   この余白は Root Header と先頭 Section の間 / 末尾 Section と Root Footer の間、つまり
     *   Root Header / Footer の内側に入る。Root Header / Footer 自体は余白の対象外。
     * - Root Header / Footer が無い場合、先頭 Section の上・末尾 Section の下がそのまま
     *   list の端に接するため、余白は list 端の余白として現れる。
     *
     * Section の境界判定には平坦リスト（[CellListItem]）の前後項目の `sectionId` を使う。
     *
     * @param view offset を求める対象の child View
     * @param parent 対象の RecyclerView
     * @param marginTopPx Section 単位の上余白（px）
     * @param marginBottomPx Section 単位の下余白（px）
     * @return (上 offset, 下 offset) の組
     */
    fun verticalOffsets(
        view: View,
        parent: RecyclerView,
        marginTopPx: Int,
        marginBottomPx: Int,
    ): Pair<Int, Int> {
        if (marginTopPx <= 0 && marginBottomPx <= 0) return 0 to 0

        val holder = parent.getChildViewHolder(view) ?: return 0 to 0
        val bindingAdapter = holder.bindingAdapter
        if (bindingAdapter !is KsSettingsListAdapter) return 0 to 0
        val position = holder.bindingAdapterPosition
        if (position < 0) return 0 to 0
        val list = bindingAdapter.currentList
        val item = list.getOrNull(position) ?: return 0 to 0

        val previous = list.getOrNull(position - 1)
        val next = list.getOrNull(position + 1)
        val isSectionFirst = previous == null || previous.sectionId != item.sectionId
        val isSectionLast = next == null || next.sectionId != item.sectionId

        return (if (isSectionFirst) marginTopPx else 0) to (if (isSectionLast) marginBottomPx else 0)
    }
}
