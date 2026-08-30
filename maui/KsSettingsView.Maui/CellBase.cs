using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui;

/// <summary>
/// 設定画面の 1 行を表す Cell の共通基底。
/// </summary>
/// <remarks>
/// Cell は表示内容を保持する model であり、描画は Native 側が受け持つ。プロパティの変更は
/// UI スレッドから行う (呼び出し側契約であり、facade はスレッド marshal を行わない)。
/// 同じインスタンスを複数の <see cref="Section"/> へ配置することはできない。
/// スタイル系のプロパティは未指定 (null) のとき <see cref="SettingsView"/> の既定スタイルを継承し、
/// 指定した行だけがその値で描かれる。
/// </remarks>
public abstract class CellBase : Element
{
    /// <summary><see cref="Title"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleProperty = BindableProperty.Create(
        nameof(Title),
        typeof(string),
        typeof(CellBase),
        string.Empty);

    /// <summary><see cref="Description"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DescriptionProperty = BindableProperty.Create(
        nameof(Description),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="HintText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HintTextProperty = BindableProperty.Create(
        nameof(HintText),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="IsEnabled"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsEnabledProperty = BindableProperty.Create(
        nameof(IsEnabled),
        typeof(bool),
        typeof(CellBase),
        true);

    /// <summary><see cref="IsVisible"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsVisibleProperty = BindableProperty.Create(
        nameof(IsVisible),
        typeof(bool),
        typeof(CellBase),
        true);

    /// <summary><see cref="IconSource"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IconSourceProperty = BindableProperty.Create(
        nameof(IconSource),
        typeof(ImageSource),
        typeof(CellBase),
        default(ImageSource));

    /// <summary><see cref="TitleColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleColorProperty = BindableProperty.Create(
        nameof(TitleColor),
        typeof(Color),
        typeof(CellBase),
        default(Color));

    /// <summary><see cref="TitleFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleFontFamilyProperty = BindableProperty.Create(
        nameof(TitleFontFamily),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="TitleFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleFontSizeProperty = BindableProperty.Create(
        nameof(TitleFontSize),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="TitleFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleFontAttributesProperty = BindableProperty.Create(
        nameof(TitleFontAttributes),
        typeof(FontAttributes?),
        typeof(CellBase),
        default(FontAttributes?));

    /// <summary><see cref="DescriptionColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DescriptionColorProperty = BindableProperty.Create(
        nameof(DescriptionColor),
        typeof(Color),
        typeof(CellBase),
        default(Color));

    /// <summary><see cref="DescriptionFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DescriptionFontFamilyProperty = BindableProperty.Create(
        nameof(DescriptionFontFamily),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="DescriptionFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DescriptionFontSizeProperty = BindableProperty.Create(
        nameof(DescriptionFontSize),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="DescriptionFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DescriptionFontAttributesProperty = BindableProperty.Create(
        nameof(DescriptionFontAttributes),
        typeof(FontAttributes?),
        typeof(CellBase),
        default(FontAttributes?));

    /// <summary><see cref="ValueTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextColorProperty = BindableProperty.Create(
        nameof(ValueTextColor),
        typeof(Color),
        typeof(CellBase),
        default(Color));

    /// <summary><see cref="ValueTextFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextFontFamilyProperty = BindableProperty.Create(
        nameof(ValueTextFontFamily),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="ValueTextFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextFontSizeProperty = BindableProperty.Create(
        nameof(ValueTextFontSize),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="ValueTextFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextFontAttributesProperty = BindableProperty.Create(
        nameof(ValueTextFontAttributes),
        typeof(FontAttributes?),
        typeof(CellBase),
        default(FontAttributes?));

    /// <summary><see cref="HintTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HintTextColorProperty = BindableProperty.Create(
        nameof(HintTextColor),
        typeof(Color),
        typeof(CellBase),
        default(Color));

    /// <summary><see cref="HintFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HintFontFamilyProperty = BindableProperty.Create(
        nameof(HintFontFamily),
        typeof(string),
        typeof(CellBase),
        default(string));

    /// <summary><see cref="HintFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HintFontSizeProperty = BindableProperty.Create(
        nameof(HintFontSize),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="HintFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HintFontAttributesProperty = BindableProperty.Create(
        nameof(HintFontAttributes),
        typeof(FontAttributes?),
        typeof(CellBase),
        default(FontAttributes?));

    /// <summary><see cref="BackgroundColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty BackgroundColorProperty = BindableProperty.Create(
        nameof(BackgroundColor),
        typeof(Color),
        typeof(CellBase),
        default(Color));

    /// <summary><see cref="IconSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IconSizeProperty = BindableProperty.Create(
        nameof(IconSize),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="IconRadius"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IconRadiusProperty = BindableProperty.Create(
        nameof(IconRadius),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary><see cref="Height"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeightProperty = BindableProperty.Create(
        nameof(Height),
        typeof(double?),
        typeof(CellBase),
        default(double?));

    /// <summary>行の主タイトル。</summary>
    public string Title
    {
        get => (string)GetValue(TitleProperty);
        set => SetValue(TitleProperty, value);
    }

    /// <summary>タイトルの下に表示する説明文。null で非表示。</summary>
    public string? Description
    {
        get => (string?)GetValue(DescriptionProperty);
        set => SetValue(DescriptionProperty, value);
    }

    /// <summary>補足として表示するヒントテキスト。null で非表示。</summary>
    public string? HintText
    {
        get => (string?)GetValue(HintTextProperty);
        set => SetValue(HintTextProperty, value);
    }

    /// <summary>行が有効かどうか。false の行は無効表示になる。</summary>
    public bool IsEnabled
    {
        get => (bool)GetValue(IsEnabledProperty);
        set => SetValue(IsEnabledProperty, value);
    }

    /// <summary>行を表示するかどうか。false の行は表示から除外される。</summary>
    public bool IsVisible
    {
        get => (bool)GetValue(IsVisibleProperty);
        set => SetValue(IsVisibleProperty, value);
    }

    /// <summary>
    /// 行の先頭に表示するアイコン。null でアイコンなし。
    /// </summary>
    /// <remarks>
    /// 画像は MAUI の画像解決の仕組みで platform の画像へ実体化され、解決できた時点で表示に反映される。
    /// 表示前 (Handler 未接続) に設定した画像は接続後に解決される。解決できなかった画像はアイコンなしと
    /// して扱う。
    /// </remarks>
    public ImageSource? IconSource
    {
        get => (ImageSource?)GetValue(IconSourceProperty);
        set => SetValue(IconSourceProperty, value);
    }

    /// <summary>タイトルの文字色。null で既定スタイルを継承。</summary>
    public Color? TitleColor
    {
        get => (Color?)GetValue(TitleColorProperty);
        set => SetValue(TitleColorProperty, value);
    }

    /// <summary>タイトルのフォントファミリ。null で既定スタイルを継承。</summary>
    public string? TitleFontFamily
    {
        get => (string?)GetValue(TitleFontFamilyProperty);
        set => SetValue(TitleFontFamilyProperty, value);
    }

    /// <summary>タイトルのフォントサイズ。null で既定スタイルを継承。</summary>
    public double? TitleFontSize
    {
        get => (double?)GetValue(TitleFontSizeProperty);
        set => SetValue(TitleFontSizeProperty, value);
    }

    /// <summary>タイトルの太字・斜体の指定。null で既定スタイルを継承。</summary>
    public FontAttributes? TitleFontAttributes
    {
        get => (FontAttributes?)GetValue(TitleFontAttributesProperty);
        set => SetValue(TitleFontAttributesProperty, value);
    }

    /// <summary>説明文の文字色。null で既定スタイルを継承。</summary>
    public Color? DescriptionColor
    {
        get => (Color?)GetValue(DescriptionColorProperty);
        set => SetValue(DescriptionColorProperty, value);
    }

    /// <summary>説明文のフォントファミリ。null で既定スタイルを継承。</summary>
    public string? DescriptionFontFamily
    {
        get => (string?)GetValue(DescriptionFontFamilyProperty);
        set => SetValue(DescriptionFontFamilyProperty, value);
    }

    /// <summary>説明文のフォントサイズ。null で既定スタイルを継承。</summary>
    public double? DescriptionFontSize
    {
        get => (double?)GetValue(DescriptionFontSizeProperty);
        set => SetValue(DescriptionFontSizeProperty, value);
    }

    /// <summary>説明文の太字・斜体の指定。null で既定スタイルを継承。</summary>
    public FontAttributes? DescriptionFontAttributes
    {
        get => (FontAttributes?)GetValue(DescriptionFontAttributesProperty);
        set => SetValue(DescriptionFontAttributesProperty, value);
    }

    /// <summary>値テキストの文字色。null で既定スタイルを継承。</summary>
    public Color? ValueTextColor
    {
        get => (Color?)GetValue(ValueTextColorProperty);
        set => SetValue(ValueTextColorProperty, value);
    }

    /// <summary>値テキストのフォントファミリ。null で既定スタイルを継承。</summary>
    public string? ValueTextFontFamily
    {
        get => (string?)GetValue(ValueTextFontFamilyProperty);
        set => SetValue(ValueTextFontFamilyProperty, value);
    }

    /// <summary>値テキストのフォントサイズ。null で既定スタイルを継承。</summary>
    public double? ValueTextFontSize
    {
        get => (double?)GetValue(ValueTextFontSizeProperty);
        set => SetValue(ValueTextFontSizeProperty, value);
    }

    /// <summary>値テキストの太字・斜体の指定。null で既定スタイルを継承。</summary>
    public FontAttributes? ValueTextFontAttributes
    {
        get => (FontAttributes?)GetValue(ValueTextFontAttributesProperty);
        set => SetValue(ValueTextFontAttributesProperty, value);
    }

    /// <summary>ヒントテキストの文字色。null で既定スタイルを継承。</summary>
    public Color? HintTextColor
    {
        get => (Color?)GetValue(HintTextColorProperty);
        set => SetValue(HintTextColorProperty, value);
    }

    /// <summary>ヒントテキストのフォントファミリ。null で既定スタイルを継承。</summary>
    public string? HintFontFamily
    {
        get => (string?)GetValue(HintFontFamilyProperty);
        set => SetValue(HintFontFamilyProperty, value);
    }

    /// <summary>ヒントテキストのフォントサイズ。null で既定スタイルを継承。</summary>
    public double? HintFontSize
    {
        get => (double?)GetValue(HintFontSizeProperty);
        set => SetValue(HintFontSizeProperty, value);
    }

    /// <summary>ヒントテキストの太字・斜体の指定。null で既定スタイルを継承。</summary>
    public FontAttributes? HintFontAttributes
    {
        get => (FontAttributes?)GetValue(HintFontAttributesProperty);
        set => SetValue(HintFontAttributesProperty, value);
    }

    /// <summary>この行だけの背景色。null で既定スタイルを継承。</summary>
    public Color? BackgroundColor
    {
        get => (Color?)GetValue(BackgroundColorProperty);
        set => SetValue(BackgroundColorProperty, value);
    }

    /// <summary>アイコンの表示サイズ。null で既定スタイルを継承。</summary>
    public double? IconSize
    {
        get => (double?)GetValue(IconSizeProperty);
        set => SetValue(IconSizeProperty, value);
    }

    /// <summary>アイコンの角丸半径。null で既定スタイルを継承。</summary>
    public double? IconRadius
    {
        get => (double?)GetValue(IconRadiusProperty);
        set => SetValue(IconRadiusProperty, value);
    }

    /// <summary>行の高さ。null で既定スタイルを継承。</summary>
    public double? Height
    {
        get => (double?)GetValue(HeightProperty);
        set => SetValue(HeightProperty, value);
    }

    /// <summary>
    /// interop 境界へ運ぶ内容を現在値から写し取る。
    /// </summary>
    /// <remarks>
    /// 派生 Cell は <see cref="CreateSnapshot{T}"/> で共通項目を写した自分の種別の写しを作り、
    /// そこへ固有の項目を重ねて返す。どの種別にも当てはまらない Cell は、共通項目だけを持つ
    /// 読み取り専用の行として扱われる (輸送側の基底 DTO と同じ扱い)。
    /// </remarks>
    internal virtual KsCellSnapshot CreateSnapshot() => CreateSnapshot<KsLabelCellSnapshot>();

    /// <summary>
    /// 共通項目だけを写し取った、指定した種別の写しを作る。
    /// </summary>
    /// <remarks>
    /// <see cref="Title"/> は輸送側が非 null を要求するため、null は空文字へ解決する。
    /// <see cref="IconSource"/> は写しに載らない — 解決済みの platform 画像は変換経路が Cell 単位で
    /// 持ち、gateway が輸送 DTO を組み立てるときに引き当てる。
    /// </remarks>
    /// <typeparam name="T">作る写しの種別</typeparam>
    private protected T CreateSnapshot<T>()
        where T : KsCellSnapshot, new() => new()
        {
            Title = Title ?? string.Empty,
            Description = Description,
            HintText = HintText,
            IsEnabled = IsEnabled,
            IsVisible = IsVisible,
            Style = CreateStyleSnapshot(),
        };

    /// <summary>
    /// 指定したプロパティの変更が、interop 境界へ運ぶ内容に影響するかどうかを返す。
    /// </summary>
    /// <remarks>
    /// 派生 Cell は自分が写し取る項目を基底の判定に重ねる。ここで false になる変更は
    /// 内容更新として配信しない。<see cref="IconSource"/> は写しに載らず、解決を挟んでから
    /// 反映されるため、ここでは影響ありとして扱わない。
    /// </remarks>
    /// <param name="propertyName">変更されたプロパティの名前</param>
    internal virtual bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(Title) or nameof(Description) or nameof(HintText)
        or nameof(IsEnabled) or nameof(IsVisible)
        or nameof(TitleColor) or nameof(TitleFontFamily) or nameof(TitleFontSize)
        or nameof(TitleFontAttributes)
        or nameof(DescriptionColor) or nameof(DescriptionFontFamily) or nameof(DescriptionFontSize)
        or nameof(DescriptionFontAttributes)
        or nameof(ValueTextColor) or nameof(ValueTextFontFamily) or nameof(ValueTextFontSize)
        or nameof(ValueTextFontAttributes)
        or nameof(HintTextColor) or nameof(HintFontFamily) or nameof(HintFontSize)
        or nameof(HintFontAttributes)
        or nameof(BackgroundColor) or nameof(IconSize) or nameof(IconRadius) or nameof(Height);

    /// <summary>
    /// 行そのものに掛かるスタイル上書きだけを写し取る。何も指定されていなければ null。
    /// </summary>
    /// <remarks>
    /// 共通行レイアウトのスロット (タイトル・説明文・値テキスト・ヒント・アイコン) を持たない
    /// Cell が使う。それらに掛かる指定は表示に使えないため写さず、行の高さと背景色だけを残す。
    /// </remarks>
    private protected KsCellStyleSnapshot? CreateRowStyleSnapshot()
    {
        KsCellStyleSnapshot style = new()
        {
            CellHeight = Height,
            BackgroundColor = KsWireValues.Color(BackgroundColor),
        };

        return style == KsCellStyleSnapshot.Unspecified ? null : style;
    }

    /// <summary>この行だけのスタイル上書きを写し取る。何も指定されていなければ null。</summary>
    private KsCellStyleSnapshot? CreateStyleSnapshot()
    {
        KsCellStyleSnapshot style = new()
        {
            TitleColor = KsWireValues.Color(TitleColor),
            TitleFont = KsWireValues.Font(TitleFontFamily, TitleFontSize, TitleFontAttributes),
            DescriptionColor = KsWireValues.Color(DescriptionColor),
            DescriptionFont = KsWireValues.Font(
                DescriptionFontFamily,
                DescriptionFontSize,
                DescriptionFontAttributes),
            ValueTextColor = KsWireValues.Color(ValueTextColor),
            ValueTextFont = KsWireValues.Font(
                ValueTextFontFamily,
                ValueTextFontSize,
                ValueTextFontAttributes),
            IconSize = IconSize,
            IconRadius = IconRadius,
            CellHeight = Height,
            HintTextColor = KsWireValues.Color(HintTextColor),
            HintTextFont = KsWireValues.Font(HintFontFamily, HintFontSize, HintFontAttributes),
            BackgroundColor = KsWireValues.Color(BackgroundColor),
        };

        return style == KsCellStyleSnapshot.Unspecified ? null : style;
    }
}
