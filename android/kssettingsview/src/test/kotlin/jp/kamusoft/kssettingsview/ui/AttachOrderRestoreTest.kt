package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
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
 * Root の header / footer は Store の現在状態に含まれない UI 層プロパティであり、取り付け時の
 * 再取り込みでは戻らない。こちらは Store が bind 中の Host へ直接知らせる受け口が保持を担うため
 * （core/ADR-0033）、復元とは別経路の検証として同じクラスに並べる。
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
        assertEquals("取り付け前は Theme も Host に届かない", Theme(), view.theme)

        val expectedRows = listOf("更新見出し", "A2", "E", "B2", "追加見出し", "D")

        activity.container.addView(view)
        awaitConvergence(view, extraDiagnostics = { "Theme: ${view.theme}" }) {
            committedTexts(view) == expectedRows && view.theme == newTheme
        }
        activity.layoutSettingsView(view)

        assertEquals(
            "取り付け後の表示は Store 現在状態の構造・Section accessory・Cell 内容と一致する",
            expectedRows,
            visibleRowTexts(view),
        )
        assertEquals("取り付け後は Store の現在 Theme が反映される", newTheme, view.theme)
        assertEquals(
            "RecyclerView 背景も Store の現在 Theme の色になる",
            newTheme.backgroundColor.toArgb(),
            (view.internalRecyclerView().background as ColorDrawable).color,
        )
        assertEquals(
            "ItemDecoration も Store の現在 Theme で作り直される",
            newTheme.resolvedFor(darkTheme = false),
            (view.internalCurrentDecoration() as ClassicSectionDecoration).theme,
        )
        assertEquals(
            "表示中の Cell も Store の現在 Theme の配色で描かれる",
            newTheme.cellTitleColor.toArgb(),
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

    // MARK: - Root の header / footer の保持

    /** [initialRoot] を bind した Host が表示する、Root accessory を除いた行。 */
    private val initialRows = listOf("初期見出し", "A", "B", "C")

    /** 中身に [label] を表示する TextView を持つ、任意 View 形式の Root accessory。 */
    private fun viewAccessory(label: String): SettingsAccessory.Root = SettingsAccessory.Root(
        RootAccessory.View(
            KsAnyView.AndroidView { context -> TextView(context).apply { text = label } },
        ),
    )

    /** [label] を表示する text 形式の Root accessory。 */
    private fun textAccessory(label: String): SettingsAccessory.Root =
        SettingsAccessory.Root(RootAccessory.Text(label))

    /** 取り付けてから、Cell 行のコミットとレイアウトを済ませる。 */
    private fun HostActivity.attachAndSettle(target: KsSettingsView) {
        container.addView(target)
        awaitConvergence(target) { committedTexts(target) == initialRows }
        layoutSettingsView(target)
    }

    /** メインスレッド以外から [block] を実行し、完了まで待つ。 */
    private fun runOffMainThread(block: () -> Unit) {
        var failure: Throwable? = null
        val worker = Thread {
            try {
                block()
            } catch (error: Throwable) {
                failure = error
            }
        }
        worker.start()
        worker.join()
        failure?.let { throw it }
    }

    @Test
    fun `取り付け前に渡した Root の header が取り付け後に表示される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()

        activity.attachAndSettle(view)

        assertEquals(
            "取り付け前に渡した Root の header が先頭行に表示される",
            listOf("ヘッダ") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `取り付け前に渡した Root の footer が取り付け後に表示される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        store.updateAccessory(AccessoryTarget.RootFooter, textAccessory("フッタ"))
        idle()

        activity.attachAndSettle(view)

        assertEquals(
            "取り付け前に渡した Root の footer が末尾行に表示される",
            initialRows + "フッタ",
            visibleRowTexts(view),
        )
    }

    @Test
    fun `取り外し中に渡した Root の header の更新が再取り付け後に反映される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("旧ヘッダ"))
        activity.attachAndSettle(view)
        assertEquals("前提: 取り付け時に旧値が表示されている", "旧ヘッダ", visibleRowTexts(view).first())

        activity.container.removeView(view)
        idle()

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("新ヘッダ"))
        idle()

        activity.attachAndSettle(view)

        assertEquals(
            "取り外し中の更新が再取り付け後の先頭行に反映される",
            listOf("新ヘッダ") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `取り付け前に複数回渡した Root accessory は最後の値が反映される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ A"))
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ B"))
        store.updateAccessory(AccessoryTarget.RootFooter, textAccessory("フッタ C"))
        store.updateAccessory(AccessoryTarget.RootFooter, null)
        idle()

        activity.attachAndSettle(view)

        assertEquals(
            "header は最後に渡した値を表示し、解除した footer は行を作らない",
            listOf("ヘッダ B") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `bind 直後にキューを流さず渡した Root の header が表示される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        // bind から戻った時点で保持が成立していることを見るため、キューを流さずに続けて渡す。
        view.bind(store)
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))

        activity.attachAndSettle(view)

        assertEquals(
            "bind 直後に渡した値が取り付け後に表示される",
            listOf("ヘッダ") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `キューを流さない多数回の連続更新でも最後の Root の header が表示される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        // Store の通知の容量（extraBufferCapacity = 64）を超える件数を、キューを流さずに続けて渡す。
        val updateCount = 200
        repeat(updateCount) { index ->
            store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ $index"))
        }

        activity.attachAndSettle(view)

        assertEquals(
            "容量を超える連続更新でも最後の値が表示される",
            listOf("ヘッダ ${updateCount - 1}") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `同じ Store に bind した 2 つの Host の両方で Root の header が表示される`() {
        val activity = startActivity()

        val store = SettingsRootStore(initialRoot = initialRoot())
        val first = KsSettingsView(activity)
        val second = KsSettingsView(activity)
        first.bind(store)
        second.bind(store)
        idle()

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()

        activity.attachAndSettle(first)
        activity.attachAndSettle(second)

        assertEquals("先に bind した Host に反映される", "ヘッダ", visibleRowTexts(first).first())
        assertEquals("後に bind した Host にも反映される", "ヘッダ", visibleRowTexts(second).first())
    }

    @Test
    fun `一方の Host の unbind は他方の Root の header の受け取りを妨げない`() {
        val activity = startActivity()

        val store = SettingsRootStore(initialRoot = initialRoot())
        val unbound = KsSettingsView(activity)
        val bound = KsSettingsView(activity)
        unbound.bind(store)
        bound.bind(store)
        idle()

        unbound.unbind()
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()

        activity.attachAndSettle(bound)

        assertEquals(
            "bind を維持した Host は値を受け取る",
            listOf("ヘッダ") + initialRows,
            visibleRowTexts(bound),
        )
    }

    @Test
    fun `メインスレッド以外から渡した Root の header が取り付け後に表示される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        idle()

        runOffMainThread {
            store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        }

        activity.attachAndSettle(view)

        assertEquals(
            "メインスレッド以外から渡した値も取り付け後に表示される",
            listOf("ヘッダ") + initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `取り付け中にメインスレッド以外から渡した Root の header はメインスレッドで反映される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("旧ヘッダ"))
        activity.attachAndSettle(view)

        // 反映がどのスレッドで起きたかは、Adapter の変更通知を受けたスレッドで観測する。
        val notifiedThreads = mutableListOf<Thread>()
        view.internalHeaderAdapter().registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
                    notifiedThreads += Thread.currentThread()
                }
            },
        )

        runOffMainThread {
            store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("新ヘッダ"))
        }
        idle()
        activity.layoutSettingsView(view)

        assertEquals("新しい値が表示される", "新ヘッダ", visibleRowTexts(view).first())
        assertEquals(
            "表示への反映はメインスレッドで行われる",
            listOf(Looper.getMainLooper().thread),
            notifiedThreads.distinct(),
        )
    }

    @Test
    fun `別 Store へ bind し直した後は以前の Store へ渡した Root の header が反映されない`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val first = SettingsRootStore(initialRoot = initialRoot())
        val second = SettingsRootStore(initialRoot = initialRoot())
        view.bind(first)
        view.bind(second)
        idle()

        first.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()

        activity.attachAndSettle(view)

        assertEquals(
            "以前の Store へ渡した値は表示されない",
            initialRows,
            visibleRowTexts(view),
        )
    }

    @Test
    fun `unbind 後に渡した Root の header は反映されない`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        view.unbind()
        idle()

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("ヘッダ"))
        idle()

        activity.container.addView(view)
        awaitConvergence(view) { committedTexts(view) == initialRows }
        activity.layoutSettingsView(view)

        assertEquals("unbind 後に渡した値は表示されない", initialRows, visibleRowTexts(view))
    }

    @Test
    fun `取り付け中の Root の header の更新は変更通知を 1 回だけ発行する`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("旧ヘッダ"))
        activity.attachAndSettle(view)

        // 行数は常に 0 か 1 で二重適用を映さないため、Adapter が発行した通知の回数で観測する。
        val observer = ChangeRecordingObserver()
        view.internalHeaderAdapter().registerAdapterDataObserver(observer)

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("新ヘッダ"))
        idle()
        activity.layoutSettingsView(view)

        assertEquals("新しい値が表示される", "新ヘッダ", visibleRowTexts(view).first())
        assertEquals(
            "1 回の更新に対する Root の header の行への通知は 1 回",
            1,
            observer.notifications.size,
        )
    }

    @Test
    fun `同じ Store へ bind し直した Host でも Root の header の更新は 1 回だけ適用される`() {
        val activity = startActivity()

        val view = KsSettingsView(activity)
        val store = SettingsRootStore(initialRoot = initialRoot())
        view.bind(store)
        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("旧ヘッダ"))
        activity.attachAndSettle(view)
        view.bind(store)
        idle()

        val observer = ChangeRecordingObserver()
        view.internalHeaderAdapter().registerAdapterDataObserver(observer)

        store.updateAccessory(AccessoryTarget.RootHeader, viewAccessory("新ヘッダ"))
        idle()
        activity.layoutSettingsView(view)

        assertEquals("新しい値が表示される", "新ヘッダ", visibleRowTexts(view).first())
        assertEquals(
            "bind をやり直しても通知は 1 回",
            1,
            observer.notifications.size,
        )
    }
}
