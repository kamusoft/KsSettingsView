---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-08-16
last-seen: 2026-08-16
evidence:
  - perf-android-customcell-composition-reuse (tasks 2.7 を記録なしでチェック → verify-001 INVALID。新設した記録が修正前ビルドの SHA に対する測定 → review-002 Major-1。コメント修正後に SHA が再ドリフト → review-003 が注記追加を要求。1 change 内で 3 回の齟齬)
---

## ルール文

ミューテーション・実機検証など「提出物の検出力・成立」を主張する記録には測定対象ソースの SHA を記し、記録後にコードへ変更を入れたら同一手順で取り直すか、差分が結果へ影響しない根拠を新 SHA 付きで追記する。tasks の検証タスクは、記録ファイルが change 配下に存在し提出コードと対応して初めてチェックする。

## 経緯

- 2026-08-16 perf-android-customcell-composition-reuse: tasks 2.7 (検出力確認) がチェック済みなのに記録が存在せず verify-001 が INVALID 判定。記録新設後も測定対象がクラッシュ修正前ビルドで review-002 Major-1 (提出コードの証跡になっていない — 特に修正の中核 measure guard の検出力が未記録)。取り直し後、review-003 Minor のコメント修正で再び SHA が乖離し、アーカイブ前の注記追加が必要になった。SHA の明記自体は review-002 以降機能した (乖離の検出はすべて SHA 照合による)。
- 類似パターン: [device-evidence-left-in-session-scratchpad](device-evidence-left-in-session-scratchpad.md) (証跡の所在の欠落)。こちらは証跡の**版**が提出物と対応しない点で異なる。
