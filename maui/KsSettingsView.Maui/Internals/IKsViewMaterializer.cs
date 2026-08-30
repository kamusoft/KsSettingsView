using System;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// accessory として表示する <see cref="View"/> を platform view へ実体化する口。
/// </summary>
/// <remarks>
/// 実体化した platform view の型は platform ごとに異なるため、この境界では型を持たない参照として
/// 扱い、輸送する platform 実装だけが実体の型を知る (<see cref="IKsImageResolver"/> と同じ seam の
/// 考え方)。実体化の口は Native Host と同じ寿命を持ち、Host を作り直すたびに新しい口へ差し替わる。
/// 実体化された platform view は自分で計測・配置を行い、包んだ View の必要サイズが変わったときは
/// <c>measureInvalidated</c> で知らせる。
/// </remarks>
internal interface IKsViewMaterializer
{
    /// <summary>View を platform view として実体化する。</summary>
    /// <param name="view">実体化する View</param>
    /// <param name="measureInvalidated">包んだ View の必要サイズが変わったときに呼ばれる処理</param>
    IKsViewLease Materialize(View view, Action measureInvalidated);
}
