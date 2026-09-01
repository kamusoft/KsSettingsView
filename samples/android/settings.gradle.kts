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
//   自動置換に任せると、置換が発火しなかったときに Maven Central の公開版へ静かに
//   フォールバックし「本体の修正が Sample に映らない」壊れ方をする。明示置換なら
//   置換先を失った時点で必ずビルドエラーになる。
includeBuild("../../android") {
    dependencySubstitution {
        substitute(module("jp.kamusoft:kssettingsview"))
            .using(project(":kssettingsview"))
    }
}

rootProject.name = "kssettingsview-sample-android"

// :app: Sample アプリ本体（Compose Activity + 各デモ画面で本体 LabelCell 等を使用）
include(":app")
