using System;
using NUnit.Framework;

namespace KsSettingsView.Tests.Support;

/// <summary>
/// 参照が GC で回収されることを確かめる検証ヘルパ。
/// </summary>
/// <remarks>
/// 回収の判定は即時には決まらないため、回収されるまで GC を繰り返し、規定回数で
/// 回収されなければ失敗させる。検証対象への強参照はテスト側で確実に手放してから呼ぶ。
/// </remarks>
internal static class GcProbe
{
    private const int MaxAttempts = 10;

    /// <summary>参照先が回収されることを確かめる。</summary>
    /// <param name="reference">検証する弱参照</param>
    /// <param name="subject">失敗時に示す対象の名前</param>
    public static void AssertCollected(WeakReference reference, string subject)
    {
        ArgumentNullException.ThrowIfNull(reference);

        for (int attempt = 0; attempt < MaxAttempts; attempt++)
        {
            GC.Collect();
            GC.WaitForPendingFinalizers();
            GC.Collect();

            if (!reference.IsAlive)
            {
                return;
            }
        }

        Assert.Fail($"{subject} が回収されず、強参照が残っている。");
    }
}
