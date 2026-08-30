using Android.App;
using Android.OS;
using Android.Views;
using Android.Widget;
using AndroidX.Activity;
using KsSettingsView.Bridge;

namespace KsSettingsView.IntegrationHost;

/// <summary>
/// Bridge が生成した Native Host を content view として表示する Activity。
/// </summary>
/// <remarks>
/// 基底クラスに ComponentActivity を使う。Host は ViewTree の lifecycle owner と
/// SavedStateRegistry owner を必要とするため、素の Activity では動作しない。
/// ライブラリは Fragment を必要としないので (android/ADR-0020 のホスト前提の撤廃)、
/// この統合ホストも ViewTree owner を供給する最小の基底に合わせる。
///
/// 「Host 生成 → 操作 → view 階層へ取り付け」の順序で組み立て、取り付け前の操作が attach 時に
/// Store の現在状態から復元されることを確認する (core/ADR-0019)。画面上部の「解放 → 再生成」
/// ボタンは Host だけを解放し、解放中に Store を更新してから Host を作り直す経路
/// (maui/ADR-0007) を通す。
/// </remarks>
[Activity(
    Label = "KsBridge Host",
    MainLauncher = true,
    Exported = true)]
public sealed class MainActivity : ComponentActivity
{
    private KsSettingsBridge? _bridge;
    private KsBridgeScenarioHandles? _handles;
    private LinearLayout? _hostContainer;
    private View? _host;

    /// <inheritdoc/>
    protected override void OnCreate(Bundle? savedInstanceState)
    {
        base.OnCreate(savedInstanceState);

        var bridge = new KsSettingsBridge();
        _bridge = bridge;

        var releaseButton = new Button(this) { Text = "解放 → 再生成" };
        releaseButton.Click += (_, _) => ReleaseAndRecreateHost();

        var container = new LinearLayout(this) { Orientation = Orientation.Vertical };
        // SDK 35 の edge-to-edge 既定ではコンテンツがシステムバーの下へ潜るため、
        // ルートにシステムバー inset を padding として適用する (先頭のボタンが操作不能になる)。
        container.SetFitsSystemWindows(true);
        container.AddView(
            releaseButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MatchParent,
                ViewGroup.LayoutParams.WrapContent));
        _hostContainer = container;

        var host = bridge.MakeHostView(this)
            ?? throw new InvalidOperationException("Native Host を生成できませんでした。");

        // Host を取り付ける前に操作する。取り付け前の Host は Store の通知を購読していないが、
        // attach 時に Store の現在状態から復元される。
        _handles = KsBridgeScenario.Apply(bridge);

        AttachHost(host);
        SetContentView(container);

        // Root の header / footer は復元の対象外なので、取り付け後に所有者として適用する。
        // 取り付け前に積んだ Post は attach の時点で実行される。
        host.Post(() => KsBridgeScenario.ApplyRootAccessory(bridge));
    }

    /// <inheritdoc/>
    protected override void OnDestroy()
    {
        if (_bridge is { } bridge)
        {
            KsBridgeScenario.Shutdown(bridge);
            _bridge = null;
        }

        base.OnDestroy();
    }

    /// <summary>
    /// Host だけを解放し、解放中に Store を更新してから Host を作り直して画面へ取り付ける。
    /// </summary>
    /// <remarks>
    /// 旧 Host は解放時点で Store の購読を切られているため、続く更新は表示に届かない。
    /// 新しい Host は取り付け時点の Store 現在状態から表示を復元するので、画面には解放中に適用した
    /// 内容が現れる。旧 Host の view 階層からの取り外しは呼び出し側の責務なので、ここで行う。
    /// </remarks>
    private void ReleaseAndRecreateHost()
    {
        if (_bridge is not { } bridge || _handles is not { } handles || _hostContainer is not { } container)
        {
            return;
        }

        bridge.ReleaseHost();
        KsBridgeScenario.ApplyWhileReleased(bridge, handles);

        if (_host is { } oldHost)
        {
            container.RemoveView(oldHost);
            _host = null;
        }

        var host = bridge.MakeHostView(this)
            ?? throw new InvalidOperationException("解放後の Native Host を再生成できませんでした。");
        AttachHost(host);
    }

    /// <summary>Host を残りの領域いっぱいに広げてコンテナへ追加する。</summary>
    /// <param name="host">Bridge が生成した Native Host</param>
    private void AttachHost(View host)
    {
        _host = host;
        _hostContainer?.AddView(
            host,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MatchParent,
                0,
                1.0f));
    }
}
