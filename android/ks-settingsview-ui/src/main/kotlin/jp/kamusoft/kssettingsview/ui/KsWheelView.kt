package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * [KsWheelView] の描画に必要な解決済みスタイル値。
 *
 * ホイールは選択面（シート）の一部品だが、シート固有の値（区切り線・Ripple など）には依存しない。
 * 将来 DatePicker のホイール版（3連ホイール）へ再利用するため、必要最小限の値だけを受け取る
 * （android/ADR-0007）。
 *
 * @property accentColor 選択中候補の強調色（中央ハイライト帯と選択中行の文字色）
 * @property surfaceColor ホイールが載る面の色（上下端のフェード先の色）
 * @property itemTextColor 候補行の文字色（選択中以外）
 * @property itemTypeface 候補行の Typeface
 * @property itemTextSizeSp 候補行の基準文字サイズ（sp）。中央・周辺の増減はここからの倍率で導く
 */
internal data class KsWheelStyle(
    @ColorInt val accentColor: Int,
    @ColorInt val surfaceColor: Int,
    @ColorInt val itemTextColor: Int,
    val itemTypeface: Typeface,
    val itemTextSizeSp: Float,
) {
    companion object {
        /** 選択面のスタイル [PickerSheetStyle] からホイール用のスタイルを取り出す。 */
        fun from(sheetStyle: PickerSheetStyle): KsWheelStyle = KsWheelStyle(
            accentColor = sheetStyle.accentColor,
            surfaceColor = sheetStyle.sheetBackgroundColor,
            itemTextColor = sheetStyle.itemTextColor,
            itemTypeface = sheetStyle.itemTypeface,
            itemTextSizeSp = sheetStyle.itemTextSizeSp,
        )
    }
}

/**
 * スナップ式のホイール選択部品（`RecyclerView` + `LinearSnapHelper`）。
 *
 * 可視行数は固定（[VISIBLE_ROW_COUNT] 行）で、中央の1行が選択位置。中央には accent 淡色の
 * 丸角帯を敷き、選択中の行は accent 色・太字で表示する。中央から離れた行は距離に応じて
 * フェードと縮小で減衰し、上下端は面の色へのグラデーションでフェードアウトする。
 *
 * **選択中候補の更新はスナップ静止時にのみ行う。** ドラッグ・慣性移動の途中では直前に静止した
 * 候補を [selectedIndex] として保持するため、移動中に確定操作を行っても採用されるのは
 * 「直前に静止した候補」になる。
 *
 * ホイールのスクロールは親（ボトムシート）へ伝播しない（[SelfContainedRecyclerView]）。
 * これにより候補領域の下方向操作は候補の遷移になり、シートの dismiss を引き起こさない。
 *
 * アクセシビリティ上はホイール全体で1つのコントロール（`NumberPicker` 相当）として振る舞う。
 * 選択中候補の表示文字列を [android.view.View.setContentDescription] として公開し、
 * 前候補 / 次候補への変更を `ACTION_SCROLL_BACKWARD` / `ACTION_SCROLL_FORWARD` で提供する。
 *
 * 汎用部品として候補の件数と「index → 表示文字列」の関数だけを受け取り、値の型やフォーマット規則は
 * 関知しない（android/ADR-0007: 将来 DatePicker ホイール版へ展開する前提）。候補列を実体化せず
 * index 単位で解決するため、`Int` の表現上限に近い件数でも表示中の行の分しか文字列を作らない。
 *
 * 候補そのものは表示中に差し替えられる（[setCandidates]）。DatePicker の3連ホイール
 * （android/ADR-0009）では、年・月の選択に応じて月・日の候補が変わるため、ホイールを作り直さずに
 * 件数と表示文字列だけを入れ替える。
 *
 * @param itemCount 候補の件数
 * @param displayTextAt index に対応する表示文字列を返す関数（`0 until itemCount` の範囲で呼ばれる）
 * @param initialIndex 初期の選択中 index（範囲外は最も近い有効な index へ丸める）
 * @param wheelStyle 解決済みのスタイル値
 * @param seriesLabel 系列の意味を表す名前（年 / 月 / 日 など）。指定時はアクセシビリティへ選択中の
 *   表示文字列と併せて公開する。単独のホイール（[NumberSelectionSheet]）では `null`
 * @param showsBand 中央の選択位置を示す帯を自分で敷くか。3連ホイールでは列を横断する1本の帯を
 *   親側で敷くため `false` にする
 */
