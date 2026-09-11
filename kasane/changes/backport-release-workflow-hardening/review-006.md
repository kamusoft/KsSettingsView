# レビュー結果: backport-release-workflow-hardening (006 回目)

**日付**: 2026-09-11
**判定**: APPROVED

## サマリー

前回 (005 回目) 以降の変更は `scripts/release/check-time-budget.py` への反映待ち系統の追加と、その自己テストを `.github/workflows/ci.yml` の lint job へ繋いだ 2 箇所。publish 側の予算表は数値・内訳ともに変わっておらず (合計 217 分 / 上限 240 分 / 余裕 23 分)、`publish_timeout_minutes` → `job_timeout_minutes(text, job, errors)` への一般化も job 範囲の特定が正しく効いている。反映待ちの上界の式 (待機の上限 + 応答上限 1 件ぶん + job のオーバーヘッド) は `wait-for-registries.sh` の実装構造と対応しており、ミューテーションで自己テスト 12 件の検出力も実測した。Critical / Major に相当する実害は無い。

これは最終確認のレビューであり、新規 Suggestion の掘り起こしは行っていない。既存の未修正 Suggestion 4 件はオーナー報告事項として据え置く方針を前提とし、判定の材料にしていない。指摘は下記 Minor 2 件のみで、いずれも判定を変えない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always — コメント構文を持つ全ソース) — 新規コメント (`check-time-budget.py` の module docstring・定数の説明・`check_registry_wait` の docstring、`ci.yml` の step 前コメント) を照合。デルタスペック構文キーワード・変更識別子・通番・作業文書のパスの混入は無い。ただし module docstring に文書名の裸参照が 1 件ある (指摘 1)
- `kasane/handbook/cross/test-execution.md` (適用のきっかけ: テスト実行・テスト結果の報告) — 実行件数の併記に従い、下表へ件数を明記した
- `kasane/lessons/code-review.md` の重点観点 [L-001] — 自己テストの検出力をミューテーションで実測する手法を今回も適用した (下記「検出力の実測」)

## 実行結果

lint job が回す検査を手元で全件実行し、すべて成功 (終了コード 0)。

