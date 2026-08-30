using System;
using System.Collections.Generic;
using System.Linq;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;
using Element = Microsoft.Maui.Controls.Element;

namespace KsSettingsView.Maui.Tests.Fakes;

/// <summary>
/// platform を持たないテスト用の実体化の口。
/// </summary>
/// <remarks>
/// wrapper の代わりに目印のオブジェクトを作り、実体化の記録を <see cref="Leases"/> に残す。
/// MAUI の Handler は View と 1 対 1 であり、同じ View を包み直すと platform 側の Handler が
/// 再利用される。この共有を <see cref="FakeViewHandler"/> で再現し、実体の後片付けが
/// 別の実体の Handler を巻き添えにする経路をテストから見えるようにする。
/// 記録は実体を強く保持するため、回収を検証するときは <see cref="Forget"/> で先に手放す。
/// </remarks>
internal sealed class FakeViewMaterializer : IKsViewMaterializer
{
    private readonly List<FakeViewLease> _leases = [];

    /// <summary>View ごとに 1 つだけ存在する Handler の代役。</summary>
    private readonly Dictionary<View, FakeViewHandler> _handlers =
        new(KsReferenceComparer<View>.Instance);

    /// <summary>実体化された順の記録。</summary>
    public IReadOnlyList<FakeViewLease> Leases => _leases;

    /// <inheritdoc/>
    public IKsViewLease Materialize(View view, Action measureInvalidated)
    {
        ArgumentNullException.ThrowIfNull(view);
        ArgumentNullException.ThrowIfNull(measureInvalidated);

        FakeViewLease lease = new(view, HandlerFor(view), measureInvalidated);
        _leases.Add(lease);
        return lease;
    }

    /// <summary>指定 View に対する最後の実体化。</summary>
    /// <param name="view">対象の View</param>
    public FakeViewLease LatestFor(View view)
        => _leases.Last(lease => ReferenceEquals(lease.View, view));

    /// <summary>指定 View が実体化された回数。</summary>
    /// <param name="view">対象の View</param>
    public int CountFor(View view)
        => _leases.Count(lease => ReferenceEquals(lease.View, view));

    /// <summary>記録を手放す。回収の検証で、観測している側が実体を生かし続けないようにする。</summary>
    public void Forget()
    {
        _leases.Clear();
        _handlers.Clear();
    }

    /// <summary>この View の Handler を返す。生きているものがあれば再利用する。</summary>
    /// <remarks>切断済みの Handler は使えないため、platform 側と同じく作り直す。</remarks>
    /// <param name="view">対象の View</param>
    private FakeViewHandler HandlerFor(View view)
    {
        if (_handlers.TryGetValue(view, out FakeViewHandler? existing) && existing.IsConnected)
        {
            return existing;
        }

        FakeViewHandler created = new();
        _handlers[view] = created;
        return created;
    }
}

/// <summary>実体化 1 件分の記録。</summary>
/// <param name="view">包んだ View</param>
/// <param name="handler">包んだ View の Handler の代役</param>
/// <param name="measureInvalidated">必要サイズが変わったときに呼ぶ処理</param>
internal sealed class FakeViewLease(View view, FakeViewHandler handler, Action measureInvalidated)
    : IKsViewLease
{
    /// <inheritdoc/>
    public object PlatformView { get; } = new FakePlatformView();

    /// <summary>包んだ View。</summary>
    public View View { get; } = view;

    /// <summary>
    /// 実体化された瞬間の論理上の親。
    /// </summary>
    /// <remarks>
    /// platform 実装はここで Handler を作るため、この時点で所有が確定していなければ
    /// BindingContext の定まらない View に対して Handler が作られる。
    /// </remarks>
    public Element? ParentAtMaterialize { get; } = view.Parent;

    /// <summary>実体化された瞬間の BindingContext。</summary>
    public object? BindingContextAtMaterialize { get; } = view.BindingContext;

    /// <summary>包んだ View の Handler の代役。同じ View の実体どうしで共有される。</summary>
    public FakeViewHandler Handler { get; } = handler;

    /// <summary>破棄が呼ばれた回数。</summary>
    public int DisposeCount { get; private set; }

    /// <summary>破棄済みかどうか。</summary>
    public bool IsDisposed => DisposeCount > 0;

    /// <summary>破棄の瞬間に走らせる観測処理。破棄の時機を他の記録と突き合わせるために使う。</summary>
    public Action? OnDispose { get; set; }

    /// <summary>包んだ View の必要サイズが変わったことにする。</summary>
    public void RaiseMeasureInvalidated() => measureInvalidated();

    /// <inheritdoc/>
    public void Dispose()
    {
        DisposeCount++;

        // platform 実装の後片付けは包んだ View の Handler を切る。
        Handler.Disconnect();
        OnDispose?.Invoke();
    }
}

/// <summary>Handler の代役。View と 1 対 1 で、切断されたかどうかだけを覚える。</summary>
internal sealed class FakeViewHandler
{
    /// <summary>接続されているかどうか。</summary>
    public bool IsConnected { get; private set; } = true;

    /// <summary>接続を切る。</summary>
    public void Disconnect() => IsConnected = false;
}

/// <summary>platform view の代わりに置く目印。</summary>
internal sealed class FakePlatformView
{
}
