using System;

namespace KsSettingsView.Internals;

/// <summary>
/// <see cref="SettingsView"/> が作る <see cref="IScrollController"/> の実体。
/// </summary>
/// <remarks>
/// 命令は持ち主の変換経路へそのまま渡し、対象の解決と gateway への配信はそちらが行う
/// (maui/ADR-0029)。持ち主は弱参照で持つ — ハンドルは ViewModel へ渡り、ページより長く
/// 保持されうるため、ハンドル経由で SettingsView とその Native Host を生かし続けない。
/// 持ち主が回収された後の命令は何もしない。
/// </remarks>
/// <param name="owner">このハンドルを作った SettingsView</param>
internal sealed class KsSettingsScrollController(SettingsView owner) : IScrollController
{
    private readonly WeakReference<SettingsView> _owner = new(owner);

    /// <inheritdoc/>
    public void ScrollTo(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true)
        => Owner?.Controller.ScrollToCell(target, position, animated);

    /// <inheritdoc/>
    public void ScrollToSection(
        object target,
        ScrollPosition position = ScrollPosition.Start,
        bool animated = true)
        => Owner?.Controller.ScrollToSection(target, position, animated);

    /// <inheritdoc/>
    public void ScrollToStart(bool animated = true) => Owner?.Controller.ScrollToStart(animated);

    /// <inheritdoc/>
    public void ScrollToEnd(bool animated = true) => Owner?.Controller.ScrollToEnd(animated);

    private SettingsView? Owner => _owner.TryGetTarget(out SettingsView? view) ? view : null;
}
