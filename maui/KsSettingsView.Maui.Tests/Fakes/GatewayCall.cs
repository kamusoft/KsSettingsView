using System.Collections.Generic;
using KsSettingsView.Internals;

namespace KsSettingsView.Tests.Fakes;

/// <summary>
/// <see cref="FakeSettingsGateway"/> が記録する呼び出し 1 件。
/// </summary>
internal abstract record GatewayCall
{
    /// <summary>設定ツリー全体の置き換え。</summary>
    /// <param name="Sections">渡された Section 群</param>
    internal sealed record SetRoot(IReadOnlyList<Section> Sections) : GatewayCall;

    /// <summary>Section の挿入。</summary>
    /// <param name="Section">挿入された Section</param>
    /// <param name="Index">指定された挿入位置</param>
    internal sealed record InsertSection(Section Section, int Index) : GatewayCall;

    /// <summary>Section の削除。</summary>
    /// <param name="SectionId">指定された Section の ID</param>
    internal sealed record RemoveSection(string SectionId) : GatewayCall;

    /// <summary>Section の移動。</summary>
    /// <param name="From">移動元の位置</param>
    /// <param name="To">移動先の位置</param>
    internal sealed record MoveSection(int From, int To) : GatewayCall;

    /// <summary>Section の内容置き換え。</summary>
    /// <param name="SectionId">対象 Section の ID</param>
    /// <param name="NewSection">置き換え後の内容を持つ Section</param>
    /// <param name="RetainedCellIds">配下 Cell へ引き継ぐよう指定された ID (配置順)</param>
    internal sealed record ReplaceSection(
        string SectionId,
        Section NewSection,
        IReadOnlyList<string> RetainedCellIds) : GatewayCall;

    /// <summary>Cell の挿入。</summary>
    /// <param name="Cell">挿入された Cell</param>
    /// <param name="SectionId">挿入先 Section の ID</param>
    /// <param name="Index">指定された挿入位置</param>
    internal sealed record InsertCell(CellBase Cell, string SectionId, int Index) : GatewayCall;

    /// <summary>Cell の削除。</summary>
    /// <param name="CellId">指定された Cell の ID</param>
    internal sealed record RemoveCell(string CellId) : GatewayCall;

    /// <summary>Cell の移動。</summary>
    /// <param name="CellId">対象 Cell の ID</param>
    /// <param name="Index">移動先の位置</param>
    internal sealed record MoveCell(string CellId, int Index) : GatewayCall;

    /// <summary>Cell 1 件の内容置き換え。</summary>
    /// <param name="CellId">対象 Cell の ID</param>
    /// <param name="NewCell">置き換え後の内容を持つ Cell</param>
    /// <param name="Snapshot">呼び出しの時点で写し取られた内容</param>
    /// <param name="ContentView">呼び出しの時点で引き当てられた内容の platform view</param>
    internal sealed record ReplaceCell(
        string CellId,
        CellBase NewCell,
        KsCellSnapshot Snapshot,
        object? ContentView) : GatewayCall;

    /// <summary>複数 Cell の内容置き換え (1 バッチ)。</summary>
    /// <param name="Updates">バッチに含まれた更新の並び</param>
    internal sealed record ReplaceCells(IReadOnlyList<CellUpdate> Updates) : GatewayCall;

    /// <summary>バッチに含まれた更新 1 件分。</summary>
    /// <param name="CellId">対象 Cell の ID</param>
    /// <param name="NewCell">置き換え後の内容を持つ Cell</param>
    /// <param name="Snapshot">呼び出しの時点で写し取られた内容</param>
    /// <param name="ContentView">呼び出しの時点で引き当てられた内容の platform view</param>
    internal sealed record CellUpdate(
        string CellId,
        CellBase NewCell,
        KsCellSnapshot Snapshot,
        object? ContentView);

    /// <summary>accessory テキストの更新。</summary>
    /// <param name="Target">更新対象</param>
    /// <param name="SectionId">Section を対象にするときの ID</param>
    /// <param name="Text">指定されたテキスト</param>
    internal sealed record UpdateAccessory(KsAccessoryTarget Target, string? SectionId, string? Text)
        : GatewayCall;

    /// <summary>accessory の platform view の更新。</summary>
    /// <param name="Target">更新対象</param>
    /// <param name="SectionId">Section を対象にするときの ID</param>
    /// <param name="View">指定された platform view</param>
    internal sealed record UpdateAccessoryView(KsAccessoryTarget Target, string? SectionId, object? View)
        : GatewayCall;

    /// <summary>accessory 領域の測り直しの要求。</summary>
    /// <param name="Target">対象</param>
    /// <param name="SectionId">Section を対象にするときの ID</param>
    internal sealed record InvalidateAccessoryMeasurement(KsAccessoryTarget Target, string? SectionId)
        : GatewayCall;

    /// <summary>既定スタイルの適用。</summary>
    /// <param name="Theme">適用された既定スタイル</param>
    internal sealed record SetTheme(KsThemeSnapshot Theme) : GatewayCall;

    /// <summary>見た目スタイルの適用。</summary>
    /// <param name="Style">適用された見た目スタイル</param>
    internal sealed record SetStyle(SettingsViewStyle Style) : GatewayCall;

    /// <summary>Native Host の解放。</summary>
    internal sealed record ReleaseHost : GatewayCall;
}
