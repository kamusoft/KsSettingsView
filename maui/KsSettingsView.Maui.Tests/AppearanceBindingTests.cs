using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Section / Cell に設定した binding が、外観の変更と Resources の差し替えで再評価され、
/// 通常のプロパティ変更と同じ経路で表示へ届くことを確認する。
/// </summary>
/// <remarks>
/// 再評価が成り立つのは Section / Cell が論理子であり、祖先 (ページ / アプリ) まで
/// 辿れるため。所属を解いた要素は旧所属先の Resources に追随しない。
/// 端末の外観の変更もアプリの外観の変更と同じ通知を経るため、ここでは区別しない。
/// </remarks>
[TestFixture]
public class AppearanceBindingTests
{
    private const string ColorKey = "KsTestColor";

    private Application? _application;

    /// <summary>外観と Resources の持ち主になるアプリを用意する。</summary>
    [SetUp]
    public void SetUp()
    {
        _application = new Application();
        _application.UserAppTheme = AppTheme.Light;
    }

    /// <summary>アプリを片付け、外観と Resources が後続のテストへ残らないようにする。</summary>
    [TearDown]
    public void TearDown()
    {
        _application?.Resources.Clear();
        _application = null;
        Application.Current = null;
    }

    // ---- binding の再評価 ----

    /// <summary>Cell の色の AppThemeBinding は外観の変更で再評価される。</summary>
    [Test]
    public void CellColorFollowsTheAppearanceChange()
    {
        ButtonCell cell = new();
        Place(cell);
        cell.SetAppThemeColor(CellBase.TitleColorProperty, Colors.Black, Colors.White);

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Black));

        _application!.UserAppTheme = AppTheme.Dark;

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.White));
    }

    /// <summary>Section のプロパティの AppThemeBinding も外観の変更で再評価される。</summary>
    [Test]
    public void SectionPropertyFollowsTheAppearanceChange()
    {
        Section section = new();
        PlaceSection(section);
        section.SetAppTheme(Section.HeaderTextProperty, "昼", "夜");

        Assert.That(section.HeaderText, Is.EqualTo("昼"));

        _application!.UserAppTheme = AppTheme.Dark;

        Assert.That(section.HeaderText, Is.EqualTo("夜"));
    }

    /// <summary>Cell の DynamicResource はページ Resources の差し替えで再評価される。</summary>
    [Test]
    public void CellColorFollowsThePageResourceChange()
    {
        LabelCell cell = new();
        ContentPage page = Place(cell);
        page.Resources[ColorKey] = Colors.Black;
        cell.SetDynamicResource(CellBase.TitleColorProperty, ColorKey);

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Black));

        page.Resources[ColorKey] = Colors.Red;

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Red));
    }

    /// <summary>ページ Resources に無いキーは、アプリ Resources の差し替えで再評価される。</summary>
    [Test]
    public void CellColorFollowsTheApplicationResourceChange()
    {
        LabelCell cell = new();
        Place(cell);
        _application!.Resources[ColorKey] = Colors.Black;
        cell.SetDynamicResource(CellBase.TitleColorProperty, ColorKey);

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Black));

        _application.Resources[ColorKey] = Colors.Red;

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Red));
    }

    /// <summary>所属を解いた Cell は、旧所属先の Resources の差し替えに追随しない。</summary>
    [Test]
    public void DetachedCellStopsFollowingTheOldPageResources()
    {
        LabelCell cell = new();
        ContentPage page = Place(cell);
        page.Resources[ColorKey] = Colors.Black;
        cell.SetDynamicResource(CellBase.TitleColorProperty, ColorKey);

        Section section = (Section)cell.Parent;
        section.Cells.Remove(cell);

        page.Resources[ColorKey] = Colors.Red;

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Black));
    }

    /// <summary>素の List へ接続前に足した Cell も、接続の後は外観の変更で再評価される。</summary>
    /// <remarks>
    /// 素の List は増減を通知しないため、所属の確定は表示へ変換する時点になる。そこから先は
    /// observable なコレクションへ置いた要素と同じに振る舞う。
    /// </remarks>
    [Test]
    public void CellAddedToPlainCellsListBeforeConnectFollowsTheAppearanceChange()
    {
        List<CellBase> cells = [];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        ContentPage page = new() { Content = view };
        _ = new Window(page) { Parent = _application };

        ButtonCell cell = new();
        cells.Add(cell);
        cell.SetAppThemeColor(CellBase.TitleColorProperty, Colors.Black, Colors.White);

        _ = GatewayScope.Connect(view);

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.Black));

        _application!.UserAppTheme = AppTheme.Dark;

        Assert.That(cell.TitleColor, Is.EqualTo(Colors.White));
    }

    // ---- 再評価の反映経路 ----

    /// <summary>Cell の色の再評価は、行を作り直さない内容更新として配信される。</summary>
    [Test]
    public void ReevaluatedCellColorIsDeliveredAsAContentUpdate()
    {
        ButtonCell cell = new();
        ContentPage page = Place(cell);
        cell.SetAppThemeColor(CellBase.TitleColorProperty, Colors.Black, Colors.White);

        SettingsView view = (SettingsView)page.Content;
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        _application!.UserAppTheme = AppTheme.Dark;
        scope.Flush();

        // 単発でもバッチでも、再評価後の色が載った内容更新が届いていること。
        List<KsCellSnapshot> delivered =
        [
            .. scope.All<GatewayCall.ReplaceCell>().Select(call => call.Snapshot),
            .. scope.All<GatewayCall.ReplaceCells>().SelectMany(call => call.Updates)
                .Select(update => update.Snapshot),
        ];

        Assert.That(delivered, Is.Not.Empty);
        Assert.That(
            delivered[^1].Style!.TitleColor,
            Is.EqualTo(KsWireValues.Color(Colors.White)));
        Assert.That(scope.All<GatewayCall.RemoveCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.InsertCell>(), Is.Empty);
    }

    /// <summary>Section の HeaderText の再評価は accessory の更新として配信される。</summary>
    [Test]
    public void ReevaluatedSectionHeaderTextIsDeliveredAsAnAccessoryUpdate()
    {
        Section section = new();
        ContentPage page = PlaceSection(section);
        section.SetAppTheme(Section.HeaderTextProperty, "昼", "夜");

        SettingsView view = (SettingsView)page.Content;
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        _application!.UserAppTheme = AppTheme.Dark;
        scope.Flush();

        GatewayCall.UpdateAccessory update = scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(update.Text, Is.EqualTo("夜"));
    }

    /// <summary>この Cell を、アプリまで辿れるページに載せた SettingsView の中へ置く。</summary>
    /// <param name="cell">置く Cell</param>
    private ContentPage Place(CellBase cell)
    {
        Section section = new() { Cells = { cell } };
        return PlaceSection(section);
    }

    /// <summary>この Section を、アプリまで辿れるページに載せた SettingsView の中へ置く。</summary>
    /// <remarks>
    /// 外観と Resources の解決は、要素から Window を経てアプリまで辿れることを前提にする。
    /// platform の口を持たないテストでは窓を開けないため、窓の親を直に結んで同じ経路を作る。
    /// </remarks>
    /// <param name="section">置く Section</param>
    private ContentPage PlaceSection(Section section)
    {
        SettingsView view = new() { Root = { section } };
        ContentPage page = new() { Content = view };
        _ = new Window(page) { Parent = _application };
        return page;
    }
}
