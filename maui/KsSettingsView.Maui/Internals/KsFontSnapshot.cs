namespace KsSettingsView.Maui.Internals;

/// <summary>
/// フォントの指定を interop 境界へ運ぶために写し取った値。
/// </summary>
/// <remarks>
/// 公開面では family / size / attributes の 3 プロパティに分かれており、その組を 1 つの記述子へ
/// 合成したもの。<see cref="PointSize"/> が 0 以下のときは Native の本文既定サイズが使われる。
/// </remarks>
internal sealed record KsFontSnapshot
{
    /// <summary>フォントファミリ名。null または解決できない名前では システムフォントが使われる。</summary>
    public string? FamilyName { get; init; }

    /// <summary>ポイントサイズ。0 以下で Native 既定。</summary>
    public double PointSize { get; init; }

    /// <summary>太字にするかどうか。</summary>
    public bool IsBold { get; init; }

    /// <summary>斜体にするかどうか。</summary>
    public bool IsItalic { get; init; }
}
