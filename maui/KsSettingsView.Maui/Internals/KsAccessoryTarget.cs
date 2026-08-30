namespace KsSettingsView.Maui.Internals;

/// <summary>
/// accessory (Root / Section の header・footer) の更新対象。
/// </summary>
internal enum KsAccessoryTarget
{
    /// <summary>設定画面全体のヘッダ。</summary>
    RootHeader,

    /// <summary>設定画面全体のフッタ。</summary>
    RootFooter,

    /// <summary>指定 Section のヘッダ。</summary>
    SectionHeader,

    /// <summary>指定 Section のフッタ。</summary>
    SectionFooter,
}
