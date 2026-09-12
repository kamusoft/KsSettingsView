# 検証結果: backport-release-workflow-hardening (002 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `specs/release-workflow/spec.md` の 4 Requirement / 11 Scenario をすべて実装とテストに突き合わせた。❌ は無い。虚偽チェック・足場の逆流・テスト失敗も無い。

修正サイクル 1 で入った変更 (照会ごとの応答上限の切り詰め / NuGet `versions` の要素型検査 / 順序検査の前後関係 / 自己テストの明示上限) は、いずれも既存の Scenario の実装を強める方向で、対応表の帰属は変わっていない。品質面の所見は `review-002.md` を見ること (対応表の判定とは別軸)。

## 対応表

### MODIFIED / Requirement: Maven Central の公開待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同期に時間がかかっても公開を待ち切る | `scripts/release/central-portal.sh:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS:-5400` = 90 分)、`:47-55` (使い方と分離の理由) | `scripts/release/central-portal.sh:526-529` (PUBLISHING を経て PUBLISHED で成功・照会を繰り返す)。上限値そのものは `scripts/release/check-time-budget.py:106` が 90 分として読み取り lint job で検査 | ✅ 一致 |
| 検証待ちと公開待ちの上限を別々に設定できる | `scripts/release/central-portal.sh:270` (公開待ち) / `scripts/release/central-portal.sh:228` (`KSR_POLL_TIMEOUT_SECONDS:-1800`、検証待ち) | `scripts/release/central-portal.sh:541-548` (検証待ちの上限は公開待ちを打ち切らない / 公開待ちの上限は検証待ちを打ち切らない)。上限分離を元に戻したコピーで 2 件が NG になることを実測 | ✅ 一致 |
| 時間予算が待ちの最悪ケースと応答上限を収容する | `.github/workflows/release.yml:429-443` (内訳のコメントと `timeout-minutes: 240`)、`scripts/release/check-time-budget.py` | `.github/workflows/ci.yml:198-199` (lint job で実行)。実行結果: 合計 217 分 / 上限 240 分 / 余裕 23 分 (exit 0) | ✅ 一致 |

### MODIFIED / Requirement: 公開レジストリへの反映待ち

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未反映は待機を続ける | `scripts/release/wait-for-registries.sh:199` (Maven 404) / `:248-252` (NuGet 404) / `:263` (version 不在)、`:378-397` (巡回の継続) | `scripts/release/wait-for-registries.sh:537-538` / `:554-561` (404・version 不在は未反映)、`:617-630` (未反映のまま巡回が続き、反映後に成功) | ✅ 一致 |
| 照会できなかった場合も待機を続ける | `:192-201` (Maven の分類)、`:237-265` (NuGet の分類)、`:58-62` (5 分類の語彙)、`:378-397` (判定不能でも継続) | `:539-547` / `:562-587` (5xx・通信失敗・JSON 不正・配列でない・null / 数値 / object 混在・ステータス不読) | ✅ 一致 |
| 判定不能から回復すれば反映済みになる | `:331-333` (反映済みは再照会しない)、`:334-337` (分類の更新) | `:633-646` (1 巡目で通信失敗した対象が 2 巡目で反映済みになり、以後 5 件しか照会しない) | ✅ 一致 |
| 対象ごとに状態が分かれて残る | `:290-314` (4 対象を並びの同じ配列で保持)、`:341-352` (対象ごとの出力) | `:596-611` (反映済み / 反映済み / 未反映 / 判定不能 (503) の混在が 4 行に分かれて残る) | ✅ 一致 |
| 上限超過時に対象ごとの最後の状態が分かる | `:390-394` (上限到達時に `print_states stderr` してから失敗) | `:602-613` (上限に達して失敗し、出力に「対象ごとの最後の分類」と種別つき判定不能が載る) | ✅ 一致 (出力の内容に関する所見は `review-002.md` の Major を参照) |

### MODIFIED / Requirement: 保留中 deployment の引き継ぎ

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みが他の成果物の取得より先に行われる | `.github/workflows/release.yml:500-515` (`Download previous deployment id` → `Inspect previous deployment handover` を Android (`:518`) / MAUI (`:523`) の取得より前に配置)、`scripts/release/check-publish-step-order.py` | `scripts/release/check-publish-step-order.py:191-219` (`--selftest` の 7 ケース。実物 + 破壊入力 6 通り)、`.github/workflows/ci.yml:201-207` (lint job で検査と自己テストを実行) | ✅ 一致 |
| 引き継ぎが無い初回の実行と区別できる | `scripts/release/deployment-handover.sh:69-96` (`loaded` / `present` / `deployment-id` の 3 値と読み込み失敗時の exit 1)、`.github/workflows/release.yml:510-515` (`tee -a "$GITHUB_OUTPUT"`) | `scripts/release/deployment-handover.sh:154-198` (引き継ぎあり / 初回 / 空白のみ / 通常ファイルでない / 権限で読めない) | ✅ 一致 |

### ADDED / Requirement: 待機スクリプトの自己テスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:187-188` (`wait-for-registries.sh --selftest`)、`scripts/release/wait-for-registries.sh:407-654` (自己テスト本体) | 自己テスト自身がテスト。実行結果は失敗なし (40 件 OK)。sticky を外したコピーで NG 6 件になることを実測 (退化して常に緑にならないことの確認) | ✅ 一致 |

## 追加検査

### tasks.md

| 項目 | 結果 |
|---|---|
| 虚偽チェック | 無し。チェック済み 15 件はいずれも対応表の実装・テストと突き合わせて実体を確認した |
| 未完了 | 5.2 (`dry-run` で release を起動し、validate から消費者検証までが通ることを確認する) が未チェック・未実施。実際に release を起動しないと満たせない確認で、チェック状態は実態と一致している。扱いはオーナー判断 (公開の不可逆性に関わる項目のため、判定材料として本検証に含めない) |

### 逆流検査

`git log` / `git status` で確認。`proposal.md` と `specs/release-workflow/spec.md` は起票コミット (cfb1d23) 以降に変更が無く、作業ツリーでも未変更。作業ツリーで変更されている change 内のファイルは `tasks.md` のみで、差分はチェック状態だけ。逆流なし。

### 未記録乖離

対応表に ❌ が無いため、未記録乖離の候補は無い。

### 付随修正

`deviation.md` の 2 件はいずれも Requirement を持たないため対応表の対象外。diff にあって Scenario に対応しない変更は、この 2 件 (lint job への `deployment-handover.sh --selftest` と `check-publish-step-order.py --selftest` の追加) だけで、いずれも記録済み。記録のない Scenario 対応外の変更は見つからなかった。

### テストの全件成功

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 失敗なし |
| `scripts/release/wait-for-registries.sh --selftest` | 40 件 OK / 失敗なし |
| `scripts/release/deployment-handover.sh --selftest` | 22 件 OK / 失敗なし |
| `python3 scripts/release/check-time-budget.py` | exit 0 (合計 217 分 / 上限 240 分) |
| `python3 scripts/release/check-publish-step-order.py` | exit 0 |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 OK / 失敗なし |

本 diff は workflow と `scripts/release/` のみに触れ、3 platform の本体コードに変更が無いため、iOS / Android / MAUI のテストスイートは検証対象外とした。

### UI 変更

無し (`ui/` アーティファクトを持たない変更)。
