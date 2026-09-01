package jp.kamusoft.kssettingsview.ui

import android.content.Context

/**
 * 入力系 Cell 5 種（EntryCell / PickerCell / NumberPickerCell / TimePickerCell / DatePickerCell）を
 * [KsCellRegistry] にまとめて登録する拡張関数。
 *
 * # viewType 割り当て
 *
 * 基本 Cell の 100 番台と衝突しないよう 110 番以降に割り当てる：
 * - 110: EntryCell
 * - 111: PickerCell
 * - 112: NumberPickerCell
 * - 113: TimePickerCell
 * - 114: DatePickerCell
 */
@Suppress("UNUSED_PARAMETER")
public fun KsCellRegistry.registerInputCells(context: Context) {
    register(
        cellClass = EntryCell::class,
        viewType = VIEW_TYPE_ENTRY_CELL,
        factory = { parent -> EntryCellViewHolder.create(parent) },
    )
    register(
        cellClass = PickerCell::class,
        viewType = VIEW_TYPE_PICKER_CELL,
        factory = { parent -> PickerCellViewHolder.create(parent) },
    )
    register(
        cellClass = NumberPickerCell::class,
        viewType = VIEW_TYPE_NUMBER_PICKER_CELL,
        factory = { parent -> NumberPickerCellViewHolder.create(parent) },
    )
    register(
        cellClass = TimePickerCell::class,
        viewType = VIEW_TYPE_TIME_PICKER_CELL,
        factory = { parent -> TimePickerCellViewHolder.create(parent) },
    )
    register(
        cellClass = DatePickerCell::class,
        viewType = VIEW_TYPE_DATE_PICKER_CELL,
        factory = { parent -> DatePickerCellViewHolder.create(parent) },
    )
}

internal const val VIEW_TYPE_ENTRY_CELL: Int = 110
internal const val VIEW_TYPE_PICKER_CELL: Int = 111
internal const val VIEW_TYPE_NUMBER_PICKER_CELL: Int = 112
internal const val VIEW_TYPE_TIME_PICKER_CELL: Int = 113
internal const val VIEW_TYPE_DATE_PICKER_CELL: Int = 114
