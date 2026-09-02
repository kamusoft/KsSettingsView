using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using KsSettingsView.Internals;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;

namespace KsSettingsView;

/// <summary>
/// Native の設定画面を MAUI のビューとして配置するコントロール。
/// </summary>
/// <remarks>
/// <see cref="Root"/> が content property であり、XAML では SettingsView の直下に Section を並べる。
/// <see cref="Root"/> / <see cref="Section.Cells"/> への操作と、Section / Cell のプロパティ変更は
/// UI スレッドから行う (呼び出し側契約であり、facade はスレッド marshal を行わない)。
/// 同じ Section / Cell インスタンスを複数箇所へ配置することはできない。
/// </remarks>
[ContentProperty(nameof(Root))]
public class SettingsView : View
{
    /// <summary><see cref="Root"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// 既定値は SettingsView ごとに新しい <see cref="SettingsRoot"/> を作る。BindableProperty の
    /// 既定値は型で共有されるため、可変コレクションは defaultValueCreator で用意する。
    /// </remarks>
    public static readonly BindableProperty RootProperty = BindableProperty.Create(
        nameof(Root),
        typeof(IList<Section>),
        typeof(SettingsView),
        defaultValueCreator: static _ => new SettingsRoot(),
        propertyChanged: static (bindable, _, newValue) =>
        {
            SettingsView view = (SettingsView)bindable;
            view._controller.SetRootCollection(newValue as IList<Section>);
            view._sectionBinder.OnTargetChanged();
            view._sectionContextBinder.OnTargetChanged();
        });

