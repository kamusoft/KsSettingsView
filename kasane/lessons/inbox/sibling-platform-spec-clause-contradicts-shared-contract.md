---
scope: spec-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-20
last-seen: 2026-08-26
evidence:
  - implement-modern-style (Android spec の「Header / Footer 行は水平方向の inset 対象にもしない SHALL NOT」が、同 spec 冒頭の共有契約「sectionMargin は Section 単位 [Header・Cell 箱・Footer 一体] の外側余白 SHALL」および iOS spec の同契約・Scenario「sectionMargin は Header / Footer を含む Section 単位を包む」と矛盾。spec-review [自己 + 相方 second-opinion-spec-001] は検出せず、実装後に Header 文字と Cell 文字が 16dp ずれる platform 間視覚不一致として露呈)
  - retarget-docs-refresh-to-skills (「コード正の機械チェック」の「差分があれば該当 README / Skill ファイルを要追従リストに追加する SHALL」と「実行フラグ」の「--readme-only は skills/ 本体をスキップする SHALL」が交差ケースで衝突。spec-review [自己 + 相方 second-opinion-spec-001] は検出せず、実装レビュー [review-001 + 相方 code-001 の双方] で露呈しオーナー判断が必要になった)
---

## ルール文

spec-review では、条項単体の明確さで満足せず SHALL / SHALL NOT を**条項ペアで突き合わせて**矛盾を検算する。照合対象は (1) 同一 spec 冒頭の共有契約段落、(2) 相方 platform spec の同一契約 (cross 変更の場合)、(3) 横断的に適用される Requirement (実行フラグ・モード・全体規律) と個別機能 Requirement の交差ケース。矛盾条項は文言が明確なだけに単体では実装可能で、spec lint も Scenario 対応表もすり抜け、実装後の視覚不一致や実装フェーズでのオーナー判断差し戻しとして初めて露呈する。

## 経緯

- 2026-08-20 implement-modern-style: Android spec の Requirement「Modern の Section 箱描画」に「Section Header / Footer 行は箱に含めず、水平方向の inset 対象にもしない (SHALL NOT)」という条項があった。「箱に含めない」は探索決定どおりだが、「水平 inset 対象にもしない」は同 spec 冒頭の「sectionMargin は Section 単位 (Header・Cell 箱・Footer を一体とした表示単位) の外側余白 (SHALL)」と両立しない (外側余白なら H/F 行も水平成分だけ内側に入るはず)。iOS spec は同じ共有契約に加えて Scenario「sectionMargin は Header / Footer を含む Section 単位を包む」を持ち、iOS 実装は H/F を箱と水平で揃えた。Android 実装は SHALL NOT を文字どおり実装し、Header text が画面端 16dp・Cell title が 32dp と 16dp ずれる結果になった。提案フェーズの spec-review と相方レビュー (second-opinion-spec-001) はいずれも検出せず、実装ワーカーの報告で発覚した。
- 2026-08-26 retarget-docs-refresh-to-skills: 単一 capability の spec 内で、機能 Requirement「コード正の機械チェック」(差分があれば該当 README / Skill ファイルを要追従リストへ SHALL) とモード Requirement「実行フラグ」(--readme-only は skills/ 本体をスキップ SHALL) が「--readme-only 実行中にツールバージョン差分が Skill 導入節へ及ぶ」交差ケースで衝突。どちらの条項も単体では明確で Scenario 対応表も通過したが、提案フェーズの spec-review (自己 + 相方 second-opinion-spec-001) は交差を検出せず、実装レビュー (review-001 Major-2、相方 code-001 Major-2 の一致指摘) で露呈。解決にオーナー判断 (報告のみ = フラグ優先、deviation.md 記録) が必要になった。platform 間の共有契約に限らず、横断 Requirement × 機能 Requirement の交差でも同じ型が出ることを示す観測。
