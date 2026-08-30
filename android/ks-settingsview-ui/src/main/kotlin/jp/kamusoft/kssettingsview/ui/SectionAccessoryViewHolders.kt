package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.graphics.toArgb
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SectionAccessory

/**
 * Section H/F の `Text` 形式を描画する ViewHolder。
 *
 * `SectionAccessory.Text(value)` を `TextView` に描画する。Header / Footer どちらの位置でも
 * 共通の ViewHolder を使い、bind 時に `isHeader` フラグで色を切り替える（Theme 由来）。
 *
 * Section の装飾はモデル（`Section.header` / `footer`）が持つため、`CellListItem.SectionHeader` /
 * `SectionFooter` として `mainListAdapter` の項目に載る（core/ADR-0005）。
 */
internal class SectionTextAccessoryViewHolder(view: TextView) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view

    /**
     * Text 形式の Section H/F を ViewHolder に反映する。
     *
     * 縦位置と固定高さの規則:
     * - `isHeader = true` のときは [Gravity.BOTTOM] | [Gravity.START] で **下端揃え**
     *   （AiForms オリジナル `Platforms/iOS/TextHeaderView.SetVerticalAlignment(LayoutAlignment.End)` 既定挙動に揃える）。
     * - `isHeader = false` のときは [Gravity.TOP] | [Gravity.START] で **上端揃え**
     *   （AiForms オリジナル `Platforms/iOS/TextFooterView` の TopAnchor 既定挙動に揃える）。
     * - 固定高さの解決は [applySectionHeaderHeight] に委ね、accessory 種別に依らず同じ規則で適用する。
     *
     * @param accessory 表示する `SectionAccessory.Text`
     * @param theme 文字色 / 背景色を取得するための Theme
     * @param isHeader Header の場合 true（色は `headerTextColor` / `headerBackgroundColor`、下端揃え）、
     *   Footer の場合 false（`footerTextColor` / `footerBackgroundColor`、上端揃え）
     * @param headerHeight Header の固定高さ（dp 単位）。Header のみに適用。`-1.0` で自動高さ。
     *   Footer 側からの呼び出しでは無視される。
     */
    fun bind(
        accessory: SectionAccessory.Text,
        theme: Theme,
        isHeader: Boolean,
        headerHeight: Double = -1.0,
    ) {
        textView.text = accessory.value
        if (isHeader) {
            textView.setTextColor(theme.headerTextColor.toArgb())
            textView.setBackgroundColor(theme.headerBackgroundColor.toArgb())
            // Header テキストは下端揃え（AiForms 既定挙動に揃える）。
            textView.gravity = Gravity.BOTTOM or Gravity.START
        } else {
            textView.setTextColor(theme.footerTextColor.toArgb())
            textView.setBackgroundColor(theme.footerBackgroundColor.toArgb())
            // Footer テキストは上端揃え（AiForms 既定挙動に揃える）。
            textView.gravity = Gravity.TOP or Gravity.START
        }
        applySectionTextVerticalPadding(textView, isHeader)

        // `Theme.headerFont` / `Theme.footerFont` を描画に反映する。
        // `headerFontSize` / `footerFontSize > 0` のとき size を上書きする。
        // 責務は EffectiveStyle に集約し、Typeface / sp は本 ViewHolder で適用する。
        applyHeaderFooterFont(theme = theme, isHeader = isHeader)

        applySectionHeaderHeight(
            view = textView,
            theme = theme,
            isHeader = isHeader,
            headerHeight = headerHeight,
        )
    }

    /**
     * Header / Footer 用フォント (`Theme.headerFont` / `Theme.footerFont`) と size 上書き
     * (`headerFontSize` / `footerFontSize > 0`) を本 ViewHolder の TextView に反映する。
     *
     * 解決自体は [EffectiveStyle.effectiveHeaderOrFooterFont] に集約し、本メソッドは
     * Typeface と sp への変換・適用だけを担う。
     */
    private fun applyHeaderFooterFont(theme: Theme, isHeader: Boolean) {
        val style = EffectiveStyle.effectiveHeaderOrFooterFont(theme = theme, isHeader = isHeader)
        textView.typeface = style.toTypeface()
        val sizeSp: Float? = if (isHeader) {
            if (theme.headerFontSize > 0.0) theme.headerFontSize.toFloat()
            else style.fontSize.toSpFloatOrNull()
        } else {
            if (theme.footerFontSize > 0.0) theme.footerFontSize.toFloat()
            else style.fontSize.toSpFloatOrNull()
        }
        if (sizeSp != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }
        // sizeSp が null（Theme.headerFont 未指定 / fontSize 未指定 / sp/em 以外単位）の場合、
        // TextView 既定 size をそのまま使う（明示的に変更しない）。
    }

    companion object {
        fun create(parent: ViewGroup): SectionTextAccessoryViewHolder {
            // テキスト accessory はライブラリ所有の chrome なので同梱テーマで生成する（android/ADR-0020）。
            val ctx = parent.ksThemedContext()
            val tv = createSectionTextView(ctx)
            return SectionTextAccessoryViewHolder(tv)
        }
    }
}

