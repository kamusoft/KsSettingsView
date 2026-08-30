namespace KsSettingsView.IntegrationHost;

/// <summary>
/// 前半シナリオが Store 上に残した Section / Cell の ID。解放中の更新シナリオが対象を指すために使う。
/// </summary>
/// <remarks>
/// Builder が構築し insert / setRoot 系 API で Store に追加された DTO の ID は、そのまま
/// Store 上の identity になる。一方 replace 系 API では置き換え対象の既存 identity が維持され、
/// 渡した新 DTO 自身の ID は採用されない。ここで持ち回るのは Store 上で有効な ID に限る。
/// </remarks>
/// <param name="ThemeCellID">「一般」Section の「テーマ」Cell の ID</param>
/// <param name="LanguageCellID">「一般」Section の「言語」Cell の ID</param>
/// <param name="NotificationSectionID">「通知」Section の ID</param>
public sealed record KsBridgeScenarioHandles(
    string ThemeCellID,
    string LanguageCellID,
    string NotificationSectionID);
