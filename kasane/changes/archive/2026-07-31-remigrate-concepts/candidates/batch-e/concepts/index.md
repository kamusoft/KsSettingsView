# concepts 目次

カテゴリ定義と配置基準は [rules.md](rules.md) を参照。

## architecture/

- [architecture/native-host-boundary.md](architecture/native-host-boundary.md) — Core の設定状態を各 platform の Native list へ接続し、宣言 UI から再利用する共通境界
- [architecture/store-and-update-streams.md](architecture/store-and-update-streams.md) — 復元可能な現在状態と一過性の更新通知を分離する SettingsRootStore の共通契約
- [architecture/display-state-synchronization.md](architecture/display-state-synchronization.md) — 構造・同一 ID の内容・可視性・Theme を異なる更新経路へ分ける共通原則
- [architecture/cell-renderer-registry.md](architecture/cell-renderer-registry.md) — Cell model 型と Native 描画型を分離し、利用者定義 Cell を追加する拡張境界
- [architecture/declarative-ui-bridge.md](architecture/declarative-ui-bridge.md) — SwiftUI・Compose の宣言状態を Store と Native Host の共通更新経路へ接続する境界
- [architecture/declarative-tree-identity.md](architecture/declarative-tree-identity.md) — 宣言 UI の再評価をまたいで Section と Cell を継続追跡する識別契約
- [architecture/repository-boundaries.md](architecture/repository-boundaries.md) — 横断変更をまとめる monorepo と、独立した platform build・Sample の責務分担

## core-model/

- [core-model/settings-tree.md](core-model/settings-tree.md) — SettingsRoot・Section・Cell の設定ツリーと Accessory 境界
- [core-model/structural-changes.md](core-model/structural-changes.md) — SettingsRootDiff による設定ツリーの構造変更契約

## cells/

- [cells/basic-cells.md](cells/basic-cells.md) — 表示・操作・二値・単一選択を担う基本7種の Cell
- [cells/input-cells.md](cells/input-cells.md) — 文字列・候補・数値・時刻・日付を編集する入力5種の Cell
- [cells/ks-image.md](cells/ks-image.md) — Cell の icon を表す KsImage と platform fallback

## styling/

- [styling/style-resolution.md](styling/style-resolution.md) — UI 層が Theme と CellStyle を所有し、platform の描画値へ段階的に解決する共通規則
- [styling/cell-row-layout.md](styling/cell-row-layout.md) — Cell 種別をまたいで共有する視覚文法と platform 別の行寸法
- [styling/cell-visual-states.md](styling/cell-visual-states.md) — 通常・押下または選択・無効状態を実効 style へ重ねる描画契約
- [styling/list-appearance.md](styling/list-appearance.md) — Classic・Modern の視覚 mode と Section・Root Header／Footer の配置原則

## platforms/

- [platforms/android-native-host.md](platforms/android-native-host.md) — SettingsRootStore と Android View Host の構築・更新・ViewHolder 拡張境界
- [platforms/android-compose.md](platforms/android-compose.md) — Compose の Store / DSL 方式、identity、modifier、Theme 伝播
- [platforms/ios-native-host.md](platforms/ios-native-host.md) — SettingsRootStore と UIKit Host の構築・更新・Renderer 拡張境界
- [platforms/ios-swiftui.md](platforms/ios-swiftui.md) — SwiftUI の Store / DSL 方式、identity、modifier、Theme 伝播

## conventions/

- [conventions/public-identifiers.md](conventions/public-identifiers.md) — 所有主体・製品・成果物の役割を ecosystem ごとの識別子へ写像する規約
