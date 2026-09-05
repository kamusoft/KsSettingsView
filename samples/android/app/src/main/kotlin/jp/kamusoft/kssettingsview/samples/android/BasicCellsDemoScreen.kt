package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.CheckboxCell
import jp.kamusoft.kssettingsview.compose.CommandCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.RadioCell
import jp.kamusoft.kssettingsview.compose.SimpleCheckCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * 7 種の基本 Cell（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell /
 * RadioCell / SimpleCheckCell）を 1 画面に並べて目視確認できる Compose デモ画面。
 *
 * DSL 経路（`KsSettingsView { Section { LabelCell(title = "...") } }`）で記述し、
 * iOS Sample 側の `BasicCellsDemoView` と同じ Cell タイプ別 7 セクション構成にする。
 *
 * Theme / CellStyle / KsImage は UI 層に属し（core/ADR-0009）、フィールドは Compose の
 * `Color` / `TextStyle` / `Dp` を直接保持する。利用者は `Color(0xFFXXXXXX)` のような
 * 慣れた API でそのまま渡せる。
 *
 * `AiForms.Maui.SettingsView` の Sample（Sample/Views/MainPage.xaml）と限りなく一致する
 * 見た目を目指し、MAUI 互換 Theme（[SampleTheme.maui]）を明示渡しする。Theme 定義は
 * 入力 Cell 5 種デモと共有する。実効外観がダークのときは同じ Theme の dark 側を渡す。
 */
@Composable
fun BasicCellsDemoScreen() {
    val dark = isSystemInDarkTheme()
    var notifEnabled by remember { mutableStateOf(true) }
    var agreedTerms by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("TypeA") }
    var simpleCheck1 by remember { mutableStateOf(true) }
    var simpleCheck2 by remember { mutableStateOf(false) }
    var simpleCheck3 by remember { mutableStateOf(false) }
    var lastTappedTitle by remember { mutableStateOf("(none)") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "最後にタップ: $lastTappedTitle",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
        )

        KsSettingsView(
            modifier = Modifier.fillMaxSize(),
            style = KsSettingsViewStyle.Classic,
            theme = SampleTheme.maui(dark),
        ) {
            // 1. CommandCell セクション
            Section(header = "CommandCell", headerHeight = 60.0) {
                CommandCell(
                    style = CellStyle(cellHeight = 80.dp),
                    title = "Tanaka Taro",
                    description = "tanaka.taro@example.com",
                    icon = KsImage.Resource(R.drawable.ic_account_circle),
                    onTap = { lastTappedTitle = "Tanaka Taro" },
                )
                CommandCell(
                    title = "プロフィール",
                    onTap = { lastTappedTitle = "プロフィール" },
                )
                CommandCell(
                    title = "通知設定",
                    valueText = "オン",
                    onTap = { lastTappedTitle = "通知設定" },
                )
            }

            // 2. LabelCell セクション
            Section(header = "LabelCell") {
                LabelCell(
                    title = "Storage",
                    description = "This is description. you can write detail explanation of the item here. long text wrap automatically.",
                    valueText = "256 GB",
                    icon = KsImage.Resource(R.drawable.ic_storage),
                )
                LabelCell(
                    title = "バージョン",
                    valueText = "1.0.0",
                )
            }

            // 3. SwitchCell セクション
            Section(header = "SwitchCell") {
                SwitchCell(
                    title = "Notification",
                    description = "This is description. you can write detail explanation of the item here. long text wrap automatically.",
                    isOn = notifEnabled,
                    onValueChanged = { value ->
                        notifEnabled = value
                        lastTappedTitle = "Notification → $value"
                    },
                )
            }

            // 4. CheckboxCell セクション
            Section(header = "CheckboxCell") {
                CheckboxCell(
                    title = "Agree to Terms",
                    isChecked = agreedTerms,
                    onValueChanged = { value ->
                        agreedTerms = value
                        lastTappedTitle = "Agree → $value"
                    },
                )
            }

            // 5. RadioCell セクション
            Section(
                header = "RadioCell",
                footer = "You can select either TypeA or TypeB.",
            ) {
                RadioCell(
                    title = "TypeA",
                    groupId = "type",
                    value = "TypeA",
                    selectedValue = selectedType,
                    onSelected = { v ->
                        selectedType = v
                        lastTappedTitle = "Type → $v"
                    },
                )
                RadioCell(
                    title = "TypeB",
                    groupId = "type",
                    value = "TypeB",
                    selectedValue = selectedType,
                    onSelected = { v ->
                        selectedType = v
                        lastTappedTitle = "Type → $v"
                    },
                )
            }

            // 6. SimpleCheckCell セクション
            Section(header = "SimpleCheckCell") {
                SimpleCheckCell(
                    title = "Item 1",
                    isChecked = simpleCheck1,
                    onValueChanged = { value ->
                        simpleCheck1 = value
                        lastTappedTitle = "Item 1 → $value"
                    },
                )
                SimpleCheckCell(
                    title = "Item 2",
                    isChecked = simpleCheck2,
                    onValueChanged = { value ->
                        simpleCheck2 = value
                        lastTappedTitle = "Item 2 → $value"
                    },
                )
                SimpleCheckCell(
                    title = "Item 3",
                    isChecked = simpleCheck3,
                    onValueChanged = { value ->
                        simpleCheck3 = value
                        lastTappedTitle = "Item 3 → $value"
                    },
                )
            }

            // 7. ButtonCell セクション
            Section(header = "ButtonCell") {
                ButtonCell(
                    style = CellStyle(titleColor = SampleTheme.mauiTitleText(dark)),
                    title = "ログアウト",
                    titleAlignment = CellTitleAlignment.CENTER,
                    onTap = { lastTappedTitle = "ログアウト" },
                )
            }
        }
    }
}
