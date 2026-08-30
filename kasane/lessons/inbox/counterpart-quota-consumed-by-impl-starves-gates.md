---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - consolidate-readmes-and-contribution (workers.impl / verify / scout / extract を counterpart に寄せた構成で、L 級の実装を 5 ラウンド委譲した後にレビューゲートへ到達したところ、相方 (codex) が second-opinion の実行途中で利用枠の上限に達し判定を出せず停止。同じ枠を使う verify も同時に実行不能になった。品質ゲート 2 つが同時に落ちた一方、実装そのものは完了していた)
---

## ルール文 (候補)

`workers.impl: counterpart` のように**実装を相方に寄せる構成**では、相方の利用枠を実装が先に食い潰し、後段の品質ゲート (`second-opinion` / `verify`) が枠切れで実行できなくなる。ゲートは実装より後に来るぶん、枯れたときの損失が大きい (実装はやり直せるが、ゲートを飛ばした変更はゲートを飛ばしたまま完了してしまう)。次のいずれかで守る:

- **ゲートを host 側に寄せる** — `workers.verify: host` にする。`second-opinion` はクロスモデルであること自体が目的なので host へ移せない。その分 `verify` を逃がして枠を空ける
- **実装の委譲回数を絞る** — タスクグループをまとめ、停止・再委譲の往復を減らす (往復 1 回ごとに相方はコンテキスト全体を読み直す)
- 枠切れが起きたら、ゲートを黙って飛ばさず**未実施として deviation に記録**し、枠回復後に単独実行できる形で申し送る

## 経緯

- 2026-08-29 consolidate-readmes-and-contribution: 実装 5 ラウンド (うち 2 ラウンドは sandbox 制約による停止・再委譲) を counterpart で消化した後、second-opinion (code-review) が 218,352 tokens を消費した時点で枠切れ。判定なしで exit 1。`workers.verify: counterpart` も同じ枠を使うため verify も同時に不能となり、ホスト側へ読み替えて実施した。相方レビューは代替が無いため未実施のまま記録した。
