---
scope: process
kind: success
severity: normal
count: 3
first-seen: 2026-08-20
last-seen: 2026-09-05
evidence:
  - add-maui-nuget-distribution (相方コードレビューがホスト側レビューの見逃し 2 件を検出 — 名前空間改名で生じる MAUI Controls との型名衝突が deviation では SwitchCell 1 件のみ記録で EntryCell が数え漏れ、Scenario「README の例による消費者ビルド」が要求する「XAML と MauiProgram の登録」の例のうち MauiProgram 側が README に散文しか無く証跡も登録コードの出所を示していない。いずれも採用し 1 周目で修正)
  - add-maui-modern-style (相方コードレビューがホスト側レビューの見逃した証跡完全性の欠け 2 件を検出 — task 5.1 の視覚照合が 12 組要求に対し 10 組で Classic × Bordered が両 OS 分欠け [この組だけが「Classic は border 指定を無視する」を直接検証する]、task 4.1 の sample-parity 対応表が change 配下に成果物として不在。いずれも採用され 1 周目で修正、parity-table.md と 24 枚のスクショが揃った)
  - fix-default-colors-dark-appearance (相方コードレビューがホスト側レビューの見逃し 2 件を Major で検出 — (1) `ksThemedContext()` の早期 return で外観変更後に開き直した選択面のテーマ属性が古い外観のまま残る実欠陥 [実経路プローブで colorSurface が旧値のままと実証、修正]、(2) MAUI Sample の `MainActivity.OnConfigurationChanged()` が `Recreate()` を呼ぶため spike 注記と brief.md の「Activity 再生成なしで表示中に描き替わった」が現行 Sample では証明できない証跡主張の矛盾。ホスト側は「ContextThemeWrapper が resources を委譲するため正しい」と判定して (1) を通し、(2) は成果物の主張と Sample コードを突き合わせていなかった)
---

## ルール文

タスクが「全組合せ照合」「成果物を作る」を要求している場合、レビューは組合せの員数 (要求組数と証跡数の一致) と成果物ファイルの実在をアーティファクト側で数え上げて確認する — 実装内容の正しさの確認だけでは、証跡・成果物の欠けは検出できない。

## 経緯

- 2026-08-20 add-maui-modern-style: ホスト側 review-001 は実装・テスト・視覚照合の内容確認は網羅していたが、証跡の員数 (2 OS × 2 style × 3 preset = 12 組) の突き合わせと task 4.1 の成果物実在確認を落とした。相方 (codex) は tasks.md の要求文言から機械的に数え上げて両方を検出。突き合わせで双方一致の指摘 (Critical/Major) に加え、相方単独の指摘 2 件がそのまま採用された — セカンドオピニオン並走が証跡完全性の観点で相互補完した成功例。
- 2026-09-02 add-maui-nuget-distribution: ホスト側 review-001 は pack / CPM / ガードの再実行検証を網羅していたが、公開型の衝突の員数 (deviation の 1 件が全てか) と README の例が Scenario の要求要素 (XAML + MauiProgram) を両方含むかの突き合わせを落とした。相方 (codex) は spec の要求文言と MAUI の公開型から機械的に照合して両方を検出。
- 2026-09-05 fix-default-colors-dark-appearance: ホスト review-001 (Minor 3) と相方 second-opinion-code-001 (Major 2) が同一入力で割れ、相方の Major 2 件がいずれも採用・実証された。(1) は「Configuration は最新でもテーマ属性は生成時に固定される」という ContextThemeWrapper の性質で、ホストは前半だけを確認して後半を見なかった。(2) は成果物 (spike 注記・照合記録) の「再生成なし」という主張を、同じリポジトリの Sample コード (proposal Non-Goals にも書かれた `Recreate()` 回避策) と突き合わせれば机上で反証できた。証跡の主張はコードと突き合わせて初めて証跡になる、という観測。
