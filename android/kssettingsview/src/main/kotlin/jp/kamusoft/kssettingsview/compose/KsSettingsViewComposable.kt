package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
import jp.kamusoft.kssettingsview.ui.KsScrollController
import jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
import jp.kamusoft.kssettingsview.ui.KsSettingsView as KsSettingsViewLayout
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle

/**
 * `KsSettingsView` の Compose ラッパ（Store 方式）。
 *
 * 内部は `AndroidView` で `KsSettingsViewLayout`（FrameLayout 派生）を埋め込み、
 * `factory` で `view.bind(store)` を呼び、`update` で `style` / `rootHeader` / `rootFooter` を反映する。
 *
 * Theme は `store.theme` の StateFlow を View が購読することで反映される（独立経路）。
 *
 * @param store バインドする `SettingsRootStore`
 * @param modifier Compose Modifier
 * @param style 見た目スタイル（既定 [KsSettingsViewStyle.Classic]）
 * @param rootHeader Root Header として描画する Composable（`null` でヘッダ非表示）。[rootHeaderText] より優先する
 * @param rootFooter Root Footer として描画する Composable（`null` でフッタ非表示）。[rootFooterText] より優先する
 * @param rootHeaderText Root Header として描画する文字列（`null` でヘッダ非表示）。一覧の既定の Root Header の
 *   文字の大きさ・色・余白で描画される。[rootHeader] を渡したときは [rootHeader] が表示される
 * @param rootFooterText Root Footer として描画する文字列（`null` でフッタ非表示）。一覧の既定の Root Footer の
 *   文字の大きさ・色・余白で描画される。[rootFooter] を渡したときは [rootFooter] が表示される
 * @param scrollController スクロール命令を受け取るハンドル（`null` なら接続しない）。命令で指す ID は
 *   Store の Cell の `id` と Section の `id`。1 つのハンドルは最後に接続した画面にだけ命令を届ける
 */
@Composable
public fun KsSettingsView(
    store: SettingsRootStore,
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    rootHeader: (@Composable () -> Unit)? = null,
    rootFooter: (@Composable () -> Unit)? = null,
    rootHeaderText: String? = null,
    rootFooterText: String? = null,
    scrollController: KsScrollController? = null,
) {
    bindAndroidView(
        store = store,
        modifier = modifier,
        style = style,
        rootHeader = rootAccessoryOf(content = rootHeader, text = rootHeaderText),
        rootFooter = rootAccessoryOf(content = rootFooter, text = rootFooterText),
        connectScrollController = { view ->
            if (view.scrollController !== scrollController) {
                view.scrollController = scrollController
            }
        },
        releaseScrollController = { view -> view.scrollController = null },
    )
}

/**
 * `KsSettingsView` の Compose ラッパ（DSL 方式）。
 *
 * 内部で `remember { SettingsRootStore(...) }` を保持し、Recomposition のたびに
 * 宣言ツリーを再評価して `SettingsRootDiff` 列を算出、内部 Store の Diff 経路に流す。
 *
 * `theme` パラメータの変化は `store.applyTheme(newTheme)` 経由で View に反映する（Diff 経路ではない）。
 *
 * @param modifier Compose Modifier
 * @param style 見た目スタイル（既定 [KsSettingsViewStyle.Classic]）
 * @param theme UI 層 [Theme]（Compose `Color` / `TextStyle` を直接保持）。既定は現在の外観
 *   （ライト / ダーク）に対応するライブラリ既定色
 * @param rootHeader Root Header として描画する Composable（`null` でヘッダ非表示）。[rootHeaderText] より優先する
 * @param rootFooter Root Footer として描画する Composable（`null` でフッタ非表示）。[rootFooterText] より優先する
 * @param rootHeaderText Root Header として描画する文字列（`null` でヘッダ非表示）。一覧の既定の Root Header の
 *   文字の大きさ・色・余白で描画される。[rootHeader] を渡したときは [rootHeader] が表示される
 * @param rootFooterText Root Footer として描画する文字列（`null` でフッタ非表示）。一覧の既定の Root Footer の
 *   文字の大きさ・色・余白で描画される。[rootFooter] を渡したときは [rootFooter] が表示される
 * @param scrollController スクロール命令を受け取るハンドル（`null` なら接続しない）。命令で指す ID は
 *   `cellID(...)` / `sectionID(...)` で付けた ID、または `forEach` の key。同じ値が両方にあるときは
 *   `cellID(...)` / `sectionID(...)` で付けた要素を指す。同じ処理の中で状態を変えてから出した命令は、
 *   変更後の宣言で解決される
 * @param content DSL レシーバラムダ
 */
