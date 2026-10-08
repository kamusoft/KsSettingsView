package jp.kamusoft.kssettingsview.samples.android

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.kamusoft.kssettingsview.ui.KsCellRegistry

/**
 * KsSettingsView Sample アプリのエントリポイント Activity。
 *
 * ルートメニュー → 各デモ画面への遷移は、iOS Sample の `NavigationStack` +
 * `NavigationLink` 構造に合わせて、Android では Navigation Compose の `NavHost` +
 * `composable` ルートで構成する。デモ画面からは `TopAppBar` の戻るアイコン or システム
 * バックで Menu に戻れる。デモ画面の `TopAppBar` には、次のデモ画面へ進む操作とルートメニューを
 * 重ねて開く操作も置く（[DemoScaffold]）。
 *
 * 基底クラスは Compose テンプレート標準の [ComponentActivity]。本体 UI 層は選択面を
 * ライブラリ自身のシート／ダイアログで提示し、Fragment にも Material3 の XML テーマにも
 * 依存しないため、Compose 専業アプリの素の構成のままで全 Cell が動作する (android/ADR-0020)。
 *
 * ルートメニューで選ぶ外観（[SampleAppearance]）は、この Activity の Configuration 上書きで
 * 反映する。選択の変更時は [recreate] で作り直し、上書きを [attachBaseContext] からやり直す。
 */
class MainActivity : ComponentActivity() {

    /**
     * 保存済みの外観に応じて、この Activity の Configuration へ夜間モードを上書きする。
     *
     * 上書きは Resources を最初に読む前に済ませる必要があるため、`super` の直後で行う。
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        SampleAppearanceStore.nightModeOverride(newBase)?.let { applyOverrideConfiguration(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KsCellRegistry の strictMode を明示的に切り替える。
        // - Debug ビルド: strictMode = true
        // - Release ビルド: strictMode = false
        //
        // 基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell /
        // CheckboxCell / RadioCell / SimpleCheckCell）は KsSettingsView の
        // 初回コンストラクト時に registerBasicCells(context) が自動呼び出しされて
        // 登録されるため、Sample 側で明示的な register は不要。
        KsCellRegistry.strictMode = BuildConfig.DEBUG

        setContent {
            SampleAppTheme {
                SampleApp()
            }
        }
    }
}

/** ルートメニューの Navigation Compose ルート。 */
private const val MENU_ROUTE: String = "menu"

/**
 * Sample アプリのルート Composable。
 *
 * `NavHost` でルートメニューと各デモ画面を切り替える。
 * 遷移先の定義・タイトルは [SampleScreen] を単一の定義元として参照する。
 */
@Composable
private fun SampleApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    NavHost(
        navController = navController,
        startDestination = MENU_ROUTE,
    ) {
        composable(MENU_ROUTE) {
            // 選択が変わるのは recreate() を挟むときだけなので、composition ごとに 1 回読めばよい。
            val currentAppearance = remember { SampleAppearanceStore.load(context) }
            MenuScreen(
                appearance = currentAppearance,
                onSelectAppearance = { appearance ->
                    // 同じ外観を選び直したときは実効外観が変わらないため何もしない。
                    // 保存と再生成を通すと画面が作り直され、入力途中の状態まで失われる。
                    if (appearance != currentAppearance) {
                        SampleAppearanceStore.save(context, appearance)
                        // 上書きは attachBaseContext でしか差し替えられないため、Activity を作り直す。
                        activity?.recreate()
                    }
                },
                onSelect = { screen -> navController.navigate(screen.route) },
            )
        }
        SampleScreen.demos.forEachIndexed { index, screen ->
            // 並びの次のデモ画面（最後の画面からは最初の画面）。
            val next = SampleScreen.demos[(index + 1) % SampleScreen.demos.size]
            composable(screen.route) {
                DemoScaffold(
                    title = screen.title,
                    onBack = { navController.popBackStack() },
                    onOpenNextDemo = { navController.navigate(next.route) },
                    onOpenMenu = { navController.navigate(MENU_ROUTE) },
                ) {
                    screen.Content()
                }
            }
        }
    }
}

/**
 * この Compose ツリーを載せている [ComponentActivity]。ラッパ Context 経由で呼ばれても辿れるよう、
 * [ContextWrapper] を降りて探す。
 */
private fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext ?: return null
    }
    return null
}

/**
 * Sample 自身の Compose UI（ルートメニュー・デモ画面の枠）の配色。
 *
 * 実効外観は Activity の Configuration が持つため、[isSystemInDarkTheme] は外観の選択と
 * 端末の夜間モードの両方を反映する。配色は Material 3 の標準 light / dark に任せる。
 */
@Composable
private fun SampleAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

/**
 * デモ画面を `TopAppBar` で包む Scaffold。`TopAppBar` の戻るアイコンで Menu に戻れる
 * ようにすることで、iOS の `NavigationStack` 自動戻るボタンと同じ操作性を提供する。
 *
 * `TopAppBar` の右側には、今の画面を残したまま別の画面を上に重ねる操作を 2 つ置く。Navigation Compose は
 * 下になった画面のコンポジションを破棄し、戻ったときに保存した状態から組み直す。デモ画面の
 * `KsSettingsView` が、進んで戻った後も同じスクロール位置を表示することをここから確かめられる。
 * 行き先が `KsSettingsView` の画面の場合（次のデモ画面）と、そうでない画面の場合（ルートメニュー）の
 * 両方を確かめられるよう、操作を分けてある。
 *
 * @param title 画面タイトル
 * @param onBack 戻るアイコンをタップしたときの通知
 * @param onOpenNextDemo 「次のデモへ進む」をタップしたときの通知
 * @param onOpenMenu 「メニューを開く」をタップしたときの通知
 * @param content デモ画面の内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScaffold(
    title: String,
    onBack: () -> Unit,
    onOpenNextDemo: () -> Unit,
    onOpenMenu: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "メニューを開く",
                        )
                    }
                    IconButton(onClick = onOpenNextDemo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "次のデモへ進む",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
