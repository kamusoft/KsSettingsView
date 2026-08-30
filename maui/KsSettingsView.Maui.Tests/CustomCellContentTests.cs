using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using KsSettingsView.Maui.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// CustomCell の内容に置いた View の実体化・所有・寿命を確認する。
/// </summary>
/// <remarks>
/// 内容の変化は live な実体が描き替えるため、送り直しが起きるのは View そのものを入れ替えたとき
/// だけになる。その境目を「内容の世代が変わったかどうか」と「実体を作り直したかどうか」の
/// 両面から見る。
/// </remarks>
[TestFixture]
public class CustomCellContentTests
{
    // ---- 実体化と輸送 ----

    /// <summary>置いた内容は Host の取り付け後に実体化され、輸送の引き当てに載る。</summary>
    [Test]
    public void ContentIsMaterializedAndAvailableForTransportAfterAttach()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);

        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
        Assert.That(fixture.Token, Is.Not.Empty);
    }

    /// <summary>Host の取り付け前は実体化を待ち、内容の配信も起きない。</summary>
    [Test]
    public void ContentIsNotDeliveredBeforeTheHostIsAttached()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
        Assert.That(scope.Gateway.CellContentViewOf(cell), Is.Null);

        scope.Attach();

        Assert.That(
            scope.Single<GatewayCall.ReplaceCell>().ContentView,
            Is.SameAs(scope.Views.LatestFor(content).PlatformView));
    }

    /// <summary>配信された内容更新には、その時点の世代と実体が載る。</summary>
    [Test]
    public void DeliveredUpdateCarriesTheCurrentGenerationAndInstance()
    {
        Fixture fixture = Fixture.Connected(new Label());
        Label replacement = new();
        fixture.Scope.Reset();

        fixture.Cell.Content = replacement;

        GatewayCall.ReplaceCell call = fixture.Scope.Single<GatewayCall.ReplaceCell>();
        Assert.That(
            ((KsCustomCellSnapshot)call.Snapshot).ContentToken,
            Is.EqualTo(fixture.Token));
        Assert.That(call.ContentView, Is.SameAs(fixture.Scope.Views.LatestFor(replacement).PlatformView));
    }

    /// <summary>
    /// Host の取り付けで複数 Cell の内容を配信するときは、1 回の一括更新にまとめて送る。
    /// </summary>
    /// <remarks>
    /// 1 件ずつ送ると Android では先行する更新の反映通知が後続に追い越されて破棄され、内容の
    /// View を持たない世代のまま行が残る。
    /// </remarks>
    [Test]
    public void AttachDeliversEveryContentInASingleBatch()
    {
        CustomCell first = new() { Content = new Label() };
        CustomCell second = new() { Content = new Label() };
        CustomCell third = new() { Content = new Label() };
        Section section = new() { Cells = { first, second, third } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        scope.Attach();

        GatewayCall.ReplaceCells batch = scope.Single<GatewayCall.ReplaceCells>();
        Assert.That(batch.Updates.Count, Is.EqualTo(3));
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);

        // バッチの各件が、呼び出しの時点でその行の世代と実体を載せていたことまで見る。
        foreach (CustomCell cell in new[] { first, second, third })
        {
            GatewayCall.CellUpdate update = UpdateFor(batch, scope, cell);
            Assert.That(
                ((KsCustomCellSnapshot)update.Snapshot).ContentToken,
                Is.EqualTo(Token(cell)));
            Assert.That(
                update.ContentView,
                Is.SameAs(scope.Views.LatestFor(cell.Content!).PlatformView));
        }
    }

    /// <summary>Native Host の解放で送り直す内容なしの世代も、1 回の一括更新にまとめて送る。</summary>
    [Test]
    public void HostReleaseDeliversEveryContentInASingleBatch()
    {
        CustomCell first = new() { Content = new Label() };
        CustomCell second = new() { Content = new Label() };
        Section section = new() { Cells = { first, second } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();
        string firstToken = Token(first);
        string secondToken = Token(second);
        scope.Reset();

        view.ReleaseHost();

        GatewayCall.ReplaceCells batch = scope.Single<GatewayCall.ReplaceCells>();
        Assert.That(batch.Updates.Count, Is.EqualTo(2));
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);

        // 退役した実体を指したままにしないため、各件が実体なしの新しい世代で送られている。
        Assert.That(UpdateFor(batch, scope, first).ContentView, Is.Null);
        Assert.That(UpdateFor(batch, scope, second).ContentView, Is.Null);
        Assert.That(
            ((KsCustomCellSnapshot)UpdateFor(batch, scope, first).Snapshot).ContentToken,
            Is.Not.EqualTo(firstToken));
        Assert.That(
            ((KsCustomCellSnapshot)UpdateFor(batch, scope, second).Snapshot).ContentToken,
            Is.Not.EqualTo(secondToken));
    }

    // ---- 内容の live 更新 ----

    /// <summary>内容の中の変化は送り直しを起こさず、実体も世代もそのまま保たれる。</summary>
    [Test]
    public void InnerContentChangeDoesNotReissueTheContent()
    {
        Label content = new();
        content.SetBinding(Label.TextProperty, static (Owner value) => value.Name);
        Fixture fixture = Fixture.Connected(content);
        fixture.View.BindingContext = new Owner("first");
        Assert.That(content.Text, Is.EqualTo("first"));

        string token = fixture.Token;
        fixture.Scope.Reset();

        fixture.View.BindingContext = new Owner("second");
        fixture.Scope.Flush();

        Assert.That(content.Text, Is.EqualTo("second"));
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
        Assert.That(fixture.Token, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.CountFor(content), Is.EqualTo(1));
        Assert.That(fixture.Scope.Views.LatestFor(content).IsDisposed, Is.False);
    }

    /// <summary>必要サイズの変化も送り直しにはならない。</summary>
    [Test]
    public void MeasureInvalidationDoesNotReissueTheContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        string token = fixture.Token;
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);
        fixture.Scope.Reset();

        lease.RaiseMeasureInvalidated();
        lease.RaiseMeasureInvalidated();
        fixture.Scope.Flush();

        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
        Assert.That(fixture.Token, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.LatestFor(content), Is.SameAs(lease));
    }

    // ---- 差し替え ----

    /// <summary>別の View への差し替えは、新しい世代と実体で送り直される。</summary>
    [Test]
    public void ReplacingContentReissuesWithANewGeneration()
    {
        Label first = new();
        Label second = new();
        Fixture fixture = Fixture.Connected(first);
        string token = fixture.Token;
        FakeViewLease firstLease = fixture.Scope.Views.LatestFor(first);
        fixture.Scope.Reset();

        fixture.Cell.Content = second;

        Assert.That(fixture.Token, Is.Not.EqualTo(token));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(second).PlatformView));
        Assert.That(firstLease.IsDisposed, Is.True);
        Assert.That(first.Parent, Is.Null);
    }

    /// <summary>旧実体の破棄は、新しい内容が配信された後に行われる。</summary>
    [Test]
    public void PreviousContentIsDisposedAfterTheNewOneIsDelivered()
    {
        Label first = new();
        Fixture fixture = Fixture.Connected(first);
        fixture.Scope.Reset();

        int deliveredAtDispose = -1;
        fixture.Scope.Views.LatestFor(first).OnDispose =
            () => deliveredAtDispose = fixture.Scope.All<GatewayCall.ReplaceCell>().Count;

        fixture.Cell.Content = new Label();

        Assert.That(deliveredAtDispose, Is.EqualTo(1));
    }

    /// <summary>同じ View への往復差し替えでも、作り直した実体の Handler は接続されたままになる。</summary>
    [Test]
    public void SwappingContentBackAndForthKeepsTheNewHandlerConnected()
    {
        Label first = new();
        Label second = new();
        Fixture fixture = Fixture.Connected(first);

        fixture.Cell.Content = second;
        fixture.Cell.Content = first;

        FakeViewLease latest = fixture.Scope.Views.LatestFor(first);
        Assert.That(fixture.Scope.Views.CountFor(first), Is.EqualTo(2));
        Assert.That(latest.IsDisposed, Is.False);
        Assert.That(latest.Handler.IsConnected, Is.True);
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(latest.PlatformView));
    }

    /// <summary>内容を外すと空の行になり、外した View は別の行で使い直せる。</summary>
    [Test]
    public void ClearingContentEmptiesTheRowAndFreesTheView()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        string token = fixture.Token;
        fixture.Scope.Reset();

        fixture.Cell.Content = null;

        Assert.That(fixture.Scope.Gateway.CellContentViewOf(fixture.Cell), Is.Null);
        Assert.That(fixture.Scope.Single<GatewayCall.ReplaceCell>().ContentView, Is.Null);
        Assert.That(fixture.Token, Is.Not.EqualTo(token));
        Assert.That(content.Parent, Is.Null);

        CustomCell other = new();
        fixture.Section.Cells.Add(other);
        other.Content = content;

        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(other),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    // ---- 構造的な除去 ----

    /// <summary>Cell を取り除くと内容は解放され、その View は別の行で使い直せる。</summary>
    [Test]
    public void RemovingTheCellReleasesItsContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);

        fixture.Section.Cells.Remove(fixture.Cell);

        Assert.That(lease.IsDisposed, Is.True);
        Assert.That(fixture.Scope.Gateway.CellContentViewOf(fixture.Cell), Is.Null);
        Assert.That(content.Parent, Is.Null);

        CustomCell other = new() { Content = content };
        fixture.Section.Cells.Add(other);

        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(other),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    /// <summary>Cell を置き換えると、置き換えられた側の内容が解放される。</summary>
    [Test]
    public void ReplacingTheCellReleasesTheOldContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);

        fixture.Section.Cells[0] = new LabelCell();

        Assert.That(lease.IsDisposed, Is.True);
        Assert.That(content.Parent, Is.Null);
    }

    /// <summary>Section を取り除くと、配下の Cell の内容が解放される。</summary>
    [Test]
    public void RemovingTheSectionReleasesCellContents()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);

        fixture.View.Root.Remove(fixture.Section);

        Assert.That(lease.IsDisposed, Is.True);
        Assert.That(fixture.Scope.Gateway.CellContentViewOf(fixture.Cell), Is.Null);
        Assert.That(content.Parent, Is.Null);
    }

    /// <summary>Cell コレクションを差し替えると、外れた Cell の内容が解放される。</summary>
    [Test]
    public void ResettingTheCellCollectionReleasesCellContents()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);

        fixture.Section.Cells = new ObservableCollection<CellBase> { new LabelCell() };

        Assert.That(lease.IsDisposed, Is.True);
        Assert.That(content.Parent, Is.Null);
    }

    /// <summary>ItemsSource から項目を外すと、その行と内容が解放され、残る行はそのまま残る。</summary>
    [Test]
    public void RemovingAnItemReleasesTheGeneratedRowAndKeepsTheRest()
    {
        ObservableCollection<Owner> items = [new Owner("first"), new Owner("second")];
        Section section = new()
        {
            ItemsSource = items,
            ItemTemplate = new DataTemplate(() => new CustomCell { Content = new Label() }),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        CustomCell removed = (CustomCell)section.Cells[0];
        CustomCell kept = (CustomCell)section.Cells[1];
        FakeViewLease removedLease = scope.Views.LatestFor(removed.Content!);
        object? keptView = scope.Gateway.CellContentViewOf(kept);
        scope.Reset();

        items.RemoveAt(0);

        Assert.That(scope.All<GatewayCall.RemoveCell>(), Has.Count.EqualTo(1));
        Assert.That(removedLease.IsDisposed, Is.True);
        Assert.That(scope.Gateway.CellContentViewOf(removed), Is.Null);
        Assert.That(scope.Gateway.CellContentViewOf(kept), Is.SameAs(keptView));
    }

    /// <summary>設定ツリーを作り直しても、作り直した後の実体の Handler は接続されたままになる。</summary>
    [Test]
    public void RebuildingTheRootKeepsTheNewContentHandlerConnected()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease first = fixture.Scope.Views.LatestFor(content);

        fixture.View.Root = new ObservableCollection<Section> { fixture.Section };

        FakeViewLease latest = fixture.Scope.Views.LatestFor(content);
        Assert.That(first.IsDisposed, Is.True);
        Assert.That(fixture.Scope.Views.CountFor(content), Is.EqualTo(2));
        Assert.That(latest.IsDisposed, Is.False);
        Assert.That(latest.Handler.IsConnected, Is.True);
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
    }

    /// <summary>1 件の内容の後片付けが失敗しても、退役した残りは破棄され取りこぼされない。</summary>
    /// <remarks>
    /// 待ち行列は破棄の前に空にされるため、途中で例外が抜けると残りの実体は誰からも破棄されなくなる。
    /// 破棄の順に依らず取りこぼしを検出できるよう、2 件を失敗させて全件の試行を観測する。
    /// </remarks>
    [Test]
    public void FailingContentDisposalDoesNotStrandTheRemainingRetiredViews()
    {
        Label first = new();
        Label second = new();
        Label third = new();
        Section section = new()
        {
            Cells =
            {
                new CustomCell { Content = first },
                new CustomCell { Content = second },
                new CustomCell { Content = third },
            },
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        FakeViewLease firstLease = scope.Views.LatestFor(first);
        FakeViewLease secondLease = scope.Views.LatestFor(second);
        FakeViewLease thirdLease = scope.Views.LatestFor(third);

        firstLease.OnDispose = () => throw new InvalidOperationException("first disposal failed");
        secondLease.OnDispose = () => throw new InvalidOperationException("second disposal failed");

        AggregateException? thrown = Assert.Throws<AggregateException>(
            () => view.Root = new ObservableCollection<Section>());

        Assert.Multiple(() =>
        {
            Assert.That(thrown!.InnerExceptions, Has.Count.EqualTo(2));
            Assert.That(firstLease.DisposeCount, Is.EqualTo(1));
            Assert.That(secondLease.DisposeCount, Is.EqualTo(1));
            Assert.That(thirdLease.DisposeCount, Is.EqualTo(1));
        });
    }

    // ---- BindingContext の継承 ----

    /// <summary>内容は所有する Cell の BindingContext を継承する。</summary>
    [Test]
    public void ContentInheritsTheOwningCellBindingContext()
    {
        Label content = new();
        content.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        Fixture fixture = Fixture.Connected(content);

        fixture.View.BindingContext = new Owner("view context");

        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
        Assert.That(content.Text, Is.EqualTo("view context"));
    }

    /// <summary>所有者の BindingContext の変更は内容へ伝わる。</summary>
    [Test]
    public void BindingContextChangePropagatesToTheContent()
    {
        Label content = new();
        content.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        Fixture fixture = Fixture.Connected(content);
        fixture.View.BindingContext = new Owner("first");

        fixture.View.BindingContext = new Owner("second");

        Assert.That(content.Text, Is.EqualTo("second"));
    }

    /// <summary>明示的に設定された BindingContext は継承で上書きされない。</summary>
    [Test]
    public void ExplicitBindingContextIsNotOverwritten()
    {
        Label content = new() { BindingContext = new Owner("explicit") };
        content.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
        Fixture fixture = Fixture.Connected(content);

        fixture.View.BindingContext = new Owner("inherited");

        Assert.That(content.Text, Is.EqualTo("explicit"));
    }

    /// <summary>テンプレートから生成した行の内容は、それぞれの item を解決する。</summary>
    [Test]
    public void GeneratedRowsResolveTheirOwnItem()
    {
        ObservableCollection<Owner> items = [new Owner("first"), new Owner("second")];
        Section section = new()
        {
            ItemsSource = items,
            ItemTemplate = new DataTemplate(() =>
            {
                Label label = new();
                label.SetBinding(Label.TextProperty, static (Owner owner) => owner.Name);
                return new CustomCell { Content = label };
            }),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        CustomCell first = (CustomCell)section.Cells[0];
        CustomCell second = (CustomCell)section.Cells[1];

        Assert.That(((Label)first.Content!).Text, Is.EqualTo("first"));
        Assert.That(((Label)second.Content!).Text, Is.EqualTo("second"));
        Assert.That(
            scope.Gateway.CellContentViewOf(first),
            Is.Not.SameAs(scope.Gateway.CellContentViewOf(second)));
    }

    /// <summary>テンプレートから生成した行は、それぞれ別の内容として世代も実体も独立に持つ。</summary>
    [Test]
    public void GeneratedRowsHoldIndependentContent()
    {
        ObservableCollection<Owner> items = [new Owner("first"), new Owner("second")];
        Section section = new()
        {
            ItemsSource = items,
            ItemTemplate = new DataTemplate(() => new CustomCell { Content = new Label() }),
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        CustomCell first = (CustomCell)section.Cells[0];
        CustomCell second = (CustomCell)section.Cells[1];
        Label secondContent = (Label)second.Content!;

        Assert.That(first.Content, Is.Not.SameAs(second.Content));
        Assert.That(Token(first), Is.Not.EqualTo(Token(second)));
        Assert.That(
            scope.Views.LatestFor(first.Content!),
            Is.Not.SameAs(scope.Views.LatestFor(secondContent)));

        scope.Reset();
        Label replacement = new();
        first.Content = replacement;

        Assert.That(
            scope.Gateway.CellContentViewOf(first),
            Is.SameAs(scope.Views.LatestFor(replacement).PlatformView));
        Assert.That(
            scope.Gateway.CellContentViewOf(second),
            Is.SameAs(scope.Views.LatestFor(secondContent).PlatformView));
        Assert.That(scope.Views.CountFor(secondContent), Is.EqualTo(1));
        Assert.That(scope.Views.LatestFor(secondContent).IsDisposed, Is.False);
    }

    // ---- 多重配置 ----

    /// <summary>同じ View を 2 つの Cell の内容へ置くと例外になる。</summary>
    [Test]
    public void PlacingTheSameViewInTwoCellsThrows()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        CustomCell other = new();
        fixture.Section.Cells.Add(other);

        Assert.Throws<InvalidOperationException>(() => other.Content = content);

        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    /// <summary>accessory に置かれている View を内容へ置くと例外になる。</summary>
    [Test]
    public void PlacingAnAccessoryViewAsContentThrows()
    {
        Fixture fixture = Fixture.Connected(new Label());
        Label accessory = new();
        fixture.View.RootHeaderView = accessory;

        Assert.Throws<InvalidOperationException>(() => fixture.Cell.Content = accessory);

        Assert.That(accessory.Parent, Is.SameAs(fixture.View));
    }

    /// <summary>内容に置かれている View を accessory へ置くと例外になる。</summary>
    [Test]
    public void PlacingAContentViewAsAccessoryThrows()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);

        Assert.Throws<InvalidOperationException>(() => fixture.Section.HeaderView = content);

        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
    }

    /// <summary>置かれている View を使い回す Cell を後から追加すると例外になる。</summary>
    [Test]
    public void AddingACellThatReusesAPlacedViewThrows()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);

        // Cell の組み立ては例外にならない — 置かれている View は引き取られないだけで、
        // 多重配置として弾かれるのは設定ツリーへ入れる時点になる。
        CustomCell added = new() { Content = content };
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));

        Assert.Throws<InvalidOperationException>(() => fixture.Section.Cells.Add(added));

        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
    }

    /// <summary>作り直す設定ツリーの中に同じ View が 2 つあると、作り直す前に例外になる。</summary>
    [Test]
    public void RebuildingWithADuplicateContentThrowsWithoutTouchingTheCurrentTree()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);

        CustomCell first = new() { Content = content };
        CustomCell second = new() { Content = content };

        Assert.Throws<InvalidOperationException>(
            () => fixture.View.Root = new ObservableCollection<Section>
            {
                new() { Cells = { first } },
                new() { Cells = { second } },
            });

        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
    }

    /// <summary>
    /// 同じ View を内容に持つ 2 つの Cell を 1 度に追加すると、1 件も入れないまま例外になる。
    /// </summary>
    [Test]
    public void AddingCellsThatShareAContentViewThrowsBeforeAnyInsert()
    {
        Label content = new();
        CustomCell placed = new() { Content = content };
        RangeAddCollection<CellBase> cells = [placed];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        string sectionId = scope.Gateway.SectionIds[0];
        IReadOnlyList<string> cellIds = scope.Gateway.CellIdsOf(sectionId);
        string placedId = view.Controller.FindCellId(placed)!;
        FakeViewLease lease = scope.Views.LatestFor(content);
        scope.Reset();

        Label shared = new();
        CustomCell first = new() { Content = shared };
        CustomCell second = new() { Content = shared };

        Assert.Throws<InvalidOperationException>(() => cells.AddRange(first, second));

        // native も対応表も、追加の途中まで進んだ形にならない。
        Assert.That(scope.Gateway.Calls, Is.Empty);
        Assert.That(scope.Gateway.CellIdsOf(sectionId), Is.EqualTo(cellIds));
        Assert.That(view.Controller.FindCellId(first), Is.Null);
        Assert.That(view.Controller.FindCellId(second), Is.Null);
        Assert.That(scope.Views.CountFor(shared), Is.Zero);

        // 置かれていた行はそのまま残る。
        Assert.That(view.Controller.FindCellId(placed), Is.EqualTo(placedId));
        Assert.That(scope.Views.LatestFor(content), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(scope.Gateway.CellContentViewOf(placed), Is.SameAs(lease.PlatformView));
        Assert.That(content.Parent, Is.SameAs(placed));
    }

    /// <summary>
    /// 同じ View を内容に持つ 2 つの Cell へ差し替えると、今の行に触れないまま例外になる。
    /// </summary>
    [Test]
    public void ResettingTheCellsWithADuplicateContentThrowsWithoutTouchingTheCurrentCells()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        string sectionId = fixture.Scope.Gateway.SectionIds[0];
        IReadOnlyList<string> cellIds = fixture.Scope.Gateway.CellIdsOf(sectionId);
        string cellId = fixture.View.Controller.FindCellId(fixture.Cell)!;
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);
        string token = fixture.Token;
        fixture.Scope.Reset();

        Label shared = new();
        CustomCell first = new() { Content = shared };
        CustomCell second = new() { Content = shared };

        Assert.Throws<InvalidOperationException>(
            () => fixture.Section.Cells = new ObservableCollection<CellBase> { first, second });

        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
        Assert.That(fixture.Scope.Gateway.CellIdsOf(sectionId), Is.EqualTo(cellIds));
        Assert.That(fixture.View.Controller.FindCellId(first), Is.Null);
        Assert.That(fixture.View.Controller.FindCellId(second), Is.Null);
        Assert.That(fixture.Scope.Views.CountFor(shared), Is.Zero);

        Assert.That(fixture.View.Controller.FindCellId(fixture.Cell), Is.EqualTo(cellId));
        Assert.That(fixture.Token, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.LatestFor(content), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(lease.PlatformView));
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
    }

    /// <summary>他所で使われている View への差し替えに失敗しても、今の内容がそのまま残る。</summary>
    [Test]
    public void AFailedContentReplacementKeepsTheCurrentContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        Label otherContent = new();
        CustomCell other = new() { Content = otherContent };
        fixture.Section.Cells.Add(other);
        FakeViewLease lease = fixture.Scope.Views.LatestFor(content);
        string token = fixture.Token;
        fixture.Scope.Reset();

        Assert.Throws<InvalidOperationException>(() => fixture.Cell.Content = otherContent);

        // 公開値・論理上の所有・実体・世代のいずれも変更前のまま。
        Assert.That(fixture.Cell.Content, Is.SameAs(content));
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
        Assert.That(fixture.Token, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.LatestFor(content), Is.SameAs(lease));
        Assert.That(lease.IsDisposed, Is.False);
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(lease.PlatformView));
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);

        // 置かれている側も無傷で残る。
        Assert.That(otherContent.Parent, Is.SameAs(other));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(other),
            Is.SameAs(fixture.Scope.Views.LatestFor(otherContent).PlatformView));
    }

    /// <summary>accessory の View への差し替えに失敗しても、今の内容がそのまま残る。</summary>
    [Test]
    public void AFailedContentReplacementWithAnAccessoryViewKeepsTheCurrentContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        Label accessory = new();
        fixture.View.RootHeaderView = accessory;
        string token = fixture.Token;
        fixture.Scope.Reset();

        Assert.Throws<InvalidOperationException>(() => fixture.Cell.Content = accessory);

        Assert.That(fixture.Cell.Content, Is.SameAs(content));
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
        Assert.That(fixture.Token, Is.EqualTo(token));
        Assert.That(accessory.Parent, Is.SameAs(fixture.View));
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>設定ツリーから外れた Cell は、置き場所の検査を受けない Cell に戻る。</summary>
    [Test]
    public void ACellDroppedByARootRebuildStopsConsultingThePlacement()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        CustomCell dropped = new();
        fixture.View.Root.Add(new Section { Cells = { dropped } });

        // 作り直しで 2 つ目の Section ごと設定ツリーから外す。
        fixture.View.Root = new ObservableCollection<Section> { fixture.Section };

        // 外れた Cell には検査を行う相手がいないため、置かれている View を設定しても例外に
        // ならず、引き取られないだけになる。
        Assert.DoesNotThrow(() => dropped.Content = content);
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    /// <summary>外した View は別の Cell へ置き直せる。</summary>
    [Test]
    public void ReleasedViewCanBePlacedAgain()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        fixture.Cell.Content = null;

        CustomCell other = new();
        fixture.Section.Cells.Add(other);
        other.Content = content;

        Assert.That(content.Parent, Is.SameAs(other));
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(other),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    // ---- Native Host の世代 ----

    /// <summary>Host を手放すと実体は破棄され、取り付け直しで作り直される。</summary>
    [Test]
    public void ContentIsRebuiltForANewHost()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        FakeViewLease first = fixture.Scope.Views.LatestFor(content);
        string token = fixture.Token;

        fixture.View.ReleaseHost();
        Assert.That(first.IsDisposed, Is.True);
        Assert.That(content.Parent, Is.SameAs(fixture.Cell));

        fixture.Scope.Reset();
        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        FakeViewLease second = fixture.Scope.Views.LatestFor(content);
        Assert.That(second, Is.Not.SameAs(first));
        Assert.That(second.Handler.IsConnected, Is.True);
        Assert.That(fixture.Token, Is.Not.EqualTo(token));
        Assert.That(
            fixture.Scope.Single<GatewayCall.ReplaceCell>().ContentView,
            Is.SameAs(second.PlatformView));
    }

    /// <summary>Host を手放すとき、退役した実体を指したままにならないよう内容なしで送り直す。</summary>
    [Test]
    public void ReleasingTheHostClearsTheDeliveredContent()
    {
        Label content = new();
        Fixture fixture = Fixture.Connected(content);
        string token = fixture.Token;
        fixture.Scope.Reset();

        fixture.View.ReleaseHost();

        GatewayCall.ReplaceCell call = fixture.Scope.Single<GatewayCall.ReplaceCell>();
        Assert.That(call.ContentView, Is.Null);
        Assert.That(((KsCustomCellSnapshot)call.Snapshot).ContentToken, Is.Not.EqualTo(token));
    }

    /// <summary>切断中に差し替えた内容は、取り付け直したときの表示に反映される。</summary>
    [Test]
    public void ContentReplacedWhileDetachedIsAppliedOnReattach()
    {
        Fixture fixture = Fixture.Connected(new Label());
        fixture.View.ReleaseHost();
        fixture.Scope.Reset();

        Label replacement = new();
        fixture.Cell.Content = replacement;
        Assert.That(fixture.Scope.Views.CountFor(replacement), Is.Zero);

        fixture.Scope.Reconnect();
        fixture.Scope.Attach();

        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(fixture.Cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(replacement).PlatformView));
    }

    /// <summary>指定した Cell を今この時点で輸送するときの内容の世代。</summary>
    /// <param name="cell">対象の Cell</param>
    private static string Token(CustomCell cell)
        => ((KsCustomCellSnapshot)cell.CreateSnapshot()).ContentToken;

    /// <summary>バッチの中から、指定した Cell に対する更新を ID で引き当てる。</summary>
    /// <param name="batch">対象のバッチ</param>
    /// <param name="scope">配信を受けた足場</param>
    /// <param name="cell">対象の Cell</param>
    private static GatewayCall.CellUpdate UpdateFor(
        GatewayCall.ReplaceCells batch,
        GatewayScope scope,
        CustomCell cell)
    {
        string cellId = scope.View.Controller.FindCellId(cell)!;
        GatewayCall.CellUpdate update = batch.Updates.Single(item => item.CellId == cellId);
        Assert.That(update.NewCell, Is.SameAs(cell));
        return update;
    }

    /// <summary>CustomCell 1 件を配置し、Native Host まで取り付けた足場。</summary>
    private sealed class Fixture
    {
        private Fixture(SettingsView view, Section section, CustomCell cell, GatewayScope scope)
        {
            View = view;
            Section = section;
            Cell = cell;
            Scope = scope;
        }

        /// <summary>接続した SettingsView。</summary>
        public SettingsView View { get; }

        /// <summary>Cell を持つ Section。</summary>
        public Section Section { get; }

        /// <summary>対象の Cell。</summary>
        public CustomCell Cell { get; }

        /// <summary>接続した足場。</summary>
        public GatewayScope Scope { get; }

        /// <summary>今この時点で輸送するときの内容の世代。</summary>
        public string Token => ((KsCustomCellSnapshot)Cell.CreateSnapshot()).ContentToken;

        /// <summary>指定した View を内容に持つ CustomCell を 1 件配置して接続し、Host を取り付ける。</summary>
        /// <param name="content">内容に置く View</param>
        public static Fixture Connected(View content)
        {
            CustomCell cell = new() { Content = content };
            Section section = new() { Cells = { cell } };
            SettingsView view = new() { Root = { section } };
            GatewayScope scope = GatewayScope.Connect(view);
            scope.Attach();
            return new Fixture(view, section, cell, scope);
        }
    }
}
