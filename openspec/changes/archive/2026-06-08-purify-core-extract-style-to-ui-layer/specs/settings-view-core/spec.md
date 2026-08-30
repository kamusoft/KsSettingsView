## MODIFIED Requirements

### Requirement: SettingsRoot ドメインモデル

`SettingsRoot` は設定画面全体の状態を表すルート型でなければならない (SHALL)。複数の `Section` を保持しなければならない (MUST)。Root ヘッダ / Root フッタは `SettingsRoot` のドメインモデルでは保持してはならず (MUST NOT)、UI 層（View）の責務として扱わなければならない (MUST)。**Theme は `SettingsRoot` に保持してはならず (MUST NOT)、UI 層が View 側引数または modifier として個別に受け取らなければならない (MUST)。** 値の等価性は `sections` の等価性のみで決定されなければならない (MUST)。

#### Scenario: SettingsRoot の構築

- **GIVEN** 任意個数の `Section` リスト
- **WHEN** `SettingsRoot` を構築する
- **THEN** `sections` を保持するイミュータブル値型として生成される。`theme` フィールドは存在しない

#### Scenario: SettingsRoot の等価性（同一 sections）

- **GIVEN** 同じ `sections` を持つ 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: SettingsRoot の等価性（異なる sections）

- **GIVEN** `sections` が異なる 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない

#### Scenario: Theme フィールド不在

- **GIVEN** `SettingsRoot` の型定義
- **WHEN** プロパティ一覧を確認する
- **THEN** `theme` プロパティは存在しない。Theme は UI 層が View 側で別経路（iOS: `.theme(_:)` modifier、Android: `KsSettingsView(theme = ...)` 引数）から受け取る

### Requirement: Cell 抽象

`Cell` 抽象は全 Cell 種類が満たすべき共通契約でなければならない (SHALL)。一意な `id`（iOS は `UUID`、Android は `String`）を保持しなければならない (MUST)。`Cell` は外部モジュール（Sample アプリや利用側アプリ）からも実装可能でなければならない (MUST)。Kotlin においては `sealed` 制約を持ってはならない (MUST NOT)。**`CellStyle` プロパティの保持は本抽象では要求しない (MUST NOT 要求)。各具象 Cell が個別に `style` プロパティを持つかどうかは UI 層の各実装に委ねられる。**

#### Scenario: Swift プロトコル定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `KsCell` プロトコルを参照する
- **THEN** `Hashable` および `Identifiable` を継承し、`var id: UUID { get }` のみを要求する。`var style: CellStyle { get }` は要求されない

#### Scenario: Kotlin インターフェース定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `Cell` を参照する
- **THEN** 通常の `interface Cell`（`sealed` ではない）であり、`val id: String` のみを抽象プロパティとして持つ。`val style: CellStyle` は抽象プロパティとして要求されない。外部 Gradle モジュール（Sample アプリ等）からも `Cell` を実装する具象 Cell 型を新規定義できる

### Requirement: Hashable / equals 契約

すべての構造ドメイン値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、各具象 `Cell`、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは一般的な値比較・テスト・コレクション操作のための値型としての性質である。**Core から削除された `Theme` / `CellStyle` は本契約の対象外となる (MUST NOT)。**

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

#### Scenario: Theme / CellStyle は Core の Hashable 契約対象外

- **GIVEN** `KsSettingsViewCore` / `ks-settingsview-core` の公開型一覧
- **WHEN** 型を確認する
- **THEN** `Theme` / `CellStyle` 型は Core 内に存在しないため、Core 仕様の Hashable 契約の対象外である。`Theme` / `CellStyle` の等価性契約は `settings-view-{ios,android}-style` 仕様で規定される

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
- `moveCell(cellID, to: Int)`: Cell 順序変更（Section 内のみ、Section 間移動は別途 `removeCell` + `insertCell` で表現）
- `updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)`: Root H/F / Section H/F の追加・更新・削除（`nil` は削除を意味する）

**`updateTheme(Theme)` ケースは含まれてはならない (MUST NOT)**。Theme 更新は構造差分の責務ではなく、UI 層独立 API (`SettingsRootStore.applyTheme(_:)` 相当) の責務とする。

`replaceCell(cellID, new:)` は「同一 id を持つ Cell の内容（表示プロパティ）を更新する」ことを意味し (MUST)、セルの破棄・再生成（フルリバインド / reload）を意味してはならない (MUST NOT)。UI 層は `replaceCell` を受けたとき、同一セル（Android: ViewHolder、iOS: Cell）の部分更新（reconfigure）で反映しなければならない (MUST)。id が変化する差し替えは `removeCell` + `insertCell`（または構造差分）で表現する。

`SettingsRootDiff` は `Hashable` / `equals` / `hashCode` 契約を満たさなければならない (MUST)。Swift では `Hashable` プロトコルへ準拠する（ただし `insertCell` / `replaceCell` など `any KsCell` を含むケースは existential type の制約により手動 `Hashable` 実装が必要であり、内部で `AnyHashable` 経由などで Cell の hash を取り込む）。Kotlin では各ケースを `data class` として定義し `equals` / `hashCode` を自動取得する。

