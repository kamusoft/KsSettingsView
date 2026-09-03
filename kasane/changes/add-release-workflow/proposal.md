# Proposal: add-release-workflow

## Why

3 platform のパッケージング (SwiftPM 配信リポジトリ / Maven Central 発行構成 / NuGet pack) と、配布物を利用者と同じ経路で検証する消費者検証 (dry-run / smoke) は揃ったが、それらを 1 回の手動起動でつなぎ、公開レジストリへ実際に publish して tag と GitHub Release を作る release workflow は存在しない。cross/ADR-0020 (dispatch 起動・publish 全成功後に tag・version は CI が注入) は決定済みで受け口も実装済みだが、workflow 本体・secrets・Central Portal の 2 段操作・失敗時の再実行は未整備であり、初回リリース (`0.1.0-beta.1`) はこの変更で初めて行う。

設計判断はフェーズ議論で決着済み ([agenda](../../roadmaps/package-distribution/phases/phase-8-release-workflow/agenda.md) の決定事項 16 件)。本提案はそれをアーティファクトに落とし、初回リリースの実施までを含む。

## What Changes

- **release workflow の新設** (`.github/workflows/release.yml`、`workflow_dispatch` で version を入力): validate → test (phase-3 の `verify-*.yml` を無改修で呼ぶ) ∥ package (version 注入で配布物を artifact 化) → dry-run (`verify-consumer-*.yml` に artifact を渡す) → publish (直列 1 job、`environment: release`) → 反映待ち → smoke の 6 段。publish 段は SPM スナップショット commit push → Maven upload 保留 (署名 `.asc` の生成確認つき) → NuGet push (Trusted Publishing) → Maven release (Portal API) → SPM tag → monorepo tag + GitHub Release (自動生成ノート、suffix があれば prerelease) の順。全ステップは冪等で、同じ version で「失敗した job から再実行」できる (Maven の deployment ID は run の artifact に保存し、状態で分岐する)。実行はリポジトリ全体で直列化する。失敗時は drop 可能な状態の Maven deployment を drop する
- **validate**: semver (`X.Y.Z(-{alpha|beta|rc}.N)?`) の検査、起動ブランチが `main` であること (`dry-run` 入力によるリハーサル時は免除)、monorepo の同名 tag が別 commit を指していないこと、配信リポジトリの同名 tag の内容が今回のスナップショットと異なっていないこと (不可逆操作の前に検出)、README 2 枚のインストール例が dispatch の version と一致すること
- **Central Portal の 2 段操作**: vanniktech plugin の `publishToMavenCentral` (自動 release なし) で upload し、ログから deployment ID を抽出して後段へ渡す。release は Portal Publisher API (`POST /api/v1/publisher/deployment/<id>`、直前に status で VALIDATED を再確認)。scripts/ に API 呼び出しの shell を置く
- **secrets / Environment**: GitHub Environment `release` (deployment branch policy = `main`、required reviewers なし) に 7 件 (Central Portal の User Token ペア、GPG 鍵・ID・パスフレーズ、nuget.org ユーザー名、SPM の deploy key)。登録はオーナーの手作業で、リリース手順書を handbook (`cross/release-procedure.md`、guide) として同梱する。配信リポジトリへの書き込み用 deploy key の作成と登録を含む
- **`main` ブランチの作成・branch protection・default branch の切替**: `develop` と同じ 7 job 必須の保護を作成と同時に付け、default branch を `main` に切り替える。`main` を base とする PR は head が `develop` のものだけを CI (lint job) が通す (リポジトリのトップと `skills/` のコピー元が最新リリースの状態になり、NuGet 同梱 README と揃う)。README 2 枚の `blob/develop/` (各 7 箇所。`skills/` には無い) の絶対リンクは `blob/main/` に付け替える (docs-refresh 依頼に含める)
- **README のインストール例の version 置換 script** (`scripts/`): README 2 枚の 3 行 (SwiftPM `exact:` / Maven 座標 / NuGet `Version`) を同じ値に置換する。check モードを validate が使う。AGENTS.md の docs-refresh 専任の記述に、この script を例外として 1 行加える
- **MAUI パッケージングの改変**: `maui/nuget.config` (nuget.org のみ + packageSourceMapping、NU1507 の恒久対処)。pack 時に自 assembly 用 aar (`KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar`) を nupkg から除き、生成 aar が推移依存の `.so` 以外を含んだら pack を失敗させる検査を同居させる (XA4301 の恒久対処)。消費者検証 (MAUI) の Release ビルドで XA4301 だけを検出対象に加える
- **`.github/release.yml`**: Release ノートの分類 (最小のラベル構成)
- **初回リリースの実施**: `0.1.0-beta.1` を `main` から dispatch し、3 レジストリからの smoke 成功 (phase-7 で未実証の正ケース) を証跡に残す。事前に docs-refresh (オーナーの明示依頼: 「配信準備中」バナー削除・Maven / NuGet の未公開表記削除・phase-5〜7 の追随) と version 置換 script の実行をリリース PR に含める

