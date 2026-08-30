---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-23
last-seen: 2026-08-23
evidence:
  - fix-cell-icon-size-parity (主行の幅配分を「title を守り valueText を省略する」へ入れ替える Requirement が、同じ title 領域の余白に依存して効く既存 public API `ButtonCell.titleAlignment` の見え方 (aux ありの通常レイアウトで CENTER / END が視覚に出なくなる) を Scenario でも Impact でも扱っておらず、実装ワーカーの報告でオーナー判断へ持ち越された)
---

## ルール文

レイアウト資源の配分規則 (幅・高さの取り合い、weight / 優先度 / コンテンツ幅の別) を変える Requirement をレビューするときは、**その資源に配分された領域の内側で効く既存 public API** を列挙して (alignment・gravity・中央寄せ・省略位置など、配分結果として残る余白を前提に観測される API)、新しい配分でそれぞれの見え方が変わらないかを1つずつ確認する。変わるものは Scenario で扱うか、少なくとも proposal の Impact に「見た目が変わる利用者」として明記する。配分規則の Requirement は単体では自然に完結して読めるうえ、既存契約の文言 (「残った領域内で alignment される」等) にも違反しないため、spec lint も Scenario 対応表もすり抜け、実装フェーズのオーナー判断へ持ち越される。

## 経緯

- 2026-08-23 fix-cell-icon-size-parity: Android の主行配分を iOS へ揃える Requirement (core/ADR-0026) で title を `wrap_content` (コンテンツ幅) へ変えた結果、`ButtonCell` の通常レイアウト (aux = valueText / icon / hintText のいずれかあり) では title 領域に余白が残らず `titleAlignment` の CENTER / END が視覚に出なくなった。concepts の既存文言「行内 trailing があれば残った title 領域内で alignment される」には違反しないため spec-review・相方 second-opinion-spec-001 とも検出せず、実装ワーカーが「spec が沈黙する利用者可視の変化」(process L-001) として報告して初めて表面化した。オーナー裁定は受け入れ (iOS が元から同挙動で parity 目的に合致) で、deviation 記録と concepts 明確化の申し送りに落着。提案段階で列挙していれば Impact の「見た目が変わる利用者」に載せられた。
