using System.Collections.Specialized;

namespace KsSettingsView.Internals;

/// <summary>
/// コレクションの変更通知を弱参照購読から受け取る観測者。
/// </summary>
internal interface IKsCollectionObserver
{
    /// <summary>購読中のコレクションが変更されたときに呼ばれる。</summary>
    /// <param name="context">どのコレクションからの通知かを観測者が識別するための目印</param>
    /// <param name="args">変更の内容</param>
    void OnObservedCollectionChanged(object? context, NotifyCollectionChangedEventArgs args);
}
