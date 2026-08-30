using System;
using Android.App;
using Android.Runtime;
using Microsoft.Maui;
using Microsoft.Maui.Hosting;

namespace KsSettingsView.Sample.Maui;

/// <summary>Android のアプリケーション。</summary>
/// <param name="handle">ランタイムが渡すハンドル</param>
/// <param name="ownership">ハンドルの所有権</param>
[Application]
public class MainApplication(IntPtr handle, JniHandleOwnership ownership)
    : MauiApplication(handle, ownership)
{
    /// <inheritdoc/>
    protected override MauiApp CreateMauiApp() => MauiProgram.CreateMauiApp();
}
