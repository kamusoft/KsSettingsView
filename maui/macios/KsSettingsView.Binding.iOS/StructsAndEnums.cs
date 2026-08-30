using ObjCRuntime;

namespace KsSettingsView.Bridge;

/// <summary>
/// Accessory (Root / Section の header・footer) の更新対象。
/// </summary>
/// <remarks>
/// <see cref="SectionHeader"/> / <see cref="SectionFooter"/> を指定するときは、あわせて対象
/// Section の sectionID を渡す。<see cref="RootHeader"/> / <see cref="RootFooter"/> では
/// sectionID は参照されない。
/// </remarks>
[Native]
public enum KsBridgeAccessoryTarget : long
{
    /// <summary>Root レベルのヘッダ</summary>
    RootHeader = 0,

    /// <summary>Root レベルのフッタ</summary>
    RootFooter = 1,

    /// <summary>指定 Section のヘッダ</summary>
    SectionHeader = 2,

    /// <summary>指定 Section のフッタ</summary>
    SectionFooter = 3,
}
