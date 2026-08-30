using System;
using System.Collections.Generic;
using System.Reflection;
using System.Windows.Input;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>
/// CustomCell の公開面と、写しへ載る項目を確認する。
/// </summary>
/// <remarks>
/// 内容の View の実体化と寿命は <see cref="CustomCellContentTests"/> が受け持つ。
/// </remarks>
[TestFixture]
public class CustomCellTests
{
    // ---- 公開面 ----

    /// <summary>CustomCell は内容なし・矢印なし・タップ動作なしから始まる。</summary>
    [Test]
    public void CustomCellDefaultsToEmptyRowWithoutTapBehaviour()
    {
        CustomCell cell = new();

        Assert.That(cell.Content, Is.Null);
        Assert.That(cell.ShowArrowIndicator, Is.False);
        Assert.That(cell.Command, Is.Null);
        Assert.That(cell.CommandParameter, Is.Null);
        Assert.That(cell.IsEnabled, Is.True);
        Assert.That(cell.IsVisible, Is.True);
    }

    /// <summary>Content が content property であり、XAML では直下に View を書ける。</summary>
    [Test]
    public void ContentIsTheContentProperty()
    {
        ContentPropertyAttribute? attribute =
            typeof(CustomCell).GetCustomAttribute<ContentPropertyAttribute>();

        Assert.That(attribute, Is.Not.Null);
        Assert.That(attribute!.Name, Is.EqualTo(nameof(CustomCell.Content)));
    }

    // ---- 写しへ載る項目 ----

    /// <summary>内容を持つ行は、内容の世代を載せた CustomCell の写しとして運ばれる。</summary>
    [Test]
    public void ContentIsCarriedAsAGenerationInTheSnapshot()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        Fixture fixture = Fixture.For(cell);

