# Delta: settings-view-ios-ui (add-accessory-visibility-toggle)

## ADDED Requirements

### Requirement: Section の Header / Footer 表示トグル

`Section` は `isHeaderVisible` / `isFooterVisible` (いずれも `Bool`、既定 `true`) を公開し、Header / Footer の表示判定を「トグル && 内容あり」の AND 合成とする (core/ADR-0023)。トグルが `false` のとき、内容があっても該当の Header / Footer 領域を生成してはならない (SHALL NOT)。両フィールドは `Section` の値等価性に参加する (SHALL)。

#### Scenario: 内容がある Header をトグルで隠す
- **GIVEN** header に text または view が設定された Section
- **WHEN** `isHeaderVisible = false` で表示する
- **THEN** その Section の Header 領域は生成されない

#### Scenario: 内容がある Footer をトグルで隠す
- **GIVEN** footer に text または view が設定された Section
- **WHEN** `isFooterVisible = false` で表示する
- **THEN** その Section の Footer 領域は生成されない

#### Scenario: 非空 accessory では既定値で現行挙動と一致する
- **GIVEN** 非空の header / footer を持ちトグルを指定しない (既定 `true`) Section
- **WHEN** 表示する
- **THEN** Header / Footer の表示は従来と同一である

#### Scenario: replaceSection でトグル変更が反映される
- **GIVEN** Header が表示されている Section
- **WHEN** `isHeaderVisible = false` にした Section で `replaceSection` を適用する
- **THEN** Header 領域が非表示になる (逆方向の `true` への変更では再表示される)

#### Scenario: トグルは値等価性に参加する
- **GIVEN** トグル以外が同一内容の2つの Section
- **WHEN** 一方だけ `isHeaderVisible = false` にして比較する
- **THEN** 2つの Section は等価にならない

### Requirement: トグルの独立性と保持

Header / Footer のトグルは互いに独立で、一方を `false` にしても他方と Cell 群の表示に影響してはならない (SHALL NOT)。Section を内部で再構築する操作 (Cell の挿入・削除・置換・移動、accessory 更新、visible projection の構築) をまたいでトグル値を保持する (SHALL)。非表示中も accessory の内容は Section の状態として保持され、再表示時は最新の内容を表示する (SHALL)。

#### Scenario: Header を隠しても Footer と Cell は表示されたまま
- **GIVEN** header・footer・Cell を持つ Section
- **WHEN** `isHeaderVisible = false` だけを設定して表示する
- **THEN** Footer と Cell 群は従来どおり表示される

#### Scenario: Cell 操作をまたいでトグルが保持される
- **GIVEN** `isHeaderVisible = false` の Section
- **WHEN** その Section へ Cell の挿入 (または削除・置換・移動) を適用する
- **THEN** Header は非表示のままである

#### Scenario: 非表示中の内容更新が再表示に反映される
- **GIVEN** `isHeaderVisible = false` の Section
- **WHEN** 非表示中に `updateAccessory` で header text を変更し、その後 `isHeaderVisible = true` にする
- **THEN** 変更後の text で Header が表示される

### Requirement: 内容不在の統一判定

「内容の不在」は **nil または空 text** とし、header / footer で共通の判定とする (SHALL)。内容が不在の Header / Footer には、トグルの値に依らず表示領域を生成してはならない (SHALL NOT)。

#### Scenario: 空 text の Header は領域を生成しない
- **GIVEN** header が `.text("")` の Section
- **WHEN** 表示する
- **THEN** Header 領域は生成されない

#### Scenario: 空 text の Footer は領域を生成しない
- **GIVEN** footer が `.text("")` の Section
- **WHEN** 表示する
- **THEN** Footer 領域は生成されない

### Requirement: 高さ解決は存在判定の後に適用する

Header の高さ解決 (core/ADR-0021 の優先順位) は「表示する」と判定された Header にのみ適用する (SHALL)。`Section.headerHeight` / `Theme.headerHeight` は存在する Header の高さを決めるだけで、Header の存在を作ってはならない (SHALL NOT)。従来の「`Section.headerHeight` 正値なら header nil でも supplementary を生成する」挙動は廃止する (公開挙動の変更。この逆契約を固定していた既存テストは新契約へ反転する)。

#### Scenario: header 不在なら Section.headerHeight 正値でも領域を生成しない
- **GIVEN** header が nil で `headerHeight` が正値の Section
- **WHEN** 表示する
- **THEN** その Section の Header 領域は生成されない

#### Scenario: 空 text の header は Section.headerHeight 正値でも領域を生成しない
- **GIVEN** header が `.text("")` で `headerHeight` が正値の Section
- **WHEN** 表示する
- **THEN** Header 領域は生成されない

#### Scenario: header 不在なら Theme.headerHeight があっても領域を生成しない
- **GIVEN** header が nil の Section と `headerHeight` が正値の Theme
- **WHEN** 表示する
- **THEN** その Section の Header 領域は生成されない

#### Scenario: トグル false なら高さ指定があっても領域を生成しない
- **GIVEN** header に内容があり `headerHeight` が正値の Section
- **WHEN** `isHeaderVisible = false` で表示する
- **THEN** Header 領域は生成されない

### Requirement: 宣言 DSL のトグル指定と Store 経路との対称性

SwiftUI 宣言 DSL の Section 構築 (`ksSection` 等) で `isHeaderVisible` / `isFooterVisible` を指定でき、解決後の `Section` へ転写する (SHALL)。DSL の再評価でトグルだけが変わった場合も表示へ反映する (SHALL)。Store 経路と DSL 経路で同じトグル状態は同じ表示結果に到達する (core/ADR-0018 の対称テストを追加する)。

#### Scenario: DSL でトグルを指定して構築する
- **GIVEN** `isHeaderVisible: false` を指定した DSL の Section
- **WHEN** 表示する
- **THEN** その Section の Header は表示されない

#### Scenario: DSL 再評価でトグル変更が反映される
- **GIVEN** DSL で表示中の Header を持つ Section
- **WHEN** 再評価で `isHeaderVisible` が `false` に変わる
- **THEN** Header が非表示になる (逆方向も同様)

#### Scenario: Store 経路と DSL 経路の表示結果が一致する
- **GIVEN** 同一内容の Section に対する同じトグル操作
- **WHEN** Store 経由と DSL 経由でそれぞれ適用する
- **THEN** 両経路の表示結果は一致する
