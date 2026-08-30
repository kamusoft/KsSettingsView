---
type: reference
title: iOS Native Host の利用と更新境界
description: SettingsRootStore と KsSettingsViewController を使って UIKit の設定画面を構築・更新・拡張する方法
tags: [ios, uikit, host, public-api]
timestamp: 2026-08-29
---

この文書は、iOS の Native API で設定画面を組み込むための公開 API 利用契約と責務境界を整理した reference である。読むと、`SettingsRootStore` と `KsSettingsViewController` の役割、表示後の更新方法、独自 Cell の登録方法が分かる。SwiftUI から使う場合は [iOS SwiftUI Bridge と宣言 DSL](ios-swiftui.md) を参照する。設定ツリーと差分の型自体は [SettingsRoot・Section・Cell の設定ツリー](../../core/core-model/settings-tree.md) と [SettingsRootDiff による構造変更](../../core/core-model/structural-changes.md) を先に読む。

## 目的

`KsSettingsViewController` は Core の `SettingsRoot` を UIKit のリストへ接続する公開 Host である。`SettingsRootStore` が hidden 要素を含む現在状態と変更通知を持ち、Controller が visible projection、Native snapshot、Cell 描画を担当する。

この Host は UIKit から直接利用できるほか、SwiftUI と将来の外部バインディングから再利用される。Core モデルの定義、宣言ツリー同士の比較、Theme と CellStyle の実効値解決は Host の外側にある。

## 状態と所有者

| 用語 | 所有者 | 意味と担当する更新 |
|---|---|---|
| model | `SettingsRootStore` と Controller の内部状態 | hidden 要素を含む完全な `SettingsRoot`。Store の公開操作で更新する |
| visible projection | `KsSettingsViewController` | model から表示対象だけを元の順序・ID で取り出した派生状態 |
| Native snapshot | `KsSettingsViewController` | visible projection を Diffable Data Source へ渡す iOS の表示構造 |
| Host | `KsSettingsViewController` | Store の状態と通知を visible projection・snapshot・Cell 描画へ接続する境界 |

## 公開入口

### SettingsRootStore

`SettingsRootStore(initialRoot:initialTheme:)` は現在の `SettingsRoot` と `Theme` を保持する。`root` と `theme` は `public private(set)` であり、表示後の変更は次の公開操作を使う。

| 対象 | 操作 |
|---|---|
| Root 全体 | `replaceAll(_:)` |
| Section | `insertSection(_:at:)`、`removeSection(sectionID:)`、`moveSection(from:to:)`、`replaceSection(sectionID:new:)` |
| Cell | `insertCell(_:in:at:)`、`removeCell(cellID:)`、`replaceCell(cellID:new:)`、`moveCell(cellID:to:)`、`replaceCells(_:)` |
| Accessory | `updateAccessory(target:accessory:)` |
| Theme | `applyTheme(_:)` |

Section / Cell 操作の index は、非表示要素を含む model 配列上の位置である。挿入先と移動先は有効範囲へ clamp される。対象 ID が見つからない remove / move / replace、および挿入先 Section が見つからない Cell insert は、状態を変えず構造 Diff も発行しない。

`replaceCells(_:)` は複数 Cell の内容更新を1回の状態更新と1バッチ通知で適用する (Android の `replaceCells` と対称の観察可能挙動)。未知 ID はスキップされ、適用が0件なら通知しない。Controller はこのバッチを受け取り、構造変更なしで対象行の表示内容だけを更新する。

`applyTheme(_:)` は構造 Diff を発行せず、同値の Theme なら通知もしない。Theme は設定ツリーの一部ではない。

### KsSettingsViewController

UIKit からの公開初期化経路は次である。

```swift
let controller = KsSettingsViewController(
    store: store,
    style: .classic,
    registry: .shared,
    autoRegisterBasicCells: true,
    autoRegisterInputCells: true
)
```

Controller は Store の初期 root / theme を取り込み、その後の構造変更と Theme 変更を購読する。`SettingsRoot` の公開 setter は持たない。空の Root も有効で、空の `UICollectionView` として表示できる。

`style` は `.classic` と `.modern` を受ける。`.classic` は UIKit の `.plain` list appearance、`.modern` は `.insetGrouped` を使わず compositional layout 上の自前 Section 装飾で箱を描く ([ios/ADR-0003](../../../decisions/ios/0003-modern-self-drawn-section-decoration.md))。装飾の寸法・ボーダーは Theme の Section 装飾4属性から解決する ([設定 list の外観と補助領域](../../core/styling/list-appearance.md))。切替は設定内容や ID を変えず、同じ値の再代入では layout を作り直さない。

