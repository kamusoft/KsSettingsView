## 参考実装

本変更提案の実装に着手する **前に必ず** 以下を確認すること。

- 初期 DSL 実装の archive（DSL 復活の出発点）：
  - [`openspec/changes/archive/2026-05-09-add-settings-view-ios-ui/`](../archive/2026-05-09-add-settings-view-ios-ui/)
  - 初期実装の SwiftUI ラッパ `@Binding<SettingsRoot>` 経路、`@resultBuilder` 設計
- 既存 SwiftUI DSL 実装：
  - [`ios/Sources/KsSettingsViewSwiftUI/SettingsRootBuilder.swift`](../../../ios/Sources/KsSettingsViewSwiftUI/SettingsRootBuilder.swift)
  - [`ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift`](../../../ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift)
  - [`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`](../../../ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift)
- 既存 Compose DSL 実装：
  - [`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt`](../../../android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/SettingsRootScope.kt)
  - [`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt`](../../../android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt)
- 並走 in-progress 提案（先行 archive 必須）：
  - [`openspec/changes/add-partial-update-core/`](../add-partial-update-core/)
  - [`openspec/changes/add-partial-update-native/`](../add-partial-update-native/)
- AiForms 由来の ID 自動採番パターン：
  - `../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/DataSourceItem.cs`（`NSUuid Id { get; init; } = new();` 構造）
- 議論結果のドラフト：
  - [`openspec/drafts/03-declarative-dsl-design.md`](../../drafts/03-declarative-dsl-design.md)

## Context

KsSettingsView は履歴的に二度の API シフトを経験している：

1. **初期 DSL 方式（archive 済 `add-settings-view-ios-ui`）**: SwiftUI 利用者向けに `KsSettingsView(root: $root)` + `SettingsRoot { Section { Cell... } }` という `@resultBuilder` ベースの宣言的 API を提供。Compose 側も `settingsRoot { section { cell(...) } }` の lambda receiver 版 DSL を提供。`@Binding<SettingsRoot>` の `wrappedValue` を `KsSettingsViewController.root` に流し、`UICollectionViewDiffableDataSource` / `DiffUtil` の Hashable 比較で部分更新を実現していた。
2. **現 Store 方式（in-progress `add-partial-update-native`）**: 上記方式の課題（Swift 値型の全再構築、Accessory 更新の推測 refresh ロジック）を解決すべく `SettingsRootStore` を導入。SwiftUI ラッパは `init(store:)` のみ、Compose ラッパは `KsSettingsView(store:, ...)` のみとした。`store.insertCell(...)` 等の命令型操作と `applyDiff(_:)` の明示 Diff 経路で、Swift 値型の全再構築を完全回避する設計。

Store 方式は性能面では理想に近いが、設定画面の **典型用途（数十〜数百セルの静的構造）** でも `SettingsRootStore` の事前構築と命令型 API が必須となり、SwiftUI / Compose の宣言的 UI 流儀から大きく乖離する結果となった。本提案はこの乖離を埋めるべく、**DSL 方式と Store 方式の両立** を実現する。

技術的な制約：

- 「Swift 値型の全再構築を避ける」性能ポリシーは維持したい（特に無限スクロールや累積成長型データ）
- Native UI 層（`KsSettingsViewController` / `KsSettingsView`）は `add-partial-update-native` で大規模に整備されたばかりであり、無修正で再利用したい
- SwiftUI / Compose の宣言的記法を「書き味」だけ復活させ、内部実装としては Store + `applyDiff` の経路に流す
- Cell / Section の ID 管理を利用者に意識させない（AiForms.Maui.NativeCollectionView の `NSUuid Id { get; init; } = new()` パターンを参考）

## Goals / Non-Goals

**Goals:**

- SwiftUI / Compose 利用者が宣言的記法（`KsSettingsView { Section { Cell... } }`）で設定画面を記述できる DSL を提供する
- DSL 内で Section H/F、Root H/F、Cell ごとの属性を Modifier 風 API で指定できるようにする（文字列・任意 View 両対応）
- 動的コレクションを `ForEach` / `forEach` 関数で展開できるようにする（`Identifiable` / `id:` KeyPath / `key:` lambda 対応）
- Cell / Section の ID 採番をユーザーから隠蔽し、明示が必要な場合は `.cellID(_:)` / `.sectionID(_:)` modifier を提供する
- 双方向バインド Cell（`SwitchCell` / `EntryCell` 等）の Binding 引数規約を DSL レベルで規定する
- DSL 方式と Store 方式（`init(store:)`）の両 API を併存させ、用途別に使い分け可能とする
- Native UI 層を無修正のまま、SwiftUI / Compose ラッパ層の追加のみで実現する
- iOS / Compose で **同じ書き味・同じ振る舞い** を実現する（仕様レベルで揃える）

**Non-Goals:**

- 具象 Cell（`SwitchCell` / `EntryCell` / `PickerCell` 等）の Cell 型自体の追加 → `add-cell-types-*` 系で対応
- Native UI 層（`KsSettingsViewController` / `KsSettingsView`）の API 変更 → 既存実装を完全に維持
- MAUI 層への影響 → MAUI Bridge / Bindings / MAUI 本体は SwiftUI / Compose DSL とは独立した経路
- 既存 Store 方式 API の破壊 → `init(store:)` は維持
- 無限スクロール / 大量データの最適化 → Store 方式の利用を推奨し、DSL 方式は典型用途に限定
- KMP（Kotlin Multiplatform）対応 → Phase 2 以降
- SwiftUI 本家の `ForEach(0..<10)` 相当の `Range<Int>` 専用オーバーロード → 本提案では提供しない（必要があれば後続提案で検討）
- Compose `LazyColumn` への切替対応 → **本提案では一切提供しない**。本ライブラリは MAUI バインディングを経由した利用も想定しており、MAUI 側からは `LazyColumn` ベースの実装を呼び出せない（`AndroidView` 経由で従来の `RecyclerView + ListAdapter` を埋め込む必要がある）。Native Compose ユーザーと MAUI ユーザーで内部実装が分岐するのは保守上のコストが大きいため、既存 RecyclerView + ListAdapter 経路に統一する

