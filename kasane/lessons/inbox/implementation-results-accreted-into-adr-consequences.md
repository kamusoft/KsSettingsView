---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-07
last-seen: 2026-09-07
evidence:
  - restore-adr-decision-records (decisions/ 96 本のうち 8 本 22 行で、本文に日付付きの「(YYYY-MM-DD 追記)」マーカーが累積し ADR が変更ログとして読める状態になっていた。cross/ADR-0020 は Consequences が 3714 字 / 15 箇条で本文の約 6 割を占め、初回リリースの所要 39 分・Trusted Publishing の初回 login・`--skip-duplicate` の冪等性など「今どうなっているか」を含み、うち 8 要素は handbook `cross/release-procedure.md` や concepts と二重管理になっていた。オーナー指摘「履歴代わりに使うのは NG」で発覚)
---

## ルール文 (候補)

ADR の Consequences に書くのは**決定から導かれる影響**だけにする。実装や運用で分かった事実 — 実測値・所要時間・外部 API の制約・積み残しの消化状況・他 ADR との関係 — は、出典が「実装結果」であっても Consequences に積まない。現況は concepts、作業の決まりは handbook、ADR 間の関係は frontmatter と index が持つ。

proposed の ADR を実装後の視点で更新するときも同じで、日付マーカー「(YYYY-MM-DD 追記)」を付けて足すのではなく、決定が 1 つの文章として読める形に書き直す。いつ何が決まったかの経緯は出典が指す change / roadmap の記録が持つ。

事後判定: ADR の本文 (frontmatter と footer を除く) に日付付きの追記マーカーが無く、Consequences の各項が「この決定を採ったから起きること」として読める。

## 関連

[[project-adr-carves-exception-into-harness-rule]] と対をなす — あちらは「ADR にすべきでないものを ADR にした」、こちらは「ADR に書くべきでないものを Consequences に書いた」。どちらも ADR が決定の記録であることの取り違え。

## 退役の条件

本パターンはハーネス側の対応までの繋ぎ。ksn-distill Step 3c の「実装を経て判明した帰結があれば Consequences に追記してよい (出典: 実装結果)」が本症状を招いた面があり、ハーネス本体側で対応する方針がオーナーから示されている (2026-09-07)。ハーネス側に機械検査または規律が入ったら、本パターン (昇格済みなら当該ルール) は ksn-drift の棚卸しで降格候補になる。

## 経緯

- 2026-09-07 restore-adr-decision-records: 検出は機械的に可能だった (本文から frontmatter と footer を除き、同一行に日付と「追記」がある行を拾う試作を 96 本へ流して 22 行を検出、偽陽性 0 件)。ただし表層の追記マーカーが検出できるだけで、本質である「Consequences に何を書くか」の判断は機械では判定できない。プロジェクト側の lint 化はハーネス本体で対応する方針のため見送り、本ルールで押さえる。
