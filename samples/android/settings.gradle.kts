// KsSettingsView Android Sample アプリ — Gradle settings
//
// 本 Sample は KsSettingsView 本体（android/）を Gradle composite build により
// ソース参照する。これにより、本体ソースに直接ブレークポイントを置いて
// ステップインでき、本体修正 → Sample 動作確認 のループが短くなる。

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // pluginManagement.includeBuild は plugin（Settings 用）の解決のために本体 build を
    // 取り込む。実体の dependency 置換は下方トップレベルの includeBuild で改めて行う。
    includeBuild("../../android")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // AGP / Kotlin / Compose BOM / 本体ライブラリの版は本体 build のバージョンカタログを
    // そのまま共有し、Sample 側で二重に宣言しない。
    versionCatalogs {
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

// 本体ライブラリ（android/）の build を本 Sample のビルドへ取り込む（依存置換あり）。
//
// 明示的な dependencySubstitution を付与する理由:
//   AGP (Android Library) はデフォルトでは Maven publication を生成しないため、
//   `implementation("jp.kamusoft.kssettingsview:ks-settingsview-core:...")` 形式での
//   composite build 自動置換が発火しない。利用側 (本 Sample) の settings.gradle.kts で
//   GAV → 含まれるビルドの Project への置換を明示する必要がある。
includeBuild("../../android") {
    dependencySubstitution {
        substitute(module("jp.kamusoft.kssettingsview:ks-settingsview-core"))
            .using(project(":ks-settingsview-core"))
        substitute(module("jp.kamusoft.kssettingsview:ks-settingsview-ui"))
            .using(project(":ks-settingsview-ui"))
        substitute(module("jp.kamusoft.kssettingsview:ks-settingsview-compose"))
            .using(project(":ks-settingsview-compose"))
    }
}

rootProject.name = "ks-settingsview-sample-android"

// :app: Sample アプリ本体（Compose Activity + 各デモ画面で本体 LabelCell 等を使用）
include(":app")
