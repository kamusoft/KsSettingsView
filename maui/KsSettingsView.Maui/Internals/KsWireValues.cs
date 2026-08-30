using System;
using System.Collections.Generic;
using System.Globalization;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 公開 API の型を interop 境界の輸送表現へ写す変換 (maui/ADR-0012)。
/// </summary>
/// <remarks>
/// 壁時計値である時刻・日付はタイムゾーンを含まない固定書式の文字列、enum は序数、色は ARGB を
/// 詰めた 32bit 整数で運ぶ。書式は culture に依存しない (InvariantCulture 固定) ため、
/// 生成結果は実行環境を問わず同じになる。逆向きの解釈 (Native からの通知値) も同じ書式で行う。
/// </remarks>
internal static class KsWireValues
{
    /// <summary>時刻の輸送書式。</summary>
    public const string TimeFormat = @"hh\:mm";

    /// <summary>日付の輸送書式。</summary>
    public const string DateFormat = "yyyy-MM-dd";

    /// <summary>日付の型の既定値。</summary>
    public static DateTime DefaultDate { get; } = new(1970, 1, 1);

    /// <summary>時刻を輸送書式の文字列へ写す。</summary>
    /// <param name="value">写し取る時刻</param>
    public static string Time(TimeSpan value)
        => value.ToString(TimeFormat, CultureInfo.InvariantCulture);

    /// <summary>日付を輸送書式の文字列へ写す。</summary>
    /// <param name="value">写し取る日付 (時刻部分は捨てられる)</param>
    public static string Date(DateTime value)
        => value.ToString(DateFormat, CultureInfo.InvariantCulture);

    /// <summary>未指定を許す日付を輸送書式の文字列へ写す。</summary>
    /// <param name="value">写し取る日付。null で未指定</param>
    public static string? OptionalDate(DateTime? value)
        => value is null ? null : Date(value.Value);

    /// <summary>輸送書式の文字列を時刻として解釈する。</summary>
    /// <remarks>桁数不足や区切り文字違いは解釈失敗として扱う。</remarks>
    /// <param name="text">解釈する文字列</param>
    /// <param name="value">解釈できた時刻</param>
    public static bool TryParseTime(string? text, out TimeSpan value)
    {
        value = default;
        return text is not null
            && TimeSpan.TryParseExact(text, TimeFormat, CultureInfo.InvariantCulture, out value);
    }

    /// <summary>輸送書式の文字列を日付として解釈する。</summary>
    /// <remarks>桁数不足・区切り文字違い・暦上存在しない日は解釈失敗として扱う。</remarks>
    /// <param name="text">解釈する文字列</param>
    /// <param name="value">解釈できた日付</param>
    public static bool TryParseDate(string? text, out DateTime value)
    {
        value = default;
        return text is not null
            && DateTime.TryParseExact(
                text,
                DateFormat,
                CultureInfo.InvariantCulture,
                DateTimeStyles.None,
                out value);
    }

