## 依存関係

- 前提:
  - `add-monorepo-foundation`（archive 済）: monorepo 構造
  - `add-settings-view-core`（archive 済）: iOS / Android Core
  - `add-settings-view-ios-ui`（archive 済）: iOS UI 基盤
  - `add-settings-view-android-ui`（archive 済）: Android UI 基盤
  - `add-partial-update-core`（先行・archive 必須）: `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` 型、`SettingsRoot.header/footer` 削除
  - `add-partial-update-native`（先行・archive 必須）: `SettingsRootStore` および `applyDiff` API、Root H/F の UI 層プロパティ化
- 後続:
  - `add-maui-core`: 本提案 archive 後に着手
  - `add-maui-cells`: 本提案 Bridge に他 Cell 用 `addXxxCell(...)` を追加していく

## 1. Native Bridge: iOS

- [ ] 1.1 `ios/Sources/KsSettingsViewBridge/` モジュールを作成し、`Package.swift` のターゲットに追加（`KsSettingsViewCore`、`KsSettingsViewUI` 依存）
- [ ] 1.2 `KsSettingsViewBridge.swift` で `@objc public class KsSettingsViewBridge` を実装、ファクトリメソッド `makeBuilder()` / `makeController(delegate:)` を公開
- [ ] 1.3 `KsSettingsViewBuilder.swift` で `@objc public class KsSettingsViewBuilder` を実装：
  - `beginSection(header:footer:)` / `endSection()`
  - `addLabelCell(id:title:description:valueText:icon:hintText:)`（本提案で実装する Cell 追加 API）
  - `build() -> KsSettingsRootDTO`
  - **Theme メソッド（`setTheme` 等）は Builder には実装しない**（`purify-core-extract-style-to-ui-layer` により Theme は `SettingsRoot` から分離されており、Bridge Controller / View 側の `setTheme(_:)` 独立 API で適用するため）
  - Root H/F メソッド（`setRootHeader` / `setRootFooter`）は Builder には**実装しない**（Bridge Controller / View 側で別途設定する設計のため）
- [ ] 1.4 `KsSettingsViewControllerBridge.swift` で Native Controller のラッパを実装：
  - `static func makeController(delegate: KsCellInteractionDelegate) -> KsSettingsViewControllerBridge`
  - `func setRoot(_ root: KsSettingsRootDTO)`: 全体差し替え・初期化
  - `func applyDiff(_ diff: KsSettingsRootDiffDTO)`: 部分更新（内部で Native `SettingsRootDiff` に変換し `controller.applyDiff(_:)` を呼ぶ）
  - `func setTheme(_ theme: KsThemeDTO)`: Theme 適用（内部で `KsThemeDTO` → Native `Theme` に変換し `SettingsRootStore.applyTheme(_:)` を呼ぶ。Diff Publisher は不発行）
  - `func setStyle(_ style: KsSettingsViewStyle)`
  - `func setRootHeader(_ view: KsAnyViewDTO?)` / `func setRootFooter(_ view: KsAnyViewDTO?)`: Root H/F 設定（Native `controller.rootHeader` / `controller.rootFooter` に反映）
  - `func updateCellValue(cellId: String, value: NSObject)`
- [ ] 1.5 `KsCellInteractionDelegate.swift` で `@objc public protocol KsCellInteractionDelegate` を定義（14 Cell 種別分の通知メソッド宣言を含めるが、本提案では LabelCell 経路のみ実装すれば良い）
- [ ] 1.6 Bridge 内で LabelCell の `onTap` を delegate メソッド `didTapCell(cellId:)` に転送
- [ ] 1.7 Bridge 内部に 200ms debounce ユーティリティを実装（EntryCell 等の高頻度通知用、本提案ではユーティリティのみ実装、Cell 連携は後続提案）
- [ ] 1.8 `swift package generate-xcframework` 用の build スクリプトを `ios/Scripts/build-bridge-xcframework.sh` に追加

## 2. Native Bridge: Android

