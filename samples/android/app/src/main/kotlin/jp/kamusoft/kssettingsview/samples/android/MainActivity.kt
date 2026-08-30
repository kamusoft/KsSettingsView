package jp.kamusoft.kssettingsview.samples.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * バックで Menu に戻れる。
 *
 * 基底クラスは Compose テンプレート標準の [ComponentActivity]。本体 UI 層は選択面を
 * ライブラリ自身のシート／ダイアログで提示し、Fragment にも Material3 の XML テーマにも
 * 依存しないため、Compose 専業アプリの素の構成のままで全 Cell が動作する (android/ADR-0020)。
 */
class MainActivity : ComponentActivity() {

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
            SampleApp()
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
    NavHost(
        navController = navController,
        startDestination = MENU_ROUTE,
    ) {
        composable(MENU_ROUTE) {
            MenuScreen(onSelect = { screen -> navController.navigate(screen.route) })
        }
        SampleScreen.demos.forEach { screen ->
            composable(screen.route) {
                DemoScaffold(
                    title = screen.title,
                    onBack = { navController.popBackStack() },
                ) {
                    screen.Content()
                }
            }
        }
    }
}

/**
 * デモ画面を `TopAppBar` で包む Scaffold。`TopAppBar` の戻るアイコンで Menu に戻れる
 * ようにすることで、iOS の `NavigationStack` 自動戻るボタンと同じ操作性を提供する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoScaffold(
    title: String,
    onBack: () -> Unit,
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
