# android-build-toolchain (delta)

## ADDED Requirements

### Requirement: JDK 25 の Gradle JVM でビルドできる

`android/` および `samples/android/` の Gradle プロジェクトは、Gradle JVM に JDK 25 を選択した環境で sync・ビルドが成功しなければならない。

#### Scenario: JDK 25 での CLI ビルド

- GIVEN JAVA_HOME (または Gradle JVM) が JDK 25 を指す環境
- WHEN `android/` で `./gradlew testDebugUnitTest`、`samples/android/` で `./gradlew :app:assembleDebug` を実行する
- THEN いずれも成功する

#### Scenario: JDK 21 での後方互換

- GIVEN Gradle JVM が JDK 21 の環境
- WHEN 同じビルドを実行する
- THEN いずれも成功する (既存開発環境を壊さない)

### Requirement: ビルド関連バージョンの宣言がカタログに一元化される

AGP / Kotlin / Compose Compiler plugin / Compose BOM / project version は `android/gradle/libs.versions.toml` にのみ宣言され、`android/` の 4 module と `samples/android/app` はそれを参照しなければならない。

#### Scenario: バージョン直書きの不在

- GIVEN 更新後のリポジトリ
- WHEN `android/ks-settingsview-*/build.gradle.kts` と `samples/android/app/build.gradle.kts` を検索する
- THEN plugins ブロックの `version "..."`、`compose-bom:` のリテラル版、`version = "..."` の直書きが存在しない

#### Scenario: samples からのカタログ共有

- GIVEN `samples/android/settings.gradle.kts` が `android/gradle/libs.versions.toml` を `versionCatalogs` で取り込んでいる
- WHEN `samples/android/` で `./gradlew :app:assembleDebug` を実行する
- THEN 本体と同じ AGP / Kotlin / Compose BOM 版で成功する

### Requirement: ツールチェーン更新後も既存の品質ゲートを全て通過する

ツールチェーン更新はライブラリの公開 API・実行時挙動・成果物のターゲット (Java 17 / compileSdk) を変えてはならない。

#### Scenario: 既存ユニットテストの全数通過

- GIVEN 更新後のツールチェーン
- WHEN `android/` で `./gradlew testDebugUnitTest` を実行する
- THEN 既存の全テスト (core / ui / compose / bridge) が失敗 0 で通過する

#### Scenario: サンプルアプリの実機動作

- GIVEN 更新後のツールチェーンでビルドした samples/android
- WHEN 実機にインストールして起動する
- THEN アプリが起動し、デモ画面の表示・入力操作が更新前と同等に動作する

#### Scenario: MAUI binding からの native ビルド

- GIVEN 更新後のツールチェーン
- WHEN `maui/android/KsSettingsView.Binding.Android` を `dotnet build` する (内部で `android/gradlew assembleRelease` が Exec 実行される)
- THEN 4 module の release aar が生成され、binding のビルドが成功する
