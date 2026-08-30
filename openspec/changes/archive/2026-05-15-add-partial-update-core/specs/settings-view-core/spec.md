## MODIFIED Requirements

### Requirement: SettingsRoot ドメインモデル

`SettingsRoot` は設定画面全体の状態を表すルート型でなければならない (SHALL)。複数の `Section` と全体に適用される `Theme` を保持しなければならない (MUST)。Root ヘッダ / Root フッタは `SettingsRoot` のドメインモデルでは保持してはならず (MUST NOT)、UI 層（View）の責務として扱わなければならない (MUST)。値の等価性は `sections` と `theme` の等価性のみで決定されなければならない (MUST)。

<!-- 注: 旧 Scenario「SettingsRoot の構築（Root H/F なし）」「SettingsRoot の構築（Root H/F あり、text）」「SettingsRoot の構築（Root H/F あり、view）」「等価性（H/F なし同士）」「等価性（text 同士）」「等価性（view 同士、中身は無視）」「等価性（nil と非 nil）」は、SettingsRoot.header/footer 削除に伴い MODIFIED 内で削除している。新規 Scenario「SettingsRoot の構築」「SettingsRoot の等価性（同一 sections / theme）」「SettingsRoot の等価性（異なる sections）」が代替となる。 -->

#### Scenario: SettingsRoot の構築

- **GIVEN** 任意個数の `Section` リストと任意の `Theme`
- **WHEN** `SettingsRoot` を構築する
- **THEN** `sections` と `theme` を保持するイミュータブル値型として生成される

#### Scenario: SettingsRoot の等価性（同一 sections / theme）

- **GIVEN** 同じ `sections` と `theme` を持つ 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: SettingsRoot の等価性（異なる sections）

- **GIVEN** `theme` は同一だが `sections` が異なる 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない

### Requirement: Hashable / equals 契約

すべての値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、`Theme`、`CellStyle`、各具象 `Cell`、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは UICollectionViewDiffableDataSource および DiffUtil による差分検出の前提条件である。

