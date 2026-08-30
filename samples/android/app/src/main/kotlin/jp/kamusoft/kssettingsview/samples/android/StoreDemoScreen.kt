package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.settingsRoot
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.LabelCell as LabelCellData
import jp.kamusoft.kssettingsview.ui.SettingsRootStore

/** Store 方式デモの最初の Section の ID。 */
private const val FIRST_SECTION_ID: String = "sample-section"

/**
 * Store 方式（[SettingsRootStore] による部分更新）のデモ画面。
 *
 * `remember` で [SettingsRootStore] を保持し、「項目追加」「項目削除」ボタンで
 * `store.insertCell` / `store.removeCell` を呼んで部分更新を確認する。
 * 表示文言は iOS `StoreDemoView.swift` と一字一句一致させる（cross/ADR-0016）。
 */
@Composable
fun StoreDemoScreen() {
    val store = remember {
        SettingsRootStore(
            initialRoot = settingsRoot {
                section(
                    id = FIRST_SECTION_ID,
                    header = SectionAccessory.Text("PoC Section"),
                    footer = SectionAccessory.Text("This is a footer"),
                ) {
                    cell(LabelCellData(id = "sample-1", title = "Sample Row 1"))
                    cell(LabelCellData(id = "sample-2", title = "Sample Row 2"))
                    cell(LabelCellData(id = "sample-3", title = "Sample Row 3"))
                }
            },
        )
    }
    // 「項目追加」で生成する Cell の通番。configuration change を跨いで保持する。
    var nextIndex by rememberSaveable { mutableIntStateOf(4) }

    // Flow の収集はライフサイクル連携付きで行う（画面が STOPPED の間は収集を止める）。
    val state by store.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val firstSection = state.sections.firstOrNull { it.id == FIRST_SECTION_ID }
                        ?: return@Button
                    val endIndex = firstSection.cells.size
                    store.insertCell(
                        cell = LabelCellData(
                            id = "sample-extra-$nextIndex",
                            title = "新規 $nextIndex",
                        ),
                        sectionId = FIRST_SECTION_ID,
                        at = endIndex,
                    )
                    nextIndex += 1
                },
            ) {
                Text(text = "項目追加")
            }
            OutlinedButton(
                onClick = {
                    val firstSection = state.sections.firstOrNull { it.id == FIRST_SECTION_ID }
                        ?: return@OutlinedButton
                    val lastCell = firstSection.cells.lastOrNull() ?: return@OutlinedButton
                    store.removeCell(cellId = lastCell.id)
                },
            ) {
                Text(text = "項目削除")
            }
        }

        KsSettingsView(
            store = store,
            style = KsSettingsViewStyle.Classic,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
