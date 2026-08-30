# 宣言的 DSL 復活設計 - 議論結果まとめ

> 探索モード (`/sdd-explore`) での議論結果。
> 本ドラフトは SwiftUI / Jetpack Compose の宣言的 DSL を復活させる変更提案の元情報。

## 背景・動機

### これまでの経緯

KsSettingsView は実装の歴史を経て、以下の変遷をたどっている：

```
① 2026-05-09 add-settings-view-ios-ui (archive 済)
   - SwiftUI 流儀の `@resultBuilder` ベース DSL を提供
   - `KsSettingsView(root: $root)` + `SettingsRoot { Section { Cell... } }`
   - @Binding<SettingsRoot> 方式で双方向

② 2026-05-09 refactor-accessory-and-root-hf (archive 済)
   - Root H/F 追加、`KsAnyView` 型消去ラッパ導入

③ add-partial-update-core (in-progress)
   - `SettingsRootDiff` 型を導入

④ add-partial-update-native (in-progress)  ← 切替点
   - BREAKING: @Binding<SettingsRoot> 廃止
   - `SettingsRootStore` 一本化
   - 動機: Swift 値型の全再構築を避けるための極限性能ポリシー
   - 代償: SwiftUI 流儀から離れた API になった
```

### 現状の問題

現 Store 方式は性能面では理想だが、以下の課題がある：

1. **SwiftUI / Compose ユーザーから見て不自然な API**
   - `KsSettingsView(store: store)` という命令型のラッパ
   - `store.insertCell(...)` / `store.removeCell(...)` の命令型操作
   - SwiftUI 本家の `List` / `Form` の宣言的記法から乖離

2. **静的な設定画面の記述が冗長**
   - 設定画面の典型的用途（数十〜数百セルの静的構造）でも Store の初期化と DSL 構築の両方が必要
   - `SettingsRoot { Section { ... } }` を Store の引数に渡す現状は中途半端

3. **Section H/F / Root H/F の Modifier 風 API が欲しい**
   - 引数による指定 (`SectionAccessory.text("...")` 等) は SwiftUI 流儀から外れる
   - 任意 View（`KsAnyView`）も Modifier クロージャで指定したい

4. **Cell ごとの属性（フォントサイズ等）も Modifier で指定したい**

### 解決方針

**DSL 方式と Store 方式の両立**。

- DSL 方式: SwiftUI / Compose 流儀の宣言的記法を公開 API として提供
- Store 方式: 大量データ・無限スクロール用にパワーユーザー向けに維持
- 内部実装: 両方とも共通の `SettingsRootStore` + `applyDiff` 経路に集約

これにより、Native UI 層 (`KsSettingsViewController` / `KsSettingsView`) を **無修正で再利用** しつつ、SwiftUI / Compose ラッパ層のみを刷新する。

## ユーザー要求の整理

### 元のイメージ（ユーザー提示）

```
KsSettingsView() {
    SettingsRoot() {
        Section() {
            Header("text") { }
            LabelCell("Title") { }
            CommandCell("Title") { }
            Footer("footer") { }
        }
        Section() { }
    }
}
```

### 要求の解釈

| 要素 | 要求 | 採用方針 |
|------|------|----------|
| `SettingsRoot()` 層 | 省略したい | `KsSettingsView { }` 直下に Section を並べる |
| Root H/F | 必要、任意 View 含む | `.rootHeader { }` / `.rootFooter { }` modifier |
| Section H/F | 必要、任意 View 含む | `.sectionHeader { }` / `.sectionFooter { }` modifier |
| Cell ごとの属性 | フォント等の Modifier 必要 | `.font(...)` / `.icon(...)` 等 |
| `{}` の意味 | アクション持つ Cell のみ | CommandCell 等のみ trailing closure |
| SwiftUI 流儀 | 極力近づけたい | 本家 `List`/`Form` の書き味を模倣 |

## DSL 方式 vs Store 方式の本質的違い

### 各方式の生成オブジェクト範囲

