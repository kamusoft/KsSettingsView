package jp.kamusoft.kssettingsview.ui

/**
 * `KsSettingsView` の見た目スタイルを表す enum。
 *
 * - [Classic]: `AiForms.Maui.SettingsView` と同じフラットな見た目（区切り線のみ）
 * - [Modern]: 最新 OS 設定画面風の角丸グルーピング（Section 単位の外側マージン・角丸背景）
 *
 * 描画基盤（RecyclerView / ListAdapter）は共通で、`ItemDecoration` のみが切り替わる
 * （[ClassicSectionDecoration] / [ModernSectionDecoration]）。
 */
enum class KsSettingsViewStyle {
    Classic,
    Modern,
}
