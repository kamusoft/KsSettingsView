using System.Linq;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// 画面全体の既定スタイルが setTheme の経路で届き、Cell 個別のスタイルが輸送内容へ載ることを
/// 確認する。
/// </summary>
[TestFixture]
public class ThemeAndCellStyleTests
{
    /// <summary>接続前に設定した既定スタイルは、接続時にまとめて適用される。</summary>
    [Test]
    public void ThemeSetBeforeConnectIsAppliedOnConnect()
    {
        SettingsView view = new()
        {
            CellAccentColor = Colors.Red,
            SeparatorColor = Colors.Blue,
        };

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.CellAccentColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
        Assert.That(theme.SeparatorColor, Is.EqualTo(unchecked((int)0xFF0000FF)));
    }

    /// <summary>表示中の既定スタイルの変更は、そのつど適用される。</summary>
    [Test]
    public void ThemeChangeWhileConnectedIsApplied()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.CellAccentColor = Colors.Green;

        KsThemeSnapshot theme = scope.Single<GatewayCall.SetTheme>().Theme;
        Assert.That(theme.CellAccentColor, Is.EqualTo(unchecked((int)0xFF008000)));
        Assert.That(scope.Gateway.Theme, Is.SameAs(theme));
    }

    /// <summary>設定画面全体の背景色は、継承した背景色のプロパティから運ばれる。</summary>
    [Test]
    public void BackgroundColorIsCarriedAsThemeBackground()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.BackgroundColor = Colors.Black;

        Assert.That(
            scope.Single<GatewayCall.SetTheme>().Theme.BackgroundColor,
            Is.EqualTo(unchecked((int)0xFF000000)));
    }

    /// <summary>フォントは family / size / attributes の組から 1 つの記述子へ合成される。</summary>
    [Test]
    public void ThemeFontIsComposedFromSplitProperties()
    {
        SettingsView view = new()
        {
            CellTitleFontFamily = "Noto",
            CellTitleFontSize = 17,
            CellTitleFontAttributes = FontAttributes.Bold | FontAttributes.Italic,
        };

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.CellTitleFont, Is.EqualTo(new KsFontSnapshot
        {
            FamilyName = "Noto",
            PointSize = 17,
            IsBold = true,
            IsItalic = true,
        }));

        // サイズは最終サイズの上書きとしても運ばれる。
        Assert.That(theme.CellTitleFontSize, Is.EqualTo(17));
    }

    /// <summary>何も指定しなければ既定スタイルは全項目が未指定になる。</summary>
    [Test]
    public void UnsetThemeCarriesNothing()
    {
        SettingsView view = new();

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.Single<GatewayCall.SetTheme>().Theme, Is.EqualTo(new KsThemeSnapshot()));
    }

    /// <summary>スタイルを何も指定していない Cell は、継承だけを意味する写しになる。</summary>
    [Test]
    public void CellWithoutStyleCarriesNoStyle()
    {
        LabelCell cell = new();

        Assert.That(cell.CreateSnapshot().Style, Is.Null);
    }

    /// <summary>Cell 個別のスタイルは写しへ載り、未指定の項目は継承のままになる。</summary>
    [Test]
    public void CellStyleIsCarriedInSnapshot()
    {
        LabelCell cell = new()
        {
            TitleColor = Colors.Red,
            IconSize = 32,
            IconRadius = 16,
            Height = 60,
            BackgroundColor = Colors.White,
        };

        KsCellStyleSnapshot style = cell.CreateSnapshot().Style!;

        Assert.That(style.TitleColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
        Assert.That(style.IconSize, Is.EqualTo(32));
        Assert.That(style.IconRadius, Is.EqualTo(16));
        Assert.That(style.CellHeight, Is.EqualTo(60));
        Assert.That(style.BackgroundColor, Is.EqualTo(unchecked((int)0xFFFFFFFF)));
        Assert.That(style.DescriptionColor, Is.Null);
        Assert.That(style.ValueTextFont, Is.Null);
    }

    /// <summary>Cell のフォントも family / size / attributes の組から合成される。</summary>
    [Test]
    public void CellFontIsComposedFromSplitProperties()
    {
        LabelCell cell = new() { ValueTextFontSize = 12, ValueTextFontAttributes = FontAttributes.Bold };

        Assert.That(cell.CreateSnapshot().Style!.ValueTextFont, Is.EqualTo(new KsFontSnapshot
        {
            FamilyName = null,
            PointSize = 12,
            IsBold = true,
            IsItalic = false,
        }));
    }

    /// <summary>スタイルの変更は内容更新として配信される。</summary>
    [Test]
    public void CellStyleChangeIsPublished()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        cell.TitleColor = Colors.Red;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>対話・選択系の Cell は強調色を自分の写しへ載せる。</summary>
    [Test]
    public void AccentColorIsCarriedByInteractiveCells()
    {
        SwitchCell switchCell = new() { AccentColor = Colors.Red };
        CheckboxCell checkbox = new() { AccentColor = Colors.Red };
        SimpleCheckCell simpleCheck = new() { AccentColor = Colors.Red };
        RadioCell radio = new() { AccentColor = Colors.Red };
        EntryCell entry = new() { AccentColor = Colors.Red };
        PickerCell picker = new() { AccentColor = Colors.Red };
        NumberPickerCell numberPicker = new() { AccentColor = Colors.Red };
        TimePickerCell timePicker = new() { AccentColor = Colors.Red };
        DatePickerCell datePicker = new() { AccentColor = Colors.Red };

        int red = unchecked((int)0xFFFF0000);
        Assert.That(((KsSwitchCellSnapshot)switchCell.CreateSnapshot()).AccentColor, Is.EqualTo(red));
        Assert.That(((KsCheckboxCellSnapshot)checkbox.CreateSnapshot()).AccentColor, Is.EqualTo(red));
        Assert.That(
            ((KsSimpleCheckCellSnapshot)simpleCheck.CreateSnapshot()).AccentColor,
            Is.EqualTo(red));
        Assert.That(((KsRadioCellSnapshot)radio.CreateSnapshot()).AccentColor, Is.EqualTo(red));
        Assert.That(((KsEntryCellSnapshot)entry.CreateSnapshot()).AccentColor, Is.EqualTo(red));
        Assert.That(((KsPickerCellSnapshot)picker.CreateSnapshot()).AccentColor, Is.EqualTo(red));
        Assert.That(
            ((KsNumberPickerCellSnapshot)numberPicker.CreateSnapshot()).AccentColor,
            Is.EqualTo(red));
        Assert.That(
            ((KsTimePickerCellSnapshot)timePicker.CreateSnapshot()).AccentColor,
            Is.EqualTo(red));
        Assert.That(
            ((KsDatePickerCellSnapshot)datePicker.CreateSnapshot()).AccentColor,
            Is.EqualTo(red));
    }

    /// <summary>強調色の変更も内容更新として配信される。</summary>
    [Test]
    public void AccentColorChangeIsPublished()
    {
        SwitchCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        cell.AccentColor = Colors.Red;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>入力欄のプレースホルダ色は写しへ載り、未指定なら継承の意思のまま運ばれる。</summary>
    [Test]
    public void PlaceholderColorIsCarriedByEntryCell()
    {
        EntryCell specified = new() { PlaceholderColor = Colors.Red };
        EntryCell unspecified = new();

        Assert.That(
            ((KsEntryCellSnapshot)specified.CreateSnapshot()).PlaceholderColor,
            Is.EqualTo(unchecked((int)0xFFFF0000)));
        Assert.That(((KsEntryCellSnapshot)unspecified.CreateSnapshot()).PlaceholderColor, Is.Null);
    }

    /// <summary>プレースホルダ色の変更も内容更新として配信される。</summary>
    [Test]
    public void PlaceholderColorChangeIsPublished()
    {
        EntryCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        cell.PlaceholderColor = Colors.Red;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));
    }

    /// <summary>既定のプレースホルダ色は Theme として運ばれ、Cell 個別の指定と混ざらない。</summary>
    [Test]
    public void CellPlaceholderColorIsCarriedByTheme()
    {
        EntryCell cell = new() { PlaceholderColor = Colors.Blue };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { CellPlaceholderColor = Colors.Red, Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.CellPlaceholderColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
        Assert.That(
            ((KsEntryCellSnapshot)cell.CreateSnapshot()).PlaceholderColor,
            Is.EqualTo(unchecked((int)0xFF0000FF)));
    }

    /// <summary>未指定の既定プレースホルダ色は Theme でも未指定のまま運ばれる。</summary>
    [Test]
    public void UnspecifiedCellPlaceholderColorIsCarriedAsUnspecified()
    {
        SettingsView view = new();

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(
            scope.All<GatewayCall.SetTheme>().Last().Theme.CellPlaceholderColor,
            Is.Null);
    }

    /// <summary>表示中の既定プレースホルダ色の変更は Theme 更新として届く。</summary>
    [Test]
    public void CellPlaceholderColorChangeWhileConnectedIsApplied()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.CellPlaceholderColor = Colors.Green;

        Assert.That(
            scope.Single<GatewayCall.SetTheme>().Theme.CellPlaceholderColor,
            Is.EqualTo(unchecked((int)0xFF008000)));
    }
}
