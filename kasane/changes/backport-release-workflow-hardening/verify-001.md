# 一致検証結果: backport-release-workflow-hardening (001 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `specs/release-workflow/spec.md` の 4 Requirement / 11 Scenario をすべて実装とテストへ突き合わせた。❌ は 0 件。

## 対応表

### MODIFIED Requirement: Maven Central の公開待ち

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 公開を待つ上限を検証の決着待ちと独立に設定できる | `scripts/release/central-portal.sh:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS`)、`scripts/release/central-portal.sh:228` (`KSR_POLL_TIMEOUT_SECONDS` は wait-validated 専用に) | `scripts/release/central-portal.sh:533` / `:538` | ✅ 一致 |
| 公開を待つ上限の既定値は 90 分 | `scripts/release/central-portal.sh:270` (`:-5400`) | `scripts/release/check-time-budget.py:107` が読み取り 90 分として算入 (実行出力「公開待ち 90 x 1」) | ✅ 一致 |
| publish の実行時間上限が各項の合計以上 (表の 150/15/2/30/20/余裕 23 = 240) | `.github/workflows/release.yml:443` (`timeout-minutes: 240`) と同 `:429-442` の内訳コメント | `scripts/release/check-time-budget.py` (`.github/workflows/ci.yml:198` で実行)。実行結果 150/15/2/30/20 = 217、上限 240、余裕 23 で spec の表と完全一致 | ✅ 一致 |
| 各項の定数を変えたときに合計が上限を超えていないことを機械的に検査できる | `scripts/release/check-time-budget.py:96-153` | 定数 3 通りのミューテーション (公開待ち 5400→9000 / `--max-time` 300→600 / `timeout-minutes` 240→120) でいずれも exit 1 を実測 | ✅ 一致 |
| Scenario: 同期に時間がかかっても公開を待ち切る | `scripts/release/central-portal.sh:267-290` (上限 5400 秒 = 90 分 > 60 分) | 上限値としての検証は `check-time-budget.py`。60 分の実待ちは本番リリースでのみ到達 (proposal「リスク」で承知済み) | ✅ 一致 |
| Scenario: 検証待ちと公開待ちの上限を別々に設定できる | `scripts/release/central-portal.sh:228` / `:270` | `scripts/release/central-portal.sh:533`「検証待ちの上限は公開待ちを打ち切らない」/ `:538`「公開待ちの上限は検証待ちを打ち切らない」 | ✅ 一致 |
| Scenario: 時間予算が待ちの最悪ケースと応答上限を収容する | `scripts/release/check-time-budget.py` | `.github/workflows/ci.yml:198` で lint job が実行。合計 217 < 240 で exit 0 | ✅ 一致 |

### MODIFIED Requirement: 公開レジストリへの反映待ち

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 照会できたうえでの未反映と、照会が行えなかったことを区別する | `scripts/release/wait-for-registries.sh:53-58` (5 分類)、`:157-176` (Maven)、`:198-232` (NuGet) | `scripts/release/wait-for-registries.sh:452-499` (Maven 6 件 / NuGet 8 件) | ✅ 一致 |
| 区別を対象ごとに独立に保つ (Maven 1 + NuGet 3 = 4 対象) | `scripts/release/wait-for-registries.sh:263-292` (`TARGET_*` 5 本の並列配列)、`:294-306` | `scripts/release/wait-for-registries.sh:513` / `:515` / `:517` / `:519` (4 対象の分類が混在する巡回) | ✅ 一致 |
| 判定不能を失敗の種別 3 つまで区別する | `scripts/release/wait-for-registries.sh:56-58` (`unknown-transport` / `unknown-status` / `unknown-parse`)、`:234-249` (`state_label`) | `scripts/release/wait-for-registries.sh:462` / `:465` / `:467`、`:485` / `:488` / `:491` / `:494` / `:497` | ✅ 一致 |
| いずれの場合も待機を継続する (照会不能で即座に失敗しない) | `scripts/release/wait-for-registries.sh:294-306` (分類を保持するだけで打ち切らない)、`:333-370` | `scripts/release/wait-for-registries.sh:548`「判定不能から回復すれば成功する」(1 巡目 `000` でも継続) | ✅ 一致 |
| 反映済みと判定した対象は以後再照会しない (sticky) | `scripts/release/wait-for-registries.sh:297-299` | `scripts/release/wait-for-registries.sh:537`「反映済みの対象は再照会しない」(呼び出し 7 件)、`:552`「回復後は再照会しない」(呼び出し 5 件) | ✅ 一致 |
| 上限に達して失敗するときは対象ごとの分類を出力に含める | `scripts/release/wait-for-registries.sh:355-358` | `scripts/release/wait-for-registries.sh:521`。削除するミューテーションで NG になることを実測 | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `scripts/release/wait-for-registries.sh:172` (Maven 404)、`:212` / `:229` (NuGet 404 / version なし) | `scripts/release/wait-for-registries.sh:460` / `:477` / `:483`、`:517` | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `scripts/release/wait-for-registries.sh:165-175`、`:203-222` | `scripts/release/wait-for-registries.sh:465` / `:467`、`:488` / `:491` / `:494` / `:497`、`:519` | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `scripts/release/wait-for-registries.sh:294-306` | `scripts/release/wait-for-registries.sh:548` / `:550` / `:552` | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `scripts/release/wait-for-registries.sh:308-320` (`print_states`)、`:345-346` | `scripts/release/wait-for-registries.sh:513` / `:515` / `:517` / `:519` (反映済み・未反映・判定不能が同じ巡回に並ぶ台本) | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `scripts/release/wait-for-registries.sh:354-359` | `scripts/release/wait-for-registries.sh:511` / `:521` | ✅ 一致 |

