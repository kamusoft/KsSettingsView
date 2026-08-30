## Why

オリジナル `AiForms.Maui.SettingsView` の `CellBase` は `Title` / `Description` / `HintText` / `IconSource` / `BackgroundColor` / `IconSize` / `IconRadius` 等を **全 Cell の共通プロパティ** として提供しており、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` / `ButtonCell` も description / hint / icon を表示できた。しかし KsSettingsView は現状 `LabelCell` / `CommandCell` だけが `description` / `valueText` / `icon` / `hintText` を持ち、`SwitchCell` / `CheckboxCell` は `title` + `description` のみ、`RadioCell` / `SimpleCheckCell` / `ButtonCell` は `title` 中心しか持てていない。これは「LabelCell ベース機能のみが横展開された結果の実装漏れ」であり、オリジナルとの表示互換性が損なわれている。

加えて、各 Cell View / ViewHolder が **個別レイアウト** を抱えているため共通フィールドを横展開する基盤がない。本変更で共通行レイアウト関数（コンポジションベース）を切り出し、各 Cell View は accessory slot のみ専用実装にすることで、オリジナル `CellBase` の共通プロパティ概念を完成させる。副次的に `RadioCell.accentColor` / `SimpleCheckCell.accentColor` の移植漏れも同時補完する。

前提として、Change 1 (`port-theme-and-cellstyle-missing-fields`、アーカイブ済み) で Theme/CellStyle の解決順序 (`CellStyle → Theme → 既定`) が確立されているため、本 change で追加される全 Cell の共通フィールドはこの解決順序にそのまま乗る。

## What Changes

### 共通行レイアウトの切り出し（新規）

> 改訂: spec レビュー（CHANGES_REQUESTED）後の議論を経て、Compose 採用を撤回し View ベースに統一する方針へ変更（`design.md` Decision 11 / 12 参照）。両プラットフォームとも `applyCellBaseLayout` 関数で命名統一する。

- **iOS**: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`（旧 `KsCellRowLayout.swift` をリネーム）を新規追加。`@MainActor internal func applyCellBaseLayout(_ listCell: UICollectionViewListCell, title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Bool, accessories: [UICellAccessory])` の UIKit Builder。`hintText` は `UICellAccessory` ではなく `cell` 直下の `hintLabel`（subview）として右上 float 配置する（オリジナル `AiForms.Maui.SettingsView` の `HintLabel` 踏襲）。
- **Android**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt` を新規追加（旧 `KsCellRowLayout.kt` の Compose 版は完全削除）。View ベース `ConstraintLayout` programmatic 構築の `CellBaseViews` 構造体と、`internal fun applyCellBaseLayout(views: CellBaseViews, title: String, description: String?, valueText: String?, icon: KsImage?, hintText: String?, effective: EffectiveStyle, isEnabled: Boolean)` を提供する。`hintText` は `ConstraintLayout` の `End=parent / Top=parent` で右上 float 配置（オリジナル `cellbaseview.axml` の `CellHintText` 踏襲）。
- 既存 7 種 Cell の View / ViewHolder は共通レイアウト関数を呼び出し、accessory slot（iOS: `accessories` 引数 / Android: `CellBaseViews.accessoryHolder` への `addView`）だけ専用実装する。

### 全 Cell モデルへの共通フィールド追加

各 Cell のドメインモデル (iOS struct / Android data class) に下記を追加する。全フィールドは Optional / 既定 `nil` / `null`。

| Cell | 追加フィールド |
|------|---------------|
| `SwitchCell` | `valueText`, `icon`, `hintText` |
| `CheckboxCell` | `valueText`, `icon`, `hintText` |
| `RadioCell` | `description`, `valueText`, `icon`, `hintText`, `accentColor` |
| `SimpleCheckCell` | `description`, `valueText`, `icon`, `hintText`, `accentColor` |
| `ButtonCell` | `valueText`, `icon`, `hintText`（**`description` は意図的に追加しない**。オリジナル `AiForms.Maui.SettingsView` の `ButtonCell` が `CellBase.Description` を `private new` で隠蔽し、iOS の `ButtonCellView` も `DescriptionLabel.Hidden = true` としている挙動を踏襲する） |
| `LabelCell` | （既存維持） |
| `CommandCell` | （既存維持） |

### Cell View の整理

| Cell View | accessory slot に置く内容 |
|-----------|--------------------------|
| `LabelCellView` | （accessory なし） |
| `CommandCellView` | chevron / disclosure indicator（`hideArrow = false` のときのみ） |
| `SwitchCellView` | `UISwitch` / `Switch` |
| `CheckboxCellView` | `KsCheckBoxView` / `MaterialCheckBox` |
| `RadioCellView` | `KsCheckmarkAccessoryView` / radio 表示 |
| `SimpleCheckCellView` | `KsSimpleCheckView` 風表示 |
| `ButtonCellView` | （accessory なし、`title` を `titleAlignment` に従って配置） |

### Breaking changes

なし。すべて Optional フィールドの追加であり、既存呼び出しは引数省略で動作する。

## Capabilities

### New Capabilities

（なし。共通行レイアウトは spec 化された capability ではなく、既存 capability の実装規約として扱う）

### Modified Capabilities

- `cell-types-basic`: 7 種 Cell の Requirement を更新し、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` / `ButtonCell` に共通フィールド (`description` / `valueText` / `icon` / `hintText`) を追加。`RadioCell` / `SimpleCheckCell` に `accentColor` を追加。
- `settings-view-ios-swiftui`: 共通行レイアウト関数 (`applyCellBaseLayout`) の I/F 規約を追加。各 Cell View が共通レイアウト関数を呼び出さなければならない実装規約と、`hintText` を `cell` 直下 subview として右上 float 配置する規約。
- `settings-view-android-compose`: 共通行レイアウト関数 (`applyCellBaseLayout`) の I/F 規約を追加。View ベース `CellBaseViews`（`ConstraintLayout` programmatic 構築）+ `applyCellBaseLayout` 経由で全 CellViewHolder を統一する規約と、Compose（`KsCellRow`）を採用しない旨。**spec ファイル名（`settings-view-android-compose`）は本 change では変更せず維持する**（内部の Compose 化 Requirement のみ撤回し、View ベースの規約に書き換える）。spec ファイル名のリネーム（例: `settings-view-android-ui` 等への変更）は後続 change で別途扱う予定とする。

