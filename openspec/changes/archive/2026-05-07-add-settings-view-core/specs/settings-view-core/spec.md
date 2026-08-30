## ADDED Requirements

### Requirement: SettingsRoot ドメインモデル

`SettingsRoot` は設定画面全体の状態を表すルート型でなければならない (SHALL)。複数の `Section` と全体に適用される `Theme` を保持しなければならない (MUST)。値の等価性は全フィールドの等価性で決定されなければならない (MUST)。

#### Scenario: SettingsRoot の構築

- **GIVEN** 任意個数の `Section` リストと任意の `Theme`
- **WHEN** `SettingsRoot` を構築する
- **THEN** `sections` と `theme` を保持するイミュータブル値型として生成される

#### Scenario: 等価性

- **GIVEN** 同じ `sections` と `theme` を持つ 2 つの `SettingsRoot` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される（Swift では `==`、Kotlin では `equals()`）

### Requirement: Section ドメインモデル

`Section` は単一セクションを表す値型でなければならない (SHALL)。一意な `id`、任意の `header`（`SectionAccessory?`）、任意の `footer`（`SectionAccessory?`）、`Cell` のリストを保持しなければならない (MUST)。`header` / `footer` は文字列のみならず、任意の Cell（SwiftUI View / Composable をセル化したもの）も格納できなければならない (MUST)。

#### Scenario: Section の構築（文字列ヘッダ）

- **GIVEN** id・header（`SectionAccessory.text("一般")`）・footer（`nil`）・cells リスト
- **WHEN** `Section` を構築する
- **THEN** すべてのフィールドを保持するイミュータブル値型として生成され、`header` から元の文字列 `"一般"` を取り出せる

#### Scenario: Section の構築（カスタムヘッダ）

- **GIVEN** id・header（`SectionAccessory.custom(anyCell)`）・footer（`nil`）・cells リスト（`anyCell` は任意 View をラップした Cell）
- **WHEN** `Section` を構築する
- **THEN** `header` から元の Cell を取り出せ、UI 層が任意 View として描画する根拠となる

#### Scenario: 空セクション

- **GIVEN** cells が空リストの `Section`
- **WHEN** インスタンスを生成する
- **THEN** 例外なく構築でき、`cells.isEmpty` が真となる

### Requirement: SectionAccessory 型

`SectionAccessory` は Section のヘッダ・フッタ位置に配置可能な内容を表す sum type でなければならない (SHALL)。最低限、文字列を保持する `text` 種別と、任意 Cell を保持する `custom` 種別を持たなければならない (MUST)。

#### Scenario: Swift enum 定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `SectionAccessory` を参照する
- **THEN** `public enum SectionAccessory: Hashable` であり、`case text(String)` および `case custom(AnyCell)` の 2 ケースを持つ

#### Scenario: Kotlin sealed interface 定義

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `SectionAccessory` を参照する
- **THEN** `sealed interface SectionAccessory` であり、`data class Text(val value: String) : SectionAccessory` および `data class Custom(val cell: Cell) : SectionAccessory` の 2 サブタイプを持つ

#### Scenario: 等価性

- **GIVEN** 同じ内部値（同一文字列、または同一 Cell）を持つ 2 つの `SectionAccessory` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される（`text` と `custom` は別ケースとして区別される）

### Requirement: Cell 抽象

`Cell` 抽象は全 Cell 種類が満たすべき共通契約でなければならない (SHALL)。一意な `id`（iOS は `UUID`、Android は `String`）と `CellStyle` を保持しなければならない (MUST)。

#### Scenario: Swift プロトコル定義

- **GIVEN** Swift `KsSettingsViewCore` モジュール
- **WHEN** `KsCell` プロトコルを参照する
- **THEN** `Hashable` および `Identifiable` を継承し、`var id: UUID { get }` および `var style: CellStyle { get }` を要求する

#### Scenario: Kotlin sealed 抽象

- **GIVEN** Kotlin `ks-settingsview-core` モジュール
- **WHEN** `Cell` を参照する
- **THEN** `sealed interface Cell` であり、`val id: String`、`val style: CellStyle` を抽象プロパティとして持つ

### Requirement: AnyCell 型消去（iOS）

