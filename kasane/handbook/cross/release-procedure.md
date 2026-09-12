---
kind: guide
applies-when:
  always: false
  tasks: [リリースの実施, release workflow の secrets / Environment の設定, リリースの再実行, リリースのリハーサル]
title: リリース手順
description: main ブランチと branch protection の用意、Environment release と secrets の登録、配信リポジトリの deploy key、事前確認からリリース PR・起動・見守り・公開後の確認までの各段、失敗時の再実行、dry-run によるリハーサル
timestamp: 2026-09-12
---

# リリース手順

この文書は、3 platform (SwiftPM 配信リポジトリ / Maven Central / NuGet.org) へ同じ version を 1 回の操作で公開するまでの手順をまとめる。公開は `.github/workflows/release.yml` を手で起動して行い、その前提となる GitHub 側の設定 (ブランチ・Environment・鍵) はオーナーが手作業で用意する。

コマンド例の `<version>` は `0.1.0` または `0.1.0-beta.1` の形の値に読み替える。リポジトリは `kamusoft/KsSettingsView` (monorepo) と `kamusoft/KsSettingsView-SPM` (SwiftPM 配信リポジトリ) の 2 つを扱う。

## この手順書の使い方

この文書がリリース手順の正であり、1 回のリリースはここを上から順に読めば完了する。

1. **初回だけ行う設定** — 済んでいれば飛ばす
2. **リリースのたびに行うこと** — 事前確認から公開後の確認まで、書かれた順に実行する
3. **失敗したとき** — 実行が止まったときだけ開く
4. **リハーサル** — 配信先を変えずに経路を通したいときだけ開く

リリース用スキル (`.agents/skills/release/SKILL.md`) は、実行時にこの文書を読んで書かれた順に従う薄い層である。段の順序・節の並び・コマンドはいずれもこの文書が持ち、スキルの側には写しを持たない。手順を変えるときはこの文書だけを直せばよい ([cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md))。判断 — version 番号の決定、`## Changes` の最終的な文面、失敗したときに再実行するかどうか — は人が行い、スキルは代行しない。

## ブランチの役割

| ブランチ | 先端が表すもの |
|---|---|
| `develop` | 開発の最新。ローカルの作業ブランチ (worktree) をローカルでマージして直接 push する。push のたびに検証 CI (lint + 3 platform) が事後検証として走り、失敗は通知で拾う |
| `main` | 最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補。リポジトリの default branch |

`main` へ入るのは `develop` からの pull request だけで、それ以外の head は CI の lint job が失敗させる。この pull request では 3 platform の検証と lint に加えて消費者検証 3 本が走り、7 件すべてが `main` の必須 status check になっている。リリースの起動も `main` に限られる。

`develop` には必須 status check も pull request の必須化も付けない (force-push 禁止と削除禁止だけ)。開発者 1 人が直接 push する運用に合わせた設定で、経緯は [cross/ADR-0028](../../decisions/cross/0028-ci-triggers-by-branch-role.md)。

## 初回だけ行う設定

### main の作成と保護

`develop` から `main` を作り、下の保護を付けてから default branch を切り替える。必須 status check は 7 件で、名前は `.github/workflows/ci.yml` の job 名 (再利用 workflow を呼ぶ job は「呼び出し側 / verify」) と一致させる。`develop` の保護は必須 check を持たないので写さない。

```bash
gh api -X POST repos/kamusoft/KsSettingsView/git/refs \
  -f ref=refs/heads/main \
  -f sha="$(gh api repos/kamusoft/KsSettingsView/git/ref/heads/develop --jq .object.sha)"
```

保護は完全な payload を PUT する (`gh api -X PUT` は部分更新にならず、書かなかった項目は消える)。

```bash
gh api -X PUT repos/kamusoft/KsSettingsView/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": {
    "strict": false,
    "checks": [
      { "context": "ios / verify", "app_id": 15368 },
      { "context": "android / verify", "app_id": 15368 },
      { "context": "maui / verify", "app_id": 15368 },
      { "context": "consumer-ios / verify", "app_id": 15368 },
      { "context": "consumer-android / verify", "app_id": 15368 },
      { "context": "consumer-maui / verify", "app_id": 15368 },
      { "context": "lint", "app_id": 15368 }
    ]
  },
  "enforce_admins": false,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 0
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```

