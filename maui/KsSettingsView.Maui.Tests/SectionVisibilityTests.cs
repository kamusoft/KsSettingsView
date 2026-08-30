using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// Section の可視性と header / footer の表示トグルの切り替えが Section 単位の差し替えとして届き、
/// 配下 Cell の identity と双方向バインドが切り替えをまたいで保たれることを確認する。
/// </summary>
[TestFixture]
public class SectionVisibilityTests
{
    /// <summary>Section は表示状態から始まり、接続時の設定ツリーにその値が載る。</summary>
    [Test]
    public void SectionDefaultsToVisible()
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(section.IsVisible, Is.True);
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].IsVisible, Is.True);
    }

    /// <summary>可視性の変更は Section の差し替え 1 回だけになり、内容更新には載らない。</summary>
    [Test]
    public void VisibilityChangeIsDeliveredAsSingleReplaceSection()
    {
        Section section = new() { Cells = { new LabelCell { Title = "行" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection, Is.SameAs(section));
        Assert.That(call.NewSection.IsVisible, Is.False);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>差し替えでは配下 Cell の採番済み ID が引き継がれる。</summary>
    [Test]
    public void VisibilityChangeKeepsCellIds()
    {
        LabelCell first = new();
        SwitchCell second = new();
        Section section = new() { Cells = { first, second } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        IReadOnlyList<string> before =
        [
            view.Controller.FindCellId(first)!,
            view.Controller.FindCellId(second)!,
        ];

        section.IsVisible = false;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceSection>().RetainedCellIds, Is.EqualTo(before));
        Assert.That(view.Controller.FindCellId(first), Is.EqualTo(before[0]));
        Assert.That(view.Controller.FindCellId(second), Is.EqualTo(before[1]));
        Assert.That(view.Controller.FindCell(before[1]), Is.SameAs(second));
    }

    /// <summary>非表示から復帰した後も、ユーザー操作の書き戻しが同じ ID で機能する。</summary>
    [Test]
    public void WritebackKeepsWorkingAcrossVisibilityToggle()
    {
        SwitchCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        string cellId = view.Controller.FindCellId(cell)!;

        section.IsVisible = false;
        scope.Flush();
        section.IsVisible = true;
        scope.Flush();

        scope.Gateway.Sink!.SwitchCellChanged(cellId, true);

        Assert.That(cell.On, Is.True);
    }

    /// <summary>非表示の間に変えた内容は、復帰したときの差し替えに載る。</summary>
    [Test]
    public void ContentChangedWhileHiddenAppearsOnRestore()
    {
        LabelCell cell = new() { ValueText = "前" };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsVisible = false;
        scope.Flush();

        cell.ValueText = "後";
        scope.Flush();

        section.IsVisible = true;
        scope.Flush();

        GatewayCall.ReplaceSection restored = scope.All<GatewayCall.ReplaceSection>().Last();
        Assert.That(restored.NewSection.IsVisible, Is.True);
        Assert.That(((LabelCell)restored.NewSection.Cells[0]).ValueText, Is.EqualTo("後"));
    }

    /// <summary>除去済みの Section の可視性変更は配信されない。</summary>
    [Test]
    public void VisibilityChangeOnRemovedSectionIsDropped()
    {
        Section section = new();
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);

        section.IsVisible = false;
        root.Remove(section);
        scope.Reset();
        scope.Flush();

        Assert.That(scope.All<GatewayCall.ReplaceSection>(), Is.Empty);
    }

    // ---- header / footer の表示トグル ----

    /// <summary>header / footer は表示状態から始まり、接続時の設定ツリーにその値が載る。</summary>
    [Test]
    public void HeaderAndFooterDefaultToVisible()
    {
        Section section = new() { HeaderText = "H", FooterText = "F" };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(section.IsHeaderVisible, Is.True);
        Assert.That(section.IsFooterVisible, Is.True);
        Section carried = scope.Single<GatewayCall.SetRoot>().Sections[0];
        Assert.That(carried.IsHeaderVisible, Is.True);
        Assert.That(carried.IsFooterVisible, Is.True);
    }

    /// <summary>初期構築時に指定したトグルは接続時の設定ツリーにそのまま載る。</summary>
    [Test]
    public void AccessoryVisibilityIsCarriedBySetRoot()
    {
        Section hiddenHeader = new() { HeaderText = "H", FooterText = "F", IsHeaderVisible = false };
        Section hiddenFooter = new() { HeaderText = "H", FooterText = "F", IsFooterVisible = false };
        SettingsView view = new() { Root = { hiddenHeader, hiddenFooter } };

        GatewayScope scope = GatewayScope.Connect(view);

        IReadOnlyList<Section> carried = scope.Single<GatewayCall.SetRoot>().Sections;
        Assert.That(carried[0].IsHeaderVisible, Is.False);
        Assert.That(carried[0].IsFooterVisible, Is.True, "footer 側は巻き込まれない");
        Assert.That(carried[1].IsFooterVisible, Is.False);
        Assert.That(carried[1].IsHeaderVisible, Is.True, "header 側は巻き込まれない");
    }

    /// <summary>header の表示トグルの変更は Section の差し替え 1 回だけになる。</summary>
    [Test]
    public void HeaderVisibilityChangeIsDeliveredAsSingleReplaceSection()
    {
        Section section = new() { HeaderText = "H", Cells = { new LabelCell { Title = "行" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsHeaderVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection, Is.SameAs(section));
        Assert.That(call.NewSection.IsHeaderVisible, Is.False);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>footer の表示トグルの変更も Section の差し替え 1 回として届く。</summary>
    [Test]
    public void FooterVisibilityChangeIsDeliveredAsSingleReplaceSection()
    {
        Section section = new() { FooterText = "F", Cells = { new LabelCell { Title = "行" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsFooterVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection, Is.SameAs(section));
        Assert.That(call.NewSection.IsFooterVisible, Is.False);
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>true へ戻す変更も差し替えとして届き、再表示が native へ伝わる。</summary>
    [Test]
    public void HeaderVisibilityRestoreIsDeliveredAsReplaceSection()
    {
        Section section = new() { HeaderText = "H" };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsHeaderVisible = false;
        scope.Flush();
        scope.Reset();

        section.IsHeaderVisible = true;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceSection>().NewSection.IsHeaderVisible, Is.True);
    }

    /// <summary>トグルの差し替えでは配下 Cell の採番済み ID と表示内容が保たれる。</summary>
    [Test]
    public void AccessoryVisibilityChangeKeepsCellIdentityAndContent()
    {
        LabelCell first = new() { Title = "一", ValueText = "値" };
        SwitchCell second = new() { Title = "二", On = true };
        Section section = new() { HeaderText = "H", Cells = { first, second } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        IReadOnlyList<string> before =
        [
            view.Controller.FindCellId(first)!,
            view.Controller.FindCellId(second)!,
        ];

        section.IsHeaderVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.RetainedCellIds, Is.EqualTo(before));
        Assert.That(view.Controller.FindCellId(first), Is.EqualTo(before[0]));
        Assert.That(view.Controller.FindCell(before[1]), Is.SameAs(second));
        Assert.That(((LabelCell)call.NewSection.Cells[0]).ValueText, Is.EqualTo("値"));
        Assert.That(((SwitchCell)call.NewSection.Cells[1]).On, Is.True);
    }

    /// <summary>可視性と header / footer のトグルを続けて変えても、差し替えは 1 回にまとまる。</summary>
    [Test]
    public void VisibilityAndAccessoryToggleChangesShareOneReplaceSection()
    {
        Section section = new() { HeaderText = "H", FooterText = "F", Cells = { new LabelCell() } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.IsHeaderVisible = false;
        section.IsFooterVisible = false;
        section.IsVisible = false;
        scope.Flush();

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.NewSection.IsHeaderVisible, Is.False);
        Assert.That(call.NewSection.IsFooterVisible, Is.False);
        Assert.That(call.NewSection.IsVisible, Is.False);
    }
}
