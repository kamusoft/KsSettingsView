## Why

`add-partial-update-core` で導入する `SettingsRootDiff` 型と `SettingsAccessory` 型を、Native UI 層（iOS `KsSettingsViewUI` / Android `ks-settingsview-ui` + `ks-settingsview-compose`）に反映し、**部分更新可能な API** を SwiftUI / Compose 利用者へ提供する。本提案は `AiForms.Maui.NativeCollectionView` の `OnCellCollectionChanged` パターンに倣い、`SettingsRootStore` という ObservableObject / StateFlow ベースのストア型を導入し、`insertCell` / `removeCell` / `replaceCell` / `moveCell` などのメソッド呼び出しを Native の `applyDiff(_:)` に変換することで、root 全体再構築を回避する。

加えて、`add-partial-update-core` で `SettingsRoot.header` / `footer` プロパティが削除されたため、Root H/F は UI 層プロパティとして再導入する。Swift では `KsSettingsViewController.rootHeader` / `rootFooter`、SwiftUI ラッパでは `.header(...)` / `.footer(...)` modifier 、Android では `KsSettingsView.headerView` / `footerView`、Compose ラッパでは `headerView` / `footerView` 引数として公開する。

これに伴い、既存の `@Binding<SettingsRoot>` ベース API と `refreshAccessoriesIfNeeded` の推測 refresh ロジックは廃止し、Store 方式に一本化する。

## What Changes

- **BREAKING**: `KsSettingsViewController.root: SettingsRoot` 公開プロパティ（setter）を削除する
  - Store 経由でのみ root を操作可能とする
- **BREAKING**: `KsSettingsView`（Android FrameLayout）の `var root: SettingsRoot` 公開プロパティ（setter）を削除する
  - 同上
- **BREAKING**: SwiftUI ラッパ `KsSettingsView` の `init(root: Binding<SettingsRoot>, style:)` を削除し、`init(store: SettingsRootStore, style:)` に置き換える
- **BREAKING**: Compose ラッパ `@Composable fun KsSettingsView(...)` の `root: SettingsRoot` / `onChange` 引数を削除し、`store: SettingsRootStore` 引数に置き換える
- 新規 `SettingsRootStore`（Swift `@MainActor public final class : ObservableObject`、Kotlin `class`）を `KsSettingsViewUI` / `ks-settingsview-ui` に追加
  - 公開メソッド: `replaceAll(_:)`、`insertSection(_:at:)`、`removeSection(sectionID:)`、`moveSection(from:to:)`、`replaceSection(sectionID:new:)`、`insertCell(_:in:at:)`、`removeCell(cellID:)`、`replaceCell(cellID:new:)`、`moveCell(cellID:to:)`、`updateAccessory(target:accessory:)`、`updateTheme(_:)`
  - Swift: `@Published public private(set) var root: SettingsRoot`
  - Kotlin: `val state: StateFlow<SettingsRoot>`、内部 `SharedFlow<SettingsRootDiff>` で diff 発行
  - Preview/Test 用ファクトリ: `SettingsRootStore.preview(root:)`（Swift / Kotlin 共通）
- `KsSettingsViewController` に `public func applyDiff(_ diff: SettingsRootDiff)` API を追加
  - iOS は `NSDiffableDataSourceSnapshot.insertItemsBefore` / `deleteItems` / `moveItemBefore` / `moveItemAfter` / `reloadItems` 等で部分操作
  - 公開 init: `init(store: SettingsRootStore, style:, registry:)`
  - Preview/Test 用 internal init: `init(root: SettingsRoot, style:, registry:)`
- `KsSettingsView`（Android）に `fun applyDiff(diff: SettingsRootDiff)` API を追加
  - 内部 `List<CellListItem>` を変更したのち `mainListAdapter.submitList(...)` を呼ぶ
  - Compose ラッパ用に `internal fun setRootDirect(root: SettingsRoot)` を追加（Test/Preview 用）
- `KsSettingsViewController` に Root H/F 用プロパティを新規追加：
  - `public var rootHeader: RootAccessory?` / `public var rootFooter: RootAccessory?`
  - 既存の `rootHeaderElementKind` / `rootFooterElementKind` の boundary supplementary 配置経路は維持
- `KsSettingsView`（Android）に Root H/F 用プロパティを新規追加：
  - `var headerView: View?` / `var footerView: View?` （`RootHeaderFooterAdapter` 経由で描画）
  - もしくは現状の `RootAccessory?` ベースを保つ場合は `var rootHeader: RootAccessory?` / `var rootFooter: RootAccessory?`
