package jp.kamusoft.kssettingsview.ui

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import jp.kamusoft.kssettingsview.R

/**
 * [CustomCell] 描画用 ViewHolder。
 *
 * `itemView` は [ComposeCellViewHolder] が用意する `ComposeView` そのもの（full-bleed）であり、
 * 共通行レイアウト（[CellBaseViews]）は使わない（core/ADR-0022）。
 *
 * # content のリサイクル
 *
 * `setContent` は生成時に一度だけ張り、行ごとの値は `MutableState` の書き換えで差し替える
 * （android/ADR-0015）。宣言ツリーは `ReusableContentHost` と `ReusableContent` で包む。
 *
 * - [contentKey]（Cell の同一性）が変わると、`remember` / `DisposableEffect` は破棄され、再利用可能な
 *   ノード（reusable node と `onReset` 付きの `AndroidView`）だけが引き継がれる。行をまたいで content の
 *   内部状態が持ち越されることはない
 * - [isContentActive] が `false` の間は content が非活性になり、`remember` の保持物が破棄されて
 *   ノードツリーだけが残る。プール投入時（[reset]）に非活性化し、[bind] で活性へ戻す
 *
 * 値をラムダのキャプチャではなく state として composition の内側から読ませるのは、`setContent` を
 * 張り直さずに更新を届けるため。キャプチャした値は composition を張り直さない限り更新されない。
 *
 * # なぜ宣言ツリーを [Layout] で包むのか
 *
 * 非活性のノードは measure できない。Compose は非活性ノードの `remeasure` を
 * `IllegalArgumentException("measure is called on a deactivated node")` で拒否する。そして
 * `ComposeView` のルートに置かれた `RootMeasurePolicy` は、子が非活性かどうかを見ずに無条件で
 * 測るため、`ReusableContentHost` を `setContent` の直下に置くと非活性の瞬間に必ず落ちる。
 * `ReusableContentHost` は本来 `SubcomposeLayout` の内側で使われる機構であり、`SubcomposeLayout` の
 * measure policy が非活性の slot を測らないことで成立している。
 *
 * その「測らない」責務をここで自分で持つ。[Layout] の measure policy は content が活性な composition
 * として存在する間だけ子を測り、そうでない間は行の高さだけ確保して測定を見送る。
 *
 * 判定に使うのは要求値（[isContentActive]）ではなく反映値（[isContentComposed]）である。`RecyclerView`
 * は `onBindViewHolder` と `measureChildWithMargins` を同一のレイアウトパスで続けて実行するが、
 * [bind] の書き込みが composition へ届くのは次の composition であり、その間は「活性を要求したが
 * ノードはまだ非活性」という状態になるため。
 *
 * この構造では、プールで非活性化された行を再 bind したとき、content が現れるのは再活性化が
 * composition へ反映された後（通常は次のフレーム）になる。その間の行の高さは確保するが、確保値の
 * 正確さは行高さの決まり方で変わる。固定高さの行（`Theme.hasUnevenRows == false`）は解決値
 * （[heightDpState]）がそのまま行高さなので確保値は正確で、レイアウトは動かない。可変高さの行では
 * 解決値は最低高でしかなく、新しい content の自然高は測らなければ分からない（測れないことがこの
 * 分岐の理由なので、この 1 フレームでは原理的に得られない）。直前に測った行高さ
 * （[lastContentHeightPx]）を確保値の下限に使って縮みを抑えるが、新旧の行高さが違えばその 1 フレーム
 * だけ高さがずれる。`RecyclerView` の prefetch が効く経路では、表示より前に bind されるため反映が
 * 表示に間に合う。
 *
 * Disclosure Indicator は UIKit / classic View 側の accessory ではなく、hosted な Compose ツリーの
 * 内側で合成する。共通行レイアウトのアクセサリと同じアセットと寸法定数
 * （[CELL_DISCLOSURE_WIDTH_DP] / [CELL_DISCLOSURE_HEIGHT_DP] / [CELL_ROW_HORIZONTAL_MARGIN_DP]）を
 * 共有することで見た目を揃える。
 */
internal class CustomCellViewHolder(context: Context) : ComposeCellViewHolder<CustomCell<*>>(context) {

    /** content を活性に保つかどうか。`false` で非活性化（ノードは保持し remember だけ破棄）する。 */
    private val isContentActive = mutableStateOf(false)

    /** content の同一性キー。Cell の `id` を入れる（位置や内容ハッシュは同一性を表さない）。 */
    private val contentKey = mutableStateOf<String?>(null)

    /** 描画する content（型消去済みの Cell の描画エントリポイント）。 */
    private val contentState = mutableStateOf<@Composable () -> Unit>(EMPTY_CELL_CONTENT)

