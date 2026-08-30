---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - fix-android-chevron-vertical-centering (drawable の補正に <group translateY> を提案し、オーナーが「原データの pathData 自体を直す」方向へ修正指示)
---

## ルール文

アセットやデータの欠陥 (drawable のパス位置・座標値など) を恒久修正するときは、変換レイヤ (`<group translateY>` 等のラッパ) を足すのではなく、原データ自体を修正できないかを先に検討し、できるならそちらを提案する。ラッパは検証プローブとしては良いが、恒久修正に残すと構造が 1 段増え「なぜ包んでいるのか」が追加の説明を要する。

## 経緯

- 2026-08-02 fix-android-chevron-vertical-centering: chevron drawable のパス非対称の修正で `<group android:translateY="1">` を巻く形を実装案として提示 → オーナーが「translateY するより ic_navigate_next 自体を修正する方向にできる?」と指摘。pathData の絶対 y 座標を +1 する書き換え (単一パス維持) に変更した。検証段階のプローブとしての translateY は有効だった。
