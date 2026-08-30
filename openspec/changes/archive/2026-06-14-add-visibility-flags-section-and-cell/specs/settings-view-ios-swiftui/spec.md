## MODIFIED Requirements

### Requirement: DSL → SettingsRootDiff 算出ロジック

`KsSettingsViewSwiftUI` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsViewController.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは以下の手順に従わなければならない (MUST)：

1. **可視性変化の preflight 検出**：
   - 旧ツリーと新ツリーの間で、同一 ID の Section について `isVisible` の値が変化している、または同一 Cell ID について `(cell as? VisibilityAware)?.isVisible ?? true` の値が変化していることを検出した場合、通常の section / cell 差分算出には進まず、`.full(newRoot)` Diff のみを発行して終了しなければならない (MUST)。
   - 可視性差分は通常の `.replaceCell`（reconfigure 経路）に乗せてはならない (MUST NOT)。可視性変化は構造同期上の追加・削除として表現される必要があり、reconfigure 経路では正しく扱えないため。
2. **Section レベルの突合**（可視性差分が無い場合に実施）：
   - 旧ツリーと新ツリーの Section ID 集合を比較
   - 新ツリーにあって旧ツリーにない Section ID → `.insertSection(at:, section:)` Diff を発行
   - 旧ツリーにあって新ツリーにない Section ID → `.removeSection(sectionID:)` Diff を発行
   - 両ツリーに存在し位置が異なる Section ID → `.moveSection(from:, to:)` Diff を発行
   - 両ツリーに存在し H/F（`SectionAccessory`）が異なる Section → `.updateAccessory(target: .sectionHeader/.sectionFooter, accessory:)` Diff を発行
3. **各 Section 内の Cell レベルの突合**：
   - 新セクションにあって旧セクションにない Cell ID → `.insertCell(sectionID:, at:, cell:)` Diff を発行
   - 旧セクションにあって新セクションにない Cell ID → `.removeCell(cellID:)` Diff を発行
   - 両セクションに存在し位置が異なる Cell ID → `.moveCell(cellID:, to:)` Diff を発行
   - 両セクションに存在し Cell 値が異なる Cell ID → `.replaceCell(cellID:, new:)` Diff を発行（**`replaceCell` は同一 id の内容更新を表し、`reconfigureItems` 経路で反映される。セルの破棄・再生成を意味しない**）
4. **Root H/F の突合**：
   - `.rootHeader(...)` / `.rootFooter(...)` modifier の値が変化した場合 → `.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff を発行
5. **Theme の突合**：
   - Theme は `SettingsRootDiff` には含まれない (MUST NOT)。Theme の変化は `.theme(_:)` modifier の再評価で `store.applyTheme(newTheme)` を呼ぶ経路で反映される（独立 API）
6. **Cell 値の比較対象**：
   - `KsAnyView` を含むフィールドは比較対象から除外（既存仕様、`Hashable` 非準拠）
   - その他のフィールドは `KsCell` の `Hashable`（`Equatable`）契約で比較し、**差があれば内容更新として `.replaceCell`（reconfigure 経路）を発行する**。`.replaceCell` は構造同期（snapshot の item 集合・順序）を変更せず、同一 id のセル内容の reconfigure として扱われる
   - 注: プラットフォーム間で内容更新の経路が異なる。iOS は DSL から `.replaceCell` を発行し `applyDiff` が `reconfigureItems` で反映する。Android（`settings-view-android-ui` の DSL → SettingsRootDiff 算出ロジック（Compose））は内容変化で `ReplaceCell` を発行せず、アダプタが ViewHolder を直接部分更新する。いずれも上位原則「構造同期は id 同一性のみ・内容更新はセルを再生成しない」に従う（経路の差は実装都合であり原則は共通）
7. **任意 View 形式（`.view(KsAnyView)`）の Section H/F / Root H/F の比較**：
   - `SectionAccessory.view` ケース同士・`RootAccessory.view` ケース同士は `KsAnyView` の中身を比較しない（既存仕様、`KsAnyView` は差分検出非参加）
   - 同ケース同士は等価とみなし `updateAccessory` Diff は **発行しない**
   - 異なるケース（`.text` → `.view` または `.view` → `.text`、`nil` → `.view` 等）の場合のみ `updateAccessory` Diff を発行

#### Scenario: Cell 内容変更時の Diff 発行（reconfigure 経路）

- **GIVEN** 旧ツリー `Section { LabelCell("Taro") }` と新ツリー `Section { LabelCell("Hanako") }`(Section ID・Cell ID は同じ)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.replaceCell(cellID: <same>, new: LabelCell("Hanako"))` のみが発行される。この Diff は構造同期（item 集合・順序）を変えず、`reconfigureItems` で同一セルの内容のみ更新される（セル破棄・再生成は伴わない）

#### Scenario: Cell 追加時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }` と新ツリー `Section { LabelCell("A"); LabelCell("B") }`(A の Cell ID は同じ、B は新規)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.insertCell(sectionID: <same>, at: 1, cell: LabelCell("B"))` のみが発行される

#### Scenario: Cell 削除時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B") }` と新ツリー `Section { LabelCell("A") }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.removeCell(cellID: <B のID>)` のみが発行される

#### Scenario: Section 追加時の Diff 発行

- **GIVEN** 旧ツリーが Section 1 つのみ、新ツリーが Section 2 つ（既存 + 末尾追加）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.insertSection(at: 1, section: <newSection>)` のみが発行される

#### Scenario: Section 削除時の Diff 発行

- **GIVEN** 旧ツリーが Section 2 つ（Section A + Section B、各々 Section ID は安定）、新ツリーが Section 1 つ（Section A のみ）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.removeSection(sectionID: <B のID>)` のみが発行される（Section A 内の Cell は完全保持）

