---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - proofread-skills-wording (ja/en が 1:1 対応する skills/ で、ja 側の見出し言い換え (Classic / Modern の切り替え) をユーザー指示どおり ja だけに入れ、summary.md には「en は ja を正として追従」と書いて独立レビューへ出した。review-001 が「en は元の見出しのままで ja と別の切り口、en 3 platform の不揃いも温存、サマリの方針と不整合」と Minor で指摘。オーナーに諮って en も揃えることになり 1 サイクル追加)
---

## ルール文 (候補)

対で保守される文書群 (ja/en、platform 別の同構成ファイル) の片側だけを編集して確定するときは、相方側の同じ箇所を「揃えた / 意図的に据え置いた (理由)」のどちらかとして summary の据え置き一覧に明記してからレビューへ出す。ユーザーの指示範囲が片側でも、相方側が未追従である事実を summary に書かずに「追従済み」と要約しない。

## 経緯

- 2026-09-05 proofread-skills-wording: ユーザーは ja ベースで校正を進め、en は最後に "row" だけ一括対処する方針だった。指揮側は見出しの言い換えが ja 固有の判断 (「伝わりにくい」) だったため en を触らず、summary では en を「追従済み」と要約した。レビュアーは summary と実態のずれとして指摘し、オーナー判断で en の見出しも ja と同じ切り口に統一した。
