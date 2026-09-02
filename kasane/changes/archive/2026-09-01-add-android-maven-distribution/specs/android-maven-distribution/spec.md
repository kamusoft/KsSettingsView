# android-maven-distribution デルタスペック

## ADDED Requirements

### Requirement: 単一 library module のビルド構成

Android ビルド (`android/`) は、公開ライブラリを単一の Gradle module `:kssettingsview` (ディレクトリ `android/kssettingsview`、namespace `jp.kamusoft.kssettingsview`) として構成する SHALL。旧 `ks-settingsview-core` / `-ui` / `-compose` の全ソースは Kotlin パッケージ (`jp.kamusoft.kssettingsview.core` / `.ui` / `.compose`) を維持したまま同 module に含まれる SHALL。`:kssettingsview-bridge` は別 module として維持され、公開対象に含まれない SHALL。

#### Scenario: 統合 aar の生成

- **GIVEN** 統合後の `android/`
- **WHEN** `./gradlew :kssettingsview:assembleRelease` を実行する
- **THEN** `kssettingsview-release.aar` が生成され、旧 core / ui / compose の全公開クラスが `jp.kamusoft.kssettingsview.core` / `.ui` / `.compose` パッケージのまま含まれる

#### Scenario: bridge module の独立維持

- **GIVEN** 統合後の `android/`
- **WHEN** `./gradlew :kssettingsview-bridge:assembleRelease` を実行する
- **THEN** `kssettingsview-bridge-release.aar` が生成され、`:kssettingsview` への project 依存で解決される

### Requirement: 既存テストの無改変実行

統合 module のユニットテストは、旧 core 由来の JUnit 5 テストと旧 ui / compose 由来の JUnit 4 + Robolectric テストを、テストコード無改変のまま単一のテスト実行で走らせる SHALL。`:kssettingsview-bridge` の既存テストも改名・依存変更後に全件実行できる SHALL。

#### Scenario: 全既存テストの実行

- **GIVEN** 統合・改名後の `android/`
- **WHEN** `./gradlew test` を実行する (統合 module と bridge の両方が対象)
- **THEN** JUnit 5 テストと JUnit 4 + Robolectric テストの両方が実行され、実行件数の合計は旧 4 module (core / ui / compose / bridge) の合計と一致し、すべて成功する (報告には実行件数を併記する)

### Requirement: Maven 発行物の座標と内容

`:kssettingsview` は Maven 座標 `jp.kamusoft:kssettingsview:<version>` (version は catalog の単一宣言元から解決) で発行できる SHALL。発行物は release variant の aar・sources jar・javadoc jar・POM で構成され、POM は name / description / url / MIT license / developers / scm を含む SHALL。発行メタデータの依存は、公開 API に現れる外部型の依存 (compose runtime / compose-ui / kotlinx-coroutines-core / androidx.annotation) を compile (`api`) スコープで含み、テスト専用ライブラリ (JUnit / Robolectric / AndroidX Test / ui-test-manifest) を含まない SHALL。`:kssettingsview-bridge` は発行タスクを持たない SHALL。

#### Scenario: ローカル発行での発行物検証

- **GIVEN** 発行設定を導入した `:kssettingsview`
- **WHEN** `./gradlew :kssettingsview:publishToMavenLocal` を実行する
- **THEN** ローカル Maven リポジトリの `jp/kamusoft/kssettingsview/<version>/` に aar・sources jar・javadoc jar・POM が配置される。sources jar には `.core` / `.ui` / `.compose` 3 パッケージのソースが含まれ、javadoc jar は意図どおり空である。POM に MIT license・scm・developers と依存 (recyclerview / material / compose 等) が記載されている

#### Scenario: 発行メタデータの依存スコープ

- **GIVEN** `publishToMavenLocal` の発行物
- **WHEN** POM と Gradle Module Metadata (`.module`) の依存を検査する
- **THEN** 公開 API に現れる外部型の依存 (compose runtime / compose-ui / kotlinx-coroutines-core / androidx.annotation) が compile (`api`) スコープに載り、テスト専用ライブラリ (JUnit / Robolectric / AndroidX Test / `ui-test-manifest`) がいずれのスコープにも存在しない

#### Scenario: bridge の非公開

- **GIVEN** 発行設定を導入した `android/`
- **WHEN** `./gradlew :kssettingsview-bridge:tasks --group publishing` を実行する
- **THEN** Maven Central へ向く発行タスクが存在しない

### Requirement: Sample のソース参照の維持

Android Sample (`samples/android`) は、公開座標 `jp.kamusoft:kssettingsview` への依存 1 行で本体を参照し、composite build の明示 dependencySubstitution により本体 project へ置換される SHALL (明示置換を採る理由 — 公開版への無音フォールバック防止 — は design.md Decision 4)。

#### Scenario: Sample のビルドとソース参照

- **GIVEN** 統合後の本体と追随後の `samples/android`
- **WHEN** Sample アプリをビルドする
- **THEN** `jp.kamusoft:kssettingsview` が `:kssettingsview` project に置換されてビルドが成功し、本体ソースの変更が Sample の再ビルドに反映される

### Requirement: MAUI binding のビルド入力の追随

MAUI binding (`maui/android/KsSettingsView.Binding.Android`) は、統合後の 2 本の aar (`kssettingsview-release.aar` を同梱、`kssettingsview-bridge-release.aar` を束縛) からビルドされる SHALL。

#### Scenario: binding のビルド

- **GIVEN** 統合後の本体と追随後の binding csproj
- **WHEN** binding プロジェクトをビルドする
- **THEN** gradlew 経由で 2 module の aar が生成・取り込まれ、ビルドが成功する

#### Scenario: IntegrationHost での実行確認

- **GIVEN** 追随後の binding でビルドした Android IntegrationHost
- **WHEN** IntegrationHost を実行し既存の固定シナリオを流す
- **THEN** Bridge 経由で本体クラス (統合 aar 内) がロードされ、固定シナリオが成立する (aar 統合による同梱・resource merge の退行がない)
