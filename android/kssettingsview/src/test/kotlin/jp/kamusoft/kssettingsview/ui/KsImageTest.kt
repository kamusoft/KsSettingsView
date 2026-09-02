package jp.kamusoft.kssettingsview.ui

import android.graphics.drawable.ColorDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [KsImage]（UI 層）の派生・equals / hashCode 契約を検証する。
 *
 * [KsImage] は UI 層に置かれ、`Resource` / `Drawable` / `SystemName` の 3 派生を持つ（core/ADR-0009）。
 *
 * `ColorDrawable` を生成するため Robolectric を利用する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsImageTest {

    @Test
    fun `Resource 派生を構築できる`() {
        val img: KsImage = KsImage.Resource(resId = 1234)
        assertTrue(img is KsImage.Resource)
        assertEquals(1234, (img as KsImage.Resource).resId)
    }

    @Test
    fun `Drawable 派生を構築できる`() {
        val drawable = ColorDrawable(0)
        val img: KsImage = KsImage.Drawable(drawable)
        assertTrue(img is KsImage.Drawable)
        assertSame(drawable, (img as KsImage.Drawable).drawable)
    }

    @Test
    fun `SystemName 派生を構築できる`() {
        val img: KsImage = KsImage.SystemName("bell")
        assertTrue(img is KsImage.SystemName)
        assertEquals("bell", (img as KsImage.SystemName).name)
    }

    @Test
    fun `Resource は値同一性で等価`() {
        val a = KsImage.Resource(resId = 1234)
        val b = KsImage.Resource(resId = 1234)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `Resource の resId 違いは非等価`() {
        val a = KsImage.Resource(resId = 1234)
        val b = KsImage.Resource(resId = 5678)
        assertNotEquals(a, b)
    }

    @Test
    fun `Drawable は参照同一性で等価`() {
        val drawable = ColorDrawable(0)
        val a = KsImage.Drawable(drawable)
        val b = KsImage.Drawable(drawable)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `Drawable の別インスタンスは非等価（参照同一性）`() {
        val a = KsImage.Drawable(ColorDrawable(0))
        val b = KsImage.Drawable(ColorDrawable(0))
        assertNotEquals(a, b)
    }

    @Test
    fun `SystemName は値同一性で等価`() {
        val a = KsImage.SystemName("bell")
        val b = KsImage.SystemName("bell")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `SystemName の name 違いは非等価`() {
        val a = KsImage.SystemName("bell")
        val b = KsImage.SystemName("storage")
        assertNotEquals(a, b)
    }

    @Test
    fun `異なる派生は非等価`() {
        val a: KsImage = KsImage.Resource(resId = 1234)
        val b: KsImage = KsImage.SystemName("bell")
        assertNotEquals(a, b)
    }

    @Test
    fun `when 式で全派生を網羅できる`() {
        val cases: List<KsImage> = listOf(
            KsImage.Resource(resId = 1),
            KsImage.Drawable(ColorDrawable(0)),
            KsImage.SystemName("bell"),
        )
        val labels = cases.map { img ->
            when (img) {
                is KsImage.Resource -> "Resource(${img.resId})"
                is KsImage.Drawable -> "Drawable"
                is KsImage.SystemName -> "SystemName(${img.name})"
            }
        }
        assertEquals(3, labels.size)
        assertEquals("Resource(1)", labels[0])
        assertEquals("Drawable", labels[1])
        assertEquals("SystemName(bell)", labels[2])
    }
}
