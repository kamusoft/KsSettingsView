using Foundation;
using KsSettingsView.Bridge;
using UIKit;

namespace KsSettingsView.IntegrationHost;

/// <summary>
/// probe 専用の実行時検証（add-maui-basic-input-cells / tasks 1.1〜1.5）。
/// binding 生成だけでなく実行時に通知と値が往復するかを Console 出力で確認する。
/// </summary>
internal static class KsBridgeProbeRunner
{
    /// <summary>出力タグ。</summary>
    private const string Tag = "KsBridgeProbe";

    /// <summary>probe の全項目を実行して結果を出力する。</summary>
    public static void Run()
    {
        var probe = new KsBridgeProbe();

        // 1.1: C# 実装の delegate を Swift 側へ渡して通知を受け取る
        var recorder = new ProbeDelegate();
        probe.Delegate = recorder;
        Console.WriteLine($"{Tag}: 1.1 hasDelegate after set = {probe.HasDelegate}");
        probe.FireAll();
        Console.WriteLine($"{Tag}: 1.1 delegate received: {string.Join(" / ", recorder.Received)}");

        // 解除の検証
        recorder.Received.Clear();
        probe.Delegate = null;
        probe.FireAll();
        Console.WriteLine($"{Tag}: 1.1 after detach (期待: 0) count={recorder.Received.Count} hasDelegate={probe.HasDelegate}");

        // weak 保持の検証: C# 側が参照を落とすと delegate が消えるか
        probe.Delegate = new ProbeDelegate();
        GC.Collect();
        GC.WaitForPendingFinalizers();
        GC.Collect();
        Console.WriteLine($"{Tag}: 1.1 weak 保持 (参照落とし後) hasDelegate={probe.HasDelegate}");

        // 以降は強参照を保った delegate で継続する
        probe.Delegate = recorder;

        // 1.3 / 1.4 / 1.5: 異種 DTO を基底型の配列で混載し、UIImage と nullable scalar を載せる
        var label = new KsBridgeProbeLabelCell("ラベル")
        {
            Icon = MakeImage(24, 24),
            IconSize = NSNumber.FromDouble(24.0),
            UiStyle = null,
        };
        var toggle = new KsBridgeProbeSwitchCell("スイッチ", true)
        {
            Icon = null,
            IconSize = null,
            UiStyle = NSNumber.FromInt32(1),
        };
        Console.WriteLine($"{Tag}: 1.4 ctor: label.CellID={label.CellID[..8]} switch.CellID={toggle.CellID[..8]}");

        probe.Cells = [label, toggle];
        Console.WriteLine($"{Tag}: 1.3-1.5 setCells: {probe.DescribeCells()}");

        foreach (var cell in probe.Cells)
        {
            Console.WriteLine($"{Tag}: 1.4 readback: clrType={cell.GetType().Name} title={cell.Title}");
        }

        probe.Cells = [];
        probe.AddCell(label);
        probe.AddCell(toggle);
        Console.WriteLine($"{Tag}: 1.4 addCell: {probe.DescribeCells()}");

        GC.KeepAlive(recorder);
    }

    /// <summary>指定サイズの単色 UIImage を作る。</summary>
    /// <param name="width">幅 (pt)</param>
    /// <param name="height">高さ (pt)</param>
    private static UIImage MakeImage(int width, int height)
    {
        var renderer = new UIGraphicsImageRenderer(new CoreGraphics.CGSize(width, height));
        return renderer.CreateImage(context =>
        {
            UIColor.Red.SetFill();
            context.FillRect(new CoreGraphics.CGRect(0, 0, width, height));
        });
    }

    /// <summary>C# 側の delegate 実装。</summary>
    private sealed class ProbeDelegate : KsBridgeProbeDelegate
    {
        /// <summary>受け取った通知の記録。</summary>
        public List<string> Received { get; } = [];

        /// <inheritdoc/>
        public override void ProbeTapped(string cellID) => Received.Add($"tapped({cellID})");

        /// <inheritdoc/>
        public override void ProbeSwitchChanged(string cellID, bool isOn) => Received.Add($"switch({cellID},{isOn})");

        /// <inheritdoc/>
        public override void ProbeIndicesChanged(string cellID, NSNumber[] indices)
            => Received.Add($"indices({cellID},[{string.Join(',', indices.Select(n => n.Int32Value))}])");

        /// <inheritdoc/>
        public override void ProbeTimeChanged(string cellID, string time) => Received.Add($"time({cellID},{time})");
    }
}
