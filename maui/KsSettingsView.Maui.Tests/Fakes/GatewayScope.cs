using System.Collections.Generic;
using System.Linq;

namespace KsSettingsView.Tests.Fakes;

/// <summary>
/// fake gateway と fake dispatcher を接続した SettingsView を扱うテスト用の足場。
/// </summary>
/// <remarks>
/// 接続・記録の絞り込み・flush の実行という、変換経路のテストで毎回必要になる操作をまとめる。
/// </remarks>
internal sealed class GatewayScope
{
    private GatewayScope(SettingsView view) => View = view;

    /// <summary>接続対象の SettingsView。</summary>
    public SettingsView View { get; }

    /// <summary>接続した gateway。</summary>
    public FakeSettingsGateway Gateway { get; } = new();

    /// <summary>接続した dispatcher。</summary>
    public FakeDispatcher Dispatcher { get; } = new();

    /// <summary>接続した画像解決の口。再接続で作り直すとインスタンスが入れ替わる。</summary>
    public FakeImageResolver Images { get; private set; } = new();

    /// <summary>接続した View の実体化の口。</summary>
    public FakeViewMaterializer Views { get; } = new();

    /// <summary>記録された呼び出しの並び。</summary>
    public IReadOnlyList<GatewayCall> Calls => Gateway.Calls;

    /// <summary>指定の SettingsView へ fake gateway を接続する。</summary>
    /// <param name="view">接続対象</param>
    public static GatewayScope Connect(SettingsView view)
    {
        GatewayScope scope = new(view);
        scope.Reconnect();
        return scope;
    }

    /// <summary>
    /// 指定の SettingsView へ fake gateway を接続し、Native Host を作る手前で止める。
    /// </summary>
    /// <remarks>Host の生成そのものを Handler に行わせるテストで使う。</remarks>
    /// <param name="view">接続対象</param>
    public static GatewayScope ConnectWithoutHost(SettingsView view)
    {
        GatewayScope scope = new(view);
        scope.ConnectFacade();
        return scope;
    }

    /// <summary>
    /// Native Host を作る手順として、口を差し込み直してから root の accessory を適用する。
    /// </summary>
    /// <remarks>
    /// Handler が platform view を作る一連の流れ (facade の接続 → Native Host の生成 → root の
    /// accessory の適用) をまとめて再現する。ここまでで、置かれている View は表示へ届いている。
    /// </remarks>
    /// <param name="renewImages">
    /// 画像解決の口を作り直すかどうか。Host 世代ごとに解決口が作り直される実装を再現する
    /// </param>
    public void Reconnect(bool renewImages = false)
    {
        ConnectFacade(renewImages);
        CreateHost();
    }

    /// <summary>
    /// Native Host を作る直前までを再現する。
    /// </summary>
    /// <remarks>
    /// この呼び出しから戻った時点が「Native Host の生成時点」であり、Host はここまでに届いた
    /// 設定ツリーの現在状態から表示を復元する。
    /// </remarks>
    /// <param name="renewImages">画像解決の口を作り直すかどうか</param>
    public void ConnectFacade(bool renewImages = false)
    {
        if (renewImages)
        {
            Images = new FakeImageResolver();
        }

        View.ConnectGateway(() => Gateway, Dispatcher, Images, Views);
    }

    /// <summary>Native Host が出来た直後の手順として、root の accessory を適用する。</summary>
    public void CreateHost() => View.ApplyRootAccessories();

    /// <summary>ここまでの記録を捨て、以後の呼び出しだけを見る。</summary>
    public GatewayScope Reset()
    {
        Gateway.ClearCalls();
        return this;
    }

    /// <summary>予約済みの flush を実行する。この時点がバッチの境界になる。</summary>
    public void Flush() => Dispatcher.RunPending();

    /// <summary>指定の種類の呼び出しがちょうど 1 件記録されていることを前提に取り出す。</summary>
    /// <typeparam name="T">呼び出しの種類</typeparam>
    public T Single<T>()
        where T : GatewayCall => Calls.OfType<T>().Single();

    /// <summary>指定の種類の呼び出しを記録順に取り出す。</summary>
    /// <typeparam name="T">呼び出しの種類</typeparam>
    public IReadOnlyList<T> All<T>()
        where T : GatewayCall => [.. Calls.OfType<T>()];
}
