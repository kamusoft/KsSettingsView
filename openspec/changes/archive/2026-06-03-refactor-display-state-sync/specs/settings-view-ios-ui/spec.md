# settings-view-ios-ui 仕様変更（内容更新は reconfigureItems・構造同期は id のみ）

## MODIFIED Requirements

### Requirement: DiffableDataSource

`KsSettingsViewController` は内部で `UICollectionViewDiffableDataSource<UUID, KsCellID>` を保持しなければならない (SHALL)（Section 識別子は `UUID`、Item 識別子は `KsCellID` で `Hashable`）。スナップショットの**構造同期（Section / Item の追加・削除・移動・差し替え）は `KsCellID`（id）の同一性のみで算出されなければならない** (MUST)。Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）の `Hashable` 等価性を構造同期（snapshot の item 集合・順序の再構築）の判定に用いてはならない (MUST NOT)（「表示状態同期の二層分離」参照）。装飾領域（Section H/F、Root H/F）の `KsAnyView` は差分検出に参加せず、`SettingsRoot` / `Section` 等の `Hashable` 実装は `view` ケースの中身を判定対象外として扱わなければならない (MUST)。

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

#### Scenario: Theme 更新

- **GIVEN** `controller.applyDiff(.updateTheme(newTheme))`
- **WHEN** 適用後の描画を観察する
- **THEN** すべての可視 Cell の `KsCellRenderer.render(cell:theme:)` が新 Theme で再呼び出しされる（`reconfigureItems` ベースでセルを破棄せず再構成する）

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

### Requirement: DSL → SettingsRootDiff 算出ロジック

`KsSettingsViewSwiftUI` モジュールは、DSL で構築された旧宣言ツリーと新宣言ツリーを比較し、`SettingsRootDiff` 列を算出する内部ロジックを提供しなければならない (SHALL)。算出された Diff 列は内部 `SettingsRootStore` の Diff 経路に流され、最終的に `KsSettingsViewController.applyDiff(_:)` が呼ばれる。

算出アルゴリズムは以下の手順に従わなければならない (MUST)：

1. **Section レベルの突合**：
   - 旧ツリーと新ツリーの Section ID 集合を比較
   - 新ツリーにあって旧ツリーにない Section ID → `.insertSection(at:, section:)` Diff を発行
   - 旧ツリーにあって新ツリーにない Section ID → `.removeSection(sectionID:)` Diff を発行
   - 両ツリーに存在し位置が異なる Section ID → `.moveSection(from:, to:)` Diff を発行
   - 両ツリーに存在し H/F（`SectionAccessory`）が異なる Section → `.updateAccessory(target: .sectionHeader/.sectionFooter, accessory:)` Diff を発行
2. **各 Section 内の Cell レベルの突合**：
   - 新セクションにあって旧セクションにない Cell ID → `.insertCell(sectionID:, at:, cell:)` Diff を発行
   - 旧セクションにあって新セクションにない Cell ID → `.removeCell(cellID:)` Diff を発行
   - 両セクションに存在し位置が異なる Cell ID → `.moveCell(cellID:, to:)` Diff を発行
   - 両セクションに存在し Cell 値が異なる Cell ID → `.replaceCell(cellID:, new:)` Diff を発行（**`replaceCell` は同一 id の内容更新を表し、`reconfigureItems` 経路で反映される。セルの破棄・再生成を意味しない**）
3. **Root H/F の突合**：
   - `.rootHeader(...)` / `.rootFooter(...)` modifier の値が変化した場合 → `.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff を発行
4. **Theme の突合**：
   - 旧 Theme と新 Theme が異なる場合 → `.updateTheme(newTheme)` Diff を発行
5. **Cell 値の比較対象**：
   - `KsAnyView` を含むフィールドは比較対象から除外（既存仕様、`Hashable` 非準拠）
   - その他のフィールドは `KsCell` の `Hashable`（`Equatable`）契約で比較し、**差があれば内容更新として `.replaceCell`（reconfigure 経路）を発行する**。`.replaceCell` は構造同期（snapshot の item 集合・順序）を変更せず、同一 id のセル内容の reconfigure として扱われる
   - 注: プラットフォーム間で内容更新の経路が異なる。iOS は DSL から `.replaceCell` を発行し `applyDiff` が `reconfigureItems` で反映する。Android（`settings-view-android-ui` の DSL → SettingsRootDiff 算出ロジック（Compose））は内容変化で `ReplaceCell` を発行せず、アダプタが ViewHolder を直接部分更新する。いずれも上位原則「構造同期は id 同一性のみ・内容更新はセルを再生成しない」に従う（経路の差は実装都合であり原則は共通）
6. **任意 View 形式（`.view(KsAnyView)`）の Section H/F / Root H/F の比較**：
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

#### Scenario: Cell 移動時の Diff 発行

- **GIVEN** 旧ツリー `Section { LabelCell("A"); LabelCell("B"); LabelCell("C") }` と新ツリー `Section { LabelCell("A"); LabelCell("C"); LabelCell("B") }`(同 Section ID、B と C の Cell ID は同じ、位置のみ入れ替わり)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `.moveCell(cellID: <B のID>, to: 2)` または `.moveCell(cellID: <C のID>, to: 1)` のいずれか（実装定義）が発行され、内容の変化は伴わない

#### Scenario: 任意 View 形式の Section H/F が変化しても updateAccessory 非発行

- **GIVEN** 旧ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardA() }` と新ツリー `Section { LabelCell("A") }.sectionHeader { ProfileCardB() }`(同 Section ID、Header が両方 `.view` ケース)
- **WHEN** Diff 算出ロジックを実行
- **THEN** `KsAnyView` は差分検出に参加しないため、`.view` ケース同士は等価とみなされ `updateAccessory` Diff は発行されない。任意 View の中身更新は既存仕様通り `UIHostingConfiguration` の再構成に委ねられる
