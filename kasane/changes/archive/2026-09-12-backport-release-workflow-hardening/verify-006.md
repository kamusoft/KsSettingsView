# 一致検証: backport-release-workflow-hardening (006 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `kasane/changes/backport-release-workflow-hardening/specs/release-workflow/spec.md` の 4 Requirement / 11 Scenario を実装と突き合わせた。前回 (005 回目) 以降の変更は `scripts/release/check-time-budget.py` への反映待ち系統の追加と `.github/workflows/ci.yml` の lint job への接続の 2 箇所で、影響するのは Requirement「Maven Central の公開待ち」の Scenario「時間予算が待ちの最悪ケースと応答上限を収容する」のみ。11 Scenario すべてが ✅ 一致で、❌ は 0 件。

## 対応表

### Requirement: Maven Central の公開待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同期に時間がかかっても公開を待ち切る | `scripts/release/central-portal.sh:267` `cmd_wait_published` (上限 `KSR_PUBLISHED_TIMEOUT_SECONDS` 既定 5400 秒 = 90 分、`:270`) | `central-portal.sh --selftest` の `wait-published` 群 (52 件に含まれる) | ✅ 一致 |
| 検証待ちと公開待ちの上限を別々に設定できる | `central-portal.sh:228` (`KSR_POLL_TIMEOUT_SECONDS` 既定 1800) / `:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS` 既定 5400) の 2 変数 | 同 selftest (`:536` / `:543` / `:547` — 一方の 0 指定が他方に効かないことを含む) | ✅ 一致 |
| 時間予算が待ちの最悪ケースと応答上限を収容する | `scripts/release/check-time-budget.py:139` `check_publish` (定数を読んで合計を算出) + `.github/workflows/release.yml:443` `timeout-minutes: 240` | `.github/workflows/ci.yml:198` で lint job が実行 (実測: 合計 217 分 / 上限 240 分)、`:201` で `--selftest` 12 件 | ✅ 一致 |

デルタスペックの予算表 (150 / 15 / 2 / 30 / 20 / 余裕 23 / 合計 240) と検査の出力が項目単位で一致することを実行して確認した。今回 `publish_timeout_minutes` が `job_timeout_minutes(text, job, errors)` (`check-time-budget.py:96`) へ一般化されたが、`publish` job の範囲 (`release.yml:414`〜) に入る 4 桁字下げの `timeout-minutes` は `:443` の 240 のみで、読み取り値は変わっていない。

### Requirement: 公開レジストリへの反映待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未反映は待機を続ける | `scripts/release/wait-for-registries.sh:208` / `:259` (404 → `STATE_PENDING`)、`:370` `has_unreflected` | `wait-for-registries.sh --selftest` (61 件) の分類表と巡回検査 | ✅ 一致 |
| 照会できなかった場合も待機を続ける | `:206` / `:247` / `:252` / `:263` / `:273` (`unknown-transport` / `unknown-status` / `unknown-parse` の 3 種別) | 同 selftest の Maven / NuGet 分類表 | ✅ 一致 |
| 判定不能から回復すれば反映済みになる | `:337` `poll_once` (反映済みの対象を再照会しない) | 同 selftest (回復・再照会なしの検査) | ✅ 一致 |
| 対象ごとに状態が分かれて残る | `:306` `reset_targets` + `TARGET_STATE` 配列 (Maven 1 + NuGet 3 = 4 対象)、`:278` `state_label` | 同 selftest (混在巡回の検査) | ✅ 一致 |
| 上限超過時に対象ごとの最後の状態が分かる | `:408` 「対象ごとの最後の分類」の出力 (種別つき)。この出力が job の打ち切りに先行することは `check-time-budget.py:188` `check_registry_wait` が検査する (`release.yml:929` `timeout-minutes: 60` に対し上界 52 分) | 同 selftest `:659`、および `check-time-budget.py --selftest` の反映待ち 4 ケース | ✅ 一致 |

最後の Scenario は前回まで待機スクリプト側だけで担保されていたが、今回 job の打ち切りが先に来ないことの機械検査が加わった。spec 本文は反映待ち job の時間予算を規定していないため、この検査は Scenario の要求を上回る補強であり、`deviation.md` にオーナー判断として記録済み。

