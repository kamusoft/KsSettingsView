using System;
using System.Collections.Generic;
using KsSettingsView.Maui.Internals;

namespace KsSettingsView.Maui.Tests.Fakes;

/// <summary>
/// 予約された処理を任意の時点で実行できるテスト用の dispatcher。
/// </summary>
/// <remarks>
/// <see cref="Dispatch"/> は処理を積むだけで実行しない。<see cref="RunPending"/> を呼んだ時点が
/// バッチの境界になる。実行中に積まれた処理は次の <see cref="RunPending"/> へ回すため、
/// flush が自分自身を予約し直しても無限には回らない。
/// </remarks>
internal sealed class FakeDispatcher : IKsDispatcher
{
    private readonly Queue<Action> _pending = new();

    /// <summary>まだ実行していない予約の件数。</summary>
    public int PendingCount => _pending.Count;

    /// <inheritdoc/>
    public bool Dispatch(Action action)
    {
        ArgumentNullException.ThrowIfNull(action);
        _pending.Enqueue(action);
        return true;
    }

    /// <summary>この時点で予約済みの処理をすべて実行する。</summary>
    /// <returns>実行した件数</returns>
    public int RunPending()
    {
        int count = _pending.Count;
        for (int i = 0; i < count; i++)
        {
            _pending.Dequeue().Invoke();
        }

        return count;
    }
}
