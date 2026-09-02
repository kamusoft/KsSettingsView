using System;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// Android で accessory の View を platform view へ実体化する口。
/// </summary>
/// <remarks>
/// 産物は計測と配置を自分で行う wrapper であり、accessory 領域はその測定結果で高さを決める。
/// </remarks>
/// <param name="context">Handler の生成と Context の解決に使う MAUI のコンテキスト</param>
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
            view.DisconnectHandlers();
        }
    }
}
