package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat

/**
 * 全 Cell ViewHolder が共通で保持する View 参照群。
 *
 * View 構造（ConstraintLayout root + 本体行 LinearLayout）:
 *
 * ```
 *  root (ConstraintLayout, padding なし)
 *  ├─ iconView           (Start=parent+16dp, Top=parent+4dp, Bottom=parent+4dp) ← 左端中央
 *  ├─ contentRow         (Start=iconView.End, Top=parent+4dp, End=accessoryHolder.Start, Bottom=descriptionView.Top, width=0dp)
 *  │                     ← 本体行（水平 LinearLayout）。vertical chain head
 *  │   ├─ titleView      (wrap_content, weight=0) ← コンテンツ幅（主行幅が上限）
 *  │   └─ valueTextView  (0dp + weight=1) ← 主行の残り幅を占める行内 trailing
 *  ├─ descriptionView    (Start=iconView.End, Top=contentRow.Bottom, End=accessoryHolder.Start, Bottom=parent+4dp) ← chain tail
 *  ├─ accessoryHolder    (End=parent+16dp, Top=parent+4dp, Bottom=parent+4dp) ← 右端中央（Cell 級アクセサリ）
 *  └─ hintTextView       (End=parent+10dp, Top=parent+2dp, Bottom≦parent−12dp, Start≧parent+16dp)
 *                        ← 右上 float（Z 順は accessoryHolder より前面）
 * ```
 *
 * **行の余白の持ち主**: 行内容と行端の距離（横 [CELL_ROW_HORIZONTAL_MARGIN_DP] /
 * 縦 [CELL_ROW_VERTICAL_MARGIN_DP]）は root の padding ではなく、内容側の View のマージンが持つ。
 * root を無余白にすることで、`hintTextView` に与えた TOP 2dp / END 10dp が cell 外縁からの
 * 実距離になり、iOS の hintLabel と同じ位置に float する。
 *
 * **GONE な View を経由する余白**: ConstraintLayout は `GONE` の View を「サイズ 0 の点、かつ
 * 自身のマージンは 0」として扱うため、GONE になり得る View にマージンを預けた経路は
 * 参照側の `goneMargin` で補う必要がある。この行構造では次の 3 経路が該当する。
 * - `iconView` が `GONE`（icon 無しの行）→ `contentRow` / `descriptionView` の START 側 goneMargin
 * - `accessoryHolder` が `GONE`（aux ありの `ButtonCell` 等）→ 同 2 つの END 側 goneMargin
 * - `descriptionView` が `GONE`（副題無しの行）→ `contentRow` の BOTTOM 側 goneMargin
 *
 * **幅配分**: 主行（title + 行内 trailing）の残り幅配分は `contentRow` の LinearLayout weight が
 * 担う（構造の設計判断: android/ADR-0002）。幅が足りないときは title を守り行内 trailing を省略する
 * ため、title が `wrap_content`（コンテンツ幅・主行幅が上限）、行内 trailing が `0dp + weight=1`
 * （残り幅）を取る（core/ADR-0026）。行内 trailing を持たない行では [applyCellBaseLayout] が
 * `titleView` を `0dp + weight=1` へ切り替え、title が主行の全幅を使えるようにする。
 * `titleView` は原典 `CellTitle` の `paddingRight="6dp"` 踏襲で `paddingEnd` を持ち、
 * title と行内 trailing が文字同士で接するのを防ぐ。
 *
 * **ベースライン揃え**: `titleView` と行内 trailing のベースライン揃えは
 * `LinearLayout.isBaselineAligned`（水平 LinearLayout の既定挙動）が担う。本体行を
 * `contentRow` に入れ子化しているため、root の ConstraintLayout からは baseline 制約を張れない。
 *
 * **縦中央**: `contentRow` と `descriptionView` が vertical chain（`CHAIN_PACKED` +
 * `verticalBias = 0.5f`）で結ばれ、cell 縦中央付近に packed 配置される。`descriptionView` が
 * `GONE` のときも ConstraintLayout は GONE chain member をスペース 0 として扱うため、
 * `contentRow` 単独でも縦中央寄せ配置が維持される。ただし chain の下端の余白は GONE の
 * `descriptionView` が預かっているため、`contentRow` の BOTTOM 側 goneMargin で補って
 * 上下対称の領域を保つ（非対称なままだと packed 配置が下へずれ、縦中央の `accessoryHolder` と
 * 食い違う）。これにより `accessoryHolder`（縦中央）と整合する。
 * さらに `contentRow` には -1dp の `translationY`（光学中心補正、android/ADR-0004）を掛け、
 * フォントメトリクス由来のテキストの見た目の沈みを打ち消してアクセサリと光学的に揃える
 * （`buildCellBaseViews` 内 `opticalCenterOffsetY` のコメント参照。descriptionView は
 * アクセサリと対にならないため補正対象外）。
 *
 * `hintTextView` を `accessoryHolder` より **後に `addView`** することで Z 順を前面に保つ。
 */
