---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-27
last-seen: 2026-08-28
evidence:
  - relax-android-host-prerequisites (グループ6 のワーカーが検証目的で git stash push/pop を一時使用。制約違反の自己申告あり、ツリーは復元済み・最終差分は意図どおりを確認。deviation.md に記録)
---

## ルール文 (候補)

実装ワーカーは検証のために作業ツリーの状態を git 操作 (stash / checkout / reset 等) で一時的に切り替えない。差分を除いた状態での検証が必要になったら、自分で切り替えずにオーケストレーター/ユーザーへ確認を上げる (worktree の分離やビルド成果物の比較など、ツリーを動かさない代替を編成側が判断する)。事後判定: ワーカーの報告・シェル履歴に stash / 一時 checkout が現れない。

## 経緯

- 2026-08-27 relax-android-host-prerequisites: ワーカーが A/B 検証のため git stash push/pop を使用し、自己申告した。ツリーは復元され実害はなかったが、オーケストレーターと共有する作業ツリーを黙って動かす操作は、並行作業の破壊・復元漏れのリスクがある (蒸留時 2026-08-28 に捕捉)。
