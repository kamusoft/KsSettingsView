package jp.kamusoft.kssettingsview.ui

/**
 * 命令ハンドル ([KsScrollController]) から受け口へ渡すスクロール命令。
 *
 * ID は利用者が渡した値をそのまま運ぶ。何の ID として解釈するか (Store の Cell / Section の ID か、
 * 宣言 UI の明示 ID / `forEach` の key か) は受け口が決める。
 */
internal sealed class KsScrollCommand {

    /** 命令をアニメーションするか。 */
    abstract val animated: Boolean

    /** Cell の行へ送る。 */
    data class ToCell(
        val id: Any,
        val position: KsScrollPosition,
        override val animated: Boolean,
    ) : KsScrollCommand()

    /** Section の範囲 (見出し・Cell・Footer) へ送る。 */
    data class ToSection(
        val id: Any,
        val position: KsScrollPosition,
        override val animated: Boolean,
    ) : KsScrollCommand()

    /** 内容の最上端 (Root Header を含む) へ送る。 */
    data class ToStart(override val animated: Boolean) : KsScrollCommand()

    /** 内容の最下端 (Root Footer を含む) へ送る。 */
    data class ToEnd(override val animated: Boolean) : KsScrollCommand()
}

/**
 * 命令ハンドル ([KsScrollController]) が命令を届ける受け口。
 *
 * View Host ([KsSettingsView]) と、宣言 UI の ID を引き直す Compose 側の受け口が実装する。
 * ハンドルは受け口を弱参照で持つため、受け口の寿命はハンドルに延ばされない (core/ADR-0037)。
 */
internal interface KsScrollCommandReceiver {
    /** 命令を受け取る。受けた処理の中では実行せず、受け口の待ち行列に積む。メインスレッドで呼ばれる。 */
    fun receive(command: KsScrollCommand)
}
