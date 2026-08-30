---
id: 0019
title: 全 platform を lockstep の単一バージョンでリリースする
status: proposed
date: 2026-08-21
---

## Context

KsSettingsView は iOS (SwiftPM)・Android (Maven Central)・MAUI (NuGet.org) の 3 platform を同一モノレポから配布する (cross/ADR-0018)。MAUI の NuGet は同一コミットの iOS / Android native 成果物をビルドして同梱する構造 (maui/ADR-0006) であり、Sample は全 platform で同一画面構成を要求される (cross/ADR-0016)。SwiftPM は git tag をそのまま version として解釈する。

現状、version は Android 4 module の `build.gradle.kts` に `0.1.0-SNAPSHOT` として重複して書かれ、iOS と MAUI には version 表現がない。

姉妹ライブラリ KsDialogs は同じ判断を先に下しており (KsDialogs cross/ADR-0009)、本 ADR はその翻案である。

## Decision

全 platform・全 artifact に同一の semver `x.y.z` を付け、モノレポの git tag と一致させる。1 回のリリースで SwiftPM tag・Maven artifact・NuGet package をすべて同じ version で一斉に出す。

- platform ごとの独立したバージョンや互換マトリクスは作らない。利用者への案内は「全部同じ番号を入れる」の 1 行とする
- version の宣言箇所は platform ごとに 1 箇所へ集約する (Android は `android/gradle/libs.versions.toml`、change upgrade-android-build-toolchain で導入)。tag と宣言値の一致をどう機械的に担保するか (tag からの注入か、宣言値を正として tag と照合するか) は配信 CI の設計で決める

## Alternatives Considered

- **platform ごとの独立バージョン (`ios/vX.Y.Z` のような接頭辞付き tag)**: 却下。SwiftPM は接頭辞付き tag を version として解釈できず、iOS を別リポジトリへ切り出さない限り成立しない。changelog と CI トリガーも platform 数だけ増える。
- **Native は lockstep、MAUI だけ独立 (MAUI が Native の version を pin する)**: 却下。MAUI 利用者が「MAUI x.y は Native a.b を同梱」と読み解く必要が生じ、同一コミットから native を同梱する構造の利点が失われる。

## Consequences

- 正: 「MAUI 1.2.0 = Native 1.2.0」が自然に成り立ち、利用者への説明と不具合報告時の版特定が単純になる。
- 正: 配信 CI は tag 1 本をトリガーにした 1 ワークフローで済む。
- 正: KsDialogs と同じ運用になる。
- 負: 1 platform だけの修正でも全 platform の version が上がり、無変更の platform にも空リリースが出る。
- 負: リリース CI は常に全 platform の一斉ビルドが前提になり、1 platform のビルド失敗がリリース全体を止める。

出典: kasane/roadmaps/package-distribution/exploration.md (B) / ../KsDialogs/kasane/decisions/cross/0009-lockstep-single-version.md (翻案元)
