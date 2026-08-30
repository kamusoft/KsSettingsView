package jp.kamusoft.kssettingsview.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SettingsRoot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * 書き戻しラウンドトリップが入力より遅れて届いても、入力中の [EntryCell] の入力欄を巻き戻さない
 * ことを検証する。
 *
 * 打鍵は `TextWatcher` から利用側へ通知され、利用側の書き戻しが内容更新として同じ Cell へ戻って
 * くる。この往復は 1 フレーム前後遅れるため、往復より速い入力では「入力欄の方が先に進んでいる」
 * 状態で古い値の再バインドが届く。無条件に反映すると確定済みの打鍵が巻き戻り、文字の欠落・
 * 並び替えとキャレット移動になる。
 *
 * 検証は Store → Host → RecyclerView の実経路で行い、遅れて届く再バインドは
 * `store.replaceCell` を打鍵の後に明示的に流すことで決定論的に再現する。入力欄のフォーカスと
 * キー入力を実物で扱うため、Host は Activity へ載せてレイアウトまで済ませる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EntryCellWriteBackGuardTest {

    /** [KsSettingsView] を載せる器だけを持つホスト Activity。 */
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

    // MARK: - 実経路の足場

    /**
     * EntryCell 1 行だけの設定画面を Activity 上に組み立て、行の ViewHolder まで取り出す。
     *
     * 通知された text は [Harness.notified] へ順に積む。利用側の書き戻しはここでは行わず、
     * テストが `deliverContentUpdate` を呼んだ時点を「往復が届いた時点」として扱う。
     */
    private fun startHarness(cell: EntryCell): Harness {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()

        val notified = mutableListOf<String>()
        val store = SettingsRootStore(
            initialRoot = SettingsRoot(
                sections = listOf(
                    Section(id = "s1", cells = listOf(cell.copy(onTextChanged = { notified += it }))),
                ),
            ),
        )
        val view = KsSettingsView(activity)
        activity.container.addView(view)
        view.bind(store)
        idle()

        val harness = Harness(activity, view, store, notified)
        harness.layout()
        return harness
    }

    /** [startHarness] が組み立てた設定画面と、通知の記録。 */
    private inner class Harness(
        val activity: HostActivity,
        val view: KsSettingsView,
        val store: SettingsRootStore,
        val notified: MutableList<String>,
    ) {
        /** レイアウトを走らせ、RecyclerView に行を生成・再バインドさせる。 */
        fun layout() {
            val metrics = activity.resources.displayMetrics
            view.measure(
                View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, metrics.widthPixels, metrics.heightPixels)
        }

        /** 表示中の EntryCell 行の ViewHolder。 */
        val holder: EntryCellViewHolder
            get() {
                val rv = view.internalRecyclerView()
                val found = rv.findViewHolderForAdapterPosition(0)
                assertTrue(
                    "EntryCell 行の ViewHolder が生成されていない (実際: $found)",
                    found is EntryCellViewHolder,
                )
                return found as EntryCellViewHolder
            }

        /** 入力欄の現在値。 */
        val editorText: String
            get() = holder.editText.text?.toString() ?: ""

        /** 入力欄のキャレット位置。 */
        val caret: Int
            get() = holder.editText.selectionStart

        /** 入力欄へフォーカスを与える。 */
        fun focusEditor() {
            assertTrue("入力欄がフォーカスを取得できない", holder.editText.requestFocus())
            assertTrue("入力欄がフォーカスを持っていない", holder.editText.isFocused)
        }

        /** キーイベントとして [text] を 1 文字ずつ打鍵する（キャレット位置に挿入される）。 */
        fun type(text: String) {
            text.forEach { ch ->
                val keyCode = KeyEvent.KEYCODE_A + (ch - 'a')
                holder.editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                holder.editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }

        /**
         * 書き戻しの往復が届いた状態を作る（同一 id の Cell を [text] で置き換える）。
         *
         * Store → Host → `notifyItemChanged` → 再バインドの実経路を通す。`submitList` の差分計算は
         * バックグラウンドで走るため、コミット完了を待ってからレイアウトを走らせて再バインドさせる。
         */
        fun deliverContentUpdate(cellId: String, text: String, mutate: (EntryCell) -> EntryCell = { it }) {
            val current = cellInStore(cellId)
            val updated = mutate(current.copy(text = text))
            store.replaceCell(cellId, updated)
            awaitDifferCommit({ committedSummary() }) { committedCell(cellId) == updated }
            layout()
        }

        /** Store が保持している [cellId] の EntryCell。 */
        fun cellInStore(cellId: String): EntryCell =
            store.state.value.sections
                .flatMap { it.cells }
                .filterIsInstance<EntryCell>()
                .first { it.id == cellId }

        /** Adapter がコミット済みの平坦リストにある [cellId] の EntryCell。 */
        fun committedCell(cellId: String): EntryCell? =
            view.internalMainListAdapter().currentList
                .filterIsInstance<CellListItem.CellRow>()
                .map { it.cell }
                .filterIsInstance<EntryCell>()
                .firstOrNull { it.id == cellId }

        /** コミット済みの平坦リストを、失敗メッセージ用に要約する。 */
        fun committedSummary(): List<String> =
            view.internalMainListAdapter().currentList.map { item ->
                when (item) {
                    is CellListItem.CellRow -> "cell(${(item.cell as? EntryCell)?.text})"
                    is CellListItem.SectionHeader -> "header"
                    is CellListItem.SectionFooter -> "footer"
                }
            }
    }

    // MARK: - フォーカス中の上書き抑止

    @Test
    fun `往復より速い連続入力でも全文字が入力順どおり残る`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()

        // 打鍵ごとに遅れて届く往復を、常に 1 文字ぶん古い値で挟み込む。
        h.type("a")
        h.deliverContentUpdate("c1", "Tanaka")
        h.type("b")
        h.deliverContentUpdate("c1", "Tanakaa")
        h.type("c")
        h.deliverContentUpdate("c1", "Tanakaab")
        h.type("d")
        h.type("e")
        h.deliverContentUpdate("c1", "Tanakaabc")

        assertEquals("Tanakaabcde", h.editorText)
        assertEquals("キャレットが旧末尾へ巻き戻らない", "Tanakaabcde".length, h.caret)
    }

    @Test
    fun `フォーカス中の同一 Cell へのプログラム的更新は入力欄を上書きしない`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()
        h.type("ab")
        val caretBefore = h.caret

        h.deliverContentUpdate("c1", "別の値")

        assertEquals("Tanakaab", h.editorText)
        assertEquals(caretBefore, h.caret)
    }

    // MARK: - 入力継続性

    @Test
    fun `遅れて届く再バインドを繰り返した後も入力を受け付け続ける`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()

        repeat(3) {
            h.type("ab")
            h.deliverContentUpdate("c1", "Tanaka")
        }
        h.type("cd")

        assertEquals("Tanakaabababcd", h.editorText)
    }

    @Test
    fun `フォーカス中の再バインドは IME の未確定文字列を維持する`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()
        val connection = requireNotNull(h.holder.editText.onCreateInputConnection(EditorInfo())) {
            "入力欄から InputConnection を取得できない"
        }
        connection.setComposingText("かん", 1)
        assertEquals("Tanakaかん", h.editorText)

        h.deliverContentUpdate("c1", "Tanaka")

        val editable = requireNotNull(h.holder.editText.text)
        assertEquals("Tanakaかん", editable.toString())
        // 未確定区間（composing span）が残っていることが「変換操作が続けられる」条件。
        // setText はこの span を落とすため、範囲が消えていれば変換が確定・破棄されている。
        assertEquals(6, BaseInputConnection.getComposingSpanStart(editable))
        assertEquals(8, BaseInputConnection.getComposingSpanEnd(editable))
    }

    // MARK: - フォーカス喪失時の再同期

    @Test
    fun `フォーカス喪失で保留されていたプログラム的更新が反映され通知は発火しない`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()
        h.type("ab")
        h.deliverContentUpdate("c1", "正規化済み")
        assertEquals("フォーカス中は保留される", "Tanakaab", h.editorText)

        val notifiedBeforeBlur = h.notified.toList()
        h.holder.editText.clearFocus()

        assertEquals("正規化済み", h.editorText)
        assertEquals(
            "再同期は書き戻し経路へ逆流しない",
            notifiedBeforeBlur,
            h.notified,
        )
    }

    @Test
    fun `フォーカス喪失直前の入力は静穏化後の表示と通知の双方に残る`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()

        // 打鍵の書き戻しがまだ届いていない状態（最後にバインドされた text は "Tanaka" のまま）。
        h.type("ab")
        assertEquals(listOf("Tanakaa", "Tanakaab"), h.notified)

        h.holder.editText.clearFocus()
        // 未完了の往復がこの後で配信され、静穏化する。
        h.deliverContentUpdate("c1", "Tanakaab")

        assertEquals("Tanakaab", h.editorText)
        assertEquals(
            "blur 時の再同期が古い値をアプリ状態へ書き戻さない",
            listOf("Tanakaa", "Tanakaab"),
            h.notified,
        )
    }

    // MARK: - 非フォーカス時と別 Cell の反映

    @Test
    fun `フォーカスを持たない入力欄には内容更新がそのまま反映される`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        assertEquals("Tanaka", h.editorText)

        h.deliverContentUpdate("c1", "Suzuki")

        assertEquals("Suzuki", h.editorText)
    }

    // MARK: - Cell 同一性の判別

    /**
     * ViewHolder 1 つを Activity 上へ載せ、レイアウトまで済ませる。
     *
     * リサイクルによる別 Cell への再バインドと [EntryCellViewHolder.reset] は行の入れ替わりを
     * 伴うため、Store 経路では狙った順序で起こせない。ここでは ViewHolder へ直接 bind するが、
     * フォーカスと打鍵は実物を使う（フォーカスの取得には window に載った実レイアウトが要る）。
     */
    private fun startRecycledHolder(cell: EntryCell): EntryCellViewHolder {
        val ctrl = Robolectric.buildActivity(HostActivity::class.java).setup()
        controller = ctrl
        val activity = ctrl.get()
        val holder = EntryCellViewHolder.create(activity.container)
        activity.container.addView(holder.views.root)
        holder.bind(cell, Theme())
        activity.container.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
        )
        activity.container.layout(0, 0, 1000, 2000)
        return holder
    }

    /** キーイベントとして [text] を 1 文字ずつ打鍵する。 */
    private fun EntryCellViewHolder.type(text: String) {
        text.forEach { ch ->
            val keyCode = KeyEvent.KEYCODE_A + (ch - 'a')
            editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    @Test
    fun `同一 id で text だけが違う再バインドはフォーカス中の入力欄を上書きしない`() {
        val holder = startRecycledHolder(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        assertTrue(holder.editText.requestFocus())
        holder.type("ab")

        // Cell の等価性は text を含むため、equals や参照比較で同一性を判定するとこの再バインドが
        // 「別 Cell」になり、入力欄が巻き戻る。
        holder.bind(EntryCell(id = "c1", title = "名前", text = "Suzuki"), Theme())

        assertEquals("Tanakaab", holder.editText.text?.toString())
    }

    @Test
    fun `別 id への再バインドは text が同じでも新しい Cell として反映される`() {
        val holder = startRecycledHolder(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        assertTrue(holder.editText.requestFocus())
        holder.type("ab")

        // リサイクルで別 Cell を担当する。text は前 Cell のバインド値と偶然同じ。
        holder.bind(EntryCell(id = "c2", title = "会社", text = "Tanaka"), Theme())

        assertEquals("Tanaka", holder.editText.text?.toString())
    }

    @Test
    fun `reset 後の再利用では前 Cell の保持状態を持ち越さない`() {
        val holder = startRecycledHolder(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        assertTrue(holder.editText.requestFocus())
        holder.type("ab")

        holder.reset()
        // reset がフォーカス中の入力欄を空にするため、前 Cell の同一性判定が残っていると
        // 同じ id への再バインドが抑止され、入力欄が空のまま取り残される。
        holder.bind(EntryCell(id = "c1", title = "名前", text = "Suzuki"), Theme())

        assertEquals("Suzuki", holder.editText.text?.toString())
    }

    // MARK: - フォーカス中のプロパティ変更

    @Test
    fun `フォーカス中の placeholder 変更は反映され text は保たれる`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka", placeholder = "未入力"))
        h.focusEditor()
        h.type("ab")

        h.deliverContentUpdate("c1", "Tanaka") { it.copy(placeholder = "氏名を入力") }

        assertEquals("氏名を入力", h.holder.editText.hint?.toString())
        assertEquals("Tanakaab", h.editorText)
    }

    @Test
    fun `フォーカス中の無効化は編集を終了させ直前の入力を静穏化後に残す`() {
        val h = startHarness(EntryCell(id = "c1", title = "名前", text = "Tanaka"))
        h.focusEditor()
        h.type("ab")
        assertEquals(listOf("Tanakaa", "Tanakaab"), h.notified)

        // 書き戻しの往復が未完了のまま無効化が届く。
        h.deliverContentUpdate("c1", "Tanaka") { it.copy(isEnabled = false) }

        assertTrue("無効化で編集が終了する", !h.holder.editText.isFocused)
        assertTrue("入力欄が無効になる", !h.holder.editText.isEnabled)

        // 未完了の往復が配信されて静穏化する。
        h.deliverContentUpdate("c1", "Tanakaab") { it.copy(isEnabled = false) }

        assertEquals("Tanakaab", h.editorText)
        assertEquals(
            "無効化に伴う再同期が古い値を書き戻さない",
            listOf("Tanakaa", "Tanakaab"),
            h.notified,
        )
    }
}
