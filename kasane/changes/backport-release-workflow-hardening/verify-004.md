# 一致検証: backport-release-workflow-hardening (004 回目)

**日付**: 2026-09-11
**判定**: VALID

デルタスペック `specs/release-workflow/spec.md` の 4 Requirement / 11 Scenario をすべて突き合わせた。❌ は 0 件。

## 対応表

### Requirement: Maven Central の公開待ち (MODIFIED)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 公開待ちの上限を検証の決着待ちと独立に設定できる / 既定 90 分 | `scripts/release/central-portal.sh:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS:-5400`)、宣言は同 `:49-50` | `central-portal.sh --selftest`「検証待ちの上限は公開待ちを打ち切らない」「公開待ちの上限は検証待ちを打ち切らない」 | ✅ 一致 |
| publish の実行時間上限が各項の合計以上 (240 分) | `.github/workflows/release.yml:443` (`timeout-minutes: 240`) と算出根拠のコメント | `python3 scripts/release/check-time-budget.py` (合計 217 / 上限 240) | ✅ 一致 |
| 定数を変えたときに合計超過を機械検査できる | `scripts/release/check-time-budget.py` (release.yml と central-portal.sh の定数を読んで突き合わせる)、`.github/workflows/ci.yml:197` で lint job に接続 | 意図的に定数を壊すと落ちることを tasks 5.3 で確認済み | ✅ 一致 |
| Scenario: 同期に時間がかかっても公開を待ち切る | `central-portal.sh` `cmd_wait_published` (`:270` 以降。PUBLISHED になるまで巡回) | `central-portal.sh --selftest`「PUBLISHING を経て PUBLISHED になれば成功する」「PUBLISHED まで照会を繰り返す」 | ✅ 一致 |
| Scenario: 検証待ちと公開待ちの上限を別々に設定できる | `central-portal.sh:228` (`KSR_POLL_TIMEOUT_SECONDS:-1800`) / `:270` (`KSR_PUBLISHED_TIMEOUT_SECONDS:-5400`) | `central-portal.sh --selftest` の上記 2 件 (互いの上限が相手を打ち切らない) | ✅ 一致 |
| Scenario: 時間予算が待ちの最悪ケースと応答上限を収容する | `scripts/release/check-time-budget.py` | lint job での実行 (`ci.yml:197`)。手元実行で余裕 23 分 | ✅ 一致 |

### Requirement: 公開レジストリへの反映待ち (MODIFIED)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 照会できたうえで未反映と、照会そのものが行えなかったことを区別する | `scripts/release/wait-for-registries.sh:196-213` (`maven_probe`) / `:242-277` (`nuget_probe`)。分類定数は `:58-66` (未照会 `:63` を含む) | `wait-for-registries.sh --selftest` の [Maven の分類] 5 件 / [nuget.org の分類] 11 件 | ✅ 一致 |
| 区別を対象ごとに独立に保つ (Maven 1 + NuGet 3) | 同 `:303-325` (`reset_targets` が 4 対象の並列配列を作る) / `:336-353` (`poll_once`) | 同 [巡回]「混在する巡回で〜が残る」4 件 | ✅ 一致 |
| 判定不能を 3 種別まで区別する | 同 `:64-66` (`unknown-transport` / `unknown-status` / `unknown-parse`)、表示は `:281-293` (`state_label`) | 同 [Maven の分類] / [nuget.org の分類] の判定不能 7 件 | ✅ 一致 |
| いずれの場合も待機を継続する | 同 `:371-379` (`has_unreflected` が反映済み以外をすべて未完了として返す) | 同「判定不能から回復すれば成功する」「未照会が残っていれば待機は続く」 | ✅ 一致 |
| 反映済みの対象は再照会しない / 出力はその時点で保持している分類 | 同 `:341-343` (`poll_once` が反映済みを読み飛ばす) / `:356-367` (`print_states`) | 同「反映済みの対象は再照会しない」(照会 7 件)「回復後は再照会しない」(照会 5 件) | ✅ 一致 |
| 上限に達して失敗するときに対象ごとの分類を出力する | 同 `:406-410` (失敗前に `print_states stderr`) | 同「上限超過の出力に対象ごとの最後の分類が付く」 | ✅ 一致 |
| Scenario: 未反映は待機を続ける | `:208` / `:259` / `:272` (404 と version 不在を `pending` へ) | 「404 は未反映」(Maven / nuget)「200 かつ当該 version なしは未反映」 | ✅ 一致 |
| Scenario: 照会できなかった場合も待機を続ける | `:207` / `:205` / `:253` / `:264` / `:274` (3 種の判定不能)、`:371-379` | 「5xx は判定不能 (応答が成功を示さない)」「通信の失敗は判定不能」「ステータスコードが読めない応答は判定不能」他 | ✅ 一致 |
| Scenario: 判定不能から回復すれば反映済みになる | `:341-343` + `:350-351` (分類の上書き) | 「判定不能から回復すれば成功する」「回復する前の巡回では判定不能として出る」「回復後は再照会しない」 | ✅ 一致 |
| Scenario: 対象ごとに状態が分かれて残る | `:356-367` (`print_states` が 4 行) | 「混在する巡回で Maven の分類が残る」「反映済みの Package ID が残る」「未反映の Package ID が残る」「判定不能が種別つきで残る」 | ✅ 一致 |
| Scenario: 上限超過時に対象ごとの最後の状態が分かる | `:406-410` | 「上限に達すれば失敗する」「上限超過の出力に対象ごとの最後の分類が付く」「上限に達する実行でも 4 対象を 1 巡だけ照会する」 | ✅ 一致 |

