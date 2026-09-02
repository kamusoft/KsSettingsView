namespace KsSettingsView.Internals;

/// <summary>
/// プロパティの変更通知を弱参照購読から受け取る観測者。
/// </summary>
internal interface IKsPropertyObserver
{
    /// <summary>購読中のオブジェクトのプロパティが変更されたときに呼ばれる。</summary>
    /// <param name="sender">変更されたオブジェクト</param>
    /// <param name="propertyName">変更されたプロパティの名前</param>
    void OnObservedPropertyChanged(object sender, string? propertyName);
}
