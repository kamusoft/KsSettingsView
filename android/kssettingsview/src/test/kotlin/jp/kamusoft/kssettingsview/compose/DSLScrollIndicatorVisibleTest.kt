package jp.kamusoft.kssettingsview.compose

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.Theme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * DSL 方式の入口を通しても `Theme.scrollIndicatorVisible` が設定リストの縦スクロールバーへ届くことの
 * 検証。Store 方式と DSL 方式は同じ観測結果になることを両方で確かめる決まりになっている。
 *
 * Store 方式の同じ検証は `jp.kamusoft.kssettingsview.ui.ScrollIndicatorVisibleTest` にある。DSL 方式は
 * 内部で Store へ流し込む経路が別なので、片方だけでは再評価で届かない回帰を拾えない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DSLScrollIndicatorVisibleTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `DSL の再評価で渡した false が縦スクロールバーへ届く`() {
        var theme by mutableStateOf(Theme())
        composeRule.setContent {
            KsSettingsView(theme = theme) {
                Section(header = "見出し") {
                    cell(LabelCell(id = "c1", title = "A"))
                }
            }
        }
        composeRule.waitForIdle()

        val recyclerView = requireNotNull(findLayout()) {
            "KsSettingsView が View ツリーから見つかりません"
        }.internalRecyclerView()
        assertTrue("前提: 既定では有効", recyclerView.isVerticalScrollBarEnabled)

        theme = Theme(scrollIndicatorVisible = false)
        composeRule.waitForIdle()

        assertFalse(
            "DSL 経由の Theme 更新でも縦スクロールバーが無効になる",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    @Test
    fun `DSL に最初から渡した false が縦スクロールバーへ届く`() {
        composeRule.setContent {
            KsSettingsView(theme = Theme(scrollIndicatorVisible = false)) {
                Section(header = "見出し") {
                    cell(LabelCell(id = "c1", title = "A"))
                }
            }
        }
        composeRule.waitForIdle()

        val recyclerView = requireNotNull(findLayout()) {
            "KsSettingsView が View ツリーから見つかりません"
        }.internalRecyclerView()
        assertFalse(
            "初期構築の DSL でも scrollIndicatorVisible = false が反映される",
            recyclerView.isVerticalScrollBarEnabled,
        )
    }

    private fun findLayout(): KsSettingsViewLayout? =
        findLayoutIn(composeRule.activity.window.decorView as ViewGroup)

    private fun findLayoutIn(parent: ViewGroup): KsSettingsViewLayout? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is KsSettingsViewLayout) return child
            if (child is ViewGroup) findLayoutIn(child)?.let { return it }
        }
        return null
    }
}
