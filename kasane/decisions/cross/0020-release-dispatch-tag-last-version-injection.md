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
- (2026-09-01 追記) **tag の表記は接頭辞なしの `X.Y.Z`**。dispatch 入力・tag・SwiftPM の解決バージョン・Gradle / MSBuild への注入値が同一文字列のまま変換なしで流れることを優先する。monorepo と SwiftPM 配信リポジトリ (`KsSettingsView-SPM`) の tag は同じ値。姉妹ライブラリ KsDialogs も同表記で揃える。
- (2026-09-04 追記、出典: release workflow の設計) **本番のリリースは `main` からのみ起動**し、secrets は GitHub Environment `release` (deployment branch policy = `main`) に集約して publish job だけが持つ。`main` の先端は「最新リリース、またはリリース進行中のリリース候補」であり、`develop` からの pull request だけが入る (head の検査は検証 CI の lint job)。`dry-run` 入力によるリハーサルは publish 手前で止まるため、起動ブランチの制限を免除する。手順は handbook `cross/release-procedure.md`。

## Alternatives Considered

- **tag push をトリガーにする (tag が先)**: 却下。SwiftPM は tag の時点でリリース済みになるため、後段の Maven / NuGet publish が失敗すると iOS だけ先行した状態が残り、tag 削除 + 打ち直しでしか回復できない。
- **release ブランチ / PR マージをトリガーにする**: 却下。tag が先に出る問題は同じで、ブランチ運用だけが増える。
- **ファイル (catalog / Directory.Build.props) を version の正とし、tag との一致を CI で検証する**: 却下。version bump のコミットが毎リリース必要になり、platform ごとに別ファイルを同時に更新する手間と不一致の余地が残る。tag 起点の注入なら構造的にずれない。
- **tag 表記を `vX.Y.Z` にする** (2026-09-01 追記): 却下。SwiftPM は `v` 付き tag も解決できるため技術差はないが、Maven / NuGet の version 表記には `v` が入らないため、CI・手順書・検証の各所に v の付け外し変換が散らばる。GitHub 慣例の見た目より変換ゼロを取った。
- **`develop` から起動し `main` は作らない** (2026-09-04 追記): 却下。作業途中の commit からも起動できてしまい、リリース対象 commit が一意に定まらない。
- **publish 済み version の再実行を禁止し、次の version で出し直す** (2026-09-04 追記): 却下。部分 publish (例: NuGet は出たが Maven は出ていない) を同じ version で埋められないと、その version が片側だけ存在する欠番として残り lockstep (cross/ADR-0019) が崩れる。
- **smoke を通してから tag と GitHub Release を作る** (2026-09-04 追記): 却下。smoke 失敗の時点で公開は取り消せず、tag を止めても「出ているのに tag が無い」状態が残るだけで利用者を守れない。smoke の間、配信リポジトリの tag だけ先行する窓もできる。
- **release workflow が README の version を書き換えて `main` に commit する** (2026-09-04 追記): 却下。CI に `main` への push 権限と branch protection の bypass が要り、本 ADR の「CI が bump commit を積まない」に反する。

## Consequences

- 正: 外に出る成果物は常に全 platform 揃っており、失敗時は再実行するだけで後始末が要らない。
- 正: version の宣言を複数ファイルで同期する作業が消える。
- 正: NuGet.org の push (unlist しかできない不可逆操作) を Maven の release より前、tag より前に置くことで、不可逆操作の後に失敗し得る工程を最小にできる。
- 負: リリースは GitHub UI (または `gh workflow run`) からの手動起動になり、git 操作だけでは完結しない。
- 負: ローカルビルドや Sample は開発用 version (SNAPSHOT / dev) で動き、リリース版番号はリポジトリのファイルからは読めない (tag と Release が履歴になる)。
- 負: Maven Central Portal の「upload して保留 → 後で release」の 2 段階を CI から操作する必要がある。
- (2026-09-02 追記、出典: 消費者検証の実装結果) 注入の受け口は決定時点では未実装だった。Gradle 側は `android/build.gradle.kts` が `-Pversion=` の注入があればそれを全モジュールの version に使い、無いときだけカタログの開発用既定値 (`0.1.0-SNAPSHOT`) を使う形で、消費者検証の変更 (add-consumer-verification) が付随修正として実装した。MSBuild 側は `Version` の既定値 `0.0.0-dev` を `-p:Version=` が上書きする形で NuGet 化の時点から成立している。
- (2026-09-02 追記、同上) 負: 署名鍵 (`signingInMemoryKey`) を持たない Maven 発行は、リリース版の version でも署名を skip して未署名で成功する (publish 前の dry-run が鍵なしでリリース版を扱えるようにするため)。鍵の渡し忘れは Maven Central Portal の未署名拒否で止まるが CI 自身の検査ではないため、release workflow は publish 前に署名ファイルの生成を確認する。

