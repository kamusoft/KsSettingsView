namespace KsSettingsView.Internals;

/// <summary>Command の実行可否の変化を受け取る観測者。</summary>
internal interface IKsCommandObserver
{
    /// <summary>観測中の Command の実行可否が変わった。</summary>
    void OnCommandCanExecuteChanged();
}
