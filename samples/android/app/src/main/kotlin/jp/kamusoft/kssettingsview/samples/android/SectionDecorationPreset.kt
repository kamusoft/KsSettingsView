package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.Theme

/**
 * Section 装飾デモが切り替える Theme の Section 装飾 4 属性
 * （`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`）の組。
 *
 * 各プリセットは色定義を持たず、共通の [SampleTheme.sectionDecorationDemo] に 4 属性だけを
 * 渡して Theme を組み立てる（色値の二重管理を作らない）。
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SectionDecorationPreset.swift
 *
 * @property title プリセット選択 UI に表示する名前
 */
enum class SectionDecorationPreset(val title: String) {

    /** 4 属性すべて未指定。style ごとのライブラリ既定へ解決される。 */
    Standard("既定"),

    /** 余白を広く・角丸を小さくした組。 */
    WideMargin("余白広め・角丸小"),

    /** 既定の余白・角丸のままボーダーを指定した組。 */
    Bordered("ボーダーあり"),
    ;

    /**
     * プリセットに対応する Theme。
     *
     * @param dark 実効外観がダークなら `true`
     */
    fun theme(dark: Boolean): Theme = when (this) {
        Standard -> SampleTheme.sectionDecorationDemo(dark = dark)
        WideMargin -> SampleTheme.sectionDecorationDemo(
            dark = dark,
            sectionMargin = PaddingValues(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 0.dp),
            sectionCornerRadius = 8.dp,
        )
        Bordered -> SampleTheme.sectionDecorationDemo(
            dark = dark,
            sectionBorderWidth = 2.dp,
            sectionBorderColor = SampleTheme.demoSectionBorder,
        )
    }
}
