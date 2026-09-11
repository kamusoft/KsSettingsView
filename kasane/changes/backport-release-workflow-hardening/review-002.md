# レビュー結果: backport-release-workflow-hardening (002 回目)

**日付**: 2026-09-11
**判定**: CHANGES_REQUESTED

## サマリー

前回確定した 4 件はすべて閉じている。いずれも「直した」だけでなく、壊したコピーに対して検査が実際に落ちることまで実測した (下の「前回指摘の閉じ確認」)。検査・自己テストは全件成功する。

ただし指摘 1 の修正として入った「照会ごとに残り時間で応答上限を切り詰める」仕組みに、新しい欠陥がある。巡回の期限判定が `sleep` の前にあるため、**上限まで待って失敗する実行では最後の 1 巡がほぼ必ず期限を過ぎてから始まり、4 対象すべてが応答上限 1 秒で照会される**。その 1 巡の分類がそのまま失敗出力になるので、単に未反映だっただけの対象が「判定不能 (通信そのものの失敗)」として報告されうる。本変更が掲げた「遅いのか壊れているのかを出力から読み分ける」の逆を引き起こす経路で、実測で再現した。

なお job の実行時間上限 (60 分) を追い越す経路そのものは解消している。切り詰めにより待機は上限 + 数秒で戻る。

## 照合した規約

`kasane/handbook/index.md` から cross の index を読み、担当範囲 (`.github/workflows/` と `scripts/release/` の bash / python、テスト実行) に当たる文書を本文までロードした。

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントが多いため本文まで照合
- `kasane/handbook/cross/test-execution.md` (テスト実行・テスト結果の報告) — 自己テストと検査の実行・件数報告。3 platform の本体コードに変更が無いため iOS / Android / MAUI のテストスイートは対象外と判定した
- `kasane/handbook/cross/release-procedure.md` (release workflow を触る作業) — publish の待ちと再実行の手順との整合
- 適用外と判定: `runtime-behavior-verification.md` / `sample-parity.md` / `public-identifiers.md` / `diagnostic-message-language.md` (本体コードの開発者向け文字列が対象) / `aiforms-origin-reference.md` / `user-skill-api-listing.md` / `local-development-setup.md`
- `kasane/lessons/code-review.md` の重点観点 [L-001] を適用し、修正の検出力を静的読解でなくミューテーションで実測した

`kasane/decisions/cross/index.md` の accepted ADR に抵触なし。

## 実行結果

いずれもリポジトリルートで実行 (lint job と同じ内容)。

