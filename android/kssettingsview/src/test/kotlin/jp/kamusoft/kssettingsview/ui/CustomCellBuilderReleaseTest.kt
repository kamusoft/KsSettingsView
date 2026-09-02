package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.ref.Reference
import java.lang.ref.WeakReference

/**
 * Composition が破棄されたあと、[CustomCell] の builder とその参照対象が解放可能になることを検証する
 * （android/ADR-0015）。
 *
 * # 単独のテストクラスにしている理由
 *
 * 回収の判定は `System.gc()` の要求に依存し、同一 JVM に積み上がったヒープの状態に左右される。
 * 他の検証と同居させると、回収されるかどうかが同居するテストの数と順序で揺れて、判定が信用できなく
 * なる。参照保持の検証だけを独立させることで、失敗が「本当に解放されていない」ことだけを意味する
 * 状態を保つ。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CustomCellBuilderReleaseTest {

    private lateinit var activity: ComponentActivity
    private lateinit var container: FrameLayout
    private lateinit var settingsView: KsSettingsView
    private lateinit var frameDriver: ComposeFrameDriver

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        settingsView = KsSettingsView(activity)
        container = FrameLayout(activity)
        frameDriver = ComposeFrameDriver()
        // composition が作られる前に差し込む。
        frameDriver.installOn(container)
        container.addView(settingsView)
        activity.setContentView(container)
    }

    @After
    fun tearDown() {
        frameDriver.stop()
    }

    @Test
    fun `Composition 破棄後は builder が参照するものが解放可能になる`() {
        val (reference, holder) = showAndReleaseCapturedContent()

        assertTrue("builder が参照する対象が回収されない", awaitCollected(reference))
        assertFalse(
            "前提: 対象 ViewHolder の Composition が破棄されていない",
            (holder.itemView as ComposeView).hasComposition,
        )
        // 判定が終わるまで ViewHolder を到達可能に保つ。ViewHolder ごと回収されてしまうと、
        // ViewHolder が builder への参照を握ったままでも弱参照は回収され、判定が素通りする。
        Reference.reachabilityFence(holder)
    }

    /**
     * builder だけが参照する対象を持つ行を表示し、リサイクル・プール破棄・root 差し替えまで進めて、
     * 弱参照とその行を描いた ViewHolder を返す。
     *
     * 強参照をテストメソッドのローカル変数に残すと、スタック上の参照で回収が妨げられ得るため、生成から
     * 解放までを別メソッドへ閉じ込める。ViewHolder だけは呼び出し元へ返し、「ViewHolder が生きたまま
     * builder への参照だけが切れている」ことを判定できるようにする。
     */
    private fun showAndReleaseCapturedContent(): Pair<WeakReference<Any>, RecyclerView.ViewHolder> {
        val captured = Any()
        showCells(
            buildList {
                add(
                    CustomCell(id = "a", content = "a") {
                        Box(
                            Modifier
                                .testTag("captured-${captured.hashCode()}")
                                .fillMaxWidth()
                                .height(ROW_HEIGHT_DP.dp),
                        )
                    },
                )
                addAll((0 until FILLER_COUNT).map { LabelCell(id = "f$it", title = "filler $it") })
            },
        )
        // プール投入を保証するため itemViewCache を無効化する。
        recyclerView.setItemViewCacheSize(0)
        pump()
        val holder = requireNotNull(recyclerView.findViewHolderForAdapterPosition(0)) {
            "前提: 先頭の CustomCell 行が生成されていない"
        }

        scrollToEnd()
        recyclerView.recycledViewPool.clear()
        // Cell 側の参照も手放す（元のリストと重ならない内容へ差し替える）。
        showCells((0 until 3).map { LabelCell(id = "released$it", title = "released $it") })

        return WeakReference(captured) to holder
    }

    /**
     * 弱参照が回収されるまで GC を促しながら待つ。
     *
     * `System.gc()` は要求でしかないため、確保と解放を挟んでヒープに圧力をかけながら繰り返す。
     */
    private fun awaitCollected(reference: WeakReference<*>): Boolean {
        repeat(GC_ATTEMPTS) {
            if (reference.get() == null) return true
            // 保留中の state 変更を各 Recomposer に処理させ、通知待ちの参照を解かせる。
            frameDriver.frame()
            val pressure = ArrayList<ByteArray>()
            repeat(GC_PRESSURE_BLOCKS) { pressure.add(ByteArray(GC_PRESSURE_BYTES)) }
            pressure.clear()
            System.gc()
            System.runFinalization()
            Thread.sleep(GC_PAUSE_MILLIS)
        }
        return reference.get() == null
    }

    // MARK: - 表示のためのヘルパ

    private val recyclerView
        get() = settingsView.internalRecyclerView()

    private fun showCells(cells: List<Cell>) {
        settingsView.setRootDirect(
            SettingsRoot(sections = listOf(Section(id = "s1", cells = cells))),
        )
        pump()
    }

    /**
     * 差分計算の完了を待ってレイアウトと再 composition を確定させる。
     *
     * `submitList` の差分計算は更新前後がどちらも非空のときバックグラウンドスレッドへ回り、結果は
     * メインスレッドへ post されてから反映される。単発の `idle()` では取りこぼすため、直前に流した
     * root がコミットされる収束条件を待ってから確定させる。
     */
    private fun pump() {
        awaitRootCommit(settingsView)
        settle()
    }

    private fun settle() {
        idle()
        frameDriver.frame()
        val metrics = activity.resources.displayMetrics
        container.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        container.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        frameDriver.frame()
        idle()
    }

    /**
     * 末尾まで少しずつスクロールする。
     *
     * 位置指定のジャンプは全行の再レイアウトになり、行は一時 detach を経て取り外される。実際の指
     * スクロールが通る「1 行ずつの取り外し」とは経路が異なるため、刻んだスクロールで実経路をなぞる。
     */
    private fun scrollToEnd() {
        var guard = 0
        while (recyclerView.canScrollVertically(1) && guard++ < MAX_SCROLL_STEPS) {
            recyclerView.scrollBy(
                0,
                (SCROLL_STEP_DP * activity.resources.displayMetrics.density).toInt(),
            )
            settle()
        }
    }

    private companion object {
        /** probe 行の高さ（dp）。 */
        const val ROW_HEIGHT_DP: Int = 48

        /** スクロール量を稼ぐための埋め草の数。 */
        const val FILLER_COUNT: Int = 60

        /** 刻みスクロール 1 回分の移動量（dp）。 */
        const val SCROLL_STEP_DP: Int = ROW_HEIGHT_DP * 2

        /** 刻みスクロールの打ち切り回数（無限ループ防止）。 */
        const val MAX_SCROLL_STEPS: Int = 200

        /** 弱参照の回収待ちで GC を促す回数。 */
        const val GC_ATTEMPTS: Int = 100

        /** GC を促すために都度確保するブロック数。 */
        const val GC_PRESSURE_BLOCKS: Int = 8

        /** GC を促すために確保する 1 ブロックのバイト数。 */
        const val GC_PRESSURE_BYTES: Int = 4 * 1024 * 1024

        /** GC の完了を待つ 1 回あたりの休止時間（ミリ秒）。 */
        const val GC_PAUSE_MILLIS: Long = 5
    }
}
