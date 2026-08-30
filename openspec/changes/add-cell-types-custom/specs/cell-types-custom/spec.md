## ADDED Requirements

### Requirement: CustomCell の id デフォルト値規約

本変更提案で追加される iOS / Android の `CustomCell` は、UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に配置され、`add-declarative-dsl` で確定した「具象 Cell コンストラクタの `id` デフォルト値規約」に従わなければならない (SHALL)。`purify-core-extract-style-to-ui-layer` により `Cell` 抽象（iOS `KsCell` / Android `Cell`）から `style` プロパティ要求が削除されたため、`CustomCell` の `style: CellStyle` プロパティは **個別の任意プロパティ** として保持する（`DSLStyleModifiable` / `DSLStyleModifiableCell` 準拠手段）。`CellStyle` は UI 層所属で `UIColor?` / `UIFont?` ／ Compose `Color?` / `TextStyle?` を直接保持する型である。`DSLStyleModifiable` / `DSLStyleModifiableCell` 規約も UI 層に再配置されている。

- iOS: `CustomCell<Content>` struct は `id: UUID = UUID()` のデフォルト値を持つ
- Android: `CustomCell<Content>` data class は `id: String = "custom-cell-${java.util.UUID.randomUUID()}"` のデフォルト値を持つ
- DSL 経路では `DSLReidentifiable.withDSLID(_:)` / `DSLReidentifiableCell.withDSLId(...)` により本仕様の優先順位に従う ID に rebind される
- 利用者は DSL 内で `CustomCell(content: profileData)` のように `id` 引数を省略して記述できなければならない (MUST)

#### Scenario: id 引数省略で生成

- **GIVEN** iOS `CustomCell(content: ProfileData(name: "kamu"))`、Android `CustomCell(content = ProfileData(name = "kamu"))`（`id` 引数省略）
- **WHEN** Cell インスタンスを生成する
- **THEN** デフォルト値の UUID ベース ID が自動採番される

#### Scenario: DSL 経路での id rebind

- **GIVEN** DSL 経路で `Section("プロフィール") { CustomCell(content: profile) }`
- **WHEN** DSL → Diff 算出ロジックが評価される
- **THEN** コンストラクタデフォルト値の `id` は本仕様の優先順位に従う安定 ID に rebind される

### Requirement: Compose DSL 拡張関数による CustomCell 直置き

Android の `CustomCell<Content>` は、`add-declarative-dsl` で確定した「具象 Cell 型ごとの DSL 拡張関数」規約に従い、`DSLSectionScope` の拡張関数として直置き API を提供しなければならない (SHALL)。

- `fun <Content : Any> DSLSectionScope.CustomCell(content: Content, style: CellStyle = CellStyle()): CellHandle = cell(CustomCell(content = content, style = style))`
- `style` 引数の型 `CellStyle` は **UI 層所属** (`ks-settingsview-ui` パッケージ) であり、Compose `Color?` / `TextStyle?` を直接保持する型である（`purify-core-extract-style-to-ui-layer` で UI 層に再配置済み）
- 戻り値は `CellHandle` でなければならない (MUST)（`.cellHeight(...)` 等の handle 経由 modifier chain を可能にするため）
- 利用者は `Section("...") { CustomCell(content = profileData) }` のように iOS と並列な書き味で Cell を直置きできる

iOS 側では Swift `@resultBuilder SectionBuilder` の機構により Cell 値を直置きできるため、別途 DSL 拡張関数の規約は不要。iOS `CustomCell.style: CellStyle` プロパティは UI 層所属の `KsSettingsViewUI.CellStyle`（`UIColor?` / `UIFont?` 直接保持）を参照する。

#### Scenario: Compose DSL 内での CustomCell 直置き

- **GIVEN**
  ```kotlin
  KsCellRegistry.registerComposeCustomCell(ProfileData::class) { profile -> MyCustomComposable(profile) }
  KsSettingsView {
      Section("プロフィール") {
          CustomCell(content = ProfileData(name = "kamu"))
      }
  }
  ```
