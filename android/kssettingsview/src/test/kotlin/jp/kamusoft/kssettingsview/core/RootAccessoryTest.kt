package jp.kamusoft.kssettingsview.core

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * RootAccessory 型の仕様検証。
 *
 * 検証する性質:
 *   - sealed interface として Text / View 両ケースを構築でき、中身を取り出せる
 *   - 等価性は Text では文字列内容、View ではクラス一致のみで決まる（中身は無視）
 *   - SectionAccessory とは別型である
 */
class RootAccessoryTest {

    // -------- Text ケース --------

    @Test
    @DisplayName("Text: 構築とケース別取り出し")
    fun text_build_and_extract() {
        val accessory: RootAccessory = RootAccessory.Text("プロフィール")

        assertTrue(accessory is RootAccessory.Text)
        assertEquals("プロフィール", (accessory as RootAccessory.Text).value)
    }

    @Test
    @DisplayName("Text 等価性: 同じ文字列は等しい")
    fun text_equality_same_value_are_equal() {
        val a = RootAccessory.Text("h")
        val b = RootAccessory.Text("h")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("Text 等価性: 異なる文字列は等しくない")
    fun text_equality_different_value_are_not_equal() {
        val a = RootAccessory.Text("h1")
        val b = RootAccessory.Text("h2")
        assertNotEquals(a, b)
    }

    // -------- View ケース --------

    @Test
    @DisplayName("View: 構築")
    fun view_build() {
        val composable: @Composable () -> Unit = {}
        val accessory: RootAccessory = RootAccessory.View(KsAnyView.Compose(composable))

        assertTrue(accessory is RootAccessory.View)
    }

    @Test
    @DisplayName("View 等価性: 中身が違っても等しい（クラス一致のみで等価）")
    fun view_equality_ignore_inner_view() {
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val a: RootAccessory = RootAccessory.View(KsAnyView.Compose(composable1))
        val b: RootAccessory = RootAccessory.View(KsAnyView.Compose(composable2))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // -------- Text と View の区別 --------

    @Test
    @DisplayName("Text と View は別ケースとして区別される")
    fun text_and_view_are_distinct() {
        val composable: @Composable () -> Unit = {}
        val text: RootAccessory = RootAccessory.Text("h")
        val view: RootAccessory = RootAccessory.View(KsAnyView.Compose(composable))
        assertNotEquals(text, view)
    }

    // -------- SectionAccessory との別型保証 --------

    @Test
    @DisplayName("SectionAccessory との別型保証: 互いに代入互換性を持たない")
    fun root_and_section_accessory_are_distinct_types() {
        val root: RootAccessory = RootAccessory.Text("h")
        val section: SectionAccessory = SectionAccessory.Text("h")

        // 互いに別のインターフェースを実装しているため、互いに代入互換性を持たない
        assertTrue(root !is SectionAccessory)
        assertTrue(section !is RootAccessory)
        // 同じ "h" を表していても、型が異なるため等価ではない（型エラーで equals は呼べない想定だが
        // any 比較でも false になる）
        @Suppress("EqualsBetweenInconvertibleTypes")
        assertNotEquals(root as Any, section as Any)
    }

    // -------- HashSet 格納 --------

    @Test
    @DisplayName("HashSet に格納でき、ケース別に区別される")
    fun hashset_distinguishes_cases() {
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val set: Set<RootAccessory> = setOf(
            RootAccessory.Text("a"),
            RootAccessory.Text("a"), // 重複
            RootAccessory.Text("b"),
            RootAccessory.View(KsAnyView.Compose(composable1)),
            RootAccessory.View(KsAnyView.Compose(composable2)), // 中身違いだが View 同士は等価
        )
        // Text("a") / Text("b") / View(...) の 3 要素
        assertEquals(3, set.size)
    }
}
