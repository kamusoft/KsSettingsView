package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell

/**
 * 範囲指定の数値選択 Cell。
 *
 * 行タップで `min` から `max` まで `step` 刻みの候補を並べた選択面を提示し、確定した値を
 * [onValueChanged] へ通知する。id は未指定なら生成した一意値を既定とする。
 *
 * @property min 最小値（既定 `0`）
 * @property max 最大値（既定 `100`）
 * @property step 刻み（既定 `1`）
 * @property value 現在値
 * @property unit 値に付与する単位（既定 `""` = 単位なし）
 * @property pickerTitle モーダル画面のタイトル（任意）
 * @property accentColor 強調色（任意）
 * @property valueText 明示指定の valueText（`null` で [format] による自動表示）
 * @property onValueChanged 値変更 callback
 */
public data class NumberPickerCell(
    override val id: String = "number-picker-cell-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val min: Int = 0,
    val max: Int = 100,
    val step: Int = 1,
    val value: Int = 0,
    val unit: String = "",
    val pickerTitle: String? = null,
    val accentColor: Color? = null,
    val onValueChanged: ((Int) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    /**
     * Cell 行に表示する valueText。
     *
     * [valueText] の明示指定があればそれを優先し、`null` のときは [format] で自動生成する。
     */
    internal fun effectiveValueText(): String = valueText ?: format(value, unit)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumberPickerCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            min == other.min &&
            max == other.max &&
            step == other.step &&
            value == other.value &&
            unit == other.unit &&
            pickerTitle == other.pickerTitle &&
            accentColor == other.accentColor &&
            isEnabled == other.isEnabled &&
            isVisible == other.isVisible
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (valueText?.hashCode() ?: 0)
        result = 31 * result + (icon?.hashCode() ?: 0)
        result = 31 * result + (hintText?.hashCode() ?: 0)
        result = 31 * result + min
        result = 31 * result + max
        result = 31 * result + step
        result = 31 * result + value
        result = 31 * result + unit.hashCode()
        result = 31 * result + (pickerTitle?.hashCode() ?: 0)
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }

    public companion object {
        /**
         * 値の表示文字列を組み立てる共通フォーマッタ。
         *
         * [unit] が空なら数値のみ、非空なら「値 + 半角スペース + 単位」を返す。
         * Cell 行の valueText 自動表示と、選択面の候補表示の双方で同じ規則を使う。
         */
        internal fun format(value: Int, unit: String): String =
            if (unit.isEmpty()) value.toString() else "$value $unit"
    }
}
