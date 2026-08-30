package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
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
 * `Theme.cellPlaceholderColor` の変更が、表示中の [EntryCell] の hint 色へ追従することを
 * 実際の View 経路（`KsSettingsView` へ Theme を代入し、表示行が再 bind される経路）で検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EntryCellPlaceholderThemeRefreshTest {

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

    /** Theme 通知が実際の再 bind へ届くところまで進める。 */
    private fun HostActivity.settle(target: KsSettingsView) {
        repeat(2) {
            idle()
            layoutSettingsView(target)
        }
    }

    /** 表示中の行に載っている唯一の `EditText`（`EntryCell` の入力欄）を返す。 */
    private fun editTextIn(view: KsSettingsView): EditText {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount).flatMap { collectEditTexts(rv.getChildAt(it)) }
        return found.singleOrNull()
            ?: error("表示行の EditText がちょうど 1 つではない (${found.size} 個)")
    }

    private fun collectEditTexts(view: View): List<EditText> = when (view) {
        is EditText -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { collectEditTexts(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun rootWithEntry(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                cells = listOf(
                    EntryCell(id = "entry-1", title = "名前", text = "", placeholder = "未入力"),
                ),
            ),
        ),
    )

    @Test
    fun `theme 代入で表示中の EntryCell の placeholder 色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF1933E6)

        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.setRootDirect(rootWithEntry(), Theme(cellPlaceholderColor = initial))
        awaitConvergence(view) { view.internalRecyclerView().childCount > 0 }
        activity.layoutSettingsView(view)

        assertEquals(
            "前提: 初期 Theme の cellPlaceholderColor が hint 色へ反映されていない",
            initial.toArgb(),
            editTextIn(view).currentHintTextColor,
        )

        view.theme = Theme(cellPlaceholderColor = updated)
        activity.settle(view)

        assertEquals(
            "theme 代入後の hint 色は新しい Theme の cellPlaceholderColor になる",
            updated.toArgb(),
            editTextIn(view).currentHintTextColor,
        )
    }
}
