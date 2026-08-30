using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;

namespace KsSettingsView.Maui.Tests.Support;

/// <summary>
/// 連続した複数件をまとめて移動できる observable なコレクション。
/// </summary>
/// <remarks>
/// <see cref="ObservableCollection{T}"/> の移動は 1 件ずつしか行えないため、複数件を含む
/// 移動通知を受ける経路はこのコレクションでしか再現できない。
/// </remarks>
/// <typeparam name="T">要素の型</typeparam>
internal sealed class RangeMoveCollection<T> : ObservableCollection<T>
{
    /// <summary>
    /// <paramref name="oldIndex"/> から始まる <paramref name="count"/> 件を
    /// <paramref name="newIndex"/> へまとめて移し、1 回の移動として通知する。
    /// </summary>
    /// <param name="oldIndex">移動元の先頭位置</param>
    /// <param name="newIndex">移動先の先頭位置</param>
    /// <param name="count">移動する件数</param>
    public void MoveRange(int oldIndex, int newIndex, int count)
    {
        List<T> moved = [];
        for (int i = 0; i < count; i++)
        {
            moved.Add(Items[oldIndex]);
            Items.RemoveAt(oldIndex);
        }

        for (int i = 0; i < count; i++)
        {
            Items.Insert(newIndex + i, moved[i]);
        }

        OnCollectionChanged(new NotifyCollectionChangedEventArgs(
            NotifyCollectionChangedAction.Move,
            moved,
            newIndex,
            oldIndex));
    }
}
