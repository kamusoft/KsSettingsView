package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * Section 装飾デモの操作部。style（Classic / Modern）と Section 装飾プリセットを選ぶ。
 *
 * SettingsView 自身の描画に影響を与えないよう、デモ本体の上に独立して置く。
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SectionDecorationDemoControls.swift
 *
 * @param style 現在選択中の style
 * @param onStyleChange style 選択時のコールバック
 * @param preset 現在選択中の装飾プリセット
 * @param onPresetChange プリセット選択時のコールバック
 * @param modifier Compose Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDecorationDemoControls(
    style: KsSettingsViewStyle,
    onStyleChange: (KsSettingsViewStyle) -> Unit,
    preset: SectionDecorationPreset,
    onPresetChange: (SectionDecorationPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val styles = listOf(KsSettingsViewStyle.Classic, KsSettingsViewStyle.Modern)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            styles.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = style == item,
                    onClick = { onStyleChange(item) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = styles.size),
                ) {
                    Text(text = item.name)
                }
            }
        }

        Box(contentAlignment = Alignment.CenterStart) {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = "装飾プリセット",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SectionDecorationPreset.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item.title) },
                        onClick = {
                            onPresetChange(item)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
