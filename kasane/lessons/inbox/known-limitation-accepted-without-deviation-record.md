---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-02
last-seen: 2026-08-05
evidence:
  - datepickercell-color-adjust (review-002 がカスタム calendar style での初回誤分類を「spec に照らして受容可能」と deviation 記録なしで判定。相方は記録を受容の条件として Major を維持し、オーナー裁定は「deviation 記録で受容」— 相方側の手続き論が採用された)
  - fix-replace-section-header-refresh (実装フェーズで判明した既知の制限 2 件 — `.full` での `.view` accessory 中身差し替えの非反映・view accessory を持つ Section の `replaceSection` が内容不変でも全 Cell reload — が当初どのアーティファクトにも記録されず、review-001 Minor 2 件で発覚。exploration.md への追記で決着したが、review 追記は「deviation.md の方が蒸留機構に乗りやすい」と申し送り)
---

## ルール文

実装が spec (デルタスペック・部位対応表等の規範) の規定どおりに動作できない既知の限界を「受容可能」とレビュー判定するときは、deviation.md への記録 (またはオーナー合意による spec 適用範囲の変更) を受容の条件として要求する。レビュー判定文中の受容宣言は合意記録の代替にならない — 記録のない乖離だけが問題、の裏返しとして、受容には記録が要る。

## 経緯

- 2026-08-02 datepickercell-color-adjust: ホスト review-002 は「カスタム calendar style 構成での通常日付マスの初回誤分類」を Scenario の GIVEN 外・THEN 非違反として deviation 不要の受容可と判定。相方 (second-opinion-003) は「部位対応表が通常日/選択日/今日を別ロールと規定する以上、受容には deviation 記録か spec 適用範囲変更が必要」と Major を維持し、2周連続残存の収束シグナルで NEEDS_DISCUSSION → オーナーが「deviation 記録で受容」を決定。技術的な追加実装 (selection 値 + adapter 位置の状態導出) は不採用で、手続きの整備のみで決着した。
- 2026-08-05 fix-replace-section-header-refresh: 合意スコープ内で意図的に残した既知の制限 (view 形式 accessory の等価比較不能に由来する 2 件) が実装完了時点でどのアーティファクトにも記録されておらず、review-001 が「このまま蒸留に進むと情報が失われる」と Minor 2 件で指摘。exploration.md への追記 (日付・出典明示の append) で決着したが、review 追記は「S 級でも既知の制限は deviation.md に置く方が蒸留機構 (ksn-distill が deviation.md を読む建付け) に乗りやすい」と申し送った。蒸留時は concepts (display-state-synchronization.md) へ反映して回収。
