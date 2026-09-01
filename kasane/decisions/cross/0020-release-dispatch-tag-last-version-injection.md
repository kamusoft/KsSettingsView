---
id: 0020
title: リリースは version 入力の手動起動で行い、全 platform の publish 成功後に tag を打ち、version は CI が注入する
status: proposed
date: 2026-08-21
---

## Context

全 platform を lockstep の単一バージョンで配布する (cross/ADR-0019) が、チャネルごとに「リリース済みになる瞬間」が異なる。SwiftPM は git tag を push した瞬間に利用者が解決できるようになり、Maven Central と NuGet.org は CI の publish が成功して初めて出る。tag を起点にリリース CI を動かすと、後段の publish が失敗したときに iOS だけが先行して解決できる「lockstep が壊れた状態」が残り、tag の削除・打ち直しで回復するしかない。

また version の宣言箇所は platform ごとに別 (Android は `android/gradle/libs.versions.toml`、MAUI は `maui/Directory.Build.props`、iOS は tag のみ) で、ファイルを正にすると tag とファイルの一致をリリースのたびに人が保証することになる。

リポジトリには CI が存在せず (`.github/` なし)、リリース手順はこれから新設する。

## Decision

- リリース CI は **`workflow_dispatch` で version (semver) を入力して手動起動**する。tag push をトリガーにしない。
- CI は全 platform のビルド・テストを通した後、**取り消せる順**で publish する: Maven Central Portal へ upload (自動 release せず保留) → NuGet.org へ push → Maven を release → 最後に **git tag と GitHub Release を作る**。tag は publish 全成功時にのみ生まれる。
- **version の SSoT は dispatch の入力値 (= 生成される tag)** とし、CI が Gradle (`-Pversion=`) と MSBuild (`-p:Version=`) へ注入する。リポジトリ内の宣言値 (catalog / Directory.Build.props) は開発用の既定値 (SNAPSHOT / dev) に留め、リリースのたびに version bump のコミットを積まない。
- (2026-09-01 追記) **tag の表記は接頭辞なしの `X.Y.Z`**。dispatch 入力・tag・SwiftPM の解決バージョン・Gradle / MSBuild への注入値が同一文字列のまま変換なしで流れることを優先する。monorepo と SwiftPM 配信リポジトリ (`KsSettingsView-SPM`) の tag は同じ値。姉妹ライブラリ KsDialogs も同表記で揃える。

## Alternatives Considered

- **tag push をトリガーにする (tag が先)**: 却下。SwiftPM は tag の時点でリリース済みになるため、後段の Maven / NuGet publish が失敗すると iOS だけ先行した状態が残り、tag 削除 + 打ち直しでしか回復できない。
- **release ブランチ / PR マージをトリガーにする**: 却下。tag が先に出る問題は同じで、ブランチ運用だけが増える。
- **ファイル (catalog / Directory.Build.props) を version の正とし、tag との一致を CI で検証する**: 却下。version bump のコミットが毎リリース必要になり、platform ごとに別ファイルを同時に更新する手間と不一致の余地が残る。tag 起点の注入なら構造的にずれない。
- **tag 表記を `vX.Y.Z` にする** (2026-09-01 追記): 却下。SwiftPM は `v` 付き tag も解決できるため技術差はないが、Maven / NuGet の version 表記には `v` が入らないため、CI・手順書・検証の各所に v の付け外し変換が散らばる。GitHub 慣例の見た目より変換ゼロを取った。

## Consequences

- 正: 外に出る成果物は常に全 platform 揃っており、失敗時は再実行するだけで後始末が要らない。
- 正: version の宣言を複数ファイルで同期する作業が消える。
- 正: NuGet.org の push (unlist しかできない不可逆操作) を Maven の release より前、tag より前に置くことで、不可逆操作の後に失敗し得る工程を最小にできる。
- 負: リリースは GitHub UI (または `gh workflow run`) からの手動起動になり、git 操作だけでは完結しない。
- 負: ローカルビルドや Sample は開発用 version (SNAPSHOT / dev) で動き、リリース版番号はリポジトリのファイルからは読めない (tag と Release が履歴になる)。
- 負: Maven Central Portal の「upload して保留 → 後で release」の 2 段階を CI から操作する必要がある。

出典: kasane/roadmaps/package-distribution/exploration.md (F1・F2) / kasane/decisions/cross/0019-lockstep-single-version.md
出典 (2026-09-01 tag 表記の確定): kasane/roadmaps/package-distribution/phases/phase-4-ios-packaging/history.md (2026-09-01「tag 表記の統一」)
