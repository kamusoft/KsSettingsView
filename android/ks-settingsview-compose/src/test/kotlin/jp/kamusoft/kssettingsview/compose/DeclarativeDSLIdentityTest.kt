package jp.kamusoft.kssettingsview.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * DSL ID 採番ユーティリティ（`DSLIdentityId`）の安定性・優先順位を検証する。
 */
class DeclarativeDSLIdentityTest {

    @Test
    fun `同一ヒントから決定的IDが返る`() {
        val h = DSLIdentityHint.HeaderText(rootIdx = 1, text = "Hello")
        assertEquals(DSLIdentityId.id(from = h), DSLIdentityId.id(from = h))
    }

    @Test
    fun `異なるヒントは異なるID`() {
        val h1 = DSLIdentityHint.HeaderText(rootIdx = 0, text = "A")
        val h2 = DSLIdentityHint.HeaderText(rootIdx = 1, text = "A")
        val h3 = DSLIdentityHint.HeaderText(rootIdx = 0, text = "B")
        assertNotEquals(DSLIdentityId.id(from = h1), DSLIdentityId.id(from = h2))
        assertNotEquals(DSLIdentityId.id(from = h1), DSLIdentityId.id(from = h3))
        assertNotEquals(DSLIdentityId.id(from = h2), DSLIdentityId.id(from = h3))
    }

    @Test
    fun `異なるケースは異なるID`() {
        val h1 = DSLIdentityHint.RootPosition(rootIdx = 0)
        val h2 = DSLIdentityHint.HeaderText(rootIdx = 0, text = "")
        assertNotEquals(DSLIdentityId.id(from = h1), DSLIdentityId.id(from = h2))
    }

    @Test
    fun `explicit String ヒントは決定的`() {
        val h = DSLIdentityHint.Explicit("dynamic-1")
        assertEquals(DSLIdentityId.id(from = h), DSLIdentityId.id(from = h))
    }

    @Test
    fun `forEach Int ヒントは決定的`() {
        val h = DSLIdentityHint.ForEach(42)
        assertEquals(DSLIdentityId.id(from = h), DSLIdentityId.id(from = h))
    }

    @Test
    fun `positional ヒント型違いは異なるID`() {
        val h1 = DSLIdentityHint.Positional(sectionId = "S", indexInSection = 0, cellType = "Foo")
        val h2 = DSLIdentityHint.Positional(sectionId = "S", indexInSection = 0, cellType = "Bar")
        assertNotEquals(DSLIdentityId.id(from = h1), DSLIdentityId.id(from = h2))
    }

    @Test
    fun `123 Int と 123 String は異なるID`() {
        val h1 = DSLIdentityHint.ForEach(123)
        val h2 = DSLIdentityHint.ForEach("123")
        assertNotEquals(DSLIdentityId.id(from = h1), DSLIdentityId.id(from = h2))
    }
}
