using System;
using CoreGraphics;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Platform;
using UIKit;

namespace KsSettingsView.Internals;

/// <summary>
/// accessory の View を包み、自分で計測・配置を行う platform view。
/// </summary>
/// <remarks>
/// accessory の領域は包まれた view を上下左右に張り付ける制約で高さを決めるため、必要な高さは
/// <see cref="IntrinsicContentSize"/> で答える。<c>SizeThatFits</c> の応答だけでは制約に参加せず、
/// 領域が潰れてしまう。
/// 内容が変わって必要サイズが変わったときは Auto Layout へ知らせ、あわせて呼び出し側へも伝えて
/// 行の高さの測り直しまで届ける。
/// </remarks>
internal sealed class KsAccessoryHostView : MauiView, ICrossPlatformLayout
{
    private readonly View _view;
    private readonly Action _measureInvalidated;

    /// <summary>直近に必要サイズを求めたときの幅。</summary>
    private nfloat _measuredWidth = -1;

    /// <summary>包んだ View を実体化して子として取り付ける。</summary>
    /// <param name="view">包む View</param>
    /// <param name="context">Handler の生成に使う MAUI のコンテキスト</param>
    /// <param name="measureInvalidated">必要サイズが変わったときに呼ぶ処理</param>
    public KsAccessoryHostView(View view, IMauiContext context, Action measureInvalidated)
    {
        _view = view;
        _measureInvalidated = measureInvalidated;

        CrossPlatformLayout = this;
        ClipsToBounds = true;

        UIView platformView = view.ToPlatform(context);
        platformView.RemoveFromSuperview();
        AddSubview(platformView);

        view.MeasureInvalidated += OnMeasureInvalidated;
    }

    /// <inheritdoc/>
    public override CGSize IntrinsicContentSize
    {
        get
        {
            _measuredWidth = Bounds.Width;

            // 幅がまだ決まっていない間は内容が要求する幅で測る。決まった後はその幅に対する高さを返す。
            double widthConstraint = _measuredWidth > 0 ? _measuredWidth : double.PositiveInfinity;
            Size measured = _view.Measure(widthConstraint, double.PositiveInfinity);
            return new CGSize(NoIntrinsicMetric, measured.Height);
        }
    }

    /// <inheritdoc/>
    public Size CrossPlatformMeasure(double widthConstraint, double heightConstraint)
        => _view.Measure(widthConstraint, heightConstraint);

    /// <inheritdoc/>
    public Size CrossPlatformArrange(Rect bounds) => ((IView)_view).Arrange(bounds);

    /// <inheritdoc/>
    public override void LayoutSubviews()
    {
        // 幅が変わると必要な高さも変わるため、必要サイズの答えを取り直させる。
        if (Math.Abs(_measuredWidth - Bounds.Width) > 0.5)
        {
            _measuredWidth = Bounds.Width;
            InvalidateIntrinsicContentSize();
        }

        base.LayoutSubviews();
    }

    /// <summary>包んだ View との結び付きを解く。</summary>
    public void Detach() => _view.MeasureInvalidated -= OnMeasureInvalidated;

    private void OnMeasureInvalidated(object? sender, EventArgs e)
    {
        InvalidateIntrinsicContentSize();
        SetNeedsLayout();
        _measureInvalidated();
    }
}
