using System;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// <see cref="ImageSource"/> を platform の画像へ解決する口。
/// </summary>
/// <remarks>
/// 解決した画像の型は platform ごとに異なるため、この境界では型を持たない参照として扱い、
/// 輸送 DTO へ載せる platform 実装だけが実体の型を知る (maui/ADR-0009 と同じ seam の考え方)。
/// 解決は非同期に進み、結果は呼び出したときと同じ UI スレッドで通知される。解決できなかった
/// 場合は null を渡して完了する (例外は投げない)。
/// 結果は <see cref="KsImageLease"/> として渡され、受け取り側が画像を使わなくなった時点で
/// 破棄する責任を持つ。
/// </remarks>
internal interface IKsImageResolver
{
    /// <summary>画像の解決を始める。</summary>
    /// <param name="source">解決する画像の指定</param>
    /// <param name="completed">解決結果を受け取る処理。失敗時は null が渡る</param>
    void Resolve(ImageSource source, Action<KsImageLease?> completed);
}
