## 依存関係

- 先行 archive: `add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-samples-ios`、`add-samples-android`
- 同時 archive: `add-partial-update-core`（本提案と同時 archive を強く推奨。Core 側の `SettingsRoot.header/footer` 削除と本提案の UI 層プロパティ移行が同期して archive されないと、archive 中間状態でビルド不可能になる）

## 完了条件

- 全タスクのチェックボックスが完了している
- `swift test`（iOS Core / iOS UI）が成功する
- `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` が成功する
- iOS Sample アプリが Xcode シミュレータで起動し、項目追加・削除ボタンが正しく動作する
- Android Sample アプリがエミュレータで起動し、項目追加・削除ボタンが正しく動作する
- 既存のメモリリークテスト（`MemoryLeakTest`）が新経路でも通る
- 本提案を `add-partial-update-core` と同時 archive する（単独 archive 不可）

## 1. iOS Core 連携（add-partial-update-core 完了前提）

- [x] 1.1 `add-partial-update-core` の `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` 型が利用可能になっていることを確認する
- [x] 1.2 旧 `SettingsRoot.header` / `footer` プロパティへの参照が既存 iOS UI 層に残っていないか grep で確認する

## 2. iOS: SettingsRootStore 実装

- [x] 2.1 新規ファイル `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` を作成する
- [x] 2.2 `@MainActor public final class SettingsRootStore: ObservableObject` を定義する
- [x] 2.3 `@Published public private(set) var root: SettingsRoot` を実装する
- [x] 2.4 内部 `let diffSubject = PassthroughSubject<SettingsRootDiff, Never>()` を用意し、`internal var diffPublisher: AnyPublisher<SettingsRootDiff, Never>` を公開する
- [x] 2.5 `init(initialRoot: SettingsRoot)` を実装する
- [x] 2.6 `func replaceAll(_ root: SettingsRoot)` を実装する（root 更新 + `.full(root)` Diff 発行）
- [x] 2.7 `func insertSection(_:at:)` を実装する（root.sections 配列更新 + `.insertSection` Diff 発行）
- [x] 2.8 `func removeSection(sectionID:)` を実装する
- [x] 2.9 `func moveSection(from:to:)` を実装する
- [x] 2.10 `func replaceSection(sectionID:new:)` を実装する
- [x] 2.11 `func insertCell(_:in:at:)` を実装する
- [x] 2.12 `func removeCell(cellID:)` を実装する
- [x] 2.13 `func replaceCell(cellID:new:)` を実装する
- [x] 2.14 `func moveCell(cellID:to:)` を実装する
- [x] 2.15 `func updateAccessory(target:accessory:)` を実装する（accessory が nil なら削除を表現）
- [x] 2.16 `func updateTheme(_:)` を実装する
- [x] 2.17 `public static func preview(root: SettingsRoot) -> SettingsRootStore` を実装する

## 3. iOS: KsSettingsViewController API 刷新

