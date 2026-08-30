using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;

namespace KsSettingsView.Maui.Tests.Support;

/// <summary>
/// 複数件をまとめて追加できる observable なコレクション。
/// </summary>
/// <remarks>
/// <see cref="ObservableCollection{T}"/> の追加は 1 件ずつしか通知できないため、複数件を含む
/// 追加通知を受ける経路はこのコレクションでしか再現できない。
/// </remarks>
/// <typeparam name="T">要素の型</typeparam>
internal sealed class RangeAddCollection<T> : ObservableCollection<T>
{
    /// <summary>末尾へ複数件をまとめて足し、1 回の追加として通知する。</summary>
    /// <param name="added">足す要素</param>
    public void AddRange(params T[] added)
    {
        int index = Items.Count;
        foreach (T item in added)
        {
            Items.Add(item);
        }

        OnCollectionChanged(new NotifyCollectionChangedEventArgs(
            NotifyCollectionChangedAction.Add,
            new List<T>(added),
            index));
    }
}
