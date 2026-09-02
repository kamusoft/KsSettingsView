using System;
using Android.Content;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Platform;
using AndroidView = Android.Views.View;
using MeasureSpec = Android.Views.View.MeasureSpec;
using MeasureSpecMode = Android.Views.MeasureSpecMode;
using ViewGroup = Android.Views.ViewGroup;

namespace KsSettingsView.Internals;

/// <summary>
/// accessory の View を包み、自分で計測・配置を行う platform view。
/// </summary>
/// <remarks>
/// accessory の領域はこの view の測定結果で高さを決めるため、包んだ View の必要サイズを
/// そのまま測定結果として返す。内容が変わって必要サイズが変わったときは自分の再配置を要求し、
/// あわせて呼び出し側へも伝えて行の高さの測り直しまで届ける。
/// </remarks>
internal sealed class KsAccessoryHostView : ViewGroup
{
    private readonly Context _context;
    private readonly View _view;
    private readonly Action _measureInvalidated;

    /// <summary>包んだ View を実体化して子として取り付ける。</summary>
    /// <param name="view">包む View</param>
    /// <param name="context">Handler の生成と Context の解決に使う MAUI のコンテキスト</param>
    /// <param name="measureInvalidated">必要サイズが変わったときに呼ぶ処理</param>
    public KsAccessoryHostView(View view, IMauiContext context, Action measureInvalidated)
        : base(context.Context!)
    {
        _context = context.Context!;
        _view = view;
        _measureInvalidated = measureInvalidated;

        AndroidView platformView = view.ToPlatform(context);
        platformView.RemoveFromParent();
        AddView(platformView);

        view.MeasureInvalidated += OnMeasureInvalidated;
    }

    /// <summary>包んだ View との結び付きを解き、子から外す。</summary>
    public void Detach()
    {
        _view.MeasureInvalidated -= OnMeasureInvalidated;
        RemoveAllViews();
    }

    /// <inheritdoc/>
    protected override void OnMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        double widthConstraint = widthMeasureSpec.ToDouble(_context);
        double heightConstraint = heightMeasureSpec.ToDouble(_context);
        Size measured = _view.Measure(widthConstraint, heightConstraint);

        // 確定した制約はそのまま採用し、それ以外の方向だけ内容の必要サイズを使う。
        double width = MeasureSpec.GetMode(widthMeasureSpec) == MeasureSpecMode.Exactly
            ? widthConstraint
            : measured.Width;
        double height = MeasureSpec.GetMode(heightMeasureSpec) == MeasureSpecMode.Exactly
            ? heightConstraint
            : measured.Height;

        SetMeasuredDimension((int)_context.ToPixels(width), (int)_context.ToPixels(height));
    }

    /// <inheritdoc/>
    protected override void OnLayout(bool changed, int l, int t, int r, int b)
        => _view.Arrange(_context.ToCrossPlatformRectInReferenceFrame(l, t, r, b));

    private void OnMeasureInvalidated(object? sender, EventArgs e)
    {
        RequestLayout();
        _measureInvalidated();
    }
}
