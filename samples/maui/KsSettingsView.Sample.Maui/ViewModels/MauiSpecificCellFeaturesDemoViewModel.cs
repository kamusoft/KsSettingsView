using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Input;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>MAUI facade 固有の PickerCell 機能を確認するデモページの ViewModel。</summary>
public sealed class MauiSpecificCellFeaturesDemoViewModel : SampleViewModel
{
    private object? _singleSelectedItem = "ダーク";
    private IList _multipleSelectedItems = new List<object> { "メール", "SMS" };
    private int _singleCompletionCount;
    private int _multipleCompletionCount;

    /// <summary>デモの初期値と選択完了 Command を作る。</summary>
    public MauiSpecificCellFeaturesDemoViewModel()
    {
        SingleSelectionCompletedCommand = new SampleCommand(_ =>
        {
            _singleCompletionCount++;
            OnPropertyChanged(nameof(SingleStatus));
        });
        MultipleSelectionCompletedCommand = new SampleCommand(_ =>
        {
            _multipleCompletionCount++;
            OnPropertyChanged(nameof(MultipleStatus));
        });
    }

    /// <summary>単一選択の候補。</summary>
    public IList<string> SingleItems { get; } = ["ライト", "ダーク", "自動"];

    /// <summary>複数選択の候補。</summary>
    public IList<string> MultipleItems { get; } = ["メール", "プッシュ", "SMS", "アプリ内"];

    /// <summary>単一選択のバインド先が保持する選択項目。</summary>
    public object? SingleSelectedItem
    {
        get => _singleSelectedItem;
        set
        {
            if (Set(ref _singleSelectedItem, value))
            {
                OnPropertyChanged(nameof(SingleStatus));
            }
        }
    }

    /// <summary>複数選択のバインド先が保持する選択要素列。</summary>
    public IList MultipleSelectedItems
    {
        get => _multipleSelectedItems;
        set
        {
            if (Set(ref _multipleSelectedItems, value ?? new List<object>()))
            {
                OnPropertyChanged(nameof(MultipleStatus));
            }
        }
    }

    /// <summary>単一選択の完了通知を受け取る Command。</summary>
    public ICommand SingleSelectionCompletedCommand { get; }

    /// <summary>複数選択の完了通知を受け取る Command。</summary>
    public ICommand MultipleSelectionCompletedCommand { get; }

    /// <summary>単一選択のバインド先と完了通知回数を示す文言。</summary>
    public string SingleStatus
        => $"バインド先: {SingleSelectedItem ?? "(未選択)"} / 完了通知: {_singleCompletionCount} 回";

    /// <summary>複数選択のバインド先と完了通知回数を示す文言。</summary>
    public string MultipleStatus
    {
        get
        {
            string[] items = [.. MultipleSelectedItems.Cast<object?>().Select(item => item?.ToString() ?? string.Empty)];
            string selection = items.Length == 0 ? "(未選択)" : string.Join(", ", items);
            return $"バインド先: {selection} / 完了通知: {_multipleCompletionCount} 回";
        }
    }
}
