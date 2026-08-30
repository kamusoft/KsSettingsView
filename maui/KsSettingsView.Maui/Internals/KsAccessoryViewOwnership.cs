using Microsoft.Maui.Controls;
using Microsoft.Maui.Controls.Internals;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// accessory に置かれた View の論理上の所有を扱う。
/// </summary>
/// <remarks>
/// 論理ツリーへの接続と BindingContext の継承は、Native Host の有無と関係なく成り立つ契約であり、
/// platform 実体の寿命とは切り離して所有者 (SettingsView / Section) 自身が受け持つ。これにより
/// Handler が生成される前 (XAML の構築時) に置いた View も、Host を手放している間に所有者の
/// BindingContext が変わった場合も、継承がそのまま働く。
/// 入口は 2 つあり、多重配置の検査を通れるかどうかで使い分ける。
/// <list type="bullet">
/// <item>
/// 設定ツリーに載っている所有者からは <see cref="Reassign"/> を使う。呼び出し元が多重配置を
/// 検査した後・platform 実体を作る前に呼ぶ約束であり、検査の後にすることで例外になった配置が
/// 他所の正しい配置の所有を奪わず、実体化より先にすることで Handler は BindingContext の
/// 定まった View に対して作られる。
/// </item>
/// <item>
/// まだ設定ツリーに載っていない所有者からは <see cref="ReassignIfFree"/> を使う。この経路には
/// 検査を行う相手がいないため、他所に所有されている View は引き取らない。引き取らなかった
/// 配置は、所有者が変換経路に加わった時点 (Native Host 未接続のまま設定ツリーへ入った場合は
/// Host 接続時) で多重配置として弾かれる。
/// </item>
/// </list>
/// </remarks>
internal static class KsAccessoryViewOwnership
{
    /// <summary>accessory の View を所有者の論理子として付け替える。</summary>
    /// <remarks>
    /// 継承プロパティを配り、BindingContext を確定させてから論理ツリーへ入れる。この順序により、
    /// 後で Handler を作る時点では BindingContext が既に定まっている。
    /// 同じ状態へ何度呼んでも結果は変わらない — 変換経路とプロパティの両方から呼ばれ、どちらが先に
    /// 走るかは呼び出し元の事情で決まるため。既に外れている View は外さず、既にこの所有者へ
    /// 付いている View は付け直さない。
    /// </remarks>
    /// <param name="owner">所有者</param>
    /// <param name="oldView">外す View。無ければ null</param>
    /// <param name="newView">入れる View。無ければ null</param>
    public static void Reassign(Element owner, View? oldView, View? newView)
    {
        Detach(owner, oldView);

        if (newView is null || ReferenceEquals(newView.Parent, owner))
        {
            return;
        }

        PropertyPropagationExtensions.PropagatePropertyChanged(null, newView, owner);
        BindableObject.SetInheritedBindingContext(newView, owner.BindingContext);
        owner.AddLogicalChild(newView);
    }

    /// <summary>
    /// どこにも所有されていない View に限って、所有者の論理子として引き取る。
    /// </summary>
    /// <remarks>
    /// 多重配置を検査できない経路のための入口。他所に所有されている View を引き取ると、正しく
    /// 置かれている側の論理親と継承 BindingContext を黙って奪ってしまうため、その場合は
    /// 引き取らずに置く (外す方の後始末だけは行う)。引き取らなかった配置は表示にも論理ツリーにも
    /// 現れず、所有者が変換経路に加わった時点 (Native Host 未接続のまま設定ツリーへ入った場合は
    /// Host 接続時) で多重配置の例外になる。
    /// </remarks>
    /// <param name="owner">所有者</param>
    /// <param name="oldView">外す View。無ければ null</param>
    /// <param name="newView">引き取る View。無ければ null</param>
    public static void ReassignIfFree(Element owner, View? oldView, View? newView)
    {
        if (newView?.Parent is { } holder && !ReferenceEquals(holder, owner))
        {
            Detach(owner, oldView);
            return;
        }

        Reassign(owner, oldView, newView);
    }

    /// <summary>この所有者に付いている View を論理子から外す。</summary>
    /// <param name="owner">所有者</param>
    /// <param name="oldView">外す View。無ければ null</param>
    private static void Detach(Element owner, View? oldView)
    {
        if (oldView is not null && ReferenceEquals(oldView.Parent, owner))
        {
            owner.RemoveLogicalChild(oldView);
        }
    }
}
