using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Maui.Internals;

namespace KsSettingsView.Maui.Tests.Fakes;

/// <summary>
/// Bridge を持たないテスト用の gateway。
/// </summary>
/// <remarks>
/// 呼び出しを <see cref="Calls"/> へ順に記録し、Section / Cell の ID を自分で採番する。
/// 未知の ID を渡された操作は no-op になり戻り値は null という Bridge 側の契約と、
/// 範囲外 index が端へ丸められる契約を再現する。accessory の更新だけはこの ID 検査の
/// 対象外であり、削除済みの ID を渡しても素通しする。
/// </remarks>
internal sealed class FakeSettingsGateway : IKsSettingsGateway
{
    private readonly List<GatewayCall> _calls = [];
    private readonly List<FakeSection> _sections = [];
    private int _sectionSeed;
    private int _cellSeed;

    /// <summary>記録された呼び出しの並び。</summary>
    public IReadOnlyList<GatewayCall> Calls => _calls;

    /// <summary>現在の Section の ID (配置順)。</summary>
    public IReadOnlyList<string> SectionIds => _sections.Select(section => section.Id).ToList();

    /// <summary><see cref="ReleaseHost"/> が呼ばれた回数。</summary>
    public int ReleaseHostCount { get; private set; }

    /// <summary>
    /// 差し込まれているユーザー操作の通知先。未接続なら null。
    /// </summary>
    /// <remarks>Native からの通知はこの受け口を直接呼んで模擬する。</remarks>
    public IKsInteractionSink? Sink { get; private set; }

    /// <summary><see cref="AttachInteractions"/> が実際に通知先を差し込んだ回数。</summary>
    public int AttachInteractionsCount { get; private set; }

    /// <summary><see cref="DetachInteractions"/> が呼ばれた回数。</summary>
    public int DetachInteractionsCount { get; private set; }

    /// <summary>最後に適用された既定スタイル。未適用なら null。</summary>
    public KsThemeSnapshot? Theme { get; private set; }

    /// <summary>最後に適用された見た目スタイル。未適用なら null。</summary>
    public SettingsViewStyle? Style { get; private set; }

    /// <summary>差し込まれた icon の引き当て先。未差し込みなら null。</summary>
    public IKsIconStore? Icons { get; private set; }

    /// <summary>差し込まれた platform view の引き当て先。未差し込みなら null。</summary>
    public IKsPlatformViewStore? PlatformViews { get; private set; }

    /// <summary>指定 Section 配下の Cell の ID (配置順)。未知の ID では空。</summary>
    /// <param name="sectionId">対象 Section の ID</param>
    public IReadOnlyList<string> CellIdsOf(string sectionId)
        => Find(sectionId)?.CellIds.ToList() ?? (IReadOnlyList<string>)[];

    /// <summary>記録済みの呼び出しを捨てる。</summary>
    public void ClearCalls() => _calls.Clear();

    /// <inheritdoc/>
    public IReadOnlyList<KsSectionIdentity> SetRoot(IReadOnlyList<Section> sections)
    {
        ArgumentNullException.ThrowIfNull(sections);
        _calls.Add(new GatewayCall.SetRoot(sections.ToList()));

        _sections.Clear();
        List<KsSectionIdentity> identities = new(sections.Count);
        foreach (Section section in sections)
        {
            FakeSection created = Create(section);
            _sections.Add(created);
            identities.Add(created.ToIdentity());
        }

        return identities;
    }

    /// <inheritdoc/>
    public KsSectionIdentity? InsertSection(Section section, int index)
    {
        ArgumentNullException.ThrowIfNull(section);
        _calls.Add(new GatewayCall.InsertSection(section, index));

        FakeSection created = Create(section);
        _sections.Insert(Math.Clamp(index, 0, _sections.Count), created);
        return created.ToIdentity();
    }

    /// <inheritdoc/>
    public void RemoveSection(string sectionId)
    {
        _calls.Add(new GatewayCall.RemoveSection(sectionId));

        FakeSection? target = Find(sectionId);
        if (target is not null)
        {
            _sections.Remove(target);
        }
    }

