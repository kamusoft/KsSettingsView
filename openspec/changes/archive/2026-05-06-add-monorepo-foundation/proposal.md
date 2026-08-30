## Why

`AiForms.Maui.SettingsView` を `KsSettingsView` としてリニューアルし、Native (iOS/Android) を一次ソース、MAUI を Native のラッパーとして再構築する。複数プラットフォーム（iOS Native / Android Native / MAUI / 将来の KMP）と複数の Sample を一元管理するため、最初にモノレポの土台（ディレクトリ構成・ビルド入口・ドキュメント雛形）を確立する必要がある。これがないと後続の各 capability（Core / iOS UI / Android UI / Cell / MAUI バインディング）が独立して進められない。

## What Changes

- リポジトリ直下にプラットフォーム別ディレクトリを定義する：`ios/`、`android/`、`maui/`、`samples/{ios,android,maui}/`、`docs/`
- 各ディレクトリにルートビルドファイル（`ios/Package.swift`、`android/settings.gradle.kts` + `build.gradle.kts`、`maui/KsSettingsView.slnx`）を最小構成で配置する
- リポジトリルートに `README.md`（プロジェクト概要・モジュール一覧・ビルド方法インデックス）と `LICENSE` 雛形を配置する
- iOS / Android のビルドツールチェイン（最低 Xcode/Swift バージョン、Gradle/AGP バージョン、JDK バージョン、Android SDK API 29 以上、iOS 16 以上）を `docs/development.md` に明示する
- モノレポの命名規約・パッケージ ID 規約（iOS: `jp.kamusoft.kssettingsview.*`、Android: `jp.kamusoft.kssettingsview.*`、Maven Central groupId: `jp.kamusoft`、.NET: `KsSettingsView.*`）を `docs/conventions.md` に記述する

## Capabilities

### New Capabilities
- `monorepo-foundation`: モノレポのディレクトリ構成・ビルド入口ファイル・命名規約・最低ツールチェインを規定する

### Modified Capabilities
（なし）

## Impact

- 影響範囲：リポジトリ全体の構造。後続の capability すべてが本構成に従う前提となる
- 依存：なし（最初の変更提案）
- 後続変更が依存：`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-cell-types-*`、`add-maui-bindings`
- リスク：低。空のディレクトリ・最小ビルドファイルのみで、コードを伴わない
