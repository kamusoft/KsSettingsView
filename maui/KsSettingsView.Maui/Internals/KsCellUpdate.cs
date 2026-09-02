namespace KsSettingsView.Internals;

/// <summary>
/// 複数 Cell の内容をまとめて更新するときの 1 件分の指定。
/// </summary>
/// <param name="CellId">更新対象の Cell の ID</param>
/// <param name="Cell">更新後の内容を持つ Cell</param>
internal sealed record KsCellUpdate(string CellId, CellBase Cell);
