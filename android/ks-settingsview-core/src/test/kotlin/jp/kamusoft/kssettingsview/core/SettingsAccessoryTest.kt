package jp.kamusoft.kssettingsview.core

import androidx.compose.runtime.Composable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SettingsAccessory の仕様検証。
 *
 * Root / Section の 2 ケースが内部 Accessory の等価性を引き継ぎ、
 * 中身が同じでもケースが異なれば不等になることを確認する。
 */
class SettingsAccessoryTest {

    @Test
    @DisplayName("Root: 同一中身は等価かつ同一ハッシュ")
    fun root_same_inner_equal() {
        val a: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("X"))
        val b: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("X"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("Root: 異なる中身は不等")
    fun root_different_inner_not_equal() {
        val a: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("X"))
        val b: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("Y"))
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("Section: 同一中身は等価")
    fun section_same_inner_equal() {
        val a: SettingsAccessory = SettingsAccessory.Section(SectionAccessory.Text("X"))
        val b: SettingsAccessory = SettingsAccessory.Section(SectionAccessory.Text("X"))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("Root と Section は同一テキストでも不等")
    fun root_vs_section_not_equal() {
        val a: SettingsAccessory = SettingsAccessory.Root(RootAccessory.Text("X"))
        val b: SettingsAccessory = SettingsAccessory.Section(SectionAccessory.Text("X"))
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("Root.View 同士は中身無視で等価")
    fun root_view_inner_ignored() {
        // KsAnyView の中身は等価性に参加しない（RootAccessory.View の equals/hashCode 仕様）
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val a: SettingsAccessory = SettingsAccessory.Root(RootAccessory.View(KsAnyView.Compose(composable1)))
        val b: SettingsAccessory = SettingsAccessory.Root(RootAccessory.View(KsAnyView.Compose(composable2)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("Section.View 同士は中身無視で等価")
    fun section_view_inner_ignored() {
        val composable1: @Composable () -> Unit = {}
        val composable2: @Composable () -> Unit = {}
        val a: SettingsAccessory = SettingsAccessory.Section(SectionAccessory.View(KsAnyView.Compose(composable1)))
        val b: SettingsAccessory = SettingsAccessory.Section(SectionAccessory.View(KsAnyView.Compose(composable2)))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
