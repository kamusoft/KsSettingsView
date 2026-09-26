---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - ios-appearance-test-observe-rendered-values (探索で scout が「行背景について trait 変化を購読している箇所は無い」と報告し、これを「製品は外観変化で行背景を再適用していない」と言い換えて exploration.md の事実と判断材料に書いた。実際には外観切替で UIKit が行の構成更新 [`configurationUpdateHandler`] を呼び、平常色を入れ直していた。実装時のミューテーションで露見し deviation.md に訂正を記録。結論は変わらなかった)
---

## ルール文 (候補)

「〜の変化で X を再設定していない」と書く前に、明示的な購読 (`registerForTraitChanges` / listener 登録等) が無いことに加えて、framework が変化時に呼ぶ既存のコールバック (UIKit の構成更新・`layoutSubviews`・再 bind 等) の中で X に触れていないかを確かめる。購読の不在は再設定の不在を意味しない。確かめられなかったら「明示的な購読は無い」と観測どおりに書き、再設定の有無は未確認として残す。事後判定: 不在を述べた記述が、確かめた範囲 (購読のみ / コールバックまで) を明記している。

## 経緯

- 2026-09-26 ios-appearance-test-observe-rendered-values: 調査報告の「購読が無い」を「再適用が無い」へ広げて探索メモに書き、「塗り直しを止める改変は成立しない」という論点の整理にも使った。昇格済み process L-006 (不在の断定は対象を特定できる検索を通してから) に近いが、機序は「検索を通していない」ではなく「検索した対象 (購読) より広い主張 (再設定全般) を書いた」
