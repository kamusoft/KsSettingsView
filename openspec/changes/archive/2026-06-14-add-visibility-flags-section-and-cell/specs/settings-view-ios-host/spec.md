## MODIFIED Requirements

### Requirement: DiffableDataSource

`KsSettingsViewController` は内部で `UICollectionViewDiffableDataSource<UUID, KsCellID>` を保持しなければならない (SHALL)（Section 識別子は `UUID`、Item 識別子は `KsCellID` で `Hashable`）。スナップショットの**構造同期（Section / Item の追加・削除・移動・差し替え）は `KsCellID`（id）の同一性のみで算出されなければならない** (MUST)。Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）の `Hashable` 等価性を構造同期（snapshot の item 集合・順序の再構築）の判定に用いてはならない (MUST NOT)（「表示状態同期の三層分離」参照）。装飾領域（Section H/F、Root H/F）の `KsAnyView` は差分検出に参加せず、`SettingsRoot` / `Section` 等の `Hashable` 実装は `view` ケースの中身を判定対象外として扱わなければならない (MUST)。

`KsSettingsViewController` は `cellIndex: [KsCellID: any KsCell]` 等により id → 最新 Cell モデルのマップを保持し、`cellProvider` はこのマップから最新 Cell を引いて描画しなければならない (MUST)。

`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、`NSDiffableDataSourceSnapshot` の構造操作（`insertItemsBefore` / `deleteItems` / `moveItemBefore` / `moveItemAfter` / `appendSections` / `deleteSections` / `moveSection` 等）を実行しなければならない (MUST)。同一 id の Cell の**内容更新（`replaceCell`）は、セルを破棄・再生成する `reloadItems` ではなく `reconfigureItems`（iOS 15+、同一セルインスタンスを破棄せず `cellProvider` で再構成）で反映しなければならない** (MUST)。`reloadItems` を内容更新に用いてはならない (MUST NOT)。Deployment Target が iOS 15 未満を含み `reconfigureItems` が利用できない場合に限り `reloadItems` へフォールバックしてよい (MAY)。

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `controller.applyDiff(.insertCell(sectionID: sid, at: 0, cell: newCell))`
- **WHEN** スナップショット適用を観察する
- **THEN** snapshot に 1 件だけ新しい `KsCellID` が挿入され、その Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除時のアニメーション

- **GIVEN** `controller.applyDiff(.removeCell(cellID: cid))`
- **WHEN** スナップショット適用を観察する
- **THEN** snapshot から該当 `KsCellID` が削除され、その Cell 行が削除アニメーションで消える

#### Scenario: Section 移動時の挙動

- **GIVEN** Section が 3 つ並んでいる状態
- **WHEN** `controller.applyDiff(.moveSection(from: 0, to: 2))` を呼ぶ
- **THEN** snapshot 上の Section 順序が移動先に反映され、Section とその Cell が一体で移動アニメーションする

#### Scenario: 内容更新は reconfigureItems で反映（セル破棄なし）

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）が変化し、`controller.applyDiff(.replaceCell(cellID: cid, new: updated))` が呼ばれる
- **WHEN** スナップショット適用を観察する
- **THEN** `cellIndex` の当該 Cell が更新され、`snapshot.reconfigureItems([cid])` により同一セルインスタンスが破棄されずに再構成される。セルの破棄・再生成（reload）やそれに伴うちらつきは発生しない

#### Scenario: 存在しない cellID への操作（DEBUG）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** DEBUG ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** `assertionFailure(...)` で即座にクラッシュする

#### Scenario: 存在しない cellID への操作（Release）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** Release ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** クラッシュせず、`os_log` 等でログ出力されるのみで snapshot は変更されない

#### Scenario: チェック系の TwoWay トグル

- **GIVEN** Checkbox / Switch / SimpleCheck の CellView が表示されている
- **WHEN** ユーザーがセルをタップ／操作する
- **THEN** CellView が自身の表示状態を直接更新し `onValueChanged` でモデルへ書き戻す（TwoWay）。この内容更新は snapshot の構造再構築を経由せず、行全体の再描画は発生しない。RadioCell のグループ連動（他セルの選択解除）は該当セルの reconfigure で反映する

## ADDED Requirements

### Requirement: visible projection の二重管理

`KsSettingsViewController` は、`SettingsRoot` を hidden 含むフル状態として保持しつつ、UI 描画には visible projection（`Section.isVisible` および各 Cell の `VisibilityAware.isVisible` が `true` のもののみで構成される派生ビュー）を用いなければならない (MUST)。

具体的には、以下の UI 描画経路で参照される sections は **visible projection ベース** でなければならない (MUST)：

- `UICollectionViewCompositionalLayout` の `sectionProvider` クロージャ
- セクションヘッダ／フッタ supplementary view の生成
- 罫線（separator）の `itemSeparatorHandler` 経路
- `NSDiffableDataSourceSnapshot` の section / item 構築

一方、以下の経路は **model（`root.sections`）ベース** でなければならない (MUST)：

- `SettingsRootDiff` 受信時の対象 Section / Cell の探索（hidden 対象を見つけられないと no-op 判定ができない）
- 部分 Diff の `index` 引数の解釈

#### Scenario: visible projection で section が除外される

- **GIVEN** `SettingsRoot.sections` に `isVisible = false` の Section を含む
- **WHEN** `KsSettingsViewController` が snapshot を構築する
- **THEN** 当該 Section は snapshot の section identifiers に含まれず、UI 上には描画されない。一方で `root.sections` には保持される

#### Scenario: visible projection で cell が除外される

- **GIVEN** visible な Section の `cells` に `isVisible = false` の Cell（`VisibilityAware` 準拠）を含む
- **WHEN** `KsSettingsViewController` が snapshot を構築する
- **THEN** 当該 Cell は snapshot の item identifiers に含まれず、UI 上には描画されない

#### Scenario: indexPath ベースの描画経路が visible projection を参照する

- **GIVEN** hidden な Section が `root.sections` の先頭にあり、その後ろに visible な Section が続く構成
- **WHEN** `indexPath.section = 0` で separator / supplementary view を生成する
- **THEN** 参照される Section は visible projection の先頭（= root.sections[1] にあたる visible Section）であり、hidden Section が参照されることはない

### Requirement: 部分 Diff の index 規約と hidden 対象の no-op

`KsSettingsViewController.applyDiff(_:)` は、`SettingsRootDiff` の部分 Diff ケース（`insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `replaceCell` / `moveCell` / `updateAccessory`）について、以下の規約に従わなければならない (MUST)。

