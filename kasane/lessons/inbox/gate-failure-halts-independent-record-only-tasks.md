---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-15
last-seen: 2026-09-15
evidence:
  - maui-appearance-change-tracking (tasks 0.3 の切り分けゲートが「仮説が両方外れたら以降に進まず探索へ戻す」だったため、オーケストレーターがグループ 1〜6 を一括で止めた。しかし グループ 5 の spike (Section / Cell の element ツリー接続の可否、記録のみ・対応 Requirement なし) は反証された不具合前提と独立で、再探索の判断材料として proposal が明示的に残していたもの。オーナーが「逆になんでやらなかったんだ」と指摘して実施)
---

## ルール文

tasks のゲート (再現・切り分け・spike) が不成立で「以降に進まず探索へ戻す」に入ったとき、オーケストレーターは残りのタスクを一括で止めず、各グループを「反証された前提に依存するか」で仕分けする。前提に依存しないタスク (記録のみの spike・調査項目・回帰資産の追加のように対応 Requirement を持たない、または proposal の Non-Goal / 探索の判断材料として独立に置かれたもの) は差し戻しの前に実施し、結果を差し戻しの引き継ぎに含める。事後判定: 差し戻し報告に「未着手のタスク」として挙げたグループのそれぞれに、反証された前提への依存が 1 行で書いてある。

## 経緯

- 2026-09-15 maui-appearance-change-tracking: 0.3 の注記の文言「以降に進まず」を tasks 全体に適用した。spike 5.1 は「結果に関わらず本 change には同梱しない。成立していれば別 change として起票するかをオーナーが裁定する」と proposal / tasks が書いており、探索への差し戻しでこそ要る材料だった。Simulator / Emulator も起動済みで実施コストは低かった。
