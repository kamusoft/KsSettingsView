package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * `Section.isVisible` / `Cell.isVisible` の挙動を確認するサンプル画面。
 *
 * 末尾 / 中間それぞれの位置で Cell・Section を出し入れし、アニメーションを比較する。
 * あわせて `Section.isHeaderVisible` / `Section.isFooterVisible` による
 * Header / Footer 単位の出し入れも観察できる。
 */
@Composable
fun VisibilityDemoScreen() {
    var showTailCell by remember { mutableStateOf(true) }
    var showMiddleCell by remember { mutableStateOf(true) }
    var showTailSection by remember { mutableStateOf(true) }
    var showMiddleSection by remember { mutableStateOf(true) }
    var showHeader by remember { mutableStateOf(true) }
    var showFooter by remember { mutableStateOf(true) }

    KsSettingsView(
        modifier = Modifier.fillMaxSize(),
        style = KsSettingsViewStyle.Classic,
    ) {
        // Section 1: 制御用トグル群（常に表示）
        Section(header = "基本設定") {
            SwitchCell(
                title = "末尾セル表示",
                description = "「観察対象 Section A」の末尾 Cell を出し入れ",
                isOn = showTailCell,
                onValueChanged = { showTailCell = it },
            )
            SwitchCell(
                title = "中間セル表示",
                description = "「観察対象 Section A」の中間 Cell を出し入れ",
                isOn = showMiddleCell,
                onValueChanged = { showMiddleCell = it },
            )
            SwitchCell(
                title = "末尾セクション表示",
                description = "末尾の Section C をまるごと出し入れ",
                isOn = showTailSection,
                onValueChanged = { showTailSection = it },
            )
            SwitchCell(
                title = "中間セクション表示",
                description = "中間の Section B をまるごと出し入れ",
                isOn = showMiddleSection,
                onValueChanged = { showMiddleSection = it },
            )
            SwitchCell(
                title = "ヘッダー表示",
                description = "「観察対象 Section D」の Header だけを出し入れ",
                isOn = showHeader,
                onValueChanged = { showHeader = it },
            )
            SwitchCell(
                title = "フッター表示",
                description = "「観察対象 Section D」の Footer だけを出し入れ",
                isOn = showFooter,
                onValueChanged = { showFooter = it },
            )
        }

        // Section 2 (A): Cell visibility 観察用
        //   - A-1 は常に表示（基準）
        //   - A-2 は「中間セル表示」トグルで visibility 切替
        //   - A-3 は常に表示（中間 Cell の前後を可視化するためのアンカー）
        //   - A-4 は「末尾セル表示」トグルで visibility 切替
        Section(header = "観察対象 Section A（Cell 単位）") {
            LabelCell(title = "A-1: 常時表示")
            LabelCell(
                title = "A-2: 中間セル",
                description = "「中間セル表示」トグルで出し入れ",
                isVisible = showMiddleCell,
            )
            LabelCell(title = "A-3: 常時表示（中間 Cell のアンカー）")
            LabelCell(
                title = "A-4: 末尾セル",
                description = "「末尾セル表示」トグルで出し入れ",
                isVisible = showTailCell,
            )
        }

        // Section 3 (B): Section 単位（中間）の visibility 観察用
        //   「中間セクション表示」トグルで Section ごと出し入れ。
        //   前後 Section が押し下げ・押し上げされるスライドアニメを観察できる。
        Section(
            header = "観察対象 Section B（中間 Section）",
            footer = "Section 全体が非表示になります",
            isVisible = showMiddleSection,
        ) {
            LabelCell(title = "B-1")
            LabelCell(title = "B-2")
            ButtonCell(title = "B-3 （Button）")
        }

        // Section 4 (Pivot): 中間 Section の出し入れを観察するためのアンカー。
        //   Section B の上下挙動を見るための「動かない隣」。
        Section(header = "アンカー Section（中間 Section の隣）") {
            LabelCell(title = "P-1: 常時表示")
            LabelCell(title = "P-2: 常時表示")
        }

        // Section 5 (D): Header / Footer 単位の visibility 観察用
        //   「ヘッダー表示」「フッター表示」トグルで Header / Footer を独立に出し入れする。
        //   Cell の内容と accessory の文言はそのまま保持され、表示だけが切り替わる。
        Section(
            header = "観察対象 Section D（Header / Footer）",
            footer = "Header / Footer は内容を保持したまま隠れます",
            isHeaderVisible = showHeader,
            isFooterVisible = showFooter,
        ) {
            LabelCell(title = "D-1: 常時表示")
            LabelCell(title = "D-2: 常時表示")
        }

        // Section 6 (C): Section 単位（末尾）の visibility 観察用
        //   「末尾セクション表示」トグルで Section ごと出し入れ。
        //   末尾なので fade のみ（前後の押し下げは発生しない）。
        Section(
            header = "観察対象 Section C（末尾 Section）",
            footer = "末尾なので fade アニメのみ",
            isVisible = showTailSection,
        ) {
            LabelCell(title = "C-1")
            LabelCell(title = "C-2")
        }
    }
}
