package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.compose.KsIdentifiable
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.cellHeight
import jp.kamusoft.kssettingsview.compose.forEach
import jp.kamusoft.kssettingsview.compose.sectionFooter
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * iOS `DemoItem(id: Int, name: String)` に対応する Android 側の動的項目データクラス。
 *
 * `KsIdentifiable` を実装することで、DSL の `forEach(items)` で `key` lambda を省略できる。
 */
private data class DemoItem(override val id: Int, val name: String) : KsIdentifiable

/**
 * DSL 方式のデモ画面。
 *
 * iOS `samples/ios/KsSettingsViewSample/DSLDemoView.swift` と同じ 3 セクション構成に
 * 合わせる:
 *   - 静的 Section（Section footer & `.cellID(...)` 明示 ID）
 *   - 動的 Section（`forEach` + DemoItem.id 由来の安定 key）
 *   - Cell Modifier（`.cellHeight(80.dp)` のデモ）
 *
 * Root には `rootHeader` / `rootFooter` の双方を設定し、iOS 側 modifier
 * `.rootHeader(...)` / `.rootFooter(...)` と一致させる。
 *
 * 動的 Section の見出しは、プラットフォーム間で同一文言にできる中立な表現にする。
 */
@Composable
fun DSLDemoScreen() {
    var items by remember {
        mutableStateOf(
            listOf(
                DemoItem(id = 1, name = "Item A"),
                DemoItem(id = 2, name = "Item B"),
                DemoItem(id = 3, name = "Item C"),
            ),
        )
    }
    var nextId by rememberSaveable { mutableIntStateOf(4) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = {
                items = items + DemoItem(id = nextId, name = "Item $nextId")
                nextId++
            }) { Text("項目追加") }
            OutlinedButton(onClick = {
                if (items.isNotEmpty()) {
                    items = items.dropLast(1)
                }
            }) { Text("末尾削除") }
        }

        // 注意:
        //   DSL 経路では `LabelCell(...)` の `id` は DSL ノードが採番する安定 ID に
        //   rebind される（`DSLReidentifiableCell.withDSLId(...)` 経由）。本 Sample は
        //   `id` を明示指定せず、DSL の同一性判定ロジック（Section 内位置 + Cell 型 /
        //   `forEach(items)` の `KsIdentifiable.id` 引き継ぎ）に任せる方針を取る。
        //   宣言ツリーの同一性解決の原則は core/ADR-0008 を参照。
        KsSettingsView(
            modifier = Modifier.fillMaxSize(),
            style = KsSettingsViewStyle.Classic,
            rootHeader = { Text("DSL 方式のデモ画面") },
            rootFooter = {
                Text(
                    text = "© 2026 KsSettingsView Sample",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        ) {
            // 静的 Section（iOS の `.sectionFooter("...")` modifier と同等の chain 記述）。
            Section(header = "静的 Section") {
                LabelCell(title = "固定 Cell A")
                LabelCell(title = "固定 Cell B")
            }.sectionFooter("Section H/F は modifier で指定")

            // 動的 Section（iOS の `ForEach(items)` に相当）。`DemoItem` が `KsIdentifiable`
            // を実装しているため `forEach(items)` の `key` lambda は省略できる。
            // 見出しは platform API 名に依存しない中立文言にする（iOS / Android 共通）。
            Section(header = "動的 Section（繰り返し）") {
                forEach(items) { item ->
                    LabelCell(title = item.name)
                }
            }

            // Cell Modifier の例。`CellHandle.cellHeight(...)` で chain 記述する。
            Section(header = "Cell Modifier") {
                LabelCell(title = "Cellは Modifier で装飾できる").cellHeight(80.dp)
            }
        }
    }
}