## Decisions

### Decision 1: DSL 方式と Store 方式の併存

**選択**: SwiftUI / Compose ラッパに DSL init を **追加** し、既存の `init(store:)` も **維持** する。利用者が用途で使い分ける。

```swift
// 一般用途（DSL 方式）
KsSettingsView {
    Section { ... }
}

// パワーユーザー向け（Store 方式）
KsSettingsView(store: myStore)
```

```kotlin
// DSL 方式
KsSettingsView {
    Section { ... }
}

// Store 方式
KsSettingsView(store = myStore, ...)
```

**理由**:

- 設定画面の典型用途（静的・数十〜数百セル）は DSL 方式が圧倒的に書きやすい
- 無限スクロール / 大量データ / リアルタイム高頻度更新は Store 方式が性能的に必須（Swift 値型の全再構築を完全回避）
- 両方を提供することで利用者の選択肢が広がり、用途別に最適化された API を使える
- 内部実装は両者とも同じ `SettingsRootStore` + `applyDiff` 経路に流れるため、Native UI 層を一切変更せずに済む

**代替案**:

- DSL 一本化（Store API を内部実装に隠蔽）：無限スクロール用途で命令型操作の逃げ道が失われる。**不採用**
- Store 一本化（現状維持）：宣言的記法のメリットが永久に得られない、SwiftUI / Compose ユーザーから不自然な API のまま。**不採用**
- DSL と Store のハイブリッド init（`KsSettingsView(store: store) { ... }`）：意味的に曖昧（DSL の宣言が初期 root か、それとも継続反映か）、設計者にとっても利用者にとっても混乱の元。**不採用**

### Decision 2: DSL 方式の内部実装も `SettingsRootStore` + `applyDiff` 経路を再利用

**選択**: SwiftUI ラッパは `@StateObject` で `SettingsRootStore` を内部保持し、Compose ラッパは `remember` で同等の Store を保持する。`body` 再評価 / Recomposition のたびに新旧の宣言ツリーを比較し `SettingsRootDiff` 列を算出、内部 Store の `applyDiff(_:)` 相当に流す。

```
利用者の DSL 記述
       │
       │ body 再評価 / Recomposition
       ▼
新しい宣言ツリー（[KsSection]）を構築
       │
       │ ラッパ内部の DiffCalculator
       │   旧ツリー（前回保持）と新ツリーを比較
       │   → [SettingsRootDiff] を算出
       ▼
内部 Store に Diff 列を順次 apply
       │
       │ Store の diffPublisher で発行
       ▼
KsSettingsViewController.applyDiff(_:)（既存・無修正）
       │
       ▼
UICollectionView 部分更新
```

**理由**:

- Native UI 層（`KsSettingsViewController` / Android `KsSettingsView`）は `add-partial-update-native` で精緻に設計されたばかりで、`applyDiff` ベースの統一経路が完成している。これを無修正で再利用するのが最も安全で省コスト
- DSL 方式と Store 方式が同じ Store / Diff 経路を共有することで、テスト・デバッグ・MAUI 連携などすべての副次的恩恵が共通化される
- 統一された Diff ログ（`replaceCell` / `insertSection` / `updateAccessory` 等）で動作トレースが容易になる
- 将来 MAUI Handler が SwiftUI DSL 経路を呼ぶ場合（不要だが理論上可能）も同じ Store API で対応できる

**代替案**:

- DSL 専用の別経路（`KsSettingsViewController.root` setter 復活）：Native UI 層に二系統の状態管理が混入、メンテナンス地獄。**不採用**
- Diff 計算を Native 層に押し込む（旧 `refreshAccessoriesIfNeeded` 風）：推測ロジックの再導入で `add-partial-update-native` の改善を巻き戻すことになる。**不採用**
- Store を持たず、直接 `applyDiff` を Controller に呼ぶ：Store の `@Published` / StateFlow による状態管理が失われ、SwiftUI / Compose の reactive な性質を活かせない。**不採用**

### Decision 3: 独自 `ForEach` / `forEach` 関数の提供

**選択**:

- SwiftUI 側: 関数名 `ForEach` を採用（本家の View 版 `ForEach` と型推論で振り分け）。4 オーバーロード提供：
  - ルート用 × `Identifiable` 版
  - ルート用 × `id:` KeyPath 版
  - セクション内用 × `Identifiable` 版
  - セクション内用 × `id:` KeyPath 版
- Compose 側: 関数名 `forEach`（小文字、Kotlin 慣習）を採用。`DSLSettingsRootScope` / `DSLSectionScope` の receiver method / 拡張関数として提供：
  - `DSLSettingsRootScope.forEach(items, key, content)`: Compose 公式の `key` lambda 流儀（既存）
  - `DSLSectionScope.forEach(items, key, content)`: 同上
  - `DSLSettingsRootScope.forEach<T : KsIdentifiable>(items, content)`: `KsIdentifiable` marker interface 経由で `key` 省略可（SwiftUI Identifiable 版と並列）
  - `DSLSectionScope.forEach<T : KsIdentifiable>(items, content)`: 同上
