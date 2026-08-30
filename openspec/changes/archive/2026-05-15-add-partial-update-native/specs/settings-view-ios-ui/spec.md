## MODIFIED Requirements

### Requirement: KsSettingsViewController の公開 API

`KsSettingsViewController` は `UIViewController` を継承し、`SettingsRootStore` 経由での部分更新と `applyDiff(_:)` メソッドにより内部 `UICollectionView` のスナップショットを更新しなければならない (SHALL)。本コントローラは UIKit 利用者および MAUI バインディング、SwiftUI ラッパから直接利用される (MUST)。`root: SettingsRoot` の公開 setter は廃止し (MUST NOT)、内部状態は `SettingsRootStore` または `applyDiff(_:)` 経由でのみ更新可能としなければならない (MUST)。Preview / Test 向けに `internal init(root: SettingsRoot, style:, registry:)` を提供しなければならない (MUST)。

`KsSettingsViewController` は Root H/F 用プロパティ `public var rootHeader: RootAccessory?` および `public var rootFooter: RootAccessory?` を持たなければならない (MUST)。setter で boundary supplementary item の構成を更新する。

#### Scenario: Store 経由の初期化

- **GIVEN** `let store = SettingsRootStore(initialRoot: ...)` で生成した Store
- **WHEN** `KsSettingsViewController(store: store, style: .classic)` を初期化する
- **THEN** Controller は Store の初期 root で内部スナップショットを構築し、Store の `diffPublisher` を購読する

#### Scenario: Store メソッド呼び出しで表示が更新

- **GIVEN** `KsSettingsViewController(store: store)` が画面表示中
- **WHEN** `store.insertCell(newCell, in: sectionID, at: 0)` を呼ぶ
- **THEN** Controller は対応する `SettingsRootDiff.insertCell` を購読経路で受け取り、`applyDiff(_:)` を介して内部 snapshot に 1 件だけ Cell を追加する

#### Scenario: applyDiff の直接呼び出し

- **GIVEN** `KsSettingsViewController` インスタンス
- **WHEN** `controller.applyDiff(.removeCell(cellID: someID))` を呼ぶ
- **THEN** 対象 Cell が snapshot から削除され、UICollectionView に削除アニメーションが反映される

#### Scenario: Preview / Test 用 internal init

- **GIVEN** `let controller = KsSettingsViewController(root: SettingsRoot(sections: [...], theme: Theme()))` （internal init）
- **WHEN** インスタンスを生成する
- **THEN** Store を介さず直接 `SettingsRoot` を受け取って初期スナップショットを構築できる

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsViewController(store: SettingsRootStore(initialRoot: SettingsRoot(sections: [], theme: Theme())))` を初期化した直後
- **WHEN** `viewDidLoad()` 完了時点を確認する
- **THEN** 内部 `UICollectionView` および空 `SettingsRoot` 相当のスナップショットが構成され、エラーなく `present` できる

#### Scenario: rootHeader の設定

- **GIVEN** `KsSettingsViewController` インスタンス
- **WHEN** `controller.rootHeader = .text("プロフィール")` を代入する
- **THEN** UICollectionView 上端に boundary supplementary item として "プロフィール" が描画される

#### Scenario: rootHeader を nil にすると boundary 削除

- **GIVEN** `controller.rootHeader = .text("X")` で描画中
- **WHEN** `controller.rootHeader = nil` を代入する
- **THEN** boundary supplementary item が boundary 構成から削除され、レイアウトが再構築される

### Requirement: SwiftUI ラッパ KsSettingsView

`KsSettingsView` は `UIViewControllerRepresentable` に準拠し、SwiftUI から `KsSettingsViewController` を直接利用できなければならない (SHALL)。`SettingsRootStore` を受け取る `init(store: SettingsRootStore, style:)` を公開しなければならない (MUST)。`@Binding<SettingsRoot>` を受け取る旧 init は廃止しなければならない (MUST NOT)。Root H/F は `.header(_ accessory: RootAccessory?)` / `.footer(_ accessory: RootAccessory?)` の View modifier 形式で指定可能でなければならない (MUST)。

#### Scenario: Store 渡しでの初回作成

- **GIVEN** SwiftUI View 内で `@StateObject var store = SettingsRootStore(initialRoot: ...)` 宣言
- **WHEN** `KsSettingsView(store: store)` を body から返す
- **THEN** SwiftUI は `makeUIViewController(context:)` を呼び、`KsSettingsViewController(store: store, style: .classic)` が生成される

#### Scenario: Store メソッド呼び出しで再描画

- **GIVEN** `KsSettingsView(store: store)` が画面表示中
- **WHEN** ボタン押下などで `store.insertCell(newCell, in: sectionID, at: 0)` を呼ぶ
- **THEN** Controller は Store の Diff Publisher を購読しており、`applyDiff(.insertCell(...))` が呼ばれて新しい Cell 行が挿入アニメーションで追加される

#### Scenario: header modifier の適用

- **GIVEN** SwiftUI View 内で `KsSettingsView(store: store).header(.text("プロフィール"))` を記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `rootHeader` プロパティが `.text("プロフィール")` に設定される

#### Scenario: footer modifier の適用

- **GIVEN** SwiftUI View 内で `KsSettingsView(store: store).footer(.view(KsAnyView.swiftUI { Text("v1.0.0") }))` を記述
- **WHEN** SwiftUI が `updateUIViewController(_:context:)` を呼ぶ
- **THEN** Controller の `rootFooter` プロパティが `.view(...)` に設定される

### Requirement: DiffableDataSource

`KsSettingsViewController` は内部で `UICollectionViewDiffableDataSource<UUID, KsCellID>` を保持しなければならない (SHALL)（Section 識別子は `UUID`、Item 識別子は `KsCellID` で `Hashable`）。スナップショット差分は `Hashable` の等価性で算出されなければならない (MUST)。装飾領域（Section H/F、Root H/F）の `KsAnyView` は差分検出に参加せず、`SettingsRoot` / `Section` 等の `Hashable` 実装は `view` ケースの中身を判定対象外として扱わなければならない (MUST)。`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、`NSDiffableDataSourceSnapshot` の部分操作（`insertItemsBefore` / `deleteItems` / `moveItemBefore` / `moveItemAfter` / `reloadItems` / `appendSections` / `deleteSections` / `moveSection` 等）を実行しなければならない (MUST)。

