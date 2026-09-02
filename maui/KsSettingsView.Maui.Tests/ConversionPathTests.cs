using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using KsSettingsView.Internals;
using KsSettingsView.Tests.Fakes;
using KsSettingsView.Tests.Support;
using Microsoft.Maui;
using Microsoft.Maui.Graphics;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// コレクションの構造変更が gateway の構造操作へ 1 対 1 で変換され、対応表が
/// gateway 採番の ID と一致し続けることを確認する。
/// </summary>
[TestFixture]
public class ConversionPathTests
{
    /// <summary>接続時に設定ツリー全体が 1 回の setRoot として送られる。</summary>
    [Test]
    public void ConnectSendsWholeTreeAsSetRoot()
    {
        LabelCell first = new() { Title = "first" };
        LabelCell second = new() { Title = "second" };
        Section section = new() { HeaderText = "header", Cells = { first, second } };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        IReadOnlyList<Section> sent = scope.Single<GatewayCall.SetRoot>().Sections;
        Assert.That(sent, Has.Count.EqualTo(1));
        Assert.That(sent[0], Is.SameAs(section));
        Assert.That(sent[0].Cells, Is.EqualTo(new CellBase[] { first, second }));
    }

    /// <summary>対応表には gateway が採番した ID だけが双方向で載る。</summary>
    [Test]
    public void ConnectRegistersGatewayAssignedIds()
    {
        LabelCell cell = new();
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };

        GatewayScope scope = GatewayScope.Connect(view);

