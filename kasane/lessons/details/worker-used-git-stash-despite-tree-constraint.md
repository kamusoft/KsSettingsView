# worker-used-git-stash-despite-tree-constraint (impl L-009 の経緯)

inbox パターンとして pain 3 件で閾値到達し、2026-09-06 にオーナー承認で `impl.md` L-009 へ昇格した (強制力ルーティングは lessons: hook 化はオーナー判断で見送り)。

## ルール文 (昇格時)

実装ワーカーは検証のために作業ツリーの状態を git 操作 (stash / checkout / reset 等) で一時的に切り替えない。差分を除いた状態での検証が必要になったら、自分で切り替えずにオーケストレーター/ユーザーへ確認を上げる (worktree の分離やビルド成果物の比較など、ツリーを動かさない代替を編成側が判断する)。事後判定: ワーカーの報告・シェル履歴に stash / 一時 checkout が現れない。

## 経緯

- 2026-08-27 relax-android-host-prerequisites: ワーカーが A/B 検証のため git stash push/pop を使用し、自己申告した。ツリーは復元され実害はなかったが、オーケストレーターと共有する作業ツリーを黙って動かす操作は、並行作業の破壊・復元漏れのリスクがある (蒸留時 2026-08-28 に捕捉)。
- 2026-09-04 add-release-workflow: レビューワーカーが `git checkout develop` で作業ツリーの branch を切り替えた (review-004 の diff 取得のためと推定)。オーケストレーターは切り替えに気づかず commit → push し、develop に保護をバイパスした直接 push が発生した (内容は change 配下の記録のみで実害は無いが、運用上の事故)。ルール文の対象は実装ワーカーに限らずレビュー・検証ワーカーも含める (読み取り専用の役割でも checkout は禁止。diff は `git diff <base>...<head>` や `git show` で取れる)。
- 2026-09-06 cell-style-dark-appearance-parity: probe 復元のための `git checkout -- <file>` が未コミット編集を巻き込んで消した。「stash / checkout / reset を使わない」を制約の列挙に含めるだけでは網羅できず、「作業ツリーを git で巻き戻さない (復元は手作業の逆編集か、変更前バイト列の退避で行う)」と原理で書く必要がある。pain 3 件目 = 昇格閾値到達 (蒸留の絞り口で判定)。
