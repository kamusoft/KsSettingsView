---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-22
last-seen: 2026-08-22
evidence:
  - fix-cell-icon-size-parity (提案の相方レビューで iOS の狭幅時の幅配分が icon 枠の契約と同じ stackH 優先度の話だと判明したのに、ホストは「現状も required 同士の衝突」を理由に Non-Goal + 別 change の簡易起票を提案。オーナーが「同じ領域なんだからついでに直す方が良い。すぐ別 change を切りたがるのは良くない」と却下し、本 change のスコープへ戻した)
---

## ルール文 (候補)

探索・提案・レビュー対応で、今の change が触る機構 (同じ View 階層・同じ優先度設定・同じ解決関数・同じ bind 経路など) の中に隣接する課題を見つけたら、次のどちらかに当たる限り、別 change の簡易起票や Non-Goal への明記で切り離さず、本 change の What Changes とデルタスペックに含める: (a) 同じファイル・同じ機構の修正で直る、(b) 今の change の契約を閉じるのに必要 (例: 「icon 枠は譲らない」は「足りないとき誰が譲るか」が決まって初めて閉じる)。切り離してよいのは独立した機構・別の設計判断・別 platform の別問題に限り、そのときは Non-Goals に切り離す理由を 1 行で書く。事後判定: proposal の Non-Goals に「同じ機構の課題を後続 change へ」という項目が無く、隣接課題が specs / tasks に反映されている。

## 関連

昇格済みの process L-005 (数行で閉じる修正を別 change へ逃がさない) と同根。L-005 は実装・レビュー対応フェーズで「数行で閉じるか」を基準にするのに対し、本パターンは探索・提案フェーズで「同じ機構か / 契約を閉じるのに必要か」を基準にする。昇格時は L-005 への統合 (適用フェーズと判定基準の拡張) を第一候補にする。

## 経緯

- 2026-08-22 fix-cell-icon-size-parity: 相方 (codex) の spec-review Major-1 (iOS 優先度案が狭幅時に title の CCR に負ける) を受け、ホストは「狭幅時に誰が譲るかは現状も未定義。幅配分の契約に踏み込むので Non-Goal + 別 change」と提案した。オーナーは「同じ領域なら一緒に直す」と却下。調べ直すと幅配分の契約自体は concepts (cell-row-layout) と android/ADR-0002 で決定済みで、iOS がその契約と逆 (valueText が先に省略) という drift だった — 新しい設計判断は不要で、切り離す理由は最初から無かった。
