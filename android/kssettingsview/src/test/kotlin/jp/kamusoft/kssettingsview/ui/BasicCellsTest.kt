package jp.kamusoft.kssettingsview.ui

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import com.google.android.material.R as MaterialR
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.checkbox.MaterialCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 基本 Cell 7 種（[LabelCell] / [CommandCell] / [ButtonCell] / [SwitchCell] /
 * [CheckboxCell] / [RadioCell] / [SimpleCheckCell]）の bind / 通知 / 再利用クリアを検証する。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BasicCellsTest {

    // MaterialSwitch / 一部 AppCompat Widget は Theme.AppCompat / Theme.MaterialComponents
    // 派生テーマでなければ初期化時に IllegalArgumentException を投げるため、
    // テスト用の Context は ContextThemeWrapper で Material3 系テーマを明示する。
    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )
    private val parent get() = FrameLayout(ctx)

    // MARK: - id デフォルト値規約

    @Test
    fun `LabelCell は id デフォルト値で自動採番される`() {
        val a = LabelCell(title = "x")
        val b = LabelCell(title = "x")
        assertNotEquals(a.id, b.id)
        assertTrue(a.id.startsWith("label-"))
    }

    @Test
    fun `全 Cell の id デフォルト値規約`() {
        assertTrue(CommandCell(title = "x").id.startsWith("command-"))
        assertTrue(ButtonCell(title = "x").id.startsWith("button-"))
        assertTrue(SwitchCell(title = "x").id.startsWith("switch-"))
        assertTrue(CheckboxCell(title = "x").id.startsWith("checkbox-"))
        assertTrue(
            RadioCell(
                title = "x",
                groupId = "g",
                value = "a",
                selectedValue = "a",
            ).id.startsWith("radio-"),
        )
        assertTrue(SimpleCheckCell(title = "x").id.startsWith("simple-check-"))
    }

    // MARK: - DSL 規約

    @Test
    fun `LabelCell withDSLId は新しい id を持つ copy を返す`() {
        val orig = LabelCell(title = "x", description = "d")
        val copy = orig.withDSLId("new-id") as LabelCell
        assertEquals("new-id", copy.id)
        assertEquals("x", copy.title)
        assertEquals("d", copy.description)
    }

    @Test
    fun `SwitchCell withDSLStyle は新しい style を持つ copy を返す`() {
        val orig = SwitchCell(title = "x", isOn = true)
        val newStyle = CellStyle(titleColor = Color(red = 1.0f, green = 0.0f, blue = 0.0f))
        val copy = orig.withDSLStyle(newStyle) as SwitchCell
        assertEquals(orig.id, copy.id)
        assertEquals(newStyle, copy.style)
        assertTrue(copy.isOn)
    }

    @Test
    fun `CommandCell の equals は onTap を無視する`() {
        val a = CommandCell(id = "x", title = "T", onTap = { /* a */ })
        val b = CommandCell(id = "x", title = "T", onTap = { /* b */ })
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // MARK: - LabelCell ViewHolder

    @Test
    fun `LabelCellViewHolder bind で title が反映される`() {
        val vh = LabelCellViewHolder.create(parent)
        val cell = LabelCell(title = "プロフィール")
        vh.bind(cell, Theme())
        val container = vh.itemView as android.view.ViewGroup
        val title = findFirstTextView(container)
        assertEquals("プロフィール", title?.text?.toString())
    }

    @Test
    fun `LabelCellViewHolder reset で title がクリアされる`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "X"), Theme())
        vh.reset()
        val container = vh.itemView as android.view.ViewGroup
        val title = findFirstTextView(container)
        // TextView.text は setText(null) 後でも空文字列を返すため、空文字列で確認
        assertEquals("", title?.text?.toString() ?: "")
    }

    // MARK: - CommandCell ViewHolder

    @Test
    fun `CommandCellViewHolder hideArrow_false で disclosure が表示される`() {
        val vh = CommandCellViewHolder.create(parent)
        vh.bind(CommandCell(title = "License"), Theme())
        val disclosure = findDisclosure(vh.itemView as android.view.ViewGroup)
        assertNotNull(disclosure)
        assertEquals(View.VISIBLE, disclosure?.visibility)
    }

    @Test
    fun `CommandCellViewHolder hideArrow_true で disclosure 非表示`() {
        val vh = CommandCellViewHolder.create(parent)
        vh.bind(CommandCell(title = "X", hideArrow = true), Theme())
        val disclosure = findDisclosure(vh.itemView as android.view.ViewGroup)
        assertEquals(View.GONE, disclosure?.visibility)
    }

    @Test
    fun `CommandCellViewHolder onTap がタップで発火する`() {
        val vh = CommandCellViewHolder.create(parent)
        var called = 0
        vh.bind(CommandCell(title = "X", onTap = { called++ }), Theme())
        vh.itemView.performClick()
        assertEquals(1, called)
    }

    @Test
    fun `CommandCellViewHolder reset で onClickListener が解除される`() {
        val vh = CommandCellViewHolder.create(parent)
        var called = 0
        vh.bind(CommandCell(title = "X", onTap = { called++ }), Theme())
        vh.reset()
        vh.itemView.performClick()
        assertEquals(0, called)
    }

    // MARK: - ButtonCell ViewHolder

    @Test
    fun `ButtonCellViewHolder で title が中央寄せで描画される`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "ログアウト"), Theme())
        // aux 指定なしのためボタンスタイル TextView 側で描画される。
        val tv = vh.buttonTextView
        assertEquals("ログアウト", tv.text.toString())
        // gravity = CENTER（=0x11）の確認
        assertTrue("CENTER bit が立っていること", (tv.gravity and android.view.Gravity.CENTER) != 0)
    }

    @Test
    fun `ButtonCellViewHolder onTap が発火する`() {
        val vh = ButtonCellViewHolder.create(parent)
        var called = 0
        vh.bind(ButtonCell(title = "X", onTap = { called++ }), Theme())
        // 改訂後: ボタンスタイル / 通常レイアウトいずれでも root にクリックリスナが設定される。
        vh.itemView.performClick()
        assertEquals(1, called)
    }

    // MARK: - SwitchCell ViewHolder

    @Test
    fun `SwitchCellViewHolder で初期状態 isOn が反映される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", isOn = true), Theme())
        val sw = findMaterialSwitch(vh.itemView as android.view.ViewGroup)
        assertNotNull(sw)
        assertTrue(sw!!.isChecked)
    }

    @Test
    fun `SwitchCellViewHolder 値変更で onValueChanged が呼ばれる`() {
        val vh = SwitchCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            SwitchCell(title = "X", isOn = false, onValueChanged = { received = it }),
            Theme(),
        )
        vh.simulateValueChange(true)
        assertEquals(true, received)
    }

    @Test
    fun `SwitchCellViewHolder reset で listener がクリアされる`() {
        val vh = SwitchCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            SwitchCell(title = "X", isOn = false, onValueChanged = { received = it }),
            Theme(),
        )
        vh.reset()
        vh.simulateValueChange(true)
        assertNull("reset 後は listener が呼ばれない", received)
    }

    @Test
    fun `SwitchCellViewHolder セル本体タップで onValueChanged が発火する`() {
        val vh = SwitchCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            SwitchCell(title = "X", isOn = false, onValueChanged = { received = it }),
            Theme(),
        )
        // セル本体（container）タップでスイッチがトグルし、OnCheckedChangeListener 経由で
        // onValueChanged が一度だけ発火する（二重発火しない）
        vh.simulateContainerTap()
        assertEquals(true, received)
    }

    // MARK: - CheckboxCell ViewHolder

    @Test
    fun `CheckboxCellViewHolder isChecked_true で CheckBox が isChecked=true`() {
        val vh = CheckboxCellViewHolder.create(parent)
        vh.bind(CheckboxCell(title = "X", isChecked = true), Theme())
        val cb = findCheckBox(vh.itemView as android.view.ViewGroup)
        assertNotNull(cb)
        assertTrue(cb!!.isChecked)
    }

    @Test
    fun `CheckboxCellViewHolder isChecked_false で CheckBox が isChecked=false`() {
        val vh = CheckboxCellViewHolder.create(parent)
        vh.bind(CheckboxCell(title = "X", isChecked = false), Theme())
        val cb = findCheckBox(vh.itemView as android.view.ViewGroup)
        assertNotNull(cb)
        assertFalse(cb!!.isChecked)
    }

    @Test
    fun `CheckboxCellViewHolder タップで toggle 値が通知される`() {
        val vh = CheckboxCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            CheckboxCell(title = "X", isChecked = false, onValueChanged = { received = it }),
            Theme(),
        )
        vh.itemView.performClick()
        assertEquals(true, received)
    }

    // MARK: - RadioCell ViewHolder

    @Test
    fun `RadioCellViewHolder value 一致で KsSimpleCheckView が isChecked=true`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(title = "Dark", groupId = "theme", value = "dark", selectedValue = "dark"),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertTrue(check!!.isChecked)
    }

    @Test
    fun `RadioCellViewHolder value 不一致で KsSimpleCheckView が isChecked=false`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(
                title = "Light",
                groupId = "theme",
                value = "light",
                selectedValue = "dark",
            ),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertFalse(check!!.isChecked)
    }

    @Test
    fun `RadioCellViewHolder タップで onSelected に value が渡される`() {
        val vh = RadioCellViewHolder.create(parent)
        var received: String? = null
        vh.bind(
            RadioCell(
                title = "L",
                groupId = "g",
                value = "light",
                selectedValue = "dark",
                onSelected = { received = it },
            ),
            Theme(),
        )
        vh.itemView.performClick()
        assertEquals("light", received)
    }

    // MARK: - SimpleCheckCell ViewHolder

    @Test
    fun `SimpleCheckCellViewHolder isChecked_true で KsSimpleCheckView が isChecked=true`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        vh.bind(SimpleCheckCell(title = "X", isChecked = true), Theme())
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertTrue(check!!.isChecked)
    }

    @Test
    fun `SimpleCheckCellViewHolder isChecked_false で KsSimpleCheckView が isChecked=false`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        vh.bind(SimpleCheckCell(title = "X", isChecked = false), Theme())
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertFalse(check!!.isChecked)
    }

    @Test
    fun `SimpleCheckCellViewHolder タップで toggle 値が通知される`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            SimpleCheckCell(title = "X", isChecked = true, onValueChanged = { received = it }),
            Theme(),
        )
        vh.itemView.performClick()
        assertEquals(false, received)
    }

    // MARK: - accent 着色

    @Test
    fun `RadioCellViewHolder のチェックは Theme_cellAccentColor で着色される`() {
        val vh = RadioCellViewHolder.create(parent)
        // 既定とは異なる accent 色（緑）を指定した Theme
        val theme = Theme(cellAccentColor = Color(0.0f, 1.0f, 0.0f, 1.0f))
        vh.bind(
            RadioCell(title = "Dark", groupId = "theme", value = "dark", selectedValue = "dark"),
            theme,
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        // title 色（黒）流用ではなく、accent 色が反映されていること
        assertEquals(theme.cellAccentColor.toArgb(), check!!.color)
        assertNotEquals(android.graphics.Color.BLACK, check.color)
    }

    @Test
    fun `SimpleCheckCellViewHolder のチェックは Theme_cellAccentColor で着色される`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        val theme = Theme(cellAccentColor = Color(0.0f, 1.0f, 0.0f, 1.0f))
        vh.bind(SimpleCheckCell(title = "X", isChecked = true), theme)
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertEquals(theme.cellAccentColor.toArgb(), check!!.color)
        assertNotEquals(android.graphics.Color.BLACK, check.color)
    }

    @Test
    fun `accent 未指定時は Theme 既定の cellAccentColor が使われる`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(title = "Dark", groupId = "theme", value = "dark", selectedValue = "dark"),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertEquals(Theme.DEFAULT_ACCENT_COLOR.toArgb(), check!!.color)
    }

    // MARK: - 表示状態同期の三層分離（core/ADR-0010）

    @Test
    fun `単一トグル系 Cell の equals は内部状態を含む素直な値型である`() {
        // 値型の equals は内部状態を含む全フィールド比較とする。差分検出（構造同期）は内容等価性を
        // 用いず id 同一性のみで行うため、内部状態を含めてもちらつきは生じない。
        assertNotEquals(
            SwitchCell(id = "s", title = "T", isOn = false),
            SwitchCell(id = "s", title = "T", isOn = true),
        )
        assertNotEquals(
            CheckboxCell(id = "c", title = "T", isChecked = false),
            CheckboxCell(id = "c", title = "T", isChecked = true),
        )
        assertNotEquals(
            SimpleCheckCell(id = "sc", title = "T", isChecked = false),
            SimpleCheckCell(id = "sc", title = "T", isChecked = true),
        )
    }

    @Test
    fun `単一トグル系 Cell の equals は内部状態以外の差も検出する`() {
        // 内部状態以外（title 等）が変われば従来どおり非等価。
        assertNotEquals(
            SwitchCell(id = "s", title = "A", isOn = true),
            SwitchCell(id = "s", title = "B", isOn = true),
        )
        assertNotEquals(
            CheckboxCell(id = "c", title = "A", isChecked = true),
            CheckboxCell(id = "c", title = "B", isChecked = true),
        )
        assertNotEquals(
            SimpleCheckCell(id = "sc", title = "A", isChecked = true),
            SimpleCheckCell(id = "sc", title = "B", isChecked = true),
        )
    }

    @Test
    fun `クロージャ onValueChanged は equals の比較対象から除外される`() {
        // 関数型（クロージャ）は構造的等価性を持たないため equals 対象から除外する。
        // 全フィールドが同一なら onValueChanged の有無に関わらず等価。
        assertEquals(
            SwitchCell(id = "s", title = "T", isOn = true),
            SwitchCell(id = "s", title = "T", isOn = true, onValueChanged = {}),
        )
        assertEquals(
            RadioCell(id = "r", title = "T", groupId = "g", value = "a", selectedValue = "a"),
            RadioCell(id = "r", title = "T", groupId = "g", value = "a", selectedValue = "a", onSelected = {}),
        )
    }

    @Test
    fun `RadioCell の equals は selectedValue を比較対象に含める`() {
        // RadioCell も他の Cell 同様、内部状態 selectedValue を含む素直な値型。選択変更の検出は
        // contentUpdates（内容変化の列挙）が担い、ReplaceCell → notifyItemChanged の部分更新で
        // 旧選択セルの ✓ が消える（複数 ✓ にならない）。
        assertNotEquals(
            RadioCell(id = "r", title = "T", groupId = "g", value = "a", selectedValue = "a"),
            RadioCell(id = "r", title = "T", groupId = "g", value = "a", selectedValue = "b"),
        )
    }

    @Test
    fun `areContentsTheSame は同一 id なら内容変化を無視し常に true を返す`() {
        // 「表示状態同期の三層分離」: 構造同期は id 同一性のみ。areContentsTheSame は内容（内部状態・
        // title・RadioCell の selectedValue）の異同に関わらず、同一 id なら常に true を返す。
        // これにより内容変化が行のフルリバインド（ちらつき）を起こさない。内容更新は ReplaceCell
        // → notifyItemChanged の部分更新で反映される。
        fun row(cell: jp.kamusoft.kssettingsview.core.Cell) =
            CellListItem.CellRow(sectionId = "sec", cell = cell)

        // Checkbox の内部状態変化
        val cbOld = row(CheckboxCell(id = "c", title = "T", isChecked = false))
        val cbChanged = row(CheckboxCell(id = "c", title = "T", isChecked = true))
        assertTrue(CellListItemDiffCallback.areItemsTheSame(cbOld, cbChanged))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(cbOld, cbChanged))

        // title 変化（内容）も同一 id なら true
        val lblOld = row(LabelCell(id = "l", title = "A"))
        val lblChanged = row(LabelCell(id = "l", title = "B"))
        assertTrue(CellListItemDiffCallback.areItemsTheSame(lblOld, lblChanged))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(lblOld, lblChanged))

        // RadioCell の selectedValue 変化（グループ連動）も同一 id なら true。
        // 旧選択セルの ✓ 解除は ReplaceCell → notifyItemChanged の部分更新で反映される。
        val rOld = row(RadioCell(id = "r", title = "T", groupId = "g", value = "dark", selectedValue = "dark"))
        val rChanged = row(RadioCell(id = "r", title = "T", groupId = "g", value = "dark", selectedValue = "light"))
        assertTrue(CellListItemDiffCallback.areItemsTheSame(rOld, rChanged))
        assertTrue(CellListItemDiffCallback.areContentsTheSame(rOld, rChanged))
    }

    @Test
    fun `getItemId は内容変化でも同一 id の Cell に対して安定`() {
        // refactor-display-state-sync: getItemId は内容非依存の id ベース安定 ID。
        // 内部状態 / title / selectedValue が変わっても同一 id なら同一 itemId を返す。
        fun rowId(cell: jp.kamusoft.kssettingsview.core.Cell): Long =
            KsSettingsListAdapter.stableIdOf(CellListItem.CellRow(sectionId = "sec", cell = cell))

        assertEquals(
            rowId(SwitchCell(id = "s", title = "T", isOn = false)),
            rowId(SwitchCell(id = "s", title = "U", isOn = true)),
        )
        assertEquals(
            rowId(RadioCell(id = "r", title = "T", groupId = "g", value = "dark", selectedValue = "dark")),
            rowId(RadioCell(id = "r", title = "T", groupId = "g", value = "dark", selectedValue = "light")),
        )
    }

    // MARK: - TwoWay トグル（オリジナル AiForms 準拠）回帰テスト

    @Test
    fun `CheckboxCellViewHolder セルタップで View が即トグルし通知される`() {
        // オリジナル CheckboxCellView.cs 準拠: セルタップで checkBox.toggle() され、
        // OnCheckedChangeListener 経由で onValueChanged が一度だけ発火する（二重発火しない）。
        val vh = CheckboxCellViewHolder.create(parent)
        var received: Boolean? = null
        var callCount = 0
        vh.bind(
            CheckboxCell(title = "X", isChecked = false, onValueChanged = { received = it; callCount++ }),
            Theme(),
        )
        vh.simulateContainerTap()
        val cb = findCheckBox(vh.itemView as android.view.ViewGroup)
        assertNotNull(cb)
        // View 自身が即トグルされている（submitList を介さない）
        assertTrue(cb!!.isChecked)
        assertEquals(true, received)
        assertEquals("通知は一度だけ（二重発火しない）", 1, callCount)
    }

    @Test
    fun `SimpleCheckCellViewHolder セルタップで View が即トグルし通知される`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        var received: Boolean? = null
        vh.bind(
            SimpleCheckCell(title = "X", isChecked = false, onValueChanged = { received = it }),
            Theme(),
        )
        vh.simulateContainerTap()
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertTrue(check!!.isChecked)
        assertEquals(true, received)
    }

    @Test
    fun `RadioCellViewHolder セルタップで自分を即選択し value を通知する`() {
        // オリジナル RadioCellView.cs 準拠: 未選択なら自分を即 ON にし onSelected(value) を発火。
        val vh = RadioCellViewHolder.create(parent)
        var received: String? = null
        vh.bind(
            RadioCell(
                title = "L",
                groupId = "g",
                value = "light",
                selectedValue = "dark",
                onSelected = { received = it },
            ),
            Theme(),
        )
        vh.simulateContainerTap()
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        // 自分を即 ON にしている（submitList を介さない）
        assertTrue(check!!.isChecked)
        assertEquals("light", received)
    }

    @Test
    fun `RadioCellViewHolder 既に選択済みならタップしても再通知しない`() {
        // オリジナルの !_simpleCheck.Selected 条件に相当。
        val vh = RadioCellViewHolder.create(parent)
        var callCount = 0
        vh.bind(
            RadioCell(
                title = "D",
                groupId = "g",
                value = "dark",
                selectedValue = "dark",
                onSelected = { callCount++ },
            ),
            Theme(),
        )
        vh.simulateContainerTap()
        assertEquals("既に選択済みなら onSelected は呼ばれない", 0, callCount)
    }

    // MARK: - Ripple 用 clickable（バグ② 回帰防止）

    @Test
    fun `LabelCellViewHolder は bind 後に container が clickable で Ripple 可能`() {
        // バグ②: LabelCell の container が clickable でないと RippleDrawable の ripple が出ない。
        // applyCellBackground で isClickable=true を設定するため、onTap を持たない LabelCell でも
        // container が clickable かつ背景が RippleDrawable であることを検証する。
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "プロフィール"), Theme())
        val container = vh.itemView as android.view.ViewGroup
        assertTrue("LabelCell container は Ripple 用に clickable であるべき", container.isClickable)
        assertTrue(
            "container 背景は RippleDrawable であるべき",
            container.background is android.graphics.drawable.RippleDrawable,
        )
    }

    @Test
    fun `ハンドラ未指定の CheckboxCell でも container が clickable で Ripple 可能`() {
        // onValueChanged 未指定でも Ripple は出す（オリジナル CellBaseView.cs 準拠）。
        val vh = CheckboxCellViewHolder.create(parent)
        vh.bind(CheckboxCell(title = "X", isChecked = false), Theme())
        val container = vh.itemView as android.view.ViewGroup
        assertTrue(container.isClickable)
        assertTrue(container.background is android.graphics.drawable.RippleDrawable)
    }

    // MARK: - 一括登録 API

    @Test
    fun `registerBasicCells で 7 種が登録される`() {
        KsCellRegistry.clear()
        try {
            KsCellRegistry.registerBasicCells(ctx)
            assertTrue(KsCellRegistry.isRegistered(LabelCell::class))
            assertTrue(KsCellRegistry.isRegistered(CommandCell::class))
            assertTrue(KsCellRegistry.isRegistered(ButtonCell::class))
            assertTrue(KsCellRegistry.isRegistered(SwitchCell::class))
            assertTrue(KsCellRegistry.isRegistered(CheckboxCell::class))
            assertTrue(KsCellRegistry.isRegistered(RadioCell::class))
            assertTrue(KsCellRegistry.isRegistered(SimpleCheckCell::class))

            // viewType 解決も確認
            val cell = LabelCell(title = "x")
            val vt = KsCellRegistry.viewTypeOf(cell)
            assertEquals(VIEW_TYPE_LABEL_CELL, vt)
        } finally {
            KsCellRegistry.clear()
        }
    }

    // MARK: - isEnabled / titleAlignment（refine-basic-cells-style）

    @Test
    fun `全 Cell のデフォルト isEnabled は true`() {
        assertTrue(LabelCell(title = "x").isEnabled)
        assertTrue(CommandCell(title = "x").isEnabled)
        assertTrue(ButtonCell(title = "x").isEnabled)
        assertTrue(SwitchCell(title = "x").isEnabled)
        assertTrue(CheckboxCell(title = "x").isEnabled)
        assertTrue(
            RadioCell(title = "x", groupId = "g", value = "a", selectedValue = "a").isEnabled,
        )
        assertTrue(SimpleCheckCell(title = "x").isEnabled)
    }

    @Test
    fun `isEnabled を false に指定できる`() {
        assertFalse(LabelCell(title = "x", isEnabled = false).isEnabled)
        assertFalse(SwitchCell(title = "x", isEnabled = false).isEnabled)
        assertFalse(CheckboxCell(title = "x", isEnabled = false).isEnabled)
    }

    @Test
    fun `isEnabled を変えると等価でなくなる`() {
        val id = "x"
        val a = LabelCell(id = id, title = "x", isEnabled = true)
        val b = LabelCell(id = id, title = "x", isEnabled = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `isEnabled を変えると hashCode も変わる`() {
        val id = "s"
        val a = SwitchCell(id = id, title = "x", isOn = true, isEnabled = true)
        val b = SwitchCell(id = id, title = "x", isOn = true, isEnabled = false)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `ButtonCell のデフォルト titleAlignment は CENTER`() {
        assertEquals(
            jp.kamusoft.kssettingsview.core.CellTitleAlignment.CENTER,
            ButtonCell(title = "x").titleAlignment,
        )
    }

    @Test
    fun `ButtonCell の titleAlignment を指定できる`() {
        assertEquals(
            jp.kamusoft.kssettingsview.core.CellTitleAlignment.START,
            ButtonCell(
                title = "x",
                titleAlignment = jp.kamusoft.kssettingsview.core.CellTitleAlignment.START,
            ).titleAlignment,
        )
        assertEquals(
            jp.kamusoft.kssettingsview.core.CellTitleAlignment.END,
            ButtonCell(
                title = "x",
                titleAlignment = jp.kamusoft.kssettingsview.core.CellTitleAlignment.END,
            ).titleAlignment,
        )
    }

    @Test
    fun `ButtonCell titleAlignment が異なれば等価でない`() {
        val id = "b"
        val a = ButtonCell(
            id = id,
            title = "x",
            titleAlignment = jp.kamusoft.kssettingsview.core.CellTitleAlignment.CENTER,
        )
        val b = ButtonCell(
            id = id,
            title = "x",
            titleAlignment = jp.kamusoft.kssettingsview.core.CellTitleAlignment.START,
        )
        assertNotEquals(a, b)
    }

    // MARK: - 内部 View への isEnabled 委譲（refine-basic-cells-style Suggestion-1）

    /**
     * RadioCellViewHolder: `isEnabled = false` の Cell を bind したとき、
     * disabled 表現が **内部 KsSimpleCheckView の `isEnabled`** に委譲され、
     * **View 全体の alpha は変更されない**ことを確認する。
     */
    @Test
    fun `RadioCellViewHolder disabled は内部 KsSimpleCheckView の isEnabled に委譲される`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(
                title = "Dark",
                groupId = "theme",
                value = "dark",
                selectedValue = "dark",
                isEnabled = false,
            ),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertFalse("内部 KsSimpleCheckView の isEnabled は false に委譲される", check!!.isEnabled)
        // View 全体の alpha は 1.0（描画分岐は KsSimpleCheckView の onDraw 内部で実施）
        assertEquals(
            "内部 View 全体の alpha は 1.0 のまま（描画分岐は onDraw 内部）",
            1.0f, check.alpha, 0.001f,
        )
    }

    @Test
    fun `RadioCellViewHolder isEnabled=true では内部 KsSimpleCheckView も enabled`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(
                title = "Dark",
                groupId = "theme",
                value = "dark",
                selectedValue = "dark",
                isEnabled = true,
            ),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertTrue(check!!.isEnabled)
    }

    @Test
    fun `SimpleCheckCellViewHolder disabled は内部 KsSimpleCheckView の isEnabled に委譲される`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        vh.bind(
            SimpleCheckCell(title = "X", isChecked = true, isEnabled = false),
            Theme(),
        )
        val check = findSimpleCheck(vh.itemView as android.view.ViewGroup)
        assertNotNull(check)
        assertFalse("内部 KsSimpleCheckView の isEnabled は false に委譲される", check!!.isEnabled)
        assertEquals(
            "内部 View 全体の alpha は 1.0 のまま（描画分岐は onDraw 内部）",
            1.0f, check.alpha, 0.001f,
        )
    }

    /**
     * CheckboxCellViewHolder: MaterialCheckBox は Android 標準の `isEnabled` 機構を持つため、
     * `cell.isEnabled = false` 時に `checkBox.isEnabled = false` が反映されることを確認する
     * （内部 View に disabled 表現を委譲）。
     */
    @Test
    fun `CheckboxCellViewHolder disabled は内部 MaterialCheckBox の isEnabled に委譲される`() {
        val vh = CheckboxCellViewHolder.create(parent)
        vh.bind(
            CheckboxCell(title = "X", isChecked = true, isEnabled = false),
            Theme(),
        )
        val cb = findCheckBox(vh.itemView as android.view.ViewGroup)
        assertNotNull(cb)
        assertFalse("内部 MaterialCheckBox の isEnabled は false に委譲される", cb!!.isEnabled)
        assertEquals(
            "内部 View 全体の alpha は 1.0 のまま（MaterialCheckBox 標準の disabled 描画に委譲）",
            1.0f, cb.alpha, 0.001f,
        )
    }

    /**
     * KsSimpleCheckView: `isEnabled` プロパティが View 標準の `setEnabled(Boolean)` 経路で
     * 切り替え可能かを単体で確認する。
     */
    @Test
    fun `KsSimpleCheckView の isEnabled は setEnabled で切替できる`() {
        val v = KsSimpleCheckView(ctx)
        assertTrue("既定は enabled = true", v.isEnabled)
        v.isEnabled = false
        assertFalse(v.isEnabled)
        v.isEnabled = true
        assertTrue(v.isEnabled)
    }

    // ====================================================================
    // ButtonCell baseColor の 4 段階優先順位
    // ====================================================================

    @Test
    fun `ButtonCellViewHolder baseColor Cell 個別 titleColor 優先`() {
        val vh = ButtonCellViewHolder.create(parent)
        val red = Color(red = 1.0f, green = 0.0f, blue = 0.0f, alpha = 1.0f)
        val themeColor = Color(red = 0.0f, green = 1.0f, blue = 0.0f, alpha = 1.0f)
        vh.bind(
            ButtonCell(title = "削除", titleColor = red),
            Theme(cellTitleColor = themeColor),
        )
        val tv = vh.buttonTextView
        assertEquals(red.toArgb(), tv.currentTextColor)
    }

    @Test
    fun `ButtonCellViewHolder baseColor CellStyle titleColor 優先`() {
        val vh = ButtonCellViewHolder.create(parent)
        val purple = Color(red = 0.5f, green = 0.0f, blue = 0.5f, alpha = 1.0f)
        vh.bind(
            ButtonCell(title = "次へ", style = CellStyle(titleColor = purple)),
            Theme(),
        )
        val tv = vh.buttonTextView
        assertEquals(purple.toArgb(), tv.currentTextColor)
    }

    @Test
    fun `ButtonCellViewHolder baseColor Theme titleColor 優先`() {
        val vh = ButtonCellViewHolder.create(parent)
        val orange = Color(red = 0.8f, green = 0.6f, blue = 0.0f, alpha = 1.0f)
        vh.bind(
            ButtonCell(title = "登録"),
            Theme(cellTitleColor = orange),
        )
        val tv = vh.buttonTextView
        assertEquals(orange.toArgb(), tv.currentTextColor)
    }

    @Test
    fun `ButtonCellViewHolder baseColor 全て未指定はテーマの colorPrimary か systemBlue にフォールバック`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "OK"), Theme())
        val tv = vh.buttonTextView
        // テーマの colorPrimary が解決できなければ SYSTEM_BLUE (0xFF007AFF) にフォールバック。
        // Robolectric の app theme 次第なので、少なくとも完全透明ではない (alpha = 0xFF) ことのみ担保。
        assertEquals(0xFF, android.graphics.Color.alpha(tv.currentTextColor))
    }

    @Test
    fun `ButtonCellViewHolder isEnabled false で disabledTextColor が baseColor より優先される`() {
        val vh = ButtonCellViewHolder.create(parent)
        val red = Color(red = 1.0f, green = 0.0f, blue = 0.0f, alpha = 1.0f)
        val gray = Color(red = 0.5f, green = 0.5f, blue = 0.5f, alpha = 1.0f)
        vh.bind(
            ButtonCell(title = "削除", titleColor = red, isEnabled = false),
            Theme(disabledTextColor = gray),
        )
        val tv = vh.buttonTextView
        assertEquals(gray.toArgb(), tv.currentTextColor)
    }

    // ====================================================================
    // 各 Cell で Theme.titleColor が反映される
    // ====================================================================

    @Test
    fun `LabelCellViewHolder Theme titleColor が反映される`() {
        val vh = LabelCellViewHolder.create(parent)
        val themeColor = Color(red = 0.3f, green = 0.7f, blue = 0.2f, alpha = 1.0f)
        vh.bind(LabelCell(title = "X"), Theme(cellTitleColor = themeColor))
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `CommandCellViewHolder Theme titleColor が反映される`() {
        val vh = CommandCellViewHolder.create(parent)
        val themeColor = Color(red = 0.4f, green = 0.5f, blue = 0.6f, alpha = 1.0f)
        vh.bind(CommandCell(title = "X"), Theme(cellTitleColor = themeColor))
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `SwitchCellViewHolder Theme titleColor が反映される`() {
        // SwitchCell の Theme.titleColor 反映を直接検証する。
        val vh = SwitchCellViewHolder.create(parent)
        val themeColor = Color(red = 0.5f, green = 0.0f, blue = 0.8f, alpha = 1.0f)
        vh.bind(
            SwitchCell(title = "通知", isOn = false, onValueChanged = {}),
            Theme(cellTitleColor = themeColor),
        )
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `CheckboxCellViewHolder Theme titleColor が反映される`() {
        // CheckboxCell の Theme.titleColor 反映を直接検証する。
        val vh = CheckboxCellViewHolder.create(parent)
        val themeColor = Color(red = 0.2f, green = 0.4f, blue = 0.6f, alpha = 1.0f)
        vh.bind(
            CheckboxCell(title = "同意する", isChecked = false, onValueChanged = {}),
            Theme(cellTitleColor = themeColor),
        )
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `RadioCellViewHolder Theme titleColor が反映される`() {
        // RadioCell の Theme.titleColor 反映を直接検証する。
        val vh = RadioCellViewHolder.create(parent)
        val themeColor = Color(red = 0.7f, green = 0.1f, blue = 0.3f, alpha = 1.0f)
        vh.bind(
            RadioCell(title = "Dark", groupId = "theme", value = "dark", selectedValue = "dark"),
            Theme(cellTitleColor = themeColor),
        )
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `SimpleCheckCellViewHolder Theme titleColor が反映される`() {
        // SimpleCheckCell の Theme.titleColor 反映を直接検証する。
        val vh = SimpleCheckCellViewHolder.create(parent)
        val themeColor = Color(red = 0.1f, green = 0.9f, blue = 0.4f, alpha = 1.0f)
        vh.bind(
            SimpleCheckCell(title = "選択", isChecked = false, onValueChanged = {}),
            Theme(cellTitleColor = themeColor),
        )
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(themeColor.toArgb(), title!!.currentTextColor)
    }

    @Test
    fun `LabelCellViewHolder CellStyle titleColor が Theme titleColor より優先される`() {
        val vh = LabelCellViewHolder.create(parent)
        val cellColor = Color(red = 1.0f, green = 0.0f, blue = 0.0f, alpha = 1.0f)
        val themeColor = Color(red = 0.0f, green = 0.0f, blue = 1.0f, alpha = 1.0f)
        vh.bind(
            LabelCell(title = "X", style = CellStyle(titleColor = cellColor)),
            Theme(cellTitleColor = themeColor),
        )
        val title = findFirstTextView(vh.itemView as android.view.ViewGroup)
        assertNotNull(title)
        assertEquals(cellColor.toArgb(), title!!.currentTextColor)
    }

    // MARK: - ヘルパ
    //
    // 改訂後（unify-cell-common-fields-via-shared-row-layout）では Cell の root View が
    // ConstraintLayout になり、子の構造が `iconView` (ImageView), `titleView` (TextView),
    // `descriptionView` (TextView), `valueTextView` (TextView), `accessoryHolder` (FrameLayout),
    // `hintTextView` (TextView) になった。
    // さらに fix-android-cell-width-allocation で本体行が `contentRow` (LinearLayout) に入れ子化し、
    // root 直下は `iconView` → `contentRow`(→ `titleView`, `valueTextView`) → `descriptionView`
    // → `accessoryHolder` → `hintTextView` になった。
    // ヘルパは汎用的に descendant を探索する形に変更する。

    /**
     * ViewGroup の descendants（子孫）を深さ優先の**行きがけ順**で列挙するシーケンス。
     *
     * 行きがけ順（pre-order）であることが重要: 本体行の入れ子化により `titleView` は
     * `contentRow` の子になったため、幅優先で列挙すると root 直下の `descriptionView` が
     * `titleView` より先に現れてしまう。
     */
    private fun descendants(root: android.view.ViewGroup): Sequence<View> = sequence {
        for (i in 0 until root.childCount) {
            val v = root.getChildAt(i)
            yield(v)
            if (v is android.view.ViewGroup) {
                yieldAll(descendants(v))
            }
        }
    }

    /**
     * Cell root の descendants の中から「title 用の TextView」を返す。
     * CellBaseViews の構造では `titleView` が 1 つ目の TextView になる
     * （description / valueText / hintText は visibility = GONE の場合がある）。
     * 最初に出現する TextView を返す（`contentRow` 内の `valueTextView` や、root 直下の
     * `descriptionView` / `hintTextView` より上にある）。
     */
    private fun findFirstTextView(container: android.view.ViewGroup): TextView? {
        return descendants(container).firstOrNull { it is TextView } as TextView?
    }

    /**
     * accessoryHolder 内の [AppCompatImageView] を見つける（disclosure 表示用）。
     *
     * 注意: CellBaseViews 構造では root 直下に `iconView`（AppCompatImageView）もある。
     * disclosure View は `accessoryHolder` の子なので、`iconView`（root の最初の AppCompatImageView）
     * を除外して 2 つ目以降の AppCompatImageView を返す。
     */
    private fun findDisclosure(container: android.view.ViewGroup): AppCompatImageView? {
        var skipFirstIcon = true
        for (v in descendants(container)) {
            if (v is AppCompatImageView) {
                if (skipFirstIcon) {
                    // iconView をスキップ
                    skipFirstIcon = false
                    continue
                }
                return v
            }
        }
        return null
    }

    /**
     * accessoryHolder 内の [KsSimpleCheckView] を返す（RadioCell / SimpleCheckCell の選択表示）。
     */
    private fun findSimpleCheck(container: android.view.ViewGroup): KsSimpleCheckView? {
        return descendants(container).firstOrNull { it is KsSimpleCheckView } as KsSimpleCheckView?
    }

    private fun findMaterialSwitch(container: android.view.ViewGroup): MaterialSwitch? {
        return descendants(container).firstOrNull { it is MaterialSwitch } as MaterialSwitch?
    }

    /**
     * accessoryHolder 内のチェックボックス（[MaterialCheckBox]、`AppCompatCheckBox` 互換）を返す。
     */
    private fun findCheckBox(container: android.view.ViewGroup): AppCompatCheckBox? {
        return descendants(container).firstOrNull { it is AppCompatCheckBox } as AppCompatCheckBox?
    }

    /** accessoryHolder 内の [MaterialCheckBox] を返す。 */
    @Suppress("unused")
    private fun findMaterialCheckBox(container: android.view.ViewGroup): MaterialCheckBox? {
        return descendants(container).firstOrNull { it is MaterialCheckBox } as MaterialCheckBox?
    }

    @Suppress("unused")
    private fun keepBuildersAlive() {
        // 未使用警告抑制（fixture 関数は分割テストで使う）
        assertFalse(false)
    }

    // ============================================================================
    // refine-basic-cells-sample-layout: SwitchCell の Thumb / Track 色分離
    // ============================================================================

    /**
     * `SwitchCell` の Track には実効 accent 色が設定される。
     */
    @Test
    fun `SwitchCellViewHolder で trackTintList に accent 色が設定される`() {
        val vh = SwitchCellViewHolder.create(parent)
        val accent = Color(1.0f, 0.5f, 0.0f, 1.0f)
        vh.bind(
            SwitchCell(title = "通知", isOn = true),
            Theme(cellAccentColor = accent),
        )
        val sw = findMaterialSwitch(vh.itemView as android.view.ViewGroup)
        assertNotNull(sw)
        // trackTintList は状態別 ColorStateList で設定される
        // （オン時 = accent そのもの、オフ時 = accent から導出した淡色）
        val trackList = sw!!.trackTintList
        assertNotNull("trackTintList が設定される", trackList)
    }

    /**
     * `SwitchCell` の Track は state_checked = true で実効 accent、state_checked = false で
     * accent から導出した淡色となり、オン時とオフ時で色が分離する。
     *
     * 移植元 AiForms の `SwitchCellView.cs` と同じ Thumb / Track 色分離を保つ。
     */
    @Test
    fun `SwitchCellViewHolder で trackTintList が状態別に色を分離する`() {
        val vh = SwitchCellViewHolder.create(parent)
        val accent = Color(1.0f, 0.5f, 0.0f, 1.0f)
        vh.bind(
            SwitchCell(title = "通知", isOn = true),
            Theme(cellAccentColor = accent),
        )
        val sw = findMaterialSwitch(vh.itemView as android.view.ViewGroup)
        assertNotNull(sw)
        val trackList = sw!!.trackTintList!!
        val checkedColor = trackList.getColorForState(intArrayOf(android.R.attr.state_checked), 0)
        val uncheckedColor =
            trackList.getColorForState(intArrayOf(-android.R.attr.state_checked), 0)
        assertNotEquals(
            "Track の checked 色（accent）と unchecked 色（accent 由来の淡色）は異なる",
            checkedColor,
            uncheckedColor,
        )
    }

    /**
     * `SwitchCell` の Thumb には Track と独立した ColorStateList が設定される（同色塗りではない）。
     *
     * オン時は accent に対するコントラスト色（通常は白）、オフ時は accent の色相を
     * `colorOutline` の明度に載せた減彩色で、いずれもテーマの `colorOnPrimary` は参照しない。
     */
    @Test
    fun `SwitchCellViewHolder で thumbTintList に状態別 ColorStateList が設定される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(title = "通知", isOn = true),
            Theme(cellAccentColor = Color(1.0f, 0.5f, 0.0f, 1.0f)),
        )
        val sw = findMaterialSwitch(vh.itemView as android.view.ViewGroup)
        assertNotNull(sw)
        val thumbList = sw!!.thumbTintList
        assertNotNull("thumbTintList が設定される", thumbList)
        // Thumb は state_checked で異なる色が出るはず（同一塗りではない）
        val checkedColor = thumbList!!.getColorForState(intArrayOf(android.R.attr.state_checked), 0)
        val uncheckedColor =
            thumbList.getColorForState(intArrayOf(-android.R.attr.state_checked), 0)
        assertNotEquals(
            "Thumb の checked 色と unchecked 色は異なる（Track の accent 色とは独立）",
            checkedColor,
            uncheckedColor,
        )
    }

    /**
     * オフ時の Track と Thumb が **異なる色** で塗られていることを検証する。
     *
     * オフ時に両者を同じ導出で塗ると同色化して輪郭が見えなくなるため、色相は共通の accent から
     * 取りつつ、**明度の土台に別のテーマ attr を使う**ことで分離する
     * （Track = `colorSurfaceContainerHighest` / Thumb = `colorOutline`）。この土台の違いが
     * 両者の主な色差であり、ダークテーマで attr が反転したときの明度関係もここで担保される。
     * 本テストは「オフ時 Track の色 != オフ時 Thumb の色」であることを保証する。
     */
    @Test
    fun `SwitchCellViewHolder でオフ時の Track 色と Thumb 色が等しくない`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(title = "通知", isOn = false),
            Theme(cellAccentColor = Color(1.0f, 0.5f, 0.0f, 1.0f)),
        )
        val sw = findMaterialSwitch(vh.itemView as android.view.ViewGroup)
        assertNotNull(sw)
        val unchecked = intArrayOf(-android.R.attr.state_checked)
        val offTrackColor = sw!!.trackTintList!!.getColorForState(unchecked, 0)
        val offThumbColor = sw.thumbTintList!!.getColorForState(unchecked, 0)
        assertNotEquals(
            "オフ時の Track 色（colorSurfaceContainerHighest の明度）と Thumb 色（colorOutline の明度）は" +
                "異なる色でなければならない（つまみの輪郭を保つため）",
            offTrackColor,
            offThumbColor,
        )
    }

    // ============================================================================
    // CheckboxCell の 24dp 明示サイズ
    // ============================================================================

    /**
     * `CheckboxCell` の `MaterialCheckBox` には 24dp × 24dp の `LayoutParams` が設定される。
     *
     * 明示サイズを与えることで、他の Cell の右端アクセサリと X 座標が揃う。
     */
    @Test
    fun `CheckboxCellViewHolder で 24dp 明示サイズが適用される`() {
        val vh = CheckboxCellViewHolder.create(parent)
        vh.bind(CheckboxCell(title = "X", isChecked = false), Theme())
        val cb = findMaterialCheckBox(vh.itemView as android.view.ViewGroup)
        assertNotNull(cb)
        val expectedPx = (24 * ctx.resources.displayMetrics.density).toInt()
        assertEquals(expectedPx, cb!!.layoutParams.width)
        assertEquals(expectedPx, cb.layoutParams.height)
    }

    // ============================================================================
    // KsImage 派生のアイコン解決
    // ============================================================================

    /**
     * `KsImage.Resource` を指定した `LabelCell` は ImageView を VISIBLE にする。
     */
    @Test
    fun `LabelCellViewHolder で KsImage_Resource が描画される`() {
        val vh = LabelCellViewHolder.create(parent)
        // android.R.drawable.ic_menu_help は標準 framework 由来の Drawable で必ず解決可能
        vh.bind(
            LabelCell(
                title = "with icon",
                icon = KsImage.Resource(
                    android.R.drawable.ic_menu_help,
                ),
            ),
            Theme(),
        )
        val container = vh.itemView as android.view.ViewGroup
        val icon = findIconView(container)
        assertNotNull(icon)
        assertEquals(View.VISIBLE, icon!!.visibility)
        assertNotNull("Resource 派生は drawable に解決される", icon.drawable)
    }

    /**
     * `KsImage.Drawable` 派生は渡された Drawable をそのまま設定する。
     */
    @Test
    fun `LabelCellViewHolder で KsImage_Drawable が描画される`() {
        val vh = LabelCellViewHolder.create(parent)
        val drawable = android.graphics.drawable.ColorDrawable(0xFF112233.toInt())
        vh.bind(
            LabelCell(
                title = "with drawable",
                icon = KsImage.Drawable(drawable),
            ),
            Theme(),
        )
        val container = vh.itemView as android.view.ViewGroup
        val icon = findIconView(container)
        assertNotNull(icon)
        assertEquals(View.VISIBLE, icon!!.visibility)
        assertTrue("Drawable 派生は渡されたインスタンスがそのまま設定される", icon.drawable === drawable)
    }

    /**
     * `KsImage.SystemName` 派生は Android では解決不可、ImageView は GONE にフォールバック。
     */
    @Test
    fun `LabelCellViewHolder で KsImage_SystemName は View_GONE にフォールバック`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(
            LabelCell(
                title = "with sf symbol",
                icon = KsImage.SystemName("bell"),
            ),
            Theme(),
        )
        val container = vh.itemView as android.view.ViewGroup
        val icon = findIconView(container)
        assertNotNull(icon)
        assertEquals(
            "SystemName は Android で解決不可、View.GONE にフォールバックする",
            View.GONE,
            icon!!.visibility,
        )
    }

    /** container の descendants から [AppCompatImageView]（アイコン用）を返す。 */
    private fun findIconView(container: android.view.ViewGroup): AppCompatImageView? {
        // iconView は CellBaseViews 構造の最初の AppCompatImageView。
        return descendants(container).firstOrNull { it is AppCompatImageView } as AppCompatImageView?
    }

    // -------------------------------------------------------------------------
    // 基本 Cell 共通の行余白（横 16dp / 縦 4dp、縦は移植元 AiForms の cellbaseview.axml と同値）。
    // root は無余白で、余白は内容側 View のマージンが持つ。
    // -------------------------------------------------------------------------

    /** [views] の root からの実効オフセットとして (左, 上, 右, 下) の行余白を返す。 */
    private fun rowMarginsOf(views: CellBaseViews): IntArray {
        val iconLp = views.iconView.layoutParams as ConstraintLayout.LayoutParams
        val contentLp = views.contentRow.layoutParams as ConstraintLayout.LayoutParams
        val accessoryLp = views.accessoryHolder.layoutParams as ConstraintLayout.LayoutParams
        val descLp = views.descriptionView.layoutParams as ConstraintLayout.LayoutParams
        return intArrayOf(
            iconLp.marginStart,
            contentLp.topMargin,
            accessoryLp.marginEnd,
            descLp.bottomMargin,
        )
    }

    @Test
    fun `LabelCellViewHolder の行余白が root の padding ではなく内容側のマージンで構築される`() {
        val parent = FrameLayout(ctx)
        val views = buildCellBaseViews(parent)
        val density = ctx.resources.displayMetrics.density
        val expected4 = (4 * density).toInt()
        val expected16 = (16 * density).toInt()

        assertEquals("root は無余白（上）", 0, views.root.paddingTop)
        assertEquals("root は無余白（下）", 0, views.root.paddingBottom)
        assertEquals("root は無余白（左）", 0, views.root.paddingLeft)
        assertEquals("root は無余白（右）", 0, views.root.paddingRight)

        val margins = rowMarginsOf(views)
        assertEquals("左余白は 16dp（icon の START マージン）", expected16, margins[0])
        assertEquals("上余白は 4dp（contentRow の TOP マージン）", expected4, margins[1])
        assertEquals("右余白は 16dp（accessoryHolder の END マージン）", expected16, margins[2])
        assertEquals("下余白は 4dp（descriptionView の BOTTOM マージン）", expected4, margins[3])
    }

    @Test
    fun `ButtonCellViewHolder の行余白が root の padding ではなく内容側のマージンで構築される`() {
        val parent = FrameLayout(ctx)
        val holder = ButtonCellViewHolder.create(parent)
        val root = holder.views.root
        val density = ctx.resources.displayMetrics.density
        val expected4 = (4 * density).toInt()
        val expected16 = (16 * density).toInt()

        assertEquals("root は無余白（上）", 0, root.paddingTop)
        assertEquals("root は無余白（下）", 0, root.paddingBottom)
        assertEquals("root は無余白（左）", 0, root.paddingLeft)
        assertEquals("root は無余白（右）", 0, root.paddingRight)

        val margins = rowMarginsOf(holder.views)
        assertEquals("左余白は 16dp", expected16, margins[0])
        assertEquals("上余白は 4dp", expected4, margins[1])
        assertEquals("右余白は 16dp", expected16, margins[2])
        assertEquals("下余白は 4dp", expected4, margins[3])
    }

    /**
     * ボタンスタイル（icon / valueText / hintText なし）でも、contentRow は行余白の内側に収まる。
     * root が無余白になったため、この余白は buttonStyleSet のマージンが担う。
     */
    @Test
    fun `ButtonCell のボタンスタイルでも contentRow は行余白の内側に収まる`() {
        val parent = FrameLayout(ctx)
        val holder = ButtonCellViewHolder.create(parent)
        holder.bind(ButtonCell(title = "ログアウト"), Theme())

        val density = ctx.resources.displayMetrics.density
        val expected4 = (4 * density).toInt()
        val expected16 = (16 * density).toInt()
        val lp = holder.views.contentRow.layoutParams as ConstraintLayout.LayoutParams

        assertEquals("左余白は 16dp", expected16, lp.marginStart)
        assertEquals("右余白は 16dp", expected16, lp.marginEnd)
        assertEquals("上余白は 4dp", expected4, lp.topMargin)
        assertEquals("下余白は 4dp", expected4, lp.bottomMargin)
    }

    /**
     * ボタンスタイルへ切り替えても本体行の光学中心補正が残る。
     *
     * `ConstraintSet.applyTo` は制約だけでなく translation も設定値で上書きするため、
     * 切替用の constraint set が補正値を持っていないと補正が 0 に戻る。
     */
    @Test
    fun `ButtonCell のボタンスタイルでも本体行の光学中心補正が残る`() {
        val parent = FrameLayout(ctx)
        val holder = ButtonCellViewHolder.create(parent)
        val density = ctx.resources.displayMetrics.density
        val expected = CELL_ROW_OPTICAL_CENTER_OFFSET_DP * density

        holder.bind(ButtonCell(title = "ログアウト"), Theme())
        assertEquals(
            "ボタンスタイルでも contentRow の translationY は光学補正値",
            expected,
            holder.views.contentRow.translationY,
            0.001f,
        )

        // aux ありの通常レイアウトへ復帰しても補正は保たれる。
        holder.bind(ButtonCell(title = "ログアウト", valueText = "実行"), Theme())
        assertEquals(
            "通常レイアウトへ復帰しても contentRow の translationY は光学補正値",
            expected,
            holder.views.contentRow.translationY,
            0.001f,
        )
    }
}
