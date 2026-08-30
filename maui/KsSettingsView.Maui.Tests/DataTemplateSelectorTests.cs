using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Maui.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// ItemTemplate にテンプレートの出し分けが設定されたとき、item ごとに実テンプレートが選び直されて
/// 実体化されることを確認する。
/// </summary>
[TestFixture]
public class DataTemplateSelectorTests
{
    /// <summary>Section 配下では item ごとに違う Cell 種が生成される。</summary>
    [Test]
    public void CellsAreGeneratedFromSelectedTemplate()
    {
        ObservableCollection<string> items = ["switch", "label"];
        Section section = new() { ItemsSource = items, ItemTemplate = new CellSelector() };
        SettingsView view = new() { Root = { section } };

        GatewayScope.Connect(view);

        Assert.That(section.Cells.Select(cell => cell.GetType()), Is.EqualTo(new[]
        {
            typeof(SwitchCell),
            typeof(LabelCell),
        }));
        Assert.That(section.Cells.Select(cell => cell.BindingContext), Is.EqualTo(items));
    }

    /// <summary>Section 配下の出し分けには、テンプレートを持つ Section が渡される。</summary>
    [Test]
    public void CellSelectorReceivesOwningSectionAsContainer()
    {
        RecordingCellSelector selector = new();
        Section section = new() { ItemsSource = new[] { "a" }, ItemTemplate = selector };
        SettingsView view = new() { Root = { section } };

        GatewayScope.Connect(view);

        Assert.That(selector.Containers, Is.EqualTo(new object[] { section }));
    }

    /// <summary>SettingsView 直下でも item ごとに Section テンプレートが選び直される。</summary>
    [Test]
    public void SectionsAreGeneratedFromSelectedTemplate()
    {
        ObservableCollection<string> items = ["header", "plain"];
        SettingsView view = new() { ItemsSource = items, ItemTemplate = new SectionSelector() };

        GatewayScope.Connect(view);

        Assert.That(
            view.Root.Select(section => section.HeaderText),
            Is.EqualTo(new string?[] { "ヘッダあり", null }));
    }

    /// <summary>SettingsView 直下の出し分けには、テンプレートを持つ SettingsView が渡される。</summary>
    [Test]
    public void SectionSelectorReceivesSettingsViewAsContainer()
    {
        RecordingSectionSelector selector = new();
        SettingsView view = new() { ItemsSource = new[] { "a" }, ItemTemplate = selector };

        GatewayScope.Connect(view);

        Assert.That(selector.Containers, Is.EqualTo(new object[] { view }));
    }

    /// <summary>items が増えたときも、増えた item に対して選び直される。</summary>
    [Test]
    public void AddedItemUsesSelectedTemplate()
    {
        ObservableCollection<string> items = ["label"];
        Section section = new() { ItemsSource = items, ItemTemplate = new CellSelector() };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        items.Add("switch");

        Assert.That(section.Cells.Select(cell => cell.GetType()), Is.EqualTo(new[]
        {
            typeof(LabelCell),
            typeof(SwitchCell),
        }));
    }

    /// <summary>items が差し替わったときも、新しい item に対して選び直される。</summary>
    [Test]
    public void ReplacedItemUsesSelectedTemplate()
    {
        ObservableCollection<string> items = ["label"];
        Section section = new() { ItemsSource = items, ItemTemplate = new CellSelector() };
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        items[0] = "switch";

        Assert.That(section.Cells.Single(), Is.TypeOf<SwitchCell>());
    }

    /// <summary>テンプレートを選べない item は実体化できず、その場で失敗する。</summary>
    [Test]
    public void MissingTemplateFails()
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        section.ItemTemplate = new CellSelector();

        Assert.Throws<InvalidOperationException>(() => section.ItemsSource = new[] { "unknown" });
    }

    /// <summary>選ばれたテンプレートが別種の要素を作る場合も、既存の経路と同じく失敗する。</summary>
    [Test]
    public void WrongElementTypeFails()
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        section.ItemTemplate = new WrongTypeSelector();

        Assert.Throws<InvalidOperationException>(() => section.ItemsSource = new[] { "a" });
    }

    /// <summary>出し分けが別の出し分けを返す場合も、既存の経路と同じ例外型で失敗する。</summary>
    [Test]
    public void NestedSelectorFailsInSection()
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };
        GatewayScope.Connect(view);

        section.ItemTemplate = new NestedCellSelector();

        InvalidOperationException? exception = Assert.Throws<InvalidOperationException>(
            () => section.ItemsSource = new[] { "a" });
        Assert.That(exception!.InnerException, Is.TypeOf<NotSupportedException>());
    }

    /// <summary>SettingsView 直下でも、出し分けが出し分けを返せば同じ例外型で失敗する。</summary>
    [Test]
    public void NestedSelectorFailsUnderSettingsView()
    {
        SettingsView view = new();
        GatewayScope.Connect(view);

        view.ItemTemplate = new NestedSectionSelector();

        InvalidOperationException? exception = Assert.Throws<InvalidOperationException>(
            () => view.ItemsSource = new[] { "a" });
        Assert.That(exception!.InnerException, Is.TypeOf<NotSupportedException>());
    }

    /// <summary>item の文字列で Cell 種を出し分けるテンプレート。未知の item では選ばない。</summary>
    private sealed class CellSelector : DataTemplateSelector
    {
        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            => (string)item switch
            {
                "switch" => new DataTemplate(() => new SwitchCell()),
                "label" => new DataTemplate(() => new LabelCell()),
                _ => null!,
            };
    }

    /// <summary>渡された container を記録するテンプレートの出し分け。</summary>
    private sealed class RecordingCellSelector : DataTemplateSelector
    {
        /// <summary>選び分けのたびに渡された container (呼ばれた順)。</summary>
        public List<BindableObject> Containers { get; } = [];

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            Containers.Add(container);
            return new DataTemplate(() => new LabelCell());
        }
    }

    /// <summary>item の文字列で Section テンプレートを出し分ける。</summary>
    private sealed class SectionSelector : DataTemplateSelector
    {
        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            => (string)item == "header"
                ? new DataTemplate(() => new Section { HeaderText = "ヘッダあり" })
                : new DataTemplate(() => new Section());
    }

    /// <summary>渡された container を記録する Section テンプレートの出し分け。</summary>
    private sealed class RecordingSectionSelector : DataTemplateSelector
    {
        /// <summary>選び分けのたびに渡された container (呼ばれた順)。</summary>
        public List<BindableObject> Containers { get; } = [];

        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        {
            Containers.Add(container);
            return new DataTemplate(() => new Section());
        }
    }

    /// <summary>Cell ではない要素を作るテンプレートを選ぶ出し分け。</summary>
    private sealed class WrongTypeSelector : DataTemplateSelector
    {
        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            => new(() => new Section());
    }

    /// <summary>Cell の出し分けとして、テンプレートではなく別の出し分けを返す。</summary>
    private sealed class NestedCellSelector : DataTemplateSelector
    {
        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            => new CellSelector();
    }

    /// <summary>Section の出し分けとして、テンプレートではなく別の出し分けを返す。</summary>
    private sealed class NestedSectionSelector : DataTemplateSelector
    {
        protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
            => new SectionSelector();
    }
}