| 検査 | 結果 |
|---|---|
| `python3 scripts/release/check-time-budget.py` | `[publish]` 合計 217 分 / 上限 240 分 (余裕 23 分)、`[wait-for-registries]` 合計 52 分 / 上限 60 分 (余裕 8 分) |
| `python3 scripts/release/check-time-budget.py --selftest` | 12 件 OK / 0 件 NG |
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 0 件 NG |
| `scripts/release/wait-for-registries.sh --selftest` | 61 件 OK / 0 件 NG |
| `scripts/release/deployment-handover.sh --selftest` | 21 件 OK / 0 件 NG |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG |
| `python3 scripts/release/check-publish-step-order.py` / `--selftest` | 順序正常 / 7 件 OK・0 件 NG |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 787 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` | 検出なし / 検出なし / 最小例 4 件一致 |

## 1. publish 側の検査は壊れていない

`job_timeout_minutes` (`scripts/release/check-time-budget.py:96`) は 2 桁字下げの裸キー行 (`^  [A-Za-z][\w-]*:\s*$`) で job の境界を取り、目的の job の範囲に入っている間だけ 4 桁字下げの `timeout-minutes:` を読む。

- `.github/workflows/release.yml` で 4 桁字下げの `timeout-minutes` が現れるのは 6 箇所 (71 / 191 / 229 / 294 / 443 / 929) で、`publish:` (414) の範囲に入るのは 443 の `240` のみ、`wait-for-registries:` (919) の範囲に入るのは 929 の `60` のみ。取り違えは無い
- `on:` 配下の 2 桁字下げキー (`workflow_dispatch:` 等) も同じ正規表現に当たるが、いずれも `jobs:` より前で `inside` を False にするだけなので影響しない。`concurrency` / `env` の 2 桁字下げは値を伴うため一致しない
- step 段の `timeout-minutes` は 8 桁字下げになるため、job 段だけを読む性質も保たれている
- 出力の数値は 150 / 15 / 2 / 30 / 20 → 合計 217、上限 240、余裕 23 でデルタスペックの予算表と完全に一致する

取り違えが起きた場合に自己テストが気づくことも実測した。`check_registry_wait` が `publish` の値を読むよう改変すると NG 3 件、`check_publish` が `wait-for-registries` の値を読むよう改変すると NG 1 件 (基準ケース「現在の定数では両系統とも収まる」が exit 1 になる) で、どちらの向きの取り違えも捕まる。

## 2. 反映待ちの上界の式は実装と対応している

式は `to_minutes(2700) + to_minutes(120 × 1) + 5 = 45 + 2 + 5 = 52 分`。根拠を実装で確認した。

- `poll_once` (`scripts/release/wait-for-registries.sh:337`) は対象 1 件ごとに `can_start_probe "$(remaining_seconds)"` を評価し、期限を過ぎていれば残りの対象を照会せず `break` する。したがって期限を跨いで走り続けられるのは、期限直前に開始した照会 1 件だけ — `REGISTRY_OVERRUNNING_REQUESTS = 1` は正しい
- `probe_target` (`:327`) は 1 対象につき `http_get` を 1 回しか呼ばない (maven は HEAD 1 回、nuget は GET 1 回)。1 照会が複数要求に膨らむ経路は無い
- `http_get` (`:140`) の curl 引数に `--retry` は無く、`--max-time "$(request_max_time)"` が転送全体 (`--location` のリダイレクト追従を含む) を 120 秒で切る。`request_max_time` は残り時間による切り詰めを行わず常に `HTTP_MAX_TIME_SECONDS` を返すので、1 件ぶんの超過は最大 120 秒で確定する
- 巡回の切れ目の判定 (`:407` の `SECONDS + interval >= deadline`) は `poll_once` の後にあるため、超過は上記 1 件ぶんの上に積み増さない
- 読み取り対象の一意性も確認した。`KSR_POLL_TIMEOUT_SECONDS:-(\d+)` に一致するのは `:384` の 1 箇所のみ (自己テストの `KSR_POLL_TIMEOUT_SECONDS=5` は `:-` を持たず一致しない)、`HTTP_MAX_TIME_SECONDS=(\d+)` に一致するのは `:100` の 1 箇所のみ (`:481` の `max-time=${HTTP_MAX_TIME_SECONDS}$` は一致しない)

構造依存の注意書きの位置も妥当。式が壊れる条件 (照会ごとの期限判定をやめること) は `REGISTRY_OVERRUNNING_REQUESTS` の定義 (`check-time-budget.py:61`) に書かれ、対になる契約 (「居座るのは最後に始まった照会 1 件ぶんまで」「呼び出し側の実行時間上限より先にこちらが失敗する」) が `wait-for-registries.sh` の冒頭 (`:36-39`) にも書かれている。構造を変える側 (待機スクリプト) と式を持つ側 (検査スクリプト) の両方に注意が置かれている。

## 3. `REGISTRY_JOB_OVERHEAD_MINUTES = 5` は過小ではない

`wait-for-registries` job (`.github/workflows/release.yml:919-941`) が持つのは `actions/checkout` 1 つと `scripts/release/wait-for-registries.sh` の実行 1 つだけで、行列も matrix も artifact の授受も無い。`timeout-minutes` が計測するのはランナー割り当て後の実行時間 (`Set up job` 以降) であり、キュー待ちは含まれない。このリポジトリの checkout と runner セットアップは分オーダーに届かないため、5 分は余裕を見た値として妥当。余裕 8 分も残っている。

## 4. 自己テスト 12 件に空回りは無い

ミューテーションで検出力を実測した (一時ファイルは scratchpad 上のコピーに対して行い、作業ツリーは改変していない)。

| 改変 | 結果 |
|---|---|
| `check_registry_wait` の判定を常に True にする | NG 3 件 (反映待ちの上限 / 応答上限 / timeout-minutes の 3 ケース) |
| `single_value` が読み取り失敗・値の割れで黙って 0 を返す | NG 3 件 (publish の定数読み取り / 反映待ちの応答上限読み取り / 値の割れ) |
| `job_timeout_minutes` が読み取り失敗で 9999 を返す | NG 1 件 (publish の timeout-minutes 読み取り) |
| `check_registry_wait` が publish の timeout を読む (取り違え) | NG 3 件 |
| `check_publish` が wait-for-registries の timeout を読む (取り違え) | NG 1 件 |

12 件のうち「現在の定数では両系統とも収まる」は基準ケースとして期待値 0 を持ち、他 11 件はすべて期待値 1 の破壊ケース。`mutated` が置換対象の不在を `AssertionError` にしているため、土台が変わって置換が空振りした場合も無音にならない。

## 5. lint job の接続は正しい

`.github/workflows/ci.yml:198` `Release time budget check` (改称) と `:201` `Release time budget check selftest` (新設) が隣接し、直前のコメント (`:195-196`) が「検査と自己テストの両方をここで回す理由」を説明している。後続の `:204` `Publish step order check` / `:209` `Publish step order check selftest` と同じ並びで、既存の selftest 群 (`:184` / `:187` / `:190` / `:193`) の下に置かれている。step 名の改称に伴う参照切れは無い (`ci.yml` 内に step 名を参照する箇所は無く、branch protection の必須 status check は job 名 `lint` 単位)。

## 指摘事項

### [🟡 Minor] `timeout-minutes: 60` の脇のコメントが、実際に効いている検査の式より緩い

**該当箇所**: `.github/workflows/release.yml:924-928`

**問題点**: コメントは「45 + 2 < 60 の関係が保てるかをここで見直す」と書いているが、機械検査が実際に課すのは `45 + 2 + 5 (job の前後) <= 60` で、こちらのほうが厳しい。待機の上限を 54 分へ延ばした場合、コメントの式 (54 + 2 = 56 < 60) では通るのに `check-time-budget.py` は落ちる。この change が潰そうとしている「二重管理された値が食い違う」状態が、workflow のコメントと検査スクリプトの間で小さく残っている。publish 側のコメント (`:430-442`) が予算表と検査スクリプト名を明記しているのと比べても非対称。

**推奨修正**: コメントの式へ job の前後ぶん (5 分) を含め、publish 側と同様に突き合わせを `scripts/release/check-time-budget.py` が lint job で行う旨を添える。

**判定への影響**: 無し。検査スクリプトが正であり、食い違いは lint job で必ず可視化されるため、リリース時の実害は生じない。

### [🟡 Minor] module docstring に文書名の裸参照がある

**該当箇所**: `scripts/release/check-time-budget.py:22`

**問題点**: 「spec が求める「対象ごとに保持している分類を出力に含める」が失われる」の `spec` が、`kasane/handbook/cross/comment-policy.md` の「禁止する参照 — 変更・フェーズ識別子や文書名の裸参照」に当たる。求められている内容そのものは引用されているため読解は可能で、`comment-policy-lint.py` の検出パターン (`delta\s+spec|デルタスペック`) にも掛からないが、規約本文からの判定は違反側になる (規約は「検査の検出範囲は本規約より狭い」と明記し、この判定をコードレビューに委ねている)。

**推奨修正**: 「spec が求める」を文書に依存しない言い方 (例:「反映待ちが約束している」) へ置き換える。

**判定への影響**: 無し。機能・検査の正しさには影響しない。

## アクションプラン

1. (任意) `release.yml:924-928` のコメントを検査の式に合わせる — 指摘 1
2. (任意) `check-time-budget.py:22` の `spec` 裸参照を書き換える — 指摘 2

どちらも実害が無く、オーナー報告事項として据え置いても差し支えない。この change の実装内容としては APPROVED。

## 確認したがそのままでよいと判断した点

- `KSR_POLL_TIMEOUT_SECONDS` は `central-portal.sh` (既定 1800) と `wait-for-registries.sh` (既定 2700) で共有された名前だが、`release.yml` は両 job のどちらでもこの変数を設定していないため、検査がスクリプトの既定値を読むことと実行時の値は一致する。将来 workflow 側で上書きを足すと検査が見えない前提になるが、これは publish 側にも同じ形で存在する既存の設計であり、今回の変更が持ち込んだものではない
- `check-time-budget.py` / `check-publish-step-order.py` の実行権限、`POLL_DEADLINE` と `deadline` の 2 名持ち、`POLL_DEADLINE_SET=0` の実行時呼び出し元不在 — いずれも据え置きが合意済みの Suggestion であり、判定の材料にしていない
- `tasks.md` 5.2 (`dry-run` での release 起動) の未チェックは実態と一致しており、虚偽チェックではない
- 足場アーティファクト (`proposal.md` / `specs/release-workflow/spec.md` / `exploration.md`) は HEAD との差分なし。実装中の書き換えは無い
