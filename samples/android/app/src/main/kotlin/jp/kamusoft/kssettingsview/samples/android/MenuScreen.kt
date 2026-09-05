package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 起動直後のルートメニュー。デモ画面を一覧形式で表示する。
 *
 * iOS Sample の `ContentView`（`List` + `Section("デモ")` + `NavigationLink`）に対応する。
 * 項目文言・並び順は [SampleScreen.demos] を参照するため、遷移先の画面タイトルと同一定義になる。
 * iOS の「検証」グループ（Minimal Diffable 検証）は platform 固有画面のため Android には設けない。
 *
 * 先頭の「外観」の項目群でアプリ全体の外観を選ぶ。選択中の項目にはチェックが付く。
 *
 * @param appearance 現在の外観の選択
 * @param onSelectAppearance 外観の項目をタップしたときの通知
 * @param onSelect デモ画面の項目をタップしたときの通知
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    appearance: SampleAppearance,
    onSelectAppearance: (SampleAppearance) -> Unit,
    onSelect: (SampleScreen) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            // 外観の見出し（iOS の Section("外観") ヘッダに対応）。
            item {
                Text(
                    text = SampleAppearance.SECTION_TITLE,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            items(items = SampleAppearance.entries, key = { it.name }) { entry ->
                ListItem(
                    headlineContent = { Text(text = entry.title) },
                    trailingContent = {
                        if (entry == appearance) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = SampleAppearance.SELECTED_LABEL,
                            )
                        }
                    },
                    modifier = Modifier.clickable { onSelectAppearance(entry) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }

            // デモ群の見出し（iOS の Section("デモ") ヘッダに対応）。
            item {
                Text(
                    text = "デモ",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                )
            }
            items(items = SampleScreen.demos, key = { it.route }) { screen ->
                ListItem(
                    headlineContent = { Text(text = screen.title) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            // 行全体が遷移を表すため、装飾扱いにして読み上げの重複を避ける。
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { onSelect(screen) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}