| 実行 | 結果 |
|---|---|
| `scripts/release/central-portal.sh --selftest` | 失敗なし (52 件 OK。新規 2 件を含む) |
| `scripts/release/wait-for-registries.sh --selftest` | 失敗なし (40 件 OK / 1.9 秒) |
| `scripts/release/deployment-handover.sh --selftest` | 失敗なし (22 件 OK) |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 / 余裕 23 分 (exit 0) |
| `python3 scripts/release/check-publish-step-order.py` | 順序は正しい (exit 0) |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 失敗なし (7 件 OK) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` | 禁止 0 件 |
| 両 workflow の YAML 解析 | 正常 (jobs を読める) |

## 前回指摘の閉じ確認

ミューテーションはすべて一時コピーに対して行い、作業ツリーは変更していない。

| 前回の指摘 | 現在 | 検出力の実測 |
|---|---|---|
| (Major) 上限超過時に対象ごとの分類が出力されない経路 | 応答上限を 120 秒へ戻し (`scripts/release/wait-for-registries.sh:96`)、さらに照会ごとに残り時間で切り詰める (`:108-121`)。誤記コメント「時間予算の検査が読む値でもある」は削除済み | 切り詰めを無効化したコピーで `--selftest` が NG 3 件。**ただし下の Major を新たに持ち込んでいる** |
| (Major) 不正な NuGet `versions` 要素を未反映と誤分類 | 全要素が文字列であることを検査し、違えば exit 2 (`:224`) | `isinstance` 検査を外して `str(v)` に戻したコピーで NG 3 件 (null / 数値混在 / object) |
| (Minor) 順序検査が download と inspect の前後関係を見ていない | `inspect_index < download_index` を検査 (`scripts/release/check-publish-step-order.py:113`)。`--selftest` で 6 通りの破壊入力を検査し lint job に接続 | 当該 3 行を外したコピーで `--selftest` が NG 1 件 |
| (Minor) 自己テストの台本切れが NG ではなく hang | 台本切れを印ファイルに残して検査が明示的に見る (`wait-for-registries.sh:418` / `:442-448`、`central-portal.sh:362` / `:584`)。巡回を回す検査に明示の上限を与えた | sticky を外したコピーで、前回 20 秒経っても進まなかった `--selftest` が **10.9 秒で NG 6 件**を出して終了 |

明示の上限が検査を骨抜きにしていないことも確認した。「上限に達すれば失敗する」は `KSR_POLL_TIMEOUT_SECONDS=0` を明示して上限経路そのものを通しており、`=5` を与えているのは全件反映・判定不能からの回復という**成功を確かめる 2 件だけ**で、この 2 件は上限を見ていない。`central-portal.sh` で上限分離を戻したコピー (`KSR_PUBLISHED_TIMEOUT_SECONDS` → `KSR_POLL_TIMEOUT_SECONDS`) は新規検査が NG 2 件を出す。

## 指摘事項

### [🟠 Major] 期限到達後に始まる最後の 1 巡が、未反映を「通信そのものの失敗」へ書き換える

**該当箇所**: `scripts/release/wait-for-registries.sh:108-121`、`scripts/release/wait-for-registries.sh:327-338`、`scripts/release/wait-for-registries.sh:390-396`

**問題点**:

期限の判定 (`:390`) は `sleep "${interval}"` (`:396`) の**前**にある。したがって判定を通過した巡回の次は、期限の最大 `interval` 秒後から始まる。上限 2700 秒・間隔 30 秒の既定では、最後の判定は期限の 30 秒前から直前までのどこかで起き、その後の `sleep 30` を経て始まる巡回は**ほぼ必ず期限を過ぎている**。

期限を過ぎた照会は `request_max_time` が残り時間を 1 秒に丸めるため (`:114-115`)、この最後の 1 巡は 4 対象すべてを `--max-time 1` で照会する。1 秒に収まらなかった対象は curl の打ち切りとなり、`http_get` が `000` を返して「判定不能 (通信そのものの失敗)」に分類される。そして失敗出力に載るのは、まさにこの巡回で保持した分類である。

実測 (期限と巡回の関係を見るため上限 5 秒・間隔 3 秒に縮めた。既定値でも構造は同じ):

```
probe ... max-time=5 (SECONDS=0 deadline=5)   ← 巡回 1
probe ... max-time=2 (SECONDS=3 deadline=5)   ← 巡回 2
probe ... max-time=1 (SECONDS=6 deadline=5)   ← 巡回 3 (期限後に開始、4 対象すべて 1 秒)
```

応答に 1.5 秒かかる健全な相手 (未反映なので 404 を返すだけ) を模すと、巡回 2 までは 4 対象とも「未反映」だったものが、最後の巡回で全対象が次の形になる:

```
対象ごとの最後の分類:
  Maven Central jp.kamusoft:kssettingsview: 判定不能 (通信そのものの失敗)
  nuget.org/kssettingsview.maui: 判定不能 (通信そのものの失敗)
  ... (4 対象すべて)
