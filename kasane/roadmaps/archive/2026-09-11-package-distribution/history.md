# package-distribution 改訂履歴

## 2026-08-21: SwiftPM の配信形を配信リポジトリ方式へ (前提の改訂・phase-4 / 7 / 8 / 9 の整合)

phase-2-public-readiness の議論で、SwiftPM 利用者が monorepo を履歴ごと full clone する構造が開発の足場 (検証証跡の媒体) を制約すると判明し、cross/ADR-0018 をルート Package.swift 移設から配信リポジトリ方式へ改訂した。ロードマップ側の反映:

- 前提 / 制約: cross/ADR-0001 への例外の記述を削除し、配信リポジトリ方式と phase-4 / phase-8 の分担を明記。cross/ADR-0021 (新規リポジトリでの public 化) を前提に追加
- 全体図: phase-4 のラベルを「配信リポジトリ + umbrella product」へ
- phase-4 agenda: サマリと論点をルート移設前提から配信リポジトリ前提へ差し替え (samples/ios の参照変更・ルート `.build/` の ignore・ADR-0001 例外の concepts 追随は不要になり削除。配信リポジトリの名前・初期設定・スナップショット生成・書き込み権限を追加)
- phase-7 / 8 / 9 agenda: SwiftPM の dry-run 方式、publish 順序、Package URL の記述を配信リポジトリ前提へ
- フェーズの追加・取り下げ・順序変更はなし

## 2026-08-21: docs フェーズの拡張 — README の二本立てを phase-9 へ、docs 基盤の再編は別ロードマップへ

オーナー要望「ルート README を大幅改訂 (英語 README + README_ja の原典運用を踏襲)、docs-refresh の再編、細かいガイドの Skills 化」を二分割した。README の二本立てと「開発中」表記の外し方は phase-9-docs の論点に追加 (ゴールも追随)。docs-refresh の再編とガイドの Skills 化はセットで、配信ロードマップのゴールから独立したテーマ (配信完了後も続く、複数 change 規模) のため本ロードマップには入れず、別ロードマップとして起案する。非ゴールに明記し、phase-9 がその成果に依存し得る旨を記録。

## 2026-08-21: 依存の追加 — public 化は docs-to-skills ロードマップの完了後

オーナー判断: 公開リポジトリの履歴に旧 `docs/` を一度も載せないため、phase-2 の public 化実施 (手順書 2 節以降) は docs-to-skills ロードマップ (`docs/` 廃止・`skills/` 生成) の完了を待つ。phase-2 の議論と 1 節の下ごしらえは先行可。これに伴い phase-2 の「docs-refresh で `docs/legacy-aiforms-reference.md` を直す」TODO は不要になり削除。前提 / 制約に依存を追記。

## 2026-08-21: 順序・依存の変更 — phase-9-docs を public 化の前へ

オーナー判断: 旧 README も公開履歴に載せないため、phase-9 (README 英語化 + README_ja、インストール手順、移行ガイド) を phase-8 の後ろから phase-2 実施の前へ移動 (docs-to-skills → phase-9 → phase-2 実施)。インストール手順は確定済み識別子 (cross/ADR-0002、android/ADR-0016、maui/ADR-0025、SwiftPM 配信リポジトリ) で未配信の時点で書き、「未配信」の状態表記を phase-8 の初回リリースで解除する (phase-8 agenda に論点追加)。フェーズ一覧の行順と全体図を更新、phase-9 の番号は維持。

## 2026-08-21: フェーズ追加 — Skills 化 (phase-10 / 11 / 12) を本ロードマップ内で扱う

オーナー判断により、docs 基盤の再編 (docs/ 廃止・skills/ 生成・docs-refresh 改修) は別ロードマップに分離せず、本ロードマップのフェーズ追加で扱う (同日の「docs フェーズの拡張」「依存の追加」の記述で「別ロードマップ (docs-to-skills)」としていた部分はこの判断で置き換わる)。末尾採番で phase-10-skills-design (research)、phase-11-docs-refresh-retarget (change)、phase-12-skills-rollout (change) を追加し、実行順は phase-10 → 11 → 12 → phase-9 → phase-2 実施とした (フェーズ一覧の行順と全体図で表現、番号の振り直しなし)。ゴールに Skills 化 (2 言語、manifest 差分更新、docs/ 廃止、cross/ADR-0014 の supersede) を追加、非ゴールに Skill の配布パッケージングと Kasane 側スキルの変更を追加、前提に知識の正・Skill 形式・docs-refresh の位置づけを追加。

## 2026-08-29: 種別変更 — phase-9-docs を research → change

ksn-agenda での議論の結果、phase-9 は調査・意思決定だけで終わらず実装を伴う change を持つことが確定したため、種別を `research` から `change` へ変更した (状態は `in-progress` のまま)。

change に含まれる範囲: 英日 README 2 枚の新規作成 / 旧 README 5 枚 (`android/` `maui/` `samples/*3`) の廃止 / 移送 (MAUI binding 知識・環境セットアップ手順・検証ホスト手順 → concepts、サードパーティ通知 → ルート README) / `.github/` 一式 (Issue Forms 2 本・CONTRIBUTING 英日) / docs-refresh の対象定義変更 (README 8 → 4 枚、デモ画面一覧の照合検査の廃止) / `skills/` の iOS 配布座標の修正。

実行方式を変更フローにしたのは cross/ADR-0022 が「初期生成・構成の見直しは変更フローの承認を通す。docs-refresh は既存構成への追従更新に限定」と定めているため。本フェーズで起票した ADR は cross/0023 (README をルート 2 枚に集約) と cross/0024 (貢献は Issue で受け PR は受け付けない) で、cross/0018 (配信リポジトリ名の確定) と cross/0022 (未公開注記の縛りを削除) にも改訂を入れた。agenda の TODO は research 用の完了マークから `ksn-propose で変更提案を起こす` へ差し替えた。

## 2026-09-04: フェーズ追加 — phase-13-ci-trigger-slimming (CI トリガーを開発運用に合わせて絞る)

初回リリース (phase-8) 後の実測で、`develop` へ反映するだけで検証 CI 7 job (本体 3・消費者 3・lint) が PR と push で 2 回走り (壁時計 14〜20 分、消費者検証 MAUI が 14.2 分で最長)、開発効率を著しく下げていることが分かった。開発体制は「外部 PR なし・開発者 1 人・ローカルで worktree 並列作業 → `develop` へローカルマージして push」であり、phase-3 で決めたトリガー (`develop` 宛て PR + push) と必須チェック化 (`develop` に 7 件) は運用と噛み合っていない。

オーナー判断 (ksn-explore、change `slim-ci-triggers`): `develop` 宛て PR トリガーを廃止、`develop` push は lint + 本体検証 3 本、`main` 宛て PR (head は develop のみ) はそれに消費者検証 3 本を加える。`develop` の必須 status check は撤去 (force-push 禁止・削除禁止は維持)、`main` の 7 件は維持、release workflow は変更なし。`develop` push の concurrency はブランチ単位の group + cancel-in-progress (従来は commit 単位の group で打ち切りが起きない構造だった)。変更パスによる絞り込みを行わない方針 (cross/ADR-0025) は維持し、絞るのは事象であって変更内容ではない。決定は cross/ADR-0028 (proposed) に起票した。

末尾採番で phase-13-ci-trigger-slimming (change、in-progress) を追加し、phase-3 の決定事項「トリガー」「必須チェック化と通知」と phase-8 の `main` 保護設定の記述に改訂注記を入れた。ゴールの「PR / push で検証 CI がある」はブランチの役割に合わせた表現へ改訂。

