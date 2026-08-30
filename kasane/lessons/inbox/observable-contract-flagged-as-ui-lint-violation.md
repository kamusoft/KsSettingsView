---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-01
last-seen: 2026-08-01
evidence:
  - fix-android-cell-width-allocation (相方 spec-review が「主行内」「コンテンツ幅」等の配置契約を UI lint 違反と Major 指摘し、突き合わせで降格)
---

## ルール文

デルタスペックの UI lint 違反を指摘するのは、px 値・色・余白などの視覚パラメータ生値やコントロール配置の見た目指定が書かれている場合に限る。「どの要素がどの領域の幅を占めるか」のような測定テストの指標になる観察可能な配置契約は、それ自体が機能契約であり UI lint の対象外 — これを違反として指摘しない。

## 経緯

- 2026-08-01 fix-android-cell-width-allocation: 相方 (codex) の spec-review が「主行内」「trailing 側」「コンテンツ幅」「末尾省略」等の記述を「Kasane の UI lint に反する」と Major 指摘。突き合わせで「幅配分・2 系統配置は本 change の機能契約そのもので、UI lint が禁じる視覚パラメータ生値 (px/色/余白) は含まれない。アーカイブ済み前例 (fix-cell-accessory-vertical-fill) も同形式で承認済み」として降格した。UI lint の線引きは「観察可能な状態遷移・配置関係 = 可 / 視覚パラメータの生値・見た目指定 = 不可」(ksn-core delta-spec.md)。
