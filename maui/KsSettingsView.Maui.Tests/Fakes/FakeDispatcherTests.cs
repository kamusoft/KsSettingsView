using NUnit.Framework;

namespace KsSettingsView.Tests.Fakes;

/// <summary>テスト用 dispatcher がバッチ境界を作れることを確認する。</summary>
[TestFixture]
public class FakeDispatcherTests
{
    /// <summary>予約した処理は RunPending を呼ぶまで実行されない。</summary>
    [Test]
    public void DispatchDoesNotRunUntilRunPending()
    {
        FakeDispatcher dispatcher = new();
        int executed = 0;

        dispatcher.Dispatch(() => executed++);

        Assert.That(executed, Is.Zero);
        Assert.That(dispatcher.PendingCount, Is.EqualTo(1));

        Assert.That(dispatcher.RunPending(), Is.EqualTo(1));
        Assert.That(executed, Is.EqualTo(1));
        Assert.That(dispatcher.PendingCount, Is.Zero);
    }

    /// <summary>実行中に積まれた処理は次の RunPending へ回る。</summary>
    [Test]
    public void ReentrantDispatchIsDeferredToNextRun()
    {
        FakeDispatcher dispatcher = new();
        int executed = 0;

        dispatcher.Dispatch(() =>
        {
            executed++;
            dispatcher.Dispatch(() => executed++);
        });

        Assert.That(dispatcher.RunPending(), Is.EqualTo(1));
        Assert.That(executed, Is.EqualTo(1));
        Assert.That(dispatcher.PendingCount, Is.EqualTo(1));

        Assert.That(dispatcher.RunPending(), Is.EqualTo(1));
        Assert.That(executed, Is.EqualTo(2));
    }
}
