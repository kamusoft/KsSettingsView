# 検証結果: backport-release-workflow-hardening (003 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `specs/release-workflow/spec.md` の全 Requirement / Scenario と実装・テストの対応を突き合わせた。❌ は 0 件。未記録の乖離・虚偽チェック・足場の逆流はいずれも無く、自己テストと検査は全件成功する。

## 対応表

### MODIFIED: Maven Central の公開待ち

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 公開を待つ上限を検証の決着待ちと独立に設定できる | `scripts/release/central-portal.sh:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS`) / `:228` (`KSR_POLL_TIMEOUT_SECONDS`) | `central-portal.sh --selftest`「検証待ちの上限は公開待ちを打ち切らない」「公開待ちの上限は検証待ちを打ち切らない」 | ✅ 一致 |
| 公開待ちの上限の既定値は 90 分 | `scripts/release/central-portal.sh:270` (`:-5400`) | `check-time-budget.py` が読み「公開待ち 90 x 1」として出力 | ✅ 一致 |
| publish の実行時間上限が予算表の合計以上 | `.github/workflows/release.yml:443` (`timeout-minutes: 240`) と `:429-442` の内訳コメント | `python3 scripts/release/check-time-budget.py` (合計 217 分 ≤ 240 分)。`.github/workflows/ci.yml:199` で lint job に接続 | ✅ 一致 |
| 各項の定数を変えたときに合計超過を機械検査できる | `scripts/release/check-time-budget.py:104-111` (上限 2 種・巡回間隔・応答上限を `central-portal.sh` から読む) | 同上 + 本検証でのミューテーション 3 通り (下記) | ✅ 一致 |
| Scenario: 同期に時間がかかっても公開を待ち切る | `scripts/release/central-portal.sh:267-300` (上限 5400 秒 = 90 分 > 実測 60 分) | `central-portal.sh --selftest`「PUBLISHING を経て PUBLISHED になれば成功する」「PUBLISHED まで照会を繰り返す」 | ✅ 一致 |
| Scenario: 検証待ちと公開待ちの上限を別々に設定できる | `central-portal.sh:228` / `:270` | 上記の上限分離 2 件 | ✅ 一致 |
| Scenario: 時間予算が待ちの最悪ケースと応答上限を収容する | `check-time-budget.py:121-161` | lint job の `Publish time budget check` | ✅ 一致 |

### MODIFIED: 公開レジストリへの反映待ち

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 照会できたうえでの未反映と、照会が行えなかったことを区別する | `scripts/release/wait-for-registries.sh:191-207` (maven) / `:236-271` (nuget) / `:58-62` (分類の定義) | `wait-for-registries.sh --selftest`「[Maven の分類]」6 件 / 「[nuget.org の分類]」13 件 | ✅ 一致 |
| 区別を対象ごとに独立に保つ (Maven 1 + NuGet 3) | `:295-319` (4 本の並行配列) / `:332-348` | 「混在する巡回で〜が残る」4 件 | ✅ 一致 |
| 判定不能を失敗の種別 3 つまで区別する | `:60-62` (`unknown-transport` / `unknown-status` / `unknown-parse`) / `:274-288` (`state_label`) | 上記分類 19 件のうち種別を見る 8 件 | ✅ 一致 |
| いずれの場合も待機を継続する | `:332-348` (分類を記録するだけで失敗しない) | 「判定不能から回復すれば成功する」「回復する前の巡回では判定不能として出る」 | ✅ 一致 |
| 反映済みは再照会せず、出力はその時点で保持している分類とする | `:336-338` (`continue`) / `:350-362` (`print_states`) | 「反映済みの対象は再照会しない」(照会 7 件) / 「回復後は再照会しない」(照会 5 件) | ✅ 一致 |
| 上限到達時に対象ごとの分類を出力する | `:402-405` | 「上限超過の出力に対象ごとの最後の分類が付く」 | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `:204` (maven 404) / `:253-256` (nuget 404) / `:266-269` | 「404 は未反映」2 件 / 「200 かつ当該 version なしは未反映」 | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `:202` / `:205` / `:197-200` / `:266-269` | 「5xx は判定不能」2 件 / 「通信の失敗は判定不能」2 件 / 「応答を解釈できない」6 件 | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `:332-348` (毎巡回で非反映対象を再分類) | 「判定不能から回復すれば成功する」+「回復後は再照会しない」 | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `:350-362` (4 行出力) | 「混在する巡回で〜」4 件 (反映済み 2 / 未反映 1 / 判定不能 1) | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `:402-405` | 「上限に達すれば失敗する」+「上限超過の出力に対象ごとの最後の分類が付く」+ 種別つきの判定不能 1 件 | ✅ 一致 (※) |

※ 1 巡目の途中で期限が切れると、一度も照会していない対象が初期値のまま「未反映」として並ぶ。spec の分類語彙に「未照会」が無く、既定値では到達しない経路のため乖離としない。詳細と推奨は `review-003.md` の Minor 1 件。

### MODIFIED: 保留中 deployment の引き継ぎ

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みを他の成果物の取得より先に行う | `.github/workflows/release.yml:500` (download) → `:510` (inspect) → `:517` (Android 成果物) | `python3 scripts/release/check-publish-step-order.py` (lint job `:202`) + `--selftest` 7 件 (`:206`) | ✅ 一致 |
| 読み込んだかと引き継ぎが存在したかを出力から区別できる | `scripts/release/deployment-handover.sh:69-96` (`loaded` / `present` / `deployment-id`) / `release.yml:510-516` (`tee -a "$GITHUB_OUTPUT"`) | `deployment-handover.sh --selftest`「[引き継ぎあり]」3 件 /「[初回で引き継ぎなし]」4 件 /「[引き継ぎの読み込み失敗]」4 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | 上記の step 順序 | `check-publish-step-order.py --selftest` の破壊入力 6 通り (読み込みを前後へ移動 / download を後ろへ移動 / 各 step の削除) | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `deployment-handover.sh:74-78` (不在 → `present=false`) / `:79-89` (読めない → `fail`) | 「ファイルが無ければ present=false」「通常のファイルでなければ失敗する」「読み込みに失敗したら loaded=true を出さない」「権限で読めなければ失敗する」「読めないファイルを引き継ぎなしに畳み込まない」 | ✅ 一致 |