internal class CellBaseViews(
    val root: ConstraintLayout,
    val iconView: AppCompatImageView,
    val contentRow: LinearLayout,
    val titleView: TextView,
    val descriptionView: TextView,
    val valueTextView: TextView,
    val accessoryHolder: FrameLayout,
    val hintTextView: TextView,
) {
    /**
     * 主行の残り幅全体を占める行内 trailing（EntryCell の入力フィールド）が
     * [contentRow] に追加済みかどうか。
     *
     * この行では `valueTextView` が非表示でも残り幅の受け手が別に居るため、
     * [applyCellBaseLayout] は `titleView` をコンテンツ幅のままにする。
     */
    var hasFillingInlineTrailing: Boolean = false
        private set

    /** [addFillingInlineTrailing] から呼ぶ、残り幅を占める行内 trailing の登録。 */
    internal fun markFillingInlineTrailing() {
        hasFillingInlineTrailing = true
    }
}

/**
 * [CellBaseViews] を programmatic に構築する。
 *
 * `ConstraintLayout` をルートとし、内部 View を [ConstraintSet] で配置する。XML レイアウトは使わない
 * （依存ライブラリ化の容易さのため）。`hintTextView` の Z 順を前面に保つため、`addView` の順序を
 * `iconView` → `contentRow` → `descriptionView` → `accessoryHolder` → `hintTextView`
 * と最後に `hintTextView` を追加する（`titleView` / `valueTextView` は `contentRow` の子）。
 *
 * 共通行はライブラリ所有の UI であるため、親の Context をそのまま使わず同梱テーマをかぶせた Context
 * から生成する（android/ADR-0020）。行に載せるアクセサリも `views.root.context` を使えば同じ Context
 * を共有できる。
 *
 * @param parent 親 ViewGroup（[android.view.LayoutInflater] 経由ではなくテスト時の `parent.context` を取るためのもの）
 */
internal fun buildCellBaseViews(parent: ViewGroup): CellBaseViews =
    buildCellBaseViews(parent.ksThemedContext())

/**
 * 行の左右余白（dp）。共通行レイアウトでは `iconView` の START マージンと `accessoryHolder` の
 * END マージンに与えられ、行の内容と行端の距離を決める。
 *
 * `accessoryHolder` は root の END にこの値の margin で接続されるため、そのまま
 * 「アクセサリ右端から行右端までの距離」になる。共通行レイアウトを使わない full-bleed 系 Cell
 * （[CustomCell]）が同じ位置にアクセサリを描くために共有する。
 */
internal const val CELL_ROW_HORIZONTAL_MARGIN_DP: Int = 16

/**
 * 行の上下余白（dp）。共通行レイアウトでは `iconView` / `accessoryHolder` の TOP・BOTTOM
 * マージンと、vertical chain の両端（`contentRow` の TOP / `descriptionView` の BOTTOM）に
 * 与えられ、行の内容と行の上下端の距離を決める。
 *
 * 値は移植元 AiForms の `cellbaseview.axml` の `paddingTop="4dp"` / `paddingBottom="4dp"` に揃える。
 */
internal const val CELL_ROW_VERTICAL_MARGIN_DP: Int = 4

/**
 * 本体行の光学中心補正量（dp、`translationY` に与える負値）。
 *
 * 本体行を幾何中心よりわずかに持ち上げ、フォントメトリクス由来のテキストの沈みを打ち消して
 * 縦中央配置のアクセサリと光学的に揃える（android/ADR-0004。詳細は [buildCellBaseViews] 内の
 * `opticalCenterOffsetY` のコメント）。本体行の制約を差し替える経路も、この値を維持する。
 */
internal const val CELL_ROW_OPTICAL_CENTER_OFFSET_DP: Float = -1f

/** Disclosure Indicator（[R.drawable.ic_navigate_next]）の描画幅（dp）。 */
internal const val CELL_DISCLOSURE_WIDTH_DP: Int = 18