`app_id` 15368 は GitHub Actions を指す。これを省くと同名の check を出す他のアプリでも必須が満たせてしまう。設定後に `gh api repos/kamusoft/KsSettingsView/branches/main/protection` を読み直して 7 件が並ぶことを確かめ、default branch を切り替える。

```bash
gh api -X PATCH repos/kamusoft/KsSettingsView -f default_branch=main
```

切替後は新規 pull request の base 既定が `main` になる。feature ブランチからの pull request は base に `develop` を明示して作る。

### 配信リポジトリの deploy key

publish job は配信リポジトリへ commit と tag を push する。書き込み可の deploy key を作り、公開鍵を配信リポジトリへ、秘密鍵を monorepo の Environment secret へ置く。

```bash
ssh-keygen -t ed25519 -C "KsSettingsView release" -N "" -f ./spm-deploy-key
gh repo deploy-key add ./spm-deploy-key.pub \
  --repo kamusoft/KsSettingsView-SPM --title "release workflow" --allow-write
```

秘密鍵はこのあとの secret 登録に使い、登録が終わったら鍵ファイル 2 つを `trash` で消す。

### Environment release と secrets

secrets は Environment `release` にだけ置く。publish job だけがこの Environment を参照し、deployment branch policy が `main` 以外からの参照を拒む。required reviewers は付けない (起動そのものが手動のゲートになっている)。

GitHub の画面で Environment `release` を作り、Deployment branches を `Selected branches` にして `main` を追加してから、次の 7 件を登録する。

| secret | 中身 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal の User Token のユーザー名 |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal の User Token のパスワード |
| `SIGNING_KEY` | GPG 秘密鍵の armored export |
| `SIGNING_KEY_ID` | GPG 鍵の短い ID (`96DB9B8F`) |
| `SIGNING_PASSWORD` | GPG 鍵のパスフレーズ |
| `NUGET_USER` | nuget.org のユーザー名 |
| `SPM_DEPLOY_KEY` | 配信リポジトリの deploy key の秘密鍵 |

鍵の中身はファイルに落とさず、標準入力へ直接流し込む。

```bash
gpg --armor --export-secret-keys 96DB9B8F | gh secret set SIGNING_KEY --env release
gh secret set SPM_DEPLOY_KEY --env release < ./spm-deploy-key
trash ./spm-deploy-key ./spm-deploy-key.pub
```

nuget.org 側には、この monorepo の `release.yml` と Environment `release` を指す Trusted Publisher Policy を登録しておく。publish job は長期の API key を持たず、実行のたびに短命な key を受け取る。

## リリースのたびに行うこと

出す version を決めたら、次の 5 段を上から順に実行する。各段の見出しの下に、その段を終えた状態 (到達状態) を書く。到達していないまま次の段へ進まない。

### 1. 事前確認

到達状態: `develop` の検証 CI が緑で、利用者向けドキュメントの追随の要否が判断できている。

`develop` の先端に対する検証 CI が成功していることを確かめる。

```bash
gh run list --branch develop --workflow=ci.yml --limit 1 \
  --json conclusion,headSha,url --jq '.[0]'
```

失敗しているときは先へ進まない。原因を直して緑にしてから戻る。

`skills/` と README 群が現状から遅れていないかを見て、遅れていれば `docs-refresh` をオーナーが依頼する (このスキルは自発的に発動しない)。追随した更新はこの後のリリース PR に含める。

### 2. リリース PR

到達状態: `develop` → `main` の pull request が 7 件の check を通してマージされ、`main` の先端がリリース対象の commit になっている。

1. `## Changes` に書く材料を集める。範囲は**この pull request が `main` へ新しく持ち込む差分**だけで、前回のリリース以降の全変更ではない (既に `main` へ入った変更は、それを持ち込んだ pull request の記載が既に持っている。両方に書くとノートへ二重に載る)。