### ADDED: 待機スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトが自己テストを持ち、検証 CI で実行される | `scripts/release/wait-for-registries.sh:419-718` (`--selftest`) / `.github/workflows/ci.yml:187-188` | 自己テスト 53 件が lint job の `Registry wait script selftest` で実行される | ✅ 一致 |
| Scenario: 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:184-207` (central-portal / wait-for-registries / deployment-handover / set-readme-version の 4 本 + 検査 3 本) | 本検証で lint job と同じコマンドを全件実行 (すべて exit 0) | ✅ 一致 |

## 追加検査

### tasks.md の虚偽チェック

| 項目 | 結果 |
|---|---|
| 1.1 / 1.2 / 1.3 / 1.4 | `central-portal.sh:270` / 上限分離の検査 2 件 / `release.yml:429-443` / `check-time-budget.py` + ci.yml `:199` を確認。虚偽なし |
| 2.1 〜 2.5 | `wait-for-registries.sh` の分類・保持・出力を確認。虚偽なし |
| 3.1 〜 3.5 | step 順序・`deployment-handover.sh`・順序検査を確認。3.4 の 5 状態 (引き継ぎあり / 初回なし / 読み込み失敗 / 読み込み後に他が失敗 = `writeback "" ""` → keep / 各失敗経路で空上書きしない = 7 状態のループ) がすべて自己テストに存在。虚偽なし |
| 4.1 / 4.2 / 4.3 | 4.1 が挙げる 6 入力は Maven 側 (200 相当の反映済み・404・5xx・通信失敗・解釈不能) と NuGet 側 (200 かつ version あり / なし・404・5xx・通信失敗・解釈不能 5 種) の双方に存在。4.2 の 3 観点も存在。ci.yml `:187-188` を確認。虚偽なし |
| 5.1 / 5.3 / 5.4 | 本検証で全件を再実行して確認 (5.3 の「意図的に定数を壊したときに失敗する」はミューテーション実測で追認) |
| 5.2 (未チェック) | `dry-run` 未実施。チェックも付いていないため虚偽ではない。扱いはオーナー裁定 |

**虚偽チェックなし。**

### 逆流検査 (足場アーティファクトの書き換え)

`git status` / `git diff` で確認。`proposal.md` / `specs/release-workflow/spec.md` / `exploration.md` はいずれも未変更。`tasks.md` の diff は `- [ ]` → `- [x]` のチェック状態のみで、タスク文面の変更なし。**逆流なし。**

### 未記録乖離

対応表に ❌ が無いため、未記録の欠落・乖離は無い。`deviation.md` の `[付随修正]` 2 件 (ci.yml lint job への `deployment-handover.sh --selftest` と `check-publish-step-order.py --selftest` の追加) は Requirement を持たないため対応表の対象外とした。diff にあって Scenario に対応しない変更は、この 2 件のほかに `scripts/release/central-portal.sh:363-368` / `:538-540` の自己テスト上限に関するコメント訂正のみで、これは前回レビュー指摘の修正であり新たな乖離ではない。

### UI 変更

無し (`ui/` ディレクトリを持たない change)。

### テストの全件成功

| 実行 | 件数 / 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 52 件 / 失敗なし |
| `scripts/release/wait-for-registries.sh --selftest` | 53 件 / 失敗なし |
| `scripts/release/deployment-handover.sh --selftest` | 22 件 / 失敗なし |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 / 失敗なし |
| `python3 scripts/release/check-time-budget.py` | exit 0 (合計 217 分 / 上限 240 分) |
| `python3 scripts/release/check-publish-step-order.py` | exit 0 |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | いずれも違反 0 件 |

3 platform (iOS / Android / MAUI) の本体コード・テストに変更が無いため、それぞれのテストスイートは検証対象外と判定した。

### 検査の検出力 (ミューテーション実測)

「検査が存在する」だけでは Scenario を担保したことにならないため、主要 3 経路を実測した。すべて backup からの復元を shasum で照合済み。

| 変異 | 検出 |
|---|---|
| `poll_once` の期限判定 (`can_start_probe` の break) を削除 | `wait-for-registries.sh --selftest` が NG 3 件 |
| ループ末尾の打ち切りを `SECONDS >= deadline` へ戻す | NG 1 件 (「期限後に巡回を始めない」) |
| `request_max_time` を残り時間で切り詰める形へ戻す | NG 2 件 (「期限が目前でも応答上限は変わらない」「上限に達する実行でも照会は応答上限いっぱいを使う」) |
| `central-portal.sh` の公開待ちを 90 → 120 分 (写しに対して) | `check-time-budget.py` が合計 247 分 > 240 分で失敗 |
| `central-portal.sh` の応答上限を 300 → 900 秒 (写しに対して) | 合計 307 分 > 240 分で失敗 |
| `release.yml` の `timeout-minutes` を 240 → 120 (写しに対して) | 合計 217 分 > 120 分で失敗 |

## 判定

全 Requirement / Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、未記録乖離なし、テスト全件成功。**VALID。**
