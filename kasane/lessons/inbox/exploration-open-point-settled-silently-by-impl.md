---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-11
last-seen: 2026-08-11
evidence:
  - fix-maui-entrycell-focus-loss (exploration の未決論点「非 EXACT spec が飛んでくる正確な条件」を実装が「無限制約は 0 を返す」で暗黙に確定し、VerticalStackLayout 直下等で SettingsView が無警告で高さ 0 になる公開挙動の後退を作った。review-001 Major-1 が差し戻し、フォールバック方式へ転換)
---

## ルール文

S 級などデルタスペックを作らない変更で、exploration.md に「未決の論点」「実装時に確認」と残された点を実装が確定するときは、その確定を暗黙にせず、選んだ挙動と理由を完了報告 (またはオーナーへの確認) に明示する。合意されたのは方針までで、未決論点の確定は新しい判断 — 特にその確定が公開挙動の後退・platform 非対称を生む場合は実装内で黙って決めない。

## 経緯

- 2026-08-11 fix-maui-entrycell-focus-loss: 探索で合意されたのは案A (MAUI handler 層で measure 契約を閉じ fill 即答) までで、「制約が無限のときどうするか」は未決論点として明記されていた。実装は無限制約でも 0 を返す形で暗黙に確定し、高さ制約のない親 (VerticalStackLayout / 縦 ScrollView / Grid Auto 行) で SettingsView が無警告で消える後退と iOS との非対称が入った。review-001 が Major で差し戻し、「非有限は base.GetDesiredSize へフォールバック」で解決 (review-002 で APPROVED)。
