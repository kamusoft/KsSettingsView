# Delta: settings-view-ios-ui (fix-dsl-header-height-diff)

## ADDED Requirements

### Requirement: SwiftUI DSL の headerHeight 変更の表示反映

SwiftUI DSL の diff 算出は、旧・新の宣言ツリーの間で同一 ID の Section の `headerHeight` が変化している場合、可視性 preflight と同じ段階で検出し、`.full(newRoot)` を発行する SHALL。検出対象の変化は、固定高さ間の変更 (`正値A → 正値B`)・自動から固定 (`-1 → 正値`)・固定から自動 (`正値 → -1`) のいずれも含む SHALL。これにより DSL 経由の headerHeight 変更は、Store 経由 (`replaceSection` / `.full`) と同じ表示結果へ到達する (core/ADR-0018)。

同一再評価内で同一 ID の Cell の内容も変わっている場合、diff 算出は `.full(newRoot)` に**続けて**当該 Cell の `.replaceCell` を発行する SHALL (`.full` → `.replaceCell` の順)。これにより header の高さと Cell の内容の両方が表示へ反映される。

#### Scenario: headerHeight のみの変更が表示へ反映される
- **GIVEN** Text accessory の header を持つ Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で同一 Section ID のまま `headerHeight` だけが変わる
- **THEN** diff 算出は `.full(新ツリー)` を発行し、表示中の header の高さは新しい固定高さになる

#### Scenario: headerHeight と Cell 内容の同時変更で両方が反映される
- **GIVEN** Text accessory の header と既知の ID の Cell を持つ Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で同一 ID のまま `headerHeight` と Cell の内容 (title 等) が同時に変わる
- **THEN** diff 算出は `.full(新ツリー)` に続けて当該 Cell の `.replaceCell` を発行し、表示は header の高さと Cell の内容の両方が新しくなる

#### Scenario: headerHeight が不変なら preflight は発火しない
- **GIVEN** Section が SwiftUI DSL で表示されている
- **WHEN** 再評価で `headerHeight` は同一のまま Cell の内容だけが変わる
- **THEN** `.full` は発行されず、内容変化は `.replaceCell` (reconfigure 経路) の既存経路で反映される

### Requirement: Store 経由の headerHeight 変更の表示反映

iOS host は、section identity が同一のまま `headerHeight` だけが変わる Store 経由の更新 (`replaceSection`・`.full`) を、表示中の Section header の高さへ反映する SHALL (core/ADR-0018 の経路対称の iOS Store 側の確認 — 探索時点で未検証)。

#### Scenario: replaceSection による headerHeight 変更が表示へ反映される
- **GIVEN** Text accessory の header を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Section ID のまま `headerHeight` だけが異なる Section へ `replaceSection` する
- **THEN** 表示中の header の高さは新しい固定高さになる

#### Scenario: .full による headerHeight 変更が表示へ反映される
- **GIVEN** Text accessory の header を持つ Section が Store 接続で表示されている
- **WHEN** 同一 Section ID・同一 header accessory のまま `headerHeight` だけが異なる root で `.full` を適用する
- **THEN** 表示中の header の高さは新しい固定高さになる
