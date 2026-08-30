---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-21
last-seen: 2026-08-21
evidence:
  - upgrade-android-build-toolchain (Scenario「MAUI binding からの native ビルド」の THEN「4 module の release aar が生成され」が、csproj の Exec が設計上呼ぶ 3 module (core / ui / bridge、maui/ADR-0006) と不一致。ホスト側レビュー・相方レビューの双方が NEEDS_DISCUSSION の理由に挙げ、deviation 記録で吸収した)
---

## ルール文

Scenario の THEN が成果物の**数・一覧** (生成される aar / モジュール / ファイル等) を断定するときは、その成果物を実際に生成する既存経路 (csproj の Exec コマンド・スクリプト・CI ジョブ) のソースを開いて対象を数え、spec の数と一致させてから提案に載せる。「リポジトリに N module ある」は「その経路が N module を生成する」の根拠にならない — 消費側 (binding 等) が一部しか使わない設計は ADR に既に書かれていることが多い。spec-review L-001 (前提の成立可能性の検算) の THEN 側への適用。

## 経緯

- 2026-08-21 upgrade-android-build-toolchain: exploration で「bridge module が追加され現在は 4 module」と再確認した流れのまま、MAUI binding の Scenario THEN に「4 module の release aar」と書いた。実際の `KsSettingsView.Binding.Android.csproj` の Exec は `:ks-settingsview-core` / `:ks-settingsview-ui` / `:ks-settingsview-bridge` の 3 つだけを `assembleRelease` し、compose は束縛対象外 (maui/ADR-0006 の設計どおり)。実装・検証は正しく 3 module で通っていたが、verify が ⚠️、レビュー 2 系統が NEEDS_DISCUSSION を返し、deviation 記録 (spec 側の誤記) で閉じることになった。spec 作成時に csproj の Exec 行を 1 度開いていれば防げた。
