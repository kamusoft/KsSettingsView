## 1. KsSettingsViewUI モジュール初期設定

- [x] 1.1 `ios/Sources/KsSettingsViewUI/` ディレクトリを作成
- [x] 1.2 `ios/Package.swift` に `KsSettingsViewUI` ターゲット（`KsSettingsViewCore` 依存）を追加
- [x] 1.3 `ios/Tests/KsSettingsViewUITests/` ディレクトリを作成

## 2. UIKit 変換ユーティリティ

- [x] 2.1 `UIColor+KsColor.swift` を作成（`init(ksColor: KsColor)`）
- [x] 2.2 `UIFont+KsFont.swift` を作成（`init(ksFont: KsFont)`）
- [x] 2.3 `EffectiveStyle.swift` を作成（`Theme` と `CellStyle` を合成し `UIColor`/`UIFont` を返すユーティリティ）

## 3. Cell 描画基盤

- [x] 3.1 `KsCellRenderer.swift` でプロトコル `public protocol KsCellRenderer: AnyObject { ... }`（任意の `KsCell` 準拠 Cell と `Theme` を受け取り描画する `render` を要求）を定義
- [x] 3.2 `KsCellRegistry.swift` で型登録・解決ロジックを実装（`register(cellType:rendererType:)`、`resolveRendererType(for cell:)`、`shared` シングルトン）

## 4. KsSettingsViewController

- [x] 4.1 `KsSettingsViewStyle.swift` で `public enum KsSettingsViewStyle { case classic; case modern }` を実装
- [x] 4.2 `KsSettingsViewController.swift` で `UIViewController` を継承し以下を実装：
  - 内部 `UICollectionView` を `UICollectionViewCompositionalLayout.list(using:)` で生成
  - `public var style: KsSettingsViewStyle { didSet { rebuildLayout() } }`：`.classic` → `.plain`、`.modern` → `.insetGrouped` の Appearance を設定
  - `public var root: SettingsRoot { didSet { applySnapshot(animated: true) } }`
  - `UICollectionViewDiffableDataSource<Section.ID, KsCellID>` を保持（`KsCellID` は Cell を一意識別する Hashable な値型）
  - cell provider は `KsCellRegistry` 経由で renderer を解決し、`render` を呼ぶ
  - `init(style: KsSettingsViewStyle = .classic)` をデフォルトイニシャライザとして公開
- [x] 4.3 Section H/F supplementary registration を実装（`UICollectionLayoutListConfiguration.headerMode = .supplementary` / `footerMode = .supplementary`）。`SectionAccessory.text` は文字列描画、`SectionAccessory.view(KsAnyView)` は `UICollectionViewListCell.contentConfiguration` を `UIHostingConfiguration`（SwiftUI backing）または `addSubview`（UIView backing）で構成して描画する
- [x] 4.4 Root H/F の boundary supplementary を実装：`UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header"` / `"ks-root-footer"`、`alignment: .top` / `.bottom` を 1 つずつ追加（`pinToVisibleBounds = false`）。supplementary view provider で `RootAccessory` の `text` / `view` を解決し、`UIListContentConfiguration` または `UIHostingConfiguration` / `addSubview` で描画する。`SettingsRoot.header` / `footer` が `nil` のときは boundary item を省略する
- [x] 4.5 `viewDidLoad()` で空 SettingsRoot 相当の初期スナップショットを apply
- [x] 4.6 `deinit` で DataSource の参照を解放

## 5. PoC Cell

- [x] 5.1 `PoCLabelCell.swift` を `internal` で実装（id・title のみ）
- [x] 5.2 `PoCLabelCellView.swift` を `internal final class ... UICollectionViewCell, KsCellRenderer` として実装
- [x] 5.3 `KsSettingsViewController.init` 内で `KsCellRegistry.shared` に PoC Cell を登録

## 6. SwiftUI ラッパ

- [x] 6.1 `ios/Sources/KsSettingsViewSwiftUI/` モジュールを `Package.swift` に追加（`KsSettingsViewUI` 依存）
- [x] 6.2 `KsSettingsView.swift` で `public struct KsSettingsView: UIViewControllerRepresentable` を実装、`@Binding var root: SettingsRoot`、`let style: KsSettingsViewStyle`（デフォルト `.classic`）、`makeUIViewController` / `updateUIViewController` を実装
- [x] 6.3 `Coordinator` で前回 root をキャッシュし、等価時は updateUIViewController を no-op 化

## 7. SwiftUI DSL

- [x] 7.1 `SettingsRootBuilder.swift` を作成し `@resultBuilder public struct SettingsRootBuilder` を実装、`SettingsRoot { Section { Cell... } }` 構文を可能にする
- [x] 7.2 `SectionBuilder.swift` を作成（`@resultBuilder public struct SectionBuilder`）
- [x] 7.3 `Section.init(_ header: String?, ..., @SectionBuilder cells: () -> [Cell])` のような便利 init を提供

