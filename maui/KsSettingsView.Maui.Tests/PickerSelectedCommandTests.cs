using System;
using System.Collections;
using System.Collections.Generic;
using System.Windows.Input;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>PickerCell の選択完了 Command の公開面と通知境界を確認する。</summary>
[TestFixture]
public sealed class PickerSelectedCommandTests
{
    /// <summary>SelectedCommand は null を既定とする OneWay の ICommand として公開される。</summary>
    [Test]
    public void SelectedCommandHasExpectedPublicShape()
    {
        PickerCell cell = new();

        Assert.That(PickerCell.SelectedCommandProperty.PropertyName, Is.EqualTo(nameof(PickerCell.SelectedCommand)));
        Assert.That(PickerCell.SelectedCommandProperty.ReturnType, Is.EqualTo(typeof(ICommand)));
        Assert.That(PickerCell.SelectedCommandProperty.DefaultBindingMode, Is.EqualTo(BindingMode.OneWay));
        Assert.That(cell.SelectedCommand, Is.Null);
    }

    /// <summary>確定通知だけが届いた時点では、値は反映されても Command は実行されない。</summary>
    [TestCase(PickerSelectionMode.Single)]
    [TestCase(PickerSelectionMode.Multiple)]
    public void SelectionChangeAloneUpdatesValuesWithoutExecuting(PickerSelectionMode mode)
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            SelectionMode = mode,
            ItemsSource = new List<string> { "ライト", "ダーク" },
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        if (mode == PickerSelectionMode.Single)
        {
            scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
            Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        }
        else
        {
            scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [1]);
            Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 1 }));
        }

        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>単一選択は、値と TwoWay 先の更新を終えた状態で閉じ切り通知に応じる。</summary>
    [Test]
    public void SingleSelectionExecutesOnCompletionAfterValuesAndBindingsAreUpdated()
    {
        PickerViewModel viewModel = new();
        PickerCell cell = new()
        {
            BindingContext = viewModel,
            ItemsSource = new List<string> { "ライト", "ダーク" },
        };
        cell.SetBinding(PickerCell.SelectedIndexProperty, nameof(PickerViewModel.SelectedIndex));
        cell.SetBinding(PickerCell.SelectedItemProperty, nameof(PickerViewModel.SelectedItem));
        RecordingCommand command = new(parameter =>
        {
            Assert.That(cell.SelectedIndex, Is.EqualTo(1));
            Assert.That(cell.SelectedItem, Is.EqualTo("ダーク"));
            Assert.That(viewModel.SelectedIndex, Is.EqualTo(1));
            Assert.That(viewModel.SelectedItem, Is.EqualTo("ダーク"));
            Assert.That(parameter, Is.SameAs(cell.SelectedItem));
        });
        cell.SelectedCommand = command;
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
        scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.EqualTo("ダーク"));
    }

    /// <summary>複数選択は、値と TwoWay 先の更新を終えた状態で閉じ切り通知に応じる。</summary>
    [Test]
    public void MultipleSelectionExecutesOnCompletionAfterValuesAndBindingsAreUpdated()
    {
        PickerViewModel viewModel = new();
        PickerCell cell = new()
        {
            BindingContext = viewModel,
            SelectionMode = PickerSelectionMode.Multiple,
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
        };
        cell.SetBinding(PickerCell.SelectedIndicesProperty, nameof(PickerViewModel.SelectedIndices));
        cell.SetBinding(PickerCell.SelectedItemsProperty, nameof(PickerViewModel.SelectedItems));
        RecordingCommand command = new(parameter =>
        {
            Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
            Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
            Assert.That(viewModel.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
            Assert.That(viewModel.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
            Assert.That(parameter, Is.SameAs(cell.SelectedItems));
        });
        cell.SelectedCommand = command;
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [2, 0, 2]);
        scope.Sink.PickerCellMultiSelectionCompleted(scope.CellId, [2, 0, 2]);

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.SameAs(cell.SelectedItems));
    }

    /// <summary>同じ選択の再確定は値の再送をせず、閉じ切り通知で完了だけを通知する。</summary>
    [TestCase(PickerSelectionMode.Single)]
    [TestCase(PickerSelectionMode.Multiple)]
    public void ReconfirmingSameSelectionExecutesWithoutAnotherWriteback(PickerSelectionMode mode)
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            SelectionMode = mode,
            ItemsSource = new List<string> { "松", "竹" },
            SelectedIndex = mode == PickerSelectionMode.Single ? 1 : null,
            SelectedIndices = mode == PickerSelectionMode.Multiple ? new List<int> { 1 } : null,
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        if (mode == PickerSelectionMode.Single)
        {
            scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
            scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);
        }
        else
        {
            scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [1]);
            scope.Sink.PickerCellMultiSelectionCompleted(scope.CellId, [1]);
        }
        scope.Scope.Flush();

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>閉じ切り通知は CanExecute を確認せず Execute を直接呼ぶ。</summary>
    [Test]
    public void CompletionExecutesWithoutCheckingCanExecute()
    {
        RecordingCommand command = new(canExecute: _ => false);
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "松", "竹" },
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
        scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);

        Assert.That(command.CanExecuteCount, Is.Zero);
        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.EqualTo("竹"));
    }

    /// <summary>公開選択値の直接設定では Command を実行しない。</summary>
    [Test]
    public void DirectSelectionChangesDoNotExecuteCommand()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "松", "竹" },
            SelectedCommand = command,
        };

        cell.SelectedIndex = 1;
        cell.SelectedItem = "松";
        cell.SelectionMode = PickerSelectionMode.Multiple;
        cell.SelectedIndices = [0, 1];
        cell.SelectedItems = new List<object> { "竹" };

        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>Command を設定していない Cell の閉じ切り通知は何も起こさない。</summary>
    [Test]
    public void CompletionWithoutCommandDoesNothing()
    {
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "松", "竹" },
            SelectedIndex = 1,
        };
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
        scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);
        scope.Scope.Flush();

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(cell.SelectedItem, Is.EqualTo("竹"));
        Assert.That(scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>未知の Cell ID の単一選択通知は値も Command も変更しない。</summary>
    [Test]
    public void UnknownCellSingleSelectionNotificationDoesNotChangeValuesOrExecuteCommand()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "松", "竹" },
            SelectedIndex = 0,
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellSelectionChanged("unknown", 1);
        scope.Sink.PickerCellSelectionCompleted("unknown", 1);

        Assert.That(cell.SelectedIndex, Is.Zero);
        Assert.That(cell.SelectedItem, Is.EqualTo("松"));
        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>未知の Cell ID の複数選択通知は値も Command も変更しない。</summary>
    [Test]
    public void UnknownCellMultipleSelectionNotificationDoesNotChangeValuesOrExecuteCommand()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            SelectionMode = PickerSelectionMode.Multiple,
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
            SelectedIndices = [0, 2],
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellMultiSelectionChanged("unknown", [1]);
        scope.Sink.PickerCellMultiSelectionCompleted("unknown", [1]);

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(cell.SelectedItems, Is.EqualTo(new[] { "メール", "SMS" }));
        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>PickerCell 以外の Cell ID で届いた閉じ切り通知は無視される。</summary>
    [Test]
    public void CompletionForOtherCellKindIsIgnored()
    {
        RecordingCommand command = new();
        PickerCell picker = new()
        {
            ItemsSource = new List<string> { "松", "竹" },
            SelectedCommand = command,
        };
        SwitchCell other = new();
        Section section = new() { Cells = { picker, other } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        string otherId = view.Controller.FindCellId(other)!;

        scope.Gateway.Sink!.PickerCellSelectionCompleted(otherId, 1);
        scope.Gateway.Sink!.PickerCellMultiSelectionCompleted(otherId, [1]);

        Assert.That(command.ExecuteCount, Is.Zero);
    }

    /// <summary>確定から閉じ切りまでに選択が変わっても、引数は閉じ切り時点の現値になる。</summary>
    [Test]
    public void CompletionPassesCurrentValueWhenSelectionChangedInBetween()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "松", "竹", "梅" },
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);

        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
        cell.SelectedIndex = 2;
        scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);

        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.EqualTo("梅"));
    }

    /// <summary>現在のモードが複数でも、単一選択の閉じ切り通知は選択項目を引数にする。</summary>
    [Test]
    public void SingleCompletionUsesSelectedItemAfterModeChangesToMultiple()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            ItemsSource = new List<string> { "ライト", "ダーク" },
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);
        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);
        cell.SelectionMode = PickerSelectionMode.Multiple;

        scope.Sink.PickerCellSelectionCompleted(scope.CellId, 1);

        Assert.That(cell.SelectionMode, Is.EqualTo(PickerSelectionMode.Multiple));
        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.SameAs(cell.SelectedItem));
        Assert.That(command.LastParameter, Is.EqualTo("ダーク"));
    }

    /// <summary>現在のモードが単一でも、複数選択の閉じ切り通知は選択項目列を引数にする。</summary>
    [Test]
    public void MultipleCompletionUsesSelectedItemsAfterModeChangesToSingle()
    {
        RecordingCommand command = new();
        PickerCell cell = new()
        {
            SelectionMode = PickerSelectionMode.Multiple,
            ItemsSource = new List<string> { "メール", "プッシュ", "SMS" },
            SelectedIndices = [0],
            SelectedCommand = command,
        };
        PickerScope scope = PickerScope.For(cell);
        scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [1, 2]);
        cell.SelectionMode = PickerSelectionMode.Single;

        scope.Sink.PickerCellMultiSelectionCompleted(scope.CellId, [1, 2]);

        Assert.That(cell.SelectionMode, Is.EqualTo(PickerSelectionMode.Single));
        Assert.That(command.ExecuteCount, Is.EqualTo(1));
        Assert.That(command.LastParameter, Is.SameAs(cell.SelectedItems));
        Assert.That(command.LastParameter, Is.EqualTo(new[] { "プッシュ", "SMS" }));
    }

    /// <summary>Picker の TwoWay バインド先。</summary>
    private sealed class PickerViewModel
    {
        /// <summary>単一選択の位置。</summary>
        public int? SelectedIndex { get; set; }

        /// <summary>単一選択の項目。</summary>
        public object? SelectedItem { get; set; }

        /// <summary>複数選択の位置。</summary>
        public IList<int>? SelectedIndices { get; set; }

        /// <summary>複数選択の項目。</summary>
        public IList? SelectedItems { get; set; }
    }

    /// <summary>実行回数と実行引数を記録する Command。</summary>
    private sealed class RecordingCommand(
        Action<object?>? execute = null,
        Func<object?, bool>? canExecute = null) : ICommand
    {
        public event EventHandler? CanExecuteChanged
        {
            add { }
            remove { }
        }

        /// <summary>CanExecute が呼ばれた回数。</summary>
        public int CanExecuteCount { get; private set; }

        /// <summary>Execute が呼ばれた回数。</summary>
        public int ExecuteCount { get; private set; }

        /// <summary>最後に Execute へ渡された引数。</summary>
        public object? LastParameter { get; private set; }

        /// <inheritdoc/>
        public bool CanExecute(object? parameter)
        {
            CanExecuteCount++;
            return canExecute?.Invoke(parameter) ?? true;
        }

        /// <inheritdoc/>
        public void Execute(object? parameter)
        {
            ExecuteCount++;
            LastParameter = parameter;
            execute?.Invoke(parameter);
        }
    }

    /// <summary>PickerCell 1 件を配置して接続した通知テスト用の足場。</summary>
    /// <param name="Scope">接続した足場</param>
    /// <param name="CellId">対象 Cell の ID</param>
    private sealed record PickerScope(GatewayScope Scope, string CellId)
    {
        /// <summary>接続した gateway。</summary>
        public FakeSettingsGateway Gateway => Scope.Gateway;

        /// <summary>ユーザー操作の受け口。</summary>
        public IKsInteractionSink Sink => Gateway.Sink!;

        /// <summary>指定した Cell を持つ設定ツリーを組み立てて接続する。</summary>
        /// <param name="cell">配置する Cell</param>
        public static PickerScope For(PickerCell cell)
        {
            Section section = new() { Cells = { cell } };
            SettingsView view = new() { Root = { section } };
            GatewayScope scope = GatewayScope.Connect(view).Reset();
            return new PickerScope(scope, view.Controller.FindCellId(cell)!);
        }
    }
}
