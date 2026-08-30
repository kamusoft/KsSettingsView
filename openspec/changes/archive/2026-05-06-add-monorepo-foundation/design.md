## Context

KsSettingsView は iOS Native (Swift)、Android Native (Kotlin)、MAUI (.NET)、将来の KMP の各プラットフォームを横断するライブラリ群である。各プラットフォームのビルドツールチェインが大きく異なるため（Xcode/SwiftPM、Gradle/AGP、.NET SDK）、単一リポジトリで管理しつつ各プラットフォームのワーキングセットが互いに干渉しない構成が求められる。本変更提案では、後続の各 capability（Core / iOS UI / Android UI / Cell / MAUI バインディング）が並行して開発・テスト・配信できるよう、土台となるディレクトリ構成・命名規約・最低ツールチェインを確立する。

## Goals / Non-Goals

**Goals:**
- 単一リポジトリで iOS / Android / MAUI / Sample / ドキュメントを一元管理する
- 各プラットフォームのビルド入口（`Package.swift`、`settings.gradle.kts`、`*.slnx`）を最小構成で先行配置し、後続作業者がすぐビルドできる状態にする
- 命名規約とパッケージ ID を統一し、後続変更でブレないようにする
- 開発者が必要とする最低ツールチェイン情報を `docs/development.md` に集約する

**Non-Goals:**
- 実コード（Cell・UI・モデル）の実装は行わない（後続変更提案で対応）
- KMP・配信（SwiftPM/Maven Central 公開）・CI/CD パイプライン構築は本変更提案では行わない（次フェーズ）
- 旧 `AiForms.Maui.SettingsView` からの移行 shim は提供しない（独立ブランドとして breaking change）

## Decisions

### Decision 1: モノレポ構成の採用

**選択**: 単一リポジトリ配下に `ios/`、`android/`、`maui/`、`samples/`、`docs/`、`openspec/` のトップレベルディレクトリを配置する。

**理由**:
- クロスプラットフォーム変更（例：Cell モデルの追加）が同一 PR で完結し、整合性確認が容易
- バージョニング・CHANGELOG・OpenSpec 管理が一元化できる
- Sample プロジェクトをプラットフォームごとに用意するため、参照経路が短くなる

**代替案**:
- マルチリポジトリ（`KsSettingsView.iOS`、`KsSettingsView.Android`、`KsSettingsView.Maui` を別リポジトリ）：プラットフォーム独立性は高いが、API 変更時に複数リポジトリの同期 PR が必要となり運用コストが高い。本プロジェクトの初期は単一メンテナーであり、モノレポの方が現実的。

### Decision 2: 各プラットフォームの独立ビルド

**選択**: `ios/Package.swift`、`android/settings.gradle.kts`、`maui/KsSettingsView.slnx` をそれぞれ独立した「ビルドルート」として扱い、ルートディレクトリには共通ビルドファイルを置かない。

**理由**:
- Xcode は `Package.swift` を含むディレクトリを開けば独立して動作する
- Android Studio は `android/` を開くだけで完結する
- .NET MAUI は `maui/KsSettingsView.slnx` を開けば良い
- ルートに共通ビルドファイルを置くと（例：`Makefile`、複合 Gradle）、各 IDE のプロジェクト認識を阻害する

**代替案**:
- ルートに統合 `build.gradle.kts`（Kotlin Multiplatform 風）：KMP を将来導入する際に再検討するが、現時点では Native プロジェクトを KMP より先に独立させる方針のため不採用。

### Decision 3: パッケージ ID プレフィックス `jp.kamusoft.kssettingsview`

**選択**: iOS バンドル ID / Android パッケージ名は `jp.kamusoft.kssettingsview.*`、Maven Central groupId は `jp.kamusoft`、.NET 名前空間は `KsSettingsView.*` とする。

**理由**:
- `kamusoft.jp` ドメインはユーザー（プロジェクトオーナー）が保有しており、Maven Central の DNS TXT 検証も自身で行える
- ドメイン取得・維持コストが追加発生しない
- reverse-DNS 形式は Apple / Google 両プラットフォームの標準慣習に準拠
- `kssettingsview` は kebab-case ではなく lowercase で連結（Android パッケージは ASCII lowercase 必須のため）
- .NET は PascalCase 慣例

**代替案**:
- `com.kamusoft.*`：`kamusoft.com` は他者が保有しており取得不可（whois で確認済み）。
- `dev.kamusoft.*` / `io.kamusoft.*`：新規ドメイン取得が必要となり、維持コストが発生する。
- `io.github.muak.*`：Maven Central は GitHub アカウント検証で受け付けるが、長く可読性が落ち、ブランド要素（kamusoft）が薄れる。所有ドメインがある以上、優先度は低い。

### Decision 4: 最低ツールチェイン

**選択**:
- iOS: Xcode 16 以上、Swift 5.10 以上、iOS Deployment Target 16.0
- Android: AGP 8.7 以上、Gradle 8.10 以上、JDK 17、minSdk 29、compileSdk 35
- .NET: .NET 9 SDK、MAUI Workload 9.0.x

**理由**:
- iOS 16 は `UICollectionLayoutListConfiguration`（iOS 14+）+ DiffableDataSource（iOS 13+）に加え、`UIHostingConfiguration`（iOS 16+）による SwiftUI Cell の高効率組み込みが利用できる
- Android API 29（Android 10）は計画書で確定済みの最低 API
- .NET 9 は MAUI 9 の前提

**代替案**:
- iOS 13 / 15 維持・Android API 26 維持：旧版互換性のため低いが、新規プロジェクトとしての価値（モダン API 活用、特に `UIHostingConfiguration` による CustomCell の高効率実装）が薄れるため不採用。

## Risks / Trade-offs

- **リスク**: 各 IDE が別ディレクトリのため、開発者がリポジトリルートで「全ビルド」できない
  - **緩和策**: `docs/development.md` に各プラットフォームのビルド手順を明記。将来 `scripts/build-all.sh` で順次ビルドするスクリプトを Phase 3 で追加検討。
- **リスク**: モノレポのチェックアウトサイズが将来肥大化
  - **緩和策**: バイナリ成果物は git に含めず、`.gitignore` で `bin/`、`obj/`、`build/`、`.build/`、`DerivedData/` を除外。
- **トレードオフ**: 単一リポジトリのため、特定プラットフォームのみ更新したいユーザーも全リポジトリをクローンする必要がある（NuGet/SwiftPM/Maven Central 利用者には影響なし、コントリビューターのみ）。

## Migration Plan

新規プロジェクトのため移行は不要。旧 `AiForms.Maui.SettingsView` は独立リポジトリで存続し、本リポジトリとの自動移行は提供しない。
