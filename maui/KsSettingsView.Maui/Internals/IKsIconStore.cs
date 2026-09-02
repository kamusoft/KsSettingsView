namespace KsSettingsView.Internals;

/// <summary>
/// Cell の icon として輸送する解決済み platform 画像を引く口。
/// </summary>
/// <remarks>
/// icon は Cell の写し (<see cref="KsCellSnapshot"/>) に載せられない — platform の画像型は
/// platform 別アセンブリでしか扱えず、Cell 自身も解決結果を持たないため。gateway は輸送 DTO を
/// 組み立てるときにここから引き当てる。
/// </remarks>
internal interface IKsIconStore
{
    /// <summary>指定 Cell の解決済み platform 画像。未解決・icon なしでは null。</summary>
    /// <param name="cell">対象の Cell</param>
    object? FindIcon(CellBase cell);
}
