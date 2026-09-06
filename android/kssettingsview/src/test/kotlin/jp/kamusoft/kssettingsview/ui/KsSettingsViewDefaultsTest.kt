package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * ライブラリ既定色の light / dark 2 セットと、未指定色の解決の値検証。
 *
 * 生値は色ロール対応表を正とし、本テストが 2 セットの各フィールドを直接固定する。light セットは
 * ライブラリのライト外観の見た目そのものであり、ここが崩れると利用者のライト時の表示が変わる。
 *
 * セットが値を持つのは「解決済み Theme を直接読む描画箇所が使う色」10 個だけである。タイトルと
 * 説明文は factory でも解決でも埋めず、実効スタイルの最終段で外観から選ぶ（ここを埋めると
 * ButtonCell のタイトル解決が最終段へ到達しない）。
 */
class KsSettingsViewDefaultsTest {

    // MARK: - Theme() は全色が未指定

    @Test
    fun `引数なしの Theme は全ての色フィールドが未指定`() {
        val theme = Theme()
        val colors = listOf(
            "separatorColor" to theme.separatorColor,
            "backgroundColor" to theme.backgroundColor,
            "cellBackgroundColor" to theme.cellBackgroundColor,
            "selectedColor" to theme.selectedColor,
            "cellAccentColor" to theme.cellAccentColor,
            "disabledTextColor" to theme.disabledTextColor,
            "headerTextColor" to theme.headerTextColor,
            "headerBackgroundColor" to theme.headerBackgroundColor,
            "footerTextColor" to theme.footerTextColor,
            "footerBackgroundColor" to theme.footerBackgroundColor,
            "cellTitleColor" to theme.cellTitleColor,
            "cellValueTextColor" to theme.cellValueTextColor,
            "cellDescriptionColor" to theme.cellDescriptionColor,
            "cellHintTextColor" to theme.cellHintTextColor,
            "cellPlaceholderColor" to theme.cellPlaceholderColor,
            "sectionBorderColor" to theme.sectionBorderColor,
        )
        for ((name, value) in colors) {
            assertEquals("$name は未指定", Color.Unspecified, value)
        }
    }

    // MARK: - light セット

    @Test
    fun `lightTheme はライト外観の既定色を持つ`() {
        val theme = KsSettingsViewDefaults.lightTheme()
        assertEquals(Color(0xFFC8C7CC), theme.separatorColor)
        assertEquals(Color(0xFFFFFFFF), theme.backgroundColor)
        assertEquals(Color(0xFFFFFFFF), theme.cellBackgroundColor)
        assertEquals(Color(0xFFD9D9D9), theme.selectedColor)
        assertEquals(Color(0xFF007AFF), theme.cellAccentColor)
        assertEquals(Color(0xFF999999), theme.disabledTextColor)
        assertEquals(Color(0xFFF2F2F7), theme.headerBackgroundColor)
        assertEquals(Color(0xFF6D6D72), theme.headerTextColor)
        assertEquals(Color(0xFFF2F2F7), theme.footerBackgroundColor)
        assertEquals(Color(0xFF6D6D72), theme.footerTextColor)
    }

    // MARK: - dark セット

    @Test
    fun `darkTheme は色ロール対応表の dark 列と同値`() {
        val theme = KsSettingsViewDefaults.darkTheme()
        assertEquals(Color(0xFF38383A), theme.separatorColor)
        assertEquals(Color(0xFF000000), theme.backgroundColor)
        assertEquals(Color(0xFF1C1C1E), theme.cellBackgroundColor)
        assertEquals(Color(0xFF2C2C2E), theme.selectedColor)
        assertEquals(Color(0xFF0A84FF), theme.cellAccentColor)
        assertEquals(Color(0xFF636366), theme.disabledTextColor)
        assertEquals(Color(0xFF000000), theme.headerBackgroundColor)
        assertEquals(Color(0xFF8E8E93), theme.headerTextColor)
        assertEquals(Color(0xFF000000), theme.footerBackgroundColor)
        assertEquals(Color(0xFF8E8E93), theme.footerTextColor)
    }

    @Test
    fun `どちらのセットもタイトルと説明文を埋めず後段の解決へ委ねる`() {
        for (theme in listOf(KsSettingsViewDefaults.lightTheme(), KsSettingsViewDefaults.darkTheme())) {
            assertEquals(Color.Unspecified, theme.cellTitleColor)
            assertEquals(Color.Unspecified, theme.cellDescriptionColor)
        }
    }

