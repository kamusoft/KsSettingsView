package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 取り付け順序によらず表示が Store の現在状態へ収束することを検証する（core/ADR-0019）。
 *
 * Host を作ってから Store を操作し、その後で view 階層へ取り付ける順序でも、取り付け後には
 * 設定ツリーの構造・Cell 内容・Section accessory・Theme が Store の現在状態と一致する。
 * `diffs` / `contentUpdateBatches` は replay を持たないため、取り付け前・detach 中に発行された
 * 更新は通知としては誰にも届かない。収束は `onAttachedToWindow` の Store 再取り込みが担う。
 *
 * 収束の観測境界は「取り付け後、メインスレッドのキューが空になった時点」である。Theme の
 * `StateFlow` 購読開始と `submitList` の差分コミットが非同期に走るため、判定はメインループを
 * 流し切ってから行う。
 *
 * Root の header / footer は Store の現在状態に含まれない UI 層プロパティのため復元対象ではなく、
 * 本テストの検証対象にも含めない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AttachOrderRestoreTest {

    /** `KsSettingsView` を載せる器だけを持つホスト Activity。 */
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

    private fun startActivity(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView(target: KsSettingsView) {
        val metrics = resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /** 表示中の行のうち [text] を表示している TextView の文字色を返す。 */
    private fun rowTextColor(view: KsSettingsView, text: String): Int {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount)
            .flatMap { collectTextViews(rv.getChildAt(it)) }
            .firstOrNull { it.text?.toString() == text }
        return requireNotNull(found) { "表示行に \"$text\" の TextView が見つからない" }.currentTextColor
    }

    private fun collectTextViews(view: View): List<TextView> = when (view) {
        is TextView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { collectTextViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    /** 取り付け前の Host に bind するための初期 root（Section 1 つ + Cell 3 つ）。 */
    private fun initialRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.Text("初期見出し"),
                cells = listOf(
                    LabelCell(id = "c1", title = "A"),
                    LabelCell(id = "c2", title = "B"),
                    LabelCell(id = "c3", title = "C"),
                ),
            ),
        ),
    )

    @Test
    fun `取り付け前の構造操作と内容更新と Theme 変更が取り付け後に反映される`() {
        val activity = startActivity()

        // view 階層へ取り付ける前に bind する（Compose `AndroidView.factory` や
        // MAUI Handler のプロパティマッパー適用と同じ順序）。
        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        val newTheme = Theme(
            backgroundColor = Color(0xFF102030),
            separatorColor = Color(0xFF405060),
            cellTitleColor = Color(0xFF00A0B0),
        )

        // 取り付け前に構造・Section accessory・Cell 内容・Theme をひととおり動かす。
        store.insertSection(
            Section(
                id = "s2",
                header = SectionAccessory.Text("追加見出し"),
                cells = listOf(LabelCell(id = "c4", title = "D")),
            ),
            at = 1,
        )
        store.insertCell(LabelCell(id = "c5", title = "E"), sectionId = "s1", at = 1)
        store.removeCell("c3")
        store.updateAccessory(
            target = AccessoryTarget.SectionHeader("s1"),
            accessory = SettingsAccessory.Section(SectionAccessory.Text("更新見出し")),
        )
        store.replaceCells(
            listOf(
                "c1" to LabelCell(id = "c1", title = "A2"),
                "c2" to LabelCell(id = "c2", title = "B2"),
            ),
        )
        store.applyTheme(newTheme)
        idle()

        // 取り付け前は購読が張られておらず、更新は通知として届いていない
        // （収束が取り付け時の再取り込みによることを示す対照）。
        assertEquals("取り付け前は Store 更新が Host に届かない", listOf("A", "B", "C"), cellTitles(view))
        assertEquals("取り付け前は Theme も Host に届かない", Theme(), view.internalTheme())

        val expectedRows = listOf("更新見出し", "A2", "E", "B2", "追加見出し", "D")

        activity.container.addView(view)
        awaitConvergence(view, extraDiagnostics = { "Theme: ${view.internalTheme()}" }) {
            committedTexts(view) == expectedRows && view.internalTheme() == newTheme
        }
        activity.layoutSettingsView(view)

        assertEquals(
            "取り付け後の表示は Store 現在状態の構造・Section accessory・Cell 内容と一致する",
            expectedRows,
            visibleRowTexts(view),
        )
        assertEquals("取り付け後は Store の現在 Theme が反映される", newTheme, view.internalTheme())
        assertEquals(
            "RecyclerView 背景も Store の現在 Theme の色になる",
            newTheme.backgroundColor.toArgb(),
            (view.internalRecyclerView().background as ColorDrawable).color,
        )
        assertEquals(
            "ItemDecoration も Store の現在 Theme で作り直される",
            newTheme,
            (view.internalCurrentDecoration() as ClassicSectionDecoration).theme,
        )
        assertEquals(
            "表示中の Cell も Store の現在 Theme の配色で描かれる",
            newTheme.cellTitleColor?.toArgb(),
            rowTextColor(view, "A2"),
        )
    }

    @Test
    fun `detach 中の Cell 内容更新が再取り付け後に反映される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        val initialRows = listOf("初期見出し", "A", "B", "C")

        activity.container.addView(view)
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == initialRows }
        activity.layoutSettingsView(view)

        assertEquals("detach 前は Store の初期状態が表示されている", initialRows, visibleRowTexts(view))

        activity.container.removeView(view)
        idle()

        // detach 中は購読が切れているため、単数・複数どちらの内容更新も通知としては届かない。
        store.replaceCell("c1", LabelCell(id = "c1", title = "A2"))
        store.replaceCells(
            listOf(
                "c2" to LabelCell(id = "c2", title = "B2"),
                "c3" to LabelCell(id = "c3", title = "C2"),
            ),
        )
        idle()
        assertEquals("detach 中は内容更新が Host に届かない", listOf("A", "B", "C"), cellTitles(view))

        val restoredRows = listOf("初期見出し", "A2", "B2", "C2")

        activity.container.addView(view)
        awaitConvergence(view) { committedTexts(view) == restoredRows }
        activity.layoutSettingsView(view)

        assertEquals(
            "再取り付け後の表示は Store 現在状態の Cell 内容と一致する",
            restoredRows,
            visibleRowTexts(view),
        )
    }
}
