# レビュー結果: backport-release-workflow-hardening (001 回目)

**日付**: 2026-09-11
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 4 Requirement はいずれも実装されており、検査・自己テストは全件成功する。判定ロジックの検出力はミューテーションで実測し、時間予算の検査・step 順序の検査・状態分類の自己テストのいずれも、壊したときに実際に落ちることを確認した (下の「実測した検出力」)。

一方で、`scripts/release/wait-for-registries.sh` の照会の応答上限が 120 秒から 300 秒へ引き上げられており、その理由がどこにも記録されていない。この変更で `wait-for-registries` job の最悪所要が自分の `timeout-minutes: 60` を超えうる形になり、Requirement「公開レジストリへの反映待ち」が求める「上限に達して失敗するときの対象別出力」を job 打ち切りで失う経路ができている。あわせて、この定数に添えられたコメントが「時間予算の検査が読む値でもある」と述べているが、`check-time-budget.py` はこのファイルを読まない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントを多数追加しているため本文まで照合
- `kasane/handbook/cross/test-execution.md` (テスト実行・テスト結果の報告) — 自己テストと検査スクリプトの実行・件数報告
- `kasane/handbook/cross/release-procedure.md` (release workflow を触る作業) — publish の待ちと再実行の手順との整合
- `kasane/lessons/code-review.md` の重点観点 [L-001] を適用し、静的読解でなくミューテーションで検出力を実測した

`kasane/decisions/cross/index.md` の accepted ADR に抵触なし (0018 / 0019 / 0020 / 0025 / 0026 / 0028 を確認)。0029 / 0030 は proposed で、いずれも本変更の Non-Goal 側の論点。

## 実行結果

いずれもリポジトリルートで実行 (lint job と同じ内容)。

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 失敗なし (新規 2 件を含む全件 OK) |
| `scripts/release/wait-for-registries.sh --selftest` | 失敗なし (29 件 OK) |
| `scripts/release/deployment-handover.sh --selftest` | 失敗なし (23 件 OK) |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 / 余裕 23 分 (exit 0) |
| `python3 scripts/release/check-publish-step-order.py` | 順序は正しい (exit 0) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | 禁止 0 件 (comment-policy の要確認 164 件はすべて `samples/` の既存分で本 diff 由来なし) |

本 diff は workflow と `scripts/release/` のみに触れ、3 platform の本体コードに変更がないため、iOS / Android / MAUI のテストスイートは対象外とした。

### 実測した検出力 (ミューテーション、いずれも作業ツリーは変更せず一時コピーに対して実施)

| 壊した箇所 | 検査の反応 |
|---|---|
| 引き継ぎの download / inspect を Android 成果物の取得より後ろへ移動 | `check-publish-step-order.py` が 2 件のエラーで exit 1 |
| 引き継ぎの download step を削除 | 同上 (「1 件でない (0 件)」) |
| `KSR_PUBLISHED_TIMEOUT_SECONDS` を 5400→9000 | `check-time-budget.py` が「必要 277 分 > 240 分」で exit 1 |
| `--max-time 300`→600 (central-portal.sh) | 同上 (「必要 262 分」) |
| `timeout-minutes` を 240→120 | 同上 (「必要 217 分 > 120 分」) |
| Maven の 5xx を未反映へ畳み込む | `wait-for-registries.sh --selftest` が NG 1 件 |
| 上限超過時の対象別出力を削除 | 同上 NG 1 件 |
| 反映済みの再照会スキップ (sticky) を削除 | 検出はされるが NG ではなく hang になる (指摘 2) |

## 指摘事項

### [🟠 Major] 照会の応答上限の引き上げが、反映待ち job の実行時間上限を追い越している

**該当箇所**: `scripts/release/wait-for-registries.sh:91`、`.github/workflows/release.yml:916`

**問題点**:

2 点ある。

1. **コメントが事実と違う。** `readonly HTTP_MAX_TIME_SECONDS=300` に「時間予算の検査が読む値でもある」と添えてあるが、`check-time-budget.py` が `--max-time (\d+)` を読むのは `scripts/release/central-portal.sh` だけで、このファイルは引数にも既定値にも現れない。加えてこちらは `--max-time "${HTTP_MAX_TIME_SECONDS}"` と変数で書かれているため、仮に対象へ加えても同じ正規表現では拾えない。守られていない定数を「検査が読む」と説明すると、次にこの値を触る人が根拠なく安全だと判断する。