- Compose 側で導入する marker interface:
  - `interface KsIdentifiable { val id: Any }`（`ks-settingsview-compose` モジュールに定義）
  - Kotlin / Compose には Swift `Identifiable` 標準が存在しないため、本ライブラリ独自に DSL 専用 marker として提供
  - Compose 公式の `LazyColumn.items(key = { ... })` も `key` lambda を標準作法としているため、`key` lambda 版は Compose イディオム尊重として継続提供する（両方併存）

```swift
// SwiftUI: セクション内 Cell
ForEach(items) { item in              // Identifiable 版
    LabelCell(item.name)
}
ForEach(items, id: \.myKey) { item in // id KeyPath 版
    LabelCell(item.name)
}

// SwiftUI: ルートで Section 群
ForEach(groups) { group in
    Section { ... }
}
```

```kotlin
// Compose: KsIdentifiable 版（key 省略、SwiftUI Identifiable と並列）
data class Todo(override val id: Int, val name: String) : KsIdentifiable

forEach(items) { item ->                          // key 省略
    LabelCell(item.name)
}

// Compose: key lambda 版（Compose 公式作法）
forEach(items, key = { it.id }) { item ->
    LabelCell(item.name)
}

// Compose: ルートで Section 群（KsIdentifiable 版 / key lambda 版とも提供）
forEach(groups) { group -> Section { ... } }
forEach(groups, key = { it.id }) { group -> Section { ... } }
```

**理由**:

- SwiftUI の `@resultBuilder` は戻り型ごとにオーバーロードを定義するため、本家 `ForEach<Data, ID, Content>` View 型はそのまま使えない（内部状態にアクセス不可）
- 同じ `ForEach` という名前を使うことで利用者の学習コストを最小化（本家からの移行が違和感ない）
- `Identifiable` 版と `id:` KeyPath 版を両方提供することで本家と完全な API 一致
- Compose は Kotlin 慣習に従い小文字 `forEach`、`key` パラメータで同一性を引き継ぐ（Compose 公式の `LazyColumn.items(key = ...)` と作法を揃える）
- 加えて Compose 側で `KsIdentifiable` marker interface 版を提供することで、利用者が data class に `KsIdentifiable` を実装すれば `key` 省略可となり、SwiftUI `Identifiable` 版と並列した書き味を実現する（iOS / Android 整合性向上）
- `KsIdentifiable` 版と `key` lambda 版は併存させ、利用者の好み・用途に応じて選択可能とする
- ルート用とセクション用は戻り型が違う（`[KsSection]` vs `[any KsCell]`）ため別オーバーロードが必須、ただし関数名は統一できる

**代替案**:

- 別名（`KsForEach` / `Repeat` 等）にする：SwiftUI 本家との一貫性が失われる、利用者の学習コストが増える。**不採用**
- SwiftUI 本家の `ForEach` View をそのまま受け入れる：本家 `ForEach<Data, ID, Content>` の内部状態は private のため、`@resultBuilder` 内で `data` / `id` / `content` を取り出す手段が公式には提供されていない。**不採用**
- Compose で `ForEach` 大文字を採用：Kotlin 慣習（lower camelCase 関数）から外れる。**不採用**
- Compose で `KsIdentifiable` 版を提供せず `key` lambda 必須のまま：iOS との API 並列性が損なわれ、Sample コードが冗長になる。**不採用**
- Compose で `KsIdentifiable` 版のみ提供し `key` lambda 版を廃止：Compose 公式の `LazyColumn` 等の作法と乖離、外部 data class に marker interface 実装を強要することになる。**不採用**
- `KProperty1<T, ID>` 版（`forEach(items, id = DemoItem::id) { ... }`）を追加：Kotlin では `key = { it.id }` lambda の方が自然な記法のため、KeyPath 相当 API は不要。**不採用**

### Decision 4: Cell / Section の同一性判定戦略

**選択**: 以下の優先順位で ID を採番する：

**Section ID 判定**:

1. ForEach 配下 → `item.id`（`Identifiable.id`、`id:` KeyPath の値、`key:` lambda の戻り値）を採用
2. `.sectionID(_ id: AnyHashable)` modifier で明示指定 → それを採用
3. ヘッダが `SectionAccessory.text(String)` の場合 → ハッシュ(ルート位置, ヘッダ文字列)
4. フォールバック → ルート位置（`rootIdx`）ベース

**Cell ID 判定**:

1. ForEach 配下 → `item.id` を採用
2. `.cellID(_ id: AnyHashable)` modifier で明示指定 → それを採用
3. デフォルト → ハッシュ(SectionID, Section 内位置, Cell 型)

**Cell コンストラクタ `id` パラメータのデフォルト値規約**:

- 具象 Cell（SwiftUI: `LabelCell` / `SwitchCell` 等の `KsCell` 準拠 struct、Compose: 同等の `Cell` 準拠 data class）の `id` パラメータには **デフォルト値（UUID ベースのランダム文字列、もしくは `UUID()`）** を持たせなければならない (SHALL)
- 利用者は DSL 経路で Cell を生成する際、`id` 引数を **省略可能** とする（`LabelCell(title = "...")` のように直置きできる）
- DSL 経路では `DSLReidentifiableCell.withDSLId(...)`（Android）または `DSLReidentifiable.withDSLID(_:)`（iOS）で **コンストラクタの id デフォルト値は本仕様の優先順位に従う ID に rebind される**ため、デフォルト UUID 値が最終 Cell ID として漏れることはない
- Store 方式での利用時（DSL 経路を通らない場合）は、利用者が `id` 引数を明示指定するかデフォルト値を使うかを選択できる
- これにより iOS の `id: UUID = UUID()` と Compose の `id: String = "<className>-${UUID.randomUUID()}"` の挙動が並列に揃い、Sample / 利用コードで `id` 二重指定の冗長性を排除できる

