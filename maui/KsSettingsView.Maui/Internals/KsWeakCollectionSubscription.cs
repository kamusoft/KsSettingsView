using System;
using System.Collections.Specialized;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// コレクションの変更通知を弱参照で観測者へ中継する購読。
/// </summary>
/// <remarks>
/// 通知元はこの中継を強く保持するが、中継から観測者への参照は弱い。外部 (ViewModel 等) が
/// コレクションを保持し続けても、購読が観測者を巻き添えで生かし続けることはない。
/// 観測者への参照が生きていないときは、その場で通知元の購読を外す。
/// </remarks>
/// <param name="observer">通知を受け取る観測者</param>
/// <param name="context">どのコレクションからの通知かを観測者が識別するための目印</param>
internal sealed class KsWeakCollectionSubscription(IKsCollectionObserver observer, object? context)
{
    private readonly WeakReference<IKsCollectionObserver> _observer = new(observer);
    private INotifyCollectionChanged? _source;

    /// <summary>観測者へ渡す目印。</summary>
    public object? Context { get; } = context;

    /// <summary>指定したコレクションの変更通知を購読する。</summary>
    /// <param name="source">購読するコレクション</param>
    public void Subscribe(INotifyCollectionChanged source)
    {
        ArgumentNullException.ThrowIfNull(source);

        Unsubscribe();
        _source = source;
        source.CollectionChanged += OnCollectionChanged;
    }

    /// <summary>購読を解除する。解除済みでも安全に呼べる。</summary>
    public void Unsubscribe()
    {
        if (_source is null)
        {
            return;
        }

        _source.CollectionChanged -= OnCollectionChanged;
        _source = null;
    }

    private void OnCollectionChanged(object? sender, NotifyCollectionChangedEventArgs args)
    {
        if (_observer.TryGetTarget(out IKsCollectionObserver? observer))
        {
            observer.OnObservedCollectionChanged(Context, args);
        }
        else
        {
            Unsubscribe();
        }
    }
}
