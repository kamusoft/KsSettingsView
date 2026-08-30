namespace KsSettingsView.Maui.Internals;

/// <summary>
/// Native Host を、それを抱える側の ViewController の子として結び付ける手順。
/// </summary>
/// <remarks>
/// 子として結び付ける platform では、登録 (<see cref="AddToParent"/>) を Host の view が view 階層へ
/// 入る前に、成立の確定 (<see cref="ConfirmAdded"/>) を入った後に行う必要がある。この境界は
/// その順序を Handler 共通部が持てるように切り出したものであり、ViewController を持たない
/// platform では実体を用意しない。
/// </remarks>
internal interface IKsHostContainment
{
    /// <summary>Native Host を親の子として登録する。</summary>
    /// <remarks>登録済みの場合と親が見つからない場合は何もしない。</remarks>
    void AddToParent();

    /// <summary>view 階層への取り付けが済んだことを親子関係へ確定させる。</summary>
    /// <remarks>登録がまだ済んでいなければ、この時点で登録してから確定させる。</remarks>
    void ConfirmAdded();

    /// <summary>親子関係を解消し、Native Host の view を view 階層から取り外す。</summary>
    void Remove();
}
