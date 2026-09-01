package jp.kamusoft.kssettingsview.bridge

import android.graphics.Typeface
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * フォントを interop 境界で輸送する記述子。
 *
 * interop 境界では platform のフォント型を直接渡せないため、family 名・サイズ・太字／斜体の
 * プリミティブで表現し、Native 側で `TextStyle` へ解決する（maui/ADR-0004）。
 *
 * @property familyName フォントファミリ名。`null` のときはフォントファミリを指定しない
 *   （解決結果の見た目は描画層のフォント解決に従う）
 * @property pointSize ポイントサイズ。`0` 以下のときはサイズ未指定として扱う
 * @property isBold 太字にするか
 * @property isItalic 斜体にするか
 */
class KsBridgeFont @JvmOverloads constructor(
    var familyName: String? = null,
    var pointSize: Double = 0.0,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
) {

    /** 記述子から `TextStyle` を解決する。 */
    @JvmSynthetic
    internal fun resolve(): TextStyle = TextStyle(
        fontSize = if (pointSize > 0) pointSize.sp else TextUnit.Unspecified,
        fontWeight = if (isBold) FontWeight.Bold else null,
        fontStyle = if (isItalic) FontStyle.Italic else null,
        fontFamily = familyName?.let { FontFamily(Typeface.create(it, Typeface.NORMAL)) },
    )
}