/** Disclosure Indicator（[R.drawable.ic_navigate_next]）の描画高さ（dp）。 */
internal const val CELL_DISCLOSURE_HEIGHT_DP: Int = 26

/**
 * [CellBaseViews] を programmatic に構築する（Context 直接受け取り版）。
 */
internal fun buildCellBaseViews(ctx: Context): CellBaseViews {
    val density = ctx.resources.displayMetrics.density
    // 行の余白は root の padding ではなく内容側 View のマージンが持つ（クラスの構造説明を参照）。
    val rowMarginH = (CELL_ROW_HORIZONTAL_MARGIN_DP * density).toInt()
    val rowMarginV = (CELL_ROW_VERTICAL_MARGIN_DP * density).toInt()
    // icon 枠の初期値。実際の一辺は bind のたびに実効 style から再評価するため
    // （[applyCellBaseLayout]）、ここでは既定サイズを置くだけにとどめる。
    val iconSize = (Theme.DEFAULT_CELL_ICON_SIZE_DP_VALUE * density).toInt()
    // iconMarginEnd: iOS の `UIListContentConfiguration` 標準余白（11〜16pt）相当に揃え、
    // また Material Design の `?attr/listPreferredItemPaddingStart` (16dp) とも整合させる。
    // 8dp では iOS と比較して詰まって見えるため 16dp を採る。
    val iconMarginEnd = (16 * density).toInt()
    val hintMarginTop = (2 * density).toInt()
    val hintMarginEnd = (10 * density).toInt()
    // hint の下端ガード。cell 下端との間にこの距離を残し、内容が伸びても行の下側へ食い込ませない。
    val hintMarginBottom = (12 * density).toInt()
    // title と行内 trailing のクリアランス。オリジナル `cellbaseview.axml` の
    // `CellTitle` の `android:paddingRight="6dp"` 踏襲（padding は View 幅に含まれるため
    // 「title 幅 + 行内 trailing 幅 = 主行幅」の等式は保たれる）。
    val titlePaddingEnd = (6 * density).toInt()
    // 本体行の光学中心補正（android/ADR-0004）。Android の TextView は Roboto の
    // ascent/descent 非対称により CJK グリフの ink 中心が View 幾何中心より
    // 約 0.04em（16〜20sp で約 1dp）下に沈み、縦中央配置のアクセサリ（chevron 等）に
    // 対してテキストだけが下がって見える。translationY は描画時オフセットのため
    // レイアウト計算（chain・最低高さ保証）へ影響せず、contentRow ごと動かすことで
    // title / 行内 trailing のベースライン関係も崩れない。
    val opticalCenterOffsetY = CELL_ROW_OPTICAL_CENTER_OFFSET_DP * density

    // root は MinHeightConstraintLayout を使う。標準 ConstraintLayout は実機の
    // `RecyclerView` + `LinearLayoutManager` 配下で `heightSpec = UNSPECIFIED` のとき
    // `setMinimumHeight()` を尊重しないケースがあるため、`onMeasure` 後の下限ガードで
    // 60dp 最低保証を確実にする。
    val root = MinHeightConstraintLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        id = View.generateViewId()
    }

    val iconView = AppCompatImageView(ctx).apply {
        id = View.generateViewId()
        // marginEnd は ConstraintLayout の対応 anchor が無いと無視されるため、ここでは設定しない。
        // 右側余白は titleView / descriptionView の START=iconView.END 接続に margin パラメータを
        // 渡して与える（下記 ConstraintSet 構成参照）。
        layoutParams = ConstraintLayout.LayoutParams(iconSize, iconSize)
        visibility = View.GONE
    }

    // 本体行（主行）: title と行内 trailing が水平に並び、weight で残り幅を配分する。
    // 移植元 AiForms.SettingsView の `CellContentStack` と同型（android/ADR-0002）。
    val contentRow = LinearLayout(ctx).apply {
        id = View.generateViewId()
        orientation = LinearLayout.HORIZONTAL
        // 既定値だが、title と行内 trailing のベースライン揃えの根拠として明示する
        // （旧構造の ConstraintSet.BASELINE 紐付けの代替）。
        isBaselineAligned = true
        // 光学中心補正（上記 opticalCenterOffsetY のコメント参照）
        translationY = opticalCenterOffsetY
        layoutParams = ConstraintLayout.LayoutParams(
            // 0dp = MATCH_CONSTRAINT。icon と accessory の間の全幅を占有する。
            0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    val titleView = TextView(ctx).apply {
        id = View.generateViewId()
        // 既定配分: title はコンテンツ幅を確保する（core/ADR-0026）。
        // 行内 trailing を持たない行では applyCellBaseLayout が `0dp + weight=1` へ切り替える。
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0f,
        )
        // 主行幅に収まらない title は末尾省略で切り詰める（行内 trailing と重ならないため）。
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        // title と行内 trailing が文字同士で接しないようクリアランスを設ける（原典同型）。
        setPaddingRelative(0, 0, titlePaddingEnd, 0)
    }

    val descriptionView = TextView(ctx).apply {
        id = View.generateViewId()
        layoutParams = ConstraintLayout.LayoutParams(
            0,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
        visibility = View.GONE
    }

    val valueTextView = TextView(ctx).apply {
        id = View.generateViewId()
        // 既定配分: valueText は主行の残り幅を占め、収まらない分を末尾省略する（core/ADR-0026）。
        // 残り幅が 0 なら表示幅も 0 になる。
        layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        )
        gravity = Gravity.END
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        visibility = View.GONE
    }

    val accessoryHolder = FrameLayout(ctx).apply {
        id = View.generateViewId()
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
    }

    val hintTextView = TextView(ctx).apply {
        id = View.generateViewId()
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
        )
        // オリジナル cellbaseview.axml の CellHintText 踏襲: 小さい・右寄せ・1 行・末尾省略
        gravity = Gravity.END
        isSingleLine = true
        ellipsize = TextUtils.TruncateAt.END
        visibility = View.GONE
    }

    // 本体行の子: titleView → valueTextView（左から title、右に行内 trailing）。
    contentRow.addView(titleView)
    contentRow.addView(valueTextView)

    // root の addView 順序: iconView → contentRow → descriptionView → accessoryHolder → hintTextView
    // 最後の hintTextView を addView することで accessoryHolder より Z 順前面に置く。
    root.addView(iconView)
    root.addView(contentRow)
    root.addView(descriptionView)
    root.addView(accessoryHolder)
    root.addView(hintTextView)

    // ConstraintSet で配置を確定する
    val set = ConstraintSet()
    set.clone(root)

    // iconView: 左端中央。START の margin が行左端の余白を、TOP / BOTTOM の margin が
    // 行上下端の余白を持つ（両側同値なので縦中央配置は保たれる）。行高より大きい icon を
    // 指定したときに icon が行の上下端へ密着しないのはこの margin による。
    set.connect(
        iconView.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
        rowMarginH,
    )
    set.connect(
        iconView.id,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        rowMarginV,
    )
    set.connect(
        iconView.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
        rowMarginV,
    )

    // contentRow（本体行）: icon の右、accessoryHolder の左、vertical chain の head
    //   TOP = parent.TOP / BOTTOM = descriptionView.TOP（chain head）
    // 幅は MATCH_CONSTRAINT（0dp）で icon と accessory の間の全幅を占有し、内部の weight で
    // title / 行内 trailing に残り幅を配分する（android/ADR-0002）。
    // START=iconView.END に margin = iconMarginEnd を渡して icon との右余白を確保する。
    // iconView が GONE のときは自身のマージンが 0 に潰れて parent の START に重なるため、
    // goneMargin に行左端の余白を持たせて左端から 16dp の開始位置を保つ。
    // TOP の margin が行上端の余白を持つ。
    set.connect(contentRow.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)
    set.setGoneMargin(contentRow.id, ConstraintSet.START, rowMarginH)
    set.connect(
        contentRow.id,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        rowMarginV,
    )
    set.connect(contentRow.id, ConstraintSet.BOTTOM, descriptionView.id, ConstraintSet.TOP)
    // descriptionView が GONE のとき、chain 下端の余白は GONE 側のマージンごと消えるため
    // ここで補う（上下非対称のままだと packed 配置が下へずれる）。
    set.setGoneMargin(contentRow.id, ConstraintSet.BOTTOM, rowMarginV)
    set.connect(contentRow.id, ConstraintSet.END, accessoryHolder.id, ConstraintSet.START)
    // accessoryHolder が GONE のとき、行右端の余白は GONE 側のマージンごと消えるためここで補う。
    set.setGoneMargin(contentRow.id, ConstraintSet.END, rowMarginH)
    set.constrainWidth(contentRow.id, ConstraintSet.MATCH_CONSTRAINT)

    // descriptionView: icon の右、accessoryHolder の左、本体行 vertical chain の tail
    //   TOP = contentRow.BOTTOM / BOTTOM = parent.BOTTOM（chain tail）
    // START=iconView.END に margin = iconMarginEnd を渡して icon との右余白を確保する。
    // iconView が GONE のときは contentRow と同じく goneMargin で行左端の余白を保つ。
    // BOTTOM の margin が行下端の余白を持つ。
    set.connect(descriptionView.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)
    set.setGoneMargin(descriptionView.id, ConstraintSet.START, rowMarginH)
    set.connect(descriptionView.id, ConstraintSet.TOP, contentRow.id, ConstraintSet.BOTTOM)
    set.connect(descriptionView.id, ConstraintSet.END, accessoryHolder.id, ConstraintSet.START)
    // accessoryHolder が GONE のとき、行右端の余白は GONE 側のマージンごと消えるためここで補う。
    set.setGoneMargin(descriptionView.id, ConstraintSet.END, rowMarginH)
    set.connect(
        descriptionView.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
        rowMarginV,
    )
    set.setHorizontalBias(descriptionView.id, 0.0f)

    // 本体行（contentRow + description）の vertical chain を packed + bias 0.5 に設定。
    // → cell 縦中央付近に packed 配置され、accessoryHolder（CenterVertical）と整合する。
    // descriptionView が GONE のときも ConstraintLayout は GONE chain member をスペース 0 として
    // 扱うため、contentRow 単独で縦中央寄せが維持される。
    set.setVerticalChainStyle(contentRow.id, ConstraintSet.CHAIN_PACKED)
    set.setVerticalBias(contentRow.id, 0.5f)

    // valueTextView は contentRow の子（LinearLayout weight 配分 + baselineAligned）のため、
    // ConstraintSet では扱わない。

    // accessoryHolder: 右端中央。END の margin が行右端の余白を、TOP / BOTTOM の margin が
    // 行上下端の余白を持つ（両側同値なので縦中央配置は保たれる）。
    set.connect(
        accessoryHolder.id,
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END,
        rowMarginH,
    )
    set.connect(
        accessoryHolder.id,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        rowMarginV,
    )
    set.connect(
        accessoryHolder.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
        rowMarginV,
    )

    // hintTextView: cell 外縁を基準にした右上 float（上 2dp / 右 10dp）。root が無余白のため
    // ここで与えるマージンがそのまま cell 外縁からの実距離になる。
    // BOTTOM は下端ガードで、verticalBias 0 により通常は上端 2dp に置かれ、
    // 高さが足りないときだけ下端 12dp を守って縮む（constrainedHeight）。
    // START は左端ガードで、horizontalBias 1 により通常は右 10dp 基準のコンテンツ幅を取り、
    // 長い hint でも行左端の余白より内側へは入らず末尾省略で切り詰める（constrainedWidth）。
    set.connect(
        hintTextView.id,
        ConstraintSet.TOP,
        ConstraintSet.PARENT_ID,
        ConstraintSet.TOP,
        hintMarginTop,
    )
    set.connect(
        hintTextView.id,
        ConstraintSet.END,
        ConstraintSet.PARENT_ID,
        ConstraintSet.END,
        hintMarginEnd,
    )
    set.connect(
        hintTextView.id,
        ConstraintSet.BOTTOM,
        ConstraintSet.PARENT_ID,
        ConstraintSet.BOTTOM,
        hintMarginBottom,
    )
    set.connect(
        hintTextView.id,
        ConstraintSet.START,
        ConstraintSet.PARENT_ID,
        ConstraintSet.START,
        rowMarginH,
    )
    set.setVerticalBias(hintTextView.id, 0.0f)
    set.constrainedHeight(hintTextView.id, true)
    set.setHorizontalBias(hintTextView.id, 1.0f)
    set.constrainedWidth(hintTextView.id, true)

    set.applyTo(root)

    return CellBaseViews(
        root = root,
        iconView = iconView,
        contentRow = contentRow,
        titleView = titleView,
        descriptionView = descriptionView,
        valueTextView = valueTextView,
        accessoryHolder = accessoryHolder,
        hintTextView = hintTextView,
    )
}

