using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// items とテンプレートから要素を生成し、生成先コレクションへ差し込む器。
/// </summary>
/// <remarks>
/// 生成した要素そのものを控える (provenance) ことで、生成区間へ手動で要素を挿入されても
/// 除去対象をテンプレ生成分だけに限定できる。生成・除去は生成先コレクションへの通常の
/// 構造操作として行うため、表示への反映は他の構造変更と同じ経路を通る。
/// items コレクションの購読は弱参照で張り、外部が items を保持し続けても
/// 生成先 (SettingsView / Section) を巻き添えで生かし続けない。
/// </remarks>
/// <typeparam name="T">生成する要素の型</typeparam>
/// <param name="container">テンプレートが設定されている側。テンプレートの出し分けに渡す</param>
/// <param name="targetProvider">生成先コレクションを返す関数</param>
internal sealed class KsItemsSourceBinder<T>(BindableObject container, Func<IList<T>?> targetProvider)
    : IKsCollectionObserver
    where T : BindableObject
{
    private readonly BindableObject _container = container;

    private readonly Func<IList<T>?> _targetProvider = targetProvider;

    /// <summary>テンプレートから生成した要素 (items の並び順)。</summary>
    private readonly List<T> _generated = [];

    private KsWeakCollectionSubscription? _subscription;
    private IEnumerable? _itemsSource;
    private DataTemplate? _itemTemplate;
    private int _startIndex;

    /// <summary>items コレクションを差し替える。</summary>
    /// <param name="itemsSource">新しい items コレクション。null で生成分の除去のみ行う</param>
    public void SetItemsSource(IEnumerable? itemsSource)
    {
        _subscription?.Unsubscribe();
        _subscription = null;
        _itemsSource = itemsSource;

        if (itemsSource is INotifyCollectionChanged observable)
        {
            KsWeakCollectionSubscription subscription = new(this, null);
            subscription.Subscribe(observable);
            _subscription = subscription;
        }

        Regenerate();
    }

    /// <summary>テンプレートを差し替える。</summary>
    /// <param name="itemTemplate">新しいテンプレート</param>
    public void SetItemTemplate(DataTemplate? itemTemplate)
    {
        _itemTemplate = itemTemplate;
        Regenerate();
    }

    /// <summary>生成物を差し込み始める位置を変える。</summary>
    /// <param name="startIndex">生成先コレクション内の開始位置</param>
    public void SetTemplateStartIndex(int startIndex)
    {
        _startIndex = startIndex;
        Regenerate();
    }

    /// <summary>生成先コレクションが差し替わったことを伝える。</summary>
    /// <remarks>旧コレクションは捨てられるため、控えだけを捨てて新しい方へ生成し直す。</remarks>
    public void OnTargetChanged()
    {
        _generated.Clear();
        Generate();
    }

    /// <inheritdoc/>
    public void OnObservedCollectionChanged(object? context, NotifyCollectionChangedEventArgs args)
    {
        if (_itemTemplate is null || _targetProvider() is not { } target)
        {
            return;
        }

        switch (args.Action)
        {
            case NotifyCollectionChangedAction.Add:
                MirrorAdd(target, args);
                break;
            case NotifyCollectionChangedAction.Remove:
                MirrorRemove(target, args);
                break;
            case NotifyCollectionChangedAction.Replace:
                MirrorReplace(target, args);
                break;
            case NotifyCollectionChangedAction.Move:
                MirrorMove(target, args);
                break;
            default:
                Regenerate();
                break;
        }
    }

    private void Regenerate()
    {
        RemoveGenerated();
        Generate();
    }

    private void Generate()
    {
        if (_itemsSource is null || _itemTemplate is null || _targetProvider() is not { } target)
        {
            return;
        }

        int position = Math.Clamp(_startIndex, 0, target.Count);
        foreach (object? item in _itemsSource)
        {
            T created = Create(item);

            // 控えを先に更新する。差し込みが生成先の購読側で失敗しても、生成先に入った要素が
            // 控えから漏れず、後始末 (RemoveGenerated) で取り残されない。
            _generated.Add(created);
            target.Insert(position, created);
            position++;
        }
    }

    private void RemoveGenerated()
    {
        if (_generated.Count == 0)
        {
            return;
        }

        if (_targetProvider() is { } target)
        {
            foreach (T generated in _generated)
            {
                target.Remove(generated);
            }
        }

        _generated.Clear();
    }

    private void MirrorAdd(IList<T> target, NotifyCollectionChangedEventArgs args)
    {
        if (args.NewItems is null)
        {
            return;
        }

        int itemIndex = args.NewStartingIndex >= 0 ? args.NewStartingIndex : _generated.Count;
        foreach (object? item in args.NewItems)
        {
            T created = Create(item);

            // 差し込み位置は控えの現在の並びから求めるため、控えを更新する前に決める。
            int position = Math.Clamp(InsertPosition(target, itemIndex), 0, target.Count);
            _generated.Insert(Math.Clamp(itemIndex, 0, _generated.Count), created);
            target.Insert(position, created);
            itemIndex++;
        }
    }

    private void MirrorRemove(IList<T> target, NotifyCollectionChangedEventArgs args)
    {
        if (args.OldItems is null || args.OldStartingIndex < 0)
        {
            return;
        }

        // 連続した複数件の除去では、先頭を抜いた後も次の対象が同じ位置に来る。
        for (int i = 0; i < args.OldItems.Count; i++)
        {
            if (args.OldStartingIndex >= _generated.Count)
            {
                return;
            }

            T removed = _generated[args.OldStartingIndex];
            _generated.RemoveAt(args.OldStartingIndex);
            target.Remove(removed);
        }
    }

    private void MirrorReplace(IList<T> target, NotifyCollectionChangedEventArgs args)
    {
        if (args.NewItems is null)
        {
            return;
        }

        int startIndex = args.NewStartingIndex >= 0 ? args.NewStartingIndex : args.OldStartingIndex;
        if (startIndex < 0)
        {
            return;
        }

        for (int i = 0; i < args.NewItems.Count; i++)
        {
            int itemIndex = startIndex + i;
            if (itemIndex >= _generated.Count)
            {
                return;
            }

            int position = target.IndexOf(_generated[itemIndex]);
            T created = Create(args.NewItems[i]);
            _generated[itemIndex] = created;

            if (position >= 0)
            {
                target[position] = created;
            }
            else
            {
                target.Insert(Math.Clamp(InsertPosition(target, itemIndex), 0, target.Count), created);
            }
        }
    }

    /// <summary>items の移動を生成先へ写す。</summary>
    /// <remarks>
    /// 複数件をまとめた移動は、元の並びを保つ 1 件ずつの移動へ分解して適用する。
    /// </remarks>
    /// <param name="target">生成先コレクション</param>
    /// <param name="args">items の変更内容</param>
    private void MirrorMove(IList<T> target, NotifyCollectionChangedEventArgs args)
    {
        int count = args.OldItems?.Count ?? 1;
        foreach ((int source, int destination) in
            KsRangeMove.Steps(args.OldStartingIndex, args.NewStartingIndex, count))
        {
            MirrorMoveOne(target, source, destination);
        }
    }

    /// <summary>生成物 1 件を <paramref name="from"/> から <paramref name="to"/> へ移す。</summary>
    /// <param name="target">生成先コレクション</param>
    /// <param name="from">移動元の生成物の並びにおける位置</param>
    /// <param name="to">移動先の生成物の並びにおける位置</param>
    private void MirrorMoveOne(IList<T> target, int from, int to)
    {
        if (from >= _generated.Count || to >= _generated.Count)
        {
            return;
        }

        T moved = _generated[from];
        int oldPosition = target.IndexOf(moved);
        if (oldPosition < 0)
        {
            return;
        }

        _generated.RemoveAt(from);
        int newPosition = PositionAfterRemoval(target, oldPosition, to);
        _generated.Insert(to, moved);

        if (target is ObservableCollection<T> observable)
        {
            observable.Move(oldPosition, newPosition);
        }
        else
        {
            target.RemoveAt(oldPosition);
            target.Insert(Math.Clamp(newPosition, 0, target.Count), moved);
        }
    }

    /// <summary>生成物 <paramref name="itemIndex"/> 番目を差し込む生成先の位置を求める。</summary>
    /// <remarks>
    /// 前後の生成物に隣接させることで、生成区間へ手動要素が挿入されていても生成物どうしの
    /// 並びが items の並びと一致する。
    /// </remarks>
    /// <param name="target">生成先コレクション</param>
    /// <param name="itemIndex">生成物の並びにおける位置</param>
    private int InsertPosition(IList<T> target, int itemIndex)
    {
        if (_generated.Count == 0)
        {
            return Math.Clamp(_startIndex, 0, target.Count);
        }

        if (itemIndex <= 0)
        {
            int first = target.IndexOf(_generated[0]);
            return first >= 0 ? first : Math.Clamp(_startIndex, 0, target.Count);
        }

        int previousIndex = Math.Min(itemIndex, _generated.Count) - 1;
        int previous = target.IndexOf(_generated[previousIndex]);
        return previous >= 0 ? previous + 1 : target.Count;
    }

    /// <summary>移動対象を抜いた後のリストにおける挿入位置を求める。</summary>
    /// <param name="target">生成先コレクション (移動対象をまだ含む)</param>
    /// <param name="removedPosition">移動対象の現在位置</param>
    /// <param name="itemIndex">移動後の生成物の並びにおける位置</param>
    private int PositionAfterRemoval(IList<T> target, int removedPosition, int itemIndex)
    {
        if (_generated.Count == 0)
        {
            return Math.Clamp(_startIndex, 0, target.Count - 1);
        }

        if (itemIndex <= 0)
        {
            return AdjustedIndexOf(target, _generated[0], removedPosition);
        }

        int previousIndex = Math.Min(itemIndex, _generated.Count) - 1;
        return AdjustedIndexOf(target, _generated[previousIndex], removedPosition) + 1;
    }

    private static int AdjustedIndexOf(IList<T> target, T item, int removedPosition)
    {
        int index = target.IndexOf(item);
        return index > removedPosition ? index - 1 : index;
    }

    private T Create(object? item)
    {
        object content = ResolveTemplate(item).CreateContent();
        if (content is not T created)
        {
            throw new InvalidOperationException(
                $"ItemTemplate must create a {typeof(T).Name}, but created {content?.GetType().Name ?? "null"}.");
        }

        created.BindingContext = item;
        return created;
    }

    /// <summary>この item を実体化するテンプレートを決める。</summary>
    /// <remarks>
    /// テンプレートの出し分けが設定されている場合は item ごとに実テンプレートを選び直す。
    /// 選び先が決まらないときは実体化できないため、その場で失敗させる。出し分けが別の出し分けを
    /// 返した場合は MAUI 側が <see cref="NotSupportedException"/> を投げるが、テンプレート解決の
    /// 失敗として他の失敗と同じ例外型へ揃える (元の例外は内部例外として保つ)。
    /// 出し分けの実装自身が <see cref="NotSupportedException"/> を投げた場合も同じ翻訳を受けるため、
    /// 送出元の切り分けには内部例外を見る。翻訳を送出元で絞り込まないのは、MAUI 側の例外メッセージに
    /// 依存すると文言の変更で翻訳が黙って止まるため。
    /// </remarks>
    /// <param name="item">実体化する item</param>
    private DataTemplate ResolveTemplate(object? item)
    {
        if (_itemTemplate is not DataTemplateSelector selector)
        {
            return _itemTemplate!;
        }

        DataTemplate? selected;
        try
        {
            selected = selector.SelectTemplate(item, _container);
        }
        catch (NotSupportedException exception)
        {
            throw new InvalidOperationException(
                "DataTemplateSelector must not return another DataTemplateSelector.", exception);
        }

        return selected
            ?? throw new InvalidOperationException(
                "DataTemplateSelector must return a template for every item.");
    }
}
