namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 輸送 DTO へ載せる platform view の引き当て先。
/// </summary>
/// <remarks>
/// Section / Cell の輸送 DTO を組み立てるたびにここから引く。差し込まれていない間、および
/// 実体化前は view なしとして扱う (accessory はテキストの指定だけが載り、Cell の内容は空になる)。
/// </remarks>
internal interface IKsPlatformViewStore
{
    /// <summary>この Section の指定位置に実体化済みの platform view。無ければ null。</summary>
    /// <param name="section">対象の Section</param>
    /// <param name="target">対象の位置 (header または footer)</param>
    object? FindAccessoryView(Section section, KsAccessoryTarget target);

    /// <summary>この Cell の内容として実体化済みの platform view。無ければ null。</summary>
    /// <param name="cell">対象の Cell</param>
    object? FindCellContentView(CellBase cell);
}
