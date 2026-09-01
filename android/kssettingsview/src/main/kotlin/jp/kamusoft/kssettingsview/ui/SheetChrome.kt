package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.TouchDelegate
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/**
 * 選択面（ボトムシート）で共有する寸法定数。
 *
 * [PickerSelectionSheet]（android/ADR-0005）と [NumberSelectionSheet]（android/ADR-0007）は
 * 同じ器・同じヘッダー意匠を持つため、寸法もここに一元化する。
 */
internal object SheetMetrics {
    /** 操作要素の最小タップ領域（dp）。 */
    const val MIN_TOUCH_TARGET_DP: Float = 48f

    /** シート左右の共通パディング（dp）。 */
    const val PADDING_HORIZONTAL_DP: Float = 16f

    const val DRAG_HANDLE_WIDTH_DP: Float = 32f
    const val DRAG_HANDLE_HEIGHT_DP: Float = 4f
    const val DRAG_HANDLE_MARGIN_TOP_DP: Float = 10f
    const val DRAG_HANDLE_MARGIN_BOTTOM_DP: Float = 6f

    /** ドラッグハンドル色のアルファ（0-255）。 */
    const val DRAG_HANDLE_ALPHA: Int = 102

    const val HEADER_ACTION_PADDING_H_DP: Float = 8f
    const val HEADER_ACTION_PADDING_V_DP: Float = 6f

    const val CONFIRM_PADDING_H_DP: Float = 16f
    const val CONFIRM_PADDING_V_DP: Float = 6f
    const val CONFIRM_CORNER_RADIUS_DP: Float = 100f
}

/** dp を現在の画面条件の px へ換算する。 */
internal fun Context.sheetDp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

/**
 * 制約なしで測ったときの [view] の希望幅。
 */
internal fun sheetDesiredWidthOf(view: View): Int {
    val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    view.measure(unspecified, unspecified)
    return view.measuredWidth
}

/**
 * アクセシビリティサービスへ [view] をボタンとして公開する。
 */
internal fun publishAsSheetButton(view: View) {
    view.accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.className = Button::class.java.name
        }
    }
}

/**
 * [slot] の領域全体（横・縦とも）を [target] のタップ領域として委譲する。
 *
 * [target] の見た目の寸法を変えずに当たり判定を最小タップ領域まで広げるために使う。委譲範囲は
 * レイアウト後の実寸に依存するため、レイアウトのたびに設定し直す。[target] が非表示のときは
 * 委譲しない（非表示の操作がスロットのタップで発火しないようにする）。
 */
internal fun delegateSheetTouchToChild(slot: FrameLayout, target: View) {
    slot.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        slot.touchDelegate = if (target.visibility == View.VISIBLE) {
            TouchDelegate(Rect(0, 0, slot.width, slot.height), target)
        } else {
            null
        }
    }
}

/**
 * シート面の色を [color] にする。
 *
 * コンテナの背景 drawable は差し替えずに tint だけを与える。これにより Material が用意する
 * 角丸・elevation の外形・全展開時の角丸補間をそのまま保てる（背景 drawable は
 * `BottomSheetBehavior` がレイアウト時に差し替えるが、tint は後から設定される背景にも適用される）。
 */
internal fun applySheetSurfaceColor(container: View, @ColorInt color: Int) {
    container.backgroundTintList = ColorStateList.valueOf(color)
}

/**
 * シート上端のドラッグハンドルを構築する。
 *
 * 色は Material テーマの `colorOnSurfaceVariant` を薄めて使い、取得できない環境では
 * [fallbackColor] へフォールバックする。装飾要素のためアクセシビリティの読み上げ対象から外す。
 */
internal fun buildSheetDragHandle(context: Context, @ColorInt fallbackColor: Int): View {
    val handleColor = ColorUtils.setAlphaComponent(
        MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            fallbackColor,
        ),
        SheetMetrics.DRAG_HANDLE_ALPHA,
    )
    return View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            context.sheetDp(SheetMetrics.DRAG_HANDLE_WIDTH_DP),
            context.sheetDp(SheetMetrics.DRAG_HANDLE_HEIGHT_DP),
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = context.sheetDp(SheetMetrics.DRAG_HANDLE_MARGIN_TOP_DP)
            bottomMargin = context.sheetDp(SheetMetrics.DRAG_HANDLE_MARGIN_BOTTOM_DP)
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = context.sheetDp(SheetMetrics.DRAG_HANDLE_HEIGHT_DP / 2f).toFloat()
            setColor(handleColor)
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}

