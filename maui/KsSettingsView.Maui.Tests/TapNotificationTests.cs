using System;
using System.Collections.Generic;
using System.Windows.Input;
using KsSettingsView.Maui.Internals;
using KsSettingsView.Maui.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests;

/// <summary>タップ通知が Tapped と Command へ届き、実効有効状態に従うことを確認する。</summary>
[TestFixture]
public class TapNotificationTests
{
    /// <summary>CommandCell のタップは Tapped の後に Command を実行する。</summary>
    [Test]
    public void CommandCellTapRaisesTappedThenExecutesCommand()
    {
        List<string> order = [];
        RelayCommand command = new(_ => order.Add("execute"));
        CommandCell cell = new() { Command = command, CommandParameter = "arg" };
        cell.Tapped += (_, _) => order.Add("tapped");
        TapScope scope = TapScope.For(cell);

        scope.Sink.CommandCellTapped(scope.CellId);

        Assert.That(order, Is.EqualTo(new[] { "tapped", "execute" }));
        Assert.That(command.LastParameter, Is.EqualTo("arg"));
    }

    /// <summary>ButtonCell のタップは Tapped の後に Command を実行する。</summary>
    [Test]
    public void ButtonCellTapRaisesTappedThenExecutesCommand()
    {
        List<string> order = [];
        RelayCommand command = new(_ => order.Add("execute"));
        ButtonCell cell = new() { Command = command };
        cell.Tapped += (_, _) => order.Add("tapped");
        TapScope scope = TapScope.For(cell);

        scope.Sink.ButtonCellTapped(scope.CellId);

        Assert.That(order, Is.EqualTo(new[] { "tapped", "execute" }));
    }

    /// <summary>Command を持たない行でもタップ通知は Tapped へ届く。</summary>
    [Test]
    public void TapWithoutCommandStillRaisesTapped()
    {
        int tapped = 0;
        CommandCell cell = new();
        cell.Tapped += (_, _) => tapped++;
        TapScope scope = TapScope.For(cell);

        scope.Sink.CommandCellTapped(scope.CellId);

        Assert.That(tapped, Is.EqualTo(1));
    }

    /// <summary>無効な行はタップ通知でも何も起こさない。</summary>
    [Test]
    public void DisabledCellIgnoresTap()
    {
        int tapped = 0;
        RelayCommand command = new(_ => tapped += 10);
        CommandCell cell = new() { IsEnabled = false, Command = command };
        cell.Tapped += (_, _) => tapped++;
        TapScope scope = TapScope.For(cell);

        scope.Sink.CommandCellTapped(scope.CellId);

        Assert.That(tapped, Is.Zero);
    }

    /// <summary>実行できない Command を持つ行は無効表示になり、タップも通らない。</summary>
    [Test]
    public void CellWithUnexecutableCommandIsDisabledAndIgnoresTap()
    {
        int tapped = 0;
        RelayCommand command = new(_ => tapped += 10, _ => false);
        ButtonCell cell = new() { Command = command };
        cell.Tapped += (_, _) => tapped++;
        TapScope scope = TapScope.For(cell);

        Assert.That(cell.CreateSnapshot().IsEnabled, Is.False);

        scope.Sink.ButtonCellTapped(scope.CellId);

        Assert.That(tapped, Is.Zero);
    }

    /// <summary>CanExecuteChanged で実効有効状態が戻り、表示も追随する。</summary>
    [Test]
    public void CanExecuteChangedRestoresEffectiveEnabledState()
    {
        bool canExecute = false;
        int tapped = 0;
        RelayCommand command = new(_ => tapped++, _ => canExecute);
        ButtonCell cell = new() { Command = command };
        TapScope scope = TapScope.For(cell);
        scope.Gateway.ClearCalls();

        canExecute = true;
        command.RaiseCanExecuteChanged();
        scope.Scope.Flush();

        Assert.That(cell.CreateSnapshot().IsEnabled, Is.True);
        Assert.That(scope.Scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(cell));

        scope.Sink.ButtonCellTapped(scope.CellId);
        Assert.That(tapped, Is.EqualTo(1));
    }

