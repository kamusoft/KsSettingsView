using System;
using System.Collections.ObjectModel;
using System.Runtime.CompilerServices;
using KsSettingsView.Maui.Handlers;
using KsSettingsView.Maui.Tests.Fakes;
using KsSettingsView.Maui.Tests.Support;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>切断後の資源回収を検証する。</summary>
[TestFixture]
public sealed class LeakTests
{
    /// <summary>検証ヘルパが、生きている参照をきちんと失敗として報告することを確かめる。</summary>
    [Test]
    public void GcProbeFailsWhenReferenceSurvives()
    {
        object held = new();
        WeakReference reference = new(held);

        Assert.Throws<AssertionException>(() => GcProbe.AssertCollected(reference, "held object"));

        GC.KeepAlive(held);
    }

    /// <summary>切断後、facade 側に Handler と Native Host への強参照が残らない。</summary>
    [Test]
    public void HandlerAndHostAreCollectedAfterDisconnect()
    {
        SettingsView view = new();
        view.Root.Add(new Section { Cells = { new LabelCell { Title = "title" } } });
        GatewayScope scope = GatewayScope.Connect(view);

        (WeakReference handler, WeakReference host) = ConnectThenDisconnect(view);

        GcProbe.AssertCollected(handler, "Handler");
        GcProbe.AssertCollected(host, "Native Host");

        // facade と gateway は生きたままであり、そのうえで回収されている。
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
        Assert.That(view.Root, Has.Count.EqualTo(1));
    }

    /// <summary>外部がコレクションと Cell を保持していても facade と gateway は回収される。</summary>
    [Test]
    public void FacadeAndGatewayAreCollectedWhileExternalHoldsModel()
    {
        ObservableCollection<Section> root = [];
        ObservableCollection<CellBase> cells = [];
        LabelCell cell = new() { Title = "held" };
        cells.Add(cell);
        root.Add(new Section { Cells = cells });

        (WeakReference view, WeakReference gateway) = BuildConnectedViewThenDrop(root);

        GcProbe.AssertCollected(view, "SettingsView");
        GcProbe.AssertCollected(gateway, "gateway");

        // 外部が保持し続けているモデルは生きたままであることを確かめる。
        Assert.That(root, Has.Count.EqualTo(1));
        Assert.That(cells, Does.Contain(cell));
    }

    /// <summary>外部が内容を置いた CustomCell を保持していても facade と gateway は回収される。</summary>
    [Test]
    public void FacadeAndGatewayAreCollectedWhileExternalHoldsCustomCell()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        ObservableCollection<Section> root = [new Section { Cells = { cell } }];

        (WeakReference view, WeakReference gateway) = BuildConnectedViewThenDrop(root);

        GcProbe.AssertCollected(view, "SettingsView");
        GcProbe.AssertCollected(gateway, "gateway");

        // 外部が保持し続けている Cell と内容は生きたままであることを確かめる。
        Assert.That(cell.Content, Is.SameAs(content));
    }

    /// <summary>accessory の View を別のインスタンスへ差し替えると、旧実体は回収される。</summary>
    [Test]
    public void AccessoryHostIsCollectedAfterTheViewIsReplaced()
        => AssertAccessoryHostCollected(static (view, section, _) => section.HeaderView = new Label());

    /// <summary>accessory の View を外すと、実体は回収される。</summary>
    [Test]
    public void AccessoryHostIsCollectedAfterTheViewIsCleared()
        => AssertAccessoryHostCollected(static (view, section, _) => section.HeaderView = null);

    /// <summary>accessory を持つ Section を取り除くと、実体は回収される。</summary>
    [Test]
    public void AccessoryHostIsCollectedAfterTheSectionIsRemoved()
        => AssertAccessoryHostCollected(static (view, section, _) => view.Root.Remove(section));

    /// <summary>設定ツリー全体を作り直すと、前の実体は回収される。</summary>
    [Test]
    public void AccessoryHostIsCollectedAfterTheRootIsRebuilt()
        => AssertAccessoryHostCollected(
            static (view, section, _) => view.Root = new ObservableCollection<Section>());

    /// <summary>Native Host を手放すと、accessory の実体は回収される。</summary>
    [Test]
    public void AccessoryHostIsCollectedAfterTheNativeHostIsReleased()
        => AssertAccessoryHostCollected(static (view, section, _) => view.ReleaseHost());

    /// <summary>root の accessory の実体も、外した時点で回収される。</summary>
    [Test]
    public void RootAccessoryHostIsCollectedAfterTheViewIsCleared()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);
        Label accessory = new();
        view.RootHeaderView = accessory;
        scope.Attach();

        WeakReference host = TakeHostReference(scope, accessory);
        view.RootHeaderView = null;
        Forget(scope);

        GcProbe.AssertCollected(host, "accessory の実体");
        GC.KeepAlive(accessory);
    }

    /// <summary>指定の操作で退役した accessory の実体が回収されることを確かめる。</summary>
    /// <param name="retire">実体を退役させる操作</param>
    private static void AssertAccessoryHostCollected(Action<SettingsView, Section, GatewayScope> retire)
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        Label accessory = new();
        section.HeaderView = accessory;
        scope.Attach();

        WeakReference host = TakeHostReference(scope, accessory);
        retire(view, section, scope);
        Forget(scope);

        // ここで見るのは platform 実体が回収されることだけで、facade が持つ View 自体は
        // 生かしたまま確かめる。実体の後片付けに伴う Handler の接続状態は
        // AccessoryViewTests が受け持つ。
        GcProbe.AssertCollected(host, "accessory の実体");
        GC.KeepAlive(accessory);
    }

    /// <summary>実体化された platform 実体への弱参照だけを取り出す。</summary>
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static WeakReference TakeHostReference(GatewayScope scope, Label accessory)
        => new(scope.Views.LatestFor(accessory).PlatformView);

    /// <summary>観測している側が実体を生かし続けないよう、記録をすべて手放す。</summary>
    private static void Forget(GatewayScope scope)
    {
        scope.Views.Forget();
        scope.Reset();
    }

    /// <summary>Handler を接続してから切断し、Handler と Native Host への弱参照を返す。</summary>
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static (WeakReference Handler, WeakReference Host) ConnectThenDisconnect(SettingsView view)
    {
        SettingsViewHandler handler = new();
        view.Handler = handler;

        WeakReference handlerReference = new(handler);
        WeakReference hostReference = new(handler.PlatformView);

        view.Handler = null;

        return (handlerReference, hostReference);
    }

    /// <summary>gateway を接続した SettingsView を作り、その弱参照だけを返す。</summary>
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static (WeakReference View, WeakReference Gateway) BuildConnectedViewThenDrop(
        ObservableCollection<Section> root)
    {
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        view.Handler = handler;
        view.Handler = null;

        return (new WeakReference(view), new WeakReference(scope.Gateway));
    }
}
