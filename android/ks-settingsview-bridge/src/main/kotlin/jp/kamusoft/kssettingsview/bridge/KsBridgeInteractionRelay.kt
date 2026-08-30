package jp.kamusoft.kssettingsview.bridge

import java.time.LocalDate
import java.time.LocalTime

/**
 * Cell のコールバックと [KsBridgeInteractionListener] の間に立つ中継。
 *
 * Cell のコールバック閉包はこの中継だけを掴み、listener も Bridge も直接掴まない。listener の
 * 差し替え・解除は生成済みの Cell にそのまま反映され、未設定・解除後の通知は黙って破棄される。
 *
 * 通知は Native の UI スレッド上で同期に届く経路でのみ使う。Bridge の全 API と同じスレッド契約
 * （maui/ADR-0005）に従うため、中継自身は同期化を行わない。
 */
internal class KsBridgeInteractionRelay {

    /** 通知先。未設定・解除後は `null` で、通知は破棄される。 */
    var listener: KsBridgeInteractionListener? = null

    /** CommandCell のタップを転送する。 */
    fun commandCellTapped(cellID: String) {
        listener?.commandCellTapped(cellID)
    }

    /** ButtonCell のタップを転送する。 */
    fun buttonCellTapped(cellID: String) {
        listener?.buttonCellTapped(cellID)
    }

    /** CustomCell の行タップを転送する。 */
    fun customCellTapped(cellID: String) {
        listener?.customCellTapped(cellID)
    }

    /** SwitchCell の値変更を転送する。 */
    fun switchCellChanged(cellID: String, isOn: Boolean) {
        listener?.switchCellChanged(cellID, isOn)
    }

    /** CheckboxCell の値変更を転送する。 */
    fun checkboxCellChanged(cellID: String, isChecked: Boolean) {
        listener?.checkboxCellChanged(cellID, isChecked)
    }

    /** SimpleCheckCell の値変更を転送する。 */
    fun simpleCheckCellChanged(cellID: String, isChecked: Boolean) {
        listener?.simpleCheckCellChanged(cellID, isChecked)
    }

    /** RadioCell の選択を転送する。 */
    fun radioCellSelected(cellID: String, value: String) {
        listener?.radioCellSelected(cellID, value)
    }

    /** EntryCell のテキスト変更を転送する。 */
    fun entryCellTextChanged(cellID: String, text: String) {
        listener?.entryCellTextChanged(cellID, text)
    }

    /** PickerCell（単一選択）の選択変更を転送する。 */
    fun pickerCellSelectionChanged(cellID: String, index: Int) {
        listener?.pickerCellSelectionChanged(cellID, index)
    }

    /** PickerCell（複数選択）の選択変更を、昇順・重複なしへ正規化して転送する。 */
    fun pickerCellMultiSelectionChanged(cellID: String, indices: Set<Int>) {
        listener?.pickerCellMultiSelectionChanged(
            cellID,
            KsBridgeValueTransport.indexList(indices),
        )
    }

    /** NumberPickerCell の値変更を転送する。 */
    fun numberPickerCellChanged(cellID: String, value: Int) {
        listener?.numberPickerCellChanged(cellID, value)
    }

    /** TimePickerCell の時刻変更を輸送書式の文字列へ変換して転送する。 */
    fun timePickerCellChanged(cellID: String, time: LocalTime) {
        listener?.timePickerCellChanged(cellID, KsBridgeValueTransport.timeText(time))
    }

    /** DatePickerCell の日付変更を輸送書式の文字列へ変換して転送する。 */
    fun datePickerCellChanged(cellID: String, date: LocalDate) {
        listener?.datePickerCellChanged(cellID, KsBridgeValueTransport.dateText(date))
    }
}
