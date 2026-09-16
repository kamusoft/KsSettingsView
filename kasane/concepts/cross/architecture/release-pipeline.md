---
type: concept
title: リリースパイプラインの構成
description: 3 platform を 1 本の release workflow で公開する段の構成、publish 段の内部順序、version の注入経路、各チャネルへの publish 機構、Release ノートの出所、待ちの時間予算、公開確認の手段
tags: [architecture, release, ci, distribution]
timestamp: 2026-09-14
---

# リリースパイプラインの構成

この文書は、iOS・Android・MAUI を 1 本の release workflow で同時に公開する仕組みを説明する。読むと、publish の前後にどの段が並ぶか、publish 段の内側がどんな順序で外へ書き込むか、version がどこから注入されるか、各レジストリへ出すときに CI が何を操作しているか、公開できたことをどう確かめているかが分かる。

3 platform のソースは 1 つのリポジトリ (monorepo) にあり、platform ごとに独立したビルドの起点 (build root) を持つ。その分割と、下に出てくる「本体検証」「消費者検証」の役割は [リポジトリとビルドの責務境界](repository-boundaries.md) が定義する。起動と再実行の操作手順は [リリース手順](../../../handbook/cross/release-procedure.md) が持つ。

## 段の構成

3 platform の公開は 1 本の release workflow (`.github/workflows/release.yml`、version を入力する手動起動 = `workflow_dispatch`) が担う。

```
           ┌─ 本体検証 ───────────────────────────┐
validate ──┤                                      ├─ publish → 反映待ち → 消費者検証 (smoke)
           └─ 配布物の生成 → 消費者検証 (dry-run) ┘
```

本体検証と「配布物の生成 → 消費者検証 (dry-run)」は互いに依存せず並列に走り、publish は両方の成功を待つ。各段の役割は次のとおり。

| 段 | 何をするか |
|---|---|
| validate | 入力 version の形式と、同じ version の tag が monorepo と配信リポジトリに無いことを検査し、Release ノートを組み立てて検査する |
| 本体検証 | 3 platform のビルドとテストを全件通す |
| 配布物の生成 | 各チャネルへ出す成果物 (SwiftPM スナップショット・aar・NuGet パッケージ) を作る |
| 消費者検証 (dry-run) | 生成した成果物を、利用者と同じ解決経路でローカルのフィードから引いてビルドする。公開はしない |
| publish | 配信先へ実際に書き込む (下の順序) |
| 反映待ち | 公開レジストリに版が現れるまで待つ |
| 消費者検証 (smoke) | 公開レジストリから解決してビルドする |

publish より前の段がすべて成功したときにだけ配信先へ書き込む。

## publish 段の順序

publish 段の内側は**取り消せる順**に並ぶ。不可逆な操作 (NuGet.org への push は unlist しかできない) の後に失敗し得る工程を最小にするためで、順序の根拠は [ADR-0020](../../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md)。外へ書き込む step は次の順に走る。

1. 配信リポジトリ `KsSettingsView-SPM` へ snapshot commit を push する
2. Maven Central Portal へ upload する (自動 release せず保留)
3. deployment ID を run の artifact へ保存する
4. Portal の検証の決着を待つ — `VALIDATED` 以外ならここで止まる
5. **NuGet.org へ push する** (ここから不可逆)
6. Maven Central の deployment を release する
7. 配信リポジトリの tag を push する
8. monorepo の tag を push する
9. GitHub Release を作る

smoke はこの後、公開レジストリへの反映待ちを挟んで走る。smoke が失敗しても tag と Release は残し、原因は次の version で直す。

## version の注入

version の唯一の正 (SSoT) は `workflow_dispatch` の入力値 (= 生成される tag) で、CI が各ビルドへ注入する。

