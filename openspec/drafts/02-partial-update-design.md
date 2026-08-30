# 部分更新設計 - 議論結果まとめ

> 探索モード (`/sdd-explore`) での議論結果。
> 本ドラフトは複数の変更提案 (`add-partial-update-core`, `add-partial-update-native`, MAUI 系修正) の元情報。

## 背景・動機

### 現状の問題

KsSettingsView の現状実装では、`SettingsRoot` 全体差し替え方式を採用しており、以下の問題がある：

1. **iOS (`KsSettingsViewController.applySnapshot`)** および **Android (`KsSettingsView.root setter`)** は、root 代入のたびに O(N) で全 cell をループして snapshot/list を再構築
2. **SwiftUI ラッパ `KsSettingsView` の `updateUIViewController`** は `lastRoot != root` 判定で O(N) の Hashable/Equatable 評価
3. **動的な追加・削除のたびに root 全体を作り直す**必要があり、大量データ (10,000 件以上 + 高頻度更新) で実用上の問題が発生
4. **`refreshAccessoriesIfNeeded`** で `KsAnyView` を含む accessory の中身変化を「推測」で refresh する複雑なロジックがある
5. **進行中の MAUI 提案 (`add-maui-bridge` Decision 2)** も「全体再構築 + setRoot」方針で、AiForms 系 (`AiForms.Maui.NativeCollectionView`) の部分更新へのこだわりを継承していない

### 解決方針

`AiForms.Maui.NativeCollectionView/Platforms/iOS/NativeViewProviderOfSectionModel.cs` および `Platforms/Android/NativeViewProviderOfSectionModel.cs` の実装パターンを参考に、`ObservableCollection.CollectionChanged` ベースの部分更新設計に刷新する。

## 設計決定一覧（議論結果）

| # | 決定事項 |
|---|----------|
| 1 | API は **Store 方式オンリー** (`@Binding<SettingsRoot>` 廃止) |
| 2 | Root Header/Footer は **View プロパティ化** (`SettingsRoot.header/footer` 削除) |
| 3 | MAUI Root H/F は **`SettingsView.HeaderView/FooterView` BindableProperty** (旧 AiForms 互換) |
| 4 | Section Header/Footer は **データ構造として現状維持** (`Section.header/footer` プロパティ残す) |
| 5 | Accessory 型は **`RootAccessory` / `SectionAccessory` を区別保持** |
| 6 | Diff API は **`updateAccessory(target, accessory?)` 統一型** |
| 7 | **batch API なし** (NativeCollectionView 流儀、1 操作 = 1 Diff = 1 apply) |
| 8 | MAUI ObservableCollection は **Handler が CollectionChanged 購読 → Diff DTO 変換 → `Bridge.applyDiff`** |
| 9 | Diff 適用エラーは **DEBUG assert / Release silent skip** |
| 10 | Preview/Test 用に **`Store.preview(root:)` ファクトリ + `internal init(root:)`** |
| 11 | **`refreshAccessoriesIfNeeded` は完全削除** (`@Binding` 廃止に伴い不要) |
| 12 | 3 提案 + MAUI 進行中提案修正、**全て同時に実行** |

## 変更提案の構造

### Change 1: `add-partial-update-core`

**対象 spec**: `settings-view-core` (MODIFIED)

**主要変更**:
- `SettingsRoot.header/footer` プロパティ削除
- `SettingsRootDiff` sealed enum / sealed interface 追加
- `AccessoryTarget` enum 追加 (`.rootHeader`, `.rootFooter`, `.sectionHeader(sectionID)`, `.sectionFooter(sectionID)`)
- `RootAccessory` 型は維持（ただし `SettingsRoot` 外で扱う）

### Change 2: `add-partial-update-native`

**対象 spec**:
- `settings-view-ios-ui` (MODIFIED)
- `settings-view-android-ui` (MODIFIED)
- `samples-ios` (MODIFIED)
- `samples-android` (MODIFIED)

**主要変更**:
- `SettingsRootStore` (Swift/Kotlin) 新規追加
- `applyDiff` API を `KsSettingsViewController` / `KsSettingsView` に追加
- `rootHeader` / `rootFooter` View プロパティ追加
- `@Binding<SettingsRoot>` API 削除
- `refreshAccessoriesIfNeeded` 削除
- Preview/Test 用 `internal init(root: SettingsRoot)` 公開
- Sample コードを Store 方式に更新

**依存**: Change 1

### Change 3: 進行中 MAUI 提案を直接修正

