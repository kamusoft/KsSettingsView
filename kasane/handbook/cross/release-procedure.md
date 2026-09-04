---
kind: guide
applies-when:
  always: false
  tasks: [リリースの実施, release workflow の secrets / Environment の設定, リリースの再実行, リリースのリハーサル]
title: リリース手順
description: main ブランチと branch protection の用意、Environment release と secrets の登録、配信リポジトリの deploy key、リリース PR と dispatch、失敗時の再実行、dry-run によるリハーサル
timestamp: 2026-09-03
---

# リリース手順

この文書は、3 platform (SwiftPM 配信リポジトリ / Maven Central / NuGet.org) へ同じ version を 1 回の操作で公開するまでの手順をまとめる。公開は `.github/workflows/release.yml` を手で起動して行い、その前提となる GitHub 側の設定 (ブランチ・Environment・鍵) はオーナーが手作業で用意する。

コマンド例の `<version>` は `0.1.0` または `0.1.0-beta.1` の形の値に読み替える。リポジトリは `kamusoft/KsSettingsView` (monorepo) と `kamusoft/KsSettingsView-SPM` (SwiftPM 配信リポジトリ) の 2 つを扱う。

## ブランチの役割

| ブランチ | 先端が表すもの |
|---|---|
| `develop` | 検証 CI を通った開発の最新。すべての feature ブランチのマージ先 |
| `main` | 最新リリース、またはリリース進行中 (リリース PR のマージ後、publish 成功まで) のリリース候補。リポジトリの default branch |

`main` へ入るのは `develop` からの pull request だけで、それ以外の head は CI の lint job が失敗させる。リリースの起動も `main` に限られる。

## 初回だけ行う設定

### main の作成と保護

`develop` から `main` を作り、`develop` と同じ保護を付けてから default branch を切り替える。必須 status check は 7 件で、名前は `develop` の設定が正なので先に読み出して確かめる。

```bash
gh api repos/kamusoft/KsSettingsView/branches/develop/protection
gh api -X POST repos/kamusoft/KsSettingsView/git/refs \
  -f ref=refs/heads/main \
  -f sha="$(gh api repos/kamusoft/KsSettingsView/git/ref/heads/develop --jq .object.sha)"
```

保護は完全な payload を PUT する (`gh api -X PUT` は部分更新にならず、書かなかった項目は消える)。`required_pull_request_reviews` の各値は上で読み出した `develop` の内容に合わせる。

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

### リリース PR

1. `docs-refresh` をオーナーが依頼し、`skills/` と README 群を現状へ追随させる
2. `python3 scripts/release/set-readme-version.py <version>` で README 2 枚のインストール例を新しい version に揃える
3. 1 と 2 を含む pull request を `develop` → `main` で作り、7 件の check が通ったらマージする

version の置換を忘れると release workflow の validate が README の不一致で止まる。手元で `python3 scripts/release/set-readme-version.py --check <version>` を先に通しておくと早く気づける。

### 起動

`main` の先端がリリース対象の commit になっていることを確かめてから起動する。

```bash
gh workflow run release.yml --ref main -f version=<version>
gh run watch "$(gh run list --workflow=release.yml --limit 1 --json databaseId --jq '.[0].databaseId')"
```

全体で 60〜90 分かかる。publish が終わると配信リポジトリと monorepo に tag が付き、prerelease の suffix を持つ version は prerelease として Release が作られる。そのあと公開レジストリへの反映を待って smoke が走る。

### 公開後の確認

- nuget.org の 3 パッケージのページ (README が表示されること)
- Maven Central の `jp.kamusoft:kssettingsview` の当該 version
- 配信リポジトリの tag と、monorepo の Release 本文

前回の tag が無い初回リリースでは自動生成ノートに全 pull request が並ぶので、Release 本文は手で整える。

## 失敗したとき

publish の各ステップは冪等なので、原因を取り除いてから **同じ version で「失敗した job から再実行」** する。済んでいる公開は skip され、残りだけが実行される。GitHub の実行画面の `Re-run failed jobs` を使う。`Re-run all jobs` でも成立する (artifact は同名を上書きし、保留中の deployment ID は attempt をまたいで引き継がれる) が、成功済みの本体検証と配布物の生成をやり直すぶん 1 時間近く余計にかかるので、原則として使わない。

| 失敗した位置 | 再実行で起きること |
|---|---|
| 配信リポジトリへの commit push | 差分が無ければ commit を skip して先へ進む |
| Maven の upload | 前の attempt の deployment の状態で分岐する (検証済みなら upload せず release へ。削除済みなら upload をやり直す) |
| NuGet の push | 公開済みのパッケージは skip される |
| Maven の release | 保留中の deployment を release する |
| tag / Release | 同じ内容の tag は skip、別内容なら失敗する |

publish が途中で失敗すると、保留中の Maven deployment は失敗経路の後始末で削除され、次の attempt へ引き継ぐ ID も同時に破棄される (再実行は upload からやり直す)。削除できない状態 (公開処理が始まっている) のときは何もせず理由が出て ID もそのまま残るので (次の attempt がその状態を見て続きを行う)、[Central Portal の deployment 一覧](https://central.sonatype.com/publishing/deployments) で状態を見る。手で操作するときは次を使う。

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

`<version>` には実際に出す予定の値を与える。README の version 検査もこの実行に含まれるため、リリース PR のマージ後に一度回すと、本番起動で validate が落ちる事態を避けられる。

## 関連

- [ローカル開発環境と Sample の実行](local-development-setup.md)
- [公開識別子と配布座標](public-identifiers.md)