ただし、装飾領域専用の型消去ラッパ `KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持たない (MUST NOT)。`SectionAccessory.view(KsAnyView)` および `RootAccessory.view(KsAnyView)` は手動実装で、`KsAnyView` の中身を判定対象外とし「ケース一致のみで等価」とみなさなければならない (MUST)。これに連動して `Section` の hash/equals も、`view` ケースの中身を判定対象から除外しなければならない (MUST)。

#### Scenario: 同一フィールドのインスタンスは等しい

- **GIVEN** 同じフィールド値を持つ 2 つの `Section` または `Cell` インスタンス（`SectionAccessory.view` の `KsAnyView` 中身は問わない）
- **WHEN** ハッシュ値および等価性を比較する
- **THEN** ハッシュ値が一致し、等価と判定される

#### Scenario: フィールド変更後は等しくない

- **GIVEN** `text` ケース内容や Cell の通常フィールドが 1 つだけ異なる 2 つのインスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない

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

## ADDED Requirements

### Requirement: SettingsRootDiff 型

`settings-view-core` は、`SettingsRoot` に対する部分更新を表現する `SettingsRootDiff` 型を提供しなければならない (SHALL)。`SettingsRootDiff` は Swift では `enum`、Kotlin では `sealed interface` として定義され、以下のケースを持たなければならない (MUST)：

- `full(SettingsRoot)`: 全体差し替え
- `insertSection(at: Int, section: Section)`: Section 追加
- `removeSection(sectionID)`: Section 削除（Swift は `UUID`、Kotlin は `String`）
- `moveSection(from: Int, to: Int)`: Section 順序変更
- `replaceSection(sectionID, new: Section)`: Section 全体置換
- `insertCell(sectionID, at: Int, cell)`: Section 内 Cell 追加（`cell` は Swift `any KsCell`、Kotlin `Cell`）
- `removeCell(cellID)`: Cell 削除（Swift は `KsCellID`、Kotlin は `String`）
- `replaceCell(cellID, new: Cell)`: Cell 置換
- `moveCell(cellID, to: Int)`: Cell 順序変更（Section 内のみ、Section 間移動は別途 `removeCell` + `insertCell` で表現）
- `updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)`: Root H/F / Section H/F の追加・更新・削除（`nil` は削除を意味する）
- `updateTheme(Theme)`: Theme 差分更新

`SettingsRootDiff` は `Hashable` / `equals` / `hashCode` 契約を満たさなければならない (MUST)。Swift では `Hashable` プロトコルへ準拠する（ただし `insertCell` / `replaceCell` など `any KsCell` を含むケースは existential type の制約により手動 `Hashable` 実装が必要であり、内部で `AnyHashable` 経由などで Cell の hash を取り込む）。Kotlin では各ケースを `data class` として定義し `equals` / `hashCode` を自動取得する。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `SettingsRootDiff` を参照する
- **THEN** `public enum SettingsRootDiff: Hashable` であり、上記 11 ケースを持つ

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `SettingsRootDiff` を参照する
- **THEN** `sealed interface SettingsRootDiff` であり、各ケースは `data class` または `object` として定義される

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

### Requirement: AccessoryTarget 型

`settings-view-core` は、Diff API での Accessory 更新対象を表現する `AccessoryTarget` 型を提供しなければならない (SHALL)。`AccessoryTarget` は以下のケースを持たなければならない (MUST)：

- `rootHeader`: Root レベルのヘッダ
- `rootFooter`: Root レベルのフッタ
- `sectionHeader(sectionID)`: 指定 Section のヘッダ（Swift は `UUID`、Kotlin は `String`）
- `sectionFooter(sectionID)`: 指定 Section のフッタ

`AccessoryTarget` は Swift `Hashable`、Kotlin `data class` で `equals` / `hashCode` を自動取得しなければならない (MUST)。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `AccessoryTarget` を参照する
- **THEN** `public enum AccessoryTarget: Hashable` であり、上記 4 ケースを持つ

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `AccessoryTarget` を参照する
- **THEN** `sealed interface AccessoryTarget` であり、`object RootHeader`、`object RootFooter`、`data class SectionHeader(sectionId: String)`、`data class SectionFooter(sectionId: String)` の 4 サブタイプを持つ

#### Scenario: 同一 sectionID の sectionHeader は等価

- **GIVEN** 同じ `sectionID` を持つ 2 つの `AccessoryTarget.sectionHeader`
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: 異なる種別は等価でない

- **GIVEN** `AccessoryTarget.rootHeader` と `AccessoryTarget.rootFooter`
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない

### Requirement: SettingsAccessory 型

`settings-view-core` は、`SettingsRootDiff.updateAccessory` で `RootAccessory` と `SectionAccessory` を統一的に扱うための `SettingsAccessory` 型を提供しなければならない (SHALL)。`SettingsAccessory` は以下の 2 ケースを持つ sum type でなければならない (MUST)：

- `root(RootAccessory)`: Root レベル H/F に使用
- `section(SectionAccessory)`: Section レベル H/F に使用

`SettingsAccessory` は Swift `Hashable`、Kotlin `sealed interface` + `data class` で `equals` / `hashCode` を実装しなければならない (MUST)。ただし内部 `RootAccessory.view` / `SectionAccessory.view` ケースの `KsAnyView` は Hashable 不参加であるため、ケース一致のみで等価とみなされる（既存の `RootAccessory` / `SectionAccessory` の等価性契約を継承）。

`SettingsAccessory` は `RootAccessory` / `SectionAccessory` を置き換えるものではない (MUST NOT)。`RootAccessory` / `SectionAccessory` は独立した別型として維持され、Store API や利用者コードでは個別型を使う。`SettingsAccessory` は Diff DTO 内部での統一表現専用とする。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `SettingsAccessory` を参照する
- **THEN** `public enum SettingsAccessory: Hashable` であり、`case root(RootAccessory)` と `case section(SectionAccessory)` の 2 ケースを持つ

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `SettingsAccessory` を参照する
- **THEN** `sealed interface SettingsAccessory` であり、`data class Root(val accessory: RootAccessory) : SettingsAccessory` と `data class Section(val accessory: SectionAccessory) : SettingsAccessory` の 2 サブタイプを持つ

#### Scenario: ケース別判定

- **GIVEN** `SettingsAccessory.root(.text("X"))` と `SettingsAccessory.section(.text("X"))`
- **WHEN** 等価性を比較する
- **THEN** ケースが異なるため等価でないと判定される

#### Scenario: 同一ケース・同一中身は等価

- **GIVEN** `SettingsAccessory.root(.text("X"))` を 2 つ
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

### Requirement: SettingsRootDiff のユニットテスト

`settings-view-core` には、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory` の生成と等価性を検証するユニットテスト（iOS は XCTest、Android は JUnit）が含まれなければならない (SHALL)。

#### Scenario: SettingsRootDiff の生成テスト

- **GIVEN** `SettingsRootDiff` の全 11 ケース
- **WHEN** 各ケースのインスタンスを生成し、payload を取り出す
- **THEN** 生成時の引数と一致する payload が取り出せる

#### Scenario: AccessoryTarget の等価性テスト

- **GIVEN** `AccessoryTarget` の全 4 ケース
- **WHEN** 等価性および hashValue / hashCode を比較する
- **THEN** 同一ケース・同一引数は等価で同一ハッシュ、異なるケース・異なる引数は不等になる

#### Scenario: SettingsAccessory の等価性テスト

- **GIVEN** `SettingsAccessory.root(.text("X"))` と `SettingsAccessory.section(.text("X"))`
- **WHEN** 等価性を比較する
- **THEN** ケース不一致で不等と判定される

<!--
旧 Scenario「SettingsRoot の構築（Root H/F あり、text）」「SettingsRoot の構築（Root H/F あり、view）」「等価性（H/F なし同士）」「等価性（text 同士）」「等価性（view 同士、中身は無視）」「等価性（nil と非 nil）」は、MODIFIED Requirements: SettingsRoot ドメインモデルの変更後全文（Root H/F 関連 Scenario を含まない記述）で削除を表現している。

Root H/F は後続提案 `add-partial-update-native` で UI 層プロパティとして再導入される（Swift `KsSettingsViewController.rootHeader: RootAccessory?`、Android `KsSettingsView.headerView: View?` など）。MAUI では進行中提案修正で `SettingsView.HeaderView` / `FooterView` BindableProperty が導入される。利用者は `SettingsRoot(header:, footer:, ...)` の構築コードを Store / View の API 呼び出しに書き換える必要がある。
-->

