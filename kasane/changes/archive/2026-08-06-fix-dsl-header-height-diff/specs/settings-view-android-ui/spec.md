# Delta: settings-view-android-ui (fix-dsl-header-height-diff)

## ADDED Requirements

### Requirement: Compose DSL の headerHeight 変更の表示反映

Compose DSL の diff 算出は、旧・新の宣言ツリーの間で同一 ID の Section の `headerHeight` が変化している場合、可視性変化と同様に preflight で検出し、`SettingsRootDiff.Full(newRoot)` のみを発行する SHALL。検出対象の変化は、固定高さ間の変更 (`正値A → 正値B`)・自動から固定 (`-1.0 → 正値`)・固定から自動 (`正値 → -1.0`) のいずれも含む SHALL。同条件下で `contentUpdates` は空リストを返す SHALL (同時に変化した Cell 内容は Full の適用が内包する)。これにより DSL 経由の headerHeight 変更は、Store 経由 (`replaceSection` / full 更新) と同じ表示結果へ到達する (core/ADR-0018)。

#### Scenario: headerHeight のみの変更が表示へ反映される
- **GIVEN** Text accessory の header を持つ Section が Compose DSL で表示されている
- **WHEN** 再評価で同一 Section ID のまま `headerHeight` だけが変わる
- **THEN** diff 算出は `Full(新ツリー)` のみを返し、表示中の header の高さは新しい固定高さになる

#### Scenario: headerHeight と Cell 内容の同時変更で両方が反映される
- **GIVEN** Text accessory の header と既知の ID の Cell を持つ Section が Compose DSL で表示されている
- **WHEN** 再評価で同一 ID のまま `headerHeight` と Cell の内容 (title 等) が同時に変わる
- **THEN** diff 算出は `Full(新ツリー)` のみを返し、`contentUpdates` は空リストを返し、表示は header の高さと Cell の内容の両方が新しくなる

#### Scenario: headerHeight が不変なら preflight は発火しない
- **GIVEN** Section が Compose DSL で表示されている
- **WHEN** 再評価で `headerHeight` は同一のまま Cell の内容だけが変わる
- **THEN** `Full` は発行されず、内容変化は `contentUpdates` の列挙による既存の部分更新経路で反映される
