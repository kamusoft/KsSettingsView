## 参考実装

本変更提案は、`AiForms.Maui.NativeCollectionView` の `NotifyCollectionChangedAction` ベースの部分更新パターンを iOS / Android Native UI 層に反映する。実装着手前に以下を熟読すること。

- `../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/NativeViewProviderOfSectionModel.cs` — iOS の `NSDiffableDataSourceSnapshot` 部分操作（`InsertItemsBefore` / `DeleteItems` / `MoveItemBefore` / `MoveItemAfter` / `ReloadItems` / `AppendItems`）
- `../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/NativeViewProviderOfSectionModel.cs` — Android の `ListAdapter.SubmitList` ベースの差分更新（内部 List 変更後に submit）
- `openspec/drafts/02-partial-update-design.md` — 探索モードでの議論結果まとめ

## Context

archive 済み `add-settings-view-ios-ui` / `add-settings-view-android-ui` で確立した Native UI 層は、`SettingsRoot` 全体差し替え方式（`controller.root = newRoot` / `view.root = newRoot`）で内部 snapshot を再構築する設計だった。これは利用者が変更を「全体スナップショット」として表現することを強制し、大量データ・高頻度更新ユースケースでパフォーマンス問題を起こす。

本提案は、`add-partial-update-core` で導入される `SettingsRootDiff` 型を活用し、Native UI 層に `applyDiff(_:)` API を追加することで部分更新を可能にする。さらに `SettingsRootStore` という ObservableObject / StateFlow ベースのストア抽象を導入し、SwiftUI / Compose 利用者が「Store のメソッド呼び出し」という宣言的 API で部分更新を表現できるようにする。

`SettingsRoot.header/footer` プロパティが Core 層から削除されるため、Root H/F は UI 層プロパティ（`KsSettingsViewController.rootHeader` / `KsSettingsView.headerView` 等）として再導入する。SwiftUI ラッパでは `.header(...)` / `.footer(...)` modifier、Compose ラッパでは `headerView` / `footerView` 引数として公開する。

## Goals / Non-Goals

**Goals:**

- `SettingsRootStore`（Swift / Kotlin）の新規導入
- `KsSettingsViewController.applyDiff(_:)` API の追加（iOS）
- `KsSettingsView.applyDiff(_:)` API の追加（Android）
- 既存 `controller.root` / `view.root` 公開 setter の削除
- SwiftUI ラッパ `KsSettingsView` を Store 方式に刷新
- Compose ラッパ `KsSettingsView` を Store 方式に刷新
- Root H/F を UI 層プロパティとして再導入
- `refreshAccessoriesIfNeeded` 関連ロジックの完全削除
- Preview / Test 用ヘルパ（`Store.preview(root:)` / `internal init(root:)`）の整備
- iOS / Android Sample アプリの Store 方式書き換え

**Non-Goals:**

- Core 層の Diff 型定義 → `add-partial-update-core`（依存先）
- MAUI Bridge / Handler の部分更新対応 → 進行中提案修正（後続）
- Diff の連続適用時のアニメーション結合 → Native フレームワーク任せ（iOS は連続 `apply`、Android は `submitList` の DiffUtil に委譲）
- Section 間 Cell 移動 API → 現時点では非対応（`removeCell` + `insertCell` で表現）
- Diff のバッチング API（`store.batch { ... }`）→ NativeCollectionView 流儀に倣い、1 操作 = 1 Diff = 1 apply の設計に統一

## Decisions

### Decision 1: SettingsRootStore は ObservableObject / StateFlow ベース

**選択**: Swift では `@MainActor public final class SettingsRootStore: ObservableObject` + `@Published public private(set) var root: SettingsRoot`、Kotlin では `class SettingsRootStore` + `val state: StateFlow<SettingsRoot>` + 内部 `MutableSharedFlow<SettingsRootDiff>` で Diff 発行する設計とする。

**理由**:

- SwiftUI / Compose の最も自然な状態管理パターンに合致
- 利用者の学習コストが低い（標準 API）
- ViewModel パターンへの統合が容易（`StateObject` / `viewModel.store` で扱える）
- `root` を `private(set)` にすることで、外部からの直接代入を防ぎ、Store メソッド経由のみで更新を強制

