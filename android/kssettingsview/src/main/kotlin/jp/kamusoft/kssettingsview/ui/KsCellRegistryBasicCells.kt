package jp.kamusoft.kssettingsview.ui

import android.content.Context

/**
 * 基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell /
 * RadioCell / SimpleCheckCell）を [KsCellRegistry] にまとめて登録する拡張関数。
 *
 * 個別に [KsCellRegistry.register] を書く代わりに本 API を 1 度呼ぶだけで、
 * Sample アプリやユーザーアプリで全 7 種が利用できるようになる。
 *
 * # viewType 割り当て
 *
 * `CELL_VIEW_TYPE_MIN = 100` を起点に連番で割り当てる：
 * - 100: LabelCell
 * - 101: CommandCell
 * - 102: ButtonCell
 * - 103: SwitchCell
 * - 104: CheckboxCell
 * - 105: RadioCell
 * - 106: SimpleCheckCell
 *
 * すでに登録済みの Cell 型に対しては上書き登録（後勝ち）になる。
 *
 * @param context 現状は ViewHolder ファクトリが直接 [Context] を使わないため未使用だが、
 *   将来の Material アイコンリソース解決等のために引数として受け取る。
 */
@Suppress("UNUSED_PARAMETER")
fun KsCellRegistry.registerBasicCells(context: Context) {
    register(
        cellClass = LabelCell::class,
        viewType = VIEW_TYPE_LABEL_CELL,
        factory = { parent -> LabelCellViewHolder.create(parent) },
    )
    register(
        cellClass = CommandCell::class,
        viewType = VIEW_TYPE_COMMAND_CELL,
        factory = { parent -> CommandCellViewHolder.create(parent) },
    )
    register(
        cellClass = ButtonCell::class,
        viewType = VIEW_TYPE_BUTTON_CELL,
        factory = { parent -> ButtonCellViewHolder.create(parent) },
    )
    register(
        cellClass = SwitchCell::class,
        viewType = VIEW_TYPE_SWITCH_CELL,
        factory = { parent -> SwitchCellViewHolder.create(parent) },
    )
    register(
        cellClass = CheckboxCell::class,
        viewType = VIEW_TYPE_CHECKBOX_CELL,
        factory = { parent -> CheckboxCellViewHolder.create(parent) },
    )
    register(
        cellClass = RadioCell::class,
        viewType = VIEW_TYPE_RADIO_CELL,
        factory = { parent -> RadioCellViewHolder.create(parent) },
    )
    register(
        cellClass = SimpleCheckCell::class,
        viewType = VIEW_TYPE_SIMPLE_CHECK_CELL,
        factory = { parent -> SimpleCheckCellViewHolder.create(parent) },
    )
}

internal const val VIEW_TYPE_LABEL_CELL: Int = 100
internal const val VIEW_TYPE_COMMAND_CELL: Int = 101
internal const val VIEW_TYPE_BUTTON_CELL: Int = 102
internal const val VIEW_TYPE_SWITCH_CELL: Int = 103
internal const val VIEW_TYPE_CHECKBOX_CELL: Int = 104
internal const val VIEW_TYPE_RADIO_CELL: Int = 105
internal const val VIEW_TYPE_SIMPLE_CHECK_CELL: Int = 106
