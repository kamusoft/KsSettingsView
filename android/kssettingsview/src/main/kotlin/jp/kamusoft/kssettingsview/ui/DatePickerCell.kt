package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.DSLReidentifiableCell
import java.time.LocalDate

/**
 * 日付選択用 Cell。
 *
 * `date` は **Native 型 [java.time.LocalDate]** を直接公開する。
 * `uiStyle` で `Material` / `Spinner` の UI を切り替える。
 *
 * @property date 現在日付（既定は `LocalDate.now()` ではなく `LocalDate.of(1970, 1, 1)` 相当の epoch）
 * @property format `DateTimeFormatter.ofPattern` 互換のフォーマット文字列（既定 `"yyyy/MM/dd"`）
 * @property minDate 最小日付（任意）
 * @property maxDate 最大日付（任意）
 * @property pickerTitle モーダル画面のタイトル（任意）
 * @property uiStyle Android UI スタイル切替（既定 [DatePickerUIStyle.Material]）。
 *   iOS 側 `DatePickerCell` も同名 `uiStyle` プロパティを持つ（ケースは別、`wheels` / `calendar`）。
 * @property todayText 「今日」へジャンプする操作のラベル（任意）。`null` / 空文字で非表示
 *   （iOS と同じオプトイン）
 * @property androidButtonColor ホイール型（[DatePickerUIStyle.Spinner]）選択面のヘッダー操作色（任意）。
 *   カレンダー型（[DatePickerUIStyle.Material]）には効かず、そちらの操作色は強調ロール
 *   （[accentColor]）に従う
 * @property accentColor 強調色（任意）
 * @property valueText 明示指定の valueText（`null` で `format` に従って自動表示）
 * @property onValueChanged 日付変更 callback
 */
data class DatePickerCell(
    override val id: String = "date-picker-cell-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val description: String? = null,
    val valueText: String? = null,
    val icon: KsImage? = null,
    val hintText: String? = null,
    val date: LocalDate = LocalDate.of(1970, 1, 1),
    val format: String = "yyyy/MM/dd",
    val minDate: LocalDate? = null,
    val maxDate: LocalDate? = null,
    val pickerTitle: String? = null,
    val uiStyle: DatePickerUIStyle = DatePickerUIStyle.Material,
    val todayText: String? = null,
    val androidButtonColor: Color? = null,
    val accentColor: Color? = null,
    val onValueChanged: ((LocalDate) -> Unit)? = null,
    val isEnabled: Boolean = true,
    override val isVisible: Boolean = true,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell, DSLIconModifiableCell, VisibilityAware {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
    override fun withDSLIcon(newIcon: KsImage?): Cell = copy(icon = newIcon)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DatePickerCell) return false
        return id == other.id &&
            style == other.style &&
            title == other.title &&
            description == other.description &&
            valueText == other.valueText &&
            icon == other.icon &&
            hintText == other.hintText &&
            date == other.date &&
            format == other.format &&
            minDate == other.minDate &&
            maxDate == other.maxDate &&
            pickerTitle == other.pickerTitle &&
            uiStyle == other.uiStyle &&
            todayText == other.todayText &&
            androidButtonColor == other.androidButtonColor &&
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
        result = 31 * result + date.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (minDate?.hashCode() ?: 0)
        result = 31 * result + (maxDate?.hashCode() ?: 0)
        result = 31 * result + (pickerTitle?.hashCode() ?: 0)
        result = 31 * result + uiStyle.hashCode()
        result = 31 * result + (todayText?.hashCode() ?: 0)
        result = 31 * result + (androidButtonColor?.hashCode() ?: 0)
        result = 31 * result + (accentColor?.hashCode() ?: 0)
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + isVisible.hashCode()
        return result
    }
}
