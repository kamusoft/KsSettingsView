using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Support;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>PickerCell の object 候補と DisplayMember / SubDisplayMember による表示射影を確認する。</summary>
[TestFixture]
public class PickerItemProjectionTests
{
    /// <summary>DisplayMember に指定したプロパティの値が主表示になる。</summary>
    [Test]
    public void DisplayMemberProjectsMainText()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Plan("松"),
                new PickerFixtures.Plan("竹"),
            },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
        };

        Assert.That(Texts(cell), Is.EqualTo(new[] { "松", "竹" }));
    }

    /// <summary>SubDisplayMember に指定したプロパティの値が副表示になる。</summary>
    [Test]
    public void SubDisplayMemberProjectsSubText()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Plan("松", "全部入り"),
                new PickerFixtures.Plan("竹", "標準"),
            },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
            SubDisplayMember = nameof(PickerFixtures.Plan.Note),
        };

        Assert.That(SubTexts(cell), Is.EqualTo(new[] { "全部入り", "標準" }));
    }

    /// <summary>射影を指定しないときは要素の ToString() が主表示になり、副表示は付かない。</summary>
    [Test]
    public void UnspecifiedMembersFallBackToToString()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Opaque("松"),
                new PickerFixtures.Opaque("竹"),
            },
        };

        Assert.That(Texts(cell), Is.EqualTo(new[] { "松", "竹" }));
        Assert.That(SubTexts(cell), Is.EqualTo(new string?[] { null, null }));
    }

    /// <summary>要素の型に無いプロパティ名は例外にならず ToString() へ落ちる。</summary>
    [Test]
    public void UnresolvableMemberFallsBackToToString()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Opaque("松") },
            DisplayMember = "NotThere",
            SubDisplayMember = "NotThereEither",
        };

        Assert.That(Texts(cell), Is.EqualTo(new[] { "松" }));
        Assert.That(SubTexts(cell), Is.EqualTo(new string?[] { null }));
    }

    /// <summary>非 public なプロパティと静的プロパティは射影対象にならない。</summary>
    [Test]
    public void NonPublicAndStaticMembersAreNotResolved()
    {
        PickerCell hidden = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Awkward { Count = 3 } },
            DisplayMember = "Hidden",
        };
        PickerCell shared = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Awkward { Count = 3 } },
            DisplayMember = nameof(PickerFixtures.Awkward.Shared),
        };

        Assert.That(Texts(hidden), Is.EqualTo(new[] { "awkward:3/hidden" }));
        Assert.That(Texts(shared), Is.EqualTo(new[] { "awkward:3/hidden" }));
    }

    /// <summary>string 以外のプロパティ値は ToString() され、null の値は空文字列になる。</summary>
    [Test]
    public void NonStringAndNullMemberValuesAreNormalized()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Awkward { Count = 7 },
                new PickerFixtures.Awkward { Count = 0, Missing = null },
            },
            DisplayMember = nameof(PickerFixtures.Awkward.Count),
        };
        PickerCell nullValued = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Awkward { Missing = null } },
            DisplayMember = nameof(PickerFixtures.Awkward.Missing),
        };

        Assert.That(Texts(cell), Is.EqualTo(new[] { "7", "0" }));
        Assert.That(Texts(nullValued), Is.EqualTo(new[] { string.Empty }));
    }

    /// <summary>副表示の値が null・空文字列のときは副表示なしに揃う。</summary>
    [Test]
    public void EmptyOrNullSubTextBecomesNoSubText()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Plan("松", "全部入り"),
                new PickerFixtures.Plan("竹", string.Empty),
                new PickerFixtures.Plan("梅", null),
            },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
            SubDisplayMember = nameof(PickerFixtures.Plan.Note),
        };

        Assert.That(SubTexts(cell), Is.EqualTo(new string?[] { "全部入り", null, null }));
    }

    /// <summary>同名プロパティが複数ある型でも例外にならず、主表示・副表示とも未解決へ落ちる。</summary>
    [Test]
    public void AmbiguousMemberNameFallsBackInsteadOfThrowing()
    {
        PickerCell cell = new();

        Assert.That(
            () => cell.ItemsSource = new List<object> { new PickerFixtures.Indexed() },
            Throws.Nothing);
        Assert.That(
            () =>
            {
                cell.DisplayMember = "Item";
                cell.SubDisplayMember = "Item";
            },
            Throws.Nothing);

        Assert.That(Texts(cell), Is.EqualTo(new[] { "indexed" }));
        Assert.That(SubTexts(cell), Is.EqualTo(new string?[] { null }));
    }

    /// <summary>getter が送出した例外は握りつぶさず呼び出し元へ伝わる。</summary>
    [Test]
    public void GetterExceptionIsPropagated()
    {
        PickerCell cell = new();

        Assert.That(
            () => cell.ItemsSource = new List<object> { new PickerFixtures.Exploding() },
            Throws.Nothing);

        Assert.That(
            () => cell.DisplayMember = nameof(PickerFixtures.Exploding.Boom),
            Throws.InstanceOf<InvalidOperationException>());
    }

    /// <summary>null 要素を含む並びは設定時に拒否される。</summary>
    [Test]
    public void NullElementIsRejectedOnAssignment()
    {
        PickerCell cell = new();

        Assert.That(
            () => cell.ItemsSource = new List<object?> { "松", null },
            Throws.ArgumentException);
        Assert.That(cell.ItemsSource, Is.Null);
    }

    /// <summary>射影は DisplayMember / SubDisplayMember の差し替えで引き直される。</summary>
    [Test]
    public void ReplacingMembersReprojectsItems()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Plan("松", "全部入り") },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
        };

        Assert.That(Texts(cell), Is.EqualTo(new[] { "松" }));

        cell.DisplayMember = nameof(PickerFixtures.Plan.Note);
        cell.SubDisplayMember = nameof(PickerFixtures.Plan.Name);

        Assert.That(Texts(cell), Is.EqualTo(new[] { "全部入り" }));
        Assert.That(SubTexts(cell), Is.EqualTo(new string?[] { "松" }));
    }

    /// <summary>設定した並びを in-place で変更しても、表示と選択項目は設定時の写しのままになる。</summary>
    [Test]
    public void InPlaceMutationOfItemsSourceIsNotObserved()
    {
        List<object> items = [new PickerFixtures.Plan("松"), new PickerFixtures.Plan("竹")];
        PickerCell cell = new()
        {
            ItemsSource = items,
            DisplayMember = nameof(PickerFixtures.Plan.Name),
        };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        Fakes.GatewayScope scope = Fakes.GatewayScope.Connect(view).Reset();

        items[1] = new PickerFixtures.Plan("梅");
        items.Add(new PickerFixtures.Plan("桜"));
        scope.Gateway.Sink!.PickerCellSelectionChanged(view.Controller.FindCellId(cell)!, 1);

        Assert.That(Texts(cell), Is.EqualTo(new[] { "松", "竹" }));
        Assert.That(cell.SelectedItem, Is.EqualTo(new PickerFixtures.Plan("竹")));
    }

    /// <summary>候補の主表示の並び。</summary>
    /// <param name="cell">対象の Cell</param>
    private static IEnumerable<string> Texts(PickerCell cell)
        => ((KsPickerCellSnapshot)cell.CreateSnapshot()).Items.Select(static item => item.Text);

    /// <summary>候補の副表示の並び。副表示なしは null。</summary>
    /// <param name="cell">対象の Cell</param>
    private static IEnumerable<string?> SubTexts(PickerCell cell)
        => ((KsPickerCellSnapshot)cell.CreateSnapshot()).Items.Select(static item => item.SubText);
}
