package jp.kamusoft.kssettingsview.ui

import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cell 共通フィールド（`description` / `valueText` / `icon` / `hintText` / `accentColor`）と、
 * それらを描画する ConstraintLayout ベースの共通行（`CellBaseViews` +
 * `applyCellBaseLayout(views, ...)`）の振る舞いを検証する。各 Cell は個別に描画するのではなく
 * この共通行を経由する（core/ADR-0011）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UnifyCellCommonFieldsTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )
    private val parent get() = FrameLayout(ctx)

    // MARK: - equals / hashCode に共通フィールドが反映される

    @Test
    fun `SwitchCell equals は追加フィールドを判定対象に含む`() {
        val id = "fixed-id"
        val a = SwitchCell(id = id, title = "x", valueText = "A", isOn = true)
        val b = SwitchCell(id = id, title = "x", valueText = "B", isOn = true)
        assertNotEquals(a, b)
        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `CheckboxCell equals は追加フィールドを判定対象に含む`() {
        val id = "fixed-id"
        val a = CheckboxCell(id = id, title = "x", icon = KsImage.Resource(1))
        val b = CheckboxCell(id = id, title = "x", icon = KsImage.Resource(2))
        assertNotEquals(a, b)
    }

    @Test
    fun `RadioCell equals は description と accentColor を判定対象に含む`() {
        val id = "fixed-id"
        val a = RadioCell(
            id = id,
            title = "x",
            description = "d1",
            groupId = "g",
            value = "v",
            selectedValue = "v",
            accentColor = Color.Red,
        )
        val b = RadioCell(
            id = id,
            title = "x",
            description = "d2",
            groupId = "g",
            value = "v",
            selectedValue = "v",
            accentColor = Color.Red,
        )
        assertNotEquals(a, b)

        val c = RadioCell(
            id = id,
            title = "x",
            description = "d1",
            groupId = "g",
            value = "v",
            selectedValue = "v",
            accentColor = Color.Blue,
        )
        assertNotEquals(a, c)
    }

    @Test
    fun `SimpleCheckCell equals は accentColor を判定対象に含む`() {
        val id = "fixed-id"
        val a = SimpleCheckCell(id = id, title = "x", isChecked = true, accentColor = Color.Red)
        val b = SimpleCheckCell(id = id, title = "x", isChecked = true, accentColor = Color.Green)
        assertNotEquals(a, b)
    }

    @Test
    fun `ButtonCell equals は valueText icon hintText を判定対象に含む`() {
        val id = "fixed-id"
        val a = ButtonCell(id = id, title = "x", valueText = "A")
        val b = ButtonCell(id = id, title = "x", valueText = "B")
        assertNotEquals(a, b)

        val c = ButtonCell(id = id, title = "x", icon = KsImage.Resource(1))
        val d = ButtonCell(id = id, title = "x", icon = KsImage.Resource(2))
        assertNotEquals(c, d)
    }

    // MARK: - withDSLId / withDSLStyle が追加フィールドを保持する

    @Test
    fun `SwitchCell withDSLId は追加フィールドを保持する`() {
        val orig = SwitchCell(
            title = "x",
            description = "d",
            valueText = "v",
            icon = KsImage.Resource(1),
            hintText = "h",
            isOn = true,
        )
        val copy = orig.withDSLId("new-id") as SwitchCell
        assertEquals("d", copy.description)
        assertEquals("v", copy.valueText)
        assertEquals(KsImage.Resource(1), copy.icon)
        assertEquals("h", copy.hintText)
    }

    @Test
    fun `RadioCell withDSLStyle は追加フィールドと accentColor を保持する`() {
        val orig = RadioCell(
            title = "x",
            description = "d",
            valueText = "v",
            icon = KsImage.Resource(1),
            hintText = "h",
            groupId = "g",
            value = "v",
            selectedValue = "v",
            accentColor = Color.Magenta,
        )
        val copy = orig.withDSLStyle(CellStyle()) as RadioCell
        assertEquals("d", copy.description)
        assertEquals(Color.Magenta, copy.accentColor)
    }

    @Test
    fun `ButtonCell には description プロパティが存在しない`() {
        // コンパイル時テスト: 以下の呼び出しはコンパイルエラーになるべきである。
        // ButtonCell(title = "x", description = "X")  // NG: description は存在しない
        val cell = ButtonCell(title = "x", valueText = "A")
        assertEquals("A", cell.valueText)
    }

    // MARK: - ViewHolder bind が新フィールドを反映する（CellBaseViews 経由）

    @Test
    fun `SwitchCellViewHolder bind で valueText icon hintText が反映される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(
                title = "通知",
                description = "プッシュ通知",
                valueText = "オン",
                icon = KsImage.Resource(android.R.drawable.ic_dialog_info),
                hintText = "推奨",
                isOn = true,
            ),
            Theme(),
        )
        assertEquals("オン", vh.views.valueTextView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.valueTextView.visibility)
        assertEquals("推奨", vh.views.hintTextView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.hintTextView.visibility)
        assertEquals(android.view.View.VISIBLE, vh.views.iconView.visibility)
    }

    @Test
    fun `RadioCellViewHolder bind で description が反映される`() {
        val vh = RadioCellViewHolder.create(parent)
        vh.bind(
            RadioCell(
                title = "ダーク",
                description = "暗い背景",
                groupId = "theme",
                value = "dark",
                selectedValue = "dark",
            ),
            Theme(),
        )
        assertEquals("暗い背景", vh.views.descriptionView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.descriptionView.visibility)
    }

    @Test
    fun `RadioCell accentColor が Cell 個別で最優先される`() {
        val vh = RadioCellViewHolder.create(parent)
        val theme = Theme(cellAccentColor = Color.Blue)
        vh.bind(
            RadioCell(
                title = "x",
                groupId = "g",
                value = "v",
                selectedValue = "v",
                accentColor = Color.Red,
            ),
            theme,
        )
        // 内部 KsSimpleCheckView.color が Color.Red.toArgb() になっているはず
        val container = vh.itemView as android.view.ViewGroup
        val checkView = findKsSimpleCheckView(container)
        assertEquals(Color.Red.toArgb(), checkView?.color)
    }

    @Test
    fun `SimpleCheckCell accentColor が Cell 個別で最優先される`() {
        val vh = SimpleCheckCellViewHolder.create(parent)
        val theme = Theme(cellAccentColor = Color.Blue)
        vh.bind(
            SimpleCheckCell(
                title = "x",
                isChecked = true,
                accentColor = Color.Red,
            ),
            theme,
        )
        val container = vh.itemView as android.view.ViewGroup
        val checkView = findKsSimpleCheckView(container)
        assertEquals(Color.Red.toArgb(), checkView?.color)
    }

    // MARK: - 既存呼び出しの互換性

    @Test
    fun `既存呼び出しは追加フィールド未指定で破壊されない`() {
        SwitchCell(title = "通知", isOn = true)
        CheckboxCell(title = "規約", isChecked = false)
        RadioCell(title = "ダーク", groupId = "theme", value = "dark", selectedValue = "dark")
        SimpleCheckCell(title = "通知1")
        ButtonCell(title = "ログアウト", onTap = {})
    }

    // MARK: - ButtonCellViewHolder の aux 切替（CellBaseViews 経由）
    //
    // aux（icon / valueText / hintText）がすべて未指定ならボタンスタイル、
    // いずれか指定なら通常レイアウトへ切り替わる。

    @Test
    fun `ButtonCellViewHolder aux 未指定はボタンスタイルで titleView のみが描画される`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "ログアウト"), Theme())
        // ボタンスタイル: titleView のみ VISIBLE、他は GONE。
        assertEquals(android.view.View.VISIBLE, vh.views.titleView.visibility)
        assertEquals(android.view.View.GONE, vh.views.iconView.visibility)
        assertEquals(android.view.View.GONE, vh.views.descriptionView.visibility)
        assertEquals(android.view.View.GONE, vh.views.valueTextView.visibility)
        assertEquals(android.view.View.GONE, vh.views.hintTextView.visibility)
        assertEquals(android.view.View.GONE, vh.views.accessoryHolder.visibility)
        assertEquals("ログアウト", vh.views.titleView.text.toString())
    }

    @Test
    fun `ButtonCellViewHolder valueText 指定で通常レイアウトに切り替わり valueText が反映される`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "登録", valueText = "送信"), Theme())
        // 通常レイアウト: applyCellBaseLayout 経由で titleView と valueTextView が VISIBLE。
        assertEquals("登録", vh.views.titleView.text.toString())
        assertEquals("送信", vh.views.valueTextView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.valueTextView.visibility)
    }

    @Test
    fun `ButtonCellViewHolder icon 指定で通常レイアウトに切り替わり iconView が VISIBLE になる`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(
            ButtonCell(
                title = "編集",
                icon = KsImage.Resource(android.R.drawable.ic_dialog_info),
            ),
            Theme(),
        )
        assertEquals(android.view.View.VISIBLE, vh.views.iconView.visibility)
    }

    @Test
    fun `ButtonCellViewHolder hintText 指定で通常レイアウトに切り替わり hintText が反映される`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "更新", hintText = "推奨"), Theme())
        assertEquals("推奨", vh.views.hintTextView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.hintTextView.visibility)
    }

    @Test
    fun `ButtonCellViewHolder aux 指定時 titleAlignment は title 列内 gravity に反映される`() {
        val vh = ButtonCellViewHolder.create(parent)
        // titleAlignment = END、aux 指定（hintText）
        vh.bind(
            ButtonCell(
                title = "削除",
                hintText = "危険",
                titleAlignment = jp.kamusoft.kssettingsview.core.CellTitleAlignment.END,
            ),
            Theme(),
        )
        val titleGravity = vh.views.titleView.gravity
        org.junit.Assert.assertTrue(
            "title 列の gravity に END が含まれる",
            (titleGravity and android.view.Gravity.END) != 0,
        )
    }

    @Test
    fun `ButtonCellViewHolder aux 指定時 onTap は root container 側で発火する`() {
        val vh = ButtonCellViewHolder.create(parent)
        var called = 0
        vh.bind(
            ButtonCell(title = "X", valueText = "v", onTap = { called++ }),
            Theme(),
        )
        // 通常レイアウト時は root 側にクリックリスナが設定される。
        vh.views.root.performClick()
        assertEquals(1, called)
    }

    // MARK: - LabelCell 経由で description / valueText / icon / hintText の各表示有無

    @Test
    fun `LabelCell 経由で description が null のとき description View は GONE になる`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "x", description = null), Theme())
        assertEquals(android.view.View.GONE, vh.views.descriptionView.visibility)
    }

    @Test
    fun `LabelCell 経由で description が指定されているとき description が表示される`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "x", description = "副題"), Theme())
        assertEquals("副題", vh.views.descriptionView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.descriptionView.visibility)
    }

    @Test
    fun `LabelCell 経由で hintText が指定されているとき hintText が表示される`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "x", hintText = "注意"), Theme())
        assertEquals("注意", vh.views.hintTextView.text.toString())
        assertEquals(android.view.View.VISIBLE, vh.views.hintTextView.visibility)
    }

    // MARK: - CellBaseViews ConstraintLayout 配置の回帰テスト
    //
    // hintTextView は右上に float 配置され、accessoryHolder と重ならない。

    /**
     * hintTextView が cell 外縁から 上 2dp / 右 10dp の位置へ float 配置される
     * （iOS の hintLabel と同じ実距離。root は無余白で、行の余白は内容側のマージンが持つ）。
     */
    @Test
    fun `hintTextView は cell 外縁から 上2dp 右10dp に float 配置される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", hintText = "推奨", isOn = true), Theme())
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val hint = vh.views.hintTextView
        val density = ctx.resources.displayMetrics.density
        // hintTextView.top は cell 上端から 2dp（±1px 以内）
        val expectedTop = (2 * density).toInt()
        org.junit.Assert.assertTrue(
            "hintTextView.top(${hint.top}) が予想($expectedTop)から ±1px 以内",
            kotlin.math.abs(hint.top - expectedTop) <= 1,
        )
        // hintTextView.right は cell 右端から 10dp（±1px 以内）
        val expectedRightOffset = (10 * density).toInt()
        val actualRightOffset = root.measuredWidth - hint.right
        org.junit.Assert.assertTrue(
            "hintTextView の右オフセット($actualRightOffset) が予想($expectedRightOffset)から ±1px 以内",
            kotlin.math.abs(actualRightOffset - expectedRightOffset) <= 1,
        )
    }

    /**
     * hintTextView は下端ガードを持ち、cell 下端から 12dp 以上あける。
     *
     * 通常の行高では上端 2dp 基準の配置が保たれ（`verticalBias = 0`）、
     * 下端ガードは `BOTTOM` 制約として常に張られている。
     *
     * ガードが実際に高さを縮める局面は `CellRowWidthAllocationTest` が受け持つ。本クラスの
     * legacy graphics では TextView の測定高さが `textSize` に追随せず、ガードに触れる状況を
     * 作れないため、ここでは制約そのものが張られていることを検証する。
     */
    @Test
    fun `hintTextView は cell 下端から 12dp の下端ガードを持つ`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", hintText = "推奨", isOn = true), Theme())
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val hint = vh.views.hintTextView
        val density = ctx.resources.displayMetrics.density
        val lp = hint.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        assertEquals(
            "hintTextView の BOTTOM は parent に接続される",
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID,
            lp.bottomToBottom,
        )
        assertEquals(
            "下端ガードは 12dp",
            (12 * density).toInt(),
            lp.bottomMargin,
        )
        assertEquals("上端基準を保つため verticalBias は 0", 0.0f, lp.verticalBias, 0.0001f)
        org.junit.Assert.assertTrue(
            "wrap_content の高さが制約に従う（constrainedHeight）",
            lp.constrainedHeight,
        )

        // 通常の行高では下端ガードに触れず、上端 2dp 基準の配置が保たれる。
        val expectedTop = (2 * density).toInt()
        org.junit.Assert.assertTrue(
            "hintTextView.top(${hint.top}) が上端基準($expectedTop)から ±1px 以内",
            kotlin.math.abs(hint.top - expectedTop) <= 1,
        )
        org.junit.Assert.assertTrue(
            "hintTextView.bottom(${hint.bottom}) は cell 下端 ${root.measuredHeight} から 12dp 以上内側",
            root.measuredHeight - hint.bottom >= (12 * density).toInt(),
        )
    }

    /**
     * 10.R.2: accessoryHolder がセル縦中央配置（CenterVertical）。
     */
    @Test
    fun `accessoryHolder はセル縦中央配置`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", isOn = true), Theme())
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val accessory = vh.views.accessoryHolder
        val rootCenter = (root.top + root.bottom) / 2
        val accessoryCenter = (accessory.top + accessory.bottom) / 2
        org.junit.Assert.assertTrue(
            "accessoryHolder.centerY($accessoryCenter) ≒ root.centerY($rootCenter), ±1px 以内",
            kotlin.math.abs(accessoryCenter - rootCenter) <= 1,
        )
    }

    /**
     * description が無い行でも本体行が縦中央に置かれ、アクセサリと縦位置が揃う。
     *
     * 縦の余白は vertical chain の両端（`contentRow` の TOP と `descriptionView` の BOTTOM）が
     * 持つが、`descriptionView` が GONE の行では下端側のマージンが消える。補われていないと
     * 上下非対称の領域で packed 配置され、本体行だけが下へずれてアクセサリと食い違う。
     */
    @Test
    fun `description が無い行でも本体行はアクセサリと同じ縦中央に置かれる`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", description = null, isOn = true), Theme())
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val root = vh.views.root
        root.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(
                width,
                android.view.View.MeasureSpec.EXACTLY,
            ),
            android.view.View.MeasureSpec.makeMeasureSpec(
                0,
                android.view.View.MeasureSpec.UNSPECIFIED,
            ),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        assertEquals(
            "この行では descriptionView が GONE",
            android.view.View.GONE,
            vh.views.descriptionView.visibility,
        )

        val contentRow = vh.views.contentRow
        val rootCenter = (root.top + root.bottom) / 2f
        val contentCenter = (contentRow.top + contentRow.bottom) / 2f
        org.junit.Assert.assertTrue(
            "contentRow.centerY($contentCenter) ≒ root.centerY($rootCenter), ±1px 以内",
            kotlin.math.abs(contentCenter - rootCenter) <= 1f,
        )

        // 縦中央のアクセサリと食い違わないこと（本体行の縦位置はアクセサリを基準に揃える）。
        val accessory = vh.views.accessoryHolder
        val accessoryCenter = (accessory.top + accessory.bottom) / 2f
        org.junit.Assert.assertTrue(
            "contentRow.centerY($contentCenter) ≒ accessoryHolder.centerY($accessoryCenter), ±1px 以内",
            kotlin.math.abs(contentCenter - accessoryCenter) <= 1f,
        )
    }

    /**
     * hintTextView は accessoryHolder との Z 順で前面に置かれている（重なっても hintText
     * が見える）。両者の物理的な重なりは許容するが、Z 順では hintText を必ず前面に置く。
     *
     * 物理的な縦方向分離は実機で十分なセル高さ（48dp 最低保証）が確保される前提で成立するが、
     * Robolectric の measure では cell height が反映されない（パディング+title 行 1 行のみ）ため、
     * ここでは「Z 順前面」を検証する（addView 順序の最後に hintTextView が来ている）。
     */
    @Test
    fun `hintTextView は accessoryHolder より Z 順で前面に配置される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", hintText = "推奨", isOn = true), Theme())
        val root = vh.views.root
        // root の子の中での hintTextView のインデックスが accessoryHolder より後ろであることを確認
        val hintIndex = root.indexOfChild(vh.views.hintTextView)
        val accessoryIndex = root.indexOfChild(vh.views.accessoryHolder)
        org.junit.Assert.assertTrue(
            "hintTextView ($hintIndex) は accessoryHolder ($accessoryIndex) より後ろの addView 順序にある",
            hintIndex > accessoryIndex,
        )
    }

    /**
     * 10.R.4: visibility 制御（null → GONE、非 null → VISIBLE）。
     */
    @Test
    fun `applyCellBaseLayout は各 View の visibility を null per null で制御する`() {
        val vh = LabelCellViewHolder.create(parent)
        // すべて null のケース
        vh.bind(LabelCell(title = "x"), Theme())
        assertEquals(android.view.View.GONE, vh.views.descriptionView.visibility)
        assertEquals(android.view.View.GONE, vh.views.valueTextView.visibility)
        assertEquals(android.view.View.GONE, vh.views.hintTextView.visibility)
        assertEquals(android.view.View.GONE, vh.views.iconView.visibility)

        // すべて指定のケース
        vh.bind(
            LabelCell(
                title = "x",
                description = "d",
                valueText = "v",
                icon = KsImage.Resource(android.R.drawable.ic_dialog_info),
                hintText = "h",
            ),
            Theme(),
        )
        assertEquals(android.view.View.VISIBLE, vh.views.descriptionView.visibility)
        assertEquals(android.view.View.VISIBLE, vh.views.valueTextView.visibility)
        assertEquals(android.view.View.VISIBLE, vh.views.hintTextView.visibility)
        assertEquals(android.view.View.VISIBLE, vh.views.iconView.visibility)
    }

    /**
     * 10.R.5: 各 ViewHolder が CellBaseViews を views プロパティで保持していることを確認
     * （applyCellBaseLayout 経由の描画の前提）。
     */
    @Test
    fun `各 ViewHolder は views プロパティで CellBaseViews を保持する`() {
        val labelVh = LabelCellViewHolder.create(parent)
        assertEquals(labelVh.views.root, labelVh.itemView)

        val cmdVh = CommandCellViewHolder.create(parent)
        assertEquals(cmdVh.views.root, cmdVh.itemView)

        val swVh = SwitchCellViewHolder.create(parent)
        assertEquals(swVh.views.root, swVh.itemView)

        val cbVh = CheckboxCellViewHolder.create(parent)
        assertEquals(cbVh.views.root, cbVh.itemView)

        val rdVh = RadioCellViewHolder.create(parent)
        assertEquals(rdVh.views.root, rdVh.itemView)

        val scVh = SimpleCheckCellViewHolder.create(parent)
        assertEquals(scVh.views.root, scVh.itemView)

        val btVh = ButtonCellViewHolder.create(parent)
        assertEquals(btVh.views.root, btVh.itemView)
    }

    /**
     * 10.R.6: Compose 版 KsCellRow.kt 不在の確認。
     * `androidx.compose.runtime.Composable` を共通行レイアウトとして利用する `KsCellRow` 関数が
     * 存在しないことを class loader で確認する（ファイル削除済みのため、関連 class が見つからない）。
     */
    @Test
    fun `KsCellRow Composable は削除されている`() {
        // KsCellRowLayoutKt は旧 Compose 版 `KsCellRow` の Kotlin file class 名。削除済み。
        val cls = try {
            Class.forName("jp.kamusoft.kssettingsview.ui.KsCellRowLayoutKt")
        } catch (_: ClassNotFoundException) {
            null
        }
        org.junit.Assert.assertNull("KsCellRowLayoutKt（Compose 版 KsCellRow）class は存在しないはず", cls)
    }

    // MARK: - 右端アクセサリ X 座標整列の回帰テスト

    @Test
    fun `Switch CheckBox Radio SimpleCheck の accessoryHolder 右端 X 座標が揃う`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )

        val theme = Theme()
        val rights = mutableListOf<Int>()

        // SwitchCell
        run {
            val vh = SwitchCellViewHolder.create(rootParent)
            vh.bind(SwitchCell(title = "通知", isOn = true), theme)
            val root = vh.views.root
            root.measure(widthSpec, heightSpec)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            rights += vh.views.accessoryHolder.right
        }

        // CheckboxCell
        run {
            val vh = CheckboxCellViewHolder.create(rootParent)
            vh.bind(CheckboxCell(title = "規約", isChecked = true), theme)
            val root = vh.views.root
            root.measure(widthSpec, heightSpec)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            rights += vh.views.accessoryHolder.right
        }

        // RadioCell
        run {
            val vh = RadioCellViewHolder.create(rootParent)
            vh.bind(
                RadioCell(title = "ダーク", groupId = "g", value = "v", selectedValue = "v"),
                theme,
            )
            val root = vh.views.root
            root.measure(widthSpec, heightSpec)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            rights += vh.views.accessoryHolder.right
        }

        // SimpleCheckCell
        run {
            val vh = SimpleCheckCellViewHolder.create(rootParent)
            vh.bind(SimpleCheckCell(title = "通知", isChecked = true), theme)
            val root = vh.views.root
            root.measure(widthSpec, heightSpec)
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            rights += vh.views.accessoryHolder.right
        }

        val minRight = rights.min()
        val maxRight = rights.max()
        org.junit.Assert.assertTrue(
            "右端 X 座標が ±1px 以内で揃う (min=$minRight max=$maxRight diff=${maxRight - minRight})",
            (maxRight - minRight) <= 1,
        )
    }

    // MARK: - 本体行 vertical chain（packed, bias 0.5）

    /**
     * `description == null`（GONE）のとき、titleView が cell 縦中央付近に配置されることを検証する。
     * vertical chain (packed, bias 0.5) + descriptionView が GONE chain member（スペース 0 として扱う）
     * という ConstraintLayout の挙動により、titleView 単独でも縦中央寄せが維持される。
     */
    @Test
    fun `description が GONE のとき titleView は縦中央付近に配置される`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        // セルに十分な高さ（80dp）を持たせて vertical chain の効果を観測する
        val height = (80 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            height,
            android.view.View.MeasureSpec.EXACTLY,
        )

        val vh = LabelCellViewHolder.create(rootParent)
        vh.bind(LabelCell(title = "通知"), Theme())
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // description は GONE であることを前提
        assertEquals(android.view.View.GONE, vh.views.descriptionView.visibility)

        // titleView の縦中心は root の縦中心 (root.height / 2) の近傍にあるはず
        val titleCenterY = (vh.views.titleTopInRoot() + vh.views.titleBottomInRoot()) / 2
        val rootCenterY = root.height / 2
        val tolerance = (8 * ctx.resources.displayMetrics.density).toInt() // 8dp 許容
        org.junit.Assert.assertTrue(
            "titleView の縦中心 ($titleCenterY) は root 縦中心 ($rootCenterY) から ±${tolerance}px 以内にあるはず",
            kotlin.math.abs(titleCenterY - rootCenterY) <= tolerance,
        )
    }

    /**
     * 4.6: `description != null`（VISIBLE）のとき、title + description のペアが cell 縦中央付近に
     * 配置されることを検証する。両者を結合した縦中心が root 縦中心の近傍にあれば良い。
     */
    @Test
    fun `description が VISIBLE のとき title と description は本体行縦中央付近に配置される`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val height = (80 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            height,
            android.view.View.MeasureSpec.EXACTLY,
        )

        val vh = SwitchCellViewHolder.create(rootParent)
        vh.bind(
            SwitchCell(
                title = "通知",
                description = "プッシュ通知を受信",
                isOn = true,
            ),
            Theme(),
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // descriptionView は VISIBLE
        assertEquals(android.view.View.VISIBLE, vh.views.descriptionView.visibility)

        // title + description の結合縦中心（title は contentRow の子なので root 座標系へ換算）
        val pairTop = vh.views.titleTopInRoot()
        val pairBottom = vh.views.descriptionView.bottom
        val pairCenterY = (pairTop + pairBottom) / 2
        val rootCenterY = root.height / 2
        val tolerance = (8 * ctx.resources.displayMetrics.density).toInt() // 8dp 許容
        org.junit.Assert.assertTrue(
            "title+description の縦中心 ($pairCenterY) は root 縦中心 ($rootCenterY) から ±${tolerance}px 以内にあるはず",
            kotlin.math.abs(pairCenterY - rootCenterY) <= tolerance,
        )
    }

    /**
     * 4.7: `valueText` が `titleView.BASELINE` に紐付くため、title 行と同じ縦位置にあることを検証する。
     * 厳密にはベースラインが揃うため、title の center と value の center は同じか、フォントの descent
     * 分のごく小さな差に収まる。
     */
    @Test
    fun `valueText は title のベースラインに揃って同じ縦位置に配置される`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val height = (80 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            height,
            android.view.View.MeasureSpec.EXACTLY,
        )

        val vh = SwitchCellViewHolder.create(rootParent)
        vh.bind(
            SwitchCell(
                title = "通知",
                valueText = "オン",
                isOn = true,
            ),
            Theme(),
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // valueTextView は VISIBLE
        assertEquals(android.view.View.VISIBLE, vh.views.valueTextView.visibility)

        // title と value の縦中心が近接（ベースライン紐付けなので同じテキスト系であれば誤差は数 px 程度）
        val titleCenterY = (vh.views.titleView.top + vh.views.titleView.bottom) / 2
        val valueCenterY = (vh.views.valueTextView.top + vh.views.valueTextView.bottom) / 2
        val tolerance = (6 * ctx.resources.displayMetrics.density).toInt() // 6dp 許容
        org.junit.Assert.assertTrue(
            "valueText 縦中心 ($valueCenterY) は title 縦中心 ($titleCenterY) から ±${tolerance}px 以内にあるはず",
            kotlin.math.abs(titleCenterY - valueCenterY) <= tolerance,
        )
    }

    // MARK: - refine-cell-layout-after-unify-review 修正 Phase: iconMarginEnd 実測検証
    //
    // codex 指摘: `iconView.layoutParams.marginEnd = iconMarginEnd` だけでは ConstraintLayout の
    // 対応 anchor が無いため余白が反映されない。代わりに titleView / descriptionView の
    // START = iconView.END に margin を渡している。実機での 16dp 余白を実測で確認する。

    /**
     * アイコンありの SwitchCell を measure / layout し、titleView.left - iconView.right が
     * `16dp 相当の px` であることを検証する（オリジナル踏襲の iOS 余白に合わせた 16dp）。
     */
    @Test
    fun `iconView と titleView の余白は 16dp 相当の px に等しい`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )

        val vh = SwitchCellViewHolder.create(rootParent)
        vh.bind(
            SwitchCell(
                title = "通知",
                icon = KsImage.Resource(android.R.drawable.ic_dialog_info),
                isOn = true,
            ),
            Theme(),
        )
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // iconView が VISIBLE であることを前提
        assertEquals(android.view.View.VISIBLE, vh.views.iconView.visibility)

        val density = ctx.resources.displayMetrics.density
        val expected = (16 * density).toInt()
        val actual = vh.views.titleLeftInRoot() - vh.views.iconView.right
        org.junit.Assert.assertTrue(
            "titleView.left - iconView.right ($actual px) が 16dp 相当 ($expected px) と ±1px 以内",
            kotlin.math.abs(actual - expected) <= 1,
        )
    }

    /**
     * アイコン無し（iconView = GONE）のとき、titleView.left は行左端の余白 16dp のところに来る
     * （icon との間の余白は goneMargin が行左端の余白へ置き換わって潰れる）。
     */
    @Test
    fun `アイコン無しのとき titleView は左端に張り付き iconMarginEnd 余白を持たない`() {
        val rootParent = FrameLayout(ctx)
        val width = (320 * ctx.resources.displayMetrics.density).toInt()
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            width,
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )

        val vh = SwitchCellViewHolder.create(rootParent)
        vh.bind(SwitchCell(title = "通知", isOn = true), Theme())
        val root = vh.views.root
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        // iconView が GONE であることを前提
        assertEquals(android.view.View.GONE, vh.views.iconView.visibility)

        // titleView.left（root 座標系）は行左端の余白 16dp のところにあるはず（START の goneMargin）
        val expected = (CELL_ROW_HORIZONTAL_MARGIN_DP * ctx.resources.displayMetrics.density).toInt()
        val actual = vh.views.titleLeftInRoot()
        org.junit.Assert.assertTrue(
            "アイコン無し時 titleView.left ($actual) は行左端の余白 ($expected) から ±1px 以内",
            kotlin.math.abs(actual - expected) <= 1,
        )
    }

    // MARK: - refine-cell-layout-after-unify-review 修正 Phase: 下限保証 60dp の回帰テスト

    /**
     * `Theme()` 引数なしで bind したとき、root view の `minimumHeight` が
     * `60dp` 相当の px に一致することを確認する（`applyEffectiveHeight` 適用後）。
     * `hasUnevenRows = true`（新デフォルト）なので `layoutParams.height = WRAP_CONTENT`、
     * `minimumHeight = 60dp` でちょうどオリジナル踏襲の挙動になる。
     */
    @Test
    fun `Theme 未指定時に root の minimumHeight は 60dp 相当の px になる`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", isOn = true), Theme())
        val density = ctx.resources.displayMetrics.density
        val expectedPx = (60 * density).toInt()
        val root = vh.views.root
        org.junit.Assert.assertEquals(
            "root.minimumHeight は 60dp 相当の px",
            expectedPx,
            root.minimumHeight,
        )
        // hasUnevenRows = true デフォルトのため layoutParams.height = WRAP_CONTENT
        org.junit.Assert.assertEquals(
            "layoutParams.height は WRAP_CONTENT",
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            root.layoutParams.height,
        )
    }

    // MARK: - MinHeightConstraintLayout の measure 検証

    /**
     * `Theme()` 引数なしで bind した SwitchCell を、親の `LinearLayoutManager` 相当
     * （`heightSpec = UNSPECIFIED`）で measure したとき、root の `measuredHeight` が 60dp 相当の
     * px 以上になることを確認する（`MinHeightConstraintLayout.onMeasure` の下限ガードが効くこと）。
     *
     * このテストは「`minimumHeight` を設定するだけだと標準 `ConstraintLayout` では measure 結果が
     * 反映されないケースがある」事象への回帰テストである（実機オーナー確認）。
     */
    @Test
    fun `Theme 未指定時の root measuredHeight は 60dp 相当の px 以上になる`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(SwitchCell(title = "通知", isOn = true), Theme())
        val density = ctx.resources.displayMetrics.density
        val expectedMinPx = (60 * density).toInt()
        val root = vh.views.root

        // 親（LinearLayoutManager 相当）から heightSpec = UNSPECIFIED で measure される状況を再現
        val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            (320 * density).toInt(),
            android.view.View.MeasureSpec.EXACTLY,
        )
        val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
            0,
            android.view.View.MeasureSpec.UNSPECIFIED,
        )
        root.measure(widthSpec, heightSpec)

        org.junit.Assert.assertTrue(
            "root.measuredHeight (${root.measuredHeight}) は 60dp 相当の px ($expectedMinPx) 以上",
            root.measuredHeight >= expectedMinPx,
        )
    }

    // MARK: - Helpers

    // fix-android-cell-width-allocation で本体行が `contentRow` (LinearLayout) へ入れ子化したため、
    // `titleView` / `valueTextView` の top / left は root ではなく `contentRow` からの相対座標になる。
    // root 直下の View（iconView / descriptionView / accessoryHolder）と比較するときは root 座標系へ
    // 換算する。

    /** `titleView.left` を root 座標系に換算した値。 */
    private fun CellBaseViews.titleLeftInRoot(): Int = contentRow.left + titleView.left

    /** `titleView.top` を root 座標系に換算した値。 */
    private fun CellBaseViews.titleTopInRoot(): Int = contentRow.top + titleView.top

    /** `titleView.bottom` を root 座標系に換算した値。 */
    private fun CellBaseViews.titleBottomInRoot(): Int = contentRow.top + titleView.bottom

    private fun findKsSimpleCheckView(container: android.view.ViewGroup): KsSimpleCheckView? {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is KsSimpleCheckView) return child
            if (child is android.view.ViewGroup) {
                val found = findKsSimpleCheckView(child)
                if (found != null) return found
            }
        }
        return null
    }
}
