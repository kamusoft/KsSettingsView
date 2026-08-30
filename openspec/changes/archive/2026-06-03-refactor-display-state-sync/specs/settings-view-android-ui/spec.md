# settings-view-android-ui 仕様変更（構造同期は id のみ・内容更新はViewHolder部分更新）

## MODIFIED Requirements

### Requirement: DiffUtil 差分検出

`DiffUtil.ItemCallback<CellListItem>` を実装し、`areItemsTheSame` は ID 比較でなければならない (MUST)。`areContentsTheSame` は **同一 id（`areItemsTheSame` が true）であれば常に `true` を返さなければならない** (MUST)。すなわち `areContentsTheSame` は Cell の内容（`data class equals` の全フィールド比較）を判定に用いてはならない (MUST NOT)（「表示状態同期の二層分離」: 構造同期は id 同一性のみ）。`SectionAccessory.View(KsAnyView)` の扱い（差分検出非参加）は従来どおりとする。

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

### Requirement: CellViewHolder 抽象

`CellViewHolder<T : Cell>` は `RecyclerView.ViewHolder` を継承する抽象クラスでなければならない (SHALL)。`abstract fun bind(cell: T, theme: Theme)` および `open fun reset()` を持たなければならない (MUST)。`CellViewHolder` は外部モジュール（Sample アプリや利用側アプリ）から派生可能な可視性（Kotlin の `public`）を持たなければならない (MUST)。

`CellViewHolder` は同一 id の Cell の内容更新を、セルを再生成せずに反映できなければならない (MUST)。具体的には、内容変化時に同一 ViewHolder に対して最新 Cell を反映する**部分更新**が可能であること（`notifyItemChanged` による再 bind、または ViewHolder が保持する View への直接反映）。チェック系 Cell（Switch / Checkbox / Radio / SimpleCheck）の ViewHolder は、ユーザー操作時に **View 自身を直接トグル（TwoWay）** し `onValueChanged` / `onSelected` でモデルへ書き戻さなければならない (MUST)。この TwoWay の内容更新は `submitList` / `DiffUtil` の再構築を経由してはならない (MUST NOT)。RadioCell のグループ連動（同一 `groupId` の他セルの選択解除）は、該当セル（旧選択・新選択）の部分更新で反映しなければならず (MUST)、グループ全体の再生成（`ReplaceCell`）を用いてはならない (MUST NOT)。

#### Scenario: bind の呼び出し

- **GIVEN** `CellViewHolder` 派生クラス
- **WHEN** ListAdapter が `onBindViewHolder` する
- **THEN** `bind(cell, theme)` が呼ばれ、Cell の内容と Theme が View に反映される

#### Scenario: reset によるクリア

- **GIVEN** `CellViewHolder` が一度 bind された後 RecyclerView から detach
- **WHEN** `onViewRecycled` が呼ばれる
- **THEN** ViewHolder 内の画像・テキスト参照、保持していた `Job`/`Disposable` がクリアされる

#### Scenario: チェック系の TwoWay トグル

- **GIVEN** Checkbox / Switch / SimpleCheck の ViewHolder が表示されている
- **WHEN** ユーザーがセルをタップする
- **THEN** ViewHolder が自身の View（CheckBox / Switch / KsSimpleCheckView）を直接トグルし、`onValueChanged(newValue)` を発火する。`submitList` / `DiffUtil` の再構築を経由せず、行全体の再描画（ちらつき）は発生しない

#### Scenario: RadioCell のグループ連動

- **GIVEN** 同一 `groupId` の RadioCell 群が表示され、ある値が選択されている
- **WHEN** ユーザーが別の RadioCell をタップする
- **THEN** タップされたセルが選択状態（チェック表示）になり `onSelected(value)` を発火し、旧選択セルの選択解除は該当セルの部分更新で反映される。グループ全体の再生成は発生しない

### Requirement: DSL → SettingsRootDiff 算出ロジック（Compose）

`ks-settingsview-compose` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsView.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは iOS 側（`settings-view-ios-ui` capability の同名 Requirement）と完全に同等でなければならない (MUST)：

1. **Section レベルの突合**：Section ID 集合の比較で `InsertSection` / `RemoveSection` / `MoveSection` / `UpdateAccessory`（Section H/F 用）を発行
2. **各 Section 内の Cell レベルの突合**：Cell ID 集合の比較で `InsertCell` / `RemoveCell` / `MoveCell` を発行する（構造変化＝追加・削除・移動・id 変化のみ）。**両セクションに同一 Cell ID が存在し内容（プロパティ）だけが異なる場合、`ReplaceCell` を構造同期の差分として発行してはならない** (MUST NOT)（「表示状態同期の二層分離」: 構造同期は id 同一性のみ）。同一 id の内容更新は ViewHolder の部分更新経路（`DiffUtil 差分検出` Requirement / `CellViewHolder 抽象` Requirement 参照）で反映する
3. **Root H/F の突合**：`rootHeader` / `rootFooter` パラメータの値が変化した場合 `UpdateAccessory`（Root H/F 用）を発行
4. **Theme の突合**：Theme が変化した場合 `UpdateTheme` を発行
5. **構造同期の同一性判定対象**：Section / Cell の **id 同一性のみ** で追加・削除・移動を判定する。Cell の内容プロパティ（`data class equals` の全フィールド比較）を構造同期の判定に用いてはならない (MUST NOT)。`KsAnyView` を含むフィールドは従来どおり比較対象から除外
6. **任意 View 形式（`SectionAccessory.View(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.View` ケース同士・`RootAccessory.View` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `UpdateAccessory` Diff は **発行しない**
   - 異なるケース（`Text` → `View` または `View` → `Text`、`null` → `View` 等）の場合のみ `UpdateAccessory` Diff を発行

