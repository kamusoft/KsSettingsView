---
id: 0020
title: リリースは version 入力の手動起動で行い、全 platform の publish 成功後に tag を打ち、version は CI が注入する
status: accepted
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
- **tag の表記は接頭辞なしの `X.Y.Z`**。dispatch 入力・tag・SwiftPM の解決バージョン・Gradle / MSBuild への注入値が同一文字列のまま変換なしで流れることを優先する。monorepo と SwiftPM 配信リポジトリ (`KsSettingsView-SPM`) の tag は同じ値。姉妹ライブラリ KsDialogs も同表記で揃える。
- **本番のリリースは `main` からのみ起動**し、secrets は GitHub Environment `release` (deployment branch policy = `main`) に集約して publish job だけが持つ。`main` の先端は「最新リリース、またはリリース進行中のリリース候補」であり、`develop` からの pull request だけが入る (head の検査は検証 CI の lint job)。`dry-run` 入力によるリハーサルは publish 手前で止まるため、起動ブランチの制限を免除する。手順は handbook `cross/release-procedure.md`。

## Alternatives Considered

- **tag push をトリガーにする (tag が先)**: 却下。SwiftPM は tag の時点でリリース済みになるため、後段の Maven / NuGet publish が失敗すると iOS だけ先行した状態が残り、tag 削除 + 打ち直しでしか回復できない。
- **release ブランチ / PR マージをトリガーにする**: 却下。tag が先に出る問題は同じで、ブランチ運用だけが増える。
- **ファイル (catalog / Directory.Build.props) を version の正とし、tag との一致を CI で検証する**: 却下。version bump のコミットが毎リリース必要になり、platform ごとに別ファイルを同時に更新する手間と不一致の余地が残る。tag 起点の注入なら構造的にずれない。
- **tag 表記を `vX.Y.Z` にする**: 却下。SwiftPM は `v` 付き tag も解決できるため技術差はないが、Maven / NuGet の version 表記には `v` が入らないため、CI・手順書・検証の各所に v の付け外し変換が散らばる。GitHub 慣例の見た目より変換ゼロを取った。
- **`develop` から起動し `main` は作らない**: 却下。作業途中の commit からも起動できてしまい、リリース対象 commit が一意に定まらない。
- **publish 済み version の再実行を禁止し、次の version で出し直す**: 却下。部分 publish (例: NuGet は出たが Maven は出ていない) を同じ version で埋められないと、その version が片側だけ存在する欠番として残り lockstep (cross/ADR-0019) が崩れる。
- **smoke を通してから tag と GitHub Release を作る**: 却下。smoke 失敗の時点で公開は取り消せず、tag を止めても「出ているのに tag が無い」状態が残るだけで利用者を守れない。smoke の間、配信リポジトリの tag だけ先行する窓もできる。
- **release workflow が README の version を書き換えて `main` に commit する**: 却下。CI に `main` への push 権限と branch protection の bypass が要り、本 ADR の「CI が bump commit を積まない」に反する。

## Consequences

- 正: 外に出る成果物は常に全 platform 揃っており、失敗時は再実行するだけで後始末が要らない。
- 正: version の宣言を複数ファイルで同期する作業が消える。
- 正: NuGet.org の push (unlist しかできない不可逆操作) を Maven の release より前、tag より前に置くことで、不可逆操作の後に失敗し得る工程を最小にできる。
- 負: リリースは GitHub UI (または `gh workflow run`) からの手動起動になり、git 操作だけでは完結しない。
- 負: ローカルビルドや Sample は開発用 version (SNAPSHOT / dev) で動き、リリース版番号はリポジトリのファイルからは読めない (tag と Release が履歴になる)。
- 負: Maven Central Portal の「upload して保留 → 後で release」の 2 段階を CI から操作する必要がある。

出典: kasane/roadmaps/package-distribution/exploration.md (F1・F2) / kasane/decisions/cross/0019-lockstep-single-version.md
出典 (tag 表記の確定): kasane/roadmaps/package-distribution/phases/phase-4-ios-packaging/history.md (2026-09-01「tag 表記の統一」)
出典 (version 注入の受け口): kasane/changes/archive/2026-09-02-add-consumer-verification/deviation.md (付随修正 1・2 件目)
出典 (起動条件と却下案): kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md (決定事項) / kasane/changes/archive/2026-09-04-add-release-workflow/design.md (Decisions) / kasane/changes/archive/2026-09-04-add-release-workflow/deviation.md / kasane/changes/archive/2026-09-04-add-release-workflow/evidence/github-actions-runs.txt (9・12 節)
整理: 2026-09-07 Consequences から実装結果の記述 (Portal の 2 段階操作・再実行の冪等条件・配信リポジトリの commit / tag 分離・smoke の位置・version 注入の受け口と署名の扱い・初回リリースの所要と Trusted Publishing の初回 login) を除き、Decision と Alternatives の日付マーカー 7 箇所を溶かした。除いた記述のうち Portal の 2 段階操作と制約・公開確認の手段・Android の鍵つき再ビルドは [リリースパイプラインの構成](../../concepts/cross/architecture/release-pipeline.md) へ移し、再実行の挙動と所要時間は [リリース手順](../../handbook/cross/release-procedure.md) が既に持つ。初回リリースの Trusted Publishing の初回 login は一度きりの観測のため残さない。決定内容 (Context / Decision / Alternatives) は不変
現行照合: 2026-09-07 確認。`.github/workflows/release.yml` の publish job は `workflow_dispatch` 起動で、書き込みを伴う step は 配信リポジトリへの snapshot commit → Maven Central Portal へ upload (保留) → deployment ID の artifact 保存 → 検証の決着待ち → NuGet.org へ push → Maven の release → 配信リポジトリの tag → monorepo の tag → GitHub Release の順。Decision の「取り消せる順」(upload → NuGet push → Maven release → tag) はそのまま保たれており、upload と NuGet push の間に Central Portal の検証決着待ちが加わった (fix-release-central-validation-wait)。判定: 維持
