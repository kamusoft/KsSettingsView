package jp.kamusoft.kssettingsview.samples.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import jp.kamusoft.kssettingsview.ui.KsImage
import kotlin.math.roundToInt

/**
 * 角丸の色付き四角に白いシンボルを載せた「バッジ型アイコン」を組み立てるヘルパ。
 *
 * Cell のアイコンは [KsImage] の画像をそのまま表示するため、アイコンの地色は利用者側で
 * 画像に焼き込む。四角形の画像を渡すとアイコン列の幅がシンボルの字形に依存しなくなり、
 * 行ごとの title の開始位置が揃う。角丸は `Theme.cellIconRadius` が担当する。
 *
 * [KsImage.Drawable] の等価判定は `Drawable` の参照同一性で行われるため、画像は 1 度だけ
 * 生成した同じインスタンスを毎回渡す（毎回生成すると差分計算が常に「変更あり」になる）。
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleIconBadge.swift
 */
object SampleIconBadge {

    /** バッジの一辺。`Theme.cellIconSize` に渡す値と一致させる。 */
    val size: Dp = 29.dp

    /** バッジの角丸半径。`Theme.cellIconRadius` に渡す値と一致させる。 */
    val cornerRadius: Dp = 7.dp

    /** バッジ内に描くシンボルの一辺。地色の余白がバッジらしく残る比率にする。 */
    private val symbolSize: Dp = 20.dp

    /** 生成済みバッジの保持先。参照同一性を保つため 1 度だけ作る。 */
    private var cached: Badges? = null

    /**
     * Section 装飾デモが使うバッジ型アイコン一式。
     *
     * @property airplane 機内モード（オレンジ地）
     * @property wifi Wi-Fi（青地）
     * @property bluetooth Bluetooth（明るい青地）
     * @property battery バッテリー（緑地）
     */
    class Badges(
        val airplane: KsImage,
        val wifi: KsImage,
        val bluetooth: KsImage,
        val battery: KsImage,
    )

    /**
     * バッジ型アイコン一式を返す。初回呼び出しで生成し、以降は同じインスタンスを返す。
     *
     * @param context 画像生成に使う Context（application context へ読み替える）
     */
    fun badges(context: Context): Badges {
        cached?.let { return it }
        val appContext = context.applicationContext
        val created = Badges(
            airplane = make(appContext, R.drawable.ic_airplanemode_active, SampleTheme.demoIconOrange),
            wifi = make(appContext, R.drawable.ic_wifi, SampleTheme.demoIconBlue),
            bluetooth = make(appContext, R.drawable.ic_bluetooth, SampleTheme.demoIconVividBlue),
            battery = make(appContext, R.drawable.ic_battery_full, SampleTheme.demoAccentGreen),
        )
        cached = created
        return created
    }

    /**
     * 地色の正方形に白いシンボルを中央配置した画像を作る。
     *
     * @param context drawable 解決と density 取得に使う Context
     * @param resId シンボルの drawable リソース ID
     * @param background バッジの地色
     */
    private fun make(context: Context, @DrawableRes resId: Int, background: Color): KsImage {
        val density = context.resources.displayMetrics.density
        val canvasPx = (size.value * density).roundToInt()
        val symbolPx = (symbolSize.value * density).roundToInt()

        val bitmap = Bitmap.createBitmap(canvasPx, canvasPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background.toArgb())

        val symbol = ContextCompat.getDrawable(context, resId)?.mutate()
        if (symbol != null) {
            // vector 側の tint 指定を白へ差し替え、地色の上でシンボルが白抜きになるようにする。
            symbol.setTint(android.graphics.Color.WHITE)
            val offset = (canvasPx - symbolPx) / 2
            symbol.setBounds(offset, offset, offset + symbolPx, offset + symbolPx)
            symbol.draw(canvas)
        }

        return KsImage.Drawable(BitmapDrawable(context.resources, bitmap))
    }
}
