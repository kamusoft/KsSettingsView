package jp.kamusoft.kssettingsview.ui

/**
 * 設定画面へスクロール命令を送るハンドルの interface です。
 *
 * [KsScrollController] が実装します。ViewModel がこの型で命令ハンドルを持てば、テストで命令を
 * 記録するだけの実装に差し替えられます。
 *
 * 命令はメインスレッドから呼びます。
 */
public interface KsScrollControlling {

    /**
     * 指定した ID の Cell の行へスクロールします。
     *
     * @param id Cell の ID。View の `KsSettingsView` と Store を使う画面では Cell の `id`、宣言的に
     *   書いた画面では `cellID(...)` で付けた ID か `forEach` の key
     * @param position 行を表示範囲のどこへ合わせるか
     * @param animated アニメーションするか
     */
    public fun scrollTo(
        id: Any,
        position: KsScrollPosition = KsScrollPosition.Start,
        animated: Boolean = true,
    )

    /**
     * 指定した ID の Section へ、見出しごと見えるようにスクロールします。
     *
     * @param id Section の ID。View の `KsSettingsView` と Store を使う画面では Section の `id`、
     *   宣言的に書いた画面では `sectionID(...)` で付けた ID か `forEach` の key
     * @param position Section の範囲 (見出し・Cell・Footer) を表示範囲のどこへ合わせるか
     * @param animated アニメーションするか
     */
    public fun scrollToSection(
        id: Any,
        position: KsScrollPosition = KsScrollPosition.Start,
        animated: Boolean = true,
    )

    /**
     * 内容の最上端 (Root Header を含む) へスクロールします。
     *
     * @param animated アニメーションするか
     */
    public fun scrollToStart(animated: Boolean = true)

    /**
     * 内容の最下端 (Root Footer を含む) へスクロールします。
     *
     * @param animated アニメーションするか
     */
    public fun scrollToEnd(animated: Boolean = true)
}
