using System.Collections.Generic;
using System.Runtime.CompilerServices;

namespace KsSettingsView.Internals;

/// <summary>
/// インスタンスの同一性だけで一致を判定する比較子。
/// </summary>
/// <remarks>
/// 対応表と dirty set の鍵は「どのインスタンスか」であり、値としての等価性で束ねてはいけない。
/// 対象型が等価演算子を上書きしても判定が変わらないよう、参照比較を明示して固定する。
/// </remarks>
/// <typeparam name="T">比較対象の型</typeparam>
internal sealed class KsReferenceComparer<T> : IEqualityComparer<T>
    where T : class
{
    /// <summary>共有インスタンス。状態を持たないため使い回せる。</summary>
    public static readonly KsReferenceComparer<T> Instance = new();

    private KsReferenceComparer()
    {
    }

    /// <inheritdoc/>
    public bool Equals(T? x, T? y) => ReferenceEquals(x, y);

    /// <inheritdoc/>
    public int GetHashCode(T obj) => RuntimeHelpers.GetHashCode(obj);
}
