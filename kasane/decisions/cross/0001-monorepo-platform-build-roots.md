---
id: 0001
title: モノレポとプラットフォーム別ビルドルート
status: accepted
date: 2026-05-06
---

## Context

KsSettingsView は iOS Native、Android Native、MAUI、および将来の KMP を横断するライブラリ群である。クロスプラットフォームの変更を同期して管理する必要がある一方、Xcode / SwiftPM、Gradle / AGP、.NET SDK ではビルドツールチェインと IDE のワーキングセットが異なる。

後続の Core、各 Native UI、Cell、MAUI バインディングを並行して開発・テストでき、各 IDE が自身のプロジェクトを独立して認識できる構成が必要である。

## Decision

単一リポジトリ配下に iOS、Android、MAUI、Sample、ドキュメントを配置し、クロスプラットフォームの変更、バージョニング、CHANGELOG、および変更管理を一元化する。

ビルドについては、iOS、Android、MAUI の各ディレクトリを独立したビルドルートとして扱い、リポジトリルートには共通ビルドファイルを置かない。各プラットフォームは、それぞれの標準ビルド入口を IDE から直接開く。

## Alternatives Considered

- iOS、Android、MAUI を別々のリポジトリに分割する案。プラットフォーム独立性は高いが、API 変更時に複数リポジトリで同期した PR が必要となり、単一メンテナーで始めるプロジェクトには運用コストが高いため採用しない。
- リポジトリルートに Kotlin Multiplatform 風の統合 `build.gradle.kts` を置く案。KMP 導入時には再検討しうるが、現時点では Native プロジェクトを先に独立させる方針であり、共通ビルドファイルが各 IDE のプロジェクト認識を阻害するため採用しない。

## Consequences

- クロスプラットフォームの API 変更を同一 PR で完結でき、整合性を確認しやすくなる。
- バージョニング、CHANGELOG、および Sample の参照経路を一元化できる。
- 各プラットフォームの開発者は、対象ディレクトリを IDE で開くだけで独立して作業できる。
- リポジトリルートから全プラットフォームを一括ビルドする共通入口は持たないため、プラットフォームごとのビルド手順が必要になる。

出典: openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md
