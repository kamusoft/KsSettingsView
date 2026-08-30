---
scope: test
timestamp: 2026-08-13
---

# lessons: test

- [L-001] UI の動的挙動 (タップ領域・スクロール・状態遷移・更新通知) のテストは、保証したい性質そのもの (実タップ判定・実レイアウト制約・実イベント列・実通知の発行) を実経路で検証する。観測しやすい代理値 (容器の幅・直接代入した状態・人工的な measure 制約・新規 ViewHolder への直接 bind) が緑でも保証にならない。状態遷移は往復両方向を通す。事例は [details/test-asserts-proxy-not-real-path.md](details/test-asserts-proxy-not-real-path.md)。(昇格: 2026-08-05、出典: android-picker-selection-sheet / android-numberpicker-modern-ui / fix-android-accessory-header-refresh)
- [L-002] 両 OS 対称のテスト表・パラメータ化テストへケースを追加するときは、観測点が対象の回帰を実際に検出できるか (実装へのミューテーションでそのケースが落ちるか) を追加した側の OS でも確認する。ケース名と GIVEN/WHEN が対称でも、観測点 (何を assert するか) が非対称なら片側だけ空振りする。事例は [details/symmetric-test-case-added-without-regression-detection.md](details/symmetric-test-case-added-without-regression-detection.md)。(昇格: 2026-08-13、出典: harden-update-accessory-unknown-id / fix-ios-replace-cell-type-change / align-maui-accessory-placement-guard)
