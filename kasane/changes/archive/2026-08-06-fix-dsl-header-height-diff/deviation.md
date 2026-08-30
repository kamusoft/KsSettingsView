# Deviation: fix-dsl-header-height-diff

デルタスペックと実装の間で、合意のうえ生じた差分を記録する (spec 本体は書き換えない)。

- **Requirement「SwiftUI DSL の headerHeight 変更の表示反映」の `.replaceCell` 発行範囲**: spec では「同一再評価内で同一 ID の Cell の内容も変わっている場合、`.full(newRoot)` に続けて当該 Cell の `.replaceCell` を発行する」と**無条件**に記述 → 実装は**新ツリーで可視な Cell (Section の `isVisible` と `VisibilityAware.isVisible` の双方が true) に限定**して発行する。理由: レビュー指摘 (second-opinion-002 Major-1) の修正で可視性変化との併発を扱う際、非表示 Cell へ `.replaceCell` を出すと host 側 `applyReplaceCell` の no-op に依存する形になるため、オーケストレーター判断で対象を絞った。非表示 Cell の内容は先行する `.full` が model へ反映しており、再表示時に新しい内容で構成されるため**観測結果は spec の Scenario と同一** (verify-002 で確認済み・Scenario 単位の乖離なし)。(2026-08-05)

## 蒸留時の申し送り

- `concepts/core/architecture/display-state-synchronization.md` に「非表示 Cell の内容更新は `.full` の model 反映に委ね、`.replaceCell` は可視 Cell にのみ発行する」を知識として書き下す (verify-002 の提案)
