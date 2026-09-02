// KsSettingsView Android Native — ルートプロジェクトのビルドファイル
//
// プラグインは各モジュールの build.gradle.kts が `gradle/libs.versions.toml` の
// alias 経由で宣言するため、ルートでは何も適用しない（モノレポ土台）。
//
// Maven 座標のうち group と version だけは全モジュール共通の事項のため、ここで一括設定する。
// version の単一宣言元はバージョンカタログであり、samples/android も同じカタログを読む。

// カタログ由来の値は subprojects ブロックの外で解決する。ブロック内の `libs` は
// 対象サブプロジェクトの拡張として解決されるため参照できない。
// リリース時は CI が `-Pversion=` で version を注入する（cross/ADR-0020）。注入があればそれを優先し、
// 無ければカタログの開発用既定値を使う。
val ksSettingsViewVersion = providers.gradleProperty("version").orNull ?: libs.versions.kssettingsview.get()

subprojects {
    // Maven Central の groupId（cross/ADR-0002）。
    group = "jp.kamusoft"
    version = ksSettingsViewVersion
}
