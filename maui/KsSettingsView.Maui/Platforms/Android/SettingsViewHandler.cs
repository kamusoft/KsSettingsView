using System;
using KsSettingsView.Internals;
using Microsoft.Maui;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Platform;
using AndroidView = Android.Views.View;
using MeasureSpecMode = Android.Views.MeasureSpecMode;

namespace KsSettingsView.Handlers;

/// <summary>
/// Android の <see cref="SettingsViewHandler"/> の実体。
/// </summary>
/// <remarks>
/// Bridge が返す Native Host は View であり、そのまま platform view として返して
/// view 階層へ取り付けさせる。親子関係を結ぶ ViewController は存在しない。
/// root の header / footer は取り付け後に適用する必要があり、その順序は共通部が受け持つ。
/// </remarks>
public partial class SettingsViewHandler
{
    private partial AndroidView CreateHost()
    {
        KsBridgeGateway gateway = VirtualView.ConnectGateway(
            static () => new KsBridgeGateway(),
            new KsMauiDispatcher(VirtualView.Dispatcher),
            new KsImageResolver(MauiContext!),
            new KsViewMaterializer(MauiContext!));

        return gateway.MakeHost(Context)
            ?? throw new InvalidOperationException("Failed to create the native settings host view.");
    }

    /// <summary>
    /// 割り当てられる領域を満たす大きさを返す。
    /// </summary>
    /// <remarks>
    /// SettingsView は与えられた領域を満たす一覧であり、制約が定まっているかぎり内容ぶんの
    /// 大きさを Native Host へ問い合わせる必要がない。むしろ問い合わせは害になる — Android の
    /// 一覧は確定でない大きさで measure されると measure の途中で配置まで走らせ、そこで行の内部が
    /// 一時的に幅ゼロになる。Android は大きさがゼロになったフォーカス中の view のフォーカスを
    /// 外すため、入力欄が打鍵のたびにフォーカスを失う。大きさを制約から決めて Native Host の
    /// measure へ降りないことで、この経路自体を無くす。
    /// 制約が定まっていない方向がある場合だけは、内容ぶんの大きさを返せるのが Native Host しか
    /// いないため、既定の問い合わせに委ねる (この配置では上記の経路が残る)。
    /// 設計判断: maui/ADR-0014。
    /// </remarks>
    /// <param name="widthConstraint">横方向の制約</param>
    /// <param name="heightConstraint">縦方向の制約</param>
    /// <returns>求める大きさ</returns>
    public override Size GetDesiredSize(double widthConstraint, double heightConstraint)
    {
        IViewHandler handler = this;
        if (handler.VirtualView is IView view
            && KsFillMeasure.CanResolve(widthConstraint, heightConstraint))
        {
            return new Size(
                KsFillMeasure.ResolveLength(widthConstraint, view.Width, view.MinimumWidth, view.MaximumWidth),
                KsFillMeasure.ResolveLength(heightConstraint, view.Height, view.MinimumHeight, view.MaximumHeight));
        }

        return base.GetDesiredSize(widthConstraint, heightConstraint);
    }

    /// <summary>
    /// 割り当てられた領域へ Native Host を配置する。
    /// </summary>
    /// <remarks>
    /// Android の View は最後に measure された大きさで内部を配置するため、配置の直前に
    /// 割り当て領域を確定値として measure し直す。これをしないと Host 内部の一覧が
    /// 内容ぶんの大きさのまま残り、割り当て領域より狭く表示される。
    /// </remarks>
    /// <param name="frame">割り当てられた領域</param>
    public override void PlatformArrange(Rect frame)
    {
        if (PlatformView is { } host && Context is { } context)
        {
            host.Measure(
                AndroidView.MeasureSpec.MakeMeasureSpec((int)context.ToPixels(frame.Width), MeasureSpecMode.Exactly),
                AndroidView.MeasureSpec.MakeMeasureSpec((int)context.ToPixels(frame.Height), MeasureSpecMode.Exactly));
        }

        base.PlatformArrange(frame);
    }
}