    /// <summary>Command の差し替え後は、旧 Command の通知も実行も届かない。</summary>
    [Test]
    public void ReplacedCommandIsFullyDetached()
    {
        RelayCommand old = new(_ => { }, _ => false);
        RelayCommand replacement = new(_ => { });
        CommandCell cell = new() { Command = old };
        TapScope scope = TapScope.For(cell);
        Assert.That(cell.CreateSnapshot().IsEnabled, Is.False);

        cell.Command = replacement;
        scope.Scope.Flush();
        scope.Gateway.ClearCalls();

        old.RaiseCanExecuteChanged();
        scope.Scope.Flush();

        Assert.That(cell.CreateSnapshot().IsEnabled, Is.True);
        Assert.That(scope.Gateway.Calls, Is.Empty, "旧 Command の通知は配信を起こさない");

        scope.Sink.CommandCellTapped(scope.CellId);
        Assert.That(replacement.ExecuteCount, Is.EqualTo(1));
        Assert.That(old.ExecuteCount, Is.Zero);
    }

    /// <summary>Command を外すと実効有効状態は IsEnabled だけで決まる。</summary>
    [Test]
    public void ClearingCommandLeavesEnabledStateToIsEnabled()
    {
        RelayCommand command = new(_ => { }, _ => false);
        CommandCell cell = new() { Command = command };
        TapScope scope = TapScope.For(cell);
        Assert.That(cell.CreateSnapshot().IsEnabled, Is.False);

        cell.Command = null;

        Assert.That(cell.CreateSnapshot().IsEnabled, Is.True);
        scope.Sink.CommandCellTapped(scope.CellId);
    }

    /// <summary>CommandParameter の差し替えで実行可否が変わると表示も追随する。</summary>
    [Test]
    public void CommandParameterChangeUpdatesEffectiveEnabledState()
    {
        RelayCommand command = new(_ => { }, parameter => parameter is "ok");
        CommandCell cell = new() { Command = command };
        TapScope scope = TapScope.For(cell);
        Assert.That(cell.CreateSnapshot().IsEnabled, Is.False);

        cell.CommandParameter = "ok";
        scope.Scope.Flush();

        Assert.That(cell.CreateSnapshot().IsEnabled, Is.True);
        Assert.That(scope.Scope.All<GatewayCall.ReplaceCell>(), Is.Not.Empty);
    }

    /// <summary>タップ通知の Cell 種別が食い違うときは何も起こらない。</summary>
    [Test]
    public void TapNotificationForOtherCellKindDoesNothing()
    {
        int tapped = 0;
        CommandCell cell = new();
        cell.Tapped += (_, _) => tapped++;
        TapScope scope = TapScope.For(cell);

        scope.Sink.ButtonCellTapped(scope.CellId);
        scope.Sink.CommandCellTapped("unknown");

        Assert.That(tapped, Is.Zero);
    }

    /// <summary>実行可否と実行回数を数えられる Command。</summary>
    /// <param name="execute">実行時の処理</param>
    /// <param name="canExecute">実行可否を返す処理。null なら常に実行できる</param>
    private sealed class RelayCommand(Action<object?> execute, Func<object?, bool>? canExecute = null)
        : ICommand
    {
        public event EventHandler? CanExecuteChanged;

        /// <summary>実行された回数。</summary>
        public int ExecuteCount { get; private set; }

        /// <summary>最後の実行時に渡されたパラメータ。</summary>
        public object? LastParameter { get; private set; }

        /// <inheritdoc/>
        public bool CanExecute(object? parameter) => canExecute?.Invoke(parameter) ?? true;

        /// <inheritdoc/>
        public void Execute(object? parameter)
        {
            ExecuteCount++;
            LastParameter = parameter;
            execute(parameter);
        }

        /// <summary>実行可否が変わったことを通知する。</summary>
        public void RaiseCanExecuteChanged() => CanExecuteChanged?.Invoke(this, EventArgs.Empty);
    }

    /// <summary>Cell 1 件だけを配置して接続した、タップ通知テスト用の足場。</summary>
    /// <param name="Scope">接続した足場</param>
    /// <param name="CellId">対象 Cell の ID</param>
    private sealed record TapScope(GatewayScope Scope, string CellId)
    {
        /// <summary>接続した gateway。</summary>
        public FakeSettingsGateway Gateway => Scope.Gateway;

        /// <summary>ユーザー操作の受け口。</summary>
        public IKsInteractionSink Sink => Scope.Gateway.Sink!;

        /// <summary>指定した Cell だけを持つ設定ツリーを組み立てて接続する。</summary>
        /// <param name="cell">配置する Cell</param>
        public static TapScope For(CellBase cell)
        {
            Section section = new() { Cells = { cell } };
            SettingsView view = new() { Root = { section } };
            GatewayScope scope = GatewayScope.Connect(view).Reset();
            return new TapScope(scope, view.Controller.FindCellId(cell)!);
        }
    }
}
