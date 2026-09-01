package jp.kamusoft.kssettingsview.core

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SectionAccessory 型の仕様検証。
 *
 * 検証する性質:
 *   - sealed interface として Text / View 両ケースを構築でき、中身を取り出せる
 *   - 等価性は Text では文字列内容、View ではクラス一致のみで決まる（中身は無視）
 *   - HashSet に格納できる
 */
class SectionAccessoryTest {

    // -------- Text ケース --------

    @Test
    @DisplayName("Text: 構築とケース別取り出し")
    fun text_build_and_extract() {
        val accessory: SectionAccessory = SectionAccessory.Text("一般")

        assertTrue(accessory is SectionAccessory.Text)
        assertEquals("一般", (accessory as SectionAccessory.Text).value)
    }

    @Test
    @DisplayName("Text 等価性: 同じ文字列は等しい")
    fun text_equality_same_value_are_equal() {
        val a = SectionAccessory.Text("h")
        val b = SectionAccessory.Text("h")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("Text 等価性: 異なる文字列は等しくない")
    fun text_equality_different_value_are_not_equal() {
        val a = SectionAccessory.Text("h1")
        val b = SectionAccessory.Text("h2")
        assertNotEquals(a, b)
    }

    // -------- View ケース --------

    @Test
    @DisplayName("View: 構築")
    fun view_build() {
        val composable: @Composable () -> Unit = {}
        val accessory: SectionAccessory = SectionAccessory.View(KsAnyView.Compose(composable))

        assertTrue(accessory is SectionAccessory.View)
    }

    @Test
    @DisplayName("View 等価性: 中身が違っても等しい（クラス一致のみで等価）")
    fun view_equality_ignore_inner_view() {
        // 中身の違う KsAnyView を持つ 2 つの .View ケース
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val a: SectionAccessory = SectionAccessory.View(KsAnyView.Compose(composable1))
        val b: SectionAccessory = SectionAccessory.View(KsAnyView.Compose(composable2))
        // KsAnyView は等価性に参加しないため、ケース一致のみで等価
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // -------- Text と View の区別 --------

    @Test
    @DisplayName("Text と View は別ケースとして区別される")
    fun text_and_view_are_distinct() {
        val composable: @Composable () -> Unit = {}
        val text: SectionAccessory = SectionAccessory.Text("h")
        val view: SectionAccessory = SectionAccessory.View(KsAnyView.Compose(composable))
        assertNotEquals(text, view)
    }

    // -------- HashSet 格納 --------

    @Test
    @DisplayName("HashSet に格納でき、ケース別に区別される")
    fun hashset_distinguishes_cases() {
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val set: Set<SectionAccessory> = setOf(
            SectionAccessory.Text("a"),
            SectionAccessory.Text("a"), // 重複
            SectionAccessory.Text("b"),
            SectionAccessory.View(KsAnyView.Compose(composable1)),
            SectionAccessory.View(KsAnyView.Compose(composable2)), // 中身違いだが View 同士は等価
        )
        // Text("a") / Text("b") / View(...) の 3 要素
        assertEquals(3, set.size)
    }
}
