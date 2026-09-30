using System.Collections.ObjectModel;
using System.Windows.Input;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// スクロール制御デモの ViewModel。SettingsView から受け取った命令ハンドルで各操作の命令を出す。
/// </summary>
/// <remarks>
/// 命令ハンドルは <see cref="SettingsView.ScrollController"/> から OneWayToSource で
/// <see cref="Scroll"/> へ受け取り、命令が効くようになった時点は
/// <see cref="SettingsView.ScrollControllerReadyCommand"/> に渡した <see cref="ScrollReadyCommand"/>
/// で控える。準備が済む前の操作では命令を出さない。
/// </remarks>
public sealed class ScrollControlDemoViewModel : SampleViewModel
{
    /// <summary>「Section へ」の対象の Section の明示 ID。</summary>
    private const string TargetSectionId = "section-4";

    /// <summary>「Cell を中央へ」の対象の Cell の明示 ID。</summary>
    private const string TargetCellId = "cell-3-3";

    private IScrollController? _scroll;
    private bool _isScrollReady;

    /// <summary>ViewModel を作る。</summary>
    public ScrollControlDemoViewModel()
    {
        ScrollReadyCommand = new SampleCommand(_ => _isScrollReady = true);
        ScrollToEndCommand = new SampleCommand(_ => ReadyScroll?.ScrollToEnd());
        ScrollToSectionCommand = new SampleCommand(
            _ => ReadyScroll?.ScrollToSection(TargetSectionId, ScrollPosition.Start));
        ScrollToCellCommand = new SampleCommand(
            _ => ReadyScroll?.ScrollTo(TargetCellId, ScrollPosition.Center));
        AddAndScrollToEndCommand = new SampleCommand(_ => AddAndScrollToEnd());
        ScrollToStartCommand = new SampleCommand(_ => ReadyScroll?.ScrollToStart(animated: false));
    }

    /// <summary>SettingsView が作ったスクロール命令ハンドル。SettingsView から渡される。</summary>
    public IScrollController? Scroll
    {
        get => _scroll;
        set => Set(ref _scroll, value);
    }

    /// <summary>命令が効くようになったことを控えるコマンド。SettingsView が実行する。</summary>
    public ICommand ScrollReadyCommand { get; }

    /// <summary>「追加して末尾へ」で足した項目。最後の番号付き Section の末尾に並ぶ。</summary>
    public ObservableCollection<string> AddedItems { get; } = [];

    /// <summary>内容の末尾へ移る。</summary>
    public ICommand ScrollToEndCommand { get; }

    /// <summary>Section 4 の見出しを表示範囲の上端へ合わせる。</summary>
    public ICommand ScrollToSectionCommand { get; }

    /// <summary>項目 3-3 を表示範囲の中央へ合わせる。</summary>
    public ICommand ScrollToCellCommand { get; }

    /// <summary>最後の番号付き Section に項目を 1 つ足し、内容の末尾へ移る。</summary>
    public ICommand AddAndScrollToEndCommand { get; }

    /// <summary>アニメーションなしで内容の先頭へ移る。</summary>
    public ICommand ScrollToStartCommand { get; }

    /// <summary>準備が済んでいれば命令ハンドル、済んでいなければ null。</summary>
    private IScrollController? ReadyScroll => _isScrollReady ? Scroll : null;

    /// <summary>
    /// 項目を 1 つ足し、同じ処理の中で末尾への命令を出す。
    /// </summary>
    /// <remarks>
    /// 命令は足した項目が表示に反映された後に実行されるため、足した項目を含む末尾へ届く。
    /// </remarks>
    private void AddAndScrollToEnd()
    {
        AddedItems.Add($"追加した項目 {AddedItems.Count + 1}");
        ReadyScroll?.ScrollToEnd();
    }
}