**代替案**:

- Combine `CurrentValueSubject` / Kotlin `Channel` のみ：標準的でなく、SwiftUI / Compose との統合が手動
- 値型 `SettingsRoot` を直接公開（@Binding 互換）：探索モードで「Store 方式オンリー」と決定済み

### Decision 2: applyDiff は 1 操作 = 1 apply、batch なし

**選択**: `applyDiff(_ diff: SettingsRootDiff)` は単一 Diff を受け取り、iOS は `NSDiffableDataSource.apply(snapshot, animatingDifferences: true)` を 1 回、Android は `ListAdapter.submitList(list)` を 1 回呼ぶ。`store.batch { ... }` のような複数操作を 1 つのアニメーションに統合する API は提供しない。

**理由**:

- `AiForms.Maui.NativeCollectionView` 流儀と一致（`AddItems` / `RemoveItems` などが各々 `ApplySnapshot` を 1 回呼ぶ）
- 設計と実装がシンプル
- 設定画面ユースケースでは高頻度連続更新はまれで、1 操作 1 apply のオーバーヘッドは無視できる
- iOS の `NSDiffableDataSource.apply` は内部で複数の `delete` / `insert` をまとめて単一アニメーションにする（snapshot ベースのため）
- Android の `ListAdapter` も内部 `AsyncListDiffer` で複数 `submitList` 呼び出しを最適化

**代替案**:

- `store.batch { ... }` をサポート、Native に `applyDiffs([SettingsRootDiff])` を渡して `performBatchUpdates` で統合：実装複雑度が上がる、設計判断 7 で却下済み

### Decision 3: SettingsRootStore は UI 層モジュールに配置

**選択**: `SettingsRootStore` は Core モジュールではなく **UI 層モジュール**（`KsSettingsViewUI` / `ks-settingsview-ui`）に配置する。`KsSettingsViewController` / `KsSettingsView`（Android）が Store の `diff` 出力を購読して `applyDiff` を呼ぶ統合経路を内包する。

**理由**:

- Store は「UI 状態管理」の責務であり、Core（プラットフォーム非依存ドメインモデル）の責務ではない
- Swift の `ObservableObject` / Kotlin の `StateFlow` は UI フレームワーク依存（Combine / coroutines）であり、Core モジュールに置くと Core が UI に依存する逆転が発生する
- UI 層に置くことで Store ↔ Controller / View の統合をシームレスに実装可能

**代替案**:

- Core に Store を置く：Combine / coroutines への依存が Core に侵入、設計責務違反
- Store を別モジュール（`KsSettingsViewStore` / `ks-settingsview-store`）として独立：モジュール数が増え、利用者の import が冗長

### Decision 4: Preview / Test 用ヘルパは公開ファクトリ + internal init の二段構え

**選択**:

- `SettingsRootStore.preview(root:)` を `public static func`（Swift）/ `companion object fun`（Kotlin）として提供。Preview / Test で `@StateObject` / `remember` 不要のショートカットとして利用
- `KsSettingsViewController.init(root: SettingsRoot, ...)` を `internal` として提供。Test / Snapshot test 用に Store 抽象を介さず直接 SettingsRoot を渡せる
- `KsSettingsView`（Android）に `internal fun setRootDirect(root: SettingsRoot)` を提供。Test 用

**理由**:

- 利用者から見て公開 API は Store のみで一貫
- Preview コード簡略化（`#Preview { KsSettingsView(store: .preview(root: ...)) }`）
- Snapshot test は Store 抽象を経由せず純粋関数的に書ける

**代替案**:

- Preview / Test も Store 必須：Preview コードが冗長、Snapshot test の独立性が損なわれる
- `controller.root` を `internal` で残す：内部から直接 SettingsRoot を渡せるが、API としての一貫性が下がる

### Decision 5: Root H/F は UI 層プロパティとして再導入（View modifier 化）

**選択**:

