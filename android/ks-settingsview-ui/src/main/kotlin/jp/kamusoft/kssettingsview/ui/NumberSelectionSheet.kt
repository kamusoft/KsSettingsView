package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * [NumberPickerCell] の選択面（ボトムシート + スナップ式ホイール）。
 *
 * 器は Material の `BottomSheetDialog`、中身は「ドラッグハンドル + ヘッダー + ホイール」で構成する
 * （android/ADR-0007。ヘッダーの意匠は [PickerSelectionSheet]（android/ADR-0005）と共有）。
 * ヘッダーは左に取消、中央に `pickerTitle ?: title`、右に確定を置き、操作ラベルは OS の公開
 * 文字列リソース（`android.R.string.ok` / `android.R.string.cancel`）から解決する。
 *
 * 確定経路は確定ボタンだけで、そのとき選択中の候補 index を [onConfirmed] へ1回渡して閉じる。
 * それ以外の閉じ方（取消ボタン・シート外側タップ・Back 操作・下方向スワイプ）では callback を
 * 発火しない。ホイールのスクロールはシートへ伝播しないため、候補領域の下方向操作は候補の遷移に
 * なり dismiss を引き起こさない。
 *
 * 高さはコンテンツ高（ホイールの可視行数で固定）で表示する。
 *
 * 器と中身はライブラリ所有の UI であり、ホストのテーマに関わらず同梱テーマをかぶせた Context から
 * 生成する（android/ADR-0020）。`bottomSheetDialogTheme` も同梱テーマが提供するため、ホストが
 * Material3 派生テーマでなくても成立する。
 *
 * @param hostContext シートを表示する Context
 * @param sheetTitle ヘッダー中央に表示するタイトル
 * @param itemCount 候補の件数
 * @param displayTextAt index に対応する候補の表示文字列（unit 適用後）を返す関数
 * @param initialIndex 初期の選択中 index
 * @param sheetStyle 解決済みのスタイル値
 * @param onConfirmed 確定 callback（確定操作でのみ発火する）
 */
internal class NumberSelectionSheet(
    hostContext: Context,
    sheetTitle: String,
    itemCount: Int,
    displayTextAt: (Int) -> String,
    initialIndex: Int,
    private val sheetStyle: PickerSheetStyle,
    private val onConfirmed: (Int) -> Unit,
) : BottomSheetDialog(hostContext.ksThemedContext()) {

    /** シート内容のルート（ドラッグハンドル + ヘッダー + ホイール）。 */
    internal val contentRoot: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    /** 候補のホイール。 */
    internal val wheelView: KsWheelView = KsWheelView(
        context = context,
        itemCount = itemCount,
        displayTextAt = displayTextAt,
        initialIndex = initialIndex,
        wheelStyle = KsWheelStyle.from(sheetStyle),
    )

    /** ヘッダー（取消 / タイトル / 確定）。 */
    internal val headerView: SheetHeaderView = SheetHeaderView(
        context = context,
        style = sheetStyle,
        title = sheetTitle,
        showConfirm = true,
        onCancel = { cancel() },
        onConfirm = { confirmSelection() },
    )

    /** ヘッダー左の取消ラベル。 */
    internal val cancelView: TextView get() = headerView.cancelView

    /** ヘッダー中央のタイトル。 */
    internal val titleView: TextView get() = headerView.titleView

    /** ヘッダー右の確定ラベル。 */
    internal val confirmView: TextView get() = headerView.confirmView

    /** ヘッダー左のスロット（取消ラベルの当たり判定を担う）。 */
    internal val cancelSlot: FrameLayout get() = headerView.cancelSlot

    /** ヘッダー右のスロット（確定ラベルの当たり判定を担う）。 */
    internal val confirmSlot: FrameLayout get() = headerView.confirmSlot

    init {
        contentRoot.addView(buildSheetDragHandle(context, sheetStyle.separatorColor))
        contentRoot.addView(headerView)
        contentRoot.addView(buildDivider())
        contentRoot.addView(wheelView)
        contentRoot.addView(buildBottomPadding())
        setContentView(contentRoot)
        // シート外側のタップで閉じられる（確定 callback は発火しない）。
        setCanceledOnTouchOutside(true)
    }

    override fun onStart() {
        super.onStart()
        val container = findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        applySheetSurfaceColor(container, sheetStyle.sheetBackgroundColor)
        // 内容の高さが固定（ホイールの可視行数分）なので、常に内容高で全展開して表示する。
        // 折り目を経由しないため、下方向のドラッグはそのまま dismiss になる。
        BottomSheetBehavior.from(container).apply {
            isFitToContents = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /** ヘッダーとホイールの境界に引く区切り線。 */
    private fun buildDivider(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            DIVIDER_THICKNESS_PX,
        )
        setBackgroundColor(sheetStyle.separatorColor)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** ホイール下端とシート下端のあいだの余白。 */
    private fun buildBottomPadding(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            context.sheetDp(BOTTOM_PADDING_DP),
        )
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * 確定操作。その時点で選択中の候補 index を1回だけ通知して閉じる。
     *
     * 選択中候補はスナップ静止時にのみ更新されるため、慣性移動の途中で確定しても採用されるのは
     * 直前に静止した候補になる。
     */
    private fun confirmSelection() {
        val index = wheelView.selectedIndex
        if (index != KsWheelView.NO_SELECTION) {
            onConfirmed(index)
        }
        dismiss()
    }

    companion object {
        /** 区切り線の太さ（1 物理 pixel 固定。dp 換算しない）。 */
        private const val DIVIDER_THICKNESS_PX: Int = 1

        /** ホイール下の余白（dp）。デザイン確定値。 */
        private const val BOTTOM_PADDING_DP: Float = 18f
    }
}
