# Candidate: settings-view-ios-host

## 概念候補

### iOS Native Host (提案カテゴリ: platforms/)

#### 目的

`KsSettingsViewController` は `SettingsRoot` を UIKit のリストへ接続する公開 Host である。UIKit 利用者が直接組み込めるほか、SwiftUI の `KsSettingsView` や MAUI バインディングから再利用される。Core モデルの定義、宣言ツリー同士の比較、`Theme` / `CellStyle` の値解決は Host の責務ではない。

#### 責務境界

- `SettingsRootStore` の初期 `root` / `theme` を受け取り、構造変更通知と Theme 変更を購読する。
- `SettingsRootDiff` を `UICollectionViewDiffableDataSource<UUID, KsCellID>` の snapshot 操作へ変換し、Section / Cell の追加・削除・移動・内容更新を描画へ反映する。
- `rootHeader` / `rootFooter` を Root レベルの supplementary view として保持する。Section の Header / Footer は `Section` の `SectionAccessory` を描画する。
- `KsCellRegistry` から Renderer 型を解決し、`KsCellRenderer.render(cell:theme:)` に最新 Cell と Theme を渡す。
- `style`、Section H/F の寸法・罫線・セル行レイアウトの視覚契約は iOS styling 候補へ、SwiftUI DSL と Diff 算出は `settings-view-ios-swiftui` 候補へ合流させる。

#### 保証すること

- 公開初期化経路は `KsSettingsViewController(store:style:registry:autoRegisterBasicCells:autoRegisterInputCells:)` であり、`SettingsRoot` の公開 setter は持たない。状態更新は Store または `applyDiff(_:)` を通す。
- 空の `SettingsRoot` も有効な入力で、空 snapshot を持つ `UICollectionView` として表示できる。
- `applyTheme(_:)` は構造 Diff と分離され、現在 Theme と背景色を更新したうえで、既存 item の集合と順序を変えずに表示中 Cell を再構成する。
- `RootAccessory.text` と `RootAccessory.view`、`SectionAccessory.text` と `SectionAccessory.view` を扱う。`KsAnyView.swiftUI` は `UIHostingConfiguration`、`KsAnyView.uiKit` は `UIView` の埋め込みとして描画する。
- Store が Controller より長命でも、Store 購読と UIKit の DataSource / Delegate が Controller を延命しない。Controller の解放は `MemoryLeakTests` で検証されている。

#### してはいけないこと

- 利用者コードから Controller の内部 `root` を直接差し替えない。
- Theme 更新を `SettingsRootDiff` に混ぜない。
- Cell 型ごとの描画分岐を `KsSettingsViewController` へ追加しない。追加 Cell は `KsCellRegistry` / `KsCellRenderer` 境界を使う。
- DiffableDataSource の差分アニメーションと同じ更新サイクルで `setCollectionViewLayout(_:animated:)` を同期実行しない。Section ごとの supplementary 構成は、現在の visible projection を参照する `sectionProvider` と `invalidateLayout()` で追従させる。

#### 公開 API

- `KsSettingsViewController(store:style:registry:autoRegisterBasicCells:autoRegisterInputCells:)`: UIKit Host の入口。
- `applyDiff(_:)`: `SettingsRootDiff` を直接適用する入口。
- `applyTheme(_:)`: 構造変更と独立した Theme 適用。
- `style`: `.classic` / `.modern` の切替。
- `rootHeader` / `rootFooter`: Root H/F の追加・更新・削除。`nil` は非表示。
- `registry`: 利用する `KsCellRegistry`。

#### 利用例

```swift
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

let section = Section(
    header: .text("一般"),
    cells: [LabelCell(title: "バージョン", valueText: "1.0.0")]
)
let store = SettingsRootStore(
    initialRoot: SettingsRoot(sections: [section]),
    initialTheme: Theme()
)
let settingsViewController = KsSettingsViewController(store: store, style: .classic)
settingsViewController.rootHeader = .text("プロフィール")

// 表示後の構造変更は Store 経由で反映される。
store.insertCell(LabelCell(title: "ライセンス"), in: section.id, at: 1)
```

