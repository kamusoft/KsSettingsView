# settings-view-ios-host Specification

## Purpose

`settings-view-ios-host` は、`KsSettingsViewUI`（iOS）の **ホスト層**（`KsSettingsViewController` 本体・`UICollectionView` + `UICollectionViewDiffableDataSource` ベースのリスト基盤・Cell レジストリと `KsCellRenderer` プロトコル・`SettingsRootStore`・ライフサイクル管理）を担う capability である。`KsSettingsViewCore` のドメインモデル (`SettingsRoot` / `Section` / `Cell`) を入力として、構造変化（追加・削除・移動）は DiffableDataSource の snapshot 適用で、内容変化は `reconfigureItems` の経路で表現するための土台を定義する。スタイル切替（クラシック/モダン）や Section H/F・罫線・余白・行高さといった見た目の詳細は `settings-view-ios-style`、Theme/CellStyle 変換は `settings-view-ios-theme-bridge`、SwiftUI ラッパ・DSL は `settings-view-ios-swiftui` に分離されている。本 UI 層は UIKit、SwiftUI（`UIViewControllerRepresentable` 経由）、および MAUI バインディングから利用されることを前提とする。

## Requirements
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

### Requirement: DiffableDataSource

`KsSettingsViewController` は内部で `UICollectionViewDiffableDataSource<UUID, KsCellID>` を保持しなければならない (SHALL)（Section 識別子は `UUID`、Item 識別子は `KsCellID` で `Hashable`）。スナップショットの**構造同期（Section / Item の追加・削除・移動・差し替え）は `KsCellID`（id）の同一性のみで算出されなければならない** (MUST)。Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）の `Hashable` 等価性を構造同期（snapshot の item 集合・順序の再構築）の判定に用いてはならない (MUST NOT)（「表示状態同期の三層分離」参照）。装飾領域（Section H/F、Root H/F）の `KsAnyView` は差分検出に参加せず、`SettingsRoot` / `Section` 等の `Hashable` 実装は `view` ケースの中身を判定対象外として扱わなければならない (MUST)。

`KsSettingsViewController` は `cellIndex: [KsCellID: any KsCell]` 等により id → 最新 Cell モデルのマップを保持し、`cellProvider` はこのマップから最新 Cell を引いて描画しなければならない (MUST)。

`applyDiff(_:)` API は受け取った `SettingsRootDiff` のケースに応じて、`NSDiffableDataSourceSnapshot` の構造操作（`insertItemsBefore` / `deleteItems` / `moveItemBefore` / `moveItemAfter` / `appendSections` / `deleteSections` / `moveSection` 等）を実行しなければならない (MUST)。同一 id の Cell の**内容更新（`replaceCell`）は、セルを破棄・再生成する `reloadItems` ではなく `reconfigureItems`（iOS 15+、同一セルインスタンスを破棄せず `cellProvider` で再構成）で反映しなければならない** (MUST)。`reloadItems` を内容更新に用いてはならない (MUST NOT)。Deployment Target が iOS 15 未満を含み `reconfigureItems` が利用できない場合に限り `reloadItems` へフォールバックしてよい (MAY)。

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

#### Scenario: 内容更新は reconfigureItems で反映（セル破棄なし）

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）が変化し、`controller.applyDiff(.replaceCell(cellID: cid, new: updated))` が呼ばれる
- **WHEN** スナップショット適用を観察する
- **THEN** `cellIndex` の当該 Cell が更新され、`snapshot.reconfigureItems([cid])` により同一セルインスタンスが破棄されずに再構成される。セルの破棄・再生成（reload）やそれに伴うちらつきは発生しない

#### Scenario: 存在しない cellID への操作（DEBUG）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** DEBUG ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** `assertionFailure(...)` で即座にクラッシュする

#### Scenario: 存在しない cellID への操作（Release）

- **GIVEN** snapshot に存在しない `cellID` を持つ `removeCell` Diff
- **WHEN** Release ビルドで `controller.applyDiff(.removeCell(cellID: notExistID))` を呼ぶ
- **THEN** クラッシュせず、`os_log` 等でログ出力されるのみで snapshot は変更されない

#### Scenario: チェック系の TwoWay トグル

- **GIVEN** Checkbox / Switch / SimpleCheck の CellView が表示されている
- **WHEN** ユーザーがセルをタップ／操作する
- **THEN** CellView が自身の表示状態を直接更新し `onValueChanged` でモデルへ書き戻す（TwoWay）。この内容更新は snapshot の構造再構築を経由せず、行全体の再描画は発生しない。RadioCell のグループ連動（他セルの選択解除）は該当セルの reconfigure で反映する

### Requirement: Cell レジストリ

