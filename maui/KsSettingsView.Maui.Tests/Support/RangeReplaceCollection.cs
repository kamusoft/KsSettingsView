using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;

namespace KsSettingsView.Maui.Tests.Support;

/// <summary>
/// 連続した複数件をまとめて差し替えられる observable なコレクション。
/// </summary>
/// <remarks>
/// <see cref="ObservableCollection{T}"/> の差し替えは 1 件ずつしか通知できないため、複数件を含む
/// 差し替え通知を受ける経路はこのコレクションでしか再現できない。
/// </remarks>
/// <typeparam name="T">要素の型</typeparam>
internal sealed class RangeReplaceCollection<T> : ObservableCollection<T>
{
    /// <summary>
    /// <paramref name="index"/> から始まる範囲を差し替え、1 回の差し替えとして通知する。
    /// </summary>
    /// <param name="index">差し替える範囲の先頭位置</param>
    /// <param name="replacement">差し替え後の要素</param>
    public void ReplaceRange(int index, params T[] replacement)
    {
        List<T> replaced = new(replacement.Length);
        for (int i = 0; i < replacement.Length; i++)
        {
            replaced.Add(Items[index + i]);
            Items[index + i] = replacement[i];
        }

        OnCollectionChanged(new NotifyCollectionChangedEventArgs(
            NotifyCollectionChangedAction.Replace,
            new List<T>(replacement),
            replaced,
            index));
    }
}
