using System;
using System.Windows.Input;
using KsSettingsView.Internals;
using Microsoft.Maui.Controls;

namespace KsSettingsView;

/// <summary>
/// タップで処理を実行する Cell。
/// </summary>
/// <remarks>
/// 行の実効有効状態は <see cref="CellBase.IsEnabled"/> と <see cref="Command"/> の実行可否の
/// 両方で決まり、実効無効の行はタップできない。タップは <see cref="Tapped"/> の発火に続けて
/// <see cref="Command"/> の実行という順序で通知される。
/// </remarks>
public class CommandCell : CellBase
{
    /// <summary><see cref="ValueText"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty ValueTextProperty = BindableProperty.Create(
        nameof(ValueText),
        typeof(string),
        typeof(CommandCell),
        default(string));

    /// <summary><see cref="HideArrow"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty HideArrowProperty = BindableProperty.Create(
        nameof(HideArrow),
        typeof(bool),
        typeof(CommandCell),
        false);

    /// <summary><see cref="Command"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandProperty = BindableProperty.Create(
        nameof(Command),
        typeof(ICommand),
        typeof(CommandCell),
        default(ICommand),
        propertyChanged: static (bindable, _, newValue) =>
            ((CommandCell)bindable)._tapCommand.SetCommand(newValue as ICommand));

    /// <summary><see cref="CommandParameter"/> のバッキングプロパティ。</summary>
    public static readonly BindableProperty CommandParameterProperty = BindableProperty.Create(
        nameof(CommandParameter),
        typeof(object),
        typeof(CommandCell),
        default(object),
        propertyChanged: static (bindable, _, _) =>
            ((CommandCell)bindable).NotifyEffectiveEnabledChanged());

    private readonly KsTapCommand _tapCommand;

    /// <summary>タップされていない CommandCell を作る。</summary>
    public CommandCell() => _tapCommand = new KsTapCommand(NotifyEffectiveEnabledChanged);

    /// <summary>行がタップされたときに発火する。</summary>
    /// <remarks>実効無効の行では発火しない。</remarks>
    public event EventHandler? Tapped;

    /// <summary>行の右側に表示する値文字列。null で非表示。</summary>
    public string? ValueText
    {
        get => (string?)GetValue(ValueTextProperty);
        set => SetValue(ValueTextProperty, value);
    }

    /// <summary>Disclosure Indicator を隠すかどうか。</summary>
    public bool HideArrow
    {
        get => (bool)GetValue(HideArrowProperty);
        set => SetValue(HideArrowProperty, value);
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
        => CreateSnapshot<KsCommandCellSnapshot>() with
        {
            IsEnabled = IsEffectivelyEnabled,
            ValueText = ValueText,
            HideArrow = HideArrow,
        };

    /// <inheritdoc/>
    internal override bool AffectsSnapshot(string? propertyName) => propertyName is
        nameof(ValueText) or nameof(HideArrow) or nameof(Command) or nameof(CommandParameter)
        || base.AffectsSnapshot(propertyName);

    /// <summary>実効有効状態の変化を、行の有効状態の変更として知らせる。</summary>
    private void NotifyEffectiveEnabledChanged() => OnPropertyChanged(nameof(IsEnabled));
}
