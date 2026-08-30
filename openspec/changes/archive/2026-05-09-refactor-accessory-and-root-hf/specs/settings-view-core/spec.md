## ADDED Requirements

### Requirement: KsAnyView 型消去ラッパ

`settings-view-core` は、装飾領域（Root H/F、Section H/F の `view` ケース）に任意 View を格納するための型消去ラッパ `KsAnyView` を提供しなければならない (SHALL)。`KsAnyView` は SwiftUI `View` / `UIView`（iOS）、`@Composable` / Android `View`（Android）の二択 backing を持たなければならない (MUST)。`KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持ってはならない (MUST NOT)。

#### Scenario: Swift 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `KsAnyView` を参照する
- **THEN** `public struct KsAnyView`（または非 `Hashable` の class/struct）であり、内部 backing として `swiftUI(() -> AnyView)` と `uiKit(() -> UIView)` の二択を保持する。`KsAnyView` 自身は `Hashable` / `Equatable` に準拠しない

#### Scenario: Swift ファクトリ API

- **GIVEN** Swift `KsAnyView`
- **WHEN** 利用者が任意の SwiftUI View または UIView を渡す
- **THEN** `KsAnyView.swiftUI<V: SwiftUI.View>(@ViewBuilder _ build: @escaping () -> V)` および `KsAnyView.uiKit(_ factory: @escaping () -> UIView)` の 2 系統ファクトリで生成でき、利用者は identity を渡す必要がない

#### Scenario: Kotlin 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `KsAnyView` を参照する
- **THEN** `sealed interface KsAnyView` であり、サブタイプとして `class Compose(val content: @Composable () -> Unit) : KsAnyView` および `class AndroidView(val factory: (Context) -> View) : KsAnyView` を持つ。`equals` / `hashCode` は `Any` のデフォルト（参照同一性）以外を実装しない

#### Scenario: 差分検出非対応の明示

- **GIVEN** `KsAnyView` を保持するコンテナ（`SettingsRoot` / `SectionAccessory`）
- **WHEN** コンテナの等価性比較が呼ばれる
- **THEN** `KsAnyView` のフィールドはコンテナの `Hashable` / `equals` / `hashCode` の判定対象から除外される（存在の有無のみ判定し、中身の View は比較しない）

### Requirement: RootAccessory 型

`settings-view-core` は、`SettingsRoot` のヘッダ／フッタ位置に配置可能な内容を表す `RootAccessory` 型を提供しなければならない (SHALL)。`RootAccessory` は `text` ケース（`String`）と `view` ケース（`KsAnyView`）の 2 ケースを持つ sum type でなければならない (MUST)。`SectionAccessory` とは別型として定義しなければならない (MUST)。Swift では `Hashable` に準拠しなければならないが、`view(KsAnyView)` ケースの `Hashable` 実装は `KsAnyView` の中身を hash 計算に含めず、ケース判別のみで判定する手動実装としなければならない (MUST)。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `RootAccessory` を参照する
- **THEN** `public enum RootAccessory: Hashable` であり、`case text(String)` および `case view(KsAnyView)` の 2 ケースを持つ。`Hashable` は手動実装で、`text` ケースは内部 String の hash、`view` ケースは `KsAnyView` の中身を含めずケース判別のみで hash する

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `RootAccessory` を参照する
- **THEN** `sealed interface RootAccessory` であり、`data class Text(val value: String) : RootAccessory` および `class View(val view: KsAnyView) : RootAccessory` の 2 サブタイプを持つ

#### Scenario: SectionAccessory との別型保証

- **GIVEN** `RootAccessory` と `SectionAccessory`
- **WHEN** 型チェックを行う
- **THEN** 互いに代入互換性を持たない別型であり、API レベルで Root と Section の役割が区別される

#### Scenario: 等価性（text ケース）

- **GIVEN** 同じ文字列を持つ 2 つの `RootAccessory.text`
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: 等価性（view ケース）

- **GIVEN** 2 つの `RootAccessory.view`（中身の `KsAnyView` は問わない）
- **WHEN** 等価性を比較する
- **THEN** `KsAnyView` は差分検出に参加しないため、`view` ケースは「中身を比較せず、ケース一致のみで等価」とみなす（`text` ケースとは別ケースとして区別される）

## MODIFIED Requirements

### Requirement: SettingsRoot ドメインモデル

`SettingsRoot` は設定画面全体の状態を表すルート型でなければならない (SHALL)。複数の `Section` と全体に適用される `Theme` を保持しなければならない (MUST)。任意の Root ヘッダ（`header: RootAccessory?`）および任意の Root フッタ（`footer: RootAccessory?`）を保持しなければならない (MUST)。値の等価性は `sections` と `theme` の等価性、および `header` / `footer` の存在有無（`nil` / 非 `nil`）と `text` ケース内容で決定されなければならない (MUST)。`view` ケースの中身（`KsAnyView`）は等価性判定対象から除外しなければならない (MUST)。

#### Scenario: SettingsRoot の構築（Root H/F なし）

- **GIVEN** 任意個数の `Section` リストと任意の `Theme`、`header = nil`、`footer = nil`
- **WHEN** `SettingsRoot` を構築する
- **THEN** `sections`、`theme`、`header`、`footer` を保持するイミュータブル値型として生成される

#### Scenario: SettingsRoot の構築（Root H/F あり、text）

- **GIVEN** `header = RootAccessory.text("プロフィール")`、`footer = RootAccessory.text("v1.0.0")`、Section リスト、Theme
- **WHEN** `SettingsRoot` を構築する
- **THEN** `header` から `"プロフィール"`、`footer` から `"v1.0.0"` を取り出せる

#### Scenario: SettingsRoot の構築（Root H/F あり、view）

- **GIVEN** `header = RootAccessory.view(anyView)`（`anyView` は任意の SwiftUI View または UIView をラップした `KsAnyView`）
- **WHEN** `SettingsRoot` を構築する
- **THEN** `header` から `KsAnyView` を取り出せ、UI 層が任意 View として描画する根拠となる

#### Scenario: 等価性（H/F なし同士）

- **GIVEN** 同じ `sections` と `theme` を持ち `header` / `footer` がともに `nil` の 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: 等価性（text 同士）

- **GIVEN** 同じ `sections` / `theme` / `header = .text(同一文字列)` を持つ 2 つの `SettingsRoot`
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: 等価性（view 同士、中身は無視）

- **GIVEN** `header = .view(viewA)` と `header = .view(viewB)` を持つ 2 つの `SettingsRoot`（`sections` / `theme` は同一）
- **WHEN** 等価性を比較する
- **THEN** `KsAnyView` の中身は判定対象外のため、両者は等価と判定される（ケース一致 + 存在のみで判断）

#### Scenario: 等価性（nil と非 nil）

- **GIVEN** `header = nil` の `SettingsRoot` と `header = .view(...)` の `SettingsRoot`
- **WHEN** 等価性を比較する
- **THEN** 不等と判定される

### Requirement: Section ドメインモデル

`Section` は単一セクションを表す値型でなければならない (SHALL)。一意な `id`、任意の `header`（`SectionAccessory?`）、任意の `footer`（`SectionAccessory?`）、`Cell` のリストを保持しなければならない (MUST)。`header` / `footer` は文字列のみならず、任意の View を `KsAnyView` 経由で格納できなければならない (MUST)。Cell（タップ・選択・編集する行）は `cells` フィールドにのみ格納し、`header` / `footer` には格納しない (MUST NOT)。

#### Scenario: Section の構築（文字列ヘッダ）

- **GIVEN** id・header（`SectionAccessory.text("一般")`）・footer（`nil`）・cells リスト
- **WHEN** `Section` を構築する
- **THEN** すべてのフィールドを保持するイミュータブル値型として生成され、`header` から元の文字列 `"一般"` を取り出せる

#### Scenario: Section の構築（任意 View ヘッダ）

- **GIVEN** id・header（`SectionAccessory.view(anyView)`）・footer（`nil`）・cells リスト（`anyView` は任意 View をラップした `KsAnyView`）
- **WHEN** `Section` を構築する
- **THEN** `header` から元の `KsAnyView` を取り出せ、UI 層が任意 View として描画する根拠となる

#### Scenario: 空セクション

- **GIVEN** cells が空リストの `Section`
- **WHEN** インスタンスを生成する
- **THEN** 例外なく構築でき、`cells.isEmpty` が真となる

### Requirement: SectionAccessory 型

`SectionAccessory` は Section のヘッダ・フッタ位置に配置可能な内容を表す sum type でなければならない (SHALL)。最低限、文字列を保持する `text` 種別と、任意 View を保持する `view` 種別を持たなければならない (MUST)。Cell（タップ・選択・編集する行）の概念を含んではならない (MUST NOT)。Swift では `Hashable` に準拠しなければならないが、`view(KsAnyView)` ケースの `Hashable` 実装は `KsAnyView` の中身を hash 計算に含めず、ケース判別のみで判定する手動実装としなければならない (MUST)。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `SectionAccessory` を参照する
- **THEN** `public enum SectionAccessory: Hashable` であり、`case text(String)` および `case view(KsAnyView)` の 2 ケースを持つ。`Hashable` は手動実装で、`text` ケースは内部 String の hash を組み合わせ、`view` ケースは `KsAnyView` の中身を hash 計算に含めず、ケース判別のみ（例: discriminator + 固定値）で hash する

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `SectionAccessory` を参照する
- **THEN** `sealed interface SectionAccessory` であり、`data class Text(val value: String) : SectionAccessory` および `class View(val view: KsAnyView) : SectionAccessory` の 2 サブタイプを持つ

#### Scenario: 等価性（text ケース）

- **GIVEN** 同じ文字列を持つ 2 つの `SectionAccessory.text`
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される（`text` と `view` は別ケースとして区別される）

#### Scenario: 等価性（view ケース、中身は無視）

- **GIVEN** 2 つの `SectionAccessory.view`（中身の `KsAnyView` は問わない）
- **WHEN** 等価性を比較する
- **THEN** `KsAnyView` は差分検出に参加しないため、`view` ケースは「ケース一致のみで等価」とみなされる

### Requirement: Hashable / equals 契約

すべての値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、`Theme`、`CellStyle` および各具象 `Cell`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは UICollectionViewDiffableDataSource および DiffUtil による差分検出の前提条件である。

ただし、装飾領域専用の型消去ラッパ `KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持たない (MUST NOT)。`SectionAccessory.view(KsAnyView)` および `RootAccessory.view(KsAnyView)` は手動実装で、`KsAnyView` の中身を判定対象外とし「ケース一致のみで等価」とみなさなければならない (MUST)。これに連動して `SettingsRoot` / `Section` の hash/equals も、`view` ケースの中身を判定対象から除外しなければならない (MUST)。

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

## REMOVED Requirements

### Requirement: AnyCell 型消去（iOS）

**Reason**: 旧 `SectionAccessory.custom(AnyCell)` ケースが廃止され、`AnyCell` の主要な利用先（Section H/F の任意 View 格納）が消失したため、本要件を削除する。Cell リストへの異種 Cell 格納（`Section.cells`）に必要な型消去機構は、後続提案 `add-cell-types-custom` の `CustomCell` 設計の中で再定義される。

**Migration**: 旧 `SectionAccessory.custom(AnyCell)` を経由していた利用箇所は `SectionAccessory.view(KsAnyView)` に書き換える。`Section.cells: [AnyCell]` を必要とする利用箇所は `add-cell-types-custom` 提案の Cell 抽象再設計を待つ（ただし旧 `Cell` プロトコル / `sealed interface` 自体は維持されるため、暫定的に `[any KsCell]` / `List<Cell>` 等で代用可）。