/**
 * Section H/F の `View` 形式（[KsAnyView] backing）を描画する ViewHolder。
 *
 * 内部 container（[FrameLayout]）に [KsAnyView] の中身を入れ替え式で表示する。
 *
 * - [KsAnyView.Compose]: container 配下に `ComposeView` を配置し、`setContent { content() }`
 *   で再描画する。`ComposeView` は `DisposeOnDetachedFromWindow` を強制する。
 * - [KsAnyView.AndroidView]: `factory(context)` で生成した `View` を `addView` する。
 *
 * # ライフサイクル
 *
 * `bind` のたびに container をクリアして新しい中身を入れ直す。これは `KsAnyView` が
 * 差分検出に参加しないため、bind の度に最新の中身が入ってくる前提によるもの。
 * `reset()` でも container をクリアして参照解放する。
 * ただし [applyHeaderHeight] は container をクリアしないもう 1 つの入口で、高さのみの
 * 更新では hosted view が入れ替わらずに生き残る。
 */
internal class SectionAnyViewAccessoryViewHolder(view: FrameLayout) : RecyclerView.ViewHolder(view) {

    private val container: FrameLayout = view

    /**
     * `SectionAccessory.View` の中身と Header の固定高さを ViewHolder に反映する。
     *
     * 固定高さの解決は Text accessory と共通で [applySectionHeaderHeight] が行う。指定が無ければ
     * container は `WRAP_CONTENT` に戻るため、再利用された ViewHolder が前回の固定高さを引きずらない。
     * 固定高さのときは中身も領域いっぱいに配置される（[applyHeaderHeight]）。
     *
     * @param accessory 表示する `SectionAccessory.View`
     * @param theme 固定高さのフォールバック（`Theme.headerHeight`）を取得するための Theme
     * @param isHeader Header の場合 true。Footer の場合 false（固定高さの対象外）
     * @param headerHeight Header の固定高さ（dp 単位）。`-1.0` で Theme のフォールバックへ委ねる
     */
    fun bind(
        accessory: SectionAccessory.View,
        theme: Theme,
        isHeader: Boolean,
        headerHeight: Double = -1.0,
    ) {
        bindKsAnyView(container, accessory.view)
        applyHeaderHeight(theme = theme, isHeader = isHeader, headerHeight = headerHeight)
    }

    /**
     * 中身を再構築せずに Header の固定高さだけを反映する。
     *
     * 高さのみが変化した更新で使う。`KsAnyView.AndroidView` の View は factory から作り直すと
     * 内部状態を失うため、高さの反映が中身の再構築を巻き込まない経路を分けている。
     *
     * 固定高さが解決されたときは、container 自身の高さに加えて中身（hosted view）も領域いっぱいへ
     * 広げる（[applyHostedViewFill]）。bind 経由でもこの経路を通るため、固定 ⇔ 自動の切り替えに
     * 中身の占有範囲も追随する。
     *
     * @param theme 固定高さのフォールバック（`Theme.headerHeight`）を取得するための Theme
     * @param isHeader Header の場合 true。Footer の場合 false（固定高さの対象外）
     * @param headerHeight Header の固定高さ（dp 単位）。`-1.0` で Theme のフォールバックへ委ねる
     */
    fun applyHeaderHeight(theme: Theme, isHeader: Boolean, headerHeight: Double) {
        val fixedHeight = applySectionHeaderHeight(
            view = container,
            theme = theme,
            isHeader = isHeader,
            headerHeight = headerHeight,
        )
        applyHostedViewFill(container = container, fill = fixedHeight)
    }

