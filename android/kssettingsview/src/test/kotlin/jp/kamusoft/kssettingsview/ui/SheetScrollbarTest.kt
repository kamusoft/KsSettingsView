package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowDialog
import org.robolectric.annotation.Config

/**
 * 選択面の中の `RecyclerView` に縦スクロールバーが付く範囲を検証する。
 *
 * `Theme.scrollIndicatorVisible` に従うのは PickerCell の候補リストまでで、回転ホイールの可動部は
 * 対象外である。スクロールバーを描ける View にするための `android:scrollbars` は `recyclerViewStyle`
 * 経由で届くため、これを同梱テーマ自体へ置くと同じテーマから作られるリスト全部に付いてしまう。
 * 属性の適用点が候補リストと設定リストに限られていることを、選択面の両側から観測する。
 *
 * 観測点は 2 つに分ける。スクロールバーを描ける View かどうかは描画状態
 * (`verticalScrollbarThumbDrawable`。`android:scrollbars` が届いていなければ `null`)、表示するか
 * どうかは `isVerticalScrollBarEnabled` で見る。前者だけでは Theme の設定が届いたか分からず、
 * 後者だけでは描画状態の無い View でも `true` を返し得る。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SheetScrollbarTest {

    /** `KsSettingsView` を 1 つだけ載せるホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private fun rootWithPickers(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(
                    PickerCell(
                        id = "picker",
                        title = "テーマ",
                        items = listOf("ライト", "ダーク", "システム"),
                    ),
                    NumberPickerCell(id = "number", title = "音量", value = 50, min = 0, max = 100),
                ),
            ),
        ),
    )

    /** [theme] を渡した `KsSettingsView` を表示し、レイアウトまで済ませて返す。 */
    private fun showView(theme: Theme): KsSettingsView {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(SettingsRootStore(initialRoot = rootWithPickers(), initialTheme = theme))
        awaitConvergence(view) { view.internalMainListAdapter().currentList.isNotEmpty() }
        val metrics = view.resources.displayMetrics
        view.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        idle()
        return view
    }

    /** 表示中の行のうち [id] の Cell の行をタップして選択面を開く。 */
    private fun openSheetOf(view: KsSettingsView, id: String) {
        val recyclerView = view.internalRecyclerView()
        val holder = (0 until recyclerView.childCount)
            .map { recyclerView.getChildViewHolder(recyclerView.getChildAt(it)) }
            .first { holder ->
                val position = holder.bindingAdapterPosition
                val item = view.internalMainListAdapter().currentList.getOrNull(position)
                item is CellListItem.CellRow && item.cell.id == id
            }
        holder.itemView.performClick()
        idle()
    }

    @Test
    fun `既定の Theme では候補リストにスクロールバーが出る`() {
        val view = showView(Theme())
        openSheetOf(view, "picker")
        val sheet = ShadowDialog.getLatestDialog() as PickerSelectionSheet

        assertNotNull(
            "候補リストはスクロールバーの描画に必要な状態が初期化されている",
            sheet.listView.verticalScrollbarThumbDrawable,
        )
        assertTrue(
            "既定の Theme では候補リストの縦スクロールバーが有効",
            sheet.listView.isVerticalScrollBarEnabled,
        )
    }

    @Test
    fun `false を指定した Theme では候補リストのスクロールバーが無効になる`() {
        val view = showView(Theme(scrollIndicatorVisible = false))
        openSheetOf(view, "picker")
        val sheet = ShadowDialog.getLatestDialog() as PickerSelectionSheet

        assertFalse(
            "Theme.scrollIndicatorVisible = false は候補リストにも反映される",
            sheet.listView.isVerticalScrollBarEnabled,
        )
    }

    /**
     * 表示中の選択面は Theme 差し替えに追従しなくてよいが、開き直した選択面は新しい値に従う。
     *
     * 候補リストは開いた時点の値を控えるため、追従しないことは構造上そうなる。一方「次に開いたとき
     * から新しい値に従う」は、Theme を差し替えてから開き直す経路を通らないと確かめられない。行の
     * 再 bind が漏れると、開き直しても古い値のままになる。
     */
    @Test
    fun `Theme を差し替えて開き直した候補リストは新しい値に従う`() {
        val view = showView(Theme())
        openSheetOf(view, "picker")
        val first = ShadowDialog.getLatestDialog() as PickerSelectionSheet
        assertTrue("前提: 既定では有効", first.listView.isVerticalScrollBarEnabled)
        first.dismiss()
        idle()

        view.theme = Theme(scrollIndicatorVisible = false)
        idle()
        openSheetOf(view, "picker")
        val reopened = ShadowDialog.getLatestDialog() as PickerSelectionSheet

        assertFalse(
            "開き直した候補リストは差し替え後の Theme に従う",
            reopened.listView.isVerticalScrollBarEnabled,
        )
    }

    @Test
    fun `回転ホイールの可動部にはスクロールバーの描画状態が無い`() {
        val view = showView(Theme())
        openSheetOf(view, "number")
        val sheet = ShadowDialog.getLatestDialog() as NumberSelectionSheet

        assertNull(
            "回転ホイールの可動部にはスクロールバーを付けない",
            sheet.wheelView.listView.verticalScrollbarThumbDrawable,
        )
    }

    @Test
    fun `同梱テーマから作った RecyclerView にスクロールバーの描画状態が無い`() {
        // ライブラリ所有 UI は同梱テーマ付き Context から生成される。属性の適用点が設定リストと
        // 候補リストの生成箇所に限られていることを、経路をひとつ挟まずに直接観測する。
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val themed = ctrl.get().ksThemedContext()

        assertNull(
            "同梱テーマ自体は recyclerViewStyle でスクロールバーを配らない",
            SelfContainedRecyclerView(themed).verticalScrollbarThumbDrawable,
        )
    }
}
