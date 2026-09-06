using Microsoft.Maui.Controls;

namespace KsSettingsView.Sample.Maui;

/// <summary>Sample アプリのアプリケーション。</summary>
/// <remarks>
/// 入口はデモ一覧ページで、各画面へは <see cref="NavigationPage"/> の push / pop で行き来する。
/// </remarks>
public class App : Application
{
    // ウィンドウはアプリのプロセスの寿命の間 1 つだけ作り、Activity が作り直されるたびに同じものを返す。
    // MAUI の Window は自分に載っている Page の Handler 変更イベントを購読し、その解除は Page が
    // 差し替わったときにしか行わない (破棄時には行わない)。そのため再生成のたびに新しい Window へ同じ
    // ページを載せ直すと、破棄済みの Window がページを購読したまま積み上がる。Window ごと再利用すれば
    // 購読は 1 本のまま保たれ、MAUI 自身の CreateWindow の既定 (作成済みのウィンドウがあればそれを返す)
    // と同じ形になる。
    //
    // 引き継がれるのは外観の切り替えによる再生成に限らない。ページと入力状態は、プロセスが生きたまま
    // Activity だけが作り直される経路すべて (システムによる破棄からの復帰など) で保たれる。
    private Window? _window;

    /// <summary>アプリを作り、保存済みの外観の選択を反映する。</summary>
    public App() => SampleAppearanceStore.Apply(SampleAppearanceStore.Load());

    /// <inheritdoc/>
    protected override Window CreateWindow(IActivationState? activationState)
        => _window ??= new Window(CreateNavigation());

    private static NavigationPage CreateNavigation()
    {
        var navigation = new NavigationPage(new MenuPage())
        {
            // バー配色は Android テーマへのフォールバックに委ねず MAUI 層で明示する。
            // テンプレート既定テーマ (Maui.MainTheme) のフォールバックは暗色地に黒文字となり判読できない。
            BarBackgroundColor = Color.FromArgb("#2C3E50"),
            BarTextColor = Colors.White,
        };
        // iOS ネイティブサンプルのナビゲーションバー表示 (Large Title) と揃える (cross/ADR-0016)
        Microsoft.Maui.Controls.PlatformConfiguration.iOSSpecific.NavigationPage
            .SetPrefersLargeTitles(navigation, true);
        return navigation;
    }
}
