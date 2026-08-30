---
type: reference
title: Android Native Host の利用と更新境界
description: SettingsRootStore と KsSettingsView を使って Android View の設定画面を構築・更新・拡張する方法
tags: [android, views, host, public-api]
timestamp: 2026-07-19
---

この文書は、Android View から KsSettingsView を使うための公開 API 利用契約と責務境界を整理した reference である。読むと、`SettingsRootStore` と `KsSettingsView` の役割、表示後の更新方法、独自 Cell の登録方法、ホスト側の Theme 前提が分かる。Jetpack Compose から使う場合は [Android Compose Bridge と宣言 DSL](android-compose.md) を参照する。設定ツリーと差分の型自体は [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md) と [SettingsRootDiff による構造変更](../core-model/structural-changes.md) を先に読む。

## 目的

`jp.kamusoft.kssettingsview.ui.KsSettingsView` は Core の `SettingsRoot` を `RecyclerView` へ接続する公開 Host である。`SettingsRootStore` が hidden 要素を含む現在状態と更新通知を持ち、Host が visible projection、平坦な表示リスト、Cell 描画を担当する。

この Host は XML / Android View から直接利用できるほか、Compose の `AndroidView` と将来の外部バインディングから再利用される。Core モデルの定義、宣言ツリー同士の比較、Theme と CellStyle の実効値解決は Host の外側にある。

## 状態と所有者

| 用語 | 所有者 | 意味と担当する更新 |
|---|---|---|
| model | `SettingsRootStore` と Host の内部状態 | hidden 要素を含む完全な `SettingsRoot`。Store の公開操作で更新する |
| visible projection | `KsSettingsView` | model から表示対象だけを元の順序・IDで取り出した派生状態 |
| 表示リスト | `KsSettingsView` と内部 Adapter | Section Header / Footer と Cell を平坦化し、Root Header / Footer と接続した `RecyclerView` の行列 |
| Host | `KsSettingsView` | Store の状態と通知を visible projection・Adapter・Cell 描画へ接続する境界 |

## 公開 API

### SettingsRootStore

`SettingsRootStore(initialRoot, initialTheme)` は現在の `SettingsRoot` と `Theme` を読み取り専用 `StateFlow` として公開する。表示後の変更は次の公開操作を使う。

| 対象 | 操作 |
|---|---|
| Root 全体 | `replaceAll` |
| Section | `insertSection`、`removeSection`、`moveSection`、`replaceSection` |
| Cell | `insertCell`、`removeCell`、`replaceCell`、`replaceCells`、`moveCell` |
| Accessory | `updateAccessory` |
| Theme | `applyTheme` |

Section / Cell 操作の index は、非表示要素を含む model 配列上の位置である。挿入先と移動先は有効範囲へ clamp される。対象 ID が見つからない remove / move / replace、および挿入先 Section が見つからない Cell insert は、状態を変えず構造 Diff も発行しない。

`replaceCells` は存在する Cell だけを一回の状態更新にまとめ、同時に変更した Cell ID 群を一つの内容更新バッチとして流す。RadioCell の選択変更など、複数行を同時に再描画するときに使う。対象が一つなら `replaceCell` を使う。どちらも Cell ID を変更する操作ではない。

`applyTheme` は構造 Diff を発行しない。同値の Theme なら通知しない。購読開始時は一過性の通知ではなく `state.value` と `theme.value` から現在値を復元する。

### KsSettingsView

XML またはコードで `KsSettingsView` を生成し、`bind(store)` で Store へ接続する。`bind` は Store の現在 root と Theme を直ちに反映する。ViewTree に `LifecycleOwner` がまだない場合は Store を保持し、attach 後に購読開始を再試行する。

`style` は `Classic` / `Modern`、`rootHeader` / `rootFooter` は Root Header / Footer を表す `RootAccessory` である。Root Header / Footer は `SettingsRoot` に含まれず、`null` は対応行を表示しない。Section Header / Footer は各 `Section` の `SectionAccessory` から描画する。どちらも文字列と `KsAnyView.Compose` / `.AndroidView` の任意 View を利用できる。

通常の Store 方式では、Theme の唯一の正は `store.theme` である。初期値は `SettingsRootStore(initialTheme = ...)`、表示後の変更は `store.applyTheme(...)` を使う。`bind(store)` は、その前に `view.theme` へ直接設定した値を `store.theme.value` で上書きする。bind 後の `view.theme = ...` は View だけを一時的に変更して Store を更新せず、次の Store Theme 通知や再 bind で上書きされるため、Store 方式では使わない。

公開 `view.theme` は、外部バインディングや Preview が Store を使わず `view.applyDiff(SettingsRootDiff.Full(root))` から Host を直接駆動する場合の入口である。この高度な方式と `bind(store)` を同じ View で併用しない。

空の `SettingsRoot` も有効で、空の `RecyclerView` として表示できる。detach 時は Store 購読を停止し、内部 RecyclerView から Adapter 参照を切る。

## model と表示の同期

Host は hidden 要素を含む model と、表示対象だけの visible projection を分けて保持する。visible projection は `Section.isVisible` と、Cell が `VisibilityAware` の場合の `isVisible` から作る。`VisibilityAware` に準拠しない独自 Cell は visible として扱う。

