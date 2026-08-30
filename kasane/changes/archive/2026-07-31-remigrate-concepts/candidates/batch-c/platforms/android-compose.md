---
type: reference
title: Android Compose Bridge と宣言 DSL
description: KsSettingsView の Store 方式・DSL 方式、identity、modifier、Theme 伝播の利用契約
tags: [android, compose, dsl, public-api]
timestamp: 2026-07-19
---

この文書は、Jetpack Compose から KsSettingsView を使うための公開 API 利用契約と責務境界を整理した reference である。読むと、Store 方式と DSL 方式の選び方、動的要素の identity、Root・Section・Cell の構築、Theme の更新経路が分かる。Android View Host を直接使う場合は [Android Native Host の利用と更新境界](android-native-host.md) を参照する。

## 目的

Compose の `KsSettingsView` は、Compose の状態または利用者所有の `SettingsRootStore` を Android View の `KsSettingsView` へ接続する公開 Composable である。内部は `AndroidView` で Native Host を埋め込み、Compose 専用の list、visible projection、Cell renderer、Theme / CellStyle 解決を再実装しない。

Compose 側は宣言ツリーの構築、Recomposition をまたぐ状態保持、値と event の橋渡しを担う。最終的な描画と表示同期は Store と Native Host の既存経路へ収束する。

## 公開 API

公開入口は同名の `KsSettingsView` 2 overload であり、DSL 方式と Store 方式を用途に応じて選ぶ。

| 方式 | Store の所有者 | 向くケース | 更新方法 |
|---|---|---|---|
| DSL | `KsSettingsView` の内部 | 静的・中規模の一般的な設定画面 | Compose state から宣言ツリーを再評価する |
| Store | 利用者 | 大量データ、高頻度更新、命令型の部分操作 | 利用者が `SettingsRootStore` の公開操作を呼ぶ |

両方式とも `modifier`、`style`、任意 Composable の `rootHeader` / `rootFooter` を受ける。`style` の既定値は `Classic` である。

## 利用例

### DSL 方式

一般的な設定画面には `KsSettingsView(theme = ...) { ... }` を使う。内部 Store、前回の resolved tree、前回 Theme は Composition の identity が続く間 `remember` で保持される。Recomposition 時は `AndroidView.update` で宣言ツリーを再評価し、初回だけでなく連続した state 変更も Store へ渡す。

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.sectionFooter

@Composable
fun SettingsScreen() {
    var enabled by remember { mutableStateOf(true) }

    KsSettingsView {
        Section(header = "通知") {
            SwitchCell(
                title = "プッシュ通知",
                isOn = enabled,
                onValueChanged = { enabled = it },
            )
        }.sectionFooter("端末の通知設定も確認してください")
    }
}
```

### Store 方式

Store 方式は利用者所有の `SettingsRootStore` を `KsSettingsView(store = ...)` へ渡す。Store は Recomposition で作り直さず `remember` などで保持し、表示後は Store の公開操作を呼ぶ。

```kotlin
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.settingsRoot
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme

val store = remember {
    SettingsRootStore(
        initialRoot = settingsRoot {
            section(id = "general", header = "一般") {
                cell(LabelCell(id = "version", title = "バージョン"))
            }
        },
        initialTheme = Theme(),
    )
}

