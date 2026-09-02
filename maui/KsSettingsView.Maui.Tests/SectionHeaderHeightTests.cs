using System.Collections.Generic;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Section のヘッダ高さが設定ツリーに載り、変更が Section 単位の差し替えとして届くことを確認する。
/// </summary>
[TestFixture]
public class SectionHeaderHeightTests
{
    /// <summary>指定したヘッダ高さは接続時の設定ツリーにそのまま載る。</summary>
    [Test]
    public void HeaderHeightIsCarriedBySetRoot()
    {
        Section section = new() { HeaderText = "S", HeaderHeight = 60 };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].HeaderHeight, Is.EqualTo(60));
    }

    /// <summary>未指定のヘッダ高さは未指定のまま運ばれ、Native 既定に委ねられる。</summary>
    [Test]
    public void UnspecifiedHeaderHeightIsCarriedAsUnspecified()
    {
        Section section = new() { HeaderText = "S" };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].HeaderHeight, Is.Null);
    }

    /// <summary>ヘッダ高さの変更は Section の差し替え 1 回だけになり、内容更新には載らない。</summary>
    [Test]
    public void HeaderHeightChangeIsDeliveredAsSingleReplaceSection()
    {
        Section section = new() { HeaderText = "S", Cells = { new LabelCell { Title = "行" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.HeaderHeight = 60;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection, Is.SameAs(section));
        Assert.That(call.NewSection.HeaderHeight, Is.EqualTo(60));
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>差し替えでは配下 Cell の採番済み ID が引き継がれる。</summary>
    [Test]
    public void HeaderHeightChangeKeepsCellIds()
    {
        LabelCell first = new();
        SwitchCell second = new();
        Section section = new() { HeaderText = "S", Cells = { first, second } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        IReadOnlyList<string> before =
        [
            view.Controller.FindCellId(first)!,
            view.Controller.FindCellId(second)!,
        ];

        section.HeaderHeight = 60;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceSection>().RetainedCellIds, Is.EqualTo(before));
        Assert.That(view.Controller.FindCellId(first), Is.EqualTo(before[0]));
        Assert.That(view.Controller.FindCellId(second), Is.EqualTo(before[1]));
    }

    /// <summary>ヘッダ高さと可視性を続けて変えても、差し替えは 1 回にまとまる。</summary>
    [Test]
    public void HeaderHeightAndVisibilityChangesShareOneReplaceSection()
    {
        Section section = new() { HeaderText = "S", Cells = { new LabelCell() } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.HeaderHeight = 60;
        section.IsVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection.HeaderHeight, Is.EqualTo(60));
        Assert.That(call.NewSection.IsVisible, Is.False);
    }
}
