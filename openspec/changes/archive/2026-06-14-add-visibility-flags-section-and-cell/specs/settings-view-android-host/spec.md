## MODIFIED Requirements

### Requirement: DiffUtil 差分検出

`DiffUtil.ItemCallback<CellListItem>` を実装し、`areItemsTheSame` は ID 比較でなければならない (MUST)。`areContentsTheSame` は **同一 id（`areItemsTheSame` が true）であれば常に `true` を返さなければならない** (MUST)。すなわち `areContentsTheSame` は Cell の内容（`data class equals` の全フィールド比較）を判定に用いてはならない (MUST NOT)（「表示状態同期の三層分離」: 構造同期は id 同一性のみ）。`SectionAccessory.View(KsAnyView)` の扱い（差分検出非参加）は従来どおりとする。

`getItemId` は Cell / Section の **id に基づく安定 ID** を返さなければならない (MUST)。内容依存の `hashCode`（Cell の全フィールドから算出される値）を `getItemId` に用いてはならない (MUST NOT)。`RootHeaderFooterAdapter` の予約値（`1L` / `2L`）と衝突しない値域を維持する。

`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、追加・削除・移動・差し替え（id 変化）の構造操作で内部 `List<CellListItem>` を変更し `mainListAdapter.submitList(newList)` を呼ぶ (MUST)。一方、`replaceCell`（同一 id の内容更新）は、セルの再生成を伴わない **ViewHolder の部分更新**（`notifyItemChanged(position)` 相当、または該当 ViewHolder への直接反映）で処理しなければならず (MUST)、`submitList` による行差し替え（フルリバインド）を引き起こしてはならない (MUST NOT)。`DiffUtil` のバックグラウンド差分計算は追加・削除・移動のアニメーションにのみ用いる。

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.InsertCell(sectionId = sid, index = 0, cell = newCell))`
- **WHEN** `submitList` 後の DiffUtil 差分計算を観察する
- **THEN** 内部リストに 1 件だけ `CellListItem.CellRow` が挿入され、対応する Cell 行のみ挿入アニメーションが発生する

#### Scenario: Cell 削除時のアニメーション

- **GIVEN** `view.applyDiff(SettingsRootDiff.RemoveCell(cellId = cid))`
- **WHEN** `submitList` 後の差分計算を観察する
- **THEN** 該当 `CellListItem.CellRow` が削除され、その行のみ削除アニメーションが発生する

#### Scenario: 内容変化は areContentsTheSame で再描画されない

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）だけが異なる新旧 `CellListItem.CellRow`
- **WHEN** `DiffUtil` が `areItemsTheSame`（true）と `areContentsTheSame` を評価する
- **THEN** `areContentsTheSame` は（同一 id のため）`true` を返し、当該行のフルリバインド（`onBindViewHolder` による行全体の再生成・再 bind）は発生しない

#### Scenario: getItemId は内容に依存しない

- **GIVEN** 同一 id だが内容プロパティが異なる 2 つの Cell（順に submit される）
- **WHEN** `getItemId(position)` を評価する
- **THEN** 同一 id の Cell に対しては内容が変化しても同一の itemId を返す（内容依存の hashCode を用いない）。`RootHeaderFooterAdapter` の予約値 `1L` / `2L` とは衝突しない

#### Scenario: replaceCell は ViewHolder の部分更新で反映

- **GIVEN** `view.applyDiff(SettingsRootDiff.ReplaceCell(cellId = cid, newCell = updated))`（同一 id の内容更新）
- **WHEN** 適用後の描画を観察する
- **THEN** 該当 position の同一 ViewHolder が部分更新（再生成を伴わない bind 反映）され、内容が更新される。行の差し替えアニメーションやちらつきは発生しない

#### Scenario: Theme 更新

- **GIVEN** `view.applyDiff(SettingsRootDiff.UpdateTheme(newTheme))`
- **WHEN** 適用後の描画を観察する
- **THEN** すべての可視 Cell の bind が新 Theme で再呼び出しされる

#### Scenario: 存在しない cellId への操作（DEBUG）

- **GIVEN** 内部リストに存在しない `cellId` を持つ `RemoveCell` Diff
- **WHEN** DEBUG ビルドで `view.applyDiff(RemoveCell(cellId = notExistId))` を呼ぶ
- **THEN** `error(...)` などで即座にクラッシュする

#### Scenario: 存在しない cellId への操作（Release）

- **GIVEN** 内部リストに存在しない `cellId` を持つ `RemoveCell` Diff
- **WHEN** Release ビルドで `view.applyDiff(RemoveCell(cellId = notExistId))` を呼ぶ
- **THEN** クラッシュせず、`Log.w` でログ出力されるのみで内部リストは変更されない

## ADDED Requirements

### Requirement: visible projection の flatten 規約

`KsSettingsView` の `flatten` 経路（`SettingsRoot.sections` を `CellListItem` 平坦リストへ展開する処理）は、`Section.isVisible = false` の Section、および `VisibilityAware.isVisible = false` の Cell を平坦リストから除外しなければならない (MUST)。

具体的には：

- `Section.isVisible = false` の Section は、header / footer / 全 cells を `CellListItem` 平坦リストから除外しなければならない (MUST)。
- visible な Section 内の Cell について、`(cell as? VisibilityAware)?.isVisible == false` の Cell を `CellListItem.CellRow` から除外しなければならない (MUST)。
- `VisibilityAware` プロトコルに準拠していない Cell は、フィルタの判定で常に visible として扱わなければならない (MUST)。