| 観点 | 初期 DSL 方式 | 現 Store 方式 | DSL+内部Store(両立) |
|------|---------------|---------------|---------------------|
| 書き味 | SwiftUI 流儀 ✨ | 命令的 | SwiftUI 流儀 ✨ |
| 動的追加削除 | `root` 再代入 | `store.insertCell` | `@State` 配列変更 |
| **Swift 値型** | ⚠️ 全再構築 | ✅ 該当のみ | △ 宣言ツリー分は再構築 |
| **UI 部分更新** | ✅ ある（DiffableDataSource） | ✅ ある | ✅ ある |
| Accessory 正確性 | ⚠️ refresh ロジック | ✅ 明示 Diff | ✅ 明示 Diff |
| EntryCell 連続入力 | ⚠️ 毎回全構築 | ✅ 直行パス | △ 工夫必要 |

### 重要な技術的事実

- **UI 部分更新は初期 DSL 方式でも実現されていた**（`UICollectionViewDiffableDataSource` が自動的に差分計算）
- **Swift 値型再構築を避けることが Store 化の真の動機**
- **無限スクロール用途では Store の優位性が顕著**（累積件数が多いほど DSL 方式の値型生成コスト O(N) が増大）

### SwiftUI / Compose 本家の振る舞い参考

- SwiftUI `List` / Compose `LazyColumn` は body 再評価時に値型を全件生成する設計
- ただし「@State 変更時のみ」評価される（スクロール中の再評価は起きない）
- 数万件程度までは実用上問題なく動作
- 10 万件超や累積成長型データでは独自の差分制御が必要

## アーキテクチャ設計

### 統一像

```
iOS (SwiftUI)            Android (Compose)
────────────             ─────────────────
KsSettingsView { }       KsSettingsView { }
 + Section/Cell DSL       + Section/Cell DSL
 + Modifier               + Modifier
 + ForEach                + forEach
      │                        │
      │ body 再評価             │ Recomposition
      │ → 宣言ツリー構築        │ → 宣言ツリー構築
      ▼                        ▼
 Diff 計算（共通アルゴリズム）
      │                        │
      ▼                        ▼
 SettingsRootStore       SettingsRootStore
 (Swift, @MainActor      (Kotlin, StateFlow)
  ObservableObject)
      │                        │
      │ Diff 発行                │ Diff 発行
      ▼                        ▼
 KsSettingsViewController KsSettingsView (RecyclerView)
  + UICollectionView       + ListAdapter
  + DiffableDataSource     + DiffUtil
      │                        │
      ▼                        ▼
 該当 Cell のみ部分更新      該当 Cell のみ部分更新
```

### 共通化の意義

DSL → Diff → Store → Native UI という共通経路を採用する理由：

1. **Native UI 層は無修正で再利用できる**（最大メリット）
   - `applyDiff(_:)` という既存 API がそのまま使える
   - Controller 側のテスト・実装を二重化しなくて済む

2. **DSL 方式と Store 方式が同じ Store を共有**
   - 利用者が用途で使い分けやすい
   - 一部 DSL、一部 Store のハイブリッドも理論上可能

3. **MAUI Bridge にも同じパスが使える**
   - MAUI Handler は Store/applyDiff 前提で設計済

4. **統一された Diff ログでデバッグしやすい**

## 公開 API 設計

### SwiftUI（iOS）

#### 基本形

```swift
struct SettingsPage: View {
    @State private var notificationsEnabled = true
    @State private var userName = "Taro"
    @State private var todos: [Todo] = []

    var body: some View {
        KsSettingsView {
            // ─── 静的Section ───
            Section {
                LabelCell("名前", value: userName)
                    .font(.headline)
                    .icon(.system("person"))
                CommandCell("プロフィール編集") {
                    showEditProfile()
                }
            }
            .sectionHeader("ユーザー")
            .sectionFooter("v1.0.0")

            // ─── ViewヘッダSection ───
            Section {
                SwitchCell("通知", isOn: $notificationsEnabled)
                EntryCell("ニックネーム", text: $userName)
            }
            .sectionHeader {
                HStack {
                    Image(systemName: "bell")
                    Text("通知設定").bold()
                }
            }

            // ─── 動的Section ───
            Section {
                ForEach(todos, id: \.id) { todo in
                    LabelCell(todo.name)
                }
            }
            .sectionHeader("Todo (\(todos.count))")
        }
        .rootHeader {
            ProfileCardView(name: userName)
        }
        .rootFooter("© 2026 Kamusoft")
        .style(.classic)
    }
}
```

