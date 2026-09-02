using System.Collections.Generic;

namespace KsSettingsView.Internals;

/// <summary>
/// gateway が採番した Section の ID と、その Section 配下の Cell の ID (配置順)。
/// </summary>
/// <remarks>
/// facade の対応表へ登録してよいのはここに載る ID だけであり、輸送 DTO が自分で公開する ID は
/// Store 上の identity にならない。
/// </remarks>
/// <param name="SectionId">Section の ID</param>
/// <param name="CellIds">配下 Cell の ID (配置順)</param>
internal sealed record KsSectionIdentity(string SectionId, IReadOnlyList<string> CellIds);
