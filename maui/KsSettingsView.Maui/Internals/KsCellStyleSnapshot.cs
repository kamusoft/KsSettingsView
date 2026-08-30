namespace KsSettingsView.Maui.Internals;

/// <summary>
/// Cell 個別のスタイル上書きを interop 境界へ運ぶために写し取った値。
/// </summary>
/// <remarks>
/// 項目は Native の CellStyle と 1 対 1 で対応する。色は ARGB を詰めた 32bit 整数、寸法は数値で表し、
/// null は「未指定 → Theme から継承」を意味する。全項目が未指定の Cell では写し自体を作らない。
/// </remarks>
internal sealed record KsCellStyleSnapshot
{
    /// <summary>全項目が未指定の写し。作った写しが継承だけを意味するかどうかの判定に使う。</summary>
    public static KsCellStyleSnapshot Unspecified { get; } = new();

    /// <summary>タイトル文字色 (ARGB)。</summary>
    public int? TitleColor { get; init; }

    /// <summary>タイトルフォント。</summary>
    public KsFontSnapshot? TitleFont { get; init; }

    /// <summary>説明文色 (ARGB)。</summary>
    public int? DescriptionColor { get; init; }

    /// <summary>説明文フォント。</summary>
    public KsFontSnapshot? DescriptionFont { get; init; }

    /// <summary>値テキスト色 (ARGB)。</summary>
    public int? ValueTextColor { get; init; }

    /// <summary>値テキストフォント。</summary>
    public KsFontSnapshot? ValueTextFont { get; init; }

    /// <summary>アイコンの表示サイズ。</summary>
    public double? IconSize { get; init; }

    /// <summary>アイコンの角丸半径。</summary>
    public double? IconRadius { get; init; }

    /// <summary>行の高さ。</summary>
    public double? CellHeight { get; init; }

    /// <summary>ヒントテキスト色 (ARGB)。</summary>
    public int? HintTextColor { get; init; }

    /// <summary>ヒントテキストフォント。</summary>
    public KsFontSnapshot? HintTextFont { get; init; }

    /// <summary>行の背景色 (ARGB)。</summary>
    public int? BackgroundColor { get; init; }
}
