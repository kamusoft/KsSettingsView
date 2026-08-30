package jp.kamusoft.kssettingsview.ui

import android.app.Activity
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * 入力系 Cell 5 種（[EntryCell] / [PickerCell] / [NumberPickerCell] / [TimePickerCell] /
 * [DatePickerCell]）の bind / 通知 / 再利用クリア / 共通フィールド表示を検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InputCellsTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )
    private val parent get() = FrameLayout(ctx)

    /** placeholder 色の検証に使う、ホスト既定と明確に異なる色。 */
    private val pinkColor = Color(0xFFFF2D87)

    /** 不透明な黒（ARGB）。ホストテーマ由来の色と区別するための比較値。 */
    private val blackArgb = 0xFF000000.toInt()

    @After
    fun tearDown() {
        // KsCellRegistry はテスト間で共有される可能性があるため、必要に応じて clear する。
        // ただし他テストへの影響を考慮し、ここでは明示的に基本 Cell の再登録を行わない。
    }

    // MARK: - id デフォルト値規約

    @Test
    fun `入力系 Cell 5 種の id デフォルト値規約`() {
        assertTrue(EntryCell(title = "x").id.startsWith("entry-cell-"))
        assertTrue(PickerCell(title = "x").id.startsWith("picker-cell-"))
        assertTrue(NumberPickerCell(title = "x").id.startsWith("number-picker-cell-"))
        assertTrue(TimePickerCell(title = "x").id.startsWith("time-picker-cell-"))
        assertTrue(DatePickerCell(title = "x").id.startsWith("date-picker-cell-"))
    }

    @Test
    fun `入力系 Cell は連続生成しても id が衝突しない`() {
        val a = EntryCell(title = "x")
        val b = EntryCell(title = "x")
        assertNotEquals(a.id, b.id)
    }

    // MARK: - VisibilityAware opt-in

    @Test
    fun `入力系 Cell は VisibilityAware に opt-in する`() {
        assertTrue((EntryCell(title = "x") as Any) is VisibilityAware)
        assertTrue((PickerCell(title = "x") as Any) is VisibilityAware)
        assertTrue((NumberPickerCell(title = "x") as Any) is VisibilityAware)
        assertTrue((TimePickerCell(title = "x") as Any) is VisibilityAware)
        assertTrue((DatePickerCell(title = "x") as Any) is VisibilityAware)
    }

    @Test
    fun `EntryCell の equals は onTextChanged を無視する`() {
        val a = EntryCell(id = "x", title = "T", onTextChanged = { /* a */ })
        val b = EntryCell(id = "x", title = "T", onTextChanged = { /* b */ })
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // MARK: - EntryCell ViewHolder

    @Test
    fun `EntryCellViewHolder bind で title と text が反映される`() {
        val vh = EntryCellViewHolder.create(parent)
        val cell = EntryCell(title = "名前", text = "Taro")
        vh.bind(cell, Theme())
        assertEquals("名前", vh.views.titleView.text?.toString())
        assertEquals("Taro", vh.editText.text?.toString())
    }

    @Test
    fun `EntryCell の keyboardType は Native Int を直接 EditText inputType に反映する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "電話", keyboardType = InputType.TYPE_CLASS_PHONE), Theme())
        assertEquals(InputType.TYPE_CLASS_PHONE, vh.editText.inputType)
    }

    @Test
    fun `EntryCell の isPassword は TYPE_TEXT_VARIATION_PASSWORD を設定する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "パスワード", isPassword = true), Theme())
        val expected =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertEquals(expected, vh.editText.inputType)
        assertTrue(vh.editText.transformationMethod is PasswordTransformationMethod)
    }

    @Test
    fun `EntryCell の isPassword は keyboardType と併用しても伏せ字になる`() {
        // keyboardType には Bridge が facade の keyboard 種別から写す値がそのまま入る。
        // variation を持つ種別と併用してもフレームワークのパスワード判定に一致することを確認する。
        val bases = listOf(
            "既定" to InputType.TYPE_CLASS_TEXT,
            "URL" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI),
            "メール" to (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS),
            "数値" to (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL),
            "電話番号" to InputType.TYPE_CLASS_PHONE,
        )

        for ((label, base) in bases) {
            val vh = EntryCellViewHolder.create(parent)
            vh.bind(
                EntryCell(title = "パスワード", keyboardType = base, isPassword = true),
                Theme(),
            )
            assertTrue(
                "$label キーボードとの併用で伏せ字にならない",
                vh.editText.transformationMethod is PasswordTransformationMethod,
            )
        }
    }

    @Test
    fun `EntryCell の isPassword は keyboardType の class に応じた variation を選ぶ`() {
        val vh = EntryCellViewHolder.create(parent)

        // 数値キーボードには数値用のパスワード variation を使う（小数許可フラグは残す）。
        vh.bind(
            EntryCell(
                title = "暗証番号",
                keyboardType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                isPassword = true,
            ),
            Theme(),
        )
        assertEquals(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            vh.editText.inputType,
        )

        // メールの variation は残さず置き換える（OR 合成では未定義の組み合わせになる）。
        val mailVh = EntryCellViewHolder.create(parent)
        mailVh.bind(
            EntryCell(
                title = "パスワード",
                keyboardType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                isPassword = true,
            ),
            Theme(),
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            mailVh.editText.inputType,
        )

        // 電話番号クラスにはパスワード variation が無いため、テキストクラスへ倒す。
        val phoneVh = EntryCellViewHolder.create(parent)
        phoneVh.bind(
            EntryCell(
                title = "パスワード",
                keyboardType = InputType.TYPE_CLASS_PHONE,
                isPassword = true,
            ),
            Theme(),
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
            phoneVh.editText.inputType,
        )
    }

    @Test
    fun `EntryCell の isPassword は複数行フラグを保ったまま伏せ字にする`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(
                title = "パスワード",
                keyboardType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                isPassword = true,
            ),
            Theme(),
        )
        assertEquals(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
            vh.editText.inputType,
        )
    }

    @Test
    fun `EntryCell の maxLength は EditText filters に LengthFilter を設定する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "x", maxLength = 5), Theme())
        assertEquals(1, vh.editText.filters.size)
        val filter = vh.editText.filters[0]
        assertTrue(filter is android.text.InputFilter.LengthFilter)
    }

    @Test
    fun `EntryCell maxLength = null のとき filters は空配列`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "x", maxLength = null), Theme())
        assertEquals(0, vh.editText.filters.size)
    }

    @Test
    fun `EntryCell の TextWatcher 経由で onTextChanged が呼ばれる`() {
        val vh = EntryCellViewHolder.create(parent)
        var received: String? = null
        vh.bind(
            EntryCell(
                title = "メモ",
                text = "",
                onTextChanged = { received = it },
            ),
            Theme(),
        )
        vh.simulateTextInput("hello")
        assertEquals("hello", received)
    }

    @Test
    fun `EntryCell reset で TextWatcher が解除され再利用時に旧 callback が呼ばれない`() {
        val vh = EntryCellViewHolder.create(parent)
        var oldCalls = 0
        vh.bind(EntryCell(title = "X", onTextChanged = { oldCalls++ }), Theme())
        vh.reset()
        // reset 後に text を変更しても旧 callback は呼ばれない
        vh.editText.setText("after-reset")
        assertEquals(0, oldCalls)
    }

    @Test
    fun `EntryCell isEnabled = false のとき EditText は disabled になる`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "x", isEnabled = false), Theme())
        assertFalse(vh.editText.isEnabled)
    }

    @Test
    fun `EntryCell の textAlignment END は EditText gravity を END に設定する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(title = "x", textAlignment = CellTitleAlignment.END),
            Theme(),
        )
        // END gravity + CENTER_VERTICAL を確認。
        assertTrue((vh.editText.gravity and android.view.Gravity.END) != 0)
    }

    // MARK: - EntryCell の placeholder 色

    @Test
    fun `EntryCell の placeholderColor が hint 色へ適用される`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(title = "メモ", text = "", placeholder = "入力してください", placeholderColor = pinkColor),
            Theme(),
        )

        assertEquals(pinkColor.toArgb(), vh.editText.currentHintTextColor)
    }

    @Test
    fun `EntryCell の placeholder 色は Cell 固有値 CellStyle Theme の順に解決する`() {
        val styleColor = Color(0xFF2E7D32)
        val themeColor = Color(0xFF1565C0)

        val fromCell = EntryCellViewHolder.create(parent)
        fromCell.bind(
            EntryCell(
                style = CellStyle(placeholderColor = styleColor),
                title = "メモ",
                placeholder = "p",
                placeholderColor = pinkColor,
            ),
            Theme(cellPlaceholderColor = themeColor),
        )
        assertEquals("Cell 固有値が最優先", pinkColor.toArgb(), fromCell.editText.currentHintTextColor)

        val fromStyle = EntryCellViewHolder.create(parent)
        fromStyle.bind(
            EntryCell(
                style = CellStyle(placeholderColor = styleColor),
                title = "メモ",
                placeholder = "p",
            ),
            Theme(cellPlaceholderColor = themeColor),
        )
        assertEquals(
            "Cell 固有値がなければ CellStyle",
            styleColor.toArgb(),
            fromStyle.editText.currentHintTextColor,
        )

        val fromTheme = EntryCellViewHolder.create(parent)
        fromTheme.bind(
            EntryCell(title = "メモ", placeholder = "p"),
            Theme(cellPlaceholderColor = themeColor),
        )
        assertEquals(
            "CellStyle もなければ Theme",
            themeColor.toArgb(),
            fromTheme.editText.currentHintTextColor,
        )
    }

    @Test
    fun `placeholder 色が全段未指定ならホストテーマの hint 色をそのまま使う`() {
        val vh = EntryCellViewHolder.create(parent)
        val hostDefault = vh.editText.hintTextColors

        vh.bind(EntryCell(title = "メモ", placeholder = "p"), Theme())

        // ライブラリ独自の既定色を持ち込まず、生成時のホスト既定 ColorStateList をそのまま維持する。
        assertSame(hostDefault, vh.editText.hintTextColors)
    }

    @Test
    fun `明示した placeholder 色は無効状態でも変わらない`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(title = "メモ", placeholder = "p", placeholderColor = pinkColor, isEnabled = false),
            Theme(disabledTextColor = Color(0xFF999999)),
        )

        assertFalse("無効状態でも hint 色は単色のまま", vh.editText.isEnabled)
        assertEquals(pinkColor.toArgb(), vh.editText.currentHintTextColor)
    }

    @Test
    fun `placeholder 色を明示から未指定へ戻すとホスト既定の hint 色へ復帰する`() {
        val vh = EntryCellViewHolder.create(parent)
        val hostDefault = vh.editText.hintTextColors

        vh.bind(
            EntryCell(id = "entry-1", title = "メモ", placeholder = "p", placeholderColor = pinkColor),
            Theme(),
        )
        assertEquals(pinkColor.toArgb(), vh.editText.currentHintTextColor)

        // 同一 id への内容更新で指定を外す（全段未指定になる）。
        vh.bind(EntryCell(id = "entry-1", title = "メモ", placeholder = "p"), Theme())

        assertSame(hostDefault, vh.editText.hintTextColors)
    }

    @Test
    fun `再利用行に placeholder 色が残らない`() {
        val vh = EntryCellViewHolder.create(parent)
        val hostDefault = vh.editText.hintTextColors

        vh.bind(
            EntryCell(id = "colored", title = "色つき", placeholder = "p", placeholderColor = pinkColor),
            Theme(),
        )
        assertEquals(pinkColor.toArgb(), vh.editText.currentHintTextColor)

        // 行の再利用は onViewRecycled → reset → 別 Cell の bind の順で起きる。
        vh.reset()
        vh.bind(EntryCell(id = "plain", title = "色なし", placeholder = "p"), Theme())

        assertSame(hostDefault, vh.editText.hintTextColors)
    }

    @Test
    fun `同一 Cell への再バインドで変化の無い placeholder 色を再適用しない`() {
        val vh = EntryCellViewHolder.create(parent)
        val cell =
            EntryCell(id = "entry-1", title = "メモ", placeholder = "p", placeholderColor = pinkColor)
        vh.bind(cell, Theme())
        assertEquals(pinkColor.toArgb(), vh.editText.currentHintTextColor)

        // 入力欄の hint 色を外から書き換えておく。再適用が走れば指定色で上書きされる。
        val sentinel = Color(0xFF00A0A0)
        vh.editText.setHintTextColor(sentinel.toArgb())

        // 同一 id・同一 placeholder 色のまま text だけ変わる再バインド（入力中に起きる経路）。
        vh.bind(cell.copy(text = "a"), Theme())

        assertEquals(
            "変化の無い placeholder 色は再適用しない",
            sentinel.toArgb(),
            vh.editText.currentHintTextColor,
        )
    }

    @Test
    fun `EntryCell は placeholderColor だけ異なると非同値`() {
        val base = EntryCell(id = "e", title = "x", placeholder = "p")
        val colored = EntryCell(id = "e", title = "x", placeholder = "p", placeholderColor = pinkColor)
        val other = EntryCell(
            id = "e",
            title = "x",
            placeholder = "p",
            placeholderColor = Color(0xFF2E7D32),
        )

        assertNotEquals(base, colored)
        assertNotEquals(colored, other)
        assertNotEquals(base.hashCode(), colored.hashCode())
    }

    // MARK: - EntryCell の入力文字色（valueText 解決）

    @Test
    fun `EntryCell の入力文字色は valueText の解決色を使う`() {
        val vh = EntryCellViewHolder.create(parent)
        val valueColor = Color(0xFF1565C0)
        vh.bind(
            EntryCell(title = "メモ", text = "入力済み"),
            Theme(cellTitleColor = Color(0xFFE01919), cellValueTextColor = valueColor),
        )

        assertEquals(valueColor.toArgb(), vh.editText.currentTextColor)
    }

    @Test
    fun `EntryCell の入力文字色は valueText 未指定なら title 色へ fallback する`() {
        val vh = EntryCellViewHolder.create(parent)
        val titleColor = Color(0xFFE01919)
        vh.bind(EntryCell(title = "メモ", text = "入力済み"), Theme(cellTitleColor = titleColor))

        assertEquals(titleColor.toArgb(), vh.editText.currentTextColor)
    }

    @Test
    fun `EntryCell の入力文字色は無効状態で disabledTextColor が優先される`() {
        val vh = EntryCellViewHolder.create(parent)
        val disabled = Color(0xFF999999)
        vh.bind(
            EntryCell(title = "メモ", text = "入力済み", isEnabled = false),
            Theme(cellValueTextColor = Color(0xFF1565C0), disabledTextColor = disabled),
        )

        assertEquals(disabled.toArgb(), vh.editText.currentTextColor)
    }

    @Test
    fun `EntryCell の入力文字色は全段未指定なら同梱テーマの文字色になりホストテーマに追従しない`() {
        val darkCtx = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Dark_NoActionBar,
        )
        // 既定色の解決元はホストのテーマではなく同梱テーマ（常時ラップ。android/ADR-0020）。
        val expected = hostTextColorPrimary(darkCtx.ksThemedContext())
        val hostDark = hostTextColorPrimary(darkCtx)
        // 空振り防止: 同梱テーマ（ライト側）とホストのダーク既定色が実際に異なることを前提にする。
        assertNotEquals("前提: 同梱テーマとホストダークの textColorPrimary が異なる", hostDark, expected)

        val vh = EntryCellViewHolder.create(FrameLayout(darkCtx))
        vh.bind(EntryCell(title = "メモ", text = "入力済み"), Theme())

        assertEquals(expected, vh.editText.currentTextColor)
        assertNotEquals("ホストテーマの文字色が漏れている", hostDark, vh.editText.currentTextColor)
    }

    /** 指定 Context のテーマから既定の文字色（`android:textColorPrimary`）を取り出す。 */
    private fun hostTextColorPrimary(context: android.content.Context): Int {
        val tv = android.util.TypedValue()
        assertTrue(
            "前提: テーマが textColorPrimary を解決できる",
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true),
        )
        if (tv.resourceId != 0) {
            val csl = androidx.core.content.ContextCompat.getColorStateList(context, tv.resourceId)
            if (csl != null) return csl.defaultColor
        }
        return tv.data
    }

    // MARK: - EntryCell の Enter キー挙動

    @Test
    fun `EntryCell の EditText は imeOptions に完了アクションを設定する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "ニックネーム"), Theme())
        assertEquals(
            EditorInfo.IME_ACTION_DONE,
            vh.editText.imeOptions and EditorInfo.IME_MASK_ACTION,
        )
    }

    @Test
    fun `単一行 EntryCell は IME へ完了アクションを通知する`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "名前"), Theme())

        val outAttrs = EditorInfo()
        vh.editText.onCreateInputConnection(outAttrs)
        // 明示した完了アクションがそのまま IME へ渡り、Enter がフォーカス移動を起こさない。
        assertEquals(
            EditorInfo.IME_ACTION_DONE,
            outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION,
        )
        assertEquals(0, outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION)
    }

    @Test
    fun `複数行 EntryCell では Enter が改行として扱われる`() {
        val row = buildRowWithFocusableSibling(
            EntryCell(
                title = "メモ",
                text = "あ",
                keyboardType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            ),
        )

        // 複数行 inputType のときはフレームワークが改行キー要求フラグを付加するため、
        // IME は完了アクションではなく改行キーを送る。
        val outAttrs = EditorInfo()
        row.vh.editText.onCreateInputConnection(outAttrs)
        assertNotEquals(0, outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION)

        // 届いた Enter キーは改行として挿入される（消費もフォーカス移動もされない）。
        assertTrue(row.vh.editText.requestFocus())
        row.vh.editText.setSelection(1)
        dispatchEnterKey(row.vh.editText)
        assertEquals("あ\n", row.vh.editText.text?.toString())
        assertTrue(row.vh.editText.hasFocus())
        assertFalse(row.sibling.hasFocus())
    }

    @Test
    fun `単一行 EntryCell は生の Enter キーでフォーカス探索を起こさない`() {
        val row = buildRowWithFocusableSibling(EntryCell(title = "ニックネーム", text = "太郎"))
        assertTrue(row.vh.editText.requestFocus())
        // 後続にフォーカス探索の着地先が実在する状態で検証する（着地先が無ければ
        // フォーカス探索が走っても移動が起きず、退行を検出できないため）。
        assertSame(row.sibling, row.vh.editText.focusSearch(View.FOCUS_DOWN))

        dispatchEnterKey(row.vh.editText)

        // Enter は完了として消費される。フォーカスは EditText に残り、テキストも変わらない。
        assertTrue(row.vh.editText.hasFocus())
        assertFalse(row.sibling.hasFocus())
        assertEquals("太郎", row.vh.editText.text?.toString())
    }

    @Test
    fun `単一行 EntryCell は Enter の離上だけが届いてもフォーカス探索を起こさない`() {
        val row = buildRowWithFocusableSibling(EntryCell(title = "ニックネーム", text = "太郎"))
        assertTrue(row.vh.editText.requestFocus())
        assertSame(row.sibling, row.vh.editText.focusSearch(View.FOCUS_DOWN))

        // IME 表示中は押下が IME 側で消費され、離上だけがアプリへ届く配達パターンになる。
        row.vh.editText.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))

        assertTrue(row.vh.editText.hasFocus())
        assertFalse(row.sibling.hasFocus())
        assertEquals("太郎", row.vh.editText.text?.toString())
    }

    @Test
    fun `単一行 EntryCell は Enter 以外のキーを消費しない`() {
        val row = buildRowWithFocusableSibling(EntryCell(title = "ニックネーム"))
        assertTrue(row.vh.editText.requestFocus())

        val consumed = row.vh.editText.dispatchKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A),
        )

        // 通常のキーは EditText の入力処理へ渡り、文字として反映される。
        assertTrue(consumed)
        assertEquals("a", row.vh.editText.text?.toString())
    }

    @Test
    fun `EntryCell を単一行と複数行で使い回しても Enter の扱いが混ざらない`() {
        val row = buildRowWithFocusableSibling(EntryCell(id = "entry-enter", title = "名前"))
        val multiLineCell = EntryCell(
            id = "entry-enter",
            title = "メモ",
            keyboardType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
        )
        val singleLineCell = EntryCell(id = "entry-enter", title = "名前")

        assertTrue(row.vh.editText.requestFocus())
        dispatchEnterKey(row.vh.editText)
        assertEquals("", row.vh.editText.text?.toString())
        assertFalse(row.sibling.hasFocus())

        // 複数行 Cell へ再利用すると Enter は改行に戻る。
        row.vh.reset()
        row.vh.bind(multiLineCell, Theme())
        assertTrue(row.vh.editText.requestFocus())
        dispatchEnterKey(row.vh.editText)
        assertEquals("\n", row.vh.editText.text?.toString())

        // 単一行 Cell へ戻すと再び消費される（複数行 bind のリスナー解除が残らない）。
        row.vh.reset()
        row.vh.bind(singleLineCell, Theme())
        assertTrue(row.vh.editText.requestFocus())
        dispatchEnterKey(row.vh.editText)
        assertEquals("", row.vh.editText.text?.toString())
        assertTrue(row.vh.editText.hasFocus())
        assertFalse(row.sibling.hasFocus())
    }

    @Test
    fun `EntryCell の reset と再 bind を経ても imeOptions は完了アクションのまま維持される`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(
                title = "メモ",
                keyboardType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            ),
            Theme(),
        )

        vh.reset()
        assertEquals(
            EditorInfo.IME_ACTION_DONE,
            vh.editText.imeOptions and EditorInfo.IME_MASK_ACTION,
        )

        // 複数行 Cell を表示した ViewHolder を単一行 Cell へ再利用しても、
        // 完了アクションと改行キー要求フラグの組み合わせが食い違わない。
        vh.bind(EntryCell(title = "名前"), Theme())
        val outAttrs = EditorInfo()
        vh.editText.onCreateInputConnection(outAttrs)
        assertEquals(
            EditorInfo.IME_ACTION_DONE,
            outAttrs.imeOptions and EditorInfo.IME_MASK_ACTION,
        )
        assertEquals(0, outAttrs.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION)
    }

    /** 指定 [EditText] へ Enter キーの down / up を送る。 */
    private fun dispatchEnterKey(edit: EditText) {
        edit.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        edit.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    /** [buildRowWithFocusableSibling] の結果。 */
    private class EnterKeyRow(val vh: EntryCellViewHolder, val sibling: View)

    /**
     * EntryCell 行の直下にフォーカスを取れる View を並べ、Activity へ載せてレイアウトまで済ませる。
     * `focusSearch` は root namespace までの親と実寸を必要とするため、Enter キーによる
     * フォーカス探索が実際に走る条件をここで整える。
     */
    private fun buildRowWithFocusableSibling(cell: EntryCell): EnterKeyRow {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val container = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val vh = EntryCellViewHolder.create(container)
        container.addView(vh.views.root)
        val sibling = android.widget.TextView(ctx).apply {
            text = "後続 View"
            isFocusable = true
            isFocusableInTouchMode = true
        }
        container.addView(sibling)
        activity.setContentView(container)
        vh.bind(cell, Theme())
        container.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY),
        )
        container.layout(0, 0, 1000, 2000)
        return EnterKeyRow(vh, sibling)
    }

    // MARK: - EntryCell 同値再バインド時の IME 保護

    @Test
    fun `EntryCell 同値 Cell の再 bind では inputType setter が呼ばれない`() {
        val edit = InputTypeCountingEditText(ctx)
        val vh = createEntryCellViewHolder(edit)
        val cell = EntryCell(
            id = "entry-ime",
            title = "電話",
            text = "090",
            keyboardType = InputType.TYPE_CLASS_PHONE,
        )
        vh.bind(cell, Theme())
        assertEquals(1, edit.inputTypeSetCount)

        // 入力 1 文字ごとに走る同値のフルリバインド相当。setInputType は
        // InputMethodManager.restartInput を伴うため、ここで呼ばれてはいけない。
        vh.bind(cell.copy(), Theme())
        assertEquals(1, edit.inputTypeSetCount)
        assertEquals(InputType.TYPE_CLASS_PHONE, edit.inputType)
    }

    @Test
    fun `EntryCell の keyboardType が変わったときは inputType setter が呼ばれる`() {
        val edit = InputTypeCountingEditText(ctx)
        val vh = createEntryCellViewHolder(edit)
        vh.bind(
            EntryCell(id = "entry-ime", title = "x", keyboardType = InputType.TYPE_CLASS_PHONE),
            Theme(),
        )
        assertEquals(1, edit.inputTypeSetCount)

        vh.bind(
            EntryCell(id = "entry-ime", title = "x", keyboardType = InputType.TYPE_CLASS_NUMBER),
            Theme(),
        )
        assertEquals(2, edit.inputTypeSetCount)
        assertEquals(InputType.TYPE_CLASS_NUMBER, edit.inputType)
    }

    @Test
    fun `EntryCell 同値 Cell の再 bind では filters が差し替えられない`() {
        val vh = EntryCellViewHolder.create(parent)
        val cell = EntryCell(id = "entry-ime", title = "x", maxLength = 5)
        vh.bind(cell, Theme())
        val filtersAfterFirstBind = vh.editText.filters

        vh.bind(cell.copy(), Theme())
        assertSame(filtersAfterFirstBind, vh.editText.filters)
    }

    @Test
    fun `EntryCell の maxLength が変わったときは filters が差し替えられる`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(id = "entry-ime", title = "x", maxLength = 5), Theme())

        vh.bind(EntryCell(id = "entry-ime", title = "x", maxLength = 8), Theme())
        val filter = vh.editText.filters.single()
        assertTrue(filter is InputFilter.LengthFilter)
        assertEquals(8, (filter as InputFilter.LengthFilter).max)

        vh.bind(EntryCell(id = "entry-ime", title = "x", maxLength = null), Theme())
        assertEquals(0, vh.editText.filters.size)
    }

    @Test
    fun `EntryCell 同値 Cell の再 bind では hint が差し替えられない`() {
        val vh = EntryCellViewHolder.create(parent)
        val cell = EntryCell(id = "entry-ime", title = "x", placeholder = "未入力")
        vh.bind(cell, Theme())
        val hintAfterFirstBind = vh.editText.hint

        // 同値だが別インスタンスの文字列で再 bind する。`setHint` は final で呼び出し回数を
        // 観測できないため、差分ガードが効いていれば hint が初回 bind のインスタンスのまま
        // 維持されること（setHint 未呼び出し）をインスタンス同一性で検証する。
        vh.bind(cell.copy(placeholder = buildString { append("未入力") }), Theme())
        assertSame(hintAfterFirstBind, vh.editText.hint)

        vh.bind(cell.copy(placeholder = null), Theme())
        assertNull(vh.editText.hint)
    }

    /**
     * 共通行レイアウトに任意の [EditText] を差し込んだ [EntryCellViewHolder] を組み立てる。
     * setter 呼び出しを観測できる EditText を注入するために使う。
     */
    private fun createEntryCellViewHolder(edit: EditText): EntryCellViewHolder {
        val views = buildCellBaseViews(parent)
        edit.isSingleLine = true
        // 本番の `EntryCellViewHolder.create` と同じく、本体行へ「残り幅全体を占める行内 trailing」
        // として追加する（android/ADR-0002）。
        addFillingInlineTrailing(views, edit)
        return EntryCellViewHolder(views = views, editText = edit)
    }

    /**
     * `setInputType` の呼び出し回数を数える [EditText]。
     * `TextView.setInputType` は同値でも `InputMethodManager.restartInput` を呼ぶため、
     * 「呼ばれた回数」が IME の未確定文字列が壊される回数と対応する。
     */
    private class InputTypeCountingEditText(context: android.content.Context) : EditText(context) {
        /** コンストラクタ完了後（プロパティ初期化後）からの呼び出し回数。 */
        var inputTypeSetCount = 0

        override fun setInputType(type: Int) {
            inputTypeSetCount++
            super.setInputType(type)
        }
    }

    // MARK: - PickerCell

    @Test
    fun `PickerCell single 自動 valueText は selectedIndex の items 要素を返す`() {
        val cell = PickerCell(
            title = "テーマ",
            items = listOf("ライト", "ダーク", "自動"),
            selectedIndex = 1,
        )
        assertEquals("ダーク", cell.autoValueText())
    }

    @Test
    fun `PickerCell multi 自動 valueText はカンマ連結で表示`() {
        val cell = PickerCell(
            title = "通知種別",
            items = listOf("メール", "プッシュ", "SMS"),
            selectedIndices = setOf(0, 2),
        )
        assertEquals("メール, SMS", cell.autoValueText())
    }

    @Test
    fun `PickerCellViewHolder bind で title と auto valueText が反映される`() {
        val vh = PickerCellViewHolder.create(parent)
        val cell = PickerCell(
            title = "テーマ",
            items = listOf("ライト", "ダーク"),
            selectedIndex = 0,
        )
        vh.bind(cell, Theme())
        assertEquals("テーマ", vh.views.titleView.text?.toString())
        assertEquals("ライト", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `PickerCellViewHolder bind で valueText 明示指定が優先される`() {
        val vh = PickerCellViewHolder.create(parent)
        val cell = PickerCell(
            title = "テーマ",
            items = listOf("ライト", "ダーク"),
            selectedIndex = 0,
            valueText = "カスタム",
        )
        vh.bind(cell, Theme())
        assertEquals("カスタム", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `PickerCellViewHolder single タップでボトムシートの選択面が表示される`() {
        val vh = PickerCellViewHolder.create(parent)
        val cell = PickerCell(
            title = "テーマ",
            items = listOf("A", "B", "C"),
            selectedIndex = 0,
        )
        vh.bind(cell, Theme())
        vh.views.root.performClick()
        // ShadowDialog.getLatestDialog で表示中の Dialog を取得する。
        val latest = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertNotNull(latest)
        assertTrue(latest is PickerSelectionSheet)
    }

    @Test
    fun `PickerCellViewHolder isEnabled = false のときタップは無効化される`() {
        val vh = PickerCellViewHolder.create(parent)
        val cell = PickerCell(title = "x", items = listOf("A"), isEnabled = false)
        vh.bind(cell, Theme())
        assertFalse(vh.views.root.isClickable)
    }

    // MARK: - NumberPickerCell

    @Test
    fun `NumberPickerCell 既定値は min=0 max=100 step=1`() {
        val c = NumberPickerCell(title = "x")
        assertEquals(0, c.min)
        assertEquals(100, c.max)
        assertEquals(1, c.step)
    }

    @Test
    fun `NumberPickerCellViewHolder bind で valueText が value 文字列化を表示`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(NumberPickerCell(title = "音量", value = 50), Theme())
        assertEquals("50", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `NumberPickerCell 既定の unit は空文字`() {
        assertEquals("", NumberPickerCell(title = "x").unit)
    }

    @Test
    fun `NumberPickerCell format は unit 非空なら半角スペース区切りで連結する`() {
        assertEquals("15 px", NumberPickerCell.format(15, "px"))
    }

    @Test
    fun `NumberPickerCell format は unit が空なら数値のみを返す`() {
        assertEquals("30", NumberPickerCell.format(30, ""))
    }

    @Test
    fun `NumberPickerCell equals は unit の差を区別する`() {
        val a = NumberPickerCell(id = "np", title = "サイズ", value = 15, unit = "px")
        val b = a.copy(unit = "pt")
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
        assertEquals(a, a.copy(unit = "px"))
    }

    @Test
    fun `NumberPickerCellViewHolder bind で unit 指定時は単位付き文字列を表示`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(NumberPickerCell(title = "サイズ", value = 15, unit = "px"), Theme())
        assertEquals("15 px", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `NumberPickerCellViewHolder bind で unit 未指定なら数値のみを表示`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(NumberPickerCell(title = "サイズ", value = 30), Theme())
        assertEquals("30", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `NumberPickerCellViewHolder bind で valueText 明示指定は unit より優先される`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(
            NumberPickerCell(title = "サイズ", value = 15, unit = "px", valueText = "十五ピクセル"),
            Theme(),
        )
        assertEquals("十五ピクセル", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `NumberPickerCellViewHolder タップで選択面のボトムシートが表示される`() {
        val vh = NumberPickerCellViewHolder.create(parent)
        vh.bind(NumberPickerCell(title = "音量", value = 50), Theme())
        vh.views.root.performClick()
        // ShadowDialog.getLatestDialog で表示中の Dialog を取得する。
        val latest = org.robolectric.shadows.ShadowDialog.getLatestDialog()
        assertNotNull(latest)
        assertTrue(latest is NumberSelectionSheet)
    }

    // MARK: - TimePickerCell

    @Test
    fun `TimePickerCell 既定 format は HH colon mm`() {
        val c = TimePickerCell(title = "x")
        assertEquals("HH:mm", c.format)
    }

    @Test
    fun `TimePickerCell 既定 is24Hour は true`() {
        assertTrue(TimePickerCell(title = "x").is24Hour)
    }

    @Test
    fun `TimePickerCell の is24Hour は equals と hashCode に反映される`() {
        val base = TimePickerCell(id = "tp", title = "アラーム")
        val twelveHour = base.copy(is24Hour = false)
        assertNotEquals(base, twelveHour)
        assertNotEquals(base.hashCode(), twelveHour.hashCode())
        assertEquals(base, base.copy(is24Hour = true))
    }

    @Test
    fun `TimePickerCell formatTime は LocalTime を format 文字列化する`() {
        val s = TimePickerCellViewHolder.formatTime(LocalTime.of(7, 30), "HH:mm")
        assertEquals("07:30", s)
    }

    @Test
    fun `TimePickerCellViewHolder bind で valueText が format 適用文字列を表示`() {
        val vh = TimePickerCellViewHolder.create(parent)
        vh.bind(TimePickerCell(title = "アラーム", time = LocalTime.of(8, 0)), Theme())
        assertEquals("08:00", vh.views.valueTextView.text?.toString())
    }

    // MARK: - DatePickerCell

    @Test
    fun `DatePickerCell 既定 format は yyyy slash MM slash dd`() {
        val c = DatePickerCell(title = "x")
        assertEquals("yyyy/MM/dd", c.format)
    }

    @Test
    fun `DatePickerCell 既定 uiStyle は Material`() {
        val c = DatePickerCell(title = "x")
        assertEquals(DatePickerUIStyle.Material, c.uiStyle)
    }

    @Test
    fun `DatePickerCell formatDate は LocalDate を format 文字列化する`() {
        val s = DatePickerCellViewHolder.formatDate(LocalDate.of(2000, 1, 15), "yyyy/MM/dd")
        assertEquals("2000/01/15", s)
    }

    @Test
    fun `DatePickerCellViewHolder bind で valueText が format 適用文字列を表示`() {
        val vh = DatePickerCellViewHolder.create(parent)
        vh.bind(
            DatePickerCell(title = "誕生日", date = LocalDate.of(2000, 12, 31)),
            Theme(),
        )
        assertEquals("2000/12/31", vh.views.valueTextView.text?.toString())
    }

    @Test
    fun `DatePickerCell 既定 todayText は null`() {
        assertNull(DatePickerCell(title = "x").todayText)
    }

    @Test
    fun `DatePickerCell の todayText は equals と copy に反映される`() {
        val base = DatePickerCell(title = "x", todayText = "今日")
        assertEquals(base, base.copy())
        assertNotEquals(base, base.copy(todayText = null))
        assertNotEquals(base.hashCode(), base.copy(todayText = "Today").hashCode())
    }

    @Test
    fun `DatePickerCell の minDate maxDate は equals に反映される`() {
        val a = DatePickerCell(
            title = "x",
            minDate = LocalDate.of(2026, 6, 1),
            maxDate = LocalDate.of(2026, 12, 31),
        )
        val b = DatePickerCell(
            title = "x",
            minDate = LocalDate.of(2026, 6, 1),
            maxDate = LocalDate.of(2026, 12, 31),
        )
        // id が異なるため equals は false だが、コピー後は等価になる
        val c = a.copy(id = b.id)
        assertEquals(b, c)
    }

    // MARK: - 共通フィールド表示テスト

    @Test
    fun `TimePickerCell ViewHolder で title description icon hintText valueText がすべて反映される`() {
        val vh = TimePickerCellViewHolder.create(parent)
        val cell = TimePickerCell(
            title = "アラーム",
            description = "毎朝",
            hintText = "新規",
            time = LocalTime.of(7, 0),
        )
        vh.bind(cell, Theme())
        assertEquals("アラーム", vh.views.titleView.text?.toString())
        assertEquals("毎朝", vh.views.descriptionView.text?.toString())
        assertEquals("新規", vh.views.hintTextView.text?.toString())
        assertEquals("07:00", vh.views.valueTextView.text?.toString())
    }

    // MARK: - isVisible フィルタ

    @Test
    fun `EntryCell isVisible = false でも値型としては保持される`() {
        val c = EntryCell(title = "x", isVisible = false)
        assertFalse(c.isVisible)
        assertEquals("x", c.title)
    }

    // MARK: - 登録 API

    @Test
    fun `KsCellRegistry registerInputCells で 5 種が登録される`() {
        // clear → registerInputCells → 5 種が isRegistered = true になる
        KsCellRegistry.clear()
        KsCellRegistry.registerInputCells(ctx)
        assertTrue(KsCellRegistry.isRegistered(EntryCell::class))
        assertTrue(KsCellRegistry.isRegistered(PickerCell::class))
        assertTrue(KsCellRegistry.isRegistered(NumberPickerCell::class))
        assertTrue(KsCellRegistry.isRegistered(TimePickerCell::class))
        assertTrue(KsCellRegistry.isRegistered(DatePickerCell::class))
        // teardown
        KsCellRegistry.clear()
    }

    // MARK: - 補助型のケース存在

    @Test
    fun `PickerSelectionMode は Single と Multiple を持つ`() {
        assertEquals(2, PickerSelectionMode.entries.size)
        assertEquals(PickerSelectionMode.Single, PickerSelectionMode.valueOf("Single"))
        assertEquals(PickerSelectionMode.Multiple, PickerSelectionMode.valueOf("Multiple"))
    }

    @Test
    fun `DatePickerUIStyle は Material と Spinner を持つ`() {
        assertEquals(2, DatePickerUIStyle.entries.size)
        assertEquals(DatePickerUIStyle.Material, DatePickerUIStyle.valueOf("Material"))
        assertEquals(DatePickerUIStyle.Spinner, DatePickerUIStyle.valueOf("Spinner"))
    }
}
