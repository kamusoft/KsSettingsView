using System;
using System.Collections.Concurrent;
using System.Reflection;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 要素をプロパティ名で指して表示文字列を取り出す射影。
/// </summary>
/// <remarks>
/// 解決対象は public instance の引数なし readable プロパティに限る。名前を解決できないときは
/// 「未解決」として呼び出し側の既定 (主表示は <c>ToString()</c>、副表示はなし) へ委ねる。
/// 解決結果は要素の実行時型とプロパティ名の組で覚えるため、候補 1 件ごとに解決をやり直さない。
/// 取り出しは <see cref="PropertyInfo"/> 経由で行い、動的コード生成に依存しない。
/// </remarks>
internal static class KsMemberProjection
{
    /// <summary>実行時型とプロパティ名の組に対する解決結果。未解決は null として覚える。</summary>
    private static readonly ConcurrentDictionary<(Type Type, string Member), PropertyInfo?> s_getters = new();

    /// <summary>
    /// 主表示テキストを取り出す。
    /// </summary>
    /// <remarks>
    /// プロパティ名が未指定・未解決なら要素の <c>ToString()</c>、プロパティ値が null なら空文字列。
    /// getter が送出した例外はそのまま伝播する。
    /// </remarks>
    /// <param name="item">射影する要素</param>
    /// <param name="member">主表示に使うプロパティ名。null で未指定</param>
    public static string Text(object item, string? member)
    {
        if (member is null || Getter(item.GetType(), member) is not PropertyInfo getter)
        {
            return item.ToString() ?? string.Empty;
        }

        return Value(getter, item)?.ToString() ?? string.Empty;
    }

    /// <summary>
    /// 副表示テキストを取り出す。
    /// </summary>
    /// <remarks>
    /// プロパティ名が未指定・未解決、プロパティ値が null、値の文字列化が空文字列のいずれでも
    /// 「副表示なし」を表す null を返す。getter が送出した例外はそのまま伝播する。
    /// </remarks>
    /// <param name="item">射影する要素</param>
    /// <param name="member">副表示に使うプロパティ名。null で未指定</param>
    public static string? SubText(object item, string? member)
    {
        if (member is null || Getter(item.GetType(), member) is not PropertyInfo getter)
        {
            return null;
        }

        string? text = Value(getter, item)?.ToString();
        return string.IsNullOrEmpty(text) ? null : text;
    }

    /// <summary>
    /// プロパティの値を取り出す。
    /// </summary>
    /// <remarks>
    /// getter が送出した例外を reflection の入れ物で包まず、そのまま呼び出し元へ通す。
    /// </remarks>
    /// <param name="getter">値を取り出すプロパティ</param>
    /// <param name="item">取り出す対象</param>
    private static object? Value(PropertyInfo getter, object item)
        => getter.GetMethod!.Invoke(item, BindingFlags.DoNotWrapExceptions, null, null, null);

    /// <summary>指定した型とプロパティ名に対する getter。未解決は null。</summary>
    /// <param name="type">要素の実行時型</param>
    /// <param name="member">プロパティ名</param>
    private static PropertyInfo? Getter(Type type, string member)
        => s_getters.GetOrAdd((type, member), static key => Resolve(key.Type, key.Member));

    /// <summary>
    /// 型階層をたどって public instance の引数なし readable プロパティを探す。
    /// </summary>
    /// <remarks>
    /// 派生側で名前を隠しているプロパティを一意に選ぶため、宣言元ごとに区切って派生側から探す。
    /// 名前で 1 件に絞る <c>GetProperty</c> は、同名のプロパティが複数ある型 (引数の型だけが違う
    /// indexer を複数持つ型など) で例外になるため使わない。宣言分を列挙して、名前・public な
    /// instance getter・引数なしの条件で自前に絞る。
    /// </remarks>
    /// <param name="type">要素の実行時型</param>
    /// <param name="member">プロパティ名</param>
    private static PropertyInfo? Resolve(Type type, string member)
    {
        for (Type? current = type; current is not null; current = current.BaseType)
        {
            foreach (PropertyInfo property in current.GetProperties(
                BindingFlags.Public | BindingFlags.Instance | BindingFlags.DeclaredOnly))
            {
                if (property.Name == member
                    && property is { GetMethod: { IsPublic: true, IsStatic: false } }
                    && property.GetIndexParameters().Length == 0)
                {
                    return property;
                }
            }
        }

        return null;
    }
}