```bash
git fetch origin main develop
git log --no-merges --reverse origin/main..origin/develop --pretty=format:'%h %s'
```

2. pull request を `develop` → `main` で作り、本文の `## Changes` セクションに利用者向けの変更だけを書く
3. 7 件の check が通ったらマージする

本文は `.github/pull_request_template.md` の雛形から始める。書式と種別の一覧は雛形の「記入の仕方」が持つ (利用者向けの変更が無いときは `- none` を単独で置く)。**説明は英語で書く** — Release ページは閲覧者の言語圏を仮定しない利用者向けの公開物であり、種別の見出しと定型文言も英語で出る。**このセクションの項目が GitHub Release 本文の材料になる** — 書いた順や字面がそのまま出るのではなく、workflow が項目を種別別に再編し、種別の見出し・pull request 番号・前回の版との比較リンクを付けて整形する。

`## Changes` を持たない pull request、または認識できない行を含む pull request が範囲にあると、release は publish に入る前に止まる ([cross/ADR-0030](../../decisions/cross/0030-release-notes-from-pr-body-and-handbook-as-procedure-source.md))。範囲に入るのは前回の公開済み Release 以降に `main` へマージされた pull request すべてなので、書き忘れは次のリリースのときに露見する。手元で先に確かめるなら、`main` からのリハーサル (下記) を 1 回回す。

### 3. 起動

到達状態: release workflow の run が始まり、その URL が分かる。

`main` の先端がリリース対象の commit になっていることを確かめてから起動する。

```bash
gh workflow run release.yml --ref main -f version=<version>
```

### 4. 見守り

到達状態: 全 job が成功し、tag 2 本・GitHub Release・3 レジストリへの公開・smoke まで終わっている。

```bash
gh run watch "$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

全体で 40 分前後かかる (初回リリース `0.1.0-beta.1` の実測は 39 分。消費者検証 MAUI の dry-run 12 分と publish 11 分が大半で、Maven Central の反映待ちは公式には 10〜30 分かかり得るが初回は数秒だった)。publish は Maven の upload の直後に Central Portal の検証の決着を待ち (上限 30 分)、検証を通らなければ NuGet へ push する前に止まる。検証が長引くぶんは publish の所要時間に上乗せされる。publish が終わると配信リポジトリと monorepo に tag が付き、GitHub Release が作られる。そのあと公開レジストリへの反映を待って smoke が走る。

Release の本文は validate の段で確定済みで、publish は pull request 本文を読み直さない。version が prerelease の表記 (`-beta.1` 等) を持っていても GitHub の prerelease 印は付けず、作った Release を最新として明示的に指定する ([cross/ADR-0029](../../decisions/cross/0029-install-examples-without-pinned-version.md))。

途中で止まったら「失敗したとき」へ。

### 5. 公開後の確認

到達状態: 3 経路の公開物と Release ページを実物で確認できている。

- nuget.org の `KsSettingsView.Maui` のページ (README が表示されること) と、`KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` の公開。README を同梱するのは facade の 1 本だけで、Binding 2 本は間接参照専用のため description だけが出る
- Maven Central の `jp.kamusoft:kssettingsview` の当該 version
- 配信リポジトリの tag と、monorepo の Release 本文
- `https://github.com/kamusoft/KsSettingsView/releases/latest` が今回の版に解決すること (README と利用者向け Skill のインストール例は具体 version を持たず、この案内に委ねている)

Release 本文は pull request の `## Changes` から組み立てられたものであり、手で補わない。文面を直したいときは次のリリースの `## Changes` の書き方を直す。

## 失敗したとき

### publish に入る前 (validate) の失敗

公開物には何も起きていない。原因を直してから同じ version で起動し直す。