出典: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` / `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` / `ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift` / `ios/Tests/KsSettingsViewUITests/RootAccessoryRenderingTests.swift` / `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift` / `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift` / `samples/ios/KsSettingsViewSample/ContentView.swift` / `openspec/specs/settings-view-ios-host/spec.md` (Purpose) / `docs/platform-guide-ios.md`

### Store の状態と更新通知 (提案カテゴリ: architecture/)

#### 目的

`SettingsRootStore` は、購読開始時点でも取得できる現在状態と、その後の変更意図を伝える通知を分離する。iOS では `@Published` の `root` / `theme` と内部 `SettingsRootDiff` Publisher を組み合わせ、Controller と SwiftUI の双方から利用できる状態保持境界を提供する。

#### 責務境界

- hidden 要素を含む完全な `SettingsRoot` と現在の `Theme` を保持する。
- Section / Cell / Accessory 操作では保持状態を更新し、同じ操作を表す `SettingsRootDiff` を発行する。
- Theme は構造ではないため、`applyTheme(_:)` では `theme` のみを更新し、構造 Diff を発行しない。
- snapshot 操作、Renderer の選択、アニメーション、model 座標から visible projection 座標への変換は Host が担う。

#### 保証すること

- `root` と `theme` は `public private(set)` で、利用者は Store の公開操作を通して更新する。
- `insertSection` / `moveSection` / `insertCell` / `moveCell` の index は hidden を含む model 配列上の位置を表し、範囲外の挿入先は有効範囲へ clamp される。
- 対象 Section / Cell が存在しない remove / move / replace / insert 操作は、状態を変更せず Diff も発行しない。
- `replaceCell` は Cell の `id` で対象を探すため、同一 id への連続した内容更新を失わない。
- 同値の Theme を再適用しても `$theme` の不要な通知を発行しない。

#### してはいけないこと

- `root` / `theme` を外部から直接代入しない。
- Theme 更新を `SettingsRootDiff` として発行しない。
- Store 内で UIKit snapshot や Renderer を操作しない。
- 部分操作の index を visible projection 上の位置として渡さない。

#### 公開 API

- 初期化: `SettingsRootStore(initialRoot:initialTheme:)`、`preview(root:theme:)`。
- Root: `replaceAll(_:)`。
- Section: `insertSection(_:at:)`、`removeSection(sectionID:)`、`moveSection(from:to:)`、`replaceSection(sectionID:new:)`。
- Cell: `insertCell(_:in:at:)`、`removeCell(cellID:)`、`replaceCell(cellID:new:)`、`moveCell(cellID:to:)`。
- Accessory / Theme: `updateAccessory(target:accessory:)`、`applyTheme(_:)`。

#### 利用例

```swift
let store = SettingsRootStore(initialRoot: SettingsRoot(sections: [section]))
let controller = KsSettingsViewController(store: store)

store.insertCell(newCell, in: section.id, at: section.cells.count)
store.replaceCell(cellID: KsCellID(cell: oldCell), new: updatedCell)
store.applyTheme(darkTheme) // SettingsRootDiff は発行しない
```

出典: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` / `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift` / `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift` / `ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift` / `samples/ios/KsSettingsViewSample/ContentView.swift` / `openspec/specs/settings-view-ios-host/spec.md` (Purpose, SettingsRootStore) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/store-and-update-streams.md` / `docs/architecture.md`

### 表示状態同期 (提案カテゴリ: architecture/)

#### 目的

同じ設定ツリーの変化でも、表示構造、同一 id の内容、可視性、Theme では必要な更新経路が異なる。`KsSettingsViewController` はこれらを混同せず、Cell のちらつき、hidden 要素の喪失、snapshot 識別子の drift を防ぐ。

#### 責務境界

| 対象 | iOS Host の反映経路 |
|---|---|
| Section / Cell の追加・削除・移動 | `NSDiffableDataSourceSnapshot` の構造操作 |
| 同一 id の Cell 内容更新 | `reconfigureItems` (iOS 15+)。旧 OS のみ `reloadItems` へ fallback |
| `isVisible` の変更 | hidden を含む model から visible projection を再構築 |
| Theme | `applyTheme(_:)` で独立更新 |

#### 保証すること

- snapshot の item identity は内容を含まない `KsCellID`、すなわち `KsCell.id` のみである。同一 id の連続内容更新でも snapshot 識別子は安定する。
- `root.sections`、`sectionIndex`、`cellIndex` は hidden を含む model を保持する。snapshot、`sectionProvider`、supplementary view、separator は visible projection を参照する。
- `VisibilityAware` に準拠しない利用者定義 Cell は safe-by-default で visible として扱う。
- hidden 対象への remove / move / Accessory 更新は model に反映する一方、snapshot では通常の no-op とする。後で visible に戻したとき、更新済み状態が現れる。
- 部分 Diff の index は model 配列基準で解釈し、前方にある visible 要素数から snapshot 上の位置へ変換する。
- `replaceCell` で旧 Cell と新 Cell の `isVisible` が変わる場合、および `replaceSection` の場合は full snapshot 経路へ fallback する。

#### してはいけないこと

- Cell の内容値を `KsCellID` の equality / hash に含めない。
- `replaceCell` を id の異なる Cell への差し替えに使わない。identity の変更は remove + insert で表現する。
- hidden 要素を model から削除して可視性を表現しない。
- hidden 対象が snapshot にないことを missing-ID エラーとして扱わない。
- Theme 更新を構造同期、内容更新、可視性変更のいずれかへ擬装しない。

出典: `ios/Sources/KsSettingsViewCore/KsCellID.swift` / `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` / `ios/Sources/KsSettingsViewUI/VisibilityAware.swift` / `ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift` / `ios/Tests/KsSettingsViewUITests/VisibilityProjectionTests.swift` / `ios/Tests/KsSettingsViewUITests/DiffableDataSourceTests.swift` / `openspec/specs/settings-view-ios-host/spec.md` (Purpose, DiffableDataSource, visible projection) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/display-state-synchronization.md` / `docs/architecture.md`

