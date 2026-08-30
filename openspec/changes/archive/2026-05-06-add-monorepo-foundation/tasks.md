## 1. ディレクトリ構成

- [x] 1.1 リポジトリルートに `ios/`、`android/`、`maui/`、`samples/`、`docs/` の各ディレクトリを作成
- [x] 1.2 `samples/` 配下に `ios/`、`android/`、`maui/` の 3 サブディレクトリを作成
- [x] 1.3 `.gitignore` を整備し、`bin/`、`obj/`、`build/`、`.build/`、`DerivedData/`、`*.user`、`.idea/`、`.vscode/`、`local.properties` を除外する

## 2. iOS ビルド入口

- [x] 2.1 `ios/Package.swift` を最小構成（Swift Tools 5.10、platforms: `.iOS(.v16)`、空の `targets: []`）で作成
- [x] 2.2 `swift package describe`（または `swift package dump-package`）が `ios/` ディレクトリで成功することを確認

## 3. Android ビルド入口

- [x] 3.1 `android/settings.gradle.kts` を最小構成（`pluginManagement`、`dependencyResolutionManagement` を `google()` / `mavenCentral()` 指定、`rootProject.name = "ks-settingsview"`、`include(...)` は空）で作成
- [x] 3.2 `android/build.gradle.kts` を最小構成（プラグインなしの空ファイルでよい）で作成
- [x] 3.3 `android/gradle.properties` を作成（`org.gradle.jvmargs`、`android.useAndroidX=true`、`kotlin.code.style=official`）
- [x] 3.4 `android/gradle/wrapper/` 配下に Gradle Wrapper を追加（`gradle-wrapper.properties` で Gradle 8.10、`gradle-wrapper.jar`、`gradlew`、`gradlew.bat`）
- [x] 3.5 `./gradlew tasks` が `android/` ディレクトリで成功することを確認

## 4. MAUI ビルド入口

- [x] 4.1 `maui/KsSettingsView.slnx` を空のソリューションファイルとして作成（Visual Studio / `dotnet sln` で読み込み可能な最小 slnx 形式）
- [x] 4.2 `dotnet sln maui/KsSettingsView.slnx list` が成功することを確認（プロジェクトは未追加でよい）

## 5. ドキュメント

- [x] 5.1 リポジトリルートに `README.md` を作成し、プロジェクト概要・対応プラットフォーム・主要モジュール一覧・各プラットフォームのビルド手順リンク・ライセンス情報を記載
- [x] 5.2 `LICENSE` を MIT ライセンスで作成（著作権表示は `Copyright (c) kamusoft`）
- [x] 5.3 `docs/development.md` を作成し、最低ツールチェイン（Xcode 16+、Swift 5.10+、iOS 16.0、AGP 8.7+、Gradle 8.10+、JDK 17、minSdk 29、compileSdk 35、.NET 9 SDK、MAUI Workload 9.0.x）を明記
- [x] 5.4 `docs/conventions.md` を作成し、ディレクトリ命名規約、Swift モジュール命名（PascalCase）、Kotlin パッケージ命名（lowercase ドット区切り）、.NET 名前空間（PascalCase）、パッケージ ID プレフィックス（iOS/Android: `jp.kamusoft.kssettingsview.*`、Maven Central groupId: `jp.kamusoft`、.NET: `KsSettingsView.*`）を記述

## 6. 検証

- [x] 6.1 全ディレクトリ構成が `monorepo-foundation` の Scenario をすべて満たすことを手動確認
- [x] 6.2 各プラットフォームのビルド入口コマンド（`swift package describe`、`./gradlew tasks`、`dotnet sln list`）がエラーなく実行できることを確認
- [x] 6.3 `docs/development.md`、`docs/conventions.md`、`README.md`、`LICENSE` の存在と内容を確認

## 依存関係

- 先行する変更提案：なし（最初の変更提案）
- 本変更提案の完了が前提となる後続：`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`

## 完了条件

- 全タスクのチェックボックスが完了している
- `monorepo-foundation` capability の全 Scenario が通る
- 各プラットフォームの入口ビルドコマンドが成功する
- `README.md`、`LICENSE`、`docs/development.md`、`docs/conventions.md` が存在し、内容が記述されている
