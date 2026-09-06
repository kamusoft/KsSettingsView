package jp.kamusoft.kssettingsview.compose

import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.ChangeRecordingObserver
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.LabelCellViewHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout

/**
 * Compose DSL で `CellStyle` の色だけを変えた再 composition が、表示中の行へ届くことの検証。
 *
 * 利用者が両外観で異なる色を使いたいときの手段は「構築時に外観で色を選ぶ」ことであり、その
 * 手段が成立する条件が「style だけの差分が行の再 bind として届く」ことである。構造 Diff を
 * 発行しない内容更新の経路なので、行の実描画色を観測点にする（新しい Cell 値だけを見ると、
 * 行へ配り忘れていても緑になる）。あわせて、その更新で Adapter が発行した通知が内容更新だけで
 * あること（行が削除・挿入として作り直されていないこと）を観測する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DSLCellStyleUpdateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val lightTitle = Color(0xFF112233)
    private val darkTitle = Color(0xFFEEDDCC)

    @Test
    @Config(qualifiers = "notnight")
    fun `DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く`() {
        var useDarkSide by mutableStateOf(false)

        composeRule.setContent {
            val titleColor = if (useDarkSide) darkTitle else lightTitle
            KsSettingsView {
                Section(header = "見出し") {
                    cell(
                        LabelCell(
                            id = "c1",
                            title = "テキスト",
                            style = CellStyle(titleColor = titleColor),
                        ),
                    )
                }
            }
        }
        awaitTitleColor(lightTitle.toArgb())
        val observer = ChangeRecordingObserver()
        val adapter = requireNotNull(findLayout()) { "KsSettingsView が生成されていません" }
            .internalMainListAdapter()
        adapter.registerAdapterDataObserver(observer)

        useDarkSide = true
        awaitTitleColor(darkTitle.toArgb())
        adapter.unregisterAdapterDataObserver(observer)

        assertEquals("再 composition の style が行へ届いていない", darkTitle.toArgb(), titleColorOfRow())
        assertEquals(
            "style だけの更新で行が作り直されている（内容更新以外の通知が発行された）",
            emptyList<String>(),
            observer.structuralNotifications,
        )
        assertTrue(
            "style の差分が内容更新として発行されていない (通知列: ${observer.notifications})",
            observer.payloads.contains(KsSettingsViewLayout.PAYLOAD_CONTENT),
        )
    }

    /**
     * 表示中の LabelCell 行の文字色が [expected] になるまで、Compose とメインループを流しながら待つ。
     *
     * `ListAdapter` の差分計算はバックグラウンドで走り結果はメインループへ post されるため、
     * `waitForIdle()` はキューが空の状態で即座に戻り、反映の完了を待てない。行の生成前は観測
     * そのものが成立しないので、「対象行が存在し期待色になった」ことを述語にして待つ。
     *
     * 時間切れは待機条件の誤りか反映の不達であり、黙って戻ると収束前の状態を検証したことに
     * されるため、その時点の色と行一覧を載せて明示的に失敗させる。
     */
    private fun awaitTitleColor(expected: Int, timeoutMillis: Long = 5_000) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            if (labelRowHolder()?.views?.titleView?.currentTextColor == expected) return
            if (System.nanoTime() >= deadline) {
                fail(
                    "行の title 色が $timeoutMillis ms 以内に $expected へ収束しなかった" +
                        " (現在の色: ${labelRowHolder()?.views?.titleView?.currentTextColor}" +
                        " / 現在の行: ${rowHolders()})",
                )
            }
            // バックグラウンドで走る差分計算へ実行機会を譲る。yield は OS へのヒントに留まり、
            // CPU が飽和した状況では譲れる保証がないため sleep で確実に手放す。
            Thread.sleep(1)
        }
    }

    /** 表示中の LabelCell 行のタイトル文字色（実描画値）。 */
    private fun titleColorOfRow(): Int =
        requireNotNull(labelRowHolder()) { "LabelCell 行が生成されていません: ${rowHolders()}" }
            .views.titleView.currentTextColor

    /** 表示中の LabelCell 行の ViewHolder。まだ生成されていなければ null。 */
    private fun labelRowHolder(): LabelCellViewHolder? =
        rowHolders().filterIsInstance<LabelCellViewHolder>().singleOrNull()

    /** RecyclerView に並んでいる行の ViewHolder。View がまだ無い間は空リスト。 */
    private fun rowHolders(): List<RecyclerView.ViewHolder> {
        val recyclerView = findLayout()?.internalRecyclerView() ?: return emptyList()
        return (0 until recyclerView.childCount).map {
            recyclerView.getChildViewHolder(recyclerView.getChildAt(it))
        }
    }

    private fun findLayout(): KsSettingsViewLayout? =
        findLayoutIn(composeRule.activity.window.decorView as ViewGroup)

    private fun findLayoutIn(parent: ViewGroup): KsSettingsViewLayout? {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is KsSettingsViewLayout) return child
            if (child is ViewGroup) findLayoutIn(child)?.let { return it }
        }
        return null
    }
}
