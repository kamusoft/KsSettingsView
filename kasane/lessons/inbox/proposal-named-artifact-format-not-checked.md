---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-05
last-seen: 2026-08-05
evidence:
  - add-maui-native-bridge (proposal / tasks が名指しする「XcodeProject / AndroidGradleProject 形式の Binding csproj」と実装 (Exec 方式) の不一致を review-001 が見逃し、second-opinion-003 が無記録逸脱として検出)
---

## ルール文

proposal「What Changes」/ tasks が成果物の形式・方式を名指ししている場合 (ビルドアイテム形式・プロジェクト構成・採用ライブラリ等)、レビューはその名指しと実装の一致を照合項目に含め、deviation.md に記録のない不一致は無記録逸脱 (Major) として扱う。挙動・テストの検証が通っていることは、アーティファクト整合の代替にならない。

## 経緯

- 2026-08-05 add-maui-native-bridge: tasks 5.1/5.2 は「XcodeProject / AndroidGradleProject 参照の csproj 新設」を完了条件として明記していたが、実装はスクリプト/Exec + 手動参照方式だった。ホスト review-001 はビルド成功・binding 表面の検証に集中して形式の不一致を見逃し、相方 (second-opinion-003) が「記載された成果物は存在しない」「deviation.md もないため未合意の逸脱」と Major で検出。オーナーが裏取り調査を指示する起点になった。
