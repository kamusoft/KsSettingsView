package jp.kamusoft.kssettingsview.core

/**
 * 設定画面全体の状態を表すルート値型。
 *
 * 複数の [Section] のみを保持し、Theme は持たない。スタイルは UI 層の責務であり
 * （core/ADR-0009）、`KsSettingsView(theme = ...)` / `SettingsRootStore.applyTheme(_)` の
 * 引数経路で渡す。
 *
 * # 等価性契約
 *
 * `data class` の自動 `equals` / `hashCode` を採用し、`sections` のみで等価性が決定される。
 *
 * @property sections セクション群
 */
public data class SettingsRoot(
    val sections: List<Section> = emptyList(),
)