KsSettingsView(store = store)
```

Store 方式と DSL 方式は別の描画基盤を持たず、どちらも `SettingsRootStore → Android Native Host` に収束する。Store 初期値用の純粋な `jp.kamusoft.kssettingsview.compose.settingsRoot { section(id = ...) { ... } }` builder と、Recomposition 用の `KsSettingsView { Section { ... } }` DSL は別の scope である。

## DSL の構築 API

`Section(...)` は文字列または任意 Composable の Header / Footer、`headerHeight`、`isVisible` と Cell 列を受け、`SectionHandle` を返す。同じ位置に文字列と Composable を同時指定すると失敗する。

Section 内には基本 Cell 7種と入力 Cell 5種を直接置ける。既存または利用者定義の `Cell` は `cell(cell)` か `+cell` で追加する。Cell を追加すると `CellHandle` が返る。

| 対象 | 主な操作 |
|---|---|
| Root | `style` / `theme` 引数、`rootHeader` / `rootFooter` |
| SectionHandle | `sectionHeader`、`sectionFooter`、`sectionID` |
| CellHandle / Cell | `font`、`cellHeight`、`titleColor`、`backgroundColor`、`icon`、`cellID` |

modifier は元の Cell の他フィールドと、それまでに指定した style 値を維持する。組み込み Cell 12種は style と icon の両 modifier に対応する。利用者定義 Cell で同じ操作を有効にするには、`DSLStyleModifiableCell` / `DSLIconModifiableCell` と対応 copy API を実装する。`font` が変更するのは `CellStyle.titleFont` だけで、hintText の font は変更しない。

同名 modifier には次の二つの形がある。

| 形 | receiver と戻り値 | 非対応時の挙動 |
|---|---|---|
| `Cell` extension | `Cell` を受け、新しい `Cell` を返す | style / icon capability がなければ同じ Cell を返す。後続の chain は継続できる |
| `CellHandle` extension | DSL に追加済みの Cell を指す Handle を受け、同じ `CellHandle` を返す | 対象 Cell に capability がない、または Handle の index が無効なら Cell を変更しない。後続の chain は継続できる |

`cellID` は style / icon とは別の契約である。明示 identity hint 自体は記録されるが、最終的な `Cell.id` の再束縛には `DSLReidentifiableCell` が必要になる。非準拠 Cell では例外にせず元の `Cell.id` を維持するため、利用者定義 Cell 自身が再評価間の安定 ID を保証する。

```kotlin
// 組み込み Cell は style・icon・ID 再束縛に対応する。
LabelCell(title = "通知")
    .titleColor(Color.Gray)
    .icon(notificationIcon)
    .cellID("notification")

// capability を実装しない CustomCell では style / icon は no-op。
// cellID の hint は記録されるが、実体 ID は CustomCell.id のままになる。
cell(CustomCell(id = "stable-custom"))
    .titleColor(Color.Gray)
    .icon(notificationIcon)
    .cellID("custom-hint")
```

`disabled(Boolean)` は `CellHandle` 版・`Cell` 版とも現行では常に no-op である。無効な Cell は各 Cell initializer の `isEnabled` で構築する。

## Compose state の橋渡し

TwoWay helper は `MutableState` 自体を Cell の永続状態として保持しない。DSL 評価時点の `state.value` を Cell 値へ写し、Cell callback から `state.value` へ書き戻す。`SwitchCell` と入力 Cell 5種には `MutableState` overload がある。値と callback を明示する通常 overload でも同じ境界を構成できる。

同一 ID の Cell 内容が変わると、DSL は構造 Diff を作らず `replaceCells` の内容更新経路へ渡す。Section / Cell の可視性が変わると、他の差分と混ぜず完全な model から visible projection を再構築する full 更新へ切り替える。

## 宣言ツリーの identity

Compose は Recomposition ごとに Section / Cell の値を作り直すため、一時インスタンスの既定 ID を最終 identity として使わない。DSL は identity hint から決定的な String ID を解決し、同じ ID の内容変更を remove + insert ではなく内容更新として扱う。

- 動的コレクションでは DSL の `forEach(items, key = ...)` を使う。要素が `KsIdentifiable` なら key lambda を省略できる。
- 静的 Section は header text と位置、静的 Cell は親 Section ID・位置・Cell 型から fallback ID を解決する。
- `.sectionID(...)` / `.cellID(...)` は明示 hint を与えるが、引数値そのものを最終 ID 文字列にする API ではない。
- title、選択値、CellStyle などの内容は identity に含めない。

同じ要素では `forEach` key と `sectionID` / `cellID` を併用しない。どちらか一方だけを identity として指定する。両方を組み合わせた優先順位は [ADR-0008](../../decisions/0008-stable-declarative-tree-identity.md) と現行 Android 実装で食い違うためである。

位置 fallback は動的な挿入・削除・並べ替えに弱い。動的構造で位置を意味上の identity として使わない。一つの `forEach` item から同じ階層へ複数 Section / Cell を返すと、同じ hint により ID が衝突するため、一 item は一要素へ対応させる。

```kotlin
data class Item(
    override val id: String,
    val title: String,
) : KsIdentifiable

