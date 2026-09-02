using System;
using System.Collections.ObjectModel;
using System.Runtime.CompilerServices;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using KsSettingsView.Tests.Support;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// ユーザー操作の通知経路の寿命を確認する。解除後は通知先が残らず、通知経路があっても
/// SettingsView の回収は妨げられない。
/// </summary>
[TestFixture]
public sealed class InteractionLifetimeTests
{
    /// <summary>Native Host の解放で通知先が外れ、以後の操作は届く先を持たない。</summary>
    [Test]
    public void ReleasingHostDetachesInteractions()
    {
        SwitchCell cell = new();
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);
        Assert.That(scope.Gateway.Sink, Is.Not.Null);

        view.ReleaseHost();

        Assert.That(scope.Gateway.Sink, Is.Null);
        Assert.That(scope.Gateway.DetachInteractionsCount, Is.EqualTo(1));
    }

    /// <summary>解除後に届いた通知は、対象が除去済みなら何も起こさずに捨てられる。</summary>
    [Test]
    public void NotificationForRemovedCellIsDropped()
    {
        SwitchCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        IKsInteractionSink sink = scope.Gateway.Sink!;
        string cellId = view.Controller.FindCellId(cell)!;

        section.Cells.Remove(cell);
        view.ReleaseHost();
        scope.Reset();

        sink.SwitchCellChanged(cellId, true);
        scope.Flush();

        Assert.That(cell.On, Is.False);
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>再接続では通知先が取り直され、書き戻しが再び機能する。</summary>
    [Test]
    public void ReconnectRestoresInteractions()
    {
        SwitchCell cell = new();
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);
        string cellId = view.Controller.FindCellId(cell)!;

        view.ReleaseHost();
        scope.Reconnect();

        scope.Gateway.Sink!.SwitchCellChanged(cellId, true);

        Assert.That(cell.On, Is.True);
        Assert.That(scope.Gateway.AttachInteractionsCount, Is.EqualTo(2));
    }

    /// <summary>通知先を差し込んだ後でも、SettingsView と gateway は回収される。</summary>
    [Test]
    public void ViewIsCollectedWhileInteractionsAreAttached()
    {
        ObservableCollection<Section> root = [];
        root.Add(new Section { Cells = { new SwitchCell() } });

        (WeakReference view, WeakReference gateway) = BuildViewWithInteractionsThenDrop(root);

        GcProbe.AssertCollected(view, "SettingsView");
        GcProbe.AssertCollected(gateway, "gateway");
    }

    /// <summary>通知先を差し込んだ SettingsView を作り、その弱参照だけを返す。</summary>
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static (WeakReference View, WeakReference Gateway) BuildViewWithInteractionsThenDrop(
        ObservableCollection<Section> root)
    {
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);

        // 通知先が差し込まれた状態を作ってから参照を落とす。
        Assert.That(scope.Gateway.Sink, Is.Not.Null);

        return (new WeakReference(view), new WeakReference(scope.Gateway));
    }
}
