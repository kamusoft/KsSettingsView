---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-09
last-seen: 2026-08-25
evidence:
  - adjust-section-spacing (review-001〜003 の3周連続で同型指摘: Section 余白と Switch オフ色の値変更後、旧値・旧仕様を現在形で語るテスト説明・KDoc・区切りコメントが残存 — 「AiForms 準拠で 0」「オフ = colorSurfaceContainerHighest/colorOutline」「オン thumb = colorOnPrimary」。値の変更に追随して周辺の説明文を洗う手順が実装フローに無い)
  - harden-update-accessory-unknown-id (review-001 Major: Store の契約変更 (core/ADR-0020) 後も、MAUI 層3箇所の C# コメントが「updateAccessory は未知 ID no-op 契約の対象外」という旧契約を断定したまま残り、実装者は掃き残した。ホスト review が検出し修正)
---

## ルール文

観察可能な契約を変更する実装では、旧契約を記述する既存のコメント・doc を、変更対象の capability 外の層を含むリポジトリ全体で grep し (契約の特徴語で検索する)、現行契約へ追随させてから完了とする。コンパイルもテストも旧契約の記述を検出しない — 誤った契約説明は将来の設計判断を誤らせる。

## 経緯

- 2026-08-09 harden-update-accessory-unknown-id: 未知 sectionID の `updateAccessory` を Store で no-op にする契約変更 (core/ADR-0020) で、実装対象外の MAUI 層 (`IKsSettingsGateway.cs` / `KsSettingsController.cs` / `RemovedElementNotificationTests.cs`) に「この操作だけは no-op 契約の対象外」という正反対の記述が残った。review-001 が Major として検出、`契約の対象外` / `素通し` の再走査で修正完了を確認 (review-002)。
- 2026-08-25 adjust-section-spacing: ライブ調整で確定した余白・Switch 色の値変更後、周辺のテスト説明・KDoc・区切りコメントの旧仕様記述が review-001/002/003 の3周にわたり順次検出された (1回で網羅的に掃けていない)。特徴語 grep (colorOnPrimary / AiForms 準拠 等) で最終的に全消化。
