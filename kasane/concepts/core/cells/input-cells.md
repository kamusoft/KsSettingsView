---
type: reference
title: 入力 Cell
description: 文字列・候補・数値・時刻・日付を編集する入力5種の公開契約
tags: [cells, input, public-api]
timestamp: 2026-08-28
---

この文書は、`EntryCell`、`PickerCell`、`NumberPickerCell`、`TimePickerCell`、`DatePickerCell` の公開契約を説明する。読むと、各入力 Cell の状態型、TwoWay 経路、表示値の生成、iOS / Android 固有 API の差が分かる。

## 目的

入力 Cell は、文字列、候補、数値、時刻、日付を編集する UI 層の値モデルである。外部状態所有、`style`、`isEnabled`、`isVisible`、Registry、visible projection の共通契約は [基本 Cell](basic-cells.md) を正とし、本書は入力固有の差分だけを説明する。

値 + callback 経路では、利用者または `SettingsRootStore` が状態を保持し、callback を受けて新しい Cell 値を供給する。入力5種には宣言 DSL の TwoWay overload があり、iOS の `Binding<T>` initializer、Android Compose の `MutableState<T>` 拡張関数が同じ状態所有者へ変更を書き戻す。

## 公開 API

| 型 | 用途と固有契約 |
|---|---|
| `EntryCell` | `text`、`placeholder`、`placeholderColor`、Native `keyboardType`、`isPassword`、`textAlignment`、`maxLength`、`onTextChanged` を持つ |
| `PickerCell` | `PickerItem` (主表示 `text` + 副表示 `subText`) の列 `items` から単一または複数を選択する。単一は `selectedIndex`、複数は `selectedIndices` と `maxSelectedNumber` を使う。object 候補はジェネリック縁で受ける (下記「PickerCell の object 候補と書き戻し」) |
| `NumberPickerCell` | `min` から `max` まで `step` 刻みで `value` を選ぶ |
| `TimePickerCell` | iOS は `Date` の時刻部分、Android は `LocalTime` を編集する。選択面の時制は `is24Hour` (既定 `true` = 24時間制) が唯一の決定源で、`format` は行の表示専用 ([core/ADR-0028](../../../decisions/core/0028-timepickercell-is24hour-sole-hour-cycle-source.md)) |
| `DatePickerCell` | iOS は `Date` の日付部分、Android は `LocalDate` を編集する |

5種は `style`、`title`、`description`、`icon`、`hintText`、`isEnabled`、`isVisible` を持つ。Picker / NumberPicker / TimePicker / DatePicker は `valueText` も持ち、明示値がなければ現在値から表示文字列を作る。`EntryCell` は入力 control 自身が値を表示するため `valueText` を持たず、`text` を使う。入力中テキストの色は valueText の解決順 (`CellStyle` → Theme の valueText 既定 → Theme の title 既定 → platform default) に従う ([スタイルの所有と実効値解決](../styling/style-resolution.md))。

`EntryCell` の placeholder 文字色は標準の解決順 — `placeholderColor` → `CellStyle.placeholderColor` → `Theme.cellPlaceholderColor` → OS 既定 — で解決する。未指定なら OS 既定の placeholder 色のままで、ダークモードに自動追従する。MAUI facade は `EntryCell.PlaceholderColor` (Cell 単位) と `SettingsView.CellPlaceholderColor` (画面全体既定) を公開する。

日付・時刻・keyboard は Native 型を直接公開する。iOS は `Foundation.Date` / `UIKeyboardType`、Android は `LocalDate` / `LocalTime` / `android.text.InputType` の `Int` である。

## 選択と表示値

- `PickerSelectionMode.single` / `Single` は `selectedIndex` と `onSelectionChanged(Int)`、`.multiple` / `Multiple` は `selectedIndices` と `onMultiSelectionChanged(Set<Int>)` を使う。`maxSelectedNumber = 0` は複数選択の上限なしを意味する。
- `valueText` があれば自動表示より優先する。なければ Picker は選択項目、NumberPicker は `unit` 適用結果 (`NumberPickerCell.format`: `unit` が空なら数値のみ、非空なら `"<値> <unit>"`。iOS / Android 共通)、TimePicker / DatePicker は `format` 適用結果を表示する。
- Picker の自動表示は選択項目の `PickerItem.text` のみで組み立てる (`subText` は含めない)。複数選択は有効な index を順に `, ` で連結する。表示の加工は縁の射影 (`displayText`) が担う — 旧 `displayFormatter` は削除済み ([core/ADR-0029](../../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md))。
- PickerCell の行タップで開く選択 UI (確定・破棄・上限・スタイル継承・初期スクロール) の契約は [PickerCell の選択面](picker-selection-surface.md) を正とする。
- NumberPickerCell の行タップで開く選択 UI (候補生成・unit 適用・確定と破棄・初期選択) の契約は [NumberPickerCell の選択面](number-picker-selection-surface.md) を正とする。
- DatePickerCell の行タップで開く選択 UI (uiStyle ごとの器・確定と破棄・`todayText`・Android のカレンダーダイアログ配色と Spinner 3連ホイール) の契約は [DatePickerCell の選択面](date-picker-selection-surface.md) を正とする。
- Android の TimePickerCell の行タップで開く選択 UI (時制の決定・系列構成・確定と破棄・配色) の契約は [TimePickerCell の選択面](time-picker-selection-surface.md) を正とする。

## PickerCell の object 候補と書き戻し

候補のモデルは `PickerItem` 値型 (text + subText) の列で、object の世界は API の縁で受ける ([core/ADR-0029](../../../decisions/core/0029-pickercell-item-model-with-generic-edge-projection.md)):

