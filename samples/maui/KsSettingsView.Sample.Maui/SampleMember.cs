using System.Collections.Generic;

namespace KsSettingsView.Sample.Maui;

/// <summary>
/// 通知先の候補として並べる架空のメンバー。
/// </summary>
/// <remarks>
/// PickerCell の object 候補デモで、<c>DisplayMember</c> に <see cref="Name"/>、
/// <c>SubDisplayMember</c> に <see cref="Role"/> を指定して表示を射影する。
/// <see cref="Role"/> が null の要素は副表示を持たず、選択面では 1 行で描画される。
///
/// 対応する定義は samples/ios/KsSettingsViewSample/SampleMember.swift と
/// samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleMember.kt。
/// </remarks>
/// <param name="Name">主表示に使う名前</param>
/// <param name="Role">副表示に使う役割 (null なら副表示なし)</param>
public sealed record SampleMember(string Name, string? Role)
{
    /// <summary>
    /// 入力 Cell デモの PickerCell (object 候補) セクションが並べる候補。
    /// </summary>
    /// <remarks>副表示の長さがばらばらで、副表示を持たない要素も混ざる並びにしてある。</remarks>
    public static IReadOnlyList<SampleMember> NotificationTargets { get; } =
    [
        new SampleMember("佐藤 花子", "プロダクトマネージャー"),
        new SampleMember("鈴木 一郎", "モバイルアプリ開発チーム / テックリード (iOS・Android 横断アーキテクチャ担当)"),
        new SampleMember("高橋 次郎", "QA エンジニア"),
        new SampleMember("全体アナウンス", null),
        new SampleMember("田中 三郎", "デザイナー"),
    ];
}
