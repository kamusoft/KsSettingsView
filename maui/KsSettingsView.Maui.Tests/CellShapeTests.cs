using System;
using System.Collections.Generic;
using System.Reflection;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Support;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>Cell 種別ごとの公開プロパティの形 (名前・型・既定値・binding mode) を確認する。</summary>
[TestFixture]
public class CellShapeTests
{
    /// <summary>ユーザー操作で値が変わるプロパティと、それを持つ Cell の組。</summary>
    private static readonly (Type CellType, BindableProperty Property)[] TwoWayProperties =
    [
        (typeof(SwitchCell), SwitchCell.OnProperty),
        (typeof(CheckboxCell), CheckboxCell.CheckedProperty),
        (typeof(SimpleCheckCell), SimpleCheckCell.CheckedProperty),
        (typeof(RadioCell), RadioCell.SelectedValueProperty),
        (typeof(EntryCell), EntryCell.ValueTextProperty),
        (typeof(PickerCell), PickerCell.SelectedIndexProperty),
        (typeof(PickerCell), PickerCell.SelectedIndicesProperty),
        (typeof(PickerCell), PickerCell.SelectedItemProperty),
        (typeof(PickerCell), PickerCell.SelectedItemsProperty),
        (typeof(NumberPickerCell), NumberPickerCell.NumberProperty),
        (typeof(TimePickerCell), TimePickerCell.TimeProperty),
        (typeof(DatePickerCell), DatePickerCell.DateProperty),
    ];

    /// <summary>Cell は有効かつ可視の状態から始まる。</summary>
    [Test]
    public void CellDefaultsToEnabledAndVisible()
    {
        LabelCell cell = new();

        Assert.That(cell.Title, Is.Empty);
        Assert.That(cell.Description, Is.Null);
        Assert.That(cell.HintText, Is.Null);
        Assert.That(cell.ValueText, Is.Null);
        Assert.That(cell.IsEnabled, Is.True);
        Assert.That(cell.IsVisible, Is.True);
    }

    /// <summary>基本 Cell の固有プロパティは既定値から始まり、設定した値を保つ。</summary>
    [Test]
    public void BasicCellsExposeTheirOwnState()
    {
        CommandCell command = new();
        Assert.That(command.HideArrow, Is.False);
        Assert.That(command.ValueText, Is.Null);
        Assert.That(command.Command, Is.Null);
        Assert.That(command.CommandParameter, Is.Null);
        command.HideArrow = true;
        Assert.That(command.HideArrow, Is.True);

        ButtonCell button = new();
        Assert.That(button.TitleAlignment, Is.Null);
        button.TitleAlignment = TextAlignment.Start;
        Assert.That(button.TitleAlignment, Is.EqualTo(TextAlignment.Start));

        SwitchCell switchCell = new();
        Assert.That(switchCell.On, Is.False);
        switchCell.On = true;
        Assert.That(switchCell.On, Is.True);

        CheckboxCell checkbox = new();
        Assert.That(checkbox.Checked, Is.False);
        checkbox.Checked = true;
        Assert.That(checkbox.Checked, Is.True);

        SimpleCheckCell simpleCheck = new();
        Assert.That(simpleCheck.Checked, Is.False);
        simpleCheck.Checked = true;
        Assert.That(simpleCheck.Checked, Is.True);

        RadioCell radio = new();
        Assert.That(radio.GroupId, Is.Empty);
        Assert.That(radio.Value, Is.Empty);
        Assert.That(radio.SelectedValue, Is.Empty);
        radio.GroupId = "theme";
        radio.Value = "dark";
        radio.SelectedValue = "dark";
        Assert.That(radio.GroupId, Is.EqualTo("theme"));
        Assert.That(radio.Value, Is.EqualTo("dark"));
        Assert.That(radio.SelectedValue, Is.EqualTo("dark"));
    }