- **ジェネリック縁**: iOS は convenience init、Android は factory 関数 (callback 経路は `ks-settingsview-ui`、`MutableState` 経路は `ks-settingsview-compose`) が `List<T>` + 射影 (`displayText` / `subText`) を受け、構築時に1回だけ `PickerItem` 列へ射影する。iOS の縁は `T: Sendable`、object TwoWay (`selectedItem`) はさらに `T: Equatable` を要求する (Kotlin に対応する制約はない)
- **文字列ケースは String 特殊化** (射影省略時は恒等)。従来の文字列列の呼び出し形はそのまま書ける — 互換保証ではなく設計としての簡易形
- **書き戻し**: 選択の正は index のまま。確定操作で index の書き戻しが先、object callback (`onItemSelected` / 複数選択は index 昇順の `onItemsSelected`) が後に1回ずつ。`selectedItem` TwoWay の object → index 逆引きは値等価の**最初に一致した index** に解決する。候補に無い要素は未選択
- **複数選択の object TwoWay は提供しない** — TwoWay は `selectedIndices` (`Set<Int>`) のみで、object 列は確定 callback で受け取る
- **snapshot 契約**: 縁は元要素列を構築時にコピーして捕捉し、表示・逆引き・object callback は同一 snapshot を参照する。元コレクションの in-place 変更は観測しない (差し替えで反映)
- 空文字列の `subText` は縁で「副表示なし」(`nil` / `null`) へ正規化される

```swift
struct Plan: Sendable { let name: String; let detail: String }

PickerCell(
    title: "プラン",
    items: plans,
    displayText: { $0.name },
    subText: { $0.detail },
    selectedIndex: $planIndex,
    onItemSelected: { plan in /* 元の Plan が届く */ }
)
```

## プラットフォーム差

- `DatePickerUIStyle` は iOS が `.wheels` / `.calendar`、Android が `Material` / `Spinner` を持つ。
- `DatePickerCell.todayText` は「今日」へジャンプする操作のオプトインで iOS / Android の全形式に適用される ([DatePickerCell の選択面](date-picker-selection-surface.md))。`androidButtonColor` は Android 固有である。
- `EntryCell.keyboardType` は iOS が `UIKeyboardType`、Android が `InputType` 定数の `Int` である。
- Android の `EntryCell` はフォーカス中の入力欄を値の SSoT として扱い、同一 Cell への内容更新 (書き戻しエコー含む) で text とキャレットを差し替えない。反映はフォーカス喪失時の再同期で行われるため、callback を受けて `text` を更新しない構成ではフォーカス喪失時に表示が最後の供給値へ戻る ([android/ADR-0014](../../../decisions/android/0014-entrycell-focused-editor-owns-text.md))。**iOS の `EntryCell` はこの契約を持たない** — iOS Host は行を描画する時点で最新の Cell 値を引くため、書き戻しエコーが遅れて届いても入力中の値を巻き戻さない。フォーカス中の内容更新はそのまま入力欄へ反映され、callback を受けて `text` を更新しない構成でも表示は戻らない ([iOS Native Host の更新境界](../../ios/api/ios-native-host.md))。
- Android の単一行 `EntryCell` は Enter (IME の完了アクション・物理キーボードとも) を「完了」として扱い、キーボードを閉じてフォーカスは維持する。次フィールドへのフォーカス送りは行わない。`keyboardType` に `TYPE_TEXT_FLAG_MULTI_LINE` を含めた場合は Enter で改行する ([android/ADR-0003](../../../decisions/android/0003-entrycell-enter-key-ime-action-done.md))。
- Android の `TimePickerCell` が開く時刻選択 UI は、ホスト形態によらずボトムシート + 時・分ホイールである ([android/ADR-0018](../../../decisions/android/0018-timepickercell-bottom-sheet-wheel-unification.md))。時制 (12/24h) は両 platform とも `is24Hour` で決まり、契約は [TimePickerCell の選択面](time-picker-selection-surface.md) を正とする。iOS の `accentColor` は埋め込み picker の `tintColor` と入力ツールバーに適用され、選択面全体の配色契約は Android 固有。

## 保証すること

- 入力変更は対応 callback に通知され、Binding / MutableState 経路では元の状態へ書き戻される。
- `PickerCell` の `maxSelectedNumber > 0` は複数選択数の上限となる。範囲外 index は自動表示から除外される。
- `NumberPickerCell.step <= 0` は描画側で1へ fallback する。iOS は表示時に値を min / max と候補 step に合わせる。
- `EntryCell.maxLength` を超える入力は受け付けず、`isPassword` は Native の secure/password 入力へ反映する。
- `isEnabled = false` は入力 control、picker 起動、callback を無効にする。
- キーボード回避 (フォーカスした入力 Cell がソフトウェアキーボードに隠れない) はライブラリに明示実装を持たず、各 platform の標準機構で成立する。見え方は OS で異なる — iOS はコンテンツのスクロール調整 (画面上部の行は残る)、Android / MAUI Android は window ごと押し上げる pan 系 — が、いずれもフォーカスした Cell はキーボード直上に収まる (iOS / Android / MAUI iOS / MAUI Android の4環境で実測確認 2026-08-24。MAUI Android は `WindowSoftInputMode` 未指定の構成のまま動作する)。
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

- [SettingsRoot・Section・Cell の設定ツリー](../core-model/settings-tree.md)
- [基本 Cell](basic-cells.md)
- [PickerCell の選択面](picker-selection-surface.md)
- [NumberPickerCell の選択面](number-picker-selection-surface.md)
- [TimePickerCell の選択面](time-picker-selection-surface.md)
- [DatePickerCell の選択面](date-picker-selection-surface.md)
- [KsImage](ks-image.md)
