using System;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Handlers;
using UIKit;

namespace KsSettingsView.Maui.Handlers;

/// <summary>
/// iOS の <see cref="SettingsViewHandler"/> の実体。
/// </summary>
/// <remarks>
/// Bridge が返す Native Host は <see cref="UIViewController"/> であり、その
/// <see cref="UIViewController.View"/> を platform view として返す。ViewController は
/// view 階層を持つ ViewController の子として結び付ける (子 ViewController の契約)。
/// </remarks>
public partial class SettingsViewHandler
{
    private partial UIView CreateHost()
    {
        KsBridgeGateway gateway = VirtualView.ConnectGateway(
            static () => new KsBridgeGateway(),
            new KsMauiDispatcher(VirtualView.Dispatcher),
            new KsImageResolver(MauiContext!),
            new KsViewMaterializer(MauiContext!));

        UIViewController controller = gateway.MakeHost()
            ?? throw new InvalidOperationException("Failed to create the native settings host view controller.");

        UIView hostView = controller.View
            ?? throw new InvalidOperationException("The native settings host view controller has no view.");

        Containment = new KsHostContainment(controller, hostView, () => FindParentController(controller, hostView));
        return hostView;
    }

    /// <summary>Native Host を抱えることになる ViewController を探す。</summary>
    /// <remarks>
    /// まず SettingsView の親をたどって Page の ViewController を求める — view 階層への取り付けを
    /// 待たずに解決できるため、子 ViewController の登録を view 追加より先に済ませられる。
    /// そこで見つからない場合だけ、取り付け済みの view 階層をさかのぼって探す。
    /// </remarks>
    /// <param name="controller">Native Host の ViewController</param>
    /// <param name="hostView">Native Host の view</param>
    private UIViewController? FindParentController(UIViewController controller, UIView hostView)
        => FindParentControllerFromElements() ?? FindParentControllerFromViewHierarchy(controller, hostView);

    /// <summary>SettingsView の親をたどって、最初に見つかる Page の ViewController を返す。</summary>
    private UIViewController? FindParentControllerFromElements()
    {
        Element? element = VirtualView;
        while (element is not null)
        {
            if (element is Page && element.Handler is IPlatformViewHandler { ViewController: { } controller })
            {
                return controller;
            }

            element = element.Parent;
        }

        return null;
    }

    /// <summary>view 階層をさかのぼって最初に見つかる ViewController を返す。</summary>
    /// <remarks>
    /// responder chain 上で最も近い ViewController が、この view を実際に抱えている親になる。
    /// Native Host の view 自身の responder は Host の ViewController であるため、
    /// 探索は superview から始める。取り付け前の view では見つからず null を返す。
    /// </remarks>
    /// <param name="controller">除外する Native Host の ViewController</param>
    /// <param name="hostView">起点にする view</param>
    private static UIViewController? FindParentControllerFromViewHierarchy(
        UIViewController controller,
        UIView hostView)
    {
        UIResponder? responder = hostView.Superview;
        while (responder is not null)
        {
            if (responder is UIViewController found && !ReferenceEquals(found, controller))
            {
                return found;
            }

            responder = responder.NextResponder;
        }

        return null;
    }
}
