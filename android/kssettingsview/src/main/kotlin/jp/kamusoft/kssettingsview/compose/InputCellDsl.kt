package jp.kamusoft.kssettingsview.compose

import android.text.InputType
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import java.time.LocalDate
import java.time.LocalTime
// 拡張関数 `DSLSectionScope.EntryCell(...)` 等が同名で UI 層の data class を
// シャドウしてしまうため、UI 層 Cell 型はエイリアスで参照する。
import jp.kamusoft.kssettingsview.ui.DatePickerCell as UiDatePickerCell
import jp.kamusoft.kssettingsview.ui.EntryCell as UiEntryCell
import jp.kamusoft.kssettingsview.ui.NumberPickerCell as UiNumberPickerCell
import jp.kamusoft.kssettingsview.ui.PickerCell as UiPickerCell
import jp.kamusoft.kssettingsview.ui.TimePickerCell as UiTimePickerCell

/**
 * 入力系 Cell 5 種を `DSLSectionScope` に直置きするための拡張関数群。
 *
 * 値の型は Android/Kotlin の標準型をそのまま公開し、TwoWay overload は `MutableState` を
 * 受け取って確定値を書き戻す（callback 経路が必要な場合は UI 層の Cell を直接使う）。
 */

// =============================================================================
// EntryCell
// =============================================================================

/** `Section { EntryCell(title = "...", text = state) }` 用の TwoWay binding 拡張関数。 */
public fun DSLSectionScope.EntryCell(
    title: String,
    text: MutableState<String>,
    description: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    placeholder: String? = null,
    keyboardType: Int = InputType.TYPE_CLASS_TEXT,
    isPassword: Boolean = false,
    textAlignment: CellTitleAlignment = CellTitleAlignment.END,
    accentColor: Color? = null,
    maxLength: Int? = null,
    style: CellStyle = CellStyle(),
    placeholderColor: Color? = null,
): CellHandle = cell(
    UiEntryCell(
        style = style,
        title = title,
        description = description,
        icon = icon,
        hintText = hintText,
        text = text.value,
        placeholder = placeholder,
        keyboardType = keyboardType,
        isPassword = isPassword,
        textAlignment = textAlignment,
        accentColor = accentColor,
        maxLength = maxLength,
        onTextChanged = { newValue -> text.value = newValue },
        isEnabled = isEnabled,
        isVisible = isVisible,
        placeholderColor = placeholderColor,
    ),
)

/** `Section { EntryCell(title = "...", text = "current", onTextChanged = { ... }) }` 用の callback 拡張関数。 */
public fun DSLSectionScope.EntryCell(
    title: String,
    text: String,
    description: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    placeholder: String? = null,
    keyboardType: Int = InputType.TYPE_CLASS_TEXT,
    isPassword: Boolean = false,
    textAlignment: CellTitleAlignment = CellTitleAlignment.END,
    accentColor: Color? = null,
    maxLength: Int? = null,
    onTextChanged: ((String) -> Unit)? = null,
    style: CellStyle = CellStyle(),
    placeholderColor: Color? = null,
): CellHandle = cell(
    UiEntryCell(
        style = style,
        title = title,
        description = description,
        icon = icon,
        hintText = hintText,
        text = text,
        placeholder = placeholder,
        keyboardType = keyboardType,
        isPassword = isPassword,
        textAlignment = textAlignment,
        accentColor = accentColor,
        maxLength = maxLength,
        onTextChanged = onTextChanged,
        isEnabled = isEnabled,
        isVisible = isVisible,
        placeholderColor = placeholderColor,
    ),
)

// =============================================================================
// PickerCell（単一選択 overload）
// =============================================================================

