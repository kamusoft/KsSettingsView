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
/// 高さの答えは最初から折り返し後の値になるよう、幅が分かっている側の幅で測る (maui/ADR-0028)。
/// 幅付きで問い合わせる経路 (<see cref="SizeThatFits"/>) と自分で幅を決める経路
/// (<see cref="IntrinsicContentSize"/>) の両方が同じ幅に対して同じ高さを答える。
/// </remarks>
internal sealed class KsAccessoryHostView : MauiView, ICrossPlatformLayout
{
    private readonly View _view;
    private readonly Action _measureInvalidated;

    /// <summary>直近に必要サイズを問われたときの自分の幅 (親の幅で測った場合も自分の幅を控える)。</summary>
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

            Size measured = _view.Measure(ResolveWidthConstraint(), double.PositiveInfinity);
            return new CGSize(NoIntrinsicMetric, measured.Height);
        }
    }

    /// <inheritdoc/>
    public override CGSize SizeThatFits(CGSize size)
    {
        // 高さの提案は上限なしとして解釈する (有限の巨大値も同じ扱い)。折り返し後の高さを知りたい
        // 問い合わせであり、有限の高さ制約を通すと内容によっては希望高が変わるため、幅だけを制約に
        // する。これで幅が同じなら IntrinsicContentSize と同じ高さを答える (maui/ADR-0028)。
        return base.SizeThatFits(new CGSize(size.Width, nfloat.PositiveInfinity));
    }

    /// <inheritdoc/>
    public Size CrossPlatformMeasure(double widthConstraint, double heightConstraint)
        => _view.Measure(widthConstraint, heightConstraint);

    /// <summary>必要な高さを求めるときに使う幅の制約を決める。</summary>
    /// <remarks>
    /// 領域に取り付いた直後は自分の幅がまだ 0 で、そのまま測ると折り返す内容が 1 行ぶんの高さを
    /// 答えてしまう。自分の幅が決まる前は領域の幅を持つ親の幅を使い、最初の答えから折り返し後の
    /// 高さになるようにする (maui/ADR-0028)。どちらも無ければ内容が要求する幅で測る。
    /// </remarks>
    /// <returns>幅の制約</returns>
    private double ResolveWidthConstraint()
    {
        nfloat ownWidth = Bounds.Width;
        if (ownWidth > 0)
        {
            return ownWidth;
        }

        nfloat superviewWidth = Superview?.Bounds.Width ?? 0;
        return superviewWidth > 0 ? superviewWidth : double.PositiveInfinity;
    }

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
        // 計測結果は幅・高さの制約の組ごとに 1 段だけ控えられる。必要サイズの無効化だけでは
        // その控えが残り、幅付きの問い合わせが内容の変化前の高さを答えてしまう (maui/ADR-0028)。
        InvalidateConstraintsCache();
        InvalidateIntrinsicContentSize();
        SetNeedsLayout();
        _measureInvalidated();
    }
}
