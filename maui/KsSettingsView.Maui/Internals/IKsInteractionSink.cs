using System.Collections.Generic;

namespace KsSettingsView.Internals;

/// <summary>
/// Native で起きたユーザー操作を facade へ届ける受け口 (maui/ADR-0003)。
/// </summary>
/// <remarks>
/// Bridge の通知チャネルと 1 対 1 に対応し、Cell 種別はメソッドで識別する。値は interop 境界の
/// 輸送表現のまま渡され (壁時計値は固定書式の文字列、選択は index)、facade 型への解釈は実装が担う。
/// 未知の cellId・Cell 種別の食い違いは何も起こさない。
/// 全メソッドは Native の UI スレッド上で同期に呼ばれる。
/// </remarks>
internal interface IKsInteractionSink
{
    /// <summary>CommandCell がタップされた。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    void CommandCellTapped(string cellId);

    /// <summary>ButtonCell がタップされた。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    void ButtonCellTapped(string cellId);

    /// <summary>CustomCell の行がタップされた。</summary>
    /// <remarks>
    /// タップ通知を持たない CustomCell は行タップ動作そのものを持たないため、この通知は届かない。
    /// </remarks>
    /// <param name="cellId">対象 Cell の ID</param>
    void CustomCellTapped(string cellId);

    /// <summary>SwitchCell の値が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="isOn">新しい ON/OFF 値</param>
    void SwitchCellChanged(string cellId, bool isOn);

    /// <summary>CheckboxCell の値が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="isChecked">新しいチェック状態</param>
    void CheckboxCellChanged(string cellId, bool isChecked);

    /// <summary>SimpleCheckCell の値が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="isChecked">新しいチェック状態</param>
    void SimpleCheckCellChanged(string cellId, bool isChecked);

    /// <summary>RadioCell が選択された。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="value">選択された値</param>
    void RadioCellSelected(string cellId, string value);

    /// <summary>EntryCell のテキストが変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="text">新しいテキスト</param>
    void EntryCellTextChanged(string cellId, string text);

    /// <summary>PickerCell (単一選択) の選択が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="index">選択された位置</param>
    void PickerCellSelectionChanged(string cellId, int index);

    /// <summary>PickerCell (複数選択) の選択が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="indices">選択された位置の並び</param>
    void PickerCellMultiSelectionChanged(string cellId, IReadOnlyList<int> indices);

    /// <summary>NumberPickerCell の値が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="value">新しい値</param>
    void NumberPickerCellChanged(string cellId, int value);

    /// <summary>TimePickerCell の時刻が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="time">新しい時刻 ("HH:mm")</param>
    void TimePickerCellChanged(string cellId, string time);

    /// <summary>DatePickerCell の日付が変わった。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="date">新しい日付 ("yyyy-MM-dd")</param>
    void DatePickerCellChanged(string cellId, string date);
}
