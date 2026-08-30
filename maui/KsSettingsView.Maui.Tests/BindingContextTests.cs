using System.Collections.ObjectModel;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>XAML で直接並べた Section / Cell への BindingContext 伝播を検証する。</summary>
[TestFixture]
public sealed class BindingContextTests
{
    /// <summary>SettingsView の BindingContext が Section と Cell へ届く。</summary>
    [Test]
    public void BindingContextReachesDirectlyPlacedSectionAndCell()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };

        object context = new Owner("name");
        view.BindingContext = context;

        Assert.That(section.BindingContext, Is.SameAs(context));
        Assert.That(cell.BindingContext, Is.SameAs(context));
    }

    /// <summary>Cell の Binding が SettingsView の BindingContext で解決される。</summary>
    [Test]
    public void CellBindingResolvesAgainstTheViewBindingContext()
    {
        LabelCell cell = new();
        cell.SetBinding(CellBase.TitleProperty, static (Owner owner) => owner.Name);
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };

        view.BindingContext = new Owner("bound title");

        Assert.That(cell.Title, Is.EqualTo("bound title"));
    }

    /// <summary>BindingContext 設定後に追加した Section / Cell へも届く。</summary>
    [Test]
    public void BindingContextReachesLaterAddedElements()
    {
        SettingsView view = new();
        object context = new Owner("later");
        view.BindingContext = context;

        Section section = new();
        view.Root.Add(section);
        LabelCell cell = new();
        section.Cells.Add(cell);

        Assert.That(section.BindingContext, Is.SameAs(context));
        Assert.That(cell.BindingContext, Is.SameAs(context));
    }

    /// <summary>コレクションを差し替えても新しい要素へ届く。</summary>
    [Test]
    public void BindingContextReachesReplacedCollections()
    {
        SettingsView view = new();
        object context = new Owner("replaced");
        view.BindingContext = context;

        LabelCell cell = new();
        Section section = new() { Cells = new ObservableCollection<CellBase> { cell } };
        view.Root = new ObservableCollection<Section> { section };

        Assert.That(section.BindingContext, Is.SameAs(context));
        Assert.That(cell.BindingContext, Is.SameAs(context));
    }

    /// <summary>ItemsSource から生成した Cell の BindingContext は item のままになる。</summary>
    [Test]
    public void GeneratedCellKeepsItsItemAsBindingContext()
    {
        ObservableCollection<string> items = ["first"];
        Section section = new()
        {
            ItemsSource = items,
            ItemTemplate = new DataTemplate(static () => new LabelCell()),
        };
        SettingsView view = new() { Root = { section } };

        view.BindingContext = new Owner("view context");

        Assert.That(section.Cells, Has.Count.EqualTo(1));
        Assert.That(section.Cells[0].BindingContext, Is.EqualTo("first"));
    }

}

/// <summary>BindingContext の供給元。</summary>
/// <param name="Name">Cell へ束縛する値</param>
internal sealed record Owner(string Name);
