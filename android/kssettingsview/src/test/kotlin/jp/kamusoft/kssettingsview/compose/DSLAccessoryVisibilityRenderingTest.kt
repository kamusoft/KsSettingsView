package jp.kamusoft.kssettingsview.compose

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * Compose 宣言 DSL の Header / Footer 表示トグルが、実際の行として表示へ届くことを検証する。
 *
 * 観測は Compose ラッパが生成した `KsSettingsView` 配下の RecyclerView に並んだ行のテキストで
 * 行い、DSL ツリーや Store の値だけを見て通したことにしない。Store 経路と DSL 経路を同一
 * composition に並べ、同じトグル操作が同じ表示結果へ到達すること（core/ADR-0018 の対称性）も
 * ここで確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DSLAccessoryVisibilityRenderingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // MARK: - 表示の観測

    /** composition に並んだ `KsSettingsView` を、view ツリーの出現順で集める。 */
    private fun settingsLayouts(): List<KsSettingsViewLayout> {
        val found = mutableListOf<KsSettingsViewLayout>()
        fun walk(view: View) {
            if (view is KsSettingsViewLayout) found.add(view)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(composeRule.activity.window.decorView)
        return found
    }

    /**
     * [index] 番目の `KsSettingsView` に並んでいる行のテキストを上から順に取り出す。
     *
     * View がまだ生成されていない間は空リストを返す（成立の待機は [awaitRows] が担う）。
     */
    private fun rowTexts(index: Int = 0): List<String> {
        val layout = settingsLayouts().getOrNull(index) ?: return emptyList()
        val recycler = findRecyclerView(layout) ?: return emptyList()
        return (0 until recycler.childCount).map { i ->
            collectTexts(recycler.getChildAt(i)).joinToString("/")
        }
    }

    private fun findRecyclerView(root: ViewGroup): RecyclerView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is RecyclerView) return child
            if (child is ViewGroup) findRecyclerView(child)?.let { return it }
        }
        return null
    }

    private fun collectTexts(view: View): List<String> = when (view) {
        is TextView -> listOfNotNull(view.text?.toString()?.takeIf { it.isNotBlank() })
        is ViewGroup -> (0 until view.childCount).flatMap { collectTexts(view.getChildAt(it)) }
        else -> emptyList()
    }

    /**
     * [index] 番目の `KsSettingsView` の行が [expected] になるまで、フレームとメインループを
     * 流しながら待つ。
     *
     * 差分計算はバックグラウンドで走り結果はメインループへ post されるため、成立を待つ形にする。
     * 時間切れは待機条件の誤りか反映の不達であり、黙って戻ると収束前の状態を検証したことに
     * されるため、その時点の行を載せて明示的に失敗させる。
     */
    private fun awaitRows(expected: List<String>, index: Int = 0, timeoutMillis: Long = 5_000) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            if (rowTexts(index) == expected) return
            if (System.nanoTime() >= deadline) {
                fail(
                    "表示行が $timeoutMillis ms 以内に $expected へ収束しなかった" +
                        " (index=$index の現在の行: ${rowTexts(index)})",
                )
            }
            // バックグラウンドで走る差分計算へ実行機会を譲る。yield は OS へのヒントに留まり、
            // CPU が飽和した状況では譲れる保証がないため sleep で確実に手放す。
            Thread.sleep(1)
        }
    }

    /**
     * Store 経路（index 0）と DSL 経路（index 1）の両方の行が [expected] へ収束するまで待つ。
     *
     * 2 経路は別々の RecyclerView であり反映のタイミングも独立しているため、片方の収束だけを待って
     * 両者を比較すると、もう片方が収束前の行のまま比較されて間欠的に失敗する。
     */
    private fun awaitBothRows(expected: List<String>) {
        awaitRows(expected, index = 0)
        awaitRows(expected, index = 1)
    }

    /** 検証に使う Section（Header「一般」/ Cell「A」/ Footer「補足」）。 */
    private fun storeSection(
        isHeaderVisible: Boolean = true,
        isFooterVisible: Boolean = true,
    ) = Section(
        id = "s1",
        header = SectionAccessory.Text("一般"),
        footer = SectionAccessory.Text("補足"),
        cells = listOf(LabelCell(id = "c1", title = "A")),
        isHeaderVisible = isHeaderVisible,
        isFooterVisible = isFooterVisible,
    )

    /** DSL 側の同一内容 Section。トグルだけを引数で切り替える。 */
    @Composable
    private fun DslSettings(
        isHeaderVisible: Boolean,
        isFooterVisible: Boolean,
        modifier: Modifier = Modifier,
    ) {
        KsSettingsView(modifier = modifier) {
            Section(
                header = "一般",
                footer = "補足",
                isHeaderVisible = isHeaderVisible,
                isFooterVisible = isFooterVisible,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
    }

    // MARK: - DSL 経路の表示反映

    @Test
    fun `DSL でトグルを指定して構築すると Header 行が現れない`() {
        composeRule.setContent {
            DslSettings(isHeaderVisible = false, isFooterVisible = true)
        }
        awaitRows(listOf("A", "補足"))
    }

    @Test
    fun `DSL 再評価でトグル変更が両方向に反映される`() {
        var headerVisible by mutableStateOf(true)
        composeRule.setContent {
            DslSettings(isHeaderVisible = headerVisible, isFooterVisible = true)
        }
        awaitRows(listOf("一般", "A", "補足"))

        composeRule.runOnUiThread { headerVisible = false }
        awaitRows(listOf("A", "補足"))

        composeRule.runOnUiThread { headerVisible = true }
        awaitRows(listOf("一般", "A", "補足"))
    }

    // MARK: - Store 経路と DSL 経路の対称性（core/ADR-0018）

    @Test
    fun `Store 経路と DSL 経路で Header トグルの表示結果が一致する`() {
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(storeSection())))
        var headerVisible by mutableStateOf(true)
        composeRule.setContent {
            Column {
                KsSettingsView(store = store, modifier = Modifier.weight(1f))
                DslSettings(
                    isHeaderVisible = headerVisible,
                    isFooterVisible = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        awaitBothRows(listOf("一般", "A", "補足"))

        // 同じトグル操作をそれぞれの経路で与える
        store.replaceSection("s1", storeSection(isHeaderVisible = false))
        composeRule.runOnUiThread { headerVisible = false }

        awaitBothRows(listOf("A", "補足"))
        assertEquals(
            "Header トグルの表示結果が Store 経路と DSL 経路で一致しない",
            rowTexts(0),
            rowTexts(1),
        )
    }

    @Test
    fun `Store 経路と DSL 経路で Footer トグルの表示結果が一致する`() {
        val store = SettingsRootStore(initialRoot = SettingsRoot(sections = listOf(storeSection())))
        var footerVisible by mutableStateOf(true)
        composeRule.setContent {
            Column {
                KsSettingsView(store = store, modifier = Modifier.weight(1f))
                DslSettings(
                    isHeaderVisible = true,
                    isFooterVisible = footerVisible,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        awaitBothRows(listOf("一般", "A", "補足"))

        store.replaceSection("s1", storeSection(isFooterVisible = false))
        composeRule.runOnUiThread { footerVisible = false }

        awaitBothRows(listOf("一般", "A"))
        assertEquals(
            "Footer トグルの表示結果が Store 経路と DSL 経路で一致しない",
            rowTexts(0),
            rowTexts(1),
        )
    }
}