/**
 * 本体行に「主行の残り幅全体を占める行内 trailing」を追加する（EntryCell の入力フィールド用）。
 *
 * [trailing] を `0dp` + `weight = 1`（残り幅全体）で `contentRow` の末尾に追加し、
 * この行を「残り幅の受け手が居る行」として記録する。title は既定配分どおりコンテンツ幅のまま
 * （`valueTextView` を使う行と同型）で、[applyCellBaseLayout] も記録に従って
 * title を全幅へ切り替えない。
 *
 * 移植元 AiForms.SettingsView の `EntryCellRenderer`
 * （`//remove weight and change width due to fill _EditText.`）と同型（android/ADR-0002）。
 * title が主行を使い切る場合に trailing の幅が 0 になるのは原典同型の許容挙動。
 *
 * @param views 対象の [CellBaseViews]
 * @param trailing 本体行へ追加する行内 trailing View（EntryCell の [android.widget.EditText] 等）
 */
internal fun addFillingInlineTrailing(views: CellBaseViews, trailing: View) {
    views.markFillingInlineTrailing()
    applyTitleWidthMode(views, fillsRow = false)
    views.contentRow.addView(
        trailing,
        LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f,
        ),
    )
}

/**
 * `titleView` の主行内での幅の取り方を切り替える。
 *
 * - [fillsRow] = true: `0dp + weight = 1`。行内 trailing が無い行で title が主行の全幅を使う
 *   （`ButtonCell` の中央揃えはこの状態に依存する）。
 * - [fillsRow] = false: `wrap_content + weight = 0`。行内 trailing がある行で title は
 *   コンテンツ幅（主行幅が上限）を確保し、残り幅を行内 trailing へ渡す（core/ADR-0026）。
 *
 * 共通行の描画を [applyCellBaseLayout] に通さない ViewHolder（`ButtonCell` のボタンスタイル）は、
 * 自分で全幅側を選ぶ。値が変わるときだけ `layoutParams` を差し戻し、不要な `requestLayout()` を避ける。
 */
