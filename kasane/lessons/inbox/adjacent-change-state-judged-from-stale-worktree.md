---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-01
last-seen: 2026-09-01
evidence:
  - fix-ios-test-pump-condition-wait (worktree での長時間作業中、隣接 change `fix-ios-tapnotifyingrenderer-actor-isolation` を「exploration.md だけのスタブ = 未着手」と判断し、レビュアーと相方への申し送りにも「別 change のスタブとして切り出し済み、指摘対象外」と書いた。実際には同 change は既に実装・レビュー・マージまで完了して develop に入っており、worktree の基点が 7 コミット古かったために未着手に見えていた。マージ時に判明。昇格済みの process L-006「不在の断定は対象を特定できる検索を通してから行う」を、指揮側が自分で破っていた)
---

## ルール文 (候補)

worktree で作業しているとき、**隣接 change の進行状態を `kasane/changes/<id>/` のファイル構成から判断しない**。worktree の基点が古ければ、他所で完了・マージ済みの change も「exploration.md だけのスタブ」に見える。

隣接 change の状態に言及する前 (特にレビュアー・相方への申し送りに「別 change として切り出し済み」「未着手」と書く前) に、次のいずれかで実際の状態を確認する:

- `git log <作業ブランチ>..develop --oneline` で、作業開始後に develop へ入った変更を見る
- `git log develop --oneline -- kasane/changes/<隣接 id>/` で当該 change の進行を見る

作業が長時間に及ぶほど基点は古くなる。**「今どうなっているか」を worktree のファイルシステムだけで答えない。**

事後判定: 隣接 change の状態に言及した箇所に、確認に使ったコマンドか、確認した時点の develop の位置が書かれている。

## 関連

昇格済み process L-006 (不在の断定は対象を特定できる検索を通してから) の worktree 版。L-006 は「リポジトリ全体を対象語で検索する」ことを求めるが、**worktree では検索対象そのものが古い**ため、検索を通しても誤る。ブランチ間の差分を見る手順が要る点が独自。

## 経緯

- 2026-09-01 fix-ios-test-pump-condition-wait: 2 時間弱の作業中、`kasane/changes/` を一覧して 7 件の隣接 change をすべて「exploration.md だけのスタブ」と判定した。うち 1 件は実際には完了済みで、develop には proposal / specs / tasks / review 3 本 / verify / second-opinion 2 本が入っていた。この誤認はレビュアー 4 回分と相方 1 回分の申し送りに「Non-Goal の内側」として書かれ、レビュー判定の前提として使われた (幸い判定を変える性質のものではなかった)。マージ後、develop の 7 コミットを確認して初めて判明した。