**index 引数の解釈:**

部分 Diff の `index` / `at` / `to` 引数は、すべて **model 配列基準（hidden 含む）** で解釈しなければならない (MUST)。visible projection 基準で解釈してはならない (MUST NOT)。

**hidden 対象の挙動:**

- 対象 Section / Cell が hidden の場合、UI 層は当該 Diff のために `NSDiffableDataSourceSnapshot` への構造操作（`insertSections` / `deleteSections` / `insertItems` / `deleteItems` 等）を行ってはならない (MUST NOT)。
- model（`root`）の更新は実行しなければならない (MUST)。これにより、後で `isVisible = true` に切り替わったときに正しい状態で復活する。
- hidden 対象を指す `removeCell` / `moveCell` / `updateAccessory` 等が snapshot 上の missing ID として観測される場合、エラーや警告として扱わず通常の no-op として処理しなければならない (MUST)。

**visible 対象の挙動:**

- 対象 Section / Cell が visible の場合、UI 層は通常通り snapshot 操作を行わなければならない (MUST)。
- 部分 Diff の `index` 引数（model 基準）から visible projection 上の正しい位置を算出して snapshot に反映しなければならない (MUST)。

#### Scenario: hidden Cell への removeCell は no-op

- **GIVEN** `Section` に `isVisible = false` の Cell を含み、その Cell に対する `SettingsRootDiff.removeCell(cellID)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model（`root`）から当該 Cell が削除される一方、snapshot への構造操作は行われない。エラーや警告は発生しない

#### Scenario: hidden Section への updateAccessory は no-op だが model は更新される

- **GIVEN** `isVisible = false` の Section に対する `updateAccessory` Diff が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 上の当該 Section の `header` / `footer` は更新される一方、snapshot への構造操作は行われない。後で当該 Section が `isVisible = true` に切り替わったとき、更新済みの accessory が描画される

#### Scenario: insertCell の index は model 配列基準

- **GIVEN** `Section.cells` が `[A(visible), B(hidden), C(visible)]` の状態で `insertCell(sectionID:, at: 2, cell: D)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 配列基準で index = 2 の位置（= C の前）に D が挿入され、`Section.cells` は `[A, B, D, C]` となる。visible projection 上では `[A, D, C]` として描画される

#### Scenario: moveCell で hidden を跨ぐ移動