#### Scenario: チェック系の内容変化はセルを再生成しない

- **GIVEN** 旧ツリー `Section { CheckboxCell("規約", isChecked: false) }` と新ツリー `Section { CheckboxCell("規約", isChecked: true) }`(同 Section ID・Cell ID)
- **WHEN** Diff 算出 → applyDiff を実行
- **THEN** `.replaceCell` が発行され `reconfigureItems` で同一セルの内容のみ更新される。セルの破棄・再生成（reload）や行全体のちらつきは発生しない

#### Scenario: Section H/F 変更時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader("旧")` と新ツリー `Section { LabelCell("A") }.sectionHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .sectionHeader(sectionID), accessory: .section(.text("新")))` が発行される

#### Scenario: Root H/F 変更時の Diff 発行

- **GIVEN** 旧 modifier `.rootHeader("旧")` と新 modifier `.rootHeader("新")`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.updateAccessory(target: .rootHeader, accessory: .root(.text("新")))` が発行される

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`(同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveCell(cellID: <B のID>, to: 2)` または `.moveCell(cellID: <C のID>, to: 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない（Cell 値は等価のため `replaceCell` は発行されない）

#### Scenario: Section 移動時の Diff 発行

- **GIVEN** 旧ツリーで Section 3 つが並んでいる状態と、新ツリーで Section の順序が変わった状態（各 Section ID は不変）
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveSection(from: <旧位置>, to: <新位置>)` Diff が発行され、Section 内の Cell は再構築されずに移動アニメーションが走る

#### Scenario: 任意 View 形式の Section H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardA() }` と新ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardB() }`(同 Section ID、Header が両方 `.view` ケース)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`.view` ケース同士は等価とみなされ `updateAccessory` Diff は発行されない。任意 View の中身更新は既存仕様通り `UIHostingConfiguration` の再構成に委ねられる

#### Scenario: 任意 View 形式の Root H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧 modifier `.rootHeader { HeaderA() }` と新 modifier `.rootHeader { HeaderB() }`(両方とも任意 View 指定)
- **WHEN** Diff 算出ロジックを実行
- **THEN** 同じ `.view` ケース同士は等価とみなされ、`updateAccessory(target: .rootHeader, ...)` Diff は発行されない

#### Scenario: Section H/F のケース変化（text → view）で updateAccessory 発行

- **GIVEN** 旧ツリー `Section { ... }.sectionHeader("文字列")` と新ツリー `Section { ... }.sectionHeader { CustomHeader() }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.text` ケースから `.view` ケースへの遷移は検出可能なため `.updateAccessory(target: .sectionHeader(...), accessory: .section(.view(...)))` が発行される

#### Scenario: 同一ツリーで Diff 空

- **GIVEN** 旧ツリーと新ツリーが完全に同一（Cell の Equatable 比較で全一致）
- **WHEN** Diff 算出ロジックを実行
- **THEN** 発行される Diff 列は空となり、`applyDiff` は呼ばれない（無駄な再描画を防止）

#### Scenario: 可視性変化のみで `.full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A", isVisible: true) }` と新ツリー `Section { LabelCell("A", isVisible: false) }`（同 Section ID、同 Cell ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される。`.replaceCell` は発行されない

#### Scenario: 可視性変化 + 内容変化で `.full` 発行

- **GIVEN** 旧ツリー `Section { LabelCell("旧", isVisible: true) }` と新ツリー `Section { LabelCell("新", isVisible: false) }`
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される。内容変化は `.full` に内包される（`.replaceCell` は発行されない）

#### Scenario: Section.isVisible 変化で `.full` 発行

- **GIVEN** 旧ツリー `Section("一般", isVisible: true) { ... }` と新ツリー `Section("一般", isVisible: false) { ... }`（同 Section ID、isVisible のみ変化）
- **WHEN** Diff 算出ロジックを実行
- **THEN** preflight で可視性変化が検出され、`.full(newRoot)` のみが発行される

## ADDED Requirements

### Requirement: SwiftUI DSL における isVisible 引数

`KsSettingsViewSwiftUI` の SwiftUI DSL は、Section ヘルパおよび本変更提案で扱う 7 種の Cell ヘルパに `isVisible: Bool = true` 引数を提供しなければならない (SHALL)。

- Section ヘルパ：`Section(_ header: String?, ..., isVisible: Bool = true) { ... }` の形で `isVisible` 引数を受け取り、生成される `Section` ドメインモデルの `isVisible` フィールドに反映する。
- 各 Cell ヘルパ：`LabelCell(..., isVisible: Bool = true)` の形で `isVisible` 引数を受け取り、生成される Cell モデルの `isVisible` フィールドに反映する。

既定値は `true` で、既存呼び出しは引数省略で互換維持される。

#### Scenario: Section に isVisible を指定できる

- **GIVEN** SwiftUI DSL で `Section("一般", isVisible: condition) { LabelCell(title: "通知") }` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `Section` ドメインモデルの `isVisible` が `condition` の値を反映する

#### Scenario: Cell に isVisible を指定できる

- **GIVEN** SwiftUI DSL で `LabelCell(title: "通知", isVisible: showAdvanced)` と書く
- **WHEN** Diff 算出ロジックがツリーを評価する
- **THEN** 生成される `LabelCell` モデルの `isVisible` が `showAdvanced` の値を反映する

#### Scenario: isVisible 未指定でも既存コードがビルドできる

- **GIVEN** 既存コード `LabelCell(title: "通知")`（`isVisible` 引数を指定しない）
- **WHEN** コンパイル・実行する
- **THEN** 既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない
