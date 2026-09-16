---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-12
last-seen: 2026-09-12
evidence:
  - install-examples-and-release-notes (Release ノートを組み立てる `scripts/release/build-release-notes.py` の定型文言 — 種別の見出し 4 件・利用者向けの変更が無いときの 1 文・初回リリースの 1 文 — を日本語で実装した。これらは GitHub の Release ページに出る利用者向けの公開物であり、`## What's Changed` / `**Full Changelog**` という英語の定型と同じノートに並ぶ。オーナー指示で英語へ揃え、自己テストの期待値も追随させた)
---

## ルール文 (候補)

実装で**利用者・閲覧者の目に触れる公開物**に定型文言を新設するとき (Release ノート・配布物の説明・公開ページの見出し等)、開発者向けの文字列と同じ既定で日本語で書かない。公開物の言語は、`kasane/handbook/cross/` が既に引いている「閲覧者の言語圏を仮定しない」線 (`diagnostic-message-language.md` / `user-skill-api-listing.md`) に照らして決め、同じ画面に出る既存の文言 (プラットフォームが出す定型を含む) と揃える。

デルタスペックが文言の文字列まで規定していないことは、言語を選ばなくてよい理由にならない。spec は種別・項目を概念として説明するだけで、公開物としてどの言語で出すかは実装の判断に残る。

事後判定: 新設した公開物の文言について、レビューまたは完了報告で「同じ画面に並ぶ既存文言と言語が揃っている」ことに言及がある。

## 経緯

- 2026-09-12 install-examples-and-release-notes (蒸留時の補完): Release ノートの組み立てを新設した際、ノート本体に出る種別見出しと定型文を日本語で実装した。Release ページは公開物であり、`## What's Changed` と `**Full Changelog**` は GitHub が英語で出すため、`## Changes` の項目だけを英語にしても見出しの日本語が残る混在になる。オーナー指示で公開物の文言ごと英語へ揃えた。workflow のログに出る開発者向けエラーメッセージは対象外として日本語のまま残しており、境界は「誰が読むか」にある。関連: 開発者向け文字列の言語を規範層で決めた english-diagnostic-messages (archive 2026-09-07)。
