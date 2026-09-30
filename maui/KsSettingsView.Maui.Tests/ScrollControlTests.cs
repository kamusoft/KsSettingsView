using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using KsSettingsView.Handlers;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using KsSettingsView.Tests.Support;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// スクロール命令のハンドル・対象の解決・gateway への経路・準備完了のコマンドを検証する。
/// </summary>
/// <remarks>
/// Native Host の世代は GatewayScope の接続・<see cref="SettingsView.ReleaseHost"/>・再接続と、
/// Handler の接続・切断で再現する。
/// </remarks>
[TestFixture]
public sealed class ScrollControlTests
{
    // ---- スクロール命令のインターフェースと位置の公開 ----

    /// <summary>ViewModel は fake 実装で検証でき、既定の引数は位置 Start・アニメーションありになる。</summary>
    [Test]
    public void FakeScrollControllerRecordsDefaultArguments()
    {
        RecordingScrollController fake = new();
        ScrollingViewModel viewModel = new() { Scroll = fake };

        viewModel.ShowLatest();
        viewModel.Scroll!.ScrollTo("a");
        viewModel.Scroll.ScrollToSection("s");
        viewModel.Scroll.ScrollToStart();

        Assert.That(fake.Calls, Is.EqualTo(new[]
        {
            "End animated=True",
            "Cell a Start animated=True",
            "Section s Start animated=True",
            "Start animated=True",
        }));
    }

    /// <summary>位置は Start = 0 / Center = 1 / End = 2 の整数で運ぶ。</summary>
    [Test]
    public void ScrollPositionIsTransportedAsOrdinal()
    {
        Assert.That(KsWireValues.ScrollPosition(ScrollPosition.Start), Is.EqualTo(0));
        Assert.That(KsWireValues.ScrollPosition(ScrollPosition.Center), Is.EqualTo(1));
        Assert.That(KsWireValues.ScrollPosition(ScrollPosition.End), Is.EqualTo(2));
    }

    // ---- SettingsView が作った命令ハンドルを ViewModel へ渡す ----

    /// <summary>XAML の Binding で、ViewModel にその SettingsView の実体が代入される。</summary>
    [Test]
    public void BindingDeliversOwnHandleToViewModel()
    {
        const string xaml = """
            <ks:SettingsView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
                             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
                             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
                             ScrollController="{Binding Scroll}" />
            """;
        ScrollingViewModel viewModel = new();

        SettingsView view = new SettingsView().LoadFromXaml(xaml);
        view.BindingContext = viewModel;

        Assert.That(viewModel.Scroll, Is.Not.Null);
        Assert.That(viewModel.Scroll, Is.SameAs(view.ScrollController));
    }

    /// <summary>BindingContext を先に与えてから Binding を張っても実体が届く。</summary>
    [Test]
    public void BindingAppliedAfterContextDeliversOwnHandle()
    {
        ScrollingViewModel viewModel = new();
        SettingsView view = new() { BindingContext = viewModel };

        view.SetBinding(SettingsView.ScrollControllerProperty, nameof(ScrollingViewModel.Scroll));

        Assert.That(viewModel.Scroll, Is.SameAs(view.ScrollController));
    }

    /// <summary>SettingsView ごとに別の実体を持ち、読み直しても同じ実体が返る。</summary>
    [Test]
    public void EachSettingsViewHasItsOwnHandle()
    {
        SettingsView first = new();
        SettingsView second = new();

        Assert.That(first.ScrollController, Is.Not.Null);
        Assert.That(second.ScrollController, Is.Not.Null);
        Assert.That(first.ScrollController, Is.Not.SameAs(second.ScrollController));
        Assert.That(first.ScrollController, Is.SameAs(first.ScrollController));
    }

    /// <summary>外から代入した値は採用されず、SettingsView 自身の実体のまま変わらない。</summary>
    [Test]
    public void AssignedHandleIsNotAdopted()
    {
        SettingsView view = new();
        IScrollController own = view.ScrollController;

        view.ScrollController = new RecordingScrollController();

        Assert.That(view.ScrollController, Is.SameAs(own));
    }