```

読む人は「4 つとも壊れている」と判断するが、実際にはどれも正常に応答しており単に未反映だった。分類を持ち込んだ目的 (遅いのか壊れているのかを読み分ける) を、いちばん必要な瞬間に裏切る。しかも打ち切ったのは自分自身であり、レジストリ側の通信失敗として報告するのは事実に反する。

上限に達する実行は「レジストリが遅い・不調」と相関するため、応答が 1 秒に収まらない確率はむしろ高い側に寄る。

この経路は自己テストの外にある。`request_max_time` の単体検査 (`:505-527`) は境界を押さえているが、巡回の検査は `http_get` をモックへ差し替えるため、**最後の巡回が実際にどの応答上限で照会するかを見ている検査が 1 つも無い**。

**推奨修正**: 期限を過ぎた照会を送らない形にする。いずれでもよい。

- `poll_once` に期限判定を入れ、期限を過ぎていたら残りの対象を照会せず、保持している分類をそのまま残す
- 巡回の入口で残り時間を見て、1 巡ぶんの余裕が無ければ巡回に入らずに失敗へ進む (期限判定を `sleep` の前後どちらでも成立する位置へ移す)

あわせて、切り詰めによる打ち切りを「通信そのものの失敗」と書かない (打ち切ったのは自分だと分かる分類か、そもそも照会しない)。検査は、最後の巡回が 1 秒照会にならないこと — 期限後に巡回が始まらないこと — を見る形にする (`request_max_time` の単体検査だけでは今回の経路は通らない)。

### [🟡 Minor] 自己テストのコメントが、干渉しない側の上限を「既定」と書いている

**該当箇所**: `scripts/release/central-portal.sh:539-540`

**問題点**: 「片方を 0 にしても、もう片方の待ちは自分の上限 (既定) に従って続く」とあるが、この自己テストは冒頭 (`:367-368`) で `KSR_POLL_TIMEOUT_SECONDS=5` と `KSR_PUBLISHED_TIMEOUT_SECONDS=5` を export しているため、続く側が従うのは既定 (1800 / 5400) ではなく 5 秒である。安全網の export を足したときにこのコメントを読み直していない。次にこの検査を触る人が「既定で走っている」と読み、安全網を外しても同じだと判断しうる。

**推奨修正**: 「自分側の上限 (この自己テストでは安全網の 5 秒)」のように、実際に効いている値が分かる書き方にする。

### [🔵 Suggestion] `writeback` の `keep` / `store` が workflow から到達しない (前回からの持ち越し)

**該当箇所**: `scripts/release/deployment-handover.sh:115`、`.github/workflows/release.yml:887`

**問題点**: 前回と同じ。呼び出し側の `Drop pending deployment` step が `if:` で空 ID を弾くため `keep` は出ず、`store` も文字列を出すだけで書き戻しは起きない。workflow が実際に分岐するのは `clear` かどうかの 1 点で、自己テストの「[読み込み後に別の成果物の取得が失敗]」節は workflow に存在しない呼び出し形を検査している。仕様違反ではなく、`NOT_FOUND` の語彙が 1 箇所に集まった利点もあるため、今回も修正を求めるものではない。

**推奨修正**: `keep` が「呼び出し側のガードが外れた場合の保険」であることをコメントに明記する (または workflow 側で `store` を実際に分岐させる)。前者なら自己テストのコメントも、workflow の経路ではなく関数の契約を検査していると読み取れる形にする。

### [🔵 Suggestion] 反映待ち job の予算だけが機械検査の外にある

**該当箇所**: `.github/workflows/release.yml:919`、`scripts/release/wait-for-registries.sh:369`

**問題点**: publish 側は定数と `timeout-minutes` の突き合わせを `check-time-budget.py` が持つようになった一方、`wait-for-registries` job は「スクリプトの上限 2700 秒」と「job の 60 分」が別々の場所にリテラルで置かれたままで、誰も突き合わせていない。切り詰めが入ったことで現状は上限 + 数秒に収まるが、上限側を延ばしたときに job の打ち切りが先に来る形は同じ手順で再発しうる。

**推奨修正**: `check-time-budget.py` に独立した算出として足すか、少なくとも `release.yml:919` のコメントに「スクリプト側の上限 (`KSR_POLL_TIMEOUT_SECONDS` の既定 2700 秒) を延ばすならこの値も見直す」と根拠を書き残す。

## 確認して問題なかった観点

- 切り詰めは待機の外から 1 回だけ照会する使い方を壊していない。`POLL_DEADLINE_SET` の既定が 0 で、`request_max_time` はそのとき応答上限をそのまま返す (`:109-112`)。自己テストも期限未設定のケースを 1 件目に置いている。本番の呼び出し経路は `main` → `wait_for_registries` の 1 本だけで、期限未設定で照会するのは自己テストのみ
- 切り詰めの境界 (残り 7 秒 → 7 / 残り 0 → 1 / 期限超過 → 1) は単体検査があり、切り詰めを無効化すると 3 件が NG になる。`0` を渡すと curl が無制限になるため 1 を下限にしている判断も妥当
- 明示の上限 (`=5`) を与えた 2 件はいずれも「成功すること」を見る検査で、上限そのものを見る検査は `=0` を明示している。骨抜きにはなっていない
- 増えた自己テストに空回りは見つからなかった。台本切れの印 (`not_exhausted`) は巡回を回す 3 件すべてに付き、sticky を外すと実際に NG になる。`central-portal.sh` 側は `arrange` が印を消さないため全検査を通した累積判定になっており、こちらのほうが強い
- 時間予算の表 (150 / 15 / 2 / 30 / 20 / 余裕 23 / 合計 240) は spec・`release.yml` のコメント・`check-time-budget.py` の算出で一致する。`single_value` が「定数を読めない」「値が割れている」をどちらも失敗にするため、`check-time-budget.py` 自身は自己テストが無くても無音で退化しない (`--max-time` を変数化した `wait-for-registries.sh` のような書き換えは exit 1 になる)
- `check-publish-step-order.py` は実物の workflow を土台に 6 通りの破壊入力を作って検出力を確かめており、`--selftest` も lint job に接続されている
- step 順序は `Download previous deployment id` → `Inspect previous deployment handover` → `Download Android artifacts` → `Download MAUI packages`。旧実装のインライン読み込み (`id_file`) の残骸はなく、`Sign and publish` は `KS_HANDOVER_DEPLOYMENT_ID` を受け取るだけになっている
- `deployment-handover.sh inspect` は `tee -a "$GITHUB_OUTPUT"` で output とログの両方に残り、`set -euo pipefail` と合わせて読み込み失敗時はその step で publish が止まる (`loaded=true` が立たない)
- `deviation.md` の付随修正 2 件 (lint job への `deployment-handover.sh --selftest` と `check-publish-step-order.py --selftest` の追加) はいずれも同梱条件を満たす。本務で触るファイル内、公開契約に触れない、局所的、追加した検査自身が担保、ユーザー判断を要しない
- 新規コメントに comment-policy の禁止参照 (作業文書のパス・変更識別子の裸参照・ローカル通番・履歴記述・spec 構文キーワード) は無い。スクリプト名への言及はリポジトリ内のソースファイル名なので許容参照
- `tasks.md` の変更はチェック状態のみで、`proposal.md` / `specs/` への逆流はない (`git log` / `git status` で確認)

## アクションプラン

1. (Major) 期限を過ぎてから巡回を始めない形にする。最後の 1 巡が `--max-time 1` にならないこと (= 失敗出力の分類が自分の打ち切りで書き換わらないこと) を検査で押さえる
2. (Minor) `central-portal.sh:539-540` のコメントを、安全網の export が効いている実態に合わせる
3. (Suggestion) `writeback` の `keep` が防御的分岐であることを明記する — 前回からの持ち越しで、オーナー判断待ち
4. (Suggestion) 反映待ち job の上限と `timeout-minutes` の関係を検査か根拠コメントで残す