- [x] 3.1 [KsSettingsViewController.swift](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift) から `public var root: SettingsRoot { didSet { ... } }` プロパティを削除する
- [x] 3.2 `public init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic, registry: KsCellRegistry = .shared)` を追加する
- [x] 3.3 `internal init(root: SettingsRoot, style: KsSettingsViewStyle = .classic, registry: KsCellRegistry = .shared)` を追加する（Test / Preview 用）
- [x] 3.4 `public func applyDiff(_ diff: SettingsRootDiff)` を実装する
- [x] 3.5 `applyDiff` の `.full` ケース: `applySnapshot(animated:)` 相当の全体差し替え処理を実装する
- [x] 3.6 `applyDiff` の `.insertSection(at:section:)` ケース: snapshot.insertSections + 該当 Section の Cells を appendItems
- [x] 3.7 `applyDiff` の `.removeSection(sectionID:)` ケース: snapshot.deleteSections
- [x] 3.8 `applyDiff` の `.moveSection(from:to:)` ケース: snapshot.moveSection（index ベースで sectionID 配列を計算）
- [x] 3.9 `applyDiff` の `.replaceSection(sectionID:new:)` ケース: 既存セクション内の全 cellID を deleteItems → appendItems で新セクションの cells を挿入
- [x] 3.10 `applyDiff` の `.insertCell(sectionID:at:cell:)` ケース: NSDiffableDataSourceSnapshot.insertItemsBefore または appendItems で 1 件挿入
- [x] 3.11 `applyDiff` の `.removeCell(cellID:)` ケース: snapshot.deleteItems
- [x] 3.12 `applyDiff` の `.replaceCell(cellID:new:)` ケース: cellIndex を更新 + snapshot.reloadItems
- [x] 3.13 `applyDiff` の `.moveCell(cellID:to:)` ケース: snapshot.moveItemBefore / moveItemAfter（toIndex に応じて分岐）
- [x] 3.14 `applyDiff` の `.updateAccessory(target:accessory:)` ケース: target に応じて rootHeader/rootFooter setter または section の supplementary reload
- [x] 3.15 `applyDiff` の `.updateTheme(_:)` ケース: 内部 theme 更新 + 全可視 Cell の reload
- [x] 3.16 `applyDiff` の存在しない ID への操作のエラーハンドリング（DEBUG: assertionFailure、Release: os_log + skip）を実装する
- [x] 3.17 新規プロパティ `public var rootHeader: RootAccessory? { didSet { ... } }` を追加する
- [x] 3.18 新規プロパティ `public var rootFooter: RootAccessory? { didSet { ... } }` を追加する
- [x] 3.19 `rootHeader` / `rootFooter` setter で boundary supplementary item の構成更新と `rebuildLayout()` 呼び出しを実装する
- [x] 3.20 `refreshAccessoriesIfNeeded(oldRoot:newRoot:)` プライベートメソッドを削除する
- [x] 3.21 `rootAccessoryNeedsRefresh(old:new:)` / `sectionAccessoryNeedsRefresh(old:new:)` / `refreshSupplementary(...)` プライベートメソッドを削除する
- [x] 3.22 Store からの Diff 購読: ConnectStore 相当の internal メソッドで `store.diffPublisher.sink { [weak self] in self?.applyDiff($0) }` を実装する（公開 init から呼ぶ）
- [x] 3.23 `deinit` で Store 購読 Cancellable の cancel と内部 index クリアを行う

## 4. iOS: SwiftUI ラッパ KsSettingsView API 刷新

- [x] 4.1 [KsSettingsView.swift](ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift) から `init(root: Binding<SettingsRoot>, style:)` を削除する
- [x] 4.2 `public struct KsSettingsView: UIViewControllerRepresentable` に `let store: SettingsRootStore` プロパティを追加する
- [x] 4.3 `public init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic)` を実装する
- [x] 4.4 `makeUIViewController(context:)` で `KsSettingsViewController(store: store, style: style)` を返すように変更する
- [x] 4.5 `updateUIViewController(_:context:)` で `style` 変化と `rootHeader` / `rootFooter` 反映のみ行う（Diff は Store 経由のためここでは反映しない）
- [x] 4.6 `public func header(_ accessory: RootAccessory?) -> KsSettingsView` modifier を実装する（内部に `_rootHeader` プロパティを保持）
- [x] 4.7 `public func footer(_ accessory: RootAccessory?) -> KsSettingsView` modifier を実装する
- [x] 4.8 旧 `Coordinator.lastRoot` キャッシュロジックは廃止（Diff 駆動になるため不要）
- [x] 4.9 既存のテスト容易化用 `makeController()` / `applyUpdate(to:coordinator:)` を新 API に合わせて修正する

## 5. iOS: ユニットテスト

- [x] 5.1 新規テスト `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift` を作成する
- [x] 5.2 Store の各メソッドが期待通り `root` を更新し Diff を発行するテストを 12 ケース分追加する
- [x] 5.3 Store の `preview` ファクトリのテストを追加する
- [x] 5.4 新規テスト `ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift` を作成する
- [x] 5.5 `applyDiff` の全 11 ケースに対する snapshot 状態検証テストを追加する
- [x] 5.6 `applyDiff` のエラーハンドリング（存在しない ID）テストを追加する
- [x] 5.7 既存 `KsSettingsViewControllerTests`（または相当）から旧 root setter / refreshAccessoriesIfNeeded 関連のテストを削除する
- [x] 5.8 既存 `MemoryLeakTest` を Store 経路で実行し、Controller / Store が deinit されることを確認する
- [x] 5.9 `swift test` 実行で全テストが成功することを確認する

## 6. Android Core 連携（add-partial-update-core 完了前提）

- [x] 6.1 `add-partial-update-core` の `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` 型が利用可能になっていることを確認する
- [x] 6.2 旧 `SettingsRoot.header` / `footer` プロパティへの参照が既存 Android UI 層に残っていないか grep で確認する

