using System.Collections.Generic;
using System.Collections.Specialized;
using System.Reflection;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>SettingsView の公開コンテナ形状と root accessory の公開面を確認する。</summary>
[TestFixture]
public class SettingsViewShapeTests
{
    /// <summary>Root の既定値は observable な SettingsRoot になる。</summary>
    [Test]
    public void RootDefaultsToObservableSettingsRoot()
    {
        SettingsView view = new();

        Assert.That(view.Root, Is.InstanceOf<SettingsRoot>());
        Assert.That(view.Root, Is.InstanceOf<INotifyCollectionChanged>());
        Assert.That(view.Root, Is.Empty);
    }

    /// <summary>Root の既定値はインスタンスごとに別のコレクションになる。</summary>
    [Test]
    public void RootDefaultIsNotSharedBetweenInstances()
    {
        SettingsView first = new();
        SettingsView second = new();

        first.Root.Add(new Section());

        Assert.That(second.Root, Is.Not.SameAs(first.Root));
        Assert.That(second.Root, Is.Empty);
    }

    /// <summary>Root は別のコレクションへ差し替えられる。</summary>
    [Test]
    public void RootCanBeReplacedWithAnotherCollection()
    {
        SettingsView view = new();
        List<Section> replacement = [new Section()];

        view.Root = replacement;

        Assert.That(view.Root, Is.SameAs(replacement));
    }

    /// <summary>XAML で Section を直接並べられるよう content property は Root になる。</summary>
    [Test]
    public void ContentPropertyIsRoot()
    {
        ContentPropertyAttribute? attribute =
            typeof(SettingsView).GetCustomAttribute<ContentPropertyAttribute>();

        Assert.That(attribute, Is.Not.Null);
        Assert.That(attribute!.Name, Is.EqualTo(nameof(SettingsView.Root)));
    }

    /// <summary>root の header / footer は既定で未設定になる。</summary>
    [Test]
    public void RootAccessoryTextDefaultsToNull()
    {
        SettingsView view = new();

        Assert.That(view.RootHeaderText, Is.Null);
        Assert.That(view.RootFooterText, Is.Null);
    }

    /// <summary>root の header / footer は設定と null クリアができる。</summary>
    [Test]
    public void RootAccessoryTextCanBeSetAndCleared()
    {
        SettingsView view = new()
        {
            RootHeaderText = "header",
            RootFooterText = "footer",
        };

        Assert.That(view.RootHeaderText, Is.EqualTo("header"));
        Assert.That(view.RootFooterText, Is.EqualTo("footer"));

        view.RootHeaderText = null;
        view.RootFooterText = null;

        Assert.That(view.RootHeaderText, Is.Null);
        Assert.That(view.RootFooterText, Is.Null);
    }
}
