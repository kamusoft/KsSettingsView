// ks-settingsview-ui: KsSettingsView Android UI 基盤
//
// SettingsRoot を描画する Android UI 基盤を提供するモジュール。
// `KsSettingsView`（FrameLayout）、`KsSettingsListAdapter`、`KsCellRegistry`、
// `CellViewHolder` 抽象、`ItemDecoration`（Classic / Modern）、スタイル型（`Theme` / `CellStyle`）を含む。

plugins {
    // Android Library プラグイン（ks-settingsview-core と整合）
    alias(libs.plugins.android.library)
    // Kotlin Android プラグイン
    alias(libs.plugins.kotlin.android)
    // Compose Compiler プラグイン（Section Header/Footer の `KsAnyView.Compose` 描画で
    // `ComposeView.setContent` を使うため）
    alias(libs.plugins.kotlin.compose)
}

group = "jp.kamusoft.kssettingsview"
version = libs.versions.ks.settingsview.get()

android {
    namespace = "jp.kamusoft.kssettingsview.ui"
    compileSdk = 35

    defaultConfig {
        // development.md の規定: minSdk 29
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // ComposeView を ViewHolder で利用するため、Compose を有効化
        compose = true
        // BuildConfig は不要
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

            all {
                // Robolectric は生成した Resources.Theme を実行環境の生存期間だけ保持する。
                // ライブラリ UI は同梱テーマをかぶせた Context から生成されるため (android/ADR-0020)、
                // テストが作る Context ごとに Theme が 1 つ増える。Gradle 既定の 512m では
                // クラスをまたぐ累積で足りなくなるため、テスト JVM のヒープを明示する。
                it.maxHeapSize = "2g"
            }
        }
    }
}

// JDK 17 を採用（development.md 準拠）
kotlin {
    jvmToolchain(17)
}

dependencies {
    // Core モジュール（SettingsRoot / Section / Cell / KsAnyView / RootAccessory 等のドメインモデル）
    implementation(project(":ks-settingsview-core"))

    // RecyclerView（ListAdapter / DiffUtil / ConcatAdapter）
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // AppCompat（AppCompatImageView / AppCompatRadioButton / AppCompatCheckBox を使うため）
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ConstraintLayout（CellBaseViews のルート ViewGroup として使用）
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Components（MaterialSwitch を使うため、および同梱テーマ
    // res/values/themes.xml の親 Theme.Material3.DayNight.NoActionBar のため）。
    // 利用者アプリのテーマに要件はない。ライブラリ UI は同梱テーマをかぶせた Context から
    // 生成され、Material ウィジェットが要求する属性はその同梱テーマが供給する（android/ADR-0020）。
    implementation("com.google.android.material:material:1.12.0")

    // Activity（カレンダー選択面の器に使う `ComponentDialog`。ViewTree の lifecycle /
    //           savedStateRegistry の所有者を自前で供給するため、Fragment を必要としない）
    implementation("androidx.activity:activity:1.9.3")

    // Coroutines（SettingsRootStore の StateFlow / SharedFlow 用）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lifecycle Runtime（findViewTreeLifecycleOwner / lifecycleScope での collect 用）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Compose（ComposeCellViewHolder / Section Header/Footer 用の ComposeView と
    //          KsAnyView.Compose 描画で `ComposeView.setContent` を使うため）
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    // KsAnyView.Compose / Section Accessory 描画で Text / Image / painterResource を使うため
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-viewbinding")

    // テストフレームワーク: JUnit 4（Robolectric が JUnit 4 ランナー前提）
    testImplementation("junit:junit:4.13.2")
    // Robolectric: Android Framework のシャドウ実装で JVM 上で UI テストを行う
    testImplementation("org.robolectric:robolectric:4.13")
    // AndroidX Test Core（ApplicationProvider 等）
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    // Coroutines テスト（runTest / TestScope）
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Pooling Container（recyclerview の推移的依存）。プール滞在＝「pooling container の内側で
    // window から外れた状態」をテストから直接作るために、コンパイル時参照を明示する。
    testImplementation("androidx.customview:customview-poolingcontainer:1.0.0")
}
