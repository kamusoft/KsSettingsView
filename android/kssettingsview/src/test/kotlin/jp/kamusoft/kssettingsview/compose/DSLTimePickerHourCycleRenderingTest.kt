package jp.kamusoft.kssettingsview.compose

import android.app.Dialog
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout
import jp.kamusoft.kssettingsview.ui.TimePickerCell as UiTimePickerCell

/**
 * Compose 宣言 DSL で `TimePickerCell` の `is24Hour` を変えたとき、DSL 再評価 → 差分検出 → Store →
 * 表示の経路を通って選択面の系列構成へ届くことを検証する。
 *
 * `is24Hour` は生成後の変更が表示へ反映される動的反映プロパティであり、Store 経路と DSL 経路の
 * 双方に反映テストを持つ（core/ADR-0018 の対称テスト）。DSL 側は変化が明示されないため、検出層が
 * `is24Hour` の差を翻訳できないと diff 0 件のまま表示に届かない無音の失敗になる。ここでは実際に
 * 提示された選択面のホイール系列数を観測し、DSL ツリーの値だけを見て通したことにしない。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ja")
class DSLTimePickerHourCycleRenderingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    // MARK: - 表示の観測

    /** composition に並んだ `KsSettingsView` を、view ツリーの出現順で集める。 */
    private fun settingsLayouts(): List<KsSettingsViewLayout> {
        val found = mutableListOf<KsSettingsViewLayout>()
        fun walk(view: View) {
            if (view is KsSettingsViewLayout) found.add(view)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(composeRule.activity.window.decorView)
        return found
    }

    private fun findRecyclerView(root: ViewGroup): RecyclerView? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is RecyclerView) return child
            if (child is ViewGroup) findRecyclerView(child)?.let { return it }
        }
        return null
    }

    /**
     * [index] 番目の `KsSettingsView` の先頭行をタップし、提示された選択面のホイール系列数を返す。
     *
     * 系列数は 24時間制なら 2（時 / 分）、12時間制なら 3（時 / 分 / 午前・午後）になる。ホイールの
     * View 型はライブラリ内部の型なので、クラス名で数える。行が生成されていなければ 0 を返す
     * （成立の待機は [awaitSeriesCount] が担う）。
     *
     * `ShadowDialog.getLatestDialog()` は dismiss 済みの選択面も「最後に表示されたもの」として
     * 返し続けるため、タップ前のインスタンスを控えて別物になったことを確かめてから数える。
     * 同一のままなら今回のタップでは開いていないので 0 を返し、待機を続けさせる。
     */
    private fun seriesCountByTappingRow(index: Int): Int {
        val layout = settingsLayouts().getOrNull(index) ?: return 0
        val recycler = findRecyclerView(layout) ?: return 0
        val row = recycler.getChildAt(0) ?: return 0
        val previousDialog = ShadowDialog.getLatestDialog()
        composeRule.runOnUiThread { row.performClick() }
        shadowOf(Looper.getMainLooper()).idle()
        val dialog = ShadowDialog.getLatestDialog() ?: return 0
        if (dialog === previousDialog) return 0
        val count = countWheels(dialog)
        dialog.dismiss()
        shadowOf(Looper.getMainLooper()).idle()
        return count
    }

    /**
     * [index] 番目の選択面のホイール系列数が [expected] になるまで、行タップを繰り返して待つ。
     *
     * 行の内容更新は `AsyncListDiffer` のバックグラウンド差分計算とメインループへの post を経て
     * 届くため、recomposition 直後にタップすると更新前の Cell が bind されたままの行を開くことが
     * ある。idle 系の呼び出しだけでは差分計算の完了を待てないので、観測したい結果そのものが
     * 成立するまで実時間の deadline で区切って待つ。時間切れは黙って戻らず、その時点の系列数を
     * 載せて失敗させる。
     */
    private fun awaitSeriesCount(index: Int, expected: Int, message: String, timeoutMillis: Long = 5_000) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            val actual = seriesCountByTappingRow(index)
            if (actual == expected) return
            if (System.nanoTime() >= deadline) {
                fail("$message (index=$index の系列数が $timeoutMillis ms 以内に $expected へ収束しない。現在: $actual)")
            }
            // バックグラウンドで走る差分計算へ実行機会を譲る。yield は OS へのヒントに留まり、
            // CPU が飽和した状況では譲れる保証がないため sleep で確実に手放す。
            Thread.sleep(1)
        }
    }

    /** [dialog] の view ツリーに載っているホイールの数。 */
    private fun countWheels(dialog: Dialog): Int {
        var count = 0
        fun walk(view: View) {
            if (view.javaClass.simpleName == WHEEL_VIEW_CLASS_NAME) count++
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) walk(view.getChildAt(i))
            }
        }
        walk(requireNotNull(dialog.window) { "選択面に window が無い" }.decorView)
        return count
    }

    // MARK: - 検証対象の画面

    /** DSL 側の TimePickerCell 1 行。時制だけを引数で切り替える。 */
    @Composable
    private fun DslSettings(is24Hour: Boolean, modifier: Modifier = Modifier) {
        val time = remember { mutableStateOf(LocalTime.of(22, 15)) }
        KsSettingsView(modifier = modifier) {
            Section {
                TimePickerCell(title = "就寝", time = time, is24Hour = is24Hour)
            }
        }
    }

    /** Store 側の同一内容 Section。 */
    private fun storeSection(is24Hour: Boolean) = Section(
        id = "s1",
        cells = listOf(
            UiTimePickerCell(
                id = "tp",
                title = "就寝",
                time = LocalTime.of(22, 15),
                is24Hour = is24Hour,
            ),
        ),
    )

    // MARK: - DSL 経路の表示反映

    @Test
    fun `DSL 再評価で is24Hour 変更が選択面の系列へ反映される`() {
        var is24Hour by mutableStateOf(true)
        composeRule.setContent {
            DslSettings(is24Hour = is24Hour)
        }

        awaitSeriesCount(0, expected = 2, message = "既定は 24 時間制の 2 系列にならない")

        composeRule.runOnUiThread { is24Hour = false }

        awaitSeriesCount(0, expected = 3, message = "DSL 再評価の is24Hour 変更が選択面へ届いていない")
    }

    // MARK: - Store 経路と DSL 経路の対称性（core/ADR-0018）

    @Test
    fun `Store 経路と DSL 経路で is24Hour 変更後の選択面が一致する`() {
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(sections = listOf(storeSection(is24Hour = true))),
        )
        var is24Hour by mutableStateOf(true)
        composeRule.setContent {
            Column {
                KsSettingsView(store = store, modifier = Modifier.weight(1f))
                DslSettings(is24Hour = is24Hour, modifier = Modifier.weight(1f))
            }
        }

        awaitSeriesCount(0, expected = 2, message = "Store 経路の初期表示が 24 時間制にならない")
        awaitSeriesCount(1, expected = 2, message = "DSL 経路の初期表示が 24 時間制にならない")

        // 同じ変更をそれぞれの経路で与える。
        store.replaceSection("s1", storeSection(is24Hour = false))
        composeRule.runOnUiThread { is24Hour = false }

        // 2 経路は別々の RecyclerView で反映のタイミングも独立しているため、それぞれの収束を待って
        // から比較する。片方の収束だけを待つと、もう片方が収束前の状態のまま比較される。
        awaitSeriesCount(0, expected = 3, message = "Store 経路で 12 時間制の 3 系列にならない")
        awaitSeriesCount(1, expected = 3, message = "DSL 経路で 12 時間制の 3 系列にならない")
        assertEquals(
            "is24Hour 変更後の選択面が Store 経路と DSL 経路で一致しない",
            seriesCountByTappingRow(0),
            seriesCountByTappingRow(1),
        )
    }

    private companion object {
        /** 選択面のホイールを数えるための View クラス名。 */
        const val WHEEL_VIEW_CLASS_NAME: String = "KsWheelView"
    }
}
