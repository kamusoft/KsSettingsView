using System;
using System.Windows.Input;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// Command の実行可否の変化を弱参照で観測者へ中継する購読。
/// </summary>
/// <remarks>
/// Command はふつう ViewModel が所有し Cell より長生きするため、中継から観測者への参照は弱くする。
/// 観測者への参照が生きていないときは、その場で Command の購読を外す。
/// </remarks>
/// <param name="observer">通知を受け取る観測者</param>
internal sealed class KsWeakCommandSubscription(IKsCommandObserver observer)
{
    private readonly WeakReference<IKsCommandObserver> _observer = new(observer);
    private ICommand? _source;

    /// <summary>指定した Command の実行可否の変化を購読する。</summary>
    /// <param name="source">購読する Command</param>
    public void Subscribe(ICommand source)
    {
        ArgumentNullException.ThrowIfNull(source);

        Unsubscribe();
        _source = source;
        source.CanExecuteChanged += OnCanExecuteChanged;
    }

    /// <summary>購読を解除する。解除済みでも安全に呼べる。</summary>
    public void Unsubscribe()
    {
        if (_source is null)
        {
            return;
        }

        _source.CanExecuteChanged -= OnCanExecuteChanged;
        _source = null;
    }

    private void OnCanExecuteChanged(object? sender, EventArgs args)
    {
        if (_observer.TryGetTarget(out IKsCommandObserver? observer))
        {
            observer.OnCommandCanExecuteChanged();
        }
        else
        {
            Unsubscribe();
        }
    }
}
