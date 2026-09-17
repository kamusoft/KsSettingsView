---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-17
last-seen: 2026-09-17
evidence:
  - scroll-indicator-visible-not-applied (Android の設定リストにスクロールバーを出すため、同梱テーマ全体へ `recyclerViewStyle` を差し込んだ。同じテーマから生成される選択面の候補リストと回転ホイールにもスクロールバーが付き、review-001 が Major で検出。適用点を設定リストの生成箇所だけに重ねる専用オーバーレイへ限定して解消)
---

## ルール文 (候補)

共有されるテーマ・既定 style・基底クラスへ属性を足して 1 つの対象の見た目を変えるときは、その共有物から生成される他の対象を検索で列挙し、変化が及ぶ範囲を対象だけに限定する (専用のオーバーレイ・生成箇所での指定)。限定した後は、及んではいけない対象に変化が無いことをテストで固定する。

## 経緯

- 2026-09-17 scroll-indicator-visible-not-applied: コード生成の `RecyclerView` は `android:scrollbars` を持つ style 経由でしかスクロールバーの描画状態を初期化できないため、探索で「同梱テーマに `recyclerViewStyle` を定義する」手を採った。同梱テーマはライブラリ所有 UI の全体 (選択面・ホイールを含む) が共有しており、実装時にその消費者を洗わなかった。修正後は「回転ホイールには出ない」をテストと実機証跡で分けて固定した。
