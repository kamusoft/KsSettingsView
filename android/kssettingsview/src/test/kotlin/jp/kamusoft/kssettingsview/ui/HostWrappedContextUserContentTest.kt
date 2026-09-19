package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * ホストがテーマを与えるために被せた `ContextThemeWrapper` を `KsSettingsView` に渡したとき、
 * 利用者所有コンテンツ（[CustomCell] の content・`KsAnyView` 経由の Section / Root accessory）が
 * **そのラッパ**の Context から生成されることを検証する。ライブラリ所有 UI は同梱テーマから、
 * 利用者所有コンテンツはホストが与えたテーマから解決する、という契約に対する回帰テストである。
 *
 * 内部 `RecyclerView` は同梱テーマをかぶせた Context から生成されるが、そのラップ元は
 * Activity まで降りた結果であり、ホストが渡したラッパではない。行の親（`RecyclerView`）の Context を
 * そのまま利用者コンテンツへ渡すと、ホストがラッパで与えたテーマ属性が解決できなくなる。
 *
 * 観測は「ラッパと Activity で値が異なるテーマ属性（`colorPrimary`）が、利用者コンテンツの Context
 * からラッパ側の値として解決できるか」で行う。値が Activity 側になっていれば解決元が誤っている。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HostWrappedContextUserContentTest {

    /** `KsSettingsView` を載せる器だけを持つホスト Activity。 */
    class HostActivity : FragmentActivity() {
        lateinit var container: FrameLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(MaterialR.style.Theme_Material3_Light_NoActionBar)
            super.onCreate(savedInstanceState)
            container = FrameLayout(this)
            setContentView(container)
        }
    }

    private var controller: ActivityController<HostActivity>? = null

    /** Section accessory の `KsAnyView.AndroidView` factory が受け取った Context。 */
    private var sectionAccessoryContext: Context? = null

    /** Root accessory の `KsAnyView.AndroidView` factory が受け取った Context。 */
    private var rootAccessoryContext: Context? = null

    @After
    fun tearDown() {
        controller?.close()
        controller = null
    }

    private fun startActivity(): HostActivity {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        return ctrl.get()
    }

    /** 指定 Context のテーマから `colorPrimary` を解決する。 */
    private fun colorPrimaryOf(context: Context): Int {
        val value = TypedValue()
        val resolved = context.theme.resolveAttribute(AppCompatR.attr.colorPrimary, value, true)
        require(resolved) { "colorPrimary が解決できない Context がテストに渡っている" }
        return value.data
    }

    private fun layoutSettingsView(activity: HostActivity, target: KsSettingsView) {
        val metrics = activity.resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * 行と accessory が実際に生成されるところまで進める。
     *
     * 構造の反映は `AsyncListDiffer` の差分計算をまたぐためバックグラウンドで進み、結果が main looper
     * へ post されるまでキューは空になり得る。回数を決めて `idle()` を呼ぶと post 前に戻り、利用者
     * コンテンツの factory がまだ呼ばれていない状態を観測してしまう。観測したいもの（利用者コンテンツ
     * の生成）そのものの成立を条件にして待つ。
     */
    private fun settle(
        activity: HostActivity,
        target: KsSettingsView,
        condition: () -> Boolean,
    ) {
        awaitConvergence(
            view = target,
            extraDiagnostics = {
                "section=${sectionAccessoryContext != null}" +
                    " / root=${rootAccessoryContext != null}" +
                    " / compose=${composeViewIn(target) != null}"
            },
        ) {
            layoutSettingsView(activity, target)
            condition()
        }
    }

    private fun rootWithUserContent(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(
                id = "s1",
                header = SectionAccessory.View(
                    KsAnyView.AndroidView { ctx ->
                        sectionAccessoryContext = ctx
                        View(ctx)
                    },
                ),
                cells = listOf(
                    CustomCell(
                        id = "c1",
                        content = "x",
                        builder = { Box(Modifier.height(24.dp)) },
                    ),
                ),
            ),
        ),
    )

    /** `RecyclerView` の表示行から `CustomCell` の `ComposeView` を探す。 */
    private fun composeViewIn(target: KsSettingsView): ComposeView? {
        val rv = target.internalRecyclerView()
        return (0 until rv.childCount)
            .flatMap { collectComposeViews(rv.getChildAt(it)) }
            .firstOrNull()
    }

    private fun collectComposeViews(view: View): List<ComposeView> = when (view) {
        is ComposeView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { collectComposeViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    @Test
    fun `ホストが被せた ContextThemeWrapper のテーマ属性が利用者所有コンテンツから解決できる`() {
        val activity = startActivity()
        // ホストが `android:theme` 等でテーマを与えた Context を `KsSettingsView` に渡す状況を作る。
        // Activity のテーマ（ライト）へ上書きする形でダークを重ね、`colorPrimary` に差を作る。
        val hostContext = ContextThemeWrapper(
            activity,
            MaterialR.style.Theme_Material3_Dark_NoActionBar,
        )
        val hostPrimary = colorPrimaryOf(hostContext)
        val activityPrimary = colorPrimaryOf(activity)
        assertNotEquals(
            "前提: ホストが被せたラッパと Activity で colorPrimary が異なる",
            activityPrimary,
            hostPrimary,
        )

        val view = KsSettingsView(hostContext)
        activity.container.addView(view)
        view.rootHeader = RootAccessory.View(
            KsAnyView.AndroidView { ctx ->
                rootAccessoryContext = ctx
                View(ctx)
            },
        )
        view.setRootDirect(rootWithUserContent())
        // 3 つの利用者コンテンツがすべて生成されるまで待つ。
        settle(activity, view) {
            sectionAccessoryContext != null &&
                rootAccessoryContext != null &&
                composeViewIn(view) != null
        }

        val sectionCtx = requireNotNull(sectionAccessoryContext) {
            "Section accessory の View factory が呼ばれていない"
        }
        val rootCtx = requireNotNull(rootAccessoryContext) {
            "Root accessory の View factory が呼ばれていない"
        }
        val composeView = requireNotNull(composeViewIn(view)) {
            "CustomCell の行が生成されていない"
        }

        // 「ホストが渡した Context そのもの」が届くことを、テーマ属性の一致より強い同一性で固定する。
        // 属性の一致だけだと、同じ属性へ解決される別の Context に差し替わっても通ってしまう。
        assertSame(
            "Section accessory の利用者 View にはホストが渡した Context そのものが届く",
            hostContext,
            sectionCtx,
        )
        assertSame(
            "Root accessory の利用者 View にはホストが渡した Context そのものが届く",
            hostContext,
            rootCtx,
        )
        assertSame(
            "CustomCell の content にはホストが渡した Context そのものが届く",
            hostContext,
            composeView.context,
        )

        assertEquals(
            "Section accessory の利用者 View はホストが渡した Context のテーマで解決する",
            hostPrimary,
            colorPrimaryOf(sectionCtx),
        )
        assertEquals(
            "Root accessory の利用者 View はホストが渡した Context のテーマで解決する",
            hostPrimary,
            colorPrimaryOf(rootCtx),
        )
        assertEquals(
            "CustomCell の content はホストが渡した Context のテーマで解決する",
            hostPrimary,
            colorPrimaryOf(composeView.context),
        )
    }

    @Test
    fun `ホストが被せた ContextThemeWrapper でもスクロールバーの描画状態は同梱テーマから届く`() {
        val activity = startActivity()
        val hostContext = ContextThemeWrapper(
            activity,
            MaterialR.style.Theme_Material3_Dark_NoActionBar,
        )

        val view = KsSettingsView(hostContext)
        activity.container.addView(view)
        view.setRootDirect(rootWithUserContent())
        settle(activity, view) { view.internalMainListAdapter().currentList.isNotEmpty() }

        // 利用者コンテンツの解決元を戻しても、一覧自身は同梱テーマの Context から生成され続ける。
        assertNotNull(
            "内部 RecyclerView はスクロールバーの描画状態を持つ",
            view.internalRecyclerView().verticalScrollbarThumbDrawable,
        )
    }
}
