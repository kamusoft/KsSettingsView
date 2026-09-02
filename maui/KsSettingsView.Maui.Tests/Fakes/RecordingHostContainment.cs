using System.Collections.Generic;
using KsSettingsView.Handlers;
using KsSettingsView.Internals;
using Microsoft.Maui;

namespace KsSettingsView.Tests.Fakes;

/// <summary>
/// 親子関係の手順を記録するだけの結び付け。
/// </summary>
/// <remarks>
/// ViewController を持たない TFM でも Handler 共通部が踏む手順の順序を確かめられるよう、
/// 手順の並びと、その時点で外から観測できる Handler / gateway の状態を控える。
/// </remarks>
/// <param name="handler">結び付け先の Handler</param>
/// <param name="gateway">接続済みの gateway</param>
internal sealed class RecordingHostContainment(SettingsViewHandler handler, FakeSettingsGateway gateway)
    : IKsHostContainment
{
    private readonly List<string> _steps = [];

    /// <summary>記録された手順の並び。</summary>
    public IReadOnlyList<string> Steps => _steps;

    /// <summary>登録の時点で Handler が platform view を抱えていたかどうか。</summary>
    public bool HandlerHadPlatformViewOnAdd { get; private set; }

    /// <summary>成立を確定させた時点で Handler が platform view を抱えていたかどうか。</summary>
    public bool HandlerHadPlatformViewOnConfirm { get; private set; }

    /// <summary>成立を確定させた時点までに gateway へ届いていた呼び出しの件数。</summary>
    public int GatewayCallCountOnConfirm { get; private set; }

    /// <summary>解消した時点までに Native Host が解放された回数。</summary>
    public int ReleaseHostCountOnRemove { get; private set; }

    /// <inheritdoc/>
    public void AddToParent()
    {
        HandlerHadPlatformViewOnAdd = HasPlatformView();
        _steps.Add(nameof(AddToParent));
    }

    /// <inheritdoc/>
    public void ConfirmAdded()
    {
        HandlerHadPlatformViewOnConfirm = HasPlatformView();
        GatewayCallCountOnConfirm = gateway.Calls.Count;
        _steps.Add(nameof(ConfirmAdded));
    }

    /// <inheritdoc/>
    public void Remove()
    {
        ReleaseHostCountOnRemove = gateway.ReleaseHostCount;
        _steps.Add(nameof(Remove));
    }

    /// <summary>Handler が platform view を抱えているかどうか。</summary>
    /// <remarks>
    /// 型付きのプロパティは platform view 不在で例外になるため、素の口から確かめる。
    /// </remarks>
    private bool HasPlatformView() => ((IElementHandler)handler).PlatformView is not null;
}