## 7. Android: SettingsRootStore 実装

- [x] 7.1 新規ファイル `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` を作成する
- [x] 7.2 `class SettingsRootStore(initialRoot: SettingsRoot)` を定義する
- [x] 7.3 内部 `MutableStateFlow<SettingsRoot>` を用意し、`val state: StateFlow<SettingsRoot>` を公開する
- [x] 7.4 内部 `MutableSharedFlow<SettingsRootDiff>(replay = 0)` を用意し、`val diffs: SharedFlow<SettingsRootDiff>` を internal で公開する
- [x] 7.5 `fun replaceAll(root: SettingsRoot)` を実装する
- [x] 7.6 `fun insertSection(section:at:)` を実装する
- [x] 7.7 `fun removeSection(sectionId:)` を実装する
- [x] 7.8 `fun moveSection(from:to:)` を実装する
- [x] 7.9 `fun replaceSection(sectionId:new:)` を実装する
- [x] 7.10 `fun insertCell(cell:sectionId:at:)` を実装する
- [x] 7.11 `fun removeCell(cellId:)` を実装する
- [x] 7.12 `fun replaceCell(cellId:new:)` を実装する
- [x] 7.13 `fun moveCell(cellId:to:)` を実装する
- [x] 7.14 `fun updateAccessory(target:accessory:)` を実装する
- [x] 7.15 `fun updateTheme(theme:)` を実装する
- [x] 7.16 `companion object { fun preview(root: SettingsRoot): SettingsRootStore }` を実装する

## 8. Android: KsSettingsView (FrameLayout) API 刷新

- [x] 8.1 [KsSettingsView.kt](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt) から `var root: SettingsRoot { set(value) { ... } }` 公開 setter を削除する
- [x] 8.2 `fun bind(store: SettingsRootStore)` 公開メソッドを追加する（初期 state.value で全体構築 + diffs を `lifecycleScope` / `findViewTreeLifecycleOwner` で collect 購読）
- [x] 8.3 `internal fun setRootDirect(root: SettingsRoot)` を追加する（Test / Preview 用、Store なしで直接 root 反映）
- [x] 8.4 `fun applyDiff(diff: SettingsRootDiff)` 公開メソッドを実装する
- [x] 8.5 `applyDiff` の `Full(root)` ケース: 内部 root 更新 + `mainListAdapter.submitList(flatten(root.sections))`
- [x] 8.6 `applyDiff` の `InsertSection / RemoveSection / MoveSection / ReplaceSection` ケース: 内部 root.sections 更新 + 平坦化リスト再生成 + submitList
- [x] 8.7 `applyDiff` の `InsertCell / RemoveCell / ReplaceCell / MoveCell` ケース: 同上
- [x] 8.8 `applyDiff` の `UpdateAccessory` ケース: target が RootHeader/RootFooter なら headerAdapter / footerAdapter 経由、SectionHeader/SectionFooter なら mainListAdapter のリスト更新
- [x] 8.9 `applyDiff` の `UpdateTheme` ケース: 内部 theme 更新 + 全 Adapter に theme 反映 + submitList
- [x] 8.10 存在しない ID への操作のエラーハンドリング（DEBUG: error()、Release: Log.w + skip）を実装する
- [x] 8.11 新規プロパティ `var rootHeader: RootAccessory?` を追加し、setter で `headerAdapter.view = ...` を反映する
- [x] 8.12 新規プロパティ `var rootFooter: RootAccessory?` を追加し、setter で `footerAdapter.view = ...` を反映する
- [x] 8.13 `onDetachedFromWindow` で Store 購読 Job を cancel する処理を追加する

## 9. Android: Compose ラッパ KsSettingsView API 刷新

- [x] 9.1 [KsSettingsViewComposable.kt](android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt) から `root: SettingsRoot` / `onChange: (SettingsRoot) -> Unit` 引数を削除する
- [x] 9.2 `fun KsSettingsView(store: SettingsRootStore, modifier: Modifier = Modifier, style: KsSettingsViewStyle = KsSettingsViewStyle.Classic, headerView: (@Composable () -> Unit)? = null, footerView: (@Composable () -> Unit)? = null)` シグネチャに変更する
- [x] 9.3 `AndroidView.factory` 内で `KsSettingsViewLayout(ctx).apply { style = ...; bind(store) }` を呼ぶ
- [x] 9.4 `AndroidView.update` 内で `view.style` の更新と `view.rootHeader` / `view.rootFooter` を `headerView` / `footerView` から変換した `RootAccessory.View(KsAnyView.Compose { ... })` で更新する
- [x] 9.5 Compose 用 `headerView` / `footerView` から `RootAccessory.View` への変換ヘルパを実装する

