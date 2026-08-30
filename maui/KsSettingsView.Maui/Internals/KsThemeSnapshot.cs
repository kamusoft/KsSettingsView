namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 画面全体の既定スタイルを interop 境界へ運ぶために写し取った値。
/// </summary>
/// <remarks>
/// 項目は Native の Theme と 1 対 1 で対応する。色は ARGB を詰めた 32bit 整数、寸法とフラグは数値で
/// 表し、null は未指定 (Native 既定) を意味する。Cell の実効値は Cell 固有値 → Cell 個別スタイル →
/// ここ → platform 既定の順で解決される。
/// </remarks>
internal sealed record KsThemeSnapshot
{
    /// <summary>セパレータ色 (ARGB)。</summary>
    public int? SeparatorColor { get; init; }

    /// <summary>設定画面全体の背景色 (ARGB)。</summary>
    public int? BackgroundColor { get; init; }

    /// <summary>Cell の既定背景色 (ARGB)。</summary>
    public int? CellBackgroundColor { get; init; }

    /// <summary>Cell 選択時の背景色 (ARGB)。</summary>
    public int? SelectedColor { get; init; }

    /// <summary>強調色 (ARGB)。</summary>
    public int? CellAccentColor { get; init; }

    /// <summary>無効な行のテキスト色 (ARGB)。</summary>
    public int? DisabledTextColor { get; init; }

    /// <summary>スクロールインジケータを表示するかどうか。</summary>
    public bool? ScrollIndicatorVisible { get; init; }

    /// <summary>行の高さの基準値。</summary>
    public int? RowHeight { get; init; }

    /// <summary>行ごとに高さを変えるかどうか。</summary>
    public bool? HasUnevenRows { get; init; }

    /// <summary>Section ヘッダのテキスト色 (ARGB)。</summary>
    public int? HeaderTextColor { get; init; }

    /// <summary>Section ヘッダの背景色 (ARGB)。</summary>
    public int? HeaderBackgroundColor { get; init; }

    /// <summary>Section ヘッダの最終フォントサイズ。</summary>
    public double? HeaderFontSize { get; init; }

    /// <summary>Section ヘッダの既定フォント。</summary>
    public KsFontSnapshot? HeaderFont { get; init; }

    /// <summary>Section ヘッダの既定高さ。</summary>
    public double? HeaderHeight { get; init; }

    /// <summary>Section フッタのテキスト色 (ARGB)。</summary>
    public int? FooterTextColor { get; init; }

    /// <summary>Section フッタの背景色 (ARGB)。</summary>
    public int? FooterBackgroundColor { get; init; }

    /// <summary>Section フッタの最終フォントサイズ。</summary>
    public double? FooterFontSize { get; init; }

    /// <summary>Section フッタの既定フォント。</summary>
    public KsFontSnapshot? FooterFont { get; init; }

    /// <summary>Cell タイトルの既定色 (ARGB)。</summary>
    public int? CellTitleColor { get; init; }

    /// <summary>Cell タイトルの既定フォント。</summary>
    public KsFontSnapshot? CellTitleFont { get; init; }

    /// <summary>Cell タイトルの最終フォントサイズ。</summary>
    public double? CellTitleFontSize { get; init; }

    /// <summary>値テキストの既定色 (ARGB)。</summary>
    public int? CellValueTextColor { get; init; }

    /// <summary>値テキストの既定フォント。</summary>
    public KsFontSnapshot? CellValueTextFont { get; init; }

    /// <summary>説明文の既定色 (ARGB)。</summary>
    public int? CellDescriptionColor { get; init; }

    /// <summary>説明文の既定フォント。</summary>
    public KsFontSnapshot? CellDescriptionFont { get; init; }

    /// <summary>ヒントテキストの既定色 (ARGB)。</summary>
    public int? CellHintTextColor { get; init; }

    /// <summary>ヒントテキストの既定フォント。</summary>
    public KsFontSnapshot? CellHintFont { get; init; }

    /// <summary>EntryCell のプレースホルダの既定文字色 (ARGB)。</summary>
    public int? CellPlaceholderColor { get; init; }

    /// <summary>アイコンの既定サイズ。</summary>
    public double? CellIconSize { get; init; }

    /// <summary>アイコンの既定角丸半径。</summary>
    public double? CellIconRadius { get; init; }

    /// <summary>Section の外側余白の上成分。</summary>
    /// <remarks>4 成分は余白全体で 1 つの指定として扱い、全部が null か全部が非 null になる。</remarks>
    public double? SectionMarginTop { get; init; }

    /// <summary>Section の外側余白の leading 成分 (論理方向の起点)。</summary>
    public double? SectionMarginLeading { get; init; }

    /// <summary>Section の外側余白の下成分。</summary>
    public double? SectionMarginBottom { get; init; }

    /// <summary>Section の外側余白の trailing 成分 (論理方向の終端)。</summary>
    public double? SectionMarginTrailing { get; init; }

    /// <summary>Section の箱の角丸半径。</summary>
    public double? SectionCornerRadius { get; init; }

    /// <summary>Section の箱のボーダー幅。</summary>
    public double? SectionBorderWidth { get; init; }

    /// <summary>Section の箱のボーダー色 (ARGB)。</summary>
    public int? SectionBorderColor { get; init; }
}
