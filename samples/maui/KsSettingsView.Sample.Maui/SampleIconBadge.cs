using KsSettingsView.Maui;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// 角丸の色付き四角に白いシンボルを載せた「バッジ型アイコン」の寸法。
/// </summary>
/// <remarks>
/// Cell のアイコンは <see cref="CellBase.IconSource"/> の画像をそのまま表示するため、アイコンの
/// 地色は利用者側で画像に焼き込む。四角形の画像を渡すとアイコン列の幅がシンボルの字形に依存
/// しなくなり、行ごとの title の開始位置が揃う。角丸は <see cref="SettingsView.CellIconRadius"/>
/// が担当する。
///
/// 画像は Resources/Images の ic_badge_airplane / ic_badge_wifi / ic_badge_bluetooth /
/// ic_badge_battery で、それぞれ <see cref="Size"/> 四方の正方形として作ってある。
///
/// 対応する定義は samples/ios/KsSettingsViewSample/SampleIconBadge.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleIconBadge.kt。
/// </remarks>
public static class SampleIconBadge
{
    /// <summary>バッジの一辺。画像の実寸と一致させる。</summary>
    public const double Size = 29;

    /// <summary>バッジの角丸半径。</summary>
    public const double CornerRadius = 7;
}
