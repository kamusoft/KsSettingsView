using System;
using System.Windows.Input;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui;

/// <summary>
/// 任意の View を行の内容として表示する Cell。
/// </summary>
/// <remarks>
/// <see cref="Content"/> が content property であり、XAML では CustomCell の直下に View を書く。
/// 行の内容領域は Disclosure Indicator の領域を除く全域で、共通行レイアウトのスロット
/// (タイトル・説明文・アイコン) を持たない。そのため <see cref="CellBase"/> から継承する
/// 次のプロパティは表示に影響しない — <see cref="CellBase.Title"/>、<see cref="CellBase.Description"/>、
/// <see cref="CellBase.HintText"/>、<see cref="CellBase.IconSource"/>、
/// <see cref="CellBase.IconSize"/>、<see cref="CellBase.IconRadius"/>、および
/// テキストに掛かるスタイル項目 (<c>TitleColor</c> / <c>TitleFont*</c> / <c>DescriptionColor</c> /
/// <c>DescriptionFont*</c> / <c>ValueTextColor</c> / <c>ValueTextFont*</c> / <c>HintTextColor</c> /
/// <c>HintFont*</c>)。設定しても例外や警告にはならず、そのまま無視される —
/// 同一のスタイル指定値を CustomCell を含む複数の Cell へまとめて当てられる
/// ようにするため (maui/ADR-0021)。効くのは <see cref="CellBase.IsEnabled"/>、<see cref="CellBase.IsVisible"/>、
/// <see cref="CellBase.BackgroundColor"/>、<see cref="CellBase.Height"/> と、この Cell 固有の
/// プロパティになる。
/// <see cref="Content"/> の内部の変化はバインディングでそのまま表示へ届き、プロパティの再設定は
/// 要らない。表示を別の View へ入れ替えるときだけ <see cref="Content"/> を設定し直す
/// (maui/ADR-0020)。行の高さは <see cref="Content"/> のサイズに追従する。
/// 行の実効有効状態 (<see cref="CellBase.IsEnabled"/> と <see cref="Command"/> の実行可否の連動) と、
/// <see cref="Tapped"/> の発火に続けて <see cref="Command"/> を実行する順序は
/// <see cref="CommandCell"/> と同一。
/// 同じ View インスタンスを複数の <see cref="Content"/> や accessory へ同時に置くことはできない。
/// </remarks>
[ContentProperty(nameof(Content))]
public class CustomCell : CellBase
{
    /// <summary><see cref="Content"/> のバッキングプロパティ。</summary>
    /// <remarks>
    /// この Cell が SettingsView の変換経路に載っている間は、値を確定させる前に多重配置を
    /// 検査する。値の確定はそれまでの内容の論理上の所有を先に解いてしまい、後から見つけた
    /// 多重配置を元へ戻せないため。検査を通った後の論理上の所有は、変更通知を受けた変換経路が
    /// 確定させる。ここでの確定は、まだ載っていない Cell — XAML の構築中や、Section へ入れる
    /// 前の Cell — のための受け皿であり、確定済みなら何も起こらない。受け皿には検査を行う
    /// 相手がいないため、既に他所へ置かれている View は引き取らない (この Cell が変換経路に
    /// 加わった時点で多重配置の例外になる)。
    /// 検査の失敗は validateValue の false 返却ではなく <see cref="InvalidOperationException"/> の
    /// 送出で表す。false を返すと BindableProperty 側が ArgumentException に変換してしまい、
    /// 多重配置が公開契約どおりの例外型で観測できなくなるため (maui/ADR-0022)。
    /// </remarks>
    public static readonly BindableProperty ContentProperty = BindableProperty.Create(
        nameof(Content),
        typeof(View),
        typeof(CustomCell),
        default(View),
        validateValue: static (bindable, value) =>
        {
            CustomCell cell = (CustomCell)bindable;
            cell.ContentGuard?.EnsureContentCanBePlaced(cell, value as View);
            return true;
        },
        propertyChanged: static (bindable, oldValue, newValue) => KsAccessoryViewOwnership.ReassignIfFree(
            (CustomCell)bindable,
            oldValue as View,
            newValue as View));