    /** Disclosure Indicator を表示するか。 */
    private val showArrowState = mutableStateOf(false)

    /** content を有効状態で描画するか。 */
    private val isContentEnabledState = mutableStateOf(true)

    /** 行高さの解決値（dp）。 */
    private val heightDpState = mutableIntStateOf(0)

    /** 行高さを固定するか（`false` なら最低高として働く）。 */
    private val isFixedHeightState = mutableStateOf(false)

    /**
     * content が composition に活性な状態で存在するか（[isContentActive] の「反映済み」の値）。
     *
     * measure の可否はこちらで判定する。要求値（[isContentActive]）は [bind] の時点で `true` に
     * なるが、ノードが実際に再活性化されるのは次の composition であり、その間に measure が来ると
     * 非活性ノードを測ることになる。
     */
    private val isContentComposed = mutableStateOf(false)

    /**
     * content を保持している間に測った、直近の行の高さ（px）。可変高さの行が非活性の間、確保する
     * 高さの下限に使う。
     *
     * 非活性の間は content を測れないため、行の自然高を新しく得る手立てがない。同じ器が直前に
     * 描いていた行の高さを下限に置くことで、最低高まで縮んで後続行がせり上がるのを防ぐ。
     *
     * 更新するのは [isContentActive] が `true` の間の測定だけにする。[reset] は空 content 化と
     * 非活性化を同一スナップショットに書くため両者は composition へ揃って届く — 反映前の測定が
     * 観測するのは旧 content の高さであり、反映後は非活性分岐が content を測らない。したがって
     * 現行経路でこの条件が効く瞬間はなく、活性を要求していない間の測定を下限に取り込まないため
     * の防御として置いている。
     *
     * measure の中だけで読み書きするため snapshot state にしない。値が変わるのは content を測った
     * 直後であり、その測定結果はそのフレームの行高さとしてすでに反映されている。
     */
    private var lastContentHeightPx: Int = 0

    init {
        // RecyclerView へ追加される前の bind でも高さ適用が効くよう、既定の LayoutParams を持たせる。
        composeView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        composeView.setContent {
            Layout(
                content = {
                    ReusableContentHost(active = isContentActive.value) {
                        ReusableContent(key = contentKey.value) {
                            // 活性・非活性が composition へ実際に反映された瞬間を measure 側へ伝える。
                            // effect は composition の適用と同じ同期点で走るため、ノードの活性状態と
                            // この値はずれない。
                            DisposableEffect(Unit) {
                                isContentComposed.value = true
                                onDispose { isContentComposed.value = false }
                            }
                            CustomCellRow(
                                content = contentState.value,
                                showArrow = showArrowState.value,
                                isEnabled = isContentEnabledState.value,
                                heightDp = heightDpState.intValue,
                                isFixedHeight = isFixedHeightState.value,
                            )
                        }
                    }
                },
            ) { measurables, constraints ->
                if (!isContentComposed.value) {
                    // 非活性のノードは measure できない（Compose が例外で拒否する）。行の高さだけ
                    // 確保して測定を見送り、再活性化が composition へ反映された次の測定で描く。
                    //
                    // 固定高さの行は解決値がそのまま行高さなので、確保値は実際の行高さと一致する。
                    // 可変高さの行では解決値は最低高でしかなく、content の自然高はここでは測れない
                    // ため正確な値を得る手立てがない。直前に測った行高さを下限に使い、少なくとも
                    // 行が縮む向きのずれは起こさないようにする。
                    val minHeight = heightDpState.intValue.dp.roundToPx()
                    val reservedHeight = if (isFixedHeightState.value) {
                        minHeight
                    } else {
                        maxOf(minHeight, lastContentHeightPx)
                    }
                    layout(constraints.minWidth, constraints.constrainHeight(reservedHeight)) {}
                } else {
                    val placeables = measurables.map { it.measure(constraints) }
                    val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
                    val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
                    val rowHeight = constraints.constrainHeight(height)
                    if (isContentActive.value) {
                        lastContentHeightPx = rowHeight
                    }
                    layout(constraints.constrainWidth(width), rowHeight) {
                        placeables.forEach { it.place(0, 0) }
                    }
                }
            }
        }
    }