iOS の `KsSettingsViewCore` は、異なる具象 Cell 型を同一コレクションに格納するための型消去ラッパ `AnyCell` を提供しなければならない (SHALL)。`AnyCell` は `Hashable` でなければならない (MUST)。

#### Scenario: 異種 Cell の格納

- **GIVEN** `LabelCell`（具象 Cell A）と `SwitchCell`（具象 Cell B）の各インスタンス
- **WHEN** `Section.cells: [AnyCell]` に両方を格納する
- **THEN** コンパイルエラーなく格納でき、`AnyCell` から元の具象型を `as?` で取り出せる

### Requirement: Theme 型

`Theme` は SettingsView 全体に適用される論理スタイルを保持する値型でなければならない (SHALL)。最低限、`separatorColor`、`cellBackgroundColor`、`selectedColor`、`headerTextColor`、`headerBackgroundColor`、`footerTextColor`、`footerBackgroundColor`、`scrollIndicatorVisible` を含まなければならない (MUST)。

#### Scenario: Theme のデフォルト値

- **GIVEN** デフォルトコンストラクタ（Swift: パラメータなし init、Kotlin: 引数なし）
- **WHEN** `Theme()` を構築する
- **THEN** 旧 AiForms.Maui.SettingsView の標準スタイルに準じたデフォルト値（システム標準の灰色 separator、白 cellBackground 等）を持つ

#### Scenario: プラットフォーム型を持たない

- **GIVEN** `Theme` の各プロパティ
- **WHEN** 型を確認する
- **THEN** `UIColor` や `android.graphics.Color` を直接持たず、論理表現（例: `KsColor` の独自 RGBA 値型、フォントファミリ名と weight を表す `KsFont`）のみを保持する

### Requirement: CellStyle 型

`CellStyle` は単一 Cell に適用されるスタイルを表す値型でなければならない (SHALL)。最低限、`titleColor`、`titleFont`、`descriptionColor`、`descriptionFont`、`iconSize`、`iconRadius`、`cellHeight`、`hintTextColor`、`hintTextFont` を含まなければならない (MUST)。

#### Scenario: CellStyle のデフォルト値

- **GIVEN** デフォルトコンストラクタ
- **WHEN** `CellStyle()` を構築する
- **THEN** 旧 AiForms.Maui.SettingsView の標準スタイルに準じたデフォルト値を持ち、`Theme` から継承可能なフィールドは「未指定（nil/null）」として表現される

#### Scenario: Theme との継承関係

- **GIVEN** `CellStyle` のあるフィールドが「未指定」、対応する `Theme` のフィールドに値あり
- **WHEN** UI 層が描画用に「実効スタイル」を計算する
- **THEN** `CellStyle` 未指定フィールドは `Theme` の値で補完される（実効スタイル合成は UI 層の責務であるため、Core ではフィールドが nullable / Optional で定義されていることのみが Core 仕様の対象）

### Requirement: Hashable / equals 契約

すべての値型（`SettingsRoot`、`Section`、`Theme`、`CellStyle`、`AnyCell` および各具象 Cell）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは UICollectionViewDiffableDataSource および DiffUtil による差分検出の前提条件である。

#### Scenario: 同一フィールドのインスタンスは等しい

- **GIVEN** 同じフィールド値を持つ 2 つの `Section` または `Cell` インスタンス
- **WHEN** ハッシュ値および等価性を比較する
- **THEN** ハッシュ値が一致し、等価と判定される

#### Scenario: フィールド変更後は等しくない

- **GIVEN** フィールドの 1 つだけ異なる 2 つのインスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない

### Requirement: ユニットテストの存在

各 Native モジュールには、上記 Requirement の Scenario を検証するユニットテスト（iOS は XCTest、Android は JUnit）が含まれなければならない (SHALL)。

#### Scenario: iOS テスト実行

- **GIVEN** `ios/` ディレクトリ
- **WHEN** `swift test` を実行する
- **THEN** `KsSettingsViewCoreTests` のテストがすべて成功する

#### Scenario: Android テスト実行

- **GIVEN** `android/` ディレクトリ
- **WHEN** `./gradlew :ks-settingsview-core:test` を実行する
- **THEN** すべてのユニットテストが成功する