@Composable
public fun KsSettingsView(
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    theme: Theme = KsSettingsViewDefaults.theme(),
    rootHeader: (@Composable () -> Unit)? = null,
    rootFooter: (@Composable () -> Unit)? = null,
    rootHeaderText: String? = null,
    rootFooterText: String? = null,
    scrollController: KsScrollController? = null,
    content: DSLSettingsRootScope.() -> Unit,
) {
    // DSL を評価し DSLRootTree を構築する純粋関数。
    fun buildTree(): DSLRootTree {
        val scope = DSLSettingsRootScope().apply(content)
        return DSLRootTree(
            sectionNodes = scope.build(),
            rootHeader = rootAccessoryOf(content = rootHeader, text = rootHeaderText),
            rootFooter = rootAccessoryOf(content = rootFooter, text = rootFooterText),
        )
    }

    // 初回 Composition で Store と前回ツリーを初期化する。
    val bookkeeper = remember {
        val initialTree = buildTree()
        val initialRoot = SettingsRoot(sections = initialTree.resolvedSections())
        DSLBookkeeper(
            store = SettingsRootStore(initialRoot = initialRoot, initialTheme = theme),
            lastTree = DSLDiffCalculator.ResolvedTree(
                sections = initialTree.resolvedSections(),
                rootHeader = initialTree.rootHeader,
                rootFooter = initialTree.rootFooter,
            ),
            lastTheme = theme,
            scrollResolver = DSLScrollCommandResolver(initialTree.resolvedSections()),
        )
    }

    // 命令の引き直しは、コンポジションのフレームを 1 回待ってから行う。同じ処理で行った状態変更に
    // よる宣言の更新 (`AndroidView` の update) は、そのフレームまでに内部 Store へ流れている。
    val resolver = bookkeeper.scrollResolver
    LaunchedEffect(resolver) {
        snapshotFlow { resolver.enqueuedCount }.collect {
            withFrameNanos { }
            resolver.resolvePending()
        }
    }
    DisposableEffect(resolver) {
        onDispose { resolver.connect(null) }
    }

    bindAndroidView(
        store = bookkeeper.store,
        modifier = modifier,
        style = style,
        rootHeader = rootAccessoryOf(content = rootHeader, text = rootHeaderText),
        rootFooter = rootAccessoryOf(content = rootFooter, text = rootFooterText),
        // DSL 方式のハンドルは Host ではなく引き直しの受け口へ接続し、受け口が最終 ID に直して Host へ
        // 渡す（利用者の手がかりは明示 ID / forEach の key で、最終 ID を知らないため）。
        connectScrollController = { view ->
            resolver.host = view
            resolver.connect(scrollController)
        },
        releaseScrollController = { resolver.connect(null) },
        extraUpdate = {
            val newTree = buildTree()
            val newResolved = DSLDiffCalculator.ResolvedTree(
                sections = newTree.resolvedSections(),
                rootHeader = newTree.rootHeader,
                rootFooter = newTree.rootFooter,
            )
            // 1) Theme 変化は applyTheme 経路で反映する（Diff には含めない）。
            if (bookkeeper.lastTheme != theme) {
                bookkeeper.store.applyTheme(theme)
                bookkeeper.lastTheme = theme
            }
            // 2) 構造同期の差分を適用する。
            val diffs = DSLDiffCalculator.compute(from = bookkeeper.lastTree, to = newResolved)
            for (diff in diffs) {
                applyDiffToStore(bookkeeper.store, diff)
            }
            // 3) 内容更新（同一 id で内容のみ変化した Cell）を ViewHolder の部分更新経路へ流す。
            val contentUpdates = DSLDiffCalculator.contentUpdates(from = bookkeeper.lastTree, to = newResolved)
            if (contentUpdates.isNotEmpty()) {
                bookkeeper.store.replaceCells(contentUpdates.map { it.id to it })
            }
            bookkeeper.lastTree = newResolved
            // 引き直しの受け口に、内部 Store へ流した宣言ツリーを知らせる（世代を進める）。
            resolver.treeDidUpdate(newResolved.sections)
        },
    )
}

