using System;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using UIKit;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// iOS で <see cref="ImageSource"/> を <see cref="UIImage"/> へ解決する口。
/// </summary>
/// <remarks>
/// 解決は MAUI 標準の画像解決サービスに委ね、結果を型を持たない参照として返す。呼び出しは UI
/// スレッドから行い、完了もそのスレッドへ戻る (await が呼び出し元の同期コンテキストへ戻るため)。
/// 解決できなかった場合は icon なしとして null を返し、例外は外へ出さない。
/// 解決サービスの結果は破棄で後片付けを行うため、画像と一体の <see cref="KsImageLease"/> として
/// 渡し、破棄の時機は受け取り側に委ねる。
///
/// <para>
/// ただし後片付けの口をそのまま渡してよいのは、facade が所有する画像に限る。iOS の後片付けは
/// <see cref="UIImage"/> の破棄であり、asset catalog の名前や拡張子なしのファイル名は
/// UIKit の名前付き画像キャッシュ経由で解決されて、同じ名前なら常に同一インスタンスが返る。
/// この画像を所有しているのはキャッシュであって facade ではない — 破棄しても native の実体は
/// キャッシュが抱えたまま解放されず、managed の参照だけが無効になって、同じ画像を表示している
/// 他の Cell や他の SettingsView の表示を壊す。そこで解決結果ごとに所有を分類し
/// (<see cref="KsFileImageOwnership"/>)、キャッシュ所有の画像には後片付けの口を付けない。
/// </para>
/// </remarks>
/// <param name="context">画像解決サービスを引く MAUI のコンテキスト</param>
internal sealed class KsImageResolver(IMauiContext context) : IKsImageResolver
{
    /// <inheritdoc/>
    public void Resolve(ImageSource source, Action<KsImageLease?> completed)
    {
        ArgumentNullException.ThrowIfNull(source);
        ArgumentNullException.ThrowIfNull(completed);

        _ = ResolveAsync(source, completed);
    }

    private async Task ResolveAsync(ImageSource source, Action<KsImageLease?> completed)
    {
        KsImageLease? lease = null;
        try
        {
            IImageSourceService service = context.Services
                .GetRequiredService<IImageSourceServiceProvider>()
                .GetRequiredImageSourceService(source);
            IImageSourceServiceResult<UIImage>? result = await service.GetImageAsync(source);
            if (result?.Value is { } image)
            {
                lease = new KsImageLease(image, CleanupFor(source, image, result));
            }
            else
            {
                result?.Dispose();
            }
        }
        catch (Exception)
        {
            // 途中で失敗したら icon なしとして返す。リースを作った後なら後片付けまで済ませる。
            lease?.Dispose();
            lease = null;
        }

        completed(lease);
    }

    /// <summary>
    /// この解決結果に付ける後片付けの口を決める。
    /// </summary>
    /// <remarks>
    /// ファイル指定だけが名前付き画像キャッシュを経由し得る。URI / stream / font の経路は
    /// 解決のたびに新しい <see cref="UIImage"/> を起こすため、後片付けは常に facade の責任になる。
    /// キャッシュ所有と分かった結果オブジェクトは破棄せずに手放す — 保持しているのは画像への参照と
    /// 「画像を破棄する」処理だけで、それ自体は固有の資源を持たないため、回収に委ねて取りこぼしはない。
    /// 分類そのものが失敗した場合も口を付けない — ここで例外を外へ出すと解決結果を誰も持たないまま
    /// 落とすことになり、破棄してよいかも分からない画像に手を出すことになる。
    /// </remarks>
    /// <param name="source">解決を頼まれた画像の指定</param>
    /// <param name="image">解決できた画像</param>
    /// <param name="result">解決サービスが返した結果 (後片付けの口を兼ねる)</param>
    /// <returns>リースへ渡す後片付けの口。付けない場合は null</returns>
    private static IDisposable? CleanupFor(ImageSource source, UIImage image, IDisposable result)
    {
        if (source is not IFileImageSource file)
        {
            return result;
        }

        try
        {
            bool cacheOwned = KsFileImageOwnership.IsOwnedByPlatformCache(
                file.File,
                image,
                static name => UIImage.FromBundle(name));

            return cacheOwned ? null : result;
        }
        catch (Exception)
        {
            return null;
        }
    }
}
