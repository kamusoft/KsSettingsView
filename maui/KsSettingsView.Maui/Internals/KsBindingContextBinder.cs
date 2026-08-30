using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// コレクションの要素へ、持ち主の BindingContext を配る器。
/// </summary>
/// <remarks>
/// XAML で直接並べた Section / Cell にもページの BindingContext が届くようにする。
/// 要素自身に BindingContext が明示設定されている場合 (ItemsSource から生成した要素など) は
/// そちらが優先され、ここからの配布では上書きされない。
/// コレクションの購読は弱参照で張り、外部がコレクションを保持し続けても持ち主を
/// 巻き添えで生かし続けない。要素から持ち主への参照は作らない。
/// </remarks>
/// <typeparam name="T">配布先の要素の型</typeparam>
/// <param name="owner">BindingContext の供給元</param>
/// <param name="targetProvider">配布先コレクションを返す関数</param>
internal sealed class KsBindingContextBinder<T>(BindableObject owner, Func<IList<T>?> targetProvider)
    : IKsCollectionObserver
    where T : BindableObject
{
    private readonly BindableObject _owner = owner;
    private readonly Func<IList<T>?> _targetProvider = targetProvider;

    private KsWeakCollectionSubscription? _subscription;

    /// <summary>配布先コレクションが差し替わったことを伝え、購読と配布をやり直す。</summary>
    public void OnTargetChanged()
    {
        _subscription?.Unsubscribe();
        _subscription = null;

        if (_targetProvider() is INotifyCollectionChanged observable)
        {
            KsWeakCollectionSubscription subscription = new(this, null);
            subscription.Subscribe(observable);
            _subscription = subscription;
        }

        Apply();
    }

    /// <summary>現在のコレクション全体へ持ち主の BindingContext を配る。</summary>
    public void Apply()
    {
        if (_targetProvider() is not { } target)
        {
            return;
        }

        foreach (T child in target)
        {
            Distribute(child);
        }
    }

    /// <inheritdoc/>
    public void OnObservedCollectionChanged(object? context, NotifyCollectionChangedEventArgs args)
    {
        if (args.Action == NotifyCollectionChangedAction.Reset)
        {
            Apply();
            return;
        }

        if (args.NewItems is null)
        {
            return;
        }

        foreach (object? item in args.NewItems)
        {
            if (item is T child)
            {
                Distribute(child);
            }
        }
    }

    private void Distribute(T child)
    {
        if (child is not null)
        {
            BindableObject.SetInheritedBindingContext(child, _owner.BindingContext);
        }
    }
}
