using System;
using System.Collections;
using System.Collections.Generic;
using System.Windows.Input;
using KsSettingsView.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView;

/// <summary>
/// 一覧から項目を選ぶ Cell。
/// </summary>
/// <remarks>
/// 選択状態の正は index (<see cref="SelectedIndex"/> / <see cref="SelectedIndices"/>) であり、
/// <see cref="SelectedItem"/> と <see cref="SelectedItems"/> は <see cref="ItemsSource"/> との
/// 相互導出で提供される書き味である。<see cref="ItemsSource"/> は差し替えで反映される
/// (コレクション自身の増減・要素の入れ替えは観測しない)。
/// </remarks>
public class PickerCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(PickerCell),
        default(string));

    /// <summary><see cref="ItemsSource"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ItemsSourceProperty = BindableProperty.Create(
        nameof(ItemsSource),
        typeof(IList),
        typeof(PickerCell),
        default(IList),
        validateValue: static (_, value) => RejectNullElements(value as IList),
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).OnItemsSourceChanged());

    /// <summary><see cref="DisplayMember"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DisplayMemberProperty = BindableProperty.Create(
        nameof(DisplayMember),
        typeof(string),
        typeof(PickerCell),
        default(string),
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).ProjectItems());

    /// <summary><see cref="SubDisplayMember"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SubDisplayMemberProperty = BindableProperty.Create(
        nameof(SubDisplayMember),
        typeof(string),
        typeof(PickerCell),
        default(string),
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).ProjectItems());

    /// <summary><see cref="SelectionMode"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SelectionModeProperty = BindableProperty.Create(
        nameof(SelectionMode),
        typeof(PickerSelectionMode),
        typeof(PickerCell),
        PickerSelectionMode.Single);

    /// <summary><see cref="SelectedIndex"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty SelectedIndexProperty = BindableProperty.Create(
        nameof(SelectedIndex),
        typeof(int?),
        typeof(PickerCell),
        default(int?),
        defaultBindingMode: BindingMode.TwoWay,
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).SyncSelectedItemFromIndex());

    /// <summary><see cref="SelectedIndices"/> のバッキングプロパティ。</summary>
    /// <remarks>ユーザー操作で値が変わるため、既定の binding mode は TwoWay。</remarks>
    public static readonly BindableProperty SelectedIndicesProperty = BindableProperty.Create(
        nameof(SelectedIndices),
        typeof(IList<int>),
        typeof(PickerCell),
        default(IList<int>),
        defaultBindingMode: BindingMode.TwoWay,
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).SyncSelectedItemsFromIndices());

    /// <summary><see cref="SelectedItem"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SelectedItemProperty = BindableProperty.Create(
        nameof(SelectedItem),
        typeof(object),
        typeof(PickerCell),
        default(object),
        defaultBindingMode: BindingMode.TwoWay,
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).SyncIndexFromSelectedItem());

    /// <summary><see cref="SelectedItems"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SelectedItemsProperty = BindableProperty.Create(
        nameof(SelectedItems),
        typeof(IList),
        typeof(PickerCell),
        default(IList),
        defaultBindingMode: BindingMode.TwoWay,
        propertyChanged: static (bindable, _, _) =>
            ((PickerCell)bindable).SyncIndicesFromSelectedItems());

    /// <summary><see cref="SelectedCommand"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SelectedCommandProperty = BindableProperty.Create(
        nameof(SelectedCommand),
        typeof(ICommand),
        typeof(PickerCell),
        default(ICommand));

    /// <summary><see cref="MaxSelectedNumber"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty MaxSelectedNumberProperty = BindableProperty.Create(
        nameof(MaxSelectedNumber),
        typeof(int),
        typeof(PickerCell),
        0);

    /// <summary><see cref="PageTitle"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty PageTitleProperty = BindableProperty.Create(
        nameof(PageTitle),
        typeof(string),
        typeof(PickerCell),
        default(string));

    /// <summary><see cref="AccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty AccentColorProperty = BindableProperty.Create(
        nameof(AccentColor),
        typeof(Color),
        typeof(PickerCell),
        default(Color));

    /// <summary>相互導出による書き戻しが、逆向きの導出を再び起こさないための番人。</summary>
    private bool _syncingSelection;

    /// <summary><see cref="ItemsSource"/> の設定時に確定した候補の写し。</summary>
    private IReadOnlyList<object> _items = [];

    /// <summary>候補へ表示射影を適用した写し。表示・輸送はこれを参照する。</summary>
    private IReadOnlyList<KsPickerItemSnapshot> _projectedItems = [];

    /// <summary>行の右側に表示する値文字列。null なら現在の選択から作られる。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>
    /// 選択候補の項目。任意の型の要素を持てる。
    /// </summary>
    /// <remarks>
    /// 要素に null は指定できない (指定すると <see cref="ArgumentException"/>)。null を許すと
    /// <see cref="SelectedItem"/> の null が「未選択」と「null 要素の選択」のどちらか決まらなくなるため。
    /// 設定時に要素と表示射影の結果を写し取り、以後の差し替えまで表示・逆引き・
    /// <see cref="SelectedItem"/> / <see cref="SelectedItems"/> の導出はその写しを参照する。
    /// </remarks>
    public IList? ItemsSource
    {
        get => (IList?)GetValue(ItemsSourceProperty);
        set => SetValue(ItemsSourceProperty, value);
    }

    /// <summary>
    /// 主表示に使う要素のプロパティ名。null なら要素の <c>ToString()</c> を表示する。
    /// </summary>
    /// <remarks>
    /// 解決対象は public instance の引数なし readable プロパティ。名前を解決できないときも
    /// <c>ToString()</c> へ落ちる。プロパティ値が string 以外なら <c>ToString()</c> で文字列化し、
    /// null なら空文字列になる。
    /// </remarks>
    public string? DisplayMember
    {
        get => (string?)GetValue(DisplayMemberProperty);
        set => SetValue(DisplayMemberProperty, value);
    }

    /// <summary>
    /// 選択面の候補行に副表示として出す要素のプロパティ名。null で副表示なし。
    /// </summary>
    /// <remarks>
    /// 解決規則は <see cref="DisplayMember"/> と同じ。名前を解決できないとき・プロパティ値が
    /// null のとき・文字列化が空文字列のときは副表示なしとして扱う。
    /// </remarks>
    public string? SubDisplayMember
    {
        get => (string?)GetValue(SubDisplayMemberProperty);
        set => SetValue(SubDisplayMemberProperty, value);
    }

    /// <summary>単一選択か複数選択か。</summary>
    public PickerSelectionMode SelectionMode
    {
        get => (PickerSelectionMode)GetValue(SelectionModeProperty);
        set => SetValue(SelectionModeProperty, value);
    }

    /// <summary>単一選択モードでの選択位置。null で未選択。</summary>
    public int? SelectedIndex
    {
        get => (int?)GetValue(SelectedIndexProperty);
        set => SetValue(SelectedIndexProperty, value);
    }

    /// <summary>複数選択モードでの選択位置の並び。</summary>
    public IList<int>? SelectedIndices
    {
        get => (IList<int>?)GetValue(SelectedIndicesProperty);
        set => SetValue(SelectedIndicesProperty, value);
    }

    /// <summary>
    /// 単一選択モードで選ばれている項目。
    /// </summary>
    /// <remarks>
    /// <see cref="ItemsSource"/> と <see cref="SelectedIndex"/> から導出される。設定すると
    /// 値等価で最初に一致した位置が <see cref="SelectedIndex"/> になり、候補に見つからない
    /// ときは未選択になる。<see cref="SelectedIndex"/> が範囲外のときは null を返す
    /// (このとき正は <see cref="SelectedIndex"/> の値)。<see cref="ItemsSource"/> がまだ
    /// 無いときの設定値は捨てずに保持し、候補が届いた時点で逆引きして位置へ解決する。
    /// </remarks>
    public object? SelectedItem
    {
        get => GetValue(SelectedItemProperty);
        set => SetValue(SelectedItemProperty, value);
    }

    /// <summary>
    /// 複数選択モードで選ばれている項目の並び。
    /// </summary>
    /// <remarks>
    /// <see cref="ItemsSource"/> と <see cref="SelectedIndices"/> から位置の昇順で導出される
    /// (範囲外の位置は導出から除外され、有効な選択が無ければ空の並びになる)。設定すると各要素を
    /// 値等価で最初に一致した位置へ逆引きして <see cref="SelectedIndices"/> になり、見つからない
    /// 要素は保持されない。null の設定は空の並び (選択なし) と同じ意味になる。
    /// <see cref="ItemsSource"/> がまだ無いときの設定値は捨てずに保持し、候補が届いた時点で
    /// 逆引きして位置の並びへ解決する。公開値は常に正
    /// (<see cref="SelectedIndices"/>) からの再導出で確定するため、同値要素を重ねて設定しても
    /// 位置としては 1 件に揃い、設定した並びがそのまま返るとは限らない。
    /// </remarks>
    public IList? SelectedItems
    {
        get => (IList?)GetValue(SelectedItemsProperty);
        set => SetValue(SelectedItemsProperty, value);
    }

    /// <summary>
    /// 利用者が選択を確定した後に実行する Command。
    /// </summary>
    /// <remarks>
    /// 単一選択では <see cref="SelectedItem"/>、複数選択では <see cref="SelectedItems"/> を
    /// 引数として、選択値の反映後に実行する。実行可否は確認しない。
    /// </remarks>
    public ICommand? SelectedCommand
    {
        get => (ICommand?)GetValue(SelectedCommandProperty);
        set => SetValue(SelectedCommandProperty, value);
    }

    /// <summary>複数選択モードでの選択上限。0 で無制限。</summary>
    public int MaxSelectedNumber
    {
        get => (int)GetValue(MaxSelectedNumberProperty);
        set => SetValue(MaxSelectedNumberProperty, value);
    }

    /// <summary>選択面のタイトル。null でタイトルなし。</summary>
    public string? PageTitle
    {
        get => (string?)GetValue(PageTitleProperty);
        set => SetValue(PageTitleProperty, value);
    }

    /// <summary>この行の強調表示の色。null で既定スタイルを継承。</summary>
    public Color? AccentColor
    {
        get => (Color?)GetValue(AccentColorProperty);
        set => SetValue(AccentColorProperty, value);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsPickerCellSnapshot>() with
        {
            ValueText = ValueText,
            Items = _projectedItems,
            SelectionMode = SelectionMode,
            SelectedIndex = SelectedIndex,
            SelectedIndices = KsWireValues.Indices(SelectedIndices),
            MaxSelectedNumber = MaxSelectedNumber,
            PageTitle = PageTitle,
            AccentColor = KsWireValues.Color(AccentColor),
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(ItemsSource) or nameof(DisplayMember)
        or nameof(SubDisplayMember) or nameof(SelectionMode) or nameof(SelectedIndex)
        or nameof(SelectedIndices) or nameof(MaxSelectedNumber) or nameof(PageTitle)
        or nameof(AccentColor)
        || base.AffectsSnapshot(propertyName);

    /// <summary>確定通知の種類に対応する選択項目を引数として完了を通知する。</summary>
    /// <remarks>
    /// 選択面は表示を始めた時点のモードで動く。引数の選び方を <see cref="SelectionMode"/> の
    /// 現在値に委ねると、選択面の表示中にモードが変えられたとき、利用者が確定した種類とは違う
    /// 引数を渡してしまう。そのため根拠は届いた確定通知の種類に置く。
    /// </remarks>
    /// <param name="mode">確定通知の種類</param>
    internal void NotifySelectionCompleted(PickerSelectionMode mode)
    {
        object? parameter = mode == PickerSelectionMode.Multiple
            ? SelectedItems
            : SelectedItem;
        SelectedCommand?.Execute(parameter);
    }

    /// <summary>
    /// 候補の並びに null 要素が無いことを確かめる。
    /// </summary>
    /// <remarks>
    /// 値の検査は代入の前に行われるため、送出した時点で <see cref="ItemsSource"/> は元の値のまま残る。
    /// </remarks>
    /// <param name="items">検査する並び。null は候補なしとして通す</param>
    /// <exception cref="ArgumentException">null 要素が含まれるとき</exception>
    private static bool RejectNullElements(IList? items)
    {
        if (items is null)
        {
            return true;
        }

        for (int i = 0; i < items.Count; i++)
        {
            if (items[i] is null)
            {
                throw new ArgumentException(
                    $"{nameof(ItemsSource)} の要素に null は指定できない (位置 {i})。",
                    nameof(ItemsSource));
            }
        }

        return true;
    }

    /// <summary>候補が 1 件以上あるかどうか。無い間は相互導出を行わない。</summary>
    private bool HasItems => _items.Count > 0;

    /// <summary>候補の写しと表示射影を取り直し、選択の導出をやり直す。</summary>
    /// <remarks>
    /// 候補が無かった間に設定された <see cref="SelectedItem"/> / <see cref="SelectedItems"/> は
    /// 導出されずに残っているため、候補が届いた時点でそれを逆引きの起点にする。候補が既にあった
    /// 状態からの差し替えでは、いつもどおり位置を正として選択項目を引き直す。
    /// 候補を null / 空へ差し替えたときは選択を消さずそのまま保ち、次に候補が届いた時点で
    /// 逆引きし直す (候補が一時的に外れただけで選択が失われないようにするため)。
    /// </remarks>
    private void OnItemsSourceChanged()
    {
        bool hadNoItems = !HasItems;
        CaptureItems();
        ProjectItems();
        if (!HasItems)
        {
            return;
        }

        if (hadNoItems && HasPendingItemSelection())
        {
            RestoreSelectionFromItems();
            return;
        }

        SyncSelectionFromIndices();
    }

    /// <summary>候補が無い間に設定された選択項目が残っているかどうか。</summary>
    private bool HasPendingItemSelection()
        => SelectedItem is not null || SelectedItems is { Count: > 0 };

    /// <summary>現在の <see cref="ItemsSource"/> から候補の写しを取る。</summary>
    private void CaptureItems()
    {
        IList? source = ItemsSource;
        if (source is null || source.Count == 0)
        {
            _items = [];
            return;
        }

        object[] captured = new object[source.Count];
        for (int i = 0; i < source.Count; i++)
        {
            captured[i] = source[i]!;
        }

        _items = captured;
    }

    /// <summary>候補の写しへ表示射影を適用する。</summary>
    private void ProjectItems()
    {
        if (_items.Count == 0)
        {
            _projectedItems = [];
            return;
        }

        string? displayMember = DisplayMember;
        string? subDisplayMember = SubDisplayMember;
        KsPickerItemSnapshot[] projected = new KsPickerItemSnapshot[_items.Count];
        for (int i = 0; i < _items.Count; i++)
        {
            object item = _items[i];
            projected[i] = new KsPickerItemSnapshot(
                KsMemberProjection.Text(item, displayMember),
                KsMemberProjection.SubText(item, subDisplayMember));
        }

        _projectedItems = projected;
    }

    /// <summary>現在の位置から単一選択・複数選択の選択項目をまとめて導出して反映する。</summary>
    private void SyncSelectionFromIndices()
    {
        if (_syncingSelection)
        {
            return;
        }

        _syncingSelection = true;
        try
        {
            ApplySelectedItem(ResolveSelectedItem());
            ApplySelectedItems(ResolveSelectedItems());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>候補が無い間に設定された選択項目を起点に、位置と公開値を組み直す。</summary>
    private void RestoreSelectionFromItems()
    {
        _syncingSelection = true;
        try
        {
            if (SelectedItem is not null)
            {
                SetValue(SelectedIndexProperty, ResolveSelectedIndex());
            }

            ApplySelectedItem(ResolveSelectedItem());

            if (SelectedItems is { Count: > 0 })
            {
                ApplySelectedIndices(ResolveSelectedIndices());
            }

            ApplySelectedItems(ResolveSelectedItems());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>現在の位置から選択項目を導出して反映する。</summary>
    private void SyncSelectedItemFromIndex()
    {
        if (_syncingSelection || !HasItems)
        {
            return;
        }

        _syncingSelection = true;
        try
        {
            ApplySelectedItem(ResolveSelectedItem());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>現在の選択項目から位置を導出して反映する。</summary>
    /// <remarks>
    /// 候補に無い値は保持せず null へ揃える。位置が未選択のまま選択項目だけが選択済みに見える
    /// 食い違いを残さないため。<see cref="ItemsSource"/> がまだ無いときは導出も書き戻しもせず、
    /// 設定された値をそのまま残す (候補が届いた時点で <see cref="OnItemsSourceChanged"/> が
    /// 逆引きし直す)。バインドの適用順で選択が黙って失われないようにするため。
    /// </remarks>
    private void SyncIndexFromSelectedItem()
    {
        if (_syncingSelection || !HasItems)
        {
            return;
        }

        _syncingSelection = true;
        try
        {
            SetValue(SelectedIndexProperty, ResolveSelectedIndex());
            ApplySelectedItem(ResolveSelectedItem());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>現在の位置の並びから選択項目の並びを導出して反映する。</summary>
    private void SyncSelectedItemsFromIndices()
    {
        if (_syncingSelection || !HasItems)
        {
            return;
        }

        _syncingSelection = true;
        try
        {
            ApplySelectedItems(ResolveSelectedItems());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>現在の選択項目の並びから位置の並びを導出して反映する。</summary>
    /// <remarks>
    /// 逆引きできなかった要素は保持しない。反映後の公開値は正である位置の並びから引き直すため、
    /// 同値要素を重ねて設定しても 1 件に揃う。<see cref="ItemsSource"/> がまだ無いときは導出も
    /// 書き戻しもせず、設定された並びをそのまま残す (候補が届いた時点で
    /// <see cref="OnItemsSourceChanged"/> が逆引きし直す)。
    /// </remarks>
    private void SyncIndicesFromSelectedItems()
    {
        if (_syncingSelection || !HasItems)
        {
            return;
        }

        _syncingSelection = true;
        try
        {
            ApplySelectedIndices(ResolveSelectedIndices());
            ApplySelectedItems(ResolveSelectedItems());
        }
        finally
        {
            _syncingSelection = false;
        }
    }

    /// <summary>
    /// 導出した選択項目を、現在値と同じ実体でないときだけ書き戻す。
    /// </summary>
    /// <remarks>
    /// 導出のたびに作り直した値を無条件に書き戻すと、書き戻し自身が次の導出を呼ぶ連鎖が
    /// 止まらなくなる。導出元は候補の写しの要素そのものなので、参照が同じなら書き戻さない
    /// ことで連鎖を収束させる。値等価は逆引き (<see cref="IndexOfItem"/>) だけに使い、
    /// 書き戻しの可否には使わない — 値等価な別実体を設定されたときも公開値を候補の写しの
    /// 実体へ揃えるため (正は位置であり、公開値はそこからの再導出で確定する)。
    /// </remarks>
    /// <param name="item">書き戻す選択項目</param>
    private void ApplySelectedItem(object? item)
    {
        if (ReferenceEquals(SelectedItem, item))
        {
            return;
        }

        SetValue(SelectedItemProperty, item);
    }

    /// <summary>導出した選択項目の並びを、現在値と実体の並びが違うときだけ書き戻す。</summary>
    /// <param name="items">書き戻す選択項目の並び</param>
    private void ApplySelectedItems(IList items)
    {
        if (SelectedItems is IList current && SameItems(current, items))
        {
            return;
        }

        SetValue(SelectedItemsProperty, items);
    }

    /// <summary>導出した位置の並びを、現在値と集合として違うときだけ書き戻す。</summary>
    /// <param name="indices">書き戻す位置の並び</param>
    private void ApplySelectedIndices(IList<int> indices)
    {
        if (SelectedIndices is IList<int> current && KsWireValues.IndicesEqual(current, indices))
        {
            return;
        }

        SetValue(SelectedIndicesProperty, indices);
    }

    /// <summary>2 つの並びが同じ実体を同じ順で持つかどうかを返す。</summary>
    /// <remarks>
    /// 値等価ではなく参照で比べる。値等価な別実体が並んでいるだけの状態を「同じ」と見なすと、
    /// 候補の実体への正規化がそこで止まってしまうため。
    /// </remarks>
    /// <param name="left">比較する並び</param>
    /// <param name="right">比較する並び</param>
    private static bool SameItems(IList left, IList right)
    {
        if (left.Count != right.Count)
        {
            return false;
        }

        for (int i = 0; i < left.Count; i++)
        {
            if (!ReferenceEquals(left[i], right[i]))
            {
                return false;
            }
        }

        return true;
    }

    /// <summary>現在の位置に対応する候補。範囲外・候補なしでは null。</summary>
    private object? ResolveSelectedItem()
    {
        int? index = SelectedIndex;
        if (index is not int position || position < 0 || position >= _items.Count)
        {
            return null;
        }

        return _items[position];
    }

    /// <summary>現在の選択項目に対応する位置。見つからなければ null。</summary>
    private int? ResolveSelectedIndex()
    {
        int index = IndexOfItem(SelectedItem);
        return index >= 0 ? index : null;
    }

    /// <summary>現在の位置の並びに対応する候補を位置の昇順で並べたもの。</summary>
    private IList ResolveSelectedItems()
    {
        IReadOnlyList<int> indices = KsWireValues.Indices(SelectedIndices);
        List<object> selected = new(indices.Count);
        foreach (int index in indices)
        {
            if (index >= 0 && index < _items.Count)
            {
                selected.Add(_items[index]);
            }
        }

        return selected;
    }

    /// <summary>現在の選択項目の並びに対応する位置の並び (昇順・重複なし)。</summary>
    private IList<int> ResolveSelectedIndices()
    {
        IList? selected = SelectedItems;
        if (selected is null || selected.Count == 0)
        {
            return [];
        }

        SortedSet<int> indices = [];
        for (int i = 0; i < selected.Count; i++)
        {
            int index = IndexOfItem(selected[i]);
            if (index >= 0)
            {
                indices.Add(index);
            }
        }

        return [.. indices];
    }

    /// <summary>候補の写しの中で、指定した項目に値等価で最初に一致する位置。無ければ -1。</summary>
    /// <param name="item">探す項目</param>
    private int IndexOfItem(object? item)
    {
        if (item is null)
        {
            return -1;
        }

        for (int i = 0; i < _items.Count; i++)
        {
            if (Equals(_items[i], item))
            {
                return i;
            }
        }

        return -1;
    }
}
