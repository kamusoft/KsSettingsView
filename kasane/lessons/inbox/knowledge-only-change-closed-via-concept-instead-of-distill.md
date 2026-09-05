---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-05
last-seen: 2026-09-05
evidence:
  - maui-android-customcell-embed-perf (探索の結果「コード変更なし・知見だけ残して閉じる」となった change の締め方として、ホストは ksn-concept へのハンドオフを提案。オーナーが「蒸留でコンセプトに流せるんでは?」と正し、ksn-distill で concepts 追随と archive 移設を一度に行った)
---

## ルール文 (候補)

探索 (ksn-explore) が「コード変更は行わず知見だけを長命層に残す」で終わるとき、証跡 (evidence/) や探索メモを持つ change の締め方は ksn-distill を第一候補にする (実装完了・レビュー完了の前提は「探索完了 = オーナーの決定」と読み替える旨を一言添えて起動する)。ksn-concept を選ぶのは change ディレクトリ自体が存在しない (会話だけの知識) ときに限る。理由: distill は concepts 追随と archive 移設を同じ Step で行い、証跡の参照パスを archive 後の形で確定できるが、ksn-concept は archive しないため証跡の置き場所が宙に浮く。事後判定: 探索の終わり方の提示に、change ディレクトリの有無と証跡の移設先が書かれている。

## 関連

ksn-explore「終わり方」の ksn-concept へのハンドオフ条件 (「変更ではなく決まり事・知識の整理だった」) は change ディレクトリの有無を区別していない。昇格先はスキル本文 (ksn-explore の終わり方の条件文) が第一候補。

## 経緯

- 2026-09-05 maui-android-customcell-embed-perf: 計測 (Pixel 4a / 6a 実機、atrace) を経て F (閉じる) と決めた直後、ホストは exploration.md に「ksn-concept への申し送り」節を書き、ksn-concept の起動を案内した。オーナーが蒸留を指摘。前例 (2026-08-28 customcell-android-maui-perf、コード変更なし) も蒸留で archive されており、証跡の参照パスは archive 後の形で concepts から指されていた。
