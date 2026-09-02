using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Root / Section の header・footer テキストが accessory の更新へ変換されることを確認する。
/// </summary>
[TestFixture]
public class AccessoryTests
{
    /// <summary>RootHeaderText の設定は root header の更新として配信される。</summary>
    [Test]
    public void RootHeaderTextIsDeliveredAsRootAccessory()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.RootHeaderText = "header";

        GatewayCall.UpdateAccessory call = scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.RootHeader));
        Assert.That(call.SectionId, Is.Null);
        Assert.That(call.Text, Is.EqualTo("header"));
    }

    /// <summary>RootFooterText への null 設定は表示のクリアとして配信される。</summary>
    [Test]
    public void RootFooterTextNullClearsRootAccessory()
    {
        SettingsView view = new() { RootFooterText = "footer" };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.RootFooterText = null;

        GatewayCall.UpdateAccessory call = scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.RootFooter));
        Assert.That(call.Text, Is.Null);
    }

    /// <summary>接続前に設定した root accessory は適用の呼び出しで反映される。</summary>
    [Test]
    public void RootAccessorySetBeforeConnectIsAppliedOnApply()
    {
        SettingsView view = new() { RootHeaderText = "header", RootFooterText = "footer" };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ApplyHostViews();

        IReadOnlyList<GatewayCall.UpdateAccessory> calls = scope.All<GatewayCall.UpdateAccessory>();
        Assert.That(calls, Has.Count.EqualTo(2));
        Assert.That(calls[0].Target, Is.EqualTo(KsAccessoryTarget.RootHeader));
        Assert.That(calls[0].Text, Is.EqualTo("header"));
        Assert.That(calls[1].Target, Is.EqualTo(KsAccessoryTarget.RootFooter));
        Assert.That(calls[1].Text, Is.EqualTo("footer"));
    }

    /// <summary>Native Host を解放しても所有値は残り、適用の呼び出しで復元される。</summary>
    [Test]
    public void RootAccessoryIsReappliedAfterHostRelease()
    {
        SettingsView view = new() { RootHeaderText = "header" };
        GatewayScope scope = GatewayScope.Connect(view);
        view.ApplyHostViews();
        scope.Reset();

        view.ReleaseHost();
        view.ApplyHostViews();

        Assert.That(scope.Gateway.ReleaseHostCount, Is.EqualTo(1));
        IReadOnlyList<GatewayCall.UpdateAccessory> calls = scope.All<GatewayCall.UpdateAccessory>();
        Assert.That(calls, Has.Count.EqualTo(2));
        Assert.That(calls[0].Target, Is.EqualTo(KsAccessoryTarget.RootHeader));
        Assert.That(calls[0].Text, Is.EqualTo("header"));
    }

    /// <summary>Section の FooterText 変更は該当 Section の accessory として配信される。</summary>
    [Test]
    public void SectionFooterTextIsDeliveredWithGatewayId()
    {
        Section section = new() { HeaderText = "header" };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        section.FooterText = "footer";

        GatewayCall.UpdateAccessory call = scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.SectionFooter));
        Assert.That(call.SectionId, Is.EqualTo(sectionId));
        Assert.That(call.Text, Is.EqualTo("footer"));
    }

    /// <summary>Section の HeaderText への null 設定はクリアとして配信される。</summary>
    [Test]
    public void SectionHeaderTextNullClearsSectionAccessory()
    {
        Section section = new() { HeaderText = "header" };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        section.HeaderText = null;

        GatewayCall.UpdateAccessory call = scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.SectionHeader));
        Assert.That(call.SectionId, Is.EqualTo(sectionId));
        Assert.That(call.Text, Is.Null);
    }

    /// <summary>接続前の root accessory の設定は配信されない。</summary>
    [Test]
    public void RootAccessorySetBeforeConnectIsNotDeliveredImmediately()
    {
        SettingsView view = new();
        FakeSettingsGateway gateway = new();
        FakeDispatcher dispatcher = new();

        view.RootHeaderText = "header";

        Assert.That(gateway.Calls, Is.Empty);
        view.ConnectGateway(() => gateway, dispatcher, new FakeImageResolver(), new FakeViewMaterializer());
        Assert.That(gateway.Calls.OfType<GatewayCall.UpdateAccessory>(), Is.Empty);
    }
}
