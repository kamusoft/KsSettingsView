package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.ui.CustomCell

/**
 * `CustomCell` を返すラップ関数の例（CustomCell デモ「再利用（SliderCell ラップ関数）」）。
 *
 * 独自の Cell 型を新設しなくても、CustomCell を返す関数を 1 つ用意すれば
 * 「アプリ固有の Cell」を再利用単位として切り出せる、という利用パターンを示す。
 * ラップ関数を別ファイルに置いてあるのは、その再利用性そのものを示すため。
 *
 * 表示に効く値は builder のキャプチャではなく content に持たせる（core/ADR-0014）。
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleSliderCell.swift
 */

/**
 * [SliderCell] の content 値。
 *
 * 「表示に効く値はクロージャのキャプチャではなく content に含める」という CustomCell の
 * 利用者契約に従い、ラベルと値の両方を content に持たせる。
 * これにより値が変わった行だけが再バインドされる。
 */
data class SampleSliderValue(
    val label: String,
    val value: Int,
)

/**
 * ラベル + スライダー + 数値の 1 行を [CustomCell] として組み立てる。
 *
 * ```kotlin
 * Section(header = "再利用（SliderCell ラップ関数）") {
 *     cell(SliderCell(label = "明るさ", value = brightness) { brightness = it })
 * }
 * ```
 *
 * @param label 行頭のラベル
 * @param value 0..100 の値。content に含まれるため、変化すると行が再バインドされる
 * @param isEnabled `false` で content 内部の操作（スライダーのドラッグ）が抑止される
 * @param onValueChanged ドラッグ確定時に呼ばれる。関数値は等価性に参加しない
 * @return そのまま `cell(...)` に流せる [CustomCell]
 */
fun SliderCell(
    label: String,
    value: Int,
    isEnabled: Boolean = true,
    onValueChanged: ((Int) -> Unit)? = null,
): CustomCell<SampleSliderValue> = CustomCell(
    content = SampleSliderValue(label = label, value = value),
    isEnabled = isEnabled,
    builder = { content ->
        SampleSliderRow(content = content, onValueChanged = onValueChanged)
    },
)

/** [SliderCell] の行 Composable。 */
@Composable
private fun SampleSliderRow(
    content: SampleSliderValue,
    onValueChanged: ((Int) -> Unit)?,
) {
    // ドラッグ追従用のローカル値。
    // ドラッグのたびに content を差し替えると 1 フレームごとに再バインドが走るため、
    // ドラッグ中はローカル state で追従し、確定時にだけ onValueChanged で外へ返す。
    var draggingValue by remember(content) { mutableFloatStateOf(content.value.toFloat()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = content.label,
            fontSize = 16.sp,
            color = SampleTheme.mauiDeepText,
            modifier = Modifier.width(64.dp),
        )
        // 形状は Material 3 の標準描画のままとし、色だけ SampleTheme のアクセント色に揃える。
        // iOS 側は `.tint(SampleTheme.mauiAccent)` を渡しており、Sample はプラットフォーム間で
        // 同じ色を渡すことを規約としているため、M3 既定色（紫）のままにはしない。
        Slider(
            value = draggingValue,
            onValueChange = { draggingValue = it },
            onValueChangeFinished = { onValueChanged?.invoke(draggingValue.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = SampleTheme.mauiAccent,
                activeTrackColor = SampleTheme.mauiAccent,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(
            text = draggingValue.toInt().toString(),
            fontSize = 14.sp,
            color = SampleTheme.mauiFooterText,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}
