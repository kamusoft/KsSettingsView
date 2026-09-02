package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import jp.kamusoft.kssettingsview.core.KsAnyView
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.core.SettingsRootDiff
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
 * @param rootHeader Root Header として描画する Composable（`null` でヘッダ非表示）
 * @param rootFooter Root Footer として描画する Composable（`null` でフッタ非表示）
 */
@Composable
public fun KsSettingsView(
    store: SettingsRootStore,
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    rootHeader: (@Composable () -> Unit)? = null,
    rootFooter: (@Composable () -> Unit)? = null,
) {
    bindAndroidView(
        store = store,
        modifier = modifier,
        style = style,
        rootHeader = rootHeader,
        rootFooter = rootFooter,
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
 * @param theme UI 層 [Theme]（Compose `Color` / `TextStyle` を直接保持）
 * @param rootHeader Root Header
 * @param rootFooter Root Footer
 * @param content DSL レシーバラムダ
 */
@Composable
public fun KsSettingsView(
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    theme: Theme = Theme(),
    rootHeader: (@Composable () -> Unit)? = null,
    rootFooter: (@Composable () -> Unit)? = null,
    content: DSLSettingsRootScope.() -> Unit,
) {
    // DSL を評価し DSLRootTree を構築する純粋関数。
    fun buildTree(): DSLRootTree {
        val scope = DSLSettingsRootScope().apply(content)
        val rootHeaderAcc: RootAccessory? = rootHeader?.let { rh ->
            RootAccessory.View(KsAnyView.Compose { rh() })
        }
        val rootFooterAcc: RootAccessory? = rootFooter?.let { rf ->
            RootAccessory.View(KsAnyView.Compose { rf() })
        }
        return DSLRootTree(
            sectionNodes = scope.build(),
            rootHeader = rootHeaderAcc,
            rootFooter = rootFooterAcc,
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
        )
    }

    bindAndroidView(
        store = bookkeeper.store,
        modifier = modifier,
        style = style,
        rootHeader = rootHeader,
        rootFooter = rootFooter,
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
    rootHeader: (@Composable () -> Unit)?,
    rootFooter: (@Composable () -> Unit)?,
    extraUpdate: ((KsSettingsViewLayout) -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            KsSettingsViewLayout(ctx).apply {
                this.style = style
                bind(store)
            }
        },
        update = { view ->
            if (view.style != style) {
                view.style = style
            }
            view.rootHeader = rootHeader?.let { content -> toRootAccessory(content) }
            view.rootFooter = rootFooter?.let { content -> toRootAccessory(content) }
            extraUpdate?.invoke(view)
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
)

/**
 * Compose 用の `@Composable () -> Unit` を `RootAccessory.View(KsAnyView.Compose { ... })` に変換する。
 */
private fun toRootAccessory(content: @Composable () -> Unit): RootAccessory {
    return RootAccessory.View(KsAnyView.Compose { content() })
}