    /**
     * ViewHolder 再利用時の状態リセット。
     * container を空にして `KsAnyView` 由来の View 参照を解放する。
     */
    fun reset() {
        container.removeAllViews()
        // ComposeView 再利用キャッシュも破棄して、次回 bind で再生成させる
        container.tag = null
    }

    companion object {
        fun create(parent: ViewGroup): SectionAnyViewAccessoryViewHolder {
            // container の中身は利用者所有の View なので、ホストの Context のまま生成する
            // （利用者 View のテーマ属性はホストのテーマで解決させる。android/ADR-0020）。
            val ctx = parent.context.ksHostContext()
            val container = createAccessoryContainer(ctx)
            return SectionAnyViewAccessoryViewHolder(container)
        }
    }
}

/**
 * Root H/F の `Text` 形式を描画する ViewHolder（[RootHeaderFooterAdapter] から利用）。
 *
 * Section の Text Accessory と同じ `TextView` ベースだが、Root 用は色を `headerTextColor` /
 * `footerTextColor` に固定せず、独立した見た目（やや大きめの文字、太字）で描画する想定。
 * 現状は Theme の Header / Footer 色を流用しつつ、`isHeader` で切り替える。
 */
internal class RootTextAccessoryViewHolder(view: TextView) : RecyclerView.ViewHolder(view) {

    private val textView: TextView = view

    fun bind(accessory: RootAccessory.Text, theme: Theme, isHeader: Boolean) {
        textView.text = accessory.value
        if (isHeader) {
            textView.setTextColor(theme.headerTextColor.toArgb())
            textView.setBackgroundColor(theme.headerBackgroundColor.toArgb())
        } else {
            textView.setTextColor(theme.footerTextColor.toArgb())
            textView.setBackgroundColor(theme.footerBackgroundColor.toArgb())
        }
        // Root H/F も Section と同じ `Theme.headerFont` / `Theme.footerFont` を反映する。
        val style = EffectiveStyle.effectiveHeaderOrFooterFont(theme = theme, isHeader = isHeader)
        textView.typeface = style.toTypeface()
        val sizeSp: Float? = if (isHeader) {
            if (theme.headerFontSize > 0.0) theme.headerFontSize.toFloat()
            else style.fontSize.toSpFloatOrNull()
        } else {
            if (theme.footerFontSize > 0.0) theme.footerFontSize.toFloat()
            else style.fontSize.toSpFloatOrNull()
        }
        if (sizeSp != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }
    }

    companion object {
        fun create(parent: ViewGroup): RootTextAccessoryViewHolder {
            // テキスト accessory はライブラリ所有の chrome なので同梱テーマで生成する（android/ADR-0020）。
            val ctx = parent.ksThemedContext()
            val tv = createSectionTextView(ctx)
            return RootTextAccessoryViewHolder(tv)
        }
    }
}

/**
 * Root H/F の `View` 形式（[KsAnyView] backing）を描画する ViewHolder。
 *
 * Section 用 [SectionAnyViewAccessoryViewHolder] と同じ機構（`bindKsAnyView`）で描画する。
 */
internal class RootAnyViewAccessoryViewHolder(view: FrameLayout) : RecyclerView.ViewHolder(view) {

    private val container: FrameLayout = view

    fun bind(accessory: RootAccessory.View) {
        bindKsAnyView(container, accessory.view)
    }

    fun reset() {
        container.removeAllViews()
        // ComposeView 再利用キャッシュも破棄して、次回 bind で再生成させる
        container.tag = null
    }

    companion object {
        fun create(parent: ViewGroup): RootAnyViewAccessoryViewHolder {
            // container の中身は利用者所有の View なので、ホストの Context のまま生成する
            // （利用者 View のテーマ属性はホストのテーマで解決させる。android/ADR-0020）。
            val ctx = parent.context.ksHostContext()
            val container = createAccessoryContainer(ctx)
            return RootAnyViewAccessoryViewHolder(container)
        }
    }
}

// -----------------------------------------------------------------------------
// 内部ヘルパ: ViewHolder 用 itemView 構築 / KsAnyView の bind ロジック
// -----------------------------------------------------------------------------