#### Scenario: Cell 内容変更時は ReplaceCell を発行しない

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`(Section ID・Cell ID は同じ、内容のみ変化)
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff（`InsertCell` / `RemoveCell` / `MoveCell` / `ReplaceCell`）は発行されない。内容更新は ViewHolder の部分更新経路で反映される

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertCell(sectionId = <same>, index = 1, cell = LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveCell(cellId = <B の ID>)` のみが発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`(同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveCell(cellId = <B の ID>, toIndex = 2)` または `SettingsRootDiff.MoveCell(cellId = <C の ID>, toIndex = 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない

#### Scenario: Section 追加時の Diff 発行

- **GIVEN** 旧ツリーが Section 1 つのみ、新ツリーが Section 2 つ（既存 + 末尾追加）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.InsertSection(index = 1, section = <newSection>)` のみが発行される

#### Scenario: Section 削除時の Diff 発行

- **GIVEN** 旧ツリーが Section 2 つ（Section A + Section B、各々 Section ID は安定）、新ツリーが Section 1 つ（Section A のみ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.RemoveSection(sectionId = <B の ID>)` のみが発行される（Section A 内の Cell は完全保持）

#### Scenario: Section 移動時の Diff 発行

- **GIVEN** 旧ツリーで Section 3 つが並んでいる状態と、新ツリーで Section の順序が変わった状態（各 Section ID は不変）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.MoveSection(fromIndex = <旧位置>, toIndex = <新位置>)` Diff が発行され、Section 内の Cell は再構築されずに移動アニメーションが走る

#### Scenario: 任意 Composable 形式の Section H/F が変化しても UpdateAccessory 非発行

- **GIVEN** 旧ツリー `Section(headerContent = { CardA() }) { ... }` と新ツリー `Section(headerContent = { CardB() }) { ... }`(同 Section ID、Header が両方 `View` ケース)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`View` ケース同士は等価とみなされ `UpdateAccessory` Diff は発行されない。任意 Composable の中身更新は既存仕様通り `ComposeView.setContent` の再実行に委ねられる

#### Scenario: 任意 Composable 形式の Root H/F が変化しても UpdateAccessory 非発行

- **GIVEN** 旧 `KsSettingsView(rootHeader = { HeaderA() }) { ... }` と新 `KsSettingsView(rootHeader = { HeaderB() }) { ... }`(両方とも任意 Composable 指定)
- **WHEN** Recomposition で Diff 算出ロジックを実行
- **THEN** 同じ `View` ケース同士は等価とみなされ、`UpdateAccessory(target = RootHeader, ...)` Diff は発行されない

#### Scenario: Section H/F のケース変化（Text → View）で UpdateAccessory 発行

- **GIVEN** 旧ツリー `Section(header = "文字列") { ... }` と新ツリー `Section(headerContent = { CustomHeader() }) { ... }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `Text` ケースから `View` ケースへの遷移は検出可能なため `SettingsRootDiff.UpdateAccessory(target = SectionHeader(sectionId), accessory = SettingsAccessory.Section(SectionAccessory.View(...)))` が発行される

#### Scenario: Section H/F 変更時の Diff 発行

- **GIVEN** 旧ツリー `Section(header = "旧") { ... }` と新ツリー `Section(header = "新") { ... }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `SettingsRootDiff.UpdateAccessory(target = SectionHeader(sectionId), accessory = SettingsAccessory.Section(SectionAccessory.Text("新")))` が発行される

#### Scenario: Root H/F 変更時の Diff 発行（null → 任意 Composable のケース変化）

- **GIVEN** 旧 `KsSettingsView(rootHeader = null) { ... }` と新 `KsSettingsView(rootHeader = { Text("新") }) { ... }`(`null` から `View` ケースへの遷移)
- **WHEN** Recomposition で Diff 算出ロジックを実行
- **THEN** ケース変化（`null` → `View`）が検出され、`SettingsRootDiff.UpdateAccessory(target = RootHeader, accessory = SettingsAccessory.Root(RootAccessory.View(...)))` が発行される

#### Scenario: 同一 id の構造で構造 Diff 空

- **GIVEN** 旧ツリーと新ツリーで Section / Cell の id 集合・順序が完全に同一（内容プロパティの異同は問わない）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff 列（Insert/Remove/Move/Replace 系）は空となる。内容プロパティが変化していても構造同期は発火せず、内容更新は ViewHolder の部分更新経路で反映される