internal class KsWheelView(
    context: Context,
    private var itemCount: Int,
    private var displayTextAt: (Int) -> String,
    initialIndex: Int,
    private val wheelStyle: KsWheelStyle,
    private val seriesLabel: String? = null,
    showsBand: Boolean = true,
) : FrameLayout(context) {

    /** 有効な候補 index の範囲。 */
    private val itemIndices: IntRange get() = 0 until itemCount

    /** 1候補あたりの行高（px）。 */
    private val rowHeightPx: Int = context.sheetDp(ROW_HEIGHT_DP)

    /** ホイール全体の高さ（px）。可視行数分で固定する。 */
    private val wheelHeightPx: Int = rowHeightPx * VISIBLE_ROW_COUNT

    /** 選択中行に使う太字 Typeface。 */
    private val selectedTypeface: Typeface = Typeface.create(wheelStyle.itemTypeface, Typeface.BOLD)

    private val wheelLayoutManager = WheelLayoutManager(context)

    private val snapHelper = LinearSnapHelper()

    /** 候補の並び。ホイールの可動部。 */
    internal val listView: RecyclerView = SelfContainedRecyclerView(context)

    /** 中央の選択位置を示す帯。 */
    internal val bandView: View = View(context)

    /**
     * 現在選択中の候補 index。
     *
     * 候補の並びが候補位置へスナップして静止した時点でのみ更新される。候補が空のときは `-1`。
     */
    internal var selectedIndex: Int =
        if (itemCount <= 0) NO_SELECTION else initialIndex.coerceIn(0, itemCount - 1)
        private set

    /**
     * 選択中候補が変わったときに呼ばれる通知。
     *
     * 候補位置へスナップして静止した時点でのみ呼ばれる（通常スクロール・アクセシビリティ操作・
     * [setSelectedIndex] によるプログラム的な移動の全経路）。[setCandidates] による候補差し替えと
     * それに伴う選択の丸めでは呼ばれない（差し替えを指示した側が結果を知っているため、
     * 通知して再入させない）。
     */
    internal var onSelectionChanged: ((Int) -> Unit)? = null

    /**
     * 進行中のスクロールを打ち切っている最中かどうか。
     *
     * 打ち切りは静止として通知されるが、その位置は目的地ではない中間位置でしかないため、
     * 選択の確定（[commitSnappedSelection]）を抑止する。
     */
    private var isStoppingScroll: Boolean = false

    init {
        if (showsBand) buildBand()
        buildList()
        buildFade(Gravity.TOP)
        buildFade(Gravity.BOTTOM)

        // ホイール全体を1つのコントロールとしてアクセシビリティサービスへ公開する。
        listView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = accessibilityText()
    }

    /** 可視行数分の高さで固定する（親の指定によらずホイールの見た目の行数を保つ）。 */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(wheelHeightPx, MeasureSpec.EXACTLY),
        )
    }

    // MARK: - View 構築

    private fun buildBand() {
        val inset = context.sheetDp(BAND_INSET_HORIZONTAL_DP)
        bandView.apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                rowHeightPx,
                Gravity.CENTER_VERTICAL,
            ).apply {
                marginStart = inset
                marginEnd = inset
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.sheetDp(BAND_CORNER_RADIUS_DP).toFloat()
                setColor(ColorUtils.setAlphaComponent(wheelStyle.accentColor, BAND_ALPHA))
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(bandView)
    }

    private fun buildList() {
        listView.apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            layoutManager = wheelLayoutManager
            adapter = ItemsAdapter()
            itemAnimator = null
            overScrollMode = View.OVER_SCROLL_NEVER
            // 先頭・末尾の候補も中央（選択位置）へ来られるよう、上下に (可視行数 - 1) / 2 行分の余白を置く。
            clipToPadding = false
            setPadding(0, rowHeightPx * SIDE_ROW_COUNT, 0, rowHeightPx * SIDE_ROW_COUNT)
            addOnScrollListener(WheelScrollListener())
        }
        snapHelper.attachToRecyclerView(listView)
        if (selectedIndex >= 0) {
            // 選択中候補を中央（上余白の直後）へ置く。
            wheelLayoutManager.scrollToPositionWithOffset(selectedIndex, 0)
        }
        addView(listView)
    }

    /**
     * ホイール上端 / 下端のフェードを構築する。
     *
     * 面の色から透明へのグラデーションを重ね、候補が端で消えていくように見せる。
     * タップを消費しない装飾要素のため、クリック不可のまま重ねる（下のホイールへ透過する）。
     */
    private fun buildFade(verticalGravity: Int) {
        val orientation = if (verticalGravity == Gravity.TOP) {
            GradientDrawable.Orientation.TOP_BOTTOM
        } else {
            GradientDrawable.Orientation.BOTTOM_TOP
        }
        val fade = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                context.sheetDp(FADE_HEIGHT_DP),
                verticalGravity,
            )
            background = GradientDrawable(
                orientation,
                intArrayOf(
                    wheelStyle.surfaceColor,
                    ColorUtils.setAlphaComponent(wheelStyle.surfaceColor, 0),
                ),
            )
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(fade)
    }

    private fun createRow(): TextView = TextView(context).apply {
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeightPx)
        gravity = Gravity.CENTER
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        textSize = wheelStyle.itemTextSizeSp
        typeface = wheelStyle.itemTypeface
        setTextColor(wheelStyle.itemTextColor)
        // 選択状態と表示値はホイール全体で1ノードとして公開するため、行は読み上げ対象から外す。
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    // MARK: - 選択の状態遷移

    /** 選択中候補の表示文字列。候補が空のときは `null`。 */
    internal fun selectedDisplayText(): String? =
        if (selectedIndex in itemIndices) displayTextAt(selectedIndex) else null

    /**
     * アクセシビリティサービスへ公開する文字列。
     *
     * [seriesLabel] が与えられている場合は「系列の名前 + 選択中候補の表示文字列」を組にして公開し、
     * 3連ホイールのどの系列かを識別できるようにする。単独のホイールでは選択中候補の表示文字列
     * だけを公開する。
     */
    private fun accessibilityText(): String? {
        val value = selectedDisplayText()
        val label = seriesLabel
        return when {
            label.isNullOrEmpty() -> value
            value == null -> label
            else -> "$label, $value"
        }
    }

    /**
     * 候補を差し替える。
     *
     * 件数と「index → 表示文字列」を入れ替え、[selectedIndex] を [selectedIndex] へ移す。
     * 選択変更の通知（[onSelectionChanged]）は行わない — 差し替えを指示した側が新しい選択を
     * 知っているため、通知して再入させない。
     *
     * @param itemCount 新しい候補の件数
     * @param displayTextAt 新しい「index → 表示文字列」
     * @param selectedIndex 差し替え後の選択中 index（範囲外は最も近い有効な index へ丸める）
     */
    internal fun setCandidates(itemCount: Int, displayTextAt: (Int) -> String, selectedIndex: Int) {
        // 差し替え前の候補に向かって進んでいた移動は、新しい候補には無意味なため打ち切る。
        stopScroll()
        this.itemCount = itemCount
        this.displayTextAt = displayTextAt
        val index = if (itemCount <= 0) NO_SELECTION else selectedIndex.coerceIn(0, itemCount - 1)
        // 候補列そのものが入れ替わるため、差分計算の余地がない全件更新として通知する。
        @Suppress("NotifyDataSetChanged")
        listView.adapter?.notifyDataSetChanged()
        if (index != NO_SELECTION) {
            wheelLayoutManager.scrollToPositionWithOffset(index, 0)
        }
        updateSelection(index, notify = false)
    }

    /**
     * 選択中候補を [index] へプログラム的に移す（候補位置へ直接スクロールする）。
     *
     * 「今日」へのジャンプのように、外部の操作で選択を移すための経路。移動によって選択中候補が
     * 変われば [onSelectionChanged] を発火する。
     */
    internal fun setSelectedIndex(index: Int) {
        if (index !in itemIndices) return
        stopScroll()
        wheelLayoutManager.scrollToPositionWithOffset(index, 0)
        updateSelection(index, notify = true)
    }

    /**
     * 進行中のスクロール（慣性移動と、そこから続くスナップの補正スクロール）を打ち切る。
     *
     * `scrollToPositionWithOffset` は次のレイアウトでの位置を予約するだけで、駆動中の移動は
     * 止まらない。止めずに位置を移すと移動がそのまま続き、着地した候補が静止時に選択として
     * 確定されて、プログラム的に移した選択を上書きしてしまう。
     *
     * 打ち切りは静止として通知され、その通知を受けた [LinearSnapHelper] が近傍の候補位置への
     * 補正スクロールを始めるため、始まっていればそれも続けて打ち切る。打ち切りの過程で通る
     * 静止は目的地ではない中間位置でしかないため、その間は選択を確定しない。
     */
    private fun stopScroll() {
        isStoppingScroll = true
        try {
            listView.stopScroll()
            if (listView.scrollState != RecyclerView.SCROLL_STATE_IDLE) {
                listView.stopScroll()
            }
        } finally {
            isStoppingScroll = false
        }
    }

    /**
     * スナップ静止した候補を選択中として確定する。
     *
     * 候補の並びが静止した時点（`SCROLL_STATE_IDLE`）に呼ばれるが、`SCROLL_STATE_IDLE` は
     * 「行間で指を離した」時点でも通知される（そこから `LinearSnapHelper` の補正スクロールが
     * 始まる）。そのため中央からの残距離が 0 — つまり候補位置へ実際に整列している — ときだけ
     * 選択を確定し、補正スクロールが完了した後の静止で更新されるようにする。
     *
     * これにより、移動中に確定操作を行っても採用されるのは直前に静止した候補になる。
     *
     * プログラム的な移動のために進行中のスクロールを打ち切っている最中（[isStoppingScroll]）も、
     * 通る位置は目的地ではないため確定しない。
     */
    private fun commitSnappedSelection() {
        if (isStoppingScroll) return
        val snapView = snapHelper.findSnapView(wheelLayoutManager) ?: return
        val distance = snapHelper.calculateDistanceToFinalSnap(wheelLayoutManager, snapView) ?: return
        if (distance[0] != 0 || distance[1] != 0) return
        val position = wheelLayoutManager.getPosition(snapView)
        if (position != RecyclerView.NO_POSITION) {
            updateSelection(position, notify = true)
        }
    }

    /**
     * 選択中候補を [index] へ更新し、強調表示と公開状態を追随させる。
     *
     * 公開状態（アクセシビリティ）の変化は表示文字列で判定する。候補の差し替えでは index が
     * 変わらないまま表示文字列だけが変わりうるため、index の比較では取りこぼす。
     * 選択変更の通知（[onSelectionChanged]）は index が実際に変わったときにのみ、
     * [notify] が `true` の場合に行う。
     */
    private fun updateSelection(index: Int, notify: Boolean) {
        val previousText = contentDescription?.toString()
        val indexChanged = index != selectedIndex
        selectedIndex = index
        contentDescription = accessibilityText()
        if (contentDescription?.toString() != previousText) {
            // 候補の並びは読み上げ対象から外してあり、`RecyclerView` のスクロール通知も届かない。
            // ホイール全体で1つのコントロールとして振る舞う以上、選択の変化は自分で通知する。
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
        }
        applyRowAppearance()
        if (indexChanged && notify) {
            onSelectionChanged?.invoke(index)
        }
    }

    /**
     * 中央からの距離に応じて各行の減衰（フェード + 縮小）と強調を適用する。
     *
     * 強調（accent 色・太字）は [selectedIndex] の行にのみ与える。減衰は行の実位置から連続的に
     * 求めるため、スクロール中も滑らかに変化する。
     */
    private fun applyRowAppearance() {
        val viewportCenter = listView.height / 2f
        if (viewportCenter <= 0f) return
        for (i in 0 until listView.childCount) {
            val child = listView.getChildAt(i)
            val childCenter = (child.top + child.bottom) / 2f + child.translationY
            val distanceInRows = abs(childCenter - viewportCenter) / rowHeightPx
            child.alpha = interpolate(ALPHA_BY_ROW_DISTANCE, distanceInRows)
            val scale = interpolate(SCALE_BY_ROW_DISTANCE, distanceInRows)
            child.scaleX = scale
            child.scaleY = scale
            val row = child as? TextView ?: continue
            val isSelected = listView.getChildAdapterPosition(child) == selectedIndex
            row.setTextColor(if (isSelected) wheelStyle.accentColor else wheelStyle.itemTextColor)
            row.typeface = if (isSelected) selectedTypeface else wheelStyle.itemTypeface
        }
    }

    /**
     * 距離 [distanceInRows]（行数単位）における [values] の値を線形補間で求める。
     *
     * [values] は index を距離（0行 / 1行 / 2行 …）とみなした系列で、末尾より遠い距離では
     * 末尾の値を使う。
     */
    private fun interpolate(values: FloatArray, distanceInRows: Float): Float {
        val clamped = distanceInRows.coerceIn(0f, (values.size - 1).toFloat())
        val lower = floor(clamped).toInt()
        val upper = ceil(clamped).toInt()
        if (lower == upper) return values[lower]
        return values[lower] + (values[upper] - values[lower]) * (clamped - lower)
    }

    // MARK: - アクセシビリティ

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        // スピナー相当のコントロールとして、選択中候補の表示文字列とともに公開する。
        info.className = NumberPicker::class.java.name
        info.contentDescription = accessibilityText()
        if (selectedIndex + 1 in itemIndices) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }
        if (selectedIndex - 1 in itemIndices) {
            info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean = when (action) {
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> selectAdjacent(selectedIndex + 1)
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> selectAdjacent(selectedIndex - 1)
        else -> super.performAccessibilityAction(action, arguments)
    }

    /**
     * アクセシビリティ操作で選択中候補を [index] へ移す。
     *
     * 端の候補でその方向へ移動できない場合は何もせず `false` を返す（選択中の候補は変わらない）。
     */
    private fun selectAdjacent(index: Int): Boolean {
        if (index !in itemIndices) return false
        stopScroll()
        wheelLayoutManager.scrollToPositionWithOffset(index, 0)
        updateSelection(index, notify = true)
        return true
    }

    // MARK: - 検証用フック

    /**
     * 候補行を [RecyclerView] のレイアウトを経由せずに生成して bind する。
     *
     * 候補の表示文字列を、ホイールの実表示を伴わずに検証するための経路。返る View は本番と同じ
     * bind 経路を通っている。
     */
    internal fun bindRow(index: Int): TextView {
        val row = createRow()
        bindRow(row, index)
        return row
    }

    /** レイアウト済みのホイールから [index] の行 View を取り出す（未レイアウトなら `null`）。 */
    internal fun rowViewAt(index: Int): TextView? =
        listView.findViewHolderForAdapterPosition(index)?.itemView as? TextView

    // MARK: - Adapter / LayoutManager / ScrollListener

    private fun bindRow(row: TextView, position: Int) {
        row.text = displayTextAt(position)
    }

    private inner class ItemViewHolder(val row: TextView) : RecyclerView.ViewHolder(row)

    private inner class ItemsAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        // Adapter 自身の `itemCount` と同名のため、ホイール側の件数であることを明示して参照する。
        override fun getItemCount(): Int = this@KsWheelView.itemCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
            ItemViewHolder(createRow())

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            bindRow(holder.row, position)
        }
    }

    /** レイアウト完了のたびに行の減衰・強調を適用し直す LayoutManager。 */
    private inner class WheelLayoutManager(context: Context) :
        LinearLayoutManager(context, VERTICAL, false) {
        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            applyRowAppearance()
        }
    }

    /** スクロール中は減衰だけを更新し、静止した時点で選択中候補を確定する。 */
    private inner class WheelScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            applyRowAppearance()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                commitSnappedSelection()
            }
        }
    }

    companion object {
        /** 候補が無く選択中候補を持たないことを示す index。 */
        const val NO_SELECTION: Int = -1

        /** 1候補あたりの行高（dp）。 */
        const val ROW_HEIGHT_DP: Float = 44f

        /** 可視行数（奇数。中央が選択位置）。 */
        const val VISIBLE_ROW_COUNT: Int = 5

        /** 中央行の上下に置く行数。 */
        private const val SIDE_ROW_COUNT: Int = VISIBLE_ROW_COUNT / 2

        /** 中央ハイライト帯の左右インセット（dp）。 */
        private const val BAND_INSET_HORIZONTAL_DP: Float = 12f

        /** 中央ハイライト帯の角丸半径（dp）。 */
        const val BAND_CORNER_RADIUS_DP: Float = 12f

        /** 中央ハイライト帯のアルファ（0-255。強調色を約 14% の淡さで敷く）。 */
        const val BAND_ALPHA: Int = 36

        /** 上下端フェードの高さ（dp）。 */
        private const val FADE_HEIGHT_DP: Float = 60f

        /**
         * 中央からの距離（行数）ごとの不透明度。選択位置から離れるほど淡くする。
         */
        private val ALPHA_BY_ROW_DISTANCE: FloatArray = floatArrayOf(1f, 0.5f, 0.25f)

        /**
         * 中央からの距離（行数）ごとの文字倍率。
         *
         * [KsWheelStyle.itemTextSizeSp] に対する倍率で、中央行だけをやや拡大し、離れるほど縮小する。
         */
        private val SCALE_BY_ROW_DISTANCE: FloatArray = floatArrayOf(1.12f, 0.94f, 0.88f)
    }
}