- [ ] 2.1 `android/ks-settingsview-bridge/` Gradle サブプロジェクトを作成、`settings.gradle.kts` に追加
- [ ] 2.2 `build.gradle.kts` を `com.android.library` で作成（`ks-settingsview-core`、`ks-settingsview-ui` 依存）
- [ ] 2.3 `KsSettingsViewBridge.kt` で公開クラスを実装、`@JvmStatic` ファクトリ群を提供
- [ ] 2.4 `KsSettingsViewBuilder.kt` で iOS と対称な Builder API を実装：
  - `beginSection(header:footer:)` / `endSection()`
  - `addLabelCell(...)`（本提案で実装する Cell 追加 API）
  - `build(): KsSettingsRootDTO`
  - **Theme メソッド（`setTheme` 等）は Builder には実装しない**（`purify-core-extract-style-to-ui-layer` 方針に追随、Bridge View 側の `setTheme(_:)` 独立 API で適用するため）
  - Root H/F メソッドは Builder には**実装しない**（Bridge View 側で別途設定する設計のため）
- [ ] 2.5 `KsSettingsViewBridgeView.kt` で Native View のラッパを実装：
  - `companion object { @JvmStatic fun makeView(context: Context, listener: KsCellInteractionListener): KsSettingsViewBridgeView }`
  - `fun setRoot(root: KsSettingsRootDTO)`: 全体差し替え・初期化
  - `fun applyDiff(diff: KsSettingsRootDiffDTO)`: 部分更新（内部で Native `SettingsRootDiff` に変換し `view.applyDiff(_:)` を呼ぶ）
  - `fun setTheme(theme: KsThemeDTO)`: Theme 適用（内部で `KsThemeDTO` → Native `Theme` に変換し `SettingsRootStore.applyTheme(_:)` を呼ぶ。SharedFlow Diff は不発行）
  - `fun setStyle(style: KsSettingsViewStyle)`
  - `fun setRootHeader(view: KsAnyViewDTO?)` / `fun setRootFooter(view: KsAnyViewDTO?)`: Root H/F 設定（Native `view.rootHeader` / `view.rootFooter` に反映）
  - `fun updateCellValue(cellId: String, value: Any)`
- [ ] 2.6 `KsCellInteractionListener.kt` で `interface KsCellInteractionListener` を定義（14 Cell 種別分の通知メソッド宣言）
- [ ] 2.7 Bridge 内で LabelCell の `onTap` を listener メソッド `didTapCell(cellId)` に転送
- [ ] 2.8 Bridge 内部に 200ms debounce ユーティリティを実装
- [ ] 2.9 `./gradlew :ks-settingsview-bridge:assembleRelease` で aar が生成されることを確認

## 2.5. KsSettingsRootDiffDTO の実装

- [ ] 2.5.1 iOS: `ios/Sources/KsSettingsViewBridge/KsSettingsRootDiffDTO.swift` に `@objc public class KsSettingsRootDiffDTO : NSObject` 抽象基底型を実装する
- [ ] 2.5.2 iOS: **10 個**のサブクラス（`KsSettingsRootDiffFullDTO`、`KsSettingsRootDiffInsertSectionDTO`、`KsSettingsRootDiffRemoveSectionDTO`、`KsSettingsRootDiffMoveSectionDTO`、`KsSettingsRootDiffReplaceSectionDTO`、`KsSettingsRootDiffInsertCellDTO`、`KsSettingsRootDiffRemoveCellDTO`、`KsSettingsRootDiffReplaceCellDTO`、`KsSettingsRootDiffMoveCellDTO`、`KsSettingsRootDiffUpdateAccessoryDTO`）を実装する。**`KsSettingsRootDiffUpdateThemeDTO` は実装しない**（`purify-core-extract-style-to-ui-layer` で Native `SettingsRootDiff.updateTheme` が削除されたため、Theme 更新は `controller.setTheme(_:)` / `view.setTheme(_:)` 独立 API で扱う）
- [ ] 2.5.3 iOS: `KsAccessoryTargetDTO`（`KsAccessoryTargetRootHeaderDTO` / `KsAccessoryTargetRootFooterDTO` / `KsAccessoryTargetSectionHeaderDTO(sectionId)` / `KsAccessoryTargetSectionFooterDTO(sectionId)`）を実装する
- [ ] 2.5.4 iOS: `KsSettingsAccessoryDTO`（`KsSettingsAccessoryRootDTO(rootAccessory)` / `KsSettingsAccessorySectionDTO(sectionAccessory)`）を実装する
- [ ] 2.5.5 iOS: DTO → Native `SettingsRootDiff` 変換関数を実装する（`sectionId: String` → `UUID` 変換含む。Bridge 内部で sectionId Map を保持して整合性を維持）
- [ ] 2.5.6 Android: `android/ks-settingsview-bridge/src/main/kotlin/.../KsSettingsRootDiffDTO.kt` に `sealed class` 階層で **10 ケース** DTO を実装する（`UpdateThemeDTO` を含まない、`KsSettingsRootDiffUpdateThemeDTO` 等の Theme 更新ケースは導入しない）
- [ ] 2.5.7 Android: `KsAccessoryTargetDTO` / `KsSettingsAccessoryDTO` を実装する
- [ ] 2.5.8 Android: DTO → Native `SettingsRootDiff` 変換関数を実装する（`sectionId: String` はそのまま渡す）

