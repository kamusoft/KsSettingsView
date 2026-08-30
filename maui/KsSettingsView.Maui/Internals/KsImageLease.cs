using System;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 解決済みの platform 画像と、その画像を有効に保つ後片付けの口を一体で持つ器。
/// </summary>
/// <remarks>
/// 画像解決サービスは結果オブジェクトの破棄に後片付け (ストリームや platform 資源の解放) を
/// 紐づけるため、画像だけを取り出して結果を捨てると後片付けが行われない。逆に受け取った直後に
/// 破棄すると画像の寿命が表示より先に尽きる。そこで結果と画像を一体で持ち、その画像を使わなく
/// なった時点 (差し替え・登録解除・追い抜かれた解決の破棄・解決口の切り替え) で破棄する。
/// </remarks>
/// <param name="image">解決できた platform 画像</param>
/// <param name="handle">画像を有効に保つ後片付けの口。持たない場合は null</param>
internal sealed class KsImageLease(object image, IDisposable? handle) : IDisposable
{
    private bool _disposed;

    /// <summary>解決できた platform 画像。</summary>
    public object Image { get; } = image;

    /// <summary>画像を手放し、後片付けを実行する。二度目以降の呼び出しは何もしない。</summary>
    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        handle?.Dispose();
    }
}