    /// <inheritdoc/>
    public void MoveSection(int from, int to)
    {
        _calls.Add(new GatewayCall.MoveSection(from, to));

        if (_sections.Count == 0)
        {
            return;
        }

        int source = Math.Clamp(from, 0, _sections.Count - 1);
        int destination = Math.Clamp(to, 0, _sections.Count - 1);
        FakeSection moved = _sections[source];
        _sections.RemoveAt(source);
        _sections.Insert(destination, moved);
    }

    /// <inheritdoc/>
    public KsSectionIdentity? ReplaceSection(
        string sectionId,
        Section newSection,
        IReadOnlyList<string> retainedCellIds)
    {
        ArgumentNullException.ThrowIfNull(newSection);
        ArgumentNullException.ThrowIfNull(retainedCellIds);
        _calls.Add(new GatewayCall.ReplaceSection(sectionId, newSection, retainedCellIds.ToList()));

        FakeSection? target = Find(sectionId);
        if (target is null)
        {
            return null;
        }

        target.CellIds.Clear();
        for (int i = 0; i < newSection.Cells.Count; i++)
        {
            target.CellIds.Add(i < retainedCellIds.Count ? retainedCellIds[i] : NextCellId());
        }

        return target.ToIdentity();
    }

    /// <inheritdoc/>
    public string? InsertCell(CellBase cell, string sectionId, int index)
    {
        ArgumentNullException.ThrowIfNull(cell);
        _calls.Add(new GatewayCall.InsertCell(cell, sectionId, index));

        FakeSection? target = Find(sectionId);
        if (target is null)
        {
            return null;
        }

        string cellId = NextCellId();
        target.CellIds.Insert(Math.Clamp(index, 0, target.CellIds.Count), cellId);
        return cellId;
    }

    /// <inheritdoc/>
    public void RemoveCell(string cellId)
    {
        _calls.Add(new GatewayCall.RemoveCell(cellId));

        FindCellOwner(cellId)?.CellIds.Remove(cellId);
    }

    /// <inheritdoc/>
    public void MoveCell(string cellId, int index)
    {
        _calls.Add(new GatewayCall.MoveCell(cellId, index));

        FakeSection? owner = FindCellOwner(cellId);
        if (owner is null)
        {
            return;
        }

        owner.CellIds.Remove(cellId);
        owner.CellIds.Insert(Math.Clamp(index, 0, owner.CellIds.Count), cellId);
    }

    /// <inheritdoc/>
    public string? ReplaceCell(string cellId, CellBase newCell)
    {
        ArgumentNullException.ThrowIfNull(newCell);

        // 実装の gateway は呼び出しの時点で輸送 DTO を組み立てるため、そのときの写しと
        // 引き当て結果もあわせて記録する。
        _calls.Add(new GatewayCall.ReplaceCell(
            cellId,
            newCell,
            newCell.CreateSnapshot(),
            CellContentViewOf(newCell)));

        return FindCellOwner(cellId) is null ? null : cellId;
    }

    /// <inheritdoc/>
    public void ReplaceCells(IReadOnlyList<KsCellUpdate> updates)
    {
        ArgumentNullException.ThrowIfNull(updates);

        // 単発の置き換えと同じく、呼び出しの時点の写しと引き当て結果を各件について記録する。
        _calls.Add(new GatewayCall.ReplaceCells(
        [
            .. updates.Select(update => new GatewayCall.CellUpdate(
                update.CellId,
                update.Cell,
                update.Cell.CreateSnapshot(),
                CellContentViewOf(update.Cell))),
        ]));
    }

    /// <inheritdoc/>
    public void UpdateAccessory(KsAccessoryTarget target, string? sectionId, string? text)
        => _calls.Add(new GatewayCall.UpdateAccessory(target, sectionId, text));