    /// <summary><see cref="RootHeaderText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty RootHeaderTextProperty = BindableProperty.Create(
        nameof(RootHeaderText),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._controller
                .SetRootAccessoryText(KsAccessoryTarget.RootHeader, (string?)newValue));

    /// <summary><see cref="RootFooterText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty RootFooterTextProperty = BindableProperty.Create(
        nameof(RootFooterText),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._controller
                .SetRootAccessoryText(KsAccessoryTarget.RootFooter, (string?)newValue));

    /// <summary><see cref="RootHeaderView"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// 値を確定させる前に多重配置を検査する。値の確定はそれまで置かれていた View の置き場所を
    /// 先に動かしてしまい、後から見つけた多重配置を元へ戻せないため。検査を通った後の置き場所と
    /// 論理上の所有の確定は、変更通知を受けた変換経路が行う。
    /// 検査の失敗は validateValue の false 返却ではなく <see cref="InvalidOperationException"/> の
    /// 送出で表す。false を返すと BindableProperty 側が ArgumentException に変換してしまい、
    /// 多重配置が公開契約どおりの例外型で観測できなくなるため (maui/ADR-0022)。
    /// 変換経路は SettingsView の構築時に作られるため、それより前に値が来ても検査は行わない。
    /// </remarks>
    public static readonly BindableProperty RootHeaderViewProperty = BindableProperty.Create(
        nameof(RootHeaderView),
        typeof(View),
        typeof(SettingsView),
        default(View),
        validateValue: static (bindable, value) =>
        {
            ((SettingsView)bindable)._controller?
                .EnsureRootAccessoryViewCanBePlaced(KsAccessoryTarget.RootHeader, value as View);
            return true;
        },
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._controller
                .SetRootAccessoryView(KsAccessoryTarget.RootHeader, newValue as View));

    /// <summary><see cref="RootFooterView"/> のバッキングプロパティ。</summary>
    /// <remarks>検査と置き場所の確定の扱いは <see cref="RootHeaderViewProperty"/> と同じ。</remarks>
    public static readonly BindableProperty RootFooterViewProperty = BindableProperty.Create(
        nameof(RootFooterView),
        typeof(View),
        typeof(SettingsView),
        default(View),
        validateValue: static (bindable, value) =>
        {
            ((SettingsView)bindable)._controller?
                .EnsureRootAccessoryViewCanBePlaced(KsAccessoryTarget.RootFooter, value as View);
            return true;
        },
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._controller
                .SetRootAccessoryView(KsAccessoryTarget.RootFooter, newValue as View));

    /// <summary><see cref="ItemsSource"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ItemsSourceProperty = BindableProperty.Create(
        nameof(ItemsSource),
        typeof(IEnumerable),
        typeof(SettingsView),
        default(IEnumerable),
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._sectionBinder.SetItemsSource(newValue as IEnumerable));

    /// <summary><see cref="ItemTemplate"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ItemTemplateProperty = BindableProperty.Create(
        nameof(ItemTemplate),
        typeof(DataTemplate),
        typeof(SettingsView),
        default(DataTemplate),
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._sectionBinder.SetItemTemplate(newValue as DataTemplate));

    /// <summary><see cref="TemplateStartIndex"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TemplateStartIndexProperty = BindableProperty.Create(
        nameof(TemplateStartIndex),
        typeof(int),
        typeof(SettingsView),
        0,
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._sectionBinder.SetTemplateStartIndex((int)newValue));

    /// <summary><see cref="ListStyle"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// 既定スタイルとは別経路で運ぶため、変更は <see cref="ApplyTheme"/> ではなく変換経路の
    /// スタイル設定を呼ぶ (maui/ADR-0023)。
    /// </remarks>
    public static readonly BindableProperty ListStyleProperty = BindableProperty.Create(
        nameof(ListStyle),
        typeof(SettingsViewStyle),
        typeof(SettingsView),
        SettingsViewStyle.Classic,
        propertyChanged: static (bindable, _, newValue) =>
            ((SettingsView)bindable)._controller.SetStyle((SettingsViewStyle)newValue));

    /// <summary><see cref="SeparatorColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SeparatorColorProperty = BindableProperty.Create(
        nameof(SeparatorColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="SelectedColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SelectedColorProperty = BindableProperty.Create(
        nameof(SelectedColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellBackgroundColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellBackgroundColorProperty = BindableProperty.Create(
        nameof(CellBackgroundColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellAccentColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellAccentColorProperty = BindableProperty.Create(
        nameof(CellAccentColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="DisabledTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty DisabledTextColorProperty = BindableProperty.Create(
        nameof(DisabledTextColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="ScrollIndicatorVisible"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ScrollIndicatorVisibleProperty = BindableProperty.Create(
        nameof(ScrollIndicatorVisible),
        typeof(bool?),
        typeof(SettingsView),
        default(bool?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="RowHeight"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty RowHeightProperty = BindableProperty.Create(
        nameof(RowHeight),
        typeof(int?),
        typeof(SettingsView),
        default(int?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HasUnevenRows"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HasUnevenRowsProperty = BindableProperty.Create(
        nameof(HasUnevenRows),
        typeof(bool?),
        typeof(SettingsView),
        default(bool?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderTextColorProperty = BindableProperty.Create(
        nameof(HeaderTextColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderBackgroundColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderBackgroundColorProperty = BindableProperty.Create(
        nameof(HeaderBackgroundColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderFontFamilyProperty = BindableProperty.Create(
        nameof(HeaderFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderFontSizeProperty = BindableProperty.Create(
        nameof(HeaderFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderFontAttributesProperty = BindableProperty.Create(
        nameof(HeaderFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="HeaderHeight"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HeaderHeightProperty = BindableProperty.Create(
        nameof(HeaderHeight),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="FooterTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterTextColorProperty = BindableProperty.Create(
        nameof(FooterTextColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="FooterBackgroundColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterBackgroundColorProperty = BindableProperty.Create(
        nameof(FooterBackgroundColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="FooterFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterFontFamilyProperty = BindableProperty.Create(
        nameof(FooterFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="FooterFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterFontSizeProperty = BindableProperty.Create(
        nameof(FooterFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="FooterFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty FooterFontAttributesProperty = BindableProperty.Create(
        nameof(FooterFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellTitleColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellTitleColorProperty = BindableProperty.Create(
        nameof(CellTitleColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellTitleFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellTitleFontFamilyProperty = BindableProperty.Create(
        nameof(CellTitleFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellTitleFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellTitleFontSizeProperty = BindableProperty.Create(
        nameof(CellTitleFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellTitleFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellTitleFontAttributesProperty = BindableProperty.Create(
        nameof(CellTitleFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellValueTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellValueTextColorProperty = BindableProperty.Create(
        nameof(CellValueTextColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellValueTextFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellValueTextFontFamilyProperty = BindableProperty.Create(
        nameof(CellValueTextFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellValueTextFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellValueTextFontSizeProperty = BindableProperty.Create(
        nameof(CellValueTextFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellValueTextFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellValueTextFontAttributesProperty = BindableProperty.Create(
        nameof(CellValueTextFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellDescriptionColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellDescriptionColorProperty = BindableProperty.Create(
        nameof(CellDescriptionColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellDescriptionFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellDescriptionFontFamilyProperty = BindableProperty.Create(
        nameof(CellDescriptionFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellDescriptionFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellDescriptionFontSizeProperty = BindableProperty.Create(
        nameof(CellDescriptionFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellDescriptionFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellDescriptionFontAttributesProperty = BindableProperty.Create(
        nameof(CellDescriptionFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellHintTextColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellHintTextColorProperty = BindableProperty.Create(
        nameof(CellHintTextColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellHintFontFamily"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellHintFontFamilyProperty = BindableProperty.Create(
        nameof(CellHintFontFamily),
        typeof(string),
        typeof(SettingsView),
        default(string),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellHintFontSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellHintFontSizeProperty = BindableProperty.Create(
        nameof(CellHintFontSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellHintFontAttributes"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellHintFontAttributesProperty = BindableProperty.Create(
        nameof(CellHintFontAttributes),
        typeof(FontAttributes?),
        typeof(SettingsView),
        default(FontAttributes?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellPlaceholderColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellPlaceholderColorProperty = BindableProperty.Create(
        nameof(CellPlaceholderColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellIconSize"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellIconSizeProperty = BindableProperty.Create(
        nameof(CellIconSize),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="CellIconRadius"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CellIconRadiusProperty = BindableProperty.Create(
        nameof(CellIconRadius),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="SectionMargin"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// 値の検証は行わない。負の成分や過大な値の正規化は Native の描画時に行われる。
    /// </remarks>
    public static readonly BindableProperty SectionMarginProperty = BindableProperty.Create(
        nameof(SectionMargin),
        typeof(Thickness?),
        typeof(SettingsView),
        default(Thickness?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="SectionCornerRadius"/> のバッキングプロパティ。</summary>
    /// <remarks>値の検証は行わない。箱の寸法に対する clamp は Native の描画時に行われる。</remarks>
    public static readonly BindableProperty SectionCornerRadiusProperty = BindableProperty.Create(
        nameof(SectionCornerRadius),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="SectionBorderWidth"/> のバッキングプロパティ。</summary>
    /// <remarks>値の検証は行わない。負値の正規化は Native の描画時に行われる。</remarks>
    public static readonly BindableProperty SectionBorderWidthProperty = BindableProperty.Create(
        nameof(SectionBorderWidth),
        typeof(double?),
        typeof(SettingsView),
        default(double?),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    /// <summary><see cref="SectionBorderColor"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty SectionBorderColorProperty = BindableProperty.Create(
        nameof(SectionBorderColor),
        typeof(Color),
        typeof(SettingsView),
        default(Color),
        propertyChanged: static (bindable, _, _) => ((SettingsView)bindable).ApplyTheme());

    private readonly KsSettingsController _controller;
    private readonly KsItemsSourceBinder<Section> _sectionBinder;
    private readonly KsBindingContextBinder<Section> _sectionContextBinder;

    /// <summary>空の SettingsView を作る。</summary>
    public SettingsView()
    {
        _controller = new KsSettingsController(this);
        _sectionBinder = new KsItemsSourceBinder<Section>(this, () => Root);
        _sectionContextBinder = new KsBindingContextBinder<Section>(this, () => Root);
        _controller.SetRootCollection(Root);
        _sectionContextBinder.OnTargetChanged();
    }

    /// <summary>設定画面を構成する Section 群。</summary>
    public IList<Section> Root
    {
        get => (IList<Section>)GetValue(RootProperty);
        set => SetValue(RootProperty, value);
    }

    /// <summary>設定画面全体の先頭に表示するヘッダテキスト。null でヘッダなし。</summary>
    public string? RootHeaderText
    {
        get => (string?)GetValue(RootHeaderTextProperty);
        set => SetValue(RootHeaderTextProperty, value);
    }

    /// <summary>設定画面全体の末尾に表示するフッタテキスト。null でフッタなし。</summary>
    public string? RootFooterText
    {
        get => (string?)GetValue(RootFooterTextProperty);
        set => SetValue(RootFooterTextProperty, value);
    }

    /// <summary>設定画面全体の先頭に表示するヘッダの View。null で View 指定なし。</summary>
    /// <remarks>
    /// 設定されている間は <see cref="RootHeaderText"/> より優先して表示される。null に戻すと
    /// <see cref="RootHeaderText"/> の表示へ戻る。この View は SettingsView の BindingContext を
    /// 継承する。
    /// </remarks>
    public View? RootHeaderView
    {
        get => (View?)GetValue(RootHeaderViewProperty);
        set => SetValue(RootHeaderViewProperty, value);
    }

    /// <summary>設定画面全体の末尾に表示するフッタの View。null で View 指定なし。</summary>
    /// <remarks>
    /// 設定されている間は <see cref="RootFooterText"/> より優先して表示される。null に戻すと
    /// <see cref="RootFooterText"/> の表示へ戻る。この View は SettingsView の BindingContext を
    /// 継承する。
    /// </remarks>
    public View? RootFooterView
    {
        get => (View?)GetValue(RootFooterViewProperty);
        set => SetValue(RootFooterViewProperty, value);
    }

    /// <summary>
    /// <see cref="ItemTemplate"/> から Section を生成する元になる items。
    /// </summary>
    /// <remarks>
    /// <see cref="ItemTemplate"/> が未設定の間は生成しない。実体が
    /// <see cref="System.Collections.Specialized.INotifyCollectionChanged"/> のときは
    /// items の増減が生成済みの Section へ反映される。null にすると生成分だけを取り除き、
    /// 手動で追加した Section は残す。
    /// </remarks>
    public IEnumerable? ItemsSource
    {
        get => (IEnumerable?)GetValue(ItemsSourceProperty);
        set => SetValue(ItemsSourceProperty, value);
    }

    /// <summary>items 1 件から Section を作るテンプレート。</summary>
    /// <remarks>生成された Section の BindingContext は対応する item になる。</remarks>
    public DataTemplate? ItemTemplate
    {
        get => (DataTemplate?)GetValue(ItemTemplateProperty);
        set => SetValue(ItemTemplateProperty, value);
    }

    /// <summary>生成した Section を <see cref="Root"/> のどこから並べ始めるか。</summary>
    public int TemplateStartIndex
    {
        get => (int)GetValue(TemplateStartIndexProperty);
        set => SetValue(TemplateStartIndexProperty, value);
    }

    /// <summary>設定画面の見た目スタイル。既定は <see cref="SettingsViewStyle.Classic"/>。</summary>
    /// <remarks>
    /// 切り替えると Section の装飾と区切り線の規則が変わる。設定内容と Cell の値、Section / Cell の
    /// identity は変化しない。箱の余白・角丸・ボーダーは <see cref="SectionMargin"/> をはじめとする
    /// Section 装飾のプロパティで調整する。
    /// </remarks>
    public SettingsViewStyle ListStyle
    {
        get => (SettingsViewStyle)GetValue(ListStyleProperty);
        set => SetValue(ListStyleProperty, value);
    }

    /// <summary>行の区切り線の色。null で Native 既定。</summary>
    public Color? SeparatorColor
    {
        get => (Color?)GetValue(SeparatorColorProperty);
        set => SetValue(SeparatorColorProperty, value);
    }

    /// <summary>行を選択したときの背景色。null で Native 既定。</summary>
    public Color? SelectedColor
    {
        get => (Color?)GetValue(SelectedColorProperty);
        set => SetValue(SelectedColorProperty, value);
    }

    /// <summary>Cell の既定背景色。null で Native 既定。</summary>
    public Color? CellBackgroundColor
    {
        get => (Color?)GetValue(CellBackgroundColorProperty);
        set => SetValue(CellBackgroundColorProperty, value);
    }

    /// <summary>スイッチ・チェック・選択印などの既定の強調色。null で Native 既定。</summary>
    public Color? CellAccentColor
    {
        get => (Color?)GetValue(CellAccentColorProperty);
        set => SetValue(CellAccentColorProperty, value);
    }

    /// <summary>無効な行のテキスト色。null で Native 既定。</summary>
    public Color? DisabledTextColor
    {
        get => (Color?)GetValue(DisabledTextColorProperty);
        set => SetValue(DisabledTextColorProperty, value);
    }

    /// <summary>スクロールインジケータを表示するかどうか。null で Native 既定。</summary>
    public bool? ScrollIndicatorVisible
    {
        get => (bool?)GetValue(ScrollIndicatorVisibleProperty);
        set => SetValue(ScrollIndicatorVisibleProperty, value);
    }

    /// <summary>行の高さの基準値。null で Native 既定。</summary>
    public int? RowHeight
    {
        get => (int?)GetValue(RowHeightProperty);
        set => SetValue(RowHeightProperty, value);
    }

    /// <summary>行ごとに高さを変えるかどうか。null で Native 既定。</summary>
    public bool? HasUnevenRows
    {
        get => (bool?)GetValue(HasUnevenRowsProperty);
        set => SetValue(HasUnevenRowsProperty, value);
    }

    /// <summary>Section ヘッダのテキスト色。null で Native 既定。</summary>
    public Color? HeaderTextColor
    {
        get => (Color?)GetValue(HeaderTextColorProperty);
        set => SetValue(HeaderTextColorProperty, value);
    }

    /// <summary>Section ヘッダの背景色。null で Native 既定。</summary>
    public Color? HeaderBackgroundColor
    {
        get => (Color?)GetValue(HeaderBackgroundColorProperty);
        set => SetValue(HeaderBackgroundColorProperty, value);
    }

    /// <summary>Section ヘッダのフォントファミリ。null で Native 既定。</summary>
    public string? HeaderFontFamily
    {
        get => (string?)GetValue(HeaderFontFamilyProperty);
        set => SetValue(HeaderFontFamilyProperty, value);
    }

    /// <summary>Section ヘッダのフォントサイズ。null で Native 既定。</summary>
    public double? HeaderFontSize
    {
        get => (double?)GetValue(HeaderFontSizeProperty);
        set => SetValue(HeaderFontSizeProperty, value);
    }

    /// <summary>Section ヘッダの太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? HeaderFontAttributes
    {
        get => (FontAttributes?)GetValue(HeaderFontAttributesProperty);
        set => SetValue(HeaderFontAttributesProperty, value);
    }

    /// <summary>Section ヘッダの既定高さ。null で Native 既定。</summary>
    public double? HeaderHeight
    {
        get => (double?)GetValue(HeaderHeightProperty);
        set => SetValue(HeaderHeightProperty, value);
    }

    /// <summary>Section フッタのテキスト色。null で Native 既定。</summary>
    public Color? FooterTextColor
    {
        get => (Color?)GetValue(FooterTextColorProperty);
        set => SetValue(FooterTextColorProperty, value);
    }

    /// <summary>Section フッタの背景色。null で Native 既定。</summary>
    public Color? FooterBackgroundColor
    {
        get => (Color?)GetValue(FooterBackgroundColorProperty);
        set => SetValue(FooterBackgroundColorProperty, value);
    }

    /// <summary>Section フッタのフォントファミリ。null で Native 既定。</summary>
    public string? FooterFontFamily
    {
        get => (string?)GetValue(FooterFontFamilyProperty);
        set => SetValue(FooterFontFamilyProperty, value);
    }

    /// <summary>Section フッタのフォントサイズ。null で Native 既定。</summary>
    public double? FooterFontSize
    {
        get => (double?)GetValue(FooterFontSizeProperty);
        set => SetValue(FooterFontSizeProperty, value);
    }

    /// <summary>Section フッタの太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? FooterFontAttributes
    {
        get => (FontAttributes?)GetValue(FooterFontAttributesProperty);
        set => SetValue(FooterFontAttributesProperty, value);
    }

    /// <summary>Cell タイトルの既定色。null で Native 既定。</summary>
    public Color? CellTitleColor
    {
        get => (Color?)GetValue(CellTitleColorProperty);
        set => SetValue(CellTitleColorProperty, value);
    }

    /// <summary>Cell タイトルの既定フォントファミリ。null で Native 既定。</summary>
    public string? CellTitleFontFamily
    {
        get => (string?)GetValue(CellTitleFontFamilyProperty);
        set => SetValue(CellTitleFontFamilyProperty, value);
    }

    /// <summary>Cell タイトルの既定フォントサイズ。null で Native 既定。</summary>
    public double? CellTitleFontSize
    {
        get => (double?)GetValue(CellTitleFontSizeProperty);
        set => SetValue(CellTitleFontSizeProperty, value);
    }

    /// <summary>Cell タイトルの既定の太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? CellTitleFontAttributes
    {
        get => (FontAttributes?)GetValue(CellTitleFontAttributesProperty);
        set => SetValue(CellTitleFontAttributesProperty, value);
    }

    /// <summary>値テキストの既定色。null で Cell タイトルの既定色を継承。</summary>
    public Color? CellValueTextColor
    {
        get => (Color?)GetValue(CellValueTextColorProperty);
        set => SetValue(CellValueTextColorProperty, value);
    }

    /// <summary>値テキストの既定フォントファミリ。null で Native 既定。</summary>
    public string? CellValueTextFontFamily
    {
        get => (string?)GetValue(CellValueTextFontFamilyProperty);
        set => SetValue(CellValueTextFontFamilyProperty, value);
    }

    /// <summary>値テキストの既定フォントサイズ。null で Native 既定。</summary>
    public double? CellValueTextFontSize
    {
        get => (double?)GetValue(CellValueTextFontSizeProperty);
        set => SetValue(CellValueTextFontSizeProperty, value);
    }

    /// <summary>値テキストの既定の太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? CellValueTextFontAttributes
    {
        get => (FontAttributes?)GetValue(CellValueTextFontAttributesProperty);
        set => SetValue(CellValueTextFontAttributesProperty, value);
    }

    /// <summary>説明文の既定色。null で Native 既定。</summary>
    public Color? CellDescriptionColor
    {
        get => (Color?)GetValue(CellDescriptionColorProperty);
        set => SetValue(CellDescriptionColorProperty, value);
    }

    /// <summary>説明文の既定フォントファミリ。null で Native 既定。</summary>
    public string? CellDescriptionFontFamily
    {
        get => (string?)GetValue(CellDescriptionFontFamilyProperty);
        set => SetValue(CellDescriptionFontFamilyProperty, value);
    }

    /// <summary>説明文の既定フォントサイズ。null で Native 既定。</summary>
    public double? CellDescriptionFontSize
    {
        get => (double?)GetValue(CellDescriptionFontSizeProperty);
        set => SetValue(CellDescriptionFontSizeProperty, value);
    }

    /// <summary>説明文の既定の太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? CellDescriptionFontAttributes
    {
        get => (FontAttributes?)GetValue(CellDescriptionFontAttributesProperty);
        set => SetValue(CellDescriptionFontAttributesProperty, value);
    }

    /// <summary>ヒントテキストの既定色。null で既定の強調色を継承。</summary>
    public Color? CellHintTextColor
    {
        get => (Color?)GetValue(CellHintTextColorProperty);
        set => SetValue(CellHintTextColorProperty, value);
    }

    /// <summary>ヒントテキストの既定フォントファミリ。null で Native 既定。</summary>
    public string? CellHintFontFamily
    {
        get => (string?)GetValue(CellHintFontFamilyProperty);
        set => SetValue(CellHintFontFamilyProperty, value);
    }

    /// <summary>ヒントテキストの既定フォントサイズ。null で Native 既定。</summary>
    public double? CellHintFontSize
    {
        get => (double?)GetValue(CellHintFontSizeProperty);
        set => SetValue(CellHintFontSizeProperty, value);
    }

    /// <summary>ヒントテキストの既定の太字・斜体の指定。null で Native 既定。</summary>
    public FontAttributes? CellHintFontAttributes
    {
        get => (FontAttributes?)GetValue(CellHintFontAttributesProperty);
        set => SetValue(CellHintFontAttributesProperty, value);
    }

    /// <summary>EntryCell のプレースホルダの既定文字色。null で Native 既定。</summary>
    public Color? CellPlaceholderColor
    {
        get => (Color?)GetValue(CellPlaceholderColorProperty);
        set => SetValue(CellPlaceholderColorProperty, value);
    }

    /// <summary>アイコンの既定表示サイズ。null で Native 既定。</summary>
    public double? CellIconSize
    {
        get => (double?)GetValue(CellIconSizeProperty);
        set => SetValue(CellIconSizeProperty, value);
    }

    /// <summary>アイコンの既定角丸半径。null で Native 既定。</summary>
    public double? CellIconRadius
    {
        get => (double?)GetValue(CellIconRadiusProperty);
        set => SetValue(CellIconRadiusProperty, value);
    }

    /// <summary>
    /// Section (ヘッダ・Cell の箱・フッタを一体とした表示単位) の外側余白。null で Native 既定。
    /// </summary>
    /// <remarks>
    /// <see cref="Thickness.Left"/> / <see cref="Thickness.Right"/> は物理的な左右ではなく論理方向
    /// (leading / trailing) として解釈する — 右から左へ書く言語環境では
    /// <see cref="Thickness.Left"/> の値が画面右側に現れる。左右の解決は Native の方向解決機構が
    /// 行い、SettingsView は <see cref="VisualElement.FlowDirection"/> を参照しない (maui/ADR-0024)。
    /// <see cref="SettingsViewStyle.Classic"/> では Section の境界を全幅に保つため上下成分だけが
    /// 適用され、左右成分は無視される。値そのものはスタイルによらず Native へ渡される。
    /// 隣り合う Section の間隔は、前の Section の下成分と次の Section の上成分の合計になる。
    /// 負の成分は Native の描画時に 0 として扱われる。
    /// 型が nullable な <see cref="Thickness"/> のため XAML の型変換は自動では解決されない。
    /// 属性記法 (<c>SectionMargin="16,22,16,0"</c>) を通すために型変換器を明示している。
    /// </remarks>
    [System.ComponentModel.TypeConverter(typeof(Microsoft.Maui.Converters.ThicknessTypeConverter))]
    public Thickness? SectionMargin
    {
        get => (Thickness?)GetValue(SectionMarginProperty);
        set => SetValue(SectionMarginProperty, value);
    }

    /// <summary>
    /// <see cref="SettingsViewStyle.Modern"/> の Section の箱の角丸半径。null で Native 既定。
    /// </summary>
    /// <remarks>箱の寸法に対して大きすぎる値は Native の描画時に幾何的に許される値へ抑えられる。</remarks>
    public double? SectionCornerRadius
    {
        get => (double?)GetValue(SectionCornerRadiusProperty);
        set => SetValue(SectionCornerRadiusProperty, value);
    }

    /// <summary>
    /// <see cref="SettingsViewStyle.Modern"/> の Section の箱のボーダー幅。null で Native 既定
    /// (ボーダーなし)。
    /// </summary>
    public double? SectionBorderWidth
    {
        get => (double?)GetValue(SectionBorderWidthProperty);
        set => SetValue(SectionBorderWidthProperty, value);
    }

    /// <summary>
    /// <see cref="SettingsViewStyle.Modern"/> の Section の箱のボーダー色。null で Native 既定
    /// (透明)。
    /// </summary>
    public Color? SectionBorderColor
    {
        get => (Color?)GetValue(SectionBorderColorProperty);
        set => SetValue(SectionBorderColorProperty, value);
    }

    /// <summary>変換経路。テストから内部状態を確かめるために公開する。</summary>
    internal KsSettingsController Controller => _controller;

    /// <summary>
    /// BindingContext の変更を <see cref="Root"/> の Section へ配る。
    /// </summary>
    /// <remarks>
    /// XAML で直接並べた Section と、その配下の Cell にもページの BindingContext を届けるため、
    /// 階層ごとに明示して配る。
    /// </remarks>
    protected override void OnBindingContextChanged()
    {
        base.OnBindingContextChanged();
        _sectionContextBinder.Apply();
    }

    /// <summary>
    /// 継承した背景色の変更を既定スタイルへ反映する。
    /// </summary>
    /// <remarks>
    /// 設定画面全体の背景は <see cref="VisualElement.BackgroundColor"/> がそのまま既定スタイルの
    /// 背景色になる。他の既定スタイルのプロパティは自分のバッキングプロパティから反映する。
    /// </remarks>
    /// <param name="propertyName">変更されたプロパティの名前</param>
    protected override void OnPropertyChanged([CallerMemberName] string? propertyName = null)
    {
        base.OnPropertyChanged(propertyName);

        if (propertyName == nameof(BackgroundColor))
        {
            ApplyTheme();
        }
    }

    /// <summary>
    /// gateway を接続し、この SettingsView が使う gateway を返す。
    /// </summary>
    /// <remarks>
    /// gateway は SettingsView と同じ寿命を持つ。初回だけ <paramref name="gatewayFactory"/> で
    /// 作って接続し、その時点の設定ツリー全体を表示へ反映させる。接続済みなら既存の gateway を
    /// そのまま返すため、Native Host の解放をまたいでも作り直されない。
    /// ユーザー操作の通知は Native Host のある間だけ起きるため、ここで毎回受け取り始める。
    /// </remarks>
    /// <typeparam name="T">gateway の実装型</typeparam>
    /// <param name="gatewayFactory">未接続のときに gateway を作る関数</param>
    /// <param name="dispatcher">内容更新の flush を予約する実行口</param>
    /// <param name="images">icon の画像を platform の表現へ解決する口</param>
    /// <param name="views">accessory と Cell の内容の View を platform view へ実体化する口</param>
    internal T ConnectGateway<T>(
        Func<T> gatewayFactory,
        IKsDispatcher dispatcher,
        IKsImageResolver images,
        IKsViewMaterializer views)
        where T : class, IKsSettingsGateway
    {
        ArgumentNullException.ThrowIfNull(gatewayFactory);

        if (_controller.Gateway is not { } connected)
        {
            connected = gatewayFactory();
            _controller.Connect(connected, dispatcher);
        }

        _controller.AttachInteractions();
        _controller.AttachImages(images);
        _controller.AttachViews(views);
        return (T)connected;
    }

    /// <summary>
    /// Native Host と同じ寿命を持つ表示内容を、現在の所有値で適用し直す。
    /// </summary>
    /// <remarks>
    /// root の accessory は Native Host 単位のプロパティであり、Host を作り直すと失われる。
    /// accessory と Cell の内容の View の実体も Host と同じ寿命を持つ。Host が view 階層へ
    /// 取り付けられた後にここを通す。
    /// </remarks>
    internal void ApplyHostViews() => _controller.ApplyHostViews();

    /// <summary>Native Host だけを解放する。設定ツリーの状態と購読は維持される。</summary>
    internal void ReleaseHost() => _controller.ReleaseHost();

    /// <summary>現在の既定スタイルを写し取って表示へ反映する。</summary>
    /// <remarks>未接続の間は写しを持つだけで、接続時にまとめて適用される。</remarks>
    private void ApplyTheme() => _controller.SetTheme(CreateThemeSnapshot());

    /// <summary>
    /// 既定スタイルを interop 境界へ運ぶ形へ写し取る。
    /// </summary>
    /// <remarks>
    /// フォントは family / size / attributes の組を 1 つの記述子へ合成する。ヘッダ・フッタ・
    /// Cell タイトルのサイズは、記述子とは別に最終サイズを上書きする値としても運ぶ。
    /// </remarks>
    private KsThemeSnapshot CreateThemeSnapshot() => new()
    {
        SeparatorColor = KsWireValues.Color(SeparatorColor),
        BackgroundColor = KsWireValues.Color(BackgroundColor),
        CellBackgroundColor = KsWireValues.Color(CellBackgroundColor),
        SelectedColor = KsWireValues.Color(SelectedColor),
        CellAccentColor = KsWireValues.Color(CellAccentColor),
        DisabledTextColor = KsWireValues.Color(DisabledTextColor),
        ScrollIndicatorVisible = ScrollIndicatorVisible,
        RowHeight = RowHeight,
        HasUnevenRows = HasUnevenRows,
        HeaderTextColor = KsWireValues.Color(HeaderTextColor),
        HeaderBackgroundColor = KsWireValues.Color(HeaderBackgroundColor),
        HeaderFontSize = HeaderFontSize,
        HeaderFont = KsWireValues.Font(HeaderFontFamily, HeaderFontSize, HeaderFontAttributes),
        HeaderHeight = HeaderHeight,
        FooterTextColor = KsWireValues.Color(FooterTextColor),
        FooterBackgroundColor = KsWireValues.Color(FooterBackgroundColor),
        FooterFontSize = FooterFontSize,
        FooterFont = KsWireValues.Font(FooterFontFamily, FooterFontSize, FooterFontAttributes),
        CellTitleColor = KsWireValues.Color(CellTitleColor),
        CellTitleFontSize = CellTitleFontSize,
        CellTitleFont = KsWireValues.Font(
            CellTitleFontFamily,
            CellTitleFontSize,
            CellTitleFontAttributes),
        CellValueTextColor = KsWireValues.Color(CellValueTextColor),
        CellValueTextFont = KsWireValues.Font(
            CellValueTextFontFamily,
            CellValueTextFontSize,
            CellValueTextFontAttributes),
        CellDescriptionColor = KsWireValues.Color(CellDescriptionColor),
        CellDescriptionFont = KsWireValues.Font(
            CellDescriptionFontFamily,
            CellDescriptionFontSize,
            CellDescriptionFontAttributes),
        CellHintTextColor = KsWireValues.Color(CellHintTextColor),
        CellHintFont = KsWireValues.Font(
            CellHintFontFamily,
            CellHintFontSize,
            CellHintFontAttributes),
        CellPlaceholderColor = KsWireValues.Color(CellPlaceholderColor),
        CellIconSize = CellIconSize,
        CellIconRadius = CellIconRadius,
        SectionMarginTop = KsWireValues.MarginTop(SectionMargin),
        SectionMarginLeading = KsWireValues.MarginLeading(SectionMargin),
        SectionMarginBottom = KsWireValues.MarginBottom(SectionMargin),
        SectionMarginTrailing = KsWireValues.MarginTrailing(SectionMargin),
        SectionCornerRadius = SectionCornerRadius,
        SectionBorderWidth = SectionBorderWidth,
        SectionBorderColor = KsWireValues.Color(SectionBorderColor),
    };
}
