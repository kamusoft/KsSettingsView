package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.CheckboxCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.RadioCell
import jp.kamusoft.kssettingsview.compose.SimpleCheckCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * Cell 共通行レイアウトが備える共通フィールド（`description` / `valueText` / `icon` /
 * `hintText`、core/ADR-0011）と Radio/SimpleCheck の `accentColor` を視覚的に確認する
 * ためのデモ画面（Android 側）。
 */
@Composable
fun UnifyCellCommonFieldsDemoScreen() {
    val switch1 = remember { mutableStateOf(true) }
    val switch2 = remember { mutableStateOf(false) }
    val checkbox1 = remember { mutableStateOf(false) }
    val simpleCheck1 = remember { mutableStateOf(true) }
    val simpleCheck2 = remember { mutableStateOf(false) }
    val selectedTheme = remember { mutableStateOf("dark") }

    KsSettingsView(
        modifier = Modifier.fillMaxSize(),
        style = KsSettingsViewStyle.Classic,
    ) {
        Section(header = "SwitchCell — 共通フィールド") {
            SwitchCell(
                title = "通知",
                isOn = switch1,
                description = "プッシュ通知を受信",
                valueText = if (switch1.value) "オン" else "オフ",
                icon = KsImage.Resource(R.drawable.ic_notifications),
                hintText = "推奨",
            )
            SwitchCell(
                title = "Wi-Fi のみ同期",
                isOn = switch2,
                description = "従量回線では同期を停止",
                icon = KsImage.Resource(R.drawable.ic_wifi),
                hintText = "省データ",
            )
        }

        Section(header = "CheckboxCell — 共通フィールド") {
            CheckboxCell(
                title = "規約に同意",
                isChecked = checkbox1.value,
                description = "全文を読みました",
                icon = KsImage.Resource(R.drawable.ic_description),
                onValueChanged = { checkbox1.value = it },
            )
        }

        Section(header = "RadioCell — accentColor / description / icon / hintText") {
            RadioCell(
                title = "ライト",
                groupId = "theme",
                value = "light",
                selectedValue = selectedTheme.value,
                description = "明るい背景",
                icon = KsImage.Resource(R.drawable.ic_light_mode),
                accentColor = SampleTheme.demoAccentOrange,
                onSelected = { selectedTheme.value = it },
            )
            RadioCell(
                title = "ダーク",
                groupId = "theme",
                value = "dark",
                selectedValue = selectedTheme.value,
                description = "暗い背景",
                icon = KsImage.Resource(R.drawable.ic_dark_mode),
                accentColor = SampleTheme.demoAccentPurple,
                // Radio + hintText の組み合わせをデモするため、ダークセルにのみ hintText を置く。
                hintText = "推奨",
                onSelected = { selectedTheme.value = it },
            )
            RadioCell(
                title = "自動",
                groupId = "theme",
                value = "auto",
                selectedValue = selectedTheme.value,
                description = "システム設定に従う",
                icon = KsImage.Resource(R.drawable.ic_brightness_auto),
                accentColor = SampleTheme.demoAccentTeal,
                onSelected = { selectedTheme.value = it },
            )
        }

        Section(header = "SimpleCheckCell — 共通フィールド + accentColor") {
            SimpleCheckCell(
                title = "通知 1",
                isChecked = simpleCheck1.value,
                description = "週次レポート",
                icon = KsImage.Resource(R.drawable.ic_email),
                hintText = "新規",
                accentColor = SampleTheme.demoAccentPink,
                onValueChanged = { simpleCheck1.value = it },
            )
            SimpleCheckCell(
                title = "通知 2",
                isChecked = simpleCheck2.value,
                description = "月次サマリ",
                icon = KsImage.Resource(R.drawable.ic_calendar_today),
                accentColor = SampleTheme.demoAccentGreen,
                onValueChanged = { simpleCheck2.value = it },
            )
        }

        Section(header = "ButtonCell — icon / valueText / hintText 指定時") {
            ButtonCell(
                title = "登録",
                valueText = "送信",
                icon = KsImage.Resource(R.drawable.ic_send),
                hintText = "推奨",
                titleColor = SampleTheme.demoTitleBlue,
                titleAlignment = CellTitleAlignment.START,
            )
            ButtonCell(
                title = "ログアウト",
                titleAlignment = CellTitleAlignment.CENTER,
            )
        }
    }
}