#### Scenario: replaceCell は内容更新を表す

- **GIVEN** 同一 id の Cell の内容プロパティが変化し、`replaceCell(cellID, new:)` が発行される
- **WHEN** UI 層が当該 Diff を適用する
- **THEN** 同一セルが部分更新（reconfigure）で内容反映され、セルの破棄・再生成は発生しない

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `SettingsRootDiff` を参照する
- **THEN** `public enum SettingsRootDiff: Hashable` であり、上記 10 ケースを持つ（`updateTheme` は含まれない）

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `SettingsRootDiff` を参照する
- **THEN** `sealed interface SettingsRootDiff` であり、各ケースは `data class` または `object` として定義される。`UpdateTheme` ケースは含まれない

#### Scenario: insertSection の生成

- **GIVEN** Section インスタンスと挿入位置 `index = 2`
- **WHEN** `SettingsRootDiff.insertSection(at: 2, section: ...)` を構築する
- **THEN** Diff インスタンスから `index` と `section` を取り出せる

#### Scenario: updateAccessory による Root H/F 更新

- **GIVEN** `target = AccessoryTarget.rootHeader` と `accessory = SettingsAccessory.root(.text("プロフィール"))`
- **WHEN** `SettingsRootDiff.updateAccessory(target:, accessory:)` を構築する
- **THEN** Diff インスタンスから `target` と `accessory` を取り出せる

#### Scenario: updateAccessory による Section H/F 削除

- **GIVEN** `target = AccessoryTarget.sectionHeader(sectionID: someID)` と `accessory = nil`
- **WHEN** `SettingsRootDiff.updateAccessory(target:, accessory: nil)` を構築する
- **THEN** Diff インスタンスから「該当 Section H/F の削除」を表現できる

#### Scenario: moveCell の生成（Section 内移動）

- **GIVEN** `cellID` と `toIndex = 5`
- **WHEN** `SettingsRootDiff.moveCell(cellID:, to: 5)` を構築する
- **THEN** Diff インスタンスから `cellID` と `to` を取り出せる

#### Scenario: updateTheme ケース不在

- **GIVEN** `SettingsRootDiff` のケース一覧
- **WHEN** 型定義を確認する
- **THEN** `updateTheme(Theme)` ケースは存在しない。Theme 更新は UI 層独立 API（例: `SettingsRootStore.applyTheme(_:)`）が責務を持つ

## REMOVED Requirements

### Requirement: Theme 型

**Reason**: Theme はスタイル系の値であり、UI 層 (`settings-view-{ios,android}-style` capability) に再配置する。Core は構造的ドメインモデル（SettingsRoot / Section / Cell 抽象 / Diff）のみを担う責務に純化する。

**Migration**: `Theme` は `settings-view-ios-style` / `settings-view-android-style` capability に新規 Requirement として再定義する。フィールド型は iOS では `UIColor` / `UIFont`、Android では `androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle` を直接保持する。利用者は `SettingsRoot(theme: ...)` を `KsSettingsView { ... }.theme(_:)`（iOS）または `KsSettingsView(theme = ...)`（Android）に書き換える。

### Requirement: CellStyle 型

**Reason**: `CellStyle` は Theme と同様にスタイル系の値であり、UI 層に再配置する。

**Migration**: `CellStyle` は `settings-view-ios-style` / `settings-view-android-style` capability に新規 Requirement として再定義する。フィールド型は Native 型を直接保持する。各具象 Cell が UI 層で `style: CellStyle` プロパティを個別に持つ形に変更する。Core の `KsCell` / `Cell` 抽象からは `style` プロパティ要求が削除される。

## ADDED Requirements

### Requirement: スタイル系型の Core 不在

`KsSettingsViewCore` / `ks-settingsview-core` は、`KsColor`、`KsFont`、`KsFontWeight`、`KsImage`、`Theme`、`CellStyle` を **公開してはならない (MUST NOT)**。これらはすべて UI 層（`settings-view-{ios,android}-style` capability）に所属する。

#### Scenario: 公開型一覧の確認（iOS）

- **GIVEN** `KsSettingsViewCore` モジュールをインポート
- **WHEN** 公開型一覧を参照する
- **THEN** `KsColor` / `KsFont` / `KsFontWeight` / `KsImage` / `Theme` / `CellStyle` のいずれも存在しない。`import KsSettingsViewCore` のみではこれらの型を解決できない

#### Scenario: 公開型一覧の確認（Android）

- **GIVEN** `ks-settingsview-core` モジュールへの依存
- **WHEN** `jp.kamusoft.kssettingsview.core` パッケージの公開型を参照する
- **THEN** `KsColor` / `KsFont` / `KsFontWeight` / `KsImage` / `Theme` / `CellStyle` のいずれも存在しない

#### Scenario: Theme / CellStyle / KsImage の参照経路

- **GIVEN** UI 層実装または利用者コード
- **WHEN** `Theme` / `CellStyle` / `KsImage` を参照したい
- **THEN** `KsSettingsViewUI`（iOS）または `ks-settingsview-ui`（Android）からインポートする