    /// <summary>
    /// 値文字列を表示できる基本 Cell 5 種は、表示専用の値文字列を公開する。
    /// </summary>
    /// <remarks>
    /// 入力値を持つ EntryCell の値文字列とは別物で、こちらは表示だけに使う片方向のプロパティ。
    /// </remarks>
    [Test]
    public void BasicCellsExposeValueText()
    {
        (CellBase Cell, BindableProperty Property, Func<CellBase, string?> Read)[] cells =
        [
            (new ButtonCell(), ButtonCell.ValueTextProperty, cell => ((ButtonCell)cell).ValueText),
            (new SwitchCell(), SwitchCell.ValueTextProperty, cell => ((SwitchCell)cell).ValueText),
            (new CheckboxCell(), CheckboxCell.ValueTextProperty, cell => ((CheckboxCell)cell).ValueText),
            (new SimpleCheckCell(), SimpleCheckCell.ValueTextProperty, cell => ((SimpleCheckCell)cell).ValueText),
            (new RadioCell(), RadioCell.ValueTextProperty, cell => ((RadioCell)cell).ValueText),
        ];

        foreach ((CellBase cell, BindableProperty property, Func<CellBase, string?> read) in cells)
        {
            string label = $"{cell.GetType().Name}.{property.PropertyName}";
            Assert.That(read(cell), Is.Null, label);
            Assert.That(property.DefaultBindingMode, Is.EqualTo(BindingMode.OneWay), label);

            cell.SetValue(property, "値");
            Assert.That(read(cell), Is.EqualTo("値"), label);
        }
    }

    /// <summary>値文字列の変更は輸送内容へ影響する変更として扱われる。</summary>
    [Test]
    public void ValueTextChangeAffectsSnapshotOfBasicCells()
    {
        CellBase[] cells =
        [
            new ButtonCell(),
            new SwitchCell(),
            new CheckboxCell(),
            new SimpleCheckCell(),
            new RadioCell(),
        ];

        foreach (CellBase cell in cells)
        {
            Assert.That(
                cell.AffectsSnapshot(nameof(LabelCell.ValueText)),
                Is.True,
                cell.GetType().Name);
        }
    }

    /// <summary>入力 Cell の固有プロパティは native と同じ意味論の既定値から始まる。</summary>
    [Test]
    public void InputCellsExposeTheirOwnState()
    {
        EntryCell entry = new();
        Assert.That(entry.ValueText, Is.Empty);
        Assert.That(entry.Placeholder, Is.Null);
        Assert.That(entry.Keyboard, Is.Null);
        Assert.That(entry.IsPassword, Is.False);
        Assert.That(entry.TextAlignment, Is.Null);
        Assert.That(entry.MaxLength, Is.Null);

        PickerCell picker = new();
        Assert.That(picker.ItemsSource, Is.Null);
        Assert.That(picker.SelectionMode, Is.EqualTo(PickerSelectionMode.Single));
        Assert.That(picker.SelectedIndex, Is.Null);
        Assert.That(picker.SelectedIndices, Is.Null);
        Assert.That(picker.SelectedItem, Is.Null);
        Assert.That(picker.SelectedItems, Is.Null);
        Assert.That(picker.MaxSelectedNumber, Is.Zero);
        Assert.That(picker.PageTitle, Is.Null);
        Assert.That(picker.ValueText, Is.Null);
        Assert.That(picker.DisplayMember, Is.Null);
        Assert.That(picker.SubDisplayMember, Is.Null);

        NumberPickerCell number = new();
        Assert.That(number.Min, Is.Zero);
        Assert.That(number.Max, Is.EqualTo(100));
        Assert.That(number.Step, Is.EqualTo(1));
        Assert.That(number.Number, Is.Zero);
        Assert.That(number.Unit, Is.Empty);
        Assert.That(number.PickerTitle, Is.Null);

        TimePickerCell time = new();
        Assert.That(time.Time, Is.EqualTo(TimeSpan.Zero));
        Assert.That(time.Format, Is.Null);
        Assert.That(time.Is24Hour, Is.True);
        Assert.That(time.PickerTitle, Is.Null);

        DatePickerCell date = new();
        Assert.That(date.Date, Is.EqualTo(new DateTime(1970, 1, 1)));
        Assert.That(date.MinimumDate, Is.Null);
        Assert.That(date.MaximumDate, Is.Null);
        Assert.That(date.Format, Is.Null);
        Assert.That(date.TodayText, Is.Null);
        Assert.That(date.PickerTitle, Is.Null);
        Assert.That(date.UIStyle, Is.Null);
        Assert.That(date.AndroidButtonColor, Is.Null);
    }

