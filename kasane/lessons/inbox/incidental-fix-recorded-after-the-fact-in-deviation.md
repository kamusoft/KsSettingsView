---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-consumer-verification (verify-001 が INVALID — Scenario の欠落・乖離は 0 件だったが、deviation.md に無い付随修正が 2 件あった。(1) `kasane/config.yaml` の `lint.identity.allow` への `repo.local` 追加 (tasks 4.3 は scope への `verification` 追加しか指示していない)、(2) `android/kssettingsview/build.gradle.kts` の SNAPSHOT ガードのエラー案内の改訂 (deviation 2 件目の署名条件化の記述に含まれていなかった)。いずれも実装の欠陥ではなく記録の漏れで、deviation.md の追記だけで verify-002 が VALID になった)
---

## ルール文 (候補)

tasks に無いファイルへ手を入れた (付随修正) 瞬間に、その編集と同じ作業単位で deviation.md に `[付随修正] <箇所>: <何を直したか>。理由: <一言> (日付)` を 1 行書く。レビュー指摘への対応で既存の付随修正を広げたとき (同じファイルの別箇所・文言) も、既存の行を広げるか新しい行を足す。実装フェーズの終わりにまとめて書こうとすると、lint を通すための設定変更やメッセージ文言の追随のような「小さすぎて判断ではない」修正から漏れる。

事後判定: `git diff --stat` に現れるファイルのうち tasks が名指ししていないものは、すべて deviation.md の `[付随修正]` 行に箇所として現れている (verify がこの突き合わせを行う)。

## 関連

[[known-limitation-accepted-without-deviation-record]] (既知の限界を記録なしで受容する型) と対をなす — あちらは「限界の受容」、こちらは「付随修正の記録」で、どちらも deviation.md が蒸留の入力になることを前提にした記録規律。

## 経緯

- 2026-09-02 add-consumer-verification: 付随修正 2 件 (version 注入の受け口・署名の条件化) はオーナー裁定を経て記録されたが、裁定を要さない小修正 (identity-lint の allow 追加、SNAPSHOT ガードの文言追随) は記録されず、verify-001 の未記録差分検査で発覚した。verify の突き合わせ (tasks が名指ししない変更ファイルの列挙) が機能した例でもあり、記録の起点を「編集した瞬間」に前倒しすれば verify の往復が 1 回減る。
