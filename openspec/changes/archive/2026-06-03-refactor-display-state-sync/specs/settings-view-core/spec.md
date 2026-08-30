# settings-view-core 仕様変更（表示状態同期の二層分離）

## ADDED Requirements

### Requirement: 表示状態同期の二層分離

SettingsView の表示状態同期は、**(1) 構造同期** と **(2) 内容更新** の二層に分離されなければならない (SHALL)。各 UI 層実装（Android / iOS）はこの原則に従わなければならない (MUST)。

- **(1) 構造同期**: 差分検出（Android `DiffUtil` / iOS `UICollectionViewDiffableDataSource` snapshot）は、Cell / Section の **追加・削除・移動・差し替え（id の変化）** を検出する目的に限定されなければならない (MUST)。構造同期の同一性判定は **id（識別子）の同一性のみ** を用いなければならず (MUST)、Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）を判定に用いてはならない (MUST NOT)。
- **(2) 内容更新**: 同一 id を持つ Cell の内容（表示プロパティ）の変化は、セルを破棄・再生成せずに **同一セル（Android: ViewHolder、iOS: Cell）の部分更新（reconfigure）** で反映されなければならない (MUST)。内容変化を「セルの差し替え（フルリバインド／reload）」として扱ってはならない (MUST NOT)。

この分離により、内容変化（チェック ON/OFF・スイッチ・値更新等）が行全体の再生成（ちらつき）を引き起こさないことを保証する。移植元 `AiForms.Maui.SettingsView` の Android 実装（`GetItemId(position) => position`、`CellPropertyChanged → NotifyItemChanged`）と同一の責務分担である。MAUI の `Binding` に相当する内容更新の役割を、Native では ViewHolder / Cell が担う。

#### Scenario: 構造変化は id 同一性で検出

- **GIVEN** SettingsRoot に対し Cell の追加・削除・移動・id 変化を伴う差し替えが発生する
- **WHEN** 構造同期（diff / snapshot）が評価される
- **THEN** id の同一性のみで追加・削除・移動・差し替えが検出され、該当する構造操作（insert / delete / move）が行われる

#### Scenario: 内容変化はセルを再生成しない

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）が変化する
- **WHEN** 表示状態が更新される
- **THEN** 構造同期は「変化なし（同一 id）」と判定し、内容更新は同一セルの部分更新（reconfigure）として反映される。セル（ViewHolder / Cell）の破棄・再生成は発生しない

#### Scenario: チェック系の TwoWay 反映

- **GIVEN** チェック系 Cell（Switch / Checkbox / Radio / SimpleCheck）をユーザーが操作する
- **WHEN** セルがタップ等で操作される
- **THEN** セル（ViewHolder / Cell）が自身の表示状態を直接更新し、`onValueChanged` / `onSelected` 等でモデルへ通知する（TwoWay）。この内容更新は構造同期（diff / snapshot の再構築）を経由しない

## MODIFIED Requirements

### Requirement: Hashable / equals 契約

すべての値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、`Theme`、`CellStyle`、各具象 `Cell`、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは一般的な値比較・テスト・コレクション操作のための値型としての性質である。

ただし、差分検出（diff / snapshot の構造同期）は、この内容等価性（`equals` / `Hashable` の全フィールド比較）を **構造同期の同一性判定に用いてはならない** (MUST NOT)。構造同期は id（識別子）の同一性のみを用いなければならない (MUST)（「表示状態同期の二層分離」Requirement を参照）。値型の `equals` / `Hashable` は値比較やテストでは引き続き全フィールドを比較してよい。

