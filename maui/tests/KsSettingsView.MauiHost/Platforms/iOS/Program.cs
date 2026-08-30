using UIKit;

namespace KsSettingsView.MauiHost;

/// <summary>iOS の起動口。</summary>
public static class Program
{
    /// <summary>アプリを起動する。</summary>
    /// <param name="args">起動引数</param>
    public static void Main(string[] args) => UIApplication.Main(args, null, typeof(AppDelegate));
}