`KsCellRegistry` は具象 Cell 型から `UICollectionViewCell` サブクラスへの解決を担う中央レジストリでなければならない (SHALL)。`KsCellRenderer` プロトコルを実装する `UICollectionViewCell` サブクラスをアプリ起動時に登録できなければならない (MUST)。

#### Scenario: Cell 型の登録と解決

- **GIVEN** `KsCellRegistry` が初期化済み
- **WHEN** `registry.register(cellType: MyCell.self, rendererType: MyCellView.self)` を呼ぶ
- **THEN** 以後 `MyCell` を含む snapshot 適用時に `MyCellView` が `dequeueReusableCell` され、`KsCellRenderer.render(cell:theme:)` が呼ばれる

#### Scenario: 未登録 Cell の扱い

- **GIVEN** `KsCellRegistry` に未登録の Cell が渡される
- **WHEN** スナップショット適用を試みる
- **THEN** 開発時は assertion failure（DEBUG ビルドのみ）、リリース時はプレースホルダ Cell（背景色違いの空セル）を返してアプリクラッシュを防ぐ

### Requirement: KsCellRenderer プロトコル

`KsCellRenderer` は具象 `UICollectionViewCell` サブクラスが実装すべきプロトコルでなければならない (SHALL)。任意の `KsCell` 準拠の Cell と `Theme` を受け取って描画する `render(cell:theme:)` 形式の関数（associatedtype 経由 / 型消去経由のいずれかは実装で決定）を要求しなければならない (MUST)。

#### Scenario: render の呼び出し

- **GIVEN** `KsCellRenderer` 準拠の `UICollectionViewCell` サブクラス
- **WHEN** DataSource が当該 Cell を `dequeueReusableCell` し snapshot 適用する
- **THEN** Cell ごとの `render` 関数が呼ばれ、Cell が描画される

#### Scenario: prepareForReuse でのクリーンアップ

- **GIVEN** `KsCellRenderer` 準拠 Cell が一度 render された後再利用される
- **WHEN** `prepareForReuse()` が UIKit から呼ばれる
- **THEN** Cell 内のサブビュー・テキスト・画像参照がリセットされ、再 `render` 時に古い状態が表示されない

### Requirement: メモリリーク防止

`KsSettingsViewController` および `KsSettingsView` は `deinit` 時に内部 `UICollectionView` の DataSource、Delegate、registered Cell の参照、および Store の Diff Publisher 購読をすべて解放しなければならない (MUST)。SwiftUI ラッパの Coordinator が Store を強参照する場合、Controller が破棄された時点で購読が解除されること。

**DSL 方式での `@StateObject` 内部 Store のライフサイクル**: `KsSettingsView` の View identity が維持される間は内部 Store も保持され、View が破棄されると Store も解放されなければならない (MUST)。`@StateObject` の標準的なライフサイクル（SwiftUI Runtime 管理）に従う。

#### Scenario: ViewController が deinit される

- **GIVEN** `KsSettingsViewController` を `present` したのち `dismiss` する
- **WHEN** 親 ViewController から開放後 1 ランループ以上経過する
- **THEN** `KsSettingsViewController` インスタンスは deinit され、`weak var` で保持していた参照が `nil` になる

#### Scenario: Store 購読の解除

- **GIVEN** `KsSettingsViewController(store: store)` が deinit される
- **WHEN** Controller の deinit を観察する
- **THEN** Store の Diff Publisher 購読は解除され、Controller への参照が残らない（Store が長命であっても Controller がリークしない）

#### Scenario: DSL 方式の内部 Store が View 破棄時に解放される

- **GIVEN** `KsSettingsView { Section { ... } }` を含む View が画面に表示中
- **WHEN** 親 View 階層から `KsSettingsView` が外れ、View identity が失われる
- **THEN** 内部 `@StateObject` の Store も解放される（`@StateObject` の標準ライフサイクル）

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

### Requirement: visible projection の二重管理

`KsSettingsViewController` は、`SettingsRoot` を hidden 含むフル状態として保持しつつ、UI 描画には visible projection（`Section.isVisible` および各 Cell の `VisibilityAware.isVisible` が `true` のもののみで構成される派生ビュー）を用いなければならない (MUST)。

具体的には、以下の UI 描画経路で参照される sections は **visible projection ベース** でなければならない (MUST)：

- `UICollectionViewCompositionalLayout` の `sectionProvider` クロージャ
- セクションヘッダ／フッタ supplementary view の生成
- 罫線（separator）の `itemSeparatorHandler` 経路
- `NSDiffableDataSourceSnapshot` の section / item 構築

一方、以下の経路は **model（`root.sections`）ベース** でなければならない (MUST)：

- `SettingsRootDiff` 受信時の対象 Section / Cell の探索（hidden 対象を見つけられないと no-op 判定ができない）
- 部分 Diff の `index` 引数の解釈

