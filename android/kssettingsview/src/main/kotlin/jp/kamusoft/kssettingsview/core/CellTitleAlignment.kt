package jp.kamusoft.kssettingsview.core

/**
 * タイトルの水平方向の揃え位置。
 *
 * `ButtonCell.titleAlignment` などで使用する。プラットフォーム非依存の論理表現として
 * `START` / `CENTER` / `END` の 3 ケースを提供する。UI 層側で `Gravity.START` / `Gravity.END` 等に
 * 変換する（RTL 環境では Android プラットフォームの差し替えに委ねる）。
 */
public enum class CellTitleAlignment {
    /** 先頭寄せ（LTR では左寄せ、RTL では右寄せ） */
    START,

    /** 中央寄せ */
    CENTER,

    /** 末尾寄せ（LTR では右寄せ、RTL では左寄せ） */
    END,
}
