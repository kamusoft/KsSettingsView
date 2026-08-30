namespace KsSettingsView.Maui.Internals;

/// <summary>
/// interop 境界へ運ぶキーボード種別。
/// </summary>
/// <remarks>
/// <see cref="Microsoft.Maui.Keyboard"/> の標準キーボードと 1 対 1 で対応する正規化した種別で、
/// 値がそのまま輸送される序数になる。Native 側の keyboard 型 (iOS の keyboardType、Android の
/// InputType) への対応は Bridge が持ち、対応の取れない種別は Native 既定へ倒される。
/// </remarks>
internal enum KsKeyboardKind
{
    /// <summary>既定のキーボード。</summary>
    Default = 0,

    /// <summary>補助機能を持たない素のキーボード。</summary>
    Plain = 1,

    /// <summary>一般的な文章入力向けのキーボード。</summary>
    Text = 2,

    /// <summary>チャット入力向けのキーボード。</summary>
    Chat = 3,

    /// <summary>URL 入力向けのキーボード。</summary>
    Url = 4,

    /// <summary>メールアドレス入力向けのキーボード。</summary>
    Email = 5,

    /// <summary>数値入力向けのキーボード。</summary>
    Numeric = 6,

    /// <summary>電話番号入力向けのキーボード。</summary>
    Telephone = 7,
}
