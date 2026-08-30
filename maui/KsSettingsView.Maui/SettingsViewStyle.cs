namespace KsSettingsView.Maui;

/// <summary>
/// <see cref="SettingsView"/> の見た目スタイル (maui/ADR-0023)。
/// </summary>
/// <remarks>
/// 両 platform のスタイルを 1 つの列挙で表す。装飾と区切り線の規則だけが変わり、設定内容と
/// Section / Cell の identity は切り替えても変化しない。
/// </remarks>
public enum SettingsViewStyle
{
    /// <summary>フラットな見た目。Section の境界は全幅の区切り線で示される。</summary>
    Classic,

    /// <summary>Section の Cell 行を角丸の箱にまとめる見た目。</summary>
    Modern,
}
