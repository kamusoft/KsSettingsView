package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.cellID
import jp.kamusoft.kssettingsview.compose.rememberScrollController
import jp.kamusoft.kssettingsview.compose.sectionID
import jp.kamusoft.kssettingsview.ui.KsScrollPosition
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/** 番号付き Section の数。 */
private const val NUMBERED_SECTION_COUNT: Int = 5

/** 番号付き Section 1 つあたりの Cell の数。 */
private const val CELLS_PER_SECTION: Int = 5

/** 「Section へ」の対象の Section の明示 ID。 */
private const val TARGET_SECTION_ID: String = "section-4"

/** 「Cell を中央へ」の対象の Cell の明示 ID。 */
private const val TARGET_CELL_ID: String = "cell-3-3"

/**
 * スクロール命令ハンドル (`KsScrollController`) の使い方を示すデモ画面。
 *
 * 一覧の中の ButtonCell から Cell・Section・先頭・末尾への命令を出し、項目を足した直後の
 * 末尾への命令が追加した項目まで届くことを確かめる。
 *
 * 命令ハンドルは画面の Composable が [rememberScrollController] で持ち、`KsSettingsView(...)` の
 * `scrollController` 引数で一覧へ接続する。命令で指す Section / Cell には `sectionID(...)` /
 * `cellID(...)` で明示 ID を付ける。開いたときに自動でスクロールはしない（操作の Section が
 * 画面外に出ないようにするため）。
 *
 * 文言・Section 構成・デモデータは iOS / MAUI と一字一句揃える。
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/ScrollControlDemoView.swift
 */
@Composable
fun ScrollControlDemoScreen() {
    val scroll = rememberScrollController()
    // 「追加して末尾へ」で足した項目の番号。最後の番号付き Section の末尾に並ぶ。
    var addedNumbers by remember { mutableStateOf(emptyList<Int>()) }

    KsSettingsView(
        modifier = Modifier.fillMaxSize(),
        style = KsSettingsViewStyle.Classic,
        rootFooterText = "ここが内容の末尾です（Root Footer）",
        scrollController = scroll,
    ) {
        Section(
            header = "操作",
            footer = "「Section へ」は Section 4 の見出しを上端へ、「Cell を中央へ」は項目 3-3 を表示範囲の中央へ移します。「追加して末尾へ」は Section 5 に項目を足して末尾へ移ります。",
        ) {
            ButtonCell(title = "末尾へ", onTap = { scroll.scrollToEnd() })
            ButtonCell(
                title = "Section へ",
                onTap = { scroll.scrollToSection(id = TARGET_SECTION_ID, position = KsScrollPosition.Start) },
            )
            ButtonCell(
                title = "Cell を中央へ",
                onTap = { scroll.scrollTo(id = TARGET_CELL_ID, position = KsScrollPosition.Center) },
            )
            ButtonCell(
                title = "追加して末尾へ",
                onTap = {
                    // 命令は足した項目が表示に反映された後に実行されるため、同じ処理の中で出せば
                    // 足した項目を含む末尾へ届く。
                    addedNumbers = addedNumbers + (addedNumbers.size + 1)
                    scroll.scrollToEnd()
                },
            )
        }

        for (number in 1..NUMBERED_SECTION_COUNT) {
            Section(header = "Section $number", footer = "Section $number の Footer") {
                for (index in 1..CELLS_PER_SECTION) {
                    LabelCell(title = "項目 $number-$index").cellID("cell-$number-$index")
                }
                if (number == NUMBERED_SECTION_COUNT) {
                    // 足した項目は番号を forEach の key にして、表示の同一性を保つ。
                    forEach(addedNumbers, key = { it }) { added ->
                        LabelCell(title = "追加した項目 $added")
                    }
                }
            }.sectionID("section-$number")
        }

        Section(header = "先頭へ戻る", footer = "「先頭へ」はアニメーションなしで移ります。") {
            ButtonCell(title = "先頭へ", onTap = { scroll.scrollToStart(animated = false) })
        }
    }
}
