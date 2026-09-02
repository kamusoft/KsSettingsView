using System;
using UIKit;

namespace KsSettingsView.Internals;

/// <summary>
/// Native Host の ViewController を、それを抱える ViewController の子として結び付ける。
/// </summary>
/// <remarks>
/// 子 ViewController は「登録 → view 階層への追加 → 成立の確定」の順で組み立てる契約であり、
/// この順序でだけ appearance とライフサイクルの通知が正しく伝わる。
/// </remarks>
/// <param name="controller">Native Host の ViewController</param>
/// <param name="hostView">Native Host の view</param>
/// <param name="parentResolver">親になる ViewController を探す関数。見つからなければ null を返す</param>
internal sealed class KsHostContainment(
    UIViewController controller,
    UIView hostView,
    Func<UIViewController?> parentResolver) : IKsHostContainment
{
    /// <inheritdoc/>
    public void AddToParent()
    {
        if (controller.ParentViewController is not null)
        {
            return;
        }

        if (parentResolver() is not { } parent)
        {
            return;
        }

        parent.AddChildViewController(controller);
    }

    /// <inheritdoc/>
    public void ConfirmAdded()
    {
        // Host を作った時点で親が決まらなかった場合に備え、取り付け後にもう一度だけ試みる。
        AddToParent();

        if (controller.ParentViewController is { } parent)
        {
            controller.DidMoveToParentViewController(parent);
        }
    }

    /// <inheritdoc/>
    public void Remove()
    {
        controller.WillMoveToParentViewController(null);
        hostView.RemoveFromSuperview();
        controller.RemoveFromParentViewController();
    }
}