**`DSLReidentifiableCell` / `DSLStyleModifiableCell` の配置モジュール（Gradle / SwiftPM 循環依存回避）**:

- Android 側の `DSLReidentifiableCell` / `DSLStyleModifiableCell` interface は、**`ks-settingsview-core` モジュール**（パッケージ `jp.kamusoft.kssettingsview.core`）に配置しなければならない (MUST)
- iOS 側の `DSLReidentifiable` / `DSLStyleModifiable` protocol も同様に、**`KsSettingsViewCore` モジュール**（フレームワーク `KsSettingsViewCore`）に配置しなければならない (MUST)
- 当初は Android `DSLReidentifiableCell` は `ks-settingsview-compose` モジュール、iOS `DSLReidentifiable` は `KsSettingsViewSwiftUI` モジュールに定義されていたが、後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `ks-settingsview-ui` / `KsSettingsViewUI` モジュールに配置されると、UI モジュールの Cell が SwiftUI / Compose モジュールの interface に依存することになり、既存の `*-compose / SwiftUI → *-ui` 依存と合わせて循環依存になる
- 解決策として、**Cell 値型の DSL rebind 規約は本質的に Core 層の責務**として両 OS とも Core モジュールに移動する
- 既存 `ks-settingsview-compose` / `KsSettingsViewSwiftUI` の interface 定義は Core 版を re-export するか、または完全移動して上位モジュールからは import で参照する
- 両 OS で対称的な配置とし、後続 `add-cell-types-*` 系では `*-ui` / `KsSettingsViewUI` の具象 Cell が Core 版の interface を implement する形になる

**理由**:

- AiForms.Maui.NativeCollectionView の `DataSourceItem<T>.Id = NSUuid()` パターン（インスタンス生成時に自動採番）は **動的構造には適用できない**（body 再評価で毎回 UUID が変わる）
- DSL 方式では body 再評価のたびに値型が新規生成されるため、UUID プロパティではなく **位置 + 内容ベースの導出 ID** が必要
- 動的なケースでは `ForEach` の `id` / `key` を明示的に引き継ぐことで、ユーザーが書く `item.id` の値が直接 Cell ID として使われる
- 静的なケースではヘッダ文字列やルート位置でハッシュを取ることで body 再評価をまたいだ同一性が維持される
- 明示 API `.sectionID(_:)` / `.cellID(_:)` を「上級者向け逃げ道」として用意することで、フォールバックの位置ベースで壊れるケース（ヘッダなし複数 Section の動的構造）に対応できる
- Cell コンストラクタ `id` のデフォルト値は **DSL 経路で rebind される前提のプレースホルダ** であり、利用者が DSL 内で `id` を意識する必要をなくすための設計（iOS Sample で `SampleLabelCell(title: "...")` 直置きが可能なのと同じ書き味を Compose Sample にも提供する）

**代替案**:

- すべて `UUID` 自動採番（AiForms 流）：body 再評価で UUID が毎回変わり、Diff が機能しない。**不採用**
- ユーザーに常に明示 ID を要求：DSL の書き味が悪化、SwiftUI 流儀から離れる。**不採用**
- ハッシュベースのみ（位置に依存しない、内容ハッシュのみ）：内容変更で別 Cell 扱いになり、Cell の状態（フォーカス等）が失われる。**不採用**

### Decision 5: Section / Cell の Modifier 風 API

**選択**:

- iOS: View modifier 風の拡張メソッドチェーン
  - Root H/F: `KsSettingsView { ... }.rootHeader("text").rootFooter { CustomView() }`
  - Section H/F: `Section { ... }.sectionHeader("text").sectionFooter("text")`
  - Cell modifier: `LabelCell("...").font(.headline).icon(.system("..."))`
- Compose: 以下の **2 層構成** で iOS との書き味を可能な限り揃える
  - **Section H/F**: `Section(header) { ... }.sectionHeader("text").sectionFooter("text").sectionID("id")` の **handle 経由 modifier chain**（iOS と並列）
    - `DSLSettingsRootScope.Section(...)` の戻り値を `Unit` から `SectionHandle` に変更
    - `SectionHandle.sectionHeader(text|content)` / `.sectionFooter(text|content)` / `.sectionID(id)` を `@SettingsRootDsl` 付き拡張関数として提供
    - 引数指定版（`Section(header = "...", footer = "...")`）も併存維持し利用者が選択可能
  - **Cell modifier**: `LabelCell(title = "...").font(...).cellHeight(...).cellID(...)` の **CellHandle 経由 chain**（iOS と並列）
    - `DSLSectionScope.cell(...)` および「具象 Cell 用 DSL 拡張関数」（後述）の戻り値を `Unit` から `CellHandle` に変更
    - `CellHandle.font(...)` / `.cellHeight(...)` / `.titleColor(...)` / `.cellID(id)` を `@SettingsRootDsl` 付き拡張関数として提供
    - 既存の `Cell.font(...)` などの値型 modifier は維持（外部 Cell 値や Store 方式での利用用）
  - **Cell 直置き（`cell(...)` ラップ省略）**: 具象 Cell 型ごとの DSL 拡張関数を `DSLSectionScope` に定義する
    - 例: `fun DSLSectionScope.SampleLabelCell(title: String, style: CellStyle = CellStyle()): CellHandle = cell(SampleLabelCell(style = style, title = title))`
    - data class `SampleLabelCell` と同名の DSL 関数を **同一スコープで共存** させる（Kotlin の overload 解決により DSL ブロック内では関数版が優先される）
    - これにより `Section("...") { SampleLabelCell(title = "...") }` のように iOS と完全一致した書き味を実現
    - 補助として `operator fun Cell.unaryPlus(): CellHandle = cell(this)` を `DSLSectionScope` に定義し、外部から渡された `Cell` 値を `+cell` で流せる逃げ道とする
  - **Root H/F**: `@Composable fun KsSettingsView` の引数指定（`rootHeader: @Composable () -> Unit?`、`rootFooter: @Composable () -> Unit?`）を維持する
    - Compose では `@Composable` 関数の戻り値は `Unit` であるべきという慣習（recomposition / SideEffect / remember の保証）が強い
    - `KsSettingsView { ... }.rootHeader(...)` のような chain は構造的に不可能（`@Composable` 関数を呼び出した結果を「ハンドル」として持ち回ると Compose runtime 規約と衝突する）
    - 一方 `Section(...)` および Cell 用 DSL 関数は **非 `@Composable`** な DSL receiver method / 拡張関数のため、戻り値型を変更しても Compose 規約に抵触しない（lambda 内で評価されるだけ）
    - したがって Root H/F のみ Compose イディオム尊重で引数指定とし、Section H/F・Cell は iOS と並列な modifier chain とする「意図的な非対称」を採用する

