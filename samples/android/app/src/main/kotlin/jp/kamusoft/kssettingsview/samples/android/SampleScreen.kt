package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.runtime.Composable

/**
 * Sample アプリの画面一覧と表示名の一元定義。
 *
 * ルートメニューの項目文言と遷移先画面の `TopAppBar` タイトルは必ずこの [title] を参照する。
 * 文言を 2 箇所に手書きすると表記ゆれが再発するため、定義はここ 1 箇所に閉じる。
 *
 * Sample はプラットフォーム間のパリティ検証装置であり、文言は全 platform で一致させる
 * （cross/ADR-0016）。対応する iOS 側定義: samples/ios/KsSettingsViewSample/SampleScreen.swift
 *
 * @property route Navigation Compose のルート文字列
 * @property title ルートメニュー項目と画面タイトルに共通で使う表示名
 */
enum class SampleScreen(val route: String, val title: String) {
    Store("store", "Store 方式デモ"),
    Dsl("dsl", "DSL 方式デモ"),
    BasicCells("basic_cells", "基本 Cell 7 種デモ"),
    InputCells("input_cells", "入力 Cell 5 種デモ"),
    CustomCell("custom_cell", "CustomCell デモ"),
    UnifyCommonFields("unify_common_fields", "共通フィールド統合デモ"),
    Visibility("visibility", "isVisible デモ（条件付き非表示）"),
    SectionDecoration("section_decoration", "Section 装飾デモ（style 切替）"),
    ;

    companion object {
        /**
         * ライブラリの使い方を示すデモ画面。プラットフォーム間で一致させる対象。
         *
         * Android には platform 固有の技術検証画面（iOS の Minimal Diffable 検証）が
         * 存在しないため、iOS の「検証」グループに相当する定義は持たない。
         */
        val demos: List<SampleScreen> = entries
    }
}

/** 遷移先の画面。iOS `SampleScreen.destination` に対応する。 */
@Composable
fun SampleScreen.Content() {
    when (this) {
        SampleScreen.Store -> StoreDemoScreen()
        SampleScreen.Dsl -> DSLDemoScreen()
        SampleScreen.BasicCells -> BasicCellsDemoScreen()
        SampleScreen.InputCells -> InputCellsDemoScreen()
        SampleScreen.CustomCell -> CustomCellDemoScreen()
        SampleScreen.UnifyCommonFields -> UnifyCellCommonFieldsDemoScreen()
        SampleScreen.Visibility -> VisibilityDemoScreen()
        SampleScreen.SectionDecoration -> SectionDecorationDemoScreen()
    }
}