    /// <summary>
    /// MAUI のキーボードを正規化した種別へ写す。
    /// </summary>
    /// <remarks>
    /// 標準キーボードは種別ごとの単一インスタンスとして提供されるため、同一性で判別する。
    /// 標準以外のキーボード (利用者が組み立てたもの) と未指定は既定として扱う。
    /// </remarks>
    /// <param name="keyboard">写し取るキーボード。null で既定</param>
    public static KsKeyboardKind Keyboard(Keyboard? keyboard)
    {
        if (keyboard is null)
        {
            return KsKeyboardKind.Default;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Plain))
        {
            return KsKeyboardKind.Plain;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Text))
        {
            return KsKeyboardKind.Text;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Chat))
        {
            return KsKeyboardKind.Chat;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Url))
        {
            return KsKeyboardKind.Url;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Email))
        {
            return KsKeyboardKind.Email;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Numeric))
        {
            return KsKeyboardKind.Numeric;
        }

        if (ReferenceEquals(keyboard, Microsoft.Maui.Keyboard.Telephone))
        {
            return KsKeyboardKind.Telephone;
        }

        return KsKeyboardKind.Default;
    }

    /// <summary>揃え位置を輸送する序数へ写す。</summary>
    /// <param name="value">写し取る揃え位置。null で未指定 (Native 既定)</param>
    public static int? Alignment(TextAlignment? value) => value switch
    {
        TextAlignment.Start => 0,
        TextAlignment.Center => 1,
        TextAlignment.End => 2,
        _ => null,
    };

    /// <summary>選択面の形式を輸送する序数へ写す。</summary>
    /// <param name="value">写し取る形式。null で未指定 (Native 既定)</param>
    public static int? UIStyle(DatePickerUIStyle? value) => value switch
    {
        DatePickerUIStyle.Calendar => 0,
        DatePickerUIStyle.Wheels => 1,
        _ => null,
    };

    /// <summary>選択モードを輸送する序数へ写す。</summary>
    /// <param name="value">写し取る選択モード</param>
    public static int SelectionMode(PickerSelectionMode value)
        => value == PickerSelectionMode.Multiple ? 1 : 0;

    /// <summary>見た目スタイルを輸送する序数へ写す。</summary>
    /// <remarks>
    /// 列挙に定義されていない値も序数のまま運ぶ。定義域外の序数を Classic へ倒す正規化は
    /// Bridge が行う。
    /// </remarks>
    /// <param name="value">写し取る見た目スタイル</param>
    public static int ListStyle(SettingsViewStyle value) => (int)value;

    /// <summary>Section の外側余白の上成分。null で未指定。</summary>
    /// <param name="margin">写し取る余白。null で未指定</param>
    public static double? MarginTop(Thickness? margin) => margin?.Top;

    /// <summary>Section の外側余白の下成分。null で未指定。</summary>
    /// <param name="margin">写し取る余白。null で未指定</param>
    public static double? MarginBottom(Thickness? margin) => margin?.Bottom;

    /// <summary>
    /// Section の外側余白の leading 成分。null で未指定。
    /// </summary>
    /// <remarks>
    /// <see cref="Thickness.Left"/> は物理的な左ではなく論理方向の起点として解釈する
    /// (maui/ADR-0024)。左右の解決は Native の方向解決機構が行う。
    /// </remarks>
    /// <param name="margin">写し取る余白。null で未指定</param>
    public static double? MarginLeading(Thickness? margin) => margin?.Left;

    /// <summary>
    /// Section の外側余白の trailing 成分。null で未指定。
    /// </summary>
    /// <remarks><see cref="Thickness.Right"/> を論理方向の終端として解釈する (maui/ADR-0024)。</remarks>
    /// <param name="margin">写し取る余白。null で未指定</param>
    public static double? MarginTrailing(Thickness? margin) => margin?.Right;

    /// <summary>
    /// 分割して公開されているフォント指定を 1 つの記述子へ合成する。
    /// </summary>
    /// <remarks>
    /// どの項目も指定されていなければ記述子を作らない (未指定として上位のスタイルを継承する)。
    /// サイズだけが未指定のときは 0 を入れ、Native の本文既定サイズへ委ねる。
    /// </remarks>
    /// <param name="family">フォントファミリ名。null で未指定</param>
    /// <param name="size">ポイントサイズ。null で未指定</param>
    /// <param name="attributes">太字・斜体の指定。null で未指定</param>
    public static KsFontSnapshot? Font(string? family, double? size, FontAttributes? attributes)
    {
        if (family is null && size is null && attributes is null)
        {
            return null;
        }

        return new KsFontSnapshot
        {
            FamilyName = family,
            PointSize = size ?? 0,
            IsBold = attributes?.HasFlag(FontAttributes.Bold) ?? false,
            IsItalic = attributes?.HasFlag(FontAttributes.Italic) ?? false,
        };
    }

    /// <summary>色を ARGB を詰めた 32bit 整数へ写す。</summary>
    /// <param name="color">写し取る色。null で未指定</param>
    public static int? Color(Color? color)
    {
        if (color is null)
        {
            return null;
        }

        int alpha = Channel(color.Alpha);
        int red = Channel(color.Red);
        int green = Channel(color.Green);
        int blue = Channel(color.Blue);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /// <summary>選択 index 群を輸送表現 (昇順・重複なし) へ正規化する。</summary>
    /// <param name="indices">正規化する index 群。null で空</param>
    public static IReadOnlyList<int> Indices(IEnumerable<int>? indices)
    {
        if (indices is null)
        {
            return [];
        }

        SortedSet<int> normalized = [.. indices];
        return [.. normalized];
    }

    /// <summary>2 つの選択 index 群が集合として等しいかどうかを返す。</summary>
    /// <remarks>順序と重複の違いは差分として扱わない。</remarks>
    /// <param name="left">比較する index 群</param>
    /// <param name="right">比較する index 群</param>
    public static bool IndicesEqual(IEnumerable<int>? left, IEnumerable<int>? right)
    {
        IReadOnlyList<int> normalizedLeft = Indices(left);
        IReadOnlyList<int> normalizedRight = Indices(right);
        if (normalizedLeft.Count != normalizedRight.Count)
        {
            return false;
        }

        for (int i = 0; i < normalizedLeft.Count; i++)
        {
            if (normalizedLeft[i] != normalizedRight[i])
            {
                return false;
            }
        }

        return true;
    }

    /// <summary>0.0〜1.0 の成分値を 0〜255 の整数へ丸める。</summary>
    private static int Channel(float component)
        => Math.Clamp((int)Math.Round(component * 255f, MidpointRounding.AwayFromZero), 0, 255);
}