### Cell Renderer Registry (提案カテゴリ: architecture/)

#### 目的

`KsCellRegistry` と `KsCellRenderer` は Cell モデル型と UIKit Renderer 型の対応を Host の型分岐から分離する。ライブラリ提供 Cell と利用者定義 Cell を同じ解決経路へ載せ、Cell の追加で `KsSettingsViewController` を変更しないための拡張境界である。

#### 責務境界

- `KsCellRegistry` は具象 `KsCell` 型から `UICollectionViewCell & KsCellRenderer` 型への対応を登録・解決する。
- `KsCellRenderer.render(cell:theme:)` は型消去された Cell と現在 Theme を受け取り、具象 Renderer が型検査と描画を行う。
- Host は Renderer 型の解決、dequeue、`render` 呼び出しを担うが、Cell 固有の表示内容を知らない。

#### 保証すること

- `KsCellRegistry.shared` を既定のプロセス共通 Registry とし、テストや隔離構成では独立 Registry を Controller へ注入できる。
- 登録と解決はロックで保護され、複数スレッドから Registry の辞書を同時に破壊しない。
- `register` は同じ Cell 型の既存対応を後勝ちで置き換える。
- 未登録 Cell は DEBUG で assertion により早期検出し、assertion が無効なビルドでは空の placeholder Cell に退避する。
- 再利用される標準 Renderer は、前のテキスト、画像、補助 View を `prepareForReuse()` で除去する。編集中の `UITextField` を含む subtree は再 render で取り外さず first responder を保護する。

#### してはいけないこと

- Host 内に Cell 具象型ごとの switch / if 分岐を増やさない。
- `KsCellRenderer.render(cell:theme:)` で想定外の Cell 型を黙って描画しない。
- Cell 再利用時に前のモデルのテキスト、画像、補助 View、イベント状態を残さない。

#### 公開 API

- `KsCellRegistry.shared` / `KsCellRegistry()`。
- `register(cellType:rendererType:)` / `resolveRendererType(for:)` / `removeAll()`。
- `registerBasicCells()` / `registerInputCells()`。
- `KsCellRenderer.render(cell:theme:)`。

#### 利用例

利用者定義 Cell は公開 `UICollectionViewCell` サブクラスで `KsCellRenderer` に準拠し、Registry へ登録する。`KsListCellBase` は internal のため、ライブラリ外からは継承できない。

```swift
let registry = KsCellRegistry()
registry.register(cellType: MyCell.self, rendererType: MyCellView.self)

let controller = KsSettingsViewController(
    store: store,
    registry: registry,
    autoRegisterBasicCells: false,
    autoRegisterInputCells: false
)
```

