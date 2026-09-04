---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - add-consumer-verification (review-001 Major-1 — `verification/android/` を 3 つめの Gradle build root として追加したが `local.properties` を持たず生成もせず、`ANDROID_HOME` が必須になっていた。handbook `cross/local-development-setup.md` は SDK の解決手段として環境変数と `local.properties` の 2 経路を対等に案内しており、後者の環境では Scenario「引数なしで dry-run が動く」(GIVEN 本体がビルドできる状態) が成立しなかった。evidence でも片方の実行例だけ `ANDROID_HOME=` を前置しており前提が揃っていなかった。修正は `android/local.properties` の `sdk.dir` へフォールバックする `android-sdk.sh` の新設)
---

## ルール文 (候補)

新しい build root・ビルド入口スクリプト・CI 外で人が実行するスクリプトを追加するときは、handbook の環境構築ガイド (`local-development-setup.md` 等) が案内する環境の作り方を列挙し、その**すべての経路**で「引数なし」「ガイドどおりの環境」の Scenario が成立するかを 1 経路ずつ実行して確かめる。自分の環境で通ることを「本体がビルドできる状態なら通る」と読み替えない。ガイドの経路で通らないなら、スクリプト側で吸収する (ガイドを増やす方向は手作業を増やす) か、吸収できない理由を deviation に書く。

事後判定: evidence の実行例が、ガイドの各経路 (環境変数あり / なし等) ごとに 1 件ずつあり、いずれも同じ前置なしで成功している。

## 経緯

- 2026-09-02 add-consumer-verification: 実装者の環境は `ANDROID_HOME` 設定済みで、消費者検証は引数なしで通っていた。レビュアーが環境変数を外して `local.properties` 経路で再実行し `SDK location not found` を検出。handbook を読めば 2 経路の存在は書かれていたが、実装は自分の環境しか試していなかった。修正は `ANDROID_HOME` → `ANDROID_SDK_ROOT` → `android/local.properties` の順に解決する共通スクリプトで閉じ、handbook 側は変更不要だった。
