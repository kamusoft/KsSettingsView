---
id: 0028
title: 検証 CI のトリガーはブランチの役割で分け、develop は直接 push、main は develop からの PR だけを検証する
status: accepted
date: 2026-09-04
---

## Context

検証 CI の入口 ([ADR-0025](0025-verification-ci-reusable-platform-workflows.md)) は、`develop` / `main` 宛ての pull request と `develop` への push の両方で起動し、いずれも本体検証 3 本 (iOS / Android / MAUI)・消費者検証 3 本・lint の 7 job を走らせていた。`develop` と `main` の branch protection はこの 7 job を必須 status check として登録していた。

しかし、このリポジトリの開発体制は次の通りで、上の構えとは噛み合っていない。

- 外部からの pull request は受け付けず ([ADR-0024](0024-contributions-via-issues-no-external-pull-requests.md))、開発者は 1 人
- 開発はローカルで `develop` から worktree を切って並列に進め、ローカルで `develop` へマージして push する。`develop` 宛ての pull request は本来この運用に存在しない
- `main` へ入るのは `develop` からの pull request だけ (lint job が head を検査する)

実測 (2026-09-04 の `develop` への push、run 33844204532) では 7 job の壁時計が 14〜20 分で、そのうち消費者検証 MAUI が 14.2 分と最長を決めていた。加えて pull request と push で同じ 7 job が 2 回走るため、1 マージあたり macOS ランナー約 54 分ぶんを消費し、開発の待ち時間が著しく長くなっていた。

消費者検証 3 本は「配布物が利用者と同じ経路で解決できるか」を見るもので、本体のコード変更ごとに繰り返す意味は薄く、リリース候補が `main` へ入る時点で確かめれば足りる。

## Decision

検証 CI のトリガーをブランチの役割で分ける。

| 事象 | 走る job |
|---|---|
| `develop` への push | lint + 本体検証 3 本 (iOS / Android / MAUI) |
| `main` 宛ての pull request (head は `develop` に限る) | 上の 4 本 + 消費者検証 3 本 (dry-run) |
| リリース (workflow_dispatch) | 変更なし |

- `develop` 宛ての pull request トリガーは廃止する
- `develop` の branch protection から必須 status check を撤去する。force-push 禁止と削除禁止は残す。`develop` の CI は直接 push に対する事後検証として動き、失敗は通知で拾う
- `main` の branch protection は 7 件の必須 status check を維持する
- 消費者検証 3 job は入口 workflow の中で事象による条件付けとし、job 名は変えない (status check 名を固定する [ADR-0025](0025-verification-ci-reusable-platform-workflows.md) の規律を保つ)
- `develop` への push の同時実行はブランチ単位でまとめ、新しい push が走行中の古い実行を打ち切る。`develop` の検証は「最新の状態が通るか」の事後検証であり、追い越された中間 commit の結果を残す必要はない (中間 commit の切り分けはローカルで行う)
- 変更パスによる絞り込みを行わない方針 ([ADR-0025](0025-verification-ci-reusable-platform-workflows.md)) は `main` 宛ての pull request では変えない。`develop` への push に限り、必須 status check を持たないため「素通り経路を作らない」という理由が成り立たず、ビルド・テスト・lint のどれにも入力されないファイル (`kasane/**`・Issue テンプレート・貢献案内) だけの push では起動しない。README と `skills/` は lint の入力なので除外しない

## Alternatives Considered

- **`develop` の必須 status check を走る 4 本に絞って残す** — 却下。開発者は管理者で、管理者への強制 (enforce_admins) は off のため、直接 push は必須 check の有無によらず通る。設定は整合するが実質的な効力がなく、保護設定の見かけと運用が食い違う
- **`develop` の必須 status check 7 本をそのまま残す** — 却下。消費者検証 3 本は `develop` では二度と走らないため、必須 check が「expected」のまま宙に浮く不整合になる
- **`develop` への push を打ち切らず完走させる (従来の設定を維持する)** — 却下。従来の設定は group が commit 単位で、そもそも打ち切りが起きない構造だった。worktree 並列で push が連続すると全部が完走して macOS ランナーを重ねて消費する。最新の push が古い commit を含むため、完走させても検出力は増えない
- **`develop` への push でも変更パスによる絞り込みを一切入れない (ADR-0025 をそのまま適用する)** — 却下。ADR-0025 が絞り込みを退けた理由は必須 status check の素通りだが、`develop` は必須 check を持たなくなった。開発ハーネスの記録だけの push で macOS ランナーが 2 本起きるのは無駄で、除外対象を「どの検査の入力にもならないファイル」に限れば検出力は落ちない
- **消費者検証も `develop` への push で毎回走らせる** — 却下。消費者検証は配布経路の壊れを検出するもので、本体のコード変更ごとに繰り返す価値が低い。壁時計を 1 本で 2 倍以上に伸ばす (14.2 分) 一方、リリース候補が `main` へ入る時点とリリース本番 (dry-run + smoke) で同じ検査が走る

## Consequences

- 正: `develop` へのマージ 1 回あたりの CI が「7 job × 2 回」から「4 job × 1 回」になり、壁時計は約 6.5 分、macOS ランナー消費は約 54 分から約 12 分に下がる
- 正: 保護設定と実際の運用 (直接 push、PR は `main` 宛てだけ) が一致し、設定を読んだ人が運用を誤解しない
- 正: 開発ハーネスの記録だけの push では CI が起動しない
- 正: 消費者検証は `main` 宛て PR とリリースの 2 箇所に集約され、配布経路の検査がリリースの前に必ず 1 回は走る
- 負: `develop` へ push が連続すると中間 commit の CI 結果は残らない。壊した commit の特定はローカルで行う。ただしリモートへの push は並列しない運用 (worktree の並列はローカル作業に閉じる) のため、通常は発生しない
- 負: `develop` の検証は事後になる。壊れた commit が `develop` に載り得るため、失敗通知を見て直す運用が前提になる (ローカルの pre-commit / pre-push hook が lint 相当を先に掛けている)
- 負: 除外対象に新しい検査の入力が入ると、その検査が `develop` で走らない経路ができる。除外リストを広げるときは各 lint の入力を確かめる
- 負: 開発体制が変わって外部や複数人からの pull request を `develop` で受けるようになったら、この決定は見直しが要る
- 負: `main` への取り込み制限 (head が `develop` であること) は入口 workflow の lint job 内の検査なので、`main` 宛ての pull request 自身が workflow 定義を書き換えれば無効化できる。起動できるのは collaborators だけ ([ADR-0024](0024-contributions-via-issues-no-external-pull-requests.md)) で、意図的に検査を消す pull request は運用上の逸脱として扱うことにし、Ruleset や `pull_request_target` 化は採らない (残存リスクとして受容)
- (2026-09-04 追記、出典: 実装結果) `develop` への push の実測は壁時計 7.7 分 (lint 0.1 / iOS 3.3 / Android 5.3 / MAUI 7.6 分。消費者検証 3 job は skipped)。壁時計は MAUI の本体検証に張り付く。変更パスの除外は、workflow 定義を含む push では起動し、開発ハーネスの記録だけの push では起動しないことを run 一覧で確認した

出典: kasane/changes/archive/2026-09-04-slim-ci-triggers/exploration.md (検討した選択肢・決定事項・実装結果) / kasane/changes/archive/2026-09-04-add-release-workflow/deviation.md (Requirement「マージ保護」の受容) / kasane/roadmaps/package-distribution/phases/phase-13-ci-trigger-slimming/agenda.md (TODO の実測)