/**
 * Store 方式 / DSL 方式の双方で共有する `AndroidView` バインドヘルパ。
 */
@Composable
private fun bindAndroidView(
    store: SettingsRootStore,
    modifier: Modifier,
    style: KsSettingsViewStyle,
    rootHeader: RootAccessory?,
    rootFooter: RootAccessory?,
    connectScrollController: (KsSettingsViewLayout) -> Unit,
    releaseScrollController: (KsSettingsViewLayout) -> Unit,
    extraUpdate: ((KsSettingsViewLayout) -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            KsSettingsViewLayout(ctx).apply {
                this.style = style
                bind(store)
                connectScrollController(this)
            }
        },
        onRelease = { view -> releaseScrollController(view) },
        update = { view ->
            if (view.style != style) {
                view.style = style
            }
            // 文字列は値で比べられるため、同じ文字列の再代入で行を再 bind させない。Composable は中身を
            // 比べられないため、毎回渡して再 bind させる。
            if (!isSameText(view.rootHeader, rootHeader)) {
                view.rootHeader = rootHeader
            }
            if (!isSameText(view.rootFooter, rootFooter)) {
                view.rootFooter = rootFooter
            }
            extraUpdate?.invoke(view)
            connectScrollController(view)
        },
    )
}

/**
 * `SettingsRootDiff` を内部 Store の対応メソッドに変換して呼ぶ。
 *
 * `purify-core-extract-style-to-ui-layer` で `UpdateTheme` ケースは削除されたため、本関数の
 * `when` から除外されている。Theme 変更は `store.applyTheme(_)` 経由で別途反映する。
 */
private fun applyDiffToStore(store: SettingsRootStore, diff: SettingsRootDiff) {
    when (diff) {
        is SettingsRootDiff.Full -> store.replaceAll(diff.root)
        is SettingsRootDiff.InsertSection -> store.insertSection(diff.section, at = diff.index)
        is SettingsRootDiff.RemoveSection -> store.removeSection(diff.sectionId)
        is SettingsRootDiff.MoveSection -> store.moveSection(from = diff.from, to = diff.to)
        is SettingsRootDiff.ReplaceSection -> store.replaceSection(
            sectionId = diff.sectionId,
            new = diff.newSection,
        )
        is SettingsRootDiff.InsertCell -> store.insertCell(
            cell = diff.cell,
            sectionId = diff.sectionId,
            at = diff.index,
        )
        is SettingsRootDiff.RemoveCell -> store.removeCell(diff.cellId)
        is SettingsRootDiff.ReplaceCell -> store.replaceCell(
            cellId = diff.cellId,
            new = diff.newCell,
        )
        is SettingsRootDiff.MoveCell -> store.moveCell(cellId = diff.cellId, to = diff.toIndex)
        is SettingsRootDiff.UpdateAccessory -> store.updateAccessory(
            target = diff.target,
            accessory = diff.accessory,
        )
    }
}

/**
 * DSL 方式の内部 Store / 前回ツリー / 前回 Theme を保持する書記係。
 * Compose の `remember` でライフサイクル管理される。
 */
internal class DSLBookkeeper(
    val store: SettingsRootStore,
    var lastTree: DSLDiffCalculator.ResolvedTree,
    var lastTheme: Theme,
    /** スクロール命令の ID を宣言ツリーの最終 ID へ引き直す受け口。 */
    val scrollResolver: DSLScrollCommandResolver,
)

/**
 * Root Header / Footer の Composable と文字列から、Host へ渡す `RootAccessory` を決める。
 *
 * Composable があればそれを `RootAccessory.View` で包み（文字列より優先する）、無ければ文字列を
 * `RootAccessory.Text` にする。文字列は Host の既定の Root Header / Footer の描画で表示される。
 */
private fun rootAccessoryOf(content: (@Composable () -> Unit)?, text: String?): RootAccessory? = when {
    content != null -> RootAccessory.View(KsAnyView.Compose { content() })
    text != null -> RootAccessory.Text(text)
    else -> null
}

/** どちらも同じ文字列の `RootAccessory.Text` なら true。 */
private fun isSameText(current: RootAccessory?, next: RootAccessory?): Boolean =
    current is RootAccessory.Text && current == next
