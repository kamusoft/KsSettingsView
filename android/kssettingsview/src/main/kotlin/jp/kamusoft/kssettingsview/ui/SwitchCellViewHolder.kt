package jp.kamusoft.kssettingsview.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * オフ track の彩度（絶対値）。
 *
 * 素の `MaterialSwitch` のオフ track（`colorSurfaceContainerHighest` の直値）とほぼ同じ淡さに、
 * accent の色相の気配だけを載せる強さ。
 */
private const val OFF_TRACK_SATURATION = 0.09f

/**
 * オフ thumb の彩度（絶対値）。
 *
 * accent の色相がかすかに分かる「色みを帯びた灰色」になる強さ（素の `colorOutline` と同程度）。
 */
private const val OFF_THUMB_SATURATION = 0.04f

/** オフ track の明度: 土台となる attr の明度に対する微調整比（1.0 = attr のまま）。 */
private const val OFF_TRACK_LIGHTNESS_RATIO = 1.0f

/** オフ thumb の明度: 土台となる attr の明度に対する微調整比（1.0 = attr のまま）。 */
private const val OFF_THUMB_LIGHTNESS_RATIO = 0.92f

/**
 * 土台色の明度を保ったまま、accent の色相を指定の彩度で載せた色を作る。
 *
 * 素の `MaterialSwitch` はオフ色をテーマ attr（`colorOutline` / `colorSurfaceContainerHighest`）
 * から取るため、ダークテーマでは attr 自体が反転して thumb と track の明度関係が保たれる。
 * ここでも **明度は [base] の attr から取り、色相と彩度だけを accent 由来にする** ことで、
 * ライトでの見た目を保ったままダークの反転へ自動追従させる（android/ADR-0017）。
 *
 * `HSLToColor` は常に不透明色を返すため、[accent] の alpha を明示的に載せ直す。
 *
 * @param base 明度の土台にするテーマ attr の色
 * @param accent 色相の供給元となる実効 accent
 * @param saturation 出力の彩度（絶対値）
 * @param lightnessRatio [base] の明度に掛ける微調整比
 */
private fun tintedFrom(base: Int, accent: Int, saturation: Float, lightnessRatio: Float): Int {
    val accentHsl = FloatArray(3)
    ColorUtils.colorToHSL(accent, accentHsl)
    val baseHsl = FloatArray(3)
    ColorUtils.colorToHSL(base, baseHsl)
    val out = floatArrayOf(
        accentHsl[0],
        saturation.coerceIn(0.0f, 1.0f),
        (baseHsl[2] * lightnessRatio).coerceIn(0.0f, 1.0f),
    )
    return ColorUtils.setAlphaComponent(ColorUtils.HSLToColor(out), Color.alpha(accent))
}

/**
 * オフ状態の track 色を accent から導出する。
 *
 * 明度は [surface]（`colorSurfaceContainerHighest`）から取り、accent の色相を
 * [OFF_TRACK_SATURATION] の淡さで載せる。オン状態（accent そのもの）とは明確に区別できる。
 */
private fun offTrackColorFrom(accent: Int, surface: Int): Int =
    tintedFrom(surface, accent, OFF_TRACK_SATURATION, OFF_TRACK_LIGHTNESS_RATIO)

/**
 * オフ状態の thumb 色を accent から導出する。
 *
 * 明度は [outline]（`colorOutline`）から取り、accent の色相を [OFF_THUMB_SATURATION] の
 * 淡さで載せる。track（[offTrackColorFrom]）とは attr 由来の明度差でつまみの位置が読め、
 * accent そのものよりは鈍いのでオンとは取り違えない。
 */
private fun offThumbColorFrom(accent: Int, outline: Int): Int =
    tintedFrom(outline, accent, OFF_THUMB_SATURATION, OFF_THUMB_LIGHTNESS_RATIO)

/**
 * オン thumb を白のままにしてよい、accent に対する白のコントラスト比の下限。
 *
 * つまみは文字ではなく面積のある図形なので、素の Material 3 も彩度の高い primary の上に
 * 白い thumb を置く（例: 青 #007AFF で 4.0、緑・橙・青緑では 2.2〜2.5）。ここでの目的は
 * WCAG の文字基準を満たすことではなく「白がほぼ沈む明色 accent」だけを弾くことなので、
 * 一般的な accent が白のままになる水準に置く。
 */
private const val ON_THUMB_MIN_CONTRAST = 1.5

/** 明色 accent で使うオン thumb の彩度（accent の色相をかすかに残した暗色）。 */
private const val ON_THUMB_DARK_SATURATION = 0.10f

/** 明色 accent で使うオン thumb の明度。 */
private const val ON_THUMB_DARK_LIGHTNESS = 0.15f

/**
 * オン状態の thumb 色を accent から導出する。
 *
 * 素の `MaterialSwitch` は `track = colorPrimary` / `thumb = colorOnPrimary` の関係で塗るが、
 * 本 Cell の track はテーマではなく実効 accent なので、thumb だけ `colorOnPrimary` を参照すると
 * ダークテーマでテーマ由来の暗色（紫青系）が漏れて track と調和しない。ここでは同じ関係を
 * **accent 基準**で作り直し、accent の上で読める側のコントラスト色を選ぶ。
 *
 * 通常の accent（中〜暗トーン）では白になり、ライト／ダークどちらでも従来どおりの白い
 * つまみになる。accent が明るく白では沈む場合（[ON_THUMB_MIN_CONTRAST] 未満）だけ、
 * accent の色相を残した暗色へ倒して視認性を確保する（android/ADR-0017）。
 */
