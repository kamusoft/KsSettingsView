package jp.kamusoft.kssettingsview.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DSL marker `SettingsRootDsl` の付与先の検証。
 *
 * marker のスコープ制御は receiver の型に付与された注釈だけに由来するため、注釈の許容ターゲットが
 * 型に限定されていることと、DSL の receiver 型すべてに付与されていることを固定する。
 */
class SettingsRootDslMarkerTest {

    @Test
    fun `marker 注釈の許容ターゲットは型に限定されている`() {
        val target = SettingsRootDsl::class.java.getAnnotation(Target::class.java)
        assertNotNull("SettingsRootDsl に @Target が付与されていません", target)
        // 許容ターゲットは集合として一致すればよく、宣言の並び順は契約に含めない
        assertEquals(
            setOf(
                AnnotationTarget.CLASS,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPEALIAS,
            ),
            target!!.allowedTargets.toSet(),
        )
    }

    @Test
    fun `DSL の receiver 型すべてに marker が付与されている`() {
        val receiverTypes = listOf(
            SettingsRootScope::class.java,
            SectionScope::class.java,
            DSLSettingsRootScope::class.java,
            DSLSectionScope::class.java,
            SectionHandle::class.java,
            CellHandle::class.java,
        )
        for (type in receiverTypes) {
            assertTrue(
                "${type.simpleName} に SettingsRootDsl が付与されていません",
                type.isAnnotationPresent(SettingsRootDsl::class.java),
            )
        }
    }
}
