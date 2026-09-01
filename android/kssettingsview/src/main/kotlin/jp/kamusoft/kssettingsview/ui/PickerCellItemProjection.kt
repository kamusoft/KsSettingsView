package jp.kamusoft.kssettingsview.ui

import androidx.compose.ui.graphics.Color

/**
 * 任意の要素型 `T` を候補として渡すためのジェネリック factory 群と、その String 特殊化。
 *
 * 縁は構築時に要素列をコピーして捕捉し、表示用の [PickerItem] 列へ射影する。要素型はここから
 * 外（モデル・描画・equality・輸送）へは現れず、選択の正は index のままである（core/ADR-0029）。
 * 確定操作では index の書き戻し（index callback）が先、元要素の callback が後に走る。
 */

/** [PickerCell] の既定 id を生成する。data class の既定値と同じ形。 */
private fun newPickerCellId(): String = "picker-cell-${java.util.UUID.randomUUID()}"

// =============================================================================
// ジェネリック縁（単一選択）
// =============================================================================

/**
 * 任意の要素列と射影から単一選択の [PickerCell] を構築する。
 *
 * @param displayText 要素から主表示テキストを作る射影
 * @param subText 要素から副表示テキストを作る射影（`null` または空文字列を返した要素は副表示なし）
 * @param onItemSelected 確定した index に対応する元要素を受け取る callback
 */
fun <T> PickerCell(
    id: String = newPickerCellId(),
    style: CellStyle = CellStyle(),
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    items: List<T>,
    displayText: (T) -> String,
    subText: ((T) -> String?)? = null,
    selectedIndex: Int? = null,
    pageTitle: String? = null,
    accentColor: Color? = null,
    onSelectionChanged: ((Int) -> Unit)? = null,
    onItemSelected: ((T) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): PickerCell {
    val elements = items.toList()
    return PickerCell(
        id = id,
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        items = projectPickerItems(elements, displayText, subText),
        selectionMode = PickerSelectionMode.Single,
        selectedIndex = selectedIndex,
        pageTitle = pageTitle,
        accentColor = accentColor,
        onSelectionChanged = composeSingleSelection(elements, onSelectionChanged, onItemSelected),
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}

// =============================================================================
// ジェネリック縁（複数選択）
// =============================================================================

/**
 * 任意の要素列と射影から複数選択の [PickerCell] を構築する。
 *
 * @param onItemsSelected 確定した index 集合に対応する元要素を index 昇順で受け取る callback
 *   （範囲外 index に対応する要素は含まれない）
 */
fun <T> PickerCell(
    id: String = newPickerCellId(),
    style: CellStyle = CellStyle(),
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    items: List<T>,
    displayText: (T) -> String,
    subText: ((T) -> String?)? = null,
    selectedIndices: Set<Int>,
    maxSelectedNumber: Int = 0,
    pageTitle: String? = null,
    accentColor: Color? = null,
    onMultiSelectionChanged: ((Set<Int>) -> Unit)? = null,
    onItemsSelected: ((List<T>) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): PickerCell {
    val elements = items.toList()
    return PickerCell(
        id = id,
        style = style,
        title = title,
        description = description,
        valueText = valueText,
        icon = icon,
        hintText = hintText,
        items = projectPickerItems(elements, displayText, subText),
        selectionMode = PickerSelectionMode.Multiple,
        selectedIndices = selectedIndices,
        maxSelectedNumber = maxSelectedNumber,
        pageTitle = pageTitle,
        accentColor = accentColor,
        onMultiSelectionChanged = composeMultiSelection(elements, onMultiSelectionChanged, onItemsSelected),
        isEnabled = isEnabled,
        isVisible = isVisible,
    )
}

// =============================================================================
// String 特殊化（射影は恒等、`displayText` を省略できる簡易形）
// =============================================================================

/** 文字列列から単一選択の [PickerCell] を構築する。 */
fun PickerCell(
    id: String = newPickerCellId(),
    style: CellStyle = CellStyle(),
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    items: List<String>,
    subText: ((String) -> String?)? = null,
    selectedIndex: Int? = null,
    pageTitle: String? = null,
    accentColor: Color? = null,
    onSelectionChanged: ((Int) -> Unit)? = null,
    onItemSelected: ((String) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): PickerCell = PickerCell(
    id = id,
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    items = items,
    displayText = { it },
    subText = subText,
    selectedIndex = selectedIndex,
    pageTitle = pageTitle,
    accentColor = accentColor,
    onSelectionChanged = onSelectionChanged,
    onItemSelected = onItemSelected,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

/** 文字列列から複数選択の [PickerCell] を構築する。 */
fun PickerCell(
    id: String = newPickerCellId(),
    style: CellStyle = CellStyle(),
    title: String,
    description: String? = null,
    valueText: String? = null,
    icon: KsImage? = null,
    hintText: String? = null,
    items: List<String>,
    subText: ((String) -> String?)? = null,
    selectedIndices: Set<Int>,
    maxSelectedNumber: Int = 0,
    pageTitle: String? = null,
    accentColor: Color? = null,
    onMultiSelectionChanged: ((Set<Int>) -> Unit)? = null,
    onItemsSelected: ((List<String>) -> Unit)? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
): PickerCell = PickerCell(
    id = id,
    style = style,
    title = title,
    description = description,
    valueText = valueText,
    icon = icon,
    hintText = hintText,
    items = items,
    displayText = { it },
    subText = subText,
    selectedIndices = selectedIndices,
    maxSelectedNumber = maxSelectedNumber,
    pageTitle = pageTitle,
    accentColor = accentColor,
    onMultiSelectionChanged = onMultiSelectionChanged,
    onItemsSelected = onItemsSelected,
    isEnabled = isEnabled,
    isVisible = isVisible,
)

// =============================================================================
// 射影と callback の組み立て
// =============================================================================

/** 要素列へ射影を適用して候補列を作る。 */
internal fun <T> projectPickerItems(
    elements: List<T>,
    displayText: (T) -> String,
    subText: ((T) -> String?)?,
): List<PickerItem> = elements.map { element ->
    PickerItem(text = displayText(element), subText = subText?.invoke(element))
}

/**
 * 単一選択の確定 callback を組み立てる。index の書き戻しを先に、元要素の通知を後に走らせる。
 * どちらの受け口も無ければ callback 自体を持たない。
 */
internal fun <T> composeSingleSelection(
    elements: List<T>,
    indexSink: ((Int) -> Unit)?,
    itemSink: ((T) -> Unit)?,
): ((Int) -> Unit)? {
    if (indexSink == null && itemSink == null) return null
    return { newIndex ->
        indexSink?.invoke(newIndex)
        if (itemSink != null && newIndex in elements.indices) {
            itemSink(elements[newIndex])
        }
    }
}

/**
 * 複数選択の確定 callback を組み立てる。index 集合の書き戻しを先に、元要素列の通知を後に走らせる。
 * 元要素列は index 昇順で、範囲外 index に対応する要素は含めない。
 */
internal fun <T> composeMultiSelection(
    elements: List<T>,
    indicesSink: ((Set<Int>) -> Unit)?,
    itemsSink: ((List<T>) -> Unit)?,
): ((Set<Int>) -> Unit)? {
    if (indicesSink == null && itemsSink == null) return null
    return { newIndices ->
        indicesSink?.invoke(newIndices)
        if (itemsSink != null) {
            itemsSink(newIndices.sorted().filter { it in elements.indices }.map { elements[it] })
        }
    }
}