`rootHeader` / `rootFooter` は Root レベルの `RootAccessory` を保持し、`nil` は非表示を表す。`SettingsRoot` には含まれない。Section Header / Footer は各 `Section` の `SectionAccessory` から描画する。text と `KsAnyView.swiftUI` / `.uiKit` の任意 View を利用できる。

## view load 時の Store 現在状態からの復元

Store 接続済みの Controller は、view load (viewDidLoad) の完了時点で接続中 Store の現在状態から表示を構築する。Host 生成から view load までの間に Store へ適用した変更は種類によらず view load 時の表示へ反映されるため、Host 生成・Store 操作・view 階層への取り付けの順序を利用側が意識する必要はない ([core/ADR-0019](../../../decisions/core/0019-host-restores-from-store-on-attach.md))。

- 復元の対象は Store が現在状態として保持するもの — 設定ツリーの構造・Cell 内容・Section accessory・theme。
- `rootHeader` / `rootFooter` は Store の現在状態に含まれないため復元対象外。所有者 (呼び出し側) が view load 後に適用する。
- Store 接続中は Store が正である。view load 前に公開 API `applyTheme` / `applyDiff(.full)` で直接適用した値は、view load 時に Store 現在状態で置き換わる (Store 接続と直接適用の併用は非保証)。
- 保証するのは viewDidLoad 完了時点での Store 現在状態への収束のみで、view load 前に届いた個々の Diff のイベントとしての適用は保証しない。view load は `loadViewIfNeeded()` や `.view` 参照でも発生し、window への attach とは独立のイベントである。
- 公開 init は Store 経由のみである。root 直接指定の init は internal (Preview / Test 用) で、利用者は公開 API だけでは Store を接続しない Controller を作れない — 直接適用 API (`applyTheme` / `applyDiff`) は実質的に Store 接続中の Controller に対する操作になる。

## model と表示の同期

Controller は hidden 要素を含む model と、表示対象だけの visible projection を分けて保持する。visible projection は `Section.isVisible` と、Cell が `VisibilityAware` の場合の `isVisible` から作る。`VisibilityAware` に準拠しない独自 Cell は visible として扱う。

| 変更 | iOS の反映 |
|---|---|
| Section / Cell の追加・削除・移動 | Diffable Data Source の snapshot 構造を更新する |
| 同じ ID の Cell 内容更新 | item の identity を保ったまま再構成する |
| 可視性の変更 | model から visible projection を再構築する |
| Theme の変更 | 構造を変えず、現在の Theme と表示中 Cell を更新する |

snapshot の Cell identity は `KsCell.id` だけを包む `KsCellID` である。title、選択値、style などの内容を identity に含めない。同じ ID の内容更新には `replaceCell` を使い、ID 自体を変える場合は remove + insert で表す。`replaceCell` / `replaceCells` は同じ ID のまま具象型が変わる差し替え (例: `LabelCell → SwitchCell`) も受け付け、その場合は item identity を保ったまま Native cell の交換で反映される ([表示状態同期](../../core/architecture/display-state-synchronization.md)の「内容更新」節)。

内容更新は **行を描画する時点の最新 Cell 値**で反映される。Controller は更新要求を受けた時点で model の索引を同期更新し、行の描画時にその索引から Cell を引くため、Native への適用が遅延しても更新要求時点の古い値が後から画面に現れることはない。入力 Cell で「打鍵 → callback → 書き戻し → 再描画」が往復する構成でも、遅れて届いた再描画が入力中の値を巻き戻さないのはこの性質による (実測で確認: `fix-ios-entrycell-writeback-race`)。

hidden な Section / Cell は model から削除しない。hidden 対象への更新は model に保持され、再表示時に更新済みの値が現れる。部分操作の index を visible projection の位置として渡してはならない。

## Cell Renderer Registry

`KsCellRegistry` は具象 `KsCell` 型と `UICollectionViewCell & KsCellRenderer` 型の対応を保持する。Host は Registry から型を解決して `render(cell:theme:)` を呼ぶため、独自 Cell を追加しても Controller に型分岐を加えない。

標準 Cell 12 種は、既定の `KsCellRegistry.shared` を使う Controller で自動登録できる。独立 Registry を注入する場合、自動登録 flag が `true` でも shared Registry にはならないため、必要な標準 Cell と独自 Cell をその Registry へ登録する。

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

