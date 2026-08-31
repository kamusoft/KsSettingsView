---
id: 0025
title: 検証 CI は platform 別の再利用可能 workflow と、それを呼ぶ入口で構成する
status: accepted
date: 2026-08-31
---

## Context

公開リポジトリに移行した時点で、pull request と push を機械検査する仕組みが無く、3 platform (iOS / Android / MAUI) のテストと lint はローカル実行に頼っていた。公開後は他環境・他者からの変更も入るため、リポジトリ側で検査を保証する必要がある。

検査を必要とする場面は 2 つある。日常の変更を検証する場面と、リリース時に配布物を作る前に検証する場面である。両者が要求する検査の中身は同じであり、別々に定義すると片方だけが更新されて食い違う。

各 platform は独立したビルドルートを持ち ([ADR-0001](0001-monorepo-platform-build-roots.md))、必要なランナー・ツールチェーン・実行手順が platform ごとに異なる。iOS と MAUI は macOS ランナーと Xcode を要し、Android は Linux ランナーと JDK を要する。

## Decision

検証 CI を、platform 別の**再利用可能 workflow** 3 本と、それらを呼ぶ**入口 workflow** 1 本で構成する。

- platform 別 workflow は再利用可能な形 (`workflow_call`) で定義し、入口を経由せず単独でも呼び出せる契約とする
- 入口 workflow は 3 本を呼び、あわせて lint を実行する
- リリース用の workflow は、検証を再定義せず同じ platform 別 workflow を呼んで再利用する
- 検査対象の絞り込み (変更パスによる実行スキップ) は行わず、変更内容によらず常に全 platform を検証する

status check の名前は、呼び出し側と呼ばれる側の双方で固定する。再利用可能 workflow を呼ぶ job の check 名は「呼び出し側の job 名 / 呼ばれた側の job 名」として現れるため、いずれかが変わるとマージ保護に登録した必須 check が解決されなくなる。

## Alternatives Considered

- **入口 workflow に 3 platform の手順を直接書く** — 却下。リリース用 workflow が同じ検証を必要とするため、手順が 2 箇所に重複し、片方だけが更新されて食い違う
- **変更パスによる絞り込みを入れて、触れた platform だけ検証する** — 却下。必須 check として登録した job が、パスの条件で起動しないと未実行のまま素通りする経路になる。実行時間の節約より、素通り経路を作らないことを優先する
- **platform ごとに独立した入口 workflow を持つ** — 却下。1 つの変更に対する検証結果が複数の入口に分かれ、マージ保護に登録する対象と、開発者が見る「この変更は検証を通ったか」の単位が一致しなくなる

## Consequences

- 正: リリース時の検証が日常の検証と同一の定義を使うため、両者が食い違わない
- 正: platform ごとにランナーとツールチェーンを閉じ込められ、1 platform の要件変更が他に波及しない
- 正: 姉妹ライブラリへ同じ構造を展開するとき、platform 別 workflow の単位で移せる
- 負: 変更が 1 platform に閉じていても常に 3 platform 分の実行時間とランナーを消費する
- 負: status check の名前が 2 つの job 名の組み合わせで決まるため、job 名の変更がマージ保護の設定を壊す。名前を変えるときは保護設定の更新が要る

出典: kasane/roadmaps/package-distribution/phases/phase-3-verification-ci/agenda.md (workflow の構成 / トリガー) / kasane/changes/archive/2026-08-31-add-verification-ci/proposal.md / kasane/changes/archive/2026-08-31-add-verification-ci/specs/verification-ci/spec.md (Requirement: platform workflow の再利用契約)