    /// <summary>ユーザー操作で値が変わる 12 プロパティは TwoWay を既定にする。</summary>
    [Test]
    public void UserEditablePropertiesDefaultToTwoWayBinding()
    {
        Assert.That(TwoWayProperties, Has.Length.EqualTo(12));

        foreach ((Type cellType, BindableProperty property) in TwoWayProperties)
        {
            Assert.That(
                property.DefaultBindingMode,
                Is.EqualTo(BindingMode.TwoWay),
                $"{cellType.Name}.{property.PropertyName}");
        }
    }

    /// <summary>それ以外のプロパティは OneWay を既定にする。</summary>
    [Test]
    public void OtherPropertiesDefaultToOneWayBinding()
    {
        BindableProperty[] oneWay =
        [
            CellBase.TitleProperty,
            CellBase.IsEnabledProperty,
            CellBase.IsVisibleProperty,
            CommandCell.HideArrowProperty,
            ButtonCell.TitleAlignmentProperty,
            RadioCell.ValueProperty,
            EntryCell.KeyboardProperty,
            PickerCell.ItemsSourceProperty,
            NumberPickerCell.MinProperty,
            DatePickerCell.UIStyleProperty,
        ];

        foreach (BindableProperty property in oneWay)
        {
            Assert.That(
                property.DefaultBindingMode,
                Is.EqualTo(BindingMode.OneWay),
                property.PropertyName);
        }
    }

    /// <summary>ButtonCell は基底の説明文を自型から公開しない。</summary>
    [Test]
    public void ButtonCellHidesDescriptionFromItsOwnSurface()
    {
        PropertyInfo? hidden = typeof(ButtonCell).GetProperty(
            nameof(CellBase.Description),
            BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.DeclaredOnly);

        Assert.That(hidden, Is.Not.Null, "ButtonCell は基底の説明文を隠す宣言を持つ");
        Assert.That(hidden!.GetMethod!.IsPublic, Is.False);
    }

    /// <summary>ButtonCell は基底経由で説明文を設定されても輸送しない。</summary>
    [Test]
    public void ButtonCellDoesNotCarryDescriptionSetThroughBase()
    {
        CellBase cell = new ButtonCell { Title = "button" };
        cell.Description = "ignored";

        KsButtonCellSnapshot snapshot = (KsButtonCellSnapshot)cell.CreateSnapshot();

        Assert.That(snapshot.Title, Is.EqualTo("button"));
        Assert.That(snapshot.Description, Is.Null);
    }

    /// <summary>公開プロパティの値がそのまま輸送内容へ写る。</summary>
    [Test]
    public void SnapshotCarriesPublicProperties()
    {
        LabelCell cell = new()
        {
            Title = "title",
            Description = "description",
            HintText = "hint",
            ValueText = "value",
            IsEnabled = false,
            IsVisible = false,
        };

        KsLabelCellSnapshot snapshot = (KsLabelCellSnapshot)cell.CreateSnapshot();

        Assert.That(snapshot.Title, Is.EqualTo("title"));
        Assert.That(snapshot.Description, Is.EqualTo("description"));
        Assert.That(snapshot.HintText, Is.EqualTo("hint"));
        Assert.That(snapshot.ValueText, Is.EqualTo("value"));
        Assert.That(snapshot.IsEnabled, Is.False);
        Assert.That(snapshot.IsVisible, Is.False);
    }

