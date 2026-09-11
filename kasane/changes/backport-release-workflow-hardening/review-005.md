# レビュー結果: backport-release-workflow-hardening (005 回目)

**日付**: 2026-09-11
**判定**: APPROVED

## サマリー

前回 (004 回目) 以降の変更は `scripts/release/deployment-handover.sh` と `.github/workflows/release.yml` の `Drop pending deployment` step の 2 箇所のみ。`writeback` の判定語を `clear` / `store` の 2 語に絞り、ID が空の呼び出しを契約違反として失敗させ、workflow 側を `case` にして想定外の語を `::error::` で落とす形にしたもの。step の本体を切り出して stub 付きで 5 ケース実行し、`clear` が返る条件・後段の連携・契約違反の枝のいずれも壊れていないことを実測した。Critical / Major に相当する実害は無い。

これは最終確認のレビューであり、新規 Suggestion の掘り起こしは行っていない。既存の未修正 Suggestion 5 件はオーナー報告事項として据え置く方針を前提とし、判定の材料にしていない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規・改稿されたコメント (`deployment-handover.sh` の使い方欄と `cmd_writeback` 内、`release.yml` の `Drop pending deployment` のコメント) に作業文書のパス・変更識別子・通番・デルタスペック構文キーワードの混入が無いことを確認。参照しているのは `scripts/release/deployment-handover.sh` というリポジトリ内のソースファイル名のみで、規約が許容する範囲。`comment-policy-lint.py` も禁止 0 件
- `kasane/handbook/cross/test-execution.md` (適用のきっかけ: テスト実行・テスト結果の報告) — 実行件数を併記する要求に従い、下表に件数を明記する
- `kasane/lessons/code-review.md` の重点観点 [L-001] (ミューテーションでアサーションの検出力を実測する) を今回も主な手法として適用した

## 実行結果

lint job が回す検査を手元で全件実行し、すべて成功 (終了コード 0)。

| 検査 | 結果 |
|---|---|
| `scripts/release/deployment-handover.sh --selftest` | 21 件 OK / 0 件 NG |
| `scripts/release/wait-for-registries.sh --selftest` | 61 件 OK / 0 件 NG |
| `scripts/release/central-portal.sh --selftest` | 52 件 OK / 0 件 NG |
| `python3 scripts/release/set-readme-version.py --selftest` | 33 件 OK / 0 件 NG |
| `python3 scripts/release/check-time-budget.py` | 合計 217 分 / 上限 240 分 (余裕 23 分) |
| `python3 scripts/release/check-publish-step-order.py` | 順序正常 |
| `python3 scripts/release/check-publish-step-order.py --selftest` | 7 件 OK / 0 件 NG |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 (検査対象 787 ファイル) |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | 検出なし |

## 1. `clear` が返る条件は実経路で変わっていない

`cmd_writeback` の判定は、ID が非空である限り「`state` が `NOT_FOUND` なら `clear`、それ以外は `store`」の一点のみ。ID の値そのものは判定に使われず、非空判定だけに使われる (`scripts/release/deployment-handover.sh` の `cmd_writeback`)。したがって非空 ID の入力に対する `clear` / `store` の分かれ目は今回の変更前と同一である。

呼び出し側が常に非空を渡すという主張も確認した。`.github/workflows/release.yml:874` の `if:` は

```
failure() && (steps.maven.outputs.deployment-id || steps.recover-deployment.outputs.deployment-id) != ''
```

で、GitHub の `||` は先に真となる値を返し両方偽なら後者を返すため、この式が真になるのは少なくとも一方が非空のときだけ。`env` の `KS_DEPLOYMENT_ID` は同じ式で組み立てられているので、step が動く限り非空になる。

空白だけの値が渡る余地も無い。ID の出所は 2 つで、いずれも空白を含まない。

- 引き継ぎ経由 — `inspect` が `tr -d '[:space:]'` で全空白を落とした値を output に載せる
- upload ログ経由 — `grep -oiE 'deployment id: [0-9a-f-]{36}'` で抜いた UUID (`.github/workflows/release.yml:662` / `:698`)