/** `Section { PickerCell(title = "...", items = list, selectedIndex = state) }` 単一選択 overload。 */
public fun DSLSectionScope.PickerCell(
    title: String,
    items: List<String>,
    selectedIndex: MutableState<Int?>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    pageTitle: String? = null,
    subText: ((String) -> String?)? = null,
    onItemSelected: ((String) -> Unit)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = PickerCell(
    title = title,
    items = items,
    displayText = { it },
    selectedIndex = selectedIndex,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
    pageTitle = pageTitle,
    subText = subText,
    onItemSelected = onItemSelected,
    accentColor = accentColor,
    style = style,
)

/**
 * 任意の要素列と射影から単一選択 Cell を組み立てる overload。
 *
 * 要素列は構築時にコピーして捕捉され、[onItemSelected] にはその捕捉列の要素が届く。
 */
public fun <T> DSLSectionScope.PickerCell(
    title: String,
    items: List<T>,
    displayText: (T) -> String,
    selectedIndex: MutableState<Int?>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    pageTitle: String? = null,
    subText: ((T) -> String?)? = null,
    onItemSelected: ((T) -> Unit)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = cell(
    UiPickerCell(
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        items = items,
        displayText = displayText,
        subText = subText,
        selectedIndex = selectedIndex.value,
        pageTitle = pageTitle,
        accentColor = accentColor,
        onSelectionChanged = { newIndex -> selectedIndex.value = newIndex },
        onItemSelected = onItemSelected,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

// =============================================================================
// PickerCell（単一選択 / 元要素の TwoWay overload）
// =============================================================================

/** `Section { PickerCell(title = "...", items = list, selectedItem = state) }` 文字列の TwoWay overload。 */
public fun DSLSectionScope.PickerCell(
    title: String,
    items: List<String>,
    selectedItem: MutableState<String?>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    pageTitle: String? = null,
    subText: ((String) -> String?)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = PickerCell(
    title = title,
    items = items,
    displayText = { it },
    selectedItem = selectedItem,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
    pageTitle = pageTitle,
    subText = subText,
    accentColor = accentColor,
    style = style,
)

/**
 * 元要素の TwoWay state から単一選択 Cell を組み立てる overload。
 *
 * 構築時に [selectedItem] を候補列から同値で逆引きして選択 index を決める。同値の要素が
 * 複数あるときは最初の位置に、候補列に無い要素は未選択に解決する。確定時は対応する元要素へ
 * 書き戻す（有効な候補が無い index では `null` になる）。
 */
public fun <T> DSLSectionScope.PickerCell(
    title: String,
    items: List<T>,
    displayText: (T) -> String,
    selectedItem: MutableState<T?>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    pageTitle: String? = null,
    subText: ((T) -> String?)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle {
    val elements = items.toList()
    val current = selectedItem.value
    val initialIndex = if (current == null) null else elements.indexOf(current).takeIf { it >= 0 }
    return cell(
        UiPickerCell(
            style = style,
            title = title,
            description = description,
            valueText = valueText,
            icon = icon,
            hintText = hintText,
            items = elements,
            displayText = displayText,
            subText = subText,
            selectedIndex = initialIndex,
            pageTitle = pageTitle,
            accentColor = accentColor,
            onSelectionChanged = { newIndex -> selectedItem.value = elements.getOrNull(newIndex) },
            isEnabled = isEnabled,
            isVisible = isVisible,
        ),
    )
}

// =============================================================================
// PickerCell（複数選択 overload）
// =============================================================================

/** `Section { PickerCell(title = "...", items = list, selectedIndices = state) }` 複数選択 overload。 */
public fun DSLSectionScope.PickerCell(
    title: String,
    items: List<String>,
    selectedIndices: MutableState<Set<Int>>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    maxSelectedNumber: Int = 0,
    pageTitle: String? = null,
    subText: ((String) -> String?)? = null,
    onItemsSelected: ((List<String>) -> Unit)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = PickerCell(
    title = title,
    items = items,
    displayText = { it },
    selectedIndices = selectedIndices,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
    maxSelectedNumber = maxSelectedNumber,
    pageTitle = pageTitle,
    subText = subText,
    onItemsSelected = onItemsSelected,
    accentColor = accentColor,
    style = style,
)

/**
 * 任意の要素列と射影から複数選択 Cell を組み立てる overload。
 *
 * [onItemsSelected] には選択中の元要素が index 昇順で届く（範囲外 index の要素は含まれない）。
 */
public fun <T> DSLSectionScope.PickerCell(
    title: String,
    items: List<T>,
    displayText: (T) -> String,
    selectedIndices: MutableState<Set<Int>>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    maxSelectedNumber: Int = 0,
    pageTitle: String? = null,
    subText: ((T) -> String?)? = null,
    onItemsSelected: ((List<T>) -> Unit)? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = cell(
    UiPickerCell(
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        items = items,
        displayText = displayText,
        subText = subText,
        selectedIndices = selectedIndices.value,
        maxSelectedNumber = maxSelectedNumber,
        pageTitle = pageTitle,
        accentColor = accentColor,
        onMultiSelectionChanged = { newSet -> selectedIndices.value = newSet },
        onItemsSelected = onItemsSelected,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

// =============================================================================
// NumberPickerCell
// =============================================================================

/** `Section { NumberPickerCell(title = "...", value = state) }` 用の TwoWay binding 拡張関数。 */
public fun DSLSectionScope.NumberPickerCell(
    title: String,
    value: MutableState<Int>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
    unit: String = "",
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = cell(
    UiNumberPickerCell(
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        min = min,
        max = max,
        step = step,
        value = value.value,
        unit = unit,
        pickerTitle = pickerTitle,
        accentColor = accentColor,
        onValueChanged = { newValue -> value.value = newValue },
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

// =============================================================================
// TimePickerCell
// =============================================================================

/** `Section { TimePickerCell(title = "...", time = state) }` 用の TwoWay binding 拡張関数。 */
public fun DSLSectionScope.TimePickerCell(
    title: String,
    time: MutableState<LocalTime>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    format: String = "HH:mm",
    is24Hour: Boolean = true,
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = cell(
    UiTimePickerCell(
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        time = time.value,
        format = format,
        is24Hour = is24Hour,
        pickerTitle = pickerTitle,
        accentColor = accentColor,
        onValueChanged = { newValue -> time.value = newValue },
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

// =============================================================================
// DatePickerCell
// =============================================================================

/** `Section { DatePickerCell(title = "...", date = state) }` 用の TwoWay binding 拡張関数。 */
public fun DSLSectionScope.DatePickerCell(
    title: String,
    date: MutableState<LocalDate>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
    format: String = "yyyy/MM/dd",
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    uiStyle: DatePickerUIStyle = DatePickerUIStyle.Material,
    todayText: String? = null,
    androidButtonColor: Color? = null,
    pickerTitle: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
): CellHandle = cell(
    UiDatePickerCell(
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        date = date.value,
        format = format,
        minDate = minDate,
        maxDate = maxDate,
        pickerTitle = pickerTitle,
        uiStyle = uiStyle,
        todayText = todayText,
        androidButtonColor = androidButtonColor,
        accentColor = accentColor,
        onValueChanged = { newValue -> date.value = newValue },
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)