**修正対象**:
- `add-maui-bridge` (proposal / design / spec / tasks)
- `add-maui-core` (proposal / design / spec / tasks)
- `add-maui-cells` (依存変更のみ)
- `add-samples-maui` (依存変更のみ)

**主要変更**:
- `Decision 2` を「全体再構築 + setRoot」→「ObservableCollection 連動 + applyDiff」に変更
- Bridge に `applyDiff(diff)` API 追加
- Bridge に `setRootHeader(view:)` / `setRootFooter(view:)` 追加
- SettingsViewHandler は **旧 AiForms ModelProxy 方式** (CollectionChanged 購読)
- `SettingsView.HeaderView` / `FooterView` BindableProperty 追加
- `SettingsRootDefinition` から `Header` / `Footer` プロパティ削除

## 設計詳細

### SettingsRootDiff 型 (Swift)

```swift
public enum SettingsRootDiff {
    case full(SettingsRoot)
    case insertSection(at: Int, section: Section)
    case removeSection(sectionID: UUID)
    case moveSection(from: Int, to: Int)
    case replaceSection(sectionID: UUID, new: Section)
    case insertCell(sectionID: UUID, at: Int, cell: any KsCell)
    case removeCell(cellID: KsCellID)
    case replaceCell(cellID: KsCellID, new: any KsCell)
    case moveCell(cellID: KsCellID, to: Int)
    case updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)
    case updateTheme(theme: Theme)
}

public enum AccessoryTarget: Hashable {
    case rootHeader
    case rootFooter
    case sectionHeader(sectionID: UUID)
    case sectionFooter(sectionID: UUID)
}

// updateAccessory の accessory は target に応じて RootAccessory または SectionAccessory
// 型安全のため SettingsAccessory enum で内部判別:
public enum SettingsAccessory {
    case root(RootAccessory)
    case section(SectionAccessory)
}
```

### SettingsRootDiff 型 (Kotlin)

```kotlin
sealed interface SettingsRootDiff {
    data class Full(val root: SettingsRoot) : SettingsRootDiff
    data class InsertSection(val index: Int, val section: Section) : SettingsRootDiff
    data class RemoveSection(val sectionId: String) : SettingsRootDiff
    data class MoveSection(val from: Int, val to: Int) : SettingsRootDiff
    data class ReplaceSection(val sectionId: String, val newSection: Section) : SettingsRootDiff
    data class InsertCell(val sectionId: String, val index: Int, val cell: Cell) : SettingsRootDiff
    data class RemoveCell(val cellId: String) : SettingsRootDiff
    data class ReplaceCell(val cellId: String, val newCell: Cell) : SettingsRootDiff
    data class MoveCell(val cellId: String, val toIndex: Int) : SettingsRootDiff
    data class UpdateAccessory(val target: AccessoryTarget, val accessory: SettingsAccessory?) : SettingsRootDiff
    data class UpdateTheme(val theme: Theme) : SettingsRootDiff
}

sealed interface AccessoryTarget {
    object RootHeader : AccessoryTarget
    object RootFooter : AccessoryTarget
    data class SectionHeader(val sectionId: String) : AccessoryTarget
    data class SectionFooter(val sectionId: String) : AccessoryTarget
}
```

### SettingsRootStore API (Swift)

```swift
@MainActor
public final class SettingsRootStore: ObservableObject {
    @Published public private(set) var root: SettingsRoot

    // 部分更新 API
    public func insertSection(_ section: Section, at index: Int)
    public func removeSection(sectionID: UUID)
    public func moveSection(from: Int, to: Int)
    public func replaceSection(sectionID: UUID, new: Section)

    public func insertCell(_ cell: any KsCell, in sectionID: UUID, at index: Int)
    public func removeCell(cellID: KsCellID)
    public func replaceCell(cellID: KsCellID, new: any KsCell)
    public func moveCell(cellID: KsCellID, to index: Int)

    public func updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)
    public func updateTheme(_ theme: Theme)

    public func replaceAll(_ root: SettingsRoot)

    // 内部用: Native View が購読する Diff 発行
    internal var diffPublisher: AnyPublisher<SettingsRootDiff, Never> { ... }

    // Preview/Test 用
    public static func preview(root: SettingsRoot) -> SettingsRootStore
}
```

### SettingsRootStore API (Kotlin)

