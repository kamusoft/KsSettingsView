package jp.kamusoft.kssettingsview.ui

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.R as MaterialR
import jp.kamusoft.kssettingsview.core.CellTitleAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 共通行の主行幅配分（`contentRow` = 水平 LinearLayout + weight）を固定親幅のもとで検証する。
 *
 * 配分の構造は android/ADR-0002（水平 LinearLayout + weight）、幅が足りないときに誰が譲るかは
 * core/ADR-0026（title を守り行内 trailing を省略する）に従う。icon 領域と Cell 級アクセサリは
 * 主行より先に譲らない。Cell 級アクセサリと行内 trailing の 2 系統配置、および EntryCell の
 * 入力フィールドが行内に置かれることも併せて検証する。
 *
 * `@GraphicsMode(NATIVE)`: legacy graphics では `TextUtils.ellipsize` が動作せず
 * `Layout.getEllipsisCount` が常に 0 になるため、末尾省略を実レンダリングで検証できるよう
 * native graphics を使う。**legacy に戻すと [assertTruncatedAtEnd] が必ず落ちる。**
 * native graphics は Robolectric の nativeruntime アーティファクトを取得して実 Skia を動かすため、
 * legacy より起動コストとプラットフォーム依存が大きい（本リポジトリではこのクラスが唯一の使用箇所）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CellRowWidthAllocationTest {

    private val ctx: android.content.Context
        get() = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            MaterialR.style.Theme_Material3_Light_NoActionBar,
        )

    private val parent get() = FrameLayout(ctx)

    private val density get() = ctx.resources.displayMetrics.density

    /** 親幅を [widthDp] に固定して measure / layout する。 */
    private fun layoutRow(root: View, widthDp: Int = 320) =
        layoutRowPx(root, (widthDp * density).toInt())

    /** 親幅を [widthPx] に固定して measure / layout する。 */
    private fun layoutRowPx(root: View, widthPx: Int) {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    /**
     * 主行（`contentRow`）の幅が [targetRowWidth] になるよう root 幅を調整して layout し直す。
     *
     * 「行幅に収まらない」ケースを、テキスト計測系（Robolectric / 実機でフォント metrics が異なる）
     * に依存せず再現するために使う。呼び出し前に 1 度 layout 済みであること。
     */
    private fun relayoutWithRowWidth(views: CellBaseViews, targetRowWidth: Int) {
        val delta = views.contentRow.width - targetRowWidth
        layoutRowPx(views.root, views.root.width - delta)
    }

    /** [text] を [view] のフォントで 1 行描画したときの自然幅（px）。 */
    private fun naturalWidthOf(view: android.widget.TextView): Int =
        kotlin.math.ceil(
            android.text.Layout.getDesiredWidth(view.text ?: "", view.paint).toDouble(),
        ).toInt() + view.compoundPaddingLeft + view.compoundPaddingRight

    /**
     * [view] のテキストが実描画される x 座標（px）。
     * **原点は content box の左端**（= `paddingStart` の右）であり View の左端ではない。
     *
     * `isSingleLine` の TextView は `Layout` 幅が `TextView.VERY_WIDE` になるため
     * `Layout.getLineLeft` 単体では実位置にならない。`TextView.bringTextIntoView()` が
     * 設定する `scrollX` を引くと実描画位置になる。
     * 呼び出し前に対象の `viewTreeObserver.dispatchOnPreDraw()` が必要
     * （`root.draw()` だけでは `scrollX` 補正が入らない）。
     */
    private fun drawnTextLeftOf(view: android.widget.TextView): Float =
        view.layout.getLineLeft(0) - view.scrollX

    /**
     * [view] が「1 行 + 末尾省略」で切り詰められていることを検証する。
     *
     * `@GraphicsMode(NATIVE)` により `Layout.getEllipsisCount` が実際の省略文字数を返すため、
     * 構成（maxLines / ellipsize）だけでなくレンダリング結果でも確認する。
     * ただし **"…" グリフの画面上の描画位置までは検証していない**（Sample アプリに長い title /
     * 長い valueText の行が無く、実機スクリーンショットでも未確認）。
     */
    private fun assertTruncatedAtEnd(view: android.widget.TextView, label: String) {
        // `maxLines == 1` は `isSingleLine` の必要条件でしかないため、構成そのものも直接見る
        // （`isSingleLine` は `setHorizontallyScrolling(true)` も伴う）。
        assertTrue("$label は単一行構成 (isSingleLine)", view.isSingleLine)
        assertEquals("$label は 1 行で表示される", 1, view.maxLines)
        assertEquals(
            "$label は末尾省略で切り詰める",
            android.text.TextUtils.TruncateAt.END,
            view.ellipsize,
        )
        val natural = naturalWidthOf(view)
        assertTrue(
            "$label の表示幅 (${view.width}) は自然幅 ($natural) より狭く切り詰めが発生する",
            view.width < natural,
        )
        assertEquals("$label は 1 行に収まる", 1, view.layout.lineCount)
        assertTrue(
            "$label は実際に末尾省略される (ellipsisCount=${view.layout.getEllipsisCount(0)})",
            view.layout.getEllipsisCount(0) > 0,
        )
    }

    /** 主行（`contentRow`）の内容幅。 */
    private val CellBaseViews.rowWidth: Int get() = contentRow.width

    // MARK: - EntryCell（行内 trailing が残り幅全体を占める）

    /**
     * EntryCell の入力フィールドが残り幅全体を占める。
     *
     * 入力フィールドの左端が title の右端に接し、右端が主行の右端に一致することを、
     * 主行内の座標で検証する（= 「title のコンテンツ幅を除いた残り幅全体」）。
     */
    @Test
    fun `EntryCell の入力フィールドは title の右端から主行の右端までを占める`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(
                title = "名前",
                text = "ｇｋぎぎぎぎぎぎｇｋｇひごｈ ここから先は表示幅を超える長文",
            ),
            Theme(),
        )
        layoutRow(vh.views.root)

        val views = vh.views
        val edit = vh.editText

        assertTrue("主行が幅を持つこと (rowWidth=${views.rowWidth})", views.rowWidth > 0)
        assertTrue("title が表示されること", views.titleView.width > 0)
        assertEquals(
            "入力フィールドの左端は title の右端に接する",
            views.titleView.right,
            edit.left,
        )
        assertEquals(
            "入力フィールドの右端は主行の右端に一致する",
            views.rowWidth,
            edit.right,
        )
        assertEquals(
            "入力フィールド幅 = 主行幅 − title 幅",
            views.rowWidth - views.titleView.width,
            edit.width,
        )
    }

    /**
     * パスワード入力でも同じ配分になる。
     */
    @Test
    fun `EntryCell はパスワードでも同じ幅配分になる`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(
                title = "パスワード",
                text = "correct-horse-battery-staple-0123456789",
                isPassword = true,
            ),
            Theme(),
        )
        layoutRow(vh.views.root)

        val views = vh.views
        assertEquals(
            "入力フィールドの左端は title の右端に接する",
            views.titleView.right,
            vh.editText.left,
        )
        assertEquals(
            "入力フィールド幅 = 主行幅 − title 幅",
            views.rowWidth - views.titleView.width,
            vh.editText.width,
        )
    }

    /**
     * 入力フィールドの幅が固定の最低幅に依存しない。
     *
     * 入力フィールドに固定の最低幅を設けないことを、(1) `EditText.minWidth == 0`、
     * (2) title が主行を使い切るとき入力フィールド幅が 0 まで縮むこと、
     * の 2 点で検証する（原典同型の許容挙動）。
     */
    @Test
    fun `EntryCell の入力フィールド幅は固定最低幅に依存せず主行幅から title 幅を引いた値になる`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(
            EntryCell(title = "とても長いタイトルのケースで入力欄と共存する", text = "abc"),
            Theme(),
        )
        val views = vh.views
        layoutRow(views.root)

        assertEquals("EditText に最低幅ハックが設定されていない", 0, vh.editText.minWidth)

        // title の自然幅より狭い主行に絞り、「title が主行を使い切る」状況を作る。
        val titleNatural = naturalWidthOf(views.titleView)
        relayoutWithRowWidth(views, titleNatural * 3 / 4)

        val minWidthHackPx = (160 * density).toInt()
        assertTrue(
            "title は主行幅を超えない (title=${views.titleView.width} row=${views.rowWidth})",
            views.titleView.width <= views.rowWidth,
        )
        assertEquals(
            "title が主行を使い切る",
            views.rowWidth,
            views.titleView.width,
        )
        assertEquals(
            "入力フィールド幅 = 主行幅 − title 幅（下限 0）",
            (views.rowWidth - views.titleView.width).coerceAtLeast(0),
            vh.editText.width,
        )
        assertTrue(
            "固定最低幅 160dp ($minWidthHackPx px) による title の押し出しが起きない " +
                "(入力フィールド幅 ${vh.editText.width} px)",
            vh.editText.width < minWidthHackPx,
        )
        assertEquals(
            "title が固定最低幅で押し出されず主行の左端から始まる",
            0,
            views.titleView.left,
        )
    }

    /**
     * EntryCell の入力フィールドは行内に置かれる（Android）。
     * Cell 級アクセサリの領域は確保されない。
     */
    @Test
    fun `EntryCell の入力フィールドは本体行の子で accessoryHolder は空のまま`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "Name", text = "Taro"), Theme())
        layoutRow(vh.views.root)

        assertEquals(
            "EditText の親は contentRow（本体行）である",
            vh.views.contentRow,
            vh.editText.parent,
        )
        assertEquals(
            "accessoryHolder には子が追加されない",
            0,
            vh.views.accessoryHolder.childCount,
        )
        assertEquals(
            "accessoryHolder は幅を占有しない",
            0,
            vh.views.accessoryHolder.width,
        )
    }

    // MARK: - 行内 trailing なし

    /**
     * 行内 trailing がない場合は title が全幅を使う。
     *
     * title はコンテンツ幅より広い領域を得る（= `wrap_content` のままでは成立しない）ことまで
     * 見るため、主行に収まる短い title を使う。
     */
    @Test
    fun `行内 trailing がない Cell では title が主行の全幅を使う`() {
        val vh = CommandCellViewHolder.create(parent)
        vh.bind(CommandCell(title = "詳細"), Theme())
        layoutRow(vh.views.root)

        val views = vh.views
        assertEquals(android.view.View.GONE, views.valueTextView.visibility)
        assertEquals("title は主行の左端から始まる", 0, views.titleView.left)
        assertEquals("title は主行の全幅を占める", views.rowWidth, views.titleView.width)
        assertTrue(
            "title 領域 (${views.titleView.width}) はコンテンツ幅 (${naturalWidthOf(views.titleView)}) より広い",
            views.titleView.width > naturalWidthOf(views.titleView),
        )
        assertTrue(
            "主行は Cell 級アクセサリ（chevron）と重ならない " +
                "(contentRow.right=${views.contentRow.right} accessory.left=${views.accessoryHolder.left})",
            views.contentRow.right <= views.accessoryHolder.left,
        )
    }

    // MARK: - valueText 系

    /**
     * title はコンテンツ幅を確保し、収まらない valueText が末尾省略される（core/ADR-0026）。
     *
     * icon と Cell 級アクセサリを持つ行で、主行だけが縮んで両者の幅が維持されることも併せて見る。
     */
    @Test
    fun `長い valueText は残り幅で末尾省略され title は全文残る`() {
        val vh = CommandCellViewHolder.create(parent)
        vh.bind(
            CommandCell(
                title = "音量",
                valueText = "主行の幅を大きく超える長さの値テキストをここに設定して末尾省略を確認する",
                icon = KsImage.Drawable(android.graphics.drawable.ColorDrawable(0xFF00FF00.toInt())),
            ),
            Theme(cellIconSize = 32.dp),
        )
        val views = vh.views
        val value = views.valueTextView
        val title = views.titleView
        layoutRow(views.root)

        val iconWidth = views.iconView.width
        val accessoryWidth = views.accessoryHolder.width
        // title のコンテンツ幅 + valueText の自然幅の半分を主行幅にし、
        // 「title はコンテンツ幅・valueText は残り幅で収まらない」状況を作る。
        val titleNatural = naturalWidthOf(title)
        relayoutWithRowWidth(views, titleNatural + naturalWidthOf(value) / 2)

        assertEquals(android.view.View.VISIBLE, value.visibility)
        assertNotNull("title の layout が生成されていること", title.layout)
        assertEquals(
            "title はコンテンツ幅を保つ",
            titleNatural,
            title.width,
        )
        assertEquals(
            "title は末尾省略されず全文表示される",
            0,
            title.layout.getEllipsisCount(0),
        )
        assertEquals(
            "valueText は主行の残り幅を占める",
            views.rowWidth - title.width,
            value.width,
        )
        assertTrue(
            "title と valueText は重ならない (title.right=${title.right} value.left=${value.left})",
            title.right <= value.left,
        )
        assertTruncatedAtEnd(value, "残り幅に収まらない valueText")

        // 主行だけが縮み、icon 領域と Cell 級アクセサリの幅は維持される。
        assertEquals("icon 領域の幅は縮まない", iconWidth, views.iconView.width)
        assertEquals("icon 領域は正方形のまま", iconWidth, views.iconView.height)
        assertEquals("Cell 級アクセサリの幅は縮まない", accessoryWidth, views.accessoryHolder.width)
        assertTrue(
            "valueText は主行の右端からはみ出さない (value.right=${value.right} row=${views.rowWidth})",
            value.right <= views.rowWidth,
        )
        assertTrue(
            "主行は Cell 級アクセサリと重ならない",
            views.contentRow.right <= views.accessoryHolder.left,
        )
    }

    /**
     * 主行幅を超える title は上限で末尾省略され、valueText には残り幅（0 以上）が渡る。
     */
    @Test
    fun `主行幅を超える title は末尾省略され valueText は残り幅になる`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(
            LabelCell(
                title = "とても長いタイトルは主行の幅を超えるため末尾省略される",
                valueText = "Green",
            ),
            Theme(),
        )
        val views = vh.views
        val value = views.valueTextView
        val title = views.titleView
        layoutRow(views.root)

        // title の自然幅より狭い主行に絞る。
        relayoutWithRowWidth(views, naturalWidthOf(title) * 3 / 4)

        assertTrue(
            "title は主行幅を上限とする (title=${title.width} row=${views.rowWidth})",
            title.width <= views.rowWidth,
        )
        assertEquals(
            "valueText の幅は主行の残り幅（0 以上）",
            (views.rowWidth - title.width).coerceAtLeast(0),
            value.width,
        )
        assertTrue(
            "valueText は主行の右端からはみ出さない",
            value.right <= views.rowWidth,
        )
        assertTruncatedAtEnd(title, "主行幅を超える title")
    }

    /**
     * 同じ行を valueText の有無で再 bind しても配分が追随する（ViewHolder 再利用時の回帰防止）。
     */
    @Test
    fun `同じ行で valueText の有無が切り替わっても幅配分が追随する`() {
        val vh = LabelCellViewHolder.create(parent)
        val views = vh.views

        vh.bind(LabelCell(id = "c1", title = "通知", valueText = "オン"), Theme())
        layoutRow(views.root)
        assertEquals(
            "valueText があるとき title はコンテンツ幅",
            naturalWidthOf(views.titleView),
            views.titleView.width,
        )

        vh.bind(LabelCell(id = "c1", title = "通知"), Theme())
        layoutRow(views.root)
        assertEquals(android.view.View.GONE, views.valueTextView.visibility)
        assertEquals(
            "valueText が無いとき title は主行の全幅を使う",
            views.rowWidth,
            views.titleView.width,
        )

        vh.bind(LabelCell(id = "c1", title = "通知", valueText = "オン"), Theme())
        layoutRow(views.root)
        assertEquals(
            "valueText が戻ると title はコンテンツ幅に戻る",
            naturalWidthOf(views.titleView),
            views.titleView.width,
        )
        assertEquals(
            "valueText が主行の残り幅を占める",
            views.rowWidth - views.titleView.width,
            views.valueTextView.width,
        )
    }

    // MARK: - icon 領域は主行より先に譲らない

    /**
     * 行幅が足りなくても icon 枠は解決済みサイズのままで、主行の title が末尾省略される。
     */
    @Test
    fun `狭幅でも icon 枠は縮まず title が末尾省略される`() {
        val vh = CommandCellViewHolder.create(parent)
        vh.bind(
            CommandCell(
                title = "とても長いタイトルで主行の幅を使い切り末尾省略が起きるケースの検証",
                icon = KsImage.Drawable(android.graphics.drawable.ColorDrawable(0xFF00FF00.toInt())),
            ),
            Theme(cellIconSize = 44.dp),
        )
        val views = vh.views
        layoutRow(views.root)

        val expectedIconPx = (44 * density).toInt()
        val accessoryWidth = views.accessoryHolder.width
        assertEquals("解決済み icon size の枠で表示される", expectedIconPx, views.iconView.width)

        // title の自然幅の半分まで主行を絞り、行幅が自然幅の合計より狭い状況を作る。
        relayoutWithRowWidth(views, naturalWidthOf(views.titleView) / 2)

        assertEquals("icon 領域の幅は縮まない", expectedIconPx, views.iconView.width)
        assertEquals("icon 領域の高さも縮まない", expectedIconPx, views.iconView.height)
        assertEquals("Cell 級アクセサリの幅も縮まない", accessoryWidth, views.accessoryHolder.width)
        assertTruncatedAtEnd(views.titleView, "狭幅の title")
    }

    // MARK: - 本体行を入れ子にした構造での整列（android/ADR-0002）

    /**
     * 本体行を LinearLayout に入れ子化した後も、valueText と title のベースラインが揃うこと。
     *
     * 旧構造の `ConstraintSet.BASELINE` 紐付けは使えないため、`LinearLayout.isBaselineAligned`
     * （水平 LinearLayout の既定挙動）が代替手段になっている。
     */
    @Test
    fun `本体行入れ子化後も valueText は title とベースラインが揃う`() {
        val vh = LabelCellViewHolder.create(parent)
        vh.bind(LabelCell(title = "通知", valueText = "オン"), Theme())
        layoutRow(vh.views.root)

        val views = vh.views
        assertTrue("contentRow の baselineAligned が有効", views.contentRow.isBaselineAligned)

        val titleBaseline = views.titleView.top + views.titleView.baseline
        val valueBaseline = views.valueTextView.top + views.valueTextView.baseline
        assertEquals(
            "title と valueText のベースライン（contentRow 座標系）が一致する",
            titleBaseline,
            valueBaseline,
        )
    }

    /**
     * 本体行を LinearLayout に入れ子化した後も、title + description の縦チェーン
     * （`CHAIN_PACKED` + `verticalBias = 0.5f`）が成立し Cell 縦中央に packed 配置されること。
     */
    @Test
    fun `本体行入れ子化後も title と description は Cell 縦中央に packed 配置される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(title = "通知", description = "プッシュ通知を受信", isOn = true),
            Theme(),
        )
        val root = vh.views.root
        // vertical chain の効果を観測するため、行高さを内容より大きい 80dp に固定する。
        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            (320 * density).toInt(),
            View.MeasureSpec.EXACTLY,
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            (80 * density).toInt(),
            View.MeasureSpec.EXACTLY,
        )
        root.measure(widthSpec, heightSpec)
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val views = vh.views
        assertEquals(android.view.View.VISIBLE, views.descriptionView.visibility)

        // contentRow（title を含む）と descriptionView は root 直下なので root 座標系で比較できる。
        val pairCenterY = (views.contentRow.top + views.descriptionView.bottom) / 2
        val rootCenterY = root.height / 2
        val tolerance = (8 * density).toInt()
        assertTrue(
            "title+description の縦中心 ($pairCenterY) は root 縦中心 ($rootCenterY) から ±$tolerance px 以内",
            kotlin.math.abs(pairCenterY - rootCenterY) <= tolerance,
        )
        assertEquals(
            "description は本体行の直下に続く",
            views.contentRow.bottom,
            views.descriptionView.top,
        )
    }

    /**
     * EntryCell でも入力フィールドが title とベースライン揃えになること（行内 trailing の共通挙動）。
     */
    @Test
    fun `EntryCell の入力フィールドは title とベースラインが揃う`() {
        val vh = EntryCellViewHolder.create(parent)
        vh.bind(EntryCell(title = "名前", text = "Taro"), Theme())
        layoutRow(vh.views.root)

        val titleBaseline = vh.views.titleView.top + vh.views.titleView.baseline
        val editBaseline = vh.editText.top + vh.editText.baseline
        assertEquals(
            "title と入力フィールドのベースライン（contentRow 座標系）が一致する",
            titleBaseline,
            editBaseline,
        )
    }

    // MARK: - hintText の左端ガード

    /**
     * 長い hintText でも行左端の余白より内側へは入らず、末尾省略で切り詰める。
     *
     * `hintTextView` は cell 外縁基準の右上 float（右 10dp / 上 2dp）で、`START` の左端ガードが
     * 行左端の余白 16dp を守る。ガードが無いと hint はコンテンツ幅のまま左へ伸び、title に重なる。
     * 幅が足りない分はフォント縮小ではなく末尾省略で吸収する。
     */
    @Test
    fun `長い hintText は行左端の余白より内側へ入らず末尾省略される`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(
                title = "通知",
                hintText = "非常に長い注釈テキストで行の幅を超えて左へ伸びようとする".repeat(3),
                isOn = true,
            ),
            Theme(),
        )
        val views = vh.views
        layoutRow(views.root)

        val hint = views.hintTextView
        val root = views.root
        val guard = (CELL_ROW_HORIZONTAL_MARGIN_DP * density).toInt()
        val hintMarginEnd = (10 * density).toInt()

        assertTrue(
            "hintTextView.left (${hint.left}) は cell 外縁から 16dp ($guard) より内側",
            hint.left >= guard,
        )
        assertEquals(
            "右 10dp 基準は保たれる",
            hintMarginEnd,
            root.width - hint.right,
        )
        assertTruncatedAtEnd(hint, "hintText")
    }

    // MARK: - Cell 級アクセサリと description の非重なり（cell-types-basic の 2 系統配置）

    /**
     * SwitchCell の description がアクセサリの下に回り込まない。
     *
     * Switch は Cell 全体に対して垂直センター、description は Switch の leading 側で折り返す。
     * 本体行の入れ子化で `descriptionView` の `END = accessoryHolder.START` 制約が壊れていないことの
     * 回帰テスト（実機証跡だけでなく自動テストでも押さえる）。
     */
    @Test
    fun `SwitchCell の description は Cell 級アクセサリと重ならず accessory は縦中央に置かれる`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(
                title = "Notification",
                description = "This is description. you can write detail explanation of the item " +
                    "here. long text wrap automatically.",
                isOn = true,
            ),
            Theme(),
        )
        val root = vh.views.root
        layoutRow(root)

        val views = vh.views
        assertEquals(android.view.View.VISIBLE, views.descriptionView.visibility)
        assertTrue("accessoryHolder に Switch が入っている", views.accessoryHolder.childCount > 0)

        assertTrue(
            "description は Cell 級アクセサリと重ならない " +
                "(description.right=${views.descriptionView.right} accessory.left=${views.accessoryHolder.left})",
            views.descriptionView.right <= views.accessoryHolder.left,
        )
        assertTrue(
            "本体行も Cell 級アクセサリと重ならない " +
                "(contentRow.right=${views.contentRow.right} accessory.left=${views.accessoryHolder.left})",
            views.contentRow.right <= views.accessoryHolder.left,
        )

        // accessoryHolder は Cell 全体（title + description）に対して垂直センター。
        val accessoryCenterY = (views.accessoryHolder.top + views.accessoryHolder.bottom) / 2
        val rootCenterY = root.height / 2
        assertTrue(
            "accessoryHolder の縦中心 ($accessoryCenterY) は root 縦中心 ($rootCenterY) から ±1px 以内",
            kotlin.math.abs(accessoryCenterY - rootCenterY) <= 1,
        )
    }

    /**
     * Picker 系は valueText が行内・chevron が Cell 級。
     *
     * 2 系統配置（行内 trailing = `contentRow` の子 / Cell 級アクセサリ = `accessoryHolder` の子）が
     * View 階層として保たれ、description が chevron と重ならないことを検証する。
     */
    @Test
    fun `PickerCell は valueText が行内 chevron が Cell 級で description と重ならない`() {
        val vh = PickerCellViewHolder.create(parent)
        vh.bind(
            PickerCell(
                title = "Favorites",
                description = "This is description. you can write detail explanation of the item " +
                    "here. long text wrap automatically.",
                items = listOf("Green", "Red", "Blue"),
                selectedIndex = 0,
            ),
            Theme(),
        )
        val root = vh.views.root
        layoutRow(root)

        val views = vh.views

        // 行内 trailing: valueText は本体行の子。
        assertEquals(android.view.View.VISIBLE, views.valueTextView.visibility)
        assertEquals(
            "valueText は contentRow（本体行）の子である",
            views.contentRow,
            views.valueTextView.parent,
        )
        // Cell 級アクセサリ: chevron は accessoryHolder の子。
        assertTrue("accessoryHolder に chevron が入っている", views.accessoryHolder.childCount > 0)

        assertTrue(
            "valueText は chevron と重ならない " +
                "(contentRow.right=${views.contentRow.right} accessory.left=${views.accessoryHolder.left})",
            views.contentRow.right <= views.accessoryHolder.left,
        )
        assertTrue(
            "description は chevron と重ならない " +
                "(description.right=${views.descriptionView.right} accessory.left=${views.accessoryHolder.left})",
            views.descriptionView.right <= views.accessoryHolder.left,
        )
        assertTrue(
            "title と valueText は重ならない",
            views.titleView.right <= views.valueTextView.left,
        )

        val accessoryCenterY = (views.accessoryHolder.top + views.accessoryHolder.bottom) / 2
        val rootCenterY = root.height / 2
        assertTrue(
            "chevron はセル全体に対して垂直センター ($accessoryCenterY vs $rootCenterY)",
            kotlin.math.abs(accessoryCenterY - rootCenterY) <= 1,
        )
    }

    // MARK: - ButtonCell の ConstraintSet 切替

    /**
     * aux なしのボタンスタイルでは、本体行が Cell 全体に広がり title が全幅・中央揃えになる。
     *
     * ConstraintSet の対象は、title を内包する行コンテナの `contentRow` である。
     *
     * 行内 trailing が無い行では title が主行の全幅を取る（core/ADR-0026）ため、`titleAlignment`
     * が配る余白が生まれる。ここではその全幅構成のうち、本体行が Cell 全体へ広がるボタンスタイルを
     * 測る。そのため gravity のフラグ値だけでなく
     * **テキストの実描画位置**まで測る:
     *
     * 1. title 領域がテキストの自然幅より広く、gravity が配る余白がある
     * 2. gravity が `Layout` の alignment まで到達している（`ALIGN_CENTER` / `ALIGN_NORMAL`）
     * 3. テキストが title 領域（content box）の中央 / 左端に実際に描画される
     *
     * 3 の測り方: `isSingleLine = true` は `setHorizontallyScrolling(true)` を伴い `Layout` の幅が
     * `TextView.VERY_WIDE` になるため、`Layout.getLineLeft` 単体では View 座標にならない。
     * `dispatchOnPreDraw()` で `TextView.bringTextIntoView()` の `scrollX` 補正を発火させると
     * Robolectric でも実描画位置を px 単位で測れる（[drawnTextLeftOf]）。
     *
     * **1 と 3 は役割が違うので両方残すこと**: 3 は期待値を View の実幅から導くため
     * 幅そのものの退行（title が `wrap_content` に戻る等）は検出できず、そこは 1 が担保する。
     * 逆に 1・2 を通過してしまう差分（title に compound drawable が付く等）は 3 だけが捕捉する。
     * また 3 は `paddingEnd`（title の 6dp クリアランス）にも依存するため、
     * クリアランス値を変えるとこのアサーションも更新が要る。
     */
    @Test
    fun `ButtonCell の aux なしボタンスタイルは title が Cell 全幅で中央揃えになる`() {
        fun bindAndLayout(alignment: CellTitleAlignment): ButtonCellViewHolder {
            val vh = ButtonCellViewHolder.create(parent)
            vh.bind(ButtonCell(title = "ログアウト", titleAlignment = alignment), Theme())
            layoutRow(vh.views.root)
            return vh
        }

        val centered = bindAndLayout(CellTitleAlignment.CENTER)
        val start = bindAndLayout(CellTitleAlignment.START)
        val views = centered.views
        val root = views.root
        val centerTitle = views.titleView
        val startTitle = start.views.titleView

        val lp = views.contentRow.layoutParams as ConstraintLayout.LayoutParams
        assertEquals(
            "ボタンスタイルでは contentRow の END が parent に紐づく",
            ConstraintLayout.LayoutParams.PARENT_ID,
            lp.endToEnd,
        )
        val rowMarginH =
            (CELL_ROW_HORIZONTAL_MARGIN_DP * ctx.resources.displayMetrics.density).toInt()
        assertEquals(
            "contentRow は行左端の余白の内側から始まる",
            rowMarginH,
            views.contentRow.left,
        )
        assertEquals(
            "contentRow は行右端の余白の内側まで広がる",
            root.width - rowMarginH,
            views.contentRow.right,
        )
        assertEquals(
            "title は本体行の全幅を占める",
            views.contentRow.width,
            centerTitle.width,
        )
        assertEquals(
            "titleAlignment CENTER が gravity に反映される",
            android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL,
            centerTitle.gravity,
        )

        // 前提 1: title 領域がテキストの自然幅より広い = gravity が配る余白がある。
        val naturalTextWidth = naturalWidthOf(centerTitle)
        assertTrue(
            "title 領域 (${centerTitle.width}) はテキストの自然幅 ($naturalTextWidth) より広く " +
                "gravity が配る余白がある",
            centerTitle.width > naturalTextWidth,
        )
        assertEquals(
            "titleAlignment は幅配分に影響しない",
            centerTitle.width,
            startTitle.width,
        )

        // 前提 2: gravity が Layout の alignment まで到達している。
        assertEquals(
            "CENTER は Layout の ALIGN_CENTER になる",
            android.text.Layout.Alignment.ALIGN_CENTER,
            centerTitle.layout.getParagraphAlignment(0),
        )
        assertEquals(
            "START は Layout の ALIGN_NORMAL になる",
            android.text.Layout.Alignment.ALIGN_NORMAL,
            startTitle.layout.getParagraphAlignment(0),
        )

        // 前提 3: テキストが title 領域のどこに実描画されるか。
        // scrollX 補正は preDraw で入るため、明示的に発火させてから測る。
        centerTitle.viewTreeObserver.dispatchOnPreDraw()
        startTitle.viewTreeObserver.dispatchOnPreDraw()

        // gravity は content box（padding を除いた領域）の中で働く。
        val contentWidth = centerTitle.width - centerTitle.paddingStart - centerTitle.paddingEnd
        assertEquals(
            "CENTER ではテキストが title 領域 (content box) の中央に描画される",
            (contentWidth - centerTitle.layout.getLineMax(0)) / 2f,
            drawnTextLeftOf(centerTitle),
            1.0f,
        )
        assertEquals(
            "START ではテキストが title 領域の左端に描画される",
            0.0f,
            drawnTextLeftOf(startTitle),
            1.0f,
        )
    }

    /**
     * aux ありの通常レイアウトでは、title がコンテンツ幅・valueText が残り幅になる。
     *
     * 行内 trailing があるとき title は自分のコンテンツ幅までしか取らないため
     * （core/ADR-0026）、`titleAlignment` が配る余白は生まれない。gravity は
     * `Layout` の alignment までは到達しており（`UnifyCellCommonFieldsTest` が見る
     * フラグ値と対応する）、余白が無いことで見た目が変わらないだけである。
     */
    @Test
    fun `ButtonCell 通常レイアウトでは title がコンテンツ幅で valueText が残り幅を占める`() {
        fun bindAndLayout(alignment: CellTitleAlignment): ButtonCellViewHolder {
            val vh = ButtonCellViewHolder.create(parent)
            vh.bind(
                ButtonCell(title = "登録", valueText = "送信", titleAlignment = alignment),
                Theme(),
            )
            layoutRow(vh.views.root)
            return vh
        }

        val centered = bindAndLayout(CellTitleAlignment.CENTER)
        val start = bindAndLayout(CellTitleAlignment.START)
        val centerTitle = centered.views.titleView
        val startTitle = start.views.titleView

        assertEquals(
            "titleAlignment は幅配分に影響しない",
            startTitle.width,
            centerTitle.width,
        )
        assertEquals(
            "title はコンテンツ幅を占める",
            naturalWidthOf(centerTitle),
            centerTitle.width,
        )
        assertEquals(
            "valueText は主行の残り幅を占める",
            centered.views.rowWidth - centerTitle.width,
            centered.views.valueTextView.width,
        )

        // gravity は Layout の alignment まで到達している（配る余白が無いだけ）。
        assertEquals(
            "CENTER は Layout の ALIGN_CENTER になる",
            android.text.Layout.Alignment.ALIGN_CENTER,
            centerTitle.layout.getParagraphAlignment(0),
        )
        assertEquals(
            "START は Layout の ALIGN_NORMAL になる",
            android.text.Layout.Alignment.ALIGN_NORMAL,
            startTitle.layout.getParagraphAlignment(0),
        )
    }

    // MARK: - hintText の下端ガード

    /**
     * hint が行高に対して大きいとき、下端ガードが実際に hint を縮めて cell 下端から 12dp を残す。
     *
     * 上端 2dp 基準（`verticalBias = 0`）は保ったまま、`constrainedHeight` により
     * wrap_content の高さが「行高 − 上 2dp − 下 12dp」まで縛られる。
     * 高さがフォントサイズに追随するのは NATIVE graphics だけなので、この検証は本クラスに置く
     * （legacy graphics では測定高さが textSize に追随せず、ガードに触れる状況を作れない）。
     */
    @Test
    fun `行高に対して hint が大きいとき下端ガードが hint を縮める`() {
        val vh = SwitchCellViewHolder.create(parent)
        vh.bind(
            SwitchCell(title = "通知", hintText = "推奨", isOn = true),
            Theme(cellHintFont = TextStyle(fontSize = 60.sp)),
        )
        val rowHeight = (60 * density).toInt()
        val root = vh.views.root
        root.measure(
            View.MeasureSpec.makeMeasureSpec((320 * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(rowHeight, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val hint = vh.views.hintTextView
        val hintMarginTop = (2 * density).toInt()
        val hintMarginBottom = (12 * density).toInt()

        // ガードが実際に縛っていること: 1 行ぶんの自然高がガード内の高さを超えている。
        val fm = hint.paint.fontMetricsInt
        val naturalLineHeight = fm.bottom - fm.top
        assertTrue(
            "1 行の自然高 ($naturalLineHeight) がガード内の高さ (${hint.height}) を超えており、" +
                "ガードが高さを縛っている",
            naturalLineHeight > hint.height,
        )

        assertEquals("上端 2dp 基準は保たれる", hintMarginTop, hint.top)
        assertEquals(
            "hint の下端は cell 下端から 12dp 内側",
            hintMarginBottom,
            rowHeight - hint.bottom,
        )
    }

    /**
     * aux ありの `ButtonCell`（`accessoryHolder` が GONE）でも行右端の余白 16dp が残る。
     *
     * 行右端の余白は `accessoryHolder` の END マージンが持つが、`accessoryHolder` が GONE の行では
     * そのマージンごと消える。`contentRow` / `descriptionView` の END 側 goneMargin がこれを補う。
     * 接続先（`endToStart`）だけでは検出できないため、実位置を測る。
     */
    @Test
    fun `accessoryHolder が GONE の行でも行右端の余白が残る`() {
        val vh = ButtonCellViewHolder.create(parent)
        vh.bind(ButtonCell(title = "登録", valueText = "送信"), Theme())
        val views = vh.views
        layoutRow(views.root)

        val rowMarginH = (CELL_ROW_HORIZONTAL_MARGIN_DP * density).toInt()
        assertEquals(
            "この行では accessoryHolder が GONE",
            View.GONE,
            views.accessoryHolder.visibility,
        )
        assertEquals(
            "contentRow の右端は行右端から 16dp 内側",
            views.root.width - rowMarginH,
            views.contentRow.right,
        )
        assertTrue(
            "valueText が行の右端に密着しない " +
                "(value.right=${views.valueTextView.right} root.width=${views.root.width})",
            views.root.width - views.valueTextView.right >= rowMarginH,
        )
    }

    /**
     * `accessoryHolder` が GONE の行では description も行右端の余白の内側で折り返す。
     */
    @Test
    fun `accessoryHolder が GONE の行では description も行右端の余白の内側に収まる`() {
        val vh = ButtonCellViewHolder.create(parent)
        // ButtonCell は description を持たないため、共通行へ直接 description を流して確認する。
        val views = vh.views
        applyCellBaseLayout(
            views = views,
            title = "登録",
            description = "説明文",
            valueText = "送信",
            icon = null,
            hintText = null,
            effective = EffectiveStyle.from(views.root.context, Theme(), CellStyle()),
        )
        views.accessoryHolder.visibility = View.GONE
        layoutRow(views.root)

        val rowMarginH = (CELL_ROW_HORIZONTAL_MARGIN_DP * density).toInt()
        assertEquals(
            "descriptionView の右端は行右端から 16dp 内側",
            views.root.width - rowMarginH,
            views.descriptionView.right,
        )
    }

    /**
     * ボタンスタイルから aux ありの通常レイアウトへ復帰できる（ViewHolder 再利用時の回帰防止）。
     */
    @Test
    fun `ButtonCell はボタンスタイルから通常レイアウトへ復帰する`() {
        val vh = ButtonCellViewHolder.create(parent)

        // 1) 先にボタンスタイルで bind する
        vh.bind(ButtonCell(title = "ログアウト"), Theme())
        layoutRow(vh.views.root)

        // 2) aux（valueText）ありで再 bind → 通常レイアウトへ復帰
        vh.bind(ButtonCell(title = "登録", valueText = "送信"), Theme())
        layoutRow(vh.views.root)

        val views = vh.views
        val lp = views.contentRow.layoutParams as ConstraintLayout.LayoutParams
        assertEquals(
            "通常レイアウトでは contentRow の END が accessoryHolder に紐づく",
            views.accessoryHolder.id,
            lp.endToStart,
        )
        assertEquals(android.view.View.VISIBLE, views.valueTextView.visibility)
        assertTrue(
            "title と valueText は重ならない " +
                "(title.right=${views.titleView.right} value.left=${views.valueTextView.left})",
            views.titleView.right <= views.valueTextView.left,
        )
        assertEquals(
            "主行は title と valueText で分け合う",
            views.rowWidth,
            views.titleView.width + views.valueTextView.width,
        )
    }
}
