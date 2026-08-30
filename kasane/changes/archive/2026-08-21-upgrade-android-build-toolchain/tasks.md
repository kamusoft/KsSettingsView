# Tasks: upgrade-android-build-toolchain

## 1. バージョン確定 (実装着手時)

- [x] 1.1 Gradle 9.x / AGP / Kotlin / Compose Compiler の互換組み合わせを公式互換表で確定する (→ Requirement: JDK 25 の Gradle JVM でビルドできる)
- [x] 1.2 確定した組み合わせを proposal.md の What Changes に追記する

## 2. バージョンカタログ導入

- [x] 2.1 `android/gradle/libs.versions.toml` を新設し、AGP / Kotlin / Compose Compiler plugin / Compose BOM / project version (`0.1.0-SNAPSHOT`) を宣言する (→ Requirement: ビルド関連バージョンの宣言がカタログに一元化される)
- [x] 2.2 android 4 モジュール (core / ui / compose / bridge) の plugins ブロック・Compose BOM・`version =` を catalog 参照へ置き換える (→ 同上)
- [x] 2.3 `samples/android/settings.gradle.kts` で同じ toml を共有し、`samples/android/app/build.gradle.kts` の直書きを catalog 参照へ置き換える (→ 同上)

## 3. ツールチェーン更新

- [x] 3.1 `android/gradle/wrapper/gradle-wrapper.properties` を更新する (→ Requirement: JDK 25 の Gradle JVM でビルドできる)
- [x] 3.2 `samples/android/gradle/wrapper/gradle-wrapper.properties` を更新する (→ 同上)
- [x] 3.3 catalog の AGP / Kotlin / Compose Compiler 版を 1.1 で確定した値へ更新する (→ 同上)
- [x] 3.4 `samples/android/gradle.properties` に `org.gradle.tooling.parallel=true` を追加する (→ 同上)
- [x] 3.5 更新が強制する非推奨 API / DSL の機械的修正があれば最小限で対応する (→ Requirement: ツールチェーン更新後も既存の品質ゲートを全て通過する)

## 4. 検証

- [x] 4.1 JDK 25 で `./gradlew testDebugUnitTest` (android) が全数 pass することを確認する (→ Scenario: JDK 25 での CLI ビルド / 既存ユニットテストの全数通過)
- [x] 4.2 JDK 21 でも同ビルドが成功することを確認する (→ Scenario: JDK 21 での後方互換)
- [x] 4.3 samples/android を実機へインストールしデモ画面の表示・入力を確認する (→ Scenario: サンプルアプリの実機動作)
- [x] 4.4 Android Studio (JDK 25) で sync が成功することを確認する (→ Requirement: JDK 25 の Gradle JVM でビルドできる)
- [x] 4.5 `android/` 配下と `samples/android/app` に AGP / Kotlin / Compose BOM / project version の直書きが残っていないことを grep で確認する (→ Scenario: バージョン直書きの不在)
- [x] 4.6 `maui/android/KsSettingsView.Binding.Android` の `dotnet build` (Exec 経由の `gradlew assembleRelease`) が成功することを確認する (→ Scenario: MAUI binding からの native ビルド)