また、装飾領域専用の型消去ラッパ `KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持たない (MUST NOT)。`SectionAccessory.view(KsAnyView)` および `RootAccessory.view(KsAnyView)` は手動実装で、`KsAnyView` の中身を判定対象外とし「ケース一致のみで等価」とみなさなければならない (MUST)。これに連動して `Section` の hash/equals も、`view` ケースの中身を判定対象から除外しなければならない (MUST)。

#### Scenario: 同一フィールドのインスタンスは等しい

- **GIVEN** 同じフィールド値を持つ 2 つの `Section` または `Cell` インスタンス（`SectionAccessory.view` の `KsAnyView` 中身は問わない）
- **WHEN** ハッシュ値および等価性を比較する
- **THEN** ハッシュ値が一致し、等価と判定される

#### Scenario: フィールド変更後は等しくない（値型としての性質）

- **GIVEN** `text` ケース内容や Cell の通常フィールドが 1 つだけ異なる 2 つのインスタンス
- **WHEN** 値型として等価性を比較する
- **THEN** 等価と判定されない（値型の性質として全フィールドを比較する）

#### Scenario: 差分検出は内容等価性を構造同期に使わない

- **GIVEN** 同一 id だが内容プロパティ（例: `isChecked` や `title`）が異なる 2 つの Cell
- **WHEN** 構造同期（diff / snapshot）の同一性判定が行われる
- **THEN** id が同一であるため「同一アイテム・構造変化なし」と判定される。値型としての `equals` が `false` を返すことを構造同期の判定（areContentsTheSame / snapshot 再構築）に用いてはならない

#### Scenario: KsAnyView の中身違いは等価とみなす

- **GIVEN** `SectionAccessory.view(KsAnyView A)` と `SectionAccessory.view(KsAnyView B)`（A ≠ B、ただしケースは同じ）
- **WHEN** 等価性を比較する
- **THEN** ケース一致のみで等価と判定される（中身は比較されない）

#### Scenario: KsAnyView は Hashable に参加しない

- **GIVEN** `KsAnyView` インスタンス
- **WHEN** Swift では `Hashable` 準拠を確認、Kotlin では `equals` / `hashCode` の独自実装を確認
- **THEN** `KsAnyView` は `Hashable` / `Equatable` 準拠を持たず、Kotlin では `Any` のデフォルト（参照同一性）以外の equals / hashCode を実装していない

#### Scenario: SettingsRootDiff の等価性

- **GIVEN** 同じケース・同じ payload を持つ 2 つの `SettingsRootDiff` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: AccessoryTarget の等価性

- **GIVEN** 同じケース・同じ `sectionID` を持つ 2 つの `AccessoryTarget` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

### Requirement: SettingsRootDiff 型

`settings-view-core` は、`SettingsRoot` に対する部分更新を表現する `SettingsRootDiff` 型を提供しなければならない (SHALL)。`SettingsRootDiff` は Swift では `enum`、Kotlin では `sealed interface` として定義され、以下のケースを持たなければならない (MUST)：

- `full(SettingsRoot)`: 全体差し替え
- `insertSection(at: Int, section: Section)`: Section 追加
- `removeSection(sectionID)`: Section 削除（Swift は `UUID`、Kotlin は `String`）
- `moveSection(from: Int, to: Int)`: Section 順序変更
- `replaceSection(sectionID, new: Section)`: Section 全体置換
- `insertCell(sectionID, at: Int, cell)`: Section 内 Cell 追加（`cell` は Swift `any KsCell`、Kotlin `Cell`）
- `removeCell(cellID)`: Cell 削除（Swift は `KsCellID`、Kotlin は `String`）
- `replaceCell(cellID, new: Cell)`: **同一 id の Cell の内容更新（reconfigure / 部分更新）**

`replaceCell(cellID, new:)` は「同一 id を持つ Cell の内容（表示プロパティ）を更新する」ことを意味し (MUST)、セルの破棄・再生成（フルリバインド / reload）を意味してはならない (MUST NOT)。UI 層は `replaceCell` を受けたとき、同一セル（Android: ViewHolder、iOS: Cell）の部分更新（reconfigure）で反映しなければならない (MUST)。id が変化する差し替えは `removeCell` + `insertCell`（または構造差分）で表現する。

#### Scenario: replaceCell は内容更新を表す

- **GIVEN** 同一 id の Cell の内容プロパティが変化し、`replaceCell(cellID, new:)` が発行される
- **WHEN** UI 層が当該 Diff を適用する
- **THEN** 同一セルが部分更新（reconfigure）で内容反映され、セルの破棄・再生成は発生しない

#### Scenario: SettingsRootDiff の各ケースが生成できる

- **GIVEN** `settings-view-core` モジュール
- **WHEN** `full` / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `replaceCell` の各ケースを生成する
- **THEN** いずれもコンパイル・生成でき、`Hashable` / `equals` 契約を満たす