```kotlin
class SettingsRootStore(initialRoot: SettingsRoot) {
    val state: StateFlow<SettingsRoot>
    internal val diffs: SharedFlow<SettingsRootDiff>

    fun insertSection(section: Section, at: Int)
    fun removeSection(sectionId: String)
    fun moveSection(from: Int, to: Int)
    fun replaceSection(sectionId: String, new: Section)

    fun insertCell(cell: Cell, sectionId: String, at: Int)
    fun removeCell(cellId: String)
    fun replaceCell(cellId: String, new: Cell)
    fun moveCell(cellId: String, to: Int)

    fun updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)
    fun updateTheme(theme: Theme)

    fun replaceAll(root: SettingsRoot)

    companion object {
        fun preview(root: SettingsRoot): SettingsRootStore
    }
}
```

### Native UI API 変更

#### iOS (`KsSettingsViewController`)

```swift
public final class KsSettingsViewController: UIViewController {
    // 公開 init (Store 経由)
    public init(store: SettingsRootStore, style: KsSettingsViewStyle = .classic, registry: KsCellRegistry = .shared)

    // Preview/Test 用 internal init
    internal init(root: SettingsRoot, style: KsSettingsViewStyle = .classic, registry: KsCellRegistry = .shared)

    // 部分更新 API
    public func applyDiff(_ diff: SettingsRootDiff)

    // Root H/F (View プロパティ化、SettingsRoot から削除されたため独立プロパティに)
    public var rootHeader: RootAccessory? { didSet { ... } }
    public var rootFooter: RootAccessory? { didSet { ... } }

    // ✗ public var root: SettingsRoot ← 削除（Store 経由のみ）
    // ✗ refreshAccessoriesIfNeeded ← 削除
}
```

#### Android (`KsSettingsView`)

```kotlin
class KsSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    fun bind(store: SettingsRootStore)  // 公開 API
    internal fun setRootDirect(root: SettingsRoot)  // Test/Preview 用

    fun applyDiff(diff: SettingsRootDiff)

    var headerView: View? = null
        set(value) { ... }
    var footerView: View? = null
        set(value) { ... }

    // ✗ var root: SettingsRoot ← 削除
}
```

#### SwiftUI ラッパ (`KsSettingsView`)

```swift
public struct KsSettingsView: UIViewControllerRepresentable {
    let store: SettingsRootStore
    let style: KsSettingsViewStyle
    let header: RootAccessory?
    let footer: RootAccessory?

    public init(
        store: SettingsRootStore,
        style: KsSettingsViewStyle = .classic
    ) { ... }

    // View プロパティとしての header/footer 設定 (modifier 風)
    public func header(_ accessory: RootAccessory?) -> KsSettingsView
    public func footer(_ accessory: RootAccessory?) -> KsSettingsView

    // 内部実装: Coordinator が Store の diff を購読して controller.applyDiff を呼ぶ
}
```

#### Compose ラッパ (`KsSettingsView`)

```kotlin
@Composable
fun KsSettingsView(
    store: SettingsRootStore,
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    headerView: (@Composable () -> Unit)? = null,
    footerView: (@Composable () -> Unit)? = null,
)
// AndroidView 内で store.diffs を collect して view.applyDiff を呼ぶ
```

### MAUI 側

#### `SettingsView : View`

```csharp
public class SettingsView : View
{
    public static readonly BindableProperty SectionsProperty = BindableProperty.Create(
        nameof(Sections), typeof(IList<Section>), typeof(SettingsView));

    public IList<Section>? Sections { get; set; }

    // 旧 AiForms 互換
    public static readonly BindableProperty HeaderViewProperty = BindableProperty.Create(
        nameof(HeaderView), typeof(View), typeof(SettingsView));
    public View? HeaderView { get; set; }

    public static readonly BindableProperty FooterViewProperty = BindableProperty.Create(
        nameof(FooterView), typeof(View), typeof(SettingsView));
    public View? FooterView { get; set; }

    public static readonly BindableProperty StyleProperty = ...
    public KsSettingsViewStyle Style { get; set; }
}
```

#### `Section : BindableObject`

```csharp
public class Section : BindableObject
{
    public ObservableCollection<CellBase> Cells { get; }

    public static readonly BindableProperty HeaderProperty = BindableProperty.Create(...);
    public SectionAccessory? Header { get; set; }

    public static readonly BindableProperty FooterProperty = BindableProperty.Create(...);
    public SectionAccessory? Footer { get; set; }
}
```

#### `SettingsViewHandler` (旧 AiForms ModelProxy 方式)