## Impact

**影響モジュール（iOS）**
- 新規: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift`
- 既存改修（モデル拡張）: `ios/Sources/KsSettingsViewUI/{SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell,ButtonCell}.swift`
- 既存改修（View → 共通レイアウト呼び出し）: `ios/Sources/KsSettingsViewUI/{LabelCellView,CommandCellView,SwitchCellView,CheckboxCellView,RadioCellView,SimpleCheckCellView,ButtonCellView}.swift`
- 既存改修（DSL 拡張関数の引数追加）: `ios/Sources/KsSettingsViewUI/DSL...` 関連の `Section` 拡張群（必要箇所）

**影響モジュール（Android）**
- 新規: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt`
- 既存改修（モデル拡張）: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/{SwitchCell,CheckboxCell,RadioCell,SimpleCheckCell,ButtonCell}.kt`
- 既存改修（ViewHolder → 共通レイアウト呼び出し）: 対応する `*ViewHolder.kt`（`CellBaseViews` + `applyCellBaseLayout` 経由に書き換え）
- 既存改修（DSL 拡張関数の引数追加）: `android/ks-settingsview-compose/...` の `DSLSectionScope` 拡張群

**サンプル**
- `samples/ios` / `samples/android` に「Switch + icon + description + hintText」「Radio + accentColor」等のサンプルページを追加し、共通フィールドが視覚的に反映されることを確認できるようにする。

**互換性**
- 既存呼び出し（`SwitchCell(title: "...")` 等）は破壊なし。新規引数は省略可。

**ビルド・テスト**
- `swift test`（iOS）/ `./gradlew :ks-settingsview-ui:test`（Android）の追加テストですべての Cell が description / valueText / icon / hintText を表示できることを検証。
- `KsListCellBase` の `preferredLayoutAttributesFitting` override（`CellStyle.cellHeight` 反映）を壊さないこと（共通レイアウト関数が `contentConfiguration` 経由で組まれる場合、`preferredLayoutAttributesFitting` の sizing 経路が維持されることをテストで確認）。

**着手前チェック実施結果**（羅針盤 §2.7）
- 既存 in-progress change の `add-cell-types-input` / `add-cell-types-custom` を確認したところ、いずれも新規 Cell 種別（EntryCell / PickerCell / CustomCell など）を追加するもので、本 change が触れる 7 種 Cell のモデル定義には触れていない。フィールド名の衝突なし。
- アーカイブ `archive/2026-06-07-add-refine-basic-cells-sample-layout`（archive ディレクトリ名は `2026-06-07-refine-basic-cells-sample-layout`）のレイアウト規約は、共通行レイアウト関数の引数仕様と整合させる方向で design.md にて反映する。