未照会 (`unprobed`) の追加は、この Requirement の先頭 SHALL「照会できたうえで未反映であることと、照会そのものが行えなかったことを区別する」に対して、未反映へ畳み込まれていた「照会そのものを行っていない」を分離したもの。判定不能の 3 種別には寄せていないため、種別区別の SHALL とも衝突しない。検査は [巡回の途中で期限に達する] の 7 件 (「照会していない対象 1〜3 は未照会のまま」「未照会は出力でも未反映と区別できる」「照会していない対象を未反映として出さない」「未照会が残っていれば待機は続く」等)。

### Requirement: 保留中 deployment の引き継ぎ (MODIFIED)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 引き継ぎの読み込みを他の成果物の取得より先に行う | `.github/workflows/release.yml:500-517` (`Download previous deployment id` → `Inspect previous deployment handover` → `Download Android artifacts`) | `python3 scripts/release/check-publish-step-order.py` (lint job `ci.yml:200`) + `--selftest` 7 件 | ✅ 一致 |
| 読み込んだか / 引き継ぎが存在したかを出力から区別できる | `scripts/release/deployment-handover.sh:69-96` (`cmd_inspect` が `loaded` / `present` / `deployment-id` を出す)、`release.yml:510-515` が `$GITHUB_OUTPUT` へ写す | `deployment-handover.sh --selftest` [引き継ぎあり] / [初回で引き継ぎなし] / [引き継ぎの読み込み失敗] | ✅ 一致 |
| 引き継ぎの準備判定をスクリプトへ切り出す | `scripts/release/deployment-handover.sh` (`inspect` / `writeback`)、workflow 側は `release.yml:514` / `:887` から呼ぶのみ | `deployment-handover.sh --selftest` 22 件 | ✅ 一致 |
| Scenario: 引き継ぎの読み込みが他の成果物の取得より先に行われる | `release.yml:500-517` | `check-publish-step-order.py` 本体 + `--selftest` (順序を崩した入力で落ちることを検査) | ✅ 一致 |
| Scenario: 引き継ぎが無い初回の実行と区別できる | `deployment-handover.sh:76` / `:92` (`loaded=true present=false`)、読み込み失敗は出力を出さずに失敗 | `deployment-handover.sh --selftest` [初回で引き継ぎなし] / [引き継ぎの読み込み失敗] / [読み込み後に別の成果物の取得が失敗] / [失敗経路の書き戻し判定] | ✅ 一致 |

### Requirement: 待機スクリプトの自己テスト (ADDED)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 待機スクリプトがネットワークに出ない自己テストを持ち、日常の検証 CI で実行される | `wait-for-registries.sh:414-758` (`http_get` をモックへ差し替える `selftest`)、`central-portal.sh` の `--selftest` | 手元実行で 61 件 / 52 件すべて OK | ✅ 一致 |
| Scenario: 待機スクリプトの自己テストが lint job で走る | `.github/workflows/ci.yml:188` (`Registry wait script selftest`)、`:185` (`Central Portal script selftest`)、`:191` (`Deployment handover script selftest`) | lint job の step として登録済み。実行権限は git index 上 `100755` で、`bash` を介さない直接実行が成立する | ✅ 一致 |

## 追加検査

### tasks.md の虚偽チェック

- 5.2 (`dry-run` で release を起動) のみ未チェックで、実態と一致する (未実施)
- 他の 17 タスクはすべてチェック済み。対応表と突き合わせて、実装が存在しないのにチェックされている項目は無い
- 5.3 (定数を壊したときに検査が失敗する) と 5.4 (3.4 の自己テストが 5 状態で通る) は、手元で `check-time-budget.py` / `check-publish-step-order.py --selftest` / `deployment-handover.sh --selftest` が通ることを確認した

### 逆流検査 (足場の書き換え)

```
git diff --stat HEAD -- .../proposal.md .../specs .../exploration.md
→ 差分なし
```

前回レビュー (`review-003.md`、18:47) 以降に更新されたファイルは `scripts/release/wait-for-registries.sh` (18:51) のみ。`tasks.md` は 17:44 で前回以降の更新が無く、`specs/release-workflow/spec.md` は 16:20 (提案確定時) のまま。逆流なし。

### 未記録乖離

対応表に ❌ が無いため、未記録乖離は 0 件。

`deviation.md` の `[付随修正]` 2 件 (`ci.yml` lint job への `deployment-handover.sh --selftest` 追加、`check-publish-step-order.py --selftest` の新設と追加) はいずれも Requirement を持たないため対応表の対象外。diff にあって Scenario に対応しない変更はこの 2 件のみで、両方とも記録済み。

### テストの全件成功

| 検査 | 件数 | 結果 |
|---|---|---|
| `scripts/release/wait-for-registries.sh --selftest` | 61 | 全件成功 (失敗 0) |
| `scripts/release/central-portal.sh --selftest` | 52 | 全件成功 (失敗 0) |
| `scripts/release/deployment-handover.sh --selftest` | 22 | 全件成功 (失敗 0) |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 | 全件成功 (失敗 0) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 | 全件成功 (失敗 0) |
| `python3 scripts/release/check-time-budget.py` | — | 成功 (合計 217 分 / 上限 240 分) |
| `python3 scripts/release/check-publish-step-order.py` | — | 成功 (順序正常) |

`wait-for-registries.sh --selftest` は 5 回連続で 61 OK / 0 NG。時刻に依存する検査の揺れは観測されなかった。

### UI 変更

この change は UI に触れないため、`ui/brief.md` の検査は対象外。

## 判定

**VALID** — 11 Scenario すべてが「✅ 一致」。虚偽チェックなし、逆流なし、未記録乖離なし、テスト全件成功。004 回目も含めて 4 周連続で VALID。