```csharp
public partial class SettingsViewHandler : ViewHandler<SettingsView, NativeViewType>
{
    void ConnectHandler()
    {
        // VirtualView.Sections (ObservableCollection<Section>) を購読
        if (VirtualView.Sections is INotifyCollectionChanged sectionsObs)
            sectionsObs.CollectionChanged += OnSectionsCollectionChanged;

        // 各 Section.Cells (ObservableCollection<CellBase>) を購読
        // CellBase.PropertyChanged で個別 cell の変更を Bridge.replaceCell に変換
        // Section.PropertyChanged (Header/Footer) を Bridge.updateAccessory に変換

        // 初期化: Bridge.SetRoot(全体)
        Bridge.SetRoot(BuildRootDTO());
    }

    void OnSectionsCollectionChanged(NotifyCollectionChangedAction action, ...)
    {
        switch (action)
        {
            case Add: Bridge.ApplyDiff(InsertSectionDiff(...)); break;
            case Remove: Bridge.ApplyDiff(RemoveSectionDiff(...)); break;
            case Move: Bridge.ApplyDiff(MoveSectionDiff(...)); break;
            case Replace: Bridge.ApplyDiff(ReplaceSectionDiff(...)); break;
            case Reset: Bridge.SetRoot(BuildRootDTO()); break;  // フォールバック
        }
    }

    // CellBase.PropertyChanged → Bridge.ApplyDiff(ReplaceCellDiff(...))
    // Section.HeaderProperty / FooterProperty 変更 → Bridge.ApplyDiff(UpdateAccessoryDiff(...))
    // SettingsView.HeaderView / FooterView 変更 → Bridge.SetRootHeader(view) / SetRootFooter(view)
}
```

#### Bridge API 変更

```objc
// iOS Bridge
@interface KsSettingsViewBridge : NSObject
- (id<KsSettingsViewController>)makeController:(...);

// 既存 (維持・初期化用)
- (void)setRoot:(KsSettingsRootDTO *)root;
- (void)setStyle:(KsSettingsViewStyleDTO)style;
- (void)setRootHeader:(KsAnyViewDTO *)view;
- (void)setRootFooter:(KsAnyViewDTO *)view;

// 新規 (部分更新)
- (void)applyDiff:(KsSettingsRootDiffDTO *)diff;
```

```kotlin
// Android Bridge
object KsSettingsViewBridge {
    @JvmStatic fun makeView(context: Context, listener: KsCellInteractionListener): ViewType
    @JvmStatic fun setRoot(view: ViewType, root: KsSettingsRootDTO)
    @JvmStatic fun setStyle(view: ViewType, style: KsSettingsViewStyleDTO)
    @JvmStatic fun setRootHeader(view: ViewType, headerView: View?)
    @JvmStatic fun setRootFooter(view: ViewType, footerView: View?)

    // 新規
    @JvmStatic fun applyDiff(view: ViewType, diff: KsSettingsRootDiffDTO)
}
```

## エラーハンドリング方針

- Diff 適用時、存在しない `sectionID` / `cellID` に対する操作:
  - **DEBUG**: `assertionFailure(...)` / `error(...)`
  - **Release**: silent skip + ログ出力
- 既存の `KsSettingsViewController` の `assertionFailure("KsCellRegistry: no renderer registered for ...")` パターンを踏襲

## Preview/Test サポート

### Swift Preview

```swift
#Preview {
    let store = SettingsRootStore.preview(root: SettingsRoot {
        Section { SampleLabelCell(title: "Preview Row") }
    })
    return KsSettingsView(store: store)
}
```

### Swift Snapshot Test

```swift
// internal init を使う
let controller = KsSettingsViewController(root: SettingsRoot { ... })
```

### Compose Preview

```kotlin
@Preview
@Composable
fun Preview() {
    val store = remember { SettingsRootStore.preview(...) }
    KsSettingsView(store = store)
}
```

## 全体フロー図

```
利用者コード（SwiftUI / Compose / MAUI XAML）
   │
   ├─ SwiftUI: @StateObject var store; store.insertCell(...)
   ├─ Compose: remember { SettingsRootStore(...) }; store.insertCell(...)
   └─ MAUI:    section.Cells.Add(newCell)  (ObservableCollection)
                    │
                    ▼
          SettingsViewHandler (MAUI のみ)
          ObservableCollection.CollectionChanged 購読
              → Diff DTO 変換
              → Bridge.applyDiff()
                    │
                    ▼
          SettingsRootStore (Native)
              内部 root 更新
              Diff 発行 → controller.applyDiff(diff)
                    │
                    ▼
          KsSettingsViewController (iOS) / KsSettingsView (Android)
              applyDiff(diff) で部分更新
              - iOS:     NSDiffableDataSourceSnapshot の部分操作
                          (insertItemsBefore, deleteItems, moveItemAfter, reloadItems, appendItems)
              - Android: 内部 CellListItem List 変更 + submitList
                          (DiffUtil が自動差分計算)
```

