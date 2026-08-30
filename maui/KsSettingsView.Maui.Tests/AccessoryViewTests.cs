using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using KsSettingsView.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// Root / Section の header・footer に置いた View が accessory の更新へ変換されることを確認する。
/// </summary>
/// <remarks>
/// 4 つの位置 (Root / Section × header / footer) で契約は同一のため、位置を与える形で同じ検証を回す。
/// </remarks>
[TestFixture]
internal sealed class AccessoryViewTests
{
    /// <summary>検証対象の 4 つの位置。</summary>
    private static readonly KsAccessoryTarget[] Targets =
    [
        KsAccessoryTarget.RootHeader,
        KsAccessoryTarget.RootFooter,
        KsAccessoryTarget.SectionHeader,
        KsAccessoryTarget.SectionFooter,
    ];

    // ---- 設定・クリア ----

    /// <summary>View の設定は実体化された platform view の更新として配信される。</summary>
    [TestCaseSource(nameof(Targets))]
    public void ViewIsDeliveredAsAccessoryView(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.Reset();

        fixture.SetView(target, accessory);

        GatewayCall.UpdateAccessoryView call = fixture.Scope.Single<GatewayCall.UpdateAccessoryView>();
        Assert.That(call.Target, Is.EqualTo(target));
        Assert.That(call.SectionId, Is.EqualTo(fixture.SectionIdOf(target)));
        Assert.That(call.View, Is.SameAs(fixture.Scope.Views.LatestFor(accessory).PlatformView));
    }

