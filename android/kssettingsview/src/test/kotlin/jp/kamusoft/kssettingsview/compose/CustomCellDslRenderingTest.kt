package jp.kamusoft.kssettingsview.compose

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * DSL 直置きの [jp.kamusoft.kssettingsview.ui.CustomCell] が、Registry への明示登録なしで
 * Compose ラッパ経由の行として成立することを end-to-end で確認する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellDslRenderingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `Registry 未操作でも DSL 直置きの CustomCell が行として反映される`() {
        composeRule.setContent {
            KsSettingsView {
                Section(header = "カスタム") {
                    CustomCell(content = "A") { value ->
                        Box(Modifier.testTag("probe-$value").fillMaxWidth().height(80.dp))
                    }.cellHeight(120.dp)
                    CustomCell { Box(Modifier.testTag("static").fillMaxWidth().height(40.dp)) }
                }
            }
        }
        composeRule.waitForIdle()

        val layout = findSettingsLayout(composeRule.activity.window.decorView as ViewGroup)
        assertNotNull("Compose ラッパが KsSettingsView を生成する", layout)
        val recycler = findRecyclerView(layout!!)
        assertNotNull(recycler)
        // Section ヘッダ 1 + CustomCell 2
        assertEquals(3, recycler!!.adapter!!.itemCount)
    }

    private fun findSettingsLayout(root: ViewGroup): KsSettingsViewLayout? {
        if (root is KsSettingsViewLayout) return root
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is KsSettingsViewLayout) return child
            if (child is ViewGroup) findSettingsLayout(child)?.let { return it }
        }
        return null
    }

    private fun findRecyclerView(root: ViewGroup): RecyclerView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is RecyclerView) return child
            if (child is ViewGroup) findRecyclerView(child)?.let { return it }
        }
        return null
    }
}