以下は release workflow の実装と初回リリース (`0.1.0-beta.1`、2026-09-04) で確定した帰結 (出典: 実装結果)。

- Maven Central Portal の 2 段階は次の形で CI から操作する: vanniktech plugin の `publishToMavenCentral` (自動 release なし = `USER_MANAGED`) で upload し、plugin のログ 1 行から deployment ID を抽出して run の artifact に保存する。NuGet push の後、Portal Publisher API で `VALIDATED` を再確認してから release する。plugin には保留 deployment を後から release するタスクがなく、Portal に deployment 一覧 API も「座標 + version が公開済みか」を返す API も無いため、ID のログ抽出と API 直接呼び出しが唯一の手段で、公開確認は `repo1.maven.org` への HEAD で代替する。負: ID の抽出は plugin のログ文言に依存する。抽出できなければその場で失敗させ、静かに通らないようにしている。
- 「失敗時は再実行するだけ」を成立させる条件は、publish 段の全ステップが存在検査で冪等であること (配信リポジトリの commit は差分ゼロなら skip、Maven upload は前回 deployment の状態で分岐、NuGet push は `--skip-duplicate`、tag は同じ内容なら skip・別内容なら失敗、Release は既存なら触らない) と、deployment ID を attempt をまたいで引き継ぐこと。失敗経路では drop 可能な状態 (VALIDATED / FAILED) の deployment だけを drop する。再実行の位置ごとの挙動は handbook `cross/release-procedure.md` が持つ。
- 配信リポジトリへの push は commit と tag を分ける。commit は publish 段の先頭に置き、deploy key の認証と push 経路の失敗を不可逆操作 (NuGet push・Maven release) の前に出す。tag は Maven release の後・monorepo の tag の直前に置き、iOS の公開瞬間を monorepo の tag と同じ段に揃える。途中失敗で残る未 tag の commit は公開されず、次回上書きされる。
- 公開後の smoke (公開レジストリからの解決) は tag と GitHub Release の後に置き、公開レジストリへの反映待ち job を挟む。smoke が失敗しても tag と Release は残し、原因は次の version で直す。
- 「リリースのたびに version bump のコミットを積まない」の範囲はビルドの version 宣言 (catalog / `Directory.Build.props`) に限る。README のインストール例は具体 version を書き、リリース PR (`develop` → `main`) の中で専用 script (`scripts/release/set-readme-version.py`) が置換し、validate が一致を検査する。人の PR に乗るので workflow に書き込み権限は要らない。
- Android の publish は dry-run が検証した未署名の成果物ではなく、publish job が鍵つきで再ビルドしたものを upload する。同一 OS・JDK・commit で再現することを実測で確認し、署名ファイルを除く同一性比較で差異があれば upload しない。署名ファイル (`.asc`) の存在検査も CI 自身が行うため、上の 2026-09-02 追記の「CI 自身の検査ではない」は解消した。
- 初回リリースは attempt 1 で完走し、所要 39 分 (見込みは 60〜90 分。消費者検証 MAUI の dry-run 12 分と publish 11 分が大半で、Maven Central と nuget.org の反映待ちは数秒だった)。nuget.org の Trusted Publishing (OIDC) の初回 login は事前検証できず本番で初めて踏んだが成功した。

出典: kasane/roadmaps/package-distribution/exploration.md (F1・F2) / kasane/decisions/cross/0019-lockstep-single-version.md
出典 (2026-09-01 tag 表記の確定): kasane/roadmaps/package-distribution/phases/phase-4-ios-packaging/history.md (2026-09-01「tag 表記の統一」)
出典 (2026-09-02 実装結果の追記): kasane/changes/archive/2026-09-02-add-consumer-verification/deviation.md (付随修正 1・2 件目)
出典 (2026-09-04 起動条件・却下案・実装結果の追記): kasane/roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md (決定事項) / kasane/changes/archive/2026-09-04-add-release-workflow/design.md (Decisions) / kasane/changes/archive/2026-09-04-add-release-workflow/deviation.md / kasane/changes/archive/2026-09-04-add-release-workflow/evidence/github-actions-runs.txt (9・12 節)
