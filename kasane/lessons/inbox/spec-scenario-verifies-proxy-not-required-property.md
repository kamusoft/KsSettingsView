---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-maui-nuget-distribution (second-opinion-spec-001 Major 3・4 — Requirement「platform TFM のビルドでのみガードする」に対し Scenario は Android / iOS の成功・失敗だけで、非 platform TFM・outer build・間接参照で何も起きないことを検査していなかった。Requirement「GitHub と nuget.org の双方で表示できる」に対し Scenario は URL の prefix 一致しか検査していなかった。ホスト側の自己レビュー 2 周は検出せず、相方の指摘で Scenario 2 件と THEN の到達確認を追加)
---

## ルール文

Requirement の SHALL が適用範囲の限定 (「〜でのみ」「〜では何もしない」) や外部への到達 (「表示できる」「取得できる」) を述べているとき、提案を出す前にその Requirement 配下の Scenario を読み、限定の外側で何も起きないことを検査する Scenario と、実物への到達 (取得成功・応答の種類) を検査する THEN があるかを突き合わせる。無ければ Scenario を足す — 文字列の形式 (prefix・拡張子) や範囲内の成功だけを検査する Scenario は、その SHALL の代理観測であって検証ではない。

## 経緯

- 2026-09-02 add-maui-nuget-distribution: `buildTransitive/` の資産は推移的な全消費者へ import されるため、Condition の誤りは `net10.0` 消費者や outer build を壊すが、当初の Scenario (platform TFM の成功・失敗) では検出できなかった。README 画像も「表示できる」の検査が prefix 一致に縮退していた。いずれも相方 (codex) の spec-review が検出し、design Decision 5 の Condition と spec の Scenario「非 platform TFM と outer build ではガードが動かない」・THEN の到達確認 (取得成功と Content-Type) へ反映された。