    override fun bind(cell: CustomCell<*>, theme: Theme) {
        val effective = EffectiveStyle.from(composeView.context, theme, cell.style)

        // 行レベルの style（背景色 / 選択時色）を適用する。テキスト色・フォント等の
        // コンテンツ内装項目は builder の出力が持つため、ここでは適用先を持たない。
        applyCellBackground(composeView, effective)

        // 以下の書き込みは同一スナップショットの中で行われ、次の再 composition ですべてが揃った状態で
        // 読まれる。したがって代入の順序は結果に影響しない（活性化を先頭に置いても同じ）。
        contentKey.value = cell.id
        contentState.value = cell.composeContent
        showArrowState.value = cell.showArrow
        isContentEnabledState.value = cell.isEnabled
        heightDpState.intValue = effective.effectiveHeightDp
        isFixedHeightState.value = effective.isFixedHeight
        isContentActive.value = true

        val isEnabled = cell.isEnabled

        // タップ通知。毎回 listener を上書き（または解除）することで、再利用時に旧クロージャが
        // 残らないようにする。content 内の要素がジェスチャを消費した場合、Compose 側が
        // タッチイベントを消費するため itemView の click listener までは届かない。
        val tapHandler = cell.onTap
        if (isEnabled && tapHandler != null) {
            composeView.setOnClickListener { tapHandler.invoke() }
        } else {
            composeView.setOnClickListener(null)
        }

        // 有効な行は onTap の有無にかかわらず clickable にする。共通行の視覚状態契約では
        // clickable flag は「押下 feedback（ripple）を出すための状態」であって callback の
        // 存在を意味しない（LabelCell も handler なしで clickable を持つ）。
        composeView.isClickable = isEnabled

        // hosting View 自体も無効状態にする。ripple の state_enabled が落ちて押下 feedback が
        // 消え、View の click 経路も塞がる。
        composeView.isEnabled = isEnabled
        // キーボード / D-pad フォーカスが composition の内側へ入るのを塞ぐ。Compose のフォーカス
        // 探索は内包する View がフォーカスを取れることを前提にするため、View 階層で止めれば
        // 子孫の操作可能要素へフォーカスが渡らない。
        composeView.descendantFocusability = if (isEnabled) {
            ViewGroup.FOCUS_AFTER_DESCENDANTS
        } else {
            ViewGroup.FOCUS_BLOCK_DESCENDANTS
        }

        applyEffectiveHeight(composeView, effective)
    }

    /**
     * リサイクルプールへ入るときの後始末。
     *
     * content を非活性にし、直前の builder を指す参照を手放す。`remember` の保持物と
     * `DisposableEffect` が実際に破棄される（購読が止まる）のは、非活性化が再 composition に
     * 観測された時点であり、この関数の戻り時点ではない。ノードツリーは保持したままなので、次の
     * [bind] で再利用できる（android/ADR-0015）。Composition 自体の破棄は基底が指定する破棄戦略が担う。
     *
     * `RecyclerView` が同一のレイアウトパスの中で行の recycle と bind を続けて行う経路では、非活性は
     * 一度も再 composition に観測されない。この経路での行間の状態隔離は、同一性キー（[contentKey]）が
     * 変わることだけが担う。
     *
     * 保持されたノードが持つ旧 content 由来の参照（Modifier やパラメータの slot）は、次の再利用か
     * Composition の破棄まで残る。ViewHolder が直接握る参照（content と click listener）はここで
     * 確実に切れる。
     *
     * 画面外へ出ただけで `itemViewCache` に留まっている行はこの経路を通らず、content は活性のまま
     * 維持される。cache 経由の行は bind を経ずに再表示され得るため、非活性化すると空の行が現れる。
     */
    override fun reset() {
        composeView.setOnClickListener(null)
        composeView.isClickable = false
        composeView.isEnabled = true
        composeView.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        isContentActive.value = false
        contentState.value = EMPTY_CELL_CONTENT
    }
}

/** content 未設定の [CustomCellViewHolder] が保持する、何も描かない content。 */
private val EMPTY_CELL_CONTENT: @Composable () -> Unit = {}

/**
 * [CustomCell] の 1 行分の Compose ツリー。
 *
 * [content] が accessory 領域を除いた残り幅を占有し、[showArrow] が `true` のとき trailing に
 * Disclosure Indicator を並べる。
 *
 * 行高さの解決は宣言 UI の内側で行う。`ComposeView` は `onMeasure` を内部の composition へ
 * 委譲するため、View の `minimumHeight` が効かない。[isFixedHeight] が `true` なら [heightDp] へ
 * 固定し、`false` なら [heightDp] を最低高として content の自然高に従わせる。
 *
 * [isEnabled] が `false` のときは以下を [content] へ重ねる。
 *
 * - Initial パスでのポインタイベント消費（タッチ経路の遮断）
 * - accessibility action の遮断（[blockDescendantActions]。TalkBack 等はポインタを経由せず
 *   semantics action を直接実行するため、ポインタ遮断だけでは操作を止められない）
 * - 淡色化（[DISABLED_CONTENT_ALPHA]）
 *
 * 淡色化の対象は [content] だけであり、行の背景と Disclosure Indicator には掛けない。
 */
