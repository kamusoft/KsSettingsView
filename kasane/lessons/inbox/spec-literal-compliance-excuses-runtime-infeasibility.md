---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - android-numberpicker-modern-ui (有効入力で OOM 級の eager 生成を「spec が定めた契約どおり」を理由に Suggestion へ降格。相方は同じ箇所を SHALL の実行時不成立 = 仕様違反として Major 判定し、突き合わせで Major 確定)
---

## ルール文

「spec の文言どおりの実装」を、実行時に成立しない実装 (OOM・メインスレッドの長時間停止など資源限界による不成立) の免罪符にしない。spec が有効と定める入力の全域で実際に提示・動作できるかを資源面まで評価し、成立しない域があるなら仕様違反 (Major 相当) として指摘する。閾値自体の見直し提案とは区別して扱う。

## 経緯

- 2026-08-02 android-numberpicker-modern-ui: 候補列の eager List 生成は `count == Int.MAX_VALUE` の有効入力で確実に巨大確保となるが、ホスト review-001 は「spec が定めた提示閾値どおりで旧実装も同じ」を理由に Suggestion へ降格。相方 (codex) は「Int 上限以下なら提示する SHALL を実行時に満たせない」として Major と判定し、突き合わせ (second-opinion-002) で Major が確定。修正は index ベースの遅延生成 + 上限ちょうど・Int 全域の境界テスト。
