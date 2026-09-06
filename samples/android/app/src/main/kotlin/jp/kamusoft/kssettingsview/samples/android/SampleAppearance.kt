package jp.kamusoft.kssettingsview.samples.android

/**
 * ルートメニューで選ぶアプリ全体の外観。
 *
 * 見出しと項目の文言、選択中の印の読み上げ文言は全 platform で一致させる（cross/ADR-0016）。
 * 文言を画面側に手書きすると表記ゆれが再発するため、定義はここ 1 箇所に閉じる。
 *
 * 対応する定義は samples/ios/KsSettingsViewSample/SampleAppearance.swift と
 * samples/maui/KsSettingsView.Sample.Maui/SampleAppearance.cs。
 *
 * @property title ルートメニューに表示する項目名
 */
enum class SampleAppearance(val title: String) {

    /** 端末の夜間モードに従う。 */
    System("システム"),

    /** 端末の設定に関わらずライト。 */
    Light("ライト"),

    /** 端末の設定に関わらずダーク。 */
    Dark("ダーク"),
    ;

    companion object {

        /** ルートメニューで外観の項目群につける見出し。 */
        const val SECTION_TITLE: String = "外観"

        /** 選択中の項目に付く印の読み上げ文言。行の文言と合わせて読まれる。 */
        const val SELECTED_LABEL: String = "選択中"

        /** 初回起動時の選択。 */
        val DEFAULT: SampleAppearance = System
    }
}