### Requirement: 保留中 deployment の引き継ぎ

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みが他の成果物の取得より先に行われる | `.github/workflows/release.yml:500` `Download previous deployment id` → `:510` `Inspect previous deployment handover` → `:517` `Download Android artifacts` の並び。機械検査は `scripts/release/check-publish-step-order.py` | `check-publish-step-order.py` (`ci.yml:204`、実測「順序正常」) と `--selftest` 7 件 (`ci.yml:209`) | ✅ 一致 |
| 引き継ぎが無い初回の実行と区別できる | `scripts/release/deployment-handover.sh:73` `cmd_inspect` (`loaded` / `present` / `deployment-id` の 3 キー。読み込み失敗は出力を出さず失敗) | `deployment-handover.sh --selftest` の `inspect` 側 11 件 (引き継ぎあり 3 / 初回なし 4 / 読み込み失敗 4) | ✅ 一致 |

今回の変更はこの Requirement に触れていない。前回検証時の所見 (`cmd_writeback` (`deployment-handover.sh:103`) と `Drop pending deployment` (`release.yml:872`) が後始末側にあたり直接対応する Scenario を持たないこと) はそのまま維持されている。

### Requirement: 待機スクリプトの自己テスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:187` `Registry wait script selftest` (`:184` Central Portal / `:190` Deployment handover / `:193` Install example version と並ぶ) | 自己テスト自身 (`wait-for-registries.sh --selftest` 61 件) | ✅ 一致 |

step の改称・追加は `:198` / `:201` (時間予算の検査とその自己テスト) に限られ、この Scenario が名指しする `Registry wait script selftest` の step は位置も内容も変わっていない。

## 追加検査

- **tasks.md の虚偽チェック**: なし。`[x]` の 15 項目はいずれも対応表の実装と一致する。`5.2` (`dry-run` で release を起動) のみ未チェックで、実態 (未実施) と一致している。`1.4` の文面が求めるのは publish 側の突き合わせを lint job で走らせることまでで、今回追加された反映待ち側の検査はその上乗せにあたる (`deviation.md` に記録済み)
- **逆流検査**: `git status` で `specs/release-workflow/spec.md` / `proposal.md` / `exploration.md` はいずれも HEAD との差分なし (変更ありは `tasks.md` のみ)。逆流なし
- **未記録乖離**: ❌ が 0 件のため該当なし
- **付随修正**: `deviation.md` の `[付随修正]` 2 件 (`ci.yml` への `deployment-handover.sh --selftest` 追加、`check-publish-step-order.py --selftest` 追加) は Requirement を持たない記録済みの同梱で、対応表の対象外。今回追加された `check-time-budget.py` の反映待ち系統と `ci.yml` の `Release time budget check selftest` は、`deviation.md` の 3 件目 (オーナー判断による spec 上乗せ) として記録済みであり、未記録乖離ではない
- **UI 変更**: なし (該当なし)
- **テストの全件成功**: 実行して確認した (下表)

## テスト実行結果

| 検査 | 結果 |
|---|---|
| `python3 scripts/release/check-time-budget.py` | `[publish]` 217 分 / 上限 240 分、`[wait-for-registries]` 52 分 / 上限 60 分 (終了コード 0) |
| `python3 scripts/release/check-time-budget.py --selftest` | 12 件 OK / 0 件 NG (終了コード 0) |
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 0 件 NG (終了コード 0) |
| `scripts/release/wait-for-registries.sh --selftest` | 61 件 OK / 0 件 NG (終了コード 0) |
| `scripts/release/deployment-handover.sh --selftest` | 21 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/release/check-publish-step-order.py` | 順序正常 (終了コード 0) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 OK / 0 件 NG (終了コード 0) |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 検査対象 787 ファイル (終了コード 0) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` | いずれも終了コード 0 |

`check-time-budget.py --selftest` の 12 件は基準ケース 1 件 (両系統とも収まる) と破壊ケース 11 件 (publish 側 4 / 反映待ち側 3 / 読み取りの退化 4) の内訳。件数の増加 (前回まで 0 件 — 自己テスト自体が新設) に見合う検出力があることは、ミューテーションで実測して確認した (`review-006.md` に記録)。