        KsCustomCellSnapshot snapshot = fixture.Snapshot();
        Assert.That(snapshot.ContentToken, Is.Not.Empty);
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(content).PlatformView));
    }

    /// <summary>内容を持たない行も CustomCell の写しとして運ばれ、内容は空になる。</summary>
    [Test]
    public void CellWithoutContentIsCarriedAsAnEmptyRow()
    {
        CustomCell cell = new();
        Fixture fixture = Fixture.For(cell);

        Assert.That(fixture.Snapshot(), Is.Not.Null);
        Assert.That(fixture.Scope.Gateway.CellContentViewOf(cell), Is.Null);
    }

    /// <summary>派生サブクラスも CustomCell と同じ経路で運ばれる。</summary>
    [Test]
    public void DerivedCellIsCarriedThroughTheSamePath()
    {
        SliderCell cell = new();
        Fixture fixture = Fixture.For(cell);

        Assert.That(fixture.Snapshot().ContentToken, Is.Not.Empty);
        Assert.That(
            fixture.Scope.Gateway.CellContentViewOf(cell),
            Is.SameAs(fixture.Scope.Views.LatestFor(cell.Slider).PlatformView));
    }

    /// <summary>ShowArrowIndicator は Command の指定と独立に運ばれる。</summary>
    [Test]
    public void ShowArrowIndicatorIsCarriedIndependentlyOfTapBehaviour()
    {
        CustomCell cell = new();
        Fixture fixture = Fixture.For(cell);
        Assert.That(fixture.Snapshot().ShowArrowIndicator, Is.False);
        fixture.Scope.Reset();

        cell.ShowArrowIndicator = true;
        fixture.Scope.Flush();

        KsCustomCellSnapshot delivered = fixture.Delivered();
        Assert.That(delivered.ShowArrowIndicator, Is.True);
        Assert.That(delivered.HasTapHandler, Is.False);
    }

    /// <summary>行そのものに掛かる指定は写しへ載る。</summary>
    [Test]
    public void RowLevelStyleIsCarried()
    {
        CustomCell cell = new() { Height = 96, BackgroundColor = Colors.Red };
        Fixture fixture = Fixture.For(cell);

        KsCellStyleSnapshot style = fixture.Snapshot().Style!;
        Assert.That(style.CellHeight, Is.EqualTo(96));
        Assert.That(style.BackgroundColor, Is.EqualTo(KsWireValues.Color(Colors.Red)));
    }

    /// <summary>可視性を落とすと行を出力しない内容として送り直され、戻すと元に戻る。</summary>
    [Test]
    public void HidingTheRowIsDeliveredAsAnInvisibleRow()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        Fixture fixture = Fixture.For(cell);
        string token = fixture.Snapshot().ContentToken;
        fixture.Scope.Reset();

        cell.IsVisible = false;
        fixture.Scope.Flush();

        Assert.That(fixture.Delivered().IsVisible, Is.False);

        fixture.Scope.Reset();
        cell.IsVisible = true;
        fixture.Scope.Flush();

        KsCustomCellSnapshot restored = fixture.Delivered();
        Assert.That(restored.IsVisible, Is.True);
        Assert.That(restored.ContentToken, Is.EqualTo(token), "可視性の切り替えは内容を作り直さない");
        Assert.That(fixture.Scope.Views.CountFor(content), Is.EqualTo(1));
    }

    /// <summary>無効にすると無効な行として送り直され、戻すと有効な行に戻る。</summary>
    [Test]
    public void DisablingTheRowIsDeliveredAsADisabledRow()
    {
        CustomCell cell = new() { Content = new Label() };
        Fixture fixture = Fixture.For(cell);
        fixture.Scope.Reset();

        cell.IsEnabled = false;
        fixture.Scope.Flush();

        Assert.That(fixture.Delivered().IsEnabled, Is.False);

        fixture.Scope.Reset();
        cell.IsEnabled = true;
        fixture.Scope.Flush();

        Assert.That(fixture.Delivered().IsEnabled, Is.True);
    }

    /// <summary>表示後に変えた行の高さと背景色も、行そのものの指定として送り直される。</summary>
    [Test]
    public void RowLevelStyleChangeIsDeliveredWithoutRebuildingTheContent()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        Fixture fixture = Fixture.For(cell);
        string token = fixture.Snapshot().ContentToken;
        fixture.Scope.Reset();

        cell.Height = 120;
        cell.BackgroundColor = Colors.Blue;
        fixture.Scope.Flush();

        KsCustomCellSnapshot delivered = fixture.Delivered();
        Assert.That(delivered.Style!.CellHeight, Is.EqualTo(120));
        Assert.That(delivered.Style.BackgroundColor, Is.EqualTo(KsWireValues.Color(Colors.Blue)));
        Assert.That(delivered.ContentToken, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.CountFor(content), Is.EqualTo(1));
        Assert.That(fixture.Scope.Views.LatestFor(content).IsDisposed, Is.False);
    }

    // ---- 不適用プロパティ ----

    /// <summary>共通行レイアウトのスロットに掛かる指定は写しへ載らず、配信も起こさない。</summary>
    [Test]
    public void InapplicablePropertiesAreIgnoredWithoutAnyDelivery()
    {
        CustomCell cell = new() { Content = new Label() };
        Fixture fixture = Fixture.For(cell);
        fixture.Scope.Reset();

        cell.Title = "title";
        cell.Description = "description";
        cell.HintText = "hint";
        cell.TitleColor = Colors.Red;
        cell.TitleFontSize = 24;
        cell.DescriptionColor = Colors.Blue;
        cell.ValueTextColor = Colors.Green;
        cell.HintTextColor = Colors.Orange;
        cell.IconSize = 40;
        cell.IconRadius = 8;
        fixture.Scope.Flush();

        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty);

        KsCustomCellSnapshot snapshot = fixture.Snapshot();
        Assert.That(snapshot.Title, Is.Empty);
        Assert.That(snapshot.Description, Is.Null);
        Assert.That(snapshot.HintText, Is.Null);
        Assert.That(snapshot.Style, Is.Null);
    }

    /// <summary>
    /// 同じスタイル指定を Cell 群へ当てると、扱える Cell にだけ効き CustomCell では無視される。
    /// </summary>
    [Test]
    public void SharedStyleValuesTakeEffectOnlyWhereTheyApply()
    {
        LabelCell label = new() { Title = "label" };
        CustomCell custom = new() { Content = new Label() };
        Section section = new() { Cells = { label, custom } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();

        foreach (CellBase cell in new CellBase[] { label, custom })
        {
            cell.TitleColor = Colors.Red;
            cell.TitleFontSize = 20;
        }

        scope.Flush();

        Assert.That(label.CreateSnapshot().Style!.TitleColor, Is.EqualTo(KsWireValues.Color(Colors.Red)));
        Assert.That(custom.CreateSnapshot().Style, Is.Null);
    }

    // ---- 行タップ ----

    /// <summary>行のタップは Tapped の後に Command を実行する。</summary>
    [Test]
    public void RowTapRaisesTappedThenExecutesCommand()
    {
        List<string> order = [];
        RelayCommand command = new(_ => order.Add("execute"));
        CustomCell cell = new() { Command = command, CommandParameter = "arg" };
        cell.Tapped += (_, _) => order.Add("tapped");
        Fixture fixture = Fixture.For(cell);

        fixture.Tap();

        Assert.That(order, Is.EqualTo(new[] { "tapped", "execute" }));
        Assert.That(command.LastParameter, Is.EqualTo("arg"));
    }

    /// <summary>Command を持たない行でもタップ通知は Tapped へ届く。</summary>
    [Test]
    public void TapWithoutCommandStillRaisesTapped()
    {
        int tapped = 0;
        CustomCell cell = new() { Content = new Label() };
        cell.Tapped += (_, _) => tapped++;
        Fixture fixture = Fixture.For(cell);

        fixture.Tap();

        Assert.That(tapped, Is.EqualTo(1));
    }

    /// <summary>タップ通知の Cell 種別が食い違うときと、未知の行への通知では何も起こらない。</summary>
    [Test]
    public void TapNotificationForOtherCellKindOrUnknownRowDoesNothing()
    {
        int customTapped = 0;
        int commandTapped = 0;
        CustomCell custom = new() { Content = new Label() };
        custom.Tapped += (_, _) => customTapped++;
        CommandCell command = new();
        command.Tapped += (_, _) => commandTapped++;
        Section section = new() { Cells = { custom, command } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        scope.Attach();
        IKsInteractionSink sink = scope.Gateway.Sink!;

        sink.CommandCellTapped(view.Controller.FindCellId(custom)!);
        sink.CustomCellTapped(view.Controller.FindCellId(command)!);
        sink.CustomCellTapped("unknown");

        Assert.That(customTapped, Is.Zero);
        Assert.That(commandTapped, Is.Zero);
    }

    /// <summary>無効な行はタップしても何も起こさない。</summary>
    [Test]
    public void DisabledRowIgnoresTap()
    {
        int tapped = 0;
        RelayCommand command = new(_ => tapped += 10);
        CustomCell cell = new() { IsEnabled = false, Command = command };
        cell.Tapped += (_, _) => tapped++;
        Fixture fixture = Fixture.For(cell);

        fixture.Tap();

        Assert.That(tapped, Is.Zero);
    }

    /// <summary>実行できない Command を持つ行は無効表示になり、タップも通らない。</summary>
    [Test]
    public void RowWithUnexecutableCommandIsDisabledAndIgnoresTap()
    {
        int tapped = 0;
        RelayCommand command = new(_ => tapped += 10, _ => false);
        CustomCell cell = new() { Command = command };
        cell.Tapped += (_, _) => tapped++;
        Fixture fixture = Fixture.For(cell);

        Assert.That(fixture.Snapshot().IsEnabled, Is.False);

        fixture.Tap();

        Assert.That(tapped, Is.Zero);
    }

    /// <summary>通知先を持たない行は、行タップ動作なしとして運ばれる。</summary>
    [Test]
    public void RowWithoutHandlersIsCarriedWithoutTapBehaviour()
    {
        CustomCell cell = new() { Content = new Label() };
        Fixture fixture = Fixture.For(cell);

        Assert.That(fixture.Snapshot().HasTapHandler, Is.False);
    }

    /// <summary>表示後に Command を設定すると、内容はそのままで行タップ動作だけが送り直される。</summary>
    [Test]
    public void SettingCommandAfterDisplayReissuesTapBehaviourOnly()
    {
        Label content = new();
        CustomCell cell = new() { Content = content };
        Fixture fixture = Fixture.For(cell);
        string token = fixture.Snapshot().ContentToken;
        fixture.Scope.Reset();

        int executed = 0;
        cell.Command = new RelayCommand(_ => executed++);
        fixture.Scope.Flush();

        KsCustomCellSnapshot delivered = fixture.Delivered();
        Assert.That(delivered.HasTapHandler, Is.True);
        Assert.That(delivered.ContentToken, Is.EqualTo(token));
        Assert.That(fixture.Scope.Views.CountFor(content), Is.EqualTo(1));
        Assert.That(fixture.Scope.Views.LatestFor(content).IsDisposed, Is.False);

        fixture.Tap();
        Assert.That(executed, Is.EqualTo(1));
    }

    /// <summary>実行可否の変化は、行の有効状態として送り直される。</summary>
    [Test]
    public void CanExecuteChangeIsDeliveredAsTheRowEnabledState()
    {
        bool canExecute = false;
        RelayCommand command = new(_ => { }, _ => canExecute);
        CustomCell cell = new() { Content = new Label(), Command = command };
        Fixture fixture = Fixture.For(cell);
        Assert.That(fixture.Snapshot().IsEnabled, Is.False);
        fixture.Scope.Reset();

        canExecute = true;
        command.RaiseCanExecuteChanged();
        fixture.Scope.Flush();

        Assert.That(fixture.Delivered().IsEnabled, Is.True);
    }

    /// <summary>Command を外すと行タップ動作なしとして送り直される。</summary>
    [Test]
    public void ClearingCommandReissuesWithoutTapBehaviour()
    {
        int executed = 0;
        CustomCell cell = new() { Command = new RelayCommand(_ => executed++) };
        Fixture fixture = Fixture.For(cell);
        fixture.Scope.Reset();

        cell.Command = null;
        fixture.Scope.Flush();

        Assert.That(fixture.Delivered().HasTapHandler, Is.False);

        fixture.Tap();
        Assert.That(executed, Is.Zero, "外した Command は実行されない");
    }

    /// <summary>Tapped の最初の購読と最後の購読解除だけが行タップ動作の変化として送られる。</summary>
    [Test]
    public void OnlyTheFirstAndLastTappedSubscriptionAreDelivered()
    {
        CustomCell cell = new() { Content = new Label() };
        Fixture fixture = Fixture.For(cell);
        fixture.Scope.Reset();

        void First(object? sender, EventArgs args)
        {
        }

        void Second(object? sender, EventArgs args)
        {
        }

        cell.Tapped += First;
        fixture.Scope.Flush();
        Assert.That(fixture.Delivered().HasTapHandler, Is.True);

        fixture.Scope.Reset();
        cell.Tapped += Second;
        cell.Tapped -= Second;
        fixture.Scope.Flush();
        Assert.That(fixture.Scope.Gateway.Calls, Is.Empty, "途中の購読の増減は配信を起こさない");

        cell.Tapped -= First;
        fixture.Scope.Flush();
        Assert.That(fixture.Delivered().HasTapHandler, Is.False);
    }

    /// <summary>コンストラクタで内容を組み立てる派生サブクラス。</summary>
    private sealed class SliderCell : CustomCell
    {
        public SliderCell()
        {
            Slider = new Slider();
            Content = Slider;
        }

        /// <summary>組み立てた内容。</summary>
        public Slider Slider { get; }
    }

    /// <summary>実行可否と実行回数を数えられる Command。</summary>
    /// <param name="execute">実行時の処理</param>
    /// <param name="canExecute">実行可否を返す処理。null なら常に実行できる</param>
    private sealed class RelayCommand(Action<object?> execute, Func<object?, bool>? canExecute = null)
        : ICommand
    {
        public event EventHandler? CanExecuteChanged;

        /// <summary>最後の実行時に渡されたパラメータ。</summary>
        public object? LastParameter { get; private set; }

        /// <inheritdoc/>
        public bool CanExecute(object? parameter) => canExecute?.Invoke(parameter) ?? true;

        /// <inheritdoc/>
        public void Execute(object? parameter)
        {
            LastParameter = parameter;
            execute(parameter);
        }

        /// <summary>実行可否が変わったことを通知する。</summary>
        public void RaiseCanExecuteChanged() => CanExecuteChanged?.Invoke(this, EventArgs.Empty);
    }

    /// <summary>CustomCell 1 件だけを配置し、Native Host まで取り付けた足場。</summary>
    private sealed class Fixture
    {
        private Fixture(CustomCell cell, GatewayScope scope, string cellId)
        {
            Cell = cell;
            Scope = scope;
            CellId = cellId;
        }

        /// <summary>対象の Cell。</summary>
        public CustomCell Cell { get; }

        /// <summary>接続した足場。</summary>
        public GatewayScope Scope { get; }

        /// <summary>対象 Cell の ID。</summary>
        public string CellId { get; }

        /// <summary>ユーザー操作の受け口。</summary>
        public IKsInteractionSink Sink => Scope.Gateway.Sink!;

        /// <summary>指定した Cell だけを持つ設定ツリーを組み立てて接続し、Host を取り付ける。</summary>
        /// <param name="cell">配置する Cell</param>
        public static Fixture For(CustomCell cell)
        {
            Section section = new() { Cells = { cell } };
            SettingsView view = new() { Root = { section } };
            GatewayScope scope = GatewayScope.Connect(view);
            scope.Attach();
            return new Fixture(cell, scope, view.Controller.FindCellId(cell)!);
        }

        /// <summary>今この時点で輸送するときの写し。</summary>
        public KsCustomCellSnapshot Snapshot() => (KsCustomCellSnapshot)Cell.CreateSnapshot();

        /// <summary>直近に配信された内容更新に載っていた写し。</summary>
        public KsCustomCellSnapshot Delivered()
            => (KsCustomCellSnapshot)Scope.Single<GatewayCall.ReplaceCell>().Snapshot;

        /// <summary>Native からのタップ通知を模擬する。</summary>
        public void Tap() => Sink.CustomCellTapped(CellId);
    }
}
