---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-09
last-seen: 2026-08-09
evidence:
  - add-maui-samples-foundation (MAUI サンプルの LabelCell 検証ページで HintText と ValueText の描画が縦に近接して重なる見え方を「本体側レイアウトの課題候補」としてオーナーへ提示・調査タスク化したが、オーナーが「行高が低い場合に Hint と Value が重なるのは仕様通り」と却下)
---

## ルール文

LabelCell で行高が低い場合に HintText と ValueText の描画が重なるのは仕様どおりの挙動であり、欠陥として指摘・課題化しない (iOS/Android/MAUI 共通)。重なりを避けたい画面はサンプル/利用側で行高または文言量を調整する。

## 経緯

- 2026-08-09 add-maui-samples-foundation: サンプル実装エージェントが「文字サイズ」行 (ValueText「標準」+ HintText「端末の設定に従う」) で両者の重なりを観測し、両 OS 共通の見え方から native 本体のレイアウト課題候補としてエスカレーション。オーケストレーターが調査タスクとして切り出したが、オーナーが仕様どおりと明言して却下。プロダクト仕様の知識不足による誤検出で、レビュー・実装の双方が再発しうる。
