---
scope: ui-impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-02
last-seen: 2026-08-02
evidence:
  - android-picker-selection-sheet (シート背景を `setBackgroundColor(TRANSPARENT)` で潰し、Material の `MaterialShapeDrawable` が持つ角丸・elevation・展開時補間を喪失。review-001 指摘)
  - ios-picker-selection-parity (`UINavigationBarAppearance()` を新規生成して standard/compact/scrollEdge の3つすべてへ代入し、ホストアプリのカスタマイズと透過背景を破壊。review-001 Major)
---

## ルール文

ライブラリ UI がプラットフォーム既定の描画構成 (Android の `MaterialShapeDrawable`、iOS の navigation bar appearance 等) に色やスタイルを載せるときは、既定を新規オブジェクトで丸ごと差し替えず、現在有効な構成を取得・複製して必要な属性だけを変更する。既定が担う付随機能 (角丸・elevation・遷移補間・ホストアプリのカスタマイズ) を無自覚に失わない。

## 経緯

- 2026-08-02 android-picker-selection-sheet: 背景色の適用に `setBackgroundColor(TRANSPARENT)` を使った結果、`BottomSheetDialog` 既定の `MaterialShapeDrawable` が持つ角丸・elevation・展開補間が失われた (review-001)。修正は既存 drawable への tint 適用へ変更。
- 2026-08-02 ios-picker-selection-parity: タイトル文字色だけを変えたいのに `UINavigationBarAppearance()` を新規生成して3プロパティすべてへ代入し、ホストアプリの appearance カスタマイズとページシート既定の透過背景を破壊 (review-001 Major)。修正は現在有効な appearance の複製 + タイトル色のみ差し替えへ変更。
