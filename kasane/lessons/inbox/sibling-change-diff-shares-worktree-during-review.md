---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-07
last-seen: 2026-09-07
evidence:
  - english-diagnostic-messages (作業ツリーに別 change fix-release-central-validation-wait の未コミット差分 [`scripts/release/central-portal.sh` と当該 change の exploration.md] が同居した状態で独立レビューを 3 周回した。review-002 が Suggestion で「このまま `git commit -a` 相当で確定すると 2 つの変更が 1 コミットに混ざり、どちらかの revert ができなくなる」と指摘し、review-003 も「状況継続 [対処は commit 時]」として再掲。最終的に commit は 2 本へ正しく分離され実害は出ていないが、レビュアーは 2 周にわたって対象外差分の切り分けと注意喚起に紙幅を使った)
---

## ルール文 (候補)

独立レビューを起動する前に `git status --porcelain` で作業ツリーの変更ファイルを列挙し、レビュー対象 change のスコープに属さないファイルがあれば、起動前に別 change として commit して切り離す。切り離せない事情があるときは、対象外のファイル一覧をコンテキストパッケージに明記してから起動する。レビュアーは作業ツリーの差分からレビュー対象を判定するため、同居した別 change の差分は毎周「評価対象に含めていない」の断り書きと commit 分離の注意喚起として指摘に載り、レビュー 1 周ぶんの注意を消費する。

事後判定: レビュー証跡に、対象 change のスコープ外のファイルへの言及が現れない。

## 経緯

- 2026-09-07 english-diagnostic-messages: 2 つの S 級変更を同じ作業ツリーで並行して実装し、片方のレビューを先に 3 周回した。レビュアーは対象外差分を正しく除外して判定できていたが、除外したこと自体を毎回書く必要があり、指摘としても 2 周連続で載った。commit の分離は指揮側が最後に正しく行ったため、残ったのはレビュー注意の消費だけだった。