**理由**:

- SwiftUI 本家は `.navigationTitle(...)` / `.toolbar { ... }` のように modifier チェーンが基本のため、Section H/F も同じ流儀が自然
- Compose では `@Composable` 関数の戻り値 `Unit` 規約により Composable 自身に対する modifier chain は構造的に困難だが、**非 Composable な DSL 受信メソッド・拡張関数の戻り値型は自由に設計できる** ため、Section / Cell については iOS と並列な modifier chain を実現できる
- どちらも内部実装は値型 / data class を copy して新値を返すパターン（イミュータブル）。Compose では `SectionHandle` / `CellHandle` を介して内部の `DSLSectionNode` / `DSLCellNode` をスコープ経由で更新する
- Cell の `cell(...)` ラップ撤廃は、**Kotlin に Swift `@resultBuilder` 相当機構が存在しない**ことを「DSL 関数を同名で共存させる」アプローチで克服する。利用者からは `Section("...") { SampleLabelCell(title = "...") }` のように iOS と完全に同じ見た目で書ける
- Cell modifier は両 OS とも拡張関数チェーンで一致（Compose の `Modifier.padding(...)` とは独立した Cell 専用機構）
- 「Root H/F のみ引数指定の意図的な非対称」は、Compose イディオム尊重と iOS / Android 整合性のトレードオフ最適点。Section H/F・Cell の chain 化で得られるメリット（書き味の対称性、`.cellHeight(80.dp)` のような共通 modifier API）が大きいため

**代替案**:

- 両 OS とも引数渡しのみ：iOS の SwiftUI 流儀から外れる、modifier チェーンが書けない。**不採用**
- 両 OS とも modifier チェーンのみ：Compose の Composable 流儀から外れる、`@Composable` 関数の自然な引数渡しが使えない。**不採用**
- Compose の `Modifier` 機構を流用：既存 `Modifier.padding(...)` と意味的に衝突、Cell 専用属性を `Modifier` に詰めるのは不自然。**不採用**
- Compose で Root H/F も modifier chain 化：`@Composable fun KsSettingsView` の戻り値を `Unit` 以外にすると Compose runtime 規約と衝突し、recomposition / SideEffect の保証が崩れる。ビルダパターン（`ksSettingsViewSpec { ... }.rootHeader(...).Render()`）も検討したが、`Render()` 末尾呼び出しが必須となり iOS フィデリティが下がる。**不採用**
- Compose で `Section(...)` の戻り値を `Unit` のまま維持し引数指定のみ：本オーナーレビュー指摘（iOS と書き味乖離）が解決しない。**不採用**
- Compose で `cell(SampleLabelCell(...))` ラップを維持：Sample コードが冗長、iOS との見た目乖離が大きい。**不採用**

### Decision 6: 内部 Store の保持戦略

**選択**:

- SwiftUI: `@StateObject private var internalStore: SettingsRootStore` を `KsSettingsView` 構造体内に保持
- Compose: `remember { SettingsRootStore(...) }` で同等の Store を保持

```swift
public struct KsSettingsView: View {
    @StateObject private var internalStore: SettingsRootStore
    private let sectionsBuilder: () -> [KsSection]

    public init(
        style: KsSettingsViewStyle = .classic,
        @SettingsRootBuilder _ sections: @escaping () -> [KsSection]
    ) {
        // 初期 Store を一度だけ構築
        self._internalStore = StateObject(wrappedValue: SettingsRootStore(
            initialRoot: SettingsRoot(sections: sections())
        ))
        self.sectionsBuilder = sections
    }

    public var body: some View {
        // body 再評価のたびに新ツリー構築 → Diff 算出 → Store に流す
        let newSections = sectionsBuilder()
        let diffs = DiffCalculator.compute(
            from: internalStore.root.sections,
            to: newSections
        )
        diffs.forEach { internalStore.applyDiff($0) }

        return KsSettingsViewRepresentable(store: internalStore, ...)
    }
}
```