        string sectionId = scope.Gateway.SectionIds.Single();
        string cellId = scope.Gateway.CellIdsOf(sectionId).Single();
        Assert.That(view.Controller.FindSectionId(section), Is.EqualTo(sectionId));
        Assert.That(view.Controller.FindCellId(cell), Is.EqualTo(cellId));
        Assert.That(view.Controller.FindSection(sectionId), Is.SameAs(section));
        Assert.That(view.Controller.FindCell(cellId), Is.SameAs(cell));
    }

    /// <summary>接続前のコレクション操作は配信されず、接続時の setRoot に含まれる。</summary>
    [Test]
    public void StructureBuiltBeforeConnectIsSentOnlyBySetRoot()
    {
        SettingsView view = new();
        Section section = new();
        view.Root.Add(section);
        section.Cells.Add(new LabelCell());

        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(1));
        Assert.That(scope.All<GatewayCall.InsertSection>(), Is.Empty);
        Assert.That(scope.All<GatewayCall.InsertCell>(), Is.Empty);
    }

    /// <summary>Section の追加は insertSection へ変換される。</summary>
    [Test]
    public void AddingSectionIssuesInsertSection()
    {
        SettingsRoot root = [new Section()];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Section added = new() { Cells = { new LabelCell() } };
        root.Add(added);

        GatewayCall.InsertSection call = scope.Single<GatewayCall.InsertSection>();
        Assert.That(call.Section, Is.SameAs(added));
        Assert.That(call.Index, Is.EqualTo(1));
        Assert.That(view.Controller.FindSectionId(added), Is.EqualTo(scope.Gateway.SectionIds[1]));
        Assert.That(view.Controller.FindCellId(added.Cells[0]), Is.Not.Null);
    }

    /// <summary>Section の削除は removeSection へ変換され、対応表からも外れる。</summary>
    [Test]
    public void RemovingSectionIssuesRemoveSection()
    {
        Section section = new() { Cells = { new LabelCell() } };
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        CellBase cell = section.Cells[0];
        scope.Reset();

        root.Remove(section);

        Assert.That(scope.Single<GatewayCall.RemoveSection>().SectionId, Is.EqualTo(sectionId));
        Assert.That(view.Controller.FindSectionId(section), Is.Null);
        Assert.That(view.Controller.FindCellId(cell), Is.Null);
        Assert.That(view.Controller.FindSection(sectionId), Is.Null);
    }

    /// <summary>Section の移動は moveSection へ変換される。</summary>
    [Test]
    public void MovingSectionIssuesMoveSection()
    {
        SettingsRoot root = [new Section(), new Section(), new Section()];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        string movedId = view.Controller.FindSectionId(root[0])!;
        scope.Reset();

        root.Move(0, 2);

        GatewayCall.MoveSection call = scope.Single<GatewayCall.MoveSection>();
        Assert.That(call.From, Is.EqualTo(0));
        Assert.That(call.To, Is.EqualTo(2));
        Assert.That(scope.Gateway.SectionIds[2], Is.EqualTo(movedId));
    }

    /// <summary>Section の置き換えは replaceSection へ変換され、Section の ID は維持される。</summary>
    [Test]
    public void ReplacingSectionIssuesReplaceSectionAndKeepsId()
    {
        Section original = new();
        SettingsRoot root = [original];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(original)!;
        scope.Reset();

        Section replacement = new() { Cells = { new LabelCell() } };
        root[0] = replacement;

        GatewayCall.ReplaceSection call = scope.Single<GatewayCall.ReplaceSection>();
        Assert.That(call.SectionId, Is.EqualTo(sectionId));
        Assert.That(call.NewSection, Is.SameAs(replacement));
        Assert.That(view.Controller.FindSectionId(replacement), Is.EqualTo(sectionId));
        Assert.That(view.Controller.FindSectionId(original), Is.Null);
        Assert.That(view.Controller.FindCellId(replacement.Cells[0]), Is.Not.Null);
    }

    /// <summary>Cell の追加は insertCell へ変換される。</summary>
    [Test]
    public void AddingCellIssuesInsertCell()
    {
        ObservableCollection<CellBase> cells = [new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        LabelCell added = new();
        cells.Insert(0, added);

        GatewayCall.InsertCell call = scope.Single<GatewayCall.InsertCell>();
        Assert.That(call.Cell, Is.SameAs(added));
        Assert.That(call.SectionId, Is.EqualTo(sectionId));
        Assert.That(call.Index, Is.EqualTo(0));
        Assert.That(view.Controller.FindCellId(added), Is.EqualTo(scope.Gateway.CellIdsOf(sectionId)[0]));
    }

    /// <summary>Cell の削除は removeCell へ変換される。</summary>
    [Test]
    public void RemovingCellIssuesRemoveCell()
    {
        LabelCell cell = new();
        ObservableCollection<CellBase> cells = [cell];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string cellId = view.Controller.FindCellId(cell)!;
        scope.Reset();

        cells.Remove(cell);

        Assert.That(scope.Single<GatewayCall.RemoveCell>().CellId, Is.EqualTo(cellId));
        Assert.That(view.Controller.FindCellId(cell), Is.Null);
        Assert.That(view.Controller.FindCell(cellId), Is.Null);
    }

    /// <summary>Cell の移動は moveCell へ変換され、入れ替え後の順序になる。</summary>
    [Test]
    public void MovingCellIssuesMoveCell()
    {
        ObservableCollection<CellBase> cells = [new LabelCell(), new LabelCell(), new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        List<string> before = [.. scope.Gateway.CellIdsOf(sectionId)];
        scope.Reset();

        cells.Move(0, 2);

        GatewayCall.MoveCell call = scope.Single<GatewayCall.MoveCell>();
        Assert.That(call.CellId, Is.EqualTo(before[0]));
        Assert.That(call.Index, Is.EqualTo(2));
        Assert.That(
            scope.Gateway.CellIdsOf(sectionId),
            Is.EqualTo(new[] { before[1], before[2], before[0] }));
    }

    /// <summary>Cell の置き換えは replaceCell へ変換され、Cell の ID は維持される。</summary>
    [Test]
    public void ReplacingCellIssuesReplaceCellAndKeepsId()
    {
        LabelCell original = new();
        ObservableCollection<CellBase> cells = [original];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string cellId = view.Controller.FindCellId(original)!;
        scope.Reset();

        LabelCell replacement = new();
        cells[0] = replacement;

        GatewayCall.ReplaceCell call = scope.Single<GatewayCall.ReplaceCell>();
        Assert.That(call.CellId, Is.EqualTo(cellId));
        Assert.That(call.NewCell, Is.SameAs(replacement));
        Assert.That(view.Controller.FindCellId(replacement), Is.EqualTo(cellId));
        Assert.That(view.Controller.FindCellId(original), Is.Null);
    }

    /// <summary>複数 Section の前方へのまとめ移動でも、移動範囲の並びが保たれる。</summary>
    [Test]
    public void ForwardRangeSectionMoveKeepsOrder()
    {
        RangeMoveCollection<Section> root = [new Section(), new Section(), new Section(), new Section()];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        List<string> before = [.. scope.Gateway.SectionIds];
        scope.Reset();

        root.MoveRange(0, 2, 2);

        Assert.That(
            scope.Gateway.SectionIds,
            Is.EqualTo(new[] { before[2], before[3], before[0], before[1] }));
        Assert.That(
            root.Select(section => view.Controller.FindSectionId(section)),
            Is.EqualTo(scope.Gateway.SectionIds));
    }

    /// <summary>複数 Section の後方へのまとめ移動でも、移動範囲の並びが保たれる。</summary>
    [Test]
    public void BackwardRangeSectionMoveKeepsOrder()
    {
        RangeMoveCollection<Section> root = [new Section(), new Section(), new Section(), new Section()];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view);
        List<string> before = [.. scope.Gateway.SectionIds];
        scope.Reset();

        root.MoveRange(2, 0, 2);

        Assert.That(
            scope.Gateway.SectionIds,
            Is.EqualTo(new[] { before[2], before[3], before[0], before[1] }));
        Assert.That(
            root.Select(section => view.Controller.FindSectionId(section)),
            Is.EqualTo(scope.Gateway.SectionIds));
    }

    /// <summary>複数 Cell の前方へのまとめ移動でも、移動範囲の並びが保たれる。</summary>
    [Test]
    public void ForwardRangeCellMoveKeepsOrder()
    {
        RangeMoveCollection<CellBase> cells =
            [new LabelCell(), new LabelCell(), new LabelCell(), new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        List<string> before = [.. scope.Gateway.CellIdsOf(sectionId)];
        scope.Reset();

        cells.MoveRange(0, 2, 2);

        Assert.That(
            scope.Gateway.CellIdsOf(sectionId),
            Is.EqualTo(new[] { before[2], before[3], before[0], before[1] }));
        Assert.That(
            cells.Select(cell => view.Controller.FindCellId(cell)),
            Is.EqualTo(scope.Gateway.CellIdsOf(sectionId)));
    }

    /// <summary>複数 Cell の後方へのまとめ移動でも、移動範囲の並びが保たれる。</summary>
    [Test]
    public void BackwardRangeCellMoveKeepsOrder()
    {
        RangeMoveCollection<CellBase> cells =
            [new LabelCell(), new LabelCell(), new LabelCell(), new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        List<string> before = [.. scope.Gateway.CellIdsOf(sectionId)];
        scope.Reset();

        cells.MoveRange(2, 0, 2);

        Assert.That(
            scope.Gateway.CellIdsOf(sectionId),
            Is.EqualTo(new[] { before[2], before[3], before[0], before[1] }));
        Assert.That(
            cells.Select(cell => view.Controller.FindCellId(cell)),
            Is.EqualTo(scope.Gateway.CellIdsOf(sectionId)));
    }

    /// <summary>Root の Clear は setRoot による再構築として配信される。</summary>
    [Test]
    public void ClearingRootRebuildsWithSetRoot()
    {
        Section section = new() { Cells = { new LabelCell() } };
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        root.Clear();

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Is.Empty);
        Assert.That(view.Controller.FindSectionId(section), Is.Null);
        Assert.That(scope.Gateway.SectionIds, Is.Empty);
    }

    /// <summary>Cells の Clear も setRoot による再構築として配信される。</summary>
    [Test]
    public void ClearingCellsRebuildsWithSetRoot()
    {
        ObservableCollection<CellBase> cells = [new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        cells.Clear();

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(1));
        string sectionId = view.Controller.FindSectionId(section)!;
        Assert.That(scope.Gateway.CellIdsOf(sectionId), Is.Empty);
    }

    /// <summary>observable でない Root への操作は表示に反映されない。</summary>
    [Test]
    public void StaticRootCollectionIsNotObserved()
    {
        List<Section> root = [new Section()];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        root.Add(new Section());

        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>observable でない Cells への操作は表示に反映されない。</summary>
    [Test]
    public void StaticCellsCollectionIsNotObserved()
    {
        List<CellBase> cells = [new LabelCell()];
        Section section = new() { Cells = cells };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        cells.Add(new LabelCell());

        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>Root の差し替えで再構築し、旧コレクションの操作は反映されなくなる。</summary>
    [Test]
    public void ReplacingRootRebuildsAndDropsOldCollection()
    {
        SettingsView view = new() { Root = { new Section() } };
        GatewayScope scope = GatewayScope.Connect(view);
        IList<Section> old = view.Root;
        scope.Reset();

        ObservableCollection<Section> replacement = [new Section(), new Section()];
        view.Root = replacement;

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(2));
        scope.Reset();

        old.Add(new Section());
        Assert.That(scope.Calls, Is.Empty);

        replacement.Add(new Section());
        Assert.That(scope.All<GatewayCall.InsertSection>(), Has.Count.EqualTo(1));
    }

    /// <summary>Cells の差し替えは replaceSection へ変換され、Section の ID は維持される。</summary>
    [Test]
    public void ReplacingCellsIssuesReplaceSectionAndKeepsSectionId()
    {
        LabelCell original = new();
        Section section = new() { Cells = { original } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string sectionId = view.Controller.FindSectionId(section)!;
        scope.Reset();

        ObservableCollection<CellBase> replacement = [new LabelCell()];
        section.Cells = replacement;

        Assert.That(scope.Single<GatewayCall.ReplaceSection>().SectionId, Is.EqualTo(sectionId));
        Assert.That(view.Controller.FindSectionId(section), Is.EqualTo(sectionId));
        Assert.That(view.Controller.FindCellId(original), Is.Null);
        Assert.That(view.Controller.FindCellId(replacement[0]), Is.Not.Null);
        scope.Reset();

        replacement.Add(new LabelCell());
        Assert.That(scope.All<GatewayCall.InsertCell>(), Has.Count.EqualTo(1));
    }

    /// <summary>Cells の差し替えで旧コレクションの購読は外れ、以後の操作は配信されない。</summary>
    [Test]
    public void ReplacingCellsDropsOldCollection()
    {
        ObservableCollection<CellBase> old = [new LabelCell()];
        Section section = new() { Cells = old };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);

        section.Cells = new ObservableCollection<CellBase> { new LabelCell() };
        scope.Reset();

        old.Add(new LabelCell());

        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>初回接続が失敗すると未接続へ戻り、設定ツリーを直せば接続し直せる。</summary>
    [Test]
    public void FailedConnectRollsBackAndAllowsReconnect()
    {
        LabelCell shared = new();
        Section first = new() { Cells = { shared } };
        Section second = new() { Cells = { shared } };
        SettingsView view = new() { Root = { first, second } };

        Assert.That(
            () => GatewayScope.Connect(view),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));
        Assert.That(view.Controller.IsConnected, Is.False);
        Assert.That(view.Controller.Gateway, Is.Null);

        second.Cells.Remove(shared);
        GatewayScope scope = GatewayScope.Connect(view);

        Assert.That(scope.Single<GatewayCall.SetRoot>().Sections, Has.Count.EqualTo(2));
        Assert.That(view.Controller.FindCellId(shared), Is.Not.Null);
        scope.Reset();

        second.Cells.Add(new LabelCell());

        Assert.That(scope.All<GatewayCall.InsertCell>(), Has.Count.EqualTo(1));
    }

    /// <summary>配置済みの Cell を別の Section へ追加すると例外になり、表示は変わらない。</summary>
    [Test]
    public void AddingCellPlacedElsewhereThrowsWithoutTouchingDisplay()
    {
        LabelCell shared = new();
        Section first = new() { Cells = { shared } };
        Section second = new();
        SettingsView view = new() { Root = { first, second } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Assert.That(
            () => second.Cells.Add(shared),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>配置済みの Section を Root へ再度追加すると例外になり、表示は変わらない。</summary>
    [Test]
    public void AddingSectionPlacedElsewhereThrowsWithoutTouchingDisplay()
    {
        Section section = new();
        SettingsRoot root = [section];
        SettingsView view = new() { Root = root };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        Assert.That(
            () => root.Add(section),
            Throws.InvalidOperationException.With.Message.Contains("same Section instance"));
        Assert.That(scope.Calls, Is.Empty);
    }

    /// <summary>同じ Cell を 2 箇所に持つ設定ツリーは接続時に例外になる。</summary>
    [Test]
    public void ConnectingTreeWithDuplicateCellThrows()
    {
        LabelCell shared = new();
        Section first = new() { Cells = { shared } };
        Section second = new() { Cells = { shared } };
        SettingsView view = new() { Root = { first, second } };

        Assert.That(
            () => GatewayScope.Connect(view),
            Throws.InvalidOperationException.With.Message.Contains("same Cell instance"));
    }

    // ---- Cell 種別ごとの輸送内容への変換 ----

    /// <summary>LabelCell は値文字列を持つ表示行として写される。</summary>
    [Test]
    public void LabelCellConvertsToLabelSnapshot()
    {
        LabelCell cell = new() { Title = "版", ValueText = "1.0.0" };

        KsLabelCellSnapshot snapshot = Snapshot<KsLabelCellSnapshot>(cell);

        Assert.That(snapshot.Title, Is.EqualTo("版"));
        Assert.That(snapshot.ValueText, Is.EqualTo("1.0.0"));
    }

    /// <summary>CommandCell は値文字列と矢印の指定を持つ行として写される。</summary>
    [Test]
    public void CommandCellConvertsToCommandSnapshot()
    {
        CommandCell cell = new() { Title = "詳細", ValueText = "設定", HideArrow = true };

        KsCommandCellSnapshot snapshot = Snapshot<KsCommandCellSnapshot>(cell);

        Assert.That(snapshot.Title, Is.EqualTo("詳細"));
        Assert.That(snapshot.ValueText, Is.EqualTo("設定"));
        Assert.That(snapshot.HideArrow, Is.True);
        Assert.That(snapshot.IsEnabled, Is.True);
    }

    /// <summary>ButtonCell は値文字列と揃え位置を持ち、説明文を持たない行として写される。</summary>
    [Test]
    public void ButtonCellConvertsToButtonSnapshot()
    {
        ButtonCell cell = new()
        {
            Title = "削除",
            ValueText = "送信",
            TitleAlignment = TextAlignment.Start,
            Description = "説明",
        };

        KsButtonCellSnapshot snapshot = Snapshot<KsButtonCellSnapshot>(cell);

        Assert.That(snapshot.Title, Is.EqualTo("削除"));
        Assert.That(snapshot.ValueText, Is.EqualTo("送信"));
        Assert.That(snapshot.TitleAlignment, Is.EqualTo(TextAlignment.Start));
        Assert.That(snapshot.Description, Is.Null);
    }

    /// <summary>SwitchCell は ON/OFF 値と値文字列を持つ行として写される。</summary>
    [Test]
    public void SwitchCellConvertsToSwitchSnapshot()
    {
        SwitchCell cell = new() { Title = "通知", ValueText = "オン", On = true };

        KsSwitchCellSnapshot snapshot = Snapshot<KsSwitchCellSnapshot>(cell);

        Assert.That(snapshot.IsOn, Is.True);
        Assert.That(snapshot.ValueText, Is.EqualTo("オン"));
    }

    /// <summary>CheckboxCell はチェック状態と値文字列を持つ行として写される。</summary>
    [Test]
    public void CheckboxCellConvertsToCheckboxSnapshot()
    {
        CheckboxCell cell = new() { Title = "同意", ValueText = "済", Checked = true };

        KsCheckboxCellSnapshot snapshot = Snapshot<KsCheckboxCellSnapshot>(cell);

        Assert.That(snapshot.IsChecked, Is.True);
        Assert.That(snapshot.ValueText, Is.EqualTo("済"));
    }

    /// <summary>SimpleCheckCell はチェック状態と値文字列を持つ行として写される。</summary>
    [Test]
    public void SimpleCheckCellConvertsToSimpleCheckSnapshot()
    {
        SimpleCheckCell cell = new() { Title = "既定", ValueText = "有効", Checked = true };

        KsSimpleCheckCellSnapshot snapshot = Snapshot<KsSimpleCheckCellSnapshot>(cell);

        Assert.That(snapshot.IsChecked, Is.True);
        Assert.That(snapshot.ValueText, Is.EqualTo("有効"));
    }

    /// <summary>RadioCell はグループと値の組を持ち、値文字列は表示専用として別に写される。</summary>
    [Test]
    public void RadioCellConvertsToRadioSnapshot()
    {
        RadioCell cell = new()
        {
            Title = "ダーク",
            ValueText = "推奨",
            GroupId = "theme",
            Value = "dark",
            SelectedValue = "light",
        };

        KsRadioCellSnapshot snapshot = Snapshot<KsRadioCellSnapshot>(cell);

        Assert.That(snapshot.GroupId, Is.EqualTo("theme"));
        Assert.That(snapshot.Value, Is.EqualTo("dark"));
        Assert.That(snapshot.SelectedValue, Is.EqualTo("light"));
        Assert.That(snapshot.ValueText, Is.EqualTo("推奨"));
    }

    /// <summary>EntryCell の入力値は text として写され、keyboard は正規化した種別になる。</summary>
    [Test]
    public void EntryCellConvertsToEntrySnapshot()
    {
        EntryCell cell = new()
        {
            Title = "名前",
            ValueText = "kamu",
            Placeholder = "未入力",
            PlaceholderColor = Colors.Red,
            Keyboard = Keyboard.Email,
            IsPassword = true,
            TextAlignment = TextAlignment.Center,
            MaxLength = 20,
        };

        KsEntryCellSnapshot snapshot = Snapshot<KsEntryCellSnapshot>(cell);

        Assert.That(snapshot.Text, Is.EqualTo("kamu"));
        Assert.That(snapshot.Placeholder, Is.EqualTo("未入力"));
        Assert.That(snapshot.PlaceholderColor, Is.EqualTo(unchecked((int)0xFFFF0000)));
        Assert.That(snapshot.Keyboard, Is.EqualTo(KsKeyboardKind.Email));
        Assert.That(snapshot.IsPassword, Is.True);
        Assert.That(snapshot.TextAlignment, Is.EqualTo(TextAlignment.Center));
        Assert.That(snapshot.MaxLength, Is.EqualTo(20));
    }

    /// <summary>PickerCell は選択 index と項目を持つ行として写される。</summary>
    [Test]
    public void PickerCellConvertsToPickerSnapshot()
    {
        PickerCell cell = new()
        {
            Title = "テーマ",
            ItemsSource = new List<string> { "ライト", "ダーク" },
            SelectionMode = PickerSelectionMode.Multiple,
            SelectedIndices = [2, 0, 2],
            MaxSelectedNumber = 3,
            PageTitle = "選択",
            ValueText = "指定",
        };

        KsPickerCellSnapshot snapshot = Snapshot<KsPickerCellSnapshot>(cell);

        Assert.That(snapshot.Items.Select(static item => item.Text), Is.EqualTo(new[] { "ライト", "ダーク" }));
        Assert.That(snapshot.Items.Select(static item => item.SubText), Is.EqualTo(new string?[] { null, null }));
        Assert.That(snapshot.SelectionMode, Is.EqualTo(PickerSelectionMode.Multiple));
        Assert.That(snapshot.SelectedIndices, Is.EqualTo(new[] { 0, 2 }));
        Assert.That(snapshot.MaxSelectedNumber, Is.EqualTo(3));
        Assert.That(snapshot.PageTitle, Is.EqualTo("選択"));
        Assert.That(snapshot.ValueText, Is.EqualTo("指定"));
    }

    /// <summary>NumberPickerCell は範囲と現在値を持つ行として写される。</summary>
    [Test]
    public void NumberPickerCellConvertsToNumberPickerSnapshot()
    {
        NumberPickerCell cell = new()
        {
            Title = "個数",
            Min = 1,
            Max = 10,
            Step = 2,
            Number = 5,
            Unit = "個",
            PickerTitle = "選ぶ",
        };

        KsNumberPickerCellSnapshot snapshot = Snapshot<KsNumberPickerCellSnapshot>(cell);

        Assert.That(snapshot.Min, Is.EqualTo(1));
        Assert.That(snapshot.Max, Is.EqualTo(10));
        Assert.That(snapshot.Step, Is.EqualTo(2));
        Assert.That(snapshot.Number, Is.EqualTo(5));
        Assert.That(snapshot.Unit, Is.EqualTo("個"));
        Assert.That(snapshot.PickerTitle, Is.EqualTo("選ぶ"));
    }

    /// <summary>TimePickerCell の時刻は固定書式の壁時計値として写される。</summary>
    [Test]
    public void TimePickerCellConvertsTimeToWallClockText()
    {
        TimePickerCell cell = new()
        {
            Title = "アラーム",
            Time = new TimeSpan(9, 5, 0),
            Format = "HH:mm",
            PickerTitle = "時刻",
        };

        KsTimePickerCellSnapshot snapshot = Snapshot<KsTimePickerCellSnapshot>(cell);

        Assert.That(snapshot.Time, Is.EqualTo("09:05"));
        Assert.That(snapshot.Format, Is.EqualTo("HH:mm"));
        Assert.That(snapshot.PickerTitle, Is.EqualTo("時刻"));
    }

    /// <summary>TimePickerCell の時制は未指定なら 24時間制として写される。</summary>
    [Test]
    public void TimePickerCellCarriesDefaultHourCycleAs24Hour()
    {
        TimePickerCell cell = new() { Title = "アラーム" };

        KsTimePickerCellSnapshot snapshot = Snapshot<KsTimePickerCellSnapshot>(cell);

        Assert.That(cell.Is24Hour, Is.True);
        Assert.That(snapshot.Is24Hour, Is.True);
    }

    /// <summary>TimePickerCell の 12時間制の指定はそのまま写される (表示フォーマットとは独立)。</summary>
    [Test]
    public void TimePickerCellCarriesExplicitTwelveHourCycle()
    {
        TimePickerCell cell = new()
        {
            Title = "就寝",
            Format = "HH:mm",
            Is24Hour = false,
        };

        KsTimePickerCellSnapshot snapshot = Snapshot<KsTimePickerCellSnapshot>(cell);

        Assert.That(snapshot.Is24Hour, Is.False);
        Assert.That(snapshot.Format, Is.EqualTo("HH:mm"));
    }

    /// <summary>表示済み TimePickerCell の時制変更は、写しを伴う更新として gateway へ送られる。</summary>
    [Test]
    public void TimePickerCellHourCycleChangeIsDeliveredToGateway()
    {
        TimePickerCell cell = new() { Title = "就寝" };
        Section section = new() { Cells = { cell } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view);
        string cellId = view.Controller.FindCellId(cell)!;
        scope.Reset();

        cell.Is24Hour = false;
        scope.Flush();

        GatewayCall.ReplaceCell call = scope.Single<GatewayCall.ReplaceCell>();
        Assert.That(call.CellId, Is.EqualTo(cellId));
        Assert.That(call.NewCell, Is.SameAs(cell));
        Assert.That(((KsTimePickerCellSnapshot)call.Snapshot).Is24Hour, Is.False);
    }

    /// <summary>DatePickerCell の日付は固定書式の壁時計値として写される。</summary>
    [Test]
    public void DatePickerCellConvertsDateToWallClockText()
    {
        DatePickerCell cell = new()
        {
            Title = "期限",
            Date = new DateTime(2026, 8, 10, 23, 59, 0),
            MinimumDate = new DateTime(2026, 1, 1),
            MaximumDate = new DateTime(2026, 12, 31),
            Format = "yyyy/MM/dd",
            TodayText = "今日",
            PickerTitle = "日付",
            UIStyle = DatePickerUIStyle.Wheels,
        };

        KsDatePickerCellSnapshot snapshot = Snapshot<KsDatePickerCellSnapshot>(cell);

        Assert.That(snapshot.Date, Is.EqualTo("2026-08-10"));
        Assert.That(snapshot.MinDate, Is.EqualTo("2026-01-01"));
        Assert.That(snapshot.MaxDate, Is.EqualTo("2026-12-31"));
        Assert.That(snapshot.Format, Is.EqualTo("yyyy/MM/dd"));
        Assert.That(snapshot.TodayText, Is.EqualTo("今日"));
        Assert.That(snapshot.PickerTitle, Is.EqualTo("日付"));
        Assert.That(snapshot.UIStyle, Is.EqualTo(DatePickerUIStyle.Wheels));
    }

    /// <summary>選択面の形式と揃え位置は、両 OS 共通の序数として輸送される。</summary>
    [Test]
    public void EnumsAreCarriedAsSharedOrdinals()
    {
        Assert.That(KsWireValues.UIStyle(DatePickerUIStyle.Calendar), Is.Zero);
        Assert.That(KsWireValues.UIStyle(DatePickerUIStyle.Wheels), Is.EqualTo(1));
        Assert.That(KsWireValues.UIStyle(null), Is.Null);

        Assert.That(KsWireValues.SelectionMode(PickerSelectionMode.Single), Is.Zero);
        Assert.That(KsWireValues.SelectionMode(PickerSelectionMode.Multiple), Is.EqualTo(1));

        Assert.That(KsWireValues.Alignment(TextAlignment.Start), Is.Zero);
        Assert.That(KsWireValues.Alignment(TextAlignment.Center), Is.EqualTo(1));
        Assert.That(KsWireValues.Alignment(TextAlignment.End), Is.EqualTo(2));
        Assert.That(KsWireValues.Alignment(null), Is.Null);
    }

    /// <summary>キーボード種別は標準キーボードごとの序数へ正規化される。</summary>
    [Test]
    public void KeyboardIsNormalizedToSharedOrdinals()
    {
        Assert.That((int)KsWireValues.Keyboard(null), Is.Zero);
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Default), Is.Zero);
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Plain), Is.EqualTo(1));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Text), Is.EqualTo(2));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Chat), Is.EqualTo(3));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Url), Is.EqualTo(4));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Email), Is.EqualTo(5));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Numeric), Is.EqualTo(6));
        Assert.That((int)KsWireValues.Keyboard(Keyboard.Telephone), Is.EqualTo(7));
    }

    /// <summary>異種の Cell は同じバッチに混ざって配信される。</summary>
    [Test]
    public void MixedCellKindsShareOneBatch()
    {
        LabelCell label = new();
        SwitchCell switchCell = new();
        DatePickerCell date = new();
        Section section = new() { Cells = { label, switchCell, date } };
        SettingsView view = new() { Root = { section } };
        GatewayScope scope = GatewayScope.Connect(view).Reset();

        label.ValueText = "値";
        switchCell.On = true;
        date.Date = new DateTime(2026, 8, 10);
        scope.Flush();

        IReadOnlyList<GatewayCall.CellUpdate> updates = scope.Single<GatewayCall.ReplaceCells>().Updates;
        Assert.That(updates.Select(update => update.NewCell), Is.EqualTo(new CellBase[] { label, switchCell, date }));
    }

    /// <summary>指定した Cell の輸送内容を、期待する種別として取り出す。</summary>
    /// <typeparam name="T">期待する輸送内容の種別</typeparam>
    /// <param name="cell">写し取る Cell</param>
    private static T Snapshot<T>(CellBase cell)
        where T : KsCellSnapshot
    {
        KsCellSnapshot snapshot = cell.CreateSnapshot();
        Assert.That(snapshot, Is.InstanceOf<T>());
        return (T)snapshot;
    }
}