2. **この引き上げで job の予算が逆転している。** 変更前は `--max-time 120` だった (`git show HEAD:scripts/release/wait-for-registries.sh`)。上限の判定は `poll_once` の**後**にあるため、最後の 1 巡は上限直前に始まりうる。最悪所要は「スクリプト上限 2700 秒 + 1 巡 (4 対象 × 応答上限)」で、

   - 変更前: 2700 + 4×120 = 3180 秒 ≒ 53 分 (< `timeout-minutes: 60`)
   - 変更後: 2700 + 4×300 = 3900 秒 ≒ 65 分 (> `timeout-minutes: 60`)

   となり、job 側の打ち切りが先に来る。job が打ち切られると `fail` 直前の「対象ごとの最後の分類」が出力されない — これは Requirement「公開レジストリへの反映待ち」の「上限に達して失敗するときは、対象ごとに保持している分類を出力に含める」が守られない経路であり、本変更が掲げた「上限まで待って失敗したとき、遅いのか壊れているのかを出力から読み分ける」という目的そのものを取り落とす。`release.yml:916` のコメント「スクリプト側の上限 (45 分) を超えても job が居座らないようにする」も、45 分で収まる前提のまま残っている。

   なお `wait-for-registries` は publish とは別 job なので 240 分の予算表には入らない。だからこそ、この job の予算は誰も検査していない。

**推奨修正**: 次のいずれか。

- 応答上限を 120 秒に戻す (spec は `wait-for-registries` 側の応答上限に何も要求していない。引き上げの理由が無いなら元に戻すのが最小)
- 300 秒を維持するなら `wait-for-registries` job の `timeout-minutes` を最悪ケース (66 分以上) に合わせ、`release.yml:916` のコメントの根拠も書き換える

いずれの場合も `HTTP_MAX_TIME_SECONDS` のコメントから「時間予算の検査が読む値でもある」を削る (この値を検査対象にしたいなら `check-time-budget.py` 側に加える必要があるが、別 job なので同じ予算表には載らない — 載せるなら独立した算出として)。

### [🟡 Minor] 待ちの自己テストが、暴走したときに NG ではなく hang になる

**該当箇所**: `scripts/release/wait-for-registries.sh:534`、`scripts/release/wait-for-registries.sh:547`、`scripts/release/central-portal.sh:506`

**問題点**: モックの台本が尽きたとき、`http_get` は標準出力に何も出さずに `return 1` する。bash は AND-OR リスト配下のコマンド置換で `set -e` を無視するため (手元の bash 3.2 で実測。CI の bash 5.x でも同じ挙動が期待される)、この失敗は呼び出し側へ伝わらず、空応答が「判定不能 (応答を解釈できない)」へ畳み込まれて巡回が続く。

巡回の検査のうち「全件が反映済みになれば成功する」と「判定不能から回復すれば成功する」は `KSR_POLL_TIMEOUT_SECONDS` を指定していないため既定の 2700 秒で走り、間隔は 0 秒。したがって**照会回数が想定より増える回帰が入ると、NG を出す前に最大 45 分の busy loop になる**。実測: sticky (反映済みの再照会スキップ) を外したコピーの `--selftest` は 20 秒経っても該当検査から進まず、CPU を回し続けた。lint job は `timeout-minutes: 10` なので被害は 10 分で頭打ちだが、報告されるのは「どの検査が何を返したか」ではなく job のタイムアウトになる。

`central-portal.sh` の `cmd_wait_published` を上限なしで呼ぶ検査 (506 / 517 / 522 行) も同じ形で、本変更で既定が 1800→5400 秒になったぶん暴走時の待ちが 3 倍に伸びている。

**推奨修正**: 待ちのループを回す自己テストには、必要な巡回数を満たす範囲で明示的な上限を与える (例: `KSR_POLL_TIMEOUT_SECONDS=5` / `KSR_PUBLISHED_TIMEOUT_SECONDS=5`)。あるいはモックの台本が尽きたことを記録ファイルへ立て、巡回側がそれを見て即座に打ち切る。回帰が「10 分後に job がタイムアウト」ではなく「数秒で NG」として出ることが要点。

