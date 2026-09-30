package jp.kamusoft.kssettingsview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.ui.KsScrollController

/**
 * コンポジションの生存期間だけ保持される [KsScrollController] を返します。
 *
 * 再コンポジションをまたいで同じハンドルを返します。`KsSettingsView(...)` の `scrollController`
 * 引数へ渡して使います。
 *
 * 画面が所有する場合はこの関数を、ViewModel などが所有する場合は [KsScrollController] を直接
 * 生成して使います。
 */
@Composable
public fun rememberScrollController(): KsScrollController = remember { KsScrollController() }
