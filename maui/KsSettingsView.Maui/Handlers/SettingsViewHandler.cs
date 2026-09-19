using System;
using System.Collections.Generic;
using KsSettingsView.Internals;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Handlers;

#if IOS
using PlatformView = UIKit.UIView;
#elif ANDROID
using PlatformView = Android.Views.View;
#else
using PlatformView = System.Object;
#endif

namespace KsSettingsView.Handlers;

/// <summary>
/// <see cref="SettingsView"/> と Native Host を結ぶ Handler。
/// </summary>
/// <remarks>
/// Handler は Native Host の生成と解放だけを受け持ち、設定ツリーの状態は
/// <see cref="SettingsView"/> と Bridge 側が保つ。そのため切断・再接続をまたいでも
/// 表示内容は Host 生成時の復元で戻る。
/// 配置された View が最初の表示に含まれるよう、実体化は Host を作る前に済ませ、Host 単位の
/// プロパティである root の header / footer は Host を作った直後・platform view を返す前に
/// 適用する。
/// Native Host が子として結び付けられる platform では、その登録を platform view を返す前に、
/// 成立の確定を <see cref="VisualElement.Loaded"/> で受け取る取り付け後に行う。
/// Host の生成から root の適用・親子関係の登録までは 1 つの失敗単位として扱い、途中で失敗したら
/// その世代ごと手放してから失敗を伝える。
/// </remarks>
public partial class SettingsViewHandler : ViewHandler<SettingsView, PlatformView>
{
    /// <summary>
    /// <see cref="SettingsView"/> の対応付け。
    /// </summary>
    /// <remarks>
    /// 設定ツリーと accessory の反映は facade の変換経路が受け持つため、ここでは
    /// View 共通の対応付けだけを持つ。
    /// </remarks>
    public static readonly IPropertyMapper<SettingsView, SettingsViewHandler> Mapper =
        new PropertyMapper<SettingsView, SettingsViewHandler>(ViewMapper);

    /// <summary>既定の対応付けで Handler を作る。</summary>
    public SettingsViewHandler()
        : base(Mapper)
    {
    }

    /// <summary>対応付けを差し替えて Handler を作る。</summary>
    /// <param name="mapper">使用する対応付け。null で既定の対応付け</param>
    public SettingsViewHandler(IPropertyMapper? mapper)
        : base(mapper ?? Mapper)
    {
    }

    /// <summary>
    /// Native Host と、それを抱える側との親子関係。
    /// </summary>
    /// <remarks>
    /// Host 生成時に platform 側が用意する。親子関係を持たない platform では null のままになり、
    /// 結び付けの手順はすべて省かれる。
    /// </remarks>
    internal IKsHostContainment? Containment { get; set; }

    /// <inheritdoc/>
    protected override PlatformView CreatePlatformView()
    {
        try
        {
            PlatformView platformView = CreateHost();

            // root の accessory は Host が持つため、Host が出来た直後が最も早い適用の時点になる
            // (maui/ADR-0027)。この時点の値を Host が取り付けまで失わないことは、両 OS の Native
            // Host が保証する (core/ADR-0033)。
            VirtualView.ApplyRootAccessories();

            // 親子関係の登録は、Host の view が view 階層へ入る前に済ませる。
            Containment?.AddToParent();

            return platformView;
        }
        catch (Exception failure)
        {
            RollbackHost(failure);
            throw;
        }
    }

    /// <inheritdoc/>
    protected override void ConnectHandler(PlatformView platformView)
    {
        base.ConnectHandler(platformView);

        VirtualView.Loaded += OnVirtualViewLoaded;

        // 取り付け済みの状態で接続された場合は Loaded が来ないため、その場で通す。
        if (VirtualView.IsLoaded)
        {
            OnHostAttached();
        }
    }

    /// <inheritdoc/>
    protected override void DisconnectHandler(PlatformView platformView)
    {
        SettingsView view = VirtualView;
        view.Loaded -= OnVirtualViewLoaded;

        Containment?.Remove();
        Containment = null;
        view.ReleaseHost();

        base.DisconnectHandler(platformView);
    }

    /// <summary>Native Host を作る。</summary>
    /// <remarks>親子関係を持つ platform では、あわせて <see cref="Containment"/> を用意する。</remarks>
    private partial PlatformView CreateHost();

    /// <summary>
    /// Host を作る途中で起きた失敗を受けて、その世代ごと手放す。
    /// </summary>
    /// <remarks>
    /// 手放す手順は切断時の後片付けと同じ (親子関係の解消 → Native Host の解放)。世代の途中で
    /// 止まったまま残すと、gateway・Native Host・実体化した View がその世代のまま次の生成へ
    /// 持ち越され、新しい MauiContext での作り直しが起きない。
    /// 後片付けの 1 つが失敗しても残りを試み、後片付けが失敗した場合だけ、元の失敗を先頭に置いて
    /// まとめて投げる — 後片付けの失敗で、そもそもの失敗の原因を見失わせないため。
    /// </remarks>
    /// <param name="failure">Host を作る途中で起きた失敗</param>
    private void RollbackHost(Exception failure)
    {
        List<Exception> failures = [failure];

        try
        {
            Containment?.Remove();
        }
        catch (Exception exception)
        {
            failures.Add(exception);
        }

        Containment = null;

        try
        {
            VirtualView.ReleaseHost();
        }
        catch (Exception exception)
        {
            failures.Add(exception);
        }

        if (failures.Count > 1)
        {
            throw new AggregateException(failures).Flatten();
        }
    }

    /// <summary>Native Host が view 階層へ取り付けられたことを受けて後始末を進める。</summary>
    /// <remarks>
    /// ここで行うのは親子関係の成立の確定だけで、表示内容は Host を作る時点までに適用済みになる。
    /// </remarks>
    internal void OnHostAttached()
    {
        IElementHandler handler = this;
        if (handler.VirtualView is not SettingsView || handler.PlatformView is null)
        {
            return;
        }

        Containment?.ConfirmAdded();
    }

    private void OnVirtualViewLoaded(object? sender, EventArgs e) => OnHostAttached();
}
