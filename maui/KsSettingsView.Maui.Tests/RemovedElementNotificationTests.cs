using System.Collections.ObjectModel;
using KsSettingsView.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// コレクションから外れた Section / Cell への通知が gateway へ届かないことを確認する。
/// </summary>
/// <remarks>
/// 未知 ID の accessory 更新は Store 側でも no-op になる (core/ADR-0020) が、
/// 除去済み要素の通知は MAUI 層の購読解除と対応表ガードで先に遮断する契約を保つ。
/// </remarks>
[TestFixture]
public class RemovedElementNotificationTests
{
    /// <summary>削除済み Section の HeaderText 変更は配信も例外も起こさない。</summary>
    [Test]
    public void RemovedSectionHeaderTextChangeIsSilent()
    {
        Section section = new();
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        root.Remove(section);
        scope.Reset();

        Assert.That(() => section.HeaderText = "changed", Throws.Nothing);
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>削除済み Section の FooterText 変更は配信も例外も起こさない。</summary>
    [Test]
    public void RemovedSectionFooterTextChangeIsSilent()
    {
        Section section = new();
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        root.Remove(section);
        scope.Reset();

        Assert.That(() => section.FooterText = "changed", Throws.Nothing);
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>置き換えで外れた Section の accessory 変更は配信されない。</summary>
    [Test]
    public void ReplacedSectionAccessoryChangeIsSilent()
    {
        Section original = new();
        SettingsRoot root = [original];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        root[0] = new Section();
        scope.Reset();

        original.HeaderText = "changed";

        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>Clear で外れた Section の accessory 変更は配信されない。</summary>
    [Test]
    public void SectionRemovedByClearIsSilent()
    {
        Section section = new();
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        root.Clear();
        scope.Reset();

        section.FooterText = "changed";

        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>削除済み Cell のプロパティ変更は flush の予約すら行わない。</summary>
    [Test]
    public void RemovedCellPropertyChangeIsSilent()
    {
        LabelCell cell = new();
        ObservableCollection<CellBase> cells = [cell];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        cells.Remove(cell);
        scope.Reset();

        cell.Title = "changed";
        cell.IsVisible = false;

        Assert.That(scope.Dispatcher.PendingCount, Is.EqualTo(0));
        scope.Flush();
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>Section ごと外れた Cell のプロパティ変更も配信されない。</summary>
    [Test]
    public void CellOfRemovedSectionIsSilent()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        root.Remove(section);
        scope.Reset();

        cell.Title = "changed";

        scope.Flush();
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>Cells の差し替えで外れた Cell のプロパティ変更も配信されない。</summary>
    [Test]
    public void CellDroppedByCellsReplacementIsSilent()
    {
        LabelCell dropped = new();
        Section section = new() { Cells = { dropped } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        section.Cells = new ObservableCollection<CellBase> { new LabelCell() };
        scope.Reset();

        dropped.Title = "changed";

        scope.Flush();
        Assert.That(scope.Calls, Is.Empty);
    }
}
