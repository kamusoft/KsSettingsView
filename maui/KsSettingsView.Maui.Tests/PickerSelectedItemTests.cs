using System.Collections;
using System.Collections.Generic;
using KsSettingsView.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>PickerCell の SelectedItem / SelectedItems と選択位置の相互導出を確認する。</summary>
[TestFixture]
public class PickerSelectedItemTests
{
    /// <summary>binding の書き戻しが変更通知を伴っても動くよう、dispatcher を据える。</summary>
    [OneTimeSetUp]
    public void InstallDispatcher() => InlineDispatcher.Install();

    /// <summary>SelectedItem の設定は ItemsSource 内の位置へ解決される。</summary>
    [Test]
    public void SettingSelectedItemResolvesIndex()
    {
        PickerCell cell = new() { ItemsSource = Themes() };

        cell.SelectedItem = "ダーク";

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.EqualTo("ダーク"));
    }

    /// <summary>位置の変更は選択項目へ追随する。</summary>
    [Test]
    public void ChangingIndexUpdatesSelectedItem()
    {
        PickerCell cell = new() { ItemsSource = Themes() };

        cell.SelectedIndex = 0;

        Assert.That(cell.SelectedItem, Is.EqualTo("ライト"));
    }

    /// <summary>ItemsSource の差し替えで選択項目が引き直される。</summary>
    [Test]
    public void ReplacingItemsSourceRederivesSelectedItem()
    {
        PickerCell cell = new() { ItemsSource = Themes(), SelectedIndex = 1 };

        cell.ItemsSource = new List<string> { "赤", "青", "緑" };

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.EqualTo("青"));
    }

    /// <summary>ItemsSource 未設定のときの選択項目は null で、位置は保たれる。</summary>
    [Test]
    public void SelectedItemIsNullWithoutItemsSource()
    {
        PickerCell cell = new() { SelectedIndex = 0 };

        Assert.That(cell.SelectedItem, Is.Null);
        Assert.That(cell.SelectedIndex, Is.Zero);
    }

    /// <summary>範囲外の位置では選択項目が null になり、位置はそのまま残る。</summary>
    [Test]
    public void SelectedItemIsNullWhenIndexIsOutOfRange()
    {
        PickerCell cell = new() { ItemsSource = Themes() };

        cell.SelectedIndex = 5;

        Assert.That(cell.SelectedItem, Is.Null);
        Assert.That(cell.SelectedIndex, Is.EqualTo(5));
    }

    /// <summary>ItemsSource にない項目を指定すると未選択になり、選択項目も残らない。</summary>
    [Test]
    public void SettingUnknownSelectedItemClearsIndex()
    {
        PickerCell cell = new() { ItemsSource = Themes(), SelectedIndex = 1 };

        cell.SelectedItem = "セピア";

        Assert.That(cell.SelectedIndex, Is.Null);
        Assert.That(cell.SelectedItem, Is.Null);
    }

    /// <summary>ItemsSource 未設定のまま指定した選択項目は、書き戻されずそのまま保たれる。</summary>
    [Test]
    public void SettingSelectedItemWithoutItemsSourceIsHeld()
    {
        PickerCell cell = new();

        cell.SelectedItem = "セピア";
        cell.SelectedItems = new List<object> { "セピア" };

        Assert.That(cell.SelectedIndex, Is.Null);
        Assert.That(cell.SelectedIndices, Is.Null);
        Assert.That(cell.SelectedItem, Is.EqualTo("セピア"));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "セピア" }));
    }

    /// <summary>候補を外しても選択は保たれ、次に候補が届いた時点で逆引きし直される。</summary>
    [Test]
    public void ClearingItemsSourceHoldsSelectionUntilCandidatesReturn()
    {
        PickerCell cell = new()
        {
            SelectionMode = PickerSelectionMode.Multiple,
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
            SelectedIndex = 2,
            SelectedIndices = [0, 2],
        };

        cell.ItemsSource = null;

        Assert.That(cell.SelectedIndex, Is.EqualTo(2));
        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItem, Is.EqualTo("SMS"));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));

        cell.ItemsSource = new List<string> { "SMS", "メール", "プッシュ" };

        Assert.That(cell.SelectedIndex, Is.Zero);
        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 1 }));
        Assert.That(cell.SelectedItem, Is.EqualTo("SMS"));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "SMS", "メール" }));
    }

    /// <summary>候補より先に指定した選択項目は、候補が届いた時点で位置へ解決される。</summary>
    [Test]
    public void SelectedItemSetBeforeItemsSourceResolvesOnArrival()
    {
        PickerCell cell = new();
        cell.SelectedItem = "ダーク";

        cell.ItemsSource = Themes();

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.EqualTo("ダーク"));
    }

    /// <summary>候補より先に指定した選択項目の並びも、候補が届いた時点で解決される。</summary>
    [Test]
    public void SelectedItemsSetBeforeItemsSourceResolveOnArrival()
    {
        PickerCell cell = new() { SelectionMode = PickerSelectionMode.Multiple };
        cell.SelectedItems = new List<object> { "SMS", "メール" };

        cell.ItemsSource = new List<string> { "メール", "プッシュ", "SMS" };

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
    }

    /// <summary>候補が届いた時点で見つからない要素は落ちる。</summary>
    [Test]
    public void PendingSelectedItemsDropUnknownElementsOnArrival()
    {
        PickerCell cell = new() { SelectionMode = PickerSelectionMode.Multiple };
        cell.SelectedItem = "セピア";
        cell.SelectedItems = new List<object> { "SMS", "電話" };

        cell.ItemsSource = new List<string> { "メール", "プッシュ", "SMS" };

        Assert.That(cell.SelectedIndex, Is.Null);
        Assert.That(cell.SelectedItem, Is.Null);
        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 2 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "SMS" }));
    }

    /// <summary>バインドの登録順が逆でも、ViewModel の初期選択は失われない。</summary>
    [Test]
    public void ViewModelSelectionSurvivesReversedBindingOrder()
    {
        PickerFixtures.Plan bamboo = new("竹");
        PickerFixtures.Plan plum = new("梅");
        List<object> plans = [new PickerFixtures.Plan("松"), bamboo, plum];
        PickerFixtures.SelectionViewModel vm = new()
        {
            Items = plans,
            Selected = bamboo,
            SelectedMany = new List<object> { plans[0], plum },
        };
        PickerCell cell = new() { SelectionMode = PickerSelectionMode.Multiple };

        cell.BindingContext = vm;
        cell.SetBinding(
            PickerCell.SelectedItemProperty,
            new Binding(nameof(PickerFixtures.SelectionViewModel.Selected), BindingMode.TwoWay));
        cell.SetBinding(
            PickerCell.SelectedItemsProperty,
            new Binding(nameof(PickerFixtures.SelectionViewModel.SelectedMany), BindingMode.TwoWay));
        cell.SetBinding(
            PickerCell.ItemsSourceProperty,
            new Binding(nameof(PickerFixtures.SelectionViewModel.Items)));

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(vm.Selected, Is.SameAs(bamboo));
        Assert.That(vm.SelectedMany, Is.EqualTo(new object[] { plans[0], plum }));
    }

    /// <summary>
    /// 選択項目の正規化は Cell の公開値までで、TwoWay 先の ViewModel は値等価な別実体を持ち得る。
    /// </summary>
    [Test]
    public void NormalizationStopsAtCellAndDoesNotReachViewModel()
    {
        PickerFixtures.SelectionViewModel vm = new()
        {
            Items = new List<object> { new PickerFixtures.Plan("松"), new PickerFixtures.Plan("竹") },
        };
        PickerCell cell = new();
        cell.BindingContext = vm;
        cell.SetBinding(
            PickerCell.ItemsSourceProperty,
            new Binding(nameof(PickerFixtures.SelectionViewModel.Items)));
        cell.SetBinding(
            PickerCell.SelectedItemProperty,
            new Binding(nameof(PickerFixtures.SelectionViewModel.Selected), BindingMode.TwoWay));
        cell.SelectedIndex = 1;
        object? held = vm.Selected;

        PickerFixtures.Plan replaced = new("竹");
        vm.Items = new List<object> { new PickerFixtures.Plan("松"), replaced };

        Assert.That(cell.SelectedItem, Is.SameAs(replaced));
        Assert.That(vm.Selected, Is.SameAs(held));
        Assert.That(vm.Selected, Is.EqualTo(replaced));
        Assert.That(vm.Selected, Is.Not.SameAs(replaced));
    }

    /// <summary>選択項目を null にすると未選択になる。</summary>
    [Test]
    public void ClearingSelectedItemClearsIndex()
    {
        PickerCell cell = new() { ItemsSource = Themes(), SelectedIndex = 1 };

        cell.SelectedItem = null;

        Assert.That(cell.SelectedIndex, Is.Null);
    }

    /// <summary>ユーザーの選択確定では位置と選択項目の両方が更新される。</summary>
    [Test]
    public void UserSelectionUpdatesBothIndexAndItem()
    {
        PickerCell cell = new() { ItemsSource = Themes() };
        Fakes.GatewayScope scope = Connect(cell, out SettingsView view);

        scope.Gateway.Sink!.PickerCellSelectionChanged(view.Controller.FindCellId(cell)!, 1);

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.EqualTo("ダーク"));
    }

    /// <summary>object 候補でもユーザーの選択確定で元の要素が返る。</summary>
    [Test]
    public void UserSelectionReturnsOriginalObject()
    {
        PickerFixtures.Plan bamboo = new("竹", "標準");
        PickerCell cell = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Plan("松", "全部入り"), bamboo },
            DisplayMember = nameof(PickerFixtures.Plan.Name),
        };
        Fakes.GatewayScope scope = Connect(cell, out SettingsView view);

        scope.Gateway.Sink!.PickerCellSelectionChanged(view.Controller.FindCellId(cell)!, 1);

        Assert.That(cell.SelectedItem, Is.SameAs(bamboo));
    }

    /// <summary>同値の候補が複数あるとき、選択項目の逆引きは最初の位置に解決される。</summary>
    [Test]
    public void DuplicateItemResolvesToFirstIndex()
    {
        PickerCell cell = new() { ItemsSource = new List<string> { "松", "竹", "松" } };

        cell.SelectedItem = "松";

        Assert.That(cell.SelectedIndex, Is.Zero);
    }

    /// <summary>複数選択のユーザー確定では位置の並びと選択項目の並びの両方が更新される。</summary>
    [Test]
    public void UserMultiSelectionUpdatesBothIndicesAndItems()
    {
        PickerCell cell = new()
        {
            SelectionMode = PickerSelectionMode.Multiple,
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
        };
        Fakes.GatewayScope scope = Connect(cell, out SettingsView view);

        scope.Gateway.Sink!.PickerCellMultiSelectionChanged(view.Controller.FindCellId(cell)!, [2, 0]);

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
    }

    /// <summary>選択項目の並びは位置の昇順で導出され、範囲外の位置は除かれる。</summary>
    [Test]
    public void SelectedItemsAreDerivedInIndexOrderSkippingOutOfRange()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
            SelectedIndices = [2, 9, 0],
        };

        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 2, 9, 0 }));
    }

    /// <summary>選択項目の並びを設定すると、見つかった要素の位置だけが残る。</summary>
    [Test]
    public void SettingSelectedItemsKeepsOnlyResolvableElements()
    {
        PickerCell cell = new() { ItemsSource = new List<string> { "メール", "プッシュ", "SMS" } };

        cell.SelectedItems = new List<object> { "SMS", "電話", "メール" };

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
    }

    /// <summary>同値要素を重ねて設定しても、位置としても公開値としても 1 件に揃う。</summary>
    [Test]
    public void DuplicateSelectedItemsCollapseToSingleIndex()
    {
        PickerCell cell = new() { ItemsSource = new List<string> { "松", "竹", "松" } };

        cell.SelectedItems = new List<object> { "松", "松" };

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "松" }));
    }

    /// <summary>選択項目の並びを null にすると選択なしへ揃う。</summary>
    [Test]
    public void ClearingSelectedItemsWithNullSelectsNothing()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "メール", "プッシュ" },
            SelectedIndices = [0, 1],
        };

        cell.SelectedItems = null;

        Assert.That(cell.SelectedIndices, Is.Empty);
        Assert.That(cell.SelectedItems, Is.Empty);
    }

    /// <summary>空の並びの設定も選択なしとして扱われる。</summary>
    [Test]
    public void ClearingSelectedItemsWithEmptyListSelectsNothing()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "メール", "プッシュ" },
            SelectedIndices = [0, 1],
        };

        cell.SelectedItems = new List<object>();

        Assert.That(cell.SelectedIndices, Is.Empty);
        Assert.That(cell.SelectedItems, Is.Empty);
    }

    /// <summary>ItemsSource の差し替えで選択項目の並びも引き直される。</summary>
    [Test]
    public void ReplacingItemsSourceRederivesSelectedItems()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
            SelectedIndices = [0, 2],
        };

        cell.ItemsSource = new List<string> { "赤", "青", "緑" };

        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "赤", "緑" }));
    }

    /// <summary>値等価な別実体を設定しても、公開される選択項目は候補の実体に揃う。</summary>
    [Test]
    public void SettingValueEqualSelectedItemNormalizesToSourceInstance()
    {
        PickerFixtures.Plan bamboo = new("竹");
        List<object> plans = [new PickerFixtures.Plan("松"), bamboo];
        PickerCell cell = new() { ItemsSource = plans };

        cell.SelectedItem = new PickerFixtures.Plan("竹");

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.SameAs(bamboo));
    }

    /// <summary>値等価な別実体の並びへ差し替えると、選択項目は新しい候補の実体へ揃う。</summary>
    [Test]
    public void ReplacingItemsSourceWithValueEqualItemsNormalizesSelectedItem()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object> { new PickerFixtures.Plan("松"), new PickerFixtures.Plan("竹") },
            SelectedIndex = 1,
        };
        PickerFixtures.Plan replaced = new("竹");

        cell.ItemsSource = new List<object> { new PickerFixtures.Plan("松"), replaced };

        Assert.That(cell.SelectedItem, Is.SameAs(replaced));
    }

    /// <summary>値等価な別実体の並びを設定しても、選択項目の並びは候補の実体に揃う。</summary>
    [Test]
    public void SettingValueEqualSelectedItemsNormalizesToSourceInstances()
    {
        PickerFixtures.Plan pine = new("松");
        PickerFixtures.Plan plum = new("梅");
        List<object> plans = [pine, new PickerFixtures.Plan("竹"), plum];
        PickerCell cell = new() { ItemsSource = plans };

        cell.SelectedItems = new List<object> { new PickerFixtures.Plan("松"), new PickerFixtures.Plan("梅") };

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItems![0], Is.SameAs(pine));
        Assert.That(cell.SelectedItems![1], Is.SameAs(plum));
    }

    /// <summary>値等価な別実体の並びへ差し替えると、選択項目の並びも新しい候補の実体へ揃う。</summary>
    [Test]
    public void ReplacingItemsSourceWithValueEqualItemsNormalizesSelectedItems()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<object>
            {
                new PickerFixtures.Plan("松"),
                new PickerFixtures.Plan("竹"),
                new PickerFixtures.Plan("梅"),
            },
            SelectedIndices = [0, 2],
        };
        PickerFixtures.Plan pine = new("松");
        PickerFixtures.Plan plum = new("梅");

        cell.ItemsSource = new List<object> { pine, new PickerFixtures.Plan("竹"), plum };

        Assert.That(cell.SelectedItems![0], Is.SameAs(pine));
        Assert.That(cell.SelectedItems![1], Is.SameAs(plum));
    }

    /// <summary>選択候補。</summary>
    private static IList Themes() => new List<string> { "ライト", "ダーク" };

    /// <summary>Cell を載せた SettingsView を Native へ繋いだ状態にする。</summary>
    /// <param name="cell">載せる Cell</param>
    /// <param name="view">組み立てた SettingsView</param>
    private static Fakes.GatewayScope Connect(PickerCell cell, out SettingsView view)
    {
        Section section = new() { Cells = { cell } };
        view = new SettingsView { Root = { section } };
        return Fakes.GatewayScope.Connect(view).Reset();
    }
}
