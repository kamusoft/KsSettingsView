package jp.kamusoft.kssettingsview.ui

/**
 * [PickerCell] の選択モードを表す列挙型（UI 層独自）。
 *
 * Android プラットフォーム標準には「単一 / 複数」を 1 つの enum で表す型が存在しないため、
 * UI 層独自の論理スイッチとして本列挙型を定義する。これは Native 型を中間論理表現でラップする
 * ものではなく、Cell の動作モードを切り替える論理スイッチであり、UI 層 API では Native 型を
 * そのまま公開する原則（core/ADR-0009）と矛盾しない。
 *
 * - [Single]: 1 件のみ選択可能。`PickerCell.selectedIndex: Int?` と組み合わせる
 * - [Multiple]: 複数件選択可能。`PickerCell.selectedIndices: Set<Int>` と組み合わせる
 */
public enum class PickerSelectionMode {
    /** 単一選択モード（既定）。 */
    Single,

    /** 複数選択モード。 */
    Multiple,
}