    /// <summary>輸送側が非 null を要求する Title は null 指定でも空文字へ解決する。</summary>
    [Test]
    public void SnapshotResolvesNullTitleToEmpty()
    {
        LabelCell cell = new() { Title = null! };

        Assert.That(cell.CreateSnapshot().Title, Is.Empty);
    }

    /// <summary>どの種別にも当てはまらない Cell は、共通項目だけを持つ表示行として扱われる。</summary>
    [Test]
    public void SnapshotOfUnknownCellKindFallsBackToLabel()
    {
        StubCell cell = new() { Title = "title", ValueText = "ignored" };

        KsCellSnapshot snapshot = cell.CreateSnapshot();

        Assert.That(snapshot, Is.InstanceOf<KsLabelCellSnapshot>());
        Assert.That(snapshot.Title, Is.EqualTo("title"));
        Assert.That(((KsLabelCellSnapshot)snapshot).ValueText, Is.Null);
    }

    /// <summary>色は ARGB を詰めた 32bit 整数として輸送される。</summary>
    [Test]
    public void ColorIsCarriedAsPackedArgb()
    {
        DatePickerCell cell = new() { AndroidButtonColor = Color.FromRgba(0x12, 0x34, 0x56, 0x78) };

        KsDatePickerCellSnapshot snapshot = (KsDatePickerCellSnapshot)cell.CreateSnapshot();

        Assert.That(snapshot.AndroidButtonColor, Is.EqualTo(unchecked((int)0x78123456)));
    }

    /// <summary>Picker の項目は差し替えで反映される。</summary>
    [Test]
    public void PickerItemsAreCarriedFromItemsSource()
    {
        List<string> items = ["ライト", "ダーク"];
        PickerCell cell = new() { ItemsSource = items };

        KsPickerCellSnapshot snapshot = (KsPickerCellSnapshot)cell.CreateSnapshot();

        Assert.That(snapshot.Items.Select(static item => item.Text), Is.EqualTo(items));
    }