- **WHEN** Composition する
- **THEN** `DSLSectionScope` の拡張関数 `CustomCell(content:)` が解決され、内部で `cell(CustomCell(content = ...))` が呼ばれて Cell が DSL ツリーに追加される。登録済み Composable で描画される

### Requirement: CustomCell（iOS）

iOS の `CustomCell<Content: Hashable & Identifiable>` は `KsCell` に準拠する値型でなければならない (SHALL)。`content: Content` フィールドを持ち、ユーザーが事前登録した「Content → UIView 生成関数」または「Content → SwiftUI View 生成関数」によって任意の表示を行わなければならない (MUST)。

#### Scenario: SwiftUI View の埋め込み

- **GIVEN** SwiftUI View `MyCustomView(profile: ProfileData)` を定義済み、`KsCellRegistry.shared.registerSwiftUICustomCell(contentType: ProfileData.self) { profile in MyCustomView(profile: profile) }` で登録済み
- **WHEN** `SettingsRoot` に `CustomCell(id: ..., content: ProfileData(name: "kamu"))` を含めて表示
- **THEN** Cell には `MyCustomView(profile: ProfileData(name: "kamu"))` が埋め込まれて表示される

#### Scenario: UIView の埋め込み

- **GIVEN** UIView ファクトリ `(content: ProfileData) -> UIView` を `registerUIViewCustomCell` で登録済み
- **WHEN** SettingsRoot に CustomCell を含める
- **THEN** Cell には対応する UIView が表示される

### Requirement: CustomCell（Android）

Android の `CustomCell<Content : Any>` は `Cell` に準拠する `data class` でなければならない (SHALL)。`content: Content` を持ち、利用者が登録した Composable または View ファクトリで描画されなければならない (MUST)。`CustomCell` が `DSLStyleModifiableCell` 規約に準拠する際は、UI 層 (`ks-settingsview-ui` パッケージ) の `DSLStyleModifiableCell` および `CellStyle` を import しなければならない (MUST)。`ks-settingsview-core` (Core 層) には当該型は存在しない（`purify-core-extract-style-to-ui-layer` で UI 層に移動済みのため）。

#### Scenario: DSLStyleModifiableCell の UI 層 import

- **GIVEN** Android `CustomCell.kt` の import 文
- **WHEN** `DSLStyleModifiableCell` および `CellStyle` の import 元を確認する
- **THEN** `ks-settingsview-ui` (UI 層) からの import になっており、`ks-settingsview-core` (Core 層) からの import は存在しない

#### Scenario: Composable の埋め込み

- **GIVEN** `@Composable fun MyCustomComposable(profile: ProfileData)` を定義済み、`KsCellRegistry.registerComposeCustomCell(ProfileData::class) { profile -> MyCustomComposable(profile) }` で登録済み
- **WHEN** SettingsRoot に CustomCell を含めて表示
- **THEN** Cell には `MyCustomComposable(profile)` が描画される

#### Scenario: Android View の埋め込み

- **GIVEN** View ファクトリ `(Context, ProfileData) -> View` を `registerViewCustomCell` で登録済み
- **WHEN** SettingsRoot に CustomCell を含める
- **THEN** Cell には対応する Android View が表示される

### Requirement: UIHostingConfiguration による SwiftUI 統合（iOS）

iOS の SwiftUI 用 `CustomCellView<Content>` は、`UICollectionViewCell.contentConfiguration` プロパティに `UIHostingConfiguration { ... }`（iOS 16+）を設定する方式で SwiftUI View を表示しなければならない (SHALL)。`UIHostingController` を ViewHolder 内に手動で埋め込む実装を採用してはならない (MUST NOT)。

#### Scenario: contentConfiguration への設定

- **GIVEN** 登録済み SwiftUI ビルダ `(Content) -> some View` を保持する `SwiftUICustomCellView<Content>`
- **WHEN** Cell が初回 bind され `render(cell:theme:)` が呼ばれる
- **THEN** `self.contentConfiguration = UIHostingConfiguration { builder(content) }` が設定され、Auto Layout は OS が自動管理する