```kotlin
@Composable
fun KsSettingsView(
    modifier: Modifier = Modifier,
    style: KsSettingsViewStyle = KsSettingsViewStyle.Classic,
    rootHeader: (@Composable () -> Unit)? = null,
    rootFooter: (@Composable () -> Unit)? = null,
    content: SettingsRootScope.() -> Unit,
) {
    val bookkeeper = remember {
        DSLBookkeeper(
            store = SettingsRootStore(initialRoot = /* 初回 DSL 評価結果 */),
            lastTree = /* 初回 ResolvedTree */,
        )
    }

    // Diff 適用は AndroidView.update で行う。
    // SideEffect は当該 Composable がリコンポーズ skip されると登録されないため不適切。
    // AndroidView.update は Compose runtime がリコンポーズコミットごとに直接スケジュール
    // するため skip 判定の影響を受けない（iOS の updateUIViewController と同じセマンティクス）。
    AndroidView(
        modifier = modifier,
        factory = { ctx -> KsSettingsViewLayout(ctx).apply { bind(bookkeeper.store) } },
        update = { view ->
            view.style = style
            view.rootHeader = rootHeader?.let { /* RootAccessory.View */ }
            view.rootFooter = rootFooter?.let { /* RootAccessory.View */ }

            // Recomposition のたびに新ツリー構築 → Diff 算出 → Store に流す
            val newResolved = DSLSettingsRootScope().apply(content).buildResolved()
            val diffs = DSLDiffCalculator.compute(bookkeeper.lastTree, newResolved)
            for (diff in diffs) applyDiffToStore(bookkeeper.store, diff)
            bookkeeper.lastTree = newResolved
        },
    )
}
```

**理由**:

- `@StateObject` / `remember`（key 指定なし）は View identity が同じ間は Store インスタンスを保持し続けるため、body 再評価をまたいで状態が維持される
- 内部 Store を介することで Diff 経路を Store 方式と完全に共有でき、Native UI 層を無修正で再利用できる
- Store の `@Published` / StateFlow による状態更新通知が SwiftUI / Compose の reactive モデルと自然に統合される
- 初期 Store 構築コストは初回 init / 初回 Composition の一度のみ（以降は Diff 経路のみ）

**代替案**:

- `@State` で `SettingsRoot` 値型を保持し Native へ毎回全代入：`add-partial-update-native` で削除した「全代入」経路の再復活、性能ポリシー違反。**不採用**
- 利用者側で `@StateObject SettingsRootStore` を持ってもらう：DSL 方式の意味がなくなる、Store 方式と同じ書き味になる。**不採用**
- Singleton Store：View ごとの状態分離ができない、メモリリーク懸念。**不採用**

### Decision 7: Diff 算出アルゴリズム

**選択**: 旧宣言ツリーと新宣言ツリーを **Section ID / Cell ID ベースで突き合わせ**、以下の Diff 列を生成する：

```
1. Section レベルの比較:
   - 新ツリーにあって旧ツリーにない SectionID → insertSection
   - 旧ツリーにあって新ツリーにない SectionID → removeSection
   - 両ツリーに同 SectionID があり位置が違う → moveSection
   - 両ツリーに同 SectionID があり H/F が違う → updateAccessory

2. 各 Section 内の Cell レベルの比較:
   - 新セクションにあって旧セクションにない CellID → insertCell
   - 旧セクションにあって新セクションにない CellID → removeCell
   - 両セクションに同 CellID があり位置が違う → moveCell
   - 両セクションに同 CellID があり Cell 値が違う（Equatable 比較）→ replaceCell

3. Theme の比較:
   - 旧 Theme と新 Theme が違う → updateTheme
```

**理由**:

- 既存 `SettingsRootDiff` enum のケースとそのまま 1:1 対応するため、内部 Store の `applyDiff` を変更なしに呼べる
- Section / Cell の同一性は Decision 4 の ID 戦略で決まるため、アルゴリズム自体は単純な集合演算で済む
- Cell の値比較は `KsCell` の `Hashable` 要件で自動的に成立する（既存仕様）
- Theme の比較は `Theme` の `Equatable` で成立する（既存仕様）

**代替案**:

- LCS（Longest Common Subsequence）ベースの動的計画法：計算量 O(N×M)、過剰スペック。Cell ID ベースで十分。**不採用**
- 単純な全置換（`.full(newRoot)` のみ発行）：Swift 値型は新規生成だが、UI レベルでは全 reload となり性能ポリシー違反。**不採用**

### Decision 8: Bindingセル（双方向バインド）

**選択**:

- SwiftUI: `@Binding<T>` を Cell イニシャライザで受け取り、内部に保持。値変更時は Binding 経由で `@State` に書き戻す。
- Compose: `MutableState<T>` を Cell イニシャライザで受け取り、内部に保持。`state.value = newValue` で書き戻す。

```swift
// 注: 以下は説明用のサンプルコードであり、最終実装の指示ではない。
//     特に `id: UUID` を `UUID()` で自動採番する形は body 再評価のたびに ID が変わるため、
//     DSL → Diff 算出の同一性判定が破壊される。Cell ID は Decision 4 の判定戦略に従い、
//     `ForEach` の `item.id` 引き継ぎ、`.cellID(_:)` modifier、または
//     `(SectionID, Section 内位置, Cell 型)` のハッシュで決定する。
//     具象 Cell 型自体は `add-cell-types-*` で実装される。
public struct SwitchCell: KsCell {
    public let id: KsCellID  // ID は DSL → Diff 算出ロジックで決定される（UUID 自動採番禁止）
    public let title: String
    public let isOn: Binding<Bool>

    public init(_ title: String, isOn: Binding<Bool>) {
        // id は DSL ラッパが採番ヒントを付与する仕組みで決まる
        self.id = KsCellID.placeholder  // 実装時は適切な ID 解決経路で代入
        self.title = title
        self.isOn = isOn
    }
}
```

```kotlin
// 注: 以下は説明用のサンプルコードであり、最終実装の指示ではない。
//     具象 Cell（SwitchCell 等）は Decision 4 の「Cell コンストラクタ id パラメータの
//     デフォルト値規約」に従い、`id: String = "switch-${UUID.randomUUID()}"` のような
//     デフォルト値を持たせなければならない。また `DSLReidentifiableCell` /
//     `DSLStyleModifiableCell` 規約に準拠（Android 側の interface は `ks-settingsview-core`
//     モジュール配置）。具象 Cell 型自体は `add-cell-types-*` で実装される。
data class SwitchCell(
    override val id: String = "switch-${java.util.UUID.randomUUID()}",
    override val style: CellStyle = CellStyle(),
    val title: String,
    val isOn: MutableState<Boolean>,
) : Cell, DSLReidentifiableCell, DSLStyleModifiableCell {
    override fun withDSLId(newId: String): Cell = copy(id = newId)
    override fun withDSLStyle(newStyle: CellStyle): Cell = copy(style = newStyle)
}
```