## NativeCollectionView から学んだパターン

参照: `AiForms.Maui.NativeCollectionView/Platforms/iOS/NativeViewProviderOfSectionModel.cs` (363 行)、`Platforms/Android/NativeViewProviderOfSectionModel.cs` (311 行)

1. **OnCellCollectionChanged**: `switch (e.Action) { Add → AddItems; Remove → RemoveItems; Move → MoveItem; Replace → ReplaceItems; Reset → SetDataSource(全体); }`
2. **iOS の snapshot 部分操作**:
   - `snapshot.InsertItemsBefore(ids, beforeId)` / `snapshot.AppendItems(ids, sectionId)`
   - `snapshot.DeleteItems(ids)`
   - `snapshot.MoveItemBefore(id, beforeId)` / `snapshot.MoveItemAfter(id, afterId)`
   - `snapshot.ReloadItems(ids)` (Replace 時)
3. **Android の submitList 戦略**:
   - 内部 `List<DataSourceItem>` を変更後、`adapter.SubmitList(list)` を呼ぶだけ
   - DiffUtil が自動でバックグラウンド差分計算
4. **HeaderSection/FooterSection 化**:
   - 専用 SectionType (Header/Footer) を持つ DataSourceSection として登録
   - snapshot 構築時に `[HeaderSection, ...DataSourceSections, FooterSection]` で append
5. **`CreateCellCollectionChangedClosure`**:
   - Section ごとに ObservableCollection<TCellModel> へクロージャ生成して購読
   - Section が削除されるとクロージャも解除（メモリリーク防止）
6. **batch 概念なし**: 1 操作 = 1 ApplySnapshot/SubmitList

## 既存設計との関係

### archive 済み spec への影響

| spec | 影響度 | 内容 |
|------|--------|------|
| `settings-view-core` | 大 | `SettingsRoot.header/footer` 削除、`SettingsRootDiff` / `AccessoryTarget` 追加 |
| `settings-view-ios-ui` | 大 | `KsSettingsViewController` 公開 API 刷新、`SettingsRootStore` 追加 |
| `settings-view-android-ui` | 大 | 同上 |
| `samples-ios` | 中 | Sample コードを Store 方式に書き換え |
| `samples-android` | 中 | 同上 |
| `monorepo-foundation` | なし | - |

### 進行中提案への影響

| 提案 | 影響度 | 内容 |
|------|--------|------|
| `add-maui-bridge` | 大 | Decision 2 変更、Bridge に applyDiff API 追加 |
| `add-maui-core` | 大 | Decision 2 変更、Handler を ModelProxy 方式に |
| `add-maui-cells` | 小 | 依存変更のみ |
| `add-cell-types-*` | なし | - |
| `add-samples-maui` | 小 | 依存変更のみ |

## オープン論点（実装時に判断）

1. **Swift `any KsCell` を `KsCellID` で識別する仕組み**: 既存の `KsCellID(cell:)` を活用するが、`replaceCell` 時に同じ ID で別 cell 実体を作る場合の挙動を検討する必要あり
2. **Compose 側 Store の diff 発行**: `SharedFlow` で発行するか `Channel` か。`replay = 0` の `MutableSharedFlow` を採用予定
3. **`SettingsViewHandler.DisconnectHandler` での購読解除**: 各 Section.Cells および Sections 自体の購読解除を確実に行うため、AiForms の `_closureEventHandlerDictionary` パターンを継承
4. **`SettingsView.Sections` を `ObservableCollection<Section>` ではなく `IList<Section>` で受ける場合のフォールバック**: NativeCollectionView と同様、`ObservableCollection` を検知した場合のみ部分更新、それ以外は全体 `SetRoot`

## 提案作成順序

1. `add-partial-update-core` 作成
2. `add-partial-update-native` 作成 (Change 1 を依存に含める)
3. 進行中 MAUI 提案 (`add-maui-bridge`, `add-maui-core`) の修正
4. 関連提案 (`add-maui-cells`, `add-samples-maui`) の依存記述更新

各提案でレビュー → 修正のサイクルを回す。
