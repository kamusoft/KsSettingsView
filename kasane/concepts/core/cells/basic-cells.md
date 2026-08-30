---
type: reference
title: 基本 Cell
description: 表示・操作・二値・単一選択を担う基本7種の Cell と状態所有の公開契約
tags: [cells, public-api, ui]
timestamp: 2026-08-24
---

この文書は、`LabelCell`、`CommandCell`、`ButtonCell`、`SwitchCell`、`CheckboxCell`、`RadioCell`、`SimpleCheckCell` の用途と公開契約を説明する。読むと、各 Cell の選び方、状態と callback の責務、iOS / Android の宣言 DSL での使い方が分かる。

## 目的

基本 Cell は、設定行の表示時点の内容と状態を持つ UI 層の値モデルである。Native control の寿命や画面全体の状態は所有せず、利用者操作を callback で外部へ通知する。本書を Cell 共通契約の入口とし、入力固有の差分は [入力 Cell](input-cells.md) で説明する。

iOS では `KsCell`、Android では `Cell` を実装する。直接構築時の ID は iOS が `UUID()`、Android が Cell 種別 prefix + UUID の `String` で自動採番される。宣言 DSL は再評価のたびに identity が変わらないよう、自動採番 ID を安定位置または明示 `.cellID(...)` に基づく ID へ置き換える。

## Cell 共通契約

- 値 + callback 経路では、利用者または Store が状態値を保持し、callback を受けて新しい Cell 値を供給する。
- 宣言 DSL は、対象 Cell に TwoWay overload がある場合だけ Binding / MutableState から現在値を読み、操作 callback で同じ状態所有者へ書き戻す。基本7種では Android `SwitchCell` の `MutableState<Boolean>` overload だけが該当し、iOS の基本 Cell と Android の他6種は値 + callback を使う。
- `style` は Cell 単位の視覚上書きで、UI 層が Theme と合成する。
- `isEnabled = false` は操作を無効にし、`isVisible = false` は [設定ツリー](../core-model/settings-tree.md) で定義する visible projection から Cell を除外する。

## 公開 API

| 型 | 用途と固有契約 |
|---|---|
| `LabelCell` | 読み取り専用表示。操作 control は持たない |
| `CommandCell` | 処理・遷移を要求し、`onTap` を通知する。`hideArrow = false` なら Disclosure Indicator を表示する |
| `ButtonCell` | 明示的なボタン操作。`onTap`、`titleColor`、`titleAlignment` を持ち、Disclosure Indicator は表示しない |
| `SwitchCell` | `isOn` を表示し、`onValueChanged` へ反転値を通知する |
| `CheckboxCell` | 独立した二値 `isChecked` を checkbox で表す |
| `RadioCell` | `groupId` 内の単一選択。`value == selectedValue` のとき選択表示し、`onSelected(value)` を通知する |
| `SimpleCheckCell` | 独立した二値 `isChecked` を簡易 checkmark で表す |

全7種が `style`、`title`、`valueText`、`icon`、`hintText`、`isEnabled`（既定 `true`）、`isVisible`（既定 `true`）を持つ。`ButtonCell` だけは `description` を公開しない。

`ButtonCell.titleAlignment` の既定は center で、Swift は `.start` / `.center` / `.end`、Kotlin は `START` / `CENTER` / `END` を使う。alignment が視覚に出るのは title が主行の全幅を使える行 — つまり `valueText` (行内 trailing) を持たない行に限る。`valueText` がある行では title 領域はコンテンツ幅になり、配る余白がないため CENTER / END は視覚に出ない ([Cell 共通行のレイアウト](../styling/cell-row-layout.md) の主行の幅配分、core/ADR-0026)。`icon` / `hintText` は主行の幅配分に参加しないため、これらだけを持つ行では alignment は従来どおり働く。補助フィールドの有無にかかわらず Disclosure Indicator は表示しない。

画像の case と fallback は [KsImage](ks-image.md) を参照する。

## 状態所有と callback

状態値と表示内容は Cell の値等価・hash に参加し、callback は参加しない。ユーザー操作時は Native 表示を更新して callback を通知するが、Cell 値を内部で永続更新するわけではない。利用者は callback を受け、更新後の状態値を持つ Cell を次の描画へ供給する。

`RadioCell` のグループ状態も利用者が所有する。`groupId` は所属を表し、`selectedValue` を自動更新する Store を Cell 自身は持たない。Android の `SwitchCell` DSL だけは `MutableState<Boolean>` overload を持つため、利用例では callback の代わりに TwoWay 経路を使っている。

## 保証すること

- `isEnabled = false` では操作 callback を発火せず、control も操作不能になる。
- `isVisible = false` では Cell 値を model に保持したまま visible projection から除外する。
- `CheckboxCell` と `SimpleCheckCell` は反転した二値を通知する。
- Android の `RadioCell` は選択済み行の再タップで `onSelected` を再通知しない。iOS は選択済みでも `onSelected(value)` を通知するため、共通ロジックは再通知の有無へ依存しない。
- `KsCellRegistry.registerBasicCells()`（iOS）/ `KsCellRegistry.registerBasicCells(context)`（Android）で7種を一括登録できる。

## してはいけないこと

- `ButtonCell` に `description` を渡してはならない。
- callback が状態を所有すると仮定してはならない。
- `CheckboxCell`、`RadioCell`、`SimpleCheckCell` を交換可能な同義 Cell として扱ってはならない。独立二値、共有単一選択、簡易な独立選択という意味が異なる。
- iOS の基本 Cell に `Binding` initializer があると仮定してはならない。現行 API は値 + callback である。
- 独自 Cell を標準7種の一括登録だけで描画できると仮定してはならない。独自 renderer / ViewHolder 登録は [設定ツリー](../core-model/settings-tree.md) を参照する。

## 利用例

```swift
@State private var notifications = false
@State private var theme = "light"

KsSettingsView {
    Section("一般") {
        LabelCell(title: "バージョン", valueText: "1.0.0")
        SwitchCell(
            title: "通知",
            isOn: notifications,
            onValueChanged: { notifications = $0 }
        )
        RadioCell(
            title: "ダーク",
            groupId: "theme",
            value: "dark",
            selectedValue: theme,
            onSelected: { theme = $0 }
        )
    }
}
```

```kotlin
val notifications = remember { mutableStateOf(false) }
var theme by remember { mutableStateOf("light") }

KsSettingsView {
    Section(header = "一般") {
        LabelCell(title = "バージョン", valueText = "1.0.0")
        SwitchCell(title = "通知", isOn = notifications)
        RadioCell(
            title = "ダーク",
            groupId = "theme",
            value = "dark",
            selectedValue = theme,
            onSelected = { theme = it },
        )
    }
}
```

## 関連

- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [入力 Cell](input-cells.md)
- [KsImage](ks-image.md)
