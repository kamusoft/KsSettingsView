# settings-view-core Specification

## Purpose

`KsSettingsViewCore` は、設定画面 (Settings View) のドメインモデル層を提供する Native モジュールである。iOS (Swift Package) と Android (Gradle module) の両方で同等の値型・抽象を提供し、UI 層 (settings-view-ios-ui / settings-view-android-ui) から共有利用される。プラットフォーム固有型 (`UIColor`、`android.graphics.Color` など) を含まない論理モデルとして定義され、UICollectionViewDiffableDataSource および DiffUtil による差分検出の前提となる `Hashable` / `equals` 契約を満たす。
## Requirements
### Requirement: 表示状態同期の三層分離

SettingsView の表示状態同期は、**(1) 構造同期**、**(2) 内容更新**、**(3) 可視性変化** の三層に分離されなければならない (SHALL)。各 UI 層実装（Android / iOS）はこの原則に従わなければならない (MUST)。

- **(1) 構造同期**: 差分検出（Android `DiffUtil` / iOS `UICollectionViewDiffableDataSource` snapshot）は、Cell / Section の **追加・削除・移動・差し替え（id の変化）** を検出する目的に限定されなければならない (MUST)。構造同期の同一性判定は **id（識別子）の同一性のみ** を用いなければならず (MUST)、Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）を判定に用いてはならない (MUST NOT)。
- **(2) 内容更新**: 同一 id を持つ Cell の内容（表示プロパティ）の変化は、セルを破棄・再生成せずに **同一セル（Android: ViewHolder、iOS: Cell）の部分更新（reconfigure）** で反映されなければならない (MUST)。内容変化を「セルの差し替え（フルリバインド／reload）」として扱ってはならない (MUST NOT)。
- **(3) 可視性変化**: `Section.isVisible` / `Cell.isVisible` の変化は、上記 (1)(2) のいずれにも該当しない第三カテゴリとして扱わなければならない (MUST)。UI 層は **model（hidden 含むフル状態）と visible projection（`isVisible = true` のみで構成される表示用ビュー）を分離管理** しなければならない (MUST)。可視性変化は構造同期上の追加・削除アニメーションを伴って反映されなければならない (MUST)。可視性変化を (2) 内容更新（reconfigure 経路）で表現してはならない (MUST NOT)。

この分離により、内容変化（チェック ON/OFF・スイッチ・値更新等）が行全体の再生成（ちらつき）を引き起こさず、かつ可視性変化が構造同期の追加・削除として正しくアニメートされることを保証する。移植元 `AiForms.Maui.SettingsView` の Android 実装（`GetItemId(position) => position`、`CellPropertyChanged → NotifyItemChanged`）と同一の責務分担に、`Section.IsVisible` / `CellBase.IsVisible` のモデル保持＋表示射影の概念を加えたものである。

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

#### Scenario: Cell の isVisible 変化は構造同期上の追加削除になる

- **GIVEN** 同一 id の Cell について `isVisible` が `true → false` に変化する
- **WHEN** 表示状態が更新される
- **THEN** UI 層の visible projection から当該 Cell が除外され、構造同期上は削除として検出される。逆に `false → true` の変化は visible projection に当該 Cell が追加され、構造同期上は挿入として検出される。これは reconfigure 経路を経由しない

#### Scenario: Section の isVisible 変化は section 全体の追加削除になる

- **GIVEN** 同一 id の Section について `isVisible` が `true → false` に変化する
- **WHEN** 表示状態が更新される
- **THEN** UI 層の visible projection から当該 section（header / footer / cells 含む）が除外され、構造同期上は section 削除として検出される

#### Scenario: model と visible projection の分離管理

- **GIVEN** `SettingsRoot.sections` に hidden な Section / Cell を含む model
- **WHEN** UI 層が描画する
- **THEN** UI 層は model（hidden 含むフル状態）を保持しつつ、描画には visible projection（hidden を除外した派生ビュー）を用いる。model と visible projection はそれぞれ独立した責務として管理される

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

### Requirement: Section ドメインモデル

`Section` は単一セクションを表す値型でなければならない (SHALL)。一意な `id`、任意の `header`（`SectionAccessory?`）、任意の `footer`（`SectionAccessory?`）、`Cell` のリスト、`headerHeight: Double`（既定 `-1`）、および `isVisible: Bool`（既定 `true`）を保持しなければならない (MUST)。`header` / `footer` は文字列のみならず、任意の View を `KsAnyView` 経由で格納できなければならない (MUST)。Cell（タップ・選択・編集する行）は `cells` フィールドにのみ格納し、`header` / `footer` には格納しない (MUST NOT)。

`headerHeight: Double` は AiForms.Maui.SettingsView の `Section.HeaderHeight` 相当のプロパティでなければならない (MUST)。意味は以下：

- `-1`（既定値） → 「自動高さ」を意味し、`header` テキストが空または未設定の場合は UI 層は Section Header の supplementary 自体を生成してはならない (MUST NOT)。`header` テキストが存在する場合は UI 層がテキスト寸法に基づいて自動算出する。
- 正値（> 0） → その値を固定高さとして用いる。

`isVisible: Bool` は AiForms.Maui.SettingsView の `Section.IsVisible` 相当のプロパティでなければならない (MUST)。意味は以下：