| 変更 | Android の反映 |
|---|---|
| Section / Cell の追加・削除・移動 | 現在 model を平坦化し、`ListAdapter.submitList` へ渡す |
| 同じ ID の Cell 内容更新 | item identity を保ち、`submitList` の反映完了後に対象 ViewHolder を再 bind する |
| 複数 Cell の連動内容更新 | 一回の `submitList` の反映完了後に対象行をまとめて再 bind する |
| 可視性の変更 | model から visible projection を再構築する |
| Theme の変更 | 構造を変えず、背景・行・Accessory・装飾を再評価する |

stable item ID は行種別と Section / Cell ID から決まり、title、選択値、style などの内容を含めない。同じ ID の内容更新には `replaceCell` / `replaceCells` を使い、ID 自体を変える場合は remove + insert で表す。

hidden な Section / Cell は model から削除しない。hidden 対象への更新は model に保持され、再表示時に更新済みの値が現れる。部分操作の index を visible projection の位置として渡してはならない。

## Cell Renderer Registry

`KsCellRegistry` は具象 `Cell` 型を `viewType` と `CellViewHolder` factory へ対応付ける。Host は Registry から型を解決して `bind(cell, theme)` を呼ぶため、独自 Cell を追加しても Host に型分岐を加えない。

標準 Cell 12 種は Host の構築時に自動登録される。利用者定義 Cell は表示前に登録し、Root / Section Accessory の予約値を避けるため `KsCellRegistry.CELL_VIEW_TYPE_MIN` 以上の `viewType` を使う。

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG

KsCellRegistry.register(
    cellClass = MyCell::class,
    viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN + 50,
) { parent ->
    MyCellViewHolder(parent)
}
```

同じ Cell 型の再登録は後勝ちで factory を置き換える。別の Cell 型に同じ `viewType` を割り当てると失敗する。未登録 Cell は `strictMode == true` で例外として早期検出し、`false` では高さ0の placeholder へ退避する。`strictMode` の既定値は `true` であり、アプリの build 種別へ自動追従しない。

独自 `CellViewHolder` は bind ごとに最新 Cell と Theme を反映し、再利用時の `reset()` で listener、Job、画像、埋め込み View などを解放する。

## スタイルと視覚状態

画面全体の既定値は `Theme`、単一 Cell の上書きは `CellStyle` が持つ。通常属性は `CellStyle`、Theme、Android / Material の既定値の順で解決する。`Theme.backgroundColor` が RecyclerView の canvas、`Theme.cellBackgroundColor` が Cell の既定背景、`CellStyle.backgroundColor` が個別 Cell の背景であり、互いに代用しない。

Cell 個別高さは Theme の行高さより優先され、Android の最終行高は60dpを下回らない。`Theme.hasUnevenRows == true` では内容に応じて伸び、`false` では解決済み高さへ固定する。無効化は Cell initializer の `isEnabled` で指定し、無効時は Theme の disabled text 色と Native control の disabled 表現を使う。

`Classic` は Cell 行へ1物理 pixelの hairline を描き、Section 内の中間線だけ左16dp inset とする。`Modern` は Section の上下12dp・左右16dpに外側領域を取り、Section H/F と Cell を角丸背景でまとめる。Style の切替は model、stable ID、Registry を変えない。Theme の変更時は現在の Style の装飾も再構築される。

## Material3 Theme の前提

Host は `MaterialSwitch`、`MaterialCheckBox` と Material color attributes を描画入力として使う。Compose の `MaterialTheme` だけではなく、`AndroidView` が受け取る Context の XML Theme を `Theme.Material3.*` 派生にする。`?attr/materialSwitchStyle` を解決できない旧 framework / AppCompat / MaterialComponents Theme だけで動作すると想定してはならない。

## 保証すること

- 初期状態と後続更新は同じ `SettingsRootStore → KsSettingsView` 経路へ流れる。
- Root / Section Accessory が空なら意味のない行を生成しない。
- Theme 更新を SettingsRoot の構造変更として扱わない。
- Registry の解決後は Cell 固有の bind / reset を ViewHolder へ委譲する。
- Cell の内容更新と可視性変更を別の表示同期経路へ流す。

## してはいけないこと

- Host の内部 root や module-internal の `setRootDirect` を利用者コードから操作しない。
- Theme 更新を `SettingsRootDiff` に混ぜない。
- Cell 具象型ごとの分岐を `KsSettingsView` へ追加しない。
- hidden 要素を model から削除して可視性を表現しない。
- Diff の index を visible projection 上の位置として渡さない。
- Cell の内容値を stable item ID に含めない。
- 利用者定義 Cell の `viewType` に100未満の予約領域を使わない。

## 利用例

```kotlin
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme

val section = Section(
    id = "general",
    header = SectionAccessory.Text("一般"),
    cells = listOf(LabelCell(title = "バージョン", valueText = "1.0.0")),
)
val store = SettingsRootStore(
    initialRoot = SettingsRoot(sections = listOf(section)),
    initialTheme = Theme(),
)
val updatedTheme = Theme(cellTitleColor = Color.DarkGray)

findViewById<KsSettingsView>(R.id.settings_view).apply {
    rootHeader = RootAccessory.Text("プロフィール")
    bind(store)
}
store.insertCell(
    cell = LabelCell(title = "ライセンス"),
    sectionId = section.id,
    at = section.cells.size,
)
store.applyTheme(updatedTheme)
```

## 関連

- [Android Compose Bridge と宣言 DSL](android-compose.md)
- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [SettingsRootDiff による構造変更](../core-model/structural-changes.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
- [KsImage](../cells/ks-image.md)
