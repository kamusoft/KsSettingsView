---
scope: spec-review
timestamp: 2026-08-12
---

# lessons: spec-review

- [L-001] spec の Scenario / Requirement が置く前提 (数値境界への到達可能性、復元・取得系なら参照元がその状態を実際に保持していること、WHEN の操作が公開面・画面構成で実際に実行可能であること等) は、入力型の値域やソース照合で成立可能性を検算してから提案に載せる。成立不能なら、その前提の Scenario を書かずに Requirement 本文で扱いを明記する (対象外化・防御的ガードの注記等) か、前提を成立可能な形に引き直す。同一 Requirement 内の他の SHALL と WHEN が矛盾しないことも検算対象。経緯は [details/spec-scenario-premise-unreachable-in-input-domain.md](details/spec-scenario-premise-unreachable-in-input-domain.md)。(昇格: 2026-08-12、出典: android-datepicker-spinner-wheel / clarify-host-attach-order-contract / add-maui-custom-cell)
