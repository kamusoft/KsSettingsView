using System.Collections.Generic;
using System.Linq;

namespace KsSettingsView.Maui.Tests.Fakes;

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

    /// <summary>Native Host の再接続として、通知・画像解決・実体化の口を差し込み直す。</summary>
    /// <param name="renewImages">
    /// 画像解決の口を作り直すかどうか。Host 世代ごとに解決口が作り直される実装を再現する
    /// </param>
    public void Reconnect(bool renewImages = false)
    {
        if (renewImages)
        {
            Images = new FakeImageResolver();
        }

        View.ConnectGateway(() => Gateway, Dispatcher, Images, Views);
    }

    /// <summary>Native Host の取り付けとして、Host と同じ寿命を持つ表示内容を適用する。</summary>
    public void Attach() => View.ApplyHostViews();

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
