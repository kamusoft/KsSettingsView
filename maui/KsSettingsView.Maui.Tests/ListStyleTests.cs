using System.Linq;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// 見た目スタイルが既定スタイルとは独立した経路で gateway へ届くことを確認する。
/// </summary>
[TestFixture]
public class ListStyleTests
{
    /// <summary>指定しなければ見た目スタイルは Classic になる。</summary>
    [Test]
    public void ListStyleDefaultsToClassic()
    {
        SettingsView view = new();

        Assert.That(view.ListStyle, Is.EqualTo(SettingsViewStyle.Classic));
    }

    /// <summary>既定のままなら、接続時に届く見た目スタイルも Classic になる。</summary>
    [Test]
    public void DefaultListStyleIsDeliveredAsClassic()
    {
        SettingsView view = new();

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.All<GatewayCall.SetStyle>().Last().Style, Is.EqualTo(SettingsViewStyle.Classic));
    }

    /// <summary>接続前に指定した見た目スタイルは、接続時に配信される。</summary>
    [Test]
    public void ListStyleSetBeforeConnectIsDeliveredOnConnect()
    {
        SettingsView view = new() { ListStyle = SettingsViewStyle.Modern };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.All<GatewayCall.SetStyle>().Last().Style, Is.EqualTo(SettingsViewStyle.Modern));
        Assert.That(scope.Gateway.Style, Is.EqualTo(SettingsViewStyle.Modern));
    }

    /// <summary>表示中の見た目スタイルの切り替えは、そのつど配信される。</summary>
    [Test]
    public void ListStyleChangeWhileConnectedIsDelivered()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ListStyle = SettingsViewStyle.Modern;

        Assert.That(scope.Single<GatewayCall.SetStyle>().Style, Is.EqualTo(SettingsViewStyle.Modern));
    }

    /// <summary>逆向きの切り替えも同じ経路で配信される。</summary>
    [Test]
    public void ListStyleChangeBackToClassicIsDelivered()
    {
        SettingsView view = new() { ListStyle = SettingsViewStyle.Modern };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ListStyle = SettingsViewStyle.Classic;

        Assert.That(scope.Single<GatewayCall.SetStyle>().Style, Is.EqualTo(SettingsViewStyle.Classic));
    }

    /// <summary>見た目スタイルの切り替えは設定ツリーの構造を触らない。</summary>
    [Test]
    public void ListStyleChangeDoesNotTouchTree()
    {
        Section section = new() { Cells = { new LabelCell { Title = "A" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ListStyle = SettingsViewStyle.Modern;
        scope.Flush();

        Assert.That(scope.Calls.Count, Is.EqualTo(1));
        Assert.That(scope.Single<GatewayCall.SetStyle>().Style, Is.EqualTo(SettingsViewStyle.Modern));
    }

    /// <summary>見た目スタイルは既定スタイルの写しには載らない。</summary>
    [Test]
    public void ListStyleIsNotCarriedInThemeSnapshot()
    {
        SettingsView view = new() { ListStyle = SettingsViewStyle.Modern };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.All<GatewayCall.SetTheme>().Last().Theme, Is.EqualTo(new KsThemeSnapshot()));
    }
}
