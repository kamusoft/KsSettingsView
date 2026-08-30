---
type: reference
title: 入力 Cell
description: 文字列・候補・数値・時刻・日付を編集する入力5種の公開契約
tags: [cells, input, public-api]
timestamp: 2026-07-18
---

この文書は、`EntryCell`、`PickerCell`、`NumberPickerCell`、`TimePickerCell`、`DatePickerCell` の公開契約を説明する。読むと、各入力 Cell の状態型、TwoWay 経路、表示値の生成、iOS / Android 固有 API の差が分かる。

## 目的

入力 Cell は、文字列、候補、数値、時刻、日付を編集する UI 層の値モデルである。外部状態所有、`style`、`isEnabled`、`isVisible`、Registry、visible projection の共通契約は [基本 Cell](cells/basic-cells.md) を正とし、本書は入力固有の差分だけを説明する。

値 + callback 経路では、利用者または `SettingsRootStore` が状態を保持し、callback を受けて新しい Cell 値を供給する。入力5種には宣言 DSL の TwoWay overload があり、iOS の `Binding<T>` initializer、Android Compose の `MutableState<T>` 拡張関数が同じ状態所有者へ変更を書き戻す。

## 公開 API

| 型 | 用途と固有契約 |
|---|---|
| `EntryCell` | `text`、`placeholder`、Native `keyboardType`、`isPassword`、`textAlignment`、`maxLength`、`onTextChanged` を持つ |
| `PickerCell` | `items` から単一または複数を選択する。単一は `selectedIndex`、複数は `selectedIndices` と `maxSelectedNumber` を使う |
| `NumberPickerCell` | `min` から `max` まで `step` 刻みで `value` を選ぶ |
| `TimePickerCell` | iOS は `Date` の時刻部分、Android は `LocalTime` を編集する |
| `DatePickerCell` | iOS は `Date` の日付部分、Android は `LocalDate` を編集する |

5種は `style`、`title`、`description`、`icon`、`hintText`、`isEnabled`、`isVisible` を持つ。Picker / NumberPicker / TimePicker / DatePicker は `valueText` も持ち、明示値がなければ現在値から表示文字列を作る。`EntryCell` は入力 control 自身が値を表示するため `valueText` を持たず、`text` を使う。

日付・時刻・keyboard は Native 型を直接公開する。iOS は `Foundation.Date` / `UIKeyboardType`、Android は `LocalDate` / `LocalTime` / `android.text.InputType` の `Int` である。

## 選択と表示値

- `PickerSelectionMode.single` / `Single` は `selectedIndex` と `onSelectionChanged(Int)`、`.multiple` / `Multiple` は `selectedIndices` と `onMultiSelectionChanged(Set<Int>)` を使う。`maxSelectedNumber = 0` は複数選択の上限なしを意味する。
- `valueText` があれば自動表示より優先する。なければ Picker は選択項目、NumberPicker は数値、TimePicker / DatePicker は `format` 適用結果を表示する。
- `displayFormatter` は Picker の項目表示へ適用する。複数選択の自動表示は有効な index を順に `, ` で連結する。

## プラットフォーム差

- `NumberPickerCell.unit` は iOS 固有で、Android の現行 API にはない。
- `DatePickerUIStyle` は iOS が `.wheels` / `.calendar`、Android が `Material` / `Spinner` を持つ。
- `DatePickerCell.todayText` は iOS 固有、`androidButtonColor` は Android 固有である。
- `EntryCell.keyboardType` は iOS が `UIKeyboardType`、Android が `InputType` 定数の `Int` である。

## 保証すること

- 入力変更は対応 callback に通知され、Binding / MutableState 経路では元の状態へ書き戻される。
- `PickerCell` の `maxSelectedNumber > 0` は複数選択数の上限となる。範囲外 index は自動表示から除外される。
- `NumberPickerCell.step <= 0` は描画側で1へ fallback する。iOS は表示時に値を min / max と候補 step に合わせる。
- `EntryCell.maxLength` を超える入力は受け付けず、`isPassword` は Native の secure/password 入力へ反映する。
- `isEnabled = false` は入力 control、picker 起動、callback を無効にする。
- `KsCellRegistry.registerInputCells()`（iOS）/ `KsCellRegistry.registerInputCells(context)`（Android）で5種を一括登録できる。
- 呼び出し側は `NumberPickerCell.min <= max`、`EntryCell.maxLength` は `nil` / `null` または0以上を指定する。それ以外の範囲外値に対する platform fallback は公開契約として依存しない。

## してはいけないこと

- `EntryCell` に `valueText` を追加して二重の入力値 API を作ってはならない。
- iOS と Android の日付・時刻・keyboard 型、`DatePickerUIStyle` case を同一と仮定してはならない。
- `PickerCell.selectionMode` と状態フィールドを混同してはならない。
- iOS / Android 固有引数をクロスプラットフォーム共通引数として扱ってはならない。
- `selectionMode` と異なる側の state / callback を使ってはならない。

## 利用例

```swift
@State private var name = ""
@State private var themeIndex: Int? = 0
@State private var alarm = Date()

KsSettingsView {
    Section("入力") {
        EntryCell(title: "名前", text: $name, maxLength: 20)
        PickerCell(
            title: "テーマ",
            items: ["ライト", "ダーク"],
            selectedIndex: $themeIndex
        )
        TimePickerCell(title: "アラーム", time: $alarm)
    }
}
```

```kotlin
val name = remember { mutableStateOf("") }
val themeIndex = remember { mutableStateOf<Int?>(0) }
val alarm = remember { mutableStateOf(LocalTime.of(7, 0)) }

KsSettingsView {
    Section(header = "入力") {
        EntryCell(title = "名前", text = name, maxLength = 20)
        PickerCell(
            title = "テーマ",
            items = listOf("ライト", "ダーク"),
            selectedIndex = themeIndex,
        )
        TimePickerCell(title = "アラーム", time = alarm)
    }
}
```

## 関連

- [SettingsRoot・Section・Cell の設定ツリー](core-model/settings-tree.md)
- [基本 Cell](cells/basic-cells.md)
- [KsImage](cells/ks-image.md)
