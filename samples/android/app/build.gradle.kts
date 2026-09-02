// KsSettingsView Android Sample アプリ — :app モジュール
//
// 役割:
//   - ComponentActivity ベースのエントリポイント (MainActivity)
//   - Navigation Compose による「Store 方式デモ」「DSL 方式デモ」「基本 Cell 7 種デモ」の遷移
//   - 本体 LabelCell を含む基本 Cell 7 種を Sample 側で目視確認

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "jp.kamusoft.kssettingsview.samples.android"
    compileSdk = 35

    defaultConfig {
        // monorepo-foundation / KsSettingsView 本体に合わせて minSdk = 29
        applicationId = "jp.kamusoft.kssettingsview.samples.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.DEBUG を MainActivity から参照して KsCellRegistry.strictMode を
        // 「Debug ビルドのみ厳格、Release ビルドではプレースホルダにフォールバック」
        // と明示的に切り替える運用例を示すため有効化する（指摘 #3 への対応）。
        buildConfig = true
    }

    // Kotlin ソースルートを `src/main/kotlin` に保つ（本体 android/ モジュールと整合）
    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            // Sample 用途のため signingConfig は割り当てない（debug キーで署名）
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // KsSettingsView 本体（composite build 経由でソース参照）
    // settings.gradle.kts の `includeBuild("../../android")` と明示 dependencySubstitution により、
    // 公開座標 1 点への依存が本体プロジェクトへ置換される。利用者が書く依存と同じ 1 行にする。
    // バージョンは本体と共有するバージョンカタログから取り、本体側の `version` と必ず一致させる。
    implementation("jp.kamusoft:kssettingsview:${libs.versions.kssettingsview.get()}")

    // ComponentActivity（setContent { } のため）
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose（BOM で版整合）
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    // Material 3（Button / Text / OutlinedButton 等の Sample アプリ UI 部品で利用）
    implementation("androidx.compose.material3:material3")

    // Material Icons Extended（TopAppBar の戻るアイコン ArrowBack 用）
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle Runtime Compose（collectAsStateWithLifecycle 用）。
    // バージョンは本体の lifecycle-runtime-ktx と揃える。
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Navigation Compose（iOS の NavigationStack 相当の戻れる導線を実現するため）
    implementation("androidx.navigation:navigation-compose:2.8.4")
}