- **削除**: `KsSettingsViewController.refreshAccessoriesIfNeeded(oldRoot:newRoot:)` プライベートメソッドおよび関連の `rootAccessoryNeedsRefresh` / `sectionAccessoryNeedsRefresh` / `refreshSupplementary` を削除する
  - Store 経由の `updateAccessory` Diff で明示的に accessory 更新を表現するため、推測 refresh は不要になる
- SwiftUI ラッパ `KsSettingsView`：
  - `init(store: SettingsRootStore, style:)` に変更
  - `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier を追加
  - `Coordinator` は `Store.diffPublisher` を購読し `controller.applyDiff(_:)` を呼ぶ
- Compose ラッパ `KsSettingsView`：
  - `KsSettingsView(store: SettingsRootStore, modifier:, style:, headerView: (@Composable () -> Unit)?, footerView: (@Composable () -> Unit)?)`
  - `AndroidView` 内で `store.diffs.collect { view.applyDiff(it) }` を起動する
- iOS / Android Sample アプリ：
  - `ContentView` / `MainActivity` を Store 方式に書き換え
  - `SettingsRoot` の DSL 直接代入から `SettingsRootStore` の初期化＋メソッド呼び出しに変更
  - 動的な追加・削除ボタンを Sample に追加し、`store.insertCell(...)` / `store.removeCell(...)` の動作を確認可能にする
- 既存テストの修正：
  - Snapshot 全体差し替えを前提とするテストは `applyDiff` 経路のテストに置き換え
  - メモリリークテスト（`MemoryLeakTest`）は Store 経由のフローでも動作することを確認

## Capabilities

### New Capabilities
（なし。本提案は既存 capability の変更のみ）

### Modified Capabilities

- `settings-view-ios-ui`: `KsSettingsViewController` の root setter 削除、`applyDiff` / `rootHeader` / `rootFooter` 追加、`SettingsRootStore` 新規導入、SwiftUI ラッパの Store 化、`refreshAccessoriesIfNeeded` 削除
- `settings-view-android-ui`: `KsSettingsView` の root setter 削除、`applyDiff` / `headerView` / `footerView` 追加、`SettingsRootStore` 新規導入、Compose ラッパの Store 化
- `samples-ios`: Sample アプリを Store 方式に書き換え、動的更新デモを追加
- `samples-android`: 同上

## Impact

- 影響範囲：
  - 既存 `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の API 刷新（root setter 削除、applyDiff 追加、refreshAccessoriesIfNeeded 削除、rootHeader/rootFooter 追加）
  - 既存 `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` の API 刷新
  - 新規 `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`
  - 既存 `android/ks-settingsview-ui/.../KsSettingsView.kt` の API 刷新
  - 既存 `android/ks-settingsview-compose/.../KsSettingsViewComposable.kt` の API 刷新
  - 新規 `android/ks-settingsview-ui/.../SettingsRootStore.kt`
  - 既存 `samples/ios/KsSettingsViewSample/ContentView.swift` を Store 方式に書き換え
  - 既存 `samples/android/.../MainActivity.kt`（または相当）を Store 方式に書き換え
  - 既存テスト（`KsSettingsViewTest` / `ListAdapterDiffTest` / `RootAccessoryRenderingTest` など）の更新
- 依存：
  - `add-partial-update-core`（先行・同時 archive 推奨）
  - 既存 archive 済 `add-settings-view-core` / `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-samples-ios` / `add-samples-android`
- 後続：
  - 進行中 `add-maui-bridge` / `add-maui-core` 修正: 本提案で確立した `SettingsRootStore` / `applyDiff` の概念を MAUI Bridge / Handler に反映
- リスク：**高**（破壊的変更 + 大規模 API 刷新）
  - **既存 SwiftUI / Compose / MAUI 利用コードの破壊**: 利用者は現状おらず（変更提案中）、Sample コードは本提案で同時修正、進行中 MAUI 提案は別提案で対応
  - **`applyDiff` 実装の網羅性漏れ**: 11 ケース全てを iOS / Android 両方で正しく実装する必要があり、テスト網羅が重要
  - **`SettingsRootStore` の State 同期問題**: Diff 適用と `@Published` / `StateFlow` 更新の整合性、複数連続操作時のアニメーション一体感
  - **メモリリークリスク**: Store と Controller の循環参照に注意（`weak self` パターンを徹底）
  - **`KsAnyView` を含む Accessory の明示更新**: `refreshAccessoriesIfNeeded` を削除するため、`KsAnyView` の中身変化は利用者が `store.updateAccessory(...)` を明示的に呼ぶ必要がある（ドキュメントで明示）
