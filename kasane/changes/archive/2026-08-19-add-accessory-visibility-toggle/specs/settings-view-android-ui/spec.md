# Delta: settings-view-android-ui (add-accessory-visibility-toggle)

## ADDED Requirements

### Requirement: Section の Header / Footer 表示トグル

`Section` は `isHeaderVisible` / `isFooterVisible` (いずれも `Boolean`、既定 `true`) を公開し、Header / Footer の表示判定を「トグル && 内容あり」の AND 合成とする (core/ADR-0023)。トグルが `false` のとき、内容があっても該当の Header / Footer 行を生成してはならない (SHALL NOT)。両フィールドは `Section` の値等価性に参加する (SHALL)。

#### Scenario: 内容がある Header をトグルで隠す
- **GIVEN** header に Text または View が設定された Section
- **WHEN** `isHeaderVisible = false` で表示する
- **THEN** その Section の Header 行はリストに現れない

#### Scenario: 内容がある Footer をトグルで隠す
- **GIVEN** footer に Text または View が設定された Section
- **WHEN** `isFooterVisible = false` で表示する
- **THEN** その Section の Footer 行はリストに現れない

#### Scenario: 非空 accessory では既定値で現行挙動と一致する
- **GIVEN** 非空の header / footer を持ちトグルを指定しない (既定 `true`) Section
- **WHEN** 表示する
- **THEN** Header / Footer の表示は従来と同一である

#### Scenario: replaceSection でトグル変更が反映される
- **GIVEN** Header が表示されている Section
- **WHEN** `isHeaderVisible = false` にした Section で `replaceSection` を適用する
- **THEN** Header 行が消える (逆方向の `true` への変更では再表示される)。同一 ID の Cell 群は保持される

#### Scenario: トグルは値等価性に参加する
- **GIVEN** トグル以外が同一内容の2つの Section
- **WHEN** 一方だけ `isFooterVisible = false` にして比較する
- **THEN** 2つの Section は等価にならない

### Requirement: トグルの独立性と保持

Header / Footer のトグルは互いに独立で、一方を `false` にしても他方と Cell 群の表示に影響してはならない (SHALL NOT)。Cell の挿入・削除・置換・移動、accessory 更新をまたいでトグル値を保持する (SHALL)。非表示中も accessory の内容は Section の状態として保持され、再表示時は最新の内容を表示する (SHALL)。

#### Scenario: Footer を隠しても Header と Cell は表示されたまま
- **GIVEN** header・footer・Cell を持つ Section
- **WHEN** `isFooterVisible = false` だけを設定して表示する
- **THEN** Header と Cell 群は従来どおり表示される

#### Scenario: Cell 操作をまたいでトグルが保持される
- **GIVEN** `isHeaderVisible = false` の Section
- **WHEN** その Section へ Cell の挿入 (または削除・置換・移動) を適用する
- **THEN** Header は非表示のままである

#### Scenario: 非表示中の内容更新が再表示に反映される
- **GIVEN** `isHeaderVisible = false` の Section
- **WHEN** 非表示中に `updateAccessory` で header text を変更し、その後 `isHeaderVisible = true` にする
- **THEN** 変更後の text で Header 行が表示される

### Requirement: 内容不在の統一判定 (iOS への対称化)

「内容の不在」は **null または空 text** とし、header / footer で共通の判定とする (SHALL)。内容が不在の Header / Footer には、トグルの値に依らず行を生成してはならない (SHALL NOT)。従来の「空文字 text でも行が生成される」挙動は廃止する (公開挙動の変更、core/ADR-0023)。

#### Scenario: 空 text の Header は行を生成しない
- **GIVEN** header が `SectionAccessory.Text("")` の Section
- **WHEN** 表示する
- **THEN** Header 行はリストに現れない

#### Scenario: 空 text の Footer は行を生成しない
- **GIVEN** footer が `SectionAccessory.Text("")` の Section
- **WHEN** 表示する
- **THEN** Footer 行はリストに現れない

### Requirement: 宣言 DSL のトグル指定と Store 経路との対称性

Compose 宣言 DSL の Section 構築 (`DSLScope.Section`) で `isHeaderVisible` / `isFooterVisible` を指定でき、解決後の `Section` へ転写する (SHALL)。DSL の再評価でトグルだけが変わった場合も表示へ反映する (SHALL)。Store 経路と DSL 経路で同じトグル状態は同じ表示結果に到達する (core/ADR-0018 の対称テストを追加する)。

#### Scenario: DSL でトグルを指定して構築する
- **GIVEN** `isHeaderVisible = false` を指定した DSL の Section
- **WHEN** 表示する
- **THEN** その Section の Header 行はリストに現れない

#### Scenario: DSL 再評価でトグル変更が反映される
- **GIVEN** DSL で表示中の Header を持つ Section
- **WHEN** 再評価で `isHeaderVisible` が `false` に変わる
- **THEN** Header 行が消える (逆方向も同様)

#### Scenario: Store 経路と DSL 経路の表示結果が一致する
- **GIVEN** 同一内容の Section に対する同じトグル操作
- **WHEN** Store 経由と DSL 経由でそれぞれ適用する
- **THEN** 両経路の表示結果は一致する
