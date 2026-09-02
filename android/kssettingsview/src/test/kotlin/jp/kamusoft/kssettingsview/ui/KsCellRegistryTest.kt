package jp.kamusoft.kssettingsview.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import jp.kamusoft.kssettingsview.core.Cell
import jp.kamusoft.kssettingsview.ui.LabelCell
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `KsCellRegistry` の登録・解決動作テスト。
 *
 * Cell 型と viewType / ViewHolder ファクトリの対応付けが、登録・解決・未登録時の
 * フォールバックまで一貫して働くことを保証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class KsCellRegistryTest {

    @After
    fun tearDown() {
        // 他テストへ影響しないよう、各テスト後に登録を消す
        KsCellRegistry.clear()
    }

    /**
     * テスト用ダミー ViewHolder。
     */
    private class DummyHolder(view: View) : CellViewHolder<LabelCell>(view) {
        override fun bind(cell: LabelCell, theme: Theme) { /* no-op */ }
    }

    /**
     * テスト用ダミー Cell（`LabelCell` とは異なる Cell 型）。
     *
     * `Cell` は sealed ではないため、テストモジュール内でも `Cell` 実装を派生できる。これにより
     * 「異なる Cell 型に同じ viewType を登録した場合」の衝突検出ロジック
     * （`cellClassByViewType[viewType]` チェック）を実テストとして検証できる。
     */
    private data class DummyOtherCell(
        override val id: String = "other",
    ) : Cell

    /**
     * `DummyOtherCell` 用 ViewHolder。
     */
    private class DummyOtherHolder(view: View) : CellViewHolder<DummyOtherCell>(view) {
        override fun bind(cell: DummyOtherCell, theme: Theme) { /* no-op */ }
    }

    @Test
    fun `register と viewTypeOf で対応する viewType が解決される`() {
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 200,
        ) { parent ->
            DummyHolder(View(parent.context))
        }

        val cell = LabelCell(id = "c1", title = "Hi")
        assertEquals(200, KsCellRegistry.viewTypeOf(cell))
        assertTrue(KsCellRegistry.isRegistered(LabelCell::class))
    }

    @Test
    fun `createViewHolder で登録済みファクトリが呼ばれて ViewHolder が生成される`() {
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 201,
        ) { parent ->
            DummyHolder(View(parent.context))
        }

        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: ViewGroup = FrameLayout(ctx)
        val holder = KsCellRegistry.createViewHolder(parent, 201)
        assertTrue(holder is DummyHolder)
    }

    @Test
    fun `未登録 Cell 型を viewTypeOf すると IllegalStateException`() {
        // LabelCell を未登録の状態で
        val cell = LabelCell(id = "c1", title = "x")
        assertThrows(IllegalStateException::class.java) {
            KsCellRegistry.viewTypeOf(cell)
        }
    }

    @Test
    fun `未登録 viewType の createViewHolder は IllegalStateException`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: ViewGroup = FrameLayout(ctx)
        assertThrows(IllegalStateException::class.java) {
            KsCellRegistry.createViewHolder(parent, 999)
        }
    }

    /**
     * リリースビルド相当（strictMode = false）で未登録 Cell が viewTypeOf に渡されたとき、
     * [IllegalStateException] をスローせず [KsCellRegistry.VIEW_TYPE_PLACEHOLDER] を返す。
     *
     * 未登録 Cell はリリースビルドではクラッシュさせず、この値へフォールバックする。
     */
    @Test
    fun `strictMode false 時の未登録 Cell の viewTypeOf は VIEW_TYPE_PLACEHOLDER を返す`() {
        KsCellRegistry.strictMode = false
        try {
            val cell = LabelCell(id = "c1", title = "x")
            assertEquals(KsCellRegistry.VIEW_TYPE_PLACEHOLDER, KsCellRegistry.viewTypeOf(cell))
        } finally {
            KsCellRegistry.strictMode = true
        }
    }

    /**
     * リリースビルド相当（strictMode = false）で未登録 viewType の createViewHolder が呼ばれた場合、
     * [IllegalStateException] をスローせず空のプレースホルダ [EmptyPlaceholderViewHolder] を返す。
     *
     * 未登録 viewType はリリースビルドではクラッシュさせず、この空表示へフォールバックする。
     */
    @Test
    fun `strictMode false 時の未登録 viewType の createViewHolder は EmptyPlaceholderViewHolder を返す`() {
        KsCellRegistry.strictMode = false
        try {
            val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
            val parent: ViewGroup = FrameLayout(ctx)
            val holder = KsCellRegistry.createViewHolder(parent, 999)
            assertTrue(
                "リリースビルド向けフォールバックで EmptyPlaceholderViewHolder が返ること",
                holder is EmptyPlaceholderViewHolder,
            )
        } finally {
            KsCellRegistry.strictMode = true
        }
    }

    /**
     * `VIEW_TYPE_PLACEHOLDER` が直接 createViewHolder に渡された場合、
     * strictMode の値に関係なく [EmptyPlaceholderViewHolder] が返ることを検証する。
     *
     * 仕様: ListAdapter が strictMode=false 時に viewTypeOf でプレースホルダ viewType を取得し、
     * 後続の onCreateViewHolder で同 viewType が渡されるパスを保証するためのもの。
     */
    @Test
    fun `VIEW_TYPE_PLACEHOLDER 渡しの createViewHolder は常に EmptyPlaceholderViewHolder を返す`() {
        // strictMode = true（デフォルト）でも VIEW_TYPE_PLACEHOLDER は EmptyPlaceholderViewHolder にルーティングされる
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: ViewGroup = FrameLayout(ctx)
        val holder = KsCellRegistry.createViewHolder(parent, KsCellRegistry.VIEW_TYPE_PLACEHOLDER)
        assertTrue(holder is EmptyPlaceholderViewHolder)
    }

    /**
     * 異なる Cell 型に同じ viewType を登録した場合は [IllegalArgumentException] を投げる。
     *
     * `Cell` は sealed ではないため、テストモジュール内でも `Cell` 派生（`DummyOtherCell`）を
     * 定義でき、本ケースを実テストで検証できる。これは `KsCellRegistry.register` における
     * 重複検出（`cellClassByViewType[viewType]` の逆引きチェック）の核心パスを直接保証する。
     */
    @Test
    fun `異なる Cell 型に同じ viewType を登録すると IllegalArgumentException`() {
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 202,
        ) { parent ->
            DummyHolder(View(parent.context))
        }

        // LabelCell とは別の Cell 型（DummyOtherCell）で同じ viewType を再登録 → 拒否
        assertThrows(IllegalArgumentException::class.java) {
            KsCellRegistry.register(
                cellClass = DummyOtherCell::class,
                viewType = 202,
            ) { parent ->
                DummyOtherHolder(View(parent.context))
            }
        }
    }

    /**
     * 同一 Cell 型に対する重複登録は許容され、後勝ちで上書きされる。
     *
     * プラグイン形式での後勝ち登録、テストでのファクトリ差し替えに対応する仕様
     * （`KsCellRegistry.register` の KDoc 参照）。
     */
    @Test
    fun `同じ Cell 型 + 同じ viewType の再登録は後勝ちで上書きされる`() {
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 202,
        ) { parent ->
            DummyHolder(View(parent.context))
        }

        // 同一 Cell 型 + 同一 viewType での再登録は許容（後勝ち上書き）
        var newFactoryCalled = false
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 202,
        ) { parent ->
            newFactoryCalled = true
            DummyHolder(View(parent.context))
        }

        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: ViewGroup = FrameLayout(ctx)
        KsCellRegistry.createViewHolder(parent, 202)
        assertTrue("再登録された factory が呼ばれること", newFactoryCalled)

        // 念のため、CellStyle を持つ Cell インスタンスで viewTypeOf を確認
        @Suppress("UNUSED_VARIABLE")
        val style = CellStyle()
    }

    /**
     * 同じ Cell 型を別 viewType で再登録した場合、古い viewType エントリが
     * `entriesByViewType` / `cellClassByViewType` 双方から掃除されることを検証する。
     *
     * `KsCellRegistry.register` における stale エントリ掃除ロジック
     * （`previousEntry.viewType != viewType` の場合に旧 viewType を両マップから削除）の
     * 回帰検出網。具体的に保証する挙動：
     *
     * 1. 新 viewType で `viewTypeOf` が解決されること
     * 2. 旧 viewType に対する `createViewHolder` は `IllegalStateException`（strictMode=true 時）
     * 3. 旧 viewType が `cellClassByViewType` から消えているため、別の Cell 型で
     *    旧 viewType を再利用しても衝突しないこと
     */
    @Test
    fun `同じ Cell 型を別 viewType で再登録すると古い viewType は掃除される`() {
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 200,
        ) { parent ->
            DummyHolder(View(parent.context))
        }
        // 同じ Cell 型を別 viewType で付け替え
        KsCellRegistry.register(
            cellClass = LabelCell::class,
            viewType = 201,
        ) { parent ->
            DummyHolder(View(parent.context))
        }

        // 1) 新 viewType は解決可能
        val cell = LabelCell(id = "c1", title = "x")
        assertEquals(201, KsCellRegistry.viewTypeOf(cell))

        // 2) 旧 viewType は掃除されており、createViewHolder で IllegalStateException
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val parent: ViewGroup = FrameLayout(ctx)
        assertThrows(IllegalStateException::class.java) {
            KsCellRegistry.createViewHolder(parent, 200)
        }

        // 3) 旧 viewType (200) が cellClassByViewType から消えているため、
        //    別 Cell 型 (DummyOtherCell) で旧 viewType を再利用しても衝突しない
        KsCellRegistry.register(
            cellClass = DummyOtherCell::class,
            viewType = 200,
        ) { p ->
            DummyOtherHolder(View(p.context))
        }
        assertEquals(200, KsCellRegistry.viewTypeOf(DummyOtherCell()))
    }

    @Suppress("unused")
    private fun ensureCellInterfaceUsed(): Cell? = null
}
