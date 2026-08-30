using System;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// iOS で accessory の View を platform view へ実体化する口。
/// </summary>
/// <remarks>
/// 産物は計測と配置を自分で行う wrapper であり、accessory 領域はその必要サイズで高さを決める。
/// </remarks>
/// <param name="context">Handler の生成に使う MAUI のコンテキスト</param>
internal sealed class KsViewMaterializer(IMauiContext context) : IKsViewMaterializer
{
    /// <inheritdoc/>
    public IKsViewLease Materialize(View view, Action measureInvalidated)
    {
        ArgumentNullException.ThrowIfNull(view);
        ArgumentNullException.ThrowIfNull(measureInvalidated);

        return new Lease(new KsAccessoryHostView(view, context, measureInvalidated), view);
    }

    /// <summary>実体化した wrapper と、その後片付けの手順。</summary>
    /// <param name="host">実体化した wrapper</param>
    /// <param name="view">包んでいる View</param>
    private sealed class Lease(KsAccessoryHostView host, View view) : IKsViewLease
    {
        /// <inheritdoc/>
        public object PlatformView => host;

        /// <inheritdoc/>
        public void Dispose()
        {
            host.Detach();

            // 退役した表示 (セル) が wrapper を子として抱えたままになるため、明示的に外す。
            host.RemoveFromSuperview();
            view.DisconnectHandlers();
        }
    }
}
