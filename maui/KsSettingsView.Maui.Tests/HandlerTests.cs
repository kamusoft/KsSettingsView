using System;
using System.Linq;
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

    /// <summary>実体化の途中で接続が失敗したときは、作られた実体を片付けて実体化の口も手放す。</summary>
    /// <remarks>
    /// 実体化の口は接続より前に差し込まれ、設定ツリーの実体化は対応表への登録より前に進む。失敗した
    /// 接続の後に口が残ると gateway が無いまま実体だけが作られ、作りかけの実体が残ると次の接続では
    /// 作り直されないまま新しい Host へ渡ってしまう。
    /// </remarks>
    [Test]
    public void AFailedInitialConnectReleasesTheMaterializationSeam()
    {
        Section first = new() { HeaderView = new Label() };
        Section second = new() { HeaderView = new Label() };
        SettingsView view = new() { Root = { first, second } };

        FakeSettingsGateway gateway = new();
        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();

        // 1 件目の実体化だけが通り、2 件目で失敗する。
        FakeViewMaterializer views = new() { SucceedsUpTo = 1 };

        Assert.Throws<InvalidOperationException>(
            () => view.ConnectGateway(() => gateway, dispatcher, images, views));

        // 口が残っていれば、ここで置いた View がそのまま実体化される。
        view.RootHeaderView = new Label();

        Assert.That(views.Leases, Has.Count.EqualTo(1));
        Assert.That(views.Leases[0].IsDisposed, Is.True);
        Assert.That(view.Controller.Gateway, Is.Null);
    }

    /// <summary>設定ツリーの配信が失敗したときも、その接続で作られた実体を片付ける。</summary>
    [Test]
    public void AFailedRootDeliveryDisposesTheViewsMaterializedForThatConnection()
    {
        Section section = new() { HeaderView = new Label() };
        SettingsView view = new() { Root = { section } };

        FakeSettingsGateway gateway = new() { SetRootFails = true };
        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();
        FakeViewMaterializer views = new();

        Assert.Throws<InvalidOperationException>(
            () => view.ConnectGateway(() => gateway, dispatcher, images, views));

        Assert.That(views.Leases, Has.Count.EqualTo(1));
        Assert.That(views.Leases[0].IsDisposed, Is.True);
        Assert.That(view.Controller.Gateway, Is.Null);
    }

    /// <summary>gateway を作るところで失敗したときは、実体化の口が差し込まれないまま終わる。</summary>
    /// <remarks>
    /// gateway を作る呼び出しは接続の外側にあり、そこでの失敗は接続の後始末を通らない。口を先に
    /// 差し込むと、その口が残って未接続のまま実体を作れる状態になる。
    /// </remarks>
    [Test]
    public void AFailedGatewayCreationLeavesNoMaterializationSeam()
    {
        SettingsView view = new();
        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();
        FakeViewMaterializer views = new();

        Assert.Throws<InvalidOperationException>(
            () => view.ConnectGateway<FakeSettingsGateway>(
                () => throw new InvalidOperationException("gateway creation failed"),
                dispatcher,
                images,
                views));

        // 口が残っていれば、ここで置いた View がそのまま実体化される。
        view.RootHeaderView = new Label();

        Assert.That(views.Leases, Is.Empty);
        Assert.That(view.Controller.Gateway, Is.Null);
    }

    /// <summary>失敗した接続の後にやり直すと、View は新しい口で実体化し直されて配信に載る。</summary>
    [Test]
    public void ReconnectingAfterAFailedConnectMaterializesTheViewsWithTheNewSeam()
    {
        Label header = new();
        Section section = new() { HeaderView = header };
        SettingsView view = new() { Root = { section } };

        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();
        FakeViewMaterializer failedViews = new();

        Assert.Throws<InvalidOperationException>(
            () => view.ConnectGateway(
                () => new FakeSettingsGateway { SetRootFails = true },
                dispatcher,
                images,
                failedViews));

        FakeSettingsGateway gateway = new();
        FakeViewMaterializer views = new();
        view.ConnectGateway(() => gateway, dispatcher, images, views);

        GatewayCall.SetRoot delivered = gateway.Calls.OfType<GatewayCall.SetRoot>().Single();
        Assert.That(views.CountFor(header), Is.EqualTo(1));
        Assert.That(
            delivered.Transported[0].HeaderView,
            Is.SameAs(views.LatestFor(header).PlatformView));
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

    /// <summary>root accessory の適用は Host の生成直後で、親子関係の登録より先に行う。</summary>
    [Test]
    public void RootAccessoryIsAppliedRightAfterTheHostIsCreated()
    {
        SettingsView view = new() { RootHeaderText = "header" };
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway);
        handler.Containment = containment;
        scope.Reset();

        view.Handler = handler;

        Assert.That(scope.All<GatewayCall.UpdateAccessory>(), Has.Count.EqualTo(2));
        Assert.That(containment.GatewayCallCountOnAdd, Is.EqualTo(2));
    }

    /// <summary>取り付けの通知で行うのは成立の確定だけで、表示内容は動かさない。</summary>
    [Test]
    public void AttachOnlyConfirmsContainment()
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
        Assert.That(scope.Gateway.Calls, Is.Empty);
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

    /// <summary>Root の footer の実体化で失敗したときは、その世代の Host ごと手放す。</summary>
    /// <remarks>
    /// Host の生成から root の accessory の適用・親子関係の登録までは 1 つの失敗単位であり、
    /// 途中で失敗した世代の実体・Host を残すと、やり直しがその世代のものを使い回す。
    /// </remarks>
    [Test]
    public void AFailedRootAccessoryApplicationReleasesTheHostGeneration()
    {
        Label header = new();
        Label footer = new();
        SettingsView view = new() { RootHeaderView = header, RootFooterView = footer };
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        // Root の header だけが実体化でき、続く footer で失敗する。
        scope.Views.SucceedsUpTo = 1;

        Assert.Throws<InvalidOperationException>(() => view.Handler = new SettingsViewHandler());

        Assert.That(scope.Views.Leases, Has.Count.EqualTo(1));
        Assert.That(scope.Views.Leases[0].IsDisposed, Is.True);
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));

        // 新しい実体化の口で作り直すと、両方の位置が新しい実体で配信される。
        FakeViewMaterializer renewed = new();
        view.ConnectGateway(() => scope.Gateway, scope.Dispatcher, scope.Images, renewed);
        scope.Reset();

        view.Handler = new SettingsViewHandler();

        Assert.That(renewed.CountFor(header), Is.EqualTo(1));
        Assert.That(renewed.CountFor(footer), Is.EqualTo(1));
        Assert.That(
            scope.All<GatewayCall.UpdateAccessoryView>().Select(call => call.View),
            Is.EquivalentTo(new[] { renewed.LatestFor(header).PlatformView, renewed.LatestFor(footer).PlatformView }));
    }

    /// <summary>Host の生成で失敗したときは、その世代のために作られた実体と Host を手放す。</summary>
    [Test]
    public void AFailedHostCreationReleasesTheHostGeneration()
    {
        Label header = new();
        Section section = new() { HeaderView = header };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        SettingsViewHandler handler = new() { FailsToCreateHost = true };
        RecordingHostContainment containment = new(handler, scope.Gateway);
        handler.Containment = containment;

        Assert.Throws<InvalidOperationException>(() => view.Handler = handler);

        Assert.That(scope.Views.LatestFor(header).IsDisposed, Is.True);
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
        Assert.That(containment.Steps, Is.EqualTo(new[] { "Remove" }));
        Assert.That(handler.Containment, Is.Null);
    }

    /// <summary>親子関係の登録で失敗したときも、その世代の Host ごと手放す。</summary>
    [Test]
    public void AFailedContainmentRegistrationReleasesTheHostGeneration()
    {
        Label header = new();
        Section section = new() { HeaderView = header };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway) { FailsToAdd = true };
        handler.Containment = containment;

        Assert.Throws<InvalidOperationException>(() => view.Handler = handler);

        Assert.That(scope.Views.LatestFor(header).IsDisposed, Is.True);
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
        Assert.That(containment.Steps, Is.EqualTo(new[] { "AddToParent", "Remove" }));
        Assert.That(handler.Containment, Is.Null);
    }

    /// <summary>後片付け自体が失敗しても、元の失敗を失わず残りの後片付けも続ける。</summary>
    [Test]
    public void AFailedRollbackKeepsTheOriginalHostFailure()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway)
        {
            FailsToAdd = true,
            FailsToRemove = true,
        };
        handler.Containment = containment;

        AggregateException? thrown = Assert.Throws<AggregateException>(() => view.Handler = handler);

        Assert.That(thrown!.InnerExceptions, Has.Count.EqualTo(2));
        Assert.That(
            thrown.InnerExceptions[0].Message,
            Is.EqualTo("adding the host to its parent failed"));

        // 親子関係の解消が失敗しても Host の解放まで進む。
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
    }

    /// <summary>接続の失敗の後片付けは、1 件の破棄が失敗しても残りの実体を破棄する。</summary>
    [Test]
    public void AFailedDisposeDuringConnectFailureStillDisposesTheRemainingViews()
    {
        Section first = new() { HeaderView = new Label() };
        Section second = new() { HeaderView = new Label() };
        SettingsView view = new() { Root = { first, second } };

        FakeSettingsGateway gateway = new() { SetRootFails = true };
        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();
        FakeViewMaterializer views = new();

        // 先に作られた実体の破棄だけが失敗する。
        views.Materialized = lease =>
        {
            if (views.Leases.Count == 1)
            {
                lease.OnDispose = () => throw new InvalidOperationException("dispose failed");
            }
        };

        AggregateException? thrown = Assert.Throws<AggregateException>(
            () => view.ConnectGateway(() => gateway, dispatcher, images, views));

        Assert.That(views.Leases, Has.Count.EqualTo(2));
        Assert.That(views.Leases[1].IsDisposed, Is.True);
        Assert.That(
            thrown!.InnerExceptions.Select(exception => exception.Message),
            Is.EquivalentTo(new[] { "SetRoot failed", "dispose failed" }));
        Assert.That(view.Controller.Gateway, Is.Null);
    }

    /// <summary>
    /// ロールバックの後片付けで実体の破棄が失敗しても、残りの後片付けを最後まで試みる。
    /// </summary>
    /// <remarks>
    /// Host 世代の後片付けは accessory の実体・Cell の内容の実体・通知の受け取り・Native Host の
    /// 4 単位に分かれる。1 単位の失敗で残りを飛ばすと Native Host が解放されないまま残り、
    /// その世代が次の生成へ持ち越される。
    /// </remarks>
    [Test]
    public void AFailedAccessoryDisposeDuringRollbackStillReleasesTheHost()
    {
        Label header = new();
        Label footer = new();
        SettingsView view = new() { RootHeaderView = header, RootFooterView = footer };
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        // 先に実体化される root の header だけが破棄で失敗する。
        scope.Views.Materialized = lease =>
        {
            if (scope.Views.Leases.Count == 1)
            {
                lease.OnDispose = () => throw new InvalidOperationException("dispose failed");
            }
        };

        SettingsViewHandler handler = new();
        RecordingHostContainment containment = new(handler, scope.Gateway) { FailsToAdd = true };
        handler.Containment = containment;

        AggregateException? thrown = Assert.Throws<AggregateException>(() => view.Handler = handler);

        // 元の Host 生成の失敗が先頭に残る。
        Assert.That(
            thrown!.InnerExceptions[0].Message,
            Is.EqualTo("adding the host to its parent failed"));
        Assert.That(
            thrown.InnerExceptions.Select(exception => exception.Message),
            Does.Contain("dispose failed"));

        // 後続のリースも破棄され、通知の解除と Native Host の解放まで到達する。
        Assert.That(scope.Views.Leases, Has.Count.EqualTo(2));
        Assert.That(scope.Views.Leases[1].IsDisposed, Is.True);
        Assert.That(scope.Gateway.DetachInteractionsCount, Is.EqualTo(1));
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
    }

    /// <summary>通常の切断でも、実体の破棄の失敗で残りの後片付けを飛ばさない。</summary>
    [Test]
    public void AFailedAccessoryDisposeDuringDisconnectStillReleasesTheHost()
    {
        Label header = new();
        Label footer = new();
        SettingsView view = new() { RootHeaderView = header, RootFooterView = footer };
        GatewayScope scope = GatewayScope.ConnectWithoutHost(view);

        scope.Views.Materialized = lease =>
        {
            if (scope.Views.Leases.Count == 1)
            {
                lease.OnDispose = () => throw new InvalidOperationException("dispose failed");
            }
        };

        view.Handler = new SettingsViewHandler();

        AggregateException? thrown = Assert.Throws<AggregateException>(() => view.Handler = null);

        Assert.That(
            thrown!.InnerExceptions.Select(exception => exception.Message),
            Is.EquivalentTo(new[] { "dispose failed" }));
        Assert.That(scope.Views.Leases, Has.Count.EqualTo(2));
        Assert.That(scope.Views.Leases[1].IsDisposed, Is.True);
        Assert.That(scope.Gateway.DetachInteractionsCount, Is.EqualTo(1));
        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
    }

    /// <summary>
    /// 接続の失敗の後片付けは、置き場所側の破棄が失敗しても後片付け待ちの分を試みる。
    /// </summary>
    /// <remarks>
    /// 対応表への登録が途中で失敗すると、登録済みの Section の実体は後片付け待ちへ回り、
    /// 未登録の Section の実体は置き場所に残る。どちらか一方の失敗でもう一方を飛ばすと、
    /// 手放したはずの実体がどこからも破棄されなくなる。
    /// </remarks>
    [Test]
    public void AFailedPlacedDisposeDuringConnectFailureStillDisposesTheRetiredViews()
    {
        Section registered = new() { HeaderView = new Label() };
        Section failing = new()
        {
            HeaderView = new Label(),
            Cells = new UnsubscribableCellCollection(),
        };
        Section unregistered = new() { HeaderView = new Label() };
        SettingsView view = new() { Root = { registered, failing, unregistered } };

        FakeSettingsGateway gateway = new();
        FakeDispatcher dispatcher = new();
        FakeImageResolver images = new();
        FakeViewMaterializer views = new();

        // 置き場所に残る 3 件目の実体だけが破棄で失敗する。
        views.Materialized = lease =>
        {
            if (views.Leases.Count == 3)
            {
                lease.OnDispose = () => throw new InvalidOperationException("dispose failed");
            }
        };

        AggregateException? thrown = Assert.Throws<AggregateException>(
            () => view.ConnectGateway(() => gateway, dispatcher, images, views));

        Assert.That(
            thrown!.InnerExceptions.Select(exception => exception.Message),
            Is.EquivalentTo(new[] { "subscribing to the cells failed", "dispose failed" }));

        // 置き場所側が失敗しても、後片付け待ちの実体は破棄される。
        Assert.That(views.Leases, Has.Count.EqualTo(3));
        Assert.That(views.Leases[0].IsDisposed, Is.True);
        Assert.That(views.Leases[1].IsDisposed, Is.True);
        Assert.That(view.Controller.Gateway, Is.Null);
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