internal fun applyTitleWidthMode(views: CellBaseViews, fillsRow: Boolean) {
    val titleView = views.titleView
    val lp = titleView.layoutParams as? LinearLayout.LayoutParams ?: return
    val targetWidth = if (fillsRow) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
    val targetWeight = if (fillsRow) 1f else 0f
    if (lp.width != targetWidth || lp.weight != targetWeight) {
        lp.width = targetWidth
        lp.weight = targetWeight
        titleView.layoutParams = lp
    }
}

/**
 * 全 Cell 共通の行レイアウト関数。
 *
 * `title` / `description` / `valueText` / `icon` / `hintText` を [views] 内の対応する View に
 * 反映し、visibility / 色 / フォント / サイズを [effective] から解決する。
 * `isEnabled == false` のときは各テキスト色を [EffectiveStyle.disabledTextColor] で上書きする。
 *
 * あわせて、行内 trailing の有無に応じた主行の幅配分（[applyTitleWidthMode]）と、
 * icon 領域の正方形枠・角丸（[applyIconFrame]）も bind のたびに再評価する。
 * 再利用された行に前回の bind の配分や clip が残らないのはこのためである。
 *
 * `accessoryHolder` への trailing コントロールの addView は本関数の責務外
 * （ViewHolder 側で `views.accessoryHolder.addView(...)` を呼ぶ）。
 *
 * 各 Cell が個別に共通フィールドを描画するのではなく本関数へ集約することで、
 * Cell 間で行構造を揃える（core/ADR-0011）。
 *
 * @param views CellBaseViews インスタンス（ViewHolder が保持するもの）
 * @param title タイトル（必須）
 * @param description 副題（任意、null で descriptionView を GONE）
 * @param valueText 値テキスト（任意、null で valueTextView を GONE）
 * @param icon アイコン（任意、null / SystemName で iconView を GONE）
 * @param hintText ヒントテキスト（任意、null で hintTextView を GONE。右上 float 配置）
 * @param effective 実効スタイル
 * @param isEnabled 有効／無効（false のときテキスト色を `effective.disabledTextColor` で上書き）
 */
