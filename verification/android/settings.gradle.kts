// KsSettingsView Android 消費者検証 — Gradle settings
//
// 利用者と同じ経路 (公開座標 1 行の依存) で配布物を解決する消費者アプリ。本体ソースは
// 参照しない (composite build も dependencySubstitution も持たない)。
//
// `jp.kamusoft` は mode に応じて mavenLocal (dry-run) か mavenCentral (smoke) の
// どちらか一方へ exclusiveContent で排他的に割り当てる。repository-level の content filter は
// 「この repository はこの group を含みうる」の宣言にすぎず、filter を持たない他の repository も
// 同じ group を検索するため、参照先に無いときに公開済みの版へ静かにフォールバックする。
// exclusiveContent なら参照先に無い version は必ず解決失敗になる。
//
// mode は Gradle プロパティ `ksSettingsViewMode` で受け取る (既定 dry-run)。
// dry-run の参照先は既定で mavenLocal だが、外部で準備した配布物 (CI の artifact 等) を
// 使うときは `ksSettingsViewReference` にそのローカル Maven リポジトリのパスを渡す。

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val ksSettingsViewMode: String =
    (settings.providers.gradleProperty("ksSettingsViewMode").orNull ?: "dry-run")

require(ksSettingsViewMode == "dry-run" || ksSettingsViewMode == "smoke") {
    "ksSettingsViewMode は dry-run か smoke のいずれかです: $ksSettingsViewMode"
}

val ksSettingsViewReference: String? =
    settings.providers.gradleProperty("ksSettingsViewReference").orNull

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                when {
                    ksSettingsViewMode == "smoke" -> mavenCentral()
                    ksSettingsViewReference != null ->
                        maven { url = uri(ksSettingsViewReference) }
                    else -> mavenLocal()
                }
            }
            filter { includeGroup("jp.kamusoft") }
        }
        google()
        mavenCentral()
    }
    // AGP / Kotlin / Compose BOM の版は本体 build のバージョンカタログをそのまま共有し、
    // 消費者側で二重に宣言しない (Sample と同じ)。
    versionCatalogs {
        create("libs") {
            from(files("../../android/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kssettingsview-verification-android"

include(":app")
