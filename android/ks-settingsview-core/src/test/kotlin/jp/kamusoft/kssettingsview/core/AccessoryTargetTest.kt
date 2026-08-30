package jp.kamusoft.kssettingsview.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AccessoryTarget の仕様検証。
 *
 * 4 ケース（Root H/F・Section H/F）が互いに区別され、`sectionId` を含めた等価性と
 * hashCode 契約を満たすことを確認する。
 */
class AccessoryTargetTest {

    @Test
    @DisplayName("RootHeader と RootFooter は不等")
    fun rootHeader_and_rootFooter_not_equal() {
        val a: AccessoryTarget = AccessoryTarget.RootHeader
        val b: AccessoryTarget = AccessoryTarget.RootFooter
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("RootHeader 同士は等価かつ同一ハッシュ")
    fun rootHeader_equality() {
        val a: AccessoryTarget = AccessoryTarget.RootHeader
        val b: AccessoryTarget = AccessoryTarget.RootHeader
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("RootFooter 同士は等価かつ同一ハッシュ")
    fun rootFooter_equality() {
        val a: AccessoryTarget = AccessoryTarget.RootFooter
        val b: AccessoryTarget = AccessoryTarget.RootFooter
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("SectionHeader: 同一 sectionId は等価かつ同一ハッシュ")
    fun sectionHeader_same_id_equal() {
        val a: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec1")
        val b: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec1")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("SectionHeader: 異なる sectionId は不等")
    fun sectionHeader_different_id_not_equal() {
        val a: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec1")
        val b: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec2")
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("SectionFooter: 同一 sectionId は等価")
    fun sectionFooter_same_id_equal() {
        val a: AccessoryTarget = AccessoryTarget.SectionFooter(sectionId = "sec1")
        val b: AccessoryTarget = AccessoryTarget.SectionFooter(sectionId = "sec1")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    @DisplayName("SectionHeader と SectionFooter は同一 sectionId でも不等")
    fun sectionHeader_vs_sectionFooter_not_equal() {
        val a: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec1")
        val b: AccessoryTarget = AccessoryTarget.SectionFooter(sectionId = "sec1")
        assertNotEquals(a, b)
    }

    @Test
    @DisplayName("RootHeader と SectionHeader は不等")
    fun rootHeader_vs_sectionHeader_not_equal() {
        val a: AccessoryTarget = AccessoryTarget.RootHeader
        val b: AccessoryTarget = AccessoryTarget.SectionHeader(sectionId = "sec1")
        assertNotEquals(a, b)
    }
}
