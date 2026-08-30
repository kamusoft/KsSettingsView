---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-05
last-seen: 2026-08-05
evidence:
  - add-maui-native-bridge (second-opinion-003 の rm -rf フォールバック指摘をオーナーが的外れとして却下)
---

## ルール文

リポジトリ組み込みのスクリプト (ビルドスクリプト・CI から実行されるもの) 内の `rm` 使用を「trash 規約違反」として指摘しない。trash 規約はエージェントが対話的にファイル操作するときのツール選択の規律であり、trash は環境依存 (CI に存在しない) のためスクリプトへの持ち込みはむしろ NG — スクリプト内に trash があれば rm への置き換えを指摘する側が正しい。

## 経緯

- 2026-08-05 add-maui-native-bridge: 相方レビュー (codex) が `ios/binding/build-xcframework.sh` の `rm -rf` フォールバックを規約違反として指摘 → オーナーが「規約はエージェントのツール操作限定。スクリプトの trash は CI が回らないので逆に NG」と却下。スクリプトからは trash 側を除去する修正になった