internal fun applyCellBaseLayout(
    views: CellBaseViews,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Boolean = true,
) {
    val ctx = views.root.context

    // isEnabled = false 時は disabledTextColor で各テキスト色を置換する。
    val titleColor = if (isEnabled) effective.titleColor else effective.disabledTextColor
    val descColor = if (isEnabled) effective.descriptionColor else effective.disabledTextColor
    val valueColor = if (isEnabled) effective.valueTextColor else effective.disabledTextColor
    // hintText 色: SoT は `EffectiveStyle.effectiveHintTextColor`（CellStyle.hintTextColor →
    // Theme.cellHintTextColor → Theme.cellAccentColor）。
    val hintColor = if (isEnabled) effective.hintTextColor else effective.disabledTextColor

    // title 反映
    views.titleView.text = title
    views.titleView.setTextColor(titleColor)
    views.titleView.typeface = effective.titleTypeface
    views.titleView.textSize = effective.titleSizeSp

    // description 反映
    if (description != null) {
        views.descriptionView.visibility = View.VISIBLE
        views.descriptionView.text = description
        views.descriptionView.setTextColor(descColor)
        views.descriptionView.typeface = effective.descriptionTypeface
        views.descriptionView.textSize = effective.descriptionSizeSp
    } else {
        views.descriptionView.visibility = View.GONE
        views.descriptionView.text = null
    }

    // valueText 反映
    if (valueText != null) {
        views.valueTextView.visibility = View.VISIBLE
        views.valueTextView.text = valueText
        views.valueTextView.setTextColor(valueColor)
        views.valueTextView.typeface = effective.valueTextTypeface
        views.valueTextView.textSize = effective.valueTextSizeSp
    } else {
        views.valueTextView.visibility = View.GONE
        views.valueTextView.text = null
    }

    // 主行の幅配分（core/ADR-0026）: 行内 trailing がある行では title をコンテンツ幅にして
    // 残り幅を行内 trailing へ渡し、行内 trailing が無い行では title が主行の全幅を使う。
    // 再利用された行でも valueText の有無に追随するよう、bind のたびに評価する。
    applyTitleWidthMode(
        views,
        fillsRow = valueText == null && !views.hasFillingInlineTrailing,
    )

    // icon 枠: 画像の有無・実寸によらず、解決済みの寸法と角丸を毎 bind で反映する。
    applyIconFrame(views.iconView, effective)

    // hintText 反映
    if (hintText != null) {
        views.hintTextView.visibility = View.VISIBLE
        views.hintTextView.text = hintText
        views.hintTextView.setTextColor(hintColor)
        views.hintTextView.typeface = effective.hintTextTypeface
        views.hintTextView.textSize = effective.hintTextSizeSp
        // `bringToFront()` はここでは呼ばない（毎 bind ごとに ViewGroup.requestLayout() を誘発する
        // ためパフォーマンスへの影響がある）。Z 順前面は `buildCellBaseViews` で
        // `accessoryHolder` よりも後に `hintTextView` を `addView` することで静的に保証している。
        // ViewHolder 側で bind 後に `accessoryHolder.addView(...)` を行っても hintTextView の Z 順は
        // 変動しない（accessoryHolder の子の追加は accessoryHolder 内部の階層であるため、
        // root の子 View の描画順は変わらない）。
    } else {
        views.hintTextView.visibility = View.GONE
        views.hintTextView.text = null
    }

    // icon 解決（KsImage sealed 派生に応じて切り替える）
    when (icon) {
        null -> {
            views.iconView.visibility = View.GONE
            views.iconView.setImageDrawable(null)
        }
        is KsImage.Drawable -> {
            views.iconView.setImageDrawable(icon.drawable)
            views.iconView.visibility = View.VISIBLE
        }
        is KsImage.Resource -> {
            val drawable = ContextCompat.getDrawable(ctx, icon.resId)
            if (drawable != null) {
                views.iconView.setImageDrawable(drawable)
                views.iconView.visibility = View.VISIBLE
            } else {
                views.iconView.visibility = View.GONE
                views.iconView.setImageDrawable(null)
            }
        }
        is KsImage.SystemName -> {
            // SystemName は iOS の SF Symbols。Android では解決不可で非表示にフォールバック。
            views.iconView.visibility = View.GONE
            views.iconView.setImageDrawable(null)
        }
    }

    // 背景色を root に適用（タッチフィードバックを伴う Ripple は ViewHolder 側で applyCellBackground を呼ぶ）。
    // 背景は applyCellBackground で上書きされるため、ここでは EffectiveStyle.backgroundColor の確実な反映用に
    // root.isEnabled を更新するだけにとどめる。
    views.root.isEnabled = isEnabled
}