/**
 * Section / Root の Text 用 TextView を生成する（共通スタイル）。
 *
 * - 生成時の上下 padding は **0**。Section H/F として使う場合は
 *   [SectionTextAccessoryViewHolder.bind] が Cell 群に面する側にだけ余白を入れ直す
 *   （[applySectionTextVerticalPadding]）。Root の H/F はここで生成した 0 のまま使う。
 *   Header = bottom / Footer = top の垂直配置は `bind()` 側の `gravity` 設定で担保する。
 * - 横方向 padding は標準左マージン 16dp 相当を維持する。
 */
private fun createSectionTextView(ctx: Context): TextView {
    return TextView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        // 横方向のみ 16dp 相当。上下は 0 で生成し、Section H/F の場合だけ bind 側で入れ直す。
        val pad = (16 * resources.displayMetrics.density).toInt()
        setPadding(pad, 0, pad, 0)
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
    }
}

/** Section Header / Footer のテキストと Cell 群の間に入れる内側余白（dp。両 platform で生値 4 に統一 — core/ADR-0027）。 */
private const val SECTION_TEXT_GAP_DP = 4

/**
 * Section Header / Footer の TextView に、Cell 群に面する側の内側余白を与える。
 *
 * Header は下端揃えでテキストの下に Cell が続くため **下** に、Footer は上端揃えで
 * テキストの上に Cell があるため **上** に [SECTION_TEXT_GAP_DP] を入れる。反対側は 0。
 * 水平 padding は [createSectionTextView] が設定した値を保つ。
 */
private fun applySectionTextVerticalPadding(view: TextView, isHeader: Boolean) {
    val gap = (SECTION_TEXT_GAP_DP * view.resources.displayMetrics.density).toInt()
    view.setPadding(
        view.paddingLeft,
        if (isHeader) 0 else gap,
        view.paddingRight,
        if (isHeader) gap else 0,
    )
}

/**
 * Section Header の固定高さを解決し、[view]（ViewHolder の itemView）の layoutParams へ適用する。
 *
 * 優先順位は Text / View の accessory 種別に依らず共通で、iOS 側の解決とも対称になる
 * （core/ADR-0021、両 OS 対称の公開契約）。
 *
 * 1. [headerHeight]（`Section.headerHeight`）が正値ならその固定高さ
 * 2. `-1.0`（自動）で `Theme.headerHeight` が正値なら Theme の固定高さ
 * 3. いずれも正値でなければ内容に応じた自動高さ（`WRAP_CONTENT`）
 *
 * dp から px への換算は表示密度を掛けて行う。Footer（[isHeader] = false）は固定高さの対象外で、
 * 常に自動高さになる（`Section` に footerHeight は存在しない）。
 *
 * 指定が無いときに `WRAP_CONTENT` へ戻すことで、再利用された ViewHolder が前回の固定高さを
 * 引きずらないことも担保する。
 *
 * [view] は [createSectionTextView] / [createAccessoryContainer] が生成時に layoutParams を
 * 設定した itemView のみを想定するため、layoutParams は非 null として扱う。
 *
 * @return 固定高さが解決されたとき true、自動高さ（`WRAP_CONTENT`）になったとき false
 */
private fun applySectionHeaderHeight(
    view: View,
    theme: Theme,
    isHeader: Boolean,
    headerHeight: Double,
): Boolean {
    val resolvedHeight: Double = when {
        !isHeader -> -1.0
        headerHeight > 0.0 -> headerHeight
        theme.headerHeight > 0.0 -> theme.headerHeight
        else -> -1.0
    }
    val isFixed = resolvedHeight > 0.0
    val targetHeight: Int = if (isFixed) {
        (resolvedHeight * view.resources.displayMetrics.density).toInt()
    } else {
        ViewGroup.LayoutParams.WRAP_CONTENT
    }
    val lp = view.layoutParams
    if (lp.height != targetHeight) {
        lp.height = targetHeight
        view.layoutParams = lp
    }
    return isFixed
}

