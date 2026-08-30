using System;
using KsSettingsView.Maui.Internals;
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

namespace KsSettingsView.Maui.Handlers;

/// <summary>
/// <see cref="SettingsView"/> と Native Host を結ぶ Handler。
/// </summary>
/// <remarks>
/// Handler は Native Host の生成と解放だけを受け持ち、設定ツリーの状態は
/// <see cref="SettingsView"/> と Bridge 側が保つ。そのため切断・再接続をまたいでも
/// 表示内容は Host 生成時の復元で戻る。
/// Native Host が view 階層へ取り付けられたことを <see cref="VisualElement.Loaded"/> で受け取り、
/// そこで Host と同じ寿命を持つ表示内容を適用する — root の accessory は Host 単位のプロパティで
/// あり Host を作り直すと失われるうえ、取り付け前の適用は Android で黙って失われる
/// (core/ADR-0019)。accessory と Cell の内容の View の実体も Host と同じ寿命であり、ここで
/// 作り直す。
/// Native Host が子として結び付けられる platform では、その登録を platform view を返す前に、
/// 成立の確定を取り付け後に行う。
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
        PlatformView platformView = CreateHost();

        // 親子関係の登録は、Host の view が view 階層へ入る前に済ませる。
        Containment?.AddToParent();

        return platformView;
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

    /// <summary>Native Host が view 階層へ取り付けられたことを受けて後始末を進める。</summary>
    /// <remarks>
    /// 親子関係の成立をここで確定させ、そのうえで Host と同じ寿命を持つ表示内容を適用する。
    /// </remarks>
    internal void OnHostAttached()
    {
        IElementHandler handler = this;
        if (handler.VirtualView is not SettingsView view || handler.PlatformView is null)
        {
            return;
        }

        Containment?.ConfirmAdded();
        view.ApplyHostViews();
    }

    private void OnVirtualViewLoaded(object? sender, EventArgs e) => OnHostAttached();
}
