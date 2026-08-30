using Foundation;
using KsSettingsView.Bridge;
using UIKit;

namespace KsSettingsView.IntegrationHost;

/// <summary>
/// Bridge が生成した Native Host を root view controller として表示するアプリ delegate。
/// </summary>
/// <remarks>
/// 「Host 生成 → 操作 → view 階層へ取り付け」の順序で組み立て、取り付け前の操作が view load 時に
/// Store の現在状態から復元されることを確認する (core/ADR-0019)。ナビゲーションバーの
/// 「解放 → 再生成」ボタンは Host だけを解放し、解放中に Store を更新してから Host を作り直す
/// 経路 (maui/ADR-0007) を通す。
/// </remarks>
[Register("AppDelegate")]
public sealed class AppDelegate : UIApplicationDelegate
{
    private KsSettingsBridge? _bridge;
    private KsBridgeScenarioHandles? _handles;
    private UINavigationController? _navigation;

    /// <inheritdoc/>
    public override UIWindow? Window { get; set; }

    /// <inheritdoc/>
    public override bool FinishedLaunching(UIApplication application, NSDictionary? launchOptions)
    {
        var bridge = new KsSettingsBridge();
        _bridge = bridge;

        var host = bridge.MakeHostViewController();
        if (host is null)
        {
            throw new InvalidOperationException("Native Host を生成できませんでした。");
        }

        var navigation = new UINavigationController(Decorate(host));
        _navigation = navigation;

        // Host を取り付ける前に操作する。view 構築前に届いた更新は個々には適用されないが、
        // view load 時に Store の現在状態から復元される。
        _handles = KsBridgeScenario.Apply(bridge);

        // Info.plist に scene manifest を持たない単一 window のアプリなので、
        // UIWindowScene ではなく画面全体を占める window を直接生成する。
#pragma warning disable CA1422
        Window = new UIWindow(UIScreen.MainScreen.Bounds)
        {
            RootViewController = navigation,
        };
#pragma warning restore CA1422
        Window.MakeKeyAndVisible();

        // Root の header / footer は復元の対象外なので、所有者として自分で適用する。
        // 反映先は view の構築後にしか存在しないため、window 表示だけでは足りず、
        // ここで view の読み込みを確定させてから適用する。
        host.LoadViewIfNeeded();
        KsBridgeScenario.ApplyRootAccessory(bridge);
        return true;
    }

    /// <inheritdoc/>
    public override void WillTerminate(UIApplication application)
    {
        if (_bridge is { } bridge)
        {
            KsBridgeScenario.Shutdown(bridge);
            _bridge = null;
        }
    }

    /// <summary>
    /// Host だけを解放し、解放中に Store を更新してから Host を作り直して画面へ取り付ける。
    /// </summary>
    /// <remarks>
    /// 旧 Host は解放時点で Store の購読を切られているため、続く更新は表示に届かない。
    /// 新しい Host は接続時点の Store 現在状態から表示を復元するので、画面には解放中に適用した
    /// 内容が現れる。旧 Host の view 階層からの取り外しは、ここで新しい Host へ差し替えることで行う。
    /// </remarks>
    private void ReleaseAndRecreateHost()
    {
        if (_bridge is not { } bridge || _handles is not { } handles || _navigation is not { } navigation)
        {
            return;
        }

        bridge.ReleaseHost();
        KsBridgeScenario.ApplyWhileReleased(bridge, handles);

        var host = bridge.MakeHostViewController();
        if (host is null)
        {
            throw new InvalidOperationException("解放後の Native Host を再生成できませんでした。");
        }

        navigation.SetViewControllers([Decorate(host)], animated: false);
    }

    /// <summary>Host にタイトルと「解放 → 再生成」ボタンを付ける。</summary>
    /// <param name="host">Bridge が生成した Native Host</param>
    /// <returns>受け取った Host そのもの</returns>
    private UIViewController Decorate(UIViewController host)
    {
        host.Title = "KsSettingsView Bridge";
        host.NavigationItem.RightBarButtonItem = new UIBarButtonItem(
            "解放 → 再生成",
            UIBarButtonItemStyle.Plain,
            (_, _) => ReleaseAndRecreateHost());
        return host;
    }
}