private fun onThumbColorFrom(accent: Int): Int {
    val opaqueAccent = ColorUtils.setAlphaComponent(accent, 255)
    if (ColorUtils.calculateContrast(Color.WHITE, opaqueAccent) >= ON_THUMB_MIN_CONTRAST) {
        return Color.WHITE
    }
    val accentHsl = FloatArray(3)
    ColorUtils.colorToHSL(accent, accentHsl)
    return ColorUtils.HSLToColor(
        floatArrayOf(accentHsl[0], ON_THUMB_DARK_SATURATION, ON_THUMB_DARK_LIGHTNESS),
    )
}

/**
 * [SwitchCell] 描画用 ViewHolder。
 *
 * [CellBaseViews] の `accessoryHolder` に [MaterialSwitch] を配置する。
 * `bind` 内で [applyCellBaseLayout] を呼び出して共通フィールドを描画し、
 * `setOnCheckedChangeListener` を毎回設定する（共通行の描画は core/ADR-0011 に従う）。
 */
internal class SwitchCellViewHolder(
    internal val views: CellBaseViews,
    private val switchView: MaterialSwitch,
) : CellViewHolder<SwitchCell>(views.root) {

    /** 現在 bind 中の Cell の通知ハンドラ。listener 設定に使う。 */
    private var currentHandler: ((Boolean) -> Unit)? = null

    override fun bind(cell: SwitchCell, theme: Theme) {
        val effective = EffectiveStyle.from(views.root.context, theme, cell.style)
        applyCellBaseLayout(
            views = views,
            title = cell.title,
            description = cell.description,
            valueText = cell.valueText,
            icon = cell.icon,
            hintText = cell.hintText,
            effective = effective,
            isEnabled = cell.isEnabled,
        )
        applyCellBackground(views.root, effective)

        // listener を一度 null 化して setChecked のコールバック発火を防ぐ
        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = cell.isOn

        // accent 色: SwitchCell.accentColor → effective.accentColor（CellStyle.accentColor ?? Theme.cellAccentColor）
        val accent = cell.accentColor?.toArgb() ?: effective.accentColor

        // オン thumb は accent に対するコントラスト色として決める（テーマ attr は参照しない）。
        val onThumbColor = onThumbColorFrom(accent)
        // オフ状態の明度の土台にするテーマ attr。素の MaterialSwitch と同じ attr を使うことで、
        // ダークテーマでの明度の反転にそのまま追従する。
        val surfaceColor = MaterialColors.getColor(
            switchView,
            com.google.android.material.R.attr.colorSurfaceContainerHighest,
            Color.LTGRAY,
        )
        val outlineColor = MaterialColors.getColor(
            switchView,
            com.google.android.material.R.attr.colorOutline,
            Color.GRAY,
        )

        val checkedStates = intArrayOf(android.R.attr.state_checked)
        val uncheckedStates = intArrayOf(-android.R.attr.state_checked)

        val offThumbColor = offThumbColorFrom(accent, outlineColor)

        switchView.thumbTintList = ColorStateList(
            arrayOf(checkedStates, uncheckedStates),
            intArrayOf(onThumbColor, offThumbColor),
        )
        switchView.trackTintList = ColorStateList(
            arrayOf(checkedStates, uncheckedStates),
            intArrayOf(accent, offTrackColorFrom(accent, surfaceColor)),
        )
        // track の枠線（trackDecoration）はオフ時のみ描かれる（Material3 既定でオン時は透明）。
        // オフ時の色を thumb と同じ導出色に揃え、オン時は既定どおり透明のままにする。
        switchView.trackDecorationTintList = ColorStateList(
            arrayOf(checkedStates, uncheckedStates),
            intArrayOf(Color.TRANSPARENT, offThumbColor),
        )

        // 通知は OnCheckedChangeListener 一本に集約する
        currentHandler = cell.onValueChanged
        attachHandler()

        // isEnabled = false の場合はスイッチも container も操作不能にする
        switchView.isEnabled = cell.isEnabled
        if (cell.isEnabled) {
            // セル本体タップでもスイッチをトグルする。
            views.root.isClickable = true
            views.root.setOnClickListener {
                switchView.toggle()
            }
        } else {
            views.root.setOnClickListener(null)
            views.root.isClickable = false
        }

        // 実効行高さの反映
        applyEffectiveHeight(views.root, effective)
    }

    /** 保持中の [currentHandler] を switchView の listener に再設定する。 */
    private fun attachHandler() {
        val handler = currentHandler
        if (handler != null) {
            switchView.setOnCheckedChangeListener { _, value ->
                handler.invoke(value)
            }
        } else {
            switchView.setOnCheckedChangeListener(null)
        }
    }

    override fun reset() {
        views.titleView.text = null
        views.descriptionView.text = null
        // 再利用時に古い listener / handler が呼ばれないよう null 化
        currentHandler = null
        switchView.setOnCheckedChangeListener(null)
        views.root.setOnClickListener(null)
        views.root.isClickable = false
    }

    /** テスト用：UI 操作なしで checked 状態を切り替えて listener を発火させるフック。 */
    internal fun simulateValueChange(newValue: Boolean) {
        switchView.isChecked = newValue
    }

    /** テスト用：セル本体タップ相当のトグルを行うフック。 */
    internal fun simulateContainerTap() {
        views.root.performClick()
    }

    companion object {
        fun create(parent: ViewGroup): SwitchCellViewHolder {
            val views = buildCellBaseViews(parent)
            // アクセサリも共通行と同じ Context（同梱テーマ適用済み）から生成する。
            val ctx = views.root.context
            val sw = MaterialSwitch(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
                showText = false
                textOn = ""
                textOff = ""
                isClickable = false
                isFocusable = false
            }
            views.accessoryHolder.addView(sw)
            @Suppress("UNUSED_VARIABLE")
            val _v: View = sw
            return SwitchCellViewHolder(views = views, switchView = sw)
        }
    }
}
