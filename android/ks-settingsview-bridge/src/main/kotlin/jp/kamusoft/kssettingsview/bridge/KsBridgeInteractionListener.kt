package jp.kamusoft.kssettingsview.bridge

/**
 * Bridge が表示中の Cell に対するユーザー操作を通知する listener。
 *
 * Bridge instance あたり 1 個の通知チャネルで全 Cell の操作を運び、Cell 種別はメソッド名で
 * 識別する（maui/ADR-0003）。通知は cellID と新しい値を引数に取り、値の表現は interop 境界の
 * 輸送規約に従う（maui/ADR-0012）— 二値は Boolean、数値と選択 index は Int、複数選択は昇順・
 * 重複なしの Int 配列、時刻は `"HH:mm"`、日付は `"yyyy-MM-dd"` の文字列。
 *
 * 通知は Native の UI スレッド上で同期に呼ばれる。Bridge は listener を強参照で保持するため、
 * 不要になったら `KsSettingsBridge.interactionListener` へ `null` を設定して解除する。
 */
interface KsBridgeInteractionListener {

    /**
     * CommandCell がタップされた。
     *
     * @param cellID 対象 Cell の cellID
     */
    fun commandCellTapped(cellID: String)

    /**
     * ButtonCell がタップされた。
     *
     * @param cellID 対象 Cell の cellID
     */
    fun buttonCellTapped(cellID: String)

    /**
     * CustomCell の行がタップされた。
     *
     * タップ通知を持たせずに構築された CustomCell は行タップ動作そのものを持たないため、この
     * メソッドは呼ばれない。
     *
     * @param cellID 対象 Cell の cellID
     */
    fun customCellTapped(cellID: String)

    /**
     * SwitchCell の値が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param isOn 新しい ON/OFF 値
     */
    fun switchCellChanged(cellID: String, isOn: Boolean)

    /**
     * CheckboxCell の値が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param isChecked 新しいチェック状態
     */
    fun checkboxCellChanged(cellID: String, isChecked: Boolean)

    /**
     * SimpleCheckCell の値が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param isChecked 新しいチェック状態
     */
    fun simpleCheckCellChanged(cellID: String, isChecked: Boolean)

    /**
     * RadioCell が選択された。
     *
     * @param cellID 選択された Cell の cellID
     * @param value 選択された Cell の値
     */
    fun radioCellSelected(cellID: String, value: String)

    /**
     * EntryCell のテキストが変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param text 新しいテキスト
     */
    fun entryCellTextChanged(cellID: String, text: String)

    /**
     * PickerCell（単一選択）の選択が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param index 新しい選択 index
     */
    fun pickerCellSelectionChanged(cellID: String, index: Int)

    /**
     * PickerCell（複数選択）の選択が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param indices 新しい選択 index（昇順・重複なし）
     */
    fun pickerCellMultiSelectionChanged(cellID: String, indices: IntArray)

    /**
     * NumberPickerCell の値が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param value 新しい数値
     */
    fun numberPickerCellChanged(cellID: String, value: Int)

    /**
     * TimePickerCell の時刻が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param time 新しい時刻（`"HH:mm"`）
     */
    fun timePickerCellChanged(cellID: String, time: String)

    /**
     * DatePickerCell の日付が変わった。
     *
     * @param cellID 対象 Cell の cellID
     * @param date 新しい日付（`"yyyy-MM-dd"`）
     */
    fun datePickerCellChanged(cellID: String, date: String)
}
