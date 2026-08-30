using System.Collections.Generic;
using System.Collections.Specialized;
using System.Reflection;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>Section の公開コンテナ形状と accessory テキストの公開面を確認する。</summary>
[TestFixture]
public class SectionShapeTests
{
    /// <summary>Cells の既定値は observable なコレクションになる。</summary>
    [Test]
    public void CellsDefaultsToObservableCollection()
    {
        Section section = new();

        Assert.That(section.Cells, Is.InstanceOf<INotifyCollectionChanged>());
        Assert.That(section.Cells, Is.Empty);
    }

    /// <summary>Cells の既定値はインスタンスごとに別のコレクションになる。</summary>
    [Test]
    public void CellsDefaultIsNotSharedBetweenInstances()
    {
        Section first = new();
        Section second = new();

        first.Cells.Add(new LabelCell());

        Assert.That(second.Cells, Is.Not.SameAs(first.Cells));
        Assert.That(second.Cells, Is.Empty);
    }

    /// <summary>Cells は別のコレクションへ差し替えられる。</summary>
    [Test]
    public void CellsCanBeReplacedWithAnotherCollection()
    {
        Section section = new();
        List<CellBase> replacement = [new LabelCell()];

        section.Cells = replacement;

        Assert.That(section.Cells, Is.SameAs(replacement));
    }

    /// <summary>XAML で Cell を直接並べられるよう content property は Cells になる。</summary>
    [Test]
    public void ContentPropertyIsCells()
    {
        ContentPropertyAttribute? attribute =
            typeof(Section).GetCustomAttribute<ContentPropertyAttribute>();

        Assert.That(attribute, Is.Not.Null);
        Assert.That(attribute!.Name, Is.EqualTo(nameof(Section.Cells)));
    }

    /// <summary>header / footer は対称対として設定と null クリアができる。</summary>
    [Test]
    public void HeaderAndFooterTextCanBeSetAndCleared()
    {
        Section section = new();

        Assert.That(section.HeaderText, Is.Null);
        Assert.That(section.FooterText, Is.Null);

        section.HeaderText = "header";
        section.FooterText = "footer";

        Assert.That(section.HeaderText, Is.EqualTo("header"));
        Assert.That(section.FooterText, Is.EqualTo("footer"));

        section.HeaderText = null;
        section.FooterText = null;

        Assert.That(section.HeaderText, Is.Null);
        Assert.That(section.FooterText, Is.Null);
    }

    /// <summary>ヘッダ高さは未指定から始まり、指定した値を保つ。</summary>
    [Test]
    public void HeaderHeightDefaultsToUnspecified()
    {
        Section section = new();

        Assert.That(section.HeaderHeight, Is.Null);

        section.HeaderHeight = 60;

        Assert.That(section.HeaderHeight, Is.EqualTo(60));
    }
}
