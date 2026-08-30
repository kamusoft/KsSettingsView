using System;
using Microsoft.Maui.Dispatching;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// MAUI の <see cref="IDispatcher"/> を <see cref="IKsDispatcher"/> として使うためのアダプタ。
/// </summary>
/// <param name="dispatcher">実行を委譲する MAUI の dispatcher</param>
internal sealed class KsMauiDispatcher(IDispatcher dispatcher) : IKsDispatcher
{
    private readonly IDispatcher _dispatcher = dispatcher
        ?? throw new ArgumentNullException(nameof(dispatcher));

    /// <inheritdoc/>
    public bool Dispatch(Action action) => _dispatcher.Dispatch(action);
}
