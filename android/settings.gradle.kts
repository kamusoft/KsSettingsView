// KsSettingsView Android Native — モノレポのビルド入口（settings）
//
// Core / UI / Compose / Bridge の 4 モジュールを構成する。

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

rootProject.name = "ks-settingsview"

// Core: SettingsRoot / Section / Cell 抽象 / KsAnyView / RootAccessory のドメインモデル層
//
// UI 非依存の `kotlin("jvm")` ではなく Android Library (`com.android.library`) として構成する。
// `KsAnyView` が `(Context) -> View` ファクトリと `@Composable () -> Unit` を保持し、
// Android API（`android.content.Context` / `android.view.View`）と Compose Runtime に
// 依存するため。テストは Android Library の Unit Test
// （`./gradlew :ks-settingsview-core:test`）として JVM 上で実行される。
include(":ks-settingsview-core")

// UI: KsSettingsView (FrameLayout) / RecyclerView ベースの Adapter / Cell レジストリ /
// ItemDecoration（Classic / Modern）/ ComposeView 基盤クラス / Theme / CellStyle。
// スタイルは Core ではなく UI 層に置く（core/ADR-0009）。
include(":ks-settingsview-ui")

// Compose: KsSettingsView の Compose ラッパ（@Composable）+ DSL（settingsRoot { ... }）。
include(":ks-settingsview-compose")

// Bridge: interop 境界（.NET binding 等）向けの JVM 互換 Bridge。
// 内部所有 Store と Native Host を保持し、公開 API を Store 公開操作へ変換する（maui/ADR-0001）。
include(":ks-settingsview-bridge")
