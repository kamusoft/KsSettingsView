## MODIFIED Requirements

### Requirement: KsSettingsViewController の公開 API

`KsSettingsViewController` は `UIViewController` を継承し、`SettingsRootStore` 経由での部分更新と `applyDiff(_:)` メソッドにより内部 `UICollectionView` のスナップショットを更新しなければならない (SHALL)。本コントローラは UIKit 利用者および MAUI バインディング、SwiftUI ラッパから直接利用される (MUST)。`root: SettingsRoot` の公開 setter は廃止し (MUST NOT)、内部状態は `SettingsRootStore` または `applyDiff(_:)` 経由でのみ更新可能としなければならない (MUST)。Preview / Test 向けに `internal init(root: SettingsRoot, theme: Theme = Theme(), style:, registry:)` を提供しなければならない (MUST)。

`KsSettingsViewController` は Root H/F 用プロパティ `public var rootHeader: RootAccessory?` および `public var rootFooter: RootAccessory?` を持たなければならない (MUST)。setter で boundary supplementary item の構成を更新する。

**Theme 経路**: Controller は `SettingsRootStore` の `theme` Publisher を購読するか、または `public func applyTheme(_ theme: Theme)` メソッド経由で Theme を受け取らなければならない (MUST)。**Theme は `SettingsRoot` には含まれないため、Theme 更新は `applyDiff(_:)` 経路ではなく独立した `applyTheme(_:)` 経路を通る (MUST)**。`SettingsRoot(sections: [...])` には `theme` 引数は存在しない (MUST NOT)。

#### Scenario: Store 経由の初期化

- **GIVEN** `let store = SettingsRootStore(initialRoot: ..., initialTheme: someTheme)` で生成した Store
- **WHEN** `KsSettingsViewController(store: store, style: .classic)` を初期化する
- **THEN** Controller は Store の初期 root で内部スナップショットを構築し、Store の `diffPublisher` および `theme` Publisher を購読する

#### Scenario: Store メソッド呼び出しで表示が更新

- **GIVEN** `KsSettingsViewController(store: store)` が画面表示中
- **WHEN** `store.insertCell(newCell, in: sectionID, at: 0)` を呼ぶ
- **THEN** Controller は対応する `SettingsRootDiff.insertCell` を購読経路で受け取り、`applyDiff(_:)` を介して内部 snapshot に 1 件だけ Cell を追加する

#### Scenario: applyDiff の直接呼び出し

- **GIVEN** `KsSettingsViewController` インスタンス
- **WHEN** `controller.applyDiff(.removeCell(cellID: someID))` を呼ぶ
- **THEN** 対象 Cell が snapshot から削除され、UICollectionView に削除アニメーションが反映される

#### Scenario: applyTheme の直接呼び出し

- **GIVEN** `KsSettingsViewController` インスタンスが表示中
- **WHEN** `controller.applyTheme(Theme(separatorColor: .systemGray3))` を呼ぶ
- **THEN** `UICollectionView.backgroundColor` および各表示中 Cell の実効スタイルが新 Theme に基づいて再評価される。`SettingsRootDiff` Publisher は何も発行しない

#### Scenario: Preview / Test 用 internal init

- **GIVEN** `let controller = KsSettingsViewController(root: SettingsRoot(sections: [...]), theme: Theme())` （internal init）
- **WHEN** インスタンスを生成する
- **THEN** Store を介さず直接 `SettingsRoot` と `Theme` を受け取って初期スナップショットを構築できる

#### Scenario: 初期化直後の状態

- **GIVEN** `KsSettingsViewController(store: SettingsRootStore(initialRoot: SettingsRoot(sections: [])))` を初期化した直後
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

### Requirement: SettingsRootStore（iOS）

`KsSettingsViewUI` モジュールは、`SettingsRoot` の状態管理と部分更新 Diff 発行を担う `SettingsRootStore` クラスを提供しなければならない (SHALL)。`SettingsRootStore` は `@MainActor public final class : ObservableObject` であり、`@Published public private(set) var root: SettingsRoot` プロパティで現在の root を公開しなければならない (MUST)。**併せて `@Published public private(set) var theme: Theme` プロパティで現在の Theme も公開しなければならない (MUST)**。内部に `SettingsRootDiff` を発行する Publisher（Combine `PassthroughSubject` 等）を持ち、UI 層 Controller がこれを購読することで `applyDiff(_:)` を呼ぶ統合経路を確立しなければならない (MUST)。

`SettingsRootStore` は以下のメソッドを公開しなければならない (MUST)：

- `init(initialRoot: SettingsRoot, initialTheme: Theme = Theme())`
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
- `func applyTheme(_ theme: Theme)`

**`applyTheme(_:)` は Diff Publisher 経路を通らない (MUST NOT)**。代わりに `@Published var theme` を更新し、購読者（Controller / SwiftUI ラッパ）が Theme 変更通知を受けて View に反映する。

Preview / Test 用ファクトリとして `static func preview(root: SettingsRoot, theme: Theme = Theme()) -> SettingsRootStore` を提供しなければならない (MUST)。

#### Scenario: Store の初期化と root / theme 取得

- **GIVEN** `let initial = SettingsRoot { Section { ... } }` と `let theme = Theme(separatorColor: .systemGray3)`
- **WHEN** `let store = SettingsRootStore(initialRoot: initial, initialTheme: theme)` を構築する
- **THEN** `store.root` は `initial` と等価、`store.theme` は `theme` と等価になる

#### Scenario: Store の Theme 省略時の初期化

- **GIVEN** `let initial = SettingsRoot { Section { ... } }`
- **WHEN** `let store = SettingsRootStore(initialRoot: initial)` を構築する
- **THEN** `store.theme` は `Theme()` の既定値になる

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

#### Scenario: applyTheme メソッド呼び出し

- **GIVEN** Store が初期化済み、現在 Theme は既定値
- **WHEN** `store.applyTheme(Theme(separatorColor: .systemGray3))` を呼ぶ
- **THEN** `store.theme` が新 Theme に更新され、`@Published var theme` が変更通知を発行する。`SettingsRootDiff` Publisher は何も発行しない

#### Scenario: preview ファクトリの利用

- **GIVEN** Preview コードで `let store = SettingsRootStore.preview(root: SettingsRoot { ... })`
- **WHEN** `KsSettingsView(store: store)` を Preview に表示する
- **THEN** 通常の Store と同じ動作で Preview が表示される

#### Scenario: @Published の通知

- **GIVEN** Store と SwiftUI View（`@ObservedObject` で監視）
- **WHEN** `store.insertCell(...)` を呼ぶ
- **THEN** SwiftUI View は `@Published` の変更通知を受け取り、再評価される（必要に応じて）
