using UIKit;

namespace KsSettingsView.IntegrationHost;

/// <summary>検証ホストの起動入口。</summary>
public static class Program
{
    private static void Main(string[] args)
    {
        UIApplication.Main(args, null, typeof(AppDelegate));
    }
}
