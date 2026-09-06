package jp.kamusoft.kssettingsview.samples.android

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.compose.CommandCell
import jp.kamusoft.kssettingsview.compose.CustomCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * `CustomCell`（任意の Compose コンテンツを行全体に描画する Cell）を 5 構成で目視確認できる
 * Compose デモ画面。
 *
 *   ① インライン        — content + builder を DSL に直書き / content 省略の静的糖衣
 *   ② 再利用            — CustomCell を返すラップ関数（SampleSliderCell.kt）
 *   ③ 動的高さ          — content 内の操作で展開/折りたたみし、行高さが追従する
 *   ④ showArrow / onTap — Disclosure Indicator と行タップ（子要素タップとの二重発火なし）
 *   ⑤ スクロール耐性    — 同型の CustomCell 40 行。行の再利用で表示・listener が混線しないこと
 *
 * 対応する iOS 側定義: samples/ios/KsSettingsViewSample/CustomCellDemoView.swift
 *   （文言・セクション構成・パラメータは iOS 側と一字一句揃える）
 */
@Composable
fun CustomCellDemoScreen() {
    val dark = isSystemInDarkTheme()

    // ② SliderCell ラップ関数に渡す値
    var brightness by remember { mutableIntStateOf(70) }
    var volume by remember { mutableIntStateOf(40) }
    var disabledValue by remember { mutableIntStateOf(60) }

    // ③ 動的高さ
    var isTermsExpanded by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(true) }

    // ④ 行タップ
    var rowTapCount by remember { mutableIntStateOf(0) }

    // ⑤ スクロール耐性（タップ済みのダミー行）
    var tappedDummyIndices by remember { mutableStateOf(emptySet<Int>()) }

    // ⑤ のダミー行データ。アクセント 6 色を循環させる。
    val dummyItems = (1..DUMMY_ROW_COUNT).map { index ->
        SampleDummyItem(index = index, isTapped = tappedDummyIndices.contains(index))
    }

    KsSettingsView(
        modifier = Modifier.fillMaxSize(),
        style = KsSettingsViewStyle.Classic,
        theme = SampleTheme.maui(dark),
    ) {
        // ① インライン CustomCell
        Section(
            header = "インライン CustomCell",
            footer = "content + builder を DSL に直書きした例と、content 省略の静的糖衣の例。",
        ) {
            // content + builder を直書きする形。
            CustomCell(content = SampleSyncState(isOk = true)) { state ->
                SampleAccentRow(
                    dotColor = SampleTheme.demoAccentGreen,
                    title = "同期ステータス",
                    subtitle = "content: SyncState(ok: ${state.isOk})",
                ) {
                    SampleTagLabel(
                        text = "同期済み",
                        background = SampleTheme.mauiAccent,
                        foreground = SampleTheme.mauiCellBackground,
                    )
                }
            }

            // content を持たない静的糖衣の形（builder だけ）。
            CustomCell {
                Text(
                    text = "content なしの静的 CustomCell（キャプション行）",
                    fontSize = 13.sp,
                    color = SampleTheme.mauiFooterText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        // ② 再利用（SliderCell ラップ関数）
        Section(
            header = "再利用（SliderCell ラップ関数）",
            footer = "SliderCell(label:value:) 関数が CustomCell を返す再利用例。",
        ) {
            cell(SliderCell(label = "明るさ", value = brightness) { brightness = it })
            cell(SliderCell(label = "音量", value = volume) { volume = it })
            // isEnabled = false の行。行タップだけでなく content 内のスライダーのドラッグも
            // 抑止され、content が淡色で描画される。
            cell(
                SliderCell(label = "無効", value = disabledValue, isEnabled = false) {
                    disabledValue = it
                },
            )
        }

        // ③ 動的高さ
        Section(
            header = "動的高さ",
            footer = "content 内の状態で展開/折りたたみ。行高さは self-sizing で追従する。",
        ) {
            CustomCell(
                content = SampleExpanderState(
                    title = "利用規約（タップで展開）",
                    body = POLICY_BODY,
                    isExpanded = isTermsExpanded,
                ),
            ) { state ->
                SampleExpanderRow(state = state, onToggle = { isTermsExpanded = !isTermsExpanded })
            }

            CustomCell(
                content = SampleExpanderState(
                    title = "プライバシーポリシー（展開中）",
                    body = POLICY_BODY,
                    isExpanded = isPrivacyExpanded,
                ),
            ) { state ->
                SampleExpanderRow(state = state, onToggle = { isPrivacyExpanded = !isPrivacyExpanded })
            }
        }

        // ④ showArrow / onTap
        Section(
            header = "showArrow / onTap",
            footer = "chevron は既存 CommandCell と同一素材・同一位置で表示される。",
        ) {
            // showArrow = true。行タップは下の行タップカウンタと同じカウンタを進める。
            CustomCell(
                content = SampleTitledContent(
                    title = "詳細設定",
                    subtitle = "showArrow: true / onTap で遷移",
                ),
                showArrow = true,
                onTap = { rowTapCount += 1 },
            ) { content ->
                SampleAccentRow(
                    dotColor = null,
                    title = content.title,
                    subtitle = content.subtitle,
                    // chevron 側の余白は CustomCell が持つため、content 側は詰める。
                    trailingPadding = 8.dp,
                ) {}
            }

            // chevron の見た目・位置を隣接比較するための既存 Cell（検証用の基準行）。
            CommandCell(title = "詳細設定（CommandCell）")

            // onTap のみ（矢印なし）。ピルは content 内のクリック可能要素で、
            // タップしてもカウンタは進まず 0 に戻る（子要素の操作で行 onTap は発火しない）。
            CustomCell(
                content = SampleTapCounter(count = rowTapCount),
                onTap = { rowTapCount += 1 },
            ) { content ->
                SampleAccentRow(
                    dotColor = null,
                    title = "行タップカウンタ",
                    subtitle = "onTap のみ（矢印なし）",
                ) {
                    SampleTagLabel(
                        text = "${content.count} 回",
                        background = SampleTheme.demoPillBackground,
                        foreground = SampleTheme.mauiHeaderText,
                        modifier = Modifier.clickable { rowTapCount = 0 },
                    )
                }
            }
        }

        // ⑤ スクロール耐性（ダミー #01–#40）
        Section(
            header = "スクロール耐性（ダミー #01–#40）",
            footer = "十分なスクロール量を確保し、行の再利用（リサイクル）で表示・listener が混線しないことを確認する。",
        ) {
            forEach(items = dummyItems, key = { it.index }) { item ->
                CustomCell(
                    content = item,
                    onTap = {
                        tappedDummyIndices = if (tappedDummyIndices.contains(item.index)) {
                            tappedDummyIndices - item.index
                        } else {
                            tappedDummyIndices + item.index
                        }
                    },
                ) { content ->
                    val number = paddedNumber(content.index)
                    SampleAccentRow(
                        dotColor = SampleTheme.demoAccentPalette[
                            (content.index - 1) % SampleTheme.demoAccentPalette.size,
                        ],
                        title = "ダミー行 #$number",
                        subtitle = "content: DummyItem(${content.index})",
                    ) {
                        SampleTagLabel(
                            text = if (content.isTapped) "#$number ✓" else "#$number",
                            background = SampleTheme.demoPillBackground,
                            foreground = SampleTheme.mauiHeaderText,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// content 値
// =============================================================================

/** ①「同期ステータス」行の content。 */
data class SampleSyncState(val isOk: Boolean)

/**
 * ③「動的高さ」行の content。
 *
 * 展開状態を content に含めることで、トグル時に等価性が崩れて行が再バインドされ、
 * 新しい高さで再計測される（これが「高さの自動追従」の実体）。
 */
data class SampleExpanderState(
    val title: String,
    val body: String,
    val isExpanded: Boolean,
)

/** ④「詳細設定」行の content。 */
data class SampleTitledContent(
    val title: String,
    val subtitle: String,
)

/** ④「行タップカウンタ」行の content。 */
data class SampleTapCounter(val count: Int)

/** ⑤「スクロール耐性」ダミー行の content。 */
data class SampleDummyItem(
    val index: Int,
    val isTapped: Boolean,
)

// =============================================================================
// 行 Composable
// =============================================================================

/**
 * 「ドット + タイトル/サブタイトル + trailing スロット」の共通行 Composable。
 *
 * [CustomCell] は full-bleed（ライブラリ側が行の内側マージンを持たない）ため、
 * 標準 Cell と横位置を揃える 16dp の余白は content 側で明示的に持たせる。
 */
@Composable
fun SampleAccentRow(
    dotColor: Color?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingPadding: Dp = 16.dp,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = trailingPadding, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(12.dp)
                    .background(color = dotColor, shape = CircleShape),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = SampleTheme.mauiDeepText)
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SampleTheme.mauiFooterText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        trailing()
    }
}

/** バッジ / ピル用の角丸ラベル。 */
@Composable
fun SampleTagLabel(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = modifier
            .background(color = background, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/**
 * ③「動的高さ」の展開/折りたたみ行。
 *
 * 見出し部が content 内のクリック可能要素であり、行の `onTap` は指定していない
 * （`onTap` 未指定の CustomCell は行タップ動作を持たず、content 内の操作を妨げない）。
 */
@Composable
fun SampleExpanderRow(
    state: SampleExpanderState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (state.isExpanded) "▼" else "▶",
                fontSize = 13.sp,
                color = SampleTheme.mauiAccent,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = state.title, fontSize = 16.sp, color = SampleTheme.mauiDeepText)
        }

        if (state.isExpanded) {
            Text(
                text = state.body,
                fontSize = 13.sp,
                lineHeight = 20.8.sp,
                color = SampleTheme.demoExpandText,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .background(
                        color = SampleTheme.demoExpandBackground,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

// =============================================================================
// デモデータ
// =============================================================================

/** ⑤ のダミー行数。 */
private const val DUMMY_ROW_COUNT: Int = 40

/** ③ の展開本文（2 行とも同じ本文を使う）。 */
private const val POLICY_BODY: String =
    "本アプリはお客様の設定情報を端末内にのみ保存します。収集した情報を第三者に提供することはありません。" +
        "設定のバックアップを有効にした場合のみ、暗号化した上でクラウドに保存します。"

/** 2 桁ゼロ埋めの連番文字列。 */
private fun paddedNumber(value: Int): String = value.toString().padStart(2, '0')