/**
 * 選択面のヘッダー（取消 / タイトル / 確定）。
 *
 * 操作ラベル自身の寸法は見た目の基準どおりに保ち、最小タップ領域は親スロット
 * （[cancelSlot] / [confirmSlot]）の大きさと `TouchDelegate` で確保する。ヘッダーの上下 padding を
 * 持たせずスロット高（48dp）をそのままヘッダー高とすることで、当たり判定を満たしつつヘッダーの
 * 総高を抑える。
 *
 * 左右のスロットは操作ラベルの実測幅（と最小タップ領域）を確保した対称幅を持ち、中央のタイトルが
 * 残り幅を占める。幅が足りないときに先に縮むのはタイトル側であり、OS ロケール由来で長さが変わる
 * 操作ラベルは切り詰められない。左右が同幅であることでタイトルはシート幅の中央に置かれ、
 * 確定ラベルを隠してもタイトルの中央位置は変わらない。
 * 対称幅ではヘッダーに収まらない場合（狭い画面・大きな文字サイズ・長いロケール文字列）は、
 * 左右を各ラベルの固有幅へ縮退させ、対称性より操作ラベルの表示を優先する。スロットの最小幅は
 * 操作ラベルを実測して決めるため、文字サイズが Theme 由来で変わっても配分は成立する。
 *
 * 操作ラベルの文字列は OS の公開文字列リソース（`android.R.string.ok` / `android.R.string.cancel`）
 * から解決し、ライブラリ側で文字列リソースを同梱しない。
 *
 * @param style 解決済みのシートスタイル
 * @param title ヘッダー中央に表示するタイトル
 * @param showConfirm 確定ラベルを表示するか（`false` で [confirmView] は `GONE`）
 * @param onCancel 取消ラベルのタップ時に呼ぶ処理
 * @param onConfirm 確定ラベルのタップ時に呼ぶ処理
 */
