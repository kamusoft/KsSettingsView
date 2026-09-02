package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import java.time.LocalTime

/**
 * 時刻選択用 Cell。
 *
 * `time` は **Native 型 [java.time.LocalTime]** を直接公開する（中間論理表現でラップしない。
 * core/ADR-0009）。`valueText` 自動表示は
 * `LocalTime.format(DateTimeFormatter.ofPattern(format))` で文字列化する。
 *
 * @property time 現在時刻（既定 `LocalTime.MIDNIGHT`）
 * @property format `DateTimeFormatter.ofPattern` 互換のフォーマット文字列（既定 `"HH:mm"`）。
 *   行の valueText の文字列化にだけ効き、選択面の時制には関与しない（core/ADR-0028）
 * @property is24Hour 選択面の時制（既定 `true` = 24時間制、`false` で12時間制）。選択面の時制は
 *   この値だけで決まる（core/ADR-0028）
 * @property pickerTitle モーダル画面のタイトル（任意）
 * @property accentColor 強調色（任意）
 * @property valueText 明示指定の valueText（`null` で `format` に従って自動表示）
 * @property onValueChanged 時刻変更 callback
 */
public data class TimePickerCell(
    override val id: String = "time-picker-cell-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val time: LocalTime = LocalTime.MIDNIGHT,
    val format: String = "HH:mm",
    val is24Hour: Boolean = true,
    val pickerTitle: String? = null,
    val accentColor: Color? = null,
    val onValueChanged: ((LocalTime) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TimePickerCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            time == other.time &&
            format == other.format &&
            is24Hour == other.is24Hour &&
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
        result = 31 * result + time.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + is24Hour.hashCode()
        result = 31 * result + (pickerTitle?.hashCode() ?: 0)
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