    /// <summary><see cref="Command"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandProperty = BindableProperty.Create(
        nameof(Command),
        typeof(ICommand),
        typeof(CustomCell),
        default(ICommand),
        propertyChanged: static (bindable, _, newValue) =>
            ((CustomCell)bindable)._tapCommand.SetCommand(newValue as ICommand));

    /// <summary><see cref="CommandParameter"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandParameterProperty = BindableProperty.Create(
        nameof(CommandParameter),
        typeof(object),
        typeof(CustomCell),
        default(object),
        propertyChanged: static (bindable, _, _) =>
            ((CustomCell)bindable).NotifyEffectiveEnabledChanged());

    /// <summary><see cref="ShowArrowIndicator"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ShowArrowIndicatorProperty = BindableProperty.Create(
        nameof(ShowArrowIndicator),
        typeof(bool),
        typeof(CustomCell),
        false);

    private readonly KsTapCommand _tapCommand;

    private EventHandler? _tapped;

    private WeakReference<IKsCellContentGuard>? _contentGuard;

    /// <summary>内容を持たない CustomCell を作る。</summary>
    public CustomCell() => _tapCommand = new KsTapCommand(NotifyEffectiveEnabledChanged);

    /// <summary>行がタップされたときに発火する。</summary>
    /// <remarks>
    /// 実効無効の行では発火しない。<see cref="Content"/> の中の操作可能な要素がタップを
    /// 受け取った場合も発火しない。最初の購読と最後の購読解除は行タップ動作の有無として
    /// 表示へ反映される。
    /// </remarks>
    public event EventHandler? Tapped
    {
        add
        {
            bool had = HasTapHandler;
            _tapped += value;
            NotifyTapHandlingChanged(had);
        }

        remove
        {
            bool had = HasTapHandler;
            _tapped -= value;
            NotifyTapHandlingChanged(had);
        }
    }

    /// <summary>行の内容として表示する View。null で空の内容になる。</summary>
    /// <remarks>
    /// この View は所有する CustomCell の <see cref="BindableObject.BindingContext"/> を継承する
    /// (View 自身に明示的な BindingContext があるときはそちらが優先される)。
    /// </remarks>
    public View? Content
    {
        get => (View?)GetValue(ContentProperty);
        set => SetValue(ContentProperty, value);
    }

    /// <summary>タップで実行する Command。</summary>
    /// <remarks>
    /// 実行可否 (<see cref="ICommand.CanExecute"/>) は行の実効有効状態に含まれ、
    /// <see cref="ICommand.CanExecuteChanged"/> の発火で表示が追随する。
    /// </remarks>
    public ICommand? Command
    {
        get => (ICommand?)GetValue(CommandProperty);
        set => SetValue(CommandProperty, value);
    }

    /// <summary><see cref="Command"/> の実行時に渡すパラメータ。</summary>
    public object? CommandParameter
    {
        get => GetValue(CommandParameterProperty);
        set => SetValue(CommandParameterProperty, value);
    }

    /// <summary>行の末尾に Disclosure Indicator を表示するかどうか。</summary>
    /// <remarks>
    /// 表示すると <see cref="Content"/> の占有領域は indicator の分だけ狭くなる。
    /// <see cref="Command"/> / <see cref="Tapped"/> の指定とは独立に決められる。
    /// </remarks>
    public bool ShowArrowIndicator
    {
        get => (bool)GetValue(ShowArrowIndicatorProperty);
        set => SetValue(ShowArrowIndicatorProperty, value);
    }

    /// <summary>行が実際に操作できるかどうか。</summary>
    internal bool IsEffectivelyEnabled => IsEnabled && _tapCommand.CanExecute(CommandParameter);

    /// <summary>行タップの通知先を持つかどうか。</summary>
    /// <remarks>
    /// 通知先を持たない行はタップ動作そのものを持たず、<see cref="Content"/> の中の操作を妨げない。
    /// </remarks>
    internal bool HasTapHandler => Command is not null || _tapped is not null;

    /// <summary>
    /// 内容として実体化されている View の世代。
    /// </summary>
    /// <remarks>
    /// 変換経路が実体を作り直すたびに新しい値を振る。写しに載るのはこの世代だけで View の実体は
    /// 載らない — 値比較の経路へ View の変更を流さず、実体が入れ替わったときにだけ Native の
    /// 内容が作り直されるようにするため (maui/ADR-0020)。
    /// </remarks>
    internal string ContentToken { get; set; } = string.Empty;

    /// <summary>
    /// 内容に置く View の可否を尋ねる相手。設定ツリーに載っていない間は null。
    /// </summary>
    /// <remarks>
    /// 変換経路がこの Cell を対応表へ載せている間だけ差し込まれる。
    /// 保持は弱参照で行う — 外部 (ViewModel 等) がこの Cell を保持し続けても、尋ね先を
    /// 巻き添えで生かし続けないため (プロパティ変更の購読と同じ規律)。
    /// </remarks>
    internal IKsCellContentGuard? ContentGuard
    {
        get => _contentGuard is not null && _contentGuard.TryGetTarget(out IKsCellContentGuard? guard)
            ? guard
            : null;

        set => _contentGuard = value is null ? null : new WeakReference<IKsCellContentGuard>(value);
    }

    /// <summary>Native からのタップ通知を受けて、この行のタップ通知経路を通す。</summary>
    /// <remarks>実効無効の行では何も起こらない。</remarks>
    internal void NotifyTapped()
    {
        if (!IsEffectivelyEnabled)
        {
            return;
        }

        _tapped?.Invoke(this, EventArgs.Empty);
        _tapCommand.Execute(CommandParameter);
    }

    /// <inheritdoc/>
    /// <remarks>
    /// 共通行レイアウトのスロットを持たないため、タイトル・説明文・ヒント・アイコンと、それらに
    /// 掛かるスタイル項目は写し取らない。写るのは行そのものに掛かる指定と、この Cell 固有の項目。
    /// </remarks>
    internal override KsCellSnapshot CreateSnapshot() => new KsCustomCellSnapshot
    {
        IsEnabled = IsEffectivelyEnabled,
        IsVisible = IsVisible,
        Style = CreateRowStyleSnapshot(),
        ContentToken = ContentToken,
        ShowArrowIndicator = ShowArrowIndicator,
        HasTapHandler = HasTapHandler,
    };

    /// <inheritdoc/>
    /// <remarks>
    /// 写しに載らないプロパティは内容更新の対象にしないため、基底の判定は引き継がない。
    /// <see cref="Content"/> は実体化を挟むため、内容更新とは別の経路で追われる。
    /// </remarks>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(IsEnabled) or nameof(IsVisible) or nameof(BackgroundColor) or nameof(Height)
        or nameof(ShowArrowIndicator) or nameof(Command) or nameof(CommandParameter)
        or nameof(HasTapHandler);

    /// <summary>実効有効状態の変化を、行の有効状態の変更として知らせる。</summary>
    private void NotifyEffectiveEnabledChanged() => OnPropertyChanged(nameof(IsEnabled));

    /// <summary>行タップ動作の有無が変わったときだけ、その変化を知らせる。</summary>
    /// <param name="had">変化前に通知先を持っていたかどうか</param>
    private void NotifyTapHandlingChanged(bool had)
    {
        if (had != HasTapHandler)
        {
            OnPropertyChanged(nameof(HasTapHandler));
        }
    }
}