    /// <summary>DisplayMember / SubDisplayMember を設定すると、射影後の項目が輸送される。</summary>
    [Test]
    public void PickerItemsAreProjectedByDisplayMembers()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Plan("松", "全部入り"), new PickerFixtures.Plan("竹", "標準") },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
            SubDisplayMember = nameof(PickerFixtures.Plan.Note),
        };

        KsPickerCellSnapshot snapshot = (KsPickerCellSnapshot)cell.CreateSnapshot();

        Assert.That(snapshot.Items.Select(static item => item.Text), Is.EqualTo(new[] { "松", "竹" }));
        Assert.That(snapshot.Items.Select(static item => item.SubText), Is.EqualTo(new[] { "全部入り", "標準" }));
        Assert.That(cell.SelectedItem, Is.Null);
    }

    /// <summary>スタイル系のプロパティは未指定から始まり、設定した値を保つ。</summary>
    [Test]
    public void CellStylePropertiesDefaultToUnspecified()
    {
        LabelCell cell = new();

        Assert.That(cell.IconSource, Is.Null);
        Assert.That(cell.TitleColor, Is.Null);
        Assert.That(cell.TitleFontFamily, Is.Null);
        Assert.That(cell.TitleFontSize, Is.Null);
        Assert.That(cell.TitleFontAttributes, Is.Null);
        Assert.That(cell.DescriptionColor, Is.Null);
        Assert.That(cell.ValueTextColor, Is.Null);
        Assert.That(cell.HintTextColor, Is.Null);
        Assert.That(cell.BackgroundColor, Is.Null);
        Assert.That(cell.IconSize, Is.Null);
        Assert.That(cell.IconRadius, Is.Null);
        Assert.That(cell.Height, Is.Null);

        cell.IconSize = 24;
        cell.TitleFontAttributes = FontAttributes.Bold;
        Assert.That(cell.IconSize, Is.EqualTo(24));
        Assert.That(cell.TitleFontAttributes, Is.EqualTo(FontAttributes.Bold));
    }

    /// <summary>対話・選択系の Cell だけが強調色を公開し、未指定から始まる。</summary>
    [Test]
    public void AccentColorIsExposedByInteractiveCellsOnly()
    {
        Assert.That(new SwitchCell().AccentColor, Is.Null);
        Assert.That(new CheckboxCell().AccentColor, Is.Null);
        Assert.That(new SimpleCheckCell().AccentColor, Is.Null);
        Assert.That(new RadioCell().AccentColor, Is.Null);
        Assert.That(new EntryCell().AccentColor, Is.Null);
        Assert.That(new PickerCell().AccentColor, Is.Null);
        Assert.That(new NumberPickerCell().AccentColor, Is.Null);
        Assert.That(new TimePickerCell().AccentColor, Is.Null);
        Assert.That(new DatePickerCell().AccentColor, Is.Null);

        Assert.That(typeof(LabelCell).GetProperty("AccentColor"), Is.Null);
        Assert.That(typeof(CommandCell).GetProperty("AccentColor"), Is.Null);
        Assert.That(typeof(ButtonCell).GetProperty("AccentColor"), Is.Null);
    }

    /// <summary>プレースホルダ色は入力欄の Cell だけが公開し、未指定から始まる。</summary>
    /// <remarks>
    /// 単一 Cell のスタイル段には置かない。入力欄の Cell が持つ個別指定と役割が重なるため。
    /// </remarks>
    [Test]
    public void PlaceholderColorIsExposedByEntryCellOnly()
    {
        Assert.That(new EntryCell().PlaceholderColor, Is.Null);

        Assert.That(typeof(LabelCell).GetProperty("PlaceholderColor"), Is.Null);
        Assert.That(typeof(PickerCell).GetProperty("PlaceholderColor"), Is.Null);
        Assert.That(typeof(CellBase).GetProperty("PlaceholderColor"), Is.Null);
        Assert.That(typeof(KsCellStyleSnapshot).GetProperty("PlaceholderColor"), Is.Null);
    }

    /// <summary>Section は表示状態から始まる。</summary>
    [Test]
    public void SectionDefaultsToVisible() => Assert.That(new Section().IsVisible, Is.True);

    /// <summary>既定スタイル系のプロパティは未指定から始まる。</summary>
    [Test]
    public void ThemePropertiesDefaultToUnspecified()
    {
        SettingsView view = new();

        Assert.That(view.SeparatorColor, Is.Null);
        Assert.That(view.SelectedColor, Is.Null);
        Assert.That(view.CellBackgroundColor, Is.Null);
        Assert.That(view.CellAccentColor, Is.Null);
        Assert.That(view.DisabledTextColor, Is.Null);
        Assert.That(view.ScrollIndicatorVisible, Is.Null);
        Assert.That(view.RowHeight, Is.Null);
        Assert.That(view.HasUnevenRows, Is.Null);
        Assert.That(view.HeaderTextColor, Is.Null);
        Assert.That(view.HeaderHeight, Is.Null);
        Assert.That(view.FooterTextColor, Is.Null);
        Assert.That(view.CellTitleColor, Is.Null);
        Assert.That(view.CellValueTextColor, Is.Null);
        Assert.That(view.CellDescriptionColor, Is.Null);
        Assert.That(view.CellHintTextColor, Is.Null);
        Assert.That(view.CellPlaceholderColor, Is.Null);
        Assert.That(view.CellIconSize, Is.Null);
        Assert.That(view.CellIconRadius, Is.Null);
    }

    /// <summary>どの種別にも当てはまらない Cell。</summary>
    private sealed class StubCell : CellBase
    {
        /// <summary>基底が写し取らないことを確かめるための、輸送対象外の値。</summary>
        public string? ValueText { get; set; }
    }
}
