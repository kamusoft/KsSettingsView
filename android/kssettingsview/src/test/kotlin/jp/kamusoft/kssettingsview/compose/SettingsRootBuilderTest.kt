package jp.kamusoft.kssettingsview.compose

import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.LabelCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `settingsRoot { ... }` DSL の構築結果検証。
 *
 * `settingsRoot` は Store 初期値の `SettingsRoot` を組み立てる純粋関数であり、`theme` 引数を
 * 持たない。Theme は UI 層の関心のため（core/ADR-0009）、`KsSettingsView(theme = ...)` または
 * `SettingsRootStore(initialTheme = ...)` 経路で渡す。
 */
class SettingsRootBuilderTest {

    @Test
    fun `空の settingsRoot は SettingsRoot の既定値を返す`() {
        val root = settingsRoot { /* 空 */ }
        assertEquals(0, root.sections.size)
    }

    @Test
    fun `section ヘルパで Section が 1 つ追加される（文字列ヘッダ版）`() {
        val root = settingsRoot {
            section(id = "s1", header = "一般") {
                cell(LabelCell(id = "c1", title = "ニックネーム"))
                cell(LabelCell(id = "c2", title = "メールアドレス"))
            }
        }
        assertEquals(1, root.sections.size)
        val section = root.sections[0]
        assertEquals("s1", section.id)
        assertNotNull(section.header)
        assertEquals(SectionAccessory.Text("一般"), section.header)
        assertEquals(2, section.cells.size)
    }

    @Test
    fun `複数の section を順次追加できる`() {
        val root = settingsRoot {
            section(id = "s1", header = "一般") {
                cell(LabelCell(id = "c1", title = "A"))
            }
            section(id = "s2", header = "詳細") {
                cell(LabelCell(id = "c2", title = "B"))
            }
        }
        assertEquals(2, root.sections.size)
        assertEquals("s1", root.sections[0].id)
        assertEquals("s2", root.sections[1].id)
    }

    @Test
    fun `headerHeight を指定すると Section へ転写される`() {
        val root = settingsRoot {
            section(
                id = "s1",
                header = SectionAccessory.Text("一般"),
                headerHeight = 40.0,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        assertEquals(40.0, root.sections[0].headerHeight, 0.0)
    }

    @Test
    fun `Header と Footer の表示トグルを指定しても内容は保持される（accessory 版）`() {
        val root = settingsRoot {
            section(
                id = "s1",
                header = SectionAccessory.Text("一般"),
                footer = SectionAccessory.Text("補足"),
                isHeaderVisible = false,
                isFooterVisible = false,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        val section = root.sections[0]
        assertFalse(section.isHeaderVisible)
        assertFalse(section.isFooterVisible)
        assertEquals(SectionAccessory.Text("一般"), section.header)
        assertEquals(SectionAccessory.Text("補足"), section.footer)
    }

    @Test
    fun `文字列ヘッダ版でも同じ属性を指定できる`() {
        val root = settingsRoot {
            section(
                id = "s1",
                header = "一般",
                footer = "補足",
                headerHeight = 40.0,
                isVisible = false,
                isHeaderVisible = false,
                isFooterVisible = false,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        val section = root.sections[0]
        assertEquals(SectionAccessory.Text("一般"), section.header)
        assertEquals(SectionAccessory.Text("補足"), section.footer)
        assertEquals(40.0, section.headerHeight, 0.0)
        assertFalse(section.isVisible)
        assertFalse(section.isHeaderVisible)
        assertFalse(section.isFooterVisible)
    }

    @Test
    fun `文字列ヘッダ版で footer を省略すると footer は無い`() {
        val root = settingsRoot {
            section(id = "s1", header = "一般") {
                cell(LabelCell(id = "c1", title = "A"))
            }
        }
        assertNull(root.sections[0].footer)
    }

    @Test
    fun `新引数を省略した section は Section の既定値で構築した Section と等価`() {
        val labelCell = LabelCell(id = "c1", title = "A")
        val root = settingsRoot {
            section(id = "s1", header = SectionAccessory.Text("A")) {
                cell(labelCell)
            }
        }
        assertEquals(
            Section(
                id = "s1",
                header = SectionAccessory.Text("A"),
                cells = listOf(labelCell),
            ),
            root.sections[0],
        )
    }

    @Test
    fun `位置引数で規定の並びどおりに呼び出せる（accessory 版）`() {
        val root = settingsRoot {
            section(
                "s1",
                SectionAccessory.Text("H"),
                SectionAccessory.Text("F"),
                40.0,
                false,
                true,
                true,
            ) {
                cell(LabelCell(id = "c1", title = "A"))
            }
            section(
                "s2",
                SectionAccessory.Text("H"),
                SectionAccessory.Text("F"),
                40.0,
                true,
                false,
                true,
            ) {
                cell(LabelCell(id = "c2", title = "B"))
            }
            section(
                "s3",
                SectionAccessory.Text("H"),
                SectionAccessory.Text("F"),
                40.0,
                true,
                true,
                false,
            ) {
                cell(LabelCell(id = "c3", title = "C"))
            }
        }
        assertEquals(3, root.sections.size)
        assertPositionalSection(
            root.sections[0],
            isVisible = false,
            isHeaderVisible = true,
            isFooterVisible = true,
        )
        assertPositionalSection(
            root.sections[1],
            isVisible = true,
            isHeaderVisible = false,
            isFooterVisible = true,
        )
        assertPositionalSection(
            root.sections[2],
            isVisible = true,
            isHeaderVisible = true,
            isFooterVisible = false,
        )
    }

    @Test
    fun `位置引数で規定の並びどおりに呼び出せる（文字列ヘッダ版）`() {
        val root = settingsRoot {
            section("s1", "H", "F", 40.0, false, true, true) {
                cell(LabelCell(id = "c1", title = "A"))
            }
            section("s2", "H", "F", 40.0, true, false, true) {
                cell(LabelCell(id = "c2", title = "B"))
            }
            section("s3", "H", "F", 40.0, true, true, false) {
                cell(LabelCell(id = "c3", title = "C"))
            }
        }
        assertEquals(3, root.sections.size)
        assertPositionalSection(
            root.sections[0],
            isVisible = false,
            isHeaderVisible = true,
            isFooterVisible = true,
        )
        assertPositionalSection(
            root.sections[1],
            isVisible = true,
            isHeaderVisible = false,
            isFooterVisible = true,
        )
        assertPositionalSection(
            root.sections[2],
            isVisible = true,
            isHeaderVisible = true,
            isFooterVisible = false,
        )
    }

    /**
     * 位置引数で構築した Section が、規定の位置の値をそのまま持つことを検証する。
     *
     * 3 つの Boolean 引数は同じ型のため、すべて同じ値で呼ぶと並び順が入れ替わっても気づけない。
     * 呼び出し側では 1 つだけ `false` にした 3 通りを渡し、ここで各フィールドを個別に照合することで
     * Boolean 引数の相対順序まで固定する。
     */
    private fun assertPositionalSection(
        section: Section,
        isVisible: Boolean,
        isHeaderVisible: Boolean,
        isFooterVisible: Boolean,
    ) {
        assertEquals(SectionAccessory.Text("H"), section.header)
        assertEquals(SectionAccessory.Text("F"), section.footer)
        assertEquals(40.0, section.headerHeight, 0.0)
        assertEquals("isVisible", isVisible, section.isVisible)
        assertEquals("isHeaderVisible", isHeaderVisible, section.isHeaderVisible)
        assertEquals("isFooterVisible", isFooterVisible, section.isFooterVisible)
    }
}