#### Store 直接利用（パワーユーザー向け）

```swift
@StateObject var store = SettingsRootStore(initialRoot: ...)

KsSettingsView(store: store)
    .rootHeader { ProfileCard() }
    .style(.classic)

// 動的操作
store.insertCell(...)
```

### Jetpack Compose（Android）

#### 基本形

```kotlin
@Composable
fun SettingsPage() {
    val notificationsEnabled = remember { mutableStateOf(true) }
    val userName = remember { mutableStateOf("Taro") }
    val todos = remember { mutableStateListOf<Todo>() }

    KsSettingsView(
        rootHeader = { ProfileCard(name = userName.value) },
        rootFooter = { Text("© 2026 Kamusoft") },
        style = KsSettingsViewStyle.Classic,
    ) {
        // ─── 静的Section ───
        Section(header = "ユーザー", footer = "v1.0.0") {
            LabelCell("名前", value = userName.value)
                .font(KsFont.headline)
                .icon(KsIcon.system("person"))
            CommandCell("プロフィール編集") {
                showEditProfile()
            }
        }

        // ─── ViewヘッダSection ───
        Section(
            headerContent = {
                Row {
                    Icon(...)
                    Text("通知設定", fontWeight = FontWeight.Bold)
                }
            }
        ) {
            SwitchCell("通知", isOn = notificationsEnabled)
            EntryCell("ニックネーム", text = userName)
        }

        // ─── 動的Section ───
        Section(header = "Todo (${todos.size})") {
            forEach(todos, key = { it.id }) { todo ->
                LabelCell(todo.name)
            }
        }
    }
}
```

#### Store 直接利用

```kotlin
val store = remember { SettingsRootStore(initialRoot = ...) }

KsSettingsView(
    store = store,
    rootHeader = { ProfileCard() },
    style = KsSettingsViewStyle.Classic,
)

// 動的操作
store.insertCell(...)
```

### iOS / Compose 仕様対応表

| 機能 | iOS (SwiftUI) | Compose | 備考 |
|------|---------------|---------|------|
| ルート | `KsSettingsView { }` | `KsSettingsView { }` | 同形 |
| Section | `Section { }` | `Section { }` | 同形 |
| Section H | `.sectionHeader(...)` modifier | `Section(header=...)` か Slot | Compose は引数渡しの方が自然 |
| Section F | `.sectionFooter(...)` modifier | `Section(footer=...)` | 同上 |
| Root H | `.rootHeader { }` modifier | `rootHeader = { }` 引数 | Compose は引数 |
| Root F | `.rootFooter { }` modifier | `rootFooter = { }` 引数 | 同上 |
| 動的 | `ForEach(items) { }` | `forEach(items) { }` | iOS 大文字 / Compose 小文字慣習 |
| Cell modifier | `.font(...).icon(...)` | `.font(...).icon(...)` | 同形 |
| Cell action | `Cell("...") { }` | `Cell("...") { }` | 同形 |
| Cell ID 自動 | 位置+型ベース | 同左 | 共通アルゴリズム |
| Cell binding | `$state` | `mutableStateOf` | 各言語の流儀 |
| 内部 Store | 共通 `SettingsRootStore` | 共通 `SettingsRootStore` | 既存実装活用 |

## 独自 ForEach 関数（SwiftUI）

### 必要なオーバーロード

SwiftUI 標準の `ForEach` (View) は使えないため、独自の `ForEach` 関数を提供する。
**ルート用（Section列を返す）** と **セクション内用（Cell列を返す）** で別関数が必要。