#### Scenario: visible projection で section が除外される

- **GIVEN** `SettingsRoot.sections` に `isVisible = false` の Section を含む
- **WHEN** `KsSettingsViewController` が snapshot を構築する
- **THEN** 当該 Section は snapshot の section identifiers に含まれず、UI 上には描画されない。一方で `root.sections` には保持される

#### Scenario: visible projection で cell が除外される

- **GIVEN** visible な Section の `cells` に `isVisible = false` の Cell（`VisibilityAware` 準拠）を含む
- **WHEN** `KsSettingsViewController` が snapshot を構築する
- **THEN** 当該 Cell は snapshot の item identifiers に含まれず、UI 上には描画されない

#### Scenario: indexPath ベースの描画経路が visible projection を参照する

- **GIVEN** hidden な Section が `root.sections` の先頭にあり、その後ろに visible な Section が続く構成
- **WHEN** `indexPath.section = 0` で separator / supplementary view を生成する
- **THEN** 参照される Section は visible projection の先頭（= root.sections[1] にあたる visible Section）であり、hidden Section が参照されることはない

### Requirement: 部分 Diff の index 規約と hidden 対象の no-op

`KsSettingsViewController.applyDiff(_:)` は、`SettingsRootDiff` の部分 Diff ケース（`insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `replaceCell` / `moveCell` / `updateAccessory`）について、以下の規約に従わなければならない (MUST)。

**index 引数の解釈:**

部分 Diff の `index` / `at` / `to` 引数は、すべて **model 配列基準（hidden 含む）** で解釈しなければならない (MUST)。visible projection 基準で解釈してはならない (MUST NOT)。

**hidden 対象の挙動:**

- 対象 Section / Cell が hidden の場合、UI 層は当該 Diff のために `NSDiffableDataSourceSnapshot` への構造操作（`insertSections` / `deleteSections` / `insertItems` / `deleteItems` 等）を行ってはならない (MUST NOT)。
- model（`root`）の更新は実行しなければならない (MUST)。これにより、後で `isVisible = true` に切り替わったときに正しい状態で復活する。
- hidden 対象を指す `removeCell` / `moveCell` / `updateAccessory` 等が snapshot 上の missing ID として観測される場合、エラーや警告として扱わず通常の no-op として処理しなければならない (MUST)。

**visible 対象の挙動:**

- 対象 Section / Cell が visible の場合、UI 層は通常通り snapshot 操作を行わなければならない (MUST)。
- 部分 Diff の `index` 引数（model 基準）から visible projection 上の正しい位置を算出して snapshot に反映しなければならない (MUST)。

#### Scenario: hidden Cell への removeCell は no-op

- **GIVEN** `Section` に `isVisible = false` の Cell を含み、その Cell に対する `SettingsRootDiff.removeCell(cellID)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model（`root`）から当該 Cell が削除される一方、snapshot への構造操作は行われない。エラーや警告は発生しない

#### Scenario: hidden Section への updateAccessory は no-op だが model は更新される

- **GIVEN** `isVisible = false` の Section に対する `updateAccessory` Diff が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 上の当該 Section の `header` / `footer` は更新される一方、snapshot への構造操作は行われない。後で当該 Section が `isVisible = true` に切り替わったとき、更新済みの accessory が描画される

#### Scenario: insertCell の index は model 配列基準

- **GIVEN** `Section.cells` が `[A(visible), B(hidden), C(visible)]` の状態で `insertCell(sectionID:, at: 2, cell: D)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 配列基準で index = 2 の位置（= C の前）に D が挿入され、`Section.cells` は `[A, B, D, C]` となる。visible projection 上では `[A, D, C]` として描画される

#### Scenario: moveCell で hidden を跨ぐ移動

- **GIVEN** `Section.cells` が `[A(visible), B(hidden), C(visible), D(visible)]` で `moveCell(cellID: A, to: 3)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** model 配列基準で A が index 3 に移動し、`Section.cells` は `[B, C, D, A]` となる。visible projection 上では `[C, D, A]` として描画される

### Requirement: ReplaceCell / ReplaceSection の可視性切替防御

DSL / アプリ層は、`SettingsRootDiff.replaceCell` / `SettingsRootDiff.replaceSection` で可視性（`isVisible`）だけを変える操作を行ってはならない (MUST NOT)。可視性変更は `SettingsRootDiff.full(newRoot)` 経由で発行されなければならない (MUST)。

UI 層 (`KsSettingsViewController`) は、受け取った `replaceCell` で旧 Cell と新 Cell の `isVisible` が異なることを **`root.sections` から取得した旧値で** 検出した場合、Full 経路（`applyFullSnapshot` 相当）にフォールバックしなければならない (MUST)。検出は snapshot の存在チェックよりも先に行わなければならず (MUST)、旧 Cell が hidden であっても model 上から取得した旧値で判定できなければならない (MUST)。