#### Scenario: Cell 追加時のアニメーション

- **GIVEN** `controller.applyDiff(.insertCell(sectionID: sid, at: 0, cell: newCell))`
- **WHEN** スナップショット適用を観察する
- **THEN** snapshot に 1 件だけ新しい `KsCellID` が挿入され、その Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除時のアニメーション

- **GIVEN** `controller.applyDiff(.removeCell(cellID: cid))`
- **WHEN** スナップショット適用を観察する
- **THEN** snapshot から該当 `KsCellID` が削除され、その Cell 行が削除アニメーションで消える

#### Scenario: Section 移動時の挙動

- **GIVEN** Section が 3 つ並んでいる状態
- **WHEN** `controller.applyDiff(.moveSection(from: 0, to: 2))` を呼ぶ
- **THEN** snapshot 上の Section 順序が移動先に反映され、Section とその Cell が一体で移動アニメーションする

#### Scenario: Theme 更新

- **GIVEN** `controller.applyDiff(.updateTheme(newTheme))`
- **WHEN** 適用後の描画を観察する
- **THEN** すべての可視 Cell の `KsCellRenderer.render(cell:theme:)` が新 Theme で再呼び出しされる

#### Scenario: 存在しない cellID への操作（DEBUG）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** DEBUG ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** `assertionFailure(...)` で即座にクラッシュする

#### Scenario: 存在しない cellID への操作（Release）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** Release ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** クラッシュせず、`os_log` 等でログ出力されるのみで snapshot は変更されない

### Requirement: メモリリーク防止

`KsSettingsViewController` および `KsSettingsView` は `deinit` 時に内部 `UICollectionView` の DataSource、Delegate、registered Cell の参照、および Store の Diff Publisher 購読をすべて解放しなければならない (MUST)。SwiftUI ラッパの Coordinator が Store を強参照する場合、Controller が破棄された時点で購読が解除されること。

#### Scenario: ViewController が deinit される

- **GIVEN** `KsSettingsViewController` を `present` したのち `dismiss` する
- **WHEN** 親 ViewController から開放後 1 ランループ以上経過する
- **THEN** `KsSettingsViewController` インスタンスは deinit され、`weak var` で保持していた参照が `nil` になる

#### Scenario: Store 購読の解除

- **GIVEN** `KsSettingsViewController(store: store)` が deinit される
- **WHEN** Controller の deinit を観察する
- **THEN** Store の Diff Publisher 購読は解除され、Controller への参照が残らない（Store が長命であっても Controller がリークしない）