#### Scenario: 再利用時の更新

- **GIVEN** CustomCellView が一度 bind された後、別 Content で再 bind される
- **WHEN** `render(cell:theme:)` が呼ばれる
- **THEN** `contentConfiguration` が新しい Content から作られた `UIHostingConfiguration` に差し替わり、内部 SwiftUI ビューツリーは OS が効率的に更新する

#### Scenario: 再利用前のクリア

- **GIVEN** CustomCellView が `prepareForReuse()` を呼ばれた直後
- **WHEN** `contentConfiguration` を観察する
- **THEN** `nil` または空の configuration となっており、保持していた Content の参照が解放されている

### Requirement: ComposeView 統合（Android）

Android の `ComposeCustomCellViewHolder` は `ComposeView` を内蔵し、`setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)` を必ず設定しなければならない (MUST)。

#### Scenario: 戦略の自動設定

- **GIVEN** `ComposeCustomCellViewHolder` が ListAdapter から `onCreateViewHolder` で生成される
- **WHEN** ViewHolder の itemView の `ComposeView` を取得する
- **THEN** Composition Strategy が `DisposeOnDetachedFromWindow` に設定されている

#### Scenario: detach 時の Composition 破棄

- **GIVEN** ComposeView を含む Cell が画面外にスクロールアウトし、RecyclerView から detach される
- **WHEN** `onDetachedFromWindow` が呼ばれる
- **THEN** 内部 Composition が dispose される（Compose ランタイムによる自動処理）

### Requirement: 登録 API

`KsCellRegistry` は CustomCell を登録するための専用 API を提供しなければならない (SHALL)。Content 型単位で複数 CustomCell を区別できなければならない (MUST)。

#### Scenario: 複数 Content 型の登録

- **GIVEN** 既に `ProfileData` 型用の CustomCell を登録済み
- **WHEN** `LicenseInfo` 型用の CustomCell を追加登録する
- **THEN** SettingsRoot に両方の CustomCell（異なる Content 型）が混在しても、それぞれの登録に従って正しく描画される

#### Scenario: 同一 Content 型の重複登録

- **GIVEN** `ProfileData` 用 CustomCell を登録済み
- **WHEN** 同じ `ProfileData` で再登録する
- **THEN** 後勝ちで上書き登録される（旧登録は破棄）

### Requirement: メモリ管理

CustomCell が表示している View / Composable は、Cell が画面外に出たときおよび `KsSettingsViewController` / `KsSettingsView` が破棄されたときに適切に解放されなければならない (MUST)。

#### Scenario: iOS Content 参照の解放

- **GIVEN** CustomCell を含む `KsSettingsViewController` を `present` → `dismiss`
- **WHEN** dismiss 後 1 ランループ以上経過する
- **THEN** `UIHostingConfiguration` のビルダクロージャが保持していた Content および外部参照が解放され、Allocations 計測でリークが検出されない

#### Scenario: Android Composable の dispose

- **GIVEN** ComposeView を含む CustomCell が表示中
- **WHEN** Activity を finish する
- **THEN** Composition は dispose され、LeakCanary 等の検出ツールでリークが検出されない

### Requirement: ユニットテスト

CustomCell の登録、bind、再利用時の差し替え、メモリ管理を検証するユニットテストが存在しなければならない (SHALL)。

#### Scenario: iOS 再利用テスト

- **GIVEN** 同一 ViewHolder インスタンスが Content A で bind され、その後 Content B で再 bind される
- **WHEN** Content A → Content B の遷移を観察する
- **THEN** `contentConfiguration` が Content B 由来の `UIHostingConfiguration` に差し替わっている

#### Scenario: Android リーク検証テスト

- **GIVEN** Robolectric テストで CustomCell を含む `KsSettingsView` を作成 → detach
- **WHEN** WeakReference で adapter / ViewHolder を観察、GC を強制発火
- **THEN** WeakReference が `null` になる（リークなし）