    /// <summary>ViewModel がハンドルを持ち続けても SettingsView は回収される。</summary>
    [Test]
    public void HandleDoesNotKeepSettingsViewAlive()
    {
        (WeakReference view, IScrollController handle) = CreateViewAndTakeHandle();

        GcProbe.AssertCollected(view, "SettingsView");

        // 持ち主が回収された後の命令は何もしない。
        Assert.DoesNotThrow(() => handle.ScrollToEnd());
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static (WeakReference View, IScrollController Handle) CreateViewAndTakeHandle()
    {
        SettingsView view = new() { Root = { new Section { Cells = { new LabelCell { Title = "a" } } } } };
        GatewayScope.Connect(view);
        return (new WeakReference(view), view.ScrollController);
    }

    // ---- Section と Cell の明示 ID ----

    /// <summary>明示 ID を付けても、Bridge への配信内容は明示 ID 以外同じになる。</summary>
    [Test]
    public void ExplicitIdsDoNotChangeDelivery()
    {
        SettingsView plain = new() { Root = { NumberedSection(sectionId: null, cellId: null) } };
        SettingsView tagged = new() { Root = { NumberedSection(sectionId: "general", cellId: "wifi") } };

        GatewayScope plainScope = GatewayScope.Connect(plain);
        GatewayScope taggedScope = GatewayScope.Connect(tagged);

        Assert.That(Describe(taggedScope.Calls), Is.EqualTo(Describe(plainScope.Calls)));
    }

    /// <summary>表示中に明示 ID を変えても、Bridge へは何も配信されない。</summary>
    [Test]
    public void ChangingExplicitIdsDeliversNothing()
    {
        Section section = NumberedSection(sectionId: null, cellId: null);
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        section.SectionId = "general";
        section.Cells[0].CellId = "wifi";
        scope.Flush();

        Assert.That(scope.Calls, Is.Empty);
    }

    // ---- 命令の対象の解決 ----

    /// <summary>XAML で手で並べた Cell を明示 ID で指せる。</summary>
    [Test]
    public void ExplicitCellIdResolvesManualCell()
    {
        const string xaml = """
            <ks:SettingsView xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
                             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
                             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui">
                <ks:Section HeaderText="ネットワーク">
                    <ks:LabelCell Title="機内モード" />
                    <ks:LabelCell Title="Wi-Fi" CellId="wifi" />
                </ks:Section>
            </ks:SettingsView>
            """;
        SettingsView view = new SettingsView().LoadFromXaml(xaml);
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        CellBase wifi = view.Root[0].Cells[1];

        view.ScrollController.ScrollTo("wifi", ScrollPosition.Center);

        Assert.That(scope.Calls, Is.EqualTo(new GatewayCall[]
        {
            new GatewayCall.ScrollToCell(view.Controller.FindCellId(wifi)!, ScrollPosition.Center, true),
        }));
    }

    /// <summary>手で並べた Section を明示 ID で指せる。</summary>
    [Test]
    public void ExplicitSectionIdResolvesManualSection()
    {
        Section target = new() { SectionId = "notice", HeaderText = "通知" };
        SettingsView view = new() { Root = { new Section { HeaderText = "一般" }, target } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollToSection("notice", ScrollPosition.End, animated: false);

        Assert.That(scope.Calls, Is.EqualTo(new GatewayCall[]
        {
            new GatewayCall.ScrollToSection(view.Controller.FindSectionId(target)!, ScrollPosition.End, false),
        }));
    }

    /// <summary>ItemsSource の項目で、その項目から生成した Cell を指せる。</summary>
    [Test]
    public void ItemResolvesGeneratedCell()
    {
        Item a = new("A");
        Item b = new("B");
        Item c = new("C");
        Section section = new() { ItemsSource = new ObservableCollection<Item> { a, b, c }, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollTo(b);

        Assert.That(scope.Single<GatewayCall.ScrollToCell>().CellId, Is.EqualTo(CellIdOf(view, section, "B")));
    }

    /// <summary>SettingsView の ItemsSource の項目で、その項目から生成した Section を指せる。</summary>
    [Test]
    public void ItemResolvesGeneratedSection()
    {
        Item x = new("X");
        Item y = new("Y");
        SettingsView view = new()
        {
            ItemsSource = new ObservableCollection<Item> { x, y },
            ItemTemplate = SectionTemplate(),
        };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        Section generatedY = view.Root.Single(section => section.HeaderText == "Y");

        view.ScrollController.ScrollToSection(y);

        Assert.That(
            scope.Single<GatewayCall.ScrollToSection>().SectionId,
            Is.EqualTo(view.Controller.FindSectionId(generatedY)));
    }

    /// <summary>明示 ID と項目の両方に当たるときは、明示 ID の要素を採る。</summary>
    [Test]
    public void ExplicitIdWinsOverItem()
    {
        LabelCell p = new() { Title = "P", CellId = "a" };
        Section generated = new() { ItemsSource = new ObservableCollection<string> { "a" }, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { generated, new Section { Cells = { p } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollTo("a");

        Assert.That(scope.Single<GatewayCall.ScrollToCell>().CellId, Is.EqualTo(view.Controller.FindCellId(p)));
    }

    /// <summary>同じ明示 ID の要素が複数あるときは、表示順で最初の要素を採る。</summary>
    [Test]
    public void FirstElementInDisplayOrderIsChosen()
    {
        LabelCell first = new() { Title = "first", CellId = "dup" };
        LabelCell second = new() { Title = "second", CellId = "dup" };
        SettingsView view = new() { Root = { new Section { Cells = { first } }, new Section { Cells = { second } } } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollTo("dup");

        Assert.That(scope.Single<GatewayCall.ScrollToCell>().CellId, Is.EqualTo(view.Controller.FindCellId(first)));
    }

    /// <summary>生成物の BindingContext を差し替えても、元の項目で指せる。</summary>
    [Test]
    public void ReplacingBindingContextKeepsCorrespondence()
    {
        Item a = new("A");
        Item b = new("B");
        Section section = new() { ItemsSource = new ObservableCollection<Item> { a, b }, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        CellBase generatedB = section.Cells.Single(cell => cell.Title == "B");
        string expected = view.Controller.FindCellId(generatedB)!;

        generatedB.BindingContext = new Item("other");
        view.ScrollController.ScrollTo(b);

        Assert.That(scope.Single<GatewayCall.ScrollToCell>().CellId, Is.EqualTo(expected));
    }

    /// <summary>項目の移動と置換の後も、元の項目で指せ、置き換えで消えた項目では命令しない。</summary>
    [Test]
    public void MoveAndReplaceKeepCorrespondence()
    {
        Item a = new("A");
        Item b = new("B");
        Item c = new("C");
        Item d = new("D");
        ObservableCollection<Item> items = [a, b, c];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        items.Move(1, 2);
        items[items.IndexOf(c)] = d;
        scope.Flush();
        scope.Reset();

        view.ScrollController.ScrollTo(c);
        Assert.That(scope.All<GatewayCall.ScrollToCell>(), Is.Empty, "置き換えで消えた項目では命令しない");

        view.ScrollController.ScrollTo(b);
        view.ScrollController.ScrollTo(d);

        Assert.That(
            scope.All<GatewayCall.ScrollToCell>().Select(call => call.CellId),
            Is.EqualTo(new[] { CellIdOf(view, section, "B"), CellIdOf(view, section, "D") }));
    }

    /// <summary>項目の追加・削除・作り直しの後も、生成物との対応が保たれる。</summary>
    [Test]
    public void AddRemoveAndResetKeepCorrespondence()
    {
        Item a = new("A");
        Item b = new("B");
        Item c = new("C");
        ObservableCollection<Item> items = [a, b];
        Section section = new() { ItemsSource = items, ItemTemplate = CellTemplate() };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        items.Insert(0, c);
        items.Remove(a);
        scope.Reset();
        view.ScrollController.ScrollTo(c);
        view.ScrollController.ScrollTo(b);
        view.ScrollController.ScrollTo(a);

        Assert.That(
            scope.All<GatewayCall.ScrollToCell>().Select(call => call.CellId),
            Is.EqualTo(new[] { CellIdOf(view, section, "C"), CellIdOf(view, section, "B") }));

        items.Clear();
        items.Add(a);
        scope.Reset();
        view.ScrollController.ScrollTo(a);
        view.ScrollController.ScrollTo(b);

        Assert.That(
            scope.All<GatewayCall.ScrollToCell>().Select(call => call.CellId),
            Is.EqualTo(new[] { CellIdOf(view, section, "A") }));
    }

    /// <summary>非表示の要素も解決の対象に含め、何もしない扱いは Native に委ねる。</summary>
    [Test]
    public void HiddenElementsAreResolved()
    {
        LabelCell hidden = new() { Title = "hidden", CellId = "hidden", IsVisible = false };
        Section hiddenSection = new() { SectionId = "hidden-section", IsVisible = false };
        SettingsView view = new() { Root = { new Section { Cells = { hidden } }, hiddenSection } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollTo("hidden");
        view.ScrollController.ScrollToSection("hidden-section");

        Assert.That(scope.Single<GatewayCall.ScrollToCell>().CellId, Is.EqualTo(view.Controller.FindCellId(hidden)));
        Assert.That(
            scope.Single<GatewayCall.ScrollToSection>().SectionId,
            Is.EqualTo(view.Controller.FindSectionId(hiddenSection)));
    }

    /// <summary>どの明示 ID にも項目にも当たらない値では gateway を呼ばず、Debug に警告を出す。</summary>
    [Test]
    public void UnresolvedTargetDoesNotCallGateway()
    {
        Section section = new()
        {
            SectionId = "general",
            ItemsSource = new ObservableCollection<string> { "a" },
            ItemTemplate = CellTemplate(),
            Cells = { new LabelCell { Title = "manual", CellId = "wifi" } },
        };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        List<string> messages = CaptureDebugOutput(() =>
        {
            view.ScrollController.ScrollTo("missing");
            view.ScrollController.ScrollTo(new Item("a"));
            view.ScrollController.ScrollToSection("wifi");
            view.ScrollController.ScrollTo("general");
        });

        Assert.That(scope.Calls, Is.Empty);
        Assert.That(messages.Count(message => message.Contains("ignored a scroll command")), Is.EqualTo(4));
    }

    // ---- 命令の gateway 経路と Host が無い間の扱い ----

    /// <summary>先頭・末尾への命令が、アニメーションの指定のまま gateway に届く。</summary>
    [Test]
    public void StartAndEndCommandsReachGateway()
    {
        SettingsView view = new() { Root = { NumberedSection(null, null) } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        view.ScrollController.ScrollToEnd(false);
        view.ScrollController.ScrollToStart();

        Assert.That(scope.Calls, Is.EqualTo(new GatewayCall[]
        {
            new GatewayCall.ScrollToEnd(false),
            new GatewayCall.ScrollToStart(true),
        }));
    }

    /// <summary>Host の初回生成前の命令は何もせず、その後に接続した gateway もこの命令を受けない。</summary>
    [Test]
    public void CommandsBeforeConnectingAreIgnored()
    {
        LabelCell cell = new() { Title = "a", CellId = "a" };
        SettingsView view = new() { Root = { new Section { SectionId = "s", Cells = { cell } } } };

        Assert.DoesNotThrow(() =>
        {
            view.ScrollController.ScrollToEnd();
            view.ScrollController.ScrollToStart();
            view.ScrollController.ScrollTo("a");
            view.ScrollController.ScrollToSection("s");
        });
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Flush();

        Assert.That(scope.All<GatewayCall.ScrollToEnd>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ScrollToStart>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ScrollToCell>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.ScrollToSection>(), Is.Empty);
    }

    /// <summary>Native Host を解放している間の命令は gateway へ渡す (何もしない扱いは Bridge に委ねる)。</summary>
    [Test]
    public void CommandsWhileHostIsReleasedReachGateway()
    {
        LabelCell cell = new() { Title = "a", CellId = "a" };
        SettingsView view = new() { Root = { new Section { Cells = { cell } } } };
        GatewayScope scope = GatewayScope.Connect(view);
        view.ReleaseHost();
        scope.Reset();

        view.ScrollController.ScrollTo("a", ScrollPosition.End, animated: false);
        view.ScrollController.ScrollToEnd();

        Assert.That(scope.Calls, Is.EqualTo(new GatewayCall[]
        {
            new GatewayCall.ScrollToCell(view.Controller.FindCellId(cell)!, ScrollPosition.End, false),
            new GatewayCall.ScrollToEnd(true),
        }));
    }

    // ---- 準備完了のコマンド ----

    /// <summary>Host の生成と取り付けが済むと、コマンドの Execute が 1 回呼ばれる。</summary>
    [Test]
    public void ReadyCommandIsExecutedAfterHostIsAttached()
    {
        CountingCommand command = new();
        SettingsView view = new() { ScrollControllerReadyCommand = command };
        GatewayScope.Connect(view);
        SettingsViewHandler handler = new();

        view.Handler = handler;
        Assert.That(command.ExecuteCount, Is.Zero, "取り付けが済むまでは知らせない");
        handler.OnHostAttached();

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.Null);
    }

    /// <summary>同じ Host の取り付けが繰り返し届いても、知らせるのは Host ごとに 1 回だけ。</summary>
    [Test]
    public void ReadyCommandIsExecutedOncePerHost()
    {
        CountingCommand command = new();
        SettingsView view = new() { ScrollControllerReadyCommand = command };
        GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        view.Handler = handler;

        handler.OnHostAttached();
        handler.OnHostAttached();

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
    }

    /// <summary>Handler の切断と再接続で Host が作り直されるたびに、もう 1 回知らせる。</summary>
    [Test]
    public void ReadyCommandIsExecutedAgainWhenHostIsRecreated()
    {
        CountingCommand command = new();
        SettingsView view = new() { ScrollControllerReadyCommand = command };
        GatewayScope.Connect(view);
        SettingsViewHandler first = new();
        view.Handler = first;
        first.OnHostAttached();

        view.Handler = null;
        SettingsViewHandler second = new();
        view.Handler = second;
        second.OnHostAttached();

        Assert.That(command.ExecuteCount, Is.EqualTo(2));
    }

    /// <summary>CanExecute が偽を返すコマンドは Execute されない。</summary>
    [Test]
    public void ReadyCommandIsNotExecutedWhenItCannotExecute()
    {
        CountingCommand command = new() { CanExecuteResult = false };
        SettingsView view = new() { ScrollControllerReadyCommand = command };
        GatewayScope.Connect(view);
        SettingsViewHandler handler = new();
        view.Handler = handler;

        handler.OnHostAttached();

        Assert.That(command.CanExecuteCount, Is.EqualTo(1));
        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>
    /// 合図の中で出した命令は、Host の作り直し (Bridge が位置の復元を要求する時点) より後に gateway へ届く。
    /// </summary>
    /// <remarks>
    /// 復元そのものは Bridge が Host を作る中で Native の待ち行列の先頭に積むため、ここでは合図の命令が
    /// Host の生成より後に届くことを確かめる。復元より後に実行されることは Bridge のテストと検証ホストで
    /// 確かめる。
    /// </remarks>
    [Test]
    public void CommandsInReadyCommandReachGatewayAfterHostIsRecreated()
    {
        SettingsView view = new() { Root = { NumberedSection(null, null) } };
        view.ScrollControllerReadyCommand = new Command(() => view.ScrollController.ScrollToStart(false));
        GatewayScope scope = GatewayScope.Connect(view);
        SettingsViewHandler first = new();
        view.Handler = first;
        first.OnHostAttached();
        view.Handler = null;
        scope.Reset();

        SettingsViewHandler second = new();
        view.Handler = second;
        Assert.That(scope.All<GatewayCall.ScrollToStart>(), Is.Empty, "Host の生成の時点ではまだ命令しない");
        second.OnHostAttached();

        Assert.That(scope.Calls.Last(), Is.EqualTo(new GatewayCall.ScrollToStart(false)));
    }

    /// <summary>Host の生成が失敗した世代では合図を出さない。</summary>
    [Test]
    public void ReadyCommandIsNotExecutedWhenHostCreationFails()
    {
        CountingCommand command = new();
        SettingsView view = new() { ScrollControllerReadyCommand = command };
        GatewayScope.Connect(view);
        SettingsViewHandler handler = new() { FailsToCreateHost = true };

        Assert.Catch(() => view.Handler = handler);
        handler.OnHostAttached();

        Assert.That(command.ExecuteCount, Is.Zero);
    }

    // ---- 補助 ----

    private static Section NumberedSection(string? sectionId, string? cellId) => new()
    {
        SectionId = sectionId,
        HeaderText = "一般",
        Cells =
        {
            new LabelCell { Title = "Wi-Fi", CellId = cellId },
            new LabelCell { Title = "Bluetooth" },
        },
    };

    /// <summary>項目の名前を Title に写す Cell のテンプレート。</summary>
    private static DataTemplate CellTemplate() => new(() =>
    {
        LabelCell cell = new();
        cell.SetBinding(CellBase.TitleProperty, nameof(Item.Name));
        return cell;
    });

    /// <summary>項目の名前を HeaderText に写す Section のテンプレート。</summary>
    private static DataTemplate SectionTemplate() => new(() =>
    {
        Section section = new();
        section.SetBinding(Section.HeaderTextProperty, nameof(Item.Name));
        return section;
    });

    /// <summary>生成物のうち、Title が <paramref name="title"/> の Cell の gateway 採番 ID。</summary>
    private static string CellIdOf(SettingsView view, Section section, string title)
        => view.Controller.FindCellId(section.Cells.Single(cell => cell.Title == title))!;

    /// <summary>gateway の呼び出しを、Section / Cell の実体と採番 ID に依らない形へ写す。</summary>
    private static IReadOnlyList<string> Describe(IReadOnlyList<GatewayCall> calls)
        => [.. calls.Select(call => call switch
        {
            GatewayCall.SetRoot root => string.Join(
                "|",
                root.Sections.Select(section => $"{section.HeaderText}:" + string.Join(
                    ",",
                    section.Cells.Select(cell => cell.CreateSnapshot().ToString())))),
            _ => call.GetType().Name,
        })];

    /// <summary>処理の間に Debug 出力へ書かれた行を集める。</summary>
    private static List<string> CaptureDebugOutput(Action action)
    {
        CollectingListener listener = new();
        Trace.Listeners.Add(listener);
        try
        {
            action();
        }
        finally
        {
            Trace.Listeners.Remove(listener);
        }

        return listener.Lines;
    }

    /// <summary>ItemsSource の項目。参照で同一性を持つ。</summary>
    /// <param name="name">表示名</param>
    public sealed class Item(string name)
    {
        /// <summary>表示名。</summary>
        public string Name { get; } = name;

        /// <inheritdoc/>
        public override string ToString() => Name;
    }

    /// <summary>スクロール命令のハンドルを受け取る ViewModel。</summary>
    public sealed class ScrollingViewModel
    {
        /// <summary>SettingsView から受け取るハンドル。</summary>
        public IScrollController? Scroll { get; set; }

        /// <summary>最新の項目を見せる。</summary>
        public void ShowLatest() => Scroll?.ScrollToEnd();
    }

    /// <summary>呼び出しを記録するだけの <see cref="IScrollController"/>。</summary>
    private sealed class RecordingScrollController : IScrollController
    {
        public List<string> Calls { get; } = [];

        public void ScrollTo(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true)
            => Calls.Add($"Cell {target} {position} animated={animated}");

        public void ScrollToSection(
            object target,
            ScrollPosition position = ScrollPosition.Start,
            bool animated = true)
            => Calls.Add($"Section {target} {position} animated={animated}");

        public void ScrollToStart(bool animated = true) => Calls.Add($"Start animated={animated}");

        public void ScrollToEnd(bool animated = true) => Calls.Add($"End animated={animated}");
    }

    /// <summary>呼び出し回数を数えるコマンド。</summary>
    private sealed class CountingCommand : ICommand
    {
        public event EventHandler? CanExecuteChanged
        {
            add { }
            remove { }
        }

        public bool CanExecuteResult { get; init; } = true;

        public int CanExecuteCount { get; private set; }

        public int ExecuteCount { get; private set; }

        public object? LastParameter { get; private set; } = new();

        public bool CanExecute(object? parameter)
        {
            CanExecuteCount++;
            return CanExecuteResult;
        }

        public void Execute(object? parameter)
        {
            ExecuteCount++;
            LastParameter = parameter;
        }
    }

    /// <summary>書き込まれた行を集める TraceListener。</summary>
    private sealed class CollectingListener : TraceListener
    {
        public List<string> Lines { get; } = [];

        public override void Write(string? message)
        {
        }

        public override void WriteLine(string? message)
        {
            if (message is not null)
            {
                Lines.Add(message);
            }
        }
    }
}