    @Test
    fun `どちらのセットもフォールバック契約の色は未指定のまま`() {
        for (theme in listOf(KsSettingsViewDefaults.lightTheme(), KsSettingsViewDefaults.darkTheme())) {
            assertEquals(Color.Unspecified, theme.cellValueTextColor)
            assertEquals(Color.Unspecified, theme.cellHintTextColor)
            assertEquals(Color.Unspecified, theme.cellPlaceholderColor)
            assertEquals(Color.Unspecified, theme.sectionBorderColor)
        }
    }

    @Test
    fun `light と dark は同じロールで異なる値を持つ`() {
        val light = KsSettingsViewDefaults.lightTheme()
        val dark = KsSettingsViewDefaults.darkTheme()
        assertNotEquals(light.backgroundColor, dark.backgroundColor)
        assertNotEquals(light.cellBackgroundColor, dark.cellBackgroundColor)
        assertNotEquals(light.headerTextColor, dark.headerTextColor)
    }

    // MARK: - theme(darkTheme)

    @Test
    fun `theme は外観で light dark を選ぶ`() {
        assertEquals(KsSettingsViewDefaults.darkTheme(), KsSettingsViewDefaults.theme(darkTheme = true))
        assertEquals(KsSettingsViewDefaults.lightTheme(), KsSettingsViewDefaults.theme(darkTheme = false))
    }

    // MARK: - resolvedFor

    @Test
    fun `解決は未指定だけを埋め明示指定は素通しする`() {
        val explicitAccent = Color(0xFF12AB34)
        val resolved = Theme(cellAccentColor = explicitAccent).resolvedFor(darkTheme = true)
        assertEquals("明示した accent は解決で変わらない", explicitAccent, resolved.cellAccentColor)
        assertEquals(KsSettingsViewDefaults.darkTheme().backgroundColor, resolved.backgroundColor)
        assertEquals(KsSettingsViewDefaults.darkTheme().separatorColor, resolved.separatorColor)
    }

    @Test
    fun `解決はタイトルと説明文を埋めず後段の解決へ委ねる`() {
        val resolved = Theme().resolvedFor(darkTheme = true)
        assertEquals(Color.Unspecified, resolved.cellTitleColor)
        assertEquals(Color.Unspecified, resolved.cellDescriptionColor)
    }

    @Test
    fun `解決はフォールバック契約の色を未指定のまま残す`() {
        val resolved = Theme().resolvedFor(darkTheme = false)
        assertEquals(Color.Unspecified, resolved.cellValueTextColor)
        assertEquals(Color.Unspecified, resolved.cellHintTextColor)
        assertEquals(Color.Unspecified, resolved.cellPlaceholderColor)
        assertEquals(Color.Unspecified, resolved.sectionBorderColor)
    }

    @Test
    fun `Unspecified を明示的に渡すと既定へ戻る`() {
        val theme = Theme(cellAccentColor = Color(0xFF12AB34)).copy(cellAccentColor = Color.Unspecified)
        val resolved = theme.resolvedFor(darkTheme = true)
        assertEquals(KsSettingsViewDefaults.darkTheme().cellAccentColor, resolved.cellAccentColor)
    }

    @Test
    fun `既定と同じ生値を明示した Theme は既定 Theme と等価でなく外観で変わらない`() {
        val lightRawValue = KsSettingsViewDefaults.lightTheme().cellBackgroundColor
        val explicit = Theme(cellBackgroundColor = lightRawValue)
        assertNotEquals(Theme(), explicit)
        assertEquals(lightRawValue, explicit.resolvedFor(darkTheme = true).cellBackgroundColor)
    }

    // MARK: - valueText / title のフォールバック

    @Test
    fun `title を明示した Theme の valueText は title へフォールバックする`() {
        val explicitTitle = Color(0xFF335A99)
        val theme = Theme(cellTitleColor = explicitTitle).resolvedFor(darkTheme = true)
        val result = EffectiveStyle.effectiveValueTextColor(
            cellStyle = CellStyle(),
            theme = theme,
            darkTheme = true,
        )
        assertEquals(explicitTitle, result)
        assertNotEquals(KsThemePalette.Dark.cellTitle, result)
    }

    @Test
    fun `全段未指定の valueText は外観のタイトル既定へ落ちる`() {
        val theme = Theme().resolvedFor(darkTheme = true)
        assertEquals(
            KsThemePalette.Dark.cellTitle,
            EffectiveStyle.effectiveValueTextColor(CellStyle(), theme, darkTheme = true),
        )
    }

    // MARK: - hintText のフォールバック

    @Test
    fun `全段未指定の hintText は解決済み accent へ落ちる`() {
        val theme = Theme().resolvedFor(darkTheme = true)
        assertEquals(
            KsSettingsViewDefaults.darkTheme().cellAccentColor,
            EffectiveStyle.effectiveHintTextColor(CellStyle(), theme),
        )
    }
}
