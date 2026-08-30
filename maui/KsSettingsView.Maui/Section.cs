using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui;

/// <summary>
/// 設定画面を区切る Section。header / footer の内容と Cell 群を持つ。
/// </summary>
/// <remarks>
/// <see cref="Cells"/> が content property であり、XAML では Section の直下に Cell を並べる。
/// header / footer にはテキストと View の両方を指定でき、両方あるときは View が表示される。
/// 指定した View はこの Section の BindingContext を継承する。
/// 実体が <see cref="System.Collections.Specialized.INotifyCollectionChanged"/> を実装する
/// コレクションのときだけ、以後の構造変更が表示へ反映される。
/// <see cref="Cells"/> への操作とプロパティの変更は UI スレッドから行う (呼び出し側契約であり、
/// facade はスレッド marshal を行わない)。
/// 同じ Cell インスタンスを複数の Section へ配置することはできない。
/// </remarks>
[ContentProperty(nameof(Cells))]
public class Section : Element
{
    /// <summary><see cref="HeaderText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderTextProperty = BindableProperty.Create(
        nameof(HeaderText),
        typeof(string),
        typeof(Section),
        default(string));

    /// <summary><see cref="FooterText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterTextProperty = BindableProperty.Create(
        nameof(FooterText),
        typeof(string),
        typeof(Section),
        default(string));

    /// <summary><see cref="HeaderView"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// この Section が SettingsView の変換経路に載っている間は、値を確定させる前に多重配置を
    /// 検査する。値の確定はそれまで置かれていた View の論理上の所有を先に解いてしまい、後から
    /// 見つけた多重配置を元へ戻せないため。検査を通った後は、変更通知を受けた変換経路が論理上の
    /// 所有を確定させる。ここでの確定は、まだ載っていない Section — XAML の構築中や、設定ツリーに
    /// 入れる前の Section — のための受け皿であり、確定済みなら何も起こらない。受け皿には検査を
    /// 行う相手がいないため、既に他所へ置かれている View は引き取らない (この Section が変換経路に
    /// 加わった時点 — Native Host 未接続のまま設定ツリーへ入れた場合は Host 接続時 — で例外になる)。
    /// 検査の失敗は validateValue の false 返却ではなく <see cref="InvalidOperationException"/>
    /// の送出で表す。false を返すと BindableProperty 側が ArgumentException に変換してしまい、
    /// 多重配置が公開契約どおりの例外型で観測できなくなるため (maui/ADR-0022)。
    /// </remarks>
    public static readonly BindableProperty HeaderViewProperty = BindableProperty.Create(
        nameof(HeaderView),
        typeof(View),
        typeof(Section),
        default(View),
        validateValue: static (bindable, value) =>
        {
            Section section = (Section)bindable;
            section.AccessoryGuard?.EnsureAccessoryViewCanBePlaced(
                section,
                KsAccessoryTarget.SectionHeader,
                value as View);
            return true;
        },
        propertyChanged: static (bindable, oldValue, newValue) => KsAccessoryViewOwnership.ReassignIfFree(
            (Section)bindable,
            oldValue as View,
            newValue as View));

    /// <summary><see cref="FooterView"/> のバッキングプロパティ。</summary>
    /// <remarks>検査と所有の確定の扱いは <see cref="HeaderViewProperty"/> と同じ。</remarks>
    public static readonly BindableProperty FooterViewProperty = BindableProperty.Create(
        nameof(FooterView),
        typeof(View),
        typeof(Section),
        default(View),
        validateValue: static (bindable, value) =>
        {
            Section section = (Section)bindable;
            section.AccessoryGuard?.EnsureAccessoryViewCanBePlaced(
                section,
                KsAccessoryTarget.SectionFooter,
                value as View);
            return true;
        },
        propertyChanged: static (bindable, oldValue, newValue) => KsAccessoryViewOwnership.ReassignIfFree(
            (Section)bindable,
            oldValue as View,
            newValue as View));

    /// <summary><see cref="IsVisible"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsVisibleProperty = BindableProperty.Create(
        nameof(IsVisible),
        typeof(bool),
        typeof(Section),
        true);

    /// <summary><see cref="IsHeaderVisible"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsHeaderVisibleProperty = BindableProperty.Create(
        nameof(IsHeaderVisible),
        typeof(bool),
        typeof(Section),
        true);

    /// <summary><see cref="IsFooterVisible"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty IsFooterVisibleProperty = BindableProperty.Create(
        nameof(IsFooterVisible),
        typeof(bool),
        typeof(Section),
        true);

    /// <summary><see cref="HeaderHeight"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderHeightProperty = BindableProperty.Create(
        nameof(HeaderHeight),
        typeof(double?),
        typeof(Section),
        default(double?));

    /// <summary><see cref="Cells"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// 既定値は Section ごとに新しい observable なコレクションを作る。BindableProperty の
    /// 既定値は型で共有されるため、可変コレクションは defaultValueCreator で用意する。
    /// </remarks>
    public static readonly BindableProperty CellsProperty = BindableProperty.Create(
        nameof(Cells),
        typeof(IList<CellBase>),
        typeof(Section),
        defaultValueCreator: static _ => new ObservableCollection<CellBase>(),
        propertyChanged: static (bindable, _, _) =>
        {
            Section section = (Section)bindable;
            section._cellBinder.OnTargetChanged();
            section._cellContextBinder.OnTargetChanged();
        });

    /// <summary><see cref="ItemsSource"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ItemsSourceProperty = BindableProperty.Create(
        nameof(ItemsSource),
        typeof(IEnumerable),
        typeof(Section),
        default(IEnumerable),
        propertyChanged: static (bindable, _, newValue) =>
            ((Section)bindable)._cellBinder.SetItemsSource(newValue as IEnumerable));

    /// <summary><see cref="ItemTemplate"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ItemTemplateProperty = BindableProperty.Create(
        nameof(ItemTemplate),
        typeof(DataTemplate),
        typeof(Section),
        default(DataTemplate),
        propertyChanged: static (bindable, _, newValue) =>
            ((Section)bindable)._cellBinder.SetItemTemplate(newValue as DataTemplate));

    /// <summary><see cref="TemplateStartIndex"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TemplateStartIndexProperty = BindableProperty.Create(
        nameof(TemplateStartIndex),
        typeof(int),
        typeof(Section),
        0,
        propertyChanged: static (bindable, _, newValue) =>
            ((Section)bindable)._cellBinder.SetTemplateStartIndex((int)newValue));

    private readonly KsItemsSourceBinder<CellBase> _cellBinder;
    private readonly KsBindingContextBinder<CellBase> _cellContextBinder;

    private WeakReference<IKsAccessoryViewGuard>? _accessoryGuard;

    /// <summary>空の Section を作る。</summary>
    public Section()
    {
        _cellBinder = new KsItemsSourceBinder<CellBase>(this, () => Cells);
        _cellContextBinder = new KsBindingContextBinder<CellBase>(this, () => Cells);
        _cellContextBinder.OnTargetChanged();
    }

    /// <summary>Section の上に表示するヘッダテキスト。null でヘッダなし。</summary>
    public string? HeaderText
    {
        get => (string?)GetValue(HeaderTextProperty);
        set => SetValue(HeaderTextProperty, value);
    }

    /// <summary>Section の下に表示するフッタテキスト。null でフッタなし。</summary>
    public string? FooterText
    {
        get => (string?)GetValue(FooterTextProperty);
        set => SetValue(FooterTextProperty, value);
    }

    /// <summary>Section の上に表示するヘッダの View。null で View 指定なし。</summary>
    /// <remarks>
    /// 設定されている間は <see cref="HeaderText"/> より優先して表示される。null に戻すと
    /// <see cref="HeaderText"/> の表示へ戻る。
    /// </remarks>
    public View? HeaderView
    {
        get => (View?)GetValue(HeaderViewProperty);
        set => SetValue(HeaderViewProperty, value);
    }

    /// <summary>Section の下に表示するフッタの View。null で View 指定なし。</summary>
    /// <remarks>
    /// 設定されている間は <see cref="FooterText"/> より優先して表示される。null に戻すと
    /// <see cref="FooterText"/> の表示へ戻る。
    /// </remarks>
    public View? FooterView
    {
        get => (View?)GetValue(FooterViewProperty);
        set => SetValue(FooterViewProperty, value);
    }

    /// <summary>
    /// Section を表示するかどうか。false で header / footer / 配下 Cell ごと表示から除外される。
    /// </summary>
    /// <remarks>
    /// 非表示にしている間の内容変更も保持され、true へ戻すと元の位置に変更後の内容で復帰する。
    /// 可視性の切り替えをまたいでも配下 Cell の双方向バインドは機能し続ける。
    /// </remarks>
    public bool IsVisible
    {
        get => (bool)GetValue(IsVisibleProperty);
        set => SetValue(IsVisibleProperty, value);
    }

    /// <summary>ヘッダを表示するかどうか。false で内容があってもヘッダを表示しない。</summary>
    /// <remarks>
    /// 内容が無い (<see cref="HeaderText"/> が null または空文字列で <see cref="HeaderView"/> も
    /// null) ヘッダを、このトグルで表示させることはできない。
    /// 非表示にしている間もヘッダの内容は保持され、true へ戻すとその時点の内容で再表示される。
    /// <see cref="IsFooterVisible"/> および配下 Cell の表示とは独立している。
    /// </remarks>
    public bool IsHeaderVisible
    {
        get => (bool)GetValue(IsHeaderVisibleProperty);
        set => SetValue(IsHeaderVisibleProperty, value);
    }

    /// <summary>フッタを表示するかどうか。false で内容があってもフッタを表示しない。</summary>
    /// <remarks>意味論は <see cref="IsHeaderVisible"/> と対称。</remarks>
    public bool IsFooterVisible
    {
        get => (bool)GetValue(IsFooterVisibleProperty);
        set => SetValue(IsFooterVisibleProperty, value);
    }

    /// <summary>ヘッダの固定高さ。null で既定の自動高さ。</summary>
    public double? HeaderHeight
    {
        get => (double?)GetValue(HeaderHeightProperty);
        set => SetValue(HeaderHeightProperty, value);
    }

    /// <summary>Section が持つ Cell 群。</summary>
    public IList<CellBase> Cells
    {
        get => (IList<CellBase>)GetValue(CellsProperty);
        set => SetValue(CellsProperty, value);
    }

    /// <summary>
    /// <see cref="ItemTemplate"/> から Cell を生成する元になる items。
    /// </summary>
    /// <remarks>
    /// <see cref="ItemTemplate"/> が未設定の間は生成しない。実体が
    /// <see cref="System.Collections.Specialized.INotifyCollectionChanged"/> のときは
    /// items の増減が生成済みの Cell へ反映される。null にすると生成分だけを取り除き、
    /// 手動で追加した Cell は残す。
    /// </remarks>
    public IEnumerable? ItemsSource
    {
        get => (IEnumerable?)GetValue(ItemsSourceProperty);
        set => SetValue(ItemsSourceProperty, value);
    }

    /// <summary>items 1 件から Cell を作るテンプレート。</summary>
    /// <remarks>生成された Cell の BindingContext は対応する item になる。</remarks>
    public DataTemplate? ItemTemplate
    {
        get => (DataTemplate?)GetValue(ItemTemplateProperty);
        set => SetValue(ItemTemplateProperty, value);
    }

    /// <summary>生成した Cell を <see cref="Cells"/> のどこから並べ始めるか。</summary>
    public int TemplateStartIndex
    {
        get => (int)GetValue(TemplateStartIndexProperty);
        set => SetValue(TemplateStartIndexProperty, value);
    }

    /// <summary>
    /// header / footer に置く View の可否を尋ねる相手。設定ツリーに載っていない間は null。
    /// </summary>
    /// <remarks>
    /// 変換経路がこの Section を対応表へ載せている間だけ差し込まれる。
    /// 保持は弱参照で行う — 外部 (ViewModel 等) がこの Section を保持し続けても、尋ね先を
    /// 巻き添えで生かし続けないため (プロパティ変更の購読と同じ規律)。
    /// </remarks>
    internal IKsAccessoryViewGuard? AccessoryGuard
    {
        get => _accessoryGuard is not null && _accessoryGuard.TryGetTarget(out IKsAccessoryViewGuard? guard)
            ? guard
            : null;

        set => _accessoryGuard = value is null ? null : new WeakReference<IKsAccessoryViewGuard>(value);
    }

    /// <summary>
    /// BindingContext の変更を <see cref="Cells"/> の Cell へ配る。
    /// </summary>
    /// <remarks>
    /// XAML で直接並べた Cell にも、SettingsView 経由で届いた BindingContext を渡す。
    /// </remarks>
    protected override void OnBindingContextChanged()
    {
        base.OnBindingContextChanged();
        _cellContextBinder.Apply();
    }
}
