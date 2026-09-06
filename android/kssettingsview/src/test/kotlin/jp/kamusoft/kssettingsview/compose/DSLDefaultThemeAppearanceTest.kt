package jp.kamusoft.kssettingsview.compose

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jp.kamusoft.kssettingsview.ui.ButtonCell
import jp.kamusoft.kssettingsview.ui.ButtonCellViewHolder
import jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults
import jp.kamusoft.kssettingsview.ui.KsThemePalette
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * DSL 方式 Composable の `theme` 既定値が、composition 時の外観で選ばれることの検証。
 *
 * 既定値式が固定の `Theme()` に戻ると、composition の外観に関わらず同じ Theme が Store へ流れる。
 * ライト / ダークの両方を確かめて、片方だけ偶然一致している状態を弾く。
 *
 * あわせて、この入口を通した ButtonCell のタイトルが外観の ButtonCell 既定色（青）で描かれることを
 * 実描画値で確かめる。既定セットがタイトル色を埋めると 4 段解決（ButtonCell → CellStyle →
 * `Theme.cellTitleColor` → ButtonCell 既定）が 3 段目で止まり、ButtonCell のタイトルが白 / 黒に
 * なるため、Theme を渡さない経路と既定セットを明示的に渡す経路の両方を観測点にする。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DSLDefaultThemeAppearanceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "notnight")
    fun `theme を渡さない DSL はライトの既定セットを Store へ流す`() {
        assertEquals(KsSettingsViewDefaults.lightTheme(), themeOfDefaultDsl())
    }

    @Test
    @Config(qualifiers = "night")
    fun `theme を渡さない DSL は夜間モードでダークの既定セットを Store へ流す`() {
        assertEquals(KsSettingsViewDefaults.darkTheme(), themeOfDefaultDsl())
    }

    // MARK: - ButtonCell のタイトル既定（回帰）

    @Test
    @Config(qualifiers = "notnight")
    fun `theme を渡さない DSL の ButtonCell タイトルはライトの ButtonCell 既定になる`() {
        assertEquals(KsThemePalette.Light.buttonTitle.toArgb(), buttonTitleColorOfDsl())
    }

    @Test
    @Config(qualifiers = "night")
    fun `theme を渡さない DSL の ButtonCell タイトルは夜間でもダークの ButtonCell 既定になる`() {
        assertEquals(KsThemePalette.Dark.buttonTitle.toArgb(), buttonTitleColorOfDsl())
    }

    @Test
    @Config(qualifiers = "night")
    fun `accent を明示しても DSL の ButtonCell タイトル既定は変わらない`() {
        val accent = Color(0xFF12AB34)
        val actual = buttonTitleColorOfDsl(Theme(cellAccentColor = accent))
        assertEquals(KsThemePalette.Dark.buttonTitle.toArgb(), actual)
        assertNotEquals(accent.toArgb(), actual)
    }

    @Test
    @Config(qualifiers = "night")
    fun `darkTheme を明示的に渡しても ButtonCell タイトル既定は変わらない`() {
        assertEquals(
            KsThemePalette.Dark.buttonTitle.toArgb(),
            buttonTitleColorOfDsl(KsSettingsViewDefaults.darkTheme()),
        )
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `lightTheme を明示的に渡しても ButtonCell タイトル既定は変わらない`() {
        assertEquals(
            KsThemePalette.Light.buttonTitle.toArgb(),
            buttonTitleColorOfDsl(KsSettingsViewDefaults.lightTheme()),
        )
    }

    /**
     * DSL 方式を composition し、title を指定しない ButtonCell 行の実描画色（ARGB）を返す。
     *
     * @param theme 渡す Theme。`null` なら `theme` 引数を省略して既定値式を通す
     */
    private fun buttonTitleColorOfDsl(theme: Theme? = null): Int {
        composeRule.setContent {
            if (theme == null) {
                KsSettingsView {
                    Section(header = "見出し") {
                        cell(ButtonCell(id = "b1", title = "ボタン"))
                    }
                }
            } else {
                KsSettingsView(theme = theme) {
                    Section(header = "見出し") {
                        cell(ButtonCell(id = "b1", title = "ボタン"))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val layout = requireNotNull(findLayout()) { "KsSettingsView が View ツリーから見つかりません" }
        val recyclerView = layout.internalRecyclerView()
        val holders = (0 until recyclerView.childCount).map {
            recyclerView.getChildViewHolder(recyclerView.getChildAt(it))
        }
        val holder = holders.filterIsInstance<ButtonCellViewHolder>().singleOrNull()
        return requireNotNull(holder) { "ButtonCell 行が生成されていません: $holders" }
            .buttonTextView.currentTextColor
    }

    /** `theme` を渡さない DSL 方式を composition し、内部 View が受け取った利用者 Theme を返す。 */
    private fun themeOfDefaultDsl() = run {
        composeRule.setContent {
            KsSettingsView {
                Section(header = "見出し") {
                    cell(LabelCell(id = "c1", title = "A"))
                }
            }
        }
        composeRule.waitForIdle()
        val layout = requireNotNull(findLayout()) { "KsSettingsView が View ツリーから見つかりません" }
        layout.theme
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
