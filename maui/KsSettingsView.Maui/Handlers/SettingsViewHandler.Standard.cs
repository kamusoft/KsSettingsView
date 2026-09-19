using System;

namespace KsSettingsView.Handlers;

/// <summary>
/// platform を持たない TFM 向けの <see cref="SettingsViewHandler"/> の実体。
/// </summary>
/// <remarks>
/// この TFM は Bridge を参照できず Native Host も存在しないため、Handler の寿命と
/// 参照の後始末だけを素の net10.0 で検証できるよう、置き場所としての Host を返す。
/// 親子関係を持たないため、結び付けの手順は用意しない。
/// </remarks>
public partial class SettingsViewHandler
{
    /// <summary>
    /// Host の生成を失敗させるかどうか。
    /// </summary>
    /// <remarks>
    /// Native Host を持たないこの TFM では Host の生成が失敗し得ないため、生成の失敗に対する
    /// 後始末をこの口から踏ませる。
    /// </remarks>
    internal bool FailsToCreateHost { get; set; }

    private partial object CreateHost()
        => FailsToCreateHost
            ? throw new InvalidOperationException("Failed to create the native settings host view.")
            : new KsPlaceholderHost();
}

/// <summary>Native Host の代わりに置く空の器。</summary>
internal sealed class KsPlaceholderHost
{
}