- `true`（既定値） → 通常の表示。UI 層は当該 Section（header / footer / cells 含む）を visible projection に含め、描画する。
- `false` → UI 層は当該 Section（header / footer / cells 含む）を visible projection から除外しなければならない (MUST)。model 上にはデータとして保持されなければならず (MUST)、`true` に戻したとき元の位置に復活しなければならない (MUST)。

`isVisible` は値型としての等価性に含まれなければならない (MUST)（`Hashable` / `equals` の判定対象）。

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

#### Scenario: headerHeight 既定値

- **GIVEN** `Section(id: ..., header: nil, footer: nil, cells: [...])`（`headerHeight` を指定しない）
- **WHEN** インスタンスを生成する
- **THEN** `section.headerHeight == -1`（自動）が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: headerHeight 明示指定（固定高さ）

- **GIVEN** `Section(id: ..., header: SectionAccessory.text("一般"), footer: nil, cells: [...], headerHeight: 40)`
- **WHEN** 値を参照する
- **THEN** `section.headerHeight == 40` を保持し、UI 層はその値を固定 Header 高さとして用いる

#### Scenario: headerHeight = -1 で header テキスト空のときの supplementary 非生成（UI 層への契約）

- **GIVEN** `Section(id: ..., header: nil, footer: nil, cells: [...])`（既定 `headerHeight = -1`、`header` 未指定）
- **WHEN** UI 層が描画する
- **THEN** UI 層は Section Header の supplementary 領域を生成してはならず、Section 間に header 由来の余白が発生しない

#### Scenario: isVisible 既定値

- **GIVEN** `Section(id: ..., header: ..., footer: ..., cells: [...])`（`isVisible` を指定しない）
- **WHEN** インスタンスを生成する
- **THEN** `section.isVisible == true` が適用され、既存呼び出しがビルドエラー・実行時エラーを起こさない

#### Scenario: isVisible = false の Section は visible projection から除外される

- **GIVEN** `Section(id: ..., header: ..., footer: ..., cells: [...], isVisible: false)`
- **WHEN** UI 層が描画する
- **THEN** UI 層は当該 Section の header / footer / 全 cells を visible projection から除外し、画面には描画しない。一方で `SettingsRoot.sections` には当該 Section が保持されたままである

#### Scenario: isVisible を true に戻すと元の位置に復活する

- **GIVEN** `isVisible: false` で描画から除外されていた Section について、`isVisible: true` に切り替える
- **WHEN** 表示状態が更新される
- **THEN** 当該 Section（同一 id・同一の cells リスト）が `SettingsRoot.sections` 内の元の位置に対応する描画位置に復活する

#### Scenario: 値型としての等価性に isVisible が含まれる

- **GIVEN** 同一 id・同一の他フィールドを持つ 2 つの `Section` インスタンスで、`isVisible` のみが異なる
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない。`Hashable` / `equals` の判定対象に `isVisible` が含まれる

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

### Requirement: CellTitleAlignment 列挙型

`KsSettingsViewCore` は `CellTitleAlignment` という公開列挙型を定義しなければならない (SHALL)。`start`、`center`、`end` の 3 ケースを持たなければならない (MUST)。これは `ButtonCell.titleAlignment` などのフィールドで使用される。

#### Scenario: 3 ケースの定義

- **GIVEN** `KsSettingsViewCore` モジュールをインポート
- **WHEN** `CellTitleAlignment` を参照する
- **THEN** `CellTitleAlignment.start` / `.center` / `.end` の 3 ケースがすべて参照可能で、Swift `enum` または Kotlin `enum class` として宣言されている

#### Scenario: Hashable / equals 契約

- **GIVEN** `CellTitleAlignment.center` の 2 つの参照
- **WHEN** 等価性を比較する
- **THEN** Swift `==` および Kotlin `equals()` で `true` を返す

### Requirement: Hashable / equals 契約

すべての構造ドメイン値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、各具象 `Cell`、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは一般的な値比較・テスト・コレクション操作のための値型としての性質である。**Core から削除された `Theme` / `CellStyle` は本契約の対象外となる (MUST NOT)。**

ただし、差分検出（diff / snapshot の構造同期）は、この内容等価性（`equals` / `Hashable` の全フィールド比較）を **構造同期の同一性判定に用いてはならない** (MUST NOT)。構造同期は id（識別子）の同一性のみを用いなければならない (MUST)（「表示状態同期の三層分離」Requirement を参照）。値型の `equals` / `Hashable` は値比較やテストでは引き続き全フィールドを比較してよい。

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

- **GIVEN** `SettingsRootDiff` の全 10 ケース
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

<!--
旧 Scenario「SettingsRoot の構築（Root H/F あり、text）」「SettingsRoot の構築（Root H/F あり、view）」「等価性（H/F なし同士）」「等価性（text 同士）」「等価性（view 同士、中身は無視）」「等価性（nil と非 nil）」は、MODIFIED Requirements: SettingsRoot ドメインモデルの変更後全文（Root H/F 関連 Scenario を含まない記述）で削除を表現している。

Root H/F は後続提案 `add-partial-update-native` で UI 層プロパティとして再導入される（Swift `KsSettingsViewController.rootHeader: RootAccessory?`、Android `KsSettingsView.headerView: View?` など）。MAUI では進行中提案修正で `SettingsView.HeaderView` / `FooterView` BindableProperty が導入される。利用者は `SettingsRoot(header:, footer:, ...)` の構築コードを Store / View の API 呼び出しに書き換える必要がある。
-->