@Composable
private fun CustomCellRow(
    content: @Composable () -> Unit,
    showArrow: Boolean,
    isEnabled: Boolean,
    heightDp: Int,
    isFixedHeight: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFixedHeight) {
                    Modifier.height(heightDp.dp)
                } else {
                    Modifier.heightIn(min = heightDp.dp)
                },
            )
            .consumePointerInput(blocked = !isEnabled),
        verticalAlignment = CenterOrTopVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (isEnabled) Modifier else Modifier.alpha(DISABLED_CONTENT_ALPHA))
                .blockDescendantActions(blocked = !isEnabled),
        ) {
            content()
        }
        if (showArrow) {
            Image(
                painter = painterResource(R.drawable.ic_navigate_next),
                contentDescription = DISCLOSURE_CONTENT_DESCRIPTION,
                modifier = Modifier
                    .padding(end = CELL_ROW_HORIZONTAL_MARGIN_DP.dp)
                    .size(
                        width = CELL_DISCLOSURE_WIDTH_DP.dp,
                        height = CELL_DISCLOSURE_HEIGHT_DP.dp,
                    ),
            )
        }
    }
}

/**
 * [blocked] が `true` のとき、Initial パスで全ポインタイベントを消費して子孫へのタッチ配送を止める。
 *
 * Initial パスはルートから葉へ向かって伝播するため、親であるこの Modifier が先に消費を宣言でき、
 * 子孫のジェスチャ検出（`clickable` / Slider 等）は消費済みの変化を無視する。
 */
private fun Modifier.consumePointerInput(blocked: Boolean): Modifier =
    if (!blocked) {
        this
    } else {
        this.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    }

/**
 * 行の中での content の縦位置。収まるときは縦中央、収まらないときは上端揃えにする。
 *
 * `Alignment.CenterVertically` は content が行より高いとき負のオフセットを返し、content が上下へ
 * 均等にはみ出す。行高さが固定（`Theme.hasUnevenRows == false`）で content がその高さを超える場合、
 * 上へはみ出した分は見えないまま切れるため、上端から順に見える方が自然になる。
 */
private val CenterOrTopVertically: Alignment.Vertical =
    Alignment.Vertical { size, space -> ((space - size) / 2).coerceAtLeast(0) }

/**
 * [blocked] が `true` のとき、子孫の semantics を accessibility ツリーから切り離し、subtree 全体を
 * 1 つの無効ノードとして見せる。
 *
 * TalkBack / Switch Access などの accessibility service はポインタイベントを配送せず、semantics
 * ツリーの action（`OnClick` / `SetProgress` 等）を直接実行する。したがって [consumePointerInput]
 * だけでは content 内部の操作を抑止できない。
 *
 * `clearAndSetSemantics` を使うのは、`mergeDescendants` による畳み込みでは足りないため。畳み込み
 * では「自身も畳み込みノードである子孫」（`clickable` / `Slider` など、操作可能要素はまさにこれに
 * 当たる）が独立ノードとして accessibility ツリーに残り、その action を service から実行できて
 * しまう。`clearAndSetSemantics` は subtree を丸ごと置き換えるため、残る経路がない。
 *
 * 引き換えに、無効な行の content は読み上げ対象から外れる。任意の Compose ツリーである content に
 * 対しては「操作可能要素だけを無効化して読み上げは残す」という選択肢が取れないため、視覚状態契約の
 * 「無効 Cell は操作 callback と内包 control の操作を抑止する」を優先する（core/ADR-0017）。
 */
private fun Modifier.blockDescendantActions(blocked: Boolean): Modifier =
    if (!blocked) {
        this
    } else {
        this.clearAndSetSemantics { disabled() }
    }

/** Disclosure Indicator の contentDescription（共通行レイアウトのアクセサリと同一文言）。 */
private const val DISCLOSURE_CONTENT_DESCRIPTION: String = "Disclosure indicator"

/**
 * `isEnabled = false` のときに content へ掛ける不透明度。
 *
 * 共通行レイアウトの Cell はテキスト色を `Theme.disabledTextColor`（既定 `#999999`）へ置換して
 * 無効を表すが、任意の Compose ツリーである content には色の置換を適用できない。白背景上で
 * `#999999` 相当の濃度になる値を選び、標準 Cell と並んだときの見え方を揃える。
 */
private const val DISABLED_CONTENT_ALPHA: Float = 0.38f
