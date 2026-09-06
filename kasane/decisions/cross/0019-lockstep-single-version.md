---
id: 0019
title: 全 platform を lockstep の単一バージョンでリリースする
status: accepted
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
- (2026-09-04 追記) 上の「tag 1 本をトリガーにした 1 ワークフロー」は、tag 先行で lockstep が壊れるのを防ぐため version 入力の手動起動に置き換わった (cross/ADR-0020)。1 ワークフローで全 platform を一斉に出す点は変わらない。
- (2026-09-04 追記、出典: ロードマップの制約と初回リリース) semver には prerelease 形式 `X.Y.Z-{alpha|beta|rc}.N` を含める。`-pre` / `-preview` は Maven の版比較で正式版より新しいと判定されるため使わない。NuGet と SwiftPM は suffix を prerelease として扱うが Maven Central では同格に見えるため、その旨は README の prerelease 節が担う。初回リリースは `0.1.0-beta.1` で、3 レジストリと配信リポジトリの tag が同じ文字列で揃うことを実証した。
- (2026-09-04 追記、同上) 負: 「全部同じ番号を入れる」の案内に SwiftPM だけ例外がある。`from:` は prerelease を解決しないため、prerelease の間は README のインストール例を `exact:` で書く。MAUI の facade → binding は完全一致ではなく下限指定で、lockstep の同時発行と NuGet の最小版選択で同版に揃う。消費者検証はその一致を検査する。

出典: kasane/roadmaps/package-distribution/exploration.md (B) / ../KsDialogs/kasane/decisions/cross/0009-lockstep-single-version.md (翻案元)
出典 (2026-09-04 prerelease 形式・初回リリースの追記): kasane/roadmaps/package-distribution/exploration.md (prerelease の扱い、2026-08-21 追記) / kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md (初回リリースの version) / kasane/changes/archive/2026-09-04-add-release-workflow/evidence/github-actions-runs.txt (12 節)
