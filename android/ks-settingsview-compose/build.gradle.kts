// ks-settingsview-compose: KsSettingsView Compose ラッパ + DSL
//
// Compose から設定画面を組むための公開 Composable `KsSettingsView` を提供するモジュール。
// 利用者所有の `SettingsRootStore` を渡す Store 方式と、`KsSettingsView { Section { ... } }` の
// 宣言 DSL 方式の 2 overload を持つ。Store 初期値の構築には純粋関数の
// `settingsRoot { section("general") { ... } }` builder を使う（`section` は ID が必須）。
// 描画は UI 層の Android View 実装（`ks-settingsview-ui`）へ `AndroidView` 経由で委譲し、
// Compose 専用の描画基盤は持たない。

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

group = "jp.kamusoft.kssettingsview"
version = libs.versions.ks.settingsview.get()

android {
    namespace = "jp.kamusoft.kssettingsview.compose"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        named("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core モジュール（SettingsRoot / Section / Cell / KsAnyView / RootAccessory）
    implementation(project(":ks-settingsview-core"))
    // UI モジュール（KsSettingsView FrameLayout、KsSettingsViewStyle、Theme）
    implementation(project(":ks-settingsview-ui"))

    // Compose
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-viewbinding")
    // AndroidView Composable のため
    implementation("androidx.compose.foundation:foundation")

    // テスト
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    // Compose UI Test（Robolectric バックエンドで動かす：createComposeRule）
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Material 3（テストの `headerView` slot で androidx.compose.material3.Text を使うため）
    testImplementation("androidx.compose.material3:material3")
    // ui-test-manifest はテスト時に必要な Activity（ComponentActivity）の AndroidManifest を提供する。
    // unit test は debug / release 両 variant で実行されるため、両方に含める。
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    releaseImplementation("androidx.compose.ui:ui-test-manifest")
    // テストで内部 RecyclerView の adapter.itemCount を検証するために必要
    // （ks-settingsview-ui 側で `implementation` 指定のため、テストの compile classpath に
    //  recyclerview を明示的に持ち込む）
    testImplementation("androidx.recyclerview:recyclerview:1.3.2")
}