一方、`internalRoot`（model）は hidden 含むフル状態として保持しなければならない (MUST)。`flatten` の結果（visible projection）と `internalRoot`（model）は明確に役割を分離して管理しなければならない (MUST)。

#### Scenario: hidden Section は flatten 結果から除外される

- **GIVEN** `internalRoot.sections` に `Section(id: "s1", isVisible: false, ...)` を含む
- **WHEN** `flatten(internalRoot.sections)` を呼ぶ
- **THEN** 結果の `List<CellListItem>` には s1 由来の `SectionHeader` / `CellRow` / `SectionFooter` がいずれも含まれない

#### Scenario: hidden Cell は flatten 結果から除外される

- **GIVEN** visible な Section の `cells` に `VisibilityAware.isVisible = false` の Cell を含む
- **WHEN** `flatten` を呼ぶ
- **THEN** 結果の `List<CellListItem>` から当該 Cell の `CellListItem.CellRow` が除外される

#### Scenario: VisibilityAware 非準拠 Cell は常に flatten 結果に含まれる

- **GIVEN** `VisibilityAware` に準拠しない外部 Cell が `cells` に含まれる
- **WHEN** `flatten` を呼ぶ
- **THEN** 当該 Cell の `CellListItem.CellRow` は除外されず、常に flatten 結果に含まれる

### Requirement: 部分 Diff の index 規約と hidden 対象の no-op（Android）

`KsSettingsView.applyDiff(_:)` は、`SettingsRootDiff` の部分 Diff ケース（`InsertSection` / `RemoveSection` / `MoveSection` / `ReplaceSection` / `InsertCell` / `RemoveCell` / `ReplaceCell` / `MoveCell` / `UpdateAccessory`）について、以下の規約に従わなければならない (MUST)。

**index 引数の解釈:**

部分 Diff の `index` / `at` / `to` 引数は、すべて **model 配列基準（hidden 含む）** で解釈しなければならない (MUST)。

**hidden 対象の挙動:**

- 対象 Section / Cell が hidden の場合、`internalRoot` の更新は実行しなければならない (MUST)。
- `flatten` 再計算経路によって visible projection は自動的に更新されるため、hidden 対象の Diff は `flatten` 結果に変化を生じない（自然な no-op となる）。
- `notifyItemChanged` 系の部分更新を hidden 対象に対して呼び出した場合でも、対応する ViewHolder が存在しないため自然に no-op となる。これは正常な動作として扱わなければならない (MUST)。

#### Scenario: hidden Cell への RemoveCell は flatten 結果に影響しない

- **GIVEN** `Section` に hidden Cell を含み、その Cell に対する `RemoveCell(cellId)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** `internalRoot` から当該 Cell が削除される一方、`flatten` 結果には元から含まれていなかったため `submitList` の前後で visible projection は変化しない

#### Scenario: hidden Section への UpdateAccessory は model のみ更新

- **GIVEN** hidden Section に対する `UpdateAccessory` Diff が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** `internalRoot` 上の当該 Section の `header` / `footer` は更新される。`flatten` 結果には元から含まれていないため visible projection は変化しない。後で当該 Section が `isVisible = true` に切り替わると、更新済みの accessory が描画される

### Requirement: ReplaceCell / ReplaceSection の可視性切替防御（Android）

DSL / アプリ層は、`SettingsRootDiff.ReplaceCell` / `SettingsRootDiff.ReplaceSection` で可視性（`isVisible`）だけを変える操作を行ってはならない (MUST NOT)。可視性変更は `SettingsRootDiff.Full(newRoot)` 経由で発行されなければならない (MUST)。

UI 層 (`KsSettingsView`) は、受け取った `ReplaceCell` で旧 Cell と新 Cell の `isVisible` が異なることを **`internalRoot` から取得した旧値で** 検出した場合、Full 経路（`setRootDirect(internalRoot, internalTheme)` 相当）にフォールバックしなければならない (MUST)。検出は visible projection 上の存在チェックよりも先に行わなければならず (MUST)、旧 Cell が hidden であっても model 上から取得した旧値で判定できなければならない (MUST)。

UI 層は、受け取った `ReplaceSection` を常に Full 経路（`setRootDirect(internalRoot, internalTheme)` 相当）で処理しなければならない (MUST)。`ReplaceSection` は型上 Section 全体置換であり、内部の任意の変化を内包し得るため、内部 cell の細粒度差分抽出を試みてはならない (MUST NOT)。

#### Scenario: ReplaceCell で visibility 切替が検出される（Android）

- **GIVEN** `internalRoot` 上の Cell `X` が `isVisible = true` で、新 Cell `X'`（同一 id、`isVisible = false`）を伴う `ReplaceCell(cellId: X, newCell: X')` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は可視性切替を検出し、Full 経路にフォールバックする。`internalRoot` 更新後に `setRootDirect` 相当が呼ばれ、visible projection が再構築される

#### Scenario: ReplaceSection は常に Full 経路で処理される（Android）

- **GIVEN** `ReplaceSection(sectionId:, newSection:)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は内部の cells / accessory / visibility の細粒度差分を抽出せず、Full 経路にフォールバックする