実測 (step の `run` を YAML から切り出し、`central-portal.sh` を stub にして実行):

| `status` の応答 | ID | 終了コード | `GITHUB_OUTPUT` | 引き継ぎファイル |
|---|---|---|---|---|
| `NOT_FOUND` | 非空 | 0 | `cleared=true` | 空 (改行のみ) に上書き |
| `PUBLISHING` | 非空 | 0 | (なし) | 手つかず |
| `VALIDATED` | 非空 | 0 | (なし) | 手つかず |
| 空文字 | 非空 | 0 | (なし) | 手つかず |
| `NOT_FOUND` | 空 | 1 | (なし) | 手つかず |

## 2. 空 ID をエラーにする判断について (判断の所見)

**結論: 妥当。ただし実装者の理由づけは「`clear` を返してはいけない」までを支える根拠であり、「失敗させる」ところは別の理由で立っている。** 両方とも成立するので修正は不要だが、切り分けて記録しておく。

`writeback` は引き継ぎファイルに書かない。書くのは workflow 側で、`clear` のときだけ空で上書きする。したがって「他人の引き継ぎを消す」危険が現実化するのは**空 ID に対して `clear` を返した場合だけ**であり、そこは実装者の言うとおり。空 ID で `NOT_FOUND` が来ても `clear` にしてはならない理由も明快で、`state` は ID を照会して得た値である以上、ID が空なら `NOT_FOUND` は「何も照会していない」以上の意味を持たない (空の ID に対する `NOT_FOUND` は、消えたことの確認ではない)。

一方「失敗させる」か「黙って `store` (現状維持) を返す」かは、どちらでも引き継ぎファイルは壊れない。ここを失敗にした判断の実質的な根拠は、消える方の副作用ではなく**呼び出し側のゲートが外れた事実を隠さないこと**にある。この選択のコストは小さい。

- 実経路では到達しない (上の `if:` のゲート)。仮にゲートを外す改変が入っても、同じ step の先頭にある `central-portal.sh drop ""` が先に落ちるので、`writeback` の枝は二重の防御として働く
- `Drop pending deployment` は `failure()` の後始末 step であり、ここが赤くなっても既に失敗している job の表示に注釈が 1 つ増えるだけ。後続の `Clear stored deployment id` は `cleared` が立たないので飛ぶ (実測済み)

別の読み方としては「`keep` を残し、workflow 側で明示的に何もしない語として扱う」も選べたが、`store` と workflow 側の効果が同一 (どちらも引き継ぎに触れない) なので、到達しない第 3 の語を維持する利得は無い。今回の 2 語化はスコープを増やさない方向の整理として適切。

## 3. `Drop pending deployment` → `Clear stored deployment id` の連携

`cleared=true` を `GITHUB_OUTPUT` へ書くのは `clear` の枝だけで、`store` と `*)` の枝は書かない。`Clear stored deployment id` の `if: failure() && steps.drop-deployment.outputs.cleared == 'true'` は、上表のとおり `store` のとき不成立 (output が空) になる。`clear` の枝では `mkdir -p` → `printf '\n' >` → `cleared=true` の順で、出力を立てる前にファイルが確実に用意されるため、`if-no-files-found: error` に引っかかる並びにはなっていない。

`*)` の枝も実測した。`writeback` の代わりに `keep` を出す stub を置くと、終了コード 1・`::error::引き継ぎの扱いを判定できない: keep`・`GITHUB_OUTPUT` は空・引き継ぎファイルは手つかず、で止まる。

## 4. `inspect` への影響

`cmd_inspect` は今回の変更の対象外で、`writeback` と共有しているのは `handover_path` / `HANDOVER_FILE_NAME` / `fail` だけ。いずれも変更されていない。`DEPLOYMENT_NOT_FOUND` は `writeback` からのみ参照される。自己テストの `inspect` 側 11 件 (引き継ぎあり 3 / 初回なし 4 / 読み込み失敗 4) はすべて通り、後述のミューテーションで検出力も確認した。

