package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * Theme 変更が表示中の Root Header / Footer（text 形式）へ届くことを検証する。
 *
 * Root H/F は `SettingsRoot` ではなく View 層のプロパティに載るため、`submitList` の差分計算では
 * 再描画の引き金が立たない。Theme の反映は専用 Adapter への部分更新通知だけが担っており、通知が
 * 欠けても内部の Theme 値だけは新しくなるため、内部状態を観測するテストでは追従の欠落を検出できない。
 * そこで本テストは実際に生成された `TextView` の文字色とフォントサイズを観測する。
 *
 * Theme が View へ入る経路は 3 つあり、それぞれ独立に通知が欠け得るため個別に検証する。
 * - `theme` プロパティへの直接代入
 * - Store の `theme` StateFlow 購読
 * - `bind` による Store の初期 Theme 取り込み
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RootAccessoryThemeRefreshTest {

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

    /** レイアウトを走らせて RecyclerView に行を生成させる。 */
    private fun HostActivity.layoutSettingsView(target: KsSettingsView) {
        val metrics = resources.displayMetrics
        target.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
        )
        target.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * 部分更新通知が実際の再 bind へ届くところまで進める。
     *
     * `notifyItemRangeChanged` は行に「描き直しが要る」印を付けるだけで、`onBindViewHolder` は
     * 次のレイアウトで走る。キューを流してからレイアウトを 2 度繰り返し、通知の消化と再 bind の
     * 両方を確実に通す。
     */
    private fun HostActivity.settle(target: KsSettingsView) {
        repeat(2) {
            idle()
            layoutSettingsView(target)
        }
    }

    /** 表示中の行から [text] を表示している TextView を探す。 */
    private fun textViewOf(view: KsSettingsView, text: String): TextView {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount)
            .flatMap { collectTextViews(rv.getChildAt(it)) }
            .firstOrNull { it.text?.toString() == text }
        return requireNotNull(found) { "表示行に \"$text\" の TextView が見つからない" }
    }

    private fun collectTextViews(view: View): List<TextView> = when (view) {
        is TextView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { collectTextViews(view.getChildAt(it)) }
        else -> emptyList()
    }

    /** 表示中の行に載っている唯一の `EditText`（View 形式 accessory の中身）を返す。 */
    private fun editTextIn(view: KsSettingsView): EditText {
        val rv = view.internalRecyclerView()
        val found = (0 until rv.childCount)
            .flatMap { collectTextViews(rv.getChildAt(it)) }
            .filterIsInstance<EditText>()
        return found.singleOrNull()
            ?: error("表示行の EditText がちょうど 1 つではない (${found.size} 個)")
    }

    /** sp 指定のフォントサイズを、`TextView.textSize` が返す px へ換算する。 */
    private fun spToPx(view: TextView, sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, view.resources.displayMetrics)

    private fun singleCellRoot(): SettingsRoot = SettingsRoot(
        sections = listOf(
            Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "A"))),
        ),
    )

    /**
     * Root H/F を表示した状態のホストを作る。
     *
     * Store を介さず `setRootDirect` で初期 Theme ごと反映し、レイアウトまで済ませて
     * ViewHolder が [theme] で bind された状態にする。
     */
    private fun hostView(
        activity: HostActivity,
        rootHeader: RootAccessory? = null,
        rootFooter: RootAccessory? = null,
        theme: Theme = Theme(),
    ): KsSettingsView {
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.rootHeader = rootHeader
        view.rootFooter = rootFooter
        view.setRootDirect(singleCellRoot(), theme)
        awaitConvergence(view) { committedTexts(view) == listOf("A") }
        activity.layoutSettingsView(view)
        return view
    }

    // ---------------------------------------------------------------------
    // theme プロパティ経由
    // ---------------------------------------------------------------------

    @Test
    fun `theme 代入で表示中の Root Header のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF1933E6)
        val view = hostView(
            activity,
            rootHeader = RootAccessory.Text("プロフィール"),
            theme = Theme(headerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の headerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )

        view.theme = Theme(headerTextColor = updated)
        activity.settle(view)

        assertEquals(
            "theme 代入後の Root Header の文字色は新しい Theme の headerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )
    }

    @Test
    fun `theme 代入で表示中の Root Footer のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF008040)
        val view = hostView(
            activity,
            rootFooter = RootAccessory.Text("v1.0.0"),
            theme = Theme(footerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の footerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "v1.0.0").currentTextColor,
        )

        view.theme = Theme(footerTextColor = updated)
        activity.settle(view)

        assertEquals(
            "theme 代入後の Root Footer の文字色は新しい Theme の footerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "v1.0.0").currentTextColor,
        )
    }

    @Test
    fun `theme 代入で表示中の Root Header のフォントサイズが追従する`() {
        val activity = startActivity()
        val view = hostView(
            activity,
            rootHeader = RootAccessory.Text("プロフィール"),
            theme = Theme(headerFontSize = 12.0),
        )

        val before = textViewOf(view, "プロフィール")
        assertEquals(
            "前提: 初期 Theme の headerFontSize が反映されていない",
            spToPx(before, 12.0f),
            before.textSize,
            0.5f,
        )

        view.theme = Theme(headerFontSize = 24.0)
        activity.settle(view)

        val after = textViewOf(view, "プロフィール")
        assertEquals(
            "theme 代入後の Root Header のフォントサイズは新しい Theme の headerFontSize になる",
            spToPx(after, 24.0f),
            after.textSize,
            0.5f,
        )
    }

    @Test
    fun `theme 代入で表示中の Root Footer のフォントサイズが追従する`() {
        val activity = startActivity()
        val view = hostView(
            activity,
            rootFooter = RootAccessory.Text("v1.0.0"),
            theme = Theme(footerFontSize = 11.0),
        )

        val before = textViewOf(view, "v1.0.0")
        assertEquals(
            "前提: 初期 Theme の footerFontSize が反映されていない",
            spToPx(before, 11.0f),
            before.textSize,
            0.5f,
        )

        view.theme = Theme(footerFontSize = 22.0)
        activity.settle(view)

        val after = textViewOf(view, "v1.0.0")
        assertEquals(
            "theme 代入後の Root Footer のフォントサイズは新しい Theme の footerFontSize になる",
            spToPx(after, 22.0f),
            after.textSize,
            0.5f,
        )
    }

    // ---------------------------------------------------------------------
    // Store 購読経由
    // ---------------------------------------------------------------------

    @Test
    fun `Store の Theme 変更で表示中の Root Header のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF33B24D)

        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.rootHeader = RootAccessory.Text("プロフィール")
        val store = SettingsRootStore(
            initialRoot = singleCellRoot(),
            initialTheme = Theme(headerTextColor = initial),
        )
        view.bind(store)
        awaitConvergence(view) { committedTexts(view) == listOf("A") }
        activity.layoutSettingsView(view)

        assertEquals(
            "前提: Store の初期 Theme の headerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )

        val updatedTheme = Theme(headerTextColor = updated)
        store.applyTheme(updatedTheme)
        awaitConvergence(view, extraDiagnostics = { "Theme: ${view.internalTheme()}" }) {
            view.internalTheme() == updatedTheme
        }
        activity.settle(view)

        assertEquals(
            "Store 経由の Theme 変更でも Root Header の文字色は新しい Theme へ追従する",
            updated.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )
    }

    // ---------------------------------------------------------------------
    // bind による Store 初期 Theme の取り込み経由
    // ---------------------------------------------------------------------

    @Test
    fun `bind で初期 Theme が変わったとき表示中の Root Header のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF1933E6)
        val view = hostView(
            activity,
            rootHeader = RootAccessory.Text("プロフィール"),
            theme = Theme(headerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の headerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )

        // bind は Store の初期 Theme を内部値へ直接書き込む。この書き込みの後に Store の
        // theme StateFlow が同じ値を流しても theme setter の同値スキップに阻まれるため、
        // bind 自身が再 bind を促さないと表示は古い Theme のまま取り残される。
        view.bind(
            SettingsRootStore(
                initialRoot = singleCellRoot(),
                initialTheme = Theme(headerTextColor = updated),
            ),
        )
        activity.settle(view)

        assertEquals(
            "bind 後の Root Header の文字色は Store の初期 Theme の headerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "プロフィール").currentTextColor,
        )
    }

    @Test
    fun `bind で初期 Theme が変わったとき表示中の Root Footer のテキスト色が追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF008040)
        val view = hostView(
            activity,
            rootFooter = RootAccessory.Text("v1.0.0"),
            theme = Theme(footerTextColor = initial),
        )

        assertEquals(
            "前提: 初期 Theme の footerTextColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "v1.0.0").currentTextColor,
        )

        view.bind(
            SettingsRootStore(
                initialRoot = singleCellRoot(),
                initialTheme = Theme(footerTextColor = updated),
            ),
        )
        activity.settle(view)

        assertEquals(
            "bind 後の Root Footer の文字色は Store の初期 Theme の footerTextColor になる",
            updated.toArgb(),
            textViewOf(view, "v1.0.0").currentTextColor,
        )
    }

    // ---------------------------------------------------------------------
    // View 形式は Theme 変更で中身を作り直さない
    // ---------------------------------------------------------------------

    @Test
    fun `theme 代入で View 形式の Root Header は factory が再実行されない`() {
        val activity = startActivity()
        var factoryCount = 0
        val view = hostView(
            activity,
            rootHeader = RootAccessory.View(
                KsAnyView.AndroidView { ctx ->
                    factoryCount += 1
                    TextView(ctx).apply { text = "View 形式ヘッダ" }
                },
            ),
            theme = Theme(headerTextColor = Color(0xFFE01919)),
        )
        assertEquals("前提: View 形式の Root Header が一度も生成されていない", 1, factoryCount)

        view.theme = Theme(headerTextColor = Color(0xFF1933E6))
        activity.settle(view)

        assertEquals(
            "View 形式の Root Header は Theme 変更で factory を再実行しない（内部状態を失うため）",
            1,
            factoryCount,
        )
    }

    @Test
    fun `theme 代入で View 形式の Root Footer は factory が再実行されない`() {
        val activity = startActivity()
        var factoryCount = 0
        val view = hostView(
            activity,
            rootFooter = RootAccessory.View(
                KsAnyView.AndroidView { ctx ->
                    factoryCount += 1
                    TextView(ctx).apply { text = "View 形式フッタ" }
                },
            ),
            theme = Theme(footerTextColor = Color(0xFFE01919)),
        )
        assertEquals("前提: View 形式の Root Footer が一度も生成されていない", 1, factoryCount)

        view.theme = Theme(footerTextColor = Color(0xFF008040))
        activity.settle(view)

        assertEquals(
            "View 形式の Root Footer は Theme 変更で factory を再実行しない（内部状態を失うため）",
            1,
            factoryCount,
        )
    }

    @Test
    fun `theme 代入を挟んでも View 形式の Root Header の入力途中テキストが残る`() {
        val activity = startActivity()
        val view = hostView(
            activity,
            rootHeader = RootAccessory.View(
                KsAnyView.AndroidView { ctx -> EditText(ctx) },
            ),
            theme = Theme(headerTextColor = Color(0xFFE01919)),
        )

        // 利用者が入力した「作り直すと失われる内部状態」を作る。
        editTextIn(view).setText("入力途中")
        activity.settle(view)
        assertEquals("前提: 入力が反映されていない", "入力途中", editTextIn(view).text.toString())

        view.theme = Theme(headerTextColor = Color(0xFF1933E6))
        activity.settle(view)

        assertEquals(
            "Theme 変更を挟んでも View 形式 Root Header の入力途中テキストは保持される",
            "入力途中",
            editTextIn(view).text.toString(),
        )
    }

    @Test
    fun `View 形式の Root Header は別インスタンスへの差し替えで作り直される`() {
        val activity = startActivity()
        var factoryCount = 0
        val view = hostView(
            activity,
            rootHeader = RootAccessory.View(
                KsAnyView.AndroidView { ctx ->
                    factoryCount += 1
                    TextView(ctx).apply { text = "View 形式ヘッダ" }
                },
            ),
        )
        assertEquals("前提: View 形式の Root Header が一度も生成されていない", 1, factoryCount)

        // Theme ではなく中身そのものの差し替え。こちらは従来どおり作り直す。
        view.rootHeader = RootAccessory.View(
            KsAnyView.AndroidView { ctx ->
                factoryCount += 1
                TextView(ctx).apply { text = "差し替え後ヘッダ" }
            },
        )
        activity.settle(view)

        assertEquals("中身の差し替えでは View 形式の Root Header を作り直す", 2, factoryCount)
        assertEquals("差し替え後の中身が表示される", "差し替え後ヘッダ", textViewOf(view, "差し替え後ヘッダ").text.toString())
    }

    @Test
    fun `bind で初期 Theme が変わったとき表示中の Cell のタイトル色も追従する`() {
        val activity = startActivity()
        val initial = Color(0xFFE01919)
        val updated = Color(0xFF1933E6)
        val view = hostView(activity, theme = Theme(cellTitleColor = initial))

        assertEquals(
            "前提: 初期 Theme の cellTitleColor が反映されていない",
            initial.toArgb(),
            textViewOf(view, "A").currentTextColor,
        )

        view.bind(
            SettingsRootStore(
                initialRoot = singleCellRoot(),
                initialTheme = Theme(cellTitleColor = updated),
            ),
        )
        activity.settle(view)

        assertEquals(
            "bind 後の Cell タイトル色は Store の初期 Theme の cellTitleColor になる",
            updated.toArgb(),
            textViewOf(view, "A").currentTextColor,
        )
    }
}