## 8. ユニットテスト（KsSettingsViewUITests）

- [x] 8.1 `KsSettingsViewControllerTests.swift`：root 設定後の Section 数・セル数検証
- [x] 8.2 `DiffableDataSourceTests.swift`：同一 root 連続代入で no-op、Cell 追加で挿入アニメーション
- [x] 8.3 `KsCellRegistryTests.swift`：型登録・解決・未登録時の assertion failure
- [x] 8.4 `EffectiveStyleTests.swift`：CellStyle 未指定 → Theme から補完
- [x] 8.5 `MemoryLeakTests.swift`：`weak var weakController` で `deinit` 確認
- [x] 8.6 `KsSettingsViewStyleTests.swift`：`.classic` 初期化で `.plain` Appearance、`.modern` 初期化で `.insetGrouped` Appearance、setter 経由の動的切替でレイアウト再構築を検証
- [x] 8.7 `SectionAccessoryRenderingTests.swift`：`SectionAccessory.text` でヘッダ文字列描画、`SectionAccessory.view(KsAnyView)` で SwiftUI / UIView backing いずれも描画される、`.view` の中身を差し替えても同一スロットで再描画されることを検証
- [x] 8.8 `RootAccessoryRenderingTests.swift`：`SettingsRoot.header` / `footer` の `RootAccessory.text` / `.view` が boundary supplementary view として描画される、`nil` のときは boundary item が省略される、スクロール時に追従する（pinToVisibleBounds=false）ことを検証
- [x] 8.9 `swift test` でテスト全成功を確認

## 9. ユニットテスト（KsSettingsViewSwiftUITests）

- [x] 9.1 `KsSettingsViewRepresentableTests.swift`：makeUIViewController で controller.root が初期化される、updateUIViewController で root が反映される、`KsSettingsView(root: $root, style: .modern)` で生成した controller の `style` が `.modern` で初期化されることを検証
- [x] 9.2 `SettingsRootBuilderTests.swift`：DSL から SettingsRoot 構築結果の検証

## 10. ドキュメント

- [x] 10.1 `docs/ios-ui.md` を作成し、KsSettingsViewController の使い方、SwiftUI ラッパの使い方、Cell 登録方法を記載

## 11. レビュー対応（review-result_001.md）

- [x] 11.1 [Major] `KsSettingsViewController.root` の didSet で「装飾領域の同型内変化」（`.text → .text` 別文字列、`.view → .view` 別中身）に対し可視 supplementary view を強制リフレッシュする処理を追加（`refreshAccessoriesIfNeeded` / `refreshSupplementary` / `applyAccessoryToListCell`）
- [x] 11.2 [Major] Spec「view 形式ヘッダの中身更新（差分検出非対応）」「Root Header の中身更新（差分検出非対応）」シナリオを実検証するテストを追加
  - `SectionAccessoryRenderingTests.test_text形式ヘッダの文字列更新でcontentConfigurationが新しいテキストを保持する`
  - `SectionAccessoryRenderingTests.test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する`
  - `RootAccessoryRenderingTests.test_root_textヘッダの中身更新でcontentConfigurationが新しいテキストを保持する`
- [x] 11.3 [Minor] `KsSettingsViewController.loadView` を「ルート UIView に UICollectionView を addSubview」する形に変更し、Spec「List 設定の使用」シナリオの `view.subviews` 経路と整合させる。`KsSettingsViewControllerTests.test_view_subviewsからUICollectionViewを取り出せる` を追加
- [x] 11.4 [Minor] `appearance(for:)` を `internal static` に昇格し、`KsSettingsViewStyleTests.test_classicに対応するAppearanceはplain` / `test_modernに対応するAppearanceはinsetGrouped` で appearance マッピングを直接検証
- [x] 11.5 [Suggestion] `KsCellID.contentHash` の hash 衝突注意書きをドキュメントコメントに追記
- [x] 11.6 [Suggestion] `KsSettingsViewController.init` で `registry === KsCellRegistry.shared` のときのみ PoC Cell を自動登録するよう変更（DI で渡された registry を汚染しない）

## 依存関係

- 先行：`add-monorepo-foundation`、`add-settings-view-core`
- 後続：`add-samples-ios`（Sample アプリ土台）、`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`

## 完了条件

- 全タスクのチェックボックスが完了している
- `settings-view-ios-ui` capability の全 Scenario が通る
- `swift test` でテスト全成功
- PoCLabelCell を含む SettingsRoot がユニットテストレベルで描画検証される（`UIListContentConfiguration.text` の値検証等。`SectionAccessoryRenderingTests` / `RootAccessoryRenderingTests` 等で実施済み）

> **補足**: 実機・シミュレータでの目視確認は別変更提案 `add-samples-ios` の責務（`samples/ios/` の SwiftUI Sample アプリ整備）として独立しており、本提案のスコープ外。
