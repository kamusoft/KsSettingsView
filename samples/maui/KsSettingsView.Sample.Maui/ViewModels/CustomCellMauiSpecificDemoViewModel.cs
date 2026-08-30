using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Windows.Input;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// MAUI facade 固有の CustomCell の挙動を確認するデモページの ViewModel。
/// </summary>
/// <remarks>
/// 内容の View は所有 Cell の BindingContext を継承するため、ここのプロパティがそのまま
/// バインド元になる。View インスタンスの生成と差し替えはページ側が持ち、この ViewModel は
/// 表示する値だけを扱う。
/// </remarks>
public sealed class CustomCellMauiSpecificDemoViewModel : SampleViewModel
{
    private const string ShortText = "内容: 1 行";

    private const string LongText = """
        内容: 3 行
        高さが変わる内容変化を作るための 2 行目
        行の高さはこの行まで含めて伸びる
        """;

    private static int _createdCount;

    private readonly int _serial = Interlocked.Increment(ref _createdCount);

    private string _growingText = ShortText;
    private bool _handlerWasDetached;

    /// <summary>デモの表示値を作る。</summary>
    public CustomCellMauiSpecificDemoViewModel()
    {
        TemplateItems =
        [
            .. new[] { "テンプレート行 A", "テンプレート行 B", "テンプレート行 C" }
                .Select(static name => new CustomCellTemplateItem(name)),
        ];

        ToggleGrowingTextCommand = new SampleCommand(_ =>
            GrowingText = GrowingText == ShortText ? LongText : ShortText);
    }

    /// <summary>このページが何個目のインスタンスかを示す文言。</summary>
    /// <remarks>
    /// 番号が変わらないまま再表示されていれば、同じ SettingsView インスタンスで
    /// Handler の切断と再接続が起きたことになる。
    /// </remarks>
    public string InstanceText => $"{_serial} 個目のページインスタンス";

    /// <summary>ItemTemplate から生成する行の元データ。</summary>
    public IReadOnlyList<CustomCellTemplateItem> TemplateItems { get; }

    /// <summary>内容の行数を 1 行 ⇔ 3 行で切り替える。</summary>
    public ICommand ToggleGrowingTextCommand { get; }

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
            + "／離脱中に Content も差し替え";

    /// <summary>切断と再接続を 1 回数える。</summary>
    /// <param name="wasDetached">離脱中に Handler が切れていたかどうか</param>
    public void CountReconnect(bool wasDetached)
    {
        ReconnectCount++;
        _handlerWasDetached = wasDetached;
        OnPropertyChanged(nameof(ReconnectText));
    }
}

/// <summary>
/// ItemTemplate から生成される CustomCell 1 行分のデータ。
/// </summary>
/// <remarks>行ごとに独立した状態を持たせ、1 行の操作が他の行へ及ばないことを目視できるようにする。</remarks>
public sealed class CustomCellTemplateItem : SampleViewModel
{
    private int _count;

    /// <summary>行名からテンプレート行を作る。</summary>
    /// <param name="name">行の見出しに出す名前</param>
    public CustomCellTemplateItem(string name)
    {
        Name = name;
        IncrementCommand = new SampleCommand(_ => Count++);
    }

    /// <summary>行の見出し。</summary>
    public string Name { get; }

    /// <summary>内容の中のピルを押したときに、この行だけの回数を進める。</summary>
    public ICommand IncrementCommand { get; }

    /// <summary>ピルに表示する回数。</summary>
    public string CountText => $"{_count} 回";

    private int Count
    {
        get => _count;
        set
        {
            if (Set(ref _count, value))
            {
                OnPropertyChanged(nameof(CountText));
            }
        }
    }
}
