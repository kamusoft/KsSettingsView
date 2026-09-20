---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-20
last-seen: 2026-09-20
evidence:
  - date-time-picker-locale (review-001 Major: iOS の Locale 変更通知で full render を再利用したため、表示中の Time / Date Wheels の未確定値が提示開始時の Cell 値へ巻き戻った。部品へ Locale を直接注入するテストと別 Locale の静止画は green のままで、production notification 経路のテスト追加により検出可能にした)
---

## ルール文

Locale・外観・構成変更などの実行時通知から入力 UI を更新するときは、production の通知経路を通して未確定の入力を作った状態で refresh し、その値が保持されて確定 callback に届くことを検証する。表示属性だけの更新に full render を再利用する場合は、モデルの再適用が編集中の値を巻き戻さないことを完了条件に含める。

## 経緯

2026-09-20 の date-time-picker-locale では、Locale 通知 handler が `render` を呼び直し、表示言語だけでなく picker の選択値まで元の Cell 値へ戻していた。review-001 が production notification 経路と未確定入力を組み合わせて発見し、Locale だけを更新する経路への分離と Time / Date Wheels / Date Calendar の回帰テストで解消した。