    /// <summary>View を null に戻すと、控えているテキストの表示へ戻る。</summary>
    [TestCaseSource(nameof(Targets))]
    public void NullViewFallsBackToText(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.SetText(target, "text");
        fixture.SetView(target, new Label());
        fixture.Reset();

        fixture.SetView(target, null);

        GatewayCall.UpdateAccessory call = fixture.Scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(target));
        Assert.That(call.SectionId, Is.EqualTo(fixture.SectionIdOf(target)));
        Assert.That(call.Text, Is.EqualTo("text"));
    }

    /// <summary>テキストが無いまま View を null に戻すと accessory なしになる。</summary>
    [TestCaseSource(nameof(Targets))]
    public void NullViewWithoutTextClearsAccessory(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.SetView(target, new Label());
        fixture.Reset();

        fixture.SetView(target, null);

        GatewayCall.UpdateAccessory call = fixture.Scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Text, Is.Null);
    }

    // ---- View 優先とフォールバック ----

    /// <summary>View が置かれている間はテキストの変更を配信しない。</summary>
    [TestCaseSource(nameof(Targets))]
    public void TextIsNotDeliveredWhileViewIsPlaced(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.SetView(target, new Label());
        fixture.Reset();

        fixture.SetText(target, "text");

        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessory>(), Is.Empty);
    }

    /// <summary>View 表示中に変えたテキストは、View を外した時点で反映される。</summary>
    [TestCaseSource(nameof(Targets))]
    public void TextChangedWhileViewIsPlacedIsAppliedAfterRelease(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.SetText(target, "first");
        fixture.SetView(target, new Label());
        fixture.SetText(target, "second");
        fixture.Reset();

        fixture.SetView(target, null);

        Assert.That(fixture.Scope.Single<GatewayCall.UpdateAccessory>().Text, Is.EqualTo("second"));
    }

    /// <summary>View を置くと、その前に設定されていたテキストは表示に使われない。</summary>
    [TestCaseSource(nameof(Targets))]
    public void ViewTakesPrecedenceOverText(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.SetText(target, "text");
        fixture.Reset();

        fixture.SetView(target, new Label());

        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessoryView>(), Has.Count.EqualTo(1));
        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessory>(), Is.Empty);
    }

    // ---- 差し替え ----

    /// <summary>別のインスタンスへの差し替えは、明示の更新として配信し直される。</summary>
    [TestCaseSource(nameof(Targets))]
    public void ReplacingViewReissuesTheAccessory(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label first = new();
        Label second = new();
        fixture.SetView(target, first);
        fixture.Reset();

        fixture.SetView(target, second);

        GatewayCall.UpdateAccessoryView call = fixture.Scope.Single<GatewayCall.UpdateAccessoryView>();
        Assert.That(call.View, Is.SameAs(fixture.Scope.Views.LatestFor(second).PlatformView));
        Assert.That(fixture.Scope.Views.LatestFor(first).IsDisposed, Is.True);
    }

    /// <summary>旧実体の破棄は、新しい内容が native へ配信された後に行われる。</summary>
    [TestCaseSource(nameof(Targets))]
    public void PreviousViewIsDisposedAfterTheNewOneIsDelivered(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label first = new();
        fixture.SetView(target, first);
        fixture.Reset();

        int deliveredAtDispose = -1;
        fixture.Scope.Views.LatestFor(first).OnDispose =
            () => deliveredAtDispose = fixture.Scope.All<GatewayCall.UpdateAccessoryView>().Count;

        fixture.SetView(target, new Label());

        Assert.That(deliveredAtDispose, Is.EqualTo(1));
    }

    /// <summary>View を null にしたときも、解除の配信を終えてから旧実体を破棄する。</summary>
    [TestCaseSource(nameof(Targets))]
    public void PreviousViewIsDisposedAfterTheClearIsDelivered(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.SetView(target, placed);
        fixture.Reset();

        int deliveredAtDispose = -1;
        fixture.Scope.Views.LatestFor(placed).OnDispose =
            () => deliveredAtDispose = fixture.Scope.All<GatewayCall.UpdateAccessory>().Count;

        fixture.SetView(target, null);

        Assert.That(deliveredAtDispose, Is.EqualTo(1));
    }

    // ---- 内容変化に伴う測り直し ----

    /// <summary>必要サイズの変化は、同じ配信の区切りで 1 回だけ測り直しとして送られる。</summary>
    [TestCaseSource(nameof(Targets))]
    public void MeasureInvalidationIsCoalescedIntoOneRequest(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.SetView(target, accessory);
        fixture.Reset();

        FakeViewLease lease = fixture.Scope.Views.LatestFor(accessory);
        lease.RaiseMeasureInvalidated();
        lease.RaiseMeasureInvalidated();
        lease.RaiseMeasureInvalidated();
        fixture.Scope.Flush();

        GatewayCall.InvalidateAccessoryMeasurement call =
            fixture.Scope.Single<GatewayCall.InvalidateAccessoryMeasurement>();
        Assert.That(call.Target, Is.EqualTo(target));
        Assert.That(call.SectionId, Is.EqualTo(fixture.SectionIdOf(target)));

        // 内容の変化そのものは live な実体が描き替えるため、accessory は送り直さない。
        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessoryView>(), Is.Empty);
    }

    /// <summary>外された View の必要サイズの変化は測り直しにならない。</summary>
    [TestCaseSource(nameof(Targets))]
    public void MeasureInvalidationOfARemovedViewIsIgnored(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.SetView(target, accessory);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(accessory);
        fixture.SetView(target, null);
        fixture.Reset();

        lease.RaiseMeasureInvalidated();
        fixture.Scope.Flush();

        Assert.That(fixture.Scope.All<GatewayCall.InvalidateAccessoryMeasurement>(), Is.Empty);
    }

    // ---- BindingContext の継承 ----

    /// <summary>root の accessory は SettingsView の BindingContext を継承する。</summary>
    [Test]
    public void RootAccessoryInheritsTheViewBindingContext()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);

        fixture.View.BindingContext = new Owner("root context");
        fixture.View.RootHeaderView = accessory;

        Assert.That(accessory.Text, Is.EqualTo("root context"));
    }

    /// <summary>Section の accessory は所有 Section の BindingContext を継承する。</summary>
    [Test]
    public void SectionAccessoryInheritsTheOwningSectionBindingContext()
    {
        ObservableCollection<string> items = ["item context"];
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (string item) => item);
        SettingsView view = new()
        {
            ItemsSource = items,
            ItemTemplate = new DataTemplate(() => new Section { HeaderView = accessory }),
        };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        view.BindingContext = new Owner("view context");

        Assert.That(view.Root, Has.Count.EqualTo(1));
        Assert.That(view.Root[0].BindingContext, Is.EqualTo("item context"));
        Assert.That(accessory.Text, Is.EqualTo("item context"));
    }

    /// <summary>所有者の BindingContext の変更は accessory へ伝わる。</summary>
    [Test]
    public void BindingContextChangePropagatesToTheAccessory()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.Section.HeaderView = accessory;
        fixture.View.BindingContext = new Owner("first");

        fixture.View.BindingContext = new Owner("second");

        Assert.That(accessory.Text, Is.EqualTo("second"));
    }

    /// <summary>明示的に設定された BindingContext は継承で上書きされない。</summary>
    [Test]
    public void ExplicitBindingContextIsNotOverwritten()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new() { BindingContext = new Owner("explicit") };
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);

        fixture.View.BindingContext = new Owner("inherited");
        fixture.View.RootHeaderView = accessory;

        Assert.That(accessory.Text, Is.EqualTo("explicit"));
    }

    // ---- 多重配置 ----

    /// <summary>同じ View を 2 箇所へ置くと例外になる。</summary>
    [Test]
    public void PlacingTheSameViewTwiceThrows()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.View.RootHeaderView = accessory;

        Assert.Throws<InvalidOperationException>(() => fixture.Section.HeaderView = accessory);
    }

    /// <summary>同じ Section の header と footer へ置く場合も例外になる。</summary>
    [Test]
    public void PlacingTheSameViewInHeaderAndFooterThrows()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.Section.HeaderView = accessory;

        Assert.Throws<InvalidOperationException>(() => fixture.Section.FooterView = accessory);
    }

    /// <summary>同じ View を持つ Section を後から追加した場合も例外になる。</summary>
    [Test]
    public void AddingASectionThatReusesAPlacedViewThrows()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.View.RootHeaderView = accessory;
        FakeViewLease placed = fixture.Scope.Views.LatestFor(accessory);
        fixture.Reset();

        // Section の組み立ては例外にならない — 置かれている View は引き取られないだけで、
        // 多重配置として弾かれるのは設定ツリーへ入れる時点になる。
        Section added = new() { HeaderView = accessory };
        Assert.That(accessory.Parent, Is.SameAs(fixture.View));

        Assert.Throws<InvalidOperationException>(() => fixture.View.Root.Add(added));

        Assert.That(accessory.Parent, Is.SameAs(fixture.View));
        Assert.That(accessory.Text, Is.EqualTo("root-ctx"));
        Assert.That(fixture.Scope.Views.LatestFor(accessory), Is.SameAs(placed));
        Assert.That(placed.IsDisposed, Is.False);

        // 追加は native へ 1 件も届かない。検査は Section を対応表へ載せる前に全件を通る。
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>
    /// 変換経路が繋がっていない間に組み立てた重複は、繋がった時点で例外になる。
    /// </summary>
    /// <remarks>
    /// 未参加の所有者には検査を行う相手がいないため、重複は設定ツリーへ入れる時点まで持ち越される。
    /// </remarks>
    [Test]
    public void ADuplicateBuiltWhileDisconnectedThrowsWhenTheHostConnects()
    {
        Label shared = new();
        SettingsView view = new();

        Assert.DoesNotThrow(() =>
        {
            view.Root.Add(new Section { HeaderView = shared });
            view.Root.Add(new Section { HeaderView = shared });
        });

        Assert.Throws<InvalidOperationException>(() => GatewayScope.Connect(view));
    }

    /// <summary>
    /// テンプレートが配置済みの View を使い回すと例外になり、既存の配置は無傷のままになる。
    /// </summary>
    /// <remarks>
    /// 生成された Section は設定ツリーへ入るまで変換経路に載らないため、多重配置の検査を
    /// 通らないまま所有を確定させると既存の配置を奪ってしまう経路になる。
    /// </remarks>
    [Test]
    public void GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.View.RootHeaderView = accessory;
        fixture.View.ItemsSource = new ObservableCollection<string> { "item" };
        fixture.Reset();

        Assert.Throws<InvalidOperationException>(
            () => fixture.View.ItemTemplate = new DataTemplate(() => new Section
            {
                HeaderView = accessory,
            }));

        Assert.That(accessory.Parent, Is.SameAs(fixture.View));
        Assert.That(accessory.Text, Is.EqualTo("root-ctx"));
        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessoryView>(), Is.Empty);
    }

    /// <summary>
    /// Root への多重配置が例外になっても、既に置かれている Section 側の配置は無傷のままになる。
    /// </summary>
    /// <remarks>
    /// 壊れてはいけないのは失敗した配置ではなく、何も間違っていない側の既存配置である。
    /// 論理上の親も、そこから継承している BindingContext も、native への輸送も動かない。
    /// </remarks>
    [Test]
    public void FailedRootPlacementLeavesTheExistingSectionPlacementIntact()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.Section.BindingContext = new Owner("section-ctx");
        fixture.Section.HeaderView = accessory;
        object? transported =
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader);

        Assert.Throws<InvalidOperationException>(() => fixture.View.RootHeaderView = accessory);

        Assert.That(accessory.Parent, Is.SameAs(fixture.Section));
        Assert.That(accessory.Text, Is.EqualTo("section-ctx"));
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader),
            Is.SameAs(transported));
    }

    /// <summary>Section への多重配置が例外になっても、既に置かれている Root 側の配置は無傷になる。</summary>
    [Test]
    public void FailedSectionPlacementLeavesTheExistingRootPlacementIntact()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.Section.BindingContext = new Owner("section-ctx");
        fixture.View.RootHeaderView = accessory;

        Assert.Throws<InvalidOperationException>(() => fixture.Section.HeaderView = accessory);

        Assert.That(accessory.Parent, Is.SameAs(fixture.View));
        Assert.That(accessory.Text, Is.EqualTo("root-ctx"));
    }

    /// <summary>Native Host が無い状態でも、多重配置の例外は既存配置を壊さない。</summary>
    [Test]
    public void FailedPlacementLeavesTheExistingPlacementIntactWithoutAHost()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        accessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.Section.BindingContext = new Owner("section-ctx");
        fixture.Section.HeaderView = accessory;
        fixture.Scope.Attach();

        // Native Host を手放し、platform 実体が無い状態で誤った配置を試す。
        fixture.View.ReleaseHost();
        Assert.That(fixture.Scope.Views.LatestFor(accessory).IsDisposed, Is.True);

        Assert.Throws<InvalidOperationException>(() => fixture.View.RootHeaderView = accessory);

        Assert.That(accessory.Parent, Is.SameAs(fixture.Section));
        Assert.That(accessory.Text, Is.EqualTo("section-ctx"));
    }

    /// <summary>
    /// 外された View は、まだ設定ツリーに入れていない Section でも引き取れる。
    /// </summary>
    /// <remarks>
    /// 受け皿が既配置の View を引き取らないようにしても、正規の置き直しは通る必要がある。
    /// </remarks>
    [Test]
    public void UnregisteredSectionClaimsAViewThatWasReleased()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.View.RootHeaderView = accessory;
        fixture.View.RootHeaderView = null;

        Section added = new() { HeaderView = accessory };
        Assert.That(accessory.Parent, Is.SameAs(added));

        fixture.View.Root.Add(added);

        Assert.That(accessory.Parent, Is.SameAs(added));
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(added, KsAccessoryTarget.SectionHeader),
            Is.SameAs(fixture.Scope.Views.LatestFor(accessory).PlatformView));
    }

    /// <summary>変換経路が繋がっていなくても多重配置は例外になる。</summary>
    [Test]
    public void DuplicatePlacementIsDetectedWithoutAGateway()
    {
        SettingsView view = new() { Root = { new Section() } };
        Label accessory = new();
        view.RootHeaderView = accessory;

        Assert.Throws<InvalidOperationException>(() => view.RootFooterView = accessory);

        Assert.That(accessory.Parent, Is.SameAs(view));
        Assert.That(view.RootHeaderView, Is.SameAs(accessory));
    }

    // ---- 多重配置: 失敗した差し替えの原子性 ----

    /// <summary>他所で使われている View への差し替えに失敗しても、今の View がそのまま残る。</summary>
    /// <remarks>
    /// 検査はプロパティが値を確定させる前に通るため、公開値・論理上の所有・実体・表示のいずれも
    /// 動かない。動いてはいけないのは失敗した側だけでなく、衝突相手の正しい配置も同じ。
    /// </remarks>
    [TestCaseSource(nameof(Targets))]
    public void AFailedViewReplacementKeepsTheCurrentView(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.SetView(target, placed);

        // 衝突相手は、別の Section の header に正しく置かれている View。
        Label other = new();
        Section otherSection = new() { HeaderView = other };
        fixture.View.Root.Add(otherSection);

        FakeViewLease lease = fixture.Scope.Views.LatestFor(placed);
        object? otherTransported =
            fixture.Scope.Gateway.AccessoryViewOf(otherSection, KsAccessoryTarget.SectionHeader);
        fixture.Reset();

        Assert.Throws<InvalidOperationException>(() => fixture.SetView(target, other));

        // 公開値・論理上の所有・実体のいずれも変更前のまま。
        Assert.That(fixture.ViewOf(target), Is.SameAs(placed));
        Assert.That(placed.Parent, Is.SameAs(fixture.OwnerOf(target)));
        Assert.That(fixture.Scope.Views.LatestFor(placed), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);

        // 失敗した操作に由来する配信は 1 件も起きない。
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);

        // 置かれている側も無傷で残る。
        Assert.That(other.Parent, Is.SameAs(otherSection));
        Assert.That(fixture.Scope.Views.LatestFor(other).IsDisposed, Is.False);
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(otherSection, KsAccessoryTarget.SectionHeader),
            Is.SameAs(otherTransported));
    }

    /// <summary>Cell の内容の View への差し替えに失敗しても、root の View がそのまま残る。</summary>
    [Test]
    public void AFailedRootViewReplacementWithACellContentKeepsTheCurrentView()
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.View.RootHeaderView = placed;

        Label content = new();
        CustomCell cell = new() { Content = content };
        fixture.Section.Cells.Add(cell);

        FakeViewLease lease = fixture.Scope.Views.LatestFor(placed);
        fixture.Reset();

        Assert.Throws<InvalidOperationException>(() => fixture.View.RootHeaderView = content);

        Assert.That(fixture.View.RootHeaderView, Is.SameAs(placed));
        Assert.That(placed.Parent, Is.SameAs(fixture.View));
        Assert.That(fixture.Scope.Views.LatestFor(placed), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);

        // 内容として置かれている側も無傷で残る。
        Assert.That(content.Parent, Is.SameAs(cell));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    // ---- 多重配置: 構造変更のまとまり ----

    /// <summary>
    /// 同じ View を header に持つ 2 つの Section を 1 度に追加すると、1 件も入れないまま例外になる。
    /// </summary>
    [Test]
    public void AddingSectionsThatShareAnAccessoryViewThrowsBeforeAnyInsert()
    {
        Label placedView = new();
        Section placedSection = new() { HeaderView = placedView };
        RangeAddCollection<Section> root = [placedSection];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        IReadOnlyList<string> sectionIds = scope.Gateway.SectionIds;
        string placedId = view.Controller.FindSectionId(placedSection)!;
        FakeViewLease lease = scope.Views.LatestFor(placedView);
        scope.Reset();

        Label shared = new();
        Section first = new() { HeaderView = shared };
        Section second = new() { HeaderView = shared };

        Assert.Throws<InvalidOperationException>(() => root.AddRange(first, second));

        // native も対応表も、追加の途中まで進んだ形にならない。
        Assert.That(scope.Gateway.Calls, Is.Empty);
        Assert.That(scope.Gateway.SectionIds, Is.EqualTo(sectionIds));
        Assert.That(view.Controller.FindSectionId(first), Is.Null);
        Assert.That(view.Controller.FindSectionId(second), Is.Null);
        Assert.That(scope.Views.CountFor(shared), Is.Zero);

        // 公開コレクションは呼び出し元の操作後のまま残り、表示とは分離する。
        Assert.That(root, Is.EqualTo(new[] { placedSection, first, second }));

        // 置かれていた Section はそのまま残る。
        Assert.That(view.Controller.FindSectionId(placedSection), Is.EqualTo(placedId));
        Assert.That(scope.Views.LatestFor(placedView), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(placedView.Parent, Is.SameAs(placedSection));
    }

    /// <summary>
    /// header と配下の内容が同じ View を指す Section の追加は、1 件も入れないまま例外になる。
    /// </summary>
    [Test]
    public void AddingASectionWhoseAccessoryAndCellContentShareAViewThrowsBeforeAnyInsert()
    {
        Section placedSection = new();
        RangeAddCollection<Section> root = [placedSection];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        IReadOnlyList<string> sectionIds = scope.Gateway.SectionIds;
        scope.Reset();

        Label shared = new();
        CustomCell cell = new() { Content = shared };
        Section added = new() { HeaderView = shared, Cells = { cell } };

        Assert.Throws<InvalidOperationException>(() => root.Add(added));

        // native も対応表も、追加の途中まで進んだ形にならない。
        Assert.That(scope.Gateway.Calls, Is.Empty);
        Assert.That(scope.Gateway.SectionIds, Is.EqualTo(sectionIds));
        Assert.That(view.Controller.FindSectionId(added), Is.Null);
        Assert.That(view.Controller.FindCellId(cell), Is.Null);
        Assert.That(scope.Views.CountFor(shared), Is.Zero);

        // 内容として先に引き取られていた所有はそのまま残る。
        Assert.That(shared.Parent, Is.SameAs(cell));
    }

    /// <summary>後続の Section だけが既存配置と衝突しても、先頭の Section は入らない。</summary>
    [Test]
    public void AddingSectionsWhereOnlyTheLastCollidesLeavesTheFirstOut()
    {
        Label placedView = new();
        Section placedSection = new() { HeaderView = placedView };
        RangeAddCollection<Section> root = [placedSection];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        IReadOnlyList<string> sectionIds = scope.Gateway.SectionIds;
        object? transported =
            scope.Gateway.AccessoryViewOf(placedSection, KsAccessoryTarget.SectionHeader);
        scope.Reset();

        Section free = new() { HeaderView = new Label() };
        Section colliding = new() { HeaderView = placedView };

        Assert.Throws<InvalidOperationException>(() => root.AddRange(free, colliding));

        Assert.That(scope.Gateway.Calls, Is.Empty);
        Assert.That(scope.Gateway.SectionIds, Is.EqualTo(sectionIds));
        Assert.That(view.Controller.FindSectionId(free), Is.Null);
        Assert.That(view.Controller.FindSectionId(colliding), Is.Null);

        // 既存の配置はそのまま残る。
        Assert.That(placedView.Parent, Is.SameAs(placedSection));
        Assert.That(
            scope.Gateway.AccessoryViewOf(placedSection, KsAccessoryTarget.SectionHeader),
            Is.SameAs(transported));
    }

    /// <summary>後続だけが衝突する差し替えでも、先頭の差し替えは適用されない。</summary>
    [Test]
    public void ReplacingSectionsWhereOnlyTheLastCollidesAppliesNothing()
    {
        Label placedView = new();
        Section placedSection = new() { HeaderView = placedView };
        Section replacedFirst = new();
        Section replacedSecond = new();
        RangeReplaceCollection<Section> root = [replacedFirst, replacedSecond, placedSection];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        IReadOnlyList<string> sectionIds = scope.Gateway.SectionIds;
        string firstId = view.Controller.FindSectionId(replacedFirst)!;
        string secondId = view.Controller.FindSectionId(replacedSecond)!;
        scope.Reset();

        Section free = new() { HeaderView = new Label() };
        Section colliding = new() { HeaderView = placedView };

        Assert.Throws<InvalidOperationException>(() => root.ReplaceRange(0, free, colliding));

        Assert.That(scope.Gateway.Calls, Is.Empty);
        Assert.That(scope.Gateway.SectionIds, Is.EqualTo(sectionIds));
        Assert.That(view.Controller.FindSectionId(free), Is.Null);
        Assert.That(view.Controller.FindSectionId(colliding), Is.Null);

        // 差し替え前の Section が対応表に残り、表示も差し替え前のままになる。
        Assert.That(view.Controller.FindSectionId(replacedFirst), Is.EqualTo(firstId));
        Assert.That(view.Controller.FindSectionId(replacedSecond), Is.EqualTo(secondId));
        Assert.That(placedView.Parent, Is.SameAs(placedSection));
    }

    /// <summary>まとまりの失敗で分離した表示は、Root の作り直しで揃い直せる。</summary>
    [Test]
    public void AFailedAddBatchIsRecoveredByRebuildingTheRoot()
    {
        Section placedSection = new();
        RangeAddCollection<Section> root = [placedSection];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        Label shared = new();
        Section first = new() { HeaderView = shared };
        Section second = new() { HeaderView = shared };
        Assert.Throws<InvalidOperationException>(() => root.AddRange(first, second));
        Assert.That(view.Controller.FindSectionId(first), Is.Null);

        // 衝突する Section を除いた新しいコレクションを設定し直す。
        view.Root = new ObservableCollection<Section> { placedSection, first };

        Assert.That(scope.Gateway.SectionIds, Has.Count.EqualTo(2));
        Assert.That(view.Controller.FindSectionId(first), Is.Not.Null);

        // 論理上の所有は受け皿が引き取った時点から first にあり、作り直しで表示にも載る。
        Assert.That(shared.Parent, Is.SameAs(first));
        Assert.That(
            scope.Gateway.AccessoryViewOf(first, KsAccessoryTarget.SectionHeader),
            Is.SameAs(scope.Views.LatestFor(shared).PlatformView));
    }

    /// <summary>取り除かれた Section は、置き場所の検査を受けない Section に戻る。</summary>
    [Test]
    public void ASectionRemovedFromTheRootStopsConsultingThePlacement()
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.Section.HeaderView = placed;
        Section dropped = new();
        fixture.View.Root.Add(dropped);

        fixture.View.Root.Remove(dropped);

        // 外れた Section には検査を行う相手がいないため、置かれている View を設定しても例外に
        // ならず、引き取られないだけになる。
        Assert.DoesNotThrow(() => dropped.HeaderView = placed);
        Assert.That(placed.Parent, Is.SameAs(fixture.Section));
    }

    /// <summary>作り直しで設定ツリーから外れた Section も、置き場所の検査を受けなくなる。</summary>
    [Test]
    public void ASectionDroppedByARootRebuildStopsConsultingThePlacement()
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.Section.HeaderView = placed;
        Section dropped = new();
        fixture.View.Root.Add(dropped);

        // 作り直しで 2 つ目の Section ごと設定ツリーから外す。
        fixture.View.Root = new ObservableCollection<Section> { fixture.Section };

        Assert.DoesNotThrow(() => dropped.HeaderView = placed);
        Assert.That(placed.Parent, Is.SameAs(fixture.Section));
    }

    // ---- 多重配置: Root の作り直し ----

    /// <summary>作り直す設定ツリーの中に同じ View が 2 つあると、作り直す前に例外になる。</summary>
    [Test]
    public void RebuildingWithADuplicateAccessoryThrowsWithoutTouchingTheCurrentTree()
    {
        Fixture fixture = Fixture.Connected();
        Label placed = new();
        fixture.Section.HeaderView = placed;
        FakeViewLease lease = fixture.Scope.Views.LatestFor(placed);
        string sectionId = fixture.View.Controller.FindSectionId(fixture.Section)!;
        fixture.Reset();

        Label shared = new();
        Section first = new() { HeaderView = shared };
        Section second = new() { HeaderView = shared };

        Assert.Throws<InvalidOperationException>(
            () => fixture.View.Root = new ObservableCollection<Section> { first, second });

        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
        Assert.That(fixture.View.Controller.FindSectionId(fixture.Section), Is.EqualTo(sectionId));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(placed.Parent, Is.SameAs(fixture.Section));
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader),
            Is.SameAs(lease.PlatformView));
    }

    /// <summary>作り直す設定ツリーが root の accessory と衝突しても、今の木には触れない。</summary>
    /// <remarks>root の accessory は設定ツリーの外に置かれているため、作り直しをまたいで残る。</remarks>
    [Test]
    public void RebuildingWithASectionThatCollidesWithTheRootAccessoryThrows()
    {
        Fixture fixture = Fixture.Connected();
        Label rootAccessory = new();
        fixture.View.RootHeaderView = rootAccessory;
        FakeViewLease lease = fixture.Scope.Views.LatestFor(rootAccessory);
        string sectionId = fixture.View.Controller.FindSectionId(fixture.Section)!;
        fixture.Reset();

        Section added = new() { HeaderView = rootAccessory };

        Assert.Throws<InvalidOperationException>(
            () => fixture.View.Root = new ObservableCollection<Section> { fixture.Section, added });

        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
        Assert.That(fixture.View.Controller.FindSectionId(fixture.Section), Is.EqualTo(sectionId));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(rootAccessory.Parent, Is.SameAs(fixture.View));
    }

    /// <summary>同じ Section を含む作り直しは、その accessory を保ったまま成立する。</summary>
    [Test]
    public void RebuildingWithTheSameSectionKeepsItsAccessory()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.Section.HeaderView = accessory;

        Assert.DoesNotThrow(() => fixture.View.Root =
            new ObservableCollection<Section> { fixture.Section, new Section() });

        Assert.That(accessory.Parent, Is.SameAs(fixture.Section));
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader),
            Is.SameAs(fixture.Scope.Views.LatestFor(accessory).PlatformView));
    }

    // ---- 実体化の時点で所有が確定していること ----

    /// <summary>
    /// platform 実体は、論理上の所有と BindingContext が確定した後に作られる。
    /// </summary>
    /// <remarks>
    /// 実体化は Handler を作る操作であり、その時点で BindingContext が定まっていないと、
    /// 既定値で組み上がった Handler が直後に組み直されることになる。
    /// </remarks>
    [TestCaseSource(nameof(Targets))]
    public void ViewIsOwnedBeforeItIsMaterialized(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.Section.BindingContext = new Owner("section-ctx");
        Label accessory = new();

        fixture.SetView(target, accessory);

        FakeViewLease lease = fixture.Scope.Views.LatestFor(accessory);
        Assert.That(lease.ParentAtMaterialize, Is.SameAs(fixture.OwnerOf(target)));
        Assert.That(
            lease.BindingContextAtMaterialize,
            Is.SameAs(fixture.OwnerOf(target).BindingContext));
    }

    /// <summary>Host を取り付け直したときの実体化でも、所有は先に確定している。</summary>
    [TestCaseSource(nameof(Targets))]
    public void ViewIsOwnedBeforeItIsRematerializedForANewHost(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.View.BindingContext = new Owner("root-ctx");
        fixture.Section.BindingContext = new Owner("section-ctx");
        Label accessory = new();
        fixture.SetView(target, accessory);
        fixture.Scope.Attach();

        fixture.View.ReleaseHost();
        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        FakeViewLease lease = fixture.Scope.Views.LatestFor(accessory);
        Assert.That(fixture.Scope.Views.CountFor(accessory), Is.EqualTo(2));
        Assert.That(lease.ParentAtMaterialize, Is.SameAs(fixture.OwnerOf(target)));
        Assert.That(
            lease.BindingContextAtMaterialize,
            Is.SameAs(fixture.OwnerOf(target).BindingContext));
    }

    /// <summary>null で外した View は別の位置へ置き直せる。</summary>
    [Test]
    public void ViewCanBePlacedAgainAfterItIsReleased()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.View.RootHeaderView = accessory;
        fixture.View.RootHeaderView = null;
        fixture.Reset();

        fixture.Section.HeaderView = accessory;

        GatewayCall.UpdateAccessoryView call = fixture.Scope.Single<GatewayCall.UpdateAccessoryView>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.SectionHeader));
        Assert.That(call.View, Is.SameAs(fixture.Scope.Views.LatestFor(accessory).PlatformView));
    }

    // ---- Native Host の世代 ----

    /// <summary>Host を手放すと実体は破棄され、取り付け直しで作り直される。</summary>
    [TestCaseSource(nameof(Targets))]
    public void AccessoryViewIsRebuiltForANewHost(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.SetView(target, accessory);
        fixture.Scope.Attach();
        FakeViewLease first = fixture.Scope.Views.LatestFor(accessory);

        fixture.View.ReleaseHost();
        Assert.That(first.IsDisposed, Is.True);

        fixture.Reset();
        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        FakeViewLease second = fixture.Scope.Views.LatestFor(accessory);
        Assert.That(second, Is.Not.SameAs(first));
        Assert.That(fixture.Scope.Views.CountFor(accessory), Is.EqualTo(2));
        Assert.That(
            fixture.Scope.All<GatewayCall.UpdateAccessoryView>(),
            Has.One.Matches<GatewayCall.UpdateAccessoryView>(
                call => call.Target == target && ReferenceEquals(call.View, second.PlatformView)));
    }

    /// <summary>Host を手放すとき、Section の accessory は控えているテキストへ書き戻される。</summary>
    [Test]
    public void SectionAccessoryIsWrittenBackAsTextWhenTheHostIsReleased()
    {
        Fixture fixture = Fixture.Connected();
        fixture.Section.HeaderText = "header";
        fixture.Section.HeaderView = new Label();
        fixture.Scope.Attach();
        fixture.Reset();

        fixture.View.ReleaseHost();

        GatewayCall.UpdateAccessory call = fixture.Scope.Single<GatewayCall.UpdateAccessory>();
        Assert.That(call.Target, Is.EqualTo(KsAccessoryTarget.SectionHeader));
        Assert.That(call.SectionId, Is.EqualTo(fixture.SectionId));
        Assert.That(call.Text, Is.EqualTo("header"));
    }

    /// <summary>切断中に置いた View は、取り付け直したときの表示に反映される。</summary>
    [TestCaseSource(nameof(Targets))]
    public void ViewPlacedWhileDetachedIsAppliedOnReattach(KsAccessoryTarget target)
    {
        Fixture fixture = Fixture.Connected();
        fixture.Scope.Attach();
        fixture.View.ReleaseHost();
        fixture.Reset();

        Label accessory = new();
        fixture.SetView(target, accessory);
        Assert.That(fixture.Scope.All<GatewayCall.UpdateAccessoryView>(), Is.Empty);

        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        Assert.That(
            fixture.Scope.All<GatewayCall.UpdateAccessoryView>(),
            Has.One.Matches<GatewayCall.UpdateAccessoryView>(
                call => call.Target == target
                    && ReferenceEquals(call.View, fixture.Scope.Views.LatestFor(accessory).PlatformView)));
    }

    // ---- Section の輸送 ----

    /// <summary>実体化済みの View は、Section を輸送するときの引き当てに載る。</summary>
    [Test]
    public void MaterializedSectionAccessoryIsAvailableForSectionTransport()
    {
        Fixture fixture = Fixture.Connected();
        Label header = new();
        Label footer = new();
        fixture.Section.HeaderView = header;
        fixture.Section.FooterView = footer;

        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader),
            Is.SameAs(fixture.Scope.Views.LatestFor(header).PlatformView));
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionFooter),
            Is.SameAs(fixture.Scope.Views.LatestFor(footer).PlatformView));
    }

    /// <summary>Section を取り除くと、その accessory の実体は破棄され引き当ても消える。</summary>
    [Test]
    public void RemovingASectionDisposesItsAccessoryViews()
    {
        Fixture fixture = Fixture.Connected();
        Label header = new();
        fixture.Section.HeaderView = header;
        FakeViewLease lease = fixture.Scope.Views.LatestFor(header);

        fixture.View.Root.Remove(fixture.Section);

        Assert.That(lease.IsDisposed, Is.True);
        Assert.That(
            fixture.Scope.Gateway.AccessoryViewOf(fixture.Section, KsAccessoryTarget.SectionHeader),
            Is.Null);
    }

    /// <summary>設定ツリー全体を作り直すと、accessory の実体は作り直される。</summary>
    [Test]
    public void RebuildingTheRootRebuildsSectionAccessoryViews()
    {
        Fixture fixture = Fixture.Connected();
        Label header = new();
        fixture.Section.HeaderView = header;
        FakeViewLease first = fixture.Scope.Views.LatestFor(header);

        fixture.View.Root = new ObservableCollection<Section> { fixture.Section };

        Assert.That(first.IsDisposed, Is.True);
        Assert.That(fixture.Scope.Views.CountFor(header), Is.EqualTo(2));
    }

    /// <summary>
    /// 設定ツリーを作り直しても、作り直した後の accessory の Handler は接続されたままになる。
    /// </summary>
    /// <remarks>
    /// 同じ Section を含む新しい Root を設定すると、同じ View が実体化し直される。Handler は
    /// View と 1 対 1 のため、旧実体の後片付けを新実体より後に回すと、作ったばかりの実体が
    /// 抱える Handler まで切れてしまう。
    /// </remarks>
    [Test]
    public void RebuildingTheRootKeepsTheNewAccessoryHandlerConnected()
    {
        Fixture fixture = Fixture.Connected();
        Label header = new();
        fixture.Section.HeaderView = header;

        fixture.View.Root = new ObservableCollection<Section> { fixture.Section };

        FakeViewLease latest = fixture.Scope.Views.LatestFor(header);
        Assert.That(latest.IsDisposed, Is.False);
        Assert.That(latest.Handler.IsConnected, Is.True);
    }

    /// <summary>Host を取り付け直した後も、accessory の Handler は接続されたままになる。</summary>
    [Test]
    public void ReattachingTheHostKeepsTheNewAccessoryHandlerConnected()
    {
        Fixture fixture = Fixture.Connected();
        Label header = new();
        fixture.Section.HeaderView = header;
        fixture.Scope.Attach();

        fixture.View.ReleaseHost();
        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        FakeViewLease latest = fixture.Scope.Views.LatestFor(header);
        Assert.That(latest.IsDisposed, Is.False);
        Assert.That(latest.Handler.IsConnected, Is.True);
    }

    // ---- 論理上の所有 (Native Host の有無と無関係) ----

    /// <summary>Native Host が無くても、置いた View は所有者の BindingContext を継承する。</summary>
    [Test]
    public void AccessoryInheritsTheBindingContextWithoutAHost()
    {
        Section section = new();
        SettingsView view = new() { Root = { section } };
        view.BindingContext = new Owner("no host");

        Label rootAccessory = new();
        rootAccessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        Label sectionAccessory = new();
        sectionAccessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);

        view.RootHeaderView = rootAccessory;
        section.HeaderView = sectionAccessory;

        Assert.That(rootAccessory.Parent, Is.SameAs(view));
        Assert.That(sectionAccessory.Parent, Is.SameAs(section));
        Assert.That(rootAccessory.Text, Is.EqualTo("no host"));
        Assert.That(sectionAccessory.Text, Is.EqualTo("no host"));
    }

    /// <summary>Native Host を手放している間の BindingContext の変更も accessory へ伝わる。</summary>
    [Test]
    public void BindingContextChangeReachesTheAccessoryWhileTheHostIsReleased()
    {
        Fixture fixture = Fixture.Connected();
        Label rootAccessory = new();
        rootAccessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        Label sectionAccessory = new();
        sectionAccessory.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        fixture.View.RootHeaderView = rootAccessory;
        fixture.Section.HeaderView = sectionAccessory;
        fixture.View.BindingContext = new Owner("first");
        fixture.Scope.Attach();

        fixture.View.ReleaseHost();
        fixture.View.BindingContext = new Owner("second");

        Assert.That(rootAccessory.Text, Is.EqualTo("second"));
        Assert.That(sectionAccessory.Text, Is.EqualTo("second"));
    }

    /// <summary>Native Host を手放しても、accessory は論理ツリーに残る。</summary>
    [Test]
    public void AccessoryStaysInTheLogicalTreeWhileTheHostIsReleased()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.Section.HeaderView = accessory;
        fixture.Scope.Attach();

        fixture.View.ReleaseHost();

        Assert.That(fixture.Scope.Views.LatestFor(accessory).IsDisposed, Is.True);
        Assert.That(accessory.Parent, Is.SameAs(fixture.Section));
    }

    /// <summary>View を外すと論理ツリーからも外れる。</summary>
    [Test]
    public void ClearingTheViewRemovesItFromTheLogicalTree()
    {
        Fixture fixture = Fixture.Connected();
        Label accessory = new();
        fixture.Section.HeaderView = accessory;

        fixture.Section.HeaderView = null;

        Assert.That(accessory.Parent, Is.Null);
    }

    /// <summary>足場: 接続済みの SettingsView と、位置ごとの操作をまとめる。</summary>
    private sealed class Fixture
    {
        private Fixture(SettingsView view, Section section, GatewayScope scope)
        {
            View = view;
            Section = section;
            Scope = scope;
            SectionId = view.Controller.FindSectionId(section)!;
        }

        public SettingsView View { get; }

        public Section Section { get; }

        public GatewayScope Scope { get; }

        /// <summary>gateway が採番した Section の ID。</summary>
        public string SectionId { get; }

        /// <summary>Section を 1 件持つ SettingsView へ fake gateway を接続する。</summary>
        public static Fixture Connected()
        {
            Section section = new();
            SettingsView view = new() { Root = { section } };
            return new Fixture(view, section, GatewayScope.Connect(view));
        }

        /// <summary>ここまでの記録を捨てる。</summary>
        public void Reset() => Scope.Reset();

        /// <summary>この位置を配信するときに渡される Section の ID。root 対象では null。</summary>
        /// <param name="target">対象の位置</param>
        public string? SectionIdOf(KsAccessoryTarget target)
            => IsSectionTarget(target) ? SectionId : null;

        /// <summary>この位置の accessory を論理上所有する要素。</summary>
        /// <param name="target">対象の位置</param>
        public Element OwnerOf(KsAccessoryTarget target)
            => IsSectionTarget(target) ? Section : View;

        /// <summary>この位置に置かれている View の公開値。</summary>
        /// <param name="target">対象の位置</param>
        public View? ViewOf(KsAccessoryTarget target) => target switch
        {
            KsAccessoryTarget.RootHeader => View.RootHeaderView,
            KsAccessoryTarget.RootFooter => View.RootFooterView,
            KsAccessoryTarget.SectionHeader => Section.HeaderView,
            _ => Section.FooterView,
        };

        /// <summary>この位置に View を置く。</summary>
        /// <param name="target">対象の位置</param>
        /// <param name="value">置く View。null で解除</param>
        public void SetView(KsAccessoryTarget target, View? value)
        {
            switch (target)
            {
                case KsAccessoryTarget.RootHeader:
                    View.RootHeaderView = value;
                    break;
                case KsAccessoryTarget.RootFooter:
                    View.RootFooterView = value;
                    break;
                case KsAccessoryTarget.SectionHeader:
                    Section.HeaderView = value;
                    break;
                default:
                    Section.FooterView = value;
                    break;
            }
        }

        /// <summary>この位置にテキストを設定する。</summary>
        /// <param name="target">対象の位置</param>
        /// <param name="value">設定するテキスト</param>
        public void SetText(KsAccessoryTarget target, string? value)
        {
            switch (target)
            {
                case KsAccessoryTarget.RootHeader:
                    View.RootHeaderText = value;
                    break;
                case KsAccessoryTarget.RootFooter:
                    View.RootFooterText = value;
                    break;
                case KsAccessoryTarget.SectionHeader:
                    Section.HeaderText = value;
                    break;
                default:
                    Section.FooterText = value;
                    break;
            }
        }

        private static bool IsSectionTarget(KsAccessoryTarget target)
            => target is KsAccessoryTarget.SectionHeader or KsAccessoryTarget.SectionFooter;
    }
}
