using System;
using System.Threading.Tasks;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Drawable = Android.Graphics.Drawables.Drawable;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// Android で <see cref="ImageSource"/> を <see cref="Drawable"/> へ解決する口。
/// </summary>
/// <remarks>
/// 解決は MAUI 標準の画像解決サービスに委ね、結果を型を持たない参照として返す。呼び出しは UI
/// スレッドから行い、完了もそのスレッドへ戻る (await が呼び出し元の同期コンテキストへ戻るため)。
/// 解決できなかった場合は icon なしとして null を返し、例外は外へ出さない。
/// 解決サービスの結果は破棄で後片付けを行うため、画像と一体の <see cref="KsImageLease"/> として
/// 渡し、破棄の時機は受け取り側に委ねる。
/// </remarks>
/// <param name="context">画像解決サービスと Android の Context を引く MAUI のコンテキスト</param>
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
            IImageSourceServiceResult<Drawable>? result =
                await service.GetDrawableAsync(source, context.Context!);
            if (result?.Value is { } drawable)
            {
                lease = new KsImageLease(drawable, result);
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
}
