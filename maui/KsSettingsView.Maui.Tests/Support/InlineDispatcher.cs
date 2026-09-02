using System;
using Microsoft.Maui.Dispatching;

namespace KsSettingsView.Tests.Support;

/// <summary>
/// 予約された処理をその場で実行する dispatcher。
/// </summary>
/// <remarks>
/// binding の書き戻しが起点になって元の ViewModel が変更通知を出すと、MAUI は通知を受けた
/// スレッドを判定するために dispatcher を要求する。ホストアプリのない単体テストでは
/// 取得元が無いため、この実装を <see cref="DispatcherProvider"/> の現在値として据える。
/// </remarks>
internal sealed class InlineDispatcher : IDispatcher, IDispatcherProvider
{
    /// <summary>この dispatcher を現在のスレッドの取得元として据える。</summary>
    public static void Install() => DispatcherProvider.SetCurrent(new InlineDispatcher());

    /// <inheritdoc/>
    public bool IsDispatchRequired => false;

    /// <inheritdoc/>
    public bool Dispatch(Action action)
    {
        ArgumentNullException.ThrowIfNull(action);
        action();
        return true;
    }

    /// <inheritdoc/>
    public bool DispatchDelayed(TimeSpan delay, Action action)
    {
        ArgumentNullException.ThrowIfNull(action);
        action();
        return true;
    }

    /// <inheritdoc/>
    public IDispatcherTimer CreateTimer() => throw new NotSupportedException();

    /// <inheritdoc/>
    public IDispatcher GetForCurrentThread() => this;
}
