using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Internals;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Tests.Fakes;

/// <summary>
/// 解決の完了をテストが任意の時点で起こせる画像解決の口。
/// </summary>
/// <remarks>
/// 依頼を受け取っても解決せず <see cref="Pending"/> に積むだけで、<see cref="Complete"/> を
/// 呼んだ時点で完了する。積まれた依頼を任意の順で完了できるため、追い抜きの競合を再現できる。
/// 完了のさせ方は 2 通りあり、<see cref="CompleteTracked"/> は後片付けの口をリースへ渡す
/// (facade が所有する画像に解決できたときの形)。<see cref="CompleteCacheOwned"/> は口を渡さない
/// (platform のキャッシュが所有する画像と分類されたときの形) ため、同じ画像を複数の依頼へ渡して
/// 共有を再現しても、どのリースの破棄でも後片付けが起きないことを確かめられる。
/// </remarks>
internal sealed class FakeImageResolver : IKsImageResolver
{
    private readonly List<PendingRequest> _pending = [];

    /// <summary>まだ完了していない依頼 (受け取った順)。</summary>
    public IReadOnlyList<PendingRequest> Pending => _pending;

    /// <inheritdoc/>
    public void Resolve(ImageSource source, Action<KsImageLease?> completed)
    {
        ArgumentNullException.ThrowIfNull(source);
        ArgumentNullException.ThrowIfNull(completed);

        _pending.Add(new PendingRequest(source, completed));
    }

    /// <summary>指定の画像に対する最新の依頼を完了させる。</summary>
    /// <param name="source">依頼時の画像</param>
    /// <param name="icon">解決結果。null で解決失敗</param>
    public void Complete(ImageSource source, object? icon)
        => Complete(Latest(source), icon);

    /// <summary>指定の依頼を完了させる。</summary>
    /// <param name="request">完了させる依頼</param>
    /// <param name="icon">解決結果。null で解決失敗</param>
    public void Complete(PendingRequest request, object? icon)
    {
        _pending.Remove(request);
        request.Completed(icon is null ? null : new KsImageLease(icon, null));
    }

    /// <summary>指定の画像に対する最新の依頼を、破棄を観測できる結果で完了させる。</summary>
    /// <param name="source">依頼時の画像</param>
    /// <param name="icon">解決結果</param>
    /// <returns>結果に紐づく後片付けの口</returns>
    public DisposeProbe CompleteTracked(ImageSource source, object icon)
        => CompleteTracked(Latest(source), icon);

    /// <summary>指定の依頼を、破棄を観測できる結果で完了させる。</summary>
    /// <param name="request">完了させる依頼</param>
    /// <param name="icon">解決結果</param>
    /// <returns>結果に紐づく後片付けの口</returns>
    public DisposeProbe CompleteTracked(PendingRequest request, object icon)
    {
        DisposeProbe probe = new();
        _pending.Remove(request);
        request.Completed(new KsImageLease(icon, probe));
        return probe;
    }

    /// <summary>指定の画像に対する最新の依頼を、キャッシュ所有の画像として完了させる。</summary>
    /// <param name="source">依頼時の画像</param>
    /// <param name="icon">解決結果。同じインスタンスを複数の依頼へ渡すと共有になる</param>
    public void CompleteCacheOwned(ImageSource source, object icon)
        => CompleteCacheOwned(Latest(source), icon);

    /// <summary>指定の依頼を、キャッシュ所有の画像として完了させる。</summary>
    /// <remarks>
    /// platform のキャッシュが所有する画像と分類されたときと同じく、後片付けの口を持たない
    /// リースを渡す。破棄しても画像には何も起きない。
    /// </remarks>
    /// <param name="request">完了させる依頼</param>
    /// <param name="icon">解決結果。同じインスタンスを複数の依頼へ渡すと共有になる</param>
    public void CompleteCacheOwned(PendingRequest request, object icon)
    {
        _pending.Remove(request);
        request.Completed(new KsImageLease(icon, null));
    }

    private PendingRequest Latest(ImageSource source)
        => _pending.Last(pending => ReferenceEquals(pending.Source, source));

    /// <summary>まだ完了していない依頼 1 件。</summary>
    /// <param name="Source">解決を頼まれた画像</param>
    /// <param name="Completed">結果を渡す先</param>
    internal sealed record PendingRequest(ImageSource Source, Action<KsImageLease?> Completed);

    /// <summary>破棄されたかどうかと、破棄された時点の状況を観測できる後片付けの口。</summary>
    internal sealed class DisposeProbe : IDisposable
    {
        /// <summary>破棄が呼ばれた回数。</summary>
        public int DisposeCount { get; private set; }

        /// <summary>破棄済みかどうか。</summary>
        public bool IsDisposed => DisposeCount > 0;

        /// <summary>破棄の瞬間に走らせる観測処理。破棄時機を他の記録と突き合わせるために使う。</summary>
        public Action? OnDispose { get; set; }

        /// <inheritdoc/>
        public void Dispose()
        {
            DisposeCount++;
            OnDispose?.Invoke();
        }
    }
}
