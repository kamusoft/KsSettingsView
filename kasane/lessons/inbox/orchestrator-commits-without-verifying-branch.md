---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-04
last-seen: 2026-09-04
evidence:
  - add-release-workflow (2026-09-04。ワーカー起動の直後に、branch を確認せず `git commit` → `git push` を実行し、切り替わっていた develop へ直接 push した)
---

## ルール文 (候補)

オーケストレーターが git commit / push を行う直前には、必ず `git branch --show-current` (と push 先の upstream) を確認し、意図した作業 branch と一致しないなら push しない。特にワーカー (実装・レビュー・検証) を起動した後は、ワーカーが作業ツリーの状態を変えた可能性があるものとして扱い、確認を省かない。commit と push を同じコマンド行に連結する場合は、branch 検査を同じ行の先頭に置いて不一致なら中断させる。

## 経緯

- 2026-09-04 add-release-workflow: レビューワーカーが `git checkout develop` で共有作業ツリーを切り替えた直後、オーケストレーターが「chore/release-prep にいる」前提で commit → push し、保護された develop へ直接 push した (bypass 権限のため拒否されなかった)。push 出力の「Bypassed rule violations for refs/heads/develop」で事後に気づいた。ワーカー側の規律 (worker-used-git-stash-despite-tree-constraint) と対になる、指揮側の防波堤。
