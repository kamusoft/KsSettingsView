# レビュー結果: fix-release-central-validation-wait (001 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

合意済みスコープ (検証の決着を upload の直後・NuGet push より前で待つ / 待ちを `wait-validated` サブコマンドへ一本化する / `release` の安全弁を残す) はいずれも実装されており、待ちの位置・再実行経路の分岐の等価性・標準出力の純度・後始末との整合をコードで確認して問題は見つからなかった。自己テストは 8 件が追加され全件成功、comment-policy lint と doc-structure lint も指摘なし。

指摘は Critical / Major なし。Minor 2 件はいずれも「job が cancel / timeout されたとき」という運用上の縁のケースで、待ち時間が最大 30 分ぶん伸びたことで既存の窓が広がった点を指すもの。手動回収の入口 (Portal の deployment 一覧) は手順書に既にあるため、本変更を止める理由にはしない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメント (script 4 箇所・workflow 2 箇所) を節ごとに照合。禁止参照 (作業文書パス・change 識別子・ローカル通番)、禁止記述類型 (進捗ログ・履歴記述・デルタスペック構文キーワード) のいずれにも該当なし。`python3 scripts/comment-policy-lint.py` は禁止 0 件 (784 ファイル)、`--advisory` でも対象 2 ファイルに報告なし
- `kasane/handbook/cross/release-procedure.md` (リリースを行うとき / 再実行するとき) — 本変更が更新する当事者の文書として、記述と実装の一致を照合 (下記「確認した観点」参照)
- `kasane/handbook/cross/test-execution.md` (テスト実行・報告) — 本変更のテストは bash の自己テストのみで platform テストの節は非該当。実行件数の併記の規律に従い件数を報告する
- `kasane/lessons/code-review.md` — 重点観点 L-001 (アサーションの検出力はミューテーションで実測する) を適用。下記 Suggestion 1 はその実測結果

## 実行した検証

| 検証 | 結果 |
|---|---|
| `bash scripts/release/central-portal.sh --selftest` | 「失敗なし」(exit 0)。全 56 チェック OK / 0 失敗。うち `[wait-validated]` の 8 件が本変更で追加されたもの |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 784 ファイル)。`--advisory` でも対象 2 ファイルに報告なし |
| `python3 scripts/doc-structure-lint.py` | `kasane/handbook/cross/release-procedure.md` に指摘なし |
| workflow の run ブロックの `bash -n` | publish job の全 run ブロックを YAML から抽出して構文確認、失敗 0 件 |
| ミューテーション実測 2 件 | 下記 Suggestion 1 に記載。実施後は backup との shasum 一致 (`aaf33b2a...`) と自己テスト再実行で原状復帰を確認済み |

## 確認した観点 (問題なしと判断したもの)

