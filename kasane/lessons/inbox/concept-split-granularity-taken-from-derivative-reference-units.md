---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - restructure-maui-api-concepts (散文 16,000 字超の maui-facade.md の分割粒度を決める際、利用者向け派生物 skills/kssettingsview-maui が同じ 1 本を cells / custom-cells / styling / updates の 4 references に翻訳して参照していることを skills/.manifest.json から読み取り、「読者が主題ごとに求める単位」の実測として 4 分割を提案。オーナーが推奨どおり採用し、初見可読性レビューでも入口からの導線は問題なしと判定)
---

## ルール文 (候補)

肥大した概念ファイルの分割粒度を提案するときは、その概念を源泉とする派生物 (利用者向け Skill の references・manifest の targets 逆引き) がどの単位で参照しているかを先に調べ、派生物が既に複数の単位へ翻訳しているならそれを「参照される単位」の実測として分割案の根拠に載せる。節の字数だけで割らない。

## 経緯

- 2026-09-05 restructure-maui-api-concepts: 節ごとの散文量 (公開 API の形 5,757 字等) だけでは境界が決めにくかったが、manifest の targets 逆引きで派生物側の 4 単位が見え、案の比較表で「skills 派生物との対応」を軸に加えられた。
