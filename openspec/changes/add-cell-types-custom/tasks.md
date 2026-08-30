## 1. iOS CustomCell 型定義

- [ ] 1.1 `ios/Sources/KsSettingsViewUI/Cells/CustomCell.swift` で `public struct CustomCell<Content: Hashable & Identifiable>: KsCell, DSLReidentifiable, DSLStyleModifiable` を実装
  - **add-declarative-dsl 連動**: `id: UUID = UUID()` デフォルト値、`style: CellStyle = CellStyle()` デフォルト値を持たせる
  - `DSLReidentifiable.withDSLID(_:)` および `DSLStyleModifiable.withStyle(_:)` を実装
  - **`purify-core-extract-style-to-ui-layer` 追随**: `KsCell` 抽象は `style` プロパティ要求を持たないため、`style: CellStyle` は **任意プロパティ** として保持する。`CellStyle` は UI 層所属 (`KsSettingsViewUI.CellStyle`、`UIColor?` / `UIFont?` 直接保持)、`DSLStyleModifiable` 規約も UI 層に再配置済みのため、UI 層内 import で循環依存にならない
- [ ] 1.2 ViewHolder 用に内部で content type をキーにした registry 引きを行うため、`AnyCustomCell` の型消去ヘルパを実装

## 2. iOS SwiftUI 用 CustomCellView（UIHostingConfiguration ベース）

- [ ] 2.1 `SwiftUICustomCellView.swift` で `final class SwiftUICustomCellView<Content>: UICollectionViewCell, KsCellRenderer` を実装
- [ ] 2.2 `render(cell:theme:)` 内で `self.contentConfiguration = UIHostingConfiguration { builder(content) }` を設定（Cell 再利用時はこの差し替えのみで反映）
- [ ] 2.3 `prepareForReuse()` では `contentConfiguration = nil` で SwiftUI ビューツリーをクリアし、リーク防止

## 3. iOS UIView 用 CustomCellView

- [ ] 3.1 `UIViewCustomCellView.swift` で `UICollectionViewCell, KsCellRenderer` を実装
- [ ] 3.2 `contentView` に対しファクトリで生成した UIView を addSubview し、再利用時は差し替え（古い view は removeFromSuperview）
- [ ] 3.3 Auto Layout 制約を `contentView` の四辺に固定する共通ヘルパを用意

## 4. iOS 登録 API

- [ ] 4.1 `KsCellRegistry+CustomCells.swift` で以下 2 つの拡張メソッドを実装：
  - `func registerSwiftUICustomCell<Content, V: View>(contentType: Content.Type, _ builder: @escaping (Content) -> V)`
  - `func registerUIViewCustomCell<Content>(contentType: Content.Type, _ factory: @escaping (Content) -> UIView)`
- [ ] 4.2 内部で content type を key としてファクトリを map に保持し、`AnyCell.dynamicType` 解決経由で正しい renderer に dispatch する

## 5. iOS ユニットテスト

- [ ] 5.1 `CustomCellRegistrationTests.swift`：SwiftUI / UIView 両方の登録・解決
- [ ] 5.2 `SwiftUICustomCellViewTests.swift`：bind 後の `contentConfiguration` 設定、再 bind での差し替え、`prepareForReuse` 後に nil 化されること
- [ ] 5.3 `CustomCellMemoryTests.swift`：Cell の再利用 / 画面破棄サイクルで Content への WeakReference が解放されること（UIHostingConfiguration 経由のため UIHostingController 直参照テストではなく Content 値型のリークを確認）

## 6. Android CustomCell 型定義

- [ ] 6.1 `android/ks-settingsview-ui/src/main/kotlin/.../cells/CustomCell.kt` で `data class CustomCell<Content : Any>(override val id: String = "custom-cell-${java.util.UUID.randomUUID()}", val style: CellStyle = CellStyle(), val content: Content) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell` を実装
  - **add-declarative-dsl 連動**: `id` パラメータにデフォルト値（UUID ベース）、`style` パラメータにデフォルト値を持たせる
  - **`purify-core-extract-style-to-ui-layer` 追随**: `Cell` 抽象は `style` プロパティ要求を持たないため、`style: CellStyle` は **任意プロパティ** として保持する（`override` 不要、`DSLStyleModifiableCell.withDSLStyle(...)` 準拠手段として個別実装）。`DSLReidentifiableCell.withDSLId(...)` および `DSLStyleModifiableCell.withDSLStyle(...)` を data class copy で実装
  - **モジュール依存に関する前提（`purify-core-extract-style-to-ui-layer` 追随）**: `DSLReidentifiableCell` interface は Core 層に残置されるが、`DSLStyleModifiableCell` interface および `CellStyle` 型（Compose `Color?` / `TextStyle?` 直接保持）は **UI 層 (`ks-settingsview-ui`)** に移動済み。`CustomCell` は UI 層配置のため、Core 版 `DSLReidentifiableCell` と UI 層版 `DSLStyleModifiableCell` を import して implement することで、`ks-settingsview-ui → ks-settingsview-compose` の循環依存を回避する

## 6.5. Android CustomCell の DSL 拡張関数（add-declarative-dsl 連動）