### Requirement: Root H/F（SettingsRoot.header / footer）の描画

`KsSettingsViewController` は `rootHeader: RootAccessory?` および `rootFooter: RootAccessory?` を UI 層プロパティとして持ち、`UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header"` / `"ks-root-footer"`、`alignment: .top` / `.bottom` で配置しなければならない (SHALL)。デフォルトで `pinToVisibleBounds = false`（スクロール追従）でなければならない (MUST)。`RootAccessory.text` ケースは `UIListContentConfiguration` ベースの文字列描画、`RootAccessory.view(KsAnyView)` ケースは `UIHostingConfiguration` または `addSubview` で描画しなければならない (MUST)。`rootHeader` / `rootFooter` が `nil` の場合は対応する supplementary item を boundary から省略しなければならない (MUST)。

`SettingsRoot` 値型自体には `header` / `footer` を含まないため (MUST NOT)、本 Requirement の入力は UI 層プロパティ（`controller.rootHeader` の代入、SwiftUI ラッパの `.header(...)` modifier、または `SettingsRootStore.updateAccessory(target: .rootHeader, accessory:)` Diff 経由）のみとする。

<!-- 注: `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、Root H/F の入力源を UI 層プロパティ（`KsSettingsViewController.rootHeader` / `rootFooter`、SwiftUI ラッパの `.header(...)` / `.footer(...)` modifier、`SettingsRootStore.updateAccessory(target: .rootHeader/.rootFooter, accessory:)` Diff 経由）に変更している。boundary supplementary item 配置・描画ロジック自体は維持される。Requirement 名は archive 済 spec との連続性を保つため変更しないが、説明文と Scenario は新 API に合わせて書き直している。 -->

#### Scenario: Root Header（text）の描画

- **GIVEN** `controller.rootHeader = .text("プロフィール")` を代入
- **WHEN** Controller が描画される
- **THEN** UICollectionView 上端に "プロフィール" の boundary supplementary view が表示される

#### Scenario: Root Footer（view、SwiftUI backing）の描画

- **GIVEN** `controller.rootFooter = .view(KsAnyView.swiftUI { Text("v1.0.0") })` を代入
- **WHEN** Controller が描画される
- **THEN** UICollectionView 下端に Text("v1.0.0") の boundary supplementary view が描画される

#### Scenario: Root H/F のスクロール追従

- **GIVEN** Root Header を持つ Controller が描画中
- **WHEN** UICollectionView を下方向にスクロールする
- **THEN** Root Header は画面上端に固定されず、コンテンツと共にスクロールアウトする（`pinToVisibleBounds = false` のデフォルト挙動）

#### Scenario: Root H/F が nil の場合

- **GIVEN** `controller.rootHeader = nil` および `controller.rootFooter = nil`
- **WHEN** Controller が描画される
- **THEN** boundary supplementary items は配置されず、既存の sections のみが描画される

#### Scenario: Store 経由の Accessory 更新

- **GIVEN** Store が初期化済み、Controller が `store` を購読中
- **WHEN** `store.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` を呼ぶ
- **THEN** Store が `.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` Diff を発行し、Controller の `applyDiff` が `rootHeader` を `.text("X")` に更新する

### Requirement: SwiftUI DSL

宣言的 DSL（`@resultBuilder` を用いた `SettingsRootBuilder`、`SectionBuilder`）を提供し、SwiftUI 内で Cell ツリーを構築できなければならない (SHALL)。DSL は `SettingsRoot` を生成する純粋関数として動作しなければならない (MUST)。`SettingsRoot` は Root H/F を保持しないため、DSL も `header` / `footer` 引数を取らない (MUST NOT)。

#### Scenario: DSL から SettingsRoot 構築

- **GIVEN** SwiftUI コード内で
  ```swift
  let root = SettingsRoot {
      Section("一般") { /* SampleLabelCell(title: "...") など */ }
  }
  ```
  と記述
- **WHEN** `root` を評価する
- **THEN** `SettingsRoot.sections` に 1 つの `Section` が含まれ、その `cells` に DSL で記述された Cell が並ぶ。`SettingsRoot` は `header` / `footer` プロパティを持たない

#### Scenario: Store の初期 root を DSL で構築

- **GIVEN** `@StateObject var store = SettingsRootStore(initialRoot: SettingsRoot { Section { ... } })`
- **WHEN** Store を Controller に渡す
- **THEN** Store の初期 root が DSL で構築されたものとなる

## ADDED Requirements

### Requirement: SettingsRootStore（iOS）

`KsSettingsViewUI` モジュールは、`SettingsRoot` の状態管理と部分更新 Diff 発行を担う `SettingsRootStore` クラスを提供しなければならない (SHALL)。`SettingsRootStore` は `@MainActor public final class : ObservableObject` であり、`@Published public private(set) var root: SettingsRoot` プロパティで現在の root を公開しなければならない (MUST)。内部に `SettingsRootDiff` を発行する Publisher（Combine `PassthroughSubject` 等）を持ち、UI 層 Controller がこれを購読することで `applyDiff(_:)` を呼ぶ統合経路を確立しなければならない (MUST)。

`SettingsRootStore` は以下のメソッドを公開しなければならない (MUST)：

- `init(initialRoot: SettingsRoot)`
- `func replaceAll(_ root: SettingsRoot)`
- `func insertSection(_ section: Section, at index: Int)`
- `func removeSection(sectionID: UUID)`
- `func moveSection(from: Int, to: Int)`
- `func replaceSection(sectionID: UUID, new: Section)`
- `func insertCell(_ cell: any KsCell, in sectionID: UUID, at index: Int)`
- `func removeCell(cellID: KsCellID)`
- `func replaceCell(cellID: KsCellID, new: any KsCell)`
- `func moveCell(cellID: KsCellID, to index: Int)`
- `func updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)`
- `func updateTheme(_ theme: Theme)`

Preview / Test 用ファクトリとして `static func preview(root: SettingsRoot) -> SettingsRootStore` を提供しなければならない (MUST)。

#### Scenario: Store の初期化と root 取得

- **GIVEN** `let initial = SettingsRoot { Section { ... } }`
- **WHEN** `let store = SettingsRootStore(initialRoot: initial)` を構築する
- **THEN** `store.root` は `initial` と等価になる

#### Scenario: insertCell メソッド呼び出し

- **GIVEN** Store が初期化済み、Section が 1 つ存在
- **WHEN** `store.insertCell(newCell, in: sectionID, at: 0)` を呼ぶ
- **THEN** `store.root` の該当 Section の `cells[0]` が `newCell` になり、内部 Diff Publisher が `.insertCell(sectionID:, at: 0, cell: newCell)` を発行する

#### Scenario: removeCell メソッド呼び出し

- **GIVEN** Store が初期化済み、Section に Cell が複数存在
- **WHEN** `store.removeCell(cellID: someID)` を呼ぶ
- **THEN** `store.root` の該当 Section から `cellID` を持つ Cell が除去され、Diff Publisher が `.removeCell(cellID: someID)` を発行する

#### Scenario: updateAccessory メソッド呼び出し

- **GIVEN** Store が初期化済み
- **WHEN** `store.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` を呼ぶ
- **THEN** Diff Publisher が `.updateAccessory(target: .rootHeader, accessory: .root(.text("X")))` を発行する

#### Scenario: preview ファクトリの利用

- **GIVEN** Preview コードで `let store = SettingsRootStore.preview(root: SettingsRoot { ... })`
- **WHEN** `KsSettingsView(store: store)` を Preview に表示する
- **THEN** 通常の Store と同じ動作で Preview が表示される

#### Scenario: @Published の通知

- **GIVEN** Store と SwiftUI View（`@ObservedObject` で監視）
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** SwiftUI View は `@Published` の変更通知を受け取り、再評価される（必要に応じて）

<!--
Migration:
旧 `@Binding<SettingsRoot>` API は MODIFIED `SwiftUI ラッパ KsSettingsView` Requirement の変更後全文に含まれない形で削除している。利用者は `@State private var root: SettingsRoot` を `@StateObject private var store: SettingsRootStore` に書き換え、`KsSettingsView(root: $root)` を `KsSettingsView(store: store)` に置き換える。動的更新は `root = newRoot` ではなく `store.replaceAll(newRoot)` または部分更新メソッド（`store.insertCell(...)` 等）を使う。

同様に、旧 `KsSettingsViewController.root` 公開 setter は MODIFIED `KsSettingsViewController の公開 API` Requirement の変更後全文で削除されている。直接代入していたコードは `KsSettingsViewController(store: store)` 経由か、Test 限定で `internal init(root:)` を使う形に書き換える。

旧 Requirement「Root H/F（SettingsRoot.header / footer）の描画」は MODIFIED「Root H/F（UI 層プロパティ）の描画」に置き換えている。
-->