UI 層は、受け取った `replaceSection` を常に Full 経路（`applyFullSnapshot` 相当）で処理しなければならない (MUST)。`replaceSection` は型上 Section 全体置換であり、`header` / `footer` / `headerHeight` / `isVisible` / `cells` の任意の変化を内包し得るため、内部 cell の細粒度差分抽出を試みてはならない (MUST NOT)。

#### Scenario: ReplaceCell で visibility 切替が検出される

- **GIVEN** model 上の Cell `X` が `isVisible = true` で、新 Cell `X'`（同一 id、`isVisible = false`）を伴う `replaceCell(cellID: X, new: X')` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は可視性切替を検出し、Full 経路にフォールバックする。snapshot は再構築され、当該 Cell は visible projection から除外される

#### Scenario: ReplaceSection は常に Full 経路で処理される

- **GIVEN** `replaceSection(sectionID:, new: newSection)` が発行される
- **WHEN** `applyDiff` が処理する
- **THEN** UI 層は内部の cells / accessory / visibility の細粒度差分を抽出せず、Full 経路にフォールバックする。snapshot は新 Section の visible projection に基づいて再構築される

### Requirement: partial Section / UpdateAccessory の supplementary 追従

`KsSettingsViewController` は、Section の追加・削除・置換・移動および `updateAccessory`（Section H/F 対象）の処理後、visible projection の最新状態が **section ごとの header / footer 表示有無として正しく反映** されることを保証しなければならない (MUST)。判定対象は **visible projection** であり、hidden Section の header / footer は judging に影響してはならない (MUST NOT)。

実装方式は (a) `UICollectionViewLayout` インスタンスを差し替える、(b) 同一 layout インスタンス内で `sectionProvider` クロージャが section ごとに supplementary 構成を動的に決定する、のいずれでもよい。ただし `UICollectionViewDiffableDataSource` の差分アニメーション中に `setCollectionViewLayout(_:animated:)` を同期実行すると、Compositional Layout 側の section 構造再構築と DiffableDataSource の section アニメが衝突し、全 Cell バウンドや描画乱れを招くことが確認されている。したがって UI 層は、**`dataSource.apply(_:animatingDifferences:)` の差分アニメーションと同じ更新サイクル内で `setCollectionViewLayout(_:animated:)` を呼んではならない** (MUST NOT)。

#### Scenario: hidden Section の header は判定に影響しない

- **GIVEN** `[S1(visible, header=nil), S2(hidden, header="A")]` の構成
- **WHEN** visible projection の header 有無を判定する
- **THEN** visible projection は `[S1]` のみで構成され、header を持つ visible section が無いと判定される。S2 の header は判定に影響しない

#### Scenario: partial Diff で visibility が変化したら supplementary が追従する

- **GIVEN** すべての visible section が header を持たない状態で、`insertSection(at: ..., section: newSection)` により header を持つ新 visible section が追加される
- **WHEN** `applyDiff` が処理する
- **THEN** 追加された Section が描画されるとき、その header supplementary view が正しく表示される。既存 visible section の表示状態は変化しない

#### Scenario: 差分アニメと layout 差し替えの同時実行は禁止

- **GIVEN** `applyFullSnapshot` / 部分 Diff のいずれかが visibility 変化を伴い、`dataSource.apply(_, animatingDifferences: true)` が呼ばれる
- **WHEN** 同じ更新サイクル内で `setCollectionViewLayout(_, animated: false)` を併用する
- **THEN** これは禁止であり (MUST NOT)、UI 層は同期差し替えを避けて supplementary 追従を実現しなければならない（例: `sectionProvider` の動的評価 + `invalidateLayout()` 等）

<!--
Migration:
旧 `@Binding<SettingsRoot>` API は MODIFIED `SwiftUI ラッパ KsSettingsView` Requirement の変更後全文に含まれない形で削除している。利用者は `@State private var root: SettingsRoot` を `@StateObject private var store: SettingsRootStore` に書き換え、`KsSettingsView(root: $root)` を `KsSettingsView(store: store)` に置き換える。動的更新は `root = newRoot` ではなく `store.replaceAll(newRoot)` または部分更新メソッド（`store.insertCell(...)` 等）を使う。

同様に、旧 `KsSettingsViewController.root` 公開 setter は MODIFIED `KsSettingsViewController の公開 API` Requirement の変更後全文で削除されている。直接代入していたコードは `KsSettingsViewController(store: store)` 経由か、Test 限定で `internal init(root:)` を使う形に書き換える。

旧 Requirement「Root H/F（SettingsRoot.header / footer）の描画」は MODIFIED「Root H/F（UI 層プロパティ）の描画」に置き換えている。
-->

