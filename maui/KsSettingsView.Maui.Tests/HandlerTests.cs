using System;
using KsSettingsView.Handlers;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui;
using Microsoft.Maui.Hosting;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>Handler の接続・切断と登録を検証する。</summary>
[TestFixture]
public sealed class HandlerTests
{
    /// <summary>Handler の接続で Native Host が作られる。</summary>
    [Test]
    public void ConnectingHandlerCreatesHost()
    {
        SettingsView view = new();
        GatewayScope.Connect(view);

        SettingsViewHandler handler = new();
        view.Handler = handler;

        Assert.That(handler.PlatformView, Is.Not.Null);
    }

    /// <summary>Handler の切断で Native Host が解放される。</summary>
    [Test]
    public void DisconnectingHandlerReleasesHost()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);

        SettingsViewHandler handler = new();
        view.Handler = handler;
        Assert.That(scope.Gateway.ReleaseHostCount, Is.Zero);

        view.Handler = null;

        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
    }

    /// <summary>再接続でも gateway は作り直されず、設定ツリーの再送も起きない。</summary>
    [Test]
    public void ReconnectingHandlerKeepsTheConnectedGateway()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);

        view.Handler = new SettingsViewHandler();
        view.Handler = null;
        scope.Reset();
        view.Handler = new SettingsViewHandler();

        Assert.That(scope.All<GatewayCall.SetRoot>(), Is.Empty);
        Assert.That(view.Controller.Gateway, Is.SameAs(scope.Gateway));
    }

    /// <summary>接続済みの gateway があるときは新しい gateway を作らない。</summary>
    [Test]
    public void ConnectGatewayReusesTheFirstGateway()
    {
        SettingsView view = new();
        FakeSettingsGateway first = new();
        FakeSettingsGateway second = new();
        FakeDispatcher dispatcher = new();
        int factoryCalls = 0;

        FakeImageResolver images = new();
        FakeViewMaterializer views = new();

        IKsSettingsGateway connected = view.ConnectGateway(() => first, dispatcher, images, views);
        IKsSettingsGateway reconnected = view.ConnectGateway(
            () =>
            {
                factoryCalls++;
                return second;
            },
            dispatcher,
            images,
            views);

        Assert.That(connected, Is.SameAs(first));
        Assert.That(reconnected, Is.SameAs(first));
        Assert.That(factoryCalls, Is.Zero);
        Assert.That(second.Calls, Is.Empty);
    }

    /// <summary>親子関係の登録は、platform view が Handler へ渡るより前に済ませる。</summary>
    [Test]
    public void HostIsAddedToParentBeforeThePlatformViewIsHandedOver()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway);
        handler.Containment = containment;

        view.Handler = handler;

        Assert.That(containment.Steps, Is.EqualTo(new[] { "AddToParent" }));
        Assert.That(containment.HandlerHadPlatformViewOnAdd, Is.False);
        Assert.That(handler.PlatformView, Is.Not.Null);
    }

    /// <summary>成立の確定は view 階層への取り付け後であり、root accessory の適用より先に行う。</summary>
    [Test]
    public void AttachConfirmsContainmentBeforeApplyingRootAccessory()
    {
        SettingsView view = new() { RootHeaderText = "header" };
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway);
        handler.Containment = containment;
        view.Handler = handler;
        scope.Reset();

        handler.OnHostAttached();

        Assert.That(containment.Steps, Is.EqualTo(new[] { "AddToParent", "ConfirmAdded" }));
        Assert.That(containment.HandlerHadPlatformViewOnConfirm, Is.True);
        Assert.That(containment.GatewayCallCountOnConfirm, Is.Zero);
        Assert.That(scope.All<GatewayCall.UpdateAccessory>(), Has.Count.EqualTo(2));
    }

    /// <summary>切断では親子関係を解消してから Native Host を解放し、結び付けを手放す。</summary>
    [Test]
    public void DisconnectRemovesContainmentBeforeReleasingHost()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway);
        handler.Containment = containment;
        view.Handler = handler;

        view.Handler = null;

        Assert.That(containment.Steps, Is.EqualTo(new[] { "AddToParent", "Remove" }));
        Assert.That(containment.ReleaseHostCountOnRemove, Is.Zero);
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
        Assert.That(handler.Containment, Is.Null);
    }

    /// <summary><see cref="MauiAppBuilderExtensions.AddKsSettingsView"/> は SettingsView の Handler だけを登録する。</summary>
    [Test]
    public void AddKsSettingsViewRegistersOnlyTheSettingsViewHandler()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder(useDefaults: false);

        builder.AddKsSettingsView();

        using ServiceProvider services = builder.Services.BuildServiceProvider();
        IMauiHandlersFactory factory = services.GetRequiredService<IMauiHandlersFactory>();

        Assert.That(factory.GetHandler(typeof(SettingsView)), Is.TypeOf<SettingsViewHandler>());
        Assert.That(factory.GetHandlerType(typeof(LabelCell)), Is.Null);
        Assert.That(factory.GetHandlerType(typeof(Section)), Is.Null);
    }
}