- `KsSettingsViewController.rootHeader: RootAccessory?` / `rootFooter: RootAccessory?` を公開プロパティとして追加
- SwiftUI ラッパ `KsSettingsView` に `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier を追加
- `KsSettingsView`（Android）の Root H/F プロパティは `var rootHeader: RootAccessory?` / `var rootFooter: RootAccessory?` として既存 `RootHeaderFooterAdapter` 経路を維持
- Compose ラッパは `KsSettingsView(store:, headerView:, footerView: )` の引数として `(@Composable () -> Unit)?` を受け取る
- 旧 `SettingsRoot.header` / `footer` から渡すコードは不可能になる（Core 側で削除済み）

**理由**:

- 探索モードで「Root H/F は View プロパティ化」と決定済み
- 旧 AiForms.Maui.SettingsView / NativeCollectionView の `HeaderView` / `FooterView` BindableProperty と思想一致
- View（描画責務）と Data（ドメインモデル）の責務分離

**代替案**:

- Root H/F を Store のプロパティとして公開（`store.rootHeader = ...`）：Store の責務肥大、SwiftUI の宣言的 modifier スタイルから外れる
- SettingsRoot.header / footer を維持：探索モードで却下済み

### Decision 6: Compose ラッパは Store 引数 + headerView/footerView 引数

**選択**:

```kotlin
@Composable
fun KsSettingsView(
    store: SettingsRootStore,
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    headerView: (@Composable () -> Unit)? = null,
    footerView: (@Composable () -> Unit)? = null,
)
```

`headerView` / `footerView` は `@Composable () -> Unit` で受け、内部で `KsAnyView.Compose(...)` に変換して `view.rootHeader` / `rootFooter` に設定する。

**理由**:

- Compose の慣用句に従う（slot-based composition）
- `RootAccessory.Text` を渡すには `headerView = { Text("プロフィール") }` で表現可能
- 内部実装は `KsAnyView.Compose` 統一でシンプル

**代替案**:

- `headerAccessory: RootAccessory?` / `footerAccessory: RootAccessory?`：Compose 利用者から見て型が冗長
- Modifier 化（`.ksRootHeader(...)`）：Compose には Modifier extension で View 設定する慣習がない

### Decision 7: applyDiff 適用時のエラーハンドリング

**選択**: Diff 適用時に対象の `sectionID` / `cellID` が内部状態に存在しない場合：

- DEBUG ビルド: `assertionFailure(...)`（Swift）/ `error(...)`（Kotlin） で即座にクラッシュ
- Release ビルド: 黙ってスキップ + `os_log` / `Log.w` でログ出力

**理由**:

- 探索モードでの決定 9 を反映
- 既存 `KsSettingsViewController` の `assertionFailure("KsCellRegistry: no renderer registered for ...")` パターンを踏襲
- 開発時の誤り検出（早期発見）と本番時の堅牢性（クラッシュ回避）の両立

**代替案**:

- 常に例外を throw：本番でクラッシュリスク
- エラーコールバック提供：Store API が複雑化、設計判断 9 で却下済み

### Decision 8: refreshAccessoriesIfNeeded 完全削除

**選択**: `KsSettingsViewController` の `refreshAccessoriesIfNeeded(oldRoot:newRoot:)` / `rootAccessoryNeedsRefresh` / `sectionAccessoryNeedsRefresh` / `refreshSupplementary` プライベートメソッド群を完全削除する。

**理由**:

- `@Binding<SettingsRoot>` API が削除されるため、root 全体差し替え経路が消える
- `KsAnyView` を含む accessory の中身変化は Store API の `updateAccessory(target:, accessory:)` で利用者が明示的に通知する設計に統一
- 推測 refresh の複雑なロジックがなくなり、コードベースがシンプル化

**代替案**:

- 推測 refresh を残し、Store 経由でも呼ぶ：コード重複、責務不明瞭
- 削除しつつ Store 内部で Diff 比較して自動 refresh：Store の責務肥大、暗黙的挙動

### Decision 9: Android 側の Section / Cell ID 型は String 統一

**選択**: Android `SettingsRootDiff` の `sectionId` / `cellId` は `String` 型（Core 側決定）。`mainListAdapter.submitList(...)` で渡す `CellListItem` の ID も既存 `sectionId: String` と整合させる。

**理由**:

- `add-partial-update-core` で Kotlin 側は `String` ID 採用済み
- 既存 `CellListItem.SectionHeader(sectionId, ...)` と整合
- Android の `ListAdapter` は ID ベースの diff には適合（`AreItemsTheSame` で ID 比較）

**代替案**:

- UUID / Long ID：iOS 側との型差異が広がる、Core 仕様と矛盾

## Risks / Trade-offs

- **リスク**: `applyDiff` の 11 ケース実装漏れによるバグ
  - **緩和策**: 全 11 ケースに対するユニットテストを iOS / Android 両方で書く。Snapshot/list 内部状態の検証を行う
- **リスク**: Store と Controller の循環参照によるメモリリーク
  - **緩和策**: SwiftUI Coordinator の Subscription は `[weak controller]` キャプチャ、`DisconnectHandler` / `deinit` 相当で確実に解放。既存 `MemoryLeakTest` を本提案の経路でも実行
- **リスク**: 既存 archive 済 spec への大規模 MODIFIED でレビュー困難
  - **緩和策**: spec は Requirement 単位で MODIFIED を行い、変更後の全文を含める。Section H/F は維持されることを明示
- **リスク**: `@Binding<SettingsRoot>` 廃止によりサンプルコードが大幅書き換え
  - **緩和策**: Sample コードは本提案で同時修正。動的更新デモを追加することで Store 方式のメリットを示す
- **リスク**: `KsAnyView` を含む accessory の中身変化を利用者が明示通知しないと描画が古いまま
  - **緩和策**: README / ドキュメントで明示的に説明、Sample コードに「KsAnyView 中身変化のときは必ず `store.updateAccessory(...)` を呼ぶ」コメントを記載

## Migration Plan

本提案は archive 済み複数 spec の MODIFIED であり、`@Binding<SettingsRoot>` API 削除は破壊的変更。archive 順序：

1. `add-monorepo-foundation`（archive 済）
2. `add-settings-view-core`（archive 済、`add-partial-update-core` で MODIFIED）
3. `add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-samples-ios` / `add-samples-android`（archive 済、本提案で MODIFIED）
4. `add-partial-update-core` archive
5. **本提案**（`add-partial-update-native`）archive ※ `add-partial-update-core` と同時に archive する運用を推奨

利用側コード（Sample および進行中 MAUI 提案）の移行手順：

- 旧 `SettingsRoot(header:, footer:, sections:, theme:)` 構築 → `SettingsRootStore` 初期化 + Store メソッド or `KsSettingsView(...).header(...).footer(...)` modifier
- 旧 `@State private var root: SettingsRoot` → `@StateObject private var store: SettingsRootStore`
- 旧 `root = newRoot` → `store.replaceAll(newRoot)`
- 旧 `root.sections[0].cells.append(...)` → `store.insertCell(_:in:at:)`

## Open Questions

- **Swift `any KsCell` の Diff payload 経由でのアイデンティティ**: `insertCell` / `replaceCell` の `cell: any KsCell` は内部 `cellIndex: [KsCellID: any KsCell]` に登録される。`replaceCell(cellID:, new:)` で新しい Cell の `KsCellID` が古い ID と一致しない場合の挙動を `applyDiff` 実装で明示する必要あり（推奨: 古い ID で内部マップを更新、Snapshot の `reloadItems` で再描画）
- **Compose の `headerView` / `footerView` から内部 View への変換タイミング**: `KsAnyView.Compose { ... }` で包んだものを Native `KsSettingsView.rootHeader = RootAccessory.View(...)` に流すが、Compose Recomposition のたびに新しい `KsAnyView` インスタンスができる。`store.updateAccessory(...)` を介さないと中身変化が反映されないため、Compose ラッパ内で `LaunchedEffect(headerView)` 等で変化検知して `view.rootHeader = ...` を再代入する設計を検討
- **Sample アプリの動的更新デモ範囲**: 「項目追加」「項目削除」「項目並び替え」のどこまで Sample で公開するか。本提案では最小限「追加」「削除」のみを Sample に含め、並び替え・移動は別途ドキュメントで案内する方針を採用予定
