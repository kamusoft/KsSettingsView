package jp.kamusoft.kssettingsview.core

/**
 * [SettingsRoot] への部分更新を表現する sum type。
 *
 * Native UI 層・MAUI Handler 層の `applyDiff` 実装は本型の全ケースを網羅して処理する。
 * 各ケースは `data class` または `data object` として定義され、自動 `equals` / `hashCode` を取得する。
 *
 * # Theme を扱わない理由
 *
 * 本型に Theme 更新のケースは持たない。`Theme` は UI 層（`jp.kamusoft.kssettingsview.ui`
 * パッケージ）に属し
 * （core/ADR-0009）、Core の `SettingsRootDiff` からは参照できない。Theme 更新は構造差分では
 * なく描画スタイルの再適用であり、UI 層の独立 API
 * （`SettingsRootStore.applyTheme(_:)` / `KsSettingsView.theme` プロパティ）が担う。
 */
public sealed interface SettingsRootDiff {

    /** 全体差し替え。 */
    public data class Full(public val root: SettingsRoot) : SettingsRootDiff

    /** Section 追加。 */
    public data class InsertSection(public val index: Int, public val section: Section) : SettingsRootDiff

    /** Section 削除。 */
    public data class RemoveSection(public val sectionId: String) : SettingsRootDiff

    /** Section 順序変更。 */
    public data class MoveSection(public val from: Int, public val to: Int) : SettingsRootDiff

    /** Section 全体置換。 */
    public data class ReplaceSection(public val sectionId: String, public val newSection: Section) : SettingsRootDiff

    /** Section 内 Cell 追加。 */
    public data class InsertCell(
        public val sectionId: String,
        public val index: Int,
        public val cell: Cell,
    ) : SettingsRootDiff

    /** Cell 削除。 */
    public data class RemoveCell(public val cellId: String) : SettingsRootDiff

    /**
     * Cell 置換（= **同一 id を持つ Cell の内容更新 / reconfigure**）。
     *
     * [cellId]（置換対象 Cell の identity）と [newCell] の `id`（新しい Cell の identity）が
     * 一致していることは **呼び出し側の責務** とする。両者が不一致の場合の挙動は未定義であり、
     * Native UI 層・MAUI Handler 層の `applyDiff` 実装は一致を前提に最適化される。
     * Cell の identity を変更したい場合は本ケースではなく [RemoveCell] + [InsertCell] で表現すること。
     *
     * # 意味論
     *
     * [ReplaceCell] は「**同一 id を持つ Cell の表示内容（プロパティ）を更新する**」ことを意味し、
     * セルの破棄・再生成（フルリバインド / reload）を意味しない。UI 層はこの Diff を受けたとき、
     * 同一セル（Android: ViewHolder、iOS: Cell）の **部分更新（reconfigure）** で反映しなければならない。
     *
     * - iOS: `applyReplaceCell` が `cellIndex` の当該 Cell を差し替えてから
     *   `snapshot.reconfigureItems([cellID])`（iOS 15+、同一セルインスタンスを破棄せず再構成）で反映する。
     * - Android: アダプタが id → position を解決し payload 付きの `notifyItemChanged(position, payload)`
     *   （同一 ViewHolder への再 bind。payload なしでは既定の ItemAnimator が ViewHolder を作り直す）で反映する。
     *   `submitList` による行差し替え（フルリバインド）は起こさない。
     *
     * なお Android の Compose DSL 経路（`DSLDiffCalculator`）は「表示状態同期の三層分離」原則に従い、
     * 内容変化では [ReplaceCell] を **発行しない**（id 同一性のみの構造同期）。内容更新はアダプタの
     * 部分更新経路（`KsSettingsView.applyDiff` の [ReplaceCell] ハンドリング）が担う。一方 iOS DSL は
     * 内容変化で [ReplaceCell] を発行し `reconfigureItems` 経路に載せる（経路差は実装都合であり原則は共通）。
     */
    public data class ReplaceCell(public val cellId: String, public val newCell: Cell) : SettingsRootDiff

    /**
     * Cell 順序変更（Section 内のみ、Section 間移動は別途 [RemoveCell] + [InsertCell] で表現）。
     */
    public data class MoveCell(public val cellId: String, public val toIndex: Int) : SettingsRootDiff

    /**
     * Root H/F / Section H/F の追加・更新・削除。
     *
     * @property target 更新対象（Root H/F もしくは Section H/F）
     * @property accessory 新しい Accessory。`null` は削除を意味する
     */
    public data class UpdateAccessory(
        public val target: AccessoryTarget,
        public val accessory: SettingsAccessory?,
    ) : SettingsRootDiff
}
