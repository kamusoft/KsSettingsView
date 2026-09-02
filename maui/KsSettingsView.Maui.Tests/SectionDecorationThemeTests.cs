using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui;
using Microsoft.Maui.Controls.Xaml;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Section 装飾の 4 属性が既定スタイルの写しへ載り、値の検証を挟まず素通しされることを確認する。
/// </summary>
[TestFixture]
public class SectionDecorationThemeTests
{
    /// <summary>何も指定しなければ、装飾の項目はすべて未指定のまま運ばれる。</summary>
    [Test]
    public void UnsetSectionDecorationCarriesNothing()
    {
        SettingsView view = new();

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginTop, Is.Null);
        Assert.That(theme.SectionMarginLeading, Is.Null);
        Assert.That(theme.SectionMarginBottom, Is.Null);
        Assert.That(theme.SectionMarginTrailing, Is.Null);
        Assert.That(theme.SectionCornerRadius, Is.Null);
        Assert.That(theme.SectionBorderWidth, Is.Null);
        Assert.That(theme.SectionBorderColor, Is.Null);
    }

    /// <summary>指定した 4 属性は既定スタイルの一部として運ばれる。</summary>
    [Test]
    public void SectionDecorationIsCarriedAsTheme()
    {
        SettingsView view = new()
        {
            SectionMargin = new Thickness(16, 12, 8, 4),
            SectionCornerRadius = 12,
            SectionBorderWidth = 2,
            SectionBorderColor = Colors.Red,
        };

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginTop, Is.EqualTo(12));
        Assert.That(theme.SectionMarginLeading, Is.EqualTo(16));
        Assert.That(theme.SectionMarginBottom, Is.EqualTo(4));
        Assert.That(theme.SectionMarginTrailing, Is.EqualTo(8));
        Assert.That(theme.SectionCornerRadius, Is.EqualTo(12));
        Assert.That(theme.SectionBorderWidth, Is.EqualTo(2));
        Assert.That(theme.SectionBorderColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
    }

    /// <summary>表示中の装飾の変更は、そのつど既定スタイルの更新として配信される。</summary>
    [Test]
    public void SectionDecorationChangeWhileConnectedIsApplied()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.SectionCornerRadius = 20;

        Assert.That(scope.Single<GatewayCall.SetTheme>().Theme.SectionCornerRadius, Is.EqualTo(20));
    }

    /// <summary>余白は 4 成分をひとまとまりで運び、未指定なら成分も全部未指定になる。</summary>
    [Test]
    public void SectionMarginIsCarriedAllOrNone()
    {
        SettingsView view = new() { SectionMargin = new Thickness(1, 2, 3, 4) };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.SectionMargin = null;

        KsThemeSnapshot theme = scope.Single<GatewayCall.SetTheme>().Theme;
        Assert.That(theme.SectionMarginTop, Is.Null);
        Assert.That(theme.SectionMarginLeading, Is.Null);
        Assert.That(theme.SectionMarginBottom, Is.Null);
        Assert.That(theme.SectionMarginTrailing, Is.Null);
    }

    /// <summary>余白の左右は論理方向 (leading / trailing) として運ばれる。</summary>
    [Test]
    public void SectionMarginHorizontalComponentsAreCarriedAsLogicalDirections()
    {
        SettingsView view = new() { SectionMargin = new Thickness(24, 0, 6, 0) };

        GatewayScope scope = GatewayScope.Connect(view);

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginLeading, Is.EqualTo(24));
        Assert.That(theme.SectionMarginTrailing, Is.EqualTo(6));
    }

    /// <summary>Classic でも余白の左右成分はそのまま運ばれる (無視するのは Native の適用側)。</summary>
    [Test]
    public void SectionMarginHorizontalComponentsAreCarriedUnderClassic()
    {
        SettingsView view = new() { SectionMargin = new Thickness(24, 10, 6, 10) };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(view.ListStyle, Is.EqualTo(SettingsViewStyle.Classic));
        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginLeading, Is.EqualTo(24));
        Assert.That(theme.SectionMarginTrailing, Is.EqualTo(6));
    }

    /// <summary>範囲外の値も検証されず、そのまま運ばれる (正規化は Native の描画時)。</summary>
    [Test]
    public void OutOfRangeSectionDecorationIsCarriedAsIs()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Assert.DoesNotThrow(() =>
        {
            view.SectionMargin = new Thickness(-8, -4, -2, -1);
            view.SectionCornerRadius = 100000;
            view.SectionBorderWidth = -3;
        });

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginLeading, Is.EqualTo(-8));
        Assert.That(theme.SectionMarginTop, Is.EqualTo(-4));
        Assert.That(theme.SectionMarginTrailing, Is.EqualTo(-2));
        Assert.That(theme.SectionMarginBottom, Is.EqualTo(-1));
        Assert.That(theme.SectionCornerRadius, Is.EqualTo(100000));
        Assert.That(theme.SectionBorderWidth, Is.EqualTo(-3));
    }

    /// <summary>非有限の値も検証されず、そのまま運ばれる (0 への正規化は Native の描画時)。</summary>
    [Test]
    public void NonFiniteSectionDecorationIsCarriedAsIs()
    {
        SettingsView view = new();
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Assert.DoesNotThrow(() =>
        {
            view.SectionMargin = new Thickness(
                double.NaN,
                double.PositiveInfinity,
                double.NegativeInfinity,
                double.NaN);
            view.SectionCornerRadius = double.NaN;
            view.SectionBorderWidth = double.PositiveInfinity;
        });

        KsThemeSnapshot theme = scope.All<GatewayCall.SetTheme>().Last().Theme;
        Assert.That(theme.SectionMarginLeading, Is.NaN);
        Assert.That(theme.SectionMarginTop, Is.EqualTo(double.PositiveInfinity));
        Assert.That(theme.SectionMarginTrailing, Is.EqualTo(double.NegativeInfinity));
        Assert.That(theme.SectionMarginBottom, Is.NaN);
        Assert.That(theme.SectionCornerRadius, Is.NaN);
        Assert.That(theme.SectionBorderWidth, Is.EqualTo(double.PositiveInfinity));
    }

    /// <summary>XAML の属性記法で書いた余白は、C# で Thickness を代入したときと同じ値になる。</summary>
    [Test]
    public void SectionMarginAttributeTextMatchesThicknessAssignment()
    {
        const string xaml = """
            <ks:SettingsView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
                             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
                             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
                             SectionMargin="16,22,16,0" />
            """;

        SettingsView view = new SettingsView().LoadFromXaml(xaml);

        Assert.That(view.SectionMargin, Is.EqualTo(new Thickness(16, 22, 16, 0)));
    }
}
