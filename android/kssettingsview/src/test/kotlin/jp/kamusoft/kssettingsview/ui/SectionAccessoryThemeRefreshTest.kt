package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
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
 * Theme 変更が Section Header / Footer をどこまで追随させるかを検証する。
 *
 * text 形式は文字色が新しい Theme へ追従する。View 形式は Theme が決める文字を持たないため
 * 中身を作り直さず、Theme が決める寸法（`Theme.headerHeight`）だけを反映する。中身の作り直しは
 * `KsAnyView.AndroidView` の View の内部状態（入力途中のテキスト等）を失わせるため、見た目が
 * 変わらない更新でそれを起こさないことが利用者から見た保証になる。
 *
 * 中身そのものが差し替わった更新では従来どおり作り直す。両者を分けているのが payload の種類で、
 * 本テストは「Theme だけの通知」と「内容の通知」を実経路で撃ち分けて観測する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SectionAccessoryThemeRefreshTest {

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

    private fun HostActivity.layoutSettingsView(target: KsSettingsView) {
        val metrics = resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /** 部分更新通知が実際の再 bind へ届くところまで進める。 */
    private fun HostActivity.settle(target: KsSettingsView) {
        repeat(2) {
            idle()
            layoutSettingsView(target)
        }
    }

    private fun textViewOf(view: KsSettingsView, text: String): TextView {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount)
            .flatMap { collectTextViews(rv.getChildAt(it)) }
            .firstOrNull { it.text?.toString() == text }
        return requireNotNull(found) { "表示行に \"$text\" の TextView が見つからない" }
    }

    private fun collectTextViews(view: View): List<TextView> = when (view) {
        is TextView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { collectTextViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    /** 表示中の行に載っている唯一の `EditText`（View 形式 accessory の中身）を返す。 */
    private fun editTextIn(view: KsSettingsView): EditText {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount)
            .flatMap { collectTextViews(rv.getChildAt(it)) }
            .filterIsInstance<EditText>()
        return found.singleOrNull()
            ?: error("表示行の EditText がちょうど 1 つではない (${found.size} 個)")
    }

    /** 表示中の View 形式 accessory の container（`FrameLayout`）の実高さを返す。 */
    private fun accessoryContainerHeight(view: KsSettingsView): Int {
        val rv = view.internalRecyclerView()
        val container = (0 until rv.childCount)
            .map { rv.getChildAt(it) }
            .filterIsInstance<FrameLayout>()
            .firstOrNull()
        return requireNotNull(container) { "View 形式 accessory の container が見つからない" }
            .layoutParams.height
    }

    private fun rootWith(
        header: SectionAccessory? = null,
        footer: SectionAccessory? = null,
    ): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = header,
                footer = footer,
                cells = listOf(LabelCell(id = "c1", title = "A")),
            ),
        ),
    )

    private fun hostView(
        activity: HostActivity,
        root: SettingsRoot,
        theme: Theme = Theme(),
    ): KsSettingsView {
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.setRootDirect(root, theme)
        awaitConvergence(view) { committedTexts(view).contains("A") }
        activity.layoutSettingsView(view)
        return view
    }

    // ---------------------------------------------------------------------
    // text 形式は Theme に追従する
    // ---------------------------------------------------------------------

    @Test
    fun `theme 代入で表示中の Section Header のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF1933E6)
        val view = hostView(
            activity,
            rootWith(header = SectionAccessory.Text("静的 Section")),
            Theme(headerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の headerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "静的 Section").currentTextColor,
        )

        view.theme = Theme(headerTextColor = updated)
        activity.settle(view)

        assertEquals(
            "theme 代入後の Section Header の文字色は新しい Theme の headerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "静的 Section").currentTextColor,
        )
    }

    @Test
    fun `theme 代入で表示中の Section Footer のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF008040)
        val view = hostView(
            activity,
            rootWith(footer = SectionAccessory.Text("補足説明")),
            Theme(footerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の footerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "補足説明").currentTextColor,
        )

        view.theme = Theme(footerTextColor = updated)
        activity.settle(view)

        assertEquals(
            "theme 代入後の Section Footer の文字色は新しい Theme の footerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "補足説明").currentTextColor,
        )
    }

    // ---------------------------------------------------------------------
    // View 形式は Theme 変更で中身を作り直さない
    // ---------------------------------------------------------------------

    @Test
    fun `theme 代入で View 形式の Section Header は factory が再実行されない`() {
        val activity = startActivity()
        var factoryCount = 0
        val view = hostView(
            activity,
            rootWith(
                header = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx ->
                        factoryCount += 1
                        TextView(ctx).apply { text = "View 形式ヘッダ" }
                    },
                ),
            ),
            Theme(headerTextColor = Color(0xFFE01919)),
        )
        assertEquals("前提: View 形式の Section Header が一度も生成されていない", 1, factoryCount)

        view.theme = Theme(headerTextColor = Color(0xFF1933E6))
        activity.settle(view)

        assertEquals(
            "View 形式の Section Header は Theme 変更で factory を再実行しない（内部状態を失うため）",
            1,
            factoryCount,
        )
    }

    @Test
    fun `theme 代入で View 形式の Section Footer は factory が再実行されない`() {
        val activity = startActivity()
        var factoryCount = 0
        val view = hostView(
            activity,
            rootWith(
                footer = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx ->
                        factoryCount += 1
                        TextView(ctx).apply { text = "View 形式フッタ" }
                    },
                ),
            ),
            Theme(footerTextColor = Color(0xFFE01919)),
        )
        assertEquals("前提: View 形式の Section Footer が一度も生成されていない", 1, factoryCount)

        view.theme = Theme(footerTextColor = Color(0xFF008040))
        activity.settle(view)

        assertEquals(
            "View 形式の Section Footer は Theme 変更で factory を再実行しない（内部状態を失うため）",
            1,
            factoryCount,
        )
    }

    @Test
    fun `theme 代入を挟んでも View 形式の Section Header の入力途中テキストが残る`() {
        val activity = startActivity()
        val view = hostView(
            activity,
            rootWith(header = SectionAccessory.View(KsAnyView.AndroidView { ctx -> EditText(ctx) })),
            Theme(headerTextColor = Color(0xFFE01919)),
        )

        editTextIn(view).setText("入力途中")
        activity.settle(view)
        assertEquals("前提: 入力が反映されていない", "入力途中", editTextIn(view).text.toString())

        view.theme = Theme(headerTextColor = Color(0xFF1933E6))
        activity.settle(view)

        assertEquals(
            "Theme 変更を挟んでも View 形式 Section Header の入力途中テキストは保持される",
            "入力途中",
            editTextIn(view).text.toString(),
        )
    }

    @Test
    fun `theme 代入で View 形式の Section Header の固定高さは Theme に追従する`() {
        val activity = startActivity()
        val view = hostView(
            activity,
            rootWith(
                header = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx -> TextView(ctx).apply { text = "View 形式ヘッダ" } },
                ),
            ),
            Theme(headerHeight = 40.0),
        )
        val density = view.resources.displayMetrics.density
        assertEquals(
            "前提: 初期 Theme の headerHeight が反映されていない",
            (40.0 * density).toInt(),
            accessoryContainerHeight(view),
        )

        view.theme = Theme(headerHeight = 90.0)
        activity.settle(view)

        assertEquals(
            "中身を作り直さない Theme 変更でも、Theme 由来の固定高さは反映される",
            (90.0 * density).toInt(),
            accessoryContainerHeight(view),
        )
    }

    // ---------------------------------------------------------------------
    // 内容の差し替えは従来どおり作り直す
    // ---------------------------------------------------------------------

    @Test
    fun `View 形式の Section Header は別インスタンスへの差し替えで作り直される`() {
        val activity = startActivity()
        var factoryCount = 0
        val theme = Theme(headerTextColor = Color(0xFFE01919))
        val view = hostView(
            activity,
            rootWith(
                header = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx ->
                        factoryCount += 1
                        TextView(ctx).apply { text = "View 形式ヘッダ" }
                    },
                ),
            ),
            theme,
        )
        assertEquals("前提: View 形式の Section Header が一度も生成されていない", 1, factoryCount)

        // Theme は据え置き、accessory の中身だけを別インスタンスへ差し替える。
        view.setRootDirect(
            rootWith(
                header = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx ->
                        factoryCount += 1
                        TextView(ctx).apply { text = "差し替え後ヘッダ" }
                    },
                ),
            ),
            theme,
        )
        awaitConvergence(view, extraDiagnostics = { "factory 呼び出し回数: $factoryCount" }) {
            activity.layoutSettingsView(view)
            factoryCount == 2
        }

        assertEquals("中身の差し替えでは View 形式の Section Header を作り直す", 2, factoryCount)
        assertEquals(
            "差し替え後の中身が表示される",
            "差し替え後ヘッダ",
            textViewOf(view, "差し替え後ヘッダ").text.toString(),
        )
    }
}