```swift
// ─────────────────────────────────────────
// ルート（Section列）用 ForEach
// ─────────────────────────────────────────
public func ForEach<Data, ID>(
    _ data: Data,
    id: KeyPath<Data.Element, ID>,
    @SettingsRootBuilder content: (Data.Element) -> [KsSection]
) -> [KsSection]
where Data: RandomAccessCollection, ID: Hashable

// Identifiable 版オーバーロード
public func ForEach<Data>(
    _ data: Data,
    @SettingsRootBuilder content: (Data.Element) -> [KsSection]
) -> [KsSection]
where Data: RandomAccessCollection, Data.Element: Identifiable

// ─────────────────────────────────────────
// セクション内（Cell列）用 ForEach
// ─────────────────────────────────────────
public func ForEach<Data, ID>(
    _ data: Data,
    id: KeyPath<Data.Element, ID>,
    @SectionBuilder content: (Data.Element) -> [any KsCell]
) -> [any KsCell]
where Data: RandomAccessCollection, ID: Hashable

public func ForEach<Data>(
    _ data: Data,
    @SectionBuilder content: (Data.Element) -> [any KsCell]
) -> [any KsCell]
where Data: RandomAccessCollection, Data.Element: Identifiable
```

合計 4 つ。`Identifiable` 版と `id:` KeyPath 版 × Section 用と Cell 用。
すべて `ForEach` という同じ名前で大丈夫（Swift コンパイラがクロージャの戻り型と引数型から振り分け）。

### Compose の対応

Compose では同等機能を `forEach`（小文字）として提供。`key` パラメータで同一性を引き継ぐ。

```kotlin
// セクション内用
fun SectionScope.forEach(
    items: List<T>,
    key: (T) -> Any,
    content: (T) -> Unit
)

// ルート用
fun SettingsRootScope.forEach(
    items: List<T>,
    key: (T) -> Any,
    content: (T) -> Unit
)
```

## Cell / Section 同一性判定戦略

### 設計の前提

**ユーザーには ID を意識させない**ことを最優先。
AiForms.Maui.NativeCollectionView の `DataSourceItem<T>.Id = NSUuid()` パターン（インスタンス生成時に自動採番）を参考。

### Section ID 判定の優先順位

```
1. ForEach 配下 → item.id（Identifiable または id: KeyPath）を引き継ぐ
2. .sectionID(_:) modifier で明示指定 → それ
3. ヘッダ文字列あり → ハッシュ(rootIdx, headerText)
4. フォールバック → 位置ベース (rootIdx)
```

### Cell ID 判定の優先順位

```
1. ForEach 配下 → item.id を引き継ぐ
2. .cellID(_:) modifier で明示指定 → それ
3. デフォルト → ハッシュ(SectionID, Section内位置, Cell型)
```

### ユースケース別の挙動

#### ケース1: 完全静的（典型的な設定画面）

```swift
KsSettingsView {
    Section {                          // ID: ハッシュ(rootIdx=0, "ユーザー")
        LabelCell("名前")               // ID: ハッシュ(Sec1, 0, LabelCell)
        SwitchCell("通知")              // ID: ハッシュ(Sec1, 1, SwitchCell)
    }
    .sectionHeader("ユーザー")
    Section {                          // ID: ハッシュ(rootIdx=1, "詳細")
        CommandCell("ログアウト") {...} // ID: ハッシュ(Sec2, 0, CommandCell)
    }
    .sectionHeader("詳細")
}
```

→ ヘッダ文字列があるので Section ID 安定、Cell も Section ID + 位置 で安定。
body 何度再評価されても全 Cell 同一判定。

#### ケース2: Cell が動的（無限スクロール / Todo リスト）

```swift
KsSettingsView {
    Section {
        ForEach(items) { item in       // Identifiable
            LabelCell(item.name)        // ID: item.id を引き継ぐ
        }
    }
    .sectionHeader("Todo")
}
```

→ `items.append` で「新規 item の id」だけ insertCell される。既存 item は完全保持。

#### ケース3: Section も動的

```swift
KsSettingsView {
    ForEach(groups) { group in         // ルート用 ForEach
        Section {
            ForEach(group.items) { item in
                LabelCell(item.name)
            }
        }
        .sectionHeader(group.name)
    }
}
```

→ Section ID = group.id、Cell ID = item.id。完全動的でも安定。

#### ケース4: ヘッダなし静的 Section が複数（既知の弱点）

```swift
KsSettingsView {
    Section {                          // ヘッダなし
        LabelCell("一行目")
    }
    Section {                          // ヘッダなし
        LabelCell("二行目")
    }
}
```

→ ヘッダで区別不可、位置ベースにフォールバック。
→ Section 追加・削除で全ズレる可能性。
→ 動的なら ForEach か `.sectionID(...)` 明示を **ドキュメント指針** として案内。

