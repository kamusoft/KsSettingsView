---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-03
last-seen: 2026-08-03
evidence:
  - add-cell-types-custom (オーケストレーターが ksn-ui-implementer への制約に「ui/brief.md を編集しない」と一律に書いたため、ksn-ui が完了条件として要求する「照合結果 / トークン候補 / 合意済み妥協」の brief.md への記録がワーカー側で実施できず、verification/index.md に代替記載＋オーケストレーターへの転記依頼という二度手間になった。ksn-ui は「ui/ は足場。実装中の brief.md への追記は自由」と明記している)
---

## ルール文

ワーカーへ「足場アーティファクトの書き換え禁止」を渡すときは、禁止対象を **spec / proposal / design / mock に限定して列挙する**。ワーカースキルが完了条件として書き込みを要求しているファイル (ksn-ui の `ui/brief.md` への照合結果・トークン候補・合意済み妥協など) を巻き込まないこと。逆流修正の禁止は「実装の都合で仕様を書き換えない」ことが目的であり、ワーカースキルが規約として求める記録はこれに当たらない。禁止範囲を決める前に、渡すワーカースキルが**どのファイルへの書き込みを求めているか**を確認する。

## 経緯

- 2026-08-03 add-cell-types-custom: ksn-ui-implementer への制約に `ui/brief.md` を含めて一律禁止にしたため、ワーカーは規約上必要な照合結果を `ui/verification/index.md` に書いた上で「brief.md への転記が必要」と申し送りする形になった。オーケストレーターが ksn-ui SKILL.md を確認して誤りに気づき、以後のパッケージでは「`ui/brief.md` への追記は ksn-ui の規約どおり許可する」と明示して訂正した。
