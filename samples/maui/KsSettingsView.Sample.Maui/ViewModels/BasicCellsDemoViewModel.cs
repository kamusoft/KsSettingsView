using System.Windows.Input;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// 基本 Cell 7 種デモの状態と、直近の操作表示を供給する ViewModel。
/// </summary>
/// <remarks>
/// タップ系 Cell は <see cref="Tap"/> に行のタイトルを渡して直近の操作を記録し、値を持つ Cell は
/// 双方向バインドされたプロパティの setter で記録する。表示文字列は iOS / Android の同デモ画面と
/// 一致させるため、真偽値は "true" / "false" の小文字で表す。
/// </remarks>
public sealed class BasicCellsDemoViewModel : SampleViewModel
{
    private string _lastTapped = "(none)";
    private bool _notificationEnabled = true;
    private bool _agreedTerms = true;
    private string _selectedType = "TypeA";
    private bool _simpleCheck1 = true;
    private bool _simpleCheck2;
    private bool _simpleCheck3;

    /// <summary>ViewModel を作る。</summary>
    public BasicCellsDemoViewModel()
        => Tap = new Command<string>(title => LastTapped = title);

    /// <summary>画面上部に出す直近の操作。</summary>
    public string LastTapped
    {
        get => _lastTapped;
        private set => Set(ref _lastTapped, value);
    }

    /// <summary>SwitchCell「Notification」の状態。</summary>
    public bool NotificationEnabled
    {
        get => _notificationEnabled;
        set
        {
            if (Set(ref _notificationEnabled, value))
            {
                LastTapped = $"Notification → {Format(value)}";
            }
        }
    }

    /// <summary>CheckboxCell「Agree to Terms」の状態。</summary>
    public bool AgreedTerms
    {
        get => _agreedTerms;
        set
        {
            if (Set(ref _agreedTerms, value))
            {
                LastTapped = $"Agree → {Format(value)}";
            }
        }
    }

    /// <summary>RadioCell グループ "type" の選択値。</summary>
    public string SelectedType
    {
        get => _selectedType;
        set
        {
            if (Set(ref _selectedType, value))
            {
                LastTapped = $"Type → {value}";
            }
        }
    }

    /// <summary>SimpleCheckCell「Item 1」の状態。</summary>
    public bool SimpleCheck1
    {
        get => _simpleCheck1;
        set
        {
            if (Set(ref _simpleCheck1, value))
            {
                LastTapped = $"Item 1 → {Format(value)}";
            }
        }
    }

    /// <summary>SimpleCheckCell「Item 2」の状態。</summary>
    public bool SimpleCheck2
    {
        get => _simpleCheck2;
        set
        {
            if (Set(ref _simpleCheck2, value))
            {
                LastTapped = $"Item 2 → {Format(value)}";
            }
        }
    }

    /// <summary>SimpleCheckCell「Item 3」の状態。</summary>
    public bool SimpleCheck3
    {
        get => _simpleCheck3;
        set
        {
            if (Set(ref _simpleCheck3, value))
            {
                LastTapped = $"Item 3 → {Format(value)}";
            }
        }
    }

    /// <summary>タップされた行のタイトルを直近の操作として記録するコマンド。</summary>
    public ICommand Tap { get; }

    /// <summary>真偽値の表示形式。</summary>
    private static string Format(bool value) => value ? "true" : "false";
}
