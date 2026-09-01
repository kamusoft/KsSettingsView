package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.ButtonCell
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.CheckboxCell
import jp.kamusoft.kssettingsview.ui.CommandCell
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SimpleCheckCell
import jp.kamusoft.kssettingsview.ui.SwitchCell

/**
 * 基本 Cell 7 種を `DSLSectionScope` に直置きするための拡張関数群。
 *
 * iOS Sample 側の `Section("一般") { LabelCell(title: "...") }` イディオムと一致する書き味を
 * Compose 側でも提供する。
 *
 * 各拡張関数の戻り値は [CellHandle] で、`.cellHeight(...)` / `.cellID(...)` 等の
 * Cell modifier chain が可能。
 *
 * `CellStyle` / `KsImage` は Core ではなく UI 層（`jp.kamusoft.kssettingsview.ui` パッケージ）に
 * 属し、色の引数は
 * Compose の [Color]? をそのまま受ける（core/ADR-0009）。
 */

// =============================================================================
// data class 構築ヘルパ（同名拡張関数のシャドウ回避）
// =============================================================================

private fun buildLabelCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    style: CellStyle,
    isEnabled: Boolean,
    isVisible: Boolean,
): LabelCell = LabelCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildCommandCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    hideArrow: Boolean,
    style: CellStyle,
    onTap: (() -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
): CommandCell = CommandCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    hideArrow = hideArrow,
    onTap = onTap,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildButtonCell(
    title: String,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    titleColor: Color?,
    style: CellStyle,
    onTap: (() -> Unit)?,
    titleAlignment: CellTitleAlignment,
    isEnabled: Boolean,
    isVisible: Boolean,
): ButtonCell = ButtonCell(
    style = style,
    title = title,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    titleColor = titleColor,
    onTap = onTap,
    titleAlignment = titleAlignment,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildSwitchCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    isOn: Boolean,
    accentColor: Color?,
    style: CellStyle,
    onValueChanged: ((Boolean) -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
): SwitchCell = SwitchCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isOn = isOn,
    accentColor = accentColor,
    onValueChanged = onValueChanged,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildCheckboxCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    isChecked: Boolean,
    accentColor: Color?,
    style: CellStyle,
    onValueChanged: ((Boolean) -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
): CheckboxCell = CheckboxCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isChecked = isChecked,
    accentColor = accentColor,
    onValueChanged = onValueChanged,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildRadioCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    groupId: String,
    value: String,
    selectedValue: String,
    accentColor: Color?,
    style: CellStyle,
    onSelected: ((String) -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
): RadioCell = RadioCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    groupId = groupId,
    value = value,
    selectedValue = selectedValue,
    accentColor = accentColor,
    onSelected = onSelected,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

private fun buildSimpleCheckCell(
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    isChecked: Boolean,
    accentColor: Color?,
    style: CellStyle,
    onValueChanged: ((Boolean) -> Unit)?,
    isEnabled: Boolean,
    isVisible: Boolean,
): SimpleCheckCell = SimpleCheckCell(
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    isChecked = isChecked,
    accentColor = accentColor,
    onValueChanged = onValueChanged,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

// =============================================================================
// DSL 拡張関数
// =============================================================================

/** `Section { LabelCell(title = "...") }` のように Cell を直置きするための拡張関数。 */
public fun DSLSectionScope.LabelCell(
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    style: CellStyle = CellStyle(),
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildLabelCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        style = style,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { CommandCell(title = "...", onTap = { ... }) }` 用の拡張関数。 */
public fun DSLSectionScope.CommandCell(
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    hideArrow: Boolean = false,
    style: CellStyle = CellStyle(),
    onTap: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildCommandCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        hideArrow = hideArrow,
        style = style,
        onTap = onTap,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { ButtonCell(title = "...", onTap = { ... }) }` 用の拡張関数。 */
public fun DSLSectionScope.ButtonCell(
    title: String,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    titleColor: Color? = null,
    style: CellStyle = CellStyle(),
    onTap: (() -> Unit)? = null,
    titleAlignment: CellTitleAlignment = CellTitleAlignment.CENTER,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildButtonCell(
        title = title,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        titleColor = titleColor,
        style = style,
        onTap = onTap,
        titleAlignment = titleAlignment,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/**
 * `Section { SwitchCell(title = "...", isOn = isOnState) }` 用の拡張関数。
 *
 * 引数 `isOn` は `MutableState<Boolean>` を受け取り、内部で値の読み取り / 書き込みを行う。
 */
public fun DSLSectionScope.SwitchCell(
    title: String,
    isOn: MutableState<Boolean>,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildSwitchCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        isOn = isOn.value,
        accentColor = accentColor,
        style = style,
        onValueChanged = { newValue -> isOn.value = newValue },
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { SwitchCell(title = "...", isOn = true, onValueChanged = { ... }) }` 用の拡張関数。 */
public fun DSLSectionScope.SwitchCell(
    title: String,
    isOn: Boolean,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
    onValueChanged: ((Boolean) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildSwitchCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        isOn = isOn,
        accentColor = accentColor,
        style = style,
        onValueChanged = onValueChanged,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { CheckboxCell(title = "...", isChecked = state, onValueChanged = { ... }) }` 用。 */
public fun DSLSectionScope.CheckboxCell(
    title: String,
    isChecked: Boolean,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
    onValueChanged: ((Boolean) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildCheckboxCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        isChecked = isChecked,
        accentColor = accentColor,
        style = style,
        onValueChanged = onValueChanged,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { RadioCell(title = "...", groupId = "g", value = "a", selectedValue = sel) }` 用。 */
public fun DSLSectionScope.RadioCell(
    title: String,
    groupId: String,
    value: String,
    selectedValue: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
    onSelected: ((String) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildRadioCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        groupId = groupId,
        value = value,
        selectedValue = selectedValue,
        accentColor = accentColor,
        style = style,
        onSelected = onSelected,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)

/** `Section { SimpleCheckCell(title = "...", isChecked = state, onValueChanged = { ... }) }` 用。 */
public fun DSLSectionScope.SimpleCheckCell(
    title: String,
    isChecked: Boolean,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    accentColor: Color? = null,
    style: CellStyle = CellStyle(),
    onValueChanged: ((Boolean) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): CellHandle = cell(
    buildSimpleCheckCell(
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        isChecked = isChecked,
        accentColor = accentColor,
        style = style,
        onValueChanged = onValueChanged,
        isEnabled = isEnabled,
        isVisible = isVisible,
    ),
)
