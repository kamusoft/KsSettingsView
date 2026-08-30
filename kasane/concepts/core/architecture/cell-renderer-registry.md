---
type: concept
title: Cell Renderer Registry
description: Cell model 型と Native 描画型を分離し、利用者定義 Cell を追加する拡張境界
tags: [architecture, cell, renderer, extensibility]
timestamp: 2026-07-19
---

この文書は、iOS / Android の `KsCellRegistry` に共通する拡張境界を説明する。読むと、Cell model と Renderer / ViewHolder の分離、標準 Cell の登録、利用者定義 Cell、未登録時と再利用時の契約が分かる。

ここで Cell model は、Core の Cell 抽象へ準拠し、UI 層が定義する具象の値である。Renderer / ViewHolder はそれを描画する Native cell であり、model 自体ではない。3層の関係は [Native Host の責務境界](native-host-boundary.md#cell-の3層) を参照する。

## 目的

Registry は具象 Cell 型と Native 描画型の対応を Host 本体から分離する。Host は Registry から描画型を解決し、最新の Cell と Theme を渡す。Cell の追加時に DataSource / Adapter へ具象型の分岐を加えない。

| 境界 | iOS | Android |
|---|---|---|
| model | `KsCell` 具象型 | `Cell` 具象型 |
| 描画型 | `UICollectionViewCell & KsCellRenderer` | `CellViewHolder<T>` factory |
| 登録集合 | `.shared` または注入した独立 Registry | app process 全体で共有する singleton |
| 未登録時 | DEBUG assertion、無効時は空の代替行 | `strictMode` なら例外、false なら高さ0の代替行 |

## 登録と解決

標準登録対象の Cell 集合は通常の Host で自動登録される。現在の集合は [基本 Cell](../cells/basic-cells.md) 7種と [入力 Cell](../cells/input-cells.md) 5種である。利用者定義 Cell は表示前に対応する Renderer / ViewHolder factory を登録する。

iOS は Controller へ独立 Registry を注入できる。独立 Registry を使う場合、必要な標準 Cell もその Registry へ明示登録する。Android は `KsCellRegistry` singleton を使い、利用者 `viewType` は Root / Section Accessory の予約域を避けて `CELL_VIEW_TYPE_MIN` 以上にする。

```swift
let registry = KsCellRegistry()
registry.registerBasicCells()
registry.registerInputCells()
registry.register(cellType: MyCell.self, rendererType: MyCellView.self)

let controller = KsSettingsViewController(
    store: store,
    registry: registry,
    autoRegisterBasicCells: false,
    autoRegisterInputCells: false
)
```

既定の `.shared` Registry を使う Controller は標準集合を自動登録できる。上のように独立 Registry を注入する場合は、その独立集合へ必要な標準 Cell と利用者定義 Cell を登録する。`MyCellView` は公開 `UICollectionViewCell` subclass として `KsCellRenderer` に準拠する。

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG
KsCellRegistry.register(
    cellClass = MyCell::class,
    viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN + 50,
) { parent ->
    MyCellViewHolder(parent)
}
```

Android の key は `cellClass`、Native 行種別の数値識別子は `viewType` である。異なる Cell 型へ同じ `viewType` を割り当てない。`strictMode` は未登録 Cell を例外で早期検出する診断 mode であり、app の Debug / Release へ自動追従しないため利用者が明示する。

同じ Cell 型を再登録した場合は後の描画型を使う。iOS では Cell model 型、Android では `cellClass` と `viewType` の対応を登録単位とし、別 Cell 型の識別子を衝突させない。

## 再利用境界

Renderer / ViewHolder は bind ごとに最新 Cell model と Theme を反映し、再利用時に前の listener、購読、画像、埋め込み View を残さない。iOS の Renderer は `prepareForReuse()`、Android の ViewHolder は `reset()` で前の model に属する資源を解放する。入力中の Native control は、同じ ID の内容更新で不必要に再生成しない。

SwiftUI / Compose の任意 View を内包する Accessory は、Native cell / ViewHolder の寿命へ宣言 UI の構成と破棄を合わせる。

## 保証すること

- 標準 Cell と利用者定義 Cell を同じ解決経路で描画する。
- Cell 型の追加で Native Host 本体へ型分岐を増やさない。
- bind 時に現在の Cell と Theme を Renderer / ViewHolder へ渡す。
- 再利用時に前の model に属する状態と非同期処理を解放する。

## してはいけないこと

- DataSource / Adapter へ Cell 具象型ごとの分岐を追加しない。
- Android の利用者 `viewType` に予約領域を使わない。
- iOS の internal `KsListCellBase` を利用者定義 Renderer の基底型として案内しない。
- 未登録 Cell の fallback を通常の登録方法として利用しない。

## 関連

- [Native Host の責務境界](native-host-boundary.md)
- [iOS Native Host の Cell Renderer Registry](../../ios/api/ios-native-host.md#cell-renderer-registry)
- [Android Native Host の Cell Renderer Registry](../../android/api/android-native-host.md#cell-renderer-registry)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
