using System;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// x:Key を持たない Style (暗黙 Style) をアプリのリソース辞書に置いた状態で、SettingsView が
/// 構築でき、Style で指定した値が表示へ届くことを確認する。
/// </summary>
/// <remarks>
/// 暗黙 Style の Setter は基底コンストラクタの中で適用されるため、プロパティの変更通知は
/// SettingsView 自身のコンストラクタ本体より先に届く。通知の受け手を待たせると、構築が落ちるか、
/// 落ちなくても既定値のまま配信されて Style の指定が消える。
/// </remarks>
[TestFixture]
public class ImplicitStyleTests
{
    private Application? _application;

    /// <summary>暗黙 Style の置き場所になるアプリを用意する。</summary>
    [SetUp]
    public void SetUp() => _application = new Application();

    /// <summary>アプリを片付け、暗黙 Style が後続のテストへ残らないようにする。</summary>
    [TearDown]
    public void TearDown()
    {
        _application?.Resources.Clear();
        _application = null;
        Application.Current = null;
    }

    /// <summary>既定スタイルを指定した暗黙 Style を当てても構築できる。</summary>
    [Test]
    public void ImplicitStyleWithThemeValueDoesNotBreakConstruction()
    {
        RegisterImplicitStyle(SettingsView.CellAccentColorProperty, Colors.Red);

        Assert.That(() => new SettingsView(), Throws.Nothing);
    }

    /// <summary>暗黙 Style で指定した既定スタイルは、接続時に配信される。</summary>
    [Test]
    public void ImplicitStyleThemeValueIsDeliveredOnConnect()
    {
        RegisterImplicitStyle(SettingsView.CellAccentColorProperty, Colors.Red);

        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.CellAccentColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
    }

    /// <summary>暗黙 Style で指定した背景色も、既定スタイルの背景として配信される。</summary>
    [Test]
    public void ImplicitStyleBackgroundColorIsDeliveredOnConnect()
    {
        RegisterImplicitStyle(VisualElement.BackgroundColorProperty, Colors.Black);

        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.BackgroundColor, Is.EqualTo(unchecked((int)0xFF000000)));
    }

    /// <summary>暗黙 Style で指定した見た目スタイルも、接続時に配信される。</summary>
    [Test]
    public void ImplicitStyleListStyleIsDeliveredOnConnect()
    {
        RegisterImplicitStyle(SettingsView.ListStyleProperty, SettingsViewStyle.Modern);

        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(
            scope.All<GatewayCall.SetStyle>().Last().Style,
            Is.EqualTo(SettingsViewStyle.Modern));
    }

    /// <summary>暗黙 Style で指定した root header のテキストも、接続時に配信される。</summary>
    [Test]
    public void ImplicitStyleRootHeaderTextIsDeliveredOnConnect()
    {
        RegisterImplicitStyle(SettingsView.RootHeaderTextProperty, "header");

        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        GatewayCall.UpdateAccessory call = scope.All<GatewayCall.UpdateAccessory>()
            .Last(c => c.Target == KsAccessoryTarget.RootHeader);
        Assert.That(call.Text, Is.EqualTo("header"));
    }

    /// <summary>暗黙 Style で置いた root header の View も、取り付け後に実体として配信される。</summary>
    [Test]
    public void ImplicitStyleRootHeaderViewIsDeliveredOnAttach()
    {
        Label accessory = new();
        RegisterImplicitStyle(SettingsView.RootHeaderViewProperty, accessory);

        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        GatewayCall.UpdateAccessoryView call = scope.All<GatewayCall.UpdateAccessoryView>()
            .Last(c => c.Target == KsAccessoryTarget.RootHeader);
        Assert.That(call.View, Is.SameAs(scope.Views.LatestFor(accessory).PlatformView));
    }

    /// <summary>暗黙 Style を当てた SettingsView でも、設定ツリーはそのまま構築される。</summary>
    [Test]
    public void ImplicitStyleKeepsRootCollectionIntact()
    {
        RegisterImplicitStyle(SettingsView.CellAccentColorProperty, Colors.Red);

        Section section = new() { Cells = { new LabelCell { Title = "A" } } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(view.Controller.FindSectionId(section), Is.Not.Null);
        Assert.That(scope.Gateway.SectionIds.Count, Is.EqualTo(1));
    }

    /// <summary>
    /// 暗黙 Style で同じ View を 2 か所の root accessory へ置くと、構築時に多重配置として弾かれる。
    /// </summary>
    /// <remarks>
    /// 多重配置の検査は値が確定する前に走る。基底コンストラクタの中で Setter が適用される経路でも
    /// 検査が飛ばされないことをここで固定する。
    /// </remarks>
    [Test]
    public void ImplicitStyleWithSamePlacedViewInTwoSlotsThrows()
    {
        Label accessory = new();
        Style style = new(typeof(SettingsView));
        style.Setters.Add(new Setter
        {
            Property = SettingsView.RootHeaderViewProperty,
            Value = accessory,
        });
        style.Setters.Add(new Setter
        {
            Property = SettingsView.RootFooterViewProperty,
            Value = accessory,
        });
        _application!.Resources.Add(style);

        Assert.That(() => new SettingsView(), Throws.InstanceOf<InvalidOperationException>());
    }

    /// <summary>x:Key を持たない Style をアプリのリソース辞書へ登録する。</summary>
    /// <param name="property">Setter が設定するプロパティ</param>
    /// <param name="value">Setter が設定する値</param>
    private void RegisterImplicitStyle(BindableProperty property, object value)
    {
        Style style = new(typeof(SettingsView));
        style.Setters.Add(new Setter { Property = property, Value = value });
        _application!.Resources.Add(style);
    }
}
