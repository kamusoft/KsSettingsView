// KsSettingsView Android Native — モノレポのビルド入口（settings）
//
// 公開ライブラリ本体と interop Bridge の 2 モジュールを構成する。

pluginManagement {
    repositories {
        google()
        mavenCentral()
        // AGP / Kotlin プラグインを `plugins { ... }` 形式で解決するために必要。
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kssettingsview"

// 公開ライブラリ本体。SettingsRoot / Section / Cell 抽象などのドメインモデル (`.core`)、
// RecyclerView ベースの Android View 実装とスタイル (`.ui`)、Compose ラッパと宣言 DSL
// (`.compose`) を単一モジュールに収め、`jp.kamusoft:kssettingsview` 1 artifact として
// 発行する（android/ADR-0016）。層の区別は Kotlin パッケージ名が担う。
include(":kssettingsview")

// Bridge: interop 境界（.NET binding 等）向けの JVM 互換 Bridge。
// 内部所有 Store と Native Host を保持し、公開 API を Store 公開操作へ変換する（maui/ADR-0001）。
// Maven には発行しない（android/ADR-0016）。
include(":kssettingsview-bridge")
