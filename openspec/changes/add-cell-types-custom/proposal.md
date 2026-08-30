## Why

旧 `AiForms.Maui.SettingsView` には任意の View をセル化できる `CustomCell` があり、ユーザーが定義済みの Cell では足りない場合に独自 UI を埋め込む用途で多用されていた。本変更提案で iOS / Android Native の `CustomCell` を実装する。利用者は SwiftUI / UIKit View（iOS）、Composable / Android View（Android）を任意に渡してセル化できる。最低 iOS 16 サポートのため、SwiftUI 統合は `UIHostingConfiguration`（iOS 16+）をデフォルトで採用し、`UIHostingController` の手動親付けを排除した軽量・安全な実装とする。UIView ベース／Compose ベース／Android View ベースのカスタムセルも併せて提供する。

> 注: Section ヘッダ／フッタおよび Root ヘッダ／フッタへの任意 View 描画は本変更提案のスコープ外である。装飾領域（H/F）には Cell 概念を持ち込まない方針が `refactor-accessory-and-root-hf` で確定し、`SectionAccessory.view(KsAnyView)` および `RootAccessory.view(KsAnyView)` として Cell とは別の型消去ラッパで扱われる。当該描画は `settings-view-ios-ui` / `settings-view-android-ui` 側で本実装される。本変更提案は `CustomCell`（Cell 本体）のみに集中する。

## What Changes

- iOS `KsSettingsViewUI` に以下を追加：
  - `public struct CustomCell<Content: Hashable & Identifiable>: KsCell`：`content: Content` を保持し、利用者が指定した「Content → SwiftUI View 生成関数」または「Content → UIView 生成関数」を `KsCellRegistry` で登録する。`KsCell` 抽象は `purify-core-extract-style-to-ui-layer` により `style` プロパティ要求を持たないため、`style: CellStyle` は **任意プロパティ** として保持する（`DSLStyleModifiable` 準拠手段。`CellStyle` は UI 層所属で `UIColor?` / `UIFont?` 直接保持）
  - SwiftUI 統合：`UICollectionViewCell` の `contentConfiguration` に `UIHostingConfiguration { ... }` を設定する方式を採用し、Cell 再利用時は configuration の差し替えのみで再描画する
  - UIView 統合：`UIViewCustomCellView<Content>: UICollectionViewCell, KsCellRenderer`（従来通り `contentView` に UIView をぶら下げる）
  - `KsCellRegistry.registerSwiftUICustomCell<Content, V: View>(contentType:_:)` および `registerUIViewCustomCell<Content>(contentType:_:)` の 2 系統登録 API
- Android `ks-settingsview-ui` に以下を追加：
  - `data class CustomCell<Content : Any>(val id: String, val style: CellStyle, val content: Content) : Cell`：`Cell` 抽象は `purify-core-extract-style-to-ui-layer` により `style` プロパティ要求を持たないため、`style: CellStyle` は **任意プロパティ** として保持する（`DSLStyleModifiableCell` 準拠手段。`CellStyle` は UI 層所属で Compose `Color?` / `TextStyle?` 直接保持）
  - `ComposeCustomCellViewHolder`：`ComposeView` を内包し、`@Composable (Content) -> Unit` を呼び出し
  - `ViewCustomCellViewHolder`：従来 Android View を View ファクトリで埋め込む
  - `KsCellRegistry.registerComposeCustomCell<Content>(contentClass, content: @Composable (Content) -> Unit)`
  - `KsCellRegistry.registerViewCustomCell<Content>(contentClass, factory: (Context, Content) -> View)`
- `ComposeView` ライフサイクル管理（`DisposeOnDetachedFromWindow`）を ViewHolder 基底で適用する
- **`add-declarative-dsl` 連動**: iOS `CustomCell` struct に `id: UUID = UUID()` デフォルト値、Android `CustomCell` data class に `id: String = "custom-cell-${UUID.randomUUID()}"` デフォルト値を持たせ、DSL 経路で `id` 引数省略を可能にする
- **`add-declarative-dsl` 連動 + `purify-core-extract-style-to-ui-layer` 追随**: Android の `CustomCell` data class は **UI 層に再配置された** `DSLReidentifiableCell` / `DSLStyleModifiableCell` 規約に準拠、iOS の `CustomCell` struct は **UI 層に再配置された** `DSLReidentifiable` / `DSLStyleModifiable` 規約に準拠。`DSLStyleModifiable` / `DSLStyleModifiableCell` および `CellStyle` 型は UI 層所属、Native 型 (`UIColor?` / `UIFont?` ／ Compose `Color?` / `TextStyle?`) を直接保持する
- **`add-declarative-dsl` 連動**: Android の `CustomCell` に対応する `DSLSectionScope` 拡張関数（`fun <Content : Any> DSLSectionScope.CustomCell(content: Content, style: CellStyle = CellStyle()): CellHandle`）を `ks-settingsview-compose` モジュールに追加
- iOS / Android Sample に各 1 つカスタム表示の例を追加（CustomCell を含むサンプル、新 DSL 形式で記述）
- ユニットテスト：bind 後の content View 生成、recycle 後の View リセット、メモリリーク（複数回 push/pop で deinit/dispose 確認）、DSL 拡張関数経由での CustomCell 配置検証

## Capabilities

### New Capabilities
- `cell-types-custom`: 任意ビューをセル化する CustomCell（iOS UIView / SwiftUI View、Android View / Composable）の振る舞いを規定する。

### Modified Capabilities
（なし）

> 注: 本提案の `CustomCell` 関連の DSL 拡張関数（`fun <Content : Any> DSLSectionScope.CustomCell(...)`）は `cell-types-custom` capability の Requirement「Compose DSL 拡張関数による CustomCell 直置き」として規定する。`settings-view-android-ui` capability の Compose DSL Requirement で既に「具象 Cell 型ごとに `DSLSectionScope` の拡張関数として直置き API を提供する規約」が `add-declarative-dsl` で確定済みのため、本提案はその規約に従って実装するのみで、`settings-view-android-ui` 自体の Requirement を Modify する必要はない。

> 注: Section H/F・Root H/F への任意 View 描画は `refactor-accessory-and-root-hf` 提案で本実装される（`KsAnyView` ベース、Cell 概念排除）。本提案は `CustomCell`（Cell 本体）のみを対象とし、`settings-view-ios-ui` / `settings-view-android-ui` の H/F 描画には踏み込まない。

## Impact

- 影響範囲：iOS UI モジュール、Android UI モジュール、Android Compose モジュール（DSL 拡張関数追加）、両 Sample
- 依存：`add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`refactor-accessory-and-root-hf`（`KsAnyView` 型と装飾領域の Cell 概念排除に依存。`AnyCell` 型は本提案で再定義する Cell 抽象に再構成する）、**`add-declarative-dsl`**（DSL 拡張関数規約・`SectionHandle` / `CellHandle` の定義を前提とする）、**`purify-core-extract-style-to-ui-layer`**（`Cell` 抽象から `style` 要求削除、`DSLStyleModifiable` / `DSLStyleModifiableCell` / `CellStyle` / `Theme` の UI 層配置、Native 型直接保持への切替を前提とする）
- 並列可能：`add-cell-types-basic`、`add-cell-types-input` と並列で開発可能
- 後続変更が依存：`add-maui-cells`（MAUI 側 `CustomCell` Handler の実装、および `samples/maui/` への CustomCell ページ追加を担当。MAUI では DataTemplate で C# View を使う）
- リスク：高。`UIHostingController` の lifecycle、`ComposeView` の lifecycle、ジェネリック型登録 API の使い心地が要慎重設計
