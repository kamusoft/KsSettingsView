package jp.kamusoft.kssettingsview.ui

import android.content.Context

/**
 * [CustomCell] を [KsCellRegistry] に登録する拡張関数。
 *
 * [CustomCell] は基本 Cell 7 種・入力 Cell 5 種と同じ標準登録集合に属し、`KsSettingsView` の
 * 初期化時に自動登録される。利用者が Registry を操作しなくても DSL に直接書けること
 * （core/ADR-0014）を成立させるための登録経路であり、利用者側での明示登録は不要。
 *
 * Kotlin の型消去により `CustomCell<*>` の実体型は content 型によらず単一のクラスになるため、
 * 登録も 1 回で足りる。
 *
 * # viewType 割り当て
 *
 * 基本 Cell の 100 番台・入力 Cell の 110 番台と衝突しないよう 120 番を割り当てる。
 *
 * @param context 現状は ViewHolder ファクトリが親 ViewGroup から Context を得るため未使用だが、
 *   基本 Cell / 入力 Cell の登録 API とシグネチャを揃えるために受け取る。
 */
@Suppress("UNUSED_PARAMETER")
public fun KsCellRegistry.registerCustomCell(context: Context) {
    register(
        cellClass = CustomCell::class,
        viewType = VIEW_TYPE_CUSTOM_CELL,
        // content は利用者所有のコンテンツであり、テーマ属性はホストのテーマで解決させる
        // （android/ADR-0020）。
        factory = { parent -> CustomCellViewHolder(parent.context.ksHostContext()) },
    )
}

internal const val VIEW_TYPE_CUSTOM_CELL: Int = 120