KsSettingsView {
    Section(header = "項目") {
        forEach(items) { item ->
            LabelCell(title = item.title)
        }
    }
}
```

この例では item の追加・削除・並べ替え後も既存 item の ID が保たれる。`LabelCell` に `cellID` を重ねない。

静的要素を意味上の名前で追跡したい場合は、明示 ID だけを使う。

```kotlin
KsSettingsView {
    Section(header = "一般") {
        LabelCell(title = "バージョン").cellID("app-version")
    }.sectionID("general")
}
```

`"app-version"` と `"general"` は安定 ID を導く hint であり、最終 ID 文字列そのものではない。この例に `forEach` key を重ねない。

## Theme の伝播

DSL 方式では `theme` 引数を内部 Store の初期 Theme とし、以後は前回と異なる Theme だけを `store.applyTheme` へ渡す。Theme 更新は構造 Diff を発行せず、Section / Cell の ID と構造を変えない。

Store 方式には `theme` 引数がない。利用者は `SettingsRootStore(initialTheme = ...)` と `store.applyTheme(newTheme)` を使い、Host は Store の `theme` を購読する。`style` と Root H/F は Theme とは別の画面状態として `AndroidView.update` から Host へ渡る。

Theme と CellStyle は UI 層で Jetpack Compose 側の型 `Color`、`TextStyle`、`Dp` を直接持つ。通常属性の解決順は CellStyle、Theme、Android / Material の既定値である。Compose の `MaterialTheme` だけでは Native Host の Material widget 要件を満たさないため、アプリの XML Theme も `Theme.Material3.*` 派生にする。詳細は [Android Native Host の利用と更新境界](android-native-host.md#material3-theme-の前提) を参照する。

## 保証すること

- Store 方式と DSL 方式は同じ Native Host と Store / Diff 経路を使う。
- DSL の内部 Store と前回 resolved tree は Composition の identity が続く間保持する。
- 同じ意味上の identity と内容から再評価した場合、不要な構造 Diff を発行しない。
- 同じ ID の内容変更は identity を保ったまま Cell 内容を更新する。
- 可視性変更は通常の内容更新へ押し込まず full 更新へ切り替える。
- Cell modifier は元の Cell を破壊せず、対応する copy または Handle 上の置換として反映する。
- Theme 更新は Section / Cell の ID と構造を変えない。

## してはいけないこと

- Store 方式と DSL 方式に別の Native 描画基盤を持たせない。
- Root H/F、Theme、CellStyle を Core の `SettingsRoot` に含めない。
- Store や DSL 内部状態を Recomposition ごとに作り直さない。
- Compose state object を Cell の永続状態として保持させない。
- 動的構造で位置 fallback に意味上の identity を期待しない。
- 同じ要素で `forEach` key と明示 `sectionID` / `cellID` を併用しない。
- 一つの `forEach` item から同階層へ複数要素を返さない。
- `disabled(true)` を機能する無効化 API として案内しない。

## 用語

| 用語 | 意味 |
|---|---|
| Store 方式 | 利用者所有の `SettingsRootStore` を渡す方式 |
| DSL 方式 | `KsSettingsView` が内部 Store と前回ツリーを保持する方式 |
| `settingsRoot` builder | Store 初期値の `SettingsRoot` を明示 ID 付きで構築する純粋関数。Recomposition DSL とは別物 |
| identity hint | `forEach` key、明示 ID、静的位置など、最終 ID を決める入力 |
| resolved tree | identity hint を `Section.id` / `Cell.id` へ反映済みの宣言ツリー |
| 位置 fallback | 明示 key がない静的要素を親 ID・位置・型などから追跡する代替規則 |
| 内容更新 | 同じ Cell ID のまま表示内容だけを再 bind する更新 |
| full 更新 | visible projection の再構築が必要なとき、Root 全体を現在 model から反映する更新 |

## 関連

- [Android Native Host の利用と更新境界](android-native-host.md)
- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [SettingsRootDiff による構造変更](../core-model/structural-changes.md)
- [基本 Cell](../cells/basic-cells.md)
- [入力 Cell](../cells/input-cells.md)
- [KsImage](../cells/ks-image.md)
