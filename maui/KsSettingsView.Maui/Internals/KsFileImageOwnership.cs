using System;
using System.IO;

namespace KsSettingsView.Internals;

/// <summary>
/// ファイル指定から解決した画像を facade が後片付けしてよいか (所有しているか) の分類。
/// </summary>
/// <remarks>
/// 決定の経緯は maui/ADR-0026 (所有権分類の採用と参照カウント表の廃止)。
///
/// <para>
/// 画像解決サービスは解決結果に後片付けの口を必ず付けるが、その口が正しいとは限らない。
/// platform の名前付き画像キャッシュ (iOS の <c>imageNamed:</c> 相当) が返した画像は
/// キャッシュの所有物で、同じ名前を解決するたびに同一インスタンスが返る。これを後片付けすると
/// キャッシュが抱えたままの実体を指す他の利用者ごと壊れるため、facade は手を出さない。
/// 逆に画像ファイルから起こした画像は解決のたびに新しい実体であり、後片付けは facade の責任になる。
/// </para>
///
/// <para>
/// 判定は解決の分岐を推し量るのではなく、<b>キャッシュに同じ名前で引き直して、返る実体が
/// 解決結果そのものかどうか</b>を見る。分岐を真似ると、真似た先の実装が変わったときに黙って
/// 食い違い、その食い違いがそのまま誤分類になる。実体の同一性は解決経路に依らず答えが出る。
/// </para>
///
/// <para>
/// 引き直しに使う名前は、画像解決サービスがキャッシュを引くときと同じ形
/// (ディレクトリと拡張子を落とした名前) でなければならない。名前がずれると必ず一致せず、
/// キャッシュ所有の画像を facade 所有と誤って判定する。
/// </para>
///
/// <para>
/// 材料が足りず判定できない入力は「キャッシュ所有 = 後片付けしない」側へ倒す。
/// 取りこぼした結果オブジェクトは固有の資源を持たず GC で回収されるが、誤って後片付けすると
/// 表示中の画像が壊れる。失敗の向きを片側へ揃えることがこの分類の要件になる。
/// </para>
///
/// <para>
/// この分類は <see cref="KsSettingsController"/> が同一画像への再解決で旧リースをその場で
/// 解放できる前提でもある — 共有され得るのはキャッシュ所有 (後片付けの口を持たない) の画像だけで、
/// facade 所有と分類した画像は解決のたびに別実体になり、他のリースと共有されない。
/// </para>
/// </remarks>
internal static class KsFileImageOwnership
{
    /// <summary>
    /// この画像が platform の名前付き画像キャッシュの所有物かどうかを判定する。
    /// </summary>
    /// <remarks>
    /// 解決がキャッシュ経由だった場合、その名前は既にキャッシュ済みなので引き直しは追加の
    /// 読み込みにならない。ファイルから起こした画像だった場合は、同じ名前の資産があれば
    /// この引き直しが新規の読み込みとキャッシュへの常駐を起こし得る。
    /// 表示を壊さないための代償として引き受けている。
    /// </remarks>
    /// <param name="fileName">解決に使ったファイル指定</param>
    /// <param name="image">解決できた platform 画像</param>
    /// <param name="platformNamedImage">
    /// 名前付き画像キャッシュを引く処理。渡す名前はディレクトリと拡張子を落とした形になる
    /// </param>
    /// <returns>キャッシュの所有物で facade が後片付けしてはいけない場合 true</returns>
    public static bool IsOwnedByPlatformCache(
        string? fileName,
        object image,
        Func<string, object?> platformNamedImage)
    {
        ArgumentNullException.ThrowIfNull(image);
        ArgumentNullException.ThrowIfNull(platformNamedImage);

        if (string.IsNullOrEmpty(fileName))
        {
            return true;
        }

        string cacheName = Path.GetFileNameWithoutExtension(fileName);
        if (string.IsNullOrEmpty(cacheName))
        {
            // 引き直せる名前を取り出せない。由来を確かめられないので後片付けしない側へ倒す。
            return true;
        }

        return ReferenceEquals(platformNamedImage(cacheName), image);
    }
}