出典: `ios/Sources/KsSettingsViewUI/KsCellRegistry.swift` / `ios/Sources/KsSettingsViewUI/KsCellRenderer.swift` / `ios/Sources/KsSettingsViewUI/KsCellRegistry+BasicCells.swift` / `ios/Sources/KsSettingsViewUI/KsCellRegistry+InputCells.swift` / `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` / `ios/Tests/KsSettingsViewUITests/KsCellRegistryTests.swift` / `openspec/specs/settings-view-ios-host/spec.md` (Purpose, Cell レジストリ, KsCellRenderer) / `kasane/changes/remigrate-concepts/reference/old-concepts/architecture/cell-renderer-registry.md` / `docs/platform-guide-ios.md`

## ADR 候補

- DiffableDataSource の差分アニメーションと同じ更新サイクルでは `setCollectionViewLayout(_:animated:)` を同期実行せず、最新 visible projection を読む `sectionProvider` と `invalidateLayout()` で supplementary を追従させる — 出典: `openspec/specs/settings-view-ios-host/spec.md`「partial Section / UpdateAccessory の supplementary 追従」、選別基準: コンポーネント境界を越える（DiffableDataSource / Compositional Layout）・将来の実装を制約する。同期差し替えは全 Cell バウンドと描画乱れを招いたという却下理由が出典にある。

## drift 所見

- `applyDiff(_:)` は public で Store 購読の共通適用口だが、`viewDidLoad` 前は `.full` 以外の Diff を `updateInternalRoot(for:)` が無視する。`KsSettingsViewController(store:)` の生成後、View を load する前に `store.insertCell(...)` 等を呼ぶと Store の `root` は更新される一方、Controller の初期 snapshot は生成時の古い `root` から構築される (`openspec/specs/settings-view-ios-host/spec.md`「KsSettingsViewController の公開 API」 / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`)。
- 旧 concept は「対象が存在しないなど適用できない操作は、状態も通知も変えない」とするが、`SettingsRootStore.updateAccessory(target:.sectionHeader/.sectionFooter, ...)` は Section ID が存在しなくても `updateSectionAccessory` の失敗後に `.updateAccessory` を無条件発行する。購読済み Controller は missing Section をエラーとして処理する (`kasane/changes/remigrate-concepts/reference/old-concepts/architecture/store-and-update-streams.md` / `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`)。
- `docs/platform-guide-ios.md` §10 は利用者定義 Renderer について「`KsListCellBase` を継承するのが楽」と案内するが、`KsListCellBase` は `internal class` であり、ライブラリ外の利用者は継承できない (`docs/platform-guide-ios.md` / `ios/Sources/KsSettingsViewUI/KsListCellBase.swift`)。

## 用語

- Host: Core の `SettingsRoot` / `SettingsRootDiff` を UIKit のリスト表示へ接続する `KsSettingsViewController`。
- model: hidden Section / Cell を含む `SettingsRoot` の完全な状態。
- visible projection: `Section.isVisible` と `VisibilityAware.isVisible` によって model から表示対象だけを抽出した派生状態。
- reconfigure: 同一 `KsCellID` の行を破棄・再生成せず、最新 Cell と Theme で表示内容だけを再構成すること。
- Renderer: `KsCellRenderer` に準拠し、Cell モデルを `UICollectionViewCell` に描画する型。
- Root H/F: 画面全体の Header / Footer。`SettingsRoot` には含めず、`KsSettingsViewController.rootHeader` / `rootFooter` が保持する。

## 抽出メモ

- 4 概念候補を抽出した。`iOS Native Host` は `platforms/` の iOS 固有公開 API と利用例として独立させ、`Store の状態と更新通知`、`表示状態同期`、`Cell Renderer Registry` は Android Host 材料と照合後に `architecture/` へ統合するのが妥当である。ここでは統合判断を行っていない。
- `style`、Section H/F の細かな寸法・余白・罫線、`KsListCellBase` の stack layout は `settings-view-ios-style` / `settings-view-ios-theme-bridge` の候補と重なるため、Host の責務境界と公開契約に必要な範囲だけ残した。
- Root / Section Accessory の `.text` / `.view` 描画契約は Host の公開入力として高価値だが、Core の Accessory 型定義自体は Batch A の `settings-tree` / `structural-changes` と重なるため再定義していない。
