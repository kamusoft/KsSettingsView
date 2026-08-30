// ks-settingsview-core: KsSettingsView Android Core
//
// SettingsRoot / Section / Cell 抽象 / KsAnyView / RootAccessory / SectionAccessory 等の
// ドメインモデルを提供するモジュール。スタイル（Theme / CellStyle）は UI 層が持つ。
//
// 純粋 JVM ライブラリ（`kotlin("jvm")`）ではなく Android Library
// (`com.android.library`) として構築する。`KsAnyView` が以下を保持し、
// Compose Runtime と Android Framework に依存するため:
//   - `class Compose(val content: @Composable () -> Unit) : KsAnyView` （Compose Runtime 依存）
//   - `class AndroidView(val factory: (Context) -> View) : KsAnyView` （Android Framework 依存）

plugins {
    // Android Library プラグイン
    alias(libs.plugins.android.library)
    // Kotlin Android プラグイン（Compose を使うため JVM 用ではなく Android 用）
    alias(libs.plugins.kotlin.android)
    // Compose Compiler プラグイン（Kotlin 2.0+ で必須）
    alias(libs.plugins.kotlin.compose)
}

group = "jp.kamusoft.kssettingsview"
version = libs.versions.ks.settingsview.get()

android {
    namespace = "jp.kamusoft.kssettingsview.core"
    compileSdk = 35

    defaultConfig {
        // 本ライブラリの最低サポート Android バージョン
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // KsAnyView.Compose サブタイプは @Composable ラムダを保持するため、Compose を有効化
        compose = true
        // BuildConfig は不要
        buildConfig = false
    }

    // Kotlin ソースルートは Android Library 既定の `src/main/java` ではなく
    // `src/main/kotlin` / `src/test/kotlin` とする。
    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
            // 本モジュールはドメインモデルのみを提供するため、AndroidManifest.xml は最小構成とする。
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        named("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

// JDK 17 を採用（リポジトリ全体の Android ビルド共通）
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Compose Runtime（@Composable アノテーション解決のため）。BOM で版整合。
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")

    // androidx.annotation（UI 層の `KsImage.Resource(@DrawableRes resId: Int)` 等が使う
    // @DrawableRes アノテーション解決のため）。依存モジュールへ推移的に届くよう api で公開する。
    api("androidx.annotation:annotation:1.9.1")

    // テストフレームワーク: JUnit 5 (Jupiter)
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
