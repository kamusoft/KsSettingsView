---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-17
last-seen: 2026-09-17
evidence:
  - scroll-indicator-visible-not-applied (相方 codex が spec レビューで「M 級の利用者可視 UI 変更なのに `ui/` (brief・承認モック) を省略している」を Major 指摘。規約の文面上は正しいが、変更の見た目は OS 標準のスクロールバーと「Header / Footer の既定では背景を描かない」だけで、配色・寸法・配置の選択が無く、実機証跡が既に揃っていた。オーナーが「省略する」と確定し降格)
---

## ルール文 (候補)

提案レビューで `ui/` ゲート (brief・承認モック) の欠落を指摘する前に、その変更に**モックで選ぶ見た目** (配色・寸法・配置・状態の見せ方) が実際にあるかを確認する。OS 標準の描画をそのまま使う・既存の要素を描かなくするだけ、のように選択の余地が無い変更では、`ui/` の欠落を Major にせず、「省略の理由と合意が proposal に記録されているか」だけを見る。

## 経緯

- 2026-09-17 scroll-indicator-visible-not-applied: S 級で着手後に M 級へ引き上げた change。ホスト側は proposal の Impact に省略理由を書いていたが、相方は規約の「UI に触れる変更は級を問わず `ui/` 必須」を根拠に Major とした。オーナーは「省略する」を選び、proposal に合意済みの逸脱として記録した。