## 2.6. KsThemeDTO の実装と setTheme 経路

`purify-core-extract-style-to-ui-layer` の方針追随：Theme は `SettingsRoot` / `SettingsRootDiff` から完全分離した独立 API として扱う。`KsColor` 中間 DTO を廃し、MAUI `Microsoft.Maui.Graphics.Color` から Native `UIColor` / Compose `Color` への 1 段直接変換を行う。

- [ ] 2.6.1 iOS: `ios/Sources/KsSettingsViewBridge/KsThemeDTO.swift` に `@objc public class KsThemeDTO : NSObject` を実装する。プロパティは Native UI 層 `KsSettingsViewUI.Theme` の各フィールドに対応する payload（`separatorColor` 等の MAUI `Color` を受け入れる ObjC 互換型 / フォント属性）を保持する。**`KsColorDTO` / `KsFontDTO` のような独自中間 Color / Font DTO 型は導入しない**
- [ ] 2.6.2 iOS: `KsThemeDTO` → Native `KsSettingsViewUI.Theme` 変換関数を Bridge 内部に実装する。MAUI Color → `UIColor` の 1 段直接変換、MAUI Font 指定 → `UIFont` の 1 段直接変換を行う（旧 `KsColor` 経由のロジックは導入しない）
- [ ] 2.6.3 iOS: `KsSettingsViewControllerBridge.setTheme(_ theme: KsThemeDTO)` を実装し、内部で `SettingsRootStore.applyTheme(_:)` を呼ぶ（Diff Publisher は不発行）
- [ ] 2.6.4 Android: `android/ks-settingsview-bridge/src/main/kotlin/.../KsThemeDTO.kt` を実装する。プロパティは Native UI 層 `Theme`（Compose `Color` / `TextStyle` 直接保持）に対応する payload を保持する。**`KsColorDTO` / `KsFontDTO` のような独自中間 DTO 型は導入しない**
- [ ] 2.6.5 Android: `KsThemeDTO` → Native `Theme` 変換関数を実装する。MAUI Color → Compose `Color` の 1 段直接変換、MAUI Font 指定 → Compose `TextStyle` の 1 段直接変換を行う
- [ ] 2.6.6 Android: `KsSettingsViewBridgeView.setTheme(theme: KsThemeDTO)` を実装し、内部で `SettingsRootStore.applyTheme(theme)` を呼ぶ（SharedFlow Diff は不発行）

## 3. MAUI バインディングプロジェクト: iOS

- [ ] 3.1 `maui/KsSettingsView.Bindings.iOS/` ディレクトリを作成
- [ ] 3.2 `KsSettingsView.Bindings.iOS.csproj` を `XcodeProject` 形式で作成（`maui-native-binding-skill` 参照）
- [ ] 3.3 `objective-sharpie` を使い `KsSettingsViewBridge.xcframework` から `ApiDefinitions.cs` を自動生成
- [ ] 3.4 必要な手動修正を `ApiDefinitions.cs` に適用、修正パッチを `Patches/` 配下に `.diff` で配置（不要な場合は `Patches/README.md` でその旨を記載）
- [ ] 3.5 `dotnet build -f net9.0-ios` がコマンドラインから警告なしで成功することを確認

## 4. MAUI バインディングプロジェクト: Android