利用者定義 Renderer は公開 `UICollectionViewCell` サブクラスとして `KsCellRenderer` に準拠する。ライブラリ内部の `KsListCellBase` は継承できない。未登録 Cell は DEBUG では assertion で検出し、assertion が無効なビルドでは空の placeholder Cell を表示する。

## スタイルと視覚状態

画面全体の既定値は `Theme`、単一 Cell の上書きは `CellStyle` が持つ。通常の描画値は Cell 個別値、CellStyle、Theme、UIKit 既定値の順で解決する。`Theme.backgroundColor` が list の canvas、`Theme.cellBackgroundColor` が Cell の既定背景、`CellStyle.backgroundColor` が個別 Cell の背景であり、互いに代用しない。

Cell 個別高さは Theme の行高さより優先され、iOS の最終行高は 48pt を下回らない。`Theme.hasUnevenRows == true` では内容に応じて伸び、`false` では解決済み高さへ固定する。

操作可能な Cell の highlighted / selected 中は `Theme.selectedColor` を使い、無効な Cell は選択背景を使わず、テキスト色を `Theme.disabledTextColor` へ置き換える。無効化は Cell initializer の `isEnabled` で指定する。

Theme 属性の未指定時に使われるライブラリ既定値は、`Theme` の public static 定数として公開される。利用者は「既定へ戻す」「既定値を基準に派生値を作る」用途でこれらを参照できる。

| 定数 | 既定値の対象 |
|---|---|
| `defaultSeparatorColor` | 罫線色 |
| `defaultSelectedColor` | 選択中背景色 |
| `defaultAccentColor` | アクセント色 |
| `defaultBackgroundColor` | list 背景色 |
| `defaultDisabledTextColor` | 無効時テキスト色 |
| `defaultHeaderBackgroundColor` | Header 背景色 |
| `defaultFooterBackgroundColor` | Footer 背景色 |
| `defaultHeaderTextColor` | Header テキスト色 |
| `defaultFooterTextColor` | Footer テキスト色 |
| `defaultHeaderFooterFont` | Header / Footer フォント |
| `defaultCellTitleColor` | Cell タイトル色 |
| `defaultCellTitleFont` | Cell タイトルフォント |
| `defaultCellDescriptionColor` | Cell 説明文色 |
| `defaultCellDescriptionFont` | Cell 説明文フォント |
| `defaultCellHintFont` | Cell ヒントフォント |
| `defaultButtonTitleColor` | ButtonCell タイトル色 |
| `defaultCellIconSize` | icon サイズ |
| `defaultCellIconRadius` | icon 角丸半径 |

## 保証すること

- Store 方式では、初期状態と後続更新が同じ `SettingsRootStore → KsSettingsViewController` 経路へ流れる。
- Store 接続済みなら、view load 完了時点の表示は Store の現在状態と一致する (取り付け順序に依存しない。[core/ADR-0019](../../../decisions/core/0019-host-restores-from-store-on-attach.md))。
- Root / Section Accessory が空または `nil` なら、意味のない supplementary 領域を生成しない。
- Store が Controller より長命でも、Store 購読と UIKit の DataSource / Delegate が Controller を延命しない。
- Registry の登録・解決は排他制御され、同じ Cell 型を再登録した場合は後の Renderer が使われる。
- Cell の再利用時は前の内容を除去し、編集中の text field は不必要な再生成で first responder を失わない。

## してはいけないこと

- Controller の内部 root を直接差し替えない。
- Theme 更新を `SettingsRootDiff` に混ぜない。
- Cell 具象型ごとの分岐を Controller へ追加しない。
- hidden 要素を model から削除して可視性を表現しない。
- Diff の index を visible projection 上の位置として渡さない。
- Cell の内容値を snapshot identity に含めない。

## 利用例

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
let controller = KsSettingsViewController(store: store, style: .classic)
controller.rootHeader = .text("プロフィール")

store.insertCell(
    LabelCell(title: "ライセンス"),
    in: section.id,
    at: section.cells.count
)
```

## 関連

- [iOS SwiftUI Bridge と宣言 DSL](ios-swiftui.md)
- [SettingsRoot・Section・Cell の設定ツリー](../../core/core-model/settings-tree.md)
- [SettingsRootDiff による構造変更](../../core/core-model/structural-changes.md)
- [基本 Cell](../../core/cells/basic-cells.md)
- [入力 Cell](../../core/cells/input-cells.md)