internal class SheetHeaderView(
    context: Context,
    style: PickerSheetStyle,
    title: String,
    showConfirm: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) : LinearLayout(context) {

    /** ヘッダー左の取消ラベル。 */
    val cancelView: TextView = TextView(context)

    /** ヘッダー中央のタイトル。 */
    val titleView: TextView = TextView(context)

    /** ヘッダー右の確定ラベル。 */
    val confirmView: TextView = TextView(context)

    /** ヘッダー左のスロット（取消ラベルの当たり判定を担う）。 */
    val cancelSlot: FrameLayout = FrameLayout(context)

    /** ヘッダー右のスロット（確定ラベルの当たり判定を担う）。 */
    val confirmSlot: FrameLayout = FrameLayout(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(
            context.sheetDp(SheetMetrics.PADDING_HORIZONTAL_DP),
            0,
            context.sheetDp(SheetMetrics.PADDING_HORIZONTAL_DP),
            0,
        )

        cancelView.apply {
            text = context.getString(android.R.string.cancel)
            setTextColor(style.accentColor)
            typeface = style.itemTypeface
            textSize = style.headerActionTextSizeSp
            isSingleLine = true
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPaddingRelative(
                0,
                context.sheetDp(SheetMetrics.HEADER_ACTION_PADDING_V_DP),
                context.sheetDp(SheetMetrics.HEADER_ACTION_PADDING_H_DP),
                context.sheetDp(SheetMetrics.HEADER_ACTION_PADDING_V_DP),
            )
            isClickable = true
            isFocusable = true
            publishAsSheetButton(this)
            setOnClickListener { onCancel() }
        }

        titleView.apply {
            text = title
            setTextColor(style.itemTextColor)
            typeface = Typeface.create(style.itemTypeface, Typeface.BOLD)
            textSize = style.headerTitleTextSizeSp
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
        }

        confirmView.apply {
            text = context.getString(android.R.string.ok)
            setTextColor(style.onAccentTextColor)
            typeface = Typeface.create(style.itemTypeface, Typeface.BOLD)
            textSize = style.headerActionTextSizeSp
            isSingleLine = true
            gravity = Gravity.CENTER
            setPadding(
                context.sheetDp(SheetMetrics.CONFIRM_PADDING_H_DP),
                context.sheetDp(SheetMetrics.CONFIRM_PADDING_V_DP),
                context.sheetDp(SheetMetrics.CONFIRM_PADDING_H_DP),
                context.sheetDp(SheetMetrics.CONFIRM_PADDING_V_DP),
            )
            val pill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.sheetDp(SheetMetrics.CONFIRM_CORNER_RADIUS_DP).toFloat()
                setColor(style.accentColor)
            }
            background = RippleDrawable(ColorStateList.valueOf(style.rippleColor), pill, null)
            isClickable = true
            isFocusable = true
            publishAsSheetButton(this)
            visibility = if (showConfirm) View.VISIBLE else View.GONE
            setOnClickListener { onConfirm() }
        }

        cancelSlot.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            minimumHeight = context.sheetDp(SheetMetrics.MIN_TOUCH_TARGET_DP)
            addView(
                cancelView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.START or Gravity.CENTER_VERTICAL,
                ),
            )
            delegateSheetTouchToChild(this, cancelView)
        }
        confirmSlot.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            minimumHeight = context.sheetDp(SheetMetrics.MIN_TOUCH_TARGET_DP)
            addView(
                confirmView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.END or Gravity.CENTER_VERTICAL,
                ),
            )
            delegateSheetTouchToChild(this, confirmView)
        }

        addView(cancelSlot)
        // タイトルは残り幅を占め、幅が足りないときに先に縮む側になる。
        addView(titleView, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(confirmSlot)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        resolveSlotMinWidths(available)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * 左右スロットの最小幅を、ヘッダーに与えられた幅に応じて決める。
     *
     * 通常は左右を `max(取消ラベル幅, 確定ラベル幅, 最小タップ領域)` の対称幅にしてタイトルを中央へ置く。
     * 対称幅ではヘッダーに収まらない場合は各ラベルの固有幅（最小タップ領域を下限とする）へ縮退させ、
     * 対称性より操作ラベルが画面内に収まることを優先する。
     */
    private fun resolveSlotMinWidths(availableWidth: Int) {
        val minTouchTarget = context.sheetDp(SheetMetrics.MIN_TOUCH_TARGET_DP)
        val cancelIntrinsic = maxOf(sheetDesiredWidthOf(cancelView), minTouchTarget)
        val confirmIntrinsic = maxOf(sheetDesiredWidthOf(confirmView), minTouchTarget)
        val symmetric = maxOf(cancelIntrinsic, confirmIntrinsic)
        if (symmetric * 2 <= availableWidth) {
            cancelSlot.minimumWidth = symmetric
            confirmSlot.minimumWidth = symmetric
        } else {
            cancelSlot.minimumWidth = cancelIntrinsic
            confirmSlot.minimumWidth = confirmIntrinsic
        }
    }
}

/**
 * スクロールを親へ伝播しないシート内リスト。
 *
 * リストのスクロールは常にリスト内部で完結させ、シートの高さや位置を動かさない。
 * `isNestedScrollingEnabled` 自体は有効なままにして `BottomSheetBehavior` から
 * 「スクロールする子」として認識させる（これによりリスト上で始まったドラッグをシートが
 * 横取りせず、リストのスクロールとして扱われる）。一方で nested scroll の開始・fling の
 * 伝播は行わないため、リストのスクロールがシートの展開や非表示を引き起こさない。
 */
internal class SelfContainedRecyclerView(context: Context) : RecyclerView(context) {
    override fun startNestedScroll(axes: Int): Boolean = false

    override fun startNestedScroll(axes: Int, type: Int): Boolean = false

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean = false

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean,
    ): Boolean = false
}
