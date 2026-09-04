---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-27
last-seen: 2026-09-04
evidence:
  - relax-android-host-prerequisites (グループ6 のワーカーが検証目的で git stash push/pop を一時使用。制約違反の自己申告あり、ツリーは復元済み・最終差分は意図どおりを確認。deviation.md に記録)
  - add-release-workflow (2026-09-04。レビューワーカー (ksn-reviewer) が diff 取得のために共有作業ツリーを `git checkout develop` に切り替え、直後にオーケストレーターが commit → push して develop へ直接 push (branch protection をバイパス) となった)
---

## ルール文 (候補)

実装ワーカーは検証のために作業ツリーの状態を git 操作 (stash / checkout / reset 等) で一時的に切り替えない。差分を除いた状態での検証が必要になったら、自分で切り替えずにオーケストレーター/ユーザーへ確認を上げる (worktree の分離やビルド成果物の比較など、ツリーを動かさない代替を編成側が判断する)。事後判定: ワーカーの報告・シェル履歴に stash / 一時 checkout が現れない。

## 経緯

- 2026-08-27 relax-android-host-prerequisites: ワーカーが A/B 検証のため git stash push/pop を使用し、自己申告した。ツリーは復元され実害はなかったが、オーケストレーターと共有する作業ツリーを黙って動かす操作は、並行作業の破壊・復元漏れのリスクがある (蒸留時 2026-08-28 に捕捉)。
- 2026-09-04 add-release-workflow: レビューワーカーが `git checkout develop` で作業ツリーの branch を切り替えた (review-004 の diff 取得のためと推定)。オーケストレーターは切り替えに気づかず commit → push し、develop に保護をバイパスした直接 push が発生した (内容は change 配下の記録のみで実害は無いが、運用上の事故)。ルール文の対象は実装ワーカーに限らずレビュー・検証ワーカーも含める (読み取り専用の役割でも checkout は禁止。diff は `git diff <base>...<head>` や `git show` で取れる)。