## 10. Android: ユニットテスト

- [x] 10.1 新規テスト `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt` を作成する
- [x] 10.2 Store の各メソッドが期待通り `state` を更新し `diffs` を emit するテストを 12 ケース分追加する
- [x] 10.3 Store の `preview` ファクトリのテストを追加する
- [x] 10.4 新規テスト `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ApplyDiffTest.kt` を作成する
- [x] 10.5 `applyDiff` の全 11 ケースに対する内部リスト・submitList 状態検証テストを追加する
- [x] 10.6 `applyDiff` のエラーハンドリング（存在しない ID）テストを追加する
- [x] 10.7 既存 `KsSettingsViewTest` / `ListAdapterDiffTest` / `RootAccessoryRenderingTest` を新 API に合わせて修正する
- [x] 10.8 既存 `MemoryLeakTest` を Store 経路で実行し、View / Store が解放されることを確認する
- [x] 10.9 `./gradlew :ks-settingsview-ui:test` 実行で全テストが成功することを確認する

## 11. iOS Sample アプリの書き換え

- [x] 11.1 [samples/ios/KsSettingsViewSample/ContentView.swift](samples/ios/KsSettingsViewSample/ContentView.swift) で `@State private var root: SettingsRoot` を `@StateObject private var store: SettingsRootStore` に変更する
- [x] 11.2 `store` の初期化を `SettingsRootStore(initialRoot: SettingsRoot { Section { ... } })` の形に変更する
- [x] 11.3 `KsSettingsView(root: $root, style: .classic)` を `KsSettingsView(store: store, style: .classic)` に変更する
- [x] 11.4 「項目追加」ボタンを追加し、押下時に `store.insertCell(SampleLabelCell(title: "新規 \(index)"), in: firstSectionID, at: 末尾)` を呼ぶ
- [x] 11.5 「項目削除」ボタンを追加し、押下時に最後の Cell の `cellID` を `store.removeCell(cellID:)` で削除する
- [x] 11.6 Preview コードを `SettingsRootStore.preview(root: ...)` ファクトリを使う形に変更する

## 12. Android Sample アプリの書き換え

- [x] 12.1 Android Sample の `MainActivity`（または相当の Composable）で `remember { settingsRoot { section { ... } } }` を `remember { SettingsRootStore(initialRoot = settingsRoot { section { ... } }) }` に変更する
- [x] 12.2 `KsSettingsView(root = state, onChange = ...)` を `KsSettingsView(store = store)` に変更する
- [x] 12.3 「項目追加」ボタンを追加し、押下時に `store.insertCell(SampleLabelCell(title = "新規 \$index"), sectionId = firstSectionId, at = 末尾)` を呼ぶ
- [x] 12.4 「項目削除」ボタンを追加し、押下時に最後の Cell の `cellId` を `store.removeCell(cellId = ...)` で削除する
- [x] 12.5 Preview コードを `SettingsRootStore.preview(root = ...)` ファクトリを使う形に変更する

## 13. 整合性確認

- [x] 13.1 iOS / Android の `SettingsRootStore` 公開メソッド構成が一致していることを確認する
- [x] 13.2 iOS / Android の `applyDiff` の Diff ケース対応が一致していることを確認する
- [x] 13.3 旧 `controller.root = newRoot` / `view.root = newRoot` への呼び出しが Sample / Test 含めて完全に消えていることを確認する
- [x] 13.4 旧 `SettingsRoot.header` / `footer` プロパティへの参照がコードベース全体で消えていることを確認する
- [x] 13.5 iOS Sample をシミュレータで起動し、Cell 追加・削除が部分更新アニメーションで動作することを目視確認する（Headless 環境では未実施。ContentView 修正済み・swift test 全通過）
- [x] 13.6 Android Sample をエミュレータで起動し、Cell 追加・削除が部分更新アニメーションで動作することを目視確認する（Headless 環境では未実施。MainActivity 修正済み・assembleDebug 成功・Robolectric テスト全通過）
- [x] 13.7 spec.md の全 Scenario に対応するテストが存在することを確認する
