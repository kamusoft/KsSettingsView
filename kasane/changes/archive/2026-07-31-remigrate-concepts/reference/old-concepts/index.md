# concepts 目次

## architecture/

- [repository-boundaries.md](architecture/repository-boundaries.md) — 横断変更をまとめるリポジトリと独立ビルドルートの責務境界
- [native-host-boundary.md](architecture/native-host-boundary.md) — Coreの設定状態をNativeリストへ接続する共通Host境界
- [store-and-update-streams.md](architecture/store-and-update-streams.md) — 永続状態と一過性の更新通知を分離するStore契約
- [declarative-ui-bridge.md](architecture/declarative-ui-bridge.md) — SwiftUI・ComposeをStoreとNative Hostへ接続するBridge境界
- [declarative-tree-identity.md](architecture/declarative-tree-identity.md) — 宣言UIの再評価をまたぐSection・Cellの安定同一性
- [cell-renderer-registry.md](architecture/cell-renderer-registry.md) — CellモデルとNative描画型を分離する拡張Registry
- [display-state-synchronization.md](architecture/display-state-synchronization.md) — 構造・内容・可視性を分離する表示状態同期の原則

## core-model/

- [settings-tree.md](core-model/settings-tree.md) — Root・Section・Cell・Accessoryの語彙と責務
- [structural-changes.md](core-model/structural-changes.md) — 設定ツリーの変更意図とUI適用責務の境界

## cells/

- [basic-cell-semantics.md](cells/basic-cell-semantics.md) — 基本 Cell の意味、値等価、操作通知、外部状態所有の契約
- [cell-image-boundary.md](cells/cell-image-boundary.md) — Cell 用画像を UI 層へ隔離する値同一性とフォールバック規則

## styling/

- [style-resolution.md](styling/style-resolution.md) — UI層でThemeとCellStyleを段階的に実効値へ解決する規則
- [cell-visual-states.md](styling/cell-visual-states.md) — 通常・選択・無効状態を重ねる共通描画契約
- [cell-row-layout.md](styling/cell-row-layout.md) — Cell共通行の視覚文法とplatform別の寸法トークン
- [list-appearance.md](styling/list-appearance.md) — Classic・ModernとHeader／Footerの配置原則

## platforms/

## conventions/

- [public-identifiers.md](conventions/public-identifiers.md) — 所有主体・製品・成果物を区別する公開識別子と配布座標の規約
