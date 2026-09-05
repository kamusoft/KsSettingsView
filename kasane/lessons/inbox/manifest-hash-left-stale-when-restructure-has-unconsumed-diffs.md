---
scope: process
kind: success
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - restructure-maui-api-concepts (concepts の再構成後に skills/.manifest.json を合わせる際、記録済みハッシュを各 commit の内容と突き合わせて 2026-09-03 時点のものと判明。その後の 2 commit (配布状況の追記・用語統一) が docs-refresh 未消費だったため、ハッシュを現在値へ更新せず targets の参照元だけを付け替え、新規 3 本のハッシュも足さずに「新規追加」として次回検出させる形にした。網羅検査は OK)
---

## ルール文 (候補)

concepts の分割・改名で `skills/.manifest.json` を手で合わせるときは、`targets` の参照元だけを付け替え、`concepts` のハッシュは docs-refresh が Skill 本文を追従させた時にだけ更新する。付け替え前に記録済みハッシュが直前の commit 内容と一致するかを確かめ、一致しなければ未消費の差分があるものとして据え置き、新規ファイルのハッシュも足さない (足すと未消費の差分が消費済み扱いになり、次回の docs-refresh が検出しない)。

## 経緯

- 2026-09-05 restructure-maui-api-concepts: ハッシュを現在値に揃えるのが自然に見えたが、`git log` の各版をハッシュ化して照合したことで 09-04 / 09-05 の未消費差分が見つかり、据え置きの判断に至った。