| 失敗の理由 | 直し方 |
|---|---|
| 対象の pull request に `## Changes` が無い / 認識できない行がある | ログが原因の pull request と行を示す。その pull request の本文を直してから `Re-run failed jobs` (ノートは実行時の本文を読むので、マージ済みでも直せば反映される) |
| version の形式が不正 | 入力した version を見直す |
| 同名の tag が別の commit を指す | その version は出し直せない。次の version で出す。同名の tag が**起動した commit を指している**場合は衝突ではなく、workflow は再実行として続行する (Release の作成だけが失敗したときなど、同じ version で続けるべき経路) |

`## Changes` の記載の不備は、手元でも確かめられる。`render` は GitHub API も git も使わず、pull request 本文を模した JSON (`[{"number": 42, "body": "..."}]`) だけで組み立てを再現する。

```bash
python3 scripts/release/build-release-notes.py render \
    --pulls <本文を並べた JSON> --repo kamusoft/KsSettingsView --version <version>
```

### publish の途中での失敗

publish の各ステップは冪等なので、原因を取り除いてから **同じ version で「失敗した job から再実行」** する。済んでいる公開は skip され、残りだけが実行される。GitHub の実行画面の `Re-run failed jobs` を使う。`Re-run all jobs` でも成立する (artifact は同名を上書きし、保留中の deployment ID は attempt をまたいで引き継がれる) が、成功済みの本体検証と配布物の生成をやり直すぶん 1 時間近く余計にかかるので、原則として使わない。

| 失敗した位置 | 再実行で起きること |
|---|---|
| 配信リポジトリへの commit push | 差分が無ければ commit を skip して先へ進む |
| Maven の upload | 前の attempt の deployment の状態で分岐する (検証済みなら upload せず release へ。削除済みなら upload をやり直す) |
| Maven の検証待ち (上限超過) | 検証中の deployment は削除できないので ID が残り、次の attempt が同じ deployment の決着を待つところから続ける |
| Maven の検証 (FAILED) | upload からやり直す (NuGet は未 push なので、原因を直せば同じ version で埋め直せる) |
| NuGet の push | 公開済みのパッケージは skip される |
| Maven の release | 保留中の deployment を release する |
| tag / Release | 同じ内容の tag は skip、別内容なら失敗する |

publish が途中で失敗すると、保留中の Maven deployment は失敗経路の後始末で削除され、次の attempt へ引き継ぐ ID も同時に破棄される (再実行は upload からやり直す)。削除できない状態 (検証中か公開処理中) のときは何もせず理由が出て ID もそのまま残るので (次の attempt がその状態を見て続きを行う)、[Central Portal の deployment 一覧](https://central.sonatype.com/publishing/deployments) で状態を見る。手で操作するときは次を使う。

```bash
export MAVEN_CENTRAL_USERNAME=... MAVEN_CENTRAL_PASSWORD=...
scripts/release/central-portal.sh status <deployment-id>
scripts/release/central-portal.sh drop <deployment-id>
```

公開レジストリへ一度出したものは取り消せない (nuget.org は unlist のみ、Maven Central は削除できない)。smoke が失敗しても tag と Release は残したまま、原因を次の version で直す。

## リハーサル

`dry-run` を true にすると publish 以降を行わず、起動ブランチの制限も外れる。配信先の状態は一切変わらないので、workflow を変更したときはこれで validate から消費者検証までを通しておく。

```bash
gh workflow run release.yml --ref <branch> -f version=<version> -f dry-run=true
```

`<version>` には実際に出す予定の値を与える。Release ノートの扱いは起動ブランチで変わる。

| 起動ブランチ | Release ノートの扱い |
|---|---|
| `main` | 対象の収集・検査・整形と成果物への受け渡しまでを本番と同じ経路で行う (Release だけ作らない)。リリース PR のマージ後に一度回すと、`## Changes` の不備で本番の validate が落ちる事態を避けられる |
| `main` 以外 | 収集も検査も行わない。`main` の pull request に紐づかない commit なので、記載が無いことを理由に失敗させない |

## 関連

- [ローカル開発環境と Sample の実行](local-development-setup.md)
- [公開識別子と配布座標](public-identifiers.md)
