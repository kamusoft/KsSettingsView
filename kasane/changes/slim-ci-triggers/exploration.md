# Exploration: slim-ci-triggers

## 課題 / 動機

検証 CI の入口 (`.github/workflows/ci.yml`) が `develop` / `main` 宛ての pull request と `develop` への push の両方で 7 job (本体検証 3 本・消費者検証 3 本・lint) を走らせており、`develop` へ反映するだけで同じ検証が 2 回走っていた。実測 (2026-09-04、run 33844204532) では壁時計 14〜20 分、最長 job は消費者検証 MAUI の 14.2 分。1 マージあたり macOS ランナー約 54 分ぶんを消費し、開発効率を著しく下げていた。

開発体制は「外部 PR なし・開発者 1 人・ローカルで `develop` から worktree を切って並列作業し、ローカルで `develop` へマージして push」であり、`develop` 宛ての pull request は運用上存在しない。

現状の図解: 探索時に HTML で作成 (CI トリガー 3 系統、job 構成、develop push の job 実測、再利用 workflow の責務、観察)。

## 検討した選択肢 (却下案と理由を含む)

### トリガーと job の割り当て

| 事象 | いま | 変更後 |
|---|---|---|
| PR → develop | 7 job | トリガー廃止 |
| push → develop | 7 job | lint + ios / android / maui の 4 job |
| PR → main (head は develop のみ) | 7 job | 7 job のまま (消費者検証はここだけ) |
| workflow_dispatch (release) | 4 段 | 変更なし |

- 消費者検証を develop push でも走らせる案: 却下 (配布経路の検査は main 宛て PR とリリースで足りる。壁時計を 2 倍以上に伸ばす)

### develop の branch protection

| 案 | 内容 | 評価 |
|---|---|---|
| 1 (採用) | 必須 status check を全撤去し、force-push 禁止と削除禁止だけ残す | 直接 push 運用と一致。CI は事後検証、失敗は通知で拾う |
| 2 | 必須 check を走る 4 本に絞る | enforce_admins が off なので直接 push は素通りし、実質的な効力がない。却下 |
| (現状) | 7 本のまま | 消費者検証 3 本が develop で二度と走らず「expected」のまま宙に浮く。却下 |

### 実装方式

- 入口 workflow 1 本のまま、消費者検証 3 job に `if: github.event_name == 'pull_request'` を付ける (採用見込み)。job 名を変えないので main の必須 check 名「consumer-xxx / verify」は変わらない
- 入口を develop 用 / main 用の 2 ファイルに割る案: 検討したが、status check 名は job 名で決まるので分ける利点がなく、ADR-0025 の「入口 1 本」に反する

## 決定事項

- トリガーはブランチの役割で分ける (上の表の「変更後」)。ユーザー決定 2026-09-04
- develop の必須 status check は撤去 (案 1)。ユーザー決定 2026-09-04
- main の必須 status check 7 件は維持
- リリース workflow は触らない
- 変更パスによる絞り込みは引き続き行わない (ADR-0025 を維持。絞るのは事象であって変更内容ではない)
- develop への push の concurrency は「ブランチ単位の group + cancel-in-progress: true」にする。現状の group は push では commit SHA なので同じ group に後続が来ず、cancel 設定に関わらず打ち切りは起きない構造だった。ブランチ名 (`github.ref`) を group にすれば連続 push で古い実行を新しい実行が打ち切る。打ち切られた commit の結果は残らないが、最新 push がその commit を含むので壊れていれば最新の実行で失敗する。commit 単位の切り分けはローカルで行う前提。加えてリモートへの push は並列しない運用 (worktree の並列はローカル作業で、develop への push は 1 本ずつ) なので、このトレードオフは実質発生しない (ユーザー確認 2026-09-04)。main 宛て PR は PR 番号 group のまま。ユーザー決定 2026-09-04

## ADR 候補 (作成済み: cross/ADR-0028 (proposed) / 未起票: なし)

ロードマップ: package-distribution の [phase-13-ci-trigger-slimming](../../roadmaps/package-distribution/phases/phase-13-ci-trigger-slimming/agenda.md) として登録済み (phase-3 の決定「トリガー」「必須チェック化と通知」の改訂)。

## 未決の論点

なし (concurrency の扱いは 2026-09-04 に決定事項へ)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S

理由: 触るのは `ci.yml` 1 ファイル (トリガーと job 条件) と GitHub の branch protection (develop の必須 check 撤去)、それに handbook `cross/release-procedure.md` の追従 (「develop の保護設定が正」「必須 check 7 件」の記述)。公開 API に触れず、可逆で、UI もない。

## 実装の見当 (S 級のため tasks の代わり)

1. `ci.yml`: `on.pull_request.branches` から `develop` を外す。consumer 3 job に `if: github.event_name == 'pull_request'` を付ける。concurrency を `group: ci-${{ github.workflow }}-${{ github.event.pull_request.number || github.ref }}` / `cancel-in-progress: true` に変える。冒頭コメントと concurrency のコメントを新しい前提に書き直す
2. GitHub: `develop` の branch protection から `required_status_checks` を外す (完全 payload の PUT。release-procedure.md の手順に倣う)。`main` は変更しない。設定後に読み直して確かめる
3. handbook `cross/release-procedure.md`: main の保護は「develop を写す」ではなく main 独自の 7 件として書き直す。develop の説明 (「検証 CI を通った開発の最新」) を直接 push 運用に合わせる
4. `develop` へ push して 4 job だけが走ること、`main` 宛て PR (次回リリース時) で 7 job が走ることを確認する

## 実装結果 (2026-09-04)

- commit e85cb98 (ci.yml・handbook・保護設定の証跡)。develop の保護設定は evidence/branch-protection-develop.md
- develop への push で起動した run 33849577847: 4 job のみ実行、consumer 3 job は skipped。壁時計 7.7 分 (lint 0.1 / ios 3.3 / android 5.3 / maui 7.6)。変更前の 14〜20 分 × 2 回 (PR + push) から 1 回 7.7 分に短縮
- concurrency の打ち切りは GitHub 標準の挙動で、push が並列しない運用のため実測での確認は行っていない

