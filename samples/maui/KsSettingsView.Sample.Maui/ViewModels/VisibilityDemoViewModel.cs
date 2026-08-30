namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>isVisible デモの 6 つの表示トグルを供給する ViewModel。</summary>
public sealed class VisibilityDemoViewModel : SampleViewModel
{
    private bool _showTailCell = true;
    private bool _showMiddleCell = true;
    private bool _showTailSection = true;
    private bool _showMiddleSection = true;
    private bool _showHeader = true;
    private bool _showFooter = true;

    /// <summary>観察対象 Section A の末尾 Cell を表示するかどうか。</summary>
    public bool ShowTailCell
    {
        get => _showTailCell;
        set => Set(ref _showTailCell, value);
    }

    /// <summary>観察対象 Section A の中間 Cell を表示するかどうか。</summary>
    public bool ShowMiddleCell
    {
        get => _showMiddleCell;
        set => Set(ref _showMiddleCell, value);
    }

    /// <summary>末尾の Section C を表示するかどうか。</summary>
    public bool ShowTailSection
    {
        get => _showTailSection;
        set => Set(ref _showTailSection, value);
    }

    /// <summary>中間の Section B を表示するかどうか。</summary>
    public bool ShowMiddleSection
    {
        get => _showMiddleSection;
        set => Set(ref _showMiddleSection, value);
    }

    /// <summary>観察対象 Section D の Header を表示するかどうか。</summary>
    public bool ShowHeader
    {
        get => _showHeader;
        set => Set(ref _showHeader, value);
    }

    /// <summary>観察対象 Section D の Footer を表示するかどうか。</summary>
    public bool ShowFooter
    {
        get => _showFooter;
        set => Set(ref _showFooter, value);
    }
}
