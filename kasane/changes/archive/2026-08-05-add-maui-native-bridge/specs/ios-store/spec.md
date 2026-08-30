# Delta Spec: ios-store (add-maui-native-bridge)

## ADDED Requirements

### Requirement: 複数 Cell 内容更新のバッチ適用 (replaceCells)

iOS の `SettingsRootStore` は、複数 Cell の内容更新を1回の呼び出しで適用する `replaceCells` を提供しなければならない (SHALL)。観察可能挙動は Android の既存 `replaceCells` と対称とする:

- 状態は1回で更新され、適用された Cell ID 群が1回のバッチ内容更新として配信される。配信は状態更新の後に行われ、購読者は配信時点で更新後の現在状態を参照できる
- 更新は入力順に適用される。既知 ID と未知 ID が混在する場合、既知 ID だけが適用・配信される
- 配信される ID 群は適用順であり、同一 ID が複数回適用された場合は適用ごとに含まれる (Android の現行挙動と対称)
- 同一 ID を複数回指定した場合は入力順に適用され、最後の値が状態に残る
- 個々の更新は同じ ID の内容更新であり、Cell の identity を変えない。呼び出し側は対象 cellID と新しい Cell の ID を一致させる — 不一致を渡した場合の挙動は公開契約として保証しない

`replaceCells` は内容更新専用である。呼び出し側は `isVisible` を変更する Cell を渡してはならない — 可視性の変化は内容更新ではなく full 更新・構造変更の経路で行う (display-state-synchronization の既存契約)。この契約に違反した場合の表示挙動は公開契約として保証しない。

#### Scenario: 複数 Cell の更新が1バッチで配信される
- **GIVEN** 複数の Cell を含む root を保持する Store
- **WHEN** `replaceCells` に複数の (cellID, 新 Cell) を渡す
- **THEN** Store の現在状態は全ての更新を反映し、適用された ID 群を含むバッチ内容更新が1回だけ配信される

#### Scenario: 既知・未知 ID の混在では既知だけが適用・配信される
- **GIVEN** root を保持する Store
- **WHEN** `replaceCells` に既知 ID と存在しない ID の更新を混在して渡す
- **THEN** 既知 ID の更新だけが状態へ適用され、配信されるバッチには既知 ID だけが含まれる

#### Scenario: 存在しない ID は無視され適用0件なら配信しない
- **GIVEN** root を保持する Store
- **WHEN** `replaceCells` に存在しない cellID のみを渡す
- **THEN** 状態は変化せず、バッチ内容更新は配信されない

#### Scenario: 空リストは no-op
- **GIVEN** root を保持する Store
- **WHEN** `replaceCells` に空リストを渡す
- **THEN** 状態は変化せず、通知は配信されない

#### Scenario: 同一 ID の重複指定は最後の値が残る
- **GIVEN** root を保持する Store
- **WHEN** `replaceCells` に同一 cellID の更新を2回 (値 A → 値 B の順) 含めて渡す
- **THEN** Store の現在状態には値 B が残る

### Requirement: Native Host のバッチ内容更新反映

iOS Native Host は、Store のバッチ内容更新を受け取り、対象 Cell の表示内容を構造変更なしで更新しなければならない (SHALL)。

#### Scenario: バッチ更新が表示へ反映される
- **GIVEN** Host に接続され表示中の設定 list
- **WHEN** Store の `replaceCells` で表示中の複数 Cell の内容を更新する
- **THEN** 対象行の表示内容が更新され、行の追加・削除・移動 (構造変更) は発生しない
