---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-12
last-seen: 2026-09-12
evidence:
  - install-examples-and-release-notes (accepted な cross/ADR-0019 の事実誤認を本文の直接修正で直すことをオーナーが裁定した [「決定を修正するわけではないので直接修正、訂正の注記も不要」2026-09-11] が、裁定が change の成果物に記録されなかった。新鮮な文脈で読む相方の spec レビューが「accepted ADR の本文は不変という規律に反する」と Major で指摘し、突き合わせで「一般規則より裁定が優先する」として却下する周回が生まれた)
---

## ルール文 (候補)

ハーネスの一般規則から外れる形を成果物に残すオーナー裁定が出たら、**その場で change の成果物へ 1 行記録する** (実装前なら exploration.md の決定事項、実装中なら deviation.md)。記録先には、外れる規則・裁定の内容・裁定日を書く。

レビュアーは新鮮な文脈で成果物と diff だけを見るため、会話で出た裁定を知り得ない。記録が無いと、同じ箇所が規約違反として毎回指摘され、指揮側が突き合わせで却下する往復にレビュー 1 周ぶんの紙幅が費やされる。

事後判定: 一般規則と異なる形をした成果物について、その根拠 (裁定日とオーナー判断の要旨) が change 内のいずれかのファイルから読める。

## 経緯

- 2026-09-12 install-examples-and-release-notes (蒸留時の補完): SwiftPM の prerelease 解決についての事実誤認が accepted な cross/ADR-0019 の本文にあり、オーナーが「決定を修正するわけではないので直接修正でよい、訂正の注記も不要」と裁定した。裁定はどの成果物にも残らず、相方 spec レビュー (second-opinion-spec-001) が accepted ADR の本文不変の規律に照らして Major 指摘。突き合わせでは前半を却下・後半 (proposed 側に残った古い記述) を採用しており、指摘自体は半分当たっていた。記録があれば前半は上がらなかった。関連: 受容の側に記録を要求する型として [[known-limitation-accepted-without-deviation-record]]。