/**
 * View Accessory の container 配下にある中身（hosted view）の縦方向の占有範囲を切り替える。
 *
 * - [fill] = true（固定高さ）: 中身を `MATCH_PARENT` にして Header 領域全体を占めさせる。
 *   iOS 側が hosted view を contentView の 4 辺へ pin しているのと対称の配置になる（core/ADR-0021）。
 * - [fill] = false（自動高さ）: 中身を `WRAP_CONTENT` に戻し、内容なりの高さで配置する。
 *   container 自身も `WRAP_CONTENT` のため、ここで `MATCH_PARENT` を残すと高さが決まらなくなる。
 *
 * 高さ専用の更新経路（中身を作り直さない更新）でも同じ結果になるよう、中身の追加時ではなく
 * 高さの適用時に設定する。
 */
private fun applyHostedViewFill(container: FrameLayout, fill: Boolean) {
    val targetHeight: Int = if (fill) {
        FrameLayout.LayoutParams.MATCH_PARENT
    } else {
        FrameLayout.LayoutParams.WRAP_CONTENT
    }
    for (index in 0 until container.childCount) {
        val child = container.getChildAt(index)
        // 子は bindKsAnyView が LayoutParams 付きで addView するため、layoutParams は非 null
        val lp = child.layoutParams
        if (lp.height != targetHeight) {
            lp.height = targetHeight
            child.layoutParams = lp
        }
    }
}

/** Section / Root の View Accessory 用コンテナ FrameLayout を生成する。 */
private fun createAccessoryContainer(ctx: Context): FrameLayout {
    return FrameLayout(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }
}

/**
 * [KsAnyView] の中身を `container` に bind する共通ロジック。
 *
 * `KsAnyView.Compose` の場合：
 *   - ViewHolder 単位で `ComposeView` を 1 度だけ生成して再利用する（`container.tag` に
 *     [ComposeAccessoryHolder] をキャッシュ）。`setContent` も生成時 1 度だけ呼び、
 *     bind のたびに更新するのは `MutableState<@Composable () -> Unit>` の値のみとする。
 *     これにより、再 bind 時に毎回コンポジションが新規作成されるのを防ぎ、`ComposeView` の
 *     再利用効果（`DisposeOnDetachedFromWindow` 戦略）と Compose の差分更新を活かせる。
 * `KsAnyView.AndroidView` の場合：
 *   - 既存の中身を取り除き、`factory(context)` で生成した View を `addView` する。
 *
 * Compose と AndroidView 間の入れ替え時には、container の中身を一度クリアして再構築する
 * （tag のキャッシュも作り直す）。
 */
internal fun bindKsAnyView(container: FrameLayout, anyView: KsAnyView) {
    // 中身は利用者所有のコンテンツであり、テーマ属性はホストのテーマで解決させる（android/ADR-0020）。
    val hostContext = container.context.ksHostContext()
    when (anyView) {
        is KsAnyView.Compose -> {
            // Compose ↔ AndroidView の入れ替え時のみ container を作り直す
            val cached = container.tag as? ComposeAccessoryHolder
            if (cached == null) {
                container.removeAllViews()
                val state = mutableStateOf<@Composable () -> Unit>(anyView.content)
                val composeView = ComposeView(hostContext).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        // state.value の差し替えのみで recomposition が走る
                        state.value.invoke()
                    }
                }
                container.addView(
                    composeView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
                container.tag = ComposeAccessoryHolder(composeView, state)
            } else {
                // 既存 ComposeView を再利用し、state の値（描画内容）だけ更新する
                cached.contentState.value = anyView.content
            }
        }
        is KsAnyView.AndroidView -> {
            // Compose 用キャッシュが残っていれば破棄
            container.tag = null
            container.removeAllViews()
            val nativeView: View = anyView.factory(hostContext)
            container.addView(
                nativeView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
    }
}

/**
 * `KsAnyView.Compose` 用の ViewHolder 単位キャッシュ。
 *
 * [ComposeView] は ViewHolder の生存期間で 1 つだけ生成し、再 bind 時は [contentState] を
 * 差し替えることで内容を更新する（recomposition は state 変更で自動的に走る）。
 *
 * `setContent` を毎回呼ばないことで、`DisposeOnDetachedFromWindow` 戦略下でも
 * ComposeView の再利用効果を高める。
 */
private data class ComposeAccessoryHolder(
    val composeView: ComposeView,
    val contentState: MutableState<@Composable () -> Unit>,
)
