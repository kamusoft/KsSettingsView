// KsSettingsView Android 消費者検証 — :app モジュール
//
// 利用者向けドキュメントの Android 最小例 (src/main/kotlin/SettingsScreen.kt) を
// そのままコンパイル対象に含める application。配布物への依存は公開座標 1 行だけで、
// 本体ソースへの参照は持たない。
//
// 解決する version は Gradle プロパティ `ksSettingsViewVersion` で受け取り、
// 未指定なら本体の開発用既定値 (バージョンカタログの kssettingsview) を使う。

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val ksSettingsViewVersion: String =
    (providers.gradleProperty("ksSettingsViewVersion").orNull
        ?: libs.versions.kssettingsview.get())

android {
    namespace = "jp.kamusoft.kssettingsview.verification.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "jp.kamusoft.kssettingsview.verification.android"
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
    }

    // Kotlin ソースルートを `src/main/kotlin` に保つ（本体・Sample と整合）
    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    buildTypes {
        named("release") {
            // 縮小・難読化は行わない。R8 が走らないため、ライブラリの consumer ProGuard
            // ルールの不足はこの検証では検出できない (見るのはビルドの成立まで)。
            isMinifyEnabled = false
            // signingConfig を割り当てないため release の出力は未署名 APK になる
            // (app-release-unsigned.apk)。検証は署名情報を要求せずに済むことを見る。
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // 利用者が書くのと同じ 1 行。参照先は settings.gradle.kts の exclusiveContent が決める。
    implementation("jp.kamusoft:kssettingsview:$ksSettingsViewVersion")

    // 最小例が使う Compose の API 群（版は本体と共有する BOM で整合させる）
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")

    // Compose の推移依存として入る androidx.activity の版を固定する（Sample と同じ 1.9.3）。
    // 1.12 以降は推移依存の androidx.navigationevent が compileSdk 36 を要求する。
    implementation("androidx.activity:activity-compose:1.9.3")
}
