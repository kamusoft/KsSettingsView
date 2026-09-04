---
scope: code-review
kind: pain
severity: normal
count: 2
first-seen: 2026-08-05
last-seen: 2026-09-03
evidence:
  - add-maui-native-bridge (second-opinion-003 の rm -rf フォールバック指摘をオーナーが的外れとして却下)
  - add-release-workflow (second-opinion-code-001 の「新規 script の rm -rf が trash 規約違反」指摘。オーケストレーターが本パターンで降格し、オーナーが却下を確認)
---

## ルール文

リポジトリ組み込みのスクリプト (ビルドスクリプト・CI から実行されるもの) 内の `rm` 使用を「trash 規約違反」として指摘しない。trash 規約はエージェントが対話的にファイル操作するときのツール選択の規律であり、trash は環境依存 (CI に存在しない) のためスクリプトへの持ち込みはむしろ NG — スクリプト内に trash があれば rm への置き換えを指摘する側が正しい。

## 経緯

- 2026-08-05 add-maui-native-bridge: 相方レビュー (codex) が `ios/binding/build-xcframework.sh` の `rm -rf` フォールバックを規約違反として指摘 → オーナーが「規約はエージェントのツール操作限定。スクリプトの trash は CI が回らないので逆に NG」と却下。スクリプトからは trash 側を除去する修正になった
- 2026-09-03 add-release-workflow: 相方レビュー (codex) が `scripts/release/central-portal.sh` / `compare-maven-artifacts.sh` の mktemp ディレクトリ掃除の `rm -rf` を規約違反 (Minor) として指摘 → 本パターンを根拠に降格、オーナーが却下を確認