影響する能力: release-workflow (新設)、consumer-verification (XA4301 の検出)、verification-ci (`main` の保護と default branch)、maui-nuget-distribution (nuget.config / pack の aar 除外)。あわせて handbook cross にリリース手順 (guide) を追加する

## Non-Goals

- **KsDialogs への逆流** — 本ロードマップの非ゴール。release.yml と scripts をコピーして値を差し替える申し送りを KsDialogs phase-11 の agenda に書くのみ
- **別リポジトリの共有 workflow 化** — agenda 決定 (逆流先の形態が未確定)
- **dotnet/android への upstream 起票** (`CreateAar` が native lib に `Pack` を見ない非対称) — 任意の TODO。本変更の成否に関わらない
- **CHANGELOG ファイルの導入** — agenda 決定 (自動生成ノートを採用)
- **smoke 失敗時の自動ロールバック** — 公開レジストリへの publish は取り消せず、tag も publish 成功で確定する (agenda 決定)。smoke の失敗は workflow の失敗として人が扱う
- **`README_ja` の内容の英日同期** — docs-refresh の責務 (version 置換 script は同じ値の機械置換のみ)

## Impact

- 破壊的変更なし。default branch の切替後は新規 PR の base 既定が `main` になるため、feature PR は `develop` を明示して作る (誤って `main` に向けた PR は head 制限の検査で必須 check が失敗し、マージできない)。ライブラリのコードと公開 API には触れない。MAUI の nupkg の中身は変わる (自 assembly 用 aar が消える) が、その aar は推移依存の `.so` しか含まず利用者が失うものはない (実測済み)
- 公開レジストリへの初回 publish は取り消せない操作 (NuGet は unlist のみ、Maven Central は削除不可)。dry-run (配布物そのもの) → publish の順序と冪等な再実行でリスクを下げる
- secrets の登録・`main` の作成と default branch の切替・Environment の作成・docs-refresh の依頼・dispatch はオーナーの手作業。手順書を change に同梱する
- release 全体の所要時間は 60〜90 分見込み (consumer-maui が dry-run・smoke で各約 20 分、Maven Central の反映待ち 10〜30 分)
- リスク: deployment ID のログ抽出 (plugin の文言依存)、Portal API の公開確認エンドポイント、pack 拡張点での aar 除外、Android 発行物の再ビルド再現性、同じ run の再実行からの artifact download は机上確定のため、tasks の冒頭で実測する (lessons process L-004)。Trusted Publishing の初回 login は事前検証できず本番 run で初めて踏むが、不可逆操作の前の位置にあり失敗しても戻れる
- セカンドオピニオン (second-opinion-spec-001.md) の指摘を反映済み: 配信リポジトリ tag の事前検査、実行の直列化、deployment ID の run をまたぐ保持と状態分岐、`main` の定義と head 制限、semver の先頭ゼロ、NuGet の push 順と 3 Package ID の反映待ち、Android 成果物の同一性検査、`Directory.Build.targets` は既存への追記、コメント規約
- 非公式手段 (pack 拡張点、SDK 内部ターゲット順序) への依存は検査の同居で SDK 更新時に露見させる

## 級: L

設計判断が publish 順序・冪等化・Environment・pack の改変にまたがり、能力も 4 つに及ぶ。初回 publish は取り消せない操作を含み、design.md に Decision と却下案を残す価値がある。

domain: cross
roadmap: package-distribution/phase-8-release-workflow
