package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import jp.kamusoft.kssettingsview.compose.CommandCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * style（Classic / Modern）と Theme の Section 装飾 4 属性を実行時に切り替えて、
 * 同じ設定内容が装飾だけ変わって描かれることを目視確認するデモ画面。
 *
 * Modern で確認できること:
 *   - Section の Cell 行だけを角丸の箱が覆い、Header / Footer は箱の外側に置かれる
 *   - separator は箱の中間だけに引かれ、箱の上下端には出ない（単一 Cell の Section には出ない）
 *   - プリセット切替で箱の余白・角丸・ボーダーが変わる
 *
 * Classic では箱を描かないため、プリセットの差は Section 間の上下余白にだけ現れる。
 *
 * 装飾値は SettingsView 全体の Theme が持つため、ボーダーの有無は画面内の全 Section に
 * 一括で効く（Section 単位の上書きは公開していない）。
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/SectionDecorationDemoView.swift
 */
@Composable
fun SectionDecorationDemoScreen() {
    val dark = isSystemInDarkTheme()
    var style by remember { mutableStateOf(KsSettingsViewStyle.Modern) }
    var preset by remember { mutableStateOf(SectionDecorationPreset.Standard) }

    var airplaneMode by remember { mutableStateOf(false) }
    var autoAppearance by remember { mutableStateOf(true) }
    var trueTone by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val badges = remember(context) { SampleIconBadge.badges(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        SectionDecorationDemoControls(
            style = style,
            onStyleChange = { style = it },
            preset = preset,
            onPresetChange = { preset = it },
        )

        KsSettingsView(
            modifier = Modifier.fillMaxSize(),
            style = style,
            theme = preset.theme(dark),
        ) {
            // 1. Header / Footer なし・icon 付き Cell の Section
            Section {
                SwitchCell(
                    title = "機内モード",
                    icon = badges.airplane,
                    isOn = airplaneMode,
                    onValueChanged = { newValue -> airplaneMode = newValue },
                )
                CommandCell(
                    title = "Wi-Fi",
                    valueText = "demoAP-0a1b2c-5",
                    icon = badges.wifi,
                )
                CommandCell(
                    title = "Bluetooth",
                    valueText = "オン",
                    icon = badges.bluetooth,
                )
                CommandCell(
                    title = "バッテリー",
                    icon = badges.battery,
                )
            }

            // 2. Header / Footer 付きの Section（どちらも箱の外側に置かれる）
            Section(
                header = "外観モード",
                footer = "好みに応じて外観モードを選択できます。Header と Footer は箱の外側に配置されます。",
            ) {
                SwitchCell(
                    title = "自動",
                    isOn = autoAppearance,
                    onValueChanged = { newValue -> autoAppearance = newValue },
                )
                CommandCell(title = "テキストサイズを変更")
            }

            // 3. 単一 Cell の Section（separator は引かれない）
            Section {
                SwitchCell(
                    title = "True Tone",
                    isOn = trueTone,
                    onValueChanged = { newValue -> trueTone = newValue },
                )
            }

            // 4. ボーダー指定の観察用 Section
            Section(
                header = "ボーダー指定時の例",
                footer = "既定はボーダーなし (width 0)。指定時のみ枠線が箱の輪郭に描かれます。",
            ) {
                LabelCell(title = "sectionBorderWidth: 2")
                LabelCell(title = "sectionBorderColor: gray")
            }
        }
    }
}
