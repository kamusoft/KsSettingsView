# Tasks: add-android-maven-distribution

## 1. module 統合

- [x] 1.1 `git mv` で `ks-settingsview-core` → `kssettingsview`、`ks-settingsview-bridge` → `kssettingsview-bridge` に改名し、`ks-settingsview-ui` / `-compose` のソース・テスト・res・AndroidManifest を `kssettingsview` へ統合する (Kotlin パッケージ維持) (→ Requirement: 単一 library module のビルド構成)
- [x] 1.2 `kssettingsview/build.gradle.kts` を単一化する: namespace `jp.kamusoft.kssettingsview`、依存の統合、buildFeatures / compileOptions の統一。依存は公開 ABI 露出で仕分け — 公開 API に現れる外部型の依存 (compose runtime / compose-ui / kotlinx-coroutines-core / androidx.annotation) は `api`、内部利用は `implementation`、テスト専用依存 (`ui-test-manifest` 含む) は release 発行物に混入しない configuration へ (design Decision 6) (→ Requirement: 単一 library module のビルド構成 / Maven 発行物の座標と内容)
- [x] 1.3 旧 ui パッケージ内の自モジュール R 参照 (ViewHolder の `setImageResource` / `KsThemedContext` / テスト) に `jp.kamusoft.kssettingsview.R` の明示 import を足す (→ Requirement: 単一 library module のビルド構成)
- [x] 1.4 `settings.gradle.kts` を 2 project 構成 (`:kssettingsview` / `:kssettingsview-bridge`) に更新し、`rootProject.name` を `kssettingsview` にする。bridge の project 依存を `:kssettingsview` 1 本にする (→ Requirement: 単一 library module のビルド構成 / Scenario: bridge module の独立維持)
- [x] 1.5 ルート `build.gradle.kts` に `group = "jp.kamusoft"` / version の subprojects 一括設定を置き、各 module の宣言を削除する。catalog キーを `ks-settingsview` → `kssettingsview` に改名する (→ Requirement: Maven 発行物の座標と内容)

## 2. テスト基盤

- [x] 2.1 統合 module のテスト実行を JUnit Platform に載せ、junit-vintage-engine を追加して JUnit 4 + Robolectric テストを同居させる。`maxHeapSize = "2g"` / `isIncludeAndroidResources = true` を module 全体に適用する (→ Requirement: 既存テストの無改変実行)
- [x] 2.2 旧 4 module (core / ui / compose / bridge) のテスト件数を統合前に記録し、`./gradlew test` (統合 module + bridge) の実行件数が合計と一致してすべて green であることを件数併記で確認する (handbook cross/test-execution.md) (→ Scenario: 全既存テストの実行)

## 3. 発行設定

- [x] 3.1 `com.vanniktech.maven.publish` 0.37.0 を `:kssettingsview` にのみ導入する: `publishToMavenCentral()` + `signAllPublications()`、`AndroidSingleVariantLibrary("release")` + sources jar + 空 javadoc jar (→ Requirement: Maven 発行物の座標と内容)
- [x] 3.2 POM (name / description / url / MIT license / developers / scm を public リポジトリ URL で) を記述する (→ Requirement: Maven 発行物の座標と内容)
- [x] 3.3 `publishToMavenLocal` で発行物を検証し、証跡を evidence/ に残す: aar / sources jar (3 パッケージ含有) / javadoc jar (空) / POM の内容、POM と `.module` の依存スコープ (公開 API 露出依存が `api`、テスト専用ライブラリの不在) (→ Scenario: ローカル発行での発行物検証 / 発行メタデータの依存スコープ)
- [x] 3.4 `:kssettingsview-bridge` に発行タスクが生えていないことを確認する (→ Scenario: bridge の非公開)

## 4. 消費者の追随

- [x] 4.1 `samples/android`: dependencySubstitution を `jp.kamusoft:kssettingsview` → `:kssettingsview` の 1 本に更新し、アプリ依存 3 行を 1 行へ、catalog キー参照を追随する (→ Requirement: Sample のソース参照の維持)
- [x] 4.2 Sample をビルドし、本体ソースの変更が反映されること (置換の実効) を確認する (→ Scenario: Sample のビルドとソース参照)
- [x] 4.3 `maui/android/KsSettingsView.Binding.Android.csproj`: `AndroidLibrary` を 2 本 (`kssettingsview` 同梱 / `kssettingsview-bridge` 束縛) に更新し、ソース glob と gradlew task 名を追随する (→ Requirement: MAUI binding のビルド入力の追随)
- [x] 4.4 binding プロジェクトをビルドして成功を確認する (→ Scenario: binding のビルド)
- [x] 4.5 Android IntegrationHost をビルド・実行し、既存の固定シナリオの成立を確認する (handbook maui/integration-host-verification.md) (→ Scenario: IntegrationHost での実行確認)

## 5. 規範の追随

- [x] 5.1 `kasane/handbook/cross/public-identifiers.md` を改訂する: artifactId 規則 (`ks-settingsview-*` → 単一 `kssettingsview`)・namespace 表 (3 行 → 単一 namespace)・「Maven 座標の現在地」節 (GAV 1 本化・maven-publish 導入済みの記述・catalog キー名)
- [x] 5.2 `kasane/decisions/cross/0018` (accepted) に日付付き追記で Android 座標の統合 (`jp.kamusoft:kssettingsview`、android/ADR-0016) を注記する — 表のセルは書き換えない (履歴を保つ)