### 採用方針

- 完全静的な構造 → デフォルトで OK
- 動的に Section / Cell 追加削除を伴う場合 → ForEach か `.sectionID()` / `.cellID()` で ID 指定を推奨
- 明示 API を提供することで「ID を意識させない」と「ID 明示の逃げ道」を両立

## Modifier API 設計

### Cell Modifier

iOS / Compose 共通の命名で、Cell の属性を変更する関数チェーン。

```swift
LabelCell("名前", value: "Taro")
    .font(.headline)
    .icon(.system("person"))
    .cellHeight(60)
    .backgroundColor(.systemGray6)
    .disabled(true)
```

```kotlin
LabelCell("名前", value = "Taro")
    .font(KsFont.headline)
    .icon(KsIcon.system("person"))
    .cellHeight(60)
    .backgroundColor(KsColor.systemGray6)
    .disabled(true)
```

**実装方針**:
- Cell 値型は値型のまま保持
- modifier は新しい値を返す（SwiftUI 流儀）
- 内部の `CellStyle` プロパティを書き換える

```swift
extension LabelCell {
    public func font(_ font: KsFont) -> LabelCell {
        var copy = self
        copy.style.font = font
        return copy
    }
}
```

### Section Modifier

```swift
Section { ... }
    .sectionHeader("ユーザー")           // 文字列ヘッダ
    .sectionHeader { CustomHeaderView() }  // 任意 View ヘッダ
    .sectionFooter("v1.0.0")
    .sectionFooter { CustomFooterView() }
    .sectionID("user-section")           // 明示 ID（動的構造用）
```

### KsSettingsView Modifier

```swift
KsSettingsView { ... }
    .rootHeader("プロフィール")          // 文字列ヘッダ
    .rootHeader { ProfileCardView() }    // 任意 View
    .rootFooter("© 2026")
    .rootFooter { FooterView() }
    .style(.classic)
    .theme(myTheme)
```

## 内容変更時の挙動

### Cell 内容変更（例: `userName` の変化）

```
@State userName = "Taro" → "Hanako"
       │
       │ body 再評価
       ▼
LabelCell(userName) が新規値型として生成
  → Cell ID は変わらない（SectionID + 位置 + 型 が同じ）
  → 中身は違う（title プロパティが "Hanako"）
       │
       │ ラッパが旧 LabelCell との比較
       │ → 同じ ID で内容違う → .replaceCell Diff
       ▼
Store: cellIndex[id] = new LabelCell
       │
       ▼
Controller: snapshot.reloadItems([cellID])
       │
       ▼
UI: 該当 Cell のみ再描画（周辺は無傷）
```

### レイヤ別の再生成範囲

| レイヤ | DSL 方式 | Store 方式 |
|--------|---------|------------|
| Swift 値型（該当 Cell） | ✅ 新規生成 | ✅ 新規生成 |
| Swift 値型（その他の body 内 Cell） | ⚠️ 全部新規生成 | ❌ 生成されない |
| `KsCellID` | ❌ 同じまま | ❌ 同じまま |
| Store の sections 配列 | ✅ 該当 Cell のみ差し替え | ✅ 該当 Cell のみ差し替え |
| `UICollectionViewCell` 実体 | ❌ 再利用 | ❌ 再利用 |
| Cell の表示内容（テキスト等） | ✅ 再描画 | ✅ 再描画 |
| 周辺 Cell | ❌ 完全に無傷 | ❌ 完全に無傷 |

### Equatable 判定で無駄な再描画を回避

```swift
let old = LabelCell("Taro")
let new = LabelCell("Hanako")

if old != new {
    store.applyDiff(.replaceCell(cellID: id, new: new))
} else {
    // 何もしない（無駄な再描画を避ける）
}
```

`KsCell` は既に `Hashable` 要件があるので `Equatable` も自動的に満たされる。

## Bindingセル（双方向バインド）

### SwiftUI

`@Binding<T>` を Cell イニシャライザで受け取る。

```swift
@State var notificationsEnabled = true
@State var userName = "Taro"

SwitchCell("通知", isOn: $notificationsEnabled)
EntryCell("名前", text: $userName)
PickerCell("国", selection: $country, options: countries)
```

