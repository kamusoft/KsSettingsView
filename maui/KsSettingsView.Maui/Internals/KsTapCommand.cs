using System;
using System.Windows.Input;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// タップで実行する Command と、その実行可否の追随をまとめて受け持つ。
/// </summary>
/// <remarks>
/// Command は差し替えられるため、購読は常に現在の Command 1 つだけに張る。差し替え・解除の後は
/// 旧 Command からの実行可否の通知が届いても無視される。実行可否が変わったときは
/// <paramref name="canExecuteChanged"/> で持ち主へ知らせる。
/// </remarks>
/// <param name="canExecuteChanged">実行可否が変わったときに呼ぶ処理</param>
internal sealed class KsTapCommand(Action canExecuteChanged) : IKsCommandObserver
{
    private readonly Action _canExecuteChanged = canExecuteChanged;
    private KsWeakCommandSubscription? _subscription;
    private ICommand? _command;

    /// <summary>観測対象の Command を差し替える。</summary>
    /// <param name="command">新しい Command。null で解除</param>
    public void SetCommand(ICommand? command)
    {
        _subscription?.Unsubscribe();
        _subscription = null;
        _command = command;

        if (command is not null)
        {
            KsWeakCommandSubscription subscription = new(this);
            subscription.Subscribe(command);
            _subscription = subscription;
        }

        _canExecuteChanged();
    }

    /// <summary>現在の Command が実行できるかどうかを返す。Command 未設定なら true。</summary>
    /// <param name="parameter">実行時に渡すパラメータ</param>
    public bool CanExecute(object? parameter) => _command?.CanExecute(parameter) ?? true;

    /// <summary>現在の Command を実行する。Command 未設定なら何もしない。</summary>
    /// <param name="parameter">実行時に渡すパラメータ</param>
    public void Execute(object? parameter)
    {
        if (_command is { } command && command.CanExecute(parameter))
        {
            command.Execute(parameter);
        }
    }

    /// <inheritdoc/>
    public void OnCommandCanExecuteChanged() => _canExecuteChanged();
}
