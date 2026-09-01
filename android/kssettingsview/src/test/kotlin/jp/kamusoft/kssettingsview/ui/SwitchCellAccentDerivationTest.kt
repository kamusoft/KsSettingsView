package jp.kamusoft.kssettingsview.ui

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.math.min

/**
 * [SwitchCell] の Switch 色が **実効 accent から導出** されていることを検証する。
 *
 * オフ色は「accent の気配は残るが、オンとは明確に区別できる」導出で、オン色は accent と
 * そのコントラスト色で決まる。テーマの `colorOutline` / `colorSurfaceContainerHighest` /
 * `colorOnPrimary` をそのまま使う実装や、accent に依存しない固定色へ退行しても
 * 「オン色 != オフ色」「Track 色 != Thumb 色」だけを見るテストは通ってしまうため、ここでは
 *
 * - オフ色がテーマ attr の直値と一致しないこと
 * - accent を変えるとオフ色も変わり、色相が accent へ追従すること
 * - ダークテーマでも Thumb と Track の明度差が残ること
 * - オフ時のボーダー（`trackDecoration`）が Thumb と同色であること
 * - オン時は accent がそのまま Track に出ること
 * - オン Thumb がテーマに依存せず（ライト／ダークで同色）、明色 accent では暗色へ倒れること
 *
 * を検出する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SwitchCellAccentDerivationTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val unchecked = intArrayOf(-android.R.attr.state_checked)
    private val checked = intArrayOf(android.R.attr.state_checked)

    /**
     * 端末を夜間モードへ切り替える。
     *
     * ライブラリ UI は同梱の DayNight テーマで解決するため（android/ADR-0020）、暗い側の値を得る
     * 条件は端末の夜間モードであり、ホストが選んだテーマではない。
     */
    private fun switchToNightMode() {
        RuntimeEnvironment.setQualifiers("+night")
    }

    /** [accent] を実効 accent として bind した Switch を返す。 */
    private fun bindSwitch(
        accent: Color,
        isOn: Boolean = false,
        context: android.content.Context = ctx,
    ): MaterialSwitch {
        val vh = SwitchCellViewHolder.create(FrameLayout(context))
        vh.bind(
            SwitchCell(title = "通知", isOn = isOn),
            Theme(cellAccentColor = accent),
        )
        val sw = findMaterialSwitch(vh.itemView as ViewGroup)
        assertNotNull("MaterialSwitch が見つからない", sw)
        return sw!!
    }

    private fun findMaterialSwitch(root: ViewGroup): MaterialSwitch? {
        for (index in 0 until root.childCount) {
            when (val child: View = root.getChildAt(index)) {
                is MaterialSwitch -> return child
                is ViewGroup -> findMaterialSwitch(child)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    private fun hueOf(color: Int): Float {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl[0]
    }

    /** 色相環上の最短距離（0〜180 度）。 */
    private fun hueDistance(a: Float, b: Float): Float {
        val diff = abs(a - b) % 360.0f
        return min(diff, 360.0f - diff)
    }

    // MARK: - テーマ attr の直値ではないこと

    @Test
    fun `オフ状態の Track と Thumb はテーマ attr の直値ではない`() {
        val sw = bindSwitch(Color(1.0f, 0.5f, 0.0f, 1.0f))
        val surfaceContainerHighest = MaterialColors.getColor(
            sw,
            MaterialR.attr.colorSurfaceContainerHighest,
            android.graphics.Color.LTGRAY,
        )
        val outline = MaterialColors.getColor(sw, MaterialR.attr.colorOutline, android.graphics.Color.GRAY)

        assertNotEquals(
            "オフ Track が colorSurfaceContainerHighest の直値になっている（accent 由来の導出ではない）",
            surfaceContainerHighest,
            sw.trackTintList!!.getColorForState(unchecked, 0),
        )
        assertNotEquals(
            "オフ Thumb が colorOutline の直値になっている（accent 由来の導出ではない）",
            outline,
            sw.thumbTintList!!.getColorForState(unchecked, 0),
        )
    }

    // MARK: - accent への追従

    @Test
    fun `accent を変えるとオフ状態の Track と Thumb も変わる`() {
        val red = bindSwitch(Color(0xFFD3.toFloat() / 255f, 0x2F / 255f, 0x2F / 255f, 1.0f))
        val blue = bindSwitch(Color(0x19 / 255f, 0x76 / 255f, 0xD2 / 255f, 1.0f))

        assertNotEquals(
            "accent が違ってもオフ Track が同じ色になっている（accent に追従していない）",
            red.trackTintList!!.getColorForState(unchecked, 0),
            blue.trackTintList!!.getColorForState(unchecked, 0),
        )
        assertNotEquals(
            "accent が違ってもオフ Thumb が同じ色になっている（accent に追従していない）",
            red.thumbTintList!!.getColorForState(unchecked, 0),
            blue.thumbTintList!!.getColorForState(unchecked, 0),
        )
    }

    @Test
    fun `オフ状態の Track の色相は accent の色相に追従する`() {
        val accent = Color(0x19 / 255f, 0x76 / 255f, 0xD2 / 255f, 1.0f)
        val sw = bindSwitch(accent)
        val offTrackHue = hueOf(sw.trackTintList!!.getColorForState(unchecked, 0))

        // 減彩しても色相そのものは保たれる（下地との blend でわずかにずれる分だけ許容する）。
        assertTrue(
            "オフ Track の色相が accent の色相から離れすぎている",
            hueDistance(offTrackHue, hueOf(accent.toArgb())) < 30.0f,
        )
    }

    // MARK: - ダークテーマでの明度関係

    /** 明度（HSL の L）。 */
    private fun lightnessOf(color: Int): Float {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        return hsl[2]
    }

    /**
     * 夜間モードでも Track と Thumb の明度差が潰れないことを検証する。
     *
     * オフ色の明度をライト実測ベースの固定係数で作ると、暗い側では土台が暗いため両者とも
     * 黒に張り付いて区別が付かなくなる。明度をテーマ attr から取る導出であれば、素の
     * `MaterialSwitch` と同じく attr の反転に追従して Thumb が Track より明るくなる。
     */
    @Test
    fun `ダークテーマでも Thumb は Track より明るく明度差が残る`() {
        switchToNightMode()
        val sw = bindSwitch(Color(1.0f, 0.5f, 0.0f, 1.0f))
        val trackLightness = lightnessOf(sw.trackTintList!!.getColorForState(unchecked, 0))
        val thumbLightness = lightnessOf(sw.thumbTintList!!.getColorForState(unchecked, 0))

        assertTrue(
            "ダークテーマで Thumb が Track より明るくない（attr の反転に追従していない）: " +
                "track=$trackLightness thumb=$thumbLightness",
            thumbLightness > trackLightness,
        )
        assertTrue(
            "ダークテーマで Track と Thumb の明度差が潰れている: " +
                "track=$trackLightness thumb=$thumbLightness",
            (thumbLightness - trackLightness) > 0.15f,
        )
    }

    // MARK: - ボーダー（trackDecoration）

    @Test
    fun `オフ状態のボーダーは Thumb と同色でオン状態では透明になる`() {
        val sw = bindSwitch(Color(1.0f, 0.5f, 0.0f, 1.0f))
        val decoration = sw.trackDecorationTintList
        assertNotNull("trackDecorationTintList が設定されていない", decoration)

        assertEquals(
            "オフ時のボーダー色は Thumb と同色でなければならない",
            sw.thumbTintList!!.getColorForState(unchecked, 0),
            decoration!!.getColorForState(unchecked, 0),
        )
        assertEquals(
            "オン時のボーダーは透明でなければならない（Material 3 既定と同じ見た目を保つ）",
            android.graphics.Color.TRANSPARENT,
            decoration.getColorForState(checked, 0),
        )
    }

    // MARK: - オン状態は accent そのもの

    @Test
    fun `オン状態の Track には実効 accent がそのまま出る`() {
        val accent = Color(1.0f, 0.5f, 0.0f, 1.0f)
        val sw = bindSwitch(accent, isOn = true)

        assertEquals(
            "オン時の Track は accent そのものでなければならない",
            accent.toArgb(),
            sw.trackTintList!!.getColorForState(checked, 0),
        )
        assertEquals(
            "通常トーンの accent ではオン時の Thumb は白でなければならない",
            android.graphics.Color.WHITE,
            sw.thumbTintList!!.getColorForState(checked, 0),
        )
    }

    /**
     * オン thumb が明暗の切り替えに依存しないことを検証する。
     *
     * `colorOnPrimary` を参照すると、夜間モードではテーマ primary の暗トーン（紫青系）に
     * 解決されて track（accent 由来）と調和しない。オン thumb は accent に対するコントラスト色
     * として決まるため、明暗どちらでも同じ色でなければならない。
     */
    @Test
    fun `オン状態の Thumb はテーマが変わっても同じ色になる`() {
        val accent = Color(1.0f, 0.5f, 0.0f, 1.0f)
        val light = bindSwitch(accent, isOn = true)
        val lightThumb = light.thumbTintList!!.getColorForState(checked, 0)

        switchToNightMode()
        val dark = bindSwitch(accent, isOn = true)
        val darkThumb = dark.thumbTintList!!.getColorForState(checked, 0)
        assertEquals(
            "ダークテーマでオン Thumb がテーマ色に引きずられている（accent 基準になっていない）",
            lightThumb,
            darkThumb,
        )
        assertNotEquals(
            "オン Thumb がダークテーマの colorOnPrimary と一致している（テーマ漏れ）",
            MaterialColors.getColor(dark, MaterialR.attr.colorOnPrimary, android.graphics.Color.WHITE),
            darkThumb,
        )
    }

    @Test
    fun `明るい accent ではオン Thumb が暗色へ倒れて視認性を確保する`() {
        // 白では沈む明色 accent（淡い黄）。
        val paleAccent = Color(1.0f, 0.94f, 0.6f, 1.0f)
        val sw = bindSwitch(paleAccent, isOn = true)
        val onThumb = sw.thumbTintList!!.getColorForState(checked, 0)

        assertNotEquals(
            "明色 accent でもオン Thumb が白のままで沈んでいる",
            android.graphics.Color.WHITE,
            onThumb,
        )
        val paleOpaque = ColorUtils.setAlphaComponent(paleAccent.toArgb(), 255)
        assertTrue(
            "明色 accent に対するオン Thumb のコントラストが白のときより改善していない",
            ColorUtils.calculateContrast(onThumb, paleOpaque) >
                ColorUtils.calculateContrast(android.graphics.Color.WHITE, paleOpaque),
        )
    }

    @Test
    fun `Cell 個別の accentColor がオフ色の導出にも効く`() {
        val themeAccent = Color(1.0f, 0.5f, 0.0f, 1.0f)
        val cellAccent = Color(0x19 / 255f, 0x76 / 255f, 0xD2 / 255f, 1.0f)

        val vh = SwitchCellViewHolder.create(FrameLayout(ctx))
        vh.bind(
            SwitchCell(title = "通知", isOn = false, accentColor = cellAccent),
            Theme(cellAccentColor = themeAccent),
        )
        val sw = findMaterialSwitch(vh.itemView as ViewGroup)!!
        val offTrackHue = hueOf(sw.trackTintList!!.getColorForState(unchecked, 0))

        assertTrue(
            "Cell 個別の accentColor がオフ色の導出に効いていない",
            hueDistance(offTrackHue, hueOf(cellAccent.toArgb())) <
                hueDistance(offTrackHue, hueOf(themeAccent.toArgb())),
        )
    }
}