- [ ] 4.1 `maui/KsSettingsView.Bindings.Android/` ディレクトリを作成
- [ ] 4.2 `KsSettingsView.Bindings.Android.csproj` を `AndroidGradleProject` 形式で作成
- [ ] 4.3 aar を取り込み、Java バインディング自動生成設定を確認
- [ ] 4.4 `dotnet build -f net9.0-android` がコマンドラインから警告なしで成功することを確認

## 5. MAUI ソリューションファイル

- [ ] 5.1 `maui/KsSettingsView.slnx` に `KsSettingsView.Bindings.iOS.csproj` を追加
- [ ] 5.2 `maui/KsSettingsView.slnx` に `KsSettingsView.Bindings.Android.csproj` を追加

## 6. Bridge ユニットテスト

- [ ] 6.1 `ios/Tests/KsSettingsViewBridgeTests/` テストターゲットを `Package.swift` に追加
- [ ] 6.2 `KsSettingsViewBuilderTests.swift`：Builder の `beginSection` / `addLabelCell` / `endSection` / `build` の組み合わせが期待する `KsSettingsRootDTO` を返すことを検証
- [ ] 6.3 `KsSettingsViewControllerBridgeTests.swift`：`setRoot` 連続呼び出しで Native 側 `DiffableDataSource` が差分のみ更新することを検証
- [ ] 6.4 `KsSettingsViewControllerBridgeApplyDiffTests.swift`：`applyDiff` が `KsSettingsRootDiffDTO` の全 **10 ケース**に対して Native `controller.applyDiff(_:)` を正しく呼ぶことを検証（`UpdateThemeDTO` ケースは存在しないため対象外）
- [ ] 6.5 `KsSettingsRootHeaderFooterTests.swift`：`setRootHeader(_ view:)` / `setRootHeader(_ view: nil)` で Native 側 `controller.rootHeader` が期待通り変化することを検証
- [ ] 6.5.1 `KsSettingsViewControllerBridgeThemeTests.swift`：`setTheme(_:)` が `KsThemeDTO` を Native `Theme` に変換し `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` が発火することを検証（Diff Publisher が不発行であることも assert）。MAUI Color → `UIColor` の 1 段直接変換結果も assert
- [ ] 6.6 `KsDebounceUtilityTests.swift`：200ms debounce ユーティリティが期待通り動作することを検証
- [ ] 6.7 `android/ks-settingsview-bridge/src/test/` に Builder / Bridge View / applyDiff (10 ケース) / setTheme (`KsThemeDTO` → Compose Color 直接変換と `StateFlow<Theme>` 発火、SharedFlow Diff 不発行) / debounce ユーティリティの対応テストを実装
- [ ] 6.8 `swift test --filter KsSettingsViewBridgeTests` と `./gradlew :ks-settingsview-bridge:test` の両方で全テストが成功することを確認

## 7. ドキュメント

- [ ] 7.1 `docs/maui-bindings.md` を作成し、以下のセクションを含む：
  - 概要・Native Library Interop パターン採用の理由
  - Bridge 公開 API 一覧（Builder / Controller / Delegate）と iOS / Android 用例
  - xcframework / aar のビルド手順（`build-bridge-xcframework.sh` / `assembleRelease`）
  - `objective-sharpie` 自動生成と手動修正パッチの運用ルール
- [ ] 7.2 `docs/maui-bindings.md` に「本提案では LabelCell のみ Bridge API として公開され、他 Cell は `add-maui-cells` で追加される」旨を明記

## 8. 全テスト・ビルド確認

- [ ] 8.1 iOS Bridge テスト全成功（`swift test`）
- [ ] 8.2 Android Bridge テスト全成功（`./gradlew :ks-settingsview-bridge:test`）
- [ ] 8.3 `dotnet build -f net9.0-ios` 成功
- [ ] 8.4 `dotnet build -f net9.0-android` 成功
- [ ] 8.5 既存の Native iOS / Android テストが影響を受けず全成功

## 完了条件

- 全タスクのチェックボックスが完了している
- `maui-bridge` capability の全 Scenario が通る
- iOS / Android Bridge モジュールがそれぞれ独立してビルド・テスト成功する
- MAUI バインディングプロジェクト 2 つが `dotnet build` で警告なし成功する
- `docs/maui-bindings.md` が完成している
