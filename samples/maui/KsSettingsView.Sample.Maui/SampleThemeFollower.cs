using System;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// ページが表示されている間、実効外観の変化に合わせて配色を適用し直す仕掛け。
/// </summary>
/// <remarks>
/// 実効外観はルートメニューでの選択と端末の外観の両方で変わる。表示中のページも追随させるため、
/// <see cref="Application.RequestedThemeChanged"/> を購読して同じ適用処理を呼び直す。
/// 購読はページが画面に載っている間だけ持ち、外れたら解除する。
///
/// ページ側は生成時に <see cref="Attach"/> を 1 度呼ぶ。呼んだ時点で現在の実効外観が 1 度
/// 適用されるため、初回描画のために別途呼び出す必要はない。
/// </remarks>
public sealed class SampleThemeFollower
{
    private readonly Action<bool> _apply;

    // 購読先。解除は必ずこの参照に対して行い、購読の有無とフラグが食い違わないようにする。
    private Application? _subscribedTo;

    private SampleThemeFollower(Page page, Action<bool> apply)
    {
        ArgumentNullException.ThrowIfNull(page);
        ArgumentNullException.ThrowIfNull(apply);

        _apply = apply;

        page.Loaded += OnPageLoaded;
        page.Unloaded += OnPageUnloaded;

        _apply(SampleTheme.IsDark);
    }

    /// <summary>
    /// 適用処理をページの表示状態に結びつける。
    /// </summary>
    /// <remarks>
    /// 作った追随役はページのイベント購読から参照されるため、ページ側で保持する必要はない。
    /// </remarks>
    /// <param name="page">配色を追随させるページ</param>
    /// <param name="apply">実効外観 (ダークなら true) を受け取って配色を適用する処理</param>
    public static void Attach(Page page, Action<bool> apply) => new SampleThemeFollower(page, apply);

    private void OnPageLoaded(object? sender, EventArgs e)
    {
        _apply(SampleTheme.IsDark);

        if (_subscribedTo is not null || Application.Current is not { } application)
        {
            return;
        }

        application.RequestedThemeChanged += OnRequestedThemeChanged;
        _subscribedTo = application;
    }

    private void OnPageUnloaded(object? sender, EventArgs e)
    {
        if (_subscribedTo is not { } application)
        {
            return;
        }

        application.RequestedThemeChanged -= OnRequestedThemeChanged;
        _subscribedTo = null;
    }

    private void OnRequestedThemeChanged(object? sender, AppThemeChangedEventArgs e)
        => _apply(e.RequestedTheme == AppTheme.Dark);
}
