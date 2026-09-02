using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using Microsoft.Maui.Controls;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Native で起きたユーザー操作が facade のプロパティへ書き戻され、Store へコミットされることを
/// 確認する。
/// </summary>
[TestFixture]
public class NativeValueWritebackTests
{
    /// <summary>スイッチの操作は On へ書き戻され、内容更新として配信される。</summary>
    [Test]
    public void SwitchChangeIsWrittenBackAndPublished()
    {
        SwitchCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.SwitchCellChanged(scope.CellId, true);

        Assert.That(cell.On, Is.True);
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>チェックボックスの操作は Checked へ書き戻される。</summary>
    [Test]
    public void CheckboxChangeIsWrittenBack()
    {
        CheckboxCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.CheckboxCellChanged(scope.CellId, true);

        Assert.That(cell.Checked, Is.True);
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>簡易チェックの操作は Checked へ書き戻される。</summary>
    [Test]
    public void SimpleCheckChangeIsWrittenBack()
    {
        SimpleCheckCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.SimpleCheckCellChanged(scope.CellId, true);

        Assert.That(cell.Checked, Is.True);
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>テキスト入力は ValueText へ書き戻される。</summary>
    [Test]
    public void EntryTextChangeIsWrittenBack()
    {
        EntryCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.EntryCellTextChanged(scope.CellId, "かむ");

        Assert.That(cell.ValueText, Is.EqualTo("かむ"));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>単一選択の確定は SelectedIndex へ書き戻される。</summary>
    [Test]
    public void PickerSelectionIsWrittenBack()
    {
        PickerCell cell = new() { ItemsSource = new List<string> { "ライト", "ダーク" } };
        Writeback scope = Writeback.For(cell);

        scope.Sink.PickerCellSelectionChanged(scope.CellId, 1);

        Assert.That(cell.SelectedIndex, Is.EqualTo(1));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>複数選択の確定は SelectedIndices へ昇順・重複なしで書き戻される。</summary>
    [Test]
    public void PickerMultiSelectionIsWrittenBackNormalized()
    {
        PickerCell cell = new() { SelectionMode = PickerSelectionMode.Multiple };
        Writeback scope = Writeback.For(cell);

        scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [2, 0, 2]);

        Assert.That(cell.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>数値の確定は Number へ書き戻される。</summary>
    [Test]
    public void NumberPickerChangeIsWrittenBack()
    {
        NumberPickerCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.NumberPickerCellChanged(scope.CellId, 7);

        Assert.That(cell.Number, Is.EqualTo(7));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>時刻の確定は Time へ書き戻される。</summary>
    [Test]
    public void TimePickerChangeIsWrittenBack()
    {
        TimePickerCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.TimePickerCellChanged(scope.CellId, "09:05");

        Assert.That(cell.Time, Is.EqualTo(new TimeSpan(9, 5, 0)));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>日付の確定は Date へ書き戻される。</summary>
    [Test]
    public void DatePickerChangeIsWrittenBack()
    {
        DatePickerCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.DatePickerCellChanged(scope.CellId, "2026-08-10");

        Assert.That(cell.Date, Is.EqualTo(new DateTime(2026, 8, 10)));
        Assert.That(scope.PublishedCells(), Is.EqualTo(new CellBase[] { cell }));
    }

    /// <summary>解釈できない壁時計値の通知は捨てられ、現在値を壊さない。</summary>
    [Test]
    public void UnparsableWallClockNotificationIsDiscarded()
    {
        TimePickerCell time = new() { Time = new TimeSpan(1, 2, 0) };
        DatePickerCell date = new() { Date = new DateTime(2026, 1, 1) };
        Section section = new() { Cells = { time, date } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        IKsInteractionSink sink = scope.Gateway.Sink!;

        sink.TimePickerCellChanged(view.Controller.FindCellId(time)!, "9:5");
        sink.DatePickerCellChanged(view.Controller.FindCellId(date)!, "2026/08/10");
        scope.Flush();

        Assert.That(time.Time, Is.EqualTo(new TimeSpan(1, 2, 0)));
        Assert.That(date.Date, Is.EqualTo(new DateTime(2026, 1, 1)));
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>radio の選択は同じグループの全ての行へ反映される。</summary>
    [Test]
    public void RadioSelectionIsAppliedToWholeGroup()
    {
        RadioCell first = new() { GroupId = "theme", Value = "light", SelectedValue = "light" };
        RadioCell second = new() { GroupId = "theme", Value = "dark", SelectedValue = "light" };
        RadioCell third = new() { GroupId = "theme", Value = "auto", SelectedValue = "light" };
        RadioCell other = new() { GroupId = "size", Value = "large", SelectedValue = "small" };
        Section section = new() { Cells = { first, second, third, other } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        scope.Gateway.Sink!.RadioCellSelected(view.Controller.FindCellId(third)!, "auto");
        scope.Flush();

        Assert.That(first.SelectedValue, Is.EqualTo("auto"));
        Assert.That(second.SelectedValue, Is.EqualTo("auto"));
        Assert.That(third.SelectedValue, Is.EqualTo("auto"));
        Assert.That(other.SelectedValue, Is.EqualTo("small"));

        IReadOnlyList<GatewayCall.CellUpdate> updates = scope.Single<GatewayCall.ReplaceCells>().Updates;
        Assert.That(
            updates.Select(update => update.NewCell),
            Is.EquivalentTo(new CellBase[] { first, second, third }));
    }

    /// <summary>既に新しい値を持つ行は radio の一括反映から外れる。</summary>
    [Test]
    public void RadioSelectionSkipsCellsAlreadyHoldingTheValue()
    {
        RadioCell first = new() { GroupId = "theme", Value = "light", SelectedValue = "dark" };
        RadioCell second = new() { GroupId = "theme", Value = "dark", SelectedValue = "light" };
        Section section = new() { Cells = { first, second } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        scope.Gateway.Sink!.RadioCellSelected(view.Controller.FindCellId(second)!, "dark");
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().NewCell, Is.SameAs(second));
    }

    /// <summary>同値の通知は書き戻されず、変更通知も配信も起きない。</summary>
    [Test]
    public void SameValueNotificationIsIgnored()
    {
        SwitchCell cell = new() { On = true };
        Writeback scope = Writeback.For(cell);
        List<string?> changed = [];
        cell.PropertyChanged += (_, args) => changed.Add(args.PropertyName);

        scope.Sink.SwitchCellChanged(scope.CellId, true);
        scope.Gateway.ClearCalls();
        scope.Scope.Flush();

        Assert.That(changed, Is.Empty);
        Assert.That(scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>選択位置の順序・重複だけが違う通知は同値として無視される。</summary>
    [Test]
    public void PickerMultiSelectionIgnoresOrderAndDuplicateOnlyNotifications()
    {
        PickerCell cell = new()
        {
            SelectionMode = PickerSelectionMode.Multiple,
            SelectedIndices = [0, 2],
        };
        Writeback scope = Writeback.For(cell);
        IList<int> before = cell.SelectedIndices!;

        scope.Sink.PickerCellMultiSelectionChanged(scope.CellId, [2, 0, 2]);
        scope.Scope.Flush();

        Assert.That(cell.SelectedIndices, Is.SameAs(before));
        Assert.That(scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>書き戻し後に Native から同じ値が返ってきても、折り返しはそこで止まる。</summary>
    [Test]
    public void WritebackRoundTripConverges()
    {
        SwitchCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.SwitchCellChanged(scope.CellId, true);
        scope.Scope.Flush();
        Assert.That(scope.Gateway.Calls, Has.Count.EqualTo(1));

        scope.Sink.SwitchCellChanged(scope.CellId, true);
        scope.Scope.Flush();

        Assert.That(scope.Gateway.Calls, Has.Count.EqualTo(1));
    }

    /// <summary>書き戻しは TwoWay バインドされた ViewModel へ伝播する。</summary>
    [Test]
    public void WritebackReachesTwoWayBoundViewModel()
    {
        SwitchViewModel viewModel = new();
        SwitchCell cell = new() { BindingContext = viewModel };
        cell.SetBinding(SwitchCell.OnProperty, nameof(SwitchViewModel.IsOn));
        Writeback scope = Writeback.For(cell);

        scope.Sink.SwitchCellChanged(scope.CellId, true);

        Assert.That(viewModel.IsOn, Is.True);
    }

    /// <summary>未知の ID・Cell 種別の食い違いでは何も起きない。</summary>
    [Test]
    public void UnknownCellNotificationDoesNothing()
    {
        SwitchCell cell = new();
        Writeback scope = Writeback.For(cell);

        scope.Sink.SwitchCellChanged("unknown", true);
        scope.Sink.CheckboxCellChanged(scope.CellId, true);
        scope.Scope.Flush();

        Assert.That(cell.On, Is.False);
        Assert.That(scope.Gateway.Calls, Is.Empty);
    }

    /// <summary>Native Host の解放で通知の受け取りを止め、再接続で受け取り直す。</summary>
    [Test]
    public void InteractionsFollowNativeHostLifecycle()
    {
        SwitchCell cell = new();
        Writeback scope = Writeback.For(cell);
        Assert.That(scope.Gateway.AttachInteractionsCount, Is.EqualTo(1));

        scope.Scope.View.ReleaseHost();
        Assert.That(scope.Gateway.Sink, Is.Null);
        Assert.That(scope.Gateway.DetachInteractionsCount, Is.EqualTo(1));

        IKsInteractionSink resumed = Reconnect(scope.Scope);
        resumed.SwitchCellChanged(scope.CellId, true);

        Assert.That(scope.Gateway.AttachInteractionsCount, Is.EqualTo(2));
        Assert.That(cell.On, Is.True);
    }

    /// <summary>同じ SettingsView へ接続し直し、通知の受け口を取り直す。</summary>
    /// <param name="scope">対象の足場</param>
    private static IKsInteractionSink Reconnect(GatewayScope scope)
    {
        scope.Reconnect();
        return scope.Gateway.Sink!;
    }

    /// <summary>TwoWay バインドの相手役。</summary>
    private sealed class SwitchViewModel
    {
        /// <summary>バインド先のプロパティ。</summary>
        public bool IsOn { get; set; }
    }

    /// <summary>Cell 1 件だけを配置して接続した、書き戻しテスト用の足場。</summary>
    /// <param name="Scope">接続した足場</param>
    /// <param name="CellId">対象 Cell の ID</param>
    private sealed record Writeback(GatewayScope Scope, string CellId)
    {
        /// <summary>接続した gateway。</summary>
        public FakeSettingsGateway Gateway => Scope.Gateway;

        /// <summary>ユーザー操作の受け口。</summary>
        public IKsInteractionSink Sink => Scope.Gateway.Sink!;

        /// <summary>指定した Cell だけを持つ設定ツリーを組み立てて接続する。</summary>
        /// <param name="cell">配置する Cell</param>
        public static Writeback For(CellBase cell)
        {
            Section section = new() { Cells = { cell } };
            SettingsView view = new() { Root = { section } };
            GatewayScope scope = GatewayScope.Connect(view).Reset();
            return new Writeback(scope, view.Controller.FindCellId(cell)!);
        }

        /// <summary>flush まで進めて、内容更新として配信された Cell を取り出す。</summary>
        public IReadOnlyList<CellBase> PublishedCells()
        {
            Scope.Flush();
            return
            [
                .. Scope.All<GatewayCall.ReplaceCell>().Select(call => call.NewCell),
                .. Scope.All<GatewayCall.ReplaceCells>()
                    .SelectMany(call => call.Updates.Select(update => update.NewCell)),
            ];
        }
    }
}
