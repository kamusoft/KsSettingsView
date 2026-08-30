package jp.kamusoft.kssettingsview.bridge

import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.PickerCell
import jp.kamusoft.kssettingsview.ui.PickerSelectionMode

/**
 * 一覧から項目を選ぶ Cell（`PickerCell`）を輸送する DTO。
 *
 * 選択値は Native の実体である index で運ぶ（maui/ADR-0012）。項目の表示整形は上位層が適用済みの
 * 主表示・副表示のペア（[KsBridgePickerItem]）として [items] に載せる。
 *
 * 選択変更は [selectionMode] に応じて
 * [KsBridgeInteractionListener.pickerCellSelectionChanged] または
 * [KsBridgeInteractionListener.pickerCellMultiSelectionChanged] で通知される。
 */
class KsBridgePickerCell @JvmOverloads constructor(
    title: String,
    descriptionText: String? = null,
    valueText: String? = null,
    hintText: String? = null,
    isEnabled: Boolean = true,
    isVisible: Boolean = true,
) : KsBridgeCell(
    title = title,
    descriptionText = descriptionText,
    valueText = valueText,
    hintText = hintText,
    isEnabled = isEnabled,
    isVisible = isVisible,
) {

    /** 選択候補の項目（表示整形済み） */
    var items: List<KsBridgePickerItem> = emptyList()

    /** 選択モードの序数（`0 = Single / 1 = Multiple`） */
    var selectionMode: Int = 0

    /** 単一選択モードの選択 index（未選択は `null`） */
    var selectedIndex: Int? = null

    /** 複数選択モードの選択 index 群 */
    var selectedIndices: IntArray = IntArray(0)

    /** 複数選択モードでの選択上限（`0` で無制限） */
    var maxSelectedNumber: Int = 0

    /** 選択面のタイトル（未指定は `null`） */
    var pageTitle: String? = null

    /** 選択強調色（ARGB、未指定は `null`） */
    var accentColor: Int? = null

    // enum を subject にした `when` は分岐表を持つ合成クラス（`WhenMappings`）を生成し、それが
    // Binding の公開束縛面へ現れる。二値の分岐は `if` で書いて合成クラス自体を作らせない。
    @JvmSynthetic
    override fun makeCell(id: String, relay: KsBridgeInteractionRelay): Cell {
        val pickerItems = KsBridgeValueTransport.pickerItems(items)
        return if (KsBridgeValueTransport.selectionMode(selectionMode) == PickerSelectionMode.Multiple) {
            PickerCell(
                id = id,
                style = resolvedStyle,
                title = title,
                description = descriptionText,
                valueText = valueText,
                icon = resolvedIcon,
                hintText = hintText,
                items = pickerItems,
                selectionMode = PickerSelectionMode.Multiple,
                selectedIndices = KsBridgeValueTransport.indexSet(selectedIndices),
                maxSelectedNumber = maxSelectedNumber,
                pageTitle = pageTitle,
                accentColor = KsBridgeColor.color(accentColor),
                onMultiSelectionChanged = { relay.pickerCellMultiSelectionChanged(id, it) },
                isEnabled = isEnabled,
                isVisible = isVisible,
            )
        } else {
            PickerCell(
                id = id,
                style = resolvedStyle,
                title = title,
                description = descriptionText,
                valueText = valueText,
                icon = resolvedIcon,
                hintText = hintText,
                items = pickerItems,
                selectionMode = PickerSelectionMode.Single,
                selectedIndex = selectedIndex,
                pageTitle = pageTitle,
                accentColor = KsBridgeColor.color(accentColor),
                onSelectionChanged = { relay.pickerCellSelectionChanged(id, it) },
                isEnabled = isEnabled,
                isVisible = isVisible,
            )
        }
    }
}
