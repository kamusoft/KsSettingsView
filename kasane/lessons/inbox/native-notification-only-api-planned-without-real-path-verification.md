---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-29
last-seen: 2026-08-29
evidence:
  - restore-maui-picker-selected-command (SelectedCommand は「native の確定通知でしか発火しない」設計なのに、初回の提案は facade テスト (controller の sink 直呼び) だけを完了条件とし、Sample 反映と Simulator / Emulator での実経路検証が計画から漏れた。verify-001 / 002 も sink 直呼びテストを Scenario の対応根拠として通しており、公開 API の発火側が一度も実経路で確認されないまま完了しかけた。再探索 (オーナー起点) でスコープを拡張して塞いだ)
---

## ルール文

公開 API の発火源が native 通知だけの場合 (直接 setter からは到達できない設計)、facade / controller の通知口を直接呼ぶ単体テストは公開 API の動作確認にならない — 提案・spec レビューの段階で、native 面 → gateway → facade の実経路を踏む検証 (Simulator / Emulator 実行と証跡) が完了条件に含まれているかを確認する。

## 経緯

- 2026-08-29 restore-maui-picker-selected-command: 初回スコープは facade の API 復元とユニットテストのみで、実経路検証と Sample 反映が漏れていた。オーナー起点の再探索で「発火側の実経路が未検証 = 公開 API の動作そのものが未確認」と整理され、Sample の経路組み替え + 両 OS の実経路検証 (証跡 56 枚) をスコープに加えて決着した。実装ウェーブに E2E を含める成功則 [[e2e-in-impl-wave-caught-handler-platform-bugs]] の spec-review 側の裏面にあたる。
