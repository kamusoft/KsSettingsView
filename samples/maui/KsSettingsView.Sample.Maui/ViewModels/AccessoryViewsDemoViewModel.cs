using System.Threading;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// Header / Footer へ置いた View がバインドで受け取る値を供給する ViewModel。
/// </summary>
/// <remarks>
/// accessory へ置いた View は所有者 (SettingsView または Section) の BindingContext を継承するため、
/// ここのプロパティがそのまま View のバインド元になる。View インスタンスの生成と差し替えは
/// View 層の責務としてページ側が持ち、この ViewModel は表示する値だけを扱う。
/// </remarks>
public sealed class AccessoryViewsDemoViewModel : SampleViewModel
{
    private const string ShortText = "内容: 1 行";

    private const string LongText = """
        内容: 3 行
        高さが変わる内容変化を作るための 2 行目
        自動高さの領域はこの行まで含めて伸びる
        """;

    private static int _createdCount;

    private readonly int _serial = Interlocked.Increment(ref _createdCount);

    private string _boundText = "ViewModel の初期値";
    private string _growingText = ShortText;
    private int _boundTextCount;
    private bool _handlerWasDetached;

    /// <summary>このページが何個目のインスタンスかを示す文言。</summary>
    /// <remarks>
    /// 番号が変わらないまま再表示されていれば、同じ SettingsView インスタンスで
    /// Handler の切断と再接続が起きたことになる。
    /// </remarks>
    public string InstanceText => $"{_serial} 個目のページインスタンス";

    /// <summary>Section の Header View がバインドで表示する文字列。</summary>
    public string BoundText
    {
        get => _boundText;
        private set => Set(ref _boundText, value);
    }

    /// <summary>高さ追従を観察するための、行数が変わる文字列。</summary>
    public string GrowingText
    {
        get => _growingText;
        private set => Set(ref _growingText, value);
    }

    /// <summary>切断と再接続を行った回数。</summary>
    public int ReconnectCount { get; private set; }

    /// <summary>切断と再接続の結果を示す文言。</summary>
    public string ReconnectText => ReconnectCount == 0
        ? "再接続はまだ行っていません"
        : $"{ReconnectCount} 回目の再接続後／離脱中の Handler: {(_handlerWasDetached ? "切断" : "接続のまま")}"
            + "／離脱中に Root Header View を差し替え";

    /// <summary>バインド元の値を書き換える。</summary>
    /// <remarks>View インスタンスはそのままなので、内容変化だけが表示へ届く。</remarks>
    public void ChangeBoundText()
        => BoundText = $"ViewModel から更新: {++_boundTextCount} 回目";

    /// <summary>高さ追従用の文字列を 1 行と 3 行の間で切り替える。</summary>
    public void ToggleGrowingText()
        => GrowingText = GrowingText == ShortText ? LongText : ShortText;

    /// <summary>再接続の回数を 1 つ進める。</summary>
    /// <param name="handlerWasDetached">離脱中に Handler が切れていたなら true</param>
    public void CountReconnect(bool handlerWasDetached)
    {
        ReconnectCount++;
        _handlerWasDetached = handlerWasDetached;
        OnPropertyChanged(nameof(ReconnectCount));
        OnPropertyChanged(nameof(ReconnectText));
    }
}
