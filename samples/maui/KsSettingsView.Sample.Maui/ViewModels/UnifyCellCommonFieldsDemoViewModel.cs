namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>共通フィールド統合デモの各 Cell の状態を供給する ViewModel。</summary>
public sealed class UnifyCellCommonFieldsDemoViewModel : SampleViewModel
{
    private bool _notification = true;
    private bool _wifiOnlySync;
    private bool _agreedTerms;
    private bool _notice1 = true;
    private bool _notice2;
    private string _selectedTheme = "dark";

    /// <summary>SwitchCell「通知」の状態。</summary>
    public bool Notification
    {
        get => _notification;
        set
        {
            if (Set(ref _notification, value))
            {
                OnPropertyChanged(nameof(NotificationValueText));
            }
        }
    }

    /// <summary>SwitchCell「通知」の状態を表す値文字列。</summary>
    public string NotificationValueText => _notification ? "オン" : "オフ";

    /// <summary>SwitchCell「Wi-Fi のみ同期」の状態。</summary>
    public bool WifiOnlySync
    {
        get => _wifiOnlySync;
        set => Set(ref _wifiOnlySync, value);
    }

    /// <summary>CheckboxCell「規約に同意」の状態。</summary>
    public bool AgreedTerms
    {
        get => _agreedTerms;
        set => Set(ref _agreedTerms, value);
    }

    /// <summary>SimpleCheckCell「通知 1」の状態。</summary>
    public bool Notice1
    {
        get => _notice1;
        set => Set(ref _notice1, value);
    }

    /// <summary>SimpleCheckCell「通知 2」の状態。</summary>
    public bool Notice2
    {
        get => _notice2;
        set => Set(ref _notice2, value);
    }

    /// <summary>RadioCell グループ "theme" の選択値。</summary>
    public string SelectedTheme
    {
        get => _selectedTheme;
        set => Set(ref _selectedTheme, value);
    }
}