- 高頻度更新パス（`EntryCell` 等）は Native 側で 200ms debounce し、`updateCellValue(cellId:value:)` 直行パスで Binding に反映する
- 双方向バインド時の DSL → Diff 算出では、Binding の `wrappedValue` / `state.value` を比較対象とする

**理由**:

- SwiftUI / Compose の reactive モデルと自然に統合される（`$state` / `MutableState<T>`）
- Binding は参照型なので body 再評価をまたいで同一性が維持される
- 高頻度更新パスは `add-partial-update-native` で導入予定の仕組みをそのまま流用できる

**代替案**:

- Cell に値だけ持たせ、変更コールバックを別途渡す：SwiftUI / Compose の reactive モデルから外れる、`@State` との連携が複雑。**不採用**
- Binding を持たない静的 Cell のみ提供：双方向バインド用途で利用者に追加の boilerplate が必要、SwiftUI / Compose 流儀から外れる。**不採用**

### Decision 9: 既存 `.header(...)` / `.footer(...)` modifier の即時削除

**選択**: `add-partial-update-native` で導入される SwiftUI ラッパの `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` modifier、および Compose ラッパの `headerView` / `footerView` パラメータは、本提案で **完全に削除** する。新規 `.rootHeader(_:)` / `.rootFooter(_:)`（SwiftUI）および `rootHeader` / `rootFooter` パラメータ（Compose）に一本化する。

```swift
// 削除（add-partial-update-native で導入される API）
view.header(.text("プロフィール"))      // ← 削除
view.header(.view(KsAnyView.swiftUI { ProfileCard() }))  // ← 削除

// 本提案で唯一の Root H/F API
view.rootHeader("プロフィール")
view.rootHeader { ProfileCard() }
```

```kotlin
// 削除
KsSettingsView(headerView = { ProfileCard() })  // ← 削除
KsSettingsView(footerView = { Footer() })       // ← 削除

// 本提案で唯一の Root H/F API
KsSettingsView(rootHeader = { ProfileCard() })
KsSettingsView(rootFooter = { Footer() })
```

**理由**:

- 本ライブラリは **運用前** であり、外部利用者が存在しないため互換維持は不要
- `.header(_ accessory: RootAccessory?)` は `RootAccessory` enum を直接渡す形で、SwiftUI 流儀の `@ViewBuilder` クロージャ記法から離れる
- `.rootHeader { ProfileCard() }` の方が SwiftUI 本家の `.toolbar { ... }` 等と一貫性がある
- Compose の `headerView` / `footerView` も命名が曖昧で、`rootHeader` / `rootFooter` の方が「Root の H/F」という意味が明確
- DEPRECATED 化して残置すると API が冗長になり、利用者がどちらを使うべきか迷う

**代替案**:

- DEPRECATED 化して当面残置：運用前なので互換維持の必要がない、API が冗長になるデメリットしかない。**不採用**
- `add-partial-update-native` 自体を archive 取り消しして API 名を直接 `.rootHeader` で導入：archive 順序の管理が複雑化、本提案の責務が曖昧になる。**不採用**

**前提**:

- 本 BREAKING 変更は `add-partial-update-native` の archive 完了後に本提案が archive されることで連続的に適用される
- 利用者コード（Sample / docs / テスト）の書き換えも本提案のタスクとして含める

## Risks / Trade-offs

### リスク

- **DSL → SettingsRootDiff 算出ロジックの実装複雑性**:
  - 旧宣言ツリーと新宣言ツリーの比較、Section / Cell の同一性判定、Diff 列の最適生成と多くの要素が絡む
  - 緩和策: Decision 7 のアルゴリズムを delta spec の Requirement / Scenario で厳密に規定し、テストパターンを網羅する。Section 追加・削除・移動・置換、Cell 追加・削除・移動・置換、Theme 更新、Accessory 更新の全ケースを iOS / Compose で検証する

- **ヘッダなし複数 Section の動的構造での同一性問題**:
  - 既知の弱点として残る（Decision 4 のフォールバック挙動）
  - 緩和策: ドキュメント指針で `ForEach` / `.sectionID(_:)` の明示指定を強調する。delta spec にもフォールバック挙動を明記し、開発者が「壊れたら明示 ID で逃げる」判断ができるようにする

- **iOS / Compose の挙動差異**:
  - 同じ仕様でも実装機構（`@resultBuilder` vs `@Composable` lambda receiver）が違うため、振る舞いに微妙な差が生じる可能性
  - 緩和策: delta spec を両 OS 共通の振る舞いベースで記述する。テストパターンを揃え、片方で発見された不具合を必ず両 OS で検証する

- **`@StateObject` / `remember` の View identity 依存**:
  - View が再生成される（親 View の `if` 分岐内で `KsSettingsView` を出し入れする等）と内部 Store も再生成され、状態がリセットされる
  - 緩和策: ドキュメントで「`KsSettingsView` の View identity を維持する利用パターン」を案内する。SwiftUI / Compose の標準的な View ライフサイクルから外れた使い方は非推奨と明示する

