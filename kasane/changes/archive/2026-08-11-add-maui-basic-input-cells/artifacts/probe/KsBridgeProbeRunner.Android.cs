using Android.Graphics.Drawables;
using KsSettingsView.Bridge;
using AndroidLog = Android.Util.Log;

namespace KsSettingsView.IntegrationHost;

/// <summary>
/// probe 専用の実行時検証（add-maui-basic-input-cells / tasks 1.1〜1.5）。
/// binding 生成だけでなく実行時に通知と値が往復するかを logcat で確認する。
/// </summary>
internal static class KsBridgeProbeRunner
{
    /// <summary>logcat のタグ。</summary>
    private const string Tag = "KsBridgeProbe";

    /// <summary>probe の全項目を実行して結果を logcat へ出す。</summary>
    /// <param name="context">Drawable の生成に使う Context</param>
    public static void Run(Android.Content.Context context)
    {
        var probe = new KsBridgeProbe();

        // 1.2: C# 実装の listener を Kotlin 側へ渡して通知を受け取る
        var recorder = new ProbeListener();
        probe.Listener = recorder;
        probe.FireAll();
        AndroidLog.Info(Tag, $"1.2 listener received: {string.Join(" / ", recorder.Received)}");

        // 解除の検証
        recorder.Received.Clear();
        probe.Listener = null;
        probe.FireAll();
        AndroidLog.Info(Tag, $"1.2 after detach (期待: 空): count={recorder.Received.Count}");

        // 1.3 / 1.4 / 1.5: 異種 DTO を基底型のコレクションで混載し、Drawable と nullable scalar を載せる
        var label = new KsBridgeProbeLabelCell
        {
            Title = "ラベル",
            Icon = new ColorDrawable(Android.Graphics.Color.Red),
            IconSize = new Java.Lang.Double(24.0),
            UiStyle = null,
        };
        var toggle = new KsBridgeProbeSwitchCell
        {
            Title = "スイッチ",
            On = true,
            Icon = null,
            IconSize = null,
            UiStyle = new Java.Lang.Integer(1),
        };

        probe.Cells = new List<KsBridgeProbeCell> { label, toggle };
        AndroidLog.Info(Tag, $"1.3-1.5 setCells: {probe.DescribeCells()}");

        // 読み戻し (基底型のコレクションから派生型へ復元できるか)
        var readBack = probe.Cells;
        foreach (var cell in readBack)
        {
            AndroidLog.Info(Tag, $"1.4 readback: clrType={cell.GetType().Name} title={cell.Title}");
        }

        // addCell 経由の混載
        probe.Cells = new List<KsBridgeProbeCell>();
        probe.AddCell(label);
        probe.AddCell(toggle);
        AndroidLog.Info(Tag, $"1.4 addCell: {probe.DescribeCells()}");
    }

    /// <summary>C# 側の listener 実装。</summary>
    private sealed class ProbeListener : Java.Lang.Object, IKsBridgeProbeListener
    {
        /// <summary>受け取った通知の記録。</summary>
        public List<string> Received { get; } = [];

        /// <inheritdoc/>
        public void ProbeTapped(string cellId) => Received.Add($"tapped({cellId})");

        /// <inheritdoc/>
        public void ProbeSwitchChanged(string cellId, bool isOn) => Received.Add($"switch({cellId},{isOn})");

        /// <inheritdoc/>
        public void ProbeIndicesChanged(string cellId, int[] indices)
            => Received.Add($"indices({cellId},[{string.Join(',', indices)}])");

        /// <inheritdoc/>
        public void ProbeTimeChanged(string cellId, string time) => Received.Add($"time({cellId},{time})");
    }
}
