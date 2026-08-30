# 経緯: 実機/視覚証跡を完了・承認の条件にする (L-003)

2026-08-12 の蒸留 (add-maui-accessory-views) で、同一の根「実機/視覚で確かめずに完了・承認・照合済みにする」を共有する inbox 4 件をオーナー合意で統合し、count 6 (独立 change 6 件・重複なし) で昇格した。

## 統合元と evidence

- `review-approved-runtime-fix-without-repro-evidence` (code-review / 1): fix-entrycell-ime-composition — review-001 が実機証跡なしで実行時挙動の修正を APPROVED し、オーナー実機確認で症状未解消と判明。実機確認は Suggestion 止まりで判定条件になっていなかった
- `stale-verification-screenshots-not-retaken` (ui-impl / 2): android-picker-selection-sheet — APPROVED 後の配色・文字サイズ変更で verification 11 枚中 9 枚が旧状態のまま「照合完了 + オーナー最終承認」になった / ios-picker-selection-parity — 照合スクショ 3 点が Major 修正前のビルドのまま提出コードと対応が切れていた
- `static-screenshots-miss-transition-animation-defects` (ui-impl / 1): add-cell-types-custom — 展開遷移で content が飛び出してから落ちる動き。静止画照合・全テスト green・レビューのいずれも検出せず、オーナーの実機操作で発覚
- `visible-appearance-fix-reported-done-without-visual-check` (process / 2): fix-decoration-theme-not-applied-on-initial-bind / fix-android-header-height-refresh — いずれも「ユニット再現可能だから runtime-behavior-verification 規約の対象外」という判断が視覚確認を省く理由になり、オーナー指摘で実機 A/B を追加実施。2 件とも実機確認の副産物として別の実欠陥 (iOS 側の同症状 / Compose DSL 経路の diff 欠落) を発見した

## 既存規約との関係

`cross/conventions/runtime-behavior-verification.md` は「ユニット再現不能な不具合」を完了 3 条件の対象とするが、本パターンはその適用範囲外 (ユニット再現可能) の外観・実行時変更でも視覚証跡を要求する — 規約の隙間がまさに再発点だった (6 件中 2 件が同型の判断で省略)。

関連 (統合対象外): success 側の `inbox/pixel-measurement-before-ui-alignment-fix.md` (実装前の画素測定で仮説を裏取りする成功パターン) は別ルールとして inbox に残置。
