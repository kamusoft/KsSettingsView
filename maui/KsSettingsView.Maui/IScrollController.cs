namespace KsSettingsView;

/// <summary>
/// <see cref="SettingsView"/> へスクロール命令を送るハンドル。
/// </summary>
/// <remarks>
/// 実体は <see cref="SettingsView"/> が 1 つずつ作り、<see cref="SettingsView.ScrollController"/>
/// から ViewModel へ渡す (既定の向きは OneWayToSource)。ViewModel はこの型でハンドルを持つため、
/// テストでは命令を記録するだけの実装に差し替えられる。
/// <list type="bullet">
/// <item>命令は、同じ処理の中で行った設定ツリーの変更が表示に反映された後に実行される。
/// 項目を追加した直後に <see cref="ScrollToEnd"/> を呼べば、追加した項目を含む末尾へ届く。</item>
/// <item>続けて出した命令は呼んだ順に処理され、最終位置は最後の命令のものになる。</item>
/// <item>画面がまだ表示されていない間の命令は何もしない。表示された時点を知るには
/// <see cref="SettingsView.ScrollControllerReadyCommand"/> を使う。</item>
/// <item>対象が見つからない命令・非表示の要素への命令は何もしない。</item>
/// </list>
/// 命令は UI スレッドから呼ぶ。
/// </remarks>
public interface IScrollController
{
    /// <summary>指定した Cell の行へスクロールする。</summary>
    /// <remarks>
    /// <paramref name="target"/> は、<see cref="CellBase.CellId"/> が等しい Cell を先に探し、
    /// 無ければ <see cref="Section.ItemsSource"/> の項目のうち等しいものから生成した Cell を探す。
    /// どちらも表示順で最初に見つかったものを対象にする。
    /// </remarks>
    /// <param name="target">Cell の明示 ID、または Cell を生成した ItemsSource の項目</param>
    /// <param name="position">行を表示範囲のどこへ合わせるか</param>
    /// <param name="animated">アニメーションするか</param>
    void ScrollTo(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true);

    /// <summary>指定した Section へ、見出しごと見えるようにスクロールする。</summary>
    /// <remarks>
    /// <paramref name="target"/> は、<see cref="Section.SectionId"/> が等しい Section を先に探し、
    /// 無ければ <see cref="SettingsView.ItemsSource"/> の項目のうち等しいものから生成した Section を
    /// 探す。どちらも表示順で最初に見つかったものを対象にする。
    /// </remarks>
    /// <param name="target">Section の明示 ID、または Section を生成した ItemsSource の項目</param>
    /// <param name="position">Section の範囲 (見出し・Cell・フッター) を表示範囲のどこへ合わせるか</param>
    /// <param name="animated">アニメーションするか</param>
    void ScrollToSection(object target, ScrollPosition position = ScrollPosition.Start, bool animated = true);

    /// <summary>内容の最上端 (root の header を含む) へスクロールする。</summary>
    /// <param name="animated">アニメーションするか</param>
    void ScrollToStart(bool animated = true);

    /// <summary>内容の最下端 (root の footer を含む) へスクロールする。</summary>
    /// <param name="animated">アニメーションするか</param>
    void ScrollToEnd(bool animated = true);
}
