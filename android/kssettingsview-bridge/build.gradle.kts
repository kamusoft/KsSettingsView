// kssettingsview-bridge: KsSettingsView interop Bridge (Android)
//
// interop 境界 (.NET binding 等) から設定画面を操作するための JVM 互換 Bridge を提供する。
// 内部に `SettingsRootStore` を所有し、公開 API を Store の公開操作へ変換する (maui/ADR-0001)。
// 公開ライブラリ本体 (`kssettingsview`) の API を interop 都合の型で汚染しないため、
// 独立モジュールとする。Maven には発行しない (android/ADR-0016)。

plugins {
    // Android Library プラグイン（kssettingsview と整合）
    alias(libs.plugins.android.library)
    // Kotlin Android プラグイン
    alias(libs.plugins.kotlin.android)
    // Compose Compiler プラグイン（CustomCell の content を `AndroidView` で埋め込むため）
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "jp.kamusoft.kssettingsview.bridge"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // 輸送された `View` を CustomCell の content として `AndroidView` で埋め込むため、Compose を有効化
        compose = true
        // BuildConfig は使わない
        buildConfig = false
    }

    // Kotlin ソースルートを `src/main/kotlin` / `src/test/kotlin` に保つ
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
            // Robolectric は Android リソース・Resources 系 API を要求するため有効化
            isIncludeAndroidResources = true
        }
    }
}

// JDK 17 を採用（kssettingsview と整合）
kotlin {
    jvmToolchain(17)
}

dependencies {
    // 公開ライブラリ本体（SettingsRoot / Section / SettingsRootStore / KsSettingsView /
    // LabelCell / Theme 等）。Bridge の公開 API は Bridge 自身の DTO と `android.view.View`
    // だけを露出させ、本体の型は輸送の内側に閉じ込める。
    implementation(project(":kssettingsview"))

    // Coroutines（`SettingsRootStore.state` が `StateFlow` のため、現在状態の参照に必要）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Compose。`Theme` の各項目が要求する値型（Color / TextStyle / Dp）の変換と、
    // CustomCell の content として輸送された `View` を埋め込む `AndroidView` に必要。
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-unit")
    // 埋め込みを行幅いっぱいに広げる Modifier（fillMaxWidth）
    implementation("androidx.compose.foundation:foundation-layout")
    // 埋め込みの上でのタップ検出（detectTapGestures）
    implementation("androidx.compose.foundation:foundation")

    // テストフレームワーク: JUnit 4（Robolectric が JUnit 4 ランナー前提）
    testImplementation("junit:junit:4.13.2")
    // Robolectric: Android Framework のシャドウ実装で JVM 上で UI テストを行う
    testImplementation("org.robolectric:robolectric:4.13")
    // AndroidX Test Core（ApplicationProvider 等）
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    // RecyclerView（Host の実描画内容と Adapter 通知をテストから観察するため）
    testImplementation("androidx.recyclerview:recyclerview:1.3.2")
    // Material Components（ホスト Activity に Theme.Material3.* を適用するため）
    testImplementation("com.google.android.material:material:1.12.0")
    // Fragment（ホスト Activity として FragmentActivity を使うため）
    testImplementation("androidx.fragment:fragment-ktx:1.8.4")
}
