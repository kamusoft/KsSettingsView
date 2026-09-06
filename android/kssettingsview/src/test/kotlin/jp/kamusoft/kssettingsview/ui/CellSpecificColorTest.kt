package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cell 種別が固有値として持つ色引数 12 本の未指定表現と、その段階解決を検証する。
 *
 * 12 本は「未指定 = `Color.Unspecified`」の 1 流儀で表され、既定値も `Color.Unspecified` である。
 * 未指定の色は次の段（`CellStyle` → `Theme` → 外観の既定）へ進み、`toArgb()` で透明な黒
 * （ARGB `0x00000000`）へ落ちてはならない。等価性は手書きの `equals` / `hashCode` が担うため、
 * 12 本すべてを表で回して「未指定同士は等価」「明示色が違えば非等価」を確かめる。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CellSpecificColorTest {

    private val ctx: Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    /** 透明な黒。未指定色を素の `toArgb()` に掛けたときに現れる値。 */
    private val transparentBlack = Color.Unspecified.toArgb()

    /**
     * Cell 固有色 1 本ぶんの検査軸。
     *
     * [omitted] は色引数を渡さずに構築した Cell、[build] は対象フィールドへ渡した色を載せた Cell、
     * [read] は組み立てた Cell から対象フィールドの色を読み出す。
     */
    private class ColorField(
        val name: String,
        val omitted: () -> Any,
        val build: (Color) -> Any,
        val read: (Any) -> Color,
    )

    /** 12 本の Cell 固有色。id を固定して等価性判定を色の違いだけに絞る。 */
    private val fields: List<ColorField> = listOf(
        ColorField(
            name = "EntryCell.accentColor",
            omitted = { EntryCell(id = "e", title = "x") },
            build = { EntryCell(id = "e", title = "x", accentColor = it) },
            read = { (it as EntryCell).accentColor },
        ),
        ColorField(
            name = "EntryCell.placeholderColor",
            omitted = { EntryCell(id = "e", title = "x") },
            build = { EntryCell(id = "e", title = "x", placeholderColor = it) },
            read = { (it as EntryCell).placeholderColor },
        ),
        ColorField(
            name = "ButtonCell.titleColor",
            omitted = { ButtonCell(id = "b", title = "x") },
            build = { ButtonCell(id = "b", title = "x", titleColor = it) },
            read = { (it as ButtonCell).titleColor },
        ),
        ColorField(
            name = "SwitchCell.accentColor",
            omitted = { SwitchCell(id = "s", title = "x") },
            build = { SwitchCell(id = "s", title = "x", accentColor = it) },
            read = { (it as SwitchCell).accentColor },
        ),
        ColorField(
            name = "CheckboxCell.accentColor",
            omitted = { CheckboxCell(id = "c", title = "x") },
            build = { CheckboxCell(id = "c", title = "x", accentColor = it) },
            read = { (it as CheckboxCell).accentColor },
        ),
        ColorField(
            name = "SimpleCheckCell.accentColor",
            omitted = { SimpleCheckCell(id = "sc", title = "x") },
            build = { SimpleCheckCell(id = "sc", title = "x", accentColor = it) },
            read = { (it as SimpleCheckCell).accentColor },
        ),
        ColorField(
            name = "RadioCell.accentColor",
            omitted = { RadioCell(id = "r", title = "x", groupId = "g", value = "a", selectedValue = "a") },
            build = {
                RadioCell(id = "r", title = "x", groupId = "g", value = "a", selectedValue = "a", accentColor = it)
            },
            read = { (it as RadioCell).accentColor },
        ),
        ColorField(
            name = "PickerCell.accentColor",
            omitted = { PickerCell(id = "p", title = "x") },
            build = { PickerCell(id = "p", title = "x", accentColor = it) },
            read = { (it as PickerCell).accentColor },
        ),
        ColorField(
            name = "NumberPickerCell.accentColor",
            omitted = { NumberPickerCell(id = "n", title = "x") },
            build = { NumberPickerCell(id = "n", title = "x", accentColor = it) },
            read = { (it as NumberPickerCell).accentColor },
        ),
        ColorField(
            name = "TimePickerCell.accentColor",
            omitted = { TimePickerCell(id = "t", title = "x") },
            build = { TimePickerCell(id = "t", title = "x", accentColor = it) },
            read = { (it as TimePickerCell).accentColor },
        ),
        ColorField(
            name = "DatePickerCell.accentColor",
            omitted = { DatePickerCell(id = "d", title = "x") },
            build = { DatePickerCell(id = "d", title = "x", accentColor = it) },
            read = { (it as DatePickerCell).accentColor },
        ),
        ColorField(
            name = "DatePickerCell.androidButtonColor",
            omitted = { DatePickerCell(id = "d", title = "x") },
            build = { DatePickerCell(id = "d", title = "x", androidButtonColor = it) },
            read = { (it as DatePickerCell).androidButtonColor },
        ),
    )

    // MARK: - 既定値と未指定表現

    @Test
    fun `Cell 固有色 12 本の既定は Unspecified`() {
        assertEquals("検査対象の Cell 固有色は 12 本", 12, fields.size)
        for (field in fields) {
            assertEquals(
                "${field.name} の既定が Unspecified ではない",
                Color.Unspecified,
                field.read(field.omitted()),
            )
        }
    }

    @Test
    fun `Cell 固有色 12 本は明示した色をそのまま保つ`() {
        val explicit = Color(0xFF12AB34)
        for (field in fields) {
            assertEquals(
                "${field.name} が明示した色を保っていない",
                explicit,
                field.read(field.build(explicit)),
            )
        }
    }

    // MARK: - 等価性（手書き equals / hashCode）

    @Test
    fun `Cell 固有色 12 本は未指定同士で等価になる`() {
        for (field in fields) {
            val omitted = field.omitted()
            val unspecified = field.build(Color.Unspecified)
            assertEquals("${field.name}: 省略と Unspecified の明示が非等価", omitted, unspecified)
            assertEquals(
                "${field.name}: 省略と Unspecified の明示で hashCode が違う",
                omitted.hashCode(),
                unspecified.hashCode(),
            )
        }
    }

    @Test
    fun `Cell 固有色 12 本は明示色の違いで非等価になる`() {
        val red = Color(0xFFFF0000)
        val green = Color(0xFF00FF00)
        for (field in fields) {
            assertNotEquals("${field.name}: 明示色が違うのに等価", field.build(red), field.build(green))
            assertNotEquals(
                "${field.name}: 明示色が違うのに hashCode が同じ（hashCode から欠落している）",
                field.build(red).hashCode(),
                field.build(green).hashCode(),
            )
            assertNotEquals("${field.name}: 明示色と未指定が等価", field.build(red), field.omitted())
        }
    }

    // MARK: - accent の 3 段解決（Cell 固有 → CellStyle → Theme）

    /** [cell] を bind した行の Switch を返す。 */
    private fun bindSwitchRow(cell: SwitchCell, theme: Theme): MaterialSwitch {
        val holder = SwitchCellViewHolder.create(FrameLayout(ctx))
        holder.bind(cell, theme)
        return requireNotNull(findMaterialSwitch(holder.itemView as ViewGroup)) {
            "MaterialSwitch が行に見つからない"
        }
    }

    private fun findMaterialSwitch(root: ViewGroup): MaterialSwitch? {
        for (index in 0 until root.childCount) {
            when (val child: View = root.getChildAt(index)) {
                is MaterialSwitch -> return child
                is ViewGroup -> findMaterialSwitch(child)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    /** オン状態の Track 色。実効 accent がそのまま出る。 */
    private fun onTrackColor(sw: MaterialSwitch): Int =
        sw.trackTintList!!.getColorForState(intArrayOf(android.R.attr.state_checked), 0)

    private val themeAccent = Color(0xFF0000FF)
    private val styleAccent = Color(0xFF00FF00)
    private val cellAccent = Color(0xFFFF0000)

    @Test
    fun `明示した Cell 固有 accent は CellStyle と Theme より優先する`() {
        val sw = bindSwitchRow(
            SwitchCell(
                id = "s",
                title = "通知",
                isOn = true,
                style = CellStyle(accentColor = styleAccent),
                accentColor = cellAccent,
            ),
            Theme(cellAccentColor = themeAccent),
        )
        assertEquals(cellAccent.toArgb(), onTrackColor(sw))
    }

    @Test
    fun `未指定の Cell 固有 accent は CellStyle へ継承する`() {
        val sw = bindSwitchRow(
            SwitchCell(id = "s", title = "通知", isOn = true, style = CellStyle(accentColor = styleAccent)),
            Theme(cellAccentColor = themeAccent),
        )
        assertEquals(styleAccent.toArgb(), onTrackColor(sw))
        assertNotEquals("未指定が透明な黒へ落ちている", transparentBlack, onTrackColor(sw))
    }

    @Test
    fun `未指定の Cell 固有 accent と未指定の CellStyle は Theme へ継承する`() {
        val sw = bindSwitchRow(
            SwitchCell(id = "s", title = "通知", isOn = true),
            Theme(cellAccentColor = themeAccent),
        )
        assertEquals(themeAccent.toArgb(), onTrackColor(sw))
        assertNotEquals("未指定が透明な黒へ落ちている", transparentBlack, onTrackColor(sw))
    }

    /**
     * 手描きチェックマークを持つ 2 Cell も同じ 3 段解決に従う。
     *
     * Switch とは消費点（[KsSimpleCheckView.color]）が別なので、片方だけ透明な黒へ落ちる退行を
     * 拾えるように両方を観測する。
     */
    @Test
    fun `SimpleCheckCell と RadioCell の accent も 3 段で解決する`() {
        val theme = Theme(cellAccentColor = themeAccent)

        val simpleHolder = SimpleCheckCellViewHolder.create(FrameLayout(ctx))
        simpleHolder.bind(SimpleCheckCell(id = "sc", title = "x", accentColor = cellAccent), theme)
        assertEquals(cellAccent.toArgb(), checkViewOf(simpleHolder.itemView).color)

        simpleHolder.bind(SimpleCheckCell(id = "sc", title = "x"), theme)
        assertEquals(themeAccent.toArgb(), checkViewOf(simpleHolder.itemView).color)
        assertNotEquals(
            "未指定が透明な黒へ落ちている",
            transparentBlack,
            checkViewOf(simpleHolder.itemView).color,
        )

        val radioHolder = RadioCellViewHolder.create(FrameLayout(ctx))
        radioHolder.bind(
            RadioCell(id = "r", title = "x", groupId = "g", value = "a", selectedValue = "a"),
            theme,
        )
        assertEquals(themeAccent.toArgb(), checkViewOf(radioHolder.itemView).color)
        assertNotEquals(
            "未指定が透明な黒へ落ちている",
            transparentBlack,
            checkViewOf(radioHolder.itemView).color,
        )
    }

    // MARK: - accent の消費点（Checkbox の buttonTint / Entry のハイライトと caret）

    /**
     * [CheckboxCell] の accent が `MaterialCheckBox` の buttonTint として出ることを検証する。
     *
     * Switch や手描きチェックマークとは消費点が別で、未指定の accent を素の `toArgb()` に掛けると
     * ここだけ透明な黒になり、チェックボックスの箱が見えなくなる。3 段解決の結果ではなく
     * 消費点そのもの（`buttonTintList`）を観測する。
     */
    @Test
    fun `CheckboxCell の accent は buttonTint として出る`() {
        val theme = Theme(cellAccentColor = themeAccent)

        val holder = CheckboxCellViewHolder.create(FrameLayout(ctx))
        holder.bind(CheckboxCell(id = "cb", title = "x", accentColor = cellAccent), theme)
        assertEquals(cellAccent.toArgb(), buttonTintOf(holder.itemView))

        holder.bind(CheckboxCell(id = "cb", title = "x"), theme)
        assertEquals(themeAccent.toArgb(), buttonTintOf(holder.itemView))
        assertNotEquals("未指定が透明な黒へ落ちている", transparentBlack, buttonTintOf(holder.itemView))
    }

    /**
     * [EntryCell] の accent が入力欄の選択ハイライトと caret に出ることを検証する。
     *
     * ハイライトと caret tint は同じ実効 accent から作られるため、観測できる
     * `highlightColor` が退行すれば caret も同じ色で退行している。未指定を素の `toArgb()` に
     * 掛けると透明な黒になり、選択範囲が見えなくなる。
     */
    @Test
    fun `EntryCell の accent は入力欄のハイライト色として出る`() {
        val theme = Theme(cellAccentColor = themeAccent)

        val holder = EntryCellViewHolder.create(FrameLayout(ctx))
        holder.bind(EntryCell(id = "e", title = "x", accentColor = cellAccent), theme)
        assertEquals(cellAccent.toArgb(), holder.editText.highlightColor)

        holder.bind(EntryCell(id = "e", title = "x"), theme)
        assertEquals(themeAccent.toArgb(), holder.editText.highlightColor)
        assertNotEquals("未指定が透明な黒へ落ちている", transparentBlack, holder.editText.highlightColor)
    }

    /** 行の View ツリーからチェックボックスの buttonTint を取り出す。 */
    private fun buttonTintOf(itemView: View): Int {
        val checkBox = requireNotNull(findCheckBox(itemView as ViewGroup)) {
            "MaterialCheckBox が行に見つからない"
        }
        return requireNotNull(checkBox.buttonTintList) { "buttonTintList が設定されていない" }.defaultColor
    }

    private fun findCheckBox(root: ViewGroup): MaterialCheckBox? {
        for (index in 0 until root.childCount) {
            when (val child: View = root.getChildAt(index)) {
                is MaterialCheckBox -> return child
                is ViewGroup -> findCheckBox(child)?.let { return it }
                else -> Unit
            }
        }
        return null
    }

    /** 行の View ツリーから手描きチェックマークを取り出す。 */
    private fun checkViewOf(itemView: View): KsSimpleCheckView =
        requireNotNull(findCheckView(itemView as ViewGroup)) { "KsSimpleCheckView が行に見つからない" }

    private fun findCheckView(root: ViewGroup): KsSimpleCheckView? {
        for (index in 0 until root.childCount) {
            when (val child: View = root.getChildAt(index)) {
                is KsSimpleCheckView -> return child
                is ViewGroup -> findCheckView(child)?.let { return it }
                else -> Unit
            }
        }
        return null
    }
}