### Compose

`MutableState<T>` を渡す。

```kotlin
val notificationsEnabled = remember { mutableStateOf(true) }
val userName = remember { mutableStateOf("Taro") }

SwitchCell("通知", isOn = notificationsEnabled)
EntryCell("名前", text = userName)
PickerCell("国", selection = country, options = countries)
```

### 高頻度更新の最適化（EntryCell 等）

- Native 側で 200ms debounce
- debounce 完了時のみ `updateCellValue(cellId:value:)` 直行パスで Store に通知
- Diff 計算経路は通らない（高速化）
- 既存の `add-partial-update-native` で導入予定の仕組みを流用

## 影響範囲

### 修正が必要

| モジュール | 修正内容 |
|------------|----------|
| iOS `KsSettingsViewSwiftUI` | DSL init 復活、Modifier API、独自 ForEach、Diff 計算 |
| Android `ks-settingsview-compose` | DSL receiver 拡張、Modifier API、独自 forEach、Diff 計算 |

### 修正不要（既存実装をそのまま活用）

| モジュール | 理由 |
|------------|------|
| iOS `KsSettingsViewCore` | ドメインモデル変更なし |
| iOS `KsSettingsViewUI` | Store / Controller / applyDiff そのまま |
| Android `ks-settingsview-core` | 同上 |
| Android `ks-settingsview-ui` | 同上 |
| MAUI Bridge | 同上 |
| MAUI 本体 | 同上 |

## 依存関係

### 前提（archive 必須）

- `add-monorepo-foundation` (archive 済)
- `add-settings-view-core` (archive 済)
- `add-settings-view-ios-ui` (archive 済)
- `add-settings-view-android-ui` (archive 済)
- `add-partial-update-core` (in-progress、先行 archive 必須)
- `add-partial-update-native` (in-progress、先行 archive 必須)

### 既存 in-progress 提案との関係

`add-partial-update-native` で SwiftUI ラッパは `init(store:)` のみとされているが、本提案で `init(@SettingsRootBuilder)` を **追加** する形にする。`init(store:)` は維持。

## リスク・トレードオフ

### リスク

1. **DSL → Diff 計算ロジックの複雑性**
   - 新旧の宣言ツリーを比較して `SettingsRootDiff` 列を生成
   - Section / Cell の同一性判定アルゴリズムが正確である必要
   - 緩和策: 静的・動的・混在の各パターンで網羅テスト

2. **Bindingセルの実装が型システム的に複雑**
   - `@Binding<Bool>` を持つ `SwitchCell` と値型の `LabelCell` の統一表現
   - 緩和策: Cell プロトコル設計を踏襲、Binding は内部参照型で保持

3. **ヘッダなし静的 Section の同一性問題**
   - 既知の弱点として残る
   - 緩和策: ドキュメント指針で `ForEach` / `.sectionID()` 推奨

4. **iOS / Compose の挙動差異**
   - 同じ仕様でも実装機構が違う
   - 緩和策: 振る舞いテストを両 OS で揃える

### トレードオフ

- DSL 方式は Swift 値型レイヤで再構築が走る → 通常用途では実用上問題ない
- 大量データには Store 方式の逃げ道を残す
- API サーフェスが増える（DSL init + Store init）→ ドキュメントで使い分け案内

## Open Questions

- Modifier の型シグネチャ細部（`.font().icon()` の戻り型を `Self` で返すか共通 `CellBase` で返すか）
- `.cellID(_:) / .sectionID(_:)` の型（`String` か `AnyHashable` か `UUID` か）
- 既存 archive 済の SwiftUI ラッパテストの再利用範囲
- MAUI ユーザーへの影響（基本的に無影響だが docs/migration-from-aiforms.md の追記要否）

## 次のステップ

1. 本ドラフトを元に正式な変更提案を作成
   - 名前候補: `add-declarative-dsl` / `restore-swiftui-compose-dsl`
2. proposal.md / design.md / specs / tasks.md を `opsx:propose` で一括生成
3. 既存 in-progress 提案 (`add-partial-update-core` / `add-partial-update-native`) との整合確認
4. Bindingセル型設計の詳細詰め
5. Modifier API の正確なシグネチャ確定
