---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - datepickercell-color-adjust 探索セッション (2026-08-02。貼られたスクショを「チャット添付はファイルとして取り出せない」と誤答し、ユーザーの指摘でトランスクリプトから抽出できた)
---

## ルール文

チャットに貼られた画像を `ui/references/` 等へ保存するとき、「添付はファイルとして取り出せない」と判断してはいけない。添付はセッショントランスクリプト (`~/.claude/projects/<project-slug>/<session-id>.jsonl`) に base64 で埋まっているので、`type: "image"` の `source.data` を base64 デコードして保存する (session-id はスクラッチパッドのパス等から特定できる。拡張子は `source.media_type` に合わせる)。エミュレータでの再現撮影やユーザーへの保存依頼は、抽出が実際に失敗した場合の代替手段にとどめる。

## 経緯

- 2026-08-02 datepickercell-color-adjust 探索: ksn-explore Step 4 (デザイン素材の即時保存) で、貼られた不具合スクショを「チャット添付からは取り出せないので提案フェーズでエミュレータから撮り直す」と回答した。ユーザーから「トランスクリプトに base64 で埋まってるはずなので取り出せる」と指摘を受け、`90d065b4-….jsonl` から `image/webp` を抽出して `ui/references/current-calendar-dialog-broken.webp` として保存できた (56,824 bytes)。
