## MODIFIED Requirements

### Requirement: DSL → SettingsRootDiff 算出ロジック（Compose）

`ks-settingsview-compose` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsView.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは iOS 側（`settings-view-ios-ui` capability の同名 Requirement）と完全に同等でなければならない (MUST)：

1. **可視性変化の preflight 検出**：
   - 旧ツリーと新ツリーの間で、同一 ID の Section について `isVisible` の値が変化している、または同一 Cell ID について `(cell as? VisibilityAware)?.isVisible ?: true` の値が変化していることを検出した場合、通常の section / cell 差分算出には進まず、`SettingsRootDiff.Full(newRoot)` のみを発行して終了しなければならない (MUST)。
   - 可視性差分を内容更新経路（ViewHolder の部分更新経路 / `contentUpdates`）に流してはならない (MUST NOT)。同条件下では `contentUpdates` は空リストを返さなければならない (MUST)。可視性変化は構造同期上の追加・削除として表現される必要があり、内容更新経路では正しく扱えないため。
2. **Section レベルの突合**（可視性差分が無い場合に実施）：Section ID 集合の比較で `InsertSection` / `RemoveSection` / `MoveSection` / `UpdateAccessory`（Section H/F 用）を発行
3. **各 Section 内の Cell レベルの突合**：Cell ID 集合の比較で `InsertCell` / `RemoveCell` / `MoveCell` を発行する（構造変化＝追加・削除・移動・id 変化のみ）。**両セクションに同一 Cell ID が存在し内容（プロパティ）だけが異なる場合、`ReplaceCell` を構造同期の差分として発行してはならない** (MUST NOT)（「表示状態同期の三層分離」: 構造同期は id 同一性のみ）。同一 id の内容更新は ViewHolder の部分更新経路（`DiffUtil 差分検出` Requirement / `CellViewHolder 抽象` Requirement 参照）で反映する
4. **Root H/F の突合**：`rootHeader` / `rootFooter` パラメータの値が変化した場合 `UpdateAccessory`（Root H/F 用）を発行
5. **Theme の突合**：Theme は `SettingsRootDiff` には含まれない (MUST NOT)。Theme の変化は `KsSettingsView(theme = ...)` パラメータの再評価で `store.applyTheme(newTheme)` を呼ぶ経路で反映される（独立 API）
6. **構造同期の同一性判定対象**：Section / Cell の **id 同一性のみ** で追加・削除・移動を判定する。Cell の内容プロパティを構造同期の判定に用いてはならない (MUST NOT)
7. **任意 View 形式（`SectionAccessory.View(KsAnyView)`）の Section H/F / Root H/F の比較**：
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

注: 同じ `View` ケース同士の Composable 中身変化は `UpdateAccessory` 非発行となる（直前の Scenario 参照）。Compose の `rootHeader` は `(@Composable () -> Unit)?` 型のため、ケース変化として観測可能なのは `null` ↔ 非 `null` の遷移のみとなる。

#### Scenario: 同一 id の構造で構造 Diff 空

- **GIVEN** 旧ツリーと新ツリーで Section / Cell の id 集合・順序が完全に同一（内容プロパティの異同は問わない）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 構造同期の Diff 列（Insert/Remove/Move/Replace 系）は空となる。内容プロパティが変化していても構造同期は発火せず、内容更新は ViewHolder の部分更新経路で反映される

#### Scenario: Theme 変化時の Diff 不発行

- **GIVEN** 旧 `KsSettingsView(theme = themeA) { ... }` と新 `KsSettingsView(theme = themeB) { ... }`（root 内容は不変）
- **WHEN** Recomposition が起こる
- **THEN** `SettingsRootDiff` は何も発行されない。代わりに `store.applyTheme(themeB)` が呼ばれて `KsSettingsView.theme` プロパティが更新される

#### Scenario: 可視性変化のみで `Full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A", isVisible = true) }` と新ツリー `Section { LabelCell("A", isVisible = false) }`（同 Section ID、同 Cell ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される。`contentUpdates` は空リストを返す

#### Scenario: 可視性変化 + 内容変化で `Full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("旧", isVisible = true) }` と新ツリー `Section { LabelCell("新", isVisible = false) }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される。`contentUpdates` は空リストを返し、内容変化は `Full` に内包される

#### Scenario: Section.isVisible 変化で `Full` 発行

- **GIVEN** 旧ツリー `Section("一般", isVisible = true) { ... }` と新ツリー `Section("一般", isVisible = false) { ... }`（同 Section ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`SettingsRootDiff.Full(newRoot)` のみが発行される

## ADDED Requirements

### Requirement: Compose DSL における isVisible 引数

`ks-settingsview-compose` の Compose DSL は、Section ヘルパおよび本変更提案で扱う 7 種の Cell ヘルパに `isVisible: Boolean = true` 引数を提供しなければならない (SHALL)。

- Section ヘルパ：`Section(header: String?, ..., isVisible: Boolean = true) { ... }` の形で `isVisible` 引数を受け取り、生成される `Section` ドメインモデルの `isVisible` フィールドに反映する。
- 各 Cell ヘルパ：`LabelCell(..., isVisible: Boolean = true)` の形で `isVisible` 引数を受け取り、生成される Cell モデルの `isVisible` フィールドに反映する。

既定値は `true` で、既存呼び出しは引数省略で互換維持される。

#### Scenario: Section に isVisible を指定できる

- **GIVEN** Compose DSL で `Section(header = "一般", isVisible = condition) { LabelCell(title = "通知") }` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `Section` ドメインモデルの `isVisible` が `condition` の値を反映する

#### Scenario: Cell に isVisible を指定できる

- **GIVEN** Compose DSL で `LabelCell(title = "通知", isVisible = showAdvanced)` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `LabelCell` モデルの `isVisible` が `showAdvanced` の値を反映する

#### Scenario: isVisible 未指定でも既存コードがビルドできる

- **GIVEN** 既存コード `LabelCell(title = "通知")`（`isVisible` 引数を指定しない）
- **WHEN** コンパイル・実行する
- **THEN** 既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない
