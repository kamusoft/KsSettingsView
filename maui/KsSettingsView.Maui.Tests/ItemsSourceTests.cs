using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Tests.Fakes;
using KsSettingsView.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// ItemsSource / ItemTemplate / TemplateStartIndex による生成と、items の変更の追随を確認する。
/// </summary>
[TestFixture]
public class ItemsSourceTests
{
    /// <summary>items とテンプレートから Cell が生成され、BindingContext が item になる。</summary>
    [Test]
    public void CellsAreGeneratedWithItemAsBindingContext()
    {
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(section.Cells, Has.Count.EqualTo(3));
        Assert.That(
            section.Cells.Select(cell => cell.BindingContext),
            Is.EqualTo(new object[] { "a", "b", "c" }));
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections[0].Cells, Has.Count.EqualTo(3));
    }

    /// <summary>SettingsView 直下でも同じ形で Section が生成される。</summary>
    [Test]
    public void SectionsAreGeneratedWithItemAsBindingContext()
    {
        ObservableCollection<string> items = ["a", "b"];
        SettingsView view = new() { ItemsSource = items, ItemTemplate = SectionTemplate() };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(view.Root, Has.Count.EqualTo(2));
        Assert.That(
            view.Root.Select(section => section.BindingContext),
            Is.EqualTo(new object[] { "a", "b" }));
        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(2));
    }

    /// <summary>ItemTemplate が未設定の間は生成せず、設定した時点で生成する。</summary>
    [Test]
    public void TemplateSetAfterItemsSourceGeneratesThroughConversionPath()
    {
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new() { ItemsSource = items };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        Assert.That(section.Cells, Is.Empty);

        section.ItemTemplate = CellTemplate();

        Assert.That(section.Cells, Has.Count.EqualTo(3));
        Assert.That(scope.All<GatewayCall.InsertCell>(), Has.Count.EqualTo(3));
    }

    /// <summary>表示中の ItemTemplate 変更は生成分を作り直す。</summary>
    [Test]
    public void ChangingTemplateWhileConnectedRegenerates()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.ItemTemplate = new DataTemplate(() => new LabelCell { Title = "regenerated" });

        Assert.That(section.Cells, Has.Count.EqualTo(2));
        Assert.That(section.Cells.Select(cell => cell.Title), Is.All.EqualTo("regenerated"));
        Assert.That(scope.All<GatewayCall.RemoveCell>(), Has.Count.EqualTo(2));
        Assert.That(scope.All<GatewayCall.InsertCell>(), Has.Count.EqualTo(2));
    }

    /// <summary>表示中の TemplateStartIndex 変更は生成分を作り直して並べ位置を変える。</summary>
    [Test]
    public void ChangingTemplateStartIndexMovesGeneratedRange()
    {
        LabelCell manual = new() { Title = "manual" };
        ObservableCollection<string> items = ["a", "b"];
        Section section = new()
        {
            Cells = { manual },
            ItemsSource = items,
            ItemTemplate = CellTemplate(),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);
        Assert.That(section.Cells[2], Is.SameAs(manual));

        section.TemplateStartIndex = 1;

        Assert.That(section.Cells, Has.Count.EqualTo(3));
        Assert.That(section.Cells[0], Is.SameAs(manual));
        Assert.That(
            section.Cells.Skip(1).Select(cell => cell.BindingContext),
            Is.EqualTo(new object[] { "a", "b" }));
    }

    /// <summary>items への追加は生成先へミラーされる。</summary>
    [Test]
    public void ItemAddIsMirrored()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        items.Add("c");

        Assert.That(section.Cells, Has.Count.EqualTo(3));
        Assert.That(section.Cells[2].BindingContext, Is.EqualTo("c"));
        Assert.That(scope.Single<GatewayCall.InsertCell>().Index, Is.EqualTo(2));
    }

    /// <summary>先頭への items 挿入も対応する位置へミラーされる。</summary>
    [Test]
    public void ItemInsertAtHeadIsMirrored()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        items.Insert(0, "z");

        Assert.That(section.Cells[0].BindingContext, Is.EqualTo("z"));
        Assert.That(scope.Single<GatewayCall.InsertCell>().Index, Is.EqualTo(0));
    }

    /// <summary>items からの除去は生成先へミラーされる。</summary>
    [Test]
    public void ItemRemoveIsMirrored()
    {
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        items.RemoveAt(1);

        Assert.That(
            section.Cells.Select(cell => cell.BindingContext),
            Is.EqualTo(new object[] { "a", "c" }));
        Assert.That(scope.All<GatewayCall.RemoveCell>(), Has.Count.EqualTo(1));
    }

    /// <summary>items の置き換えは生成先の置き換えとしてミラーされる。</summary>
    [Test]
    public void ItemReplaceIsMirrored()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string replacedId = view.Controller.FindCellId(section.Cells[1])!;
        scope.Reset();

        items[1] = "z";

        Assert.That(section.Cells[1].BindingContext, Is.EqualTo("z"));
        Assert.That(scope.Single<GatewayCall.ReplaceCell>().CellId, Is.EqualTo(replacedId));
        Assert.That(view.Controller.FindCellId(section.Cells[1]), Is.EqualTo(replacedId));
    }

    /// <summary>items の移動は生成先の移動としてミラーされる。</summary>
    [Test]
    public void ItemMoveIsMirrored()
    {
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string movedId = view.Controller.FindCellId(section.Cells[0])!;
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        items.Move(0, 2);

        Assert.That(
            section.Cells.Select(cell => cell.BindingContext),
            Is.EqualTo(new object[] { "b", "c", "a" }));
        Assert.That(scope.Single<GatewayCall.MoveCell>().CellId, Is.EqualTo(movedId));
        Assert.That(scope.Gateway.CellIdsOf(sectionId)[2], Is.EqualTo(movedId));
    }

    /// <summary>複数 items の前方への移動でも、生成物・対応表・表示側の並びが items と揃う。</summary>
    [Test]
    public void ForwardRangeItemMoveKeepsGeneratedCellOrder()
    {
        RangeMoveCollection<string> items = ["a", "b", "c", "d"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        LabelCell manual = new() { Title = "manual" };
        section.Cells.Insert(1, manual);

        items.MoveRange(0, 2, 2);

        Assert.That(
            GeneratedContexts(section, manual),
            Is.EqualTo(new object[] { "c", "d", "a", "b" }));
        AssertCellOrderIsConsistent(view, scope, section);
    }

    /// <summary>複数 items の後方への移動でも、生成物・対応表・表示側の並びが items と揃う。</summary>
    [Test]
    public void BackwardRangeItemMoveKeepsGeneratedCellOrder()
    {
        RangeMoveCollection<string> items = ["a", "b", "c", "d"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        LabelCell manual = new() { Title = "manual" };
        section.Cells.Insert(1, manual);

        items.MoveRange(2, 0, 2);

        Assert.That(
            GeneratedContexts(section, manual),
            Is.EqualTo(new object[] { "c", "d", "a", "b" }));
        AssertCellOrderIsConsistent(view, scope, section);
    }

    /// <summary>SettingsView 直下でも、複数 items の前方への移動で Section の並びが items と揃う。</summary>
    [Test]
    public void ForwardRangeItemMoveKeepsGeneratedSectionOrder()
    {
        RangeMoveCollection<string> items = ["a", "b", "c", "d"];
        SettingsView view = new() { ItemsSource = items, ItemTemplate = SectionTemplate() };
        GatewayScope scope = GatewayScope.Connect(view);
        Section manual = new() { HeaderText = "manual" };
        view.Root.Insert(1, manual);

        items.MoveRange(0, 2, 2);

        Assert.That(
            GeneratedContexts(view, manual),
            Is.EqualTo(new object[] { "c", "d", "a", "b" }));
        AssertSectionOrderIsConsistent(view, scope);
    }

    /// <summary>SettingsView 直下でも、複数 items の後方への移動で Section の並びが items と揃う。</summary>
    [Test]
    public void BackwardRangeItemMoveKeepsGeneratedSectionOrder()
    {
        RangeMoveCollection<string> items = ["a", "b", "c", "d"];
        SettingsView view = new() { ItemsSource = items, ItemTemplate = SectionTemplate() };
        GatewayScope scope = GatewayScope.Connect(view);
        Section manual = new() { HeaderText = "manual" };
        view.Root.Insert(1, manual);

        items.MoveRange(2, 0, 2);

        Assert.That(
            GeneratedContexts(view, manual),
            Is.EqualTo(new object[] { "c", "d", "a", "b" }));
        AssertSectionOrderIsConsistent(view, scope);
    }

    /// <summary>既に配置済みの Cell を返すテンプレートを設定すると例外になる。</summary>
    [Test]
    public void TemplateCreatingAlreadyPlacedCellThrows()
    {
        LabelCell shared = new();
        Section placed = new() { Cells = { shared } };
        ObservableCollection<string> items = ["a"];
        Section generating = new() { ItemsSource = items };
        SettingsView view = new() { Root = { placed, generating } };
        GatewayScope.Connect(view);

        Assert.That(
            () => generating.ItemTemplate = new DataTemplate(() => shared),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));
    }

    /// <summary>生成区間へ手動挿入した Cell は items の Reset で残る。</summary>
    [Test]
    public void ResetKeepsManuallyInsertedCell()
    {
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);
        LabelCell manual = new() { Title = "manual" };
        section.Cells.Insert(1, manual);

        items.Clear();

        Assert.That(section.Cells, Has.Count.EqualTo(1));
        Assert.That(section.Cells[0], Is.SameAs(manual));
    }

    /// <summary>生成区間へ手動挿入した Cell は Reset 後の再生成でも残る。</summary>
    [Test]
    public void ResetWithNewItemsKeepsManuallyInsertedCell()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);
        LabelCell manual = new() { Title = "manual" };
        section.Cells.Insert(1, manual);

        items.Clear();
        items.Add("z");

        Assert.That(section.Cells, Has.Count.EqualTo(2));
        Assert.That(section.Cells, Contains.Item(manual));
        Assert.That(section.Cells.Any(cell => Equals(cell.BindingContext, "z")), Is.True);
    }

    /// <summary>ItemsSource の null 化はテンプレ生成分だけを取り除く。</summary>
    [Test]
    public void NullItemsSourceKeepsManualCells()
    {
        LabelCell manual = new() { Title = "manual" };
        ObservableCollection<string> items = ["a", "b", "c"];
        Section section = new()
        {
            Cells = { manual },
            ItemsSource = items,
            ItemTemplate = CellTemplate(),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.ItemsSource = null;

        Assert.That(section.Cells, Has.Count.EqualTo(1));
        Assert.That(section.Cells[0], Is.SameAs(manual));
        Assert.That(scope.All<GatewayCall.RemoveCell>(), Has.Count.EqualTo(3));
    }

    /// <summary>null 化した後は items の変更を追随しない。</summary>
    [Test]
    public void ItemsAreNotMirroredAfterItemsSourceIsCleared()
    {
        ObservableCollection<string> items = ["a"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        section.ItemsSource = null;
        items.Add("b");

        Assert.That(section.Cells, Is.Empty);
    }

    /// <summary>Cell を生成すべきテンプレートが別の型を返すと例外になる。</summary>
    [Test]
    public void TemplateCreatingWrongTypeForCellsThrows()
    {
        ObservableCollection<string> items = ["a"];
        Section section = new() { ItemsSource = items };

        Assert.That(
            () => section.ItemTemplate = SectionTemplate(),
            Throws.InvalidOperationException.With.Message.Contains("must create a CellBase"));
    }

    /// <summary>Section を生成すべきテンプレートが別の型を返すと例外になる。</summary>
    [Test]
    public void TemplateCreatingWrongTypeForSectionsThrows()
    {
        ObservableCollection<string> items = ["a"];
        SettingsView view = new() { ItemsSource = items };

        Assert.That(
            () => view.ItemTemplate = CellTemplate(),
            Throws.InvalidOperationException.With.Message.Contains("must create a Section"));
    }

    /// <summary>手動追加の Cell の後ろから生成させられる。</summary>
    [Test]
    public void TemplateStartIndexPlacesGeneratedAfterManualCells()
    {
        LabelCell manual = new() { Title = "manual" };
        ObservableCollection<string> items = ["a", "b"];
        Section section = new()
        {
            Cells = { manual },
            TemplateStartIndex = 1,
            ItemsSource = items,
            ItemTemplate = CellTemplate(),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        Assert.That(section.Cells[0], Is.SameAs(manual));
        Assert.That(
            section.Cells.Skip(1).Select(cell => cell.BindingContext),
            Is.EqualTo(new object[] { "a", "b" }));
    }

    /// <summary>生成先コレクションを差し替えると、生成分は新しい方へ作り直される。</summary>
    [Test]
    public void ReplacingCellsRegeneratesIntoNewCollection()
    {
        ObservableCollection<string> items = ["a", "b"];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        LabelCell manual = new() { Title = "manual" };
        section.Cells = new ObservableCollection<CellBase> { manual };

        Assert.That(section.Cells, Has.Count.EqualTo(3));
        Assert.That(section.Cells, Contains.Item(manual));
        Assert.That(
            section.Cells.Count(cell => cell.BindingContext is "a" or "b"),
            Is.EqualTo(2));
        Assert.That(view.Controller.FindSectionId(section), Is.EqualTo(sectionId));
        Assert.That(
            section.Cells.All(cell => view.Controller.FindCellId(cell) is not null),
            Is.True);
        Assert.That(scope.Gateway.CellIdsOf(sectionId), Has.Count.EqualTo(3));
    }

    /// <summary>
    /// 差し込みに失敗した生成物も控えに残り、items を外したときに取り除かれる。
    /// </summary>
    /// <remarks>
    /// 既に別の Section へ置かれている Cell を返すテンプレートは、生成先へ差し込む段で拒否される。
    /// 控えを差し込みより先に更新していないと、生成先に入った要素が控えから漏れて取り残される。
    /// </remarks>
    [Test]
    public void GeneratedElementIsTrackedEvenWhenInsertIsRejected()
    {
        LabelCell shared = new();
        Section placed = new() { Cells = { shared } };
        ObservableCollection<string> items = [];
        Section generating = new() { ItemsSource = items, ItemTemplate = new DataTemplate(() => shared) };
        SettingsView view = new() { Root = { placed, generating } };
        GatewayScope.Connect(view);

        Assert.Throws<InvalidOperationException>(() => items.Add("a"));
        Assert.That(generating.Cells, Has.Count.EqualTo(1));

        generating.ItemsSource = null;

        Assert.That(generating.Cells, Is.Empty);
    }

    private static DataTemplate CellTemplate() => new(() => new LabelCell());

    private static DataTemplate SectionTemplate() => new(() => new Section());


    /// <summary>手動で置いた要素を除いた、テンプレ生成分の BindingContext を並び順で取り出す。</summary>
    /// <param name="section">対象の Section</param>
    /// <param name="manual">手動で置いた Cell</param>
    private static IEnumerable<object?> GeneratedContexts(Section section, CellBase manual)
        => section.Cells
            .Where(cell => !ReferenceEquals(cell, manual))
            .Select(cell => cell.BindingContext);

    /// <summary>手動で置いた要素を除いた、テンプレ生成分の BindingContext を並び順で取り出す。</summary>
    /// <param name="view">対象の SettingsView</param>
    /// <param name="manual">手動で置いた Section</param>
    private static IEnumerable<object?> GeneratedContexts(SettingsView view, Section manual)
        => view.Root
            .Where(section => !ReferenceEquals(section, manual))
            .Select(section => section.BindingContext);

    /// <summary>Cell の並びが、対応表と表示側の並びと一致していることを確かめる。</summary>
    /// <param name="view">対象の SettingsView</param>
    /// <param name="scope">接続した gateway の足場</param>
    /// <param name="section">対象の Section</param>
    private static void AssertCellOrderIsConsistent(SettingsView view, GatewayScope scope, Section section)
    {
        string sectionId = view.Controller.FindSectionId(section)!;
        Assert.That(
            section.Cells.Select(cell => view.Controller.FindCellId(cell)),
            Is.EqualTo(scope.Gateway.CellIdsOf(sectionId)));
    }

    /// <summary>Section の並びが、対応表と表示側の並びと一致していることを確かめる。</summary>
    /// <param name="view">対象の SettingsView</param>
    /// <param name="scope">接続した gateway の足場</param>
    private static void AssertSectionOrderIsConsistent(SettingsView view, GatewayScope scope)
        => Assert.That(
            view.Root.Select(section => view.Controller.FindSectionId(section)),
            Is.EqualTo(scope.Gateway.SectionIds));
}
