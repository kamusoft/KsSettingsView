using System.Collections.Generic;
using System.Linq;
using System.Windows.Input;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// CustomCell デモが表示する値を供給する ViewModel。
/// </summary>
/// <remarks>
/// 画面構成・文言は iOS / Android の同名デモ画面と一致させてある。対応する定義は
/// samples/ios/KsSettingsViewSample/CustomCellDemoView.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/CustomCellDemoScreen.kt。
/// </remarks>
public sealed class CustomCellDemoViewModel : SampleViewModel
{
    /// <summary>スクロール耐性のダミー行数。</summary>
    private const int DummyRowCount = 40;

    private int _brightness = 70;
    private int _volume = 40;
    private int _disabledValue = 60;
    private int _rowTapCount;

    /// <summary>デモの表示値を作る。</summary>
    public CustomCellDemoViewModel()
    {
        DummyItems =
        [
            .. Enumerable
                .Range(1, DummyRowCount)
                .Select(static index => new CustomCellDemoDummyItem(index)),
        ];

        CountRowTapCommand = new SampleCommand(_ => RowTapCount++);
        ResetRowTapCountCommand = new SampleCommand(_ => RowTapCount = 0);
    }

    /// <summary>展開行の本文 (2 行とも同じ本文を使う)。</summary>
    public string PolicyBody { get; } =
        "本アプリはお客様の設定情報を端末内にのみ保存します。収集した情報を第三者に提供することはありません。"
        + "設定のバックアップを有効にした場合のみ、暗号化した上でクラウドに保存します。";

    /// <summary>スクロール耐性のダミー行。</summary>
    public IReadOnlyList<CustomCellDemoDummyItem> DummyItems { get; }

    /// <summary>行タップでカウンタを 1 進める。</summary>
    public ICommand CountRowTapCommand { get; }

    /// <summary>カウンタを 0 に戻す。</summary>
    public ICommand ResetRowTapCountCommand { get; }

    /// <summary>「明るさ」スライダーの値。</summary>
    public int Brightness
    {
        get => _brightness;
        set => Set(ref _brightness, value);
    }

    /// <summary>「音量」スライダーの値。</summary>
    public int Volume
    {
        get => _volume;
        set => Set(ref _volume, value);
    }

    /// <summary>「無効」スライダーの値。</summary>
    public int DisabledValue
    {
        get => _disabledValue;
        set => Set(ref _disabledValue, value);
    }

    /// <summary>ピルに表示する行タップの回数。</summary>
    public string RowTapCountText => $"{_rowTapCount} 回";

    private int RowTapCount
    {
        get => _rowTapCount;
        set
        {
            if (Set(ref _rowTapCount, value))
            {
                OnPropertyChanged(nameof(RowTapCountText));
            }
        }
    }
}

/// <summary>
/// スクロール耐性のダミー行 1 件。
/// </summary>
/// <remarks>
/// 行ごとに独立した状態とコマンドを持たせ、スクロールで行が再利用されても表示と通知先が
/// 混ざらないことを目視できるようにする。
/// </remarks>
public sealed class CustomCellDemoDummyItem : SampleViewModel
{
    private readonly string _number;

    private bool _isTapped;

    /// <summary>連番からダミー行を作る。</summary>
    /// <param name="index">1 起点の連番</param>
    public CustomCellDemoDummyItem(int index)
    {
        _number = index.ToString("D2");
        Title = $"ダミー行 #{_number}";
        Subtitle = $"content: DummyItem({index})";
        DotColor = SampleTheme.DemoAccentPalette[(index - 1) % SampleTheme.DemoAccentPalette.Count];
        ToggleCommand = new SampleCommand(_ => IsTapped = !IsTapped);
    }

    /// <summary>行の 1 行目。</summary>
    public string Title { get; }

    /// <summary>行の 2 行目。</summary>
    public string Subtitle { get; }

    /// <summary>行頭のドットの色。</summary>
    public Color DotColor { get; }

    /// <summary>行タップでタップ済み状態を切り替える。</summary>
    public ICommand ToggleCommand { get; }

    /// <summary>ピルに表示する連番。タップ済みなら印が付く。</summary>
    public string TagText => _isTapped ? $"#{_number} ✓" : $"#{_number}";

    private bool IsTapped
    {
        get => _isTapped;
        set
        {
            if (Set(ref _isTapped, value))
            {
                OnPropertyChanged(nameof(TagText));
            }
        }
    }
}