- **検証待ちの位置**: 新しい待ちは `Publish Android to Central Portal` step (`.github/workflows/release.yml:648`) の中、`Login to NuGet.org` (`:718`) と `Push packages to NuGet.org` より前にある。NuGet push より前という決定事項を満たす
- **再実行経路の分岐の等価性**: 直書きループの置換 (`.github/workflows/release.yml:584`) は、待機を抜ける条件 (PENDING / VALIDATING 以外)・後続の `echo "状態: ${state}"` と case (VALIDATED / PUBLISHING / PUBLISHED / FAILED / NOT_FOUND / `*`)・既定のポーリング間隔 (30 秒) と上限 (1800 秒)・上限超過を失敗にすること、のすべてが置換前と一致する。差は上限超過時のメッセージ文言と、進捗行の出力先が標準エラーに変わったこと (step ログには従来どおり出る) のみ
- **標準出力の純度**: `cmd_wait_validated` は決着した状態のみを標準出力へ出し、進捗は標準エラーへ出す。ミューテーション (`>&2` の除去) で自己テストの 3 件が NG に転じることを実測し、呼び出し側のコマンド置換を守る性質に検出力があることを確認した
- **上限超過時の後始末との整合**: 上限超過で step が落ちると `deployment_id` は出力へ書かれないまま失敗するが、`Recover deployment id from upload log` (`:672`) が upload ログから拾い直して artifact に保存し、`Drop pending deployment` (`:836`) が呼ばれる。PENDING / VALIDATING は `cmd_drop` が何も送らず理由だけ出して正常終了し、続く `status` が NOT_FOUND でないため ID が残る。次の attempt が引き継ぎ経路で同じ deployment の決着待ちに入る — handbook の新しい表の記述どおり
- **FAILED 時の後始末との整合**: fresh upload 経路で FAILED になると step が落ち、同じ拾い直し経路を経て `cmd_drop` が FAILED を DELETE し、`status` が NOT_FOUND を返すため `cleared=true` で ID が空に置き換わる。次の attempt は upload からやり直す。NuGet push はこの時点で未実行 — handbook の新しい表の記述どおり
- **NOT_FOUND の扱い**: fresh upload 直後に deployment が消えた場合も `!= VALIDATED` で止まり、後始末は NOT_FOUND を「削除しない」で通して ID を空にする。停止しない
- **`release` の安全弁**: `cmd_release` の VALIDATED 再確認は変更されていない (ヘッダの契約説明に 1 文足しただけ)
- **サブコマンドの配線**: usage・ヘッダの契約・`main` の受理リスト・内側の dispatch の 4 箇所すべてに `wait-validated` が入っており、引数個数と認証情報の要求も他の Portal サブコマンドと同じ扱いになっている
- **handbook と実装の一致**: 「上限 30 分」は `KSR_POLL_TIMEOUT_SECONDS` の既定 1800 秒と一致し、workflow は上書きしていない。追加された 2 行は実行順どおりの位置に置かれ、内容は上記の実コード確認と一致する
- **足場アーティファクト**: `exploration.md` の変更は「契約の精緻化 (実装時)」の追記であり、決定事項を実装に合わせて後付けで書き換えたものではなく、決定の細目を明示する追記として妥当と判断した

## 指摘事項

### [🟡 Minor] 検証待ちの間に job が cancel / timeout されると、保留 deployment を回収する手がかりが残らない

**該当箇所**: `.github/workflows/release.yml:648-658` (待ちの位置) / `:672` (`Recover deployment id from upload log`) / `:707` (`Upload deployment id`)

**問題点**: deployment ID が durable になるのは step が正常に終わった後の `Upload deployment id` (artifact 保存) であり、upload 受理から artifact 保存までの窓の中に、最大 30 分の待ちが新たに入った。この窓で失敗した場合は `if: failure()` の拾い直し + drop が回収するが、**cancel と job timeout は `failure()` に該当しない**ため、`if: failure()` の step 群 (`Recover deployment id from upload log` / `Store recovered deployment id` / `Drop pending deployment`) はいずれも走らない。upload ログは `RUNNER_TEMP` にしかないので attempt をまたいで残らず、次の attempt は ID を持たずに再 upload し、同じ version の保留 deployment が 2 件になる。

窓そのものは本変更以前から存在した (gradle の upload 実行の最後尾) が、当時は数秒だったものが最大 30 分になる。30 分の待ちは人が痺れを切らして cancel を押す長さでもある。

**推奨修正**: fresh upload 経路の待ちだけを独立した step へ出し、`Upload deployment id` の後・`Login to NuGet.org` の前に置く (`if: steps.maven.outputs.release-needed == 'true'`)。引き継ぎ VALIDATED の場合は即座に返るので余計な待ちは生じず、決定事項「NuGet push より前」も満たしたまま、回収不能な窓を元の長さに戻せる。この step が落ちれば ID は既に出力にも artifact にも載っているので、既存の drop 経路がそのまま効く。

採らない場合は、handbook の「失敗したとき」に「検証待ちの最中に cancel した場合は ID が残らないので Portal の deployment 一覧から探す」の 1 行を足しておくと運用で回収できる。

### [🟡 Minor] publish job の 90 分の予算に対し、待ちの合計が最悪ケースで収まらなくなる

**該当箇所**: `.github/workflows/release.yml:431` (`timeout-minutes: 90`)

**問題点**: 待ちの上限はいずれも 30 分で、1 回の attempt が踏み得る待ちは「引き継いだ deployment の決着待ち (30) → FAILED で drop → 再 upload の決着待ち (30) → `wait-published` (30)」の 3 つが直列に並ぶ経路がある。待ちだけで 90 分となり、本体の作業 (実測 11 分) を足すと job の予算 90 分を超える。本変更以前は待ちの合計が最大 60 分で、予算内に収まっていた。

