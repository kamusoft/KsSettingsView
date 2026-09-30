package jp.kamusoft.kssettingsview.ui

/**
 * スクロール命令で、対象を表示範囲のどこへ合わせるかを表す位置です。
 *
 * 表示範囲は、一覧の内容の余白を除いた、実際に見えている領域です。
 */
public enum class KsScrollPosition {
    /** 対象の上端を表示範囲の上端へ合わせます。 */
    Start,

    /** 対象の中央を表示範囲の中央へ合わせます。 */
    Center,

    /** 対象の下端を表示範囲の下端へ合わせます。 */
    End,
}
