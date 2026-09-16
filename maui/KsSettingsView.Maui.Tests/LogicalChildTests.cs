using System.Collections.Generic;
using System.Collections.ObjectModel;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Section / Cell が所属先の論理子になり、所属が終われば解除されることを確認する。
/// </summary>
/// <remarks>
/// 論理子であることは Native Host の有無に依らず、コレクションへの所属の寿命と一致する。
/// 他所に所有されたままの要素を別の場所へ入れることはできない。
/// </remarks>
[TestFixture]
public class LogicalChildTests
{
    // ---- 論理子の付け外し ----

    /// <summary>Host 未接続でも、並べた Section と Cell はその場で論理子になる。</summary>
    [Test]
    public void SectionAndCellBecomeLogicalChildrenWithoutHost()
    {
        SettingsView view = new();
        Section section = new();
        LabelCell cell = new();

        view.Root.Add(section);
        section.Cells.Add(cell);

        Assert.That(section.Parent, Is.SameAs(view));
        Assert.That(cell.Parent, Is.SameAs(section));
    }

    /// <summary>Root から外した Section は論理子でなくなり、配下 Cell の所属は変わらない。</summary>
    [Test]
    public void RemovingSectionFromRootReleasesOnlyThatSection()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };

        view.Root.Remove(section);

        Assert.That(section.Parent, Is.Null);
        Assert.That(cell.Parent, Is.SameAs(section));
    }

    /// <summary>Section から外した Cell は論理子でなくなる。</summary>
    [Test]
    public void RemovingCellFromSectionReleasesIt()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };

        section.Cells.Remove(cell);

        Assert.That(cell.Parent, Is.Null);
    }

    /// <summary>Root を差し替えると、旧 Section が外れて新 Section が論理子になる。</summary>
    [Test]
    public void ReplacingRootCollectionMovesOwnership()
    {
        Section first = new();
        Section second = new();
        SettingsView view = new() { Root = { first } };

        view.Root = new ObservableCollection<Section> { second };

        Assert.That(first.Parent, Is.Null);
        Assert.That(second.Parent, Is.SameAs(view));
    }

    /// <summary>Clear は所属の終わりであり、同じ Cell を入れ直せば再び論理子になる。</summary>
    [Test]
    public void ResetReleasesAllAndReaddingAdoptsAgain()
    {
        LabelCell first = new();
        LabelCell second = new();
        Section section = new() { Cells = { first, second } };

        section.Cells.Clear();

        Assert.That(first.Parent, Is.Null);
        Assert.That(second.Parent, Is.Null);

        section.Cells.Add(first);

        Assert.That(first.Parent, Is.SameAs(section));
        Assert.That(second.Parent, Is.Null);
    }

    /// <summary>Cells を差し替えると、旧コレクションはもう所属の増減を届けない。</summary>
    [Test]
    public void ReplacingCellsCollectionMovesOwnershipAndStopsWatchingTheOldOne()
    {
        LabelCell first = new();
        LabelCell second = new();
        ObservableCollection<CellBase> original = [first];
        Section section = new() { Cells = original };

        section.Cells = new ObservableCollection<CellBase> { second };

        Assert.That(first.Parent, Is.Null);
        Assert.That(second.Parent, Is.SameAs(section));

        LabelCell late = new();
        original.Add(late);

        Assert.That(late.Parent, Is.Null);
    }

    /// <summary>二重に入れた Cell は、所属が 1 件でも残っている間は論理子のまま。</summary>
    [Test]
    public void CellAddedTwiceStaysAdoptedUntilEveryPlacementIsRemoved()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell, cell } };

        section.Cells.Remove(cell);

        Assert.That(cell.Parent, Is.SameAs(section));

        section.Cells.Remove(cell);

        Assert.That(cell.Parent, Is.Null);
    }

    /// <summary>ItemsSource を差し替えると、旧生成 Cell が外れ新生成 Cell が論理子になる。</summary>
    [Test]
    public void RegeneratingFromItemsSourceMovesOwnership()
    {
        Section section = new()
        {
            ItemTemplate = new DataTemplate(() => new LabelCell()),
            ItemsSource = new List<string> { "一" },
        };

        CellBase generated = section.Cells[0];
        Assert.That(generated.Parent, Is.SameAs(section));

        section.ItemsSource = new List<string> { "二" };

        CellBase regenerated = section.Cells[0];
        Assert.That(generated.Parent, Is.Null);
        Assert.That(regenerated.Parent, Is.SameAs(section));
        Assert.That(regenerated.BindingContext, Is.EqualTo("二"));
    }

    /// <summary>Host 接続中の除去でも論理子は解除され、構造更新はこれまでどおり配信される。</summary>
    [Test]
    public void RemovingCellWhileConnectedReleasesItAndStillDeliversTheStructureUpdate()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.Cells.Remove(cell);

        Assert.That(cell.Parent, Is.Null);
        Assert.That(scope.All<GatewayCall.RemoveCell>(), Has.Count.EqualTo(1));
    }

    // ---- 多重配置 ----

    /// <summary>他所に所有された Cell の追加は、Host 未接続でも追加の時点で例外になる。</summary>
    [Test]
    public void AddingCellOwnedByAnotherSettingsViewThrowsOnAdd()
    {
        LabelCell shared = new();
        Section first = new() { Cells = { shared } };
        SettingsView placed = new() { Root = { first } };
        GatewayScope scope = GatewayScope.Connect(placed).Reset();
        placed.BindingContext = "A";

        Section second = new();
        _ = new SettingsView { Root = { second } };

        Assert.That(
            () => second.Cells.Add(shared),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));

        // 正しく置かれている側の所有と継承 BindingContext は動かない。
        Assert.That(shared.Parent, Is.SameAs(first));
        Assert.That(shared.BindingContext, Is.EqualTo("A"));
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>他の SettingsView に所有された Section の追加も追加の時点で例外になる。</summary>
    [Test]
    public void AddingSectionOwnedByAnotherSettingsViewThrowsOnAdd()
    {
        Section shared = new();
        SettingsView placed = new() { Root = { shared } };
        SettingsView other = new();

        Assert.That(
            () => other.Root.Add(shared),
            Throws.InvalidOperationException.With.Message.Contains("same Section instance"));
        Assert.That(shared.Parent, Is.SameAs(placed));
    }

    /// <summary>設定ツリーから外れた Section が所有したままの Cell も追加できない。</summary>
    [Test]
    public void AddingCellStillOwnedByADetachedSectionThrows()
    {
        LabelCell cell = new();
        Section detached = new() { Cells = { cell } };
        SettingsView view = new() { Root = { detached } };
        view.Root.Remove(detached);

        Section other = new();

        Assert.That(
            () => other.Cells.Add(cell),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));
        Assert.That(cell.Parent, Is.SameAs(detached));
    }

    /// <summary>所属を解いてから入れ直した Cell は、新しい所属先の論理子になる。</summary>
    [Test]
    public void CellMovedAfterRemovalIsAdoptedByTheNewOwner()
    {
        LabelCell cell = new();
        Section first = new() { Cells = { cell } };
        Section second = new();

        first.Cells.Remove(cell);
        second.Cells.Add(cell);

        Assert.That(cell.Parent, Is.SameAs(second));
    }

    /// <summary>
    /// Host を手放した側から移した Cell は、新しい所属先の論理子として新しい値で変換される。
    /// </summary>
    [Test]
    public void CellMovedAfterHostReleaseIsConvertedWithTheNewOwnersValue()
    {
        LabelCell cell = new();
        cell.SetBinding(CellBase.TitleProperty, static (string source) => source.Length);
        Section first = new() { Cells = { cell } };
        SettingsView placed = new() { Root = { first }, BindingContext = "12" };
        _ = GatewayScope.Connect(placed);
        placed.ReleaseHost();
        first.Cells.Remove(cell);

        Section second = new();
        SettingsView other = new() { Root = { second }, BindingContext = "1234" };
        second.Cells.Add(cell);

        Assert.That(cell.Parent, Is.SameAs(second));

        GatewayScope scope = GatewayScope.Connect(other);

        // 最初の変換に載るのは、新しい所属先から継承した BindingContext で解決した値になる。
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].Cells[0].Title, Is.EqualTo("4"));
    }

    /// <summary>別の SettingsView に置かれた View を accessory へ置くと、変換時に例外になる。</summary>
    [Test]
    public void PlacingAccessoryViewOwnedByAnotherSettingsViewThrowsOnConversion()
    {
        Label accessory = new();
        Section placedSection = new() { HeaderView = accessory };
        SettingsView placed = new() { Root = { placedSection } };
        GatewayScope placedScope = GatewayScope.Connect(placed);
        placedScope.Attach();
        placedScope.Reset();

        Section target = new();
        SettingsView other = new() { Root = { target } };
        target.HeaderView = accessory;

        // 置いた時点では尋ねる相手がおらず、所有も動かない。
        Assert.That(accessory.Parent, Is.SameAs(placedSection));

        Assert.That(
            () => GatewayScope.Connect(other),
            Throws.InvalidOperationException.With.Message.Contains("same View instance"));
        Assert.That(accessory.Parent, Is.SameAs(placedSection));
        Assert.That(placedScope.Calls, Is.Empty);
    }

    /// <summary>別の SettingsView の CustomCell に置かれた View を内容へ置くと、変換時に例外になる。</summary>
    [Test]
    public void PlacingContentViewOwnedByAnotherSettingsViewThrowsOnConversion()
    {
        Label content = new();
        CustomCell placedCell = new() { Content = content };
        SettingsView placed = new() { Root = { new Section { Cells = { placedCell } } } };
        GatewayScope placedScope = GatewayScope.Connect(placed);
        placedScope.Attach();
        placedScope.Reset();

        CustomCell target = new();
        SettingsView other = new() { Root = { new Section { Cells = { target } } } };
        target.Content = content;

        Assert.That(content.Parent, Is.SameAs(placedCell));

        Assert.That(
            () => GatewayScope.Connect(other),
            Throws.InvalidOperationException.With.Message.Contains("same View instance"));
        Assert.That(content.Parent, Is.SameAs(placedCell));
        Assert.That(placedScope.Calls, Is.Empty);
    }

    /// <summary>外れた Section が所有したままの View を accessory へ置くと例外になる。</summary>
    [Test]
    public void PlacingAccessoryViewStillOwnedByADetachedSectionThrows()
    {
        Label accessory = new();
        Section detached = new() { HeaderView = accessory };
        SettingsView view = new() { Root = { detached } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();
        view.Root.Remove(detached);

        Section target = new();
        view.Root.Add(target);

        Assert.That(
            () => target.HeaderView = accessory,
            Throws.InvalidOperationException.With.Message.Contains("same View instance"));
        Assert.That(accessory.Parent, Is.SameAs(detached));
        Assert.That(target.HeaderView, Is.Null);
    }

    /// <summary>
    /// 他所に所有された Section を含むコレクションへの差し替えは、旧コレクションの状態を動かさない。
    /// </summary>
    [Test]
    public void ReplacingRootWithASectionOwnedElsewhereLeavesTheOldPlacementUntouched()
    {
        Section shared = new();
        SettingsView placed = new() { Root = { shared } };

        Section first = new();
        ObservableCollection<Section> original = [first];
        SettingsView view = new();
        view.SetValue(SettingsView.RootProperty, original);
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Assert.That(
            () => view.SetValue(
                SettingsView.RootProperty,
                new ObservableCollection<Section> { shared }),
            Throws.InvalidOperationException.With.Message.Contains("same Section instance"));

        // 旧コレクションの Section の所有も、正しく置かれている側の所有も動かない。
        Assert.That(first.Parent, Is.SameAs(view));
        Assert.That(shared.Parent, Is.SameAs(placed));
        Assert.That(scope.Calls, Is.Empty);

        // 変換経路の監視先も旧コレクションのまま残る。
        Section late = new();
        original.Add(late);

        Assert.That(late.Parent, Is.SameAs(view));
        Assert.That(scope.All<GatewayCall.InsertSection>(), Has.Count.EqualTo(1));
    }

    // ---- 非 observable なコレクション ----

    /// <summary>接続前に素の List へ足した Section も、接続の時点で論理子になる。</summary>
    [Test]
    public void SectionAddedToPlainRootListBeforeConnectBecomesLogicalChildOnConnect()
    {
        List<Section> root = [];
        SettingsView view = new();
        view.SetValue(SettingsView.RootProperty, root);

        Section section = new();
        root.Add(section);

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(section.Parent, Is.SameAs(view));
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(1));
    }

    /// <summary>接続前に素の List へ足した Cell も、接続の時点で論理子になる。</summary>
    [Test]
    public void CellAddedToPlainCellsListBeforeConnectBecomesLogicalChildOnConnect()
    {
        List<CellBase> cells = [];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };

        LabelCell cell = new();
        cells.Add(cell);

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(cell.Parent, Is.SameAs(section));
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].Cells, Has.Count.EqualTo(1));
    }

    /// <summary>接続前に素の List から外した Section は、接続の時点で論理子でなくなる。</summary>
    [Test]
    public void SectionRemovedFromPlainRootListBeforeConnectIsReleasedOnConnect()
    {
        Section section = new();
        List<Section> root = [section];
        SettingsView view = new();
        view.SetValue(SettingsView.RootProperty, root);

        Assert.That(section.Parent, Is.SameAs(view));

        root.Remove(section);
        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(section.Parent, Is.Null);
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Is.Empty);
    }

    /// <summary>接続済みの Root へ足した Section の、素の List に居る Cell も論理子になる。</summary>
    [Test]
    public void CellInPlainCellsListBecomesLogicalChildWhenTheSectionIsAddedToConnectedRoot()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        List<CellBase> cells = [];
        Section section = new() { Cells = cells };
        LabelCell cell = new();
        cells.Add(cell);

        view.Root.Add(section);

        Assert.That(cell.Parent, Is.SameAs(section));
        Assert.That(scope.Single<GatewayCall.InsertSection>().Section.Cells, Has.Count.EqualTo(1));
    }

    /// <summary>接続済みの Root の Section を差し替えたとき、素の List に居る Cell も論理子になる。</summary>
    [Test]
    public void CellInPlainCellsListBecomesLogicalChildWhenItsSectionReplacesAConnectedOne()
    {
        SettingsView view = new() { Root = { new Section() } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        List<CellBase> cells = [];
        Section replacement = new() { Cells = cells };
        LabelCell cell = new();
        cells.Add(cell);

        view.Root[0] = replacement;

        Assert.That(cell.Parent, Is.SameAs(replacement));
        Assert.That(scope.Single<GatewayCall.ReplaceSection>().NewSection.Cells, Has.Count.EqualTo(1));
    }

    /// <summary>Section 自身のプロパティ変更で送り直すときも、素の List に居る Cell が論理子になる。</summary>
    [Test]
    public void CellInPlainCellsListBecomesLogicalChildWhenTheSectionItselfIsReplaced()
    {
        List<CellBase> cells = [];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        LabelCell cell = new();
        cells.Add(cell);

        section.HeaderHeight = 60;
        scope.Flush();

        Assert.That(cell.Parent, Is.SameAs(section));
        Assert.That(scope.Single<GatewayCall.ReplaceSection>().NewSection.Cells, Has.Count.EqualTo(1));
    }
}