| platform | 注入の受け口 | 注入が無いとき |
|---|---|---|
| Android | `android/build.gradle.kts` が `-Pversion=` を全モジュールの version に使う | version catalog (`android/gradle/libs.versions.toml`) の開発用既定値 `0.1.0-SNAPSHOT` |
| MAUI | `maui/Directory.Build.props` の `Version` を `-p:Version=` が上書きする | 開発用既定値 `0.0.0-dev` |
| iOS | version 表現を持たない (tag が version) | — |

README と利用者向け Skill のインストール例は具体 version を持たず、プレースホルダ `{version}` を置いて最新版の案内を GitHub Releases に委ねる ([ADR-0029](../../../decisions/cross/0029-install-examples-without-pinned-version.md))。リリースのたびに文書へ version を書き戻す経路は存在しない。ビルドの version 宣言 (version catalog / `Directory.Build.props`) にリリースごとの bump コミットも積まない。

## 各チャネルへの publish

### SwiftPM (配信リポジトリ)

iOS は monorepo を直接解決させず、専用の公開配信リポジトリ `KsSettingsView-SPM` から配る ([ADR-0018](../../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md))。`ios/Package.swift` / `ios/Sources/` / `ios/Tests/` と `LICENSE`・誘導 README のスナップショットを配信リポジトリのルートへ commit し、同じ version の tag を push する (ファイル配置は `scripts/spm-snapshot/`)。monorepo のルートに `Package.swift` は置かず、`ios/Package.swift` が開発用かつ配信用の唯一のマニフェストである。

**commit と tag は分ける。** commit を publish 段の先頭 (順序 1) に置くのは、deploy key の認証と push 経路の失敗を不可逆操作より前に出すためである。tag は Maven の release の後 (順序 7) に押し、iOS の公開瞬間を monorepo の tag と同じ段に揃える。途中で失敗して残る未 tag の commit は公開されず、次回上書きされる。利用者が解決できるのは tag の付いた版だけである。

### Maven Central (Portal の 2 段階)

Central Portal は「upload して保留 → 後で release」の 2 段階で、CI からは Maven Central 公開用の Gradle plugin (vanniktech maven-publish) と Portal Publisher API を組み合わせて操作する。

1. plugin の `publishToMavenCentral` を自動 release なし (`USER_MANAGED`) で実行して upload する
2. plugin のログ 1 行から deployment ID を抽出し、run の artifact に保存する
3. Publisher API で検証の決着を待ち、`VALIDATED` を確認してから release する

**ログ抽出と API の直接呼び出しが唯一の手段である。** plugin には保留 deployment を後から release するタスクが無く、Portal には deployment の一覧 API も「座標 (groupId:artifactId) + version が公開済みか」を返す API も無い。ID を抽出できなければその場で失敗させ、静かに通らないようにしている。

Android の publish は dry-run が検証した未署名の成果物ではなく、publish job が鍵つきで再ビルドしたものを upload する。同一 OS・JDK・commit で再現することは実測で確認済みで、署名ファイルを除く同一性比較に差異があれば upload しない。署名鍵を持たない Maven 発行はリリース版の version でも署名を skip して成功してしまうため (dry-run が鍵なしでリリース版を扱えるようにした結果)、署名ファイル (`.asc`) の存在検査を CI 自身が行う。

### NuGet.org

nuget.org への push は Trusted Publishing (OIDC) で認証する。push は `--skip-duplicate` で、既に公開済みの版は skip される。

## Release ノート

Release ノートは、`main` 宛て pull request 本文の `## Changes` セクションを集めて組み立てる ([ADR-0030](../../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md))。収集の範囲・書式・欠落時に止まる契約は ADR が持ち、実装は `scripts/release/build-release-notes.py` にある。

**組み立ては validate 段で一度だけ行い、結果を artifact で publish へ渡す。** publish の Release 作成は渡された本文をそのまま使い、pull request 本文を読み直さない。検査を通した本文と実際に公開されるノートを同一にするためである。GitHub Release には prerelease の印を付けず、`--latest` で最新を明示する ([ADR-0029](../../../decisions/cross/0029-install-examples-without-pinned-version.md))。

## 待ちの時間予算

