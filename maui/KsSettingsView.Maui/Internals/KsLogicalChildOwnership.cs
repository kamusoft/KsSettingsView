using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// コレクションの要素を持ち主の論理子として所有し、持ち主の BindingContext を配る器。
/// </summary>
/// <remarks>
/// Section / Cell にとっての所有の寿命は「コレクションへの所属」であり、その増減をここで見る。
/// 論理子であることは Native Host の有無と関係なく成り立つ契約なので、付け外しは Handler の
/// 生成・解放とは切り離してこの器が行う (cross/ADR-0032)。論理子であることで、要素へ設定した
/// <c>AppThemeBinding</c> / <c>DynamicResource</c> が祖先 (ページ / アプリ) の外観と Resources の
/// 変更で再評価される。
/// 配布と引き取りの順序は「継承 BindingContext を配ってから論理子にする」で固定する。逆にすると
/// 論理子化に伴う MAUI 本体の継承配布が先に走り、同じ値を配る手順が二度に分かれて見えるため。
/// どの手順も同じ状態へ何度呼んでも結果は変わらない (持ち主の BindingContext が変わるたびに
/// <see cref="Apply"/> が呼ばれ、付け外しは行わない)。
/// コレクションの購読は弱参照で張り、外部がコレクションを保持し続けても持ち主を
/// 巻き添えで生かし続けない。要素から持ち主への参照は MAUI 本体の <c>Parent</c> だけで、
/// これは弱参照として保たれる。
/// </remarks>
/// <typeparam name="T">配布先の要素の型</typeparam>
/// <param name="owner">論理上の所有者であり BindingContext の供給元</param>
/// <param name="targetProvider">配布先コレクションを返す関数</param>
/// <param name="elementKind">多重配置の例外に載せる要素の種別</param>
internal sealed class KsLogicalChildOwnership<T>(Element owner, Func<IList<T>?> targetProvider, string elementKind)
    : IKsCollectionObserver
    where T : Element
{
    private readonly Element _owner = owner;
    private readonly Func<IList<T>?> _targetProvider = targetProvider;
    private readonly string _elementKind = elementKind;

    /// <summary>今この持ち主の論理子として付けている要素。</summary>
    /// <remarks>
    /// Reset とコレクションの差し替えでは、外れた要素が変更通知に載らない。何を外すべきかを
    /// この控えから求める。
    /// </remarks>
    private readonly HashSet<T> _owned = new(KsReferenceComparer<T>.Instance);

    private KsWeakCollectionSubscription? _subscription;

    /// <summary>配布先コレクションが差し替わったことを伝え、購読と所有をやり直す。</summary>
    /// <remarks>
    /// 旧コレクション由来の要素をすべて外してから新しい方を購読する。旧コレクションへ
    /// その後要素を追加しても、購読が切れているため論理子にはならない。
    /// 引き取れない要素が新コレクションに混ざっていないかは、旧側に触れる前に確かめる。
    /// 解放を済ませた後で例外にすると、表示され続けている旧要素が論理親を失ったまま残る。
    /// </remarks>
    public void OnTargetChanged()
    {
        IList<T>? target = _targetProvider();
        EnsureAdoptable(target);

        _subscription?.Unsubscribe();
        _subscription = null;

        ReleaseAll();

        if (target is INotifyCollectionChanged observable)
        {
            KsWeakCollectionSubscription subscription = new(this, null);
            subscription.Subscribe(observable);
            _subscription = subscription;
        }

        Apply();
    }

    /// <summary>今のコレクションの内容と所有の控えを照合し直す。</summary>
    /// <remarks>
    /// 増減を通知しないコレクション (<see cref="INotifyCollectionChanged"/> でない実体) では、
    /// 設定した後の要素の出入りがここへ届かない。所属を確定させる必要がある時点 — 表示へ
    /// 変換する直前 — にこれを呼ぶと、今そこに居る要素だけが論理子として繋がった状態になる。
    /// 増減が届くコレクションでは付け外しがその時点で済んでいるため、呼んでも何も変わらない。
    /// 引き取れない要素が混ざっている場合は、今の所有に触れる前に例外にする。
    /// </remarks>
    public void Reconcile()
    {
        EnsureAdoptable(_targetProvider());
        ReleaseAbsent();
        Apply();
    }

    /// <summary>現在のコレクション全体へ持ち主の BindingContext を配り、論理子として引き取る。</summary>
    public void Apply()
    {
        if (_targetProvider() is not { } target)
        {
            return;
        }

        foreach (T child in target)
        {
            Adopt(child);
        }
    }

    /// <inheritdoc/>
    public void OnObservedCollectionChanged(object? context, NotifyCollectionChangedEventArgs args)
    {
        if (args.Action == NotifyCollectionChangedAction.Reset)
        {
            ReleaseAbsent();
            Apply();
            return;
        }

        // 外す方を先に済ませる。差し替えで同じ要素が旧側と新側の両方に現れても、所属が
        // 残っている要素は外さないため順序で結果は変わらないが、外れた要素の所有を
        // 引きずったまま新しい要素を引き取らないようにする。
        if (args.OldItems is not null)
        {
            foreach (object? item in args.OldItems)
            {
                if (item is T child)
                {
                    ReleaseIfDetached(child);
                }
            }
        }

        if (args.NewItems is null)
        {
            return;
        }

        foreach (object? item in args.NewItems)
        {
            if (item is T child)
            {
                Adopt(child);
            }
        }
    }

    /// <summary>この要素へ BindingContext を配り、論理子として引き取る。</summary>
    /// <remarks>
    /// 期待する所有者以外の facade 所有者に所有されている要素は引き取らず、その場で多重配置の
    /// 例外にする。引き取ってしまうと、正しく置かれている側の論理親と継承 BindingContext を
    /// 黙って奪うことになる。既にこの持ち主の論理子である要素 (同じコレクションへの二重追加を
    /// 含む) は付け直さない — 同じコレクション内の重複は表示へ変換する時点で弾かれる。
    /// </remarks>
    /// <param name="child">引き取る要素</param>
    private void Adopt(T child)
    {
        if (child is null)
        {
            return;
        }

        if (KsPlacementDiagnostics.IsOwnedElsewhere(_owner, child))
        {
            throw KsPlacementDiagnostics.DuplicatePlacement(_elementKind);
        }

        BindableObject.SetInheritedBindingContext(child, _owner.BindingContext);

        if (child.Parent is null)
        {
            _owner.AddLogicalChild(child);
        }

        _owned.Add(child);
    }

    /// <summary>このコレクションの要素をすべて引き取れることを確かめる。</summary>
    /// <remarks>
    /// 他所の facade 所有者に所有されている要素が 1 件でもあれば多重配置の例外にする。
    /// </remarks>
    /// <param name="target">これから引き取るコレクション。null なら何も確かめない</param>
    private void EnsureAdoptable(IList<T>? target)
    {
        if (target is null)
        {
            return;
        }

        foreach (T child in target)
        {
            if (child is not null && KsPlacementDiagnostics.IsOwnedElsewhere(_owner, child))
            {
                throw KsPlacementDiagnostics.DuplicatePlacement(_elementKind);
            }
        }
    }

    /// <summary>所属が完全に終わった要素だけを論理子から外す。</summary>
    /// <remarks>
    /// 同じ要素が二重に入っていて 1 件だけ除かれた場合は、まだ所属が残っているため外さない。
    /// </remarks>
    /// <param name="child">除去の通知に載った要素</param>
    private void ReleaseIfDetached(T child)
    {
        if (child is null || ContainsReference(_targetProvider(), child))
        {
            return;
        }

        Release(child);
    }

    /// <summary>控えにあってコレクションにもう含まれない要素を外す。</summary>
    private void ReleaseAbsent()
    {
        IList<T>? target = _targetProvider();
        foreach (T child in Snapshot())
        {
            if (!ContainsReference(target, child))
            {
                Release(child);
            }
        }
    }

    /// <summary>控えにある要素をすべて外す。</summary>
    private void ReleaseAll()
    {
        foreach (T child in Snapshot())
        {
            Release(child);
        }
    }

    /// <summary>この要素の論理上の所有を解く。</summary>
    /// <param name="child">外す要素</param>
    private void Release(T child)
    {
        _owned.Remove(child);

        if (ReferenceEquals(child.Parent, _owner))
        {
            _owner.RemoveLogicalChild(child);
        }
    }

    /// <summary>控えの写しを取る。外す途中で控えを変えるため、走査対象を固定する。</summary>
    private List<T> Snapshot() => [.. _owned];

    /// <summary>このコレクションが指定の要素そのものを含むかどうか。</summary>
    /// <param name="target">調べるコレクション。null なら含まない扱い</param>
    /// <param name="child">探す要素</param>
    private static bool ContainsReference(IList<T>? target, T child)
    {
        if (target is null)
        {
            return false;
        }

        foreach (T item in target)
        {
            if (ReferenceEquals(item, child))
            {
                return true;
            }
        }

        return false;
    }
}