### MODIFIED Requirement: 保留中 deployment の引き継ぎ

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みを他の成果物の取得より先に行う | `.github/workflows/release.yml:500` (download) / `:510` (inspect) が `:517` (Download Android artifacts) より前 | `scripts/release/check-publish-step-order.py` (`.github/workflows/ci.yml:201` で実行)。順序を入れ替えたコピーで exit 1 を実測 | ✅ 一致 |
| 読み込んだかどうかと引き継ぎが存在したかどうかを出力から区別できる | `scripts/release/deployment-handover.sh:69-97` (`loaded` / `present` / `deployment-id`)、`.github/workflows/release.yml:510-516` (`tee -a "$GITHUB_OUTPUT"`) | `scripts/release/deployment-handover.sh:154-190` (引き継ぎあり / 初回なし / 空白のみ / 読み込み失敗 2 種) | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `.github/workflows/release.yml:500` / `:510` / `:517`、`:600` (maven step は `steps.handover.outputs.deployment-id` を受け取るだけ) | `scripts/release/check-publish-step-order.py`。step 削除・順序入替の 2 ミューテーションで exit 1 | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `scripts/release/deployment-handover.sh:75-78` (不在は `present=false`)、`:80-86` (種別違い・読めないは exit 1) | `scripts/release/deployment-handover.sh:164-190` | ✅ 一致 |

### ADDED Requirement: 待機スクリプトの自己テスト

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトがネットワークに出ない自己テストを持つ | `scripts/release/wait-for-registries.sh:372-560` (`http_get` だけをモックへ差し替え) | 自己テスト自身 (29 件 OK) | ✅ 一致 |
| その自己テストが日常の検証 CI で実行される | `.github/workflows/ci.yml:187` | lint job の step として存在 | ✅ 一致 |
| Scenario: 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:187` (`wait-for-registries.sh --selftest`)、`:190` (`deployment-handover.sh --selftest`、deviation 記録済みの付随修正) | 手元で `scripts/release/wait-for-registries.sh --selftest` が exit 0、失敗を仕込めば非 0 (ミューテーション 2 種で NG を実測) | ✅ 一致 |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の虚偽チェック | なし。チェック済み 15 件はすべて対応表で実装を確認できた。5.2 (`dry-run` での release 起動) のみ未チェックで、実態と一致している |
| 逆流検査 (足場の書き換え) | なし。`git status` / `git diff` 上、change 配下の変更は `tasks.md` のチェック状態 15 行と新規 `deviation.md` のみ。`proposal.md` / `specs/release-workflow/spec.md` は無変更 |
| 未記録乖離 | なし (対応表に ❌ がない) |
| 付随修正 | `deviation.md` に 1 件記録済み (lint job への `deployment-handover.sh --selftest` 追加)。Requirement を持たないため対応表の対象外。差分は `.github/workflows/ci.yml:190` の 3 行のみで、記録と一致 |
| UI 変更 | なし (`ui/` 無し、UI に触れない変更) |
| テスト全件成功 | 下表のとおり全件成功 |

### 実行したテスト

| コマンド | 件数 / 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 失敗なし (新規 2 件を含む) |
| `scripts/release/wait-for-registries.sh --selftest` | 29 件 OK / 失敗なし |
| `scripts/release/deployment-handover.sh --selftest` | 23 件 OK / 失敗なし |
| `python3 scripts/release/check-time-budget.py` | exit 0 (合計 217 分 / 上限 240 分) |
| `python3 scripts/release/check-publish-step-order.py` | exit 0 |
| `scripts/spm-snapshot/sync-snapshot-test.sh` 等の既存 lint job 項目 | 本 diff の影響範囲外だが `local-path-lint` / `identity-lint` / `comment-policy-lint` は実行し、いずれも禁止 0 件 |

本 diff は `.github/workflows/` と `scripts/release/` のみに触れ、`ios/` `android/` `maui/` の本体コードに変更がないため、3 platform のテストスイートは検証対象外とした。

## 未検証として残る範囲 (spec の要求ではなく、経路の実地確認)

- 90 分の公開待ちと、上限超過時の対象別出力が本番の Central Portal / nuget.org に対して意図どおり動くこと — 待機経路は不可逆な公開の後にしか到達しないため、次回の本番リリースでのみ確認できる (proposal「リスク」で承知済み)
- `dry-run` での release 起動 (tasks 5.2) は未実施。ただし publish job は `dry-run` では走らないため、この change の Requirement のうち `dry-run` で確かめられるものは無い