待ちを抱える job (publish と反映待ち) は、**待ちの上限がスクリプト側の定数、job の打ち切りが workflow 側の `timeout-minutes`** という二重管理になっている。片方だけを延ばしても平時の実行は緑のまま進むため、`scripts/release/check-time-budget.py` が両者を読んで突き合わせる。

反映待ち側が満たすべき関係は次のとおり。

```
待機の上限 + 期限を跨げる照会 1 件ぶんの応答上限 + job の前後 (checkout と runner の起動) < timeout-minutes
```

「期限を跨げる照会は常に 1 件まで」という上界は、`wait-for-registries.sh` が対象を順に 1 件ずつ照会する巡回構造に依存する。**巡回の構造を変えたらこの式も変わる。**

この関係は、上限に達して失敗するときに対象ごとの分類 (反映済み / 未反映 / 判定不能 / 未照会) を出力するという契約の前提でもある。job の打ち切りが先に来ると、待ちの結果を読み分ける材料が出力ごと消え、レジストリが遅いのか壊れているのかが分からないまま実行が終わる。

## 公開確認

| チャネル | 確認の手段 |
|---|---|
| Maven Central | Portal に公開済みかを返す API が無いため、`repo1.maven.org` への HEAD で代替する (`scripts/release/wait-for-registries.sh`) |
| NuGet.org | flat container の index に当該 version が現れるかを 3 つの Package ID について確認する (同上) |
| SwiftPM | 専用の確認を持たない。smoke が配信リポジトリの tag を解決できることをもって確認とする |

## 保証すること

- Portal の検証を通らない deployment は、不可逆な NuGet push (順序 5) より前に止まる。version を焼かずにやり直せる
- publish 段の各 step は存在検査で冪等であり、失敗しても同じ version で再実行するだけで復旧できる。位置ごとの挙動は [リリース手順](../../../handbook/cross/release-procedure.md) の「失敗したとき」が持つ
- 配信リポジトリの tag が付くのは Maven の release が成功した後だけである。途中失敗で残る未 tag の commit は利用者から解決できない
- 公開待ちは Portal の単発の応答不良 (通信失敗・5xx・状態を取り出せない応答) を吸収し、続けて取り出せなかったときだけ失敗する ([cross/ADR-0031](../../../decisions/cross/0031-query-failure-as-state-value-not-fail.md))
- 状態照会・release・drop と検証の決着待ちは 1 回で失敗する。公開待ちだけが吸収するのは、取り消せない release を送った後に回る位置だから
- deployment ID は attempt をまたいで run の artifact で引き継ぐ。失敗経路で Portal の deployment をどう扱うか (破棄できる状態とできない状態) は [リリース手順](../../../handbook/cross/release-procedure.md) の「失敗したとき」が持つ

## してはいけないこと

- 配信リポジトリ `KsSettingsView-SPM` へ手で commit する: 書き手は CI だけで、ソースと Issue の窓口は monorepo 側にある
- deployment ID の抽出失敗を握り潰す: plugin のログ文言に依存しているため、抽出できなければ失敗させて気づけるようにする
- ローカル pack や mavenLocal への発行が通ったことを、その version が公開レジストリにあることの根拠にする: 公開済みの版は tag と GitHub Release が正

## 関連

- [リポジトリとビルドの責務境界](repository-boundaries.md) — build root の分割、本体検証と消費者検証の役割
- [リリース手順](../../../handbook/cross/release-procedure.md) — 起動・secrets・再実行・リハーサルの操作手順
- [ADR-0018: 配布チャネルと SwiftPM 配信リポジトリ](../../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md)
- [ADR-0019: lockstep の単一バージョン](../../../decisions/cross/0019-lockstep-single-version.md)
- [ADR-0020: 手動起動・tag は最後・version 注入](../../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md)
- [ADR-0029: インストール例は具体 version を持たない](../../../decisions/cross/0029-install-examples-without-pinned-version.md)
- [ADR-0030: Release ノートは pull request 本文から組み立てる](../../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md)
