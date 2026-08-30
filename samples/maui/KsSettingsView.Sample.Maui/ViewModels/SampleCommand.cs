using System;
using System.Windows.Input;

namespace KsSettingsView.Sample.Maui.ViewModels;

/// <summary>
/// デモが Cell の Command へ渡す最小の <see cref="ICommand"/>。
/// </summary>
/// <param name="execute">実行時に呼ぶ処理。引数は CommandParameter</param>
/// <param name="canExecute">実行できるかどうかを答える処理。省略すると常に実行できる</param>
public sealed class SampleCommand(Action<object?> execute, Func<object?, bool>? canExecute = null) : ICommand
{
    /// <inheritdoc/>
    public event EventHandler? CanExecuteChanged;

    /// <inheritdoc/>
    public bool CanExecute(object? parameter) => canExecute is null || canExecute(parameter);

    /// <inheritdoc/>
    public void Execute(object? parameter) => execute(parameter);

    /// <summary>実行できるかどうかが変わったことを知らせる。</summary>
    public void RaiseCanExecuteChanged() => CanExecuteChanged?.Invoke(this, EventArgs.Empty);
}