/**
 * icon 領域を解決済み icon size の正方形枠にし、解決済み radius に応じて角丸 clip を切り替える。
 *
 * 枠の一辺は [EffectiveStyle.iconSizeDp] だけで決まり、画像の実寸や縦横比には依存しない。
 * 画像は [android.widget.ImageView.ScaleType.FIT_CENTER] で枠へ収め、枠を超えない。
 * 角丸は枠に対してかかり、aspect fit 後の画像の描画矩形には追従しない（core/ADR-0025）。
 * 半径が 0（角丸なし）のときは clip を解除するため、同じ行を別の値で再 bind しても
 * 前回の clip 状態が残らない。
 */
private fun applyIconFrame(iconView: AppCompatImageView, effective: EffectiveStyle) {
    val density = iconView.resources.displayMetrics.density
    val sizePx = (effective.iconSizeDp * density).toInt()
    val radiusPx = effective.iconRadiusDp * density

    iconView.scaleType = ImageView.ScaleType.FIT_CENTER

    val lp = iconView.layoutParams
    if (lp != null && (lp.width != sizePx || lp.height != sizePx)) {
        lp.width = sizePx
        lp.height = sizePx
        iconView.layoutParams = lp
    }

    if (radiusPx > 0f) {
        val current = iconView.outlineProvider as? IconFrameOutlineProvider
        if (current == null || current.radiusPx != radiusPx) {
            // provider は半径を不変で持つので、半径が変わったときはインスタンスごと差し替える。
            // View 側は provider の代入時に outline を作り直すため、別途の無効化は要らない。
            iconView.outlineProvider = IconFrameOutlineProvider(radiusPx)
        }
        iconView.clipToOutline = true
    } else {
        if (iconView.outlineProvider is IconFrameOutlineProvider) {
            iconView.outlineProvider = ViewOutlineProvider.BACKGROUND
        }
        iconView.clipToOutline = false
    }
}

