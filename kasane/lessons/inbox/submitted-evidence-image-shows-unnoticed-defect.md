---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-24
last-seen: 2026-08-28
evidence:
  - android-hinttext-position (review-001 Critical「行右端の余白 16dp が消える」の実害が、実装者が提出した証跡 android-after.png の ButtonCell 行に既に写っていた。テストも実装者の目視も検出せず、レビュアーの画像照合で発覚)
  - relax-android-host-prerequisites (MAUI サンプルのテーマ復帰で NavigationBar が暗色地×黒文字の低コントラストに劣化。verify-maui-01 以降の証跡に写っていたが、検証・レビューとも Cell 部分だけを見ておりオーナー指摘で発覚。ライブラリ UI 外のアプリクロームも証跡確認の対象に含めるべき観測)
---

## ルール文

証跡スクリーンショットは保存・提出する前に自分の目で開き、変更対象以外の要素 (余白・整列・重なり) に異常がないかを修正前画像・正の画像と見比べて確認する。証跡は「撮ったら添付する」ファイルではなく、最初のレビュー機会である。

## 経緯

- 2026-08-24 android-hinttext-position: root padding 廃止の副作用 (accessoryHolder GONE 行の右余白消失) が提出済み証跡 `android-after.png` に写っていたが、実装者は気づかず提出。review-001 が画像と実測プローブで Critical として検出した。
- 2026-08-28 relax-android-host-prerequisites: MAUI サンプルのテーマをテンプレート既定へ戻した副作用で NavigationBar がフォールバック配色 (暗スレート地に黒タイトル) となり判読困難に。証跡 (verify-maui-01 以降) に一貫して写っていたが、撮影ワーカー・レビューサイクルとも検証対象 (ライブラリの Cell / 選択面) だけを照合しており、誰も指摘しないままオーナーが発見した。証跡の目視確認は変更対象の外側 (アプリクローム・システムバー境界) も含める。
