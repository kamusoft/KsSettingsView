# monorepo-foundation Specification

## Purpose

KsSettingsView のモノレポ土台を提供し、Native (iOS/Android)、MAUI、Sample、ドキュメントを単一リポジトリで一貫した構成・命名規約・ツールチェイン要件のもと管理する。

## Requirements

### Requirement: モノレポのディレクトリ構成

リポジトリは Native (iOS/Android)・MAUI・Sample・ドキュメントを単一リポジトリで管理しなければならない (SHALL)。各プラットフォームのソースコードは互いに分離されたトップレベルディレクトリに配置されなければならない (MUST)。

#### Scenario: トップレベルディレクトリの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** リポジトリルートを `ls` する
- **THEN** `ios/`、`android/`、`maui/`、`samples/`、`docs/`、`openspec/` の各ディレクトリが存在する

#### Scenario: Sample のサブディレクトリ構成

- **GIVEN** `samples/` ディレクトリ
- **WHEN** その配下を確認する
- **THEN** `samples/ios/`、`samples/android/`、`samples/maui/` の 3 つのサブディレクトリが存在する

### Requirement: ビルド入口ファイルの配置

各プラットフォームディレクトリには、当該プラットフォームのビルドを開始できる入口ファイルが配置されなければならない (SHALL)。

#### Scenario: iOS の SwiftPM 入口

- **GIVEN** `ios/` ディレクトリ
- **WHEN** ファイル一覧を確認する
- **THEN** `ios/Package.swift` が存在し、`swift package describe` がエラーなく実行できる

#### Scenario: Android の Gradle 入口

- **GIVEN** `android/` ディレクトリ
- **WHEN** ファイル一覧を確認する
- **THEN** `android/settings.gradle.kts` および `android/build.gradle.kts` が存在し、`./gradlew tasks` がエラーなく実行できる

#### Scenario: MAUI のソリューション入口

- **GIVEN** `maui/` ディレクトリ
- **WHEN** ファイル一覧を確認する
- **THEN** `maui/KsSettingsView.slnx` が存在する

### Requirement: 命名規約とパッケージ ID

各プラットフォームで一貫した命名規約とパッケージ ID 体系を採用しなければならない (MUST)。

#### Scenario: パッケージ ID の規約

- **GIVEN** プロジェクトを新規モジュールとして追加する場合
- **WHEN** パッケージ ID / バンドル ID / ルート名前空間を決定する
- **THEN** iOS は `jp.kamusoft.kssettingsview.*`、Android は `jp.kamusoft.kssettingsview.*`、Maven Central groupId は `jp.kamusoft`、.NET は `KsSettingsView.*` の接頭辞を用いる

#### Scenario: 命名規約のドキュメント化

- **GIVEN** 開発者が命名規約を確認したい場合
- **WHEN** `docs/conventions.md` を開く
- **THEN** ディレクトリ命名（kebab-case）、Swift モジュール名（PascalCase）、Kotlin パッケージ名（lowercase ドット区切り）、.NET 名前空間（PascalCase）の規約が記述されている

### Requirement: 最低ツールチェインの明示

開発に必要な最低ツール・SDK バージョンが文書化されていなければならない (SHALL)。

#### Scenario: 開発環境ドキュメントの存在

- **GIVEN** 新規開発者がリポジトリをクローンした場合
- **WHEN** `docs/development.md` を開く
- **THEN** Xcode 最低バージョン、Swift バージョン、iOS Deployment Target (iOS 16.0)、Android minSdk (29)、Android compileSdk、JDK バージョン、Gradle/AGP バージョン、.NET SDK バージョン (.NET 9) の各最低要件が明記されている

### Requirement: README の整備

リポジトリルートには KsSettingsView のプロジェクト概要・主要モジュール一覧・各プラットフォームのビルド方法へのリンクを記載した `README.md` が存在しなければならない (SHALL)。

#### Scenario: README のセクション

- **GIVEN** リポジトリルート
- **WHEN** `README.md` を開く
- **THEN** プロジェクト概要、対応プラットフォーム、モジュール一覧、各プラットフォームでのビルド手順（または `docs/development.md` へのリンク）、ライセンス情報が含まれる
