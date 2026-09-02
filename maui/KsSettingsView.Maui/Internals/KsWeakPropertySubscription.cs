using System;
using System.ComponentModel;

namespace KsSettingsView.Internals;

/// <summary>
/// プロパティの変更通知を弱参照で観測者へ中継する購読。
/// </summary>
/// <remarks>
/// 通知元はこの中継を強く保持するが、中継から観測者への参照は弱い。外部 (ViewModel 等) が
/// Section / Cell を保持し続けても、購読が観測者を巻き添えで生かし続けることはない。
/// 観測者への参照が生きていないときは、その場で通知元の購読を外す。
/// </remarks>
/// <param name="observer">通知を受け取る観測者</param>
internal sealed class KsWeakPropertySubscription(IKsPropertyObserver observer)
{
    private readonly WeakReference<IKsPropertyObserver> _observer = new(observer);
    private INotifyPropertyChanged? _source;

    /// <summary>指定したオブジェクトのプロパティ変更通知を購読する。</summary>
    /// <param name="source">購読するオブジェクト</param>
    public void Subscribe(INotifyPropertyChanged source)
    {
        ArgumentNullException.ThrowIfNull(source);

        Unsubscribe();
        _source = source;
        source.PropertyChanged += OnPropertyChanged;
    }

    /// <summary>購読を解除する。解除済みでも安全に呼べる。</summary>
    public void Unsubscribe()
    {
        if (_source is null)
        {
            return;
        }

        _source.PropertyChanged -= OnPropertyChanged;
        _source = null;
    }

    private void OnPropertyChanged(object? sender, PropertyChangedEventArgs args)
    {
        if (sender is null)
        {
            return;
        }

        if (_observer.TryGetTarget(out IKsPropertyObserver? observer))
        {
            observer.OnObservedPropertyChanged(sender, args.PropertyName);
        }
        else
        {
            Unsubscribe();
        }
    }
}
