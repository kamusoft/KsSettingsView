using System;
using System.Windows.Input;
using KsSettingsView.Maui.Internals;
using Microsoft.Maui;
using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui;

/// <summary>
/// 明示的なボタン操作を表す Cell。
/// </summary>
/// <remarks>
/// 説明文を持たない Cell のため、基底の <see cref="CellBase.Description"/> は自型から公開せず、
/// 基底経由で設定されても輸送・表示しない。Disclosure Indicator も表示しない。
/// 行の実効有効状態は <see cref="CellBase.IsEnabled"/> と <see cref="Command"/> の実行可否の
/// 両方で決まり、タップは <see cref="Tapped"/> の発火に続けて <see cref="Command"/> の実行という
/// 順序で通知される。
/// </remarks>
public class ButtonCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(ButtonCell),
        default(string));

    /// <summary><see cref="TitleAlignment"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty TitleAlignmentProperty = BindableProperty.Create(
        nameof(TitleAlignment),
        typeof(TextAlignment?),
        typeof(ButtonCell),
        default(TextAlignment?));

    /// <summary><see cref="Command"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandProperty = BindableProperty.Create(
        nameof(Command),
        typeof(ICommand),
        typeof(ButtonCell),
        default(ICommand),
        propertyChanged: static (bindable, _, newValue) =>
            ((ButtonCell)bindable)._tapCommand.SetCommand(newValue as ICommand));

    /// <summary><see cref="CommandParameter"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandParameterProperty = BindableProperty.Create(
        nameof(CommandParameter),
        typeof(object),
        typeof(ButtonCell),
        default(object),
        propertyChanged: static (bindable, _, _) =>
            ((ButtonCell)bindable).NotifyEffectiveEnabledChanged());

    private readonly KsTapCommand _tapCommand;

    /// <summary>タップされていない ButtonCell を作る。</summary>
    public ButtonCell() => _tapCommand = new KsTapCommand(NotifyEffectiveEnabledChanged);

    /// <summary>行がタップされたときに発火する。</summary>
    /// <remarks>実効無効の行では発火しない。</remarks>
    public event EventHandler? Tapped;

    /// <summary>行の右側に表示する値文字列。null で非表示。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>タイトルの揃え位置。null で Native 既定 (中央寄せ)。</summary>
    public TextAlignment? TitleAlignment
    {
        get => (TextAlignment?)GetValue(TitleAlignmentProperty);
        set => SetValue(TitleAlignmentProperty, value);
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

    /// <summary>この Cell は説明文を持たないため、基底の説明文を自型から公開しない。</summary>
    private new string? Description => base.Description;

    /// <summary>行が実際に操作できるかどうか。</summary>
    internal bool IsEffectivelyEnabled => IsEnabled && _tapCommand.CanExecute(CommandParameter);

    /// <summary>Native からのタップ通知を受けて、この行のタップ通知経路を通す。</summary>
    /// <remarks>実効無効の行では何も起こらない。</remarks>
    internal void NotifyTapped()
    {
        if (!IsEffectivelyEnabled)
        {
            return;
        }

        Tapped?.Invoke(this, EventArgs.Empty);
        _tapCommand.Execute(CommandParameter);
    }

    /// <inheritdoc/>
    internal override KsCellSnapshot CreateSnapshot()
        => CreateSnapshot<KsButtonCellSnapshot>() with
        {
            Description = null,
            IsEnabled = IsEffectivelyEnabled,
            ValueText = ValueText,
            TitleAlignment = TitleAlignment,
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(TitleAlignment) or nameof(Command) or nameof(CommandParameter)
        || base.AffectsSnapshot(propertyName);

    /// <summary>実効有効状態の変化を、行の有効状態の変更として知らせる。</summary>
    private void NotifyEffectiveEnabledChanged() => OnPropertyChanged(nameof(IsEnabled));
}
