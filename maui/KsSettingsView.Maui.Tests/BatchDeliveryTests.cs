using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// Cell の内容更新が flush までの 1 バッチにまとまり、可視性の変更だけが単発へ分かれることを確認する。
/// </summary>
[TestFixture]
public class BatchDeliveryTests
{
    /// <summary>複数 Cell の内容変更は 1 回の一括更新として配信される。</summary>
    [Test]
    public void MultipleContentChangesAreDeliveredAsOneBatch()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(2);
        string firstId = view.Controller.FindCellId(cells[0])!;
        string secondId = view.Controller.FindCellId(cells[1])!;

        cells[0].Title = "changed";
        cells[1].Title = "changed";
        Assert.That(scope.Calls, Is.Empty);

        scope.Flush();

        IReadOnlyList<GatewayCall.CellUpdate> updates = scope.Single<GatewayCall.ReplaceCells>().Updates;
        Assert.That(updates.Select(update => update.CellId), Is.EqualTo(new[] { firstId, secondId }));
        Assert.That(scope.All<GatewayCall.ReplaceCell>(), Is.Empty);
    }

    /// <summary>1 件だけの内容変更は単発の置き換えとして配信される。</summary>
    [Test]
    public void SingleContentChangeIsDeliveredAsReplaceCell()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(2);
        string cellId = view.Controller.FindCellId(cells[1])!;

        ((LabelCell)cells[1]).ValueText = "value";
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().CellId, Is.EqualTo(cellId));
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>可視性を変えた Cell は複数あっても各 1 件の単発更新として配信される。</summary>
    [Test]
    public void VisibilityChangesAreDeliveredIndividually()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(2);
        string firstId = view.Controller.FindCellId(cells[0])!;
        string secondId = view.Controller.FindCellId(cells[1])!;

        cells[0].IsVisible = false;
        cells[1].IsVisible = false;
        scope.Flush();

        Assert.That(
            scope.All<GatewayCall.ReplaceCell>().Select(call => call.CellId),
            Is.EqualTo(new[] { firstId, secondId }));
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
        Assert.That(cells.All(cell => !cell.IsVisible), Is.True);
    }

    /// <summary>可視性の単発更新は同じバッチの内容更新より先に配信される。</summary>
    [Test]
    public void VisibilityChangeIsSplitFromContentBatchAndSentFirst()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(3);
        string visibilityId = view.Controller.FindCellId(cells[2])!;

        cells[0].Title = "changed";
        cells[1].Title = "changed";
        cells[2].IsVisible = false;
        scope.Flush();

        Assert.That(scope.Calls, Has.Count.EqualTo(2));
        Assert.That(((GatewayCall.ReplaceCell)scope.Calls[0]).CellId, Is.EqualTo(visibilityId));
        Assert.That(((GatewayCall.ReplaceCells)scope.Calls[1]).Updates, Has.Count.EqualTo(2));
    }

    /// <summary>可視性と内容が同時に変わった Cell は単発更新だけで配信される。</summary>
    [Test]
    public void CellWithVisibilityAndContentChangeIsSentOnceAsReplaceCell()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(1);
        string cellId = view.Controller.FindCellId(cells[0])!;

        cells[0].Title = "changed";
        cells[0].IsVisible = false;
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().CellId, Is.EqualTo(cellId));
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
    }

    /// <summary>flush 前に削除された Cell の保留更新は捨てられ、残りだけが配信される。</summary>
    [Test]
    public void PendingUpdateForRemovedCellIsDropped()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(2);
        CellBase removed = cells[0];
        string keptId = view.Controller.FindCellId(cells[1])!;

        removed.Title = "changed";
        cells[1].Title = "changed";
        cells.Remove(removed);

        Assert.That(scope.Flush, Throws.Nothing);
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Is.Empty);
        Assert.That(scope.Single<GatewayCall.ReplaceCell>().CellId, Is.EqualTo(keptId));
    }

    /// <summary>バッチの境界は最初の変更で予約された flush が実行されるまで。</summary>
    [Test]
    public void BatchBoundaryIsTheFirstScheduledFlush()
    {
        (GatewayScope scope, IList<CellBase> cells, SettingsView view) = CreateScope(2);
        string firstId = view.Controller.FindCellId(cells[0])!;

        cells[0].Title = "first";
        Assert.That(scope.Dispatcher.PendingCount, Is.EqualTo(1));
        cells[1].Title = "second";
        Assert.That(scope.Dispatcher.PendingCount, Is.EqualTo(1));

        scope.Flush();
        Assert.That(scope.All<GatewayCall.ReplaceCells>(), Has.Count.EqualTo(1));
        scope.Reset();

        cells[0].Title = "third";
        Assert.That(scope.Dispatcher.PendingCount, Is.EqualTo(1));
        scope.Flush();

        Assert.That(scope.Single<GatewayCall.ReplaceCell>().CellId, Is.EqualTo(firstId));
    }

    /// <summary>輸送内容に影響しないプロパティの変更は flush を予約しない。</summary>
    [Test]
    public void ChangeOfNonTransportedPropertyIsNotDelivered()
    {
        (GatewayScope scope, IList<CellBase> cells, _) = CreateScope(1);

        cells[0].BindingContext = new object();

        Assert.That(scope.Dispatcher.PendingCount, Is.EqualTo(0));
        scope.Flush();
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>Cell を指定数だけ持つ Section を 1 つ構成し、接続済みの足場を返す。</summary>
    private static (GatewayScope Scope, IList<CellBase> Cells, SettingsView View) CreateScope(int cellCount)
    {
        ObservableCollection<CellBase> cells = [];
        for (int i = 0; i < cellCount; i++)
        {
            cells.Add(new LabelCell { Title = $"cell{i}" });
        }

        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();
        return (scope, cells, view);
    }
}