- **DSL 方式での無限スクロール / 大量データ性能**:
  - body 再評価のたびに宣言ツリー全体を生成するため、累積件数が増えると O(N) のコストが増大する
  - 緩和策: ドキュメントで「大量データ / 無限スクロールは Store 方式を推奨」と明記する。実測ベンチマーク（数百〜数千件）を docs に掲載する

- **`add-partial-update-native` との並走リスク**:
  - 本提案は `add-partial-update-native` の archive 後の状態を前提とする
  - 緩和策: `add-partial-update-native` を先行 archive、その後に本提案の実装に着手する。タスク順序を tasks.md で明示する

- **既存 archive 済テスト（初期 DSL 方式時代のテスト）の扱い**:
  - 初期実装時の DSL テストは `@Binding<SettingsRoot>` 前提で書かれており、現状の Store 方式に合わせて既に書き換えられているはず
  - 緩和策: 既存テストはそのまま、本提案で **追加** されたテスト（DSL → Diff 算出、ID 自動採番、Modifier 適用等）を新規追加する形で並存させる

### トレードオフ

- DSL 方式は Swift 値型レイヤで body 再評価のたびに値型再構築が走る（典型用途では実用上問題なし）vs Store 方式の極限性能
- API サーフェスが増える（DSL init + Store init + Modifier API）vs ドキュメントでの使い分け案内の必要性
- 内部 Store を `@StateObject` / `remember` で保持する設計 → View identity に依存するが、SwiftUI / Compose 流儀には合致
- `.cellID(_:)` / `.sectionID(_:)` の明示 API → 「ID を意識させない」原則から外れる場面があるが、上級者向け逃げ道として必要

## Migration Plan

本提案は DSL API の追加（純粋な機能追加）に加え、`.header(...)` / `.footer(...)`（SwiftUI）および `headerView` / `footerView`（Compose）パラメータの **BREAKING 削除** を含む。本ライブラリは運用前であり外部利用者は存在しないため、互換維持は不要と判断し即時削除する。既存 `KsSettingsView(store:)` の Store 方式 API シグネチャ自体は維持される。

### 開発フェーズ

1. `add-partial-update-core` を archive
2. `add-partial-update-native` を archive
3. 本提案の実装に着手（iOS / Compose 並行）
4. iOS Sample に DSL 方式デモ画面を追加（既存 Store 方式デモ画面は維持し、両方並存）
5. Android Sample に同等の DSL 方式デモ画面を追加（同上、既存 Store 方式デモは維持）
6. `add-partial-update-native` で導入された `.header(...)` / `.footer(...)` / `headerView` / `footerView` を本ライブラリ内のすべての参照箇所（Sample・テスト・docs）から書き換える
7. `docs/declarative-dsl-guide.md` 公開
8. 本提案を archive

### 内部利用者（リポジトリ内）への影響

- 既存の `KsSettingsView(store: store)` 呼び出しはシグネチャが維持されるため、Store 方式利用箇所は無修正で動作する
- `.header(...)` / `.footer(...)` modifier を使っている Sample / テストコードは本提案で削除されるため、`.rootHeader(...)` / `.rootFooter(...)` への書き換えが必要（ビルドエラーで検出可能）
- `headerView` / `footerView` パラメータを使っている Sample / テストコードも同様に書き換えが必要

### ロールバック戦略

- 本提案で追加された DSL init / Modifier API は `if false` で無効化できる構造で実装し、緊急時は単体でビルド除外可能とする
- BREAKING 削除部分のロールバックは `add-partial-update-native` の archive 取り消しが必要となる。基本的にロールバック想定はしない（運用前のため）
- 既存 Store 方式 API は維持されているため、DSL 方式が問題を起こした場合でも Store 方式に戻して稼働継続できる

## Open Questions

- **Modifier の戻り型**: `.font(...).icon(...)` のメソッドチェーンで `Self` を返すか、共通の `CellBase` プロトコル / 型を返すか。`Self` 返却の方が型情報が保持されるが、Cell 種別ごとにすべての Modifier をオーバーロードする実装コストがある。実装着手時に確定する
  - **確定（オーナーレビュー対応）**: iOS は `Self` 返却（既存実装通り）。Compose は **`CellHandle` 経由 chain** に統一し、`CellHandle` を共通の戻り型とする（具象 Cell 型情報は handle 内部に保持）
- **`.cellID(_:) / .sectionID(_:)` の引数型**: `String` / `AnyHashable` / `UUID` のいずれが最も使いやすいか。`AnyHashable` が型柔軟性で優れるが、`String` が最もシンプル。実装着手時にユーザーフィードバックを取りながら確定する
  - **確定**: iOS は `AnyHashable`、Compose は `Any`（既存実装通り、内部で `String` に変換）
- **ForEach の `Range` 版（`ForEach(0..<10) { ... }` 形式）の必要性**: SwiftUI 本家は `Range<Int>` 専用オーバーロードも提供している。本提案では Identifiable / id KeyPath の 2 種類で十分か、Range も追加するか
  - **確定**: 本提案では Range 版は提供しない（Non-Goals に明記済）
- **本オーナーレビューでの追加確定事項**:
  - Compose の Section H/F は `SectionHandle` 経由 modifier chain（iOS と並列）
  - Compose の Cell 直置きは「具象 Cell 型ごとの DSL 拡張関数」+ 補助の `unaryPlus`
  - Compose の Cell modifier は `CellHandle` 経由 chain（`.cellHeight(80.dp)` 等）
  - Compose の `forEach` は `key` lambda 版（Compose 公式作法）と `KsIdentifiable` 版（iOS 並列）を併存
  - Compose の Root H/F は引数指定維持（Compose イディオム尊重の意図的な非対称）
  - 具象 Cell の `id` パラメータはデフォルト値（UUID ベース）を持たせ、利用者が DSL 内で `id` を意識しなくて済むようにする