- **GIVEN** `Section.cells` が `[A(visible), B(hidden), C(visible), D(visible)]` で `moveCell(cellID: A, to: 3)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 配列基準で A が index 3 に移動し、`Section.cells` は `[B, C, D, A]` となる。visible projection 上では `[C, D, A]` として描画される

### Requirement: ReplaceCell / ReplaceSection の可視性切替防御

DSL / アプリ層は、`SettingsRootDiff.replaceCell` / `SettingsRootDiff.replaceSection` で可視性（`isVisible`）だけを変える操作を行ってはならない (MUST NOT)。可視性変更は `SettingsRootDiff.full(newRoot)` 経由で発行されなければならない (MUST)。

UI 層 (`KsSettingsViewController`) は、受け取った `replaceCell` で旧 Cell と新 Cell の `isVisible` が異なることを **`root.sections` から取得した旧値で** 検出した場合、Full 経路（`applyFullSnapshot` 相当）にフォールバックしなければならない (MUST)。検出は snapshot の存在チェックよりも先に行わなければならず (MUST)、旧 Cell が hidden であっても model 上から取得した旧値で判定できなければならない (MUST)。

UI 層は、受け取った `replaceSection` を常に Full 経路（`applyFullSnapshot` 相当）で処理しなければならない (MUST)。`replaceSection` は型上 Section 全体置換であり、`header` / `footer` / `headerHeight` / `isVisible` / `cells` の任意の変化を内包し得るため、内部 cell の細粒度差分抽出を試みてはならない (MUST NOT)。

#### Scenario: ReplaceCell で visibility 切替が検出される

- **GIVEN** model 上の Cell `X` が `isVisible = true` で、新 Cell `X'`（同一 id、`isVisible = false`）を伴う `replaceCell(cellID: X, new: X')` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は可視性切替を検出し、Full 経路にフォールバックする。snapshot は再構築され、当該 Cell は visible projection から除外される

#### Scenario: ReplaceSection は常に Full 経路で処理される

- **GIVEN** `replaceSection(sectionID:, new: newSection)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は内部の cells / accessory / visibility の細粒度差分を抽出せず、Full 経路にフォールバックする。snapshot は新 Section の visible projection に基づいて再構築される

### Requirement: partial Section / UpdateAccessory の supplementary 追従

`KsSettingsViewController` は、Section の追加・削除・置換・移動および `updateAccessory`（Section H/F 対象）の処理後、visible projection の最新状態が **section ごとの header / footer 表示有無として正しく反映** されることを保証しなければならない (MUST)。判定対象は **visible projection** であり、hidden Section の header / footer は judging に影響してはならない (MUST NOT)。

実装方式は (a) `UICollectionViewLayout` インスタンスを差し替える、(b) 同一 layout インスタンス内で `sectionProvider` クロージャが section ごとに supplementary 構成を動的に決定する、のいずれでもよい。ただし `UICollectionViewDiffableDataSource` の差分アニメーション中に `setCollectionViewLayout(_:animated:)` を同期実行すると、Compositional Layout 側の section 構造再構築と DiffableDataSource の section アニメが衝突し、全 Cell バウンドや描画乱れを招くことが確認されている。したがって UI 層は、**`dataSource.apply(_:animatingDifferences:)` の差分アニメーションと同じ更新サイクル内で `setCollectionViewLayout(_:animated:)` を呼んではならない** (MUST NOT)。

#### Scenario: hidden Section の header は判定に影響しない

- **GIVEN** `[S1(visible, header=nil), S2(hidden, header="A")]` の構成
- **WHEN** visible projection の header 有無を判定する
- **THEN** visible projection は `[S1]` のみで構成され、header を持つ visible section が無いと判定される。S2 の header は判定に影響しない

#### Scenario: partial Diff で visibility が変化したら supplementary が追従する

- **GIVEN** すべての visible section が header を持たない状態で、`insertSection(at: ..., section: newSection)` により header を持つ新 visible section が追加される
- **WHEN** `applyDiff` が処理する
- **THEN** 追加された Section が描画されるとき、その header supplementary view が正しく表示される。既存 visible section の表示状態は変化しない

#### Scenario: 差分アニメと layout 差し替えの同時実行は禁止

- **GIVEN** `applyFullSnapshot` / 部分 Diff のいずれかが visibility 変化を伴い、`dataSource.apply(_, animatingDifferences: true)` が呼ばれる
- **WHEN** 同じ更新サイクル内で `setCollectionViewLayout(_, animated: false)` を併用する
- **THEN** これは禁止であり (MUST NOT)、UI 層は同期差し替えを避けて supplementary 追従を実現しなければならない（例: `sectionProvider` の動的評価 + `invalidateLayout()` 等）