### [🔵 Suggestion] `writeback` の `keep` / `store` が workflow から到達しない

**該当箇所**: `scripts/release/deployment-handover.sh:99`、`.github/workflows/release.yml:887`

**問題点**: 呼び出し側の `Drop pending deployment` step は `if: failure() && (steps.maven.outputs.deployment-id || steps.recover-deployment.outputs.deployment-id) != ''` で守られているため、`writeback` に空の ID が渡ることがない。よって `keep` は workflow からは出ず、`store` も `else` 節で文字列を echo するだけで書き戻しは起きない。workflow が実際に分岐するのは以前と同じ `clear` かどうかの 1 点である。

その結果、自己テストの「[読み込み後に別の成果物の取得が失敗]」節 (`cmd_writeback "" ""` → `keep`) は、workflow に存在しない呼び出し形を検査している。tasks 3.4 の 5 状態のうち「読み込み後に別の成果物の取得が失敗」「各失敗経路が既存の引き継ぎを空で上書きしない」を実際に担保しているのは、この関数ではなく step の `if:` 条件と `cleared` output のゲートのほう。判定の切り出し自体は妥当 (`NOT_FOUND` の語彙が 1 箇所に集まり、自己テストが掛かるようになった) なので、実装の修正ではなく、防御的な分岐であることが分かる書き方にしたい。

**推奨修正**: `keep` を「呼び出し側のガードが外れた場合の保険」と明記するか、workflow 側で `store` / `keep` を実際に分岐させる (例: `store` のときに手元の ID を書き戻す step を持たせる)。前者なら自己テストのコメントも「workflow の経路ではなく関数の契約を検査している」ことが読み取れる形にする。

## 確認して問題なかった観点

- 時間予算の表 (150 / 15 / 2 / 30 / 20 / 余裕 23 / 合計 240) は spec・`release.yml` のコメント・`check-time-budget.py` の算出の 3 者で完全に一致する。`single_value` が「同じ定数が複数箇所で割れている」ケースを検査の失敗にしているのも妥当
- `wait-for-registries.sh` の状態分類は spec の 5 Scenario すべてに対応し、sticky 性 (`poll_once` の `STATE_REFLECTED` スキップ) は呼び出し回数のアサーション (7 件 / 5 件) で担保されている。モックは `http_get` 1 関数だけを差し替えるので、URL 組み立てと分類は本番と同じ経路を通る
- step 順序は `Download previous deployment id` → `Inspect previous deployment handover` → `Download Android artifacts` → `Download MAUI packages` で、引き継ぎの読み込みは成果物の取得より前にある。旧実装のインライン読み込み (`id_file`) の残骸はない
- `deployment-handover.sh inspect` は `tee -a "$GITHUB_OUTPUT"` で output とログの両方に残り、`tr -d '[:space:]'` により改行注入が起きない。読み込み失敗時は stdout を出さずに exit 1 するので `loaded=true` が立たない
- `deviation.md` の付随修正 (lint job への `deployment-handover.sh --selftest` 追加) は同梱条件を満たす (本務で触るファイル・公開契約に触れない・局所的・自己テスト自身が担保・ユーザー判断を要しない)
- 新規コメントに comment-policy の禁止参照 (作業文書のパス・変更識別子の裸参照・ローカル通番・履歴記述・SHALL 等) はない。`scripts/release/check-time-budget.py` 等の参照はリポジトリ内のソースファイル名なので許容参照
- `tasks.md` の変更はチェック状態のみで、`proposal.md` / `specs/` への逆流はない

## アクションプラン

1. (Major) `wait-for-registries.sh` の応答上限を 120 秒へ戻すか、`wait-for-registries` job の `timeout-minutes` を最悪ケースに合わせる。あわせて `HTTP_MAX_TIME_SECONDS` のコメントから「時間予算の検査が読む値でもある」を削る
2. (Minor) 待ちのループを回す自己テスト 3 箇所に明示的な上限を与え、暴走が hang ではなく NG になるようにする
3. (Suggestion) `writeback` の `keep` / `store` が防御的分岐であることをコメントで明示する (または workflow 側で分岐させる)
