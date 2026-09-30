package jp.kamusoft.kssettingsview.compose

import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RootAnyViewAccessoryViewHolder
import jp.kamusoft.kssettingsview.ui.RootTextAccessoryViewHolder
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
 * Compose の `KsSettingsView(...)` の文字列の Root Header / Footer（`rootHeaderText` / `rootFooterText`）を検証する。
 *
 * 文字列は View Host の文字列の Root Header / Footer と同じ行（[RootTextAccessoryViewHolder]）で描かれ、
 * Theme の header / footer の文字色で表示されることを、実際に配置された行から確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComposeRootAccessoryTextTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val theme = Theme()

    private fun store() = SettingsRootStore(
        initialRoot = SettingsRoot(
            sections = listOf(Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "Hi")))),
        ),
        initialTheme = theme,
    )

    @Test
    fun `Store 方式の文字列の Root Header と Footer は View Host の文字列の行と同じ描画になる`() {
        composeRule.setContent {
            Column {
                KsSettingsView(
                    store = store(),
                    modifier = Modifier.weight(1f),
                    rootHeaderText = "ヘッダ文言",
                    rootFooterText = "フッタ文言",
                )
                ReferenceViewHost(header = "ヘッダ文言", footer = "フッタ文言", modifier = Modifier.weight(1f))
            }
        }
        val (compose, reference) = awaitLayouts(2)

        assertEquals(RootAccessory.Text("ヘッダ文言"), compose.rootHeader)
        assertEquals(RootAccessory.Text("フッタ文言"), compose.rootFooter)
        assertSameRendering(awaitRootTextRow(reference, "ヘッダ文言"), awaitRootTextRow(compose, "ヘッダ文言"))
        assertSameRendering(awaitRootTextRow(reference, "フッタ文言"), awaitRootTextRow(compose, "フッタ文言"))
    }

    @Test
    fun `DSL 方式の文字列の Root Footer は宣言の変更で書き換わり null で消える`() {
        var footerText by mutableStateOf<String?>("初期フッタ")
        composeRule.setContent {
            KsSettingsView(theme = theme, rootFooterText = footerText) {
                Section(header = "S") {
                    cell(LabelCell(id = "c1", title = "Hi"))
                }
            }
        }
        val layout = awaitLayout()
        awaitRootTextRow(layout, "初期フッタ")

        footerText = "変更後フッタ"
        awaitRootTextRow(layout, "変更後フッタ")
        assertEquals(RootAccessory.Text("変更後フッタ"), layout.rootFooter)

        footerText = null
        awaitCondition(
            "Root Footer が消える",
            observe = { layout.rootFooter to rootTextsOf(layout) },
            until = { (footer, texts) -> footer == null && texts.isEmpty() },
        )
    }

    @Test
    fun `DSL 方式の文字列の Root Header と Footer は View Host の文字列の行と同じ描画になる`() {
        composeRule.setContent {
            Column {
                KsSettingsView(
                    modifier = Modifier.weight(1f),
                    theme = theme,
                    rootHeaderText = "DSL ヘッダ",
                    rootFooterText = "DSL フッタ",
                ) {
                    Section(header = "S") {
                        cell(LabelCell(id = "c1", title = "Hi"))
                    }
                }
                ReferenceViewHost(header = "DSL ヘッダ", footer = "DSL フッタ", modifier = Modifier.weight(1f))
            }
        }
        val (compose, reference) = awaitLayouts(2)

        assertSameRendering(awaitRootTextRow(reference, "DSL ヘッダ"), awaitRootTextRow(compose, "DSL ヘッダ"))
        assertSameRendering(awaitRootTextRow(reference, "DSL フッタ"), awaitRootTextRow(compose, "DSL フッタ"))
    }

    @Test
    fun `Composable と文字列を両方渡すと Composable が表示される`() {
        composeRule.setContent {
            KsSettingsView(
                store = store(),
                rootHeader = { Text("Composable ヘッダ") },
                rootHeaderText = "文字列ヘッダ",
                rootFooter = { Text("Composable フッタ") },
                rootFooterText = "文字列フッタ",
            )
        }
        val layout = awaitLayout()

        assertTrue(layout.rootHeader is RootAccessory.View)
        assertTrue(layout.rootFooter is RootAccessory.View)
        // 一覧の差分反映は main looper へ後から届くため、Composable の Root Header / Footer の行が
        // 配置されるまで待ってから、文字列の行が無いことを確かめる。
        awaitCondition(
            "Composable の Root Header と Footer の行が配置される",
            observe = { findRootViewRows(layout).size },
            until = { it == 2 },
        )
        assertTrue("文字列の行は作られない", findRootTextRows(layout).isEmpty())
    }

    @Test
    fun `同じ文字列のまま他の引数が変わっても Root Footer を差し替えない`() {
        var style by mutableStateOf(KsSettingsViewStyle.Classic)
        val store = store()
        composeRule.setContent {
            // style の変更で AndroidView の update が走る（文字列は変えない）
            KsSettingsView(store = store, style = style, rootFooterText = "フッタ")
        }
        val layout = awaitLayout()
        awaitRootTextRow(layout, "フッタ")
        val before = layout.rootFooter

        style = KsSettingsViewStyle.Modern
        awaitCondition(
            "style の変更が Host に届く",
            observe = { layout.style },
            until = { it == KsSettingsViewStyle.Modern },
        )

        assertSame("同じ文字列では Host の値を差し替えない", before, layout.rootFooter)
    }

    @Test
    fun `文字列を渡さなければ Root Header と Footer は無い`() {
        composeRule.setContent {
            KsSettingsView(store = store())
        }
        val layout = awaitLayout()
        flushIdle()
        assertNull(layout.rootHeader)
        assertNull(layout.rootFooter)
    }

    // MARK: - ヘルパ

    /** 比較の基準にする View Host。Root Header / Footer を文字列で直接渡す。 */
    @Composable
    private fun ReferenceViewHost(header: String, footer: String, modifier: Modifier) {
        val referenceStore = remember { store() }
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                KsSettingsViewLayout(ctx).apply {
                    bind(referenceStore)
                    rootHeader = RootAccessory.Text(header)
                    rootFooter = RootAccessory.Text(footer)
                }
            },
        )
    }

    /** 文字の大きさ・色・書体・余白・背景が基準の行と一致することを確かめる。 */
    private fun assertSameRendering(expected: TextView, actual: TextView) {
        assertEquals("文字の大きさ", expected.textSize, actual.textSize, 0.0f)
        assertEquals("文字色", expected.currentTextColor, actual.currentTextColor)
        assertEquals("書体", expected.typeface, actual.typeface)
        assertEquals(
            "余白",
            listOf(expected.paddingLeft, expected.paddingTop, expected.paddingRight, expected.paddingBottom),
            listOf(actual.paddingLeft, actual.paddingTop, actual.paddingRight, actual.paddingBottom),
        )
        assertEquals(
            "背景色",
            (expected.background as? ColorDrawable)?.color,
            (actual.background as? ColorDrawable)?.color,
        )
        assertEquals("行の高さ", expected.height, actual.height)
    }

    private fun awaitLayouts(count: Int): List<KsSettingsViewLayout> {
        return awaitCondition(
            "KsSettingsViewLayout が $count 個配置される",
            observe = {
                mutableListOf<KsSettingsViewLayout>().also {
                    collectLayoutsIn(composeRule.activity.window.decorView as ViewGroup, it)
                }
            },
            describe = { "${it.size} 個" },
            until = { it.size == count },
        )
    }

    private fun collectLayoutsIn(parent: ViewGroup, out: MutableList<KsSettingsViewLayout>) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is KsSettingsViewLayout) {
                out.add(child)
            } else if (child is ViewGroup) {
                collectLayoutsIn(child, out)
            }
        }
    }

    private fun flushIdle() {
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }

    private fun awaitLayout(): KsSettingsViewLayout {
        val found = awaitCondition(
            "KsSettingsViewLayout が配置される",
            observe = { findLayoutIn(composeRule.activity.window.decorView as ViewGroup) },
            describe = { if (it == null) "未配置" else "配置済み" },
            until = { it != null },
        )
        return requireNotNull(found)
    }

    /** [text] を表示する文字列の Root Header / Footer の行が配置されるまで待ち、その TextView を返す。 */
    private fun awaitRootTextRow(layout: KsSettingsViewLayout, text: String): TextView {
        val rows = awaitCondition(
            "文字列の Root 行「$text」が配置される",
            observe = { findRootTextRows(layout) },
            describe = { rows -> "配置済みの文字列の Root 行 ${rows.map { it.text.toString() }}" },
            until = { rows -> rows.any { it.text.toString() == text } },
        )
        return rows.first { it.text.toString() == text }
    }

    /** 配置済みの行のうち、文字列の Root Header / Footer の行の TextView を列挙する。 */
    private fun findRootTextRows(layout: KsSettingsViewLayout): List<TextView> {
        val rv = (layout as FrameLayout).getChildAt(0) as RecyclerView
        return (0 until rv.childCount)
            .map { rv.getChildViewHolder(rv.getChildAt(it)) }
            .filterIsInstance<RootTextAccessoryViewHolder>()
            .map { it.itemView as TextView }
    }

    /** 配置済みの文字列の Root Header / Footer の行の文字列。 */
    private fun rootTextsOf(layout: KsSettingsViewLayout): List<String> =
        findRootTextRows(layout).map { it.text.toString() }

    /** 配置済みの行のうち、View（Composable を含む）の Root Header / Footer の行を列挙する。 */
    private fun findRootViewRows(layout: KsSettingsViewLayout): List<RootAnyViewAccessoryViewHolder> {
        val rv = (layout as FrameLayout).getChildAt(0) as RecyclerView
        return (0 until rv.childCount)
            .map { rv.getChildViewHolder(rv.getChildAt(it)) }
            .filterIsInstance<RootAnyViewAccessoryViewHolder>()
    }

    /**
     * [observe] で観測した値が [until] を満たすまで Compose と main looper を進めながら待ち、その値を返す。
     *
     * 一覧の差分反映はバックグラウンドで計算されて main looper へ post されるため、実時間の期限で待ち、
     * 期限を過ぎたら最後の観測値 ([describe] で文字列にしたもの) を載せて失敗させる。
     */
    private fun <T> awaitCondition(
        description: String,
        timeoutMillis: Long = 5_000,
        observe: () -> T,
        describe: (T) -> String = { it.toString() },
        until: (T) -> Boolean,
    ): T {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            val observed = observe()
            if (until(observed)) return observed
            if (System.nanoTime() >= deadline) {
                fail("$description まで $timeoutMillis ms 以内に到達しなかった（最後の観測値: ${describe(observed)}）")
            }
            Thread.sleep(1)
        }
    }

    private fun findLayoutIn(parent: ViewGroup): KsSettingsViewLayout? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is KsSettingsViewLayout) return child
            if (child is ViewGroup) {
                findLayoutIn(child)?.let { return it }
            }
        }
        return null
    }
}
