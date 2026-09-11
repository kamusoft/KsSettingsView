---
id: 0030
title: Release ノートは main 宛て pull request 本文の Changes セクションから組み立て、リリース手順の正は handbook に置く
status: proposed
date: 2026-09-11
---

## Context

GitHub の自動生成ノートは、マージされた pull request をラベルで分類する。`.github/release.yml` はその分類設定 (`breaking` / `feature` / `fix` / `docs` と、`kasane` / `ci` の除外) を持つ。

しかし [cross/ADR-0028](0028-ci-triggers-by-branch-role.md) の運用では、個別の変更は `develop` へ直接 push され、`main` に入るのは集約された `develop` → `main` の pull request だけである。実際に発行済みの `0.1.0-beta.2` のノートに並んでいるのは集約 pull request 2 件のみで、どちらも分類されずに「その他」へ落ちている。設定が参照する 6 ラベルはリポジトリに 1 件も存在しない。

ラベルを作っても分類できるのは集約 pull request の粒度でしかなく、集約 pull request に `kasane` を付ければ、その中に含まれる利用者向けの変更ごとノートから消える。分類の単位と変更の単位が噛み合っていない。

リリース手順そのものは handbook `cross/release-procedure.md` にあるが、それを定型的に実行する道具は無く、オーナーが手順書を見ながら都度行っている。

## Decision

### Release ノートの出所

- Release ノートは、**`main` 宛て pull request の本文に書かれた `## Changes` セクション**から組み立てる。
- 対象は、**直前の GitHub Release に対応する tag** と今回のリリース対象 commit の間に入った commit に紐づく pull request のうち、**`main` を base とするもの**に限る。
  - 起点は、**今回の version ではなく、draft でない公開済みの Release のうち、その tag が `main` の first-parent 上で対象 commit の祖先であり、対象 commit からもっとも近いもの**の tag とする。Release を伴わない tag、別の枝の Release、draft はいずれも起点にしない。今回の version の tag が既に存在する再実行でも、今回の version を除くため起点は変わらない。
  - 条件を満たす Release が 1 つも無い場合 (初回のリリース) は、履歴の最初から今回の commit までを範囲とする。
  - base を `main` に限るのは、commit への紐づけが base を問わないため。`develop` 宛ての pull request まで拾うと、集約 pull request と二重に載る。
- 取得・解析・整形は publish より前の段で一度だけ行い、その結果を publish へ受け渡す。publish は pull request 本文を読み直さない。検査を通した本文と実際に公開されるノートを同一にするため。
- セクションの各行は `- <種別>: <説明>` とし、種別は `breaking` / `feature` / `fix` / `docs` の 4 つ。
- 開発ハーネスの作業と CI の整備は、行を書かないことでノートから外す。ラベルによる除外機構は持たない。
- セクションは必須とする。利用者向けの変更が無い pull request は `- none` と明示する。セクションを持たない pull request が範囲に含まれる場合、release は止まる。
- 組み立ては release workflow が行い、ノートの末尾に比較リンクを付ける。認識できない入力は黙って無視せず失敗させる。
- 公開を伴わないリハーサル (dry-run) では、`main` から起動した場合に限り収集・検査・受け渡しまで行い、それ以外のブランチから起動した場合は収集と検査を行わない。起動ブランチの制限が外れているため (cross/ADR-0020) `main` の pull request に紐づかない commit から起動されうる一方、`main` からのリハーサルでは実際の経路を通しておく必要がある。
- `.github/release.yml` (自動生成ノートの設定) は廃止し、分類用のラベルは作らない。

### リリース手順の正とスキルの役割

- リリース手順の正は handbook `cross/release-procedure.md` に置いたままとする。
- リリース用スキル (`.agents/skills/release/`、`.claude/skills/release` は symlink) は、**実行時に handbook の単一の入口を読み、そこに書かれた順序とコマンドに従う**。段の順序・節の並び・コマンドはいずれも handbook が所有し、スキル側に書き写さない。
- スキル固有の内容は、`## Changes` の下書き生成の手順と、判断を人へ返す境界に限る。handbook のどの節をどの順に読むかはスキルが持たない (それ自体が順序の複製になるため)。
- スキルが担う範囲は、事前確認 (`develop` の CI の状態、docs-refresh の依頼の要否)・**これから作る pull request が `main` へ持ち込む差分**からの `## Changes` の下書き生成・リリース pull request の作成・release の起動・実行の見守りと失敗時の handbook 該当箇所の案内・公開後の確認項目の実行。下書きの範囲を直前のリリース以降の全変更にすると、既に `main` へ入った pull request の記載と二重に載る。
- スキルは判断を担わない。version 番号の決定、`## Changes` の最終文面、失敗時に再実行するかどうかは人が決める。

## Alternatives Considered

- **ラベルによる分類を維持し、6 ラベルを作成する**: 却下。分類できるのは集約 pull request の粒度でしかなく、`kasane` / `ci` による除外は集約 pull request ごとノートから落とす。分類の単位が変更の単位と噛み合わない。
- **個別の変更を pull request 化し、ADR-0028 を改訂する**: 却下。ラベル分類は成立するようになるが、日常の開発フロー全体 (`develop` への直接 push) を変える代償が、ノートの分類のためだけには大きい。
- **commit メッセージまたは変更のメタデータからノートを生成する**: 却下。commit の粒度は利用者向けの変更の単位と一致せず、ハーネス作業と利用者向け変更の切り分けを機械的に決められない。
- **直近のリリース pull request 1 本の本文だけを見る**: 却下。tag 間に `develop` → `main` の pull request が複数入りうる (`0.1.0-beta.2` の範囲に 2 件入った実例がある)。他の pull request が `main` に入った瞬間に静かに取りこぼす。
- **`main` への pull request をリリース pull request 1 本に限る運用にする**: 却下。取りこぼしは消えるが、リリース以外の理由で `main` を触れなくなる。
- **`## Changes` セクションを任意にする**: 却下。セクションが無い場合に「書き忘れ」と「利用者向けの変更が本当に無い」を区別できない。
- **スキルを手順の正とし、handbook をポインタに縮める**: 却下 (オーナー判断 2026-09-11)。実行と記述が一致するという利点はあるが、採らない。

## Consequences

- 正: ノートに載る内容を人が明示的に決められる。GitHub の分類推測に依存しない。
- 正: ブランチ運用 (ADR-0028) を変えずにノートの分類が成立する。
- 正: リリース手順が定型的に実行できるようになり、手順書を目で追う作業が減る。
- 負: pull request を作るたびに `## Changes` を書く手作業が増える。スキルによる下書き生成でこれを相殺する。
- 負: release workflow に「pull request 本文を取得して解析する」という新しい責務が増える。
- 負: 手順の正が handbook にある以上、スキルは handbook の更新に追随する必要がある。追随が漏れるとスキルが古い手順を実行しうる。
- 負: セクションを必須にしたため、書き忘れた pull request が範囲にあると release が止まる。

## Revisit When

- `main` への取り込み単位が変わったとき (個別の変更が pull request として `main` に入るようになる、あるいは `develop` を経由しなくなる) — 集約 pull request を前提とした `## Changes` の粒度が合わなくなる
- ブランチ運用 (cross/ADR-0028) が改訂されたとき
- `## Changes` の記入漏れによる release の停止が繰り返し起きたとき — 必須とする判断か、下書き生成の実効性を見直す

出典: kasane/changes/install-examples-and-release-notes/exploration.md
