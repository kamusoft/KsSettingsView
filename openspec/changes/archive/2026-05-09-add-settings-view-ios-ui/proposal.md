## Why

`add-settings-view-core` で確立した Core モデルを描画する iOS の UI 基盤を構築する必要がある。`UICollectionView` と `UICollectionViewDiffableDataSource` を採用することで、旧 `AiForms.Maui.SettingsView`（UITableView ベース）より高パフォーマンスかつ滑らかな差分アニメーションを実現する。本変更提案では UI 基盤（ViewController、DataSource、レイアウト、ViewHolder 抽象、SwiftUI ラッパ + DSL）を整備し、PoC として最小 1 種の動作確認用 Cell（`PoCLabelCell`）を表示できるところまで持っていく（具象 Cell の本格実装は `add-cell-types-*` で対応）。

## What Changes

- 新モジュール `KsSettingsViewUI`（iOS Swift）を追加：
  - `public final class KsSettingsViewController: UIViewController`：`root: SettingsRoot { didSet { applySnapshot() } }`、および `style: KsSettingsViewStyle { didSet { rebuildLayout() } }` を持つ
  - `public enum KsSettingsViewStyle`：`.classic`（旧 AiForms 互換のフラットな見た目）/ `.modern`（最新 OS 設定画面風の角丸グルーピング）の 2 種類
  - 内部レイアウトに `UICollectionLayoutListConfiguration`（iOS 14+）を採用：`.classic` は `.plain` Appearance、`.modern` は `.insetGrouped` Appearance を使用。`estimatedItemSize` は `.automatic` で Auto Layout 高さ計算
  - `UICollectionViewDiffableDataSource<Section.ID, KsCellID>` を採用（`KsCellID` は `Section.cells` 内の Cell を一意に識別する Hashable な値型。具体的な定義は実装で決定）
  - `protocol KsCellRenderer`：Cell ごとの描画契約。`UICollectionViewCell` サブクラスが実装する
  - `KsCellRegistry`：Cell 型 → `UICollectionViewCell` 型への登録・解決を行う中央レジストリ
  - Root H/F 描画：`UICollectionViewCompositionalLayout.configuration.boundarySupplementaryItems` に `elementKind: "ks-root-header" / "ks-root-footer"` を 1 つずつ配置し、`pinToVisibleBounds = false`（スクロール追従）をデフォルトとする
  - Section H/F の `.view(KsAnyView)` ケースは `UICollectionViewListCell.contentConfiguration` に `UIHostingConfiguration { ... }`（SwiftUI backing）または `addSubview`（UIView backing）で配置する
  - `Theme` および `CellStyle` を `UIColor` / `UIFont` に変換するユーティリティ
- 新モジュール `KsSettingsViewSwiftUI`（iOS Swift）を追加：
  - `public struct KsSettingsView: UIViewControllerRepresentable`：SwiftUI から使えるラッパ。`style: KsSettingsViewStyle` をイニシャライザ引数で受け取る
  - `@resultBuilder` ベースの DSL（`SettingsRootBuilder`、`SectionBuilder`）：宣言的に Cell ツリーを構築できる
- PoC として `PoCLabelCell`（id・title のみ表示）を `KsSettingsViewUI` 内部に置き、テストとサンプル動作確認に使用する。本 Cell は本格 Cell が追加された段階で削除する
- ViewController のメモリリーク検証用に WeakReference テストを XCTest で追加
- 単体テストで以下を検証：snapshot 適用後の Section 数・セル数、Theme 適用後のセル背景色、SwiftUI ラッパの `updateUIViewController` での再適用、`style` 切替（`.classic` ↔ `.modern`）でレイアウトが再構築され Appearance が一致すること、`SectionAccessory.text` ヘッダの文字列描画、`SectionAccessory.view` ヘッダの `KsAnyView` 描画、`SettingsRoot.header` / `footer`（Root H/F）の `text` / `view` 両ケースの描画

## Capabilities

### New Capabilities
- `settings-view-ios-ui`: iOS UI 基盤（UIViewController、UICollectionView、DiffableDataSource、Cell レジストリ、SwiftUI ラッパ、DSL）の振る舞いを規定する

### Modified Capabilities
（なし）

## Impact

- 影響範囲：iOS Native の UI 層
- 依存：`add-monorepo-foundation`、`add-settings-view-core`
- 後続変更が依存：`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`（iOS バインディング部分）
- リスク：中。`UICollectionView` の Cell 高さ計測コストとメモリ管理に注意が必要