    /// <inheritdoc/>
    public void UpdateAccessoryView(KsAccessoryTarget target, string? sectionId, object? view)
        => _calls.Add(new GatewayCall.UpdateAccessoryView(target, sectionId, view));

    /// <inheritdoc/>
    public void InvalidateAccessoryMeasurement(KsAccessoryTarget target, string? sectionId)
        => _calls.Add(new GatewayCall.InvalidateAccessoryMeasurement(target, sectionId));

    /// <inheritdoc/>
    public void SetTheme(KsThemeSnapshot theme)
    {
        ArgumentNullException.ThrowIfNull(theme);
        _calls.Add(new GatewayCall.SetTheme(theme));
        Theme = theme;
    }

    /// <inheritdoc/>
    public void SetStyle(SettingsViewStyle style)
    {
        _calls.Add(new GatewayCall.SetStyle(style));
        Style = style;
    }

    /// <inheritdoc/>
    public void AttachIcons(IKsIconStore icons)
    {
        ArgumentNullException.ThrowIfNull(icons);
        Icons = icons;
    }

    /// <inheritdoc/>
    public void AttachPlatformViews(IKsPlatformViewStore views)
    {
        ArgumentNullException.ThrowIfNull(views);
        PlatformViews = views;
    }

    /// <summary>指定 Cell を今この時点で輸送するときに載る icon。</summary>
    /// <remarks>実装の gateway が輸送 DTO を組み立てるときと同じ引き当てを行う。</remarks>
    /// <param name="cell">対象の Cell</param>
    public object? IconOf(CellBase cell) => Icons?.FindIcon(cell);

    /// <summary>指定 Section を今この時点で輸送するときに載る accessory の platform view。</summary>
    /// <remarks>実装の gateway が輸送 DTO を組み立てるときと同じ引き当てを行う。</remarks>
    /// <param name="section">対象の Section</param>
    /// <param name="target">対象の位置 (header または footer)</param>
    public object? AccessoryViewOf(Section section, KsAccessoryTarget target)
        => PlatformViews?.FindAccessoryView(section, target);

    /// <summary>指定 Cell を今この時点で輸送するときに載る内容の platform view。</summary>
    /// <remarks>実装の gateway が輸送 DTO を組み立てるときと同じ引き当てを行う。</remarks>
    /// <param name="cell">対象の Cell</param>
    public object? CellContentViewOf(CellBase cell) => PlatformViews?.FindCellContentView(cell);

    /// <inheritdoc/>
    public void ReleaseHost()
    {
        _calls.Add(new GatewayCall.ReleaseHost());
        ReleaseHostCount++;
    }

    /// <inheritdoc/>
    public void AttachInteractions(IKsInteractionSink sink)
    {
        ArgumentNullException.ThrowIfNull(sink);

        if (ReferenceEquals(Sink, sink))
        {
            return;
        }

        Sink = sink;
        AttachInteractionsCount++;
    }

    /// <inheritdoc/>
    public void DetachInteractions()
    {
        Sink = null;
        DetachInteractionsCount++;
    }

    /// <summary>Section とその配下 Cell の ID を採番する。</summary>
    private FakeSection Create(Section section)
    {
        FakeSection created = new(NextSectionId());
        foreach (CellBase _ in section.Cells)
        {
            created.CellIds.Add(NextCellId());
        }

        return created;
    }

    private FakeSection? Find(string? sectionId)
        => _sections.FirstOrDefault(section => section.Id == sectionId);

    private FakeSection? FindCellOwner(string? cellId)
        => _sections.FirstOrDefault(section => section.CellIds.Contains(cellId!));

    private string NextSectionId() => $"section-{++_sectionSeed}";

    private string NextCellId() => $"cell-{++_cellSeed}";

    /// <summary>採番済みの Section と配下 Cell の ID。</summary>
    private sealed class FakeSection(string id)
    {
        public string Id { get; } = id;

        public List<string> CellIds { get; } = [];

        public KsSectionIdentity ToIdentity() => new(Id, CellIds.ToList());
    }
}