/**
 * icon 領域の正方形枠へ角丸の clip 形状を与える [ViewOutlineProvider]。
 *
 * 形状は View の実寸から決めるため、枠の一辺が変わっても outline がそのまま追随する。
 *
 * @property radiusPx 角丸半径（px）
 */
private class IconFrameOutlineProvider(val radiusPx: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
    }
}

/**
 * Cell コンテナの背景に [RippleDrawable] を適用する共通ヘルパ。
 *
 * 通常背景に [EffectiveStyle.backgroundColor]、タップ時の Ripple 色に [EffectiveStyle.selectedColor]
 * （= `Theme.selectedColor`、既定 `#D9D9D9` ≒ オリジナルの `Rgb(180,180,180)` 相当）を用いる。
 * 全 Cell の ViewHolder で本ヘルパを使用し、タッチフィードバックを統一適用する。
 *
 * [RippleDrawable] の ripple エフェクトは、その View が押下状態（`android.R.attr.state_pressed`）を
 * 受け取れる＝ `isClickable == true` でなければ発生しない。本ヘルパで `view.isClickable = true` を設定する。
 */
internal fun applyCellBackground(view: View, effective: EffectiveStyle) {
    val ripple = RippleDrawable(
        ColorStateList.valueOf(effective.selectedColor),
        ColorDrawable(effective.backgroundColor),
        null,
    )
    view.background = ripple
    view.isClickable = true
}

/**
 * 実効行高さを Cell コンテナに適用する共通ヘルパ。
 *
 * - `effective.isFixedHeight == true`（`Theme.hasUnevenRows == false`）:
 *   `layoutParams.height = effectiveHeightPx` で **固定高さ** を適用。
 * - `effective.isFixedHeight == false`: `layoutParams.height = WRAP_CONTENT` かつ
 *   `minimumHeight = effectiveHeightPx` で **最低高さ保証付きの可変高さ** を適用。
 *
 * 前回値と異なる場合のみ `requestLayout()` を呼ぶ（パフォーマンス）。
 */
internal fun applyEffectiveHeight(view: View, effective: EffectiveStyle) {
    val heightPx = EffectiveStyle.dpToPx(view.context, effective.effectiveHeightDp)
    val lp = view.layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    )

    val targetHeight: Int =
        if (effective.isFixedHeight) heightPx else ViewGroup.LayoutParams.WRAP_CONTENT
    val targetMinHeight: Int = if (effective.isFixedHeight) 0 else heightPx

    var changed = false
    if (lp.height != targetHeight) {
        lp.height = targetHeight
        changed = true
    }
    if (view.minimumHeight != targetMinHeight) {
        view.minimumHeight = targetMinHeight
        changed = true
    }
    if (changed) {
        view.layoutParams = lp
        view.requestLayout()
    }
}
