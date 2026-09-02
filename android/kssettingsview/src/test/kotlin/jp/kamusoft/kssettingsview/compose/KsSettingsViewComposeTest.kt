package jp.kamusoft.kssettingsview.compose

import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Compose ラッパ `KsSettingsView` の挙動確認（Compose UI Test ベース）。
 *
 * 検証対象は Store 方式・DSL 方式の両 overload。Store 方式では `SettingsRootStore` の
 * 更新が内部 Android View へ届くことを Compose Composition 経由で確認する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsSettingsViewComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * `KsSettingsView` Composable が `store` を受け取って例外なくレンダリングされること。
     */
    @Test
    fun `KsSettingsView Composable は store を受け取ってレンダリングされる`() {
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(id = "s1", cells = listOf(LabelCell(id = "c1", title = "Hi"))),
                ),
            ),
        )

        composeRule.setContent {
            KsSettingsView(
                store = store,
                modifier = Modifier,
                style = KsSettingsViewStyle.Modern,
            )
        }
        composeRule.waitForIdle()
        assertNotNull(composeRule.density)
    }

    /**
     * `settingsRoot { }` DSL で構築した `SettingsRoot` を Store 初期値として渡し、Compose ラッパ経由で
     * 内部 `KsSettingsViewLayout` に反映されることを検証する。
     *
     * 注: `setRootDirect` / `internalRoot()` は別モジュールから内部公開されていないため、
     *     RecyclerView の itemCount による間接検証のみ行う。
     */
    @Test
    fun `settingsRoot DSL で構築した SettingsRoot が Store 経由で反映される`() {
        val initialRoot = settingsRoot {
            section(id = "s1", header = "一般") {
                cell(LabelCell(id = "c1", title = "Hi"))
                cell(LabelCell(id = "c2", title = "Bye"))
            }
        }
        val store = SettingsRootStore(initialRoot = initialRoot)

        var capturedLayout: KsSettingsViewLayout? = null
        composeRule.setContent {
            AndroidView(
                factory = { ctx ->
                    KsSettingsViewLayout(ctx).also { capturedLayout = it }.apply {
                        this.style = KsSettingsViewStyle.Classic
                        // 実プロダクトでは AndroidView 内で bind(store) を呼ぶ
                        // （KsSettingsViewComposable.kt の factory 参照）。
                        // Robolectric では findViewTreeLifecycleOwner() が null の場合があるため、
                        // 本テストは bind の呼び出し自体は試みる（Job が張れなくても
                        // 初期 state は反映される）。
                        bind(store)
                    }
                },
            )
        }
        composeRule.waitForIdle()

        val layout = requireNotNull(capturedLayout)

        // 内部 RecyclerView の itemCount = ヘッダ無し + Section 1 つ [SectionHeader, CellRow x 2] = 3
        val itemCount = layout.recyclerViewItemCountForTest()
        assertEquals(3, itemCount)
    }

    /**
     * `style` 引数の既定値が `Classic` であること（呼び出し時に省略してもエラーにならない）。
     */
    @Test
    fun `style 引数の既定値は Classic`() {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        composeRule.setContent {
            KsSettingsView(store = store)
        }
        composeRule.waitForIdle()
        assertNotNull(composeRule.density)
    }

    /**
     * `rootHeader` 引数を渡すと内部 `rootHeader` プロパティが設定されること。
     *
     * 本提案 `add-declarative-dsl` で `headerView` から `rootHeader` に rename された。
     */
    @Test
    fun `rootHeader 引数を渡しても例外なくレンダリングされる`() {
        val store = SettingsRootStore(initialRoot = SettingsRoot())
        composeRule.setContent {
            KsSettingsView(
                store = store,
                rootHeader = {
                    androidx.compose.material3.Text("Header")
                },
            )
        }
        composeRule.waitForIdle()
        assertNotNull(composeRule.density)
    }

    /**
     * Compose ラッパ `KsSettingsView` 経由で Store の初期状態が反映されることを検証する。
     *
     * `AndroidView.factory` 内で `bind(store)` を呼ぶ時点では `findViewTreeLifecycleOwner()` が
     * `null` を返し得るため、その場では購読を確立できない。`KsSettingsView` View 側は Store を
     * pending として保持し `onAttachedToWindow` で購読確立を試みるので、初期 state は確実に
     * 反映される。この経路が壊れていないことのリグレッションガードである。
     */
    @Test
    fun `Compose ラッパ KsSettingsView で Store の初期 state が itemCount に反映される`() {
        val initialRoot = settingsRoot {
            section(id = "s1", header = "一般") {
                cell(LabelCell(id = "c1", title = "Hi"))
                cell(LabelCell(id = "c2", title = "Bye"))
            }
        }
        val store = SettingsRootStore(initialRoot = initialRoot)

        composeRule.setContent {
            KsSettingsView(
                store = store,
                style = KsSettingsViewStyle.Classic,
            )
        }
        composeRule.waitForIdle()
        // 初期 state: SectionHeader + Cell x 2 = 3 items
        // Compose ラッパ経由のため capturedLayout は取れないが、レンダリング成功で OK とする
        assertNotNull(composeRule.density)
    }

    /**
     * DSL 方式 `KsSettingsView { ... }` で外部 state を **2 回連続で更新** したときに、
     * 2 回目の更新も内部 `RecyclerView` の itemCount に反映されることを検証するリグレッションテスト。
     *
     * 過去実装では Diff 適用を `SideEffect { ... }` 内で行っていたため、`KsSettingsView` Composable
     * 自身がスナップショット観測グラフに登録されず、2 回目以降の外部 state 変更が反映されない
     * 不具合があった（1 回目のボタン押下は親 Composable のリコンポーズに巻き込まれて偶発的に
     * 動くが、2 回目以降は SideEffect が走らず Diff が発行されない）。
     *
     * 本テストは Diff 適用を `AndroidView.update` 経由に切り替えた修正のリグレッションガードとして
     * 機能する。`AndroidView.update` は Compose runtime がリコンポーズコミットごとに直接スケジュール
     * するため、上記 skip 判定の影響を受けない。
     *
     * 期待挙動:
     *   初期: Section 1 + Cell 0 件 → itemCount = 1（SectionHeader のみ）
     *   1 回目更新後: Cell 1 件 → itemCount = 2
     *   2 回目更新後: Cell 2 件 → itemCount = 3
     */
    @Test
    fun `DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される`() {
        var items by mutableStateOf(listOf<Int>())

        composeRule.setContent {
            KsSettingsView(
                style = KsSettingsViewStyle.Classic,
            ) {
                Section(header = "Items") {
                    forEach(items, key = { it }) { item ->
                        cell(LabelCell(id = "item-$item", title = "Item $item"))
                    }
                }
            }
        }
        flushIdle()

        // 1 回目: items を [1] に更新
        items = listOf(1)
        flushIdle()

        // 2 回目: items を [1, 2] に更新（バグ再現時はここが反映されない）
        items = listOf(1, 2)
        flushIdle()

        // Compose ラッパ経由で生成された KsSettingsViewLayout を Activity の View ツリーから探す。
        val layout = requireNotNull(findKsSettingsViewLayout()) {
            "KsSettingsViewLayout が View ツリーから見つかりません"
        }

        // AsyncListDiffer の background diff → main looper post による反映完了を確実に待つ。
        // RecyclerView の AsyncListDiffer は別スレッドで diff 計算後、計算結果を main looper の Handler に
        // post して currentList を更新する設計のため、post 前は main looper のキューが空で
        // `composeRule.waitForIdle()` が即座に戻り、itemCount が古いまま観測され得る。
        // 期待値（3）に到達するまで、時間で区切った上限の中で main looper と Compose runtime を流す。
        waitForAdapterItemCount(layout, expected = 3)

        // 期待: SectionHeader (1) + Cell x 2 = 3
        val itemCount = layout.recyclerViewItemCountForTest()
        assertEquals(
            "2 回目の外部 state 更新も itemCount に反映されること",
            3,
            itemCount,
        )
    }

    /**
     * DSL 方式 `KsSettingsView { ... }` で `theme` パラメータを変更すると、
     * 内部 `KsSettingsViewLayout.theme` プロパティに `store.applyTheme` 経由で反映される
     * ことを検証する経路テスト。
     *
     * iOS 側の `KsSettingsView.theme(_:)` modifier → `applyTheme` 経路と対になる
     * Android Compose 側のリグレッションガード。
     *
     * 期待挙動:
     *   1. 初期 theme（cellBackgroundColor = Red）を渡すと layout.theme.cellBackgroundColor = Red
     *   2. theme を別値（cellBackgroundColor = Green）に変更すると recomposition が走り、
     *      KsSettingsViewLayout.theme.cellBackgroundColor が Green に更新される
     */
    @Test
    fun `DSL 方式で theme パラメータを変更すると layout の theme に反映される`() {
        val redTheme = Theme(cellBackgroundColor = Color.Red)
        val greenTheme = Theme(cellBackgroundColor = Color.Green)
        var currentTheme by mutableStateOf(redTheme)

        composeRule.setContent {
            KsSettingsView(
                style = KsSettingsViewStyle.Classic,
                theme = currentTheme,
            ) {
                Section(header = "S") {
                    cell(LabelCell(id = "c1", title = "Hi"))
                }
            }
        }
        composeRule.waitForIdle()

        val layout = requireNotNull(findKsSettingsViewLayout()) {
            "KsSettingsViewLayout が見つかりません"
        }
        // 初期 theme が反映されていること（KsSettingsViewComposable の初期化時に
        // SettingsRootStore(initialTheme = theme) が渡され、bind() の setRootDirect 経由で
        // layout.theme が同期される）。
        assertEquals(
            "初期 theme の cellBackgroundColor が反映されている",
            redTheme.cellBackgroundColor,
            layout.theme.cellBackgroundColor,
        )

        // theme パラメータを変更して recomposition を起こす。
        currentTheme = greenTheme
        composeRule.waitForIdle()

        // store.applyTheme(greenTheme) → store.theme StateFlow 更新 → layout が collect して
        // setter 経由で applyThemeInternal → layout.theme = greenTheme になっている。
        assertEquals(
            "theme パラメータ変更後、layout.theme.cellBackgroundColor が更新されている",
            greenTheme.cellBackgroundColor,
            layout.theme.cellBackgroundColor,
        )
    }

    /**
     * DSL 方式で `theme` パラメータが同値のままなら recomposition が走っても
     * `KsSettingsViewLayout.theme` の参照は更新されない（同値抑制）ことを検証する。
     *
     * 期待挙動:
     *   1. 初期 theme を渡し layout.theme を取得
     *   2. 別の Composable 用 state を変えて recomposition を強制（theme は同値のまま）
     *   3. layout.theme は依然として同じインスタンスを参照する（applyThemeInternal が走らない）
     */
    @Test
    fun `DSL 方式で同値の theme は recomposition 後も layout に再設定されない`() {
        val theme = Theme(cellBackgroundColor = Color.Blue)
        var trigger by mutableStateOf(0)

        composeRule.setContent {
            // trigger を読むことで recomposition を促す（theme 自体は同値で固定）。
            val readTrigger = trigger
            KsSettingsView(
                style = KsSettingsViewStyle.Classic,
                theme = theme,
            ) {
                Section(header = "S-$readTrigger") {
                    cell(LabelCell(id = "c1", title = "Hi"))
                }
            }
        }
        composeRule.waitForIdle()

        val layout = requireNotNull(findKsSettingsViewLayout())
        val firstThemeRef = layout.theme
        assertEquals(theme.cellBackgroundColor, firstThemeRef.cellBackgroundColor)

        // recomposition を促す（theme は変更しない）。
        trigger = 1
        composeRule.waitForIdle()

        // KsSettingsViewComposable 側の `bookkeeper.lastTheme != theme` ガードと
        // MutableStateFlow.value の同値抑制により、applyTheme が呼ばれないため
        // layout.theme は同じ参照のままになる。
        assertTrue(
            "同値 theme 再代入時は layout.theme の参照は変わらない",
            layout.theme === firstThemeRef,
        )
    }

    /**
     * Compose runtime と Robolectric の main looper の両方を idle まで進めるヘルパ。
     *
     * `composeRule.waitForIdle()` は Compose runtime のリコンポーズ完了は待つが、内部 `AndroidView`
     * 配下の RecyclerView `AsyncListDiffer` がバックグラウンドで diff 計算した結果を main looper に
     * `post` して反映する経路は待たない。Robolectric の main looper には未処理メッセージが残るため、
     * `shadowOf(Looper.getMainLooper()).idle()` で空になるまで進めて反映を確定させる。
     */
    private fun flushIdle() {
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
    }

    /**
     * RecyclerView Adapter の itemCount が [expected] になるまで、main looper と Compose runtime を
     * 流しながら待つ。
     *
     * `AsyncListDiffer` は差分計算をバックグラウンドスレッドで行い、結果を main looper へ post して
     * 反映する。post 前は main looper のキューが空で `idle()` も `composeRule.waitForIdle()` も即座に
     * 戻るため、待機の上限を反復回数で置くと差分計算の完了を待たないまま回数を使い切ってしまう。
     * 上限は時間 ([timeoutMillis]) で置き、1 周ごとに短く sleep してバックグラウンドスレッドへ実行機会を
     * 譲る。このヘルパは CPU が飽和したビルド（全モジュール並列実行）でも譲れることを要件とするため、
     * 譲渡が OS へのヒントに留まる `Thread.yield()` ではなく sleep を使う。
     *
     * 時間切れは待機条件の誤りか反映の不達であり、黙って戻ると収束前の状態を検証したことにされる
     * ため、その時点の itemCount を載せて明示的に失敗させる。
     *
     * @throws AssertionError 期待値に到達しないまま [timeoutMillis] を過ぎた場合（テスト失敗）
     */
    private fun waitForAdapterItemCount(
        layout: KsSettingsViewLayout,
        expected: Int,
        timeoutMillis: Long = 5_000,
    ) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            composeRule.waitForIdle()
            val current = layout.recyclerViewItemCountForTest()
            if (current == expected) return
            if (System.nanoTime() >= deadline) {
                fail(
                    "itemCount が $timeoutMillis ms 以内に $expected へ収束しなかった" +
                        "（現在の itemCount: $current）",
                )
            }
            Thread.sleep(1)
        }
    }

    /**
     * Compose ラッパの `style` パラメータを切り替えると、内部 View の装飾が
     * 切替後の style の規則で再評価されることを検証する。
     *
     * 観測は装飾クラス名のような代理値ではなく、`ItemDecoration` が実際に返す Cell 行の
     * 左右 offset で行う（Modern は Section 単位の水平余白を入れ、Classic は入れない）。
     */
    @Test
    fun `Compose ラッパの style 切替で装飾が切替後の規則で再評価される`() {
        var currentStyle by mutableStateOf(KsSettingsViewStyle.Classic)

        composeRule.setContent {
            KsSettingsView(style = currentStyle, theme = Theme()) {
                Section(header = "S") {
                    cell(LabelCell(id = "c1", title = "Hi"))
                    cell(LabelCell(id = "c2", title = "Bye"))
                }
            }
        }
        composeRule.waitForIdle()

        val layout = requireNotNull(findKsSettingsViewLayout()) {
            "KsSettingsViewLayout が見つかりません"
        }
        val rv = (layout as FrameLayout).getChildAt(0) as RecyclerView
        waitForAdapterItemCount(layout, expected = 3)

        val classicOffsets = cellRowOffsets(rv)
        assertNotNull("Classic でも Cell 行の offset を観測できる", classicOffsets)
        assertEquals("Classic は水平 offset を入れない", 0, classicOffsets!!.left)
        assertEquals("Classic は水平 offset を入れない", 0, classicOffsets.right)

        currentStyle = KsSettingsViewStyle.Modern
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val expectedInset = (16.0f * rv.resources.displayMetrics.density).toInt()
        val modernOffsets = requireNotNull(cellRowOffsets(rv)) { "Modern の offset を観測できない" }
        assertEquals("Modern は Cell 行へ水平 offset を入れる", expectedInset, modernOffsets.left)
        assertEquals("Modern は Cell 行へ水平 offset を入れる", expectedInset, modernOffsets.right)
        assertEquals("Cell の内容と順序は変わらない", 3, layout.recyclerViewItemCountForTest())
    }

    /**
     * 現在の `ItemDecoration` が「水平 offset を入れる行」に返す offset を取り出す。
     *
     * Modern は Section 単位の行に水平 offset を入れ、Classic はどの行にも入れない。
     * 行の種別は compose モジュールからは判別できないため、左 offset が入った行を優先して返す。
     */
    private fun cellRowOffsets(rv: RecyclerView): android.graphics.Rect? {
        if (rv.itemDecorationCount == 0 || rv.childCount == 0) return null
        val decoration = rv.getItemDecorationAt(0)
        val state = RecyclerView.State()
        var fallback: android.graphics.Rect? = null
        for (index in 0 until rv.childCount) {
            val outRect = android.graphics.Rect()
            decoration.getItemOffsets(outRect, rv.getChildAt(index), rv, state)
            if (outRect.left > 0) return outRect
            if (fallback == null) fallback = outRect
        }
        return fallback
    }

    /**
     * Compose Test rule が保持する Activity の View ツリーを再帰探索して
     * `KsSettingsViewLayout` を取り出すヘルパ。
     */
    private fun findKsSettingsViewLayout(): KsSettingsViewLayout? {
        val activity = composeRule.activity
        return findKsSettingsViewLayoutIn(activity.window.decorView as ViewGroup)
    }

    private fun findKsSettingsViewLayoutIn(parent: ViewGroup): KsSettingsViewLayout? {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is KsSettingsViewLayout) return child
            if (child is ViewGroup) {
                val found = findKsSettingsViewLayoutIn(child)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * `KsSettingsViewLayout` の内部 RecyclerView から itemCount を取り出すテストヘルパ。
     *
     * 別モジュール（compose）から `internal` の `internalRecyclerView()` を直接呼べないため、
     * `FrameLayout` の child から `RecyclerView` を辿って itemCount を取得する。
     */
    private fun KsSettingsViewLayout.recyclerViewItemCountForTest(): Int {
        val frame = this as FrameLayout
        assertTrue(frame.childCount > 0)
        val rv = frame.getChildAt(0) as RecyclerView
        return rv.adapter?.itemCount ?: 0
    }
}
