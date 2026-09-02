using System;

namespace KsSettingsView.Internals;

/// <summary>
/// 内容更新のバッチ配信を予約する実行口。
/// </summary>
/// <remarks>
/// Cell の内容更新は即時に配信せず、最初の変更でここへ flush を 1 回だけ予約する。
/// 予約した flush が実行されるまでの変更が 1 バッチになる。
/// </remarks>
internal interface IKsDispatcher
{
    /// <summary>UI サイクルの実行キューへ処理を積む。</summary>
    /// <param name="action">実行する処理</param>
    /// <returns>予約できたかどうか</returns>
    bool Dispatch(Action action);
}
