using System;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// 多重配置の判定と、そのときに送出する例外を組み立てる。
/// </summary>
/// <remarks>
/// 判定と文言を 1 箇所へ集めるのは、所属の時点で弾く経路 (<see cref="KsLogicalChildOwnership{T}"/>) と
/// 表示へ変換する時点で弾く経路 (<see cref="KsSettingsController"/>) が同じ契約を表すため。
/// 経路ごとに文言が分かれると、利用者から見て同じ誤用が別の失敗に見える。
/// </remarks>
internal static class KsPlacementDiagnostics
{
    /// <summary>この要素が facade の配置先として要素を所有する側かどうか。</summary>
    /// <remarks>
    /// facade が論理上の所有者になるのは SettingsView (root の accessory と Section)、
    /// Section (accessory と Cell)、CustomCell (内容) の 3 つだけ。利用者が組んだ
    /// レイアウトの中に置かれた View はここに当たらず、多重配置ではない。
    /// </remarks>
    /// <param name="element">判定する要素</param>
    public static bool IsFacadeOwner(Element? element) => element is SettingsView or Section or CustomCell;

    /// <summary>この要素が、期待する所有者以外の facade 所有者に所有されているかどうか。</summary>
    /// <remarks>
    /// 設定ツリーから外れた Section / CustomCell が所有したままの場合も、所有者が facade である
    /// 限り多重配置として扱う。所有を解かずに別の場所へ置き直すことはできないため。
    /// </remarks>
    /// <param name="expectedOwner">これから所有する側</param>
    /// <param name="element">置こうとしている要素。null なら所有されていない扱い</param>
    public static bool IsOwnedElsewhere(Element expectedOwner, Element? element)
        => element?.Parent is { } holder
            && !ReferenceEquals(holder, expectedOwner)
            && IsFacadeOwner(holder);

    /// <summary>多重配置を表す例外を作る。</summary>
    /// <param name="kind">重なった対象の種別 (Section / Cell / View)</param>
    public static InvalidOperationException DuplicatePlacement(string kind)
        => new($"The same {kind} instance cannot be placed more than once.");
}