- [ ] 6.5.1 `android/ks-settingsview-compose/src/main/kotlin/.../CustomCellDsl.kt` を新規作成
- [ ] 6.5.2 `fun <Content : Any> DSLSectionScope.CustomCell(content: Content, style: CellStyle = CellStyle()): CellHandle = cell(CustomCell(content = content, style = style))` を実装
  - **オーバーロード解決の注記**: data class `CustomCell<Content>`（`ks-settingsview-ui` 配置）とジェネリック拡張関数 `DSLSectionScope.CustomCell<Content>`（`ks-settingsview-compose` 配置）は名前空間（パッケージ）が異なるため、利用側で両方 import するときの優先順位を意識する必要がある。DSL ブロック内（`DSLSectionScope` レシーバスコープ内）では Kotlin のオーバーロード解決により拡張関数版が優先される。詳細は `add-declarative-dsl` の `tasks.md` 25.3.2 の注記および `add-cell-types-basic` の `tasks.md` 1.5.5 の規約に従う
- [ ] 6.5.3 DSL 拡張関数のユニットテスト：DSL 内呼び出しで `CustomCell` が正しく `DSLCellNode` に格納され、戻り値 `CellHandle` への `.cellID(...)` / `.cellHeight(...)` chain が動作することを検証

## 7. Android Compose 用 ViewHolder

- [ ] 7.1 `ComposeCustomCellViewHolder.kt` で `class ComposeCustomCellViewHolder<Content>` を実装、`ComposeView` を内包し `DisposeOnDetachedFromWindow` を設定
- [ ] 7.2 bind 時に `composeView.setContent { content() }` で Composable を切り替え

## 8. Android View 用 ViewHolder

- [ ] 8.1 `ViewCustomCellViewHolder.kt` を実装、ファクトリで生成した View を `itemView` 内に差し替え

## 9. Android 登録 API

- [ ] 9.1 `KsCellRegistry+CustomCells.kt` で以下を実装：
  - `fun <Content : Any> registerComposeCustomCell(contentClass: KClass<Content>, content: @Composable (Content) -> Unit)`
  - `fun <Content : Any> registerViewCustomCell(contentClass: KClass<Content>, factory: (Context, Content) -> View)`
- [ ] 9.2 内部で `KClass` を key としてファクトリを map に保持

## 10. Android ユニットテスト

- [ ] 10.1 `CustomCellRegistrationTest.kt`：登録・解決
- [ ] 10.2 `ComposeCustomCellViewHolderTest.kt`（Compose Test）：bind / re-bind の検証
- [ ] 10.3 `CustomCellMemoryTest.kt`（Robolectric）：detach 後の WeakReference 解放

## 11. Sample 更新

> **前提**: `samples/ios/` / `samples/android/` の Sample アプリ土台は別変更提案 `add-samples-ios` / `add-samples-android` で整備される。本セクションのタスクはそれらの archive 完了後に着手する。
>
> **MAUI Sample への展開**: 本提案では行わない。`samples/maui/` への CustomCell ページ追加は別変更提案 `add-maui-cells` の責務であり、本提案は Native iOS / Android のみを対象とする。

- [ ] 11.1 `samples/ios/` SwiftUI Sample に CustomCell の表示例を追加（プロフィールカード Composition）
  - **add-declarative-dsl 連動**: 新 DSL 形式で記述（`Section("...") { CustomCell(content: profileData) }`、`id` 引数省略、Section H/F は `.sectionFooter(...)` modifier chain）
- [ ] 11.2 `samples/android/` Compose Sample に CustomCell の表示例を追加
  - **add-declarative-dsl 連動**: 新 DSL 形式で記述（`Section("...") { CustomCell(content = profileData) }` のように 6.5 で追加した DSL 拡張関数で直置き、`cell(...)` ラップ不要、`id` 引数省略）

## 12. ドキュメント

- [ ] 12.1 `docs/cell-types-custom.md` を作成し、CustomCell の登録 API 使用例を iOS（SwiftUI/UIView）/Android（Compose/View）でそれぞれ記載
- [ ] 12.2 メモリ管理上の注意点（参照リーク回避のための Content 設計、`remember` パターン）を記載

## 13. 全テスト実行

- [ ] 13.1 `ios/` で `swift test` 全成功
- [ ] 13.2 `android/` で `./gradlew :ks-settingsview-ui:test` 全成功
- [ ] 13.3 iOS Sample / Android Sample で CustomCell が表示されることを実機（またはシミュレータ・エミュレータ）で目視確認（Sample 土台は `add-samples-ios` / `add-samples-android` で整備済み前提。MAUI Sample での確認は `add-maui-cells` 側で担当）
- [ ] 13.4 CustomCell 含む画面の push/pop を 10 回繰り返し、Instruments / LeakCanary でリーク検出ゼロを確認

## 依存関係

- 先行：`add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-samples-ios`、`add-samples-android`、**`add-declarative-dsl`**（DSL 拡張関数規約・`SectionHandle` / `CellHandle` / `KsIdentifiable` の定義を前提とする）、**`purify-core-extract-style-to-ui-layer`**（`Cell` 抽象から `style` 要求削除、`DSLStyleModifiable` / `DSLStyleModifiableCell` / `CellStyle` / `Theme` の UI 層配置、Native 型直接保持への切替を前提とする）
- 並列可能：`add-cell-types-basic`、`add-cell-types-input` と並列で開発可能
- 後続：`add-maui-cells`（本提案完了後、CustomCell の MAUI 側 Handler 実装と `samples/maui/` への CustomCell ページ追加を担当）
- 本提案は Native iOS / Android のみを対象とし、MAUI Sample 拡張は責務に含めない

## 完了条件

- 全タスクのチェックボックスが完了している
- `cell-types-custom` capability の全 Scenario が通る
- iOS / Android の各 Sample で CustomCell の表示・操作が可能（Sample 土台は `add-samples-ios` / `add-samples-android` で整備済み前提）
- メモリリーク検出ゼロ
- 全ユニットテスト成功
