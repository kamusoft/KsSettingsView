using System;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 実体化した accessory View の platform 実体と、その後片付けの口を一体で持つ器。
/// </summary>
/// <remarks>
/// platform 実体は Native Host と同じ寿命であり、Host を手放すときや別の View へ差し替えるときに
/// 破棄する。破棄は包んだ View の Handler を切り離し、platform の表示階層からも外す
/// (外し忘れると退役した表示に古い実体が残る)。
/// 破棄は native への配信を終えた後に行う — native がまだこの実体を子として抱えている間に
/// 壊さないため。
/// </remarks>
internal interface IKsViewLease : IDisposable
{
    /// <summary>実体化された platform view。</summary>
    object PlatformView { get; }
}
