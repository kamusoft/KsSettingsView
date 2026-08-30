---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - consolidate-readmes-and-contribution (相方 spec-review の「移送先が未定義」指摘を受けて design.md Decision 1 に移送対応表を追加したが、行を「出典ファイル×節名」で立てたため、同じ内容クラスの節が別の README にも存在するケースを網羅できなかった。廃止対象 5 枚の全節を突き合わせた実装ワーカーが未分類 9 件を検出して停止 — 「必要環境」は android/README.md の行だけ、「ディレクトリ構成」は samples/maui/README.md の行だけ、「Android SDK ロケーションの設定」は samples/android/README.md 側が無い等。加えて spec Scenario が要求する MAUI のステップイン手順は移送元 README に節自体が存在せず、対応表からは生成可否が読めなかった)
---

## ルール文 (候補)

複数ファイルの内容を移送・統廃合する変更で design に対応表を置くときは、行を**内容クラス (何の情報か)** で立て、出典は「どのファイルに現れるか」の列として添える。行を「出典ファイル×節名」で立てると、同じ内容クラスが別ファイルにも現れた分が表から漏れ、実装時に未分類として停止する。あわせて表の冒頭に「出典を問わず同一内容クラスは同じ行の指示に従う」と明記し、移送元に存在しないが spec が要求する項目 (対向 platform には有って当該 platform には無い手順等) は「新規記述の可否」を行として立てる。

## 経緯

- 2026-08-29 consolidate-readmes-and-contribution: README 5 枚の廃止に伴う知識移送で、design.md Decision 1 が対応表を 12 行で定義。実装ワーカーが全節突き合わせ (tasks 1.1) を行い、未分類 9 件 + spec 要求に対する移送元不在 1 件で停止。オーケストレーターが「対応表は内容クラスで読む」包括解釈と MAUI ステップインの新規記述可否を deviation で確定して再委譲した。表そのものを置いた判断は正しく (spec-review の指摘は妥当)、粒度の軸だけが誤っていた。