超過したときの実害は Minor 1 と同じ (job timeout は cancel 扱いで、後始末の step が走らない) 点で連動する。

**推奨修正**: 実発生の確率は低い (引き継いだ deployment が 29 分 VALIDATING の後に FAILED になる、という連鎖が要る) ため、対応は次のいずれかで足りる。(a) `timeout-minutes` を 120 に上げる、(b) 待ちの上限 `KSR_POLL_TIMEOUT_SECONDS` を workflow 側で短く上書きする、(c) 現状を許容し、job timeout が cancel であって後始末が走らないことを handbook に 1 行残す。

### [🔵 Suggestion] 自己テストの「待機中の進捗は標準出力に出ない」はトートロジーになっている

**該当箇所**: `scripts/release/central-portal.sh:457-458`

**問題点**: このチェックは `KSR_POLL_TIMEOUT_SECONDS=0` で呼ぶため、1 回目の照会の直後に上限判定に入って `fail` する。進捗行 (`central-portal.sh:238`) は 1 度も実行されないので、出力先が標準出力でも標準エラーでも結果は空になる。実測 (`>&2` を外すミューテーション) で、このチェックだけは NG にならず OK のままだった。

守りたい性質そのものは他のチェックが担保している — 同じミューテーションで「PENDING / VALIDATING を経て VALIDATED を返す」「FAILED / PUBLISHING / PUBLISHED は失敗にせず状態として返す」の 3 件が NG に転じることを実測済みなので、回帰検出力の穴ではない。名前と検査内容がずれている点だけの問題。

**推奨修正**: 上限を過ぎない設定 (`KSR_POLL_TIMEOUT_SECONDS` 既定のまま、interval 0) で 1 回以上ポーリングさせた実行の標準出力が決着状態の 1 行だけであることを確かめる形にする。あるいはチェック名を実際に検査している内容 (上限超過時に標準出力が空であること) に合わせる。

### [🔵 Suggestion] 上限超過の 2 つのチェックが 1 つの `arrange` を共有していて、台本の消費数に暗黙に依存している

**該当箇所**: `scripts/release/central-portal.sh:454-458`

**問題点**: `arrange` は VALIDATING を 2 件だけ積み、その後 `cmd_wait_validated` を 2 回呼ぶ。「1 回の呼び出しが台本を 1 件だけ消費する」ことに依存しており、ループの構造を後から変えると台本が尽きた側の経路 (モックが `return 1` を返し、状態が空文字になって `*)` 分岐へ落ちる) に入る。実測でも、上限判定を取り除くミューテーションを当てたとき、検出はできたものの経路は台本切れ側だった。

**推奨修正**: 2 つのチェックそれぞれの直前で `arrange` を呼び直す。台本の共有をやめれば、後からループを変えても各チェックが意図した経路を通る。

### [🔵 Suggestion] handbook の新しい行のうち FAILED の行が、表の列の意味 (再実行で起きること) と半分ずれている

**該当箇所**: `kasane/handbook/cross/release-procedure.md:154`

**問題点**: 表の列は「失敗した位置 | 再実行で起きること」だが、追加された FAILED の行の前半「後始末で deployment が削除され、NuGet は未 push のまま」は失敗した時点で起きることであって、再実行で起きることではない。直後の段落が同じ内容 (後始末で削除され ID も破棄される) を既に述べているため、重複でもある。

**推奨修正**: 前半を落として「upload からやり直す (NuGet は未 push なので同じ version で埋め直せる)」のように再実行側の記述へ寄せる。表の情報量は落ちない。

## アクションプラン

1. Minor 1 — 待ちを独立 step に出して `Upload deployment id` の後に置く (推奨) か、cancel 時の回収手順を handbook に 1 行足す
2. Minor 2 — Minor 1 の対応にあわせて、`timeout-minutes` の据え置き / 引き上げ / 待ち上限の短縮のいずれかを決める
3. Suggestion 1・2 — 自己テストの `[wait-validated]` 節の 2 点を整える (どちらも数行)
4. Suggestion 3 — handbook の FAILED 行を再実行側の記述へ寄せる

1〜4 はいずれも本変更の目的 (検証の決着を NuGet push より前で待つ) を損なわないので、まとめて次の 1 往復で片付けるか、Suggestion のみ見送るかはオーケストレーターの判断でよい。