## 5. 契約違反の枝で引き継ぎファイルが壊れないか

壊れない。`cmd_writeback` はそもそもファイルを開かず、判定語を標準出力へ出すだけ。契約違反では `fail` が `::error::` を stderr へ出して `exit 1` するので標準出力には何も出ない。workflow 側は `decision="$(... writeback ...)"` の代入で受けており、`set -euo pipefail` 下では代入の終了コードがコマンド置換のものになるため、`case` に入る前に step ごと終了する。実測でも引き継ぎファイルは前の内容 (`PREV-HANDOVER-ID`) のまま残った。

## 6. 自己テストの検出力 (ミューテーション実測)

作業ツリーは触らず、scratchpad の複製へ変異を入れて自己テストを走らせた。空回りは無い。

| 変異 | 落ちた検査 |
|---|---|
| 1. 空 ID のエラーを外して `store` へ落とす | NG 2 件 (「ID を持たない呼び出しは判定を返さず失敗する」「空白だけの ID も失敗にする」) |
| 2. 空 ID で `clear` を返す | NG 2 件 (同上) |
| 3. `NOT_FOUND` でも `store` を返す | NG 1 件 (「消えた deployment の ID は引き継がない」) |
| 4. 未知状態を `clear` にする (`store` の既定を壊す) | NG 7 件 (`PUBLISHING` / `PUBLISHED` / `VALIDATED` / `FAILED` / `PENDING` / `VALIDATING` / 状態不明) |
| 5. `inspect` で空白だけの内容を `present=true` に倒す | NG 1 件 (「空白だけの内容は引き継ぎなし」) |

`keep` の削除についても、`store` を期待する 7 状態の検査 (変異 4 で全件落ちる) が「消えた確認が無いときは必ず `store`」を押さえているため、第 3 の語が戻ってくれば検出される。`keep` の文字列はスクリプト・workflow・ci.yml・tasks.md のいずれにも残っていない。

## 足場の凍結

前回レビュー (`review-004.md` 19:01) 以降に更新されたのは `scripts/release/deployment-handover.sh` (19:10) と `.github/workflows/release.yml` (19:11) のみ。`proposal.md` (16:00) / `specs/release-workflow/spec.md` (16:20) / `exploration.md` (16:21) / `tasks.md` (17:44) / `deviation.md` (18:14) はいずれも更新されておらず、`specs` / `proposal` / `exploration` は HEAD との差分も無い。実装中の書き換え (逆流) は無い。

## 指摘事項

Critical / Major に相当する実害は無し。今回の修正に起因する新規の指摘は無い。

## アクションプラン

1. なし (実装側の対応は不要)
2. `tasks.md` 5.2 (`dry-run` での release 起動) の扱いをオーナーに諮る — この change の判定材料にはしていない

## 確認した観点 (指摘に至らなかったもの)

- `writeback` の呼び出し元の網羅 — `deployment-handover.sh writeback` を呼ぶのは `.github/workflows/release.yml:888` の 1 箇所のみ。`inspect` は `:514` の 1 箇所で、`check-publish-step-order.py` がその step の位置を検査する
- 契約違反が後段を止めないか — `Drop pending deployment` が落ちても、その後の step は `Clear stored deployment id` (`cleared` が空なので skip) だけで、job は既に失敗している
- `status` の照会自体が失敗したときの向き — `set -e` により `state=` の代入で step が終わり、引き継ぎは手つかず (ID を残す側に倒れる) になる。安全側
- 使い方の説明と実装の一致 — 冒頭の使い方欄が挙げる判定語は `clear` / `store` の 2 語で、`main` の `writeback` は引数 2 個を要求する。語・引数数とも実装と一致
- 引き継ぎが古い ID を指したまま残る経路 (drop の後に再 upload が ID を得られず失敗する等) — 次の attempt は `wait-validated` が `NOT_FOUND` を返す分岐で ID を捨てて再 upload へ進むため自己回復する。今回の変更で生じた経路ではない
