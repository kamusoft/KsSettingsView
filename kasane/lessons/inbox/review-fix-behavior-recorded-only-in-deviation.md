---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-16
last-seen: 2026-09-16
evidence:
  - maui-cell-appthemebinding-logical-child (相方 code-review の Major を受けた修正で「増減を通知しないコレクションでは所属の確定が表示へ変換する時点になる」という利用者から見える挙動が生まれたが、実装者は deviation.md に 1 行書いただけで、同じ change が開いていた concepts (`maui-rendering-lifecycle.md` の論理所有の段落と多重配置の表) と release-note の移行手順は無条件の記述のまま提出した。deviation.md は change と一緒にアーカイブされるため長命層に残らず、review-002 が Minor で差し戻して 1 周増えた)
---

## ルール文

レビュー指摘の修正で利用者から見える挙動 (成立する時点・入力ごとの違い・例外の時点) が新しく生まれたときは、deviation.md へ記録するだけで終えず、同じ change が既に開いている長命層の文書 (concepts) と利用者向け成果物 (release-note・Sample・doc コメント) のうち、その挙動を無条件に書いている箇所を検索して同じサイクルで書き分ける。deviation.md は archive に沈む足場であり、長命層の記述の正にはならない。事後判定: deviation.md の「決定事項と違う形」の行ごとに、対応する concepts / release-note の記述が同じ限定を持っている (grep で該当文を引いて確認できる)。

## 経緯

- 2026-09-16 maui-cell-appthemebinding-logical-child: 素の `List<T>` を `Root` / `Cells` に置いた場合の所属確定が変換時になる挙動を、修正サイクルで deviation.md にだけ記録して提出。concepts の「所属が始まった時点で論理子になる」「追加の時点 (Handler の有無に依らない)」と release-note の移行手順「先に除去してから追加すれば新しい所属先の論理子になる」が素の `List<T>` では成り立たないまま残り、review-002 が Minor として長命層への移送を要求。review-003 で解消 (concepts 2 箇所の但し書き・release-note の 2 段書き分け)。
