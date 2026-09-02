---
scope: code-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-maui-nuget-distribution (review-001 Minor「同梱アイコンの第三者由来に対する帰属表示がない」— 原典 AiForms.Maui.SettingsView の LICENSE.txt は `Copyright (c) 2022 kamu` で本リポジトリの著作権者と同一人物。レビュアーは `../AiForms.Maui.SettingsView/LICENSE.txt` を開けば確認できたが「第三者由来」を前提に指摘し、オーナー確認の往復が 1 回発生した)
---

## ルール文

移植元・原典由来の素材 (アイコン・画像・コード片) について帰属表示やライセンス表記の欠落を指摘する前に、原典リポジトリ (`../<リポジトリ名>/LICENSE*`) の著作権者名義を読み、本リポジトリの著作権者と別人であることを確認する。同一人物・同一組織なら第三者素材ではないため指摘しない。確認できない (原典が手元にない) 場合は「著作権者が別人なら」の条件付きで書き、確認先のパスを添える。

## 経緯

- 2026-09-02 add-maui-nuget-distribution: 「不在の断定は対象を確認してから」(lessons/process.md L-006) の同型がライセンス帰属の領域で起きた。指摘自体は理屈として正しいが、前提 (第三者由来) を原典で裏取りせずに Minor として挙げ、オーナーから「よくわからない」と差し戻された。
